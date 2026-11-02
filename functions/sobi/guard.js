'use strict';

/**
 * SOBİ GÜVENLİK KATMANI — modele girenle çıkanın denetimi.
 *
 * GİRİŞ (cleanChildText)
 *   Çocuğun yazdığı/söylediği metin modele gitmeden e-posta, telefon ve bağlantı kalıpları
 *   maskeleniyor. 7–10 yaşındaki bir çocuk adresini ya da annesinin numarasını yazabilir; bu bilgi
 *   üçüncü taraf bir hizmete gitmemeli. Ad ve adres gibi kalıbı olmayan bilgiyi burada yakalamak
 *   mümkün değil; talimat metni modele bunları tekrar etmemesini söylüyor.
 *
 * ÇIKIŞ (checkReply)
 *   Cevaptaki her sayı bir kaynakta geçmeli: bağlamda, çocuğun mesajlarında ya da bu sohbetteki
 *   bir araç sonucunda. Model "kardeşi 2 gider" diye yanlış bir sayı uydurursa bu denetime takılır;
 *   çocuk yerine fallbackReply'deki kural motoru metnini görür. Bu kaba ama güvenli bir denetim:
 *   kaynakta geçen bir sayıyı yanlış yerde kullanmayı yakalamaz. Kural adlarındaki 5 ve 10 her zaman
 *   serbest.
 */

const explainer = require('./stepExplainer');

const MAX_CHILD_TEXT = 300;
const MAX_REPLY = 500;
const ALWAYS_ALLOWED = new Set([5, 10]);
const SMALL_FREE = 10;

const EMAIL = /[^\s@]+@[^\s@]+\.[^\s@]+/g;
const URL = /\b(?:https?:\/\/|www\.)\S+/gi;
// 9 ya da daha çok rakam (aralarında boşluk/tire/nokta olabilir): telefon, kimlik no. Abaküs
// 5 basamaklı; ders sayıları buna hiç yaklaşmıyor.
const LONG_DIGITS = /(?:\+?\d[\s.-]?){9,}/g;
// "benim adım Ali", "ismim Ayşe": ad modele gitmesin (denemede Sobi adı geri söyledi). Tek başına
// "adım" maskelenmiyor: derste "bu adım" (basamak) anlamında da geçiyor. Büyük harfler açıkça
// yazılı: /i bayrağı Türkçe "İ"yi "i" ile eşleştirmiyor. Endonezce "nama saya (adalah) Budi",
// "namaku Budi": "adalah" kalıba dahil, yoksa ad yerine o maskelenirdi.
const NAME = /([Bb]enim\s+[Aa]d[ıi]m|[Bb]enim\s+[İi]smim|[İi]smim|[Mm]y\s+name\s+is|[Mm]y\s+name's|[Nn]ama\s+saya(?:\s+adalah)?|[Nn]amaku(?:\s+adalah)?)\s+\p{L}+/gu;

/** Çocuğun metni: kırpılmış, kısaltılmış, kişisel kalıplar maskelenmiş. */
function cleanChildText(text) {
  if (typeof text !== 'string') return { text: '', masked: false };
  let t = text.replace(/\s+/g, ' ').trim().slice(0, MAX_CHILD_TEXT);
  const before = t;
  t = t.replace(EMAIL, '[e-posta]').replace(URL, '[bağlantı]').replace(LONG_DIGITS, '[numara] ').replace(NAME, '$1 [ad]');
  return { text: t.trim(), masked: t !== before };
}

/** Metindeki tam sayılar. Binlik ayırıcı nokta birleştirilir ("99.999" → 99999). */
function numbersIn(text) {
  const out = new Set();
  if (typeof text !== 'string') return out;
  const joined = text.replace(/(\d)\.(?=\d{3}(?!\d))/g, '$1');
  for (const m of joined.matchAll(/\d+/g)) out.add(Number(m[0]));
  return out;
}

/** Sohbet geçmişindeki kaynak sayılar: kullanıcı tarafındaki her metin ve araç sonucu. */
function sourceNumbers(messages) {
  const out = new Set();
  const addText = (t) => numbersIn(t).forEach((n) => out.add(n));
  for (const msg of messages) {
    if (msg.role !== 'user') continue;
    if (typeof msg.content === 'string') { addText(msg.content); continue; }
    for (const block of msg.content || []) {
      if (block.type === 'text') addText(block.text);
      else if (block.type === 'tool_result') {
        if (typeof block.content === 'string') addText(block.content);
        else for (const c of block.content || []) if (c.type === 'text') addText(c.text);
      }
    }
  }
  return out;
}

/**
 * Döner: { ok, reasons: [...] }. [explanation]: anlatım adımı (beklenen cevap yok). Orada 10'a kadarki
 * sayılar serbest: "sağdan 3. sütun", "1 boncuk" gibi doğal ifadeler; denetlenecek bir işlem yok.
 * Kapatınca Sobi anlatım adımında neredeyse hiç konuşamıyordu (28.10.2026, "kaynaksiz_sayi:1").
 * Soru adımında denetim sıkı kalıyor: orada yanlış boncuk sayısı çocuğu yanlış hamleye götürür.
 */
function checkReply(reply, sources, { explanation = false } = {}) {
  const reasons = [];
  if (typeof reply !== 'string' || reply.trim() === '') reasons.push('bos');
  else {
    if (reply.length > MAX_REPLY) reasons.push('uzun');
    const free = (n) => ALWAYS_ALLOWED.has(n) || (explanation && n <= SMALL_FREE);
    const unknown = [...numbersIn(reply)].filter((n) => !free(n) && !sources.has(n));
    if (unknown.length) reasons.push(`kaynaksiz_sayi:${unknown.join(',')}`);
  }
  return { ok: reasons.length === 0, reasons };
}

/**
 * Model cevap veremediğinde ya da cevabı denetime takıldığında çocuğa giden metin. Adımın ilk
 * hamlesi kural motorundan; sayılar hesaplanmış olduğu için güvenli.
 */
function fallbackReply(ctx, tools, lang = 'tr') {
  if (lang === 'en') return fallbackReplyEn(ctx);
  if (lang === 'id') return fallbackReplyId(ctx);
  // Genel sohbette (Sobi sekmesi) adım yok; kural motorunun söyleyeceği bir hamle de yok.
  if (ctx && ctx.mode === 'genel') {
    return 'Bunu şu an cevaplayamadım. Abaküs ya da uygulama hakkında başka bir şey sormak ister misin?';
  }
  // Çarpma adımında kural motoru yalnızca toplamayı görür ("10'luk kural: 10 gelir…"); çarpma
  // dersinde çocuğa anlamsız geliyordu (değerlendirme 29.10.2026). Adımın kendi sözü daha güvenli.
  if (ctx && Number.isInteger(ctx.target) && /^\s*\d+\s*[x×*]\s*\d+\s*$/.test(ctx.questionText || '') && ctx.stepText) {
    return `Birlikte bakalım! Bu adımda: "${ctx.stepText}" İstersen "Bana göster"e dokun.`;
  }
  const e = ctx && Number.isInteger(ctx.base) && Number.isInteger(ctx.target)
    ? explainer.explainTransition(ctx.base, ctx.target)
    : null;
  if (e && e.moves.length) {
    const m = tools.describe({ ...e, moves: [e.moves[0]] }).hamleler[0];
    return `Birlikte bakalım! ${m.basamak[0].toUpperCase()}${m.basamak.slice(1)} basamağında ${m.kural}: ${m.adimlar.join(', ')}. İstersen "Bana göster"e dokun.`;
  }
  // Anlatım adımında (beklenen cevap yok) "Bana göster" düğmesi ve gösterim yok.
  // Anlatım adımı: gösterim yok; dersin o adımdaki kendi sözü en güvenli açıklama.
  if (!ctx || !Number.isInteger(ctx.target)) {
    return ctx && ctx.stepText
      ? `Birlikte bakalım! Derste şöyle diyoruz: "${ctx.stepText}" Neresi zor geldi?`
      : 'Bunu birlikte düşünelim! Neresi zor geldi, biraz daha anlatır mısın?';
  }
  return 'Bunu birlikte abaküste görelim mi? "Bana göster"e dokunabilirsin.';
}

/**
 * İngilizce yedek metin. Kural motorunun hamle metni (tools.describe) Türkçe olduğu için hamleyi
 * söylemiyor; çocuğu "Show me"a ve adımın kendi (İngilizce) metnine yönlendiriyor.
 */
function fallbackReplyEn(ctx) {
  if (ctx && ctx.mode === 'genel') {
    return "I couldn't answer that right now. Do you want to ask something else about the abacus or the app?";
  }
  if (!ctx || !Number.isInteger(ctx.target)) {
    return ctx && ctx.stepText
      ? `Let's look at it together! In the lesson we say: "${ctx.stepText}" Which part was hard?`
      : "Let's think about it together! Which part was hard? Can you tell me a bit more?";
  }
  return 'Let\'s look at it together on the abacus! You can tap "Show me".';
}

/** Endonezce yedek metin: İngilizcedeki gibi hamleyi söylemiyor, "Tunjukkan"a yönlendiriyor. */
function fallbackReplyId(ctx) {
  if (ctx && ctx.mode === 'genel') {
    return 'Aku belum bisa menjawab itu sekarang. Mau tanya hal lain tentang sempoa atau aplikasinya?';
  }
  if (!ctx || !Number.isInteger(ctx.target)) {
    return ctx && ctx.stepText
      ? `Ayo kita lihat bersama! Di pelajaran kita bilang: "${ctx.stepText}" Bagian mana yang sulit?`
      : 'Ayo kita pikirkan bersama! Bagian mana yang sulit? Bisa ceritakan sedikit lagi?';
  }
  return 'Ayo kita lihat bersama di sempoa! Kamu bisa ketuk "Tunjukkan".';
}

module.exports = {
  MAX_CHILD_TEXT,
  MAX_REPLY,
  cleanChildText,
  numbersIn,
  sourceNumbers,
  checkReply,
  fallbackReply,
};

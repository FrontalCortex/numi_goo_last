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

const EMAIL = /[^\s@]+@[^\s@]+\.[^\s@]+/g;
const URL = /\b(?:https?:\/\/|www\.)\S+/gi;
// 9 ya da daha çok rakam (aralarında boşluk/tire/nokta olabilir): telefon, kimlik no. Abaküs
// 5 basamaklı; ders sayıları buna hiç yaklaşmıyor.
const LONG_DIGITS = /(?:\+?\d[\s.-]?){9,}/g;

/** Çocuğun metni: kırpılmış, kısaltılmış, kişisel kalıplar maskelenmiş. */
function cleanChildText(text) {
  if (typeof text !== 'string') return { text: '', masked: false };
  let t = text.replace(/\s+/g, ' ').trim().slice(0, MAX_CHILD_TEXT);
  const before = t;
  t = t.replace(EMAIL, '[e-posta]').replace(URL, '[bağlantı]').replace(LONG_DIGITS, '[numara] ');
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

/** Döner: { ok, reasons: [...] }. */
function checkReply(reply, sources) {
  const reasons = [];
  if (typeof reply !== 'string' || reply.trim() === '') reasons.push('bos');
  else {
    if (reply.length > MAX_REPLY) reasons.push('uzun');
    const unknown = [...numbersIn(reply)].filter((n) => !ALWAYS_ALLOWED.has(n) && !sources.has(n));
    if (unknown.length) reasons.push(`kaynaksiz_sayi:${unknown.join(',')}`);
  }
  return { ok: reasons.length === 0, reasons };
}

/**
 * Model cevap veremediğinde ya da cevabı denetime takıldığında çocuğa giden metin. Adımın ilk
 * hamlesi kural motorundan; sayılar hesaplanmış olduğu için güvenli.
 */
function fallbackReply(ctx, tools) {
  const e = ctx && Number.isInteger(ctx.base) && Number.isInteger(ctx.target)
    ? explainer.explainTransition(ctx.base, ctx.target)
    : null;
  if (e && e.moves.length) {
    const m = tools.describe({ ...e, moves: [e.moves[0]] }).hamleler[0];
    return `Birlikte bakalım! ${m.basamak[0].toUpperCase()}${m.basamak.slice(1)} basamağında ${m.kural}: ${m.adimlar.join(', ')}. İstersen "Bana göster"e dokun.`;
  }
  return 'Bunu birlikte abaküste görelim mi? "Bana göster"e dokunabilirsin.';
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

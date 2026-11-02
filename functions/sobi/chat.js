'use strict';

/**
 * SOBİ SOHBET TURU — çocuğun bir mesajını alır, modelle araç döngüsünü yürütür, cevabı denetler.
 *
 * Firestore'a ve Firebase'e bağlı değil: istemci (Anthropic SDK) dışarıdan veriliyor. Böylece
 * scripts/test-sobi.js sahte bir istemciyle her yolu (araç, ret, uzun cevap, kaynaksız sayı)
 * API'ye para ödemeden sınayabiliyor. Firestore işi callable.js'te.
 *
 * NEDEN ELLE YAZILMIŞ DÖNGÜ (SDK'nın tool runner'ı yerine)
 *   - Tur başına araç çağrısı sınırı var (MAX_TOOL_ROUNDS): model kendini döngüye sokarsa çocuk
 *     beklemesin, fatura büyümesin.
 *   - Son cevap çocuğa gitmeden guard.checkReply'den geçiyor; kaynak sayılar araç sonuçlarından.
 *   - Ekran işleri (abakuste_goster, alistirma_ver) toplanıp uygulamaya dönüyor.
 *
 * GEÇMİŞ YALNIZCA EKLENEREK BÜYÜR
 *   Modelin her cevabı (düşünme blokları dahil) geçmişe olduğu gibi ekleniyor, hiçbir eski mesaj
 *   düzenlenmiyor. Model, önceki turlardaki düşünme bloklarının değişmediğini denetliyor;
 *   düzenlenmiş geçmiş reddedilir. Denetime takılan bir cevap da bu yüzden geçmişte KALIYOR;
 *   çocuğun yerine ne gördüğü bir sonraki turda <not> ile modele söyleniyor.
 *   Ret (refusal) ve yarıda kesilen (max_tokens) cevaplar geçmişe hiç eklenmiyor.
 */

const tools = require('./tools');
const guard = require('./guard');
const explainer = require('./stepExplainer');
const explanations = require('./explanations');
const wallet = require('./wallet');

/**
 * Model: claude-haiku-5-5 (kullanıcı kararı 28.10.2026). Hesabı kural motoru yaptığı için model
 * yalnızca araç çağırıp anlatıyor; Opus 5.5'in yaklaşık kırkta biri maliyetle. Opus ile
 * karşılaştırma: `npm run sobi:dene -- --model claude-opus-5-5`.
 */
const DEFAULT_MODEL = 'claude-haiku-5-5';
/**
 * 'medium' (Haiku 5.5'in kendi varsayılanı). 28.10.2026 denemesinde 'low' uzun ve dağınık yazdı,
 * çocuğun adını geri söyledi, işlemin sonucunu kendisi hesaplayıp denetime takıldı; 'medium' kısa,
 * doğru ve kurallara uygun kaldı. Maliyet farkı mesaj başına bir sentin onda birinden az.
 */
const DEFAULT_EFFORT = 'medium';
const MAX_TOKENS = 8000;
const MAX_TOOL_ROUNDS = 4;
/**
 * Ret olursa sunucu isteği kategoriye göre önerilen modelle yeniden çalıştırır. Haiku 5.5'te bu
 * yok; orada ret, çocuğa yedek metin olarak döner (runTurn).
 */
const FALLBACK_BETA = 'server-side-fallback-2026-07-01';
const FALLBACK_MODELS = new Set(['claude-opus-5-5', 'claude-opus-5', 'claude-fable-5-1']);

const STEP_ID = /^[a-z0-9_]{1,60}$/;
const MAX_VALUE = 99999;
const MAX_LOG = 200;

/**
 * Dersin başından bu adıma kadarki bütün adımlar (kullanıcı kararı 29.10.2026; eskiden son 6).
 * Çocuk "baştaki kardeş olayı" diye önceki bir konuyu da sorabiliyor; en uzun öğretici ~80 adım,
 * ~2.700 karakter. Toplam sınır, sonda kalan (yeni) adımlar korunarak.
 */
const MAX_RECENT_STEPS = 150;
const MAX_STEPS_CHARS = 8000;
/** Bağlamda "az önce" diye ayrı gösterilen son adımlar; "bunu" önce bunlardır. */
const NEAR_STEPS = 6;

/** Kural anahtarı → çocuğun bildiği ad (homework.js ve RuleAnalysis ile aynı anahtarlar). */
const RULE_LABELS = {
  add_direct: 'kuralsız toplama',
  add_five: "5'lik toplama",
  add_ten: "10'luk toplama",
  add_bead: 'boncuk kuralıyla toplama',
  sub_direct: 'kuralsız çıkarma',
  sub_five: "5'lik çıkarma",
  sub_ten: "10'luk çıkarma",
  sub_bead: 'boncuk kuralıyla çıkarma',
};

const intIn = (v, lo, hi) => (Number.isInteger(v) && v >= lo && v <= hi ? v : null);
const text = (v, max) => (typeof v === 'string' ? v.replace(/\s+/g, ' ').trim().slice(0, max) : '');

/** Sondan başa toplam karakter sınırı: en eski adımlar düşer. */
function capChars(list, max) {
  let total = 0;
  let i = list.length;
  while (i > 0 && total + list[i - 1].length <= max) total += list[--i].length;
  return list.slice(i);
}

/** Uygulamanın gönderdiği adım bağlamı; bilinmeyen alanlar atılır, aralık dışı sayılar null. */
function sanitizeContext(raw) {
  const r = raw && typeof raw === 'object' ? raw : {};
  const log = Array.isArray(r.valueLog)
    ? r.valueLog.slice(-MAX_LOG).map((v) => intIn(v, 0, MAX_VALUE)).filter((v) => v !== null)
    : [];
  return {
    stepId: typeof r.stepId === 'string' && STEP_ID.test(r.stepId) ? r.stepId : null,
    stepText: text(r.stepText, 400),
    questionText: text(r.questionText, 200),
    base: intIn(r.base, 0, MAX_VALUE),
    target: intIn(r.target, 0, MAX_VALUE),
    current: intIn(r.current, 0, MAX_VALUE),
    attempts: intIn(r.attempts, 0, 99),
    valueLog: log,
    // Uygulama alıştırma hazırlayabiliyor mu. Öğretici bunu yapamıyor: "Kontrol Et" yalnızca
    // adımın kendi cevabını denetliyor, Sobi başka bir işlem hazırlarsa çocuk karışır.
    practice: r.practice === true,
    // Anlatım adımlarında da soru sorulabilsin diye: dersin adı, son adımların metni (eskiden
    // yeniye) ve çocuğun o ana kadar öğrendiği kurallar. Sobi açıklamayı dersin kendi sözlerine
    // dayandırsın, henüz öğretilmemiş bir kuralı anlatıp kafa karıştırmasın.
    lessonTitle: text(r.lessonTitle, 80),
    recentSteps: Array.isArray(r.recentSteps)
      ? capChars(r.recentSteps.map((s) => text(s, 300)).filter(Boolean).slice(-MAX_RECENT_STEPS), MAX_STEPS_CHARS)
      : [],
    taughtRules: Array.isArray(r.taughtRules)
      ? [...new Set(r.taughtRules.filter((k) => typeof k === 'string' && RULE_LABELS[k]))]
      : [],
    // Hazır açıklamayı bulmak için: hangi öğretici, kaçıncı adım (explanations.find metni de denetler).
    tutorialNumber: intIn(r.tutorialNumber, 0, 99999),
    stepIndex: intIn(r.stepIndex, 0, 999),
    // Sobi ekranında açılan örneklerin sonucu (eskiden yeniye, en çok 5): Sobi zorluğu ayarlasın.
    practiceResults: Array.isArray(r.practiceResults)
      ? r.practiceResults.slice(-5).map((p) => ({
        question: p && typeof p.question === 'string' && /^[\d +\-−]{1,40}$/.test(p.question) ? p.question.trim() : null,
        wrong: intIn(p && p.wrong, 0, 99),
        done: !!(p && p.done === true),
      })).filter((p) => p.question && p.wrong !== null)
      : [],
    // Kural hafızası (uygulama, bu cihaz): her kuralda kaç doğru, kaç yanlış. Kişisel bilgi yok.
    ruleStats: Array.isArray(r.ruleStats)
      ? r.ruleStats.map((x) => ({
        key: x && RULE_LABELS[x.key] ? x.key : null,
        correct: intIn(x && x.correct, 0, 1000000),
        wrong: intIn(x && x.wrong, 0, 1000000),
      })).filter((x) => x.key && x.correct !== null && x.wrong !== null)
      : [],
  };
}

/**
 * Genel sohbet (Sobi sekmesi, kullanıcı kararı 02.11.2026): adım ve soru yok. Uygulama bağlam
 * göndermiyor; [facts] sunucunun kendi okuduğu bilgi (plan, bakiye, homeworkRuleStats), istemciye
 * güvenilmiyor. Örnek açılabiliyor (practice): düğmeye dokununca abaküslü ekranda açılıyor.
 *   facts: { plan, freeLeft, freeLimit, credits, rules: [{ key, c, w }], levels: [{ key, level }] }
 */
function generalContext(facts = {}) {
  const f = facts || {};
  const n = (v) => (Number.isInteger(v) ? v : 0);
  return {
    ...sanitizeContext({ practice: true }),
    mode: 'genel',
    general: {
      plan: typeof f.plan === 'string' ? f.plan : 'Free',
      freeLeft: n(f.freeLeft),
      freeLimit: n(f.freeLimit),
      credits: n(f.credits),
      rules: (Array.isArray(f.rules) ? f.rules : [])
        .filter((r) => r && RULE_LABELS[r.key] && Number.isInteger(r.c) && Number.isInteger(r.w) && r.c + r.w > 0),
      levels: (Array.isArray(f.levels) ? f.levels : [])
        .filter((l) => l && RULE_LABELS[l.key] && Number.isInteger(l.level)),
    },
  };
}

/** En çok zorlandığı kural: en az 10 işlem içinde başarısı en düşük olan (%90 ve üstü sayılmaz). */
function weakestRule(rules) {
  let best = null;
  for (const r of rules) {
    const total = r.c + r.w;
    const pct = Math.round((r.c * 100) / total);
    if (total < 10 || pct >= 90) continue;
    if (!best || pct < best.pct) best = { key: r.key, pct };
  }
  return best;
}

function generalBlock(ctx) {
  const g = ctx.general;
  const lines = ['Genel sohbet: çocuk Sobi sekmesinde, bir dersin içinde değil. Ekranda abaküs yok; gösterim ve örnekler düğmeyle ayrı ekranda açılır.'];
  lines.push(`Planı: ${g.plan}. Bugün kalan ücretsiz Sobi mesajı: ${g.freeLeft} / ${g.freeLimit}. Sobi kredisi: ${g.credits}.`);
  if (g.rules.length) {
    lines.push(`Çocuğun kural başarısı (son çalıştıkları: dersler, ödevler, Sobi testleri): ${g.rules.map((r) => {
      const pct = Math.round((r.c * 100) / (r.c + r.w));
      return `${RULE_LABELS[r.key]}: ${r.c} doğru, ${r.w} yanlış (yüzde ${pct})`;
    }).join('; ')}`);
    const weak = weakestRule(g.rules);
    lines.push(weak
      ? `En çok zorlandığı kural: ${RULE_LABELS[weak.key]}`
      : 'Belirgin biçimde zorlandığı bir kural yok (ya da henüz az işlem çözdü).');
  } else {
    lines.push('Çocuğun henüz kural geçmişi yok: az ders çözmüş olabilir.');
  }
  if (g.levels.length) {
    lines.push(`Sobi testi seviyeleri (1 ile 20 arası): ${g.levels.map((l) => `${RULE_LABELS[l.key]} ${l.level}`).join('; ')}`);
  }
  return `<baglam>\n${lines.join('\n')}\n</baglam>`;
}

/** Bu bağlamda modele verilecek araçlar. */
function toolsFor(ctx) {
  // Liste sohbet boyunca HİÇ değişmemeli: model önceki turlardaki düşünme bloklarının aynı araç
  // listesiyle yazıldığını doğruluyor, liste değişince isteği reddediyor ("The `tools` list
  // differs from the one this block was created with"; 28.10.2026'da anlatım adımında
  // abakuste_goster'i çıkarınca oldu). Bu adımda yapılamayan araçları runTool reddediyor.
  // Genel sohbetin listesi ayrı ama o da kendi içinde hiç değişmiyor.
  return ctx && ctx.mode === 'genel' ? tools.GENERAL_TOOL_DEFS : tools.TOOL_DEFS;
}

/**
 * Çarpma sorusunda ("46 x 3") basamak çarpımları. Sobi çarpım sonuçlarını (12, 18, 120) söyleyince
 * guard bunları kaynaksız sayıp cevabı geri çeviriyordu (değerlendirme 29.10.2026: bazı çarpma
 * derslerinde cevapların %71'i). Burada hesaplanmış oldukları için hem doğru hem kaynaklı.
 */
function productLines(questionText) {
  const m = (questionText || '').match(/^\s*(\d{1,5})\s*[x×*]\s*(\d{1,3})\s*$/);
  if (!m) return [];
  const a = m[1].split('').reverse().map(Number);
  const b = m[2].split('').reverse().map(Number);
  const out = [];
  for (let j = 0; j < b.length; j++) {
    for (let i = 0; i < a.length; i++) {
      const p = a[i] * b[j];
      out.push(`${a[i]} x ${b[j]} = ${p}, sonucun birler basamağı abaküsün ${tools.columnName(i + j)} basamağına (${p * 10 ** (i + j)})`);
    }
  }
  return out.slice(0, 15);
}

/**
 * Soru adımında bu adımın işi ve çocuğun abaküsünün durumu. Ekrandaki soru ("4 + 9") çoğu zaman
 * bütün işlem, adım ise onun bir parçası (4 → 14, yalnızca "10 gelir"); Sobi bütün işlemi
 * anlatınca çocuk yanlış sayıya gidiyordu. Durum `hatayi_bul`'un aynısı: model %44 oranında
 * çağırıyordu, burada her soru adımında hazır.
 */
function stepLines(ctx) {
  if (ctx.base === null || ctx.target === null) return [];
  const e = explainer.explainTransition(ctx.base, ctx.target);
  if (!e || !e.moves.length) return [];
  const d = tools.describe(e);
  const part = tools.partOfWhole(ctx);
  const lines = [part
    ? `Bu adımın işi: ${part.text}`
    : `Bu adımın işi: ${ctx.base} → ${ctx.target}: ${d.hamleler.map((h) => `${h.basamak} basamağında ${h.kural} (${h.adimlar.join(', ')})`).join('; ')}`];
  if (ctx.questionText && ctx.questionText.replace(/\s/g, '') !== d.islem.replace(/\s/g, '')) {
    lines.push(`Ekrandaki "${ctx.questionText}" bütün işlem; bu adımda yalnızca yukarıdaki iş yapılıyor, önceki adımları yapıldı.`);
  }
  const f = tools.findMistake(ctx);
  lines.push(`Çocuğun abaküsü: ${f.durum}`);
  if (f.yapilan_adimlar) lines.push(`Çocuğun yaptığı adımlar: ${f.yapilan_adimlar.join('; ')}`);
  if (f.ipucu) lines.push(`İpucu (ilk yardımda bunu kullan): ${f.ipucu}`);
  if (f.siradaki_adim) lines.push(`Sıradaki adım (çocuk cevabı isterse ya da yeniden yardım isterse söyle): ${f.siradaki_adim}`);
  if (f.ilk_hata) lines.push(`Şaştığı hamle (çocuk cevabı isterse ya da yeniden yardım isterse söyle): ${f.ilk_hata.basamak} basamağında ${f.ilk_hata.kural}: ${f.ilk_hata.adimlar.join(', ')}`);
  return lines;
}

/**
 * [extra.messageInStep]: Sobi'nin bu adımdaki kaçıncı mesajı. Yardım sırası buna bağlı (ilk
 * yardımda ipucu, sonra hamle; talimat "Nasıl anlatırsın").
 */
function contextBlock(ctx, extra = {}) {
  if (ctx.mode === 'genel') return generalBlock(ctx);
  const lines = [];
  if (ctx.lessonTitle) lines.push(`Ders: ${ctx.lessonTitle}`);
  if (ctx.taughtRules && ctx.taughtRules.length) {
    lines.push(`Çocuğun şimdiye kadar öğrendiği kurallar: ${ctx.taughtRules.map((k) => RULE_LABELS[k]).join(', ')}`);
  }
  if (ctx.recentSteps && ctx.recentSteps.length) {
    const earlier = ctx.recentSteps.slice(0, -NEAR_STEPS);
    const near = ctx.recentSteps.slice(-NEAR_STEPS);
    if (earlier.length) {
      lines.push('Dersin önceki kısmı (eskiden yeniye):');
      for (const s of earlier) lines.push(`- ${s}`);
    }
    lines.push('Bu derste az önce anlatılanlar (eskiden yeniye):');
    for (const s of near) lines.push(`- ${s}`);
  }
  if (ctx.target === null) lines.push('Bu bir anlatım adımı: çocuk şu an bir soru cevaplamıyor, dersi dinliyor.');
  // Sobi ekranı (uygulama 01.11.2026): alistirma_ver'in işlemini sayfa sayfa, abaküste çözdürüyor.
  if (ctx.practice) lines.push('Bu ekranda örnek işlem açılabilir: alistirma_ver ile açtığın işlemi ekran çocuğa abaküste adım adım çözdürür.');
  if (ctx.practiceResults && ctx.practiceResults.length) {
    lines.push(`Bu ekranda açılan örnekler (eskiden yeniye): ${ctx.practiceResults.map((p) => `${p.question}: ${p.done ? 'çözdü' : 'henüz bitmedi'}, ${p.wrong} yanlış deneme`).join('; ')}`);
  }
  if (ctx.ruleStats && ctx.ruleStats.length) {
    lines.push(`Çocuğun kural geçmişi (dersler ve alıştırmalar): ${ctx.ruleStats.map((x) => `${RULE_LABELS[x.key]}: ${x.correct} doğru, ${x.wrong} yanlış`).join('; ')}`);
  }
  const ex = explanations.find(ctx);
  if (ex) {
    lines.push(`Onaylı açıklama (bu kısmın konusu: ${ex.konu}):`);
    lines.push(`- Basit: ${ex.basit}`);
    lines.push(`- Neden: ${ex.neden || 'Ders burada bir gerekçe vermiyor.'}`);
  }
  if (ctx.stepText) lines.push(`Adımın metni: ${ctx.stepText}`);
  if (ctx.questionText) lines.push(`Soru: ${ctx.questionText}`);
  if (ctx.base !== null) lines.push(`Adımın başında abaküste yazan: ${ctx.base}`);
  // Sütunlarıyla: "yüzler sütununa 6 yaz" gibi doğru bir cümle de guard'dan geçsin (600'ün 6'sı).
  if (ctx.target !== null) lines.push(`Beklenen cevap: ${ctx.target}; sütunlar: ${tools.columnsText(ctx.target)}`);
  if (ctx.current !== null) lines.push(`Şu an abaküste yazan: ${ctx.current}; sütunlar: ${tools.columnsText(ctx.current)}`);
  if (ctx.attempts !== null) lines.push(`Bu adımdaki deneme sayısı: ${ctx.attempts}`);
  lines.push(...stepLines(ctx));
  const products = productLines(ctx.questionText);
  if (products.length) lines.push(`Basamak çarpımları: ${products.join('; ')}`);
  if (extra.messageInStep) lines.push(`Sobi'nin bu adımdaki mesajı: ${extra.messageInStep}. (çocuğun bu adımdaki ${extra.messageInStep}. yardım isteği)`);
  return `<baglam>\n${lines.join('\n')}\n</baglam>`;
}

/** Bir bağlam bloğunu adıma bağlayan satırlar: aynı adımın önceki mesajlarını saymak için. */
function stepKey(blockText) {
  return String(blockText).split('\n')
    .filter((l) => /^(Adımın metni|Soru|Adımın başında abaküste yazan|Beklenen cevap):/.test(l)).join('|');
}

/** Geçmişte bu adımda çocuğun kaç mesajı var (bu mesaj hariç). Sohbet adımlar boyunca sürüyor. */
function previousInStep(history, key) {
  let n = 0;
  for (const m of history || []) {
    if (m.role !== 'user' || !Array.isArray(m.content)) continue;
    const first = m.content[0];
    if (first && first.type === 'text' && first.text.startsWith('<baglam>') && stepKey(first.text) === key) n++;
  }
  return n;
}

/** Cevabın altındaki öneri balonları. Sabit liste; uygulama metinleri kendi dilinde gösterir. */
function suggestionsFor(ctx, actions) {
  // Genel sohbet: balonlar uygulamanın kendi başlangıç önerileri; cevabın altında balon yok.
  if (ctx.mode === 'genel') return [];
  // Anlatım adımı: gösterim yok, örnek istemek anlamlı.
  if (ctx.target === null) return ['neden_boyle', 'ornek_ver', 'anladim'];
  const out = [];
  if (ctx.base !== null && ctx.target !== null && !actions.some((a) => a.tur === 'goster')) out.push('bana_goster');
  out.push('neden_boyle');
  if (actions.some((a) => a.tur === 'goster')) out.push('bir_daha_goster');
  else out.push('anladim');
  return out.slice(0, 3);
}

/**
 * Öneri balonu → hazır açıklamanın alanı. Bu balonlara anlatım adımında onaylı metin varsa model
 * çağrılmadan o döner: hata riski ve maliyet yok, cevap anında (kullanıcı kararı 31.10.2026).
 */
const DIRECT_SUGGESTIONS = { bunu_anlamadim: 'basit', neden_boyle: 'neden' };

/**
 * Genel sohbetin "Krediler nasıl çalışır?" balonu: kuralları sabit, model gerekmez (ücretsiz, anında).
 * Sayılar wallet.js'ten; plan hakları değişince metin de değişir.
 */
function creditsText() {
  const f = wallet.FREE_DAILY;
  return `Her gün ücretsiz mesaj hakkın var: Free planda ${f.Free}, Lite'ta ${f.Lite}, Pro'da ${f.Pro}. `
    + 'Hakkın bitince her mesaj 1 Sobi kredisi harcar. Benimle abaküste çözdüğün her yeni örnek de 1 mesaj sayılır. '
    + 'Krediler mağazadan alınır, süresizdir; satın almayı annen ya da baban onaylar. '
    + 'Hazır cevaplar ve benim cevap veremediğim mesajlar hiçbir şey harcamaz.';
}
const GENERAL_DIRECT = { krediler: { key: 'genel_krediler', text: creditsText } };

/** Balona modelsiz cevap: { text, key } ya da null (model çalışır). */
function directReplyFor(ctx, suggestion) {
  if (ctx.mode === 'genel') {
    const d = GENERAL_DIRECT[suggestion];
    return d ? { text: d.text(), key: d.key } : null;
  }
  const field = DIRECT_SUGGESTIONS[suggestion];
  if (!field) return null;
  const ex = explanations.find(ctx);
  return ex && ex[field] ? { text: ex[field], key: ex.key } : null;
}

/**
 * Modelsiz tur. Geçmişe çocuğun mesajı ve düz metin bir asistan cevabı eklenir; önceki mesajlar
 * (düşünme blokları) değişmez, yani sonraki model turları için geçmiş geçerli kalır ve model
 * çocuğa ne söylendiğini görür. runTurn ile aynı biçimde döner.
 */
function directTurn({ history, context, childText, text }) {
  const messages = [...(history || [])];
  const { text: cleaned, masked } = guard.cleanChildText(childText);
  const messageInStep = previousInStep(history, stepKey(contextBlock(context))) + 1;
  messages.push({ role: 'user', content: [
    { type: 'text', text: contextBlock(context, { messageInStep }) },
    { type: 'text', text: `<cocuk>${cleaned || '(boş mesaj)'}</cocuk>` },
  ] });
  messages.push({ role: 'assistant', content: [{ type: 'text', text }] });
  return {
    reply: text, modelReply: text, actions: [], suggestions: suggestionsFor(context, []),
    history: messages, usage: {}, fellBack: false, reasons: [], stopReason: 'end_turn', masked,
  };
}

function addUsage(total, u) {
  if (!u) return;
  for (const k of ['input_tokens', 'output_tokens', 'cache_read_input_tokens', 'cache_creation_input_tokens']) {
    total[k] = (total[k] || 0) + (Number(u[k]) || 0);
  }
}

/**
 * Tek tur.
 * @param {object} p
 * @param {object} p.client     Anthropic istemcisi (messages.create; yedek modelli modellerde beta.messages.create)
 * @param {string|string[]} p.system  Talimat metni (prompt_tr.md); genel sohbette [prompt_tr.md, prompt_genel_tr.md]
 * @param {Set<number>} [p.extraSources]  Cevapta serbest sayılar (genel sohbette ek talimattaki plan hakları, can, anahtar…)
 * @param {Array}  p.history    Önceki turların mesajları (düşünme blokları dahil, değiştirilmemiş)
 * @param {object} p.context    sanitizeContext çıktısı
 * @param {string} p.childText  Çocuğun mesajı (henüz temizlenmemiş)
 * @param {string} [p.note]     Önceki turda çocuğa gösterilen yedek metin (denetime takıldıysa)
 * @returns {Promise<{reply, actions, suggestions, history, usage, fellBack, reasons, stopReason, masked}>}
 */
async function runTurn({ client, system, history, context, childText, note, extraSources = null, model = DEFAULT_MODEL, effort = DEFAULT_EFFORT }) {
  const messages = [...(history || [])];
  const { text: cleaned, masked } = guard.cleanChildText(childText);
  const messageInStep = previousInStep(history, stepKey(contextBlock(context))) + 1;
  const content = [{ type: 'text', text: contextBlock(context, { messageInStep }) }];
  if (note) content.push({ type: 'text', text: `<not>Önceki cevabın denetimden geçmedi; çocuk onun yerine şunu gördü: "${note}"</not>` });
  content.push({ type: 'text', text: `<cocuk>${cleaned || '(boş mesaj)'}</cocuk>` });
  messages.push({ role: 'user', content });

  const usage = {};
  const actions = [];
  const toolState = {};
  let reply = null;
  let stopReason = null;
  const reasons = [];

  const params = {
    model,
    max_tokens: MAX_TOKENS,
    // Talimatın sonunda ayrı bir önbellek noktası: araç tanımları + talimat (~6.400 jeton) bütün
    // sohbetlerde aynı. Yalnızca sondaki otomatik noktayla her yeni sohbet bu öneki baştan
    // yazıyordu (değerlendirmede 20 sohbetin 20'si); şimdi sohbetler ortak öneki okuyor.
    // Genel sohbetin ek talimatı ayrı blok ve kendi önbellek noktası: ders talimatı aynen kalıyor,
    // ek metin değişince yalnızca genel sohbetlerin önbelleği yeniden yazılıyor.
    system: (Array.isArray(system) ? system : [system])
      .map((text) => ({ type: 'text', text, cache_control: { type: 'ephemeral' } })),
    tools: toolsFor(context),
    messages,
    output_config: { effort },
    cache_control: { type: 'ephemeral' },
  };
  const send = FALLBACK_MODELS.has(model)
    ? () => client.beta.messages.create({ ...params, betas: [FALLBACK_BETA], fallbacks: 'default' })
    : () => client.messages.create(params);

  for (let round = 0; ; round++) {
    const response = await send();
    addUsage(usage, response.usage);
    stopReason = response.stop_reason;

    // Ret ya da yarıda kesilmiş cevap: geçmişe eklenmez, çocuk yedek metni görür.
    if (stopReason === 'refusal' || stopReason === 'max_tokens') {
      reasons.push(stopReason);
      break;
    }
    messages.push({ role: 'assistant', content: response.content });

    if (stopReason === 'tool_use') {
      if (round >= MAX_TOOL_ROUNDS) {
        // Araç isteği cevapsız kalamaz (sonraki istek reddedilir): sonuçları "sınır" hatasıyla dön.
        messages.push({ role: 'user', content: toolUseBlocks(response).map((b) => ({
          type: 'tool_result', tool_use_id: b.id, is_error: true, content: 'Bu mesajda araç sınırına gelindi.',
        })) });
        reasons.push('arac_siniri');
        break;
      }
      const results = toolUseBlocks(response).map((b) => {
        const r = tools.runTool(b.name, b.input, context, toolState);
        if (r.action) actions.push(r.action);
        return { type: 'tool_result', tool_use_id: b.id, content: r.content, ...(r.isError ? { is_error: true } : {}) };
      });
      messages.push({ role: 'user', content: results });
      continue;
    }
    if (stopReason === 'pause_turn') continue;

    reply = response.content.filter((b) => b.type === 'text').map((b) => b.text).join('').trim();
    break;
  }

  let fellBack = reply === null;
  if (reply !== null) {
    const sources = guard.sourceNumbers(messages);
    if (extraSources) extraSources.forEach((n) => sources.add(n));
    const check = guard.checkReply(reply, sources, { explanation: context.target === null });
    if (!check.ok) { reasons.push(...check.reasons); fellBack = true; }
    // "Örneği açtım" deyip açmamış (araç çağrılmadı ya da reddedildi): çocuk boş ekrana bakardı.
    // Genel sohbette "Testi hazırladım", "Gösterimi açtım" da düğmeyle doğru olabilir.
    const opened = context.mode === 'genel' ? ['alistirma', 'goster', 'test'] : ['alistirma'];
    if (context.practice && /(açtım|hazırladım)/i.test(reply) && !actions.some((a) => opened.includes(a.tur))) {
      reasons.push('acilmayan_ornek');
      fellBack = true;
    }
  }
  const shown = fellBack ? guard.fallbackReply(context, tools) : reply;
  return {
    reply: shown,
    modelReply: reply,
    actions,
    suggestions: suggestionsFor(context, actions),
    history: messages,
    usage,
    fellBack,
    reasons,
    stopReason,
    masked,
  };
}

function toolUseBlocks(response) {
  return response.content.filter((b) => b.type === 'tool_use');
}

module.exports = {
  DEFAULT_MODEL,
  DEFAULT_EFFORT,
  MAX_TOOL_ROUNDS,
  FALLBACK_BETA,
  FALLBACK_MODELS,
  RULE_LABELS,
  sanitizeContext,
  generalContext,
  weakestRule,
  creditsText,
  toolsFor,
  contextBlock,
  suggestionsFor,
  directReplyFor,
  directTurn,
  runTurn,
};

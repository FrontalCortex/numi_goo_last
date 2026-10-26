'use strict';

/**
 * ÖDEV ÇEKİRDEĞİ — kurs öğretmeninin sınıfına ya da seçtiği öğrencilerine verdiği ödevin
 * ayarlarını doğrular ve dokümanını kurar.
 *
 * Soruları öğrencinin telefonu üretiyor (app/.../homework/HomeworkEngine.kt): ödevde yalnızca
 * ayarlar saklanıyor, sorular her adımda yeniden üretiliyor. Bu yüzden buradaki denetim
 * ARALIKLARLA sınırlı (basamak, adet, hız, soru sayısı); "bu kurallarla en fazla kaç sayı"
 * hesabı motorda ve öğretmenin formu onu zaten uyguluyor. Formu atlayan biri olmayacak bir
 * ödev yazsa öğrencinin telefonu ödevi açamaz ("Bu ödev açılamıyor"), başka bir zarar olmaz.
 *
 * Aralıklar HomeworkEngine ile AYNI olmalı; biri değişirse öteki de değişmeli.
 *
 * MODEL
 *   homeworks/{id}                          ayarlar + kime verildiği
 *     wholeClass: true                      sınıfın tamamı (sonradan katılan da görür)
 *     assignedUids: [...]                   wholeClass değilse seçilen öğrenciler
 *   homeworks/{id}/studentProgress/{uid}    öğrencinin ilerlemesi (öğrenci yazar, kurallar sınırlar)
 *
 * NEDEN AYRI MODÜL
 *   classes.js / bugReports.js ile aynı desen: saf karar burada, Firestore işi index.js'te.
 */

const OPS = ['add', 'sub', 'mul'];
const RULES = ['direct', 'five', 'ten', 'bead'];

const BLOCK_SIZE = 10;
const MIN_QUESTIONS = 1;
const MAX_QUESTIONS = 200;
const MIN_INTERVAL_MS = 100;
const MAX_INTERVAL_MS = 30000;
const MAX_NUMBER_COUNT = 100;

/** Bir sınıfta aynı anda duran en fazla ödev; liste öğrencinin Görevler ekranını doldurmasın. */
const MAX_HOMEWORKS_PER_CLASS = 30;
/** Seçili öğrencilere verilen ödevde en fazla öğrenci (sınıf sınırıyla aynı). */
const MAX_ASSIGNED = 60;

function fail(code, message) {
  return { ok: false, code, message };
}

/** Abaküs 5 sütun; abaküssüzde sayılar ve sonuç 8 basamağa kadar (HomeworkEngine.capacityDigits). */
function capacityDigits(withAbacus) {
  return withAbacus ? 5 : 8;
}

function isInt(v) {
  return Number.isInteger(v);
}

/**
 * Gelişmiş ayar: anahtar → rakam listesi ({ five: [3, 4] }, çarpmada { "0": [2, 5] }). Yalnızca
 * izin verilen anahtarlar ve [min, max] aralığındaki tam sayılar kalıyor; boş liste atılıyor.
 * Kuralın hangi rakamlara sahip olduğu (5'likte 1–4 gibi) motorda; istemci okurken ayrıca
 * temizliyor (HomeworkEngine.normalized).
 * @returns {object|null} temiz kopya ya da hiç giriş yoksa null
 */
function normalizeDigitMap(raw, allowedKeys, min, max) {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return null;
  const out = {};
  for (const key of allowedKeys) {
    const list = raw[key];
    if (!Array.isArray(list)) continue;
    const digits = [...new Set(list.filter((d) => isInt(d) && d >= min && d <= max))].sort((a, b) => a - b);
    if (digits.length) out[key] = digits;
  }
  return Object.keys(out).length ? out : null;
}

/** Çarpmada basamak anahtarları: "0" birler … En büyük basamakta 0 olamaz. */
function normalizeMulDigits(raw, digits) {
  const keys = Array.from({ length: digits }, (_, i) => String(i));
  const out = normalizeDigitMap(raw, keys, 0, 9);
  if (!out) return null;
  const lead = String(digits - 1);
  if (out[lead]) {
    out[lead] = out[lead].filter((d) => d !== 0);
    if (!out[lead].length) delete out[lead];
  }
  return Object.keys(out).length ? out : null;
}

/**
 * İstemciden gelen ayarları doğrular ve yalnızca bilinen alanları içeren temiz bir kopya döndürür.
 */
function normalizeConfig(raw) {
  const c = raw && typeof raw === 'object' ? raw : {};
  const op = c.op;
  if (!OPS.includes(op)) return fail('invalid-argument', 'İşlem seçilmedi.');
  const withAbacus = c.withAbacus === true;
  const cap = capacityDigits(withAbacus);
  const questionCount = c.questionCount;
  if (!isInt(questionCount) || questionCount < MIN_QUESTIONS || questionCount > MAX_QUESTIONS) {
    return fail('invalid-argument', `Soru adedi ${MIN_QUESTIONS}–${MAX_QUESTIONS} arasında olmalı.`);
  }

  if (op === 'mul') {
    const { digits, digits2 } = c;
    if (!isInt(digits) || !isInt(digits2) || digits < 1 || digits2 < 1 || digits + digits2 > cap) {
      return fail('invalid-argument', `Çarpmada iki sayının basamakları toplamı en fazla ${cap} olabilir.`);
    }
    const config = { op, withAbacus, digits, digits2, questionCount };
    const mulDigits1 = normalizeMulDigits(c.mulDigits1, digits);
    const mulDigits2 = normalizeMulDigits(c.mulDigits2, digits2);
    if (mulDigits1) config.mulDigits1 = mulDigits1;
    if (mulDigits2) config.mulDigits2 = mulDigits2;
    return { ok: true, config };
  }

  const rules = Array.isArray(c.rules) ? [...new Set(c.rules)] : [];
  if (!rules.length || rules.some((r) => !RULES.includes(r))) {
    return fail('invalid-argument', 'En az bir konu seçilmeli.');
  }
  const { digits, numberCount, intervalMs } = c;
  if (!isInt(digits) || digits < 1 || digits > cap) {
    return fail('invalid-argument', `Basamak sayısı 1–${cap} arasında olmalı.`);
  }
  const minCount = op === 'sub' ? 1 : 2;
  if (!isInt(numberCount) || numberCount < minCount || numberCount > MAX_NUMBER_COUNT) {
    return fail('invalid-argument', `Sayı adedi ${minCount}–${MAX_NUMBER_COUNT} arasında olmalı.`);
  }
  if (!isInt(intervalMs) || intervalMs < MIN_INTERVAL_MS || intervalMs > MAX_INTERVAL_MS) {
    return fail('invalid-argument', 'Gösterilme hızı 0,1–30 saniye arasında olmalı.');
  }
  // Kural sırası sabit: aynı ödev her yerde aynı başlıkla görünsün.
  const ordered = RULES.filter((r) => rules.includes(r));
  const config = { op, rules: ordered, withAbacus, digits, numberCount, intervalMs, questionCount };
  const ruleDigits = normalizeDigitMap(c.ruleDigits, ordered, 1, 9);
  if (ruleDigits) config.ruleDigits = ruleDigits;
  return { ok: true, config };
}

function blockCount(questionCount) {
  return Math.ceil(questionCount / BLOCK_SIZE);
}

/**
 * Kime verildiği: `wholeClass: true` ya da `studentUids`. Üyelik denetimi index.js'te (members
 * okunarak); burada yalnızca biçim.
 */
function normalizeTarget(raw) {
  const t = raw && typeof raw === 'object' ? raw : {};
  if (t.wholeClass === true) return { ok: true, wholeClass: true, studentUids: [] };
  const uids = Array.isArray(t.studentUids)
    ? [...new Set(t.studentUids.filter((u) => typeof u === 'string' && u && !u.includes('/')))]
    : [];
  if (!uids.length) return fail('invalid-argument', 'Ödevi verecek öğrenci seçilmedi.');
  if (uids.length > MAX_ASSIGNED) return fail('invalid-argument', 'Çok fazla öğrenci seçildi.');
  return { ok: true, wholeClass: false, studentUids: uids };
}

/** Bildirim metni için kısa özet: "10'luk toplama", "2 × 1 basamak çarpma". */
const RULE_LABELS = {
  add: { direct: 'kuralsız', five: "5'lik", ten: "10'luk", bead: 'boncuk' },
  sub: { direct: 'kuralsız', five: "5'lik", ten: "10'luk", bead: 'boncuk' },
};

function homeworkSummary(config) {
  if (config.op === 'mul') return `${config.digits} × ${config.digits2} basamak çarpma`;
  const labels = config.rules.map((r) => RULE_LABELS[config.op][r]).join(', ');
  return `${labels} ${config.op === 'add' ? 'toplama' : 'çıkarma'}`;
}

/** Bitirenlerden hedefte olanların sayısı (sınıftan ayrılanlar sayılmaz). */
function countDone(doneUids, targetUids) {
  const target = new Set(targetUids);
  return [...new Set(doneUids)].filter((u) => target.has(u)).length;
}

/**
 * Öğretmene "öğrenci ödevi bitirdi" bildirimi. İstemci ödev başına tek bildirim tutuyor, yani
 * gövde her bitirişte güncel toplamı söylüyor.
 */
function doneNoticeText({ studentName, summary, correct, answered, doneCount, targetCount }) {
  const title = `Ödev tamamlandı: ${summary}`;
  const parts = [`${studentName} bitirdi`];
  if (answered > 0) parts.push(`${correct}/${answered} doğru`);
  if (targetCount > 0) parts.push(`${doneCount}/${targetCount} öğrenci bitirdi`);
  return { title, body: parts.join(' · ') };
}

/**
 * Adımın kural sayımı (istemci RuleAnalysis.kt): { "add_five": { c, w }, "mulA_2x1": { c, w } … }.
 * Öğrencinin istatistiğine eklenecek değerler; bilinmeyen anahtar ve saçma sayı atılıyor. Bir adım
 * en çok 10 soru; toplama/çıkarmada her soru en çok 100 sayı × 8 basamak işlem, yani anahtar başına
 * 10 × 100 × 8 üst sınır.
 * @returns {{ key: string, c: number, w: number }[]}
 */
const RULE_TALLY_KEY = /^(?:(?:add|sub)_(?:direct|five|ten|bead)|mul[AM]_[1-8]x[1-8])$/;
const RULE_TALLY_MAX = 10 * 100 * 8;
function sanitizeRuleTally(tally) {
  if (!tally || typeof tally !== 'object' || Array.isArray(tally)) return [];
  const out = [];
  for (const [key, v] of Object.entries(tally).slice(0, 60)) {
    if (!RULE_TALLY_KEY.test(key) || !v || typeof v !== 'object') continue;
    const c = Number.isInteger(v.c) ? Math.min(Math.max(v.c, 0), RULE_TALLY_MAX) : 0;
    const w = Number.isInteger(v.w) ? Math.min(Math.max(v.w, 0), RULE_TALLY_MAX) : 0;
    if (c + w > 0) out.push({ key, c, w });
  }
  return out;
}

/**
 * KURAL USTALIĞI — ödev başarısı ekranındaki aşama: 0 başladı, 1 gelişiyor, 2 usta. Son ~50
 * işleme bakılıyor (toplam oran zamanla düzleşip ilerlemeyi göstermiyordu); aşama hiç geri
 * düşmüyor (kullanıcı kararı 26.10.2026). İstemci aynı eşikleri kullanıyor (HomeworkRuleStatsFragment).
 */
const MASTERY_WINDOW = 50;
const MASTERY_IMPROVING_PCT = 70;
const MASTERY_MASTER_PCT = 90;

function masteryStage(c, w) {
  const total = c + w;
  if (total < MASTERY_WINDOW) return 0;
  const pct = (c * 100) / total;
  if (pct >= MASTERY_MASTER_PCT) return 2;
  if (pct >= MASTERY_IMPROVING_PCT) return 1;
  return 0;
}

function sumCw(list) {
  return list.reduce((acc, x) => ({ c: acc.c + x.c, w: acc.w + x.w }), { c: 0, w: 0 });
}

/**
 * Son işlemler penceresi: adım adım sayımlar, en yenisi sonda. Adımın içindeki sıra bilinmediği
 * için pencere adım sınırında kesiliyor: en eski adım, kalanlar hâlâ 50 işlemi karşılıyorsa düşüyor.
 */
function pushRecent(recent, c, w) {
  const out = (Array.isArray(recent) ? recent : [])
    .filter((x) => x && isInt(x.c) && isInt(x.w))
    .map((x) => ({ c: x.c, w: x.w }));
  out.push({ c, w });
  while (out.length > 1) {
    const rest = sumCw(out.slice(1));
    if (rest.c + rest.w < MASTERY_WINDOW) break;
    out.shift();
  }
  return out;
}

/**
 * Bir adımın kural sayımını öğrencinin istatistiğine uygular; yalnızca değişen anahtarlar döner
 * (merge ile yazılıyor). Penceresi olmayan eski anahtar (pencereden önceki veriler) toplamıyla
 * başlıyor: eski çalışma ustalıkta kaybolmasın.
 * @param prev homeworkRuleStats/{uid} dokümanı ({ tally, recent, level })
 * @param tally sanitizeRuleTally çıktısı
 */
function applyBlockToStats(prev, tally) {
  const p = prev && typeof prev === 'object' ? prev : {};
  const out = { tally: {}, recent: {}, level: {} };
  for (const { key, c, w } of tally) {
    const old = (p.tally && p.tally[key]) || {};
    const oc = isInt(old.c) ? old.c : 0;
    const ow = isInt(old.w) ? old.w : 0;
    out.tally[key] = { c: oc + c, w: ow + w };
    const prevRecent = p.recent && Array.isArray(p.recent[key]) ? p.recent[key]
      : (oc + ow > 0 ? [{ c: oc, w: ow }] : []);
    const recent = pushRecent(prevRecent, c, w);
    out.recent[key] = recent;
    const s = sumCw(recent);
    const oldLevel = p.level && isInt(p.level[key]) ? p.level[key] : 0;
    out.level[key] = Math.max(oldLevel, masteryStage(s.c, s.w));
  }
  return out;
}

/** Günlük kayıt anahtarı Türkiye saatiyle (UTC+3; 2016'dan beri yaz saati yok): { month: '2026-10', day: '26' }. */
function trDayKey(ms) {
  const iso = new Date(ms + 3 * 3600 * 1000).toISOString();
  return { month: iso.slice(0, 7), day: iso.slice(8, 10) };
}

/** Adımda çözülen soru: ilerlemenin önceki hâlinden farkı; bir adım en çok BLOCK_SIZE soru. */
function blockQuestionCounts(before, after) {
  const clamp = (v) => (Number.isInteger(v) ? Math.min(Math.max(v, 0), BLOCK_SIZE) : 0);
  const answered = clamp((Number(after.answered) || 0) - (Number(before.answered) || 0));
  const correct = Math.min(clamp((Number(after.correct) || 0) - (Number(before.correct) || 0)), answered);
  return { c: correct, w: answered - correct };
}

module.exports = {
  MASTERY_WINDOW,
  masteryStage,
  pushRecent,
  applyBlockToStats,
  trDayKey,
  blockQuestionCounts,
  sanitizeRuleTally,
  countDone,
  doneNoticeText,
  BLOCK_SIZE,
  MAX_QUESTIONS,
  MAX_HOMEWORKS_PER_CLASS,
  MAX_ASSIGNED,
  capacityDigits,
  normalizeConfig,
  normalizeTarget,
  blockCount,
  homeworkSummary,
};

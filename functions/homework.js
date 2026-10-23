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
    return { ok: true, config: { op, withAbacus, digits, digits2, questionCount } };
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
  return { ok: true, config: { op, rules: ordered, withAbacus, digits, numberCount, intervalMs, questionCount } };
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

module.exports = {
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

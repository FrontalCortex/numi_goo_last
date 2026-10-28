'use strict';

/**
 * KURAL ANLATICI (sunucu kopyası) — abaküste bir toplama/çıkarmanın hangi boncuk hamleleriyle
 * yapıldığını, derslerde öğretilen sırayla çıkarır.
 *
 * Asıl yazım ve gerekçesi: app/.../abacus/StepExplainer.kt (başlık yorumu). Burada aynı mantığın
 * JavaScript'i var, çünkü "Sobi'ye sor" yardımcısının araçları sunucuda çalışacak
 * (docs/ANLAMADIM_YARDIM_PLANI.md): dil modeli hamle hesaplamıyor, bunu çağırıyor.
 *
 * İKİ KOPYA AYRIŞMASIN DİYE
 *   Kotlin testi shared/step_explainer_cases.json tablosunu üretiyor; bu dosya aynı tabloyla
 *   sınanıyor (npm run test:step-explainer). Kotlin'de sıra, rol ya da encode biçimi değişirse
 *   tablo yeniden üretilir ve burası da aynı çıktıyı verecek şekilde düzeltilir.
 *
 * Sınıflandırma HomeworkEngine.kt → AbacusMoves ile aynı; kural anahtarları homework.js RULES ile
 * aynı ('direct', 'five', 'ten', 'bead').
 */

const DEFAULT_CAPACITY = 5;

/** Adımın derste söylenen adı; StepExplainer.Role anahtarlarıyla aynı. */
const ROLES = ['direct', 'five', 'friend', 'big_friend', 'beads', 'ten'];

/** Tek sütun hareketinin kuralı. d: sütunun değeri (0..9), x: eklenen rakam (1..9). */
function classifyAdd(d, x) {
  const h = Math.floor(d / 5);
  const e = d % 5;
  if (d + x <= 9) {
    return h + Math.floor(x / 5) <= 1 && e + (x % 5) <= 4 ? 'direct' : 'five';
  }
  // Taşma: sola +1, bu sütundan (10 − x) çıkar. Çıkarma doğrudansa 10'luk, değilse boncuk.
  const b = 10 - x;
  return Math.floor(b / 5) <= h && b % 5 <= e ? 'ten' : 'bead';
}

/** d: sütunun değeri (0..9), x: çıkarılan rakam (1..9). */
function classifySub(d, x) {
  const h = Math.floor(d / 5);
  const e = d % 5;
  if (d >= x) {
    return Math.floor(x / 5) <= h && x % 5 <= e ? 'direct' : 'five';
  }
  // Ödünç: soldan −1, bu sütuna (10 − x) ekle.
  const b = 10 - x;
  return h + Math.floor(b / 5) <= 1 && e + (b % 5) <= 4 ? 'ten' : 'bead';
}

/** Sayı → rakamlar (0 = birler). */
function digitsOf(n, size) {
  const out = new Array(size);
  let v = n;
  for (let i = 0; i < size; i++) {
    out[i] = v % 10;
    v = Math.floor(v / 10);
  }
  return out;
}

function valueOf(state) {
  let v = 0;
  for (let i = state.length - 1; i >= 0; i--) v = v * 10 + state[i];
  return v;
}

/**
 * [state]'in [col] sütununa [x] ekler/çıkarır, adımları sırayla uygulayarak. Elde adımı sol
 * sütunu kendi hareketiyle (özyinelemeli) işler. Sıra StepExplainer.kt → move ile aynı.
 */
function move(state, op, col, x, isCarry) {
  if (col >= state.length) throw new Error(`Abaküs taştı: col=${col}`);
  const d = state[col];
  const valueBefore = valueOf(state);
  const rule = op === 'add' ? classifyAdd(d, x) : classifySub(d, x);
  const steps = [];

  const beads = (role, upper, lower) => {
    state[col] += upper * 5 + lower;
    steps.push({ role, col, upper, lower });
  };
  const ten = () => {
    steps.push({ role: 'ten', col: col + 1, upper: 0, lower: 0, carry: move(state, op, col + 1, 1, true) });
  };

  const b = 10 - x;
  if (op === 'add') {
    switch (rule) {
      case 'direct': beads('direct', Math.floor(x / 5), x % 5); break;
      case 'five': beads('five', 1, 0); beads('friend', 0, -(5 - x)); break;
      case 'ten': ten(); beads('big_friend', -Math.floor(b / 5), -(b % 5)); break;
      case 'bead': beads('beads', 0, x - 5); beads('five', -1, 0); ten(); break;
    }
  } else {
    switch (rule) {
      case 'direct': beads('direct', -Math.floor(x / 5), -(x % 5)); break;
      case 'five': beads('five', -1, 0); beads('friend', 0, 5 - x); break;
      case 'ten': ten(); beads('big_friend', Math.floor(b / 5), b % 5); break;
      case 'bead': ten(); beads('five', 1, 0); beads('friend', 0, -(x - 5)); break;
    }
  }
  return { op, col, x, digitBefore: d, rule, isCarry, steps, valueBefore, valueAfter: valueOf(state) };
}

/**
 * [base] abaküsteyken [number] eklenir ('add') ya da çıkarılır ('sub'). Sonuç abaküse
 * sığmıyorsa ya da girdi geçersizse null.
 */
function explain(base, op, number, capacity = DEFAULT_CAPACITY) {
  if (op !== 'add' && op !== 'sub') throw new Error(`op=${op}`);
  if (!Number.isInteger(capacity) || capacity < 1 || capacity > 15) throw new Error(`capacity=${capacity}`);
  const max = 10 ** capacity - 1;
  const valid = (n) => Number.isInteger(n) && n >= 0 && n <= max;
  if (!valid(base) || !valid(number)) return null;
  const result = op === 'add' ? base + number : base - number;
  if (!valid(result)) return null;

  const state = digitsOf(base, capacity);
  const digits = digitsOf(number, capacity);
  const moves = [];
  for (let col = capacity - 1; col >= 0; col--) {
    if (digits[col] !== 0) moves.push(move(state, op, col, digits[col], false));
  }
  if (valueOf(state) !== result) throw new Error(`${base} ${op} ${number} → ${valueOf(state)}`);
  return { op, base, number, result, moves };
}

/** Abaküs [base]'den [target]'a: fark kadar toplama ya da çıkarma. */
function explainTransition(base, target, capacity = DEFAULT_CAPACITY) {
  return target >= base
    ? explain(base, 'add', target - base, capacity)
    : explain(base, 'sub', base - target, capacity);
}

/** Hareketin kullandığı kurallar, elde zinciri dahil; eldenin doğrudan ±1'i kural sayılmaz. */
function rulesUsed(m) {
  const out = new Set();
  const walk = (mv) => {
    if (!(mv.isCarry && mv.rule === 'direct')) out.add(mv.rule);
    for (const s of mv.steps) if (s.carry) walk(s.carry);
  };
  walk(m);
  return [...out];
}

const signed = (n) => (n > 0 ? `+${n}` : `${n}`);

function encodeMove(m) {
  const steps = m.steps.map((s) => {
    if (s.carry) return `ten{${encodeMove(s.carry)}}`;
    let t = `${s.role}@${s.col}`;
    if (s.upper !== 0) t += `u${signed(s.upper)}`;
    if (s.lower !== 0) t += `l${signed(s.lower)}`;
    return t;
  });
  return `c${m.col}${m.op === 'add' ? '+' : '-'}${m.x}=${m.rule}[${steps.join(' ')}]>${m.valueAfter}`;
}

/** Tablo biçimi; StepExplainer.encode ile birebir aynı. */
function encode(e) {
  return e ? e.moves.map(encodeMove).join(' ') : 'error';
}

module.exports = {
  DEFAULT_CAPACITY,
  ROLES,
  classifyAdd,
  classifySub,
  explain,
  explainTransition,
  rulesUsed,
  encode,
};

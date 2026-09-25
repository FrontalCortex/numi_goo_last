/**
 * Kupa yolu sandık nadirliği ve son eşik kuralının testi.
 *
 * Fonksiyonlar index.js'ten OKUNARAK çalıştırılıyor, kopyalanmıyor: kopya test, kaynak
 * değişince sessizce yanlışlamaya başlar.
 */
const fs = require('fs');
const path = require('path');

const src = fs.readFileSync(path.join(__dirname, '..', 'index.js'), 'utf8');

function extract(name, kind) {
  const re = kind === 'const'
    ? new RegExp(`const ${name} = [^;]+;`)
    : new RegExp(`function ${name}\\([\\s\\S]*?\\n\\}`);
  const m = src.match(re);
  if (!m) throw new Error(`index.js icinde bulunamadi: ${name}`);
  return m[0];
}

const ctx = new Function(`
  ${extract('CUP_PATH_START', 'const')}
  ${extract('CUP_PATH_STEP', 'const')}
  ${extract('CUP_PATH_MAX', 'const')}
  ${extract('cupPathChestRarity')}
  ${extract('nextCupPathMilestone')}
  ${extract('isCupPathMilestone')}
  return { CUP_PATH_START, CUP_PATH_STEP, CUP_PATH_MAX,
           cupPathChestRarity, nextCupPathMilestone, isCupPathMilestone };
`)();

let passed = 0;
let failed = 0;
function check(name, fn) {
  try {
    fn();
    passed++;
  } catch (e) {
    failed++;
    console.error(`  BASARISIZ: ${name}\n    ${e.message}`);
  }
}
function eq(actual, expected, what) {
  if (actual !== expected) throw new Error(`${what}: beklenen ${expected}, gelen ${actual}`);
}

const { cupPathChestRarity: rarity, nextCupPathMilestone: next, isCupPathMilestone: valid,
        CUP_PATH_MAX: MAX, CUP_PATH_START: START, CUP_PATH_STEP: STEP } = ctx;

check('kullanicinin verdigi ornekler', () => {
  [300, 400, 600, 700, 900, 1100].forEach((m) => eq(rarity(m), 'COMMON', `${m}`));
  [500, 1500, 2500, 3500].forEach((m) => eq(rarity(m), 'RARE', `${m}`));
  [1000, 2000, 3000, 10000].forEach((m) => eq(rarity(m), 'EPIC', `${m}`));
});

check('1000 hem 500 hem 1000 katı: destansi kazaniyor', () => {
  eq(rarity(1000), 'EPIC', '1000');
  eq(rarity(5000), 'EPIC', '5000');
});

check('300..10000 arasindaki HER esik tek bir kurala uyuyor', () => {
  for (let m = START + STEP; m <= MAX; m += STEP) {
    const expected = m % 1000 === 0 ? 'EPIC' : m % 500 === 0 ? 'RARE' : 'COMMON';
    eq(rarity(m), expected, `${m}`);
  }
});

check('dagilim: 98 esikte 10 destansi, 10 ender, 78 siradan', () => {
  const counts = { COMMON: 0, RARE: 0, EPIC: 0 };
  for (let m = START + STEP; m <= MAX; m += STEP) counts[rarity(m)]++;
  eq(counts.EPIC, 10, 'destansi');
  eq(counts.RARE, 10, 'ender');
  eq(counts.COMMON, 78, 'siradan');
  eq(counts.COMMON + counts.RARE + counts.EPIC, 98, 'toplam esik');
});

check('son esik 10000, sonrasi yok', () => {
  eq(valid(10000), true, 'isCupPathMilestone(10000)');
  eq(valid(10100), false, 'isCupPathMilestone(10100)');
  eq(valid(20000), false, 'isCupPathMilestone(20000)');
  eq(next(9900), 10000, 'next(9900)');
  eq(next(10000), 0, 'next(10000) yol bitti');
  eq(next(12345), 0, 'next(12345) yol bitti');
});

check('esik dogrulamasi eski kurallari korumaya devam ediyor', () => {
  eq(valid(200), false, 'baslangic esik degil');
  eq(valid(350), false, '100un kati olmayan');
  eq(valid(-100), false, 'negatif');
  eq(valid(300.5), false, 'ondalik');
  eq(valid(NaN), false, 'NaN');
});

check('ilk esik hala 300', () => {
  eq(next(START), 300, 'next(200)');
  eq(next(0), 300, 'next(0)');
});

console.log(`\n${passed} gecti, ${failed} basarisiz`);
process.exit(failed === 0 ? 0 : 1);

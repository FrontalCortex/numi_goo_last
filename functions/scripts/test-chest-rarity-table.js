/**
 * Sandık nadirlik tablolarının testi.
 *
 * NEDEN VAR
 *   [rollRarityUpgrade]'in üstündeki yorum bir kez koddan ayrı düştü: EFSANEVİ şansı
 *   %10 yazıyordu, kod %5 veriyordu. Yorum kimseyi uyarmadığı için fark aylarca
 *   görülmedi. Bu test tabloyu KODDAN türetip yorumdaki sayılarla karşılaştırıyor;
 *   eşikler değişip yorum güncellenmezse düşüyor.
 *
 * ÇALIŞTIRMA: node functions/scripts/test-chest-rarity-table.js
 */
const fs = require('fs');
const path = require('path');

const src = fs.readFileSync(path.join(__dirname, '..', 'index.js'), 'utf8');

function extract(re, what) {
  const m = src.match(re);
  if (!m) throw new Error(`index.js icinde bulunamadi: ${what}`);
  return m[0];
}

const TAPS = Number(src.match(/const CHEST_TAP_COUNT = (\d+)/)[1]);
const RARITIES = ['COMMON', 'RARE', 'EPIC', 'LEGENDARY'];

// Zar disaridan sabitleniyor; boylece gecis tablosu tahminle degil, 1..100'un her
// degerini gercek fonksiyona vererek cikariliyor.
const dice = { value: 1 };
const rollRarityUpgrade = new Function(
  'DICE',
  `${extract(/function rollInt\([\s\S]*?\n\}/, 'rollInt')
    .replace('return min + Math.floor(Math.random() * (max - min + 1));', 'return DICE.value;')}
   ${extract(/function rollRarityUpgrade\([\s\S]*?\n\}/, 'rollRarityUpgrade')}
   return rollRarityUpgrade;`
)(dice);

/** Tek dokunusun gecis tablosu: yuzde olarak (zar 1..100 oldugu icin adet = yuzde). */
const step = {};
for (const from of RARITIES) {
  step[from] = {};
  for (let d = 1; d <= 100; d++) {
    dice.value = d;
    const to = rollRarityUpgrade(from);
    step[from][to] = (step[from][to] || 0) + 1;
  }
}

/** n dokunus sonrasindaki dagilim (yuzde). */
function after(start, n) {
  let dist = { [start]: 100 };
  for (let i = 0; i < n; i++) {
    const next = {};
    for (const from of Object.keys(dist)) {
      for (const to of RARITIES) {
        const p = step[from][to] || 0;
        if (p > 0) next[to] = (next[to] || 0) + (dist[from] * p) / 100;
      }
    }
    dist = next;
  }
  return dist;
}

let passed = 0;
let failed = 0;
function check(name, fn) {
  try { fn(); passed++; }
  catch (e) { failed++; console.error(`  BASARISIZ: ${name}\n    ${e.message}`); }
}
function eq(actual, expected, what) {
  if (actual !== expected) throw new Error(`${what}: beklenen ${expected}, gelen ${actual}`);
}
function near(actual, expected, what, tol = 0.01) {
  if (Math.abs(actual - expected) > tol) {
    throw new Error(`${what}: beklenen ~${expected}, gelen ${actual.toFixed(3)}`);
  }
}

check('dokunus sayisi 3', () => eq(TAPS, 3, 'CHEST_TAP_COUNT'));

// index.js'teki "Tek dokunus" tablosu
check('tek dokunus tablosu yorumla ayni', () => {
  eq(step.COMMON.COMMON, 80, 'SIRADAN -> SIRADAN');
  eq(step.COMMON.RARE, 15, 'SIRADAN -> ENDER');
  eq(step.COMMON.EPIC, 5, 'SIRADAN -> DESTANSI');
  eq(step.RARE.RARE, 80, 'ENDER -> ENDER');
  eq(step.RARE.EPIC, 15, 'ENDER -> DESTANSI');
  eq(step.RARE.LEGENDARY, 5, 'ENDER -> EFSANEVI');
  eq(step.EPIC.EPIC, 95, 'DESTANSI -> DESTANSI');
  eq(step.EPIC.LEGENDARY, 5, 'DESTANSI -> EFSANEVI');
  eq(step.LEGENDARY.LEGENDARY, 100, 'EFSANEVI -> EFSANEVI');
});

check('nadirlik hic dusmuyor', () => {
  for (let i = 0; i < RARITIES.length; i++) {
    for (let j = 0; j < i; j++) {
      eq(step[RARITIES[i]][RARITIES[j]] || 0, 0, `${RARITIES[i]} -> ${RARITIES[j]}`);
    }
  }
});

check('tek dokunusta iki basamak atlanamiyor', () => {
  eq(step.COMMON.LEGENDARY || 0, 0, 'SIRADAN -> EFSANEVI');
});

// index.js'teki "3 dokunusun sonunda" tablosu
check('SIRADAN baslayan sandigin dagilimi', () => {
  const d = after('COMMON', TAPS);
  near(d.COMMON, 51.2, 'SIRADAN');
  near(d.RARE, 28.8, 'ENDER');
  near(d.EPIC, 17.25, 'DESTANSI');
  near(d.LEGENDARY, 2.75, 'EFSANEVI');
});

check('ENDER baslayan sandigin dagilimi', () => {
  const d = after('RARE', TAPS);
  eq(d.COMMON || 0, 0, 'SIRADAN');
  near(d.RARE, 51.2, 'ENDER');
  near(d.EPIC, 34.54, 'DESTANSI');
  near(d.LEGENDARY, 14.26, 'EFSANEVI');
});

check('DESTANSI baslayan sandigin dagilimi', () => {
  const d = after('EPIC', TAPS);
  eq(d.RARE || 0, 0, 'ENDER');
  near(d.EPIC, 85.74, 'DESTANSI');
  near(d.LEGENDARY, 14.26, 'EFSANEVI');
});

check('EFSANEVI baslayan sandik tavanda kaliyor', () => {
  near(after('LEGENDARY', TAPS).LEGENDARY, 100, 'EFSANEVI');
});

check('1000 katindaki esik EFSANEVI sansini bes katina cikariyor', () => {
  const siradan = after('COMMON', TAPS).LEGENDARY;
  const destansi = after('EPIC', TAPS).LEGENDARY;
  near(destansi / siradan, 5.186, 'kat', 0.01);
});

check('her dagilimin toplami 100', () => {
  for (const start of RARITIES) {
    const d = after(start, TAPS);
    near(RARITIES.reduce((a, t) => a + (d[t] || 0), 0), 100, `${start} toplam`, 1e-9);
  }
});

console.log('\n--- TEK DOKUNUS ---');
for (const from of RARITIES) {
  console.log(`  ${from.padEnd(10)} -> ` +
    RARITIES.filter((t) => step[from][t]).map((t) => `${t} %${step[from][t]}`).join(', '));
}
console.log(`\n--- ${TAPS} DOKUNUS SONRASI ---`);
for (const start of RARITIES) {
  const d = after(start, TAPS);
  console.log(`  ${start.padEnd(10)} baslar -> ` +
    RARITIES.filter((t) => d[t]).map((t) => `${t} %${d[t].toFixed(2)}`).join(' | '));
}

console.log(`\n${passed} gecti, ${failed} basarisiz`);
process.exit(failed === 0 ? 0 : 1);

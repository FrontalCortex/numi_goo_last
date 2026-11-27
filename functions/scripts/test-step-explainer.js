/**
 * Kural anlatıcısının sunucu kopyası testi (bkz. sobi/stepExplainer.js).
 *
 * NEDEN VAR
 *   Aynı mantığın Kotlin (uygulama) ve JS (sunucu) kopyası var. Ayrışırlarsa "Bana göster"
 *   animasyonu bir şey, Sobi başka bir şey anlatır. Bu test Kotlin'in ürettiği ortak tabloyu
 *   (shared/step_explainer_cases.json) okuyup her satırda aynı çıktıyı bekliyor; ayrıca boncuk
 *   düzeyinde bağımsız bir simülasyonla sonuçları denetliyor.
 *
 * ÇALIŞTIRMA: cd functions && npm run test:step-explainer
 */
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const s = require('../sobi/stepExplainer');

let passed = 0;
function test(name, fn) {
  fn();
  passed++;
  console.log(`  ✓ ${name}`);
}

console.log('ders örnekleri');
test('8 + 6: 1 boncuk gelir, 5 gider, 10 gelir', () => assert.strictEqual(
  s.encode(s.explain(8, 'add', 6)),
  'c0+6=bead[beads@0l+1 five@0u-1 ten{c1+1=direct[direct@1l+1]>14}]>14',
));
test('23 − 6: 10 gider, 5 gelir, 1 gider', () => assert.strictEqual(
  s.encode(s.explain(23, 'sub', 6)),
  'c0-6=bead[ten{c1-1=direct[direct@1l-1]>13} five@0u+1 friend@0l-1]>17',
));
test('49 + 1: elde 5\'lik kuralla', () => assert.deepStrictEqual(
  s.rulesUsed(s.explain(49, 'add', 1).moves[0]),
  ['ten', 'five'],
));
test('sığmayan işlem null', () => {
  assert.strictEqual(s.explain(99999, 'add', 1), null);
  assert.strictEqual(s.explain(0, 'sub', 1), null);
  assert.strictEqual(s.explain(1.5, 'add', 1), null);
});
test('geçiş hedefe göre', () => assert.strictEqual(
  s.encode(s.explainTransition(68, 144)),
  s.encode(s.explain(68, 'add', 76)),
));

console.log('ortak tablo');
const tablePath = path.join(__dirname, '..', '..', 'shared', 'step_explainer_cases.json');
const rows = JSON.parse(fs.readFileSync(tablePath, 'utf8'));
test(`${rows.length} satır Kotlin çıktısıyla aynı`, () => {
  assert.ok(rows.length > 1000);
  for (const r of rows) {
    assert.strictEqual(s.encode(s.explain(r.base, r.op, r.number)), r.plan, `${r.base} ${r.op} ${r.number}`);
  }
});

console.log('bağımsız boncuk simülasyonu');
/** Her adımı beşlik/birlik boncuk olarak oynatır; olmayan boncuk gidemez. */
function simulate(base, e, where) {
  const heaven = [];
  const earth = [];
  let v = base;
  for (let c = 0; c < s.DEFAULT_CAPACITY; c++) {
    const d = v % 10;
    v = Math.floor(v / 10);
    heaven.push(d >= 5);
    earth.push(d % 5);
  }
  const apply = (st) => {
    if (st.carry) { st.carry.steps.forEach(apply); return; }
    assert.ok(s.ROLES.includes(st.role), where);
    if (st.upper === 1) { assert.ok(!heaven[st.col], `${where}: beşlik zaten inik`); heaven[st.col] = true; }
    if (st.upper === -1) { assert.ok(heaven[st.col], `${where}: gidecek beşlik yok`); heaven[st.col] = false; }
    earth[st.col] += st.lower;
    assert.ok(earth[st.col] >= 0 && earth[st.col] <= 4, `${where}: birlik sınırı`);
  };
  e.moves.forEach((m) => m.steps.forEach(apply));
  let out = 0;
  for (let c = s.DEFAULT_CAPACITY - 1; c >= 0; c--) out = out * 10 + (heaven[c] ? 5 : 0) + earth[c];
  return out;
}
test('0..9999 tabanında tek rakam ekleme/çıkarma', () => {
  for (let base = 0; base <= 9999; base++) {
    for (let x = 1; x <= 9; x++) {
      for (const op of ['add', 'sub']) {
        for (const n of [x, x * 10]) {
          const e = s.explain(base, op, n);
          const expected = op === 'add' ? base + n : base - n;
          if (expected < 0) { assert.strictEqual(e, null); continue; }
          assert.strictEqual(simulate(base, e, `${base} ${op} ${n}`), expected);
        }
      }
    }
  }
});

console.log(`\n${passed} test geçti.`);

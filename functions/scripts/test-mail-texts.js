'use strict';
// E-posta metinleri (mailTexts.js): dil seçimi, üç dilde aynı onay maddeleri, doğru gizlilik sayfası.
// Kullanım: npm run test:mail
const assert = require('assert');
const m = require('../mailTexts');

let gecen = 0;
function test(ad, fn) { fn(); gecen++; console.log(`  ✓ ${ad}`); }
const TR_HARF = /[çğıöşüÇĞİÖŞÜ]/;

test('dil: tanınmayan ya da boş → Türkçe, "in" → Endonezce', () => {
  assert.strictEqual(m.mailLang(undefined), 'tr');
  assert.strictEqual(m.mailLang('de'), 'tr');
  assert.strictEqual(m.mailLang('EN'), 'en');
  assert.strictEqual(m.mailLang('in'), 'id');
  assert.strictEqual(m.mailLang('id'), 'id');
});

test('doğrulama kodu üç dilde, kod metinde', () => {
  for (const l of ['tr', 'en', 'id']) {
    const e = m.verificationEmail('482913', l);
    assert.ok(e.html.includes('482913') && e.text.includes('482913'), l);
  }
  for (const l of ['en', 'id']) assert.ok(!TR_HARF.test(JSON.stringify(m.verificationEmail('1', l))), l);
});

test('veli onayı: üç dilde aynı sayıda madde, kod ve destek adresi, dilin gizlilik sayfası', () => {
  const sayfa = { tr: 'privacy-policy-tr.html', en: 'privacy-policy.html', id: 'privacy-policy-id.html' };
  const n = m.PARENTAL.tr.collected.length;
  for (const l of ['tr', 'en', 'id']) {
    assert.strictEqual(m.PARENTAL[l].collected.length, n, `${l} madde sayısı`);
    const e = m.parentalConsentEmail('123456', 'kid@x.com', 'mom@x.com', 'destek@x.com', l);
    for (const parca of ['123456', 'destek@x.com', `${sayfa[l]}#pp-5`, 'kid@x.com']) {
      assert.ok(e.html.includes(parca) && e.text.includes(parca), `${l}: ${parca}`);
    }
  }
  for (const l of ['en', 'id']) {
    const e = m.parentalConsentEmail('1', 'a@x.com', 'b@x.com', 's@x.com', l);
    assert.ok(!TR_HARF.test(e.subject + e.html + e.text), l);
  }
});

test('veli onayı: çocuk ve veli adresi aynıysa "çocuğunuz sizin adresinizi verdi" denmiyor', () => {
  const ayni = m.parentalConsentEmail('1', 'x@x.com', 'x@x.com', 's@x.com', 'en');
  assert.ok(ayni.text.includes('with this email address') && !ayni.text.includes('gave your email'));
});

test('HTML kaçışı: e-postadaki < > işaretleri etiket olmuyor', () => {
  const e = m.parentalConsentEmail('1', 'a<b>@x.com', 'p@x.com', 's@x.com', 'id');
  assert.ok(e.html.includes('a&lt;b&gt;@x.com') && !e.html.includes('a<b>@'));
});

console.log(`\n${gecen} test geçti.`);

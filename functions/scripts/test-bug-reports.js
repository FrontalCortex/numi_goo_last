/**
 * Hata bildirimi çekirdeğinin karar mantığı testi (bkz. bugReports.js).
 *
 * NEDEN VAR
 *   Ödül gerçek parayla satılan şeyler içeriyor; iki kez ödül verme, başkasının görselini
 *   gösterme ya da sınırların delinmesi sahada fark edilmeden para kaybettirir.
 *
 * ÇALIŞTIRMA: cd functions && npm run test:bug-reports
 */
const assert = require('assert');
const {
  BUG_REPORT_TIERS,
  BUG_REPORT_MAX_PENDING,
  BUG_REPORT_DAILY_LIMIT,
  BUG_REPORT_TEXT_MAX,
  isOwnImagePath,
  sanitizeDevice,
  normalizeSubmission,
  submitDecision,
  reviewDecision,
  claimDecision,
} = require('../bugReports');

let passed = 0;
function test(name, fn) {
  fn();
  passed++;
  console.log(`  ✓ ${name}`);
}

const UID = 'abcDEF123';
const IMG = `bug_reports/${UID}_1760000000000.jpg`;
const TEXT = 'Ders ekranında buton kayboluyor.';

console.log('isOwnImagePath');
test('kendi dosyası kabul', () => assert.strictEqual(isOwnImagePath(IMG, UID), true));
test('başkasının dosyası red', () => assert.strictEqual(isOwnImagePath(IMG, 'other'), false));
test('önek benzerliği red (uid önekiyle başlayan başka uid)', () =>
  assert.strictEqual(isOwnImagePath(`bug_reports/${UID}x_1760000000000.jpg`, UID), false));
test('yol dışına çıkma red', () =>
  assert.strictEqual(isOwnImagePath(`bug_reports/${UID}_1/../../x.jpg`, UID), false));
test('başka klasör red', () =>
  assert.strictEqual(isOwnImagePath(`question_screenshots/${UID}_1760000000000.jpg`, UID), false));

console.log('normalizeSubmission');
test('geçerli gönderim', () => {
  const r = normalizeSubmission({ text: `  ${TEXT}  `, imagePath: IMG }, UID);
  assert.strictEqual(r.ok, true);
  assert.strictEqual(r.text, TEXT);
});
test('kısa metin red', () => assert.strictEqual(normalizeSubmission({ text: 'kısa', imagePath: IMG }, UID).ok, false));
test('uzun metin red', () =>
  assert.strictEqual(normalizeSubmission({ text: 'a'.repeat(BUG_REPORT_TEXT_MAX + 1), imagePath: IMG }, UID).ok, false));
test('görselsiz kabul (görsel isteğe bağlı)', () => {
  const r = normalizeSubmission({ text: TEXT }, UID);
  assert.strictEqual(r.ok, true);
  assert.strictEqual(r.imagePath, '');
});
test('boş görsel yolu görselsiz sayılır', () =>
  assert.strictEqual(normalizeSubmission({ text: TEXT, imagePath: '  ' }, UID).imagePath, ''));
test('başkasının görseli red', () =>
  assert.strictEqual(
    normalizeSubmission({ text: TEXT, imagePath: 'bug_reports/other_1760000000000.jpg' }, UID).ok,
    false
  ));
test('cihaz bilgisi süzülür', () => {
  const d = sanitizeDevice({ model: 'Pixel 8', sdk: 35, evil: 'x', locale: 'tr-TR'.repeat(10) });
  assert.deepStrictEqual(Object.keys(d).sort(), ['locale', 'model', 'sdk']);
  assert.strictEqual(d.sdk, '35');
  assert.ok(d.locale.length <= 16);
});

console.log('submitDecision');
test('ilk gönderim', () => {
  const r = submitDecision({}, 0, '2026-10-15');
  assert.deepStrictEqual(r.nextGuard, { dayKey: '2026-10-15', count: 1 });
});
test('bekleyen sınırı', () =>
  assert.strictEqual(submitDecision({}, BUG_REPORT_MAX_PENDING, '2026-10-15').ok, false));
test('günlük sınır', () =>
  assert.strictEqual(
    submitDecision({ dayKey: '2026-10-15', count: BUG_REPORT_DAILY_LIMIT }, 0, '2026-10-15').ok,
    false
  ));
test('yeni gün sayacı sıfırlar', () => {
  const r = submitDecision({ dayKey: '2026-10-14', count: BUG_REPORT_DAILY_LIMIT }, 0, '2026-10-15');
  assert.strictEqual(r.ok, true);
  assert.strictEqual(r.nextGuard.count, 1);
});

console.log('reviewDecision');
const pending = { uid: UID, status: 'pending' };
test('büyük ödülle onay', () => {
  const r = reviewDecision(pending, { decision: 'approve', tier: 'big' });
  assert.strictEqual(r.patch.status, 'approved');
  assert.deepStrictEqual(r.patch.reward, BUG_REPORT_TIERS.big);
  assert.strictEqual(r.patch.rewardClaimed, false);
});
test('bilinmeyen kademe red', () =>
  assert.strictEqual(reviewDecision(pending, { decision: 'approve', tier: 'huge' }).ok, false));
test('prototip anahtarı kademe sayılmaz', () =>
  assert.strictEqual(reviewDecision(pending, { decision: 'approve', tier: 'constructor' }).ok, false));
test('gerekçeyle ret', () => {
  const r = reviewDecision(pending, { decision: 'reject', rejectReason: 'duplicate', note: ' aynı ' });
  assert.strictEqual(r.patch.status, 'rejected');
  assert.strictEqual(r.patch.reviewNote, 'aynı');
});
test('gerekçesiz ret olmaz', () =>
  assert.strictEqual(reviewDecision(pending, { decision: 'reject' }).ok, false));
test('sonuçlanmış bildirim yeniden incelenemez', () =>
  assert.strictEqual(
    reviewDecision({ uid: UID, status: 'approved' }, { decision: 'approve', tier: 'big' }).ok,
    false
  ));

console.log('claimDecision');
const approved = { uid: UID, status: 'approved', reward: BUG_REPORT_TIERS.big, rewardClaimed: false };
test('sahibi toplar', () => assert.deepStrictEqual(claimDecision(approved, UID).reward, BUG_REPORT_TIERS.big));
test('başkası toplayamaz', () => assert.strictEqual(claimDecision(approved, 'other').ok, false));
test('ikinci kez toplanamaz', () =>
  assert.strictEqual(claimDecision(Object.assign({}, approved, { rewardClaimed: true }), UID).code, 'already-exists'));
test('bekleyen toplanamaz', () => assert.strictEqual(claimDecision(pending, UID).ok, false));
test('reddedilen toplanamaz', () =>
  assert.strictEqual(claimDecision({ uid: UID, status: 'rejected' }, UID).ok, false));

console.log(`\n${passed} test geçti.`);

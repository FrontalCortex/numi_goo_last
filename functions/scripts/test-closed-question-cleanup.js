/**
 * Kapanan soruların 30 gün sonra tamamen silinmesini test eder (runClosedQuestionCleanup).
 *
 * Kurallar:
 *   - `resolved` + `resolvedAt` 30 günden eski → silinir (mediaPurged false da true da).
 *   - `expired` + `creditRefundedAt` 30 günden eski → silinir.
 *   - 30 günden yeni kapananlar, açık sorular (`pending`, `claimed`) → kalır.
 *   - Bekleyen (pending) şikâyeti olan soru → kalır (kanıt).
 *
 * STORAGE BİLİNÇLİ OLARAK YAPILANDIRILMADI — bkz. test-delete-user-questions.js: medya
 * silme hata atıyor (`mediaFailed`), Firestore silmesi yine de tamamlanmalı.
 *
 * Emülatörde başka testlerin bıraktığı sorular olabilir; sayaçlar o yüzden kesin değil,
 * "en az" olarak kontrol ediliyor. Asıl kontrol, bu testin kendi sorularının durumu.
 *
 * ÇALIŞTIRMA (proje kökünden)
 *   firebase emulators:exec --only firestore --project numigo-new "node functions/scripts/test-closed-question-cleanup.js"
 */
const admin = require('firebase-admin');

if (!process.env.FIRESTORE_EMULATOR_HOST && process.env.ALLOW_PRODUCTION !== '1') {
  console.error('FIRESTORE_EMULATOR_HOST ayarlı değil; çıkılıyor.');
  process.exit(1);
}
// `firebase emulators:exec` ortama gerçek bucket adını ve CLI oturumunun kimliğini koyuyor;
// o zaman medya silme çağrıları CANLI Storage'a gidiyor (03.10.2026'da bir kez oldu, yollar
// sahte olduğu için hiçbir şey silinmedi). Test yalnızca Firestore emülatörüne dokunmalı.
delete process.env.FIREBASE_CONFIG;
delete process.env.GOOGLE_APPLICATION_CREDENTIALS;
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || 'numigo-new';

const fns = require('../index');
if (!admin.apps.length) admin.initializeApp({ projectId: process.env.GCLOUD_PROJECT });
const db = admin.firestore();
const { Timestamp } = admin.firestore;

const STAMP = Date.now();
const DAY = 24 * 60 * 60 * 1000;
const daysAgo = (n) => Timestamp.fromMillis(STAMP - n * DAY);

let failures = 0;
function check(label, actual, expected) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected);
  if (!ok) failures++;
  console.log(`${ok ? '  ✓' : '  ✗'} ${label}: ${JSON.stringify(actual)}` +
    (ok ? '' : ` (beklenen: ${JSON.stringify(expected)})`));
}
function checkAtLeast(label, actual, min) {
  const ok = actual >= min;
  if (!ok) failures++;
  console.log(`${ok ? '  ✓' : '  ✗'} ${label}: ${actual} (en az ${min})`);
}

const id = (name) => `test-closed-${STAMP}-${name}`;
const cases = {
  // [veri, silinmeli mi]
  resolvedOld: [{ status: 'resolved', resolvedAt: daysAgo(31), mediaPurged: false,
    videoStoragePath: 'q/x.mp4' }, true],
  resolvedOldPurged: [{ status: 'resolved', resolvedAt: daysAgo(45), mediaPurged: true }, true],
  resolvedRecent: [{ status: 'resolved', resolvedAt: daysAgo(29), mediaPurged: false }, false],
  expiredOld: [{ status: 'expired', creditRefundedAt: daysAgo(31) }, true],
  expiredRecent: [{ status: 'expired', creditRefundedAt: daysAgo(5) }, false],
  pendingOld: [{ status: 'pending', createdAt: daysAgo(60) }, false],
  claimedOld: [{ status: 'claimed', createdAt: daysAgo(60) }, false],
  resolvedOldReported: [{ status: 'resolved', resolvedAt: daysAgo(40), mediaPurged: false }, false],
};
const REPORT_ID = `test-closed-${STAMP}-report`;

async function seed() {
  for (const [name, [data]] of Object.entries(cases)) {
    await db.collection('questions').doc(id(name)).set({ studentUid: `test-closed-${STAMP}`, ...data });
  }
  // Mesaj alt koleksiyonu da gitmeli.
  await db.collection('questions').doc(id('resolvedOld')).collection('messages').doc('m1')
    .set({ text: 'merhaba', mediaStoragePath: 'q/x-m1.jpg' });
  await db.collection('messageReports').doc(REPORT_ID).set({
    questionId: id('resolvedOldReported'), status: 'pending',
  });
}

const exists = async (docId) => (await db.collection('questions').doc(docId).get()).exists;

async function cleanup() {
  const batch = db.batch();
  Object.keys(cases).forEach((name) => batch.delete(db.collection('questions').doc(id(name))));
  batch.delete(db.collection('messageReports').doc(REPORT_ID));
  await batch.commit();
}

async function main() {
  console.log('Kapanan soruların 30 gün sonra silinmesi\n');
  await seed();

  const counts = await fns._runClosedQuestionCleanup();
  console.log('\nSonuç:', counts, '\n');

  checkAtLeast('silinen', counts.deleted, 3);
  checkAtLeast('şikâyet yüzünden atlanan', counts.skippedForReport, 1);
  check('başarısız', counts.failed, 0);

  for (const [name, [, shouldDelete]] of Object.entries(cases)) {
    check(`${name} ${shouldDelete ? 'silindi' : 'KALDI'}`, await exists(id(name)), !shouldDelete);
  }
  check('silinen sorunun mesajları da silindi',
    (await db.collection('questions').doc(id('resolvedOld')).collection('messages').get()).size, 0);

  await cleanup();
  console.log('\nTest dokümanları silindi.');
  console.log(failures === 0 ? '\nSONUÇ: TÜM KONTROLLER GEÇTİ' : `\nSONUÇ: ${failures} KONTROL BAŞARISIZ`);
  process.exit(failures === 0 ? 0 : 1);
}

main().catch(async (e) => {
  console.error('Test hatası:', e);
  try { await cleanup(); } catch (_) {}
  process.exit(1);
});

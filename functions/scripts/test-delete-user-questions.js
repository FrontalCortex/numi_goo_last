/**
 * Hesap silindiğinde danışma sorularının temizlenmesini test eder.
 *
 * NEDEN TEST EDİLEBİLİR
 *   deleteUserQuestions yalnızca Firestore ve Storage ile çalışıyor; Auth tetikleyicisini
 *   beklemeye gerek yok, doğrudan çağrılabiliyor.
 *
 * STORAGE BİLİNÇLİ OLARAK YAPILANDIRILMADI
 *   admin.initializeApp'e storageBucket verilmiyor, dolayısıyla medya silme çağrısı hata
 *   atıyor. Bu bir eksiklik değil, testin asıl değerli kısmı: Storage tarafı çökse bile
 *   Firestore verisinin silinmeye devam etmesi gerekiyor. Gizlilik açısından öncelik
 *   sırası bu — veri silme bir yükümlülük, medya kalıntısı ise temizlenebilir bir artık.
 *
 * ÇALIŞTIRMA
 *   firebase emulators:start --only firestore
 *   $env:FIRESTORE_EMULATOR_HOST="127.0.0.1:8080"; $env:GCLOUD_PROJECT="numigo-new"
 *   node scripts/test-delete-user-questions.js
 */
const admin = require('firebase-admin');

if (!process.env.FIRESTORE_EMULATOR_HOST && process.env.ALLOW_PRODUCTION !== '1') {
  console.error('FIRESTORE_EMULATOR_HOST ayarlı değil; çıkılıyor.');
  process.exit(1);
}
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || 'numigo-new';

const fns = require('../index');
if (!admin.apps.length) admin.initializeApp({ projectId: process.env.GCLOUD_PROJECT });
const db = admin.firestore();

const STAMP = Date.now();
const UID = `test-del-uid-${STAMP}`;
const OTHER_UID = `test-other-uid-${STAMP}`;

let failures = 0;
function check(label, actual, expected) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected);
  if (!ok) failures++;
  console.log(`${ok ? '  ✓' : '  ✗'} ${label}: ${JSON.stringify(actual)}` +
    (ok ? '' : ` (beklenen: ${JSON.stringify(expected)})`));
}

const ids = {
  plain: `${UID}-q-plain`,
  withMessages: `${UID}-q-msgs`,
  reported: `${UID}-q-reported`,
  other: `${OTHER_UID}-q-other`,
};

async function seed() {
  // 1) Sade soru
  await db.collection('questions').doc(ids.plain).set({
    studentUid: UID, status: 'resolved', screenshotStoragePath: `q/${ids.plain}.jpg`,
  });

  // 2) Alt koleksiyonda mesajları olan soru
  await db.collection('questions').doc(ids.withMessages).set({
    studentUid: UID, status: 'pending', videoStoragePath: `q/${ids.withMessages}.mp4`,
  });
  await db.collection('questions').doc(ids.withMessages)
    .collection('messages').doc('m1')
    .set({ senderUid: UID, mediaStoragePath: `q/${ids.withMessages}-m1.jpg` });

  // 3) Açık moderasyon raporu olan soru — KORUNMALI
  await db.collection('questions').doc(ids.reported).set({
    studentUid: UID, status: 'resolved',
  });
  await db.collection('messageReports').doc(`${STAMP}-rep`).set({
    questionId: ids.reported, status: 'pending',
  });

  // 4) Başka kullanıcının sorusu — DOKUNULMAMALI
  await db.collection('questions').doc(ids.other).set({
    studentUid: OTHER_UID, status: 'resolved',
  });
}

const exists = async (id) => (await db.collection('questions').doc(id).get()).exists;

async function cleanup() {
  const batch = db.batch();
  Object.values(ids).forEach((id) => batch.delete(db.collection('questions').doc(id)));
  batch.delete(db.collection('messageReports').doc(`${STAMP}-rep`));
  await batch.commit();
}

async function main() {
  console.log('Hesap silmede danışma sorularının temizlenmesi\n');
  await seed();

  check('başlangıçta 4 soru var', [
    await exists(ids.plain), await exists(ids.withMessages),
    await exists(ids.reported), await exists(ids.other),
  ], [true, true, true, true]);

  const counts = await fns._deleteUserQuestions(UID);
  console.log('\nSonuç:', counts, '\n');

  check('silinen soru sayısı', counts.deleted, 2);
  check('rapor yüzünden atlanan', counts.skippedForReport, 1);
  check('başarısız', counts.failed, 0);
  // Storage yapılandırılmadığı için medya silme başarısız olmalı — ama Firestore silme
  // yine de tamamlanmalı. Aşağıdaki kontroller bunu doğruluyor.
  check('medya silme başarısız (beklenen)', counts.mediaFailed, 2);

  check('sade soru silindi', await exists(ids.plain), false);
  check('mesajlı soru silindi', await exists(ids.withMessages), false);
  check('mesaj alt koleksiyonu da silindi',
    (await db.collection('questions').doc(ids.withMessages).collection('messages').get()).size, 0);
  check('açık raporlu soru KORUNDU', await exists(ids.reported), true);
  check('başka kullanıcının sorusuna dokunulmadı', await exists(ids.other), true);

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

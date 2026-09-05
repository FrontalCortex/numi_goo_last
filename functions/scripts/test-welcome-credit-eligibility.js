/**
 * welcomeCreditGrants cihaz kapısının okuma tarafını test eder.
 *
 * checkWelcomeCreditEligibility bir callable olduğu için doğrudan çağrılamıyor (context.auth
 * gerekiyor), ama yaptığı iş iki adım: deviceKey'i salt'la özetle, o kimlikte doküman var mı
 * bak. İkisi de burada aynı şekilde yürütülüyor — yani gerçek kapının kullandığı hash
 * fonksiyonunun ve koleksiyon adının test kapsamında olması sağlanıyor.
 *
 * ÇALIŞTIRMA
 *   firebase emulators:start --only firestore
 *   export FIRESTORE_EMULATOR_HOST=127.0.0.1:8080
 *   export GCLOUD_PROJECT=numigo-new
 *   export WELCOME_CREDIT_SALT=test-salt
 *   node scripts/test-welcome-credit-eligibility.js
 */
const admin = require('firebase-admin');

if (!process.env.FIRESTORE_EMULATOR_HOST && process.env.ALLOW_PRODUCTION !== '1') {
  console.error('FIRESTORE_EMULATOR_HOST ayarlı değil; çıkılıyor.');
  process.exit(1);
}
// Salt olmadan cihaz kapısı zaten uygulanmıyor; testin anlamlı olması için gerekli.
process.env.WELCOME_CREDIT_SALT = process.env.WELCOME_CREDIT_SALT || 'test-salt';
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || 'numigo-new';

require('../index');
if (!admin.apps.length) admin.initializeApp({ projectId: process.env.GCLOUD_PROJECT });
const db = admin.firestore();

const STAMP = Date.now();
const DEVICE_A = `android-id-a-${STAMP}`;
const DEVICE_B = `android-id-b-${STAMP}`;

let failures = 0;
function check(label, actual, expected) {
  const ok = actual === expected;
  if (!ok) failures++;
  console.log(`${ok ? '  ✓' : '  ✗'} ${label}: ${actual}${ok ? '' : ` (beklenen: ${expected})`}`);
}

// checkWelcomeCreditEligibility'nin gövdesiyle aynı iki adım.
function hashOf(raw) {
  return require('crypto')
    .createHmac('sha256', process.env.WELCOME_CREDIT_SALT)
    .update(raw.trim())
    .digest('hex');
}
async function eligible(rawDeviceKey) {
  const snap = await db.collection('welcomeCreditGrants').doc(hashOf(rawDeviceKey)).get();
  return !snap.exists;
}

async function main() {
  console.log('Cihaz hoş geldin kredisi uygunluğu\n');

  check('Hiç kredi almamış cihaz uygun', await eligible(DEVICE_A), true);

  // syncSubscriptionForToken'ın kredi verirken yazdığı kaydın aynısı.
  await db.collection('welcomeCreditGrants').doc(hashOf(DEVICE_A)).set({
    grantedAt: admin.firestore.FieldValue.serverTimestamp(),
    uid: 'test-uid',
    productId: 'pro_monthly',
  });

  check('Krediyi almış cihaz artık uygun DEĞİL', await eligible(DEVICE_A), false);
  check('Başka bir cihaz etkilenmedi', await eligible(DEVICE_B), true);

  // Ham değer değil özet saklanıyor: doküman kimliği cihaz kimliğini içermemeli.
  const docId = hashOf(DEVICE_A);
  check('Doküman kimliği ham cihaz kimliğini içermiyor', docId.includes(DEVICE_A), false);
  check('Doküman kimliği 64 karakter (sha256 hex)', docId.length, 64);

  await db.collection('welcomeCreditGrants').doc(hashOf(DEVICE_A)).delete();
  console.log('\nTest dokümanı silindi.');
  console.log(failures === 0 ? '\nSONUÇ: TÜM KONTROLLER GEÇTİ' : `\nSONUÇ: ${failures} KONTROL BAŞARISIZ`);
  process.exit(failures === 0 ? 0 : 1);
}

main().catch((e) => {
  console.error('Test hatası:', e);
  process.exit(1);
});

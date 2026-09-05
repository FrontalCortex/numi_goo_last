/**
 * Kredi paketi iadesi geri alımını (Faz 1) ve bekleyen soru iptalini (Faz 2)
 * GERÇEK BİR SATIN ALMA / İADE OLMADAN test eder.
 *
 * NEDEN BÖYLE
 *   Bu yapının canlıdaki tetikleyicisi Google'ın Voided Purchases API'si; uçtan uca
 *   denemek gerçek para, gerçek satın alma ve gerçek iade gerektirir. Oysa geri alım
 *   mantığının tamamı (reverseVoidedPurchase + cancelPendingQuestionsForCreditDebt)
 *   yalnızca Firestore'la çalışır, Play API'sine hiç dokunmaz. Bu script sahte bir
 *   processedPurchases kaydı üretip o iki fonksiyonu doğrudan çağırır.
 *
 * ÇALIŞTIRMA (emülatör — önerilen, hiçbir gerçek veriye dokunmaz)
 *   cd functions && npm install
 *   firebase emulators:start --only firestore        # ayrı bir terminalde
 *   set FIRESTORE_EMULATOR_HOST=127.0.0.1:8080
 *   set GCLOUD_PROJECT=numigo-new
 *   node scripts/test-credit-refund-clawback.js
 *
 *   (macOS/Linux'ta `set` yerine `export`.)
 *
 * GERÇEK PROJEDE ÇALIŞTIRMA
 *   Gerekmiyor ve önerilmiyor. Yine de istenirse ALLOW_PRODUCTION=1 verilmeli;
 *   script yalnızca kendi ürettiği test dokümanlarına dokunur ve sonunda hepsini siler.
 */
const admin = require('firebase-admin');

const IS_EMULATOR = !!process.env.FIRESTORE_EMULATOR_HOST;
if (!IS_EMULATOR && process.env.ALLOW_PRODUCTION !== '1') {
  console.error(
    'FIRESTORE_EMULATOR_HOST ayarlı değil. Gerçek Firestore\'a yazmamak için çıkılıyor.\n' +
      'Emülatörle çalıştır ya da bilerek gerçek projeye yazacaksan ALLOW_PRODUCTION=1 ver.'
  );
  process.exit(1);
}

const PROJECT_ID =
  process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';
process.env.GCLOUD_PROJECT = PROJECT_ID;

// index.js kendi admin.initializeApp() çağrısını yapıyor; ondan ÖNCE burada
// initializeApp çağırmak "duplicate-app" hatası verir. Bu yüzden önce modülü
// yükleyip varsayılan app'i ona kurdurtuyoruz.
const fns = require('../index');
if (!admin.apps.length) admin.initializeApp({ projectId: PROJECT_ID });

const db = admin.firestore();

const STAMP = Date.now();

// Her senaryo kendi uid/token'ıyla çalışır; çakışma olmasın diye zaman damgalı.
let UID;
let TOKEN;
let GRANTED_CREDITS;
let PENDING_COUNT;

let failures = 0;
function check(label, actual, expected) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected);
  if (!ok) failures++;
  console.log(`${ok ? '  ✓' : '  ✗'} ${label}: ${JSON.stringify(actual)}` +
    (ok ? '' : ` (beklenen: ${JSON.stringify(expected)})`));
}

async function credits() {
  const snap = await db.collection('users').doc(UID).get();
  return snap.exists ? snap.data().questionCredits : null;
}

async function questionStatuses() {
  const snap = await db.collection('questions').where('studentUid', '==', UID).get();
  const out = {};
  snap.docs.forEach((d) => {
    out[d.id] = d.data().status;
  });
  return out;
}

async function seed() {
  const batch = db.batch();

  // Kullanıcı: 12 kredi aldı ve HEPSİNİ harcadı (bakiye 0).
  batch.set(db.collection('users').doc(UID), {
    uid: UID,
    role: 'STUDENT',
    keys: 1,
    currency: 0,
    questionCredits: 0,
    createdAt: admin.firestore.Timestamp.fromMillis(Date.now() - 2 * 24 * 60 * 60 * 1000),
  });

  // redeemGooglePlayPurchase'ın yazdığı kaydın aynısı.
  batch.set(db.collection('processedPurchases').doc(TOKEN), {
    uid: UID,
    productId: 'credits_large',
    type: 'product',
    orderId: `TEST.${STAMP}`,
    grantedKeys: 0,
    grantedCurrency: 0,
    grantedCredits: GRANTED_CREDITS,
    processedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  // 4 soru hâlâ kuyrukta (pending) — bunlar iptal edilmeli.
  for (let i = 0; i < PENDING_COUNT; i++) {
    batch.set(db.collection('questions').doc(`${TOKEN}-pending-${i}`), {
      studentUid: UID,
      status: 'pending',
      creditSpent: true,
      creditRefunded: false,
      createdAt: admin.firestore.Timestamp.fromMillis(Date.now() - i * 60000),
      createdAtMs: Date.now() - i * 60000,
    });
  }

  // 1 soru cevaplanmış — DOKUNULMAMALI (batık maliyet, öğrencinin cevabı kalmalı).
  batch.set(db.collection('questions').doc(`${TOKEN}-resolved`), {
    studentUid: UID,
    status: 'resolved',
    creditSpent: true,
    creditRefunded: false,
    createdAt: admin.firestore.Timestamp.fromMillis(Date.now() - 10 * 60000),
    createdAtMs: Date.now() - 10 * 60000,
  });

  await batch.commit();
}

async function cleanup() {
  const refs = [
    db.collection('users').doc(UID),
    db.collection('processedPurchases').doc(TOKEN),
    db.collection('creditRefundAudit').doc(TOKEN),
    db.collection('questions').doc(`${TOKEN}-resolved`),
  ];
  for (let i = 0; i < PENDING_COUNT; i++) {
    refs.push(db.collection('questions').doc(`${TOKEN}-pending-${i}`));
  }
  const batch = db.batch();
  refs.forEach((r) => batch.delete(r));
  await batch.commit();
}

/**
 * SENARYO A — istismar: alınan kredinin tamamı harcanmış, sonra iade alınmış.
 * Beklenen: bakiye eksiye düşer, TÜM bekleyen sorular iptal edilir (borç hâlâ kapanmaz).
 */
async function scenarioA() {
  UID = `test-clawback-a-uid-${STAMP}`;
  TOKEN = `test-clawback-a-token-${STAMP}`;
  GRANTED_CREDITS = 12; // credits_large (10) + Pro bonusu (2)
  PENDING_COUNT = 4;

  console.log('\n=== SENARYO A — 12 kredi alındı, hepsi harcandı, 4 soru kuyrukta ===');
  await seed();
  check('başlangıç bakiyesi', await credits(), 0);

  console.log('\nFAZ 1 — iade işleniyor (reverseVoidedPurchase)');
  const result = await fns._reverseVoidedPurchase({
    purchaseToken: TOKEN,
    voidedReason: 0,
    voidedSource: 0,
  });
  check('sonuç', result.outcome, 'reversed');
  check('geri alınan kredi', result.revokedCredits, GRANTED_CREDITS);
  check('bakiye eksiye düştü', await credits(), -GRANTED_CREDITS);

  const audit = await db.collection('creditRefundAudit').doc(TOKEN).get();
  check('denetim kaydı oluştu', audit.exists, true);
  if (audit.exists) {
    check('  revokedCredits', audit.data().revokedCredits, GRANTED_CREDITS);
    check('  accountAgeDaysAtRefund', audit.data().accountAgeDaysAtRefund, 2);
  }

  console.log('\nFAZ 2 — bekleyen sorular iptal ediliyor');
  const canceled = await fns._cancelPendingQuestionsForCreditDebt(UID);
  check('iptal edilen soru sayısı', canceled, PENDING_COUNT);
  check('borç azaldı', await credits(), -(GRANTED_CREDITS - PENDING_COUNT));

  const statuses = await questionStatuses();
  check(
    'kuyrukta kalan pending soru',
    Object.values(statuses).filter((v) => v === 'pending').length,
    0
  );
  check('cevaplanmış soruya dokunulmadı', statuses[`${TOKEN}-resolved`], 'resolved');

  console.log('\nTEKRAR — aynı iade ikinci kez işleniyor (çift geri alım olmamalı)');
  const again = await fns._reverseVoidedPurchase({ purchaseToken: TOKEN });
  check('sonuç', again.outcome, 'already');
  check('bakiye değişmedi', await credits(), -(GRANTED_CREDITS - PENDING_COUNT));
  check('yeniden iptal edilen soru', await fns._cancelPendingQuestionsForCreditDebt(UID), 0);

  await cleanup();
}

/**
 * SENARYO B — "borç kapanınca dur" kuralı.
 *
 * Kullanıcının bekleyen soru sayısı borcundan FAZLA. Sadece borcu kapatacak kadarı
 * iptal edilmeli; kalanlar gerçekten ödenmiş kredilerle sorulmuş demektir ve
 * dokunulmamalı. Bu, yapının en kolay yanlış yazılabilecek kuralı.
 */
async function scenarioB() {
  UID = `test-clawback-b-uid-${STAMP}`;
  TOKEN = `test-clawback-b-token-${STAMP}`;
  GRANTED_CREDITS = 2;
  PENDING_COUNT = 5;

  console.log('\n\n=== SENARYO B — 2 kredilik iade, kuyrukta 5 soru var ===');
  await seed();

  await fns._reverseVoidedPurchase({ purchaseToken: TOKEN });
  check('bakiye', await credits(), -2);

  const canceled = await fns._cancelPendingQuestionsForCreditDebt(UID);
  check('SADECE borç kadar soru iptal edildi', canceled, 2);
  check('bakiye tam sıfıra döndü (aşağı geçmedi)', await credits(), 0);

  const statuses = await questionStatuses();
  check(
    'dokunulmayan pending soru sayısı',
    Object.values(statuses).filter((v) => v === 'pending').length,
    PENDING_COUNT - 2
  );
  check('cevaplanmış soruya dokunulmadı', statuses[`${TOKEN}-resolved`], 'resolved');

  await cleanup();
}

async function main() {
  console.log('Proje:', PROJECT_ID, IS_EMULATOR ? '(emülatör)' : '(GERÇEK)');

  await scenarioA();
  await scenarioB();

  console.log('\nTest dokümanları silindi.');
  console.log(
    failures === 0 ? '\nSONUÇ: TÜM KONTROLLER GEÇTİ' : `\nSONUÇ: ${failures} KONTROL BAŞARISIZ`
  );
  process.exit(failures === 0 ? 0 : 1);
}

main().catch(async (e) => {
  console.error('\nTest hatası:', e);
  try {
    await cleanup();
  } catch (_) {
    /* temizlik de başarısızsa bırak */
  }
  process.exit(1);
});

/**
 * Yayın hediyesi (launchGift.js) testleri.
 *
 * 1. Saf kararlar: kim aday, sayaç doluyken ne olur, effectivePlan hediyeyi tanıyor mu,
 *    abonelik rütbe kararı hediyeden etkilenmiyor mu.
 * 2. Yarış (yalnızca Firestore emülatörü açıkken): 120 hesap 15 paralel işçiyle hediye istiyor;
 *    tam 100'ü almalı, sayaç 100'de durmalı, aynı hesap ikinci kez alamamalı.
 *
 * ÇALIŞTIRMA
 *   Yalnız saf kısım:  cd functions && node scripts/test-launch-gift.js
 *   Yarışla birlikte:  cd functions && npx --prefix ../firestore-rules-tests firebase \
 *                        emulators:exec --only firestore --project demo-numigoo \
 *                        "node scripts/test-launch-gift.js"
 */
const realEmulator = !!process.env.FIRESTORE_EMULATOR_HOST;
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || 'demo-numigoo';
process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:9';

const fns = require('../index');
const lg = require('../launchGift');
const admin = require('firebase-admin');

const DAY = 24 * 60 * 60 * 1000;
const NOW = Date.now();
let failures = 0;
function check(label, actual, expected) {
  const ok = actual === expected;
  if (!ok) failures++;
  console.log(`${ok ? '  ✓' : '  ✗'} ${label}: ${actual}${ok ? '' : ` (beklenen: ${expected})`}`);
}

console.log('Aday mı (launchGiftCandidate)');
const after = lg.LAUNCH_GIFT_START_MS + 1000;
const student = { role: 'STUDENT' };
const excluded = lg.parseExcludedEmails(' Test@Sorobit.com , inceleme@x.com ');
check('  yayından sonra açılan öğrenci', lg.launchGiftCandidate({ authCreatedMs: after, userData: student }).eligible, true);
check('  yayın anında açılan öğrenci', lg.launchGiftCandidate({ authCreatedMs: lg.LAUNCH_GIFT_START_MS, userData: student }).eligible, true);
check('  yayından 1 sn önce açılan', lg.launchGiftCandidate({ authCreatedMs: lg.LAUNCH_GIFT_START_MS - 1000, userData: student }).reason, 'before_launch');
check('  Auth tarihi yoksa', lg.launchGiftCandidate({ authCreatedMs: NaN, userData: student }).reason, 'before_launch');
check('  öğretmen', lg.launchGiftCandidate({ authCreatedMs: after, userData: { role: 'TEACHER' } }).reason, 'not_student');
check('  isTester', lg.launchGiftCandidate({ authCreatedMs: after, userData: { role: 'STUDENT', isTester: true } }).reason, 'tester');
check('  hariç e-posta (büyük/küçük harf, boşluk)', lg.launchGiftCandidate({
  authCreatedMs: after, userData: student, email: 'test@sorobit.com', excludedEmails: excluded,
}).reason, 'excluded_email');
check('  listede olmayan e-posta', lg.launchGiftCandidate({
  authCreatedMs: after, userData: student, email: 'veli@gmail.com', excludedEmails: excluded,
}).eligible, true);
check('  10.11.2026 00:00 TR = 09.11 21:00 UTC', new Date(lg.LAUNCH_GIFT_START_MS).toISOString(), '2026-11-09T21:00:00.000Z');

console.log('\nSayaç kararı (launchGiftDecision)');
check('  0 → verilir', lg.launchGiftDecision({ count: 0, alreadyClaimed: false, nowMs: NOW }).grant, true);
check('  99 → verilir, sıra 100', lg.launchGiftDecision({ count: 99, alreadyClaimed: false, nowMs: NOW }).nextCount, 100);
check('  100 → verilmez', lg.launchGiftDecision({ count: 100, alreadyClaimed: false, nowMs: NOW }).reason, 'limit_reached');
check('  daha önce almış → verilmez', lg.launchGiftDecision({ count: 3, alreadyClaimed: true, nowMs: NOW }).reason, 'already_claimed');
check('  süre 30 gün', lg.launchGiftDecision({ count: 0, alreadyClaimed: false, nowMs: NOW }).until - NOW, 30 * DAY);

console.log('\nPlan (effectivePlan / subscriptionPlan)');
const ep = fns._effectivePlan;
const resolve = fns._resolvePlanUpdate;
check('  hediyeli Free → Pro', ep({ plan: 'Free', giftProUntil: NOW + DAY }), 'Pro');
check('  alanı olmayan → Free', ep({}), 'Free');
check('  süresi geçmiş hediye → Free', ep({ giftProUntil: NOW - 1 }), 'Free');
check('  hediyeli Lite abonesi → Pro', ep({ plan: 'Lite', planExpiresAt: NOW + DAY, giftProUntil: NOW + DAY }), 'Pro');
check('  hediyeli Premium → Premium kalır', ep({ plan: 'Premium', giftProUntil: NOW + DAY }), 'Premium');
check('  hediye bitti, Lite sürüyor → Lite', ep({ plan: 'Lite', planExpiresAt: NOW + DAY, giftProUntil: NOW - 1 }), 'Lite');
check('  subscriptionPlan hediyeyi saymaz', fns._subscriptionPlan({ plan: 'Free', giftProUntil: NOW + DAY }), 'Free');
check('  hediyeli kullanıcının aldığı Lite yazılır',
  resolve({ plan: 'Free', giftProUntil: NOW + DAY }, 'lite_monthly', 'Lite').write, true);

async function raceTest() {
  console.log('\nYarış: 120 hesap, 15 paralel işçi (emülatör)');
  const db = admin.firestore();
  await db.recursiveDelete(db.collection('launchGift'));
  await db.recursiveDelete(db.collection('launchGiftClaims'));
  const uids = Array.from({ length: 120 }, (_, i) => `yaris_${i}`);
  await Promise.all(uids.map((uid) => db.collection('users').doc(uid).set({ uid, role: 'STUDENT' })));

  // 15 işçi aynı anda: transaction'lar sayaç belgesinde gerçekten çakışıyor. 120'sini birden
  // başlatmak emülatörün kilit kuyruğunu taşırıyor (gerçekte 100 kayıt saatlere yayılır,
  // üstelik tetikleyici failurePolicy ile yeniden deneniyor).
  const results = new Array(uids.length);
  let next = 0;
  await Promise.all(Array.from({ length: 15 }, async () => {
    while (next < uids.length) {
      const i = next++;
      results[i] = await lg.grantLaunchGift({ db, admin, uid: uids[i] });
    }
  }));
  const granted = results.filter((r) => r.granted);
  check('  hediye alan', granted.length, 100);
  check('  geri kalanı "limit_reached"', results.filter((r) => r.reason === 'limit_reached').length, 20);
  check('  sıra numaraları 1..100 tekil', new Set(granted.map((r) => r.rank)).size, 100);
  const counter = await db.collection('launchGift').doc('counter').get();
  check('  sayaç', counter.get('count'), 100);
  const giftedDocs = (await db.collection('users').where('giftProSource', '==', lg.LAUNCH_GIFT_SOURCE).get()).size;
  check('  giftProUntil yazılan kullanıcı', giftedDocs, 100);

  // Aynı hesap ikinci kez (doküman silinip yeniden yaratılmış gibi) — sayacı 50'ye çekip dene.
  await db.collection('launchGift').doc('counter').set({ count: 50 });
  const firstWinner = uids[results.findIndex((r) => r.granted)];
  const again = await lg.grantLaunchGift({ db, admin, uid: firstWinner });
  check('  aynı hesap tekrar', again.reason, 'already_claimed');
  const missing = await lg.grantLaunchGift({ db, admin, uid: 'dokumani_yok' });
  check('  kullanıcı dokümanı yoksa', missing.reason, 'no_user_doc');
}

(async () => {
  if (realEmulator) {
    await raceTest();
  } else {
    console.log('\n(Yarış testi atlandı: FIRESTORE_EMULATOR_HOST yok.)');
  }
  console.log(failures === 0 ? '\nHepsi geçti.' : `\n${failures} test başarısız.`);
  process.exit(failures === 0 ? 0 : 1);
})().catch((e) => { console.error(e); process.exit(1); });

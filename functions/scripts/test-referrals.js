/**
 * Davet döngüsü (referrals.js) testleri.
 *
 * 1. Saf kararlar: kod biçimi, atıf kararı, Pro uzatma, ödül kararı.
 * 2. Akış (yalnızca Firestore emülatörü açıkken): kod üretimi tekil ve kalıcı; atıf bir kez; kendi koduyla
 *    olmaz; eski hesap olmaz; ders 2 ödülü ikisine 7 gün, tekrarı zararsız; getirenin yıllık 10 sınırı.
 *
 * ÇALIŞTIRMA
 *   Yalnız saf kısım:  cd functions && node scripts/test-referrals.js
 *   Akışla birlikte:   cd functions && npx --prefix ../firestore-rules-tests firebase \
 *                        emulators:exec --only firestore --project demo-numigoo \
 *                        "node scripts/test-referrals.js"
 */
const realEmulator = !!process.env.FIRESTORE_EMULATOR_HOST;
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || 'demo-numigoo';
process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:9';

const fns = require('../index');
const rf = require('../referrals');
const admin = require('firebase-admin');

const DAY = 24 * 60 * 60 * 1000;
const NOW = Date.now();
let failures = 0;
function check(label, actual, expected) {
  const ok = actual === expected;
  if (!ok) failures++;
  console.log(`${ok ? '  ✓' : '  ✗'} ${label}: ${actual}${ok ? '' : ` (beklenen: ${expected})`}`);
}

console.log('index.js dışa aktarımları');
check('  getReferralCode', typeof fns.getReferralCode, 'function');
check('  claimReferral', typeof fns.claimReferral, 'function');
check('  grantReferralRewardOnLesson', typeof fns.grantReferralRewardOnLesson, 'function');

console.log('Kod biçimi');
check('  küçük harf düzeltilir', rf.normalizeCode(' k7m2qx '), 'K7M2QX');
check('  O harfi geçersiz (alfabede yok)', rf.normalizeCode('K7M2QO'), null);
check('  kısa', rf.normalizeCode('K7M2Q'), null);
check('  boş', rf.normalizeCode(undefined), null);
check('  üretilen kod geçerli', rf.normalizeCode(rf.randomCode()) !== null, true);

console.log('Atıf kararı');
const base = { codeOwnerUid: 'A', uid: 'B', alreadyReferred: false, accountAgeMs: DAY, device: 'd'.repeat(64) };
check('  yeni hesap', rf.claimDecision(base).reason, 'ok');
check('  bilinmeyen kod', rf.claimDecision({ ...base, codeOwnerUid: null }).reason, 'unknown_code');
check('  kendi kodu', rf.claimDecision({ ...base, codeOwnerUid: 'B' }).reason, 'self');
check('  ikinci kez', rf.claimDecision({ ...base, alreadyReferred: true }).reason, 'already_referred');
check('  8 günlük hesap', rf.claimDecision({ ...base, accountAgeMs: 8 * DAY }).reason, 'account_too_old');
check('  cihaz izi yok', rf.claimDecision({ ...base, device: null }).reason, 'no_device');

console.log('Pro uzatma');
check('  süresi geçmiş → şimdiden 7 gün', rf.extendGift(NOW - DAY, NOW), NOW + 7 * DAY);
check('  sürüyor → bitişten 7 gün', rf.extendGift(NOW + 10 * DAY, NOW), NOW + 17 * DAY);
check('  hiç yok', rf.extendGift(undefined, NOW), NOW + 7 * DAY);

console.log('Ödül kararı');
check('  davetle gelmemiş', rf.rewardDecision({ referral: null }).reason, 'not_referred');
check('  zaten ödüllü', rf.rewardDecision({ referral: { rewarded: true } }).reason, 'already_rewarded');
check('  getiren 9. ödülde', rf.rewardDecision({ referral: { rewarded: false }, referrerCount: 9 }).referrerToo, true);
check('  getiren 10. ödülden sonra', rf.rewardDecision({ referral: { rewarded: false }, referrerCount: 10 }).referrerToo, false);

/** Her test hesabı için ayrı, geçerli biçimde bir cihaz izi. */
const dev = (k) => require('crypto').createHash('sha256').update('test-' + k).digest('hex');

async function flow() {
  const db = admin.firestore();
  const user = (uid, data = {}) => db.collection('users').doc(uid).set({ role: 'STUDENT', ...data });
  await user('ref_A');
  await user('ref_B');
  await user('ref_C', { giftProUntil: NOW + 10 * DAY });

  console.log('Kod üretimi (emülatör)');
  const c1 = await rf.getOrCreateCode({ db, admin, uid: 'ref_A' });
  const c2 = await rf.getOrCreateCode({ db, admin, uid: 'ref_A' });
  check('  aynı kullanıcı aynı kod', c1.code === c2.code && !!c1.code, true);
  check('  referralCodes kaydı', (await db.collection('referralCodes').doc(c1.code).get()).get('uid'), 'ref_A');
  // Çakışma: ilk aday doluysa ikinci aday kullanılır.
  let n = 0;
  const seq = () => (n++ < rf.CODE_LEN ? 0 : 0.5); // ilk aday "AAAAAA", sonra başka
  await db.collection('referralCodes').doc('AAAAAA').set({ uid: 'baska' });
  const cB = await rf.getOrCreateCode({ db, admin, uid: 'ref_B', rand: seq });
  check('  dolu kod atlanır', cB.code !== 'AAAAAA' && !!cB.code, true);
  check('  belgesi olmayan kullanıcı', (await rf.getOrCreateCode({ db, admin, uid: 'yok' })).error, 'no_user_doc');

  console.log('Atıf (emülatör)');
  const created = NOW - DAY;
  check('  kendi kodu', (await rf.claimReferral({ db, admin, uid: 'ref_A', rawCode: c1.code, authCreatedMs: created })).reason, 'self');
  check('  B, A\'nın koduyla', (await rf.claimReferral({ db, admin, uid: 'ref_B', rawCode: c1.code.toLowerCase(), authCreatedMs: created, rawDevice: dev('B') })).reason, 'ok');
  check('  B ikinci kez', (await rf.claimReferral({ db, admin, uid: 'ref_B', rawCode: c1.code, authCreatedMs: created })).reason, 'already_referred');
  check('  C eski hesap', (await rf.claimReferral({ db, admin, uid: 'ref_C', rawCode: c1.code, authCreatedMs: NOW - 30 * DAY })).reason, 'account_too_old');
  check('  B.referredBy', (await db.collection('users').doc('ref_B').get()).get('referredBy'), c1.code);

  console.log('Ödül (emülatör)');
  check('  davetsiz C', (await rf.grantReferralReward({ db, admin, uid: 'ref_C', nowMs: NOW })).reason, 'not_referred');
  const g1 = await rf.grantReferralReward({ db, admin, uid: 'ref_B', nowMs: NOW });
  check('  B ödülü', g1.granted && g1.referrerRewarded, true);
  check('  B +7 gün', (await db.collection('users').doc('ref_B').get()).get('giftProUntil'), NOW + 7 * DAY);
  check('  A +7 gün', (await db.collection('users').doc('ref_A').get()).get('giftProUntil'), NOW + 7 * DAY);
  check('  tekrar zararsız', (await rf.grantReferralReward({ db, admin, uid: 'ref_B', nowMs: NOW })).reason, 'already_rewarded');

  console.log('Sıra hatası: ders 2 atıftan ÖNCE bitmiş (emülatör)');
  await user('ref_E');
  await db.collection('users').doc('ref_E').collection('lessonMilestones').doc(rf.REWARD_MILESTONE_ID)
    .set({ partId: 1, stableId: 'p1_i02_2_basamakli_sayilar', finishedAtMs: NOW - 60000 });
  const e = await rf.claimAndMaybeReward({ db, admin, uid: 'ref_E', rawCode: c1.code, authCreatedMs: created, nowMs: NOW, rawDevice: dev('E') });
  check('  atıf + ödül hemen', e.ok && e.rewardedNow, true);
  check('  E +7 gün', (await db.collection('users').doc('ref_E').get()).get('giftProUntil'), NOW + 7 * DAY);
  check('  A +7 gün daha (14)', (await db.collection('users').doc('ref_A').get()).get('giftProUntil'), NOW + 14 * DAY);
  await user('ref_F');
  const f = await rf.claimAndMaybeReward({ db, admin, uid: 'ref_F', rawCode: c1.code, authCreatedMs: created, nowMs: NOW, rawDevice: dev('F') });
  check('  ders 2 bitmemişse ödül yok', f.ok && !f.rewardedNow, true);

  console.log('Ödül bildirimi ve sayaç (emülatör)');
  check('  B bildirimi invitee', ((await db.collection('users').doc('ref_B').get()).get('referralRewardNotice') || {}).role, 'invitee');
  check('  A bildirimi referrer', ((await db.collection('users').doc('ref_A').get()).get('referralRewardNotice') || {}).role, 'referrer');
  const st = await rf.referralStats({ db, uid: 'ref_A' });
  check('  A davet sayısı (B, E, F)', st.invited, 3);
  check('  A kazandığı gün (B, E)', st.earnedDays, 14);

  console.log('Cihaz sınırı (emülatör)');
  const devA = 'a'.repeat(64);
  const devX = 'b'.repeat(64);
  await rf.getOrCreateCode({ db, admin, uid: 'ref_A', device: devA });
  check('  A cihazı koda yazıldı', ((await db.collection('referralCodes').doc(c1.code).get()).get('devices') || []).includes(devA), true);
  await user('ref_G');
  check('  A\'nın telefonunda açılan hesap', (await rf.claimReferral({ db, admin, uid: 'ref_G', rawCode: c1.code, authCreatedMs: created, rawDevice: devA })).reason, 'same_device');
  check('  yeni telefon X', (await rf.claimReferral({ db, admin, uid: 'ref_G', rawCode: c1.code, authCreatedMs: created, rawDevice: devX })).reason, 'ok');
  await user('ref_H');
  check('  X\'te ikinci yeni hesap', (await rf.claimReferral({ db, admin, uid: 'ref_H', rawCode: c1.code, authCreatedMs: created, rawDevice: devX })).reason, 'device_used');
  check('  bozuk parmak izi = iz yok', (await rf.claimReferral({ db, admin, uid: 'ref_H', rawCode: c1.code, authCreatedMs: created, rawDevice: 'xyz' })).reason, 'no_device');
  check('  invite kaynağı', (await rf.claimReferral({ db, admin, uid: 'ref_G', rawCode: c1.code, authCreatedMs: created, rawSource: 'invite' })).reason, 'already_referred');

  console.log('Yıllık sınır (emülatör)');
  const year = new Date(NOW).getUTCFullYear();
  await db.collection('referralRewardCounts').doc(`ref_A_${year}`).set({ count: rf.REFERRER_YEARLY_LIMIT });
  await user('ref_D');
  check('  D atıf', (await rf.claimReferral({ db, admin, uid: 'ref_D', rawCode: c1.code, authCreatedMs: created, rawDevice: dev('D') })).reason, 'ok');
  const g2 = await rf.grantReferralReward({ db, admin, uid: 'ref_D', nowMs: NOW });
  check('  D ödül alır, A almaz', g2.granted && !g2.referrerRewarded, true);
  check('  A değişmedi', (await db.collection('users').doc('ref_A').get()).get('giftProUntil'), NOW + 14 * DAY);
}

(async () => {
  if (realEmulator) await flow();
  else console.log('\n(Firestore emülatörü yok: akış testleri atlandı.)');
  console.log(failures ? `\n${failures} HATA` : '\nHepsi geçti.');
  process.exit(failures ? 1 : 0);
})().catch((e) => { console.error(e); process.exit(1); });

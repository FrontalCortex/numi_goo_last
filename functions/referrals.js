/**
 * Davet döngüsü (haftalık kart paylaşımı → yeni kullanıcı): kod, atıf, ödül.
 *
 * NEDEN
 *   Büyüme döngüsü 2 (docs/BUYUME_DONGUSU_ARASTIRMASI.md §2, §4): veli haftalık kartı WhatsApp'ta
 *   paylaşıyor, bağlantı `https://numigo-new.web.app/d/<kod>` → Play (referrer'da kod) → yeni kurulum.
 *   K'yı GA4'ten değil buradaki `referrals` koleksiyonundan sayıyoruz: GA4 tarayıcı/Play kayıplarıyla
 *   eksik sayar.
 *
 * VERİ
 *   referralCodes/{KOD}        { uid, createdAt }               kod → sahibi (yalnızca sunucu)
 *   users/{uid}.referralCode   "K7M2QX"                         sahibin kodu (istemci okur)
 *   referrals/{yeniUid}        { code, referrerUid, source, createdAt, rewarded, … }
 *   referralRewardCounts/{referrerUid}_{yıl} { count }          getirenin yıllık ödül sayacı
 *   Hepsi istemciye kapalı (kurallarda eşleşme yok = ret); yazma yalnızca bu dosyadaki callable'lar
 *   ve tetikleyiciyle.
 *
 * ÖDÜL (kullanıcı kararı 08.11.2026): gelen kullanıcı ders 2'yi (p1_i02) bitirince ikisine de 7 gün
 *   Pro (`giftProUntil` uzatılır; yayın hediyesiyle aynı alan, bkz. launchGift.js). Getiren yılda en
 *   çok 10 davetten ödül alır; gelen yine alır. "Ders 2 bitti" sinyali istemcinin yazdığı
 *   `lessonMilestones/1_p1_i02_…` (LessonMilestones.recordFinish); sahte yazılabilir ama her sahte
 *   ödül yeni bir hesap ve sahibinin yıllık 10 sınırı demek, risk küçük.
 */

const CODE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // 0/O, 1/I yok: elle yazarken karışmasın
const CODE_LEN = 6;
const CODE_RE = new RegExp(`^[${CODE_ALPHABET}]{${CODE_LEN}}$`);
const DAY_MS = 24 * 60 * 60 * 1000;
const REWARD_DAYS = 7;
const REFERRER_YEARLY_LIMIT = 10;
/** Atıf yalnızca yeni hesaba: Auth hesabı bundan eskiyse "davetle geldi" sayılmaz. */
const CLAIM_MAX_ACCOUNT_AGE_MS = 7 * DAY_MS;
/** Ödülü tetikleyen ders: Part 1'in "2 basamaklı sayılar" dersi (GA4 hunisinde "ders 2"). */
const REWARD_MILESTONE_ID = '1_p1_i02_2_basamakli_sayilar';
// 'invite': Mağaza / Ayarlar'daki genel "Arkadaşını davet et" ekranı (09.11.2026).
const SOURCES = new Set(['weekly', 'invite', 'badge', 'challenge', 'class']);
/** İstemcinin gönderdiği cihaz parmak izi: SHA-256 (64 onaltılık). Ham cihaz kimliği sunucuya gelmiyor. */
const DEVICE_RE = /^[0-9a-f]{64}$/;
const REWARD_SOURCE = 'referral';

/** Kullanıcının yazdığı / referrer'dan gelen kodu düzeltir; geçersizse null. */
function normalizeCode(raw) {
  const s = String(raw || '').trim().toUpperCase();
  return CODE_RE.test(s) ? s : null;
}

function randomCode(rand = Math.random) {
  let s = '';
  for (let i = 0; i < CODE_LEN; i++) s += CODE_ALPHABET[Math.floor(rand() * CODE_ALPHABET.length)];
  return s;
}

/**
 * Atıf kararı (saf). @returns {{ ok: boolean, reason: string }}
 */
function claimDecision({ codeOwnerUid, uid, alreadyReferred, accountAgeMs, deviceUsedBy, referrerDevices, device }) {
  if (!codeOwnerUid) return { ok: false, reason: 'unknown_code' };
  if (codeOwnerUid === uid) return { ok: false, reason: 'self' };
  if (alreadyReferred) return { ok: false, reason: 'already_referred' };
  // Cihaz sınırı (kullanıcı isteği 09.11.2026: sahte hesapla Pro toplanmasın): bir telefon yalnızca bir
  // kez "davetle gelen" sayılır ve davet edenin kendi telefonu sayılmaz. Uygulamayı silip yeni hesapla
  // yeniden kurmak cihaz parmak izini değiştirmiyor (ANDROID_ID). Parmak izi gelmezse (eski sürüm) geçiyor.
  if (device && deviceUsedBy && deviceUsedBy !== uid) return { ok: false, reason: 'device_used' };
  if (device && Array.isArray(referrerDevices) && referrerDevices.includes(device)) return { ok: false, reason: 'same_device' };
  if (!(accountAgeMs >= 0) || accountAgeMs > CLAIM_MAX_ACCOUNT_AGE_MS) return { ok: false, reason: 'account_too_old' };
  return { ok: true, reason: 'ok' };
}

/** `giftProUntil`'i [days] gün uzatır: süresi geçmişse şimdiden, sürüyorsa bittiği andan. */
function extendGift(currentUntil, nowMs, days = REWARD_DAYS) {
  const base = Math.max(Number(currentUntil) || 0, nowMs);
  return base + days * DAY_MS;
}

/** Ödül kararı (saf). */
function rewardDecision({ referral, referrerCount }) {
  if (!referral) return { grant: false, reason: 'not_referred' };
  if (referral.rewarded === true) return { grant: false, reason: 'already_rewarded' };
  return {
    grant: true,
    reason: 'ok',
    referrerToo: (Number(referrerCount) || 0) < REFERRER_YEARLY_LIMIT,
  };
}

/** Kullanıcının kodu; yoksa benzersiz yeni bir kod üretip iki yere birden yazar. */
async function getOrCreateCode({ db, admin, uid, rand = Math.random, device = null }) {
  const userRef = db.collection('users').doc(uid);
  const dev = DEVICE_RE.test(String(device || '')) ? device : null;
  // Davet edenin cihazları koda yazılıyor: o telefonda açılan "davetli" hesap ödül alamasın.
  const addDevice = (tx, codeRef) => {
    if (dev) tx.set(codeRef, { devices: admin.firestore.FieldValue.arrayUnion(dev) }, { merge: true });
  };
  for (let attempt = 0; attempt < 8; attempt++) {
    const candidate = randomCode(rand);
    const code = await db.runTransaction(async (tx) => {
      const userSnap = await tx.get(userRef);
      if (!userSnap.exists) return { error: 'no_user_doc' };
      const existing = normalizeCode(userSnap.get('referralCode'));
      if (existing) {
        addDevice(tx, db.collection('referralCodes').doc(existing));
        return { code: existing };
      }
      const codeRef = db.collection('referralCodes').doc(candidate);
      const codeSnap = await tx.get(codeRef);
      if (codeSnap.exists) return null; // çakışma: yeni adayla yeniden
      tx.set(codeRef, { uid, createdAt: admin.firestore.FieldValue.serverTimestamp(), ...(dev ? { devices: [dev] } : {}) });
      tx.update(userRef, { referralCode: candidate });
      return { code: candidate };
    });
    if (code) return code;
  }
  return { error: 'no_free_code' };
}

/** Atıf: `referrals/{uid}` (yalnızca bir kez) ve `users/{uid}.referredBy`. */
async function claimReferral({ db, admin, uid, rawCode, rawSource, authCreatedMs, rawDevice = null, nowMs = Date.now() }) {
  const code = normalizeCode(rawCode);
  if (!code) return { ok: false, reason: 'bad_code' };
  const source = SOURCES.has(rawSource) ? rawSource : 'weekly';
  const device = DEVICE_RE.test(String(rawDevice || '')) ? rawDevice : null;
  const codeRef = db.collection('referralCodes').doc(code);
  const refRef = db.collection('referrals').doc(uid);
  const userRef = db.collection('users').doc(uid);
  const devRef = device ? db.collection('referralDevices').doc(device) : null;
  return db.runTransaction(async (tx) => {
    const [codeSnap, refSnap, userSnap, devSnap] = await Promise.all([
      tx.get(codeRef), tx.get(refRef), tx.get(userRef), devRef ? tx.get(devRef) : Promise.resolve(null),
    ]);
    if (!userSnap.exists) return { ok: false, reason: 'no_user_doc' };
    const d = claimDecision({
      codeOwnerUid: codeSnap.exists ? codeSnap.get('uid') : null,
      uid,
      alreadyReferred: refSnap.exists,
      accountAgeMs: nowMs - (Number(authCreatedMs) || 0),
      deviceUsedBy: devSnap && devSnap.exists ? devSnap.get('uid') : null,
      referrerDevices: codeSnap.exists ? codeSnap.get('devices') : null,
      device,
    });
    if (!d.ok) return d;
    const ts = admin.firestore.FieldValue.serverTimestamp();
    tx.set(refRef, {
      code,
      referrerUid: codeSnap.get('uid'),
      source,
      createdAt: ts,
      createdAtMs: nowMs,
      rewarded: false,
      ...(device ? { device } : {}),
    });
    if (devRef) tx.set(devRef, { uid, code, atMs: nowMs });
    tx.update(userRef, { referredBy: code, referredSource: source });
    return { ok: true, reason: 'ok' };
  });
}

/**
 * Atıf + gerekiyorsa ödül. Sıra hatasını kapatıyor: ödül tetikleyicisi yalnızca ders 2'nin bitiş
 * kaydı YAZILDIĞI anda çalışıyor; atıf o andan sonra gelirse (ör. çevrimdışı bitirilen ders, gecikmiş
 * claimReferral) tetikleyici "davetle gelmemiş" deyip geçmişti ve ödül hiç verilmezdi. Atıf kabul
 * edildiğinde ders 2 zaten bitmişse ödül burada veriliyor; ikisi aynı anda olsa bile
 * grantReferralReward transaction'ı (rewarded bayrağı) iki kez vermiyor.
 */
async function claimAndMaybeReward(args) {
  const r = await claimReferral(args);
  if (!r.ok) return r;
  const { db, admin, uid } = args;
  const ms = await db.collection('users').doc(uid).collection('lessonMilestones').doc(REWARD_MILESTONE_ID).get();
  if (!ms.exists || !ms.get('finishedAtMs')) return r;
  const g = await grantReferralReward({ db, admin, uid, nowMs: args.nowMs });
  return { ...r, rewardedNow: g.granted === true };
}

/** Ders 2 bitince: gelene ve (yıllık sınır dolmadıysa) getirene 7 gün Pro. Tekrarı zararsız. */
async function grantReferralReward({ db, admin, uid, nowMs = Date.now() }) {
  const refRef = db.collection('referrals').doc(uid);
  const pre = await refRef.get();
  if (!pre.exists) return { granted: false, reason: 'not_referred' };
  const referrerUid = pre.get('referrerUid');
  const year = new Date(nowMs).getUTCFullYear();
  const counterRef = db.collection('referralRewardCounts').doc(`${referrerUid}_${year}`);
  const userRef = db.collection('users').doc(uid);
  const referrerRef = db.collection('users').doc(referrerUid);

  return db.runTransaction(async (tx) => {
    const [refSnap, counterSnap, userSnap, referrerSnap] = await Promise.all([
      tx.get(refRef), tx.get(counterRef), tx.get(userRef), tx.get(referrerRef),
    ]);
    const d = rewardDecision({
      referral: refSnap.exists ? refSnap.data() : null,
      referrerCount: counterSnap.exists ? counterSnap.get('count') : 0,
    });
    if (!d.grant) return { granted: false, reason: d.reason };
    const ts = admin.firestore.FieldValue.serverTimestamp();
    // referralRewardNotice: uygulama bir kez "1 hafta Pro kazandın" paneli gösteriyor (kullanıcı kararı
    // 09.11.2026; Pro sessizce açılınca döngü kendini anlatmıyordu). atMs cihazda "görüldü" diye saklanıyor.
    const notice = (role) => ({ role, days: REWARD_DAYS, atMs: nowMs });
    if (userSnap.exists) {
      tx.update(userRef, {
        giftProUntil: extendGift(userSnap.get('giftProUntil'), nowMs), giftProSource: REWARD_SOURCE,
        referralRewardNotice: notice('invitee'),
      });
    }
    const referrerToo = d.referrerToo && referrerSnap.exists;
    if (referrerToo) {
      tx.update(referrerRef, {
        giftProUntil: extendGift(referrerSnap.get('giftProUntil'), nowMs), giftProSource: REWARD_SOURCE,
        referralRewardNotice: notice('referrer'),
      });
      tx.set(counterRef, { count: (Number(counterSnap.exists ? counterSnap.get('count') : 0) || 0) + 1, year, updatedAt: ts }, { merge: true });
    }
    tx.update(refRef, { rewarded: true, rewardedAt: ts, rewardedAtMs: nowMs, referrerRewarded: referrerToo });
    return { granted: true, reason: 'ok', referrerRewarded: referrerToo };
  });
}

/** Davet ekranındaki sayaç: kaç kişi davetle geldi, getiren kaç gün Pro kazandı. */
async function referralStats({ db, uid }) {
  const col = db.collection('referrals').where('referrerUid', '==', uid);
  const [all, mine] = await Promise.all([
    col.count().get(),
    col.where('referrerRewarded', '==', true).count().get(),
  ]);
  return { invited: all.data().count, earnedDays: mine.data().count * REWARD_DAYS };
}

/** Callable'lar ve tetikleyici: index.js'te her biri ayrı export (deploy-plan yalnızca değişeni seçsin). */
function createGetReferralCode({ functions, admin, db }) {
  return functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
    const uid = context.auth.uid;
    const r = await getOrCreateCode({ db, admin, uid, device: data && data.device });
    if (r.error) throw new functions.https.HttpsError('failed-precondition', r.error);
    // Sayaç okunamazsa kod yine dönsün (paylaşım durmasın).
    let stats = { invited: 0, earnedDays: 0 };
    try { stats = await referralStats({ db, uid }); } catch (e) { console.warn('referralStats', e.message); }
    return { code: r.code, rewardDays: REWARD_DAYS, ...stats };
  });
}

function createClaimReferral({ functions, admin, db }) {
  return functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
    const uid = context.auth.uid;
    let authCreatedMs = 0;
    try {
      const u = await admin.auth().getUser(uid);
      authCreatedMs = Date.parse(u.metadata.creationTime);
    } catch (e) {
      console.warn('claimReferral: Auth kullanıcısı okunamadı', { uid, error: e.message });
    }
    const r = await claimAndMaybeReward({
      db, admin, uid, rawCode: data && data.code, rawSource: data && data.source, authCreatedMs,
      rawDevice: data && data.device,
    });
    console.log('claimReferral', { uid, ...r });
    return r;
  });
}

function createRewardTrigger({ functions, admin, db }) {
  return functions
    .firestore
    .document(`users/{uid}/lessonMilestones/{docId}`)
    .onWrite(async (change, context) => {
      if (context.params.docId !== REWARD_MILESTONE_ID) return null;
      const after = change.after.exists ? change.after.data() : null;
      if (!after || !after.finishedAtMs) return null;
      const r = await grantReferralReward({ db, admin, uid: context.params.uid });
      if (r.reason !== 'not_referred') console.log('referralReward', { uid: context.params.uid, ...r });
      return null;
    });
}

module.exports = {
  CODE_ALPHABET,
  CODE_LEN,
  REWARD_DAYS,
  REFERRER_YEARLY_LIMIT,
  CLAIM_MAX_ACCOUNT_AGE_MS,
  REWARD_MILESTONE_ID,
  normalizeCode,
  randomCode,
  claimDecision,
  extendGift,
  rewardDecision,
  getOrCreateCode,
  claimReferral,
  claimAndMaybeReward,
  grantReferralReward,
  referralStats,
  createGetReferralCode,
  createClaimReferral,
  createRewardTrigger,
};

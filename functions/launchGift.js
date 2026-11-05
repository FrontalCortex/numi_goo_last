/**
 * Yayın hediyesi: 10 Kasım 2026'dan itibaren hesap açan ilk 100 öğrenciye 1 ay Pro.
 *
 * NEDEN
 *   Reklam 13 videosunun kapanışında (TR, EN, ID, AR) bu söz verildi. Play Store indirme
 *   sırasını vermediği için "ilk 100 kişi" uygulamadaki hesap açılış sırasıdır.
 *
 * NASIL
 *   `users/{uid}` dokümanı ilk kez oluşunca (onCreate) tek bir transaction:
 *     • `launchGift/counter.count` < LIMIT ise bir artırılır,
 *     • `launchGiftClaims/{uid}` defteri yazılır (aynı hesap iki kez almasın: hesap
 *       dokümanını silip yeniden yaratmak onCreate'i yeniden tetikler),
 *     • `users/{uid}.giftProUntil` = şimdi + 30 gün yazılır.
 *   Sayaç tek belge ve transaction içinde okunup yazıldığı için aynı anda açılan
 *   hesaplar 100'ü aşamaz (çakışan transaction yeniden denenir).
 *
 *   `plan` alanına DOKUNULMUYOR: abonelik mantığı (resolvePlanUpdate, iade, Play
 *   bildirimleri) plan/planExpiresAt üzerinde çalışıyor; hediye ayrı alanda durunca
 *   hediyeli kullanıcı Lite ya da Pro satın aldığında o abonelik normal yazılır, hediye
 *   bitince de kendiliğinden düşer. Okuyan taraf effectivePlan (sunucu) ve
 *   PlanStatus.effectivePlan (istemci).
 *
 * SAYILMAYANLAR
 *   • Auth hesabı yayın anından önce açılmış olanlar (test ve kapalı test hesapları,
 *     Play inceleme hesabı). Doküman silinip yeniden yaratılsa da Auth tarihi değişmez.
 *   • Öğretmenler (onaylı öğretmen zaten her şeye sahip; söz velilere/çocuklara verildi).
 *   • `isTester: true` olanlar ve LAUNCH_GIFT_EXCLUDED_EMAILS (virgülle ayrılmış, .env)
 *     listesindeki e-postalar — yayından sonra açılan test hesapları için.
 */

// 10.11.2026 00:00 Türkiye saati (UTC+3).
const LAUNCH_GIFT_START_MS = Date.UTC(2026, 10, 9, 21, 0, 0);
const LAUNCH_GIFT_LIMIT = 100;
const LAUNCH_GIFT_DURATION_MS = 30 * 24 * 60 * 60 * 1000;
const LAUNCH_GIFT_SOURCE = 'launch_2026_11';

const COUNTER_PATH = ['launchGift', 'counter'];
const CLAIMS_COLLECTION = 'launchGiftClaims';

// Tek sayaç belgesi: aynı anda çok kayıt gelirse transaction'lar çakışıp yeniden denenir.
// Varsayılan 5 deneme emülatördeki 120 eşzamanlı testte yetmedi.
const TX_MAX_ATTEMPTS = 30;
// Tetikleyici hata verirse (ör. yine çakışma) Cloud Functions yeniden dener (failurePolicy).
// Defter (launchGiftClaims) tekrarı zararsız kılıyor; bu süreden eski olaylar bırakılır.
const RETRY_WINDOW_MS = 6 * 60 * 60 * 1000;

function parseExcludedEmails(raw) {
  return new Set(
    String(raw || '')
      .split(',')
      .map((s) => s.trim().toLowerCase())
      .filter(Boolean),
  );
}

/**
 * Hesap hediyeye aday mı (sayaçtan bağımsız kısım). Saf fonksiyon; test edilebilir.
 * @returns {{ eligible: boolean, reason: string }}
 */
function launchGiftCandidate({ authCreatedMs, userData, email, excludedEmails }) {
  const created = Number(authCreatedMs) || 0;
  if (!created || created < LAUNCH_GIFT_START_MS) return { eligible: false, reason: 'before_launch' };
  const data = userData || {};
  if ((data.role || '') !== 'STUDENT') return { eligible: false, reason: 'not_student' };
  if (data.isTester === true) return { eligible: false, reason: 'tester' };
  const mail = String(email || data.email || '').trim().toLowerCase();
  if (mail && excludedEmails && excludedEmails.has(mail)) return { eligible: false, reason: 'excluded_email' };
  return { eligible: true, reason: 'ok' };
}

/**
 * Transaction içindeki karar. Saf fonksiyon; test edilebilir.
 * @returns {{ grant: boolean, reason: string, nextCount?: number, until?: number }}
 */
function launchGiftDecision({ count, alreadyClaimed, nowMs }) {
  if (alreadyClaimed) return { grant: false, reason: 'already_claimed' };
  const n = Number(count) || 0;
  if (n >= LAUNCH_GIFT_LIMIT) return { grant: false, reason: 'limit_reached' };
  return { grant: true, reason: 'ok', nextCount: n + 1, until: nowMs + LAUNCH_GIFT_DURATION_MS };
}

/** Hediye şu an geçerli mi (sunucudaki effectivePlan bunu kullanıyor). */
function hasActiveGift(userData, nowMs = Date.now()) {
  const until = Number(userData && userData.giftProUntil) || 0;
  return until > nowMs;
}

/**
 * Sayacı transaction ile artırıp hediyeyi yazar.
 * @returns {Promise<{ granted: boolean, reason: string, rank?: number }>}
 */
async function grantLaunchGift({ db, admin, uid, nowMs = Date.now() }) {
  const counterRef = db.collection(COUNTER_PATH[0]).doc(COUNTER_PATH[1]);
  const claimRef = db.collection(CLAIMS_COLLECTION).doc(uid);
  const userRef = db.collection('users').doc(uid);

  return db.runTransaction(async (tx) => {
    const [counterSnap, claimSnap, userSnap] = await Promise.all([
      tx.get(counterRef), tx.get(claimRef), tx.get(userRef),
    ]);
    if (!userSnap.exists) return { granted: false, reason: 'no_user_doc' };
    const decision = launchGiftDecision({
      count: counterSnap.exists ? counterSnap.get('count') : 0,
      alreadyClaimed: claimSnap.exists,
      nowMs,
    });
    if (!decision.grant) return { granted: false, reason: decision.reason };

    const ts = admin.firestore.FieldValue.serverTimestamp();
    tx.set(counterRef, { count: decision.nextCount, limit: LAUNCH_GIFT_LIMIT, updatedAt: ts }, { merge: true });
    tx.set(claimRef, { uid, rank: decision.nextCount, until: decision.until, source: LAUNCH_GIFT_SOURCE, grantedAt: ts });
    tx.update(userRef, { giftProUntil: decision.until, giftProSource: LAUNCH_GIFT_SOURCE });
    return { granted: true, reason: 'ok', rank: decision.nextCount };
  }, { maxAttempts: TX_MAX_ATTEMPTS });
}

/** `users/{uid}` onCreate tetikleyicisi. */
function createLaunchGiftTrigger({ functions, admin, db }) {
  return functions
    .runWith({ failurePolicy: true })
    .firestore
    .document('users/{uid}')
    .onCreate(async (snap, context) => {
      const uid = context.params.uid;
      const eventAge = Date.now() - Date.parse(context.timestamp);
      if (eventAge > RETRY_WINDOW_MS) {
        console.warn('launchGift: olay çok eski, bırakıldı', { uid, eventAge });
        return null;
      }
      // Sayaç dolduysa Auth'a hiç gitme (100'den sonraki her kayıtta boşuna okuma olmasın).
      const counterSnap = await db.collection(COUNTER_PATH[0]).doc(COUNTER_PATH[1]).get();
      if (counterSnap.exists && (Number(counterSnap.get('count')) || 0) >= LAUNCH_GIFT_LIMIT) return null;
      if (Date.now() < LAUNCH_GIFT_START_MS) return null;

      let authUser;
      try {
        authUser = await admin.auth().getUser(uid);
      } catch (e) {
        console.warn('launchGift: Auth kullanıcısı bulunamadı', { uid, error: e.message });
        return null;
      }
      const candidate = launchGiftCandidate({
        authCreatedMs: Date.parse(authUser.metadata.creationTime),
        userData: snap.data(),
        email: authUser.email,
        excludedEmails: parseExcludedEmails(process.env.LAUNCH_GIFT_EXCLUDED_EMAILS),
      });
      if (!candidate.eligible) {
        console.log('launchGift: aday değil', { uid, reason: candidate.reason });
        return null;
      }
      const result = await grantLaunchGift({ db, admin, uid });
      console.log('launchGift', { uid, ...result });
      return null;
    });
}

module.exports = {
  LAUNCH_GIFT_START_MS,
  LAUNCH_GIFT_LIMIT,
  LAUNCH_GIFT_DURATION_MS,
  LAUNCH_GIFT_SOURCE,
  parseExcludedEmails,
  launchGiftCandidate,
  launchGiftDecision,
  hasActiveGift,
  grantLaunchGift,
  createLaunchGiftTrigger,
};

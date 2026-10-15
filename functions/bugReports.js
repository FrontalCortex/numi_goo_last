'use strict';

/**
 * HATA BİLDİRİMİ ÇEKİRDEĞİ — kullanıcının uygulamada gördüğü hatayı ekran görüntüsüyle
 * bildirmesi, admin'in onaylayıp/reddetmesi ve onaylananın ödülünü kullanıcının toplaması.
 *
 * AKIŞ
 *   1. İstemci (varsa) görseli `bug_reports/{uid}_{zaman}.jpg` yoluna yükler, sonra
 *      `submitBugReport` ile metni ve yolu gönderir → `bugReports/{id}` (status: pending).
 *      Görsel isteğe bağlı; yoksa `imagePath` boş.
 *   2. Admin (Auth custom claim `admin: true`, bkz. scripts/set-admin-claim.js) uygulama
 *      içindeki panelden `reviewBugReport` çağırır: onaylarken ödül kademesini seçer,
 *      reddederken gerekçeyi. Kullanıcıya bildirim gider.
 *   3. Kullanıcı Görevler'deki kartta "Ödülünü topla"ya basar → `claimBugReportReward`
 *      ödülü bakiyeye yazar. Ödül onayda değil toplamada yazılıyor; kullanıcı ödülü
 *      görerek alsın diye.
 *
 * NEDEN AYRI MODÜL
 *   Burada yalnızca saf karar var (doğrulama, sınırlar, durum geçişleri); Firestore'a dokunan
 *   taraf index.js. Aynı desen notifications.js ve streakReminder.js'te: kurallar emülatör
 *   kurmadan test edilebiliyor (bkz. scripts/test-bug-reports.js).
 *
 * NEDEN BU KADAR SIKI
 *   Büyük ödül gerçek parayla satılan şeyler içeriyor (danışma kredisi, anahtar). Ödül
 *   yalnızca admin onayından geçtiği için asıl koruma o; buradaki sınırlar panelin çöple
 *   dolmasını ve aynı bildirimin iki kez ödül vermesini engelliyor.
 */

/**
 * Ödül kademeleri — admin onaylarken birini seçiyor.
 *
 * `big`: gerçek, yeni bir hata. `small`: küçük bir kusur ya da başkasının daha önce
 * bildirdiği bir hatayı ikinci kez bildirmek (kullanıcı kararı: ilk bildiren tam ödülü
 * alır; sonrakini admin reddeder ya da teşekkür olarak küçük ödül verir).
 */
const BUG_REPORT_TIERS = {
  big: { gold: 10000, keys: 200, credits: 3 },
  small: { gold: 1000, keys: 20, credits: 0 },
};

/** Ret gerekçeleri; istemci koda göre kullanıcının dilinde metin gösteriyor. */
const BUG_REPORT_REJECT_REASONS = ['not_a_bug', 'duplicate', 'unclear', 'other'];

const BUG_REPORT_TEXT_MIN = 10;
const BUG_REPORT_TEXT_MAX = 1000;
/** Admin'in yazabileceği not (kullanıcıya gösteriliyor). */
const BUG_REPORT_NOTE_MAX = 300;

/** Aynı anda incelemede bekleyebilecek bildirim sayısı. */
const BUG_REPORT_MAX_PENDING = 3;
/** UTC günü başına gönderim sınırı. */
const BUG_REPORT_DAILY_LIMIT = 3;

/** Cihaz bilgisi alanları ve her birinin en fazla uzunluğu — hatayı yeniden üretmek için. */
const DEVICE_FIELDS = {
  appVersion: 32,
  versionCode: 12,
  manufacturer: 40,
  model: 60,
  sdk: 6,
  locale: 16,
  screen: 60,
};

function fail(code, message) {
  return { ok: false, code, message };
}

/** Yükleme yolu çağıranın kendi dosyası mı: `bug_reports/{uid}_{zaman}.jpg`. */
function isOwnImagePath(path, uid) {
  if (typeof path !== 'string' || typeof uid !== 'string' || !uid) return false;
  const prefix = `bug_reports/${uid}_`;
  if (!path.startsWith(prefix)) return false;
  return /^\d{10,16}\.jpg$/.test(path.slice(prefix.length));
}

/** Cihaz bilgisini beyaz listeye ve uzunluk sınırına göre süzer. */
function sanitizeDevice(raw) {
  const out = {};
  if (!raw || typeof raw !== 'object') return out;
  for (const [key, max] of Object.entries(DEVICE_FIELDS)) {
    const value = raw[key];
    if (typeof value === 'string' || typeof value === 'number') {
      const text = String(value).trim().slice(0, max);
      if (text) out[key] = text;
    }
  }
  return out;
}

/**
 * Gönderimi doğrular ve saklanacak hâle getirir.
 * @returns `{ ok: true, text, imagePath, device }` ya da `{ ok: false, code, message }`.
 */
function normalizeSubmission(data, uid) {
  const text = typeof (data && data.text) === 'string' ? data.text.trim() : '';
  if (text.length < BUG_REPORT_TEXT_MIN) {
    return fail('invalid-argument', 'Lütfen sorunu biraz daha ayrıntılı anlat.');
  }
  if (text.length > BUG_REPORT_TEXT_MAX) {
    return fail('invalid-argument', 'Açıklama çok uzun.');
  }
  // Ekran görüntüsü isteğe bağlı (kullanıcı kararı): bazı hatalar metinle anlatılabiliyor.
  // Verildiyse çağıranın kendi dosyası olmalı; boş yol "görsel yok" demek.
  const rawPath = data && data.imagePath;
  const imagePath = typeof rawPath === 'string' ? rawPath.trim() : '';
  if (imagePath && !isOwnImagePath(imagePath, uid)) {
    return fail('invalid-argument', 'Ekran görüntüsü bulunamadı.');
  }
  return { ok: true, text, imagePath, device: sanitizeDevice(data && data.device) };
}

/**
 * Gönderim sınırları: aynı anda bekleyen ve günlük gönderim.
 *
 * @param guard        `bugReportGuards/{uid}` içeriği (yoksa `{}`).
 * @param pendingCount Kullanıcının `pending` durumdaki bildirim sayısı.
 * @returns `{ ok: true, nextGuard }` ya da `{ ok: false, code, message }`.
 */
function submitDecision(guard, pendingCount, dayKey) {
  if (pendingCount >= BUG_REPORT_MAX_PENDING) {
    return fail(
      'resource-exhausted',
      'İncelenmeyi bekleyen bildirimlerin var. Onlar sonuçlanınca yenisini gönderebilirsin.'
    );
  }
  const sameDay = guard && guard.dayKey === dayKey;
  const used = sameDay ? Number(guard.count) || 0 : 0;
  if (used >= BUG_REPORT_DAILY_LIMIT) {
    return fail('resource-exhausted', 'Bugünlük bildirim hakkın doldu. Yarın tekrar deneyebilirsin.');
  }
  return { ok: true, nextGuard: { dayKey, count: used + 1 } };
}

/**
 * Admin kararını doğrular; bildirim dokümanına yazılacak alanları döndürür.
 * Zaman damgası ve inceleyen uid'i çağıran ekliyor.
 *
 * @param report Mevcut doküman içeriği.
 * @param input  `{ decision: 'approve'|'reject', tier, rejectReason, note }`.
 */
function reviewDecision(report, input) {
  if (!report) return fail('not-found', 'Bildirim bulunamadı.');
  if (report.status !== 'pending') {
    return fail('failed-precondition', 'Bu bildirim zaten sonuçlandırılmış.');
  }
  const decision = input && input.decision;
  const note =
    typeof (input && input.note) === 'string' ? input.note.trim().slice(0, BUG_REPORT_NOTE_MAX) : '';

  if (decision === 'approve') {
    const tier = input.tier;
    const reward = Object.prototype.hasOwnProperty.call(BUG_REPORT_TIERS, tier)
      ? BUG_REPORT_TIERS[tier]
      : null;
    if (!reward) return fail('invalid-argument', 'Geçersiz ödül kademesi.');
    return {
      ok: true,
      patch: { status: 'approved', tier, reward: Object.assign({}, reward), rewardClaimed: false, reviewNote: note },
    };
  }
  if (decision === 'reject') {
    const reason = input.rejectReason;
    if (!BUG_REPORT_REJECT_REASONS.includes(reason)) {
      return fail('invalid-argument', 'Geçersiz ret gerekçesi.');
    }
    return { ok: true, patch: { status: 'rejected', rejectReason: reason, reviewNote: note } };
  }
  return fail('invalid-argument', 'Geçersiz karar.');
}

/**
 * Kullanıcı onaylanan bildirimin ödülünü toplayabilir mi.
 * @returns `{ ok: true, reward: { gold, keys, credits } }` ya da hata.
 */
function claimDecision(report, uid) {
  if (!report || report.uid !== uid) return fail('not-found', 'Bildirim bulunamadı.');
  if (report.status !== 'approved') {
    return fail('failed-precondition', 'Bu bildirim henüz onaylanmadı.');
  }
  if (report.rewardClaimed === true) {
    return fail('already-exists', 'Bu ödülü zaten aldın.');
  }
  const r = report.reward || {};
  const reward = {
    gold: Math.max(0, Math.trunc(Number(r.gold) || 0)),
    keys: Math.max(0, Math.trunc(Number(r.keys) || 0)),
    credits: Math.max(0, Math.trunc(Number(r.credits) || 0)),
  };
  return { ok: true, reward };
}

/** Karar bildiriminin metni. */
function decisionNoticeText(status) {
  if (status === 'approved') {
    return {
      title: 'Hata bildirimin onaylandı! 🎉',
      body: 'Teşekkürler! Ödülün seni bekliyor, Görevler\'den topla.',
    };
  }
  return {
    title: 'Hata bildirimin incelendi',
    body: 'Bildirimin için teşekkürler. Ayrıntıları Görevler\'de görebilirsin.',
  };
}

module.exports = {
  BUG_REPORT_TIERS,
  BUG_REPORT_REJECT_REASONS,
  BUG_REPORT_TEXT_MIN,
  BUG_REPORT_TEXT_MAX,
  BUG_REPORT_NOTE_MAX,
  BUG_REPORT_MAX_PENDING,
  BUG_REPORT_DAILY_LIMIT,
  isOwnImagePath,
  sanitizeDevice,
  normalizeSubmission,
  submitDecision,
  reviewDecision,
  claimDecision,
  decisionNoticeText,
};

// Kaynak: MissionsProgressStore.kt (tamamı: görev seçimi, sayaçlar, sıfırlama, gün/hafta kimliği,
// güvenilir saat, bulut eşitleme durum makinesi, bulut belgesi eşlemesi).
// Farklar:
//  - Android'de `object` + Context idi; burada sınıf. SharedPreferences → [OyunKeyValueStore]
//    (dosya adı Android ile aynı: "missions_progress_<uid|guest>"), FirebaseAuth/Firestore →
//    [MissionCloudGateway], `System.currentTimeMillis()` / `SystemClock.elapsedRealtime()` /
//    yerel saat dilimi → yapıcı parametreleri.
//  - `java.util.Calendar` → kotlinx-datetime (gün/hafta kimlikleri Android ile AYNI biçimde üretilir;
//    bunlar Firestore'a da yazılır).
//  - `key.hashCode()` → [javaStringHashCode] (Java'nın String.hashCode algoritması açıkça yazıldı;
//    tohum platformdan bağımsız olsun diye).
// Android'deki hâliyle korunan tuhaflıklar:
//  - Hafta kimliğindeki ay 0 tabanlıdır (Calendar.MONTH): Ocak = 0. Örn. 1 Haziran 2026 → "2026-5-1".
//  - Gün kimliği "yıl-yılın günü"dür (örn. "2026-152"), sıfır doldurma yok.
//  - `hasVisibleMissionProgress` içinde `selectedMissionsFor…` çağrıları yan etkilidir (sıfırlama,
//    buluttan okuma, gerekirse yeni seçim yazma).
package com.sorobit.shared.oyun

import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.datetime.TimeZone

/**
 * `users/{uid}/missionProgress/state` belgesine erişim (Android: FirebaseAuth + Firestore).
 * Mantık [MissionsProgressStore] içindedir; bu arayüz yalnızca G/Ç'dir.
 */
interface MissionCloudGateway {
    /** `FirebaseAuth.getInstance().currentUser?.uid` */
    fun currentUid(): String?

    /** `stateDoc(uid).get()` — eşzamansız. Belge yoksa `onSuccess(null)`. */
    fun fetchState(uid: String, onSuccess: (Map<String, Any?>?) -> Unit, onFailure: () -> Unit)

    /**
     * `Tasks.await(stateDoc(uid).get())` — engelleyen okuma. Hata durumunda istisna fırlatır
     * (çağıran yutar ve yerel veriyle devam eder). Engelleyen okuma yapılamayan platformda
     * istisna fırlatmak yeterlidir; eşzamansız [fetchState] yine denenir.
     */
    fun fetchStateBlocking(uid: String): Map<String, Any?>?

    /** `stateDoc(uid).set(payload)` — tüm belgeyi yazar (merge değil). */
    fun setState(uid: String, payload: Map<String, Any>)

    /**
     * `stateDoc(uid).addSnapshotListener` — hata olmayan her anlık görüntüde çağrılır
     * (belge yoksa null). Dönen fonksiyon dinleyiciyi kaldırır.
     */
    fun listenState(uid: String, onSnapshot: (Map<String, Any?>?) -> Unit): () -> Unit
}

/** Günlük / haftalık görev sayaçları ve görev seçimi yönetimi. */
class MissionsProgressStore(
    /** Verilen adla SharedPreferences dosyasının karşılığını döndürür. */
    private val prefsProvider: (prefsName: String) -> OyunKeyValueStore,
    private val cloud: MissionCloudGateway,
    /** `SystemClock.elapsedRealtime()` karşılığı: cihaz açılışından beri geçen, uyku dahil monotonik ms. */
    private val elapsedRealtimeMs: () -> Long,
    /** `System.currentTimeMillis()` */
    private val wallClockMs: () -> Long = ::oyunSimdiMs,
    /** `Calendar.getInstance()`'ın kullandığı yerel saat dilimi. */
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {

    @Volatile private var cloudSyncRequestedForUid: String? = null
    @Volatile private var cloudStateAppliedForUid: String? = null
    @Volatile private var lastSeenUid: String? = null
    @Volatile private var applyingCloudState = false
    private var missionRealtimeListener: (() -> Unit)? = null
    private var missionRealtimeUid: String? = null

    data class Snapshot(
        val dailyStepFinishCount: Int,
        val weeklyStepFinishCount: Int,
        val dailyStepIncrementCount: Int,
        val weeklyStepIncrementCount: Int,
        val dailyPerfectStepIncrementCount: Int,
        val weeklyPerfectStepIncrementCount: Int,
        val dailyChestRecordBreakCount: Int,
        val weeklyChestRecordBreakCount: Int,
        val dailyChestStarGainCount: Int,
        val weeklyChestStarGainCount: Int,
        val dailyLearnMinutesCount: Int,
        val weeklyLearnMinutesCount: Int,
    )

    private fun prefs(): OyunKeyValueStore {
        val uid = currentUid() ?: "guest"
        return prefsProvider("${PREFS}_$uid")
    }

    /**
     * Offline desteği kullanılmadığında local görev cache'ini temizler.
     * Sonraki okumada state cloud'dan tekrar hydrate edilir.
     */
    fun clearLocalCache() {
        val uid = currentUid()
        prefsProvider("${PREFS}_guest").clear()
        if (!uid.isNullOrBlank()) {
            prefsProvider("${PREFS}_$uid").clear()
        }
        cloudSyncRequestedForUid = null
        cloudStateAppliedForUid = null
        applyingCloudState = false
    }

    /**
     * Local state'i hiçbir hydrate/defer beklemeden direkt cloud'a yazar.
     * Reset sonrası eski cloud state'in geri basmasını engellemek için kullanılır.
     */
    fun forceUploadStateToCloud() {
        ensureMissionRealtimeSync()
        val uid = currentUid() ?: return
        val p = prefs()
        cloud.setState(uid, cloudPayload(p))
        cloudStateAppliedForUid = uid
    }

    /** Buluta yazılan belgenin tamamı (alan adları = prefs anahtarları + rewardClaimedKeys). */
    private fun cloudPayload(p: OyunKeyValueStore): Map<String, Any> {
        // Android'de varsayılan değerler argüman olarak hevesle hesaplanır (güvenilir saat güncellenir).
        val today = todayId()
        val week = weekIdNow()
        return hashMapOf<String, Any>(
            KEY_DAY_ID to (p.getString(KEY_DAY_ID) ?: today),
            KEY_WEEK_ID to (p.getString(KEY_WEEK_ID) ?: week),
            KEY_DAILY_STEP_FINISH_COUNT to p.getInt(KEY_DAILY_STEP_FINISH_COUNT, 0),
            KEY_WEEKLY_STEP_FINISH_COUNT to p.getInt(KEY_WEEKLY_STEP_FINISH_COUNT, 0),
            KEY_DAILY_STEP_INCREMENT_COUNT to p.getInt(KEY_DAILY_STEP_INCREMENT_COUNT, 0),
            KEY_WEEKLY_STEP_INCREMENT_COUNT to p.getInt(KEY_WEEKLY_STEP_INCREMENT_COUNT, 0),
            KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT to p.getInt(KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT, 0),
            KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT to p.getInt(KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT, 0),
            KEY_DAILY_CHEST_RECORD_BREAK_COUNT to p.getInt(KEY_DAILY_CHEST_RECORD_BREAK_COUNT, 0),
            KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT to p.getInt(KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT, 0),
            KEY_DAILY_CHEST_STAR_GAIN_COUNT to p.getInt(KEY_DAILY_CHEST_STAR_GAIN_COUNT, 0),
            KEY_WEEKLY_CHEST_STAR_GAIN_COUNT to p.getInt(KEY_WEEKLY_CHEST_STAR_GAIN_COUNT, 0),
            KEY_DAILY_LEARN_MINUTES_COUNT to p.getInt(KEY_DAILY_LEARN_MINUTES_COUNT, 0),
            KEY_WEEKLY_LEARN_MINUTES_COUNT to p.getInt(KEY_WEEKLY_LEARN_MINUTES_COUNT, 0),
            KEY_DAILY_LEARN_REMAINDER_MS to p.getLong(KEY_DAILY_LEARN_REMAINDER_MS, 0L),
            KEY_WEEKLY_LEARN_REMAINDER_MS to p.getLong(KEY_WEEKLY_LEARN_REMAINDER_MS, 0L),
            KEY_DAILY_SELECTION_NONCE to p.getInt(KEY_DAILY_SELECTION_NONCE, 0),
            KEY_WEEKLY_SELECTION_NONCE to p.getInt(KEY_WEEKLY_SELECTION_NONCE, 0),
            KEY_DAILY_SELECTED_MISSION_IDS to (p.getString(KEY_DAILY_SELECTED_MISSION_IDS) ?: ""),
            KEY_WEEKLY_SELECTED_MISSION_IDS to (p.getString(KEY_WEEKLY_SELECTED_MISSION_IDS) ?: ""),
            FIELD_REWARD_CLAIMED_KEYS to rewardClaimKeys(p),
        )
    }

    private fun ensureSyncStateForCurrentUid() {
        val uid = currentUid()
        if (uid != lastSeenUid) {
            lastSeenUid = uid
            cloudSyncRequestedForUid = null
            cloudStateAppliedForUid = null
            applyingCloudState = false
            missionRealtimeListener?.invoke()
            missionRealtimeListener = null
            missionRealtimeUid = null
        }
    }

    private fun currentUid(): String? = cloud.currentUid()

    private fun ensureMissionRealtimeSync() {
        ensureSyncStateForCurrentUid()
        val uid = currentUid() ?: return
        if (missionRealtimeListener != null && missionRealtimeUid == uid) return
        missionRealtimeListener?.invoke()
        missionRealtimeListener = null
        missionRealtimeUid = uid
        missionRealtimeListener = cloud.listenState(uid) { data ->
            applyCloudDocument(uid, data)
        }
    }

    /** Yerel takvim günü anahtarı (günlük görevler ve roket günlük sayacı ile aynı). */
    fun calendarDayId(): String = dayId(wallClockMs(), timeZone())

    /**
     * Cihaz saatinin elle ileri/geri alınmasına karşı dayanıklı "şimdi" değeri.
     * Monotonik saat cihaz saati değiştirilse de (uyku dahil) akar; duvar saati bu değerden makul
     * bir tolerans (±5dk) dışına sapıyorsa duvar saati yerine son bilinen güvenilir zaman + geçen
     * gerçek süre kullanılır. Böylece kullanıcı tarihi ileri/geri alarak günlük/haftalık görev ve
     * ödülleri birden fazla kez tetikleyemez.
     */
    private fun trustedNowMs(): Long {
        val p = prefs()
        val nowWall = wallClockMs()
        val nowElapsed = elapsedRealtimeMs()
        val lastTrusted = p.getLong(KEY_TRUSTED_NOW_MS, 0L)
        val lastElapsed = p.getLong(KEY_TRUSTED_ANCHOR_ELAPSED_MS, 0L)

        val trusted = computeTrustedNowMs(nowWall, nowElapsed, lastTrusted, lastElapsed)

        p.putLong(KEY_TRUSTED_NOW_MS, trusted)
        p.putLong(KEY_TRUSTED_ANCHOR_ELAPSED_MS, nowElapsed)
        return trusted
    }

    private fun todayId(): String = dayId(trustedNowMs(), timeZone())

    /** Bu haftanın Pazartesi tarihini (yerel) benzersiz anahtar olarak kullanır */
    private fun weekIdNow(): String = weekId(trustedNowMs(), timeZone())

    private fun rewardClaimKey(window: MissionWindow, missionId: String): String {
        val periodId = when (window) {
            MissionWindow.DAILY -> todayId()
            MissionWindow.WEEKLY -> weekIdNow()
        }
        return rewardClaimKeyFor(window, periodId, missionId)
    }

    private fun clearRewardClaimFlags(p: OyunKeyValueStore) {
        val keysToRemove = p.keys().filter { key ->
            key.startsWith("reward_claimed_daily_") || key.startsWith("reward_claimed_weekly_")
        }
        keysToRemove.forEach { p.remove(it) }
    }

    private fun rewardClaimKeys(p: OyunKeyValueStore): List<String> =
        p.keys().filter { it.startsWith("reward_claimed_daily_") || it.startsWith("reward_claimed_weekly_") }

    private fun missionIdsKeyForWindow(window: MissionWindow): String = when (window) {
        MissionWindow.DAILY -> KEY_DAILY_SELECTED_MISSION_IDS
        MissionWindow.WEEKLY -> KEY_WEEKLY_SELECTED_MISSION_IDS
    }

    private fun uploadStateToCloud() {
        ensureMissionRealtimeSync()
        ensureSyncStateForCurrentUid()
        if (applyingCloudState) return
        val uid = currentUid() ?: return
        if (cloudStateAppliedForUid != uid) {
            requestCloudSync()
            return
        }
        val p = prefs()
        cloud.setState(uid, cloudPayload(p))
    }

    /**
     * UI okumalarından önce cloud state'i prefs'e işler.
     * Aksi halde `getSnapshot()` gibi fonksiyonlar `requestCloudSync()`'i tetikleyip
     * hemen ardından hâlâ eski local değerleri döndürebiliyor.
     */
    private fun hydrateFromServerIfNeededBlocking() {
        ensureMissionRealtimeSync()
        val uid = currentUid() ?: return
        if (cloudStateAppliedForUid == uid) return
        val data = try {
            cloud.fetchStateBlocking(uid)
        } catch (_: Exception) {
            // Ağ/izin hatasında local devam eder; async `requestCloudSync` yine denenebilir.
            return
        }
        applyCloudDocument(uid, data)
    }

    private fun applyCloudDocument(uid: String, data: Map<String, Any?>?) {
        if (data == null) {
            cloudStateAppliedForUid = uid
            cloudSyncRequestedForUid = null
            uploadStateToCloud()
            return
        }

        applyingCloudState = true
        val p = prefs()
        // Android'de tek bir Editor ile toplu yazılır; okumalar (intOf/longOf yedekleri) yazımdan
        // ÖNCEKİ değerleri görür. Bu yüzden önce tüm değerler hesaplanır, sonra yazılır.
        fun intOf(key: String) = (data[key] as? Number)?.toInt() ?: p.getInt(key, 0)
        fun longOf(key: String) = (data[key] as? Number)?.toLong() ?: p.getLong(key, 0L)
        fun strOf(key: String, fallback: String) = data[key] as? String ?: fallback
        val dayIdValue = strOf(KEY_DAY_ID, todayId())
        val weekIdValue = strOf(KEY_WEEK_ID, weekIdNow())
        val intValues = INT_STATE_KEYS.map { it to intOf(it) }
        val longValues = LONG_STATE_KEYS.map { it to longOf(it) }
        val dailyIds = data[KEY_DAILY_SELECTED_MISSION_IDS] as? String ?: ""
        val weeklyIds = data[KEY_WEEKLY_SELECTED_MISSION_IDS] as? String ?: ""
        val claimedKeys = (data[FIELD_REWARD_CLAIMED_KEYS] as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()

        clearRewardClaimFlags(p)
        p.putString(KEY_DAY_ID, dayIdValue)
        p.putString(KEY_WEEK_ID, weekIdValue)
        intValues.forEach { (k, v) -> p.putInt(k, v) }
        longValues.forEach { (k, v) -> p.putLong(k, v) }
        p.putString(KEY_DAILY_SELECTED_MISSION_IDS, dailyIds)
        p.putString(KEY_WEEKLY_SELECTED_MISSION_IDS, weeklyIds)
        claimedKeys.forEach { p.putBoolean(it, true) }
        applyingCloudState = false
        cloudStateAppliedForUid = uid
        cloudSyncRequestedForUid = null
        ensureResets()
    }

    private fun requestCloudSync() {
        ensureMissionRealtimeSync()
        ensureSyncStateForCurrentUid()
        val uid = currentUid() ?: return
        if (cloudStateAppliedForUid == uid || cloudSyncRequestedForUid == uid) return
        cloudSyncRequestedForUid = uid
        cloud.fetchState(
            uid,
            onSuccess = { data ->
                applyCloudDocument(uid, data)
            },
            onFailure = {
                cloudSyncRequestedForUid = null
                applyingCloudState = false
            },
        )
    }

    private fun selectedMissionIdsForWindow(window: MissionWindow): List<String> {
        val p = prefs()
        val keyForStoredIds = missionIdsKeyForWindow(window)
        val allowedMissionIds = MissionQuestCatalog.all
            .filter { window in it.availableWindows }
            .map { it.id }
            .toSet()
        val stored = parseMissionIdsCsv(p.getString(keyForStoredIds))
            .filter { it in allowedMissionIds }
            .distinct()
            .take(3)
        if (stored.size == 3) return stored

        val key = when (window) {
            MissionWindow.DAILY -> "${todayId()}_${p.getInt(KEY_DAILY_SELECTION_NONCE, 0)}"
            MissionWindow.WEEKLY -> "${weekIdNow()}_${p.getInt(KEY_WEEKLY_SELECTION_NONCE, 0)}"
        }
        val generated = generateMissionIds(window, key)
        p.putString(keyForStoredIds, missionIdsCsv(generated))
        uploadStateToCloud()
        return generated
    }

    fun selectedMissionsForWindow(window: MissionWindow): List<MissionQuestDefinition> {
        ensureResets()
        hydrateFromServerIfNeededBlocking()
        requestCloudSync()
        val missionById = MissionQuestCatalog.all.associateBy { it.id }
        return selectedMissionIdsForWindow(window).mapNotNull { missionById[it] }
    }

    fun selectedMissionsForDaily(): List<MissionQuestDefinition> =
        selectedMissionsForWindow(MissionWindow.DAILY)

    fun selectedMissionsForWeekly(): List<MissionQuestDefinition> =
        selectedMissionsForWindow(MissionWindow.WEEKLY)

    fun missionProgress(snapshot: Snapshot, window: MissionWindow, mission: MissionQuestDefinition): Int =
        Companion.missionProgress(snapshot, window, mission)

    fun isMissionRewardClaimed(window: MissionWindow, missionId: String): Boolean {
        ensureResets()
        hydrateFromServerIfNeededBlocking()
        requestCloudSync()
        return prefs().getBoolean(rewardClaimKey(window, missionId), false)
    }

    fun markMissionRewardClaimed(window: MissionWindow, missionId: String) {
        ensureResets()
        prefs().putBoolean(rewardClaimKey(window, missionId), true)
        uploadStateToCloud()
    }

    fun ensureResets() {
        val p = prefs()
        val t = todayId()
        val dayChanged = p.getString(KEY_DAY_ID) != t
        val w = weekIdNow()
        val weekChanged = p.getString(KEY_WEEK_ID) != w
        if (dayChanged) {
            p.putString(KEY_DAY_ID, t)
            p.putInt(KEY_DAILY_STEP_FINISH_COUNT, 0)
            p.putInt(KEY_DAILY_STEP_INCREMENT_COUNT, 0)
            p.putInt(KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT, 0)
            p.putInt(KEY_DAILY_CHEST_RECORD_BREAK_COUNT, 0)
            p.putInt(KEY_DAILY_CHEST_STAR_GAIN_COUNT, 0)
            p.putInt(KEY_DAILY_LEARN_MINUTES_COUNT, 0)
            p.putLong(KEY_DAILY_LEARN_REMAINDER_MS, 0L)
            p.remove(KEY_DAILY_SELECTED_MISSION_IDS)
        }
        if (weekChanged) {
            p.putString(KEY_WEEK_ID, w)
            p.putInt(KEY_WEEKLY_STEP_FINISH_COUNT, 0)
            p.putInt(KEY_WEEKLY_STEP_INCREMENT_COUNT, 0)
            p.putInt(KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT, 0)
            p.putInt(KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT, 0)
            p.putInt(KEY_WEEKLY_CHEST_STAR_GAIN_COUNT, 0)
            p.putInt(KEY_WEEKLY_LEARN_MINUTES_COUNT, 0)
            p.putLong(KEY_WEEKLY_LEARN_REMAINDER_MS, 0L)
            p.remove(KEY_WEEKLY_SELECTED_MISSION_IDS)
        }
    }

    /**
     * @param applyCloudHydrate false: claim sırasında az önce yazılan local sayaçları Firestore hydrate ile ezme.
     */
    fun getSnapshot(applyCloudHydrate: Boolean = true): Snapshot {
        ensureResets()
        if (applyCloudHydrate) {
            hydrateFromServerIfNeededBlocking()
            requestCloudSync()
        }
        val p = prefs()
        return Snapshot(
            dailyStepFinishCount = p.getInt(KEY_DAILY_STEP_FINISH_COUNT, 0),
            weeklyStepFinishCount = p.getInt(KEY_WEEKLY_STEP_FINISH_COUNT, 0),
            dailyStepIncrementCount = p.getInt(KEY_DAILY_STEP_INCREMENT_COUNT, 0),
            weeklyStepIncrementCount = p.getInt(KEY_WEEKLY_STEP_INCREMENT_COUNT, 0),
            dailyPerfectStepIncrementCount = p.getInt(KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT, 0),
            weeklyPerfectStepIncrementCount = p.getInt(KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT, 0),
            dailyChestRecordBreakCount = p.getInt(KEY_DAILY_CHEST_RECORD_BREAK_COUNT, 0),
            weeklyChestRecordBreakCount = p.getInt(KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT, 0),
            dailyChestStarGainCount = p.getInt(KEY_DAILY_CHEST_STAR_GAIN_COUNT, 0),
            weeklyChestStarGainCount = p.getInt(KEY_WEEKLY_CHEST_STAR_GAIN_COUNT, 0),
            dailyLearnMinutesCount = p.getInt(KEY_DAILY_LEARN_MINUTES_COUNT, 0),
            weeklyLearnMinutesCount = p.getInt(KEY_WEEKLY_LEARN_MINUTES_COUNT, 0),
        )
    }

    /** Sandık talebinden sonra çubukta görünür bir değişim oldu mu (tam dolu sayaçlarda artış görünmez). */
    fun hasVisibleMissionProgress(before: Snapshot, after: Snapshot): Boolean {
        val visibleDaily = selectedMissionsForDaily().any { mission ->
            val beforeValue = missionProgress(before, MissionWindow.DAILY, mission).coerceAtMost(mission.target)
            val afterValue = missionProgress(after, MissionWindow.DAILY, mission).coerceAtMost(mission.target)
            beforeValue != afterValue
        }
        if (visibleDaily) return true
        return selectedMissionsForWeekly().any { mission ->
            val beforeValue = missionProgress(before, MissionWindow.WEEKLY, mission).coerceAtMost(mission.target)
            val afterValue = missionProgress(after, MissionWindow.WEEKLY, mission).coerceAtMost(mission.target)
            beforeValue != afterValue
        }
    }

    fun forceReselectMissions() {
        ensureResets()
        val p = prefs()
        val dailyNonce = p.getInt(KEY_DAILY_SELECTION_NONCE, 0) + 1
        val weeklyNonce = p.getInt(KEY_WEEKLY_SELECTION_NONCE, 0) + 1
        p.putInt(KEY_DAILY_SELECTION_NONCE, dailyNonce)
        p.putInt(KEY_WEEKLY_SELECTION_NONCE, weeklyNonce)
        p.remove(KEY_DAILY_SELECTED_MISSION_IDS)
        p.remove(KEY_WEEKLY_SELECTED_MISSION_IDS)
        uploadStateToCloud()
    }

    private fun incrementBoth(dailyKey: String, weeklyKey: String, amount: Int) {
        val p = prefs()
        val daily = p.getInt(dailyKey, 0) + amount
        val weekly = p.getInt(weeklyKey, 0) + amount
        p.putInt(dailyKey, daily)
        p.putInt(weeklyKey, weekly)
    }

    fun recordStepFinishProgress() {
        ensureResets()
        incrementBoth(KEY_DAILY_STEP_FINISH_COUNT, KEY_WEEKLY_STEP_FINISH_COUNT, 1)
        uploadStateToCloud()
    }

    fun recordStepIncrementProgress() {
        ensureResets()
        incrementBoth(KEY_DAILY_STEP_INCREMENT_COUNT, KEY_WEEKLY_STEP_INCREMENT_COUNT, 1)
        uploadStateToCloud()
    }

    fun recordPerfectStepIncrementProgress() {
        ensureResets()
        incrementBoth(KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT, KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT, 1)
        uploadStateToCloud()
    }

    fun recordChestRecordBreakProgress() {
        ensureResets()
        incrementBoth(KEY_DAILY_CHEST_RECORD_BREAK_COUNT, KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT, 1)
        uploadStateToCloud()
    }

    fun recordChestStarGainProgress(amount: Int = 1) {
        if (amount <= 0) return
        ensureResets()
        incrementBoth(KEY_DAILY_CHEST_STAR_GAIN_COUNT, KEY_WEEKLY_CHEST_STAR_GAIN_COUNT, amount)
        uploadStateToCloud()
    }

    fun recordLearningDurationMs(elapsedMs: Long) {
        if (elapsedMs <= 0L) return
        ensureResets()
        val p = prefs()
        val daily = p.getLong(KEY_DAILY_LEARN_REMAINDER_MS, 0L) + elapsedMs
        val weekly = p.getLong(KEY_WEEKLY_LEARN_REMAINDER_MS, 0L) + elapsedMs
        p.putLong(KEY_DAILY_LEARN_REMAINDER_MS, daily)
        p.putLong(KEY_WEEKLY_LEARN_REMAINDER_MS, weekly)
        uploadStateToCloud()
    }

    /**
     * Öğrenme süresi birikimini dakikaya çevirip görev sayaçlarına işler.
     * Bu fonksiyon sandık ödül akışı sırasında çağrılarak ilerlemenin
     * görev ekranında toplu görünmesini sağlar.
     */
    fun applyPendingLearningMinutes() {
        ensureResets()
        val p = prefs()
        val dailyTotalMs = p.getLong(KEY_DAILY_LEARN_REMAINDER_MS, 0L)
        val weeklyTotalMs = p.getLong(KEY_WEEKLY_LEARN_REMAINDER_MS, 0L)
        val dailyMinuteGain = (dailyTotalMs / 60_000L).toInt()
        val weeklyMinuteGain = (weeklyTotalMs / 60_000L).toInt()
        if (dailyMinuteGain <= 0 && weeklyMinuteGain <= 0) return
        val dailyMinutes = p.getInt(KEY_DAILY_LEARN_MINUTES_COUNT, 0) + dailyMinuteGain
        val weeklyMinutes = p.getInt(KEY_WEEKLY_LEARN_MINUTES_COUNT, 0) + weeklyMinuteGain
        p.putInt(KEY_DAILY_LEARN_MINUTES_COUNT, dailyMinutes)
        p.putInt(KEY_WEEKLY_LEARN_MINUTES_COUNT, weeklyMinutes)
        p.putLong(KEY_DAILY_LEARN_REMAINDER_MS, dailyTotalMs % 60_000L)
        p.putLong(KEY_WEEKLY_LEARN_REMAINDER_MS, weeklyTotalMs % 60_000L)
        uploadStateToCloud()
    }

    /** Günlük ve haftalık görev sayaçlarını sıfırlar. */
    fun resetAllProgress() {
        val p = prefs()
        val today = todayId()
        val week = weekIdNow()
        clearRewardClaimFlags(p)
        p.putString(KEY_DAY_ID, today)
        p.putString(KEY_WEEK_ID, week)
        INT_COUNTER_KEYS.forEach { p.putInt(it, 0) }
        p.putLong(KEY_DAILY_LEARN_REMAINDER_MS, 0L)
        p.putLong(KEY_WEEKLY_LEARN_REMAINDER_MS, 0L)
        p.remove(KEY_DAILY_SELECTED_MISSION_IDS)
        p.remove(KEY_WEEKLY_SELECTED_MISSION_IDS)
        uploadStateToCloud()
    }

    /** Bir sonraki yerel gece yarısına kalan tam saat (en az 1) */
    fun hoursUntilDailyReset(): Int = hoursUntilDailyReset(trustedNowMs(), timeZone())

    /**
     * Haftalık pencere sonu: bu haftanın Pazartesi 00:00 + 7 gün.
     * Kalan süre 24 saatten fazlaysa gün, değilse saat metni için kullanılır.
     */
    fun millisUntilWeeklyReset(): Long = millisUntilWeeklyReset(trustedNowMs(), timeZone())

    companion object {
        const val PREFS = "missions_progress"
        const val KEY_DAY_ID = "day_id"
        const val KEY_WEEK_ID = "week_id"
        const val KEY_DAILY_STEP_FINISH_COUNT = "daily_step_finish_count"
        const val KEY_WEEKLY_STEP_FINISH_COUNT = "weekly_step_finish_count"
        const val KEY_DAILY_STEP_INCREMENT_COUNT = "daily_step_increment_count"
        const val KEY_WEEKLY_STEP_INCREMENT_COUNT = "weekly_step_increment_count"
        const val KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT = "daily_perfect_step_increment_count"
        const val KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT = "weekly_perfect_step_increment_count"
        const val KEY_DAILY_CHEST_RECORD_BREAK_COUNT = "daily_chest_record_break_count"
        const val KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT = "weekly_chest_record_break_count"
        const val KEY_DAILY_CHEST_STAR_GAIN_COUNT = "daily_chest_star_gain_count"
        const val KEY_WEEKLY_CHEST_STAR_GAIN_COUNT = "weekly_chest_star_gain_count"
        const val KEY_DAILY_LEARN_MINUTES_COUNT = "daily_learn_minutes_count"
        const val KEY_WEEKLY_LEARN_MINUTES_COUNT = "weekly_learn_minutes_count"
        const val KEY_DAILY_LEARN_REMAINDER_MS = "daily_learn_remainder_ms"
        const val KEY_WEEKLY_LEARN_REMAINDER_MS = "weekly_learn_remainder_ms"
        const val KEY_DAILY_SELECTION_NONCE = "daily_selection_nonce"
        const val KEY_WEEKLY_SELECTION_NONCE = "weekly_selection_nonce"
        const val KEY_DAILY_SELECTED_MISSION_IDS = "daily_selected_mission_ids"
        const val KEY_WEEKLY_SELECTED_MISSION_IDS = "weekly_selected_mission_ids"
        const val FIRESTORE_COLLECTION = "missionProgress"
        const val FIRESTORE_DOC_STATE = "state"
        const val FIELD_REWARD_CLAIMED_KEYS = "rewardClaimedKeys"
        const val KEY_TRUSTED_NOW_MS = "trusted_now_ms"
        const val KEY_TRUSTED_ANCHOR_ELAPSED_MS = "trusted_anchor_elapsed_ms"
        const val CLOCK_TOLERANCE_MS = 5 * 60 * 1000L

        /** `users/{uid}/missionProgress/state` */
        fun stateDocPath(uid: String): String = "users/$uid/$FIRESTORE_COLLECTION/$FIRESTORE_DOC_STATE"

        /** Sayaç anahtarları (resetAllProgress'in sıfırladıkları). */
        private val INT_COUNTER_KEYS = listOf(
            KEY_DAILY_STEP_FINISH_COUNT,
            KEY_WEEKLY_STEP_FINISH_COUNT,
            KEY_DAILY_STEP_INCREMENT_COUNT,
            KEY_WEEKLY_STEP_INCREMENT_COUNT,
            KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT,
            KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT,
            KEY_DAILY_CHEST_RECORD_BREAK_COUNT,
            KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT,
            KEY_DAILY_CHEST_STAR_GAIN_COUNT,
            KEY_WEEKLY_CHEST_STAR_GAIN_COUNT,
            KEY_DAILY_LEARN_MINUTES_COUNT,
            KEY_WEEKLY_LEARN_MINUTES_COUNT,
        )

        /** Bulut belgesinden Int olarak okunan alanlar (sayaçlar + seçim nonce'ları). */
        private val INT_STATE_KEYS = INT_COUNTER_KEYS + listOf(
            KEY_DAILY_SELECTION_NONCE,
            KEY_WEEKLY_SELECTION_NONCE,
        )

        private val LONG_STATE_KEYS = listOf(
            KEY_DAILY_LEARN_REMAINDER_MS,
            KEY_WEEKLY_LEARN_REMAINDER_MS,
        )

        /** `trustedNowMs` içindeki saf karar. */
        fun computeTrustedNowMs(nowWall: Long, nowElapsed: Long, lastTrusted: Long, lastElapsed: Long): Long =
            when {
                lastTrusted == 0L -> nowWall
                nowElapsed < lastElapsed -> {
                    // Cihaz yeniden başlatılmış (monotonik saat sıfırlandı); monotonik karşılaştırma
                    // güvenilir değil. Duvar saatine güven ama geriye gitmesine izin verme.
                    maxOf(nowWall, lastTrusted)
                }
                else -> {
                    val monotonicNow = lastTrusted + (nowElapsed - lastElapsed)
                    if (abs(nowWall - monotonicNow) <= CLOCK_TOLERANCE_MS) nowWall else monotonicNow
                }
            }

        /** "yıl-yılınGünü" (Calendar.YEAR + "-" + Calendar.DAY_OF_YEAR), yerel saat dilimiyle. */
        fun dayId(epochMs: Long, timeZone: TimeZone): String {
            val d = yerelTarih(epochMs, timeZone)
            return "${d.year}-${d.dayOfYear}"
        }

        /**
         * Haftanın Pazartesi gününün "yıl-ay-gün" anahtarı; ay Calendar.MONTH gibi 0 TABANLIDIR
         * (Ocak = 0). Firestore'daki `week_id` ile aynı kalması için bu biçim korunmalıdır.
         */
        fun weekId(epochMs: Long, timeZone: TimeZone): String {
            val monday = haftaninPazartesisi(yerelTarih(epochMs, timeZone))
            return "${monday.year}-${monday.monthNumber - 1}-${monday.dayOfMonth}"
        }

        fun hoursUntilDailyReset(nowMs: Long, timeZone: TimeZone): Int {
            val next = gunBaslangiciMs(yerelTarih(nowMs, timeZone).gunEkle(1), timeZone)
            val diff = next - nowMs
            return (diff / (1000 * 60 * 60)).toInt().coerceAtLeast(1)
        }

        fun millisUntilWeeklyReset(nowMs: Long, timeZone: TimeZone): Long {
            val monday = haftaninPazartesisi(yerelTarih(nowMs, timeZone))
            val end = gunBaslangiciMs(monday.gunEkle(7), timeZone)
            return (end - nowMs).coerceAtLeast(0L)
        }

        fun rewardClaimKeyFor(window: MissionWindow, periodId: String, missionId: String): String {
            val prefix = when (window) {
                MissionWindow.DAILY -> "reward_claimed_daily"
                MissionWindow.WEEKLY -> "reward_claimed_weekly"
            }
            return "${prefix}_${periodId}_$missionId"
        }

        fun parseMissionIdsCsv(csv: String?): List<String> =
            csv?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

        fun missionIdsCsv(ids: List<String>): String = ids.joinToString(",")

        /** Java `String.hashCode()` (s[0]*31^(n-1) + … + s[n-1], 32 bit taşmalı). */
        fun javaStringHashCode(s: String): Int {
            var h = 0
            for (ch in s) {
                h = 31 * h + ch.code
            }
            return h
        }

        /**
         * Pencere için 3 görev seçer: katalog sırası, [seedKey]'in hash'iyle tohumlanan
         * `kotlin.random.Random` ile karıştırılır ve ilk 3'ü alınır.
         * [seedKey]: "<günKimliği>_<nonce>" ya da "<haftaKimliği>_<nonce>".
         */
        fun generateMissionIds(window: MissionWindow, seedKey: String): List<String> {
            val random = Random(javaStringHashCode(seedKey))
            return MissionQuestCatalog.all
                .filter { window in it.availableWindows }
                .shuffled(random)
                .take(3)
                .map { it.id }
        }

        fun readCounter(snapshot: Snapshot, window: MissionWindow, counterType: MissionCounterType): Int {
            return when (window) {
                MissionWindow.DAILY -> when (counterType) {
                    MissionCounterType.STEP_FINISH -> snapshot.dailyStepFinishCount
                    MissionCounterType.STEP_INCREMENT -> snapshot.dailyStepIncrementCount
                    MissionCounterType.PERFECT_STEP_INCREMENT -> snapshot.dailyPerfectStepIncrementCount
                    MissionCounterType.CHEST_RECORD_BREAK -> snapshot.dailyChestRecordBreakCount
                    MissionCounterType.CHEST_STAR_GAIN -> snapshot.dailyChestStarGainCount
                    MissionCounterType.LEARN_MINUTES -> snapshot.dailyLearnMinutesCount
                }
                MissionWindow.WEEKLY -> when (counterType) {
                    MissionCounterType.STEP_FINISH -> snapshot.weeklyStepFinishCount
                    MissionCounterType.STEP_INCREMENT -> snapshot.weeklyStepIncrementCount
                    MissionCounterType.PERFECT_STEP_INCREMENT -> snapshot.weeklyPerfectStepIncrementCount
                    MissionCounterType.CHEST_RECORD_BREAK -> snapshot.weeklyChestRecordBreakCount
                    MissionCounterType.CHEST_STAR_GAIN -> snapshot.weeklyChestStarGainCount
                    MissionCounterType.LEARN_MINUTES -> snapshot.weeklyLearnMinutesCount
                }
            }
        }

        fun missionProgress(snapshot: Snapshot, window: MissionWindow, mission: MissionQuestDefinition): Int {
            return readCounter(snapshot, window, mission.counterType)
        }
    }
}

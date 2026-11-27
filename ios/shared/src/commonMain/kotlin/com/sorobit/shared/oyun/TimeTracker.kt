// Kaynak: TimeTracker.kt (tamamı: süre biriktirme, çökme sonrası kurtarma, eşitleme kuralı).
// Farklar:
//  - Android'de `object` + `initialize(context)` idi; burada sınıf. SharedPreferences
//    ("time_tracker_prefs") → [OyunKeyValueStore]; 10 saniyelik ScheduledExecutor ve Firestore
//    yazımı → [TimeTrackerPlatform]. Zamanlayıcı her 10 saniyede [onPeriodicTick] çağırır.
//  - `StateFlow<Long>` (kotlinx-coroutines bağımlılığı yok) → [unsyncedTime] + [onUnsyncedTimeChanged].
//  - Log çağrıları kaldırıldı.
// Android (güncel): her eşitleme günlük süre haritasını da (`dailyTimeSpent.<yyyyMMdd>`) aynı yazmaya bindirir ve
// [TimeTracker.RETENTION_DAYS] gün öncesinin anahtarını siler (veli paneli okur). Ortak kod bunun saf kısmını verir
// ([TimeTracker.buildSyncWrite], [TimeTracker.dayKey]); yazma [TimeTrackerPlatform.incrementTotalTimeSpent] içinde
// platform tarafından bu üç alanla birlikte yapılmalıdır — arayüz imzası değişmedi.
// Android'deki hâliyle korunan notlar:
//  - Eşitlemede yerel sayaç, Firestore yazımı BAŞLATILIRKEN sıfırlanır; yazım başarısız olursa süre
//    geri yüklenmez (Android, Firestore'un çevrimdışı kuyruğuna güvenir).
//  - Oturum yokken biriken süre yerelde kalır ve bir sonraki eşitlemede o an oturumu açık olan
//    kullanıcıya yazılır.
//  - Çökme kurtarmasında 1 saat ve üzeri (ya da negatif/0) fark yok sayılır.
package com.sorobit.shared.oyun

interface TimeTrackerPlatform {
    /** Oturumu açık kullanıcının kimliği; yoksa null. */
    fun currentUid(): String?

    /** `users/{uid}.totalTimeSpent` alanını `FieldValue.increment(seconds)` ile artırır. */
    fun incrementTotalTimeSpent(uid: String, seconds: Long)

    /** `users/{uid}.totalTimeSpent` alanını verilen değere yazar (sıfırlama). */
    fun setTotalTimeSpent(uid: String, value: Long)

    /** 10 saniyede bir [TimeTracker.onPeriodicTick] çağıran zamanlayıcıyı başlatır. */
    fun startPeriodicTick(intervalSeconds: Long)

    fun stopPeriodicTick()
}

class TimeTracker(
    /** Android: "time_tracker_prefs" adlı SharedPreferences. */
    private val prefs: OyunKeyValueStore,
    private val platform: TimeTrackerPlatform,
    private val nowMs: () -> Long = ::oyunSimdiMs,
) {
    companion object {
        const val PREFS_NAME = "time_tracker_prefs"
        const val KEY_UNSYNCED_TIME = "unsynced_time_seconds"
        const val KEY_LAST_START_TIME = "last_start_time"

        /** Firestore `users/{uid}` alanı (saniye). */
        const val FIELD_TOTAL_TIME_SPENT = "totalTimeSpent"

        const val TICK_INTERVAL_SECONDS = 10L

        /**
         * Kullanıcı dokümanındaki gün→saniye haritası (ör. {"20260911": 1840}); `users/{uid}` üzerinde ALAN olarak,
         * `totalTimeSpent` yazmasına biner (ek yazma maliyeti yok).
         */
        const val DAILY_FIELD = "dailyTimeSpent"

        /** Harita kaç gün geriye kadar tutulur; her eşitlemede bu kadar eski anahtar silinir. */
        const val RETENTION_DAYS = 14

        /**
         * Android `dayKey(offsetDays)`: "yyyyMMdd", cihazın YEREL saat diliminde ([offsetDays] negatifse geçmiş gün).
         * Tire/iki nokta içermez: Firestore alan adı olarak kullanılır.
         */
        fun dayKey(
            offsetDays: Int,
            nowMs: Long = oyunSimdiMs(),
            timeZone: kotlinx.datetime.TimeZone = kotlinx.datetime.TimeZone.currentSystemDefault(),
        ): String {
            val d = yerelTarih(nowMs, timeZone).gunEkle(offsetDays)
            return d.year.toString().padStart(4, '0') +
                d.monthNumber.toString().padStart(2, '0') +
                d.dayOfMonth.toString().padStart(2, '0')
        }

        /** Bir eşitlemenin Firestore'a yazacağı alanlar (Android `syncToFirestore` içindeki `update(...)`). */
        data class SyncWrite(
            /** `totalTimeSpent` için `FieldValue.increment` miktarı. */
            val totalIncrement: Long,
            /** `FieldPath.of(DAILY_FIELD, bugün)` — aynı miktarla artırılır. */
            val dailyFieldPath: List<String>,
            val dailyIncrement: Long,
            /** `FieldPath.of(DAILY_FIELD, bugün-14)` — `FieldValue.delete()`. */
            val expiredFieldPath: List<String>,
        )

        /**
         * Gün anahtarı senkronizasyon anına göre verilir: gece yarısını aşan bir oturumun tamamı ertesi güne yazılabilir
         * (veli raporu için önemsiz sapma).
         */
        fun buildSyncWrite(
            unsyncedSeconds: Long,
            nowMs: Long = oyunSimdiMs(),
            timeZone: kotlinx.datetime.TimeZone = kotlinx.datetime.TimeZone.currentSystemDefault(),
        ): SyncWrite = SyncWrite(
            totalIncrement = unsyncedSeconds,
            dailyFieldPath = listOf(DAILY_FIELD, dayKey(0, nowMs, timeZone)),
            dailyIncrement = unsyncedSeconds,
            expiredFieldPath = listOf(DAILY_FIELD, dayKey(-RETENTION_DAYS, nowMs, timeZone)),
        )

        /** Çökme kurtarmasında kabul edilen en uzun süre (saniye, hariç). */
        const val MAX_RECOVERABLE_SECONDS = 3600L
    }

    private var isTracking = false
    private var startTime: Long = 0

    /** Henüz buluta yazılmamış süre (saniye). Android: `unsyncedTimeFlow.value`. */
    var unsyncedTime: Long = 0L
        private set

    /** [unsyncedTime] her değiştiğinde çağrılır. */
    var onUnsyncedTimeChanged: ((Long) -> Unit)? = null

    private fun setUnsynced(value: Long) {
        unsyncedTime = value
        onUnsyncedTimeChanged?.invoke(value)
    }

    fun initialize() {
        setUnsynced(prefs.getLong(KEY_UNSYNCED_TIME, 0))

        // Uygulama kapanmadan önce takip başlamışsa, o süreyi de ekle
        val lastStartTime = prefs.getLong(KEY_LAST_START_TIME, 0)
        if (lastStartTime > 0) {
            val elapsedTime = (nowMs() - lastStartTime) / 1000
            if (elapsedTime > 0 && elapsedTime < MAX_RECOVERABLE_SECONDS) { // 1 saatten fazla değilse
                addUnsyncedTime(elapsedTime)
            }
            // Son başlangıç zamanını temizle
            prefs.remove(KEY_LAST_START_TIME)
        }

        // Başlangıçta eşitlenmemiş süre varsa buluta yolla
        syncToFirestore()
    }

    fun startTracking() {
        if (isTracking) {
            return
        }

        isTracking = true
        startTime = nowMs()

        // Başlangıç zamanını kaydet (çökme durumları için)
        prefs.putLong(KEY_LAST_START_TIME, startTime)

        // Her 10 saniyede bir süreyi yalnızca yerele ekle
        platform.startPeriodicTick(TICK_INTERVAL_SECONDS)
    }

    /** Zamanlayıcı tıkı (10 saniyede bir). */
    fun onPeriodicTick() {
        if (isTracking) {
            val currentTime = nowMs()
            val elapsedTime = (currentTime - startTime) / 1000
            if (elapsedTime >= 10) { // En az 10 saniye geçmişse
                addUnsyncedTime(elapsedTime)
                startTime = currentTime // Başlangıç zamanını güncelle
                prefs.putLong(KEY_LAST_START_TIME, startTime)
            }
        }
    }

    fun stopTracking() {
        if (!isTracking) {
            return
        }

        isTracking = false

        // Son süreyi ekle
        val elapsedTime = (nowMs() - startTime) / 1000
        if (elapsedTime > 0) {
            addUnsyncedTime(elapsedTime)
        }

        // Zamanlayıcıyı iptal et
        platform.stopPeriodicTick()

        // Son başlangıç zamanını temizle
        prefs.remove(KEY_LAST_START_TIME)

        // Uygulama arka plana geçtiği için Firestore'a tek seferlik eşitle
        syncToFirestore()
    }

    private fun addUnsyncedTime(seconds: Long) {
        if (seconds <= 0) return
        val currentUnsynced = prefs.getLong(KEY_UNSYNCED_TIME, 0)
        val newUnsynced = currentUnsynced + seconds
        prefs.putLong(KEY_UNSYNCED_TIME, newUnsynced)
        setUnsynced(newUnsynced)
    }

    private fun syncToFirestore() {
        val unsynced = prefs.getLong(KEY_UNSYNCED_TIME, 0)
        if (unsynced <= 0) return

        val uid = platform.currentUid()
        if (uid != null) {
            // Firestore işlemi başlatıldığı an yereldeki sayaç sıfırlanır.
            // Firestore çevrimdışı olsa bile isteği kuyruğa alır ve bağlantı gelince gönderir.
            // Böylece çift sayma ihtimali kalmaz.
            prefs.putLong(KEY_UNSYNCED_TIME, 0)
            setUnsynced(0L)

            platform.incrementTotalTimeSpent(uid, unsynced)
        }
    }

    fun reset() {
        prefs.putLong(KEY_UNSYNCED_TIME, 0)
        setUnsynced(0L)
        val uid = platform.currentUid()
        if (uid != null) {
            platform.setTotalTimeSpent(uid, 0)
        }
    }
}

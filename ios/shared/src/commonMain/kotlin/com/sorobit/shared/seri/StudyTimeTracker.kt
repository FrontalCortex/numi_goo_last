// Kaynak (Android): app/src/main/java/com/example/app/StudyTimeTracker.kt — tamamı.
//
// "Bugün ne kadar çalıştı" ölçümü: günlük serinin dayandığı sayaç. Yalnızca DERS ekranlarında ve
// uygulama öndeyken geçen süre toplanır; son dokunuştan sonra en fazla [MAX_IDLE_MS] yazılır.
//
// Farklar:
//  - Android `object` + SharedPreferences ("study_time"); burada sınıf + [OyunKeyValueStore] ve
//    enjekte edilen saat. Anahtar biçimi aynı: "seconds_<yyyy-MM-dd>".
//  - StreakDiag / Log satırları kaldırıldı.
//  - `addSecondsForDebug` Android'de yalnızca DEBUG derlemesinde çalışır; burada kapıyı çağıran taraf tutar.
package com.sorobit.shared.seri

import com.sorobit.shared.oyun.OyunKeyValueStore

class StudyTimeTracker(
    private val store: OyunKeyValueStore,
    private val clock: StreakClock,
) {
    companion object {
        const val KEY_PREFIX = "seconds_"

        /** Kaç günlük kayıt tutulsun. */
        const val KEEP_DAYS = 8

        /** Son dokunuştan sonra süre saymaya devam edilen en uzun aralık (5 dk). */
        const val MAX_IDLE_MS = 5L * 60 * 1000

        /** Çalışma sayılan ekranlar (Android fragment adları). */
        val STUDY_SCREENS: Set<String> = setOf(
            "AbacusFragment",
            "AbacusPracticeFragment",
            "BlindingLessonFragment",
            "TutorialFragment",
        )
    }

    /** Açık parçanın başlangıcı (monoton saat). 0 = sayım kapalı. */
    private var segmentStartMs = 0L

    /** Son kullanıcı etkileşimi (monoton saat). */
    private var lastInteractionMs = 0L

    /** Süre değiştiğinde haberdar olmak isteyen (üst bardaki alev). */
    var onChanged: (() -> Unit)? = null

    fun isStudyScreen(screen: String?): Boolean = screen != null && screen in STUDY_SCREENS

    /** Sayım şu an açık mı (bir ders ekranı önde). */
    fun isCounting(): Boolean = segmentStartMs > 0L

    /**
     * Açık ekran değişti (ya da uygulama arkaya/öne geçti).
     *
     * @param screen Yeni ekranın adı; uygulama arkaya geçtiyse null.
     */
    fun setActiveScreen(screen: String?) {
        closeSegment()
        if (isStudyScreen(screen)) {
            val now = clock.elapsedMs()
            // Monoton saat 0'dan başlayabilir; 0 "kapalı" anlamına geldiği için en az 1.
            segmentStartMs = if (now > 0L) now else 1L
            lastInteractionMs = segmentStartMs
        }
    }

    /** Kullanıcı ekrana dokundu (Activity.onUserInteraction). */
    fun onUserInteraction() {
        if (segmentStartMs <= 0L) return
        lastInteractionMs = clock.elapsedMs()
    }

    private fun creditableMs(now: Long): Long {
        val start = segmentStartMs
        if (start <= 0L) return 0L
        val idleDeadline = lastInteractionMs + MAX_IDLE_MS
        val end = if (now < idleDeadline) now else idleDeadline
        return (end - start).coerceAtLeast(0L)
    }

    private fun closeSegment() {
        if (segmentStartMs <= 0L) return
        val elapsed = creditableMs(clock.elapsedMs())
        segmentStartMs = 0L
        val seconds = (elapsed / 1000L).toInt()
        if (seconds <= 0) return
        addSeconds(seconds)
    }

    /** Bugünkü toplam çalışma süresi (saniye); açık parça da dahil. */
    fun secondsToday(): Int {
        val stored = store.getInt(KEY_PREFIX + StreakDays.dayId(clock, 0), 0)
        if (segmentStartMs <= 0L) return stored
        val open = (creditableMs(clock.elapsedMs()) / 1000L).toInt()
        return stored + open
    }

    /** Cihazdaki tüm çalışma süresi kayıtlarını siler (hesap değişimi). */
    fun clearAll() {
        store.keys().filter { it.startsWith(KEY_PREFIX) }.forEach { store.remove(it) }
        segmentStartMs = 0L
        lastInteractionMs = 0L
    }

    /** Bugüne süre ekler (Android: addSecondsForDebug; yalnızca hata ayıklama ve sahte akış). */
    fun addSecondsForDebug(seconds: Int) {
        if (seconds <= 0) return
        addSeconds(seconds)
    }

    private fun addSeconds(seconds: Int) {
        val key = KEY_PREFIX + StreakDays.dayId(clock, 0)
        store.putInt(key, store.getInt(key, 0) + seconds)
        pruneOldDays()
        onChanged?.invoke()
    }

    private fun pruneOldDays() {
        val alive = (0 until KEEP_DAYS).map { KEY_PREFIX + StreakDays.dayId(clock, -it) }.toSet()
        store.keys().filter { it.startsWith(KEY_PREFIX) && it !in alive }.forEach { store.remove(it) }
    }
}

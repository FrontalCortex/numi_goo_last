package com.example.app

import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * "Bugün ne kadar çalıştı" ölçümü — günlük serinin (bkz. [StreakRepository]) dayandığı sayaç.
 *
 * ## Neden ekran bazlı
 * Hedef dakika cinsinden konuyor, yani süreyi gerçekten saymak gerekiyor. Ama "uygulama açık
 * kaldığı süre" yanlış cevap: çocuk menüde dolaşırken ya da telefonu bırakıp gittiğinde de
 * sayardı. Burada yalnızca DERS ekranlarında ve yalnızca uygulama öndeyken geçen süre
 * toplanıyor.
 *
 * ## Neden yeni bir yaşam döngüsü dinleyicisi yok
 * [NumiGooApplication] hangi ekranın açık olduğunu ve öne/arkaya geçişleri zaten takip ediyor
 * (ekran ölçümü için). Aynı geçişler buraya da bildiriliyor; ikinci bir dinleyici kurmak aynı
 * hatayı iki yerde yapma riski demekti.
 *
 * ## Parça sınırı
 * Ekran açık kalıp kullanıcı uzaklaşırsa süre şişerdi. Tek bir kesintisiz parça
 * [MAX_SEGMENT_MS] ile sınırlı: gerçek bir soru bu kadar sürmüyor, ama unutulan bir ekran
 * saatlerce sayardı.
 */
object StudyTimeTracker {

    private const val TAG = "StudyTimeTracker"
    private const val PREFS = "study_time"
    private const val KEY_PREFIX = "seconds_"

    /** Kaç günlük kayıt tutulsun. Seri yalnızca dün ve bugüne bakıyor; gerisi arşiv değil çöp. */
    private const val KEEP_DAYS = 8

    /** Tek bir kesintisiz çalışma parçasının üst sınırı. */
    private const val MAX_SEGMENT_MS = 10L * 60 * 1000

    /**
     * Çalışma sayılan ekranlar — kullanıcının soru çözdüğü ya da abaküs kullandığı yerler.
     *
     * Dışarıda kalanlar bilerek dışarıda:
     * - Anket ekranları (`TutorialQuestionPanelFragment`, `QuestionPanelFragment`) ders
     *   aralarında açılıyor ama orada öğrenme olmuyor; okuyup "atla"ya basmak dakika
     *   kazandırmamalı.
     * - Sonuç, sandık, ödül ve kutlama ekranları da aynı sebeple dışarıda: süre geçiyor ama
     *   çalışılmıyor.
     * - Menü, mağaza, profil, liderlik zaten çalışma değil.
     */
    private val STUDY_SCREENS = setOf(
        "AbacusFragment",
        "AbacusPracticeFragment",
        "BlindingLessonFragment",
        "TutorialFragment",
    )

    /** Açık parçanın başlangıcı (monoton saat). 0 = sayım kapalı. */
    @Volatile private var segmentStartMs = 0L

    /** Süre değiştiğinde haberdar olmak isteyenler (üst bardaki alev). */
    private var listener: (() -> Unit)? = null

    fun setOnChangedListener(l: (() -> Unit)?) {
        listener = l
    }

    fun isStudyScreen(screen: String?): Boolean = screen != null && screen in STUDY_SCREENS

    /**
     * Açık ekran değişti (ya da uygulama arkaya/öne geçti).
     *
     * @param screen Yeni ekranın adı; uygulama arkaya geçtiyse null.
     */
    fun setActiveScreen(context: Context, screen: String?) {
        closeSegment(context)
        if (isStudyScreen(screen)) {
            segmentStartMs = SystemClock.elapsedRealtime()
        }
    }

    /** Açık parçayı kapatıp süresini bugüne ekler. */
    private fun closeSegment(context: Context) {
        val start = segmentStartMs
        segmentStartMs = 0L
        if (start <= 0L) return
        val elapsed = (SystemClock.elapsedRealtime() - start).coerceIn(0L, MAX_SEGMENT_MS)
        val seconds = (elapsed / 1000L).toInt()
        if (seconds <= 0) return
        addSeconds(context, seconds)
    }

    /**
     * Bugünkü toplam çalışma süresi (saniye). Açık parça da dahil edilir ki üst bardaki alev
     * ders biter bitmez değil, ders sürerken de doğru olsun.
     */
    fun secondsToday(context: Context): Int {
        val stored = prefs(context)?.getInt(KEY_PREFIX + dayId(), 0) ?: 0
        val start = segmentStartMs
        if (start <= 0L) return stored
        val open = ((SystemClock.elapsedRealtime() - start).coerceIn(0L, MAX_SEGMENT_MS) / 1000L).toInt()
        return stored + open
    }

    /**
     * Yalnızca debug derlemesi: bugüne süre ekler.
     *
     * Seriyi elle test etmek aksi halde her tur için gerçek dakikalar bekletiyor; gün
     * değişimini denerken bu iş çekilmez hale geliyor. Release'de gövde hiçbir şey yapmaz —
     * çağıran taraf zaten `BuildConfig.DEBUG` bloğunun içinde olduğu için o kod release
     * APK'sine hiç girmiyor, buradaki kontrol ikinci bir emniyet.
     */
    fun addSecondsForDebug(context: Context, seconds: Int) {
        if (!BuildConfig.DEBUG) return
        if (seconds <= 0) return
        addSeconds(context, seconds)
    }

    private fun addSeconds(context: Context, seconds: Int) {
        val p = prefs(context) ?: return
        try {
            val key = KEY_PREFIX + dayId()
            p.edit().putInt(key, p.getInt(key, 0) + seconds).apply()
            pruneOldDays(context)
            listener?.invoke()
        } catch (e: Throwable) {
            Log.w(TAG, "Çalışma süresi yazılamadı", e)
        }
    }

    private fun pruneOldDays(context: Context) {
        val p = prefs(context) ?: return
        val alive = (0 until KEEP_DAYS).map { KEY_PREFIX + dayId(-it) }.toSet()
        val stale = p.all.keys.filter { it.startsWith(KEY_PREFIX) && it !in alive }
        if (stale.isEmpty()) return
        val editor = p.edit()
        stale.forEach { editor.remove(it) }
        editor.apply()
    }

    private fun prefs(context: Context) = try {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    } catch (e: Throwable) {
        Log.w(TAG, "SharedPreferences açılamadı", e)
        null
    }

    /**
     * Yerel takvim günü, `yyyy-MM-dd`.
     *
     * Uygulamadaki "periyot" (bkz. DailyQuestionPeriod) epoch'tan itibaren 24 saatlik dilim,
     * yani Türkiye'de sabah 03:00'te değişiyor. Seri için bu yanlış olurdu: kullanıcının
     * "dün/bugün"ü takvim günü. cupHistory de aynı biçimi kullanıyor.
     */
    fun dayId(offsetDays: Int = 0): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(cal.timeInMillis))
    }
}

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
 * ## Boşta kalma sınırı
 * Ekran açık kalıp kullanıcı uzaklaşırsa süre şişerdi. Çözüm ekranın ne kadar açık kaldığına
 * değil, ETKİLEŞİME bakmak: son dokunuştan sonra en fazla [MAX_IDLE_MS] kadarı yazılır,
 * gerisi yazılmaz.
 *
 * Eskiden tek bir kesintisiz parça on dakikayla sınırlıydı. Bu, unutulan ekranı doğru
 * kesiyordu ama gerçekten çalışanı da kesiyordu: aynı ekranda yirmi dakika çalışan birine
 * on dakika yazılıyordu, yani yirmi dakikalık hedef tek oturumda ulaşılamaz hale geliyordu.
 * Boşta kalma sınırı ikisini ayırıyor — çalışan ne kadar çalışırsa o kadar yazılır,
 * unutulan ekran [MAX_IDLE_MS] sonra durur.
 */
object StudyTimeTracker {

    private const val TAG = "StudyTimeTracker"
    private const val PREFS = "study_time"
    private const val KEY_PREFIX = "seconds_"

    /** Kaç günlük kayıt tutulsun. Seri yalnızca dün ve bugüne bakıyor; gerisi arşiv değil çöp. */
    private const val KEEP_DAYS = 8

    /**
     * Son dokunuştan sonra süre saymaya devam edilen en uzun aralık.
     *
     * Beş dakika: zor bir soruda düşünmek ya da sesli/körleme dersinde sayıları izlemek bu
     * sürenin altında kalıyor, ama telefonu bırakıp gitmek kalmıyor.
     */
    private const val MAX_IDLE_MS = 5L * 60 * 1000

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

    /** Son kullanıcı etkileşimi (monoton saat). Parça açılırken de tazeleniyor. */
    @Volatile private var lastInteractionMs = 0L

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
            val now = SystemClock.elapsedRealtime()
            segmentStartMs = now
            // Ekrana yeni girildi: ilk dokunuşu beklemeden sayım başlasın. Soru okunurken
            // geçen ilk saniyeler de çalışmadır.
            lastInteractionMs = now
        }
    }

    /**
     * Kullanıcı ekrana dokundu.
     *
     * Activity'den geliyor ([android.app.Activity.onUserInteraction]): her dokunuş ve tuş
     * olayı oradan geçiyor, yani ders ekranlarının hiçbirine tek satır eklemeye gerek yok.
     * Ders içi tek tek kancalar kurulsaydı dört ekranda dört ayrı doğruluk sorusu olurdu.
     *
     * Çok sık çağrılıyor; gövdesi bu yüzden tek atamadan ibaret, disk yazımı yok.
     */
    fun onUserInteraction() {
        if (segmentStartMs <= 0L) return
        lastInteractionMs = SystemClock.elapsedRealtime()
    }

    /**
     * Açık parçanın şu ana kadar yazılabilir süresi (ms).
     *
     * Boştaki kuyruk atılıyor: son dokunuştan [MAX_IDLE_MS] sonrasına kadar sayılır.
     */
    private fun creditableMs(now: Long): Long {
        val start = segmentStartMs
        if (start <= 0L) return 0L
        val idleDeadline = lastInteractionMs + MAX_IDLE_MS
        val end = if (now < idleDeadline) now else idleDeadline
        return (end - start).coerceAtLeast(0L)
    }

    /** Açık parçayı kapatıp süresini bugüne ekler. */
    private fun closeSegment(context: Context) {
        if (segmentStartMs <= 0L) return
        val elapsed = creditableMs(SystemClock.elapsedRealtime())
        segmentStartMs = 0L
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
        if (segmentStartMs <= 0L) return stored
        val open = (creditableMs(SystemClock.elapsedRealtime()) / 1000L).toInt()
        return stored + open
    }

    /**
     * Cihazdaki tüm çalışma süresi kayıtlarını siler.
     *
     * Hesap değişiminde çağrılıyor (bkz. [StreakRepository.bindToUser]): süre kovaları uid'ye
     * değil yalnızca güne göre anahtarlanıyor, yani temizlenmeseydi yeni kullanıcı öncekinin
     * dakikalarını devralır ve hiç çalışmadan serisini ilerletirdi.
     */
    fun clearAll(context: Context) {
        prefs(context)?.edit()?.clear()?.apply()
        segmentStartMs = 0L
        lastInteractionMs = 0L
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

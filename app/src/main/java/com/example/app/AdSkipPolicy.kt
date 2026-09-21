package com.example.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [AdSkipFragment]'in ne sıklıkta çıkacağına karar verir.
 *
 * Panel bir Pro satış ekranı: reklamdan çıkan kullanıcıya "bunları görmek istemiyorsan" diyor.
 * Değeri seyrekliğinde — her reklamdan sonra çıktığında kullanıcı onu reklamın bir parçası
 * sayıp içeriğine hiç bakmadan kapatmaya alışıyor.
 *
 * ## Sayaç neden diskte
 * Eskiden karar bellekteki bir sayaca bakıyordu (`GlobalValues.interstitialAdShownCount`) ve
 * koşul `sayaç % 3 == 1` idi. Sayaç her uygulama açılışında sıfırlandığı için niyet "üç
 * reklamda bir" olsa da sonuç **her oturumun ilk reklamından sonra panel** oluyordu:
 * uygulamayı kapatıp açan kullanıcı paneli hemen yeniden görüyordu. Sayaç artık diskte, yani
 * uygulama kapansa da yerini koruyor.
 *
 * ## Sayaç "kaçıncı reklam" değil, "panelden beri kaç reklam"
 * Bazı reklamlar panele uygun değil (ör. kupa yolu açılış animasyonuna geçilecekse). Sayaç
 * mutlak reklam numarası olsaydı, sıra uygun olmayan bir reklama denk geldiğinde o tur
 * atlanır ve panel beklenenin yarısı kadar çıkardı. Bunun yerine sayaç panelden beri geçen
 * reklamları sayıyor: sıra geldiğinde reklam uygun değilse sayaç birikmeye devam ediyor ve
 * panel bir sonraki uygun reklamda çıkıyor.
 *
 * ## Üç kural birden
 * Reklam sayısı tek başına yetmiyor: interstitial'ın kendi bekleme süresi beş dakika, yani
 * uzun bir seansta dört reklam yirmi dakikada dolabiliyor ve panel gün boyu tekrar tekrar
 * çıkabiliyor. Bu yüzden üç kapı var ve üçü de açık olmalı:
 * 1. Panelden beri en az [ADS_PER_PANEL] reklam geçmiş olmalı (seans içi seyreklik).
 * 2. Son panelin üstünden en az [MIN_GAP_MS] geçmiş olmalı (üst üste gelmesin).
 * 3. O gün en fazla [MAX_PANELS_PER_DAY] panel gösterilmiş olmalı (gün içi tavan).
 *
 * Gün ve süre cihaz saatine bakıyor; kullanıcı saati oynatarak daha ÇOK panel görebilir ama
 * bu bir ödül değil satış ekranı, yani kötüye kullanımı anlamsız. Ters yön ise önemli: saat
 * geri alındığında panel sonsuza kadar kilitlenmesin diye negatif süre "süre doldu" sayılıyor.
 */
object AdSkipPolicy {

    private const val TAG = "AdSkipPolicy"
    private const val PREFS_NAME = "ad_skip_policy"
    private const val KEY_ADS_SINCE_PANEL = "ads_since_panel"
    private const val KEY_LAST_PANEL_MS = "last_panel_ms"
    private const val KEY_DAY_ID = "panel_day_id"
    private const val KEY_DAY_COUNT = "panel_day_count"

    /**
     * Kaç reklamda bir panel çıksın.
     *
     * İlk üç reklamda panel yok; yeni kullanıcı uygulamayı tanımadan satış ekranıyla
     * karşılaşmıyor.
     */
    private const val ADS_PER_PANEL = 4

    /** İki panel arasındaki en kısa süre. */
    private const val MIN_GAP_MS = 2L * 60 * 60 * 1000

    /** Bir günde en fazla kaç panel. */
    private const val MAX_PANELS_PER_DAY = 2

    private fun prefs(context: Context) = try {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    } catch (e: Throwable) {
        Log.w(TAG, "SharedPreferences açılamadı; panel gösterilmeyecek.", e)
        null
    }

    /**
     * Bir reklam kapandı. Panelin şimdi çıkması gerekiyorsa true döner ve sayacı sıfırlar.
     *
     * @param eligible Bu reklamdan sonra panel çıkabilir mi (çağıran akışın kararı).
     *   false ise panel çıkmaz ama reklam yine sayılır.
     *
     * Ayarlar okunamazsa false dönüyor: sayacı tutamadığımız bir durumda paneli her
     * reklamda göstermektense hiç göstermemek daha doğru.
     */
    fun onAdClosed(context: Context, eligible: Boolean): Boolean {
        val p = prefs(context) ?: return false
        return try {
            val since = p.getInt(KEY_ADS_SINCE_PANEL, 0) + 1
            // Sıra gelmediyse ya da süre/gün kapısı kapalıysa sayaç EŞİKTE bekletiliyor,
            // sıfırlanmıyor: kapı açıldığında panel bir sonraki uygun reklamda çıksın,
            // baştan dört reklam daha beklenmesin.
            if (!eligible || since < ADS_PER_PANEL || !withinBudget(p)) {
                p.edit().putInt(KEY_ADS_SINCE_PANEL, since).apply()
                false
            } else {
                markShown(p)
                true
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Sayaç güncellenemedi; panel gösterilmiyor.", e)
            false
        }
    }

    /** Süre ve gün kapıları açık mı. */
    private fun withinBudget(p: SharedPreferences): Boolean {
        if (panelsToday(p) >= MAX_PANELS_PER_DAY) return false
        val last = p.getLong(KEY_LAST_PANEL_MS, 0L)
        if (last <= 0L) return true
        val elapsed = System.currentTimeMillis() - last
        // Negatif: cihaz saati geri alınmış. Kilitli kalmasın diye süre dolmuş sayılıyor.
        return elapsed < 0L || elapsed >= MIN_GAP_MS
    }

    /** Bugün kaç panel gösterildi. Kayıtlı gün bugün değilse sayaç sıfırdan başlar. */
    private fun panelsToday(p: SharedPreferences): Int =
        if (p.getString(KEY_DAY_ID, null) == todayId()) p.getInt(KEY_DAY_COUNT, 0) else 0

    private fun markShown(p: SharedPreferences) {
        p.edit()
            .putInt(KEY_ADS_SINCE_PANEL, 0)
            .putLong(KEY_LAST_PANEL_MS, System.currentTimeMillis())
            .putString(KEY_DAY_ID, todayId())
            .putInt(KEY_DAY_COUNT, panelsToday(p) + 1)
            .apply()
    }

    private fun todayId(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** Yalnızca hata ayıklama/test için: sayacı sıfırlar. */
    fun resetForDebug(context: Context) {
        prefs(context)?.edit()?.clear()?.apply()
    }
}

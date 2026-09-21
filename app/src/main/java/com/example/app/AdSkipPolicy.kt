package com.example.app

import android.content.Context
import android.util.Log

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
 */
object AdSkipPolicy {

    private const val TAG = "AdSkipPolicy"
    private const val PREFS_NAME = "ad_skip_policy"
    private const val KEY_ADS_SINCE_PANEL = "ads_since_panel"

    /**
     * Kaç reklamda bir panel çıksın.
     *
     * İlk üç reklamda panel yok; yeni kullanıcı uygulamayı tanımadan satış ekranıyla
     * karşılaşmıyor.
     */
    private const val ADS_PER_PANEL = 4

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
            if (!eligible || since < ADS_PER_PANEL) {
                p.edit().putInt(KEY_ADS_SINCE_PANEL, since).apply()
                false
            } else {
                p.edit().putInt(KEY_ADS_SINCE_PANEL, 0).apply()
                true
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Sayaç güncellenemedi; panel gösterilmiyor.", e)
            false
        }
    }

    /** Yalnızca hata ayıklama/test için: sayacı sıfırlar. */
    fun resetForDebug(context: Context) {
        prefs(context)?.edit()?.clear()?.apply()
    }
}

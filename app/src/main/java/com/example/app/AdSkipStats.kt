package com.example.app

import android.content.Context
import android.util.Log

/**
 * [AdSkipFragment]'in kaç kez gösterildiğinin kalıcı sayacı ve ölçüm kovaları.
 *
 * ## Neden kalıcı olmak zorunda
 * Ölçmek istediğimiz şey "kullanıcı bu paneli kaçıncı kez görüyor" — çünkü aradığımız sinyal
 * tekrar gördükçe oluşan panel körlüğü. Sayaç bellekte tutulsaydı her uygulama açılışında
 * sıfırlanır ve herkes sonsuza kadar "1. görüş" olarak görünürdü.
 *
 * Aynı tuzak panelin çıkma sıklığında da vardı: karar bellekteki bir sayaca bakıyordu ve
 * niyet "her üç reklamda bir" olmasına rağmen panel her oturumun ilk reklamından sonra
 * çıkıyordu. O sayaç da diske taşındı, bkz. [AdSkipPolicy].
 *
 * Buradaki sayaç panelin KAÇ KEZ GÖSTERİLDİĞİNİ sayıyor, [AdSkipPolicy]'deki ise panelden
 * beri kaç reklam geçtiğini. İkisi ayrı: biri ölçüm, diğeri karar.
 */
object AdSkipStats {

    private const val TAG = "AdSkipStats"
    private const val PREFS_NAME = "ad_skip_stats"
    private const val KEY_VIEW_COUNT = "view_count"

    /** Bu kovadan sonrasını ayırmanın bilgi değeri yok; 10. görüşte kitle zaten belli olur. */
    private const val VIEW_NO_CAP = 10

    private fun prefs(context: Context) = try {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    } catch (e: Throwable) {
        Log.w(TAG, "SharedPreferences açılamadı; panel ölçümü bu oturumda atlanacak.", e)
        null
    }

    /** Sayacı bir artırır ve yeni değeri döndürür. İlk gösterimde 1 döner. */
    fun nextViewNo(context: Context): Int {
        val p = prefs(context) ?: return 0
        return try {
            val next = p.getInt(KEY_VIEW_COUNT, 0) + 1
            p.edit().putInt(KEY_VIEW_COUNT, next).apply()
            next
        } catch (e: Throwable) {
            Log.w(TAG, "nextViewNo başarısız", e)
            0
        }
    }

    /**
     * Görüş numarasının GA4 boyutu olarak gönderilecek hâli: `"01"` … `"09"`, `"10+"`.
     *
     * Başa sıfır konuyor çünkü GA4 metin boyutlarını alfabetik sıralıyor; `"1"`, `"10+"`,
     * `"2"` diye dizilen bir tablo okunmuyor. `"01"` … `"10+"` doğru sırada geliyor.
     */
    fun viewBucket(viewNo: Int): String = when {
        viewNo <= 0 -> "00"
        viewNo >= VIEW_NO_CAP -> "10+"
        else -> viewNo.toString().padStart(2, '0')
    }

    /**
     * Ekranda kalma süresinin kovası.
     *
     * Panel ilk 500 ms boyunca konfeti sesi, müzik ve alkış çalıyor; yani `"0-2"` kovası
     * "içeriğe hiç bakmadı, refleksle kapattı" demek. Görüş numarası arttıkça kitlenin bu
     * kovaya kayması, aradığımız panel körlüğünün ta kendisi.
     */
    fun dwellBucket(dwellMs: Long): String = when {
        dwellMs < 2_000L -> "0-2"
        dwellMs < 5_000L -> "2-5"
        dwellMs < 10_000L -> "5-10"
        else -> "10+"
    }

    /** Yalnızca hata ayıklama/test için: görüş sayacını sıfırlar. */
    fun resetForDebug(context: Context) {
        prefs(context)?.edit()?.clear()?.apply()
    }
}

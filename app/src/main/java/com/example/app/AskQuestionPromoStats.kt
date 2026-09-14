package com.example.app

import android.content.Context
import android.util.Log

/**
 * [AskQuestionOpenFragment]'in kaç kez gösterildiğinin kalıcı sayacı.
 *
 * ## Neden sayaç tetikleyici başına ayrı
 * Ekran iki bambaşka durumda çıkıyor: kendiliğinden açılan tanıtım ve kullanıcının "öğretmene
 * sor"a basıp kredisiz kalması. Bunlar tek sayaçta toplanırsa "kullanıcı bu ekranı 6. kez
 * görüyor" cümlesi anlamsızlaşır — 6'nın kaçı kesinti, kaçı kendi isteği belli olmaz.
 *
 * ## Neden kalıcı
 * Aradığımız sinyal tekrar gördükçe oluşan körlük. Bellekte tutulan bir sayaç her uygulama
 * açılışında sıfırlanır ve herkes sonsuza kadar "1. görüş" görünürdü.
 *
 * ## Neden özellikle bu ekranda önemli
 * Otomatik tanıtımın ömür boyu üst sınırı yok: [MainActivity.maybeShowAskQuestionPromo] her
 * gösterimden sonra sayacı sıfırlıyor, eşik 3. Yani denemeyi hiç başlatmayan Free bir kullanıcı
 * her 3 ders dönüşünde ekranı tekrar görüyor. "Kaçıncı görüşte artık kimse dönüşmüyor"
 * sorusunun cevabı, bu tekrarın faydalı mı yoksa dırdır mı olduğunu söyleyecek tek veri.
 */
object AskQuestionPromoStats {

    private const val TAG = "AskQuestionPromoStats"
    private const val PREFS_NAME = "ask_question_promo_stats"
    private const val KEY_PREFIX_VIEW_COUNT = "view_count_"

    /** Bu kovadan sonrasını ayırmanın bilgi değeri yok. */
    private const val VIEW_NO_CAP = 10

    private fun prefs(context: Context) = try {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    } catch (e: Throwable) {
        Log.w(TAG, "SharedPreferences açılamadı; tanıtım ölçümü bu oturumda atlanacak.", e)
        null
    }

    /**
     * İlgili tetikleyicinin sayacını bir artırır ve yeni değeri döndürür.
     *
     * @param trigger [AnalyticsLogger.PROMO_TRIGGER_AUTO] veya
     *   [AnalyticsLogger.PROMO_TRIGGER_OUT_OF_CREDITS].
     */
    fun nextViewNo(context: Context, trigger: String): Int {
        val p = prefs(context) ?: return 0
        return try {
            val key = KEY_PREFIX_VIEW_COUNT + trigger
            val next = p.getInt(key, 0) + 1
            p.edit().putInt(key, next).apply()
            next
        } catch (e: Throwable) {
            Log.w(TAG, "nextViewNo başarısız", e)
            0
        }
    }

    /**
     * Görüş numarasının GA4 boyutu olarak gönderilecek hâli: `"01"` … `"09"`, `"10+"`.
     *
     * Başa sıfır konuyor çünkü GA4 metin boyutlarını alfabetik sıralıyor; `"1"`, `"10+"`, `"2"`
     * diye dizilen bir tablo okunmuyor. [AdSkipStats.viewBucket] ile birebir aynı biçim: iki
     * ekran da `view_no` boyutunu paylaşıyor, farklı biçimler tabloyu bozardı.
     */
    fun viewBucket(viewNo: Int): String = when {
        viewNo <= 0 -> "00"
        viewNo >= VIEW_NO_CAP -> "10+"
        else -> viewNo.toString().padStart(2, '0')
    }

    /** Yalnızca hata ayıklama/test için: sayaçları sıfırlar. */
    fun resetForDebug(context: Context) {
        prefs(context)?.edit()?.clear()?.apply()
    }
}

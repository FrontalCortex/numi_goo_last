package com.example.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.Locale

/**
 * Öğretici (tutorial) soru adımlarında "kaçıncı denemede geçti" ölçümünün defter tutması.
 *
 * ## Neden cihazda kalıcı depolama gerekiyor
 * İstenen veri "kullanıcı bu adımı **ilk kez** gördüğünde kaç denemede geçti" — yani adım
 * başına kullanıcı başına TEK bir kayıt. Bunu bozan iki durum var ve ikisi de fragment
 * içinde tutulan bir sayaçla çözülemez:
 *
 * 1. **Geri tuşu.** Kullanıcı adımı doğru geçtikten sonra geri dönüp aynı soruyu yeniden
 *    (bilerek yanlış da olabilir) cevaplayabiliyor. Bu ikinci cevap ölçüme girmemeli.
 * 2. **Uygulamayı kapatıp dönme / dersi tekrar oynama.** Öğretici baştan başlar; fragment
 *    içindeki sayaç sıfırlanır ve 3 kez yanılmış bir çocuk "ilk denemede bildi" görünür.
 *
 * Bu yüzden hem deneme sayacı hem de "bu adım kapandı" işareti [SharedPreferences] içinde
 * tutulur. Toplam 96 soru adımı var; kayıt birkaç kilobayt.
 *
 * ## Gizlilik
 * Burada tutulan tek şey adım anahtarı ve bir tam sayı. Kişisel veri yok, sunucuya
 * gönderilmez; yalnızca "bu adımı daha önce ölçtük mü" sorusunu yanıtlar.
 */
object TutorialStepAnalytics {

    private const val TAG = "TutorialStepAnalytics"
    private const val PREFS_NAME = "tutorial_step_analytics"

    /** `attempt` sayacı: bu adımda şimdiye kadar kaç cevap gönderildi. */
    private const val PREFIX_ATTEMPTS = "a:"

    /** "Kapandı" işareti: adım ilk kez doğru geçildi, artık hiçbir cevap ölçülmez. */
    private const val PREFIX_SETTLED = "d:"

    /** "İlk kez görüldü" işareti — [markReachedOnce] için. */
    private const val PREFIX_REACHED = "r:"

    /** Adım anahtarındaki okunabilir kısmın üst sınırı. Anahtarın tamamı 100 karakteri aşmamalı. */
    private const val SLUG_MAX = 40

    /** Türkçe harfleri ASCII karşılığına indirger; slug'da yalnızca a-z0-9_ kalsın diye. */
    private val TURKISH_FOLD = mapOf(
        'ı' to 'i', 'İ' to 'I', 'ş' to 's', 'Ş' to 'S', 'ğ' to 'g', 'Ğ' to 'G',
        'ü' to 'u', 'Ü' to 'U', 'ö' to 'o', 'Ö' to 'O', 'ç' to 'c', 'Ç' to 'C',
        'â' to 'a', 'Â' to 'A', 'î' to 'i', 'Î' to 'I', 'û' to 'u', 'Û' to 'U',
    )

    private fun prefs(context: Context): SharedPreferences? = try {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    } catch (e: Throwable) {
        Log.w(TAG, "SharedPreferences açılamadı; adım ölçümü bu oturumda atlanacak.", e)
        null
    }

    /**
     * Bir soru adımının kalıcı kimliği.
     *
     * ## Neden başlık + sıra, neden adımın index'i değil
     * Adımın listedeki index'i, araya yeni bir adım eklendiğinde kayar. O zaman eski
     * kullanıcıların verisi sessizce başka bir adımın satırına karışır — GA4'te bunu fark
     * etmenin bir yolu yoktur. Başlık ise adımın kendisiyle birlikte taşınır: araya adım
     * eklemek mevcut satırları bozmaz, başlığı değiştirmek ise zaten yeni bir soru demektir
     * ve yeni satır olması doğrudur.
     *
     * [occurrence], aynı öğretici içinde **aynı başlığın** kaçıncı kullanımı olduğudur.
     * Buna ihtiyaç var: örneğin tutorial 20'de "Hangi basamağa ekleyeceksin?" başlığı aynı
     * soruyla 4 kez geçiyor (birler, onlar, yüzler...) ve bunlar farklı adımlar.
     */
    fun stepKey(tutorialNumber: Int, occurrence: Int, title: String): String {
        val slug = slug(title).ifEmpty { "adim" }
        return "t${tutorialNumber}_${occurrence}_$slug"
    }

    private fun slug(title: String): String {
        val folded = StringBuilder(title.length)
        for (ch in title) folded.append(TURKISH_FOLD[ch] ?: ch)
        val lowered = folded.toString().lowercase(Locale.ROOT)
        val out = StringBuilder(lowered.length)
        for (ch in lowered) {
            when {
                ch in 'a'..'z' || ch in '0'..'9' -> out.append(ch)
                out.isNotEmpty() && out.last() != '_' -> out.append('_')
            }
        }
        return out.toString().trim('_').take(SLUG_MAX).trim('_')
    }

    /**
     * Adım bu cihazda ilk kez görülüyorsa `true` döner ve işaretler; sonraki her çağrıda
     * `false`. Çağıran yalnızca `true` dönünce olay gönderir, böylece "bu adıma kaç kullanıcı
     * ulaştı" sayısı geri tuşu ve tekrar oynamalarla şişmez.
     */
    fun markReachedOnce(context: Context, stepKey: String): Boolean {
        val p = prefs(context) ?: return false
        return try {
            if (p.getBoolean(PREFIX_REACHED + stepKey, false)) return false
            p.edit().putBoolean(PREFIX_REACHED + stepKey, true).apply()
            true
        } catch (e: Throwable) {
            Log.w(TAG, "markReachedOnce başarısız", e)
            false
        }
    }

    /**
     * Bir cevap denemesini deftere işler.
     *
     * @return Bu cevabın kaçıncı deneme olduğu (1'den başlar), ya da adım **zaten ilk kez
     *   doğru geçilmişse** `null`. `null` dönmesi "bu cevabı ölçme" demektir: kullanıcı geri
     *   dönüp yeniden cevaplıyor ya da dersi tekrar oynuyor.
     */
    fun recordAttempt(context: Context, stepKey: String, isCorrect: Boolean): Int? {
        val p = prefs(context) ?: return null
        return try {
            if (p.getBoolean(PREFIX_SETTLED + stepKey, false)) return null
            val attemptNo = p.getInt(PREFIX_ATTEMPTS + stepKey, 0) + 1
            val editor = p.edit().putInt(PREFIX_ATTEMPTS + stepKey, attemptNo)
            if (isCorrect) editor.putBoolean(PREFIX_SETTLED + stepKey, true)
            editor.apply()
            attemptNo
        } catch (e: Throwable) {
            Log.w(TAG, "recordAttempt başarısız", e)
            null
        }
    }

    /**
     * Deneme sayısını GA4'te satır/sütun olarak kullanılabilir kovalara böler.
     *
     * GA4 Keşfet arayüzünde sayısal bir boyutu aralıklara ayırmak mümkün değil; kovayı
     * burada metin olarak üretmek, raporu tek tıkla kurulabilir hale getiriyor.
     */
    fun bucketOf(attemptNo: Int): String = when {
        attemptNo <= 1 -> "1"
        attemptNo >= 5 -> "5+"
        else -> attemptNo.toString()
    }

    /** Yalnızca hata ayıklama/test için: bu cihazdaki tüm adım defterini siler. */
    fun resetForDebug(context: Context) {
        prefs(context)?.edit()?.clear()?.apply()
    }
}

package com.example.app

import android.view.View

/**
 * Sağdan sola diller (Arapça) için yardımcılar.
 *
 * ## Ne döner, ne dönmez
 * Menüler, kartlar, oklar ve geri düğmesi Android'in kendi sağdan sola düzeniyle dönüyor
 * (`supportsRtl="true"`, layout'larda start/end). Abaküs, sayı göstergeleri, flaş sayıları, denklem
 * satırları ve sayı giriş alanları DÖNMÜYOR: abaküste birler en sağda, büyük basamak solda; Arap
 * ülkelerindeki kurslar da abaküsü ve denklemi böyle yazıyor. Bunlar layout'ta
 * `android:layoutDirection="ltr"` ve left/right kısıtlarla sabit; kodda kurulanlar [keepLtr] ile.
 *
 * ## Metin içindeki denklem
 * Sağdan sola paragrafta "3 + 4 = 7" yön algoritması yüzünden "7 = 4 + 3" diye dizilebilir, "-34" de
 * "34-" olur. [ltr] metni U+2066 (LRI) … U+2069 (PDI) arasına alıp soldan sağa yalıtıyor; Türkçe,
 * İngilizce ve Endonezcede metne dokunmuyor (görünüm aynı kalsın, karşılaştırmalar bozulmasın).
 */
object RtlSupport {

    // Görünmez karakterler kaynakta okunmasın diye kodla: U+2066 LEFT-TO-RIGHT ISOLATE, U+2069 POP DIRECTIONAL ISOLATE.
    private val LRI = Char(0x2066).toString()
    private val PDI = Char(0x2069).toString()

    /** Denklemi / işaretli sayıyı soldan sağa yalıtır; yalnızca Arapçada (bkz. sınıf açıklaması). */
    fun ltr(text: CharSequence): String =
        if (AppLanguage.isArabic()) LRI + text + PDI else text.toString()

    /**
     * Sayı + işlem dizilerini ("3 + 4 = 7", "12 − 5 = ?", "-34") metnin içinde bulup tek tek yalıtır:
     * Sobi'nin cevabı, analiz ekranı, öğretici metni gibi denklemin cümleye gömülü geldiği yerler
     * (sunucudan ya da çevirmenin cümlesinden gelen denklemin yerini kod bilmiyor). İşlemsiz düz sayıya
     * dokunmuyor. Yalnızca Arapçada.
     */
    fun isolateEquations(text: String): String {
        if (!AppLanguage.isArabic()) return text
        return EQUATION.replace(text) { m ->
            if (m.value.any { it in OPERATORS }) LRI + m.value + PDI else m.value
        }
    }

    private const val OPERATORS = "+-−×÷="

    // İsteğe bağlı işaret, sayı (ondalıklı olabilir), ardından sıfır ya da daha çok "işlem sayı|?".
    private val EQUATION =
        Regex("""(?<![\p{L}\d])(?:[-−+]\s?)?\d+(?:[.,]\d+)?(?:\s*[-−+×÷=]\s*(?:[-−]\s?)?(?:\d+(?:[.,]\d+)?|\?))*""")

    fun isRtl(view: View): Boolean = view.layoutDirection == View.LAYOUT_DIRECTION_RTL

    /**
     * Bitiş yönü işareti: soldan sağa +1, sağdan sola -1. "Başlangıç kenarından kayarak gelen" panel
     * ya da "okuma yönünde ilerleyen" geçiş animasyonlarında `translationX` bununla çarpılıyor.
     */
    fun endSign(view: View): Float = if (isRtl(view)) -1f else 1f

    /** Kodda kurulan abaküs / sayı görünümlerini aynalanmaktan korur. */
    fun keepLtr(vararg views: View?) {
        for (v in views) v?.layoutDirection = View.LAYOUT_DIRECTION_LTR
    }
}

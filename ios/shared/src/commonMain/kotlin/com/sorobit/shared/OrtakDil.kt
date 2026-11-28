// Kaynak: Android app/src/main/java/com/example/app/AppLanguage.kt — textLanguage() / Locale.getDefault().
// Android'de metni kodda kuran ya da sayıyı yerele göre biçimleyen yerler dili Locale.getDefault()'tan
// okuyor; ortak kodda uygulama dili yok, onun yerine bu tek ayar var. iOS: Metin.diliAyarla atar.
package com.sorobit.shared

/**
 * Ortak kodun metin dili. Sobi hata analizi ([com.sorobit.shared.sobianaliz.SobiAnalizDili] bu ayarı
 * kullanır) ve sayı biçimi (ondalık ayırıcı) buna bakar. Varsayılan Türkçe.
 */
object OrtakDil {
    const val TURKISH = "tr"
    const val ENGLISH = "en"

    /** Dil kodu ("tr", "en", "id", "ar"; Android: AppLanguage.textLanguage()). */
    var language: String = TURKISH

    /**
     * Seçili dilin ondalık ayırıcısı (Android: `String.format(Locale.getDefault(), "%.1f", …)`):
     * Türkçe ve Endonezcede virgül, diğer dillerde nokta (Arapça Batı rakamlı yerelde de nokta).
     */
    fun decimalSeparator(): Char = if (language == TURKISH || language == "id") ',' else '.'
}

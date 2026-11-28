// Kaynak: Android app/src/main/java/com/example/app/AppLanguage.kt — textLanguage() karşılığı.
// Android cümleyi kodda kuran yerler (MistakeAnalyzer, SubStepExplainer) dili Locale.getDefault()'tan
// okuyor; ortak kodda uygulama dili yok, onun yerine bu tek ayar var. iOS: Metin.diliAyarla atar.
package com.sorobit.shared.sobianaliz

/**
 * Sobi hata analizinin metin dili: [MistakeAnalyzer] (teşhis, sütun ve kural adları) ile
 * [SubStepExplainer] (alt adım açıklamaları) buna bakar.
 *
 * Yalnızca İngilizce taşındı; Android'deki Endonezce/Arapça dalları (MistakeAnalyzer `ID`/`AR`,
 * SubStepExplainerId/Ar) ŞİMDİLİK yok: "en" dışındaki her değer Türkçe metin verir.
 */
object SobiAnalizDili {
    const val TURKISH = "tr"
    const val ENGLISH = "en"

    /** Dil kodu ("tr", "en" …; Android: AppLanguage.textLanguage()). */
    var language: String = TURKISH

    internal fun english(): Boolean = language == ENGLISH
}

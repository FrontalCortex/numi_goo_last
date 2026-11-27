package com.example.app

import com.example.app.abacus.MistakeAnalyzer
import com.example.app.abacus.StepExplainer
import com.example.app.abacus.SubStepExplainer
import com.example.app.homework.HomeworkOp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.NumberFormat
import java.util.Locale

/**
 * Arapça altyapısı: Batı rakamları (kullanıcı kararı, docs/CEVIRI_SOZLUGU_AR.md), denklem yalıtımı ve
 * koddaki Arapça cümleler. Varsayılan yerel Arapçaya çevrilip testten sonra geri alınıyor.
 */
class ArabicSupportTest {

    private lateinit var saved: Locale
    private val arabicIndic = Regex("[٠-٩۰-۹]")

    @Before
    fun arabic() {
        saved = Locale.getDefault()
        Locale.setDefault(AppLanguage.locale(AppLanguage.ARABIC))
    }

    @After
    fun restore() {
        Locale.setDefault(saved)
    }

    @Test
    fun arapca_yerel_bati_rakami_uretir() {
        val locale = AppLanguage.locale(AppLanguage.ARABIC)
        assertEquals("ar", locale.language)
        val formatted = String.format("%d · %.1f", 1234, 2.5)
        assertFalse(formatted, arabicIndic.containsMatchIn(formatted))
        assertEquals("1234 · 2.5", formatted)
        val number = NumberFormat.getInstance(locale).format(1234.5)
        assertFalse(number, arabicIndic.containsMatchIn(number))
        assertTrue(number, number.startsWith("1") && number.endsWith("4.5"))
    }

    @Test
    fun duz_arapca_yerel_dogu_arap_rakami_uretirdi() {
        // Karşılaştırma: "ar-u-nu-latn" olmasa ٠–٩ çıkardı (ar-EG gibi). Testin anlamlı olduğunun kanıtı.
        val plain = String.format(Locale.forLanguageTag("ar-EG"), "%d", 1234)
        assertTrue(plain, arabicIndic.containsMatchIn(plain))
    }

    @Test
    fun denklem_yalitimi() {
        val lri = Char(0x2066).toString()
        val pdi = Char(0x2069).toString()
        assertEquals("$lri" + "3 + 4 = 7" + pdi, RtlSupport.ltr("3 + 4 = 7"))
        assertEquals(
            "الجواب ${lri}23 + 33 − 12 = 44$pdi صحيح، والعدد 7 وحده.",
            RtlSupport.isolateEquations("الجواب 23 + 33 − 12 = 44 صحيح، والعدد 7 وحده."),
        )
        assertEquals("${lri}-34$pdi", RtlSupport.isolateEquations("-34"))
        assertEquals("${lri}12 − 5 = ?$pdi", RtlSupport.isolateEquations("12 − 5 = ?"))
        // Türkçede metne dokunulmuyor.
        Locale.setDefault(Locale("tr", "TR"))
        assertEquals("3 + 4 = 7", RtlSupport.ltr("3 + 4 = 7"))
        assertEquals("3 + 4 = 7", RtlSupport.isolateEquations("3 + 4 = 7"))
    }

    @Test
    fun arapca_sayi_ad_uyumu() {
        assertEquals("خطأ واحد", MistakeAnalyzer.mistakesAr(1))
        assertEquals("خطآن", MistakeAnalyzer.mistakesAr(2))
        assertEquals("3 أخطاء", MistakeAnalyzer.mistakesAr(3))
        assertEquals("10 أخطاء", MistakeAnalyzer.mistakesAr(10))
        assertEquals("11 خطأ", MistakeAnalyzer.mistakesAr(11))
        assertEquals("خرزة واحدة", MistakeAnalyzer.beadsAr(1))
        assertEquals("خرزتين", MistakeAnalyzer.beadsAr(2))
        assertEquals("4 خرزات", MistakeAnalyzer.beadsAr(4))
    }

    @Test
    fun arapca_hata_teshisi_ve_alt_adim() {
        // 3 + 4: أضِف 5 واطرح صديقه 1. Çocuk 5'i getirdi, 1'i götürmedi → 8.
        val d = MistakeAnalyzer.analyze(3, "+", 4, listOf(0, 1, 2, 3, 8), 8)!!
        assertEquals(MistakeAnalyzer.Kind.MISSING_STEP, d.kind)
        assertTrue(d.text, d.text.startsWith("في خانة الآحاد، لإضافة 4 نحتاج إلى قاعدة الأصدقاء الصغار"))
        assertTrue(d.text, d.text.contains("لقد تخطّيت خطوة «اطرح صديقه 1»"))

        val m = StepExplainer.explain(3L, HomeworkOp.ADD, 4L)!!.moves.single()
        assertTrue(SubStepExplainer.question(m.steps[0], m).startsWith("ماذا تعني «"))
        val why = SubStepExplainer.whyRule(m)
        assertTrue(why, why.startsWith("في عمود الآحاد الرقم 3؛"))
        assertTrue(why, why.endsWith("لذلك: أضِف 5 واطرح صديقه 1."))
        for (s in m.steps) assertFalse(SubStepExplainer.explain(s, m).isBlank())
    }
}

// Android app/src/test/java/com/example/app/abacus/MistakeAnalyzerTest.kt'in kopyası (Türkçe metinler).
// Kayıtlar, çocuğun boncukları tek tek oynatırken abaküste görünen değerler: her dokunuş bir değer.
package com.sorobit.shared.sobianaliz

import com.sorobit.shared.sobianaliz.MistakeAnalyzer.Kind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MistakeAnalyzerTest {

    /** 248'i boncuk boncuk yazmak: yüzler 2, onlar 4, birler 5+3. */
    private val write248 = listOf(0, 100, 200, 210, 220, 230, 240, 245, 246, 247, 248)

    @Test
    fun unutulan_10_gelir_248_arti_223() {
        val log = write248 + listOf(348, 448, 498, 488, 478, 468, 463, 462, 461)
        val d = MistakeAnalyzer.analyze(248, "+", 223, log, 461)!!
        assertEquals(Kind.MISSING_STEP, d.kind)
        assertEquals(
            "Birler basamağında 3 eklerken 10'luk kural gerekiyordu: 10 gelir, büyük kardeşi 7 gider. " +
                "Sen \"10 gelir\" adımını yapmamışsın. Onlar sütununa 1 boncuk gelmeliydi.",
            d.text,
        )
        assertEquals(0, d.move!!.col)
        assertEquals(AbacusRule.TEN, d.move!!.rule)
    }

    @Test
    fun besli_kuralda_kardes_gitmedi() {
        val d = MistakeAnalyzer.analyze(3, "+", 4, listOf(0, 1, 2, 3, 8), 8)!!
        assertEquals(Kind.MISSING_STEP, d.kind)
        assertTrue(d.text.contains("\"kardeşi 1 gider\" adımını yapmamışsın"), d.text)
    }

    @Test
    fun yanlis_sutun_degeri() {
        val d = MistakeAnalyzer.analyze(21, "+", 13, listOf(0, 10, 20, 21, 31, 32, 33), 33)!!
        assertEquals(Kind.WRONG_COLUMN, d.kind)
        assertTrue(d.text.endsWith("Birler sütununda 4 olmalıydı, sende 3 oldu."), d.text)
    }

    @Test
    fun ilk_sayi_yanlis_yazilmis_ikinci_dogru_islenmis() {
        val log = listOf(0, 100, 200, 210, 220, 230, 235, 236, 237, 238, 338, 438, 448, 458, 461)
        val d = MistakeAnalyzer.analyze(248, "+", 223, log, 461)!!
        assertEquals(Kind.WRONG_FIRST_NUMBER, d.kind)
        assertTrue(d.text.startsWith("İlk sayıyı yazarken onlar basamağına 4 yazılmalıydı, sende 3 oldu."), d.text)
        assertNull(d.move)
    }

    @Test
    fun cikarmada_10_gider_unutuldu() {
        val log = listOf(0, 10, 20, 21, 22, 23, 28, 27)
        val d = MistakeAnalyzer.analyze(23, "-", 6, log, 27)!!
        assertEquals(Kind.MISSING_STEP, d.kind)
        assertTrue(d.text.contains("\"10 gider\" adımını yapmamışsın. Onlar sütunundan 1 boncuk gitmeliydi."), d.text)
        assertEquals(AbacusRule.BEAD, d.move!!.rule)
    }

    @Test
    fun cikarmada_10_luk_kural_kardes_gelmedi() {
        // 41 - 3: birlerde 1'den 3 çıkmaz → 10'luk: 10 gider (onlar 4→3: 31), kardeşi 7 gelir → 38.
        // Çocuk 10'u götürdü ama 7 getirmedi → 31.
        val d = MistakeAnalyzer.analyze(41, "-", 3, listOf(0, 10, 20, 30, 40, 41, 31), 31)!!
        assertEquals(Kind.MISSING_STEP, d.kind)
        assertEquals(
            "Birler basamağında 3 çıkarırken 10'luk kural gerekiyordu: 10 gider, kardeşi 7 gelir. " +
                "Sen \"kardeşi 7 gelir\" adımını yapmamışsın.",
            d.text,
        )
        assertEquals(AbacusRule.TEN, d.move!!.rule)
    }

    @Test
    fun kafadan_yazilan_cevapta_susar() {
        assertNull(MistakeAnalyzer.analyze(248, "+", 223, listOf(0, 100, 400, 460, 461), 461))
        assertNull(MistakeAnalyzer.analyze(248, "+", 223, emptyList(), 461))
    }

    @Test
    fun dogru_cevapta_ve_carpmada_susar() {
        assertNull(MistakeAnalyzer.analyze(3, "+", 4, listOf(0, 3, 8, 7), 7))
        assertNull(MistakeAnalyzer.analyze(3, "x", 4, listOf(0, 3, 12), 13))
    }

    @Test
    fun tek_adimli_hamlede_hic_yapmamak_unutulan_adim_sayilmaz() {
        val d = MistakeAnalyzer.analyze(21, "+", 13, listOf(0, 10, 20, 21, 31), 31)!!
        assertEquals(Kind.WRONG_COLUMN, d.kind)
    }

    @Test
    fun adim_teshisi_ve_yazma_teshisi() {
        // Analiz ekranında tek adım: 3 + 4 (5 gelir, kardeşi 1 gider), çocuk yalnızca 5'i getirdi.
        val m = StepExplainer.explain(3, HomeworkOp.ADD, 4)!!.moves.single()
        val d = MistakeAnalyzer.diagnoseStep(m, listOf(3, 8), 8)
        assertEquals(Kind.MISSING_STEP, d.kind)
        assertEquals("Onlar basamağına 4 yazılmalı, sende 3 var.", MistakeAnalyzer.diagnoseWrite(248, 238))
        assertEquals("Yüzler basamağına 2 yazılmalı, sende 0 var.", MistakeAnalyzer.diagnoseWrite(248, 48))
    }

    @Test
    fun adim_sozleri() {
        val m = StepExplainer.explain(8, HomeworkOp.ADD, 6)!!.moves.single()
        assertEquals(listOf("1 boncuk gelir", "5 gider", "10 gelir"), m.steps.map { MistakeAnalyzer.stepPhrase(it, m) })
        val s = StepExplainer.explain(10, HomeworkOp.SUB, 1)!!.moves.single()
        assertEquals(listOf("10 gider", "kardeşi 9 gelir"), s.steps.map { MistakeAnalyzer.stepPhrase(it, s) })
        assertEquals("on binler", MistakeAnalyzer.columnName(4))
        assertEquals("6. sütun", MistakeAnalyzer.columnName(5))
        assertEquals("boncuk kuralı", MistakeAnalyzer.ruleName(AbacusRule.BEAD))
    }

    // ── Flaş dizisi ─────────────────────────────────────────────────────────

    /** 50 + 28 + 9: 78'e 9 eklerken 10 geldi ama 1 yerine 2 gitti → 86. */
    private val log50_28_9 = listOf(0, 50, 50, 60, 70, 75, 76, 77, 78, 78, 88, 86)
    private val marks50_28_9 = listOf(0, 2, 9)

    @Test
    fun flas_yalnizca_hatali_islem_78_arti_9() {
        val list = MistakeAnalyzer.findSequenceMistakes(listOf(50, 28, 9), log50_28_9, marks50_28_9, 86)!!
        assertEquals(1, list.size)
        val m = list[0]
        assertEquals(2, m.index)
        assertEquals(78, m.base)
        assertEquals(86, m.value)
        assertEquals(AbacusRule.TEN, m.rule)
        assertEquals(Kind.WRONG_COLUMN, m.diagnosis!!.kind)
        assertTrue(m.diagnosis!!.text.contains("Birler sütununda 7 olmalıydı, sende 6 oldu"), m.diagnosis!!.text)
    }

    @Test
    fun flas_hatadan_sonraki_dogru_islem_hata_sayilmaz() {
        val log = log50_28_9 + listOf(86, 96, 91)
        val list = MistakeAnalyzer.findSequenceMistakes(listOf(50, 28, 9, 5), log, marks50_28_9 + 12, 91)!!
        assertEquals(listOf(2), list.map { it.index })
        assertEquals(86, list[0].value)
    }

    @Test
    fun flas_gec_kalan_cocuk_ve_hata() {
        val log = listOf(0, 50, 50, 50, 55, 56, 57, 58, 68, 63)
        val list = MistakeAnalyzer.findSequenceMistakes(listOf(50, 9, 5), log, listOf(0, 2, 3), 63)!!
        assertEquals(listOf(1), list.map { it.index })
        assertEquals(50, list[0].base)
        assertEquals(58, list[0].value)
    }

    @Test
    fun flas_gec_kalan_ama_dogru_cocuk_hatasiz() {
        val log = listOf(0, 50, 50, 50, 55, 59, 64)
        assertEquals(emptyList(), MistakeAnalyzer.findSequenceMistakes(listOf(50, 9, 5), log, listOf(0, 2, 3), 64))
    }

    @Test
    fun flas_kayit_uygun_degilse_susar() {
        assertNull(MistakeAnalyzer.findSequenceMistakes(listOf(50, 9), listOf(0, 50), listOf(0), 58))
        assertNull(MistakeAnalyzer.findSequenceMistakes(listOf(50, 9), emptyList(), emptyList(), 58))
    }

    @Test
    fun kural_ozeti() {
        fun m(rule: AbacusRule?) = MistakeAnalyzer.SequenceMistake(1, 0, 1, 2, rule, null)
        assertEquals(
            "10'luk kuralda 2 hata ve boncuk kuralında 1 hata",
            MistakeAnalyzer.ruleSummary(listOf(m(AbacusRule.BEAD), m(AbacusRule.TEN), m(AbacusRule.TEN))),
        )
        assertEquals("5'lik kuralda 1 hata", MistakeAnalyzer.ruleSummary(listOf(m(AbacusRule.FIVE))))
        assertEquals(
            "ilk sayıyı yazarken 1 hata, kuralsız işlemlerde 1 hata ve 5'lik kuralda 1 hata",
            MistakeAnalyzer.ruleSummary(listOf(m(AbacusRule.FIVE), m(null), m(AbacusRule.DIRECT))),
        )
        assertEquals("", MistakeAnalyzer.ruleSummary(emptyList()))
    }
}

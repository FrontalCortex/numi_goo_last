package com.example.app.abacus

import com.example.app.abacus.MistakeAnalyzer.Kind
import com.example.app.homework.AbacusRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [MistakeAnalyzer] testleri. Kayıtlar, çocuğun boncukları tek tek oynatırken abaküste görünen
 * değerler: her dokunuş bir değer. Yanlış teşhis en kötü sonuç (çocuk doğru yaptığını yanlış
 * sanır), bu yüzden "susmalı" durumlar da test ediliyor.
 */
class MistakeAnalyzerTest {

    /** 248'i boncuk boncuk yazmak: yüzler 2, onlar 4, birler 5+3. */
    private val write248 = listOf(0, 100, 200, 210, 220, 230, 240, 245, 246, 247, 248)

    @Test
    fun unutulan_10_gelir_248_arti_223() {
        // 248 + 223: yüzler +2 (448), onlar +2 5'lik (5 gelir → 498, kardeşi 3 gider → 468),
        // birler +3: 10'luk; çocuk büyük kardeşi 7'yi götürdü ama 10 gelmedi → 461.
        val log = write248 + listOf(348, 448, 498, 488, 478, 468, 463, 462, 461)
        val d = MistakeAnalyzer.analyze(248, "+", 223, log, 461)!!
        assertEquals(Kind.MISSING_STEP, d.kind)
        assertEquals(
            "Birler basamağında 3 eklerken 10'luk kural gerekiyordu: 10 gelir, büyük kardeşi 7 gider. " +
                "Sen \"10 gelir\" adımını yapmamışsın. Onlar sütununa 1 boncuk gelmeliydi.",
            d.text,
        )
    }

    @Test
    fun besli_kuralda_kardes_gitmedi() {
        // 3 + 4: 5 gelir, kardeşi 1 gider → 7. Çocuk 5'i getirdi, 1'i götürmedi → 8.
        val d = MistakeAnalyzer.analyze(3, "+", 4, listOf(0, 1, 2, 3, 8), 8)!!
        assertEquals(Kind.MISSING_STEP, d.kind)
        assertTrue(d.text, d.text.contains("\"kardeşi 1 gider\" adımını yapmamışsın"))
    }

    @Test
    fun yanlis_sutun_degeri() {
        // 21 + 13: onlar +1 (31), birler +3 → 34. Çocuk birlerde 2 ekledi → 33.
        val d = MistakeAnalyzer.analyze(21, "+", 13, listOf(0, 10, 20, 21, 31, 32, 33), 33)!!
        assertEquals(Kind.WRONG_COLUMN, d.kind)
        assertTrue(d.text, d.text.endsWith("Birler sütununda 4 olmalıydı, sende 3 oldu."))
    }

    @Test
    fun ilk_sayi_yanlis_yazilmis_ikinci_dogru_islenmis() {
        // 248 yerine 238 yazıp 223'ü doğru ekledi → 461.
        val log = listOf(0, 100, 200, 210, 220, 230, 235, 236, 237, 238, 338, 438, 448, 458, 461)
        val d = MistakeAnalyzer.analyze(248, "+", 223, log, 461)!!
        assertEquals(Kind.WRONG_FIRST_NUMBER, d.kind)
        assertTrue(d.text, d.text.startsWith("İlk sayıyı yazarken onlar basamağına 4 yazılmalıydı, sende 3 oldu."))
    }

    @Test
    fun cikarmada_10_gider_unutuldu() {
        // 23 - 6: 10 gider (onlar 2→1), 5 gelir, kardeşi 1 gider → 17. Çocuk onlardan 1 almadı → 27.
        val log = listOf(0, 10, 20, 21, 22, 23, 28, 27)
        val d = MistakeAnalyzer.analyze(23, "-", 6, log, 27)!!
        assertEquals(Kind.MISSING_STEP, d.kind)
        assertTrue(d.text, d.text.contains("\"10 gider\" adımını yapmamışsın. Onlar sütunundan 1 boncuk gitmeliydi."))
    }

    @Test
    fun kafadan_yazilan_cevapta_susar() {
        // İlk sayı hiç yazılmamış, doğrudan 461 kurulmuş: adım adım gidilmemiş.
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
        // 21 + 13'te birlerde hiçbir şey yapılmadı (31'de kaldı): "adımı yapmamışsın" değil, sütun farkı.
        val d = MistakeAnalyzer.analyze(21, "+", 13, listOf(0, 10, 20, 21, 31), 31)!!
        assertEquals(Kind.WRONG_COLUMN, d.kind)
    }

    // ── Flaş dizisi ─────────────────────────────────────────────────────────
    // Kayıtta her sayı göründüğü anda abaküsün değeri bir kez daha var (marks bunların sırası).

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
        // 1 yerine 2 götürmek "adımı yapmamışsın" değil, yanlış sütun değeri.
        assertEquals(Kind.WRONG_COLUMN, m.diagnosis!!.kind)
        assertTrue(m.diagnosis!!.text, m.diagnosis!!.text.contains("Birler sütununda 7 olmalıydı, sende 6 oldu"))
    }

    @Test
    fun flas_hatadan_sonraki_dogru_islem_hata_sayilmaz() {
        // ... sonra 86'ya 5 doğru eklendi (10 gelir 5 gider: 96, 91); cevap 91 (doğrusu 92).
        val log = log50_28_9 + listOf(86, 96, 91)
        val list = MistakeAnalyzer.findSequenceMistakes(listOf(50, 28, 9, 5), log, marks50_28_9 + 12, 91)!!
        assertEquals(listOf(2), list.map { it.index })
        assertEquals(86, list[0].value)
    }

    @Test
    fun flas_gec_kalan_cocuk_ve_hata() {
        // 50 + 9 + 5: çocuk 9'u 5 göründükten sonra işledi ve 58 yaptı; 5'i ondan doğru ekledi → 63.
        val log = listOf(0, 50, 50, 50, 55, 56, 57, 58, 68, 63)
        val list = MistakeAnalyzer.findSequenceMistakes(listOf(50, 9, 5), log, listOf(0, 2, 3), 63)!!
        assertEquals(listOf(1), list.map { it.index })
        assertEquals(50, list[0].base)
        assertEquals(58, list[0].value)
    }

    @Test
    fun flas_gec_kalan_ama_dogru_cocuk_hatasiz() {
        // 9'u geç işledi ama doğru: 59, sonra 64.
        val log = listOf(0, 50, 50, 50, 55, 59, 64)
        assertEquals(emptyList<Any>(), MistakeAnalyzer.findSequenceMistakes(listOf(50, 9, 5), log, listOf(0, 2, 3), 64))
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
    }
}

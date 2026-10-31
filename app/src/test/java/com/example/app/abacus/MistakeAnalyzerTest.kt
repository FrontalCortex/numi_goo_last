package com.example.app.abacus

import com.example.app.abacus.MistakeAnalyzer.Kind
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
}

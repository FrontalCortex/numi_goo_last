// 34 öğreticinin içeriği (icerik/OgreticiN.kt, donustur.js ile Android TutorialFragment.kt'den üretildi):
// beklenen sayılar ios/docs/OGRETICI_PLANI.md bölüm 1.2 tablosundan (Android kaynağı sayılarak çıkarıldı).
package com.sorobit.shared.ogretici

import com.sorobit.shared.ders.GlobalLessonData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OgreticiIcerikTest {
    /** Bir öğreticinin Android'den sayılmış özeti. */
    private class Beklenen(
        val adim: Int,
        val soru: Int,
        val secenekli: Int,
        val boncuk: Int,
        val kuralTablosu: String?,
        val kuralMetni: Int,
        val renkliSoru: Int,
        val geriCevap: Int,
    )

    private val beklenenler: Map<Int, Beklenen> = mapOf(
        1 to Beklenen(30, 5, 0, 18, null, 0, 0, 0),
        100 to Beklenen(30, 5, 0, 50, null, 0, 0, 2),
        101 to Beklenen(35, 12, 1, 60, null, 0, 24, 9),
        2 to Beklenen(17, 4, 0, 16, null, 0, 9, 2),
        102 to Beklenen(26, 6, 0, 34, null, 0, 16, 4),
        103 to Beklenen(33, 8, 0, 50, null, 0, 22, 9),
        3 to Beklenen(82, 6, 2, 50, "fiveRuleTable", 0, 55, 7),
        4 to Beklenen(27, 4, 1, 26, "fiveRuleTable", 0, 19, 4),
        5 to Beklenen(68, 4, 2, 58, "tenRuleTable", 0, 42, 4),
        6 to Beklenen(58, 6, 0, 80, "tenRuleTable", 33, 0, 4),
        104 to Beklenen(50, 6, 0, 80, null, 0, 0, 4),
        7 to Beklenen(38, 3, 1, 28, "tenRuleTableLinearLayout", 0, 0, 3),
        105 to Beklenen(53, 6, 0, 80, "tenRuleTableLinearLayout", 32, 0, 4),
        8 to Beklenen(80, 0, 2, 107, "BeadRuleTable", 0, 0, 0),
        9 to Beklenen(36, 3, 2, 22, "BeadRuleTable", 0, 29, 5),
        10 to Beklenen(22, 3, 0, 22, null, 0, 14, 3),
        11 to Beklenen(39, 3, 1, 28, "extractionFiveRuleTable", 0, 0, 3),
        1100 to Beklenen(34, 4, 3, 24, "extractionFiveRuleTable", 0, 28, 8),
        12 to Beklenen(63, 0, 1, 62, "tenRuleExtractionTableLayout", 0, 1, 0),
        13 to Beklenen(27, 3, 0, 22, "tenRuleExtractionTableLayout", 0, 21, 3),
        14 to Beklenen(65, 8, 1, 60, "tenRuleExtractionTableLayout", 40, 50, 7),
        999 to Beklenen(2, 0, 0, 0, null, 0, 0, 0),
        15 to Beklenen(31, 0, 0, 37, null, 24, 26, 0),
        16 to Beklenen(60, 7, 1, 50, null, 4, 6, 7),
        17 to Beklenen(24, 5, 0, 34, "BeadRuleExtractionTable", 4, 20, 6),
        18 to Beklenen(34, 2, 1, 44, null, 18, 21, 2),
        19 to Beklenen(35, 3, 1, 38, null, 21, 26, 4),
        20 to Beklenen(55, 4, 4, 78, null, 16, 36, 6),
        21 to Beklenen(25, 3, 0, 28, null, 0, 10, 2),
        22 to Beklenen(36, 6, 1, 36, null, 0, 20, 6),
        23 to Beklenen(27, 6, 0, 44, null, 0, 20, 6),
        24 to Beklenen(16, 0, 0, 0, null, 0, 0, 0),
        25 to Beklenen(8, 0, 0, 0, null, 0, 0, 0),
        26 to Beklenen(4, 0, 0, 0, null, 0, 0, 0),
    )

    @Test
    fun otuzDortOgreticininHepsiVar() {
        assertEquals(34, beklenenler.size)
        assertEquals(beklenenler.keys, OgreticiKatalogu.numaralar.toSet())
        assertEquals(1270, beklenenler.values.sumOf { it.adim })
        for (no in OgreticiKatalogu.numaralar) {
            val icerik = assertNotNull(OgreticiKatalogu.icerik(no), "öğretici $no içeriği yok")
            assertEquals(no, icerik.numara)
        }
    }

    @Test
    fun adimSayilariVeOzellikSayilariAndroidIleAyni() {
        for ((no, b) in beklenenler) {
            val icerik = OgreticiKatalogu.icerik(no)!!
            val ad = icerik.adimlar
            assertEquals(b.adim, icerik.adimSayisi, "öğretici $no: adım sayısı")
            assertEquals(b.soru, ad.count { it.abakusTiklanabilir }, "öğretici $no: abaküs sorusu")
            assertEquals(b.secenekli, ad.count { it.secenekler.isNotEmpty() }, "öğretici $no: seçenekli adım")
            assertEquals(b.boncuk, ad.sumOf { it.hareketler.size }, "öğretici $no: boncuk hareketi")
            assertEquals(b.kuralTablosu, icerik.kuralTablosu, "öğretici $no: kural tablosu")
            assertEquals(b.kuralMetni, ad.count { it.kuralMetni != null }, "öğretici $no: kural metni")
            assertEquals(b.renkliSoru, ad.count { it.soruRenkleri.isNotEmpty() }, "öğretici $no: renkli soru")
            assertEquals(b.geriCevap, ad.count { it.geriCevap != OgreticiSabit.SAYI_YOK }, "öğretici $no: geri cevap")
        }
    }

    @Test
    fun herAdimdaMetinVarVeSecenekler_gecerli() {
        for (no in OgreticiKatalogu.numaralar) {
            OgreticiKatalogu.icerik(no)!!.adimlar.forEachIndexed { i, a ->
                val yer = "öğretici $no adım ${i + 1}"
                assertTrue(a.metin.isNotBlank(), "$yer: metin boş")
                if (a.secenekler.isNotEmpty()) assertTrue(a.gecerliSecenekVar(), "$yer: seçenekler geçersiz")
                if (a.abakusTiklanabilir) {
                    assertTrue(a.cevap >= 0, "$yer: cevap yok")
                    assertTrue(!a.ilerlenebilir, "$yer: soru adımı dokunmayla ilerliyor")
                }
            }
        }
    }

    @Test
    fun katalogdakiHerOgreticiNumarasiIcinIcerikVar() {
        val numaralar = HashSet<Int>()
        for (kisim in 1..9) {
            for (item in GlobalLessonData.createLessonItems(kisim)) {
                if (item.tutorialNumber != 0) numaralar.add(item.tutorialNumber)
            }
        }
        assertTrue(numaralar.isNotEmpty(), "katalogda öğretici numarası bulunamadı")
        for (no in numaralar) assertTrue(OgreticiKatalogu.varMi(no), "ders kataloğundaki öğretici $no için içerik yok")
        // Planda: katalogda 32 öğretici kullanılıyor, 104 ve 999 hiçbir derste yok.
        assertEquals(OgreticiKatalogu.numaralar.toSet() - setOf(104, 999), numaralar)
    }
}

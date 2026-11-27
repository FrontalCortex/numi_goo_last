package com.sorobit.shared.baglanti

import com.sorobit.shared.ders.GlobalLessonData
import com.sorobit.shared.ders.LessonItem
import com.sorobit.shared.kupa.CupMathOperation
import com.sorobit.shared.kupa.CupRuleEngine
import com.sorobit.shared.matematik.MathOperationGenerator
import com.sorobit.shared.matematik.RastgeleKaynagi
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaketKopruleriTest {
    @AfterTest
    fun temizle() = RastgeleKaynagi.varsayilanaDon()

    @Test
    fun uretecKoprusuAsilUretecleAyniSonucuVerir() {
        // Aynı tohumla köprü üzerinden ve doğrudan çağrı birebir aynı olmalı.
        RastgeleKaynagi.rastgele = Random(42)
        val dogrudanDizi = MathOperationGenerator.generateSequenceExtractionFiveRules(4)
        val dogrudanCarpma = MathOperationGenerator.multiplicationThreeTwoFull()
        RastgeleKaynagi.rastgele = Random(42)
        assertEquals(dogrudanDizi, MatematikDailyQuestionGenerators.generateSequenceExtractionFiveRules(4))
        val kopru = MatematikDailyQuestionGenerators.multiplicationThreeTwoFull()
        assertEquals(dogrudanCarpma.firstNumber, kopru.firstNumber)
        assertEquals(dogrudanCarpma.operator, kopru.operator)
        assertEquals(dogrudanCarpma.secondNumber, kopru.secondNumber)
    }

    @Test
    fun lessonItemGorunumuAlanlariAynenTasir() {
        val asil = GlobalLessonData.createLessonItems(1)
        val gorunum = asil.asOyunLessonItems()
        assertEquals(asil.size, gorunum.size)
        asil.zip(gorunum).forEach { (a, g) ->
            assertEquals(a.type, g.type)
            assertEquals(a.title, g.title)
            assertEquals(a.titleUnit, g.titleUnit)
            assertEquals(a.stepIsFinish, g.stepIsFinish)
            assertEquals(a.record, g.record)
            assertEquals(a.cupPoint1, g.cupPoint1)
            assertEquals(a.cupPoint2, g.cupPoint2)
            assertEquals(a.isBlinding, g.isBlinding)
            assertEquals(a.stepCupIcon, g.stepCupIcon)
            assertEquals(a.mapFragmentIndex, g.mapFragmentIndex)
        }
        // Görünüm canlıdır: asıl item değişince yansır.
        val ilk = asil.first()
        val g = ilk.asOyunLessonItem()
        ilk.record = 777
        assertEquals(777, g.record)
    }

    @Test
    fun gunlukSoruHavuzuGercekKatalogdanKaynakUretir() {
        // Android: DailyQuestionPoolBuilder.buildSourcesForPart(GlobalLessonData.createLessonItems(p), p)
        val havuz = gunlukSoruHavuzuKur(Random(7))
        var toplam = 0
        for (part in 1..8) {
            // Güncel Android şablonunda hiçbir sandık bitmiş gelmez (eskiden part 1–6'da 14 tane vardı);
            // ilerlemiş bir kullanıcıyı taklit etmek için bütün sandıklar bitmiş sayılır.
            val itemlar = GlobalLessonData.createLessonItems(part).map {
                if (it.type == LessonItem.TYPE_CHEST) it.copy(stepIsFinish = true) else it
            }
            val kaynaklar = havuz.buildSourcesForPart(itemlar.asOyunLessonItems(), part)
            val sandikSayisi = itemlar.count { it.type == LessonItem.TYPE_CHEST }
            assertTrue(kaynaklar.size <= sandikSayisi, "part $part: kaynak sayısı sandık sayısını aşamaz")
            kaynaklar.forEach { assertEquals(LessonItem.TYPE_CHEST, it.item.type) }
            toplam += kaynaklar.size
        }
        assertTrue(toplam > 0, "katalogdan en az bir günlük soru kaynağı çıkmalı")
    }

    @Test
    fun kupaDersiYerTutucuyaUygulanir() {
        val yerTutucu = GlobalLessonData.createLessonItems(9).first()
        val ezme = CupRuleEngine.buildLessonItem(cupScore = 200, difficultyLevel = 2, random = Random(1))
        val sonuc = ezme.applyToPlaceholder()
        assertEquals(ezme.timePeriod, sonuc.timePeriod)
        assertEquals(ezme.cupDigitSize, sonuc.cupDigitSize)
        assertEquals(ezme.cupNumberCount, sonuc.cupNumberCount)
        assertEquals(2, sonuc.cupDifficultyLevel)
        assertEquals(ezme.cupWinDelta, sonuc.cupWinDelta)
        assertEquals(ezme.cupLossDelta, sonuc.cupLossDelta)
        // Android: isBlinding=false → null yazılır.
        assertNull(sonuc.isBlinding)
        assertNull(sonuc.isExtraction)
        // Ezilmeyen alanlar yer tutucudan gelir.
        assertEquals(yerTutucu.title, sonuc.title)
        assertEquals(yerTutucu.type, sonuc.type)
    }

    @Test
    fun kupaIslemiAsilTureCevrilir() {
        val m = CupMathOperation(12, "x", 3).toMathOperation()
        assertEquals(12, m.firstNumber)
        assertEquals("x", m.operator)
        assertEquals(3, m.secondNumber)
    }
}

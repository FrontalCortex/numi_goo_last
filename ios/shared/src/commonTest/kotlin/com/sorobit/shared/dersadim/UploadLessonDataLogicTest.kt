// AbacusFragment / BlindingLessonFragment uploadLessonData mantığının testleri.
package com.sorobit.shared.dersadim

import com.sorobit.shared.ders.LessonItem
import com.sorobit.shared.matematik.MathOperation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertSame

class UploadLessonDataLogicTest {
    private fun item(
        start: Int?,
        finish: Int?,
        stepCount: Int,
        stepIsFinish: Boolean = false,
    ) = LessonItem(
        type = LessonItem.TYPE_LESSON,
        title = "t",
        offset = 0,
        isCompleted = true,
        stepCount = stepCount,
        startStepNumber = start,
        finishStepNumber = finish,
        stepIsFinish = stepIsFinish,
    )

    @Test
    fun requestedLessonIdKurallari() {
        assertEquals(9, UploadLessonDataLogic.requestedLessonId(item(9, 10, 3)))
        // Ders bitmişse son adım.
        assertEquals(10, UploadLessonDataLogic.requestedLessonId(item(9, 10, 3, stepIsFinish = true)))
        // 0 ya da negatif başlangıç 1 sayılır.
        assertEquals(1, UploadLessonDataLogic.requestedLessonId(item(0, 5, 1)))
        assertEquals(1, UploadLessonDataLogic.requestedLessonId(item(-4, 5, 1)))
        // Android'deki `!!`: null alanlar çöker.
        assertFails { UploadLessonDataLogic.requestedLessonId(item(null, 5, 1)) }
        assertFails { UploadLessonDataLogic.requestedLessonId(item(null, 5, 1, stepIsFinish = true)) }
        assertFails { UploadLessonDataLogic.requestedLessonId(item(5, null, 1, stepIsFinish = true)) }
    }

    @Test
    fun abakusBundleDoluysaAynenKullanilir() {
        val bundle = listOf(MathOperation(1, "+", 2))
        val sonuc = UploadLessonDataLogic.abacusUploadLessonData(bundle, item(null, null, 1))
        assertSame(bundle, sonuc.operations)
        assertNull(sonuc.lessonStep)
        assertNull(sonuc.resolvedLessonId)
    }

    @Test
    fun abakusTablodanYukler() {
        // "5'lik Toplama - Kurallar": start=8, finish=10, stepCount=3 → alt sınır 8.
        val ilk = UploadLessonDataLogic.abacusUploadLessonData(emptyList(), item(8, 10, 3))
        assertEquals(8, ilk.lessonStep)
        assertEquals(8, ilk.resolvedLessonId)
        assertEquals(LessonOperations.getLessonOperations(8), ilk.operations)
        // Bitmiş ders: 10 istenir, tabloda yok → 9'a iner. lessonStep yine startStepNumber'dır.
        val bitmis = DersAdimTestYardimci.tohumla(4) {
            UploadLessonDataLogic.abacusUploadLessonData(emptyList(), item(8, 10, 3, stepIsFinish = true))
        }
        assertEquals(8, bitmis.lessonStep)
        assertEquals(9, bitmis.resolvedLessonId)
        // Android'deki ayrıntı: çözümleme, boş mu diye bakarken listeyi bir kez üretip atar (9 için bir üretim),
        // sonra asıl liste yeniden üretilir. Tohumlu karşılaştırmada bu fazladan üretim de hesaba katılır.
        val beklenen = DersAdimTestYardimci.tohumla(4) {
            LessonOperations.getLessonOperations(9)
            LessonOperations.getLessonOperations(9)
        }
        assertEquals(beklenen, bitmis.operations)
        // Android'deki boşluk: start=29, finish=31, stepCount=3 → 29'da durur, liste boş.
        val bos = UploadLessonDataLogic.abacusUploadLessonData(emptyList(), item(29, 31, 3))
        assertEquals(29, bos.resolvedLessonId)
        assertEquals(emptyList(), bos.operations)
        assertFails { UploadLessonDataLogic.abacusUploadLessonData(emptyList(), item(null, null, 1)) }
    }

    @Test
    fun korDersTablodanYukler() {
        // Part 4 "Kuralsız Toplama - 2,5 Saniye Arayla": start=5, finish=7, stepCount=3.
        val ilk = DersAdimTestYardimci.tohumla(2) {
            UploadLessonDataLogic.blindingUploadLessonData(emptyList(), item(5, 7, 3), 4)
        }
        assertEquals(5, ilk.lessonStep)
        assertEquals(5, ilk.resolvedLessonId)
        // Çözümlemenin üretip attığı ilk liste + asıl liste (bkz. abakusTablodanYukler).
        val beklenen = DersAdimTestYardimci.tohumla(2) {
            LessonOperations.getLessonOperationsBlinding(5)
            LessonOperations.getLessonOperationsBlinding(5)
        }
        assertEquals(beklenen, ilk.operations)
        val bitmis = UploadLessonDataLogic.blindingUploadLessonData(emptyList(), item(5, 7, 3, stepIsFinish = true), 4)
        assertEquals(5, bitmis.resolvedLessonId)
        val bundle: List<Any> = listOf(listOf(1, 2, 3))
        val hazir = UploadLessonDataLogic.blindingUploadLessonData(bundle, item(5, 7, 3), 4)
        assertSame(bundle, hazir.operations)
        assertNull(hazir.resolvedLessonId)
    }

    @Test
    fun kupaModundaTabloyaGidilmez() {
        // globalPartId == 9: bundle boş olsa da (ve item alanları null olsa da) erken dönülür.
        val sonuc = UploadLessonDataLogic.blindingUploadLessonData(emptyList(), item(null, null, 1), 9)
        assertEquals(emptyList(), sonuc.operations)
        assertNull(sonuc.lessonStep)
        assertNull(sonuc.resolvedLessonId)
        // Kupa modu değilse aynı item çöker (Android: startStepNumber!!).
        assertFails { UploadLessonDataLogic.blindingUploadLessonData(emptyList(), item(null, null, 1), 4) }
    }

    @Test
    fun ogreticiBitinceAbakusTablosuCozumlemesizSorulur() {
        assertEquals(LessonOperations.getLessonOperations(8), UploadLessonDataLogic.tutorialFinishedOperations(8))
        // Çözümleme yok: 10 için boş liste (uploadLessonData olsaydı 9'a inerdi).
        assertEquals(emptyList(), UploadLessonDataLogic.tutorialFinishedOperations(10))
    }
}

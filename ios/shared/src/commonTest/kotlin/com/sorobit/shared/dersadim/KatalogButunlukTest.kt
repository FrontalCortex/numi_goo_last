// Ders kataloğundaki her item'ın adım kimlikleri için tabloların sonuç verdiğini (ya da Android'de de
// vermediğini) doğrular.
package com.sorobit.shared.dersadim

import com.sorobit.shared.ders.GlobalLessonData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KatalogButunlukTest {
    @Test
    fun katalogOzetiAndroidReferansiylaAyni() {
        assertEquals(ALTIN_KATALOG_OZET, KatalogButunluk.ozetSatirlari())
    }

    @Test
    fun herDersAdimiSoruUretir_istisnaYok() {
        // Eski kopyadaki tek boşluk (part 1 "10'luk Toplama - 3 Basamaklı": start 28, finish 31, stepCount 3) güncel
        // Android kataloğunda düzeltildi (finish 30): artık hiçbir item'ın çözülen kimliği boş tabloya düşmez.
        assertEquals(emptyList(), ALTIN_KATALOG_OZET.filter { it.contains("!") })
        assertEquals(emptyList(), KatalogButunluk.ozetSatirlari().filter { it.contains("!") })
    }

    @Test
    fun herItemIlkAdimdaSoruUretir() {
        var sayac = 0
        for (p in KatalogButunluk.PARTLAR) {
            GlobalLessonData.createLessonItems(p).forEachIndexed { i, item ->
                if (item.startStepNumber == null) return@forEachIndexed
                sayac++
                val sonuc: List<Any> = DersAdimTestYardimci.tohumla(3) {
                    if (KatalogButunluk.korTabloMu(p, item)) {
                        UploadLessonDataLogic.blindingUploadLessonData(emptyList(), item, p).operations
                    } else {
                        UploadLessonDataLogic.abacusUploadLessonData(emptyList(), item).operations
                    }
                }
                assertTrue(sonuc.isNotEmpty(), "part=$p index=$i ${item.title}")
            }
        }
        assertEquals(ALTIN_KATALOG_OZET.size, sayac)
        assertEquals(141, sayac)
    }

    @Test
    fun katalogdakiBaslangicKimlikleriTablolardaTanimli() {
        val o = ANDROID_DALLAR_OPERATIONS.toSet()
        val b = ANDROID_DALLAR_BLINDING.toSet()
        for (satir in ALTIN_KATALOG_OZET) {
            val p = satir.split("|")
            val start = p[3].substringBefore("-").toInt()
            assertTrue(start in (if (p[2] == "B") b else o), satir)
        }
    }
}

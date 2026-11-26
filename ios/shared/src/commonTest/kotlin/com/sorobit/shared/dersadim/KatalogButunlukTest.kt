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
    fun herDersAdimiSoruUretirBilinenIstisnaHaric() {
        // Android'deki tek boşluk: part 1, index 19 ("10'luk Toplama - 3 Basamaklı": start=28, finish=31,
        // stepCount=3 → alt sınır 29). 29, 30 ve 31 abaküs tablosunda yok; geri adım 29'da durduğu için
        // 28'e inemez ve soru listesi BOŞ gelir. Android'de de böyledir; aynen korunur.
        val boslar = ALTIN_KATALOG_OZET.filter { it.contains("!") }
        assertEquals(listOf("1|19|O|28-31|29|28>28,29>29!,30>29!,31>29!"), boslar)
        assertEquals(boslar, KatalogButunluk.ozetSatirlari().filter { it.contains("!") })
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

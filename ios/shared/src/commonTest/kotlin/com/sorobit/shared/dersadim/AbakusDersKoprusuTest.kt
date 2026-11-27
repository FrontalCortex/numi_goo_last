// AbakusDersKoprusu: iOS ders ekranının kullandığı ince köprünün testleri. Beklenen değerler
// AbacusFragment.kt okunarak türetildi (köprü var olan ortak mantığı çağırır).
package com.sorobit.shared.dersadim

import com.sorobit.shared.abakus.AbacusOperation
import com.sorobit.shared.ders.GlobalLessonData
import com.sorobit.shared.ders.LessonItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AbakusDersKoprusuTest {
    private fun item(
        start: Int?,
        finish: Int?,
        stepCount: Int = 1,
        stepIsFinish: Boolean = false,
        type: Int = LessonItem.TYPE_LESSON,
        worstCupTime: Int? = null,
        hint: String? = null,
    ) = LessonItem(
        type = type,
        title = "Başlık",
        offset = 0,
        isCompleted = true,
        stepCount = stepCount,
        startStepNumber = start,
        finishStepNumber = finish,
        stepIsFinish = stepIsFinish,
        worstCupTime = worstCupTime,
        lessonHint = hint,
    )

    private fun ikiSoru(tur: Int = LessonItem.TYPE_LESSON): AbakusDersAkisi {
        val liste = AbakusSoruListesi()
        liste.ekle(2, "+", 3)
        liste.ekleTekSayi(7)
        assertEquals(2, liste.adet)
        return assertNotNull(AbakusDersKoprusu.akisSorulardan(liste, tur))
    }

    @Test
    fun dersGirdisiUploadLessonDataIleAyniKimligiVerir() {
        val oge = item(start = 1, finish = 1, hint = "bir iki üç dört")
        val g = assertNotNull(AbakusDersKoprusu.dersGirdisi(oge, partId = 1, mapIndex = 0))
        val beklenen = UploadLessonDataLogic.abacusUploadLessonData(emptyList(), oge)
        assertEquals(beklenen.resolvedLessonId, g.lessonOperationsId)
        assertEquals(beklenen.lessonStep, g.lessonStep)
        assertEquals(LessonItem.TYPE_LESSON, g.lessonType)
        assertFalse(g.isBlinding)
        assertEquals("bir iki üç dört", g.lessonHint)
        assertEquals("Başlık", g.title)
        assertEquals("bir iki\nüç dört", AbakusDersKoprusu.ipucuMetni(g.lessonHint))
    }

    @Test
    fun dersGirdisiAndroiddeCokenOgedeNullDoner() {
        assertNull(AbakusDersKoprusu.dersGirdisi(item(null, 5), 1, 0))
        assertNull(AbakusDersKoprusu.dersGirdisi(item(5, null, stepIsFinish = true), 1, 0))
        assertNotNull(AbakusDersKoprusu.dersGirdisi(item(5, null), 1, 0))
    }

    @Test
    fun kupaSuresiOnceOgedenSonraSablondanSonra240() {
        assertEquals(77, AbakusDersKoprusu.dersGirdisi(item(1, 1, worstCupTime = 77), 1, 0)!!.worstCupTime)
        // Şablonda süresi olan ilk öğe bulunur ve yedek olarak kullanılır.
        val sablon = GlobalLessonData.createLessonItems(1)
        val indeks = sablon.indexOfFirst { (it.worstCupTime ?: 0) > 0 }
        if (indeks >= 0) {
            assertEquals(
                sablon[indeks].worstCupTime,
                AbakusDersKoprusu.dersGirdisi(item(1, 1), 1, indeks)!!.worstCupTime,
            )
        }
        // Şablonda karşılığı yoksa 240.
        assertEquals(240, AbakusDersKoprusu.dersGirdisi(item(1, 1), 1, 100_000)!!.worstCupTime)
        assertEquals(240, AbakusDersKoprusu.dersGirdisi(item(1, 1, worstCupTime = 0), 99, 0)!!.worstCupTime)
    }

    @Test
    fun kimliktenAkisKatalogSorulariniTasir() {
        val sorular = LessonOperations.getLessonOperations(1)
        assertTrue(sorular.isNotEmpty())
        val akis = assertNotNull(AbakusDersKoprusu.akisDersKimliginden(1, LessonItem.TYPE_LESSON))
        assertEquals(sorular.size, akis.soruSayisi)
        assertTrue(akis.baslat())
        assertFalse(akis.baslat())
        assertEquals(sorular[0].firstNumber?.toString(), akis.ilkSayiMetni())
        assertEquals(sorular[0].operator, akis.islecMetni())
        assertEquals(sorular[0].secondNumber?.toString(), akis.ikinciSayiMetni())
    }

    @Test
    fun katalogdakiButunSorularCevaplanabilir() {
        for (kimlik in 1..400) {
            for (s in LessonOperations.getLessonOperations(kimlik)) {
                assertTrue(
                    AbakusDersKoprusu.cevaplanabilir(AbacusOperation(s.firstNumber, s.operator, s.secondNumber)),
                    "ders $kimlik: $s",
                )
            }
        }
    }

    @Test
    fun cevaplanamayanSoruAkisiEngeller() {
        assertFalse(AbakusDersKoprusu.cevaplanabilir(AbacusOperation(null, "abc", null)))
        assertFalse(AbakusDersKoprusu.cevaplanabilir(AbacusOperation(3, "+", null)))
        assertFalse(AbakusDersKoprusu.cevaplanabilir(AbacusOperation(null, "+", 3)))
        assertTrue(AbakusDersKoprusu.cevaplanabilir(AbacusOperation(null, null, null)))
        assertTrue(AbakusDersKoprusu.cevaplanabilir(AbacusOperation(null, "12", null)))
        assertTrue(AbakusDersKoprusu.cevaplanabilir(AbacusOperation(1, "?", 2)))
    }

    @Test
    fun dersAkisiDogruYanlisVeBitis() {
        val akis = ikiSoru()
        assertFalse(akis.baslaDugmesiGerekli)
        assertFalse(akis.sureGorunur)
        assertTrue(akis.baslat())
        assertEquals("2", akis.ilkSayiMetni())
        assertEquals("+", akis.islecMetni())
        assertEquals("3", akis.ikinciSayiMetni())

        // Yanlış cevap.
        val yanlis = akis.kontrol(1_000L, true, 4)
        assertEquals(AbakusKontrolSonucu.CEVAPLANDI, yanlis.durum)
        assertFalse(yanlis.dogru)
        assertEquals("Senin cevabın 4", yanlis.cevapMetni)
        assertEquals("Doğru cevap 5", yanlis.dogruCevapMetni)
        assertEquals(0.5f, yanlis.ilerlemeHedefi)
        assertFalse(yanlis.konfeti)
        // 100 ms dolmadan ikinci basış yok sayılır.
        assertEquals(AbakusKontrolSonucu.YOK_SAYILDI, akis.kontrol(1_050L, true, 5).durum)
        assertTrue(akis.devam())
        assertEquals(1, akis.suankiIndeks)

        // Tek sayılı soru: sayı null döner (görünüm eski metni korur), işleç sayının metnidir.
        assertNull(akis.ilkSayiMetni())
        assertEquals("7", akis.islecMetni())
        assertNull(akis.ikinciSayiMetni())

        // Çevrimdışıyken cevap denetlenmez.
        assertEquals(AbakusKontrolSonucu.CEVRIMDISI, akis.kontrol(2_000L, false, 7).durum)
        val dogru = akis.kontrol(3_000L, true, 7)
        assertTrue(dogru.dogru)
        assertEquals(1f, dogru.ilerlemeHedefi)
        assertEquals("", dogru.cevapMetni)
        // Bir yanlış olduğu için konfeti yok.
        assertFalse(dogru.konfeti)
        assertFalse(akis.devam())

        val b = akis.bitis(240)
        // Güncel Android (AbacusFragment: successRate < 75f) → %50 başarı başarısız ekranına gider
        assertEquals(1, b.yonlendirme)
        assertEquals(1, b.correctAnswers)
        assertEquals(2, b.totalQuestions)
        assertEquals(50f, b.successRate)
        assertEquals(250, b.dersPuani)
        assertEquals("0:00", b.time)
        assertEquals(0, b.seconds)
    }

    @Test
    fun hepsiDogruysaSonSorudaKonfeti() {
        val akis = ikiSoru()
        akis.baslat()
        assertFalse(akis.kontrol(1_000L, true, 5).konfeti)
        akis.devam()
        assertTrue(akis.kontrol(2_000L, true, 7).konfeti)
        assertFalse(akis.devam())
        // %100 başarı ≥ %75 → ders sonucu ekranı
        assertEquals(0, akis.bitis(240).yonlendirme)
    }

    @Test
    fun hepsiYanlissaBasarisizEkran() {
        val akis = ikiSoru()
        akis.baslat()
        akis.kontrol(1_000L, true, 0)
        akis.devam()
        akis.kontrol(2_000L, true, 0)
        assertFalse(akis.devam())
        assertEquals(1, akis.bitis(240).yonlendirme)
    }

    @Test
    fun sandikDersindeSureVePuan() {
        val akis = ikiSoru(LessonItem.TYPE_CHEST)
        assertTrue(akis.baslaDugmesiGerekli)
        assertTrue(akis.sureGorunur)
        assertFalse(akis.sureCalisiyor)
        akis.baslat()
        assertTrue(akis.sureCalisiyor)
        assertEquals("0:01", akis.sureAdimi())
        repeat(59) { akis.sureAdimi() }
        assertEquals("1:00", akis.sureMetni)

        assertFalse(akis.kontrol(1_000L, true, 5).sureDurdu)
        akis.devam()
        // Son soruda süre hemen durur.
        assertTrue(akis.kontrol(2_000L, true, 7).sureDurdu)
        assertFalse(akis.sureCalisiyor)
        akis.devam()

        // Başarı %100 → ders puanı 500; çarpan = 4 − 3·60/240 = 3.25 → toplam 1625.
        val b = akis.bitis(240)
        assertEquals(2, b.yonlendirme)
        assertEquals(500, b.dersPuani)
        assertEquals("1:00", b.time)
        assertEquals(60, b.seconds)
        assertEquals(240, b.worstCupTime)
        assertEquals(3.25f, b.carpan)
        assertEquals(1625, b.toplamPuan)
    }

    @Test
    fun sandikPuaniYetersizseBasarisiz() {
        val akis = ikiSoru(LessonItem.TYPE_CHEST)
        akis.baslat()
        akis.kontrol(1_000L, true, 0)
        akis.devam()
        akis.kontrol(2_000L, true, 7)
        akis.devam()
        // Başarı %50 → 250 puan; süre "0:00" → çarpan 4 → 1000 ≥ 500: sandık sonucu.
        assertEquals(2, akis.bitis(240).yonlendirme)
        val akis2 = ikiSoru(LessonItem.TYPE_CHEST)
        akis2.baslat()
        akis2.kontrol(1_000L, true, 0)
        akis2.devam()
        akis2.kontrol(2_000L, true, 0)
        akis2.devam()
        assertEquals(3, akis2.bitis(240).yonlendirme)
    }

    @Test
    fun bosSoruListesiCokmez() {
        val akis = assertNotNull(AbakusDersKoprusu.akisSorulardan(AbakusSoruListesi(), LessonItem.TYPE_LESSON))
        akis.baslat()
        assertNull(akis.ilkSayiMetni())
        val s = akis.kontrol(1_000L, true, 0)
        assertEquals(AbakusKontrolSonucu.CEVAPLANDI, s.durum)
        assertFalse(s.dogru)
        assertFalse(akis.devam())
        assertEquals(1, akis.bitis(240).yonlendirme)
    }
}

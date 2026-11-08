package com.example.app

import com.example.app.model.LessonItem.Companion.TYPE_CHEST
import com.example.app.model.LessonItem.Companion.TYPE_HEADER
import com.example.app.model.LessonItem.Companion.TYPE_LESSON
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialSurveyPolicyTest {

    // 1. bölümün başı, haritadaki sırasıyla.
    private val part1 = listOf(
        TYPE_HEADER to "p1_i00_sayilari_abakuste_tanima",
        TYPE_LESSON to "p1_i01_sayilari_abakuste_tanima",
        TYPE_LESSON to "p1_i02_2_basamakli_sayilar",
        TYPE_LESSON to "p1_i03_3_4_5_basamakli_sayilar",
        TYPE_CHEST to "p1_i04_unite_maratonu",
        TYPE_HEADER to "p1_i05_kuralsiz_toplama",
        TYPE_LESSON to "p1_i06_kuralsiz_toplama_1_basamakli",
        TYPE_LESSON to "p1_i07_kuralsiz_toplama_2_basamakli",
        TYPE_CHEST to "p1_i10_unite_maratonu",
        TYPE_HEADER to "p1_i11_5_lik_toplama",
        TYPE_LESSON to "p1_i12_5_lik_toplama_kurallar",
        TYPE_LESSON to "p1_i13_5_lik_toplama_kuralsiz_topla",
    )

    @Test
    fun uniteninIlkDersiAnketAlir() {
        assertTrue(TutorialSurveyPolicy.isUnitOpener(part1, "p1_i01_sayilari_abakuste_tanima"))
        assertTrue(TutorialSurveyPolicy.isUnitOpener(part1, "p1_i06_kuralsiz_toplama_1_basamakli"))
        assertTrue(TutorialSurveyPolicy.isUnitOpener(part1, "p1_i12_5_lik_toplama_kurallar"))
    }

    @Test
    fun uniteIcindekiSonrakiDerslerAnketAlmaz() {
        assertFalse(TutorialSurveyPolicy.isUnitOpener(part1, "p1_i02_2_basamakli_sayilar"))
        assertFalse(TutorialSurveyPolicy.isUnitOpener(part1, "p1_i07_kuralsiz_toplama_2_basamakli"))
        assertFalse(TutorialSurveyPolicy.isUnitOpener(part1, "p1_i13_5_lik_toplama_kuralsiz_topla"))
    }

    @Test
    fun baslikMaratonVeBilinmeyenKimlikAnketAlmaz() {
        assertFalse(TutorialSurveyPolicy.isUnitOpener(part1, "p1_i00_sayilari_abakuste_tanima"))
        assertFalse(TutorialSurveyPolicy.isUnitOpener(part1, "p1_i04_unite_maratonu"))
        assertFalse(TutorialSurveyPolicy.isUnitOpener(part1, "yok"))
    }

    @Test
    fun basliksizListedeIlkDersAnketAlir() {
        val list = listOf(TYPE_LESSON to "a", TYPE_LESSON to "b")
        assertTrue(TutorialSurveyPolicy.isUnitOpener(list, "a"))
        assertFalse(TutorialSurveyPolicy.isUnitOpener(list, "b"))
    }

    @Test
    fun ucuncuDenemedenItibarenSorulur() {
        assertFalse(TutorialSurveyPolicy.shouldAskStuck(null))
        assertFalse(TutorialSurveyPolicy.shouldAskStuck(1))
        assertFalse(TutorialSurveyPolicy.shouldAskStuck(2))
        assertTrue(TutorialSurveyPolicy.shouldAskStuck(3))
        assertTrue(TutorialSurveyPolicy.shouldAskStuck(7))
    }
}

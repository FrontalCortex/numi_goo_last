package com.sorobit.shared.baglanti

import com.sorobit.shared.ders.GlobalLessonData
import com.sorobit.shared.ders.LessonResultLogic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.sorobit.shared.ders.ChestResultLogic as DersChestResultLogic
import com.sorobit.shared.oyun.ChestResultLogic as OyunChestResultLogic

// Köprü yeni kural içermez; burada yalnızca doğru kurala bağlandığı ve varsayılanları doğru
// doldurduğu sınanır.
class SonucEkraniKoprusuTest {

    @Test
    fun basariVePuan() {
        assertEquals(0f, SonucEkraniKoprusu.basariOrani(3, 0))
        assertEquals(80f, SonucEkraniKoprusu.basariOrani(8, 10))
        assertEquals(400, SonucEkraniKoprusu.dersPuani(80f))
        assertEquals(LessonResultLogic.lessonScore(66.6f), SonucEkraniKoprusu.dersPuani(66.6f))
    }

    @Test
    fun basarisizlikAciklamasi() {
        assertEquals("Puan 500'den düşük!", SonucEkraniKoprusu.basarisizlikAciklamasi(true))
        // Android strings_ui.xml: fragment_lesson_result_false_result_title_comment (eşik %75)
        assertEquals("Başarı oranı %75'ten düşük!",SonucEkraniKoprusu.basarisizlikAciklamasi(false))
    }


    @Test
    fun sure() {
        assertEquals(125, SonucEkraniKoprusu.sureSaniye("2:05"))
        assertEquals(3725, SonucEkraniKoprusu.sureSaniye("1:02:05"))
        assertEquals(0, SonucEkraniKoprusu.sureSaniye("bozuk"))
        assertEquals("2:05", SonucEkraniKoprusu.sureMetni(125))
        assertEquals("0:00", SonucEkraniKoprusu.sureMetni(-4))
    }

    @Test
    fun cubuk() {
        assertEquals(0.5f, SonucEkraniKoprusu.cubukOrani(500, 1000))
        assertEquals(1f, SonucEkraniKoprusu.cubukOrani(5000, 1000))
        assertEquals(
            OyunChestResultLogic.scoreBarColorBlend(700, 600, 1000),
            SonucEkraniKoprusu.cubukRengi(700, 600, 1000),
        )
        val azalan = SonucEkraniKoprusu.cubukRengi(300, 600, 1000)
        assertEquals("#990000", azalan.toColor)
    }

    @Test
    fun yildizlar() {
        assertEquals(500, SonucEkraniKoprusu.yildizEsigi(1, 800, 1200))
        assertEquals(800, SonucEkraniKoprusu.yildizEsigi(2, 800, 1200))
        assertEquals(1200, SonucEkraniKoprusu.yildizEsigi(3, 800, 1200))
        assertEquals(1200, SonucEkraniKoprusu.yildizEsigi(9, 800, 1200))
        assertEquals(0.5f, SonucEkraniKoprusu.yildizOrani(600, 1200))
        assertEquals(0.9f, SonucEkraniKoprusu.yildizSesHizi(1))
        assertEquals(1.12f, SonucEkraniKoprusu.yildizSesHizi(3))
    }

    @Test
    fun tavanVeIkiYildizEsigi_gecerliPartIleCozulur() {
        val part = GlobalLessonData.globalPartId
        for (idx in listOf(0, 3, 9999)) {
            val tavan = SonucEkraniKoprusu.puanTavani(idx)
            assertEquals(DersChestResultLogic.resolveScoreCap(idx, part), tavan)
            assertEquals(
                DersChestResultLogic.resolveCupPoint2Threshold(idx, tavan, part),
                SonucEkraniKoprusu.ikiYildizEsigi(idx, tavan),
            )
            assertTrue(tavan >= 1)
        }
    }
}

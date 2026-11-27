package com.sorobit.shared.ders

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LessonEndMascotLogicTest {

    private val tumu: Set<String> = LessonEndMascotLogic.SUCCESS.toSet()

    @Test
    fun sabitler() {
        assertEquals(9, LessonEndMascotLogic.SUCCESS.size)
        assertEquals(9, tumu.size)
        assertEquals("DETERMINED", LessonEndMascotLogic.FAILURE)
        assertEquals(450L, LessonEndMascotLogic.ENTER_DELAY_MS)
    }

    @Test
    fun bosTorbadanDokuzCekimdeHepsiBirKezGelir() {
        val random = Random(7)
        var bag: String? = null
        var last: String? = null
        val cekilenler = mutableListOf<String>()
        repeat(9) {
            val d = LessonEndMascotLogic.nextSuccessWith(bag, last, random)
            cekilenler += d.emote
            bag = d.bag
            last = d.last
        }
        assertEquals(tumu, cekilenler.toSet())
        assertEquals(9, cekilenler.size)
        // Dokuzuncu çekimden sonra torba boş.
        assertEquals("", bag)
    }

    @Test
    fun torbaSinirindaUstUsteGelmez() {
        for (tohum in 0 until 300) {
            val random = Random(tohum)
            var bag: String? = null
            var last: String? = null
            var onceki: String? = null
            repeat(40) {
                val d = LessonEndMascotLogic.nextSuccessWith(bag, last, random)
                assertNotEquals(onceki, d.emote, "tohum=$tohum")
                assertEquals(d.emote, d.last)
                onceki = d.emote
                bag = d.bag
                last = d.last
            }
        }
    }

    @Test
    fun yeniTorbaninIlkiSonCekilenOlmaz() {
        for (tohum in 0 until 200) {
            val d = LessonEndMascotLogic.nextSuccessWith(null, "KARATE", Random(tohum))
            assertNotEquals("KARATE", d.emote, "tohum=$tohum")
        }
    }

    @Test
    fun sakliTorbaSirayiKorur() {
        val d = LessonEndMascotLogic.nextSuccessWith("NINJA,PIRATE,GUITAR", "RAPPER", Random(1))
        assertEquals("NINJA", d.emote)
        assertEquals("PIRATE,GUITAR", d.bag)
        assertEquals("NINJA", d.last)
    }

    @Test
    fun listedenCikmisEskiAdlarAtlanir() {
        val d = LessonEndMascotLogic.nextSuccessWith("ESKI_SAHNE,JUGGLE,YOK", null, Random(1))
        assertEquals("JUGGLE", d.emote)
        assertEquals("", d.bag)
        // Hepsi geçersizse torba yeniden karıştırılır.
        val yeni = LessonEndMascotLogic.nextSuccessWith("ESKI_SAHNE,YOK", null, Random(1))
        assertTrue(yeni.emote in tumu)
        assertEquals(8, yeni.bag.split(',').size)
    }

    @Test
    fun rastgeleKaynagiVarsayilanliSurum() {
        val d = LessonEndMascotLogic.nextSuccess(null, null)
        assertTrue(d.emote in tumu)
        assertEquals(8, d.bag.split(',').size)
    }
}

package com.example.app

import com.example.app.LessonMilestoneRules.ChestTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Haftalık kartın ders kilometre taşı kararları (LessonMilestones). */
class LessonMilestoneRulesTest {

    @Test
    fun islemMetni() {
        assertEquals("48 + 37 = 85", LessonMilestoneRules.binary(48, "+", 37)!!.text)
        assertEquals("60 - 25 = 35", LessonMilestoneRules.binary(60, "-", 25)!!.text)
        assertEquals("12 x 4 = 48", LessonMilestoneRules.binary(12, "x", 4)!!.text)
        assertEquals("60 - 25 + 7 = 42", LessonMilestoneRules.sequence(listOf(60, -25, 7))!!.text)
        assertNull(LessonMilestoneRules.binary(1, "/", 2))
        assertNull(LessonMilestoneRules.sequence(listOf(5)))
    }

    @Test
    fun enZoruSecilir_basamakOnce_sonraAdet_esitlikteSonuncu() {
        val small = LessonMilestoneRules.sequence(listOf(4, 3, 2, 1))!! // 1 basamak, 4 sayı
        val big = LessonMilestoneRules.sequence(listOf(48, 37))!!      // 2 basamak, 2 sayı
        assertEquals(big, LessonMilestoneRules.pickHarder(big, small))
        assertEquals(big, LessonMilestoneRules.pickHarder(small, big))
        val more = LessonMilestoneRules.sequence(listOf(11, 12, 13))!!
        assertEquals(more, LessonMilestoneRules.pickHarder(big, more))
        val sameA = LessonMilestoneRules.sequence(listOf(20, 30))!!
        val sameB = LessonMilestoneRules.sequence(listOf(30, 20))!!
        assertEquals(sameB, LessonMilestoneRules.pickHarder(sameA, sameB))
        assertEquals(small, LessonMilestoneRules.pickHarder(null, small))
    }

    @Test
    fun sandikSuresi_esikAlti_sayilmaz() {
        assertNull(LessonMilestoneRules.chestUpdate(null, 80, 79.9f, 1L))
        assertNull(LessonMilestoneRules.chestUpdate(null, 0, 100f, 1L))
        assertEquals(ChestTimes(80, 1L, 80, 1L), LessonMilestoneRules.chestUpdate(null, 80, 80f, 1L))
    }

    @Test
    fun sandikSuresi_ilkKalir_enIyiIyilesir() {
        val first = ChestTimes(80, 1L, 80, 1L)
        assertNull(LessonMilestoneRules.chestUpdate(first, 80, 100f, 2L)) // eşit: yazma yok
        assertNull(LessonMilestoneRules.chestUpdate(first, 95, 100f, 2L)) // yavaş: yazma yok
        assertEquals(ChestTimes(80, 1L, 52, 3L), LessonMilestoneRules.chestUpdate(first, 52, 90f, 3L))
        assertNull(LessonMilestoneRules.chestUpdate(first, 30, 70f, 4L)) // hızlı ama başarısı düşük
    }

    @Test
    fun sandikSuresi_haritaGidisDonus() {
        val t = ChestTimes(80, 1L, 52, 3L)
        assertEquals(t, ChestTimes.fromMap(t.toMap()))
        assertNull(ChestTimes.fromMap(null))
        assertNull(ChestTimes.fromMap(mapOf("firstSec" to 5)))
    }
}

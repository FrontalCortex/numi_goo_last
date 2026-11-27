package com.example.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Başarı anı paylaşımının kararları: hangi an bekler, günde bir panel. */
class ShareMomentsTest {

    private fun streak(d: Int) = ShareMoments.Moment(ShareMoments.Kind.STREAK, d, "", 1L)
    private fun badge(t: String) = ShareMoments.Moment(ShareMoments.Kind.BADGE, 3, t, 1L)

    @Test
    fun seriRozetiEzer() {
        assertTrue(ShareMoments.replaces(null, badge("Dino")))
        assertTrue(ShareMoments.replaces(badge("Dino"), streak(7)))
        assertFalse(ShareMoments.replaces(streak(7), badge("Dino")))
        assertTrue(ShareMoments.replaces(badge("Dino"), badge("Kapi")))
        assertTrue(ShareMoments.replaces(streak(7), streak(30)))
    }

    @Test
    fun gundeBirPanel() {
        assertTrue(ShareMoments.canShowToday(null, "2026-11-10"))
        assertFalse(ShareMoments.canShowToday("2026-11-10", "2026-11-10"))
        assertTrue(ShareMoments.canShowToday("2026-11-09", "2026-11-10"))
    }

    @Test
    fun anAnahtariVeJson() {
        // JSON gidiş-dönüşü org.json (Android) istediği için burada değil; cihazda paneli açan yol onu kullanıyor.
        assertEquals("STREAK::7", streak(7).key)
        assertEquals("BADGE:Şaho:3", badge("Şaho").key)
        assertEquals(setOf(7, 30, 100), ShareMoments.STREAK_DAYS)
    }
}

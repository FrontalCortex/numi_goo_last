package com.example.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Davet döngüsünün uygulama tarafındaki saf kararları (veli kapısı, referrer, paylaşım olayı). */
class ReferralShareTest {

    @Test
    fun veliKapisi() {
        val q = ParentGate.Question(7, 8)
        assertTrue(ParentGate.isCorrect(q, " 56 "))
        assertFalse(ParentGate.isCorrect(q, "50"))
        assertFalse(ParentGate.isCorrect(q, ""))
        // Sayılar yazıyla gösterilecek dizinin içinde kalmalı (0..19).
        repeat(200) {
            val n = ParentGate.newQuestion(Random(it))
            assertTrue(n.a in 3..9 && n.b in 3..9)
        }
    }

    @Test
    fun referrer() {
        assertEquals(ReferralAttribution.Parsed("K7M2QX", "weekly"), ReferralAttribution.parseParams("ref", "weekly", "k7m2qx"))
        assertEquals("weekly", ReferralAttribution.parseParams("ref", "bilinmeyen", "K7M2QX")!!.source)
        assertEquals("invite", ReferralAttribution.parseParams("ref", "invite", "K7M2QX")!!.source)
        assertNull(ReferralAttribution.parseParams("google-play", "organic", "K7M2QX"))
        assertNull(ReferralAttribution.parseParams("ref", "weekly", "K7M2Q0"))
        assertNull(ReferralAttribution.parseParams("ref", "weekly", null))
    }

    @Test
    fun paylasimOlayi() {
        assertEquals("share_done_whatsapp", WeeklyCardShare.eventFor("com.whatsapp"))
        assertEquals("share_done_whatsapp", WeeklyCardShare.eventFor("com.whatsapp.w4b"))
        assertEquals("share_done_instagram", WeeklyCardShare.eventFor("com.instagram.android"))
        assertEquals("share_done_other", WeeklyCardShare.eventFor("org.telegram.messenger"))
        assertEquals("share_done_other", WeeklyCardShare.eventFor(null))
    }
}

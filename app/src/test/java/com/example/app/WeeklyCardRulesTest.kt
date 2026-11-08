package com.example.app

import com.example.app.WeeklyCardRules.Item
import com.example.app.WeeklyCardRules.Milestone
import com.example.app.WeeklyCardRules.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyCardRulesTest {

    private val w43 = Snapshot("2026-W43", "2026-10-19", "2026-10-25")
    private val w44 = Snapshot("2026-W44", "2026-10-26", "2026-11-01")
    private val w45 = Snapshot("2026-W45", "2026-11-02", "2026-11-08")

    /** 26.10.2026 Pazartesi 12:00 TR. */
    private val midW44 = WeeklyCardRules.trDayStartMs("2026-10-28")!! + 12 * 3600_000L

    @Test
    fun pickWeeks_bitmemisHaftaElenir() {
        // Pazar 8 Kasım: 2–8 Kasım henüz kapanmadı → kart 26 Eki–1 Kas, önceki 19–25 Eki.
        val (target, prev) = WeeklyCardRules.pickWeeks(listOf(w43, w45, w44), "2026-11-08")!!
        assertEquals("2026-W44", target.week)
        assertEquals("2026-W43", prev!!.week)
        // Pazartesi 9 Kasım: 2–8 Kasım kapandı.
        assertEquals("2026-W45", WeeklyCardRules.pickWeeks(listOf(w43, w45, w44), "2026-11-09")!!.first.week)
        assertNull(WeeklyCardRules.pickWeeks(listOf(w45), "2026-11-08"))
    }

    @Test
    fun pickWeeks_cokEskiOncekiSayilmaz() {
        val old = Snapshot("2026-W38", "2026-09-14", "2026-09-20")
        val (_, prev) = WeeklyCardRules.pickWeeks(listOf(old, w44), "2026-11-08")!!
        assertNull(prev)
    }

    @Test
    fun kupa_kuralDegismedenIlerlemeSayilmaz() {
        // 200 ve 250 aynı aralık (1 basamaklı 4–6 sayı, 2,9 sn).
        val card = WeeklyCardRules.build(
            w44.copy(cups = mapOf("addition_abacus_cup" to 250)),
            w43.copy(cups = mapOf("addition_abacus_cup" to 200)),
            emptyList(),
        )
        assertTrue(card.cups.isEmpty())
        assertFalse(card.hasProgress)
    }

    @Test
    fun kupa_dususGosterilmez() {
        val card = WeeklyCardRules.build(
            w44.copy(cups = mapOf("addition_abacus_cup" to 300)),
            w43.copy(cups = mapOf("addition_abacus_cup" to 650)),
            emptyList(),
        )
        assertTrue(card.cups.isEmpty())
    }

    @Test
    fun kupaSeviyesi_kuralTablosundanOkunur() {
        val l = WeeklyCardRules.cupLevel("addition_abacus_cup", 250)
        assertEquals(WeeklyCardRules.CupLevel(1, 4, 6, 2900L), l)
        // 500–599: 1 ve 2 basamak; kartta en zoru (2 basamaklı 4–6 sayı).
        assertEquals(2, WeeklyCardRules.cupLevel("addition_abacus_cup", 550).digits)
        val mul = WeeklyCardRules.cupLevel("impact_abacus_cup", 250)
        assertEquals(2, mul.digits)
        assertEquals(1, mul.mulSecondDigits)
    }

    @Test
    fun anaSatir_kormeOnde() {
        val card = WeeklyCardRules.build(
            w44.copy(
                cups = mapOf("addition_abacus_cup" to 900, "blinding_addition_abacus_cup" to 420),
                sobiLevels = mapOf("add_five" to 7),
            ),
            w43.copy(
                cups = mapOf("addition_abacus_cup" to 300, "blinding_addition_abacus_cup" to 280),
                sobiLevels = mapOf("add_five" to 4),
            ),
            listOf(Milestone(1, "p1_i12", finishedAtMs = midW44, sampleOp = "4 + 5 = 9")),
        )
        val h = card.headline as Item.CupStep
        assertEquals("blinding_addition_abacus_cup", h.field)
        // Küçük satırlar: ana satır hariç, her türden en iyisi, en çok 3.
        assertEquals(3, card.extras.size)
        assertTrue(card.extras.none { it == h })
    }

    @Test
    fun sandik_esikVeHafta() {
        val ms = listOf(
            Milestone(1, "a", chestFirstSec = 80, chestBestSec = 52, chestBestAtMs = midW44), // geçer
            Milestone(1, "b", chestFirstSec = 80, chestBestSec = 78, chestBestAtMs = midW44), // 2 sn: eşik altı
            Milestone(1, "c", chestFirstSec = 200, chestBestSec = 185, chestBestAtMs = midW44), // %7,5: eşik altı
            Milestone(1, "d", chestFirstSec = 80, chestBestSec = 50, chestBestAtMs = midW44 - 7 * 86_400_000L), // geçen hafta
        )
        val card = WeeklyCardRules.build(w44, null, ms)
        assertEquals(listOf("a"), card.chests.map { it.stableId })
        assertTrue(card.headline is Item.ChestStep)
    }

    @Test
    fun sobi_ilkTestAtlamaSayilmaz() {
        val card = WeeklyCardRules.build(
            w44.copy(sobiLevels = mapOf("add_five" to 5, "add_ten" to 3)),
            w43.copy(sobiLevels = mapOf("add_five" to 5)),
            emptyList(),
        )
        assertTrue(card.sobi.isEmpty())
    }

    @Test
    fun emek_gunVeDakika() {
        val card = WeeklyCardRules.build(
            w44.copy(dailySeconds = mapOf("2026-10-26" to 600, "2026-10-27" to 1200), weekSeconds = 1800, streakCurrent = 12),
            null,
            emptyList(),
        )
        assertEquals(WeeklyCardRules.Effort(2, 30, 12), card.effort)
        assertFalse(card.hasProgress)
    }

    @Test
    fun oneri_yarimKalanOnce() {
        assertEquals("add_five", WeeklyCardRules.suggestNext(mapOf("add_direct" to 2, "add_five" to 1, "add_ten" to 0)))
        assertEquals("add_ten", WeeklyCardRules.suggestNext(mapOf("add_direct" to 2, "add_five" to 2)))
        assertNull(WeeklyCardRules.suggestNext(emptyMap()))
    }

    @Test
    fun bicim() {
        assertEquals("1:20", WeeklyCardRules.mmss(80))
        assertEquals("0:52", WeeklyCardRules.mmss(52))
        assertEquals("4–6", WeeklyCardRules.countRange(4, 6))
        assertEquals("5", WeeklyCardRules.countRange(5, 5))
    }
}

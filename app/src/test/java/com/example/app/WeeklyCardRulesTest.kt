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
    fun somut_yalnizcaHizDegistiyseAyniSayilar() {
        // 500–599 ve 600–699: ikisi de 2 basamaklı 4–6 sayı; aralık 4,2 → 4,05 sn.
        val step = Item.CupStep("addition_abacus_cup", 520, 640,
            WeeklyCardRules.cupLevel("addition_abacus_cup", 520), WeeklyCardRules.cupLevel("addition_abacus_cup", 640))
        val c = WeeklyCardRules.concrete(step, "2026-W44/addition")!!
        assertEquals(WeeklyCardRules.Change.FASTER, c.change)
        assertTrue(c.sameNumbers)
        assertEquals(4, c.afterText.split(" + ").size)
        assertTrue(c.afterText.split(" + ").all { it.length == 2 })
    }

    @Test
    fun somut_adetArttiysaOncekininDevami() {
        // Körleme 280 → 420: 1 basamaklı 4 sayı → 5 sayı.
        val f = "blinding_addition_abacus_cup"
        val c = WeeklyCardRules.concrete(Item.CupStep(f, 280, 420, WeeklyCardRules.cupLevel(f, 280), WeeklyCardRules.cupLevel(f, 420)), "k")!!
        assertEquals(WeeklyCardRules.Change.COUNT, c.change)
        assertTrue(c.mental)
        assertTrue(c.afterText.startsWith(c.beforeText + " + "))
    }

    @Test
    fun somut_cikarmaEksiyeDusmez() {
        for (seed in 0L until 200L) {
            for (lvl in listOf(WeeklyCardRules.Level(1, 12, 2000), WeeklyCardRules.Level(2, 5, 3000), WeeklyCardRules.Level(3, 4, 4000))) {
                val nums = WeeklyCardRules.exampleNumbers(WeeklyCardRules.Op.SUB, lvl, seed)
                assertEquals(lvl.count, nums.size)
                var run = 0
                nums.forEach { run += it; assertTrue("ara sonuç eksi: $nums", run >= 0) }
            }
        }
    }

    @Test
    fun somut_sobiBasamakAtlamasi() {
        // Seviye 2 (1 basamaklı 3 sayı) → 3 (2 basamaklı 2 sayı): basamak öncelikli.
        val c = WeeklyCardRules.concrete(Item.SobiStep("m_sub_five", 2, 3), "s")!!
        assertEquals(WeeklyCardRules.Change.DIGITS, c.change)
        assertEquals(WeeklyCardRules.Op.SUB, c.op)
        assertTrue(c.mental)
    }

    @Test
    fun merdiven() {
        val f = "addition_abacus_cup"
        // 280 → 420: basamak 2 → 4; bir geriden başla, iki ileride bitir.
        val l = WeeklyCardRules.ladderFor(Item.CupStep(f, 280, 420, WeeklyCardRules.cupLevel(f, 280), WeeklyCardRules.cupLevel(f, 420)))!!
        assertEquals(WeeklyCardRules.Ladder(6, 1, 3), l)
        // Büyük sıçramada merdiven sıkışır: arada kırık, bu hafta sondan 3. basamakta.
        val big = WeeklyCardRules.ladderFor(Item.CupStep(f, 600, 1900, WeeklyCardRules.cupLevel(f, 600), WeeklyCardRules.cupLevel(f, 1900)))!!
        assertEquals(WeeklyCardRules.Ladder(9, 1, 6, gap = true), big)
        // Sınırda (5 basamak) henüz sıkışmaz.
        assertFalse(WeeklyCardRules.ladderFor(Item.CupStep(f, 300, 800, WeeklyCardRules.cupLevel(f, 300), WeeklyCardRules.cupLevel(f, 800)))!!.gap)
        assertEquals(WeeklyCardRules.Ladder(20, 3, 6), WeeklyCardRules.ladderFor(Item.SobiStep("add_five", 4, 7)))
    }

    @Test
    fun ornekDizisiKisalir() {
        val op = WeeklyCardRules.Op.ADD
        assertEquals("354 + 586 + 373 + 714 + …", WeeklyCardRules.exampleText(op, listOf(354, 586, 373, 714, 319, 624, 136)))
        assertEquals("1 + 2 + 3 + 4 + 5", WeeklyCardRules.exampleText(op, listOf(1, 2, 3, 4, 5)))
        assertEquals("90 − 12 + 30 − 4 + …", WeeklyCardRules.exampleText(WeeklyCardRules.Op.SUB, listOf(90, -12, 30, -4, -5, -6)))
    }

    @Test
    fun bicim() {
        assertEquals("1:20", WeeklyCardRules.mmss(80))
        assertEquals("0:52", WeeklyCardRules.mmss(52))
        assertEquals("4–6", WeeklyCardRules.countRange(4, 6))
        assertEquals("5", WeeklyCardRules.countRange(5, 5))
    }
}

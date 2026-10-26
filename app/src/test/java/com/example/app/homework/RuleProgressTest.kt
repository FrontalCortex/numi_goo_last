package com.example.app.homework

import com.example.app.homework.RuleProgress.Cw
import com.example.app.homework.RuleProgress.Day
import com.example.app.homework.RuleProgress.Range
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Ödev başarısı grafiğinin hesabı ([RuleProgress]). */
class RuleProgressTest {

    private fun day(c: Int, w: Int, vararg t: Pair<String, Cw>) = Day(Cw(c, w), t.toMap())

    @Test
    fun calendarRoundTrip() {
        assertEquals(0, RuleProgress.epochDay(1970, 1, 1))
        assertEquals(Triple(2026, 10, 26), RuleProgress.civil(RuleProgress.epochDay(2026, 10, 26)))
        assertEquals(Triple(2028, 2, 29), RuleProgress.civil(RuleProgress.epochDay(2028, 2, 29)))
        for (d in 18000..22000) {
            val (y, m, dd) = RuleProgress.civil(d)
            assertEquals(d, RuleProgress.epochDay(y, m, dd))
        }
        // Türkiye günü: UTC 21:30 hâlâ aynı gün, 21:00'den sonra ertesi gün.
        val utc2030 = RuleProgress.epochDay(2026, 10, 31) * 86_400_000L + 20 * 3_600_000L + 30 * 60_000L
        assertEquals(RuleProgress.epochDay(2026, 10, 31), RuleProgress.todayTr(utc2030))
        assertEquals(RuleProgress.epochDay(2026, 11, 1), RuleProgress.todayTr(utc2030 + 2 * 3_600_000L))
    }

    @Test
    fun bucketsAndCumulative() {
        val today = RuleProgress.epochDay(2026, 10, 26)
        val days = mapOf(
            today - 40 to day(5, 5),          // 3 ay ve Tümü'de görünür, 1 ayda yalnızca toplamda
            today - 3 to day(8, 2, "add_five" to Cw(20, 4)),
            today to day(9, 1),
        )
        val week = RuleProgress.buckets(days, null, Range.WEEK, today, startTotal = 100)
        assertEquals(listOf(today - 3, today), week.map { it.from })
        // Toplam: başlangıç 100 + aralıktan önceki 10 + 10 + 10.
        assertEquals(listOf(120, 130), week.map { it.cumulative })
        assertEquals(listOf(80, 90), week.map { it.cw.pct })

        val quarter = RuleProgress.buckets(days, null, Range.QUARTER, today, 0)
        assertEquals(RuleProgress.BucketKind.WEEK, quarter.first().kind)
        assertEquals(30, quarter.last().cumulative)
        assertEquals(today, quarter.last().to)

        val all = RuleProgress.buckets(days, null, Range.ALL, today, 0)
        assertEquals(listOf(Triple(2026, 9, 1), Triple(2026, 10, 1)), all.map { RuleProgress.civil(it.from) })
        assertEquals(listOf(10, 30), all.map { it.cumulative })

        val rule = RuleProgress.buckets(days, "add_five", Range.MONTH, today, 0)
        assertEquals(listOf(Cw(20, 4)), rule.map { it.cw })
    }

    @Test
    fun startPoint() {
        val today = RuleProgress.epochDay(2026, 10, 26)
        val days = mapOf(today to day(9, 1, "add_ten" to Cw(10, 2)))
        // Kurallarda başlangıç = toplam − günlük kayıtlar.
        assertEquals(Cw(50, 3), RuleProgress.start("add_ten", null, mapOf("add_ten" to Cw(60, 5)), days))
        // Hepsi günlük kayıtta: başlangıç yok.
        assertNull(RuleProgress.start("add_ten", null, mapOf("add_ten" to Cw(10, 2)), days))
        assertEquals(Cw(30, 6), RuleProgress.start(null, Cw(30, 6), emptyMap(), days))
        assertNull(RuleProgress.start(null, null, emptyMap(), days))
    }

    @Test
    fun masteryStage() {
        assertEquals(0, RuleProgress.stage(Cw(20, 0)))   // 50'den az: başladı
        assertEquals(2, RuleProgress.stage(Cw(45, 5)))   // %90
        assertEquals(1, RuleProgress.stage(Cw(40, 10)))  // %80
        assertEquals(0, RuleProgress.stage(Cw(30, 20)))  // %60
    }
}

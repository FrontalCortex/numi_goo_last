// Seri kurallarının (StreakDays, StreakFreezeRules, StreakMilestones, StudyTimeTracker) Android
// davranışıyla aynı sonucu verdiğini doğrular. Beklenen değerler Android kaynağı okunarak türetildi.
package com.sorobit.shared.seri

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StreakDaysTest {
    @Test
    fun gunKimligiVeKayma() {
        val saat = SahteSeriSaati(LocalDateTime(2026, 11, 25, 10, 0))
        assertEquals("2026-11-25", StreakDays.dayId(saat, 0))
        assertEquals("2026-11-24", StreakDays.dayId(saat, -1))
        assertEquals("2026-11-26", StreakDays.dayId(saat, 1))
        assertEquals("2026-12-01", StreakDays.dayId(saat, 6))
        assertEquals(180, StreakDays.utcOffsetMinutes(saat))
    }

    @Test
    fun gunSiniriYerelGeceYarisi() {
        // 23:59 (+03:00) hâlâ aynı gün; UTC'de 20:59.
        val saat = SahteSeriSaati(LocalDateTime(2026, 11, 25, 23, 59))
        assertEquals("2026-11-25", StreakDays.dayId(saat, 0))
        saat.ilerlet(2 * 60_000L)
        assertEquals("2026-11-26", StreakDays.dayId(saat, 0))
    }

    @Test
    fun haftaPazartesidenBaslar() {
        // 2026-11-25 Çarşamba → hafta 23 (Pzt) … 29 (Paz).
        val saat = SahteSeriSaati(LocalDateTime(2026, 11, 25, 10, 0))
        assertEquals(
            listOf("2026-11-23", "2026-11-24", "2026-11-25", "2026-11-26", "2026-11-27", "2026-11-28", "2026-11-29"),
            StreakDays.weekDayIds(saat),
        )
        // Pazar günü: aynı haftanın son günü.
        val pazar = SahteSeriSaati(LocalDateTime(2026, 11, 29, 8, 0))
        assertEquals("2026-11-23", StreakDays.weekDayIds(pazar).first())
        // Pazartesi: kendisi.
        val pazartesi = SahteSeriSaati(LocalDateTime(2026, 11, 30, 8, 0))
        assertEquals("2026-11-30", StreakDays.weekDayIds(pazartesi).first())
    }
}

class StreakFreezeRulesTest {
    // Bugün = 2026-11-25.
    private val gunler = mapOf(
        0 to "2026-11-25", -1 to "2026-11-24", -2 to "2026-11-23", -3 to "2026-11-22", -4 to "2026-11-21",
    )
    private fun gun(k: Int): String = gunler[k] ?: "2026-10-01"

    @Test
    fun dunKactiysaKopruKurulur() {
        // Son gün evvelsi gün: dün kaçtı → dün kapatılır.
        val s = StreakFreezeRules.settle(5, "2026-11-23", 1, "", 30, ::gun)
        assertEquals("2026-11-24", s?.missedDay)
    }

    @Test
    fun ikiGunKactiysaIlkKacanGunKapatilir() {
        val s = StreakFreezeRules.settle(5, "2026-11-22", 1, "", 30, ::gun)
        assertEquals("2026-11-23", s?.missedDay)
    }

    @Test
    fun harcanmayanDurumlar() {
        assertNull(StreakFreezeRules.settle(5, "2026-11-23", 0, "", 30, ::gun)) // dondurma yok
        assertNull(StreakFreezeRules.settle(0, "2026-11-23", 1, "", 30, ::gun)) // seri yok
        assertNull(StreakFreezeRules.settle(5, "", 1, "", 30, ::gun)) // son gün yok
        assertNull(StreakFreezeRules.settle(5, "2026-11-24", 1, "", 30, ::gun)) // dün tutturuldu
        assertNull(StreakFreezeRules.settle(5, "2026-11-25", 1, "", 30, ::gun)) // bugün tutturuldu
        // Satın alındığı günden öncesini kurtarmaz.
        assertNull(StreakFreezeRules.settle(5, "2026-11-22", 1, "2026-11-25", 30, ::gun))
        // Tarama penceresinin dışında.
        assertNull(StreakFreezeRules.settle(5, "2026-01-01", 1, "", 4, ::gun))
    }
}

class StreakMilestonesTest {
    @Test
    fun odulTablosu() {
        assertEquals(StreakMilestones.Reward(3, 0), StreakMilestones.rewardFor(3))
        assertEquals(StreakMilestones.Reward(0, 2000), StreakMilestones.rewardFor(7))
        assertEquals(StreakMilestones.Reward(5, 0), StreakMilestones.rewardFor(14))
        assertEquals(StreakMilestones.Reward(0, 5000), StreakMilestones.rewardFor(30))
        assertEquals(StreakMilestones.Reward(10, 0), StreakMilestones.rewardFor(60))
        assertEquals(StreakMilestones.Reward(0, 5000), StreakMilestones.rewardFor(90))
        assertNull(StreakMilestones.rewardFor(5))
        assertNull(StreakMilestones.rewardFor(0))
        assertNull(StreakMilestones.rewardFor(3630))
        assertEquals(500, StreakMilestones.challengeReward(3))
        assertEquals(1000, StreakMilestones.challengeReward(5))
        assertEquals(1500, StreakMilestones.challengeReward(7))
        assertEquals(0, StreakMilestones.challengeReward(4))
    }

    @Test
    fun siradakiVeGorunenler() {
        assertEquals(3, StreakMilestones.next(0))
        assertEquals(7, StreakMilestones.next(3))
        assertEquals(30, StreakMilestones.next(14))
        assertEquals(60, StreakMilestones.next(30))
        assertNull(StreakMilestones.next(3600))
        assertEquals(listOf(3, 7, 14), StreakMilestones.visible(0, emptySet(), 3))
        // 8 gün, 3 toplanmış: 7 toplanabilir + sıradaki iki.
        assertEquals(listOf(7, 14, 30), StreakMilestones.visible(8, setOf(3), 3))
        // Toplanmamışlar sayıyı aşabilir.
        assertEquals(listOf(3, 7, 14, 30), StreakMilestones.visible(30, emptySet(), 3))
        assertEquals(listOf(7), StreakMilestones.claimable(8, setOf(3)))
    }
}

class StudyTimeTrackerTest {
    @Test
    fun yalnizcaDersEkraniSayilir() {
        val d = SeriDuzenek()
        d.izleyici.setActiveScreen("ShopFragment")
        d.saat.ilerlet(120_000L)
        d.izleyici.setActiveScreen(null)
        assertEquals(0, d.izleyici.secondsToday())
        d.calis(3)
        assertEquals(180, d.izleyici.secondsToday())
    }

    @Test
    fun acikParcaDaSayilir() {
        val d = SeriDuzenek()
        d.izleyici.setActiveScreen("BlindingLessonFragment")
        d.saat.ilerlet(90_000L)
        assertTrue(d.izleyici.isCounting())
        assertEquals(90, d.izleyici.secondsToday())
    }

    @Test
    fun bostaKalmaSiniri() {
        val d = SeriDuzenek()
        d.izleyici.setActiveScreen("TutorialFragment")
        // Hiç dokunmadan 20 dakika: yalnızca 5 dakika yazılır.
        d.saat.ilerlet(20 * 60_000L)
        d.izleyici.setActiveScreen(null)
        assertEquals(300, d.izleyici.secondsToday())
        assertFalse(d.izleyici.isCounting())
    }

    @Test
    fun ertesiGunSifirdanVeEskilerSilinir() {
        val d = SeriDuzenek()
        d.calis(2)
        d.saat.gunAtla()
        assertEquals(0, d.izleyici.secondsToday())
        d.saat.gunAtla(10)
        d.calis(1)
        // 8 günden eski kova silindi.
        assertFalse(d.sureDeposu.contains("seconds_2026-11-25"))
        assertEquals(60, d.izleyici.secondsToday())
    }
}

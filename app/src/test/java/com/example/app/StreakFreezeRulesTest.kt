package com.example.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Seri dondurma kuralı.
 *
 * Senaryolar sunucudaki ikizin testiyle aynı (`functions/scripts/test-streak-freeze.js`):
 * iki taraf aynı girdiyle aynı sonucu vermek zorunda. Birine senaryo eklenirse öbürüne de
 * eklenmeli.
 *
 * Çalıştırma: `.\gradlew testDebugUnitTest --tests "*StreakFreezeRulesTest"`
 */
class StreakFreezeRulesTest {

    private companion object {
        const val SAT = "2026-10-03"
        const val SUN = "2026-10-04"
        const val MON = "2026-10-05"
        const val TUE = "2026-10-06"
        const val WED = "2026-10-07"
        const val SCAN = 30
    }

    /** [today]'i bugün sayan gün kimliği üreticisi; StudyTimeTracker.dayId'nin yerine geçiyor. */
    private fun clock(today: String): (Int) -> String = { offset ->
        LocalDate.parse(today).plusDays(offset.toLong()).toString()
    }

    /** 5 günlük seri pazar günü tutturulmuş, elde cumartesi alınmış bir dondurma var. */
    private fun settle(
        today: String,
        current: Int = 5,
        lastDay: String = SUN,
        freezes: Int = 1,
        freezeDay: String = SAT,
    ) = StreakFreezeRules.settle(current, lastDay, freezes, freezeDay, SCAN, clock(today))

    // ── Harcanmayan durumlar ────────────────────────────────────────────

    @Test
    fun `dun tutturulmussa kacan gun yok`() {
        assertNull(settle(today = MON))
    }

    @Test
    fun `bugun tutturulmussa kacan gun yok`() {
        assertNull(settle(today = SUN))
    }

    @Test
    fun `dondurma yoksa hicbir sey olmaz`() {
        assertNull(settle(today = TUE, freezes = 0, freezeDay = ""))
    }

    @Test
    fun `seri yoksa korunacak bir sey yok`() {
        assertNull(settle(today = TUE, current = 0))
    }

    @Test
    fun `hic gun tutturulmamissa hicbir sey olmaz`() {
        assertNull(settle(today = TUE, lastDay = ""))
    }

    @Test
    fun `son gun ilerideyse dokunulmaz`() {
        // Saat dilimi payı: sunucudan geri yüklenen son gün bu cihazın bugününden ileride olabilir.
        assertNull(settle(today = TUE, lastDay = WED))
    }

    // ── Harcanan durumlar ───────────────────────────────────────────────

    @Test
    fun `tek gun kactiysa o gun kapatilir`() {
        // Pazartesi kaçtı, salı açıldı: pazartesi köprü olur, seri yaşar.
        assertEquals(StreakFreezeRules.Settled(MON), settle(today = TUE))
    }

    @Test
    fun `iki gun kactiysa dondurma yine harcanir`() {
        // Ürün kararı. Yalnızca İLK kaçan gün kapanıyor; son gün hâlâ dünden eski olduğu
        // için seriyi StreakRepository.refresh kırıyor.
        assertEquals(StreakFreezeRules.Settled(MON), settle(today = WED))
    }

    // ── Geçmişi onarmaz ─────────────────────────────────────────────────

    @Test
    fun `satin almadan once kacan gun kapatilmaz`() {
        // Pazartesi kaçtı (dondurma yoktu), salı satın alındı.
        assertNull(settle(today = TUE, freezeDay = TUE))
    }

    @Test
    fun `satin alindigi gun kacirilirsa kapatilir`() {
        // Pazartesi sabah alındı, pazartesi kaçırıldı.
        assertEquals(StreakFreezeRules.Settled(MON), settle(today = TUE, freezeDay = MON))
    }

    @Test
    fun `satin alma gunu bilinmiyorsa kisit yok`() {
        assertEquals(StreakFreezeRules.Settled(MON), settle(today = TUE, freezeDay = ""))
    }

    // ── Tarama sınırı ───────────────────────────────────────────────────

    @Test
    fun `tarama sinirindaki son gun bulunur`() {
        val today = "2026-11-03" // pazardan 30 gün sonra
        assertEquals(StreakFreezeRules.Settled(MON), settle(today = today))
    }

    @Test
    fun `tarama sinirindan eski seri burada kapatilmaz`() {
        // O seri çoktan kırık; dondurmanın akıbetini sunucu söylüyor.
        assertNull(settle(today = "2026-11-04"))
    }

    @Test
    fun `ay sonu dogru ilerler`() {
        assertEquals(
            StreakFreezeRules.Settled("2026-11-01"),
            settle(today = "2026-11-02", lastDay = "2026-10-31", freezeDay = "2026-10-30"),
        )
    }
}

// StreakRepository'nin Android (StreakRepository.kt) davranışıyla aynı sonucu verdiğini doğrular.
package com.sorobit.shared.seri

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StreakRepositoryTest {

    @Test
    fun hesapYokkenGunSayilmaz() {
        val d = SeriDuzenek()
        d.calis(6)
        val s = d.repo.refresh()
        assertEquals(0, s.current)
        assertTrue(s.goalReachedToday)
        assertEquals(emptyList(), d.repo.pendingSyncDays())
        // Hesap bağlanınca aynı süre DOĞRU hedefle sayılır (sahipsiz veri devralınır).
        d.repo.bindToUser("uid-1")
        assertEquals(1, d.repo.refresh().current)
    }

    @Test
    fun hedefTutturulunceGunIlerlerVeKutlamaKuyrugaGirer() {
        val d = SeriDuzenek()
        d.repo.bindToUser("uid-1")
        d.calis(4)
        assertEquals(0, d.repo.refresh().current)
        assertEquals(0, d.repo.pendingCelebration())
        d.calis(1)
        val s = d.repo.refresh()
        assertEquals(1, s.current)
        assertEquals(1, s.longest)
        assertEquals(1, d.repo.pendingCelebration())
        assertEquals(listOf(d.gun(0)), d.repo.pendingSyncDays())
        assertTrue(d.gun(0) in s.achievedDays)
        // Aynı gün ikinci kez sayılmaz.
        d.calis(10)
        assertEquals(1, d.repo.refresh().current)
        d.repo.clearPendingCelebration()
        assertEquals(0, d.repo.pendingCelebration())
    }

    @Test
    fun ardisikGunlerBuyurAtlananGunKirar() {
        val d = SeriDuzenek()
        d.repo.bindToUser("uid-1")
        d.calis(5); d.repo.refresh()
        d.saat.gunAtla(); d.calis(5)
        assertEquals(2, d.repo.refresh().current)
        d.saat.gunAtla(); d.calis(5)
        assertEquals(3, d.repo.refresh().current)
        // Bir gün atlandı: okunurken kırılır, rekor kalır.
        d.saat.gunAtla(2)
        val kirik = d.repo.refresh()
        assertEquals(0, kirik.current)
        assertEquals(3, kirik.longest)
        assertTrue(d.repo.needsNewStreakPrompt())
        d.repo.markNewStreakPromptShown()
        assertFalse(d.repo.needsNewStreakPrompt())
        // Yeni gün: yeni seri 1'den.
        d.calis(5)
        assertEquals(1, d.repo.refresh().current)
    }

    @Test
    fun hedefDegisirseYeniHedefleSayilir() {
        val d = SeriDuzenek()
        d.repo.bindToUser("uid-1")
        d.repo.setGoalMinutes(10)
        d.calis(6)
        val s = d.repo.refresh()
        assertEquals(0, s.current)
        assertEquals(4, s.minutesLeft)
        assertEquals(0.6f, s.todayFraction)
        // Geçersiz hedef varsayılana düşer.
        d.repo.setGoalMinutes(7)
        assertEquals(5, d.repo.goalMinutes())
    }

    @Test
    fun dondurmaDunuKurtarir() {
        val d = SeriDuzenek()
        d.repo.bindToUser("uid-1")
        d.calis(5); d.repo.refresh()
        d.saat.gunAtla(); d.calis(5); d.repo.refresh()
        d.repo.markFreezePurchased(1, d.gun(0))
        // Ertesi gün çalışılmadı, ondan sonraki gün açıldı.
        d.saat.gunAtla(2)
        val s = d.repo.refresh()
        assertEquals(2, s.current)
        assertFalse(s.freezeHeld)
        assertTrue(d.gun(-1) in s.frozenDays)
        assertFalse(d.gun(-1) in s.achievedDays)
        assertEquals(StreakFreezeNotice.SAVED, d.repo.takeFreezeNotice())
        assertNull(d.repo.takeFreezeNotice())
        // Bugün çalışınca seri 3 olur (kapatılan gün seriye eklenmez).
        d.calis(5)
        assertEquals(3, d.repo.refresh().current)
    }

    @Test
    fun ikiGunKactiysaDondurmaHarcanirSeriKirilir() {
        val d = SeriDuzenek()
        d.repo.bindToUser("uid-1")
        d.calis(5); d.repo.refresh()
        d.repo.markFreezePurchased(1, d.gun(0))
        d.saat.gunAtla(3)
        val s = d.repo.refresh()
        assertEquals(0, s.current)
        assertEquals(0, d.repo.freezesHeld())
        assertEquals(StreakFreezeNotice.LOST, d.repo.takeFreezeNotice())
    }

    @Test
    fun bayatDondurmaBildirimiGosterilmez() {
        val d = SeriDuzenek()
        d.repo.bindToUser("uid-1")
        d.calis(5); d.repo.refresh()
        d.repo.markFreezePurchased(1, d.gun(0))
        d.saat.gunAtla(2)
        d.repo.refresh()
        d.saat.gunAtla()
        assertNull(d.repo.takeFreezeNotice())
    }

    @Test
    fun baskaHesapVeriyiSiler() {
        val d = SeriDuzenek()
        d.repo.bindToUser("uid-1")
        d.calis(5); d.repo.refresh()
        d.repo.bindToUser("uid-1")
        assertEquals(1, d.repo.refresh().current)
        d.repo.bindToUser("uid-2")
        val s = d.repo.refresh()
        assertEquals(0, s.current)
        assertEquals(0, s.secondsToday)
        assertTrue(d.repo.isAccountBound())
    }

    @Test
    fun ileriTarihliSonGunAtilir() {
        val d = SeriDuzenek()
        d.repo.seedForPreview("uid-1", 4, 4, 5, 5, 0, 0, 0, 0, emptyList())
        d.calis(5)
        val s = d.repo.refresh()
        assertEquals(1, s.current)
        // Yarın (saat dilimi payı) ise ne ilerler ne kırılır.
        val d2 = SeriDuzenek()
        d2.repo.seedForPreview("uid-1", 4, 4, 1, 5, 0, 0, 0, 0, emptyList())
        d2.calis(5)
        assertEquals(4, d2.repo.refresh().current)
    }

    @Test
    fun meydanOkumaSecimi() {
        val d = SeriDuzenek()
        assertEquals(0, d.repo.chosenChallengeDays())
        d.repo.setChallengeDays(5)
        assertEquals(5, d.repo.chosenChallengeDays())
        d.repo.setChallengeDays(4)
        assertEquals(3, d.repo.chosenChallengeDays())
        d.repo.markChallengeClaimed(3)
        assertEquals(3, d.repo.challengeClaimed())
        assertEquals(d.gun(0), d.repo.challengeClaimedDay())
        d.repo.startNewChallenge(7)
        assertEquals(7, d.repo.chosenChallengeDays())
        assertEquals(0, d.repo.challengeClaimed())
        assertEquals("", d.repo.challengeClaimedDay())
    }

    @Test
    fun hatirlatmaSaati() {
        val d = SeriDuzenek()
        assertEquals(19, d.repo.reminderHour())
        d.repo.adoptServerReminderHour(16)
        assertEquals(16, d.repo.reminderHour())
        d.repo.setReminderHour(30)
        assertEquals(23, d.repo.reminderHour())
        assertTrue(d.repo.reminderHourPending())
        // Bekleyen seçim varken sunucunun değeri alınmaz.
        d.repo.adoptServerReminderHour(18)
        assertEquals(23, d.repo.reminderHour())
        d.repo.onReminderHourSynced(22)
        assertTrue(d.repo.reminderHourPending())
        d.repo.onReminderHourSynced(23)
        assertFalse(d.repo.reminderHourPending())
    }

    private fun bekleyen(d: SeriDuzenek, oncekiMs: Long) =
        StreakPendingSignup(10, 7, 20, d.saat.nowMs() - oncekiMs, null, 0L)

    @Test
    fun kayitCevaplariYeniHesabaUygulanir() {
        val d = SeriDuzenek()
        d.repo.bindToUser("uid-1")
        val sonuc = d.repo.applyPendingSignup(bekleyen(d, 60_000L), "uid-1", d.saat.nowMs() - 30_000L)
        assertEquals(StreakSignupResult.APPLIED, sonuc)
        assertEquals(10, d.repo.goalMinutes())
        assertEquals(7, d.repo.chosenChallengeDays())
        assertEquals(20, d.repo.reminderHour())
        assertTrue(d.repo.reminderHourPending())
        assertTrue(d.repo.isOnboardingDone())
        assertFalse(d.repo.needsNewStreakPrompt())
    }

    @Test
    fun kayitCevaplariMevcutHesaptaAtilir() {
        val d = SeriDuzenek()
        val gun = 24L * 60 * 60 * 1000
        assertEquals(
            StreakSignupResult.DISCARDED_EXISTING_ACCOUNT,
            d.repo.applyPendingSignup(bekleyen(d, 60_000L), "uid-1", d.saat.nowMs() - 3 * gun),
        )
        assertEquals(
            StreakSignupResult.DISCARDED_EXISTING_ACCOUNT,
            d.repo.applyPendingSignup(bekleyen(d, 60_000L), "uid-1", null),
        )
        assertEquals(
            StreakSignupResult.DISCARDED_STALE,
            d.repo.applyPendingSignup(bekleyen(d, 2 * gun), "uid-1", d.saat.nowMs()),
        )
        assertEquals(StreakSignupResult.NO_PENDING, d.repo.applyPendingSignup(null, "uid-1", null))
        assertEquals(5, d.repo.goalMinutes())
        // Veli onayı işareti bu hesabınsa Auth zamanı yerine o kullanılır.
        val isaretli = StreakPendingSignup(20, 3, 18, d.saat.nowMs() - 1000L, "uid-1", d.saat.nowMs() - 1000L)
        assertEquals(StreakSignupResult.APPLIED, d.repo.applyPendingSignup(isaretli, "uid-1", d.saat.nowMs() - 3 * gun))
        assertEquals(20, d.repo.goalMinutes())
    }

    @Test
    fun sunucuDurumuBenimsenir() {
        // Yeni cihaz: sunucuda dünle biten 30 günlük seri.
        val d = SeriDuzenek()
        d.repo.bindToUser("uid-1")
        d.repo.adoptServerState(
            current = 30, longest = 31, lastDay = d.gun(-1), claimed = setOf(3, 7),
            recentDays = setOf(d.gun(-1), d.gun(-2)), goalMinutes = 10, challengeDays = 5,
            challengeClaimed = 5, freezes = 0, freezeDay = "", frozenDays = emptySet(),
        )
        val s = d.repo.refresh()
        assertEquals(30, s.current)
        assertEquals(31, s.longest)
        assertEquals(10, d.repo.goalMinutes())
        assertEquals(5, d.repo.chosenChallengeDays())
        assertEquals(5, d.repo.challengeClaimed())
        assertEquals(30, d.repo.serverCurrent())
        assertEquals(setOf(3, 7), d.repo.claimedMilestones())
        // Bayat sunucu durumu yerel sayaca alınmaz.
        val d2 = SeriDuzenek()
        d2.repo.bindToUser("uid-1")
        d2.repo.adoptServerState(9, 9, d2.gun(-5), emptySet(), emptySet(), 0, 0, 0, StreakRules.FREEZES_UNKNOWN, "", emptySet())
        assertEquals(0, d2.repo.refresh().current)
        assertEquals(9, d2.repo.serverCurrent())
    }
}

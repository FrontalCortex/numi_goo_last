// Eşitleme (StreakSync), sunucu modeli (functions/index.js kopyası) ve ekran kararları (StreakScreenLogic).
package com.sorobit.shared.seri

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StreakSyncTest {

    /** Yerel gün → sahte sunucu → ödül: uçtan uca. */
    @Test
    fun gunBildirilirOdulToplanir() {
        val d = SeriDuzenek()
        val sunucu = StreakServerModel(d.saat)
        d.repo.bindToUser("uid-1")
        d.repo.setChallengeDays(3)

        // İlk açılış: gün yok ama "buradayım" gidiyor.
        val ping = StreakSync.buildSubmitRequest(d.repo, d.saat)
        assertNotNull(ping)
        assertEquals(emptyList(), ping.days)
        assertEquals(180, ping.utcOffsetMinutes)
        assertNull(sunucu.submit(ping))
        StreakSync.onSubmitSucceeded(d.repo, ping, sunucu.snapshot())
        assertNull(StreakSync.buildSubmitRequest(d.repo, d.saat))

        for (gun in 0 until 3) {
            if (gun > 0) d.saat.gunAtla()
            d.calis(5)
            d.repo.refresh()
            val istek = StreakSync.buildSubmitRequest(d.repo, d.saat)
            assertNotNull(istek)
            assertEquals(listOf(d.gun(0)), istek.days)
            assertEquals(3, istek.challengeDays)
            assertNull(sunucu.submit(istek))
            StreakSync.onSubmitSucceeded(d.repo, istek, sunucu.snapshot())
            assertEquals(emptyList(), d.repo.pendingSyncDays())
        }
        assertEquals(3, sunucu.current)
        assertEquals(3, d.repo.serverCurrent())
        assertEquals(3, sunucu.challengeDays)

        val ekran = StreakScreenLogic.screen(
            d.repo.refresh(), d.repo.chosenChallengeDays(), d.repo.challengeClaimed(),
            d.repo.challengeClaimedDay(), d.gun(0), d.repo.serverCurrent(), d.repo.claimedMilestones(), 19,
        )
        assertTrue(ekran.challenge!!.claimVisible)
        assertTrue(ekran.milestones.first().claimable)
        assertEquals(3, ekran.milestones.first().milestone)

        val tas = sunucu.claimMilestone(3)
        assertNull(tas.error)
        assertEquals(3, tas.rewardKeys)
        StreakSync.onMilestoneClaimed(d.repo, 3)
        assertEquals("already-exists", sunucu.claimMilestone(3).error?.code)
        assertEquals("failed-precondition", sunucu.claimMilestone(7).error?.code)

        val meydan = sunucu.claimChallenge()
        assertNull(meydan.error)
        assertEquals(500, meydan.rewardGold)
        StreakSync.onChallengeClaimed(d.repo, meydan.challengeDays)
        assertEquals(3, d.repo.challengeClaimed())
        assertEquals("already-exists", sunucu.claimChallenge().error?.code)
    }

    @Test
    fun kopukGunSeriyiSunucudaDaSifirlar() {
        val d = SeriDuzenek()
        val sunucu = StreakServerModel(d.saat)
        sunucu.seed(5, 5, d.gun(-3), listOf(3), 0, 0, 0, "")
        val istek = StreakSubmitRequest(listOf(d.gun(0)), 5, 0, 180, d.gun(0), null)
        assertNull(sunucu.submit(istek))
        assertEquals(1, sunucu.current)
        assertEquals(emptyList(), sunucu.claimed)
        assertEquals(5, sunucu.longest)
    }

    @Test
    fun sunucudaDondurmaKopruKurar() {
        val d = SeriDuzenek()
        val sunucu = StreakServerModel(d.saat)
        sunucu.seed(4, 4, d.gun(-2), emptyList(), 0, 0, 1, d.gun(-3))
        assertNull(sunucu.submit(StreakSubmitRequest(listOf(d.gun(0)), 5, 0, 180, d.gun(0), null)))
        assertEquals(5, sunucu.current)
        assertEquals(0, sunucu.freezes)
        assertEquals(listOf(d.gun(-1)), sunucu.frozenDays)
    }

    @Test
    fun dondurmaSatinAlma() {
        val d = SeriDuzenek()
        val sunucu = StreakServerModel(d.saat)
        assertEquals("failed-precondition", sunucu.buyFreeze(d.gun(0), 3999).error?.code)
        val ok = sunucu.buyFreeze(d.gun(0), 4000)
        assertNull(ok.error)
        assertEquals(1, ok.freezes)
        assertEquals(d.gun(0), ok.freezeDay)
        assertEquals("already-exists", sunucu.buyFreeze(d.gun(0), 9000).error?.code)
        assertEquals("invalid-argument", sunucu.buyFreeze("2020-01-01", 9000).error?.code)
        StreakSync.onFreezeBought(d.repo, ok.freezes, ok.freezeDay)
        assertEquals(1, d.repo.freezesHeld())
    }

    @Test
    fun ileriVeEskiGunlerElenir() {
        val d = SeriDuzenek()
        val sunucu = StreakServerModel(d.saat)
        assertNull(sunucu.submit(StreakSubmitRequest(listOf(d.gun(3), d.gun(-9)), 5, 0, 180, d.gun(0), null)))
        assertEquals(0, sunucu.current)
        assertEquals("invalid-argument", sunucu.submit(StreakSubmitRequest(listOf("bozuk"), 5, 0, 180, d.gun(0), null))?.code)
    }

    @Test
    fun yanitAyristirma() {
        val yanit = StreakServerState.fromFunctionResult(
            mapOf(
                "current" to 4, "longest" to 6L, "lastDay" to "2026-11-25", "claimed" to listOf(3),
                "recentDays" to listOf("2026-11-25"), "goalMinutes" to 10, "challengeDays" to 5,
                "challengeClaimed" to 0, "freezeDay" to "", "frozenDays" to emptyList<String>(),
            ),
        )
        assertNotNull(yanit)
        assertEquals(6, yanit.longest)
        assertEquals(StreakRules.FREEZES_UNKNOWN, yanit.freezes)
        assertNull(yanit.reminderLocalHour)
        assertNull(StreakServerState.fromFunctionResult(mapOf("longest" to 3)))
        assertEquals(0, StreakServerState.fromDocument(emptyMap()).freezes)
        val govde = StreakSubmitRequest(listOf("2026-11-25"), 5, 3, 180, "2026-11-25", 18).payload()
        assertEquals(18, govde["reminderHour"])
        assertFalse(StreakSubmitRequest(emptyList(), 5, 3, 180, "2026-11-25", null).payload().containsKey("reminderHour"))
    }
}

class StreakScreenLogicTest {
    private fun durum(current: Int, saniye: Int, hedef: Int = 5) =
        StreakState(current, current, hedef, saniye, emptySet(), emptySet(), false)

    @Test
    fun ustBaslikVeAltBaslik() {
        val kirik = StreakScreenLogic.screen(durum(0, 0), 0, 0, "", "2026-11-25", 0, emptySet(), 19)
        assertFalse(kirik.alive)
        assertEquals("ui_serin_su_anda_kirik", kirik.bigLabel.key)
        assertEquals(StreakText("ui_bugun_x_dakika_calis_yeni_serini", listOf(5)), kirik.bigSubtitle)
        assertNull(kirik.challenge)
        assertTrue(kirik.freezeRowClickable)

        val suren = StreakScreenLogic.screen(durum(2, 130), 0, 0, "", "2026-11-25", 2, emptySet(), 19)
        assertEquals(StreakText("ui_bugun_x_dakika_daha_calis_serin", listOf(3)), suren.bigSubtitle)
        assertEquals(StreakText("ui_x_x_dakika", listOf(2, 5)), suren.todayText)

        val bitti = StreakScreenLogic.screen(durum(3, 400), 0, 0, "", "2026-11-25", 3, emptySet(), 19)
        assertEquals("ui_bugunku_hedefini_tamamladin_yarin_da_bekleri", bitti.bigSubtitle.key)
        assertEquals("ui_hedef_tamamlandi", bitti.todayText.key)
        assertEquals(100f, bitti.todayPercent)
    }

    @Test
    fun meydanOkumaKarti() {
        val bugun = "2026-11-25"
        val c0 = StreakScreenLogic.challenge(durum(0, 0), 3, 0, "", bugun, 0)!!
        assertEquals("ui_bugun_basliyor", c0.subtitle.key)
        assertEquals("0 / 3", c0.progressText)
        val c1 = StreakScreenLogic.challenge(durum(1, 0), 3, 0, "", bugun, 1)!!
        assertEquals(StreakText("ui_x_gunun_x_gunu", listOf(3, 1)), c1.subtitle)
        // Yerelde tamam, sunucuda değil: "ödülün hazırlanıyor".
        val c2 = StreakScreenLogic.challenge(durum(3, 0), 3, 0, "", bugun, 2)!!
        assertEquals("ui_tamamlandi_odulun_hazirlaniyor", c2.subtitle.key)
        assertFalse(c2.claimVisible)
        assertEquals("ui_tamamlandi_2", c2.progressKey)
        val c3 = StreakScreenLogic.challenge(durum(3, 0), 3, 0, "", bugun, 3)!!
        assertTrue(c3.claimVisible)
        assertEquals("ui_tamamlandi_odulunu_topla", c3.subtitle.key)
        // Alındığı gün "TAMAMLANDI ✓", rozet yok; ertesi gün kart gizli.
        val c4 = StreakScreenLogic.challenge(durum(3, 0), 3, 3, bugun, bugun, 3)!!
        assertEquals("ui_tamamlandi_buyuk", c4.subtitle.key)
        assertFalse(c4.rewardBadgeVisible)
        assertNull(StreakScreenLogic.challenge(durum(4, 0), 3, 3, "2026-11-24", bugun, 4))
        // Seçilmemiş meydan okuma: kart yok.
        assertNull(StreakScreenLogic.challenge(durum(4, 0), 0, 0, "", bugun, 4))
    }

    @Test
    fun hatirlatmaSatirlari() {
        val hazir = StreakScreenLogic.reminderRows(19)
        assertEquals(listOf(16, 18, 19, 20, -1), hazir.map { it.value })
        assertEquals("Okuldan sonra", hazir[0].labelLiteral)
        assertEquals("ui_aksam", hazir[2].labelKey)
        assertTrue(hazir[2].selected)
        assertEquals("ui_sec", hazir[4].trailingKey)
        val ozel = StreakScreenLogic.reminderRows(7)
        assertTrue(ozel[4].selected)
        assertEquals("07:00", ozel[4].trailingLiteral)
        assertEquals(listOf(6, 7, 8), StreakRules.hourGrid().take(3))
        assertEquals(5, StreakRules.hourGrid().last())
    }

    @Test
    fun haftaSeridi() {
        val saat = SahteSeriSaati(kotlinx.datetime.LocalDateTime(2026, 11, 25, 10, 0))
        val seri = StreakScreenLogic.weekStrip(saat, setOf("2026-11-23", "2026-11-25"), setOf("2026-11-24", "2026-11-25"))
        assertTrue(seri[0].done)
        assertTrue(seri[1].frozen)
        assertTrue(seri[2].done)
        assertFalse(seri[2].frozen)
        assertTrue(seri[2].today)
        assertEquals(6, seri[6].index)
    }

    @Test
    fun hataMetni() {
        assertEquals(
            StreakErrorText("ui_x_internetini_kontrol_edip_tekrar_dene", "ui_odul_alinamadi", null),
            StreakScreenLogic.errorText(null, null, "ui_odul_alinamadi", true),
        )
        assertEquals("Bu ödülü zaten aldın.", StreakScreenLogic.errorText("already-exists", "Bu ödülü zaten aldın.", "ui_odul_alinamadi", true).literal)
        assertEquals("ui_x_birazdan_tekrar_dene", StreakScreenLogic.errorText("already-exists", "x", "ui_odul_alinamadi", false).key)
        assertEquals("ui_sunucuya_su_an_ulasilamiyor_birazdan_tekrar", StreakScreenLogic.errorText("unavailable", null, "k", true).key)
    }
}

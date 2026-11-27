package com.sorobit.shared.baglanti

import com.sorobit.shared.ders.GlobalLessonData
import com.sorobit.shared.matematik.RastgeleKaynagi
import com.sorobit.shared.oyun.DailyQuestionPeriod
import com.sorobit.shared.oyun.DailyQuestionRepository
import com.sorobit.shared.oyun.MissionQuestState
import com.sorobit.shared.oyun.MissionUiLogic
import com.sorobit.shared.oyun.MissionWindow
import com.sorobit.shared.oyun.MissionsProgressStore
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Güncel Android şablonunda hiçbir sandık bitmiş gelmez (eskiden part 1–6'da 14 tane vardı); günlük soru
 * havuzu yalnızca bitmiş sandıklardan beslendiği için ilerlemiş bir kullanıcı taklit edilir.
 */
private fun sandiklariBitmisPart(partId: Int) = GlobalLessonData.loadLessonItemsForPart(partId, null).map {
    if (it.type == com.sorobit.shared.ders.LessonItem.TYPE_CHEST) it.copy(stepIsFinish = true) else it
}

class GorevGunlukSoruKopruleriTest {
    @AfterTest
    fun temizle() = RastgeleKaynagi.varsayilanaDon()

    // ── Görevler ────────────────────────────────────────────────────────────

    @Test
    fun bosDepoMisafirGibiSifirdanBaslar() {
        val depo = bellekIciGorevDeposuKur(elapsedRealtimeMs = { 1_000L }, ornekSayaclar = emptyMap())
        val durum = gorevEkraniDurumu(depo)
        assertEquals(3, durum.weekly.size)
        assertEquals(3, durum.daily.size)
        (durum.weekly + durum.daily).forEach { satir ->
            assertEquals(0, satir.progress)
            assertEquals(MissionQuestState.PENDING, satir.state)
            assertFalse(satir.canClaim)
        }
        durum.weekly.forEach {
            assertEquals(MissionWindow.WEEKLY, it.window)
            assertEquals(MissionUiLogic.ICON_WEEKLY, it.iconRes)
        }
        durum.daily.forEach {
            assertEquals(MissionWindow.DAILY, it.window)
            assertEquals(MissionUiLogic.ICON_DAILY, it.iconRes)
        }
        assertEquals(MissionUiLogic.RES_HOURS_SHORT, durum.dailyCountdown.resKey)
        assertTrue(durum.dailyCountdown.value in 1..24)
        assertTrue(durum.weeklyCountdown.value >= 1)
    }

    @Test
    fun ekranDurumuDogrudanCagrilarlaAyni() {
        val depo = bellekIciGorevDeposuKur(
            elapsedRealtimeMs = { 1_000L },
            ornekSayaclar = ornekSayaclar(),
        )
        val durum = gorevEkraniDurumu(depo)

        // Android: MissionsFragment.updateMissionsUI ile aynı çağrılar, elle.
        val snap = depo.getSnapshot()
        val beklenenHaftalik = depo.selectedMissionsForWeekly().map { gorev ->
            MissionUiLogic.questData(
                gorev, MissionWindow.WEEKLY, snap,
                depo.isMissionRewardClaimed(MissionWindow.WEEKLY, gorev.id),
            )
        }
        val beklenenGunluk = depo.selectedMissionsForDaily().map { gorev ->
            MissionUiLogic.questData(
                gorev, MissionWindow.DAILY, snap,
                depo.isMissionRewardClaimed(MissionWindow.DAILY, gorev.id),
            )
        }
        assertEquals(beklenenHaftalik, durum.weekly)
        assertEquals(beklenenGunluk, durum.daily)
        assertEquals(
            MissionUiLogic.formatWeeklyCountdown(depo.millisUntilWeeklyReset()),
            durum.weeklyCountdown,
        )
        assertEquals(
            MissionUiLogic.dailyCountdownLabel(depo.hoursUntilDailyReset()),
            durum.dailyCountdown,
        )

        // Örnek sayaçlar yüklendi: ilerleme hedefe kırpılır, en az bir satır ödüle hazırdır.
        (durum.weekly + durum.daily).forEach { assertTrue(it.progress in 0..it.target) }
        assertTrue((durum.weekly + durum.daily).any { it.canClaim })
    }

    @Test
    fun odulAlininceSatirAlindiOlurVeBulutaYazilir() {
        val dosyalar = mutableMapOf<String, com.sorobit.shared.oyun.OyunKeyValueStore>()
        val bulut = BellekIciGorevBulutu(uid = "u1", baslangicBelgesi = ornekSayaclar())
        val depo = gorevDeposuKur(
            prefsProvider = { ad -> dosyalar.getOrPut(ad) { com.sorobit.shared.oyun.InMemoryKeyValueStore() } },
            cloud = bulut,
            elapsedRealtimeMs = { 5_000L },
        )
        val hazir = (gorevEkraniDurumu(depo).daily + gorevEkraniDurumu(depo).weekly).first { it.canClaim }
        depo.markMissionRewardClaimed(hazir.window, hazir.missionId)

        val sonra = gorevEkraniDurumu(depo)
        val satir = (sonra.daily + sonra.weekly).first { it.missionId == hazir.missionId && it.window == hazir.window }
        assertTrue(satir.isClaimed)
        assertEquals(MissionQuestState.CLAIMED, satir.state)
        assertFalse(satir.canClaim)

        val anahtarlar = bulut.belge?.get(MissionsProgressStore.FIELD_REWARD_CLAIMED_KEYS) as? List<*>
        assertNotNull(anahtarlar)
        assertEquals(1, anahtarlar.size)
        // Tercih dosyası adı Android'deki gibi: missions_progress_<uid>
        assertTrue(dosyalar.containsKey("${MissionsProgressStore.PREFS}_u1"))
    }

    @Test
    fun bellekIciBulutMisafirdeHicYazmaz() {
        val depo = bellekIciGorevDeposuKur(elapsedRealtimeMs = { 1L }, ornekSayaclar = emptyMap())
        val ilk = gorevEkraniDurumu(depo).daily.first()
        depo.markMissionRewardClaimed(MissionWindow.DAILY, ilk.missionId)
        assertTrue(gorevEkraniDurumu(depo).daily.first().isClaimed)
    }

    private fun ornekSayaclar(): Map<String, Int> = mapOf(
        MissionsProgressStore.KEY_DAILY_STEP_FINISH_COUNT to 2,
        MissionsProgressStore.KEY_WEEKLY_STEP_FINISH_COUNT to 3,
        MissionsProgressStore.KEY_DAILY_STEP_INCREMENT_COUNT to 5,
        MissionsProgressStore.KEY_WEEKLY_STEP_INCREMENT_COUNT to 12,
        MissionsProgressStore.KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT to 2,
        MissionsProgressStore.KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT to 4,
        MissionsProgressStore.KEY_DAILY_CHEST_RECORD_BREAK_COUNT to 1,
        MissionsProgressStore.KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT to 1,
        MissionsProgressStore.KEY_DAILY_CHEST_STAR_GAIN_COUNT to 2,
        MissionsProgressStore.KEY_WEEKLY_CHEST_STAR_GAIN_COUNT to 5,
        MissionsProgressStore.KEY_DAILY_LEARN_MINUTES_COUNT to 10,
        MissionsProgressStore.KEY_WEEKLY_LEARN_MINUTES_COUNT to 25,
    )

    // ── Günlük soru ─────────────────────────────────────────────────────────

    private val gun0 = 20_000L * DailyQuestionPeriod.PERIOD_MS + 1_000L

    private fun kaynaklar(havuz: com.sorobit.shared.oyun.DailyQuestionPoolBuilder) = gunlukSoruKaynaklari(
        havuz,
        sandiklariBitmisPart(1),
        sandiklariBitmisPart(2),
        sandiklariBitmisPart(3),
    )

    @Test
    fun varsayilanKartYuklenmemisGelir() {
        val kart = gunlukSoruVarsayilanKart(poolAvailable = false, nowMs = gun0)
        assertEquals("period_20000", kart.periodKey)
        assertEquals(0, kart.solvedCount)
        assertEquals(DailyQuestionPeriod.QUESTIONS_PER_PERIOD, kart.totalCount)
        assertEquals("", kart.titleUnit)
        assertEquals("", kart.difficulty)
        assertFalse(kart.isComplete)
        assertFalse(kart.rewardClaimed)
        assertFalse(kart.poolAvailable)
        assertNull(kart.pendingContinueSlotIndex)
        assertFalse(kart.isLoaded)

        val alindi = gunlukSoruOdulAlindiKart(kart)
        assertTrue(alindi.rewardClaimed)
        assertTrue(alindi.isLoaded)
        assertEquals(kart.periodKey, alindi.periodKey)
        assertEquals(kart.solvedCount, alindi.solvedCount)
    }

    @Test
    fun kaynaklarPartSirasiylaBirlesir() {
        val havuz = gunlukSoruHavuzuKur(Random(7))
        val hepsi = kaynaklar(havuz)
        val elle = (1..3).flatMap { part ->
            havuz.buildSourcesForPart(sandiklariBitmisPart(part).asOyunLessonItems(), part)
        }
        assertEquals(elle.map { it.partId to it.listIndex }, hepsi.map { it.partId to it.listIndex })
        assertTrue(hepsi.isNotEmpty())
        assertEquals(hepsi.map { it.partId }.sorted(), hepsi.map { it.partId })
    }

    @Test
    fun depoAkisiAndroidSirasiniIzler() {
        RastgeleKaynagi.rastgele = Random(11)
        var simdi = gun0
        val havuz = gunlukSoruHavuzuKur(Random(11))
        val depo = BellekIciGunlukSoruDeposu("u1", havuz, ornekBasariOrani = 64, nowMs = { simdi })
        val kaynak = kaynaklar(havuz)

        // İlk yükleme: üç soruluk meydan okuma üretilir ve belgeye yazılır.
        assertNull(depo.belge)
        val ilk = assertNotNull(depo.loadOrCreateChallenge(kaynak))
        assertEquals("period_20000", ilk.periodKey)
        assertEquals(3, ilk.questions.size)
        assertEquals(0, ilk.solvedCount)
        assertEquals(DailyQuestionRepository.challengeToFirestoreMap(ilk), depo.belge)

        // İkinci yükleme aynı soruları döndürür (yeniden üretmez).
        val ikinci = assertNotNull(depo.loadOrCreateChallenge(kaynak))
        assertEquals(ilk.questions, ikinci.questions)

        // Kart: yüklenmiş, havuz var, zorluk tanınıyorsa başarı oranı eklenir.
        val kart = depo.loadChallengeForCard(kaynak)
        assertTrue(kart.isLoaded)
        assertTrue(kart.poolAvailable)
        assertEquals(0, kart.solvedCount)
        assertEquals(ilk.cardTitleUnit(), kart.titleUnit)
        val beklenenOran = if (DailyQuestionRepository.difficultyKey(kart.difficulty) != null) 64 else null
        assertEquals(beklenenOran, kart.globalSuccessRate)

        // Yanlış cevap: aynı soru anahtarla devam bekler.
        assertTrue(depo.markPendingDiamondContinue(ilk.periodKey, 0))
        val bekleyen = assertNotNull(depo.loadOrCreateChallenge(kaynak))
        assertTrue(bekleyen.needsDiamondContinue)
        assertEquals(0, bekleyen.pendingContinueSlotIndex)
        assertTrue(depo.loadChallengeForCard(kaynak).needsDiamondContinue)

        // Anahtarla devam: bekleme kalkar.
        assertTrue(depo.clearPendingDiamondContinue(ilk.periodKey))
        assertFalse(assertNotNull(depo.loadOrCreateChallenge(kaynak)).needsDiamondContinue)

        // Üç doğru cevap: sayaç 3'te durur, meydan okuma tamamlanır.
        assertEquals(1, depo.incrementSolvedCount(ilk.periodKey))
        assertEquals(2, depo.incrementSolvedCount(ilk.periodKey))
        assertEquals(3, depo.incrementSolvedCount(ilk.periodKey))
        assertEquals(3, depo.incrementSolvedCount(ilk.periodKey))
        val bitti = depo.loadChallengeForCard(kaynak)
        assertTrue(bitti.isComplete)
        assertTrue(bitti.canClaimReward)
        assertEquals("", bitti.difficulty)

        // Ödül alındı.
        assertTrue(depo.markRewardClaimed(ilk.periodKey))
        val alindi = depo.loadChallengeForCard(kaynak)
        assertTrue(alindi.rewardClaimed)
        assertFalse(alindi.canClaimReward)

        // Periyot devri: yeni meydan okuma sıfırdan.
        simdi += DailyQuestionPeriod.PERIOD_MS
        val yeni = depo.loadChallengeForCard(kaynak)
        assertEquals("period_20001", yeni.periodKey)
        assertNotEquals(alindi.periodKey, yeni.periodKey)
        assertEquals(0, yeni.solvedCount)
        assertFalse(yeni.rewardClaimed)
    }

    @Test
    fun kullaniciYoksaHicbirSeyYuklenmez() {
        val havuz = gunlukSoruHavuzuVarsayilan()
        val depo = BellekIciGunlukSoruDeposu(null, havuz, ornekBasariOrani = null, nowMs = { gun0 })
        val kaynak = kaynaklar(havuz)
        assertNull(depo.loadOrCreateChallenge(kaynak))
        val kart = depo.loadChallengeForCard(kaynak)
        assertTrue(kart.isLoaded)
        assertTrue(kart.poolAvailable)
        assertEquals(0, kart.solvedCount)
        assertFalse(depo.markRewardClaimed(kart.periodKey))
        assertFalse(depo.markPendingDiamondContinue(kart.periodKey, 1))
        assertFalse(depo.clearPendingDiamondContinue(kart.periodKey))
        assertEquals(0, depo.incrementSolvedCount(kart.periodKey))
        assertNull(depo.belge)
    }

    @Test
    fun havuzBosIseKartHavuzYokDer() {
        val havuz = gunlukSoruHavuzuVarsayilan()
        val depo = bellekIciGunlukSoruDeposuKur("u1", havuz, null)
        val kart = depo.loadChallengeForCard(emptyList())
        assertTrue(kart.isLoaded)
        assertFalse(kart.poolAvailable)
        assertEquals(0, kart.solvedCount)
        assertNull(depo.belge)
    }
}

class GunlukSoruCubukVeBaslatmaTest {
    private val gun0 = 20_000L * DailyQuestionPeriod.PERIOD_MS + 1_000L

    private fun kart(cozulen: Int, alindi: Boolean) = com.sorobit.shared.oyun.DailyQuestionCardUiState(
        periodKey = "period_20000",
        solvedCount = cozulen,
        totalCount = 3,
        titleUnit = "Ünite",
        difficulty = "Kolay",
        isComplete = cozulen >= 3,
        rewardClaimed = alindi,
        poolAvailable = true,
        isLoaded = true,
    )

    @Test
    fun cubukSurerkenTuruncuVeSayacliEtiket() {
        val g = gunlukSoruCubukGorunumu(kart(1, false), pct = 33.333f, trackWidth = 200, startInset = 14, endGap = 10)
        assertEquals(GunlukSoruCubukModu.PROGRESS, g.mode)
        assertEquals("daily_question_progress_track", g.trackDrawable)
        assertEquals("daily_question_progress_format", g.labelResKey)
        assertEquals(1, g.labelCurrent)
        assertEquals(3, g.labelTotal)
        assertEquals("white", g.labelColor)
        val o = assertNotNull(g.overlay)
        assertEquals(66, o.fillWidth)
        assertEquals(42, o.shineWidth)
        assertEquals("daily_question_progress_fill", o.fillDrawable)
    }

    @Test
    fun cubukDolmadanOdulHazirGorunmez() {
        // 3/3 ama animasyon henüz %99,5'e ulaşmadı: hâlâ turuncu ve sayaçlı.
        val yolda = gunlukSoruCubukGorunumu(kart(3, false), pct = 90f, trackWidth = 200, startInset = 14, endGap = 10)
        assertEquals(GunlukSoruCubukModu.PROGRESS, yolda.mode)
        assertEquals(3, yolda.labelCurrent)

        val dolu = gunlukSoruCubukGorunumu(kart(3, false), pct = 100f, trackWidth = 200, startInset = 14, endGap = 10)
        assertEquals(GunlukSoruCubukModu.CLAIM_READY, dolu.mode)
        assertEquals(MissionUiLogic.RES_COMPLETED_LABEL, dolu.labelResKey)
        assertEquals("background_color", dolu.labelColor)
        assertEquals("daily_question_progress_fill_complete", assertNotNull(dolu.overlay).fillDrawable)
    }

    @Test
    fun odulAlininceGriVeParlamasiz() {
        val g = gunlukSoruCubukGorunumu(kart(3, true), pct = 100f, trackWidth = 200, startInset = 14, endGap = 10)
        assertEquals(GunlukSoruCubukModu.CLAIMED, g.mode)
        assertEquals("mission_progress_track", g.trackDrawable)
        assertEquals(MissionUiLogic.RES_REWARD_CLAIMED_LABEL, g.labelResKey)
        assertEquals("black", g.labelColor)
        val o = assertNotNull(g.overlay)
        assertEquals("mission_progress_fill_claimed", o.fillDrawable)
        assertFalse(o.shineVisible)
        assertEquals(200, o.fillWidth)
    }

    @Test
    fun genislikYokkenKatmanYok() {
        assertNull(gunlukSoruCubukGorunumu(kart(0, false), 0f, trackWidth = 0, startInset = 14, endGap = 10).overlay)
    }

    @Test
    fun animasyonEgrisiYavaslayarakBiter() {
        assertEquals(0f, gunlukSoruCubukAnimasyonYuzdesi(0f, 100f, 0f))
        assertEquals(100f, gunlukSoruCubukAnimasyonYuzdesi(0f, 100f, 1f))
        assertEquals(100f, gunlukSoruCubukAnimasyonYuzdesi(0f, 100f, 2f))
        // 1 - 0.5^3.2 = 0.8911...
        val orta = gunlukSoruCubukAnimasyonYuzdesi(0f, 100f, 0.5f)
        assertTrue(orta > 89.0f && orta < 89.2f)
        val ucteBir = gunlukSoruCubukAnimasyonYuzdesi(33.333f, 66.666f, 0.5f)
        assertTrue(ucteBir > 33.333f && ucteBir < 66.666f)
    }

    @Test
    fun baslatmaVeDevamVerisi() {
        val havuz = gunlukSoruHavuzuKur(Random(3))
        val kaynak = gunlukSoruKaynaklari(
            havuz,
            sandiklariBitmisPart(1),
            sandiklariBitmisPart(2),
            sandiklariBitmisPart(3),
        )
        val depo = BellekIciGunlukSoruDeposu("u1", havuz, null) { gun0 }
        val kalp = bellekIciKalpDeposuKur("u1")
        val anim = bellekIciKartAnimDeposuKur("u1")
        assertNull(anim.getLastShown("period_20000"))
        anim.setLastShown("period_20000", 2)
        assertEquals(2, anim.getLastShown("period_20000"))
        val ilk = assertNotNull(depo.loadOrCreateChallenge(kaynak))

        val b = assertNotNull(gunlukSoruBaslatma(ilk))
        val slot0 = ilk.questions[0]
        assertEquals(ilk.periodKey, b.periodKey)
        assertEquals(0, b.slotIndex)
        assertEquals(slot0.partId, b.partId)
        assertEquals(slot0.displayIntervalMs, b.displayIntervalMs)
        if (slot0.mathOperation != null) {
            assertTrue(b.sequence.isEmpty())
            assertEquals(slot0.mathOperation?.firstNumber, b.mathFirst)
            assertEquals(slot0.mathOperation?.operator, b.mathOperator)
            assertEquals(slot0.mathOperation?.secondNumber, b.mathSecond)
        } else {
            assertEquals(slot0.sequence, b.sequence)
            assertNull(b.mathFirst)
        }

        // Yanlış cevap: bekleme yazılır, kırık kalp bir sonraki bağlamada oynar.
        assertTrue(gunlukSoruSonucunuIsle(depo, kalp, ilk.periodKey, 0, isSuccess = false, nowMs = gun0))
        val bekleyen = assertNotNull(depo.loadOrCreateChallenge(kaynak))
        assertTrue(bekleyen.needsDiamondContinue)
        assertTrue(kalp.consumePlayRequest(ilk.periodKey))

        val devam = assertNotNull(gunlukSoruDevamBaslatma(bekleyen))
        assertEquals(0, devam.slotIndex)
        assertEquals(b.sequence, devam.sequence)
        val devamKarti = gunlukSoruDevamSonrasiKart(bekleyen, poolAvailable = true)
        assertFalse(devamKarti.needsDiamondContinue)
        assertTrue(devamKarti.isLoaded)

        // Doğru cevap: sayaç artar, bekleme ve kırık kalp temizlenir.
        kalp.setBrokenHold116(ilk.periodKey, true)
        assertTrue(gunlukSoruSonucunuIsle(depo, kalp, ilk.periodKey, 0, isSuccess = true, nowMs = gun0))
        val sonra = assertNotNull(depo.loadOrCreateChallenge(kaynak))
        assertEquals(1, sonra.solvedCount)
        assertFalse(sonra.needsDiamondContinue)
        assertFalse(kalp.isBrokenHold116(ilk.periodKey))
        assertEquals(1, assertNotNull(gunlukSoruBaslatma(sonra)).slotIndex)

        // Periyodu geçmiş oturumun sonucu yazılmaz.
        assertFalse(
            gunlukSoruSonucunuIsle(
                depo, kalp, ilk.periodKey, 1, isSuccess = true,
                nowMs = gun0 + DailyQuestionPeriod.PERIOD_MS,
            ),
        )
        assertEquals(1, assertNotNull(depo.loadOrCreateChallenge(kaynak)).solvedCount)
    }
}

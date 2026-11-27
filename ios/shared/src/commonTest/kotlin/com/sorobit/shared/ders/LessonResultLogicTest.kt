package com.sorobit.shared.ders

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LessonResultLogicTest {

    @BeforeTest
    fun sifirla() {
        GlobalLessonData.resetForTest()
        LessonManager.resetForTest()
    }

    @Test
    fun successRateVeLessonScore() {
        assertEquals(0f, LessonResultLogic.successRate(0, 0))
        assertEquals(0f, LessonResultLogic.successRate(5, 0))
        assertEquals(0f, LessonResultLogic.successRate(5, -1))
        assertEquals(100f, LessonResultLogic.successRate(10, 10))
        assertEquals(80f, LessonResultLogic.successRate(8, 10))
        assertEquals(87.5f, LessonResultLogic.successRate(7, 8))
        // 1/3 → %33,33 → puan 166 (aşağı yuvarlanır)
        assertEquals(33.333332f, LessonResultLogic.successRate(1, 3), 0.0001f)
        assertEquals(166, LessonResultLogic.lessonScore(LessonResultLogic.successRate(1, 3)))
        assertEquals(500, LessonResultLogic.lessonScore(100f))
        assertEquals(400, LessonResultLogic.lessonScore(80f))
        assertEquals(437, LessonResultLogic.lessonScore(87.5f))
        assertEquals(0, LessonResultLogic.lessonScore(0f))
        assertEquals("87%", LessonResultLogic.successRateText(87.5f))
        assertEquals("100%", LessonResultLogic.successRateText(100f))
        assertEquals("0%", LessonResultLogic.successRateText(0f))
    }

    @Test
    fun onClaim_dallar() {
        // Bitmemiş ders → sandık ekranı; tutorialIsFinish yerinde true olur
        val d = ders()
        assertEquals(
            LessonResultLogic.ClaimDecision(LessonResultLogic.ClaimBranch.GO_TO_CHEST, recordItemReplay = false),
            LessonResultLogic.onClaim(d),
        )
        assertTrue(d.tutorialIsFinish)
        // Bitmiş ders → haritaya, tekrar kaydı
        assertEquals(
            LessonResultLogic.ClaimDecision(LessonResultLogic.ClaimBranch.SKIP_TO_MAP, recordItemReplay = true, isLessonTypeReturn = true),
            LessonResultLogic.onClaim(ders().copy(stepIsFinish = true)),
        )
        // Sandıkta haritaya dönüş "ders türü" dönüşü değildir (Android: isLessonTypeReturn = item.type == TYPE_LESSON)
        assertEquals(
            LessonResultLogic.ClaimDecision(LessonResultLogic.ClaimBranch.SKIP_TO_MAP, recordItemReplay = true, isLessonTypeReturn = false),
            LessonResultLogic.onClaim(sandik().copy(stepIsFinish = true)),
        )
        // Bitmiş ama ders/sandık olmayan tür → tekrar kaydı yok
        assertEquals(
            LessonResultLogic.ClaimDecision(LessonResultLogic.ClaimBranch.SKIP_TO_MAP, recordItemReplay = false),
            LessonResultLogic.onClaim(baslik().copy(stepIsFinish = true)),
        )
        // Item yok → sandık dalı
        assertEquals(
            LessonResultLogic.ClaimDecision(LessonResultLogic.ClaimBranch.GO_TO_CHEST, recordItemReplay = false),
            LessonResultLogic.onClaim(null),
        )
    }

    @Test
    fun lessonResultFalse() {
        assertEquals(
            LessonResultFalseLogic.FailRecord(3, 12, 2),
            LessonResultFalseLogic.failRecordFor(ders(currentStep = 2), 12, 3),
        )
        assertEquals(
            LessonResultFalseLogic.FailRecord(1, 4, 1),
            LessonResultFalseLogic.failRecordFor(sandik(), 4, 1),
        )
        // Güncel Android: başarısız denemenin başarı oranı da kayda gider (Analytics answerSuccessRatePercent)
        assertEquals(
            LessonResultFalseLogic.FailRecord(2, 3, 1, 60f),
            LessonResultFalseLogic.failRecordFor(ders(), 3, 2, successRate = 60f),
        )
        assertNull(LessonResultFalseLogic.failRecordFor(baslik(), 0, 1))
        assertNull(LessonResultFalseLogic.failRecordFor(null, 0, 1))
        // Varsayılan part: GlobalLessonData.globalPartId
        GlobalLessonData.globalPartId = 5
        assertEquals(5, LessonResultFalseLogic.failRecordFor(ders(), 1)?.partId)

        assertEquals("Puan 500'den düşük!", LessonResultFalseLogic.resultTitleComment(true))
        // Eşik %80'den %75'e indi (Android: ui_basari_orani_75_ten_dusuk)
        assertEquals("Başarı oranı %75'ten düşük!", LessonResultFalseLogic.resultTitleComment(false))
        assertTrue(LessonResultFalseLogic.isLessonTypeReturn(ders()))
        assertFalse(LessonResultFalseLogic.isLessonTypeReturn(sandik()))
        assertFalse(LessonResultFalseLogic.isLessonTypeReturn(null))

        assertTrue(LessonResultFalseLogic.shouldRedirectToLogin(1, isLoggedIn = false))
        assertFalse(LessonResultFalseLogic.shouldRedirectToLogin(1, isLoggedIn = true))
        assertFalse(LessonResultFalseLogic.shouldRedirectToLogin(0, isLoggedIn = false))
    }
}

class ChestResultLogicTest {

    private lateinit var sink: KayitSink

    @BeforeTest
    fun sifirla() {
        GlobalLessonData.resetForTest()
        LessonManager.resetForTest()
        sink = KayitSink()
        GlobalLessonData.sink = sink
    }

    private fun yukle(partId: Int, degistir: (MutableList<LessonItem>) -> Unit = {}) {
        GlobalLessonData.initialize(partId)
        val liste = GlobalLessonData.createLessonItems(partId).toMutableList()
        degistir(liste)
        GlobalLessonData.onLoadFromFirestore(partId, liste, error = false)
    }

    @Test
    fun record_dallar() {
        // Sandık değil → dokunulmaz
        val d = ders()
        assertFalse(ChestResultLogic.record(d, 900, 3))
        assertNull(d.record)
        // İlk rekor: görev bayrağı yok, alanlar yerinde yazılır
        val s = sandik()
        assertFalse(ChestResultLogic.record(s, 900, 3))
        assertEquals(900, s.record)
        assertEquals(3, s.leaderboardSeasonId)
        assertEquals(900, s.leaderboardSeasonBest)
        // Rekor kırıldı → bayrak
        assertTrue(ChestResultLogic.record(s, 1000, 3))
        assertEquals(1000, s.record)
        assertEquals(1000, s.leaderboardSeasonBest)
        // Eşit ya da düşük → bayrak yok, rekor kalır
        assertFalse(ChestResultLogic.record(s, 1000, 3))
        assertFalse(ChestResultLogic.record(s, 10, 3))
        assertEquals(1000, s.record)
        // Yeni sezonda düşük skor: rekor kalır, sezon en iyisi koşu skoru olur
        assertFalse(ChestResultLogic.record(s, 400, 4))
        assertEquals(1000, s.record)
        assertEquals(4, s.leaderboardSeasonId)
        assertEquals(400, s.leaderboardSeasonBest)
    }

    @Test
    fun shouldSkipChest() {
        assertTrue(ChestResultLogic.shouldSkipChest(sandik().copy(stepIsFinish = true)))
        assertFalse(ChestResultLogic.shouldSkipChest(sandik()))
        assertFalse(ChestResultLogic.shouldSkipChest(ders().copy(stepIsFinish = true)))
    }

    @Test
    fun replayFinishedChest_rekorVeYildizGuncellenir() {
        // Part 1 idx 15: bitmiş sandık (cupPoint1 1400, cupPoint2 1200). Güncel şablonda bitmiş gelmez → testte bitmiş yapılır.
        yukle(1) {
            it[15] = it[15].copy(
                stepIsFinish = true, record = 600, stepCupIcon = "chest_stars_tier1", leaderboardSeasonId = 2, leaderboardSeasonBest = 600,
            )
        }
        val r = ChestResultLogic.replayFinishedChest(15, toplamPuan = 1250, season = 3)
        val g = GlobalLessonData.getLessonItem(15)!!
        assertEquals(g, r.updated)
        assertEquals(1250, g.record)
        assertEquals("chest_stars_tier2", g.stepCupIcon)
        assertEquals(3, g.leaderboardSeasonId)
        assertEquals(1250, g.leaderboardSeasonBest)
        assertEquals(1, r.gainedStars)
        assertFalse(r.shouldIncrementKarate)
        assertFalse(r.justFinishedChestForRating)
        assertEquals(listOf("tek:${kimlik(1, 15)}", "lb:${kimlik(1, 15)}"), sink.sira)
        assertEquals(GlobalLessonData.LeaderboardSubmit(1, "p1_i15_unite_maratonu", 1250, 3, "5'lik Toplama"), sink.gonderimler.single())
    }

    @Test
    fun replayFinishedChest_ilkKezUcYildiz_karate() {
        yukle(1) { it[15] = it[15].copy(stepIsFinish = true, record = 600, stepCupIcon = "chest_stars_tier1") }
        val r = ChestResultLogic.replayFinishedChest(15, 1400, 3)
        assertTrue(r.shouldIncrementKarate)
        assertEquals(2, r.gainedStars)
        assertEquals("chest_stars_tier3", r.updated?.stepCupIcon)
    }

    @Test
    fun replayFinishedChest_dusukSkor_degisiklikYokAmaYazilir() {
        yukle(1) {
            it[15] = it[15].copy(
                stepIsFinish = true, record = 1500, stepCupIcon = "chest_stars_tier3", leaderboardSeasonId = 3, leaderboardSeasonBest = 1500,
            )
        }
        val r = ChestResultLogic.replayFinishedChest(15, 300, 3)
        assertEquals(0, r.gainedStars)
        assertFalse(r.shouldIncrementKarate)
        assertEquals(1500, r.updated?.record)
        assertEquals("chest_stars_tier3", r.updated?.stepCupIcon)
        assertEquals(1500, r.updated?.leaderboardSeasonBest)
        assertEquals(listOf("tek:${kimlik(1, 15)}", "lb:${kimlik(1, 15)}"), sink.sira)
    }

    @Test
    fun replayFinishedChest_sandikDegilVeyaYok() {
        yukle(1)
        var r = ChestResultLogic.replayFinishedChest(1, 900, 3)
        assertEquals(ChestResultLogic.ReplayResult(null, 0, false, false), r)
        r = ChestResultLogic.replayFinishedChest(99, 900, 3)
        assertEquals(ChestResultLogic.ReplayResult(null, 0, false, false), r)
        assertTrue(sink.sira.isEmpty())
        // Bitmemiş sandık (normalde bu dala girilmez): puanlama bayrağı true
        r = ChestResultLogic.replayFinishedChest(4, 100, 3)
        assertTrue(r.justFinishedChestForRating)
        assertEquals(100, r.updated?.record)
    }

    @Test
    fun replayRozetKosullari_androidTutarsizligiAynen() {
        assertTrue(ChestResultLogic.shouldIncrementTornadoOnReplay(4))
        assertFalse(ChestResultLogic.shouldIncrementTornadoOnReplay(7))
        assertTrue(ChestResultLogic.shouldIncrementVolcanoOnReplay(8))
        assertFalse(ChestResultLogic.shouldIncrementVolcanoOnReplay(4))
    }

    @Test
    fun resolveScoreCapVeCupPoint2() {
        yukle(1)
        // Bellekteki sandık: cupPoint1 1600, cupPoint2 1300
        assertEquals(1600, ChestResultLogic.resolveScoreCap(4))
        assertEquals(1300, ChestResultLogic.resolveCupPoint2Threshold(4, 1600))
        // Ders: ne bellekte ne şablonda → 1 ve max(1, cap / 2)
        assertEquals(1, ChestResultLogic.resolveScoreCap(1))
        assertEquals(1, ChestResultLogic.resolveCupPoint2Threshold(1, 1))
        assertEquals(400, ChestResultLogic.resolveCupPoint2Threshold(1, 800))
        // Bellekte yok/0 ise şablona düşer
        yukle(1) { it[4] = it[4].copy(cupPoint1 = null, cupPoint2 = 0) }
        assertEquals(1600, ChestResultLogic.resolveScoreCap(4))
        assertEquals(1300, ChestResultLogic.resolveCupPoint2Threshold(4, 1600))
        // Bellekteki değer önceliklidir
        yukle(1) { it[4] = it[4].copy(cupPoint1 = 2000, cupPoint2 = 1500) }
        assertEquals(2000, ChestResultLogic.resolveScoreCap(4))
        assertEquals(1500, ChestResultLogic.resolveCupPoint2Threshold(4, 2000))
        // Liste dışı indeks
        assertEquals(1, ChestResultLogic.resolveScoreCap(99))
    }

    @Test
    fun scoreProgressVeYildizEsikleri() {
        assertEquals(0f, ChestResultLogic.scoreProgress(-10, 1600))
        assertEquals(0.5f, ChestResultLogic.scoreProgress(800, 1600))
        assertEquals(1f, ChestResultLogic.scoreProgress(1600, 1600))
        assertEquals(1f, ChestResultLogic.scoreProgress(5000, 1600))

        assertEquals(emptyList(), ChestResultLogic.starTiersCrossed(0, 499, 1300, 1600))
        assertEquals(listOf(1), ChestResultLogic.starTiersCrossed(499, 500, 1300, 1600))
        assertEquals(emptyList(), ChestResultLogic.starTiersCrossed(500, 600, 1300, 1600))
        assertEquals(listOf(2), ChestResultLogic.starTiersCrossed(1299, 1300, 1300, 1600))
        assertEquals(listOf(3), ChestResultLogic.starTiersCrossed(1599, 1700, 1300, 1600))
        // Tek adımda hepsi
        assertEquals(listOf(1, 2, 3), ChestResultLogic.starTiersCrossed(0, 1600, 1300, 1600))
        // Geriye doğru sayarken eşik geçilmez
        assertEquals(emptyList(), ChestResultLogic.starTiersCrossed(1600, 0, 1300, 1600))
    }

    @Test
    fun parseTimeToSecondsVeFormatSeconds() {
        assertEquals(83, ChestResultLogic.parseTimeToSeconds("01:23"))
        assertEquals(83, ChestResultLogic.parseTimeToSeconds(" 1 : 23 "))
        assertEquals(3723, ChestResultLogic.parseTimeToSeconds("1:02:03"))
        assertEquals(0, ChestResultLogic.parseTimeToSeconds("0:00"))
        // Saniye 60'ı aşsa da toplanır
        assertEquals(150, ChestResultLogic.parseTimeToSeconds("1:90"))
        assertNull(ChestResultLogic.parseTimeToSeconds(""))
        assertNull(ChestResultLogic.parseTimeToSeconds("83"))
        assertNull(ChestResultLogic.parseTimeToSeconds("a:10"))
        assertNull(ChestResultLogic.parseTimeToSeconds("1:b"))
        assertNull(ChestResultLogic.parseTimeToSeconds("1:2:c"))
        assertNull(ChestResultLogic.parseTimeToSeconds("1:2:3:4"))

        assertEquals("0:00", ChestResultLogic.formatSeconds(0))
        assertEquals("0:05", ChestResultLogic.formatSeconds(5))
        assertEquals("1:23", ChestResultLogic.formatSeconds(83))
        assertEquals("10:00", ChestResultLogic.formatSeconds(600))
        assertEquals("61:01", ChestResultLogic.formatSeconds(3661))
        assertEquals("0:00", ChestResultLogic.formatSeconds(-7))
    }
}

// Android BadgeProgressEngine.kt (BadgeProgressEngine + BadgeProgressRepository) davranışının testleri.
package com.sorobit.shared.kupa.rozet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BadgeProgressEngineTest {

    private fun w(c: Int, t: Int) = BadgeProgressWindow(c, t)

    private fun state(p: UserBadgeProgress, kind: BadgeKind): BadgeState =
        BadgeProgressEngine.calculate(p).first { it.kind == kind }

    // ---------------------------------------------------------------- pencere / eşik

    @Test
    fun fishingProgressWindow() {
        assertEquals(w(0, 3), BadgeProgressEngine.fishingProgressWindow(0))
        assertEquals(w(0, 3), BadgeProgressEngine.fishingProgressWindow(-4))
        assertEquals(w(2, 3), BadgeProgressEngine.fishingProgressWindow(2))
        assertEquals(w(3, 5), BadgeProgressEngine.fishingProgressWindow(3))
        assertEquals(w(5, 15), BadgeProgressEngine.fishingProgressWindow(5))
        assertEquals(w(49, 50), BadgeProgressEngine.fishingProgressWindow(49))
        assertEquals(w(50, 55), BadgeProgressEngine.fishingProgressWindow(50))
        assertEquals(w(54, 55), BadgeProgressEngine.fishingProgressWindow(54))
        assertEquals(w(55, 60), BadgeProgressEngine.fishingProgressWindow(55))
        assertEquals(15, BadgeProgressEngine.fishingNextThreshold(7))
    }

    @Test
    fun rocketProgressWindowListeSonrasi10arAdim() {
        assertEquals(w(0, 3), BadgeProgressEngine.rocketProgressWindow(0))
        assertEquals(w(3, 10), BadgeProgressEngine.rocketProgressWindow(3))
        assertEquals(w(10, 15), BadgeProgressEngine.rocketProgressWindow(10))
        assertEquals(w(29, 30), BadgeProgressEngine.rocketProgressWindow(29))
        assertEquals(w(30, 40), BadgeProgressEngine.rocketProgressWindow(30))
        assertEquals(w(39, 40), BadgeProgressEngine.rocketProgressWindow(39))
        assertEquals(w(40, 50), BadgeProgressEngine.rocketProgressWindow(40))
        assertEquals(40, BadgeProgressEngine.rocketNextThreshold(31))
    }

    @Test
    fun tornadoVeVolcanoEsikleri() {
        for (f in listOf(BadgeProgressEngine::tornadoNextThreshold, BadgeProgressEngine::volcanoNextThreshold)) {
            assertEquals(1, f(0))
            assertEquals(1, f(-2))
            assertEquals(3, f(1))
            assertEquals(15, f(14))
            assertEquals(18, f(15))
            assertEquals(18, f(17))
            assertEquals(21, f(18))
        }
        assertEquals(w(15, 18), BadgeProgressEngine.tornadoProgressWindow(15))
        assertEquals(w(4, 5), BadgeProgressEngine.volcanoProgressWindow(4))
        assertEquals(w(0, 1), BadgeProgressEngine.volcanoProgressWindow(0))
    }

    @Test
    fun kupaRozetiEsikleri() {
        val esikler = listOf(
            BadgeProgressEngine::dinoNextThreshold,
            BadgeProgressEngine::crocodileNextThreshold,
            BadgeProgressEngine::goatNextThreshold,
            BadgeProgressEngine::eagleNextThreshold,
            BadgeProgressEngine::flyNextThreshold,
            BadgeProgressEngine::turtleNextThreshold,
        )
        for (f in esikler) {
            assertEquals(500, f(-5))
            assertEquals(500, f(0))
            assertEquals(500, f(499))
            assertEquals(1000, f(500))
            assertEquals(2500, f(2499))
            assertEquals(3000, f(2500))
            assertEquals(3000, f(2999))
            assertEquals(3500, f(3000))
        }
        val pencereler = listOf(
            BadgeProgressEngine::dinoProgressWindow,
            BadgeProgressEngine::crocodileProgressWindow,
            BadgeProgressEngine::goatProgressWindow,
            BadgeProgressEngine::eagleProgressWindow,
            BadgeProgressEngine::flyProgressWindow,
            BadgeProgressEngine::turtleProgressWindow,
        )
        for (f in pencereler) {
            assertEquals(w(0, 500), f(-1))
            assertEquals(w(600, 1000), f(600))
            assertEquals(w(2500, 3000), f(2500))
            assertEquals(w(3100, 3500), f(3100))
        }
    }

    @Test
    fun bowlingProgressWindow() {
        assertEquals(w(4, 5), BadgeProgressEngine.bowlingProgressWindow(4))
        assertEquals(w(50, 100), BadgeProgressEngine.bowlingProgressWindow(50))
        assertEquals(w(100, 105), BadgeProgressEngine.bowlingProgressWindow(100))
        assertEquals(w(109, 110), BadgeProgressEngine.bowlingProgressWindow(109))
    }

    // ---------------------------------------------------------------- calculate

    @Test
    fun calculateVarsayilanIlerlemedeHerSeyKilitli() {
        val states = BadgeProgressEngine.calculate(UserBadgeProgress())
        assertEquals(
            listOf(
                BadgeKind.CUP, BadgeKind.GOLD, BadgeKind.SILVER, BadgeKind.BRONZE, BadgeKind.KARATE,
                BadgeKind.BOWLING, BadgeKind.DART, BadgeKind.FISHING, BadgeKind.GOLF, BadgeKind.ROCKET,
                BadgeKind.TORNADO, BadgeKind.VOLCANO, BadgeKind.DINO, BadgeKind.CROCODILE, BadgeKind.GOAT,
                BadgeKind.EAGLE, BadgeKind.FLY, BadgeKind.TURTLE,
            ),
            states.map { it.kind },
        )
        assertTrue(states.none { it.unlocked })
        assertTrue(states.all { it.levelTone == null })
        // Kilitliyken kademeli rozetlerde ilk eşik gösterilir
        val degerler = states.associate { it.kind to it.value }
        assertEquals(null, degerler[BadgeKind.CUP])
        assertEquals(null, degerler[BadgeKind.GOLD])
        assertEquals(null, degerler[BadgeKind.KARATE])
        assertEquals(5, degerler[BadgeKind.BOWLING])
        assertEquals(3, degerler[BadgeKind.DART])
        assertEquals(3, degerler[BadgeKind.FISHING])
        assertEquals(3, degerler[BadgeKind.GOLF])
        assertEquals(3, degerler[BadgeKind.ROCKET])
        assertEquals(1, degerler[BadgeKind.TORNADO])
        assertEquals(1, degerler[BadgeKind.VOLCANO])
        for (k in listOf(BadgeKind.DINO, BadgeKind.CROCODILE, BadgeKind.GOAT, BadgeKind.EAGLE, BadgeKind.FLY, BadgeKind.TURTLE)) {
            assertEquals(500, degerler[k])
        }
        val goster = states.associate { it.kind to it.showValue }
        for (k in listOf(BadgeKind.CUP, BadgeKind.GOLD, BadgeKind.SILVER, BadgeKind.BRONZE, BadgeKind.KARATE)) {
            assertFalse(goster[k]!!)
        }
        assertTrue(goster[BadgeKind.DART]!!)
    }

    @Test
    fun calculateDartKademeVeTon() {
        fun dart(p: Int) = state(UserBadgeProgress(userDartProgress = p), BadgeKind.DART)
        assertEquals(BadgeState(BadgeKind.DART, false, 3, true, null), dart(2))
        assertEquals(BadgeState(BadgeKind.DART, true, 3, true, BadgeLevelTone.BRONZE), dart(3))
        assertEquals(BadgeState(BadgeKind.DART, true, 3, true, BadgeLevelTone.BRONZE), dart(9))
        assertEquals(BadgeState(BadgeKind.DART, true, 10, true, BadgeLevelTone.SILVER), dart(10))
        assertEquals(BadgeState(BadgeKind.DART, true, 20, true, BadgeLevelTone.RED), dart(20))
        assertEquals(BadgeState(BadgeKind.DART, true, 30, true, BadgeLevelTone.ORIGINAL), dart(49))
        assertEquals(BadgeState(BadgeKind.DART, true, 50, true, BadgeLevelTone.PURPLE), dart(50))
        assertEquals(BadgeState(BadgeKind.DART, true, 50, true, BadgeLevelTone.PURPLE), dart(54))
        assertEquals(BadgeState(BadgeKind.DART, true, 55, true, BadgeLevelTone.PURPLE), dart(57))
    }

    @Test
    fun calculateRoketVeDinoListeSonrasi() {
        fun rocket(p: Int) = state(UserBadgeProgress(userRocketProgress = p), BadgeKind.ROCKET)
        assertEquals(25, rocket(25).value)
        assertEquals(BadgeLevelTone.ORIGINAL, rocket(25).levelTone)
        assertEquals(30, rocket(30).value)
        assertEquals(BadgeLevelTone.PURPLE, rocket(30).levelTone)
        assertEquals(30, rocket(39).value)
        assertEquals(40, rocket(45).value)

        fun dino(p: Int) = state(UserBadgeProgress(userDinoProgress = p), BadgeKind.DINO)
        assertFalse(dino(499).unlocked)
        assertEquals(500 to BadgeLevelTone.BRONZE, dino(500).value to dino(500).levelTone)
        assertEquals(1000 to BadgeLevelTone.SILVER, dino(1200).value to dino(1200).levelTone)
        assertEquals(1500 to BadgeLevelTone.RED, dino(1500).value to dino(1500).levelTone)
        assertEquals(2000 to BadgeLevelTone.ORIGINAL, dino(2000).value to dino(2000).levelTone)
        assertEquals(2500 to BadgeLevelTone.PURPLE, dino(2500).value to dino(2500).levelTone)
        assertEquals(2500 to BadgeLevelTone.PURPLE, dino(2600).value to dino(2600).levelTone)
        assertEquals(3000, dino(3100).value)
    }

    @Test
    fun calculateKupaMadalyaKarate() {
        val p = UserBadgeProgress(
            userKarateProgress = 2,
            goldMedalPiece = listOf(MedalPieceRow("A", 1)),
            bronzeMedalPiece = listOf(MedalPieceRow("B", 2)),
            cupPiece = listOf(CupPieceRow("A", 7, 1), CupPieceRow("B", 3, 2)),
        )
        assertEquals(BadgeState(BadgeKind.CUP, true, 3, true), state(p, BadgeKind.CUP))
        assertEquals(BadgeState(BadgeKind.GOLD, true, null, false), state(p, BadgeKind.GOLD))
        assertEquals(BadgeState(BadgeKind.SILVER, false, null, false), state(p, BadgeKind.SILVER))
        assertEquals(BadgeState(BadgeKind.BRONZE, true, null, false), state(p, BadgeKind.BRONZE))
        assertEquals(BadgeState(BadgeKind.KARATE, true, 2, true), state(p, BadgeKind.KARATE))
    }

    @Test
    fun calculateFishingKademeAlaniniKullanir() {
        // Değer seri sayısından değil kalıcı kademeden (userFishingProgress) gelir.
        val s = state(UserBadgeProgress(userFishingProgress = 15, userFishingStreak = 0), BadgeKind.FISHING)
        assertEquals(BadgeState(BadgeKind.FISHING, true, 15, true, BadgeLevelTone.RED), s)
    }

    // ---------------------------------------------------------------- seri

    @Test
    fun effectiveFishingStreak() {
        fun p(streak: Int, key: String) = UserBadgeProgress(userFishingStreak = streak, userFishingStreakPeriodKey = key)
        assertEquals(4, BadgeProgressEngine.effectiveFishingStreak(p(4, ""), "period_11"))
        assertEquals(4, BadgeProgressEngine.effectiveFishingStreak(p(4, "period_11"), "period_11"))
        assertEquals(4, BadgeProgressEngine.effectiveFishingStreak(p(4, "period_10"), "period_11"))
        assertEquals(0, BadgeProgressEngine.effectiveFishingStreak(p(4, "period_10"), "period_12"))
        assertEquals(0, BadgeProgressEngine.effectiveFishingStreak(p(4, "bozuk"), "period_12"))
        assertEquals(0, BadgeProgressEngine.effectiveFishingStreak(p(4, "period_10"), "bozuk"))
        assertEquals(0, BadgeProgressEngine.effectiveFishingStreak(p(-3, ""), "period_12"))
    }

    @Test
    fun fishingStreakPeriodYardimcilari() {
        assertEquals("period_0", FishingStreakPeriod.currentPeriodKey(0L))
        assertEquals("period_0", FishingStreakPeriod.currentPeriodKey(86_399_999L))
        assertEquals("period_1", FishingStreakPeriod.currentPeriodKey(86_400_000L))
        assertEquals("period_20778", FishingStreakPeriod.currentPeriodKey(1_795_219_200_000L))
        assertEquals(12L, FishingStreakPeriod.periodBucketFromKey("period_12"))
        assertNull(FishingStreakPeriod.periodBucketFromKey("12"))
        assertNull(FishingStreakPeriod.periodBucketFromKey("period_x"))
        assertFalse(FishingStreakPeriod.isSamePeriod("", ""))
        assertTrue(FishingStreakPeriod.isSamePeriod("period_1", "period_1"))
        assertTrue(FishingStreakPeriod.isConsecutivePeriodAfter("period_1", "period_2"))
        assertFalse(FishingStreakPeriod.isConsecutivePeriodAfter("period_1", "period_3"))
        assertFalse(FishingStreakPeriod.isConsecutivePeriodAfter("", "period_3"))
        assertFalse(FishingStreakPeriod.isConsecutivePeriodAfter("x", "period_3"))
        assertFalse(FishingStreakPeriod.isFishingStreakBroken("period_3", "period_2"))
    }

    @Test
    fun badgeKindModEslesmesi() {
        assertEquals(18, BadgeKind.entries.size)
        assertEquals(18, BadgeAnimMode.entries.size)
        assertEquals(BadgeAnimMode.entries.toSet(), BadgeKind.entries.map { it.mode }.toSet())
        assertEquals(BadgeKind.DART, BadgeKind.fromMode(BadgeAnimMode.DAILY_ONLY))
        assertEquals(BadgeKind.CUP, BadgeKind.fromMode(BadgeAnimMode.CUP_GOOGLE))
        assertEquals("Elit Seviye Kupası", BadgeKind.CUP.title)
        assertEquals("1 / 5", BadgeKind.FISHING.subtext)
        assertEquals("0 / 500", BadgeKind.TURTLE.subtext)
    }

    // ---------------------------------------------------------------- BadgeProgressRepository

    @Test
    fun repositoryDurumVeGuncellemeler() {
        BadgeProgressRepository.update(UserBadgeProgress(userDartProgress = 12))
        assertEquals(12, BadgeProgressRepository.getUserBadgeProgress().userDartProgress)
        assertEquals(10, BadgeProgressRepository.getState(BadgeKind.DART)!!.value)
        assertEquals(10, BadgeProgressRepository.getStateByMode(BadgeAnimMode.DAILY_ONLY)!!.value)
        assertEquals(18, BadgeProgressRepository.getStates().size)

        BadgeProgressRepository.updateDartProgress(-3)
        assertEquals(0, BadgeProgressRepository.getUserBadgeProgress().userDartProgress)
        assertFalse(BadgeProgressRepository.getState(BadgeKind.DART)!!.unlocked)

        BadgeProgressRepository.updateBowlingProgress(20)
        BadgeProgressRepository.updateKarateProgress(1)
        BadgeProgressRepository.updateGolfProgress(-1)
        BadgeProgressRepository.updateTornadoProgress(3)
        BadgeProgressRepository.updateVolcanoProgress(5)
        BadgeProgressRepository.updateFishingSync(-1, -2, "period_9")
        BadgeProgressRepository.updateRocketSync(-1, -5, "2026-1")
        val p = BadgeProgressRepository.getUserBadgeProgress()
        assertEquals(
            UserBadgeProgress(
                userBowlingProgress = 20,
                userKarateProgress = 1,
                userTornadoProgress = 3,
                userVolcanoProgress = 5,
                userFishingStreakPeriodKey = "period_9",
                userRocketDailyDayId = "2026-1",
            ),
            p,
        )
        assertEquals(BadgeLevelTone.RED, BadgeProgressRepository.getState(BadgeKind.BOWLING)!!.levelTone)
        assertEquals(BadgeLevelTone.SILVER, BadgeProgressRepository.getState(BadgeKind.TORNADO)!!.levelTone)
    }

    @Test
    fun repositoryGetProgressWindowByMode() {
        BadgeProgressRepository.update(
            UserBadgeProgress(
                userDartProgress = 12,
                userGolfProgress = 100,
                userBowlingProgress = 0,
                userTornadoProgress = 2,
                userVolcanoProgress = 15,
                userDinoProgress = 600,
                userCrocodileProgress = 0,
                userGoatProgress = 2500,
                userEagleProgress = 999,
                userFlyProgress = 1000,
                userTurtleProgress = 3200,
                userFishingProgress = 5,
                userFishingStreak = 4,
                userFishingStreakPeriodKey = "period_10",
                userRocketProgress = 3,
                userRocketDailyLessons = 2,
                userRocketDailyDayId = "2026-325",
            ),
        )
        fun win(m: BadgeAnimMode, today: String = "2026-325", key: String = "period_11") =
            BadgeProgressRepository.getProgressWindowByMode(m, today, key)
        assertEquals(w(12, 20), win(BadgeAnimMode.DAILY_ONLY))
        assertEquals(w(100, 105), win(BadgeAnimMode.GOLF_WITH_BASE))
        assertEquals(w(0, 5), win(BadgeAnimMode.BOWLING_WITH_BASE))
        assertEquals(w(2, 3), win(BadgeAnimMode.TORNADO))
        assertEquals(w(15, 18), win(BadgeAnimMode.VOLCANO))
        assertEquals(w(600, 1000), win(BadgeAnimMode.DINO))
        assertEquals(w(0, 500), win(BadgeAnimMode.CROCODILE))
        assertEquals(w(2500, 3000), win(BadgeAnimMode.GOAT))
        assertEquals(w(999, 1000), win(BadgeAnimMode.EAGLE))
        assertEquals(w(1000, 1500), win(BadgeAnimMode.FLY))
        assertEquals(w(3200, 3500), win(BadgeAnimMode.TURTLE))
        // Derin Avcı: pay = güncel seri, payda = kademenin bir sonraki eşiği
        assertEquals(w(4, 15), win(BadgeAnimMode.FISHING_WITH_BASE))
        assertEquals(w(0, 15), win(BadgeAnimMode.FISHING_WITH_BASE, key = "period_12"))
        // Roket: pay = bugünkü ders sayısı (gün değiştiyse 0), payda = bir sonraki eşik
        assertEquals(w(2, 10), win(BadgeAnimMode.ROCKET_WITH_BASE))
        assertEquals(w(0, 10), win(BadgeAnimMode.ROCKET_WITH_BASE, today = "2026-326"))
        for (m in listOf(BadgeAnimMode.CUP_GOOGLE, BadgeAnimMode.KARATE, BadgeAnimMode.GOLD_MEDAL, BadgeAnimMode.SILVER_MEDAL, BadgeAnimMode.BRONZE_MEDAL)) {
            assertNull(win(m))
        }
    }

    @Test
    fun repositoryGetProgressValueByMode() {
        BadgeProgressRepository.update(
            UserBadgeProgress(
                userDartProgress = 1, userFishingProgress = 2, userGolfProgress = 3, userTornadoProgress = 4,
                userVolcanoProgress = 5, userDinoProgress = 6, userCrocodileProgress = 7, userGoatProgress = 8,
                userEagleProgress = 9, userFlyProgress = 10, userTurtleProgress = 11, userRocketProgress = 12,
                userBowlingProgress = 13, userKarateProgress = 14,
                cupPiece = listOf(CupPieceRow("A", 9, 1), CupPieceRow("B", 4, 1)),
                goldMedalPiece = listOf(MedalPieceRow("A", 1)),
            ),
        )
        val beklenen = mapOf(
            BadgeAnimMode.DAILY_ONLY to 1, BadgeAnimMode.FISHING_WITH_BASE to 2, BadgeAnimMode.GOLF_WITH_BASE to 3,
            BadgeAnimMode.TORNADO to 4, BadgeAnimMode.VOLCANO to 5, BadgeAnimMode.DINO to 6,
            BadgeAnimMode.CROCODILE to 7, BadgeAnimMode.GOAT to 8, BadgeAnimMode.EAGLE to 9, BadgeAnimMode.FLY to 10,
            BadgeAnimMode.TURTLE to 11, BadgeAnimMode.ROCKET_WITH_BASE to 12, BadgeAnimMode.BOWLING_WITH_BASE to 13,
            BadgeAnimMode.KARATE to 14, BadgeAnimMode.CUP_GOOGLE to 4,
            BadgeAnimMode.GOLD_MEDAL to null, BadgeAnimMode.SILVER_MEDAL to null, BadgeAnimMode.BRONZE_MEDAL to null,
        )
        for ((m, v) in beklenen) assertEquals(v, BadgeProgressRepository.getProgressValueByMode(m), m.name)
        BadgeProgressRepository.update(UserBadgeProgress())
        assertNull(BadgeProgressRepository.getProgressValueByMode(BadgeAnimMode.CUP_GOOGLE))
        assertEquals(0, BadgeProgressRepository.getProgressValueByMode(BadgeAnimMode.KARATE))
    }

    @Test
    fun repositoryGetLevelSpecByMode() {
        assertEquals(listOf(3, 10, 20, 30, 50) to 5, BadgeProgressRepository.getLevelSpecByMode(BadgeAnimMode.DAILY_ONLY))
        assertEquals(listOf(3, 5, 15, 25, 50) to 5, BadgeProgressRepository.getLevelSpecByMode(BadgeAnimMode.FISHING_WITH_BASE))
        assertEquals(listOf(3, 5, 10, 15, 20) to 5, BadgeProgressRepository.getLevelSpecByMode(BadgeAnimMode.GOLF_WITH_BASE))
        assertEquals(listOf(5, 10, 20, 50, 100) to 5, BadgeProgressRepository.getLevelSpecByMode(BadgeAnimMode.BOWLING_WITH_BASE))
        assertEquals(listOf(1, 3, 5, 10, 15) to 3, BadgeProgressRepository.getLevelSpecByMode(BadgeAnimMode.TORNADO))
        assertEquals(listOf(1, 3, 5, 10, 15) to 3, BadgeProgressRepository.getLevelSpecByMode(BadgeAnimMode.VOLCANO))
        for (m in listOf(BadgeAnimMode.DINO, BadgeAnimMode.CROCODILE, BadgeAnimMode.GOAT, BadgeAnimMode.EAGLE, BadgeAnimMode.FLY, BadgeAnimMode.TURTLE)) {
            assertEquals(listOf(500, 1000, 1500, 2000, 2500) to 500, BadgeProgressRepository.getLevelSpecByMode(m))
        }
        // Android güncel: roket için tek tablo (BadgeProgressEngine.levelSpec) [3,10,15,25,30], adım 10.
        assertEquals(listOf(3, 10, 15, 25, 30) to 10, BadgeProgressRepository.getLevelSpecByMode(BadgeAnimMode.ROCKET_WITH_BASE))
        for (m in listOf(BadgeAnimMode.CUP_GOOGLE, BadgeAnimMode.KARATE, BadgeAnimMode.GOLD_MEDAL, BadgeAnimMode.SILVER_MEDAL, BadgeAnimMode.BRONZE_MEDAL)) {
            assertNull(BadgeProgressRepository.getLevelSpecByMode(m))
        }
    }

    @Test
    fun repositoryFormatLeveledTierRatio() {
        fun golf(p: Int): String? {
            BadgeProgressRepository.update(UserBadgeProgress(userGolfProgress = p))
            return BadgeProgressRepository.formatLeveledTierRatio(BadgeAnimMode.GOLF_WITH_BASE)
        }
        assertEquals("0/5", golf(0))
        assertEquals("1/5", golf(4))
        assertEquals("2/5", golf(5))
        assertEquals("5/5", golf(20))
        assertEquals("5/5", golf(49))
        assertEquals("5/5", golf(100))
        assertEquals("5/5", golf(110))

        BadgeProgressRepository.update(UserBadgeProgress(userRocketProgress = 10, userKarateProgress = 3))
        // Roket: tek tablo [3,10,15,25,30] → 10 için 2/5
        assertEquals("2/5", BadgeProgressRepository.formatLeveledTierRatio(BadgeAnimMode.ROCKET_WITH_BASE))
        assertNull(BadgeProgressRepository.formatLeveledTierRatio(BadgeAnimMode.KARATE))
        assertNull(BadgeProgressRepository.formatLeveledTierRatio(BadgeAnimMode.GOLD_MEDAL))
        assertNull(BadgeProgressRepository.formatLeveledTierRatio(BadgeAnimMode.CUP_GOOGLE))
    }

    @Test
    fun repositoryResolveShownValueByMode() {
        val r = BadgeProgressRepository
        assertEquals(0, r.resolveShownValueByMode(BadgeAnimMode.KARATE, -2))
        assertEquals(7, r.resolveShownValueByMode(BadgeAnimMode.KARATE, 7))
        assertEquals(3, r.resolveShownValueByMode(BadgeAnimMode.DAILY_ONLY, -1))
        assertEquals(3, r.resolveShownValueByMode(BadgeAnimMode.DAILY_ONLY, 2))
        assertEquals(3, r.resolveShownValueByMode(BadgeAnimMode.DAILY_ONLY, 3))
        assertEquals(20, r.resolveShownValueByMode(BadgeAnimMode.DAILY_ONLY, 25))
        assertEquals(50, r.resolveShownValueByMode(BadgeAnimMode.DAILY_ONLY, 50))
        assertEquals(50, r.resolveShownValueByMode(BadgeAnimMode.DAILY_ONLY, 54))
        assertEquals(60, r.resolveShownValueByMode(BadgeAnimMode.DAILY_ONLY, 61))
        assertEquals(15, r.resolveShownValueByMode(BadgeAnimMode.TORNADO, 17))
        assertEquals(18, r.resolveShownValueByMode(BadgeAnimMode.TORNADO, 18))
        assertEquals(3000, r.resolveShownValueByMode(BadgeAnimMode.DINO, 3499))
        // Roket (tek tablo [3,10,15,25,30], adım 10)
        assertEquals(10, r.resolveShownValueByMode(BadgeAnimMode.ROCKET_WITH_BASE, 12))
        assertEquals(30, r.resolveShownValueByMode(BadgeAnimMode.ROCKET_WITH_BASE, 30))
        assertNull(r.resolveShownValueByMode(BadgeAnimMode.GOLD_MEDAL, 1))
        assertNull(r.resolveShownValueByMode(BadgeAnimMode.CUP_GOOGLE, 1))
    }

    @Test
    fun repositoryResolveToneByMode() {
        val r = BadgeProgressRepository
        assertNull(r.resolveToneByMode(BadgeAnimMode.KARATE, 5))
        assertNull(r.resolveToneByMode(BadgeAnimMode.GOLD_MEDAL, 5))
        assertNull(r.resolveToneByMode(BadgeAnimMode.DAILY_ONLY, 2))
        assertNull(r.resolveToneByMode(BadgeAnimMode.DAILY_ONLY, -9))
        assertEquals(BadgeLevelTone.BRONZE, r.resolveToneByMode(BadgeAnimMode.DAILY_ONLY, 3))
        assertEquals(BadgeLevelTone.BRONZE, r.resolveToneByMode(BadgeAnimMode.DAILY_ONLY, 9))
        assertEquals(BadgeLevelTone.SILVER, r.resolveToneByMode(BadgeAnimMode.DAILY_ONLY, 10))
        assertEquals(BadgeLevelTone.RED, r.resolveToneByMode(BadgeAnimMode.DAILY_ONLY, 20))
        assertEquals(BadgeLevelTone.ORIGINAL, r.resolveToneByMode(BadgeAnimMode.DAILY_ONLY, 30))
        assertEquals(BadgeLevelTone.PURPLE, r.resolveToneByMode(BadgeAnimMode.DAILY_ONLY, 50))
        assertEquals(BadgeLevelTone.PURPLE, r.resolveToneByMode(BadgeAnimMode.DAILY_ONLY, 500))
        // Roket: depo ve motor artık aynı tabloyu kullanır; aynı ilerlemede aynı ton.
        assertEquals(BadgeLevelTone.ORIGINAL, r.resolveToneByMode(BadgeAnimMode.ROCKET_WITH_BASE, 25))
        assertEquals(BadgeLevelTone.RED, r.resolveToneByMode(BadgeAnimMode.ROCKET_WITH_BASE, 15))
        assertEquals(
            BadgeLevelTone.ORIGINAL,
            BadgeProgressEngine.calculate(UserBadgeProgress(userRocketProgress = 25)).first { it.kind == BadgeKind.ROCKET }.levelTone,
        )
    }
}

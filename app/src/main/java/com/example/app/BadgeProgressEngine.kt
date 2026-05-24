package com.example.app

/** Firestore `goldMedalPiece` / `silverMedalPiece` / `bronzeMedalPiece`: [titleUnit veya "null", sezon]. */
data class MedalPieceRow(
    val titleUnit: String,
    val season: Int,
)

/** Firestore `cupPiece`: [titleUnit veya "null", sıra, sezon]. */
data class CupPieceRow(
    val titleUnit: String,
    val rank: Int,
    val season: Int,
)

data class UserBadgeProgress(
    val userDartProgress: Int = 0,
    val userKarateProgress: Int = 0,
    val userGolfProgress: Int = 0,
    val userBowlingProgress: Int = 0,
    /** Kalıcı kademe eşiği ([rocketLevel] / [userRocketProgress] gibi; seri kırılsa da düşmez). */
    val userFishingProgress: Int = 0,
    /** Güncel art arda günlük soru serisi; periyot atlanınca 0. */
    val userFishingStreak: Int = 0,
    /** Son başarılı günlük soru serisi güncellemesinin [DailyQuestionPeriod] anahtarı. */
    val userFishingStreakPeriodKey: String = "",
    val userRocketProgress: Int = 0,
    /** Bugünkü tamamlanan ders sayısı; [userRocketDailyDayId] ile gün değişiminde sıfırlanır. */
    val userRocketDailyLessons: Int = 0,
    val userRocketDailyDayId: String = "",
    /** Eski alan; rozet mantığında kullanılmıyor, geriye dönük okuma için tutulabilir. */
    val abacusLeaderboardRank: Int = Int.MAX_VALUE,
    val goldMedalPiece: List<MedalPieceRow> = emptyList(),
    val silverMedalPiece: List<MedalPieceRow> = emptyList(),
    val bronzeMedalPiece: List<MedalPieceRow> = emptyList(),
    val cupPiece: List<CupPieceRow> = emptyList(),
    /**
     * Sezon sonu finalize ile yeni madalya/kupa yazıldı; kullanıcı [SeasonLeaderboardRewardGateFragment] ile
     * toplayana kadar Firestore'da kalır (çoklu cihaz).
     */
    val pendingLeaderboardRewardSeason: Int? = null,
)

enum class BadgeKind(
    val mode: BadgeFragment.BadgeAnimMode,
    val title: String,
    val subtext: String,
) {
    CUP(BadgeFragment.BadgeAnimMode.CUP_GOOGLE, "Elit Seviye Kupası", ""),
    GOLD(BadgeFragment.BadgeAnimMode.GOLD_MEDAL, "Şampiyonluk Madalyası", ""),
    SILVER(BadgeFragment.BadgeAnimMode.SILVER_MEDAL, "Gümüş Madalya", ""),
    BRONZE(BadgeFragment.BadgeAnimMode.BRONZE_MEDAL, "Bronz Madalya", ""),
    KARATE(BadgeFragment.BadgeAnimMode.KARATE, "Siyah Kuşak", ""),
    BOWLING(BadgeFragment.BadgeAnimMode.BOWLING_WITH_BASE, "Strike Ustası", "1 / 10"),
    DART(BadgeFragment.BadgeAnimMode.DAILY_ONLY, "Kusursuz Odak", "1 / 10"),
    FISHING(BadgeFragment.BadgeAnimMode.FISHING_WITH_BASE, "Derin Avcı", "1 / 5"),
    GOLF(BadgeFragment.BadgeAnimMode.GOLF_WITH_BASE, "Nokta Atışı", "1 / 10"),
    ROCKET(BadgeFragment.BadgeAnimMode.ROCKET_WITH_BASE, "Sınır Tanımaz", "1 / 10"),
    ;

    companion object {
        fun fromMode(mode: BadgeFragment.BadgeAnimMode): BadgeKind? = entries.firstOrNull { it.mode == mode }
    }
}

data class BadgeState(
    val kind: BadgeKind,
    val unlocked: Boolean,
    val value: Int?,
    val showValue: Boolean,
    val levelTone: BadgeLevelTone? = null,
)

data class BadgeProgressWindow(
    val current: Int,
    val target: Int,
)

enum class BadgeLevelTone {
    BRONZE,
    SILVER,
    ORIGINAL,
    RED,
    PURPLE,
}

object BadgeProgressEngine {
    private val dartLevel = listOf(3, 10, 20, 30, 50)
    private val golfLevel = listOf(5, 10, 20, 50, 100)
    private val rocketLevel = listOf(3, 5, 10, 15, 25)
    private val bowlingLevel = listOf(5, 10, 20, 50, 100)
    private val fishingLevel = listOf(3, 5, 15, 25, 50)

    private const val dartLevelUp = 5
    private const val fishingLevelUp = 5
    private const val golfLevelUp = 5
    private const val rocketLevelUp = 5
    private const val bowlingLevelUp = 5
    private const val karateLevelUp = 5

    /** Günlük soru serisi: en az bir periyot atlanmışsa 0; aksi halde [userFishingStreak]. */
    fun effectiveFishingStreak(progress: UserBadgeProgress): Int {
        if (DailyQuestionPeriod.isFishingStreakBroken(progress.userFishingStreakPeriodKey)) {
            return 0
        }
        return progress.userFishingStreak.coerceAtLeast(0)
    }

    /** Derin Avcı: bir sonraki kademe eşiği ([fishingLevel] listesine göre). */
    fun fishingProgressWindow(tierProgress: Int): BadgeProgressWindow {
        val levels = fishingLevel
        val levelUpStep = fishingLevelUp
        val safeProgress = tierProgress.coerceAtLeast(0)
        val first = levels.first()
        val last = levels.last()
        if (safeProgress < first) {
            return BadgeProgressWindow(current = safeProgress, target = first)
        }
        if (safeProgress < last) {
            val nextTarget = levels.firstOrNull { it > safeProgress } ?: last
            return BadgeProgressWindow(current = safeProgress, target = nextTarget)
        }
        val extraSteps = ((safeProgress - last) / levelUpStep) + 1
        val nextTarget = last + (extraSteps * levelUpStep)
        return BadgeProgressWindow(current = safeProgress, target = nextTarget)
    }

    fun fishingNextThreshold(tierProgress: Int): Int = fishingProgressWindow(tierProgress).target

    fun calculate(progress: UserBadgeProgress): List<BadgeState> {
        val cupUnlocked = progress.cupPiece.isNotEmpty()
        val karateUnlocked = progress.userKarateProgress > 0
        val dartUnlocked = progress.userDartProgress >= dartLevel.first()
        val golfUnlocked = progress.userGolfProgress >= golfLevel.first()
        val rocketUnlocked = progress.userRocketProgress >= rocketLevel.first()
        val bowlingUnlocked = progress.userBowlingProgress >= bowlingLevel.first()
        val fishingUnlocked = progress.userFishingProgress >= fishingLevel.first()

        val dartValue = resolveLeveledValue(progress.userDartProgress, dartLevel, dartLevelUp, dartUnlocked)
        val fishingValue = resolveLeveledValue(progress.userFishingProgress, fishingLevel, fishingLevelUp, fishingUnlocked)
        val golfValue = resolveLeveledValue(progress.userGolfProgress, golfLevel, golfLevelUp, golfUnlocked)
        val rocketValue = resolveLeveledValue(progress.userRocketProgress, rocketLevel, rocketLevelUp, rocketUnlocked)
        val bowlingValue = resolveLeveledValue(progress.userBowlingProgress, bowlingLevel, bowlingLevelUp, bowlingUnlocked)
        val dartTone = resolveLevelTone(progress.userDartProgress, dartLevel, dartUnlocked)
        val golfTone = resolveLevelTone(progress.userGolfProgress, golfLevel, golfUnlocked)
        val rocketTone = resolveLevelTone(progress.userRocketProgress, rocketLevel, rocketUnlocked)
        val bowlingTone = resolveLevelTone(progress.userBowlingProgress, bowlingLevel, bowlingUnlocked)
        val fishingTone = resolveLevelTone(progress.userFishingProgress, fishingLevel, fishingUnlocked)
        val karateValue = if (karateUnlocked) progress.userKarateProgress else null
        val cupValue = if (cupUnlocked) {
            progress.cupPiece.minOfOrNull { it.rank }
        } else {
            null
        }

        return listOf(
            BadgeState(BadgeKind.CUP, unlocked = cupUnlocked, value = cupValue, showValue = cupUnlocked),
            BadgeState(BadgeKind.GOLD, unlocked = progress.goldMedalPiece.isNotEmpty(), value = null, showValue = false),
            BadgeState(BadgeKind.SILVER, unlocked = progress.silverMedalPiece.isNotEmpty(), value = null, showValue = false),
            BadgeState(BadgeKind.BRONZE, unlocked = progress.bronzeMedalPiece.isNotEmpty(), value = null, showValue = false),
            BadgeState(BadgeKind.KARATE, unlocked = karateUnlocked, value = karateValue, showValue = karateUnlocked),
            BadgeState(BadgeKind.BOWLING, unlocked = bowlingUnlocked, value = bowlingValue, showValue = true, levelTone = bowlingTone),
            BadgeState(BadgeKind.DART, unlocked = dartUnlocked, value = dartValue, showValue = true, levelTone = dartTone),
            BadgeState(BadgeKind.FISHING, unlocked = fishingUnlocked, value = fishingValue, showValue = true, levelTone = fishingTone),
            BadgeState(BadgeKind.GOLF, unlocked = golfUnlocked, value = golfValue, showValue = true, levelTone = golfTone),
            BadgeState(BadgeKind.ROCKET, unlocked = rocketUnlocked, value = rocketValue, showValue = true, levelTone = rocketTone),
        )
    }

    private fun resolveLeveledValue(
        progress: Int,
        levels: List<Int>,
        levelUpStep: Int,
        unlocked: Boolean,
    ): Int {
        if (!unlocked) return levels.first()
        val bounded = levels.lastOrNull { it <= progress }
        if (bounded != null) {
            if (progress <= levels.last()) return bounded
        }
        val last = levels.last()
        if (progress <= last) return last
        val extraSteps = (progress - last) / levelUpStep
        return last + (extraSteps * levelUpStep)
    }

    private fun resolveLevelTone(
        progress: Int,
        levels: List<Int>,
        unlocked: Boolean,
    ): BadgeLevelTone? {
        if (!unlocked) return null
        val shownValue = resolveLeveledValue(progress, levels, levelUpStep = 5, unlocked = true)
        return when {
            shownValue <= levels[0] -> BadgeLevelTone.BRONZE
            shownValue <= levels[1] -> BadgeLevelTone.SILVER
            shownValue <= levels[2] -> BadgeLevelTone.RED
            shownValue <= levels[3] -> BadgeLevelTone.ORIGINAL
            else -> BadgeLevelTone.PURPLE
        }
    }

    /** Roket rozetinde bir sonraki eşik (payda); [rocketLevel] ve [rocketLevelUp] ile uyumlu. */
    fun rocketProgressWindow(rawProgress: Int): BadgeProgressWindow {
        val levels = rocketLevel
        val levelUpStep = rocketLevelUp
        val safeProgress = rawProgress.coerceAtLeast(0)
        val first = levels.first()
        val last = levels.last()
        if (safeProgress < first) {
            return BadgeProgressWindow(current = safeProgress, target = first)
        }
        if (safeProgress < last) {
            val nextTarget = levels.firstOrNull { it > safeProgress } ?: last
            return BadgeProgressWindow(current = safeProgress, target = nextTarget)
        }
        val extraSteps = ((safeProgress - last) / levelUpStep) + 1
        val nextTarget = last + (extraSteps * levelUpStep)
        return BadgeProgressWindow(current = safeProgress, target = nextTarget)
    }

    fun rocketNextThreshold(rawProgress: Int): Int = rocketProgressWindow(rawProgress).target
}

object BadgeProgressRepository {
    private var currentUserProgress: UserBadgeProgress = UserBadgeProgress()
    private var currentStates: List<BadgeState> = BadgeProgressEngine.calculate(currentUserProgress)

    fun update(progress: UserBadgeProgress) {
        currentUserProgress = progress
        currentStates = BadgeProgressEngine.calculate(progress)
    }

    fun getUserBadgeProgress(): UserBadgeProgress = currentUserProgress

    fun getStates(): List<BadgeState> = currentStates

    fun getState(kind: BadgeKind): BadgeState? = currentStates.firstOrNull { it.kind == kind }

    fun getStateByMode(mode: BadgeFragment.BadgeAnimMode): BadgeState? {
        val kind = BadgeKind.fromMode(mode) ?: return null
        return getState(kind)
    }

    fun updateDartProgress(newProgress: Int) {
        currentUserProgress = currentUserProgress.copy(userDartProgress = newProgress.coerceAtLeast(0))
        currentStates = BadgeProgressEngine.calculate(currentUserProgress)
    }

    fun updateBowlingProgress(newProgress: Int) {
        currentUserProgress = currentUserProgress.copy(userBowlingProgress = newProgress.coerceAtLeast(0))
        currentStates = BadgeProgressEngine.calculate(currentUserProgress)
    }

    fun updateKarateProgress(newProgress: Int) {
        currentUserProgress = currentUserProgress.copy(userKarateProgress = newProgress.coerceAtLeast(0))
        currentStates = BadgeProgressEngine.calculate(currentUserProgress)
    }

    fun updateGolfProgress(newProgress: Int) {
        currentUserProgress = currentUserProgress.copy(userGolfProgress = newProgress.coerceAtLeast(0))
        currentStates = BadgeProgressEngine.calculate(currentUserProgress)
    }

    fun updateFishingSync(
        userFishingProgress: Int,
        userFishingStreak: Int,
        userFishingStreakPeriodKey: String,
    ) {
        currentUserProgress = currentUserProgress.copy(
            userFishingProgress = userFishingProgress.coerceAtLeast(0),
            userFishingStreak = userFishingStreak.coerceAtLeast(0),
            userFishingStreakPeriodKey = userFishingStreakPeriodKey,
        )
        currentStates = BadgeProgressEngine.calculate(currentUserProgress)
    }

    fun updateRocketSync(
        userRocketProgress: Int,
        userRocketDailyLessons: Int,
        userRocketDailyDayId: String,
    ) {
        currentUserProgress = currentUserProgress.copy(
            userRocketProgress = userRocketProgress.coerceAtLeast(0),
            userRocketDailyLessons = userRocketDailyLessons.coerceAtLeast(0),
            userRocketDailyDayId = userRocketDailyDayId,
        )
        currentStates = BadgeProgressEngine.calculate(currentUserProgress)
    }

    fun getProgressWindowByMode(mode: BadgeFragment.BadgeAnimMode): BadgeProgressWindow? {
        val kind = BadgeKind.fromMode(mode) ?: return null
        return when (kind) {
            BadgeKind.DART -> leveledProgressWindow(currentUserProgress.userDartProgress, listOf(3, 10, 20, 30, 50), 5)
            BadgeKind.FISHING -> {
                val streak = BadgeProgressEngine.effectiveFishingStreak(currentUserProgress)
                val target = BadgeProgressEngine.fishingNextThreshold(currentUserProgress.userFishingProgress)
                BadgeProgressWindow(current = streak, target = target)
            }
            BadgeKind.GOLF -> leveledProgressWindow(currentUserProgress.userGolfProgress, listOf(5, 10, 20, 50, 100), 5)
            BadgeKind.ROCKET -> {
                val today = MissionsProgressStore.calendarDayId()
                val daily = if (currentUserProgress.userRocketDailyDayId == today) {
                    currentUserProgress.userRocketDailyLessons
                } else {
                    0
                }
                val target = BadgeProgressEngine.rocketNextThreshold(currentUserProgress.userRocketProgress)
                BadgeProgressWindow(current = daily, target = target)
            }
            BadgeKind.BOWLING -> leveledProgressWindow(currentUserProgress.userBowlingProgress, listOf(5, 10, 20, 50, 100), 5)
            else -> null
        }
    }

    fun getProgressValueByMode(mode: BadgeFragment.BadgeAnimMode): Int? {
        return when (BadgeKind.fromMode(mode)) {
            BadgeKind.DART -> currentUserProgress.userDartProgress
            BadgeKind.FISHING -> currentUserProgress.userFishingProgress
            BadgeKind.GOLF -> currentUserProgress.userGolfProgress
            BadgeKind.ROCKET -> currentUserProgress.userRocketProgress
            BadgeKind.BOWLING -> currentUserProgress.userBowlingProgress
            BadgeKind.CUP -> currentUserProgress.cupPiece.minOfOrNull { it.rank }
            BadgeKind.KARATE -> currentUserProgress.userKarateProgress
            else -> null
        }
    }

    fun getLevelSpecByMode(mode: BadgeFragment.BadgeAnimMode): Pair<List<Int>, Int>? {
        return when (BadgeKind.fromMode(mode)) {
            BadgeKind.DART -> listOf(3, 10, 20, 30, 50) to 5
            BadgeKind.FISHING -> listOf(3, 5, 15, 25, 50) to 5
            BadgeKind.GOLF -> listOf(5, 10, 20, 50, 100) to 5
            BadgeKind.ROCKET -> listOf(3, 5, 10, 15, 25) to 5
            BadgeKind.BOWLING -> listOf(5, 10, 20, 50, 100) to 5
            else -> null
        }
    }

    /**
     * Kademeli rozetler için "ulaşılan kademe / toplam kademe" (ör. golf ham 20 → 3/5).
     * Son listedeki eşiği aşan ilerleme (ör. 110) en fazla n/n gösterilir (5/5); payda liste uzunluğuyla sabit kalır.
     */
    fun formatLeveledTierRatio(mode: BadgeFragment.BadgeAnimMode): String? {
        val spec = getLevelSpecByMode(mode) ?: return null
        val (levels, _) = spec
        val raw = getProgressValueByMode(mode)?.coerceAtLeast(0) ?: return null
        val n = levels.size
        val first = levels.first()
        val last = levels.last()
        val capped = minOf(raw, last)
        val numer = if (capped < first) 0 else levels.count { it <= capped }
        return "$numer/$n"
    }

    fun resolveShownValueByMode(mode: BadgeFragment.BadgeAnimMode, progress: Int): Int? {
        if (BadgeKind.fromMode(mode) == BadgeKind.KARATE) return progress.coerceAtLeast(0)
        val spec = getLevelSpecByMode(mode) ?: return null
        val (levels, step) = spec
        val safe = progress.coerceAtLeast(0)
        if (safe < levels.first()) return levels.first()
        if (safe <= levels.last()) return levels.lastOrNull { it <= safe } ?: levels.first()
        val last = levels.last()
        val extra = (safe - last) / step
        return last + (extra * step)
    }

    fun resolveToneByMode(mode: BadgeFragment.BadgeAnimMode, progress: Int): BadgeLevelTone? {
        if (BadgeKind.fromMode(mode) == BadgeKind.KARATE) return null
        val spec = getLevelSpecByMode(mode) ?: return null
        val (levels, _) = spec
        val safe = progress.coerceAtLeast(0)
        if (safe < levels.first()) return null
        val shown = resolveShownValueByMode(mode, safe) ?: return null
        return when {
            shown <= levels[0] -> BadgeLevelTone.BRONZE
            shown <= levels[1] -> BadgeLevelTone.SILVER
            shown <= levels[2] -> BadgeLevelTone.RED
            shown <= levels[3] -> BadgeLevelTone.ORIGINAL
            else -> BadgeLevelTone.PURPLE
        }
    }

    private fun leveledProgressWindow(progress: Int, levels: List<Int>, levelUpStep: Int): BadgeProgressWindow {
        val safeProgress = progress.coerceAtLeast(0)
        val first = levels.first()
        val last = levels.last()
        if (safeProgress < first) {
            return BadgeProgressWindow(current = safeProgress, target = first)
        }
        if (safeProgress < last) {
            val nextTarget = levels.firstOrNull { it > safeProgress } ?: last
            return BadgeProgressWindow(current = safeProgress, target = nextTarget)
        }
        val extraSteps = ((safeProgress - last) / levelUpStep) + 1
        val nextTarget = last + (extraSteps * levelUpStep)
        return BadgeProgressWindow(current = safeProgress, target = nextTarget)
    }
}

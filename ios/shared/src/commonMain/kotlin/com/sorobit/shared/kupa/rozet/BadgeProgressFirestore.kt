// Kaynak (Android): app/src/main/java/com/example/app/BadgeProgressFirestore.kt — saf kısım:
//  - BadgeLevelUpPayload, userBadgeProgressFromStateSnapshot, migrateLegacyFishingFields, payloadToQueueItem,
//    computeFishingStreakAfterSolve, resolveLevelUpPayload, resolveKarateFirstUnlockPayload,
//    resolveRocketLevelUpChain, resolveFishingLevelUpChain, resolve{Dino,Crocodile,Goat,Eagle,Fly,Turtle}LevelUpChain,
//    firestoreIntOrNull, pendingLeaderboardRewardSeasonFromDoc
//  - incrementBadgeProgressAndDetectLevelUp -> transaction gövdesi [computeIncrementBadgeProgressTransaction],
//    başarı dinleyicisi [finishIncrementBadgeProgress]
//  - sync{Dino,...}ProgressAndDetectLevelUp -> transaction gövdesi [computeCupBadgeProgressSync] + ilgili zincir
// Taşınmayanlar: Firestore/Auth çağrıları, openBadgeCelebration (FragmentManager), Log satırları.
// DocumentSnapshot yerine doküman verisi `Map<String, Any?>?` olarak alınır (null = doküman yok).
package com.sorobit.shared.kupa.rozet

import com.sorobit.shared.kupa.kupaCalendarDayId

data class BadgeLevelUpPayload(
    val mode: BadgeAnimMode,
    val fromProgress: Int,
    val toProgress: Int,
    val reachedTarget: Int,
)

/**
 * [BadgeProgressFirestore.computeIncrementBadgeProgressTransaction] sonucu: Android'de transaction'ın döndürdüğü
 * `arrayOf<Any>(...)` dizisinin adlandırılmış hâli + `transaction.set(docRef, updateMap, SetOptions.merge())`
 * ile yazılacak [updateMap].
 */
data class BadgeIncrementTransactionResult(
    /** `SetOptions.merge()` ile `users/{uid}/badgeProgress/state` dokümanına yazılacak alanlar (sayılar Long). */
    val updateMap: Map<String, Any>,
    val beforeDart: Int,
    val afterDart: Int,
    val beforeBowling: Int,
    val afterBowling: Int,
    val beforeKarate: Int,
    val afterKarate: Int,
    val beforeRocket: Int,
    val afterRocket: Int,
    val daily: Int,
    val dayId: String,
    val beforeGolf: Int,
    val afterGolf: Int,
    val beforeFishingTier: Int,
    val afterFishingTier: Int,
    val afterFishingStreak: Int,
    val afterFishingStreakPeriod: String,
    val beforeTornado: Int,
    val afterTornado: Int,
    val beforeVolcano: Int,
    val afterVolcano: Int,
)

/** Kupa skoru -> hayvan rozeti senkronu transaction sonucu. [updateMap] null ise yazma yapılmaz. */
data class CupBadgeSyncResult(
    val before: Int,
    val after: Int,
    val updateMap: Map<String, Any>?,
)

object BadgeProgressFirestore {

    // ---- Firestore yolu: users/{uid}/badgeProgress/state ----
    const val COLLECTION_USERS = "users"
    const val SUBCOLLECTION_BADGE_PROGRESS = "badgeProgress"
    const val DOCUMENT_STATE = "state"

    fun badgeStateDocumentPath(uid: String): String =
        "$COLLECTION_USERS/$uid/$SUBCOLLECTION_BADGE_PROGRESS/$DOCUMENT_STATE"

    // ---- Alan adları ----
    const val F_USER_DART_PROGRESS = "userDartProgress"
    const val F_USER_KARATE_PROGRESS = "userKarateProgress"
    const val F_USER_GOLF_PROGRESS = "userGolfProgress"
    const val F_USER_BOWLING_PROGRESS = "userBowlingProgress"
    const val F_USER_FISHING_PROGRESS = "userFishingProgress"
    const val F_USER_FISHING_STREAK = "userFishingStreak"
    const val F_USER_FISHING_STREAK_PERIOD_KEY = "userFishingStreakPeriodKey"
    const val F_USER_ROCKET_PROGRESS = "userRocketProgress"
    const val F_USER_ROCKET_DAILY_LESSONS = "userRocketDailyLessons"
    const val F_USER_ROCKET_DAILY_DAY_ID = "userRocketDailyDayId"
    const val F_USER_TORNADO_PROGRESS = "userTornadoProgress"
    const val F_USER_VOLCANO_PROGRESS = "userVolcanoProgress"
    const val F_USER_DINO_PROGRESS = "userDinoProgress"
    const val F_USER_CROCODILE_PROGRESS = "userCrocodileProgress"
    const val F_USER_TURTLE_PROGRESS = "userTurtleProgress"
    const val F_USER_GOAT_PROGRESS = "userGoatProgress"
    const val F_USER_EAGLE_PROGRESS = "userEagleProgress"
    const val F_USER_FLY_PROGRESS = "userFlyProgress"
    const val F_ABACUS_LEADERBOARD_RANK = "abacusLeaderboardRank"
    const val F_GOLD_MEDAL_PIECE = "goldMedalPiece"
    const val F_SILVER_MEDAL_PIECE = "silverMedalPiece"
    const val F_BRONZE_MEDAL_PIECE = "bronzeMedalPiece"
    const val F_CUP_PIECE = "cupPiece"
    const val F_PENDING_LEADERBOARD_REWARD_SEASON = "pendingLeaderboardRewardSeason"

    // Android: `(doc.get(x) as? Number)?.toInt()`
    private fun Map<String, Any?>.numInt(field: String): Int? = (this[field] as? Number)?.toInt()

    // Android: `snapshot.getLong(x)` (alan sayı değilse Android istisna atar; burada null döner).
    private fun Map<String, Any?>.getLong(field: String): Long? = (this[field] as? Number)?.toLong()

    // Android: `doc.getString(x)` (alan String değilse Android istisna atar; burada null döner).
    private fun Map<String, Any?>.getString(field: String): String? = this[field] as? String

    /** `users/{uid}/badgeProgress/state` dokümanından tam rozet durumu (profil ve sezon ödül kapısı için). */
    fun userBadgeProgressFromStateSnapshot(
        doc: Map<String, Any?>?,
        // Android: migrateLegacyFishingFields içinde DailyQuestionPeriod.currentPeriodKey().
        currentPeriodKey: String = FishingStreakPeriod.currentPeriodKey(),
    ): UserBadgeProgress {
        if (doc == null) return UserBadgeProgress()
        val abacusRank = doc.numInt(F_ABACUS_LEADERBOARD_RANK) ?: Int.MAX_VALUE
        val progress = UserBadgeProgress(
            userDartProgress = doc.numInt(F_USER_DART_PROGRESS) ?: 0,
            userKarateProgress = doc.numInt(F_USER_KARATE_PROGRESS) ?: 0,
            userGolfProgress = doc.numInt(F_USER_GOLF_PROGRESS) ?: 0,
            userBowlingProgress = doc.numInt(F_USER_BOWLING_PROGRESS) ?: 0,
            userFishingProgress = doc.numInt(F_USER_FISHING_PROGRESS) ?: 0,
            userFishingStreak = doc.numInt(F_USER_FISHING_STREAK) ?: 0,
            userFishingStreakPeriodKey = doc.getString(F_USER_FISHING_STREAK_PERIOD_KEY) ?: "",
            userRocketProgress = doc.numInt(F_USER_ROCKET_PROGRESS) ?: 0,
            userRocketDailyLessons = doc.numInt(F_USER_ROCKET_DAILY_LESSONS) ?: 0,
            userRocketDailyDayId = doc.getString(F_USER_ROCKET_DAILY_DAY_ID) ?: "",
            userTornadoProgress = doc.numInt(F_USER_TORNADO_PROGRESS) ?: 0,
            userVolcanoProgress = doc.numInt(F_USER_VOLCANO_PROGRESS) ?: 0,
            userDinoProgress = doc.numInt(F_USER_DINO_PROGRESS) ?: 0,
            userCrocodileProgress = doc.numInt(F_USER_CROCODILE_PROGRESS) ?: 0,
            userTurtleProgress = doc.numInt(F_USER_TURTLE_PROGRESS) ?: 0,
            userGoatProgress = doc.numInt(F_USER_GOAT_PROGRESS) ?: 0,
            userEagleProgress = doc.numInt(F_USER_EAGLE_PROGRESS) ?: 0,
            userFlyProgress = doc.numInt(F_USER_FLY_PROGRESS) ?: 0,
            abacusLeaderboardRank = abacusRank,
            goldMedalPiece = BadgePieceLeaderboardSync.parseMedalPieceList(doc[F_GOLD_MEDAL_PIECE]),
            silverMedalPiece = BadgePieceLeaderboardSync.parseMedalPieceList(doc[F_SILVER_MEDAL_PIECE]),
            bronzeMedalPiece = BadgePieceLeaderboardSync.parseMedalPieceList(doc[F_BRONZE_MEDAL_PIECE]),
            cupPiece = BadgePieceLeaderboardSync.parseCupPieceList(doc[F_CUP_PIECE]),
            pendingLeaderboardRewardSeason = pendingLeaderboardRewardSeasonFromDoc(doc),
        )
        return migrateLegacyFishingFields(doc, progress, currentPeriodKey)
    }

    /** Eski şema: [UserBadgeProgress.userFishingProgress] seri sayısıydı; tier + userFishingStreak ayrımına geçiş. */
    fun migrateLegacyFishingFields(
        doc: Map<String, Any?>?,
        progress: UserBadgeProgress,
        currentPeriodKey: String = FishingStreakPeriod.currentPeriodKey(),
    ): UserBadgeProgress {
        // Android: `doc.contains("userFishingStreak")` (doküman yoksa false).
        if (doc != null && doc.containsKey(F_USER_FISHING_STREAK)) return progress
        val legacyStreak = progress.userFishingProgress.coerceAtLeast(0)
        var tier = 0
        var streakCursor = legacyStreak
        while (streakCursor >= BadgeProgressEngine.fishingNextThreshold(tier)) {
            val next = BadgeProgressEngine.fishingNextThreshold(tier)
            if (next <= tier) break
            tier = next
        }
        val streak = BadgeProgressEngine.effectiveFishingStreak(
            progress.copy(
                userFishingProgress = tier,
                userFishingStreak = legacyStreak,
            ),
            currentPeriodKey,
        )
        return progress.copy(
            userFishingProgress = tier,
            userFishingStreak = streak,
        )
    }

    fun payloadToQueueItem(payload: BadgeLevelUpPayload): String =
        "${payload.mode.name}|${payload.fromProgress}|${payload.toProgress}|${payload.reachedTarget}"

    /**
     * Günlük soru serisi: aynı periyotta tekrar çözüm sayılmaz; periyot atlanırsa 1’den başlar.
     */
    fun computeFishingStreakAfterSolve(
        storedStreak: Int,
        lastStreakPeriodKey: String,
        currentPeriodKey: String,
    ): Pair<Int, String> {
        if (FishingStreakPeriod.isSamePeriod(lastStreakPeriodKey, currentPeriodKey)) {
            return storedStreak to lastStreakPeriodKey
        }
        val after = when {
            lastStreakPeriodKey.isEmpty() -> 1
            FishingStreakPeriod.isFishingStreakBroken(lastStreakPeriodKey, currentPeriodKey) -> 1
            FishingStreakPeriod.isConsecutivePeriodAfter(lastStreakPeriodKey, currentPeriodKey) ->
                storedStreak + 1

            else -> 1
        }
        return after to currentPeriodKey
    }

    fun resolveLevelUpPayload(
        mode: BadgeAnimMode,
        before: Int,
        after: Int,
        levels: List<Int>,
    ): BadgeLevelUpPayload? {
        val nextTargetBefore = levels.firstOrNull { it > before } ?: return null
        if (nextTargetBefore > levels.last()) return null
        if (after < nextTargetBefore) return null
        return BadgeLevelUpPayload(
            mode = mode,
            fromProgress = before,
            toProgress = after,
            reachedTarget = nextTargetBefore,
        )
    }

    fun resolveKarateFirstUnlockPayload(before: Int, after: Int): BadgeLevelUpPayload? {
        if (before != 0 || after != 1) return null
        return BadgeLevelUpPayload(
            mode = BadgeAnimMode.KARATE,
            fromProgress = before,
            toProgress = after,
            reachedTarget = 1,
        )
    }

    fun resolveRocketLevelUpChain(
        beforeRocket: Int,
        afterRocket: Int
    ): List<BadgeLevelUpPayload> {
        if (afterRocket <= beforeRocket) return emptyList()
        val payloads = mutableListOf<BadgeLevelUpPayload>()
        var cur = beforeRocket
        while (cur < afterRocket) {
            val next = BadgeProgressEngine.rocketNextThreshold(cur)
            if (next <= cur) break
            payloads.add(
                BadgeLevelUpPayload(
                    mode = BadgeAnimMode.ROCKET_WITH_BASE,
                    fromProgress = cur,
                    toProgress = next,
                    reachedTarget = next,
                ),
            )
            cur = next
        }
        return payloads
    }

    fun resolveFishingLevelUpChain(
        beforeTier: Int,
        afterTier: Int
    ): List<BadgeLevelUpPayload> {
        if (afterTier <= beforeTier) return emptyList()
        val payloads = mutableListOf<BadgeLevelUpPayload>()
        var cur = beforeTier
        while (cur < afterTier) {
            val next = BadgeProgressEngine.fishingNextThreshold(cur)
            if (next <= cur) break
            payloads.add(
                BadgeLevelUpPayload(
                    mode = BadgeAnimMode.FISHING_WITH_BASE,
                    fromProgress = cur,
                    toProgress = next,
                    reachedTarget = next,
                ),
            )
            cur = next
        }
        return payloads
    }

    /**
     * Android `incrementBadgeProgressAndDetectLevelUp` içindeki `runTransaction { ... }` gövdesi.
     *
     * @param snapshot `users/{uid}/badgeProgress/state` doküman verisi (doküman yoksa null).
     * @param dailyQuestionPeriodKey Oturum periyodu; null/boş ise [currentPeriodKey] kullanılır.
     * @param today Android `MissionsProgressStore.calendarDayId()`.
     * @param currentPeriodKey Android `DailyQuestionPeriod.currentPeriodKey()` ("şimdi").
     */
    fun computeIncrementBadgeProgressTransaction(
        snapshot: Map<String, Any?>?,
        incrementDart: Boolean,
        incrementBowlingBy: Int,
        incrementKarate: Boolean,
        incrementRocketDailyLessons: Boolean = false,
        incrementGolf: Boolean = false,
        incrementFishing: Boolean = false,
        incrementTornado: Boolean = false,
        incrementVolcano: Boolean = false,
        dailyQuestionPeriodKey: String? = null,
        today: String = kupaCalendarDayId(),
        currentPeriodKey: String = FishingStreakPeriod.currentPeriodKey(),
    ): BadgeIncrementTransactionResult {
        val snap: Map<String, Any?> = snapshot ?: emptyMap()
        val beforeDart = (snap.getLong(F_USER_DART_PROGRESS) ?: 0L).toInt().coerceAtLeast(0)
        val beforeBowling =
            (snap.getLong(F_USER_BOWLING_PROGRESS) ?: 0L).toInt().coerceAtLeast(0)
        val beforeKarate =
            (snap.getLong(F_USER_KARATE_PROGRESS) ?: 0L).toInt().coerceAtLeast(0)
        val beforeRocket =
            (snap.getLong(F_USER_ROCKET_PROGRESS) ?: 0L).toInt().coerceAtLeast(0)
        val beforeGolf = (snap.getLong(F_USER_GOLF_PROGRESS) ?: 0L).toInt().coerceAtLeast(0)
        val beforeTornado =
            (snap.getLong(F_USER_TORNADO_PROGRESS) ?: 0L).toInt().coerceAtLeast(0)
        val beforeVolcano =
            (snap.getLong(F_USER_VOLCANO_PROGRESS) ?: 0L).toInt().coerceAtLeast(0)
        val beforeFishingTier =
            (snap.getLong(F_USER_FISHING_PROGRESS) ?: 0L).toInt().coerceAtLeast(0)
        val beforeFishingStreakStored = if (snap.containsKey(F_USER_FISHING_STREAK)) {
            (snap.getLong(F_USER_FISHING_STREAK) ?: 0L).toInt().coerceAtLeast(0)
        } else {
            migrateLegacyFishingFields(
                snap,
                UserBadgeProgress(
                    userFishingProgress = beforeFishingTier,
                    userFishingStreakPeriodKey = snap.getString(F_USER_FISHING_STREAK_PERIOD_KEY)
                        ?: "",
                ),
                currentPeriodKey,
            ).userFishingStreak
        }
        val lastFishingStreakPeriod = snap.getString(F_USER_FISHING_STREAK_PERIOD_KEY) ?: ""
        val fishingPeriodKey = dailyQuestionPeriodKey?.takeIf { it.isNotEmpty() }
            ?: currentPeriodKey
        var daily = (snap.getLong(F_USER_ROCKET_DAILY_LESSONS) ?: 0L).toInt().coerceAtLeast(0)
        var dayId = snap.getString(F_USER_ROCKET_DAILY_DAY_ID) ?: ""
        if (dayId != today) {
            daily = 0
            dayId = today
        }
        val afterDart = if (incrementDart) beforeDart + 1 else beforeDart
        val afterBowling =
            if (incrementBowlingBy > 0) beforeBowling + incrementBowlingBy else beforeBowling
        val afterKarate = if (incrementKarate) beforeKarate + 1 else beforeKarate
        val afterGolf = if (incrementGolf) beforeGolf + 1 else beforeGolf
        val afterTornado = if (incrementTornado) beforeTornado + 1 else beforeTornado
        val afterVolcano = if (incrementVolcano) beforeVolcano + 1 else beforeVolcano
        val afterFishingStreak: Int
        val afterFishingStreakPeriod: String
        if (incrementFishing) {
            val solved = computeFishingStreakAfterSolve(
                storedStreak = beforeFishingStreakStored,
                lastStreakPeriodKey = lastFishingStreakPeriod,
                currentPeriodKey = fishingPeriodKey,
            )
            afterFishingStreak = solved.first
            afterFishingStreakPeriod = solved.second
        } else {
            afterFishingStreak = beforeFishingStreakStored
            afterFishingStreakPeriod = lastFishingStreakPeriod
        }
        var afterFishingTier = beforeFishingTier
        if (incrementFishing) {
            while (afterFishingStreak >= BadgeProgressEngine.fishingNextThreshold(
                    afterFishingTier
                )
            ) {
                val next = BadgeProgressEngine.fishingNextThreshold(afterFishingTier)
                if (next <= afterFishingTier) break
                afterFishingTier = next
            }
        }
        if (incrementRocketDailyLessons) {
            daily++
        }
        var afterRocket = beforeRocket
        while (daily >= BadgeProgressEngine.rocketNextThreshold(afterRocket)) {
            val next = BadgeProgressEngine.rocketNextThreshold(afterRocket)
            if (next <= afterRocket) break
            afterRocket = next
        }
        val updateMap = mutableMapOf<String, Any>(
            F_USER_ROCKET_PROGRESS to afterRocket.toLong(),
            F_USER_ROCKET_DAILY_LESSONS to daily.toLong(),
            F_USER_ROCKET_DAILY_DAY_ID to dayId,
        )
        if (incrementDart) updateMap[F_USER_DART_PROGRESS] = afterDart.toLong()
        if (incrementBowlingBy > 0) updateMap[F_USER_BOWLING_PROGRESS] = afterBowling.toLong()
        if (incrementKarate) updateMap[F_USER_KARATE_PROGRESS] = afterKarate.toLong()
        if (incrementGolf) updateMap[F_USER_GOLF_PROGRESS] = afterGolf.toLong()
        if (incrementTornado) updateMap[F_USER_TORNADO_PROGRESS] = afterTornado.toLong()
        if (incrementVolcano) updateMap[F_USER_VOLCANO_PROGRESS] = afterVolcano.toLong()
        if (incrementFishing) {
            updateMap[F_USER_FISHING_PROGRESS] = afterFishingTier.toLong()
            updateMap[F_USER_FISHING_STREAK] = afterFishingStreak.toLong()
            updateMap[F_USER_FISHING_STREAK_PERIOD_KEY] = afterFishingStreakPeriod
        }
        return BadgeIncrementTransactionResult(
            updateMap = updateMap,
            beforeDart = beforeDart,
            afterDart = afterDart,
            beforeBowling = beforeBowling,
            afterBowling = afterBowling,
            beforeKarate = beforeKarate,
            afterKarate = afterKarate,
            beforeRocket = beforeRocket,
            afterRocket = afterRocket,
            daily = daily,
            dayId = dayId,
            beforeGolf = beforeGolf,
            afterGolf = afterGolf,
            beforeFishingTier = beforeFishingTier,
            afterFishingTier = afterFishingTier,
            afterFishingStreak = afterFishingStreak,
            afterFishingStreakPeriod = afterFishingStreakPeriod,
            beforeTornado = beforeTornado,
            afterTornado = afterTornado,
            beforeVolcano = beforeVolcano,
            afterVolcano = afterVolcano,
        )
    }

    /**
     * Android `incrementBadgeProgressAndDetectLevelUp` içindeki `addOnSuccessListener { values -> ... }` gövdesi:
     * [BadgeProgressRepository] bellek durumunu günceller ve kutlama payload listesini (Android ile aynı sırada)
     * döndürür. Bayraklar transaction'a verilenlerle aynı olmalıdır.
     * (Android'de hata/oturum yok durumunda `onDone(emptyList())` çağrılır; o kısım platform katmanındadır.)
     */
    fun finishIncrementBadgeProgress(
        result: BadgeIncrementTransactionResult,
        incrementDart: Boolean,
        incrementBowlingBy: Int,
        incrementKarate: Boolean,
        incrementGolf: Boolean = false,
        incrementFishing: Boolean = false,
        incrementTornado: Boolean = false,
        incrementVolcano: Boolean = false,
    ): List<BadgeLevelUpPayload> {
        if (incrementDart) BadgeProgressRepository.updateDartProgress(result.afterDart)
        if (incrementBowlingBy > 0) BadgeProgressRepository.updateBowlingProgress(result.afterBowling)
        if (incrementKarate) BadgeProgressRepository.updateKarateProgress(result.afterKarate)
        if (incrementGolf) BadgeProgressRepository.updateGolfProgress(result.afterGolf)
        if (incrementTornado) BadgeProgressRepository.updateTornadoProgress(result.afterTornado)
        if (incrementVolcano) BadgeProgressRepository.updateVolcanoProgress(result.afterVolcano)
        if (incrementFishing) {
            BadgeProgressRepository.updateFishingSync(
                userFishingProgress = result.afterFishingTier,
                userFishingStreak = result.afterFishingStreak,
                userFishingStreakPeriodKey = result.afterFishingStreakPeriod,
            )
        }
        BadgeProgressRepository.updateRocketSync(result.afterRocket, result.daily, result.dayId)
        val payloads = mutableListOf<BadgeLevelUpPayload>()
        if (incrementBowlingBy > 0) {
            resolveLevelUpPayload(
                BadgeAnimMode.BOWLING_WITH_BASE,
                result.beforeBowling,
                result.afterBowling,
                listOf(5, 10, 20, 50, 100),
            )?.let { payloads.add(it) }
        }
        if (incrementDart) {
            resolveLevelUpPayload(
                BadgeAnimMode.DAILY_ONLY,
                result.beforeDart,
                result.afterDart,
                listOf(3, 10, 20, 30, 50),
            )?.let { payloads.add(it) }
        }
        if (incrementKarate) {
            resolveKarateFirstUnlockPayload(result.beforeKarate, result.afterKarate)?.let { payloads.add(it) }
        }
        payloads.addAll(resolveRocketLevelUpChain(result.beforeRocket, result.afterRocket))
        if (incrementGolf) {
            resolveLevelUpPayload(
                BadgeAnimMode.GOLF_WITH_BASE,
                result.beforeGolf,
                result.afterGolf,
                listOf(5, 10, 20, 50, 100),
            )?.let { payloads.add(it) }
        }
        if (incrementFishing) {
            payloads.addAll(resolveFishingLevelUpChain(result.beforeFishingTier, result.afterFishingTier))
        }
        if (incrementTornado) {
            resolveLevelUpPayload(
                BadgeAnimMode.TORNADO,
                result.beforeTornado,
                result.afterTornado,
                listOf(1, 3, 5, 10, 15),
            )?.let { payloads.add(it) }
        }
        if (incrementVolcano) {
            resolveLevelUpPayload(
                BadgeAnimMode.VOLCANO,
                result.beforeVolcano,
                result.afterVolcano,
                listOf(1, 3, 5, 10, 15),
            )?.let { payloads.add(it) }
        }
        return payloads
    }

    // Android'de altı kupa rozeti için birebir aynı gövdeyle altı ayrı fonksiyon vardır; ortak gövde burada.
    private inline fun resolveCupLevelUpChain(
        mode: BadgeAnimMode,
        before: Int,
        after: Int,
        nextThreshold: (Int) -> Int,
    ): List<BadgeLevelUpPayload> {
        if (after <= before) return emptyList()
        val payloads = mutableListOf<BadgeLevelUpPayload>()
        var cur = before
        while (cur < after) {
            val nextTarget = nextThreshold(cur)
            if (after >= nextTarget) {
                payloads.add(
                    BadgeLevelUpPayload(
                        mode = mode,
                        fromProgress = cur,
                        toProgress = nextTarget,
                        reachedTarget = nextTarget,
                    )
                )
                cur = nextTarget
            } else {
                break
            }
        }
        return payloads
    }

    fun resolveDinoLevelUpChain(beforeDino: Int, afterDino: Int): List<BadgeLevelUpPayload> =
        resolveCupLevelUpChain(BadgeAnimMode.DINO, beforeDino, afterDino) { BadgeProgressEngine.dinoNextThreshold(it) }

    fun resolveCrocodileLevelUpChain(beforeCrocodile: Int, afterCrocodile: Int): List<BadgeLevelUpPayload> =
        resolveCupLevelUpChain(BadgeAnimMode.CROCODILE, beforeCrocodile, afterCrocodile) {
            BadgeProgressEngine.crocodileNextThreshold(it)
        }

    fun resolveGoatLevelUpChain(beforeGoat: Int, afterGoat: Int): List<BadgeLevelUpPayload> =
        resolveCupLevelUpChain(BadgeAnimMode.GOAT, beforeGoat, afterGoat) { BadgeProgressEngine.goatNextThreshold(it) }

    fun resolveEagleLevelUpChain(beforeEagle: Int, afterEagle: Int): List<BadgeLevelUpPayload> =
        resolveCupLevelUpChain(BadgeAnimMode.EAGLE, beforeEagle, afterEagle) { BadgeProgressEngine.eagleNextThreshold(it) }

    fun resolveFlyLevelUpChain(beforeFly: Int, afterFly: Int): List<BadgeLevelUpPayload> =
        resolveCupLevelUpChain(BadgeAnimMode.FLY, beforeFly, afterFly) { BadgeProgressEngine.flyNextThreshold(it) }

    fun resolveTurtleLevelUpChain(beforeTurtle: Int, afterTurtle: Int): List<BadgeLevelUpPayload> =
        resolveCupLevelUpChain(BadgeAnimMode.TURTLE, beforeTurtle, afterTurtle) {
            BadgeProgressEngine.turtleNextThreshold(it)
        }

    /**
     * Android `sync{Dino,Crocodile,Goat,Eagle,Fly,Turtle}ProgressAndDetectLevelUp` içindeki transaction gövdesi:
     * yeni kupa skoru kayıtlı rozet ilerlemesinden BÜYÜKSE [field] alanına (Long) yazılır, değilse yazma yok.
     *
     * @param field [F_USER_DINO_PROGRESS] vb.
     */
    fun computeCupBadgeProgressSync(
        field: String,
        snapshot: Map<String, Any?>?,
        newCupScore: Int,
    ): CupBadgeSyncResult {
        val before = ((snapshot ?: emptyMap()).getLong(field) ?: 0L).toInt().coerceAtLeast(0)
        return if (newCupScore > before) {
            CupBadgeSyncResult(before, newCupScore, mapOf(field to newCupScore.toLong()))
        } else {
            CupBadgeSyncResult(before, before, null)
        }
    }

    /** Kupa rozeti modu -> `badgeProgress/state` alan adı; kupa rozeti değilse null. */
    fun cupBadgeProgressField(mode: BadgeAnimMode): String? = when (mode) {
        BadgeAnimMode.DINO -> F_USER_DINO_PROGRESS
        BadgeAnimMode.CROCODILE -> F_USER_CROCODILE_PROGRESS
        BadgeAnimMode.GOAT -> F_USER_GOAT_PROGRESS
        BadgeAnimMode.EAGLE -> F_USER_EAGLE_PROGRESS
        BadgeAnimMode.FLY -> F_USER_FLY_PROGRESS
        BadgeAnimMode.TURTLE -> F_USER_TURTLE_PROGRESS
        else -> null
    }

    /** Kupa rozeti moduna göre ilgili `resolve*LevelUpChain`; kupa rozeti değilse boş liste. */
    fun resolveCupBadgeLevelUpChain(mode: BadgeAnimMode, before: Int, after: Int): List<BadgeLevelUpPayload> =
        when (mode) {
            BadgeAnimMode.DINO -> resolveDinoLevelUpChain(before, after)
            BadgeAnimMode.CROCODILE -> resolveCrocodileLevelUpChain(before, after)
            BadgeAnimMode.GOAT -> resolveGoatLevelUpChain(before, after)
            BadgeAnimMode.EAGLE -> resolveEagleLevelUpChain(before, after)
            BadgeAnimMode.FLY -> resolveFlyLevelUpChain(before, after)
            BadgeAnimMode.TURTLE -> resolveTurtleLevelUpChain(before, after)
            else -> emptyList()
        }

    /**
     * Android `DocumentSnapshot.pendingLeaderboardRewardSeasonFromDoc()`.
     * (Android önce `get(...)`, sonra `data?.get(...)` dener; ikisi de aynı haritayı okuduğundan tek okuma yeterli.)
     */
    fun pendingLeaderboardRewardSeasonFromDoc(doc: Map<String, Any?>?): Int? {
        if (doc == null) return null
        return doc[F_PENDING_LEADERBOARD_REWARD_SEASON].firestoreIntOrNull()
    }
}

/**
 * Firestore sayılarını Int'e çevirir (Android'deki aynı adlı uzantı).
 * Android'deki son dal `java.lang.Number.doubleValue().toInt()` idi; ortak kodda `Number.toDouble().toInt()`.
 */
fun Any?.firestoreIntOrNull(): Int? = when (this) {
    null -> null
    is Int -> this
    is Long -> this.toInt()
    is Short -> this.toInt()
    is Byte -> this.toInt()
    is Double -> if (this.isFinite()) this.toInt() else null
    is Float -> this.toInt()
    is String -> this.trim().toIntOrNull()
    else -> (this as? Number)?.toDouble()?.toInt()
}

package com.example.app

import android.util.Log
import androidx.fragment.app.FragmentManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

data class BadgeLevelUpPayload(
    val mode: BadgeFragment.BadgeAnimMode,
    val fromProgress: Int,
    val toProgress: Int,
    val reachedTarget: Int,
)

object BadgeProgressFirestore {
    private const val TAG = "BadgeProgressFirestore"

    /** `users/{uid}/badgeProgress/state` dokümanından tam rozet durumu (profil ve sezon ödül kapısı için). */
    fun userBadgeProgressFromStateSnapshot(doc: DocumentSnapshot): UserBadgeProgress {
        if (!doc.exists()) return UserBadgeProgress()
        val abacusRank = (doc.get("abacusLeaderboardRank") as? Number)?.toInt() ?: Int.MAX_VALUE
        val progress = UserBadgeProgress(
            userDartProgress = (doc.get("userDartProgress") as? Number)?.toInt() ?: 0,
            userKarateProgress = (doc.get("userKarateProgress") as? Number)?.toInt() ?: 0,
            userGolfProgress = (doc.get("userGolfProgress") as? Number)?.toInt() ?: 0,
            userBowlingProgress = (doc.get("userBowlingProgress") as? Number)?.toInt() ?: 0,
            userFishingProgress = (doc.get("userFishingProgress") as? Number)?.toInt() ?: 0,
            userFishingStreak = (doc.get("userFishingStreak") as? Number)?.toInt() ?: 0,
            userFishingStreakPeriodKey = doc.getString("userFishingStreakPeriodKey") ?: "",
            userRocketProgress = (doc.get("userRocketProgress") as? Number)?.toInt() ?: 0,
            userRocketDailyLessons = (doc.get("userRocketDailyLessons") as? Number)?.toInt() ?: 0,
            userRocketDailyDayId = doc.getString("userRocketDailyDayId") ?: "",
            abacusLeaderboardRank = abacusRank,
            goldMedalPiece = BadgePieceLeaderboardSync.parseMedalPieceList(doc.get("goldMedalPiece")),
            silverMedalPiece = BadgePieceLeaderboardSync.parseMedalPieceList(doc.get("silverMedalPiece")),
            bronzeMedalPiece = BadgePieceLeaderboardSync.parseMedalPieceList(doc.get("bronzeMedalPiece")),
            cupPiece = BadgePieceLeaderboardSync.parseCupPieceList(doc.get("cupPiece")),
            pendingLeaderboardRewardSeason = doc.pendingLeaderboardRewardSeasonFromDoc(),
        )
        return migrateLegacyFishingFields(doc, progress)
    }

    /** Eski şema: [userFishingProgress] seri sayısıydı; tier + [userFishingStreak] ayrımına geçiş. */
    private fun migrateLegacyFishingFields(doc: DocumentSnapshot, progress: UserBadgeProgress): UserBadgeProgress {
        if (doc.contains("userFishingStreak")) return progress
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
        )
        return progress.copy(
            userFishingProgress = tier,
            userFishingStreak = streak,
        )
    }

    fun payloadToQueueItem(payload: BadgeLevelUpPayload): String =
        "${payload.mode.name}|${payload.fromProgress}|${payload.toProgress}|${payload.reachedTarget}"

    fun openBadgeCelebration(
        fm: FragmentManager,
        payloads: List<BadgeLevelUpPayload>,
        seasonLeaderboardAckAfterQueue: Int? = null,
    ) {
        if (payloads.isEmpty()) return
        fm.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right,
            )
            .replace(
                R.id.badgeFragmentContainter,
                BadgeFragment.newLevelUpSequenceInstance(
                    payloads.map { payloadToQueueItem(it) },
                    0,
                    seasonLeaderboardAckAfterQueue,
                ),
            )
            .commit()
    }

    /**
     * Günlük soru serisi: aynı periyotta tekrar çözüm sayılmaz; periyot atlanırsa 1’den başlar.
     */
    private fun computeFishingStreakAfterSolve(
        storedStreak: Int,
        lastStreakPeriodKey: String,
        currentPeriodKey: String,
    ): Pair<Int, String> {
        if (DailyQuestionPeriod.isSamePeriod(lastStreakPeriodKey, currentPeriodKey)) {
            return storedStreak to lastStreakPeriodKey
        }
        val after = when {
            lastStreakPeriodKey.isEmpty() -> 1
            DailyQuestionPeriod.isFishingStreakBroken(lastStreakPeriodKey, currentPeriodKey) -> 1
            DailyQuestionPeriod.isConsecutivePeriodAfter(lastStreakPeriodKey, currentPeriodKey) ->
                storedStreak + 1
            else -> 1
        }
        return after to currentPeriodKey
    }

    private fun resolveLevelUpPayload(
        mode: BadgeFragment.BadgeAnimMode,
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

    private fun resolveKarateFirstUnlockPayload(before: Int, after: Int): BadgeLevelUpPayload? {
        if (before != 0 || after != 1) return null
        return BadgeLevelUpPayload(
            mode = BadgeFragment.BadgeAnimMode.KARATE,
            fromProgress = before,
            toProgress = after,
            reachedTarget = 1,
        )
    }

    private fun resolveRocketLevelUpChain(beforeRocket: Int, afterRocket: Int): List<BadgeLevelUpPayload> {
        if (afterRocket <= beforeRocket) return emptyList()
        val payloads = mutableListOf<BadgeLevelUpPayload>()
        var cur = beforeRocket
        while (cur < afterRocket) {
            val next = BadgeProgressEngine.rocketNextThreshold(cur)
            if (next <= cur) break
            payloads.add(
                BadgeLevelUpPayload(
                    mode = BadgeFragment.BadgeAnimMode.ROCKET_WITH_BASE,
                    fromProgress = cur,
                    toProgress = next,
                    reachedTarget = next,
                ),
            )
            cur = next
        }
        return payloads
    }

    private fun resolveFishingLevelUpChain(beforeTier: Int, afterTier: Int): List<BadgeLevelUpPayload> {
        if (afterTier <= beforeTier) return emptyList()
        val payloads = mutableListOf<BadgeLevelUpPayload>()
        var cur = beforeTier
        while (cur < afterTier) {
            val next = BadgeProgressEngine.fishingNextThreshold(cur)
            if (next <= cur) break
            payloads.add(
                BadgeLevelUpPayload(
                    mode = BadgeFragment.BadgeAnimMode.FISHING_WITH_BASE,
                    fromProgress = cur,
                    toProgress = next,
                    reachedTarget = next,
                ),
            )
            cur = next
        }
        return payloads
    }

    fun incrementBadgeProgressAndDetectLevelUp(
        incrementDart: Boolean,
        incrementBowlingBy: Int,
        incrementKarate: Boolean,
        /** [ChestFragment.updateMapProgress] içinde `recordStepIncrementProgress` ile aynı koşul (`shouldIncrementStepCountMission`). */
        incrementRocketDailyLessons: Boolean = false,
        /** Öğrenci [CreateQuestionFragment] ile soru gönderince +1. */
        incrementGolf: Boolean = false,
        /** Günlük soru doğru: art arda periyot serisi (+1); periyot atlanırsa seri sıfırlanıp 1’den başlar. */
        incrementFishing: Boolean = false,
        /** [BlindingLessonFragment] oturum periyodu; yoksa [DailyQuestionPeriod.currentPeriodKey]. */
        dailyQuestionPeriodKey: String? = null,
        onDone: (List<BadgeLevelUpPayload>) -> Unit,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onDone(emptyList())
            return
        }
        val docRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("badgeProgress")
            .document("state")

        FirebaseFirestore.getInstance().runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val beforeDart = (snapshot.getLong("userDartProgress") ?: 0L).toInt().coerceAtLeast(0)
            val beforeBowling = (snapshot.getLong("userBowlingProgress") ?: 0L).toInt().coerceAtLeast(0)
            val beforeKarate = (snapshot.getLong("userKarateProgress") ?: 0L).toInt().coerceAtLeast(0)
            val beforeRocket = (snapshot.getLong("userRocketProgress") ?: 0L).toInt().coerceAtLeast(0)
            val beforeGolf = (snapshot.getLong("userGolfProgress") ?: 0L).toInt().coerceAtLeast(0)
            val beforeFishingTier = (snapshot.getLong("userFishingProgress") ?: 0L).toInt().coerceAtLeast(0)
            val beforeFishingStreakStored = if (snapshot.contains("userFishingStreak")) {
                (snapshot.getLong("userFishingStreak") ?: 0L).toInt().coerceAtLeast(0)
            } else {
                migrateLegacyFishingFields(
                    snapshot,
                    UserBadgeProgress(
                        userFishingProgress = beforeFishingTier,
                        userFishingStreakPeriodKey = snapshot.getString("userFishingStreakPeriodKey") ?: "",
                    ),
                ).userFishingStreak
            }
            val lastFishingStreakPeriod = snapshot.getString("userFishingStreakPeriodKey") ?: ""
            val fishingPeriodKey = dailyQuestionPeriodKey?.takeIf { it.isNotEmpty() }
                ?: DailyQuestionPeriod.currentPeriodKey()
            val today = MissionsProgressStore.calendarDayId()
            var daily = (snapshot.getLong("userRocketDailyLessons") ?: 0L).toInt().coerceAtLeast(0)
            var dayId = snapshot.getString("userRocketDailyDayId") ?: ""
            if (dayId != today) {
                daily = 0
                dayId = today
            }
            val afterDart = if (incrementDart) beforeDart + 1 else beforeDart
            val afterBowling = if (incrementBowlingBy > 0) beforeBowling + incrementBowlingBy else beforeBowling
            val afterKarate = if (incrementKarate) beforeKarate + 1 else beforeKarate
            val afterGolf = if (incrementGolf) beforeGolf + 1 else beforeGolf
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
                while (afterFishingStreak >= BadgeProgressEngine.fishingNextThreshold(afterFishingTier)) {
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
            val updateMap = hashMapOf<String, Any>(
                "userRocketProgress" to afterRocket.toLong(),
                "userRocketDailyLessons" to daily.toLong(),
                "userRocketDailyDayId" to dayId,
            )
            if (incrementDart) updateMap["userDartProgress"] = afterDart.toLong()
            if (incrementBowlingBy > 0) updateMap["userBowlingProgress"] = afterBowling.toLong()
            if (incrementKarate) updateMap["userKarateProgress"] = afterKarate.toLong()
            if (incrementGolf) updateMap["userGolfProgress"] = afterGolf.toLong()
            if (incrementFishing) {
                updateMap["userFishingProgress"] = afterFishingTier.toLong()
                updateMap["userFishingStreak"] = afterFishingStreak.toLong()
                updateMap["userFishingStreakPeriodKey"] = afterFishingStreakPeriod
            }
            transaction.set(docRef, updateMap, SetOptions.merge())
            arrayOf<Any>(
                beforeDart,
                afterDart,
                beforeBowling,
                afterBowling,
                beforeKarate,
                afterKarate,
                beforeRocket,
                afterRocket,
                daily,
                dayId,
                beforeGolf,
                afterGolf,
                beforeFishingTier,
                afterFishingTier,
                afterFishingStreak,
                afterFishingStreakPeriod,
            )
        }.addOnSuccessListener { values ->
            fun num(i: Int) = (values[i] as Number).toInt()
            val beforeDart = num(0)
            val afterDart = num(1)
            val beforeBowling = num(2)
            val afterBowling = num(3)
            val beforeKarate = num(4)
            val afterKarate = num(5)
            val beforeRocket = num(6)
            val afterRocket = num(7)
            val finalDaily = num(8)
            val finalDayId = values[9] as String
            val beforeGolf = num(10)
            val afterGolf = num(11)
            val beforeFishingTier = num(12)
            val afterFishingTier = num(13)
            val afterFishingStreak = num(14)
            val afterFishingStreakPeriod = values[15] as String
            if (incrementDart) BadgeProgressRepository.updateDartProgress(afterDart)
            if (incrementBowlingBy > 0) BadgeProgressRepository.updateBowlingProgress(afterBowling)
            if (incrementKarate) BadgeProgressRepository.updateKarateProgress(afterKarate)
            if (incrementGolf) BadgeProgressRepository.updateGolfProgress(afterGolf)
            if (incrementFishing) {
                BadgeProgressRepository.updateFishingSync(
                    userFishingProgress = afterFishingTier,
                    userFishingStreak = afterFishingStreak,
                    userFishingStreakPeriodKey = afterFishingStreakPeriod,
                )
            }
            BadgeProgressRepository.updateRocketSync(afterRocket, finalDaily, finalDayId)
            val payloads = mutableListOf<BadgeLevelUpPayload>()
            if (incrementBowlingBy > 0) {
                resolveLevelUpPayload(
                    BadgeFragment.BadgeAnimMode.BOWLING_WITH_BASE,
                    beforeBowling,
                    afterBowling,
                    listOf(5, 10, 20, 50, 100),
                )?.let { payloads.add(it) }
            }
            if (incrementDart) {
                resolveLevelUpPayload(
                    BadgeFragment.BadgeAnimMode.DAILY_ONLY,
                    beforeDart,
                    afterDart,
                    listOf(3, 10, 20, 30, 50),
                )?.let { payloads.add(it) }
            }
            if (incrementKarate) {
                resolveKarateFirstUnlockPayload(beforeKarate, afterKarate)?.let { payloads.add(it) }
            }
            payloads.addAll(resolveRocketLevelUpChain(beforeRocket, afterRocket))
            if (incrementGolf) {
                resolveLevelUpPayload(
                    BadgeFragment.BadgeAnimMode.GOLF_WITH_BASE,
                    beforeGolf,
                    afterGolf,
                    listOf(5, 10, 20, 50, 100),
                )?.let { payloads.add(it) }
            }
            if (incrementFishing) {
                payloads.addAll(resolveFishingLevelUpChain(beforeFishingTier, afterFishingTier))
            }
            onDone(payloads)
        }.addOnFailureListener { e ->
            Log.e(TAG, "badge progress update failed", e)
            onDone(emptyList())
        }
    }
}

/**
 * Firestore tam sayıları Android SDK’da çoğunlukla [Long] döndürür; Kotlin’de [Long], [Number] değildir,
 * bu yüzden `(x as? Number)?.toInt()` çoğu zaman null kalır. [java.lang.Integer] vb. için [java.lang.Number] yolu kullanılır.
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
    else -> (this as? java.lang.Number)?.doubleValue()?.toInt()
}

/** `get` ile gelmeyen edge-case’ler için [data] haritası da dener. */
fun DocumentSnapshot.pendingLeaderboardRewardSeasonFromDoc(): Int? {
    if (!exists()) return null
    return get("pendingLeaderboardRewardSeason").firestoreIntOrNull()
        ?: data?.get("pendingLeaderboardRewardSeason").firestoreIntOrNull()
}

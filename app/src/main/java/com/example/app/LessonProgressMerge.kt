package com.example.app

import com.example.app.model.LessonItem

/**
 * Firestore snapshot ile bellekteki ders ilerlemesini birleştirir.
 * Claim sonrası gelen eski bulut snapshot'ının [stepIsFinish] vb. alanları geri almasını önler.
 */
object LessonProgressMerge {

    fun mergeListsPreferMoreProgress(
        local: List<LessonItem>,
        remote: List<LessonItem>,
    ): MutableList<LessonItem> {
        if (local.isEmpty()) return remote.toMutableList()
        if (remote.isEmpty()) return local.toMutableList()
        val size = maxOf(local.size, remote.size)
        return (0 until size).mapNotNull { index ->
            val l = local.getOrNull(index)
            val r = remote.getOrNull(index)
            when {
                l == null -> r
                r == null -> l
                else -> {
                    val merged = mergeItemPreferMoreProgress(l, r)
                    LessonProgressDiag.logMergeIndex(
                        "LessonProgressMerge",
                        GlobalLessonData.globalPartId,
                        index,
                        l,
                        r,
                        merged,
                    )
                    merged
                }
            }
        }.toMutableList()
    }

    /** İki liste aynı ilerleme durumunu temsil ediyorsa true (gereksiz adapter yenilemesini önler). */
    fun sameProgressState(a: List<LessonItem>, b: List<LessonItem>): Boolean {
        if (a.size != b.size) return false
        return a.indices.all { index -> sameItemProgress(a[index], b[index]) }
    }

    private fun sameItemProgress(a: LessonItem, b: LessonItem): Boolean =
        a.stepIsFinish == b.stepIsFinish &&
            a.currentStep == b.currentStep &&
            a.isCompleted == b.isCompleted &&
            a.stepCompletionStatus == b.stepCompletionStatus &&
            a.stepCupIcon == b.stepCupIcon &&
            a.record == b.record &&
            a.raceBusyLevel == b.raceBusyLevel &&
            a.finalGoldVisualUnlocked == b.finalGoldVisualUnlocked

    private fun progressRank(item: LessonItem): Int {
        if (item.type == LessonItem.TYPE_HEADER || item.type == LessonItem.TYPE_PART ||
            item.type == LessonItem.TYPE_BACK_PART
        ) {
            return 0
        }
        var rank = item.currentStep.coerceAtLeast(0)
        rank += item.stepCompletionStatus.count { it } * 1_000
        if (item.stepIsFinish) rank += 100_000
        if (item.isCompleted) rank += 10
        if (item.type == LessonItem.TYPE_CHEST) {
            rank += (item.record ?: 0) / 1_000
        }
        return rank
    }

    private fun mergeItemPreferMoreProgress(local: LessonItem, remote: LessonItem): LessonItem {
        if (local.type != remote.type) {
            return if (progressRank(local) >= progressRank(remote)) local else remote
        }
        if (local.type == LessonItem.TYPE_HEADER || local.type == LessonItem.TYPE_PART ||
            local.type == LessonItem.TYPE_BACK_PART
        ) {
            return if (progressRank(local) >= progressRank(remote)) local else remote
        }

        val stepIsFinish = local.stepIsFinish || remote.stepIsFinish
        val stepCount = local.stepCount.coerceAtLeast(remote.stepCount)
        val mergedStatus = List(stepCount) { index ->
            local.stepCompletionStatus.getOrElse(index) { false } ||
                remote.stepCompletionStatus.getOrElse(index) { false } ||
                stepIsFinish
        }
        val currentStep = when {
            stepIsFinish -> maxOf(local.currentStep, remote.currentStep, stepCount)
            else -> maxOf(local.currentStep, remote.currentStep)
        }

        val winner = if (progressRank(local) >= progressRank(remote)) local else remote
        var merged = winner.copy(
            stepCount = stepCount,
            stepIsFinish = stepIsFinish,
            currentStep = currentStep,
            stepCompletionStatus = mergedStatus,
            isCompleted = local.isCompleted || remote.isCompleted,
            finalGoldVisualUnlocked = local.finalGoldVisualUnlocked || remote.finalGoldVisualUnlocked,
            tutorialIsFinish = local.tutorialIsFinish || remote.tutorialIsFinish,
            raceBusyLevel = if (progressRank(local) >= progressRank(remote)) {
                local.raceBusyLevel
            } else {
                remote.raceBusyLevel
            },
        )

        if (merged.type == LessonItem.TYPE_CHEST) {
            merged = mergeChestFields(merged, local, remote)
        } else {
            merged = merged.copy(
                stepCupIcon = if (progressRank(local) >= progressRank(remote)) {
                    local.stepCupIcon
                } else {
                    remote.stepCupIcon
                },
            )
        }

        return merged
    }

    private fun mergeChestFields(
        base: LessonItem,
        local: LessonItem,
        remote: LessonItem,
    ): LessonItem {
        val bestRecord = listOfNotNull(local.record, remote.record).maxOrNull()
        var merged = base.copy(record = bestRecord)

        val season = SeasonClock.currentSeason()
        val localSeasonId = local.leaderboardSeasonId
        val remoteSeasonId = remote.leaderboardSeasonId
        val seasonBest = when {
            localSeasonId == season && remoteSeasonId == season ->
                maxOf(local.leaderboardSeasonBest ?: 0, remote.leaderboardSeasonBest ?: 0).takeIf { it > 0 }
            localSeasonId == season -> local.leaderboardSeasonBest
            remoteSeasonId == season -> remote.leaderboardSeasonBest
            else -> local.leaderboardSeasonBest ?: remote.leaderboardSeasonBest
        }
        merged = merged.copy(
            leaderboardSeasonId = localSeasonId ?: remoteSeasonId ?: season,
            leaderboardSeasonBest = seasonBest,
        )

        val recordForIcon = bestRecord ?: 0
        val iconFromRecord = ChestTypeProgressHelper.resolvedChestIcon(merged, recordForIcon)
        val bestIcon = listOf(
            local.stepCupIcon,
            remote.stepCupIcon,
            iconFromRecord,
        ).maxByOrNull { ChestTypeProgressHelper.starCountForChestIcon(it) }
            ?: iconFromRecord
        return merged.copy(stepCupIcon = bestIcon)
    }
}

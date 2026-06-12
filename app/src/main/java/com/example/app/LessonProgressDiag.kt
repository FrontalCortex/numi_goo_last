package com.example.app

import android.content.Context
import android.util.Log
import com.example.app.model.LessonItem
import java.util.concurrent.atomic.AtomicInteger

/**
 * Ders ilerlemesi / stepIsFinish / Firestore merge teşhisi.
 *
 * Logcat filtresi: `LessonProgressDiag`
 *
 * Akış sırası için satır başındaki `#N` seq numarasına bakın.
 */
object LessonProgressDiag {
    const val LOG_TAG = "LessonProgressDiag"

    private val seq = AtomicInteger(0)

    fun log(caller: String, message: String) {
        Log.i(LOG_TAG, "#${seq.incrementAndGet()} | $caller | $message")
    }

    fun logItem(
        caller: String,
        partId: Int,
        index: Int,
        item: LessonItem?,
        label: String = "",
    ) {
        if (item == null) {
            log(caller, "part=$partId idx=$index $label | item=null")
            return
        }
        val tag = if (label.isNotEmpty()) "$label | " else ""
        log(
            caller,
            "${tag}part=$partId idx=$index type=${item.type} title=${item.title.take(24)} | " +
                "step=${item.currentStep}/${item.stepCount} finish=${item.stepIsFinish} " +
                "filled=${item.stepCompletionStatus.count { it }}/${item.stepCount} " +
                "raceBusy=${item.raceBusyLevel} completed=${item.isCompleted} record=${item.record} " +
                "cupIcon=${item.stepCupIcon} mapIdx=${item.mapFragmentIndex}",
        )
    }

    fun logItemDelta(
        caller: String,
        partId: Int,
        index: Int,
        before: LessonItem?,
        after: LessonItem?,
    ) {
        if (before == null && after == null) return
        val finishChanged = before?.stepIsFinish != after?.stepIsFinish
        val stepChanged = before?.currentStep != after?.currentStep
        if (!finishChanged && !stepChanged && before?.stepCompletionStatus == after?.stepCompletionStatus) {
            log(caller, "part=$partId idx=$index | DELTA none (step/finish/status unchanged)")
            return
        }
        log(
            caller,
            "part=$partId idx=$index | DELTA " +
                "finish ${before?.stepIsFinish}→${after?.stepIsFinish} " +
                "step ${before?.currentStep}→${after?.currentStep} " +
                "filled ${before?.stepCompletionStatus?.count { it }}→${after?.stepCompletionStatus?.count { it }} " +
                "raceBusy ${before?.raceBusyLevel}→${after?.raceBusyLevel}",
        )
    }

    fun logMergeIndex(
        caller: String,
        partId: Int,
        index: Int,
        local: LessonItem,
        remote: LessonItem,
        merged: LessonItem,
    ) {
        val regressFinish = local.stepIsFinish && !merged.stepIsFinish
        val staleRemote = local.stepIsFinish && !remote.stepIsFinish
        val fixedByMerge = staleRemote && merged.stepIsFinish
        if (!staleRemote && local.stepIsFinish == remote.stepIsFinish && local.stepIsFinish == merged.stepIsFinish &&
            local.currentStep == merged.currentStep
        ) {
            return
        }
        log(
            caller,
            "part=$partId idx=$index MERGE | " +
                "local.finish=${local.stepIsFinish} remote.finish=${remote.stepIsFinish} merged.finish=${merged.stepIsFinish} | " +
                "local.step=${local.currentStep} remote.step=${remote.currentStep} merged.step=${merged.currentStep} | " +
                "staleRemote=$staleRemote fixedByMerge=$fixedByMerge regressFinish=$regressFinish",
        )
    }

    fun logListChestFinishSummary(caller: String, partId: Int, items: List<LessonItem>) {
        val chestIndices = items.mapIndexedNotNull { i, item ->
            if (item.type == LessonItem.TYPE_CHEST) i to item.stepIsFinish else null
        }
        log(
            caller,
            "part=$partId chestFinishSummary=${chestIndices.joinToString { (i, f) -> "$i:$f" }}",
        )
    }

    /** Son ChestFragment claim sonucu — MapFragment notify ile karşılaştırma için. */
    data class LastClaimRecord(
        val mapIdx: Int,
        val partId: Int,
        val stepFinishAfter: Boolean?,
        val currentStepAfter: Int?,
        val stepCountAfter: Int?,
        val raceBusyAfter: Int?,
        val route: String,
        val hasMissionProgress: Boolean,
        val missionCounterDelta: Boolean,
        val incrementRocketDailyLessons: Boolean,
        val tutorialNumber: Int,
        val atMs: Long = System.currentTimeMillis(),
    )

    @Volatile
    var lastClaimRecord: LastClaimRecord? = null

    fun logMissionCounterDelta(
        caller: String,
        before: MissionsProgressStore.Snapshot,
        after: MissionsProgressStore.Snapshot,
    ) {
        fun delta(label: String, b: Int, a: Int) =
            if (b != a) "$label $b→$a" else null
        val parts = listOfNotNull(
            delta("dailyFinish", before.dailyStepFinishCount, after.dailyStepFinishCount),
            delta("weeklyFinish", before.weeklyStepFinishCount, after.weeklyStepFinishCount),
            delta("dailyIncr", before.dailyStepIncrementCount, after.dailyStepIncrementCount),
            delta("weeklyIncr", before.weeklyStepIncrementCount, after.weeklyStepIncrementCount),
            delta("dailyPerfect", before.dailyPerfectStepIncrementCount, after.dailyPerfectStepIncrementCount),
            delta("weeklyPerfect", before.weeklyPerfectStepIncrementCount, after.weeklyPerfectStepIncrementCount),
            delta("dailyChestRec", before.dailyChestRecordBreakCount, after.dailyChestRecordBreakCount),
            delta("weeklyChestRec", before.weeklyChestRecordBreakCount, after.weeklyChestRecordBreakCount),
            delta("dailyStar", before.dailyChestStarGainCount, after.dailyChestStarGainCount),
            delta("weeklyStar", before.weeklyChestStarGainCount, after.weeklyChestStarGainCount),
            delta("dailyLearnMin", before.dailyLearnMinutesCount, after.dailyLearnMinutesCount),
            delta("weeklyLearnMin", before.weeklyLearnMinutesCount, after.weeklyLearnMinutesCount),
        )
        val anyDelta = parts.isNotEmpty()
        log(
            caller,
            "MISSION_COUNTERS anyDelta=$anyDelta | ${if (parts.isEmpty()) "unchanged" else parts.joinToString(", ")}",
        )
    }

    /**
     * Görev paneli neden açılmadı / açıldı — seçili görevlerde çubuk değişimi ve ham sayaç farkı.
     * Hipotez etiketleri: H1=sayaç artmadı, H2=seçili görevde görünür fark yok (hedefte dolu vb.),
     * H3=ham sayaç arttı ama seçili 3 görevde bar değişmedi.
     */
    fun logMissionPanelDecision(
        caller: String,
        context: Context,
        before: MissionsProgressStore.Snapshot,
        after: MissionsProgressStore.Snapshot,
        hasVisible: Boolean,
    ) {
        logMissionCounterDelta(caller, before, after)
        val dailyIds = MissionsProgressStore.selectedMissionsForDaily(context).map { it.id }
        val weeklyIds = MissionsProgressStore.selectedMissionsForWeekly(context).map { it.id }
        log(caller, "SELECTED_MISSIONS daily=$dailyIds weekly=$weeklyIds")

        val visibleMissions = mutableListOf<String>()
        MissionsProgressStore.selectedMissionsForDaily(context).forEach { mission ->
            val b = MissionsProgressStore.missionProgress(before, MissionWindow.DAILY, mission)
                .coerceAtMost(mission.target)
            val a = MissionsProgressStore.missionProgress(after, MissionWindow.DAILY, mission)
                .coerceAtMost(mission.target)
            if (b != a) visibleMissions += "D:${mission.id} $b→$a/${mission.target}"
            else log(caller, "MISSION_BAR daily:${mission.id} unchanged $b/${mission.target} rawBefore=${
                MissionsProgressStore.missionProgress(before, MissionWindow.DAILY, mission)
            } rawAfter=${
                MissionsProgressStore.missionProgress(after, MissionWindow.DAILY, mission)
            }")
        }
        MissionsProgressStore.selectedMissionsForWeekly(context).forEach { mission ->
            val b = MissionsProgressStore.missionProgress(before, MissionWindow.WEEKLY, mission)
                .coerceAtMost(mission.target)
            val a = MissionsProgressStore.missionProgress(after, MissionWindow.WEEKLY, mission)
                .coerceAtMost(mission.target)
            if (b != a) visibleMissions += "W:${mission.id} $b→$a/${mission.target}"
            else log(caller, "MISSION_BAR weekly:${mission.id} unchanged $b/${mission.target} rawBefore=${
                MissionsProgressStore.missionProgress(before, MissionWindow.WEEKLY, mission)
            } rawAfter=${
                MissionsProgressStore.missionProgress(after, MissionWindow.WEEKLY, mission)
            }")
        }

        val rawDelta = before != after && listOf(
            before.dailyStepFinishCount != after.dailyStepFinishCount,
            before.weeklyStepFinishCount != after.weeklyStepFinishCount,
            before.dailyStepIncrementCount != after.dailyStepIncrementCount,
            before.weeklyStepIncrementCount != after.weeklyStepIncrementCount,
            before.dailyPerfectStepIncrementCount != after.dailyPerfectStepIncrementCount,
            before.weeklyPerfectStepIncrementCount != after.weeklyPerfectStepIncrementCount,
            before.dailyChestRecordBreakCount != after.dailyChestRecordBreakCount,
            before.weeklyChestRecordBreakCount != after.weeklyChestRecordBreakCount,
            before.dailyChestStarGainCount != after.dailyChestStarGainCount,
            before.weeklyChestStarGainCount != after.weeklyChestStarGainCount,
            before.dailyLearnMinutesCount != after.dailyLearnMinutesCount,
            before.weeklyLearnMinutesCount != after.weeklyLearnMinutesCount,
        ).any { it }

        val hypothesis = when {
            hasVisible -> "PANEL_OPEN"
            !rawDelta -> "H1_NO_COUNTER_DELTA"
            visibleMissions.isEmpty() && rawDelta -> "H3_RAW_DELTA_NOT_IN_SELECTED_BARS"
            else -> "H2_BAR_UNCHANGED_AT_CAP_OR_OTHER"
        }
        log(
            caller,
            "MISSION_PANEL hasVisible=$hasVisible hypothesis=$hypothesis " +
                "visibleBars=[${visibleMissions.joinToString()}]",
        )
    }

    fun recordClaim(
        mapIdx: Int,
        partId: Int,
        itemAfter: LessonItem?,
        route: String,
        hasMissionProgress: Boolean,
        missionCounterDelta: Boolean,
        incrementRocketDailyLessons: Boolean,
        tutorialNumber: Int,
    ) {
        lastClaimRecord = LastClaimRecord(
            mapIdx = mapIdx,
            partId = partId,
            stepFinishAfter = itemAfter?.stepIsFinish,
            currentStepAfter = itemAfter?.currentStep,
            stepCountAfter = itemAfter?.stepCount,
            raceBusyAfter = itemAfter?.raceBusyLevel,
            route = route,
            hasMissionProgress = hasMissionProgress,
            missionCounterDelta = missionCounterDelta,
            incrementRocketDailyLessons = incrementRocketDailyLessons,
            tutorialNumber = tutorialNumber,
        )
        log(
            "ClaimDiag.record",
            "mapIdx=$mapIdx part=$partId route=$route finish=${itemAfter?.stepIsFinish} " +
                "step=${itemAfter?.currentStep}/${itemAfter?.stepCount} raceBusy=${itemAfter?.raceBusyLevel} " +
                "hasMission=$hasMissionProgress missionDelta=$missionCounterDelta " +
                "rocketIncr=$incrementRocketDailyLessons tutorial=$tutorialNumber",
        )
    }

    /** Map görünür olduğunda son claim ile karşılaştır (H7: güncellendi ama UI eski). */
    fun logClaimVsMapAtNotify(caller: String, partId: Int, mapIdx: Int, itemNow: LessonItem?) {
        val last = lastClaimRecord
        if (last == null) {
            log(caller, "mapIdx=$mapIdx part=$partId | no lastClaimRecord")
            return
        }
        val ageMs = System.currentTimeMillis() - last.atMs
        if (mapIdx != last.mapIdx) {
            log(caller, "mapIdx=$mapIdx != lastClaim.mapIdx=${last.mapIdx} (ageMs=$ageMs)")
        }
        val finishDrift = last.stepFinishAfter != itemNow?.stepIsFinish
        val stepDrift = last.currentStepAfter != itemNow?.currentStep
        val raceDrift = last.raceBusyAfter != itemNow?.raceBusyLevel
        if (finishDrift || stepDrift || raceDrift) {
            log(
                caller,
                "H7_MAP_DRIFT ageMs=$ageMs | claim.finish=${last.stepFinishAfter} now.finish=${itemNow?.stepIsFinish} | " +
                    "claim.step=${last.currentStepAfter} now.step=${itemNow?.currentStep} | " +
                    "claim.raceBusy=${last.raceBusyAfter} now.raceBusy=${itemNow?.raceBusyLevel} | " +
                    "claimRoute=${last.route}",
            )
        } else {
            log(
                caller,
                "CLAIM_MATCH ageMs=$ageMs mapIdx=$mapIdx finish=${itemNow?.stepIsFinish} " +
                    "step=${itemNow?.currentStep}/${itemNow?.stepCount} (data matches last claim)",
            )
        }
    }
}

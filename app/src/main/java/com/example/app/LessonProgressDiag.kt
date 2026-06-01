package com.example.app

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
}

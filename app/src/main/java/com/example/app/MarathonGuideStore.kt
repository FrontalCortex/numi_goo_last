package com.example.app

import android.content.Context
import android.util.Log
import com.example.app.model.LessonItem

/**
 * Part 1 ilk [LessonItem.TYPE_CHEST] (Ünite Maratonu) tamamlandığında gösterilecek rehber paneli.
 * Bayrak claim anında set edilir; panel harita üstü temizken [MapFragment.maybeShowPendingMarathonGuide] ile açılır.
 *
 * Logcat: `adb logcat -s MarathonGuide`
 */
object MarathonGuideStore {

    const val LOG_TAG = "MarathonGuide"

    private const val PREFS = "GuidePanelPrefs"
    private const val KEY_SHOWN = "marathon_guide_shown"
    private const val KEY_PENDING = "marathon_guide_pending"
    /** Eski CupFragment / ARG_SHOW_GUIDE anahtarı — bir kez gösterildiyse tekrar gösterme. */
    private const val LEGACY_SHOWN = "cupFragment_guide_shown"
    /** Rehber yalnızca Part 1 ilk maratonu için. */
    private const val GUIDE_PART_ID = 1

    fun isShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOWN, false) || prefs.getBoolean(LEGACY_SHOWN, false)
    }

    fun isPending(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PENDING, false)

    fun clearPending(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PENDING, false)
            .apply()
        Log.d(LOG_TAG, "clearPending")
    }

    fun markShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOWN, true)
            .putBoolean(KEY_PENDING, false)
            .apply()
        Log.d(LOG_TAG, "markShown | marathon guide consumed")
        logPrefsSnapshot(context, "markShown")
    }

    fun logPrefsSnapshot(context: Context, caller: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        Log.d(
            LOG_TAG,
            "prefs | caller=$caller marathon_shown=${prefs.getBoolean(KEY_SHOWN, false)} " +
                "legacy_shown=${prefs.getBoolean(LEGACY_SHOWN, false)} pending=${prefs.getBoolean(KEY_PENDING, false)}",
        )
    }

    fun logLessonSnapshot(caller: String, partId: Int, lessonIndex: Int, item: LessonItem?) {
        if (item == null) {
            Log.d(
                LOG_TAG,
                "lesson | caller=$caller part=$partId mapFragmentStepIndex=$lessonIndex item=null " +
                    "expectedChestIdx=${firstMarathonLessonIndex()}",
            )
            return
        }
        Log.d(
            LOG_TAG,
            "lesson | caller=$caller part=$partId mapFragmentStepIndex=$lessonIndex " +
                "expectedChestIdx=${firstMarathonLessonIndex()} type=${item.type} " +
                "stepIsFinish=${item.stepIsFinish} title=${item.title.take(32)} mapFragmentIndex=${item.mapFragmentIndex}",
        )
    }

    /**
     * @return true if pending flag was set
     */
    fun scheduleIfEligible(
        context: Context,
        partId: Int,
        lessonIndex: Int,
        item: LessonItem?,
        source: String,
    ): Boolean {
        logPrefsSnapshot(context, "scheduleBefore:$source")
        logLessonSnapshot("schedule:$source", partId, lessonIndex, item)

        if (item == null) {
            Log.w(LOG_TAG, "schedule REJECT | source=$source reason=item_null")
            return false
        }
        if (isShown(context)) {
            Log.w(
                LOG_TAG,
                "schedule REJECT | source=$source reason=already_shown " +
                    "(marathon or legacy cupFragment_guide_shown)",
            )
            return false
        }
        val rejectReason = eligibilityRejectReason(partId, lessonIndex, item)
        if (rejectReason != null) {
            Log.w(
                LOG_TAG,
                "schedule REJECT | source=$source reason=$rejectReason part=$partId " +
                    "mapFragmentStepIndex=$lessonIndex expectedChestIdx=${firstMarathonLessonIndex()} " +
                    "type=${item.type} stepIsFinish=${item.stepIsFinish}",
            )
            return false
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PENDING, true)
            .apply()
        Log.i(
            LOG_TAG,
            "schedule OK | source=$source part=$partId index=$lessonIndex title=${item.title.take(24)} " +
                "→ pending=true",
        )
        logPrefsSnapshot(context, "scheduleAfter:$source")
        return true
    }

    fun firstMarathonLessonIndex(): Int =
        LessonLeaderboardRepository.firstChestLessonIndexForPart(GUIDE_PART_ID) ?: 4

    fun eligibilityRejectReason(partId: Int, lessonIndex: Int, item: LessonItem): String? {
        if (item.type != LessonItem.TYPE_CHEST) return "not_type_chest(type=${item.type})"
        if (item.stepIsFinish) return "step_already_finish_before_claim"
        if (partId != GUIDE_PART_ID) return "wrong_part(part=$partId need=$GUIDE_PART_ID)"
        val expected = firstMarathonLessonIndex()
        if (lessonIndex != expected) {
            return "index_mismatch(mapFragmentStepIndex=$lessonIndex expectedListIdx=$expected " +
                "item.mapFragmentIndex=${item.mapFragmentIndex})"
        }
        return null
    }
}

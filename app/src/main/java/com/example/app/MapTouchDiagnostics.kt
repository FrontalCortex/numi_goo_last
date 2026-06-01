package com.example.app

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Harita dönüşü / dokunma kilidi teşhisi.
 *
 * Logcat filtresi (tek tag):
 * ```
 * adb logcat -s MapTouchDbg
 * ```
 *
 * Özet satır formatı: `PHASE=... | OUTCOME=... | VERDICT=... | reasons=...`
 */
object MapTouchDiagnostics {

    const val LOG_TAG = "MapTouchDbg"

    /** Muhtemel kilit nedeni kodları (grep / filtre için sabit). */
    object Reason {
        const val STALE_ABACUS_HOST_VISIBLE = "STALE_ABACUS_HOST_VISIBLE"
        const val STALE_ABACUS_FRAGMENT = "STALE_ABACUS_FRAGMENT"
        const val SCRIM_BLOCKING = "SCRIM_BLOCKING"
        const val CONTENT_PRACTICE_BLOCKER = "CONTENT_PRACTICE_BLOCKER"
        const val CONTENT_LESSON_BLOCKER = "CONTENT_LESSON_BLOCKER"
        const val CHROME_LOCKED = "CHROME_LOCKED"
        const val MAP_TRANSPARENT_OVERLAY = "MAP_TRANSPARENT_OVERLAY"
        const val MAP_RV_TOUCH_CONSUMED = "MAP_RV_TOUCH_CONSUMED"
        const val SANITIZE_SKIPPED_ACTIVE_OVERLAY = "SANITIZE_SKIPPED_ACTIVE_OVERLAY"
        const val RECONCILE_SKIPPED_BLOCKING_OVERLAY = "RECONCILE_SKIPPED_BLOCKING_OVERLAY"
        const val RECONCILE_RESHOW_ABACUS_HOST = "RECONCILE_RESHOW_ABACUS_HOST"
        const val MAP_TOUCH_NOT_REENABLED = "MAP_TOUCH_NOT_REENABLED"
        const val BOTTOM_SHEET_PRESENT = "BOTTOM_SHEET_PRESENT"
    }

    enum class Verdict {
        /** Akış normal görünüyor. */
        OK,
        /** Henüz karar verilmedi / ara adım. */
        CHECKING,
        /** Dokunma muhtemelen kilitli — reasons listesine bak. */
        LIKELY_BLOCKED,
        /** Bilinçli atlama (aktif ders overlay vb.). */
        SKIPPED,
    }

    fun report(
        activity: MainActivity,
        phase: String,
        outcome: String,
        extra: String = "",
        mapTouchState: MapFragment.MapTouchState? = null,
    ) {
        if (activity.isFinishing) return
        val reasons = collectReasons(activity, mapTouchState)
        val verdict = verdictFromReasons(reasons, outcome)
        val summary = buildString {
            append("PHASE=").append(phase)
            append(" | OUTCOME=").append(outcome)
            append(" | VERDICT=").append(verdict.name)
            if (reasons.isNotEmpty()) {
                append(" | reasons=").append(reasons.joinToString(","))
            }
            if (extra.isNotEmpty()) append(" | ").append(extra)
        }
        Log.w(LOG_TAG, summary)
        Log.w(LOG_TAG, "  ${activity.buildTouchDiagSnapshot()}")
        mapTouchState?.let { Log.w(LOG_TAG, "  mapTouch=$it") }
        Log.w(LOG_TAG, "  scrim=${scrimLine(activity)}")
        Log.w(LOG_TAG, "  ${overlayHostsLine(activity)}")
        Log.w(LOG_TAG, "  ${contentBlockersLine(activity)}")
        Log.w(LOG_TAG, "  ${backStackLine(activity)}")
        Log.w(LOG_TAG, "  chromeLockDepth=${MainActivityChromeBlocker.currentLockDepth()}")
    }

    fun reportFromFragment(
        fragment: Fragment,
        phase: String,
        outcome: String,
        extra: String = "",
    ) {
        val main = fragment.activity as? MainActivity ?: run {
            Log.w(LOG_TAG, "PHASE=$phase | OUTCOME=$outcome | VERDICT=CHECKING | activity=null")
            return
        }
        val mapState = (fragment as? MapFragment)?.currentMapTouchState()
        report(main, phase, outcome, extra, mapState)
    }

    private fun verdictFromReasons(reasons: List<String>, outcome: String): Verdict {
        if (outcome.startsWith("SKIP_") || outcome.contains("skip")) return Verdict.SKIPPED
        if (outcome.endsWith("_OK") || outcome == "ENABLE_MAP_TOUCH" || outcome == "NOTIFY_VISIBLE") {
            return if (reasons.any { it != Reason.MAP_TOUCH_NOT_REENABLED }) Verdict.LIKELY_BLOCKED else Verdict.OK
        }
        return when {
            reasons.isEmpty() -> Verdict.OK
            reasons.any {
                it == Reason.STALE_ABACUS_HOST_VISIBLE ||
                    it == Reason.STALE_ABACUS_FRAGMENT ||
                    it == Reason.MAP_TRANSPARENT_OVERLAY ||
                    it == Reason.MAP_RV_TOUCH_CONSUMED ||
                    it == Reason.SANITIZE_SKIPPED_ACTIVE_OVERLAY ||
                    it == Reason.RECONCILE_SKIPPED_BLOCKING_OVERLAY
            } -> Verdict.LIKELY_BLOCKED
            else -> Verdict.CHECKING
        }
    }

    fun collectReasons(
        activity: MainActivity,
        mapTouchState: MapFragment.MapTouchState? = null,
    ): MutableList<String> {
        val reasons = mutableListOf<String>()
        val abacusHost = activity.findViewById<View>(R.id.abacusFragmentContainer)
        val abacusFrag = activity.supportFragmentManager
            .findFragmentById(R.id.abacusFragmentContainer)
        if (abacusHost?.visibility == View.VISIBLE) {
            reasons.add(Reason.STALE_ABACUS_HOST_VISIBLE)
        }
        if (abacusFrag != null && activity.isBlockingLessonOverlayFragment(abacusFrag)) {
            reasons.add(Reason.STALE_ABACUS_FRAGMENT + ":" + abacusFrag.javaClass.simpleName)
        }
        val scrim = activity.findViewById<View>(R.id.scrimView)
        if (scrim?.visibility == View.VISIBLE && (scrim.alpha > 0.01f)) {
            reasons.add(Reason.SCRIM_BLOCKING)
        }
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        if (content?.findViewWithTag<View>(MainActivity.PRACTICE_TOUCH_BLOCKER_TAG) != null) {
            reasons.add(Reason.CONTENT_PRACTICE_BLOCKER)
        }
        if (content?.findViewWithTag<View>(MainActivity.LESSON_ACTION_TOUCH_BLOCKER_TAG) != null) {
            reasons.add(Reason.CONTENT_LESSON_BLOCKER)
        }
        if (MainActivityChromeBlocker.currentLockDepth() > 0) {
            reasons.add(Reason.CHROME_LOCKED)
        }
        activity.findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.coordinator_layout)
            ?.findViewWithTag<View>("bottom_sheet")
            ?.let { reasons.add(Reason.BOTTOM_SHEET_PRESENT) }
        mapTouchState?.let { state ->
            if (state.transparentOverlayAttached) reasons.add(Reason.MAP_TRANSPARENT_OVERLAY)
            if (state.recyclerConsumingTouch) reasons.add(Reason.MAP_RV_TOUCH_CONSUMED)
            if (!state.touchRoutingEnabled) reasons.add(Reason.MAP_TOUCH_NOT_REENABLED)
        }
        return reasons
    }

    private fun scrimLine(activity: FragmentActivity): String {
        val scrim = activity.findViewById<View>(R.id.scrimView)
        return "scrim vis=${scrim?.visibility} alpha=${scrim?.alpha} clickable=${scrim?.isClickable}"
    }

    private fun overlayHostsLine(activity: MainActivity): String {
        val fm = activity.supportFragmentManager
        fun host(id: Int): String {
            val v = activity.findViewById<View>(id)
            val frag = fm.findFragmentById(id)?.javaClass?.simpleName ?: "null"
            val vis = when (v?.visibility) {
                View.VISIBLE -> "VISIBLE"
                View.GONE -> "GONE"
                View.INVISIBLE -> "INVISIBLE"
                else -> "?"
            }
            return "${activity.resources.getResourceEntryName(id)} frag=$frag vis=$vis"
        }
        return listOf(
            R.id.abacusFragmentContainer,
            R.id.resultFragmentContainer,
        ).joinToString(" | ") { host(it) }
    }

    private fun contentBlockersLine(activity: FragmentActivity): String {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return "content=null"
        val practice = content.findViewWithTag<View>(MainActivity.PRACTICE_TOUCH_BLOCKER_TAG) != null
        val lesson = content.findViewWithTag<View>(MainActivity.LESSON_ACTION_TOUCH_BLOCKER_TAG) != null
        return "blockers practice=$practice lesson=$lesson"
    }

    private fun backStackLine(activity: MainActivity): String {
        val fm = activity.supportFragmentManager
        val base = fm.findFragmentById(R.id.fragmentContainerID)?.javaClass?.simpleName ?: "null"
        return "fmBackStack=${fm.backStackEntryCount} base=$base forceDismiss=${activity.isForcingAbacusOverlayDismiss()}"
    }
}

fun MainActivity.logMapTouchDiag(
    phase: String,
    outcome: String,
    extra: String = "",
) {
    val map = supportFragmentManager.findFragmentById(R.id.fragmentContainerID) as? MapFragment
    MapTouchDiagnostics.report(this, phase, outcome, extra, map?.currentMapTouchState())
}

package com.example.app

import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Dokunma kilitlenmesi teşhisi. Logcat: `adb logcat -s TouchDiag`
 */
object MainActivityTouchDiag {

    const val LOG_TAG = "TouchDiag"

    fun report(activity: MainActivity, caller: String, snapshot: String = "") {
        if (activity.isFinishing) return
        val snap = snapshot.ifEmpty { activity.buildTouchDiagSnapshot() }
        val lines = buildList {
            add("── TouchDiag [$caller] ──")
            add(snap)
            add(blockerLine(activity))
            add(overlayHostsLine(activity))
            add(chromeLine())
            add(contentBlockersLine(activity))
            add(backStackLine(activity))
        }
        lines.forEach { Log.w(LOG_TAG, it) }
    }

    /** Şüpheli katman varken dokunuş düşünce kısa rapor. */
    fun reportTouchDownIfSuspicious(activity: MainActivity, ev: MotionEvent) {
        if (ev.action != MotionEvent.ACTION_DOWN) return
        if (!hasSuspiciousTouchState(activity)) return
        Log.w(
            LOG_TAG,
            "touchDown raw=(${ev.rawX},${ev.rawY}) local=(${ev.x},${ev.y}) | ${blockerLine(activity)}",
        )
        report(activity, "touchDown.suspicious")
        activity.logMapTouchDiag(
            "userTouchDown",
            "SUSPICIOUS_TOUCH",
            "Kullanıcı dokundu ama üst katman şüpheli — VERDICT/reasons satırına bak",
        )
    }

    fun hasSuspiciousTouchState(activity: MainActivity): Boolean {
        val abacus = activity.findViewById<View>(R.id.abacusFragmentContainer)
        if (abacus?.visibility == View.VISIBLE) return true
        val scrim = activity.findViewById<View>(R.id.scrimView)
        if (scrim?.visibility == View.VISIBLE && scrim.alpha > 0.01f) return true
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return false
        if (content.findViewWithTag<View>(MainActivity.PRACTICE_TOUCH_BLOCKER_TAG) != null) return true
        if (content.findViewWithTag<View>(MainActivity.LESSON_ACTION_TOUCH_BLOCKER_TAG) != null) return true
        if (MainActivityChromeBlocker.currentLockDepth() > 0) return true
        return false
    }

    private fun blockerLine(activity: FragmentActivity): String {
        val scrim = activity.findViewById<View>(R.id.scrimView)
        val scrimVis = scrim?.visibility ?: -1
        val scrimAlpha = scrim?.alpha ?: -1f
        val scrimClick = scrim?.isClickable ?: false
        val bottomOverlay = activity.findViewById<View>(R.id.bottomPanelOverlay)
        return buildString {
            append("scrim vis=").append(scrimVis).append(" alpha=").append(scrimAlpha)
            append(" clickable=").append(scrimClick)
            append(" | bottomPanelOverlay=").append(bottomOverlay?.visibility ?: -1)
        }
    }

    private fun overlayHostsLine(activity: MainActivity): String {
        val fm = activity.supportFragmentManager
        fun host(id: Int, visView: View?): String {
            val frag = fm.findFragmentById(id)?.javaClass?.simpleName ?: "null"
            val vis = when (visView?.visibility) {
                View.VISIBLE -> "VISIBLE"
                View.GONE -> "GONE"
                View.INVISIBLE -> "INVISIBLE"
                else -> "?"
            }
            val click = visView?.isClickable ?: false
            return "${activity.resources.getResourceEntryName(id)} frag=$frag vis=$vis clickable=$click"
        }
        return buildString {
            append(host(R.id.abacusFragmentContainer, activity.findViewById(R.id.abacusFragmentContainer)))
            append(" | ")
            append(host(R.id.resultFragmentContainer, activity.findViewById(R.id.resultFragmentContainer)))
            append(" | ")
            append(host(R.id.seasonLeaderboardRewardGateContainer, activity.findViewById(R.id.seasonLeaderboardRewardGateContainer)))
            append(" | ")
            append(host(R.id.createQuestionOverlayContainer, activity.findViewById(R.id.createQuestionOverlayContainer)))
            append(" | ")
            append(host(R.id.recordingOverlayContainer, activity.findViewById(R.id.recordingOverlayContainer)))
        }
    }

    private fun chromeLine(): String =
        "chromeLockDepth=${MainActivityChromeBlocker.currentLockDepth()}"

    private fun contentBlockersLine(activity: FragmentActivity): String {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return "content=null"
        val practice = content.findViewWithTag<View>(MainActivity.PRACTICE_TOUCH_BLOCKER_TAG) != null
        val lesson = content.findViewWithTag<View>(MainActivity.LESSON_ACTION_TOUCH_BLOCKER_TAG) != null
        val childTags = (0 until content.childCount).mapNotNull { i ->
            content.getChildAt(i).tag?.toString()?.takeIf { it.isNotEmpty() }
        }
        return "contentBlockers practice=$practice lesson=$lesson childTags=$childTags childCount=${content.childCount}"
    }

    private fun backStackLine(activity: FragmentActivity): String {
        val fm = activity.supportFragmentManager
        val base = fm.findFragmentById(R.id.fragmentContainerID)
        val hidden = base?.isHidden ?: false
        val nav = activity.findViewById<BottomNavigationView>(R.id.bottomNavigationID)
        return buildString {
            append("fmBackStack=").append(fm.backStackEntryCount)
            append(" base=").append(base?.javaClass?.simpleName ?: "null")
            append(" baseHidden=").append(hidden)
            append(" navEnabled=").append(nav?.isEnabled)
            append(" navClickable=").append(nav?.isClickable)
        }
    }
}

fun MainActivity.logTouchDiag(caller: String) {
    MainActivityTouchDiag.report(this, caller, buildTouchDiagSnapshot())
}

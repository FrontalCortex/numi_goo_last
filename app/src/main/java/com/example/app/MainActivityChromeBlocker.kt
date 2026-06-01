package com.example.app

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Sandık / görev ödülü gibi tam ekran akışlarda üst para paneli ve alt navigasyonu kilitlemek için.
 * [acquire] / [release] çiftleri iç içe geçebilir (ör. ChestFragment → MissionChestRewardFragment); sayaç 0 olunca kilit kalkar.
 *
 * Logcat: `adb logcat -s ChromeBlockerDbg`
 */
object MainActivityChromeBlocker {

    private const val LOG_TAG = "ChromeBlockerDbg"

    private var lockDepth = 0

    fun currentLockDepth(): Int = lockDepth

    /** Map dönüşü / reconcile sonrası teşhis: release çağrılmadıysa depth>0 burada görünür. */
    fun logDiagnostic(caller: String, activity: Activity?) {
        val act = activity ?: run {
            Log.w(LOG_TAG, "[$caller] diagnostic: activity=null depth=$lockDepth")
            return
        }
        if (lockDepth > 0) {
            Log.w(
                LOG_TAG,
                "[$caller] STUCK_CHROME_LOCK? depth=$lockDepth (release/onDestroyView çağrılmamış olabilir)",
            )
        } else {
            Log.d(LOG_TAG, "[$caller] chrome ok depth=0")
        }
        logBottomNavState(act, "diagnostic:$caller")
    }

    /**
     * [onDestroyView] atlanırsa kilit açık kalır. Harita tabanı + abacus'ta bloklayıcı overlay yokken zorla aç.
     */
    fun ensureUnlockedForMapReturn(activity: Activity, blockingOverlayStillActive: Boolean) {
        logDiagnostic("ensureUnlocked.enter", activity)
        if (blockingOverlayStillActive) {
            Log.d(LOG_TAG, "[ensureUnlocked] skip (lesson/chest overlay hâlâ aktif) depth=$lockDepth")
            return
        }
        if (lockDepth <= 0) return
        Log.w(
            LOG_TAG,
            "[ensureUnlocked] force applyUnlock (depth was $lockDepth, release eksik kalmış)",
        )
        lockDepth = 0
        applyUnlock(activity)
        logDiagnostic("ensureUnlocked.afterForce", activity)
    }

    fun acquire(activity: Activity) {
        val caller = inferCaller()
        val appliedBlock = lockDepth == 0
        if (lockDepth++ == 0) {
            applyBlock(activity)
        }
        Log.d(
            LOG_TAG,
            "[$caller] acquire → depth=$lockDepth appliedBlock=$appliedBlock",
        )
        logBottomNavState(activity, "after acquire")
    }

    fun release(activity: Activity?) {
        val caller = inferCaller()
        if (lockDepth <= 0) {
            Log.w(LOG_TAG, "[$caller] release ignored (depth=$lockDepth)")
            return
        }
        val newDepth = (lockDepth - 1).coerceAtLeast(0)
        val shouldUnlock = lockDepth == 1
        lockDepth = newDepth

        val act = activity
        if (shouldUnlock) {
            if (act != null) {
                applyUnlock(act)
            } else {
                // Activity referansı kaybolduysa yine de depth'i sıfırla; harita dönüşünde
                // ensureUnlockedForMapReturn/applyUnlock zinciri son kilitleri toparlar.
                Log.w(LOG_TAG, "[$caller] release without activity at terminal depth; depth reset to 0")
            }
        }

        if (act != null) {
            Log.d(
                LOG_TAG,
                "[$caller] release → depth=$lockDepth appliedUnlock=$shouldUnlock",
            )
            logBottomNavState(act, "after release")
        } else {
            Log.w(
                LOG_TAG,
                "[$caller] release(activity=null) → depth=$lockDepth appliedUnlock=false",
            )
        }
    }

    private fun applyBlock(act: Activity) {
        Log.d(LOG_TAG, "applyBlock (depth was 0 → UI locked)")
        act.findViewById<View>(R.id.currencyPanel)?.apply {
            isClickable = false
            isFocusable = false
            setOnTouchListener { _, _ -> true }
        }
        act.findViewById<View>(R.id.energyText)?.apply {
            isClickable = false
            isFocusable = false
            setOnTouchListener { _, _ -> true }
            setOnClickListener(null)
        }
        act.findViewById<View>(R.id.energyIcon)?.apply {
            isClickable = false
            isFocusable = false
            setOnTouchListener { _, _ -> true }
            setOnClickListener(null)
        }
        act.findViewById<View>(R.id.teacherSendBackButton)?.apply {
            isEnabled = false
            setOnTouchListener { _, _ -> true }
        }
        act.findViewById<BottomNavigationView>(R.id.bottomNavigationID)?.apply {
            isClickable = false
            isFocusable = false
            isEnabled = false
            setOnTouchListener { _, _ -> true }
            setOnItemSelectedListener(null)
            menu.setGroupEnabled(0, false)
        }
        if (act is MainActivity) {
            act.setMapFragmentAskQuestionInteractionBlocked(true)
        }
        logBottomNavState(act, "applyBlock")
    }

    private fun applyUnlock(act: Activity) {
        Log.d(LOG_TAG, "applyUnlock (depth→0 → UI unlocked)")
        act.findViewById<View>(R.id.currencyPanel)?.apply {
            isClickable = true
            isFocusable = true
            setOnTouchListener(null)
        }
        act.findViewById<View>(R.id.energyText)?.apply {
            isClickable = true
            isFocusable = true
            setOnTouchListener(null)
        }
        act.findViewById<View>(R.id.energyIcon)?.apply {
            isClickable = true
            isFocusable = true
            setOnTouchListener(null)
        }
        act.findViewById<View>(R.id.teacherSendBackButton)?.apply {
            isEnabled = true
            setOnTouchListener(null)
        }
        act.findViewById<BottomNavigationView>(R.id.bottomNavigationID)?.apply {
            isClickable = true
            isFocusable = true
            isEnabled = true
            setOnTouchListener(null)
            menu.setGroupEnabled(0, true)
        }
        if (act is MainActivity) {
            act.setupClickListeners()
            act.setMapFragmentAskQuestionInteractionBlocked(false)
        }
        logBottomNavState(act, "applyUnlock")
    }

    private fun logBottomNavState(act: Activity, phase: String) {
        val nav = act.findViewById<BottomNavigationView>(R.id.bottomNavigationID) ?: run {
            Log.d(LOG_TAG, "[$phase] bottomNav=null depth=$lockDepth")
            return
        }
        val menuEnabled = try {
            nav.menu.getItem(0)?.isEnabled == true
        } catch (_: Exception) {
            false
        }
        val hasTouchListener = viewHasTouchListener(nav)
        Log.d(
            LOG_TAG,
            "[$phase] depth=$lockDepth bottomNav enabled=${nav.isEnabled} clickable=${nav.isClickable} " +
                "menuItem0Enabled=$menuEnabled touchListener=$hasTouchListener " +
                "chromeLocked=${lockDepth > 0}",
        )
    }

    /** API 29+ [View.hasOnTouchListeners]; derleyici uyumu için reflection. */
    private fun viewHasTouchListener(view: View): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            View::class.java.getMethod("hasOnTouchListeners").invoke(view) as Boolean
        }.getOrDefault(false)
    }

    private fun inferCaller(): String {
        val skipPrefixes = listOf(
            MainActivityChromeBlocker::class.java.name,
            "java.lang.Thread",
            "dalvik.system.VMStack",
        )
        return Throwable().stackTrace
            .asSequence()
            .drop(1)
            .firstOrNull { frame ->
                skipPrefixes.none { frame.className.startsWith(it) }
            }
            ?.let { "${it.className.substringAfterLast('.')}.${it.methodName}" }
            ?: "unknown"
    }
}

package com.example.app

import android.app.Activity
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Sandık / görev ödülü gibi tam ekran akışlarda üst para paneli ve alt navigasyonu kilitlemek için.
 * [acquire] / [release] çiftleri iç içe geçebilir (ör. ChestFragment → MissionChestRewardFragment); sayaç 0 olunca kilit kalkar.
 */
object MainActivityChromeBlocker {

    private var lockDepth = 0

    fun acquire(activity: Activity) {
        if (lockDepth++ == 0) {
            applyBlock(activity)
        }
    }

    fun release(activity: Activity?) {
        val act = activity ?: return
        if (lockDepth <= 0) return
        if (--lockDepth == 0) {
            applyUnlock(act)
        }
    }

    private fun applyBlock(act: Activity) {
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
    }

    private fun applyUnlock(act: Activity) {
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
    }
}

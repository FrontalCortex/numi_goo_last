package com.example.app

import android.util.Log

/**
 * Abaküs sıfırlama teşhisi. Logcat filtresi: TutorialAbacusResetDbg
 *
 * Yalnızca debug derlemesinde çalışır; bkz. [TutorialBeadDiagnostics.ENABLED].
 */
object TutorialAbacusResetDiagnostics {
    const val TAG = "TutorialAbacusResetDbg"

    /** Debug'da açık, release'de kapalı. */
    val ENABLED = BuildConfig.DEBUG

    fun log(message: String) {
        if (ENABLED) Log.d(TAG, message)
    }

    fun stepFlags(step: TutorialFragment.TutorialStep): String =
        "abacusReset=${step.abacusReset} nextStepAbacusReset=${step.nextStepAbacusReset} " +
            "backAnswerNumber=${step.backAnswerNumber} answerNumber=${step.answerNumber}"

    fun stepLabel(step: TutorialFragment.TutorialStep): String =
        step.text.take(48).replace('\n', ' ')
}

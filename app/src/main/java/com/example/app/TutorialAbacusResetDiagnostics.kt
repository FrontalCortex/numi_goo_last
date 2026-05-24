package com.example.app

import android.util.Log

/**
 * Abaküs sıfırlama teşhisi — sorun netleşince [ENABLED] = false yapın.
 * Logcat filtresi: TutorialAbacusResetDbg
 */
object TutorialAbacusResetDiagnostics {
    const val TAG = "TutorialAbacusResetDbg"
    const val ENABLED = true

    fun log(message: String) {
        if (ENABLED) Log.d(TAG, message)
    }

    fun stepFlags(step: TutorialFragment.TutorialStep): String =
        "abacusReset=${step.abacusReset} nextStepAbacusReset=${step.nextStepAbacusReset} " +
            "backAnswerNumber=${step.backAnswerNumber} answerNumber=${step.answerNumber}"

    fun stepLabel(step: TutorialFragment.TutorialStep): String =
        step.text.take(48).replace('\n', ' ')
}

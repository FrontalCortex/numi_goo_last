package com.example.app

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Uygulamanın dokunma titreşimleri. Ses ve Bildirimler ekranındaki "Titreşim" ayarı
 * (`AppPrefs` / [KEY]) kapalıysa hiç titretmiyor.
 *
 * Titreşim veren bütün yerler buradan geçmeli (alt bar sekme geçişi, avatar seçimi, rozet
 * ekranı); doğrudan `performHapticFeedback` çağırmak ayarı atlar.
 */
object Haptics {
    const val KEY = "vibration_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getBoolean(KEY, true)

    /** Kısa "tık" titreşimi. */
    fun click(view: View) {
        if (!isEnabled(view.context)) return
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }
}

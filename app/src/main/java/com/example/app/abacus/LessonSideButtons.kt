package com.example.app.abacus

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import com.airbnb.lottie.LottieAnimationView

/**
 * Sağ sütundaki lottie düğmelerinin (sıfırla, atla) ortak kurulumu: BlindingLessonFragment ve ödev
 * oynatıcısı. Düğmenin ne yaptığı ekranın işi; burada yalnızca animasyonun duruşu.
 */
object LessonSideButtons {

    /** Sıfırla animasyonunun beklediği kare: ok simgesi tam görünüyor. */
    private const val RESET_IDLE_FRAME = 20
    private const val RESET_IDLE_DELAY_MS = 1000L
    /** Atla animasyonu bu kareden başlıyor (öncesi boş). */
    private const val SKIP_START_FRAME = 10

    /** Basınca animasyon oynar ve [onReset] çalışır; animasyon bitince 1 sn sonra bekleme karesine döner. */
    fun setupReset(button: LottieAnimationView, onReset: () -> Unit) {
        button.frame = RESET_IDLE_FRAME
        button.setOnClickListener {
            button.playAnimation()
            onReset()
        }
        button.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Ekran kapandıysa ölü görünüme yazılmasın.
                button.postDelayed({
                    if (button.isAttachedToWindow) button.frame = RESET_IDLE_FRAME
                }, RESET_IDLE_DELAY_MS)
            }
        })
    }

    /** Animasyonu ekran oynatıyor (atlanacak bir şey yoksa oynamasın diye). */
    fun setupSkip(button: LottieAnimationView, onSkip: () -> Unit) {
        button.setMinFrame(SKIP_START_FRAME)
        button.setOnClickListener { onSkip() }
    }
}

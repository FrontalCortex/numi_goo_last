package com.example.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * "Öğretmene sor" düğmelerinin görünümü: renkli ikon ve ara sıra üstünden geçen renk dalgası.
 * Uygulamadaki bütün askQuestionButton'lar [AskQuestionButtonBinder.bind] üzerinden buraya
 * geliyor (harita, ders abaküsü, pratik, körleme dersi, eğitim).
 *
 * ## Neden
 * Gri ikon koyu zeminde sönük kalıyordu. İkon mağazadaki Super rozetinin renklerinin açık
 * tonlarıyla boyanıyor; düğme sürekli süzüldüğü için göz alışıyordu, her [WAVE_INTERVAL_MS]'de
 * bir merkezden kenarlara sarı-mavi-kırmızı bir dalga geçip ikon eski hâline dönüyor. Önce
 * çevresine halka denendi, beğenilmedi.
 *
 * Dalga ekranın görünüm yaşam döngüsüne bağlı: ekran öndeyken (RESUMED) aralıklı oynuyor,
 * arkaya geçince duruyor. İlk dalga hemen değil bir aralık sonra; ekran açılır açılmaz
 * oynasa gözden kaçıyordu.
 */
object AskQuestionButtonStyle {

    /** Super rozeti renklerinin (1DF0A4, 3A82FF, D558FF) açık tonları. */
    private val ICON_COLORS = intArrayOf(0xFF8DF8D2.toInt(), 0xFF9CC1FF.toInt(), 0xFFEAABFF.toInt())

    /** Dalganın renkleri (açık sarı, mavi, kırmızı); önden arkaya. */
    private val WAVE_COLORS = intArrayOf(0xFFFFE27A.toInt(), 0xFF7EC8FF.toInt(), 0xFFFF8A8A.toInt())

    private const val WAVE_INTERVAL_MS = 11_000L
    private const val WAVE_DURATION_MS = 1_200L

    /**
     * @param canPlayWave Dalga sırası gelince oynasın mı (ör. haritada rehber ya da ders paneli
     *   açıkken oynamıyor). Hayırsa o dalga atlanıyor, sıradakine bakılıyor. Düğme
     *   görünmüyorken zaten oynamıyor.
     */
    fun apply(fragment: Fragment, button: ImageView, canPlayWave: () -> Boolean = { true }) {
        val base = gradientIcon(fragment.requireContext(), R.drawable.teacher_ask_ic, ICON_COLORS) ?: return
        val icon = WaveGradientIconDrawable(base, WAVE_COLORS)
        button.setImageDrawable(icon)

        var animator: ValueAnimator? = null
        val tick = object : Runnable {
            override fun run() {
                if (button.isShown && canPlayWave()) {
                    animator?.cancel()
                    animator = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = WAVE_DURATION_MS
                        interpolator = AccelerateDecelerateInterpolator()
                        addUpdateListener { icon.waveProgress = it.animatedValue as Float }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                icon.waveProgress = 0f
                            }
                        })
                        start()
                    }
                }
                button.postDelayed(this, WAVE_INTERVAL_MS)
            }
        }
        val stop = {
            button.removeCallbacks(tick)
            animator?.cancel()
            animator = null
            icon.waveProgress = 0f
        }

        // Görünüm henüz yoksa viewLifecycleOwner istisna atar; o durumda fragment'ın kendi yaşam
        // döngüsü kullanılıyor (yalnızca dalganın başlayıp durduğu an değişir).
        val owner: LifecycleOwner = if (fragment.view != null) fragment.viewLifecycleOwner else fragment
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                stop()
                button.postDelayed(tick, WAVE_INTERVAL_MS)
            }

            override fun onPause(owner: LifecycleOwner) = stop()
        })
    }
}

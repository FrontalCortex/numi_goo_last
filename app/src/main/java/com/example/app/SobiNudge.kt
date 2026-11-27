package com.example.app

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * Sobi'nin kendiliğinden açılan konuşma balonu: bir düğmenin solunda, kuyruğu düğmeye bakıyor,
 * birkaç saniye sonra kendiliğinden kapanıyor. Dokununca [show]'a verilen iş yapılıyor (Sobi açılır).
 *
 * Neden: sağ üstteki küçük Sobi kafasını bir çocuk görmeyebilir; takılan çocuğa yardımı biz
 * götürüyoruz (kullanıcı kararı 28.10.2026). Ne zaman açılacağına TutorialFragment karar veriyor.
 *
 * Balon kökün (ConstraintLayout ya da FrameLayout) en üstüne ekleniyor ve konumu dokunma anındaki
 * çapa görünümün ekrandaki yerinden hesaplanıyor; çapa başka bir panelin içinde olabilir (yanlış
 * cevap paneli). Panelin kayma animasyonu bitmeden çağrılmamalı.
 */
class SobiNudge(private val root: ViewGroup) {

    private val handler = Handler(Looper.getMainLooper())
    private var bubble: View? = null

    val isShowing: Boolean get() = bubble != null

    fun show(anchor: View, text: String, onTap: () -> Unit) {
        hide()
        if (!anchor.isShown || anchor.width == 0) return
        val view = LayoutInflater.from(root.context).inflate(R.layout.view_sobi_nudge, root, false) as TextView
        view.text = text
        view.alpha = 0f
        view.setOnClickListener {
            hide()
            onTap()
        }
        val params = when (root) {
            is ConstraintLayout -> ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            }
            else -> FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        root.addView(view, params)
        view.bringToFront()
        bubble = view
        view.post {
            if (bubble !== view) return@post
            val a = IntArray(2).also { anchor.getLocationInWindow(it) }
            val r = IntArray(2).also { root.getLocationInWindow(it) }
            val gap = 4 * root.resources.displayMetrics.density
            view.translationX = (a[0] - r[0]) - view.width - gap
            view.translationY = (a[1] - r[1]) + (anchor.height - view.height) / 2f
            view.scaleX = 0.85f
            view.scaleY = 0.85f
            view.pivotX = view.width.toFloat()
            view.pivotY = view.height / 2f
            view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).start()
        }
        handler.postDelayed({ hide() }, VISIBLE_MS)
    }

    fun hide() {
        handler.removeCallbacksAndMessages(null)
        val view = bubble ?: return
        bubble = null
        view.animate().cancel()
        view.animate().alpha(0f).setDuration(180).withEndAction { root.removeView(view) }.start()
    }

    private companion object {
        const val VISIBLE_MS = 5000L
    }
}

package com.example.app.homework

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View

/**
 * Duolingo tarzı göçen düğme: basılıyken görünüm dudak kalınlığı kadar aşağı iner, bırakınca
 * geri çıkar. Dudağı çizen ve basılıyken gizleyen arka plan (ör. bg_hw_chip) ile birlikte
 * kullanılıyor. Tıklama olayını yutmuyor; setOnClickListener aynen çalışıyor.
 *
 * Basılı durumu da burada hemen veriyoruz: kaydırılan bir ekranda Android onu ~100 ms gecikmeyle
 * veriyor; o arada düğme inmiş ama dudak hâlâ çiziliyor olurdu (dudak sınırın dışına taşardı).
 */
object DepthPress {
    private const val DEPTH_DP = 4f

    @SuppressLint("ClickableViewAccessibility")
    fun attach(vararg views: View) {
        views.forEach { view ->
            val depth = DEPTH_DP * view.resources.displayMetrics.density
            view.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> press(v, v.isEnabled, depth)
                    // UP'da tıklama, görünümün kendi onTouchEvent'inde basılı duruma bakılarak veriliyor;
                    // basılı durumu ondan sonra bırakıyoruz ki tıklama kaybolmasın.
                    MotionEvent.ACTION_UP -> v.post { press(v, false, depth) }
                    MotionEvent.ACTION_CANCEL -> press(v, false, depth)
                    MotionEvent.ACTION_MOVE -> {
                        // Parmak düğmenin dışına kayınca düğme de çıksın.
                        val inside = event.x >= 0 && event.y >= 0 && event.x <= v.width && event.y <= v.height
                        press(v, inside && v.isEnabled, depth)
                    }
                }
                false
            }
        }
    }

    private fun press(v: View, down: Boolean, depth: Float) {
        v.isPressed = down
        v.translationY = if (down) depth else 0f
    }
}

package com.example.app.homework

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View

/**
 * Duolingo tarzı göçen düğme: basılıyken görünüm dudak kalınlığı kadar aşağı iner, bırakınca
 * geri çıkar. Dudağı çizen ve basılıyken gizleyen arka plan (ör. bg_hw_chip) ile birlikte
 * kullanılıyor. Tıklama olayını yutmuyor; setOnClickListener aynen çalışıyor.
 */
object DepthPress {
    private const val DEPTH_DP = 4f

    @SuppressLint("ClickableViewAccessibility")
    fun attach(vararg views: View) {
        views.forEach { view ->
            val depth = DEPTH_DP * view.resources.displayMetrics.density
            view.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> if (v.isEnabled) v.translationY = depth
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.translationY = 0f
                    MotionEvent.ACTION_MOVE -> {
                        // Parmak düğmenin dışına kayınca basılı durum düşüyor; düğme de çıksın.
                        val inside = event.x >= 0 && event.y >= 0 && event.x <= v.width && event.y <= v.height
                        v.translationY = if (inside && v.isEnabled) depth else 0f
                    }
                }
                false
            }
        }
    }
}

package com.example.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Dikey renk paleti şeridi.
 * Üstten alta gradient (siyah -> ... -> beyaz). Dokunulan Y konumuna göre renk seçilir.
 */
class DrawingColorStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    interface OnColorSelectedListener {
        fun onColorSelected(color: Int)
    }

    var listener: OnColorSelectedListener? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        if (h <= 0f) return
        // Her çizimde dikey gradient: üst (y=0) -> alt (y=h), renkler diklemesine
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            COLORS, null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), h, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP -> {
                val color = pickColorAtY(event.y)
                listener?.onColorSelected(color)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun pickColorAtY(y: Float): Int {
        if (height <= 0) return COLORS.first()
        val clampedY = min(max(y, 0f), height.toFloat())
        val ratio = clampedY / height.toFloat()
        // COLORS arasında lineer interpolasyon
        val position = ratio * (COLORS.size - 1)
        val index = position.toInt().coerceIn(0, COLORS.size - 2)
        val fraction = position - index
        val c1 = COLORS[index]
        val c2 = COLORS[index + 1]
        return interpolateColor(c1, c2, fraction)
    }

    private fun interpolateColor(c1: Int, c2: Int, fraction: Float): Int {
        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF
        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF
        val r = (r1 + ((r2 - r1) * fraction)).toInt().coerceIn(0, 255)
        val g = (g1 + ((g2 - g1) * fraction)).toInt().coerceIn(0, 255)
        val b = (b1 + ((b2 - b1) * fraction)).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    companion object {
        // Genişletilmiş palet: siyah -> gökkuşağı -> beyaz
        private val COLORS = intArrayOf(
            0xFF000000.toInt(), // Siyah
            0xFFFF0000.toInt(), // Kırmızı
            0xFFFFA500.toInt(), // Turuncu
            0xFFFFFF00.toInt(), // Sarı
            0xFF00FF00.toInt(), // Yeşil
            0xFF00FFFF.toInt(), // Camgöbeği
            0xFF0000FF.toInt(), // Mavi
            0xFFFF00FF.toInt(), // Mor
            0xFFFFFFFF.toInt()  // Beyaz
        )
    }
}


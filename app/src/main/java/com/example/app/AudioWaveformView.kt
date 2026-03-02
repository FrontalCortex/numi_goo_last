package com.example.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

/**
 * Ses çubuğunda ilerlemeyi dikey çizgiler (waveform) ile gösterir.
 * progress 0f–1f arası; çizgilerin doluluk oranı buna göre güncellenir.
 */
class AudioWaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val unfilledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFAAAAAA.toInt()
        style = Paint.Style.FILL
    }

    private val filledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lesson_completed)
        style = Paint.Style.FILL
    }

    private val lineCount = 40
    private val lineWidth = 3f
    private val lineGap = 2f

    /** 0f = başlangıç, 1f = bitti */
    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val lineHeights = FloatArray(lineCount) { 0.35f + (it % 4) * 0.2f }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val totalWidth = lineCount * (lineWidth + lineGap) - lineGap
        var x = (w - totalWidth) / 2f
        val centerY = h / 2f

        for (i in 0 until lineCount) {
            val barHeight = (h * 0.65f) * lineHeights[i]
            val top = centerY - barHeight / 2f
            val bottom = centerY + barHeight / 2f
            val lineStart = i / lineCount.toFloat()
            val lineEnd = (i + 1) / lineCount.toFloat()
            if (progress <= lineStart) {
                canvas.drawRect(x, top, x + lineWidth, bottom, unfilledPaint)
            } else if (progress >= lineEnd) {
                canvas.drawRect(x, top, x + lineWidth, bottom, filledPaint)
            } else {
                val t = (progress - lineStart) / (lineEnd - lineStart)
                val fillBottom = top + (bottom - top) * t
                canvas.drawRect(x, top, x + lineWidth, fillBottom, filledPaint)
                canvas.drawRect(x, fillBottom, x + lineWidth, bottom, unfilledPaint)
            }
            x += lineWidth + lineGap
        }
    }
}



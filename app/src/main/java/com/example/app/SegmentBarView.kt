package com.example.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Dilimli düz ilerleme çubuğu: her adım bir dilim, aralarında boşluk; dolu dilimler [fillColor],
 * boşlar [trackColor]. Haritadaki ders kartının adım çubuğu (eski daire halkasının
 * [CircleProgressBar] düz karşılığı; aynı adlı yöntemler, böylece dolma animasyonu aynen taşındı).
 * [setSegmentProgress] kesirli değer alıyor: 2,4 → iki dilim dolu, üçüncünün %40'ı.
 */
class SegmentBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var segmentCount = 1
    private var filled = 0f
    private val gapPx = 4f * resources.displayMetrics.density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val rect = RectF()
    private val clip = Path()

    var fillColor: Int
        get() = fillPaint.color
        set(value) {
            fillPaint.color = value
            invalidate()
        }

    var trackColor: Int
        get() = trackPaint.color
        set(value) {
            trackPaint.color = value
            invalidate()
        }

    /** Dilim sayısı ve dolu dilim sayısı (animasyonsuz). */
    fun setSegmentState(segmentCount: Int, completedSegments: Int) {
        this.segmentCount = segmentCount.coerceAtLeast(1)
        filled = completedSegments.coerceIn(0, this.segmentCount).toFloat()
        invalidate()
    }

    /** Dolu dilim miktarı, kesirli (dolma animasyonu için). */
    fun setSegmentProgress(value: Float) {
        filled = value.coerceIn(0f, segmentCount.toFloat())
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        if (width <= 0 || h <= 0f) return
        val n = segmentCount
        val w = (width - gapPx * (n - 1)) / n
        val r = h / 2f
        for (i in 0 until n) {
            val left = i * (w + gapPx)
            rect.set(left, 0f, left + w, h)
            canvas.drawRoundRect(rect, r, r, trackPaint)
            val part = (filled - i).coerceIn(0f, 1f)
            if (part <= 0f) continue
            // Kısmi dolum: dilimin yuvarlak hattına kırpılmış dikdörtgen.
            canvas.save()
            clip.reset()
            clip.addRoundRect(rect, r, r, Path.Direction.CW)
            canvas.clipPath(clip)
            canvas.drawRect(left, 0f, left + w * part, h, fillPaint)
            canvas.restore()
        }
    }
}

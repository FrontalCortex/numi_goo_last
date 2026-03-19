package com.example.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Kalemin anlık kalınlığını ve rengini gösteren küçük nokta.
 */
class StrokePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var radiusPx: Float = 4f

    fun setColor(color: Int) {
        paint.color = color
        invalidate()
    }

    fun setStrokeWidth(widthPx: Float) {
        // Önizleme noktası, çizimde kullanılan strokeWidth ile birebir uyumlu olsun:
        // strokeWidth bir "çap" gibi düşünülür => radius = width/2.
        // Sadece view sınırlarına sığması için maksimumu mevcut boyuta göre clamp edeceğiz.
        radiusPx = (widthPx / 2f).coerceAtLeast(1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxR = (minOf(width, height) / 2f).coerceAtLeast(1f)
        canvas.drawCircle(cx, cy, radiusPx.coerceAtMost(maxR), paint)
    }
}


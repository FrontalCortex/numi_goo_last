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
        // Çap yaklaşık strok kalınlığına yakın olsun; min/maks ile sınırla
        val r = (widthPx / 2f).coerceIn(2f, 24f)
        radiusPx = r
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, radiusPx, paint)
    }
}


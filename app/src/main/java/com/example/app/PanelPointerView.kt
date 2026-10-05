package com.example.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * Ders panelini tıklanan karta bağlayan küçük ok (üçgen). İçi panel zemininin rengi, iki eğik
 * kenarı panelin çerçeve rengi; taban kenarı çizilmiyor ki panelin çerçevesiyle birleşsin
 * (ok panelin kenarını 2dp örterek oturuyor). [pointUp] true ise yukarıyı (panel kartın
 * altında), false ise aşağıyı gösteriyor.
 */
class PanelPointerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPath = Path()
    private val edgePath = Path()

    var pointUp = true
        set(value) {
            field = value
            invalidate()
        }

    fun setColors(fill: Int, stroke: Int) {
        fillPaint.color = fill
        strokePaint.color = stroke
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val s = strokePaint.strokeWidth / 2f
        val tipY = if (pointUp) s else h - s
        val baseY = if (pointUp) h else 0f
        fillPath.reset()
        fillPath.moveTo(0f, baseY)
        fillPath.lineTo(w / 2f, tipY)
        fillPath.lineTo(w, baseY)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)
        edgePath.reset()
        edgePath.moveTo(s, baseY)
        edgePath.lineTo(w / 2f, tipY)
        edgePath.lineTo(w - s, baseY)
        canvas.drawPath(edgePath, strokePaint)
    }
}

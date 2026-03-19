package com.example.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Basit dikey slider: track + thumb (yuvarlak).
 * - Üst = max, alt = 0.
 * - Thumb merkezi, parmağın Y koordinatıyla hizalanır.
 */
class VerticalSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onStartTrackingTouch()
        fun onProgressChanged(progress: Int, fromUser: Boolean)
        fun onStopTrackingTouch()
    }

    var listener: Listener? = null

    var max: Int = 100
        set(value) {
            field = max(1, value)
            progress = progress.coerceIn(0, field)
            invalidate()
        }

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, max)
            invalidate()
        }

    var trackColor: Int = 0xFFFFFFFF.toInt()
        set(value) {
            field = value
            invalidate()
        }

    var progressColor: Int = 0xFF00C6D7.toInt()
        set(value) {
            field = value
            invalidate()
        }

    var thumbColor: Int = 0xFF00C6D7.toInt()
        set(value) {
            field = value
            invalidate()
        }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(3f)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(3f)
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val thumbRadius = dp(6.5f)

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val top = paddingTop.toFloat() + thumbRadius
        val bottom = (height - paddingBottom).toFloat() - thumbRadius
        if (bottom <= top) return

        trackPaint.color = trackColor
        progressPaint.color = progressColor
        thumbPaint.color = thumbColor

        // Track (tam çizgi)
        canvas.drawLine(cx, top, cx, bottom, trackPaint)

        // Progress çizgisi: alttan başlayıp thumb'a kadar
        val thumbY = progressToY(progress, top, bottom)
        canvas.drawLine(cx, bottom, cx, thumbY, progressPaint)

        // Thumb
        canvas.drawCircle(cx, thumbY, thumbRadius, thumbPaint)
    }

    private fun progressToY(p: Int, top: Float, bottom: Float): Float {
        val ratio = p.toFloat() / max.toFloat() // 0..1
        // üst=max: ratio=1 => y=top, ratio=0 => y=bottom
        return bottom - ratio * (bottom - top)
    }

    private fun yToProgress(y: Float, top: Float, bottom: Float): Int {
        val clamped = y.coerceIn(top, bottom)
        val ratio = (bottom - clamped) / (bottom - top) // 0..1
        return (ratio * max.toFloat()).roundToInt().coerceIn(0, max)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        val top = paddingTop.toFloat() + thumbRadius
        val bottom = (height - paddingBottom).toFloat() - thumbRadius
        if (bottom <= top) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                listener?.onStartTrackingTouch()
                val newP = yToProgress(event.y, top, bottom)
                if (progress != newP) {
                    progress = newP
                    listener?.onProgressChanged(progress, true)
                } else {
                    // yine de kullanıcı etkileşimi olarak bildir
                    listener?.onProgressChanged(progress, true)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val newP = yToProgress(event.y, top, bottom)
                if (progress != newP) {
                    progress = newP
                    listener?.onProgressChanged(progress, true)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val newP = yToProgress(event.y, top, bottom)
                if (progress != newP) {
                    progress = newP
                    listener?.onProgressChanged(progress, true)
                }
                listener?.onStopTrackingTouch()
                performClick()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()
}


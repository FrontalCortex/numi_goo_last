package com.example.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * Rakam büyümesi bittikten sonra sadece radyal çizgiler: önce sabit uzunluğa büyür,
 * sonra o uzunlukta dışarı kayar (uzayarak uzaklaşmaz).
 */
class BalloonPopBurstView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var burstProgress = -1f
    private var burstAnimator: ValueAnimator? = null

    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = 0xFFFFFFFF.toInt()
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun cancelBurst() {
        burstAnimator?.cancel()
        burstAnimator = null
        burstProgress = -1f
        invalidate()
        visibility = INVISIBLE
    }

    fun playBurst() {
        burstAnimator?.cancel()
        burstProgress = 0f
        visibility = VISIBLE
        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = BURST_MS
            interpolator = LinearInterpolator()
            addUpdateListener {
                burstProgress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    burstProgress = -1f
                    invalidate()
                    visibility = INVISIBLE
                    burstAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    burstProgress = -1f
                    invalidate()
                    visibility = INVISIBLE
                    burstAnimator = null
                }
            })
        }
        burstAnimator = anim
        anim.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val p = burstProgress
        if (p < 0f) return

        val cx = width * 0.5f
        val cy = height * 0.5f
        val density = resources.displayMetrics.density

        val maxLen = 38f * density
        val slideMax = 32f * density
        rayPaint.strokeWidth = 4.5f * density

        val growEnd = GROW_FRACTION
        val (segmentLen, slide) = if (p <= growEnd) {
            val t = (p / growEnd).coerceIn(0f, 1f)
            maxLen * t to 0f
        } else {
            val t = ((p - growEnd) / (1f - growEnd)).coerceIn(0f, 1f)
            maxLen to slideMax * t
        }

        val alpha = ((1f - p * 0.9f) * 255).toInt().coerceIn(0, 255)
        rayPaint.alpha = alpha

        val rayCount = 14
        for (i in 0 until rayCount) {
            val angle = (i * Math.PI * 2 / rayCount).toFloat()
            val ca = cos(angle)
            val sa = sin(angle)
            val x1 = cx + ca * slide
            val y1 = cy + sa * slide
            val x2 = x1 + ca * segmentLen
            val y2 = y1 + sa * segmentLen
            canvas.drawLine(x1, y1, x2, y2, rayPaint)
        }
    }

    companion object {
        private const val BURST_MS = 360L
        /** İlk bu kadarlık sürede çizgi uzunluğu 0 → maxLen büyür; sonra kayma. */
        private const val GROW_FRACTION = 0.42f
    }
}

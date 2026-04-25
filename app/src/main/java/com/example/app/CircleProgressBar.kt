package com.example.app
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class CircleProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var strokeWidth = 4f
    var progress = 0f
    private var min = 0
    private var max = 100
    private val startAngle = -90
    private var progressBarColor = Color.DKGRAY
    private var backgroundBarColor = Color.LTGRAY
    private var ringInset = 0f
    private var segmentCount = 0
    private var completedSegments = 0
    private var completedSegmentsExact = 0f
    private var segmentGapAngle = 10f
    private lateinit var rectF: RectF
    private lateinit var backgroundPaint: Paint
    private lateinit var foregroundPaint: Paint
    private lateinit var starPaint: Paint
    private data class StarParticle(
        val x: Float,
        val y: Float,
        val vx: Float,
        val vy: Float,
        val createdAtMs: Long,
        val durationMs: Long,
        val size: Float,
    )
    private val starParticles = mutableListOf<StarParticle>()
    private var lastTipPoint: PointF? = null
    private var lastTrailEmitPoint: PointF? = null
    private var lastTrailEmitMs: Long = 0L

    init {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CircleProgressBar,
            0, 0
        )
        try {
            strokeWidth = typedArray.getDimension(R.styleable.CircleProgressBar_progressBarThickness, strokeWidth)
            progress = typedArray.getFloat(R.styleable.CircleProgressBar_progress, progress)
            progressBarColor = typedArray.getInt(R.styleable.CircleProgressBar_progressbarColor, progressBarColor)
            backgroundBarColor = typedArray.getInt(R.styleable.CircleProgressBar_backgroundBarColor, backgroundBarColor)
            min = typedArray.getInt(R.styleable.CircleProgressBar_min, min)
            max = typedArray.getInt(R.styleable.CircleProgressBar_max, max)
        } finally {
            typedArray.recycle()
        }
        ringInset = 0f

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundBarColor
            style = Paint.Style.STROKE
            strokeWidth = this@CircleProgressBar.strokeWidth
            strokeCap = Paint.Cap.ROUND
        }

        foregroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = progressBarColor
            style = Paint.Style.STROKE
            strokeWidth = this@CircleProgressBar.strokeWidth
            strokeCap = Paint.Cap.ROUND
        }

        starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD600")
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val minSize = Math.min(width, height)
        setMeasuredDimension(minSize, minSize)
        val inset = strokeWidth / 2 + ringInset
        rectF = RectF(inset, inset, minSize - inset, minSize - inset)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (segmentCount >= 1) {
            drawSegmentedRing(canvas)
        } else {
            drawContinuousRing(canvas)
        }
        drawStarParticles(canvas)
    }

    private fun drawContinuousRing(canvas: Canvas) {
        canvas.drawOval(rectF, backgroundPaint)
        val clampedProgress = progress.coerceIn(min.toFloat(), max.toFloat())
        val angle = 360f * clampedProgress / max.toFloat()
        canvas.drawArc(rectF, startAngle.toFloat(), angle, false, foregroundPaint)
        lastTipPoint = getProgressTipPointExact(completedSegmentsExact)
    }

    private fun drawSegmentedRing(canvas: Canvas) {
        val safeCount = segmentCount.coerceAtLeast(1)
        val sliceAngle = 360f / safeCount
        val safeGap = segmentGapAngle.coerceIn(0f, sliceAngle - 1f)
        val segmentSweep = (sliceAngle - safeGap).coerceAtLeast(1f)
        val exactFilled = completedSegmentsExact.coerceIn(0f, safeCount.toFloat())

        for (index in 0 until safeCount) {
            val start = startAngle + index * sliceAngle + (safeGap / 2f)
            val remaining = exactFilled - index
            when {
                remaining >= 1f -> {
                    canvas.drawArc(rectF, start.toFloat(), segmentSweep, false, foregroundPaint)
                }
                remaining <= 0f -> {
                    canvas.drawArc(rectF, start.toFloat(), segmentSweep, false, backgroundPaint)
                }
                else -> {
                    val filledSweep = segmentSweep * remaining
                    canvas.drawArc(rectF, start.toFloat(), filledSweep, false, foregroundPaint)
                    canvas.drawArc(
                        rectF,
                        (start + filledSweep).toFloat(),
                        segmentSweep - filledSweep,
                        false,
                        backgroundPaint,
                    )
                }
            }
        }
        lastTipPoint = getProgressTipPointExact(exactFilled)
    }

    fun setProgressValue(progress: Float) {
        this.progress = progress
        invalidate()  // Görünümün yeniden çizilmesini sağlıyoruz
    }

    fun setProgressColor(color: Int) {
        foregroundPaint.color = color
        invalidate()
    }

    fun setBackgroundRingColor(color: Int) {
        backgroundPaint.color = color
        invalidate()
    }

    fun setSegmentState(segmentCount: Int, completedSegments: Int) {
        this.segmentCount = segmentCount.coerceAtLeast(0)
        this.completedSegments = completedSegments.coerceAtLeast(0)
        this.completedSegmentsExact = this.completedSegments.toFloat()
        this.lastTipPoint = getProgressTipPointExact(this.completedSegmentsExact)
        if (lastTipPoint != null) {
            lastTrailEmitPoint = PointF(lastTipPoint!!.x, lastTipPoint!!.y)
        }
        invalidate()
    }

    fun setSegmentProgress(completedSegments: Float) {
        this.completedSegmentsExact = completedSegments.coerceAtLeast(0f)
        this.lastTipPoint = getProgressTipPointExact(this.completedSegmentsExact)
        maybeEmitTrailStarAtTip()
        invalidate()
    }

    fun setSegmentGapAngle(gapAngle: Float) {
        this.segmentGapAngle = gapAngle
        invalidate()
    }

    fun setRingInset(insetPx: Float) {
        ringInset = insetPx.coerceAtLeast(0f)
        requestLayout()
    }

    fun emitStarsAtProgressTip(exactCompletedSegments: Float) {
        val tip = lastTipPoint ?: getProgressTipPointExact(exactCompletedSegments) ?: return
        val now = SystemClock.uptimeMillis()
        repeat(2) {
            val angle = Random.nextDouble(-155.0, -25.0)
            val speed = Random.nextDouble(18.0, 34.0).toFloat()
            val rad = Math.toRadians(angle)
            val vx = (cos(rad) * speed).toFloat()
            val vy = (sin(rad) * speed).toFloat()
            starParticles.add(
                StarParticle(
                    x = tip.x,
                    y = tip.y,
                    vx = vx,
                    vy = vy,
                    createdAtMs = now,
                    durationMs = 420L,
                    size = 16f + Random.nextFloat() * 4f,
                ),
            )
        }
        invalidate()
    }

    private fun maybeEmitTrailStarAtTip() {
        val tip = lastTipPoint ?: return
        val now = SystemClock.uptimeMillis()
        val prev = lastTrailEmitPoint
        if (prev == null) {
            lastTrailEmitPoint = PointF(tip.x, tip.y)
            lastTrailEmitMs = now
            return
        }
        val dx = tip.x - prev.x
        val dy = tip.y - prev.y
        val moved = kotlin.math.sqrt(dx * dx + dy * dy)
        val enoughMove = moved >= 3.5f
        val enoughTime = now - lastTrailEmitMs >= 90L
        if (enoughMove && enoughTime) {
            emitStarsAtProgressTip(completedSegmentsExact)
            lastTrailEmitPoint = PointF(tip.x, tip.y)
            lastTrailEmitMs = now
        }
    }

    private fun drawStarParticles(canvas: Canvas) {
        if (starParticles.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val iter = starParticles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            val t = ((now - p.createdAtMs).toFloat() / p.durationMs.toFloat()).coerceIn(0f, 1f)
            if (t >= 1f) {
                iter.remove()
                continue
            }
            val x = p.x + p.vx * t
            val y = p.y + p.vy * t + 14f * t * t
            starPaint.alpha = ((1f - t) * 255f).toInt().coerceIn(0, 255)
            starPaint.textSize = p.size
            canvas.drawText("★", x, y + (p.size / 3f), starPaint)
        }
        if (starParticles.isNotEmpty()) {
            postInvalidateOnAnimation()
        }
    }

    fun getProgressTipPoint(forCompletedSegments: Int = completedSegments): PointF? {
        if (!::rectF.isInitialized) return null
        val cx = rectF.centerX()
        val cy = rectF.centerY()
        val radius = rectF.width() / 2f

        val endAngle = if (segmentCount >= 1) {
            val safeCount = segmentCount.coerceAtLeast(1)
            val safeFilled = forCompletedSegments.coerceIn(0, safeCount)
            val sliceAngle = 360f / safeCount
            val safeGap = segmentGapAngle.coerceIn(0f, sliceAngle - 1f)
            val segmentSweep = (sliceAngle - safeGap).coerceAtLeast(1f)
            val segmentIndex = (safeFilled - 1).coerceAtLeast(0)
            (startAngle + segmentIndex * sliceAngle + (safeGap / 2f) + segmentSweep).toFloat()
        } else {
            val clampedProgress = progress.coerceIn(min.toFloat(), max.toFloat())
            startAngle + (360f * clampedProgress / max.toFloat())
        }

        val rad = Math.toRadians(endAngle.toDouble())
        return PointF(
            (cx + radius * cos(rad)).toFloat(),
            (cy + radius * sin(rad)).toFloat(),
        )
    }

    fun getProgressTipPointExact(exactCompletedSegments: Float): PointF? {
        if (!::rectF.isInitialized) return null
        val cx = rectF.centerX()
        val cy = rectF.centerY()
        val radius = rectF.width() / 2f

        val endAngle = if (segmentCount >= 1) {
            val safeCount = segmentCount.coerceAtLeast(1)
            val safeExact = exactCompletedSegments.coerceIn(0f, safeCount.toFloat())
            val sliceAngle = 360f / safeCount
            val safeGap = segmentGapAngle.coerceIn(0f, sliceAngle - 1f)
            val segmentSweep = (sliceAngle - safeGap).coerceAtLeast(1f)
            val fullSegments = safeExact.toInt().coerceAtMost(safeCount)
            val partialRaw = (safeExact - fullSegments).coerceIn(0f, 1f)
            val (segmentIndex, localProgress) = when {
                fullSegments >= safeCount -> (safeCount - 1) to 1f
                partialRaw == 0f && fullSegments > 0 -> (fullSegments - 1) to 1f
                else -> fullSegments.coerceAtLeast(0) to partialRaw
            }
            val baseStart = startAngle + segmentIndex * sliceAngle + (safeGap / 2f)
            (baseStart + segmentSweep * localProgress).toFloat()
        } else {
            val clampedProgress = progress.coerceIn(min.toFloat(), max.toFloat())
            startAngle + (360f * clampedProgress / max.toFloat())
        }

        val rad = Math.toRadians(endAngle.toDouble())
        return PointF(
            (cx + radius * cos(rad)).toFloat(),
            (cy + radius * sin(rad)).toFloat(),
        )
    }
}
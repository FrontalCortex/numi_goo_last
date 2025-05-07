package com.example.numigoo
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CircleProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Gerekli değişkenler
    private var strokeWidth = 4f
    var progress = 0f
    private var min = 0
    private var max = 100
    private val startAngle = -90
    private var progressBarColor = Color.DKGRAY
    private var backgroundBarColor = Color.LTGRAY
    private lateinit var rectF: RectF
    private lateinit var backgroundPaint: Paint
    private lateinit var foregroundPaint: Paint

    init {
        init(context, attrs)
    }

    // XML'den gelen değerleri alıp başlatıyoruz
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

        // Paint objelerini oluşturuyoruz
        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundBarColor
            style = Paint.Style.STROKE
            strokeWidth = this@CircleProgressBar.strokeWidth
        }

        foregroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = progressBarColor
            style = Paint.Style.STROKE
            strokeWidth = this@CircleProgressBar.strokeWidth
            strokeCap = Paint.Cap.ROUND
        }
    }

    // Alpha değerini ayarlıyoruz
    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    // Görünümün boyutlarını ölçüyoruz
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val minSize = Math.min(width, height)
        setMeasuredDimension(minSize, minSize)
        rectF = RectF(strokeWidth / 2, strokeWidth / 2, minSize - strokeWidth / 2, minSize - strokeWidth / 2)
    }

    // Görünümü çiziyoruz
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawOval(rectF, backgroundPaint)
        val angle = 360 * progress / max
        canvas.drawArc(rectF, startAngle.toFloat(), angle, false, foregroundPaint)
    }

    // Progress değerini ayarlayan setter metodu
    fun setProgressValue(progress: Float) {
        this.progress = progress
        invalidate()  // Görünümün yeniden çizilmesini sağlıyoruz
    }

    fun setProgressColor(color: Int) {
        foregroundPaint.color = color
        invalidate()
    }
}
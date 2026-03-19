package com.example.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

/**
 * Ekran kaydı sırasında çizim yapmak için kullanılan şeffaf overlay view.
 * Kalem modu açıkken dokunuşları tüketir ve Path listesi halinde stroke'ları çizer.
 */
class DrawingOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Stroke(val path: Path, val paint: Paint)

    private val strokes = mutableListOf<Stroke>()
    private var currentPath: Path? = null
    private var currentPaint: Paint = createPaint(DEFAULT_COLOR, DEFAULT_STROKE_WIDTH)

    private var isDrawingEnabled: Boolean = false
    private val touchSlop: Float = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var didMove: Boolean = false

    fun setDrawingEnabled(enabled: Boolean) {
        isDrawingEnabled = enabled
        // Çizim kapansa da mevcut çizimler ekranda kalabilir.
        // Eğer tamamen gizlemek istersek visibility ile yönetilebilir.
    }

    fun setStrokeColor(color: Int) {
        currentPaint = createPaint(color, currentPaint.strokeWidth)
    }

    fun setStrokeWidth(widthPx: Float) {
        val w = widthPx.coerceIn(MIN_STROKE_WIDTH, MAX_STROKE_WIDTH)
        currentPaint = createPaint(currentPaint.color, w)
    }

    fun undoLastStroke() {
        if (strokes.isNotEmpty()) {
            // Android'in kullandığı Java sürümünde List.removeLast() yok; son elemanı elle sil.
            strokes.removeAt(strokes.size - 1)
            invalidate()
        }
    }

    fun clearAllStrokes() {
        strokes.clear()
        currentPath = null
        invalidate()
    }

    private fun createPaint(color: Int, width: Float): Paint {
        return Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = width
            isAntiAlias = true
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (stroke in strokes) {
            canvas.drawPath(stroke.path, stroke.paint)
        }
        currentPath?.let {
            canvas.drawPath(it, currentPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingEnabled) {
            // Çizim modu kapalıyken dokunuşları tüketme; alttaki UI'ya geçsin.
            return false
        }

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                downY = y
                didMove = false
                currentPath = Path().apply {
                    moveTo(x, y)
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!didMove) {
                    val dx = x - downX
                    val dy = y - downY
                    if ((dx * dx + dy * dy) >= (touchSlop * touchSlop)) {
                        didMove = true
                    }
                }
                currentPath?.lineTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentPath?.let { path ->
                    // Sadece "tap" yapıldıysa (hiç sürükleme yoksa), Path tek nokta kalır ve çoğu zaman çizilmez.
                    // Çok küçük bir segment ekleyerek yuvarlak cap ile nokta gibi görünmesini sağlarız.
                    if (!didMove) {
                        path.lineTo(downX + 0.01f, downY + 0.01f)
                    }
                    // Her kaldırmada ayrı bir stroke kaydet
                    strokes.add(Stroke(Path(path), Paint(currentPaint)))
                }
                currentPath = null
                invalidate()
                return true
            }
        }
        return true
    }

    companion object {
        private const val DEFAULT_COLOR = 0xFFFF0000.toInt() // Kırmızı
        private const val DEFAULT_STROKE_WIDTH = 8f
        const val MIN_STROKE_WIDTH = 2f
        const val MAX_STROKE_WIDTH = 60f
    }
}


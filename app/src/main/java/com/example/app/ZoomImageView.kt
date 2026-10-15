package com.example.app

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

/**
 * İki parmakla yakınlaştırılıp sürüklenebilen görsel; çift dokunuş 1x ↔ 3x arasında geçer.
 *
 * Hata bildirimi incelemesinde ekran görüntüsündeki küçük ayrıntıları (kayan bir yazı, yanlış
 * bir sayı) görmek için (bkz. BugReportReviewDialogFragment). Görsel her yüklendiğinde ve
 * boyut değiştiğinde ekrana sığacak şekilde ortalanır.
 */
class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatImageView(context, attrs) {

    private val drawMatrix = Matrix()
    private var baseScale = 1f
    private var zoom = 1f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val target = (zoom * detector.scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
            val factor = target / zoom
            zoom = target
            drawMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
            clampAndApply()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            drawMatrix.postTranslate(-dx, -dy)
            clampAndApply()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val target = if (zoom > 1.01f) 1f else DOUBLE_TAP_ZOOM
            val factor = target / zoom
            zoom = target
            drawMatrix.postScale(factor, factor, e.x, e.y)
            clampAndApply()
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        super.setImageDrawable(drawable)
        fitToView()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fitToView()
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Yakınlaştırılmışken üstteki kaydırma görünümü dokunuşu çalmasın.
        if (zoom > 1.01f || event.pointerCount > 1) parent?.requestDisallowInterceptTouchEvent(true)
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun fitToView() {
        val d = drawable ?: return
        val vw = width.toFloat()
        val vh = height.toFloat()
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        if (vw <= 0f || vh <= 0f || dw <= 0f || dh <= 0f) return
        baseScale = minOf(vw / dw, vh / dh)
        zoom = 1f
        drawMatrix.reset()
        drawMatrix.postScale(baseScale, baseScale)
        drawMatrix.postTranslate((vw - dw * baseScale) / 2f, (vh - dh * baseScale) / 2f)
        imageMatrix = drawMatrix
    }

    /** Görsel ekrandan kaçmasın: küçükse ortala, büyükse kenarlar dışarıda boşluk bırakmasın. */
    private fun clampAndApply() {
        val d = drawable ?: return
        val rect = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        drawMatrix.mapRect(rect)
        val vw = width.toFloat()
        val vh = height.toFloat()
        val dx = when {
            rect.width() <= vw -> (vw - rect.width()) / 2f - rect.left
            rect.left > 0f -> -rect.left
            rect.right < vw -> vw - rect.right
            else -> 0f
        }
        val dy = when {
            rect.height() <= vh -> (vh - rect.height()) / 2f - rect.top
            rect.top > 0f -> -rect.top
            rect.bottom < vh -> vh - rect.bottom
            else -> 0f
        }
        drawMatrix.postTranslate(dx, dy)
        imageMatrix = drawMatrix
    }

    private companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 6f
        const val DOUBLE_TAP_ZOOM = 3f
    }
}

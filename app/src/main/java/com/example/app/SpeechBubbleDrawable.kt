package com.example.app

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable

/**
 * Konuşma balonu arka planı: yuvarlak köşeli kutu + konuşana bakan kuyruk.
 *  - [TailSide.LEFT]: sol kenarın ortasında, sola bakan kuyruk (kayıt sorularında maskotun
 *    sağındaki soru balonu).
 *  - [TailSide.BOTTOM]: alt kenarın ortasında, aşağı bakan kuyruk (tanışma ekranlarında
 *    tavşanın üstündeki balon).
 *
 * Kutu ile kuyruk tek yol olarak birleştiriliyor ([Path.op]) ve öyle dolduruluyor/kenarlanıyor;
 * ayrı çizilseler aralarında kenar çizgisi kalırdı. Kuyruk o kenarda [tailLength] kadar yer
 * kaplıyor: içeriğin o kenardaki dolgusu bunu hesaba katmalı.
 *
 * @param tailLength Kuyruğun kutudan dışarı uzunluğu.
 * @param tailBase Kuyruğun kutuya değen tabanının genişliği.
 */
class SpeechBubbleDrawable(
    fillColor: Int,
    strokeColor: Int,
    private val strokeWidth: Float,
    private val cornerRadius: Float,
    private val tailLength: Float,
    private val tailBase: Float,
    private val tailSide: TailSide = TailSide.LEFT,
) : Drawable() {

    enum class TailSide { LEFT, BOTTOM }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = strokeColor
        strokeWidth = this@SpeechBubbleDrawable.strokeWidth
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val tail = Path()
    private val box = RectF()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val half = strokeWidth / 2f
        // Kuyruğun tabanı kutunun biraz içinde: birleşimde boşluk kalmasın.
        val inset = strokeWidth * 2f
        tail.reset()
        when (tailSide) {
            TailSide.LEFT -> {
                box.set(bounds.left + tailLength + half, bounds.top + half, bounds.right - half, bounds.bottom - half)
                val cy = box.centerY()
                tail.moveTo(box.left + inset, cy - tailBase / 2f)
                tail.lineTo(bounds.left + half, cy)
                tail.lineTo(box.left + inset, cy + tailBase / 2f)
            }
            TailSide.BOTTOM -> {
                box.set(bounds.left + half, bounds.top + half, bounds.right - half, bounds.bottom - tailLength - half)
                val cx = box.centerX()
                tail.moveTo(cx - tailBase / 2f, box.bottom - inset)
                tail.lineTo(cx, bounds.bottom - half)
                tail.lineTo(cx + tailBase / 2f, box.bottom - inset)
            }
        }
        tail.close()
        path.reset()
        path.addRoundRect(box, cornerRadius, cornerRadius, Path.Direction.CW)
        path.op(tail, Path.Op.UNION)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        strokePaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        strokePaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

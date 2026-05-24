package com.example.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView

/**
 * Kalın kırmızı stroke + beyaz fill ile sticker benzeri rakam yazımı.
 */
class BadgeRewardTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var strokeColorInt: Int = 0xFFFF8F00.toInt()

    init {
        includeFontPadding = false
    }

    override fun onDraw(canvas: Canvas) {
        val text = text?.toString().orEmpty()
        if (text.isEmpty()) {
            super.onDraw(canvas)
            return
        }

        val density = resources.displayMetrics.density
        val strokeWidthPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            5f,
            resources.displayMetrics,
        )

        strokePaint.set(paint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = strokeWidthPx
        strokePaint.strokeJoin = Paint.Join.ROUND
        strokePaint.strokeMiter = 4f
        strokePaint.color = strokeColorInt

        fillPaint.set(paint)
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = currentTextColor

        val w = width - paddingLeft - paddingRight
        val textWidth = strokePaint.measureText(text)
        val x = paddingLeft + (w - textWidth) / 2f

        val fm = fillPaint.fontMetrics
        val baseline = paddingTop + (height - paddingTop - paddingBottom) / 2f - (fm.ascent + fm.descent) / 2f

        canvas.drawText(text, x, baseline, strokePaint)
        canvas.drawText(text, x, baseline, fillPaint)
    }

    fun setStrokeColorInt(color: Int) {
        if (strokeColorInt == color) return
        strokeColorInt = color
        invalidate()
    }
}

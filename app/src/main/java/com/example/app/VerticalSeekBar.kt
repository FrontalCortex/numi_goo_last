package com.example.app

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar

/**
 * Dikey SeekBar:
 * - Görseli -90 derece döndürür.
 * - Dokunmayı Y eksenine göre progress'e map eder (üst = max, alt = 0).
 */
class VerticalSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.seekBarStyle
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // width/height swap: dikeyde genişlik aslında yükseklik
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(canvas: Canvas) {
        // Klasik dikey SeekBar çizimi:
        // -90 derece döndürüp, X eksenini yeni koordinata taşırız.
        canvas.rotate(-90f)
        canvas.translate(-height.toFloat(), 0f)
        super.onDraw(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // SeekBar'ın iç hesapları yatay eksene göre olduğu için swap ederek bildiriyoruz.
        super.onSizeChanged(h, w, oldh, oldw)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        parent?.requestDisallowInterceptTouchEvent(true)

        // Dokunmayı yatay SeekBar koordinatına dönüştürüp, SeekBar'ın kendi thumb/track hesabını kullanalım.
        // Yatayda: sol(min) -> sağ(max). Dikeyde istediğimiz: alt(min) -> üst(max).
        // Bu yüzden x' = height - y.
        val newX = (height - event.y).coerceIn(0f, height.toFloat())
        val newY = event.x.coerceIn(0f, width.toFloat())

        val transformed = MotionEvent.obtain(
            event.downTime,
            event.eventTime,
            event.action,
            newX,
            newY,
            event.metaState
        )
        val handled = super.onTouchEvent(transformed)
        transformed.recycle()
        return handled
    }

    override fun performClick(): Boolean = super.performClick()
}


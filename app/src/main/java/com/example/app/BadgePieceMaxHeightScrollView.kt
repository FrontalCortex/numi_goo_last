package com.example.app

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec
import androidx.core.widget.NestedScrollView

/**
 * Yükseklik: içerik kadar, en fazla [R.dimen.badge_piece_panel_max_height] (varsayılan 100dp).
 */
class BadgePieceMaxHeightScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : NestedScrollView(context, attrs) {

    private val maxHeightPx: Int
        get() = resources.getDimensionPixelSize(R.dimen.badge_piece_panel_max_height)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val capped = measuredHeight.coerceAtMost(maxHeightPx).coerceAtLeast(0)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(capped, MeasureSpec.EXACTLY))
    }
}

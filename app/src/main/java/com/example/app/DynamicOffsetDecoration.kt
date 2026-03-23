package com.example.app

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.app.model.LessonItem

/**
 * LessonItem.offset ile kartı yatay kaydırır. GlobalLessonData'daki offset'ler -60..60 aralığında;
 * normalizasyon 50f ile yapılıyor (tarihsel). Dar ekranda taşmayı önlemek için kaydırma,
 * RecyclerView genişliği ve kart + item padding'e göre sınırlanır.
 */
class DynamicOffsetDecoration(
    private val maxOffsetPx: Int,
    private val lessonCardSizePx: Int,
    private val lessonItemHorizontalPaddingTotalPx: Int,
    private val edgeSafetyMarginPx: Int,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        val adapter = parent.adapter as? LessonAdapter ?: return
        val item = adapter.getItem(position)

        if (item.type == LessonItem.TYPE_LESSON) {
            val normalizedOffset = item.offset / 50f
            val desired = (normalizedOffset * maxOffsetPx).toInt()

            val innerWidth =
                parent.width - parent.paddingLeft - parent.paddingRight

            // İlk layout turunda width henüz 0 olabiliyor; setEmpty() offset'i tamamen sıfırlıyordu,
            // scroll sonrası düzeliyordu. Genişlik yokken hedef offset'i uygula, bir sonraki layout'ta kıs.
            if (innerWidth <= 0) {
                outRect.left = desired
                outRect.right = -desired
                return
            }

            val usableHalf =
                (innerWidth - lessonItemHorizontalPaddingTotalPx - lessonCardSizePx) / 2 -
                    edgeSafetyMarginPx
            val maxAllowedShift = usableHalf.coerceAtLeast(0)
            val finalOffset = desired.coerceIn(-maxAllowedShift, maxAllowedShift)

            outRect.left = finalOffset
            outRect.right = -finalOffset
        }
    }
}

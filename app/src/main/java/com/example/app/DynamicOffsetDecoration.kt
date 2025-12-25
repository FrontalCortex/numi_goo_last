package com.example.app

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.app.model.LessonItem

class DynamicOffsetDecoration(private val maxOffset: Int) : RecyclerView.ItemDecoration() {

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
            // Ekranın ortasını referans noktası olarak al
            val centerOffset = parent.width / 2 - view.layoutParams.width / 2

            // item.offset değerine göre sağa veya sola kaydır
            // offset değeri -50 ile 50 arasında olduğu için, bunu -1 ile 1 arasına normalize ediyoruz
            val normalizedOffset = item.offset / 50f
            val finalOffset = (normalizedOffset * maxOffset).toInt()

            // Yatay konumu ayarla
            outRect.left = finalOffset
            outRect.right = -finalOffset
        }
    }

}
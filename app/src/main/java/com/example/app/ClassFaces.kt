package com.example.app

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Sınıf kartlarındaki üst üste dizili öğrenci avatarları (en fazla [MAX], fazlası "+N").
 * Öğretmenin Sınıflarım listesi ve öğrencinin Sınıfım sekmesi kullanıyor.
 */
object ClassFaces {
    const val MAX = 4
    private val COLORS = listOf("#1CB0F6", "#CE82FF", "#FF9600", "#00CD9C")

    /**
     * [avatars]: ilk öğrencilerin avatarları (null: avatarı yok ya da henüz okunmadı; renkli daire
     * çiziliyor). [total]: sınıftaki öğrenci sayısı.
     */
    fun bind(row: LinearLayout, avatars: List<String?>, total: Int) {
        row.removeAllViews()
        val ctx = row.context
        val density = ctx.resources.displayMetrics.density
        val size = (28 * density).toInt()
        val shown = avatars.take(MAX)
        shown.forEachIndexed { i, encoded ->
            row.addView(TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    if (i > 0) marginStart = (-8 * density).toInt() // üst üste binsin
                }
                background = AvatarView.drawableFor(ctx, encoded, size)
                    ?: GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(COLORS[i % COLORS.size]))
                        setStroke((2 * density).toInt(), ContextCompat.getColor(ctx, R.color.bright_background_color))
                    }
            })
        }
        val more = total - shown.size
        if (more > 0) {
            row.addView(TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, size)
                    .apply { marginStart = (6 * density).toInt() }
                gravity = Gravity.CENTER_VERTICAL
                text = appString(R.string.class_card_more, more)
                setTextColor(ContextCompat.getColor(ctx, R.color.dark_text_secondary))
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
            })
        }
    }
}

package com.example.app

import android.content.Context
import android.util.AttributeSet
import androidx.drawerlayout.widget.DrawerLayout

/**
 * Sobi sekmesinin çekmecesi (fragment_sobi_chat.xml). DrawerLayout yalnızca kesin ölçüyle
 * (EXACTLY) ölçülmeyi kabul ediyor, değilse çöküyor; MainActivity'nin fragman kabı ise içini
 * "en çok şu kadar" (AT_MOST) ile ölçüyor (02.11.2026, telefonda Sobi sekmesi açılınca çöktü).
 * Sohbet ekranı kabı hep dolduruyor: üst sınır kesin ölçü sayılıyor.
 */
class SobiDrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : DrawerLayout(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(exact(widthMeasureSpec), exact(heightMeasureSpec))
    }

    private fun exact(spec: Int): Int =
        if (MeasureSpec.getMode(spec) == MeasureSpec.AT_MOST) {
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(spec), MeasureSpec.EXACTLY)
        } else spec
}

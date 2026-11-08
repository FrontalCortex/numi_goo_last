package com.example.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import androidx.core.content.ContextCompat

/**
 * Haftalık kartın SEVİYE MERDİVENİ: yatay bir yol üstünde basamaklar, geçen haftanın yeri (küçük gri
 * nokta) ve bu haftanın yeri (büyük yeşil nokta), aradaki yol yeşil. Altlarında "Geçen hafta / 4 sayı"
 * ve "Bu hafta ✔ / 5 sayı". Bkz. [WeeklyCardRules.ladderFor] (hangi basamaklar) ve WeeklyCardFragment.
 *
 * Etiketler birbirinden uzağa yaslanıyor (geçen haftanınki noktanın soluna, bu haftanınki sağına):
 * iki nokta yan yana olsa bile yazılar üst üste binmesin. Sağdan sola dillerde (Arapça) yol aynalanıyor.
 */
class LevelLadderView(context: Context) : View(context) {

    var ladder = WeeklyCardRules.Ladder(5, 1, 3)
    var beforeLabel = ""
    var beforeValue = ""
    var afterLabel = ""
    var afterValue = ""

    private val d = resources.displayMetrics.density
    private val accent = 0xFF8CC63F.toInt()
    private val track = 0xFF3D4A50.toInt()
    private val muted = ContextCompat.getColor(context, R.color.dark_text_secondary)
    private val text = ContextCompat.getColor(context, R.color.dark_text)

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 4 * d; strokeCap = Paint.Cap.ROUND }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12 * d }
    private val value = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 15 * d; typeface = Typeface.DEFAULT_BOLD }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (88 * d).toInt())
    }

    override fun onDraw(c: Canvas) {
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val pad = 14 * d
        val y = 22 * d
        val w = width.toFloat()
        val n = ladder.total.coerceAtLeast(2)
        fun x(i: Int): Float {
            val lx = pad + (w - 2 * pad) * i / (n - 1)
            return if (rtl) w - lx else lx
        }
        // Yol ve basamaklar.
        line.color = track
        c.drawLine(x(0), y, x(n - 1), y, line)
        dot.color = track
        for (i in 0 until n) c.drawCircle(x(i), y, 4 * d, dot)
        // Geçen hafta → bu hafta arası yeşil.
        line.color = accent
        c.drawLine(x(ladder.from), y, x(ladder.to), y, line)
        dot.color = muted
        c.drawCircle(x(ladder.from), y, 7 * d, dot)
        dot.color = accent
        c.drawCircle(x(ladder.to), y, 11 * d, dot)

        // Etiketler: geçen haftanınki noktadan "geriye", bu haftanınki "ileriye" yaslı; kenarda kırpılmasın.
        val fx = x(ladder.from)
        val tx = x(ladder.to)
        drawLabel(c, beforeLabel, beforeValue, fx, back = true, rtl = rtl, top = y + 30 * d, strong = false)
        drawLabel(c, afterLabel, afterValue, tx, back = false, rtl = rtl, top = y + 30 * d, strong = true)
    }

    private fun drawLabel(c: Canvas, l: String, v: String, cx: Float, back: Boolean, rtl: Boolean, top: Float, strong: Boolean) {
        small.color = if (strong) accent else muted
        value.color = if (strong) text else muted
        val width = maxOf(small.measureText(l), value.measureText(v))
        // "Geriye" = okuma yönünün tersi: soldan sağa dilde sol, Arapçada sağ.
        val towardStart = back != rtl
        val nudge = 6 * d
        var left = if (towardStart) cx + nudge - width else cx - nudge
        left = left.coerceIn(0f, this.width - width)
        small.textAlign = Paint.Align.LEFT
        value.textAlign = Paint.Align.LEFT
        // Arapçada metnin kendisi sağa yaslı görünsün diye: aynı kutu, sağ kenardan çizim.
        if (rtl) {
            small.textAlign = Paint.Align.RIGHT
            value.textAlign = Paint.Align.RIGHT
            left += width
        }
        c.drawText(l, left, top, small)
        c.drawText(v, left, top + 19 * d, value)
    }
}

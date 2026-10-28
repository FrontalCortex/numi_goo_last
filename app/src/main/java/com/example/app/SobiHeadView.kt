package com.example.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Sobi'nin yalnızca kafası (kulaklar, kafa, gözler, ağız), düğmelerde simge olarak ("Sobi'ye sor").
 *
 * Ayrı bir görsel yerine [BunnyMascotArt]'ın kendi şekilleri çiziliyor: rehber panelindeki Sobi ile
 * birebir aynı kalsın, çizim değişirse burası da değişsin. Hareket yok; [BunnyMascotView] tam
 * gövdeyi ve canlandırmayı çiziyor, burada gerekmiyor.
 *
 * Ağız olarak bekleme ağzı (MOUTH) kullanılıyor: HAPPY_MOUTH başka hayvanın koordinatlarında,
 * BunnyMascotView onu kaydırarak yerine koyuyor.
 */
class SobiHeadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /** Arkadan öne: kulaklar kafanın arkasında. */
    private val shapes: List<BunnyMascotArt.Shape> by lazy {
        BunnyMascotArt.EAR_LEFT + BunnyMascotArt.EAR_RIGHT + BunnyMascotArt.HEAD +
            BunnyMascotArt.HEAD_TOP + BunnyMascotArt.EYES + BunnyMascotArt.MOUTH
    }

    /** Şekillerin toplam sınırı (çizgi kalınlığı dahil); ölçek buna göre. */
    private val bounds: RectF by lazy {
        val out = RectF()
        val r = RectF()
        shapes.forEachIndexed { i, s ->
            s.path.computeBounds(r, true)
            r.inset(-s.strokeWidth / 2f, -s.strokeWidth / 2f)
            if (i == 0) out.set(r) else out.union(r)
        }
        out
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = (width - paddingLeft - paddingRight).toFloat()
        val h = (height - paddingTop - paddingBottom).toFloat()
        if (w <= 0f || h <= 0f || bounds.isEmpty) return
        val scale = minOf(w / bounds.width(), h / bounds.height())
        canvas.save()
        canvas.translate(
            paddingLeft + (w - bounds.width() * scale) / 2f,
            paddingTop + (h - bounds.height() * scale) / 2f,
        )
        canvas.scale(scale, scale)
        canvas.translate(-bounds.left, -bounds.top)
        for (s in shapes) {
            paint.color = s.color
            if (s.strokeWidth > 0f) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = s.strokeWidth
            } else {
                paint.style = Paint.Style.FILL
            }
            canvas.drawPath(s.path, paint)
        }
        canvas.restore()
    }
}

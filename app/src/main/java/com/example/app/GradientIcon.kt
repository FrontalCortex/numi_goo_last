package com.example.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

/**
 * Tek renkli bir ikonu renk geçişiyle boyar: ikon şekli maske olarak kullanılıyor, üstüne sol
 * üstten sağ alta [colors] geçişi çiziliyor (SRC_IN). Vektörler ancak tek bir renge
 * boyanabildiği (tint) için ikon önce bitmap'e çiziliyor.
 *
 * İlk kullanım: haritadaki "öğretmene sor" düğmesinin ikonu (MapFragment).
 *
 * @param scale Bitmap çözünürlüğü, ikonun kendi boyutunun katı; düğme ikonu büyütüyor,
 *   bulanık durmasın.
 */
fun gradientIcon(context: Context, drawableRes: Int, colors: IntArray, scale: Int = 3): BitmapDrawable? {
    val source = ContextCompat.getDrawable(context, drawableRes)?.mutate() ?: return null
    val w = source.intrinsicWidth.coerceAtLeast(1) * scale
    val h = source.intrinsicHeight.coerceAtLeast(1) * scale
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    source.setBounds(0, 0, w, h)
    source.draw(canvas)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), colors, null, Shader.TileMode.CLAMP)
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    return BitmapDrawable(context.resources, bitmap)
}

/**
 * [gradientIcon] gibi renk geçişli ikon; üstüne ara sıra merkezden kenarlara yayılan renkli bir
 * dalga çiziliyor ([waveProgress] 0 → 1). Dalga yalnızca ikonun şeklinin içinde görünüyor
 * (SRC_ATOP); geçtiği yer geride ikonun kendi renklerine dönüyor, dalga bitince ikon eski hâli.
 *
 * Dalga bir halka bandı: önde [waveColors]'un ilki, arkada sonuncusu. Bandın genişliği ikonun
 * yarı çapının [BAND_FRACTION]'ı; yarıçap ikonun köşelerini de aşacak kadar büyüyor ki dalga
 * tamamen çıksın.
 */
class WaveGradientIconDrawable(
    private val base: BitmapDrawable,
    private val waveColors: IntArray,
) : Drawable() {

    /** 0: dalga yok. (0, 1): dalga yayılıyor. Değişince yeniden çiziliyor. */
    var waveProgress: Float = 0f
        set(value) {
            field = value
            invalidateSelf()
        }

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        base.bounds = bounds
    }

    override fun getIntrinsicWidth(): Int = base.intrinsicWidth
    override fun getIntrinsicHeight(): Int = base.intrinsicHeight

    override fun draw(canvas: Canvas) {
        val p = waveProgress
        if (p <= 0f || p >= 1f) {
            base.draw(canvas)
            return
        }
        val b = bounds
        val cx = b.exactCenterX()
        val cy = b.exactCenterY()
        val half = minOf(b.width(), b.height()) / 2f
        val band = half * BAND_FRACTION
        // Köşeler merkezden ~1,42 yarıçap uzakta; bandın arka kenarı da geçsin diye + band.
        val front = p * (half * 1.42f + band)
        if (front <= 0.5f) {
            base.draw(canvas)
            return
        }
        val back = (front - band).coerceAtLeast(0f)
        val n = waveColors.size
        val colors = IntArray(n + 2)
        val stops = FloatArray(n + 2)
        // Bandın içi şeffaf (ikonun kendi rengi görünür), sonra arkadan öne: son renk → ilk renk.
        colors[0] = android.graphics.Color.TRANSPARENT
        stops[0] = back / front
        for (i in 0 until n) {
            colors[i + 1] = waveColors[n - 1 - i]
            stops[i + 1] = (back + band * (i + 0.5f) / n) / front
        }
        colors[n + 1] = android.graphics.Color.TRANSPARENT
        stops[n + 1] = 1f
        // Duraklar artan olmalı; dalganın başında arka kenar 0'da sıkışabiliyor.
        for (i in 1 until stops.size) stops[i] = maxOf(stops[i], stops[i - 1])
        wavePaint.shader = android.graphics.RadialGradient(cx, cy, front, colors, stops, Shader.TileMode.CLAMP)

        val save = canvas.saveLayer(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(), null)
        base.draw(canvas)
        canvas.drawRect(b, wavePaint)
        canvas.restoreToCount(save)
    }

    override fun setAlpha(alpha: Int) {
        base.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        base.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Drawable API")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT

    private companion object {
        const val BAND_FRACTION = 0.7f
    }
}

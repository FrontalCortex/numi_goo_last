package com.example.app

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.app.homework.DepthPress
import com.google.android.material.button.MaterialButton

/**
 * Kodla "içe göçen" düğme (Duolingo tarzı): altta 4 dp koyu dudak, basılıyken dudak gizlenir ve
 * [DepthPress] yüzü 4 dp aşağı indirir. XML'deki bg_depth_* çizimleriyle aynı yapı; burada kodla,
 * çünkü bu düğmelerin rengi duruma göre değişiyor (ders panelinde açık/bitmiş/anahtar, mağazada
 * alınabilir/hazır, kayıtta etkin/devre dışı). Kullanıcı isteği 09.11.2026: uygulamadaki diğer
 * düğmelerle aynı his.
 *
 * Renk her durumda yeniden [style] ile veriliyor; dolgu ve dokunma dinleyicisi yalnızca ilk seferde.
 * Aynı renkle tekrar çağrı (mağazadaki can sayacı her saniye yeniliyor) hiçbir şey yapmıyor.
 */
object DepthButtons {

    private const val DEPTH_DP = 4f
    private val TAG_STYLED = R.id.depthButtonStyled

    /**
     * @param face  yüz rengi
     * @param radiusDp köşe yuvarlaklığı (düğmenin eskisiyle aynı)
     * @param lip   dudak rengi; verilmezse yüzün koyusu
     * @param stroke kenar çizgisi (ör. beyaz Google düğmesi); null = yok
     */
    fun style(view: View, face: Int, radiusDp: Float, lip: Int = darken(face), stroke: Int? = null) {
        val key = "$face/$radiusDp/$lip/$stroke"
        val previous = view.getTag(TAG_STYLED)
        if (previous == key) return
        val d = view.resources.displayMetrics.density
        val depth = (DEPTH_DP * d).toInt()
        val r = radiusDp * d
        fun shape(color: Int, withStroke: Boolean) = GradientDrawable().apply {
            setColor(color)
            cornerRadius = r
            if (withStroke && stroke != null) setStroke((1.5f * d).toInt(), stroke)
        }
        fun faceOnly(color: Int): Drawable = LayerDrawable(arrayOf(shape(color, true))).apply {
            setLayerInset(0, 0, 0, 0, depth)
        }
        val normal = LayerDrawable(arrayOf(shape(lip, false), shape(face, true))).apply {
            setLayerInset(0, 0, depth, 0, 0)
            setLayerInset(1, 0, 0, 0, depth)
        }
        val bg = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), faceOnly(face))
            // Devre dışı (kilitli ders, rehber modu): dudak yok, düz yüz; basılamayacağı belli olsun.
            addState(intArrayOf(-android.R.attr.state_enabled), faceOnly(face))
            addState(intArrayOf(), normal)
        }
        when (view) {
            is MaterialButton -> {
                view.backgroundTintList = null
                view.stateListAnimator = null // Material gölgesi dudakla çakışmasın
                view.background = bg
            }
            is CardView -> {
                // CardView'ın kendi yuvarlak zemini yerine; setCardBackgroundColor artık görünmüyor.
                view.cardElevation = 0f
                view.background = bg
                // CardView kendini arka planın çizgisine göre kırpıyor; o çizgi dudak katmanından
                // geliyor (üstten 4 dp içeride) ve basınca yüzün üst kenarı yerinde kalmış gibi
                // görünüyordu. Çocukları kırpmaya ihtiyaç yok.
                view.clipToOutline = false
            }
            else -> view.background = bg
        }
        view.setTag(TAG_STYLED, key)
        if (previous == null) {
            // Yazı yüzün ortasında kalsın: dudak kadar alt boşluk.
            if (view is CardView) {
                view.setContentPadding(view.contentPaddingLeft, view.contentPaddingTop, view.contentPaddingRight, view.contentPaddingBottom + depth)
            } else {
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom + depth)
            }
            DepthPress.attach(view)
        }
    }

    /** Giriş/kayıt ekranlarının ana düğmesi (dark_primary). */
    fun primary(view: View, radiusDp: Float = 8f) =
        style(view, ContextCompat.getColor(view.context, R.color.dark_primary), radiusDp)

    /** "Google ile devam et": beyaz yüz, gri kenar ve dudak. */
    fun google(view: View) {
        val edge = ContextCompat.getColor(view.context, R.color.dark_text_secondary)
        style(view, Color.WHITE, 8f, lip = edge, stroke = edge)
    }

    fun darken(c: Int, f: Float = 0.78f): Int = Color.argb(
        Color.alpha(c), (Color.red(c) * f).toInt(), (Color.green(c) * f).toInt(), (Color.blue(c) * f).toInt(),
    )
}

package com.example.app

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * Kenardan kenara çizilen MainActivity'de durum çubuğunun (saat, pil) ve gezinme çubuğunun
 * arkasına birer renkli şerit koyar; iki çubuk da ekranın zeminiyle aynı renge bürünür.
 *
 * ## Neden
 * Android 15+ (targetSdk 35+) `window.statusBarColor` ve `navigationBarColor`'ı yok sayıyor,
 * çubuklar şeffaf. MainActivity'nin kökü fitsSystemWindows ile çubuk kadar içeriden başladığı
 * için ekranın kendi zemini oraya uzanmıyor; çubuklarda pencerenin zemin rengi görünüyor.
 *
 * Şeritler `android.R.id.content`'e kökten SONRA eklendiği için onun üstünde çiziliyor. Boyları
 * yalnızca çubuk payı kadar: içerikle çakışmıyorlar, dokunuş da yutmuyorlar. [remove] onları
 * kaldırınca altta ne varsa (ör. profilin şeridi) yeniden görünüyor. Yalnızca üst çubuk için
 * aynı yol: MainActivity.setStatusBarTint.
 */
class SystemBarStrips(activity: Activity) {

    private val content: FrameLayout = activity.findViewById(android.R.id.content)
    private val top = addStrip(Gravity.TOP)
    private val bottom = addStrip(Gravity.BOTTOM)

    /** Şeritlerin şu anki rengi; renk geçişleri buradan başlıyor. */
    @ColorInt
    var color: Int = Color.TRANSPARENT
        set(value) {
            field = value
            top.setBackgroundColor(value)
            bottom.setBackgroundColor(value)
        }

    fun remove() {
        content.removeView(top)
        content.removeView(bottom)
    }

    private fun addStrip(gravity: Int): View {
        val strip = View(content.context)
        content.addView(
            strip,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, gravity),
        )
        // Yeni eklenen görünüme pay ancak bir sonraki dağıtımda gelir; ilk boy hemen verilsin ki
        // ekran açılırken şerit bir kare boyunca eksik kalmasın. Dinleyici sonraki değişiklikler için.
        ViewCompat.getRootWindowInsets(content)?.let { fitToBar(strip, gravity, it) }
        ViewCompat.setOnApplyWindowInsetsListener(strip) { v, insets ->
            fitToBar(v, gravity, insets)
            insets
        }
        return strip
    }

    private fun fitToBar(strip: View, gravity: Int, insets: WindowInsetsCompat) {
        val height = if (gravity == Gravity.TOP) {
            insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        } else {
            insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        }
        if (strip.layoutParams.height != height) strip.updateLayoutParams { this.height = height }
    }
}

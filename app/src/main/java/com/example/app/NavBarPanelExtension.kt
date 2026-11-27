package com.example.app

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.abs

/**
 * Alttan açılan sonuç panellerini (doğru/yanlış) telefonun gezinme çubuğunun (tuşlar ya da hareket
 * çizgisi) arkasına kadar uzatır; panel ekranın en altından kayarak çıkıyormuş gibi görünür.
 *
 * Kök görünüm (activity_main `main`) fitsSystemWindows ile gezinme çubuğu kadar yukarıda bitiyor,
 * panel o sınırın dışına çizilemiyor. Bu yüzden çubuğun arkasına, android.R.id.content'e panelin
 * zemininden bir şerit konuyor ve her karede panelin ötelemesine göre kaydırılıyor: panel ile
 * şerit tek bir sayfa gibi hareket ediyor. Bunun için panel gizliyken ötelemesi
 * [hiddenTranslation] olmalı (panel yüksekliği + çubuk yüksekliği); yalnızca panel yüksekliği
 * olursa şerit kaymadan birden kaybolur.
 *
 * Şerit görünürken gezinme çubuğu şeffaf yapılıyor: API 34 ve altında [MainActivity]'nin verdiği
 * `navigationBarColor` (opak `background_color`) sistem tarafından içeriğin ÜSTÜNE çiziliyor ve
 * şeridi tamamen örtüyordu (ilk sürümde Android 13'te hiçbir şey değişmemiş görünmesinin nedeni).
 * Panel kapanınca eski renk geri konuyor. Çubuk şeffafken altında pencere zemini
 * (`background_color`) göründüğü için panel yokken de görünüm aynı.
 */
class NavBarPanelExtension(private val host: View, private vararg val panels: View) {

    private val background = (panels.first().background?.constantState?.newDrawable()?.mutate()
        as? GradientDrawable) ?: GradientDrawable().apply { setColor(0xFF222222.toInt()) }
    private val roundedRadii = background.cornerRadii
    private var rounded: Boolean? = null

    // Şeffaf yapılmadan önceki çubuk rengi / okunabilirlik perdesi; null ise çubuk eski hâlinde.
    private var savedNavBarColor: Int? = null
    private var savedContrastEnforced = true

    private val strip = View(host.context).apply {
        visibility = View.GONE
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        setBackground(this@NavBarPanelExtension.background)
    }

    private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        sync()
        true
    }

    /** onViewCreated'da çağrılabilir; şerit, görünüm pencereye bağlanınca ilk karede eklenir. */
    fun attach() {
        host.viewTreeObserver.addOnPreDrawListener(preDrawListener)
    }

    fun detach() {
        host.viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        restoreNavBar()
        (strip.parent as? ViewGroup)?.removeView(strip)
    }

    /** Panelin tamamen ekran dışında (çubuğun da altında) kaldığı öteleme. */
    fun hiddenTranslation(panel: View): Float {
        val inset = navInset()
        val extra = if (inset > 0 && restsOnNavBar(panel, inset)) inset else 0
        return (panel.height + extra).toFloat()
    }

    private fun navInset(): Int =
        ViewCompat.getRootWindowInsets(host)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0

    /** Panel yerindeyken alt kenarı tam çubuğun üstüne mi oturuyor (alt menü açıksa oturmaz). */
    private fun restsOnNavBar(panel: View, inset: Int): Boolean {
        val loc = IntArray(2)
        panel.getLocationInWindow(loc)
        val restBottom = loc[1] - panel.translationY + panel.height
        val navTop = host.rootView.height - inset
        return abs(restBottom - navTop) <= 2f
    }

    private fun window(): Window? {
        var ctx = host.context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx.window
            ctx = ctx.baseContext
        }
        return null
    }

    private fun makeNavBarTransparent() {
        if (savedNavBarColor != null) return
        val window = window() ?: return
        savedNavBarColor = window.navigationBarColor
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Sistem şeffaf çubuğun altına yarı saydam bir okunabilirlik perdesi çekiyor; şerit
            // panelle birebir aynı renkte görünsün diye kapatılıyor.
            savedContrastEnforced = window.isNavigationBarContrastEnforced
            window.isNavigationBarContrastEnforced = false
        }
    }

    private fun restoreNavBar() {
        val color = savedNavBarColor ?: return
        savedNavBarColor = null
        val window = window() ?: return
        window.navigationBarColor = color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = savedContrastEnforced
        }
    }

    private fun hideStrip() {
        if (strip.visibility != View.GONE) strip.visibility = View.GONE
        restoreNavBar()
    }

    private fun sync() {
        val panel = panels.firstOrNull { it.isShown && it.alpha > 0f }
        if (panel == null || panel.height == 0) {
            hideStrip()
            return
        }
        val inset = navInset()
        if (inset == 0 || !restsOnNavBar(panel, inset)) {
            hideStrip()
            return
        }
        if (strip.parent == null) {
            val content = host.rootView.findViewById<FrameLayout>(android.R.id.content) ?: return
            content.addView(strip, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, inset, Gravity.BOTTOM))
        }
        if (strip.layoutParams.height != inset) {
            strip.layoutParams = strip.layoutParams.apply { height = inset }
        }
        // Sayfanın üst kenarı çubuğun içine girdiyse şerit o kadar aşağıda başlar.
        val offset = (panel.translationY - panel.height).coerceIn(0f, inset.toFloat())
        strip.translationY = offset
        strip.alpha = panel.alpha
        // Üst kenar çubukta görünürken köşeler panelinki gibi yuvarlak; panel yukarıdayken düz,
        // yoksa panelin altında köşe boşlukları kalır.
        val wantRounded = offset > 0f
        if (rounded != wantRounded) {
            rounded = wantRounded
            background.cornerRadii = if (wantRounded && roundedRadii != null) roundedRadii else FloatArray(8)
        }
        makeNavBarTransparent()
        if (strip.visibility != View.VISIBLE) strip.visibility = View.VISIBLE
    }
}

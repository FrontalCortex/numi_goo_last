package com.example.app

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Tam ekran dialog'larda (Pro, plan, reklam atlama, soru tanıtımı) telefonun üst çubuğunu ve
 * alt tuşların olduğu çubuğu ekranın kendi renklerine boyar.
 *
 * ## Sorun
 * Bu ekranlar `Theme_*_NoTitleBar_Fullscreen` ile açılıyor: tema durum çubuğunu gizliyor ve
 * ekranın tepesinde (kamera çentiği bölgesi) sistemin siyah bandı kalıyordu; alttaki gezinme
 * çubuğu da sistem renginde, ekranın alt kısmından kopuk duruyordu.
 *
 * ## Ne yapıyor
 *  - Durum çubuğu görünür yapılıyor ve [top] renge, gezinme çubuğu [bottom] renge boyanıyor;
 *    simgeler zemine göre açık/koyu (açık zeminde koyu simge).
 *  - Android 15+ (targetSdk 35+) çubuk rengini yok sayıp içeriği çubukların ALTINA uzatıyor.
 *    Orada da doğru görünsün diye [topView]'in üst dolgusuna durum çubuğu, [bottomView]'in alt
 *    dolgusuna gezinme çubuğu yüksekliği ekleniyor: böylece o görünümlerin kendi arka
 *    planları çubukların altına uzanıyor, içerik çubuklarla çakışmıyor. Daha eski sürümlerde
 *    pencere çubukların altına uzanmadığı için bu dolgular 0 geliyor ve hiçbir şey değişmiyor.
 */
object SystemBarColors {

    fun applyToDialog(
        window: Window,
        @ColorInt top: Int,
        @ColorInt bottom: Int,
        topView: View,
        bottomView: View,
    ) {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = top
        window.navigationBarColor = bottom
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Sistem şeffaf çubuğun altına okunabilirlik perdesi çekiyor; renk birebir kalsın.
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.statusBars())
        controller.isAppearanceLightStatusBars = isLight(top)
        controller.isAppearanceLightNavigationBars = isLight(bottom)

        val topBase = topView.paddingTop
        val bottomBase = bottomView.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(topView) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = topBase + bars.top)
            if (v === bottomView) v.updatePadding(bottom = bottomBase + bars.bottom)
            insets
        }
        if (bottomView !== topView) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomView) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updatePadding(bottom = bottomBase + bars.bottom)
                insets
            }
        }
        ViewCompat.requestApplyInsets(topView)
    }

    private fun isLight(@ColorInt color: Int) = ColorUtils.calculateLuminance(color) > 0.5
}

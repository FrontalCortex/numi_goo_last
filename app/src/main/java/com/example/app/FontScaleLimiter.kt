package com.example.app

import android.content.Context
import android.content.res.Configuration
import kotlin.math.roundToInt

/**
 * Telefonun sistem yazı boyutu ayarını uygulama içinde belirli bir üst sınıra kırpar.
 *
 * Kullanıcı yazıyı biraz büyüttüyse (örn. 1.1) aynen uygulanır; çok büyüttüyse (1.3 - 2.0)
 * [MAX_FONT_SCALE]'e indirilir. Aksi halde abaküs/eğitim ekranları gibi sabit yerleşimli
 * sayfalarda metinler tabloların üstüne biner. Her aktivitenin attachBaseContext'inde çağrılır.
 *
 * Tablette ayrıca yoğunluğu (density) büyütür; bkz. [scaleForTablet].
 */
object FontScaleLimiter {

    const val MAX_FONT_SCALE = 1.15f

    /** Bu en küçük genişlikten (dp) itibaren cihaz tablet sayılır; altı (bütün telefonlar) olduğu gibi kalır. */
    private const val TABLET_MIN_SW_DP = 600

    /** Tablette hedeflenen "sanal telefon" ölçüsü: en küçük genişlik ve uzun kenar (dp). */
    private const val TABLET_TARGET_SW_DP = 480f
    private const val TABLET_TARGET_LONG_DP = 800f

    fun wrap(base: Context): Context {
        val current = base.resources.configuration
        val limited = Configuration(current)
        var changed = false
        if (current.fontScale > MAX_FONT_SCALE) {
            limited.fontScale = MAX_FONT_SCALE
            changed = true
        }
        if (scaleForTablet(limited)) changed = true
        return if (changed) base.createConfigurationContext(limited) else base
    }

    /**
     * Düzenlerde binlerce sabit dp/sp var; tablette (ör. Lenovo Tab M10, 800 dp) bunlar fiziksel olarak
     * küçük kalıyor, içerik geniş ekranda dağılıyordu. Her düzene tablet ölçüsü eklemek yerine yoğunluğu
     * büyütüyoruz: tablet büyük bir telefon gibi (~480-500 dp genişlik, ~800 dp boy) davranıyor, yani
     * telefonda doğrulanmış yerleşimler orantısıyla büyüyor. Boy sınırı kısa (16:10) tabletlerde abaküs
     * ekranlarının dikeyde sıkışmaması için: büyütme ikisinden küçüğüne göre.
     *
     * @return yapılandırma değiştiyse true.
     */
    private fun scaleForTablet(config: Configuration): Boolean {
        val sw = config.smallestScreenWidthDp
        if (sw < TABLET_MIN_SW_DP || config.densityDpi == Configuration.DENSITY_DPI_UNDEFINED) return false
        val longSide = maxOf(config.screenWidthDp, config.screenHeightDp)
        val factor = minOf(sw / TABLET_TARGET_SW_DP, longSide / TABLET_TARGET_LONG_DP)
        if (factor <= 1.05f) return false
        val oldDpi = config.densityDpi
        val newDpi = (oldDpi * factor).roundToInt()
        // dp ölçüleri piksel sabit kalacak şekilde yeni yoğunluğa çevriliyor.
        fun rescale(dp: Int) = (dp * oldDpi.toFloat() / newDpi).toInt()
        config.densityDpi = newDpi
        config.screenWidthDp = rescale(config.screenWidthDp)
        config.screenHeightDp = rescale(config.screenHeightDp)
        config.smallestScreenWidthDp = rescale(sw)
        return true
    }
}

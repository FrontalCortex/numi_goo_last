package com.example.app

import android.content.Context
import android.content.res.Configuration

/**
 * Telefonun sistem yazı boyutu ayarını uygulama içinde belirli bir üst sınıra kırpar.
 *
 * Kullanıcı yazıyı biraz büyüttüyse (örn. 1.1) aynen uygulanır; çok büyüttüyse (1.3 - 2.0)
 * [MAX_FONT_SCALE]'e indirilir. Aksi halde abaküs/eğitim ekranları gibi sabit yerleşimli
 * sayfalarda metinler tabloların üstüne biner. Her aktivitenin attachBaseContext'inde çağrılır.
 */
object FontScaleLimiter {

    const val MAX_FONT_SCALE = 1.15f

    fun wrap(base: Context): Context {
        val current = base.resources.configuration
        if (current.fontScale <= MAX_FONT_SCALE) return base
        val limited = Configuration(current).apply { fontScale = MAX_FONT_SCALE }
        return base.createConfigurationContext(limited)
    }
}

package com.example.app.abacus

import android.view.View
import android.view.ViewTreeObserver

/**
 * Abaküsü, sağ kenardaki yuvarlak düğme sütununun (ipucu, soru sor, sihirli değnek, ayarlar...)
 * arasına sığacak kadar küçültür.
 *
 * Abaküs (~273dp) ekranın ortasında, düğmeler 48dp ve ekran kenarına yapışık. Ortalanmış abaküs
 * ile sütunun çakışmaması için ~370dp gerekir; Android'de en yaygın genişlik 360dp ve "Ekran
 * boyutu / Yakınlaştırma" bunu daha da düşürüyor, bu yüzden düğmeler abaküsün üstüne biniyordu.
 *
 * Kullanıcının ayarlar panelinden seçtiği ölçek ([userScaleX] / [userScaleY]) korunur; görünen
 * ölçek = kullanıcı ölçeği × sığdırma payı. Kaydedilecek değer her zaman kullanıcı ölçeğidir,
 * görünen ölçek değil. Küçülme alt kenardan olur (pivot alt-orta), abaküs kontrol düğmesinden
 * kopmaz. Ölçek yalnızca çizimi değiştirir; boncuk sürükleme parmak hareketine göre çalıştığı
 * için (bkz. [AbacusBeadController]) ayarlar panelindeki ölçeklemede olduğu gibi çalışmaya devam eder.
 *
 * Her yerleşimden sonra (düğme görünür/gizli olunca, ekran değişince) yeniden hesaplanır;
 * [attach] onViewCreated'da, [detach] onDestroyView'da çağrılmalı.
 */
class AbacusSideButtonFit(
    private val abacus: View,
    private val sideButtons: List<View>,
    private val gapPx: Float,
) {
    var userScaleX = 1f
        private set
    var userScaleY = 1f
        private set

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { apply() }

    fun attach() {
        abacus.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    fun detach() {
        val observer = abacus.viewTreeObserver
        if (observer.isAlive) observer.removeOnGlobalLayoutListener(layoutListener)
    }

    fun setUserScale(x: Float, y: Float) {
        userScaleX = x
        userScaleY = y
        apply()
    }

    fun setUserScaleX(x: Float) = setUserScale(x, userScaleY)

    fun setUserScaleY(y: Float) = setUserScale(userScaleX, y)

    fun apply() {
        val fit = computeFit()
        val w = abacus.width
        val h = abacus.height
        if (w > 0 && h > 0) {
            abacus.pivotX = w / 2f
            abacus.pivotY = h.toFloat()
        }
        val sx = userScaleX * fit
        val sy = userScaleY * fit
        // Ölçek yalnızca çizimi geçersiz kılar, yerleşim tetiklemez; yine de gereksiz çizim olmasın.
        if (abacus.scaleX != sx) abacus.scaleX = sx
        if (abacus.scaleY != sy) abacus.scaleY = sy
    }

    private fun computeFit(): Float {
        val parent = abacus.parent as? View ?: return 1f
        val w = abacus.width
        if (w <= 0 || parent.width <= 0) return 1f

        // Abaküsün kullanıcı ölçeğindeki görünen dikey aralığı (pivot alt kenar).
        val abacusBottom = abacus.bottom.toFloat()
        val abacusTop = abacusBottom - abacus.height * userScaleY

        val parentLoc = IntArray(2)
        val loc = IntArray(2)
        parent.getLocationInWindow(parentLoc)
        var columnLeft = Int.MAX_VALUE
        for (button in sideButtons) {
            if (!button.isShown || button.width <= 0) continue
            button.getLocationInWindow(loc)
            val left = loc[0] - parentLoc[0]
            val top = loc[1] - parentLoc[1]
            val bottom = top + button.height
            // Abaküsle aynı yükseklikte olmayan düğme (ör. tahtanın yanındaki) yer kaplamaz.
            if (bottom <= abacusTop || top >= abacusBottom) continue
            columnLeft = minOf(columnLeft, left)
        }
        if (columnLeft == Int.MAX_VALUE) return 1f

        // Abaküs ortalı: sağdaki sütun + boşluk kadar yer solda da bırakılmalı.
        val reserved = parent.width - columnLeft + gapPx
        val available = parent.width - 2f * reserved
        return (available / (w * userScaleX)).coerceIn(MIN_FIT, 1f)
    }

    private companion object {
        // Çok dar ekranda bile boncuklar parmakla tutulabilecek boyutta kalsın.
        const val MIN_FIT = 0.6f
    }
}

package com.example.app

import android.view.View
import android.widget.TextView

/**
 * Can göstergesinin ortak çizim kuralı.
 *
 * Sonsuz enerjide sayı yerine kalp ikonunun ÜZERİNDE "∞" gösterilir; yanındaki metin
 * tamamen boşalır. Bu kural üç ayrı yerde geçiyor (üst para paneli, mağaza başlığı,
 * kupa yolu paneli) ve her birinde elle tekrarlansaydı biri kolayca atlanırdı — nitekim
 * daha önce metnin "∞" yazması da bu şekilde her yere ayrı ayrı yazılmıştı.
 */
object EnergyDisplay {

    /**
     * @param text Sayının yazıldığı görünüm.
     * @param infiniteBadge İkonun üzerine bindirilmiş "∞" görünümü; düzende yoksa null.
     * @param isInfinite Sonsuz enerji durumu.
     * @param value Sonsuz değilken yazılacak metin (ör. "3/5").
     */
    fun apply(text: TextView?, infiniteBadge: View?, isInfinite: Boolean, value: String) {
        if (isInfinite) {
            text?.text = ""
            infiniteBadge?.visibility = View.VISIBLE
        } else {
            text?.text = value
            infiniteBadge?.visibility = View.GONE
        }
    }
}

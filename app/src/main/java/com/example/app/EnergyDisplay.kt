package com.example.app

import android.view.View
import android.widget.ImageView
import android.widget.TextView

/**
 * Can göstergesinin ortak çizim kuralı.
 *
 * Sonsuz enerjide sayı yerine kalp ikonunun ÜZERİNDE "∞" gösterilir; yanındaki metin
 * tamamen boşalır. Pro/Premium planda kalp ayrıca mağaza rozetiyle (bg_shop_super_badge)
 * aynı degradeye geçer. Bu kural üç ayrı yerde geçiyor (üst para paneli, mağaza başlığı,
 * kupa yolu paneli) ve her birinde elle tekrarlansaydı biri kolayca atlanırdı — nitekim
 * daha önce metnin "∞" yazması da bu şekilde her yere ayrı ayrı yazılmıştı.
 */
object EnergyDisplay {

    /**
     * @param text Sayının yazıldığı görünüm.
     * @param infiniteBadge İkonun üzerine bindirilmiş "∞" görünümü; düzende yoksa null.
     * @param icon Kalp ikonu; düzende yoksa null.
     * @param isInfinite Sonsuz enerji durumu.
     * @param isPremium Plan Pro/Premium mi. Sonsuz enerjiden AYRI tutuldu: onaylı öğretmenin
     *   de enerjisi sonsuz ama abonesi değil, o yüzden onun kalbi normal kırmızı kalır.
     * @param value Sonsuz değilken yazılacak metin (ör. "3/5").
     */
    fun apply(
        text: TextView?,
        infiniteBadge: View?,
        icon: ImageView? = null,
        isInfinite: Boolean,
        isPremium: Boolean = false,
        value: String,
    ) {
        if (isInfinite) {
            text?.text = ""
            infiniteBadge?.visibility = View.VISIBLE
        } else {
            text?.text = value
            infiniteBadge?.visibility = View.GONE
        }
        // Hesap değiştirildiğinde eski kullanıcının ikonu kalmasın diye iki yön de yazılır.
        icon?.setImageResource(if (isPremium) R.drawable.heart_pro_ic else R.drawable.heart_ic)
    }
}

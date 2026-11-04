package com.example.app

import android.view.View
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar

/**
 * Kilitli karta (bölüm kartı, kupa kartı) dokununca çıkan "önce şunu bitir" uyarısı.
 *
 * Toast yerine Snackbar: Android 12'den beri düz metinli Toast en fazla 2 satır gösteriyor ve
 * "Açmak için "Abaküsün Temeli ve Toplama" bölümünün son ünite maratonunu bitir." gibi bir
 * cümle ortasından kesiliyordu ("…bölümünün son ün"). Bölüm adları dile göre çok uzayabildiği
 * (ör. "Multiplication on the Abacus") için metni kısaltmak kalıcı çözüm değildi; Snackbar'ın
 * satır sınırı elle açılıyor.
 */
object LockedHint {

    private const val MAX_LINES = 5
    private const val SNACKBAR_ELEVATION_DP = 32f

    fun show(anchor: View, message: String) {
        val bar = Snackbar.make(anchor, message, Snackbar.LENGTH_LONG)
        bar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.maxLines = MAX_LINES
        // Kupa yolu paneli bir BottomSheet: Snackbar onunla aynı CoordinatorLayout'a ekleniyor ama
        // panelin elevation'ı (16dp) Snackbar'ınkinden (6dp) büyük olduğu için panelin ALTINDA
        // çiziliyordu. Yükseklik panelin üstüne çekiliyor; yerleşim değişmiyor.
        bar.view.elevation = SNACKBAR_ELEVATION_DP * anchor.resources.displayMetrics.density
        bar.show()
    }
}

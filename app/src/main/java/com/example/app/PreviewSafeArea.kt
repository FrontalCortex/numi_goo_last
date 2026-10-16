package com.example.app

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.media3.ui.PlayerView

/**
 * Tam ekran resim/video önizleme pencerelerinde kapatma (X) düğmesini ve oynatıcı kontrollerini
 * telefonun güvenli alanında tutar; görselin ve videonun kendisi tam ekran kalır.
 *
 * ## Sorun
 * Önizlemeler `Theme_Black_NoTitleBar_Fullscreen` + FLAG_FULLSCREEN ile açılıyor. Android 15+
 * (targetSdk 35+) pencereyi zorla kenardan kenara çiziyor: içerik kamera deliğine kadar uzuyor,
 * gezinme çubuğu da içeriğin üstüne biniyor. Sağ üst köşeye 8–16dp boşlukla yaslanan X deliğe
 * denk gelip basılamaz hâle gelebiliyor, videonun oynat/ilerleme çubuğu gezinme tuşlarının
 * arkasında kalıyordu.
 *
 * ## Ne yapıyor
 *  - X'in kenar boşluklarına sistem çubukları + kamera deliği payını ekliyor. İç dolgu değil
 *    kenar boşluğu: 48dp'lik dokunma alanı ve simge boyutu değişmesin.
 *  - Oynatıcının kontrol katmanına (`exo_controller`) aynı payı iç dolgu olarak veriyor. media3
 *    karartma perdesini dolgudan bağımsız olarak tüm alana yaydığı için perde yine tam ekran;
 *    yalnızca düğmeler ve ilerleme çubuğu içeri çekiliyor. Video yüzeyine dokunulmuyor.
 *  - Çubukları gizleyen pencerede (soru sohbetindeki tam ekran video) çubuk payı 0 geliyor,
 *    yalnızca kamera deliği payı kalıyor; gizleme davranışı değişmiyor.
 *  - Yalnızca Android 15+'da devreye giriyor. Eski sürümlerde sistem bu pencereleri zaten
 *    çubukların ve deliğin dışında tutuyor; üstelik VideoFullscreenDialogFragment
 *    LAYOUT_STABLE istediği için Android 10 ve altı gizli durum çubuğunun yüksekliğini de pay
 *    olarak bildiriyor, X boş yere aşağı kayardı.
 */
object PreviewSafeArea {

    private val SAFE_TYPES =
        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()

    fun apply(closeButton: View, playerView: PlayerView? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return

        // Pay her seferinde XML'deki boşluğun üstüne ekleniyor; dinleyici tekrar çağrılınca birikmesin.
        val lp = closeButton.layoutParams as ViewGroup.MarginLayoutParams
        val baseMargin = Insets.of(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin)
        ViewCompat.setOnApplyWindowInsetsListener(closeButton) { v, insets ->
            val safe = insets.getInsets(SAFE_TYPES)
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(
                    left = baseMargin.left + safe.left,
                    top = baseMargin.top + safe.top,
                    right = baseMargin.right + safe.right,
                    bottom = baseMargin.bottom + safe.bottom,
                )
            }
            insets
        }

        val controls = playerView?.findViewById<View>(androidx.media3.ui.R.id.exo_controller) ?: return
        val basePadding = Insets.of(
            controls.paddingLeft, controls.paddingTop, controls.paddingRight, controls.paddingBottom,
        )
        ViewCompat.setOnApplyWindowInsetsListener(controls) { v, insets ->
            val safe = insets.getInsets(SAFE_TYPES)
            v.setPadding(
                basePadding.left + safe.left,
                basePadding.top + safe.top,
                basePadding.right + safe.right,
                basePadding.bottom + safe.bottom,
            )
            insets
        }
    }
}

package com.example.app

import android.util.Log

/**
 * Rozet kutlama akışının teşhis logları. Logcat filtresi: DEBUG_BADGE
 *
 * Rozet payload'ları uzun bir yoldan geçiyor (sandık → görev ödülü → reklam kuyruğu →
 * MainActivity → kutlama ekranı) ve yolda kaybolduklarında sebebi ancak bu izler gösteriyor.
 * Bu yüzden silinmediler; yalnızca release'de susturuldular.
 *
 * Yalnızca debug derlemesinde çalışır: release'de kullanıcıya hiçbir faydası yok, üstelik her
 * çağrıdaki string birleştirmesi boşa iş demek.
 */
object BadgeDiagnostics {
    const val TAG = "DEBUG_BADGE"

    /**
     * Debug'da açık, release'de kapalı.
     *
     * `const val` değil çünkü `BuildConfig.DEBUG` derleme zamanı sabiti değildir. Release'de
     * değer false olduğu için gövde çalışmaz; R8 ayrıca çağrıları ve onlara verilen string
     * birleştirmelerini eler.
     */
    val ENABLED = BuildConfig.DEBUG

    fun log(message: String) {
        if (ENABLED) Log.d(TAG, message)
    }
}

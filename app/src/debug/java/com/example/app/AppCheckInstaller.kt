package com.example.app

import android.content.Context
import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug: hata ayıklama sağlayıcısı.
 *
 * Debug APK'lar Play'den gelmediği için Play Integrity ile doğrulanamaz. Bu sağlayıcı ilk
 * açılışta Logcat'e bir hata ayıklama jetonu (UUID) basar:
 *
 *     D/DebugAppCheckProvider: Enter this debug secret into the allow list in the Firebase
 *     Console for your project: xxxxxxxx-xxxx-...
 *
 * Zorlama (enforcement) açıldığında test cihazının çalışmaya devam etmesi için bu değerin
 * Firebase Console → App Check → Uygulama → Hata ayıklama jetonları listesine eklenmesi
 * gerekir. Jeton kurulum başına üretilir; uygulamayı silip yeniden kurmak yenisini üretir.
 * Zorlama kapalıyken hiçbir şey yapmaya gerek yoktur.
 */
object AppCheckInstaller {
    fun install(context: Context) {
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        Log.d(NumiGooApplication.TAG, "Debug sağlayıcı kuruldu — jeton için DebugAppCheckProvider log'una bak")
    }
}

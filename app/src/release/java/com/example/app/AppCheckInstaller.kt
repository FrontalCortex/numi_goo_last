package com.example.app

import android.content.Context
import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Release: Play Integrity.
 *
 * Yalnızca Play üzerinden dağıtılan ve Play App Signing anahtarıyla imzalanmış kurulumlar
 * doğrulanır — yandan yüklenen (sideload) veya yamalanmış APK'lar jeton alamaz. Play Console
 * → Uygulama bütünlüğü ile Firebase projesinin bağlanmış olması gerekir.
 */
object AppCheckInstaller {
    fun install(context: Context) {
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        Log.d(NumiGooApplication.TAG, "Play Integrity sağlayıcısı kuruldu")
    }
}

package com.example.app

import android.app.Application
import android.util.Log

/**
 * Süreç başlangıcında yalnızca App Check sağlayıcısını kurar.
 *
 * Firebase'in kendisi `FirebaseInitProvider` (bir ContentProvider) ile bu noktadan ÖNCE
 * hazır hale geldiği için burada ayrıca `FirebaseApp.initializeApp` çağrısına gerek yoktur.
 *
 * Sağlayıcı derleme türüne göre değişiyor (bkz. `src/debug` ve `src/release` altındaki
 * [AppCheckInstaller]): release Play Integrity ile doğrulanır, debug Play'den dağıtılmadığı
 * için doğrulanamaz ve debug sağlayıcıyı kullanır.
 */
class NumiGooApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // App Check kurulumu uygulamanın açılmasının önüne geçmemeli: burada atılan bir
        // istisna süreci daha ilk karede öldürür. Zorlama kapalıyken kurulumun başarısız
        // olması zaten hiçbir çağrıyı bloklamaz.
        try {
            AppCheckInstaller.install(this)
        } catch (e: Throwable) {
            Log.w(TAG, "App Check sağlayıcısı kurulamadı", e)
        }
    }

    companion object {
        const val TAG = "AppCheck"
    }
}

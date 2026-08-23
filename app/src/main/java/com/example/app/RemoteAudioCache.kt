package com.example.app

import android.content.Context
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import java.io.File

// Remote Config üzerinden gelen tutorial adımlarının, APK'ya gömülü olmayan (yeni eklenen)
// seslerini Firebase Storage'dan indirip cihazda kalıcı olarak (filesDir - cacheDir değil,
// çünkü OS düşük depolamada cacheDir'i habersiz temizleyebilir) saklar.
object RemoteAudioCache {

    private const val TAG = "Tutorial"
    private const val CACHE_SUBDIR = "tutorial_audio"

    private fun sanitizedFileName(storagePath: String): String =
        storagePath.substringAfterLast('/').ifEmpty { storagePath.replace('/', '_') }

    fun localFile(context: Context, storagePath: String): File {
        val dir = File(context.filesDir, CACHE_SUBDIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, sanitizedFileName(storagePath))
    }

    fun isCached(context: Context, storagePath: String): Boolean =
        localFile(context, storagePath).exists()

    // Zaten cache'teyse hiçbir şey yapmaz. Değilse arka planda indirir; hata olursa
    // sessizce loglar (çağıran taraf hiçbir zaman bloklanmaz veya çökmez).
    fun prefetch(context: Context, storagePath: String) {
        val target = localFile(context, storagePath)
        if (target.exists()) return
        try {
            FirebaseStorage.getInstance().reference.child(storagePath)
                .getFile(target)
                .addOnSuccessListener {
                    Log.d(TAG, "RemoteAudioCache: indirildi $storagePath")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "RemoteAudioCache: indirilemedi $storagePath", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "RemoteAudioCache: prefetch başlatılamadı $storagePath", e)
        }
    }
}

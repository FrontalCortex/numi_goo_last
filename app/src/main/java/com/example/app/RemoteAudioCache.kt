package com.example.app

import android.content.Context
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import java.io.File

// APK'ya gömülü olmayan öğretici seslerini (Türkçe dışındaki dillerin seslendirmesi:
// `tutorial_audio/en_<anahtar>.mp3`, `tutorial_audio/id_<anahtar>.mp3`) Firebase Storage'dan
// indirip cihazda kalıcı olarak (filesDir - cacheDir değil, çünkü OS düşük depolamada cacheDir'i
// habersiz temizleyebilir) saklar.
//
// Yerel dosya adı yolun son parçası; dil öneki (`en_`/`id_`) adın içinde olduğu için aynı
// anahtarın farklı dillerdeki sesleri aynı klasörde çakışmaz.
object RemoteAudioCache {

    private const val TAG = "Tutorial"
    private const val CACHE_SUBDIR = "tutorial_audio"
    private const val PART_SUFFIX = ".part"

    // Bir öğretici açılınca onlarca adımın sesi birden istenir; hepsini aynı anda açmak ağı ve
    // Storage istemcisini boğar, ilk adımların sesi de geç iner. Sırayla, en fazla bu kadar paralel.
    private const val MAX_CONCURRENT = 2

    private val lock = Any()
    private val queue = ArrayDeque<String>()
    private val queued = HashSet<String>()   // sırada bekleyen ya da inmekte olan yollar
    private var running = 0

    // Storage'da olmadığı anlaşılan yollar (ses henüz yüklenmemiş). Süreç boyunca hatırlanır ki her
    // adım gösteriminde aynı istek tekrar atılıp günlük hatayla dolmasın; uygulama yeniden
    // açılınca bir kez daha denenir (bu arada yüklenmiş olabilir).
    private val missing = HashSet<String>()

    private fun sanitizedFileName(storagePath: String): String =
        storagePath.substringAfterLast('/').ifEmpty { storagePath.replace('/', '_') }

    fun localFile(context: Context, storagePath: String): File {
        val dir = File(context.filesDir, CACHE_SUBDIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, sanitizedFileName(storagePath))
    }

    // Asıl ad yalnızca indirme tamamlanınca verildiği için var olan dosya her zaman eksiksizdir.
    fun isCached(context: Context, storagePath: String): Boolean =
        localFile(context, storagePath).exists()

    // Zaten cache'teyse, sıradaysa ya da Storage'da olmadığı biliniyorsa hiçbir şey yapmaz.
    // Değilse sıranın sonuna ekler; çağıran taraf hiçbir zaman bloklanmaz veya çökmez.
    fun prefetch(context: Context, storagePath: String) {
        val appContext = context.applicationContext
        if (localFile(appContext, storagePath).exists()) return
        synchronized(lock) {
            if (storagePath in missing || !queued.add(storagePath)) return
            queue.addLast(storagePath)
        }
        pump(appContext)
    }

    fun prefetchAll(context: Context, storagePaths: List<String>) {
        storagePaths.forEach { prefetch(context, it) }
    }

    private fun pump(context: Context) {
        while (true) {
            val next = synchronized(lock) {
                if (running >= MAX_CONCURRENT) return
                val path = queue.removeFirstOrNull() ?: return
                running++
                path
            }
            download(context, next)
        }
    }

    private fun download(context: Context, storagePath: String) {
        val target = localFile(context, storagePath)
        // Önce yan dosyaya iner, bitince asıl ada taşınır: yarıda kalan (uygulama kapandı, ağ
        // koptu) indirme asıl adla hiç görünmez, dolayısıyla bozuk dosya çalınmaya çalışılmaz.
        val part = File(target.parentFile, target.name + PART_SUFFIX)
        part.delete()
        try {
            FirebaseStorage.getInstance().reference.child(storagePath)
                .getFile(part)
                .addOnSuccessListener {
                    if (part.length() > 0 && (part.renameTo(target) || target.exists())) {
                        Log.d(TAG, "RemoteAudioCache: indirildi $storagePath")
                    } else {
                        Log.w(TAG, "RemoteAudioCache: indirilen dosya kullanılamadı $storagePath")
                    }
                    part.delete()
                    finish(context, storagePath)
                }
                .addOnFailureListener { e ->
                    part.delete()
                    val notFound = (e as? StorageException)?.errorCode ==
                        StorageException.ERROR_OBJECT_NOT_FOUND
                    if (notFound) {
                        // Beklenen durum (ses henüz yüklenmedi): yığın izi basmadan tek satır.
                        synchronized(lock) { missing.add(storagePath) }
                        Log.d(TAG, "RemoteAudioCache: Storage'da yok $storagePath")
                    } else {
                        // Ağ hatası vb.: hatırlanmaz, adım bir dahaki gösterildiğinde yeniden denenir.
                        Log.w(TAG, "RemoteAudioCache: indirilemedi $storagePath: ${e.message}")
                    }
                    finish(context, storagePath)
                }
        } catch (e: Exception) {
            part.delete()
            Log.e(TAG, "RemoteAudioCache: prefetch başlatılamadı $storagePath", e)
            finish(context, storagePath)
        }
    }

    private fun finish(context: Context, storagePath: String) {
        synchronized(lock) {
            queued.remove(storagePath)
            running--
        }
        pump(context)
    }
}

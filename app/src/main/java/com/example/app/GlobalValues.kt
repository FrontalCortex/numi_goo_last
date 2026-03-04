package com.example.app

import android.content.Context
import com.example.app.model.QuestionMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.pow
import java.io.File

object GlobalValues {
    var lessonStep: Int = 1
    var mapFragmentStepIndex: Int = 1
    var stepIndex = 0
    var tutorialIsWorked = false
    var scrollPosition = 0  // Scroll pozisyonunu global olarak tutacak değişken
    /** Tutorial 1 akışındayken 1; login gösterildikten veya başka tutorial'a geçildikten sonra 0. */
    var currentTutorialNumber: Int = 0

    /** Bu açılışta (session) tutorial 1 claim'de login zaten gösterildi mi? Process yeniden başlayınca false olur. */
    fun randomNumberChangeToString(digitCount: Int): String {

        // Minimum ve maksimum değerleri hesapla
        val min = 10.0.pow(digitCount - 1).toInt()  // Örnek: 3 basamak için 100
        val max = 10.0.pow(digitCount).toInt() - 1  // Örnek: 3 basamak için 999

        // Random sayı üret
        return (min..max).random().toString()
    }

    fun generateRandomNumber(digitCount: Int): Int {
        // Basamak sayısı kontrolü
        if (digitCount < 1) return 0

        // Minimum ve maksimum değerleri hesapla
        val min = 10.0.pow(digitCount - 1).toInt()  // Örnek: 3 basamak için 100
        val max = 10.0.pow(digitCount).toInt() - 1  // Örnek: 3 basamak için 999

        // Random sayı üret
        return (min..max).random()
    }

    /**
     * Aynı process içinde, sohbete gir-çık yapsan bile pending upload'ları ve
     * medya görünürlük/iptal durumlarını korumak için hafıza içi buffer.
     */
    val pendingQuestionMessages: MutableMap<String, MutableList<QuestionMessage>> = mutableMapOf()

    /** Hangi questionId altında hangi mesajların medyası (blur kalkmış) göründü. */
    val revealedMediaIdsByQuestion: MutableMap<String, MutableSet<String>> = mutableMapOf()

    /** Hangi questionId altında hangi pending clientId'ler için upload iptal edildi. */
    val canceledUploadIdsByQuestion: MutableMap<String, MutableSet<String>> = mutableMapOf()

    /** Her pending clientId için tekrar upload edebilmek üzere meta bilgiler. */
    val uploadMetaByClientId: MutableMap<String, PendingUploadMeta> = mutableMapOf()

    /** Hangi questionId altında hangi pending clientId'lerin upload'ı gerçekten başladı. */
    val activeUploadIdsByQuestion: MutableMap<String, MutableSet<String>> = mutableMapOf()

    /** Hangi questionId altında hangi mesajların indirilmesi devam ediyor (alıcı taraf). */
    val activeDownloadIdsByQuestion: MutableMap<String, MutableSet<String>> = mutableMapOf()

    /**
     * Karşıdan gelen mesajlar için, medya dosyasının cihazda nereye indirildiğini tutar.
     * key: messageId, value: absolute local file path.
     */
    val downloadedMediaByMessageId: MutableMap<String, String> = mutableMapOf()

    private const val PREFS_NAME = "GlobalValuesPrefs"
    private const val KEY_DOWNLOADED_MEDIA = "downloaded_media_by_message"

    fun loadDownloadedMediaCache(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DOWNLOADED_MEDIA, null) ?: return
        val type = object : TypeToken<Map<String, String>>() {}.type
        val rawMap: Map<String, String> = try {
            Gson().fromJson<Map<String, String>>(json, type) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }

        downloadedMediaByMessageId.clear()
        for ((messageId, path) in rawMap) {
            if (path.isNullOrBlank()) continue
            val file = File(path)
            if (file.exists()) {
                downloadedMediaByMessageId[messageId] = file.absolutePath
            }
        }
    }

    fun persistDownloadedMediaCache(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Sadece gerçekten var olan dosyaları yaz.
        val filtered = downloadedMediaByMessageId.filterValues { path ->
            !path.isNullOrBlank() && File(path).exists()
        }
        val json = Gson().toJson(filtered)
        prefs.edit().putString(KEY_DOWNLOADED_MEDIA, json).apply()
    }
}

data class PendingUploadMeta(
    val questionId: String,
    val type: String,
    val filePath: String?,
    val textContent: String?,
    val caption: String?
)


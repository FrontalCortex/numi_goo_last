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
        return randomUniqueNumberStrings(digitCount, 1).single()
    }

    /**
     * [digitCount] basamaklı aralıktan [count] adet sayı string'i üretir.
     * Havuz yeterliyse hepsi birbirinden farklı; [count] havuzdan büyükse önce tüm değerler
     * bir kez (karışık), kalanlar rastgele (tekrarlı olabilir).
     */
    fun randomUniqueNumberStrings(digitCount: Int, count: Int): List<String> {
        if (count <= 0) return emptyList()
        val min = digitRangeMin(digitCount)
        val max = digitRangeMax(digitCount)
        val pool = (min..max).toMutableList()
        if (count <= pool.size) {
            pool.shuffle()
            return pool.take(count).map { it.toString() }
        }
        pool.shuffle()
        val result = pool.map { it.toString() }.toMutableList()
        while (result.size < count) {
            result.add((min..max).random().toString())
        }
        result.shuffle()
        return result
    }

    private fun digitRangeMin(digitCount: Int): Int =
        10.0.pow((digitCount - 1).coerceAtLeast(0)).toInt()

    private fun digitRangeMax(digitCount: Int): Int =
        10.0.pow(digitCount).toInt() - 1

    fun generateRandomNumber(digitCount: Int): Int {
        if (digitCount < 1) return 0
        return (digitRangeMin(digitCount)..digitRangeMax(digitCount)).random()
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

    data class PendingLessonProgressAnimation(
        val fromFilledSegments: Int,
        val toFilledSegments: Int,
    )

    /** Map görünür olduğunda tüketilecek progress artış animasyonları. */
    val pendingLessonProgressAnimations: MutableMap<String, PendingLessonProgressAnimation> = mutableMapOf()
    var canConsumePendingLessonProgressAnimations: Boolean = false

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


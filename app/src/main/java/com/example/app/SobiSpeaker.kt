package com.example.app

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Sobi'nin mesajlarını sesli okur (kullanıcı kararı 01.11.2026): 7 yaşındaki bir çocuk 3 cümlelik
 * mesajı okumakta zorlanabilir. Telefonun kendi ses motoru (uygulamanın dilinde): internet ve kredi gerekmez.
 *
 * Otomatik okuma çocuğun tercihi ([autoRead], cihazda saklanır, varsayılan açık); uygulamanın sesi
 * kapalıysa ("sound_enabled") otomatik okuma da yapılmaz, 🔊 düğmesi yine okur. O dilin sesi yoksa
 * ([available] false) düğmeler gizlenir.
 */
class SobiSpeaker(context: Context, private val onReady: () -> Unit) {

    private val appContext = context.applicationContext
    private var ready = false
    // Hazır olma geri çağrısı ana iş parçacığında, kurucudan sonra geliyor; o anda tts atanmış olur.
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Uygulamanın dili (AppLanguage varsayılan dili de ayarlıyor).
                val voice = when (AppLanguage.textLanguage()) {
                    AppLanguage.TURKISH -> Locale("tr", "TR")
                    // id-ID: ses motorları "in" ve "id"yi aynı dil sayıyor; etiketten kurmak ikisinde de çalışıyor.
                    AppLanguage.INDONESIAN -> Locale.forLanguageTag("id-ID")
                    // Arapça ses motorda yoksa ready false kalır: düğmeler gizlenir, Sobi sessiz.
                    AppLanguage.ARABIC -> Locale.forLanguageTag("ar")
                    else -> Locale.US
                }
                val r = tts.setLanguage(voice)
                ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                if (ready) {
                    // Çocuk için biraz yavaş.
                    tts.setSpeechRate(0.92f)
                    onReady()
                }
            }
        }
    }

    val available: Boolean get() = ready

    var autoRead: Boolean
        get() = prefs().getBoolean(KEY_AUTO, true)
        set(value) { prefs().edit().putBoolean(KEY_AUTO, value).apply() }

    private val soundOn: Boolean
        get() = appContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getBoolean("sound_enabled", true)

    fun speak(text: String) {
        if (!ready) return
        tts.speak(clean(text), TextToSpeech.QUEUE_FLUSH, null, "sobi")
    }

    /** Yeni Sobi mesajı: otomatik okuma açıksa okunur. */
    fun speakIfAuto(text: String) {
        if (autoRead && soundOn) speak(text)
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS = "sobi"
        private const val KEY_AUTO = "auto_read"

        /** Ses motorunun takıldığı işaretler: tırnaklar, eksi işareti ("434 - 86" → "434 eksi 86"). */
        fun clean(text: String): String {
            val (minus, plus) = when (AppLanguage.textLanguage()) {
                AppLanguage.TURKISH -> "eksi" to "artı"
                AppLanguage.INDONESIAN -> "kurang" to "tambah"
                AppLanguage.ARABIC -> "ناقص" to "زائد"
                else -> "minus" to "plus"
            }
            return text
            .replace(Regex("""(\d)\s*[-−]\s*(\d)"""), "$1 $minus $2")
            .replace(Regex("""(\d)\s*\+\s*(\d)"""), "$1 $plus $2")
            .replace(Regex("""[«»“”"]"""), "")
            .replace("‹", "")
            .replace(Regex("[\u2066-\u2069]"), "") // RtlSupport.ltr yalıtım işaretleri
        }
    }
}

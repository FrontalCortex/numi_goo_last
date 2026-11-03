package com.example.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Uygulamanın dili: Ayarlar → Dil'de seçilir, telefonun dilinden bağımsız.
 *
 * ## Neden AppCompatDelegate.setApplicationLocales değil
 * Metinlerin çoğu uygulama context'inden okunuyor ([appString], [GlobalLessonData]); AppCompat'ın
 * dil seçimi Android 13 öncesinde yalnızca aktiviteleri değiştiriyor, uygulama context'i telefonun
 * dilinde kalıyordu. Burada dil hem [NumiGooApplication.attachBaseContext]'te hem her aktivitenin
 * attachBaseContext'inde ([FontScaleLimiter.wrap]) aynı yapılandırmayla veriliyor.
 *
 * ## "Telefonun dili"
 * Telefon Türkçe ise Türkçe, Endonezce ise Endonezce, değilse İngilizce: varsayılan `values/`
 * Türkçe olduğu için desteklenmeyen bir dili olduğu gibi bırakmak Türkçe gösterirdi.
 *
 * ## Endonezce: "id" ve "in"
 * Endonezcenin ISO kodu "id", ama Java'nın Locale'i eski kodu "in"i kullanıyor: `Locale("id")`,
 * `Locale.forLanguageTag("id")` ve telefonun yapılandırması `language` olarak "in" verir (sürüme
 * göre "id" de olabilir). Kaynaklar `values-in`'de; Resources dil etiketini kaynak aramadan önce
 * "in"e çeviriyor, yani hangisi verilirse verilsin `values-in` seçilir. Bu yüzden dil
 * karşılaştırmaları [normalize] ile yapılıyor; sunucuya ve `users/{uid}.lang`'a ise hep "id"
 * ([current]) gidiyor, sunucu "in" bilmiyor.
 */
object AppLanguage {
    const val SYSTEM = "system"
    const val TURKISH = "tr"
    const val ENGLISH = "en"
    const val INDONESIAN = "id"

    /** Seçim ekranındaki sıra. */
    val CHOICES = listOf(SYSTEM, TURKISH, ENGLISH, INDONESIAN)

    private const val PREFS = "app_language"
    private const val KEY_CHOICE = "choice"

    /** Kullanıcının seçimi: [SYSTEM], [TURKISH], [ENGLISH] ya da [INDONESIAN]. */
    fun choice(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CHOICE, SYSTEM) ?: SYSTEM

    /** Şu an kullanılan dil kodu ("tr" / "en" / "id"); [SYSTEM] telefonun diline göre çözülür. */
    fun current(context: Context): String = resolve(choice(context))

    fun isTurkish(): Boolean = Locale.getDefault().language == TURKISH

    /**
     * Cümleyi kodda kuran yerlerin ([com.example.app.abacus.MistakeAnalyzer] gibi) dili: [TURKISH],
     * [ENGLISH] ya da [INDONESIAN]. [wrap] varsayılan dili ayarladığı için ona bakıyor.
     */
    fun textLanguage(): String = supported(Locale.getDefault().language)

    /** Java'nın eski Endonezce kodu "in" → "id"; öbür diller olduğu gibi. */
    private fun normalize(language: String): String = if (language == "in") INDONESIAN else language

    /** Desteklenen dillerden biri; tanınmayan dil İngilizce. */
    private fun supported(language: String): String = when (val l = normalize(language)) {
        TURKISH, INDONESIAN -> l
        else -> ENGLISH
    }

    private fun resolve(choice: String): String = when (choice) {
        TURKISH, ENGLISH, INDONESIAN -> choice
        else -> supported(Resources.getSystem().configuration.locales[0].language)
    }

    /** [base]'i seçili dille yeniden yapılandırır; dil zaten aynıysa olduğu gibi döner. */
    fun wrap(base: Context): Context {
        val locale = Locale.forLanguageTag(current(base))
        Locale.setDefault(locale)
        val config = base.resources.configuration
        if (normalize(config.locales[0].language) == normalize(locale.language)) return base
        val localized = Configuration(config)
        localized.setLocale(locale)
        return base.createConfigurationContext(localized)
    }

    /**
     * Seçimi kaydeder ve uygulamayı baştan açar: sınıf açılışında okunmuş metinler (enum etiketleri,
     * ders şablonu) ve uygulama context'i ancak yeni süreçte yeni dile geçiyor.
     */
    fun apply(activity: Activity, choice: String) {
        if (choice == choice(activity)) return
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_CHOICE, choice).commit()
        val restart = Intent(activity, SplashActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(restart)
        activity.finishAffinity()
        exitProcess(0)
    }
}

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
 * Telefon Türkçe ise Türkçe, değilse İngilizce: varsayılan `values/` Türkçe olduğu için
 * Endonezce bir telefonda olduğu gibi bırakmak Türkçe gösterirdi.
 */
object AppLanguage {
    const val SYSTEM = "system"
    const val TURKISH = "tr"
    const val ENGLISH = "en"

    /** Seçim ekranındaki sıra. */
    val CHOICES = listOf(SYSTEM, TURKISH, ENGLISH)

    private const val PREFS = "app_language"
    private const val KEY_CHOICE = "choice"

    /** Kullanıcının seçimi: [SYSTEM], [TURKISH] ya da [ENGLISH]. */
    fun choice(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CHOICE, SYSTEM) ?: SYSTEM

    /** Şu an kullanılan dil kodu ("tr" / "en"); [SYSTEM] telefonun diline göre çözülür. */
    fun current(context: Context): String = resolve(choice(context))

    fun isTurkish(): Boolean = Locale.getDefault().language == TURKISH

    private fun resolve(choice: String): String = when (choice) {
        TURKISH, ENGLISH -> choice
        else -> if (Resources.getSystem().configuration.locales[0].language == TURKISH) TURKISH else ENGLISH
    }

    /** [base]'i seçili dille yeniden yapılandırır; dil zaten aynıysa olduğu gibi döner. */
    fun wrap(base: Context): Context {
        val locale = Locale(current(base))
        Locale.setDefault(locale)
        val config = base.resources.configuration
        if (config.locales[0].language == locale.language) return base
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

// Kaynak (Android):
//  - app/src/main/java/com/example/app/SplashActivity.kt (bekleme süreleri, checkLoginStatus yönlendirmesi)
//  - app/src/main/java/com/example/app/LoginStartActivity.kt ("login_start_ever_shown" bayrağı)
//  - app/src/main/java/com/example/app/UserInfoFragment.kt (yaş doğrulaması, doğum yılı, kaynak listesi)
//  - app/src/main/java/com/example/app/TeacherLoginActivity.kt (öğretmen girişi ön denetimi)
//  - app/src/main/java/com/example/app/TeacherForgotPasswordEmailFragment.kt (e-posta ön denetimi)
//  - app/src/main/java/com/example/app/TeacherNewPasswordFragment.kt (yeni şifre ön denetimi)
//  - app/src/main/java/com/example/app/LoginActivity.kt, RegisterActivity.kt (Toast metinleri)
//
// Firebase / Intent / SharedPreferences çağrıları taşınmadı; yalnızca saf kurallar ve metinler var.
// AuthRules.kt'ye dokunmamak için ayrı dosyadadır; AuthRules'taki kurallar burada yinelenmez.
package com.sorobit.shared.model

/** Açılış ekranı (SplashActivity). */
object SplashRules {
    /**
     * Açılışın en kısa süresi (Android: SPLASH_ANIMATION_MS; eskiden 2000 ms idi). Marka bir kez okunacak
     * kadar: sistem ikonu + yazının gelişi ([BRAND_REVEAL_MS]) + kısa bekleme. Eskiden öğretici için
     * Remote Config indirilip açılış 8 sn'ye kadar bekletiliyordu; bu bekleme kaldırıldı.
     */
    const val SPLASH_ANIMATION_MS = 1200L

    /** Marka (ikon + "Sorobit" yazısı) geliş animasyonu. */
    const val BRAND_REVEAL_MS = 450L

    /** Zorunlu güncelleme kontrolü animasyon bittikten sonra açılışı en fazla bu kadar bekletir. */
    const val FORCE_UPDATE_MAX_WAIT_AFTER_SPLASH_MS = 1000L

    /** Bildirimden gelen açılışta (animasyon beklenmez) zorunlu güncelleme bekleme üst sınırı. */
    const val FORCE_UPDATE_MAX_WAIT_DEEP_LINK_MS = 1500L

    /**
     * "Bu cihazda daha önce hesap açıldı mı" bayrağı (Android: DeviceAccountStore, "AppPrefs"). İlk
     * öğreticinin gösterilme şartı tek bu: açıldıysa oturum kapalı olsa bile öğretici gösterilmez.
     * Eskiden şart `first_tutorial_shown` idi ve ilk öğretici başarısız geçilince hiç yazılmıyordu.
     */
    const val PREF_ACCOUNT_EVER_CREATED = "account_ever_created"

    /** Eski kurulumlardan devralınan bayrak; yalnızca geçişte okunur, artık yazılmaz. */
    const val PREF_LEGACY_FIRST_TUTORIAL_SHOWN = "first_tutorial_shown"

    /**
     * Android: DeviceAccountStore.hasEverHadAccount. [accountEverCreated] kalıcı bayrak, [signedIn] oturum
     * açık mı, [legacyFirstTutorialShown] eski bayrak. Dönüş true ise (ve bayrak henüz yazılı değilse)
     * çağıran [PREF_ACCOUNT_EVER_CREATED]'i true yazmalıdır: [shouldPersistAccountFlag].
     */
    fun hasEverHadAccount(accountEverCreated: Boolean, signedIn: Boolean, legacyFirstTutorialShown: Boolean): Boolean =
        accountEverCreated || signedIn || legacyFirstTutorialShown

    fun shouldPersistAccountFlag(accountEverCreated: Boolean, signedIn: Boolean, legacyFirstTutorialShown: Boolean): Boolean =
        !accountEverCreated && (signedIn || legacyFirstTutorialShown)

    /** Oturum açıkken başlangıç hedefi hep harita; oturum yokken hesap geçmişi yoksa öğretici (Android: checkLoginStatus). */
    fun showsFirstTutorial(hadAccount: Boolean): Boolean = !hadAccount

    /** "AppPrefs" içindeki bayrak: giriş başlangıç ekranı en az bir kez gösterildi mi. */
    const val PREF_LOGIN_START_EVER_SHOWN = "login_start_ever_shown"

    enum class Route {
        /** İnternet yok: giriş akışına girilmeden ana ekran açılır (orada çevrimdışı ekranı gösterilir). */
        OFFLINE_MAIN,

        /** Giriş başlangıç ekranı (LoginStartActivity). */
        LOGIN_START,

        /** Oturum var: tek cihaz kuralı denetlenir (SessionDeviceRules.decide), sonra ana ekran. */
        CHECK_SINGLE_DEVICE,

        /** İlk açılış, oturum yok: misafir olarak ana ekran (öğretici ya da harita). */
        GUEST_MAIN,
    }

    /** Android: SplashActivity.checkLoginStatus dallarının sırası. */
    fun route(isOnline: Boolean, loginStartEverShown: Boolean, hasExistingLogin: Boolean): Route =
        if (!isOnline) {
            Route.OFFLINE_MAIN
        } else if (loginStartEverShown && !hasExistingLogin) {
            Route.LOGIN_START
        } else if (hasExistingLogin) {
            Route.CHECK_SINGLE_DEVICE
        } else {
            Route.GUEST_MAIN
        }
}

/** Yaş ve "bizi nereden duydunuz" adımları (UserInfoFragment). */
object UserInfoRules {
    const val MIN_AGE = 1
    const val MAX_AGE = 120

    /** Yaş alanının en çok karakter sayısı (fragment_user_info.xml: maxLength). */
    const val AGE_MAX_LENGTH = 3

    const val MESSAGE_INVALID_AGE = "Lütfen geçerli bir yaş girin"

    /** Girişte e-posta kayıtlı çıkmayınca kayıt akışına yönlendirilirken gösterilen bilgi. */
    const val MESSAGE_EMAIL_NOT_REGISTERED_REDIRECT =
        "Bu e-posta adresi kayıtlı değil. Kayıt sayfasına yönlendirildin."

    /**
     * "Bizi nereden duydun?" seçenekleri, ekrandaki sırayla: `acquisitionSource` alanına yazılan DEĞERLER
     * (istatistik anahtarı; değişirse eski kayıtlarla yeni kayıtlar ayrı sayılır). Güncel Android'de iki
     * seçenek eklendi: "Okul/Öğretmen" ve "Veli/WhatsApp" (davet döngüsü). "Youtube" değer olarak kaldı,
     * yalnızca görünen adı "YouTube" oldu: bkz. [SOURCE_DISPLAY_NAMES_TR].
     */
    val SOURCE_NAMES: List<String> = listOf(
        "Facebook",
        "Instagram",
        "Youtube",
        "Google Araması",
        "Arkadaş/Aile",
        "Okul/Öğretmen",
        "Veli/WhatsApp",
        "TikTok",
        "Uygulama Mağazası",
        "Diğer",
    )

    /** [SOURCE_NAMES] ile aynı sırada Türkçe görünen adlar (Android bunları dile göre çevirir; Türkçe metinler burada varsayılandır). */
    val SOURCE_DISPLAY_NAMES_TR: List<String> = listOf(
        "Facebook", "Instagram", "YouTube", "Google Araması", "Arkadaş/Aile", "Okul/Öğretmen",
        "Veli/WhatsApp", "TikTok", "Uygulama Mağazası", "Diğer",
    )

    /** Android: `text.trim().toIntOrNull()`; 1..120 dışındaysa ya da sayı değilse null. */
    fun parseAge(rawAge: String?): Int? {
        val age = rawAge?.trim()?.toIntOrNull()
        return if (age != null && age in MIN_AGE..MAX_AGE) age else null
    }

    fun isValidAge(rawAge: String?): Boolean = parseAge(rawAge) != null

    /** Android: `Calendar.YEAR - age` (ay/gün dikkate alınmaz). */
    fun birthYearFromAge(age: Int, currentYear: Int): Int = currentYear - age

    /** Yaş geçerliyse doğum yılı, değilse -1. */
    fun birthYearFromRawAge(rawAge: String?, currentYear: Int): Int {
        val age = parseAge(rawAge) ?: return -1
        return birthYearFromAge(age, currentYear)
    }
}

/**
 * Kayıt soruları akışı (Android: UserInfoFragment.Step). Güncel Android'de öğrenci kaydı tanışma ve seri
 * hedefi adımlarını da içerir; öğretmene yalnızca [AGE] ve [SOURCE] sorulur.
 */
object SignupFlowRules {
    enum class Step(val isIntro: Boolean = false, val asks: Boolean = false) {
        MEET(isIntro = true),
        BRIEF(isIntro = true),
        AGE(asks = true),
        SOURCE(asks = true),
        /** Abaküs bilgisi; yalnızca öğrenci. */
        SOROBAN(asks = true),
        GOAL(asks = true),
        ROADMAP,
        CHALLENGE_INTRO,
        CHALLENGE(asks = true),
        REMINDER(asks = true),
    }

    /** Bu akışta gösterilen adımlar, sırayla: öğrencide hepsi, öğretmende yalnızca yaş ve kaynak. */
    fun flowSteps(streakStepsEnabled: Boolean): List<Step> =
        if (streakStepsEnabled) Step.entries else listOf(Step.AGE, Step.SOURCE)

    /** Seri adımları yalnızca öğrenciye sorulur (Android: `streakStepsEnabled = !forceTeacher`). */
    fun streakStepsEnabled(forceTeacher: Boolean): Boolean = !forceTeacher

    /** İlerleme çubuğu yüzdesi: tanışma ekranları sayılmaz (0); ilk soru ilk dilimi doldurur. */
    fun progressFor(step: Step, streakStepsEnabled: Boolean): Int {
        if (step.isIntro) return 0
        val counted = flowSteps(streakStepsEnabled).filter { !it.isIntro }
        return (counted.indexOf(step) + 1) * 100 / counted.size
    }

    /** Tanışmadaki "N kısa sorum var" sayısı: soru soran adımlar. */
    fun questionCount(streakStepsEnabled: Boolean): Int = flowSteps(streakStepsEnabled).count { it.asks }

    /** Seri günlük hedef seçenekleri (dakika) ve varsayılan hatırlatma saati (Android: StreakRepository). */
    val GOAL_OPTIONS: List<Int> = listOf(5, 10, 20)
    val CHALLENGE_OPTIONS: List<Int> = listOf(3, 5, 7)
    const val DEFAULT_REMINDER_HOUR = 19

    /** "Abaküs biliyor musun?" cevabı (Android: SorobanLevel); sıra bilmeyenden bilene. */
    val SOROBAN_LEVELS: List<String> = listOf("none", "some", "good")
    const val SOROBAN_LEVEL_FIELD = "sorobanLevel"

    /** Bekleyen kayıt cevabı bu süreden eskiyse atılır (24 saat). */
    const val SOROBAN_PENDING_MAX_AGE_MS = 24L * 60 * 60 * 1000
}

/** Öğretmen girişi ve şifre sıfırlama ön denetimleri. Hata yoksa null. */
object TeacherLoginRules {
    const val MESSAGE_PASSWORD_UPDATED_LOGIN_MANUALLY = "Şifre güncellendi. Giriş yapabilirsiniz."

    /** TeacherLoginActivity.loginTeacher: e-posta `trim()` edilir, şifre edilmez. */
    fun validateLogin(rawEmail: String, password: String): String? {
        val email = rawEmail.trim()
        if (email.isEmpty() || password.isEmpty()) return "Lütfen tüm alanları doldurun"
        return null
    }

    /** TeacherForgotPasswordEmailFragment: metinler giriş ekranındakinden farklıdır. */
    fun validateForgotPasswordEmail(rawEmail: String?): String? {
        val email = rawEmail?.trim().orEmpty()
        if (email.isEmpty()) return AuthRules.MESSAGE_ENTER_EMAIL
        if (!AuthRules.isValidEmail(email)) return "Geçerli bir e-posta adresi girin"
        return null
    }

    /** TeacherNewPasswordFragment: şifreler `trim()` edilmez. */
    fun validateNewPassword(newPassword: String, confirm: String): String? {
        if (newPassword.length < AuthRules.MIN_PASSWORD_LENGTH) {
            return "Şifre en az ${AuthRules.MIN_PASSWORD_LENGTH} karakter olmalıdır"
        }
        if (newPassword != confirm) return "Şifreler eşleşmiyor"
        return null
    }
}

/** Giriş/kayıt ekranlarının sabit metinleri (Android'de Kotlin içinde yazılı Toast ve düğme metinleri). */
object LoginFlowTexts {
    const val BUTTON_CONTINUE = "Devam Et"
    const val BUTTON_SENDING = "Gönderiliyor..."
    const val BUTTON_VERIFY = "Doğrula"
    const val BUTTON_VERIFYING = "Doğrulanıyor..."
    const val BUTTON_UPDATE_PASSWORD = "Şifreyi güncelle"
    const val BUTTON_UPDATING = "Güncelleniyor..."

    const val MESSAGE_CODE_RESENT = "Kod tekrar gönderildi"
    const val MESSAGE_GOOGLE_LOGIN_FAILED = "Google girişi başarısız"
    const val MESSAGE_GOOGLE_REGISTER_FAILED = "Google kaydı başarısız"

    /** OtpVerificationFragment başlığı. */
    fun otpTitle(forPasswordReset: Boolean, isRegistration: Boolean): String = when {
        forPasswordReset -> "Şifre sıfırlama"
        isRegistration -> "Hesap oluştur"
        else -> "E-postanı doğrula"
    }

    /** RegisterActivity.setupTermsAndPrivacyText */
    const val TERMS_FULL_TEXT =
        "Hesap oluşturarak Kullanım Şartlarını ve Gizlilik Politikasını kabul etmiş olursunuz."
    const val TERMS_LINK_TEXT = "Kullanım Şartlarını"
    const val PRIVACY_LINK_TEXT = "Gizlilik Politikasını"
    const val TERMS_URL = "https://numigo-new.web.app/privacy-policy.html#tos-1"
    const val PRIVACY_URL = "https://numigo-new.web.app/privacy-policy.html#pp-1"

    /**
     * Güncel Android: sayfa uygulamanın dilinde (çapalar hepsinde aynı): tr → privacy-policy-tr.html,
     * id → -id, ar → -ar, diğerleri (İngilizce asıl metin) privacy-policy.html. [langCode] ISO kodu.
     */
    fun policyPage(langCode: String): String = when (langCode) {
        "tr" -> "privacy-policy-tr.html"
        "id" -> "privacy-policy-id.html"
        "ar" -> "privacy-policy-ar.html"
        else -> "privacy-policy.html"
    }

    fun termsUrl(langCode: String): String = "https://numigo-new.web.app/${policyPage(langCode)}#tos-1"
    fun privacyUrl(langCode: String): String = "https://numigo-new.web.app/${policyPage(langCode)}#pp-1"

    /** Hesabın "az önce açıldı" sayılacağı pencere (Android: RegisterActivity.NEW_ACCOUNT_WINDOW_MS). */
    const val NEW_ACCOUNT_WINDOW_MS = 2 * 60 * 1000L
}

/**
 * AuthRules'taki kullanıcı kimliği üreteçlerinin parametresiz sarmalayıcıları: Swift'e varsayılan
 * parametre taşınmadığı ve `kotlin.random.Random` oradan verilemediği için.
 */
object UserIdRules {
    fun generateCandidate(): String = AuthRules.generateUserIdCandidate()

    fun generateFallback(nowMs: Long): String = AuthRules.generateFallbackUserId(nowMs)
}

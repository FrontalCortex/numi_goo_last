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
    /** Açılış animasyonunun en kısa süresi. */
    const val SPLASH_ANIMATION_MS = 2000L

    /** Remote Config bu kadar ek sürede gelmezse beklenmeden devam edilir. */
    const val SPLASH_MAX_EXTRA_WAIT_MS = 8000L

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

    /** Kart sırasıyla aynı; seçilen ad `acquisitionSource` alanına aynen yazılır. */
    val SOURCE_NAMES: List<String> = listOf(
        "Facebook",
        "Instagram",
        "Youtube",
        "Google Araması",
        "Arkadaş/Aile",
        "TikTok",
        "Uygulama Mağazası",
        "Diğer",
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
}

/**
 * AuthRules'taki kullanıcı kimliği üreteçlerinin parametresiz sarmalayıcıları: Swift'e varsayılan
 * parametre taşınmadığı ve `kotlin.random.Random` oradan verilemediği için.
 */
object UserIdRules {
    fun generateCandidate(): String = AuthRules.generateUserIdCandidate()

    fun generateFallback(nowMs: Long): String = AuthRules.generateFallbackUserId(nowMs)
}

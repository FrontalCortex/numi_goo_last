// Kaynak (Android):
//  - app/src/main/java/com/example/app/auth/AuthManager.kt
//      (roller, OTP amaçları, e-posta normalleştirme, bekleyen kayıt belleği, kullanıcı kimliği
//       üretimi, kullanıcı dokümanı alanları/varsayılanları, Cloud Function adları ve gövdeleri,
//       hata → mesaj eşlemeleri, önbellek anahtarları)
//  - app/src/main/java/com/example/app/RegisterActivity.kt (e-posta / öğretmen kaydı doğrulaması)
//  - app/src/main/java/com/example/app/LoginActivity.kt (e-posta doğrulaması, Google sonucu)
//  - app/src/main/java/com/example/app/EditProfileFragment.kt (ad / parola doğrulaması)
//  - app/src/main/java/com/example/app/OtpVerificationFragment.kt (kod uzunluğu, bekleme süreleri)
//
// Firebase Auth / Firestore / Functions çağrıları taşınmadı; yalnızca saf kurallar var.
//
// Büyük/küçük harf: Android `String.lowercase()` kullanır (Locale.ROOT; cihaz Türkçe olsa da
// "I" → "i", "İ" → "i̇"). Kotlin ortak `lowercase()` aynı yerelden bağımsız dönüşümü yapar.
package com.sorobit.shared.model

import kotlin.random.Random

object AuthRules {
    const val ROLE_STUDENT = "STUDENT"
    const val ROLE_TEACHER = "TEACHER"

    /** OTP gönderim amacı — hedef uid'i sunucu bu bilgiye göre kendisi çözer. */
    const val OTP_PURPOSE_REGISTER = "register"
    const val OTP_PURPOSE_LOGIN = "login"
    const val OTP_PURPOSE_TEACHER_RESET = "teacher_reset"

    // ── Yerel önbellek (SharedPreferences "auth_prefs") ───────────────────────
    const val PREFS_NAME = "auth_prefs"
    const val PREF_USER_EMAIL = "user_email"
    const val PREF_USER_TYPE = "user_type"
    const val PREF_USER_NAME = "user_name"
    const val PREF_USER_ID = "user_id"

    /** "AppPrefs" içindeki ilk öğretici bayrağı (kullanıcı dokümanına kopyalanır). */
    const val APP_PREFS_NAME = "AppPrefs"
    const val PREF_FIRST_TUTORIAL_SHOWN = "first_tutorial_shown"

    /** Android: cacheBasicUser'ın yazdığı değerler. */
    fun cacheBasicUser(email: String, role: String, name: String, userId: String = ""): Map<String, String> = mapOf(
        PREF_USER_EMAIL to email,
        PREF_USER_TYPE to role,
        PREF_USER_NAME to name,
        PREF_USER_ID to userId,
    )

    // ── Cloud Function adları ─────────────────────────────────────────────────
    const val FN_CHECK_EMAIL_REGISTERED = "checkEmailRegistered"
    const val FN_SEND_STUDENT_VERIFICATION_CODE = "sendStudentVerificationCode"
    const val FN_VERIFY_REGISTRATION_CODE = "verifyRegistrationCode"
    const val FN_VERIFY_LOGIN_CODE = "verifyLoginCode"
    const val FN_VERIFY_TEACHER_PASSWORD_RESET_CODE = "verifyTeacherPasswordResetCode"
    const val FN_RESET_TEACHER_PASSWORD = "resetTeacherPassword"
    const val FN_FIND_TEACHER_EMAIL_BY_USER_ID = "findTeacherEmailByUserId"
    const val FN_CREATE_TEACHER_INVITE = "createTeacherInvite"

    // ── Firestore ─────────────────────────────────────────────────────────────
    const val COLLECTION_USERS = "users"
    const val COLLECTION_TEACHER_INVITES = "teacherInvites"

    const val FIELD_UID = "uid"
    const val FIELD_USER_ID = "userId"
    const val FIELD_EMAIL = "email"
    const val FIELD_NAME = "name"
    const val FIELD_ROLE = "role"
    const val FIELD_VERIFIED = "verified"
    const val FIELD_FIRST_TUTORIAL_SHOWN = "first_tutorial_shown"
    const val FIELD_SKIP_STEP_TUTORIAL_SHOWN = "skip_step_tutorial_shown"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_BIRTH_YEAR = "birthYear"
    const val FIELD_ACQUISITION_SOURCE = "acquisitionSource"
    const val FIELD_TEACHER_APPROVED = "teacherApproved"

    const val INVITE_FIELD_USED = "used"
    const val INVITE_FIELD_EXPIRES_AT = "expiresAt"
    const val INVITE_FIELD_USED_BY_UID = "usedByUid"
    const val INVITE_FIELD_USED_AT = "usedAt"

    /** Google ile gelen ama Firestore'da kaydı olmayan hesap için dönen özel hata kodu. */
    const val ERROR_ACCOUNT_NOT_REGISTERED = "ACCOUNT_NOT_REGISTERED"
    const val MESSAGE_GOOGLE_CANCELLED = "Kullanıcı girişi iptal edildi"

    // ── E-posta ───────────────────────────────────────────────────────────────

    /** Android: `email.trim().lowercase()` — sunucuya giden ve anahtar olarak kullanılan biçim. */
    fun normalizeEmail(email: String): String = email.trim().lowercase()

    /**
     * `android.util.Patterns.EMAIL_ADDRESS` karşılığı (tam eşleşme):
     * yerel kısım 1..256 karakter [a-zA-Z0-9+._%-], alan adı etiketi harf/rakamla başlar
     * (en çok 65 karakter), en az bir ".etiket" (en çok 26 karakter) gerekir.
     */
    private val EMAIL_ADDRESS = Regex(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
            "@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
    )

    fun isValidEmail(email: String): Boolean = EMAIL_ADDRESS.matches(email)

    /**
     * Kayıt ekranı e-posta adımı (RegisterActivity). Girdi önce `trim()` edilir.
     * Hata yoksa null.
     */
    fun validateRegisterEmail(rawEmail: String?): String? {
        val email = rawEmail?.trim() ?: ""
        if (email.isEmpty()) return "Lütfen e-posta girin"
        if (!isValidEmail(email)) return "Geçerli bir e-posta adresi giriniz."
        return null
    }

    /**
     * Giriş ekranı e-posta adımı (LoginActivity).
     * DİKKAT (Android davranışı): hata metni kayıt ekranındakinden farklıdır ("girin" / "giriniz.").
     */
    fun validateLoginEmail(rawEmail: String?): String? {
        val email = rawEmail?.trim() ?: ""
        if (email.isEmpty()) return "Lütfen e-posta girin"
        if (!isValidEmail(email)) return "Geçerli bir e-posta adresi girin"
        return null
    }

    // ── Öğretmen kaydı / parola ───────────────────────────────────────────────

    const val MIN_PASSWORD_LENGTH = 6

    /**
     * Android: RegisterActivity.registerTeacherWithOtp'un ön denetimi.
     * [name] ve [email] çağrıdan önce `trim()` edilir; parolalar edilmez. Hata yoksa null.
     */
    fun validateTeacherRegistration(rawName: String, rawEmail: String, password: String, passwordConfirm: String): String? {
        val name = rawName.trim()
        val email = rawEmail.trim()
        if (email.isEmpty() || name.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
            return "Lütfen tüm alanları doldurun"
        }
        if (!isValidEmail(email)) return "Geçerli bir e-posta adresi giriniz."
        if (password.length < 6) return "Şifre en az 6 karakter olmalı"
        if (password != passwordConfirm) return "Şifreler eşleşmiyor"
        return null
    }

    /** Öğretmen kaydında e-posta zaten kayıtlıysa gösterilen hata. */
    fun teacherEmailAlreadyRegisteredMessage(role: String?): String =
        if (role == ROLE_TEACHER) {
            "Bu e-posta zaten bir öğretmen hesabı ile kayıtlı. Lütfen öğretmen giriş ekranını kullanın."
        } else {
            "Bu e-posta başka bir hesapta kullanılıyor."
        }

    /** Öğrenci kayıt/giriş akışında e-posta denetimi sonucu (RegisterActivity.sendOtpAndShowCodeStep). */
    enum class StudentEmailFlow {
        /** Öğretmen e-postası: "Bu e-posta ile öğrenci hesabı açılamaz." */
        REJECT_TEACHER,
        /** Zaten kayıtlı öğrenci: giriş kodu gönderilir (isRegistration = false). */
        SEND_LOGIN_CODE,
        /** Kayıtlı görünüyor ama uid yok: "Kod gönderilemedi". */
        ERROR_CODE_NOT_SENT,
        /** Yeni kayıt: bekleyen kayıt oluşturulur, kayıt kodu gönderilir. */
        NEW_REGISTRATION,
    }

    fun studentEmailFlow(alreadyRegistered: Boolean, uid: String?, role: String?): StudentEmailFlow =
        if (alreadyRegistered && !uid.isNullOrEmpty()) {
            if (role == ROLE_TEACHER) StudentEmailFlow.REJECT_TEACHER else StudentEmailFlow.SEND_LOGIN_CODE
        } else if (alreadyRegistered) {
            StudentEmailFlow.ERROR_CODE_NOT_SENT
        } else {
            StudentEmailFlow.NEW_REGISTRATION
        }

    const val MESSAGE_TEACHER_EMAIL_FOR_STUDENT = "Bu e-posta ile öğrenci hesabı açılamaz."
    const val MESSAGE_CODE_NOT_SENT = "Kod gönderilemedi"
    const val MESSAGE_OPERATION_FAILED = "İşlem başarısız"
    const val MESSAGE_INVALID_INFO = "Geçersiz bilgiler"
    const val MESSAGE_ENTER_EMAIL = "E-posta adresi girin"
    const val MESSAGE_CODE_NOT_VERIFIED = "Kod doğrulanamadı"
    const val MESSAGE_PASSWORD_NOT_UPDATED = "Şifre güncellenemedi"
    const val MESSAGE_TOKEN_MISSING = "Token alınamadı"
    const val MESSAGE_SESSION_NOT_OPENED = "Oturum açılamadı"
    const val MESSAGE_TEACHER_ON_STUDENT_LOGIN = "Bu hesap öğretmen hesabı. Öğrenci giriş ekranından giriş yapılamaz."
    const val MESSAGE_CREDENTIALS_NOT_VERIFIED = "Giriş bilgileri doğrulanamadı."
    const val MESSAGE_USER_NOT_FOUND = "Kullanıcı bulunamadı"
    const val MESSAGE_NOT_STUDENT = "Bu hesap öğrenci değil"
    const val MESSAGE_NOT_TEACHER = "Bu hesap öğretmen değil"
    const val MESSAGE_WRONG_PASSWORD_OR_EMAIL = "Şifre veya e-posta hatalı"
    const val MESSAGE_EMAIL_VERIFICATION_REQUIRED = "E-posta doğrulaması gerekli. Lütfen e-postanızı doğrulayın."
    const val MESSAGE_USER_INFO_UNAVAILABLE = "Kullanıcı bilgileri alınamadı"
    const val MESSAGE_USER_NOT_CREATED = "Kullanıcı oluşturulamadı"
    const val MESSAGE_INVALID_INVITE = "Geçersiz veya süresi dolmuş kod"
    const val MESSAGE_GOOGLE_NO_TOKEN = "Google hesap bilgisi alınamadı. Lütfen tekrar deneyin."
    const val MESSAGE_GOOGLE_NOT_SELECTED = "Google hesabı seçilemedi"

    /** Android: createPendingTeacherRegistrationForOTP'nin geçerlilik koşulu (ad `isBlank` ile). */
    fun isValidPendingTeacherRegistration(email: String, name: String, password: String): Boolean {
        val normalizedEmail = normalizeEmail(email)
        return !(normalizedEmail.isEmpty() || name.isBlank() || password.length < 6)
    }

    // ── Profil düzenleme (EditProfileFragment) ────────────────────────────────

    const val MAX_NAME_LENGTH = 25

    /** Ad doğrulaması (çağıran önce `trim()` eder). Uzunluk UTF-16 kod birimi sayısıdır. */
    fun validateName(name: String): String? {
        return when {
            name.length < 1 -> "Ad boş bırakılamaz."
            name.length > 25 -> "Ad en fazla 25 karakter olabilir."
            else -> null
        }
    }

    /** Yeni parola doğrulaması (boş parola "değiştirme" demektir; çağıran ayrıca ele alır). */
    fun validateNewPassword(newPassword: String): String? =
        if (newPassword.length < 6) "Parola en az 6 karakter olmalıdır." else null

    /** Oturum sağlayıcı kimlikleri (FirebaseUser.providerData). */
    const val PROVIDER_PASSWORD = "password"
    const val PROVIDER_GOOGLE = "google.com"

    // ── Bekleyen kayıt (yalnızca bellekte) ────────────────────────────────────

    data class PendingRegistration(
        val name: String,
        val password: String,
        val role: String,
    )

    /**
     * Kayıt sırasında girilen ad/şifre/rol bilgisini, kod doğrulama adımına kadar SADECE BELLEKTE
     * taşır (Android: AuthManager.companion.pendingRegistrations). Anahtar normalleştirilmiş e-posta.
     */
    class PendingRegistrationStore {
        private val pendingRegistrations = mutableMapOf<String, PendingRegistration>()

        fun rememberPendingRegistration(email: String, name: String, password: String, role: String) {
            pendingRegistrations[normalizeEmail(email)] =
                PendingRegistration(name = name, password = password, role = role)
        }

        /** Kaydı döndürür ve SİLER (tek kullanımlık). */
        fun consumePendingRegistration(email: String): PendingRegistration? =
            pendingRegistrations.remove(normalizeEmail(email))

        /** Android: createPendingRegistrationForOTP — öğrenci için ad ve şifre boş. */
        fun createPendingRegistrationForOTP(email: String): String? {
            val normalizedEmail = normalizeEmail(email)
            if (normalizedEmail.isEmpty()) return MESSAGE_INVALID_INFO
            rememberPendingRegistration(normalizedEmail, name = "", password = "", role = ROLE_STUDENT)
            return null
        }

        /** Android: createPendingTeacherRegistrationForOTP — ad `trim()` edilerek saklanır. */
        fun createPendingTeacherRegistrationForOTP(email: String, name: String, password: String): String? {
            if (!isValidPendingTeacherRegistration(email, name, password)) return MESSAGE_INVALID_INFO
            rememberPendingRegistration(normalizeEmail(email), name.trim(), password, ROLE_TEACHER)
            return null
        }
    }

    // ── Cloud Function gövdeleri ──────────────────────────────────────────────

    /** sendStudentVerificationCode gövdesi (e-posta çağıran tarafından normalleştirilmiş olmalı). */
    fun sendVerificationCodePayload(email: String, purpose: String): Map<String, Any> = mapOf(
        "email" to email,
        "purpose" to purpose,
    )

    /** Android: resendStudentVerificationCode'un seçtiği amaç. */
    fun resendPurpose(isRegistration: Boolean): String =
        if (isRegistration) OTP_PURPOSE_REGISTER else OTP_PURPOSE_LOGIN

    fun checkEmailRegisteredPayload(email: String): Map<String, Any> = mapOf("email" to normalizeEmail(email))

    /** checkEmailRegistered yanıtı → (kayıtlı mı, uid, rol). Hata durumunda (false, null, null). */
    fun parseEmailRegisteredResult(data: Map<*, *>?): Triple<Boolean, String?, String?> {
        val registered = data?.get("registered") as? Boolean ?: false
        val uid = data?.get("uid") as? String
        val role = data?.get("role") as? String
        return Triple(registered, uid, role)
    }

    /**
     * verifyRegistrationCode gövdesi. Bekleyen kayıt varsa: ad boş değilse `name`, şifre boş
     * değilse `password`, ve her durumda `role` eklenir.
     */
    fun verifyRegistrationCodePayload(
        email: String,
        code: String,
        pending: PendingRegistration?,
        birthYear: Int? = null,
        acquisitionSource: String? = null,
    ): Map<String, Any> {
        val payload = mutableMapOf<String, Any>("email" to normalizeEmail(email), "code" to code)
        pending?.let {
            if (it.name.isNotBlank()) payload["name"] = it.name
            if (it.password.isNotEmpty()) payload["password"] = it.password
            payload["role"] = it.role
        }
        birthYear?.let { payload["birthYear"] = it }
        acquisitionSource?.let { payload["acquisitionSource"] = it }
        return payload
    }

    /**
     * verifyLoginCode gövdesi.
     * DİKKAT (Android davranışı): e-posta burada NORMALLEŞTİRİLMEDEN gönderilir (çağıranlar
     * genelde normalleştirilmiş verir).
     */
    fun verifyLoginCodePayload(email: String, code: String): Map<String, Any> = mapOf("email" to email, "code" to code)

    fun verifyTeacherPasswordResetCodePayload(email: String, code: String): Map<String, Any> = mapOf(
        "email" to normalizeEmail(email),
        "code" to code,
    )

    fun resetTeacherPasswordPayload(email: String, code: String, newPassword: String): Map<String, Any> = mapOf(
        "email" to normalizeEmail(email),
        "code" to code,
        "newPassword" to newPassword,
    )

    /** findTeacherEmailByUserId gövdesi; kimlik `trim()` sonrası boşsa null (çağrı yapılmaz). */
    fun findTeacherEmailByUserIdPayload(userId: String): Map<String, Any>? {
        val trimmedId = userId.trim()
        if (trimmedId.isEmpty()) return null
        return mapOf("userId" to trimmedId)
    }

    /** Yanıttaki özel giriş belirteci; yoksa/boşsa null ("Token alınamadı"). */
    fun parseToken(data: Map<*, *>?): String? {
        val token = data?.get("token") as? String
        return if (token.isNullOrBlank()) null else token
    }

    // ── Kullanıcı kimliği (userId) üretimi ────────────────────────────────────

    const val USER_ID_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val USER_ID_MAX_ATTEMPTS = 15

    /** 6 haneli rastgele sayı (100000..999999) + 2 büyük harf. Örn. "483920QK". */
    fun generateUserIdCandidate(random: Random = Random.Default): String {
        val randomNum = random.nextInt(100000, 1000000)
        val suffix = "${USER_ID_LETTERS.random(random)}${USER_ID_LETTERS.random(random)}"
        return "${randomNum}${suffix}"
    }

    /**
     * 15 deneme tükenince ya da sorgu hata verince kullanılan yedek: `nowMs % 1000000` + 2 harf.
     * DİKKAT (Android davranışı): baştaki sıfırlar atıldığı için sayı kısmı 6 haneden kısa olabilir.
     */
    fun generateFallbackUserId(nowMs: Long, random: Random = Random.Default): String {
        val timestamp = nowMs % 1000000
        val fallbackSuffix = "${USER_ID_LETTERS.random(random)}${USER_ID_LETTERS.random(random)}"
        return "${timestamp}${fallbackSuffix}"
    }

    // ── Kullanıcı dokümanı (users/{uid}) ──────────────────────────────────────

    /**
     * Google ile otomatik kayıt dokümanı (signInWithGoogle, autoRegister = true).
     * [walletFields] = `UserWalletFirestore.registrationWalletFields()` (`oyun` paketi).
     * `createdAt` epoch ms olarak konur; platform Timestamp'e çevirir.
     */
    fun googleStudentUserData(
        uid: String,
        userId: String,
        email: String?,
        displayName: String?,
        firstTutorialShown: Boolean,
        nowMs: Long,
        birthYear: Int? = null,
        acquisitionSource: String? = null,
        walletFields: Map<String, Any> = emptyMap(),
    ): Map<String, Any> {
        val userData = mutableMapOf<String, Any>(
            FIELD_UID to uid,
            FIELD_USER_ID to userId,
            FIELD_EMAIL to (email ?: ""),
            FIELD_NAME to (displayName ?: ""),
            FIELD_ROLE to ROLE_STUDENT,
            FIELD_VERIFIED to true, // Google ile gelenler otomatik doğrulanmış sayılır
            FIELD_FIRST_TUTORIAL_SHOWN to firstTutorialShown,
            FIELD_CREATED_AT to nowMs,
        )
        birthYear?.let { userData[FIELD_BIRTH_YEAR] = it }
        acquisitionSource?.let { userData[FIELD_ACQUISITION_SOURCE] = it }
        userData.putAll(walletFields)
        return userData
    }

    /** E-posta/parola ile öğrenci kaydı dokümanı (registerStudent): `verified = false`. */
    fun emailStudentUserData(
        uid: String,
        userId: String,
        email: String,
        name: String,
        firstTutorialShown: Boolean,
        nowMs: Long,
        walletFields: Map<String, Any> = emptyMap(),
    ): Map<String, Any> {
        val userData = mutableMapOf<String, Any>(
            FIELD_UID to uid,
            FIELD_USER_ID to userId,
            FIELD_EMAIL to email,
            FIELD_NAME to name,
            FIELD_ROLE to ROLE_STUDENT,
            FIELD_VERIFIED to false,
            FIELD_FIRST_TUTORIAL_SHOWN to firstTutorialShown,
            FIELD_CREATED_AT to nowMs,
        )
        userData.putAll(walletFields)
        return userData
    }

    /** Davet koduyla öğretmen kaydı dokümanı (registerTeacher): `verified` alanı YOK. */
    fun teacherUserData(
        uid: String,
        userId: String,
        email: String,
        name: String,
        firstTutorialShown: Boolean,
        nowMs: Long,
        walletFields: Map<String, Any> = emptyMap(),
    ): Map<String, Any> {
        val data = mutableMapOf<String, Any>(
            FIELD_UID to uid,
            FIELD_USER_ID to userId,
            FIELD_EMAIL to email,
            FIELD_NAME to name,
            FIELD_ROLE to ROLE_TEACHER,
            FIELD_FIRST_TUTORIAL_SHOWN to firstTutorialShown,
            FIELD_CREATED_AT to nowMs,
        )
        data.putAll(walletFields)
        return data
    }

    /**
     * Öğretmen davet kodu geçerli mi (registerTeacher).
     * `used` alanı YOKSA kullanılmış sayılır (varsayılan true). Süre: bitiş < şimdi ise geçersiz
     * (eşitse geçerli); bitiş alanı yoksa süresiz.
     */
    fun isTeacherInviteValid(exists: Boolean, used: Boolean?, expiresAtMs: Long?, nowMs: Long): Boolean {
        val isUsed = used ?: true
        return !(!exists || isUsed || (expiresAtMs != null && expiresAtMs < nowMs))
    }

    fun teacherInviteUsedUpdate(uid: String, nowMs: Long): Map<String, Any> = mapOf(
        INVITE_FIELD_USED to true,
        INVITE_FIELD_USED_BY_UID to uid,
        INVITE_FIELD_USED_AT to nowMs,
    )

    /** OTP ile giriş sonrası rol (alan yoksa STUDENT). Öğrenci değilse giriş reddedilir. */
    fun otpLoginRole(roleField: String?): String = roleField ?: ROLE_STUDENT

    fun isOtpLoginAllowed(roleField: String?): Boolean = otpLoginRole(roleField) == ROLE_STUDENT

    /** Google ile girişte mevcut doküman öğretmense giriş reddedilir (rol yoksa STUDENT). */
    fun isGoogleLoginAllowed(roleField: String?): Boolean = (roleField ?: ROLE_STUDENT) != ROLE_TEACHER

    /**
     * skip_step_tutorial_shown okuması (checkSkipStepTutorialShown): alan varsa değeri (Boolean
     * değilse false), doküman/alan yoksa false. Kullanıcı yoksa ya da okuma hata verirse çağıran
     * `true` ("gösterilmiş say") kullanır.
     */
    fun skipStepTutorialShown(exists: Boolean, data: Map<String, Any?>?): Boolean =
        if (exists && data != null && data.containsKey(FIELD_SKIP_STEP_TUTORIAL_SHOWN)) {
            data[FIELD_SKIP_STEP_TUTORIAL_SHOWN] as? Boolean ?: false
        } else {
            false
        }

    // ── Hata → mesaj eşlemeleri ───────────────────────────────────────────────

    /**
     * Firebase ile Google kimlik doğrulaması başarısız olunca gösterilen mesaj
     * (signInWithGoogle). [errorCode] = FirebaseAuthException.errorCode (yoksa null).
     * DİKKAT (Android davranışı): mesajlar geliştiriciye yönelik ayrıntı (SHA-1 parmak izi,
     * Logcat yönlendirmesi) içerir ve `errorCode` null ise metinde "null" yazar; aynen korunmuştur.
     */
    fun googleFirebaseAuthErrorMessage(errorCode: String?, errorMessageFull: String): String = when {
        errorCode == "ERROR_NETWORK_REQUEST_FAILED" ||
            errorMessageFull.contains("network", ignoreCase = true) ->
            "Ağ bağlantı hatası. İnternet bağlantınızı kontrol edin."

        errorCode == "ERROR_INVALID_CREDENTIAL" ||
            errorMessageFull.contains("invalid", ignoreCase = true) ->
            "Geçersiz kimlik bilgisi. Web Client ID doğru mu kontrol edin."

        errorCode == "ERROR_OPERATION_NOT_ALLOWED" ||
            errorMessageFull.contains("permission", ignoreCase = true) ||
            errorMessageFull.contains("PERMISSION_DENIED", ignoreCase = true) ||
            errorMessageFull.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ->
            "İzin hatası (Error Code: $errorCode)\n" +
                "\n" +
                "Kontrol edin:\n" +
                "1. Firebase Console > Authentication > Sign-in method > Google ENABLED mi?\n" +
                "2. Google Cloud Console > OAuth consent screen yapılandırıldı mı?\n" +
                "3. Test users eklendi mi? (OAuth consent screen'de)\n" +
                "4. SHA-1 fingerprint eklendi mi? (71:43:C7:24:86:97:58:FA:59:EB:3D:F2:BC:E0:77:77:43:5D:B4:4E)\n" +
                "5. Identity Toolkit API etkin mi?\n" +
                "\n" +
                "Logcat'te detaylı hata: $errorMessageFull"

        errorCode == "ERROR_DEVELOPER_ERROR" ||
            errorMessageFull.contains("DEVELOPER_ERROR", ignoreCase = true) ->
            "Yapılandırma hatası:\n\n1. Web Client ID doğru mu? (strings.xml)\n2. SHA-1 fingerprint Firebase Console'a eklendi mi?\n3. Google Sign-In Firebase Console'da etkin mi?\n\nHata: $errorMessageFull"

        else ->
            "Giriş başarısız (Code: $errorCode)\n\n$errorMessageFull\n\nLogcat'te detaylı bilgi için 'AuthManager' tag'ini kontrol edin."
    }

    /** Google Sign-In ApiException durum kodu: kullanıcı iptali. */
    const val GOOGLE_STATUS_CANCELLED = 12501

    /**
     * Google hesap seçimi hatası (handleGoogleSignInResult). [detail] = `localizedMessage ?: message`.
     */
    fun googleApiExceptionMessage(statusCode: Int, detail: String?): String {
        if (statusCode == 12501) return MESSAGE_GOOGLE_CANCELLED
        return when (statusCode) {
            10 -> "Yapılandırma hatası: Web Client ID eksik veya yanlış. Firebase Console'dan Web Client ID'yi alıp strings.xml'e ekleyin."
            7 -> "Ağ bağlantı hatası. İnternet bağlantınızı kontrol edin."
            8 -> "İstek limiti aşıldı. Lütfen daha sonra tekrar deneyin."
            else -> "Google girişi başarısız (Hata: ${statusCode}): $detail"
        }
    }

    /** Giriş ekranında Google sonucu (LoginActivity). */
    enum class GoogleLoginFailureAction {
        /** Kayıtlı değil: kullanıcı bilgisi (kayıt) ekranı açılır. */
        SHOW_USER_INFO,
        /** Kullanıcı iptal etti: sessiz kalınır. */
        SILENT,
        /** Hata mesajı gösterilir (mesaj null ise "Google girişi başarısız"). */
        SHOW_ERROR,
    }

    fun googleLoginFailureAction(error: String?): GoogleLoginFailureAction = when {
        error == ERROR_ACCOUNT_NOT_REGISTERED -> GoogleLoginFailureAction.SHOW_USER_INFO
        error != MESSAGE_GOOGLE_CANCELLED -> GoogleLoginFailureAction.SHOW_ERROR
        else -> GoogleLoginFailureAction.SILENT
    }

    /**
     * E-posta/parola ile öğrenci kaydı hatası (registerStudent). [exceptionMessage] = `e.message`,
     * [localizedMessage] = `e.localizedMessage`.
     */
    fun registerStudentErrorMessage(exceptionMessage: String?, localizedMessage: String?): String = when {
        exceptionMessage?.contains("email address is already in use", ignoreCase = true) == true ->
            "Bu e-posta adresi zaten kayıtlı. Lütfen giriş yapın."
        exceptionMessage?.contains("weak password", ignoreCase = true) == true ->
            "Şifre çok zayıf. Daha güçlü bir şifre seçin."
        exceptionMessage?.contains("invalid email", ignoreCase = true) == true ->
            "Geçersiz e-posta adresi."
        else -> localizedMessage ?: "Kayıt başarısız. Lütfen tekrar deneyin."
    }
}

/** OtpVerificationFragment'in saf kuralları (kod biçimi, yeniden gönderme / yanlış deneme beklemeleri). */
object OtpRules {
    const val CODE_LENGTH = 6
    const val RESEND_COOLDOWN_MS = 45_000L // 45 saniye
    const val MAX_WRONG_ATTEMPTS = 5
    const val WRONG_ATTEMPT_COOLDOWN_MS = 15 * 60 * 1000L // 15 dk

    const val OTP_COOLDOWN_PREFS = "otp_cooldown_prefs"
    const val OTP_COOLDOWN_END_MS = "otp_wrong_cooldown_end_ms"
    const val OTP_COOLDOWN_EMAIL = "otp_wrong_cooldown_email"
    const val OTP_RESEND_COOLDOWN_END_MS = "otp_resend_cooldown_end_ms"
    const val OTP_RESEND_COOLDOWN_EMAIL = "otp_resend_cooldown_email"

    const val MESSAGE_CODE_LENGTH = "Kod 6 haneli olmalıdır"
    const val MESSAGE_TOO_MANY_WRONG = "Çok fazla yanlış deneme. 15 dakika sonra tekrar deneyebilirsiniz."

    /** R.string.otp_wrong_cooldown_try_later */
    fun wrongCooldownTryLaterMessage(remainingMin: Int): String =
        "Çok fazla yanlış deneme. $remainingMin dakika sonra tekrar deneyin."

    /** R.string.otp_resend_cooldown_try_again */
    fun resendCooldownTryAgainLabel(sec: Int): String = "$sec sn sonra tekrar"

    /** Kod `trim()` edilir ve tam 6 karakter olmalıdır (rakam denetimi yoktur). */
    fun isValidCodeLength(rawCode: String?): Boolean = rawCode?.trim().orEmpty().length == 6

    /**
     * 45 sn "yeniden gönder" beklemesinden kalan süre (ms). Kayıt yoksa, e-posta farklıysa ya da
     * süre dolduysa 0.
     */
    fun getResendCooldownRemainingMs(endMs: Long, savedEmail: String?, forEmail: String, nowMs: Long): Long {
        if (savedEmail == null) return 0L
        if (savedEmail != AuthRules.normalizeEmail(forEmail)) return 0L
        if (endMs <= nowMs) return 0L
        return (endMs - nowMs).coerceAtLeast(0L)
    }

    /** Kod başarıyla gönderilince yazılacak (bitiş zamanı, e-posta). */
    fun startResendCooldown(email: String, nowMs: Long): Pair<Long, String> =
        Pair(nowMs + RESEND_COOLDOWN_MS, AuthRules.normalizeEmail(email))

    /**
     * Yanlış deneme kilidinden kalan DAKİKA (en az 1); kilit yoksa / e-posta farklıysa null.
     */
    fun getActiveWrongAttemptCooldown(endMs: Long, savedEmail: String?, forEmail: String, nowMs: Long): Int? {
        if (savedEmail == null) return null
        if (endMs <= nowMs) return null
        if (savedEmail != AuthRules.normalizeEmail(forEmail)) return null
        return ((endMs - nowMs) / 60_000).toInt().coerceAtLeast(1)
    }

    /** Yanlış deneme kilidi başlarken yazılacak bitiş zamanı. */
    fun wrongAttemptCooldownEndMs(nowMs: Long): Long = nowMs + WRONG_ATTEMPT_COOLDOWN_MS

    /** Doğrulama başarısız olunca: yeni deneme sayısı ve kilit başlamalı mı. */
    data class WrongAttemptResult(val wrongAttempts: Int, val verifyEnabled: Boolean, val startCooldown: Boolean)

    /**
     * Android: verify() içindeki onVerifyFailed. Sunucu hata metni "15 dakika" ya da
     * "Çok fazla yanlış" içeriyorsa deneme sayısı doğrudan en yükseğe çekilir ve kilit başlar.
     */
    fun onVerifyFailed(currentWrongAttempts: Int, error: String?): WrongAttemptResult {
        var wrongAttempts = currentWrongAttempts + 1
        val verifyEnabled = wrongAttempts < MAX_WRONG_ATTEMPTS
        var startCooldown = wrongAttempts >= MAX_WRONG_ATTEMPTS
        if (error != null && (error.contains("15 dakika") || error.contains("Çok fazla yanlış"))) {
            wrongAttempts = MAX_WRONG_ATTEMPTS
            startCooldown = true
        }
        return WrongAttemptResult(wrongAttempts, verifyEnabled, startCooldown)
    }
}

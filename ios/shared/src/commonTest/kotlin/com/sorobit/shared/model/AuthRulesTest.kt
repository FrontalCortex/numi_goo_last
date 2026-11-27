// Kimlik doğrulama kurallarının (AuthManager, RegisterActivity, LoginActivity,
// EditProfileFragment, OtpVerificationFragment) Android ile aynı sonucu verdiğini doğrular.
// Beklenen değerler Android kaynağı okunarak elle türetilmiştir.
package com.sorobit.shared.model

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRulesTest {
    private val a = AuthRules

    @Test
    fun epostaNormallestirme() {
        assertEquals("foo@bar.com", a.normalizeEmail("  Foo@BAR.com "))
        // Yerelden bağımsız (Locale.ROOT): "I" → "i" (Türkçe "ı" DEĞİL)
        assertEquals("isik@x.com", a.normalizeEmail("ISIK@X.COM"))
        // "İ" (U+0130) → "i" + birleşen nokta (U+0307)
        assertEquals("i̇@x.com", a.normalizeEmail("İ@X.COM"))
        assertEquals("ı@x.com", a.normalizeEmail("ı@x.com")) // noktasız ı aynen kalır
        assertEquals("", a.normalizeEmail("   "))
    }

    @Test
    fun epostaBicimi() {
        for (iyi in listOf("a@b.co", "a@b.c", "a+b.c_d%e-f@x-y.z9", "A1@B2.C3", "a@b.c.d.e", "1@2.3")) {
            assertTrue(a.isValidEmail(iyi), iyi)
        }
        for (kotu in listOf(
            "", "a@b", "a b@c.com", "a@-b.com", "@b.com", "a@b..com", "ç@b.com", "a@b.com ", " a@b.com",
            "a@b.", "a@.com", "a@@b.com", "a@b.-c", "a!b@c.com", "a@b_c.com",
        )) {
            assertFalse(a.isValidEmail(kotu), "'$kotu'")
        }
        // Uzunluk sınırları: yerel kısım 256, alan etiketi 65, sonraki etiketler 26
        assertTrue(a.isValidEmail("a".repeat(256) + "@b.co"))
        assertFalse(a.isValidEmail("a".repeat(257) + "@b.co"))
        assertTrue(a.isValidEmail("a@" + "b".repeat(65) + ".co"))
        assertFalse(a.isValidEmail("a@" + "b".repeat(66) + ".co"))
        assertTrue(a.isValidEmail("a@b." + "c".repeat(26)))
        assertFalse(a.isValidEmail("a@b." + "c".repeat(27)))
    }

    @Test
    fun epostaAdimiDogrulama() {
        assertEquals("Lütfen e-posta girin", a.validateRegisterEmail(null))
        assertEquals("Lütfen e-posta girin", a.validateRegisterEmail("   "))
        assertEquals("Geçerli bir e-posta adresi giriniz.", a.validateRegisterEmail("x"))
        assertNull(a.validateRegisterEmail(" a@b.co "))
        // Giriş ekranındaki metin farklı
        assertEquals("Lütfen e-posta girin", a.validateLoginEmail(""))
        assertEquals("Geçerli bir e-posta adresi girin", a.validateLoginEmail("x@y"))
        assertNull(a.validateLoginEmail("a@b.co"))
    }

    @Test
    fun ogretmenKaydiDogrulama() {
        assertEquals("Lütfen tüm alanları doldurun", a.validateTeacherRegistration("", "a@b.co", "123456", "123456"))
        assertEquals("Lütfen tüm alanları doldurun", a.validateTeacherRegistration("  ", "a@b.co", "123456", "123456"))
        assertEquals("Lütfen tüm alanları doldurun", a.validateTeacherRegistration("Ali", " ", "123456", "123456"))
        assertEquals("Lütfen tüm alanları doldurun", a.validateTeacherRegistration("Ali", "a@b.co", "", "123456"))
        assertEquals("Lütfen tüm alanları doldurun", a.validateTeacherRegistration("Ali", "a@b.co", "123456", ""))
        assertEquals("Geçerli bir e-posta adresi giriniz.", a.validateTeacherRegistration("Ali", "a@b", "123456", "123456"))
        assertEquals("Şifre en az 6 karakter olmalı", a.validateTeacherRegistration("Ali", "a@b.co", "12345", "12345"))
        // Kısa şifre denetimi eşleşme denetiminden önce gelir
        assertEquals("Şifre en az 6 karakter olmalı", a.validateTeacherRegistration("Ali", "a@b.co", "12345", "x"))
        assertEquals("Şifreler eşleşmiyor", a.validateTeacherRegistration("Ali", "a@b.co", "123456", "123457"))
        assertNull(a.validateTeacherRegistration(" Ali ", " a@b.co ", "123456", "123456"))
        // Parola kırpılmaz: boşluk da karakterdir
        assertNull(a.validateTeacherRegistration("Ali", "a@b.co", "      ", "      "))

        assertEquals(
            "Bu e-posta zaten bir öğretmen hesabı ile kayıtlı. Lütfen öğretmen giriş ekranını kullanın.",
            a.teacherEmailAlreadyRegisteredMessage("TEACHER"),
        )
        assertEquals("Bu e-posta başka bir hesapta kullanılıyor.", a.teacherEmailAlreadyRegisteredMessage("STUDENT"))
        assertEquals("Bu e-posta başka bir hesapta kullanılıyor.", a.teacherEmailAlreadyRegisteredMessage(null))
    }

    @Test
    fun ogrenciEpostaAkisi() {
        assertEquals(AuthRules.StudentEmailFlow.REJECT_TEACHER, a.studentEmailFlow(true, "uid", "TEACHER"))
        assertEquals(AuthRules.StudentEmailFlow.SEND_LOGIN_CODE, a.studentEmailFlow(true, "uid", "STUDENT"))
        assertEquals(AuthRules.StudentEmailFlow.SEND_LOGIN_CODE, a.studentEmailFlow(true, "uid", null))
        assertEquals(AuthRules.StudentEmailFlow.ERROR_CODE_NOT_SENT, a.studentEmailFlow(true, "", "STUDENT"))
        assertEquals(AuthRules.StudentEmailFlow.ERROR_CODE_NOT_SENT, a.studentEmailFlow(true, null, "TEACHER"))
        assertEquals(AuthRules.StudentEmailFlow.NEW_REGISTRATION, a.studentEmailFlow(false, null, null))
        assertEquals(AuthRules.StudentEmailFlow.NEW_REGISTRATION, a.studentEmailFlow(false, "uid", "TEACHER"))
    }

    @Test
    fun adVeParola() {
        assertEquals("Ad boş bırakılamaz.", a.validateName(""))
        assertNull(a.validateName("A"))
        assertNull(a.validateName("a".repeat(25)))
        assertEquals("Ad en fazla 25 karakter olabilir.", a.validateName("a".repeat(26)))
        assertEquals("Parola en az 6 karakter olmalıdır.", a.validateNewPassword("12345"))
        assertEquals("Parola en az 6 karakter olmalıdır.", a.validateNewPassword(""))
        assertNull(a.validateNewPassword("123456"))
    }

    @Test
    fun bekleyenKayit() {
        val depo = AuthRules.PendingRegistrationStore()
        depo.rememberPendingRegistration(" A@B.com ", "Ali", "sifre1", "TEACHER")
        assertEquals(AuthRules.PendingRegistration("Ali", "sifre1", "TEACHER"), depo.consumePendingRegistration("a@b.COM"))
        assertNull(depo.consumePendingRegistration("a@b.com")) // tek kullanımlık

        assertEquals("Geçersiz bilgiler", depo.createPendingRegistrationForOTP("   "))
        assertNull(depo.createPendingRegistrationForOTP(" X@Y.co "))
        assertEquals(AuthRules.PendingRegistration("", "", "STUDENT"), depo.consumePendingRegistration("x@y.co"))

        assertEquals("Geçersiz bilgiler", depo.createPendingTeacherRegistrationForOTP("a@b.co", "Ali", "12345"))
        assertEquals("Geçersiz bilgiler", depo.createPendingTeacherRegistrationForOTP("a@b.co", "  ", "123456"))
        assertEquals("Geçersiz bilgiler", depo.createPendingTeacherRegistrationForOTP(" ", "Ali", "123456"))
        assertNull(depo.consumePendingRegistration("a@b.co"))
        assertNull(depo.createPendingTeacherRegistrationForOTP("A@B.co", "  Ali ", "123456"))
        assertEquals(AuthRules.PendingRegistration("Ali", "123456", "TEACHER"), depo.consumePendingRegistration("a@b.co"))
    }

    @Test
    fun islevGovdeleri() {
        assertEquals(mapOf<String, Any>("email" to "a@b.co", "purpose" to "login"), a.sendVerificationCodePayload("a@b.co", a.OTP_PURPOSE_LOGIN))
        assertEquals("register", a.resendPurpose(true))
        assertEquals("login", a.resendPurpose(false))
        assertEquals("teacher_reset", a.OTP_PURPOSE_TEACHER_RESET)
        assertEquals(mapOf<String, Any>("email" to "a@b.co"), a.checkEmailRegisteredPayload(" A@B.co "))

        // Kayıt doğrulama: bekleyen kayıt yok
        assertEquals(mapOf<String, Any>("email" to "a@b.co", "code" to "123456"), a.verifyRegistrationCodePayload(" A@B.co ", "123456", null))
        // Öğrenci: ad ve şifre boş → yalnızca rol eklenir
        assertEquals(
            mapOf<String, Any>("email" to "a@b.co", "code" to "1", "role" to "STUDENT", "birthYear" to 2015, "acquisitionSource" to "tiktok"),
            a.verifyRegistrationCodePayload("a@b.co", "1", AuthRules.PendingRegistration("", "", "STUDENT"), 2015, "tiktok"),
        )
        // Öğretmen: ad + şifre + rol
        assertEquals(
            mapOf<String, Any>("email" to "a@b.co", "code" to "1", "name" to "Ali", "password" to "123456", "role" to "TEACHER"),
            a.verifyRegistrationCodePayload("a@b.co", "1", AuthRules.PendingRegistration("Ali", "123456", "TEACHER")),
        )
        // Giriş kodu doğrulamada e-posta normalleştirilmez (Android davranışı)
        assertEquals(mapOf<String, Any>("email" to " A@B.co ", "code" to "9"), a.verifyLoginCodePayload(" A@B.co ", "9"))
        assertEquals(mapOf<String, Any>("email" to "a@b.co", "code" to "9"), a.verifyTeacherPasswordResetCodePayload(" A@B.co ", "9"))
        assertEquals(
            mapOf<String, Any>("email" to "a@b.co", "code" to "9", "newPassword" to "yeni123"),
            a.resetTeacherPasswordPayload("A@B.co", "9", "yeni123"),
        )
        assertNull(a.findTeacherEmailByUserIdPayload("   "))
        assertEquals(mapOf<String, Any>("userId" to "123AB"), a.findTeacherEmailByUserIdPayload(" 123AB "))

        assertNull(a.parseToken(null))
        assertNull(a.parseToken(mapOf("token" to " ")))
        assertNull(a.parseToken(mapOf("token" to 5)))
        assertEquals("t", a.parseToken(mapOf("token" to "t")))
        assertEquals(Triple(false, null, null), a.parseEmailRegisteredResult(null))
        assertEquals(Triple(true, "u", "STUDENT"), a.parseEmailRegisteredResult(mapOf("registered" to true, "uid" to "u", "role" to "STUDENT")))
        assertEquals(Triple(false, "u", null), a.parseEmailRegisteredResult(mapOf("registered" to "true", "uid" to "u")))

        assertEquals("verifyRegistrationCode", a.FN_VERIFY_REGISTRATION_CODE)
        assertEquals("sendStudentVerificationCode", a.FN_SEND_STUDENT_VERIFICATION_CODE)
        assertEquals("checkEmailRegistered", a.FN_CHECK_EMAIL_REGISTERED)
    }

    @Test
    fun kullaniciKimligiUretimi() {
        val bicim = Regex("[1-9][0-9]{5}[A-Z]{2}")
        for (tohum in 0..200) {
            val kimlik = a.generateUserIdCandidate(Random(tohum))
            assertTrue(bicim.matches(kimlik), kimlik)
        }
        // Yedek: zaman damgası mod 1.000.000 (baştaki sıfırlar atılır) + 2 harf
        val yedek = a.generateFallbackUserId(1_700_000_012_345L, Random(1))
        assertTrue(Regex("12345[A-Z]{2}").matches(yedek), yedek)
        assertTrue(Regex("0[A-Z]{2}").matches(a.generateFallbackUserId(5_000_000L, Random(2))))
        assertEquals(15, a.USER_ID_MAX_ATTEMPTS)
    }

    @Test
    fun kullaniciDokumani() {
        val cuzdan = mapOf<String, Any>("currency" to 0)
        assertEquals(
            mapOf<String, Any>(
                "uid" to "u", "userId" to "123AB", "email" to "", "name" to "", "role" to "STUDENT", "verified" to true,
                "first_tutorial_shown" to true, "createdAt" to 99L, "currency" to 0,
            ),
            a.googleStudentUserData("u", "123AB", null, null, true, 99L, walletFields = cuzdan),
        )
        val g = a.googleStudentUserData("u", "123AB", "a@b.co", "Ali", false, 99L, birthYear = 2014, acquisitionSource = "instagram")
        assertEquals(2014, g["birthYear"])
        assertEquals("instagram", g["acquisitionSource"])
        assertEquals("Ali", g["name"])
        assertEquals(10, g.size)

        assertEquals(
            mapOf<String, Any>(
                "uid" to "u", "userId" to "123AB", "email" to "a@b.co", "name" to "Ali", "role" to "STUDENT",
                "verified" to false, "first_tutorial_shown" to false, "createdAt" to 99L,
            ),
            a.emailStudentUserData("u", "123AB", "a@b.co", "Ali", false, 99L),
        )
        // Öğretmen dokümanında `verified` alanı yok
        assertEquals(
            mapOf<String, Any>(
                "uid" to "u", "userId" to "123AB", "email" to "a@b.co", "name" to "Ali", "role" to "TEACHER",
                "first_tutorial_shown" to false, "createdAt" to 99L,
            ),
            a.teacherUserData("u", "123AB", "a@b.co", "Ali", false, 99L),
        )
        assertEquals(
            mapOf("user_email" to "a@b.co", "user_type" to "STUDENT", "user_name" to "Ali", "user_id" to ""),
            a.cacheBasicUser("a@b.co", "STUDENT", "Ali"),
        )
        assertEquals("auth_prefs", a.PREFS_NAME)
    }

    @Test
    fun davetKoduVeRolKurallari() {
        assertFalse(a.isTeacherInviteValid(exists = false, used = false, expiresAtMs = null, nowMs = 100))
        assertFalse(a.isTeacherInviteValid(true, null, null, 100)) // `used` yoksa kullanılmış sayılır
        assertFalse(a.isTeacherInviteValid(true, true, null, 100))
        assertTrue(a.isTeacherInviteValid(true, false, null, 100)) // süresiz
        assertFalse(a.isTeacherInviteValid(true, false, 99, 100))
        assertTrue(a.isTeacherInviteValid(true, false, 100, 100)) // eşitse geçerli
        assertTrue(a.isTeacherInviteValid(true, false, 101, 100))
        assertEquals(mapOf<String, Any>("used" to true, "usedByUid" to "u", "usedAt" to 5L), a.teacherInviteUsedUpdate("u", 5L))

        assertTrue(a.isOtpLoginAllowed(null)); assertTrue(a.isOtpLoginAllowed("STUDENT"))
        assertFalse(a.isOtpLoginAllowed("TEACHER")); assertFalse(a.isOtpLoginAllowed(""))
        assertTrue(a.isGoogleLoginAllowed(null)); assertTrue(a.isGoogleLoginAllowed("STUDENT")); assertTrue(a.isGoogleLoginAllowed(""))
        assertFalse(a.isGoogleLoginAllowed("TEACHER"))

        assertFalse(a.skipStepTutorialShown(false, null))
        assertFalse(a.skipStepTutorialShown(true, emptyMap()))
        assertTrue(a.skipStepTutorialShown(true, mapOf("skip_step_tutorial_shown" to true)))
        assertFalse(a.skipStepTutorialShown(true, mapOf("skip_step_tutorial_shown" to false)))
        assertFalse(a.skipStepTutorialShown(true, mapOf("skip_step_tutorial_shown" to "evet")))
        assertFalse(a.skipStepTutorialShown(false, mapOf("skip_step_tutorial_shown" to true)))
    }

    @Test
    fun googleHataMesajlari() {
        val ag = "Ağ bağlantı hatası. İnternet bağlantınızı kontrol edin."
        assertEquals(ag, a.googleFirebaseAuthErrorMessage("ERROR_NETWORK_REQUEST_FAILED", ""))
        assertEquals(ag, a.googleFirebaseAuthErrorMessage(null, "A NETWORK error occurred"))
        // Öncelik: ağ, geçersiz kimlikten önce gelir
        assertEquals(ag, a.googleFirebaseAuthErrorMessage("ERROR_INVALID_CREDENTIAL", "network"))
        val gecersiz = "Geçersiz kimlik bilgisi. Web Client ID doğru mu kontrol edin."
        assertEquals(gecersiz, a.googleFirebaseAuthErrorMessage("ERROR_INVALID_CREDENTIAL", ""))
        assertEquals(gecersiz, a.googleFirebaseAuthErrorMessage(null, "Invalid token"))

        val izin = a.googleFirebaseAuthErrorMessage(null, "PERMISSION_DENIED: x")
        assertEquals(
            "İzin hatası (Error Code: null)\n\nKontrol edin:\n" +
                "1. Firebase Console > Authentication > Sign-in method > Google ENABLED mi?\n" +
                "2. Google Cloud Console > OAuth consent screen yapılandırıldı mı?\n" +
                "3. Test users eklendi mi? (OAuth consent screen'de)\n" +
                "4. SHA-1 fingerprint eklendi mi? (71:43:C7:24:86:97:58:FA:59:EB:3D:F2:BC:E0:77:77:43:5D:B4:4E)\n" +
                "5. Identity Toolkit API etkin mi?\n\n" +
                "Logcat'te detaylı hata: PERMISSION_DENIED: x",
            izin,
        )
        assertTrue(a.googleFirebaseAuthErrorMessage("ERROR_OPERATION_NOT_ALLOWED", "").startsWith("İzin hatası (Error Code: ERROR_OPERATION_NOT_ALLOWED)"))
        assertEquals(
            "Yapılandırma hatası:\n\n1. Web Client ID doğru mu? (strings.xml)\n2. SHA-1 fingerprint Firebase Console'a eklendi mi?\n3. Google Sign-In Firebase Console'da etkin mi?\n\nHata: 10: DEVELOPER_ERROR",
            a.googleFirebaseAuthErrorMessage(null, "10: DEVELOPER_ERROR"),
        )
        assertEquals(
            "Giriş başarısız (Code: ERROR_X)\n\nbir şey\n\nLogcat'te detaylı bilgi için 'AuthManager' tag'ini kontrol edin.",
            a.googleFirebaseAuthErrorMessage("ERROR_X", "bir şey"),
        )

        assertEquals("Kullanıcı girişi iptal edildi", a.googleApiExceptionMessage(12501, "x"))
        assertTrue(a.googleApiExceptionMessage(10, null).startsWith("Yapılandırma hatası: Web Client ID eksik veya yanlış."))
        assertEquals(ag, a.googleApiExceptionMessage(7, null))
        assertEquals("İstek limiti aşıldı. Lütfen daha sonra tekrar deneyin.", a.googleApiExceptionMessage(8, null))
        assertEquals("Google girişi başarısız (Hata: 99): ayrıntı", a.googleApiExceptionMessage(99, "ayrıntı"))
        assertEquals("Google girişi başarısız (Hata: 99): null", a.googleApiExceptionMessage(99, null))

        assertEquals(AuthRules.GoogleLoginFailureAction.SHOW_USER_INFO, a.googleLoginFailureAction("ACCOUNT_NOT_REGISTERED"))
        assertEquals(AuthRules.GoogleLoginFailureAction.SILENT, a.googleLoginFailureAction("Kullanıcı girişi iptal edildi"))
        assertEquals(AuthRules.GoogleLoginFailureAction.SHOW_ERROR, a.googleLoginFailureAction(null))
        assertEquals(AuthRules.GoogleLoginFailureAction.SHOW_ERROR, a.googleLoginFailureAction("başka"))
    }

    @Test
    fun kayitHataMesajlari() {
        assertEquals(
            "Bu e-posta adresi zaten kayıtlı. Lütfen giriş yapın.",
            a.registerStudentErrorMessage("The EMAIL address is already in use by another account.", "yerel"),
        )
        assertEquals("Şifre çok zayıf. Daha güçlü bir şifre seçin.", a.registerStudentErrorMessage("Weak password", null))
        assertEquals("Geçersiz e-posta adresi.", a.registerStudentErrorMessage("invalid email format", null))
        assertEquals("yerel", a.registerStudentErrorMessage("başka", "yerel"))
        assertEquals("Kayıt başarısız. Lütfen tekrar deneyin.", a.registerStudentErrorMessage(null, null))
    }
}

class OtpRulesTest {
    private val o = OtpRules

    @Test
    fun kodUzunlugu() {
        assertTrue(o.isValidCodeLength(" 123456 "))
        assertTrue(o.isValidCodeLength("abcdef")) // rakam denetimi yok
        assertFalse(o.isValidCodeLength("12345"))
        assertFalse(o.isValidCodeLength("1234567"))
        assertFalse(o.isValidCodeLength(null))
        assertEquals("Kod 6 haneli olmalıdır", o.MESSAGE_CODE_LENGTH)
    }

    @Test
    fun yenidenGondermeBeklemesi() {
        val simdi = 1_000_000L
        assertEquals(10_000L, o.getResendCooldownRemainingMs(simdi + 10_000, "a@b.com", " A@B.com ", simdi))
        assertEquals(0L, o.getResendCooldownRemainingMs(simdi + 10_000, "a@b.com", "x@b.com", simdi))
        assertEquals(0L, o.getResendCooldownRemainingMs(simdi, "a@b.com", "a@b.com", simdi)) // tam bitiş anı
        assertEquals(0L, o.getResendCooldownRemainingMs(simdi - 1, "a@b.com", "a@b.com", simdi))
        assertEquals(0L, o.getResendCooldownRemainingMs(simdi + 10_000, null, "a@b.com", simdi))
        assertEquals(Pair(simdi + 45_000L, "a@b.com"), o.startResendCooldown(" A@B.com ", simdi))
        assertEquals("12 sn sonra tekrar", o.resendCooldownTryAgainLabel(12))
    }

    @Test
    fun yanlisDenemeKilidi() {
        val simdi = 1_000_000L
        assertEquals(1, o.getActiveWrongAttemptCooldown(simdi + 59_999, "a@b.com", "A@B.com", simdi)) // en az 1 dk
        assertEquals(15, o.getActiveWrongAttemptCooldown(simdi + 900_000, "a@b.com", "a@b.com", simdi))
        assertEquals(14, o.getActiveWrongAttemptCooldown(simdi + 899_999, "a@b.com", "a@b.com", simdi))
        assertNull(o.getActiveWrongAttemptCooldown(simdi, "a@b.com", "a@b.com", simdi))
        assertNull(o.getActiveWrongAttemptCooldown(simdi + 900_000, "a@b.com", "x@b.com", simdi))
        assertNull(o.getActiveWrongAttemptCooldown(simdi + 900_000, null, "a@b.com", simdi))
        assertEquals(simdi + 900_000L, o.wrongAttemptCooldownEndMs(simdi))
        assertEquals("Çok fazla yanlış deneme. 3 dakika sonra tekrar deneyin.", o.wrongCooldownTryLaterMessage(3))

        assertEquals(OtpRules.WrongAttemptResult(1, verifyEnabled = true, startCooldown = false), o.onVerifyFailed(0, null))
        assertEquals(OtpRules.WrongAttemptResult(4, true, false), o.onVerifyFailed(3, "Kod hatalı"))
        assertEquals(OtpRules.WrongAttemptResult(5, false, true), o.onVerifyFailed(4, null))
        // Sunucu kilit bildirdiyse sayaç doğrudan en yükseğe çıkar
        assertEquals(OtpRules.WrongAttemptResult(5, true, true), o.onVerifyFailed(1, "Çok fazla yanlış deneme. 15 dakika sonra tekrar deneyin."))
        assertEquals(OtpRules.WrongAttemptResult(5, true, true), o.onVerifyFailed(0, "15 dakika bekleyin"))
    }

    @Test
    fun veliOnayiKurallari() {
        val r = AuthRules
        // 13 yaş altı: bu yıl − doğum yılı <= 13 (UTC yılı)
        assertTrue(r.requiresParentalConsent(2013, 2026))
        assertFalse(r.requiresParentalConsent(2012, 2026))
        assertTrue(r.requiresParentalConsent(2020, 2026))
        assertFalse(r.requiresParentalConsent(null, 2026))
        assertFalse(r.requiresParentalConsent(0, 2026))
        assertEquals("register_parental", r.OTP_PURPOSE_REGISTER_PARENTAL)
        assertEquals("confirmParentalConsent", r.FN_CONFIRM_PARENTAL_CONSENT)
        assertEquals("PARENTAL_CONSENT_REQUIRED", r.ERROR_PARENTAL_CONSENT_REQUIRED)
        assertEquals(
            mapOf("email" to "c@x.co", "purpose" to "register_parental", "lang" to "tr", "parentEmail" to "v@x.co", "birthYear" to 2015, "acquisitionSource" to "TikTok"),
            r.sendParentalRegistrationCodePayload(" C@X.co ", "V@x.co", 2015, "TikTok", "tr"),
        )
        assertFalse(r.sendParentalRegistrationCodePayload("c@x.co", "v@x.co", 2015, null, "tr").containsKey("acquisitionSource"))
        assertEquals(true, r.requestGoogleParentalConsentPayload("c@x.co", "v@x.co", 2015, null, "en")["google"])
        assertEquals(
            mapOf("email" to "c@x.co", "code" to "123456", "birthYear" to 2015, "parentalStep" to true),
            r.verifyChildEmailForParentalConsentPayload("c@x.co", "123456", 2015),
        )
        assertEquals(mapOf("code" to "1", "firstTutorialShown" to false), r.confirmParentalConsentPayload("1", false))
    }
}

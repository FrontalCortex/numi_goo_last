// Açılış yönlendirmesi, yaş adımı ve öğretmen girişi ön denetimlerinin (SplashActivity,
// UserInfoFragment, TeacherLoginActivity, TeacherForgotPasswordEmailFragment,
// TeacherNewPasswordFragment) Android ile aynı sonucu verdiğini doğrular.
// Beklenen değerler Android kaynağı okunarak elle türetilmiştir.
package com.sorobit.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginFlowRulesTest {
    @Test
    fun acilisYonlendirmesi() {
        val r = SplashRules
        // İnternet yoksa diğer koşullara bakılmaz.
        assertEquals(SplashRules.Route.OFFLINE_MAIN, r.route(isOnline = false, loginStartEverShown = true, hasExistingLogin = true))
        assertEquals(SplashRules.Route.OFFLINE_MAIN, r.route(isOnline = false, loginStartEverShown = false, hasExistingLogin = false))
        assertEquals(SplashRules.Route.LOGIN_START, r.route(isOnline = true, loginStartEverShown = true, hasExistingLogin = false))
        assertEquals(SplashRules.Route.CHECK_SINGLE_DEVICE, r.route(isOnline = true, loginStartEverShown = true, hasExistingLogin = true))
        assertEquals(SplashRules.Route.CHECK_SINGLE_DEVICE, r.route(isOnline = true, loginStartEverShown = false, hasExistingLogin = true))
        assertEquals(SplashRules.Route.GUEST_MAIN, r.route(isOnline = true, loginStartEverShown = false, hasExistingLogin = false))
        assertEquals(1200L, r.SPLASH_ANIMATION_MS) // güncel Android (eskiden 2000; Remote Config beklemesi kalktı)
        assertEquals(450L, r.BRAND_REVEAL_MS)
        assertEquals(1000L, r.FORCE_UPDATE_MAX_WAIT_AFTER_SPLASH_MS)
        assertEquals(1500L, r.FORCE_UPDATE_MAX_WAIT_DEEP_LINK_MS)
        assertEquals("login_start_ever_shown", r.PREF_LOGIN_START_EVER_SHOWN)
    }

    @Test
    fun yasDogrulamasi() {
        val u = UserInfoRules
        assertEquals(1, u.parseAge("1"))
        assertEquals(120, u.parseAge("120"))
        assertEquals(9, u.parseAge("  9 "))
        assertEquals(7, u.parseAge("007"))
        assertNull(u.parseAge("0"))
        assertNull(u.parseAge("121"))
        assertNull(u.parseAge("-5"))
        assertNull(u.parseAge(""))
        assertNull(u.parseAge(null))
        assertNull(u.parseAge("on"))
        assertNull(u.parseAge("9.5"))
        assertTrue(u.isValidAge("10"))
        assertFalse(u.isValidAge("abc"))
    }

    @Test
    fun dogumYili() {
        val u = UserInfoRules
        assertEquals(2016, u.birthYearFromAge(10, 2026))
        assertEquals(2016, u.birthYearFromRawAge(" 10 ", 2026))
        assertEquals(-1, u.birthYearFromRawAge("0", 2026))
        assertEquals(-1, u.birthYearFromRawAge(null, 2026))
    }

    @Test
    fun kaynakListesi() {
        assertEquals(
            listOf(
                "Facebook", "Instagram", "Youtube", "Google Araması", "Arkadaş/Aile", "Okul/Öğretmen", "Veli/WhatsApp",
                "TikTok", "Uygulama Mağazası", "Diğer",
            ),
            UserInfoRules.SOURCE_NAMES,
        )
        assertEquals(UserInfoRules.SOURCE_NAMES.size, UserInfoRules.SOURCE_DISPLAY_NAMES_TR.size)
        assertEquals("YouTube", UserInfoRules.SOURCE_DISPLAY_NAMES_TR[2]) // değer "Youtube", görünen ad "YouTube"
    }

    @Test
    fun hesapGecmisiBayragi() {
        val s = SplashRules
        assertFalse(s.hasEverHadAccount(false, false, false))
        assertTrue(s.showsFirstTutorial(false))
        assertTrue(s.hasEverHadAccount(true, false, false))
        assertTrue(s.hasEverHadAccount(false, true, false)) // oturum açıksa
        assertTrue(s.hasEverHadAccount(false, false, true)) // eski bayrak
        assertFalse(s.showsFirstTutorial(true))
        assertTrue(s.shouldPersistAccountFlag(false, true, false))
        assertTrue(s.shouldPersistAccountFlag(false, false, true))
        assertFalse(s.shouldPersistAccountFlag(true, true, true)) // zaten yazılı
        assertFalse(s.shouldPersistAccountFlag(false, false, false))
    }

    @Test
    fun kayitAkisiAdimlari() {
        val f = SignupFlowRules
        // Öğretmen: yalnızca yaş ve kaynak
        assertEquals(listOf(SignupFlowRules.Step.AGE, SignupFlowRules.Step.SOURCE), f.flowSteps(f.streakStepsEnabled(forceTeacher = true)))
        assertEquals(50, f.progressFor(SignupFlowRules.Step.AGE, false))
        assertEquals(100, f.progressFor(SignupFlowRules.Step.SOURCE, false))
        // Öğrenci: 10 adım, 2 tanışma; sayılan 8 adım
        val ogrenci = f.flowSteps(true)
        assertEquals(10, ogrenci.size)
        assertEquals(0, f.progressFor(SignupFlowRules.Step.MEET, true))
        assertEquals(0, f.progressFor(SignupFlowRules.Step.BRIEF, true))
        assertEquals(12, f.progressFor(SignupFlowRules.Step.AGE, true)) // 1*100/8
        assertEquals(100, f.progressFor(SignupFlowRules.Step.REMINDER, true))
        assertEquals(6, f.questionCount(true)) // AGE SOURCE SOROBAN GOAL CHALLENGE REMINDER
        assertEquals(listOf(5, 10, 20), f.GOAL_OPTIONS)
        assertEquals(listOf(3, 5, 7), f.CHALLENGE_OPTIONS)
        assertEquals(19, f.DEFAULT_REMINDER_HOUR)
        assertEquals(listOf("none", "some", "good"), f.SOROBAN_LEVELS)
    }

    @Test
    fun gizlilikSayfasiDile() {
        val t = LoginFlowTexts
        assertEquals("https://numigo-new.web.app/privacy-policy-tr.html#tos-1", t.termsUrl("tr"))
        assertEquals("https://numigo-new.web.app/privacy-policy-id.html#pp-1", t.privacyUrl("id"))
        assertEquals("https://numigo-new.web.app/privacy-policy-ar.html#pp-1", t.privacyUrl("ar"))
        assertEquals("https://numigo-new.web.app/privacy-policy.html#tos-1", t.termsUrl("en"))
        assertEquals(120_000L, t.NEW_ACCOUNT_WINDOW_MS)
    }

    @Test
    fun ogretmenGirisi() {
        val t = TeacherLoginRules
        assertEquals("Lütfen tüm alanları doldurun", t.validateLogin("", "123456"))
        assertEquals("Lütfen tüm alanları doldurun", t.validateLogin("   ", "123456"))
        assertEquals("Lütfen tüm alanları doldurun", t.validateLogin("a@b.co", ""))
        // Biçim denetimi yoktur; boşluktan oluşan şifre de geçer.
        assertNull(t.validateLogin("gecersiz", " "))
        assertNull(t.validateLogin(" a@b.co ", "123456"))
    }

    @Test
    fun sifremiUnuttumEposta() {
        val t = TeacherLoginRules
        assertEquals("E-posta adresi girin", t.validateForgotPasswordEmail(null))
        assertEquals("E-posta adresi girin", t.validateForgotPasswordEmail("  "))
        assertEquals("Geçerli bir e-posta adresi girin", t.validateForgotPasswordEmail("abc"))
        assertNull(t.validateForgotPasswordEmail(" a@b.co "))
    }

    @Test
    fun yeniSifre() {
        val t = TeacherLoginRules
        assertEquals("Şifre en az 6 karakter olmalıdır", t.validateNewPassword("12345", "12345"))
        assertEquals("Şifreler eşleşmiyor", t.validateNewPassword("123456", "1234567"))
        assertNull(t.validateNewPassword("123456", "123456"))
        // Uzunluk denetimi eşleşmeden önce gelir.
        assertEquals("Şifre en az 6 karakter olmalıdır", t.validateNewPassword("1", "2"))
    }

    @Test
    fun metinler() {
        val m = LoginFlowTexts
        assertEquals("Şifre sıfırlama", m.otpTitle(forPasswordReset = true, isRegistration = true))
        assertEquals("Hesap oluştur", m.otpTitle(forPasswordReset = false, isRegistration = true))
        assertEquals("E-postanı doğrula", m.otpTitle(forPasswordReset = false, isRegistration = false))
        assertTrue(m.TERMS_FULL_TEXT.contains(m.TERMS_LINK_TEXT))
        assertTrue(m.TERMS_FULL_TEXT.contains(m.PRIVACY_LINK_TEXT))
    }

    @Test
    fun kullaniciKimligi() {
        val aday = UserIdRules.generateCandidate()
        assertEquals(8, aday.length)
        assertTrue(aday.take(6).all { it in '0'..'9' })
        assertTrue(aday.takeLast(2).all { it in 'A'..'Z' })
        val yedek = UserIdRules.generateFallback(1_700_000_123_456L)
        assertTrue(yedek.startsWith("123456"))
        assertEquals(8, yedek.length)
    }
}

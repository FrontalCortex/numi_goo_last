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
        assertEquals(2000L, r.SPLASH_ANIMATION_MS)
        assertEquals(8000L, r.SPLASH_MAX_EXTRA_WAIT_MS)
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
            listOf("Facebook", "Instagram", "Youtube", "Google Araması", "Arkadaş/Aile", "TikTok", "Uygulama Mağazası", "Diğer"),
            UserInfoRules.SOURCE_NAMES,
        )
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

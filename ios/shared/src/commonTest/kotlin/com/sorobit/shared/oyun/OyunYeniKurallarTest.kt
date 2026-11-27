// Güncel Android'e (claude/soban-gamified-education-prb70m) eşitleme sırasında eklenen oyun kurallarının testleri:
// TimeTracker günlük süre, ServerEnergy yeniden deneme, yaş/rıza → Analytics reklam izni, TrustedClock, PlanStatus,
// öğretmen cüzdan kipi ve yeni cüzdan alanları. Beklenen değerler Android kaynak kodundan elle türetilmiştir.
package com.sorobit.shared.oyun

import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OyunYeniKurallarTest {

    private fun gun(y: Int, m: Int, d: Int, saat: Int = 12, dakika: Int = 0): Long =
        LocalDate(y, m, d).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() + saat * 3_600_000L + dakika * 60_000L

    // ---------------------------------------------------------------- TimeTracker günlük süre

    @Test
    fun timeTrackerGunAnahtariVeYazma() {
        val now = gun(2026, 11, 27)
        val utc = TimeZone.UTC
        assertEquals("20261127", TimeTracker.dayKey(0, now, utc))
        assertEquals("20261113", TimeTracker.dayKey(-TimeTracker.RETENTION_DAYS, now, utc))
        assertEquals("20261028", TimeTracker.dayKey(-30, now, utc))
        assertEquals("20261202", TimeTracker.dayKey(5, now, utc))
        // Yıl sınırı
        assertEquals("20261219", TimeTracker.dayKey(-14, gun(2027, 1, 2), utc))
        // Yerel saat dilimi: 22:30 UTC = ertesi gün 01:30 (+03:00)
        val trt = FixedOffsetTimeZone(UtcOffset(hours = 3))
        assertEquals("20261128", TimeTracker.dayKey(0, gun(2026, 11, 27, 22, 30), trt))

        assertEquals("dailyTimeSpent", TimeTracker.DAILY_FIELD)
        assertEquals(14, TimeTracker.RETENTION_DAYS)
        assertEquals(
            TimeTracker.Companion.SyncWrite(
                totalIncrement = 120L,
                dailyFieldPath = listOf("dailyTimeSpent", "20261127"),
                dailyIncrement = 120L,
                expiredFieldPath = listOf("dailyTimeSpent", "20261113"),
            ),
            TimeTracker.buildSyncWrite(120L, now, utc),
        )
    }

    // ---------------------------------------------------------------- ServerEnergy yeniden deneme

    @Test
    fun serverEnergyYenidenDeneme() {
        assertEquals(10, ServerEnergy.AD_VERIFY_MAX_ATTEMPTS)
        assertEquals(1500L, ServerEnergy.AD_VERIFY_RETRY_DELAY_MS)
        assertTrue(ServerEnergy.shouldRetryClaimAdEnergy(isUnavailable = true, attempt = 0))
        assertTrue(ServerEnergy.shouldRetryClaimAdEnergy(isUnavailable = true, attempt = 9))
        assertFalse(ServerEnergy.shouldRetryClaimAdEnergy(isUnavailable = true, attempt = 10))
        assertFalse(ServerEnergy.shouldRetryClaimAdEnergy(isUnavailable = false, attempt = 0))
    }

    // ---------------------------------------------------------------- Analytics reklam izni

    @Test
    fun analyticsReklamIzni() {
        val a = AgeConsentManager
        assertTrue(a.analyticsAdConsentGranted(AgeConsentManager.AgeTreatment.TEEN))
        assertFalse(a.analyticsAdConsentGranted(AgeConsentManager.AgeTreatment.CHILD))
        assertFalse(a.analyticsAdConsentGranted(AgeConsentManager.AgeTreatment.UNSPECIFIED))
        // Hesap değişince izin kapanır; yalnızca aynı hesapta sürer
        assertTrue(a.shouldRevokeAnalyticsAdConsent(null, null))
        assertTrue(a.shouldRevokeAnalyticsAdConsent("a", null))
        assertTrue(a.shouldRevokeAnalyticsAdConsent("a", "b"))
        assertTrue(a.shouldRevokeAnalyticsAdConsent(null, "b"))
        assertFalse(a.shouldRevokeAnalyticsAdConsent("a", "a"))
    }

    // ---------------------------------------------------------------- TrustedClock

    @Test
    fun trustedClockKurallari() {
        val capa = TrustedClockRules.Anchor(serverNowMs = 1000L, elapsedMs = 500L, bootCount = 7)
        // Çapa yok: düz duvar saati
        assertEquals(42L, TrustedClockRules.trustedNow(null, 42L, 1L, 7))
        // Aynı açılış: sunucu anı + geçen gerçek süre; duvar saatine bakılmaz (ileri/geri fark etmez)
        assertEquals(1300L, TrustedClockRules.trustedNow(capa, wallNowMs = 999_999L, elapsedNowMs = 800L, bootCount = 7))
        assertEquals(1300L, TrustedClockRules.trustedNow(capa, wallNowMs = 5L, elapsedNowMs = 800L, bootCount = 7))
        // Yeniden başlatma (monoton saat geriye gitti ya da açılış sayacı farklı): duvar saati, çapadan geriye gidilmez
        assertEquals(999_999L, TrustedClockRules.trustedNow(capa, 999_999L, 400L, 7))
        assertEquals(1000L, TrustedClockRules.trustedNow(capa, 5L, 400L, 7))
        assertEquals(1000L, TrustedClockRules.trustedNow(capa, 5L, 800L, 8))
    }

    @Test
    fun trustedClockSinifi() {
        val depo = InMemoryKeyValueStore()
        var elapsed = 100L
        var wall = 5_000L
        val saat = TrustedClock(depo, { elapsed }, bootCount = 3, wallNowMs = { wall })
        assertEquals(5_000L, saat.nowMs())

        var bildirim = 0
        saat.addAnchorListener { bildirim++ }
        saat.addAnchorListener { throw IllegalStateException("dinleyici hatası") }
        // Dönen değer: duvar saati - sunucu saati
        assertEquals(-5_000L, saat.onServerTime(10_000L))
        assertEquals(1, bildirim)
        elapsed = 600L
        wall = 9_999_999L // saat ileri alındı: etkisiz
        assertEquals(10_500L, saat.nowMs())

        // Çapa kalıcıdır: aynı açılışta yeni örnek geçerli
        elapsed = 700L
        val ayni = TrustedClock(depo, { elapsed }, bootCount = 3, wallNowMs = { 5_000L })
        assertEquals(10_600L, ayni.nowMs())
        // Açılış sayacı okunamıyorsa (0) çapa yalnızca alt sınırdır
        val bilinmeyen = TrustedClock(depo, { elapsed }, bootCount = 0, wallNowMs = { 5_000L })
        assertEquals(10_000L, bilinmeyen.nowMs())
        assertEquals("trusted_clock", TrustedClock.PREFS_NAME)
    }

    // ---------------------------------------------------------------- PlanStatus

    @Test
    fun planStatus() {
        val p = PlanStatus
        assertEquals("Pro", p.effectivePlan("Pro", null, null, 100L))
        assertEquals("Free", p.effectivePlan(null, null, null, 100L))
        // Süresi geçmiş abonelik Free
        assertEquals("Free", p.effectivePlan("Pro", 50L, null, 100L))
        assertEquals("Pro", p.effectivePlan("Pro", 200L, null, 100L))
        // Yayın hediyesi: Pro'dan düşük abonelik Pro'ya yükselir; Premium korunur
        assertEquals("Pro", p.effectivePlan("Lite", null, 300L, 100L))
        assertEquals("Pro", p.effectivePlan("Free", 50L, 300L, 100L))
        assertEquals("Premium", p.effectivePlan("Premium", null, 300L, 100L))
        // Biten hediye etkisiz
        assertEquals("Lite", p.effectivePlan("Lite", null, 50L, 100L))
        assertEquals(300L, p.giftProUntil(300L, 100L))
        assertNull(p.giftProUntil(50L, 100L))
        assertNull(p.giftProUntil(null, 100L))
        assertTrue(p.isProPlan("PRO"))
        assertTrue(p.isProPlan("Premium"))
        assertFalse(p.isProPlan("Lite"))
        assertEquals("giftProUntil", p.FIELD_GIFT_PRO_UNTIL)
    }

    // ---------------------------------------------------------------- Cüzdan: öğretmen kipi ve yeni alanlar

    @Test
    fun cuzdanOgretmenKipi() {
        val w = UserWalletFirestore
        assertEquals(w.MODE_NORMAL, w.modeOf(null, true))
        assertEquals(w.MODE_NORMAL, w.modeOf("STUDENT", false))
        assertEquals(w.MODE_LOCKED, w.modeOf("TEACHER", false))
        assertEquals(w.MODE_LOCKED, w.modeOf("TEACHER", null))
        assertEquals(w.MODE_UNLIMITED, w.modeOf("TEACHER", true))

        // Normal kip: gerçek değerler
        val normal = InMemoryKeyValueStore()
        assertEquals(5, w.visible(normal, 5))
        assertFalse(w.isUnlimited(normal))
        assertEquals("5", w.displayText(normal, 5))

        // Onaylı öğretmen: sınırsız görünür, önbellek GERÇEK değeri tutar
        val sinirsiz = InMemoryKeyValueStore()
        w.rememberMode(sinirsiz, "TEACHER", true)
        val yuklenen = w.resolveLoadedWallet(sinirsiz, docKeys = 5, docCurrency = 7).wallet
        assertEquals(UserWallet(w.UNLIMITED_BALANCE, w.UNLIMITED_BALANCE), yuklenen)
        assertEquals(5, sinirsiz.getInt("keys", -1))
        assertEquals(7, sinirsiz.getInt("currency", -1))
        assertEquals(w.UNLIMITED_BALANCE, w.getCachedKeys(sinirsiz))
        assertTrue(w.isUnlimited(sinirsiz))
        assertEquals("∞", w.displayText(sinirsiz, 9))
        // Sunucu yanıtı gerçek bakiyeyi taşır; önbelleğe o yazılır, ekrana kip uygulanır
        val guncel = w.walletFromUpdateResult(sinirsiz, mapOf("keys" to 4L, "currency" to 1L, "rollbackToken" to "t"))
        assertEquals(UserWallet(w.UNLIMITED_BALANCE, w.UNLIMITED_BALANCE, "t"), guncel)
        assertEquals(4, sinirsiz.getInt("keys", -1))

        // Onaysız öğretmen: 0
        val kilitli = InMemoryKeyValueStore()
        w.rememberMode(kilitli, "TEACHER", false)
        assertEquals(UserWallet(0, 0), w.walletFromSnapshot(kilitli, 3, 9))
        assertEquals(0, w.getCachedKeys(kilitli))
        assertEquals(UserWallet(0, 0), w.walletFromCache(kilitli))
        // Kip değişince (destek onaylar) aynı önbellek yeni kipte okunur
        w.rememberMode(kilitli, "TEACHER", true)
        assertEquals(w.UNLIMITED_BALANCE, w.getCachedCurrency(kilitli))
        w.rememberMode(kilitli, "STUDENT", null)
        assertEquals(9, w.getCachedCurrency(kilitli))
    }

    @Test
    fun cuzdanYeniAlanlarVeOnbellek() {
        val w = UserWalletFirestore
        assertEquals("questionCredits", w.FIELD_QUESTION_CREDITS)
        assertEquals("sobiCredits", w.FIELD_SOBI_CREDITS)
        // Varsayılanlar
        val bos = UserWallet(1, 0)
        assertEquals(0, bos.questionCredits)
        assertEquals(0, bos.sobiCredits)
        assertEquals("Free", bos.plan)

        val depo = InMemoryKeyValueStore()
        assertEquals(
            UserWallet(keys = 2, currency = 3, questionCredits = 4, sobiCredits = -1, plan = "Pro"),
            w.walletFromSnapshot(depo, 2, 3, questionCredits = 4, sobiCredits = -1, plan = "Pro"),
        )
        // Krediler ve plan önbelleğe yazılmaz
        assertEquals(setOf("keys", "currency"), depo.keys().toSet())

        w.cacheCurrency(depo, 55)
        w.cacheKeys(depo, 8)
        assertEquals(55, w.getCachedCurrency(depo))
        assertEquals(8, w.getCachedKeys(depo))
    }
}

// TimeTracker, BillingCatalog, AgeConsentManager, AppRatingManager, AdRules, MarathonGuideStore,
// FirstTutorialShownStore ve ortak altyapının (anahtar-değer deposu, saat yardımcıları) Android
// davranışıyla aynı sonucu verdiğini doğrular.
package com.sorobit.shared.oyun

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class OyunAltyapiTest {
    @Test
    fun bellekIciDepo() {
        val s = InMemoryKeyValueStore()
        // Varsayılanlar
        assertNull(s.getString("a"))
        assertEquals(7, s.getInt("a", 7))
        assertEquals(8L, s.getLong("a", 8L))
        assertTrue(s.getBoolean("a", true))
        assertFalse(s.contains("a"))

        s.putString("s", "x")
        s.putInt("i", 1)
        s.putLong("l", 2L)
        s.putBoolean("b", true)
        assertEquals("x", s.getString("s"))
        assertEquals(1, s.getInt("i", 0))
        assertEquals(2L, s.getLong("l", 0))
        assertTrue(s.getBoolean("b", false))
        assertEquals(setOf("s", "i", "l", "b"), s.keys())
        // Tip uyuşmazlığında varsayılan döner
        assertEquals(5, s.getInt("s", 5))
        assertNull(s.getString("i"))
        assertEquals(9L, s.getLong("i", 9L))

        s.remove("s")
        assertFalse(s.contains("s"))
        s.clear()
        assertTrue(s.keys().isEmpty())
    }

    @Test
    fun ikiHaneVeTakvimYardimcilari() {
        assertEquals("00", ikiHane(0))
        assertEquals("09", ikiHane(9))
        assertEquals("10", ikiHane(10))
        assertEquals("123", ikiHane(123))

        // 2026-06-01 Pazartesi
        assertEquals(LocalDate(2026, 6, 1), yerelTarih(1_780_272_000_000L, TimeZone.UTC))
        assertEquals(LocalDate(2026, 6, 1), haftaninPazartesisi(LocalDate(2026, 6, 1)))
        assertEquals(LocalDate(2026, 6, 1), haftaninPazartesisi(LocalDate(2026, 6, 7)))
        assertEquals(LocalDate(2026, 6, 8), haftaninPazartesisi(LocalDate(2026, 6, 8)))
        assertEquals(1_780_272_000_000L, gunBaslangiciMs(LocalDate(2026, 6, 1), TimeZone.UTC))
        assertEquals(LocalDate(2026, 7, 1), LocalDate(2026, 6, 30).gunEkle(1))
        // Sistem saati makul bir değer döndürür (2020'den sonra)
        assertTrue(oyunSimdiMs() > 1_577_836_800_000L)
    }
}

private class SahteSurePlatformu(var uid: String? = "u1") : TimeTrackerPlatform {
    val artislar = mutableListOf<Pair<String, Long>>()
    val yazimlar = mutableListOf<Pair<String, Long>>()
    var zamanlayiciAcik = false
    var aralik = 0L

    override fun currentUid(): String? = uid
    override fun incrementTotalTimeSpent(uid: String, seconds: Long) {
        artislar.add(uid to seconds)
    }

    override fun setTotalTimeSpent(uid: String, value: Long) {
        yazimlar.add(uid to value)
    }

    override fun startPeriodicTick(intervalSeconds: Long) {
        zamanlayiciAcik = true
        aralik = intervalSeconds
    }

    override fun stopPeriodicTick() {
        zamanlayiciAcik = false
    }
}

class TimeTrackerTest {
    private val t0 = 1_700_000_000_000L

    private class Kurulum(uid: String? = "u1", var simdi: Long, val prefs: InMemoryKeyValueStore = InMemoryKeyValueStore()) {
        val platform = SahteSurePlatformu(uid)
        val izleyici = TimeTracker(prefs, platform) { simdi }
    }

    @Test
    fun sabitler() {
        assertEquals("time_tracker_prefs", TimeTracker.PREFS_NAME)
        assertEquals("unsynced_time_seconds", TimeTracker.KEY_UNSYNCED_TIME)
        assertEquals("last_start_time", TimeTracker.KEY_LAST_START_TIME)
        assertEquals("totalTimeSpent", TimeTracker.FIELD_TOTAL_TIME_SPENT)
        assertEquals(10L, TimeTracker.TICK_INTERVAL_SECONDS)
    }

    @Test
    fun takipDonemiVeEsitleme() {
        val k = Kurulum(simdi = t0)
        val bildirimler = mutableListOf<Long>()
        k.izleyici.onUnsyncedTimeChanged = { bildirimler.add(it) }
        k.izleyici.initialize()
        assertTrue(k.platform.artislar.isEmpty())

        k.izleyici.startTracking()
        assertTrue(k.platform.zamanlayiciAcik)
        assertEquals(10L, k.platform.aralik)
        assertEquals(t0, k.prefs.getLong("last_start_time", -1))

        // 9,9 sn: 10 sn dolmadı → eklenmez
        k.simdi = t0 + 9_900
        k.izleyici.onPeriodicTick()
        assertEquals(0L, k.izleyici.unsyncedTime)
        // 10 sn: eklenir, başlangıç güncellenir
        k.simdi = t0 + 10_000
        k.izleyici.onPeriodicTick()
        assertEquals(10L, k.izleyici.unsyncedTime)
        assertEquals(10L, k.prefs.getLong("unsynced_time_seconds", -1))
        assertEquals(t0 + 10_000, k.prefs.getLong("last_start_time", -1))
        // Saniye aşağı yuvarlanır: 12,7 sn → 12
        k.simdi = t0 + 22_700
        k.izleyici.onPeriodicTick()
        assertEquals(22L, k.izleyici.unsyncedTime)

        // Durdurma: kalan 3,3 sn → 3 eklenir, hepsi buluta gider, yerel sıfırlanır
        k.simdi = t0 + 26_000
        k.izleyici.stopTracking()
        assertFalse(k.platform.zamanlayiciAcik)
        assertEquals(listOf("u1" to 25L), k.platform.artislar)
        assertEquals(0L, k.izleyici.unsyncedTime)
        assertEquals(0L, k.prefs.getLong("unsynced_time_seconds", -1))
        assertFalse(k.prefs.contains("last_start_time"))
        assertEquals(listOf(0L, 10L, 22L, 25L, 0L), bildirimler)

        // Durmuşken tık ve ikinci durdurma etkisiz
        k.simdi = t0 + 99_000
        k.izleyici.onPeriodicTick()
        k.izleyici.stopTracking()
        assertEquals(1, k.platform.artislar.size)
    }

    @Test
    fun ciftBaslatmaYokSayilir() {
        val k = Kurulum(simdi = t0)
        k.izleyici.initialize()
        k.izleyici.startTracking()
        k.simdi = t0 + 5_000
        k.izleyici.startTracking()
        // Başlangıç zamanı değişmedi
        assertEquals(t0, k.prefs.getLong("last_start_time", -1))
        k.simdi = t0 + 7_000
        k.izleyici.stopTracking()
        assertEquals(listOf("u1" to 7L), k.platform.artislar)
    }

    @Test
    fun birSaniyedenKisaOturumEklenmez() {
        val k = Kurulum(simdi = t0)
        k.izleyici.initialize()
        k.izleyici.startTracking()
        k.simdi = t0 + 999
        k.izleyici.stopTracking()
        assertTrue(k.platform.artislar.isEmpty())
        assertEquals(0L, k.izleyici.unsyncedTime)
    }

    @Test
    fun cokmeSonrasiKurtarma() {
        // Son başlangıçtan 5 dk sonra açılış: 300 sn eklenir ve hemen eşitlenir
        var prefs = InMemoryKeyValueStore().apply {
            putLong("last_start_time", t0 - 300_000)
            putLong("unsynced_time_seconds", 40)
        }
        var k = Kurulum(simdi = t0, prefs = prefs)
        k.izleyici.initialize()
        assertEquals(listOf("u1" to 340L), k.platform.artislar)
        assertFalse(prefs.contains("last_start_time"))

        // 1 saat ve üzeri fark yok sayılır (3600 sn dahil değil), ama mevcut birikim eşitlenir
        prefs = InMemoryKeyValueStore().apply {
            putLong("last_start_time", t0 - 3_600_000)
            putLong("unsynced_time_seconds", 40)
        }
        k = Kurulum(simdi = t0, prefs = prefs)
        k.izleyici.initialize()
        assertEquals(listOf("u1" to 40L), k.platform.artislar)
        assertFalse(prefs.contains("last_start_time"))

        // 3599 sn kabul edilir
        prefs = InMemoryKeyValueStore().apply { putLong("last_start_time", t0 - 3_599_000) }
        k = Kurulum(simdi = t0, prefs = prefs)
        k.izleyici.initialize()
        assertEquals(listOf("u1" to 3599L), k.platform.artislar)

        // Gelecekteki başlangıç (saat geri alınmış) yok sayılır
        prefs = InMemoryKeyValueStore().apply { putLong("last_start_time", t0 + 50_000) }
        k = Kurulum(simdi = t0, prefs = prefs)
        k.izleyici.initialize()
        assertTrue(k.platform.artislar.isEmpty())
        assertFalse(prefs.contains("last_start_time"))
    }

    @Test
    fun oturumYokkenSureYereldeBirikir() {
        val k = Kurulum(uid = null, simdi = t0)
        k.izleyici.initialize()
        k.izleyici.startTracking()
        k.simdi = t0 + 30_000
        k.izleyici.stopTracking()
        assertTrue(k.platform.artislar.isEmpty())
        assertEquals(30L, k.izleyici.unsyncedTime)
        assertEquals(30L, k.prefs.getLong("unsynced_time_seconds", -1))
        // Oturum açılınca bir sonraki eşitlemede o kullanıcıya yazılır
        k.platform.uid = "u9"
        k.izleyici.startTracking()
        k.simdi += 5_000
        k.izleyici.stopTracking()
        assertEquals(listOf("u9" to 35L), k.platform.artislar)
    }

    @Test
    fun sifirlama() {
        val k = Kurulum(simdi = t0, prefs = InMemoryKeyValueStore().apply { putLong("unsynced_time_seconds", 12) })
        k.izleyici.reset()
        assertEquals(0L, k.prefs.getLong("unsynced_time_seconds", -1))
        assertEquals(0L, k.izleyici.unsyncedTime)
        assertEquals(listOf("u1" to 0L), k.platform.yazimlar)
        // Oturum yoksa yalnızca yerel sıfırlanır
        val misafir = Kurulum(uid = null, simdi = t0)
        misafir.izleyici.reset()
        assertTrue(misafir.platform.yazimlar.isEmpty())
    }
}

class BillingCatalogTest {
    @Test
    fun urunKimlikleri() {
        assertEquals(
            listOf(
                "gold_small", "gold_medium", "gold_large", "keys_small", "keys_medium", "keys_large",
                "credits_small", "credits_medium", "credits_large",
                "sobi_credits_20", "sobi_credits_50", "sobi_credits_120",
            ),
            BillingCatalog.consumables,
        )
        assertEquals(listOf("pro_monthly", "lite_monthly"), BillingCatalog.subscriptions)
        assertEquals("gold_small", BillingCatalog.GOLD_SMALL)
        assertEquals("gold_medium", BillingCatalog.GOLD_MEDIUM)
        assertEquals("gold_large", BillingCatalog.GOLD_LARGE)
        assertEquals("keys_small", BillingCatalog.KEYS_SMALL)
        assertEquals("credits_large", BillingCatalog.CREDITS_LARGE)
        assertEquals("sobi_credits_20", BillingCatalog.SOBI_CREDITS_20)
        assertEquals("keys_medium", BillingCatalog.KEYS_MEDIUM)
        assertEquals("keys_large", BillingCatalog.KEYS_LARGE)
        // Abonelik rütbesi
        assertEquals(2, BillingCatalog.subscriptionRank(BillingCatalog.SUB_PRO))
        assertEquals(1, BillingCatalog.subscriptionRank(BillingCatalog.SUB_LITE))
        assertEquals(0, BillingCatalog.subscriptionRank("gold_small"))
    }

    @Test
    fun plandanAbonelige() {
        assertEquals("lite_monthly", BillingCatalog.subscriptionForPlanName("Lite"))
        assertEquals("lite_monthly", BillingCatalog.subscriptionForPlanName("lite"))
        assertEquals("lite_monthly", BillingCatalog.subscriptionForPlanName("LITE"))
        assertEquals("pro_monthly", BillingCatalog.subscriptionForPlanName("Pro"))
        // Tanınmayan her ad Pro'ya düşer
        assertEquals("pro_monthly", BillingCatalog.subscriptionForPlanName("Premium"))
        assertEquals("pro_monthly", BillingCatalog.subscriptionForPlanName(""))
        assertEquals("pro_monthly", BillingCatalog.subscriptionForPlanName(" Lite"))
    }
}

class AgeConsentManagerTest {
    private val child = AgeConsentManager.AgeTreatment.CHILD
    private val teen = AgeConsentManager.AgeTreatment.TEEN

    @Test
    fun ulkelereGoreRizaYasi() {
        listOf("DE", "IE", "NL", "RO", "HR", "LU", "SK", "HU", "PL").forEach {
            assertEquals(16, AgeConsentManager.getDigitalConsentAgeForCountry(it), it)
        }
        listOf("FR", "GR", "CZ", "SI").forEach {
            assertEquals(15, AgeConsentManager.getDigitalConsentAgeForCountry(it), it)
        }
        listOf("AT", "BG", "CY", "ES", "IT", "LT").forEach {
            assertEquals(14, AgeConsentManager.getDigitalConsentAgeForCountry(it), it)
        }
        listOf("TR", "US", "GB", "ID", "BE", "SE", "", "de").forEach {
            assertEquals(13, AgeConsentManager.getDigitalConsentAgeForCountry(it), it)
        }
    }

    @Test
    fun muameleSecimi() {
        // Oturum yok → CHILD
        assertEquals(child, AgeConsentManager.treatmentFor(false, 1990, 2026, "TR"))
        // Doğum yılı yok/geçersiz → CHILD
        assertEquals(child, AgeConsentManager.treatmentFor(true, -1, 2026, "TR"))
        assertEquals(child, AgeConsentManager.treatmentFor(true, 0, 2026, "TR"))
        // TR (13): 12 yaş CHILD, 13 yaş TEEN
        assertEquals(child, AgeConsentManager.treatmentFor(true, 2014, 2026, "TR"))
        assertEquals(teen, AgeConsentManager.treatmentFor(true, 2013, 2026, "TR"))
        // DE (16): 15 yaş CHILD, 16 yaş TEEN
        assertEquals(child, AgeConsentManager.treatmentFor(true, 2011, 2026, "DE"))
        assertEquals(teen, AgeConsentManager.treatmentFor(true, 2010, 2026, "DE"))
        // Yetişkin de TEEN sayılır (Android'deki hâliyle)
        assertEquals(teen, AgeConsentManager.treatmentFor(true, 1985, 2026, "FR"))
        // Gelecekteki doğum yılı (negatif yaş) → CHILD
        assertEquals(child, AgeConsentManager.treatmentFor(true, 2030, 2026, "TR"))
    }

    @Test
    fun belgedenDogumYili() {
        assertEquals(2012, AgeConsentManager.birthYearFromDoc(true, true, 2012L))
        assertEquals(-1, AgeConsentManager.birthYearFromDoc(true, true, null))
        assertEquals(-1, AgeConsentManager.birthYearFromDoc(true, false, 2012L))
        assertEquals(-1, AgeConsentManager.birthYearFromDoc(false, true, 2012L))
        assertEquals("birthYear", AgeConsentManager.FIELD_BIRTH_YEAR)
    }

    @Test
    fun reklamBayraklari() {
        assertEquals(AgeConsentManager.AdRequestTags(true, true), AgeConsentManager.adRequestTags(child))
        assertEquals(AgeConsentManager.AdRequestTags(true, true), AgeConsentManager.adRequestTags(AgeConsentManager.AgeTreatment.UNSPECIFIED))
        assertEquals(AgeConsentManager.AdRequestTags(false, false), AgeConsentManager.adRequestTags(teen))
        assertTrue(AgeConsentManager.consentUnderAge(child))
        assertTrue(AgeConsentManager.consentUnderAge(AgeConsentManager.AgeTreatment.UNSPECIFIED))
        assertFalse(AgeConsentManager.consentUnderAge(teen))
        assertEquals(listOf("CHILD", "TEEN", "UNSPECIFIED"), AgeConsentManager.AgeTreatment.entries.map { it.name })
    }

    @Test
    fun ulkeKoduOnceligi() {
        assertEquals("TR", AgeConsentManager.resolveCountryCode("tr", "de", "US"))
        assertEquals("DE", AgeConsentManager.resolveCountryCode("", "de", "US"))
        assertEquals("DE", AgeConsentManager.resolveCountryCode(null, "de", "US"))
        assertEquals("FR", AgeConsentManager.resolveCountryCode(" ", null, "fr"))
        assertEquals("US", AgeConsentManager.resolveCountryCode(null, null, ""))
        assertEquals("US", AgeConsentManager.resolveCountryCode(null, null, null))
    }
}

class AppRatingManagerTest {
    private class Kurulum(var uid: String? = "u1", var simdi: Long = 1_000_000L) {
        val prefs = InMemoryKeyValueStore()
        val yonetici = AppRatingManager(prefs, { uid }) { simdi }
        fun dene(
            ders: Int = 3,
            adSkip: Boolean = false,
            kupaYolu: Boolean = false,
            diyalogAcik: Boolean = false,
        ) = yonetici.checkAndShowRatingPrompt(ders, adSkip, kupaYolu, diyalogAcik)
    }

    private val goster = RatingPromptDecision(result = true, showDialog = true)
    private val hayir = RatingPromptDecision(result = false, showDialog = false)
    private val zatenDevrede = RatingPromptDecision(result = true, showDialog = false)

    @Test
    fun anahtarlarVeSabitler() {
        assertEquals("AppRatingPrefs", AppRatingManager.PREFS_NAME)
        assertEquals("has_rated_u1", AppRatingManager.hasRatedKey("u1"))
        assertEquals("has_rated_anonymous", AppRatingManager.hasRatedKey(null))
        assertEquals("ratingFeedback", AppRatingManager.COLLECTION_RATING_FEEDBACK)
        assertEquals("RatingDialog", AppRatingManager.TAG_RATING_DIALOG)
    }

    @Test
    fun kosullarSaglaninccaGosterilir() {
        val k = Kurulum()
        assertEquals(goster, k.dene())
        // Tam 1 ders yeterli; 0 ders yetmez
        assertEquals(goster, Kurulum().dene(ders = 1))
        assertEquals(hayir, Kurulum().dene(ders = 0))
    }

    @Test
    fun puanlamisHesapBirDahaGormez() {
        val k = Kurulum()
        k.yonetici.markAsRated()
        assertTrue(k.prefs.getBoolean("has_rated_u1", false))
        assertEquals(hayir, k.dene())
        // Başka hesap etkilenmez
        k.uid = "u2"
        assertEquals(goster, k.dene())
        // Oturumsuz kullanıcı "anonymous" anahtarını kullanır
        val misafir = Kurulum(uid = null)
        misafir.yonetici.markAsRated()
        assertTrue(misafir.prefs.getBoolean("has_rated_anonymous", false))
        assertEquals(hayir, misafir.dene())
    }

    @Test
    fun ikiSaniyeKurali() {
        val k = Kurulum()
        assertEquals(goster, k.dene())
        // 2 sn içinde yeniden: gösterilmez ama true döner
        k.simdi += 1_999
        assertEquals(zatenDevrede, k.dene())
        // Tam 2 sn sonra yeniden değerlendirilir
        k.simdi += 1
        assertEquals(goster, k.dene())
    }

    @Test
    fun engelleyiciDurumlar() {
        assertEquals(hayir, Kurulum().dene(adSkip = true))
        assertEquals(hayir, Kurulum().dene(kupaYolu = true))
        assertEquals(zatenDevrede, Kurulum().dene(diyalogAcik = true))
        // Öncelik: AdSkip → Kupa Yolu → açık diyalog
        assertEquals(hayir, Kurulum().dene(adSkip = true, diyalogAcik = true))
        assertEquals(hayir, Kurulum().dene(kupaYolu = true, diyalogAcik = true))

        // Engellenen deneme de 2 sn sayacını başlatır: hemen ardından gelen çağrı true döner
        val k = Kurulum()
        assertEquals(hayir, k.dene(adSkip = true))
        k.simdi += 500
        assertEquals(zatenDevrede, k.dene())
        k.simdi += 2_000
        assertEquals(goster, k.dene())

        // Ders eşiğine ya da "puanladı" bayrağına takılan deneme sayacı BAŞLATMAZ
        val k2 = Kurulum()
        assertEquals(hayir, k2.dene(ders = 0))
        assertEquals(goster, k2.dene())
    }

    @Test
    fun yildizVeGeriBildirimKurallari() {
        assertFalse(AppRatingManager.shouldLaunchStoreReview(1))
        assertFalse(AppRatingManager.shouldLaunchStoreReview(3))
        assertTrue(AppRatingManager.shouldLaunchStoreReview(4))
        assertTrue(AppRatingManager.shouldLaunchStoreReview(5))

        assertEquals(
            listOf("star_on_ic", "star_on_ic", "star_on_ic", "star_off_ic", "star_off_ic"),
            (0..4).map { AppRatingManager.starDrawable(it, 3) },
        )
        assertEquals(List(5) { "star_off_ic" }, (0..4).map { AppRatingManager.starDrawable(it, 0) })
        assertEquals(List(5) { "star_on_ic" }, (0..4).map { AppRatingManager.starDrawable(it, 5) })

        assertTrue(AppRatingManager.canSubmitFeedback(" iyi "))
        assertFalse(AppRatingManager.canSubmitFeedback("   "))
        assertFalse(AppRatingManager.canSubmitFeedback(""))

        assertEquals("u1", AppRatingManager.feedbackDocId("u1"))
        assertEquals("anonymous", AppRatingManager.feedbackDocId(null))
        assertEquals(
            mapOf<String, Any>("uid" to "u1", "stars" to 2, "feedback" to "yavaş"),
            AppRatingManager.feedbackData("u1", 2, "yavaş"),
        )
        assertEquals("anonymous", AppRatingManager.feedbackData(null, 1, "x")["uid"])
        assertEquals("timestamp", AppRatingManager.FIELD_TIMESTAMP)
    }
}

class AdRulesTest {
    @Test
    fun ssvVeOturumKurali() {
        assertEquals("uid123:nonce-abc", AdRules.ssvCustomData("uid123", "nonce-abc"))
        assertTrue(AdRules.canShowRewardedAd("u"))
        assertFalse(AdRules.canShowRewardedAd(null))
        assertEquals("ca-app-pub-8436855856536384/7535644549", AdRules.ANDROID_REWARDED_AD_UNIT_ID)
        // Android güncel: geçiş reklamı release'te GERÇEK birim, debug'da Google'ın test birimi
        assertEquals("ca-app-pub-8436855856536384/8539721523", AdRules.ANDROID_INTERSTITIAL_AD_UNIT_ID)
        assertEquals("ca-app-pub-3940256099942544/1033173712", AdRules.TEST_INTERSTITIAL_AD_UNIT_ID)
        assertEquals(AdRules.ANDROID_INTERSTITIAL_AD_UNIT_ID, AdRules.interstitialAdUnitId(isDebug = false))
        assertEquals(AdRules.TEST_INTERSTITIAL_AD_UNIT_ID, AdRules.interstitialAdUnitId(isDebug = true))
    }

    @Test
    fun odulluReklamSonrasiGecEkrani() {
        assertTrue(AdRules.shouldShowAdSkipAfterRewarded(showAdSkipAfter = true, hostSupportsDialogs = true))
        assertFalse(AdRules.shouldShowAdSkipAfterRewarded(showAdSkipAfter = false, hostSupportsDialogs = true))
        assertFalse(AdRules.shouldShowAdSkipAfterRewarded(showAdSkipAfter = true, hostSupportsDialogs = false))
    }

    @Test
    fun yuklemeYenidenDenemeKurali() {
        assertEquals(listOf(30_000L, 60_000L, 120_000L, 240_000L), AdRules.RETRY_DELAYS_MS)
        // Düzelebilir hatalar: dahili hata, ağ, dolgu yok, aracı dolgu yok
        for (kod in listOf(0, 2, 3, 9)) assertTrue(AdRules.isRetryable(kod))
        // Hatalı istek ve diğer kodlar düzelmez
        for (kod in listOf(1, 4, 8, 99)) assertFalse(AdRules.isRetryable(kod))
        assertEquals(30_000L, AdRules.nextRetryDelayMs(3, 0))
        assertEquals(240_000L, AdRules.nextRetryDelayMs(2, 3))
        // Bütçe doldu (4 yeniden deneme) ya da düzelmeyen hata → yok
        assertNull(AdRules.nextRetryDelayMs(3, 4))
        assertNull(AdRules.nextRetryDelayMs(1, 0))
        // Yalnızca zincirin ilk hatası olaya yazılır
        assertTrue(AdRules.shouldLogLoadFailure(0))
        assertFalse(AdRules.shouldLogLoadFailure(1))
    }

    @Test
    fun reklamGecPaneliPolitikasi() {
        var t = 1_800_000_000_000L
        val saat = 3_600_000L
        val depo = InMemoryKeyValueStore()
        val p = AdRules.AdSkipPolicy(depo, nowMs = { t }, timeZone = kotlinx.datetime.TimeZone.UTC)

        // İlk üç reklamda panel yok, dördüncüde var; sayaç sıfırlanır
        repeat(3) { assertFalse(p.onAdClosed(eligible = true)) }
        assertTrue(p.onAdClosed(eligible = true))
        assertEquals(0, depo.getInt("ads_since_panel", -1))
        assertEquals(1, depo.getInt("panel_day_count", 0))

        // İki panel arası 2 saat: sayaç eşikte bekler (sıfırlanmaz)
        repeat(4) { assertFalse(p.onAdClosed(eligible = true)) }
        assertEquals(4, depo.getInt("ads_since_panel", -1))
        t += 2 * saat - 1
        assertFalse(p.onAdClosed(eligible = true))
        t += 1
        // Süre doldu: bir sonraki uygun reklamda panel (baştan 4 reklam beklenmez); günün 2. paneli
        assertTrue(p.onAdClosed(eligible = true))
        assertEquals(2, depo.getInt("panel_day_count", 0))

        // Gün içi tavan: iki panel doldu
        t += 3 * saat
        repeat(6) { assertFalse(p.onAdClosed(eligible = true)) }

        // Ertesi gün tavan sıfırlanır
        t += 24 * saat
        assertTrue(p.onAdClosed(eligible = true))
        assertEquals(1, depo.getInt("panel_day_count", 0))

        // Uygun olmayan reklam panel çıkarmaz ama sayılır
        t += 3 * saat
        val d2 = InMemoryKeyValueStore()
        val q = AdRules.AdSkipPolicy(d2, nowMs = { t }, timeZone = kotlinx.datetime.TimeZone.UTC)
        repeat(5) { assertFalse(q.onAdClosed(eligible = false)) }
        assertEquals(5, d2.getInt("ads_since_panel", -1))
        assertTrue(q.onAdClosed(eligible = true))

        // Saat geri alındı: negatif süre "dolmuş" sayılır, kilitlenmez
        val d3 = InMemoryKeyValueStore()
        d3.putLong("last_panel_ms", t + 10 * saat)
        d3.putInt("ads_since_panel", 3)
        assertTrue(AdRules.AdSkipPolicy(d3, nowMs = { t }, timeZone = kotlinx.datetime.TimeZone.UTC).onAdClosed(eligible = true))

        // Test anahtarı: uygun her reklamdan sonra
        val d4 = InMemoryKeyValueStore()
        val r = AdRules.AdSkipPolicy(d4, nowMs = { t }, timeZone = kotlinx.datetime.TimeZone.UTC, debugEveryAd = true)
        assertTrue(r.onAdClosed(eligible = true))
        assertFalse(r.onAdClosed(eligible = false))
        r.resetForDebug()
        assertFalse(d4.contains("panel_day_count"))
    }

    @Test
    fun gecisReklamiSikligi() {
        val s = AdRules.InterstitialCounter()
        // 1., 4., 7. kapanışta gösterilir
        val sonuc = (1..7).map { s.onInterstitialDismissed(showAdSkipAfter = true, hostSupportsDialogs = true) }
        assertEquals(listOf(true, false, false, true, false, false, true), sonuc)
        assertEquals(7, s.interstitialAdShownCount)

        // İstenmese de sayaç artar; sıra kaymaz
        val s2 = AdRules.InterstitialCounter()
        assertFalse(s2.onInterstitialDismissed(showAdSkipAfter = false, hostSupportsDialogs = true))
        assertFalse(s2.onInterstitialDismissed(showAdSkipAfter = true, hostSupportsDialogs = true))
        assertFalse(s2.onInterstitialDismissed(showAdSkipAfter = true, hostSupportsDialogs = true))
        assertTrue(s2.onInterstitialDismissed(showAdSkipAfter = true, hostSupportsDialogs = true))
        assertFalse(AdRules.InterstitialCounter().onInterstitialDismissed(showAdSkipAfter = true, hostSupportsDialogs = false))
        // Başlangıç değeri verilebilir (3 → sonraki 4. → göster)
        assertTrue(AdRules.InterstitialCounter(3).onInterstitialDismissed(true, true))
    }
}

class MarathonGuideStoreTest {
    private fun sandik(bitti: Boolean = false, tip: Int = OyunLessonItem.TYPE_CHEST) =
        OyunLessonItemData(type = tip, title = "Maraton", stepIsFinish = bitti, mapFragmentIndex = 12)

    @Test
    fun bayraklar() {
        val prefs = InMemoryKeyValueStore()
        val s = MarathonGuideStore(prefs) { 4 }
        assertFalse(s.isShown())
        assertFalse(s.isPending())
        prefs.putBoolean("marathon_guide_pending", true)
        assertTrue(s.isPending())
        s.clearPending()
        assertFalse(s.isPending())
        prefs.putBoolean("marathon_guide_pending", true)
        s.markShown()
        assertTrue(s.isShown())
        assertFalse(s.isPending())
        assertTrue(prefs.getBoolean("marathon_guide_shown", false))
        // Eski anahtar da "gösterildi" sayılır
        val eski = InMemoryKeyValueStore().apply { putBoolean("cupFragment_guide_shown", true) }
        assertTrue(MarathonGuideStore(eski) { 4 }.isShown())
        assertEquals("GuidePanelPrefs", MarathonGuideStore.PREFS)
    }

    @Test
    fun ilkMaratonDizini() {
        assertEquals(6, MarathonGuideStore(InMemoryKeyValueStore()) { 6 }.firstMarathonLessonIndex())
        // Bulunamazsa 4
        assertEquals(4, MarathonGuideStore(InMemoryKeyValueStore()) { null }.firstMarathonLessonIndex())
    }

    @Test
    fun uygunlukKurali() {
        val s = MarathonGuideStore(InMemoryKeyValueStore()) { 6 }
        assertNull(s.eligibilityRejectReason(1, 6, sandik()))
        assertEquals("not_type_chest(type=0)", s.eligibilityRejectReason(1, 6, sandik(tip = OyunLessonItem.TYPE_LESSON)))
        assertEquals("step_already_finish_before_claim", s.eligibilityRejectReason(1, 6, sandik(bitti = true)))
        assertEquals("wrong_part(part=2 need=1)", s.eligibilityRejectReason(2, 6, sandik()))
        assertEquals(
            "index_mismatch(mapFragmentStepIndex=7 expectedListIdx=6 item.mapFragmentIndex=12)",
            s.eligibilityRejectReason(1, 7, sandik()),
        )
        // Denetim sırası: tip → bitmişlik → bölüm → dizin
        assertEquals("not_type_chest(type=3)", s.eligibilityRejectReason(2, 7, sandik(bitti = true, tip = OyunLessonItem.TYPE_RACE)))
        assertEquals("step_already_finish_before_claim", s.eligibilityRejectReason(2, 7, sandik(bitti = true)))
    }

    @Test
    fun zamanlama() {
        val prefs = InMemoryKeyValueStore()
        val s = MarathonGuideStore(prefs) { 6 }
        assertFalse(s.scheduleIfEligible(1, 6, null))
        assertFalse(s.scheduleIfEligible(2, 6, sandik()))
        assertFalse(s.scheduleIfEligible(1, 6, sandik(bitti = true)))
        assertFalse(s.isPending())
        assertTrue(s.scheduleIfEligible(1, 6, sandik()))
        assertTrue(s.isPending())
        // Daha önce gösterilmiş olması zamanlamayı ENGELLEMEZ (Android ile aynı; gösterim tarafı denetler)
        s.markShown()
        assertTrue(s.scheduleIfEligible(1, 6, sandik()))
        assertTrue(s.isPending())
    }
}

class FirstTutorialShownStoreTest {
    @Test
    fun yerelVeBulutBirlestirme() {
        assertEquals("AppPrefs", FirstTutorialShownStore.PREFS_NAME)
        assertEquals("first_tutorial_shown", FirstTutorialShownStore.KEY)

        // İkisi de yok
        var prefs = InMemoryKeyValueStore()
        var s = FirstTutorialShownStore(prefs)
        assertEquals(FirstTutorialShownStore.Resolution(shown = false, cloudNeedsRepair = false), s.resolveShown(null))
        assertEquals(FirstTutorialShownStore.Resolution(false, false), s.resolveShown(false))
        assertFalse(prefs.contains("first_tutorial_shown"))
        assertFalse(s.readLocal())

        // Bulut true → yerel yükseltilir
        assertEquals(FirstTutorialShownStore.Resolution(true, false), s.resolveShown(true))
        assertTrue(s.readLocal())

        // Yerel true, bulut false/null → gösterilmiş sayılır, bulut onarılmalı; yerel asla ezilmez
        prefs = InMemoryKeyValueStore().apply { putBoolean("first_tutorial_shown", true) }
        s = FirstTutorialShownStore(prefs)
        assertEquals(FirstTutorialShownStore.Resolution(true, true), s.resolveShown(false))
        assertEquals(FirstTutorialShownStore.Resolution(true, true), s.resolveShown(null))
        assertEquals(FirstTutorialShownStore.Resolution(true, false), s.resolveShown(true))
        assertTrue(s.readLocal())
    }

    @Test
    fun isaretlemeVeOnarim() {
        val prefs = InMemoryKeyValueStore()
        val s = FirstTutorialShownStore(prefs)
        val yazilan = mutableListOf<String>()
        // Yerel false iken onarım yapılmaz
        s.repairFirestoreIfLocalShown("u1") { yazilan.add(it) }
        assertTrue(yazilan.isEmpty())
        // Misafir: yalnızca yerel
        s.markShown(null) { yazilan.add(it) }
        assertTrue(s.readLocal())
        assertTrue(yazilan.isEmpty())
        s.markShown("  ") { yazilan.add(it) }
        assertTrue(yazilan.isEmpty())
        // Oturum açık: buluta da yazılır
        s.markShown("u1") { yazilan.add(it) }
        assertEquals(listOf("u1"), yazilan)
        // Onarım: yerel true + oturum varsa yazılır, oturum yoksa yazılmaz
        s.repairFirestoreIfLocalShown("u2") { yazilan.add(it) }
        s.repairFirestoreIfLocalShown(null) { yazilan.add(it) }
        assertEquals(listOf("u1", "u2"), yazilan)
    }
}

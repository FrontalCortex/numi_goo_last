package com.sorobit.shared.profil

import com.sorobit.shared.kupa.rozet.BadgeKind
import com.sorobit.shared.kupa.rozet.BadgeState
import com.sorobit.shared.kupa.rozet.UserBadgeProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Beklenen değerler Android kaynağı (ProfileFragment.kt ve komşu dosyalar) okunarak elle türetildi.
class ProfileFragmentLogicTest {

    private val L = ProfileFragmentLogic

    // ---- loadUserData ----

    @Test
    fun profilKoleksiyonu() {
        assertEquals("users", L.profileCollection(false))
        assertEquals("publicProfiles", L.profileCollection(true))
    }

    @Test
    fun avatarAdi() {
        assertEquals("avatar_ic1", L.avatarDrawableName(1))
        assertEquals("avatar_ic12", L.avatarDrawableName(12))
        assertEquals("non_user", L.avatarDrawableName(0))
        assertEquals("non_user", L.avatarDrawableName(13))
        assertEquals("non_user", L.avatarDrawableName(-1))
    }

    @Test
    fun gorunenAd() {
        assertEquals("Ada", L.displayName("Ada", "Auth", false))
        assertEquals("Auth", L.displayName(null, "Auth", false))
        assertEquals("Auth", L.displayName("  ", "Auth", false))
        // Başkasının profilinde Auth adına düşülmez.
        assertEquals("Kullanıcı", L.displayName("", "Auth", true))
        assertEquals("Kullanıcı", L.displayName(null, null, false))
        assertEquals("Kullanıcı", L.displayName(null, " ", false))
    }

    @Test
    fun kullaniciSatiri_veHedefKimlik() {
        assertEquals("@ADA123 • 2024 YILINDA KATILDI", L.usernameAndDateText("ADA123", 2024))
        assertEquals("2024 YILINDA KATILDI", L.usernameAndDateText("", 2024))
        assertEquals("ADA123", L.followTargetUserId("@ADA123 • 2024 YILINDA KATILDI"))
        // Android davranışı: kimlik yoksa metnin tamamı döner.
        assertEquals("2024 YILINDA KATILDI", L.followTargetUserId("2024 YILINDA KATILDI"))
    }

    @Test
    fun tamamlanmaMetni() {
        assertEquals("%12 tamamlandı", L.completionText(12))
    }

    @Test
    fun toplamSure() {
        assertEquals("0 dakika", L.totalTimeText(0))
        assertEquals("0 dakika", L.totalTimeText(59))
        assertEquals("59 dakika", L.totalTimeText(3599))
        assertEquals("1 saat 0 dakika", L.totalTimeText(3600))
        assertEquals("23 saat 59 dakika", L.totalTimeText(86399))
        assertEquals("1 Gün 0 saat 0 dakika", L.totalTimeText(86400))
        assertEquals("2 Gün 3 saat 4 dakika", L.totalTimeText(2 * 86400L + 3 * 3600 + 4 * 60 + 5))
    }

    @Test
    fun abonelikMetni() {
        assertEquals("PRO", L.subscriptionText("Pro"))
        assertEquals("PRO", L.subscriptionText("PRO"))
        assertEquals("Lite", L.subscriptionText("lite"))
        assertEquals("Free", L.subscriptionText("Premium"))
        assertEquals("Free", L.subscriptionText(null))
    }

    // ---- Kupa kartları ----

    @Test
    fun kupaKartlari() {
        assertEquals(6, L.CUP_CARDS.size)
        assertEquals(
            listOf(
                "addition_abacus_cup", "extraction_abacus_cup", "impact_abacus_cup",
                "blinding_addition_abacus_cup", "blinding_extraction_abacus_cup", "blinding_impact_abacus_cup",
            ),
            L.CUP_CARDS.map { it.field },
        )
        assertEquals(listOf("Toplama", "Çıkarma", "Çarpma", "Toplama", "Çıkarma", "Çarpma"), L.CUP_CARDS.map { it.title })
        assertEquals(listOf(false, false, false, true, true, true), L.CUP_CARDS.map { it.showForbiddenIcon })
        assertEquals("dinosaur_anim.json", L.CUP_CARDS[0].animFile)
        assertEquals("turtle_anim.json", L.CUP_CARDS[5].animFile)
    }

    @Test
    fun kupaSkoru() {
        assertEquals(350, L.cupScore(350L))
        assertEquals(350, L.cupScore(350.9))
        assertEquals(200, L.cupScore(null))
        assertEquals(200, L.cupScore("350"))
    }

    // ---- Rozetler ----

    @Test
    fun eksikVarsayilanlar() {
        assertEquals(10, L.missingBadgeDefaults(emptySet()).size)
        val eksik = L.missingBadgeDefaults(setOf("userDartProgress", "userRocketDailyDayId", "baska"))
        assertEquals(8, eksik.size)
        assertFalse(eksik.containsKey("userDartProgress"))
        assertEquals("", eksik["userFishingStreakPeriodKey"])
        assertEquals(0L, eksik["userGolfProgress"])
        assertTrue(L.missingBadgeDefaults(L.BADGE_PROGRESS_DEFAULTS.keys).isEmpty())
    }

    @Test
    fun takibiBirakma() {
        assertEquals(listOf("users/t/followers/ben", "users/ben/following/t"), L.unfollowDeletePaths("ben", "t"))
        assertTrue(L.followButtonTapUnfollows(true))
        assertFalse(L.followButtonTapUnfollows(false))
        assertEquals(4, L.nudgedFollowersCount(5, -1))
        assertEquals(0, L.nudgedFollowersCount(0, -1))
        assertEquals(6, L.nudgedFollowersCount(5, 1))
        assertEquals(7L, L.longestStreak(7))
        assertEquals(0L, L.longestStreak(null))
        assertEquals("longestStreak", L.FIELD_LONGEST_STREAK)
    }

    @Test
    fun rozetKademesi() {
        // DART: [3,10,20,30,50], adım 5
        assertEquals(0, L.badgeLevel(BadgeKind.DART, UserBadgeProgress(userDartProgress = 2)))
        assertEquals(1, L.badgeLevel(BadgeKind.DART, UserBadgeProgress(userDartProgress = 3)))
        assertEquals(2, L.badgeLevel(BadgeKind.DART, UserBadgeProgress(userDartProgress = 19)))
        assertEquals(5, L.badgeLevel(BadgeKind.DART, UserBadgeProgress(userDartProgress = 50)))
        assertEquals(5, L.badgeLevel(BadgeKind.DART, UserBadgeProgress(userDartProgress = 54)))
        assertEquals(6, L.badgeLevel(BadgeKind.DART, UserBadgeProgress(userDartProgress = 55)))
        assertEquals(0, L.badgeLevel(BadgeKind.DART, UserBadgeProgress(userDartProgress = -4)))
        // Kademesiz rozet
        assertEquals(0, L.badgeLevel(BadgeKind.KARATE, UserBadgeProgress(userKarateProgress = 9)))
        // ROCKET: güncel Android tek kaynak tablosu [3,10,15,25,30], adım 10 (eskiden profilde [3,5,10,15,25])
        assertEquals(1, L.badgeLevel(BadgeKind.ROCKET, UserBadgeProgress(userRocketProgress = 5)))
        assertEquals(5, L.badgeLevel(BadgeKind.ROCKET, UserBadgeProgress(userRocketProgress = 30)))
        assertEquals(6, L.badgeLevel(BadgeKind.ROCKET, UserBadgeProgress(userRocketProgress = 40)))
        // GOLF: güncel tablo [3,5,10,15,20]
        assertEquals(5, L.badgeLevel(BadgeKind.GOLF, UserBadgeProgress(userGolfProgress = 20)))
        // Profil tablosu motorla aynı
        assertEquals(com.sorobit.shared.kupa.rozet.BadgeProgressEngine.levelSpec(BadgeKind.ROCKET), L.levelSpec(BadgeKind.ROCKET))
        // DINO: adım 500
        assertEquals(7, L.badgeLevel(BadgeKind.DINO, UserBadgeProgress(userDinoProgress = 3500)))
    }

    @Test
    fun rozetSiralamaAgirligi() {
        assertEquals(0.0, L.badgeSortingWeight(BadgeKind.DART, UserBadgeProgress(userDartProgress = 2)))
        assertEquals(3.0, L.badgeSortingWeight(BadgeKind.DART, UserBadgeProgress(userDartProgress = 20)))
        // 5. kademeden sonra kesirli: 5 + (52-50)/5
        assertEquals(5.4, L.badgeSortingWeight(BadgeKind.DART, UserBadgeProgress(userDartProgress = 52)))
        assertEquals(0.0, L.badgeSortingWeight(BadgeKind.GOLD, UserBadgeProgress()))
        // TORNADO: [1,3,5,10,15], adım 3
        assertEquals(6.0, L.badgeSortingWeight(BadgeKind.TORNADO, UserBadgeProgress(userTornadoProgress = 18)))
    }

    private fun durum(kind: BadgeKind, unlocked: Boolean) =
        BadgeState(kind = kind, unlocked = unlocked, value = null, showValue = false)

    @Test
    fun rozetSecimi_hepsiKilitli_kilitliOncelikSirasi() {
        val hepsi = BadgeKind.entries.map { durum(it, false) }
        val secilen = L.pickProfileBadges(hepsi, UserBadgeProgress())
        assertEquals(
            listOf(BadgeKind.ROCKET, BadgeKind.GOLF, BadgeKind.FISHING, BadgeKind.DART),
            secilen.map { it.kind },
        )
    }

    @Test
    fun rozetSecimi_acikOlanlarOnce_ilkGrupSabitSirada() {
        val acik = setOf(BadgeKind.KARATE, BadgeKind.GOLD, BadgeKind.DART)
        val hepsi = BadgeKind.entries.map { durum(it, it in acik) }
        val secilen = L.pickProfileBadges(hepsi, UserBadgeProgress(userDartProgress = 3))
        // GOLD ve KARATE ilk grup (bu sırayla), DART ikinci grup, kalan yuva kilitli ROCKET.
        assertEquals(
            listOf(BadgeKind.GOLD, BadgeKind.KARATE, BadgeKind.DART, BadgeKind.ROCKET),
            secilen.map { it.kind },
        )
        assertEquals(listOf(true, true, true, false), secilen.map { it.unlocked })
    }

    @Test
    fun rozetSecimi_ikinciGrupKademeyeGoreAzalan_esitlikteVarsayilanSira() {
        val acik = setOf(BadgeKind.DINO, BadgeKind.GOLF, BadgeKind.DART, BadgeKind.ROCKET, BadgeKind.BOWLING)
        val hepsi = BadgeKind.entries.map { durum(it, it in acik) }
        val ilerleme = UserBadgeProgress(
            userDinoProgress = 500, // kademe 1
            userGolfProgress = 10, // kademe 3 ([3,5,10,15,20])
            userDartProgress = 20, // kademe 3
            userRocketProgress = 30, // kademe 5 ([3,10,15,25,30])
            userBowlingProgress = 5, // kademe 1
        )
        val secilen = L.pickProfileBadges(hepsi, ilerleme)
        // ROCKET(5) > DART(3) = GOLF(3) (varsayılan sırada DART önce) > DINO(1) = BOWLING(1) (DINO önce)
        assertEquals(
            listOf(BadgeKind.ROCKET, BadgeKind.DART, BadgeKind.GOLF, BadgeKind.DINO),
            secilen.map { it.kind },
        )
    }

    @Test
    fun rozetSecimi_kaplumbagaHicGosterilmez() {
        // Android davranışı: TURTLE hiçbir sıralama listesinde yok.
        val yalnizKaplumbaga = listOf(durum(BadgeKind.TURTLE, true))
        assertTrue(L.pickProfileBadges(yalnizKaplumbaga, UserBadgeProgress(userTurtleProgress = 5000)).isEmpty())
        assertTrue(L.pickProfileBadges(listOf(durum(BadgeKind.TURTLE, false)), UserBadgeProgress()).isEmpty())
    }

    @Test
    fun rozetSecimi_yuvaSayisi() {
        val hepsi = BadgeKind.entries.map { durum(it, false) }
        assertEquals(2, L.pickProfileBadges(hepsi, UserBadgeProgress(), 2).size)
        assertEquals(17, L.pickProfileBadges(hepsi, UserBadgeProgress(), 99).size)
    }

    @Test
    fun rozetCizimAyari() {
        val kupa = L.profileBadgeRenderConfig(BadgeKind.CUP)
        assertNull(kupa.baseAsset)
        assertEquals("cup_google_anim.json", kupa.topAsset)
        assertEquals(0f, kupa.topHoldFrame)
        assertEquals(1f, kupa.topScale)
        assertNull(kupa.topBackgroundRes)

        assertEquals("silver_medal_anim.json.json", L.profileBadgeRenderConfig(BadgeKind.SILVER).topAsset)
        assertEquals("bronze_medal_anim.json.json", L.profileBadgeRenderConfig(BadgeKind.BRONZE).topAsset)

        val bowling = L.profileBadgeRenderConfig(BadgeKind.BOWLING)
        assertEquals("daily_tasks_complite_badge2.json", bowling.baseAsset)
        assertEquals(47f, bowling.topHoldFrame)
        assertEquals(0.68f, bowling.topScale)
        assertEquals("bg_badge_circle_frame", bowling.topBackgroundRes)

        val roket = L.profileBadgeRenderConfig(BadgeKind.ROCKET)
        assertEquals("rocket_badge_anim2.json", roket.topAsset)
        assertEquals(60f, roket.topHoldFrame)
        assertEquals(1f, roket.topScale)
        assertNull(roket.topBackgroundRes)

        val dino = L.profileBadgeRenderConfig(BadgeKind.DINO)
        assertEquals(9999f, dino.topHoldFrame)
        assertEquals(0.5f, dino.topScale)
        assertEquals("bg_badge_circle_frame", dino.topBackgroundRes)

        // Her tür için ayar var.
        BadgeKind.entries.forEach { assertTrue(L.profileBadgeRenderConfig(it).topAsset.endsWith(".json")) }
    }

    @Test
    fun kareOrani() {
        assertEquals(0f, L.frameToProgress(10f, 5f, 5f))
        assertEquals(0.5f, L.frameToProgress(60f, 0f, 120f))
        assertEquals(1f, L.frameToProgress(9999f, 0f, 120f))
        assertEquals(0f, L.frameToProgress(-3f, 0f, 120f))
    }

    @Test
    fun rozetSatiri_bosIlerleme() {
        val satir = L.profileBadgeSlots(UserBadgeProgress())
        assertEquals(listOf("ROCKET", "GOLF", "FISHING", "DART"), satir.map { it.kindName })
        assertTrue(satir.none { it.unlocked })
        assertTrue(satir.all { it.baseAsset == "daily_tasks_complite_badge2.json" })
        assertEquals("rocket_badge_anim2.json", satir[0].topAsset)
        assertFalse(satir[0].hasCircleFrame)
    }

    @Test
    fun rozetSatiri_dokumandan() {
        // Doküman yok → boş ilerleme.
        assertEquals(
            listOf("ROCKET", "GOLF", "FISHING", "DART"),
            L.profileBadgeSlotsFromStateDoc(null).map { it.kindName },
        )
        val satir = L.profileBadgeSlotsFromStateDoc(
            mapOf("userDartProgress" to 20L, "userKarateProgress" to 1L),
        )
        assertEquals(listOf("KARATE", "DART", "ROCKET", "GOLF"), satir.map { it.kindName })
        assertEquals(listOf(true, true, false, false), satir.map { it.unlocked })
        assertEquals("", satir[0].baseAsset)
        assertEquals("karate_anim.json", satir[0].topAsset)
    }

    // ---- Boncuk seçici ----

    @Test
    fun sahipOlunanBoncuklar() {
        val hepsi = listOf("SOROBAN", "ANIMAL", "BOWLING", "BALL1")
        assertEquals(listOf("SOROBAN"), L.ownedPreviewBeadTypes(hepsi, emptyMap(), "SOROBAN"))
        assertEquals(
            listOf("SOROBAN", "BOWLING"),
            L.ownedPreviewBeadTypes(hepsi, mapOf("BOWLING" to 2, "ANIMAL" to 0), "BOWLING"),
        )
        // Başlangıç tipi sahip olunanlarda yoksa başa eklenir.
        assertEquals(
            listOf("BALL1", "SOROBAN", "ANIMAL"),
            L.ownedPreviewBeadTypes(hepsi, mapOf("ANIMAL" to 1), "BALL1"),
        )
    }

    @Test
    fun tumBoncukTipleri() {
        assertEquals("SOROBAN", L.ALL_BEAD_TYPE_NAMES.first())
        assertTrue(L.ALL_BEAD_TYPE_NAMES.contains("BOWLING"))
        assertEquals(L.ALL_BEAD_TYPE_NAMES.size, L.ALL_BEAD_TYPE_NAMES.toSet().size)
    }

    @Test
    fun boncukAdimi() {
        assertEquals(-1, L.previewBeadStep(0, 1, true))
        assertEquals(-1, L.previewBeadStep(0, 1, false))
        assertEquals(1, L.previewBeadStep(0, 3, true))
        assertEquals(0, L.previewBeadStep(2, 3, true))
        assertEquals(2, L.previewBeadStep(0, 3, false))
        assertEquals(0, L.previewBeadStep(1, 3, false))
    }

    // ---- Profil düzenleme ----

    @Test
    fun parolaAlaniGorunurlugu() {
        val parola = listOf("password")
        val google = listOf("google.com")
        val ikisi = listOf("google.com", "password")
        assertTrue(EditProfileRules.providerAllowsPasswordChange(parola))
        assertFalse(EditProfileRules.providerAllowsPasswordChange(google))
        assertTrue(EditProfileRules.providerAllowsPasswordChange(ikisi))
        assertTrue(EditProfileRules.providerAllowsPasswordChange(emptyList()))

        assertTrue(EditProfileRules.showPasswordField(parola, "TEACHER", true))
        assertFalse(EditProfileRules.showPasswordField(parola, "STUDENT", true))
        assertTrue(EditProfileRules.showPasswordField(parola, null, true))
        assertFalse(EditProfileRules.showPasswordField(google, "TEACHER", true))
        // Doküman okunamadıysa rol bilinmez; yalnızca sağlayıcıya bakılır.
        assertTrue(EditProfileRules.showPasswordField(parola, "STUDENT", false))
        assertFalse(EditProfileRules.showPasswordField(google, null, false))
    }

    @Test
    fun parolaGuncellemeKosulu() {
        assertTrue(EditProfileRules.shouldUpdatePassword("abc", true))
        assertFalse(EditProfileRules.shouldUpdatePassword("", true))
        assertFalse(EditProfileRules.shouldUpdatePassword("abcdef", false))
        assertEquals("@ADA1", EditProfileRules.usernameFieldText("ADA1"))
    }

    // ---- Hesap ayarları ----

    @Test
    fun odemelerBolumu() {
        assertFalse(AccountSettingsRules.showPayments(null))
        assertFalse(AccountSettingsRules.showPayments("free"))
        assertFalse(AccountSettingsRules.showPayments("Free"))
        assertTrue(AccountSettingsRules.showPayments("Lite"))
        assertTrue(AccountSettingsRules.showPayments("Pro"))
    }

    // ---- Arkadaş arama / takip listesi ----

    @Test
    fun aramaEsikleri() {
        assertTrue(FriendSearchRules.shouldClearResults(""))
        assertTrue(FriendSearchRules.shouldClearResults("a"))
        assertFalse(FriendSearchRules.shouldClearResults("ab"))
        assertFalse(FriendSearchRules.shouldSearch("ab"))
        assertTrue(FriendSearchRules.shouldSearch("abc"))
        assertEquals(20L, FriendSearchRules.pageSize(false))
        assertEquals(10L, FriendSearchRules.pageSize(true))
        assertEquals("ada", FriendSearchRules.queryEnd("ada"))
    }

    @Test
    fun aramaSonuclari() {
        assertEquals(listOf("a", "b", "c"), FriendSearchRules.dedupeUids(listOf("a", "b", "a", "c", "b")))
        assertFalse(FriendSearchRules.hasMoreResults(0))
        assertTrue(FriendSearchRules.hasMoreResults(1))
        assertEquals("3 sonuç gösteriliyor", FriendSearchRules.resultCountText(3))
        assertEquals("Ada takip edildi.", FriendSearchRules.followedToast("Ada"))
        assertEquals("Ada takip edildi", FriendSearchRules.followedBackToast("Ada"))
    }

    @Test
    fun takipListesi() {
        assertEquals("Henüz kimseyi takip etmiyorsun", FriendSearchRules.emptyListText(0))
        assertEquals("Henüz takipçin yok", FriendSearchRules.emptyListText(1))
        assertTrue(FriendSearchRules.showFollowBackButton(1, false))
        assertFalse(FriendSearchRules.showFollowBackButton(1, true))
        assertFalse(FriendSearchRules.showFollowBackButton(0, false))
    }

    @Test
    fun harfAvatari() {
        assertEquals("A", FriendSearchRules.avatarLetter("ada"))
        assertEquals("?", FriendSearchRules.avatarLetter(""))
        assertEquals("#1CB0F6", FriendSearchRules.followAvatarColor(0))
        assertEquals("#CE82FF", FriendSearchRules.followAvatarColor(1))
        assertEquals("#FF4B4B", FriendSearchRules.searchAvatarColor(1))
        assertEquals("#1CB0F6", FriendSearchRules.followAvatarColor(8))
        assertEquals("#8DC63F", FriendSearchRules.searchAvatarColor(15))
    }

    // ---- Geri bildirim ----

    @Test
    fun geriBildirimSiniri() {
        val saat = 60 * 60 * 1000L
        val simdi = 10 * saat

        val bos = FeedbackRules.canSendFeedback("", simdi)
        assertTrue(bos.allowed)
        assertEquals("", bos.prunedTimestamps)

        // Dört yeni kayıt: izin var.
        val dort = (1..4).joinToString(",") { (simdi - it * 1000).toString() }
        assertTrue(FeedbackRules.canSendFeedback(dort, simdi).allowed)

        // Beş yeni kayıt: izin yok.
        val bes = (1..5).joinToString(",") { (simdi - it * 1000).toString() }
        val kapali = FeedbackRules.canSendFeedback(bes, simdi)
        assertFalse(kapali.allowed)
        assertEquals(bes, kapali.prunedTimestamps)

        // Bir saatten eski olanlar atılır; tam bir saat önceki kalır (karşılaştırma ">").
        val karisik = "${simdi - saat - 1},${simdi - saat},bozuk,${simdi - 5}"
        val temiz = FeedbackRules.canSendFeedback(karisik, simdi)
        assertTrue(temiz.allowed)
        assertEquals("${simdi - saat},${simdi - 5}", temiz.prunedTimestamps)
    }

    @Test
    fun geriBildirimKaydi() {
        assertEquals("100", FeedbackRules.recordFeedbackTimestamp("", 100))
        assertEquals("50,100", FeedbackRules.recordFeedbackTimestamp("50", 100))
        assertTrue(FeedbackRules.canSubmit(" a "))
        assertFalse(FeedbackRules.canSubmit("  \n"))
        assertEquals("Gönderilemedi: ağ yok", FeedbackRules.sendFailedToast("ağ yok"))
    }

    @Test
    fun geriBildirimDokumani() {
        val dolu = FeedbackRules.feedbackDoc("uid1", "a@b.c", "merhaba", "1.2", "17.4", "Apple iPhone15,2")
        assertEquals(
            mapOf(
                "userId" to "uid1",
                "email" to "a@b.c",
                "message" to "merhaba",
                "appVersion" to "1.2",
                "androidVersion" to "17.4",
                "deviceModel" to "Apple iPhone15,2",
            ),
            dolu,
        )
        val bos = FeedbackRules.feedbackDoc(null, null, "m", null, "17", "x")
        assertEquals("anonymous", bos["userId"])
        assertEquals("No Email", bos["email"])
        assertEquals("Unknown", bos["appVersion"])
    }
}

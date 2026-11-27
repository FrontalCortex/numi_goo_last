// Android SeasonClock.kt, SeasonLeaderboardRewardPayloads.kt, LessonLeaderboardRepository.kt, RecordFragment.kt ve
// RecordLeaderboardAdapter.kt saf mantığının testleri.
package com.sorobit.shared.kupa.sezon

import com.sorobit.shared.kupa.rozet.BadgeAnimMode
import com.sorobit.shared.kupa.rozet.BadgeLevelUpPayload
import com.sorobit.shared.kupa.rozet.CupPieceRow
import com.sorobit.shared.kupa.rozet.MedalPieceRow
import com.sorobit.shared.kupa.rozet.UserBadgeProgress
import com.sorobit.shared.kupa.sezon.LessonLeaderboardRepository.LeaderboardEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SezonTest {

    private val anchor = 1_787_313_900_000L // 21 Ağustos 2026 12:05:00 UTC
    private val hafta = 604_800_000L
    private val L = LessonLeaderboardRepository

    // ---------------------------------------------------------------- SeasonClock

    @Test
    fun sabitler() {
        assertEquals(anchor, SeasonClock.SEASON_ANCHOR_UTC_MS)
        assertEquals(hafta, SeasonClock.SEASON_DURATION_MS)
        assertEquals(60_000L, SeasonClock.SEASON_POLL_INTERVAL_MS)
    }

    @Test
    fun currentSeason() {
        assertEquals(1, SeasonClock.currentSeason(0L))
        assertEquals(1, SeasonClock.currentSeason(anchor - 1))
        assertEquals(1, SeasonClock.currentSeason(anchor))
        assertEquals(1, SeasonClock.currentSeason(anchor + hafta - 1))
        assertEquals(2, SeasonClock.currentSeason(anchor + hafta))
        assertEquals(3, SeasonClock.currentSeason(anchor + 2 * hafta + 5))
        // 21 Kasım 2026 00:00 UTC = çapadan 91 gün 11 sa 55 dk sonra → 14. sezon
        assertEquals(14, SeasonClock.currentSeason(1_795_219_200_000L))
        assertTrue(SeasonClock.currentSeason() >= 1)
    }

    @Test
    fun millisUntilCurrentSeasonEnds() {
        // Çapadan önce: çapaya kalan süre
        assertEquals(5000L, SeasonClock.millisUntilCurrentSeasonEnds(anchor - 5000))
        assertEquals(hafta, SeasonClock.millisUntilCurrentSeasonEnds(anchor))
        assertEquals(hafta - 1, SeasonClock.millisUntilCurrentSeasonEnds(anchor + 1))
        assertEquals(1L, SeasonClock.millisUntilCurrentSeasonEnds(anchor + hafta - 1))
        assertEquals(hafta, SeasonClock.millisUntilCurrentSeasonEnds(anchor + hafta))
        assertEquals(hafta - 123, SeasonClock.millisUntilCurrentSeasonEnds(anchor + 5 * hafta + 123))
    }

    @Test
    fun sezonDegisimiDinleyicileri() {
        SeasonClock.resetForTest(anchor)
        val olaylar = mutableListOf<Pair<Int, Int>>()
        val dinleyici: (Int, Int) -> Unit = { eski, yeni -> olaylar.add(eski to yeni) }
        val patlayan: (Int, Int) -> Unit = { _, _ -> throw IllegalStateException("dinleyici hatası") }
        SeasonClock.addSeasonChangeListener(patlayan)
        SeasonClock.addSeasonChangeListener(dinleyici)

        assertFalse(SeasonClock.pollSeasonChange(anchor + 10))
        assertEquals(emptyList(), olaylar)
        // Patlayan dinleyici diğerlerini engellemez
        assertTrue(SeasonClock.pollSeasonChange(anchor + hafta))
        assertEquals(listOf(1 to 2), olaylar)
        assertFalse(SeasonClock.pollSeasonChange(anchor + hafta + 60_000))
        // Birden çok sezon atlanırsa tek bildirim (eski → güncel)
        assertTrue(SeasonClock.pollSeasonChange(anchor + 3 * hafta))
        assertEquals(listOf(1 to 2, 2 to 4), olaylar)

        SeasonClock.removeSeasonChangeListener(dinleyici)
        assertTrue(SeasonClock.pollSeasonChange(anchor + 4 * hafta))
        assertEquals(2, olaylar.size)
        SeasonClock.resetForTest(anchor)
    }

    @Test
    fun sunucuSaatiSapmasi() {
        SeasonClock.resetForTest(anchor)
        assertEquals(0L, SeasonClock.serverOffsetMs)
        val olaylar = mutableListOf<Pair<Int, Int>>()
        SeasonClock.addSeasonChangeListener { eski, yeni -> olaylar.add(eski to yeni) }
        var gozlenen: Long? = null
        SeasonClock.serverTimeObserver = { gozlenen = it }

        // Cihaz saati 1 hafta geride: sunucu 2. sezonda, cihaz hâlâ 1. sezonda
        val cihaz = anchor + 100
        val sunucu = anchor + hafta + 100
        assertTrue(SeasonClock.onServerTime(sunucu, deviceNowMs = cihaz))
        assertEquals(hafta, SeasonClock.serverOffsetMs)
        assertEquals(sunucu, gozlenen)
        assertEquals(listOf(1 to 2), olaylar)

        // Akla yatkın olmayan değerler yok sayılır; sapma ve gözlemci değişmez
        gozlenen = null
        assertFalse(SeasonClock.onServerTime(1_000L, deviceNowMs = cihaz))
        assertFalse(SeasonClock.onServerTime(4_000_000_000_001L, deviceNowMs = cihaz))
        assertEquals(hafta, SeasonClock.serverOffsetMs)
        assertNull(gozlenen)

        // Kalıcı sapma geri yüklenir
        SeasonClock.resetForTest(anchor)
        SeasonClock.restoreOffset(2 * hafta)
        assertEquals(2 * hafta, SeasonClock.serverOffsetMs)
        SeasonClock.resetForTest(anchor)
        assertEquals(0L, SeasonClock.serverOffsetMs)
    }

    @Test
    fun sunucuSaatiYanitiVeKisitlama() {
        assertEquals(123L, SeasonClock.parseServerNow(mapOf("serverNowMs" to 123)))
        assertEquals(123L, SeasonClock.parseServerNow(mapOf("serverNowMs" to 123.0)))
        assertNull(SeasonClock.parseServerNow(mapOf("serverNowMs" to "1")))
        assertNull(SeasonClock.parseServerNow(null))
        assertEquals("getSeasonInfo", SeasonClock.CF_GET_SEASON_INFO)
        val saat = SeasonClock.SYNC_MIN_INTERVAL_MS
        assertEquals(3_600_000L, saat)
        // Hiç eşitlenmedi: istenir; eşitleme sürüyorsa istenmez; aralık dolmadıysa istenmez (force hariç)
        assertTrue(SeasonClock.shouldRefreshFromServer(false, false, -1L, 5L))
        assertFalse(SeasonClock.shouldRefreshFromServer(true, true, -1L, 5L))
        assertFalse(SeasonClock.shouldRefreshFromServer(false, false, 1000L, 1000L + saat - 1))
        assertTrue(SeasonClock.shouldRefreshFromServer(false, false, 1000L, 1000L + saat))
        assertTrue(SeasonClock.shouldRefreshFromServer(true, false, 1000L, 1001L))
    }

    // ---------------------------------------------------------------- SeasonLeaderboardRewardPayloads

    @Test
    fun buildForSeason() {
        val p = UserBadgeProgress(
            goldMedalPiece = listOf(MedalPieceRow("A", 2), MedalPieceRow("B", 3), MedalPieceRow("C", 3)),
            silverMedalPiece = listOf(MedalPieceRow("A", 2)),
            bronzeMedalPiece = listOf(MedalPieceRow("A", 3)),
            cupPiece = listOf(CupPieceRow("A", 5, 3), CupPieceRow("B", 0, 3), CupPieceRow("C", 2, 4), CupPieceRow("D", 40, 3)),
        )
        assertEquals(
            listOf(
                // Aynı sezonda birden çok altın olsa da tek kutlama
                BadgeLevelUpPayload(BadgeAnimMode.GOLD_MEDAL, 0, 1, 1),
                BadgeLevelUpPayload(BadgeAnimMode.BRONZE_MEDAL, 0, 1, 1),
                // Kupalar sıraya göre; her kupa ayrı kutlama; sıra en az 1'e çekilir
                BadgeLevelUpPayload(BadgeAnimMode.CUP_GOOGLE, 1, 1, 1),
                BadgeLevelUpPayload(BadgeAnimMode.CUP_GOOGLE, 5, 5, 5),
                BadgeLevelUpPayload(BadgeAnimMode.CUP_GOOGLE, 40, 40, 40),
            ),
            SeasonLeaderboardRewardPayloads.buildForSeason(p, 3),
        )
        assertEquals(
            listOf(
                BadgeLevelUpPayload(BadgeAnimMode.GOLD_MEDAL, 0, 1, 1),
                BadgeLevelUpPayload(BadgeAnimMode.SILVER_MEDAL, 0, 1, 1),
            ),
            SeasonLeaderboardRewardPayloads.buildForSeason(p, 2),
        )
        assertEquals(emptyList(), SeasonLeaderboardRewardPayloads.buildForSeason(p, 9))
        assertEquals(emptyList(), SeasonLeaderboardRewardPayloads.buildForSeason(UserBadgeProgress(), 3))
    }

    // ---------------------------------------------------------------- LessonLeaderboardRepository

    @Test
    fun yollarVeSabitler() {
        assertEquals("part_1_lesson_p1_i04_unite_maratonu_season_14", L.leaderboardDocumentId(1, "p1_i04_unite_maratonu", 14))
        assertEquals("lessonLeaderboards/part_3_lesson_k0_season_2/entries", L.entriesCollectionPath(3, "k0", 2))
        assertEquals("recordScore", L.F_SCORE)
        assertEquals("recordLabel", L.F_LABEL)
        assertEquals("displayName", L.F_NAME)
        assertEquals("updatedAt", L.F_UPDATED)
        assertEquals("titleUnit", L.F_TITLE_UNIT)
        assertEquals("lessonKey", L.META_LESSON_KEY)
        assertEquals("submitLeaderboardScore", L.CF_SUBMIT_LEADERBOARD_SCORE)
        assertEquals(100L, L.LEADERBOARD_LIST_QUERY_LIMIT)
        assertEquals(30, L.AVATAR_QUERY_CHUNK)
        assertEquals("publicProfiles", L.PUBLIC_PROFILES_COLLECTION)
    }

    @Test
    fun firstChestLessonIndexForPart() {
        assertEquals(2, L.firstChestLessonIndexForPart(listOf(0, 1, 2, 0, 2)))
        assertEquals(0, L.firstChestLessonIndexForPart(listOf(2)))
        assertNull(L.firstChestLessonIndexForPart(listOf(0, 1, 3)))
        assertNull(L.firstChestLessonIndexForPart(emptyList()))
    }

    @Test
    fun rankingScore() {
        assertEquals(120, L.rankingScore(120L))
        assertEquals(120, L.rankingScore(120))
        assertEquals(120, L.rankingScore(120.9))
        assertEquals(Int.MIN_VALUE, L.rankingScore(null))
        assertEquals(Int.MIN_VALUE, L.rankingScore("120"))
        // Float tanınmaz (Android'deki when dalları yalnızca Long/Int/Double)
        assertEquals(Int.MIN_VALUE, L.rankingScore(120f))
    }

    @Test
    fun competitionRanks() {
        assertEquals(emptyList(), L.competitionRanksForOrderedScores(emptyList()))
        assertEquals(listOf(1), L.competitionRanksForOrderedScores(listOf(50)))
        assertEquals(listOf(1, 2, 3), L.competitionRanksForOrderedScores(listOf(30, 20, 10)))
        assertEquals(listOf(1, 1, 3, 4, 4, 4, 7), L.competitionRanksForOrderedScores(listOf(100, 100, 90, 80, 80, 80, 10)))
        assertEquals(listOf(1, 1, 1), L.competitionRanksForOrderedScores(listOf(5, 5, 5)))
        // Ham değerlerden: tanınmayan tipler Int.MIN_VALUE olur ve kendi aralarında berabere sayılır
        assertEquals(listOf(1, 2, 2, 4, 4), L.competitionRanksForOrderedDocs(listOf(90L, 80, 80.5, null, "x")))
    }

    @Test
    fun buildSubmitData() {
        assertNull(L.buildSubmitData(false, 1, "k", 100))
        assertNull(L.buildSubmitData(true, 1, "k", 0))
        assertNull(L.buildSubmitData(true, 1, "k", -5))
        // Android güncel: ders anahtarı boşsa gönderilmez
        assertNull(L.buildSubmitData(true, 1, "  ", 100))
        // Ad ve avatar GÖNDERİLMEZ (sunucu publicProfiles'tan çözer)
        assertEquals(
            mapOf<String, Any>("partId" to 1, "lessonKey" to "p1_i04", "recordScore" to 100, "titleUnit" to "Ünite 1"),
            L.buildSubmitData(true, 1, "p1_i04", 100, "  Ünite 1 "),
        )
        // başlık boş → alan eklenmez
        val d = L.buildSubmitData(true, 1, "k", 1, "   ")!!
        assertFalse(d.containsKey("titleUnit"))
        assertFalse(d.containsKey("displayName"))
        assertFalse(d.containsKey("photoUrl"))
        assertFalse(d.containsKey("lessonIndex"))
        // 127 karaktere kırpma (kırpma trim'den SONRA)
        val uzun = "a".repeat(200)
        val d2 = L.buildSubmitData(true, 1, "k", 1, uzun)!!
        assertEquals("a".repeat(127), d2["titleUnit"])
        assertFalse(d2.containsKey("season"))
    }

    @Test
    fun mapLeaderboardEntries() {
        val docs = listOf<Pair<String, Map<String, Any?>>>(
            "u1" to mapOf("recordScore" to 200L, "displayName" to "Ali", "recordLabel" to "01:20"),
            "u2" to mapOf("recordScore" to 200L, "displayName" to "Veli", "recordLabel" to "01:20"),
            "u3" to mapOf("recordScore" to 150L),
            "u4" to mapOf("recordScore" to 150.0, "displayName" to 5L),
        )
        // Önbellek yok: avatar null
        assertEquals(
            listOf(
                LeaderboardEntry("u1", 1, 1, "Ali", "01:20", null),
                LeaderboardEntry("u2", 2, 1, "Veli", "01:20", null),
                LeaderboardEntry("u3", 3, 3, "", "", null),
                LeaderboardEntry("u4", 4, 3, "", "", null),
            ),
            L.mapLeaderboardEntries(docs),
        )
        // Önbellek: "" = profilde avatar yok → null; kendi satırı yerel avatarı kullanır
        val sonuc = L.mapLeaderboardEntries(
            docs,
            avatarCache = mapOf("u1" to "cfg1", "u2" to "", "u3" to "cfg3"),
            ownUid = "u3",
            ownAvatarConfig = "yerel",
        )
        assertEquals(listOf("cfg1", null, "yerel", null), sonuc.map { it.avatarConfig })
        // Yerel avatar boşsa yok sayılır
        assertEquals("cfg3", L.mapLeaderboardEntries(docs, mapOf("u3" to "cfg3"), "u3", "").first { it.userId == "u3" }.avatarConfig)
        assertEquals(emptyList(), L.mapLeaderboardEntries(emptyList()))
    }

    @Test
    fun missingAvatarChunks() {
        val uids = (1..65).map { "u$it" }
        val parcalar = L.missingAvatarChunks(uids, avatarCache = mapOf("u1" to ""), ownUid = "u2", ownAvatarConfig = "x")
        // 65 kimlik − önbellekteki u1 − kendi avatarı yerelde olan u2 = 63 → 30 + 30 + 3
        assertEquals(listOf(30, 30, 3), parcalar.map { it.size })
        assertFalse(parcalar.flatten().contains("u1"))
        assertFalse(parcalar.flatten().contains("u2"))
        // Yerel avatar yoksa kendi satırı da sorgulanır
        assertTrue(L.missingAvatarChunks(listOf("a", "b"), emptyMap(), "a", null).flatten().contains("a"))
        assertEquals(emptyList(), L.missingAvatarChunks(listOf("a"), mapOf("a" to "")))
    }

    // ---------------------------------------------------------------- RecordLogic

    @Test
    fun formatSeasonCountdown() {
        val dk = 60_000L
        var c = RecordLogic.formatSeasonCountdown(2 * 24 * 60 * dk + 5 * 60 * dk + 59 * dk + 59_999)
        assertEquals("record_season_countdown_days_hours", c.stringKey)
        assertEquals(Triple(2, 5, "2 gün 5 sa"), Triple(c.arg1, c.arg2, c.text))
        // Tam 1 gün → gün/saat biçimi
        c = RecordLogic.formatSeasonCountdown(24 * 60 * dk)
        assertEquals("1 gün 0 sa", c.text)
        // 1 günden az → saat/dakika
        c = RecordLogic.formatSeasonCountdown(24 * 60 * dk - 1)
        assertEquals("record_season_countdown_hours_minutes", c.stringKey)
        assertEquals(Triple(23, 59, "23 sa 59 dk"), Triple(c.arg1, c.arg2, c.text))
        assertEquals("0 sa 0 dk", RecordLogic.formatSeasonCountdown(59_999).text)
        assertEquals("0 sa 0 dk", RecordLogic.formatSeasonCountdown(-500_000).text)
        assertEquals("7 gün 0 sa", RecordLogic.formatSeasonCountdown(hafta).text)
    }

    @Test
    fun seasonIndexLabelVeYenidenBaglama() {
        assertEquals("s5", RecordLogic.seasonIndexLabel(5, anchor))
        assertEquals("s1", RecordLogic.seasonIndexLabel(-1, anchor))
        assertEquals("s3", RecordLogic.seasonIndexLabel(0, anchor + 2 * hafta))
        assertTrue(RecordLogic.shouldRebindLeaderboard(3, 2))
        assertFalse(RecordLogic.shouldRebindLeaderboard(3, 3))
    }

    @Test
    fun splitLeaderboard() {
        val a = LeaderboardEntry("a", 1, 1, "A", "1", null)
        val b = LeaderboardEntry("b", 2, 2, "B", "2", null)
        val bos = RecordLogic.splitLeaderboard(emptyList())
        assertTrue(bos.isEmpty)
        assertNull(bos.first)
        assertFalse(bos.showRestList)
        val tek = RecordLogic.splitLeaderboard(listOf(a))
        assertFalse(tek.isEmpty)
        assertEquals(a, tek.first)
        assertFalse(tek.showRestList)
        val iki = RecordLogic.splitLeaderboard(listOf(a, b))
        assertEquals(a, iki.first)
        assertEquals(listOf(b), iki.rest)
        assertTrue(iki.showRestList)
    }

    @Test
    fun satirKurallari() {
        assertEquals("Liderlik tablosu", RecordLogic.displayNameOrFallback("  "))
        assertEquals("Ali", RecordLogic.displayNameOrFallback("Ali"))
        assertTrue(RecordLogic.hasAvatar("cfg"))
        assertFalse(RecordLogic.hasAvatar(" "))
        assertFalse(RecordLogic.hasAvatar(null))

        assertEquals("Liste yüklenemedi.", RecordLogic.leaderboardErrorMessage("ağ hatası", isPermissionDenied = true))
        assertEquals("ağ hatası", RecordLogic.leaderboardErrorMessage("ağ hatası", isPermissionDenied = false))
        assertEquals("Liste yüklenemedi.", RecordLogic.leaderboardErrorMessage(null, isPermissionDenied = false))

        assertEquals("bg_record_rank_gold", RecordLogic.badgeDrawableForRank(1))
        assertEquals("bg_record_rank_badge_purple", RecordLogic.badgeDrawableForRank(2))
        assertEquals("bg_record_rank_badge_yellow", RecordLogic.badgeDrawableForRank(3))
        assertEquals("bg_record_rank_badge_blue", RecordLogic.badgeDrawableForRank(4))
        assertEquals("bg_record_rank_badge_mint", RecordLogic.badgeDrawableForRank(5))
        assertEquals("bg_record_rank_badge_mint", RecordLogic.badgeDrawableForRank(0))

        assertTrue(RecordLogic.isCurrentUser("u1", "u1"))
        assertFalse(RecordLogic.isCurrentUser("u1", "u2"))
        assertFalse(RecordLogic.isCurrentUser("u1", null))
        assertFalse(RecordLogic.isCurrentUser(" ", " "))
        assertEquals(RecordLogic.RowColors("white", "black", "gray"), RecordLogic.rowColors(true))
        assertEquals(RecordLogic.RowColors("panel_background", "white", "button_disabled"), RecordLogic.rowColors(false))
    }
}

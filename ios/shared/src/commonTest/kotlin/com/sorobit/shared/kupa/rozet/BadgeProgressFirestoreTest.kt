// Android BadgeProgressFirestore.kt ve BadgePieceLeaderboardSync.kt saf mantığının testleri.
package com.sorobit.shared.kupa.rozet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BadgeProgressFirestoreTest {

    private val F = BadgeProgressFirestore
    private fun pl(m: BadgeAnimMode, f: Int, t: Int, r: Int) = BadgeLevelUpPayload(m, f, t, r)

    // ---------------------------------------------------------------- yol / alan adları

    @Test
    fun yolVeAlanAdlari() {
        assertEquals("users/abc/badgeProgress/state", F.badgeStateDocumentPath("abc"))
        assertEquals("userDinoProgress", F.cupBadgeProgressField(BadgeAnimMode.DINO))
        assertEquals("userCrocodileProgress", F.cupBadgeProgressField(BadgeAnimMode.CROCODILE))
        assertEquals("userGoatProgress", F.cupBadgeProgressField(BadgeAnimMode.GOAT))
        assertEquals("userEagleProgress", F.cupBadgeProgressField(BadgeAnimMode.EAGLE))
        assertEquals("userFlyProgress", F.cupBadgeProgressField(BadgeAnimMode.FLY))
        assertEquals("userTurtleProgress", F.cupBadgeProgressField(BadgeAnimMode.TURTLE))
        assertNull(F.cupBadgeProgressField(BadgeAnimMode.KARATE))
    }

    // ---------------------------------------------------------------- doküman → model

    @Test
    fun dokumanYoksaVarsayilanIlerleme() {
        assertEquals(UserBadgeProgress(), F.userBadgeProgressFromStateSnapshot(null, "period_5"))
    }

    @Test
    fun tamDokumanEslemesi() {
        val doc = mapOf<String, Any?>(
            "userDartProgress" to 1L,
            "userKarateProgress" to 2L,
            "userGolfProgress" to 3,
            "userBowlingProgress" to 4.9,
            "userFishingProgress" to 5L,
            "userFishingStreak" to 6L,
            "userFishingStreakPeriodKey" to "period_77",
            "userRocketProgress" to 7L,
            "userRocketDailyLessons" to 8L,
            "userRocketDailyDayId" to "2026-325",
            "userTornadoProgress" to 9L,
            "userVolcanoProgress" to 10L,
            "userDinoProgress" to 11L,
            "userCrocodileProgress" to 12L,
            "userTurtleProgress" to 13L,
            "userGoatProgress" to 14L,
            "userEagleProgress" to 15L,
            "userFlyProgress" to 16L,
            "abacusLeaderboardRank" to 17L,
            "goldMedalPiece" to listOf(mapOf("titleUnit" to "Ünite 1", "season" to 3L)),
            "silverMedalPiece" to listOf(listOf("Ünite 2", 4L)),
            "bronzeMedalPiece" to emptyList<Any>(),
            "cupPiece" to listOf(mapOf("titleUnit" to "Ünite 3", "rank" to 12L, "season" to 5L)),
            "pendingLeaderboardRewardSeason" to "14",
        )
        assertEquals(
            UserBadgeProgress(
                userDartProgress = 1,
                userKarateProgress = 2,
                userGolfProgress = 3,
                userBowlingProgress = 4,
                userFishingProgress = 5,
                userFishingStreak = 6,
                userFishingStreakPeriodKey = "period_77",
                userRocketProgress = 7,
                userRocketDailyLessons = 8,
                userRocketDailyDayId = "2026-325",
                userTornadoProgress = 9,
                userVolcanoProgress = 10,
                userDinoProgress = 11,
                userCrocodileProgress = 12,
                userGoatProgress = 14,
                userEagleProgress = 15,
                userFlyProgress = 16,
                userTurtleProgress = 13,
                abacusLeaderboardRank = 17,
                goldMedalPiece = listOf(MedalPieceRow("Ünite 1", 3)),
                silverMedalPiece = listOf(MedalPieceRow("Ünite 2", 4)),
                bronzeMedalPiece = emptyList(),
                cupPiece = listOf(CupPieceRow("Ünite 3", 12, 5)),
                pendingLeaderboardRewardSeason = 14,
            ),
            // userFishingStreak alanı var → eski şema geçişi uygulanmaz (seri kırık olsa bile ham değer kalır)
            F.userBadgeProgressFromStateSnapshot(doc, "period_999"),
        )
    }

    @Test
    fun bosDokumanVeYanlisTipler() {
        // Doküman var ama boş: userFishingStreak alanı yok → geçiş çalışır ama sonuç yine sıfırdır
        assertEquals(UserBadgeProgress(), F.userBadgeProgressFromStateSnapshot(emptyMap(), "period_5"))
        val doc = mapOf<String, Any?>(
            "userDartProgress" to "12", // String sayı sayılmaz (as? Number)
            "userFishingStreak" to null, // alan VAR (null) → geçiş yok
            "userFishingProgress" to 9L,
            "userFishingStreakPeriodKey" to 5L, // String değil → ""
            "abacusLeaderboardRank" to null,
        )
        val p = F.userBadgeProgressFromStateSnapshot(doc, "period_5")
        assertEquals(0, p.userDartProgress)
        assertEquals(9, p.userFishingProgress)
        assertEquals(0, p.userFishingStreak)
        assertEquals("", p.userFishingStreakPeriodKey)
        assertEquals(Int.MAX_VALUE, p.abacusLeaderboardRank)
        assertNull(p.pendingLeaderboardRewardSeason)
    }

    @Test
    fun eskiFishingSemasindanGecis() {
        fun gecis(legacy: Long, key: String, now: String): Pair<Int, Int> {
            val p = F.userBadgeProgressFromStateSnapshot(
                mapOf("userFishingProgress" to legacy, "userFishingStreakPeriodKey" to key),
                now,
            )
            return p.userFishingProgress to p.userFishingStreak
        }
        // Eski alan seri sayısıydı: 7 → ulaşılan kademe 5 (3, 5; sıradaki 15), seri 7
        assertEquals(5 to 7, gecis(7, "period_10", "period_11"))
        // Seri kırıldıysa (en az bir periyot atlanmış) seri 0, kademe kalır
        assertEquals(5 to 0, gecis(7, "period_10", "period_13"))
        assertEquals(0 to 0, gecis(0, "", "period_11"))
        assertEquals(0 to 2, gecis(2, "", "period_11"))
        assertEquals(3 to 3, gecis(3, "period_11", "period_11"))
        // Liste sonrası 5'er adım: 60 → 50, 55, 60
        assertEquals(60 to 60, gecis(60, "period_10", "period_11"))
        assertEquals(60 to 60, gecis(64, "period_10", "period_11").let { it.first to 60 })
        assertEquals(64, gecis(64, "period_10", "period_11").second)
        // Negatif eski değer 0 sayılır
        assertEquals(0 to 0, gecis(-4, "", "period_11"))
    }

    @Test
    fun pendingLeaderboardRewardSeason() {
        assertNull(F.pendingLeaderboardRewardSeasonFromDoc(null))
        assertNull(F.pendingLeaderboardRewardSeasonFromDoc(emptyMap()))
        assertEquals(7, F.pendingLeaderboardRewardSeasonFromDoc(mapOf("pendingLeaderboardRewardSeason" to 7L)))
        assertEquals(7, F.pendingLeaderboardRewardSeasonFromDoc(mapOf("pendingLeaderboardRewardSeason" to 7.0)))
        assertNull(F.pendingLeaderboardRewardSeasonFromDoc(mapOf("pendingLeaderboardRewardSeason" to "yok")))
    }

    @Test
    fun firestoreIntOrNullDonusumleri() {
        assertNull(null.firestoreIntOrNull())
        assertEquals(5, 5.firestoreIntOrNull())
        assertEquals(5, 5L.firestoreIntOrNull())
        assertEquals(5, 5.toShort().firestoreIntOrNull())
        assertEquals(5, 5.toByte().firestoreIntOrNull())
        assertEquals(3, 3.9.firestoreIntOrNull())
        assertEquals(-3, (-3.9).firestoreIntOrNull())
        assertNull(Double.NaN.firestoreIntOrNull())
        assertNull(Double.POSITIVE_INFINITY.firestoreIntOrNull())
        assertEquals(2, 2.5f.firestoreIntOrNull())
        // Float için sonluluk denetimi yok (Android'deki gibi): NaN → 0
        assertEquals(0, Float.NaN.firestoreIntOrNull())
        assertEquals(12, " 12 ".firestoreIntOrNull())
        assertEquals(-4, "-4".firestoreIntOrNull())
        assertNull("1.5".firestoreIntOrNull())
        assertNull("abc".firestoreIntOrNull())
        assertNull("".firestoreIntOrNull())
        assertNull(true.firestoreIntOrNull())
        assertNull(listOf(1).firestoreIntOrNull())
    }

    // ---------------------------------------------------------------- parça listeleri

    @Test
    fun parseMedalPieceList() {
        val S = BadgePieceLeaderboardSync
        assertEquals(emptyList(), S.parseMedalPieceList(null))
        assertEquals(emptyList(), S.parseMedalPieceList("x"))
        assertEquals(emptyList(), S.parseMedalPieceList(mapOf("titleUnit" to "A", "season" to 1L)))
        val liste = listOf(
            mapOf("titleUnit" to "  Ünite A  ", "season" to 3L), // başlık kırpılır
            mapOf("titleUnit" to "null", "season" to 4.0), // "null" metni geçerli başlıktır
            mapOf("titleUnit" to 5L, "season" to "6"), // başlık toString, sezon metinden
            mapOf("titleUnit" to "   ", "season" to 1L), // boş başlık → atılır
            mapOf("season" to 1L), // başlık yok → atılır
            mapOf("titleUnit" to "B"), // sezon yok → atılır
            mapOf("titleUnit" to "B", "season" to "x"), // sezon sayı değil → atılır
            listOf("Ünite C", 7L),
            listOf(" D ", 8, "fazlalık"),
            listOf("E"), // sezon yok → atılır
            listOf(null, 1L), // başlık yok → atılır
            emptyList<Any>(),
            42, // tanınmayan satır türü → atılır
            null,
        )
        assertEquals(
            listOf(
                MedalPieceRow("Ünite A", 3),
                MedalPieceRow("null", 4),
                MedalPieceRow("5", 6),
                MedalPieceRow("Ünite C", 7),
                MedalPieceRow("D", 8),
            ),
            S.parseMedalPieceList(liste),
        )
    }

    @Test
    fun parseCupPieceList() {
        val S = BadgePieceLeaderboardSync
        assertEquals(emptyList(), S.parseCupPieceList(null))
        assertEquals(emptyList(), S.parseCupPieceList(7))
        val liste = listOf(
            mapOf("titleUnit" to " Ünite A ", "rank" to 12L, "season" to 3L),
            mapOf("titleUnit" to "B", "rank" to "4", "season" to 5.0),
            mapOf("titleUnit" to "B", "season" to 5L), // sıra yok → atılır
            mapOf("titleUnit" to "B", "rank" to 5L), // sezon yok → atılır
            mapOf("rank" to 1L, "season" to 5L), // başlık yok → atılır
            mapOf("titleUnit" to "", "rank" to 1L, "season" to 5L), // boş başlık → atılır
            listOf("C", 9L, 2L),
            listOf("D", 9L), // sezon yok → atılır
            listOf("", 9L, 2L), // boş başlık → atılır
            "satır",
        )
        assertEquals(
            listOf(CupPieceRow("Ünite A", 12, 3), CupPieceRow("B", 4, 5), CupPieceRow("C", 9, 2)),
            S.parseCupPieceList(liste),
        )
    }

    // ---------------------------------------------------------------- payload / seri

    @Test
    fun payloadToQueueItem() {
        assertEquals("DINO|400|500|500", F.payloadToQueueItem(pl(BadgeAnimMode.DINO, 400, 500, 500)))
        assertEquals("CUP_GOOGLE|3|3|3", F.payloadToQueueItem(pl(BadgeAnimMode.CUP_GOOGLE, 3, 3, 3)))
    }

    @Test
    fun computeFishingStreakAfterSolve() {
        // Aynı periyotta tekrar çözüm sayılmaz
        assertEquals(4 to "period_5", F.computeFishingStreakAfterSolve(4, "period_5", "period_5"))
        // İlk çözüm
        assertEquals(1 to "period_5", F.computeFishingStreakAfterSolve(9, "", "period_5"))
        // Periyot atlanmış → 1'den başlar
        assertEquals(1 to "period_7", F.computeFishingStreakAfterSolve(4, "period_5", "period_7"))
        // Art arda → +1
        assertEquals(5 to "period_6", F.computeFishingStreakAfterSolve(4, "period_5", "period_6"))
        // Şimdiki periyot öncekinden eski (saat geri alınmış) → else dalı: 1
        assertEquals(1 to "period_5", F.computeFishingStreakAfterSolve(4, "period_6", "period_5"))
        // Bozuk anahtar → kırık sayılır
        assertEquals(1 to "period_6", F.computeFishingStreakAfterSolve(4, "bozuk", "period_6"))
    }

    @Test
    fun resolveLevelUpPayload() {
        val bowling = listOf(5, 10, 20, 50, 100)
        assertEquals(pl(BadgeAnimMode.BOWLING_WITH_BASE, 4, 5, 5), F.resolveLevelUpPayload(BadgeAnimMode.BOWLING_WITH_BASE, 4, 5, bowling))
        assertNull(F.resolveLevelUpPayload(BadgeAnimMode.BOWLING_WITH_BASE, 3, 4, bowling))
        // Birden çok eşik aşılsa da yalnızca ilk eşik için tek payload
        assertEquals(pl(BadgeAnimMode.BOWLING_WITH_BASE, 3, 12, 5), F.resolveLevelUpPayload(BadgeAnimMode.BOWLING_WITH_BASE, 3, 12, bowling))
        assertEquals(pl(BadgeAnimMode.BOWLING_WITH_BASE, 99, 100, 100), F.resolveLevelUpPayload(BadgeAnimMode.BOWLING_WITH_BASE, 99, 100, bowling))
        // Liste sonrası (100+) kademe atlamaları kutlanmaz
        assertNull(F.resolveLevelUpPayload(BadgeAnimMode.BOWLING_WITH_BASE, 100, 105, bowling))
        assertNull(F.resolveLevelUpPayload(BadgeAnimMode.BOWLING_WITH_BASE, 5, 5, bowling))
    }

    @Test
    fun resolveKarateFirstUnlockPayload() {
        assertEquals(pl(BadgeAnimMode.KARATE, 0, 1, 1), F.resolveKarateFirstUnlockPayload(0, 1))
        assertNull(F.resolveKarateFirstUnlockPayload(1, 2))
        assertNull(F.resolveKarateFirstUnlockPayload(0, 2))
        assertNull(F.resolveKarateFirstUnlockPayload(0, 0))
    }

    @Test
    fun rocketVeFishingZincirleri() {
        val R = BadgeAnimMode.ROCKET_WITH_BASE
        assertEquals(emptyList(), F.resolveRocketLevelUpChain(3, 3))
        assertEquals(emptyList(), F.resolveRocketLevelUpChain(10, 3))
        assertEquals(listOf(pl(R, 0, 3, 3)), F.resolveRocketLevelUpChain(0, 3))
        assertEquals(listOf(pl(R, 0, 3, 3), pl(R, 3, 10, 10)), F.resolveRocketLevelUpChain(0, 10))
        assertEquals(listOf(pl(R, 30, 40, 40), pl(R, 40, 50, 50)), F.resolveRocketLevelUpChain(30, 50))
        // Android davranışı: "sonra" değeri bir eşik değilse de aşan eşik payload'a girer (0→5: 3 ve 10).
        assertEquals(listOf(pl(R, 0, 3, 3), pl(R, 3, 10, 10)), F.resolveRocketLevelUpChain(0, 5))

        val FI = BadgeAnimMode.FISHING_WITH_BASE
        assertEquals(emptyList(), F.resolveFishingLevelUpChain(5, 5))
        assertEquals(listOf(pl(FI, 0, 3, 3), pl(FI, 3, 5, 5)), F.resolveFishingLevelUpChain(0, 5))
        assertEquals(listOf(pl(FI, 50, 55, 55)), F.resolveFishingLevelUpChain(50, 55))
    }

    @Test
    fun kupaRozetiZincirleri() {
        val zincirler = listOf(
            BadgeAnimMode.DINO to F::resolveDinoLevelUpChain,
            BadgeAnimMode.CROCODILE to F::resolveCrocodileLevelUpChain,
            BadgeAnimMode.GOAT to F::resolveGoatLevelUpChain,
            BadgeAnimMode.EAGLE to F::resolveEagleLevelUpChain,
            BadgeAnimMode.FLY to F::resolveFlyLevelUpChain,
            BadgeAnimMode.TURTLE to F::resolveTurtleLevelUpChain,
        )
        for ((m, f) in zincirler) {
            assertEquals(emptyList(), f(200, 480))
            assertEquals(emptyList(), f(500, 400))
            assertEquals(emptyList(), f(500, 500))
            assertEquals(listOf(pl(m, 480, 500, 500)), f(480, 505))
            assertEquals(listOf(pl(m, 480, 500, 500), pl(m, 500, 1000, 1000)), f(480, 1000))
            assertEquals(listOf(pl(m, 2400, 2500, 2500), pl(m, 2500, 3000, 3000)), f(2400, 3100))
            assertEquals(f(480, 1000), F.resolveCupBadgeLevelUpChain(m, 480, 1000))
        }
        assertEquals(emptyList(), F.resolveCupBadgeLevelUpChain(BadgeAnimMode.GOLD_MEDAL, 0, 5000))
    }

    // ---------------------------------------------------------------- artırma transaction'ı

    private fun tx(
        snapshot: Map<String, Any?>?,
        dart: Boolean = false,
        bowlingBy: Int = 0,
        karate: Boolean = false,
        rocket: Boolean = false,
        golf: Boolean = false,
        fishing: Boolean = false,
        tornado: Boolean = false,
        volcano: Boolean = false,
        dq: String? = null,
        today: String = "2026-325",
        now: String = "period_100",
    ) = F.computeIncrementBadgeProgressTransaction(
        snapshot, dart, bowlingBy, karate, rocket, golf, fishing, tornado, volcano, dq, today, now,
    )

    @Test
    fun artirmaDokumanYokkenYalnizcaDart() {
        val r = tx(null, dart = true)
        assertEquals(
            mapOf<String, Any>(
                "userRocketProgress" to 0L,
                "userRocketDailyLessons" to 0L,
                "userRocketDailyDayId" to "2026-325",
                "userDartProgress" to 1L,
            ),
            r.updateMap,
        )
        assertEquals(0 to 1, r.beforeDart to r.afterDart)
        assertEquals(0 to 0, r.beforeBowling to r.afterBowling)
        assertEquals("2026-325", r.dayId)
        assertEquals(0, r.daily)
        assertEquals("", r.afterFishingStreakPeriod)
    }

    @Test
    fun artirmaBayrakYokkenYalnizcaRoketAlanlariYazilir() {
        val r = tx(mapOf("userDartProgress" to 9L, "userBowlingProgress" to 4.9, "userGolfProgress" to -5L))
        assertEquals(setOf("userRocketProgress", "userRocketDailyLessons", "userRocketDailyDayId"), r.updateMap.keys)
        assertEquals(9 to 9, r.beforeDart to r.afterDart)
        assertEquals(4 to 4, r.beforeBowling to r.afterBowling) // getLong: 4.9 → 4
        assertEquals(0 to 0, r.beforeGolf to r.afterGolf) // negatif → 0
    }

    @Test
    fun artirmaRoketGunlukSayac() {
        // Aynı gün: 2 → 3 ders → ilk eşik (3) aşılır
        var r = tx(mapOf("userRocketProgress" to 0L, "userRocketDailyLessons" to 2L, "userRocketDailyDayId" to "2026-325"), rocket = true)
        assertEquals(Triple(0, 3, 3), Triple(r.beforeRocket, r.afterRocket, r.daily))
        assertEquals(3L, r.updateMap["userRocketProgress"])
        assertEquals(3L, r.updateMap["userRocketDailyLessons"])
        // Gün değişmiş: sayaç sıfırlanıp 1 olur, kademe düşmez
        r = tx(mapOf("userRocketProgress" to 3L, "userRocketDailyLessons" to 9L, "userRocketDailyDayId" to "2026-324"), rocket = true)
        assertEquals(Triple(3, 3, 1), Triple(r.beforeRocket, r.afterRocket, r.daily))
        assertEquals("2026-325", r.updateMap["userRocketDailyDayId"])
        // 9 → 10 ders, kademe 3 → 10
        r = tx(mapOf("userRocketProgress" to 3L, "userRocketDailyLessons" to 9L, "userRocketDailyDayId" to "2026-325"), rocket = true)
        assertEquals(Triple(3, 10, 10), Triple(r.beforeRocket, r.afterRocket, r.daily))
        // Tek seferde birden çok eşik: kademe 0 iken sayaç 14 → 15: 3, 10, 15
        r = tx(mapOf("userRocketDailyLessons" to 14L, "userRocketDailyDayId" to "2026-325"), rocket = true)
        assertEquals(15, r.afterRocket)
        // Android davranışı: roket bayrağı olmasa da kademe döngüsü çalışır (sayaç 3, kademe 0 → 3)
        r = tx(mapOf("userRocketDailyLessons" to 3L, "userRocketDailyDayId" to "2026-325"))
        assertEquals(Triple(0, 3, 3), Triple(r.beforeRocket, r.afterRocket, r.daily))
        // Liste sonrası 10'ar: kademe 30, sayaç 39 → 40
        r = tx(mapOf("userRocketProgress" to 30L, "userRocketDailyLessons" to 39L, "userRocketDailyDayId" to "2026-325"), rocket = true)
        assertEquals(40, r.afterRocket)
    }

    @Test
    fun artirmaFishingSeri() {
        val snap = mapOf<String, Any?>(
            "userFishingProgress" to 3L, "userFishingStreak" to 4L, "userFishingStreakPeriodKey" to "period_99",
        )
        // Art arda periyot: seri 5, kademe 3 → 5
        var r = tx(snap, fishing = true)
        assertEquals(Triple(3, 5, 5), Triple(r.beforeFishingTier, r.afterFishingTier, r.afterFishingStreak))
        assertEquals("period_100", r.afterFishingStreakPeriod)
        assertEquals(5L, r.updateMap["userFishingProgress"])
        assertEquals(5L, r.updateMap["userFishingStreak"])
        assertEquals("period_100", r.updateMap["userFishingStreakPeriodKey"])
        // Oturum periyodu verilmişse o kullanılır: aynı periyot → değişiklik yok (ama alanlar yine yazılır)
        r = tx(snap, fishing = true, dq = "period_99")
        assertEquals(Triple(3, 3, 4), Triple(r.beforeFishingTier, r.afterFishingTier, r.afterFishingStreak))
        assertEquals("period_99", r.updateMap["userFishingStreakPeriodKey"])
        assertEquals(4L, r.updateMap["userFishingStreak"])
        // Boş oturum periyodu → "şimdi"
        r = tx(snap, fishing = true, dq = "")
        assertEquals(5, r.afterFishingStreak)
        // Periyot atlanmış → seri 1; kademe düşmez
        r = tx(snap, fishing = true, dq = "period_105")
        assertEquals(Triple(3, 3, 1), Triple(r.beforeFishingTier, r.afterFishingTier, r.afterFishingStreak))
        assertEquals("period_105", r.afterFishingStreakPeriod)
        // Bayrak yoksa fishing alanları yazılmaz ve değerler aynen döner
        r = tx(snap)
        assertTrue("userFishingStreak" !in r.updateMap)
        assertEquals(Triple(3, 3, 4), Triple(r.beforeFishingTier, r.afterFishingTier, r.afterFishingStreak))
        assertEquals("period_99", r.afterFishingStreakPeriod)
    }

    @Test
    fun artirmaFishingEskiSema() {
        // userFishingStreak alanı yok: seri eski alandan (7) türetilir; "önce" kademesi ise HAM alan değeridir (7).
        val snap = mapOf<String, Any?>("userFishingProgress" to 7L, "userFishingStreakPeriodKey" to "period_99")
        var r = tx(snap, fishing = true)
        assertEquals(7, r.beforeFishingTier)
        assertEquals(8, r.afterFishingStreak)
        assertEquals(7, r.afterFishingTier) // 7'den sonraki eşik 15; 8 < 15
        assertEquals(7L, r.updateMap["userFishingProgress"])
        assertEquals(8L, r.updateMap["userFishingStreak"])
        // Eski seri "şimdi"ye göre kırıksa 0 sayılır; ardından çözüm kuralı uygulanır
        r = tx(snap, fishing = true, dq = "period_100", now = "period_200")
        assertEquals(1, r.afterFishingStreak) // 0 + art arda (99 → 100) = 1
        // İlk kez: doküman yok → seri 1, kademe 0
        r = tx(null, fishing = true)
        assertEquals(Triple(0, 0, 1), Triple(r.beforeFishingTier, r.afterFishingTier, r.afterFishingStreak))
        // Seri 2 → 3: ilk kademe
        r = tx(mapOf("userFishingStreak" to 2L, "userFishingStreakPeriodKey" to "period_99"), fishing = true)
        assertEquals(Triple(0, 3, 3), Triple(r.beforeFishingTier, r.afterFishingTier, r.afterFishingStreak))
    }

    @Test
    fun artirmaTumBayraklarVePayloadSirasi() {
        BadgeProgressRepository.update(UserBadgeProgress())
        val snap = mapOf<String, Any?>(
            "userDartProgress" to 2L,
            "userBowlingProgress" to 3L,
            "userKarateProgress" to 0L,
            "userRocketProgress" to 0L,
            "userRocketDailyLessons" to 2L,
            "userRocketDailyDayId" to "2026-325",
            "userGolfProgress" to 4L,
            "userFishingProgress" to 0L,
            "userFishingStreak" to 2L,
            "userFishingStreakPeriodKey" to "period_99",
            "userTornadoProgress" to 0L,
            "userVolcanoProgress" to 2L,
        )
        val r = tx(snap, dart = true, bowlingBy = 3, karate = true, rocket = true, golf = true, fishing = true, tornado = true, volcano = true)
        assertEquals(
            mapOf<String, Any>(
                "userRocketProgress" to 3L,
                "userRocketDailyLessons" to 3L,
                "userRocketDailyDayId" to "2026-325",
                "userDartProgress" to 3L,
                "userBowlingProgress" to 6L,
                "userKarateProgress" to 1L,
                "userGolfProgress" to 5L,
                "userTornadoProgress" to 1L,
                "userVolcanoProgress" to 3L,
                "userFishingProgress" to 3L,
                "userFishingStreak" to 3L,
                "userFishingStreakPeriodKey" to "period_100",
            ),
            r.updateMap,
        )
        val payloads = F.finishIncrementBadgeProgress(
            r, incrementDart = true, incrementBowlingBy = 3, incrementKarate = true, incrementGolf = true,
            incrementFishing = true, incrementTornado = true, incrementVolcano = true,
        )
        assertEquals(
            listOf(
                pl(BadgeAnimMode.BOWLING_WITH_BASE, 3, 6, 5),
                pl(BadgeAnimMode.DAILY_ONLY, 2, 3, 3),
                pl(BadgeAnimMode.KARATE, 0, 1, 1),
                pl(BadgeAnimMode.ROCKET_WITH_BASE, 0, 3, 3),
                pl(BadgeAnimMode.GOLF_WITH_BASE, 4, 5, 5),
                pl(BadgeAnimMode.FISHING_WITH_BASE, 0, 3, 3),
                pl(BadgeAnimMode.TORNADO, 0, 1, 1),
                pl(BadgeAnimMode.VOLCANO, 2, 3, 3),
            ),
            payloads,
        )
        // Bellek deposu güncellenmiş olmalı
        assertEquals(
            UserBadgeProgress(
                userDartProgress = 3,
                userBowlingProgress = 6,
                userKarateProgress = 1,
                userGolfProgress = 5,
                userTornadoProgress = 1,
                userVolcanoProgress = 3,
                userFishingProgress = 3,
                userFishingStreak = 3,
                userFishingStreakPeriodKey = "period_100",
                userRocketProgress = 3,
                userRocketDailyLessons = 3,
                userRocketDailyDayId = "2026-325",
            ),
            BadgeProgressRepository.getUserBadgeProgress(),
        )
    }

    @Test
    fun artirmaBayrakYokkenDepoVePayload() {
        BadgeProgressRepository.update(UserBadgeProgress(userDartProgress = 40))
        val r = tx(mapOf("userDartProgress" to 2L, "userRocketDailyLessons" to 1L, "userRocketDailyDayId" to "2026-325"))
        val payloads = F.finishIncrementBadgeProgress(r, incrementDart = false, incrementBowlingBy = 0, incrementKarate = false)
        assertEquals(emptyList(), payloads)
        val p = BadgeProgressRepository.getUserBadgeProgress()
        // Bayraksız alanlara dokunulmaz; roket alanları her zaman eşitlenir
        assertEquals(40, p.userDartProgress)
        assertEquals(Triple(0, 1, "2026-325"), Triple(p.userRocketProgress, p.userRocketDailyLessons, p.userRocketDailyDayId))
        // Eşik aşılmadıysa payload yok
        val r2 = tx(mapOf("userDartProgress" to 3L), dart = true)
        assertEquals(emptyList(), F.finishIncrementBadgeProgress(r2, incrementDart = true, incrementBowlingBy = 0, incrementKarate = false))
        assertEquals(4, BadgeProgressRepository.getUserBadgeProgress().userDartProgress)
    }

    // ---------------------------------------------------------------- kupa → rozet senkronu

    @Test
    fun computeCupBadgeProgressSync() {
        val f = "userDinoProgress"
        assertEquals(CupBadgeSyncResult(480, 505, mapOf(f to 505L)), F.computeCupBadgeProgressSync(f, mapOf(f to 480L), 505))
        assertEquals(CupBadgeSyncResult(480, 480, null), F.computeCupBadgeProgressSync(f, mapOf(f to 480L), 480))
        assertEquals(CupBadgeSyncResult(480, 480, null), F.computeCupBadgeProgressSync(f, mapOf(f to 480L), 100))
        assertEquals(CupBadgeSyncResult(0, 200, mapOf(f to 200L)), F.computeCupBadgeProgressSync(f, null, 200))
        assertEquals(CupBadgeSyncResult(0, 0, null), F.computeCupBadgeProgressSync(f, mapOf(f to -10L), 0))
        assertEquals(CupBadgeSyncResult(0, 0, null), F.computeCupBadgeProgressSync(f, emptyMap(), -5))
        // Senkron sonucu ile zincir: 480 → 505 tek kutlama
        val r = F.computeCupBadgeProgressSync(f, mapOf(f to 480L), 505)
        assertEquals(listOf(pl(BadgeAnimMode.DINO, 480, 500, 500)), F.resolveDinoLevelUpChain(r.before, r.after))
    }
}

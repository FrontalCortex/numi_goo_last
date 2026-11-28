// Sandık (ChestCrystalPolicy / NewChest / ChestResult / kupa ikonu), enerji (EnergyManager /
// ServerEnergy), sunucu ödülleri (ServerRewards) ve cüzdan (UserWalletFirestore) parçalarının
// Android davranışıyla aynı sonucu verdiğini doğrular.
package com.sorobit.shared.oyun

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ChestCrystalPolicyTest {
    @Test
    fun sunucuSonucuGorsele() {
        assertEquals("crystal_blue_blue", ChestCrystalPolicy.FALLBACK_VIDEO)
        assertEquals(ChestRewardOutcome(ChestRewardType.KEY, 3, "key", "3x"), ChestCrystalPolicy.outcomeFromServer("KEY", 3))
        assertEquals(ChestRewardOutcome(ChestRewardType.GOLD, 250, "open_chest", "250 altın"), ChestCrystalPolicy.outcomeFromServer("GOLD", 250))
        // Tanınmayan tür (küçük harf dahil) altın sayılır
        assertEquals(ChestRewardType.GOLD, ChestCrystalPolicy.outcomeFromServer("key", 1).type)
        assertEquals(ChestRewardOutcome(ChestRewardType.GOLD, 0, "open_chest", "0 altın"), ChestCrystalPolicy.outcomeFromServer("", 0))
    }
}

class NewChestLogicTest {
    private fun sonuc(yol: List<String>, tur: String = "GOLD", miktar: Int = 100) =
        ServerRewards.ChestOutcome(yol, yol.lastOrNull() ?: "COMMON", tur, miktar)

    @Test
    fun nadirlikTablosu() {
        assertEquals(listOf("COMMON", "RARE", "EPIC", "LEGENDARY"), ChestRarity.entries.map { it.name })
        assertEquals(listOf("SIRADAN", "ENDER", "DESTANSI", "EFSANEVİ"), ChestRarity.entries.map { it.label })
        assertEquals(
            listOf("new_chest_close_ic1", "new_chest_close_ic2", "new_chest_close_ic3", "new_chest_close_ic4"),
            ChestRarity.entries.map { it.drawableRes },
        )
        assertEquals(
            listOf("new_chest_open_ic1", "new_chest_open_ic2", "new_chest_open_ic3", "new_chest_open_ic4"),
            ChestRarity.entries.map { it.openDrawableRes },
        )
        assertEquals(0xFFFFB300.toInt(), ChestRarity.COMMON.labelColor)
        assertEquals(0xFFFFFFFF.toInt(), ChestRarity.RARE.labelColor)
        assertEquals(
            RarityTheme(0xFF141F23.toInt(), 0xFF263238.toInt(), 0xFFFFD54F.toInt(), 0xFFFFB300.toInt(), 0xFF3A3010.toInt()),
            ChestRarity.COMMON.theme,
        )
        assertEquals(
            RarityTheme(0xFF4AC3F6.toInt(), 0xFF2A9FE5.toInt(), 0xFFBCE5FB.toInt(), 0xFFBCE5FB.toInt(), 0xFF2A9FE5.toInt()),
            ChestRarity.RARE.theme,
        )
        assertEquals(
            RarityTheme(0xFFAB47BC.toInt(), 0xFF8E24AA.toInt(), 0xFFE1BEE7.toInt(), 0xFFE1BEE7.toInt(), 0xFF8E24AA.toInt()),
            ChestRarity.EPIC.theme,
        )
        assertEquals(
            RarityTheme(0xFFFF9800.toInt(), 0xFFF57C00.toInt(), 0xFFFFE0B2.toInt(), 0xFFFFE0B2.toInt(), 0xFFF57C00.toInt()),
            ChestRarity.LEGENDARY.theme,
        )
    }

    @Test
    fun addanNadirlik() {
        assertEquals(ChestRarity.EPIC, ChestRarity.fromNameOrNull("EPIC"))
        assertNull(ChestRarity.fromNameOrNull("epic"))
        assertNull(ChestRarity.fromNameOrNull(null))
        assertEquals(ChestRarity.COMMON, ChestRarity.startRarityFromArgument(null))
        assertEquals(ChestRarity.COMMON, ChestRarity.startRarityFromArgument("YOK"))
        assertEquals(ChestRarity.RARE, ChestRarity.startRarityFromArgument("RARE"))
        // Tanınmayan adlar atlanır
        assertEquals(
            listOf(ChestRarity.RARE, ChestRarity.LEGENDARY),
            NewChestLogic.rarityPathFromServer(listOf("RARE", "MYTHIC", "LEGENDARY")),
        )
    }

    @Test
    fun sabitlerVeSes() {
        assertEquals("start_rarity", NewChestLogic.ARG_START_RARITY)
        assertEquals("ad_nonce", NewChestLogic.ARG_AD_NONCE)
        assertEquals("earned_gold", NewChestLogic.RESULT_EARNED_GOLD)
        assertEquals("earned_key", NewChestLogic.RESULT_EARNED_KEY)
        assertEquals("chest_closed", NewChestLogic.RESULT_CHEST_CLOSED)
        assertEquals(5000L, NewChestLogic.HINT_DELAY_MS)
        assertEquals(1f, NewChestLogic.soundRateForRarity(0))
        assertEquals(1.25f, NewChestLogic.soundRateForRarity(1))
        assertEquals(1.5f, NewChestLogic.soundRateForRarity(2))
        assertEquals(1.75f, NewChestLogic.soundRateForRarity(3))
        // 1..2 aralığına sıkıştırılır
        assertEquals(2f, NewChestLogic.soundRateForRarity(9))
        assertEquals(1f, NewChestLogic.soundRateForRarity(-3))
    }

    @Test
    fun adimGostergesi() {
        // tapCount = 0: ilk daire etkin, diğerleri gelecek
        assertEquals(
            listOf(ChestStepIndicator.ACTIVE, ChestStepIndicator.FUTURE, ChestStepIndicator.FUTURE),
            (0..2).map { NewChestLogic.stepIndicator(it, 0) },
        )
        assertEquals(
            listOf(ChestStepIndicator.DONE, ChestStepIndicator.DONE, ChestStepIndicator.ACTIVE),
            (0..2).map { NewChestLogic.stepIndicator(it, 2) },
        )
        // 3 ve sonrası: hepsi tamam
        assertEquals(List(3) { ChestStepIndicator.DONE }, (0..2).map { NewChestLogic.stepIndicator(it, 3) })
        assertEquals(List(3) { ChestStepIndicator.DONE }, (0..2).map { NewChestLogic.stepIndicator(it, 5) })
    }

    @Test
    fun odulGosterimi() {
        assertEquals(
            ChestRewardDisplay(120, 0, "120", "gold_ic", "shop_coin_ic1"),
            NewChestLogic.rewardDisplay("GOLD", 120, ChestRarity.COMMON),
        )
        assertEquals("shop_coin_ic1", NewChestLogic.rewardDisplay("GOLD", 1, ChestRarity.RARE).coinDrawable)
        assertEquals("shop_coin_ic2", NewChestLogic.rewardDisplay("GOLD", 1, ChestRarity.EPIC).coinDrawable)
        assertEquals("shop_coin_ic3", NewChestLogic.rewardDisplay("GOLD", 1, ChestRarity.LEGENDARY).coinDrawable)
        // Anahtar ödülü: nadirlikten bağımsız anahtar görseli
        assertEquals(
            ChestRewardDisplay(0, 2, "2", "key", "key"),
            NewChestLogic.rewardDisplay("KEY", 2, ChestRarity.LEGENDARY),
        )
        // Tanınmayan tür altın sayılır
        assertEquals(5, NewChestLogic.rewardDisplay("GEM", 5, ChestRarity.COMMON).earnedGold)
    }

    @Test
    fun kapanisYolu() {
        assertEquals(NewChestLogic.CloseRoute.MAP_CHEST_SLIDE, NewChestLogic.closeRoute("map_chest"))
        assertEquals(NewChestLogic.CloseRoute.POP_NAMED, NewChestLogic.closeRoute("mission_chest"))
        assertEquals(NewChestLogic.CloseRoute.POP_NAMED, NewChestLogic.closeRoute("shop_chest"))
        assertEquals(NewChestLogic.CloseRoute.FINISH_OVERLAY, NewChestLogic.closeRoute(null))
        assertEquals(NewChestLogic.CloseRoute.FINISH_OVERLAY, NewChestLogic.closeRoute("baska"))
    }

    @Test
    fun sunucuSonucuGelmedenDokunusYokSayilir() {
        val s = NewChestState()
        assertEquals(ChestTapAction.IGNORED, s.onScreenTapped().action)
        assertEquals(0, s.tapCount)
        assertEquals(ChestRarity.COMMON, s.currentRarity)
    }

    @Test
    fun tamDokunmaAkisi() {
        val s = NewChestState(ChestRarity.COMMON)
        s.applyServerOutcome(sonuc(listOf("COMMON", "RARE", "EPIC"), "GOLD", 300))
        assertTrue(s.outcomeReady)

        // 1. dokunuş: nadirlik aynı → yalnızca zıplama
        var r = s.onScreenTapped()
        assertEquals(ChestTapResult(ChestTapAction.STEP, rarityChanged = false, showOpenHint = false, soundRate = 1f), r)
        assertEquals(1, s.tapCount)
        // 2. dokunuş: RARE'e yükselir
        r = s.onScreenTapped()
        assertEquals(ChestTapResult(ChestTapAction.STEP, rarityChanged = true, showOpenHint = false, soundRate = 1.25f), r)
        assertEquals(ChestRarity.RARE, s.currentRarity)
        // 3. dokunuş: EPIC + "açmak için dokun" ipucu
        r = s.onScreenTapped()
        assertEquals(ChestTapResult(ChestTapAction.STEP, rarityChanged = true, showOpenHint = true, soundRate = 1.5f), r)
        assertEquals(3, s.tapCount)
        // 4. dokunuş: sandık açılır
        r = s.onScreenTapped()
        assertEquals(ChestTapResult(ChestTapAction.OPEN_CHEST, soundRate = 1.5f), r)
        assertEquals(4, s.tapCount)

        val gosterim = s.showReward()
        assertEquals(ChestRewardDisplay(300, 0, "300", "gold_ic", "shop_coin_ic2"), gosterim)
        assertEquals(300, s.earnedGoldAmount)
        assertEquals(0, s.earnedKeyAmount)

        // Ödül animasyonu bitmeden kapanmaz
        assertEquals(ChestTapAction.IGNORED, s.onScreenTapped().action)
        assertEquals(4, s.tapCount)
        s.onRewardAnimationEnd()
        assertTrue(s.canClose)
        assertEquals(ChestTapAction.CLOSE, s.onScreenTapped().action)
        assertEquals(5, s.tapCount)
        // Kapandıktan sonraki dokunuşlar yok sayılır
        assertEquals(ChestTapAction.IGNORED, s.onScreenTapped().action)
        assertEquals(mapOf("earned_gold" to 300, "earned_key" to 0), s.closeResult())
    }

    @Test
    fun kisaYolVeAnahtarOdulu() {
        // Sunucu yolu 3'ten kısaysa eksik adımlarda mevcut nadirlik korunur
        val s = NewChestState(ChestRarity.RARE)
        s.applyServerOutcome(sonuc(listOf("EPIC"), "KEY", 2))
        assertTrue(s.onScreenTapped().rarityChanged)
        assertFalse(s.onScreenTapped().rarityChanged)
        val ucuncu = s.onScreenTapped()
        assertFalse(ucuncu.rarityChanged)
        assertTrue(ucuncu.showOpenHint)
        assertEquals(ChestRarity.EPIC, s.currentRarity)
        assertEquals(ChestTapAction.OPEN_CHEST, s.onScreenTapped().action)
        assertEquals(ChestRewardDisplay(0, 2, "2", "key", "key"), s.showReward())
        assertEquals(mapOf("earned_gold" to 0, "earned_key" to 2), s.closeResult())

        // Boş yol: nadirlik hiç değişmez
        val bos = NewChestState(ChestRarity.LEGENDARY)
        bos.applyServerOutcome(sonuc(emptyList()))
        repeat(3) { assertFalse(bos.onScreenTapped().rarityChanged) }
        assertEquals(ChestRarity.LEGENDARY, bos.currentRarity)
    }
}

class ChestResultLogicTest {
    private fun sandik(
        record: Int? = null,
        cup1: Int? = 2000,
        cup2: Int? = 1000,
        ikon: String = "chest_stars_tier0",
        zihinden: Boolean? = null,
        tip: Int = OyunLessonItem.TYPE_CHEST,
    ) = OyunLessonItemData(type = tip, title = "S", record = record, cupPoint1 = cup1, cupPoint2 = cup2, stepCupIcon = ikon, isBlinding = zihinden)

    @Test
    fun kupaIkonuSecimi() {
        val s = sandik()
        assertEquals("chest_stars_tier0", ChestTypeProgressHelper.resolvedChestIcon(s, 499, 1))
        assertEquals("chest_stars_tier1", ChestTypeProgressHelper.resolvedChestIcon(s, 500, 1))
        assertEquals("chest_stars_tier1", ChestTypeProgressHelper.resolvedChestIcon(s, 999, 1))
        assertEquals("chest_stars_tier2", ChestTypeProgressHelper.resolvedChestIcon(s, 1000, 1))
        assertEquals("chest_stars_tier3", ChestTypeProgressHelper.resolvedChestIcon(s, 2000, 1))
        assertEquals("chest_stars_tier0", ChestTypeProgressHelper.resolvedChestIcon(s, -50, 1))
        // Eşikler yoksa en fazla 1 yıldız
        assertEquals("chest_stars_tier1", ChestTypeProgressHelper.resolvedChestIcon(sandik(cup1 = null, cup2 = null), 99_999, 1))
        // cupPoint2 500'ün altındaysa 2 yıldız 1 yıldızdan önce gelir
        assertEquals("chest_stars_tier2", ChestTypeProgressHelper.resolvedChestIcon(sandik(cup2 = 300), 300, 1))
        // Zihinden sandık: part 6 dışında tek yıldız görseli
        assertEquals("star_on_ic", ChestTypeProgressHelper.resolvedChestIcon(sandik(zihinden = true), 5000, 1))
        assertEquals("chest_stars_tier3", ChestTypeProgressHelper.resolvedChestIcon(sandik(zihinden = true), 5000, 6))
        assertEquals("chest_stars_tier3", ChestTypeProgressHelper.resolvedChestIcon(sandik(zihinden = false), 5000, 1))
    }

    @Test
    fun yildizSayisi() {
        assertEquals(3, ChestTypeProgressHelper.starCountForChestIcon("chest_stars_tier3"))
        assertEquals(2, ChestTypeProgressHelper.starCountForChestIcon("chest_stars_tier2"))
        assertEquals(1, ChestTypeProgressHelper.starCountForChestIcon("chest_stars_tier1"))
        assertEquals(0, ChestTypeProgressHelper.starCountForChestIcon("chest_stars_tier0"))
        assertEquals(0, ChestTypeProgressHelper.starCountForChestIcon("star_on_ic"))
        assertEquals(0, ChestTypeProgressHelper.starCountForChestIcon("baska"))
    }

    @Test
    fun karateIlkUcYildiz() {
        // İlk kez 3 yıldız: evet
        assertTrue(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(sandik(record = 1500, ikon = "chest_stars_tier2"), 2000, 1))
        // Zaten 3 yıldızlı: hayır
        assertFalse(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(sandik(record = 2100, ikon = "chest_stars_tier3"), 2500, 1))
        // Bu koşu yetmiyor: hayır
        assertFalse(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(sandik(record = 1500, ikon = "chest_stars_tier2"), 1999, 1))
        // Eski rekor zaten yeterli ama ikon güncellenmemiş: bu koşu düşük olsa da evet (max alınır)
        assertTrue(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(sandik(record = 2500, ikon = "chest_stars_tier1"), 100, 1))
        // Rekor yokken ilk koşu
        assertTrue(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(sandik(record = null), 2000, 1))
        // Sandık değil
        assertFalse(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(sandik(tip = OyunLessonItem.TYPE_LESSON), 9999, 1))
        // Zihinden sandık (part 6 dışı): yıldız 0 sayılır → hayır; part 6'da evet
        assertFalse(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(sandik(zihinden = true), 9999, 1))
        assertTrue(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(sandik(zihinden = true), 9999, 6))
    }

    @Test
    fun kupaIkonuDegisimi() {
        // Sandık değilse hiçbir şey yapılmaz
        assertNull(ChestResultLogic.changeCupIcon(sandik(tip = OyunLessonItem.TYPE_LESSON), 1))
        // 0 → 2 yıldız
        assertEquals(CupIconChange("chest_stars_tier2", 2), ChestResultLogic.changeCupIcon(sandik(record = 1200), 1))
        // 1 → 3 yıldız
        assertEquals(CupIconChange("chest_stars_tier3", 2), ChestResultLogic.changeCupIcon(sandik(record = 2000, ikon = "chest_stars_tier1"), 1))
        // İkon değişmedi
        assertEquals(CupIconChange("chest_stars_tier1", 0), ChestResultLogic.changeCupIcon(sandik(record = 600, ikon = "chest_stars_tier1"), 1))
        // Rekor yok → 0 kabul edilir
        assertEquals(CupIconChange("chest_stars_tier0", 0), ChestResultLogic.changeCupIcon(sandik(record = null), 1))
        // İkon değişti ama yıldız azalıyor (olmaması gerekir): kazanç 0'a sıkıştırılır
        assertEquals(CupIconChange("chest_stars_tier1", 0), ChestResultLogic.changeCupIcon(sandik(record = 600, ikon = "chest_stars_tier3"), 1))
        // Zihinden sandık: ikon tek yıldıza döner, yıldız kazancı yazılmaz
        assertEquals(CupIconChange("star_on_ic", 0), ChestResultLogic.changeCupIcon(sandik(record = 5000, zihinden = true), 1))
        assertEquals(CupIconChange("chest_stars_tier3", 3), ChestResultLogic.changeCupIcon(sandik(record = 5000, zihinden = true), 6))
    }

    @Test
    fun rekorKirmaGorevi() {
        assertTrue(ChestResultLogic.shouldIncrementChestRecordBreakMission(OyunLessonItem.TYPE_CHEST, 1000, 1001))
        assertFalse(ChestResultLogic.shouldIncrementChestRecordBreakMission(OyunLessonItem.TYPE_CHEST, 1000, 1000))
        // İlk oynayış (rekor yok) rekor kırma sayılmaz
        assertFalse(ChestResultLogic.shouldIncrementChestRecordBreakMission(OyunLessonItem.TYPE_CHEST, null, 5000))
        assertFalse(ChestResultLogic.shouldIncrementChestRecordBreakMission(OyunLessonItem.TYPE_LESSON, 1000, 5000))
        assertEquals("pending_chest_record_break_mission", ChestResultLogic.ARG_PENDING_CHEST_RECORD_BREAK_MISSION)
    }

    @Test
    fun skorTavaniVeEsikler() {
        assertEquals(2000, ChestResultLogic.resolveScoreCap(2000, 1500))
        assertEquals(1500, ChestResultLogic.resolveScoreCap(null, 1500))
        assertEquals(1500, ChestResultLogic.resolveScoreCap(0, 1500))
        assertEquals(1, ChestResultLogic.resolveScoreCap(null, null))
        assertEquals(1, ChestResultLogic.resolveScoreCap(-5, 0))

        assertEquals(900, ChestResultLogic.resolveCupPoint2Threshold(900, 800, 2000))
        assertEquals(800, ChestResultLogic.resolveCupPoint2Threshold(null, 800, 2000))
        assertEquals(800, ChestResultLogic.resolveCupPoint2Threshold(0, 800, 2000))
        assertEquals(1000, ChestResultLogic.resolveCupPoint2Threshold(null, null, 2000))
        assertEquals(1, ChestResultLogic.resolveCupPoint2Threshold(null, 0, 1))
        assertEquals(listOf(500, 1000, 2000), ChestResultLogic.starThresholds(1000, 2000))
    }

    @Test
    fun ilerlemeOraniVeRenk() {
        assertEquals(0f, ChestResultLogic.scoreProgress(-10, 2000))
        assertEquals(0.25f, ChestResultLogic.scoreProgress(500, 2000))
        assertEquals(1f, ChestResultLogic.scoreProgress(2500, 2000))

        // Artarken: ilk yarıda yeşil→sarı, ikinci yarıda sarı→yeşil
        assertEquals(ScoreBarColorBlend("#7CFC00", "#FFFF99", 0.5f), ChestResultLogic.scoreBarColorBlend(500, 400, 2000))
        assertEquals(ScoreBarColorBlend("#7CFC00", "#FFFF99", 1f), ChestResultLogic.scoreBarColorBlend(1000, 1000, 2000))
        assertEquals(ScoreBarColorBlend("#FFFF99", "#7CFC00", 0.5f), ChestResultLogic.scoreBarColorBlend(1500, 1000, 2000))
        // Azalırken: yeşil→koyu kırmızı, oran = doluluk
        assertEquals(ScoreBarColorBlend("#7CFC00", "#990000", 0.25f), ChestResultLogic.scoreBarColorBlend(500, 600, 2000))

        assertEquals(0.25f, ChestResultLogic.starMarkerRatio(500, 2000))
        assertEquals(1f, ChestResultLogic.starMarkerRatio(500, 400))
        assertEquals(0f, ChestResultLogic.starMarkerRatio(-1, 400))

        assertEquals(0.9f, ChestResultLogic.starSoundRate(1))
        assertEquals(1.0f, ChestResultLogic.starSoundRate(2))
        assertEquals(1.12f, ChestResultLogic.starSoundRate(3))
        assertEquals(1.0f, ChestResultLogic.starSoundRate(7))
    }

    @Test
    fun yildizTetikleyici() {
        val t = ChestResultStarTracker(cupPoint2Threshold = 1000, scoreCap = 2000)
        assertEquals(emptyList(), t.onScoreChanged(0, 0))
        assertEquals(emptyList(), t.onScoreChanged(0, 499))
        assertEquals(listOf(1), t.onScoreChanged(499, 500))
        // Aynı eşik ikinci kez tetiklenmez (skor düşüp yeniden çıksa da)
        assertEquals(emptyList(), t.onScoreChanged(400, 600))
        // Tek karede iki eşik birden
        assertEquals(listOf(2, 3), t.onScoreChanged(900, 2100))
        assertEquals(emptyList(), t.onScoreChanged(0, 5000))

        // Tek sıçramada üç yıldız
        val hepsi = ChestResultStarTracker(1000, 2000)
        assertEquals(listOf(1, 2, 3), hepsi.onScoreChanged(0, 2000))

        // Önceki skor zaten eşiğin üstündeyse tetiklenmez; sıfırlama bayrakları temizler
        val ust = ChestResultStarTracker(1000, 2000)
        assertEquals(emptyList(), ust.onScoreChanged(600, 700))
        assertEquals(listOf(1), ust.onScoreChanged(0, 700))
        ust.reset()
        assertEquals(listOf(1), ust.onScoreChanged(0, 700))
    }

    @Test
    fun sureAyristirmaVeBicim() {
        assertEquals(90, ChestResultLogic.parseTimeToSeconds("1:30"))
        assertEquals(90, ChestResultLogic.parseTimeToSeconds(" 01 : 30 "))
        assertEquals(3723, ChestResultLogic.parseTimeToSeconds("1:02:03"))
        assertEquals(0, ChestResultLogic.parseTimeToSeconds("0:00"))
        assertNull(ChestResultLogic.parseTimeToSeconds(""))
        assertNull(ChestResultLogic.parseTimeToSeconds("90"))
        assertNull(ChestResultLogic.parseTimeToSeconds("a:30"))
        assertNull(ChestResultLogic.parseTimeToSeconds("1:b"))
        assertNull(ChestResultLogic.parseTimeToSeconds("1:2:c"))
        assertNull(ChestResultLogic.parseTimeToSeconds("1:2:3:4"))

        assertEquals("0:00", ChestResultLogic.formatSeconds(0))
        assertEquals("0:00", ChestResultLogic.formatSeconds(-7))
        assertEquals("0:09", ChestResultLogic.formatSeconds(9))
        assertEquals("1:30", ChestResultLogic.formatSeconds(90))
        // Saat ayrılmaz: dakika 60'ı aşabilir
        assertEquals("62:03", ChestResultLogic.formatSeconds(3723))
    }

    @Test
    fun ilerlemeAnahtari() {
        assertEquals("2_7_1_Maraton_4", ChestResultLogic.lessonProgressKey(2, 7, 1, "Maraton", 4))
        assertEquals("0_-1_-1_Ders_0", ChestResultLogic.lessonProgressKey(0, null, null, "Ders", 0))
    }
}

private class SahteEnerjiPlatformu(var oturum: Boolean = false) : EnergyPlatform {
    var planlanan: Long? = null
    var iptalSayisi = 0
    val harcamaIstekleri = mutableListOf<Map<String, Any>>()
    var belgeOkuma = 0

    override fun cancelTick() {
        iptalSayisi++
        planlanan = null
    }

    override fun scheduleTick(delayMs: Long) {
        planlanan = delayMs
    }

    override fun isSignedIn(): Boolean = oturum
    override fun callSpendEnergy(payload: Map<String, Any>) {
        harcamaIstekleri.add(payload)
    }

    override fun loadUserDoc() {
        belgeOkuma++
    }
}

class EnergyManagerTest {
    private val t0 = 1_700_000_000_000L
    private val dilim = 600_000L // Free: 10 dk

    private class Kurulum(val prefs: InMemoryKeyValueStore, val platform: SahteEnerjiPlatformu, var simdi: Long) {
        val enerji = EnergyManager(prefs, platform) { simdi }
    }

    private fun kur(oturum: Boolean = false, hazirla: (InMemoryKeyValueStore) -> Unit = {}): Kurulum {
        val prefs = InMemoryKeyValueStore().also(hazirla)
        return Kurulum(prefs, SahteEnerjiPlatformu(oturum), t0)
    }

    @Test
    fun sabitler() {
        assertEquals("energy_prefs", EnergyManager.PREFS_NAME)
        assertEquals("energy_full_time", EnergyManager.KEY_ENERGY_FULL_TIME)
        assertEquals("energy_full_time", EnergyManager.FIELD_ENERGY_FULL_TIME)
        assertEquals("user_plan", EnergyManager.KEY_USER_PLAN)
        assertEquals("user_role", EnergyManager.KEY_USER_ROLE)
        assertEquals("teacher_approved", EnergyManager.KEY_TEACHER_APPROVED)
        assertEquals("energy", EnergyManager.LEGACY_FIELD_ENERGY)
        assertEquals("last_energy_update", EnergyManager.LEGACY_FIELD_LAST_UPDATE)
        assertEquals("current_energy", EnergyManager.LEGACY_KEY_CURRENT_ENERGY)
    }

    @Test
    fun ilkKurulumDoluBaslar() {
        val k = kur()
        assertEquals("Free", k.enerji.getUserPlan())
        assertEquals("", k.enerji.getUserRole())
        assertFalse(k.enerji.isTeacherApproved())
        assertEquals(5, k.enerji.getMaxEnergy())
        assertEquals(10, k.enerji.getEnergyRefreshMinutes())
        assertEquals(dilim, k.enerji.getEnergyRefreshMillis())
        assertEquals(5, k.enerji.getCurrentEnergy())
        assertEquals(0L, k.enerji.getTimeUntilNextEnergy())
        assertEquals(t0, k.prefs.getLong("energy_full_time", -1))
        // Oturum yok: belge okunmaz
        assertEquals(0, k.platform.belgeOkuma)
        // Oturum varsa kurulumda belge istenir
        assertEquals(1, kur(oturum = true).platform.belgeOkuma)
    }

    @Test
    fun harcamaVeYenilenme() {
        val k = kur()
        assertTrue(k.enerji.useEnergy())
        assertEquals(t0 + dilim, k.prefs.getLong("energy_full_time", -1))
        assertEquals(4, k.enerji.getCurrentEnergy())
        // Tam sınırda: bir dilim daha beklenir
        assertEquals(dilim, k.enerji.getTimeUntilNextEnergy())
        assertEquals(dilim, k.platform.planlanan)
        // Oturum yokken sunucuya bildirilmez
        assertTrue(k.platform.harcamaIstekleri.isEmpty())

        // 1 ms sonra hâlâ 4 (tavan yuvarlama), kalan süre 1 ms azalır
        k.simdi = t0 + 1
        assertEquals(4, k.enerji.getCurrentEnergy())
        assertEquals(dilim - 1, k.enerji.getTimeUntilNextEnergy())
        // Dolmasına 1 ms kala hâlâ 4
        k.simdi = t0 + dilim - 1
        assertEquals(4, k.enerji.getCurrentEnergy())
        assertEquals(1L, k.enerji.getTimeUntilNextEnergy())
        // Tam dolduğunda 5
        k.simdi = t0 + dilim
        assertEquals(5, k.enerji.getCurrentEnergy())
        assertEquals(0L, k.enerji.getTimeUntilNextEnergy())
    }

    @Test
    fun ardisikHarcamalarHedefZamaniUzatir() {
        val k = kur()
        assertTrue(k.enerji.useEnergy(2))
        assertEquals(3, k.enerji.getCurrentEnergy())
        // 4 dk sonra bir daha: hedef zaman mevcut hedefin üstüne eklenir (kalan süre korunur)
        k.simdi = t0 + 240_000
        assertTrue(k.enerji.useEnergy())
        assertEquals(t0 + 3 * dilim, k.prefs.getLong("energy_full_time", -1))
        assertEquals(2, k.enerji.getCurrentEnergy())
        assertEquals(dilim - 240_000, k.enerji.getTimeUntilNextEnergy())
        // Yetmeyen harcama reddedilir ve durum değişmez
        assertFalse(k.enerji.useEnergy(3))
        assertEquals(t0 + 3 * dilim, k.prefs.getLong("energy_full_time", -1))
        assertTrue(k.enerji.hasEnoughEnergy(2))
        assertFalse(k.enerji.hasEnoughEnergy(3))
        // Kalanı da harca: 0
        assertTrue(k.enerji.useEnergy(2))
        assertEquals(0, k.enerji.getCurrentEnergy())
        assertFalse(k.enerji.useEnergy())
        assertFalse(k.enerji.hasEnoughEnergy())
        // 0 ve negatif miktar her zaman "başarılı" ve etkisizdir
        assertTrue(k.enerji.useEnergy(0))
        assertTrue(k.enerji.useEnergy(-1))
        assertEquals(t0 + 5 * dilim, k.prefs.getLong("energy_full_time", -1))
    }

    @Test
    fun oturumAcikkenHarcamaSunucuyaBildirilir() {
        val k = kur(oturum = true)
        assertTrue(k.enerji.useEnergy(2))
        assertEquals(listOf(mapOf<String, Any>("amount" to 2)), k.platform.harcamaIstekleri)
        // Sunucu yetkili değeri döndürür (Firestore sayıları Long/Double gelebilir)
        k.enerji.onSpendEnergyResult(mapOf("energyFullTime" to (t0 + dilim).toDouble()))
        assertEquals(t0 + dilim, k.prefs.getLong("energy_full_time", -1))
        assertEquals(4, k.enerji.getCurrentEnergy())
        // Geçersiz yanıtlar yok sayılır
        k.enerji.onSpendEnergyResult(null)
        k.enerji.onSpendEnergyResult("x")
        k.enerji.onSpendEnergyResult(mapOf("baska" to 1))
        assertEquals(t0 + dilim, k.prefs.getLong("energy_full_time", -1))
        // Sunucu reddederse belge yeniden okunur
        val once = k.platform.belgeOkuma
        k.enerji.onSpendEnergyFailed()
        assertEquals(once + 1, k.platform.belgeOkuma)
    }

    @Test
    fun litePlani() {
        val k = kur { it.putString("user_plan", "Lite") }
        assertEquals(10, k.enerji.getMaxEnergy())
        assertEquals(7, k.enerji.getEnergyRefreshMinutes())
        assertEquals(420_000L, k.enerji.getEnergyRefreshMillis())
        assertFalse(k.enerji.isInfiniteEnergy())
        assertTrue(k.enerji.useEnergy(10))
        assertEquals(0, k.enerji.getCurrentEnergy())
        assertEquals(t0 + 10 * 420_000L, k.prefs.getLong("energy_full_time", -1))
        k.simdi = t0 + 420_000L
        assertEquals(1, k.enerji.getCurrentEnergy())
    }

    @Test
    fun sinirsizPlanlar() {
        for (plan in listOf("Pro", "Premium")) {
            val k = kur { it.putString("user_plan", plan) }
            assertTrue(k.enerji.isInfiniteEnergy(), plan)
            assertEquals(5, k.enerji.getMaxEnergy())
            // Harcama başarılı ama hedef zaman değişmez, sunucuya bildirilmez
            assertTrue(k.enerji.useEnergy(50))
            assertEquals(t0, k.prefs.getLong("energy_full_time", -1))
            assertTrue(k.enerji.hasEnoughEnergy(999))
        }
        // Plan adı büyük/küçük harfe duyarlıdır
        assertFalse(kur { it.putString("user_plan", "pro") }.enerji.isInfiniteEnergy())
        // setUserPlan kalıcıdır
        val k = kur()
        k.enerji.setUserPlan("Pro")
        assertEquals("Pro", k.prefs.getString("user_plan"))
        assertTrue(k.enerji.isInfiniteEnergy())
    }

    @Test
    fun ogretmenKurallari() {
        // Onaysız öğretmen: plan ne olursa olsun enerji 0
        val onaysiz = kur { it.putString("user_plan", "Pro") }
        onaysiz.enerji.setUserRoleApproval("TEACHER", false)
        assertTrue(onaysiz.enerji.isEnergyBlocked())
        assertFalse(onaysiz.enerji.isInfiniteEnergy())
        assertEquals(0, onaysiz.enerji.getCurrentEnergy())
        assertFalse(onaysiz.enerji.useEnergy())
        assertFalse(onaysiz.enerji.hasEnoughEnergy())
        // Miktar 0 ise engelli hesapta bile true (denetim sırası)
        assertTrue(onaysiz.enerji.useEnergy(0))
        assertEquals("TEACHER", onaysiz.prefs.getString("user_role"))

        // Onaylı öğretmen: Free planda bile sınırsız
        val onayli = kur()
        onayli.enerji.setUserRoleApproval("TEACHER", true)
        assertFalse(onayli.enerji.isEnergyBlocked())
        assertTrue(onayli.enerji.isInfiniteEnergy())
        assertTrue(onayli.enerji.useEnergy(99))

        // Öğrenci rolünde onay bayrağı açıksa da sınırsız (kural yalnızca bayrağa bakar)
        val ogrenci = kur()
        ogrenci.enerji.setUserRoleApproval("STUDENT", true)
        assertTrue(ogrenci.enerji.isInfiniteEnergy())
        ogrenci.enerji.setUserRoleApproval("STUDENT", false)
        assertFalse(ogrenci.enerji.isInfiniteEnergy())
        assertFalse(ogrenci.enerji.isEnergyBlocked())
    }

    @Test
    fun hedefZamanUstSiniraKirpilir() {
        val k = kur { it.putLong("energy_full_time", t0 + 100 * 600_000L) }
        // En fazla max * dilim ileride olabilir
        assertEquals(0, k.enerji.getCurrentEnergy())
        assertEquals(t0 + 5 * dilim, k.prefs.getLong("energy_full_time", -1))
        // Tam sınırdaki değer kırpılmaz
        val sinir = kur { it.putLong("energy_full_time", t0 + 5 * 600_000L) }
        assertEquals(0, sinir.enerji.getCurrentEnergy())
        assertEquals(t0 + 5 * dilim, sinir.prefs.getLong("energy_full_time", -1))
    }

    @Test
    fun yerelEnerjiEkleme() {
        // Dolmasına 1.100.000 ms var → eksik = tavan(1,83) = 2 → enerji 3
        val k = kur { it.putLong("energy_full_time", t0 + 1_100_000L) }
        assertEquals(3, k.enerji.getCurrentEnergy())
        k.enerji.addEnergy(1)
        // Hedef bir dilim geri çekilir; kalan saniyeler korunur
        assertEquals(t0 + 500_000L, k.prefs.getLong("energy_full_time", -1))
        assertEquals(4, k.enerji.getCurrentEnergy())
        assertEquals(500_000L, k.enerji.getTimeUntilNextEnergy())
        // Fazlası "şimdi"ye sıkıştırılır
        k.enerji.addEnergy(5)
        assertEquals(t0, k.prefs.getLong("energy_full_time", -1))
        assertEquals(5, k.enerji.getCurrentEnergy())
        // Doluyken ve geçersiz miktarda etkisiz
        k.enerji.addEnergy(1)
        k.enerji.addEnergy(0)
        assertEquals(t0, k.prefs.getLong("energy_full_time", -1))
        // Sınırsız hesapta etkisiz
        val pro = kur {
            it.putString("user_plan", "Pro")
            it.putLong("energy_full_time", t0 + 1_100_000L)
        }
        pro.enerji.addEnergy(1)
        assertEquals(t0 + 1_100_000L, pro.prefs.getLong("energy_full_time", -1))
    }

    @Test
    fun eskiVeridenTuretme() {
        val k = kur()
        // Veri yok → şimdi dolu
        assertEquals(t0, k.enerji.deriveFullTimeFromLegacy(null, null))
        assertEquals(t0, k.enerji.deriveFullTimeFromLegacy(null, t0 - 5))
        // 3 enerji, 250 sn önce: kazanım yok, 2 eksik → sonraki tıka 350 sn + 1 dilim
        assertEquals(t0 + 350_000L + dilim, k.enerji.deriveFullTimeFromLegacy(3, t0 - 250_000L))
        // 3 enerji, 1.300 sn önce: 2 kazanılmış → dolu
        assertEquals(t0, k.enerji.deriveFullTimeFromLegacy(3, t0 - 1_300_000L))
        // 1 enerji, zaman bilinmiyor: 4 eksik → 4 dilim
        assertEquals(t0 + 4 * dilim, k.enerji.deriveFullTimeFromLegacy(1, null))
        // 0 enerji, 700 sn önce: 1 kazanılmış, 4 eksik, sonraki tıka 500 sn
        assertEquals(t0 + 500_000L + 3 * dilim, k.enerji.deriveFullTimeFromLegacy(0, t0 - 700_000L))
        // Üst sınırı aşan eski değer dolu sayılır
        assertEquals(t0, k.enerji.deriveFullTimeFromLegacy(9, t0))
    }

    @Test
    fun yerelGecis() {
        // Eski yerel veri varsa dönüştürülür
        val eski = kur {
            it.putInt("current_energy", 3)
            it.putLong("last_energy_update", t0 - 250_000L)
        }
        assertEquals(t0 + 950_000L, eski.prefs.getLong("energy_full_time", -1))
        assertEquals(3, eski.enerji.getCurrentEnergy())
        // Eksik eski veri (zaman yok) → dolu başlat
        val eksik = kur { it.putInt("current_energy", 2) }
        assertEquals(t0, eksik.prefs.getLong("energy_full_time", -1))
        // Zaten yeni biçim varsa dokunulmaz
        val yeni = kur {
            it.putLong("energy_full_time", t0 + 123L)
            it.putInt("current_energy", 0)
            it.putLong("last_energy_update", t0)
        }
        assertEquals(t0 + 123L, yeni.prefs.getLong("energy_full_time", -1))
    }

    @Test
    fun firestoreBelgesininBenimsenmesi() {
        val k = kur(oturum = true)
        k.enerji.useEnergy()
        // Belge yok: yerel değer korunur, zamanlayıcı yenilenir
        k.enerji.onUserDocLoaded(exists = false, serverFullTime = t0 + 3 * dilim, legacyEnergy = null, legacyLastUpdate = null)
        assertEquals(t0 + dilim, k.prefs.getLong("energy_full_time", -1))
        // Yeni alan var: yerelin üzerine yazılır
        k.enerji.onUserDocLoaded(true, t0 + 2 * dilim, legacyEnergy = 5, legacyLastUpdate = t0)
        assertEquals(t0 + 2 * dilim, k.prefs.getLong("energy_full_time", -1))
        assertEquals(3, k.enerji.getCurrentEnergy())
        // Yalnızca eski alanlar var: dönüştürülür (2 enerji, şimdi → 3 eksik → 3 dilim)
        k.enerji.onUserDocLoaded(true, null, legacyEnergy = 2, legacyLastUpdate = t0)
        assertEquals(t0 + 3 * dilim, k.prefs.getLong("energy_full_time", -1))
        assertEquals(2, k.enerji.getCurrentEnergy())
        // Hiç alan yok: dolu
        k.enerji.onUserDocLoaded(true, null, null, null)
        assertEquals(5, k.enerji.getCurrentEnergy())
        // Okuma hatası: değer değişmez
        k.enerji.useEnergy()
        k.enerji.onUserDocLoadFailed()
        assertEquals(4, k.enerji.getCurrentEnergy())
    }

    @Test
    fun geriCagriVeZamanlayici() {
        val k = kur()
        val gelen = mutableListOf<Int>()
        k.enerji.setEnergyUpdateCallback { gelen.add(it) }
        // Ayarlanır ayarlanmaz çağrılır; doluyken tık planlanmaz
        assertEquals(listOf(5), gelen)
        assertNull(k.platform.planlanan)
        k.enerji.useEnergy()
        assertEquals(listOf(5, 4), gelen)
        assertEquals(dilim, k.platform.planlanan)
        // Tık geldiğinde güncel değer bildirilir ve sonraki tık planlanır
        k.simdi = t0 + dilim
        k.enerji.onTimerTick()
        assertEquals(listOf(5, 4, 5), gelen)
        assertNull(k.platform.planlanan)
        k.enerji.useEnergy(2)
        k.simdi += dilim
        k.enerji.onTimerTick()
        assertEquals(4, gelen.last())
        assertEquals(dilim, k.platform.planlanan)
        // Plan/rol değişimi ve sunucu değeri de bildirir
        val once = gelen.size
        k.enerji.setUserPlan("Lite")
        k.enerji.setUserRoleApproval("STUDENT", false)
        k.enerji.adoptServerFullTime(k.simdi)
        assertEquals(once + 3, gelen.size)
        assertEquals(10, gelen.last())
        val iptal = k.platform.iptalSayisi
        k.enerji.destroy()
        assertEquals(iptal + 1, k.platform.iptalSayisi)
    }
}

class ServerEnergyTest {
    @Test
    fun adlarVeGovdeler() {
        assertEquals("claimAdEnergy", ServerEnergy.FUNCTION_CLAIM_AD_ENERGY)
        assertEquals("buyEnergyWithKeys", ServerEnergy.FUNCTION_BUY_ENERGY_WITH_KEYS)
        assertEquals("spendEnergy", ServerEnergy.FUNCTION_SPEND_ENERGY)
        assertEquals(mapOf<String, Any>("adNonce" to "n-1"), ServerEnergy.claimAdEnergyPayload("n-1"))
        assertEquals(emptyMap<String, Any>(), ServerEnergy.buyWithKeysPayload())
        assertEquals(mapOf<String, Any>("amount" to 3), ServerEnergy.spendEnergyPayload(3))
    }

    @Test
    fun yanitAyristirma() {
        assertEquals(123L, ServerEnergy.readFullTime(mapOf("energyFullTime" to 123L)))
        assertEquals(123L, ServerEnergy.readFullTime(mapOf("energyFullTime" to 123)))
        assertEquals(123L, ServerEnergy.readFullTime(mapOf("energyFullTime" to 123.0)))
        assertNull(ServerEnergy.readFullTime(mapOf("energyFullTime" to "123")))
        assertNull(ServerEnergy.readFullTime(mapOf("x" to 1)))
        assertNull(ServerEnergy.readFullTime(null))
        assertNull(ServerEnergy.readFullTime(listOf(1)))
        assertEquals(55L, ServerEnergy.parseClaimAdEnergyResult(mapOf("energyFullTime" to 55L)))
        assertNull(ServerEnergy.parseClaimAdEnergyResult(emptyMap<String, Any>()))

        assertEquals(
            ServerEnergy.BuyWithKeysResult(900L, 4),
            ServerEnergy.parseBuyWithKeysResult(mapOf("energyFullTime" to 900L, "keys" to 4L)),
        )
        // İki alan da gerekir
        assertNull(ServerEnergy.parseBuyWithKeysResult(mapOf("energyFullTime" to 900L)))
        assertNull(ServerEnergy.parseBuyWithKeysResult(mapOf("keys" to 4L)))
        assertNull(ServerEnergy.parseBuyWithKeysResult(null))
    }
}

class ServerRewardsTest {
    @Test
    fun adlarVeGovdeler() {
        assertEquals("openChest", ServerRewards.FUNCTION_OPEN_CHEST)
        assertEquals("openCrystalReward", ServerRewards.FUNCTION_OPEN_CRYSTAL_REWARD)
        assertEquals(10, ServerRewards.AD_VERIFY_MAX_ATTEMPTS)
        assertEquals(1500L, ServerRewards.AD_VERIFY_RETRY_DELAY_MS)
        assertEquals(mapOf<String, Any>("startRarity" to "RARE"), ServerRewards.openChestPayload("RARE"))
        assertEquals(mapOf<String, Any>("startRarity" to "COMMON", "adNonce" to "n"), ServerRewards.openChestPayload("COMMON", "n"))
        assertEquals(emptyMap<String, Any>(), ServerRewards.openCrystalPayload())
    }

    @Test
    fun sandikYanitiAyristirma() {
        assertEquals(
            ServerRewards.ChestOutcome(listOf("COMMON", "RARE"), "RARE", "KEY", 2),
            ServerRewards.parseChestOutcome(
                mapOf("rarityPath" to listOf("COMMON", 5, "RARE", null), "finalRarity" to "RARE", "rewardType" to "KEY", "rewardAmount" to 2L),
                "COMMON",
            ),
        )
        // Eksik alanlar: boş yol, başlangıç nadirliği, GOLD, 0
        assertEquals(
            ServerRewards.ChestOutcome(emptyList(), "EPIC", "GOLD", 0),
            ServerRewards.parseChestOutcome(emptyMap<String, Any>(), "EPIC"),
        )
        assertEquals(7, ServerRewards.parseChestOutcome(mapOf("rewardAmount" to 7.9), "COMMON")?.rewardAmount)
        assertNull(ServerRewards.parseChestOutcome(null, "COMMON"))
        assertNull(ServerRewards.parseChestOutcome("x", "COMMON"))
    }

    @Test
    fun kristalYanitiAyristirma() {
        assertEquals(
            ServerRewards.CrystalOutcome("KEY", 1),
            ServerRewards.parseCrystalOutcome(mapOf("videoName" to "crystal_red_yellow", "rewardType" to "KEY", "rewardAmount" to 1)),
        )
        assertEquals(
            ServerRewards.CrystalOutcome("GOLD", 0),
            ServerRewards.parseCrystalOutcome(emptyMap<String, Any>()),
        )
        assertNull(ServerRewards.parseCrystalOutcome(null))
    }

    @Test
    fun kupaYoluSandigi() {
        assertEquals("claimCupPathChest", ServerRewards.FUNCTION_CLAIM_CUP_PATH_CHEST)
        assertEquals(
            mapOf<String, Any>("cupField" to "addition_abacus_cup", "milestone" to 500),
            ServerRewards.claimCupPathChestPayload("addition_abacus_cup", 500),
        )
        assertEquals(
            ServerRewards.ChestOutcome(listOf("COMMON", "RARE"), "RARE", "KEY", 2),
            ServerRewards.parseCupPathChestOutcome(
                mapOf("rarityPath" to listOf("COMMON", 5, "RARE"), "finalRarity" to "RARE", "rewardType" to "KEY", "rewardAmount" to 2L),
            ),
        )
        // Eksik alanlar: boş yol, COMMON, GOLD, 0
        assertEquals(
            ServerRewards.ChestOutcome(emptyList(), "COMMON", "GOLD", 0),
            ServerRewards.parseCupPathChestOutcome(emptyMap<String, Any>()),
        )
        assertNull(ServerRewards.parseCupPathChestOutcome(null))
    }

    @Test
    fun yenidenDenemeKurali() {
        // Yalnızca UNAVAILABLE hatasında ve 10 yeniden denemeye kadar (deneme 0..9 → yeniden dene)
        assertTrue(ServerRewards.shouldRetryOpenChest(isUnavailable = true, attempt = 0))
        assertTrue(ServerRewards.shouldRetryOpenChest(isUnavailable = true, attempt = 9))
        assertFalse(ServerRewards.shouldRetryOpenChest(isUnavailable = true, attempt = 10))
        assertFalse(ServerRewards.shouldRetryOpenChest(isUnavailable = false, attempt = 0))
    }

    private class SahteCagirici : ServerRewards.ChestCaller {
        val cagrilar = mutableListOf<Pair<String, String?>>()
        val basari = mutableListOf<(ServerRewards.ChestOutcome) -> Unit>()
        val hata = mutableListOf<(Exception) -> Unit>()

        override fun openChestAttempt(
            startRarity: String,
            adNonce: String?,
            onResult: (ServerRewards.ChestOutcome) -> Unit,
            onFailure: (Exception) -> Unit,
        ) {
            cagrilar.add(startRarity to adNonce)
            basari.add(onResult)
            hata.add(onFailure)
        }
    }

    private val ornek = ServerRewards.ChestOutcome(listOf("RARE"), "RARE", "GOLD", 10)

    @Test
    fun onIstekYoksaDogrudanCagrilir() {
        val c = SahteCagirici()
        val p = ServerRewards.ChestPrefetcher(c)
        var gelen: ServerRewards.ChestOutcome? = null
        p.openChest("COMMON", "n", onResult = { gelen = it }, onFailure = {})
        assertEquals(listOf<Pair<String, String?>>("COMMON" to "n"), c.cagrilar)
        c.basari.single()(ornek)
        assertEquals(ornek, gelen)
    }

    @Test
    fun onIstekBittiyseSonucuAnindaVeBirKezKullanilir() {
        val c = SahteCagirici()
        val p = ServerRewards.ChestPrefetcher(c)
        p.prefetchChest("RARE")
        c.basari.single()(ornek)
        var gelen: ServerRewards.ChestOutcome? = null
        p.openChest("RARE", onResult = { gelen = it }, onFailure = {})
        assertEquals(ornek, gelen)
        // Ağa ikinci kez gidilmedi
        assertEquals(1, c.cagrilar.size)
        // Sonuç tüketildi: sonraki openChest yeni istek yapar
        p.openChest("RARE", onResult = {}, onFailure = {})
        assertEquals(2, c.cagrilar.size)
    }

    @Test
    fun onIstekSurerkenOpenChestBekler() {
        val c = SahteCagirici()
        val p = ServerRewards.ChestPrefetcher(c)
        p.prefetchChest("RARE", "nonce")
        // Sürerken ikinci ön-istek yok sayılır
        p.prefetchChest("EPIC")
        assertEquals(listOf<Pair<String, String?>>("RARE" to "nonce"), c.cagrilar)
        var gelen: ServerRewards.ChestOutcome? = null
        p.openChest("RARE", "nonce", onResult = { gelen = it }, onFailure = {})
        assertNull(gelen)
        assertEquals(1, c.cagrilar.size)
        c.basari.single()(ornek)
        assertEquals(ornek, gelen)
        // Bekleyen tüketti: sonuç saklanmaz
        p.openChest("RARE", onResult = {}, onFailure = {})
        assertEquals(2, c.cagrilar.size)
    }

    @Test
    fun onIstekHatasiIletilir() {
        val c = SahteCagirici()
        val p = ServerRewards.ChestPrefetcher(c)
        val hata = IllegalStateException("olmadı")
        // Önce biten hata, sonra openChest
        p.prefetchChest("RARE")
        c.hata.single()(hata)
        var gelen: Exception? = null
        p.openChest("RARE", onResult = {}, onFailure = { gelen = it })
        assertSame(hata, gelen)
        // Beklerken gelen hata
        p.prefetchChest("RARE")
        var gelen2: Exception? = null
        p.openChest("RARE", onResult = {}, onFailure = { gelen2 = it })
        c.hata.last()(hata)
        assertSame(hata, gelen2)
        // Hata sonrası yeniden ön-istek yapılabilir
        p.prefetchChest("RARE")
        assertEquals(3, c.cagrilar.size)
    }
}

class UserWalletFirestoreTest {
    @Test
    fun sabitlerVeKayitAlanlari() {
        assertEquals("keys", UserWalletFirestore.FIELD_KEYS)
        assertEquals("currency", UserWalletFirestore.FIELD_CURRENCY)
        assertEquals("app_prefs", UserWalletFirestore.PREFS_APP)
        assertEquals("updateUserWallet", UserWalletFirestore.FUNCTION_UPDATE_USER_WALLET)
        assertEquals("purchase_rollback", WalletReason.PURCHASE_ROLLBACK)
        assertEquals("spend", WalletReason.SPEND)
        assertEquals(mapOf<String, Any>("keys" to 1, "currency" to 0), UserWalletFirestore.registrationWalletFields())
        assertEquals(UserWallet(1, 0, null), UserWallet(1, 0))
    }

    @Test
    fun yuklemeEksikAlanlariYamalar() {
        // İki alan da var: yama yok, önbelleğe yazılır
        var prefs = InMemoryKeyValueStore()
        var r = UserWalletFirestore.resolveLoadedWallet(prefs, docKeys = 7, docCurrency = 300)
        assertEquals(WalletLoadResult(UserWallet(7, 300), emptyMap()), r)
        assertEquals(7, UserWalletFirestore.getCachedKeys(prefs))
        assertEquals(300, UserWalletFirestore.getCachedCurrency(prefs))
        assertFalse(prefs.contains("currency_migrated_to_firestore"))

        // İkisi de yok, yerelde eski altın var: anahtar varsayılanı + eski altın devralınır
        prefs = InMemoryKeyValueStore().apply { putInt("currency", 450) }
        r = UserWalletFirestore.resolveLoadedWallet(prefs, null, null)
        assertEquals(UserWallet(1, 450), r.wallet)
        assertEquals(mapOf<String, Any>("keys" to 1, "currency" to 450), r.patch)
        assertTrue(prefs.getBoolean("currency_migrated_to_firestore", false))
        // Geçiş bir kez yapılır: sonraki eksik okumada 0
        r = UserWalletFirestore.resolveLoadedWallet(prefs, 3, null)
        assertEquals(UserWallet(3, 0), r.wallet)
        assertEquals(mapOf<String, Any>("currency" to 0), r.patch)
        assertEquals(0, UserWalletFirestore.getCachedCurrency(prefs))

        // Yalnızca anahtar eksik
        prefs = InMemoryKeyValueStore()
        r = UserWalletFirestore.resolveLoadedWallet(prefs, null, 20)
        assertEquals(mapOf<String, Any>("keys" to 1), r.patch)
        assertEquals(UserWallet(1, 20), r.wallet)
    }

    @Test
    fun onbellektenVeAnlikGoruntuden() {
        val prefs = InMemoryKeyValueStore()
        // Boş önbellek: varsayılanlar
        assertEquals(UserWallet(1, 0), UserWalletFirestore.walletFromCache(prefs))
        assertEquals(UserWallet(9, 80), UserWalletFirestore.walletFromSnapshot(prefs, 9, 80))
        assertEquals(UserWallet(9, 80), UserWalletFirestore.walletFromCache(prefs))
        // Eksik alanlar: anahtar varsayılanı; altın için geçiş kuralı (ilk kez → yereldeki 80)
        assertEquals(UserWallet(1, 80), UserWalletFirestore.walletFromSnapshot(prefs, null, null))
        assertEquals(UserWallet(1, 0), UserWalletFirestore.walletFromSnapshot(prefs, null, null))
    }

    @Test
    fun deltaGovdesi() {
        assertNull(UserWalletFirestore.applyKeyDeltaPayload(0, WalletReason.SPEND))
        assertNull(UserWalletFirestore.applyCurrencyDeltaPayload(0, WalletReason.SPEND, "t"))
        assertEquals(
            mapOf<String, Any>("keys" to -1, "currency" to 0, "reason" to "spend"),
            UserWalletFirestore.applyKeyDeltaPayload(-1, WalletReason.SPEND),
        )
        assertEquals(
            mapOf<String, Any>("keys" to 0, "currency" to 500, "reason" to "purchase_rollback", "rollbackToken" to "tok"),
            UserWalletFirestore.applyCurrencyDeltaPayload(500, WalletReason.PURCHASE_ROLLBACK, "tok"),
        )
        assertEquals(
            mapOf<String, Any>("keys" to 2, "currency" to -3, "reason" to "x"),
            UserWalletFirestore.applyDeltaPayload(2, -3, "x", null),
        )
    }

    @Test
    fun guncellemeYaniti() {
        val prefs = InMemoryKeyValueStore()
        assertEquals(
            UserWallet(4, 120, "tok"),
            UserWalletFirestore.walletFromUpdateResult(prefs, mapOf("keys" to 4L, "currency" to 120.0, "rollbackToken" to "tok")),
        )
        assertEquals(4, UserWalletFirestore.getCachedKeys(prefs))
        assertEquals(120, UserWalletFirestore.getCachedCurrency(prefs))
        // Eksik alanlar önbellekten tamamlanır; jeton yoksa null
        assertEquals(UserWallet(4, 90), UserWalletFirestore.walletFromUpdateResult(prefs, mapOf("currency" to 90)))
        assertEquals(UserWallet(4, 90), UserWalletFirestore.walletFromUpdateResult(prefs, null))
        assertEquals(UserWallet(4, 90), UserWalletFirestore.walletFromUpdateResult(prefs, "bozuk"))
        // Boş önbellek + boş yanıt: varsayılanlar
        assertEquals(UserWallet(1, 0), UserWalletFirestore.walletFromUpdateResult(InMemoryKeyValueStore(), emptyMap<String, Any>()))
    }
}

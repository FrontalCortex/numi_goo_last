package com.sorobit.shared.oyun

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Beklenen değerler CrystalBreakVideoFragment.kt okunarak elle türetildi (Android'de çalıştırılmadı).
class CrystalBreakLogicTest {

    private fun durum(video: String?) = CrystalBreakState(video, Random(7))

    // ---- Profil ve kademe tablosu ----

    @Test
    fun videoProfilleri() {
        assertEquals(8, CrystalBreakLogic.videoNames.size)
        assertEquals(CrystalRarityBehavior.STATIC_COMMON, CrystalBreakLogic.behaviorFor("crystal_blue_blue"))
        assertEquals(CrystalRarityBehavior.FORCE_EPIC_THEN_LEGENDARY, CrystalBreakLogic.behaviorFor("crystal_red_yellow"))
        assertEquals(CrystalRarityBehavior.CHECKPOINT_RANDOM_PROGRESS, CrystalBreakLogic.behaviorFor("bilinmeyen"))
    }

    @Test
    fun videoAdiCozumu() {
        assertEquals("crystal_blue_blue", CrystalBreakLogic.resolveVideoName(null))
        assertEquals("crystal_blue_blue", CrystalBreakLogic.resolveVideoName("  "))
        assertEquals("crystal_red_red", CrystalBreakLogic.resolveVideoName("crystal_red_red"))
    }

    @Test
    fun kademeRenkleriVeEtiketleri() {
        assertEquals(0xFF8AD7FF.toInt(), CrystalRarityTier.COMMON.color)
        assertEquals("EPİK", CrystalRarityTier.EPIC.label)
        assertEquals("EFSANEVİ", CrystalRarityTier.LEGENDARY.label)
        assertEquals(listOf(0, 1, 2, 3), CrystalRarityTier.entries.map { it.rank })
    }

    @Test
    fun zorlanmisKademeler() {
        fun k(b: CrystalRarityBehavior, n: Int) =
            CrystalBreakLogic.nextRarityTierForCheckpoint(n, CrystalRarityTier.COMMON, b, Random(1))
        assertEquals(CrystalRarityTier.COMMON, k(CrystalRarityBehavior.FORCE_EPIC_ON_SECOND_CHECKPOINT, 1))
        assertEquals(CrystalRarityTier.EPIC, k(CrystalRarityBehavior.FORCE_EPIC_ON_SECOND_CHECKPOINT, 2))
        assertEquals(CrystalRarityTier.EPIC, k(CrystalRarityBehavior.FORCE_EPIC_FROM_FIRST_CHECKPOINT, 1))
        assertEquals(CrystalRarityTier.EPIC, k(CrystalRarityBehavior.FORCE_EPIC_FROM_FIRST_CHECKPOINT, 2))
        assertEquals(CrystalRarityTier.EPIC, k(CrystalRarityBehavior.FORCE_EPIC_THEN_DESTANSI, 1))
        assertEquals(CrystalRarityTier.DESTANSI, k(CrystalRarityBehavior.FORCE_EPIC_THEN_DESTANSI, 2))
        assertEquals(CrystalRarityTier.LEGENDARY, k(CrystalRarityBehavior.FORCE_EPIC_THEN_LEGENDARY, 2))
        assertEquals(CrystalRarityTier.COMMON, k(CrystalRarityBehavior.FORCE_DESTANSI_ON_SECOND_CHECKPOINT, 1))
        assertEquals(CrystalRarityTier.DESTANSI, k(CrystalRarityBehavior.FORCE_DESTANSI_ON_SECOND_CHECKPOINT, 2))
        assertEquals(CrystalRarityTier.DESTANSI, k(CrystalRarityBehavior.FORCE_DESTANSI_FROM_FIRST_CHECKPOINT, 1))
        assertEquals(CrystalRarityTier.DESTANSI, k(CrystalRarityBehavior.FORCE_DESTANSI_THEN_LEGENDARY, 1))
        assertEquals(CrystalRarityTier.LEGENDARY, k(CrystalRarityBehavior.FORCE_DESTANSI_THEN_LEGENDARY, 2))
    }

    @Test
    fun rastgeleIlerleme_kademeAslaDusmez() {
        for (tohum in 0 until 200) {
            val r = Random(tohum)
            val ilk = CrystalBreakLogic.nextRarityTierForCheckpoint(
                1, CrystalRarityTier.COMMON, CrystalRarityBehavior.CHECKPOINT_RANDOM_PROGRESS, r,
            )
            assertTrue(ilk != CrystalRarityTier.LEGENDARY, "1. durakta efsanevi çıkmaz")
            val ikinci = CrystalBreakLogic.nextRarityTierForCheckpoint(
                2, ilk, CrystalRarityBehavior.CHECKPOINT_RANDOM_PROGRESS, r,
            )
            assertTrue(ikinci.rank >= ilk.rank)
            val ucuncu = CrystalBreakLogic.nextRarityTierForCheckpoint(
                3, ikinci, CrystalRarityBehavior.CHECKPOINT_RANDOM_PROGRESS, r,
            )
            assertEquals(ikinci, ucuncu)
        }
    }

    @Test
    fun dalgaYildizSayisi() {
        assertEquals(0, CrystalBreakLogic.rarityWaveStarCount(CrystalRarityTier.COMMON))
        assertEquals(12, CrystalBreakLogic.rarityWaveStarCount(CrystalRarityTier.EPIC))
        assertEquals(28, CrystalBreakLogic.rarityWaveStarCount(CrystalRarityTier.DESTANSI))
        assertEquals(50, CrystalBreakLogic.rarityWaveStarCount(CrystalRarityTier.LEGENDARY))
    }

    // ---- Durak akışı ----

    @Test
    fun oynatimBaslamadanEtiketGizli_dalgaBeyaz() {
        val d = durum("crystal_red_red")
        assertFalse(d.rarityLabelVisible)
        assertEquals(0xFFFFFFFF.toInt(), d.tapWaveColor)
        d.onPlaybackStarted()
        assertTrue(d.rarityLabelVisible)
        assertEquals(CrystalRarityTier.COMMON.color, d.tapWaveColor)
    }

    @Test
    fun oynaticiHazirDegilse_vurusSurerAmaIlerlemez() {
        val d = durum("crystal_red_red")
        val s = d.onTick(5000L, isPlaying = false)
        assertTrue(s.keepTicking)
        assertFalse(s.pausePlayback)
        assertEquals(0, d.nextCheckpointIndex)
    }

    @Test
    fun ucDurak_herBirindeBekler_dokununcaDevamEder() {
        val d = durum("crystal_red_yellow")
        d.onPlaybackStarted()
        assertTrue(d.shouldTick)

        // 1. durak öncesi
        assertTrue(d.onTick(399L, true).keepTicking)
        // 1. durak: bekle. Etiket henüz değişmez (durak + 50 ms'de değişir).
        val s1 = d.onTick(400L, true)
        assertFalse(s1.keepTicking)
        assertTrue(s1.pausePlayback)
        assertTrue(d.waitingTapToContinue)
        assertTrue(d.shouldIdlePulse)
        assertFalse(d.shouldTick)
        assertEquals(CrystalRarityTier.COMMON, d.currentRarityTier)
        // Beklerken vuruş hiçbir şey yapmaz.
        assertFalse(d.onTick(400L, true).keepTicking)

        // Dokunuş: devam + 1. dalga
        val t1 = d.onTap(isPlaying = false)
        assertEquals(CrystalTapAction.RESUME, t1.action)
        assertEquals(1, t1.resumeWaveForContinueTap)
        assertFalse(d.waitingTapToContinue)
        assertTrue(d.onResumeWaveDelayElapsed(1))
        assertEquals(CrystalRarityTier.EPIC, d.currentRarityTier)
        assertEquals(1, d.rarityCheckpointAppliedCount)

        // 2. durak
        assertTrue(d.onTick(1249L, true).keepTicking)
        assertTrue(d.onTick(1250L, true).pausePlayback)
        val t2 = d.onTap(false)
        assertEquals(2, t2.resumeWaveForContinueTap)
        assertTrue(d.onResumeWaveDelayElapsed(2))
        assertEquals(CrystalRarityTier.LEGENDARY, d.currentRarityTier)

        // 3. durak: bekler ama dalga yok, etiket değişmez
        assertTrue(d.onTick(2200L, true).pausePlayback)
        val t3 = d.onTap(false)
        assertEquals(CrystalTapAction.RESUME, t3.action)
        assertEquals(0, t3.resumeWaveForContinueTap)
        assertFalse(d.onResumeWaveDelayElapsed(3))
        assertEquals(CrystalRarityTier.LEGENDARY, d.currentRarityTier)

        // Durak kalmadı
        assertFalse(d.shouldTick)
        assertFalse(d.onTick(2500L, true).keepTicking)
    }

    @Test
    fun etiketVuruslaDaGuncellenir_durakArti50ms() {
        val d = durum("crystal_red_red")
        d.onPlaybackStarted()
        d.onTick(400L, true)
        d.onTap(false)
        // Dalga gecikmesi beklenmeden gelen vuruş: 449'da değişmez, 450'de değişir.
        assertFalse(d.onTick(449L, true).rarityChanged)
        val s = d.onTick(450L, true)
        assertTrue(s.rarityChanged)
        assertEquals(CrystalRarityTier.EPIC, d.currentRarityTier)
        // Aynı durak ikinci kez uygulanmaz.
        assertFalse(d.onTick(460L, true).rarityChanged)
    }

    @Test
    fun sabitSiradanVideo_etiketDegismez() {
        val d = durum("crystal_blue_blue")
        d.onPlaybackStarted()
        assertFalse(d.onTick(3000L, true).rarityChanged)
        assertEquals(CrystalRarityTier.COMMON, d.currentRarityTier)
        assertEquals(2, d.rarityCheckpointAppliedCount)
    }

    @Test
    fun oynatimSirasindaIkiTik_sonrakiDuragiKendiligindenGecer() {
        val d = durum("crystal_red_red")
        d.onPlaybackStarted()
        assertEquals(CrystalTapAction.TAP_WAVE, d.onTap(isPlaying = true).action)
        assertEquals(0, d.pendingAutoContinues)
        assertEquals(CrystalTapAction.TAP_WAVE, d.onTap(isPlaying = true).action)
        assertEquals(1, d.pendingAutoContinues)
        assertEquals(0, d.playPhaseTapCount)

        val s = d.onTick(400L, true)
        assertTrue(s.keepTicking)
        assertFalse(s.pausePlayback)
        assertEquals(1, s.resumeWaveForContinueTap)
        assertEquals(0, d.pendingAutoContinues)
        assertEquals(1, d.continueTapCount)
        assertFalse(d.waitingTapToContinue)

        // Hak tükendi: 2. durakta bekler.
        assertTrue(d.onTick(1250L, true).pausePlayback)
    }

    @Test
    fun duraktaDokunus_oynatimTikSayaciniSifirlar() {
        val d = durum("crystal_red_red")
        d.onPlaybackStarted()
        d.onTap(true)
        assertEquals(1, d.playPhaseTapCount)
        d.onTick(400L, true)
        d.onTap(false)
        assertEquals(0, d.playPhaseTapCount)
    }

    @Test
    fun oynatimDurmusVeDuraktaDegilken_dokunusYokSayilir() {
        val d = durum("crystal_red_red")
        assertEquals(CrystalTapAction.IGNORED, d.onTap(isPlaying = false).action)
        assertEquals(0, d.playPhaseTapCount)
    }

    @Test
    fun videoBitince() {
        val d = durum("crystal_red_red")
        d.onPlaybackStarted()
        d.onTick(400L, true)
        d.onVideoEnded()
        assertTrue(d.videoEnded)
        assertFalse(d.waitingTapToContinue)
        assertFalse(d.rarityLabelVisible)
        assertFalse(d.shouldIdlePulse)
        assertFalse(d.shouldTick)
        assertFalse(d.onTick(9999L, true).keepTicking)
        // Bittikten sonra bekleme bayrağı kapalı olduğu için dokunuş yok sayılır (Android: erken dönüş).
        assertEquals(CrystalTapAction.IGNORED, d.onTap(false).action)
    }

    @Test
    fun adVerilmezse_yedekVideo() {
        assertEquals("crystal_blue_blue", CrystalBreakState(null).videoName)
    }
}

// Kaynak: app/src/main/java/com/example/app/CrystalBreakVideoFragment.kt — görünümden bağımsız kısım:
// `RarityBehavior`, `RarityTier`, `videoProfiles`, `checkpointsMs`, `checkpointTick` (durak kuralı),
// tıklama dinleyicisi (oynatım sırasındaki çift tık hakkı, durakta devam, bitince kapanış),
// `maybeUpdateRarityLabel`, `nextRarityTierForCheckpoint`, `maybeTriggerResumeTapRarityWave`,
// `currentRarityWaveColor`, `showYellowWave` içindeki yıldız sayısı.
// Farklar: ExoPlayer / Handler / animasyon yok; oynatıcının konumu ve "oynuyor mu" bilgisi parametre
// olarak gelir, yapılacak iş sonuç nesnesi olarak döner. `Color.parseColor("#RRGGBB")` → ARGB Int
// (aynı değer). `candidates.random()` enjekte edilebilir kaynaktan çekilir.
// Android'deki hâliyle korunan notlar:
//  - Nadirlik etiketi yalnızca ilk İKİ durakta (durak + 50 ms) güncellenir; üçüncü durak yalnızca bekletir.
//  - `videoProfiles`'ta olmayan bir video adı "rastgele ilerleme" davranışına düşer.
//  - Bu ekrandaki kademe adları sandık ekranındakinden farklıdır (EPİK / DESTANSI ayrı kademelerdir).
package com.sorobit.shared.oyun

import com.sorobit.shared.matematik.RastgeleKaynagi
import kotlin.random.Random

/** Videonun nadirlik etiketini duraklarda nasıl değiştireceği. */
enum class CrystalRarityBehavior {
    STATIC_COMMON,
    FORCE_EPIC_ON_SECOND_CHECKPOINT,
    FORCE_EPIC_FROM_FIRST_CHECKPOINT,
    FORCE_EPIC_THEN_DESTANSI,
    FORCE_EPIC_THEN_LEGENDARY,
    FORCE_DESTANSI_ON_SECOND_CHECKPOINT,
    FORCE_DESTANSI_FROM_FIRST_CHECKPOINT,
    FORCE_DESTANSI_THEN_LEGENDARY,
    CHECKPOINT_RANDOM_PROGRESS,
}

/** Kristal videosunun üstünde gösterilen nadirlik kademesi. [color] ARGB. */
enum class CrystalRarityTier(
    val label: String,
    val color: Int,
    val rank: Int,
) {
    COMMON("SIRADAN", 0xFF8AD7FF.toInt(), 0),
    EPIC("EPİK", 0xFFFF4D4D.toInt(), 1),
    DESTANSI("DESTANSI", 0xFFB26BFF.toInt(), 2),
    LEGENDARY("EFSANEVİ", 0xFFFFD84A.toInt(), 3),
}

/** Bir saat vuruşunun (Android: 16 ms'lik `checkpointTick`) sonucu. */
data class CrystalTickResult(
    /** Vuruşlar sürsün mü (false: durak bekleniyor, video bitti ya da durak kalmadı). */
    val keepTicking: Boolean,
    /** Oynatıcı duraklatılsın ve "dokun" nabız animasyonu başlasın. */
    val pausePlayback: Boolean,
    /** Nadirlik etiketi bu vuruşta değişti. */
    val rarityChanged: Boolean,
    /**
     * 0 değilse: durak, önceden kazanılmış çift tık hakkıyla kendiliğinden geçildi;
     * [CrystalBreakLogic.RARITY_APPLY_DELAY_MS] sonra [CrystalBreakState.onResumeWaveDelayElapsed]
     * bu değerle çağrılmalı.
     */
    val resumeWaveForContinueTap: Int,
)

enum class CrystalTapAction {
    /** Hiçbir görsel etki yok (oynatım durmuş ama durakta da değil). */
    IGNORED,

    /** Oynatım sırasında dokunuş: yalnızca dokunma dalgası. */
    TAP_WAVE,

    /** Durakta dokunuş: dokunma dalgası + oynatıma devam + vuruşları yeniden başlat. */
    RESUME,

    /** Video bittikten sonra dokunuş: ekranı kapat. */
    DISMISS,
}

data class CrystalTapResult(
    val action: CrystalTapAction,
    /** RESUME: bkz. [CrystalTickResult.resumeWaveForContinueTap]. */
    val resumeWaveForContinueTap: Int = 0,
)

object CrystalBreakLogic {
    /** Videonun bekleyip dokunuş istediği anlar (ms). */
    val checkpointsMs: List<Long> = listOf(400L, 1250L, 2200L)

    /** Nadirlik etiketi duraktan bu kadar sonra güncellenir; durağı geçiren dalga da bu kadar gecikir. */
    const val RARITY_APPLY_DELAY_MS = 50L

    /** Vuruş aralığı. */
    const val TICK_INTERVAL_MS = 16L

    /** Tıklama sesi (`R.raw.crystal_click1`) ve ses düzeyi. */
    const val CLICK_SOUND = "crystal_click1"
    const val CLICK_SOUND_VOLUME = 0.5f

    /** Videonun kendi sesinin düzeyi (ses kapalıysa 0). */
    const val VIDEO_VOLUME = 0.5f

    private val videoProfiles: Map<String, CrystalRarityBehavior> = mapOf(
        "crystal_blue_blue" to CrystalRarityBehavior.STATIC_COMMON,
        "crystal_blue_red" to CrystalRarityBehavior.FORCE_EPIC_ON_SECOND_CHECKPOINT,
        "crystal_red_red" to CrystalRarityBehavior.FORCE_EPIC_FROM_FIRST_CHECKPOINT,
        "crystal_red_purple" to CrystalRarityBehavior.FORCE_EPIC_THEN_DESTANSI,
        "crystal_red_yellow" to CrystalRarityBehavior.FORCE_EPIC_THEN_LEGENDARY,
        "crystal_blue_purple" to CrystalRarityBehavior.FORCE_DESTANSI_ON_SECOND_CHECKPOINT,
        "crystal_purple_purple" to CrystalRarityBehavior.FORCE_DESTANSI_FROM_FIRST_CHECKPOINT,
        "crystal_purple_yellow" to CrystalRarityBehavior.FORCE_DESTANSI_THEN_LEGENDARY,
    )

    /** Tanımlı kristal videolarının adları (res/raw içindeki .mp4 dosyalarının uzantısız adı). */
    val videoNames: List<String> = videoProfiles.keys.toList()

    /** `profileFor`: bilinmeyen video rastgele ilerler. */
    fun behaviorFor(videoName: String): CrystalRarityBehavior =
        videoProfiles[videoName] ?: CrystalRarityBehavior.CHECKPOINT_RANDOM_PROGRESS

    /** `resolveInitialVideoResId`: ad boşsa/yoksa ilk tercih edilen video. */
    fun resolveVideoName(requestedVideoName: String?): String =
        requestedVideoName?.takeIf { it.isNotBlank() } ?: ChestCrystalPolicy.FALLBACK_VIDEO

    /** `nextRarityTierForCheckpoint`. [checkpointNumber] 1 tabanlıdır. */
    fun nextRarityTierForCheckpoint(
        checkpointNumber: Int,
        currentTier: CrystalRarityTier,
        behavior: CrystalRarityBehavior,
        random: Random = RastgeleKaynagi.rastgele,
    ): CrystalRarityTier {
        if (behavior == CrystalRarityBehavior.FORCE_EPIC_THEN_LEGENDARY) {
            return when {
                checkpointNumber >= 2 -> CrystalRarityTier.LEGENDARY
                checkpointNumber >= 1 -> CrystalRarityTier.EPIC
                else -> CrystalRarityTier.COMMON
            }
        }
        if (behavior == CrystalRarityBehavior.FORCE_EPIC_THEN_DESTANSI) {
            return when {
                checkpointNumber >= 2 -> CrystalRarityTier.DESTANSI
                checkpointNumber >= 1 -> CrystalRarityTier.EPIC
                else -> CrystalRarityTier.COMMON
            }
        }
        if (behavior == CrystalRarityBehavior.FORCE_EPIC_FROM_FIRST_CHECKPOINT) {
            return if (checkpointNumber >= 1) CrystalRarityTier.EPIC else CrystalRarityTier.COMMON
        }
        if (behavior == CrystalRarityBehavior.FORCE_DESTANSI_THEN_LEGENDARY) {
            return when {
                checkpointNumber >= 2 -> CrystalRarityTier.LEGENDARY
                checkpointNumber >= 1 -> CrystalRarityTier.DESTANSI
                else -> CrystalRarityTier.COMMON
            }
        }
        if (behavior == CrystalRarityBehavior.FORCE_DESTANSI_FROM_FIRST_CHECKPOINT) {
            return if (checkpointNumber >= 1) CrystalRarityTier.DESTANSI else CrystalRarityTier.COMMON
        }
        if (behavior == CrystalRarityBehavior.FORCE_EPIC_ON_SECOND_CHECKPOINT) {
            return if (checkpointNumber >= 2) CrystalRarityTier.EPIC else CrystalRarityTier.COMMON
        }
        if (behavior == CrystalRarityBehavior.FORCE_DESTANSI_ON_SECOND_CHECKPOINT) {
            return if (checkpointNumber >= 2) CrystalRarityTier.DESTANSI else CrystalRarityTier.COMMON
        }

        val candidates = when (checkpointNumber) {
            1 -> listOf(CrystalRarityTier.COMMON, CrystalRarityTier.EPIC, CrystalRarityTier.DESTANSI)
            2 -> listOf(currentTier, CrystalRarityTier.EPIC, CrystalRarityTier.DESTANSI, CrystalRarityTier.LEGENDARY)
            else -> listOf(currentTier)
        }
            .distinct()
            .filter { it.rank >= currentTier.rank }
        if (candidates.isEmpty()) return currentTier
        return candidates.random(random)
    }

    /** `showYellowWave`: durağı geçiren dalgada saçılan yıldız sayısı. */
    fun rarityWaveStarCount(tier: CrystalRarityTier): Int = when (tier) {
        CrystalRarityTier.COMMON -> 0
        CrystalRarityTier.EPIC -> 12
        CrystalRarityTier.DESTANSI -> 28
        CrystalRarityTier.LEGENDARY -> 50
    }
}

/**
 * Kristal kırma videosunun oynatım durum makinesi (Android: CrystalBreakVideoFragment alanları).
 * Oynatıcı platformdadır; konum ve oynatım durumu çağrılarla bildirilir.
 */
class CrystalBreakState(
    requestedVideoName: String?,
    private val random: Random,
) {
    /** Swift'ten çağrı için: varsayılan rastgele kaynakla. */
    constructor(requestedVideoName: String?) : this(requestedVideoName, RastgeleKaynagi.rastgele)

    val videoName: String = CrystalBreakLogic.resolveVideoName(requestedVideoName)
    private val behavior = CrystalBreakLogic.behaviorFor(videoName)

    var nextCheckpointIndex = 0
        private set
    var continueTapCount = 0
        private set
    var playPhaseTapCount = 0
        private set
    var pendingAutoContinues = 0
        private set
    var currentRarityTier = CrystalRarityTier.COMMON
        private set
    var rarityCheckpointAppliedCount = 0
        private set
    var waitingTapToContinue = false
        private set
    var videoEnded = false
        private set
    var hasPlaybackStarted = false
        private set

    /** `onIsPlayingChanged`: etiket, video ilk kez oynamaya başlayınca görünür; bitince gizlenir. */
    val rarityLabelVisible: Boolean
        get() = hasPlaybackStarted && !videoEnded

    /** `currentRarityWaveColor`: etiket görünüyorsa onun rengi, değilse beyaz (ARGB). */
    val tapWaveColor: Int
        get() = if (rarityLabelVisible) currentRarityTier.color else 0xFFFFFFFF.toInt()

    /** `maybeStartIdlePulse` koşulu: durakta bekleniyor ve video bitmedi. */
    val shouldIdlePulse: Boolean
        get() = waitingTapToContinue && !videoEnded

    /** `maybeStartCheckpointTick` koşulu. */
    val shouldTick: Boolean
        get() = !videoEnded && !waitingTapToContinue && nextCheckpointIndex < CrystalBreakLogic.checkpointsMs.size

    /** Oynatıcı "oynuyor" durumuna geçtiğinde. */
    fun onPlaybackStarted() {
        hasPlaybackStarted = true
    }

    /** `Player.STATE_ENDED`: Android'de ekran bu anda kendiliğinden kapanır. */
    fun onVideoEnded() {
        videoEnded = true
        waitingTapToContinue = false
    }

    /**
     * `maybeUpdateRarityLabel`. @return kademe değişti mi.
     */
    fun maybeUpdateRarityLabel(positionMs: Long): Boolean {
        if (behavior == CrystalRarityBehavior.STATIC_COMMON) {
            var changed = false
            if (currentRarityTier != CrystalRarityTier.COMMON) {
                currentRarityTier = CrystalRarityTier.COMMON
                changed = true
            }
            rarityCheckpointAppliedCount = 2
            return changed
        }

        var changed = false
        while (rarityCheckpointAppliedCount < 2) {
            val checkpointMs = CrystalBreakLogic.checkpointsMs.getOrNull(rarityCheckpointAppliedCount) ?: return changed
            val applyAt = checkpointMs + CrystalBreakLogic.RARITY_APPLY_DELAY_MS
            if (positionMs < applyAt) return changed
            rarityCheckpointAppliedCount += 1
            val next = CrystalBreakLogic.nextRarityTierForCheckpoint(
                checkpointNumber = rarityCheckpointAppliedCount,
                currentTier = currentRarityTier,
                behavior = behavior,
                random = random,
            )
            if (next != currentRarityTier) changed = true
            currentRarityTier = next
        }
        return changed
    }

    /** `checkpointTick.run`. [isPlaying]: oynatıcı hazır ve oynuyor. */
    fun onTick(positionMs: Long, isPlaying: Boolean): CrystalTickResult {
        if (videoEnded || waitingTapToContinue) return CrystalTickResult(false, false, false, 0)
        if (nextCheckpointIndex >= CrystalBreakLogic.checkpointsMs.size) return CrystalTickResult(false, false, false, 0)
        if (!isPlaying) return CrystalTickResult(true, false, false, 0)

        val rarityChanged = maybeUpdateRarityLabel(positionMs)
        val target = CrystalBreakLogic.checkpointsMs[nextCheckpointIndex]
        var resumeWave = 0
        if (positionMs >= target) {
            nextCheckpointIndex += 1
            if (pendingAutoContinues > 0) {
                // Oynatım sırasında alınan çift tık hakkı: bu durakta otomatik devam et.
                pendingAutoContinues -= 1
                continueTapCount += 1
                if (continueTapCount in 1..2) resumeWave = continueTapCount
            } else {
                waitingTapToContinue = true
                return CrystalTickResult(false, true, rarityChanged, 0)
            }
        }
        return CrystalTickResult(true, false, rarityChanged, resumeWave)
    }

    /** Ekrana dokunuş. [isPlaying]: oynatıcı şu an oynuyor. (Tıklama sesi her dokunuşta çalınır.) */
    fun onTap(isPlaying: Boolean): CrystalTapResult {
        if (!waitingTapToContinue) {
            if (!videoEnded && isPlaying) {
                // Oynatım sırasında 2 tık = sonraki durakta otomatik devam hakkı.
                playPhaseTapCount += 1
                if (playPhaseTapCount >= 2) {
                    pendingAutoContinues += 1
                    playPhaseTapCount = 0
                }
                return CrystalTapResult(CrystalTapAction.TAP_WAVE)
            }
            return CrystalTapResult(CrystalTapAction.IGNORED)
        }
        if (videoEnded) return CrystalTapResult(CrystalTapAction.DISMISS)
        continueTapCount += 1
        val resumeWave = if (continueTapCount in 1..2) continueTapCount else 0
        waitingTapToContinue = false
        playPhaseTapCount = 0
        return CrystalTapResult(CrystalTapAction.RESUME, resumeWave)
    }

    /**
     * `maybeTriggerResumeTapRarityWave`'in 50 ms sonraki gövdesi: etiketi durağın kuralına göre
     * günceller. Ardından platform, [currentRarityTier] rengiyle büyük dalgayı ve
     * [CrystalBreakLogic.rarityWaveStarCount] kadar yıldızı gösterir.
     * @return dalga gösterilsin mi ([continueTapCount] 1..2 dışında ise false).
     */
    fun onResumeWaveDelayElapsed(continueTapCount: Int): Boolean {
        if (continueTapCount !in 1..2) return false
        val checkpointIndex = (continueTapCount - 1).coerceAtLeast(0)
        val positionForRarity = CrystalBreakLogic.checkpointsMs[checkpointIndex] + CrystalBreakLogic.RARITY_APPLY_DELAY_MS
        maybeUpdateRarityLabel(positionForRarity)
        return true
    }
}

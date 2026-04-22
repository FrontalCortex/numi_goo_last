package com.example.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.app.databinding.FragmentCrystalBreakVideoBinding
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tek MP4 üzerinde checkpoint duraklarıyla ilerler:
 * 150ms -> 360ms -> 570ms durakları, her birinde tap ile devam.
 * Son checkpoint sonrası video sonuna kadar oynar; video bittikten sonraki tap ile ekran kapanır.
 */
class CrystalBreakVideoFragment : DialogFragment() {
    private enum class RarityBehavior {
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

    private data class VideoProfile(
        val rarityBehavior: RarityBehavior,
    )

    private enum class RarityTier(
        val label: String,
        val color: Int,
        val rank: Int,
    ) {
        COMMON("SIRADAN", Color.parseColor("#8AD7FF"), 0),
        EPIC("EP\u0130K", Color.parseColor("#FF4D4D"), 1),
        DESTANSI("DESTANSI", Color.parseColor("#B26BFF"), 2),
        LEGENDARY("EFSANEV\u0130", Color.parseColor("#FFD84A"), 3),
    }

    private var _binding: FragmentCrystalBreakVideoBinding? = null
    private val binding get() = _binding!!
    private var exoPlayer: ExoPlayer? = null

    private var currentVideoResId: Int = 0
    private var currentVideoName: String = ""
    private val checkpointsMs = longArrayOf(400L, 1250L, 2200L)
    private var nextCheckpointIndex: Int = 0
    private var continueTapCount: Int = 0
    private var playPhaseTapCount: Int = 0
    private var pendingAutoContinues: Int = 0
    private var currentRarityTier: RarityTier = RarityTier.COMMON
    private var rarityCheckpointAppliedCount: Int = 0
    private var waitingTapToContinue: Boolean = false
    private var videoEnded: Boolean = false
    private var onDismissCallback: (() -> Unit)? = null
    private var idlePulse: AnimatorSet? = null
    private var tapWaveAnimator: AnimatorSet? = null
    private var yellowWaveAnimator: AnimatorSet? = null
    private var videoWaveAnimator: AnimatorSet? = null
    private var lastTapX: Float = 0f
    private var lastTapY: Float = 0f
    private var clickSoundPool: SoundPool? = null
    private var clickSoundId: Int = 0
    private var clickSoundLoaded: Boolean = false

    private val videoProfiles = mapOf(
        "crystal_blue_blue" to VideoProfile(rarityBehavior = RarityBehavior.STATIC_COMMON),
        "crystal_blue_red" to VideoProfile(rarityBehavior = RarityBehavior.FORCE_EPIC_ON_SECOND_CHECKPOINT),
        "crystal_red_red" to VideoProfile(rarityBehavior = RarityBehavior.FORCE_EPIC_FROM_FIRST_CHECKPOINT),
        "crystal_red_purple" to VideoProfile(rarityBehavior = RarityBehavior.FORCE_EPIC_THEN_DESTANSI),
        "crystal_red_yellow" to VideoProfile(rarityBehavior = RarityBehavior.FORCE_EPIC_THEN_LEGENDARY),
        "crystal_blue_purple" to VideoProfile(rarityBehavior = RarityBehavior.FORCE_DESTANSI_ON_SECOND_CHECKPOINT),
        "crystal_purple_purple" to VideoProfile(rarityBehavior = RarityBehavior.FORCE_DESTANSI_FROM_FIRST_CHECKPOINT),
        "crystal_purple_yellow" to VideoProfile(rarityBehavior = RarityBehavior.FORCE_DESTANSI_THEN_LEGENDARY),
    )

    private val progressHandler = Handler(Looper.getMainLooper())
    private val checkpointTick = object : Runnable {
        override fun run() {
            val player = exoPlayer ?: return
            if (videoEnded || waitingTapToContinue) return
            if (nextCheckpointIndex >= checkpointsMs.size) return
            if (player.playbackState != Player.STATE_READY || !player.isPlaying) {
                progressHandler.postDelayed(this, 16L)
                return
            }
            maybeUpdateRarityLabel(player.currentPosition)
            val target = checkpointsMs[nextCheckpointIndex]
            if (player.currentPosition >= target) {
                nextCheckpointIndex += 1
                if (pendingAutoContinues > 0) {
                    // Oynatım sırasında alınan çift tık hakkı: bu durakta otomatik devam et.
                    pendingAutoContinues -= 1
                    continueTapCount += 1
                    maybeTriggerResumeTapRarityWave(continueTapCount)
                } else {
                    waitingTapToContinue = true
                    player.pause()
                    maybeStartIdlePulse()
                    return
                }
            }
            progressHandler.postDelayed(this, 16L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        isCancelable = false
        currentVideoResId = savedInstanceState?.getInt(STATE_VIDEO_RES_ID) ?: 0
        if (currentVideoResId == 0) {
            val requestedVideoName = arguments?.getString(ARG_VIDEO_NAME)
            currentVideoResId = resolveInitialVideoResId(requestedVideoName)
        }
        currentVideoName = savedInstanceState?.getString(STATE_VIDEO_NAME)
            ?: requireContext().resources.getResourceEntryName(currentVideoResId)
        nextCheckpointIndex = savedInstanceState?.getInt(STATE_NEXT_CHECKPOINT_INDEX) ?: 0
        continueTapCount = savedInstanceState?.getInt(STATE_CONTINUE_TAP_COUNT) ?: 0
        playPhaseTapCount = savedInstanceState?.getInt(STATE_PLAY_PHASE_TAP_COUNT) ?: 0
        pendingAutoContinues = savedInstanceState?.getInt(STATE_PENDING_AUTO_CONTINUES) ?: 0
        currentRarityTier = savedInstanceState
            ?.getString(STATE_RARITY_TIER)
            ?.let { saved -> RarityTier.values().firstOrNull { it.name == saved } }
            ?: RarityTier.COMMON
        rarityCheckpointAppliedCount = savedInstanceState?.getInt(STATE_RARITY_CHECKPOINT_APPLIED_COUNT) ?: 0
        waitingTapToContinue = savedInstanceState?.getBoolean(STATE_WAITING_TAP) ?: false
        videoEnded = savedInstanceState?.getBoolean(STATE_VIDEO_ENDED) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCrystalBreakVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickSound()
        exoPlayer = ExoPlayer.Builder(requireContext()).build().also { player ->
            binding.crystalPlayerView.player = player
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        videoEnded = true
                        waitingTapToContinue = false
                        progressHandler.removeCallbacks(checkpointTick)
                        dismissAllowingStateLoss()
                    }
                }
            })
            val uri = Uri.parse("android.resource://${requireContext().packageName}/$currentVideoResId")
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.playWhenReady = !(waitingTapToContinue || videoEnded)
        }
        setupRarityLabel()

        binding.crystalVideoRoot.setOnTouchListener { _, event ->
            lastTapX = event.x
            lastTapY = event.y
            false
        }

        binding.crystalVideoRoot.setOnClickListener {
            playClickSound()
            val player = exoPlayer ?: return@setOnClickListener
            if (!waitingTapToContinue) {
                if (!videoEnded && player.isPlaying) {
                    showTapWave()
                    // Oynatım sırasında 2 tık = sonraki durakta otomatik devam hakkı.
                    playPhaseTapCount += 1
                    if (playPhaseTapCount >= 2) {
                        pendingAutoContinues += 1
                        playPhaseTapCount = 0
                    }
                }
                return@setOnClickListener
            }
            if (videoEnded) {
                dismissAllowingStateLoss()
                return@setOnClickListener
            }
            showTapWave()
            stopIdlePulse(resetScale = true)
            continueTapCount += 1
            maybeTriggerResumeTapRarityWave(continueTapCount)
            waitingTapToContinue = false
            playPhaseTapCount = 0
            player.play()
            maybeStartCheckpointTick()
        }
        maybeStartCheckpointTick()
        maybeStartIdlePulse()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
            setBackgroundDrawableResource(android.R.color.black)
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setOnKeyListener { _, keyCode, event ->
            keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP
        }
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
        progressHandler.removeCallbacks(checkpointTick)
        stopIdlePulse(resetScale = false)
    }

    override fun onResume() {
        super.onResume()
        if (!waitingTapToContinue && !videoEnded) {
            exoPlayer?.play()
            maybeStartCheckpointTick()
        } else {
            maybeStartIdlePulse()
        }
    }

    override fun onDestroyView() {
        progressHandler.removeCallbacks(checkpointTick)
        stopIdlePulse(resetScale = false)
        tapWaveAnimator?.cancel()
        tapWaveAnimator = null
        yellowWaveAnimator?.cancel()
        yellowWaveAnimator = null
        videoWaveAnimator?.cancel()
        videoWaveAnimator = null
        clickSoundPool?.release()
        clickSoundPool = null
        clickSoundId = 0
        clickSoundLoaded = false
        binding.crystalPlayerView.player = null
        exoPlayer?.release()
        exoPlayer = null
        _binding = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_NEXT_CHECKPOINT_INDEX, nextCheckpointIndex)
        outState.putInt(STATE_CONTINUE_TAP_COUNT, continueTapCount)
        outState.putInt(STATE_PLAY_PHASE_TAP_COUNT, playPhaseTapCount)
        outState.putInt(STATE_PENDING_AUTO_CONTINUES, pendingAutoContinues)
        outState.putInt(STATE_VIDEO_RES_ID, currentVideoResId)
        outState.putString(STATE_VIDEO_NAME, currentVideoName)
        outState.putString(STATE_RARITY_TIER, currentRarityTier.name)
        outState.putInt(STATE_RARITY_CHECKPOINT_APPLIED_COUNT, rarityCheckpointAppliedCount)
        outState.putBoolean(STATE_WAITING_TAP, waitingTapToContinue)
        outState.putBoolean(STATE_VIDEO_ENDED, videoEnded)
    }

    override fun onDestroy() {
        onDismissCallback?.invoke()
        onDismissCallback = null
        super.onDestroy()
    }

    fun setOnDismissCallback(callback: () -> Unit) {
        onDismissCallback = callback
    }

    private fun maybeStartCheckpointTick() {
        if (videoEnded || waitingTapToContinue) return
        if (nextCheckpointIndex >= checkpointsMs.size) return
        progressHandler.removeCallbacks(checkpointTick)
        progressHandler.post(checkpointTick)
    }

    private fun showTapWave() {
        val wave = binding.crystalTapWaveCircle
        val glow = binding.crystalTapWaveGlow
        val tapColor = currentRarityWaveColor()
        tapWaveAnimator?.cancel()
        wave.background?.mutate()?.setTint(tapColor)
        glow.background?.mutate()?.setTint(tapColor)
        wave.backgroundTintList = ColorStateList.valueOf(tapColor)
        glow.backgroundTintList = ColorStateList.valueOf(tapColor)
        wave.x = lastTapX - wave.width / 2f
        wave.y = lastTapY - wave.height / 2f
        glow.x = lastTapX - glow.width / 2f
        glow.y = lastTapY - glow.height / 2f
        wave.scaleX = 0.55f
        wave.scaleY = 0.55f
        wave.alpha = 0f
        glow.scaleX = 0.5f
        glow.scaleY = 0.5f
        glow.alpha = 0f
        val alphaIn = ObjectAnimator.ofFloat(wave, View.ALPHA, 0f, 1f).apply { duration = 80L }
        val alphaOut = ObjectAnimator.ofFloat(wave, View.ALPHA, 1f, 0f).apply { duration = 260L }
        val scaleX = ObjectAnimator.ofFloat(wave, View.SCALE_X, 0.55f, 3.2f).apply { duration = 340L }
        val scaleY = ObjectAnimator.ofFloat(wave, View.SCALE_Y, 0.55f, 3.2f).apply { duration = 340L }
        val glowAlphaIn = ObjectAnimator.ofFloat(glow, View.ALPHA, 0f, 0.95f).apply { duration = 70L }
        val glowAlphaOut = ObjectAnimator.ofFloat(glow, View.ALPHA, 0.95f, 0f).apply { duration = 300L }
        val glowScaleX = ObjectAnimator.ofFloat(glow, View.SCALE_X, 0.5f, 2.8f).apply { duration = 360L }
        val glowScaleY = ObjectAnimator.ofFloat(glow, View.SCALE_Y, 0.5f, 2.8f).apply { duration = 360L }
        tapWaveAnimator = AnimatorSet().apply {
            playTogether(scaleX, scaleY, glowScaleX, glowScaleY)
            play(alphaIn).before(alphaOut)
            play(glowAlphaIn).before(glowAlphaOut)
            start()
        }
        emitTapStars(
            centerX = lastTapX,
            centerY = lastTapY,
            color = tapColor,
            ringRadius = (binding.crystalTapWaveCircle.width * 0.5f) + dp(18),
        )
    }

    private fun currentRarityWaveColor(): Int {
        if (_binding == null) return Color.WHITE
        return if (binding.crystalRarityText.alpha > 0f) {
            binding.crystalRarityText.currentTextColor
        } else {
            Color.WHITE
        }
    }

    private fun emitTapStars(
        centerX: Float,
        centerY: Float,
        color: Int,
        ringRadius: Float,
        count: Int = 12,
        sizeRange: IntRange = 12..20,
    ) {
        val host = binding.crystalVideoRoot
        if (count <= 0) return
        // Yıldızlar çemberin DIŞINDAN başlasın.
        repeat(count) { i ->
            val baseAngle = (360f / count) * i
            val angle = Math.toRadians((baseAngle + Random.nextInt(-15, 15)).toDouble())
            val star = ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_tap_spark_star)
                setColorFilter(color, PorterDuff.Mode.SRC_IN)
                alpha = 0f
                val s = dp(Random.nextInt(sizeRange.first, sizeRange.last + 1))
                layoutParams = FrameLayout.LayoutParams(s, s)
                val startX = centerX + (cos(angle) * ringRadius).toFloat()
                val startY = centerY + (sin(angle) * ringRadius).toFloat()
                x = startX - s / 2f
                y = startY - s / 2f
                rotation = Random.nextInt(0, 360).toFloat()
            }
            host.addView(star)

            val distance = Random.nextInt(220, 420).toFloat()
            val dx = (cos(angle) * distance).toFloat()
            val dy = (sin(angle) * distance).toFloat()
            val duration = Random.nextInt(320, 520).toLong()

            star.animate()
                .alpha(1f)
                .setDuration(50L)
                .withEndAction {
                    star.animate()
                        .translationXBy(dx)
                        .translationYBy(dy)
                        .rotationBy(Random.nextInt(-90, 90).toFloat())
                        .alpha(0f)
                        .setDuration(duration)
                        .withEndAction { host.removeView(star) }
                        .start()
                }
                .start()
        }
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    private fun setupClickSound() {
        clickSoundPool?.release()
        clickSoundLoaded = false
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        clickSoundPool = SoundPool.Builder()
            .setAudioAttributes(attrs)
            .setMaxStreams(2)
            .build()
            .also { pool ->
                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0 && sampleId == clickSoundId) clickSoundLoaded = true
                }
                clickSoundId = pool.load(requireContext(), R.raw.crystal_click1, 1)
            }
    }

    private fun playClickSound() {
        if (!clickSoundLoaded || clickSoundId == 0) return
        clickSoundPool?.play(clickSoundId, 0.5f, 0.5f, 1, 0, 1f)
    }

    private fun maybeTriggerResumeTapRarityWave(continueTapCount: Int) {
        if (continueTapCount !in 1..2) return

        // Durağı geçiren tıkta göster; rarity kuralıyla eşleşmesi için +50ms renk güncellemesini baz al.
        binding.crystalVideoRoot.postDelayed({
            if (_binding == null) return@postDelayed
            val checkpointIndex = (continueTapCount - 1).coerceAtLeast(0)
            val positionForRarity = checkpointsMs[checkpointIndex] + 50L
            maybeUpdateRarityLabel(positionForRarity)
            val rarityColor = binding.crystalRarityText.currentTextColor
            showYellowWave(rarityColor)
        }, 50L)
    }

    private fun setupRarityLabel() {
        binding.crystalRarityText.alpha = 1f
        applyRarityTierUi(currentRarityTier)
    }

    private fun maybeUpdateRarityLabel(positionMs: Long) {
        val profile = profileFor(currentVideoName)
        if (profile.rarityBehavior == RarityBehavior.STATIC_COMMON) {
            if (currentRarityTier != RarityTier.COMMON) {
                currentRarityTier = RarityTier.COMMON
                applyRarityTierUi(currentRarityTier)
            }
            rarityCheckpointAppliedCount = 2
            return
        }

        while (rarityCheckpointAppliedCount < 2) {
            val checkpointMs = checkpointsMs.getOrNull(rarityCheckpointAppliedCount) ?: return
            val applyAt = checkpointMs + 50L
            if (positionMs < applyAt) return
            rarityCheckpointAppliedCount += 1
            currentRarityTier = nextRarityTierForCheckpoint(
                checkpointNumber = rarityCheckpointAppliedCount,
                currentTier = currentRarityTier,
                behavior = profile.rarityBehavior,
            )
            applyRarityTierUi(currentRarityTier)
        }
    }

    private fun nextRarityTierForCheckpoint(
        checkpointNumber: Int,
        currentTier: RarityTier,
        behavior: RarityBehavior,
    ): RarityTier {
        if (behavior == RarityBehavior.FORCE_EPIC_THEN_LEGENDARY) {
            return when {
                checkpointNumber >= 2 -> RarityTier.LEGENDARY
                checkpointNumber >= 1 -> RarityTier.EPIC
                else -> RarityTier.COMMON
            }
        }
        if (behavior == RarityBehavior.FORCE_EPIC_THEN_DESTANSI) {
            return when {
                checkpointNumber >= 2 -> RarityTier.DESTANSI
                checkpointNumber >= 1 -> RarityTier.EPIC
                else -> RarityTier.COMMON
            }
        }
        if (behavior == RarityBehavior.FORCE_EPIC_FROM_FIRST_CHECKPOINT) {
            return if (checkpointNumber >= 1) RarityTier.EPIC else RarityTier.COMMON
        }
        if (behavior == RarityBehavior.FORCE_DESTANSI_THEN_LEGENDARY) {
            return when {
                checkpointNumber >= 2 -> RarityTier.LEGENDARY
                checkpointNumber >= 1 -> RarityTier.DESTANSI
                else -> RarityTier.COMMON
            }
        }
        if (behavior == RarityBehavior.FORCE_DESTANSI_FROM_FIRST_CHECKPOINT) {
            return if (checkpointNumber >= 1) RarityTier.DESTANSI else RarityTier.COMMON
        }
        if (behavior == RarityBehavior.FORCE_EPIC_ON_SECOND_CHECKPOINT) {
            return if (checkpointNumber >= 2) RarityTier.EPIC else RarityTier.COMMON
        }
        if (behavior == RarityBehavior.FORCE_DESTANSI_ON_SECOND_CHECKPOINT) {
            return if (checkpointNumber >= 2) RarityTier.DESTANSI else RarityTier.COMMON
        }

        val candidates = when (checkpointNumber) {
            1 -> listOf(RarityTier.COMMON, RarityTier.EPIC, RarityTier.DESTANSI)
            2 -> listOf(currentTier, RarityTier.EPIC, RarityTier.DESTANSI, RarityTier.LEGENDARY)
            else -> listOf(currentTier)
        }
            .distinct()
            .filter { it.rank >= currentTier.rank }
        if (candidates.isEmpty()) return currentTier
        return candidates.random()
    }

    private fun profileFor(videoName: String): VideoProfile {
        return videoProfiles[videoName] ?: VideoProfile(rarityBehavior = RarityBehavior.CHECKPOINT_RANDOM_PROGRESS)
    }

    private fun applyRarityTierUi(tier: RarityTier) {
        if (_binding == null) return
        binding.crystalRarityText.text = tier.label
        binding.crystalRarityText.setTextColor(tier.color)
    }

    private fun resolveInitialVideoResId(requestedVideoName: String?): Int {
        val explicitName = requestedVideoName?.takeIf { it.isNotBlank() }
        if (explicitName != null) {
            return resolveVideoResId(explicitName)
        }

        // İlk video tercihi: crystal_blue_blue varsa onu kullan.
        val preferredFirstName = "crystal_blue_blue"
        val preferredFirstId = requireContext().resources.getIdentifier(
            preferredFirstName,
            "raw",
            requireContext().packageName,
        )
        if (preferredFirstId != 0) return preferredFirstId

        // Genel fallback: crystal_* adındaki ilk raw videoyu seç.
        val rawFields = R.raw::class.java.fields
        val firstCrystalName = rawFields
            .mapNotNull { it.name }
            .firstOrNull { it.startsWith("crystal_") }
            ?: throw IllegalStateException("raw içinde crystal_* isimli video bulunamadı.")
        return resolveVideoResId(firstCrystalName)
    }

    private fun resolveVideoResId(baseName: String): Int {
        val res = requireContext().resources
        val pkg = requireContext().packageName
        val primary = res.getIdentifier(baseName, "raw", pkg)
        if (primary != 0) return primary
        throw IllegalStateException("Raw video bulunamadı: $baseName(.mp4)")
    }

    private fun maybeStartIdlePulse() {
        if (_binding == null) return
        // Son kapanış tıkı (videoEnded=true) hariç, tüm bekleme duraklarında baloncuk efekti.
        if (!waitingTapToContinue || videoEnded) return
        if (idlePulse != null) return
        val upX = ObjectAnimator.ofFloat(binding.crystalPlayerView, View.SCALE_X, 1.0f, 1.035f).apply {
            duration = 480L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        val upY = ObjectAnimator.ofFloat(binding.crystalPlayerView, View.SCALE_Y, 1.0f, 1.035f).apply {
            duration = 480L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        idlePulse = AnimatorSet().apply {
            playTogether(upX, upY)
            start()
        }
    }

    private fun stopIdlePulse(resetScale: Boolean) {
        idlePulse?.cancel()
        idlePulse = null
        if (resetScale && _binding != null) {
            binding.crystalPlayerView.scaleX = 1f
            binding.crystalPlayerView.scaleY = 1f
        }
    }

    private fun showYellowWave(color: Int) {
        if (_binding == null) return
        val ring = binding.crystalTapWaveYellowCircle
        val glow = binding.crystalTapWaveYellowGlow
        yellowWaveAnimator?.cancel()
        ring.background?.mutate()?.setTint(color)
        glow.background?.mutate()?.setTint(color)
        ring.backgroundTintList = ColorStateList.valueOf(color)
        glow.backgroundTintList = ColorStateList.valueOf(color)
        val anchorX = binding.crystalVideoRoot.width / 2f
        val anchorY = binding.crystalVideoRoot.height / 2f
        ring.x = anchorX - ring.width / 2f
        ring.y = anchorY - ring.height / 2f
        glow.x = anchorX - glow.width / 2f
        glow.y = anchorY - glow.height / 2f
        ring.scaleX = 0.8f
        ring.scaleY = 0.8f
        ring.alpha = 0f
        glow.scaleX = 0.75f
        glow.scaleY = 0.75f
        glow.alpha = 0f

        val ringIn = ObjectAnimator.ofFloat(ring, View.ALPHA, 0f, 1f).apply { duration = 70L }
        val ringOut = ObjectAnimator.ofFloat(ring, View.ALPHA, 1f, 0f).apply { duration = 270L }
        val ringScaleX = ObjectAnimator.ofFloat(ring, View.SCALE_X, 0.8f, 8.8f).apply { duration = 340L }
        val ringScaleY = ObjectAnimator.ofFloat(ring, View.SCALE_Y, 0.8f, 8.8f).apply { duration = 340L }
        val glowIn = ObjectAnimator.ofFloat(glow, View.ALPHA, 0f, 1f).apply { duration = 70L }
        val glowOut = ObjectAnimator.ofFloat(glow, View.ALPHA, 1f, 0f).apply { duration = 300L }
        val glowScaleX = ObjectAnimator.ofFloat(glow, View.SCALE_X, 0.75f, 7.8f).apply { duration = 360L }
        val glowScaleY = ObjectAnimator.ofFloat(glow, View.SCALE_Y, 0.75f, 7.8f).apply { duration = 360L }

        yellowWaveAnimator = AnimatorSet().apply {
            playTogether(ringScaleX, ringScaleY, glowScaleX, glowScaleY)
            play(ringIn).before(ringOut)
            play(glowIn).before(glowOut)
            start()
        }

        val rarityWaveStarCount = when (currentRarityTier) {
            RarityTier.COMMON -> 0
            RarityTier.EPIC -> 12
            RarityTier.DESTANSI -> 28
            RarityTier.LEGENDARY -> 50
        }
        emitTapStars(
            centerX = anchorX,
            centerY = anchorY,
            color = color,
            ringRadius = (ring.width * 0.5f) + dp(26),
            count = rarityWaveStarCount,
            sizeRange = 16..28,
        )
        animateVideoShockwave()
    }

    private fun animateVideoShockwave() {
        if (_binding == null) return
        val playerView = binding.crystalPlayerView
        videoWaveAnimator?.cancel()
        val sx = ObjectAnimator.ofFloat(
            playerView,
            View.SCALE_X,
            1f,
            1.04f,
            0.972f,
            1.028f,
            0.986f,
            1f,
        ).apply {
            duration = 420L
        }
        val sy = ObjectAnimator.ofFloat(
            playerView,
            View.SCALE_Y,
            1f,
            1.052f,
            0.964f,
            1.034f,
            0.982f,
            1f,
        ).apply {
            duration = 420L
        }
        videoWaveAnimator = AnimatorSet().apply {
            playTogether(sx, sy)
            start()
        }
    }

    companion object {
        private const val STATE_NEXT_CHECKPOINT_INDEX = "state_next_checkpoint_index"
        private const val STATE_CONTINUE_TAP_COUNT = "state_continue_tap_count"
        private const val STATE_PLAY_PHASE_TAP_COUNT = "state_play_phase_tap_count"
        private const val STATE_PENDING_AUTO_CONTINUES = "state_pending_auto_continues"
        private const val STATE_VIDEO_RES_ID = "state_video_res_id"
        private const val STATE_VIDEO_NAME = "state_video_name"
        private const val ARG_VIDEO_NAME = "arg_video_name"
        private const val STATE_RARITY_TIER = "state_rarity_tier"
        private const val STATE_RARITY_CHECKPOINT_APPLIED_COUNT = "state_rarity_checkpoint_applied_count"
        private const val STATE_WAITING_TAP = "state_waiting_tap"
        private const val STATE_VIDEO_ENDED = "state_video_ended"

        fun newInstance(videoName: String? = null): CrystalBreakVideoFragment = CrystalBreakVideoFragment().apply {
            arguments = Bundle().apply {
                if (!videoName.isNullOrBlank()) putString(ARG_VIDEO_NAME, videoName)
            }
        }
    }
}

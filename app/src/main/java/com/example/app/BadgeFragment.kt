package com.example.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.example.app.databinding.FragmentBadgeBinding

class BadgeFragment : Fragment() {

    companion object {
        private const val TAG = "BadgeFragmentDebug"
        private const val ARG_INITIAL_MODE = "initial_mode"
        private const val ARG_CELEBRATE_LEVEL_UP = "arg_celebrate_level_up"
        private const val ARG_CELEBRATE_FROM_PROGRESS = "arg_celebrate_from_progress"
        private const val ARG_CELEBRATE_TO_PROGRESS = "arg_celebrate_to_progress"
        private const val ARG_CELEBRATE_TARGET = "arg_celebrate_target"
        private const val ARG_CELEBRATE_QUEUE = "arg_celebrate_queue"
        private const val ARG_CELEBRATE_INDEX = "arg_celebrate_index"
        /** Kutlama kuyruğu bittiğinde [MainActivity.onSeasonLeaderboardBadgeCelebrationFinished] tetiklenir. */
        private const val ARG_SEASON_LB_ACK_SEASON = "arg_season_lb_ack_season"
        private const val ROCKET_START_OFFSET_MS = 800L
        private const val REWARD_AMOUNT_SCALE_MS = 400L
        private const val TOP_HOLD_FRAME_DART = 60f
        private const val TOP_HOLD_FRAME_ROCKET = 60f
        private const val TOP_HOLD_FRAME_BOWLING = 47f
        private const val TOP_HOLD_FRAME_GOLF = 30f
        private const val TOP_HOLD_FRAME_FISHING = 24f
        private const val CUP_GOOGLE_HOLD_FRAME = 135f
        private const val KARATE_HOLD_FRAME = 1f
        private const val KARATE_REPLAY_START_FRAME = 40f
        /** cupGoogleAnim: ödül rakamı slotunu ekstra aşağı kaydırır (dp). */
        private const val SINGLE_TOP_LOTTIE_REWARD_SLOT_EXTRA_DOWN_DP = 50f
        /** Karate: sayı ve detay metni bu kadar dp yukarı (eksi translationY). */
        private const val KARATE_REWARD_SLOT_OFFSET_UP_DP = 20f

        /**
         * unlocked=true olduğunda gösterilecek metinler.
         */
        private val unlockedDetailTextOverrides = mapOf(
            BadgeAnimMode.DAILY_ONLY to "x dersi hatasız tamamlayarak Kusursuz Odak rozetini kazandın.",
            BadgeAnimMode.ROCKET_WITH_BASE to "x dersi 1 günde tamamlayarak Sınır Tanımaz rozetini kazandın.",
            BadgeAnimMode.BOWLING_WITH_BASE to "x günlük görev tamamlayarak Strike Ustası rozetini kazandın.",
            BadgeAnimMode.GOLF_WITH_BASE to "x kere eğitmene danışarak Nokta Atışı rozetini kazandın.",
            BadgeAnimMode.FISHING_WITH_BASE to "x gün üst üste Günlük Soru mücadelesini tamamlayarak Derin Avcı rozetini kazandın.",
            BadgeAnimMode.CUP_GOOGLE to "Ünite Maratonunda x. olarak Elit Seviye kupasını kazandın.",
            BadgeAnimMode.KARATE to "x Ünite Maratonunu 3 yıldız ile tamamlayarak Siyah Kuşak rozetini kazandın.",
            BadgeAnimMode.GOLD_MEDAL to "Ünite maratonunda 1. olarak Altın Madalya kazandın.",
            BadgeAnimMode.SILVER_MEDAL to "Ünite maratonunda 2. olarak Gümüş Madalya kazandın.",
            BadgeAnimMode.BRONZE_MEDAL to "Ünite maratonunda 3. olarak Bronz Madalya kazandın.",
        )

        /**
         * unlocked=false olduğunda gösterilecek metinler.
         */
        private val lockedDetailTextOverrides = mapOf(
            BadgeAnimMode.DAILY_ONLY to "Bu rozeti kazanmak için 3 dersi hatasız tamamlayarak başarı elde et.",
            BadgeAnimMode.ROCKET_WITH_BASE to "Bu rozeti kazanmak için 3 dersi 1 günde tamamlayarak başarı elde et.",
            BadgeAnimMode.BOWLING_WITH_BASE to "Bu rozeti kazanmak için 5 günlük görevi tamamlayarak başarı elde et.",
            BadgeAnimMode.GOLF_WITH_BASE to "Bu rozeti kazanmak için 5 kere öğretmene danış.",
            BadgeAnimMode.FISHING_WITH_BASE to "Bu rozeti kazanmak için 3 gün üst üste Günlük Soru mücadelesini çözerek başarı elde et.",
            BadgeAnimMode.CUP_GOOGLE to "Bu kupayı kazanmak için ünite maratonunda ilk 100'e girerek başarı elde et.",
            BadgeAnimMode.KARATE to "Bu rozeti kazanmak için ünite maratonunu 3 yıldız ile tamamlayarak başarı elde et.",
            BadgeAnimMode.GOLD_MEDAL to "Bu madalyayı kazanmak için bir ünite maratonu liderliğinde birinci ol.",
            BadgeAnimMode.SILVER_MEDAL to "Bu madalyayı kazanmak için bir ünite maratonu liderliğinde ikinci ol.",
            BadgeAnimMode.BRONZE_MEDAL to "Bu madalyayı kazanmak için bir ünite maratonu liderliğinde üçüncü ol.",
        )

        fun newInstance(initialMode: BadgeAnimMode, unlocked: Boolean = true): BadgeFragment {
            return BadgeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_MODE, initialMode.name)
                    putBoolean("arg_unlocked_legacy", unlocked)
                }
            }
        }

        fun newLevelUpInstance(
            mode: BadgeAnimMode,
            fromProgress: Int,
            toProgress: Int,
            reachedTarget: Int,
        ): BadgeFragment {
            return BadgeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_MODE, mode.name)
                    putBoolean("arg_unlocked_legacy", true)
                    putBoolean(ARG_CELEBRATE_LEVEL_UP, true)
                    putInt(ARG_CELEBRATE_FROM_PROGRESS, fromProgress)
                    putInt(ARG_CELEBRATE_TO_PROGRESS, toProgress)
                    putInt(ARG_CELEBRATE_TARGET, reachedTarget)
                }
            }
        }

        fun newLevelUpSequenceInstance(
            payloads: List<String>,
            index: Int = 0,
            seasonLeaderboardAckAfterQueue: Int? = null,
        ): BadgeFragment {
            val current = payloads.getOrNull(index) ?: "DAILY_ONLY|0|0|0"
            val parts = current.split("|")
            val mode = parts.getOrNull(0)?.let { runCatching { BadgeAnimMode.valueOf(it) }.getOrNull() }
                ?: BadgeAnimMode.DAILY_ONLY
            val from = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val to = parts.getOrNull(2)?.toIntOrNull() ?: 0
            val target = parts.getOrNull(3)?.toIntOrNull() ?: 0
            return BadgeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_MODE, mode.name)
                    putBoolean("arg_unlocked_legacy", true)
                    putBoolean(ARG_CELEBRATE_LEVEL_UP, true)
                    putInt(ARG_CELEBRATE_FROM_PROGRESS, from)
                    putInt(ARG_CELEBRATE_TO_PROGRESS, to)
                    putInt(ARG_CELEBRATE_TARGET, target)
                    putStringArrayList(ARG_CELEBRATE_QUEUE, ArrayList(payloads))
                    putInt(ARG_CELEBRATE_INDEX, index)
                    if (seasonLeaderboardAckAfterQueue != null) {
                        putInt(ARG_SEASON_LB_ACK_SEASON, seasonLeaderboardAckAfterQueue)
                    }
                }
            }
        }
    }

    private var _binding: FragmentBadgeBinding? = null
    private var isLoopActive = false
    private var rocketStartProgress = 0f
    private var currentMode: BadgeAnimMode = BadgeAnimMode.DAILY_ONLY
    private var modeSessionId = 0
    private var pendingTopStartRunnable: Runnable? = null
    private var pendingShowTopRunnable: Runnable? = null
    private var pendingTopScaleAnimator: Animator? = null
    private var rewardAmountScaleAnimator: Animator? = null
    private var currentRewardAmount: Int? = 50
    private var isUnlockedState: Boolean = true
    private var isCelebrationFlow = false
    private var backPressedCallback: OnBackPressedCallback? = null
    private var lockedProgressCurrent: Int = 2
    private var lockedProgressTarget: Int = 10
    /** Açık parça paneli hangi mod + kupa/madalya anahtarı (aynı butona tekrar basınca kapanır). */
    private var piecePanelModeKey: String? = null
    private val lockedBaseColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_locked_base)
    private val lockedTopColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_locked_top)
    private val bronzeToneColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_tone_bronze_filter)
    private val silverToneColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_tone_silver_filter)
    private val redToneColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_tone_red_filter)
    private val purpleToneColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.badge_tone_purple_filter)
    private val binding: FragmentBadgeBinding
        get() = _binding!!

    enum class BadgeAnimMode {
        DAILY_ONLY,
        ROCKET_WITH_BASE,
        BOWLING_WITH_BASE,
        GOLF_WITH_BASE,
        FISHING_WITH_BASE,
        CUP_GOOGLE,
        KARATE,
        GOLD_MEDAL,
        SILVER_MEDAL,
        BRONZE_MEDAL,
    }

    /** Bu modlarda tam ekran tıklama replay’i [medalPieceNamesList] ile çakışmasın diye root/daily kapalı; replay [rocketBadgeAnim] üzerinde. */
    private val pieceReplayModes = setOf(
        BadgeAnimMode.GOLD_MEDAL,
        BadgeAnimMode.SILVER_MEDAL,
        BadgeAnimMode.BRONZE_MEDAL,
        BadgeAnimMode.CUP_GOOGLE,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBadgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isLoopActive = true
        val initialMode = arguments?.getString(ARG_INITIAL_MODE)
            ?.let { runCatching { BadgeAnimMode.valueOf(it) }.getOrNull() }
            ?: BadgeAnimMode.DAILY_ONLY
        val sharedState = BadgeProgressRepository.getStateByMode(initialMode)
        isUnlockedState = sharedState?.unlocked ?: true
        val rewardAmount = if (initialMode == BadgeAnimMode.KARATE && !isUnlockedState) {
            1
        } else {
            sharedState?.value ?: defaultRewardAmountForMode(initialMode)
        }
        isCelebrationFlow = arguments?.getBoolean(ARG_CELEBRATE_LEVEL_UP, false) == true
        configureBackPressBehavior()

        if (isCelebrationFlow) {
            binding.root.setOnClickListener(null)
            binding.root.isClickable = true
            binding.dailyTasksCompliteBadge.setOnClickListener(null)
            binding.dailyTasksCompliteBadge.isClickable = true
            binding.quitButton.visibility = View.GONE
        } else if (isUnlockedState) {
            if (!isCelebrationFlow && initialMode in pieceReplayModes) {
                // Root tıklanabilir kalmalı; false iken dokunuşlar coordinator / BadgeDetail'e sızar.
                binding.root.isClickable = true
                binding.root.setOnClickListener { }
                binding.dailyTasksCompliteBadge.isClickable = true
                // Tam ekran alt Lottie alanı (madalyada alpha=0 olsa da); parça kartı açıkken kapanıp replay için.
                binding.dailyTasksCompliteBadge.setOnClickListener {
                    replayTopAnimationOnly()
                }
            } else {
                binding.root.isClickable = true
                binding.root.setOnClickListener {
                    replayTopAnimationOnly()
                }
                binding.dailyTasksCompliteBadge.isClickable = true
                binding.dailyTasksCompliteBadge.setOnClickListener {
                    replayTopAnimationOnly()
                }
            }
            binding.quitButton.visibility = View.VISIBLE
        } else {
            binding.root.setOnClickListener(null)
            binding.root.isClickable = true
            binding.dailyTasksCompliteBadge.setOnClickListener(null)
            binding.dailyTasksCompliteBadge.isClickable = true
            binding.quitButton.visibility = View.VISIBLE
        }
        updateProgressContainerVisibility(initialMode)
        binding.quitButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Panel açıkken ekrana dokunulunca (panel dışına) paneli kapat.
        // Panel içi dokunuşlarda liste scroll/tap akışını bozmayalım.
        binding.root.setOnTouchListener { _, event ->
            if (event.action != MotionEvent.ACTION_DOWN) return@setOnTouchListener false
            val b = _binding ?: return@setOnTouchListener false
            if (b.badgePiecePanel.visibility != View.VISIBLE) return@setOnTouchListener false

            val touchInsidePanel = isTouchInsideView(event, b.badgePiecePanel)
            val touchInsideToggle = isTouchInsideView(event, b.medalPieceNamesList)
            if (!touchInsidePanel && !touchInsideToggle) {
                collapsePieceRowsPanel()
            }
            false
        }
        if (isCelebrationFlow && (shouldShowProgressForMode(initialMode) || initialMode == BadgeAnimMode.KARATE)) {
            startLevelUpCelebration(initialMode, rewardAmount)
            return
        }
        if (isUnlockedState) {
            switchBadgeAnimation(initialMode, rewardAmount = rewardAmount)
            if (isCelebrationFlow) {
                bindCelebrationTapToAdvanceOrClose()
            }
        } else {
            showLockedStaticBadge(initialMode, rewardAmount)
        }
    }

    /** Madalya / kupa kutlamasında [startLevelUpCelebration] çalışmadığı için kuyruk ilerletme veya kapatma tıklaması burada bağlanır. */
    private fun bindCelebrationTapToAdvanceOrClose() {
        if (!isCelebrationFlow) return
        val b = _binding ?: return
        val listener = View.OnClickListener {
            if (hasNextCelebrationInQueue()) {
                goToNextCelebrationInQueue()
            } else {
                closeCelebrationFragmentWithSlide()
            }
        }
        b.root.setOnClickListener(listener)
        b.dailyTasksCompliteBadge.setOnClickListener(listener)
        b.rocketBadgeAnim.setOnClickListener(listener)
        b.root.isClickable = true
        b.dailyTasksCompliteBadge.isClickable = true
        b.rocketBadgeAnim.isClickable = true
    }

    private fun defaultRewardAmountForMode(mode: BadgeAnimMode): Int? {
        return when (mode) {
            BadgeAnimMode.DAILY_ONLY -> 50
            BadgeAnimMode.ROCKET_WITH_BASE -> 100
            BadgeAnimMode.BOWLING_WITH_BASE -> 150
            BadgeAnimMode.FISHING_WITH_BASE -> 3
            BadgeAnimMode.GOLF_WITH_BASE -> 225
            BadgeAnimMode.CUP_GOOGLE -> 87
            BadgeAnimMode.KARATE -> 1
            BadgeAnimMode.GOLD_MEDAL,
            BadgeAnimMode.SILVER_MEDAL,
            BadgeAnimMode.BRONZE_MEDAL -> null
        }
    }

    private fun startBadgeLoopCycle() {
        val safeBinding = _binding ?: return
        safeBinding.dailyTasksCompliteBadge.progress = 0f
        safeBinding.rocketBadgeAnim.setMinAndMaxProgress(rocketStartProgress, 1f)
        safeBinding.rocketBadgeAnim.progress = rocketStartProgress
        safeBinding.dailyTasksCompliteBadge.playAnimation()
        safeBinding.rocketBadgeAnim.playAnimation()
    }

    private fun startTopOnlyCycle() {
        val safeBinding = _binding ?: return
        safeBinding.rocketBadgeAnim.setMinAndMaxProgress(rocketStartProgress, 1f)
        safeBinding.rocketBadgeAnim.progress = rocketStartProgress
        safeBinding.rocketBadgeAnim.visibility = View.VISIBLE
        safeBinding.rocketBadgeAnim.playAnimation()
    }

    /**
     * Tek tur çalışır; üst animasyon bitince akış durur.
     */
    fun rocketBadgeAnim(
        baseAssetFile: String = "daily_tasks_complite_badge2.json",
        topAssetFile: String = "rocket_badge_anim2.json",
        rocketStartOffsetMs: Long = ROCKET_START_OFFSET_MS,
        topScaleX: Float = 1f,
        topScaleY: Float = 1f,
        rewardAmount: Int? = null,
    ) {
        val safeBinding = _binding ?: return
        applyRewardAmount(rewardAmount)
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        applyRewardSlotExtraDownOffset(false)
        safeBinding.dailyTasksCompliteBadge.alpha = 1f
        safeBinding.rocketBadgeAnim.scaleX = topScaleX
        safeBinding.rocketBadgeAnim.scaleY = topScaleY
        safeBinding.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
        safeBinding.rocketBadgeAnim.background = null
        safeBinding.rocketBadgeAnim.clipToOutline = false
        safeBinding.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
        safeBinding.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        safeBinding.rocketBadgeAnim.removeAllAnimatorListeners()
        safeBinding.dailyTasksCompliteBadge.cancelAnimation()
        safeBinding.rocketBadgeAnim.cancelAnimation()
        safeBinding.dailyTasksCompliteBadge.setAnimation(baseAssetFile)
        safeBinding.rocketBadgeAnim.setAnimation(topAssetFile)
        safeBinding.dailyTasksCompliteBadge.repeatCount = 0
        safeBinding.rocketBadgeAnim.repeatCount = 0
        safeBinding.rocketBadgeAnim.speed = 1f
        rocketStartProgress = 0f
        var topHoldProgress = 1f

        safeBinding.dailyTasksCompliteBadge.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val currentBinding = _binding ?: return
                if (!isLoopActive) return
                currentBinding.dailyTasksCompliteBadge.progress = 1f
                currentBinding.dailyTasksCompliteBadge.pauseAnimation()
            }
        })
        safeBinding.rocketBadgeAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val currentBinding = _binding ?: return
                if (!isLoopActive) return
                currentBinding.rocketBadgeAnim.progress = topHoldProgress
                currentBinding.rocketBadgeAnim.pauseAnimation()
            }
        })
        safeBinding.rocketBadgeAnim.addLottieOnCompositionLoadedListener { composition ->
            if (!isLoopActive || sessionId != modeSessionId) return@addLottieOnCompositionLoadedListener
            val durationMs = composition.duration.toLong().coerceAtLeast(1L)
            val normalizedOffsetMs = (rocketStartOffsetMs % durationMs).toFloat()
            rocketStartProgress = normalizedOffsetMs / durationMs.toFloat()
            topHoldProgress = frameToProgress(TOP_HOLD_FRAME_ROCKET, composition.startFrame, composition.endFrame)
            if (isLoopActive) startBadgeLoopCycle()
        }
        safeBinding.rocketBadgeAnim.visibility = View.VISIBLE
    }

    fun golfAnim(
        baseAssetFile: String = "daily_tasks_complite_badge2.json",
        topAssetFile: String = "golf_anim.json",
        golfStartOffsetMs: Long = 0L,
        topStartDelayMs: Long = 800L,
        topScale: Float = 0.60f,
        rewardAmount: Int? = null,
    ) {
        val safeBinding = _binding ?: return
        applyRewardAmount(rewardAmount)
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        applyRewardSlotExtraDownOffset(false)
        safeBinding.rocketBadgeAnim.scaleX = topScale
        safeBinding.rocketBadgeAnim.scaleY = topScale
        safeBinding.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
        safeBinding.rocketBadgeAnim.background = null
        safeBinding.rocketBadgeAnim.clipToOutline = false
        safeBinding.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
        safeBinding.dailyTasksCompliteBadge.alpha = 1f
        safeBinding.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        safeBinding.rocketBadgeAnim.removeAllAnimatorListeners()
        safeBinding.dailyTasksCompliteBadge.cancelAnimation()
        safeBinding.rocketBadgeAnim.cancelAnimation()
        safeBinding.dailyTasksCompliteBadge.setAnimation(baseAssetFile)
        safeBinding.rocketBadgeAnim.setAnimation(topAssetFile)
        safeBinding.dailyTasksCompliteBadge.repeatCount = 0
        safeBinding.rocketBadgeAnim.repeatCount = 0
        safeBinding.rocketBadgeAnim.speed = 1f
        safeBinding.rocketBadgeAnim.visibility = View.INVISIBLE
        rocketStartProgress = 0f
        var topHoldProgress = 1f

        safeBinding.dailyTasksCompliteBadge.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val currentBinding = _binding ?: return
                if (!isLoopActive) return
                currentBinding.dailyTasksCompliteBadge.progress = 1f
                currentBinding.dailyTasksCompliteBadge.pauseAnimation()
            }
        })
        safeBinding.rocketBadgeAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val currentBinding = _binding ?: return
                if (!isLoopActive) return
                currentBinding.rocketBadgeAnim.progress = topHoldProgress
                currentBinding.rocketBadgeAnim.pauseAnimation()
            }
        })
        safeBinding.rocketBadgeAnim.addLottieOnCompositionLoadedListener { composition ->
            if (!isLoopActive || sessionId != modeSessionId) return@addLottieOnCompositionLoadedListener
            val durationMs = composition.duration.toLong().coerceAtLeast(1L)
            val normalizedOffsetMs = (golfStartOffsetMs % durationMs).toFloat()
            rocketStartProgress = normalizedOffsetMs / durationMs.toFloat()
            topHoldProgress = frameToProgress(TOP_HOLD_FRAME_GOLF, composition.startFrame, composition.endFrame)
            if (isLoopActive) {
                startDelayedTopLoopCycle(
                    topStartDelayMs = topStartDelayMs,
                    growTopScaleOverDelay = true,
                    topTargetScale = topScale,
                )
            }
        }
    }

    fun fishingAnim(
        baseAssetFile: String = "daily_tasks_complite_badge2.json",
        topAssetFile: String = "fishing_pole_anim.json",
        fishingStartOffsetMs: Long = 0L,
        topStartDelayMs: Long = 800L,
        topScale: Float = 0.65f,
        rewardAmount: Int? = null,
    ) {
        val safeBinding = _binding ?: return
        applyRewardAmount(rewardAmount)
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        applyRewardSlotExtraDownOffset(false)
        safeBinding.rocketBadgeAnim.scaleX = topScale
        safeBinding.rocketBadgeAnim.scaleY = topScale
        safeBinding.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
        safeBinding.rocketBadgeAnim.background = null
        safeBinding.rocketBadgeAnim.clipToOutline = false
        safeBinding.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
        safeBinding.dailyTasksCompliteBadge.alpha = 1f
        safeBinding.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        safeBinding.rocketBadgeAnim.removeAllAnimatorListeners()
        safeBinding.dailyTasksCompliteBadge.cancelAnimation()
        safeBinding.rocketBadgeAnim.cancelAnimation()
        safeBinding.dailyTasksCompliteBadge.setAnimation(baseAssetFile)
        safeBinding.rocketBadgeAnim.setAnimation(topAssetFile)
        safeBinding.dailyTasksCompliteBadge.repeatCount = 0
        safeBinding.rocketBadgeAnim.repeatCount = 0
        safeBinding.rocketBadgeAnim.speed = 1f
        safeBinding.rocketBadgeAnim.visibility = View.INVISIBLE
        rocketStartProgress = 0f
        var topHoldProgress = 1f

        safeBinding.dailyTasksCompliteBadge.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val currentBinding = _binding ?: return
                if (!isLoopActive) return
                currentBinding.dailyTasksCompliteBadge.progress = 1f
                currentBinding.dailyTasksCompliteBadge.pauseAnimation()
            }
        })
        safeBinding.rocketBadgeAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val currentBinding = _binding ?: return
                if (!isLoopActive) return
                currentBinding.rocketBadgeAnim.progress = topHoldProgress
                currentBinding.rocketBadgeAnim.pauseAnimation()
            }
        })
        safeBinding.rocketBadgeAnim.addLottieOnCompositionLoadedListener { composition ->
            if (!isLoopActive || sessionId != modeSessionId) return@addLottieOnCompositionLoadedListener
            val durationMs = composition.duration.toLong().coerceAtLeast(1L)
            val normalizedOffsetMs = (fishingStartOffsetMs % durationMs).toFloat()
            rocketStartProgress = normalizedOffsetMs / durationMs.toFloat()
            topHoldProgress = frameToProgress(TOP_HOLD_FRAME_FISHING, composition.startFrame, composition.endFrame)
            if (isLoopActive) {
                startDelayedTopLoopCycle(
                    topStartDelayMs = topStartDelayMs,
                    growTopScaleOverDelay = true,
                    topTargetScale = topScale,
                )
            }
        }
    }

    fun bowlingAnim(
        baseAssetFile: String = "daily_tasks_complite_badge2.json",
        topAssetFile: String = "bowling_anim.json",
        bowlingStartOffsetMs: Long = 0L,
        topStartDelayMs: Long = 1000L,
        rewardAmount: Int? = null,
    ) {
        val safeBinding = _binding ?: return
        applyRewardAmount(rewardAmount)
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        applyRewardSlotExtraDownOffset(false)
        safeBinding.dailyTasksCompliteBadge.alpha = 1f
        safeBinding.rocketBadgeAnim.scaleX = 0.68f
        safeBinding.rocketBadgeAnim.scaleY = 0.68f
        safeBinding.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
        safeBinding.rocketBadgeAnim.setBackgroundResource(R.drawable.bg_badge_circle_frame)
        safeBinding.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BACKGROUND
        safeBinding.rocketBadgeAnim.clipToOutline = true
        safeBinding.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        safeBinding.rocketBadgeAnim.removeAllAnimatorListeners()
        safeBinding.dailyTasksCompliteBadge.cancelAnimation()
        safeBinding.rocketBadgeAnim.cancelAnimation()
        safeBinding.dailyTasksCompliteBadge.setAnimation(baseAssetFile)
        safeBinding.rocketBadgeAnim.setAnimation(topAssetFile)
        safeBinding.dailyTasksCompliteBadge.repeatCount = 0
        safeBinding.rocketBadgeAnim.repeatCount = 0
        safeBinding.rocketBadgeAnim.speed = 0.5f
        safeBinding.rocketBadgeAnim.visibility = View.INVISIBLE
        rocketStartProgress = 0f
        var topHoldProgress = 1f

        safeBinding.dailyTasksCompliteBadge.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val currentBinding = _binding ?: return
                if (!isLoopActive) return
                currentBinding.dailyTasksCompliteBadge.progress = 1f
                currentBinding.dailyTasksCompliteBadge.pauseAnimation()
            }
        })
        safeBinding.rocketBadgeAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val currentBinding = _binding ?: return
                if (!isLoopActive) return
                currentBinding.rocketBadgeAnim.progress = topHoldProgress
                currentBinding.rocketBadgeAnim.pauseAnimation()
            }
        })
        safeBinding.rocketBadgeAnim.addLottieOnCompositionLoadedListener { composition ->
            if (!isLoopActive || sessionId != modeSessionId) return@addLottieOnCompositionLoadedListener
            val durationMs = composition.duration.toLong().coerceAtLeast(1L)
            val normalizedOffsetMs = (bowlingStartOffsetMs % durationMs).toFloat()
            rocketStartProgress = normalizedOffsetMs / durationMs.toFloat()
            topHoldProgress = frameToProgress(TOP_HOLD_FRAME_BOWLING, composition.startFrame, composition.endFrame)
            if (isLoopActive) {
                startDelayedTopLoopCycle(
                    topStartDelayMs = topStartDelayMs,
                    growTopScaleOverDelay = false,
                    topTargetScale = 0.68f,
                )
            }
        }
    }

    fun dartAnim(
        baseAssetFile: String = "daily_tasks_complite_badge2.json",
        topAssetFile: String = "dart_anim.json",
        dailyStartOffsetMs: Long = 0L,
        topStartDelayMs: Long = 800L,
        topScale: Float = 0.65f,
        rewardAmount: Int? = null,
    ) {
        val safeBinding = _binding ?: return
        applyRewardAmount(rewardAmount)
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        applyRewardSlotExtraDownOffset(false)
        safeBinding.dailyTasksCompliteBadge.alpha = 1f
        safeBinding.rocketBadgeAnim.scaleX = 1f
        safeBinding.rocketBadgeAnim.scaleY = 1f
        safeBinding.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
        safeBinding.rocketBadgeAnim.background = null
        safeBinding.rocketBadgeAnim.clipToOutline = false
        safeBinding.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
        safeBinding.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        safeBinding.rocketBadgeAnim.removeAllAnimatorListeners()
        safeBinding.dailyTasksCompliteBadge.cancelAnimation()
        safeBinding.rocketBadgeAnim.cancelAnimation()

        safeBinding.dailyTasksCompliteBadge.setAnimation(baseAssetFile)
        safeBinding.rocketBadgeAnim.setAnimation(topAssetFile)
        safeBinding.dailyTasksCompliteBadge.repeatCount = 0
        safeBinding.rocketBadgeAnim.repeatCount = 0
        safeBinding.rocketBadgeAnim.speed = 1f
        safeBinding.rocketBadgeAnim.visibility = View.INVISIBLE
        rocketStartProgress = 0f
        var topHoldProgress = 1f
        safeBinding.dailyTasksCompliteBadge.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val currentBinding = _binding ?: return
                if (!isLoopActive) return
                currentBinding.dailyTasksCompliteBadge.progress = 1f
                currentBinding.dailyTasksCompliteBadge.pauseAnimation()
            }
        })
        safeBinding.rocketBadgeAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val currentBinding = _binding ?: return
                if (!isLoopActive) return
                currentBinding.rocketBadgeAnim.progress = topHoldProgress
                currentBinding.rocketBadgeAnim.pauseAnimation()
            }
        })
        safeBinding.rocketBadgeAnim.addLottieOnCompositionLoadedListener { composition ->
            if (!isLoopActive || sessionId != modeSessionId) return@addLottieOnCompositionLoadedListener
            val durationMs = composition.duration.toLong().coerceAtLeast(1L)
            val normalizedOffsetMs = (dailyStartOffsetMs % durationMs).toFloat()
            rocketStartProgress = normalizedOffsetMs / durationMs.toFloat()
            topHoldProgress = frameToProgress(TOP_HOLD_FRAME_DART, composition.startFrame, composition.endFrame)
            if (isLoopActive) {
                startDelayedTopLoopCycle(
                    topStartDelayMs = topStartDelayMs,
                    growTopScaleOverDelay = true,
                    topTargetScale = topScale,
                )
            }
        }
    }

    fun cupGoogleAnim(
        assetFile: String = "cup_google_anim.json",
        rewardAmount: Int? = null,
    ) {
        val safeBinding = _binding ?: return
        applyRewardAmount(rewardAmount)
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        safeBinding.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        safeBinding.dailyTasksCompliteBadge.cancelAnimation()
        safeBinding.dailyTasksCompliteBadge.alpha = 0f
        safeBinding.rocketBadgeAnim.removeAllAnimatorListeners()
        safeBinding.rocketBadgeAnim.cancelAnimation()
        safeBinding.rocketBadgeAnim.background = null
        safeBinding.rocketBadgeAnim.clipToOutline = false
        safeBinding.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
        safeBinding.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
        applyRewardSlotExtraDownOffset(true)
        playRocketSingleLottieHoldAtFrame(sessionId, assetFile, CUP_GOOGLE_HOLD_FRAME)
    }

    private fun applyRewardSlotExtraDownOffset(enabled: Boolean) {
        val b = _binding ?: return
        val dy = if (enabled) {
            SINGLE_TOP_LOTTIE_REWARD_SLOT_EXTRA_DOWN_DP * resources.displayMetrics.density
        } else {
            0f
        }
        b.badgeRewardSlot.translationY = dy
        b.badgeRewardDetailText.translationY = dy
        b.medalPieceNamesList.translationY = dy
        b.badgePiecePanel.translationY = dy
    }

    private fun applyKarateRewardSlotOffset() {
        val b = _binding ?: return
        val dy = -KARATE_REWARD_SLOT_OFFSET_UP_DP * resources.displayMetrics.density
        b.badgeRewardSlot.translationY = dy
        b.badgeRewardDetailText.translationY = dy
        b.medalPieceNamesList.translationY = dy
        b.badgePiecePanel.translationY = dy
    }

    private fun startDelayedTopLoopCycle(
        topStartDelayMs: Long,
        growTopScaleOverDelay: Boolean,
        topTargetScale: Float,
    ) {
        val safeBinding = _binding ?: return
        val sessionId = modeSessionId
        safeBinding.dailyTasksCompliteBadge.progress = 0f
        safeBinding.rocketBadgeAnim.setMinAndMaxProgress(rocketStartProgress, 1f)
        safeBinding.rocketBadgeAnim.progress = rocketStartProgress
        safeBinding.dailyTasksCompliteBadge.playAnimation()
        safeBinding.rocketBadgeAnim.pauseAnimation()
        safeBinding.rocketBadgeAnim.visibility = View.INVISIBLE
        clearPendingTopStart()

        pendingShowTopRunnable = Runnable {
            val b = _binding ?: return@Runnable
            if (!isLoopActive || sessionId != modeSessionId) return@Runnable
            b.rocketBadgeAnim.visibility = View.VISIBLE
            b.rocketBadgeAnim.playAnimation()
        }

        pendingTopStartRunnable = Runnable {
            val topView = _binding?.rocketBadgeAnim ?: return@Runnable
            if (!isLoopActive || sessionId != modeSessionId) return@Runnable

            if (growTopScaleOverDelay) {
                fun startGrowFromCenter() {
                    val v = _binding?.rocketBadgeAnim ?: return
                    if (!isLoopActive || sessionId != modeSessionId) return
                    if (v.width <= 0 || v.height <= 0) {
                        v.post { startGrowFromCenter() }
                        return
                    }
                    v.pivotX = v.width / 2f
                    v.pivotY = v.height / 2f
                    v.scaleX = 0f
                    v.scaleY = 0f
                    v.visibility = View.VISIBLE

                    pendingTopScaleAnimator?.cancel()
                    val sx = ObjectAnimator.ofFloat(v, View.SCALE_X, 0f, topTargetScale)
                    val sy = ObjectAnimator.ofFloat(v, View.SCALE_Y, 0f, topTargetScale)
                    val set = AnimatorSet().apply {
                        playTogether(sx, sy)
                        duration = topStartDelayMs.coerceAtLeast(1L)
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                if (!isLoopActive || sessionId != modeSessionId) return
                                pendingShowTopRunnable?.run()
                            }
                        })
                    }
                    pendingTopScaleAnimator = set
                    set.start()
                }

                startGrowFromCenter()
            } else {
                topView.scaleX = topTargetScale
                topView.scaleY = topTargetScale
                topView.visibility = View.INVISIBLE
                topView.postDelayed(pendingShowTopRunnable!!, topStartDelayMs)
            }
        }

        // Taban başlar başlamaz üst akışını başlat.
        safeBinding.rocketBadgeAnim.post(pendingTopStartRunnable!!)
    }

    private fun replayTopAnimationOnly() {
        collapsePieceRowsPanel()
        when (currentMode) {
            BadgeAnimMode.CUP_GOOGLE -> {
                replayCupGoogleAnimationOnly()
                return
            }
            BadgeAnimMode.KARATE -> {
                replayKarateAnimationOnly()
                return
            }
            BadgeAnimMode.GOLD_MEDAL -> {
                replayGoldMedalAnimationOnly()
                return
            }
            BadgeAnimMode.SILVER_MEDAL -> {
                replaySilverMedalAnimationOnly()
                return
            }
            BadgeAnimMode.BRONZE_MEDAL -> {
                replayBronzeMedalAnimationOnly()
                return
            }
            else -> Unit
        }
        val safeBinding = _binding ?: return
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        clearLottieFlatFilter(safeBinding.rocketBadgeAnim)
        safeBinding.rocketBadgeAnim.removeAllAnimatorListeners()
        safeBinding.rocketBadgeAnim.cancelAnimation()
        var holdFrame = TOP_HOLD_FRAME_DART
        var animationFile = "dart_anim.json"
        var topScale = 0.65f
        var speed = 1f
        var withCircleFrame = false
        var startOffsetMs = 0L

        when (currentMode) {
            BadgeAnimMode.DAILY_ONLY -> {
                holdFrame = TOP_HOLD_FRAME_DART
                animationFile = "dart_anim.json"
                topScale = 0.65f
            }
            BadgeAnimMode.ROCKET_WITH_BASE -> {
                holdFrame = TOP_HOLD_FRAME_ROCKET
                animationFile = "rocket_badge_anim2.json"
                topScale = 1f
                startOffsetMs = ROCKET_START_OFFSET_MS
            }
            BadgeAnimMode.BOWLING_WITH_BASE -> {
                holdFrame = TOP_HOLD_FRAME_BOWLING
                animationFile = "bowling_anim.json"
                topScale = 0.68f
                speed = 0.5f
                withCircleFrame = true
            }
            BadgeAnimMode.GOLF_WITH_BASE -> {
                holdFrame = TOP_HOLD_FRAME_GOLF
                animationFile = "golf_anim.json"
                topScale = 0.60f
            }
            BadgeAnimMode.FISHING_WITH_BASE -> {
                holdFrame = TOP_HOLD_FRAME_FISHING
                animationFile = "fishing_pole_anim.json"
                topScale = 0.65f
            }
            BadgeAnimMode.CUP_GOOGLE -> Unit
            BadgeAnimMode.KARATE -> Unit
            BadgeAnimMode.GOLD_MEDAL -> Unit
            BadgeAnimMode.SILVER_MEDAL -> Unit
            BadgeAnimMode.BRONZE_MEDAL -> Unit
        }

        rocketStartProgress = 0f
        safeBinding.rocketBadgeAnim.scaleX = topScale
        safeBinding.rocketBadgeAnim.scaleY = topScale
        safeBinding.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
        if (withCircleFrame) {
            safeBinding.rocketBadgeAnim.setBackgroundResource(R.drawable.bg_badge_circle_frame)
            safeBinding.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BACKGROUND
            safeBinding.rocketBadgeAnim.clipToOutline = true
        } else {
            safeBinding.rocketBadgeAnim.background = null
            safeBinding.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
            safeBinding.rocketBadgeAnim.clipToOutline = false
        }
        safeBinding.rocketBadgeAnim.setAnimation(animationFile)
        safeBinding.rocketBadgeAnim.repeatCount = 0
        safeBinding.rocketBadgeAnim.speed = speed
        safeBinding.rocketBadgeAnim.visibility = View.VISIBLE

        var topHoldProgress = 1f
        safeBinding.rocketBadgeAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val b = _binding ?: return
                if (!isLoopActive) return
                b.rocketBadgeAnim.progress = topHoldProgress
                b.rocketBadgeAnim.pauseAnimation()
            }
        })
        safeBinding.rocketBadgeAnim.addLottieOnCompositionLoadedListener { composition ->
            if (!isLoopActive || sessionId != modeSessionId) return@addLottieOnCompositionLoadedListener
            val durationMs = composition.duration.toLong().coerceAtLeast(1L)
            val normalizedOffsetMs = (startOffsetMs % durationMs).toFloat()
            rocketStartProgress = normalizedOffsetMs / durationMs.toFloat()
            topHoldProgress = frameToProgress(holdFrame, composition.startFrame, composition.endFrame)
            startTopOnlyCycle()
        }
    }

    private fun clearPendingTopStart() {
        val safeBinding = _binding ?: return
        pendingTopStartRunnable?.let { safeBinding.rocketBadgeAnim.removeCallbacks(it) }
        pendingTopStartRunnable = null
        pendingShowTopRunnable?.let { safeBinding.rocketBadgeAnim.removeCallbacks(it) }
        pendingShowTopRunnable = null
        pendingTopScaleAnimator?.cancel()
        pendingTopScaleAnimator = null
    }

    fun switchBadgeAnimation(mode: BadgeAnimMode, rewardAmount: Int? = null) {
        currentMode = mode
        currentRewardAmount = rewardAmount
        applyBadgeDetailText(mode)
        clearLottieFlatFilter(binding.dailyTasksCompliteBadge)
        clearLottieFlatFilter(binding.rocketBadgeAnim)
        updateProgressContainerVisibility(mode)
        when (mode) {
            BadgeAnimMode.DAILY_ONLY -> dartAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.ROCKET_WITH_BASE -> rocketBadgeAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.BOWLING_WITH_BASE -> bowlingAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.FISHING_WITH_BASE -> fishingAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.GOLF_WITH_BASE -> golfAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.CUP_GOOGLE -> cupGoogleAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.KARATE -> karateAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.GOLD_MEDAL -> goldMedalAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.SILVER_MEDAL -> silverMedalAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.BRONZE_MEDAL -> bronzeMedalAnim(rewardAmount = rewardAmount)
        }
        applyUnlockedLevelToneToBase(mode)
        if (!isCelebrationFlow && isUnlockedState && mode in pieceReplayModes) {
            binding.rocketBadgeAnim.setOnClickListener {
                replayTopAnimationOnly()
            }
            configureMedalPieceNamesList(mode, unlocked = true)
        }
    }

    private fun applyBadgeDetailText(mode: BadgeAnimMode, unlocked: Boolean = true) {
        val safeBinding = _binding ?: return
        val text = if (unlocked) {
            formatUnlockedDetailText(mode, unlockedDetailTextOverrides[mode].orEmpty())
        } else {
            lockedDetailTextOverrides[mode].orEmpty()
        }
        safeBinding.badgeRewardDetailText.text = text
        safeBinding.badgeRewardDetailText.visibility = View.VISIBLE
        configureMedalPieceNamesList(mode, unlocked)
    }

    private fun seasonLeaderboardCelebrationPieceSeason(): Int? {
        if (!isCelebrationFlow) return null
        val args = arguments ?: return null
        if (!args.containsKey(ARG_SEASON_LB_ACK_SEASON)) return null
        return args.getInt(ARG_SEASON_LB_ACK_SEASON)
    }

    private fun collapsePieceRowsPanel() {
        val b = _binding ?: return
        b.badgePiecePanel.visibility = View.GONE
        b.badgePieceHeaderMedal.visibility = View.GONE
        b.badgePieceHeaderCup.visibility = View.GONE
        piecePanelModeKey = null
        b.badgePieceRows.removeAllViews()
    }

    private fun togglePieceRowsPanel(mode: BadgeAnimMode, isCup: Boolean, pieceSeasonFilter: Int? = null) {
        val b = _binding ?: return
        val key = if (pieceSeasonFilter != null) {
            "${mode.name}|$isCup|s=$pieceSeasonFilter"
        } else {
            "${mode.name}|$isCup"
        }
        if (b.badgePiecePanel.visibility == View.VISIBLE && piecePanelModeKey == key) {
            collapsePieceRowsPanel()
            return
        }
        piecePanelModeKey = key

        b.badgePieceHeaderMedal.visibility = if (isCup) View.GONE else View.VISIBLE
        b.badgePieceHeaderCup.visibility = if (isCup) View.VISIBLE else View.GONE

        val rows = b.badgePieceRows
        rows.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val progress = BadgeProgressRepository.getUserBadgeProgress()
        if (isCup) {
            val cupRows = progress.cupPiece.filter { pieceSeasonFilter == null || it.season == pieceSeasonFilter }
            cupRows.forEachIndexed { index, row ->
                val rowView = inflater.inflate(R.layout.item_badge_piece_popup_cup, rows, false)
                rowView.findViewById<TextView>(R.id.tvPieceTitle).text = row.titleUnit
                rowView.findViewById<TextView>(R.id.tvPieceRank).text = row.rank.toString()
                rowView.findViewById<TextView>(R.id.tvPieceSeason).text = row.season.toString()

                // Rank bölümünde küçük cup ikonunu ProfileFragment'teki gibi sabit frame'de göster.
                val cupAnim = rowView.findViewById<LottieAnimationView>(R.id.lottiePieceCupRank)
                cupAnim.setAnimation("cup_google_anim.json")
                cupAnim.repeatCount = 0
                cupAnim.speed = 1f
                cupAnim.addLottieOnCompositionLoadedListener { composition ->
                    val progress01 = frameToProgress(
                        CUP_GOOGLE_HOLD_FRAME,
                        composition.startFrame,
                        composition.endFrame,
                    )
                    cupAnim.progress = progress01
                    cupAnim.pauseAnimation()
                }
                rows.addView(rowView)
                if (index < cupRows.lastIndex) {
                    inflater.inflate(R.layout.item_badge_piece_row_divider, rows, true)
                }
            }
        } else {
            val list = when (mode) {
                BadgeAnimMode.GOLD_MEDAL -> progress.goldMedalPiece.filter {
                    pieceSeasonFilter == null || it.season == pieceSeasonFilter
                }
                BadgeAnimMode.SILVER_MEDAL -> progress.silverMedalPiece.filter {
                    pieceSeasonFilter == null || it.season == pieceSeasonFilter
                }
                BadgeAnimMode.BRONZE_MEDAL -> progress.bronzeMedalPiece.filter {
                    pieceSeasonFilter == null || it.season == pieceSeasonFilter
                }
                else -> emptyList()
            }
            val medalAsset = when (mode) {
                BadgeAnimMode.GOLD_MEDAL -> "gold_medal_anim.json"
                BadgeAnimMode.SILVER_MEDAL -> "silver_medal_anim.json.json"
                BadgeAnimMode.BRONZE_MEDAL -> "bronze_medal_anim.json.json"
                else -> null
            }
            list.forEachIndexed { index, row ->
                val rowView = inflater.inflate(R.layout.item_badge_piece_popup_medal, rows, false)
                rowView.findViewById<TextView>(R.id.tvPieceTitle).text = row.titleUnit
                rowView.findViewById<TextView>(R.id.tvPieceSeason).text = row.season.toString()
                medalAsset?.let { asset ->
                    val medalAnim = rowView.findViewById<LottieAnimationView>(R.id.lottiePieceMedal)
                    medalAnim.setAnimation(asset)
                    medalAnim.repeatCount = 0
                    medalAnim.speed = 1f
                    medalAnim.addLottieOnCompositionLoadedListener { composition ->
                        val p = frameToProgress(0f, composition.startFrame, composition.endFrame)
                        medalAnim.progress = p
                        medalAnim.pauseAnimation()
                    }
                }
                rows.addView(rowView)
                if (index < list.lastIndex) {
                    inflater.inflate(R.layout.item_badge_piece_row_divider, rows, true)
                }
            }
        }
        b.badgePiecePanel.visibility = View.VISIBLE
        (b.medalPieceNamesList.parent as? ViewGroup)?.let { p ->
            if (b.badgePiecePanel.parent === p) {
                p.bringChildToFront(b.badgePiecePanel)
                p.bringChildToFront(b.medalPieceNamesList)
            }
        }
    }

    private fun configureMedalPieceNamesList(mode: BadgeAnimMode, unlocked: Boolean) {
        val b = _binding ?: return
        collapsePieceRowsPanel()
        b.medalPieceNamesList.setOnClickListener(null)
        val seasonLbPieceFilter = seasonLeaderboardCelebrationPieceSeason()
        if (isCelebrationFlow && seasonLbPieceFilter == null) {
            b.medalPieceNamesList.visibility = View.GONE
            b.badgeRewardDetailText.elevation = 0f
            b.medalPieceNamesList.elevation = 0f
            return
        }
        if (!unlocked || mode !in setOf(
                BadgeAnimMode.GOLD_MEDAL,
                BadgeAnimMode.SILVER_MEDAL,
                BadgeAnimMode.BRONZE_MEDAL,
                BadgeAnimMode.CUP_GOOGLE,
            )
        ) {
            b.medalPieceNamesList.visibility = View.GONE
            b.badgeRewardDetailText.elevation = 0f
            b.medalPieceNamesList.elevation = 0f
            return
        }
        val p = BadgeProgressRepository.getUserBadgeProgress()
        val (count, isCup) = when (mode) {
            BadgeAnimMode.GOLD_MEDAL -> {
                val n = p.goldMedalPiece.count { seasonLbPieceFilter == null || it.season == seasonLbPieceFilter }
                n to false
            }
            BadgeAnimMode.SILVER_MEDAL -> {
                val n = p.silverMedalPiece.count { seasonLbPieceFilter == null || it.season == seasonLbPieceFilter }
                n to false
            }
            BadgeAnimMode.BRONZE_MEDAL -> {
                val n = p.bronzeMedalPiece.count { seasonLbPieceFilter == null || it.season == seasonLbPieceFilter }
                n to false
            }
            BadgeAnimMode.CUP_GOOGLE -> {
                val n = p.cupPiece.count { seasonLbPieceFilter == null || it.season == seasonLbPieceFilter }
                n to true
            }
            else -> 0 to false
        }
        if (count <= 0) {
            b.medalPieceNamesList.visibility = View.GONE
            b.badgeRewardDetailText.elevation = 0f
            b.medalPieceNamesList.elevation = 0f
            return
        }
        b.medalPieceNamesList.visibility = View.VISIBLE
        b.medalPieceNamesList.text = "${count}x"
        (b.medalPieceNamesList.parent as? ViewGroup)?.let { p ->
            if (b.badgePiecePanel.parent === p) {
                p.bringChildToFront(b.badgePiecePanel)
                p.bringChildToFront(b.medalPieceNamesList)
            }
        }
        b.medalPieceNamesList.setOnClickListener { v ->
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            togglePieceRowsPanel(mode, isCup, seasonLbPieceFilter)
        }
    }

    private fun formatUnlockedDetailText(mode: BadgeAnimMode, template: String): String {
        val modesWithDynamicProgress = setOf(
            BadgeAnimMode.DAILY_ONLY,
            BadgeAnimMode.ROCKET_WITH_BASE,
            BadgeAnimMode.BOWLING_WITH_BASE,
            BadgeAnimMode.FISHING_WITH_BASE,
            BadgeAnimMode.GOLF_WITH_BASE,
            BadgeAnimMode.CUP_GOOGLE,
            BadgeAnimMode.KARATE,
        )
        if (mode !in modesWithDynamicProgress) return template
        val progress = BadgeProgressRepository.getProgressValueByMode(mode) ?: return template
        return template.replace("x", progress.toString())
    }

    private fun showLockedStaticBadge(mode: BadgeAnimMode, rewardAmount: Int?) {
        val b = _binding ?: return
        currentMode = mode
        currentRewardAmount = rewardAmount
        updateProgressContainerVisibility(mode)
        modeSessionId++
        clearPendingTopStart()
        rewardAmountScaleAnimator?.cancel()
        rewardAmountScaleAnimator = null
        b.badgeRewardPopBurst.cancelBurst()
        b.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        b.rocketBadgeAnim.removeAllAnimatorListeners()
        b.dailyTasksCompliteBadge.cancelAnimation()
        b.rocketBadgeAnim.cancelAnimation()
        b.rocketBadgeAnim.setMinAndMaxProgress(0f, 1f)
        b.rocketBadgeAnim.speed = 1f
        b.rocketBadgeAnim.repeatCount = 0
        b.rocketBadgeAnim.visibility = View.VISIBLE
        b.dailyTasksCompliteBadge.visibility = View.VISIBLE
        b.dailyTasksCompliteBadge.alpha = 1f
        b.rocketBadgeAnim.scaleX = 1f
        b.rocketBadgeAnim.scaleY = 1f
        b.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
        b.rocketBadgeAnim.background = null
        b.rocketBadgeAnim.clipToOutline = false
        b.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
        applyRewardSlotExtraDownOffset(false)

        val baseAsset: String?
        val topAsset: String
        val holdFrame: Float
        val topScale: Float
        val withCircleFrame: Boolean
        when (mode) {
            BadgeAnimMode.DAILY_ONLY -> {
                baseAsset = "daily_tasks_complite_badge2.json"
                topAsset = "dart_anim.json"
                holdFrame = TOP_HOLD_FRAME_DART
                topScale = 0.65f
                withCircleFrame = false
            }
            BadgeAnimMode.ROCKET_WITH_BASE -> {
                baseAsset = "daily_tasks_complite_badge2.json"
                topAsset = "rocket_badge_anim2.json"
                holdFrame = TOP_HOLD_FRAME_ROCKET
                topScale = 1f
                withCircleFrame = false
            }
            BadgeAnimMode.BOWLING_WITH_BASE -> {
                baseAsset = "daily_tasks_complite_badge2.json"
                topAsset = "bowling_anim.json"
                holdFrame = TOP_HOLD_FRAME_BOWLING
                topScale = 0.68f
                withCircleFrame = true
            }
            BadgeAnimMode.FISHING_WITH_BASE -> {
                baseAsset = "daily_tasks_complite_badge2.json"
                topAsset = "fishing_pole_anim.json"
                holdFrame = TOP_HOLD_FRAME_FISHING
                topScale = 0.65f
                withCircleFrame = false
            }
            BadgeAnimMode.GOLF_WITH_BASE -> {
                baseAsset = "daily_tasks_complite_badge2.json"
                topAsset = "golf_anim.json"
                holdFrame = TOP_HOLD_FRAME_GOLF
                topScale = 0.60f
                withCircleFrame = false
            }
            BadgeAnimMode.CUP_GOOGLE -> {
                baseAsset = null
                topAsset = "cup_google_anim.json"
                holdFrame = CUP_GOOGLE_HOLD_FRAME
                topScale = 1f
                withCircleFrame = false
                applyRewardSlotExtraDownOffset(true)
            }
            BadgeAnimMode.KARATE -> {
                baseAsset = null
                topAsset = "karate_anim.json"
                holdFrame = KARATE_HOLD_FRAME
                topScale = 1f
                withCircleFrame = false
                applyKarateRewardSlotOffset()
            }
            BadgeAnimMode.GOLD_MEDAL -> {
                baseAsset = null
                topAsset = "gold_medal_anim.json"
                holdFrame = 0f
                topScale = 1f
                withCircleFrame = false
            }
            BadgeAnimMode.SILVER_MEDAL -> {
                baseAsset = null
                topAsset = "silver_medal_anim.json.json"
                holdFrame = 0f
                topScale = 1f
                withCircleFrame = false
            }
            BadgeAnimMode.BRONZE_MEDAL -> {
                baseAsset = null
                topAsset = "bronze_medal_anim.json.json"
                holdFrame = 0f
                topScale = 1f
                withCircleFrame = false
            }
        }

        if (baseAsset == null) {
            b.dailyTasksCompliteBadge.cancelAnimation()
            b.dailyTasksCompliteBadge.alpha = 0f
            applyLottieFlatGrayFilter(b.rocketBadgeAnim, lockedTopColor)
        } else {
            b.dailyTasksCompliteBadge.alpha = 1f
            b.dailyTasksCompliteBadge.setAnimation(baseAsset)
            b.dailyTasksCompliteBadge.addLottieOnCompositionLoadedListener {
                b.dailyTasksCompliteBadge.progress = 1f
                b.dailyTasksCompliteBadge.pauseAnimation()
                applyLottieFlatGrayFilter(b.dailyTasksCompliteBadge, lockedBaseColor)
            }
            applyLottieFlatGrayFilter(b.rocketBadgeAnim, lockedTopColor)
        }

        b.rocketBadgeAnim.scaleX = topScale
        b.rocketBadgeAnim.scaleY = topScale
        if (withCircleFrame) {
            b.rocketBadgeAnim.setBackgroundResource(R.drawable.bg_badge_circle_frame)
            b.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BACKGROUND
            b.rocketBadgeAnim.clipToOutline = true
        }
        b.rocketBadgeAnim.setAnimation(topAsset)
        b.rocketBadgeAnim.addLottieOnCompositionLoadedListener { composition ->
            val progress = frameToProgress(holdFrame, composition.startFrame, composition.endFrame)
            b.rocketBadgeAnim.progress = progress
            b.rocketBadgeAnim.pauseAnimation()
            applyLottieFlatGrayFilter(b.rocketBadgeAnim, lockedTopColor)
        }

        applyRewardAmountStaticLocked(rewardAmount)
        applyBadgeDetailText(mode, unlocked = false)
    }

    private fun applyRewardAmountStaticLocked(amount: Int?) {
        val b = _binding ?: return
        val label = b.badgeRewardAmount
        if (amount == null) {
            label.visibility = View.GONE
            return
        }
        label.visibility = View.VISIBLE
        label.text = amount.toString()
        label.scaleX = 1f
        label.scaleY = 1f
        label.setTextColor(lockedBaseColor)
        label.setStrokeColorInt(lockedTopColor)
    }

    private fun applyLottieFlatGrayFilter(view: com.airbnb.lottie.LottieAnimationView, color: Int) {
        view.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            LottieValueCallback<ColorFilter>(PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)),
        )
        view.invalidate()
    }

    private fun clearLottieFlatFilter(view: com.airbnb.lottie.LottieAnimationView) {
        view.clearValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
        )
        // Bazı cihaz/Lottie sürümlerinde eski callback zinciri kalabildiği için
        // null filter callback ile üstüne yazıp tamamen nötrlüyoruz.
        view.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            LottieValueCallback<ColorFilter?>(null),
        )
        view.clearValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
        )
        view.invalidate()
    }

    private fun applyLottieNeutralFilter(view: com.airbnb.lottie.LottieAnimationView) {
        view.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            LottieValueCallback<ColorFilter>(PorterDuffColorFilter(android.graphics.Color.WHITE, PorterDuff.Mode.MULTIPLY)),
        )
        view.invalidate()
    }

    private fun applyUnlockedLevelToneToBase(mode: BadgeAnimMode) {
        val base = _binding?.dailyTasksCompliteBadge ?: return
        val tone = BadgeProgressRepository.getStateByMode(mode)?.levelTone
        when (tone) {
            BadgeLevelTone.BRONZE -> applyLottieToneFilter(base, bronzeToneColor)
            BadgeLevelTone.SILVER -> applyLottieToneFilter(base, silverToneColor)
            BadgeLevelTone.RED -> applyLottieToneFilter(base, redToneColor)
            BadgeLevelTone.PURPLE -> applyLottieToneFilterStrong(base, purpleToneColor)
            BadgeLevelTone.ORIGINAL, null -> clearLottieFlatFilter(base)
        }
    }

    private fun applyLottieToneFilter(view: com.airbnb.lottie.LottieAnimationView, color: Int) {
        view.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            LottieValueCallback<ColorFilter>(PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)),
        )
        view.invalidate()
    }

    private fun applyLottieToneFilterStrong(view: com.airbnb.lottie.LottieAnimationView, color: Int) {
        view.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            LottieValueCallback<ColorFilter>(PorterDuffColorFilter(color, PorterDuff.Mode.OVERLAY)),
        )
        view.invalidate()
    }

    private fun updateLockedProgressUi() {
        val b = _binding ?: return
        val current = lockedProgressCurrent.coerceAtLeast(0)
        val target = lockedProgressTarget.coerceAtLeast(1)
        b.badgeLockedProgressText.text = "$current/$target"
        val percent = ((current.toFloat() / target.toFloat()) * 100f).toInt().coerceIn(0, 100)
        applyMissionProgressOverlay(
            widthHost = b.badgeLockedProgressTrack,
            fill = b.badgeLockedProgressFill,
            shine = b.badgeLockedProgressShine,
            percent = percent,
            done = false,
            claimed = false,
        )
    }

    private fun startLevelUpCelebration(mode: BadgeAnimMode, rewardAmount: Int?) {
        val b = _binding ?: return
        val fromProgress = arguments?.getInt(ARG_CELEBRATE_FROM_PROGRESS, 0) ?: 0
        val toProgress = arguments?.getInt(ARG_CELEBRATE_TO_PROGRESS, fromProgress) ?: fromProgress
        val reachedTarget = arguments?.getInt(ARG_CELEBRATE_TARGET, toProgress) ?: toProgress
        val oldShownValue = BadgeProgressRepository.resolveShownValueByMode(mode, fromProgress)
            ?: resolveShownValueFallback(mode, fromProgress)
        val newShownValue = BadgeProgressRepository.resolveShownValueByMode(mode, toProgress)
            ?: resolveShownValueFallback(mode, toProgress)

        currentMode = mode
        currentRewardAmount = newShownValue
        applyBadgeDetailText(mode, unlocked = true)
        applyRewardAmountStatic(oldShownValue)
        if (mode == BadgeAnimMode.KARATE) {
            applyKarateRewardSlotOffset()
        } else {
            applyRewardSlotExtraDownOffset(false)
        }
        binding.root.setOnClickListener(null)
        binding.dailyTasksCompliteBadge.setOnClickListener(null)

        lockedProgressCurrent = fromProgress.coerceAtLeast(0)
        lockedProgressTarget = reachedTarget.coerceAtLeast(1)
        binding.badgeLockedProgressContainer.visibility = View.VISIBLE
        updateLockedProgressUi()

        val previousTone = BadgeProgressRepository.resolveToneByMode(mode, fromProgress)
        val newTone = BadgeProgressRepository.resolveToneByMode(mode, toProgress)
        Log.d(
            TAG,
            "startLevelUpCelebration mode=$mode from=$fromProgress to=$toProgress target=$reachedTarget oldShown=$oldShownValue newShown=$newShownValue previousTone=$previousTone newTone=$newTone",
        )
        setupLevelUpStaticBadge(mode, previousTone)

        val animator = ValueAnimator.ofInt(fromProgress, reachedTarget).apply {
            duration = 3000L
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                val fraction = valueAnimator.animatedFraction.coerceIn(0f, 1f)
                val span = (reachedTarget - fromProgress).coerceAtLeast(1)
                val animatedCurrent = (fromProgress + (span * fraction)).toInt().coerceAtMost(reachedTarget)
                lockedProgressCurrent = animatedCurrent
                updateCelebrationProgressUi(fraction, animatedCurrent, reachedTarget)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    Log.d(TAG, "celebration progress finished -> applying new colors and starting animations")
                    val doneText = b.badgeLockedProgressText
                    doneText.text = "Tamamlandı !"
                    applyMissionProgressOverlay(
                        widthHost = b.badgeLockedProgressTrack,
                        fill = b.badgeLockedProgressFill,
                        shine = b.badgeLockedProgressShine,
                        percent = 100,
                        done = true,
                        claimed = false,
                    )
                    applyBaseTone(previousTone = previousTone, newTone = newTone)
                    Log.d(TAG, "base tone applied with newTone=$newTone")
                    clearLottieFlatFilter(b.rocketBadgeAnim)
                    Log.d(TAG, "rocket top filter cleared before celebration animation")
                    applyRewardAmount(newShownValue)
                    playLevelUpCelebrationAnimation(mode, newTone) {
                        if (hasNextCelebrationInQueue()) {
                            b.root.setOnClickListener { goToNextCelebrationInQueue() }
                            b.dailyTasksCompliteBadge.setOnClickListener { goToNextCelebrationInQueue() }
                        } else {
                            b.root.setOnClickListener { closeCelebrationFragmentWithSlide() }
                            b.dailyTasksCompliteBadge.setOnClickListener { closeCelebrationFragmentWithSlide() }
                        }
                    }
                }
            })
        }
        animator.start()
    }

    private fun closeCelebrationFragmentWithSlide() {
        val ackSeason = arguments?.takeIf { it.containsKey(ARG_SEASON_LB_ACK_SEASON) }?.getInt(ARG_SEASON_LB_ACK_SEASON)
        if (ackSeason != null && !hasNextCelebrationInQueue()) {
            (activity as? MainActivity)?.onSeasonLeaderboardBadgeCelebrationFinished(ackSeason)
        }
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right,
            )
            .remove(this@BadgeFragment)
            .commit()
        (activity as? MainActivity)?.tryShowPendingMarathonGuideOnMap("BadgeFragment.closeCelebration")
    }

    private fun hasNextCelebrationInQueue(): Boolean {
        val queue = arguments?.getStringArrayList(ARG_CELEBRATE_QUEUE) ?: return false
        val index = arguments?.getInt(ARG_CELEBRATE_INDEX, 0) ?: 0
        return (index + 1) < queue.size
    }

    private fun goToNextCelebrationInQueue() {
        val queue = arguments?.getStringArrayList(ARG_CELEBRATE_QUEUE) ?: return
        val index = arguments?.getInt(ARG_CELEBRATE_INDEX, 0) ?: 0
        val nextIndex = index + 1
        if (nextIndex >= queue.size) return
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.badgeFragmentContainter,
                newLevelUpSequenceInstance(
                    queue,
                    nextIndex,
                    seasonLeaderboardAckAfterQueue = arguments?.takeIf { it.containsKey(ARG_SEASON_LB_ACK_SEASON) }
                        ?.getInt(ARG_SEASON_LB_ACK_SEASON),
                ),
            )
            .commit()
    }

    private data class LevelUpAnimConfig(
        val topAsset: String,
        val topHoldFrame: Float,
        val topScale: Float,
        val topSpeed: Float = 1f,
        val withCircleFrame: Boolean = false,
    )

    private fun levelUpAnimConfig(mode: BadgeAnimMode): LevelUpAnimConfig = when (mode) {
        BadgeAnimMode.DAILY_ONLY -> LevelUpAnimConfig("dart_anim.json", TOP_HOLD_FRAME_DART, 0.65f)
        BadgeAnimMode.ROCKET_WITH_BASE -> LevelUpAnimConfig("rocket_badge_anim2.json", TOP_HOLD_FRAME_ROCKET, 1f)
        BadgeAnimMode.BOWLING_WITH_BASE -> LevelUpAnimConfig("bowling_anim.json", TOP_HOLD_FRAME_BOWLING, 0.68f, topSpeed = 0.5f, withCircleFrame = true)
        BadgeAnimMode.FISHING_WITH_BASE -> LevelUpAnimConfig("fishing_pole_anim.json", TOP_HOLD_FRAME_FISHING, 0.65f)
        BadgeAnimMode.GOLF_WITH_BASE -> LevelUpAnimConfig("golf_anim.json", TOP_HOLD_FRAME_GOLF, 0.60f)
        BadgeAnimMode.KARATE -> LevelUpAnimConfig("karate_anim.json", KARATE_HOLD_FRAME, 1f)
        else -> LevelUpAnimConfig("dart_anim.json", TOP_HOLD_FRAME_DART, 0.65f)
    }

    private fun setupLevelUpStaticBadge(mode: BadgeAnimMode, previousTone: BadgeLevelTone?) {
        val b = _binding ?: return
        if (mode == BadgeAnimMode.KARATE) {
            modeSessionId++
            val sessionId = modeSessionId
            clearPendingTopStart()
            // GONE: ConstraintLayout hedefi boyutsuz sayar; reward slot + detail text yukarı kayar.
            b.dailyTasksCompliteBadge.visibility = View.INVISIBLE
            b.dailyTasksCompliteBadge.cancelAnimation()
            b.rocketBadgeAnim.cancelAnimation()
            b.rocketBadgeAnim.removeAllAnimatorListeners()
            b.rocketBadgeAnim.setAnimation("karate_anim.json")
            b.rocketBadgeAnim.repeatCount = 0
            b.rocketBadgeAnim.speed = 1f
            b.rocketBadgeAnim.scaleX = 1f
            b.rocketBadgeAnim.scaleY = 1f
            b.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
            b.rocketBadgeAnim.background = null
            b.rocketBadgeAnim.clipToOutline = false
            b.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
            b.rocketBadgeAnim.alpha = 0f
            clearLottieFlatFilter(b.rocketBadgeAnim)
            b.rocketBadgeAnim.addLottieOnCompositionLoadedListener { composition ->
                if (sessionId != modeSessionId || !isLoopActive) return@addLottieOnCompositionLoadedListener
                val holdProgress = frameToProgress(KARATE_HOLD_FRAME, composition.startFrame, composition.endFrame)
                b.rocketBadgeAnim.progress = holdProgress
                b.rocketBadgeAnim.pauseAnimation()
                applyLottieFlatGrayFilter(b.rocketBadgeAnim, lockedTopColor)
                Log.d(TAG, "setupLevelUpStaticBadge karate hold=$holdProgress")
                b.rocketBadgeAnim.alpha = 1f
            }
            return
        }
        val cfg = levelUpAnimConfig(mode)
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        b.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        b.rocketBadgeAnim.removeAllAnimatorListeners()
        b.dailyTasksCompliteBadge.cancelAnimation()
        b.rocketBadgeAnim.cancelAnimation()
        b.dailyTasksCompliteBadge.setAnimation("daily_tasks_complite_badge2.json")
        b.rocketBadgeAnim.setAnimation(cfg.topAsset)
        b.dailyTasksCompliteBadge.repeatCount = 0
        b.rocketBadgeAnim.repeatCount = 0
        b.rocketBadgeAnim.speed = cfg.topSpeed
        b.rocketBadgeAnim.scaleX = cfg.topScale
        b.rocketBadgeAnim.scaleY = cfg.topScale
        b.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
        if (cfg.withCircleFrame) {
            b.rocketBadgeAnim.setBackgroundResource(R.drawable.bg_badge_circle_frame)
            b.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BACKGROUND
            b.rocketBadgeAnim.clipToOutline = true
        } else {
            b.rocketBadgeAnim.background = null
            b.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
            b.rocketBadgeAnim.clipToOutline = false
        }
        b.dailyTasksCompliteBadge.alpha = 0f
        b.rocketBadgeAnim.alpha = 0f
        clearLottieFlatFilter(b.dailyTasksCompliteBadge)
        clearLottieFlatFilter(b.rocketBadgeAnim)

        b.dailyTasksCompliteBadge.addLottieOnCompositionLoadedListener {
            if (sessionId != modeSessionId || !isLoopActive) return@addLottieOnCompositionLoadedListener
            b.dailyTasksCompliteBadge.progress = 1f
            b.dailyTasksCompliteBadge.pauseAnimation()
            applyBaseTone(previousTone = previousTone, newTone = previousTone)
            Log.d(TAG, "setupDartStaticBadge base prepared with previousTone=$previousTone")
            b.dailyTasksCompliteBadge.alpha = 1f
        }
        b.rocketBadgeAnim.addLottieOnCompositionLoadedListener { composition ->
            if (sessionId != modeSessionId || !isLoopActive) return@addLottieOnCompositionLoadedListener
            val holdProgress = frameToProgress(cfg.topHoldFrame, composition.startFrame, composition.endFrame)
            b.rocketBadgeAnim.progress = holdProgress
            b.rocketBadgeAnim.pauseAnimation()
            applyTopVisualForPreviousState(previousTone)
            Log.d(TAG, "setupDartStaticBadge top prepared with previousTone=$previousTone holdProgress=$holdProgress")
            b.rocketBadgeAnim.alpha = 1f
        }
    }

    private fun applyTopVisualForPreviousState(previousTone: BadgeLevelTone?) {
        val top = _binding?.rocketBadgeAnim ?: return
        clearLottieFlatFilter(top)
        if (previousTone == null) {
            // Eski state kilitliyse top katman da kilitli gri görünsün.
            applyLottieFlatGrayFilter(top, lockedTopColor)
            Log.d(TAG, "applyTopVisualForPreviousState -> locked gray applied")
        } else {
            Log.d(TAG, "applyTopVisualForPreviousState -> no gray filter (previousTone=$previousTone)")
        }
    }

    private fun applyBaseTone(previousTone: BadgeLevelTone?, newTone: BadgeLevelTone?) {
        val base = _binding?.dailyTasksCompliteBadge ?: return
        val tone = newTone ?: previousTone
        clearLottieFlatFilter(base)
        when (tone) {
            BadgeLevelTone.BRONZE -> applyLottieToneFilter(base, bronzeToneColor)
            BadgeLevelTone.SILVER -> applyLottieToneFilter(base, silverToneColor)
            BadgeLevelTone.RED -> applyLottieToneFilter(base, redToneColor)
            BadgeLevelTone.PURPLE -> applyLottieToneFilterStrong(base, purpleToneColor)
            BadgeLevelTone.ORIGINAL -> {
                clearLottieFlatFilter(base)
                applyLottieNeutralFilter(base)
            }
            null -> applyLottieFlatGrayFilter(base, lockedBaseColor)
        }
        Log.d(TAG, "applyBaseTone previousTone=$previousTone newTone=$newTone appliedTone=$tone")
    }

    private fun updateCelebrationProgressUi(fraction: Float, current: Int, target: Int) {
        val b = _binding ?: return
        b.badgeLockedProgressText.text = "$current/$target"
        applyMissionProgressOverlay(
            widthHost = b.badgeLockedProgressTrack,
            fill = b.badgeLockedProgressFill,
            shine = b.badgeLockedProgressShine,
            percent = (fraction * 100f).toInt().coerceIn(0, 100),
            done = false,
            claimed = false,
        )
    }

    private fun playLevelUpCelebrationAnimation(
        mode: BadgeAnimMode,
        targetTone: BadgeLevelTone?,
        onFinished: () -> Unit,
    ) {
        val b = _binding ?: return
        val cfg = levelUpAnimConfig(mode)
        modeSessionId++
        val sessionId = modeSessionId
        Log.d(TAG, "playLevelUpCelebrationAnimation mode=$mode session=$sessionId started")
        if (mode == BadgeAnimMode.KARATE) {
            b.dailyTasksCompliteBadge.visibility = View.INVISIBLE
            clearLottieFlatFilter(b.rocketBadgeAnim)
            applyLottieNeutralFilter(b.rocketBadgeAnim)
            b.rocketBadgeAnim.removeAllAnimatorListeners()
            b.rocketBadgeAnim.cancelAnimation()
            b.rocketBadgeAnim.setAnimation(cfg.topAsset)
            b.rocketBadgeAnim.progress = 0f
            b.rocketBadgeAnim.speed = cfg.topSpeed
            b.rocketBadgeAnim.scaleX = cfg.topScale
            b.rocketBadgeAnim.scaleY = cfg.topScale
            b.rocketBadgeAnim.background = null
            b.rocketBadgeAnim.clipToOutline = false
            b.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
            var topHold = 1f
            b.rocketBadgeAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val bind = _binding ?: return
                    if (sessionId != modeSessionId || !isLoopActive) return
                    bind.rocketBadgeAnim.progress = topHold
                    bind.rocketBadgeAnim.pauseAnimation()
                    onFinished()
                }
            })
            b.rocketBadgeAnim.addLottieOnCompositionLoadedListener { composition ->
                if (sessionId != modeSessionId || !isLoopActive) return@addLottieOnCompositionLoadedListener
                clearLottieFlatFilter(b.rocketBadgeAnim)
                applyLottieNeutralFilter(b.rocketBadgeAnim)
                topHold = frameToProgress(cfg.topHoldFrame, composition.startFrame, composition.endFrame)
                b.rocketBadgeAnim.progress = 0f
                b.rocketBadgeAnim.playAnimation()
            }
            b.rocketBadgeAnim.post {
                if (sessionId != modeSessionId || !isLoopActive) return@post
                clearLottieFlatFilter(b.rocketBadgeAnim)
                applyLottieNeutralFilter(b.rocketBadgeAnim)
                b.rocketBadgeAnim.playAnimation()
            }
            return
        }
        clearLottieFlatFilter(b.rocketBadgeAnim)
        applyLottieNeutralFilter(b.rocketBadgeAnim)
        clearLottieFlatFilter(b.dailyTasksCompliteBadge)
        b.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        b.rocketBadgeAnim.removeAllAnimatorListeners()
        b.dailyTasksCompliteBadge.cancelAnimation()
        b.rocketBadgeAnim.cancelAnimation()
        b.dailyTasksCompliteBadge.setAnimation("daily_tasks_complite_badge2.json")
        b.rocketBadgeAnim.setAnimation(cfg.topAsset)
        b.dailyTasksCompliteBadge.progress = 0f
        b.rocketBadgeAnim.progress = 0f
        b.rocketBadgeAnim.speed = cfg.topSpeed
        b.rocketBadgeAnim.scaleX = cfg.topScale
        b.rocketBadgeAnim.scaleY = cfg.topScale
        if (cfg.withCircleFrame) {
            b.rocketBadgeAnim.setBackgroundResource(R.drawable.bg_badge_circle_frame)
            b.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BACKGROUND
            b.rocketBadgeAnim.clipToOutline = true
        } else {
            b.rocketBadgeAnim.background = null
            b.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
            b.rocketBadgeAnim.clipToOutline = false
        }
        var topHold = 1f
        b.dailyTasksCompliteBadge.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val bind = _binding ?: return
                bind.dailyTasksCompliteBadge.progress = 1f
                bind.dailyTasksCompliteBadge.pauseAnimation()
            }
        })
        b.rocketBadgeAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val bind = _binding ?: return
                if (sessionId != modeSessionId || !isLoopActive) return
                bind.rocketBadgeAnim.progress = topHold
                bind.rocketBadgeAnim.pauseAnimation()
                onFinished()
            }
        })
        b.rocketBadgeAnim.addLottieOnCompositionLoadedListener { composition ->
            if (sessionId != modeSessionId || !isLoopActive) return@addLottieOnCompositionLoadedListener
            clearLottieFlatFilter(b.rocketBadgeAnim)
            applyLottieNeutralFilter(b.rocketBadgeAnim)
            topHold = frameToProgress(cfg.topHoldFrame, composition.startFrame, composition.endFrame)
            Log.d(TAG, "rocket composition loaded; top filter cleared; topHold=$topHold")
            b.rocketBadgeAnim.progress = 0f
            b.rocketBadgeAnim.playAnimation()
        }
        b.dailyTasksCompliteBadge.addLottieOnCompositionLoadedListener {
            if (sessionId != modeSessionId || !isLoopActive) return@addLottieOnCompositionLoadedListener
            clearLottieFlatFilter(b.dailyTasksCompliteBadge)
            applyBaseTone(previousTone = null, newTone = targetTone)
            Log.d(TAG, "base composition loaded; base filter cleared")
            b.dailyTasksCompliteBadge.progress = 0f
            b.dailyTasksCompliteBadge.playAnimation()
        }
        b.rocketBadgeAnim.post {
            if (sessionId != modeSessionId || !isLoopActive) return@post
            clearLottieFlatFilter(b.rocketBadgeAnim)
            applyLottieNeutralFilter(b.rocketBadgeAnim)
            b.rocketBadgeAnim.playAnimation()
        }
        b.dailyTasksCompliteBadge.post {
            if (sessionId != modeSessionId || !isLoopActive) return@post
            clearLottieFlatFilter(b.dailyTasksCompliteBadge)
            applyBaseTone(previousTone = null, newTone = targetTone)
            b.dailyTasksCompliteBadge.playAnimation()
        }
    }

    private fun resolveShownValueFallback(mode: BadgeAnimMode, progress: Int): Int {
        val spec = BadgeProgressRepository.getLevelSpecByMode(mode) ?: return progress.coerceAtLeast(0)
        val levels = spec.first
        val step = spec.second
        val safe = progress.coerceAtLeast(0)
        if (safe < levels.first()) return levels.first()
        if (safe <= levels.last()) return levels.lastOrNull { it <= safe } ?: levels.first()
        val last = levels.last()
        val extra = (safe - last) / step
        return last + (extra * step)
    }

    private fun applyRewardAmountStatic(amount: Int?) {
        val b = _binding ?: return
        val label = b.badgeRewardAmount
        if (amount == null) {
            label.visibility = View.GONE
            return
        }
        label.visibility = View.VISIBLE
        label.text = amount.toString()
        label.scaleX = 1f
        label.scaleY = 1f
        label.setTextColor(android.graphics.Color.WHITE)
        label.setStrokeColorInt(0xFFFF8F00.toInt())
    }

    private fun shouldShowProgressForMode(mode: BadgeAnimMode): Boolean {
        return mode == BadgeAnimMode.DAILY_ONLY ||
            mode == BadgeAnimMode.ROCKET_WITH_BASE ||
            mode == BadgeAnimMode.BOWLING_WITH_BASE ||
            mode == BadgeAnimMode.FISHING_WITH_BASE ||
            mode == BadgeAnimMode.GOLF_WITH_BASE
    }

    private fun updateProgressContainerVisibility(mode: BadgeAnimMode) {
        val b = _binding ?: return
        if (shouldShowProgressForMode(mode)) {
            BadgeProgressRepository.getProgressWindowByMode(mode)?.let { window ->
                lockedProgressCurrent = window.current
                lockedProgressTarget = window.target
            }
            b.badgeLockedProgressContainer.visibility = View.VISIBLE
            updateLockedProgressUi()
        } else {
            b.badgeLockedProgressContainer.visibility = View.GONE
        }
    }

    fun goldMedalAnim(
        assetFile: String = "gold_medal_anim.json",
        rewardAmount: Int? = null,
    ) {
        val safeBinding = _binding ?: return
        applyRewardAmount(rewardAmount)
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        applyRewardSlotExtraDownOffset(false)
        safeBinding.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        safeBinding.dailyTasksCompliteBadge.cancelAnimation()
        safeBinding.dailyTasksCompliteBadge.alpha = 0f
        safeBinding.rocketBadgeAnim.removeAllAnimatorListeners()
        safeBinding.rocketBadgeAnim.cancelAnimation()
        safeBinding.rocketBadgeAnim.background = null
        safeBinding.rocketBadgeAnim.clipToOutline = false
        safeBinding.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
        safeBinding.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
        playRocketSingleLottieLoop(sessionId, assetFile)
    }

    fun silverMedalAnim(
        assetFile: String = "silver_medal_anim.json.json",
        rewardAmount: Int? = null,
    ) {
        val safeBinding = _binding ?: return
        applyRewardAmount(rewardAmount)
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        applyRewardSlotExtraDownOffset(false)
        safeBinding.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        safeBinding.dailyTasksCompliteBadge.cancelAnimation()
        safeBinding.dailyTasksCompliteBadge.alpha = 0f
        safeBinding.rocketBadgeAnim.removeAllAnimatorListeners()
        safeBinding.rocketBadgeAnim.cancelAnimation()
        safeBinding.rocketBadgeAnim.background = null
        safeBinding.rocketBadgeAnim.clipToOutline = false
        safeBinding.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
        safeBinding.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
        playRocketSingleLottieLoop(sessionId, assetFile)
    }

    fun bronzeMedalAnim(
        assetFile: String = "bronze_medal_anim.json.json",
        rewardAmount: Int? = null,
    ) {
        val safeBinding = _binding ?: return
        applyRewardAmount(rewardAmount)
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        applyRewardSlotExtraDownOffset(false)
        safeBinding.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        safeBinding.dailyTasksCompliteBadge.cancelAnimation()
        safeBinding.dailyTasksCompliteBadge.alpha = 0f
        safeBinding.rocketBadgeAnim.removeAllAnimatorListeners()
        safeBinding.rocketBadgeAnim.cancelAnimation()
        safeBinding.rocketBadgeAnim.background = null
        safeBinding.rocketBadgeAnim.clipToOutline = false
        safeBinding.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
        safeBinding.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
        playRocketSingleLottieLoop(sessionId, assetFile)
    }

    fun karateAnim(
        assetFile: String = "karate_anim.json",
        rewardAmount: Int? = null,
    ) {
        val safeBinding = _binding ?: return
        applyRewardAmount(rewardAmount)
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        applyKarateRewardSlotOffset()
        safeBinding.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        safeBinding.dailyTasksCompliteBadge.cancelAnimation()
        safeBinding.dailyTasksCompliteBadge.alpha = 0f
        safeBinding.rocketBadgeAnim.removeAllAnimatorListeners()
        safeBinding.rocketBadgeAnim.cancelAnimation()
        safeBinding.rocketBadgeAnim.background = null
        safeBinding.rocketBadgeAnim.clipToOutline = false
        safeBinding.rocketBadgeAnim.outlineProvider = ViewOutlineProvider.BOUNDS
        safeBinding.rocketBadgeAnim.translationY = -5f * resources.displayMetrics.density
        playRocketSingleLottieHoldAtFrame(sessionId, assetFile, KARATE_HOLD_FRAME)
    }

    private fun replayKarateAnimationOnly() {
        val safeBinding = _binding ?: return
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        playRocketSingleLottieHoldAtFrame(
            sessionId,
            "karate_anim.json",
            KARATE_HOLD_FRAME,
            playbackStartFrame = KARATE_REPLAY_START_FRAME,
        )
    }

    private fun replayCupGoogleAnimationOnly() {
        val safeBinding = _binding ?: return
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        playRocketSingleLottieHoldAtFrame(sessionId, "cup_google_anim.json", CUP_GOOGLE_HOLD_FRAME)
    }

    private fun replayGoldMedalAnimationOnly() {
        val safeBinding = _binding ?: return
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        playRocketSingleLottieLoop(sessionId, "gold_medal_anim.json")
    }

    private fun replaySilverMedalAnimationOnly() {
        val safeBinding = _binding ?: return
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        playRocketSingleLottieLoop(sessionId, "silver_medal_anim.json.json")
    }

    private fun replayBronzeMedalAnimationOnly() {
        val safeBinding = _binding ?: return
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
        playRocketSingleLottieLoop(sessionId, "bronze_medal_anim.json.json")
    }

    private fun playRocketSingleLottieLoop(
        sessionId: Int,
        assetFile: String,
    ) {
        val safeBinding = _binding ?: return
        val top = safeBinding.rocketBadgeAnim
        top.removeAllAnimatorListeners()
        top.cancelAnimation()
        top.setAnimation(assetFile)
        top.repeatCount = -1
        top.speed = 1f
        top.progress = 0f
        top.setMinAndMaxProgress(0f, 1f)
        top.scaleX = 1f
        top.scaleY = 1f
        top.visibility = View.VISIBLE
        top.addLottieOnCompositionLoadedListener {
            if (!isLoopActive || sessionId != modeSessionId) return@addLottieOnCompositionLoadedListener
            top.playAnimation()
        }
    }

    private fun playRocketSingleLottieHoldAtFrame(
        sessionId: Int,
        assetFile: String,
        holdFrame: Float,
        playbackStartFrame: Float? = null,
    ) {
        val safeBinding = _binding ?: return
        val top = safeBinding.rocketBadgeAnim
        top.removeAllAnimatorListeners()
        top.cancelAnimation()
        top.setAnimation(assetFile)
        top.repeatCount = 0
        top.speed = 1f
        top.progress = 0f
        top.setMinAndMaxProgress(0f, 1f)
        top.scaleX = 1f
        top.scaleY = 1f
        top.visibility = View.VISIBLE
        var holdProgress = 1f
        top.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val b = _binding ?: return
                if (!isLoopActive || sessionId != modeSessionId) return
                val v = b.rocketBadgeAnim
                v.setMinAndMaxProgress(0f, 1f)
                v.progress = holdProgress
                v.pauseAnimation()
            }
        })
        top.addLottieOnCompositionLoadedListener { composition ->
            if (!isLoopActive || sessionId != modeSessionId) return@addLottieOnCompositionLoadedListener
            val sf = composition.startFrame
            val ef = composition.endFrame
            holdProgress = frameToProgress(holdFrame, sf, ef)
            val startProgress = playbackStartFrame?.let { frameToProgress(it, sf, ef) } ?: 0f
            if (isLoopActive && sessionId == modeSessionId) {
                if (playbackStartFrame != null && startProgress < 1f) {
                    top.setMinAndMaxProgress(startProgress.coerceIn(0f, 1f), 1f)
                } else {
                    top.setMinAndMaxProgress(0f, 1f)
                }
                top.progress = startProgress.coerceIn(0f, 1f)
                top.playAnimation()
            }
        }
    }

    private fun applyRewardAmount(amount: Int?) {
        val safeBinding = _binding ?: return
        val label = safeBinding.badgeRewardAmount
        rewardAmountScaleAnimator?.cancel()
        rewardAmountScaleAnimator = null
        label.animate().cancel()
        safeBinding.badgeRewardPopBurst.cancelBurst()
        if (amount == null) {
            label.scaleX = 1f
            label.scaleY = 1f
            label.visibility = View.GONE
            return
        }
        label.text = amount.toString()
        label.scaleX = 0f
        label.scaleY = 0f
        label.visibility = View.VISIBLE
        label.post {
            val v = _binding?.badgeRewardAmount ?: return@post
            if (v.visibility != View.VISIBLE) return@post
            val sx = ObjectAnimator.ofFloat(v, View.SCALE_X, 0f, 1f)
            val sy = ObjectAnimator.ofFloat(v, View.SCALE_Y, 0f, 1f)
            val set = AnimatorSet().apply {
                playTogether(sx, sy)
                duration = REWARD_AMOUNT_SCALE_MS
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        val b = _binding ?: return
                        if (b.badgeRewardAmount.visibility != View.VISIBLE) return
                        b.badgeRewardPopBurst.playBurst()
                    }

                    override fun onAnimationCancel(animation: Animator) = Unit
                })
            }
            rewardAmountScaleAnimator = set
            set.start()
        }
    }

    override fun onDestroyView() {
        isLoopActive = false
        modeSessionId++
        clearPendingTopStart()
        collapsePieceRowsPanel()
        rewardAmountScaleAnimator?.cancel()
        rewardAmountScaleAnimator = null
        backPressedCallback?.remove()
        backPressedCallback = null
        binding.badgeRewardAmount.animate().cancel()
        binding.badgeRewardPopBurst.cancelBurst()
        binding.dailyTasksCompliteBadge.removeAllAnimatorListeners()
        binding.rocketBadgeAnim.removeAllAnimatorListeners()
        binding.dailyTasksCompliteBadge.setOnClickListener(null)
        binding.dailyTasksCompliteBadge.cancelAnimation()
        binding.rocketBadgeAnim.cancelAnimation()
        _binding = null
        super.onDestroyView()
    }

    private fun configureBackPressBehavior() {
        backPressedCallback?.remove()
        backPressedCallback = object : OnBackPressedCallback(isCelebrationFlow) {
            override fun handleOnBackPressed() {
                // Level-up akışında geri tuşu devre dışı.
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback!!)
    }

    private fun frameToProgress(targetFrame: Float, startFrame: Float, endFrame: Float): Float {
        val span = (endFrame - startFrame).coerceAtLeast(1f)
        return ((targetFrame - startFrame) / span).coerceIn(0f, 1f)
    }

    private fun isTouchInsideView(event: MotionEvent, view: View): Boolean {
        if (view.visibility != View.VISIBLE) return false
        if (view.width <= 0 || view.height <= 0) return false

        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val x = event.rawX
        val y = event.rawY
        return x >= loc[0] && x <= loc[0] + view.width &&
            y >= loc[1] && y <= loc[1] + view.height
    }
}

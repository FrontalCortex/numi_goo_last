package com.example.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentBadgeBinding
import com.google.android.material.tabs.TabLayout

class BadgeFragment : Fragment() {

    companion object {
        private const val ROCKET_START_OFFSET_MS = 800L
        private const val REWARD_AMOUNT_SCALE_MS = 400L
        private const val TOP_HOLD_FRAME_DART = 0f
        private const val TOP_HOLD_FRAME_ROCKET = 60f
        private const val TOP_HOLD_FRAME_BOWLING = 47f
        private const val TOP_HOLD_FRAME_GOLF = 30f
        private const val CUP_GOOGLE_HOLD_FRAME = 135f
        private const val KARATE_HOLD_FRAME = 1f
        private const val KARATE_REPLAY_START_FRAME = 40f
        /** cupGoogleAnim / karateAnim: ödül rakamı slotunu ekstra aşağı kaydırır (dp). */
        private const val SINGLE_TOP_LOTTIE_REWARD_SLOT_EXTRA_DOWN_DP = 50f
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
    private val binding: FragmentBadgeBinding
        get() = _binding!!

    enum class BadgeAnimMode {
        DAILY_ONLY,
        ROCKET_WITH_BASE,
        BOWLING_WITH_BASE,
        GOLF_WITH_BASE,
        CUP_GOOGLE,
        KARATE,
    }

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
        binding.dailyTasksCompliteBadge.setOnClickListener {
            replayTopAnimationOnly()
        }
        setupModeTabs()
        binding.badgeModeTabs.getTabAt(0)?.select()
        switchBadgeAnimation(BadgeAnimMode.DAILY_ONLY, rewardAmount = 50)
    }

    private fun setupModeTabs() {
        val tabs = binding.badgeModeTabs
        tabs.removeAllTabs()
        tabs.addTab(tabs.newTab().setText("Daily"))
        tabs.addTab(tabs.newTab().setText("Rocket"))
        tabs.addTab(tabs.newTab().setText("Bowling"))
        tabs.addTab(tabs.newTab().setText("Golf"))
        tabs.addTab(tabs.newTab().setText("Cup"))
        tabs.addTab(tabs.newTab().setText("Karate"))
        tabs.clearOnTabSelectedListeners()
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> switchBadgeAnimation(BadgeAnimMode.DAILY_ONLY, rewardAmount = 50)
                    1 -> switchBadgeAnimation(BadgeAnimMode.ROCKET_WITH_BASE, rewardAmount = 100)
                    2 -> switchBadgeAnimation(BadgeAnimMode.BOWLING_WITH_BASE, rewardAmount = 150)
                    3 -> switchBadgeAnimation(BadgeAnimMode.GOLF_WITH_BASE, rewardAmount = 225)
                    4 -> switchBadgeAnimation(BadgeAnimMode.CUP_GOOGLE, rewardAmount = 87)
                    5 -> switchBadgeAnimation(BadgeAnimMode.KARATE, rewardAmount = 4)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
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
        when (currentMode) {
            BadgeAnimMode.CUP_GOOGLE -> {
                replayCupGoogleAnimationOnly()
                return
            }
            BadgeAnimMode.KARATE -> {
                replayKarateAnimationOnly()
                return
            }
            else -> Unit
        }
        val safeBinding = _binding ?: return
        modeSessionId++
        val sessionId = modeSessionId
        clearPendingTopStart()
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
            BadgeAnimMode.CUP_GOOGLE -> Unit
            BadgeAnimMode.KARATE -> Unit
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
        when (mode) {
            BadgeAnimMode.DAILY_ONLY -> dartAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.ROCKET_WITH_BASE -> rocketBadgeAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.BOWLING_WITH_BASE -> bowlingAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.GOLF_WITH_BASE -> golfAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.CUP_GOOGLE -> cupGoogleAnim(rewardAmount = rewardAmount)
            BadgeAnimMode.KARATE -> karateAnim(rewardAmount = rewardAmount)
        }
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
        applyRewardSlotExtraDownOffset(true)
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
        rewardAmountScaleAnimator?.cancel()
        rewardAmountScaleAnimator = null
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

    private fun frameToProgress(targetFrame: Float, startFrame: Float, endFrame: Float): Float {
        val span = (endFrame - startFrame).coerceAtLeast(1f)
        return ((targetFrame - startFrame) / span).coerceIn(0f, 1f)
    }
}

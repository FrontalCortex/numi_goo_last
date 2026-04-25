package com.example.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.ArgbEvaluator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import com.airbnb.lottie.LottieAnimationView
import com.example.app.GlobalLessonData.globalPartId
import com.example.app.GlobalValues.mapFragmentStepIndex
import com.example.app.databinding.FragmentChestResultBinding
import com.example.app.model.LessonItem
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.max

class ChestResult : Fragment() {
    private lateinit var binding: FragmentChestResultBinding

    private lateinit var lessonItem : LessonItem
    private var correctAnswers: Int = 0
    private var totalQuestions: Int = 0
    private var successRate: Float = 0F
    private var time: String = ""
    private var dersPuani: Int = 0
    private var toplamPuan: Int = 0
    private var worstCupTime: Int = 0
    private var targetTimeSeconds: Int = 0
    private var carpan: Float = 1f
    private var scoreCap: Int = 1
    private var cupPoint2Threshold: Int = 1
    private var displayedScore: Int = 0
    private var activeSkippablePhaseAnimator: AnimatorSet? = null
    private var isSkippablePhaseRunning: Boolean = false
    private var bubbleMoveAnimator: AnimatorSet? = null
    private var bubbleImpactAnimator: AnimatorSet? = null
    private var finalScoreAnimator: ValueAnimator? = null
    private var isBubbleSequenceRunning: Boolean = false
    private var isFinalScoreRunning: Boolean = false
    private var hasFinalizedScore: Boolean = false
    private var starSoundPool: SoundPool? = null
    private var starSoundId: Int = 0
    private var isStarSoundLoaded: Boolean = false
    private var star1Played = false
    private var star2Played = false
    private var star3Played = false
    private val runningAnimators = mutableListOf<Animator>()
    private var isViewBeingDestroyed: Boolean = false
    private var shouldIncrementChestRecordBreakMission: Boolean = false
    private val animations = listOf(
        "animation_one.json",
        "animaton_two.json",
        "animaton_three.json",
        "animaton_four.json",
        "animaton_five.json",
        "animaton_six.json",
        "animaton_eight.json",
        "animaton_nine.json",
        "animaton_ten.json",
        "animaton_eleven.json",
        "animaton_twelve.json",
        "animaton_thirteen.json"
    )


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChestResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MainActivityChromeBlocker.acquire(requireActivity())
        isViewBeingDestroyed = false
        lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex)!!
        arguments?.let { bundle ->
            correctAnswers = bundle.getInt("correctAnswers", 0)
            totalQuestions = bundle.getInt("totalQuestions", 0)
            time = bundle.getString("time", "")
            dersPuani = bundle.getInt("dersPuani", 0)
            worstCupTime = bundle.getInt("worstCupTime", 0)
            if (worstCupTime <= 0) {
                worstCupTime = resolveWorstCupTimeFallback()
            }
            successRate = if (totalQuestions > 0) {
                (correctAnswers.toFloat() / totalQuestions.toFloat()) * 100
            } else {
                0f
            }
            targetTimeSeconds = parseTimeToSeconds(time) ?: 0
            carpan = calculateCarpan(targetTimeSeconds, worstCupTime)
            toplamPuan = calculateToplamPuan(dersPuani, carpan)
            scoreCap = resolveScoreCap()
            cupPoint2Threshold = resolveCupPoint2Threshold()
        }
        continueFragment()
        prepareInitialUiState()
        setupStarSound()
        setupSkipToEndOnTap()
        showRandomAnimation()
        setupStarMarkerPositions()
        startRevealSequence()
        record()
    }
    private fun record() {
        if(lessonItem.type== LessonItem.TYPE_CHEST){
            Log.d("kuzuşişs","work")
            if (lessonItem.record == null) {
                lessonItem.record = toplamPuan
                Log.d("kuzuşişs",toplamPuan.toString())

            } else if (toplamPuan > lessonItem.record!!) {
                lessonItem.record = toplamPuan
                shouldIncrementChestRecordBreakMission = true
                Log.d("kuzuşişs1",toplamPuan.toString())
            }
        }

    }

    private fun continueFragment() {
        binding.claimButton.setOnClickListener {
            val shouldPassRecordBreakMission = shouldIncrementChestRecordBreakMission
            shouldIncrementChestRecordBreakMission = false
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.abacusFragmentContainer,
                    ChestFragment().apply {
                        arguments = Bundle().apply {
                            putBoolean(ARG_PENDING_CHEST_RECORD_BREAK_MISSION, shouldPassRecordBreakMission)
                        }
                    },
                )
                .commit()
        }
    }

    private fun showRandomAnimation() {
        val randomAnim = animations.random()
        binding.lottieView.setAnimation(randomAnim)
        binding.lottieView.playAnimation()
    }

    private fun prepareInitialUiState() {
        binding.claimButton.isEnabled = false
        binding.claimButton.visibility = View.INVISIBLE
        binding.successRate.text = "0%"
        binding.totalScoreLargeText.text = "0"
        displayedScore = 0
        hasFinalizedScore = false
        isBubbleSequenceRunning = false
        isFinalScoreRunning = false
        bubbleMoveAnimator = null
        bubbleImpactAnimator = null
        finalScoreAnimator = null
        updateScoreProgressBar(animatedScore = 0, previousScore = 0)
        binding.totalTime.text = "0:00"
        binding.multiplierText.text = "x0.00"
        binding.timeBox.alpha = 0f
        binding.timeBox.visibility = View.INVISIBLE
        binding.multiplierBubble.alpha = 0f
        binding.multiplierBubble.visibility = View.INVISIBLE
        binding.scoreImpactFlash.alpha = 0f
        binding.scoreImpactFlash.visibility = View.INVISIBLE
        star1Played = false
        star2Played = false
        star3Played = false
        resetStarMarker(binding.starMarker1)
        resetStarMarker(binding.starMarker2)
        resetStarMarker(binding.starMarker3)
    }

    private fun startRevealSequence() {
        if (successRate.toInt() <= 0) {
            startPhase2()
            return
        }
        val phase1ScoreAnimator = ValueAnimator.ofInt(0, dersPuani).apply {
            duration = 5000L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val scoreValue = it.animatedValue as Int
                binding.totalScoreLargeText.text = scoreValue.toString()
                updateScoreProgressBar(animatedScore = scoreValue, previousScore = displayedScore)
                displayedScore = scoreValue
            }
        }
        val phase1SuccessAnimator = ValueAnimator.ofInt(0, successRate.toInt()).apply {
            duration = 5000L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                binding.successRate.text = "${it.animatedValue as Int}%"
            }
        }
        val phase1Set = AnimatorSet().apply {
            playTogether(phase1ScoreAnimator, phase1SuccessAnimator)
            isSkippablePhaseRunning = true
            activeSkippablePhaseAnimator = this
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isSkippablePhaseRunning = false
                    activeSkippablePhaseAnimator = null
                    startPhase2()
                }
            })
        }
        trackAndStart(phase1Set)
    }

    private fun startPhase2() {
        binding.timeBox.visibility = View.VISIBLE
        trackAndStart(
            ObjectAnimator.ofFloat(binding.timeBox, View.ALPHA, 0f, 1f).apply {
                duration = 220L
            }
        )
        binding.multiplierBubble.visibility = View.INVISIBLE
        binding.multiplierBubble.alpha = 0f
        positionBubbleOverTimeBox {
            binding.multiplierBubble.visibility = View.VISIBLE
            binding.multiplierBubble.alpha = 1f
        }

        val phase2TimeAnimator = ValueAnimator.ofInt(0, targetTimeSeconds).apply {
            duration = 5000L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                binding.totalTime.text = formatSeconds(it.animatedValue as Int)
            }
        }
        val phase2CarpanAnimator = ValueAnimator.ofFloat(0f, carpan).apply {
            duration = 5000L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                binding.multiplierText.text = "x${String.format("%.2f", value)}"
            }
        }
        val phase2Set = AnimatorSet().apply {
            playTogether(phase2TimeAnimator, phase2CarpanAnimator)
            isSkippablePhaseRunning = true
            activeSkippablePhaseAnimator = this
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isSkippablePhaseRunning = false
                    activeSkippablePhaseAnimator = null
                    animateBubbleToScore()
                }
            })
        }
        trackAndStart(phase2Set)
    }

    private fun animateBubbleToScore() {
        if (hasFinalizedScore) return
        isBubbleSequenceRunning = true
        val bubbleLoc = IntArray(2)
        val scoreLoc = IntArray(2)
        binding.multiplierBubble.getLocationInWindow(bubbleLoc)
        binding.totalScoreLargeText.getLocationInWindow(scoreLoc)

        val bubbleCenterX = bubbleLoc[0] + binding.multiplierBubble.width / 2f
        val bubbleCenterY = bubbleLoc[1] + binding.multiplierBubble.height / 2f
        val scoreCenterX = scoreLoc[0] + binding.totalScoreLargeText.width / 2f
        val scoreCenterY = scoreLoc[1] + binding.totalScoreLargeText.height / 2f
        val deltaX = scoreCenterX - bubbleCenterX
        val deltaY = scoreCenterY - bubbleCenterY

        val moveX = ObjectAnimator.ofFloat(binding.multiplierBubble, View.TRANSLATION_X, 0f, deltaX)
        val moveY = ObjectAnimator.ofFloat(binding.multiplierBubble, View.TRANSLATION_Y, 0f, deltaY)
        val moveSet = AnimatorSet().apply {
            duration = 650L
            interpolator = DecelerateInterpolator(1.4f)
            playTogether(moveX, moveY)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (hasFinalizedScore) return
                    playScoreImpactAndFinalize()
                }
            })
        }
        bubbleMoveAnimator = moveSet
        trackAndStart(moveSet)
    }

    private fun playScoreImpactAndFinalize() {
        val rootLoc = IntArray(2)
        val scoreLoc = IntArray(2)
        binding.root.getLocationInWindow(rootLoc)
        binding.totalScoreLargeText.getLocationInWindow(scoreLoc)
        val impactX = scoreLoc[0] - rootLoc[0] + (binding.totalScoreLargeText.width / 2f) - (binding.scoreImpactFlash.width / 2f)
        val impactY = scoreLoc[1] - rootLoc[1] + (binding.totalScoreLargeText.height / 2f) - (binding.scoreImpactFlash.height / 2f)

        binding.scoreImpactFlash.x = impactX
        binding.scoreImpactFlash.y = impactY
        binding.scoreImpactFlash.visibility = View.VISIBLE
        binding.scoreImpactFlash.scaleX = 0.5f
        binding.scoreImpactFlash.scaleY = 0.5f

        val flashAlpha = ObjectAnimator.ofFloat(binding.scoreImpactFlash, View.ALPHA, 0f, 1f, 0f).apply {
            duration = 260L
        }
        val flashScaleX = ObjectAnimator.ofFloat(binding.scoreImpactFlash, View.SCALE_X, 0.5f, 2f).apply {
            duration = 260L
        }
        val flashScaleY = ObjectAnimator.ofFloat(binding.scoreImpactFlash, View.SCALE_Y, 0.5f, 2f).apply {
            duration = 260L
        }
        val impactSet = AnimatorSet().apply {
            playTogether(flashAlpha, flashScaleX, flashScaleY)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (hasFinalizedScore) return
                    binding.scoreImpactFlash.visibility = View.INVISIBLE
                    binding.multiplierBubble.visibility = View.INVISIBLE
                    animateFinalScoreTransition()
                }
            })
        }
        bubbleImpactAnimator = impactSet
        trackAndStart(impactSet)
    }

    private fun animateFinalScoreTransition() {
        if (hasFinalizedScore) return
        isBubbleSequenceRunning = false
        isFinalScoreRunning = true
        val startScore = displayedScore
        if (startScore == toplamPuan) {
            finalizeScoreDisplay()
            return
        }
        val finalScoreAnimator = ValueAnimator.ofInt(startScore, toplamPuan).apply {
            duration = 2000L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val scoreValue = it.animatedValue as Int
                binding.totalScoreLargeText.text = scoreValue.toString()
                updateScoreProgressBar(animatedScore = scoreValue, previousScore = displayedScore)
                displayedScore = scoreValue
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (hasFinalizedScore) return
                    finalizeScoreDisplay()
                }
            })
        }
        this.finalScoreAnimator = finalScoreAnimator
        trackAndStart(finalScoreAnimator)
    }

    private fun finalizeScoreDisplay() {
        if (hasFinalizedScore) return
        hasFinalizedScore = true
        isBubbleSequenceRunning = false
        isFinalScoreRunning = false
        binding.scoreImpactFlash.visibility = View.INVISIBLE
        binding.multiplierBubble.visibility = View.INVISIBLE
        updateScoreProgressBar(animatedScore = toplamPuan, previousScore = displayedScore)
        displayedScore = toplamPuan
        binding.totalScoreLargeText.text = toplamPuan.toString()
        playFinalScorePop()
        binding.claimButton.visibility = View.VISIBLE
        binding.claimButton.isEnabled = true
    }

    private fun playFinalScorePop() {
        val popX = ObjectAnimator.ofFloat(binding.totalScoreLargeText, View.SCALE_X, 1f, 1.1f, 1f).apply {
            duration = 260L
        }
        val popY = ObjectAnimator.ofFloat(binding.totalScoreLargeText, View.SCALE_Y, 1f, 1.1f, 1f).apply {
            duration = 260L
        }
        trackAndStart(AnimatorSet().apply { playTogether(popX, popY) })
    }

    private fun positionBubbleOverTimeBox(onPositioned: (() -> Unit)? = null) {
        binding.root.post {
            val rootLoc = IntArray(2)
            val timeLoc = IntArray(2)
            binding.root.getLocationInWindow(rootLoc)
            binding.timeBox.getLocationInWindow(timeLoc)
            val centerX = timeLoc[0] - rootLoc[0] + (binding.timeBox.width / 2f) - (binding.multiplierBubble.width / 2f)
            val topY = timeLoc[1] - rootLoc[1] - binding.multiplierBubble.height - 16f
            binding.multiplierBubble.translationX = 0f
            binding.multiplierBubble.translationY = 0f
            binding.multiplierBubble.x = centerX
            binding.multiplierBubble.y = max(0f, topY)
            onPositioned?.invoke()
        }
    }

    private fun calculateCarpan(targetTimeSec: Int, worstCupTime: Int): Float {
        if (worstCupTime <= 0) return 1f
        val rawCarpan = 4f - ((targetTimeSec * 3f) / worstCupTime.toFloat())
        return rawCarpan.coerceAtLeast(1f)
    }

    private fun calculateToplamPuan(dersPuani: Int, carpan: Float): Int {
        return ceil(dersPuani * carpan).toInt()
    }

    private fun updateScoreProgressBar(animatedScore: Int, previousScore: Int) {
        val fill = binding.scoreProgressBarFill
        val empty = binding.scoreProgressBarEmpty

        val clampedScore = animatedScore.coerceIn(0, scoreCap)
        val progress = (clampedScore.toFloat() / scoreCap.toFloat()).coerceIn(0f, 1f)

        val fillParams = fill.layoutParams as android.widget.LinearLayout.LayoutParams
        val emptyParams = empty.layoutParams as android.widget.LinearLayout.LayoutParams
        fillParams.weight = progress
        emptyParams.weight = 1f - progress
        fill.layoutParams = fillParams
        empty.layoutParams = emptyParams

        val colorGreen = Color.parseColor("#7CFC00")
        val colorYellow = Color.parseColor("#FFFF99")
        val colorDarkRed = Color.parseColor("#990000")
        val isIncreasing = animatedScore >= previousScore

        val color = if (isIncreasing) {
            if (progress <= 0.5f) {
                ArgbEvaluator().evaluate(progress * 2f, colorGreen, colorYellow) as Int
            } else {
                ArgbEvaluator().evaluate((progress - 0.5f) * 2f, colorYellow, colorGreen) as Int
            }
        } else {
            ArgbEvaluator().evaluate(progress, colorGreen, colorDarkRed) as Int
        }

        val background = fill.background
        if (background is GradientDrawable) {
            background.setColor(color)
        }

        maybeTriggerStarMarkers(previousScore, animatedScore)
    }

    private fun resolveWorstCupTimeFallback(): Int {
        val fromCurrent = LessonManager.getLessonItem(mapFragmentStepIndex)?.worstCupTime
        if (fromCurrent != null && fromCurrent > 0) return fromCurrent

        val fromTemplate = GlobalLessonData.createLessonItems(globalPartId)
            .getOrNull(mapFragmentStepIndex)
            ?.worstCupTime
        if (fromTemplate != null && fromTemplate > 0) return fromTemplate

        // Nihai fallback: çarpanı devre dışı bırakmamak için güvenli varsayılan.
        return 240
    }

    private fun resolveScoreCap(): Int {
        val currentCupPoint = LessonManager.getLessonItem(mapFragmentStepIndex)?.cupPoint1
        if (currentCupPoint != null && currentCupPoint > 0) return currentCupPoint

        val templateCupPoint = GlobalLessonData.createLessonItems(globalPartId)
            .getOrNull(mapFragmentStepIndex)
            ?.cupPoint1
        if (templateCupPoint != null && templateCupPoint > 0) return templateCupPoint

        return 1
    }

    private fun resolveCupPoint2Threshold(): Int {
        val currentCupPoint2 = LessonManager.getLessonItem(mapFragmentStepIndex)?.cupPoint2
        if (currentCupPoint2 != null && currentCupPoint2 > 0) return currentCupPoint2

        val templateCupPoint2 = GlobalLessonData.createLessonItems(globalPartId)
            .getOrNull(mapFragmentStepIndex)
            ?.cupPoint2
        if (templateCupPoint2 != null && templateCupPoint2 > 0) return templateCupPoint2

        return max(1, scoreCap / 2)
    }

    private fun setupStarMarkerPositions() {
        binding.scoreProgressZone.post {
            val width = binding.scoreProgressZone.width.toFloat().coerceAtLeast(1f)
            placeStarMarker(binding.starMarker1, threshold = 500, zoneWidth = width)
            placeStarMarker(binding.starMarker2, threshold = cupPoint2Threshold, zoneWidth = width)
            placeStarMarker(binding.starMarker3, threshold = scoreCap, zoneWidth = width)
        }
    }

    private fun placeStarMarker(marker: LottieAnimationView, threshold: Int, zoneWidth: Float) {
        val safeThreshold = threshold.coerceIn(0, scoreCap)
        val ratio = (safeThreshold.toFloat() / scoreCap.toFloat()).coerceIn(0f, 1f)
        val centerX = ratio * zoneWidth
        // Clamp yok: yıldızların progress bar sınırları dışına taşmasına izin ver.
        val x = centerX - marker.width / 2f
        val barCenterY = binding.scoreProgressBarContainer.y + (binding.scoreProgressBarContainer.height / 2f)
        val y = barCenterY - (marker.height / 2f)
        marker.x = x
        marker.y = y
    }

    private fun maybeTriggerStarMarkers(previousScore: Int, currentScore: Int) {
        val wasAtOrAbove500 = previousScore >= 500
        val isAtOrAbove500 = currentScore >= 500
        if (!star1Played && !wasAtOrAbove500 && isAtOrAbove500) {
            playStarSoundForTier(1)
            playStarOnceAndHold(binding.starMarker1)
            star1Played = true
        }

        val wasAtOrAboveCup2 = previousScore >= cupPoint2Threshold
        val isAtOrAboveCup2 = currentScore >= cupPoint2Threshold
        if (!star2Played && !wasAtOrAboveCup2 && isAtOrAboveCup2) {
            playStarSoundForTier(2)
            playStarOnceAndHold(binding.starMarker2)
            star2Played = true
        }

        val wasAtOrAboveCup1 = previousScore >= scoreCap
        val isAtOrAboveCup1 = currentScore >= scoreCap
        if (!star3Played && !wasAtOrAboveCup1 && isAtOrAboveCup1) {
            playStarSoundForTier(3)
            playStarOnceAndHold(binding.starMarker3)
            star3Played = true
        }
    }

    private fun playStarOnceAndHold(marker: LottieAnimationView) {
        marker.visibility = View.VISIBLE
        marker.repeatCount = 0
        marker.progress = 0f
        marker.playAnimation()
        marker.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                marker.progress = 1f
                marker.removeAnimatorListener(this)
            }
        })
    }

    private fun resetStarMarker(marker: LottieAnimationView) {
        marker.cancelAnimation()
        marker.progress = 0f
        marker.visibility = View.VISIBLE
    }

    private fun setupStarSound() {
        starSoundPool?.release()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        starSoundPool = SoundPool.Builder()
            .setAudioAttributes(attrs)
            .setMaxStreams(4)
            .build().also { pool ->
                isStarSoundLoaded = false
                starSoundId = pool.load(requireContext(), R.raw.star_sound_effect, 1)
                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0 && sampleId == starSoundId) {
                        isStarSoundLoaded = true
                    }
                }
            }
    }

    private fun playStarSoundForTier(tier: Int) {
        if (!isStarSoundLoaded || starSoundId == 0) return
        val rate = when (tier) {
            1 -> 0.9f  // daha kalin
            2 -> 1.0f  // normal
            3 -> 1.12f // daha ince
            else -> 1.0f
        }
        starSoundPool?.play(starSoundId, 1f, 1f, 1, 0, rate)
    }

    private fun parseTimeToSeconds(value: String): Int? {
        val parts = value.split(":").map { it.trim() }
        return when (parts.size) {
            2 -> {
                val minutes = parts[0].toIntOrNull() ?: return null
                val seconds = parts[1].toIntOrNull() ?: return null
                minutes * 60 + seconds
            }
            3 -> {
                val hours = parts[0].toIntOrNull() ?: return null
                val minutes = parts[1].toIntOrNull() ?: return null
                val seconds = parts[2].toIntOrNull() ?: return null
                hours * 3600 + minutes * 60 + seconds
            }
            else -> null
        }
    }

    private fun formatSeconds(seconds: Int): String {
        val safeSeconds = max(0, seconds)
        val minutes = safeSeconds / 60
        val remainingSeconds = safeSeconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    private fun trackAndStart(animator: Animator) {
        if (isViewBeingDestroyed || !isAdded || view == null) return
        runningAnimators.add(animator)
        animator.start()
    }

    private fun setupSkipToEndOnTap() {
        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                when {
                    isSkippablePhaseRunning -> activeSkippablePhaseAnimator?.end()
                    isBubbleSequenceRunning -> {
                        bubbleMoveAnimator?.cancel()
                        bubbleImpactAnimator?.cancel()
                        finalizeScoreDisplay()
                    }
                    isFinalScoreRunning -> {
                        finalScoreAnimator?.cancel()
                        finalizeScoreDisplay()
                    }
                }
            }
            false
        }
    }

    override fun onDestroyView() {
        isViewBeingDestroyed = true
        activeSkippablePhaseAnimator = null
        bubbleMoveAnimator = null
        bubbleImpactAnimator = null
        finalScoreAnimator = null
        isSkippablePhaseRunning = false
        isBubbleSequenceRunning = false
        isFinalScoreRunning = false
        hasFinalizedScore = false
        starSoundPool?.release()
        starSoundPool = null
        starSoundId = 0
        isStarSoundLoaded = false
        val animatorsToCancel = runningAnimators.toList()
        runningAnimators.clear()
        animatorsToCancel.forEach { it.cancel() }
        MainActivityChromeBlocker.release(activity)
        super.onDestroyView()
    }

    companion object {
        const val ARG_PENDING_CHEST_RECORD_BREAK_MISSION = "pending_chest_record_break_mission"
    }

}
package com.example.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app.databinding.FragmentTasksBinding
import kotlin.math.abs
import kotlin.math.roundToInt

class TasksFragment : Fragment() {
    private lateinit var binding: FragmentTasksBinding
    private lateinit var bulletinAdapter: BulletinAdapter
    private var part1Sources: List<DailyQuestionSource> = emptyList()
    private var dailyCardState: DailyQuestionCardUiState = defaultDailyCardState()
    private var lastDailyPeriodRolloverRefreshMs = 0L
    /** Aynı periyotta "Mücadeleyi tamamladın!" yalnızca bir kez gösterilir. */
    private var dailyQuestionCompleteToastShownForPeriod: String? = null
    private var pendingLaunchAfterBrokenHeartHeal: (() -> Unit)? = null
    /** 1 elmas ile devam: dialog kapanınca touch blocker'ı kaldırma. */
    private var dailyQuestionDiamondContinueInFlight = false

    companion object {
        private const val PRACTICE_TOUCH_BLOCKER_TAG = MainActivity.PRACTICE_TOUCH_BLOCKER_TAG
        private const val VIEW_TYPE_STANDARD = 0
        private const val VIEW_TYPE_DAILY_QUESTION = 1
        private const val DAILY_PROGRESS_ANIM_DURATION_MS = 2800L
        private const val CLAIM_READY_VISUAL_PERCENT = 99.5f
    }

    private sealed class BulletinRow {
        abstract val id: String

        data class Standard(
            override val id: String,
            val title: String,
            val subtitle: String,
            val iconRes: Int? = null,
            val colorRes: Int? = null,
        ) : BulletinRow()

        data class DailyQuestion(
            val state: DailyQuestionCardUiState,
        ) : BulletinRow() {
            override val id: String = "daily_question_card"
        }
    }

    private class BulletinAdapter(
        private val onClick: (BulletinRow) -> Unit,
        private val onDailyQuestionCardClick: (DailyQuestionCardUiState?) -> Unit,
        private val onDailyQuestionProgressClaim: (String) -> Unit,
        private val onDailyQuestionProgressIncompleteTap: () -> Unit,
        private val onDailyQuestionPeriodRolledOver: () -> Unit,
        private val onBrokenHeartHealFinished: () -> Unit,
    ) : ListAdapter<BulletinRow, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<BulletinRow>() {
            override fun areItemsTheSame(oldItem: BulletinRow, newItem: BulletinRow): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: BulletinRow, newItem: BulletinRow): Boolean =
                oldItem == newItem
        },
    ) {
        override fun getItemViewType(position: Int): Int = when (getItem(position)) {
            is BulletinRow.DailyQuestion -> VIEW_TYPE_DAILY_QUESTION
            is BulletinRow.Standard -> VIEW_TYPE_STANDARD
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                VIEW_TYPE_DAILY_QUESTION -> {
                    val view = inflater.inflate(R.layout.item_bulletin_daily_question_card, parent, false)
                    DailyQuestionVH(
                        view,
                        onCardClick = onDailyQuestionCardClick,
                        onProgressClaimClick = onDailyQuestionProgressClaim,
                        onProgressIncompleteTap = onDailyQuestionProgressIncompleteTap,
                        onPeriodRolledOver = onDailyQuestionPeriodRolledOver,
                        onBrokenHeartHealFinished = onBrokenHeartHealFinished,
                    )
                }
                else -> {
                    val view = inflater.inflate(R.layout.item_bulletin_card, parent, false)
                    StandardVH(view) { pos -> onClick(getItem(pos)) }
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = getItem(position)) {
                is BulletinRow.Standard -> (holder as StandardVH).bind(item)
                is BulletinRow.DailyQuestion -> (holder as DailyQuestionVH).bind(item.state)
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            if (holder is DailyQuestionVH) holder.cancelAnim()
            super.onViewRecycled(holder)
        }

        private class StandardVH(
            itemView: View,
            onClick: (Int) -> Unit,
        ) : RecyclerView.ViewHolder(itemView) {
            private val title: TextView = itemView.findViewById(R.id.bulletinCardTitle)
            private val titleIcon: View = itemView.findViewById(R.id.bulletinCardTitleIcon)
            private val subtitle: TextView = itemView.findViewById(R.id.bulletinCardSubtitle)

            init {
                itemView.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) onClick(pos)
                }
            }

            fun bind(item: BulletinRow.Standard) {
                title.text = item.title
                subtitle.text = item.subtitle
                
                if (item.iconRes != null) {
                    titleIcon.visibility = View.VISIBLE
                    (titleIcon as android.widget.ImageView).setImageResource(item.iconRes)
                } else {
                    titleIcon.visibility = View.GONE
                }
                
                val cardView = itemView as com.google.android.material.card.MaterialCardView
                if (item.colorRes != null) {
                    cardView.setCardBackgroundColor(
                        androidx.core.content.ContextCompat.getColor(itemView.context, item.colorRes)
                    )
                } else {
                    cardView.setCardBackgroundColor(
                        androidx.core.content.ContextCompat.getColor(itemView.context, R.color.button_enabled)
                    )
                }
            }
        }

        private class DailyQuestionVH(
            itemView: View,
            private val onCardClick: (DailyQuestionCardUiState?) -> Unit,
            private val onProgressClaimClick: (String) -> Unit,
            private val onProgressIncompleteTap: () -> Unit,
            private val onPeriodRolledOver: () -> Unit,
            private val onBrokenHeartHealFinished: () -> Unit,
        ) : RecyclerView.ViewHolder(itemView) {
            private var boundPeriodKey: String = ""
            private var boundState: DailyQuestionCardUiState? = null
            private val progressZone: View = itemView.findViewById(R.id.dailyQuestionProgressZone)
            private val unitSubtitle: TextView = itemView.findViewById(R.id.dailyQuestionUnitSubtitle)
            private val progressTrack: View = itemView.findViewById(R.id.dailyQuestionProgressTrack)
            private val progressFill: View = itemView.findViewById(R.id.dailyQuestionProgressFill)
            private val progressShine: View = itemView.findViewById(R.id.dailyQuestionProgressShine)
            private val progressText: TextView = itemView.findViewById(R.id.dailyQuestionProgressText)
            private val focusLabel: TextView = itemView.findViewById(R.id.dailyQuestionFocusLabel)
            private val renewCountdown: TextView = itemView.findViewById(R.id.dailyQuestionRenewCountdown)
            private val brokenHeart: LottieAnimationView = itemView.findViewById(R.id.dailyQuestionBrokenHeart)

            private val countdownHandler = Handler(Looper.getMainLooper())
            private var brokenHeartEndListener: Animator.AnimatorListener? = null
            private var countdownRunnable: Runnable? = null
            private var animator: ValueAnimator? = null
            private var pendingWidthListener: ViewTreeObserver.OnGlobalLayoutListener? = null
            private var lastVisualPercent = 0f

            init {
                (itemView as? MaterialCardView)?.apply {
                    clipChildren = false
                    clipToPadding = false
                    clipToOutline = false
                }
                itemView.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        onCardClick(boundState)
                    }
                }
                progressZone.setOnClickListener {
                    val ui = boundState ?: return@setOnClickListener
                    if (ui.rewardClaimed) return@setOnClickListener
                    if (isProgressClaimTapReady()) {
                        performProgressClaim(ui)
                    } else {
                        onProgressIncompleteTap()
                    }
                }
            }

            private fun performProgressClaim(ui: DailyQuestionCardUiState) {
                animator?.cancel()
                animator = null
                boundState = ui.copy(rewardClaimed = true)
                progressTrack.post {
                    runWhenTrackHasWidth { applyVisualPercent(100f) }
                }
                onProgressClaimClick(ui.periodKey)
            }

            fun cancelAnim() {
                stopCountdownTicker()
                brokenHeartEndListener?.let { brokenHeart.removeAnimatorListener(it) }
                brokenHeartEndListener = null
                brokenHeart.cancelAnimation()
                animator?.cancel()
                animator = null
                pendingWidthListener?.let { listener ->
                    if (progressTrack.viewTreeObserver.isAlive) {
                        progressTrack.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                    }
                    pendingWidthListener = null
                }
            }

            private fun stopCountdownTicker() {
                countdownRunnable?.let { countdownHandler.removeCallbacks(it) }
                countdownRunnable = null
            }

            private fun isProgressClaimTapReady(): Boolean {
                val ui = boundState ?: return false
                return ui.canClaimReward &&
                    lastVisualPercent >= CLAIM_READY_VISUAL_PERCENT &&
                    animator?.isRunning != true
            }

            private fun updateProgressClaimTapEnabled() {
                val canTapProgress = boundState?.rewardClaimed != true
                progressZone.isClickable = canTapProgress
                progressZone.isFocusable = canTapProgress
            }

            private fun applyVisualPercent(pct: Float) {
                val ui = boundState ?: return
                lastVisualPercent = pct
                val ctx = itemView.context
                val total = ui.totalCount.coerceAtLeast(1)
                val curEst = (total * pct / 100f).roundToInt().coerceIn(0, total)
                val atFullVisual = pct >= CLAIM_READY_VISUAL_PERCENT
                val showClaimed = ui.rewardClaimed && atFullVisual
                val showClaimReady = ui.canClaimReward && atFullVisual
                when {
                    showClaimed -> {
                        progressTrack.setBackgroundResource(R.drawable.mission_progress_track)
                        applyMissionProgressOverlayNow(
                            widthHost = progressTrack,
                            fill = progressFill,
                            shine = progressShine,
                            percent = pct,
                            done = false,
                            claimed = true,
                        )
                    }
                    showClaimReady -> {
                        progressTrack.setBackgroundResource(R.drawable.daily_question_progress_track)
                        applyDailyQuestionProgressOverlayNow(
                            widthHost = progressTrack,
                            fill = progressFill,
                            shine = progressShine,
                            percent = pct,
                            complete = true,
                        )
                    }
                    else -> {
                        progressTrack.setBackgroundResource(R.drawable.daily_question_progress_track)
                        applyDailyQuestionProgressOverlayNow(
                            widthHost = progressTrack,
                            fill = progressFill,
                            shine = progressShine,
                            percent = pct,
                            complete = false,
                        )
                    }
                }
                when {
                    showClaimed -> {
                        progressText.text = ctx.getString(R.string.mission_reward_claimed_label)
                        progressText.setTextColor(ContextCompat.getColor(ctx, R.color.black))
                    }
                    showClaimReady -> {
                        progressText.text = ctx.getString(R.string.mission_completed_label)
                        progressText.setTextColor(
                            ContextCompat.getColor(ctx, R.color.background_color),
                        )
                    }
                    else -> {
                        progressText.text = ctx.getString(
                            R.string.daily_question_progress_format,
                            curEst.coerceAtMost(total),
                            total,
                        )
                        progressText.setTextColor(
                            ContextCompat.getColor(ctx, android.R.color.white),
                        )
                    }
                }
                updateProgressClaimTapEnabled()
            }

            private fun persistProgressShown() {
                val ui = boundState ?: return
                if (!ui.isLoaded) return
                val solved = ui.solvedCount.coerceIn(0, ui.totalCount.coerceAtLeast(1))
                DailyQuestionCardProgressAnimStore.setLastShown(
                    itemView.context,
                    ui.periodKey,
                    solved,
                )
            }

            private fun startCountdownTicker(periodKey: String) {
                stopCountdownTicker()
                boundPeriodKey = periodKey
                val tick = object : Runnable {
                    override fun run() {
                        if (bindingAdapterPosition == RecyclerView.NO_POSITION) return
                        val currentPeriodKey = DailyQuestionPeriod.currentPeriodKey()
                        if (currentPeriodKey != boundPeriodKey) {
                            onPeriodRolledOver()
                            return
                        }
                        val remaining = DailyQuestionPeriod.millisUntilCurrentPeriodEnds()
                        renewCountdown.text = itemView.context.getString(
                            R.string.daily_question_refresh_countdown,
                            DailyQuestionPeriod.formatCountdown(remaining),
                        )
                        when {
                            remaining > 0L -> countdownHandler.postDelayed(this, 1000L)
                            else -> countdownHandler.postDelayed(this, 250L)
                        }
                    }
                }
                countdownRunnable = tick
                tick.run()
            }

            fun bind(state: DailyQuestionCardUiState) {
                cancelAnim()
                boundState = state
                val ctx = itemView.context
                progressZone.isClickable = false
                progressZone.isFocusable = false
                val titleUnit = state.titleUnit.ifBlank {
                    ctx.getString(R.string.daily_question_card_subtitle_default)
                }
                unitSubtitle.text = titleUnit
                startCountdownTicker(state.periodKey)

                if (state.isComplete || state.difficulty.isBlank()) {
                    focusLabel.visibility = View.GONE
                } else {
                    focusLabel.visibility = View.VISIBLE
                    focusLabel.text = ctx.getString(
                        R.string.daily_question_focus_format,
                        state.difficulty,
                    )
                }

                val total = state.totalCount.coerceAtLeast(1)
                val solved = state.solvedCount.coerceIn(0, total)
                val endPct = (solved * 100f) / total

                bindBrokenHeart(state)

                if (!state.isLoaded) {
                    progressTrack.post {
                        runWhenTrackHasWidth {
                            applyVisualPercent(endPct)
                        }
                    }
                    return
                }

                val lastShown = DailyQuestionCardProgressAnimStore.getLastShown(ctx, state.periodKey)
                val fromSolved = when {
                    lastShown == null -> solved
                    lastShown > solved -> solved
                    else -> lastShown
                }
                val startPct = (fromSolved * 100f) / total

                progressTrack.post {
                    runWhenTrackHasWidth {
                        applyVisualPercent(startPct)
                        val shouldAnimate = lastShown != null &&
                            solved > fromSolved &&
                            abs(endPct - startPct) >= 0.01f
                        if (!shouldAnimate) {
                            applyVisualPercent(endPct)
                            persistProgressShown()
                            return@runWhenTrackHasWidth
                        }
                        animator = ValueAnimator.ofFloat(startPct, endPct).apply {
                            duration = DAILY_PROGRESS_ANIM_DURATION_MS
                            interpolator = DecelerateInterpolator(1.6f)
                            addUpdateListener { va ->
                                applyVisualPercent(va.animatedValue as Float)
                            }
                            addListener(object : android.animation.AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: android.animation.Animator) {
                                    animator = null
                                    persistProgressShown()
                                    updateProgressClaimTapEnabled()
                                }

                                override fun onAnimationCancel(animation: android.animation.Animator) {
                                    animator = null
                                    persistProgressShown()
                                    updateProgressClaimTapEnabled()
                                }
                            })
                            start()
                        }
                    }
                }
            }

            private fun runWhenTrackHasWidth(block: () -> Unit) {
                if (progressTrack.width > 0) {
                    block()
                    return
                }
                val observer = progressTrack.viewTreeObserver
                val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (progressTrack.width <= 0) return
                        observer.removeOnGlobalLayoutListener(this)
                        pendingWidthListener = null
                        block()
                    }
                }
                pendingWidthListener = listener
                observer.addOnGlobalLayoutListener(listener)
            }

            private fun bindBrokenHeart(state: DailyQuestionCardUiState) {
                val ctx = itemView.context
                brokenHeart.setAnimation("broken_heart_anim.json")
                brokenHeart.repeatCount = 0
                clearBrokenHeartListener()
                brokenHeart.cancelAnimation()
                if (!state.isLoaded) {
                    showBrokenHeartFrame(0)
                    return
                }
                when {
                    DailyQuestionBrokenHeartStore.consumeHealPlay(ctx, state.periodKey) -> {
                        playBrokenHeartHealThenFirstFrame(state.periodKey)
                    }
                    DailyQuestionBrokenHeartStore.consumePlayRequest(ctx, state.periodKey) -> {
                        playBrokenHeartBreakToHold116(state.periodKey)
                    }
                    DailyQuestionBrokenHeartStore.isBrokenHold116(ctx, state.periodKey) -> {
                        showBrokenHeartFrame(DailyQuestionPeriod.BROKEN_HEART_HOLD_FRAME)
                    }
                    else -> showBrokenHeartFrame(0)
                }
            }

            private fun clearBrokenHeartListener() {
                brokenHeartEndListener?.let { brokenHeart.removeAnimatorListener(it) }
                brokenHeartEndListener = null
            }

            private fun showBrokenHeartFrame(frame: Int) {
                val maxFrame = brokenHeart.maxFrame.toInt().coerceAtLeast(0)
                brokenHeart.setMinAndMaxFrame(0, maxFrame)
                brokenHeart.frame = frame.coerceIn(0, maxFrame)
                brokenHeart.pauseAnimation()
            }

            private fun playBrokenHeartBreakToHold116(periodKey: String) {
                val ctx = itemView.context
                val hold = DailyQuestionPeriod.BROKEN_HEART_HOLD_FRAME
                val maxFrame = brokenHeart.maxFrame.toInt().coerceAtLeast(hold)
                brokenHeart.frame = 0
                brokenHeart.setMinAndMaxFrame(0, hold)
                val listener = object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        clearBrokenHeartListener()
                        brokenHeart.pauseAnimation()
                        showBrokenHeartFrame(hold)
                        DailyQuestionBrokenHeartStore.setBrokenHold116(ctx, periodKey, true)
                    }
                }
                brokenHeartEndListener = listener
                brokenHeart.addAnimatorListener(listener)
                brokenHeart.playAnimation()
            }

            private fun playBrokenHeartHealThenFirstFrame(periodKey: String) {
                val ctx = itemView.context
                val hold = DailyQuestionPeriod.BROKEN_HEART_HOLD_FRAME
                val maxFrame = brokenHeart.maxFrame.toInt().coerceAtLeast(hold)
                showBrokenHeartFrame(hold)
                brokenHeart.setMinAndMaxFrame(hold, maxFrame)
                val listener = object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        clearBrokenHeartListener()
                        DailyQuestionBrokenHeartStore.clearBrokenHold116(ctx, periodKey)
                        showBrokenHeartFrame(0)
                        onBrokenHeartHealFinished()
                    }
                }
                brokenHeartEndListener = listener
                brokenHeart.addAnimatorListener(listener)
                brokenHeart.playAnimation()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bulletinAdapter = BulletinAdapter(
            onClick = { row ->
                when (row) {
                    is BulletinRow.Standard -> {
                        if (row.id == "feedback_card") {
                            openAbacusContainerFragment(FeedbackFragment())
                        } else {
                            openAbacusContainerFragment(AbacusPracticeFragment())
                        }
                    }
                    else -> Unit
                }
            },
            onDailyQuestionCardClick = { displayState ->
                addLaunchTouchBlocker()
                handleDailyQuestionCardClick(displayState)
            },
            onDailyQuestionProgressClaim = { periodKey -> onDailyQuestionProgressClaimTapped(periodKey) },
            onDailyQuestionProgressIncompleteTap = { showDailyQuestionClaimRequiresCompleteToast() },
            onDailyQuestionPeriodRolledOver = { onDailyQuestionPeriodRolledOver() },
            onBrokenHeartHealFinished = { onDailyQuestionBrokenHeartHealFinished() },
        )

        binding.tasksRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.tasksRecycler.adapter = bulletinAdapter
        submitBulletinList()
        refreshDailyQuestionCard()
    }

    override fun onResume() {
        super.onResume()
        releaseLaunchTouchBlocker()
        refreshDailyQuestionCard()
        val main = activity as? MainActivity
        main?.scheduleReconcileAbacusOverlayWhenTasksIsBase()
        main?.logTouchDiag("TasksFragment.onResume")
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            releaseLaunchTouchBlocker()
            refreshDailyQuestionCard()
            (activity as? MainActivity)?.scheduleReconcileAbacusOverlayWhenTasksIsBase()
        }
    }

    private fun submitBulletinList() {
        bulletinAdapter.submitList(
            listOf(
                BulletinRow.Standard(
                    id = "daily_card",
                    title = "Abaküs",
                    subtitle = "Abaküste pratik yaparak kendini geliştir.",
                    iconRes = R.drawable.mini_abacus_ic
                ),
                BulletinRow.DailyQuestion(dailyCardState),
                BulletinRow.Standard(
                    id = "feedback_card",
                    title = "Bize Ulaşın",
                    subtitle = "Bir sorun mu yaşadınız? Görüşlerinizi ve önerilerinizi bizimle paylaşın.",
                    iconRes = R.drawable.feedback_ic,
                    colorRes = android.R.color.holo_blue_dark
                ),
            ),
        )
    }

    /** Periyot bittiğinde kartı Tasks’tayken anında yeniler. */
    private fun onDailyQuestionPeriodRolledOver() {
        if (!isAdded) return
        dailyQuestionCompleteToastShownForPeriod = null
        if (dailyCardState.periodKey.isNotEmpty()) {
            DailyQuestionBrokenHeartStore.resetForPeriod(requireContext(), dailyCardState.periodKey)
        }
        if (dailyCardState.periodKey == DailyQuestionPeriod.currentPeriodKey()) return
        val now = System.currentTimeMillis()
        if (now - lastDailyPeriodRolloverRefreshMs < 400L) return
        lastDailyPeriodRolloverRefreshMs = now
        refreshDailyQuestionCard()
    }

    private fun refreshDailyQuestionCard() {
        loadPart1Sources { sources ->
            if (!isAdded) return@loadPart1Sources
            part1Sources = sources
            DailyQuestionRepository.loadChallengeForCard(requireContext(), sources) { state ->
                if (!isAdded) return@loadChallengeForCard
                dailyCardState = state ?: defaultDailyCardState(poolAvailable = sources.isNotEmpty())
                submitBulletinList()
            }
        }
    }

    private fun showDailyQuestionClaimRequiresCompleteToast() {
                    Toast.makeText(
                        requireContext(),
            R.string.daily_question_claim_requires_complete,
                        Toast.LENGTH_SHORT,
                    ).show()
    }

    private fun showDailyQuestionChallengeCompleteToast(periodKey: String) {
        if (periodKey.isEmpty()) return
        if (dailyQuestionCompleteToastShownForPeriod == periodKey) return
        dailyQuestionCompleteToastShownForPeriod = periodKey
                    Toast.makeText(
                        requireContext(),
            R.string.daily_question_challenge_complete,
                        Toast.LENGTH_SHORT,
                    ).show()
    }

    private fun handleDailyQuestionCardClick(displayState: DailyQuestionCardUiState? = null) {
        val state = displayState ?: dailyCardState
        if (state.shouldShowChallengeCompleteToast()) {
            showDailyQuestionChallengeCompleteToast(state.periodKey)
            releaseLaunchTouchBlocker()
            return
        }
        if (state.rewardClaimed) {
                    releaseLaunchTouchBlocker()
            return
        }
        loadPart1Sources { sources ->
            if (!isAdded) return@loadPart1Sources
            if (sources.isEmpty()) {
                showDailyQuestionPoolEmptyMessage()
                return@loadPart1Sources
            }
            DailyQuestionRepository.loadOrCreateChallenge(requireContext(), sources) { challenge ->
                if (!isAdded) return@loadOrCreateChallenge
                if (challenge == null) {
                    showDailyQuestionPoolEmptyMessage()
                    return@loadOrCreateChallenge
                }
                        when {
                    challenge.rewardClaimed -> {
                        releaseLaunchTouchBlocker()
                    }
                    challenge.isComplete ||
                        challenge.solvedCount >= DailyQuestionPeriod.QUESTIONS_PER_PERIOD -> {
                        showDailyQuestionChallengeCompleteToast(challenge.periodKey)
                        releaseLaunchTouchBlocker()
                    }
                    challenge.needsDiamondContinue -> {
                        showDailyQuestionContinuePanel(challenge)
                    }
                    else -> {
                        val slot = challenge.slotForPlay() ?: run {
                            releaseLaunchTouchBlocker()
                            return@loadOrCreateChallenge
                        }
                        launchDailyQuestionLesson(challenge, slot)
                    }
                }
            }
        }
    }

    private fun onDailyQuestionBrokenHeartHealFinished() {
        pendingLaunchAfterBrokenHeartHeal?.invoke()
        pendingLaunchAfterBrokenHeartHeal = null
    }

    private fun launchDailyQuestionLesson(
        challenge: DailyQuestionChallenge,
        slot: DailyQuestionSlot,
    ) {
        val slotIndex = challenge.playSlotIndex()
        openAbacusContainerFragment(
            BlindingLessonFragment.newDailyQuestionInstance(
                operations = listOf(slot.sequence),
                periodKey = challenge.periodKey,
                slotIndex = slotIndex,
            ),
        )
    }

    private fun showDailyQuestionContinuePanel(challenge: DailyQuestionChallenge) {
        if (!isAdded) return
        dailyQuestionDiamondContinueInFlight = false
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.panel_daily_question_continue)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val width = (resources.displayMetrics.widthPixels * 0.88f).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCanceledOnTouchOutside(true)

        val closeButton = dialog.findViewById<View>(R.id.dailyQuestionContinueClose)
        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.findViewById<MaterialButton>(R.id.dailyQuestionContinueDiamond).setOnClickListener {
            val main = activity as? MainActivity
            if (main?.spendKeys(DailyQuestionPeriod.KEY_CONTINUE_COST) != true) {
                    Toast.makeText(
                        requireContext(),
                    R.string.daily_question_insufficient_keys,
                        Toast.LENGTH_SHORT,
                    ).show()
                return@setOnClickListener
            }
            val slot = challenge.slotForPlay() ?: return@setOnClickListener
            val clearedChallenge = challenge.copy(pendingContinueSlotIndex = null)
            dailyQuestionDiamondContinueInFlight = true
            dialog.dismiss()
            addLaunchTouchBlocker()
            DailyQuestionRepository.clearPendingDiamondContinue(requireContext(), challenge.periodKey) { _ ->
                if (!isAdded) {
                    dailyQuestionDiamondContinueInFlight = false
                    releaseLaunchTouchBlocker()
                    return@clearPendingDiamondContinue
                }
                pendingLaunchAfterBrokenHeartHeal = healLaunch@{
                    if (!isAdded) {
                        releaseLaunchTouchBlocker()
                        return@healLaunch
                    }
                    launchDailyQuestionLesson(clearedChallenge, slot)
                }
                DailyQuestionBrokenHeartStore.requestHealPlay(requireContext(), challenge.periodKey)
                dailyCardState = clearedChallenge.toCardUiState(
                    poolAvailable = part1Sources.isNotEmpty(),
                )
                submitBulletinList()
            }
        }
        dialog.setOnDismissListener {
            dialog.findViewById<LottieAnimationView>(R.id.dailyQuestionContinueBandagedHeart)
                ?.cancelAnimation()
            if (dailyQuestionDiamondContinueInFlight) {
                dailyQuestionDiamondContinueInFlight = false
            } else {
                releaseLaunchTouchBlocker()
            }
        }
        dialog.show()
        dialog.window?.decorView?.post {
            closeButton.isPressed = false
            closeButton.refreshDrawableState()
            dialog.window?.decorView?.findFocus()?.clearFocus()
            releaseLaunchTouchBlocker()
        }
    }

    private fun onDailyQuestionProgressClaimTapped(periodKey: String) {
        dailyQuestionCompleteToastShownForPeriod = periodKey
        addLaunchTouchBlocker()
        dailyCardState = dailyCardState.copy(
            rewardClaimed = true,
            isLoaded = true,
        )
        submitBulletinList()
        startDailyQuestionRewardFlow(periodKey)
    }

    private fun startDailyQuestionRewardFlow(periodKey: String) {
        BadgeProgressFirestore.incrementBadgeProgressAndDetectLevelUp(
            incrementDart = false,
            incrementBowlingBy = 0,
            incrementKarate = false,
            incrementRocketDailyLessons = false,
            incrementGolf = false,
            incrementFishing = true,
            dailyQuestionPeriodKey = periodKey,
        ) { payloads ->
            if (!isAdded) {
                releaseLaunchTouchBlocker()
                return@incrementBadgeProgressAndDetectLevelUp
            }
            val badgeQueue = payloads.map { BadgeProgressFirestore.payloadToQueueItem(it) }
            openAbacusContainerFragment(
                DailyQuestionRewardFragment.newInstance(badgeQueue, periodKey),
            )
        }
    }

    private fun loadPart1Sources(onResult: (List<DailyQuestionSource>) -> Unit) {
        GlobalLessonData.loadLessonItemsForPart(requireContext(), partId = 1) { items ->
            if (!isAdded) return@loadLessonItemsForPart
            onResult(DailyQuestionPoolBuilder.buildPart1Sources(items))
        }
    }

    private fun showDailyQuestionPoolEmptyMessage() {
        Toast.makeText(
            requireContext(),
            R.string.daily_question_pool_empty,
            Toast.LENGTH_SHORT,
        ).show()
        releaseLaunchTouchBlocker()
    }

    private fun DailyQuestionChallenge.toCardUiState(poolAvailable: Boolean): DailyQuestionCardUiState {
        return DailyQuestionCardUiState(
            periodKey = periodKey,
            solvedCount = solvedCount,
            totalCount = DailyQuestionPeriod.QUESTIONS_PER_PERIOD,
            titleUnit = cardTitleUnit(),
            difficulty = cardDifficultyLabel(),
            isComplete = isComplete,
            rewardClaimed = rewardClaimed,
            poolAvailable = poolAvailable,
            pendingContinueSlotIndex = pendingContinueSlotIndex,
            isLoaded = true,
        )
    }

    private fun defaultDailyCardState(poolAvailable: Boolean = false): DailyQuestionCardUiState {
        return DailyQuestionCardUiState(
            periodKey = DailyQuestionPeriod.currentPeriodKey(),
            solvedCount = 0,
            totalCount = DailyQuestionPeriod.QUESTIONS_PER_PERIOD,
            titleUnit = "",
            difficulty = "",
            isComplete = false,
            rewardClaimed = false,
            poolAvailable = poolAvailable,
            pendingContinueSlotIndex = null,
            isLoaded = false,
        )
    }

    private fun openAbacusContainerFragment(targetFragment: Fragment) {
        val main = activity as? MainActivity
        if (main != null) {
            main.showAbacusOverlayFragment(targetFragment) {
                hide(this@TasksFragment)
                if (targetFragment !is BlindingLessonFragment) {
                    releaseLaunchTouchBlocker()
                }
            }
        } else {
            requireActivity().findViewById<View>(R.id.abacusFragmentContainer).visibility = View.VISIBLE
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right,
                )
                .replace(R.id.abacusFragmentContainer, targetFragment)
                .hide(this@TasksFragment)
                .addToBackStack(null)
                .commitAllowingStateLoss()
            if (targetFragment !is BlindingLessonFragment) {
                releaseLaunchTouchBlocker()
            }
        }
    }

    private fun releaseLaunchTouchBlocker() {
        val content = activity?.findViewById<ViewGroup>(android.R.id.content) ?: return
        content.findViewWithTag<View>(PRACTICE_TOUCH_BLOCKER_TAG)?.let { blocker ->
            content.removeView(blocker)
        }
    }

    private fun addLaunchTouchBlocker() {
        val content = activity?.findViewById<ViewGroup>(android.R.id.content) ?: return
        content.findViewWithTag<View>(PRACTICE_TOUCH_BLOCKER_TAG)?.let { content.removeView(it) }
        val blocker = View(requireContext()).apply {
            tag = PRACTICE_TOUCH_BLOCKER_TAG
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            isFocusable = true
            setOnTouchListener { _, _ -> true }
            elevation = 1000f
        }
        content.addView(blocker)
    }
}

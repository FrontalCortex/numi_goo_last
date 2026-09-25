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
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.example.app.model.LessonItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app.databinding.FragmentTasksBinding
import kotlin.math.abs
import kotlin.math.roundToInt

class TasksFragment : Fragment() {
    private fun startEnergyUpdateTimer(contentView: View, energyManager: EnergyManager?) {
        if (energyManager == null) return
        val energyText = contentView.findViewById<TextView>(R.id.panelCupPathEnergyText) ?: return
        val infiniteBadge = contentView.findViewById<View>(R.id.panelCupPathEnergyInfiniteBadge)
        val energyIcon = contentView.findViewById<ImageView>(R.id.panelCupPathEnergyIcon)
        val handler = Handler(Looper.getMainLooper())

        // Metin degismeden de plan degisebiliyor (ör. onayli ogretmen Pro olursa ikisinde de
        // metin bos kalir), o yuzden premium durumu ayrica takip edilir.
        var lastPremium: Boolean? = null
        var updateRunnable: Runnable? = null
        updateRunnable = Runnable {
            val activity = activity as? MainActivity
            if (activity != null) {
                val isInfinite = activity.isInfiniteEnergy()
                val isPremium = PlanStatus.isProPlan(energyManager.getUserPlan())
                val currentText = if (isInfinite) "" else energyManager.getCurrentEnergy().toString()
                if (energyText.text.toString() != currentText || lastPremium != isPremium) {
                    lastPremium = isPremium
                    EnergyDisplay.apply(
                        text = energyText,
                        infiniteBadge = infiniteBadge,
                        icon = energyIcon,
                        isInfinite = isInfinite,
                        isPremium = isPremium,
                        value = currentText,
                    )
                }
            }
            handler.postDelayed(updateRunnable!!, 1000)
        }

        contentView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                handler.post(updateRunnable!!)
            }
            override fun onViewDetachedFromWindow(v: View) {
                handler.removeCallbacks(updateRunnable!!)
            }
        })

        // Initial set
        val activity = activity as? MainActivity
        if (activity != null) {
            val isInfinite = activity.isInfiniteEnergy()
            val isPremium = PlanStatus.isProPlan(energyManager.getUserPlan())
            lastPremium = isPremium
            EnergyDisplay.apply(
                text = energyText,
                infiniteBadge = infiniteBadge,
                icon = energyIcon,
                isInfinite = isInfinite,
                isPremium = isPremium,
                value = energyManager.getCurrentEnergy().toString(),
            )
        }
    }

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private lateinit var bulletinAdapter: BulletinAdapter
    private var part1Sources: List<DailyQuestionSource> = emptyList()
    private var dailyCardState: DailyQuestionCardUiState = defaultDailyCardState()
    private var lastDailyPeriodRolloverRefreshMs = 0L
    /** Aynı periyotta "Mücadeleyi tamamladın!" yalnızca bir kez gösterilir. */
    private var dailyQuestionCompleteToastShownForPeriod: String? = null
    private var pendingLaunchAfterBrokenHeartHeal: (() -> Unit)? = null
    /** 1 elmas ile devam: dialog kapanınca touch blocker'ı kaldırma. */
    private var dailyQuestionDiamondContinueInFlight = false
    /** Kupa modu zorluk seviyesini saklar. */
    private var lastCupDifficultyProgress: Int = 0

    companion object {
        private const val PRACTICE_TOUCH_BLOCKER_TAG = MainActivity.PRACTICE_TOUCH_BLOCKER_TAG
        private const val VIEW_TYPE_STANDARD = 0
        private const val VIEW_TYPE_DAILY_QUESTION = 1
        private const val DAILY_PROGRESS_ANIM_DURATION_MS = 2800L
        private const val CLAIM_READY_VISUAL_PERCENT = 99.5f

        /**
         * Kupa sayacı ve kupa yolu çubuğu aynı anda akar; ikisi aynı süreyi kullanıyor ki
         * sayı yerine oturduğunda çubuk da oturmuş olsun.
         */
        private const val CUP_SCORE_ANIM_MS = 1000L

        /** Sayaç oturduktan sonra rozet kutlamasına geçmeden önceki kısa es. */
        private const val CUP_CELEBRATION_GAP_MS = 350L

        /** Rozet listesi için en fazla bu kadar beklenir; sonrası "rozet yok" sayılır. */
        private const val CUP_BADGE_WAIT_MS = 5000L
        private const val CUP_BADGE_POLL_MS = 50L
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
                    val rateText = state.globalSuccessRate?.let { " - Başarı: %$it" } ?: ""
                    focusLabel.text = ctx.getString(
                        R.string.daily_question_focus_format,
                        state.difficulty + rateText,
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
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bulletinAdapter = BulletinAdapter(
            onClick = { row ->
                when (row) {
                    is BulletinRow.Standard -> {
                        when (row.id) {
                            "feedback_card" -> openAbacusContainerFragment(FeedbackFragment())
                            "cup_path" -> showCupPathPanel()
                            "chest_animation" -> openAbacusContainerFragment(
                                NewChestFragment.newInstance(
                                    source = AnalyticsLogger.CHEST_SOURCE_BULLETIN,
                                )
                            )
                            else -> openAbacusContainerFragment(AbacusPracticeFragment())
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
        val isConsumingAndBlocking = consumePendingCupDelta()
        // Kupa Yolu otomatik açılış: bayrak varsa releaseLaunchTouchBlocker çağrılmaz, reveal kendi içinde yönetir
        if (!checkAndTriggerCupPathReveal() && !isConsumingAndBlocking) {
            releaseLaunchTouchBlocker()
        }
        refreshDailyQuestionCard()
        val main = activity as? MainActivity
        main?.scheduleReconcileAbacusOverlayWhenTasksIsBase()
        main?.logTouchDiag("TasksFragment.onResume")
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val isConsumingAndBlocking = consumePendingCupDelta()
            // Kupa Yolu otomatik açılış: bayrak varsa releaseLaunchTouchBlocker çağrılmaz, reveal kendi içinde yönetir
            if (!checkAndTriggerCupPathReveal() && !isConsumingAndBlocking) {
                releaseLaunchTouchBlocker()
            }
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
                    iconRes = R.drawable.abacus_svg_ic,
                ),
                BulletinRow.DailyQuestion(dailyCardState),
                BulletinRow.Standard(
                    id = "cup_path",
                    title = "Kupa Yolu",
                    subtitle = "Başarılarını kupalarla ölç, seviyeni yükselt ve yeni zorluklara ilerle.",
                    iconRes = R.drawable.infinity_cup_ic,
                    colorRes = R.color.lesson_header_yellow
                ),
                BulletinRow.Standard(
                    id = "feedback_card",
                    title = "Bize Ulaşın",
                    subtitle = "Bir sorun mu yaşadınız? Görüşlerinizi ve önerilerinizi bizimle paylaşın.",
                    iconRes = R.drawable.feedback_ic,
                    colorRes = android.R.color.holo_blue_dark
                ),
                BulletinRow.Standard(
                    id = "chest_animation",
                    title = "Sandık Animasyonu",
                    subtitle = "Yeni sandık açılış animasyonu yapısı.",
                    iconRes = R.drawable.gold_ic,
                    colorRes = android.R.color.holo_orange_dark
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
        loadDailyQuestionSources { sources ->
            if (!isAdded) return@loadDailyQuestionSources
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
        loadDailyQuestionSources { sources ->
            if (!isAdded) return@loadDailyQuestionSources
            if (sources.isEmpty()) {
                showDailyQuestionPoolEmptyMessage()
                return@loadDailyQuestionSources
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
        // Günün sorusunun başlatıldığı tek yer burası; kart tıklaması buraya kadar birçok
        // kapıdan (ödül alınmış, gün tamamlanmış, elmasla devam bekliyor) eleniyor.
        AnalyticsLogger.logDailyQuestionStart(questionNo = slotIndex + 1)
        val operationsList = if (slot.mathOperation != null) {
            listOf(slot.mathOperation)
        } else {
            listOf(slot.sequence)
        }
        openAbacusContainerFragment(
            BlindingLessonFragment.newDailyQuestionInstance(
                operations = operationsList,
                periodKey = challenge.periodKey,
                slotIndex = slotIndex,
                displayIntervalMs = slot.displayIntervalMs,
                partId = slot.partId,
                // Kartın üstünde görünen ünite adı ve kaynağın liste sırası; ikisi de
                // ölçüme gidiyor, yoksa bütün günlük sorular ayırt edilemez tek kovada kalıyor.
                titleUnit = slot.titleUnit,
                itemIndex = slot.itemIndex,
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
            val spent = main?.spendKeys(
                DailyQuestionPeriod.KEY_CONTINUE_COST,
                AnalyticsLogger.ITEM_DAILY_QUESTION_CONTINUE,
            )
            if (spent != true) {
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
            
            // Ödülün gerçekten alındığı tek yer burası.
            AnalyticsLogger.logDailyQuestionClaim()
            DailyQuestionRepository.markRewardClaimed(requireContext(), periodKey) { _ -> }
            
            parentFragmentManager.setFragmentResultListener("chest_closed", viewLifecycleOwner) { _, _ ->
                releaseLaunchTouchBlocker()
                parentFragmentManager.clearFragmentResultListener("chest_closed")
                if (badgeQueue.isNotEmpty() && isAdded) {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right,
                        )
                        .replace(
                            R.id.badgeFragmentContainter,
                            BadgeFragment.newLevelUpSequenceInstance(ArrayList(badgeQueue), 0),
                        )
                        .commit()
                }
            }
            
            // Sunucu isteğini fragment eklenmeden önce başlat — ilk açılıştaki gecikmeyi gizler.
            ServerRewards.prefetchChest(NewChestFragment.ChestRarity.RARE.name)
            openAbacusContainerFragment(
                NewChestFragment.newInstance(
                    NewChestFragment.ChestRarity.RARE,
                    source = AnalyticsLogger.CHEST_SOURCE_DAILY_QUESTION,
                )
            )
        }
    }

    private fun loadDailyQuestionSources(onResult: (List<DailyQuestionSource>) -> Unit) {
        GlobalLessonData.loadLessonItemsForPart(requireContext(), partId = 1) { part1Items ->
            if (!isAdded) return@loadLessonItemsForPart
            val part1Sources = DailyQuestionPoolBuilder.buildSourcesForPart(part1Items, 1)
            GlobalLessonData.loadLessonItemsForPart(requireContext(), partId = 2) { part2Items ->
                if (!isAdded) return@loadLessonItemsForPart
                val part2Sources = DailyQuestionPoolBuilder.buildSourcesForPart(part2Items, 2)
                GlobalLessonData.loadLessonItemsForPart(requireContext(), partId = 3) { part3Items ->
                    if (!isAdded) return@loadLessonItemsForPart
                    val part3Sources = DailyQuestionPoolBuilder.buildSourcesForPart(part3Items, 3)
                    val allSources = part1Sources + part2Sources + part3Sources
                    onResult(allSources)
                }
            }
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
                releaseLaunchTouchBlocker()
            }
        } else {
            requireActivity().findViewById<View>(R.id.abacusFragmentContainer).visibility = View.VISIBLE
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.queue_screen_in,
                    R.anim.queue_screen_out,
                    R.anim.queue_screen_in,
                    R.anim.queue_screen_out,
                )
                    .replace(R.id.abacusFragmentContainer, targetFragment)
                .hide(this@TasksFragment)
                .addToBackStack(null)
                .commitAllowingStateLoss()
            releaseLaunchTouchBlocker()
        }
    }

    private fun showCupPathPanel() {
        if (!isAdded) return
        
        // Paneli varsayılan kapalı (inaktif) durumlarla anında aç
        displayCupPathDialog(
            active1 = false, active2 = false, active3 = false, 
            active4 = false, active5 = false, active6 = false,
            isLoading = true
        )
        
        val context = requireContext()

        // Arka planda Firestore verilerini çek ve gelince paneli güncelle
        GlobalLessonData.loadLessonItemsForPart(context, 1) { items1 ->
            val active1 = items1.lastOrNull { it.type == com.example.app.model.LessonItem.TYPE_CHEST }?.stepIsFinish == true
            GlobalLessonData.loadLessonItemsForPart(context, 2) { items2 ->
                val active2 = items2.lastOrNull { it.type == com.example.app.model.LessonItem.TYPE_CHEST }?.stepIsFinish == true
                GlobalLessonData.loadLessonItemsForPart(context, 3) { items3 ->
                    val active3 = items3.lastOrNull { it.type == com.example.app.model.LessonItem.TYPE_CHEST }?.stepIsFinish == true
                    GlobalLessonData.loadLessonItemsForPart(context, 4) { items4 ->
                        val active4 = items4.lastOrNull { it.type == com.example.app.model.LessonItem.TYPE_CHEST }?.stepIsFinish == true
                        GlobalLessonData.loadLessonItemsForPart(context, 5) { items5 ->
                            val active5 = items5.lastOrNull { it.type == com.example.app.model.LessonItem.TYPE_CHEST }?.stepIsFinish == true
                            GlobalLessonData.loadLessonItemsForPart(context, 6) { items6 ->
                                val active6 = items6.lastOrNull { it.type == com.example.app.model.LessonItem.TYPE_CHEST }?.stepIsFinish == true
                                
                                if (isAdded) {
                                    requireActivity().runOnUiThread {
                                        val dialog = GlobalValues.cupPathDialogRef?.get()
                                        if (dialog != null && dialog.isShowing) {
                                            val bottomSheetId = dialog.context.resources.getIdentifier("design_bottom_sheet", "id", dialog.context.packageName)
                                            val bottomSheet = dialog.findViewById<View>(bottomSheetId)
                                            if (bottomSheet != null) {
                                                bottomSheet.findViewById<TextView>(R.id.card1CupValue)?.visibility = View.VISIBLE
                                                bottomSheet.findViewById<TextView>(R.id.card2CupValue)?.visibility = View.VISIBLE
                                                bottomSheet.findViewById<TextView>(R.id.card3CupValue)?.visibility = View.VISIBLE
                                                bottomSheet.findViewById<TextView>(R.id.card4CupValue)?.visibility = View.VISIBLE
                                                bottomSheet.findViewById<TextView>(R.id.card5CupValue)?.visibility = View.VISIBLE
                                                bottomSheet.findViewById<TextView>(R.id.card6CupValue)?.visibility = View.VISIBLE

                                                setupCupPathCard(bottomSheet, R.id.card1View, R.id.card1Title, R.id.card1CupIcon, R.id.card1CupValue, R.id.card1DinoAnim, active1)
                                                setupCupPathCard(bottomSheet, R.id.card2View, R.id.card2Title, R.id.card2CupIcon, R.id.card2CupValue, R.id.card2DinoAnim, active2)
                                                setupCupPathCard(bottomSheet, R.id.card3View, R.id.card3Title, R.id.card3CupIcon, R.id.card3CupValue, R.id.card3DinoAnim, active3)
                                                setupCupPathCard(bottomSheet, R.id.card4View, R.id.card4Title, R.id.card4CupIcon, R.id.card4CupValue, R.id.card4DinoAnim, active4)
                                                setupCupPathCard(bottomSheet, R.id.card5View, R.id.card5Title, R.id.card5CupIcon, R.id.card5CupValue, R.id.card5DinoAnim, active5)
                                                setupCupPathCard(bottomSheet, R.id.card6View, R.id.card6Title, R.id.card6CupIcon, R.id.card6CupValue, R.id.card6DinoAnim, active6)
                                                
                                                bottomSheet.findViewById<View>(R.id.card1View)?.let { card ->
                                                    card.isClickable = active1; card.isEnabled = active1
                                                }
                                                bottomSheet.findViewById<View>(R.id.card2View)?.let { card ->
                                                    card.isClickable = active2; card.isEnabled = active2
                                                }
                                                bottomSheet.findViewById<View>(R.id.card3View)?.let { card ->
                                                    card.isClickable = active3; card.isEnabled = active3
                                                }
                                                bottomSheet.findViewById<View>(R.id.card4View)?.let { card ->
                                                    card.isClickable = active4; card.isEnabled = active4
                                                }
                                                bottomSheet.findViewById<View>(R.id.card5View)?.let { card ->
                                                    card.isClickable = active5; card.isEnabled = active5
                                                }
                                                bottomSheet.findViewById<View>(R.id.card6View)?.let { card ->
                                                    card.isClickable = active6; card.isEnabled = active6
                                                }

                                                // Kartlar açıldı: çubuk ve sandık da aynı
                                                // anda açılsın. Veri geldiyse hemen çizilir,
                                                // gelmediyse cevabı gelince.
                                                revealCupPathRewards(bottomSheet, dialog)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun displayCupPathDialog(
        active1: Boolean, active2: Boolean, active3: Boolean, 
        active4: Boolean, active5: Boolean, active6: Boolean,
        isLoading: Boolean = false
    ) {
        if (!isAdded) return
        val dialog = BottomSheetDialog(requireContext())
        val contentView = LayoutInflater.from(requireContext())
            .inflate(R.layout.panel_cup_path, null)
        dialog.setContentView(contentView)
        // Ödül satırı ağdan geliyor; veri gelene kadar kartlarla aynı gri.
        applyCupPathRewardLoadingState(contentView)
        // Kartlar yüklenirken açılıyorsa ödül satırı da onları bekliyor; bkz.
        // [cupPathCardsRevealed].
        cupPathCardsRevealed = !isLoading

        val energyManager = (requireActivity() as? MainActivity)?.getEnergyManager()
        startEnergyUpdateTimer(contentView, energyManager)
        
        contentView.findViewById<android.view.View>(R.id.panelCupPathEnergyContainer)?.setOnClickListener {
            dialog.dismiss()
            GlobalValues.cupPathDialogRef?.get()?.dismiss()
            (requireActivity() as? MainActivity)?.openShopFragment()
        }

        // Expand the sheet fully on show
        dialog.setOnShowListener {
            val bottomSheetId = dialog.context.resources.getIdentifier("design_bottom_sheet", "id", dialog.context.packageName)
            val bottomSheet = dialog.findViewById<View>(bottomSheetId)
            bottomSheet?.let {
                it.setBackgroundResource(android.R.color.transparent)
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        if (isLoading) {
            contentView.findViewById<TextView>(R.id.card1CupValue)?.visibility = View.INVISIBLE
            contentView.findViewById<TextView>(R.id.card2CupValue)?.visibility = View.INVISIBLE
            contentView.findViewById<TextView>(R.id.card3CupValue)?.visibility = View.INVISIBLE
            contentView.findViewById<TextView>(R.id.card4CupValue)?.visibility = View.INVISIBLE
            contentView.findViewById<TextView>(R.id.card5CupValue)?.visibility = View.INVISIBLE
            contentView.findViewById<TextView>(R.id.card6CupValue)?.visibility = View.INVISIBLE
        }

        // Apply visual states to each card
        setupCupPathCard(contentView, R.id.card1View, R.id.card1Title, R.id.card1CupIcon, R.id.card1CupValue, R.id.card1DinoAnim, active1)
        setupCupPathCard(contentView, R.id.card2View, R.id.card2Title, R.id.card2CupIcon, R.id.card2CupValue, R.id.card2DinoAnim, active2)
        setupCupPathCard(contentView, R.id.card3View, R.id.card3Title, R.id.card3CupIcon, R.id.card3CupValue, R.id.card3DinoAnim, active3)
        setupCupPathCard(contentView, R.id.card4View, R.id.card4Title, R.id.card4CupIcon, R.id.card4CupValue, R.id.card4DinoAnim, active4)
        setupCupPathCard(contentView, R.id.card5View, R.id.card5Title, R.id.card5CupIcon, R.id.card5CupValue, R.id.card5DinoAnim, active5)
        setupCupPathCard(contentView, R.id.card6View, R.id.card6Title, R.id.card6CupIcon, R.id.card6CupValue, R.id.card6DinoAnim, active6)

        bindCupPathRewards(contentView, dialog)

        // Cancel lottie animations on dismiss
        dialog.setOnDismissListener {
            listOf(R.id.card1DinoAnim, R.id.card2DinoAnim, R.id.card3DinoAnim, R.id.card4DinoAnim, R.id.card5DinoAnim, R.id.card6DinoAnim).forEach { id ->
                contentView.findViewById<LottieAnimationView>(id)?.cancelAnimation()
            }
        }

        val card1View = contentView.findViewById<View>(R.id.card1View)
        var lastCardClickTime = 0L
        card1View?.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastCardClickTime < 500) return@setOnClickListener
            lastCardClickTime = now
            showCupDifficultyPanel(
                animFileName = "dinosaur_anim.json",
                isBlindingMode = false,
                cupScoreProvider = AbacusCupRepository::fetchCupScore,
                pendingDeltaSetter = { GlobalValues.pendingCupDelta = it },
                cardCupValueId = R.id.card1CupValue
            )
        }

        // card2View: extraction kupa modu (çıkarmalı toplama)
        contentView.findViewById<View>(R.id.card2View)?.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastCardClickTime < 500) return@setOnClickListener
            lastCardClickTime = now
            showCupDifficultyPanel(
                animFileName = "crocodile_anim.json",
                isBlindingMode = false,
                isExtractionMode = true,
                cupScoreProvider = ExtractionCupRepository::fetchCupScore,
                pendingDeltaSetter = { GlobalValues.pendingExtractionCupDelta = it },
                cardCupValueId = R.id.card2CupValue
            )
        }

        // card4View: blinding kupa modu (numberInput ile cevap)
        contentView.findViewById<View>(R.id.card4View)?.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastCardClickTime < 500) return@setOnClickListener
            lastCardClickTime = now
            showCupDifficultyPanel(
                animFileName = "eagle_anim.json",
                isBlindingMode = true,
                cupScoreProvider = BlindingAdditionCupRepository::fetchCupScore,
                pendingDeltaSetter = { GlobalValues.pendingBlindingCupDelta = it },
                cardCupValueId = R.id.card4CupValue
            )
        }

        // card3View: çarpma kupa modu (abaküs ile çarpma)
        contentView.findViewById<View>(R.id.card3View)?.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastCardClickTime < 500) return@setOnClickListener
            lastCardClickTime = now
            showCupDifficultyPanel(
                animFileName = "goat_anim.json",
                isBlindingMode = false,
                isMultiplicationMode = true,
                cupScoreProvider = ImpactCupRepository::fetchCupScore,
                pendingDeltaSetter = { GlobalValues.pendingImpactCupDelta = it },
                cardCupValueId = R.id.card3CupValue
            )
        }

        // card5View: blinding extraction kupa modu (körleme + çıkarmalı)
        contentView.findViewById<View>(R.id.card5View)?.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastCardClickTime < 500) return@setOnClickListener
            lastCardClickTime = now
            showCupDifficultyPanel(
                animFileName = "fly_anim.json",
                isBlindingMode = true,
                isExtractionMode = true,
                cupScoreProvider = BlindingExtractionCupRepository::fetchCupScore,
                pendingDeltaSetter = { GlobalValues.pendingBlindingExtractionCupDelta = it },
                cardCupValueId = R.id.card5CupValue
            )
        }

        // card6View: blinding multiplication kupa modu (körleme çarpma)
        contentView.findViewById<View>(R.id.card6View)?.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastCardClickTime < 500) return@setOnClickListener
            lastCardClickTime = now
            showCupDifficultyPanel(
                animFileName = "turtle_anim.json",
                isBlindingMode = true,
                isMultiplicationMode = true,
                cupScoreProvider = BlindingImpactCupRepository::fetchCupScore,
                pendingDeltaSetter = { GlobalValues.pendingBlindingImpactCupDelta = it },
                cardCupValueId = R.id.card6CupValue
            )
        }

        // Aktifliğe göre veya yüklenme durumuna göre tıklanabilirliği kapa
        if (!active1 || isLoading) { card1View?.isClickable = false; card1View?.isEnabled = false }
        if (!active2 || isLoading) { contentView.findViewById<View>(R.id.card2View)?.apply { isClickable = false; isEnabled = false } }
        if (!active3 || isLoading) { contentView.findViewById<View>(R.id.card3View)?.apply { isClickable = false; isEnabled = false } }
        if (!active4 || isLoading) { contentView.findViewById<View>(R.id.card4View)?.apply { isClickable = false; isEnabled = false } }
        if (!active5 || isLoading) { contentView.findViewById<View>(R.id.card5View)?.apply { isClickable = false; isEnabled = false } }
        if (!active6 || isLoading) { contentView.findViewById<View>(R.id.card6View)?.apply { isClickable = false; isEnabled = false } }

        // Dialog referansını sakla (kupa güncellemesi için)
        GlobalValues.cupPathDialogRef = java.lang.ref.WeakReference(dialog as android.app.Dialog)
        dialog.setOnDismissListener {
            listOf(R.id.card1DinoAnim, R.id.card2DinoAnim, R.id.card3DinoAnim, R.id.card4DinoAnim, R.id.card5DinoAnim, R.id.card6DinoAnim).forEach { id ->
                contentView.findViewById<LottieAnimationView>(id)?.cancelAnimation()
            }
            if (GlobalValues.cupPathDialogRef?.get() === (dialog as? android.app.Dialog)) {
                GlobalValues.cupPathDialogRef = null
            }
            releaseLaunchTouchBlocker()
        }

        // card1CupValue'yi Firestore'dan çek
        val card1CupValue = contentView.findViewById<TextView>(R.id.card1CupValue)
        AbacusCupRepository.fetchCupScore { score ->
            if (!isAdded) return@fetchCupScore
            requireActivity().runOnUiThread {
                card1CupValue?.text = score.coerceAtLeast(0).toString()
            }
        }

        // card4CupValue'yi Firestore'dan çek (Körleme)
        val card4CupValue = contentView.findViewById<TextView>(R.id.card4CupValue)
        BlindingAdditionCupRepository.fetchCupScore { score ->
            if (!isAdded) return@fetchCupScore
            requireActivity().runOnUiThread {
                card4CupValue?.text = score.coerceAtLeast(0).toString()
            }
        }

        // card2CupValue'yi Firestore'dan çek (Çıkarmalı Toplama)
        val card2CupValue = contentView.findViewById<TextView>(R.id.card2CupValue)
        ExtractionCupRepository.fetchCupScore { score ->
            if (!isAdded) return@fetchCupScore
            requireActivity().runOnUiThread {
                card2CupValue?.text = score.coerceAtLeast(0).toString()
            }
        }

        // card5CupValue'yi Firestore'dan çek (Körleme Çıkarmalı)
        val card5CupValue = contentView.findViewById<TextView>(R.id.card5CupValue)
        BlindingExtractionCupRepository.fetchCupScore { score ->
            if (!isAdded) return@fetchCupScore
            requireActivity().runOnUiThread {
                card5CupValue?.text = score.coerceAtLeast(0).toString()
            }
        }

        // card3CupValue'yi Firestore'dan çek (Çarpma)
        val card3CupValue = contentView.findViewById<TextView>(R.id.card3CupValue)
        ImpactCupRepository.fetchCupScore { score ->
            if (!isAdded) return@fetchCupScore
            requireActivity().runOnUiThread {
                card3CupValue?.text = score.coerceAtLeast(0).toString()
            }
        }

        // card6CupValue'yi Firestore'dan çek (Körleme Çarpma)
        val card6CupValue = contentView.findViewById<TextView>(R.id.card6CupValue)
        BlindingImpactCupRepository.fetchCupScore { score ->
            if (!isAdded) return@fetchCupScore
            requireActivity().runOnUiThread {
                card6CupValue?.text = score.coerceAtLeast(0).toString()
            }
        }

        dialog.show()
    }

    private fun showCupDifficultyPanel(
        animFileName: String = "dinosaur_anim.json",
        isBlindingMode: Boolean = false,
        isExtractionMode: Boolean = false,
        isMultiplicationMode: Boolean = false,
        cupScoreProvider: (onResult: (Int) -> Unit) -> Unit = AbacusCupRepository::fetchCupScore,
        pendingDeltaSetter: (Int) -> Unit = { GlobalValues.pendingCupDelta = it },
        cardCupValueId: Int? = null
    ) {
        if (!isAdded) return
        val contentView = LayoutInflater.from(requireContext())
            .inflate(R.layout.panel_cup_difficulty, null)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(contentView)
            .create()

        dialog.window?.let { window ->
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.attributes.windowAnimations = R.style.DialogAnimationSlideLeft
        }

        // Paneldeki animasyonu tıklanan karta göre ayarla
        contentView.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.cupDifficultyAnim)?.apply {
            setAnimation(animFileName)
            playAnimation()
        }

        // Çarpma modunda seekBar, thumbLabel, message gizle ve başlığı değiştir
        if (isMultiplicationMode) {
            contentView.findViewById<TextView>(R.id.cupDifficultyTitle)?.text = "Çarpma Kupa Yolu"
            contentView.findViewById<View>(R.id.cupDifficultyMessage)?.visibility = View.GONE
        }

        val closeBtn = contentView.findViewById<View>(R.id.cupDifficultyClose)
        closeBtn?.setOnClickListener { dialog.dismiss() }

        // SeekBar: 5 durak (0,25,50,75,100) → max=4
        val seekBar    = contentView.findViewById<android.widget.SeekBar>(R.id.cupDifficultySeekBar)
        val thumbLabel = contentView.findViewById<TextView>(R.id.cupDifficultyThumbLabel)
        val startBtn = contentView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cupDifficultyStartButton)
        val digitContainer = contentView.findViewById<android.view.View>(R.id.cupDigitContainer)
        val digitSpinner = contentView.findViewById<android.widget.Spinner>(R.id.cupDigitSpinner)
        var selectedDigitSize: Int? = null
        
        startBtn?.isEnabled = false

        /** Thumb etiketini seekbar üzerinde thumb'ın ortasına hizalar. */
        fun updateThumbLabel(progress: Int) {
            val percentage = progress * 25
            thumbLabel?.text = "$percentage"
            thumbLabel?.post {
                val seekBarWidth  = seekBar?.width ?: return@post
                val thumbOffset   = seekBar?.thumbOffset ?: 0
                val trackWidth    = seekBarWidth - 2 * thumbOffset
                val thumbCenterX  = thumbOffset + trackWidth * progress / 4f
                val labelHalfW    = (thumbLabel?.width ?: 0) / 2f
                thumbLabel?.translationX = thumbCenterX - labelHalfW
            }
        }

        seekBar?.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    lastCupDifficultyProgress = progress
                }
                updateThumbLabel(progress)
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar) {}
        })

        // Çarpma modunda seekBar container'ını gizle
        if (isMultiplicationMode) {
            seekBar?.visibility = View.GONE
            thumbLabel?.visibility = View.GONE
        } else {
            // Önceki seviyeyi geri yükle
            seekBar?.progress = lastCupDifficultyProgress
            updateThumbLabel(lastCupDifficultyProgress)
        }

        // Başlat butonunu kupa skoru okunduktan sonra aktif et
        cupScoreProvider { cupScore ->
            if (!isAdded) return@cupScoreProvider
            requireActivity().runOnUiThread {
                startBtn?.isEnabled = true
                
                if (!isMultiplicationMode && cupScore >= 3000) {
                    val availableDigits = CupRuleEngine.getAvailableDigits(cupScore, isBlindingMode)
                    if (availableDigits.isNotEmpty() && digitSpinner != null && digitContainer != null) {
                        digitContainer.visibility = android.view.View.VISIBLE
                        val options = mutableListOf("Rastgele")
                        options.addAll(availableDigits.map { "$it Basamak" })
                        
                        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        digitSpinner.adapter = adapter
                        
                        digitSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                                selectedDigitSize = if (position == 0) null else availableDigits[position - 1]
                            }
                            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                                selectedDigitSize = null
                            }
                        }
                    }
                }
                
                startBtn?.setOnClickListener {
                    val isInfinite = (requireActivity() as? MainActivity)?.isInfiniteEnergy() == true
                    val energyManager = (requireActivity() as? MainActivity)?.getEnergyManager()
                    if (!isInfinite && (energyManager?.getCurrentEnergy() ?: 0) <= 0) {
                        AnalyticsLogger.logEnergyBlocked(
                            blockSource = AnalyticsLogger.ENERGY_BLOCK_CUP,
                            waitSeconds = (energyManager?.getTimeUntilNextEnergy() ?: 0L) / 1000L,
                            lessonsThisSession = EnergySessionCounter.bucket(),
                            partId = null,
                            lessonId = null,
                        )
                        dialog.dismiss()
                        GlobalValues.cupPathDialogRef?.get()?.dismiss()
                        (requireActivity() as? MainActivity)?.openShopFragment()
                        return@setOnClickListener
                    }
                    
                    addLaunchTouchBlocker()
                    GlobalValues.cupPathDialogRef?.get()?.window?.setFlags(
                        android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    )
                    val difficultyLevel = seekBar?.progress ?: 0
                    dialog.dismiss()
                    GlobalValues.cupPathDialogRef?.get()?.hide()
                    GlobalValues.currentLessonOldCupScore = cupScore
                    launchCupModeLesson(
                        cupScore = cupScore,
                        difficultyLevel = if (isMultiplicationMode) 0 else difficultyLevel,
                        isBlindingMode = isBlindingMode,
                        isExtractionMode = isExtractionMode,
                        isMultiplicationMode = isMultiplicationMode,
                        pendingDeltaSetter = pendingDeltaSetter,
                        cardCupValueId = cardCupValueId,
                        forcedDigitSize = selectedDigitSize
                    )
                }
            }
        }

        dialog.show()
    }

    /** Kupa skoruna ve zorluk seviyesine göre çalışma-zamanı LessonItem oluşturur ve BlindingLessonFragment'i açar. */
    private fun launchCupModeLesson(
        cupScore: Int,
        difficultyLevel: Int = 0,
        isBlindingMode: Boolean = false,
        isExtractionMode: Boolean = false,
        isMultiplicationMode: Boolean = false,
        pendingDeltaSetter: (Int) -> Unit = { GlobalValues.pendingCupDelta = it },
        cardCupValueId: Int? = null,
        forcedDigitSize: Int? = null
    ) {
        if (!isAdded) return

        // Kupa yarışı da bir derstir. Buradan sayılmazsa yalnız kupa oynayan çocuk için sayaç
        // hep 0'da kalır ve duvara çarptığında "hiç ders yapmadan engellendi" gibi görünür —
        // ölçümde en alarm verici satır tam olarak budur, yani rapor yanlış tarafa bakar.
        // Harita dersinden farklı olarak can yarışın başında değil sonunda (bırakma/yanlış)
        // harcanır; bu yüzden artış burada, o olaylardan önce olur.
        EnergySessionCounter.onLessonStarted()

        val activity = requireActivity()
        val lessonItem = CupRuleEngine.buildLessonItem(
            cupScore = cupScore, 
            difficultyLevel = difficultyLevel, 
            isBlinding = isBlindingMode, 
            isExtraction = isExtractionMode,
            forcedDigitSize = forcedDigitSize
        )
        if (isMultiplicationMode) {
            lessonItem.isMultiplication = true
            lessonItem.cupNumberCount = 1
            lessonItem.cupWinDelta = 30
            lessonItem.cupLossDelta = 20
        }

        // Kupa modu için part 9'u initialize et
        GlobalLessonData.initialize(requireContext(), 9) {
            activity.runOnUiThread {
                val fm = activity.supportFragmentManager
                val fragmentContainer = activity.findViewById<View>(R.id.abacusFragmentContainer)
                    ?: return@runOnUiThread
                fragmentContainer.visibility = View.VISIBLE
                fm.executePendingTransactions()

                // Fragment yerleştirildi, engellemeyi kaldır
                releaseLaunchTouchBlocker()

                val operations: ArrayList<Any> = if (isMultiplicationMode) {
                    val mathOp = CupRuleEngine.generateMultiplicationQuestion(cupScore, isBlindingMode)
                    ArrayList(listOf(mathOp))
                } else {
                    // Sayı listesini üret ve bundle'a koy
                    val digitSize = lessonItem.cupDigitSize ?: 1
                    val count = lessonItem.cupNumberCount ?: 3
                    val numbers = GlobalValues.randomUniqueNumberStrings(digitSize, count)
                        .map { it.toInt() }
                        .let { if (isExtractionMode) applyExtractionNegation(it) else it }
                    ArrayList(listOf(numbers))
                }

                val fragment = BlindingLessonFragment().apply {
                    arguments = android.os.Bundle().apply {
                        putSerializable("operations", operations)
                        putSerializable("cup_lesson_item", lessonItem)
                    }
                }

                // Fragment kapandığında TasksFragment'in haberi olması için result listener ekliyoruz
                fm.setFragmentResultListener("cupModeResult", viewLifecycleOwner) { _, _ ->
                    activity.findViewById<View>(R.id.abacusFragmentContainer)?.visibility = View.GONE
                    
                    (activity as? MainActivity)?.checkAndShowInterstitialAdIfAllowed("cupModeResult")
                    val consumed = consumePendingCupDelta(pendingDeltaSetter = pendingDeltaSetter, cardCupValueId = cardCupValueId)
                    if (!consumed) {
                        // Delta yoktu (örn. quit veya ders başlamadan çıkış) — paneli yine de yeniden aç
                        loadAndShowCupPathDialogAfterCupUpdate()
                    }
                }

                fm.beginTransaction()
                    .setCustomAnimations(R.anim.queue_screen_in, R.anim.queue_screen_out)
                    .replace(R.id.abacusFragmentContainer, fragment)
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            }
        }
    }

    /**
     * Çıkarmalı toplama modu için sayı listesine negatif dönüşümü uygular.
     * İlk sayı her zaman pozitiftir. Sonraki her sayı için:
     * running_total > number ise → sayıyı negatif yapar (çıkarır)
     * Bu kural sayesinde toplam asla negatife düşmez.
     */
    private fun applyExtractionNegation(numbers: List<Int>): List<Int> {
        val result = mutableListOf<Int>()
        var runningTotal = 0
        for (n in numbers) {
            if (result.isNotEmpty() && runningTotal > n) {
                result.add(-n)
                runningTotal -= n
            } else {
                result.add(n)
                runningTotal += n
            }
        }
        return result
    }

    /**
     * BlindingLessonFragment'ten döndükten sonra delta bayrağını tüketir.
     * Delta varsa ilgili repository'e yazar ve paneldeki cardCupValue'yi günceller.
     */
    private fun consumePendingCupDelta(
        pendingDeltaSetter: (Int) -> Unit = { GlobalValues.pendingCupDelta = it },
        cardCupValueId: Int? = null
    ): Boolean {
        val delta = GlobalValues.pendingCupDelta
            ?: GlobalValues.pendingBlindingCupDelta
            ?: GlobalValues.pendingExtractionCupDelta
            ?: GlobalValues.pendingBlindingExtractionCupDelta
            ?: GlobalValues.pendingImpactCupDelta
            ?: GlobalValues.pendingBlindingImpactCupDelta
            ?: return false
        val isBlindingDelta = GlobalValues.pendingBlindingCupDelta != null
        val isExtractionDelta = GlobalValues.pendingExtractionCupDelta != null
        val isBlindingExtractionDelta = GlobalValues.pendingBlindingExtractionCupDelta != null
        val isImpactDelta = GlobalValues.pendingImpactCupDelta != null
        val isBlindingImpactDelta = GlobalValues.pendingBlindingImpactCupDelta != null
        GlobalValues.pendingCupDelta = null
        GlobalValues.pendingBlindingCupDelta = null
        GlobalValues.pendingExtractionCupDelta = null
        GlobalValues.pendingBlindingExtractionCupDelta = null
        GlobalValues.pendingImpactCupDelta = null
        GlobalValues.pendingBlindingImpactCupDelta = null

        val newScore = (GlobalValues.currentLessonOldCupScore ?: 0) + delta

        // Panel HEMEN açılıyor. Testten dönen kullanıcının beklediği ilk şey kupa yolu:
        // sayaç ve çubuk akmaya başlasın diye rozet listesi beklenmiyor.
        //
        // Eskiden önce liste beklenir, rozet varsa panel hiç açılmadan kutlamaya geçilirdi.
        // Liste yerel olarak hesaplandığı sürece bu anlıktı; artık sunucudan geldiği için
        // (bkz. BadgePrecalcHelper) beklemek panelin geç açılması demek oluyordu.
        loadAndShowCupPathDialogAfterCupUpdate(cardCupValueId, newScore, delta)
        val panelShownAtMs = android.os.SystemClock.elapsedRealtime()
        awaitCupBadgePayloads { payloads ->
            openCupBadgeCelebrationAfterAnimation(payloads, panelShownAtMs)
        }

        return true
    }

    /**
     * Rozet kademesi listesini bekler ve bir kez [onReady]'e verir.
     *
     * Liste sunucudan geliyor ([BadgePrecalcHelper]); ders biter bitmez başlatıldığı için
     * genelde hazır. Hazır değilse kısa aralıklarla yoklanıyor, [CUP_BADGE_WAIT_MS] dolunca
     * boş kabul edilip vazgeçiliyor. Vazgeçerken değer temizleniyor: geç gelen bir liste
     * sahipsiz kalıp bir sonraki derste yanlış kutlama açardı.
     */
    private fun awaitCupBadgePayloads(onReady: (List<BadgeLevelUpPayload>) -> Unit) {
        GlobalValues.pendingCupBadgePayloads?.let { ready ->
            GlobalValues.pendingCupBadgePayloads = null
            onReady(ready)
            return
        }
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val deadline = android.os.SystemClock.elapsedRealtime() + CUP_BADGE_WAIT_MS
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isAdded) return
                val ready = GlobalValues.pendingCupBadgePayloads
                if (ready != null) {
                    GlobalValues.pendingCupBadgePayloads = null
                    onReady(ready)
                    return
                }
                if (android.os.SystemClock.elapsedRealtime() >= deadline) {
                    GlobalValues.pendingCupBadgePayloads = null
                    onReady(emptyList())
                    return
                }
                handler.postDelayed(this, CUP_BADGE_POLL_MS)
            }
        }, CUP_BADGE_POLL_MS)
    }

    /**
     * Rozet kutlamasını, kupa sayacı yerine oturduktan sonra açar.
     *
     * Kutlama animasyonun ortasında açılsaydı kullanıcı kupasının eşiği geçtiğini göremeden
     * ekran değişirdi — oysa rozetin sebebi tam olarak o. Liste zaten hazır geldiyse de
     * animasyon kadar bekleniyor.
     */
    private fun openCupBadgeCelebrationAfterAnimation(
        payloads: List<BadgeLevelUpPayload>,
        panelShownAtMs: Long,
    ) {
        if (payloads.isEmpty() || !isAdded) return
        val elapsed = android.os.SystemClock.elapsedRealtime() - panelShownAtMs
        val wait = (CUP_SCORE_ANIM_MS + CUP_CELEBRATION_GAP_MS - elapsed).coerceAtLeast(0L)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            // isHidden: kullanıcı bu arada başka bir ekrana geçtiyse kutlama onun üstüne
            // açılmasın. Rozet yine kazanılmış olur, yalnızca kutlaması oynamaz.
            if (!isAdded || isHidden) return@postDelayed
            GlobalValues.cupPathDialogRef?.get()?.dismiss()
            GlobalValues.cupPathDialogRef = null
            BadgeProgressFirestore.openBadgeCelebration(
                requireActivity().supportFragmentManager,
                payloads,
            )
        }, wait)
    }


    /** Kupa güncellemesinden sonra panel_cup_path'i güncel verilerle açar. */
    private fun animateCupScore(
        textView: android.widget.TextView,
        deltaTextView: android.widget.TextView,
        oldScore: Int,
        newScore: Int,
        delta: Int
    ) {
        deltaTextView.visibility = android.view.View.VISIBLE
        deltaTextView.text = if (delta > 0) "+" + delta else delta.toString()
        deltaTextView.setTextColor(if (delta > 0) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#F44336"))
        
        val animator = android.animation.ValueAnimator.ofInt(oldScore, newScore)
        animator.duration = CUP_SCORE_ANIM_MS
        animator.addUpdateListener { anim ->
            textView.text = anim.animatedValue.toString()
        }
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                deltaTextView.visibility = android.view.View.GONE
            }
        })
        animator.start()
    }

private fun loadAndShowCupPathDialogAfterCupUpdate(updatedCardId: Int? = null, updatedScore: Int? = null, delta: Int? = null) {
        if (!isAdded) return
        val dialog = GlobalValues.cupPathDialogRef?.get()
        if (dialog != null) {
            // Eskiden olduğu gibi gizlenmiş dialogu anında aç
            dialog.show()
            // Dokunmatik kilidini kaldır (eğer derse girilirken konulmuşsa)
            dialog.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            // Arka planda verileri çekip dialogu güncelle
            updateExistingCupPathDialog(dialog, updatedCardId, updatedScore, delta)
        } else {
            // Eğer referans kayıpsa baştan yükle
            showCupPathPanel()
        }
    }

    private fun updateExistingCupPathDialog(dialog: android.app.Dialog, updatedCardId: Int? = null, updatedScore: Int? = null, delta: Int? = null) {
        val bottomSheetId = dialog.context.resources.getIdentifier("design_bottom_sheet", "id", dialog.context.packageName)
        val contentView = dialog.findViewById<View>(bottomSheetId) ?: return
        
        // Enerjiyi guncelle
        val energyManager = (requireActivity() as? MainActivity)?.getEnergyManager()
        startEnergyUpdateTimer(contentView, energyManager)
        
        contentView.findViewById<android.view.View>(R.id.panelCupPathEnergyContainer)?.setOnClickListener {
            dialog.dismiss()
            GlobalValues.cupPathDialogRef?.get()?.dismiss()
            (requireActivity() as? MainActivity)?.openShopFragment()
        }
        val context = requireContext()

        // 1. Kilit/Aktif durumlarını güncelle
        GlobalLessonData.loadLessonItemsForPart(context, 1) { items1 ->
            val active1 = items1.lastOrNull { it.type == com.example.app.model.LessonItem.TYPE_CHEST }?.stepIsFinish == true
            GlobalLessonData.loadLessonItemsForPart(context, 2) { items2 ->
                val active2 = items2.lastOrNull { it.type == com.example.app.model.LessonItem.TYPE_CHEST }?.stepIsFinish == true
                GlobalLessonData.loadLessonItemsForPart(context, 3) { items3 ->
                    val active3 = items3.lastOrNull { it.type == com.example.app.model.LessonItem.TYPE_CHEST }?.stepIsFinish == true
                    GlobalLessonData.loadLessonItemsForPart(context, 4) { items4 ->
                        val active4 = items4.lastOrNull { it.type == com.example.app.model.LessonItem.TYPE_CHEST }?.stepIsFinish == true
                        GlobalLessonData.loadLessonItemsForPart(context, 5) { items5 ->
                            val active5 = items5.lastOrNull { it.type == com.example.app.model.LessonItem.TYPE_CHEST }?.stepIsFinish == true
                            GlobalLessonData.loadLessonItemsForPart(context, 6) { items6 ->
                                val active6 = items6.lastOrNull { it.type == com.example.app.model.LessonItem.TYPE_CHEST }?.stepIsFinish == true
                                if (isAdded && dialog.isShowing) {
                                    requireActivity().runOnUiThread {
                                        setupCupPathCard(contentView, R.id.card1View, R.id.card1Title, R.id.card1CupIcon, R.id.card1CupValue, R.id.card1DinoAnim, active1)
                                        setupCupPathCard(contentView, R.id.card2View, R.id.card2Title, R.id.card2CupIcon, R.id.card2CupValue, R.id.card2DinoAnim, active2)
                                        setupCupPathCard(contentView, R.id.card3View, R.id.card3Title, R.id.card3CupIcon, R.id.card3CupValue, R.id.card3DinoAnim, active3)
                                        setupCupPathCard(contentView, R.id.card4View, R.id.card4Title, R.id.card4CupIcon, R.id.card4CupValue, R.id.card4DinoAnim, active4)
                                        setupCupPathCard(contentView, R.id.card5View, R.id.card5Title, R.id.card5CupIcon, R.id.card5CupValue, R.id.card5DinoAnim, active5)
                                        setupCupPathCard(contentView, R.id.card6View, R.id.card6Title, R.id.card6CupIcon, R.id.card6CupValue, R.id.card6DinoAnim, active6)

                                        // Ders sonrası tazeleme: hangi kart değiştiyse onun çubuğu da kupa sayısıyla
                                        // birlikte akıyor. Kart → kupa alanı eşleşmesi cupPathRewardViews'te duruyor.
                                        bindCupPathRewards(
                                            contentView,
                                            dialog,
                                            animatedCupField = cupPathRewardViews.firstOrNull { it.cupValueId == updatedCardId }?.cupField,
                                            animatedScore = updatedScore,
                                            delta = delta ?: 0,
                                        )

                                        contentView.findViewById<View>(R.id.card1View)?.apply { isClickable = active1; isEnabled = active1 }
                                        contentView.findViewById<View>(R.id.card2View)?.apply { isClickable = active2; isEnabled = active2 }
                                        contentView.findViewById<View>(R.id.card3View)?.apply { isClickable = active3; isEnabled = active3 }
                                        contentView.findViewById<View>(R.id.card4View)?.apply { isClickable = active4; isEnabled = active4 }
                                        contentView.findViewById<View>(R.id.card5View)?.apply { isClickable = active5; isEnabled = active5 }
                                        contentView.findViewById<View>(R.id.card6View)?.apply { isClickable = active6; isEnabled = active6 }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Kupa değerlerini güncelle
        val card1CupValue = contentView.findViewById<android.widget.TextView>(R.id.card1CupValue)
        if (updatedCardId == R.id.card1CupValue && updatedScore != null) {
            val tv = card1CupValue
            if (isAdded && tv != null) {
                tv.visibility = android.view.View.VISIBLE
                if (delta != null && delta != 0) {
                    tv.post {
                        val deltaTextView = contentView.findViewById<android.widget.TextView>(R.id.card1CupDeltaText)
                        val oldScore = (updatedScore - delta).coerceAtLeast(0)
                        tv.text = oldScore.toString()
                        if (deltaTextView != null) {
                            animateCupScore(tv, deltaTextView, oldScore, updatedScore.coerceAtLeast(0), delta)
                        }
                    }
                } else {
                    tv.text = updatedScore.coerceAtLeast(0).toString()
                }
            }
        } else {
            AbacusCupRepository.fetchCupScore { score ->
                if (isAdded && dialog.isShowing) requireActivity().runOnUiThread { card1CupValue?.text = score.coerceAtLeast(0).toString(); card1CupValue?.visibility = View.VISIBLE }
            }
        }
        
        val card4CupValue = contentView.findViewById<android.widget.TextView>(R.id.card4CupValue)
        if (updatedCardId == R.id.card4CupValue && updatedScore != null) {
            val tv = card4CupValue
            if (isAdded && tv != null) {
                tv.visibility = android.view.View.VISIBLE
                if (delta != null && delta != 0) {
                    tv.post {
                        val deltaTextView = contentView.findViewById<android.widget.TextView>(R.id.card4CupDeltaText)
                        val oldScore = (updatedScore - delta).coerceAtLeast(0)
                        tv.text = oldScore.toString()
                        if (deltaTextView != null) {
                            animateCupScore(tv, deltaTextView, oldScore, updatedScore.coerceAtLeast(0), delta)
                        }
                    }
                } else {
                    tv.text = updatedScore.coerceAtLeast(0).toString()
                }
            }
        } else {
            BlindingAdditionCupRepository.fetchCupScore { score ->
                if (isAdded && dialog.isShowing) requireActivity().runOnUiThread { card4CupValue?.text = score.coerceAtLeast(0).toString(); card4CupValue?.visibility = View.VISIBLE }
            }
        }
        
        val card2CupValue = contentView.findViewById<android.widget.TextView>(R.id.card2CupValue)
        if (updatedCardId == R.id.card2CupValue && updatedScore != null) {
            val tv = card2CupValue
            if (isAdded && tv != null) {
                tv.visibility = android.view.View.VISIBLE
                if (delta != null && delta != 0) {
                    tv.post {
                        val deltaTextView = contentView.findViewById<android.widget.TextView>(R.id.card2CupDeltaText)
                        val oldScore = (updatedScore - delta).coerceAtLeast(0)
                        tv.text = oldScore.toString()
                        if (deltaTextView != null) {
                            animateCupScore(tv, deltaTextView, oldScore, updatedScore.coerceAtLeast(0), delta)
                        }
                    }
                } else {
                    tv.text = updatedScore.coerceAtLeast(0).toString()
                }
            }
        } else {
            ExtractionCupRepository.fetchCupScore { score ->
                if (isAdded && dialog.isShowing) requireActivity().runOnUiThread { card2CupValue?.text = score.coerceAtLeast(0).toString(); card2CupValue?.visibility = View.VISIBLE }
            }
        }
        
        val card5CupValue = contentView.findViewById<android.widget.TextView>(R.id.card5CupValue)
        if (updatedCardId == R.id.card5CupValue && updatedScore != null) {
            val tv = card5CupValue
            if (isAdded && tv != null) {
                tv.visibility = android.view.View.VISIBLE
                if (delta != null && delta != 0) {
                    tv.post {
                        val deltaTextView = contentView.findViewById<android.widget.TextView>(R.id.card5CupDeltaText)
                        val oldScore = (updatedScore - delta).coerceAtLeast(0)
                        tv.text = oldScore.toString()
                        if (deltaTextView != null) {
                            animateCupScore(tv, deltaTextView, oldScore, updatedScore.coerceAtLeast(0), delta)
                        }
                    }
                } else {
                    tv.text = updatedScore.coerceAtLeast(0).toString()
                }
            }
        } else {
            BlindingExtractionCupRepository.fetchCupScore { score ->
                if (isAdded && dialog.isShowing) requireActivity().runOnUiThread { card5CupValue?.text = score.coerceAtLeast(0).toString(); card5CupValue?.visibility = View.VISIBLE }
            }
        }
        
        val card3CupValue = contentView.findViewById<android.widget.TextView>(R.id.card3CupValue)
        if (updatedCardId == R.id.card3CupValue && updatedScore != null) {
            val tv = card3CupValue
            if (isAdded && tv != null) {
                tv.visibility = android.view.View.VISIBLE
                if (delta != null && delta != 0) {
                    tv.post {
                        val deltaTextView = contentView.findViewById<android.widget.TextView>(R.id.card3CupDeltaText)
                        val oldScore = (updatedScore - delta).coerceAtLeast(0)
                        tv.text = oldScore.toString()
                        if (deltaTextView != null) {
                            animateCupScore(tv, deltaTextView, oldScore, updatedScore.coerceAtLeast(0), delta)
                        }
                    }
                } else {
                    tv.text = updatedScore.coerceAtLeast(0).toString()
                }
            }
        } else {
            ImpactCupRepository.fetchCupScore { score ->
                if (isAdded && dialog.isShowing) requireActivity().runOnUiThread { card3CupValue?.text = score.coerceAtLeast(0).toString(); card3CupValue?.visibility = View.VISIBLE }
            }
        }
        
        val card6CupValue = contentView.findViewById<android.widget.TextView>(R.id.card6CupValue)
        if (updatedCardId == R.id.card6CupValue && updatedScore != null) {
            val tv = card6CupValue
            if (isAdded && tv != null) {
                tv.visibility = android.view.View.VISIBLE
                if (delta != null && delta != 0) {
                    tv.post {
                        val deltaTextView = contentView.findViewById<android.widget.TextView>(R.id.card6CupDeltaText)
                        val oldScore = (updatedScore - delta).coerceAtLeast(0)
                        tv.text = oldScore.toString()
                        if (deltaTextView != null) {
                            animateCupScore(tv, deltaTextView, oldScore, updatedScore.coerceAtLeast(0), delta)
                        }
                    }
                } else {
                    tv.text = updatedScore.coerceAtLeast(0).toString()
                }
            }
        } else {
            BlindingImpactCupRepository.fetchCupScore { score ->
                if (isAdded && dialog.isShowing) requireActivity().runOnUiThread { card6CupValue?.text = score.coerceAtLeast(0).toString(); card6CupValue?.visibility = View.VISIBLE }
            }
        }
    }

    /**
     * GlobalValues.pendingCupPathRevealPartId bayrağını tüketir.
     * Bayrak set ise: veri yükler, dialog açar ve aktif olacak kartı önce inaktif,
     * 0.5s sonra aktif + pop animasyonuyla gösterir. Tüm süre boyunca ekran kilitli.
     * @return bayrak tüketildiyse true (blocker bu fonksiyon tarafından yönetilir)
     */
    private fun checkAndTriggerCupPathReveal(): Boolean {
        val partId = GlobalValues.pendingCupPathRevealPartId ?: return false
        GlobalValues.pendingCupPathRevealPartId = null

        if (!isAdded) return false

        addLaunchTouchBlocker()

        val context = requireContext()
        GlobalLessonData.loadLessonItemsForPart(context, 1) { items1 ->
            val active1 = items1.lastOrNull { it.type == LessonItem.TYPE_CHEST }?.stepIsFinish == true
            GlobalLessonData.loadLessonItemsForPart(context, 2) { items2 ->
                val active2 = items2.lastOrNull { it.type == LessonItem.TYPE_CHEST }?.stepIsFinish == true
                GlobalLessonData.loadLessonItemsForPart(context, 3) { items3 ->
                    val active3 = items3.lastOrNull { it.type == LessonItem.TYPE_CHEST }?.stepIsFinish == true
                    GlobalLessonData.loadLessonItemsForPart(context, 4) { items4 ->
                        val active4 = items4.lastOrNull { it.type == LessonItem.TYPE_CHEST }?.stepIsFinish == true
                        GlobalLessonData.loadLessonItemsForPart(context, 5) { items5 ->
                            val active5 = items5.lastOrNull { it.type == LessonItem.TYPE_CHEST }?.stepIsFinish == true
                            GlobalLessonData.loadLessonItemsForPart(context, 6) { items6 ->
                                val active6 = items6.lastOrNull { it.type == LessonItem.TYPE_CHEST }?.stepIsFinish == true
                                if (!isAdded) {
                                    releaseLaunchTouchBlocker()
                                    return@loadLessonItemsForPart
                                }
                                view?.postDelayed({
                                    displayCupPathDialogWithReveal(active1, active2, active3, active4, active5, active6, partId)
                                }, 500L) ?: displayCupPathDialogWithReveal(active1, active2, active3, active4, active5, active6, partId)
                            }
                        }
                    }
                }
            }
        }
        return true
    }

    /**
     * panel_cup_path'i [partId]'ye karşılık gelen kartı önce inaktif göstererek açar.
     * 0.5s sonra kart aktif hale gelir ve pop (scale) animasyonu oynar.
     * Animasyon bitince [releaseLaunchTouchBlocker] çağrılır.
     */
    private fun displayCupPathDialogWithReveal(
        active1: Boolean,
        active2: Boolean,
        active3: Boolean,
        active4: Boolean,
        active5: Boolean,
        active6: Boolean,
        revealPartId: Int,
    ) {
        if (!isAdded) {
            releaseLaunchTouchBlocker()
            return
        }
        val dialog = BottomSheetDialog(requireContext())
        dialog.setCancelable(false) // Animasyon bitene kadar kapatılmasını engelle
        val contentView = LayoutInflater.from(requireContext())
            .inflate(R.layout.panel_cup_path, null)
        dialog.setContentView(contentView)
        // Ödül satırı ağdan geliyor; veri gelene kadar kartlarla aynı gri.
        applyCupPathRewardLoadingState(contentView)
        // Bu panelde kartların durumu çağıran tarafından hazır geliyor (aşağıda hemen
        // çiziliyorlar), yani ödül satırının bekleyeceği bir şey yok.
        cupPathCardsRevealed = true
        
        val energyManager = (requireActivity() as? MainActivity)?.getEnergyManager()
        startEnergyUpdateTimer(contentView, energyManager)
        
        contentView.findViewById<android.view.View>(R.id.panelCupPathEnergyContainer)?.setOnClickListener {
            dialog.dismiss()
            GlobalValues.cupPathDialogRef?.get()?.dismiss()
            (requireActivity() as? MainActivity)?.openShopFragment()
        }

        dialog.setOnShowListener {
            val bottomSheetId = dialog.context.resources.getIdentifier("design_bottom_sheet", "id", dialog.context.packageName)
            val bottomSheet = dialog.findViewById<View>(bottomSheetId)
            bottomSheet?.let {
                it.setBackgroundResource(android.R.color.transparent)
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        // Aktif olacak kartın ID'lerini belirle
        val revealCardViewId: Int
        val revealTitleId: Int
        val revealCupIconId: Int
        val revealCupValueId: Int
        val revealLottieId: Int
        when (revealPartId) {
            1 -> { revealCardViewId = R.id.card1View; revealTitleId = R.id.card1Title; revealCupIconId = R.id.card1CupIcon; revealCupValueId = R.id.card1CupValue; revealLottieId = R.id.card1DinoAnim }
            2 -> { revealCardViewId = R.id.card2View; revealTitleId = R.id.card2Title; revealCupIconId = R.id.card2CupIcon; revealCupValueId = R.id.card2CupValue; revealLottieId = R.id.card2DinoAnim }
            3 -> { revealCardViewId = R.id.card3View; revealTitleId = R.id.card3Title; revealCupIconId = R.id.card3CupIcon; revealCupValueId = R.id.card3CupValue; revealLottieId = R.id.card3DinoAnim }
            4 -> { revealCardViewId = R.id.card4View; revealTitleId = R.id.card4Title; revealCupIconId = R.id.card4CupIcon; revealCupValueId = R.id.card4CupValue; revealLottieId = R.id.card4DinoAnim }
            5 -> { revealCardViewId = R.id.card5View; revealTitleId = R.id.card5Title; revealCupIconId = R.id.card5CupIcon; revealCupValueId = R.id.card5CupValue; revealLottieId = R.id.card5DinoAnim }
            else -> { revealCardViewId = R.id.card6View; revealTitleId = R.id.card6Title; revealCupIconId = R.id.card6CupIcon; revealCupValueId = R.id.card6CupValue; revealLottieId = R.id.card6DinoAnim }
        }

        // Önce tüm kartları gerçek durumlarıyla göster; reveal kart için aktif=false (inaktif) kullan
        setupCupPathCard(contentView, R.id.card1View, R.id.card1Title, R.id.card1CupIcon, R.id.card1CupValue, R.id.card1DinoAnim, if (revealPartId == 1) false else active1)
        setupCupPathCard(contentView, R.id.card2View, R.id.card2Title, R.id.card2CupIcon, R.id.card2CupValue, R.id.card2DinoAnim, if (revealPartId == 2) false else active2)
        setupCupPathCard(contentView, R.id.card3View, R.id.card3Title, R.id.card3CupIcon, R.id.card3CupValue, R.id.card3DinoAnim, if (revealPartId == 3) false else active3)
        setupCupPathCard(contentView, R.id.card4View, R.id.card4Title, R.id.card4CupIcon, R.id.card4CupValue, R.id.card4DinoAnim, if (revealPartId == 4) false else active4)
        setupCupPathCard(contentView, R.id.card5View, R.id.card5Title, R.id.card5CupIcon, R.id.card5CupValue, R.id.card5DinoAnim, if (revealPartId == 5) false else active5)
        setupCupPathCard(contentView, R.id.card6View, R.id.card6Title, R.id.card6CupIcon, R.id.card6CupValue, R.id.card6DinoAnim, if (revealPartId == 6) false else active6)

        bindCupPathRewards(contentView, dialog)

        dialog.setOnDismissListener {
            listOf(R.id.card1DinoAnim, R.id.card2DinoAnim, R.id.card3DinoAnim, R.id.card4DinoAnim, R.id.card5DinoAnim, R.id.card6DinoAnim).forEach { id ->
                contentView.findViewById<LottieAnimationView>(id)?.cancelAnimation()
            }
        }

        dialog.show()

        // Panel açıldığında dialog'un kendi penceresindeki dokunmaları da engelle
        dialog.window?.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )

        // 0.5s sonra reveal kart aktif hale geçer + pop animasyonu
        val revealCardView = contentView.findViewById<com.google.android.material.card.MaterialCardView>(revealCardViewId)
        contentView.postDelayed({
            if (!isAdded) {
                releaseLaunchTouchBlocker()
                return@postDelayed
            }
            // Kartı aktif duruma geçir
            val revealActive = when (revealPartId) { 1 -> active1; 2 -> active2; 3 -> active3; 4 -> active4; 5 -> active5; else -> active6 }
            setupCupPathCard(
                contentView,
                revealCardViewId,
                revealTitleId,
                revealCupIconId,
                revealCupValueId,
                revealLottieId,
                revealActive
            )
            
            val card1View = contentView.findViewById<View>(R.id.card1View)
            card1View?.setOnClickListener {
                showCupDifficultyPanel()
            }
            
            // card2View: extraction kupa modu (çıkarmalı toplama)
            contentView.findViewById<View>(R.id.card2View)?.setOnClickListener {
                showCupDifficultyPanel(
                    animFileName = "crocodile_anim.json",
                    isBlindingMode = false,
                    isExtractionMode = true,
                    cupScoreProvider = ExtractionCupRepository::fetchCupScore,
                    pendingDeltaSetter = { GlobalValues.pendingExtractionCupDelta = it },
                    cardCupValueId = R.id.card2CupValue
                )
            }

            // card5View: blinding extraction kupa modu (körleme + çıkarmalı)
            contentView.findViewById<View>(R.id.card5View)?.setOnClickListener {
                showCupDifficultyPanel(
                    animFileName = "fly_anim.json",
                    isBlindingMode = true,
                    isExtractionMode = true,
                    cupScoreProvider = BlindingExtractionCupRepository::fetchCupScore,
                    pendingDeltaSetter = { GlobalValues.pendingBlindingExtractionCupDelta = it },
                    cardCupValueId = R.id.card5CupValue
                )
            }
            
            // Pop (baloncuk) animasyonu: 1f → 1.18f → 1f
            revealCardView?.let { card ->
                card.animate()
                    .scaleX(1.18f).scaleY(1.18f)
                    .setDuration(160)
                    .withEndAction {
                        card.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(140)
                            .withEndAction {
                                dialog.setCancelable(true)
                                dialog.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                                releaseLaunchTouchBlocker()
                            }
                            .start()
                    }.start()
            } ?: run {
                dialog.setCancelable(true)
                dialog.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                releaseLaunchTouchBlocker()
            }
        }, 1000L)
    }

    // ── Kupa yolu sandıkları (Trophy Road) ──────────────────────────────

    /**
     * Bir kupa yolu kartındaki ödül satırının görünümleri ve ait olduğu kupa alanı.
     *
     * [cupValueId] de burada çünkü ders sonrası tazelemede hangi kartın değiştiği bu id ile
     * bildiriliyor; kart → kupa alanı eşleşmesi tek yerde kalsın diye listeye eklendi.
     */
    private data class CupPathRewardViews(
        val cupField: String,
        val cupValueId: Int,
        val zoneId: Int,
        val fillId: Int,
        val shineId: Int,
        val textId: Int,
        val chestId: Int,
    )

    /**
     * Kart sırası panel_cup_path.xml ile aynı. Eşleştirme, kartların tıklama davranışındaki
     * repository seçimiyle birebir: card1 toplama, card2 çıkarma, card3 çarpma, card4-6
     * bunların körleme hâlleri.
     */
    private val cupPathRewardViews = listOf(
        CupPathRewardViews(
            CupPathRewardRepository.FIELD_ADDITION, R.id.card1CupValue,
            R.id.card1RewardZone, R.id.card1RewardFill, R.id.card1RewardShine,
            R.id.card1RewardText, R.id.card1RewardChest,
        ),
        CupPathRewardViews(
            CupPathRewardRepository.FIELD_EXTRACTION, R.id.card2CupValue,
            R.id.card2RewardZone, R.id.card2RewardFill, R.id.card2RewardShine,
            R.id.card2RewardText, R.id.card2RewardChest,
        ),
        CupPathRewardViews(
            CupPathRewardRepository.FIELD_IMPACT, R.id.card3CupValue,
            R.id.card3RewardZone, R.id.card3RewardFill, R.id.card3RewardShine,
            R.id.card3RewardText, R.id.card3RewardChest,
        ),
        CupPathRewardViews(
            CupPathRewardRepository.FIELD_BLINDING_ADDITION, R.id.card4CupValue,
            R.id.card4RewardZone, R.id.card4RewardFill, R.id.card4RewardShine,
            R.id.card4RewardText, R.id.card4RewardChest,
        ),
        CupPathRewardViews(
            CupPathRewardRepository.FIELD_BLINDING_EXTRACTION, R.id.card5CupValue,
            R.id.card5RewardZone, R.id.card5RewardFill, R.id.card5RewardShine,
            R.id.card5RewardText, R.id.card5RewardChest,
        ),
        CupPathRewardViews(
            CupPathRewardRepository.FIELD_BLINDING_IMPACT, R.id.card6CupValue,
            R.id.card6RewardZone, R.id.card6RewardFill, R.id.card6RewardShine,
            R.id.card6RewardText, R.id.card6RewardChest,
        ),
    )

    /**
     * Süren çubuk animasyonları, kupa alanına göre. Panel animasyon bitmeden tekrar
     * tazelenirse eski animasyon iptal ediliyor; yoksa iki animasyon aynı çubuğu çekiştirir.
     */
    private val cupPathBarAnimators = mutableMapOf<String, ValueAnimator>()

    /**
     * Panelin son okuduğu kupa yolu durumları.
     *
     * Ders sonrası dönüşte çubuğun animasyonunu ağ cevabını beklemeden başlatmak için var;
     * cevap gelince tazeleniyor. Boşsa (uygulama yeniden kurulmuş) animasyon eskisi gibi
     * cevaptan sonra başlar.
     */
    private var cupPathStatesCache: Map<String, CupPathRewardRepository.CupPathState> = emptyMap()

    /**
     * Altı kartın ödül çubuğunu doldurur.
     *
     * Tek çağrı iki Firestore dokümanı okuyor (kupa puanları + ödül defteri), altı değil:
     * ikisi de tek dokümanda tutuluyor.
     *
     * Ders bitip panele dönüldüğünde [animatedCupField] verilir: o kartın çubuğu, kupa
     * sayısının yanındaki +N / -N ile aynı anda ve aynı sürede eski değerden yenisine akar.
     * Kupa düşmüşse çubuk geri çekilir.
     */
    /**
     * Ödül satırını (çubuk + sandık) veri gelene kadar gri gösterir.
     *
     * ## Neden
     * Kartların kendisi yüklenirken zaten gri duruyor ama çubukla sandık durmuyordu: sandık
     * tam renkli, çubuğun üstündeki yazı ise XML'deki yer tutucuyu ("200 / 300") gösteriyordu.
     * Yani kullanıcıya bir an UYDURMA bir ilerleme gösteriliyordu.
     *
     * ## Ne yapıyor
     * Sandık kartlardakiyle aynı gri ([R.color.lesson_locked]) filtreye alınıyor, yazı ve
     * dolgu gizleniyor (çubuk boş kalıyor, zemini zaten nötr), dokunma kapatılıyor — veri
     * yokken sandığa basmanın anlamı yok.
     *
     * Temizleme [bindCupPathRewardCard]'ın başında; yani gri yalnızca o kartın verisi
     * gerçekten geldiğinde kalkıyor. Veri hiç gelmezse kart gri kalıyor ve bu doğru:
     * bilmediğimiz bir ilerlemeyi çizmektense boş bırakmak dürüst olanı.
     */
    /**
     * Kartlar gerçek durumlarıyla çizildi mi.
     *
     * Ödül satırı (çubuk + sandık) TEK bir okumayla geliyor
     * ([CupPathRewardRepository.fetchStates]), kartların aktifliği ise ALTI bölümün ders
     * listesini arka arkaya okuyarak. İkincisi doğal olarak çok daha geç bitiyor ve ödül
     * satırı kartlardan önce renkleniyordu — panelin yarısı canlı, yarısı gri kalıyordu.
     *
     * Bu yüzden ödül verisi geldiğinde hemen çizilmiyor; [cupPathStatesCache]'e yazılıp
     * bekletiliyor ve kartlar açıldığında [revealCupPathRewards] ile birlikte çiziliyor.
     */
    private var cupPathCardsRevealed = false

    /**
     * Kartlar açıldı: elde olan ödül verisiyle çubukları ve sandıkları da aç.
     *
     * Veri henüz gelmediyse hiçbir şey yapmıyor — [bindCupPathRewards] kendi cevabı
     * geldiğinde zaten çizecek, çünkü o an bayrak artık açık.
     */
    private fun revealCupPathRewards(root: View, dialog: android.app.Dialog) {
        cupPathCardsRevealed = true
        cupPathRewardViews.forEach { views ->
            val state = cupPathStatesCache[views.cupField] ?: return@forEach
            bindCupPathRewardCard(root, views, state, dialog)
        }
    }

    private fun applyCupPathRewardLoadingState(root: View) {
        val locked = ContextCompat.getColor(root.context, R.color.lesson_locked)
        cupPathRewardViews.forEach { views ->
            root.findViewById<View>(views.fillId)?.visibility = View.INVISIBLE
            root.findViewById<View>(views.shineId)?.visibility = View.INVISIBLE
            root.findViewById<TextView>(views.textId)?.visibility = View.INVISIBLE
            root.findViewById<View>(views.zoneId)?.apply {
                isClickable = false
                setOnClickListener(null)
            }
            root.findViewById<ImageView>(views.chestId)?.apply {
                setColorFilter(locked, android.graphics.PorterDuff.Mode.SRC_IN)
                isClickable = false
                setOnClickListener(null)
            }
        }
    }

    private fun bindCupPathRewards(
        root: View,
        dialog: android.app.Dialog,
        animatedCupField: String? = null,
        animatedScore: Int? = null,
        delta: Int = 0,
    ) {
        val cacheBefore = cupPathStatesCache

        // Animasyon ağ cevabı beklenmeden başlıyor: çubuğun çizilmesi için gereken tek
        // bilinmeyen defter (hangi eşikler alındı) ve o, ders oynanırken değişemez. Panelin
        // önceki açılışından kalan kopya bu yüzden güvenli. Beklenseydi sayı çoktan artmış
        // olurken çubuk bir süre sonra hareket etmeye başlıyordu.
        val animatedViews = cupPathRewardViews.firstOrNull { it.cupField == animatedCupField }
        if (animatedViews != null && animatedScore != null && delta != 0) {
            cacheBefore[animatedViews.cupField]?.let { cached ->
                val target = cached.copy(cupScore = animatedScore.coerceAtLeast(0))
                bindCupPathRewardCard(
                    root, animatedViews, target, dialog,
                    from = target.copy(cupScore = (target.cupScore - delta).coerceAtLeast(0)),
                )
            }
        }

        CupPathRewardRepository.fetchStates { states ->
            if (!isAdded) return@fetchStates
            cupPathStatesCache = states
            cupPathRewardViews.forEach { views ->
                val fetched = states[views.cupField] ?: return@forEach
                val animated = views.cupField == animatedCupField

                // Süren animasyonun ortasına girmiyoruz: görünür bir sıçrama olurdu. Yalnızca
                // defter beklediğimizden farklıysa (ör. başka bir cihazdan sandık alınmış)
                // yeniden bağlanıyor.
                if (animated && cupPathBarAnimators.containsKey(views.cupField)) {
                    val cached = cacheBefore[views.cupField]
                    if (cached != null && cached.copy(cupScore = fetched.cupScore) == fetched) {
                        return@forEach
                    }
                }

                // Ders biter bitmez yapılan okuma yeni puanı henüz görmeyebilir. Kartın
                // üstündeki sayı ile çubuk ayrışmasın diye, o kart için ekrana yazılan puan
                // esas alınıyor; defter (hangi eşik alındı) yine okunandan geliyor.
                val state = if (animated && animatedScore != null) {
                    fetched.copy(cupScore = animatedScore.coerceAtLeast(0))
                } else {
                    fetched
                }
                val from = if (animated && delta != 0) {
                    state.copy(cupScore = (state.cupScore - delta).coerceAtLeast(0))
                } else {
                    null
                }
                bindCupPathRewardCard(root, views, state, dialog, from)
            }
        }
    }

    private fun bindCupPathRewardCard(
        root: View,
        views: CupPathRewardViews,
        state: CupPathRewardRepository.CupPathState,
        dialog: android.app.Dialog,
        from: CupPathRewardRepository.CupPathState? = null,
    ) {
        // Kartlar henüz açılmadıysa çizme; veri [cupPathStatesCache]'te bekliyor ve
        // kartlar açılınca [revealCupPathRewards] buraya geri geliyor. Sebebi
        // [cupPathCardsRevealed]'da.
        if (!cupPathCardsRevealed) return
        val zone = root.findViewById<View>(views.zoneId) ?: return
        val fill = root.findViewById<View>(views.fillId) ?: return
        val shine = root.findViewById<View>(views.shineId) ?: return
        val text = root.findViewById<TextView>(views.textId) ?: return
        val chest = root.findViewById<ImageView>(views.chestId) ?: return

        // Yükleme grisi kalkıyor; bkz. [applyCupPathRewardLoadingState].
        chest.clearColorFilter()
        fill.visibility = View.VISIBLE
        shine.visibility = View.VISIBLE
        text.visibility = View.VISIBLE

        // Sandık hak edilmemişken de net duruyor: soluk sandık "bozuk" izlenimi veriyordu.
        chest.alpha = 1f

        // Çubuk ile sandık ayrı davranıyor: hazır sandığa dokunan kullanıcı ödülü istiyor,
        // çubuğa dokunan ise ileride ne kazanacağını merak ediyor.
        zone.isClickable = true
        zone.setOnClickListener { openCupPathRoad(state.cupField, dialog) }
        chest.isClickable = true
        chest.setOnClickListener { onCupPathChestTapped(state, dialog) }

        cupPathBarAnimators.remove(views.cupField)?.cancel()
        text.text = (from ?: state).label

        // Dolgu ve parlama genişliği piksel cinsinden veriliyor ama çubuğun kendi genişliği
        // ölçüm bitmeden bilinmiyor; ilk karede 0 gelir. Bu yüzden post ile ölçüm sonrasına
        // bırakılıyor — animasyon da ancak orada anlamlı.
        zone.post {
            if (!isAdded || zone.width <= 0) return@post
            if (from == null) {
                applyCupPathProgress(zone, fill, shine, text, state)
                return@post
            }
            val animator = ValueAnimator.ofInt(from.cupScore, state.cupScore).apply {
                duration = CUP_SCORE_ANIM_MS
                addUpdateListener { anim ->
                    if (!isAdded) {
                        anim.cancel()
                        return@addUpdateListener
                    }
                    applyCupPathProgress(
                        zone, fill, shine, text,
                        state.copy(cupScore = anim.animatedValue as Int),
                    )
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        cupPathBarAnimators.remove(views.cupField)
                        if (isAdded) applyCupPathProgress(zone, fill, shine, text, state)
                    }
                })
            }
            cupPathBarAnimators[views.cupField] = animator
            animator.start()
        }
    }

    /**
     * Çubuğu tek bir duruma göre çizer.
     *
     * Dolgu, parlama ve renkler görevlerdeki/günlük sorudaki çubukla aynı yardımcıdan
     * geçiyor ([applyDailyQuestionProgressOverlayNow]); böylece üç yerde aynı görünüyorlar.
     */
    private fun applyCupPathProgress(
        zone: View,
        fill: View,
        shine: View,
        text: TextView,
        state: CupPathRewardRepository.CupPathState,
    ) {
        text.text = state.label
        applyDailyQuestionProgressOverlayNow(
            widthHost = zone,
            fill = fill,
            shine = shine,
            percent = state.fraction * 100f,
            complete = state.claimable,
        )
    }

    /**
     * Karttaki sandığa dokunuldu.
     *
     * Ödül hazırsa en kısa yol: panel kapanır, sandık ekranı açılır. Hazır değilse kupa yolu
     * ekranı açılır — "daha yok" demek yerine kullanıcıya neyin ne zaman geleceğini gösteriyor.
     *
     * Buradaki hak ediş kontrolü YALNIZCA görünüm için. Asıl doğrulamayı sunucu yapıyor
     * (claimCupPathChest), yani ekrandaki veri eskimiş olsa bile hak edilmemiş ödül verilmiyor.
     */
    private fun onCupPathChestTapped(
        state: CupPathRewardRepository.CupPathState,
        dialog: android.app.Dialog,
    ) {
        if (!isAdded) return
        if (!state.claimable) {
            openCupPathRoad(state.cupField, dialog)
            return
        }
        dismissCupPathPanel(dialog)
        // Karttaki sandık her zaman SIRADAKİ eşiği açıyor: kartta tek bir çubuk var ve o
        // çubuk sıradaki eşiği gösteriyor. Belirli bir eşiği seçmek kupa yolu ekranının işi.
        openAbacusContainerFragment(
            NewChestFragment.newInstance(
                NewChestFragment.ChestRarity.COMMON,
                source = AnalyticsLogger.CHEST_SOURCE_CUP_PATH,
                cupField = state.cupField,
                cupMilestone = state.nextMilestone,
            )
        )
    }

    /** Kupa yolu (Trophy Road) ekranını açar. */
    private fun openCupPathRoad(cupField: String, dialog: android.app.Dialog) {
        if (!isAdded) return
        dismissCupPathPanel(dialog)
        openAbacusContainerFragment(CupPathRoadFragment.newInstance(cupField))
    }

    /**
     * Paneli kapatır.
     *
     * İki kapatma var çünkü panel iki ayrı yoldan açılabiliyor ve elimizdeki referans her
     * zaman ekranda duranı göstermiyor; ikisini de kapatmak panelin arkada açık kalmasını
     * önlüyor. Aynı kalıp panelin diğer çıkışlarında da kullanılıyor.
     */
    private fun dismissCupPathPanel(dialog: android.app.Dialog) {
        dialog.dismiss()
        GlobalValues.cupPathDialogRef?.get()?.dismiss()
    }

    private fun setupCupPathCard(
        root: View,
        cardViewId: Int,
        titleId: Int,
        cupIconId: Int,
        cupValueId: Int,
        lottieId: Int,
        isActive: Boolean
    ) {
        val cardView = root.findViewById<com.google.android.material.card.MaterialCardView>(cardViewId)
        val titleText = root.findViewById<TextView>(titleId)
        val cupIcon = root.findViewById<ImageView>(cupIconId)
        val cupValue = root.findViewById<TextView>(cupValueId)
        val lottieView = root.findViewById<LottieAnimationView>(lottieId)

        val context = root.context

        if (isActive) {
            // Active visual state
            val colorRes = when (cardViewId) {
                R.id.card1View, R.id.card4View -> R.color.lesson_header_orange
                R.id.card2View, R.id.card5View -> R.color.lesson_header_green
                R.id.card3View, R.id.card6View -> R.color.lesson_header_blue
                else -> R.color.button_enabled
            }
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, colorRes))
            titleText.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            cupValue.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            cupIcon.clearColorFilter()

            // Lottie: active, playing, color filter cleared
            lottieView.speed = 1f
            lottieView.clearColorFilter()
            lottieView.clearValueCallback(
                com.airbnb.lottie.model.KeyPath("**"),
                com.airbnb.lottie.LottieProperty.COLOR_FILTER
            )
            lottieView.addValueCallback(
                com.airbnb.lottie.model.KeyPath("**"),
                com.airbnb.lottie.LottieProperty.COLOR_FILTER,
                com.airbnb.lottie.value.LottieValueCallback<android.graphics.ColorFilter?>(null)
            )
            lottieView.playAnimation()
        } else {
            // Inactive visual state
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.background_color))

            val lockedColor = ContextCompat.getColor(context, R.color.lesson_locked)
            titleText.setTextColor(lockedColor)
            cupValue.setTextColor(lockedColor)

            // Cup icon: gray color tint
            cupIcon.setColorFilter(lockedColor, android.graphics.PorterDuff.Mode.SRC_IN)

            // Lottie: gray color, paused
            lottieView.speed = 0f
            lottieView.pauseAnimation()
            lottieView.progress = 0f

            val filter = android.graphics.PorterDuffColorFilter(lockedColor, android.graphics.PorterDuff.Mode.SRC_ATOP)
            lottieView.addValueCallback(
                com.airbnb.lottie.model.KeyPath("**"),
                com.airbnb.lottie.LottieProperty.COLOR_FILTER,
                com.airbnb.lottie.value.LottieValueCallback<android.graphics.ColorFilter>(filter)
            )
            lottieView.invalidate()
        }
    }

    private fun releaseLaunchTouchBlocker() {
        activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun addLaunchTouchBlocker() {
        activity?.window?.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // View hiyerarşisini bırak (fragment geri yığınında beklerken bellekte kalıyordu).
        _binding = null
    }
}

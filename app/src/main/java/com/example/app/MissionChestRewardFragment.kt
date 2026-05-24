package com.example.app

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.databinding.FragmentMissionChestRewardBinding
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Sandık ödülünden sonra sadece bu sefer ilerleyen görevleri listeler; çubuklar animasyonla dolar.
 */
class MissionChestRewardFragment : Fragment() {

    private var _binding: FragmentMissionChestRewardBinding? = null
    private val binding get() = _binding!!
    private var isVideoFlowOpen = false
    private var popBackStackOnContinue = false
    private lateinit var beforeSnapshot: MissionsProgressStore.Snapshot
    private lateinit var afterSnapshot: MissionsProgressStore.Snapshot
    private var shouldOpenBadgeAfterContinue: Boolean = false
    private var badgePayloadQueue: ArrayList<String> = arrayListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMissionChestRewardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MainActivityChromeBlocker.acquire(requireActivity())
        popBackStackOnContinue = requireArguments().getBoolean(ARG_POP_BACKSTACK_ON_CONTINUE, false)
        shouldOpenBadgeAfterContinue = requireArguments().getBoolean(ARG_OPEN_BADGE_AFTER_CONTINUE, false)
        badgePayloadQueue = requireArguments().getStringArrayList(ARG_BADGE_PAYLOAD_QUEUE) ?: arrayListOf()
        val args = requireArguments()
        beforeSnapshot = MissionsProgressStore.Snapshot(
            dailyStepFinishCount = args.getInt(ARG_DAILY_STEP_FINISH_BEFORE),
            weeklyStepFinishCount = args.getInt(ARG_WEEKLY_STEP_FINISH_BEFORE),
            dailyStepIncrementCount = args.getInt(ARG_DAILY_STEP_INCREMENT_BEFORE),
            weeklyStepIncrementCount = args.getInt(ARG_WEEKLY_STEP_INCREMENT_BEFORE),
            dailyPerfectStepIncrementCount = args.getInt(ARG_DAILY_PERFECT_STEP_INCREMENT_BEFORE),
            weeklyPerfectStepIncrementCount = args.getInt(ARG_WEEKLY_PERFECT_STEP_INCREMENT_BEFORE),
            dailyChestRecordBreakCount = args.getInt(ARG_DAILY_CHEST_RECORD_BREAK_BEFORE),
            weeklyChestRecordBreakCount = args.getInt(ARG_WEEKLY_CHEST_RECORD_BREAK_BEFORE),
            dailyChestStarGainCount = args.getInt(ARG_DAILY_CHEST_STAR_GAIN_BEFORE),
            weeklyChestStarGainCount = args.getInt(ARG_WEEKLY_CHEST_STAR_GAIN_BEFORE),
            dailyLearnMinutesCount = args.getInt(ARG_DAILY_LEARN_MINUTES_BEFORE),
            weeklyLearnMinutesCount = args.getInt(ARG_WEEKLY_LEARN_MINUTES_BEFORE),
        )
        afterSnapshot = MissionsProgressStore.Snapshot(
            dailyStepFinishCount = args.getInt(ARG_DAILY_STEP_FINISH_AFTER),
            weeklyStepFinishCount = args.getInt(ARG_WEEKLY_STEP_FINISH_AFTER),
            dailyStepIncrementCount = args.getInt(ARG_DAILY_STEP_INCREMENT_AFTER),
            weeklyStepIncrementCount = args.getInt(ARG_WEEKLY_STEP_INCREMENT_AFTER),
            dailyPerfectStepIncrementCount = args.getInt(ARG_DAILY_PERFECT_STEP_INCREMENT_AFTER),
            weeklyPerfectStepIncrementCount = args.getInt(ARG_WEEKLY_PERFECT_STEP_INCREMENT_AFTER),
            dailyChestRecordBreakCount = args.getInt(ARG_DAILY_CHEST_RECORD_BREAK_AFTER),
            weeklyChestRecordBreakCount = args.getInt(ARG_WEEKLY_CHEST_RECORD_BREAK_AFTER),
            dailyChestStarGainCount = args.getInt(ARG_DAILY_CHEST_STAR_GAIN_AFTER),
            weeklyChestStarGainCount = args.getInt(ARG_WEEKLY_CHEST_STAR_GAIN_AFTER),
            dailyLearnMinutesCount = args.getInt(ARG_DAILY_LEARN_MINUTES_AFTER),
            weeklyLearnMinutesCount = args.getInt(ARG_WEEKLY_LEARN_MINUTES_AFTER),
        )
        binding.missionChestRewardTitle.setText(R.string.mission_chest_reward_title)
        binding.missionChestRewardRecycler.layoutManager = LinearLayoutManager(requireContext())
        renderRewardList()

        binding.missionChestRewardContinue.setOnClickListener {
            // remove() sonrası isAdded == false olur; kuyruk ve rozet kararını önce al, sonra activity FM ile aç.
            val openBadgeAfter = shouldOpenBadgeAfterContinue && badgePayloadQueue.isNotEmpty()
            val queueCopy = ArrayList(badgePayloadQueue)
            val activityFm = requireActivity().supportFragmentManager
            val main = activity as? MainActivity
            main?.prepareMapReturnAfterLessonClaim()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_right,
                )
                .remove(this@MissionChestRewardFragment)
                .commitNowAllowingStateLoss()
            main?.finalizeMapReturnAfterLessonClaim("MissionChestReward.continue")
            if (openBadgeAfter) {
                activityFm.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right,
                    )
                    .replace(
                        R.id.badgeFragmentContainter,
                        BadgeFragment.newLevelUpSequenceInstance(queueCopy, 0),
                    )
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        MainActivityChromeBlocker.release(activity)
        binding.missionChestRewardRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun renderRewardList() {
        val ctx = context ?: return
        val items = buildRewardList(ctx, beforeSnapshot, afterSnapshot)
        binding.missionChestRewardRecycler.adapter = MissionChestRewardAdapter(
            items = items,
            onCompletedQuestClick = { quest ->
                if (isVideoFlowOpen || !isAdded) return@MissionChestRewardAdapter
                val tag = MissionRewardRevealDialogFragment::class.java.simpleName
                if (childFragmentManager.findFragmentByTag(tag) != null) return@MissionChestRewardAdapter
                isVideoFlowOpen = true
                MissionRewardRevealDialogFragment().show(childFragmentManager, tag)
                childFragmentManager.executePendingTransactions()
                (childFragmentManager.findFragmentByTag(tag) as? MissionRewardRevealDialogFragment)
                    ?.setOnRewardClaimedCallback {
                        MissionsProgressStore.markMissionRewardClaimed(ctx, quest.window, quest.missionId)
                    }
                (childFragmentManager.findFragmentByTag(tag) as? MissionRewardRevealDialogFragment)
                    ?.setOnDismissCallback {
                        isVideoFlowOpen = false
                        if (isAdded && _binding != null) renderRewardList()
                    }
            },
        )
    }

    companion object {
        private const val ARG_DAILY_STEP_FINISH_BEFORE = "daily_step_finish_before"
        private const val ARG_WEEKLY_STEP_FINISH_BEFORE = "weekly_step_finish_before"
        private const val ARG_DAILY_STEP_INCREMENT_BEFORE = "daily_step_increment_before"
        private const val ARG_WEEKLY_STEP_INCREMENT_BEFORE = "weekly_step_increment_before"
        private const val ARG_DAILY_PERFECT_STEP_INCREMENT_BEFORE = "daily_perfect_step_increment_before"
        private const val ARG_WEEKLY_PERFECT_STEP_INCREMENT_BEFORE = "weekly_perfect_step_increment_before"
        private const val ARG_DAILY_CHEST_RECORD_BREAK_BEFORE = "daily_chest_record_break_before"
        private const val ARG_WEEKLY_CHEST_RECORD_BREAK_BEFORE = "weekly_chest_record_break_before"
        private const val ARG_DAILY_CHEST_STAR_GAIN_BEFORE = "daily_chest_star_gain_before"
        private const val ARG_WEEKLY_CHEST_STAR_GAIN_BEFORE = "weekly_chest_star_gain_before"
        private const val ARG_DAILY_LEARN_MINUTES_BEFORE = "daily_learn_minutes_before"
        private const val ARG_WEEKLY_LEARN_MINUTES_BEFORE = "weekly_learn_minutes_before"
        private const val ARG_DAILY_STEP_FINISH_AFTER = "daily_step_finish_after"
        private const val ARG_WEEKLY_STEP_FINISH_AFTER = "weekly_step_finish_after"
        private const val ARG_DAILY_STEP_INCREMENT_AFTER = "daily_step_increment_after"
        private const val ARG_WEEKLY_STEP_INCREMENT_AFTER = "weekly_step_increment_after"
        private const val ARG_DAILY_PERFECT_STEP_INCREMENT_AFTER = "daily_perfect_step_increment_after"
        private const val ARG_WEEKLY_PERFECT_STEP_INCREMENT_AFTER = "weekly_perfect_step_increment_after"
        private const val ARG_DAILY_CHEST_RECORD_BREAK_AFTER = "daily_chest_record_break_after"
        private const val ARG_WEEKLY_CHEST_RECORD_BREAK_AFTER = "weekly_chest_record_break_after"
        private const val ARG_DAILY_CHEST_STAR_GAIN_AFTER = "daily_chest_star_gain_after"
        private const val ARG_WEEKLY_CHEST_STAR_GAIN_AFTER = "weekly_chest_star_gain_after"
        private const val ARG_DAILY_LEARN_MINUTES_AFTER = "daily_learn_minutes_after"
        private const val ARG_WEEKLY_LEARN_MINUTES_AFTER = "weekly_learn_minutes_after"
        private const val ARG_POP_BACKSTACK_ON_CONTINUE = "pop_backstack_on_continue"
        private const val ARG_OPEN_BADGE_AFTER_CONTINUE = "open_badge_after_continue"
        private const val ARG_BADGE_PAYLOAD_QUEUE = "badge_payload_queue"

        fun newInstance(
            before: MissionsProgressStore.Snapshot,
            after: MissionsProgressStore.Snapshot,
            popBackStackOnContinue: Boolean = false,
            openBadgeAfterContinue: Boolean = false,
            badgePayloadQueue: List<String> = emptyList(),
        ) =
            MissionChestRewardFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DAILY_STEP_FINISH_BEFORE, before.dailyStepFinishCount)
                    putInt(ARG_WEEKLY_STEP_FINISH_BEFORE, before.weeklyStepFinishCount)
                    putInt(ARG_DAILY_STEP_INCREMENT_BEFORE, before.dailyStepIncrementCount)
                    putInt(ARG_WEEKLY_STEP_INCREMENT_BEFORE, before.weeklyStepIncrementCount)
                    putInt(ARG_DAILY_PERFECT_STEP_INCREMENT_BEFORE, before.dailyPerfectStepIncrementCount)
                    putInt(ARG_WEEKLY_PERFECT_STEP_INCREMENT_BEFORE, before.weeklyPerfectStepIncrementCount)
                    putInt(ARG_DAILY_CHEST_RECORD_BREAK_BEFORE, before.dailyChestRecordBreakCount)
                    putInt(ARG_WEEKLY_CHEST_RECORD_BREAK_BEFORE, before.weeklyChestRecordBreakCount)
                    putInt(ARG_DAILY_CHEST_STAR_GAIN_BEFORE, before.dailyChestStarGainCount)
                    putInt(ARG_WEEKLY_CHEST_STAR_GAIN_BEFORE, before.weeklyChestStarGainCount)
                    putInt(ARG_DAILY_LEARN_MINUTES_BEFORE, before.dailyLearnMinutesCount)
                    putInt(ARG_WEEKLY_LEARN_MINUTES_BEFORE, before.weeklyLearnMinutesCount)
                    putInt(ARG_DAILY_STEP_FINISH_AFTER, after.dailyStepFinishCount)
                    putInt(ARG_WEEKLY_STEP_FINISH_AFTER, after.weeklyStepFinishCount)
                    putInt(ARG_DAILY_STEP_INCREMENT_AFTER, after.dailyStepIncrementCount)
                    putInt(ARG_WEEKLY_STEP_INCREMENT_AFTER, after.weeklyStepIncrementCount)
                    putInt(ARG_DAILY_PERFECT_STEP_INCREMENT_AFTER, after.dailyPerfectStepIncrementCount)
                    putInt(ARG_WEEKLY_PERFECT_STEP_INCREMENT_AFTER, after.weeklyPerfectStepIncrementCount)
                    putInt(ARG_DAILY_CHEST_RECORD_BREAK_AFTER, after.dailyChestRecordBreakCount)
                    putInt(ARG_WEEKLY_CHEST_RECORD_BREAK_AFTER, after.weeklyChestRecordBreakCount)
                    putInt(ARG_DAILY_CHEST_STAR_GAIN_AFTER, after.dailyChestStarGainCount)
                    putInt(ARG_WEEKLY_CHEST_STAR_GAIN_AFTER, after.weeklyChestStarGainCount)
                    putInt(ARG_DAILY_LEARN_MINUTES_AFTER, after.dailyLearnMinutesCount)
                    putInt(ARG_WEEKLY_LEARN_MINUTES_AFTER, after.weeklyLearnMinutesCount)
                    putBoolean(ARG_POP_BACKSTACK_ON_CONTINUE, popBackStackOnContinue)
                    putBoolean(ARG_OPEN_BADGE_AFTER_CONTINUE, openBadgeAfterContinue)
                    putStringArrayList(ARG_BADGE_PAYLOAD_QUEUE, ArrayList(badgePayloadQueue))
                }
            }
    }
}

private sealed class MissionChestRewardListItem {
    data class Header(val title: String, val countdown: String) : MissionChestRewardListItem()
    data object Divider : MissionChestRewardListItem()
    data class Quest(
        val missionId: String,
        val window: MissionWindow,
        val isClaimed: Boolean,
        val title: String,
        val iconRes: Int,
        val target: Int,
        val fromCount: Int,
        val toCount: Int,
        val staggerIndex: Int,
    ) : MissionChestRewardListItem()
}

private fun buildRewardList(
    ctx: android.content.Context,
    before: MissionsProgressStore.Snapshot,
    after: MissionsProgressStore.Snapshot,
): List<MissionChestRewardListItem> {
    val out = mutableListOf<MissionChestRewardListItem>()
    var stagger = 0

    val weeklyQuests = mutableListOf<MissionChestRewardListItem.Quest>()
    MissionsProgressStore.selectedMissionsForWeekly(ctx).forEach { mission ->
        val beforeCount = minOf(
            MissionsProgressStore.missionProgress(before, MissionWindow.WEEKLY, mission),
            mission.target,
        )
        val afterCount = minOf(
            MissionsProgressStore.missionProgress(after, MissionWindow.WEEKLY, mission),
            mission.target,
        )
        if (beforeCount != afterCount) {
            val claimed = MissionsProgressStore.isMissionRewardClaimed(ctx, MissionWindow.WEEKLY, mission.id)
            weeklyQuests.add(
                MissionChestRewardListItem.Quest(
                    missionId = mission.id,
                    window = MissionWindow.WEEKLY,
                    isClaimed = claimed,
                    title = ctx.getString(mission.titleResId),
                    iconRes = R.drawable.crystal_ic,
                    target = mission.target,
                    fromCount = beforeCount,
                    toCount = afterCount,
                    staggerIndex = stagger++,
                ),
            )
        }
    }
    if (weeklyQuests.isNotEmpty()) {
        val weeklyLabel = formatWeeklyCountdownForReward(ctx, MissionsProgressStore.millisUntilWeeklyReset())
        out.add(MissionChestRewardListItem.Header(ctx.getString(R.string.missions_weekly_title), weeklyLabel))
        out.addAll(weeklyQuests)
    }

    val dailyQuests = mutableListOf<MissionChestRewardListItem.Quest>()
    MissionsProgressStore.selectedMissionsForDaily(ctx).forEach { mission ->
        val beforeCount = minOf(
            MissionsProgressStore.missionProgress(before, MissionWindow.DAILY, mission),
            mission.target,
        )
        val afterCount = minOf(
            MissionsProgressStore.missionProgress(after, MissionWindow.DAILY, mission),
            mission.target,
        )
        if (beforeCount != afterCount) {
            val claimed = MissionsProgressStore.isMissionRewardClaimed(ctx, MissionWindow.DAILY, mission.id)
            dailyQuests.add(
                MissionChestRewardListItem.Quest(
                    missionId = mission.id,
                    window = MissionWindow.DAILY,
                    isClaimed = claimed,
                    title = ctx.getString(mission.titleResId),
                    iconRes = R.drawable.crystal_ic,
                    target = mission.target,
                    fromCount = beforeCount,
                    toCount = afterCount,
                    staggerIndex = stagger++,
                ),
            )
        }
    }
    if (dailyQuests.isNotEmpty()) {
        if (out.isNotEmpty()) out.add(MissionChestRewardListItem.Divider)
        val dailyLabel = ctx.getString(R.string.missions_hours_short, MissionsProgressStore.hoursUntilDailyReset())
        out.add(MissionChestRewardListItem.Header(ctx.getString(R.string.missions_daily_title), dailyLabel))
        out.addAll(dailyQuests)
    }

    return out
}

private fun formatWeeklyCountdownForReward(ctx: android.content.Context, ms: Long): String {
    val hoursTotal = (ms / (1000 * 60 * 60)).toInt().coerceAtLeast(1)
    return if (ms >= 24L * 60 * 60 * 1000) {
        val days = ((ms + 24L * 60 * 60 * 1000 - 1) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
        ctx.getString(R.string.missions_days_short, days)
    } else {
        ctx.getString(R.string.missions_hours_short, hoursTotal)
    }
}

private class MissionChestRewardAdapter(
    private val items: List<MissionChestRewardListItem>,
    private val onCompletedQuestClick: (MissionChestRewardListItem.Quest) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_QUEST = 1
        private const val TYPE_DIVIDER = 2
        private const val STAGGER_MS = 120L
        private const val ANIM_DURATION_MS = 2800L
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is MissionChestRewardListItem.Header -> TYPE_HEADER
        is MissionChestRewardListItem.Quest -> TYPE_QUEST
        is MissionChestRewardListItem.Divider -> TYPE_DIVIDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> RewardHeaderVH(
                inflater.inflate(R.layout.item_mission_header, parent, false),
            )
            TYPE_QUEST -> RewardQuestVH(
                inflater.inflate(R.layout.item_mission_quest, parent, false),
            )
            TYPE_DIVIDER -> RewardDividerVH(
                inflater.inflate(R.layout.item_mission_divider, parent, false),
            )
            else -> throw IllegalArgumentException("unknown type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MissionChestRewardListItem.Header -> (holder as RewardHeaderVH).bind(item)
            is MissionChestRewardListItem.Quest -> (holder as RewardQuestVH).bind(item, onCompletedQuestClick)
            is MissionChestRewardListItem.Divider -> Unit
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is RewardQuestVH) holder.cancelAnim()
        super.onViewRecycled(holder)
    }

    private class RewardHeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.missionSectionTitle)
        private val countdown = view.findViewById<TextView>(R.id.missionSectionCountdown)

        fun bind(h: MissionChestRewardListItem.Header) {
            title.text = h.title
            countdown.text = h.countdown
        }
    }

    private class RewardDividerVH(view: View) : RecyclerView.ViewHolder(view)

    private class RewardQuestVH(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.missionTitle)
        private val progressTrack = view.findViewById<View>(R.id.missionProgressTrack)
        private val progressFill = view.findViewById<View>(R.id.missionProgressFill)
        private val progressShine = view.findViewById<View>(R.id.missionProgressShine)
        private val progressText = view.findViewById<TextView>(R.id.missionProgressText)
        private val icon = view.findViewById<android.widget.ImageView>(R.id.missionRewardIcon)

        private var animator: ValueAnimator? = null
        private var pendingWidthListener: ViewTreeObserver.OnGlobalLayoutListener? = null

        fun cancelAnim() {
            animator?.cancel()
            animator = null
            pendingWidthListener?.let { listener ->
                if (progressTrack.viewTreeObserver.isAlive) {
                    progressTrack.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                }
                pendingWidthListener = null
            }
        }

        fun bind(q: MissionChestRewardListItem.Quest, onCompletedQuestClick: (MissionChestRewardListItem.Quest) -> Unit) {
            cancelAnim()
            val ctx = itemView.context
            title.text = q.title
            icon.setImageResource(q.iconRes)

            val gold = ContextCompat.getColor(ctx, R.color.missions_progress_complete)
            val titleNormal = ContextCompat.getColor(ctx, R.color.missions_quest_title_normal)
            val labelDone = ContextCompat.getColor(ctx, R.color.background_color)
            val labelPending = ContextCompat.getColor(ctx, R.color.button_disabled)
            val labelClaimed = ContextCompat.getColor(ctx, R.color.black)

            val target = q.target.coerceAtLeast(1)
            val from = q.fromCount
            val to = q.toCount
            val completedAfter = to >= target
            val startPct = (from.coerceAtMost(target) * 100f) / target
            val endPct = (to.coerceAtMost(target) * 100f) / target

            val canClaim = completedAfter && !q.isClaimed
            itemView.isClickable = canClaim
            itemView.isFocusable = canClaim
            itemView.setOnClickListener {
                if (canClaim) onCompletedQuestClick(q)
            }

            fun applyVisualPercent(pct: Float) {
                val curEst = (target * pct / 100f).roundToInt().coerceIn(0, target)
                val done = curEst >= target
                applyMissionProgressOverlayNow(progressTrack, progressFill, progressShine, pct, done, q.isClaimed)
                if (q.isClaimed) {
                    title.setTextColor(titleNormal)
                    progressText.text = ctx.getString(R.string.mission_reward_claimed_label)
                    progressText.setTextColor(labelClaimed)
                } else if (done) {
                    title.setTextColor(gold)
                    progressText.text = ctx.getString(R.string.mission_completed_label)
                    progressText.setTextColor(labelDone)
                } else {
                    title.setTextColor(titleNormal)
                    progressText.text = ctx.getString(
                        R.string.mission_progress_format,
                        curEst.coerceAtMost(target),
                        target,
                    )
                    progressText.setTextColor(labelPending)
                }
            }

            fun runWhenTrackHasWidth(block: () -> Unit) {
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

            progressTrack.post {
                runWhenTrackHasWidth {
                    applyVisualPercent(startPct)
                    if (abs(endPct - startPct) < 0.01f) return@runWhenTrackHasWidth
                    val delay = q.staggerIndex * STAGGER_MS
                    progressTrack.postDelayed({
                        if (bindingAdapterPosition == RecyclerView.NO_POSITION) return@postDelayed
                        animator = ValueAnimator.ofFloat(startPct, endPct).apply {
                            duration = ANIM_DURATION_MS
                            interpolator = DecelerateInterpolator(1.6f)
                            addUpdateListener { va ->
                                applyVisualPercent(va.animatedValue as Float)
                            }
                            start()
                        }
                    }, delay)
                }
            }
        }
    }
}

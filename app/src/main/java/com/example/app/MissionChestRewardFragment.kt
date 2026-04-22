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
        val args = requireArguments()
        val before = MissionsProgressStore.Snapshot(
            dailyCount = args.getInt(ARG_DAILY_BEFORE),
            weeklyCount = args.getInt(ARG_WEEKLY_BEFORE),
        )
        val after = MissionsProgressStore.Snapshot(
            dailyCount = args.getInt(ARG_DAILY_AFTER),
            weeklyCount = args.getInt(ARG_WEEKLY_AFTER),
        )
        binding.missionChestRewardTitle.setText(R.string.mission_chest_reward_title)
        val items = buildRewardList(requireContext(), before, after)
        binding.missionChestRewardRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.missionChestRewardRecycler.adapter = MissionChestRewardAdapter(
            items = items,
            onCompletedQuestClick = {
                if (isVideoFlowOpen || !isAdded) return@MissionChestRewardAdapter
                val tag = CrystalBreakVideoFragment::class.java.simpleName
                if (childFragmentManager.findFragmentByTag(tag) != null) return@MissionChestRewardAdapter
                isVideoFlowOpen = true
                CrystalBreakVideoFragment.newInstance("crystal_red_yellow")
                    .show(childFragmentManager, tag)
                childFragmentManager.executePendingTransactions()
                (childFragmentManager.findFragmentByTag(tag) as? CrystalBreakVideoFragment)
                    ?.setOnDismissCallback { isVideoFlowOpen = false }
            },
        )

        binding.missionChestRewardContinue.setOnClickListener {
            parentFragmentManager.beginTransaction().remove(this@MissionChestRewardFragment).commit()
        }
    }

    override fun onDestroyView() {
        binding.missionChestRewardRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_DAILY_BEFORE = "daily_before"
        private const val ARG_WEEKLY_BEFORE = "weekly_before"
        private const val ARG_DAILY_AFTER = "daily_after"
        private const val ARG_WEEKLY_AFTER = "weekly_after"

        fun newInstance(before: MissionsProgressStore.Snapshot, after: MissionsProgressStore.Snapshot) =
            MissionChestRewardFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DAILY_BEFORE, before.dailyCount)
                    putInt(ARG_WEEKLY_BEFORE, before.weeklyCount)
                    putInt(ARG_DAILY_AFTER, after.dailyCount)
                    putInt(ARG_WEEKLY_AFTER, after.weeklyCount)
                }
            }
    }
}

private sealed class MissionChestRewardListItem {
    data class Header(val title: String, val countdown: String) : MissionChestRewardListItem()
    data object Divider : MissionChestRewardListItem()
    data class Quest(
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
    if (minOf(before.weeklyCount, 1) != minOf(after.weeklyCount, 1)) {
        weeklyQuests.add(
            MissionChestRewardListItem.Quest(
                title = ctx.getString(R.string.mission_finish_one_lesson),
                iconRes = R.drawable.crystal_ic,
                target = 1,
                fromCount = minOf(before.weeklyCount, 1),
                toCount = minOf(after.weeklyCount, 1),
                staggerIndex = stagger++,
            ),
        )
    }
    if (minOf(before.weeklyCount, 2) != minOf(after.weeklyCount, 2)) {
        weeklyQuests.add(
            MissionChestRewardListItem.Quest(
                title = ctx.getString(R.string.mission_finish_two_lessons),
                iconRes = R.drawable.crystal_ic,
                target = 2,
                fromCount = minOf(before.weeklyCount, 2),
                toCount = minOf(after.weeklyCount, 2),
                staggerIndex = stagger++,
            ),
        )
    }
    if (weeklyQuests.isNotEmpty()) {
        val weeklyLabel = formatWeeklyCountdownForReward(ctx, MissionsProgressStore.millisUntilWeeklyReset())
        out.add(MissionChestRewardListItem.Header(ctx.getString(R.string.missions_weekly_title), weeklyLabel))
        out.addAll(weeklyQuests)
    }

    val dailyQuests = mutableListOf<MissionChestRewardListItem.Quest>()
    if (minOf(before.dailyCount, 1) != minOf(after.dailyCount, 1)) {
        dailyQuests.add(
            MissionChestRewardListItem.Quest(
                title = ctx.getString(R.string.mission_finish_one_lesson),
                iconRes = R.drawable.crystal_ic,
                target = 1,
                fromCount = minOf(before.dailyCount, 1),
                toCount = minOf(after.dailyCount, 1),
                staggerIndex = stagger++,
            ),
        )
    }
    if (minOf(before.dailyCount, 2) != minOf(after.dailyCount, 2)) {
        dailyQuests.add(
            MissionChestRewardListItem.Quest(
                title = ctx.getString(R.string.mission_finish_two_lessons),
                iconRes = R.drawable.crystal_ic,
                target = 2,
                fromCount = minOf(before.dailyCount, 2),
                toCount = minOf(after.dailyCount, 2),
                staggerIndex = stagger++,
            ),
        )
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
    private val onCompletedQuestClick: () -> Unit,
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

        fun bind(q: MissionChestRewardListItem.Quest, onCompletedQuestClick: () -> Unit) {
            cancelAnim()
            val ctx = itemView.context
            title.text = q.title
            icon.setImageResource(q.iconRes)

            val gold = ContextCompat.getColor(ctx, R.color.missions_progress_complete)
            val titleNormal = ContextCompat.getColor(ctx, R.color.missions_quest_title_normal)
            val labelDone = ContextCompat.getColor(ctx, R.color.background_color)
            val labelPending = ContextCompat.getColor(ctx, R.color.button_disabled)

            val target = q.target.coerceAtLeast(1)
            val from = q.fromCount
            val to = q.toCount
            val completedAfter = to >= target
            val startPct = (from.coerceAtMost(target) * 100f) / target
            val endPct = (to.coerceAtMost(target) * 100f) / target

            itemView.setOnClickListener { onCompletedQuestClick() }

            fun applyVisualPercent(pct: Float) {
                val curEst = (target * pct / 100f).roundToInt().coerceIn(0, target)
                val done = curEst >= target
                applyMissionProgressOverlayNow(progressTrack, progressFill, progressShine, pct, done)
                if (done) {
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

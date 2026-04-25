package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.databinding.FragmentMissionsBinding
import java.util.Locale

class MissionsFragment : Fragment() {

    private var _binding: FragmentMissionsBinding? = null
    private val binding get() = _binding!!
    private var isVideoFlowOpen = false

    private lateinit var adapter: MissionsListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = MissionsListAdapter { quest: MissionListItem.Quest ->
            val ctx = context ?: return@MissionsListAdapter
            val done = quest.progress >= quest.target
            if (!done || quest.isClaimed) return@MissionsListAdapter
            if (isVideoFlowOpen || !isAdded) return@MissionsListAdapter
            val tag = MissionRewardRevealDialogFragment::class.java.simpleName
            if (childFragmentManager.findFragmentByTag(tag) != null) return@MissionsListAdapter
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
                    if (isAdded) adapter.submitList(buildMissionRows())
                }
        }
        binding.missionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.missionsRecyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        adapter.submitList(buildMissionRows())
    }

    override fun onDestroyView() {
        binding.missionsRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun buildMissionRows(): List<MissionListItem> {
        val ctx = requireContext()
        val snap = MissionsProgressStore.getSnapshot(ctx)
        val dailyHours = MissionsProgressStore.hoursUntilDailyReset()
        val weeklyMs = MissionsProgressStore.millisUntilWeeklyReset()
        val weeklyLabel = formatWeeklyCountdown(weeklyMs)

        return listOf(
            MissionListItem.Header(
                getString(R.string.missions_weekly_title),
                weeklyLabel,
            ),
        ) + MissionsProgressStore.selectedMissionsForWeekly(ctx).map { mission ->
            val progress = minOf(
                MissionsProgressStore.missionProgress(snap, MissionWindow.WEEKLY, mission),
                mission.target,
            )
            MissionListItem.Quest(
                missionId = mission.id,
                getString(mission.titleResId),
                progress,
                mission.target,
                R.drawable.crystal_ic,
                window = MissionWindow.WEEKLY,
                isClaimed = MissionsProgressStore.isMissionRewardClaimed(ctx, MissionWindow.WEEKLY, mission.id),
            )
        } + listOf(
            MissionListItem.Divider,
            MissionListItem.Header(
                getString(R.string.missions_daily_title),
                getString(R.string.missions_hours_short, dailyHours),
            ),
        ) + MissionsProgressStore.selectedMissionsForDaily(ctx).map { mission ->
            val progress = minOf(
                MissionsProgressStore.missionProgress(snap, MissionWindow.DAILY, mission),
                mission.target,
            )
            MissionListItem.Quest(
                missionId = mission.id,
                getString(mission.titleResId),
                progress,
                mission.target,
                R.drawable.crystal_ic,
                window = MissionWindow.DAILY,
                isClaimed = MissionsProgressStore.isMissionRewardClaimed(ctx, MissionWindow.DAILY, mission.id),
            )
        }
    }

    private fun formatWeeklyCountdown(ms: Long): String {
        val hoursTotal = (ms / (1000 * 60 * 60)).toInt().coerceAtLeast(1)
        return if (ms >= 24L * 60 * 60 * 1000) {
            val days = ((ms + 24L * 60 * 60 * 1000 - 1) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
            getString(R.string.missions_days_short, days)
        } else {
            getString(R.string.missions_hours_short, hoursTotal)
        }
    }
}

private sealed class MissionListItem {
    data class Header(val title: String, val countdown: String) : MissionListItem()
    data object Divider : MissionListItem()
    data class Quest(
        val missionId: String,
        val title: String,
        val progress: Int,
        val target: Int,
        val iconRes: Int,
        val window: MissionWindow,
        val isClaimed: Boolean,
    ) : MissionListItem()
}

private class MissionsListAdapter(
    private val onQuestClick: (MissionListItem.Quest) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<MissionListItem> = emptyList()

    fun submitList(list: List<MissionListItem>) {
        items = list
        notifyDataSetChanged()
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_QUEST = 1
        private const val TYPE_DIVIDER = 2
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is MissionListItem.Header -> TYPE_HEADER
        is MissionListItem.Quest -> TYPE_QUEST
        is MissionListItem.Divider -> TYPE_DIVIDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(
                inflater.inflate(R.layout.item_mission_header, parent, false),
            )
            TYPE_QUEST -> QuestVH(
                inflater.inflate(R.layout.item_mission_quest, parent, false),
                onQuestClick,
            )
            TYPE_DIVIDER -> DividerVH(
                inflater.inflate(R.layout.item_mission_divider, parent, false),
            )
            else -> throw IllegalArgumentException("unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MissionListItem.Header -> (holder as HeaderVH).bind(item)
            is MissionListItem.Quest -> (holder as QuestVH).bind(item)
            is MissionListItem.Divider -> Unit
        }
    }

    override fun getItemCount(): Int = items.size

    private class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.missionSectionTitle)
        private val countdown = view.findViewById<TextView>(R.id.missionSectionCountdown)

        fun bind(h: MissionListItem.Header) {
            title.text = h.title
            countdown.text = h.countdown
        }
    }

    private class DividerVH(view: View) : RecyclerView.ViewHolder(view)

    private class QuestVH(
        view: View,
        private val onQuestClick: (MissionListItem.Quest) -> Unit,
    ) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.missionTitle)
        private val progressTrack = view.findViewById<View>(R.id.missionProgressTrack)
        private val progressFill = view.findViewById<View>(R.id.missionProgressFill)
        private val progressShine = view.findViewById<View>(R.id.missionProgressShine)
        private val progressText = view.findViewById<TextView>(R.id.missionProgressText)
        private val icon = view.findViewById<android.widget.ImageView>(R.id.missionRewardIcon)

        fun bind(q: MissionListItem.Quest) {
            val ctx = itemView.context
            val done = q.progress >= q.target
            val pct = ((q.progress.coerceAtMost(q.target) * 100) / q.target.coerceAtLeast(1)).coerceIn(0, 100)

            title.text = q.title
            applyMissionProgressOverlay(progressTrack, progressFill, progressShine, pct, done, q.isClaimed)
            icon.setImageResource(q.iconRes)

            val gold = ContextCompat.getColor(ctx, R.color.missions_progress_complete)
            val titleNormal = ContextCompat.getColor(ctx, R.color.missions_quest_title_normal)
            val progressLabelDone = ContextCompat.getColor(ctx, R.color.background_color)
            val progressLabelPending = ContextCompat.getColor(ctx, R.color.button_disabled)
            val progressLabelClaimed = ContextCompat.getColor(ctx, R.color.black)

            if (q.isClaimed) {
                title.setTextColor(titleNormal)
                progressText.text = ctx.getString(R.string.mission_reward_claimed_label)
                progressText.setTextColor(progressLabelClaimed)
            } else if (done) {
                title.setTextColor(gold)
                progressText.text = ctx.getString(R.string.mission_completed_label)
                progressText.setTextColor(progressLabelDone)
            } else {
                title.setTextColor(titleNormal)
                progressText.text = String.format(
                    Locale.getDefault(),
                    "%d / %d",
                    q.progress.coerceAtMost(q.target),
                    q.target,
                )
                progressText.setTextColor(progressLabelPending)
            }

            val canClaim = done && !q.isClaimed
            itemView.isClickable = canClaim
            itemView.isFocusable = canClaim
            itemView.setOnClickListener {
                if (canClaim) onQuestClick(q)
            }
        }
    }
}

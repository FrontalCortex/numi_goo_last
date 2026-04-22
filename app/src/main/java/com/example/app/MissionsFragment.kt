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

class MissionsFragment : Fragment() {

    private var _binding: FragmentMissionsBinding? = null
    private val binding get() = _binding!!
    private var isVideoFlowOpen = false

    private val adapter = MissionsListAdapter {
        if (isVideoFlowOpen || !isAdded) return@MissionsListAdapter
        val tag = CrystalBreakVideoFragment::class.java.simpleName
        if (childFragmentManager.findFragmentByTag(tag) != null) return@MissionsListAdapter
        isVideoFlowOpen = true
        CrystalBreakVideoFragment.newInstance("crystal_red_yellow").show(childFragmentManager, tag)
        childFragmentManager.executePendingTransactions()
        (childFragmentManager.findFragmentByTag(tag) as? CrystalBreakVideoFragment)
            ?.setOnDismissCallback { isVideoFlowOpen = false }
    }

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
            MissionListItem.Quest(
                getString(R.string.mission_finish_one_lesson),
                minOf(snap.weeklyCount, 1),
                1,
                R.drawable.crystal_ic,
            ),
            MissionListItem.Quest(
                getString(R.string.mission_finish_two_lessons),
                minOf(snap.weeklyCount, 2),
                2,
                R.drawable.crystal_ic,
            ),
            MissionListItem.Divider,
            MissionListItem.Header(
                getString(R.string.missions_daily_title),
                getString(R.string.missions_hours_short, dailyHours),
            ),
            MissionListItem.Quest(
                getString(R.string.mission_finish_one_lesson),
                minOf(snap.dailyCount, 1),
                1,
                R.drawable.crystal_ic,
            ),
            MissionListItem.Quest(
                getString(R.string.mission_finish_two_lessons),
                minOf(snap.dailyCount, 2),
                2,
                R.drawable.crystal_ic,
            ),
        )
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
    data class Quest(val title: String, val progress: Int, val target: Int, val iconRes: Int) : MissionListItem()
}

private class MissionsListAdapter(
    private val onQuestClick: () -> Unit,
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
        private val onQuestClick: () -> Unit,
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
            applyMissionProgressOverlay(progressTrack, progressFill, progressShine, pct, done)
            icon.setImageResource(q.iconRes)

            val gold = ContextCompat.getColor(ctx, R.color.missions_progress_complete)
            val titleNormal = ContextCompat.getColor(ctx, R.color.missions_quest_title_normal)
            val progressLabelDone = ContextCompat.getColor(ctx, R.color.background_color)
            val progressLabelPending = ContextCompat.getColor(ctx, R.color.button_disabled)

            if (done) {
                title.setTextColor(gold)
                progressText.text = ctx.getString(R.string.mission_completed_label)
                progressText.setTextColor(progressLabelDone)
            } else {
                title.setTextColor(titleNormal)
                progressText.text = ctx.getString(R.string.mission_progress_format, q.progress.coerceAtMost(q.target), q.target)
                progressText.setTextColor(progressLabelPending)
            }

            itemView.setOnClickListener { onQuestClick() }
        }
    }
}

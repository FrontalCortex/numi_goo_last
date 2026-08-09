package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentMissionsBinding
import java.util.Locale

class MissionsFragment : Fragment() {

    private var _binding: FragmentMissionsBinding? = null
    private val binding get() = _binding!!
    private var isVideoFlowOpen = false

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
        updateMissionsUI()
    }

    override fun onResume() {
        super.onResume()
        updateMissionsUI()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun updateMissionsUI() {
        val b = _binding ?: return
        val ctx = context ?: return

        val snap = MissionsProgressStore.getSnapshot(ctx)
        val dailyHours = MissionsProgressStore.hoursUntilDailyReset()
        val weeklyMs = MissionsProgressStore.millisUntilWeeklyReset()
        val weeklyLabel = formatWeeklyCountdown(weeklyMs)

        b.weeklySectionCountdown.text = weeklyLabel
        b.dailySectionCountdown.text = getString(R.string.missions_hours_short, dailyHours)

        // Haftalık Görevleri Yükle
        b.weeklyQuestsContainer.removeAllViews()
        val weeklyMissions = MissionsProgressStore.selectedMissionsForWeekly(ctx)
        weeklyMissions.forEachIndexed { index, mission ->
            val progress = minOf(
                MissionsProgressStore.missionProgress(snap, MissionWindow.WEEKLY, mission),
                mission.target,
            )
            val isClaimed = MissionsProgressStore.isMissionRewardClaimed(ctx, MissionWindow.WEEKLY, mission.id)
            val questData = MissionQuestData(
                missionId = mission.id,
                title = getString(mission.titleResId),
                progress = progress,
                target = mission.target,
                iconRes = R.drawable.new_chest_close_ic2,
                window = MissionWindow.WEEKLY,
                isClaimed = isClaimed,
            )
            val itemView = createQuestItemView(b.weeklyQuestsContainer, questData)
            b.weeklyQuestsContainer.addView(itemView)

            if (index < weeklyMissions.size - 1) {
                addDivider(b.weeklyQuestsContainer)
            }
        }

        // Günlük Görevleri Yükle
        b.dailyQuestsContainer.removeAllViews()
        val dailyMissions = MissionsProgressStore.selectedMissionsForDaily(ctx)
        dailyMissions.forEachIndexed { index, mission ->
            val progress = minOf(
                MissionsProgressStore.missionProgress(snap, MissionWindow.DAILY, mission),
                mission.target,
            )
            val isClaimed = MissionsProgressStore.isMissionRewardClaimed(ctx, MissionWindow.DAILY, mission.id)
            val questData = MissionQuestData(
                missionId = mission.id,
                title = getString(mission.titleResId),
                progress = progress,
                target = mission.target,
                iconRes = R.drawable.new_chest_close_ic1,
                window = MissionWindow.DAILY,
                isClaimed = isClaimed,
            )
            val itemView = createQuestItemView(b.dailyQuestsContainer, questData)
            b.dailyQuestsContainer.addView(itemView)

            if (index < dailyMissions.size - 1) {
                addDivider(b.dailyQuestsContainer)
            }
        }
    }

    private fun addDivider(container: ViewGroup) {
        val dividerView = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1,
            ).apply {
                setMargins(0, 6, 0, 6)
            }
            setBackgroundColor(0x1AFFFFFF)
        }
        container.addView(dividerView)
    }

    private fun createQuestItemView(parent: ViewGroup, q: MissionQuestData): View {
        val ctx = parent.context
        val view = LayoutInflater.from(ctx).inflate(R.layout.item_mission_quest, parent, false)

        val title = view.findViewById<TextView>(R.id.missionTitle)!!
        val progressTrack = view.findViewById<View>(R.id.missionProgressTrack)!!
        val progressFill = view.findViewById<View>(R.id.missionProgressFill)!!
        val progressShine = view.findViewById<View>(R.id.missionProgressShine)!!
        val progressText = view.findViewById<TextView>(R.id.missionProgressText)!!
        val icon = view.findViewById<ImageView>(R.id.missionRewardIcon)!!

        val done = q.progress >= q.target
        val pct = ((q.progress.coerceAtMost(q.target) * 100f) / q.target.coerceAtLeast(1)).coerceIn(0f, 100f)

        title.text = q.title
        applyMissionProgressOverlay(
            widthHost = progressTrack,
            fill = progressFill,
            shine = progressShine,
            percent = pct.toInt(),
            done = done,
            claimed = q.isClaimed,
        )
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
        view.isClickable = canClaim
        view.isFocusable = canClaim
        view.setOnClickListener {
            if (canClaim) {
                onQuestClicked(q)
            }
        }

        return view
    }

    private fun onQuestClicked(quest: MissionQuestData) {
        val ctx = context ?: return
        val done = quest.progress >= quest.target
        if (!done || quest.isClaimed) return
        if (isVideoFlowOpen || !isAdded) return

        isVideoFlowOpen = true

        parentFragmentManager.setFragmentResultListener("chest_closed", viewLifecycleOwner) { _, _ ->
            MissionsProgressStore.markMissionRewardClaimed(requireContext(), quest.window, quest.missionId)
            isVideoFlowOpen = false
            updateMissionsUI()
            parentFragmentManager.clearFragmentResultListener("chest_closed")
        }

        val startRarity = if (quest.window == MissionWindow.WEEKLY) {
            NewChestFragment.ChestRarity.RARE
        } else {
            NewChestFragment.ChestRarity.COMMON
        }

        val mainActivity = activity as? MainActivity
        val containerId = mainActivity?.findViewById<View>(R.id.abacusFragmentContainer)?.id ?: R.id.fragmentContainerID
        mainActivity?.findViewById<View>(R.id.abacusFragmentContainer)?.visibility = android.view.View.VISIBLE

        parentFragmentManager.beginTransaction()
                .add(containerId, NewChestFragment.newInstance(startRarity))
                .commit()
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

    private data class MissionQuestData(
        val missionId: String,
        val title: String,
        val progress: Int,
        val target: Int,
        val iconRes: Int,
        val window: MissionWindow,
        val isClaimed: Boolean,
    )
}

package com.example.app

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentMissionChestRewardBinding
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class MissionChestRewardFragment : Fragment() {

    private var _binding: FragmentMissionChestRewardBinding? = null
    private val binding get() = _binding!!
    private var isVideoFlowOpen = false
    private var popBackStackOnContinue = false
    private lateinit var beforeSnapshot: MissionsProgressStore.Snapshot
    private lateinit var afterSnapshot: MissionsProgressStore.Snapshot
    private var shouldOpenBadgeAfterContinue: Boolean = false
    private var badgePayloadQueue: ArrayList<String> = arrayListOf()

    private val runningAnimators = mutableListOf<ValueAnimator>()

    private data class AnimatedQuest(
        val missionId: String,
        val window: MissionWindow,
        val isClaimed: Boolean,
        val title: String,
        val iconRes: Int,
        val target: Int,
        val fromCount: Int,
        val toCount: Int,
        val staggerIndex: Int,
    )

    fun setBadgePayloads(payloads: List<String>) {
        if (payloads.isNotEmpty()) {
            shouldOpenBadgeAfterContinue = true
            badgePayloadQueue.addAll(payloads)
        }
    }

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

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Hiçbir şey yapma
            }
        })

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
        
        renderRewardList()

        binding.missionChestRewardContinue.isEnabled = false
        binding.missionChestRewardContinue.postDelayed({
            if (isAdded) binding.missionChestRewardContinue.isEnabled = true
        }, 500)

        binding.missionChestRewardContinue.setOnClickListener {
            val openBadgeAfter = shouldOpenBadgeAfterContinue && badgePayloadQueue.isNotEmpty()
            val queueCopy = ArrayList(badgePayloadQueue)
            val activityFm = requireActivity().supportFragmentManager
            val main = activity as? MainActivity
            main?.logMapTouchDiag(
                "MissionChestReward.continue",
                "CLAIM_MAP_RETURN",
                "removeThenPrepare+finalize",
            )
            // FragmentTransaction'ın setCustomAnimations'ı burada güvenilir oynamıyor
            // (resultFragmentContainer prepareMapReturnAfterLessonClaim içinde GONE'a alınıyor);
            // bu yüzden view'ı elle kaydırıp animasyon bitince kaldırıyoruz.
            var mapReturnHandled = false
            val completeMapReturn: () -> Unit = {
                if (!mapReturnHandled) {
                    mapReturnHandled = true
                    if (isAdded) {
                        activityFm.beginTransaction()
                            .remove(this@MissionChestRewardFragment)
                            .commitNowAllowingStateLoss()
                    }
                    main?.prepareMapReturnAfterLessonClaim()
                    android.util.Log.d("DEBUG_BADGE", "MissionChestRewardFragment routing badge string payloads to finalizeMapReturnAfterLessonClaim, size=${queueCopy.size}, openBadgeAfter=$openBadgeAfter")
                    main?.finalizeMapReturnAfterLessonClaim(
                        caller = "MissionChestReward.continue",
                        badgeStringPayloads = if (openBadgeAfter) queueCopy else emptyList(),
                        // Görev ilerlemesi paneli gösterildiğinde haritaya dönüş buradan oluyor;
                        // item türü LESSON ise bu da bir lesson dönüşü sayılmalı.
                        isLessonTypeReturn = main?.isCurrentMapItemLessonType() == true,
                    )
                }
            }
            val rootView = binding.root
            rootView.animate()
                .translationX(rootView.width.toFloat())
                .setDuration(300L)
                .withEndAction { completeMapReturn() }
                .start()
            // Animasyon kesintiye uğrar da withEndAction hiç çalışmazsa (arka plana alma,
            // view'ın penceresinden kopması vb.) haritaya dönüşü garantiye alan yedek.
            android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed({ completeMapReturn() }, 600L)
        }
    }

    override fun onDestroyView() {
        MainActivityChromeBlocker.release(activity)
        runningAnimators.forEach { it.cancel() }
        runningAnimators.clear()
        _binding = null
        super.onDestroyView()
    }

    private fun renderRewardList() {
        val ctx = context ?: return
        
        binding.weeklyQuestsContainer.removeAllViews()
        binding.dailyQuestsContainer.removeAllViews()
        runningAnimators.forEach { it.cancel() }
        runningAnimators.clear()

        var stagger = 0
        val weeklyQuests = mutableListOf<AnimatedQuest>()
        MissionsProgressStore.selectedMissionsForWeekly(ctx).forEach { mission ->
            val beforeCount = minOf(
                MissionsProgressStore.missionProgress(beforeSnapshot, MissionWindow.WEEKLY, mission),
                mission.target,
            )
            val afterCount = minOf(
                MissionsProgressStore.missionProgress(afterSnapshot, MissionWindow.WEEKLY, mission),
                mission.target,
            )
            if (beforeCount != afterCount) {
                val claimed = MissionsProgressStore.isMissionRewardClaimed(ctx, MissionWindow.WEEKLY, mission.id)
                weeklyQuests.add(
                    AnimatedQuest(
                        missionId = mission.id,
                        window = MissionWindow.WEEKLY,
                        isClaimed = claimed,
                        title = ctx.getString(mission.titleResId),
                        iconRes = R.drawable.new_chest_close_ic2,
                        target = mission.target,
                        fromCount = beforeCount,
                        toCount = afterCount,
                        staggerIndex = stagger++,
                    )
                )
            }
        }

        if (weeklyQuests.isNotEmpty()) {
            binding.weeklySectionFrame.visibility = View.VISIBLE
            binding.weeklySectionCountdown.text = formatWeeklyCountdownForReward(ctx, MissionsProgressStore.millisUntilWeeklyReset(ctx))
            
            weeklyQuests.forEachIndexed { index, quest ->
                if (index > 0) addDivider(binding.weeklyQuestsContainer)
                binding.weeklyQuestsContainer.addView(createAnimatedQuestView(binding.weeklyQuestsContainer, quest))
            }
        } else {
            binding.weeklySectionFrame.visibility = View.GONE
        }

        val dailyQuests = mutableListOf<AnimatedQuest>()
        MissionsProgressStore.selectedMissionsForDaily(ctx).forEach { mission ->
            val beforeCount = minOf(
                MissionsProgressStore.missionProgress(beforeSnapshot, MissionWindow.DAILY, mission),
                mission.target,
            )
            val afterCount = minOf(
                MissionsProgressStore.missionProgress(afterSnapshot, MissionWindow.DAILY, mission),
                mission.target,
            )
            if (beforeCount != afterCount) {
                val claimed = MissionsProgressStore.isMissionRewardClaimed(ctx, MissionWindow.DAILY, mission.id)
                dailyQuests.add(
                    AnimatedQuest(
                        missionId = mission.id,
                        window = MissionWindow.DAILY,
                        isClaimed = claimed,
                        title = ctx.getString(mission.titleResId),
                        iconRes = R.drawable.new_chest_close_ic1,
                        target = mission.target,
                        fromCount = beforeCount,
                        toCount = afterCount,
                        staggerIndex = stagger++,
                    )
                )
            }
        }

        if (dailyQuests.isNotEmpty()) {
            binding.dailySectionFrame.visibility = View.VISIBLE
            binding.dailySectionCountdown.text = ctx.getString(R.string.missions_hours_short, MissionsProgressStore.hoursUntilDailyReset(ctx))
            
            dailyQuests.forEachIndexed { index, quest ->
                if (index > 0) addDivider(binding.dailyQuestsContainer)
                binding.dailyQuestsContainer.addView(createAnimatedQuestView(binding.dailyQuestsContainer, quest))
            }
        } else {
            binding.dailySectionFrame.visibility = View.GONE
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

    private fun createAnimatedQuestView(parent: ViewGroup, q: AnimatedQuest): View {
        val ctx = parent.context
        val view = LayoutInflater.from(ctx).inflate(R.layout.item_mission_quest, parent, false)
        
        val title = view.findViewById<TextView>(R.id.missionTitle)!!
        val progressTrack = view.findViewById<View>(R.id.missionProgressTrack)!!
        val progressFill = view.findViewById<View>(R.id.missionProgressFill)!!
        val progressShine = view.findViewById<View>(R.id.missionProgressShine)!!
        val progressText = view.findViewById<TextView>(R.id.missionProgressText)!!
        val icon = view.findViewById<ImageView>(R.id.missionRewardIcon)!!

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
        view.isClickable = canClaim
        view.isFocusable = canClaim
        view.setOnClickListener {
            if (canClaim) {
                if (isVideoFlowOpen || !isAdded) return@setOnClickListener
                isVideoFlowOpen = true

                parentFragmentManager.setFragmentResultListener("chest_closed", viewLifecycleOwner) { _, _ ->
                    MissionsProgressStore.markMissionRewardClaimed(ctx, q.window, q.missionId)
                    isVideoFlowOpen = false
                    if (isAdded && _binding != null) renderRewardList()
                    parentFragmentManager.clearFragmentResultListener("chest_closed")
                }

                val startRarity = if (q.window == MissionWindow.WEEKLY) {
                    NewChestFragment.ChestRarity.RARE
                } else {
                    NewChestFragment.ChestRarity.COMMON
                }

                // Sunucu isteğini fragment eklenmeden önce başlat — ilk açılıştaki gecikmeyi gizler.
                ServerRewards.prefetchChest(startRarity.name)

                val containerId = (requireView().parent as View).id
                parentFragmentManager.beginTransaction()
                    .add(containerId, NewChestFragment.newInstance(startRarity))
                    .addToBackStack("mission_chest")
                    .commit()
            }
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

        var pendingWidthListener: ViewTreeObserver.OnGlobalLayoutListener? = null
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
                val delay = q.staggerIndex * 120L
                progressTrack.postDelayed({
                    if (!isAdded || _binding == null) return@postDelayed
                    val animator = ValueAnimator.ofFloat(startPct, endPct).apply {
                        duration = 2800L
                        interpolator = DecelerateInterpolator(1.6f)
                        addUpdateListener { va ->
                            applyVisualPercent(va.animatedValue as Float)
                        }
                    }
                    runningAnimators.add(animator)
                    animator.start()
                }, delay)
            }
        }

        return view
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

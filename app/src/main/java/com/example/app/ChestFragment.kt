package com.example.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.example.app.databinding.FragmentChestBinding
import com.example.app.GlobalLessonData.globalPartId
import com.example.app.GlobalValues
import com.example.app.GlobalValues.mapFragmentStepIndex
import com.example.app.model.LessonItem
import com.google.firebase.auth.FirebaseAuth

class ChestFragment : Fragment() {
    private var isVideoFlowOpen = false
    private var isChestRevealReady = false
    private var _binding: FragmentChestBinding? = null
    private val binding get() = _binding!!
    private var icon: Int = 0
    private lateinit var lessonItem : LessonItem
    private var selectedVideoName: String = "crystal_red_yellow"
    private var currentReward: ChestRewardOutcome = ChestRewardOutcome(
        type = ChestRewardType.GOLD,
        amount = 0,
        iconRes = R.drawable.open_chest,
        label = "0 altın",
    )
    private var recordScore: Int = 0
    private var lessonSuccessRate: Float = 0f
    private var pendingChestRecordBreakMission: Boolean = false
    private var pendingChestStarGainAmount: Int = 0
    private var goldUpdateListener: GoldUpdateListener? = null
    /** Aynı anda yalnızca bir ödül akışı (çift tıklama engeli) */
    private var claimRewardInProgress = false

    private lateinit var loginLauncher: ActivityResultLauncher<Intent>

    private fun lessonProgressKey(item: LessonItem, fallbackIndex: Int): String {
        val stableId = item.id ?: -1
        val part = item.partId ?: -1
        return "${item.type}_${stableId}_${part}_${item.title}_$fallbackIndex"
    }

    /** [abacusFragmentContainer] üzerinde harita ([coordinator_layout]) dokunuşunu geçirmemek için. */
    private var chestHostView: View? = null
    private var chestHostSavedElevationPx = Float.NaN

    private fun elevateChestOverlayAboveMap() {
        val host = binding.root.parent as? View ?: return
        chestHostView = host
        val base = ViewCompat.getElevation(host).let { if (it.isNaN() || it < 0f) 0f else it }
        chestHostSavedElevationPx = base
        val bumpPx = 16f * resources.displayMetrics.density
        ViewCompat.setElevation(host, base + bumpPx)
    }

    private fun restoreChestOverlayElevation() {
        val h = chestHostView ?: return
        if (!chestHostSavedElevationPx.isNaN()) {
            ViewCompat.setElevation(h, chestHostSavedElevationPx)
        }
        chestHostView = null
        chestHostSavedElevationPx = Float.NaN
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            val lessonBeforeClaim = LessonManager.getLessonItem(mapFragmentStepIndex)
            val guideScheduled = MarathonGuideStore.scheduleIfEligible(
                requireContext(),
                globalPartId,
                mapFragmentStepIndex,
                lessonBeforeClaim,
                "ChestFragment.loginReturn",
            )
            updateMapProgress()
            val lessonAfterClaim = LessonManager.getLessonItem(mapFragmentStepIndex)
            Log.d(
                MarathonGuideStore.LOG_TAG,
                "ChestFragment.loginReturn | guideScheduled=$guideScheduled stepIsFinishAfter=${lessonAfterClaim?.stepIsFinish}",
            )
            (activity as? MainActivity)?.notifyMapVisibleAfterLessonClaim("ChestFragment.loginReturn")
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_left
                )
                .remove(this@ChestFragment)
                .commit()
            ChestRewardClaimHelper.applyReward(goldUpdateListener, currentReward)
            (activity as? MainActivity)?.tryShowPendingMarathonGuideOnMap("ChestFragment.loginReturn")
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is GoldUpdateListener) {
            goldUpdateListener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MainActivityChromeBlocker.acquire(requireActivity())
        lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex)!!
        recordScore = arguments?.getInt("toplamPuan", arguments?.getInt("dersPuani", 0) ?: 0) ?: 0
        lessonSuccessRate = arguments?.getFloat("successRate", 0f) ?: 0f
        pendingChestRecordBreakMission =
            arguments?.getBoolean(ChestResult.ARG_PENDING_CHEST_RECORD_BREAK_MISSION, false) == true
        //record()
        changeCupIcon()
        selectedVideoName = ChestCrystalPolicy.resolveVideoName()
        currentReward = ChestCrystalPolicy.resolveRewardForVideo(selectedVideoName)
        prepareHiddenRewardUi()
        applyRewardUiState(currentReward)
        setupClaimRewardButton()
        showCrystalBreakAtStart()
        binding.root.post { elevateChestOverlayAboveMap() }
    }


    private fun setupClaimRewardButton() {
        binding.claimRewardButton.setOnClickListener {
            // Tutorial 1'de bu açılışta sadece 1 kez: login start ekranına yönlendir (aynı açılışta tekrar gelmesin)
            val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            if (GlobalValues.currentTutorialNumber == 1) {
                FirstTutorialShownStore.markShown(requireContext(), "ChestFragment.claim")
            }
            if (GlobalValues.currentTutorialNumber == 1 && FirebaseAuth.getInstance().currentUser == null) {
                if (claimRewardInProgress) return@setOnClickListener
                claimRewardInProgress = true
                binding.claimRewardButton.isEnabled = false
                prefs.edit().putBoolean("tutorial1_login_flow_pending", true).apply()
                GlobalValues.currentTutorialNumber = 0
                loginLauncher.launch(
                    Intent(requireContext(), LoginStartActivity::class.java)
                        .putExtra(LoginStartActivity.EXTRA_BLOCK_BACK, true),
                )
                return@setOnClickListener
            }

            if (claimRewardInProgress) return@setOnClickListener
            claimRewardInProgress = true
            binding.claimRewardButton.isEnabled = false

            try {
                LessonProgressDiag.log(
                    "ChestFragment.claim",
                    "ENTER mapIdx=$mapFragmentStepIndex part=$globalPartId tutorial=${GlobalValues.currentTutorialNumber} " +
                        "successRate=$lessonSuccessRate recordScore=$recordScore",
                )
                val beforeSnap = MissionsProgressStore.getSnapshot(requireContext())
                MissionsProgressStore.applyPendingLearningMinutes(requireContext())
                if (pendingChestRecordBreakMission) {
                    MissionsProgressStore.recordChestRecordBreakProgress(requireContext())
                    pendingChestRecordBreakMission = false
                }
                if (pendingChestStarGainAmount > 0) {
                    MissionsProgressStore.recordChestStarGainProgress(requireContext(), pendingChestStarGainAmount)
                    pendingChestStarGainAmount = 0
                }
                // updateMapProgress() bu tıklamada stepIsFinish=true yapar; dart artışı için önceki durumu oku.
                val lessonBeforeClaim = LessonManager.getLessonItem(mapFragmentStepIndex)
                LessonProgressDiag.logItem(
                    "ChestFragment.claim",
                    globalPartId,
                    mapFragmentStepIndex,
                    lessonBeforeClaim,
                    "BEFORE updateMapProgress",
                )
                val guideScheduled = MarathonGuideStore.scheduleIfEligible(
                    requireContext(),
                    globalPartId,
                    mapFragmentStepIndex,
                    lessonBeforeClaim,
                    "ChestFragment.claim",
                )
                LessonProgressDiag.log("ChestFragment.claim", "BEFORE updateMapProgress mapIdx=$mapFragmentStepIndex")
                val incrementRocketDailyLessons = updateMapProgress()
                // Ders görev sayaçları updateMapProgress içinde yazılır; afterSnap bundan sonra okunmalı.
                val afterSnap = MissionsProgressStore.getSnapshot(requireContext(), applyCloudHydrate = false)
                LessonProgressDiag.logItemDelta(
                    "ChestFragment.claim",
                    globalPartId,
                    mapFragmentStepIndex,
                    lessonBeforeClaim,
                    LessonManager.getLessonItem(mapFragmentStepIndex),
                )
                val lessonAfterClaim = LessonManager.getLessonItem(mapFragmentStepIndex)
                LessonProgressDiag.logItem(
                    "ChestFragment.claim",
                    globalPartId,
                    mapFragmentStepIndex,
                    lessonAfterClaim,
                    "AFTER updateMapProgress",
                )
                MarathonGuideStore.logLessonSnapshot(
                    "ChestFragment.claimAfterUpdate",
                    globalPartId,
                    mapFragmentStepIndex,
                    lessonAfterClaim,
                )
                Log.d(
                    MarathonGuideStore.LOG_TAG,
                    "ChestFragment.claim | guideScheduled=$guideScheduled stepIsFinishAfter=${lessonAfterClaim?.stepIsFinish}",
                )
                val isPerfectLesson = lessonSuccessRate >= 100f
                val shouldIncrementDartProgress = when (val item = lessonBeforeClaim) {
                    null -> false
                    else ->
                        item.type == LessonItem.TYPE_LESSON &&
                            !item.stepIsFinish &&
                            isPerfectLesson
                }
                val shouldIncrementKarate = when (val item = lessonBeforeClaim) {
                    null -> false
                    else -> ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(item, recordScore)
                }
                val completedMissionCount = countCompletedMissions(beforeSnap, afterSnap)
                val proceedWithResult: (List<BadgeLevelUpPayload>) -> Unit = { levelUpPayloads ->
                    val fm = parentFragmentManager
                    val hostContainerId = (requireView().parent as View).id
                    if (hostContainerId == View.NO_ID) {
                        Log.e("ChestFragment", "ChestFragment host container id yok")
                        claimRewardInProgress = false
                        if (isAdded && _binding != null) {
                            binding.claimRewardButton.isEnabled = true
                        }
                    } else {
                        val hasMissionProgress = MissionsProgressStore.hasVisibleMissionProgress(requireContext(), beforeSnap, afterSnap)
                        val missionCounterDelta = beforeSnap.dailyStepFinishCount != afterSnap.dailyStepFinishCount ||
                            beforeSnap.weeklyStepFinishCount != afterSnap.weeklyStepFinishCount ||
                            beforeSnap.dailyStepIncrementCount != afterSnap.dailyStepIncrementCount ||
                            beforeSnap.weeklyStepIncrementCount != afterSnap.weeklyStepIncrementCount ||
                            beforeSnap.dailyPerfectStepIncrementCount != afterSnap.dailyPerfectStepIncrementCount ||
                            beforeSnap.weeklyPerfectStepIncrementCount != afterSnap.weeklyPerfectStepIncrementCount ||
                            beforeSnap.dailyChestRecordBreakCount != afterSnap.dailyChestRecordBreakCount ||
                            beforeSnap.weeklyChestRecordBreakCount != afterSnap.weeklyChestRecordBreakCount ||
                            beforeSnap.dailyChestStarGainCount != afterSnap.dailyChestStarGainCount ||
                            beforeSnap.weeklyChestStarGainCount != afterSnap.weeklyChestStarGainCount ||
                            beforeSnap.dailyLearnMinutesCount != afterSnap.dailyLearnMinutesCount ||
                            beforeSnap.weeklyLearnMinutesCount != afterSnap.weeklyLearnMinutesCount
                        LessonProgressDiag.logMissionPanelDecision(
                            "ChestFragment.proceed",
                            requireContext(),
                            beforeSnap,
                            afterSnap,
                            hasMissionProgress,
                        )
                        Log.d(
                            MarathonGuideStore.LOG_TAG,
                            "ChestFragment.proceed | hasMissionProgress=$hasMissionProgress " +
                                "guideScheduled=$guideScheduled badgeQueue=${levelUpPayloads.size} " +
                                "missionSnapLocalOnly=true",
                        )
                        if (hasMissionProgress) {
                            LessonProgressDiag.recordClaim(
                                mapIdx = mapFragmentStepIndex,
                                partId = globalPartId,
                                itemAfter = lessonAfterClaim,
                                route = "MISSION_PANEL",
                                hasMissionProgress = true,
                                missionCounterDelta = missionCounterDelta,
                                incrementRocketDailyLessons = incrementRocketDailyLessons,
                                tutorialNumber = GlobalValues.currentTutorialNumber,
                            )
                            fm.beginTransaction()
                                .setCustomAnimations(
                                    R.anim.slide_in_left,
                                    R.anim.slide_out_left,
                                )
                                .replace(
                                    hostContainerId,
                                    MissionChestRewardFragment.newInstance(
                                        before = beforeSnap,
                                        after = afterSnap,
                                        openBadgeAfterContinue = levelUpPayloads.isNotEmpty(),
                                        badgePayloadQueue = levelUpPayloads.map { BadgeProgressFirestore.payloadToQueueItem(it) },
                                    ),
                                )
                                .commitNowAllowingStateLoss()
                        } else {
                            LessonProgressDiag.recordClaim(
                                mapIdx = mapFragmentStepIndex,
                                partId = globalPartId,
                                itemAfter = lessonAfterClaim,
                                route = "MAP_DIRECT",
                                hasMissionProgress = false,
                                missionCounterDelta = missionCounterDelta,
                                incrementRocketDailyLessons = incrementRocketDailyLessons,
                                tutorialNumber = GlobalValues.currentTutorialNumber,
                            )
                            val main = activity as? MainActivity
                            main?.logMapTouchDiag(
                                "ChestFragment.claim",
                                "CLAIM_MAP_RETURN",
                                "removeThenPrepare+finalize",
                            )
                            if (isAdded) {
                                fm.beginTransaction()
                                    .setCustomAnimations(
                                        R.anim.slide_in_left,
                                        R.anim.slide_out_left,
                                    )
                                    .remove(this@ChestFragment)
                                    .commitNowAllowingStateLoss()
                            }
                            main?.prepareMapReturnAfterLessonClaim()
                            val activityFm = requireActivity().supportFragmentManager
                            main?.finalizeMapReturnAfterLessonClaim("ChestFragment.claimAfterRemove")
                            if (levelUpPayloads.isNotEmpty()) {
                                BadgeProgressFirestore.openBadgeCelebration(activityFm, levelUpPayloads)
                            }
                        }
                        ChestRewardClaimHelper.applyReward(goldUpdateListener, currentReward)
                    }
                }
                if (shouldIncrementDartProgress || completedMissionCount > 0 || shouldIncrementKarate || incrementRocketDailyLessons) {
                    BadgeProgressFirestore.incrementBadgeProgressAndDetectLevelUp(
                        incrementDart = shouldIncrementDartProgress,
                        incrementBowlingBy = completedMissionCount,
                        incrementKarate = shouldIncrementKarate,
                        incrementRocketDailyLessons = incrementRocketDailyLessons,
                        incrementGolf = false,
                        onDone = proceedWithResult,
                    )
                } else {
                    proceedWithResult(emptyList())
                }
            } catch (e: IllegalStateException) {
                Log.e("ChestFragment", "Ödül fragment işlemi başarısız", e)
                claimRewardInProgress = false
                if (isAdded && _binding != null) {
                    binding.claimRewardButton.isEnabled = true
                }
            }
        }
    }

    private fun countCompletedMissions(
        before: MissionsProgressStore.Snapshot,
        after: MissionsProgressStore.Snapshot,
    ): Int {
        val ctx = requireContext()
        val dailyCompleted = MissionsProgressStore.selectedMissionsForDaily(ctx).count { mission ->
            val beforeCount = MissionsProgressStore.missionProgress(before, MissionWindow.DAILY, mission)
            val afterCount = MissionsProgressStore.missionProgress(after, MissionWindow.DAILY, mission)
            beforeCount < mission.target && afterCount >= mission.target
        }
        val weeklyCompleted = MissionsProgressStore.selectedMissionsForWeekly(ctx).count { mission ->
            val beforeCount = MissionsProgressStore.missionProgress(before, MissionWindow.WEEKLY, mission)
            val afterCount = MissionsProgressStore.missionProgress(after, MissionWindow.WEEKLY, mission)
            beforeCount < mission.target && afterCount >= mission.target
        }
        return dailyCompleted + weeklyCompleted
    }

    private fun prepareHiddenRewardUi() {
        binding.chestImage.visibility = View.INVISIBLE
        binding.goldText.visibility = View.GONE
        binding.claimRewardButton.visibility = View.GONE
    }

    private fun showCrystalBreakAtStart() {
        if (isChestRevealReady || isVideoFlowOpen || !isAdded) return
        val tag = CrystalBreakVideoFragment::class.java.simpleName
        if (childFragmentManager.findFragmentByTag(tag) != null) return

        isVideoFlowOpen = true
        CrystalBreakVideoFragment.newInstance(selectedVideoName).show(childFragmentManager, tag)
        childFragmentManager.executePendingTransactions()
        (childFragmentManager.findFragmentByTag(tag) as? CrystalBreakVideoFragment)
            ?.setOnDismissCallback {
                isVideoFlowOpen = false
                if (!isAdded || _binding == null) return@setOnDismissCallback
                revealChestRewardUi()
            }
    }

    private fun revealChestRewardUi() {
        if (isChestRevealReady) return
        isChestRevealReady = true
        applyRewardUiState(currentReward)
        binding.chestImage.visibility = View.VISIBLE
        binding.goldText.visibility = View.VISIBLE
        binding.claimRewardButton.visibility = View.VISIBLE
    }

    private fun applyRewardUiState(reward: ChestRewardOutcome) {
        binding.chestImage.setImageResource(reward.iconRes)
        binding.goldText.text = reward.label
    }

    private fun changeCupIcon() {
        if (lessonItem.type != LessonItem.TYPE_CHEST) return
        val record = lessonItem.record ?: 0
        val p1 = lessonItem.cupPoint1
        val p2 = lessonItem.cupPoint2
        val resolvedIcon = when {
            p1 != null && record >= p1 -> R.drawable.chest_stars_tier3
            p2 != null && record >= p2 -> R.drawable.chest_stars_tier2
            record >= 500 -> R.drawable.chest_stars_tier1
            else -> R.drawable.chest_stars_tier0
        }
        val currentIcon = lessonItem.stepCupIcon
        val iconChanged = currentIcon != resolvedIcon
        val currentStars = starCountForChestIcon(currentIcon)
        val resolvedStars = starCountForChestIcon(resolvedIcon)
        val gainedStars = (resolvedStars - currentStars).coerceAtLeast(0)
        icon = if (iconChanged) resolvedIcon else currentIcon
        pendingChestStarGainAmount = if (iconChanged) gainedStars else 0
    }

    private fun starCountForChestIcon(iconResId: Int): Int = when (iconResId) {
        R.drawable.chest_stars_tier3 -> 3
        R.drawable.chest_stars_tier2 -> 2
        R.drawable.chest_stars_tier1 -> 1
        R.drawable.chest_stars_tier0 -> 0
        R.drawable.star_on_ic -> 0 // default tek yıldız görseli, görev hesabında 0 sayılıyor
        else -> 0
    }

    /**
     * @return `userRocketDailyLessons` (+1) için: [MissionsProgressStore.recordStepIncrementProgress] ile aynı bayrak
     * (`shouldIncrementStepCountMission` — günlük/haftalık STEP_INCREMENT görevleri). Firestore alanı görev prefs’ten bağımsızdır.
     */
    private fun updateMapProgress(): Boolean {
        val lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex) //Global verilerden tıklanan indeksteki adım öğesini alıyor
        LessonProgressDiag.log(
            "ChestFragment.updateMapProgress",
            "ENTER mapIdx=$mapFragmentStepIndex part=$globalPartId recordScore=$recordScore",
        )
        LessonProgressDiag.logItem(
            "ChestFragment.updateMapProgress",
            globalPartId,
            mapFragmentStepIndex,
            lessonItem,
            "BEFORE",
        )
        var index: Int = mapFragmentStepIndex + 1
        if(lessonItem?.cupPoint1 != null){
            index += 1
        }
        val lessonItem2 = LessonManager.getLessonItem(index)
        var shouldIncrementStepFinishMission = false
        var shouldIncrementStepCountMission = false
        var shouldIncrementPerfectStepCountMission = false
        lessonItem?.let { item ->
            // İlk adım true, diğerleri false olacak şekilde stepCompletionStatus oluştur
            val newStepCompletionStatus = List(item.stepCount) { index -> index < item.currentStep }

            when (item.raceBusyLevel) {
                1 -> {
                    LessonProgressDiag.log(
                        "ChestFragment.updateMapProgress",
                        "BRANCH raceBusyLevel==1 (race update; finish uses snapshot raceBusy=${item.raceBusyLevel})",
                    )
                    val updatedItem = item.copy(raceBusyLevel = 0)
                    LessonManager.updateRaceItem(requireContext(), mapFragmentStepIndex, updatedItem)
                    unlockNextRaceItemSkippingCompleted(mapFragmentStepIndex)
                }
                2 -> {
                    LessonProgressDiag.log(
                        "ChestFragment.updateMapProgress",
                        "BRANCH raceBusyLevel==2 (fast-forward; only self → TAMAMLANDI, next unchanged)",
                    )
                    val updatedItem = item.copy(raceBusyLevel = 0)
                    LessonManager.updateRaceItem(requireContext(), mapFragmentStepIndex, updatedItem)
                }
            }

            when {
                item.raceBusyLevel == 2 -> {
                    LessonProgressDiag.log(
                        "ChestFragment.updateMapProgress",
                        "SKIP finish block | raceBusyLevel==2 handled in fast-forward branch",
                    )
                }
                item.raceBusyLevel != null && item.raceBusyLevel != 1 -> {
                    LessonProgressDiag.log(
                        "ChestFragment.updateMapProgress",
                        "SKIP finish block | raceBusyLevel=${item.raceBusyLevel} (not null, not 1) → stepIsFinish NOT set here",
                    )
                }
                item.raceBusyLevel == 1 -> {
                    LessonProgressDiag.log(
                        "ChestFragment.updateMapProgress",
                        "SKIP finish block | raceBusy still 1 in snapshot after race branch (stale closure?) → stepIsFinish NOT set",
                    )
                }
            }
            if(item.raceBusyLevel == null){
                if(item.stepCount == item.currentStep){
                    LessonProgressDiag.log(
                        "ChestFragment.updateMapProgress",
                        "BRANCH FINISH stepCount==currentStep (${item.stepCount})",
                    )
                    val baseForChest = if (item.type == LessonItem.TYPE_CHEST) {
                        LessonItem.mergeChestRun(item, recordScore, SeasonClock.currentSeason())
                    } else {
                        item
                    }
                    val updatedItem = if (baseForChest.type == LessonItem.TYPE_CHEST) {
                        baseForChest.copy(
                            stepCompletionStatus = newStepCompletionStatus,
                            stepIsFinish = true,
                            stepCupIcon = icon,
                        )
                    } else {
                        baseForChest.copy(
                            stepCompletionStatus = newStepCompletionStatus,
                            stepIsFinish = true,
                        )
                    }
                    if (item.type == LessonItem.TYPE_LESSON && !item.stepIsFinish && updatedItem.stepIsFinish) {
                        shouldIncrementStepFinishMission = true
                        shouldIncrementStepCountMission = true
                        if (lessonSuccessRate >= 100f) {
                            shouldIncrementPerfectStepCountMission = true
                        }
                    }
                    val beforeFilled = item.stepCompletionStatus.count { it }
                    val afterFilled = updatedItem.stepCompletionStatus.count { it }
                    if (afterFilled > beforeFilled) {
                        val key = lessonProgressKey(item, mapFragmentStepIndex)
                        GlobalValues.pendingLessonProgressAnimations[key] =
                            GlobalValues.PendingLessonProgressAnimation(
                                fromFilledSegments = beforeFilled,
                                toFilledSegments = afterFilled,
                            )
                        GlobalValues.canConsumePendingLessonProgressAnimations = false
                    }
                    LessonManager.updateLessonItem(requireContext(),mapFragmentStepIndex, updatedItem)
                    LessonProgressDiag.logItem(
                        "ChestFragment.updateMapProgress",
                        globalPartId,
                        mapFragmentStepIndex,
                        LessonManager.getLessonItem(mapFragmentStepIndex),
                        "AFTER_FINISH_BRANCH",
                    )

                    lessonItem2?.let { item2 ->
                        val updatedItem2 = item2.copy(
                            isCompleted = true
                        )
                        LessonManager.updateLessonItem(requireContext(),index, updatedItem2)
                    }
                }
                else{
                    LessonProgressDiag.log(
                        "ChestFragment.updateMapProgress",
                        "BRANCH INTERMEDIATE currentStep ${item.currentStep}→${item.currentStep + 1} (stepIsFinish stays false)",
                    )
                    val updatedItem = item.copy(
                        stepCompletionStatus = newStepCompletionStatus,
                        currentStep = item.currentStep + 1,
                        startStepNumber = item.startStepNumber?.plus(1)
                    )
                    if (item.type == LessonItem.TYPE_LESSON && updatedItem.currentStep == item.currentStep + 1) {
                        shouldIncrementStepCountMission = true
                        if (lessonSuccessRate >= 100f) {
                            shouldIncrementPerfectStepCountMission = true
                        }
                    }
                    val beforeFilled = item.stepCompletionStatus.count { it }
                    val afterFilled = updatedItem.stepCompletionStatus.count { it }
                    if (afterFilled > beforeFilled) {
                        val key = lessonProgressKey(item, mapFragmentStepIndex)
                        GlobalValues.pendingLessonProgressAnimations[key] =
                            GlobalValues.PendingLessonProgressAnimation(
                                fromFilledSegments = beforeFilled,
                                toFilledSegments = afterFilled,
                            )
                        GlobalValues.canConsumePendingLessonProgressAnimations = false
                    }
                    LessonManager.updateLessonItem(requireContext(),mapFragmentStepIndex, updatedItem)
                    LessonProgressDiag.logItem(
                        "ChestFragment.updateMapProgress",
                        globalPartId,
                        mapFragmentStepIndex,
                        LessonManager.getLessonItem(mapFragmentStepIndex),
                        "AFTER_INTERMEDIATE_BRANCH",
                    )
                }
            } else {
                LessonProgressDiag.log(
                    "ChestFragment.updateMapProgress",
                    "SKIP all progress | raceBusyLevel=${item.raceBusyLevel} (finish block guard failed)",
                )
            }
        } ?: LessonProgressDiag.log(
            "ChestFragment.updateMapProgress",
            "SKIP lessonItem null at mapIdx=$mapFragmentStepIndex",
        )

        if (shouldIncrementStepFinishMission) {
            MissionsProgressStore.recordStepFinishProgress(requireContext())
        }
        if (shouldIncrementStepCountMission) {
            MissionsProgressStore.recordStepIncrementProgress(requireContext())
        }
        if (shouldIncrementPerfectStepCountMission) {
            MissionsProgressStore.recordPerfectStepIncrementProgress(requireContext())
        }
        LessonProgressDiag.log(
            "ChestFragment.updateMapProgress",
            "EXIT missionFlags finish=$shouldIncrementStepFinishMission incr=$shouldIncrementStepCountMission " +
                "perfect=$shouldIncrementPerfectStepCountMission rocketIncr=$shouldIncrementStepCountMission",
        )
        return shouldIncrementStepCountMission
    }
    
    /**
     * Sıradaki yarış dersini açar. İleri sar ile tamamlanmış (0) item'leri atlar;
     * ilk kilitli (2) item'i BAŞLA (1) yapar. Zaten 1 olan item görülürse durur.
     */
    private fun unlockNextRaceItemSkippingCompleted(completedIndex: Int) {
        val items = GlobalLessonData.lessonItems
        for (j in (completedIndex + 1) until items.size) {
            val next = items[j]
            when (next.raceBusyLevel) {
                0 -> continue
                2 -> {
                    LessonManager.updateRaceItem(requireContext(), j, next.copy(raceBusyLevel = 1))
                    LessonProgressDiag.log(
                        "ChestFragment.updateMapProgress",
                        "unlockNextRace: idx=$j KİLİTLİ→BAŞLA (skipped completed after idx=$completedIndex)",
                    )
                    return
                }
                1 -> {
                    LessonProgressDiag.log(
                        "ChestFragment.updateMapProgress",
                        "unlockNextRace: stop at idx=$j already BAŞLA",
                    )
                    return
                }
                else -> continue
            }
        }
        LessonProgressDiag.log(
            "ChestFragment.updateMapProgress",
            "unlockNextRace: no locked item found after idx=$completedIndex",
        )
    }

    private fun isRacePanelOpen(): Boolean {
        // Race panel açık mı kontrol et
        val activity = requireActivity()
        if (activity is MainActivity) {
            val coordinatorLayout = activity.findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.coordinator_layout)
            return coordinatorLayout?.findViewWithTag<View>("race_panel") != null
        }
        return false
    }
    
    private fun notifyRaceAdapterRefresh() {
        // MainActivity'deki LessonAdapter'ı bul ve race panelini yenile
        val activity = requireActivity()
        if (activity is MainActivity) {
            //activity.refreshRacePanel()
        }
    }

    override fun onDestroyView() {
        restoreChestOverlayElevation()
        MainActivityChromeBlocker.release(activity)
        isVideoFlowOpen = false
        _binding = null
        super.onDestroyView()
    }
}
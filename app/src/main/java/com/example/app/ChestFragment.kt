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
    private var goldAmount: Int = 0
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            updateMapProgress()
            GlobalValues.canConsumePendingLessonProgressAnimations = true
            LessonManager.refreshLessonsFromGlobalData()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_left
                )
                .remove(this@ChestFragment)
                .commit()
            goldUpdateListener?.onGoldUpdated(goldAmount)
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
        goldAmount = if (currentReward.type == ChestRewardType.GOLD) currentReward.amount else 0
        applyRewardUiState(currentReward)
        prepareHiddenRewardUi()
        setupClaimRewardButton()
        showCrystalBreakAtStart()
    }


    private fun setupClaimRewardButton() {
        binding.claimRewardButton.setOnClickListener {
            // Tutorial 1'de bu açılışta sadece 1 kez: login start ekranına yönlendir (aynı açılışta tekrar gelmesin)
            val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            if (GlobalValues.currentTutorialNumber == 1) {
                prefs.edit().putBoolean("first_tutorial_shown", true).apply()
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
                updateMapProgress()
                val afterSnap = MissionsProgressStore.getSnapshot(requireContext())

                val fm = parentFragmentManager
                // ChestFragment bazen resultFragmentContainer'da (LessonResult), bazen abacusFragmentContainer'da (BlindingLesson).
                // Görev ekranı yanlış container'a konursa altta kalır; her zaman sandık ile aynı host'ta replace et.
                val hostContainerId = (requireView().parent as View).id
                if (hostContainerId == View.NO_ID) {
                    throw IllegalStateException("ChestFragment host container id yok")
                }
                if (MissionsProgressStore.hasVisibleMissionProgress(requireContext(), beforeSnap, afterSnap)) {
                    fm.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_left,
                            R.anim.slide_out_left,
                        )
                        .replace(
                            hostContainerId,
                            MissionChestRewardFragment.newInstance(beforeSnap, afterSnap),
                        )
                        .commitNowAllowingStateLoss()
                } else {
                    GlobalValues.canConsumePendingLessonProgressAnimations = true
                    LessonManager.refreshLessonsFromGlobalData()
                    fm.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_left,
                            R.anim.slide_out_left,
                        )
                        .remove(this@ChestFragment)
                        .commitNowAllowingStateLoss()
                }
                goldUpdateListener?.onGoldUpdated(goldAmount)
            } catch (e: IllegalStateException) {
                Log.e("ChestFragment", "Ödül fragment işlemi başarısız", e)
                claimRewardInProgress = false
                if (isAdded && _binding != null) {
                    binding.claimRewardButton.isEnabled = true
                }
            }
        }
    }

    private fun prepareHiddenRewardUi() {
        binding.chestImage.setImageResource(R.drawable.open_chest)
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

    private fun updateMapProgress() {
        val lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex) //Global verilerden tıklanan indeksteki adım öğesini alıyor
        val lessonItem2 = LessonManager.getLessonItem(mapFragmentStepIndex+1)
        var shouldIncrementStepFinishMission = false
        var shouldIncrementStepCountMission = false
        var shouldIncrementPerfectStepCountMission = false
        lessonItem?.let { item ->
            // İlk adım true, diğerleri false olacak şekilde stepCompletionStatus oluştur
            val newStepCompletionStatus = List(item.stepCount) { index -> index < item.currentStep }

            if(item.raceBusyLevel == 1){
                val updatedItem = item.copy(
                    raceBusyLevel = 0
                )
                LessonManager.updateRaceItem(requireContext(),mapFragmentStepIndex, updatedItem)
                
                // Güncelleme sonrası yeni değeri al
                val updatedLessonItem = LessonManager.getLessonItem(mapFragmentStepIndex)
                Log.d("senko", "Güncellenmiş: ${updatedLessonItem?.raceBusyLevel}")
                Log.d("senko", "Güncellenmiş: ${updatedLessonItem?.title}")
                Log.d("senko", globalPartId.toString())

                lessonItem2?.let { item2 ->
                    val updatedItem2 = item2.copy(
                        raceBusyLevel = 1
                    )
                    LessonManager.updateRaceItem(requireContext(),mapFragmentStepIndex+1, updatedItem2)
                    
                    // Güncelleme sonrası yeni değeri al
                    val updatedLessonItem2 = LessonManager.getLessonItem(mapFragmentStepIndex+1)
                    Log.d("ukucc", "Güncellenmiş 2: ${updatedLessonItem2?.raceBusyLevel}")
                    Log.d("ukucc", "Güncellenmiş 2: ${updatedLessonItem2?.title}")

                }


                // UI'ı yenile - RaceAdapter'ı güncelle (sadece race panel açıksa)
                if (isRacePanelOpen()) {
                    //notifyRaceAdapterRefresh()
                }
            }

            if(item.raceBusyLevel == null){
                if(item.stepCount == item.currentStep){
                    var updatedItem = item.copy(
                        stepCompletionStatus = newStepCompletionStatus,
                        stepIsFinish = true
                    )
                    if(lessonItem.type==LessonItem.TYPE_CHEST){
                         updatedItem = item.copy(
                            stepCompletionStatus = newStepCompletionStatus,
                            stepIsFinish = true,
                            stepCupIcon = icon
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

                    lessonItem2?.let { item2 ->
                        val updatedItem2 = item2.copy(
                            isCompleted = true
                        )
                        LessonManager.updateLessonItem(requireContext(),mapFragmentStepIndex+1, updatedItem2)
                    }
                }
                else{
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
                }
            }
        }

        if (shouldIncrementStepFinishMission) {
            MissionsProgressStore.recordStepFinishProgress(requireContext())
        }
        if (shouldIncrementStepCountMission) {
            MissionsProgressStore.recordStepIncrementProgress(requireContext())
        }
        if (shouldIncrementPerfectStepCountMission) {
            MissionsProgressStore.recordPerfectStepIncrementProgress(requireContext())
        }
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
        MainActivityChromeBlocker.release(activity)
        isVideoFlowOpen = false
        _binding = null
        super.onDestroyView()
    }
}
package com.example.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.app.databinding.FragmentChestBinding

/**
 * Günlük soru doğru tamamlandığında sadece ödül + görev ilerleme akışını yürütür.
 * Lesson progress güncellemez.
 */
class DailyQuestionRewardFragment : Fragment() {

    private var _binding: FragmentChestBinding? = null
    private val binding get() = _binding!!

    private var isVideoFlowOpen = false
    private var isChestRevealReady = false
    private var claimRewardInProgress = false
    private var selectedVideoName: String = "crystal_red_yellow"
    private var rewardOutcome: ChestRewardOutcome = ChestRewardOutcome(
        type = ChestRewardType.GOLD,
        amount = 0,
        iconRes = R.drawable.open_chest,
        label = "0 altın",
    )
    private var goldUpdateListener: GoldUpdateListener? = null
    private var badgePayloadQueue: ArrayList<String> = arrayListOf()
    private var dailyQuestionPeriodKey: String = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is GoldUpdateListener) {
            goldUpdateListener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        badgePayloadQueue = arguments?.getStringArrayList(ARG_BADGE_PAYLOAD_QUEUE) ?: arrayListOf()
        dailyQuestionPeriodKey = arguments?.getString(ARG_PERIOD_KEY).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MainActivityChromeBlocker.acquire(requireActivity())
        selectedVideoName = ChestCrystalPolicy.resolveVideoName()
        rewardOutcome = ChestCrystalPolicy.resolveRewardForVideo(selectedVideoName)
        prepareHiddenRewardUi()
        applyRewardUiState(rewardOutcome)
        setupClaimRewardButton()
        showCrystalBreakAtStart()
    }

    private fun setupClaimRewardButton() {
        binding.claimRewardButton.setOnClickListener {
            if (claimRewardInProgress) return@setOnClickListener
            claimRewardInProgress = true
            binding.claimRewardButton.isEnabled = false
            try {
                ChestRewardClaimHelper.applyReward(goldUpdateListener, rewardOutcome)
                if (dailyQuestionPeriodKey.isNotEmpty()) {
                    DailyQuestionRepository.markRewardClaimed(
                        requireContext(),
                        dailyQuestionPeriodKey,
                    ) { _ -> }
                }
                val openBadgeAfter = badgePayloadQueue.isNotEmpty()
                val queueCopy = ArrayList(badgePayloadQueue)
                val activityFm = requireActivity().supportFragmentManager
                val main = activity as? MainActivity
                if (main != null) {
                    main.finishOverlayReturnToTasks("dailyReward.claim")
                } else {
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_right,
                        )
                        .remove(this@DailyQuestionRewardFragment)
                        .commitNowAllowingStateLoss()
                    parentFragmentManager.popBackStack()
                }
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
            } catch (e: IllegalStateException) {
                Log.e("DailyQuestionReward", "Odul akisinda hata", e)
                claimRewardInProgress = false
                if (isAdded && _binding != null) {
                    binding.claimRewardButton.isEnabled = true
                }
            }
        }
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
        applyRewardUiState(rewardOutcome)
        binding.chestImage.visibility = View.VISIBLE
        binding.goldText.visibility = View.VISIBLE
        binding.claimRewardButton.visibility = View.VISIBLE
    }

    private fun applyRewardUiState(reward: ChestRewardOutcome) {
        binding.chestImage.setImageResource(reward.iconRes)
        binding.goldText.text = reward.label
    }

    override fun onDestroyView() {
        MainActivityChromeBlocker.release(activity)
        isVideoFlowOpen = false
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_BADGE_PAYLOAD_QUEUE = "badge_payload_queue"
        private const val ARG_PERIOD_KEY = "daily_period_key"

        fun newInstance(
            badgePayloadQueue: List<String> = emptyList(),
            periodKey: String,
        ): DailyQuestionRewardFragment {
            return DailyQuestionRewardFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_BADGE_PAYLOAD_QUEUE, ArrayList(badgePayloadQueue))
                    putString(ARG_PERIOD_KEY, periodKey)
                }
            }
        }
    }
}

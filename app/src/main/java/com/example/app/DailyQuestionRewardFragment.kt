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
    private var goldAmount: Int = 0
    private var goldUpdateListener: GoldUpdateListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is GoldUpdateListener) {
            goldUpdateListener = context
        }
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
        goldAmount = if (rewardOutcome.type == ChestRewardType.GOLD) rewardOutcome.amount else 0
        applyRewardUiState(rewardOutcome)
        prepareHiddenRewardUi()
        setupClaimRewardButton()
        showCrystalBreakAtStart()
    }

    private fun setupClaimRewardButton() {
        binding.claimRewardButton.setOnClickListener {
            if (claimRewardInProgress) return@setOnClickListener
            claimRewardInProgress = true
            binding.claimRewardButton.isEnabled = false
            try {
                goldUpdateListener?.onGoldUpdated(goldAmount)
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_right,
                    )
                    .remove(this@DailyQuestionRewardFragment)
                    .commitNowAllowingStateLoss()
                parentFragmentManager.popBackStack()
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

    override fun onDestroyView() {
        MainActivityChromeBlocker.release(activity)
        isVideoFlowOpen = false
        _binding = null
        super.onDestroyView()
    }
}

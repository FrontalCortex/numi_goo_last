package com.example.app

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.app.databinding.FragmentChestBinding

class MissionRewardRevealDialogFragment : DialogFragment() {

    private var _binding: FragmentChestBinding? = null
    private val binding get() = _binding!!

    private var isVideoFlowOpen = false
    private var isRewardReady = false
    private var selectedVideoName: String = "crystal_red_yellow"
    private var goldUpdateListener: GoldUpdateListener? = null
    private var rewardOutcome: ChestRewardOutcome = ChestRewardOutcome(
        type = ChestRewardType.GOLD,
        amount = 0,
        iconRes = R.drawable.open_chest,
        label = "0 altın",
    )
    private var onDismissCallback: (() -> Unit)? = null
    private var onRewardClaimedCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        isCancelable = false
    }

    override fun onAttach(context: android.content.Context) {
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
        selectedVideoName = ChestCrystalPolicy.resolveVideoName()
        rewardOutcome = ChestCrystalPolicy.resolveRewardForVideo(selectedVideoName)
        applyRewardUiState()
        prepareHiddenRewardUi()
        binding.claimRewardButton.setOnClickListener {
            if (rewardOutcome.type == ChestRewardType.GOLD) {
                goldUpdateListener?.onGoldUpdated(rewardOutcome.amount)
            }
            onRewardClaimedCallback?.invoke()
            dismissAllowingStateLoss()
        }
        showCrystalBreakAtStart()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(false)
            setOnKeyListener { _, keyCode, event ->
                keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    fun setOnDismissCallback(callback: () -> Unit) {
        onDismissCallback = callback
    }

    fun setOnRewardClaimedCallback(callback: () -> Unit) {
        onRewardClaimedCallback = callback
    }

    private fun prepareHiddenRewardUi() {
        binding.chestImage.visibility = View.INVISIBLE
        binding.goldText.visibility = View.GONE
        binding.claimRewardButton.visibility = View.GONE
    }

    private fun applyRewardUiState() {
        binding.chestImage.setImageResource(rewardOutcome.iconRes)
        binding.goldText.text = rewardOutcome.label
    }

    private fun showCrystalBreakAtStart() {
        if (isRewardReady || isVideoFlowOpen || !isAdded) return
        val tag = CrystalBreakVideoFragment::class.java.simpleName
        if (childFragmentManager.findFragmentByTag(tag) != null) return

        isVideoFlowOpen = true
        CrystalBreakVideoFragment.newInstance(selectedVideoName).show(childFragmentManager, tag)
        childFragmentManager.executePendingTransactions()
        (childFragmentManager.findFragmentByTag(tag) as? CrystalBreakVideoFragment)
            ?.setOnDismissCallback {
                isVideoFlowOpen = false
                if (!isAdded || _binding == null) return@setOnDismissCallback
                revealRewardUi()
            }
    }

    private fun revealRewardUi() {
        if (isRewardReady) return
        isRewardReady = true
        binding.chestImage.visibility = View.VISIBLE
        binding.goldText.visibility = View.VISIBLE
        binding.claimRewardButton.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        onDismissCallback?.invoke()
        onDismissCallback = null
        onRewardClaimedCallback = null
        super.onDestroy()
    }

    override fun onDestroyView() {
        isVideoFlowOpen = false
        _binding = null
        super.onDestroyView()
    }

    override fun onDetach() {
        goldUpdateListener = null
        super.onDetach()
    }
}

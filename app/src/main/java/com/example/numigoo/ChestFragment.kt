package com.example.numigoo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.numigoo.databinding.FragmentChestBinding

class ChestFragment : Fragment() {
    private var isOpened = false
    private var pulseAnimatorSet: AnimatorSet? = null
    private var _binding: FragmentChestBinding? = null
    private val binding get() = _binding!!
    private var successRate: Float = 0F
    private var goldAmount: Int = 0
    private var goldUpdateListener: GoldUpdateListener? = null

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

        Log.d("Pepsi", "onViewCreated başladı")
        
        arguments?.let { bundle ->
            successRate = bundle.getFloat("successRate", 0F)
        }

        goldValueAlgorithm()
        startPulseAnimation(binding.chestImage)

        binding.chestImage.setOnClickListener {
            if (!isOpened) {
                isOpened = true
                pulseAnimatorSet?.cancel()
                binding.flashView.visibility = View.VISIBLE
                val flashAnim = ObjectAnimator.ofFloat(binding.flashView, "alpha", 0f, 1f, 0f)
                flashAnim.duration = 500
                flashAnim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.flashView.visibility = View.GONE
                        binding.chestImage.setImageResource(R.drawable.open_chest)
                        binding.goldText.visibility = View.VISIBLE
                        binding.claimRewardButton.visibility = View.VISIBLE
                    }
                })
                flashAnim.start()
            }
        }

        binding.claimRewardButton.setOnClickListener {
            goldUpdateListener?.onGoldUpdated(goldAmount)
            parentFragmentManager.popBackStack()
        }
    }

    private fun startPulseAnimation(view: View) {
        val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.08f)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.08f)
        val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1.08f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.08f, 1f)

        scaleUpX.duration = 400
        scaleUpY.duration = 400
        scaleDownX.duration = 400
        scaleDownY.duration = 400

        pulseAnimatorSet = AnimatorSet()
        pulseAnimatorSet?.play(scaleUpX)?.with(scaleUpY)
        pulseAnimatorSet?.play(scaleDownX)?.with(scaleDownY)?.after(scaleUpX)

        pulseAnimatorSet?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!isOpened) pulseAnimatorSet?.start()
            }
        })
        pulseAnimatorSet?.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pulseAnimatorSet?.cancel()
        _binding = null
    }

    private fun goldValueAlgorithm(){
        goldAmount = when (successRate) {
            100F -> {
                // %90 ihtimalle 100-200, %10 ihtimalle 500
                if ((0..9).random() == 0) 500 else (100..200).random()
            }
            in 76F..99F -> {
                // 50-100 arası
                (50..100).random()
            }
            in 51F..75F -> {
                // 25-50 arası
                (25..50).random()
            }
            else -> {
                // 10-25 arası
                (10..25).random()
            }
        }

        // Sonucu ekrana yaz
        binding.goldText.text = "$goldAmount altın"
    }
}
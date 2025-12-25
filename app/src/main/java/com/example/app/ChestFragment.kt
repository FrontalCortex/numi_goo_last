package com.example.app

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
import com.example.app.databinding.FragmentChestBinding
import com.example.app.GlobalLessonData.globalPartId
import com.example.app.GlobalValues.mapFragmentStepIndex

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

        setupClaimRewardButton()
    }

    private fun setupClaimRewardButton() {
        binding.claimRewardButton.setOnClickListener {
            // Önce map progress'i güncelle
            updateMapProgress()
            
            // Abacus container'daki fragment'ı kaldır
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,  // Giriş animasyonu
                    R.anim.slide_out_left // Çıkış animasyonu
                )
                .remove(this@ChestFragment)
                .commit()
                
            // Gold miktarını güncelle
            goldUpdateListener?.onGoldUpdated(goldAmount)
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

    private fun updateMapProgress() {
        val lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex) //Global verilerden tıklanan indeksteki adım öğesini alıyor
        val lessonItem2 = LessonManager.getLessonItem(mapFragmentStepIndex+1)
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
                    val updatedItem = item.copy(
                        stepCompletionStatus = newStepCompletionStatus,
                        stepIsFinish = true
                    )
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
                    LessonManager.updateLessonItem(requireContext(),mapFragmentStepIndex, updatedItem)
                }
            }
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
}
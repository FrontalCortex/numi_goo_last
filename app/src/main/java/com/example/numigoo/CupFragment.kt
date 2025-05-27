package com.example.numigoo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.CountDownTimer
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.example.numigoo.GlobalValues.mapFragmentStepIndex
import com.example.numigoo.databinding.FragmentCupBinding
import com.example.numigoo.model.LessonItem
import com.example.numigoo.model.LessonViewModel

class CupFragment : Fragment() {
    private lateinit var binding: FragmentCupBinding
    private var targetTime: String = ""
    private var currentSeconds: Int = 0
    private var timer: CountDownTimer? = null
    private val TICK_INTERVAL = 50L // 50 milisaniye
    private val SPEED_MULTIPLIER = 1 // Her tick'te 3 saniye artacak
    private lateinit var claimButton: View
    private lateinit var cupImageView: ImageView
    private lateinit var cupName: TextView
    private lateinit var motivationText: TextView
    private lateinit var lessonItem : LessonItem

    private var icon: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex)!!
        cupImageView = binding.cupImageView
        cupName = binding.cupNameTextView
        motivationText = binding.motivationTextView
        claimButton = binding.claimButton
        arguments?.let { bundle ->
            targetTime = bundle.getString("time", "")
            startTimer()
        }
        ChangeCupIcon()
        resumeClick()

    }

    private fun resumeClick(){
        binding.claimButton.setOnClickListener {
            // Önce MapFragment'ı aç
            updateMapProgress()

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerID, MapFragment())
                .remove(this@CupFragment)
                .commit()

            // Sonra kupa ikonunu güncelle ve fragment'ları temizle

        }
    }
    private fun ChangeCupIcon(){
        if (targetTime < lessonItem.cupTime1.toString()) {
            icon= R.drawable.cup_ic3
            cupName.text = "Bilgelik Kupası"
            val cupMotivation = listOf("Tüm sorular senin zekânla boy ölçüştü, ama kazanan sensin",
                "Zirve senin, bu başarı tesadüf değil!", "Mükemmel bir iş çıkardın!",
                "Bu seviye, sadece çok çalışanların ulaşabildiği yer.",
                "Sen artık bu konunun ustasısın – neyi başaramazsın ki?",)
            motivationText.text = cupMotivation.random()
        } else if (targetTime < lessonItem.cupTime2.toString()) {
            icon = R.drawable.cup_ic2
            cupName.text = "Usta Kupası"
            val cupMotivation = listOf("Gerçek ustalık; bilgi, sabır ve kararlılıkla gelir. Tebrikler!",
                "Gayet iyi!", "Biraz daha pratikle zirveye ulaşman an meselesi.",
                "Azmin gücüyle her adım seni daha da ileri taşıyor!",
                "Çok iyi gidiyorsun! Birkaç adım sonra zirvedesin.",
                "Gelişimin fark ediliyor. Pes etme, devam et!")
            motivationText.text = cupMotivation.random()
        } else if ( targetTime < "10:00") {
            icon = R.drawable.cup_ic
            cupName.text = "Deneyim Kupası"
            val cupMotivation = listOf("Her usta bir zamanlar amatördü. Sen de yoldasın!",
                "Cesaret ettin, başladın. Bu en önemli adımdı.",
                "Unutma, başarı tekrar tekrar denemekten geçer!",
                "Bugün attığın adım, yarının başarısını başlatır.")
            motivationText.text = cupMotivation.random()
        }else{
            icon = R.drawable.cup_ic
            cupName.text = "Deneyim Kupası"
            val cupMotivation = listOf("Ayıp olmasın diye bu kupayı verdik.",
                "Biraz fazla mı yavaş oldu ne ?",
                "Yanii.. Hiç yapmamış olmaktan iyi.",
                "En azından ekranı açtın, bu da bir şey.",
                "Kupayı biz de pek vermek istemedik ama...",
                "Kupa geldi ama vicdanımız biraz sızladı.")
            motivationText.text = cupMotivation.random()
        }
        cupImageView.setImageResource(icon)
    }
    private fun updateMapProgress() {
        val lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex) //Global verilerden tıklanan indeksteki adım öğesini alıyor
        val lessonItem2 = LessonManager.getLessonItem(mapFragmentStepIndex +1)
        lessonItem?.let { item ->
            // İlk adım true, diğerleri false olacak şekilde stepCompletionStatus oluştur
            val newStepCompletionStatus = List(item.stepCount) { index -> index < item.currentStep }

            if(item.stepCount == item.currentStep){
                val updatedItem = item.copy(
                    stepCompletionStatus = newStepCompletionStatus,
                    stepIsFinish = true,
                    stepCupIcon = icon
                )
                (requireActivity() as? FragmentActivity)?.let { activity ->
                    val viewModel: LessonViewModel by activity.viewModels()
                    viewModel.updateLessonItem(mapFragmentStepIndex, updatedItem)
                }
                lessonItem2?.let { item2 ->
                    val updatedItem2 = item2.copy(
                        isCompleted = true
                    )
                    (requireActivity() as? FragmentActivity)?.let { activity ->
                        val viewModel: LessonViewModel by activity.viewModels()
                        viewModel.updateLessonItem(mapFragmentStepIndex+1, updatedItem2)
                    }                }
            }
            else{
                val updatedItem = item.copy(
                    stepCompletionStatus = newStepCompletionStatus,
                    currentStep = item.currentStep + 1,
                    startStepNumber = item.startStepNumber?.plus(1)
                )
                (requireActivity() as? FragmentActivity)?.let { activity ->
                    val viewModel: LessonViewModel by activity.viewModels()
                    viewModel.updateLessonItem(mapFragmentStepIndex, updatedItem)
                }
            }
        }
    }

    private fun startTimer() {
        val (targetMinutes, targetSeconds) = targetTime.split(":").map { it.toInt() }
        val targetTotalSeconds = targetMinutes * 60 + targetSeconds
        
        // Timer'ı ileriye doğru sayacak şekilde ayarla
        timer = object : CountDownTimer(targetTotalSeconds * 1000L, TICK_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsedTime = (targetTotalSeconds * 1000L - millisUntilFinished) / TICK_INTERVAL
                currentSeconds = (elapsedTime * SPEED_MULTIPLIER).toInt().coerceAtMost(targetTotalSeconds)
                updateTimerDisplay()
                
                // Hedef zamana ulaşıldığında animasyonu başlat
                if (currentSeconds >= targetTotalSeconds) {
                    cancel()
                    showCupAnimation()
                }
            }

            override fun onFinish() {
                currentSeconds = targetTotalSeconds
                updateTimerDisplay()
                showCupAnimation()
            }
        }.start()
    }

    private fun updateTimerDisplay() {
        val minutes = currentSeconds / 60
        val seconds = currentSeconds % 60
        binding.timerTextView.text = String.format("%d:%02d", minutes, seconds)

        // Sürekli büyüyüp küçülme animasyonu
        val scaleX = ObjectAnimator.ofFloat(
            binding.timerTextView,
            "scaleX",
            1f,
            1.5f,
            1f
        ).apply {
            duration = 300
            repeatCount = 0
        }

        val scaleY = ObjectAnimator.ofFloat(
            binding.timerTextView,
            "scaleY",
            1f,
            1.5f,
            1f
        ).apply {
            duration = 300
            repeatCount = 0
        }

        val animatorSet = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }

        Log.d("TimerAnimation", "Animation started for time: ${binding.timerTextView.text}")
    }

    private fun showCupAnimation() {
        // Kayma animasyonu
        val translateY = ObjectAnimator.ofFloat(
            binding.timerTextView,
            "translationY",
            0f,
            1000f
        )

        // Küçülme animasyonu
        val scaleX = ObjectAnimator.ofFloat(
            binding.timerTextView,
            "scaleX",
            1f,
            0.5f
        )
        val scaleY = ObjectAnimator.ofFloat(
            binding.timerTextView,
            "scaleY",
            1f,
            0.5f
        )

        // Animasyonları birleştir
        val animatorSet = AnimatorSet().apply {
            playTogether(translateY, scaleX, scaleY)
            duration = 800
        }

        // Animasyon bittiğinde
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                showCup()
            }
        })

        // Animasyonu başlat
        animatorSet.start()
    }

    private fun showCup() {
        binding.cupContainer.visibility = View.VISIBLE
        binding.cupContainer.alpha = 0f
        
        // Alpha animasyonu
        val alphaAnimator = ObjectAnimator.ofFloat(
            binding.cupContainer,
            "alpha",
            0f,
            1f
        ).apply {
            duration = 1000 // 1 saniye sürecek
        }
        
        alphaAnimator.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
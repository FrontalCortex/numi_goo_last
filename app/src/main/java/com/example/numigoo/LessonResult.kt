package com.example.numigoo

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.numigoo.GlobalValues.mapFragmentStepIndex
import com.example.numigoo.databinding.FragmentLessonResultBinding
import com.example.numigoo.databinding.FragmentTasksBinding
import com.example.numigoo.model.LessonItem


class LessonResult : Fragment() {
    private lateinit var binding: FragmentLessonResultBinding
    private val animations = listOf(
        "animation_one.json",
        "animaton_two.json",
        "animaton_three.json",
        "animaton_four.json",
        "animaton_five.json",
        "animaton_six.json",
        "animaton_eight.json",
        "animaton_nine.json",
        "animaton_ten.json",
        "animaton_eleven.json",
        "animaton_twelve.json",
        "animaton_thirteen.json"
    )
    private var currentAnimIndex = 0
    private var correctAnswers: Int = 0
    private var totalQuestions: Int = 0
    private var succsessRate: Float = 0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLessonResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        GlobalValues.lessonStep++
        GlobalValues.tutorialIsWorked=true

// Başka bir sınıfta (örneğin LessonResult'ta)
        arguments?.let { bundle ->
            correctAnswers = bundle.getInt("correctAnswers", 0)
            totalQuestions = bundle.getInt("totalQuestions", 0)
            Log.d("mesi","$correctAnswers, $totalQuestions")
            succsessRate = if (totalQuestions > 0) {
                (correctAnswers.toFloat() / totalQuestions.toFloat()) * 100
            } else {
                0f
            }
            Log.d("mesi","$succsessRate")
            // Bu verileri kullanarak UI'ı güncelle
            updateUI()
        }
        binding.claimButton.setOnClickListener {

            val lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex)
            lessonItem?.tutorialIsFinish = true
            val chestFragment = ChestFragment()
            val args = Bundle().apply {
                putFloat("successRate", succsessRate)
            }
            if(lessonItem?.stepIsFinish==true){
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerID, MapFragment())
                    .remove(this@LessonResult)
                    .commit()
            }else{
                chestFragment.arguments = args
                parentFragmentManager.beginTransaction()
                    .replace(R.id.resultFragmentContainer, chestFragment)
                    .commit()
            }

        }

        // Animasyonları başlat
        showRandomAnimation()


    }


    private fun updateUI() {
        // Örnek: Doğru cevap sayısını göster
        succsessRate = if (totalQuestions > 0) ((correctAnswers.toFloat() / totalQuestions.toFloat()) * 100) else 0f
        binding.successRate.text = "${succsessRate.toInt()}%"

        // Başarı durumuna göre farklı animasyon gösterebilirsiniz
        /*if (correctAnswers >= totalQuestions * 0.8) { // %80 ve üzeri başarı
            binding.lottieView.setAnimation("success.json")
        } else {
            binding.lottieView.setAnimation("try_again.json")
        }
        binding.lottieView.playAnimation()*/
    }

    private fun showRandomAnimation() {
        val randomAnim = animations.random()
        binding.lottieView.setAnimation(randomAnim)
        binding.lottieView.playAnimation()
    }


}
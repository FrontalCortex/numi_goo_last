package com.example.app

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.app.databinding.FragmentChestResultBinding

class ChestResult : Fragment() {
    private lateinit var binding: FragmentChestResultBinding

    private var correctAnswers: Int = 0
    private var totalQuestions: Int = 0
    private var succsessRate: Float = 0F
    private var time:String = ""
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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChestResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let { bundle ->
            correctAnswers = bundle.getInt("correctAnswers", 0)
            totalQuestions = bundle.getInt("totalQuestions", 0)
            time = bundle.getString("time","")
            Log.d("KEKEKE","$time")
            succsessRate = if (totalQuestions > 0) {
                (correctAnswers.toFloat() / totalQuestions.toFloat()) * 100
            } else {
                0f
            }
            // Bu verileri kullanarak UI'ı güncelle
            updateUI()
        }
        showRandomAnimation()
        continueFragment()
    }

    private fun continueFragment(){
        binding.claimButton.setOnClickListener {
            val args = Bundle().apply {
                putString("time",time)
            }
            val cupFragment = CupFragment()
            cupFragment.arguments = args

            parentFragmentManager.beginTransaction()
                .replace(R.id.abacusFragmentContainer, cupFragment)
                .commit()
        }
    }
    private fun updateUI() {
        // Örnek: Doğru cevap sayısını göster
        succsessRate = if (totalQuestions > 0) ((correctAnswers.toFloat() / totalQuestions.toFloat()) * 100) else 0f
        binding.successRate.text = "${succsessRate.toInt()}%"
        binding.totalTime.text = time

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
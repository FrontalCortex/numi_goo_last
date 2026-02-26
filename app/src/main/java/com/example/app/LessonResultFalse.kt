package com.example.app

import android.content.Intent
import android.os.Bundle
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentLessonResultFalseBinding
import com.example.app.GlobalValues
import com.google.firebase.auth.FirebaseAuth

class LessonResultFalse : Fragment() {
    private lateinit var binding: FragmentLessonResultFalseBinding
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
    private var lessonStep: Int = 0

    private lateinit var loginLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerID, MapFragment())
                .remove(this@LessonResultFalse)
                .commit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLessonResultFalseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        binding.claimButton.setOnClickListener {

            // Tutorial 1'de bu açılışta sadece 1 kez: login start ekranına yönlendir (aynı açılışta tekrar gelmesin)
            if (GlobalValues.currentTutorialNumber == 1 && FirebaseAuth.getInstance().currentUser == null) {
                prefs.edit().putBoolean("first_tutorial_shown", true).apply()
                GlobalValues.currentTutorialNumber = 0
                loginLauncher.launch(
                    Intent(requireContext(), LoginStartActivity::class.java)
                        .putExtra(LoginStartActivity.EXTRA_BLOCK_BACK, true)
                )
                return@setOnClickListener
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerID, MapFragment())
                .remove(this@LessonResultFalse)
                .commit()
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
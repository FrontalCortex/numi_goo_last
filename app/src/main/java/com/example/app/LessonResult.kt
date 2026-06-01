package com.example.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.example.app.GlobalValues.mapFragmentStepIndex
import com.example.app.databinding.FragmentLessonResultBinding


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
    private var lessonScore: Int = 0

    /** [abacusFragmentContainer] üzerinde harita dokunuşunu geçirmemek için ([ChestFragment] ile aynı mantık). */
    private var lessonResultHostView: View? = null
    private var lessonResultHostSavedElevationPx = Float.NaN

    private fun elevateLessonResultOverlayAboveMap() {
        val host = binding.root.parent as? View ?: return
        lessonResultHostView = host
        val base = ViewCompat.getElevation(host).let { if (it.isNaN() || it < 0f) 0f else it }
        lessonResultHostSavedElevationPx = base
        val bumpPx = 16f * resources.displayMetrics.density
        ViewCompat.setElevation(host, base + bumpPx)
    }

    private fun restoreLessonResultOverlayElevation() {
        val h = lessonResultHostView ?: return
        if (!lessonResultHostSavedElevationPx.isNaN()) {
            ViewCompat.setElevation(h, lessonResultHostSavedElevationPx)
        }
        lessonResultHostView = null
        lessonResultHostSavedElevationPx = Float.NaN
    }

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
        MainActivityChromeBlocker.acquire(requireActivity())
        GlobalValues.lessonStep++
        GlobalValues.tutorialIsWorked=true

// Başka bir sınıfta (örneğin LessonResult'ta)
        arguments?.let { bundle ->
            correctAnswers = bundle.getInt("correctAnswers", 0)
            totalQuestions = bundle.getInt("totalQuestions", 0)
            succsessRate = if (totalQuestions > 0) {
                (correctAnswers.toFloat() / totalQuestions.toFloat()) * 100
            } else {
                0f
            }
            // Bu verileri kullanarak UI'ı güncelle
            updateUI()
        }
        binding.claimButton.setOnClickListener {

            val lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex)
            lessonItem?.tutorialIsFinish = true
            val chestFragment = ChestFragment()
            val args = Bundle().apply {
                putFloat("successRate", succsessRate)
                putInt("dersPuani", lessonScore)
            }
            if (lessonItem?.stepIsFinish == true) {
                val main = activity as? MainActivity
                if (isAdded) {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .remove(this@LessonResult)
                        .commitNowAllowingStateLoss()
                }
                main?.prepareMapReturnAfterLessonClaim()
                main?.finalizeMapReturnAfterLessonClaim("LessonResult.claimStepFinish")
            } else {
                chestFragment.arguments = args
                (activity as? MainActivity)?.showResultOverlayHost()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.resultFragmentContainer, chestFragment)
                    .remove(this@LessonResult)
                    .commitNowAllowingStateLoss()
            }

        }

        // Animasyonları başlat
        showRandomAnimation()

        binding.root.post { elevateLessonResultOverlayAboveMap() }
    }

    override fun onDestroyView() {
        restoreLessonResultOverlayElevation()
        MainActivityChromeBlocker.release(activity)
        super.onDestroyView()
    }

    private fun updateUI() {
        // Örnek: Doğru cevap sayısını göster
        succsessRate = if (totalQuestions > 0) ((correctAnswers.toFloat() / totalQuestions.toFloat()) * 100) else 0f
        lessonScore = (succsessRate * 5f).toInt()
        binding.successRate.text = "${succsessRate.toInt()}%"
        binding.totalScore.text = lessonScore.toString()

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
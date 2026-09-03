package com.example.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.example.app.GlobalValues.mapFragmentStepIndex
import com.example.app.databinding.FragmentLessonResultBinding
import com.example.app.model.LessonItem


class LessonResult : Fragment() {
    private var _binding: FragmentLessonResultBinding? = null
    private val binding get() = _binding!!
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
    private var questionElapsedMs: Long = -1L

    private val revealHandler = Handler(Looper.getMainLooper())
    private var activeCountAnimator: ValueAnimator? = null

    private companion object {
        const val BOX_APPEAR_DURATION_MS = 350L
        const val COUNT_UP_DURATION_MS = 1000L
        const val CLAIM_BUTTON_FADE_MS = 300L
        const val STEP_PAUSE_MS = 100L
        const val EXIT_ANIM_DURATION_MS = 300L
    }

    // --- Teşhis: fragment_lesson_result'tan beklenmedik şekilde MapFragment'e dönme sorunu ---
    private var viewCreatedAtMs: Long = 0L
    private var claimHandled = false

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
        _binding = FragmentLessonResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MainActivityChromeBlocker.acquire(requireActivity())
        LessonResultSoundPlayer.play(requireContext())
        GlobalValues.lessonStep++
        GlobalValues.tutorialIsWorked=true

        viewCreatedAtMs = System.currentTimeMillis()
        LessonProgressDiag.logItem(
            "LessonResult.onViewCreated",
            GlobalLessonData.globalPartId,
            mapFragmentStepIndex,
            LessonManager.getLessonItem(mapFragmentStepIndex),
            label = "ENTER",
        )

// Başka bir sınıfta (örneğin LessonResult'ta)
        arguments?.let { bundle ->
            correctAnswers = bundle.getInt("correctAnswers", 0)
            totalQuestions = bundle.getInt("totalQuestions", 0)
            questionElapsedMs = bundle.getLong("questionElapsedMs", -1L)
            succsessRate = if (totalQuestions > 0) {
                (correctAnswers.toFloat() / totalQuestions.toFloat()) * 100
            } else {
                0f
            }
            // Bu verileri kullanarak UI'ı güncelle
            updateUI()
        }
        binding.claimButton.setOnClickListener {
            val clickAtMs = System.currentTimeMillis()
            if (claimHandled) {
                LessonProgressDiag.log(
                    "LessonResult.claimButton",
                    "IGNORED duplicate/rapid click | msSinceViewCreated=${clickAtMs - viewCreatedAtMs} " +
                        "isAdded=$isAdded mapIdx=$mapFragmentStepIndex part=${GlobalLessonData.globalPartId}",
                )
                return@setOnClickListener
            }
            claimHandled = true

            val lessonItem = LessonManager.getLessonItem(mapFragmentStepIndex)
            LessonProgressDiag.logItem(
                "LessonResult.claimButton",
                GlobalLessonData.globalPartId,
                mapFragmentStepIndex,
                lessonItem,
                label = "CLICK msSinceViewCreated=${clickAtMs - viewCreatedAtMs}",
            )
            lessonItem?.tutorialIsFinish = true
            val chestFragment = ChestFragment()
            val args = Bundle().apply {
                putFloat("successRate", succsessRate)
                putInt("dersPuani", lessonScore)
                putLong("questionElapsedMs", questionElapsedMs)
            }
            if (lessonItem?.stepIsFinish == true) {
                LessonProgressDiag.log(
                    "LessonResult.claimButton",
                    "BRANCH=SKIP_TO_MAP (stepIsFinish already true, ChestFragment atlanıyor) " +
                        "mapIdx=$mapFragmentStepIndex part=${GlobalLessonData.globalPartId} " +
                        "currentStep=${lessonItem.currentStep}/${lessonItem.stepCount}",
                )
                if (lessonItem.type == LessonItem.TYPE_LESSON || lessonItem.type == LessonItem.TYPE_CHEST) {
                    LessonSuccessRateRepository.recordItemReplay(GlobalLessonData.globalPartId, mapFragmentStepIndex)
                }
                val main = activity as? MainActivity
                // FragmentTransaction'ın setCustomAnimations'ı burada güvenilir oynamıyor
                // (resultFragmentContainer prepareMapReturnAfterLessonClaim içinde GONE'a alınıyor);
                // bu yüzden view'ı elle kaydırıp animasyon bitince kaldırıyoruz.
                var mapReturnHandled = false
                val completeMapReturn: () -> Unit = {
                    if (!mapReturnHandled) {
                        mapReturnHandled = true
                        if (isAdded) {
                            requireActivity().supportFragmentManager.beginTransaction()
                                .remove(this@LessonResult)
                                .commitNowAllowingStateLoss()
                        }
                        main?.prepareMapReturnAfterLessonClaim()
                        main?.finalizeMapReturnAfterLessonClaim("LessonResult.claimStepFinish")
                    }
                }
                val rootView = binding.root
                rootView.animate()
                    .translationX(rootView.width.toFloat())
                    .setDuration(EXIT_ANIM_DURATION_MS)
                    .withEndAction { completeMapReturn() }
                    .start()
                // Animasyon kesintiye uğrar da withEndAction hiç çalışmazsa (arka plana alma,
                // view'ın penceresinden kopması vb.) haritaya dönüşü garantiye alan yedek.
                Handler(Looper.getMainLooper()).postDelayed({ completeMapReturn() }, EXIT_ANIM_DURATION_MS + 300L)
            } else {
                LessonProgressDiag.log(
                    "LessonResult.claimButton",
                    "BRANCH=GO_TO_CHEST mapIdx=$mapFragmentStepIndex part=${GlobalLessonData.globalPartId} " +
                        "currentStep=${lessonItem?.currentStep}/${lessonItem?.stepCount}",
                )
                chestFragment.arguments = args
                (activity as? MainActivity)?.showResultOverlayHost()
                // ChestFragment kendi ekranını göstermiyor (bkz. ChestFragment), bu yüzden onun
                // girişine animasyon vermiyoruz; sadece LessonResult sağa kayarak kapanıyor.
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(0, R.anim.slide_out_right)
                    .replace(R.id.resultFragmentContainer, chestFragment)
                    .remove(this@LessonResult)
                    .commitNowAllowingStateLoss()
            }

        }

        // Animasyonları başlat
        showRandomAnimation()
        playResultRevealSequence()

        binding.root.post { elevateLessonResultOverlayAboveMap() }
    }

    override fun onDestroyView() {
        LessonProgressDiag.log(
            "LessonResult.onDestroyView",
            "claimHandled=$claimHandled msAlive=${System.currentTimeMillis() - viewCreatedAtMs} " +
                "mapIdx=$mapFragmentStepIndex part=${GlobalLessonData.globalPartId} " +
                if (!claimHandled) "-> DESTROYED WITHOUT CLAIM CLICK (geri tuşu / dış transaction şüphesi)" else "",
        )
        revealHandler.removeCallbacksAndMessages(null)
        activeCountAnimator?.cancel()
        activeCountAnimator = null
        binding.totalScoreBox.animate().cancel()
        binding.successRateBox.animate().cancel()
        binding.claimButton.animate().cancel()
        restoreLessonResultOverlayElevation()
        MainActivityChromeBlocker.release(activity)
        super.onDestroyView()
        // View hiyerarşisini bırak (fragment geri yığınında beklerken bellekte kalıyordu).
        _binding = null
    }

    private fun updateUI() {
        // Örnek: Doğru cevap sayısını göster
        succsessRate = if (totalQuestions > 0) ((correctAnswers.toFloat() / totalQuestions.toFloat()) * 100) else 0f
        lessonScore = (succsessRate * 5f).toInt()
        // Metinler ve kutular playResultRevealSequence() tarafından sırayla gösterilir.

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

    /**
     * Toplam Puan ve Başarı kutularını sırayla ortaya çıkarır: önce Toplam Puan kutusu
     * %75 ölçekten %100'e büyüyerek belirir (bu sırada değer 0'dır), ardından değer 0'dan
     * gerçek değerine 1 saniyede sayarak artar. Bu tamamlanınca aynı akış Başarı kutusu için
     * tekrarlanır. En son claimButton görünmez->görünür şeklinde belirir.
     */
    private fun playResultRevealSequence() {
        binding.totalScore.text = "0"
        binding.successRate.text = "0%"
        binding.claimButton.isEnabled = false

        revealBox(binding.totalScoreBox) {
            revealHandler.postDelayed({
                if (!isAdded) return@postDelayed
                animateCountUp(binding.totalScore, to = lessonScore, suffix = "") {
                    revealHandler.postDelayed({
                        if (!isAdded) return@postDelayed
                        revealBox(binding.successRateBox) {
                            revealHandler.postDelayed({
                                if (!isAdded) return@postDelayed
                                animateCountUp(binding.successRate, to = succsessRate.toInt(), suffix = "%") {
                                    revealHandler.postDelayed({
                                        if (!isAdded) return@postDelayed
                                        revealClaimButton()
                                    }, STEP_PAUSE_MS)
                                }
                            }, STEP_PAUSE_MS)
                        }
                    }, STEP_PAUSE_MS)
                }
            }, STEP_PAUSE_MS)
        }
    }

    private fun revealBox(box: View, onEnd: () -> Unit) {
        box.visibility = View.VISIBLE
        box.alpha = 0f
        box.scaleX = 0.75f
        box.scaleY = 0.75f
        box.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(BOX_APPEAR_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { onEnd() }
            .start()
    }

    private fun animateCountUp(textView: TextView, to: Int, suffix: String, onEnd: () -> Unit) {
        activeCountAnimator?.cancel()
        activeCountAnimator = ValueAnimator.ofInt(0, to).apply {
            duration = COUNT_UP_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { textView.text = "${it.animatedValue as Int}$suffix" }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })
            start()
        }
    }

    private fun revealClaimButton() {
        binding.claimButton.visibility = View.VISIBLE
        binding.claimButton.alpha = 0f
        binding.claimButton.animate()
            .alpha(1f)
            .setDuration(CLAIM_BUTTON_FADE_MS)
            .withEndAction { binding.claimButton.isEnabled = true }
            .start()
    }

}
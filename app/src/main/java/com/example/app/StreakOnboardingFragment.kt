package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentStreakOnboardingBinding

/**
 * Seri kurulumu: ilk ders bittikten sonra, kayıt ekranına gitmeden önce açılır.
 *
 * ## Neden burada
 * Kullanıcı az önce ilk dersini bitirdi — "yarın da gel" demenin en inandırıcı anı bu.
 * Kayıttan sonraya bırakılsaydı form doldurmuş, motivasyonu düşmüş biriyle konuşuyor olurduk.
 *
 * ## Neden hedef meydan okumadan önce
 * "3 gün üst üste" sözü, bir günün ne demek olduğu bilinmeden verilemez. Mimo'da hedef kayıt
 * sırasında soruluyor ve meydan okuma sonra geliyor; bizim akışımızda ilk ders kayıttan önce
 * olduğu için ikisi de buraya alındı ve doğru sıraya kondu.
 *
 * ## Sonucu kim alıyor
 * Bitince `streak_onboarding_done` fragment sonucu yayınlanıyor. Çağıran taraf (ilk dersi
 * bitiren ekran) bunu dinleyip kayıt akışını başlatıyor; akışın kendisi kayıt ekranını
 * açmıyor, yalnızca "bitti" diyor.
 */
class StreakOnboardingFragment : Fragment() {

    private var _binding: FragmentStreakOnboardingBinding? = null
    private val binding get() = _binding!!

    private enum class Step { INTRO, GOAL, CHALLENGE, DONE }

    private var step = Step.INTRO
    private var goalMinutes = 0
    private var challengeDays = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStreakOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        goalMinutes = StreakRepository.goalMinutes(requireContext())

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            when (step) {
                Step.INTRO -> finish()
                Step.GOAL -> render(Step.INTRO)
                Step.CHALLENGE -> render(Step.GOAL)
                // Son ekranda geri: seçimler zaten kaydedildi, akış bitmiş sayılır.
                Step.DONE -> finish()
            }
        }

        binding.streakOnboardingContinue.setOnClickListener { onContinue() }
        render(Step.INTRO)
    }

    private fun onContinue() {
        when (step) {
            Step.INTRO -> render(Step.GOAL)
            Step.GOAL -> {
                StreakRepository.setGoalMinutes(requireContext(), goalMinutes)
                render(Step.CHALLENGE)
            }
            Step.CHALLENGE -> {
                StreakRepository.setChallengeDays(requireContext(), challengeDays)
                render(Step.DONE)
            }
            Step.DONE -> finish()
        }
    }

    private fun finish() {
        if (!isAdded) return
        StreakRepository.markOnboardingDone(requireContext())
        parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle())
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commitAllowingStateLoss()
    }

    // ── Çizim ───────────────────────────────────────────────────────────

    private fun render(next: Step) {
        step = next
        val b = binding
        b.streakOnboardingProgress.progress = when (step) {
            Step.INTRO -> 25
            Step.GOAL -> 50
            Step.CHALLENGE -> 75
            Step.DONE -> 100
        }
        b.streakOnboardingOptions.removeAllViews()
        b.streakOnboardingOptions.visibility = View.GONE
        b.streakOnboardingWeekStrip.visibility = View.GONE
        b.streakOnboardingChallengeCard.visibility = View.GONE
        b.streakOnboardingIcon.visibility = View.GONE
        b.streakOnboardingLottie.visibility = View.GONE
        b.streakOnboardingSubtitle.visibility = View.VISIBLE

        when (step) {
            Step.INTRO -> {
                b.streakOnboardingIcon.visibility = View.VISIBLE
                b.streakOnboardingTitle.text = "Şimdi bir öğrenme alışkanlığı kazanmana yardım edelim."
                b.streakOnboardingSubtitle.text =
                    "Her gün küçük bir hedef, uzun vadede büyük fark yaratır."
                b.streakOnboardingContinue.text = "Devam et"
                setContinueEnabled(true)
            }

            Step.GOAL -> {
                b.streakOnboardingTitle.text = "Her gün ne kadar öğrenmek istersin?"
                b.streakOnboardingSubtitle.text = "Bu hedefi istediğin zaman değiştirebilirsin"
                b.streakOnboardingContinue.text = "Devam et"
                b.streakOnboardingOptions.visibility = View.VISIBLE
                StreakViews.buildOptionRows(
                    container = b.streakOnboardingOptions,
                    values = StreakRepository.GOAL_OPTIONS,
                    labels = listOf("Rahat", "Düzenli", "Ciddi"),
                    trailing = StreakRepository.GOAL_OPTIONS.map { "$it dakika" },
                    selected = goalMinutes,
                ) { value ->
                    goalMinutes = value
                    render(Step.GOAL)
                }
                setContinueEnabled(goalMinutes in StreakRepository.GOAL_OPTIONS)
            }

            Step.CHALLENGE -> {
                b.streakOnboardingTitle.text = "Kaç gün üst üste öğreneceksin?"
                b.streakOnboardingSubtitle.text =
                    "Günde $goalMinutes dakika. Seni zorlamayacak bir hedef seç."
                b.streakOnboardingContinue.text = "Hedefimi onayla"
                b.streakOnboardingOptions.visibility = View.VISIBLE
                StreakViews.buildOptionRows(
                    container = b.streakOnboardingOptions,
                    values = StreakRepository.CHALLENGE_OPTIONS,
                    labels = StreakRepository.CHALLENGE_OPTIONS.map { "$it gün" },
                    trailing = listOf("Başlangıç", "İyi gidiyor", "Alışkanlık oluşuyor"),
                    selected = challengeDays,
                ) { value ->
                    challengeDays = value
                    render(Step.CHALLENGE)
                }
                // Meydan okuma bilerek ön seçimsiz: kullanıcı bu sözü kendisi vermeli.
                setContinueEnabled(challengeDays in StreakRepository.CHALLENGE_OPTIONS)
            }

            Step.DONE -> {
                val state = StreakRepository.refresh(requireContext())
                b.streakOnboardingLottie.visibility = View.VISIBLE
                b.streakOnboardingTitle.text = "Meydan okuma kabul edildi!"
                b.streakOnboardingSubtitle.text =
                    "Serini sürdürerek bir öğrenme alışkanlığı kazan."
                b.streakOnboardingContinue.text = "Devam et"
                setContinueEnabled(true)
                b.streakOnboardingWeekStrip.visibility = View.VISIBLE
                StreakViews.buildWeekStrip(b.streakOnboardingWeekStrip, state.achievedDays)
                b.streakOnboardingChallengeCard.visibility = View.VISIBLE
                b.streakOnboardingChallengeTitle.text = "$challengeDays Günlük Meydan Okuma"
                // Kullanıcı bu ekrana ilk dersini bitirir bitirmez geliyor; günlük hedefi
                // (dakika) henüz tutturmuş olmayabilir. "0. GÜNÜ" demek yerine durumu
                // olduğu gibi söylüyoruz — şişirilmiş bir sayı ilk temasta güveni bozar.
                b.streakOnboardingChallengeSub.text =
                    if (state.current <= 0) "BUGÜN BAŞLIYOR"
                    else "$challengeDays GÜNÜN ${state.current}. GÜNÜ"
            }
        }
    }

    private fun setContinueEnabled(enabled: Boolean) {
        binding.streakOnboardingContinue.isEnabled = enabled
        binding.streakOnboardingContinue.alpha = if (enabled) 1f else 0.6f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Akış bittiğinde yayınlanan fragment sonucu. */
        const val RESULT_KEY = "streak_onboarding_done"
    }
}

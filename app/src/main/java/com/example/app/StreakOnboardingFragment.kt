package com.example.app

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentStreakOnboardingBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
                buildOptions(
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
                buildOptions(
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
                buildWeekStrip(state.achievedDays)
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

    /**
     * Seçenek satırlarını üretir. Satırlar yinelenen öğeler olduğu için XML'de değil burada:
     * aksi halde her satır için ayrı kimlik ve ayrı dinleyici taşımak gerekirdi.
     */
    private fun buildOptions(
        values: List<Int>,
        labels: List<String>,
        trailing: List<String>,
        selected: Int,
        onPick: (Int) -> Unit,
    ) {
        val container = binding.streakOnboardingOptions
        val density = resources.displayMetrics.density
        values.forEachIndexed { index, value ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(
                    if (value == selected) R.drawable.bg_streak_option_selected
                    else R.drawable.bg_streak_option,
                )
                val pad = (16 * density).toInt()
                setPadding(pad, pad, pad, pad)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { if (index > 0) topMargin = (12 * density).toInt() }
                setOnClickListener { onPick(value) }
            }
            row.addView(
                TextView(requireContext()).apply {
                    text = labels.getOrElse(index) { "" }
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 17f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                },
            )
            row.addView(
                TextView(requireContext()).apply {
                    text = trailing.getOrElse(index) { "" }
                    setTextColor(android.graphics.Color.parseColor("#93A5B3"))
                    textSize = 15f
                },
            )
            container.addView(row)
        }
    }

    /**
     * Haftanın günleri, pazartesiden pazara.
     *
     * Son 7 gün değil takvim haftası gösteriliyor: kullanıcı "bu hafta neredeyim" diye
     * bakıyor, "son yedi günde" diye değil.
     */
    private fun buildWeekStrip(achieved: Set<String>) {
        val container = binding.streakOnboardingWeekStrip
        container.removeAllViews()
        val density = resources.displayMetrics.density
        val today = StudyTimeTracker.dayId()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        // Calendar.DAY_OF_WEEK: Pazar=1 … Cumartesi=7. Pazartesi başlangıç için kaydırma.
        val shift = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_YEAR, -shift)

        val letters = listOf("P", "S", "Ç", "P", "C", "C", "P")
        for (i in 0 until 7) {
            val dayId = format.format(Date(cal.timeInMillis))
            val done = dayId in achieved
            val isToday = dayId == today

            val cell = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            cell.addView(
                TextView(requireContext()).apply {
                    text = if (done) "✓" else ""
                    gravity = Gravity.CENTER
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 14f
                    setBackgroundResource(
                        when {
                            done -> R.drawable.bg_streak_day_done
                            isToday -> R.drawable.bg_streak_day_today
                            else -> R.drawable.bg_streak_day_empty
                        },
                    )
                    layoutParams = LinearLayout.LayoutParams((28 * density).toInt(), (28 * density).toInt())
                },
            )
            cell.addView(
                TextView(requireContext()).apply {
                    text = letters[i]
                    gravity = Gravity.CENTER
                    setTextColor(android.graphics.Color.parseColor(if (isToday) "#FF9800" else "#93A5B3"))
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { topMargin = (6 * density).toInt() }
                },
            )
            container.addView(cell)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
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

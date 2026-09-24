package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.app.databinding.FragmentNewStreakBinding

/**
 * Seri kırıkken ders dönüşünde çıkan "yeni seri başlat" ekranı.
 *
 * ## Neden ders sonunda
 * Serisi olmayan kullanıcıya boşlukta "kaç gün üst üste?" diye sormanın karşılığı yok —
 * o an ortada tutulacak bir söz yok. Dersten yeni çıkmış çocuk ise az önce çalıştığını
 * biliyor; söz vermenin en ucuz olduğu an orası. Mimo ve Duolingo da aynı anı kullanıyor.
 *
 * ## İki sayfa, tek ekran
 * Önce niyet ("bir alışkanlık kazanalım"), sonra soru. Soru doğrudan gelseydi çocuk neyi
 * neden seçtiğini bilmeden bir sayı seçerdi.
 *
 * ## Nerede açılıyor
 * Haritada açılan diğer şeylerle (rozet, reklam, sezon kapısı, maraton rehberi, kupa yolu
 * yönlendirmesi) AYNI kapıyı paylaşıyor: `MainActivity.marathonGuideMapBlockReason`.
 * Ekran o kapı açıkken açılıyor ve açıkken kapıyı kapatıyor, yani diğerleri de bunu
 * bekliyor. Böylece "önce şu sonra bu" diye bir sıra tanımlamak gerekmedi.
 *
 * ## Neden kapatılabiliyor
 * Zorunlu tutmak, dersten çıkışı bir kapıya çeviriyordu. Cevaplamayan kullanıcı eski
 * meydan okumasıyla devam ediyor ve soru o gün bir daha sorulmuyor
 * (bkz. [StreakRepository.needsNewStreakPrompt]).
 */
class NewStreakFragment : DialogFragment() {

    private var _binding: FragmentNewStreakBinding? = null
    private val binding get() = _binding!!

    private var asking = false
    private var picked = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Light_NoTitleBar_Fullscreen)
    }

    /**
     * Sağdan kayarak geliyor, sola itilerek gidiyor.
     *
     * Rozet kutlamasıyla AYNI çizimler ([R.style.QueueScreenAnimation]): ders sonrası
     * kuyruğundaki ekranlar art arda geldiği için hareket dilinin ortak olması gerekiyor.
     * Paylasılan `slide_in_right` kullanılmıyordu çünkü 500 ms sürüp alpha ile soluyor,
     * çıkış çizimleri ise 300 ms — iki ekran farklı hızda hareket edince kopuk duruyordu.
     */
    override fun onStart() {
        super.onStart()
        dialog?.window?.setWindowAnimations(R.style.QueueScreenAnimation)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewStreakBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Bugün için sorulmuş sayılıyor: ekran AÇILDIĞINDA işaretleniyor, cevaplandığında
        // değil. Kapatıp çıkan kullanıcıya her ders sonunda yeniden sormak, cevap
        // vermemeyi cezalandırmak olurdu.
        StreakRepository.markNewStreakPromptShown(requireContext())
        AnalyticsLogger.logStreakSetupStep(AnalyticsLogger.STREAK_STAGE_NEW_RUN_INTRO)

        renderIntro()
        binding.newStreakContinue.setOnClickListener {
            if (asking) finishWithPick() else showQuestion()
        }
    }

    private fun renderIntro() {
        asking = false
        binding.newStreakTitle.text = "Şimdi bir öğrenme alışkanlığı kazanmana yardım edelim."
        binding.newStreakSubtitle.visibility = View.GONE
        binding.newStreakOptions.visibility = View.GONE
        binding.newStreakContinue.text = "Devam et"
        setContinueEnabled(true)
    }

    private fun showQuestion() {
        asking = true
        AnalyticsLogger.logStreakSetupStep(AnalyticsLogger.STREAK_STAGE_NEW_RUN_CHALLENGE)

        val goal = StreakRepository.goalMinutes(requireContext())
        binding.newStreakTitle.text = "Kaç gün üst üste öğreneceksin?"
        binding.newStreakSubtitle.text =
            "Günde $goal dakika. Bu seçim seri boyunca değişmiyor."
        binding.newStreakSubtitle.visibility = View.VISIBLE
        binding.newStreakOptions.visibility = View.VISIBLE
        binding.newStreakContinue.text = "Başla"
        // Bilerek ön seçimsiz: söz kullanıcının kendisinin olmalı.
        setContinueEnabled(false)
        renderOptions()

        // Soru sağdan kayarak gelsin; kayıt akışındaki adım geçişiyle aynı his.
        val density = resources.displayMetrics.density
        listOf(binding.newStreakTitle, binding.newStreakSubtitle, binding.newStreakOptions)
            .forEach { v ->
                v.translationX = 220f * density
                v.alpha = 0f
                v.animate().translationX(0f).alpha(1f).setDuration(220).start()
            }
    }

    private fun renderOptions() {
        StreakViews.buildOptionRows(
            container = binding.newStreakOptions,
            values = StreakRepository.CHALLENGE_OPTIONS,
            labels = StreakRepository.CHALLENGE_OPTIONS.map { "$it gün" },
            // Ödül burada yazıyor çünkü seçim burada yapılıyor ve seri boyunca
            // değişmiyor: neyin karşılığında söz verdiğini seçerken görmeli.
            trailing = StreakRepository.CHALLENGE_OPTIONS.map {
                "+${StreakMilestones.challengeReward(it)} altın"
            },
            selected = picked,
            trailingColor = StreakViews.COLOR_GOLD,
        ) { value ->
            picked = value
            setContinueEnabled(true)
            renderOptions()
        }
    }

    /**
     * Düğmenin açık/kapalı hali.
     *
     * Rengi de değişiyor: `backgroundTint` XML'de sabit olduğu için yalnızca [View.setEnabled]
     * değiştirmek, basılmayan ama basılabilir GÖRÜNEN bir düğme bırakıyordu. Kayıt
     * akışındaki `updateContinueButton` ile aynı çözüm.
     */
    private fun setContinueEnabled(enabled: Boolean) {
        val b = _binding ?: return
        b.newStreakContinue.isEnabled = enabled
        val color = androidx.core.content.ContextCompat.getColor(
            requireContext(),
            if (enabled) R.color.dark_primary else R.color.dark_text_secondary,
        )
        b.newStreakContinue.backgroundTintList =
            android.content.res.ColorStateList.valueOf(color)
    }

    private fun finishWithPick() {
        if (picked !in StreakRepository.CHALLENGE_OPTIONS) return
        StreakRepository.startNewChallenge(requireContext(), picked)
        AnalyticsLogger.logStreakChallengeSet(picked, AnalyticsLogger.STREAK_SOURCE_NEW_RUN)
        // Seçim sunucuya ancak bir çağrıyla gidiyor; startNewChallenge günlük bildirim
        // işaretini sildiği için bu çağrı gerçekten gönderiliyor.
        StreakSyncService.syncPendingDays(requireContext())
        dismissAllowingStateLoss()
    }

    /**
     * Ekran nasıl kapanırsa kapansın (Başla, geri tuşu, sistem) ertelenmiş harita
     * dönüşü sürdürülüyor. Yalnızca "Başla"ya bağlansaydı geri tuşuyla çıkan
     * kullanıcı ders sonunda asılı kalırdı.
     */
    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        (activity as? MainActivity)?.onNewStreakPromptClosed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "NewStreakFragment"
    }
}

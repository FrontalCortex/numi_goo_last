package com.example.app

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentStreakBinding

/**
 * Günlük seri ekranı: üst bardaki aleve dokununca açılır.
 *
 * ## Ne gösteriyor
 * Serinin kaç gün olduğu, bugünkü hedefin ne kadarının tamamlandığı, haftanın hangi
 * günlerinin tutturulduğu ve meydan okumanın durumu.
 *
 * ## Neden buradan hedef değiştirilebiliyor
 * Kurulum akışı kullanıcıya "Bu hedefi istediğin zaman değiştirebilirsin" diyor. O sözün
 * karşılığı bu ekran; başka bir yerde ayar yok. Meydan okuma da aynı pencereden
 * değiştirilebiliyor: hedef tuttuktan sonra "3 gün" hedefinde takılı kalmak motive edici
 * değil, kullanıcı çıtayı kendisi yükseltebilmeli.
 *
 * ## Neden süre dinleyicisi kurulmuyor
 * [StudyTimeTracker] yalnızca ders ekranlarında sayıyor; bu ekran açıkken süre zaten
 * işlemiyor. Dinleyici kurulsaydı [MainActivity]'nin dinleyicisini ezerdi ve ekran
 * kapandığında üst bar güncellenmez hale gelirdi.
 */
class StreakFragment : Fragment() {

    private var _binding: FragmentStreakBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStreakBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStreakBack.setOnClickListener { close() }
        binding.streakChangeGoal.setOnClickListener { showGoalPicker() }
        binding.streakChallengeCard.setOnClickListener { showChallengePicker() }
    }

    override fun onResume() {
        super.onResume()
        render()
        // Bekleyen gün varsa (çevrimdışı tutturulmuş olabilir) burada kapanıyor; dönünce
        // ödül satırları da tazeleniyor.
        StreakSyncService.syncPendingDays(requireContext()) {
            if (isAdded) render()
        }
    }

    private fun close() {
        if (!isAdded) return
        parentFragmentManager.popBackStack()
    }

    // ── Çizim ───────────────────────────────────────────────────────────

    private fun render() {
        val b = _binding ?: return
        val state = StreakRepository.refresh(requireContext())
        val alive = state.current > 0

        // Seri yaşıyorsa alev oynuyor, kırıksa sönük duruyor. Aynı ayrım üst barda da var.
        b.streakLottie.visibility = if (alive) View.VISIBLE else View.GONE
        b.streakDeadIcon.visibility = if (alive) View.GONE else View.VISIBLE

        b.streakBigNumber.text = state.current.toString()
        b.streakBigNumber.setTextColor(Color.parseColor(if (alive) COLOR_ACCENT else COLOR_DEAD))
        b.streakBigLabel.text = if (alive) "gün üst üste" else "Serin şu anda kırık"
        b.streakBigSubtitle.text = when {
            state.goalReachedToday -> "Bugünkü hedefini tamamladın. Yarın da bekleriz!"
            alive -> "Bugün ${state.minutesLeft} dakika daha çalış, serin devam etsin."
            else -> "Bugün ${state.goalMinutes} dakika çalış, yeni serini başlat."
        }

        val minutesToday = state.secondsToday / 60
        b.streakTodayText.text =
            if (state.goalReachedToday) "Hedef tamamlandı ✓"
            else "$minutesToday / ${state.goalMinutes} dakika"
        // Çubuğun genişliği ölçüm bitmeden bilinmiyor; kupa yolundaki ile aynı yol.
        val zone = b.streakTodayZone
        zone.post {
            val live = _binding ?: return@post
            if (zone.width <= 0) return@post
            applyDailyQuestionProgressOverlayNow(
                widthHost = zone,
                fill = live.streakTodayFill,
                shine = live.streakTodayShine,
                percent = state.todayFraction * 100f,
                complete = state.goalReachedToday,
            )
        }

        StreakViews.buildWeekStrip(b.streakWeekStrip, state.achievedDays)

        b.streakChallengeTitle.text = "${state.challengeDays} Günlük Meydan Okuma"
        b.streakChallengeSub.text = when {
            state.current <= 0 -> "BUGÜN BAŞLIYOR"
            state.current >= state.challengeDays -> "TAMAMLANDI — yeni hedef için dokun"
            else -> "${state.challengeDays} GÜNÜN ${state.current}. GÜNÜ"
        }

        b.streakLongest.text = "${state.longest} gün"

        renderRewards(b)
    }

    /**
     * Ödül satırları.
     *
     * SUNUCUNUN bildiği seriye bakıyor, yerel sayaca değil: ödülü veren taraf sunucu, yerel
     * sayaç ondan ileride olabilir (henüz eşitlenmemiş gün) ve o durumda basılabilir ama her
     * seferinde hata veren bir "Topla" düğmesi göstermiş olurduk.
     */
    private fun renderRewards(b: FragmentStreakBinding) {
        val serverStreak = StreakRepository.serverCurrent(requireContext())
        val claimed = StreakRepository.claimedMilestones(requireContext())
        StreakViews.buildMilestoneRows(b.streakRewards, serverStreak, claimed) { milestone ->
            claimMilestone(milestone)
        }
    }

    private fun claimMilestone(milestone: Int) {
        if (!isAdded) return
        StreakSyncService.claimReward(requireContext(), milestone) { success, message, _, _ ->
            if (!isAdded) return@claimReward
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            if (success) {
                // Cüzdan değişti: üst bardaki altın/anahtar da tazelensin.
                (activity as? MainActivity)?.refreshWalletUi()
                render()
            }
        }
    }

    // ── Seçim penceresi ─────────────────────────────────────────────────

    private fun showGoalPicker() {
        showPicker(
            title = "Günlük hedefin",
            subtitle = "Her gün ne kadar öğrenmek istersin?",
            values = StreakRepository.GOAL_OPTIONS,
            labels = listOf("Rahat", "Düzenli", "Ciddi"),
            trailing = StreakRepository.GOAL_OPTIONS.map { "$it dakika" },
            selected = StreakRepository.goalMinutes(requireContext()),
        ) { value ->
            // Aynı değere tekrar dokunmak bir değişiklik değil; ölçüme de öyle gitmemeli.
            if (value != StreakRepository.goalMinutes(requireContext())) {
                AnalyticsLogger.logStreakGoalSet(value, AnalyticsLogger.STREAK_SOURCE_SETTINGS)
            }
            StreakRepository.setGoalMinutes(requireContext(), value)
        }
    }

    private fun showChallengePicker() {
        showPicker(
            title = "Meydan okuman",
            subtitle = "Kaç gün üst üste öğreneceksin?",
            values = StreakRepository.CHALLENGE_OPTIONS,
            labels = StreakRepository.CHALLENGE_OPTIONS.map { "$it gün" },
            trailing = listOf("Başlangıç", "İyi gidiyor", "Alışkanlık oluşuyor"),
            selected = StreakRepository.challengeDays(requireContext()),
        ) { value ->
            if (value != StreakRepository.challengeDays(requireContext())) {
                AnalyticsLogger.logStreakChallengeSet(
                    value,
                    AnalyticsLogger.STREAK_SOURCE_SETTINGS,
                )
            }
            StreakRepository.setChallengeDays(requireContext(), value)
        }
    }

    /**
     * Seçenek penceresi. İki seçim de aynı pencereyi kullanıyor; tek fark başlık, satırlar
     * ve kaydeden satır.
     *
     * Seçim yapılır yapılmaz kaydedilip pencere kapanıyor: "onayla" düğmesi tek seçimli bir
     * listede fazladan bir adım olurdu.
     */
    private fun showPicker(
        title: String,
        subtitle: String,
        values: List<Int>,
        labels: List<String>,
        trailing: List<String>,
        selected: Int,
        onPick: (Int) -> Unit,
    ) {
        if (!isAdded) return
        val view = layoutInflater.inflate(R.layout.dialog_streak_options, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<TextView>(R.id.streakOptionsTitle).text = title
        view.findViewById<TextView>(R.id.streakOptionsSubtitle).text = subtitle
        StreakViews.buildOptionRows(
            container = view.findViewById<LinearLayout>(R.id.streakOptionsContainer),
            values = values,
            labels = labels,
            trailing = trailing,
            selected = selected,
        ) { value ->
            onPick(value)
            dialog.dismiss()
            render()
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val COLOR_ACCENT = "#FF9800"
        const val COLOR_DEAD = "#78909C"
    }
}

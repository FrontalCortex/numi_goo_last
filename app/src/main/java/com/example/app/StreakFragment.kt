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
 * ## Neden hedef değiştirilebiliyor ama meydan okuma değiştirilemiyor
 * Kurulum akışı kullanıcıya "Bu hedefi istediğin zaman değiştirebilirsin" diyor. O sözün
 * karşılığı bu ekran; başka bir yerde ayar yok.
 *
 * Meydan okuma ise bir kez seçiliyor çünkü ÖDÜLÜ var: değiştirilebilseydi kullanıcı üç
 * günlük sözün 500 altınını alıp hemen yedi güne çıkarak 1500'ü de alabilirdi. Aynı
 * kilit sunucuda da var; buradaki yalnızca arayüzün tutarlı durması için.
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
        binding.streakChallengeClaim.setOnClickListener { claimChallenge() }
    }

    override fun onResume() {
        super.onResume()
        render()
        // Bekleyen gün varsa (çevrimdışı tutturulmuş olabilir) burada kapanıyor; dönünce
        // ödül satırları da tazeleniyor.
        StreakSyncService.syncPendingDays(requireContext()) {
            if (isAdded) render()
        }
        // Okuma yönü: ödül satırlarının dayandığı seri sunucuda tutuluyor, bu ekran her
        // açıldığında oradan tazeleniyor. Gönderecek gün olmadığında tek güncelleme yolu bu.
        StreakSyncService.refreshFromServer(requireContext()) {
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

        renderChallenge(b, state)

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

    // ── Meydan okuma ──────────────────────────────────

    /**
     * Meydan okuma kartı: verilen söz, ödül rozeti, ilerleme çubuğu ve hak edildiyse
     * toplama düğmesi.
     *
     * İlerleme YEREL seriden çiziliyor — ekranın tepesindeki büyük sayı da oradan geliyor,
     * ikisi ayrı kaynaklardan beslenseydi aynı ekran kendi kendisiyle çelişirdi. Ödülün
     * toplanabilirliği ise SUNUCUNUN serisine bakıyor: ödülü veren taraf o, yoksa basılabilen
     * ama her seferinde hata veren bir düğme göstermiş olurduk.
     *
     * İkisinin ayrıştığı tek durum — yerelde tamamlandı, henüz sunucuya gitmedi — sessiz
     * bırakılmıyor: "ödülün hazırlanıyor" satırı onu anlatıyor. Yoksa çocuk sözünü tuttuğunu
     * görüp karşılığını bulamazdı.
     */
    private fun renderChallenge(b: FragmentStreakBinding, state: StreakRepository.StreakState) {
        // Seçilmiş değer, varsayılan değil: bu özellikten ÖNCE kayıt olmuş kullanıcı hiçbir
        // söz vermedi, sunucu da onun için bir meydan okuma tanımıyor. Kart varsayılanla
        // çizilseydi o kullanıcıya basıldığında "meydan okuma seçilmemiş" hatası veren bir
        // "Topla" düğmesi gösterirdik.
        val days = StreakRepository.chosenChallengeDays(requireContext())
        val reward = StreakMilestones.challengeReward(days)
        val claimedDays = StreakRepository.challengeClaimed(requireContext())

        // Ödül alındıktan sonra kart O GÜNÜN sonuna kadar duruyor, ertesi gün gidiyor.
        //
        // Hemen kaldırmak kutlamayı çalıyor: çocuk "Topla"ya bastığı anda sözünü tuttuğunun
        // izi ekrandan siliniyordu. Sürekli bırakmak da yanlış: meydan okuma bir kez
        // seçiliyor, yani bitmiş kart bir daha asla değişmeyecek ölü bir kutu olarak kalır ve
        // ekranın asıl işini — bugün ne yapmalı — aşağı iter.
        val claimedToday = claimedDays > 0 &&
            StreakRepository.challengeClaimedDay(requireContext()) == StudyTimeTracker.dayId()
        if (days <= 0 || reward <= 0 || (claimedDays > 0 && !claimedToday)) {
            b.streakChallengeCard.visibility = View.GONE
            return
        }
        b.streakChallengeCard.visibility = View.VISIBLE

        val done = state.current.coerceIn(0, days)
        val serverStreak = StreakRepository.serverCurrent(requireContext())
        val complete = state.current >= days
        val claimable = claimedDays <= 0 && serverStreak >= days

        b.streakChallengeTitle.text = "$days Günlük Meydan Okuma"
        b.streakChallengeSub.text = when {
            claimedDays > 0 -> "TAMAMLANDI ✓"
            claimable -> "TAMAMLANDI — ödülünü topla"
            complete -> "TAMAMLANDI — ödülün hazırlanıyor"
            state.current <= 0 -> "BUGÜN BAŞLIYOR"
            else -> "$days GÜNÜN $done. GÜNÜ"
        }

        // Ödül alındıysa rozet kalkıyor: artık bir vaat değil, olmuş bitmiş bir şey.
        b.streakChallengeReward.visibility = if (claimedDays > 0) View.GONE else View.VISIBLE
        b.streakChallengeRewardText.text = "+$reward"
        b.streakChallengeClaim.visibility = if (claimable) View.VISIBLE else View.GONE

        val fraction = (done.toFloat() / days).coerceIn(0f, 1f)
        val zone = b.streakChallengeZone
        zone.post {
            val live = _binding ?: return@post
            if (zone.width <= 0) return@post
            applyDailyQuestionProgressOverlayNow(
                widthHost = zone,
                fill = live.streakChallengeFill,
                shine = live.streakChallengeShine,
                percent = fraction * 100f,
                complete = complete,
            )
        }
    }

    private fun claimChallenge() {
        if (!isAdded) return
        StreakSyncService.claimChallengeReward(requireContext()) { success, message, _, _ ->
            if (!isAdded) return@claimChallengeReward
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            if (success) {
                (activity as? MainActivity)?.refreshWalletUi()
                render()
            }
        }
    }

    // ── Hedef penceresi ──────────────────────────────────

    /**
     * Günlük hedef seçimi. Ekrandaki tek ayar bu; meydan okuma bir kez seçildiği için
     * buradan değiştirilemiyor (bkz. sınıf açıklaması).
     *
     * Seçim yapılır yapılmaz kaydedilip pencere kapanıyor: "onayla" düğmesi tek seçimli bir
     * listede fazladan bir adım olurdu.
     */
    private fun showGoalPicker() {
        if (!isAdded) return
        val view = layoutInflater.inflate(R.layout.dialog_streak_options, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<TextView>(R.id.streakOptionsTitle).text = "Günlük hedefin"
        view.findViewById<TextView>(R.id.streakOptionsSubtitle).text =
            "Her gün ne kadar öğrenmek istersin?"
        StreakViews.buildOptionRows(
            container = view.findViewById<LinearLayout>(R.id.streakOptionsContainer),
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

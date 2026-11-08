package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.app.databinding.FragmentInviteBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * "ARKADAŞINI DAVET ET, 1 HAFTA PRO KAZAN" — haftalık karttan bağımsız genel davet (kullanıcı isteği
 * 09.11.2026). Mağazada Pro kartının altından ve Ayarlar'dan açılıyor.
 *
 * Ödül ve koşullar haftalık kart paylaşımıyla aynı (functions/referrals.js): bağlantıdan gelen arkadaş
 * hesap açıp 2. dersi bitirince ikisine de 7 gün Pro; getiren yılda en çok 10 davetten. Fark yalnızca
 * görsel (kart yerine Sobi'li sabit davet, [FragmentInviteBinding.inviteHero]) ve kaynak (`invite`;
 * bağlantı `/d/<kod>?k=invite`, Play referrer'ında `utm_medium=invite`).
 *
 * Sayaç ("Davet ettiklerin: 2 · kazandığın: 14 gün Pro") `getReferralCode`'un cevabından: paylaşmayı
 * sürdürmenin en güçlü nedeni, ödülün gerçekten geldiğini görmek.
 */
class InviteFragment : Fragment() {

    companion object {
        /**
         * Ayarlar'dan: diğer ayar ekranları gibi tabandaki kabın yerine ([replace] true).
         * Mağazadan: mağazanın kendi kabına ÜSTÜNE (mağaza ders/öğretici üstündeki ayrı katmanda da açılıyor,
         * bkz. MainActivity.openShopFragment; tabana konsaydı mağazanın arkasında kalırdı).
         */
        fun open(fm: FragmentManager, containerId: Int = R.id.fragmentContainerID, replace: Boolean = true) {
            val tx = fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            if (replace) tx.replace(containerId, InviteFragment()) else tx.add(containerId, InviteFragment())
            tx.addToBackStack(null).commit()
        }
    }

    private var _binding: FragmentInviteBinding? = null
    private val binding get() = _binding!!

    /** Önceden alınmış davet (sayaç için açılışta okunuyor; paylaşırken yeniden okumaya gerek yok). */
    private var invite: WeeklyCardShare.Invite? = null
    /** 13 yaş ve üstü: veli kapısı yok (WeeklyCardFragment ile aynı kural). Bilinmiyorsa çocuk. */
    private var adult = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInviteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        AnalyticsLogger.logSimpleEvent("share_card_shown_invite")
        binding.btnInviteShare.setOnClickListener { onShare() }
        if (FirebaseAuth.getInstance().currentUser == null) return
        viewLifecycleOwner.lifecycleScope.launch {
            adult = runCatching { isAdult() }.getOrDefault(false)
            val inv = WeeklyCardShare.fetchInvite(requireContext(), "invite")
            if (_binding == null) return@launch
            invite = inv
            if (inv.hasCode) {
                binding.inviteStats.text = appString(R.string.invite_stats, inv.invited, inv.earnedDays)
                binding.inviteStats.visibility = View.VISIBLE
            }
        }
    }

    private suspend fun isAdult(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val y = FirebaseFirestore.getInstance().collection("users").document(uid).get().await().getLong("birthYear")?.toInt() ?: -1
        return y > 0 && Calendar.getInstance().get(Calendar.YEAR) - y >= 13
    }

    private fun onShare() {
        val gate: (() -> Unit) -> Unit =
            if (adult) { next -> next() } else { next -> ParentGate.show(requireContext(), next) }
        gate {
            AnalyticsLogger.logSimpleEvent("share_gate_pass")
            binding.btnInviteShare.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val inv = invite?.takeIf { it.hasCode } ?: WeeklyCardShare.fetchInvite(requireContext(), "invite")
                if (_binding == null) return@launch
                binding.btnInviteShare.isEnabled = true
                val bmp = WeeklyCardShare.renderBitmap(requireContext(), binding.inviteHero)
                WeeklyCardShare.share(requireContext(), bmp, appString(R.string.invite_share_text, inv.link))
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

package com.example.app

import android.app.Dialog
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Başarı anı paneli: "7 günlük seri! 🔥" / "Yeni rozet! 🏅" + Paylaş (bkz. [ShareMoments]).
 *
 * Paylaşılan görsel panelin üst kartı ([card]): Sobi + başlık + alt satır; kişisel veri yok.
 * Paylaşım haftalık kart ve davetle aynı yoldan: 13 yaş altında veli kapısı, davet bağlantısı
 * (`?k=moment`), ödül satırı. Kapanınca ders sonu kuyruğu dürtülüyor (ana ekran boştayken açıldığı
 * için normalde bekleyen yok; güvenlik ağı).
 */
class ShareMomentDialog : DialogFragment() {

    companion object {
        const val TAG = "ShareMoment"
        private const val ARG = "moment"

        fun show(fm: FragmentManager, m: ShareMoments.Moment) {
            if (fm.findFragmentByTag(TAG) != null) return
            ShareMomentDialog().apply { arguments = Bundle().apply { putString(ARG, m.toJson()) } }.show(fm, TAG)
        }
    }

    private lateinit var card: LinearLayout
    private var adult = false
    private var shareButton: TextView? = null
    /** Panel açılınca başlatılan davet isteği (bkz. onCreateView). */
    private var invite: Deferred<WeeklyCardShare.Invite>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = super.onCreateDialog(savedInstanceState).apply {
        window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val m = ShareMoments.Moment.fromJson(arguments?.getString(ARG)) ?: run { dismissAllowingStateLoss(); return View(ctx) }
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        fun rounded(color: Int, r: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(r).toFloat() }
        fun text(s: String, size: Float, color: Int, bold: Boolean = false) = TextView(ctx).apply {
            text = s; textSize = size; setTextColor(color); gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
        val (title, desc) = when (m.kind) {
            ShareMoments.Kind.STREAK -> appString(R.string.moment_streak_title, m.value) to appString(R.string.moment_streak_desc, m.value)
            ShareMoments.Kind.BADGE -> appString(R.string.moment_badge_title) to m.label
        }
        val white = ContextCompat.getColor(ctx, R.color.dark_text)
        val muted = ContextCompat.getColor(ctx, R.color.dark_text_secondary)

        card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = rounded(ContextCompat.getColor(ctx, R.color.bright_background_color), 20)
            setPadding(dp(24), dp(24), dp(24), dp(20))
            addView(SobiHeadView(ctx), LinearLayout.LayoutParams(dp(84), dp(84)))
            addView(text(title, 22f, white, bold = true).apply { setPadding(0, dp(12), 0, 0) })
            addView(text(desc, 16f, 0xFF8CC63F.toInt(), bold = true).apply { setPadding(0, dp(6), 0, 0) })
            addView(text("Sorobit", 13f, muted).apply { setPadding(0, dp(10), 0, 0) })
        }
        val share = text(appString(R.string.weekly_card_share), 16f, 0xFF10220A.toInt(), bold = true).apply {
            setPadding(dp(16), dp(13), dp(16), dp(13))
            // İçe göçen düğme; köşe 24 → 14 (kullanıcı isteği 10.11, ders panelindeki düğmeyle aynı).
            DepthButtons.style(this, 0xFF8CC63F.toInt(), 14f, lip = 0xFF62A12E.toInt())
            setOnClickListener { onShare(m) }
        }
        shareButton = share
        val later = text(appString(R.string.moment_later), 15f, muted).apply {
            setPadding(dp(16), dp(12), dp(16), dp(8))
            setOnClickListener { dismissAllowingStateLoss() }
        }
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(ContextCompat.getColor(ctx, R.color.background_color), 24)
            setPadding(dp(16), dp(16), dp(16), dp(8))
            addView(card)
            addView(text(appString(R.string.share_reward_hint), 13f, muted).apply { setPadding(dp(4), dp(12), dp(4), dp(10)) })
            addView(share)
            addView(later)
        }
        AnalyticsLogger.logSimpleEvent(if (m.kind == ShareMoments.Kind.STREAK) "share_card_shown_streak" else "share_card_shown_badge")
        // Yaş ve davet bağlantısı panel açılır açılmaz istenir: Paylaş'a basınca beklenmesin (09.11.2026 cihazda
        // sunucu fonksiyonu soğukken birkaç saniye sürdü ve basış fark edilmedi).
        viewLifecycleOwner.lifecycleScope.launch { adult = ReferralAttribution.isAdultUser() }
        invite = viewLifecycleOwner.lifecycleScope.async { WeeklyCardShare.fetchInvite(ctx, "moment") }
        return root
    }

    override fun onStart() {
        super.onStart()
        val w = (resources.displayMetrics.widthPixels * 0.88f).toInt()
        dialog?.window?.setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun onShare(m: ShareMoments.Moment) {
        val gate: (() -> Unit) -> Unit =
            if (adult) { next -> next() } else { next -> ParentGate.show(requireContext(), next) }
        gate {
            AnalyticsLogger.logSimpleEvent("share_gate_pass")
            // Basıldığı hemen görünsün; bağlantı hâlâ geliyorsa bekleme süresince düğme kapalı.
            shareButton?.apply {
                isEnabled = false
                alpha = 0.6f
                text = appString(R.string.share_preparing)
            }
            viewLifecycleOwner.lifecycleScope.launch {
                val inv = invite?.await() ?: WeeklyCardShare.fetchInvite(requireContext(), "moment")
                if (!isAdded) return@launch
                val bmp = WeeklyCardShare.renderBitmap(requireContext(), card)
                val text = when (m.kind) {
                    ShareMoments.Kind.STREAK -> appString(R.string.moment_share_text_streak, m.value, inv.link)
                    ShareMoments.Kind.BADGE -> appString(R.string.moment_share_text_badge, m.label, inv.link)
                }
                WeeklyCardShare.share(requireContext(), bmp, text)
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        (activity as? MainActivity)?.pumpPostLessonQueue("ShareMoment.dismiss")
    }
}

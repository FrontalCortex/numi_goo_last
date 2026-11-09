package com.example.app

import android.content.Intent
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentCreditsBinding
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

/**
 * Ayarlar → Destek → "Kaynaklar ve lisanslar" (10.11.2026, yayından önce telif için).
 *
 * Atıf metinleri lisansların istediği biçimde ve İngilizce bırakıldı (çevrilmemeli); yalnızca
 * başlıklar uygulama dilinde. Yeni bir dış kaynaklı çizim/ses/animasyon eklenince buraya da eklenmeli.
 * Lisans dayanakları: docs/DEVAM_NOTU.md "Kaynaklar ve lisanslar" bölümü.
 *
 * Alttaki "Açık kaynak lisansları" Google'ın oss-licenses eklentisinin bağımlılıklardan ürettiği
 * listeyi açar (Apache/MIT lisansları dağıtımda lisans metnini istiyor).
 */
class CreditsFragment : Fragment() {

    private data class Credit(
        @StringRes val section: Int,
        val attribution: String,
        val url: String,
        val licenseName: String? = null,
        val licenseUrl: String? = null,
        val modified: Boolean = false,
    )

    companion object {
        private const val CC_BY_4 = "https://creativecommons.org/licenses/by/4.0/"

        private val CREDITS = listOf(
            // Uygulama içindeki Sobi (BunnyMascotArt): Freepik "selection of cute animals in flat design",
            // magnific.com no. 1040416, "Free for commercial use WITH ATTRIBUTION"; istenen metin bu.
            Credit(R.string.credits_mascot, "designed by Freepik - Magnific.com", "https://www.magnific.com", modified = true),
            // AvatarArt: DiceBear "personas" = Draftbit Personas, CC BY 4.0 (değiştirildi: renk karışımı).
            Credit(R.string.credits_avatars, "Personas by Draftbit", "https://personas.draftbit.com",
                "CC BY 4.0", CC_BY_4, modified = true),
            // assets/*_anim.json "emoji_*" katmanlı Lottie'ler (rozet, madalya, hayvanlar…).
            Credit(R.string.credits_emoji, "Noto Emoji Animation by Google",
                "https://googlefonts.github.io/noto-emoji-animation/", "CC BY 4.0", CC_BY_4, modified = true),
            Credit(R.string.credits_icons, "Designed by Magnific", "https://www.magnific.com"),
            Credit(R.string.credits_sounds, "Sound Effect by floraphonic from Pixabay", "https://pixabay.com"),
            Credit(R.string.credits_animations, "LottieFiles", "https://lottiefiles.com"),
            Credit(R.string.credits_font, "Baloo 2 by Ek Type", "https://github.com/EkType/Baloo2",
                "SIL Open Font License 1.1", "https://openfontlicense.org"),
        )
    }

    private var _binding: FragmentCreditsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreditsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        CREDITS.forEach { binding.creditsList.addView(row(it)) }
        binding.btnOpenSource.setOnClickListener {
            OssLicensesMenuActivity.setActivityTitle(appString(R.string.credits_open_source))
            startActivity(Intent(requireContext(), OssLicensesMenuActivity::class.java))
        }
    }

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun text(s: String, sp: Float, color: Int, bold: Boolean = false) = TextView(requireContext()).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(color)
        setTypeface(typeface, if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }

    private fun link(label: String, url: String) = text(label, 14f, 0xFF58A6FF.toInt()).apply {
        paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
        setPadding(0, dp(4), 0, dp(2))
        setOnClickListener { openUrl(url) }
    }

    private fun row(c: Credit): View = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            setColor(0x00000000)
            cornerRadius = dp(20).toFloat()
            setStroke(dp(2), 0x66FFFFFF)
        }
        setPadding(dp(18), dp(14), dp(18), dp(14))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { bottomMargin = dp(10) }
        addView(text(appString(c.section), 13f, 0xFF9FB3BF.toInt(), bold = true))
        addView(text(c.attribution, 17f, 0xFFE6EEF2.toInt(), bold = true).apply { setPadding(0, dp(4), 0, 0) })
        addView(link(Uri.parse(c.url).host?.removePrefix("www.") ?: c.url, c.url))
        if (c.licenseName != null && c.licenseUrl != null) addView(link(c.licenseName, c.licenseUrl))
        if (c.modified) addView(text(appString(R.string.credits_modified), 13f, 0xFF9FB3BF.toInt()).apply { setPadding(0, dp(4), 0, 0) })
    }

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

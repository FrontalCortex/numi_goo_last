package com.example.app

import android.graphics.Color
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.app.abacus.SobiTestLevels
import com.example.app.abacus.SobiTestPlanner
import com.example.app.homework.DepthPress
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * Sobi'nin testi paneli (kullanıcı isteği 01.11.2026: "Sobi'nin bugünkü testi" penceresi güzelleşsin).
 * Haritadaki Sobi düğmesinden ([MapSobiTestButton]) alttan açılır; hemen açılır, veri gelene dek çark döner.
 *
 *  - Üstte selam veren Sobi ([BunnyMascotView]) ve başlık; bugünün testi bittiyse "Bir test daha".
 *  - Testin kuralları: adı, "Seviye 5/20" ve seviye çubuğu.
 *  - Zorluk kartları: Kolay (seviyenin bir altı) / Benim seviyem / Zor (bir üstü); her kartta seviye ve
 *    ne demek olduğu ("2 basamaklı 3 sayı, 2,4 sn"). Seçili kart mavi; kartlar içe göçer.
 *  - "Başla": sorular arka planda üretilir (düğme "Hazırlanıyor…"), sonra oynatıcı açılır. Ek testte
 *    düğme "Başla · 3" + anahtar görseli (Pro'da ücretsiz); anahtar sunucuda düşer, yetmezse Sobi söyler.
 * Veri yoksa Sobi "Biraz ders çöz…" der, düğme "Tamam" olur.
 */
class SobiTestSheet(private val fragment: Fragment) {

    private var dialog: BottomSheetDialog? = null
    private var offset = 0
    private val optionViews = ArrayList<Pair<Int, View>>()

    fun show() {
        if (!fragment.isAdded || dialog?.isShowing == true) return
        val ctx = fragment.requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.sheet_sobi_test, null)
        val mascot = view.findViewById<BunnyMascotView>(R.id.sobiTestMascot)
        val start = view.findViewById<TextView>(R.id.sobiTestStart)
        val later = view.findViewById<View>(R.id.sobiTestLater)
        val extra = SobiDailyTest.isDoneToday(ctx)
        if (extra) {
            view.findViewById<TextView>(R.id.sobiTestTitle).text = appString(R.string.sobi_test_dialog_extra_title)
            view.findViewById<TextView>(R.id.sobiTestSubtitle).text = appString(R.string.sobi_test_subtitle_extra)
        }
        start.isEnabled = false
        start.alpha = 0.5f
        DepthPress.attach(start)
        later.setOnClickListener { dismiss() }

        val d = BottomSheetDialog(ctx)
        d.setContentView(view)
        d.setOnShowListener {
            // Malzeme kütüphanesinin beyaz kabı görünmesin; köşeler panelin kendi arka planından.
            d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
            mascot.play(BunnyMascotView.Emote.GREET)
        }
        d.setOnDismissListener { if (dialog === d) dialog = null }
        dialog = d
        d.show()

        SobiDailyTest.load(ctx) { info ->
            if (!fragment.isAdded || dialog !== d) return@load
            view.findViewById<View>(R.id.sobiTestLoading).visibility = View.GONE
            val choice = SobiTestPlanner.choose(info.scores, info.levels, info.mentalSeen)
            if (choice == null) {
                showEmpty(view, start, appString(R.string.sobi_test_no_data))
                return@load
            }
            fill(view, choice)
            start.isEnabled = true
            start.alpha = 1f
            val cost = (fragment.activity as? MainActivity)?.let { SobiDailyTest.extraTestCost(it) } ?: 0
            if (cost > 0) start.text = keyCostLabel(start, cost)
            start.setOnClickListener { pay(view, start, mascot, info, cost) }
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    private fun fill(view: View, choice: SobiTestPlanner.Choice) {
        val ctx = view.context
        val inflater = LayoutInflater.from(ctx)
        val rules = view.findViewById<LinearLayout>(R.id.sobiTestRules)
        // Abaküssüz kural da kendi satırında ("10'luk toplama · zihinden"): çocuk ve veli testin zihinden
        // soru da içerdiğini başlamadan görsün. Zorluk kartları abaküslü zayıf kuralın seviyesinden.
        for (w in choice.weak + choice.mentalWeak) {
            val level = choice.current.getValue(w.key)
            val row = inflater.inflate(R.layout.item_sobi_test_rule, rules, false)
            row.findViewById<TextView>(R.id.ruleName).text = SobiDailyTest.ruleLabel(w.key).replaceFirstChar { it.uppercase() }
            row.findViewById<TextView>(R.id.ruleLevel).text = appString(R.string.sobi_test_level_of, level, SobiTestLevels.MAX)
            row.findViewById<LinearProgressIndicator>(R.id.ruleLevelBar).apply {
                max = SobiTestLevels.MAX
                progress = level
            }
            rules.addView(row)
        }

        val base = choice.current.getValue(choice.weak.first().key)
        val options = view.findViewById<LinearLayout>(R.id.sobiTestOptions)
        optionViews.clear()
        for ((o, label) in listOf(-1 to R.string.sobi_test_diff_easy, 0 to R.string.sobi_test_diff_mine, 1 to R.string.sobi_test_diff_hard)) {
            val level = (base + o).coerceIn(1, SobiTestLevels.MAX)
            // Seviye 1'de "Kolay", 20'de "Zor" aynı seviyeye düşüyor: anlamsız kart gösterme.
            if (o != 0 && level == base) continue
            val card = inflater.inflate(R.layout.item_sobi_test_option, options, false)
            card.findViewById<TextView>(R.id.optionTitle).text = appString(label)
            card.findViewById<TextView>(R.id.optionLevel).text = appString(R.string.sobi_test_level_short, level)
            card.findViewById<TextView>(R.id.optionDesc).text = SobiDailyTest.specText(level)
            DepthPress.attach(card)
            card.setOnClickListener { select(o) }
            options.addView(card)
            optionViews += o to card
        }
        view.findViewById<View>(R.id.sobiTestBody).visibility = View.VISIBLE
        select(0)
    }

    private fun select(o: Int) {
        offset = o
        for ((value, card) in optionViews) {
            val selected = value == o
            card.setBackgroundResource(if (selected) R.drawable.bg_sobi_test_option_selected else R.drawable.bg_hw_chip)
            card.findViewById<TextView>(R.id.optionLevel).setTextColor(if (selected) 0xFF1CB0F6.toInt() else 0xFF8B949E.toInt())
        }
    }

    /**
     * "Başla · 3" + uygulamanın anahtar görseli (üst paneldeki `key`). Emoji telefona göre farklı
     * çiziliyordu ve paneldeki anahtara benzemiyordu. Görsel metnin parçası (ImageSpan), düğme
     * genişliğine yaslanan bir compound drawable değil: metin ortada kalıyor ve düğme sonradan
     * "Hazırlanıyor…" yazınca simge de kendiliğinden gidiyor.
     */
    private fun keyCostLabel(start: TextView, cost: Int): CharSequence {
        val label = appString(R.string.sobi_test_start_keys, cost)
        val icon = ContextCompat.getDrawable(start.context, R.drawable.key)?.mutate() ?: return label
        val size = (start.textSize * 1.25f).toInt()
        icon.setBounds(0, 0, size, size)
        // ALIGN_CENTER API 29'da geldi; öncesinde taban çizgisi en yakın görünüş.
        val align = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) DynamicDrawableSpan.ALIGN_CENTER else DynamicDrawableSpan.ALIGN_BASELINE
        return SpannableStringBuilder(label).append(' ').apply {
            val at = length
            append('￼')
            setSpan(ImageSpan(icon, align), at, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /** Ek testin anahtarı (sunucuda); ücretsizse doğrudan başlar. */
    private fun pay(view: View, start: TextView, mascot: BunnyMascotView, info: SobiDailyTest.Info, cost: Int) {
        val ctx = fragment.context ?: return
        if (cost == 0 || com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
            begin(view, start, mascot, info)
            return
        }
        start.isEnabled = false
        start.text = appString(R.string.sobi_test_preparing)
        ServerSobi.startExtraTest(SobiDailyTest.testsToday(ctx), onResult = { _, keys ->
            if (!fragment.isAdded || dialog == null) return@startExtraTest
            SobiDailyTest.markExtraPaid(ctx)
            // Üst paneldeki anahtar: canlı dinleyici de getirir, beklemeden göster.
            (fragment.activity as? MainActivity)?.let { UserWalletFirestore.cacheKeys(it, keys); it.refreshWalletUi() }
            begin(view, start, mascot, info)
        }, onFailure = { e ->
            if (!fragment.isAdded || dialog == null) return@startExtraTest
            mascot.play(BunnyMascotView.Emote.THINK)
            val have = (fragment.activity as? MainActivity)?.let { UserWalletFirestore.getCachedKeys(it) } ?: 0
            val noKeys = ServerSobi.isNotEnoughKeys(e)
            showEmpty(view, start, if (noKeys) {
                appString(R.string.sobi_test_not_enough_keys, cost, have)
            } else appString(R.string.sobi_error_network))
            if (noKeys) offerPro(view)
        })
    }

    private fun begin(view: View, start: TextView, mascot: BunnyMascotView, info: SobiDailyTest.Info) {
        val ctx = fragment.context ?: return
        start.isEnabled = false
        start.text = appString(R.string.sobi_test_preparing)
        for ((_, card) in optionViews) card.isEnabled = false
        mascot.play(BunnyMascotView.Emote.THINK)
        SobiDailyTest.planAsync(ctx, info, offset) { plan ->
            if (!fragment.isAdded) return@planAsync
            if (plan == null) {
                showEmpty(view, start, appString(R.string.sobi_test_no_data))
                return@planAsync
            }
            dismiss()
            (fragment.activity as? MainActivity)?.let { SobiDailyTest.start(it, plan) }
        }
    }

    /**
     * Anahtar yetmedi: Pro'da ek testler ücretsiz (kullanıcı kararı 02.11.2026). Ders dönüşü tanıtımıyla
     * aynı kişilere ([MainActivity.isProTrialPromoEligible]) ve aynı ekran; deneme teklifi yoksa ekran
     * kendisi "Pro'ya geç" diyor.
     */
    private fun offerPro(view: View) {
        val main = fragment.activity as? MainActivity ?: return
        if (!main.isProTrialPromoEligible()) return
        view.findViewById<TextView>(R.id.sobiTestPro).apply {
            visibility = View.VISIBLE
            setOnClickListener {
                dismiss()
                SobiProTrialFragment.newInstance(AnalyticsLogger.PROMO_TRIGGER_SOBI_KEYS)
                    .show(main.supportFragmentManager, SobiProTrialFragment.TAG)
            }
        }
    }

    private fun showEmpty(view: View, start: TextView, text: String) {
        view.findViewById<View>(R.id.sobiTestLoading).visibility = View.GONE
        view.findViewById<View>(R.id.sobiTestBody).visibility = View.GONE
        view.findViewById<TextView>(R.id.sobiTestEmpty).apply {
            this.text = text
            visibility = View.VISIBLE
        }
        view.findViewById<View>(R.id.sobiTestLater).visibility = View.GONE
        start.isEnabled = true
        start.alpha = 1f
        start.text = appString(R.string.sobi_test_ok)
        start.setOnClickListener { dismiss() }
    }
}

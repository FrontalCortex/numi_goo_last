package com.example.app.homework

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.app.R
import com.example.app.appString
import com.example.app.databinding.FragmentRuleStatsBinding
import com.example.app.databinding.ItemRuleStatBinding
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Ödevlerde kural kural başarı: homeworkRuleStats/{uid} (sunucu topluyor, bkz. [RuleAnalysis] ve
 * functions/index.js onHomeworkProgressWritten).
 *
 * Bölümler: Toplama, Çıkarma (kuralsız, 5'lik, 10'luk, boncuk), Abaküslü çarpma ve Abaküssüz çarpma
 * (basamak çiftine göre). Verisi olmayan satır ve bölüm gösterilmiyor (kalabalık yapmasın; kullanıcı
 * kararı). Oran ilk denemeden itibaren, yanında deneme sayısıyla.
 *
 * Veli panelinden (uid null: kendi hesabı) ve kurs öğretmeninden (öğrencinin uid'si) açılıyor; okuma
 * izni kurallarda (öğrencinin kendisi ve sınıf öğretmeni).
 */
class HomeworkRuleStatsFragment : Fragment() {

    companion object {
        private const val ARG_STUDENT_UID = "student_uid"
        private const val ARG_STUDENT_NAME = "student_name"

        // Kullanıcının istediği sıra; listede olmayan çiftler sona, basamağa göre
        private val MUL_ABACUS = listOf("1x1", "2x1", "2x2", "3x1", "3x2", "4x1")
        private val MUL_MENTAL = listOf("1x1", "2x1", "2x2", "3x1", "3x2", "3x3", "4x1", "4x2", "4x3", "5x1", "5x2", "6x1")

        fun newInstance(studentUid: String?, studentName: String) = HomeworkRuleStatsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_STUDENT_UID, studentUid)
                putString(ARG_STUDENT_NAME, studentName)
            }
        }
    }

    private var _binding: FragmentRuleStatsBinding? = null
    private val binding get() = _binding!!

    /** Satır: ad, doğru, yanlış. */
    private data class Row(val label: String, val c: Int, val w: Int)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRuleStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        val studentUid = arguments?.getString(ARG_STUDENT_UID)
        if (studentUid != null) {
            binding.statsTitle.text = appString(R.string.rule_stats_title, arguments?.getString(ARG_STUDENT_NAME).orEmpty())
        }
        val uid = studentUid ?: FirebaseAuth.getInstance().currentUser?.uid ?: return showMessage(appString(R.string.ui_ilerleme_bilgisi_alinamadi))
        FirebaseFirestore.getInstance().collection("homeworkRuleStats").document(uid).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                @Suppress("UNCHECKED_CAST")
                render(doc.get("tally") as? Map<String, Any?> ?: emptyMap())
            }
            .addOnFailureListener {
                if (_binding != null) showMessage(appString(R.string.ui_ilerleme_bilgisi_alinamadi_internet_baglanti))
            }
    }

    private fun render(tally: Map<String, Any?>) {
        val b = _binding ?: return
        b.statsLoading.visibility = View.GONE
        fun cw(key: String): Pair<Int, Int> {
            val m = tally[key] as? Map<*, *> ?: return 0 to 0
            return ((m["c"] as? Number)?.toInt() ?: 0) to ((m["w"] as? Number)?.toInt() ?: 0)
        }
        fun ruleRows(op: HomeworkOp) = AbacusRule.values().map { r ->
            val (c, w) = cw(RuleAnalysis.ruleKey(op, r))
            Row(ruleLabel(r), c, w)
        }
        fun mulRows(prefix: String, order: List<String>): List<Row> {
            val pairs = tally.keys.filter { it.startsWith(prefix + "_") }.map { it.removePrefix(prefix + "_") }
            val sorted = pairs.sortedWith(compareBy({ order.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }, { it }))
            return sorted.map { p ->
                val (d1, d2) = p.split("x").map { it.toIntOrNull() ?: 0 }
                val (c, w) = cw("${prefix}_$p")
                Row(appString(R.string.rule_stats_mul_digits, d1, d2), c, w)
            }
        }
        val sections = listOf(
            appString(R.string.rule_stats_add) to ruleRows(HomeworkOp.ADD),
            appString(R.string.rule_stats_sub) to ruleRows(HomeworkOp.SUB),
            appString(R.string.rule_stats_mul_abacus) to mulRows("mulA", MUL_ABACUS),
            appString(R.string.rule_stats_mul_mental) to mulRows("mulM", MUL_MENTAL),
        ).map { (title, rows) -> title to rows.filter { it.c + it.w > 0 } }.filter { it.second.isNotEmpty() }

        if (sections.isEmpty()) return showMessage(appString(R.string.rule_stats_empty))
        b.statsContainer.removeAllViews()
        sections.forEach { (title, rows) -> b.statsContainer.addView(sectionCard(title, rows)) }
        b.statsContainer.addView(TextView(requireContext()).apply {
            text = appString(R.string.rule_stats_note)
            setTextColor(ContextCompat.getColor(context, R.color.dark_text_secondary))
            textSize = 12f
            setPadding(dp(4), dp(14), dp(4), 0)
        })
    }

    private fun ruleLabel(r: AbacusRule) = appString(
        when (r) {
            AbacusRule.DIRECT -> R.string.hw_rule_direct
            AbacusRule.FIVE -> R.string.hw_rule_five
            AbacusRule.TEN -> R.string.hw_rule_ten
            AbacusRule.BEAD -> R.string.hw_rule_bead
        }
    )

    private fun sectionCard(title: String, rows: List<Row>): View {
        val ctx = requireContext()
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(8))
        }
        body.addView(TextView(ctx).apply {
            text = title
            setTextColor(ContextCompat.getColor(ctx, R.color.dark_text))
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        rows.forEach { row ->
            val item = ItemRuleStatBinding.inflate(layoutInflater, body, false)
            val total = row.c + row.w
            val pct = Math.round(row.c * 100f / total)
            val color = when {
                pct >= 80 -> 0xFF8CC63F.toInt()
                pct >= 50 -> 0xFFFF9600.toInt()
                else -> 0xFFFF4B4B.toInt()
            }
            item.ruleName.text = row.label
            item.rulePercent.text = appString(R.string.rule_stats_percent, pct)
            item.rulePercent.setTextColor(color)
            item.ruleBar.setIndicatorColor(color)
            item.ruleBar.progress = pct
            item.ruleCount.text = appString(R.string.rule_stats_count, row.c, total)
            body.addView(item.root)
        }
        return MaterialCardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(12) }
            setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.bright_background_color))
            radius = dp(20).toFloat()
            cardElevation = 0f
            strokeWidth = 0
            addView(body)
        }
    }

    private fun showMessage(text: String) {
        val b = _binding ?: return
        b.statsLoading.visibility = View.GONE
        b.statsMessage.text = text
        b.statsMessage.visibility = View.VISIBLE
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

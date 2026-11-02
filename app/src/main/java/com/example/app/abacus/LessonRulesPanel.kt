package com.example.app.abacus

import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.example.app.R
import com.example.app.appString
import com.example.app.databinding.ViewRulesPanelBinding
import com.example.app.model.RulesFragment

/**
 * Sihirli değnek düğmesiyle açılan küçük kural tablosu ([R.layout.view_rules_panel]) ve kural
 * kitabının ([RulesFragment]) açılışı.
 *
 * BlindingLessonFragment (harita/kupa dersi) ile ödev oynatıcısında ortak. Hangi tablonun gösterileceği
 * ekranın işi ([activeTable]'ı ekran ayarlıyor ya da çocuk kitaptan seçiyor: [select]); burada yalnızca
 * tablonun gösterilmesi, gizlenmesi ve çarpım tablosunun yuvaya şişirilmesi var.
 *
 * @param fiveIsSubtraction 5'li tablonun başlığı çıkarma diliyle mi yazılsın ("çıkarılacak sayı");
 *   null: başlık olduğu gibi kalır.
 */
class LessonRulesPanel(
    private val panel: ViewRulesPanelBinding,
    private val inflater: LayoutInflater,
    private val fiveIsSubtraction: () -> Boolean?,
) {

    enum class Table {
        NONE, FIVE, TEN_FIVE, TEN, BEAD, BEAD_EXTRACTION, TEN_EXTRACTION, MULTIPLICATION
    }

    var activeTable = Table.NONE
    private var selectedMultiplicationDigit: Int? = null
    private var inflatedMultiplicationDigit: Int? = null

    val isVisible: Boolean get() = panel.root.visibility == View.VISIBLE

    /** Gösterilecek bir tablo var mı (çarpımda rakam da seçilmiş olmalı). */
    fun hasContentToShow(): Boolean {
        if (activeTable == Table.NONE) return false
        if (activeTable == Table.MULTIPLICATION && selectedMultiplicationDigit == null) return false
        return true
    }

    /** Kural kitabında seçilen tablo; göstermeyi çağıran yapıyor (ekran önce ipucunu kapatabilsin). */
    fun select(selection: RulesFragment.RulesTableSelection) {
        activeTable = when (selection) {
            RulesFragment.RulesTableSelection.FIVE -> Table.FIVE
            RulesFragment.RulesTableSelection.EXTRACTION_FIVE -> Table.FIVE
            RulesFragment.RulesTableSelection.TEN_FIVE -> Table.TEN_FIVE
            RulesFragment.RulesTableSelection.TEN -> Table.TEN
            RulesFragment.RulesTableSelection.TEN_EXTRACTION -> Table.TEN_EXTRACTION
            RulesFragment.RulesTableSelection.BEAD -> Table.BEAD
            RulesFragment.RulesTableSelection.BEAD_EXTRACTION -> Table.BEAD_EXTRACTION
            is RulesFragment.RulesTableSelection.MULTIPLICATION -> {
                selectedMultiplicationDigit = selection.digit
                Table.MULTIPLICATION
            }
        }
    }

    fun hideAll() {
        panel.root.visibility = View.GONE
        panel.fiveRuleTableLinearLayout.visibility = View.GONE
        panel.tenRuleFiveTableLayout.visibility = View.GONE
        panel.tenRuleTableLinearLayout.visibility = View.GONE
        panel.BeadRuleTable.visibility = View.GONE
        panel.BeadRuleExtractionTable.visibility = View.GONE
        panel.tenRuleExtractionTableLayout.visibility = View.GONE
        panel.rulesPanelMultiplicationSlot.visibility = View.GONE
    }

    fun closeIfOpen() {
        if (isVisible) hideAll()
    }

    fun show() {
        hideAll()
        val table = when (activeTable) {
            Table.FIVE -> {
                applyFiveText()
                panel.fiveRuleTableLinearLayout
            }
            Table.TEN_FIVE -> panel.tenRuleFiveTableLayout
            Table.TEN -> panel.tenRuleTableLinearLayout
            Table.BEAD -> panel.BeadRuleTable
            Table.BEAD_EXTRACTION -> panel.BeadRuleExtractionTable
            Table.TEN_EXTRACTION -> panel.tenRuleExtractionTableLayout
            Table.MULTIPLICATION -> selectedMultiplicationDigit?.let {
                ensureMultiplicationInSlot(it)
                panel.rulesPanelMultiplicationSlot
            }
            Table.NONE -> null
        } ?: return
        panel.root.visibility = View.VISIBLE
        table.visibility = View.VISIBLE
        panel.root.bringToFront()
    }

    /** Açıksa kapatır, kapalıysa gösterir; ekran [show]'dan önce ipucunu kapatmak isterse [show] onu çağırır. */
    fun toggle(show: () -> Unit = ::show) {
        if (activeTable == Table.NONE) return
        if (isVisible) hideAll() else show()
    }

    private fun applyFiveText() {
        val subtraction = fiveIsSubtraction() ?: return
        if (subtraction) {
            panel.fiveText.text = appString(R.string.ui_cikarilacak_sayi)
            panel.fiveRuleDescriptionText.text = appString(R.string.ui_5_gider_kardes_gelir)
        } else {
            panel.fiveText.text = appString(R.string.ui_eklenecek_sayi)
            panel.fiveRuleDescriptionText.text = appString(R.string.ui_5_gelir_kardes_gider)
        }
    }

    private fun ensureMultiplicationInSlot(digit: Int) {
        val slot = panel.rulesPanelMultiplicationSlot
        if (inflatedMultiplicationDigit != digit) {
            slot.removeAllViews()
            inflater.inflate(multiplicationLayoutForDigit(digit), slot, true)
            inflatedMultiplicationDigit = digit
        }
    }

    companion object {
        const val RULES_BOOK_TAG = "rules_fragment"

        private fun multiplicationLayoutForDigit(digit: Int): Int = when (digit) {
            1 -> R.layout.multiplication_table_1
            2 -> R.layout.multiplication_table_2
            3 -> R.layout.multiplication_table_3
            4 -> R.layout.multiplication_table_4
            5 -> R.layout.multiplication_table_5
            6 -> R.layout.multiplication_table_6
            7 -> R.layout.multiplication_table_7
            8 -> R.layout.multiplication_table_8
            9 -> R.layout.multiplication_table_9
            else -> R.layout.multiplication_table_1
        }

        /** Kural kitabını [containerId]'ye yukarıdan kayarak açar; ayarlanabilsin diye açılan parçayı döner. */
        fun openRulesBook(fm: FragmentManager, containerId: Int): RulesFragment? {
            fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_down, 0)
                .replace(containerId, RulesFragment(), RULES_BOOK_TAG)
                .commit()
            fm.executePendingTransactions()
            return fm.findFragmentByTag(RULES_BOOK_TAG) as? RulesFragment
        }

        /** Geri tuşu: kural kitabı açıksa önce o kapansın. Kapattıysa true. */
        fun closeRulesBookIfOpen(fm: FragmentManager): Boolean {
            val rules = fm.findFragmentByTag(RULES_BOOK_TAG) as? RulesFragment ?: return false
            if (!rules.isVisible) return false
            rules.closeWithAnimation()
            return true
        }

        /**
         * Kitap "tablo seç" kipinde: toplama tabloları açık, çocuk bir tabloya dokununca [onSelected]
         * (seçilen tablo değnek panelinde gösterilir). Çarpım tabloları varsayılan kapalı.
         */
        fun enableTablePicker(rules: RulesFragment?, onSelected: (RulesFragment.RulesTableSelection) -> Unit) {
            rules ?: return
            rules.setActiveRulesContentSection(RulesFragment.RulesContentSection.ADDITION)
            rules.updateFiveRuleTableVisibility(View.VISIBLE)
            rules.updateTenRuleTableVisibility(View.VISIBLE)
            rules.updateTenRuleFiveTableVisibility(View.VISIBLE)
            rules.updateBeadRuleTableVisibility(View.VISIBLE)
            rules.updateMultiplicationTablesVisibility(View.GONE)
            rules.setRulesTablePickerEnabled(true)
            rules.setOnRulesTableSelectedListener(onSelected)
        }
    }
}

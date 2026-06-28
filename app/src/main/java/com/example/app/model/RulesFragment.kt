package com.example.app.model

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.app.R
import com.example.app.databinding.FragmentRulesBinding

class RulesFragment : Fragment() {

    sealed class RulesTableSelection {
        data object FIVE : RulesTableSelection()
        data object TEN_FIVE : RulesTableSelection()
        data object TEN : RulesTableSelection()
        data object BEAD : RulesTableSelection()
        data class MULTIPLICATION(val digit: Int) : RulesTableSelection()
    }

    enum class RulesContentSection {
        /** Toplama tabloları — [multipleLayout] */
        ADDITION,
        /** Çıkarma tabloları — [extractionLayout] */
        EXTRACTION,
    }

    companion object {
        var lastScrollY: Int = 0
    }

    private var _binding: FragmentRulesBinding? = null
    private val binding get() = _binding!!

    private var activeSection: RulesContentSection = RulesContentSection.ADDITION
    private var rulesTablePickerEnabled = false
    private var onRulesTableSelectedListener: ((RulesTableSelection) -> Unit)? = null
    private val multiplicationTableViews by lazy {
        listOf(
            binding.multiplicationTableOnes.root to 1,
            binding.multiplicationTableTwos.root to 2,
            binding.multiplicationTableThrees.root to 3,
            binding.multiplicationTableFours.root to 4,
            binding.multiplicationTableFives.root to 5,
            binding.multiplicationTableSixes.root to 6,
            binding.multiplicationTableSevens.root to 7,
            binding.multiplicationTableEights.root to 8,
            binding.multiplicationTableNines.root to 9,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupQuitButton()
        setActiveRulesContentSection(RulesContentSection.ADDITION, resetScroll = false)
        updateMultiplicationTablesVisibility(View.GONE)
        binding.rulesScrollView.post {
            binding.rulesScrollView.scrollTo(0, lastScrollY)
        }
    }

    private fun setupQuitButton() {
        binding.quitButton.setOnClickListener {
            closeWithAnimation()
        }
    }

    fun closeWithAnimation() {
        lastScrollY = binding.rulesScrollView.scrollY
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(0, R.anim.slide_up)
            .remove(this@RulesFragment)
            .commit()
    }

    interface RulesVisibilityListener {
        fun onRulesVisibilityChanged(visibility: Int)
    }

    private var visibilityListener: RulesVisibilityListener? = null

    fun setVisibilityListener(listener: RulesVisibilityListener) {
        this.visibilityListener = listener
    }

    /** ScrollView içinde toplama / çıkarma kök layout'u arasında geçiş. */
    fun setActiveRulesContentSection(
        section: RulesContentSection,
        resetScroll: Boolean = true,
    ) {
        activeSection = section
        when (section) {
            RulesContentSection.ADDITION -> {
                binding.multipleLayout.visibility = View.VISIBLE
                binding.extractionLayout.visibility = View.GONE
            }
            RulesContentSection.EXTRACTION -> {
                binding.multipleLayout.visibility = View.GONE
                binding.extractionLayout.visibility = View.VISIBLE
            }
        }
        if (resetScroll) {
            binding.rulesScrollView.post { binding.rulesScrollView.scrollTo(0, 0) }
        }
    }

    fun getActiveRulesContentSection(): RulesContentSection = activeSection

    fun setRulesTablePickerEnabled(enabled: Boolean) {
        rulesTablePickerEnabled = enabled
        if (enabled) {
            setupRulesTableClickListeners()
        } else {
            clearRulesTableClickListeners()
        }
    }

    fun setOnRulesTableSelectedListener(listener: ((RulesTableSelection) -> Unit)?) {
        onRulesTableSelectedListener = listener
    }

    private fun setupRulesTableClickListeners() {
        if (!rulesTablePickerEnabled || _binding == null) return
        binding.fiveRuleTableLinearLayout.setRulesTablePickerClick(RulesTableSelection.FIVE)
        binding.tenRuleFiveTableLayout.setRulesTablePickerClick(RulesTableSelection.TEN_FIVE)
        binding.tenRuleTableLinearLayout.setRulesTablePickerClick(RulesTableSelection.TEN)
        binding.BeadRuleTable.setRulesTablePickerClick(RulesTableSelection.BEAD)
        multiplicationTableViews.forEach { (view, digit) ->
            view.setRulesTablePickerClick(RulesTableSelection.MULTIPLICATION(digit))
        }
    }

    private fun clearRulesTableClickListeners() {
        if (_binding == null) return
        listOf(
            binding.fiveRuleTableLinearLayout,
            binding.tenRuleFiveTableLayout,
            binding.tenRuleTableLinearLayout,
            binding.BeadRuleTable,
        ).forEach { view ->
            view.isClickable = false
            view.setOnClickListener(null)
        }
        multiplicationTableViews.forEach { (view, _) ->
            view.isClickable = false
            view.setOnClickListener(null)
        }
    }

    private fun View.setRulesTablePickerClick(selection: RulesTableSelection) {
        isClickable = true
        setOnClickListener {
            if (visibility != View.VISIBLE) return@setOnClickListener
            onRulesTableSelectedListener?.invoke(selection)
            closeWithAnimation()
        }
    }

    // —— Toplama (multipleLayout) tabloları ——

    fun updateFiveRuleTableVisibility(visibility: Int) {
        binding.fiveRuleTableLinearLayout.visibility = visibility
        visibilityListener?.onRulesVisibilityChanged(visibility)
    }

    fun updateTenRuleFiveTableVisibility(visibility: Int) {
        binding.tenRuleFiveTableLayout.visibility = visibility
        visibilityListener?.onRulesVisibilityChanged(visibility)
    }

    fun updateTenRuleTableVisibility(visibility: Int) {
        Log.d("kehribar", "worked3")
        binding.tenRuleTableLinearLayout.visibility = visibility
        visibilityListener?.onRulesVisibilityChanged(visibility)
    }

    fun updateBeadRuleTableVisibility(visibility: Int) {
        binding.BeadRuleTable.visibility = visibility
        Log.d("kehribar", "worked3")
        visibilityListener?.onRulesVisibilityChanged(visibility)
    }

    /** Çarpım tabloları (1x–9x) ve ayırıcıları [view6]–[view14] — tek çağrıda hepsi. */
    fun updateMultiplicationTablesVisibility(visibility: Int) {
        binding.view6.visibility = visibility
        binding.multiplicationTableOnes.root.visibility = visibility
        binding.view7.visibility = visibility
        binding.multiplicationTableTwos.root.visibility = visibility
        binding.view8.visibility = visibility
        binding.multiplicationTableThrees.root.visibility = visibility
        binding.view9.visibility = visibility
        binding.multiplicationTableFours.root.visibility = visibility
        binding.view10.visibility = visibility
        binding.multiplicationTableFives.root.visibility = visibility
        binding.view11.visibility = visibility
        binding.multiplicationTableSixes.root.visibility = visibility
        binding.view12.visibility = visibility
        binding.multiplicationTableSevens.root.visibility = visibility
        binding.view13.visibility = visibility
        binding.multiplicationTableEights.root.visibility = visibility
        binding.view14.visibility = visibility
        binding.multiplicationTableNines.root.visibility = visibility
        visibilityListener?.onRulesVisibilityChanged(visibility)
    }

    fun updateRulesDividerVisibilities(
        view1Visibility: Int,
        view2Visibility: Int,
        view3Visibility: Int,
    ) {
        binding.view1.visibility = view1Visibility
        binding.view2.visibility = view2Visibility
        binding.view3.visibility = view3Visibility
    }

    // —— Çıkarma (extractionLayout) tabloları ——

    fun updateExtractionFiveRuleTableVisibility(visibility: Int) {
        binding.extractionFiveRuleTable.visibility = visibility
        visibilityListener?.onRulesVisibilityChanged(visibility)
        binding.view4.visibility = View.GONE
    }
    fun updateTenRuleExtractionTableLayout(visibility: Int) {
        binding.tenRuleExtractionTableLayout.visibility = visibility
        visibilityListener?.onRulesVisibilityChanged(visibility)
        binding.view4.visibility = visibility

    }
    fun updateBeadRuleExtractionTableLayout(visibility: Int) {
        binding.BeadRuleExtractionTable.visibility = visibility
        visibilityListener?.onRulesVisibilityChanged(visibility)
        binding.view5.visibility = visibility

    }

    /** Toplama + çıkarma tablo başlık hücrelerini güncelle. */
    fun applyRulesTableHeaderTexts(isSubtractionPart: Boolean) {
        val label = if (isSubtractionPart) "Çıkarılacak Sayı" else "Eklenecek Sayı"
        binding.fiveText.text = label
        binding.tenText.text = label
        binding.tenTextSecond.text = label
        binding.beadText.text = label
        binding.extractionFiveText.text = label
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

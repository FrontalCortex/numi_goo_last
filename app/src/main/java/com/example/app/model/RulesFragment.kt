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

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
    companion object {
        // Rules paneli tekrar açıldığında son scroll konumunu korumak için
        var lastScrollY: Int = 0
    }

    private var _binding: FragmentRulesBinding? = null
    private val binding get() = _binding!!

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

        // Panel tekrar açıldığında son kaydırma konumuna dön
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
        // Mevcut scroll konumunu sakla
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

    fun updateFiveRuleTableVisibility(visibility: Int) {
        binding.fiveRuleTableLinearLayout.visibility = visibility
        visibilityListener?.onRulesVisibilityChanged(visibility)
    }
    fun updateTenRuleFiveTableVisibility(visibility: Int) {
        binding.tenRuleFiveTableLayout.visibility = visibility
        visibilityListener?.onRulesVisibilityChanged(visibility)
    }
    fun updateTenRuleTableVisibility(visibility: Int) {
        Log.d("kehribar","worked3")
        binding.tenRuleTableLinearLayout.visibility = visibility
        visibilityListener?.onRulesVisibilityChanged(visibility)
    }
    fun updateBeadRuleTableVisibility(visibility: Int) {
        binding.BeadRuleTable.visibility = visibility
        Log.d("kehribar","worked3")
        visibilityListener?.onRulesVisibilityChanged(visibility)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
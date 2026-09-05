package com.example.app

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.app.databinding.FragmentProDiffirentBinding

class ProDiffirentFragment : DialogFragment() {

    private var _binding: FragmentProDiffirentBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setWindowAnimations(R.style.DialogAnimationSlideRight)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProDiffirentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Title text formatting
        val titleHtml = "Pro ile daha hızlı öğren, daha fazla pratik yap!"
        binding.tvTitle.text = Html.fromHtml(titleHtml, Html.FROM_HTML_MODE_LEGACY)

        // Click listeners
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        SubscriptionCta.apply((activity as? MainActivity)?.billingManager, binding.btnTryFreeText)

        binding.btnTryFree.setOnClickListener {
            // Yeni fragmenti hemen açıyoruz
            PlanFragment().show(requireActivity().supportFragmentManager, "Plan")
            
            // Altında kalan bu fragmenti animasyon süresi kadar arkada bekletip sessizce kapatıyoruz.
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    dialog?.window?.setWindowAnimations(0)
                    dismiss()
                } catch (e: Exception) {}
            }, 500)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

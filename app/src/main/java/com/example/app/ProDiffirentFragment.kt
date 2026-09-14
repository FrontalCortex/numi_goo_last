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

    /**
     * Kullanıcının bu panele hangi kapıdan geldiği. [arguments] üzerinden taşınır, constructor'dan
     * değil: sistem dialog'u kendi yeniden oluşturduğunda constructor parametresi kaybolurdu.
     */
    private val entryPoint: String
        get() = arguments?.getString(ARG_ENTRY_POINT) ?: AnalyticsLogger.PRO_ENTRY_UNKNOWN

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

        // Kapı hem bu olaya hem de akışın devamındaki satın alma olaylarına yazılır; bkz. [ProFlow].
        ProFlow.start(entryPoint)
        AnalyticsLogger.logProPanelShown(entryPoint)

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

    companion object {
        private const val ARG_ENTRY_POINT = "pro_entry_point"

        /**
         * Paneli her zaman bununla aç.
         *
         * @param entryPoint [AnalyticsLogger.PRO_ENTRY_AD_SKIP], [AnalyticsLogger.PRO_ENTRY_ASK_QUESTION]
         *   veya [AnalyticsLogger.PRO_ENTRY_SHOP]. Kapı bilgisi olmadan huni dört ayrı girişi tek
         *   havuzda toplar ve hangi kapının dönüştüğü sorulamaz.
         */
        fun newInstance(entryPoint: String): ProDiffirentFragment {
            val fragment = ProDiffirentFragment()
            fragment.arguments = Bundle().apply { putString(ARG_ENTRY_POINT, entryPoint) }
            return fragment
        }
    }
}

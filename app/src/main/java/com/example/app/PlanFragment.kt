package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.app.databinding.FragmentPlanBinding

class PlanFragment : DialogFragment() {

    private var _binding: FragmentPlanBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Light_NoTitleBar_Fullscreen)
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
        _binding = FragmentPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var selectedPlan = "Pro"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardBireysel.setOnClickListener {
            selectedPlan = "Pro"
            binding.cardBireysel.setBackgroundResource(R.drawable.bg_plan_bireysel)
            binding.cardLite.setBackgroundResource(R.drawable.bg_plan_lite)
            binding.imgBadgeBireysel.visibility = View.VISIBLE
            binding.imgBadgeLite.visibility = View.GONE
        }

        binding.cardLite.setOnClickListener {
            selectedPlan = "Lite"
            binding.cardLite.setBackgroundResource(R.drawable.bg_plan_bireysel)
            binding.cardBireysel.setBackgroundResource(R.drawable.bg_plan_lite)
            binding.imgBadgeLite.visibility = View.VISIBLE
            binding.imgBadgeBireysel.visibility = View.GONE
        }

        binding.btnTryFree.setOnClickListener {
            // `plan` alanını istemci yazamaz (firestore.rules kilitli tutuyor). Play'den gelen
            // abonelik token'ı sunucuda doğrulandıktan sonra Admin SDK ile yazılır.
            val mainActivity = activity as? MainActivity ?: run { dismiss(); return@setOnClickListener }
            val productId = BillingCatalog.subscriptionForPlanName(selectedPlan)
            mainActivity.billingManager.launchPurchase(mainActivity, productId)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

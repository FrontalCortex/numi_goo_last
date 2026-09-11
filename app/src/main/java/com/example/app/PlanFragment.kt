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
            // Deneme uygunluğu ürüne göre değişebilir; düğme metni seçimi takip etmeli.
            applyLivePrices()
        }

        binding.cardLite.setOnClickListener {
            selectedPlan = "Lite"
            binding.cardLite.setBackgroundResource(R.drawable.bg_plan_bireysel)
            binding.cardBireysel.setBackgroundResource(R.drawable.bg_plan_lite)
            binding.imgBadgeLite.visibility = View.VISIBLE
            binding.imgBadgeBireysel.visibility = View.GONE
            applyLivePrices()
        }

        applyLivePrices()

        binding.btnTryFree.setOnClickListener {
            // `plan` alanını istemci yazamaz (firestore.rules kilitli tutuyor). Play'den gelen
            // abonelik token'ı sunucuda doğrulandıktan sonra Admin SDK ile yazılır.
            val mainActivity = activity as? MainActivity ?: run { dismiss(); return@setOnClickListener }
            val productId = BillingCatalog.subscriptionForPlanName(selectedPlan)
            mainActivity.billingManager.launchPurchase(mainActivity, productId)
            dismiss()
        }
    }

    /**
     * Fiyatları Play'den gelen değerlerle yazar.
     *
     * XML'deki rakamlar yalnızca yer tutucudur: Play fiyatı kullanıcının ülkesine ve para
     * birimine göre yerelleştirir, sabit metin yabancı kullanıcıya yanlış tutar gösterir.
     * Ürün bilgisi henüz gelmediyse yer tutucu korunur.
     */
    private fun applyLivePrices() {
        val billing = (activity as? MainActivity)?.billingManager ?: return

        billing.formattedPrice(BillingCatalog.SUB_PRO)?.let {
            binding.tvBireyselPriceRight.text = getString(R.string.plan_price_monthly, it)
        }
        billing.formattedPrice(BillingCatalog.SUB_LITE)?.let {
            binding.tvLitePriceRight.text = getString(R.string.plan_price_monthly, it)
        }

        applyYearlyEquivalent(billing, BillingCatalog.SUB_PRO, binding.tvBireyselPriceSub)
        applyYearlyEquivalent(billing, BillingCatalog.SUB_LITE, binding.tvLitePriceSub)

        // Düğme metni de fiyat gibi Play'den geliyor: deneme yalnızca uygun kullanıcıya
        // vaat edilmeli (bkz. SubscriptionCta).
        SubscriptionCta.apply(
            billing,
            binding.btnTryFreeText,
            BillingCatalog.subscriptionForPlanName(selectedPlan),
        )
    }

    /** Aylık tutarın 12 ile çarpımı; satın alınabilir bir yıllık plan değil, karşılaştırma. */
    private fun applyYearlyEquivalent(
        billing: BillingManager,
        productId: String,
        target: android.widget.TextView,
    ) {
        val (micros, currencyCode) = billing.subscriptionPriceAmount(productId) ?: return
        val yearly = micros * 12.0 / 1_000_000.0
        val formatter = java.text.NumberFormat.getCurrencyInstance()
        formatter.currency = runCatching { java.util.Currency.getInstance(currencyCode) }.getOrNull()
            ?: return
        target.text = getString(R.string.plan_price_yearly, formatter.format(yearly))
    }

    override fun onResume() {
        super.onResume()
        // Ürün bilgisi uygulama açılışında yükleniyor; ekran o tamamlanmadan açılmış olabilir.
        applyLivePrices()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

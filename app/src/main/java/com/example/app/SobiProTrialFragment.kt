package com.example.app

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.app.databinding.FragmentSobiProTrialBinding

/**
 * Sobi'nin Pro deneme tanıtımı (kullanıcı kararı 02.11.2026: "İkisi birden").
 *
 * Eskiden ders dönüşünde [AskQuestionOpenFragment] açılıyordu: görünüşte "öğretmene sor"u anlatıyor,
 * asıl işi ücretsiz Pro denemesini satmaktı. Öğretmen soruları gizlenince ([TeacherQuestionsFeature])
 * o tanıtım hiç açılmaz oldu. Pro'nun bugünkü asıl farkı Sobi olduğu için ekranı artık Sobi anlatıyor.
 * Öğretmen düğmesinden açılan yol ([AskQuestionButtonBinder]) bayrak açılırsa eski ekranı açmaya devam
 * ediyor; bu ekranda öğretmen ya da danışma kredisi seçeneği yok.
 *
 * İki yerden açılıyor:
 *  - [AnalyticsLogger.PROMO_TRIGGER_SOBI_AUTO]: üçüncü ders dönüşünde haritada (MainActivity
 *    maybeShowAskQuestionPromo; kilit, bekçi ve kuyruk aynı).
 *  - [AnalyticsLogger.PROMO_TRIGGER_SOBI_LIMIT]: Sobi ekranında ücretsiz mesaj ve kredi bitince
 *    "Pro'yu ücretsiz dene" bağlantısı ([MistakeReviewDialog]). Kendi penceresi olduğu için dersin ve
 *    öğreticinin önünde açılıyor.
 *
 * Ana düğme PlanFragment'in satın alma yolu: Play abonelik penceresi doğrudan açılıyor. Metin sabit
 * değil ([SubscriptionCta]): Play ücretsiz denemeyi Google hesabı başına bir kez veriyor; teklif yoksa
 * başlık ve düğme deneme vaat etmiyor.
 */
class SobiProTrialFragment : DialogFragment() {

    private var _binding: FragmentSobiProTrialBinding? = null
    private val binding get() = _binding!!

    private val trigger: String
        get() = arguments?.getString(ARG_TRIGGER) ?: AnalyticsLogger.PROMO_TRIGGER_SOBI_AUTO

    private var shownAtMs = 0L
    private var viewNoBucket = ""
    private var closeLogged = false
    private var welcomeCreditAvailable = true

    /**
     * Kapanınca (hangi yoldan olursa olsun) bir kez. Sobi ekranı bununla dersin süresini sürdürüyor.
     * Yapılandırma değişikliğinde kaybolur; o durumda ders süresi kullanıcı ekrana dönünce zaten
     * yeniden başlıyor (MistakeReviewDialog'un mağaza yolundaki kabulün aynısı).
     */
    var onClosed: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    /** [AskQuestionOpenFragment.onStart] ile aynı gerekçe: kayarken alttaki harita açığa çıkıyordu. */
    override fun onStart() {
        super.onStart()
        dialog?.window?.setWindowAnimations(0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSobiProTrialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.let { w ->
            SystemBarColors.applyToDialog(
                w,
                top = ContextCompat.getColor(requireContext(), R.color.paywall_navy),
                bottom = android.graphics.Color.WHITE,
                topView = binding.root,
                bottomView = binding.bottomWhiteContainer,
            )
        }

        welcomeCreditAvailable = WelcomeCreditEligibility.isEligible(requireContext())
        val billing = (activity as? MainActivity)?.billingManager
        binding.titleText.text = titleText(requireContext(), billing?.freeTrialDays(BillingCatalog.SUB_PRO))
        SubscriptionCta.apply(billing, binding.tryFreeText)
        bindBenefits()

        viewNoBucket = AskQuestionPromoStats.viewBucket(AskQuestionPromoStats.nextViewNo(requireContext(), trigger))
        shownAtMs = SystemClock.elapsedRealtime()
        AnalyticsLogger.logAskQuestionPromoShown(
            trigger = trigger,
            viewNo = viewNoBucket,
            welcomeCreditAvailable = welcomeCreditAvailable,
        )

        // "Mükemmel": taç ve havalı gözlük iniyor, sonra pozda kalıyor (Pro hâli).
        binding.mascotView.play(BunnyMascotView.Emote.PERFECT)
        binding.btnTryFree.setOnClickListener { startTrial() }
        binding.btnNotNow.setOnClickListener {
            logClosed(AnalyticsLogger.PROMO_NO_THANKS)
            dismiss()
        }
    }

    /** Fiyat ve deneme uygunluğu Play'den geç gelebilir (SubscriptionCta'daki not). */
    override fun onResume() {
        super.onResume()
        val billing = (activity as? MainActivity)?.billingManager ?: return
        binding.titleText.text = titleText(requireContext(), billing.freeTrialDays(BillingCatalog.SUB_PRO))
        SubscriptionCta.apply(billing, binding.tryFreeText)
    }

    private fun bindBenefits() {
        val list = binding.benefitList
        val d = resources.displayMetrics.density
        val items = buildList {
            add(R.string.sobi_trial_benefit_messages)
            add(R.string.sobi_trial_benefit_tests)
            add(R.string.sobi_trial_benefit_energy)
            add(R.string.sobi_trial_benefit_no_ads)
            // Hoş geldin hediyesi cihaz başına bir kez (functions/index.js welcomeCreditGrants); alamayacak
            // cihaza vaat edilmiyor (PlanFragment'teki satırla aynı kural).
            if (welcomeCreditAvailable) add(R.string.sobi_trial_benefit_welcome)
        }
        val check = ContextCompat.getDrawable(requireContext(), R.drawable.correct_ic2)?.mutate()?.apply {
            setTint(0xFF00B0FF.toInt())
            setBounds(0, 0, (16 * d).toInt(), (16 * d).toInt())
        }
        items.forEach { res ->
            list.addView(TextView(requireContext()).apply {
                text = appString(res)
                setTextColor(0xFF4A4A4A.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                gravity = Gravity.CENTER_VERTICAL
                setCompoundDrawablesRelative(check, null, null, null)
                compoundDrawablePadding = (10 * d).toInt()
                setPadding(0, (5 * d).toInt(), 0, (5 * d).toInt())
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun startTrial() {
        val main = activity as? MainActivity ?: return dismiss()
        logClosed(AnalyticsLogger.PROMO_TRY_FREE)
        // Satın alma olayları bu kapıyı taşısın (ProDiffirentFragment'in yaptığı gibi).
        ProFlow.start(AnalyticsLogger.PRO_ENTRY_SOBI_TRIAL)
        // `plan` alanını istemci yazamaz; Play token'ı sunucuda doğrulanınca yazılıyor (PlanFragment).
        main.billingManager.launchPurchase(main, BillingCatalog.SUB_PRO)
        dismiss()
    }

    /** Geri tuşu / dışarı dokunma; onDismiss değil (AskQuestionOpenFragment.onCancel'deki gerekçe). */
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        logClosed(AnalyticsLogger.PROMO_DISMISSED)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onClosed?.invoke()
        onClosed = null
        // Ders dönüşü tanıtımı ders sonrası kuyruğunun bir adımı; kapanınca sıradakini dürtüyor.
        // Sobi ekranından açılan yol kuyruğun parçası değil (ders sürüyor).
        if (trigger == AnalyticsLogger.PROMO_TRIGGER_SOBI_AUTO) {
            (activity as? MainActivity)?.pumpPostLessonQueue("SobiProTrial.dismiss")
        }
    }

    private fun logClosed(outcome: String) {
        if (closeLogged || shownAtMs == 0L) return
        closeLogged = true
        AnalyticsLogger.logAskQuestionPromoClosed(
            trigger = trigger,
            viewNo = viewNoBucket,
            outcome = outcome,
            dwellMs = SystemClock.elapsedRealtime() - shownAtMs,
            welcomeCreditAvailable = welcomeCreditAvailable,
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_TRIGGER = "trigger"
        /**
         * Eski tanıtımın etiketi bilerek korunuyor: MainActivity'nin harita dönüşü kapıları
         * ("promo_showing", "already_showing") ekranı bu etiketle arıyor.
         */
        const val TAG = "AskQuestionOpen"

        fun newInstance(trigger: String): SobiProTrialFragment {
            val fragment = SobiProTrialFragment()
            fragment.arguments = Bundle().apply { putString(ARG_TRIGGER, trigger) }
            return fragment
        }

        /** Deneme teklifi varsa "1 hafta Pro'yu ücretsiz dene"; yoksa deneme vaat etmeyen başlık. */
        fun titleText(context: Context, trialDays: Int?): String = when {
            trialDays == null -> context.getString(R.string.sobi_trial_title_no_trial)
            trialDays % 7 == 0 -> context.getString(R.string.sobi_trial_title_weeks, trialDays / 7)
            else -> context.getString(R.string.sobi_trial_title_days, trialDays)
        }

        /**
         * Sobi ekranında deneme bağlantısı gösterilsin mi: tanıtımın uygunluğu
         * ([MainActivity.isProTrialPromoEligible]) VE Play'de bu kullanıcıya deneme teklifi var.
         * Teklifsiz kullanıcıya yalnızca "Kredi al" kalıyor.
         */
        fun canOfferTrial(activity: MainActivity?): Boolean {
            if (activity == null || !activity.isProTrialPromoEligible()) return false
            return activity.billingManager.freeTrialDays(BillingCatalog.SUB_PRO) != null
        }
    }
}

package com.example.app

import android.content.DialogInterface
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.app.databinding.FragmentAskQuestionOpenBinding

class AskQuestionOpenFragment : DialogFragment() {

    private var _binding: FragmentAskQuestionOpenBinding? = null
    private val binding get() = _binding!!

    /**
     * Ekranı hangi durumun açtığı. [arguments] üzerinden taşınır, constructor'dan değil:
     * sistem dialog'u kendi yeniden oluşturduğunda constructor parametresi kaybolurdu.
     */
    private val trigger: String
        get() = arguments?.getString(ARG_TRIGGER) ?: AnalyticsLogger.PROMO_TRIGGER_AUTO

    private var shownAtMs: Long = 0L
    private var viewNoBucket: String = ""
    private var closeLogged = false

    /** Cihaz Pro hoş geldin kredisini hâlâ alabiliyor mu; ekranın düzenini bu belirliyor. */
    private var welcomeCreditAvailable: Boolean = true

    private var pop1: MediaPlayer? = null
    private var pop2: MediaPlayer? = null
    private var energicMusic: MediaPlayer? = null
    private var crowdCheer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

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
        _binding = FragmentAskQuestionOpenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        welcomeCreditAvailable = WelcomeCreditEligibility.isEligible(requireContext())

        viewNoBucket = AskQuestionPromoStats.viewBucket(
            AskQuestionPromoStats.nextViewNo(requireContext(), trigger),
        )
        shownAtMs = SystemClock.elapsedRealtime()
        AnalyticsLogger.logAskQuestionPromoShown(
            trigger = trigger,
            viewNo = viewNoBucket,
            welcomeCreditAvailable = welcomeCreditAvailable,
        )

        // Kutlama YALNIZCA otomatik tanıtımda. Orada ekran beklenmedik bir hediye olarak
        // beliriyor; diğer iki durumda kullanıcı "öğretmene sor"a basmış ve duvara çarpmış
        // oluyor — o anda konfeti ve alkış "tebrikler, sana bir şey satacağım" gibi okunuyor.
        if (trigger == AnalyticsLogger.PROMO_TRIGGER_AUTO) {
            playSounds()
        } else {
            binding.confettiAnim.cancelAnimation()
            binding.confettiAnim.visibility = View.GONE
        }

        bindActions()

        binding.btnNoThanks.setOnClickListener {
            logClosed(AnalyticsLogger.PROMO_NO_THANKS)
            dismiss()
        }
    }

    /**
     * Büyük düğmeye, kullanıcıya KREDİ GETİREN eylemi bağlar.
     *
     * ## Neden duruma göre değişiyor
     * Pro aboneliği danışma kredisi olarak yalnızca bir kerelik hoş geldin kredisi veriyor
     * (sunucuda `PRO_WELCOME_CREDITS = 1`) ve o da cihaz başına bir kez. Cihaz krediyi daha önce
     * tükettiyse kullanıcı "Pro'ya geç"e basıp parayı öder ve **hâlâ 0 kredisi olur** — yani
     * geldiği işi, öğretmene soru sormayı, yine yapamaz. Zaten Pro olan kullanıcı için de Pro
     * satmanın anlamı yok.
     *
     * Bu yüzden kural tek: büyük düğme o an gerçekten kredi getiren eylem olur, diğeri ikincil
     * satıra iner. Pro hiç kaybolmuyor, sadece vaadi karşılamadığı durumda öne çıkmıyor.
     */
    private fun bindActions() {
        val isAlreadyPro = trigger == AnalyticsLogger.PROMO_TRIGGER_PRO_OUT_OF_CREDITS
        val proDeliversCredit = welcomeCreditAvailable && !isAlreadyPro

        if (proDeliversCredit) {
            // Metin SABİT YAZILMAZ: Play ücretsiz denemeyi Google hesabı başına bir kez veriyor.
            // Daha önce abone olmuş kullanıcıya "1 hafta ücretsiz dene" demek yanlış vaat olurdu.
            SubscriptionCta.apply((activity as? MainActivity)?.billingManager, binding.tryFreeText)
            binding.btnTryFree.setOnClickListener { goToPro() }

            binding.btnBuyCredits.setText(R.string.ask_promo_secondary_buy_credits)
            binding.btnBuyCredits.setOnClickListener { goToShop() }
            return
        }

        binding.tryFreeText.setText(R.string.ask_promo_cta_buy_credits)
        binding.btnTryFree.setOnClickListener { goToShop() }

        if (isAlreadyPro) {
            binding.infoText.setText(R.string.ask_promo_info_pro_out_of_credits)
            binding.btnBuyCredits.visibility = View.GONE
            return
        }

        // Free ama hediye kredi alamıyor: Pro hâlâ teklif, ama abarttığımız bir vaat olmadan —
        // paketlerdeki bonus gerçek ve doğrulanabilir tek fayda.
        binding.btnBuyCredits.setText(R.string.ask_promo_secondary_pro_bonus)
        binding.btnBuyCredits.setOnClickListener { goToPro() }
    }

    private fun goToPro() {
        logClosed(AnalyticsLogger.PROMO_TRY_FREE)
        // Yeni fragmenti hemen açıyoruz
        ProDiffirentFragment
            .newInstance(AnalyticsLogger.PRO_ENTRY_ASK_QUESTION)
            .show(requireActivity().supportFragmentManager, "ProDiffirent")

        // Altında kalan bu fragmenti animasyon süresi kadar (yaklaşık 500ms) arkada bekletip,
        // daha sonra animasyonsuz ve sessizce kapatıyoruz. Böylece aradaki boşluk/bekleme hissi kayboluyor.
        handler.postDelayed({
            try {
                dialog?.window?.setWindowAnimations(0)
                dismiss()
            } catch (e: Exception) {}
        }, 500)
    }

    private fun goToShop() {
        val main = activity as? MainActivity
        logClosed(AnalyticsLogger.PROMO_BUY_CREDITS)
        dismiss()
        main?.openShopFragment()
    }

    /**
     * Geri tuşu ve panel dışına dokunma. `onDismiss` DEĞİL: o, yapılandırma değişikliğinde ve
     * uygulama öldürülürken de çalışıp sahte "kapattı" kaydı düşürürdü.
     */
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        logClosed(AnalyticsLogger.PROMO_DISMISSED)
    }

    /**
     * Kapanınca ders sonrası kuyruğunu dürter.
     *
     * Ölçüm için `onCancel` kullanılıyor (yukarıdaki gerekçe), ama kuyruk için
     * `onDismiss` gerekiyor: düğmeye basıp kapatan kullanıcıda `onCancel` çalışmıyor ve
     * sıradaki ekran asılı kalırdı. Burası yalnızca dürtüyor, kayıt düşmüyor.
     */
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as? MainActivity)?.pumpPostLessonQueue("AskQuestionOpen.dismiss")
    }

    /**
     * İlk çağrı kazanır: düğme kendi sonucunu bildirdikten sonra gelen `onCancel` sayılmaz.
     *
     * [outcome] hangi VIEW'a basıldığına değil, nereye GİDİLDİĞİNE göre yazılır: düzen
     * değiştiğinde büyük düğme mağazaya gidiyor ve o tıklama `buy_credits` sayılmalı.
     */
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

    private fun playSounds() {
        val prefs = requireContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sound_enabled", true)) return
        // İlk confetti_pop_sound hemen oynatılsın (Sesi azaltıldı -> 0.05f)
        pop1 = MediaPlayer.create(requireContext(), R.raw.confetti_pop_sound)
        pop1?.setVolume(0.05f, 0.05f)
        pop1?.start()

        // 0.2 saniye (200ms) sonra ikinci confetti_pop_sound
        handler.postDelayed({
            if (_binding != null) { // Fragment hala aktifse
                pop2 = MediaPlayer.create(requireContext(), R.raw.confetti_pop_sound)
                pop2?.setVolume(0.05f, 0.05f)
                pop2?.start()
            }
        }, 200)

        // İkinci sesten 0.3 saniye (300ms) sonra (toplam 500ms) müzik ve alkış
        handler.postDelayed({
            if (_binding != null) {
                energicMusic = MediaPlayer.create(requireContext(), R.raw.energic_music)
                crowdCheer = MediaPlayer.create(requireContext(), R.raw.crowd_cheer_sound)

                energicMusic?.setVolume(0.05f, 0.05f)
                crowdCheer?.setVolume(0.05f, 0.05f)

                energicMusic?.start()
                crowdCheer?.start()
            }
        }, 500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        pop1?.release()
        pop2?.release()
        energicMusic?.release()
        crowdCheer?.release()
        pop1 = null
        pop2 = null
        energicMusic = null
        crowdCheer = null
        _binding = null
    }

    companion object {
        private const val ARG_TRIGGER = "trigger"

        /**
         * @param trigger [AnalyticsLogger.PROMO_TRIGGER_AUTO] (ders dönüşü sayacı doldu) ya da
         *   [AnalyticsLogger.PROMO_TRIGGER_OUT_OF_CREDITS] (kullanıcı "öğretmene sor"a bastı
         *   ama kredisi yoktu). Ölçümde bu ikisi asla birleştirilmemeli.
         */
        fun newInstance(trigger: String): AskQuestionOpenFragment {
            // Fragment'ı alıcı yapan bir apply KULLANILMIYOR: blok içinde `trigger` hem
            // parametre hem de aynı adlı property olurdu. Kotlin parametreyi seçer ama okuyan
            // bunu her seferinde çözmek zorunda kalır.
            val fragment = AskQuestionOpenFragment()
            fragment.arguments = Bundle().apply { putString(ARG_TRIGGER, trigger) }
            return fragment
        }
    }
}

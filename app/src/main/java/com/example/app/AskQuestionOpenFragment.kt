package com.example.app

import android.content.DialogInterface
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.graphics.Typeface
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

        viewNoBucket = AskQuestionPromoStats.viewBucket(
            AskQuestionPromoStats.nextViewNo(requireContext(), trigger),
        )
        shownAtMs = SystemClock.elapsedRealtime()
        AnalyticsLogger.logAskQuestionPromoShown(trigger = trigger, viewNo = viewNoBucket)

        playSounds()

        val buttonText = "1 hafta ücretsiz dene"
        val spannable = SpannableString(buttonText)
        val boldStart = buttonText.indexOf("ücretsiz")
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            boldStart,
            boldStart + "ücretsiz".length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.tryFreeText.text = spannable

        binding.btnTryFree.setOnClickListener {
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

        // Aboneliğe geçmek istemeyen kullanıcı krediyi tek seferlik de alabilir; soru sorma
        // hakkı artık plana değil krediye bağlı (bkz. AskQuestionButtonBinder).
        binding.btnBuyCredits.setOnClickListener {
            logClosed(AnalyticsLogger.PROMO_BUY_CREDITS)
            val main = activity as? MainActivity
            dismiss()
            main?.openShopFragment()
        }

        binding.btnNoThanks.setOnClickListener {
            logClosed(AnalyticsLogger.PROMO_NO_THANKS)
            dismiss()
        }
    }

    /**
     * Geri tuşu ve panel dışına dokunma. `onDismiss` DEĞİL: o, yapılandırma değişikliğinde ve
     * uygulama öldürülürken de çalışıp sahte "kapattı" kaydı düşürürdü.
     */
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        logClosed(AnalyticsLogger.PROMO_DISMISSED)
    }

    /** İlk çağrı kazanır: düğme kendi sonucunu bildirdikten sonra gelen `onCancel` sayılmaz. */
    private fun logClosed(outcome: String) {
        if (closeLogged || shownAtMs == 0L) return
        closeLogged = true
        AnalyticsLogger.logAskQuestionPromoClosed(
            trigger = trigger,
            viewNo = viewNoBucket,
            outcome = outcome,
            dwellMs = SystemClock.elapsedRealtime() - shownAtMs,
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

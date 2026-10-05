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
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.app.databinding.FragmentAdSkipBinding

class AdSkipFragment : DialogFragment() {

    private var _binding: FragmentAdSkipBinding? = null
    private val binding get() = _binding!!

    private var pop1: MediaPlayer? = null
    private var pop2: MediaPlayer? = null
    private var energicMusic: MediaPlayer? = null
    private var crowdCheer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    // ── Ölçüm ───────────────────────────────────────────────────────────────
    // Ölçmek istediğimiz şey panelin yıpratıp yıpratmadığı: tekrar gördükçe kitle "hiç bakmadan
    // kapat" tarafına kayıyor mu, ve kaçı gerçekten Pro akışına giriyor.
    private var shownAtMs: Long = 0L
    private var viewNoBucket: String = ""
    private var outcome: String = AnalyticsLogger.AD_SKIP_DISMISSED
    private var closeLogged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onStart() {
        super.onStart()
        // Kuyruğun çizimleri: sağdan girer, SOLA çıkar. Bu ekran yalnızca reklam
        // akışından açılıyor ve kapanınca sıradaki kuyruk ekranı sağdan geliyor; eskiden
        // sağa çıktığı için ikisi aynı kenarda çakışıyordu.
        dialog?.window?.setWindowAnimations(R.style.QueueScreenAnimation)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdSkipBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Telefonun üst çubuğu lacivert zemine, alt tuşların çubuğu alttaki beyaz bölüme uysun.
        dialog?.window?.let { w ->
            SystemBarColors.applyToDialog(
                w,
                top = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.paywall_navy),
                bottom = android.graphics.Color.WHITE,
                topView = binding.root,
                bottomView = binding.bottomWhiteContainer,
            )
        }

        // Maskot reklamı değnekle yok edip havalı poz veriyor; panel açık kaldıkça döngüde.
        binding.centerGraphic.play(BunnyMascotView.Emote.AD_MAGIC)

        val billing = (activity as? MainActivity)?.billingManager
        val trialDays = billing?.freeTrialDays(BillingCatalog.SUB_PRO)
        bindOffer(trialDays)

        // Sesler (konfeti patlaması + müzik + alkış) yalnızca deneme teklifinde. Denemesini
        // kullanmış kullanıcıya panel sessiz açılıyor; konfeti görseli yine var.
        if (trialDays != null) playSounds()

        viewNoBucket = AdSkipStats.viewBucket(AdSkipStats.nextViewNo(requireContext()))
        shownAtMs = SystemClock.elapsedRealtime()
        AnalyticsLogger.logAdSkipShown(viewNoBucket, trialOffer = trialDays != null)

        // Bu ekran satın alma başlatmıyor (huniyi ProDiffirent → Plan diye sürdürüyor) ama
        // düğmede deneme vaadi var; uygun olmayan kullanıcıya o vaadi göstermemek gerekiyor.
        SubscriptionCta.apply(billing, binding.btnTryFreeText)

        binding.btnTryFree.setOnClickListener {
            logClosed(AnalyticsLogger.AD_SKIP_TRY_FREE)
            // Yeni fragmenti hemen açıyoruz
            ProDiffirentFragment
                .newInstance(AnalyticsLogger.PRO_ENTRY_AD_SKIP)
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

        binding.btnNoThanks.setOnClickListener {
            logClosed(AnalyticsLogger.AD_SKIP_NO_THANKS)
            dismiss()
        }
    }

    /**
     * Geri tuşu veya panel dışına dokunma.
     *
     * Ölçüm `onDismiss`'te DEĞİL burada yapılıyor: `onDismiss` yapılandırma değişikliğinde ve
     * uygulama öldürülürken de tetikleniyor, o durumlarda sahte bir "kapattı" kaydı düşerdi.
     * `onCancel` yalnızca gerçek kullanıcı hareketinde çalışır.
     *
     * Paneli hiç kapatmadan uygulamadan çıkanlar bilerek ölçüm dışında: onları
     * `ad_skip_shown` ile `ad_skip_closed` arasındaki fark ve `app_exit_screen` veriyor.
     */
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        logClosed(AnalyticsLogger.AD_SKIP_DISMISSED)
    }

    private fun logClosed(reason: String) {
        if (closeLogged || shownAtMs == 0L) return
        closeLogged = true
        outcome = reason
        val dwellMs = (SystemClock.elapsedRealtime() - shownAtMs).coerceAtLeast(0L)
        AnalyticsLogger.logAdSkipClosed(
            viewNo = viewNoBucket,
            dwellBucket = AdSkipStats.dwellBucket(dwellMs),
            dwellMs = dwellMs,
            outcome = outcome,
        )
    }

    /**
     * Panelin iki sürümü, kullanıcının ücretsiz deneme hakkına göre.
     *
     * Deneme hakkını Play tutuyor (Google hesabı başına bir kez); [BillingManager.freeTrialDays]
     * null ise kullanıcı denemesini kullanmış (ya da teklif tanımlı değil). Düğme metni
     * ([SubscriptionCta]) de aynı bilgiye bakıyor, başlık onunla çelişmesin diye.
     *
     *  - Hakkı var: "N günlük ücretsiz PRO / denemesiyle reklamları atla!" ve "deneme bitmeden
     *    haber vereceğiz" satırı (gün sayısı Play'deki tekliften).
     *  - Hakkı yok: denemeden söz edilmiyor — "PRO ile reklamsız, / kesintisiz öğren!" ve
     *    zil yerine onay işaretiyle "istediğin zaman iptal edebilirsin". Deneme vaadi, onu
     *    kullanmış birine yalan olurdu.
     */
    private fun bindOffer(trialDays: Int?) {
        if (trialDays != null) {
            binding.titleText1.text = "$trialDays günlük ücretsiz PRO"
            binding.titleText2.text = "denemesiyle reklamları atla!"
            binding.reminderText.text = "Deneme süren sona ermeden önce bildirim alacaksın."
            return
        }
        binding.titleText1.text = "PRO ile reklamsız,"
        binding.titleText2.text = "kesintisiz öğren!"
        binding.bellIcon.setImageResource(R.drawable.correct_ic)
        // Zil gri boyanıyor (layout'ta tint); yeşil onay işareti boyansa gri kareye dönerdi.
        binding.bellIcon.imageTintList = null
        binding.reminderText.text = "Aboneliğini istediğin zaman iptal edebilirsin."
    }

    /**
     * Konfeti patlaması, enerjik müzik ve alkış — yalnızca deneme teklifinde çağrılıyor:
     * hediye gibi sunulan ücretsiz denemeye kutlama yakışıyor, ücretli aboneliği aynı coşkuyla
     * satmak ısrarcı duruyor. Patlama sesleri de birlikte gidiyor: müziksiz, tek başına çalan
     * konfeti sesi amatör duruyordu (kullanıcı geri bildirimi).
     */
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
}

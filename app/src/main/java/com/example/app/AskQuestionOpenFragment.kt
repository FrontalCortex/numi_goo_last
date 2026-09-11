package com.example.app

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
            // Yeni fragmenti hemen açıyoruz
            ProDiffirentFragment().show(requireActivity().supportFragmentManager, "ProDiffirent")

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
            val main = activity as? MainActivity
            dismiss()
            main?.openShopFragment()
        }

        binding.btnNoThanks.setOnClickListener {
            dismiss()
        }
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
}

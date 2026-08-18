package com.example.app

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
        _binding = FragmentAdSkipBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playSounds()

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

        binding.btnNoThanks.setOnClickListener {
            dismiss()
        }
    }

    private fun playSounds() {
        val prefs = requireContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sound_enabled", true)) return
        // İlk confetti_pop_sound hemen oynatılsın
        pop1 = MediaPlayer.create(requireContext(), R.raw.confetti_pop_sound)
        pop1?.start()

        // 0.2 saniye (200ms) sonra ikinci confetti_pop_sound
        handler.postDelayed({
            if (_binding != null) { // Fragment hala aktifse
                pop2 = MediaPlayer.create(requireContext(), R.raw.confetti_pop_sound)
                pop2?.start()
            }
        }, 200)

        // İkinci sesten 0.3 saniye (300ms) sonra (toplam 500ms) müzik ve alkış
        handler.postDelayed({
            if (_binding != null) {
                energicMusic = MediaPlayer.create(requireContext(), R.raw.energic_music)
                crowdCheer = MediaPlayer.create(requireContext(), R.raw.crowd_cheer_sound)
                
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

package com.example.app

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentMascotPlaygroundBinding
import com.google.android.material.chip.Chip

/**
 * Tavşan maskotunun bütün hâllerini denemek için ekran (Görevler → "Karakter Animasyonu").
 *
 * Bekleme hep oynuyor ve seçeneklerde yok. Bir seçenek seçilince o hâl bir kez oynuyor;
 * bitince seçim kalkıyor ve tavşan beklemeye dönüyor. Oynarken başka bir seçenek seçilirse
 * hâl hemen ona geçiyor (geçişi görmek için de işe yarıyor).
 */
class MascotPlaygroundFragment : Fragment() {

    private var _binding: FragmentMascotPlaygroundBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMascotPlaygroundBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { close() }
        binding.btnClose.setOnClickListener { close() }

        val chipColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(
                ContextCompat.getColor(requireContext(), R.color.dark_primary),
                CHIP_IDLE_COLOR,
            ),
        )
        val textColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(
                ContextCompat.getColor(requireContext(), R.color.background_color),
                ContextCompat.getColor(requireContext(), R.color.dark_text),
            ),
        )
        for (emote in BunnyMascotView.Emote.entries) {
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = emote.label
                isCheckable = true
                isCheckedIconVisible = false
                chipBackgroundColor = chipColors
                setTextColor(textColors)
                chipStrokeWidth = 0f
                tag = emote
                setOnClickListener { play(emote, this) }
            }
            binding.emoteChips.addView(chip)
        }

        binding.mascotView.onEmoteFinished = {
            _binding?.let { b ->
                b.emoteChips.clearCheck()
                b.tvCurrent.text = "Bekleme"
            }
        }
    }

    private fun play(emote: BunnyMascotView.Emote, chip: Chip) {
        // Seçili bir çipe yeniden dokunmak seçimi kaldırıyor; hâli yine baştan oynatıp
        // çipi seçili tutuyoruz.
        chip.isChecked = true
        binding.tvCurrent.text = emote.label
        binding.mascotView.play(emote)
    }

    private fun close() {
        val main = activity as? MainActivity
        if (main != null) {
            main.finishTasksOverlayAnimated("MascotPlaygroundFragment.close")
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.mascotView?.onEmoteFinished = null
        _binding = null
    }

    private companion object {
        const val CHIP_IDLE_COLOR = 0xFF22303A.toInt()
    }
}

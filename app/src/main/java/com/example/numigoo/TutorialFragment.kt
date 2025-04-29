package com.example.numigoo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.numigoo.databinding.FragmentTutorialBinding
import com.example.numigoo.model.BeadAnimation
import com.example.numigoo.model.TutorialStep

class TutorialFragment : Fragment() {
    private lateinit var binding: FragmentTutorialBinding
    private var currentStep = 0

    companion object {
        fun newInstance() = TutorialFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTutorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTutorial()
        setupBackButton()
    }

    private fun setupTutorial() {
        // İlk adımı göster
        showStep(currentStep)

        // Ekrana tıklama ile ilerleme
        binding.root.setOnClickListener {
            if (currentStep < tutorialSteps.size - 1) {
                currentStep++
                showStep(currentStep)
            }
        }
    }

    private fun showStep(position: Int) {
        val step = tutorialSteps[position]
        binding.tutorialText.text = step.text
        step.animation?.forEach { it.animate() }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                showStep(currentStep)
            } else {
                // Fragment'i kapat ve MapFragment'e dön
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_left,  // Giriş animasyonu
                        R.anim.slide_out_left // Çıkış animasyonu
                    )
                    .remove(this)
                    .commit()
            }
        }
    }

    // Tutorial adımları
    private val tutorialSteps = listOf(
        TutorialStep(
            "Abaküs, sayıları temsil etmek için boncuklar kullanan bir hesap aracıdır.",
            null
        ),
        TutorialStep(
            "Her sütun bir basamağı temsil eder. Sağdan sola doğru birler, onlar, yüzler basamağı şeklinde ilerler.",
            null
        ),
        TutorialStep(
            "Her sütunda 5 boncuk vardır. Üstteki boncuk 5 değerini, alttaki boncuklar ise 1 değerini temsil eder.",
            null
        ),
        TutorialStep(
            "Örneğin 1 sayısı abaküste bu şekilde gösterilir.",
            listOf(BeadAnimation(this, "rod4_bead_bottom1", 1))  // en sağdaki sütun (4), 1 boncuk
        ),
        TutorialStep(
            "5 sayısı için üstteki boncuk kullanılır.",
            listOf(
                BeadAnimation(this, "rod4_bead_bottom1", 2),
                BeadAnimation(this, "rod4_bead_top", 3)
                // İhtiyaca göre daha fazla animasyon eklenebilir
            )
        ),
        TutorialStep(
            "6 sayısı için üstteki boncuk ve bir alttaki boncuk kullanılır.",
            listOf(BeadAnimation(this, "rod4_bead_bottom1", 1))  // en sağdaki sütun (4), 6 boncuk
        ),
        TutorialStep(
            "17 sayısı ise böyle gösterilir.",
            listOf(BeadAnimation(this, "rod3_bead_bottom1", 1),
                BeadAnimation(this, "rod4_bead_bottom2", 1))  // en sağdaki sütun (4), 6 boncuk
        ),
        TutorialStep(
            "51 sayısı böyle.",
            listOf(BeadAnimation(this, "rod3_bead_bottom1", 2),
                BeadAnimation(this, "rod3_bead_top", 3),
                BeadAnimation(this, "rod4_bead_bottom2", 2),
                BeadAnimation(this, "rod4_bead_top", 4))  // en sağdaki sütun (4), 6 boncuk
        ),
        TutorialStep(
            "126 sayısı ise böyle gösterilir.",
            listOf(BeadAnimation(this, "rod2_bead_bottom1", 1),
                BeadAnimation(this, "rod3_bead_bottom2", 1),
                BeadAnimation(this, "rod3_bead_top", 4),
                BeadAnimation(this, "rod3_bead_bottom1", 1),
                BeadAnimation(this, "rod4_bead_top", 1))  // en sağdaki sütun (4), 6 boncuk
        )

    )

    data class TutorialStep(
        val text: String,
        val animation: List<BeadAnimation>?
    )
} 
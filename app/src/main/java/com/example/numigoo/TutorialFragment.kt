package com.example.numigoo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.numigoo.databinding.FragmentTutorialBinding
import com.example.numigoo.model.TutorialStep
import com.example.numigoo.model.BeadAnimation

class TutorialFragment : Fragment() {
    private lateinit var binding: FragmentTutorialBinding
    private lateinit var adapter: TutorialAdapter
    private var currentStep = 0

    // Tutorial adımlarını tutan liste
    private val tutorialSteps = listOf(
        TutorialStep(
            "Abaküsün her bir sütunu sayının basamağını ifade eder.",
            null
        ),
        TutorialStep(
            "Yukarıdaki boncuklar 5'lik değere sahipken aşağıdakiler 1'lik değere sahiptir.",
            null
        ),
        TutorialStep(
            "Boncukları ortadaki değer çubuğuna değdirerek sayılar ifade ederiz.",
            null
        ),
        TutorialStep(
            "Örneğin 1 sayısı abaküste bu şekilde gösterilir.",
            BeadAnimation(4, 1)  // en sağdaki sütun (4), 1 boncuk
        ),
        TutorialStep(
            "2",
            BeadAnimation(4, 2)
        ),
        TutorialStep(
            "3",
            BeadAnimation(4, 3)
        ),
        TutorialStep(
            "10",
            BeadAnimation(3, 1)  // sağdan ikinci sütun (3), 1 boncuk
        ),
        TutorialStep(
            "5",
            BeadAnimation(4, 5)  // en sağdaki sütun (4), üst boncuk
        ),
        TutorialStep(
            "17",
            listOf(
                BeadAnimation(3, 1),  // 10 için
                BeadAnimation(4, 7)   // 7 için
            )
        ),
        TutorialStep(
            "53",
            listOf(
                BeadAnimation(2, 5),  // 50 için
                BeadAnimation(4, 3)   // 3 için
            )
        ),
        TutorialStep(
            "126",
            listOf(
                BeadAnimation(1, 1),  // 100 için
                BeadAnimation(2, 2),  // 20 için
                BeadAnimation(4, 6)   // 6 için
            )
        ),
        TutorialStep(
            "Şimdi tahtaya yazacağım sayıları abaküste doğru şekilde göstermeye çalış.",
            null
        )
    )

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
        
        setupRecyclerView()
        setupBackButton()
    }

    private fun setupRecyclerView() {
        adapter = TutorialAdapter(tutorialSteps) {
            if (currentStep < tutorialSteps.size - 1) {
                currentStep++
                binding.recyclerView.smoothScrollToPosition(currentStep)
            }
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@TutorialFragment.adapter
        }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                binding.recyclerView.smoothScrollToPosition(currentStep)
            }
        }
    }

    companion object {
        fun newInstance() = TutorialFragment()
    }
} 
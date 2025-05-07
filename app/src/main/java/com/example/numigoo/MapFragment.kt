package com.example.numigoo

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.numigoo.GlobalValues.randomNumberChangeToString
import com.example.numigoo.databinding.FragmentMapBinding
import com.example.numigoo.model.LessonItem

class MapFragment : Fragment() {
    private lateinit var binding: FragmentMapBinding
    lateinit var lessonsAdapter: LessonAdapter
    companion object {
        fun getLessonOperations(lessonId: Int): List<MathOperation> {
            return when (lessonId) {
                1 -> listOf(
                    MathOperation(null,"2", null),
                    MathOperation(null,"5", null),
                    MathOperation(null,"3", null),
                    MathOperation(null,"8", null),
                    MathOperation(null,"4", null),
                    MathOperation(null,"13", null),
                    MathOperation(null,"9", null),
                    MathOperation(null,"41", null),
                    MathOperation(null,"36", null),
                    MathOperation(null,"23", null)
                )
                2 -> listOf(
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                    MathOperation(null,randomNumberChangeToString(5), null)
                )
                3 -> listOf(
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                    MathOperation(null,randomNumberChangeToString(5), null),
                    MathOperation(null,randomNumberChangeToString(5), null),
                    MathOperation(null,randomNumberChangeToString(5), null),
                    MathOperation(null,randomNumberChangeToString(5), null),
                    MathOperation(null,randomNumberChangeToString(5), null)
                )
                4 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),

                )
                5 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(3, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 3),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(3, 1),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                )
                6 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 2),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 4),
                    MathOperationGenerator.generateRelatedNumbers(4, 3),
                    MathOperationGenerator.generateRelatedNumbers(4, 4),
                    MathOperationGenerator.generateRelatedNumbers(3, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(4, 3),
                    MathOperationGenerator.generateRelatedNumbers(4, 4),
                    )
                7 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 2),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 4),
                    MathOperationGenerator.generateRelatedNumbers(4, 3),
                    MathOperationGenerator.generateRelatedNumbers(4, 4),
                    MathOperationGenerator.generateRelatedNumbers(5, 4),
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
                    MathOperationGenerator.generateRelatedNumbers(4, 5),
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
                    MathOperationGenerator.generateRelatedNumbers(5, 5),

                )
                else -> emptyList()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Global verileri kullan
        val items = GlobalLessonData.lessonItems
        if (items.isEmpty()) {
            // İlk kez çalışıyorsa varsayılan verileri yükle
            GlobalLessonData.initialize(createLessonItems())
        }
        
        setupRecyclerView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Verileri kaydet
        LessonDataManager.saveLessonItems(requireContext(), GlobalLessonData.lessonItems)
    }

    private fun setupRecyclerView() {
        // Adapter'ı oluştur
        lessonsAdapter = LessonAdapter(
            context = requireContext(),
            items = GlobalLessonData.lessonItems.toMutableList(),
            onLessonClick = { item, position ->
                if (!item.isCompleted) {
                    Toast.makeText(context, "Bu ders henüz kilitli!", Toast.LENGTH_SHORT).show()
                    return@LessonAdapter
                }
                val fragment = item.fragment?.let { it() }
                if (fragment != null) {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerID, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        )

        // LessonManager'a adapter'ı set et
        LessonManager.setAdapter(lessonsAdapter)

        // RecyclerView'ı ayarla
        binding.lessonsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            val stickyHeader = requireActivity().findViewById<LinearLayout>(R.id.stickyHeader)
            val stickySectionUnit = requireActivity().findViewById<TextView>(R.id.stickySectionUnit)
            val stickyHeaderTitle = requireActivity().findViewById<TextView>(R.id.stickyHeaderTitle)
            // Maksimum offset değerini al
            val maxOffset = resources.getDimensionPixelSize(R.dimen.max_lesson_offset)

            // ItemDecoration'ı ekle
            addItemDecoration(DynamicOffsetDecoration(maxOffset))

            // Adapter'ı bağla
            adapter = lessonsAdapter
            binding.lessonsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    val adapter = recyclerView.adapter as LessonAdapter

                    var headerTitle: String? = null
                    var sectionUnit: String? = null
                    for (i in firstVisible downTo 0) {
                        val item = adapter.getItem(i)
                        if (item.type == LessonItem.TYPE_HEADER) {
                            headerTitle = item.title
                            sectionUnit = "${item.stepCount}. KISIM, ${item.currentStep}. ÜNİTE"

                            val colorRes = item.color
                            if (colorRes != null) {
                                val color = ContextCompat.getColor(requireContext(), colorRes)
                                stickyHeader.backgroundTintList = ColorStateList.valueOf(color)                            }
                            break
                        }
                    }
                    if (headerTitle != null) {
                        stickySectionUnit.text = sectionUnit
                        stickyHeaderTitle.text = headerTitle
                        stickyHeader.visibility = View.VISIBLE
                    } else {
                        stickyHeader.visibility = View.GONE
                    }
                }
            })
        }
    }

    private fun createLessonItems(): List<LessonItem> {
        return listOf(
            LessonItem(
                type = LessonItem.TYPE_HEADER,
                title = "Kuralsız Toplama",
                offset = 0,
                isCompleted = false,
                stepCount = 1,
                currentStep = 1,
                color = R.color.lesson_header_blue
            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Sayıları abaküste tanıma",
                offset = 0,
                isCompleted = true,
                stepCount = 3,
                currentStep = 1,
                lessonOperationsMap = 1,
                finishStepNumber = 3,
                tutorialNumber = 1,
                startStepNumber = 1,
                mapFragmentIndex = 1,
                lessonHint = getString(R.string.lesson_hint_step2)
            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Kuralsız toplama",
                offset = 30,
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                tutorialNumber = 2,
                startStepNumber = 4,
                mapFragmentIndex = 2
            ),
            LessonItem(
                type = LessonItem.TYPE_CHEST,
                title = "Ünite Değerlendirme",
                offset = 0,
                isCompleted = false,
                stepCount = 0,
                currentStep = 1,
                fragment = { AbacusFragment.newInstance("+", "Ünite Değerlendirme") }
            ),
            LessonItem(
                type = LessonItem.TYPE_HEADER,
                title = "5'lik toplama",
                offset = 0,
                isCompleted = false,
                stepCount = 1,
                currentStep = 1,
                color = R.color.lesson_header_pink

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Basit 5'lik toplama",
                offset = -30,
                isCompleted = false,
                stepCount = 4,
                currentStep = 1,

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Zor 5'lik toplama",
                offset = -60,
                isCompleted = false,
                stepCount = 4,
                currentStep = 1,

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "İmkansız 5'lik toplama",
                offset = -30,
                isCompleted = false,
                stepCount = 4,
                currentStep = 1,

            ),
            LessonItem(
                type = LessonItem.TYPE_CHEST,
                title = "Ünite Değerlendirme",
                offset = 0,
                isCompleted = false,
                stepCount = 0,
                currentStep = 0,
                fragment = { AbacusFragment.newInstance("+", "Ünite Değerlendirme") }
            ),
            LessonItem(
                type = LessonItem.TYPE_HEADER,
                title = "10'luk Toplama 1-2-3-4-5",
                offset = 0,
                isCompleted = false,
                stepCount = 1,
                currentStep = 1,
                color = R.color.lesson_header_blue

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Temel 10'luk toplama",
                offset = -30,
                isCompleted = false,
                stepCount = 4,
                currentStep = 1,

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Zor 10'luk toplama",
                offset = 0,
                isCompleted = false,
                stepCount = 4,
                currentStep = 1,

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "İmkansız 10'luk toplama",
                offset = 30,
                isCompleted = false,
                stepCount = 4,
                currentStep = 1,

            ),LessonItem(
                type = LessonItem.TYPE_CHEST,
                title = "Ünite Değerlendirme",
                offset = 0,
                isCompleted = false,
                stepCount = 0,
                currentStep = 0,
                fragment = { AbacusFragment.newInstance("+", "Ünite Değerlendirme") }
            ),
            LessonItem(
                type = LessonItem.TYPE_HEADER,
                title = "10'luk Toplama 6-7-8-9",
                offset = 0,
                isCompleted = false,
                stepCount = 1,
                currentStep = 1,
                color = R.color.lesson_header_orange

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Orta seviye 10'luk toplama",
                offset = -30,
                isCompleted = false,
                stepCount = 4,
                currentStep = 1,

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "İleri seviye 10'luk toplama",
                offset = 0,
                isCompleted = false,
                stepCount = 4,
                currentStep = 1,

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Çılgın 10'luk toplama",
                offset = 30,
                isCompleted = false,
                stepCount = 4,
                currentStep = 1,

            ),
            LessonItem(
                type = LessonItem.TYPE_CHEST,
                title = "Ünite Değerlendirme",
                offset = 0,
                isCompleted = false,
                stepCount = 0,
                currentStep = 0,
                fragment = { AbacusFragment.newInstance("+", "Ünite Değerlendirme") }
            ),
            LessonItem(
                type = LessonItem.TYPE_HEADER,
                title = "Boncuk kuralı",
                offset = 0,
                isCompleted = false,
                stepCount = 1,
                currentStep = 1,
                color = R.color.lesson_header_red
            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Temel Boncuk Kuralı",
                offset = -30,
                isCompleted = false,
                stepCount = 4,
                currentStep = 1,

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Zor Boncuk Kuralı",
                offset = 0,
                isCompleted = false,
                stepCount = 4,
                currentStep = 1,

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "İmkansız Boncuk Kuralı",
                offset = -30,
                isCompleted = false,
                stepCount = 4,
                currentStep = 1,

            ),
            LessonItem(
                type = LessonItem.TYPE_CHEST,
                title = "Ünite Değerlendirme",
                offset = 0,
                isCompleted = false,
                stepCount = 0,
                currentStep = 0,
                fragment = { AbacusFragment.newInstance("+", "Ünite Değerlendirme") }
            ),
        )
    }


}
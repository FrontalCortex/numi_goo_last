package com.example.numigoo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.numigoo.databinding.FragmentMapBinding
import com.example.numigoo.model.LessonItem

class MapFragment : Fragment() {
    private lateinit var binding: FragmentMapBinding
    private lateinit var lessonsAdapter: LessonAdapter

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
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // Örnek veri listesi oluştur
        val lessonItems = createLessonItems()

        // Adapter'ı oluştur
        lessonsAdapter = LessonAdapter(
            context = requireContext(), 
            items = lessonItems,
            onLessonClick = { item, position ->
                if (!item.isCompleted) {
                    Toast.makeText(context, "Bu ders henüz kilitli!", Toast.LENGTH_SHORT).show()
                    return@LessonAdapter
                }
                val fragment = item.fragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerID, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )

        // RecyclerView'ı ayarla
        binding.lessonsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())

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
                            break
                        }
                    }
                    val stickyHeader = requireActivity().findViewById<LinearLayout>(R.id.stickyHeader)
                    val stickySectionUnit = requireActivity().findViewById<TextView>(R.id.stickySectionUnit)
                    val stickyHeaderTitle = requireActivity().findViewById<TextView>(R.id.stickyHeaderTitle)
                    if (headerTitle != null && sectionUnit != null) {
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
                fragment = { AbacusFragment.newInstance("+", "Kuralsız Toplama") }
            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Sayıları abaküste tanıma",
                offset = 0,
                isCompleted = true,
                stepCount = 0,
                currentStep = 3,
                fragment = { TutorialFragment.newInstance() }
            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Kuralsız toplama",
                offset = 50,
                isCompleted = false,
                stepCount = 0,
                currentStep = 4,
                fragment = { AbacusFragment.newInstance("+", "Kuralsız Toplama") }
            ),
            LessonItem(
                type = LessonItem.TYPE_CHEST,
                title = "Ünite Değerlendirme",
                offset = -50,
                isCompleted = false,
                stepCount = 0,
                currentStep = 0,
                fragment = { AbacusFragment.newInstance("+", "Ünite Değerlendirme") }
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
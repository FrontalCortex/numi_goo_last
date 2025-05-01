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
import kotlin.math.pow

class MapFragment : Fragment() {
    private lateinit var binding: FragmentMapBinding
    private lateinit var lessonsAdapter: LessonAdapter
    companion object{
        val lessonOperationsMap = mapOf(
            1 to listOf(
                MathOperation(null,"2", null),
                MathOperation(null,"5", null),
                MathOperation(null,"3", null),
                MathOperation(null,"8", null),
                MathOperation(null,"4", null),
                MathOperation(null,"13", null),
                MathOperation(null,"9", null),
                MathOperation(null,"41", null),
                MathOperation(null,"32", null),
                MathOperation(null,"23", null),
            ),
            2 to listOf(
                MathOperation(10, "-", 3),
                MathOperation(15, "-", 7),
                MathOperation(8, "-", 2),
                MathOperation(12, "-", 5),
                MathOperation(9, "-", 4),
                MathOperation(14, "-", 6),
                MathOperation(7, "-", 3),
                MathOperation(11, "-", 4),
                MathOperation(13, "-", 5),
                MathOperation(6, "-", 2)
            )
            // Diğer dersler için benzer şekilde devam edebilirsiniz
        )
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
                val fragment = item.fragment?.let { it() }
                if (fragment != null) {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerID, fragment)
                        .addToBackStack(null)
                        .commit()
                }
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
            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Sayıları abaküste tanıma",
                offset = 0,
                isCompleted = true,
                stepCount = 0,
                currentStep = 3,
                fragment = {
                    TutorialFragment.newInstance()
                }
            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Kuralsız toplama",
                offset = 50,
                isCompleted = false,
                stepCount = 0,
                currentStep = 4,
                fragment = { TutorialFragment.newInstance() }
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
    private fun generateRandomNumber(digitCount: Int): Int {
        // Basamak sayısı kontrolü
        if (digitCount < 1) return 0

        // Minimum ve maksimum değerleri hesapla
        val min = 10.0.pow(digitCount - 1).toInt()  // Örnek: 3 basamak için 100
        val max = 10.0.pow(digitCount).toInt() - 1  // Örnek: 3 basamak için 999

        // Random sayı üret
        return (min..max).random()
    }
    private fun randomNumberChangeToString(digitCount: Int): String{

        // Minimum ve maksimum değerleri hesapla
        val min = 10.0.pow(digitCount - 1).toInt()  // Örnek: 3 basamak için 100
        val max = 10.0.pow(digitCount).toInt() - 1  // Örnek: 3 basamak için 999

        // Random sayı üret
        return (min..max).random().toString()
    }
}
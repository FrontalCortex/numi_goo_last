package com.example.numigoo

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
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
import com.example.numigoo.MathOperationGenerator.generateRandomMathOperation1
import com.example.numigoo.databinding.FragmentMapBinding
import com.example.numigoo.model.LessonItem
import com.example.numigoo.model.LessonViewModel
import androidx.fragment.app.viewModels
class MapFragment : Fragment() {
    private lateinit var binding: FragmentMapBinding
    private val viewModel: LessonViewModel by viewModels()
    lateinit var lessonsAdapter: LessonAdapter
    /*private val adapter by lazy {
        LessonAdapter(requireContext(), viewModel) { item, position ->
            // TYPE_PART ise ve fastForwardButton'a tıklandıysa:
            if (item.type == LessonItem.TYPE_PART) {
                item.id?.let { viewModel.showSubLessons(it) }
            }
            // Diğer tıklama işlemleri (örneğin, kart tıklaması) burada olabilir
        }
    }*/
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
                    MathOperationGenerator.generateRelatedNumbers(3, 2),
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
                8-> listOf(
                    MathOperation(3,"+", 4),
                    MathOperation(2,"+", 3),
                    MathOperation(4,"+", 1),
                    MathOperation(3,"+", 2),
                    MathOperation(1,"+", 4),
                    MathOperation(4,"+", 2),
                    MathOperation(4,"+", 3),
                    MathOperation(3,"+", 3),
                    MathOperation(4,"+", 1),
                    MathOperation(4,"+", 2),
                    MathOperation(3,"+", 2),
                    MathOperation(2,"+", 4),
                    MathOperation(4,"+", 4))
                9-> listOf(
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1()
                )
                10-> listOf(
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1()
                )
                11-> listOf(
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1()
                )
                12 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    )
                13 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    )
                14 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                )
                15 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                )
                16 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                )
                17 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                )
                18 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                )
                19 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3)
                )
                20 -> listOf(
                    MathOperation(7,"+", 3),
                    MathOperation(8,"+", 2),
                    MathOperation(9,"+", 2),
                    MathOperation(6,"+", 4),
                    MathOperation(7,"+", 5),
                    MathOperation(9,"+", 1),
                    MathOperation(80,"+", 50),
                    MathOperation(17,"+", 4),
                    MathOperation(19,"+", 1),
                    MathOperation(29,"+", 3),
                    MathOperation(3,"+", 2),
                    MathOperation(4,"+", 4),
                    MathOperation(9,"+", 1))
                21 -> listOf(
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation()
                )
                22 -> listOf(
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation()
                )
                23 -> listOf(
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation()
                )
                24 -> listOf(
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2()
                )
                25 -> listOf(
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2()
                )
                26 -> listOf(
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2()
                )
                27 -> listOf(
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2(),
                    MathOperationGenerator.generateMathOperation2()
                )
                28 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2)
                )
                29 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2)
                )
                30 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2)
                )
                31 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2)
                )
                32 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3)
                )
                33 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3)
                )
                34 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3)
                )
                35 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3)
                )
                36 -> listOf(
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3)
                    )
                37 -> listOf(
                    MathOperation(4, "+", 6),
                    MathOperation(4, "+", 9),
                    MathOperation(15, "+", 5),
                    MathOperation(3, "+", 7),
                    MathOperation(3, "+", 8),
                    MathOperation(4, "+", 7),
                    MathOperation(4, "+", 6),
                    MathOperation(2, "+", 8),
                    MathOperation(5, "+", 5)
                )
                38 -> listOf(
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                )
                39 -> listOf(
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                )
                40 -> listOf(
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                )
                41 -> listOf(
                    MathOperation(55, "+", 55),
                    MathOperation(99, "+", 66),
                    MathOperation(44, "+", 66),
                    MathOperation(33, "+", 77),
                    MathOperation(88, "+", 77),
                    MathOperation(44, "+", 88),
                    MathOperation(77, "+", 88),
                    MathOperation(66, "+", 99),
                    MathOperation(11, "+", 99)
                )
                42 -> listOf(
                    MathOperation(48, "+", 7),
                    MathOperation(98, "+", 5),
                    MathOperation(49, "+", 6),
                    MathOperation(91, "+", 9),
                    MathOperation(99, "+", 8),
                    MathOperation(49, "+", 6),
                    MathOperation(92, "+", 9),
                    MathOperation(43, "+", 7),
                    MathOperation(45, "+", 5),
                    MathOperation(47, "+", 8)
                )
                43 -> listOf(
                    MathOperation(44, "+", 16),
                    MathOperation(33, "+", 27),
                    MathOperation(22, "+", 38),
                    MathOperation(11, "+", 49),
                    MathOperation(49, "+", 16),
                    MathOperation(38, "+", 27),
                    MathOperation(27, "+", 38),
                    MathOperation(16, "+", 49),
                    MathOperation(23, "+", 38),
                    MathOperation(85, "+", 55))
                44 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                )
                45 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                )
                46 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                )
                47 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                )
                48 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                )
                49 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                )
                50 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                )
                51 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                )
                52 -> listOf(
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),

                    )
                53 -> listOf(
                    MathOperation(6,"+", 6),
                    MathOperation(7,"+", 7),
                    MathOperation(5,"+", 8),
                    MathOperation(5,"+", 9),
                    MathOperation(8,"+", 6),
                    MathOperation(6,"+", 7),
                    MathOperation(6,"+", 8),
                    MathOperation(5,"+", 9),
                    MathOperation(16,"+", 7),
                    MathOperation(28,"+", 6)
                )
                54 -> listOf(
                    MathOperation(5,"+", 9),
                    MathOperation(8,"+", 6),
                    MathOperation(5,"+", 6),
                    MathOperation(16,"+", 7),
                    MathOperation(16,"+", 18),
                    MathOperation(5,"+", 8),
                    MathOperation(50,"+", 90),
                    MathOperation(17,"+", 7),
                    MathOperation(5,"+", 8),
                    MathOperation(15,"+", 9)
                )
                55 -> listOf(
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                )
                56 -> listOf(
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    )
                57 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                )
                58 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                )
                59 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                )
                60 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                )
                61 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                )
                62 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                )
                63 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                )
                64 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                )
                65 -> listOf(
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),

                    )
                66 -> listOf(
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                )
                67 -> listOf(
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                )
                68 -> listOf(
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                )
                69 -> listOf(
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                )
                70 -> listOf(
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),

                    )
                71 -> listOf(
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),

                    )
                72 -> listOf(
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),

                    )
                73 -> listOf(
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    )
                74 -> listOf(
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(5,5),
                    MathOperationGenerator.irregularExtraction(5,5),
                    MathOperationGenerator.irregularExtraction(5,5),
                    MathOperationGenerator.irregularExtraction(5,5),
                    MathOperationGenerator.irregularExtraction(5,5),
                    MathOperationGenerator.irregularExtraction(5,5),
                    MathOperationGenerator.irregularExtraction(5,5),

                )
                75 -> listOf(
                    MathOperation(5, "-", 4),
                    MathOperation(7, "-", 3),
                    MathOperation(8, "-", 4),
                    MathOperation(60, "-", 20),
                    MathOperation(5, "-", 1),
                    MathOperation(6, "-", 2),
                    MathOperation(5, "-", 3),
                    MathOperation(50, "-", 10),
                    MathOperation(6, "-", 4),
                    MathOperation(6, "-", 3),
                    MathOperation(5, "-", 2),
                    MathOperation(5, "-", 1),
                )
                76 -> listOf(
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),

                    )
                else -> emptyList()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
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
        viewModel.currentLessons.observe(viewLifecycleOwner) { lessons ->
            if (viewModel.currentLessons.value.isNullOrEmpty()) {
                viewModel.initializeLessons()
            }
        }
        setupRecyclerView()
        restoreScrollPosition()
    }

    override fun onPause() {
        super.onPause()
        // Scroll pozisyonunu GlobalValues'a kaydet
        GlobalValues.scrollPosition = (binding.lessonsRecyclerView.layoutManager as LinearLayoutManager)
            .findFirstVisibleItemPosition()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Verileri kaydet - ViewModel'dan al
        viewModel.currentLessons.value?.let { lessons ->
            LessonDataManager.saveLessonItems(requireContext(), lessons)
        }
    }
    private fun observeLessons() {
        viewModel.currentLessons.observe(viewLifecycleOwner) { lessons ->
            lessonsAdapter.submitList(lessons)
            Log.d("SesKes","Cals")
        }
    }
    private fun restoreScrollPosition() {
        view?.post {
            if (com.example.numigoo.GlobalValues.scrollPosition > 0) {
                binding.lessonsRecyclerView.scrollToPosition(com.example.numigoo.GlobalValues.scrollPosition)
            }
        }
    }

    private fun setupRecyclerView() {
        // Adapter'ı oluştur
        lessonsAdapter = LessonAdapter(
            context = requireContext(),
            viewModel = viewModel,  // ViewModel'ı ekle
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

            // LiveData'yı dinle
            viewModel.currentLessons.observe(viewLifecycleOwner) { lessons ->
                lessons.forEachIndexed { index, item ->
                    Log.d("LESSON_OBSERVER", "[$index] ${item.title} - currentStep: ${item.currentStep} - isCompleted: ${item.isCompleted}")
                }
                lessonsAdapter.submitList(lessons)
            }

            // Scroll listener'ı ekle
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    android.util.Log.d("MapFragment", "Scroll detected: dx=$dx, dy=$dy")
                    updateStickyHeader(recyclerView, stickyHeader, stickySectionUnit, stickyHeaderTitle)
                }
            })

            // İlk yüklemede sticky header'ı güncelle
            post {
                android.util.Log.d("MapFragment", "Initial update of sticky header")
                updateStickyHeader(this, stickyHeader, stickySectionUnit, stickyHeaderTitle)
            }
        }
    }
    private fun updateStickyHeader(
        recyclerView: RecyclerView,
        stickyHeader: LinearLayout,
        stickySectionUnit: TextView,
        stickyHeaderTitle: TextView
    ) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val adapter = recyclerView.adapter as LessonAdapter

        android.util.Log.d("MapFragment", "Updating sticky header for position: $firstVisible")

        var headerTitle: String? = null
        var sectionUnit: String? = null
        for (i in firstVisible downTo 0) {
            val item = viewModel.currentLessons.value?.get(i)
            if (item?.type == LessonItem.TYPE_HEADER) {
                headerTitle = item.title
                sectionUnit = "${item.stepCount}. KISIM, ${item.currentStep}. ÜNİTE"

                val colorRes = item.color
                if (colorRes != null) {
                    val color = ContextCompat.getColor(requireContext(), colorRes)
                    android.util.Log.d("MapFragment", "Setting color for header: $headerTitle, colorRes: $colorRes")
                    stickyHeader.backgroundTintList = ColorStateList.valueOf(color)
                }
                break
            }
        }
        if (headerTitle != null) {
            stickySectionUnit.text = sectionUnit
            stickyHeaderTitle.text = headerTitle
            stickyHeader.visibility = View.VISIBLE
            android.util.Log.d("MapFragment", "Header visible: $headerTitle")
        } else {
            stickyHeader.visibility = View.GONE
            android.util.Log.d("MapFragment", "No header found, hiding sticky header")
        }
    }

    override fun onResume() {
        super.onResume()
        // Fragment yeniden görünür olduğunda sticky header'ı güncelle
        binding.lessonsRecyclerView.post {
            val stickyHeader = requireActivity().findViewById<LinearLayout>(R.id.stickyHeader)
            val stickySectionUnit = requireActivity().findViewById<TextView>(R.id.stickySectionUnit)
            val stickyHeaderTitle = requireActivity().findViewById<TextView>(R.id.stickyHeaderTitle)
            updateStickyHeader(binding.lessonsRecyclerView, stickyHeader, stickySectionUnit, stickyHeaderTitle)
        }
    }




}
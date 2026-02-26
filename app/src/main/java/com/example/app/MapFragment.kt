package com.example.app

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.GlobalLessonData.globalPartId
import com.example.app.GlobalValues.randomNumberChangeToString
import com.example.app.MathOperationGenerator.generateRandomMathOperation1
import com.example.app.databinding.FragmentMapBinding
import com.example.app.model.GuidePanelData
import com.example.app.model.LessonItem

class MapFragment : Fragment() {
    private lateinit var binding: FragmentMapBinding
    private lateinit var lessonsAdapter: LessonAdapter // Adapter'ı tanımla
    
    companion object {
        const val ARG_SHOW_GUIDE = "show_guide"
        
        fun newInstance(showGuide: Boolean = false): MapFragment {
            val fragment = MapFragment()
            val args = Bundle().apply {
                putBoolean(ARG_SHOW_GUIDE, showGuide)
            }
            fragment.arguments = args
            return fragment
        }
        
        fun getLessonOperations(lessonId: Int): List<MathOperation> {
            return when (lessonId) {
                1 -> listOf(
                    MathOperation(null,"2", null),
                    MathOperation(null,"5", null),
                    MathOperation(null,"3", null),
                    MathOperation(null,"8", null),
                    MathOperation(null,"4", null),
                    MathOperation(null,"1", null),
                    MathOperation(null,"9", null),
                    MathOperation(null,"6", null),
                    MathOperation(null,"7", null),
                    MathOperation(null,"8", null)
                )
                2 -> listOf(
                    MathOperation(null,randomNumberChangeToString(1), null),
                    MathOperation(null,randomNumberChangeToString(1), null),
                    MathOperation(null,randomNumberChangeToString(1), null),
                    MathOperation(null,randomNumberChangeToString(1), null),
                    MathOperation(null,randomNumberChangeToString(1), null),
                    MathOperation(null,randomNumberChangeToString(1), null),
                    MathOperation(null,randomNumberChangeToString(1), null)
                )
                3 -> listOf(
                    MathOperation(null,randomNumberChangeToString(1), null),
                    MathOperation(null,randomNumberChangeToString(1), null),
                    MathOperation(null,randomNumberChangeToString(1), null),
                    MathOperation(null,randomNumberChangeToString(1), null),
                    MathOperation(null,randomNumberChangeToString(1), null),
                    MathOperation(null,randomNumberChangeToString(1), null),
                    MathOperation(null,randomNumberChangeToString(1), null),
                )
                1000 -> listOf(
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                )
                1001 -> listOf(
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                )
                1002 -> listOf(
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                    MathOperation(null,randomNumberChangeToString(2), null),
                )
                1003 -> listOf(
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                )
                1004 -> listOf(
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                )
                1005 -> listOf(
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                    MathOperation(null,randomNumberChangeToString(5), null),
                    MathOperation(null,randomNumberChangeToString(5), null),
                    MathOperation(null,randomNumberChangeToString(5), null),
                    MathOperation(null,randomNumberChangeToString(5), null),
                )
                4 -> listOf(
                    MathOperation(4,"+", 5),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),

                )
                5 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                )
                6 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    )
                1007 -> listOf(
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
                1008 -> listOf(
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
                1009 -> listOf(
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
                1010 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                )
                1011 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                )
                1012 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                )
                1013 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
                    MathOperationGenerator.generateRelatedNumbers(4, 4),
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
                    MathOperationGenerator.generateRelatedNumbers(4, 4),
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
                )
                1014 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
                    MathOperationGenerator.generateRelatedNumbers(4, 4),
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
                    MathOperationGenerator.generateRelatedNumbers(4, 4),
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
                )
                7 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers0(1, 1),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(2, 2),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(3, 3),
                    MathOperationGenerator.generateRelatedNumbers(4, 4),
                    MathOperationGenerator.generateRelatedNumbers(4, 4),
                    MathOperationGenerator.generateRelatedNumbers(4, 4),
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
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
                    MathOperationGenerator.generateRelatedNumbers2(2, 3),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(3, 2),
                    MathOperationGenerator.generateRelatedNumbers2(2, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 2),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(2, 2),
                    )
                13 -> listOf(
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
                14 -> listOf(
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
                    MathOperationGenerator.generateRelatedNumbers2(4, 4),
                    MathOperationGenerator.generateRelatedNumbers2(4, 4),
                    MathOperationGenerator.generateRelatedNumbers2(4, 4),
                    MathOperationGenerator.generateRelatedNumbers2(4, 4),
                    MathOperationGenerator.generateRelatedNumbers2(4, 4),
                    MathOperationGenerator.generateRelatedNumbers2(4, 4),
                    MathOperationGenerator.generateRelatedNumbers2(4, 4),
                    MathOperationGenerator.generateRelatedNumbers2(4, 4),
                    MathOperationGenerator.generateRelatedNumbers2(4, 4),
                    MathOperationGenerator.generateRelatedNumbers2(4, 4),
                )
                17 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers2(4, 4),
                    MathOperationGenerator.generateRelatedNumbers2(5, 4),
                    MathOperationGenerator.generateRelatedNumbers2(4, 4),
                    MathOperationGenerator.generateRelatedNumbers2(4, 5),
                    MathOperationGenerator.generateRelatedNumbers2(5, 5),
                    MathOperationGenerator.generateRelatedNumbers2(4, 4),
                    MathOperationGenerator.generateRelatedNumbers2(5, 4),
                    MathOperationGenerator.generateRelatedNumbers2(4, 5),
                    MathOperationGenerator.generateRelatedNumbers2(5, 5),
                    )
                18 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers2(5, 5),
                    MathOperationGenerator.generateRelatedNumbers2(5, 5),
                    MathOperationGenerator.generateRelatedNumbers2(5, 5),
                    MathOperationGenerator.generateRelatedNumbers2(5, 5),
                    MathOperationGenerator.generateRelatedNumbers2(5, 5),
                    MathOperationGenerator.generateRelatedNumbers2(5, 5),
                    MathOperationGenerator.generateRelatedNumbers2(5, 5),
                    MathOperationGenerator.generateRelatedNumbers2(5, 5),

                    )
                19 -> listOf(
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 3),
                    MathOperationGenerator.generateRelatedNumbers2(3, 4),
                    MathOperationGenerator.generateRelatedNumbers2(4, 3),
                    MathOperationGenerator.generateRelatedNumbers2(5, 5),
                    MathOperationGenerator.generateRelatedNumbers2(5, 4),
                    MathOperationGenerator.generateRelatedNumbers2(5, 5),
                    MathOperationGenerator.generateRelatedNumbers2(5, 5),
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
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                )
                29 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                )
                30 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                )
                31 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                )
                32 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(3,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4)
                )
                33 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(3,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4)
                )
                34 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(3,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4)
                )
                35 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(3,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4)
                )
                36 -> listOf(
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,4),
                    MathOperationGenerator.generateMathOperationWithDigits(3,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4)
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
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),

                    )
                43 -> listOf(
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),
                    MathOperationGenerator.generateMathOperation4(),
                )
                42 -> listOf(
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
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2)

                    )
                77 -> listOf(
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2)

                    )
                78 -> listOf(
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2)

                    )
                79 -> listOf(
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),

                    )
                80 -> listOf(
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),

                    )
                81 -> listOf(
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),

                    )
                82 -> listOf(
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),

                    )
                83 -> listOf(
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),

                    )
                84 -> listOf(
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),

                    )
                85 -> listOf(
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),

                    )
                86 -> listOf(
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),

                    )
                87 -> listOf(
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),

                    )
                88 -> listOf(
                    MathOperation(10, "-", 1),
                    MathOperation(11, "-", 2),
                    MathOperation(12, "-", 3),
                    MathOperation(13, "-", 4),
                    MathOperation(14, "-", 5),
                    MathOperation(15, "-", 6),
                    MathOperation(16, "-", 7),
                    MathOperation(17, "-", 8),
                    MathOperation(18, "-", 9)
                )
                89 -> listOf(
                    MathOperation(100, "-", 10),
                    MathOperation(10, "-", 2),
                    MathOperation(11, "-", 3),
                    MathOperation(12, "-", 4),
                    MathOperation(13, "-", 5),
                    MathOperation(10, "-", 6),
                    MathOperation(15, "-", 7),
                    MathOperation(17, "-", 8),
                    MathOperation(10, "-", 9)
                )
                90 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                )
                91 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                )
                92 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                )
                93 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                )
                94 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                )
                95 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                )
                96 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                )
                97 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                )
                98 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                )
                99 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRules(),
                )
                100 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                )
                101 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                )
                102 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                )
                103 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                )
                104 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                )
                105 -> listOf(
                    MathOperation(11,"-", 6),
                    MathOperation(22,"-", 6),
                    MathOperation(13,"-", 7),
                    MathOperation(12,"-", 7),
                    MathOperation(23,"-", 8),
                    MathOperation(34,"-", 8),
                    MathOperation(24,"-", 9),
                    MathOperation(14,"-", 9),
                    MathOperation(34,"-", 6),
                    MathOperation(24,"-", 7)
                )
                106 -> listOf(
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                )
                107 -> listOf(
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                )
                108 -> listOf(
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                )
                109 -> listOf(
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                )
                110 -> listOf(
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                )
                111 -> listOf(
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                )
                112 -> listOf(
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                )
                113 -> listOf(
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                )
                114 -> listOf(
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                )
                115 -> listOf(
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                )
                116 -> listOf(
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                )
                117 -> listOf(
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),

                    )
                118 -> listOf(
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),

                    )
                119 -> listOf(
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),

                    )
                120 -> listOf(
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),

                    )
                121 -> listOf(
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),

                    )
                122 -> listOf(
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                )
                123 -> listOf(
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                )
                124 -> listOf(
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                )
                125 -> listOf(
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                )
                126 -> listOf(
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                )
                127 -> listOf(
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                )
                128 -> listOf(
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                )
                129 -> listOf(
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                )
                130 -> listOf(
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),

                    )
                131 -> listOf(
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                )
                132 -> listOf(
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                )
                133 -> listOf(
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                )
                134 -> listOf(
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                )
                135 -> listOf(
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                )
                136 -> listOf(
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                )
                137 -> listOf(
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                )
                138 -> listOf(
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                )
                139 -> listOf(
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    )
                140 -> listOf(
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                )
                141 -> listOf(
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                )
                142 -> listOf(
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                )
                143 -> listOf(
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                )
                144 -> listOf(
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),
                    MathOperationGenerator.multiplicationThreeFull(),

                    )
                145 -> listOf(
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                )
                146 -> listOf(
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                )
                147 -> listOf(
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                )
                148 -> listOf(
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                )
                149 -> listOf(
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                )
                150 -> listOf(
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                )
                151 -> listOf(
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                )
                152 -> listOf(
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                )
                153 -> listOf(
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFive(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                    MathOperationGenerator.multiplicationThreeTwoFull(),
                )
                else -> emptyList()
            }
        }
        fun getLessonOperationsBlinding(lessonId: Int): List<Any>{
            return when (lessonId) {
                1 -> listOf(
                    listOf(10,3,5,10),
                    listOf(7,20,10,11),
                    listOf(12,10,5,50),
                    listOf(2,5,2,40),
                    listOf(5,3,20,10)
                )
                2 -> listOf(
                    listOf(13,10,1,20),
                    listOf(4,20,50,10),
                    listOf(13,10,5,50),
                    listOf(6,1,10,10),
                    listOf(10,5,10,11)
                )
                3 -> listOf(
                    listOf(10,3,5,10),
                    listOf(7,20,10,11),
                    listOf(12,10,5,50),
                    listOf(2,5,2,40),
                    listOf(5,3,20,10)
                )
                4 -> listOf(
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                )
                5 -> listOf(
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                )
                6 -> listOf(
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                )
                7 -> listOf(
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                )
                8 -> listOf(
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                )
                9 -> listOf(
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                )
                10 -> listOf(
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                )
                11 -> listOf(
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                )
                12 -> listOf(
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                )
                13 -> listOf(
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                    MathOperationGenerator.generateSequence1(3),
                )
                14 -> listOf(
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3)
                )
                15 -> listOf(
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3)
                )
                16 -> listOf(
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3)
                )
                17 -> listOf(
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3)
                )
                18 -> listOf(
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3),
                    MathOperationGenerator.generateSequence5Rules(3)
                )
                19 -> listOf(
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                )
                20 -> listOf(
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                )
                21 -> listOf(
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                )
                22 -> listOf(
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                )
                23 -> listOf(
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),
                    MathOperationGenerator.generateSequence10RulesEasy(3),

                    )
                24 -> listOf(
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                )
                25 -> listOf(
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                )
                26 -> listOf(
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                )
                27 -> listOf(
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                )
                28 -> listOf(
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                    MathOperationGenerator.generateSequence10RulesEasyOld(3),
                )
                29 -> listOf(
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                )
                30 -> listOf(
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                )
                31 -> listOf(
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                )
                32 -> listOf(
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                )
                33 -> listOf(
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                    MathOperationGenerator.generateSequenceBeadRules(3),
                )
                34 -> listOf(
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                )
                35 -> listOf(
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                )
                36 -> listOf(
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                )
                37 -> listOf(
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                )
                38 -> listOf(
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                    MathOperationGenerator.generateSequenceExtraction(3),
                )
                39 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                )
                40 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                )
                41 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                )
                42 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                )
                43 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                    MathOperationGenerator.generateSequenceExtractionFiveRules(3),
                )
                44 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                )
                45 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                )
                46 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                )
                47 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                )
                48 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                    MathOperationGenerator.generateSequenceExtractionTenRules(3),
                )
                49 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                )
                50 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                )
                51 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                )
                52 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                )
                53 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                    MathOperationGenerator.generateSequenceExtractionBeadRules(3),
                )
                54 -> listOf(
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),

                )
                55 -> listOf(
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),

                )
                56 -> listOf(
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),

                )
                57 -> listOf(
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),

                )
                58 -> listOf(
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                )
                59 -> listOf(
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                )
                60 -> listOf(
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                )
                61 -> listOf(
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                )
                62 -> listOf(
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFive(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull(),
                    MathOperationGenerator.multiplicationLessFiveFull()
                )
                63 -> listOf(
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                )
                64-> listOf(
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                )
                65 -> listOf(
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                )
                66 -> listOf(
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                )
                67 -> listOf(
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                    MathOperationGenerator.multiplicationFull(),
                )
                68 -> listOf(
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                )
                69 -> listOf(
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                )
                70 -> listOf(
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                )
                71 -> listOf(
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                )
                72 -> listOf(
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                    MathOperationGenerator.multiplicationTwo(),
                )
                73 -> listOf(
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                )
                74 -> listOf(
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                )
                75 -> listOf(
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                )
                76 -> listOf(
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                )
                77 -> listOf(
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                    MathOperationGenerator.multiplicationTwoFull(),
                )
                78 -> listOf(
                    MathOperationGenerator.generalCollectionOneDigits(2),
                )
                79 -> listOf(
                    MathOperationGenerator.generalCollectionTwoDigits(4),
                    MathOperationGenerator.generalCollectionTwoDigits(4),
                    MathOperationGenerator.generalCollectionTwoDigits(4),
                    MathOperationGenerator.generalCollectionTwoDigits(4),
                    MathOperationGenerator.generalCollectionTwoDigits(4),
                    MathOperationGenerator.generalCollectionTwoDigits(4),
                    MathOperationGenerator.generalCollectionTwoDigits(4),
                )
                80 -> listOf(
                    MathOperationGenerator.generalCollectionTwoDigits(7),
                    MathOperationGenerator.generalCollectionTwoDigits(7),
                    MathOperationGenerator.generalCollectionTwoDigits(7),
                    MathOperationGenerator.generalCollectionTwoDigits(7),
                    MathOperationGenerator.generalCollectionTwoDigits(7)
                )
                81 -> listOf(
                    MathOperationGenerator.generalCollectionThreeDigits(4),
                    MathOperationGenerator.generalCollectionThreeDigits(4),
                    MathOperationGenerator.generalCollectionThreeDigits(4),
                    MathOperationGenerator.generalCollectionThreeDigits(4),
                    MathOperationGenerator.generalCollectionThreeDigits(4),
                    MathOperationGenerator.generalCollectionThreeDigits(4),
                    MathOperationGenerator.generalCollectionThreeDigits(4),
                )
                82 -> listOf(
                    MathOperationGenerator.generalCollectionThreeDigits(7),
                    MathOperationGenerator.generalCollectionThreeDigits(7),
                    MathOperationGenerator.generalCollectionThreeDigits(7),
                    MathOperationGenerator.generalCollectionThreeDigits(7)
                )
                83 -> listOf(
                    MathOperationGenerator.generalCollectionFourDigits(4),
                    MathOperationGenerator.generalCollectionFourDigits(4),
                    MathOperationGenerator.generalCollectionFourDigits(4),
                    MathOperationGenerator.generalCollectionFourDigits(4),
                    MathOperationGenerator.generalCollectionFourDigits(4),
                    MathOperationGenerator.generalCollectionFourDigits(4),
                    MathOperationGenerator.generalCollectionFourDigits(4),
                )
                84 -> listOf(
                    MathOperationGenerator.generalCollectionFourDigits(7),
                    MathOperationGenerator.generalCollectionFourDigits(7),
                    MathOperationGenerator.generalCollectionFourDigits(7),
                    MathOperationGenerator.generalCollectionFourDigits(7)
                )
                85 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRace(4),
                )
                86 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(4),
                )
                87 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(7)
                )
                88 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(4),
                )
                89 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(7)
                )
                90 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(4),
                )
                91 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(7)
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

        // Veriyi yükle (lokal → yoksa Firestore → yoksa varsayılan); hazır olunca listeyi kur
        GlobalLessonData.initialize(requireContext(), globalPartId) {
            activity?.runOnUiThread {
                setupRecyclerView()
                changeAdapterList()
            }
        }
        setupGuidePanel()
        
        // View hazır olduğunda scroll listener'ı tetikle
        view.post {
            if (GlobalValues.scrollPosition > 0) {
                binding.lessonsRecyclerView.scrollToPosition(GlobalValues.scrollPosition)
            }
            
            // Guide panel gösterilmeli mi kontrol et
            val showGuide = arguments?.getBoolean(ARG_SHOW_GUIDE, false) ?: false
            if (showGuide) {
                // CupFragment'ten ilk defa çağrıldığında mı kontrol et
                val prefs = requireContext().getSharedPreferences("GuidePanelPrefs", android.content.Context.MODE_PRIVATE)
                val cupFragmentGuideShown = prefs.getBoolean("cupFragment_guide_shown", false)
                
                if (!cupFragmentGuideShown) {
                    // İlk defa gösteriliyorsa flag'i kaydet
                    prefs.edit().putBoolean("cupFragment_guide_shown", true).apply()
                    // Önce tıklamaları engelle (gecikme süresinde de tıklanamaz)
                    disableMainActivityViews()
                    disableMapFragmentViews() // Overlay ekle
                    // MapFragment'in görünmesini beklemek için 1 saniye geciktir
                    view.postDelayed({
                        showGuidePanel()
                    }, 1000)
                }
            }
        }
    }
    
    private fun setupGuidePanel() {
        binding.guidePanel.setOnBackClickListener {
            // İlk adımdaysa ve back'e basılırsa paneli kapat
            binding.guidePanel.hide()
        }
        
        // Panel kapandığında MainActivity view'larını tekrar aktif et
        binding.guidePanel.setOnPanelHideListener {
            enableMainActivityViews()
        }
        
        // GuidePanel'in son adımına gelindiğinde recordLayout animasyonunu başlat
        // Her GuidePanel kendi animasyonunu bu callback içinde tanımlayabilir
        binding.guidePanel.setOnLastStepReachedListener {
            if (::lessonsAdapter.isInitialized) {
                // MapFragment için: recordLayout animasyonu
                val activity = requireActivity()
                val coordinatorLayout = activity.findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.coordinator_layout)
                val bottomSheetView = coordinatorLayout?.findViewWithTag<View>("bottom_sheet")
                val recordLayout = bottomSheetView?.findViewById<android.widget.LinearLayout>(R.id.recordLayout)

                recordLayout?.let { recordLayoutView ->
                    // Animasyonun daha önce gösterilip gösterilmediğini kontrol et
                    val prefs = requireContext().getSharedPreferences("GuidePanelPrefs", android.content.Context.MODE_PRIVATE)
                    val animationShown = prefs.getBoolean("recordLayout_animation_shown", false)

                    // Eğer animasyon daha önce gösterilmemişse başlat
                    if (!animationShown) {
                        lessonsAdapter.startPulseAnimationForView(recordLayoutView, stopAnimationOnClick = false)
                    }

                    // recordLayout'a tıklandığında: animasyonu durdur + paneli kapat + bottomSheet'i kapat + activity view'larını aktif et + flag'i kaydet
                    binding.guidePanel.setTargetViewForLastStep(recordLayoutView) {
                        // Animasyonu durdur
                        val animator = recordLayoutView.tag as? android.animation.ValueAnimator
                        animator?.cancel()
                        recordLayoutView.tag = null
                        recordLayoutView.scaleX = 1f
                        recordLayoutView.scaleY = 1f

                        // Animasyonun gösterildiğini kaydet (bir daha gösterilmesin)
                        prefs.edit().putBoolean("recordLayout_animation_shown", true).apply()

                        // BottomSheet'i kapat
                        val activity = requireActivity()
                        val coordinatorLayout = activity.findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.coordinator_layout)
                        val bottomSheetView = coordinatorLayout?.findViewWithTag<View>("bottom_sheet")
                        val bottomSheetLayout = bottomSheetView?.findViewById<android.widget.LinearLayout>(R.id.bottomSheetLayout)
                        bottomSheetLayout?.let { layout ->
                            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(layout)
                            behavior.isHideable = true
                            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
                        }

                        // Activity view'larını aktif et
                        enableMainActivityViews()

                        // Panel kapatma işlemi setTargetViewForLastStep içinde otomatik yapılacak
                    }
                }
            }
        }
    }
    
    private fun showGuidePanel() {
        // Guide panel verilerini oluştur
        val guideData = listOf(
            GuidePanelData(
                imageResId = R.drawable.teacher_emotes_gpt4,
                text = "Ünite değerlendirmede hatasız en hızlı çözümler kaydedilir."
            ),
            GuidePanelData(
                imageResId = R.drawable.teacher_emotes_gpt3,
                text = "Ünite değerlendirme panelindeki Rekor'a tıklayarak liderlik tablosunu ve kendi sıranı görebilirsin."
            )
            // Daha fazla resim/text eklenebilir
        )
        
        binding.guidePanel.setGuideData(guideData)
        
        // MainActivity'deki view'lar zaten disableMainActivityViews() ile devre dışı bırakıldı
        // (gecikme süresinde tıklamalar engellenmesi için onViewCreated'da çağrılıyor)
        
        // Panel'i göster
        binding.guidePanel.show()
        
        // Panel animasyonu tamamlandıktan sonra overlay'i kaldır (panel 500ms animasyon + 1000ms ek güvenlik)
        view?.postDelayed({
            enableMapFragmentViews() // Overlay'i kaldır (Panel tamamen göründükten sonra)
        }, 1500) // Panel animasyonu (500ms) + ek güvenlik (1000ms)
        
        // Panel gösterildikten sonra LessonAdapter'daki showLessonBottomSheet'i çağır
        // 1. listenin 3. index'i (0-based index: 3)
        view?.postDelayed({
            val lessonItem = GlobalLessonData.getLessonItem(4)
            if (lessonItem != null && ::lessonsAdapter.isInitialized) {
                lessonsAdapter.showLessonBottomSheet(lessonItem, 4)
            }
        }, 600) // Panel animasyonu tamamlandıktan sonra
    }
    
    private fun disableMainActivityViews() {
        val activity = requireActivity()
        
        // Currency panel (üstteki panel)
        activity.findViewById<View>(R.id.currencyPanel)?.apply {
            isClickable = false
            isFocusable = false
            setOnTouchListener { _, _ -> true } // Touch event'leri consume et
        }
        
        // Enerji text
        activity.findViewById<View>(R.id.energyText)?.apply {
            isClickable = false
            isFocusable = false
            setOnTouchListener { _, _ -> true } // Touch event'leri consume et
            setOnClickListener(null) // Click listener'ı kaldır
        }
        
        // Enerji icon
        activity.findViewById<View>(R.id.energyIcon)?.apply {
            isClickable = false
            isFocusable = false
            setOnTouchListener { _, _ -> true } // Touch event'leri consume et
            setOnClickListener(null) // Click listener'ı kaldır
        }
        
        // Bottom navigation
        activity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationID)?.apply {
            isClickable = false
            isFocusable = false
            isEnabled = false // Tamamen devre dışı bırak (tıklama efektleri de engellenir)
            setOnTouchListener { _, _ -> true } // Touch event'leri consume et
            setOnItemSelectedListener(null) // Item selected listener'ı kaldır
            
            // Menu item'lerini de devre dışı bırak
            menu.setGroupEnabled(0, false)
        }
        
        android.util.Log.d("MapFragment", "MainActivity views disabled")
    }
    
    private var overlayView: View? = null
    
    private fun disableMapFragmentViews() {
        // RecyclerView'ı devre dışı bırak (kaydırmayı engelle)
        binding.lessonsRecyclerView.apply {
            isClickable = false
            isFocusable = false
            setOnTouchListener { _, _ -> true } // Touch event'leri consume et
        }
        
        // Overlay view oluştur ve ekle (tüm touch event'leri consume edecek)
        overlayView = View(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnTouchListener { _, _ -> true } // Tüm touch event'leri consume et
        }
        // Overlay'i root view'a ekle
        (binding.root as? ViewGroup)?.addView(overlayView)
    }
    
    private fun enableMapFragmentViews() {
        // RecyclerView'ı tekrar aktif et
        binding.lessonsRecyclerView.apply {
            isClickable = true
            isFocusable = true
            setOnTouchListener(null) // Touch listener'ı kaldır
        }
        
        // Overlay'i kaldır
        overlayView?.let { overlay ->
            (binding.root as? ViewGroup)?.removeView(overlay)
            overlayView = null
        }
    }
    
    private fun enableMainActivityViews() {
        val activity = requireActivity()
        
        // Currency panel (üstteki panel)
        activity.findViewById<View>(R.id.currencyPanel)?.apply {
            isClickable = true
            isFocusable = true
            setOnTouchListener(null) // Touch listener'ı kaldır
        }
        
        // Enerji text
        activity.findViewById<View>(R.id.energyText)?.apply {
            isClickable = true
            isFocusable = true
            setOnTouchListener(null) // Touch listener'ı kaldır
        }
        
        // Enerji icon
        activity.findViewById<View>(R.id.energyIcon)?.apply {
            isClickable = true
            isFocusable = true
            setOnTouchListener(null) // Touch listener'ı kaldır
        }
        
        // Bottom navigation
        activity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationID)?.apply {
            isClickable = true
            isFocusable = true
            isEnabled = true // Tekrar aktif et
            setOnTouchListener(null) // Touch listener'ı kaldır
            // Menu item'lerini tekrar aktif et
            menu.setGroupEnabled(0, true)
        }
        
        // MainActivity'deki click listener'ları yeniden set et
        if (activity is MainActivity) {
            activity.setupClickListeners()
        }
        
        // MapFragment view'larını da tekrar aktif et
        enableMapFragmentViews()
        
        android.util.Log.d("MapFragment", "MainActivity views enabled")
    }

    override fun onPause() {
        super.onPause()
        // Scroll pozisyonunu GlobalValues'a kaydet
        GlobalValues.scrollPosition = (binding.lessonsRecyclerView.layoutManager as LinearLayoutManager)
            .findFirstVisibleItemPosition()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Verileri kaydet
        GlobalLessonData.saveToPreferences(requireContext())
    }

    private fun setupRecyclerView() {
        lessonsAdapter = LessonAdapter(
            context = requireContext(),
            items = GlobalLessonData.lessonItems.toMutableList(),
            onPartChange = { newPartId ->
                globalPartId = newPartId
                GlobalLessonData.initialize(requireContext(), newPartId) {
                    activity?.runOnUiThread {
                        lessonsAdapter.updateItems(GlobalLessonData.lessonItems)
                    }
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

    private fun changeAdapterList(){
        //GlobalLessonData.initialize(requireContext(),globalPartId)
        lessonsAdapter.updateItems(GlobalLessonData.lessonItems)

    }
    
    fun refreshRacePanel() {
        // Race panel açıksa yenile
        lessonsAdapter.refreshRacePanelIfOpen()
    }

    private fun updateStickyHeader(
        recyclerView: RecyclerView,
        stickyHeader: LinearLayout,
        stickySectionUnit: TextView,
        stickyHeaderTitle: TextView
    ) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val adapter = recyclerView.adapter as? LessonAdapter ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()

        android.util.Log.d("MapFragment", "Updating sticky header for position: $firstVisible")

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
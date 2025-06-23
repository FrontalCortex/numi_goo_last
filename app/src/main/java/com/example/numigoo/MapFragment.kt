package com.example.numigoo

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
import com.example.numigoo.GlobalLessonData.globalPartId
import com.example.numigoo.GlobalValues.randomNumberChangeToString
import com.example.numigoo.MathOperationGenerator.generateRandomMathOperation1
import com.example.numigoo.databinding.FragmentMapBinding
import com.example.numigoo.model.LessonItem

class MapFragment : Fragment() {
    private lateinit var binding: FragmentMapBinding
    private lateinit var lessonsAdapter: LessonAdapter // Adapter'ı tanımla
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

        // Kayıtlı verileri yükle
        GlobalLessonData.loadFromPreferences(requireContext())

        // Eğer veri yoksa veya farklı bir part'a geçiliyorsa initialize et
        GlobalLessonData.initialize(requireContext(),globalPartId)

        setupRecyclerView()
        changeAdapterList()
        // View hazır olduğunda scroll listener'ı tetikle
        view.post {
            if (GlobalValues.scrollPosition > 0) {
                binding.lessonsRecyclerView.scrollToPosition(GlobalValues.scrollPosition)
            }
        }
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
        // Adapter'ı oluştur
        // Burada Çıkartma adapter'ini bağlayacağımız yer olacak. Belkide aynı adapter içerisinde
        // Çıkartma kısmı ekleriz. Yani devam et'e tıklandığında çıkartma item'leri bağlanabilir.
        // Yapıyı cursor'a açıkla ve nasıl kurgulayabileceğini öğren. En mantıklısı ona sormak.
        lessonsAdapter = LessonAdapter(
            context = requireContext(),
            items = GlobalLessonData.lessonItems.toMutableList(),
            onPartChange = { newPartId ->
                globalPartId = newPartId  // currentPartId yerine globalPartId kullanıyoruz
                GlobalLessonData.initialize(requireContext(),newPartId)
                lessonsAdapter.updateItems(GlobalLessonData.lessonItems)
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
        GlobalLessonData.initialize(requireContext(),globalPartId)
        lessonsAdapter.updateItems(GlobalLessonData.lessonItems)

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
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
import com.example.numigoo.MathOperationGenerator.generateRandomMathOperation1
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

        // Global verileri kullan
        val items = GlobalLessonData.lessonItems
        if (items.isEmpty()) {
            // İlk kez çalışıyorsa varsayılan verileri yükle
            GlobalLessonData.initialize(createLessonItems())
        }

        setupRecyclerView()

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
        LessonDataManager.saveLessonItems(requireContext(), GlobalLessonData.lessonItems)
    }

    private fun setupRecyclerView() {
        // Adapter'ı oluştur
        // Burada Çıkartma adapter'ini bağlayacağımız yer olacak. Belkide aynı adapter içerisinde
        // Çıkartma kısmı ekleriz. Yani devam et'e tıklandığında çıkartma item'leri bağlanabilir.
        // Yapıyı cursor'a açıkla ve nasıl kurgulayabileceğini öğren. En mantıklısı ona sormak.
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
                lessonHint = getString(R.string.lesson_hint_step1)
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
                mapFragmentIndex = 2,
                finishStepNumber = 7,
                lessonHint = getString(R.string.lesson_hint_step2)
            ),
            LessonItem(
                type = LessonItem.TYPE_CHEST,
                title = "Ünite Değerlendirme",
                offset = 0,
                isCompleted = true,
                stepCount = 1,
                currentStep = 1,
                mapFragmentIndex = 3,
                finishStepNumber = 7,
                startStepNumber = 7,
                tutorialIsFinish = true,
                lessonHint = getString(R.string.lesson_hint_step3),
                cupTime1 = "1:30",
                cupTime2 = "2:00"

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
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                tutorialNumber = 3,
                startStepNumber = 8,
                mapFragmentIndex = 5,
                finishStepNumber = 9,
                lessonHint = getString(R.string.lesson_hint_step4)


            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Zor 5'lik toplama",
                offset = -60,
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                startStepNumber = 12,
                mapFragmentIndex = 6,
                finishStepNumber = 15,
                tutorialNumber = 4

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "İmkansız 5'lik toplama",
                offset = -30,
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                startStepNumber = 15,
                mapFragmentIndex = 7,
                finishStepNumber = 18,
                tutorialIsFinish = true


                ),
            LessonItem(
                type = LessonItem.TYPE_CHEST,
                title = "Ünite Değerlendirme",
                offset = 0,
                isCompleted = true,
                stepCount = 1,
                currentStep = 1,
                tutorialIsFinish = true,
                mapFragmentIndex = 8,
                startStepNumber = 19,
                finishStepNumber = 19,
                cupTime1 = "2:00",
                cupTime2 = "3:00"

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
                offset = 0,
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                startStepNumber = 20,
                mapFragmentIndex = 10,
                finishStepNumber = 23,
                tutorialNumber = 5

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Zor 10'luk toplama",
                offset = -30,
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                startStepNumber = 24,
                mapFragmentIndex = 11,
                finishStepNumber = 27,
                tutorialNumber = 6

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "İmkansız 10'luk toplama",
                offset = -60,
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                startStepNumber = 28,
                mapFragmentIndex = 12,
                finishStepNumber = 31,
                tutorialIsFinish = true
            ),LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Çılgın 10'luk toplama",
                offset = -30,
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                startStepNumber = 32,
                mapFragmentIndex = 13,
                finishStepNumber = 35,
                tutorialIsFinish = true
            ),LessonItem(
                type = LessonItem.TYPE_CHEST,
                title = "Ünite Değerlendirme",
                offset = 0,
                isCompleted = true,
                stepCount = 1,
                currentStep = 1,
                tutorialIsFinish = true,
                mapFragmentIndex = 14,
                startStepNumber = 36,
                finishStepNumber = 36,
                cupTime1 = "2:00",
                cupTime2 = "3:00"
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
                offset = 0,
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                startStepNumber = 37,
                mapFragmentIndex = 16,
                finishStepNumber = 40,
                tutorialNumber = 7

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "10'luk toplama mantığı",
                offset = -30,
                isCompleted = true,
                stepCount = 3,
                currentStep = 1,
                startStepNumber = 41,
                mapFragmentIndex = 17,
                finishStepNumber = 43,
                tutorialIsFinish = true,
                lessonHint = getString(R.string.lesson_hint_step2)

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Zor 10'luk toplama",
                offset = 0,
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                startStepNumber = 44,
                mapFragmentIndex = 18,
                finishStepNumber = 47,
                tutorialIsFinish = true,
                lessonHint = getString(R.string.lesson_hint_step2)

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Çılgın 10'luk toplama",
                offset = 30,
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                startStepNumber = 48,
                mapFragmentIndex = 19,
                finishStepNumber = 51,
                tutorialIsFinish = true,
                lessonHint = getString(R.string.lesson_hint_step2)
            ),
            LessonItem(
                type = LessonItem.TYPE_CHEST,
                title = "Ünite Değerlendirme",
                offset = 0,
                isCompleted = true,
                stepCount = 1,
                currentStep = 1,
                tutorialIsFinish = true,
                mapFragmentIndex = 20,
                startStepNumber = 52,
                finishStepNumber = 52,
                cupTime1 = "3:00",
                cupTime2 = "4:00"

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
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                startStepNumber = 53,
                mapFragmentIndex = 22,
                finishStepNumber = 56,
                tutorialNumber = 8,

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "Zor Boncuk Kuralı",
                offset = 0,
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                startStepNumber = 57,
                mapFragmentIndex = 23,
                finishStepNumber = 60,
                tutorialNumber = 9,
                lessonHint = getString(R.string.lesson_hint_step5)

            ),
            LessonItem(
                type = LessonItem.TYPE_LESSON,
                title = "İmkansız Boncuk Kuralı",
                offset = -30,
                isCompleted = true,
                stepCount = 4,
                currentStep = 1,
                startStepNumber = 61,
                mapFragmentIndex = 24,
                finishStepNumber = 64,
                tutorialIsFinish = true,
                lessonHint = getString(R.string.lesson_hint_step5)

            ),
            LessonItem(
                type = LessonItem.TYPE_CHEST,
                title = "Ünite Değerlendirme",
                offset = 0,
                isCompleted = true,
                stepCount = 1,
                currentStep = 1,
                tutorialIsFinish = true,
                mapFragmentIndex = 25,
                startStepNumber = 65,
                finishStepNumber = 65,
                cupTime1 = "3:00",
                cupTime2 = "4:00"
            ),
            LessonItem(
                type = LessonItem.TYPE_HEADER,
                title = "Ustalık Yolu",
                offset = 0,
                isCompleted = false,
                stepCount = 1,
                currentStep = 1,
                color = R.color.lesson_header_green
            ),
            LessonItem(
                type = LessonItem.TYPE_RACE,
                title = "Ustalık Yolu",
                offset = 0,
                isCompleted = true,
                stepCount = 1,
                currentStep = 1,
                tutorialIsFinish = true,
                mapFragmentIndex = 27,

            ),
            LessonItem(
                type = LessonItem.TYPE_PART,
                title = "2. Kısım",
                offset = 0,
                isCompleted = true,
                stepCount = 1,
                currentStep = 1,
                tutorialIsFinish = true,
                mapFragmentIndex = 27,
                sectionTitle = "2. Kısım Çıkartma",
                sectionDescription = "Abaküste çıkartmaya dair her şeyi öğreneceğiz. "

            )
        )
    }


}
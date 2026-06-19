package com.example.app

import android.animation.ObjectAnimator
import android.content.Context
import android.util.Log
import android.widget.Toast
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
import com.example.app.GlobalValues.randomUniqueNumberStrings
import com.example.app.MathOperationGenerator.generateRandomMathOperation1
import com.example.app.MathOperationGenerator.generateRelatedNumbers2
import com.example.app.databinding.FragmentMapBinding
import com.example.app.model.GuidePanelData
import com.example.app.model.LessonItem

class MapFragment : Fragment() {
    private lateinit var binding: FragmentMapBinding
    private lateinit var lessonsAdapter: LessonAdapter // Adapter'ı tanımla
    private var askQuestionBounceAnimators: List<ObjectAnimator>? = null

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
                2 -> randomUniqueNumberStrings(1, 7).map { n ->
                    MathOperation(null, n, null)
                }
                1000 -> randomUniqueNumberStrings(2, 7).map { n ->
                    MathOperation(null, n, null)
                }
                1003 -> listOf(
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                    MathOperation(null,randomNumberChangeToString(5), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                    MathOperation(null,randomNumberChangeToString(3), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                    MathOperation(null,randomNumberChangeToString(5), null),
                    MathOperation(null,randomNumberChangeToString(4), null),
                )
                1005 -> listOf(
                    1 to 2,
                    2 to 2,
                    3 to 3,
                    4 to 2,
                    5 to 3,
                ).flatMap { (digitCount, count) ->
                    randomUniqueNumberStrings(digitCount, count).map { n ->
                        MathOperation(null, n, null)
                    }
                }
                4 -> listOf(MathOperation(4, "+", 5)) +
                        MathOperationGenerator.generateRelatedNumbersList(6, 1, 1)
                5 ->  MathOperationGenerator.generateRelatedNumbersList(8, 1, 1)
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
                1010 -> MathOperationGenerator.generateRelatedNumbersList(6, 3, 3)

                1013 -> listOf(
                    MathOperationGenerator.generateRelatedNumbers(4, 4),
                    MathOperationGenerator.generateRelatedNumbers(5, 5),
                    MathOperationGenerator.generateRelatedNumbers(4, 4),
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
                    MathOperation(2,"+", 3),
                    MathOperation(3,"+", 3))
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
                12 -> listOf(
                    generateRelatedNumbers2(2, 3),
                    generateRelatedNumbers2(2, 2),
                    generateRelatedNumbers2(3, 2),
                    generateRelatedNumbers2(2, 3),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(2, 2),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 2),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(2, 2),
                    )
                13 -> listOf(
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 3),
                    )
                16 -> listOf(
                    generateRelatedNumbers2(4, 4),
                    generateRelatedNumbers2(4, 4),
                    generateRelatedNumbers2(4, 4),
                    generateRelatedNumbers2(4, 4),
                    generateRelatedNumbers2(4, 4),
                    generateRelatedNumbers2(4, 4),
                    generateRelatedNumbers2(4, 4)
                )
                17 -> listOf(
                    generateRelatedNumbers2(4, 4),
                    generateRelatedNumbers2(5, 4),
                    generateRelatedNumbers2(4, 4),
                    generateRelatedNumbers2(4, 5),
                    generateRelatedNumbers2(5, 5),
                    generateRelatedNumbers2(4, 4),
                    generateRelatedNumbers2(5, 4),
                    )
                18 -> listOf(
                    generateRelatedNumbers2(5, 5),
                    generateRelatedNumbers2(5, 5),
                    generateRelatedNumbers2(5, 5),
                    generateRelatedNumbers2(5, 5),
                    generateRelatedNumbers2(5, 5),
                    generateRelatedNumbers2(5, 5),
                    generateRelatedNumbers2(5, 5),
                    )
                19 -> listOf(
                    generateRandomMathOperation1(),
                    generateRandomMathOperation1(),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 3),
                    generateRelatedNumbers2(3, 4),
                    generateRelatedNumbers2(4, 3),
                    generateRelatedNumbers2(5, 5),
                    generateRelatedNumbers2(5, 4),
                    generateRelatedNumbers2(5, 5),
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
                    MathOperation(4,"+", 4))
                21 -> MathOperationGenerator.generateMathOperationList(9)
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
                )
                32 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                )
                36 -> listOf(
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperation(),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits(4,4)
                    )
                37 -> listOf(
                    MathOperation(4, "+", 6),
                    MathOperation(4, "+", 9),
                    MathOperation(17, "+", 8),
                    MathOperation(3, "+", 7),
                    MathOperation(3, "+", 8),
                    MathOperation(4, "+", 7),
                    MathOperation(4, "+", 6),
                    MathOperation(2, "+", 8),
                    MathOperation(8, "+", 9)
                )
                38 -> MathOperationGenerator.generateMathOperationList3(8)
                41 -> MathOperationGenerator.generateMathOperationList4(8)
                44 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(3,3),
                )
                48 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                )
                52 -> listOf(
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperation3(),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(2,2),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,4),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,3),
                    MathOperationGenerator.generateMathOperationWithDigits2(4,4),
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
                )
                61 -> listOf(
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),
                )
                65 -> listOf(
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationBeadRule(),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(2,2),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(3,3),
                    MathOperationGenerator.generateMathOperationWithDigitsBeadRule(4,4),

                    )
                66 -> listOf(
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2)
                )
                70 -> listOf(
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(5,5),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(5,5),
                    MathOperationGenerator.irregularExtraction(4,4),
                    )
                74 -> listOf(
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(2,2),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(3,3),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(4,4),
                    MathOperationGenerator.irregularExtraction(5,5),
                    MathOperationGenerator.irregularExtraction(5,5),
                    MathOperationGenerator.irregularExtraction(5,5),
                    MathOperationGenerator.irregularExtraction(5,5),

                )
                75 -> listOf(
                    MathOperation(5, "-", 2),
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
                    )
                83 -> listOf(
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),

                    )
                87 -> listOf(
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(2,2),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRules(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(3,3),
                    MathOperationGenerator.irregularExtractionFiveRulesMix(4,4),


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
                90 -> MathOperationGenerator.extractionGenerateMathOperationList(9)
                91 -> MathOperationGenerator.extractionGenerateMathOperationList(12)
                92 -> listOf(
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
                )
                100 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtreme()
                )
                104 -> listOf(
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperation(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTen(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
                    MathOperationGenerator.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(),
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
                106 -> MathOperationGenerator.extractionBeadRulesList(8)
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
                )
                113 -> listOf(
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree()
                )
                117 -> listOf(
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRules(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesThreeTwo(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
                    MathOperationGenerator.extractionBeadRulesFourThree(),
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

        /**
         * [requestedId] için operasyon yoksa [minLessonId]'ye kadar birer azaltarak ilk dolu id'yi döner.
         */
        fun resolveLessonOperationsId(requestedId: Int, minLessonId: Int): Int {
            var id = requestedId
            while (getLessonOperations(id).isEmpty() && id > minLessonId) {
                id--
            }
            return id
        }

        fun resolveLessonOperationsBlindingId(requestedId: Int, minLessonId: Int): Int {
            var id = requestedId
            while (getLessonOperationsBlinding(id).isEmpty() && id > minLessonId) {
                id--
            }
            return id
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
                    MathOperationGenerator.generalCollectionOneDigits(4),
                    MathOperationGenerator.generalCollectionOneDigits(4),
                    MathOperationGenerator.generalCollectionOneDigits(4),
                )
                1000 -> listOf(
                    MathOperationGenerator.generalCollectionOneDigits(6),
                    MathOperationGenerator.generalCollectionOneDigits(6),
                    MathOperationGenerator.generalCollectionOneDigits(6),
                )
                79 -> listOf(
                    MathOperationGenerator.generalCollectionTwoDigits(4),
                    MathOperationGenerator.generalCollectionTwoDigits(4),
                    MathOperationGenerator.generalCollectionTwoDigits(4),
                )
                80 -> listOf(
                    MathOperationGenerator.generalCollectionTwoDigits(4), //3 adet 7 olarak güncellenecek
                    MathOperationGenerator.generalCollectionTwoDigits(4),
                    MathOperationGenerator.generalCollectionTwoDigits(4),
                )
                1001 -> listOf(
                    MathOperationGenerator.generalCollectionTwoDigits(6),
                    MathOperationGenerator.generalCollectionTwoDigits(6),
                    MathOperationGenerator.generalCollectionTwoDigits(6),
                    )
                81 -> listOf(
                    MathOperationGenerator.generalCollectionThreeDigits(4),
                    MathOperationGenerator.generalCollectionThreeDigits(4),
                    MathOperationGenerator.generalCollectionThreeDigits(4),
                )
                1002 -> listOf(
                    MathOperationGenerator.generalCollectionThreeDigits(6),
                    MathOperationGenerator.generalCollectionThreeDigits(6),
                    MathOperationGenerator.generalCollectionThreeDigits(6),
                )
                82 -> listOf(
                    MathOperationGenerator.generalCollectionThreeDigits(7),
                    MathOperationGenerator.generalCollectionThreeDigits(7),
                    MathOperationGenerator.generalCollectionThreeDigits(7),
                )
                83 -> listOf(
                    //MathOperationGenerator.generalCollectionFourDigits(4),
                    //MathOperationGenerator.generalCollectionFourDigits(4),
                    //MathOperationGenerator.generalCollectionFourDigits(4),
                )
                1003 -> listOf(
                    MathOperationGenerator.generalCollectionOneDigits(15),
                    MathOperationGenerator.generalCollectionOneDigits(15),
                    MathOperationGenerator.generalCollectionOneDigits(15)
                )
                1004 -> listOf(
                    MathOperationGenerator.generalCollectionTwoDigits(10),
                    MathOperationGenerator.generalCollectionTwoDigits(10),
                    MathOperationGenerator.generalCollectionTwoDigits(10)
                )
                1005 -> listOf(
                    MathOperationGenerator.generalCollectionThreeDigits(10),
                    MathOperationGenerator.generalCollectionThreeDigits(10),
                    MathOperationGenerator.generalCollectionThreeDigits(10)
                )
                1006 -> listOf(
                    MathOperationGenerator.generalCollectionOneDigits(20),
                    MathOperationGenerator.generalCollectionOneDigits(20),
                    MathOperationGenerator.generalCollectionOneDigits(20)
                )
                84 -> listOf(
                    MathOperationGenerator.generalCollectionFourDigits(10),
                    MathOperationGenerator.generalCollectionFourDigits(10),
                    MathOperationGenerator.generalCollectionFourDigits(10),
                )
                85 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRace(4),
                    MathOperationGenerator.generateSequenceExtractionRace(4),
                    MathOperationGenerator.generateSequenceExtractionRace(4),
                )
                1007 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRace(6),
                    MathOperationGenerator.generateSequenceExtractionRace(6),
                    MathOperationGenerator.generateSequenceExtractionRace(6),
                )
                86 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(4),
                )
                1008 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(6),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(6),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(6)
                )
                87 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigits(7),
                )
                88 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(4),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(4),
                )
                1009 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceHard(15),
                    MathOperationGenerator.generateSequenceExtractionRaceHard(15),
                    MathOperationGenerator.generateSequenceExtractionRaceHard(15),
                )
                1010 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigitsHard(10),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigitsHard(10),
                    MathOperationGenerator.generateSequenceExtractionRaceTwoDigitsHard(10),
                )
                1011 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(10),
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(10),
                    MathOperationGenerator.generateSequenceExtractionRaceFourDigits(10),
                )
                89 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(7),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigits(7)
                )
                90 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigitsHard(10),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigitsHard(10),
                    MathOperationGenerator.generateSequenceExtractionRaceThreeDigitsHard(10),
                )
                91 -> listOf(
                    MathOperationGenerator.generateSequenceExtractionRaceHard(20),
                    MathOperationGenerator.generateSequenceExtractionRaceHard(20),
                    MathOperationGenerator.generateSequenceExtractionRaceHard(20),
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
        setupAskQuestionButton()
        
        // View hazır olduğunda scroll listener'ı tetikle
        view.post {
            // İlk açılışta liste her zaman en üstten başlasın.
            // Sadece state restore senaryosunda eski pozisyonu uygula.
            if (savedInstanceState != null && GlobalValues.scrollPosition > 0) {
                binding.lessonsRecyclerView.scrollToPosition(GlobalValues.scrollPosition)
            }
            
            // Bekleyen maraton rehberi (ChestFragment claim sonrası)
            (activity as? MainActivity)?.tryShowPendingMarathonGuideOnMap("MapFragment.onViewCreated")
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
            if (mapTransparentTouchBlockActive || overlayView?.parent != null) {
                MapTouchDiagnostics.reportFromFragment(
                    this,
                    "MapFragment.guidePanel.hide",
                    "WARN_MAP_BLOCK_STILL_ON",
                    "Panel kapandı ama enableMapFragmentViews henüz çağrılmamış olabilir",
                )
            }
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

                    // recordLayout'a tıklandığında: animasyonu durdur + paneli kapat + RecordFragment aç
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

                        enableMainActivityViews()
                        enableMapFragmentViews()

                        val chestIndex = MarathonGuideStore.firstMarathonLessonIndex()
                        val chestItem = GlobalLessonData.getLessonItem(chestIndex)
                        if (chestItem != null) {
                            openRecordFragmentFromMarathonGuide(chestIndex, chestItem)
                        }

                        // Panel kapatma işlemi setTargetViewForLastStep içinde otomatik yapılacak
                    }
                }
            }
        }
    }

    private fun setupAskQuestionButton() {
        val authManager = com.example.app.auth.AuthManager().also { it.initialize(requireContext()) }
        AskQuestionButtonBinder.bind(
            fragment = this,
            button = binding.askQuestionButton,
            isTeacher = authManager.getCurrentUserType() == com.example.app.auth.AuthManager.ROLE_TEACHER,
            onAllowedClick = {
                (activity as? MainActivity)?.startQuestionFlow(R.id.fragmentContainerID) { binding.root }
            },
            onReadyForBounce = { startAskQuestionBounceAnimation() },
        )
    }

    private fun startAskQuestionBounceAnimation() {
        val btn = binding.askQuestionButton
        // Denizde süzülür gibi hafif aşağı-yukarı hareket (sabit boyut)
        val translateY = ObjectAnimator.ofFloat(btn, "translationY", 0f, -20f, 0f).apply {
            duration = 1800
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
        askQuestionBounceAnimators = listOf(translateY)
    }
    
    private fun showGuidePanel() {
        // Guide panel verilerini oluştur
        val guideData = listOf(
            GuidePanelData(
                imageResId = R.drawable.teacher_emotes_gpt4,
                text = "Ünite maratonunda hatasız en hızlı çözümler kaydedilir."
            ),
            GuidePanelData(
                imageResId = R.drawable.teacher_emotes_gpt3,
                text = "Ünite maratonu panelindeki Rekor'a tıklayarak liderlik tablosunu ve kendi sıranı görebilirsin."
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
        view?.postDelayed({
            val chestIndex = MarathonGuideStore.firstMarathonLessonIndex()
            val lessonItem = GlobalLessonData.getLessonItem(chestIndex)
            if (lessonItem != null && ::lessonsAdapter.isInitialized) {
                lessonsAdapter.showLessonBottomSheet(lessonItem, chestIndex)
            }
        }, 600) // Panel animasyonu tamamlandıktan sonra
    }

    private fun openRecordFragmentFromMarathonGuide(lessonIndex: Int, item: LessonItem) {
        if (!isAdded) return
        val act = requireActivity()
        val openRecord = {
            act.findViewById<View>(R.id.abacusFragmentContainer).visibility = View.VISIBLE
            act.supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right,
                )
                .replace(
                    R.id.abacusFragmentContainer,
                    RecordFragment.newInstance(globalPartId, lessonIndex, item.title),
                )
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }
        (act as? MainActivity)?.runAbacusOverlayTransaction("marathonGuide.record") { openRecord() }
            ?: openRecord()
    }

    private var marathonGuidePresentationScheduled = false

    /** [MarathonGuideStore] pending bayrağı set edildiyse ve harita üstü temizse rehber panelini aç. */
    fun maybeShowPendingMarathonGuide(caller: String) {
        if (!isAdded || view == null) {
            Log.d(
                MarathonGuideStore.LOG_TAG,
                "show SKIP | caller=$caller reason=fragment_not_ready isAdded=$isAdded viewNull=${view == null}",
            )
            return
        }
        val ctx = context ?: run {
            Log.d(MarathonGuideStore.LOG_TAG, "show SKIP | caller=$caller reason=context_null")
            return
        }
        MarathonGuideStore.logPrefsSnapshot(ctx, "maybeShow:$caller")
        if (!MarathonGuideStore.isPending(ctx)) {
            Log.d(MarathonGuideStore.LOG_TAG, "show SKIP | caller=$caller reason=not_pending")
            return
        }
        if (MarathonGuideStore.isShown(ctx)) {
            Log.w(
                MarathonGuideStore.LOG_TAG,
                "show SKIP | caller=$caller reason=already_shown clearing_stale_pending",
            )
            MarathonGuideStore.clearPending(ctx)
            return
        }
        val main = activity as? MainActivity ?: run {
            Log.d(MarathonGuideStore.LOG_TAG, "show SKIP | caller=$caller reason=activity_not_main")
            return
        }
        val blockReason = main.marathonGuideMapBlockReason()
        if (blockReason != null) {
            Log.d(
                MarathonGuideStore.LOG_TAG,
                "show SKIP | caller=$caller reason=map_blocked block=$blockReason",
            )
            return
        }
        if (marathonGuidePresentationScheduled) {
            Log.d(
                MarathonGuideStore.LOG_TAG,
                "show SKIP | caller=$caller reason=presentation_already_scheduled",
            )
            return
        }

        marathonGuidePresentationScheduled = true
        Log.i(MarathonGuideStore.LOG_TAG, "show SCHEDULED | caller=$caller delay=1000ms")
        disableMainActivityViews()
        disableMapFragmentViews()
        view?.postDelayed({
            marathonGuidePresentationScheduled = false
            if (!isAdded || view == null) {
                Log.w(
                    MarathonGuideStore.LOG_TAG,
                    "show ABORT | caller=$caller reason=fragment_gone_after_delay",
                )
                return@postDelayed
            }
            if (!MarathonGuideStore.isPending(ctx)) {
                Log.w(
                    MarathonGuideStore.LOG_TAG,
                    "show ABORT | caller=$caller reason=pending_cleared_during_delay",
                )
                return@postDelayed
            }
            val blockAfterDelay = main.marathonGuideMapBlockReason()
            if (blockAfterDelay != null) {
                Log.w(
                    MarathonGuideStore.LOG_TAG,
                    "show ABORT | caller=$caller reason=map_blocked_after_delay block=$blockAfterDelay",
                )
                return@postDelayed
            }
            LessonProgressDiag.logItem(
                "MapFragment.maybeShowGuide",
                globalPartId,
                MarathonGuideStore.firstMarathonLessonIndex(),
                GlobalLessonData.getLessonItem(MarathonGuideStore.firstMarathonLessonIndex()),
                "showNOW",
            )
            Log.i(MarathonGuideStore.LOG_TAG, "show NOW | caller=$caller → showGuidePanel()")
            showGuidePanel()
            MarathonGuideStore.markShown(ctx)
        }, 1000)
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
    /** [disableMapFragmentViews] aktifken true — MapTouchDbg teşhisi. */
    private var mapTransparentTouchBlockActive = false

    data class MapTouchState(
        val transparentOverlayAttached: Boolean,
        val recyclerConsumingTouch: Boolean,
        val touchRoutingEnabled: Boolean,
    )

    fun currentMapTouchState(): MapTouchState? {
        if (!isAdded || view == null) return null
        val overlayAttached = overlayView != null && overlayView?.parent != null
        return MapTouchState(
            transparentOverlayAttached = overlayAttached,
            recyclerConsumingTouch = mapTransparentTouchBlockActive,
            touchRoutingEnabled = !mapTransparentTouchBlockActive,
        )
    }

    private fun disableMapFragmentViews() {
        mapTransparentTouchBlockActive = true
        MapTouchDiagnostics.reportFromFragment(
            this,
            "MapFragment.disableMapFragmentViews",
            "GUIDE_BLOCK_ON",
            "Cup/guide panel — şeffaf overlay ekleniyor",
        )
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
        val hadBlock = mapTransparentTouchBlockActive || (overlayView?.parent != null)
        mapTransparentTouchBlockActive = false
        if (hadBlock) {
            MapTouchDiagnostics.reportFromFragment(
                this,
                "MapFragment.enableMapFragmentViews",
                "MAP_BLOCK_OFF",
                "Şeffaf map overlay / RV touch block kaldırıldı",
            )
        }
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
        // Scroll pozisyonunu GlobalValues'a kaydet (view/layoutManager henüz hazır olmayabilir)
        if (!isAdded) return
        try {
            val lm = binding.lessonsRecyclerView?.layoutManager as? LinearLayoutManager
            if (lm != null) {
                GlobalValues.scrollPosition = lm.findFirstVisibleItemPosition()
            }
        } catch (_: Exception) {
            // binding veya RecyclerView erişilemezse atla
        }
    }

    override fun onDestroyView() {
        if (mapTransparentTouchBlockActive || overlayView?.parent != null) {
            MapTouchDiagnostics.reportFromFragment(
                this,
                "MapFragment.onDestroyView",
                "WARN_TOUCH_BLOCK_LEAK",
                "View yok edilirken şeffaf overlay / RV block hâlâ aktifti",
            )
        }
        askQuestionBounceAnimators?.forEach { it.cancel() }
        askQuestionBounceAnimators = null
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
            val stickyLinear = binding.root.findViewById<LinearLayout>(R.id.StickyLinear)
            val stickyHeader = requireActivity().findViewById<LinearLayout>(R.id.stickyHeader)
            val stickySectionUnit = requireActivity().findViewById<TextView>(R.id.stickySectionUnit)
            val stickyHeaderTitle = requireActivity().findViewById<TextView>(R.id.stickyHeaderTitle)
            
            val maxOffsetPx = resources.getDimensionPixelSize(R.dimen.max_lesson_offset)
            val cardPx = resources.getDimensionPixelSize(R.dimen.map_lesson_card_size)
            val itemPadPx = resources.getDimensionPixelSize(R.dimen.map_lesson_item_padding)
            val safetyPx = resources.getDimensionPixelSize(R.dimen.map_lesson_offset_edge_margin)

            addItemDecoration(
                DynamicOffsetDecoration(
                    maxOffsetPx = maxOffsetPx,
                    lessonCardSizePx = cardPx,
                    lessonItemHorizontalPaddingTotalPx = itemPadPx * 2,
                    edgeSafetyMarginPx = safetyPx,
                )
            )

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

            // Recycler sınırı artık tüm fragment: sticky yüksekliği kadar üst inset ver.
            // Böylece item'ler sticky alana kayarken kesilmez, sticky'nin altında akıyormuş gibi görünür.
            val extraTopGap = resources.getDimensionPixelSize(R.dimen.map_sticky_recycler_padding_top)
            val applyTopInset = {
                val stickyHeight = stickyLinear?.height ?: 0
                setPadding(paddingLeft, stickyHeight + extraTopGap, paddingRight, paddingBottom)
            }
            stickyLinear?.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                applyTopInset()
            }
            post { applyTopInset() }

            // Genişlik ilk kez >0 olduğunda decoration’ları yeniden ölç (post bazen erken kalabiliyor)
            addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                private var applied = false
                override fun onLayoutChange(
                    v: View, l: Int, t: Int, r: Int, b: Int,
                    oldL: Int, oldT: Int, oldR: Int, oldB: Int
                ) {
                    if (applied || v.width <= 0 || v.height <= 0) return
                    applied = true
                    removeOnLayoutChangeListener(this)
                    invalidateItemDecorations()
                }
            })

            // İlk yüklemede sticky header + decoration yenileme
            post {
                android.util.Log.d("MapFragment", "Initial update of sticky header")
                updateStickyHeader(this, stickyHeader, stickySectionUnit, stickyHeaderTitle)
                invalidateItemDecorations()

                // İlk açılışta (restore yoksa) ilk item'ı sticky'nin altında başlat.
                if (GlobalValues.scrollPosition <= 0) {
                    val stickyHeight = stickyLinear?.height ?: 0
                    val initialOffset = stickyHeight + extraTopGap
                    (layoutManager as? LinearLayoutManager)
                        ?.scrollToPositionWithOffset(0, initialOffset)
                }
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

    /** Recycler + MainActivity dokunma yönlendirmesini sıfırla (overlay / guide panel sonrası). */
    fun enableMapTouchRouting() {
        if (!isAdded || view == null) return
        enableMapFragmentViews()
        enableMainActivityViews()
        (activity as? MainActivity)?.logMapTouchDiag(
            "MapFragment.enableMapTouchRouting",
            "ENABLE_MAP_TOUCH",
            "Recycler + MainActivity listener'lar sıfırlandı",
        )
    }

    /** Ders overlay (Abacus quit, ChestResult vb.) kapandıktan sonra liste ve dokunuşları yenile. */
    fun notifyVisibleAfterOverlayDismiss() {
        if (!isAdded || view == null) return
        val chestIdx = MarathonGuideStore.firstMarathonLessonIndex()
        val claimMapIdx = GlobalValues.mapFragmentStepIndex
        LessonProgressDiag.logClaimVsMapAtNotify(
            "MapFragment.notifyVisibleAfterOverlayDismiss",
            globalPartId,
            claimMapIdx,
            LessonManager.getLessonItem(claimMapIdx),
        )
        LessonProgressDiag.logListChestFinishSummary(
            "MapFragment.notifyVisibleAfterOverlayDismiss",
            globalPartId,
            GlobalLessonData.lessonItems,
        )
        LessonProgressDiag.logItem(
            "MapFragment.notifyVisibleAfterOverlayDismiss",
            globalPartId,
            chestIdx,
            GlobalLessonData.getLessonItem(chestIdx),
            "marathonCard",
        )
        LessonProgressDiag.logItem(
            "MapFragment.notifyVisibleAfterOverlayDismiss",
            globalPartId,
            claimMapIdx,
            GlobalLessonData.getLessonItem(claimMapIdx),
            "claimedMapIdx",
        )
        (activity as? MainActivity)?.logMapTouchDiag(
            "MapFragment.notifyVisibleAfterOverlayDismiss",
            "NOTIFY_VISIBLE_ENTER",
        )
        (activity as? MainActivity)?.restoreMapUiAfterLessonOverlayDismiss()
        GlobalValues.canConsumePendingLessonProgressAnimations = true
        enableMapTouchRouting()
        (activity as? MainActivity)?.logMapTouchDiag(
            "MapFragment.notifyVisibleAfterOverlayDismiss",
            "NOTIFY_VISIBLE_DONE",
        )
        binding.lessonsRecyclerView.visibility = View.VISIBLE
        if (::lessonsAdapter.isInitialized) {
            lessonsAdapter.updateItems(GlobalLessonData.lessonItems)
        }
        binding.lessonsRecyclerView.invalidate()
        maybeShowPendingMarathonGuide("notifyVisibleAfterOverlayDismiss")
        scheduleMarathonGuideRetriesAfterMapVisible()
    }

    /** Sezon kapısı / overlay gecikmesinden sonra bekleyen maraton rehberini tekrar dene. */
    private fun scheduleMarathonGuideRetriesAfterMapVisible() {
        val ctx = context ?: return
        if (!MarathonGuideStore.isPending(ctx)) return
        val delaysMs = longArrayOf(1_200L, 2_800L)
        delaysMs.forEach { delay ->
            view?.postDelayed({
                if (!isAdded || view == null) return@postDelayed
                if (!MarathonGuideStore.isPending(requireContext())) return@postDelayed
                LessonProgressDiag.log(
                    "MapFragment.guideRetry",
                    "delay=${delay}ms pending still true → maybeShow",
                )
                maybeShowPendingMarathonGuide("notifyVisibleAfterOverlayDismiss+retry@${delay}ms")
            }, delay)
        }
    }

    /** Kaynak ID değişince (örn. Media3) eski color ID geçersiz olabilir; geçerli color yoksa varsayılan döner. */
    private fun safeColorRes(colorResId: Int): Int {
        val ctx = requireContext()
        return try {
            if (ctx.resources.getResourceTypeName(colorResId) == "color") {
                ContextCompat.getColor(ctx, colorResId)
            } else {
                ContextCompat.getColor(ctx, android.R.color.darker_gray)
            }
        } catch (_: Exception) {
            ContextCompat.getColor(ctx, android.R.color.darker_gray)
        }
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
                    val color = safeColorRes(colorRes)
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
        val mayConsumeProgress = (activity as? MainActivity)?.shouldConsumeLessonProgressAnimationsOnMap() == true
        if (mayConsumeProgress) {
            GlobalValues.canConsumePendingLessonProgressAnimations = true
        }
        if (::lessonsAdapter.isInitialized) {
            lessonsAdapter.updateItems(GlobalLessonData.lessonItems)
        }
        // Fragment yeniden görünür olduğunda sticky header'ı güncelle
        binding.lessonsRecyclerView.post {
            if (!isAdded) return@post
            val activity = activity ?: return@post
            val stickyHeader = activity.findViewById<LinearLayout>(R.id.stickyHeader)
            val stickySectionUnit = activity.findViewById<TextView>(R.id.stickySectionUnit)
            val stickyHeaderTitle = activity.findViewById<TextView>(R.id.stickyHeaderTitle)
            if (stickyHeader != null && stickySectionUnit != null && stickyHeaderTitle != null) {
                updateStickyHeader(binding.lessonsRecyclerView, stickyHeader, stickySectionUnit, stickyHeaderTitle)
            }
        }
        view?.post {
            if (!isAdded) return@post
            val act = activity as? MainActivity ?: return@post
            act.logChromeBlockerDiagnostic("MapFragment.onResume")
            val shouldReconcile = act.shouldReconcileAbacusOverlayOnMapResume()
            Log.d(
                MainActivity.FIRST_TUTORIAL_LOG_TAG,
                "MapFragment.onResume | shouldReconcile=$shouldReconcile",
            )
            if (shouldReconcile) {
                act.logMapTouchDiag(
                    "MapFragment.onResume",
                    "PATH_RECONCILE",
                    "shouldReconcile=true",
                )
                act.reconcileAbacusOverlayWhenMapIsBase()
            } else {
                act.logMapTouchDiag(
                    "MapFragment.onResume",
                    "PATH_SANITIZE",
                    "shouldReconcile=false",
                )
                act.sanitizeMapTouchSurface("MapFragment.onResume")
            }
            act.requestSeasonLeaderboardRewardGateIfPending()
            act.tryShowPendingMarathonGuideOnMap("MapFragment.onResume")
            act.logMapTouchDiag("MapFragment.onResume", "ON_RESUME_POST_DONE")
            act.logTouchDiag("MapFragment.onResume.post")
        }
    }
}
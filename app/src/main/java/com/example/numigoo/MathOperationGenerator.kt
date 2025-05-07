package com.example.numigoo

import android.util.Log

object MathOperationGenerator {
    private val digitRules = mapOf(
        1 to listOf(0, 1, 2, 3, 5, 6, 7, 8),
        2 to listOf(0, 1, 2, 5, 6, 7),
        3 to listOf(0, 1, 5, 6),
        4 to listOf(0, 5),
        5 to listOf(0, 1, 2, 3, 4),
        6 to listOf(0, 1, 2, 3),
        7 to listOf(0, 1, 2),
        8 to listOf(0, 1),
        9 to listOf(0)
    )

    fun generateRelatedNumbers(firstDigitCount: Int, secondDigitCount: Int): MathOperation {
        // İlk sayıyı oluştur
        var firstNumber: Int
        do {
            // İlk basamak için (0 olamaz)
            val firstDigit = (1..9).random()

            // Diğer basamaklar için
            val otherDigits = if (firstDigitCount > 1) {
                // Son basamak için özel kontrol (ikinci sayı tek basamaklı ise 9 olamaz)
                val lastDigit = if (secondDigitCount == 1) {
                    (0..8).random() // 9 hariç
                } else {
                    (0..9).random()
                }

                // Orta basamaklar için
                val middleDigits = if (firstDigitCount > 2) {
                    (0 until firstDigitCount - 2).map { (0..9).random() }.fold(0) { acc, digit -> acc * 10 + digit }
                } else {
                    0
                }

                middleDigits * 10 + lastDigit
            } else {
                0
            }

            firstNumber = firstDigit * Math.pow(10.0, (firstDigitCount - 1).toDouble()).toInt() + otherDigits
        } while (secondDigitCount == 1 && firstNumber % 10 == 9) // Son kontrol

        // İkinci sayıyı oluştur
        val firstNumberStr = firstNumber.toString()
        val secondNumberDigits = mutableListOf<Int>()

        // İkinci sayının her basamağı için
        for (i in 0 until secondDigitCount) {
            if (i < firstNumberStr.length) {
                // İlk sayının aynı basamağındaki rakama göre kural uygula
                val firstDigit = firstNumberStr[firstNumberStr.length - 1 - i].toString().toInt()
                var allowedDigits = digitRules[firstDigit] ?: listOf(0)

                // Eğer ikinci sayı tek basamaklı ve ilk sayının birler basamağı 9 değilse, 0'ı çıkar
                if (secondDigitCount == 1 && i == 0 && firstDigit != 9) {
                    allowedDigits = allowedDigits.filter { it != 0 }
                }

                // Eğer liste boşsa varsayılan olarak 1 kullan
                val selectedDigit = if (allowedDigits.isEmpty()) 1 else allowedDigits.random()
                secondNumberDigits.add(0, selectedDigit)
            } else {
                // Eğer ikinci sayı daha fazla basamağa sahipse, ekstra basamaklar için rastgele rakam
                secondNumberDigits.add(0, (0..9).random())
            }
        }

        // Basamakları birleştir
        val secondNumber = secondNumberDigits.fold(0) { acc, digit -> acc * 10 + digit }

        return MathOperation(firstNumber, "+", secondNumber)
    }
}
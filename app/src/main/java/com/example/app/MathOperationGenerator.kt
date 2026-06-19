package com.example.app

import android.util.Log
import com.example.app.GlobalValues.generateRandomNumber
import java.util.Random

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

    fun generateRelatedNumbers0(firstDigitCount: Int, secondDigitCount: Int): MathOperation {
        val onesDigitFirst = (1..8).random()
        val onesDigitSecond = when (onesDigitFirst) {
            1 -> listOf(1, 2, 3, 5, 6, 7, 8)
            2 -> listOf(1, 2, 5, 6, 7)
            3 -> listOf(1, 5, 6)
            4 -> listOf(5)
            5 -> listOf(1, 2, 3, 4)
            6 -> listOf(1, 2, 3)
            7 -> listOf(1, 2)
            8 -> listOf(1)
            else -> listOf()
        }
        val secondNumberOnes = onesDigitSecond[Random().nextInt(onesDigitSecond.size)]
        return MathOperation(onesDigitFirst, "+", secondNumberOnes)
    }

    fun generateRelatedNumbers(firstDigitCount: Int, secondDigitCount: Int): MathOperation {
        val firstNumber = generateFirstNumberForRelated(firstDigitCount)
        val secondNumber = generateSecondNumberForRelated(firstNumber, secondDigitCount)
        return MathOperation(firstNumber, "+", secondNumber)
    }

    /**
     * [generateRelatedNumbers] ile aynı kurallarda [count] adet toplama işlemi üretir.
     * firstNumber mümkün olduğunca listede tekrar etmez; havuz yetmezse tekrara düşer
     * ([GlobalValues.randomUniqueNumberStrings] mantığı).
     */
    fun generateRelatedNumbersList(
        count: Int,
        firstDigitCount: Int,
        secondDigitCount: Int,
    ): List<MathOperation> {
        if (count <= 0) return emptyList()

        val usedFirstNumbers = mutableSetOf<Int>()
        val operations = mutableListOf<MathOperation>()

        repeat(count) {
            val firstNumber = pickUniqueFirstNumberForRelated(firstDigitCount, usedFirstNumbers)
            usedFirstNumbers.add(firstNumber)
            val secondNumber = generateSecondNumberForRelated(firstNumber, secondDigitCount)
            operations.add(MathOperation(firstNumber, "+", secondNumber))
        }
        return operations
    }

    private fun pickUniqueFirstNumberForRelated(
        firstDigitCount: Int,
        usedFirstNumbers: Set<Int>,
    ): Int {
        repeat(500) {
            val candidate = generateFirstNumberForRelated(firstDigitCount)
            if (candidate !in usedFirstNumbers) return candidate
        }
        return generateFirstNumberForRelated(firstDigitCount)
    }

    private fun generateFirstNumberForRelated(firstDigitCount: Int): Int {
        val firstDigit = (1..8).random()
        val otherDigits = if (firstDigitCount > 1) {
            val tensDigit = (0..8).random()
            val remainingDigits = if (firstDigitCount > 2) {
                (0 until firstDigitCount - 2).map { (0..8).random() }
                    .fold(0) { acc, digit -> acc * 10 + digit }
            } else {
                0
            }
            remainingDigits * 10 + tensDigit
        } else {
            0
        }
        return firstDigit * Math.pow(10.0, (firstDigitCount - 1).toDouble()).toInt() + otherDigits
    }

    private fun generateSecondNumberForRelated(firstNumber: Int, secondDigitCount: Int): Int {
        val firstNumberStr = firstNumber.toString()
        val secondNumberDigits = mutableListOf<Int>()
        for (i in 0 until secondDigitCount) {
            if (i < firstNumberStr.length) {
                val firstDigit = firstNumberStr[firstNumberStr.length - 1 - i].toString().toInt()
                var allowedDigits = digitRules[firstDigit] ?: listOf(0)
                if (i == 1 || secondDigitCount == 1 || i == secondDigitCount - 1) {
                    allowedDigits = allowedDigits.filter { it != 0 }
                }
                val selectedDigit = if (allowedDigits.isEmpty()) 1 else allowedDigits.random()
                secondNumberDigits.add(0, selectedDigit)
            } else {
                val digit = when {
                    i == 1 -> (1..9).random()
                    secondDigitCount == 1 -> (1..9).random()
                    i == secondDigitCount - 1 -> (1..9).random()
                    else -> (0..9).random()
                }
                secondNumberDigits.add(0, digit)
            }
        }
        return secondNumberDigits.fold(0) { acc, digit -> acc * 10 + digit }
    }

    fun generateRelatedNumbers2(firstDigitCount: Int, secondDigitCount: Int): MathOperation {
        // İlk sayıyı oluştur - her basamak için %80 ihtimalle 1,2,3,4; %20 ihtimalle 5,6,7,8,9
        val firstNumberDigits = mutableListOf<Int>()
        val lowDigits = listOf(1, 2, 3, 4) // %80 ihtimalle
        val highDigits = listOf(5, 6, 7, 8, 9) // %20 ihtimalle
        
        for (i in 0 until firstDigitCount) {
            val digit = if (Math.random() < 0.8) {
                // %80 ihtimalle 1,2,3,4
                lowDigits.random()
            } else {
                // %20 ihtimalle 5,6,7,8,9
                highDigits.random()
            }
            firstNumberDigits.add(digit)
        }
        
        // Basamakları birleştirerek ilk sayıyı oluştur
        val firstNumber = firstNumberDigits.fold(0) { acc, digit -> acc * 10 + digit }
        
        // İlk sayının basamaklarını al
        val firstNumberStr = firstNumber.toString()

        // İkinci sayı için basamak kuralları
        val digitRules = mapOf(
            0 to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
            1 to listOf(1, 2, 3, 4, 5, 6, 7, 8),
            2 to listOf(1, 2, 3, 4, 5, 6, 7),
            3 to listOf(1, 2, 3, 4, 5, 6),
            4 to listOf(1, 2, 3, 4, 5),
            5 to listOf(1, 2, 3, 4),
            6 to listOf(1, 2, 3),
            7 to listOf(1, 2),
            8 to listOf(1),
            9 to listOf(0)
        )

        // İkinci sayıyı oluştur
        val secondNumberDigits = mutableListOf<Int>()

        // İkinci sayının her basamağı için
        for (i in 0 until secondDigitCount) {
            if (i < firstNumberStr.length) {
                // İlk sayının aynı basamağındaki rakama göre kural uygula
                val firstDigit = firstNumberStr[firstNumberStr.length - 1 - i].toString().toInt()
                var allowedDigits = digitRules[firstDigit] ?: listOf(0)

                // Özel durumlar için kontrol
                if (firstDigit == 1 && Math.random() < 0.8) {
                    // %50 ihtimalle 4
                    secondNumberDigits.add(0, 4)
                } else if (firstDigit == 2 && Math.random() < 0.8) {
                    // %50 ihtimalle 3 veya 4
                    secondNumberDigits.add(0, listOf(3, 4).random())
                } else {
                    // Normal durumda izin verilen rakamlardan birini seç
                    secondNumberDigits.add(0, allowedDigits.random())
                }
            } else {
                // Eğer ikinci sayı daha fazla basamağa sahipse, ekstra basamaklar için rastgele rakam
                secondNumberDigits.add(0, (0..9).random())
            }
        }

        // Basamakları birleştir
        val secondNumber = secondNumberDigits.fold(0) { acc, digit -> acc * 10 + digit }

        return MathOperation(firstNumber, "+", secondNumber)
    }
    fun generateRelatedNumbers2Blinding(count: Int, digitsOne: Int): List<Int> {
        if (count <= 0) return emptyList()
        val digitCount = digitsOne.coerceAtLeast(1)

        val lowDigits = listOf(1, 2, 3, 4)
        val highDigits = listOf(5, 6, 7, 8, 9)

        val digitRules = mapOf(
            0 to listOf(1, 2, 3, 4),
            1 to listOf(1, 2, 3, 4),
            2 to listOf(1, 2, 3, 4),
            3 to listOf(1, 2, 3, 4),
            4 to listOf(1, 2, 3, 4),
            5 to listOf(1, 2, 3, 4),
            6 to listOf(1, 2, 3),
            7 to listOf(1, 2),
            8 to listOf(1),
            9 to listOf(0)
        )

        fun toDigits(value: Int, size: Int): List<Int> {
            var x = value
            val arr = IntArray(size)
            for (i in size - 1 downTo 0) {
                arr[i] = x % 10
                x /= 10
            }
            return arr.toList()
        }

        fun randomDigit90_10(): Int =
            if (Math.random() < 0.9) lowDigits.random() else highDigits.random()

        val firstDigits = List(digitCount) { randomDigit90_10() }
        val firstNumber = firstDigits.fold(0) { acc, d -> acc * 10 + d }
        val numbers = mutableListOf(firstNumber)

        for (i in 1 until count) {
            val sumDigits = toDigits(numbers.sum(), digitCount)
            val newDigits = sumDigits.map { posDigit ->
                digitRules[posDigit]!!.random()
            }
            val newNumber = newDigits.fold(0) { acc, d -> acc * 10 + d }
            numbers.add(newNumber)
        }

        return numbers
    }
    fun generateRandomMathOperation1(): MathOperation {

        val firstNumberTens = (1..9).random()
        val firstNumberOnes: Int
        val secondNumberTens: Int
        val secondNumberOnes: Int

        if (firstNumberTens < 5) {
            val secondNumberTensList = when (firstNumberTens) {
                1 -> listOf(1, 2, 3, 4, 5, 6, 7, 8)
                2 -> listOf(1, 2, 3, 4, 5, 6, 7)
                3 -> listOf(1, 2, 3, 4, 5, 6)
                4 -> listOf(1, 2, 3, 4, 5)
                else -> listOf()
            }
            secondNumberTens = secondNumberTensList.random()
            val fiveRuleActive = when (firstNumberTens) {
                1 -> secondNumberTens == 4
                2 -> secondNumberTens == 3 || secondNumberTens == 4
                3 -> secondNumberTens in 2..4
                4 -> secondNumberTens in 1..4
                else -> false
            }
            if (fiveRuleActive) {
                firstNumberOnes = (1..9).random()
                val secondNumberOnesList = when (firstNumberOnes) {
                    1 -> listOf(1, 2, 3, 5, 6, 7, 8)
                    2 -> listOf(1, 2, 5, 6, 7)
                    3 -> listOf(1, 5, 6)
                    4 -> listOf(5)
                    5 -> listOf(1, 2, 3, 4)
                    6 -> listOf(1, 2, 3)
                    7 -> listOf(1, 2)
                    8 -> listOf(1)
                    9 -> listOf(0)
                    else -> listOf()
                }
                secondNumberOnes = secondNumberOnesList.random()
            } else {
                firstNumberOnes = (1..4).random()
                val secondNumberOnesList = when (firstNumberOnes) {
                    1 -> listOf(4)
                    2 -> listOf(3, 4)
                    3 -> listOf(2, 3, 4)
                    4 -> listOf(1, 2, 3, 4)
                    else -> listOf()
                }
                secondNumberOnes = secondNumberOnesList.random()
            }

        }
        else {
            val secondNumberTensList = when (firstNumberTens) {
                5 -> listOf(1, 2, 3, 4)
                6 -> listOf(1, 2, 3)
                7 -> listOf(1, 2)
                8 -> listOf(1)
                9 -> listOf(0)
                else -> listOf()
            }
            secondNumberTens = secondNumberTensList.random()
            firstNumberOnes = (1..4).random()
            val secondNumberOnesList = when (firstNumberOnes) {
                1 -> listOf(4)
                2 -> listOf(3, 4)
                3 -> listOf(2, 3, 4)
                4 -> listOf(1, 2, 3, 4)
                else -> listOf()
            }
            secondNumberOnes = secondNumberOnesList.random()
        }
        val firstNumber = firstNumberTens * 10 + firstNumberOnes
        val secondNumber = secondNumberTens * 10 + secondNumberOnes


        return MathOperation(firstNumber, "+", secondNumber)
    }
    fun generateMathOperation(): MathOperation { //basit 10'luk toplama
        return generateMathOperationWithSecondNumber((1..5).random())
    }

    /**
     * [generateMathOperation] ile aynı kurallarda [count] adet işlem üretir.
     * secondNumber (1..5) mümkün olduğunca tekrar etmez; havuz bitince karışık yeniden dolar.
     */
    fun generateMathOperationList(count: Int): List<MathOperation> {
        if (count <= 0) return emptyList()

        val pendingSecondNumbers = mutableListOf<Int>()
        val operations = mutableListOf<MathOperation>()

        repeat(count) {
            if (pendingSecondNumbers.isEmpty()) {
                pendingSecondNumbers.addAll((1..5).shuffled())
            }
            val secondNumber = pendingSecondNumbers.removeAt(0)
            operations.add(generateMathOperationWithSecondNumber(secondNumber))
        }
        return operations
    }

    private fun generateMathOperationWithSecondNumber(secondNumber: Int): MathOperation {
        val tensDigit = listOf(1, 2, 3, 5, 6, 7, 8).random()

        val onesDigit = when (secondNumber) {
            1 -> 9
            2 -> (8..9).random()
            3 -> (7..9).random()
            4 -> (6..9).random()
            5 -> (5..9).random()
            else -> 5
        }

        val firstNumber = tensDigit * 10 + onesDigit
        return MathOperation(firstNumber, "+", secondNumber)
    }
    fun generateMathOperation2(): MathOperation {
        // İlk sayı için onlar basamağı (sadece 4 veya 9)
        val tensDigit = listOf(4, 9).random()

        // İlk sayı için birler basamağı (5,6,7,8,9)
        val onesDigit = listOf(5, 6, 7, 8, 9).random()

        // İlk sayıyı oluştur
        val firstNumber = tensDigit * 10 + onesDigit

        // İkinci sayı için olası değerleri belirle
        val possibleSecondNumbers = when (onesDigit) {
            5 -> listOf(5)
            6 -> listOf(5, 4)
            7 -> listOf(5, 4, 3)
            8 -> listOf(5, 4, 3, 2)
            9 -> listOf(5, 4, 3, 2, 1)
            else -> listOf()
        }

        // İkinci sayıyı olası değerler arasından rastgele seç
        val secondNumber = possibleSecondNumbers.random()

        return MathOperation(firstNumber, "+", secondNumber)
    }
    fun generateMathOperationWithDigits(firstDigitCount: Int, secondDigitCount: Int): MathOperation {
        // İlk sayıyı oluştur
        var firstNumber = 0
        for (i in 0 until firstDigitCount) {
            val digit = (5..9).random()
            firstNumber = firstNumber * 10 + digit
        }

        // İkinci sayıyı oluştur
        var secondNumber = 0
        val firstNumberStr = firstNumber.toString()

        for (i in 0 until secondDigitCount) {
            val currentDigit = firstNumberStr[i].toString().toInt()
            val possibleDigits = when (currentDigit) {
                5 -> listOf(5)
                6 -> listOf(5,4)
                7 -> listOf(5,4,3)
                8 -> listOf(5,4,3,2)
                9 -> listOf(5,4,3,2,1)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            val digit = possibleDigits.random()
            secondNumber = secondNumber * 10 + digit
        }

        return MathOperation(firstNumber, "+", secondNumber)
    }
    fun generateMathOperation3(): MathOperation {
        return generateMathOperation3WithSecondNumber((6..9).random())
    }

    /**
     * [generateMathOperation3] ile aynı kurallarda [count] adet işlem üretir.
     * secondNumber (6..9) mümkün olduğunca tekrar etmez; havuz bitince karışık yeniden dolar.
     */
    fun generateMathOperationList3(count: Int): List<MathOperation> {
        if (count <= 0) return emptyList()

        val pendingSecondNumbers = mutableListOf<Int>()
        val operations = mutableListOf<MathOperation>()

        repeat(count) {
            if (pendingSecondNumbers.isEmpty()) {
                pendingSecondNumbers.addAll((6..9).shuffled())
            }
            val secondNumber = pendingSecondNumbers.removeAt(0)
            operations.add(generateMathOperation3WithSecondNumber(secondNumber))
        }
        return operations
    }

    private fun generateMathOperation3WithSecondNumber(secondNumber: Int): MathOperation {
        val tensDigit = listOf(1, 2, 3, 5, 6, 7, 8).random()
        val possibleOnesDigits = when (secondNumber) {
            5 -> listOf(5)
            6 -> listOf(4, 9)
            7 -> listOf(3, 4, 8, 9)
            8 -> listOf(2, 3, 4, 7, 8, 9)
            9 -> listOf(1, 2, 3, 6, 7, 8, 9)
            else -> listOf(5, 6, 7, 8, 9)
        }

        val onesDigit = possibleOnesDigits.random()
        val firstNumber = tensDigit * 10 + onesDigit
        return MathOperation(firstNumber, "+", secondNumber)
    }
    fun generateMathOperation4(): MathOperation {
        var tensDigit = listOf(4, 9).random()
        tensDigit *= 10

        val onesDigit = (1..9).random()

        val firstNumber = tensDigit + onesDigit


        // İkinci sayı için olası değerleri belirle
        val possibleSecondNumbers = when (onesDigit) {
            1 -> listOf(9)
            2 -> listOf(8, 9)
            3 -> listOf(7, 8, 9)
            4 -> listOf(6, 7, 8)
            5 -> listOf(5)
            6 -> listOf(9)
            7 -> listOf(8, 9)
            8 -> listOf(7, 8, 9)
            9 -> listOf(6, 7, 8, 9)
            else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9) // Bu durum artık oluşmayacak
        }

        // Olası değerlerden random bir sayı seç
        val secondNumber = possibleSecondNumbers.random()

        // MathOperation formatında döndür
        return MathOperation(firstNumber, "+", secondNumber)
    }

    /**
     * [generateMathOperation4] ile aynı kurallarda [count] adet işlem üretir.
     * secondNumber (6..9) mümkün olduğunca tekrar etmez; havuz bitince karışık yeniden dolar.
     */
    fun generateMathOperationList4(count: Int): List<MathOperation> {
        if (count <= 0) return emptyList()

        val pendingSecondNumbers = mutableListOf<Int>()
        val operations = mutableListOf<MathOperation>()

        repeat(count) {
            if (pendingSecondNumbers.isEmpty()) {
                pendingSecondNumbers.addAll((6..9).shuffled())
            }
            val secondNumber = pendingSecondNumbers.removeAt(0)
            operations.add(generateMathOperation4WithSecondNumber(secondNumber))
        }
        return operations
    }

    private fun generateMathOperation4WithSecondNumber(secondNumber: Int): MathOperation {
        val tensDigit = listOf(4, 9).random()
        val possibleOnesDigits = when (secondNumber) {
            6 -> listOf(4, 9)
            7 -> listOf(3, 4, 8, 9)
            8 -> listOf(2, 3, 4, 7, 8, 9)
            9 -> listOf(1, 2, 3, 6, 7, 8, 9)
            else -> (1..9).toList()
        }
        val onesDigit = possibleOnesDigits.random()
        val firstNumber = tensDigit * 10 + onesDigit
        return MathOperation(firstNumber, "+", secondNumber)
    }

    fun generateMathOperationWithDigits2(firstNumberDigits: Int, secondNumberDigits: Int): MathOperation {
        // İlk sayıyı oluştur
        var firstNumber = 0
        for (i in 0 until firstNumberDigits) {
            val digit = (1..9).random()
            firstNumber = firstNumber * 10 + digit
        }

        // İkinci sayıyı oluştur
        var secondNumber = 0
        val firstNumberStr = firstNumber.toString()

        for (i in 0 until secondNumberDigits) {
            val sourceIndex = firstNumberStr.length - secondNumberDigits + i
            val currentDigit = if (sourceIndex in firstNumberStr.indices) {
                firstNumberStr[sourceIndex].toString().toInt()
            } else {
                // secondNumberDigits > firstNumberDigits durumunda solda boş kalan basamaklar.
                9
            }
            val possibleDigits = when (currentDigit) {
                1 -> listOf(9)
                2 -> listOf(8, 9)
                3 -> listOf(7, 8, 9)
                4 -> listOf(6, 7, 8, 9)
                5 -> listOf(5)
                6 -> listOf(9)
                7 -> listOf(8, 9)
                8 -> listOf(7, 8, 9)
                9 -> listOf(6, 7, 8, 9)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            val digit = possibleDigits.random()
            secondNumber = secondNumber * 10 + digit
        }

        return MathOperation(firstNumber, "+", secondNumber)
    }
    fun generateMathOperationWithDigits3(firstNumberDigits: Int, secondNumberDigits: Int): MathOperation {
        // İlk sayıyı oluştur
        var firstNumber = 0
        for (i in 0 until firstNumberDigits) {
            val digit = (1..9).random()
            firstNumber = firstNumber * 10 + digit
        }

        // İkinci sayıyı oluştur
        var secondNumber = 0
        val firstNumberStr = firstNumber.toString()

        for (i in 0 until secondNumberDigits) {
            val currentDigit = firstNumberStr[i].toString().toInt()
            val possibleDigits = when (currentDigit) {
                1 -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
                2 -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
                3 -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
                4 -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
                5 -> listOf(1, 2, 3, 4, 5)
                6 -> listOf(1, 2, 3, 4, 5, 9)
                7 -> listOf(1, 2, 3, 4, 5, 8, 9)
                8 -> listOf(1, 2, 3, 4, 5, 7, 8, 9)
                9 -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            val digit = possibleDigits.random()
            secondNumber = secondNumber * 10 + digit
        }

        return MathOperation(firstNumber, "+", secondNumber)
    }
    //Boncuk kuralı harici bütün kuralları içeren ve basamak sayısı girdiğimiz fonksiyon
    //Ünite değerlendirmeye eklenecek bu yapı 2'ye 2 3'e 3 olarak sorulsa kıyak

    //Şimdi 2 basamaklıya 2 basamaklı toplama gelsin. Kuralsız Kurallı hepsi olabilir. Önce adım 2 eklensin ondan sonra bu dediğim eklensin.
    fun generateMathOperationBeadRule(): MathOperation { //boncuk kuralı
        // İlk sayının onlar basamağı (0, 4 ve 9 hariç)
        val tensDigit = listOf(1, 2, 3, 5, 6, 7, 8).random()

        // İlk sayının birler basamağı (5,6,7,8,9)
        val onesDigit = (5..8).random()

        val random = Random()

        // İlk sayıyı oluştur
        val firstNumber = tensDigit * 10 + onesDigit

        // İkinci sayıyı belirle (ilk sayının birler basamağına göre)
        val secondNumber = when (onesDigit) {
            8 -> 6
            7 -> (6..7).random()
            6 -> if (random.nextBoolean()) 8 else (6..7).random()
            5 -> if (random.nextBoolean()) 9 else (6..8).random()
            else -> 0 // Bu durum asla oluşmayacak ama Kotlin için gerekli
        }

        return MathOperation(firstNumber, "+", secondNumber)
    }
    fun generateMathOperationWithDigitsBeadRule(firstNumberDigits: Int, secondNumberDigits: Int): MathOperation {
        // İlk sayıyı oluştur
        var firstNumber = 0
        for (i in 0 until firstNumberDigits) {
            val digit = listOf(5, 6, 7, 8).random() // Sadece 5,6,7,8 sayılarından seçim yap
            firstNumber = firstNumber * 10 + digit
        }

        // İkinci sayıyı oluştur
        var secondNumber = 0
        val firstNumberStr = firstNumber.toString()

        for (i in 0 until secondNumberDigits) {
            val currentDigit = firstNumberStr[i].toString().toInt()
            val possibleDigits = when (currentDigit) {
                5 -> listOf(6, 7, 8, 9)
                6 -> listOf(6, 7, 8, 9)
                7 -> listOf(6, 7 ,8, 9)
                8 -> listOf(6, 7, 8, 9)
                else -> listOf(6, 7, 8, 9) // Bu duruma hiç girmeyecek ama yine de yazdım
            }

            val digit = possibleDigits.random()
            secondNumber = secondNumber * 10 + digit
        }

        return MathOperation(firstNumber, "+", secondNumber)
    }
    fun irregularExtraction(firstNumberDigits: Int, secondNumberDigits: Int): MathOperation {
        while (true) {
            var firstNumber = 0
            repeat(firstNumberDigits) {
                val digit = (1..9).random()
                firstNumber = firstNumber * 10 + digit
            }

            var secondNumber = 0
            val firstNumberStr = firstNumber.toString()

            for (i in 0 until secondNumberDigits) {
                val currentDigit = firstNumberStr[i].toString().toInt()
                val possibleDigits = when (currentDigit) {
                    1 -> listOf(1)
                    2 -> listOf(2, 1)
                    3 -> listOf(3, 2, 1)
                    4 -> listOf(4, 3, 2, 1)
                    5 -> listOf(5)
                    6 -> listOf(6, 5, 1)
                    7 -> listOf(7, 6, 5, 2, 1)
                    8 -> listOf(8, 7, 6, 5, 3, 2, 1)
                    9 -> listOf(9, 8, 7, 6, 5, 4, 3, 2, 1)
                    else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
                }

                val digit = possibleDigits.random()
                secondNumber = secondNumber * 10 + digit
            }

            if (firstNumber != secondNumber) {
                return MathOperation(firstNumber, "-", secondNumber)
            }
        }
    }
    fun irregularExtractionFiveRules(firstNumberDigits: Int, secondNumberDigits: Int): MathOperation {
        var firstNumber = 0
        repeat(firstNumberDigits) {
            val digit = (5..8).random()
            firstNumber = firstNumber * 10 + digit
        }

        // İkinci sayıyı oluştur
        var secondNumber = 0
        val firstNumberStr = firstNumber.toString()

        for (i in 0 until secondNumberDigits) {
            val currentDigit = firstNumberStr[i].toString().toInt()
            val possibleDigits = when (currentDigit) {
                5 -> listOf(4,3,2,1)
                6 -> listOf(4,3,2)
                7 -> listOf(4,3)
                8 -> listOf(4)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            val digit = possibleDigits.random()
            secondNumber = secondNumber * 10 + digit
        }
        return MathOperation(firstNumber, "-", secondNumber)
    }
    fun irregularExtractionFiveRulesMix(firstNumberDigits: Int, secondNumberDigits: Int): MathOperation {
        while (true) {
            var firstNumber = 0
            repeat(firstNumberDigits) {
                val digit = if (Math.random() < 0.9) {
                    listOf(5, 6, 7, 8, 9).random()
                } else {
                    listOf(1, 2, 3, 4).random()
                }
                firstNumber = firstNumber * 10 + digit
            }

            var secondNumber = 0
            val firstNumberStr = firstNumber.toString()

            for (i in 0 until secondNumberDigits) {
                val currentDigit = firstNumberStr[i].toString().toInt()
                val possibleDigits = when (currentDigit) {
                    5 -> listOf(5,4,3,2,1)
                    6 -> listOf(6,5,4,3,2,1)
                    7 -> {
                        if ((0..1).random() == 0) listOf(4,3) else listOf(7,6,5,2,1)
                    }
                    8 -> {
                        if ((0..1).random() == 0) listOf(4) else listOf(1,2,3,5,6,7,8)
                    }
                    1 -> listOf(1)
                    2 -> listOf(2,1)
                    3 -> listOf(3,2,1)
                    4 -> listOf(4,3,2,1)
                    9 -> listOf(9,8,7,6,5,4,3,2,1)
                    else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
                }

                val digit = possibleDigits.random()
                secondNumber = secondNumber * 10 + digit
            }

            if (firstNumber != secondNumber) {
                return MathOperation(firstNumber, "-", secondNumber)
            }
        }
    }
    /** İlk sayı birler basamağı (x) → çıkarma için izin verilen ikinci sayı (y). */
    private val extractionOnesToSecondOptions: Map<Int, List<Int>> = mapOf(
        0 to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
        1 to listOf(2, 3, 4, 5, 7, 8, 9),
        2 to listOf(3, 4, 5, 8, 9),
        3 to listOf(4, 5, 9),
        4 to listOf(5),
        5 to listOf(6, 7, 8, 9),
        6 to listOf(7, 8, 9),
        7 to listOf(8, 9),
        8 to listOf(9),
    )

    private fun extractionAllowedOnesDigitsForSecond(secondNumber: Int): List<Int> =
        (0..8).filter { secondNumber in (extractionOnesToSecondOptions[it] ?: emptyList()) }

    private fun extractionSecondOptionsForOnes(onesDigit: Int): List<Int> =
        extractionOnesToSecondOptions[onesDigit] ?: listOf(1)

    /**
     * Sabit ikinci sayı (1..9) ile çıkarma: birler basamağı ilişkisi korunur;
     * x, y'ye göre seçilir ([extractionAllowedOnesDigitsForSecond]).
     */
    private fun extractionGenerateMathOperationWithSecondNumber(secondNumber: Int): MathOperation {
        val allowedOnes = extractionAllowedOnesDigitsForSecond(secondNumber)
        val onesDigit = allowedOnes.random()
        val tensDigit = listOf(1, 2, 3, 4, 6, 7, 8, 9).random()
        val firstNumber = tensDigit * 10 + onesDigit
        return MathOperation(firstNumber, "-", secondNumber)
    }

    /**
     * İkinci sayılar 1..9 sırayla (her biri bir kez); [count] > 9 ise döngü tekrarlanır.
     * [generateRelatedNumbersList] / [generateMathOperationList] desenine benzer.
     */
    fun extractionGenerateMathOperationList(count: Int): List<MathOperation> {
        if (count <= 0) return emptyList()
        val pendingSeconds = mutableListOf<Int>()
        val operations = mutableListOf<MathOperation>()
        repeat(count) {
            if (pendingSeconds.isEmpty()) {
                pendingSeconds.addAll(1..9)
            }
            val secondNumber = pendingSeconds.removeAt(0)
            operations.add(extractionGenerateMathOperationWithSecondNumber(secondNumber))
        }
        return operations
    }

    fun extractionGenerateMathOperation(): MathOperation { // basit 10'luk çıkarma (tek soru)
        val tensDigit = listOf(1, 2, 3, 4, 6, 7, 8, 9).random()
        val onesDigit = (0..8).random()
        val firstNumber = tensDigit * 10 + onesDigit
        val secondNumber = extractionSecondOptionsForOnes(onesDigit).random()
        return MathOperation(firstNumber, "-", secondNumber)
    }
    fun extractionGenerateMathOperationTen(): MathOperation {

        // Yüzler basamağı: 1,2,3,4,6,7,8,9
        val hundreds = listOf(1,2,3,4,6,7,8,9).random()
        // Onlar ve birler basamağı: 0-8 (9 hariç)
        val tens = (0..8).random()
        val ones = (0..8).random()

        val firstNumber = hundreds * 100 + tens * 10 + ones

        // Onlar basamağına göre ikinci sayının onlar basamağı
        val secondTens = when (tens) {
            0 -> listOf(1,2,3,4,5,6,7,8,9).random()
            1 -> listOf(2,3,4,5,7,8,9).random()
            2 -> listOf(3,4,5,8,9).random()
            3 -> listOf(4,5,9).random()
            4 -> listOf(5).random()
            5 -> listOf(6,7,8,9).random()
            6 -> listOf(7,8,9).random()
            7 -> listOf(8,9).random()
            8 -> listOf(9).random()
            else -> 1
        }

        // Birler basamağına göre ikinci sayının birler basamağı
        val secondOnes = when (ones) {
            0 -> listOf(1,2,3,4,5,6,7,8,9).random()
            1 -> listOf(2,3,4,5,7,8,9).random()
            2 -> listOf(3,4,5,8,9).random()
            3 -> listOf(4,5,9).random()
            4 -> listOf(5).random()
            5 -> listOf(6,7,8,9).random()
            6 -> listOf(7,8,9).random()
            7 -> listOf(8,9).random()
            8 -> listOf(9).random()
            else -> 1
        }

        val secondNumber = secondTens * 10 + secondOnes

        return MathOperation(firstNumber, "-", secondNumber)
    }
    fun extractionGenerateMathOperationTenWithOtherRules(): MathOperation {

        // Yüzler basamağı
        val hundreds = listOf(1,2,3,4,6,7,8,9).random()

        // Onlar ve birler basamağı
        val tens = listOf(0,1,2,3,4,5,6,7,8,9).random()
        val ones = (0..8).random()

        val firstNumber = hundreds * 100 + tens * 10 + ones

        // Onlar basamağına göre ikinci sayının onlar basamağı
        val secondTens = when (tens) {
            0 -> if ((0..100).random() < 80) 0 else listOf(1,2,3,4,5,6,7,8,9).random()
            1 -> if ((0..100).random() < 80) 1 else listOf(2,3,4,5,7,8,9).random()
            2 -> if ((0..100).random() < 80) 2 else listOf(3,4,5,8,9).random()
            3 -> if ((0..100).random() < 80) 3 else listOf(4,5,9).random()
            4 -> if ((0..100).random() < 80) 4 else listOf(5).random()
            5 -> if ((0..100).random() < 80) 5 else listOf(1,2,3,4,6,7,8,9).random()
            6 -> if ((0..100).random() < 80) listOf(6,1).random() else listOf(2,3,4,5,7,8,9).random()
            7 -> if ((0..100).random() < 80) listOf(7,2).random() else listOf(1,3,4,5,6,8,9).random()
            8 -> if ((0..100).random() < 80) listOf(8,3).random() else listOf(1,2,4,5,6,7,9).random()
            9 -> if ((0..100).random() < 80) listOf(9,4).random() else listOf(1,2,3,5,6,7,).random()
            else -> 1
        }

        // Birler basamağına göre ikinci sayının birler basamağı
        val secondOnes = when (ones) {
            0 -> listOf(1,2,3,4,5,6,7,8,9).random()
            1 -> listOf(2,3,4,5,7,8,9).random()
            2 -> listOf(3,4,5,8,9).random()
            3 -> listOf(4,5,9).random()
            4 -> listOf(5).random()
            5 -> listOf(6,7,8,9).random()
            6 -> listOf(7,8,9).random()
            7 -> listOf(8,9).random()
            8 -> listOf(9).random()
            else -> 1
        }

        val secondNumber = secondTens * 10 + secondOnes

        return MathOperation(firstNumber, "-", secondNumber)
    }
    fun extractionGenerateMathOperationTenWithOtherRulesExtreme(): MathOperation {

        // Yüzler basamağı: 1,2,3,4,6,7,8,9
        val hundreds = if ((0..1).random() == 0) {
            5
        } else {
            listOf(1,2,3,4,6,7,8,9).random()
        }
        // Onlar ve birler basamağı
        val tens = listOf(0,1,2,3,4,5,6,7,8,9).random()
        val ones = (0..8).random()

        val firstNumber = hundreds * 100 + tens * 10 + ones

        // Onlar basamağına göre ikinci sayının onlar basamağı
        val secondTens = when (tens) {
            0 -> if ((0..100).random() < 80) 0 else listOf(1,2,3,4,5,6,7,8,9).random()
            1 -> if ((0..100).random() < 80) 1 else listOf(2,3,4,5,7,8,9).random()
            2 -> if ((0..100).random() < 80) 2 else listOf(3,4,5,8,9).random()
            3 -> if ((0..100).random() < 80) 3 else listOf(4,5,9).random()
            4 -> if ((0..100).random() < 80) 4 else listOf(5).random()
            5 -> if ((0..100).random() < 80) 5 else listOf(1,2,3,4,6,7,8,9).random()
            6 -> if ((0..100).random() < 80) listOf(6,1).random() else listOf(2,3,4,5,7,8,9).random()
            7 -> if ((0..100).random() < 80) listOf(7,2).random() else listOf(1,3,4,5,6,8,9).random()
            8 -> if ((0..100).random() < 80) listOf(8,3).random() else listOf(1,2,4,5,6,7,9).random()
            9 -> if ((0..100).random() < 80) listOf(9,4).random() else listOf(1,2,3,5,6,7,).random()
            else -> 1
        }

        // Birler basamağına göre ikinci sayının birler basamağı
        val secondOnes = when (ones) {
            0 -> listOf(1,2,3,4,5,6,7,8,9).random()
            1 -> listOf(2,3,4,5,7,8,9).random()
            2 -> listOf(3,4,5,8,9).random()
            3 -> listOf(4,5,9).random()
            4 -> listOf(5).random()
            5 -> listOf(6,7,8,9).random()
            6 -> listOf(7,8,9).random()
            7 -> listOf(8,9).random()
            8 -> listOf(9).random()
            else -> 1
        }

        val secondNumber = secondTens * 10 + secondOnes

        return MathOperation(firstNumber, "-", secondNumber)
    }
    fun extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton(): MathOperation {

        val hundreds = listOf(1,2,3,4,5,6,7,8,9).random()

        // Onlar ve birler basamağı
        val tens = listOf(0,1,2,3,4,5,6,7,8,9).random()
        val ones = (0..8).random()

        val firstNumber = hundreds * 100 + tens * 10 + ones

        // Onlar basamağına göre ikinci sayının onlar basamağı
        val secondTens = when (tens) {
            0 -> listOf(0,1,2,3,4,5,6,7,8,9).random()
            1 -> listOf(1,2,3,4,5,7,8,9).random()
            2 -> listOf(2,3,4,5,8,9).random()
            3 -> listOf(3,4,5,9).random()
            4 -> listOf(4,5).random()
            5 -> listOf(1,2,3,4,5,6,7,8,9).random()
            6 -> listOf(2,3,4,6,7,8,9).random()
            7 -> listOf(3,4,7,8,9).random()
            8 -> listOf(4,8,9).random()
            9 -> listOf(4,9).random()
            else -> 1
        }

        // Birler basamağına göre ikinci sayının birler basamağı
        val secondOnes = when (ones) {
            0 -> listOf(1,2,3,4,5,6,7,8,9).random()
            1 -> listOf(2,3,4,5,7,8,9).random()
            2 -> listOf(3,4,5,8,9).random()
            3 -> listOf(4,5,9).random()
            4 -> listOf(5).random()
            5 -> listOf(6,7,8,9).random()
            6 -> listOf(7,8,9).random()
            7 -> listOf(8,9).random()
            8 -> listOf(9).random()
            else -> 1
        }

        val secondNumber = secondTens * 10 + secondOnes

        return MathOperation(firstNumber, "-", secondNumber)
    }
    fun extractionBeadRules(): MathOperation { //basit 10'luk toplama
        val tensDigit = listOf(1, 2, 3, 4, 6, 7, 8, 9).random()
        val onesDigit = (1..4).random()
        val firstNumber = tensDigit * 10 + onesDigit
        val secondNumber = when (onesDigit) {
            1 -> listOf(6).random()
            2 -> listOf(6, 7).random()
            3 -> listOf(6, 7, 8).random()
            4 -> listOf(6, 7, 8, 9).random()
            else -> 6
        }
        return MathOperation(firstNumber, "-", secondNumber)
    }

    private fun extractionBeadRulesWithSecondNumber(secondNumber: Int): MathOperation {
        val onesDigit = when (secondNumber) {
            6 -> (1..4).random()
            7 -> listOf(2, 3, 4).random()
            8 -> listOf(3, 4).random()
            9 -> 4
            else -> (1..4).random()
        }
        val tensDigit = listOf(1, 2, 3, 4, 6, 7, 8, 9).random()
        val firstNumber = tensDigit * 10 + onesDigit
        return MathOperation(firstNumber, "-", secondNumber)
    }

    /**
     * [extractionBeadRules] ile aynı kurallarda [count] adet işlem üretir.
     * secondNumber (6..9) mümkün olduğunca tekrar etmez; havuz bitince karışık yeniden dolar.
     */
    fun extractionBeadRulesList(count: Int): List<MathOperation> {
        if (count <= 0) return emptyList()

        val pendingSecondNumbers = mutableListOf<Int>()
        val operations = mutableListOf<MathOperation>()

        repeat(count) {
            if (pendingSecondNumbers.isEmpty()) {
                pendingSecondNumbers.addAll((6..9).shuffled())
            }
            val secondNumber = pendingSecondNumbers.removeAt(0)
            operations.add(extractionBeadRulesWithSecondNumber(secondNumber))
        }
        return operations
    }
    fun extractionBeadRulesThreeTwo(): MathOperation {

        // Yüzler basamağı: 1,2,3,4,6,7,8,9
        val hundreds = listOf(1,2,3,4,5,6,7,8,9).random()
        // Onlar ve birler basamağı: 0-8 (9 hariç)
        val tens = (1..4).random()
        val ones = (1..4).random()

        val firstNumber = hundreds * 100 + tens * 10 + ones

        // Onlar basamağına göre ikinci sayının onlar basamağı
        val secondTens = when (tens) {
            1 -> listOf(6).random()
            2 -> listOf(6,7).random()
            3 -> {
                if ((1..100).random() <= 50) {
                    8
                } else {
                    listOf(6,7).random()
                }
            }
            4 -> {
                if ((1..100).random() <= 50) {
                    9
                } else {
                    listOf(6,7,8).random()
                }
            }
            else -> 1
        }
        val secondOnes = when (ones) {
            1 -> listOf(6).random()
            2 -> listOf(6,7).random()
            3 -> {
                if ((1..100).random() <= 50) {
                    8
                } else {
                    listOf(6,7).random()
                }
            }
            4 -> {
                if ((1..100).random() <= 50) {
                    9
                } else {
                    listOf(6,7,8).random()
                }
            }
            else -> 1
        }


        val secondNumber = secondTens * 10 + secondOnes

        return MathOperation(firstNumber, "-", secondNumber)
    }
    fun extractionBeadRulesFourThree(): MathOperation {

        val thousands = listOf(1,2,3,4,5,6,7,8,9).random()
        // Yüzler basamağı: 1,2,3,4,6,7,8,9
        val hundreds = listOf(1,2,3,4).random()
        // Onlar ve birler basamağı: 0-8 (9 hariç)
        val tens = (1..4).random()
        val ones = (1..4).random()

        val firstNumber = thousands * 1000 + hundreds * 100 + tens * 10 + ones
        //yüzlere göre yüzler basamağı
        val secondhundreds = when (hundreds) {
            1 -> listOf(6).random()
            2 -> listOf(6,7).random()
            3 -> {
                if ((1..100).random() <= 50) {
                    8
                } else {
                    listOf(6,7).random()
                }
            }
            4 -> {
                if ((1..100).random() <= 50) {
                    9
                } else {
                    listOf(6,7,8).random()
                }
            }
            else -> 1
        }

        // Onlar basamağına göre ikinci sayının onlar basamağı
        val secondTens = when (tens) {
            1 -> listOf(6).random()
            2 -> listOf(6,7).random()
            3 -> {
                if ((1..100).random() <= 50) {
                    8
                } else {
                    listOf(6,7).random()
                }
            }
            4 -> {
                if ((1..100).random() <= 50) {
                    9
                } else {
                    listOf(6,7,8).random()
                }
            }
            else -> 1
        }

        // Birler basamağına göre ikinci sayının birler basamağı
        val secondOnes = when (ones) {
            1 -> listOf(6).random()
            2 -> listOf(6,7).random()
            3 -> {
                if ((1..100).random() <= 50) {
                    8
                } else {
                    listOf(6,7).random()
                }
            }
            4 -> {
                if ((1..100).random() <= 50) {
                    9
                } else {
                    listOf(6,7,8).random()
                }
            }
            else -> 1
        }

        val secondNumber = secondhundreds * 100 + secondTens * 10 + secondOnes

        return MathOperation(firstNumber, "-", secondNumber)
    }

    fun multiplicationLessFive(): MathOperation{
        val tens = (1..5).random()
        val ones = (0..5).random()
        val firstNumber = tens * 10 + ones

        val secondNumber = (2..5).random()

        return MathOperation(firstNumber, "x", secondNumber)

    }
    fun multiplicationLessFiveFull(): MathOperation{
        val tens = (5..9).random()
        val ones = (5..9).random()
        val firstNumber = tens * 10 + ones

        val secondNumber = (5..9).random()

        return MathOperation(firstNumber, "x", secondNumber)

    }
    fun multiplicationFull(): MathOperation{
        val tens = (1..9).random()
        val ones = (0..9).random()
        val firstNumber = tens * 10 + ones

        val secondNumber = (2..9).random()

        return MathOperation(firstNumber, "x", secondNumber)

    }
    fun multiplicationTwo(): MathOperation{
        val tens = (1..5).random()
        val ones = (0..5).random()
        val firstNumber = tens * 10 + ones

        val secondTens = (2..5).random()
        val secondOnes = (0..5).random()
        val secondNumber = secondTens * 10 + secondOnes

        return MathOperation(firstNumber, "x", secondNumber)

    }
    fun multiplicationTwoFull(): MathOperation{
        val tens = (1..9).random()
        val ones = (0..9).random()
        val firstNumber = tens * 10 + ones

        val secondTens = (1..9).random()
        val secondOnes = (0..9).random()
        val secondNumber = secondTens * 10 + secondOnes

        return MathOperation(firstNumber, "x", secondNumber)

    }
    fun multiplicationThreeFull(): MathOperation{
        val hundreds = (1..9).random()
        val tens = (0..9).random()
        val ones = (0..9).random()
        val firstNumber = 100 * hundreds + tens * 10 + ones

        val secondOnes = (2..9).random()

        return MathOperation(firstNumber, "x", secondOnes)

    }
    fun multiplicationThreeTwoFive(): MathOperation{
        val hundreds = (1..5).random()
        val tens = (0..5).random()
        val ones = (0..5).random()
        val firstNumber = 100 * hundreds + tens * 10 + ones

        val secondTens = (1..5).random()
        val secondOnes = (0..5).random()

        val secondNumber = 10 * secondTens + secondOnes

        return MathOperation(firstNumber, "x", secondNumber)

    }
    fun multiplicationThreeTwoFull(): MathOperation{
        val hundreds = (1..9).random()
        val tens = (0..9).random()
        val ones = (0..9).random()
        val firstNumber = 100 * hundreds + tens * 10 + ones

        val secondTens = (1..9).random()
        val secondOnes = (0..9).random()

        val secondNumber = 10 * secondTens + secondOnes

        return MathOperation(firstNumber, "x", secondNumber)

    }

    fun generateSequence1(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        // İlk sayıyı oluştur (iki basamaklı olabilir)
        // Onlar basamağı: 0,1,2 değerlerini alabilir
        // Birler basamağı: 1,2 değerlerini alabilir
        val firstTensDigit = (0..2).random()
        val firstOnesDigit = (1..2).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {
            // Önceki sayıların toplamını hesapla
            val totalSum = numbers.sum()

            // Toplam değerin basamaklarını al
            val totalStr = totalSum.toString()
            val tensDigit = if (totalStr.length > 1) totalStr[totalStr.length - 2].toString().toInt() else 0
            val onesDigit = totalStr[totalStr.length - 1].toString().toInt()

            // Onlar basamağı için olası değerleri belirle
            val possibleTensDigits = when (tensDigit) {
                0 -> listOf(1, 2, 5)
                1 -> listOf(1, 2, 5)
                2 -> listOf(1, 5)
                3 -> listOf(1, 5)
                4 -> listOf(5)
                5 -> listOf(1, 2)
                6 -> listOf(1, 2)
                7 -> listOf(1)
                8 -> listOf(1)
                9 -> listOf(0)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Birler basamağı için olası değerleri belirle
            val possibleOnesDigits = when (onesDigit) {
                0 -> listOf(1, 2, 5)
                1 -> listOf(1, 2, 5)
                2 -> listOf(1, 5)
                3 -> listOf(1, 5)
                4 -> listOf(5)
                5 -> listOf(1, 2)
                6 -> listOf(1, 2)
                7 -> listOf(1)
                8 -> listOf(1)
                9 -> listOf(0)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Yeni sayıyı oluştur
            val newTensDigit = possibleTensDigits.random()
            val newOnesDigit = possibleOnesDigits.random()
            val newNumber = newTensDigit * 10 + newOnesDigit

            numbers.add(newNumber)
        }

        return numbers
    }
    fun generateSequence1Digits(count: Int, digitsOne: Int): List<Int> {
        if (count <= 0) return emptyList()
        val digitCount = digitsOne.coerceAtLeast(1)

        fun allowedDigitsBySumDigit(sumDigit: Int): List<Int> = when (sumDigit) {
            0, 1 -> listOf(1, 2, 5)
            2, 3 -> listOf(1, 5)
            4 -> listOf(5)
            5, 6 -> listOf(1, 2)
            7, 8 -> listOf(1)
            9 -> listOf(0)
            else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
        }

        fun Int.pow10(exp: Int): Int {
            var out = 1
            repeat(exp) { out *= 10 }
            return out
        }

        fun toDigits(value: Int, size: Int): List<Int> {
            var x = value
            val arr = IntArray(size)
            for (i in size - 1 downTo 0) {
                arr[i] = x % 10
                x /= 10
            }
            return arr.toList()
        }

        // İlk sayı: en yüksek basamak 1..2, diğer basamaklar 0..2
        val firstDigits = MutableList(digitCount) { 0 }
        firstDigits[0] = (1..2).random()
        for (i in 1 until digitCount) {
            firstDigits[i] = (0..2).random()
        }
        val firstNumber = firstDigits.fold(0) { acc, d -> acc * 10 + d }

        val numbers = mutableListOf(firstNumber)

        // Kalan sayılar
        for (i in 1 until count) {
            val totalSum = numbers.sum()
            val sumDigits = toDigits(totalSum, digitCount)

            val newDigits = MutableList(digitCount) { 0 }
            for (pos in 0 until digitCount) {
                val allowed = allowedDigitsBySumDigit(sumDigits[pos])
                newDigits[pos] = allowed.random()
            }

            // Baş basamak 0 olmasın: sadece gerekli durumda güvenli düzelt
            if (newDigits[0] == 0 && digitCount > 1) {
                val nonZeroAllowed = allowedDigitsBySumDigit(sumDigits[0]).filter { it != 0 }
                if (nonZeroAllowed.isNotEmpty()) {
                    newDigits[0] = nonZeroAllowed.random()
                }
            }

            val newNumber = newDigits.fold(0) { acc, d -> acc * 10 + d }
            numbers.add(newNumber)
        }

        return numbers
    }
    fun generateSequence5Rules(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        // İlk sayıyı oluştur (iki basamaklı olabilir)
        // Onlar basamağı: 0,1,2 değerlerini alabilir
        // Birler basamağı: 1,2 değerlerini alabilir
        val firstTensDigit = (1..3).random()
        val firstOnesDigit = (1..3).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {
            // Önceki sayıların toplamını hesapla
            val totalSum = numbers.sum()

            // Toplam değerin basamaklarını al
            val totalStr = totalSum.toString()

            // Onlar basamağı için olası değerleri belirle
            val possibleTensDigits = (1..3).random()

            // Birler basamağı için olası değerleri belirle
            val possibleOnesDigits = (1..3).random()

            val newNumber = possibleTensDigits * 10 + possibleOnesDigits

            numbers.add(newNumber)
        }

        return numbers
    }
    fun generateSequence10RulesEasy(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        // İlk sayıyı oluştur (iki basamaklı olabilir)
        // Onlar basamağı: 0,1,2 değerlerini alabilir
        // Birler basamağı: 1,2 değerlerini alabilir
        val firstTensDigit = (1..3).random()
        val firstOnesDigit = (5..8).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {
            // Önceki sayıların toplamını hesapla
            val totalSum = numbers.sum()

            // Toplam değerin basamaklarını al
            val totalStr = totalSum.toString()

            // Onlar basamağı için olası değerleri belirle
            val possibleTensDigits = (1..2).random()

            // Birler basamağı için olası değerleri belirle
            val possibleOnesDigits = (1..5).random()

            val newNumber = possibleTensDigits * 10 + possibleOnesDigits

            numbers.add(newNumber)
        }

        return numbers
    }
    fun generateSequence10RulesEasyOld(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        // İlk sayıyı oluştur (iki basamaklı olabilir)
        // Onlar basamağı: 0,1,2 değerlerini alabilir
        // Birler basamağı: 1,2 değerlerini alabilir
        val firstTensDigit = (1..3).random()
        val firstOnesDigit = (1..4).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {
            // Önceki sayıların toplamını hesapla
            val totalSum = numbers.sum()

            // Toplam değerin basamaklarını al
            val totalStr = totalSum.toString()
            val tensDigit = if (totalStr.length > 1) totalStr[totalStr.length - 2].toString().toInt() else 0
            val onesDigit = totalStr[totalStr.length - 1].toString().toInt()

            // Onlar basamağı için olası değerleri belirle
            val possibleTensDigits = when (tensDigit) {
                0 -> listOf(1, 2, 5)
                1 -> listOf(1, 2, 5)
                2 -> listOf(1, 5)
                3 -> listOf(1, 5)
                4 -> listOf(5)
                5 -> listOf(1, 2)
                6 -> listOf(1, 2)
                7 -> listOf(1)
                8 -> listOf(1)
                9 -> listOf(0)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Birler basamağı için olası değerleri belirle
            val possibleOnesDigits = when (onesDigit) {
                1 -> listOf(9)
                2 -> listOf(9,8)
                3 -> listOf(9,8,7)
                4 -> listOf(9,8,7,6)
                5 -> listOf(1, 2,3,4)
                6 -> listOf(9)
                7 -> listOf(9,8)
                8 -> listOf(9,8,7)
                9 -> listOf(9,8,7,6)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Yeni sayıyı oluştur
            val newTensDigit = possibleTensDigits.random()
            val newOnesDigit = possibleOnesDigits.random()
            val newNumber = newTensDigit * 10 + newOnesDigit

            numbers.add(newNumber)
        }

        return numbers
    }
    fun generateSequenceBeadRules(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        // İlk sayıyı oluştur (iki basamaklı olabilir)
        // Onlar basamağı: 0,1,2 değerlerini alabilir
        // Birler basamağı: 1,2 değerlerini alabilir
        val firstTensDigit = (1..2).random()
        val firstOnesDigit = (1..4).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {
            // Önceki sayıların toplamını hesapla
            val totalSum = numbers.sum()

            // Toplam değerin basamaklarını al
            val totalStr = totalSum.toString()
            val tensDigit = if (totalStr.length > 1) totalStr[totalStr.length - 2].toString().toInt() else 0
            val onesDigit = totalStr[totalStr.length - 1].toString().toInt()

            // Onlar basamağı için olası değerleri belirle
            val possibleTensDigits = when (tensDigit) {
                0 -> listOf(1, 2, 5)
                1 -> listOf(1, 2, 5)
                2 -> listOf(1, 5)
                3 -> listOf(1, 5)
                4 -> listOf(5)
                5 -> listOf(1, 2)
                6 -> listOf(1, 2)
                7 -> listOf(1)
                8 -> listOf(1)
                9 -> listOf(0)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Birler basamağı için olası değerleri belirle
            val possibleOnesDigits = when (onesDigit) {
                1 -> listOf(4,5,6,7)
                2 -> listOf(3,4,5,6)
                3 -> listOf(2,3,4,5)
                4 -> listOf(1,2,3,4)
                5 -> listOf(6)
                6 -> listOf(6,7,8)
                7 -> listOf(6,7)
                8 -> listOf(6)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Yeni sayıyı oluştur
            val newTensDigit = possibleTensDigits.random()
            val newOnesDigit = possibleOnesDigits.random()
            val newNumber = newTensDigit * 10 + newOnesDigit

            numbers.add(newNumber)
        }

        return numbers
    }
    fun generateSequenceExtraction(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        // İlk sayıyı oluştur (iki basamaklı olabilir)
        // Onlar basamağı: 0,1,2 değerlerini alabilir
        // Birler basamağı: 1,2 değerlerini alabilir
        val firstTensDigit = (8..9).random()
        val firstOnesDigit = (8..9).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {
            // Önceki sayıların toplamını hesapla
            val totalSum = numbers.sum()

            // Toplam değerin basamaklarını al
            val totalStr = totalSum.toString()
            val tensDigit = if (totalStr.length > 1) totalStr[totalStr.length - 2].toString().toInt() else 0
            val onesDigit = totalStr[totalStr.length - 1].toString().toInt()

            // Onlar basamağı için olası değerleri belirle
            val possibleTensDigits = when (tensDigit) {
                0 -> listOf(0)
                1 -> listOf(1)
                2 -> listOf(1)
                3 -> listOf(1,2)
                4 -> listOf(1,2,3)
                5 -> listOf(5)
                6 -> listOf(1,5)
                7 -> listOf(1,5)
                8 -> listOf(1,2,5)
                9 -> listOf(1,2,3,5)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Birler basamağı için olası değerleri belirle
            val possibleOnesDigits = when (onesDigit) {
                0 -> listOf(0)
                1 -> listOf(1)
                2 -> listOf(1)
                3 -> listOf(1,2)
                4 -> listOf(1,2,3)
                5 -> listOf(5)
                6 -> listOf(1,5)
                7 -> listOf(1,2,5)
                8 -> listOf(1,2,3,5)
                9 -> listOf(1,2,3,5)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Yeni sayıyı oluştur
            val newTensDigit = -possibleTensDigits.random()
            val newOnesDigit = -possibleOnesDigits.random()
            val newNumber = newTensDigit * 10 + newOnesDigit

            numbers.add(newNumber)
        }

        return numbers
    }
    fun generateSequenceExtractionFiveRules(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        // İlk sayıyı oluştur (iki basamaklı olabilir)
        // Onlar basamağı: 0,1,2 değerlerini alabilir
        // Birler basamağı: 1,2 değerlerini alabilir
        val firstTensDigit = (8..9).random()
        val firstOnesDigit = (8..9).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {
            // Önceki sayıların toplamını hesapla
            val totalSum = numbers.sum()

            // Toplam değerin basamaklarını al
            val totalStr = totalSum.toString()
            val tensDigit = if (totalStr.length > 1) totalStr[totalStr.length - 2].toString().toInt() else 0
            val onesDigit = totalStr[totalStr.length - 1].toString().toInt()

            // Onlar basamağı için olası değerleri belirle
            val possibleTensDigits = when (tensDigit) {
                0 -> listOf(0)
                1 -> listOf(1)
                2 -> listOf(1)
                3 -> listOf(1,2)
                4 -> listOf(1,2,3)
                5 -> listOf(4,3,2,1)
                6 -> listOf(4,3,2)
                7 -> listOf(4,3)
                8 -> listOf(4)
                9 -> listOf(1,2)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Birler basamağı için olası değerleri belirle
            val possibleOnesDigits = when (onesDigit) {
                0 -> listOf(0)
                1 -> listOf(1)
                2 -> listOf(1)
                3 -> listOf(1,2)
                4 -> listOf(1,2,3)
                5 -> listOf(4,3,2,1)
                6 -> listOf(4,3,2)
                7 -> listOf(4,3)
                8 -> listOf(4)
                9 -> listOf(1,2)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Yeni sayıyı oluştur
            val newTensDigit = -possibleTensDigits.random()
            val newOnesDigit = -possibleOnesDigits.random()
            val newNumber = newTensDigit * 10 + newOnesDigit

            numbers.add(newNumber)
        }

        return numbers
    }
    fun generateSequenceExtractionTenRules(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        // İlk sayıyı oluştur (iki basamaklı olabilir)
        // Onlar basamağı: 0,1,2 değerlerini alabilir
        // Birler basamağı: 1,2 değerlerini alabilir
        val firstTensDigit = (8..9).random()
        val firstOnesDigit = (5..8).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {
            // Önceki sayıların toplamını hesapla
            val totalSum = numbers.sum()

            // Toplam değerin basamaklarını al
            val totalStr = totalSum.toString()
            val tensDigit = if (totalStr.length > 1) totalStr[totalStr.length - 2].toString().toInt() else 0
            val onesDigit = totalStr[totalStr.length - 1].toString().toInt()

            // Onlar basamağı için olası değerleri belirle
            val possibleTensDigits = when (tensDigit) {
                1 -> listOf(1)
                2 -> listOf(1)
                3 -> listOf(1,2)
                4 -> listOf(1,2,3)
                5 -> listOf(4,3,2,1)
                6 -> listOf(4,3,2,1,5)
                7 -> listOf(4,3,5,2,1)
                8 -> listOf(4,3,5,2,1)
                9 -> listOf(1,2,5,6)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Birler basamağı için olası değerleri belirle
            val possibleOnesDigits = when (onesDigit) {
                1 -> listOf(2,3,4,5,7,8,9)
                2 -> listOf(3,4,5,8,9)
                3 -> listOf(4,5,9)
                4 -> listOf(5)
                5 -> listOf(9,8,7,6)
                6 -> listOf(9,8,7)
                7 -> listOf(9,8)
                8 -> listOf(9)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Yeni sayıyı oluştur
            val newTensDigit = -possibleTensDigits.random()
            val newOnesDigit = -possibleOnesDigits.random()
            val newNumber = newTensDigit * 10 + newOnesDigit

            numbers.add(newNumber)
        }

        return numbers
    }
    fun generateSequenceExtractionBeadRules(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        // İlk sayıyı oluştur (iki basamaklı olabilir)
        // Onlar basamağı: 0,1,2 değerlerini alabilir
        // Birler basamağı: 1,2 değerlerini alabilir
        val firstTensDigit = (8..9).random()
        val firstOnesDigit = (1..4).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit
        var control = false
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {
            // Önceki sayıların toplamını hesapla
            val totalSum = numbers.sum()

            // Toplam değerin basamaklarını al
            val totalStr = totalSum.toString()
            val tensDigit = if (totalStr.length > 1) totalStr[totalStr.length - 2].toString().toInt() else 0
            val onesDigit = totalStr[totalStr.length - 1].toString().toInt()

            // Onlar basamağı için olası değerleri belirle
            val possibleTensDigits = when (tensDigit) {
                1 -> listOf(1)
                2 -> listOf(1)
                3 -> listOf(1,2)
                4 -> listOf(1,2)
                5 -> listOf(1,2,3,4)
                6 -> listOf(1,5)
                7 -> listOf(1)
                8 -> listOf(1,2)
                9 -> listOf(1,2)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Birler basamağı için olası değerleri belirle
            val possibleOnesDigits = when (onesDigit) {
                1 -> listOf(6)
                2 -> listOf(6,7)
                3 -> listOf(6,7,8)
                4 -> listOf(6,7,8,9)
                5 -> listOf(1,2,3,4)
                6 -> listOf(2,3,4)
                7 -> listOf(3,4)
                8 -> listOf(4)
                9 -> listOf(5)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            // Yeni sayıyı oluştur

            val newTensDigit = if (control) {
                control = false
                -possibleTensDigits.random()
            } else {
                control = true
                0
            }
            val newOnesDigit = -possibleOnesDigits.random()
            val newNumber = newTensDigit * 10 + newOnesDigit

            numbers.add(newNumber)
        }

        return numbers
    }
    fun generalCollectionOneDigits(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        // İlk sayıyı oluştur (iki basamaklı olabilir)
        // Onlar basamağı: 0,1,2 değerlerini alabilir
        // Birler basamağı: 1,2 değerlerini alabilir
        val firstOnesDigit = (1..9).random()
        numbers.add(firstOnesDigit)

        // Kalan sayıları oluştur
        for (i in 1 until count) {

            // Birler basamağı için olası değerleri belirle
            val possibleOnesDigits = (1..9).toList()


            // Yeni sayıyı oluştur
            val newOnesDigit = possibleOnesDigits.random()

            numbers.add(newOnesDigit)
        }

        return numbers
    }
    fun generalCollectionTwoDigits(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        Log.d("imkansızım","selam")
        // İlk sayıyı oluştur (iki basamaklı olabilir)
        // Onlar basamağı: 0,1,2 değerlerini alabilir
        // Birler basamağı: 1,2 değerlerini alabilir
        val firstTensDigit = (1..9).random()
        val firstOnesDigit = (0..9).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {

            val possibleTensDigits = (1..9).toList()


            // Birler basamağı için olası değerleri belirle
            val possibleOnesDigits = (1..9).toList()


            // Yeni sayıyı oluştur
            val newTensDigit = possibleTensDigits.random()
            val newOnesDigit = possibleOnesDigits.random()
            val newNumber = newTensDigit * 10 + newOnesDigit

            numbers.add(newNumber)
        }

        return numbers
    }
    fun generalCollectionThreeDigits(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        Log.d("imkansızım","selam")
        // İlk sayıyı oluştur (iki basamaklı olabilir)
        // Onlar basamağı: 0,1,2 değerlerini alabilir
        // Birler basamağı: 1,2 değerlerini alabilir
        val firstTensDigit = (0..9).random()
        val firstOnesDigit = (0..9).random()
        val firstHundredsDigit = (1..9).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit + firstHundredsDigit * 100
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {
            val possibleHundredsDigits = (1..9).toList()
            val possibleTensDigits = (0..9).toList()
            val possibleOnesDigits = (0..9).toList()


            // Yeni sayıyı oluştur
            val newHundredsDigit = possibleHundredsDigits.random()
            val newTensDigit = possibleTensDigits.random()
            val newOnesDigit = possibleOnesDigits.random()
            val newNumber = newTensDigit * 10 + newOnesDigit + newHundredsDigit * 100

            numbers.add(newNumber)
        }

        return numbers
    }
    fun generalCollectionFourDigits(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        Log.d("imkansızım","selam")
        // İlk sayıyı oluştur (iki basamaklı olabilir)
        // Onlar basamağı: 0,1,2 değerlerini alabilir
        // Birler basamağı: 1,2 değerlerini alabilir
        val firstTensDigit = (0..9).random()
        val firstOnesDigit = (0..9).random()
        val firstThousandsDigit = (1..9).random()
        val firstHundredsDigit = (0..9).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit + firstHundredsDigit * 100 + firstThousandsDigit * 1000
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {
            val possibleHundredsDigits = (0..9).toList()
            val possibleTensDigits = (0..9).toList()
            val possibleOnesDigits = (0..9).toList()
            val possibleThousandsDigits = (1..9).toList()


            // Yeni sayıyı oluştur
            val newHundredsDigit = possibleHundredsDigits.random()
            val newTensDigit = possibleTensDigits.random()
            val newOnesDigit = possibleOnesDigits.random()
            val newThousandsDigit = possibleThousandsDigits.random()
            val newNumber = newTensDigit * 10 + newOnesDigit + newHundredsDigit * 100 + newThousandsDigit * 1000

            numbers.add(newNumber)
        }

        return numbers
    }

    fun generateSequenceExtractionRace(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        val firstTensDigit = (5..9).random()
        val firstOnesDigit = (0..9).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {


            val possibleOnesDigits = (1..9).toList()

            // Yeni sayıyı oluştur
            val newOnesDigit = -possibleOnesDigits.random()

            numbers.add(newOnesDigit)
        }

        return numbers
    }
    fun generateSequenceExtractionRaceHard(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        val firstHundredDigit = (5..9).random()
        val firstTensDigit = (0..9).random()
        val firstOnesDigit = (0..9).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit + firstHundredDigit * 100
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {


            val possibleOnesDigits = (1..9).toList()

            // Yeni sayıyı oluştur
            val newOnesDigit = -possibleOnesDigits.random()

            numbers.add(newOnesDigit)
        }

        return numbers
    }
    fun generateSequenceExtractionRaceTwoDigits(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        val firstHundredsDigit = (5..9).random()
        val firstTensDigit = (0..9).random()
        val firstOnesDigit = (0..9).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit + firstHundredsDigit * 100
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {


            val possibleOnesDigits = (1..9).toList()
            val possibleTensDigits = (1..9).toList()


            // Yeni sayıyı oluştur
            val newOnesDigit = -possibleOnesDigits.random() -possibleTensDigits.random() * 10

            numbers.add(newOnesDigit)
        }

        return numbers
    }
    fun generateSequenceExtractionRaceTwoDigitsHard(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        val firstThousandDigit = (5..9).random()
        val firstHundredsDigit = (0..9).random()
        val firstTensDigit = (0..9).random()
        val firstOnesDigit = (0..9).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit + firstHundredsDigit * 100 + firstThousandDigit * 1000
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {


            val possibleOnesDigits = (1..9).toList()
            val possibleTensDigits = (1..9).toList()


            // Yeni sayıyı oluştur
            val newOnesDigit = -possibleOnesDigits.random() -possibleTensDigits.random() * 10

            numbers.add(newOnesDigit)
        }

        return numbers
    }
    fun generateSequenceExtractionRaceThreeDigits(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        val firstThousandsDigit = (5..9).random()
        val firstHundredsDigit = (0..9).random()
        val firstTensDigit = (0..9).random()
        val firstOnesDigit = (0..9).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit + firstHundredsDigit * 100 + firstThousandsDigit * 1000
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {


            val possibleHundredsDigits = (1..9).toList()
            val possibleOnesDigits = (1..9).toList()
            val possibleTensDigits = (1..9).toList()


            // Yeni sayıyı oluştur
            val newOnesDigit = -possibleOnesDigits.random() -possibleTensDigits.random() * 10 -possibleHundredsDigits.random() * 100

            numbers.add(newOnesDigit)
        }

        return numbers
    }
    fun generateSequenceExtractionRaceThreeDigitsHard(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        val firstTenThousandsDigit = (5..9).random()
        val firstThousandsDigit = (0..9).random()
        val firstHundredsDigit = (0..9).random()
        val firstTensDigit = (0..9).random()
        val firstOnesDigit = (0..9).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit + firstHundredsDigit * 100 + firstThousandsDigit * 1000 + firstTenThousandsDigit * 10000
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {


            val possibleHundredsDigits = (1..9).toList()
            val possibleOnesDigits = (1..9).toList()
            val possibleTensDigits = (1..9).toList()


            // Yeni sayıyı oluştur
            val newOnesDigit = -possibleOnesDigits.random() -possibleTensDigits.random() * 10 -possibleHundredsDigits.random() * 100

            numbers.add(newOnesDigit)
        }

        return numbers
    }
    fun generateSequenceExtractionRaceFourDigits(count: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        val firstTenThousandsDigit = (5..9).random()
        val firstThousandsDigit = (0..9).random()
        val firstHundredsDigit = (0..9).random()
        val firstTensDigit = (0..9).random()
        val firstOnesDigit = (0..9).random()
        val firstNumber = firstTensDigit * 10 + firstOnesDigit + firstHundredsDigit * 100 + firstThousandsDigit * 1000 + firstTenThousandsDigit * 10000
        numbers.add(firstNumber)

        // Kalan sayıları oluştur
        for (i in 1 until count) {


            val possibleHundredsDigits = (1..9).toList()
            val possibleThousandsDigits = (1..9).toList()
            val possibleOnesDigits = (1..9).toList()
            val possibleTensDigits = (1..9).toList()


            // Yeni sayıyı oluştur
            val newOnesDigit = -possibleOnesDigits.random() -possibleTensDigits.random() * 10 -possibleHundredsDigits.random() * 100 -possibleThousandsDigits.random() * 1000

            numbers.add(newOnesDigit)
        }

        return numbers
    }


}
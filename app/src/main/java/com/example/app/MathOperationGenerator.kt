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
        // İlk sayıyı oluştur
        var firstNumber: Int
        do {
            // İlk basamak için (0 olamaz)
            val firstDigit = (1..9).random()

            // Diğer basamaklar için
            val otherDigits = if (firstDigitCount > 1) {
                // Onlar basamağı için (9 olamaz)
                val tensDigit = if (firstDigitCount > 1) {
                    (0..8).random() // 9 hariç
                } else {
                    0
                }

                // Diğer basamaklar için
                val remainingDigits = if (firstDigitCount > 2) {
                    (0 until firstDigitCount - 2).map { (0..9).random() }
                        .fold(0) { acc, digit -> acc * 10 + digit }
                } else {
                    0
                }

                remainingDigits * 10 + tensDigit
            } else {
                0
            }

            firstNumber =
                firstDigit * Math.pow(10.0, (firstDigitCount - 1).toDouble()).toInt() + otherDigits
        } while (firstDigitCount > 1 && (firstNumber / 10) % 10 == 9) // Onlar basamağı 9 olmamalı

        // İkinci sayıyı oluştur
        val firstNumberStr = firstNumber.toString()
        val secondNumberDigits = mutableListOf<Int>()

        // İkinci sayının her basamağı için
        for (i in 0 until secondDigitCount) {
            if (i < firstNumberStr.length) {
                // İlk sayının aynı basamağındaki rakama göre kural uygula
                val firstDigit = firstNumberStr[firstNumberStr.length - 1 - i].toString().toInt()
                var allowedDigits = digitRules[firstDigit] ?: listOf(0)

                // İkinci sayının onlar basamağı 0 olamaz
                if (i == 1) { // Onlar basamağı için
                    allowedDigits = allowedDigits.filter { it != 0 }
                }

                // Eğer liste boşsa varsayılan olarak 1 kullan
                val selectedDigit = if (allowedDigits.isEmpty()) 1 else allowedDigits.random()
                secondNumberDigits.add(0, selectedDigit)
            } else {
                // Eğer ikinci sayı daha fazla basamağa sahipse, ekstra basamaklar için rastgele rakam
                // Onlar basamağı için 0 olamaz
                val digit = if (i == 1) (1..9).random() else (0..9).random()
                secondNumberDigits.add(0, digit)
            }
        }

        // Basamakları birleştir
        val secondNumber = secondNumberDigits.fold(0) { acc, digit -> acc * 10 + digit }

        return MathOperation(firstNumber, "+", secondNumber)
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
            1 to listOf(1, 2, 3, 4, 5, 6, 7, 8), // %50 ihtimalle 4
            2 to listOf(1, 2, 3, 4, 5, 6, 7), // %50 ihtimalle 3 veya 4
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
        // İlk sayının onlar basamağı (0, 4 ve 9 hariç)
        val tensDigit = listOf(1, 2, 3, 5, 6, 7, 8).random()

        val random = Random()

        // İlk sayının birler basamağı (5,6,7,8,9)
        val onesDigit = (5..9).random()

        // İlk sayıyı oluştur
        val firstNumber = tensDigit * 10 + onesDigit

        // İkinci sayıyı belirle (ilk sayının birler basamağına göre)
        val secondNumber = when (onesDigit) {
            5 -> 5
            6 -> (4..5).random()
            7 -> if (random.nextBoolean()) 3 else (4..5).random()
            8 -> if (random.nextBoolean()) 2 else (3..5).random()
            9 -> if (random.nextBoolean()) 1 else (2..5).random()
            else -> 0 // Bu durum asla oluşmayacak ama Kotlin için gerekli
        }

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
            else -> listOf() // Bu durum asla oluşmayacak ama Kotlin'in when ifadesi için gerekli
        }

        // İkinci sayıyı olası değerler arasından rastgele seç
        val secondNumber = possibleSecondNumbers.random()

        return MathOperation(firstNumber, "+", secondNumber)
    }
    fun generateMathOperationWithDigits(firstDigitCount: Int, secondDigitCount: Int): MathOperation {
        // İlk sayıyı oluştur
        var firstNumber = 0
        repeat(firstDigitCount) {
            val digit = (1..9).random()
            firstNumber = firstNumber * 10 + digit
        }

        // İkinci sayıyı oluştur
        var secondNumber = 0
        var tempFirst = firstNumber

        repeat(secondDigitCount) {
            val digit = tempFirst % 10
            val secondDigit = when (digit) {
                1 -> (1..8).random()
                2 -> (1..7).random()
                3 -> (1..6).random()
                4 -> (1..5).random()
                5 -> 5
                6 -> listOf(5, 4).random()
                7 -> listOf(5, 4, 3).random()
                8 -> listOf(5, 4, 3, 2).random()
                9 -> listOf(5, 4, 3, 2, 1).random()
                else -> 0
            }
            secondNumber = secondDigit * Math.pow(10.0, it.toDouble()).toInt() + secondNumber
            tempFirst /= 10
        }

        return MathOperation(firstNumber, "+", secondNumber)
    }
    fun generateMathOperation3(): MathOperation {
        // İki basamaklı random sayı üret (10-99 arası)
        // Birler basamağı 0 olmayan sayıları seç
        val firstNumber = (10..99).filter { it % 10 != 0 }.random()

        // Birler basamağını al
        val onesDigit = firstNumber % 10

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
            val currentDigit = firstNumberStr[i].toString().toInt()
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
                6 -> listOf(6, 7, 8)
                7 -> listOf(6, 7)
                8 -> listOf(6)
                else -> listOf(6, 7, 8, 9) // Bu duruma hiç girmeyecek ama yine de yazdım
            }

            val digit = possibleDigits.random()
            secondNumber = secondNumber * 10 + digit
        }

        return MathOperation(firstNumber, "+", secondNumber)
    }
    fun irregularExtraction(firstNumberDigits: Int, secondNumberDigits: Int): MathOperation {
        var firstNumber = 0
        repeat(firstNumberDigits) {
            val digit = (1..9).random()
            firstNumber = firstNumber * 10 + digit
        }

        // İkinci sayıyı oluştur
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
                6 -> listOf(6,5,1)
                7 -> listOf(7,6,5,2,1)
                8 -> listOf(8,7,6,5,3,2,1)
                9 -> listOf(9,8,7,6,5,4,3,2,1)
                else -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            val digit = possibleDigits.random()
            secondNumber = secondNumber * 10 + digit
        }
        return MathOperation(firstNumber, "-", secondNumber)
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
        var firstNumber = 0
        repeat(firstNumberDigits) {
            val digit = (1..9).random()
            firstNumber = firstNumber * 10 + digit
        }

        // İkinci sayıyı oluştur
        var secondNumber = 0
        val firstNumberStr = firstNumber.toString()

        for (i in 0 until secondNumberDigits) {
            val currentDigit = firstNumberStr[i].toString().toInt()
            val possibleDigits = when (currentDigit) {
                5 -> {
                    if ((0..1).random() == 0) listOf(4,3,2,1) else listOf(5)
                }
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
        return MathOperation(firstNumber, "-", secondNumber)
    }
    fun extractionGenerateMathOperation(): MathOperation { //basit 10'luk toplama
        // İlk sayının onlar basamağı (5 hariç)
        val tensDigit = listOf(1, 2, 3, 4, 6, 7, 8, 9).random()

        val random = Random()

        // İlk sayının birler basamağı (0..5,6,7,8,9)
        val onesDigit = (0..8).random()

        // İlk sayıyı oluştur
        val firstNumber = tensDigit * 10 + onesDigit

        // İkinci sayıyı belirle (ilk sayının birler basamağına göre)
        val secondNumber = when (onesDigit) {
            0 -> listOf(1,2,3,4,5,6,7,8,9).random()
            1 -> listOf(2,3,4,5,7,8,9).random()
            2 -> listOf(3,4,5,8,9).random()
            3 -> listOf(4,5,9).random()
            4 -> listOf(5).random()
            5 -> listOf(6,7,8,9).random()
            6 -> listOf(7,8,9).random()
            7 -> listOf(8,9).random()
            8 -> listOf(9).random()
            else -> 0 // Bu durum asla oluşmayacak ama Kotlin için gerekli
        }

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
        val tens = if ((0..1).random() == 0) {
            0
        } else {
            listOf(1,2,3,4,5,6,7,8,9).random()
        }
        val ones = (0..8).random()

        val firstNumber = hundreds * 100 + tens * 10 + ones

        // Onlar basamağına göre ikinci sayının onlar basamağı
        val secondTens = when (tens) {
            0 -> 0
            1 -> if ((0..1).random() == 0) 1 else listOf(2,3,4,5,7,8,9).random()
            2 -> if ((0..1).random() == 0) 2 else listOf(3,4,5,8,9).random()
            3 -> listOf(3,4,5,9).random()
            4 -> listOf(4,5).random()
            5 -> listOf(5,6,7,8,9).random()
            6 -> listOf(6,7,8,9).random()
            7 -> listOf(7,8,9).random()
            8 -> listOf(8,9).random()
            9 -> 9
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
        val tens = if ((0..1).random() == 0) {
            0
        } else {
            listOf(1,2,3,4,5,6,7,8,9).random()
        }
        val ones = (0..8).random()

        val firstNumber = hundreds * 100 + tens * 10 + ones

        // Onlar basamağına göre ikinci sayının onlar basamağı
        val secondTens = when (tens) {
            0 -> 0
            1 -> if ((0..1).random() == 0) 1 else listOf(2,3,4,5,7,8,9).random()
            2 -> if ((0..1).random() == 0) 2 else listOf(3,4,5,8,9).random()
            3 -> listOf(3,4,5,9).random()
            4 -> listOf(4,5).random()
            5 -> listOf(5,6,7,8,9).random()
            6 -> listOf(6,7,8,9).random()
            7 -> listOf(7,8,9).random()
            8 -> listOf(8,9).random()
            9 -> 9
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
        // İlk sayının onlar basamağı (5 hariç)
        val tensDigit = listOf(1, 2, 3, 4, 6, 7, 8, 9).random()


        // İlk sayının birler basamağı (0..5,6,7,8,9)
        val onesDigit = (1..4).random()

        // İlk sayıyı oluştur
        val firstNumber = tensDigit * 10 + onesDigit

        // İkinci sayıyı belirle (ilk sayının birler basamağına göre)
        val secondNumber = when (onesDigit) {
            1 -> listOf(6).random()
            2 -> listOf(6,7).random()
            3 -> listOf(6,7,8).random()
            4 -> listOf(6,7,8,9).random()
            else -> 0 // Bu durum asla oluşmayacak ama Kotlin için gerekli
        }

        return MathOperation(firstNumber, "-", secondNumber)
    }
    fun extractionBeadRulesThreeTwo(): MathOperation {

        // Yüzler basamağı: 1,2,3,4,6,7,8,9
        val hundreds = listOf(1,2,3,4,5,6,7,8,9).random()
        // Onlar ve birler basamağı: 0-8 (9 hariç)
        val tens = (1..4).random()
        val ones = (0..9).random()

        val firstNumber = hundreds * 100 + tens * 10 + ones

        // Onlar basamağına göre ikinci sayının onlar basamağı
        val secondTens = when (tens) {
            1 -> listOf(6).random()
            2 -> listOf(6,7).random()
            3 -> listOf(6,7,8).random()
            4 -> listOf(6,7,8,9).random()
            else -> 1
        }

        // Birler basamağına göre ikinci sayının birler basamağı
        val secondOnes = (0..9).random()

        val secondNumber = secondTens * 10 + secondOnes

        return MathOperation(firstNumber, "-", secondNumber)
    }
    fun extractionBeadRulesFourThree(): MathOperation {

        val thousands = listOf(1,2,3,4,5,6,7,8,9).random()
        // Yüzler basamağı: 1,2,3,4,6,7,8,9
        val hundreds = listOf(1,2,3,4).random()
        // Onlar ve birler basamağı: 0-8 (9 hariç)
        val tens = (1..4).random()
        val ones = (0..9).random()

        val firstNumber = thousands * 1000 + hundreds * 100 + tens * 10 + ones
        //yüzlere göre yüzler basamağı
        val secondhundreds = when (hundreds) {
            1 -> listOf(6).random()
            2 -> listOf(6,7).random()
            3 -> listOf(6,7,8).random()
            4 -> listOf(6,7,8,9).random()
            else -> 1
        }

        // Onlar basamağına göre ikinci sayının onlar basamağı
        val secondTens = when (tens) {
            1 -> listOf(6).random()
            2 -> listOf(6,7).random()
            3 -> listOf(6,7,8).random()
            4 -> listOf(6,7,8,9).random()
            else -> 1
        }

        // Birler basamağına göre ikinci sayının birler basamağı
        val secondOnes = (0..9).random()

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
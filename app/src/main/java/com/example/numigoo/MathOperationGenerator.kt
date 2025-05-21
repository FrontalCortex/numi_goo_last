package com.example.numigoo

import android.util.Log
import com.example.numigoo.GlobalValues.generateRandomNumber
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
        // İlk sayıyı oluştur
        val firstNumber = generateRandomNumber(firstDigitCount)

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
                if (firstDigit == 1 && Math.random() < 0.5) {
                    // %50 ihtimalle 4
                    secondNumberDigits.add(0, 4)
                } else if (firstDigit == 2 && Math.random() < 0.5) {
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
        // İlk sayının onlar basamağı için rastgele sayı (1-9 arası)
        val firstNumberTens = Random().nextInt(9) + 1

        // İlk sayının birler basamağı için rastgele sayı (1-4 arası)
        val firstNumberOnes = Random().nextInt(4) + 1

        // İlk sayıyı oluştur
        val firstNumber = (firstNumberTens * 10) + firstNumberOnes

        // İkinci sayının onlar basamağı için olası değerler
        val possibleSecondTens = when (firstNumberTens) {
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

        // İkinci sayının onlar basamağını seç
        val secondNumberTens = possibleSecondTens[Random().nextInt(possibleSecondTens.size)]

        // İkinci sayının birler basamağı için olası değerler
        val possibleSecondOnes = when (firstNumberOnes) {
            1 -> listOf(4)
            2 -> listOf(3, 4)
            3 -> listOf(2, 3, 4)
            4 -> listOf(1, 2, 3, 4)
            else -> listOf()
        }

        // İkinci sayının birler basamağını seç
        val secondNumberOnes = possibleSecondOnes[Random().nextInt(possibleSecondOnes.size)]

        // İkinci sayıyı oluştur
        val secondNumber = (secondNumberTens * 10) + secondNumberOnes

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

}
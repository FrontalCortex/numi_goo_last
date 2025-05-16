package com.example.numigoo

import kotlin.math.pow

object GlobalValues {
    var lessonStep: Int = 1
    var mapFragmentStepIndex: Int = 1
    var stepIndex=0
    var tutorialIsWorked = false
    var scrollPosition = 0  // Scroll pozisyonunu global olarak tutacak değişken
    fun randomNumberChangeToString(digitCount: Int): String{

        // Minimum ve maksimum değerleri hesapla
        val min = 10.0.pow(digitCount - 1).toInt()  // Örnek: 3 basamak için 100
        val max = 10.0.pow(digitCount).toInt() - 1  // Örnek: 3 basamak için 999

        // Random sayı üret
        return (min..max).random().toString()
    }
    fun generateRandomNumber(digitCount: Int): Int {
        // Basamak sayısı kontrolü
        if (digitCount < 1) return 0

        // Minimum ve maksimum değerleri hesapla
        val min = 10.0.pow(digitCount - 1).toInt()  // Örnek: 3 basamak için 100
        val max = 10.0.pow(digitCount).toInt() - 1  // Örnek: 3 basamak için 999

        // Random sayı üret
        return (min..max).random()
    }
} 
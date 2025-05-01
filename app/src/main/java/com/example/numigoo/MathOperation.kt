package com.example.numigoo

import java.io.Serializable

data class MathOperation(
    val firstNumber: Int? = null,
    val operator: String? = null,
    val secondNumber: Int? = null
): Serializable
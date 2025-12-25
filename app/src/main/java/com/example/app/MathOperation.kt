package com.example.app

import java.io.Serializable

data class MathOperation(
    val firstNumber: Int? = null,
    val operator: String? = null,
    val secondNumber: Int? = null
): Serializable
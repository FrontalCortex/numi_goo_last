// Kaynak: app/src/main/java/com/example/app/MathOperation.kt (Android).
// Fark: `java.io.Serializable` kaldırıldı (commonMain'de yok; iOS'ta gerekmiyor).
package com.sorobit.shared.matematik

data class MathOperation(
    val firstNumber: Int? = null,
    val operator: String? = null,
    val secondNumber: Int? = null
)

package com.example.numigoo.model

data class TutorialStep(
    val text: String,
    val animation: Any? // BeadAnimation ya da List<BeadAnimation>
)

data class BeadAnimation(
    val rod: Int,  // 0-4 arası sütun numarası
    val count: Int // 1-5 arası boncuk sayısı
) 
package com.example.numigoo.model

data class TutorialStep(
    val text: String,
    val beadId: String,  // Boncuk ID'si (örn: "rod4_bead_top")
    val animationType: Int  // 1: animateBeadsUp, 2: animateBeadsDown, 3: animateBeadUp, 4: animateBeadDown
) 
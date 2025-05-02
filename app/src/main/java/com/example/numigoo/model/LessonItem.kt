package com.example.numigoo.model

import androidx.fragment.app.Fragment

data class LessonItem(
    val type: Int,
    val title: String,
    var offset: Int, // -50 sol, 0 orta, 50 sağ
    var isCompleted: Boolean,
    val stepCount: Int,      // toplam adım sayısı
    var currentStep: Int,     // mevcut adım (0'dan başlar)
    val fragment: (() -> Fragment)? = null, // Fragment oluşturma fonksiyonu
    val color: Int? = null    // Renk değeri, varsayılanı null
) {
    companion object {
        const val TYPE_LESSON = 0
        const val TYPE_HEADER = 1
        const val TYPE_CHEST = 2
    }
} 
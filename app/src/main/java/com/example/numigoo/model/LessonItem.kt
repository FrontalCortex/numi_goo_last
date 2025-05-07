package com.example.numigoo.model

import android.util.Log
import androidx.fragment.app.Fragment
import com.example.numigoo.AbacusFragment
import com.example.numigoo.TutorialFragment
import com.example.numigoo.MapFragment

data class LessonItem(
    val type: Int,
    val title: String,
    var offset: Int, // -50 sol, 0 orta, 50 sağ
    var isCompleted: Boolean,
    var progressBarLevel: Int = 0,
    val stepCount: Int,      // toplam adım sayısı
    var currentStep: Int,     // mevcut adım (0'dan başlar)
    @Transient var fragment: (() -> Fragment?)? = null, // Fragment oluşturma fonksiyonu
    val color: Int? = null,    // Renk değeri, varsayılanı null
    var LESSON_ID: Int? = null,
    var lessonOperationsMap: Int? = null,
    val stepCompletionStatus: List<Boolean> = List(stepCount) { false },// Her adımın tamamlanma durumu
    var finishStepNumber: Int? = null,
    var startStepNumber: Int? = null,
    var mapFragmentIndex: Int? = null,
    var stepIsFinish: Boolean = false,
    var tutorialNumber: Int = 0,
    var tutorialIsFinish: Boolean = false,
    var lessonHint: String? = null
) {
    companion object {
        const val TYPE_LESSON = 0
        const val TYPE_HEADER = 1
        const val TYPE_CHEST = 2

        // Fragment ID sabitleri

    }

    // Fragment oluşturma fonksiyonu


} 
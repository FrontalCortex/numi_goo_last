package com.example.app.model

import androidx.fragment.app.Fragment
import com.example.app.R
import java.io.Serializable

data class LessonItem(
    val id: Int? = null,
    val type: Int,
    val title: String,
    var partId:Int? = null,
    var offset: Int, // -50 sol, 0 orta, 50 sağ
    var isCompleted: Boolean,
    var progressBarLevel: Int = 0,
    var stepCount: Int,      // toplam adım sayısı
    var currentStep: Int = 0,     // mevcut adım (0'dan başlar)
    @Transient var fragment: (() -> Fragment?)? = null, // Fragment oluşturma fonksiyonu
    val color: Int? = null,    // Renk değeri, varsayılanı null
    var LESSON_ID: Int? = null,
    var isBlinding: Boolean? = null,
    var lessonOperationsMap: Int? = null,
    var stepCompletionStatus: List<Boolean> = List(stepCount) { false },// Her adımın tamamlanma durumu
    var finishStepNumber: Int? = null,
    var startStepNumber: Int? = null,
    var mapFragmentIndex: Int? = null,
    var stepIsFinish: Boolean = false,
    var tutorialNumber: Int = 0,
    var tutorialIsFinish: Boolean = false,
    var lessonHint: String? = null,
    var stepCupIcon: Int = R.drawable.cup_ic,
    var cupTime1: String? = null,
    var cupTime2: String? = null,
    var sectionTitle: String? = null,
    var backPart: Boolean? = null,
    var sectionDescription: String? = null,
    var timePeriod: Long? = null,
    var blindingMultiplication: Boolean? = null,
    var racePartId: Int? = null,  // Race item'ının hangi partId'den veri göstereceği
    var backRaceId: Int? = null,
    var raceBusyLevel: Int? = null,
) : Serializable {
    companion object {
        const val TYPE_LESSON = 0
        const val TYPE_HEADER = 1
        const val TYPE_CHEST = 2
        const val TYPE_RACE = 3
        const val TYPE_PART = 4
        const val TYPE_BACK_PART = 5
        // Fragment ID sabitleri

    }

} //global part ıd değiştirilecek adapter'da
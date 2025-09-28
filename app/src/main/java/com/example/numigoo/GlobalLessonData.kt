package com.example.numigoo

import android.content.Context
import com.example.numigoo.model.LessonItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object GlobalLessonData {
    var globalPartId = 1
    private var _lessonItems: MutableList<LessonItem> = mutableListOf()
    val lessonItems: List<LessonItem> get() = _lessonItems

    fun initialize(context: Context, partId: Int) {
        // Önce kaydedilmiş verileri yüklemeyi dene
        if (!loadFromPreferences(context)) {
            // Eğer kayıt yoksa, ilk defa açılıyorsa default verileri yükle
            _lessonItems = createLessonItems(partId).toMutableList()
            saveToPreferences(context)
        }
    }

    fun updateLessonItem(context: Context, position: Int, newItem: LessonItem) {
        if (position in _lessonItems.indices) {
            _lessonItems[position] = newItem
            saveToPreferences(context)
        }
    }

    fun getLessonItem(position: Int): LessonItem? {
        return if (position in _lessonItems.indices) {
            _lessonItems[position]
        } else {
            null
        }
    }


    fun createLessonItems(partId : Int): List<LessonItem> {
        return when(partId) {
            1 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Kuralsız Toplama",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Sayıları abaküste tanıma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    lessonOperationsMap = 1,
                    finishStepNumber = 3,
                    tutorialNumber = 1,
                    startStepNumber = 1,
                    mapFragmentIndex = 1,
                    lessonHint = "Sayıları abaküse en büyük basamaktan başlayarak yaz."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Kuralsız toplama",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialNumber = 2,
                    startStepNumber = 4,
                    mapFragmentIndex = 2,
                    finishStepNumber = 7,
                    lessonHint = "İlk sayıyı yaz. Toplamaya 2. sayının en büyük basamağından başla."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 3,
                    finishStepNumber = 7,
                    startStepNumber = 7,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "1:30",
                    cupTime2 = "2:00"

                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "5'lik toplama",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_pink

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit 5'lik toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialNumber = 3,
                    startStepNumber = 8,
                    mapFragmentIndex = 5,
                    finishStepNumber = 9,
                    lessonHint = "5 gelir kardeş gider."


                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 5'lik toplama",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 12,
                    mapFragmentIndex = 6,
                    finishStepNumber = 15,
                    tutorialNumber = 4

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 5'lik toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 15,
                    mapFragmentIndex = 7,
                    finishStepNumber = 18,
                    tutorialIsFinish = true


                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 8,
                    startStepNumber = 19,
                    finishStepNumber = 19,
                    cupTime1 = "2:00",
                    cupTime2 = "3:00"

                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "10'luk Toplama 1-2-3-4-5",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 10'luk toplama",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 20,
                    mapFragmentIndex = 10,
                    finishStepNumber = 23,
                    tutorialNumber = 5

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 10'luk toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 24,
                    mapFragmentIndex = 11,
                    finishStepNumber = 27,
                    tutorialNumber = 6

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 10'luk toplama",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 28,
                    mapFragmentIndex = 12,
                    finishStepNumber = 31,
                    tutorialIsFinish = true
                ),LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Çılgın 10'luk toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 32,
                    mapFragmentIndex = 13,
                    finishStepNumber = 35,
                    tutorialIsFinish = true
                ),LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 14,
                    startStepNumber = 36,
                    finishStepNumber = 36,
                    cupTime1 = "2:00",
                    cupTime2 = "3:00"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "10'luk Toplama 6-7-8-9",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_orange

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Orta seviye 10'luk toplama",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 37,
                    mapFragmentIndex = 16,
                    finishStepNumber = 40,
                    tutorialNumber = 7

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk toplama mantığı",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 41,
                    mapFragmentIndex = 17,
                    finishStepNumber = 43,
                    tutorialIsFinish = true,
                    lessonHint = "İlk sayıyı yaz. Toplamaya 2. sayının en büyük basamağından başla."

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 10'luk toplama",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 44,
                    mapFragmentIndex = 18,
                    finishStepNumber = 47,
                    tutorialIsFinish = true,
                    lessonHint = "İlk sayıyı yaz. Toplamaya 2. sayının en büyük basamağından başla."

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Çılgın 10'luk toplama",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 48,
                    mapFragmentIndex = 19,
                    finishStepNumber = 51,
                    tutorialIsFinish = true,
                    lessonHint = "İlk sayıyı yaz. Toplamaya 2. sayının en büyük basamağından başla."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 20,
                    startStepNumber = 52,
                    finishStepNumber = 52,
                    cupTime1 = "3:00",
                    cupTime2 = "4:00"

                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Boncuk kuralı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel Boncuk Kuralı",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 53,
                    mapFragmentIndex = 22,
                    finishStepNumber = 56,
                    tutorialNumber = 8,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor Boncuk Kuralı",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 57,
                    mapFragmentIndex = 23,
                    finishStepNumber = 60,
                    tutorialNumber = 9,
                    lessonHint = "10 gelir adımını uygularken 5'lik kuralı kullanman gerekebilir."

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız Boncuk Kuralı",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 61,
                    mapFragmentIndex = 24,
                    finishStepNumber = 64,
                    tutorialIsFinish = true,
                    lessonHint = "10 gelir adımını uygularken 5\\'lik kuralı kullanman gerekebilir."

                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 25,
                    startStepNumber = 65,
                    finishStepNumber = 65,
                    cupTime1 = "3:00",
                    cupTime2 = "4:00"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Ustalık Yolu",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_green
                ),
                LessonItem(
                    type = LessonItem.TYPE_RACE,
                    title = "Ustalık Yolu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 27,

                    ),
                LessonItem(
                    partId = 2,
                    type = LessonItem.TYPE_PART,
                    title = "2. Kısım",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 27,
                    sectionTitle = "2. Kısım Çıkartma",
                    sectionDescription = "Abaküste çıkartmaya dair her şeyi öğreneceğiz. "

                )
            )
            2 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Kuralsız Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_BACK_PART,
                    title = "1. Kısım",
                    offset = 0,
                    isCompleted = true,
                    partId = 1,
                    stepCount = 1,
                    currentStep = 1,
                    backPart = true,
                    sectionTitle = "2. Kısım Çıkartma",
                    sectionDescription = "Abaküste çıkartmaya dair her şeyi öğreneceğiz."

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit kuralsız çıkarma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 66,
                    mapFragmentIndex = 2,
                    finishStepNumber = 69,
                    tutorialNumber = 10,

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Orta seviye kuralsız çıkarma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    startStepNumber = 70,
                    mapFragmentIndex = 3,
                    finishStepNumber = 73,
                    lessonHint = "İlk sayıyı yaz. Çıkarmaya 2. sayının en büyük basamağından başla."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 4,
                    finishStepNumber = 74,
                    startStepNumber = 74,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "1:30",
                    cupTime2 = "2:00"

                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "5'lik Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_green
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 5'lik çıkarma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialNumber = 11,
                    startStepNumber = 75,
                    mapFragmentIndex = 6,
                    finishStepNumber = 78,
                    lessonHint = "5 gider. Kardeş gelir"
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 5'lik çıkarma",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    startStepNumber = 79,
                    mapFragmentIndex = 7,
                    finishStepNumber = 82,
                    lessonHint = "5 gider. Kardeş gelir"
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Kurallı - Kuralsız Çıkarma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    startStepNumber = 83,
                    mapFragmentIndex = 8,
                    finishStepNumber = 86,
                    lessonHint = "5 gider. Kardeş gelir"
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 9,
                    finishStepNumber = 87,
                    startStepNumber = 87,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "2:00",
                    cupTime2 = "2:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "10'luk Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 10'luk Çıkarma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialNumber = 12,
                    startStepNumber = 88,
                    mapFragmentIndex = 11,
                    finishStepNumber = 91,
                    lessonHint = "10 gider. Kardeş gelir."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 10'luk Çıkarma",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialNumber = 13,
                    startStepNumber = 92,
                    mapFragmentIndex = 12,
                    finishStepNumber = 95,
                    lessonHint = "10 gider. Kardeş gelir."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk Çıkarma İçerisinde 5'lik Çıkarma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialNumber = 14,
                    startStepNumber = 96,
                    mapFragmentIndex = 13,
                    finishStepNumber = 99,
                    lessonHint = "10 gider. Kardeş gelir."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 10'luk Çıkarma",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialNumber = 15,
                    startStepNumber = 100,
                    mapFragmentIndex = 14,
                    finishStepNumber = 103,
                    lessonHint = "10 gider. Kardeş gelir."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 15,
                    finishStepNumber = 104,
                    startStepNumber = 104,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "2:00",
                    cupTime2 = "3:00"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Boncuk Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_yellow
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel Boncuk Çıkarma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialNumber = 16,
                    startStepNumber = 105,
                    mapFragmentIndex = 17,
                    finishStepNumber = 108,
                    lessonHint = "Kardeş gelirken 5'lik kural uygula."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor Boncuk Çıkarma",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialNumber = 17,
                    startStepNumber = 109,
                    mapFragmentIndex = 18,
                    finishStepNumber = 112,
                    lessonHint = "Kardeş gelirken 5'lik kural uygula."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız Boncuk Çıkarma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    startStepNumber = 113,
                    mapFragmentIndex = 19,
                    finishStepNumber = 116,
                    lessonHint = "Kardeş gelirken 5'lik kural uygula."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 20,
                    finishStepNumber = 117,
                    startStepNumber = 117,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "2:00",
                    cupTime2 = "3:00"
                ),
                LessonItem(
                    partId = 3,
                    type = LessonItem.TYPE_PART,
                    title = "3. Kısım",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 21,
                    sectionTitle = "3. Kısım Çarpma",
                    sectionDescription = "Abaküste çarpmaya dair her şey. "

                )
            )
            3 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "2'ye 1 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_BACK_PART,
                    title = "2. Kısım",
                    offset = 0,
                    isCompleted = true,
                    partId = 2,
                    stepCount = 1,
                    currentStep = 1,
                    backPart = true,
                    sectionTitle = "3. Kısım Çarpma",
                    sectionDescription = "Abaküste çarpmaya dair her şey."

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 2'ye 1 çarpma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 118,
                    mapFragmentIndex = 2,
                    finishStepNumber = 121,
                    tutorialNumber = 18,
                    lessonHint = "Birler ile onlar basamağı çarpılınca sonuç onlar basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 2'ye 1 çarpma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 122,
                    mapFragmentIndex = 3,
                    finishStepNumber = 125,
                    tutorialNumber = 19,
                    lessonHint = "Birler ile onlar basamağı çarpılınca sonuç onlar basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 2'ye 1 çarpma",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 126,
                    mapFragmentIndex = 4,
                    finishStepNumber = 129,
                    tutorialIsFinish = true,
                    lessonHint = "Birler ile onlar basamağı çarpılınca sonuç onlar basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 5,
                    finishStepNumber = 130,
                    startStepNumber = 130,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "1:00",
                    cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "2'ye 2 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 2'ye 2 çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 131,
                    mapFragmentIndex = 7,
                    finishStepNumber = 134,
                    tutorialNumber = 20,
                    lessonHint = "Onlar ile onlar basamağı çarpılınca sonuç yüzler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 2'ye 2 çarpma",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 131,
                    mapFragmentIndex = 8,
                    finishStepNumber = 134,
                    tutorialIsFinish = true,
                    lessonHint = "Onlar ile onlar basamağı çarpılınca sonuç yüzler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 2'ye 2 çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 135,
                    mapFragmentIndex = 9,
                    finishStepNumber = 138,
                    tutorialIsFinish = true,
                    lessonHint = "Onlar ile onlar basamağı çarpılınca sonuç yüzler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 10,
                    finishStepNumber = 139,
                    startStepNumber = 139,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "3:30",
                    cupTime2 = "5:00"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "3'e 1 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_orange
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 3'e 1 çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 140,
                    mapFragmentIndex = 12,
                    finishStepNumber = 143,
                    tutorialNumber = 21,
                    lessonHint = "Yüzler ile birler basamağı çarpılınca sonuç yüzler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 3'e 1 çarpma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 140,
                    mapFragmentIndex = 13,
                    finishStepNumber = 143,
                    tutorialIsFinish = true,
                    lessonHint = "Yüzler ile birler basamağı çarpılınca sonuç yüzler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 3'e 1 çarpma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 140,
                    mapFragmentIndex = 14,
                    finishStepNumber = 143,
                    tutorialIsFinish = true,
                    lessonHint = "Yüzler ile birler basamağı çarpılınca sonuç yüzler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 15,
                    finishStepNumber = 144,
                    startStepNumber = 144,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "2:30",
                    cupTime2 = "3:30"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "3'e 2 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_pink
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 3'e 2 çarpma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 145,
                    mapFragmentIndex = 17,
                    finishStepNumber = 148,
                    tutorialNumber = 22,
                    lessonHint = "Yüzler ile onlar basamağı çarpılınca sonuç binler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 3'e 2 çarpma",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 145,
                    mapFragmentIndex = 18,
                    finishStepNumber = 148,
                    tutorialIsFinish = true,
                    lessonHint = "Yüzler ile onlar basamağı çarpılınca sonuç binler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 3'e 2 çarpma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 149,
                    mapFragmentIndex = 19,
                    finishStepNumber = 152,
                    tutorialNumber = 23,
                    lessonHint = "Yüzler ile onlar basamağı çarpılınca sonuç binler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 20,
                    finishStepNumber = 153,
                    startStepNumber = 153,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "3:00",
                    cupTime2 = "5:30"
                ),
                LessonItem(
                    partId = 4,
                    type = LessonItem.TYPE_PART,
                    title = "4. Kısım",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 21,
                    sectionTitle = "4. Körleme Toplama",
                    sectionDescription = "Akıldan Toplama"
                )
            )
            4 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Kuralsız Toplama",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_green
                ),
                LessonItem(
                    type = LessonItem.TYPE_BACK_PART,
                    title = "3. Kısım",
                    offset = 0,
                    isCompleted = true,
                    partId = 2,
                    stepCount = 1,
                    currentStep = 1,
                    backPart = true,
                    sectionTitle = "3. Kısım Çarpma",
                    sectionDescription = "Abaküste çarpmaya dair her şey. "

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit Kuralsız Toplama",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 1,
                    mapFragmentIndex = 2,
                    finishStepNumber = 4,
                    isBlinding = true,
                    tutorialNumber = 24,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit Kuralsız Toplama",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 5,
                    mapFragmentIndex = 3,
                    finishStepNumber = 8,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit Kuralsız Toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 9,
                    mapFragmentIndex = 4,
                    finishStepNumber = 12,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 5,
                    finishStepNumber = 13,
                    startStepNumber = 13,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    cupTime1 = "1:00",
                    cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "5'lik Toplama",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 5'lik Toplama",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 14,
                    mapFragmentIndex = 7,
                    finishStepNumber = 17,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 5'lik Toplama",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 14,
                    mapFragmentIndex = 8,
                    finishStepNumber = 17,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2500,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 5'lik Toplama",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 14,
                    mapFragmentIndex = 9,
                    finishStepNumber = 17,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 10,
                    finishStepNumber = 18,
                    startStepNumber = 18,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    cupTime1 = "1:00",
                    cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "10'luk Toplama 1-2-3-4-5",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit 10'luk Toplama",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 19,
                    mapFragmentIndex = 12,
                    finishStepNumber = 22,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 10'luk Toplama",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 19,
                    mapFragmentIndex = 13,
                    finishStepNumber = 22,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 10'luk Toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 19,
                    mapFragmentIndex = 14,
                    finishStepNumber = 22,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 15,
                    finishStepNumber = 23,
                    startStepNumber = 23,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    cupTime1 = "1:00",
                    cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "10'luk Toplama 6,7,8,9",
                    offset = 30,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit 10'luk Toplama",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 24,
                    mapFragmentIndex = 17,
                    finishStepNumber = 27,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 10'luk Toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 24,
                    mapFragmentIndex = 18,
                    finishStepNumber = 27,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 10'luk Toplama",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 24,
                    mapFragmentIndex = 19,
                    finishStepNumber = 27,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 20,
                    finishStepNumber = 28,
                    startStepNumber = 28,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    cupTime1 = "1:00",
                    cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Boncuk Kuralı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_green
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel Boncuk Toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 29,
                    mapFragmentIndex = 22,
                    finishStepNumber = 32,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor Boncuk Toplama",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 29,
                    mapFragmentIndex = 23,
                    finishStepNumber = 32,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız Boncuk Toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 29,
                    mapFragmentIndex = 24,
                    finishStepNumber = 32,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 25,
                    finishStepNumber = 33,
                    startStepNumber = 33,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    cupTime1 = "1:00",
                    cupTime2 = "1:45"
                ),
                LessonItem(
                    partId = 5,
                    type = LessonItem.TYPE_PART,
                    title = "5. Kısım",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 25,
                    sectionTitle = "5. Körleme Çıkarma",
                    sectionDescription = "Akıldan Çıkarma"
                )

            )
            5 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Kuralsız Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_BACK_PART,
                    title = "4. Kısım",
                    offset = 0,
                    isCompleted = true,
                    partId = 4,
                    stepCount = 1,
                    currentStep = 1,
                    backPart = true,
                    sectionTitle = "4. Kısım Körleme Toplama",
                    sectionDescription = "Körleme Toplama"

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit Kuralsız Çıkarma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 34,
                    mapFragmentIndex = 2,
                    finishStepNumber = 37,
                    isBlinding = true,
                    tutorialNumber = 25,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor Kuralsız Çıkarma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 34,
                    mapFragmentIndex = 3,
                    finishStepNumber = 37,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız Kuralsız Çıkarma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 34,
                    mapFragmentIndex = 4,
                    finishStepNumber = 37,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 5,
                    finishStepNumber = 38,
                    startStepNumber = 38,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    cupTime1 = "1:00",
                    cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "5'lik Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit 5'lik Çıkarma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 39,
                    mapFragmentIndex = 7,
                    finishStepNumber = 42,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 5'lik Çıkarma",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 39,
                    mapFragmentIndex = 8,
                    finishStepNumber = 42,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 5'lik Çıkarma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 39,
                    mapFragmentIndex = 9,
                    finishStepNumber = 42,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 10,
                    finishStepNumber = 43,
                    startStepNumber = 43,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    cupTime1 = "1:00",
                    cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "10'luk Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 10'luk Çıkarma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 44,
                    mapFragmentIndex = 12,
                    finishStepNumber = 47,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 10'luk Çıkarma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 44,
                    mapFragmentIndex = 13,
                    finishStepNumber = 47,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 10'luk Çıkarma",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 44,
                    mapFragmentIndex = 14,
                    finishStepNumber = 47,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 15,
                    finishStepNumber = 48,
                    startStepNumber = 48,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    cupTime1 = "1:00",
                    cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Boncuk Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel Boncuk Çıkarma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 49,
                    mapFragmentIndex = 17,
                    finishStepNumber = 52,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel Boncuk Çıkarma",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 49,
                    mapFragmentIndex = 18,
                    finishStepNumber = 52,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel Boncuk Çıkarma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 49,
                    mapFragmentIndex = 19,
                    finishStepNumber = 52,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 20,
                    finishStepNumber = 53,
                    startStepNumber = 53,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    cupTime1 = "1:00",
                    cupTime2 = "1:45"
                ),
                LessonItem(
                    partId = 6,
                    type = LessonItem.TYPE_PART,
                    title = "6. Kısım",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 21,
                    sectionTitle = "6. Körleme Çarpma",
                    sectionDescription = "Akıldan Çarpma"
                )
            )
            6 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Körleme Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_BACK_PART,
                    title = "5. Kısım",
                    offset = 0,
                    isCompleted = true,
                    partId = 5,
                    stepCount = 1,
                    currentStep = 1,
                    backPart = true,
                    sectionTitle = "5. Kısım Körleme Çıkarma",
                    sectionDescription = "Körleme Çıkarma"

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit Temel Çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 54,
                    mapFragmentIndex = 2,
                    finishStepNumber = 57,
                    isBlinding = true,
                    tutorialNumber = 26,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor Temel Çarpma",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 54,
                    mapFragmentIndex = 3,
                    finishStepNumber = 57,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız Çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 58,
                    mapFragmentIndex = 4,
                    finishStepNumber = 61,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 5,
                    finishStepNumber = 62,
                    startStepNumber = 62,
                    blindingMultiplication = true,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "1:00",
                    cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "2'ye 1 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "2'ye 1 Çarpma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 63,
                    mapFragmentIndex = 7,
                    finishStepNumber = 66,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "2'ye 1 Çarpma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 63,
                    mapFragmentIndex = 8,
                    finishStepNumber = 66,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "2'ye 1 Çarpma",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 63,
                    mapFragmentIndex = 9,
                    finishStepNumber = 66,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 10,
                    finishStepNumber = 67,
                    startStepNumber = 67,
                    blindingMultiplication = true,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "1:00",
                    cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Temel 2'ye 2 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit 2'ye 2 Çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 68,
                    mapFragmentIndex = 12,
                    finishStepNumber = 71,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 2'ye 2 Çarpma",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 68,
                    mapFragmentIndex = 13,
                    finishStepNumber = 71,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 2'ye 2 Çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 68,
                    mapFragmentIndex = 14,
                    finishStepNumber = 71,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 15,
                    finishStepNumber = 72,
                    startStepNumber = 72,
                    blindingMultiplication = true,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "1:00",
                    cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "2'ye 2 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "2'ye 2 Çarpma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 73,
                    mapFragmentIndex = 17,
                    finishStepNumber = 76,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "2'ye 2 Çarpma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 73,
                    mapFragmentIndex = 18,
                    finishStepNumber = 76,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "2'ye 2 Çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 73,
                    mapFragmentIndex = 19,
                    finishStepNumber = 76,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 20,
                    finishStepNumber = 77,
                    startStepNumber = 77,
                    blindingMultiplication = true,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "2:00",
                    cupTime2 = "3:00"
                )
            )
            else -> emptyList()
        }
    }

    // SharedPreferences işlemleri
    private const val PREFS_NAME = "LessonPrefs"
    private const val KEY_LESSON_ITEMS = "lesson_items"
    private fun getKey(partId: Int) = "lesson_items_part_$partId"

    fun saveToPreferences(context: Context) {
        val prefs = context.getSharedPreferences("LessonPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(_lessonItems)
        prefs.edit().putString(getKey(globalPartId), json).apply()
    }

    fun loadFromPreferences(context: Context): Boolean {
        val prefs = context.getSharedPreferences("LessonPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString(getKey(globalPartId), null)
        return if (json != null) {
            val gson = Gson()
            val type = object : TypeToken<List<LessonItem>>() {}.type
            _lessonItems = gson.fromJson(json, type)
            true
        } else {
            false
        }
    }

}
package com.example.numigoo.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.numigoo.R

class LessonViewModel : ViewModel() {
    // Ana dersler ve alt dersler için LiveData
    private val _currentLessons = MutableLiveData<List<LessonItem>>()
    val currentLessons: LiveData<List<LessonItem>> = _currentLessons

    private val allLessons = mutableMapOf<Int, MutableList<LessonItem>>(
        1 to getSubLessonsForPart(1).toMutableList(),
        2 to getSubLessonsForPart(2).toMutableList(),
        // ... diğer partId'ler
    )

    fun initializeLessons() {
        // Tüm parçaların derslerini birleştir
        val allLessons = mutableListOf<LessonItem>()

        // Her parça için dersleri al ve listeye ekle
        for (partNumber in 1..5) { // veya kaç parça varsa
            val partLessons = getSubLessonsForPart(partNumber)
            allLessons.addAll(partLessons)
            
            // allLessons map'ini güncelle
            this.allLessons[partNumber] = partLessons.toMutableList()
        }

        _currentLessons.value = allLessons
        Log.d("LessonViewModel", "Initialized lessons - Total items: ${allLessons.size}")
    }
    // İlk yüklemede ana dersleri göster
    init {
        // Başlangıçta partId 1'in alt derslerini göster
        _currentLessons.value = getSubLessonsForPart(1)
    }

    // Alt derslere geçiş fonksiyonu
    fun showSubLessons(partId: Int) {
        Log.d("LessonViewModel", "Showing sub lessons for part $partId")
        allLessons[partId]?.let { list ->
            val newList = ArrayList(list)
            _currentLessons.postValue(newList)
            Log.d("LessonViewModel", "Updated current lessons - size: ${newList.size}")
        }
    }

    // Ana derslere geri dönüş fonksiyonu


    // Alt dersleri getiren yardımcı fonksiyon
    private fun getSubLessonsForPart(partId: Int): List<LessonItem> {
        // Burada partId'ye göre alt dersleri döndür
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
                    isCompleted = false,
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
                    id = 2,
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
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit kuralsız çıkarma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 66,
                    mapFragmentIndex = 29,
                    finishStepNumber = 69,
                    tutorialNumber = 10,
                    tutorialIsFinish = true

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
                    mapFragmentIndex = 2,
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
                    mapFragmentIndex = 3,
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
                    mapFragmentIndex = 2,
                    finishStepNumber = 78,
                    lessonHint = "5 gider. Kardeş gelir"
                )
            )
            // ... diğer partId'ler için case'ler
            else -> emptyList()
        }
    }


    fun updateLessonItem(partId: Int, index: Int, updatedItem: LessonItem) {
        Log.d("LessonViewModel", "Updating item - partId: $partId, index: $index")
        
        // Mevcut listeyi al
        val currentList = _currentLessons.value?.toMutableList() ?: return
        Log.d("LessonViewModel", "Current list size: ${currentList.size}")
        
        // Güncellenecek item'ı bul
        val itemToUpdate = currentList.getOrNull(index) ?: return
        Log.d("LessonViewModel", "Current item at index $index: ${itemToUpdate.currentStep}")
        
        // Item'ı güncelle
        currentList[index] = updatedItem
        Log.d("LessonViewModel", "Updated item at index $index: ${currentList[index].currentStep}")
        
        // Yeni bir liste oluştur ve tüm item'ları kopyala
        val newList = currentList.map { item ->
            if (item.mapFragmentIndex == updatedItem.mapFragmentIndex) {
                updatedItem.copy(
                    currentStep = updatedItem.currentStep,
                    stepCompletionStatus = updatedItem.stepCompletionStatus.toList(),
                    stepIsFinish = updatedItem.stepIsFinish,
                    isCompleted = updatedItem.isCompleted
                )
            } else {
                item.copy(
                    currentStep = item.currentStep,
                    stepCompletionStatus = item.stepCompletionStatus.toList(),
                    stepIsFinish = item.stepIsFinish,
                    isCompleted = item.isCompleted
                )
            }
        }
        
        // LiveData'yı güncelle
        _currentLessons.value = newList
        
        // Güncellenmiş listeyi logla
        Log.d("LessonViewModel", "New list size: ${newList.size}")
        Log.d("LessonViewModel", "New list item at index $index: ${newList[index].currentStep}")
        
        // allLessons map'ini de güncelle
        allLessons[partId]?.let { list ->
            if (index in list.indices) {
                list[index] = updatedItem.copy(
                    currentStep = updatedItem.currentStep,
                    stepCompletionStatus = updatedItem.stepCompletionStatus.toList(),
                    stepIsFinish = updatedItem.stepIsFinish,
                    isCompleted = updatedItem.isCompleted
                )
            }
        }
    }
}
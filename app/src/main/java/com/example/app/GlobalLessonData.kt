package com.example.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.app.model.LessonItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object GlobalLessonData {
    var globalPartId = 1
    private var _lessonItems: MutableList<LessonItem> = mutableListOf()
    val lessonItems: List<LessonItem> get() = _lessonItems

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private const val FIRESTORE_LESSON_PROGRESS = "lessonProgress"
    private const val AUTH_WAIT_TIMEOUT_MS = 1500L

    private enum class FirestoreLoadStatus {
        LOADED,
        NOT_FOUND,
        ERROR
    }

    private const val LOG_TAG = "LessonProgress"

    fun initialize(context: Context, partId: Int, onReady: (() -> Unit)? = null) {
        globalPartId = partId
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d(LOG_TAG, "initialize partId=$partId uid=${uid?.take(8) ?: "null"} onReady=${onReady != null}")

        // Önce lokaldeki veriyi dene
        if (loadFromPreferences(context)) {
            Log.d(LOG_TAG, "initialize: loaded from LOCAL prefs, items=${_lessonItems.size}")
            onReady?.invoke()
            return
        }

        // Callback yoksa (senkron kullanım), sadece default (Firestore async beklenemez)
        if (onReady == null) {
            Log.d(LOG_TAG, "initialize: no callback -> applyDefaultLessonItems (sync path)")
            applyDefaultLessonItems(context, partId)
            return
        }

        // Callback var: lokal yoksa Firestore'u mutlaka dene (auth geç açılıyorsa kısa süre bekle)
        val auth = FirebaseAuth.getInstance()
        val immediateUid = auth.currentUser?.uid
        if (immediateUid != null) {
            Log.d(LOG_TAG, "initialize: uid present -> loadFromFirestore partId=$partId")
            loadFromFirestore(context, partId, immediateUid) { status ->
                Log.d(LOG_TAG, "initialize: loadFromFirestore result=$status")
                when (status) {
                    FirestoreLoadStatus.LOADED -> Log.d(LOG_TAG, "initialize: using CLOUD data, items=${_lessonItems.size}")
                    FirestoreLoadStatus.NOT_FOUND -> {
                        Log.d(LOG_TAG, "initialize: NOT_FOUND -> applyDefaultLessonItems (first time or no cloud data)")
                        applyDefaultLessonItems(context, partId)
                    }
                    FirestoreLoadStatus.ERROR -> {
                        Log.w(LOG_TAG, "initialize: ERROR -> applyDefaultLessonItems(saveRemote=false) to avoid overwriting cloud")
                        applyDefaultLessonItems(context, partId, saveRemote = false)
                    }
                }
                onReady.invoke()
            }
            return
        }

        Log.d(LOG_TAG, "initialize: uid null -> waitForAuth (timeout ${AUTH_WAIT_TIMEOUT_MS}ms)")
        waitForAuthUid(auth, AUTH_WAIT_TIMEOUT_MS) { waitedUid ->
            if (waitedUid == null) {
                Log.d(LOG_TAG, "initialize: after wait still no uid -> applyDefaultLessonItems (guest)")
                applyDefaultLessonItems(context, partId)
                onReady.invoke()
                return@waitForAuthUid
            }
            Log.d(LOG_TAG, "initialize: after wait uid present -> loadFromFirestore partId=$partId")
            loadFromFirestore(context, partId, waitedUid) { status ->
                Log.d(LOG_TAG, "initialize: loadFromFirestore result=$status")
                when (status) {
                    FirestoreLoadStatus.LOADED -> Log.d(LOG_TAG, "initialize: using CLOUD data, items=${_lessonItems.size}")
                    FirestoreLoadStatus.NOT_FOUND -> {
                        Log.d(LOG_TAG, "initialize: NOT_FOUND -> applyDefaultLessonItems")
                        applyDefaultLessonItems(context, partId)
                    }
                    FirestoreLoadStatus.ERROR -> {
                        Log.w(LOG_TAG, "initialize: ERROR -> applyDefaultLessonItems(saveRemote=false)")
                        applyDefaultLessonItems(context, partId, saveRemote = false)
                    }
                }
                onReady.invoke()
            }
        }
    }

    /** Varsayılan ders listesini oluşturur (tutorial1 bayrağı varsa 1. dersi günceller) ve kaydeder. */
    private fun applyDefaultLessonItems(context: Context, partId: Int, saveRemote: Boolean = true) {
        Log.d(LOG_TAG, "applyDefaultLessonItems partId=$partId saveRemote=$saveRemote (overwrites current _lessonItems with defaults)")
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val tutorial1Pending = prefs.getBoolean("tutorial1_login_flow_pending", false)
        val items = createLessonItems(partId).toMutableList()

        if (partId == 1 && tutorial1Pending && items.size > 1) {
            val original = items[1]
            val updated = original.copy(
                tutorialIsFinish = true,
                currentStep = 2,
                startStepNumber = 2,
                stepCompletionStatus = listOf(true, false, false)
            )
            items[1] = updated
        }

        _lessonItems = items
        saveToPreferences(context, saveRemote = saveRemote)
    }

    fun updateLessonItem(context: Context, position: Int, newItem: LessonItem) {
        if (position in _lessonItems.indices) {
            _lessonItems[position] = newItem
            Log.d(LOG_TAG, "updateLessonItem position=$position title=${newItem.title.take(30)} stepIsFinish=${newItem.stepIsFinish} -> saving to local+Firestore")
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
                    title = "Sayıları Abaküste Tanıma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Sayıları abaküste tanıma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    lessonOperationsMap = 2,
                    finishStepNumber = 3,
                    tutorialNumber = 1,
                    startStepNumber = 1,
                    mapFragmentIndex = 1,
                    lessonHint = "En sağdaki sütunu kullan. Aşağıda boncuklar birlik, yukarıdaki beşlik değere sahip."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İki basamaklı sayılar",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    lessonOperationsMap = 1,
                    finishStepNumber = 1002,
                    tutorialNumber = 100,
                    startStepNumber = 1000,
                    mapFragmentIndex = 2,
                    lessonHint = "Sayıları abaküse en büyük basamaktan başlayarak yaz."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Üç basamaklı sayılar",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 2,
                    currentStep = 1,
                    lessonOperationsMap = 1,
                    finishStepNumber = 1004,
                    tutorialNumber = 101,
                    startStepNumber = 1003,
                    mapFragmentIndex = 3,
                    lessonHint = "Sayıları abaküse en büyük basamaktan başlayarak yaz."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 4,
                    finishStepNumber = 1005,
                    startStepNumber = 1005,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "1:00",
                    cupTime2 = "2:00"

                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Kuralsız toplama",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Kuralsız toplama - 1 basamaklı",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    tutorialNumber = 2,
                    startStepNumber = 4,
                    mapFragmentIndex = 6,
                    finishStepNumber = 6,
                    lessonHint = "İlk sayıyı yaz. Toplanacak sayı değerinde boncuk ekle.",
                    abacusGuideNumber = 1
                ),LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Kuralsız toplama - 2 basamaklı",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    tutorialNumber = 102,
                    startStepNumber = 1007,
                    mapFragmentIndex = 7,
                    finishStepNumber = 1009,
                    lessonHint = "Toplanacak sayıyı en büyük basamaktan başlayarak ekle.",
                ),LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Kuralsız toplama - 3 basamaklı",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    tutorialNumber = 103,
                    startStepNumber = 1010,
                    mapFragmentIndex = 8,
                    finishStepNumber = 1012,
                    lessonHint = "Toplanacak sayıyı en büyük basamaktan başlayarak ekle.",
                ),LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Kuralsız toplama - 4,5 basamaklı",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    //tutorialIsFinish = true,
                    tutorialNumber = 9999,
                    startStepNumber = 1013,
                    mapFragmentIndex = 9,
                    finishStepNumber = 1015,
                    lessonHint = "Toplanacak sayıyı en büyük basamaktan başlayarak ekle.",
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Değerlendirme",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 10,
                    finishStepNumber = 7,
                    startStepNumber = 7,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupTime1 = "1:00",
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
                    mapFragmentIndex = 12,
                    finishStepNumber = 11,
                    lessonHint = "5 gelir. Kardeş gider.",
                    abacusGuideNumber = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 5'lik toplama",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 12,
                    mapFragmentIndex = 13,
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
                    startStepNumber = 16,
                    mapFragmentIndex = 14,
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
                    mapFragmentIndex = 15,
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
                    mapFragmentIndex = 17,
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
                    mapFragmentIndex = 18,
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
                    mapFragmentIndex = 19,
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
                    mapFragmentIndex = 20,
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
                    mapFragmentIndex = 21,
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
                    mapFragmentIndex = 23,
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
                    mapFragmentIndex = 24,
                    finishStepNumber = 43,
                    tutorialNumber = 105,
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
                    mapFragmentIndex = 25,
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
                    mapFragmentIndex = 26,
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
                    mapFragmentIndex = 27,
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
                    mapFragmentIndex = 29,
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
                    mapFragmentIndex = 30,
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
                    mapFragmentIndex = 31,
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
                    mapFragmentIndex = 32,
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
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 34,
                    racePartId = 7,
                    backRaceId = 1
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
                    mapFragmentIndex = 35,
                    sectionTitle = "2. Kısım Çıkarma",
                    sectionDescription = "Abaküste çıkarmaya dair her şeyi öğreneceğiz. "

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
                    sectionTitle = "2. Kısım Çıkarma",
                    sectionDescription = "Abaküste çıkarmaya dair her şeyi öğreneceğiz."

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
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 21,
                    racePartId = 8,
                    backRaceId = 2
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
            7 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Acemi Çırak",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 78,
                    mapFragmentIndex = 0,
                    finishStepNumber = 78,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 0
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Başlangıç Ustası",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 78,
                    mapFragmentIndex = 1,
                    finishStepNumber = 78,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    raceBusyLevel = 1,
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Bilgi Avcısı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 79,
                    mapFragmentIndex = 2,
                    finishStepNumber = 79,
                    tutorialIsFinish = true,
                    timePeriod = 8000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Öğrenme Ustası",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 79,
                    mapFragmentIndex = 3,
                    finishStepNumber = 79,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zihin Kaşifi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 80,
                    mapFragmentIndex = 4,
                    finishStepNumber = 80,
                    tutorialIsFinish = true,
                    timePeriod = 4000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Bilgelik Ustası",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 80,
                    mapFragmentIndex = 5,
                    finishStepNumber = 80,
                    tutorialIsFinish = true,
                    timePeriod = 2500,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zeka Kaşifi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 81,
                    mapFragmentIndex = 6,
                    finishStepNumber = 81,
                    tutorialIsFinish = true,
                    timePeriod = 8000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Beyin Mühendisi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 81,
                    mapFragmentIndex = 7,
                    finishStepNumber = 81,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Mantık Üstadı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 82,
                    mapFragmentIndex = 8,
                    finishStepNumber = 82,
                    tutorialIsFinish = true,
                    timePeriod = 4000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Deha",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 82,
                    mapFragmentIndex = 9,
                    finishStepNumber = 82,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Üst Zihin",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1, //burdayız bra
                    startStepNumber = 83,
                    mapFragmentIndex = 10,
                    finishStepNumber = 83,
                    tutorialIsFinish = true,
                    timePeriod = 8000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Bilge Şampiyon",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 83,
                    mapFragmentIndex = 11,
                    finishStepNumber = 83,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zeka Mimarı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 84,
                    mapFragmentIndex = 12,
                    finishStepNumber = 84,
                    tutorialIsFinish = true,
                    timePeriod = 4000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Ustalık Efendisi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 84,
                    mapFragmentIndex = 13,
                    finishStepNumber = 84,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Efsanevi Bilge",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 84,
                    mapFragmentIndex = 14,
                    finishStepNumber = 84,
                    tutorialIsFinish = true,
                    timePeriod = 2500,
                    raceBusyLevel = 2
                )
            )
            8 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Acemi Çırak",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 85,
                    mapFragmentIndex = 0,
                    finishStepNumber = 85,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Başlangıç Ustası",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 85,
                    mapFragmentIndex = 1,
                    finishStepNumber = 85,
                    tutorialIsFinish = true,
                    timePeriod = 1000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Bilgi Avcısı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 86,
                    mapFragmentIndex = 2,
                    finishStepNumber = 86,
                    tutorialIsFinish = true,
                    timePeriod = 8000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Öğrenme Ustası",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 86,
                    mapFragmentIndex = 3,
                    finishStepNumber = 86,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zihin Kaşifi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 87,
                    mapFragmentIndex = 4,
                    finishStepNumber = 87,
                    tutorialIsFinish = true,
                    timePeriod = 4000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Bilgelik Ustası",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 87,
                    mapFragmentIndex = 5,
                    finishStepNumber = 87,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zeka Kaşifi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 88,
                    mapFragmentIndex = 6,
                    finishStepNumber = 88,
                    tutorialIsFinish = true,
                    timePeriod = 8000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Beyin Mühendisi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 88,
                    mapFragmentIndex = 7,
                    finishStepNumber = 88,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Mantık Üstadı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 89,
                    mapFragmentIndex = 8,
                    finishStepNumber = 89,
                    tutorialIsFinish = true,
                    timePeriod = 4000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Deha",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 89,
                    mapFragmentIndex = 9,
                    finishStepNumber = 89,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Üst Zihin",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1, //burdayız bra
                    startStepNumber = 90,
                    mapFragmentIndex = 10,
                    finishStepNumber = 90,
                    tutorialIsFinish = true,
                    timePeriod = 8000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Bilge Şampiyon",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 90,
                    mapFragmentIndex = 11,
                    finishStepNumber = 90,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zeka Mimarı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 90,
                    mapFragmentIndex = 12,
                    finishStepNumber = 90,
                    tutorialIsFinish = true,
                    timePeriod = 4000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Ustalık Efendisi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 91,
                    mapFragmentIndex = 13,
                    finishStepNumber = 91,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Efsanevi Bilge",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 91,
                    mapFragmentIndex = 14,
                    finishStepNumber = 91,
                    tutorialIsFinish = true,
                    timePeriod = 2500,
                    raceBusyLevel = 1,

                    )
            )


            else -> emptyList()
        }
    }

    // SharedPreferences işlemleri - her kullanıcıya özel (uid ile ayrı dosya)
    private const val PREFS_PREFIX = "LessonPrefs_"
    private const val KEY_LESSON_ITEMS = "lesson_items"
    private fun getKey(partId: Int) = "lesson_items_part_$partId"

    /** Giriş yapmış kullanıcı için LessonPrefs_uid, giriş yoksa LessonPrefs_guest */
    private fun getLessonPrefs(context: Context): android.content.SharedPreferences {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        return context.getSharedPreferences("$PREFS_PREFIX$uid", Context.MODE_PRIVATE)
    }

    fun saveToPreferences(context: Context, saveRemote: Boolean = true) {
        val gson = Gson()
        val json = gson.toJson(_lessonItems)
        val prefs = getLessonPrefs(context)
        val key = getKey(globalPartId)
        prefs.edit().putString(key, json).apply()
        Log.d(LOG_TAG, "saveToPreferences partId=$globalPartId key=$key local OK (items=${_lessonItems.size})")

        // Giriş yapmış kullanıcı için Firestore'a da kaydet (uygulama silinse bile geri yüklensin)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (saveRemote && uid != null) {
            Log.d(LOG_TAG, "saveToPreferences -> Firestore users/${uid.take(8)}.../lessonProgress/$globalPartId")
            firestore.collection("users").document(uid).collection(FIRESTORE_LESSON_PROGRESS)
                .document(globalPartId.toString())
                .set(mapOf("items" to json))
                .addOnSuccessListener { Log.d(LOG_TAG, "saveToPreferences Firestore SUCCESS") }
                .addOnFailureListener { e -> Log.e(LOG_TAG, "saveToPreferences Firestore FAILED", e) }
        } else {
            if (!saveRemote) Log.d(LOG_TAG, "saveToPreferences skip Firestore (saveRemote=false)")
            else if (uid == null) Log.d(LOG_TAG, "saveToPreferences skip Firestore (uid=null)")
        }
    }

    fun loadFromPreferences(context: Context): Boolean {
        val key = getKey(globalPartId)
        val prefs = getLessonPrefs(context)
        val json = prefs.getString(key, null)
        return if (json != null) {
            val gson = Gson()
            val type = object : TypeToken<List<LessonItem>>() {}.type
            _lessonItems = gson.fromJson(json, type)
            Log.d(LOG_TAG, "loadFromPreferences key=$key -> loaded ${_lessonItems.size} items from LOCAL")
            true
        } else {
            Log.d(LOG_TAG, "loadFromPreferences key=$key -> no local data")
            false
        }
    }

    /**
     * Firestore'dan ilgili part'ın ders verisini yükler (kullanıcı giriş yapmışsa).
     * LOADED: _lessonItems set edilir ve lokala yazılır.
     * NOT_FOUND: doc/field yok.
     * ERROR: ağ/izin/diğer hata.
     */
    private fun loadFromFirestore(
        context: Context,
        partId: Int,
        uid: String,
        callback: (FirestoreLoadStatus) -> Unit
    ) {
        firestore.collection("users").document(uid).collection(FIRESTORE_LESSON_PROGRESS)
            .document(partId.toString())
            .get()
            .addOnSuccessListener { doc ->
                val json = doc.getString("items")
                Log.d(LOG_TAG, "loadFromFirestore partId=$partId doc.exists=${doc.exists()} itemsFieldNull=${json == null} itemsBlank=${json.isNullOrBlank()}")
                if (!json.isNullOrBlank()) {
                    try {
                        val gson = Gson()
                        val type = object : TypeToken<List<LessonItem>>() {}.type
                        _lessonItems = gson.fromJson(json, type)
                        getLessonPrefs(context).edit().putString(getKey(partId), json).apply()
                        Log.d(LOG_TAG, "loadFromFirestore LOADED ${_lessonItems.size} items from CLOUD and wrote to local")
                        callback(FirestoreLoadStatus.LOADED)
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "loadFromFirestore parse error", e)
                        callback(FirestoreLoadStatus.ERROR)
                    }
                } else {
                    Log.d(LOG_TAG, "loadFromFirestore NOT_FOUND (no items field or empty)")
                    callback(FirestoreLoadStatus.NOT_FOUND)
                }
            }
            .addOnFailureListener { e ->
                Log.e(LOG_TAG, "loadFromFirestore FAILED", e)
                callback(FirestoreLoadStatus.ERROR)
            }
    }

    private fun waitForAuthUid(auth: FirebaseAuth, timeoutMs: Long, callback: (String?) -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        var completed = false

        fun finish(uid: String?) {
            if (completed) return
            completed = true
            callback(uid)
        }

        val existing = auth.currentUser?.uid
        if (existing != null) {
            finish(existing)
            return
        }

        lateinit var authListener: FirebaseAuth.AuthStateListener
        val timeoutRunnable = Runnable {
            auth.removeAuthStateListener(authListener)
            finish(auth.currentUser?.uid)
        }

        authListener = FirebaseAuth.AuthStateListener { a ->
            val uid = a.currentUser?.uid
            if (uid != null) {
                auth.removeAuthStateListener(authListener)
                handler.removeCallbacks(timeoutRunnable)
                finish(uid)
            }
        }

        auth.addAuthStateListener(authListener)
        handler.postDelayed(timeoutRunnable, timeoutMs)
    }

    /** Sadece şu anki kullanıcının ders verisini temizler (test / sıfırlama için) */
    fun clearCurrentUserLessonData(context: Context) {
        getLessonPrefs(context).edit().clear().apply()
    }
}
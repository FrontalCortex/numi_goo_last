// Kaynak (Android): app/src/main/java/com/example/app/BlindingLessonFragment.kt
//   (stepAnswerAlgorithm, fillIncorrectPanelAnswers, controlButtonAnim içindeki kupa deltası,
//    sayı dizisi gösterimi [startShowingSequence / onSequenceRevealStep / onShowNextNumberStep],
//    showResultPanel'deki geçiş kuralları, finishLessonAfterLastQuestion, showLessonResultFalse,
//    timeStarter, cupWay, usesRulesTablePicker, raceLessonPassed)
//
// Kör (zihinden) / yarış / kupa / günlük soru derslerinin görünümden bağımsız mantığı.
// LessonItem (`ders` paketi) yerine yalnızca kullanılan bayraklar [BlindingLessonFlags] ile alınır.
// Günlük soru deposu, enerji ve kupa Firestore yazmaları (`oyun` / `kupa`) taşınmadı.
package com.sorobit.shared.abakus

/** LessonItem'ın bu mantıkta kullanılan alanları (null'lar Android'deki gibi anlamlıdır). */
data class BlindingLessonFlags(
    /** LessonItem.type (2 = sandık). */
    val type: Int = 0,
    /** null = abaküsle cevap; true = klavyeyle (kör) cevap; false = ikisi de okunmaz (bkz. not). */
    val isBlinding: Boolean? = null,
    val isMultiplication: Boolean? = null,
    val blindingMultiplication: Boolean? = null,
    val isExtraction: Boolean? = null,
    /** Yarış dersi yoğunluk düzeyi; null = yarış değil. */
    val raceBusyLevel: Int? = null,
    /** Dizi sayıları arasındaki süre (ms); null → 1000. */
    val timePeriod: Long? = null,
    val cupWinDelta: Int? = null,
    val cupLossDelta: Int? = null,
)

/** `operations` listesindeki bir öğe (Android'de `List<Any>`). */
sealed class BlindingOperation {
    data class Math(val firstNumber: Int?, val operator: String?, val secondNumber: Int?) : BlindingOperation()
    data class Sequence(val numbers: List<Int>) : BlindingOperation()
    /** Tanınmayan öğe (ör. liste doğrudan Int'lerden oluşuyorsa tek bir Int). */
    data object Other : BlindingOperation()
}

/** Kupa deltasının yazılacağı bekleyen alan (GlobalValues.pending*CupDelta). */
enum class CupDeltaTarget {
    PENDING_BLINDING_IMPACT_CUP_DELTA,
    PENDING_IMPACT_CUP_DELTA,
    PENDING_BLINDING_EXTRACTION_CUP_DELTA,
    PENDING_EXTRACTION_CUP_DELTA,
    PENDING_BLINDING_CUP_DELTA,
    PENDING_CUP_DELTA,
}

/** Ders bitişindeki yönlendirme. */
enum class BlindingLessonOutcome {
    /** Günlük soru başarıyla tamamlandı (handleDailyQuestionLessonComplete). */
    DAILY_QUESTION_COMPLETE,
    /** Ekran kapatılır (kupa modu ya da raceBusyLevel == 0). */
    CLOSE,
    /** ChestResult / (yarışta) ChestFragment. */
    CHEST_RESULT,
    /** LessonResultFalse, isChestFailure = true. */
    CHEST_FAILURE,
    LESSON_RESULT,
    LESSON_RESULT_FALSE,
}

/** Yanlış cevap panelindeki "tamam" düğmesinden sonra ne olacağı. */
enum class BlindingWrongAnswerNext {
    /** Kupa modu: showLessonResultFalse (1 enerji harcar, kapatır). */
    CUP_RESULT_FALSE,
    /** Günlük soru: soru paneli (isWrongDailyQuestion) → handleDailyQuestionWrongAnswer. */
    DAILY_QUESTION_PANEL,
    /** Yarış: soru paneli (isWrongRaceLesson) → showLessonResultFalse. */
    RACE_QUESTION_PANEL,
    /** Sıradaki soruya geçilir (ya da sorular bittiyse soru paneli). */
    NEXT_QUESTION,
}

object BlindingLessonRules {
    const val CUP_PART_ID = 9
    const val DEFAULT_CUP_WIN_DELTA = 10
    const val DEFAULT_CUP_LOSS_DELTA = 30
    const val DEFAULT_TIME_PERIOD_MS = 1000L

    /** 4. ve 5. bölümde ders sonu "geçti" için gereken en az başarı (%). Güncel Android: `successRate < 75f` → başarısız (eskiden %50). */
    const val PART_4_5_PASS_MIN_SUCCESS_RATE = 75f

    /** Dizide iki sayı arasında ekranın boş kaldığı süre (ms). */
    const val SEQUENCE_BLANK_MS = 200L

    /** Sıfırla düğmesinden sonra dizinin yeniden başlama gecikmesi (ms). */
    const val SEQUENCE_RESTART_DELAY_MS = 200L

    fun usesRulesTablePicker(globalPartId: Int, isDailyQuestionMode: Boolean): Boolean =
        globalPartId in 4..9 || isDailyQuestionMode

    /** Süre göstergesi: yalnızca sandık dersinde; 4. ve 5. bölümlerde her zaman gizli. */
    fun isTimerVisible(lessonType: Int, globalPartId: Int): Boolean {
        var visible = lessonType == 2
        if (globalPartId == 4 || globalPartId == 5) visible = false
        return visible
    }

    /** Süre sandık dersinde ve yalnızca bir kez başlatılır. */
    fun shouldStartTimer(lessonType: Int, isTimerStarted: Boolean): Boolean =
        lessonType == 2 && !isTimerStarted

    /** Kupa modunda (9. bölüm) ilerleme çubuğu gizlenir. */
    fun isProgressBarHidden(globalPartId: Int): Boolean = globalPartId == 9

    /** 4..9. bölümlerde ipucu gizlenir. */
    fun isHintHidden(globalPartId: Int): Boolean = globalPartId in listOf(4, 5, 6, 7, 8, 9)

    fun isRacePanelLesson(flags: BlindingLessonFlags): Boolean = flags.raceBusyLevel != null

    fun raceLessonPassed(correctAnswer: Int, totalQuestions: Int): Boolean =
        totalQuestions > 0 && correctAnswer == totalQuestions

    /** Kupa modunda cevap sonrası delta: doğruysa +kazanç (varsayılan 10), yanlışsa −kayıp (varsayılan 30). */
    fun cupDelta(flags: BlindingLessonFlags, isCorrect: Boolean): Int {
        val winDelta = flags.cupWinDelta ?: DEFAULT_CUP_WIN_DELTA
        val lossDelta = flags.cupLossDelta ?: DEFAULT_CUP_LOSS_DELTA
        return if (isCorrect) winDelta else -lossDelta
    }

    /** Deltanın hangi bekleyen alana yazılacağı (öncelik sırası Android'deki if zinciriyle aynı). */
    fun cupDeltaTarget(flags: BlindingLessonFlags): CupDeltaTarget =
        if (flags.isMultiplication == true && flags.isBlinding == true) {
            CupDeltaTarget.PENDING_BLINDING_IMPACT_CUP_DELTA
        } else if (flags.isMultiplication == true) {
            CupDeltaTarget.PENDING_IMPACT_CUP_DELTA
        } else if (flags.isExtraction == true && flags.isBlinding == true) {
            CupDeltaTarget.PENDING_BLINDING_EXTRACTION_CUP_DELTA
        } else if (flags.isExtraction == true) {
            CupDeltaTarget.PENDING_EXTRACTION_CUP_DELTA
        } else if (flags.isBlinding == true) {
            CupDeltaTarget.PENDING_BLINDING_CUP_DELTA
        } else {
            CupDeltaTarget.PENDING_CUP_DELTA
        }

    /** Sonuç panelinden sonra abaküs yalnızca abaküsle cevaplanan derste (isBlinding == null) sıfırlanır. */
    fun shouldResetAbacusAfterAnswer(flags: BlindingLessonFlags): Boolean = flags.isBlinding == null

    /** Yanlış cevap panelindeki "tamam" sonrası (öncelik: kupa → günlük soru → yarış → sıradaki). */
    fun wrongAnswerNext(globalPartId: Int, isDailyQuestionMode: Boolean, flags: BlindingLessonFlags): BlindingWrongAnswerNext =
        when {
            globalPartId == 9 -> BlindingWrongAnswerNext.CUP_RESULT_FALSE
            isDailyQuestionMode -> BlindingWrongAnswerNext.DAILY_QUESTION_PANEL
            isRacePanelLesson(flags) -> BlindingWrongAnswerNext.RACE_QUESTION_PANEL
            else -> BlindingWrongAnswerNext.NEXT_QUESTION
        }

    /**
     * Android: finishLessonAfterLastQuestion.
     * [toplamPuan] = `calculateChestScore(...)` sonucu (yalnızca sandık/yarış dalında kullanılır).
     */
    fun finishLessonAfterLastQuestion(
        isDailyQuestionMode: Boolean,
        globalPartId: Int,
        flags: BlindingLessonFlags,
        correctAnswer: Int,
        totalQuestions: Int,
        toplamPuan: Int,
    ): BlindingLessonOutcome = when {
        isDailyQuestionMode -> BlindingLessonOutcome.DAILY_QUESTION_COMPLETE
        globalPartId == 9 -> BlindingLessonOutcome.CLOSE
        isRacePanelLesson(flags) || (flags.type == AbacusLessonRules.TYPE_CHEST && globalPartId !in setOf(4, 5)) -> {
            if (flags.raceBusyLevel == 0) {
                BlindingLessonOutcome.CLOSE
            } else if (toplamPuan >= AbacusLessonRules.CHEST_MIN_TOTAL_SCORE) {
                BlindingLessonOutcome.CHEST_RESULT
            } else {
                BlindingLessonOutcome.CHEST_FAILURE
            }
        }
        globalPartId in setOf(4, 5) -> {
            val successRate = AbacusLessonRules.successRate(correctAnswer, totalQuestions)
            // Başarı %75'ten düşükse başarısız ekranı (güncel Android; eskiden %50)
            if (successRate < PART_4_5_PASS_MIN_SUCCESS_RATE) BlindingLessonOutcome.LESSON_RESULT_FALSE else BlindingLessonOutcome.LESSON_RESULT
        }
        else -> BlindingLessonOutcome.LESSON_RESULT
    }

    /** Android: showLessonResultFalse'un ekran göstermeden kapattığı durumlar. */
    enum class ResultFalseAction { CUP_USE_ENERGY_AND_CLOSE, CLOSE, SHOW_RESULT_FALSE }

    /** Kupa modunda 1 enerji harcanıp kapatılır; raceBusyLevel == 0 ise yalnızca kapatılır. */
    fun lessonResultFalseAction(globalPartId: Int, flags: BlindingLessonFlags): ResultFalseAction = when {
        globalPartId == 9 -> ResultFalseAction.CUP_USE_ENERGY_AND_CLOSE
        flags.raceBusyLevel == 0 -> ResultFalseAction.CLOSE
        else -> ResultFalseAction.SHOW_RESULT_FALSE
    }

    /**
     * Kupa modunda (bölüm 9) çıkışın "terk etti" sayılıp kupa kaybettirmesi ve enerji harcatması için:
     * yarış başlamış VE bu tur için sonuç henüz gönderilmemiş olmalı. Cevabını verip ekranı geri tuşuyla
     * kapatan kullanıcı ikinci kez cezalanmaz (Android: `globalPartId == 9 && lessonStarted && !cupResultSubmitted`).
     */
    fun shouldSubmitCupQuitResult(globalPartId: Int, lessonStarted: Boolean, cupResultSubmitted: Boolean): Boolean =
        globalPartId == CUP_PART_ID && lessonStarted && !cupResultSubmitted

    /** Kupa modunun ölçüm adı (Android: cupModeName); sıra [cupDeltaTarget] zinciriyle birebir aynıdır. */
    fun cupModeName(flags: BlindingLessonFlags): String = when {
        flags.isMultiplication == true && flags.isBlinding == true -> "blinding_multiplication"
        flags.isMultiplication == true -> "multiplication"
        flags.isExtraction == true && flags.isBlinding == true -> "blinding_extraction"
        flags.isExtraction == true -> "extraction"
        flags.isBlinding == true -> "blinding"
        else -> "normal"
    }

    /**
     * Ders başarı oranı (lessonSuccessRate) telemetrisine dahil mi (Android: isLessonSuccessRateScope).
     * Günlük soru ve Kupa Modu (bölüm 9) hariç; yalnızca ders (0) ve sandık (2) adımları.
     */
    fun isLessonSuccessRateScope(isDailyQuestionMode: Boolean, globalPartId: Int, lessonType: Int): Boolean =
        !isDailyQuestionMode && globalPartId != CUP_PART_ID && AbacusLessonRules.isLessonSuccessRateType(lessonType)

    /** Günlük soru oturumu yalnızca kart açılırken kilitlenen periyot hâlâ geçerliyse ödüllendirilir. */
    fun isDailyQuestionSessionPeriodValid(sessionPeriodKey: String?, currentPeriodKey: String): Boolean {
        val sessionKey = sessionPeriodKey ?: return false
        return sessionKey == currentPeriodKey
    }
}

/**
 * Sayı dizisinin ekranda tek tek gösterilmesi (kör toplama). Zamanlayıcılar platformdadır;
 * her adım, yapılacak işi [Step] olarak döndürür.
 */
class BlindingSequencePlayback(private val timePeriod: Long?) {
    enum class Timer { SHOW_NEXT_NUMBER, SEQUENCE_REVEAL }

    data class Step(
        /** numberText'e yazılacak metin; null = dokunma. */
        val display: String? = null,
        /** Kurulacak zamanlayıcı; null = yok. */
        val schedule: Timer? = null,
        val delayMs: Long = 0L,
        /** true → gösterim bitti: kontrol düğmesi etkinleştirilir. */
        val finished: Boolean = false,
        /** true → kontrol düğmesi devre dışı bırakılır (gösterim başladı). */
        val disableControl: Boolean = false,
    )

    var isShowingSequence = false
        private set
    var currentSequenceIndex = 0
        private set
    var currentSequence: List<Int> = emptyList()
        private set

    /**
     * Sayı az önce silindi ve sıradaki 200 ms içinde gelecek mi (Android: sequenceBlankPending).
     * Atlama bu aralıkta yok sayılır; yoksa hızlı basışlarda ekran boş kalır.
     */
    var isBlankPending = false
        private set

    private val period: Long get() = timePeriod ?: BlindingLessonRules.DEFAULT_TIME_PERIOD_MS

    /**
     * Android: startShowingSequence. Bekleyen zamanlayıcılar iptal edilmelidir (stopSequencePlayback).
     * Boş dizide hiçbir şey yapılmaz (kontrol düğmesinin durumu değişmez).
     */
    fun start(sequence: List<Int>): Step {
        stop()
        isBlankPending = false
        currentSequence = sequence
        if (currentSequence.isEmpty()) return Step()

        currentSequenceIndex = 0
        isShowingSequence = true

        val display = currentSequence[0].toString()
        currentSequenceIndex = 1
        return Step(display = display, schedule = Timer.SHOW_NEXT_NUMBER, delayMs = period, disableControl = true)
    }

    /** Android: onShowNextNumberStep — ekran boşaltılır, 200 ms sonra sıradaki sayı gösterilir. */
    fun onShowNextNumberStep(): Step {
        if (!isShowingSequence) return Step()
        if (currentSequenceIndex >= currentSequence.size) {
            return finish()
        }
        isBlankPending = true
        return Step(display = "", schedule = Timer.SEQUENCE_REVEAL, delayMs = BlindingLessonRules.SEQUENCE_BLANK_MS)
    }

    /** Android: onSequenceRevealStep — sıradaki sayı gösterilir; son sayıysa gösterim biter. */
    fun onSequenceRevealStep(): Step {
        isBlankPending = false
        if (!isShowingSequence) return Step()
        if (currentSequenceIndex >= currentSequence.size) {
            return finish()
        }
        val display = currentSequence[currentSequenceIndex].toString()
        currentSequenceIndex++
        return if (currentSequenceIndex < currentSequence.size) {
            Step(display = display, schedule = Timer.SHOW_NEXT_NUMBER, delayMs = period)
        } else {
            isShowingSequence = false
            Step(display = display, finished = true)
        }
    }

    /**
     * Android: skipToNextSequenceNumber (atlama düğmesi): süresini beklemeden sıradaki sayıya geçer.
     * Kendiliğinden geçişle AYNI yoldan gider ([onShowNextNumberStep]: sayı silinir, 200 ms sonra
     * sıradaki gelir). Dönüş null ise basış yok sayılmıştır (gösterim yok ya da boşluk sürüyor);
     * null değilse platform bekleyen İKİ zamanlayıcıyı da iptal edip dönen adımı uygular.
     * Düğme animasyonu gösterim sürerken (null dönse bile) oynatılır: [isShowingSequence].
     */
    fun skip(): Step? {
        if (!isShowingSequence) return null
        if (isBlankPending) return null
        return onShowNextNumberStep()
    }

    /** Android: stopSequencePlayback (zamanlayıcıları platform iptal eder). */
    fun stop() {
        isShowingSequence = false
        isBlankPending = false
    }

    private fun finish(): Step {
        isShowingSequence = false
        return Step(finished = true)
    }
}

/**
 * BlindingLessonFragment'in cevap denetimi durumu.
 */
class BlindingLessonSession(
    val operations: List<BlindingOperation>,
    val flags: BlindingLessonFlags,
) {
    var currentIndex = 0
    var answerNumber = 0
        private set
    var controlNumber = 0
        private set
    var correctAnswer = 0
    val totalQuestions: Int get() = operations.size

    /** Yanlış panelindeki metinler (fillIncorrectPanelAnswers); çağrılmadıysa eski değer kalır. */
    var correctAnswerLabel: String = ""
        private set
    var correctAnswerText: String = ""
        private set

    private fun fillIncorrectPanelAnswers() {
        correctAnswerLabel = "Doğru cevap $answerNumber"
        correctAnswerText = "Senin cevabın $controlNumber"
    }

    /**
     * Bu tur için kupa sonucu gönderildi mi (Android: cupResultSubmitted). Cevap, çıkış düğmesi ve geri
     * tuşu aynı turun sonucunu göndermeye çalışabilir; yalnızca ilki kazanır ([claimCupResult]).
     */
    var cupResultSubmitted = false
        private set

    /** true → bu çağrı sonucu gönderme hakkını aldı; false → zaten gönderilmişti. */
    fun claimCupResult(): Boolean {
        if (cupResultSubmitted) return false
        cupResultSubmitted = true
        return true
    }

    /** Yarış sonucu analitiği bir kez (Android: cupResultLogged / logCupRaceResultOnce). */
    var cupResultLogged = false
        private set

    fun claimCupRaceLog(): Boolean {
        if (cupResultLogged) return false
        cupResultLogged = true
        return true
    }

    /** Ders bitişi bir kez başlasın (Android: lessonFinishStarted; ikinci çağrı ikinci sonuç ekranı açmasın). */
    var lessonFinishStarted = false
        private set

    fun claimLessonFinish(): Boolean {
        if (lessonFinishStarted) return false
        lessonFinishStarted = true
        return true
    }

    /** Kontrol düğmesi akışının sonunda (ve sıfırla düğmesinde) `controlNumber = 0` yapılır. */
    fun clearControlNumber() {
        controlNumber = 0
    }

    /**
     * Cevabı denetler (Android: stepAnswerAlgorithm).
     * [inputText] = sayı giriş kutusundaki metin; [abacusValue] = denetleyicinin o anki değeri;
     * [currentSequenceIndex] = dizi gösteriminin konumu (0 ise dizi hiç gösterilmemiştir → yanlış).
     *
     * Korunan Android davranışları:
     *  - Çarpım olmayan MathOperation her zaman YANLIŞ döner (panel metinleri de güncellenmez).
     *  - Dizi sorusunda `isBlinding == false` ise ne giriş ne abaküs okunur; `controlNumber`'ın
     *    eski değeri (genelde 0) karşılaştırılır.
     *  - Giriş boşsa / sayı değilse panel metni `controlNumber`'ın eski değeriyle doldurulur.
     *  - Çarpımda `firstNumber` null ise beklenen cevap 0; yalnızca `secondNumber` null ise
     *    Android çöker (burada da istisna).
     */
    fun stepAnswerAlgorithm(inputText: String, abacusValue: Int, currentSequenceIndex: Int): Boolean {
        // Dizin sınır dışıysa (son sorudan sonra gelen dokunuş) yanlış döner; Android çökmez (sürüm 21 düzeltmesi).
        if (currentIndex !in operations.indices) {
            return false
        }
        val currentOperation = operations[currentIndex]
        return when (currentOperation) {
            is BlindingOperation.Math -> {
                if (flags.blindingMultiplication == true || flags.isMultiplication == true) {
                    answerNumber = currentOperation.firstNumber?.times(currentOperation.secondNumber!!) ?: 0

                    if (flags.isMultiplication == true && flags.isBlinding != true) {
                        controlNumber = abacusValue
                        if (controlNumber == answerNumber) {
                            controlNumber = 0
                            return true
                        } else {
                            fillIncorrectPanelAnswers()
                            controlNumber = 0
                            return false
                        }
                    } else {
                        if (inputText.isNotEmpty()) {
                            try {
                                controlNumber = inputText.toInt()
                                if (controlNumber == answerNumber) {
                                    controlNumber = 0
                                    true
                                } else {
                                    fillIncorrectPanelAnswers()
                                    controlNumber = 0
                                    false
                                }
                            } catch (e: NumberFormatException) {
                                fillIncorrectPanelAnswers()
                                false
                            }
                        } else {
                            fillIncorrectPanelAnswers()
                            false
                        }
                    }
                } else {
                    // Normal MathOperation işlemi
                    false
                }
            }
            is BlindingOperation.Sequence -> {
                val sequence = currentOperation.numbers
                val result = sequence.sum()
                answerNumber = result
                if (currentSequenceIndex > 0) {
                    if (inputText.isNotEmpty() || flags.isBlinding == null) {
                        try {
                            if (flags.isBlinding == true) {
                                controlNumber = inputText.toInt()
                            }
                            if (flags.isBlinding == null) {
                                controlNumber = abacusValue
                            }
                            if (controlNumber == answerNumber) {
                                controlNumber = 0
                                true
                            } else {
                                fillIncorrectPanelAnswers()
                                controlNumber = 0
                                false
                            }
                        } catch (e: NumberFormatException) {
                            fillIncorrectPanelAnswers()
                            false
                        }
                    } else {
                        fillIncorrectPanelAnswers()
                        false
                    }
                } else {
                    fillIncorrectPanelAnswers()
                    false
                }
            }
            else -> false
        }
    }
}

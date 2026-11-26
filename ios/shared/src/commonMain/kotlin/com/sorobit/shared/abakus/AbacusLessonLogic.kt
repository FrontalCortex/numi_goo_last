// Kaynak (Android):
//  - app/src/main/java/com/example/app/AbacusFragment.kt
//      (stepAnswerAlgorithm, incorrectText, controlButtonAnim içindeki çift tıklama kuralı,
//       showResultPanel'deki sayaç/geçiş kuralları, updateProgressBar hedef ağırlığı, startTimer
//       biçimi, currentSuccessRate, showLessonResult yönlendirmesi, parseTimeToSeconds,
//       calculateChestScore, showQuestionPanel sonrası yönlendirme, optimizeBeadIdsForReverse,
//       splitTextEqually, abaküs ayar paneli hesapları, AbacusUISettings anahtarları)
//  - app/src/main/java/com/example/app/AbacusPracticeFragment.kt (onKontrolClicked)
//
// Fragment'lere gömülü, görünümden bağımsız ders mantığı. Görünüm/animasyon/Fragment geçişleri
// taşınmadı; yalnızca hesaplar ve durum geçişleri saf sınıf/fonksiyon olarak ayıklandı.
// Soru modeli (MathOperation) `matematik`/`ders` paketlerinde olduğundan burada ham alanlar
// ([AbacusOperation]) kullanılır.
package com.sorobit.shared.abakus

import kotlin.math.ceil

/** MathOperation'ın bu mantığın kullandığı alanları. */
data class AbacusOperation(
    val firstNumber: Int?,
    val operator: String?,
    val secondNumber: Int?,
)

/** AbacusFragment ve BlindingLessonFragment'in ortak saf kuralları. */
object AbacusLessonRules {
    /** LessonItem.TYPE_CHEST (sandık; süre tutulur, boncuk animasyonu 50 ms). */
    const val TYPE_CHEST = 2

    /** Kontrol düğmesine ardışık basışlar arasında gereken en az süre (ms). */
    const val CONTROL_DEBOUNCE_MS = 100L

    /** Doğru / yanlış panelinden sonra kontrol düğmesinin yeniden etkinleşme gecikmesi (ms). */
    const val CONTROL_REENABLE_DELAY_CORRECT_MS = 200L
    const val CONTROL_REENABLE_DELAY_INCORRECT_MS = 700L

    /** Sandık sonucu için gereken en az toplam puan. */
    const val CHEST_MIN_TOTAL_SCORE = 500

    /** Kupa süresi hiçbir yerden bulunamazsa kullanılan yedek (sn). */
    const val WORST_CUP_TIME_FALLBACK = 240

    const val SOUND_CORRECT = "correct_answer_sound"
    const val SOUND_CORRECT_VOLUME = 0.3f
    const val SOUND_INCORRECT = "incorrect_answer_sound"

    /** İlerleme çubuğu renkleri. */
    const val PROGRESS_COLOR_GREEN = 0xFF7CFC00.toInt()
    const val PROGRESS_COLOR_YELLOW = 0xFFFFFF99.toInt()
    const val PROGRESS_COLOR_DARK_RED = 0xFF990000.toInt()
    const val PROGRESS_ANIMATION_MS = 700L

    /** Boncuk animasyon süresi: sandık dersinde 50 ms, diğerlerinde 300 ms. */
    fun beadAnimationDurationMs(lessonType: Int): Long = if (lessonType == 2) 50L else 300L

    /** Android: `String.format("%d:%02d", minutes, secs)`. */
    fun formatTimer(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return "$minutes:${secs.toString().padStart(2, '0')}"
    }

    /** Başarı yüzdesi (0..100). */
    fun successRate(correctAnswer: Int, totalQuestions: Int): Float {
        return if (totalQuestions > 0) {
            (correctAnswer.toFloat() / totalQuestions.toFloat()) * 100
        } else {
            0f
        }
    }

    fun dersPuani(successRate: Float): Int = (successRate * 5f).toInt()

    /** "d:ss" ya da "s:dd:ss" → saniye; başka biçim → 0; sayı olmayan parça 0 sayılır. */
    fun parseTimeToSeconds(value: String): Int {
        val parts = value.split(":").map { it.trim() }
        return when (parts.size) {
            2 -> {
                val minutes = parts[0].toIntOrNull() ?: 0
                val seconds = parts[1].toIntOrNull() ?: 0
                minutes * 60 + seconds
            }
            3 -> {
                val hours = parts[0].toIntOrNull() ?: 0
                val minutes = parts[1].toIntOrNull() ?: 0
                val seconds = parts[2].toIntOrNull() ?: 0
                hours * 3600 + minutes * 60 + seconds
            }
            else -> 0
        }
    }

    /**
     * Android: resolveWorstCupTimeFallback. Önce geçerli ders öğesinin süresi, sonra şablonunki
     * (ikisi de > 0 olmalı), yoksa 240.
     */
    fun resolveWorstCupTimeFallback(fromCurrent: Int?, fromTemplate: Int?): Int {
        if (fromCurrent != null && fromCurrent > 0) return fromCurrent
        if (fromTemplate != null && fromTemplate > 0) return fromTemplate
        return WORST_CUP_TIME_FALLBACK
    }

    /**
     * Sandık puanı: çarpan = max(1, 4 − 3·süre/kupaSüresi); toplam = ceil(dersPuanı × çarpan).
     * Dönüş: (carpan, toplamPuan).
     */
    fun calculateChestScore(successRate: Float, timeStr: String, worstCupTime: Int): Pair<Float, Int> {
        val dersPuani = (successRate * 5f).toInt()
        val targetTimeSec = parseTimeToSeconds(timeStr)

        val carpan = if (worstCupTime <= 0) 1f else {
            val rawCarpan = 4f - ((targetTimeSec * 3f) / worstCupTime.toFloat())
            rawCarpan.coerceAtLeast(1f)
        }
        val toplamPuan = ceil(dersPuani * carpan).toInt()
        return Pair(carpan, toplamPuan)
    }

    /** İlerleme çubuğunun animasyon sonu ağırlığı. */
    fun progressEndWeight(currentIndex: Int, operationCount: Int): Float =
        (currentIndex + 1).toFloat() / operationCount

    /** Son soruda ve tüm cevaplar doğruysa konfeti (lottie) oynatılır. */
    fun shouldPlayPerfectAnimation(currentIndex: Int, operationCount: Int, correctAnswer: Int, totalQuestions: Int): Boolean =
        currentIndex == operationCount - 1 && correctAnswer == totalQuestions

    /** Metni kelime sayısına göre iki satıra böler (ilk satır kelimelerin alt yarısı). */
    fun splitTextEqually(text: String): String {
        val words = text.split(" ")
        val totalWords = words.size

        val firstLine = words.take(totalWords / 2).joinToString(" ")
        val secondLine = words.drop(totalWords / 2).joinToString(" ")

        return "$firstLine\n$secondLine"
    }

    /**
     * Rehberde geri dönüş için boncuk kimliklerini sadeleştirir: aynı çubuğun alt boncuklarından
     * yalnızca en yüksek numaralı olan kalır (o zaten diğerlerini de hareket ettirir).
     * Sıra: önce üst/diğer boncuklar (geliş sırasıyla), sonra çubuk grupları (ilk görülme sırasıyla).
     */
    fun optimizeBeadIdsForReverse(beadIds: List<String>): List<String> {
        val optimized = mutableListOf<String>()
        val rodGroups = mutableMapOf<String, MutableList<String>>()

        beadIds.forEach { beadId ->
            when {
                beadId.startsWith("rod0BottomBead") -> rodGroups.getOrPut("rod0") { mutableListOf() }.add(beadId)
                beadId.startsWith("rod1BottomBead") -> rodGroups.getOrPut("rod1") { mutableListOf() }.add(beadId)
                beadId.startsWith("rod2BottomBead") -> rodGroups.getOrPut("rod2") { mutableListOf() }.add(beadId)
                beadId.startsWith("rod3BottomBead") -> rodGroups.getOrPut("rod3") { mutableListOf() }.add(beadId)
                beadId.startsWith("rod4BottomBead") -> rodGroups.getOrPut("rod4") { mutableListOf() }.add(beadId)
                else -> optimized.add(beadId)
            }
        }

        rodGroups.forEach { (_, beads) ->
            if (beads.isNotEmpty()) {
                val sortedBeads = beads.sortedByDescending { it ->
                    when {
                        it.contains("Bead4") -> 4
                        it.contains("Bead3") -> 3
                        it.contains("Bead2") -> 2
                        it.contains("Bead1") -> 1
                        else -> 0
                    }
                }
                optimized.add(sortedBeads.first())
            }
        }

        return optimized
    }

    /** Çarpım tablosu yerleşim adı (R.layout.multiplication_table_N); 1..9 dışı → 1. */
    fun multiplicationLayoutForDigit(digit: Int): String = when (digit) {
        in 1..9 -> "multiplication_table_$digit"
        else -> "multiplication_table_1"
    }
}

/** Ders sonu yönlendirmesi. */
enum class AbacusLessonOutcome {
    /** LessonResult ekranı. */
    LESSON_RESULT,
    /** LessonResultFalse ekranı (isChestFailure = false). */
    LESSON_RESULT_FALSE,
    /** ChestResult ekranı. */
    CHEST_RESULT,
    /** LessonResultFalse ekranı, isChestFailure = true. */
    CHEST_FAILURE,
}

/**
 * AbacusFragment'in soru akışı durumu (currentIndex / correctAnswer / answerNumber ...).
 */
class AbacusLessonSession(
    val operations: List<AbacusOperation>,
    /** LessonItem.type */
    val lessonType: Int,
) {
    var currentIndex = 0
        private set
    var answerNumber = 0
        private set
    var controlNumber = 0
        private set
    var correctAnswer = 0
        private set
    val totalQuestions: Int = operations.size
    private var lastClickTime = 0L

    var lessonStarted = false
        private set
    var isTimerStarted = false
        private set
    var seconds = 0
        private set
    var currentTime: String = "0:00"
        private set

    /** Yanlış panelindeki metinler (incorrectText). */
    var correctAnswerText: String = ""
        private set
    var correctAnswerLabel: String = ""
        private set

    /** Sandık dersinde "başla" düğmesi gösterilir; diğerlerinde ders hemen başlar. */
    val needsStartButton: Boolean get() = lessonType == 2

    /** Süre göstergesi yalnızca sandık dersinde görünür. */
    val isTimerVisible: Boolean get() = lessonType == 2

    /**
     * Android: setupStartButton (sandık değilse) ya da başla düğmesine basılması.
     * Zaten başladıysa false döner (ikinci basış yok sayılır).
     */
    fun start(): Boolean {
        if (lessonStarted) return false
        lessonStarted = true
        timeStarter()
        return true
    }

    private fun timeStarter() {
        if (lessonType == 2) {
            if (!isTimerStarted) {
                isTimerStarted = true
            }
        }
    }

    /**
     * Zamanlayıcının 1 sn'lik adımı. DİKKAT (Android davranışı): ilk adım hemen (`handler.post`)
     * çalışır; yani süre başlar başlamaz "0:01" gösterir.
     */
    fun onTimerTick(): String {
        seconds++
        currentTime = AbacusLessonRules.formatTimer(seconds)
        return currentTime
    }

    fun stopTimer() {
        if (!isTimerStarted) return
        isTimerStarted = false
    }

    /** O anki soru (dizin sınır dışındaysa null — Android'de metinler değişmez). */
    fun currentOperation(): AbacusOperation? = operations.getOrNull(currentIndex)

    /**
     * Cevabı denetler (Android: stepAnswerAlgorithm). [abacusValue] = denetleyicinin o anki değeri.
     *
     * - İki sayı da null ise beklenen cevap `operator` metninin sayı değeridir (yoksa 0).
     * - "+", "-", "x" → işlem sonucu.
     * - DİKKAT (Android davranışı): "/" için beklenen cevap HER ZAMAN 1, bilinmeyen işleç için 0.
     * - Android'de çöken durumlar burada da istisna atar: `operator` sayı değilken iki sayı null
     *   (NumberFormatException), sayılardan yalnızca biri null (NullPointerException).
     */
    fun stepAnswerAlgorithm(abacusValue: Int): Boolean {
        if (operations.isEmpty()) {
            return false
        }

        val op = operations[currentIndex]
        if (op.secondNumber == null && op.firstNumber == null) {
            answerNumber = op.operator?.toInt() ?: 0
        } else {
            val result = when (op.operator) {
                "+" -> op.firstNumber?.plus(op.secondNumber!!)
                "-" -> op.firstNumber?.minus(op.secondNumber!!)
                "x" -> op.firstNumber?.times(op.secondNumber!!)
                "/" -> 1
                else -> 0
            }
            answerNumber = result!!
        }
        controlNumber = abacusValue
        return if (controlNumber == answerNumber) {
            controlNumber = 0
            true
        } else {
            incorrectText()
            controlNumber = 0
            false
        }
    }

    private fun incorrectText() {
        correctAnswerText = "Senin cevabın $controlNumber"
        correctAnswerLabel = "Doğru cevap $answerNumber"
    }

    /** Kontrol düğmesi sonucu. */
    sealed class ControlResult {
        /** Son basıştan beri 100 ms geçmedi; hiçbir şey yapılmaz. */
        data object Ignored : ControlResult()
        /** Çevrimdışı: çevrimdışı ekranı gösterilir (cevap denetlenmez). */
        data object Offline : ControlResult()
        data class Answered(
            val isCorrect: Boolean,
            /** Son soruda süre hemen durdurulur. */
            val timerStopped: Boolean,
            /** İlerleme çubuğunun hedef ağırlığı. */
            val progressEndWeight: Float,
            /** Tüm cevaplar doğru + son soru: konfeti. */
            val playPerfectAnimation: Boolean,
        ) : ControlResult()
    }

    /**
     * Kontrol düğmesinin bırakılması (ACTION_UP/CANCEL). Android: controlButtonAnim + showResultPanel
     * başındaki sayaç güncellemesi.
     * DİKKAT (Android davranışı): 100 ms kuralı çevrimdışıyken de `lastClickTime`'ı günceller.
     */
    fun onControlPressed(nowMs: Long, isOnline: Boolean, abacusValue: Int): ControlResult {
        if (nowMs - lastClickTime < AbacusLessonRules.CONTROL_DEBOUNCE_MS) return ControlResult.Ignored
        lastClickTime = nowMs
        if (!isOnline) return ControlResult.Offline

        val isCorrect = stepAnswerAlgorithm(abacusValue)
        var timerStopped = false
        if (currentIndex == operations.size - 1 && isTimerStarted) {
            stopTimer()
            timerStopped = true
        }
        val endWeight = AbacusLessonRules.progressEndWeight(currentIndex, operations.size)
        var perfect = false
        if (isCorrect) {
            correctAnswer++
            perfect = AbacusLessonRules.shouldPlayPerfectAnimation(currentIndex, operations.size, correctAnswer, totalQuestions)
        }
        controlNumber = 0
        return ControlResult.Answered(isCorrect, timerStopped, endWeight, perfect)
    }

    /**
     * Sonuç panelindeki "devam"/"tamam" düğmesi: sıradaki soruya geçer.
     * true → gösterilecek soru var; false → sorular bitti (ders sonu soru paneli açılır).
     */
    fun onContinue(): Boolean {
        currentIndex++
        return currentIndex <= operations.size - 1
    }

    fun currentSuccessRate(): Float = AbacusLessonRules.successRate(correctAnswer, totalQuestions)

    fun dersPuani(): Int = AbacusLessonRules.dersPuani(currentSuccessRate())

    /**
     * Ders sonu soru paneli kapandıktan sonraki yönlendirme (Android: showQuestionPanel'deki
     * sonuç dinleyicisi + showLessonResult).
     * - Sandık: toplam puan < 500 → başarısız (isChestFailure), değilse sandık sonucu.
     * - Diğer: başarı < %10 → başarısız ekranı, değilse ders sonucu.
     */
    fun outcome(worstCupTime: Int): AbacusLessonOutcome {
        val successRate = currentSuccessRate()
        return if (lessonType == AbacusLessonRules.TYPE_CHEST) {
            val (_, toplamPuan) = AbacusLessonRules.calculateChestScore(successRate, currentTime, worstCupTime)
            if (toplamPuan < AbacusLessonRules.CHEST_MIN_TOTAL_SCORE) AbacusLessonOutcome.CHEST_FAILURE else AbacusLessonOutcome.CHEST_RESULT
        } else {
            if (successRate < 10) AbacusLessonOutcome.LESSON_RESULT_FALSE else AbacusLessonOutcome.LESSON_RESULT
        }
    }
}

/** AbacusPracticeFragment.onKontrolClicked. */
object AbacusPracticeLogic {
    const val DEFAULT_OPERATOR = "+"

    data class KontrolResult(
        val isCorrect: Boolean,
        val expected: Long,
        val abacusValue: Long,
    ) {
        val correctAnswerLabel: String get() = "Doğru cevap: $expected"
        val correctAnswerText: String get() = "Senin cevabın: $abacusValue"
    }

    /**
     * Girilen iki metni (boş/geçersiz → 0) seçili işleçle hesaplar ve abaküs değeriyle karşılaştırır.
     * Bilinmeyen işleç toplama sayılır. Sonuç Long'dur (taşma olmaz); abaküs en çok 99999
     * gösterebildiği için daha büyük/negatif sonuçlar hiçbir zaman "doğru" olamaz.
     */
    fun kontrol(firstText: String?, secondText: String?, selectedOperator: String, abacusValue: Int): KontrolResult {
        val a = firstText?.trim().orEmpty().toIntOrNull() ?: 0
        val b = secondText?.trim().orEmpty().toIntOrNull() ?: 0

        val expected: Long = when (selectedOperator) {
            "+" -> a.toLong() + b.toLong()
            "-" -> a.toLong() - b.toLong()
            "x" -> a.toLong() * b.toLong()
            else -> a.toLong() + b.toLong()
        }

        val value = abacusValue.toLong()
        return KontrolResult(isCorrect = expected == value, expected = expected, abacusValue = value)
    }
}

/**
 * AbacusFragment / BlindingLessonFragment'teki abaküs ayar panelinin (ölçek / boşluk) saf hesapları.
 */
object AbacusUiSettings {
    const val PREFS_NAME = "AbacusUISettings"
    const val KEY_ABACUS_SCALE_X = "savedAbacusScaleX"
    const val KEY_ABACUS_SCALE_Y = "savedAbacusScaleY"
    const val KEY_ABACUS_MARGIN_BOTTOM_DP = "savedAbacusMarginBottomDp"
    const val KEY_BEAD_SCALE_X = "savedBeadScaleX"
    const val KEY_BEAD_SCALE_Y = "savedBeadScaleY"
    const val KEY_BEAD_MARGIN_TOP_DP = "savedBeadMarginTopDp"
    const val KEY_BEAD_MARGIN_BOTTOM_DP = "savedBeadMarginBottomDp"

    const val DEFAULT_SCALE = 1.0f
    const val DEFAULT_MARGIN_DP = 0f

    const val MAX_PROGRESS_SCALE = 100
    const val MAX_PROGRESS_ABACUS_MARGIN_BOTTOM = 100
    const val MAX_PROGRESS_BEAD_MARGIN_TOP = 30
    const val MAX_PROGRESS_BEAD_MARGIN_BOTTOM = 50

    /** Kaydırıcı (0..100) → ölçek (0.5x..2.0x). */
    fun scaleFromProgress(progress: Int): Float = 0.5f + (progress / 100f) * 1.5f

    /** Ölçek → kaydırıcı konumu (0..100'e kırpılır). 1.0x → 33. */
    fun progressFromScale(scale: Float): Int = ((scale - 0.5f) * (100f / 1.5f)).toInt().coerceIn(0, 100)

    fun progressFromMarginDp(marginDp: Float, max: Int): Int = marginDp.toInt().coerceIn(0, max)

    /** Alt boncukların taban alt boşluğu (dp): bottom1 → 82, bottom2 → 58, bottom3 → 34, bottom4 → 10. */
    fun baseBottomMarginDp(idName: String): Float = when {
        idName.contains("bottom1") -> 82f
        idName.contains("bottom2") -> 58f
        idName.contains("bottom3") -> 34f
        idName.contains("bottom4") -> 10f
        else -> 0f
    }

    /** Alt boncuğun alt boşluğu (px): `((taban + ek) * yoğunluk).toInt()`. */
    fun beadBottomMarginPx(idName: String, marginBottomDp: Float, density: Float): Int =
        ((baseBottomMarginDp(idName) + marginBottomDp) * density).toInt()

    /** Üst boncuğun üst boşluğu (px). */
    fun beadTopMarginPx(marginTopDp: Float, density: Float): Int = (marginTopDp * density).toInt()
}

// Kaynak (Android): app/src/main/java/com/example/app/TasksFragment.kt — "Kupa Yolu" paneli ve zorluk paneli:
//  - showCupPathPanel / displayCupPathDialog / updateExistingCupPathDialog: altı kart, kartın kilidi
//    (ilgili part'ın SON sandık dersi bitmiş mi), gösterilen skor (`coerceAtLeast(0)`), 500 ms tıklama koruması
//  - setupCupPathCard: açık kartın rengi (1,4 turuncu; 2,5 yeşil; 3,6 mavi), kilitli kartın renkleri
//  - showCupDifficultyPanel: başlık, kaydırıcı (0..4 → %0..%100), 3000+ kupada basamak seçimi, enerji kontrolü
//  - launchCupModeLesson: CupRuleEngine.buildLessonItem + çarpma modunun ezdiği alanlar + sayı listesi üretimi
//  - applyExtractionNegation, consumePendingCupDelta (yeni skor), animateCupScore (delta metni/rengi/süresi)
// ve ProfileFragment.kt — loadCupPathScores (profildeki altı kupa kartının başlığı, animasyonu, alanı).
// Görünüm, Lottie, Firestore ve gezinme taşınmadı. R.color.x -> kaynak adı String'i.
package com.sorobit.shared.kupa

import com.sorobit.shared.matematik.GlobalValues
import kotlin.random.Random

/** Kupa Yolu panelindeki bir kart (Android `card1View` … `card6View`). */
data class CupPathCard(
    /** 1..6 — Android'deki kart sırası (panelde yukarıdan aşağı). */
    val cardNumber: Int,
    /** panel_cup_path.xml başlığı. */
    val title: String,
    /** ProfileFragment.loadCupPathScores başlığı ("Toplama" / "Çıkarma" / "Çarpma"). */
    val profileTitle: String,
    /** Profil kartında "abaküs yasak" simgesi gösterilir mi (körleme kartları). */
    val showForbiddenIcon: Boolean,
    /** Lottie dosyası (Android assets). */
    val animFile: String,
    /** Kart açıkken arka plan rengi (R.color adı). */
    val activeColorRes: String,
    /** `users/{uid}/cupWayProgress/progress` dokümanındaki alan. */
    val field: String,
    /** Kartı açan part: bu part'ın son sandık (TYPE_CHEST) dersinin `stepIsFinish` değeri true olmalı. */
    val unlockPartId: Int,
    val isBlinding: Boolean,
    val isExtraction: Boolean,
    val isMultiplication: Boolean,
    val kind: CupPathKind,
)

/** [CupPathLogic.buildCupLesson] sonucu: kupa dersini başlatmak için gereken her şey (yalın alanlar). */
data class CupLessonLaunch(
    val cardNumber: Int,
    /** Android: `GlobalValues.currentLessonOldCupScore = cupScore`. */
    val cupScore: Int,
    val timePeriod: Long,
    val cupDigitSize: Int,
    val cupNumberCount: Int,
    val cupDifficultyLevel: Int,
    val cupWinDelta: Int,
    val cupLossDelta: Int,
    /** Android LessonItem'de `Boolean?` (true ya da null); burada null = false. */
    val isBlinding: Boolean,
    val isExtraction: Boolean,
    val isMultiplication: Boolean,
    /** Çarpma dışı modlarda gösterilecek sayılar (çıkarmalı modda negatifler dahil); çarpmada boş. */
    val numbers: List<Int>,
    /** Çarpma modunda soru (`first x second`); diğer modlarda 0. */
    val multiplicationFirst: Int,
    val multiplicationSecond: Int,
)

object CupPathLogic {

    /** Android `LessonItem.TYPE_CHEST`. */
    const val LESSON_ITEM_TYPE_CHEST = 2

    /** Kart tıklamaları arasındaki en kısa süre (Android: `now - lastCardClickTime < 500`). */
    const val CARD_CLICK_DEBOUNCE_MS = 500L

    const val PANEL_TITLE = "Kupa Yolu"

    /** Kilitli kartın arka planı ve yazı/simge rengi (R.color adları). */
    const val LOCKED_CARD_COLOR_RES = "background_color"
    const val LOCKED_CONTENT_COLOR_RES = "lesson_locked"

    val CARDS: List<CupPathCard> = listOf(
        CupPathCard(
            1, "Toplama Kupa Yolu", "Toplama", false, "dinosaur_anim.json", "lesson_header_orange",
            AbacusCupRepository.FIELD, 1, isBlinding = false, isExtraction = false, isMultiplication = false,
            kind = CupPathKind.ADDITION,
        ),
        CupPathCard(
            2, "Çıkarma Kupa Yolu", "Çıkarma", false, "crocodile_anim.json", "lesson_header_green",
            ExtractionCupRepository.FIELD, 2, isBlinding = false, isExtraction = true, isMultiplication = false,
            kind = CupPathKind.EXTRACTION,
        ),
        CupPathCard(
            3, "Çarpma Kupa Yolu", "Çarpma", false, "goat_anim.json", "lesson_header_blue",
            ImpactCupRepository.FIELD, 3, isBlinding = false, isExtraction = false, isMultiplication = true,
            kind = CupPathKind.IMPACT,
        ),
        CupPathCard(
            4, "Toplama Kupa Yolu - Körleme", "Toplama", true, "eagle_anim.json", "lesson_header_orange",
            BlindingAdditionCupRepository.FIELD, 4, isBlinding = true, isExtraction = false, isMultiplication = false,
            kind = CupPathKind.BLINDING_ADDITION,
        ),
        CupPathCard(
            5, "Çıkarma Kupa Yolu - Körleme", "Çıkarma", true, "fly_anim.json", "lesson_header_green",
            BlindingExtractionCupRepository.FIELD, 5, isBlinding = true, isExtraction = true, isMultiplication = false,
            kind = CupPathKind.BLINDING_EXTRACTION,
        ),
        CupPathCard(
            6, "Çarpma Kupa Yolu - Körleme", "Çarpma", true, "turtle_anim.json", "lesson_header_blue",
            BlindingImpactCupRepository.FIELD, 6, isBlinding = true, isExtraction = false, isMultiplication = true,
            kind = CupPathKind.BLINDING_IMPACT,
        ),
    )

    /** [cardNumber] 1..6 dışındaysa en yakın kart döner (Swift tarafında istisna oluşmasın diye). */
    fun card(cardNumber: Int): CupPathCard = CARDS[cardNumber.coerceIn(1, CARDS.size) - 1]

    /**
     * Android: `items.lastOrNull { it.type == LessonItem.TYPE_CHEST }?.stepIsFinish == true`.
     * İki liste aynı sıradadır (kartın [CupPathCard.unlockPartId] part'ının dersleri).
     */
    fun isCardActive(lessonItemTypes: List<Int>, stepIsFinish: List<Boolean>): Boolean {
        val idx = lessonItemTypes.indexOfLast { it == LESSON_ITEM_TYPE_CHEST }
        if (idx < 0) return false
        return stepIsFinish.getOrNull(idx) == true
    }

    /**
     * Android (güncel) `cupPartActive`: onaylı öğretmende hepsi açık (dersleri tek seferde açılıyor ama sandıkları
     * bitmiş sayılmıyor; bu kural olmasa kupa yolu öğretmene hiç açılmazdı), değilse [isCardActive] kuralı.
     */
    fun isCardActive(lessonItemTypes: List<Int>, stepIsFinish: List<Boolean>, isApprovedTeacher: Boolean): Boolean =
        isApprovedTeacher || isCardActive(lessonItemTypes, stepIsFinish)

    /** Kilitli kartın açılması için bitirilmesi gereken bölüm, kart sırasıyla (1..6). */
    val CARD_UNLOCK_REQUIREMENTS: List<String> = listOf(
        "Abaküsün Temeli ve Toplama",
        "Abaküste Çıkarma",
        "Abaküste Çarpma",
        "Körleme Toplama",
        "Körleme Çıkarma",
        "Körleme Çarpma",
    )

    /** Kilitli karta dokunulunca uyarı ("%1$s" bölüm adıyla dolar). */
    fun lockedCardHint(cardNumber: Int): String =
        "Açmak için \"" + CARD_UNLOCK_REQUIREMENTS[cardNumber.coerceIn(1, CARD_UNLOCK_REQUIREMENTS.size) - 1] +
            "\" bölümünün son ünite maratonunu bitir."

    /** Kupa yolu paneli ikinci kez açılmasın diye bekleme (Android `CUP_PATH_PANEL_REOPEN_GUARD_MS`). */
    const val PANEL_REOPEN_GUARD_MS = 600L

    /** Kartta gösterilen skor (Android: `score.coerceAtLeast(0).toString()`). */
    fun shownScore(score: Int): Int = score.coerceAtLeast(0)

    // ---------------------------------------------------------------- zorluk paneli

    /** Kaydırıcının en büyük değeri: 5 durak (0, 25, 50, 75, 100). */
    const val DIFFICULTY_SEEKBAR_MAX = 4
    const val DIFFICULTY_MESSAGE = "Seviyeyi Ayarla"
    const val DIGIT_LABEL = "Basamak Sayısı:"
    const val DIGIT_RANDOM_LABEL = "Rastgele"
    const val START_LABEL = "BAŞLAT"

    /** Çarpma modunda başlık değişir; diğerlerinde layout'taki "Zorluk Seviyesi" kalır. */
    fun difficultyTitle(isMultiplication: Boolean): String =
        if (isMultiplication) "Çarpma Kupa Yolu" else "Zorluk Seviyesi"

    /** Çarpma modunda kaydırıcı, etiketi ve "Seviyeyi Ayarla" metni gizlenir. */
    fun showsDifficultySlider(isMultiplication: Boolean): Boolean = !isMultiplication

    /** Kaydırıcı etiketindeki sayı (Android `updateThumbLabel`: `progress * 25`). */
    fun difficultyPercent(progress: Int): Int = progress * 25

    /**
     * Basamak seçimi listesi: yalnızca çarpma dışı modda ve 3000+ kupada, [CupRuleEngine.getAvailableDigits]
     * boş değilse gösterilir. Boş liste = seçim gösterilmez.
     */
    fun digitOptions(cupScore: Int, isBlinding: Boolean, isMultiplication: Boolean): List<Int> {
        if (isMultiplication || cupScore < 3000) return emptyList()
        return CupRuleEngine.getAvailableDigits(cupScore, isBlinding)
    }

    /** Android: `"$it Basamak"`. */
    fun digitOptionLabel(digits: Int): String = "$digits Basamak"

    /** Android: sonsuz enerji yoksa ve enerji 0 ise ders başlamaz, mağaza açılır. */
    fun canStartLesson(isInfiniteEnergy: Boolean, currentEnergy: Int): Boolean =
        isInfiniteEnergy || currentEnergy > 0

    // ---------------------------------------------------------------- ders başlatma

    /** Çarpma modunun ezdiği alanlar (Android `launchCupModeLesson`). */
    const val MULTIPLICATION_NUMBER_COUNT = 1
    const val MULTIPLICATION_WIN_DELTA = 30
    const val MULTIPLICATION_LOSS_DELTA = 20

    /**
     * Android `showCupDifficultyPanel` başlat düğmesi + `launchCupModeLesson`.
     *
     * @param difficultyLevel kaydırıcı değeri (0..4); çarpma modunda yok sayılır (0).
     * @param forcedDigitSize seçilen basamak sayısı; 0 = "Rastgele" (Android'de null).
     */
    fun buildCupLesson(cardNumber: Int, cupScore: Int, difficultyLevel: Int, forcedDigitSize: Int): CupLessonLaunch =
        buildCupLesson(cardNumber, cupScore, difficultyLevel, forcedDigitSize, Random.Default)

    internal fun buildCupLesson(
        cardNumber: Int,
        cupScore: Int,
        difficultyLevel: Int,
        forcedDigitSize: Int,
        random: Random,
    ): CupLessonLaunch {
        val card = card(cardNumber)
        // Skor Android'de hiçbir zaman negatif değildir (yazarken 0'a kırpılır); kural tablosu negatifte istisna atar.
        val safeScore = cupScore.coerceAtLeast(0)
        val item = CupRuleEngine.buildLessonItem(
            cupScore = safeScore,
            difficultyLevel = if (card.isMultiplication) 0 else difficultyLevel,
            isBlinding = card.isBlinding,
            isExtraction = card.isExtraction,
            forcedDigitSize = forcedDigitSize.takeIf { it > 0 },
            random = random,
        )
        if (card.isMultiplication) {
            val op = CupRuleEngine.generateMultiplicationQuestion(safeScore, card.isBlinding, random)
            return CupLessonLaunch(
                cardNumber = card.cardNumber,
                cupScore = cupScore,
                timePeriod = item.timePeriod,
                cupDigitSize = item.cupDigitSize,
                cupNumberCount = MULTIPLICATION_NUMBER_COUNT,
                cupDifficultyLevel = item.cupDifficultyLevel,
                cupWinDelta = MULTIPLICATION_WIN_DELTA,
                cupLossDelta = MULTIPLICATION_LOSS_DELTA,
                isBlinding = card.isBlinding,
                isExtraction = card.isExtraction,
                isMultiplication = true,
                numbers = emptyList(),
                multiplicationFirst = op.firstNumber ?: 0,
                multiplicationSecond = op.secondNumber ?: 0,
            )
        }
        val numbers = GlobalValues.randomUniqueNumberStrings(item.cupDigitSize, item.cupNumberCount)
            .map { it.toInt() }
            .let { if (card.isExtraction) applyExtractionNegation(it) else it }
        return CupLessonLaunch(
            cardNumber = card.cardNumber,
            cupScore = cupScore,
            timePeriod = item.timePeriod,
            cupDigitSize = item.cupDigitSize,
            cupNumberCount = item.cupNumberCount,
            cupDifficultyLevel = item.cupDifficultyLevel,
            cupWinDelta = item.cupWinDelta,
            cupLossDelta = item.cupLossDelta,
            isBlinding = card.isBlinding,
            isExtraction = card.isExtraction,
            isMultiplication = false,
            numbers = numbers,
            multiplicationFirst = 0,
            multiplicationSecond = 0,
        )
    }

    /**
     * Çıkarmalı toplama modu için sayı listesine negatif dönüşümü uygular.
     * İlk sayı her zaman pozitiftir. Sonraki her sayı için: running_total > number ise sayı negatif yapılır.
     * Bu kural sayesinde toplam asla negatife düşmez.
     */
    fun applyExtractionNegation(numbers: List<Int>): List<Int> {
        val result = mutableListOf<Int>()
        var runningTotal = 0
        for (n in numbers) {
            if (result.isNotEmpty() && runningTotal > n) {
                result.add(-n)
                runningTotal -= n
            } else {
                result.add(n)
                runningTotal += n
            }
        }
        return result
    }

    // ---------------------------------------------------------------- ders dönüşü

    /** Skor sayacı animasyonunun süresi (Android `animateCupScore`: `duration = 1000`). */
    const val SCORE_ANIMATION_MS = 1000L

    /** Android `consumePendingCupDelta`: `(currentLessonOldCupScore ?: 0) + delta` (0'a kırpılmaz). */
    fun scoreAfterLesson(oldScore: Int, delta: Int): Int = oldScore + delta

    /** Android `updateExistingCupPathDialog`: sayaç `(updatedScore - delta).coerceAtLeast(0)` değerinden başlar. */
    fun scoreAnimationStart(updatedScore: Int, delta: Int): Int = (updatedScore - delta).coerceAtLeast(0)

    /** Sayaç `updatedScore.coerceAtLeast(0)` değerinde biter. */
    fun scoreAnimationEnd(updatedScore: Int): Int = updatedScore.coerceAtLeast(0)

    /** Android: yalnızca `delta != 0` ise animasyon oynar; aksi halde skor doğrudan yazılır. */
    fun shouldAnimateScore(delta: Int): Boolean = delta != 0

    /** Android `animateCupScore`: `if (delta > 0) "+" + delta else delta.toString()`. */
    fun deltaText(delta: Int): String = if (delta > 0) "+$delta" else delta.toString()

    /** Delta metninin rengi: artışta #4CAF50, düşüşte #F44336 (0xRRGGBB). */
    fun deltaColorRgb(delta: Int): Int = if (delta > 0) 0x4CAF50 else 0xF44336
}

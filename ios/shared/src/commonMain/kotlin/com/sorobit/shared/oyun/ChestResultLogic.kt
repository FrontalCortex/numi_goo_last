// Kaynak (yalnızca gömülü saf hesaplar; görünüm/animasyon kodu taşınmadı):
//  - ChestResult.kt: record (rekor kırma görevi bayrağı), updateScoreProgressBar (oran + renk geçişi
//    seçimi), resolveScoreCap, resolveCupPoint2Threshold, placeStarMarker (oran), maybeTriggerStarMarkers,
//    playStarSoundForTier (ton), parseTimeToSeconds, formatSeconds, animasyon dosya listesi
//  - ChestFragment.kt: changeCupIcon, starCountForChestIcon, lessonProgressKey
//  - ChestTypeProgressHelper.kt (tamamı): resolvedChestIcon, starCountForChestIcon,
//    shouldIncrementKarateForFirstThreeStars
// Farklar: R.drawable.* → kaynak adı (String); `globalPartId` genel değişkeni → parametre;
// LessonManager / GlobalLessonData okumaları → parametre; `String.format` → elle sıfır doldurma;
// renk enterpolasyonu (ArgbEvaluator) platforma bırakıldı, burada yalnızca hangi iki renk arasında
// hangi oranla geçileceği hesaplanır.
// Android'deki hâliyle korunan notlar:
//  - Zihinden (isBlinding) sandıklarda (part 6 hariç) ikon "star_on_ic" olur ve yıldız sayısı 0 sayılır;
//    bu yüzden o sandıklar yıldız kazanma görevini ve karate rozetini ilerletmez.
//  - `resolvedChestIcon` skoru 0'ın altına düşürmez; `changeCupIcon` ise ham rekoru kullanır
//    (negatif rekor yalnızca eşikler negatifse fark yaratır).
//  - Animasyon dosya adlarındaki yazım ("animaton_two.json" vb.) varlık adlarıyla aynıdır; düzeltilmedi.
package com.sorobit.shared.oyun

import kotlin.math.max

/** Kupa/yıldız ikonlarının kaynak adları ve yıldız hesabı. */
object ChestTypeProgressHelper {
    const val ICON_TIER0 = "chest_stars_tier0"
    const val ICON_TIER1 = "chest_stars_tier1"
    const val ICON_TIER2 = "chest_stars_tier2"
    const val ICON_TIER3 = "chest_stars_tier3"

    /** Zihinden sandıklarda kullanılan tek yıldız görseli (görev hesabında 0 yıldız sayılır). */
    const val ICON_STAR_ON = "star_on_ic"

    /** 1 yıldız eşiği (sabit skor). */
    const val ONE_STAR_THRESHOLD = 500

    /** Zihinden sandıkların yıldız kademesi gösterdiği tek bölüm. */
    const val BLINDING_STAR_PART_ID = 6

    fun resolvedChestIcon(item: OyunLessonItem, recordValue: Int, globalPartId: Int): String {
        if (item.isBlinding == true && globalPartId != BLINDING_STAR_PART_ID) {
            return ICON_STAR_ON
        }

        val record = recordValue.coerceAtLeast(0)
        val p1 = item.cupPoint1
        val p2 = item.cupPoint2
        return when {
            p1 != null && record >= p1 -> ICON_TIER3
            p2 != null && record >= p2 -> ICON_TIER2
            record >= ONE_STAR_THRESHOLD -> ICON_TIER1
            else -> ICON_TIER0
        }
    }

    fun starCountForChestIcon(iconRes: String): Int = when (iconRes) {
        ICON_TIER3 -> 3
        ICON_TIER2 -> 2
        ICON_TIER1 -> 1
        ICON_TIER0 -> 0
        ICON_STAR_ON -> 0
        else -> 0
    }

    /**
     * Bu oyun sonunda (recordScoreThisRun ile) ilk kez 3 yıldıza ulaşılıyor mu?
     * [item] tıklanmadan önceki harita durumu; [recordScoreThisRun] bu seferki skor.
     */
    fun shouldIncrementKarateForFirstThreeStars(
        item: OyunLessonItem,
        recordScoreThisRun: Int,
        globalPartId: Int,
    ): Boolean {
        if (item.type != OyunLessonItem.TYPE_CHEST) return false
        val oldStars = starCountForChestIcon(item.stepCupIcon)
        val mergedRecord = maxOf(item.record ?: 0, recordScoreThisRun)
        val newIcon = resolvedChestIcon(item, mergedRecord, globalPartId)
        val newStars = starCountForChestIcon(newIcon)
        return oldStars < 3 && newStars >= 3
    }
}

/** `ChestFragment.changeCupIcon` sonucu. */
data class CupIconChange(
    /** Haritada gösterilecek ikon (değişmediyse mevcut ikon). */
    val icon: String,
    /** Yıldız kazanma görevine yazılacak miktar (ikon değişmediyse 0). */
    val pendingChestStarGainAmount: Int,
)

/** İlerleme çubuğu renginin hangi iki renk arasında, hangi oranla karışacağı. */
data class ScoreBarColorBlend(val fromColor: String, val toColor: String, val fraction: Float)

object ChestResultLogic {
    const val ARG_PENDING_CHEST_RECORD_BREAK_MISSION = "pending_chest_record_break_mission"

    const val COLOR_GREEN = "#7CFC00"
    const val COLOR_YELLOW = "#FFFF99"
    const val COLOR_DARK_RED = "#990000"

    /** Sonuç ekranında rastgele oynatılan Lottie dosyaları (adlar varlıklardaki yazımla aynı). */
    val animations = listOf(
        "animation_one.json",
        "animaton_two.json",
        "animaton_three.json",
        "animaton_four.json",
        "animaton_five.json",
        "animaton_six.json",
        "animaton_eight.json",
        "animaton_nine.json",
        "animaton_ten.json",
        "animaton_eleven.json",
        "animaton_twelve.json",
        "animaton_thirteen.json",
    )

    /**
     * `ChestResult.record`: rekor kırma görevi ilerlesin mi?
     * Yalnızca daha önce bir rekor VARSA ve bu skor onu aşıyorsa (ilk oynayış rekor kırma sayılmaz).
     */
    fun shouldIncrementChestRecordBreakMission(itemType: Int, record: Int?, toplamPuan: Int): Boolean {
        if (itemType != OyunLessonItem.TYPE_CHEST) return false
        return record != null && toplamPuan > record
    }

    /** Çubuğun tavanı: geçerli adımın cupPoint1'i, yoksa şablondaki, o da yoksa 1. */
    fun resolveScoreCap(currentCupPoint1: Int?, templateCupPoint1: Int?): Int {
        if (currentCupPoint1 != null && currentCupPoint1 > 0) return currentCupPoint1
        if (templateCupPoint1 != null && templateCupPoint1 > 0) return templateCupPoint1
        return 1
    }

    /** 2 yıldız eşiği: geçerli adımın cupPoint2'si, yoksa şablondaki, o da yoksa tavanın yarısı (en az 1). */
    fun resolveCupPoint2Threshold(currentCupPoint2: Int?, templateCupPoint2: Int?, scoreCap: Int): Int {
        if (currentCupPoint2 != null && currentCupPoint2 > 0) return currentCupPoint2
        if (templateCupPoint2 != null && templateCupPoint2 > 0) return templateCupPoint2
        return max(1, scoreCap / 2)
    }

    /** `updateScoreProgressBar`: dolgu oranı (0..1). */
    fun scoreProgress(animatedScore: Int, scoreCap: Int): Float {
        val clampedScore = animatedScore.coerceIn(0, scoreCap)
        return (clampedScore.toFloat() / scoreCap.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * `updateScoreProgressBar`: renk geçişi. Skor artarken yeşil→sarı→yeşil, azalırken yeşil→koyu kırmızı.
     */
    fun scoreBarColorBlend(animatedScore: Int, previousScore: Int, scoreCap: Int): ScoreBarColorBlend {
        val progress = scoreProgress(animatedScore, scoreCap)
        val isIncreasing = animatedScore >= previousScore
        return if (isIncreasing) {
            if (progress <= 0.5f) {
                ScoreBarColorBlend(COLOR_GREEN, COLOR_YELLOW, progress * 2f)
            } else {
                ScoreBarColorBlend(COLOR_YELLOW, COLOR_GREEN, (progress - 0.5f) * 2f)
            }
        } else {
            ScoreBarColorBlend(COLOR_GREEN, COLOR_DARK_RED, progress)
        }
    }

    /** `placeStarMarker`: yıldız işaretinin çubuk üzerindeki yatay oranı (0..1). */
    fun starMarkerRatio(threshold: Int, scoreCap: Int): Float {
        val safeThreshold = threshold.coerceIn(0, scoreCap)
        return (safeThreshold.toFloat() / scoreCap.toFloat()).coerceIn(0f, 1f)
    }

    /** Üç yıldız işaretinin eşikleri: 500, cupPoint2, cupPoint1 (tavan). */
    fun starThresholds(cupPoint2Threshold: Int, scoreCap: Int): List<Int> =
        listOf(ChestTypeProgressHelper.ONE_STAR_THRESHOLD, cupPoint2Threshold, scoreCap)

    /** `playStarSoundForTier`: yıldız sesinin hız çarpanı. */
    fun starSoundRate(tier: Int): Float = when (tier) {
        1 -> 0.9f // daha kalın
        2 -> 1.0f // normal
        3 -> 1.12f // daha ince
        else -> 1.0f
    }

    /** "dd:ss" ya da "ss:dd:ss" → saniye; biçim bozuksa null. */
    fun parseTimeToSeconds(value: String): Int? {
        val parts = value.split(":").map { it.trim() }
        return when (parts.size) {
            2 -> {
                val minutes = parts[0].toIntOrNull() ?: return null
                val seconds = parts[1].toIntOrNull() ?: return null
                minutes * 60 + seconds
            }
            3 -> {
                val hours = parts[0].toIntOrNull() ?: return null
                val minutes = parts[1].toIntOrNull() ?: return null
                val seconds = parts[2].toIntOrNull() ?: return null
                hours * 3600 + minutes * 60 + seconds
            }
            else -> null
        }
    }

    /** Saniye → "d:ss" (dakika sıfır doldurulmaz, saat ayrılmaz). */
    fun formatSeconds(seconds: Int): String {
        val safeSeconds = max(0, seconds)
        val minutes = safeSeconds / 60
        val remainingSeconds = safeSeconds % 60
        return "$minutes:${ikiHane(remainingSeconds)}"
    }

    /** `ChestFragment.changeCupIcon`. Sandık değilse null (Android: hiçbir şey yapmaz). */
    fun changeCupIcon(lessonItem: OyunLessonItem, globalPartId: Int): CupIconChange? {
        if (lessonItem.type != OyunLessonItem.TYPE_CHEST) return null
        val record = lessonItem.record ?: 0
        val p1 = lessonItem.cupPoint1
        val p2 = lessonItem.cupPoint2
        var resolvedIcon = when {
            p1 != null && record >= p1 -> ChestTypeProgressHelper.ICON_TIER3
            p2 != null && record >= p2 -> ChestTypeProgressHelper.ICON_TIER2
            record >= ChestTypeProgressHelper.ONE_STAR_THRESHOLD -> ChestTypeProgressHelper.ICON_TIER1
            else -> ChestTypeProgressHelper.ICON_TIER0
        }
        if (lessonItem.isBlinding == true && globalPartId != ChestTypeProgressHelper.BLINDING_STAR_PART_ID) {
            resolvedIcon = ChestTypeProgressHelper.ICON_STAR_ON
        }
        val currentIcon = lessonItem.stepCupIcon
        val iconChanged = currentIcon != resolvedIcon
        val currentStars = ChestTypeProgressHelper.starCountForChestIcon(currentIcon)
        val resolvedStars = ChestTypeProgressHelper.starCountForChestIcon(resolvedIcon)
        val gainedStars = (resolvedStars - currentStars).coerceAtLeast(0)
        return CupIconChange(
            icon = if (iconChanged) resolvedIcon else currentIcon,
            pendingChestStarGainAmount = if (iconChanged) gainedStars else 0,
        )
    }

    /** `ChestFragment.lessonProgressKey`. */
    fun lessonProgressKey(type: Int, id: Int?, partId: Int?, title: String, fallbackIndex: Int): String {
        val stableId = id ?: -1
        val part = partId ?: -1
        return "${type}_${stableId}_${part}_${title}_$fallbackIndex"
    }
}

/**
 * `ChestResult.maybeTriggerStarMarkers`: skor animasyonu ilerlerken her yıldız eşiği YUKARI doğru
 * ilk kez aşıldığında bir kez tetiklenir.
 */
class ChestResultStarTracker(
    private val cupPoint2Threshold: Int,
    private val scoreCap: Int,
) {
    private var star1Played = false
    private var star2Played = false
    private var star3Played = false

    /** `prepareInitialUiState`: bayrakları sıfırlar. */
    fun reset() {
        star1Played = false
        star2Played = false
        star3Played = false
    }

    /** @return bu karede tetiklenen yıldız kademeleri (1, 2, 3), küçükten büyüğe. */
    fun onScoreChanged(previousScore: Int, currentScore: Int): List<Int> {
        val triggered = mutableListOf<Int>()
        val wasAtOrAbove500 = previousScore >= ChestTypeProgressHelper.ONE_STAR_THRESHOLD
        val isAtOrAbove500 = currentScore >= ChestTypeProgressHelper.ONE_STAR_THRESHOLD
        if (!star1Played && !wasAtOrAbove500 && isAtOrAbove500) {
            triggered.add(1)
            star1Played = true
        }

        val wasAtOrAboveCup2 = previousScore >= cupPoint2Threshold
        val isAtOrAboveCup2 = currentScore >= cupPoint2Threshold
        if (!star2Played && !wasAtOrAboveCup2 && isAtOrAboveCup2) {
            triggered.add(2)
            star2Played = true
        }

        val wasAtOrAboveCup1 = previousScore >= scoreCap
        val isAtOrAboveCup1 = currentScore >= scoreCap
        if (!star3Played && !wasAtOrAboveCup1 && isAtOrAboveCup1) {
            triggered.add(3)
            star3Played = true
        }
        return triggered
    }
}

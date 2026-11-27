// Kaynak (Android): app/src/main/java/com/example/app/BadgePrecalcHelper.kt — executeCupDeltaUpdateAsync içindeki
// saf kararlar: bekleyen delta seçimi, ders bayraklarına göre kupa türü/depo/rozet seçimi, yerel rozet ön hesabı.
// Firebase Auth, GlobalValues okuma/yazması ve Firestore güncellemesi platform katmanında kalır.
//
// ANDROID (güncel): executeCupDeltaUpdateAsync artık yerel ön hesap YAPMAZ. Akış: bekleyen delta seçilir
// ([resolvePendingDelta]), `submitCupResult` sunucuya gider ([buildCupSubmitPayload], bkz. [CupScoreService]), dönen
// newScore ile ilgili rozetin senkronu çalışır (`BadgeProgressFirestore.computeCupBadgeProgressSync` + zincir) ve
// kutlama listesi oradan yayınlanır. [precalculate] eski davranışın belgesi/testi olarak durur; kullanılmamalı
// (500'e çıkıp 480'e düşüp tekrar 500'e çıkan kullanıcıya aynı rozet ikinci kez kutlanırdı).
package com.sorobit.shared.kupa

import com.sorobit.shared.kupa.rozet.BadgeAnimMode
import com.sorobit.shared.kupa.rozet.BadgeLevelUpPayload
import com.sorobit.shared.kupa.rozet.BadgeProgressFirestore

/**
 * Android'deki `when { isMultiplication && isBlinding -> ...; isMultiplication -> ...; ... }` dallarının
 * her biri: hangi kupa deposu güncellenir ve hangi hayvan rozeti senkronlanır.
 */
enum class CupPathKind(val repository: CupScoreRepositorySpec, val badgeMode: BadgeAnimMode) {
    /** isMultiplication && isBlinding */
    BLINDING_IMPACT(BlindingImpactCupRepository, BadgeAnimMode.TURTLE),
    /** isMultiplication */
    IMPACT(ImpactCupRepository, BadgeAnimMode.GOAT),
    /** isExtraction && isBlinding */
    BLINDING_EXTRACTION(BlindingExtractionCupRepository, BadgeAnimMode.FLY),
    /** isExtraction */
    EXTRACTION(ExtractionCupRepository, BadgeAnimMode.CROCODILE),
    /** isBlinding */
    BLINDING_ADDITION(BlindingAdditionCupRepository, BadgeAnimMode.EAGLE),
    /** else */
    ADDITION(AbacusCupRepository, BadgeAnimMode.DINO),
}

/** [BadgePrecalcHelper.precalculate] sonucu. */
data class CupDeltaPrecalc(
    val kind: CupPathKind,
    val delta: Int,
    val oldScore: Int,
    val newScore: Int,
    /** Android: `GlobalValues.pendingCupBadgePayloads = payloadsLocally`. */
    val payloadsLocally: List<BadgeLevelUpPayload>,
)

object BadgePrecalcHelper {

    /**
     * Android'deki delta zinciri: null olmayan İLK değer kullanılır (sıra önemlidir); hepsi null ise işlem yapılmaz.
     */
    fun resolvePendingDelta(
        pendingCupDelta: Int?,
        pendingBlindingCupDelta: Int?,
        pendingExtractionCupDelta: Int?,
        pendingBlindingExtractionCupDelta: Int?,
        pendingImpactCupDelta: Int?,
        pendingBlindingImpactCupDelta: Int?,
    ): Int? = pendingCupDelta
        ?: pendingBlindingCupDelta
        ?: pendingExtractionCupDelta
        ?: pendingBlindingExtractionCupDelta
        ?: pendingImpactCupDelta
        ?: pendingBlindingImpactCupDelta

    /** LessonItem bayraklarından (`isMultiplication`, `isExtraction`, `isBlinding`; null = false) kupa türü. */
    fun resolveCupPathKind(
        isMultiplication: Boolean?,
        isExtraction: Boolean?,
        isBlinding: Boolean?,
    ): CupPathKind = when {
        isMultiplication == true && isBlinding == true -> CupPathKind.BLINDING_IMPACT
        isMultiplication == true -> CupPathKind.IMPACT
        isExtraction == true && isBlinding == true -> CupPathKind.BLINDING_EXTRACTION
        isExtraction == true -> CupPathKind.EXTRACTION
        isBlinding == true -> CupPathKind.BLINDING_ADDITION
        else -> CupPathKind.ADDITION
    }

    /**
     * Android (güncel): `CupScoreService.submitResult` çağrısının yükü. `won = delta > 0`; zorluk
     * `lessonItem.cupDifficultyLevel ?: 0`; çarpma bayrağı ders bayrağından.
     */
    fun buildCupSubmitPayload(
        delta: Int,
        isMultiplication: Boolean?,
        isExtraction: Boolean?,
        isBlinding: Boolean?,
        cupDifficultyLevel: Int?,
        dayId: String = CupHistoryRepository.todayId(),
    ): Map<String, Any> = CupScoreService.buildSubmitPayload(
        cupField = CupScoreService.cupFieldFor(isMultiplication, isExtraction, isBlinding),
        won = delta > 0,
        difficultyLevel = cupDifficultyLevel ?: 0,
        isMultiplication = isMultiplication == true,
        dayId = dayId,
    )

    /**
     * ESKİ (Android'de kaldırıldı): yerel olarak rozetleri önceden hesaplar (İyimser arayüz).
     *
     * Not (Android davranışı): `newScore = oldScore + delta` burada 0'a KIRPILMAZ; Firestore transaction'ı ise
     * `coerceAtLeast(0)` uygular. Ayrıca asıl senkron, kayıtlı rozet ilerlemesini (kupa skorunu değil) "önce"
     * değeri olarak alır; bu ön hesap `currentLessonOldCupScore` (yoksa 0) kullanır.
     *
     * @param currentLessonOldCupScore Android `GlobalValues.currentLessonOldCupScore` (null → 0).
     */
    fun precalculate(
        delta: Int,
        currentLessonOldCupScore: Int?,
        isMultiplication: Boolean?,
        isExtraction: Boolean?,
        isBlinding: Boolean?,
    ): CupDeltaPrecalc {
        val oldScore = currentLessonOldCupScore ?: 0
        val newScore = oldScore + delta
        val kind = resolveCupPathKind(isMultiplication, isExtraction, isBlinding)
        val payloadsLocally = BadgeProgressFirestore.resolveCupBadgeLevelUpChain(kind.badgeMode, oldScore, newScore)
        return CupDeltaPrecalc(kind, delta, oldScore, newScore, payloadsLocally)
    }
}

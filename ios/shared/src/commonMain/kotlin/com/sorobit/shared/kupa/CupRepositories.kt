// Kaynak (Android): app/src/main/java/com/example/app/
//   AbacusCupRepository.kt, ExtractionCupRepository.kt, ImpactCupRepository.kt,
//   BlindingAdditionCupRepository.kt, BlindingExtractionCupRepository.kt, BlindingImpactCupRepository.kt
// Altı dosya yalnızca FIELD sabitinde ayrışan aynı koddur. Firestore çağrıları taşınmadı; yol/alan adları ve
// okuma (fetchCupScore) / yazma (updateCupScore) hesapları saf fonksiyonlara ayrıldı.
// Nesne adları Android ile aynıdır.
package com.sorobit.shared.kupa

/** [CupScoreRepositorySpec.resolveFetchedCupScore] sonucu. */
data class CupFetchResult(
    /** `onResult(score)` ile iletilecek skor. */
    val score: Int,
    /** true ise alan yoktur: `set(mapOf(FIELD to score), SetOptions.merge())` ile seed edilmelidir. */
    val shouldSeed: Boolean,
)

/** [CupScoreRepositorySpec.computeUpdatedCupScore] sonucu (Android transaction'ının yaptığı iki yazma + dönüş). */
data class CupUpdateResult(
    val oldScore: Int,
    val newScore: Int,
    /** `cupWayProgress/progress` dokümanına merge ile yazılacak harita. */
    val progressUpdate: Map<String, Any>,
    /** `cupHistory/{yyyy-MM-dd}` dokümanına merge ile yazılacak harita ([CupHistoryRepository.recordSnapshot]). */
    val historyUpdate: Map<String, Any>,
)

/**
 * Bir kupa skorunun Firestore tanımı.
 *
 * Veri yolu: `users/{uid}/cupWayProgress/progress` dokümanındaki [FIELD] (INT) alanı.
 * (Android dosyalarındaki KDoc `users/{uid}/<alan>` der; gerçek kod yukarıdaki alt koleksiyonu kullanır.)
 */
abstract class CupScoreRepositorySpec(
    /** Firestore alan adı (ör. "addition_abacus_cup"). */
    val FIELD: String,
) {
    val DEFAULT_CUP_SCORE: Int = 200
    val COLLECTION: String = "users"
    val SUBCOLLECTION: String = "cupWayProgress"
    val DOCUMENT: String = "progress"

    fun progressDocumentPath(uid: String): String = "$COLLECTION/$uid/$SUBCOLLECTION/$DOCUMENT"

    /**
     * Android `fetchCupScore` başarı dalı: `(doc?.get(FIELD) as? Number)?.toInt()`; alan yoksa (ya da sayı değilse)
     * [DEFAULT_CUP_SCORE] ile seed edilir ve aynı değer döner.
     *
     * @param rawFieldValue dokümandaki [FIELD] değeri (doküman/alan yoksa null).
     */
    fun resolveFetchedCupScore(rawFieldValue: Any?): CupFetchResult {
        val raw = (rawFieldValue as? Number)?.toInt()
        return if (raw == null) {
            // Alan yok → seed et
            CupFetchResult(DEFAULT_CUP_SCORE, shouldSeed = true)
        } else {
            CupFetchResult(raw, shouldSeed = false)
        }
    }

    /** Android `fetchCupScore` hata dalı: okuma başarısız → güvenli varsayılan. */
    fun fetchFailureCupScore(): Int = DEFAULT_CUP_SCORE

    /**
     * Android `updateCupScore` transaction gövdesi: skor [delta] kadar değişir, 0'ın altına düşemez.
     *
     * @param currentFieldValue transaction içinde okunan [FIELD] değeri (`snapshot.getLong(FIELD)`); yoksa null.
     */
    fun computeUpdatedCupScore(currentFieldValue: Any?, delta: Int): CupUpdateResult {
        val current = ((currentFieldValue as? Number)?.toLong() ?: DEFAULT_CUP_SCORE.toLong()).toInt()
        val updated = (current + delta).coerceAtLeast(0)
        return CupUpdateResult(
            oldScore = current,
            newScore = updated,
            progressUpdate = mapOf(FIELD to updated),
            historyUpdate = CupHistoryRepository.recordSnapshot(FIELD, updated),
        )
    }
}

/** Toplama kupa yolu (Dino). */
object AbacusCupRepository : CupScoreRepositorySpec("addition_abacus_cup") {
    /** Kupa değişiminin sabit adım büyüklüğü. */
    const val CUP_STEP = 5
}

/** Çıkarma kupa yolu (Timo). */
object ExtractionCupRepository : CupScoreRepositorySpec("extraction_abacus_cup") {
    /** Kupa değişiminin sabit adım büyüklüğü. */
    const val CUP_STEP = 5
}

/** Card3View çarpma kupa modu (Keçi). */
object ImpactCupRepository : CupScoreRepositorySpec("impact_abacus_cup") {
    const val CUP_STEP = 5
}

/** Körleme toplama kupa yolu (Şaho). */
object BlindingAdditionCupRepository : CupScoreRepositorySpec("blinding_addition_abacus_cup") {
    /** Kupa değişiminin sabit adım büyüklüğü. */
    const val CUP_STEP = 5
}

/** Körleme çıkarma kupa yolu (Sino). */
object BlindingExtractionCupRepository : CupScoreRepositorySpec("blinding_extraction_abacus_cup") {
    /** Kupa değişiminin sabit adım büyüklüğü. */
    const val CUP_STEP = 5
}

/** Card6View körleme çarpma kupa modu (Kapi). Android'de bu depoda CUP_STEP sabiti YOKTUR. */
object BlindingImpactCupRepository : CupScoreRepositorySpec("blinding_impact_abacus_cup")

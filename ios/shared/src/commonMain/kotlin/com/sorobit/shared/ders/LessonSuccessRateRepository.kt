// KAYNAK: app/src/main/java/com/example/app/LessonSuccessRateRepository.kt
//
// Android'deki Firestore transaction gövdelerinin saf (hesap) kısmı: "okunan belge alanları → yazılacak alanlar".
// Platform katmanı transaction'ı açar, belgeleri okuyup düz Map olarak verir ve dönen haritaları
// `SetOptions.merge()` ile yazar. Alan adları ve yollar Android ile birebir aynıdır.
//
// Android'den bilinçli farklar:
//  - FirebaseAuth / FirebaseFirestore / Log çağrıları yok. Ön koşullar platformda korunmalı:
//    `partId in 1..8` ([isTrackedPart]) ve giriş yapmış kullanıcı (uid != null); aksi halde hiçbir şey yapılmaz.
//  - Android `getBoolean` / `getLong` alan türü uymazsa istisna atar (transaction başarısız olur, yalnızca loglanır);
//    burada tür uymayan alan "yok" sayılır.
package com.sorobit.shared.ders

/**
 * Part 1-8'deki chest/lesson tipi [LessonItem]'lerin kullanıcı bazlı geçme/geçememe istatistikleri iki seviyede tutulur:
 *
 * - **Item seviyesi** — `successRate/lessonSuccessRate/part{partId}/{position}`: item'ın TÜMÜ ilk kez bittiğinde
 *   (`stepIsFinish` false->true) ve sonraki her tekrar çözülüşünde güncellenir.
 * - **Adım seviyesi** — `.../part{partId}/{position}/steps/{step}` (alt koleksiyon): her adımın kendi zorluk verisi.
 *
 * Çift sayımı önlemek için her kullanıcının adım bazındaki durumu
 * `users/{uid}/lessonSuccessRateState/{partId}_{position}_{step}` içinde tutulur (`attempted`, `passed`, `failStreak`).
 */
object LessonSuccessRateRepository {

    // --- Adım seviyesi alanları ---
    const val FIELD_ATTEMPTED = "attempted"
    const val FIELD_PASSED = "passed"
    const val FIELD_FAIL_STREAK = "failStreak"

    const val FIELD_ATTEMPT_FAIL_TOTAL = "attemptFailTotal"
    const val FIELD_ATTEMPT_USER_COUNT = "attemptUserCount"
    const val FIELD_STEP_PASS_USER_COUNT = "passUserCount"
    const val FIELD_PASS_FIRST_TRY = "passOnFirstTry"
    const val FIELD_PASS_SECOND_TRY = "passOnSecondTry"
    const val FIELD_PASS_THIRD_PLUS_TRY = "passOnThirdPlusTry"
    const val FIELD_SUCCESS_RATE_PERCENT = "successRatePercent"
    const val FIELD_FIRST_TRY_SUCCESS_RATE_PERCENT = "firstTrySuccessRatePercent"

    // --- Item seviyesi alanları ---
    const val FIELD_ITEM_PASS_USER_COUNT = "passUserCount"
    const val FIELD_TOTAL_COMPLETION_COUNT = "totalCompletionCount"
    const val FIELD_AVG_REPLAY_COUNT = "avgReplayCount"

    // Sadece TYPE_CHEST için: ilk tamamlamada kaç yıldızla geçildi.
    const val FIELD_ONE_STAR_COUNT = "oneStarCount"
    const val FIELD_TWO_STAR_COUNT = "twoStarCount"
    const val FIELD_THREE_STAR_COUNT = "threeStarCount"

    /** İstatistik tutulan part'lar (Android: `if (partId !in 1..8) return`). */
    fun isTrackedPart(partId: Int): Boolean = partId in 1..8

    /** `successRate/lessonSuccessRate/part{partId}` koleksiyon yolu. */
    fun partCollectionPath(partId: Int): String = "successRate/lessonSuccessRate/part$partId"

    /** Item seviyesi doküman yolu. */
    fun itemPath(partId: Int, position: Int): String = "${partCollectionPath(partId)}/$position"

    /** Adım seviyesi doküman yolu ([step] en az 1'e yuvarlanır). */
    fun stepPath(partId: Int, position: Int, step: Int): String =
        "${itemPath(partId, position)}/steps/${step.coerceAtLeast(1)}"

    /** Kullanıcının adım durumu doküman yolu ([step] en az 1'e yuvarlanır). */
    fun userStepStatePath(uid: String, partId: Int, position: Int, step: Int): String =
        "users/$uid/lessonSuccessRateState/${partId}_${position}_${step.coerceAtLeast(1)}"

    /** Adım seviyesi transaction çıktısı: iki belge de `merge` ile yazılır. */
    data class StepWrite(
        val userState: Map<String, Any>,
        val stepDoc: Map<String, Any>,
    )

    private fun Map<String, Any?>.bool(key: String): Boolean = (this[key] as? Boolean) == true
    private fun Map<String, Any?>.long(key: String): Long? = (this[key] as? Number)?.toLong()

    /**
     * Kullanıcı bu adımı bitiremeden dersten çıktığında (quiz veya chest skoru yetersiz).
     * [userSnap]: kullanıcının adım durumu belgesi; [globalSnap]: adım istatistik belgesi (yoksa boş harita).
     * @return yazılacak alanlar; kullanıcı bu adımı daha önce geçtiyse null (hiçbir şey yazılmaz).
     */
    fun recordFail(userSnap: Map<String, Any?>, globalSnap: Map<String, Any?>): StepWrite? {
        if (userSnap.bool(FIELD_PASSED)) {
            return null
        }
        val alreadyAttempted = userSnap.bool(FIELD_ATTEMPTED)
        val newFailStreak = (userSnap.long(FIELD_FAIL_STREAK) ?: 0L) + 1

        val attemptFailTotal = (globalSnap.long(FIELD_ATTEMPT_FAIL_TOTAL) ?: 0L) + 1
        val attemptUserCount = (globalSnap.long(FIELD_ATTEMPT_USER_COUNT) ?: 0L) +
            if (alreadyAttempted) 0L else 1L
        val passUserCount = globalSnap.long(FIELD_STEP_PASS_USER_COUNT) ?: 0L
        val passFirst = globalSnap.long(FIELD_PASS_FIRST_TRY) ?: 0L

        return StepWrite(
            userState = mapOf(FIELD_ATTEMPTED to true, FIELD_FAIL_STREAK to newFailStreak),
            stepDoc = mapOf(
                FIELD_ATTEMPT_FAIL_TOTAL to attemptFailTotal,
                FIELD_ATTEMPT_USER_COUNT to attemptUserCount,
                FIELD_SUCCESS_RATE_PERCENT to successRatePercent(passUserCount, attemptUserCount),
                FIELD_FIRST_TRY_SUCCESS_RATE_PERCENT to successRatePercent(passFirst, attemptUserCount),
            ),
        )
    }

    /**
     * Kullanıcı bu adımı ilk kez başarıyla geçtiğinde (adım ilerlemesi veya item bitişi).
     * @return yazılacak alanlar; kullanıcı bu adımı daha önce geçtiyse null.
     */
    fun recordPass(userSnap: Map<String, Any?>, globalSnap: Map<String, Any?>): StepWrite? {
        if (userSnap.bool(FIELD_PASSED)) {
            return null
        }
        val alreadyAttempted = userSnap.bool(FIELD_ATTEMPTED)
        val failStreak = userSnap.long(FIELD_FAIL_STREAK) ?: 0L

        val attemptUserCount = (globalSnap.long(FIELD_ATTEMPT_USER_COUNT) ?: 0L) +
            if (alreadyAttempted) 0L else 1L
        val passUserCount = (globalSnap.long(FIELD_STEP_PASS_USER_COUNT) ?: 0L) + 1
        var passFirst = globalSnap.long(FIELD_PASS_FIRST_TRY) ?: 0L
        var passSecond = globalSnap.long(FIELD_PASS_SECOND_TRY) ?: 0L
        var passThirdPlus = globalSnap.long(FIELD_PASS_THIRD_PLUS_TRY) ?: 0L
        when (failStreak) {
            0L -> passFirst += 1
            1L -> passSecond += 1
            else -> passThirdPlus += 1
        }

        return StepWrite(
            userState = mapOf(FIELD_ATTEMPTED to true, FIELD_PASSED to true),
            stepDoc = mapOf(
                FIELD_ATTEMPT_USER_COUNT to attemptUserCount,
                FIELD_STEP_PASS_USER_COUNT to passUserCount,
                FIELD_PASS_FIRST_TRY to passFirst,
                FIELD_PASS_SECOND_TRY to passSecond,
                FIELD_PASS_THIRD_PLUS_TRY to passThirdPlus,
                FIELD_SUCCESS_RATE_PERCENT to successRatePercent(passUserCount, attemptUserCount),
                FIELD_FIRST_TRY_SUCCESS_RATE_PERCENT to successRatePercent(passFirst, attemptUserCount),
            ),
        )
    }

    /**
     * Item (tüm adımları dahil) ilk kez tamamen bittiğinde (`stepIsFinish` false->true).
     * [chestStars] yalnızca TYPE_CHEST için verilir; 1/2/3 yıldız dağılımı sadece bu durumda güncellenir
     * (0 yıldızda üç sayaç da mevcut değerleriyle yeniden yazılır).
     * @return item belgesine `merge` ile yazılacak alanlar.
     */
    fun recordItemFirstFinish(globalSnap: Map<String, Any?>, chestStars: Int? = null): Map<String, Any> {
        val totalCompletionCount = (globalSnap.long(FIELD_TOTAL_COMPLETION_COUNT) ?: 0L) + 1
        val passUserCount = (globalSnap.long(FIELD_ITEM_PASS_USER_COUNT) ?: 0L) + 1

        val updates = mutableMapOf<String, Any>(
            FIELD_TOTAL_COMPLETION_COUNT to totalCompletionCount,
            FIELD_ITEM_PASS_USER_COUNT to passUserCount,
            FIELD_AVG_REPLAY_COUNT to avgReplayCount(totalCompletionCount, passUserCount),
        )
        if (chestStars != null) {
            var oneStar = globalSnap.long(FIELD_ONE_STAR_COUNT) ?: 0L
            var twoStar = globalSnap.long(FIELD_TWO_STAR_COUNT) ?: 0L
            var threeStar = globalSnap.long(FIELD_THREE_STAR_COUNT) ?: 0L
            when (chestStars) {
                1 -> oneStar += 1
                2 -> twoStar += 1
                3 -> threeStar += 1
            }
            updates[FIELD_ONE_STAR_COUNT] = oneStar
            updates[FIELD_TWO_STAR_COUNT] = twoStar
            updates[FIELD_THREE_STAR_COUNT] = threeStar
        }
        return updates
    }

    /**
     * Daha önce tamamen bitirilmiş (stepIsFinish=true) bir item tekrar çözülüp yeniden bittiğinde.
     * Yıldız dağılımına dokunmaz, sadece toplam tamamlama sayısını ve ortalama tekrar oranını günceller.
     */
    fun recordItemReplay(globalSnap: Map<String, Any?>): Map<String, Any> {
        val totalCompletionCount = (globalSnap.long(FIELD_TOTAL_COMPLETION_COUNT) ?: 0L) + 1
        val passUserCount = globalSnap.long(FIELD_ITEM_PASS_USER_COUNT) ?: 0L
        return mapOf(
            FIELD_TOTAL_COMPLETION_COUNT to totalCompletionCount,
            FIELD_AVG_REPLAY_COUNT to avgReplayCount(totalCompletionCount, passUserCount),
        )
    }

    fun successRatePercent(passUserCount: Long, attemptUserCount: Long): Float {
        if (attemptUserCount <= 0L) return 0f
        return (passUserCount.toFloat() / attemptUserCount.toFloat()) * 100f
    }

    fun avgReplayCount(totalCompletionCount: Long, passUserCount: Long): Float {
        if (passUserCount <= 0L) return 0f
        return totalCompletionCount.toFloat() / passUserCount.toFloat()
    }
}

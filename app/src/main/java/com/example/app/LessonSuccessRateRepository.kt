package com.example.app

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Part 1-8'deki chest/lesson tipi [com.example.app.model.LessonItem]'lerin kullanıcı bazlı
 * geçme/geçememe istatistiklerini iki seviyede tutar:
 *
 * - **Item seviyesi** — `successRate/lessonSuccessRate/part{partId}/{position}`: item'ın TÜMÜ
 *   (tüm adımları) ilk kez bittiğinde (`stepIsFinish` false->true) ve sonraki her tekrar
 *   çözülüşünde güncellenir. `totalCompletionCount` / `passUserCount` / `avgReplayCount` burada.
 *   TYPE_CHEST'e özel yıldız dağılımı (`oneStarCount` vb.) da burada, çünkü chest item'leri
 *   her zaman tek adımlıdır.
 * - **Adım seviyesi** — `.../​part{partId}/{position}/steps/{step}` (alt koleksiyon): her adımın
 *   kendi zorluk verisi (`attemptFailTotal`, `passOnFirstTry` vb.) — çok adımlı derslerde adımlar
 *   birbirinden bağımsız zorlukta olabileceği için ayrı tutulur.
 *
 * Transaction deseni [DailyQuestionRepository]'deki `dailyQuestionSuccessRate` sayaçlarıyla aynıdır.
 *
 * Çift sayımı önlemek için her kullanıcının adım bazındaki durumu
 * `users/{uid}/lessonSuccessRateState/{partId}_{position}_{step}` içinde tutulur:
 * - `attempted`: kullanıcı bu adımı en az bir kez denedi mi (attemptUserCount'a bir daha eklenmesin diye)
 * - `passed`: kullanıcı bu adımı en az bir kez geçti mi (passUserCount'a bir daha eklenmesin diye)
 * - `failStreak`: bu adımdaki ilk başarıya kadar kaç kez başarısız olundu (geçiş dağılımı kovası için)
 *
 * Item seviyesinde ayrı bir kullanıcı-durum dokümanına gerek yok: çağıran taraf zaten
 * [com.example.app.model.LessonItem.stepIsFinish] alanına bakarak "ilk bitiş mi, tekrar mı"
 * ayrımını yapıp doğru fonksiyonu (`recordItemFirstFinish` / `recordItemReplay`) çağırıyor.
 */
object LessonSuccessRateRepository {

    private const val TAG = "LessonSuccessRateRepo"

    // --- Adım seviyesi alanları ---
    private const val FIELD_ATTEMPTED = "attempted"
    private const val FIELD_PASSED = "passed"
    private const val FIELD_FAIL_STREAK = "failStreak"

    private const val FIELD_ATTEMPT_FAIL_TOTAL = "attemptFailTotal"
    private const val FIELD_ATTEMPT_USER_COUNT = "attemptUserCount"
    private const val FIELD_STEP_PASS_USER_COUNT = "passUserCount"
    private const val FIELD_PASS_FIRST_TRY = "passOnFirstTry"
    private const val FIELD_PASS_SECOND_TRY = "passOnSecondTry"
    private const val FIELD_PASS_THIRD_PLUS_TRY = "passOnThirdPlusTry"
    private const val FIELD_SUCCESS_RATE_PERCENT = "successRatePercent"
    private const val FIELD_FIRST_TRY_SUCCESS_RATE_PERCENT = "firstTrySuccessRatePercent"

    // --- Item seviyesi alanları ---
    private const val FIELD_ITEM_PASS_USER_COUNT = "passUserCount"
    private const val FIELD_TOTAL_COMPLETION_COUNT = "totalCompletionCount"
    private const val FIELD_AVG_REPLAY_COUNT = "avgReplayCount"
    // Sadece TYPE_CHEST için: ilk tamamlamada kaç yıldızla geçildi.
    private const val FIELD_ONE_STAR_COUNT = "oneStarCount"
    private const val FIELD_TWO_STAR_COUNT = "twoStarCount"
    private const val FIELD_THREE_STAR_COUNT = "threeStarCount"

    private fun partCollection(partId: Int) =
        FirebaseFirestore.getInstance()
            .collection("successRate")
            .document("lessonSuccessRate")
            .collection("part$partId")

    private fun itemRef(partId: Int, position: Int) =
        partCollection(partId).document(position.toString())

    private fun stepRef(partId: Int, position: Int, step: Int) =
        itemRef(partId, position)
            .collection("steps")
            .document(step.coerceAtLeast(1).toString())

    private fun userStepStateRef(uid: String, partId: Int, position: Int, step: Int) =
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("lessonSuccessRateState")
            .document("${partId}_${position}_${step.coerceAtLeast(1)}")

    /** Kullanıcı bu adımı bitiremeden dersten çıktığında (quiz veya chest skoru yetersiz) çağrılır. */
    fun recordFail(partId: Int, position: Int, step: Int) {
        if (partId !in 1..8) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = userStepStateRef(uid, partId, position, step)
        val docRef = stepRef(partId, position, step)

        FirebaseFirestore.getInstance().runTransaction { tx ->
            val userSnap = tx.get(userRef)
            if (userSnap.getBoolean(FIELD_PASSED) == true) {
                return@runTransaction null
            }
            val alreadyAttempted = userSnap.getBoolean(FIELD_ATTEMPTED) == true
            val newFailStreak = (userSnap.getLong(FIELD_FAIL_STREAK) ?: 0L) + 1

            val globalSnap = tx.get(docRef)
            val attemptFailTotal = (globalSnap.getLong(FIELD_ATTEMPT_FAIL_TOTAL) ?: 0L) + 1
            val attemptUserCount = (globalSnap.getLong(FIELD_ATTEMPT_USER_COUNT) ?: 0L) +
                if (alreadyAttempted) 0L else 1L
            val passUserCount = globalSnap.getLong(FIELD_STEP_PASS_USER_COUNT) ?: 0L
            val passFirst = globalSnap.getLong(FIELD_PASS_FIRST_TRY) ?: 0L

            tx.set(
                userRef,
                mapOf(FIELD_ATTEMPTED to true, FIELD_FAIL_STREAK to newFailStreak),
                SetOptions.merge(),
            )
            tx.set(
                docRef,
                mapOf(
                    FIELD_ATTEMPT_FAIL_TOTAL to attemptFailTotal,
                    FIELD_ATTEMPT_USER_COUNT to attemptUserCount,
                    FIELD_SUCCESS_RATE_PERCENT to successRatePercent(passUserCount, attemptUserCount),
                    FIELD_FIRST_TRY_SUCCESS_RATE_PERCENT to successRatePercent(passFirst, attemptUserCount),
                ),
                SetOptions.merge(),
            )
            null
        }.addOnFailureListener { e ->
            Log.w(TAG, "recordFail failed part=$partId position=$position step=$step", e)
        }
    }

    /** Kullanıcı bu adımı ilk kez başarıyla geçtiğinde (adım ilerlemesi veya item bitişi) çağrılır. */
    fun recordPass(partId: Int, position: Int, step: Int) {
        if (partId !in 1..8) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = userStepStateRef(uid, partId, position, step)
        val docRef = stepRef(partId, position, step)

        FirebaseFirestore.getInstance().runTransaction { tx ->
            val userSnap = tx.get(userRef)
            if (userSnap.getBoolean(FIELD_PASSED) == true) {
                return@runTransaction null
            }
            val alreadyAttempted = userSnap.getBoolean(FIELD_ATTEMPTED) == true
            val failStreak = userSnap.getLong(FIELD_FAIL_STREAK) ?: 0L

            val globalSnap = tx.get(docRef)
            val attemptUserCount = (globalSnap.getLong(FIELD_ATTEMPT_USER_COUNT) ?: 0L) +
                if (alreadyAttempted) 0L else 1L
            val passUserCount = (globalSnap.getLong(FIELD_STEP_PASS_USER_COUNT) ?: 0L) + 1
            var passFirst = globalSnap.getLong(FIELD_PASS_FIRST_TRY) ?: 0L
            var passSecond = globalSnap.getLong(FIELD_PASS_SECOND_TRY) ?: 0L
            var passThirdPlus = globalSnap.getLong(FIELD_PASS_THIRD_PLUS_TRY) ?: 0L
            when (failStreak) {
                0L -> passFirst += 1
                1L -> passSecond += 1
                else -> passThirdPlus += 1
            }

            tx.set(
                userRef,
                mapOf(FIELD_ATTEMPTED to true, FIELD_PASSED to true),
                SetOptions.merge(),
            )
            tx.set(
                docRef,
                mapOf(
                    FIELD_ATTEMPT_USER_COUNT to attemptUserCount,
                    FIELD_STEP_PASS_USER_COUNT to passUserCount,
                    FIELD_PASS_FIRST_TRY to passFirst,
                    FIELD_PASS_SECOND_TRY to passSecond,
                    FIELD_PASS_THIRD_PLUS_TRY to passThirdPlus,
                    FIELD_SUCCESS_RATE_PERCENT to successRatePercent(passUserCount, attemptUserCount),
                    FIELD_FIRST_TRY_SUCCESS_RATE_PERCENT to successRatePercent(passFirst, attemptUserCount),
                ),
                SetOptions.merge(),
            )
            null
        }.addOnFailureListener { e ->
            Log.w(TAG, "recordPass failed part=$partId position=$position step=$step", e)
        }
    }

    /**
     * Item (tüm adımları dahil) ilk kez tamamen bittiğinde (`stepIsFinish` false->true) çağrılır.
     * [chestStars] yalnızca TYPE_CHEST için verilir; 1/2/3 yıldız dağılımı sadece bu durumda güncellenir.
     * Aynı zamanda `totalCompletionCount`/`passUserCount`/`avgReplayCount`'a da bu ilk tamamlama yansır.
     */
    fun recordItemFirstFinish(partId: Int, position: Int, chestStars: Int? = null) {
        if (partId !in 1..8) return
        if (FirebaseAuth.getInstance().currentUser?.uid == null) return
        val docRef = itemRef(partId, position)

        FirebaseFirestore.getInstance().runTransaction { tx ->
            val globalSnap = tx.get(docRef)
            val totalCompletionCount = (globalSnap.getLong(FIELD_TOTAL_COMPLETION_COUNT) ?: 0L) + 1
            val passUserCount = (globalSnap.getLong(FIELD_ITEM_PASS_USER_COUNT) ?: 0L) + 1

            val updates = mutableMapOf<String, Any>(
                FIELD_TOTAL_COMPLETION_COUNT to totalCompletionCount,
                FIELD_ITEM_PASS_USER_COUNT to passUserCount,
                FIELD_AVG_REPLAY_COUNT to avgReplayCount(totalCompletionCount, passUserCount),
            )
            if (chestStars != null) {
                var oneStar = globalSnap.getLong(FIELD_ONE_STAR_COUNT) ?: 0L
                var twoStar = globalSnap.getLong(FIELD_TWO_STAR_COUNT) ?: 0L
                var threeStar = globalSnap.getLong(FIELD_THREE_STAR_COUNT) ?: 0L
                when (chestStars) {
                    1 -> oneStar += 1
                    2 -> twoStar += 1
                    3 -> threeStar += 1
                }
                updates[FIELD_ONE_STAR_COUNT] = oneStar
                updates[FIELD_TWO_STAR_COUNT] = twoStar
                updates[FIELD_THREE_STAR_COUNT] = threeStar
            }

            tx.set(docRef, updates, SetOptions.merge())
            null
        }.addOnFailureListener { e ->
            Log.w(TAG, "recordItemFirstFinish failed part=$partId position=$position", e)
        }
    }

    /**
     * Daha önce tamamen bitirilmiş (stepIsFinish=true) bir item kullanıcı tarafından tekrar
     * çözülüp yeniden bittiğinde çağrılır (chest'te tekrar oynama, lesson'da tekrar quiz çözme).
     * Yıldız dağılımına dokunmaz, sadece toplam tamamlama sayısını ve ortalama tekrar oranını günceller.
     */
    fun recordItemReplay(partId: Int, position: Int) {
        if (partId !in 1..8) return
        if (FirebaseAuth.getInstance().currentUser?.uid == null) return
        val docRef = itemRef(partId, position)

        FirebaseFirestore.getInstance().runTransaction { tx ->
            val globalSnap = tx.get(docRef)
            val totalCompletionCount = (globalSnap.getLong(FIELD_TOTAL_COMPLETION_COUNT) ?: 0L) + 1
            val passUserCount = globalSnap.getLong(FIELD_ITEM_PASS_USER_COUNT) ?: 0L
            tx.set(
                docRef,
                mapOf(
                    FIELD_TOTAL_COMPLETION_COUNT to totalCompletionCount,
                    FIELD_AVG_REPLAY_COUNT to avgReplayCount(totalCompletionCount, passUserCount),
                ),
                SetOptions.merge(),
            )
            null
        }.addOnFailureListener { e ->
            Log.w(TAG, "recordItemReplay failed part=$partId position=$position", e)
        }
    }

    private fun successRatePercent(passUserCount: Long, attemptUserCount: Long): Float {
        if (attemptUserCount <= 0L) return 0f
        return (passUserCount.toFloat() / attemptUserCount.toFloat()) * 100f
    }

    private fun avgReplayCount(totalCompletionCount: Long, passUserCount: Long): Float {
        if (passUserCount <= 0L) return 0f
        return totalCompletionCount.toFloat() / passUserCount.toFloat()
    }
}

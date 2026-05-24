package com.example.app

import android.util.Log
import com.example.app.model.LessonItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

/**
 * Firestore: `lessonLeaderboards/{boardDocId}` (meta) + `entries/{uid}`.
 * Kupa/altın/gümüş/bronz rozet satırları sezon sonunda Cloud Function [finalizeSeasonLeaderboardMedals] ile yazılır;
 * istemci yalnızca skor gönderir. Tahta üst dokümanında `season` / `partId` / `lessonIndex` tutulur (sorgu için).
 */
object LessonLeaderboardRepository {

    private const val TAG = "LessonLeaderboard"
    private const val COLLECTION = "lessonLeaderboards"
    private const val ENTRIES = "entries"

    private const val F_SCORE = "recordScore"
    private const val F_LABEL = "recordLabel"
    private const val F_NAME = "displayName"
    private const val F_PHOTO = "photoUrl"
    private const val F_UPDATED = "updatedAt"
    private const val F_TITLE_UNIT = "titleUnit"

    private const val META_PART_ID = "partId"
    private const val META_LESSON_INDEX = "lessonIndex"
    private const val META_SEASON = "season"

    /** Kupa liderliği → rozet [abacusLeaderboardRank] senkronu: bu parttaki ilk [LessonItem.TYPE_CHEST] satırının liste indeksi. */
    const val ABACUS_BADGE_LEADERBOARD_PART_ID = 1
    const val ABACUS_BADGE_LEADERBOARD_TOP_N = 100L

    /** Kayıt ekranı liderlik listesi: Firestore sorgu limiti (tahta seed ile uyumlu, max 100). */
    const val LEADERBOARD_LIST_QUERY_LIMIT = 100L

    /** Şablonda [partId] için ilk Chest satırının 0-tabanlı indeksi; yoksa null (senkron; rozet için [GlobalLessonData.resolveFirstChestLessonIndexForUser] tercih edin). */
    fun firstChestLessonIndexForPart(partId: Int): Int? {
        val idx = GlobalLessonData.createLessonItems(partId).indexOfFirst { it.type == LessonItem.TYPE_CHEST }
        return idx.takeIf { it >= 0 }
    }

    private const val USERS = "users"
    private const val BADGE_PROGRESS = "badgeProgress"
    private const val BADGE_STATE = "state"
    private const val F_ABACUS_LEADERBOARD_RANK = "abacusLeaderboardRank"

    /**
     * Sezon bazlı tahta doküman id'si (`lessonLeaderboards/{id}/entries/...`).
     * Firestore rules güncellenirken `*_season_*` kalıbına izin verildiğinden emin olun.
     */
    fun leaderboardDocumentId(
        partId: Int,
        lessonIndex: Int,
        season: Int = SeasonClock.currentSeason(),
    ): String =
        "part_${partId}_lesson_${lessonIndex}_season_$season"

    /**
     * [orderBy recordScore DESC] ile gelen doküman sırası için beraberlikte paylaşılan ödül sırası
     * (ör. 4. ve 5. aynı puanda ikisi de 4 — UI’da yine 4 ve 5 yazılır, rozetler 4. kademeden).
     */
    private fun DocumentSnapshot.rankingScore(): Int =
        when (val v = get(F_SCORE)) {
            is Long -> v.toInt()
            is Int -> v
            is Double -> v.toInt()
            else -> Int.MIN_VALUE
        }

    private fun competitionRanksForOrderedDocs(documents: List<DocumentSnapshot>): List<Int> {
        if (documents.isEmpty()) return emptyList()
        val scores = documents.map { it.rankingScore() }
        val ranks = IntArray(documents.size)
        ranks[0] = 1
        var currentRank = 1
        var prevScore = scores[0]
        for (i in 1 until documents.size) {
            val s = scores[i]
            if (s != prevScore) {
                currentRank = i + 1
            }
            ranks[i] = currentRank
            prevScore = s
        }
        return ranks.toList()
    }

    /**
     * Mevcut kayıttan daha iyi (yüksek) puan ise Firestore'a yazar.
     * [onComplete] işlem bitince (veya atlanınca) ana thread üzerinde çağrılır.
     */
    fun submitBestIfNeeded(
        partId: Int,
        lessonIndex: Int,
        recordScore: Int,
        season: Int = SeasonClock.currentSeason(),
        titleUnit: String? = null,
        onComplete: (() -> Unit)? = null,
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onComplete?.invoke()
            return
        }
        if (recordScore <= 0) {
            onComplete?.invoke()
            return
        }
        val db = FirebaseFirestore.getInstance()
        val boardId = leaderboardDocumentId(partId, lessonIndex, season)
        val boardRef = db.collection(COLLECTION).document(boardId)
        val entryRef = boardRef.collection(ENTRIES).document(user.uid)
        val titleTrimmed = titleUnit?.trim()?.take(127)?.takeIf { it.isNotEmpty() }

        db.runTransaction { transaction ->
            transaction.get(boardRef)
            val snapshot = transaction.get(entryRef)
            val previousBest = snapshot.getLong(F_SCORE)?.toInt()
            if (previousBest != null && recordScore <= previousBest) {
                return@runTransaction null
            }
            val data = hashMapOf<String, Any>(
                F_SCORE to recordScore,
                F_LABEL to recordScore.toString(),
                F_NAME to (user.displayName?.take(127)?.ifBlank { null } ?: "Kullanıcı"),
                F_UPDATED to FieldValue.serverTimestamp(),
                F_PHOTO to (user.photoUrl?.toString() ?: ""),
            )
            if (titleTrimmed != null) {
                data[F_TITLE_UNIT] = titleTrimmed
            }
            transaction.set(entryRef, data, SetOptions.merge())

            val boardMeta = hashMapOf<String, Any>(
                META_PART_ID to partId,
                META_LESSON_INDEX to lessonIndex,
                META_SEASON to season,
                F_UPDATED to FieldValue.serverTimestamp(),
            )
            if (titleTrimmed != null) {
                boardMeta[F_TITLE_UNIT] = titleTrimmed
            }
            transaction.set(boardRef, boardMeta, SetOptions.merge())
            null
        }
            .addOnSuccessListener {
                Log.d(
                    TAG,
                    "submitBestIfNeeded OK doc=$boardId uid=${user.uid.take(8)} score=$recordScore",
                )
                onComplete?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "submitBestIfNeeded failed part=$partId lesson=$lessonIndex", e)
                onComplete?.invoke()
            }
    }

    /**
     * İlk [topLimit] içindeki ödül sırası (beraberlikte paylaşılan en iyi sıra; sezon sonu rozet ile uyumlu).
     * Listede yoksa [rank]=null. [querySucceeded] false ise Firestore sorgusu başarısızdır ([rank] anlamsız).
     */
    fun fetchUserRankInLessonLeaderboard(
        partId: Int,
        lessonIndex: Int,
        userId: String,
        topLimit: Long = ABACUS_BADGE_LEADERBOARD_TOP_N,
        season: Int = SeasonClock.currentSeason(),
        onResult: (rank: Int?, querySucceeded: Boolean) -> Unit,
    ) {
        val db = FirebaseFirestore.getInstance()
        val boardId = leaderboardDocumentId(partId, lessonIndex, season)
        val uidNorm = userId.trim()
        Log.d(
            TAG,
            "fetchRank START collection=$COLLECTION board=$boardId entries order=$F_SCORE DESC limit=$topLimit " +
                "uidPrefix=${uidNorm.take(8)} uidLen=${uidNorm.length}",
        )
        db.collection(COLLECTION)
            .document(boardId)
            .collection(ENTRIES)
            .orderBy(F_SCORE, Query.Direction.DESCENDING)
            .limit(topLimit)
            .get()
            .addOnSuccessListener { snap ->
                val docs = snap.documents
                Log.d(TAG, "fetchRank snapshot size=${docs.size} board=$boardId")
                val preview = docs.take(12).mapIndexed { i, d ->
                    val sc = d.get(F_SCORE)
                    "#${i + 1} idLen=${d.id.length} id=${d.id.take(10)}.. score=$sc (${sc?.javaClass?.simpleName})"
                }
                Log.d(TAG, "fetchRank TOP: $preview")

                val idxTrim = docs.indexOfFirst { it.id.trim() == uidNorm }
                val idxRaw = docs.indexOfFirst { it.id == userId }
                val idx = when {
                    idxTrim >= 0 -> idxTrim
                    idxRaw >= 0 -> idxRaw
                    else -> -1
                }
                val ranks = competitionRanksForOrderedDocs(docs)
                if (idx < 0) {
                    Log.w(
                        TAG,
                        "fetchRank USER_NOT_IN_TOP board=$boardId idxTrim=$idxTrim idxRaw=$idxRaw " +
                            "(yanlış lessonIndex / farklı uid / kayıt orderBy dışında kaldı mı?)",
                    )
                    // Aynı path'te kullanıcı dokümanı var mı + ham skor tipi (orderBy dışı kalma teşhisi)
                    db.collection(COLLECTION).document(boardId).collection(ENTRIES).document(userId).get()
                        .addOnSuccessListener { udoc ->
                            if (!udoc.exists()) {
                                Log.w(TAG, "fetchRank direct entry: doc yok (hiç submit edilmemiş olabilir) board=$boardId")
                            } else {
                                val raw = udoc.get(F_SCORE)
                                Log.w(
                                    TAG,
                                    "fetchRank direct entry: EXISTS score=$raw (${raw?.javaClass?.simpleName}) " +
                                        "— sorgu sonucunda yoksa recordScore alanı/tipi veya indeks uyuşmazlığı şüphesi",
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "fetchRank direct entry read failed", e)
                        }
                } else {
                    Log.d(TAG, "fetchRank MATCH competitionRank=${ranks[idx]} board=$boardId (0-based idx=$idx)")
                }
                onResult(if (idx >= 0) ranks[idx] else null, true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "fetchRank QUERY_FAILED part=$partId lesson=$lessonIndex board=$boardId", e)
                onResult(null, false)
            }
    }

    /**
     * Part 1'deki ilk Chest ([LessonItem.TYPE_CHEST]) dersinin liderliğinde ilk 100 içindeki sırayı
     * `users/{uid}/badgeProgress/state.abacusLeaderboardRank` alanına yazar.
     * İlk 100'de yoksa [Int.MAX_VALUE] yazar. Partta Chest yoksa veya sorgu/yazım başarısızsa alanı güncellemez.
     */
    fun syncAbacusLeaderboardRankToBadgeProgress(uid: String, onFinished: () -> Unit) {
        if (uid.isBlank()) {
            onFinished()
            return
        }
        GlobalLessonData.resolveFirstChestLessonIndexForUser(uid, ABACUS_BADGE_LEADERBOARD_PART_ID) { chestIndex ->
            if (chestIndex == null) {
                Log.w(TAG, "syncAbacusLeaderboardRankToBadgeProgress: no TYPE_CHEST in part ${ABACUS_BADGE_LEADERBOARD_PART_ID}")
                onFinished()
                return@resolveFirstChestLessonIndexForUser
            }
            val partId = ABACUS_BADGE_LEADERBOARD_PART_ID
            GlobalLessonData.backfillLeaderboardFromStoredChest(uid, partId, chestIndex) {
                fetchUserRankInLessonLeaderboard(
                    partId = partId,
                    lessonIndex = chestIndex,
                    userId = uid,
                    topLimit = ABACUS_BADGE_LEADERBOARD_TOP_N,
                ) { rank, querySucceeded ->
                if (!querySucceeded) {
                    Log.w(TAG, "syncAbacusRank: fetchRank query başarısız, badge yazılmıyor")
                    onFinished()
                    return@fetchUserRankInLessonLeaderboard
                }
                if (rank == null) {
                    Log.w(
                        TAG,
                        "syncAbacusRank: rank=null (snapshot'ta yok) → abacusLeaderboardRank=${Int.MAX_VALUE} yazılacak chestIndex=$chestIndex",
                    )
                }
                val rankToStore = rank ?: Int.MAX_VALUE
                val badgeRef = FirebaseFirestore.getInstance()
                    .collection(USERS)
                    .document(uid)
                    .collection(BADGE_PROGRESS)
                    .document(BADGE_STATE)
                badgeRef.set(
                    mapOf(F_ABACUS_LEADERBOARD_RANK to rankToStore),
                    SetOptions.merge(),
                )
                    .addOnSuccessListener {
                        Log.d(
                            TAG,
                            "syncAbacusLeaderboardRankToBadgeProgress uid=${uid.take(8)} part=${ABACUS_BADGE_LEADERBOARD_PART_ID} chestIndex=$chestIndex rank=$rank stored=$rankToStore",
                        )
                        onFinished()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "syncAbacusLeaderboardRankToBadgeProgress write failed", e)
                        onFinished()
                    }
                }
            }
        }
    }

    data class LeaderboardEntry(
        val userId: String,
        /** Kayıt ekranında rozet üzerinde gösterilen sıra (1, 2, 3, … — Firestore sırası). */
        val displayRank: Int,
        /** Rozet rengi / sezon sonu ödül kademesi (aynı puanda paylaşılan en iyi sıra). */
        val rewardRank: Int,
        val displayName: String,
        val recordLabel: String,
        val photoUrl: String?,
    )

    fun listenLeaderboard(
        partId: Int,
        lessonIndex: Int,
        season: Int = SeasonClock.currentSeason(),
        onUpdate: (List<LeaderboardEntry>) -> Unit,
        onError: (Exception) -> Unit,
    ): ListenerRegistration {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection(COLLECTION)
            .document(leaderboardDocumentId(partId, lessonIndex, season))
            .collection(ENTRIES)
            .orderBy(F_SCORE, Query.Direction.DESCENDING)
            .limit(LEADERBOARD_LIST_QUERY_LIMIT)

        return q.addSnapshotListener { snapshot, e ->
            if (e != null) {
                onError(e)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                onUpdate(emptyList())
                return@addSnapshotListener
            }
            val docs = snapshot.documents
            val ranks = competitionRanksForOrderedDocs(docs)
            val list = docs.mapIndexed { index, doc ->
                LeaderboardEntry(
                    userId = doc.id,
                    displayRank = index + 1,
                    rewardRank = ranks[index],
                    displayName = doc.getString(F_NAME) ?: "",
                    recordLabel = doc.getString(F_LABEL) ?: "",
                    photoUrl = doc.getString(F_PHOTO)?.takeIf { it.isNotBlank() }
                )
            }
            onUpdate(list)
        }
    }
}

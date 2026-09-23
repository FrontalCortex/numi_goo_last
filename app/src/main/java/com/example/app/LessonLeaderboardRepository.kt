package com.example.app

import android.util.Log
import com.example.app.model.LessonItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions

/**
 * Firestore: `lessonLeaderboards/{boardDocId}` (meta) + `entries/{uid}`.
 * Kupa/altın/gümüş/bronz rozet satırları sezon sonunda Cloud Function [finalizeSeasonLeaderboardMedals] ile yazılır;
 * istemci yalnızca skor gönderir. Tahta üst dokümanında `season` / `partId` / `lessonKey` tutulur (sorgu için).
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
    private const val META_SEASON = "season"

    /** Kayıt ekranı liderlik listesi: Firestore sorgu limiti (tahta seed ile uyumlu, max 100). */
    const val LEADERBOARD_LIST_QUERY_LIMIT = 100L

    /** Şablonda [partId] için ilk Chest satırının 0-tabanlı indeksi; yoksa null. */
    fun firstChestLessonIndexForPart(partId: Int): Int? {
        val idx = GlobalLessonData.createLessonItems(partId).indexOfFirst { it.type == LessonItem.TYPE_CHEST }
        return idx.takeIf { it >= 0 }
    }

    /**
     * Sezon bazlı tahta doküman id'si (`lessonLeaderboards/{id}/entries/...`).
     * Firestore rules güncellenirken `*_season_*` kalıbına izin verildiğinden emin olun.
     */
    /**
     * Tahta kimliği artık liste konumuyla DEĞİL dersin kalıcı kimliğiyle kuruluyor
     * (`part_1_lesson_p1_i04_unite_maratonu_season_7`). Müfredata araya ders eklendiğinde
     * konum kayar ve eski biçimde tahta ikiye bölünürdü.
     *
     * Sunucu tarafı da aynı biçimi üretiyor: functions/index.js → submitLeaderboardScore.
     */
    fun leaderboardDocumentId(
        partId: Int,
        lessonKey: String,
        season: Int = SeasonClock.currentSeason(),
    ): String =
        "part_${partId}_lesson_${lessonKey}_season_$season"

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
     * Mevcut kayıttan daha iyi (yüksek) puan ise Cloud Function üzerinden Firestore'a yazar.
     * Sezon belirleme SUNUCU tarafında yapılır; cihaz saati manipülasyonuna karşı koruma.
     * [onComplete] işlem bitince (veya atlanınca) ana thread üzerinde çağrılır.
     */
    fun submitBestIfNeeded(
        partId: Int,
        lessonKey: String,
        recordScore: Int,
        @Suppress("UNUSED_PARAMETER") season: Int = SeasonClock.currentSeason(), // Artık sunucu belirliyor, parametre geriye dönük uyumluluk için tutuldu
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

        if (lessonKey.isBlank()) {
            Log.w(TAG, "submitBestIfNeeded: lessonKey boş, gönderilmiyor part=$partId")
            onComplete?.invoke()
            return
        }

        // Ad ve avatar GÖNDERİLMEZ: ikisi de başka çocukların ekranında gösteriliyor, bu yüzden
        // sunucu onları publicProfiles aynasından ve kimlik jetonundan kendisi çözüyor
        // (bkz. functions/index.js → submitLeaderboardScore). Eski sürüm istemciler hâlâ
        // gönderebilir; sunucu yok sayıyor.
        val data = hashMapOf<String, Any>(
            "partId" to partId,
            "lessonKey" to lessonKey,
            "recordScore" to recordScore,
        )
        val titleTrimmed = titleUnit?.trim()?.take(127)?.takeIf { it.isNotEmpty() }
        if (titleTrimmed != null) {
            data["titleUnit"] = titleTrimmed
        }

        FirebaseFunctions.getInstance()
            .getHttpsCallable("submitLeaderboardScore")
            .call(data)
            .addOnSuccessListener {
                Log.d(TAG, "submitBestIfNeeded CF OK part=$partId lesson=$lessonKey score=$recordScore")
                onComplete?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "submitBestIfNeeded CF failed part=$partId lesson=$lessonKey", e)
                onComplete?.invoke()
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
        lessonKey: String,
        season: Int = SeasonClock.currentSeason(),
        onUpdate: (List<LeaderboardEntry>) -> Unit,
        onError: (Exception) -> Unit,
    ): ListenerRegistration {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection(COLLECTION)
            .document(leaderboardDocumentId(partId, lessonKey, season))
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

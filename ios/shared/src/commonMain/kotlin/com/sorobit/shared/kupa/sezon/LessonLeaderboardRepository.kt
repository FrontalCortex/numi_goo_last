// Kaynak (Android): app/src/main/java/com/example/app/LessonLeaderboardRepository.kt — saf kısım:
// sabitler/yollar, leaderboardDocumentId, rankingScore, competitionRanksForOrderedDocs, submitBestIfNeeded'in
// ön koşulları ve Cloud Function yükü, fetchUserRankInLessonLeaderboard'un sıra bulma mantığı,
// syncAbacusLeaderboardRankToBadgeProgress'in yazdığı değer, LeaderboardEntry ve listenLeaderboard eşlemesi.
// Taşınmayanlar: Firestore/Functions/Auth çağrıları, Log satırları, GlobalLessonData'ya bağlı Chest çözümleme
// (yalnızca tür listesinden indeks bulan saf yardımcı var).
package com.sorobit.shared.kupa.sezon

/**
 * Firestore: `lessonLeaderboards/{boardDocId}` (meta) + `entries/{uid}`.
 * Kupa/altın/gümüş/bronz rozet satırları sezon sonunda Cloud Function [finalizeSeasonLeaderboardMedals] ile yazılır;
 * istemci yalnızca skor gönderir. Tahta üst dokümanında `season` / `partId` / `lessonIndex` tutulur (sorgu için).
 */
object LessonLeaderboardRepository {

    const val COLLECTION = "lessonLeaderboards"
    const val ENTRIES = "entries"

    const val F_SCORE = "recordScore"
    const val F_LABEL = "recordLabel"
    const val F_NAME = "displayName"
    const val F_PHOTO = "photoUrl"
    const val F_UPDATED = "updatedAt"
    const val F_TITLE_UNIT = "titleUnit"

    const val META_PART_ID = "partId"
    const val META_LESSON_INDEX = "lessonIndex"
    const val META_SEASON = "season"

    /** Skor gönderen Cloud Function (callable) adı. */
    const val CF_SUBMIT_LEADERBOARD_SCORE = "submitLeaderboardScore"

    /** Kupa liderliği → rozet abacusLeaderboardRank senkronu: bu parttaki ilk Chest satırının liste indeksi. */
    const val ABACUS_BADGE_LEADERBOARD_PART_ID = 1
    const val ABACUS_BADGE_LEADERBOARD_TOP_N = 100L

    /** Kayıt ekranı liderlik listesi: Firestore sorgu limiti (tahta seed ile uyumlu, max 100). */
    const val LEADERBOARD_LIST_QUERY_LIMIT = 100L

    /** Android `LessonItem.TYPE_CHEST`. */
    const val LESSON_ITEM_TYPE_CHEST = 2

    /**
     * Android `firstChestLessonIndexForPart(partId)`: şablondaki ilk Chest satırının 0-tabanlı indeksi; yoksa null.
     * Ders şablonu başka pakette olduğundan öğe türleri (LessonItem.type) liste olarak verilir.
     */
    fun firstChestLessonIndexForPart(lessonItemTypes: List<Int>): Int? {
        val idx = lessonItemTypes.indexOfFirst { it == LESSON_ITEM_TYPE_CHEST }
        return idx.takeIf { it >= 0 }
    }

    const val USERS = "users"
    const val BADGE_PROGRESS = "badgeProgress"
    const val BADGE_STATE = "state"
    const val F_ABACUS_LEADERBOARD_RANK = "abacusLeaderboardRank"

    /**
     * Sezon bazlı tahta doküman id'si (`lessonLeaderboards/{id}/entries/...`).
     */
    fun leaderboardDocumentId(
        partId: Int,
        lessonIndex: Int,
        season: Int = SeasonClock.currentSeason(),
    ): String =
        "part_${partId}_lesson_${lessonIndex}_season_$season"

    /** `lessonLeaderboards/{boardId}/entries` koleksiyon yolu (sorgu: orderBy recordScore DESC, limit N). */
    fun entriesCollectionPath(
        partId: Int,
        lessonIndex: Int,
        season: Int = SeasonClock.currentSeason(),
    ): String = "$COLLECTION/${leaderboardDocumentId(partId, lessonIndex, season)}/$ENTRIES"

    /**
     * Android `DocumentSnapshot.rankingScore()`: `recordScore` alanının sıralama değeri.
     * Yalnızca Long / Int / Double tanınır; diğer her şey (Float dahil, alan yok) Int.MIN_VALUE sayılır.
     */
    fun rankingScore(recordScoreValue: Any?): Int =
        when (val v = recordScoreValue) {
            is Long -> v.toInt()
            is Int -> v
            is Double -> v.toInt()
            else -> Int.MIN_VALUE
        }

    /**
     * `orderBy recordScore DESC` ile gelen sıra için beraberlikte paylaşılan ödül sırası
     * (ör. 4. ve 5. aynı puanda ikisi de 4 — arayüzde yine 4 ve 5 yazılır, rozetler 4. kademeden).
     *
     * @param scores sıralı dokümanların [rankingScore] değerleri.
     */
    fun competitionRanksForOrderedScores(scores: List<Int>): List<Int> {
        if (scores.isEmpty()) return emptyList()
        val ranks = IntArray(scores.size)
        ranks[0] = 1
        var currentRank = 1
        var prevScore = scores[0]
        for (i in 1 until scores.size) {
            val s = scores[i]
            if (s != prevScore) {
                currentRank = i + 1
            }
            ranks[i] = currentRank
            prevScore = s
        }
        return ranks.toList()
    }

    /** Android `competitionRanksForOrderedDocs`: dokümanların ham `recordScore` değerlerinden sıralar. */
    fun competitionRanksForOrderedDocs(recordScoreValues: List<Any?>): List<Int> =
        competitionRanksForOrderedScores(recordScoreValues.map { rankingScore(it) })

    /**
     * Android `submitBestIfNeeded` içinde Cloud Function'a gönderilen yük; gönderim atlanacaksa null
     * (kullanıcı yok ya da `recordScore <= 0`). Sezonu SUNUCU belirler; yükte sezon yoktur.
     *
     * @param hasUser oturum açmış kullanıcı var mı (Android: `FirebaseAuth.currentUser != null`).
     * @param userDisplayName `user.displayName`
     * @param userPhotoUrl `user.photoUrl?.toString()`
     */
    fun buildSubmitData(
        hasUser: Boolean,
        partId: Int,
        lessonIndex: Int,
        recordScore: Int,
        userDisplayName: String?,
        userPhotoUrl: String?,
        titleUnit: String? = null,
    ): Map<String, Any>? {
        if (!hasUser) return null
        if (recordScore <= 0) return null

        val data = mutableMapOf<String, Any>(
            "partId" to partId,
            "lessonIndex" to lessonIndex,
            "recordScore" to recordScore,
            "displayName" to (userDisplayName?.trim()?.take(127)?.ifBlank { null } ?: "Kullanıcı"),
            "photoUrl" to (userPhotoUrl ?: ""),
        )
        val titleTrimmed = titleUnit?.trim()?.take(127)?.takeIf { it.isNotEmpty() }
        if (titleTrimmed != null) {
            data["titleUnit"] = titleTrimmed
        }
        return data
    }

    /**
     * Android `fetchUserRankInLessonLeaderboard` başarı dalı: ilk N içindeki ödül sırası (beraberlikte paylaşılan
     * en iyi sıra); listede yoksa null. Önce kırpılmış id eşleşmesi, sonra ham id eşleşmesi aranır.
     *
     * @param orderedEntries `orderBy recordScore DESC limit N` sonucu: (doküman id, ham recordScore değeri).
     */
    fun userRankInOrderedEntries(orderedEntries: List<Pair<String, Any?>>, userId: String): Int? {
        val uidNorm = userId.trim()
        val idxTrim = orderedEntries.indexOfFirst { it.first.trim() == uidNorm }
        val idxRaw = orderedEntries.indexOfFirst { it.first == userId }
        val idx = when {
            idxTrim >= 0 -> idxTrim
            idxRaw >= 0 -> idxRaw
            else -> -1
        }
        val ranks = competitionRanksForOrderedDocs(orderedEntries.map { it.second })
        return if (idx >= 0) ranks[idx] else null
    }

    /**
     * Android `syncAbacusLeaderboardRankToBadgeProgress`: `users/{uid}/badgeProgress/state.abacusLeaderboardRank`
     * alanına yazılacak değer. İlk 100'de yoksa (rank == null) Int.MAX_VALUE yazılır.
     * (Sorgu başarısızsa ya da partta Chest yoksa alan HİÇ güncellenmez — o karar platform katmanındadır.)
     */
    fun abacusLeaderboardRankToStore(rank: Int?): Int = rank ?: Int.MAX_VALUE

    /** [abacusLeaderboardRankToStore] değerinin merge ile yazıldığı harita. */
    fun abacusLeaderboardRankUpdate(rank: Int?): Map<String, Any> =
        mapOf(F_ABACUS_LEADERBOARD_RANK to abacusLeaderboardRankToStore(rank))

    fun badgeStateDocumentPath(uid: String): String = "$USERS/$uid/$BADGE_PROGRESS/$BADGE_STATE"

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

    /**
     * Android `listenLeaderboard` anlık görüntü eşlemesi.
     *
     * @param documents `orderBy recordScore DESC limit LEADERBOARD_LIST_QUERY_LIMIT` sonucu: (doküman id, veri).
     */
    fun mapLeaderboardEntries(documents: List<Pair<String, Map<String, Any?>>>): List<LeaderboardEntry> {
        val ranks = competitionRanksForOrderedDocs(documents.map { it.second[F_SCORE] })
        return documents.mapIndexed { index, (id, data) ->
            LeaderboardEntry(
                userId = id,
                displayRank = index + 1,
                rewardRank = ranks[index],
                displayName = (data[F_NAME] as? String) ?: "",
                recordLabel = (data[F_LABEL] as? String) ?: "",
                photoUrl = (data[F_PHOTO] as? String)?.takeIf { it.isNotBlank() }
            )
        }
    }
}

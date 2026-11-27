// Kaynak (Android): app/src/main/java/com/example/app/LessonLeaderboardRepository.kt — saf kısım:
// sabitler/yollar, leaderboardDocumentId (ders anahtarıyla), rankingScore, competitionRanksForOrderedDocs,
// submitBestIfNeeded'in ön koşulları ve Cloud Function yükü, LeaderboardEntry ve listenLeaderboard eşlemesi
// (avatar eşlemesi dahil).
// Android'de kaldırılanlar ortak koddan da çıkarıldı: fetchUserRankInLessonLeaderboard ve
// syncAbacusLeaderboardRankToBadgeProgress (rozetin abacusLeaderboardRank alanı artık yok).
// Taşınmayanlar: Firestore/Functions/Auth çağrıları, Log satırları, GlobalLessonData'ya bağlı Chest çözümleme
// (yalnızca tür listesinden indeks bulan saf yardımcı var).
package com.sorobit.shared.kupa.sezon

/**
 * Firestore: `lessonLeaderboards/{boardDocId}` (meta) + `entries/{uid}`.
 * Kupa/altın/gümüş/bronz rozet satırları sezon sonunda Cloud Function [finalizeSeasonLeaderboardMedals] ile yazılır;
 * istemci yalnızca skor gönderir. Tahta üst dokümanında `season` / `partId` / `lessonKey` tutulur (sorgu için).
 */
object LessonLeaderboardRepository {

    const val COLLECTION = "lessonLeaderboards"
    const val ENTRIES = "entries"

    const val F_SCORE = "recordScore"
    const val F_LABEL = "recordLabel"
    const val F_NAME = "displayName"
    const val F_UPDATED = "updatedAt"
    const val F_TITLE_UNIT = "titleUnit"

    const val META_PART_ID = "partId"
    const val META_LESSON_KEY = "lessonKey"
    const val META_SEASON = "season"

    /** Skor gönderen Cloud Function (callable) adı. */
    const val CF_SUBMIT_LEADERBOARD_SCORE = "submitLeaderboardScore"

    /** Kayıt ekranı liderlik listesi: Firestore sorgu limiti (tahta seed ile uyumlu, max 100). */
    const val LEADERBOARD_LIST_QUERY_LIMIT = 100L

    /** Avatar okuması için Firestore `whereIn` en fazla 30 değer alır. */
    const val AVATAR_QUERY_CHUNK = 30

    /** Avatar kaynağı: `publicProfiles/{uid}` dokümanı. */
    const val PUBLIC_PROFILES_COLLECTION = "publicProfiles"

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

    /**
     * Sezon bazlı tahta doküman id'si (`lessonLeaderboards/{id}/entries/...`).
     *
     * Android (güncel): tahta kimliği liste konumuyla DEĞİL dersin kalıcı kimliğiyle ([lessonKey], ör.
     * `p1_i04_unite_maratonu`) kurulur: müfredata ders eklenince konum kayar ve eski biçimde tahta ikiye bölünürdü.
     * Sunucu (functions/index.js → submitLeaderboardScore) aynı biçimi üretir.
     */
    fun leaderboardDocumentId(
        partId: Int,
        lessonKey: String,
        season: Int = SeasonClock.currentSeason(),
    ): String =
        "part_${partId}_lesson_${lessonKey}_season_$season"

    /** `lessonLeaderboards/{boardId}/entries` koleksiyon yolu (sorgu: orderBy recordScore DESC, limit N). */
    fun entriesCollectionPath(
        partId: Int,
        lessonKey: String,
        season: Int = SeasonClock.currentSeason(),
    ): String = "$COLLECTION/${leaderboardDocumentId(partId, lessonKey, season)}/$ENTRIES"

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
     * (kullanıcı yok, `recordScore <= 0` ya da [lessonKey] boş). Sezonu SUNUCU belirler; yükte sezon yoktur.
     *
     * Android (güncel): ad ve avatar GÖNDERİLMEZ — ikisi de başka çocukların ekranında gösterildiği için sunucu
     * onları publicProfiles aynasından ve kimlik jetonundan kendisi çözer.
     *
     * @param hasUser oturum açmış kullanıcı var mı (Android: `FirebaseAuth.currentUser != null`).
     */
    fun buildSubmitData(
        hasUser: Boolean,
        partId: Int,
        lessonKey: String,
        recordScore: Int,
        titleUnit: String? = null,
    ): Map<String, Any>? {
        if (!hasUser) return null
        if (recordScore <= 0) return null
        if (lessonKey.isBlank()) return null

        val data = mutableMapOf<String, Any>(
            "partId" to partId,
            "lessonKey" to lessonKey,
            "recordScore" to recordScore,
        )
        val titleTrimmed = titleUnit?.trim()?.take(127)?.takeIf { it.isNotEmpty() }
        if (titleTrimmed != null) {
            data["titleUnit"] = titleTrimmed
        }
        return data
    }

    data class LeaderboardEntry(
        val userId: String,
        /** Kayıt ekranında rozet üzerinde gösterilen sıra (1, 2, 3, … — Firestore sırası). */
        val displayRank: Int,
        /** Rozet rengi / sezon sonu ödül kademesi (aynı puanda paylaşılan en iyi sıra). */
        val rewardRank: Int,
        val displayName: String,
        val recordLabel: String,
        /**
         * Uygulama içi avatar (publicProfiles.avatarConfig, AvatarConfig.encode biçiminde).
         * null: henüz okunmadı ya da kullanıcının avatarı yok → çizen taraf varsayılan avatarı koyar.
         * (Android güncel: eski `photoUrl` — Google fotoğrafı — artık gösterilmiyor.)
         */
        val avatarConfig: String?,
    )

    /**
     * Android `listenLeaderboard` içindeki `buildList`: anlık görüntü eşlemesi.
     *
     * @param documents `orderBy recordScore DESC limit LEADERBOARD_LIST_QUERY_LIMIT` sonucu: (doküman id, veri).
     * @param avatarCache uid → avatarConfig ("" = profilde avatar yok); boş değerler null sayılır.
     * @param ownUid [ownAvatarConfig] ile birlikte: kendi satırımız için yerel avatar (ağ beklenmez).
     */
    fun mapLeaderboardEntries(
        documents: List<Pair<String, Map<String, Any?>>>,
        avatarCache: Map<String, String> = emptyMap(),
        ownUid: String? = null,
        ownAvatarConfig: String? = null,
    ): List<LeaderboardEntry> {
        val ranks = competitionRanksForOrderedDocs(documents.map { it.second[F_SCORE] })
        val useOwnLocal = !ownUid.isNullOrBlank() && !ownAvatarConfig.isNullOrBlank()
        return documents.mapIndexed { index, (id, data) ->
            val avatar = if (useOwnLocal && id == ownUid) ownAvatarConfig
            else avatarCache[id]?.takeIf { it.isNotBlank() }
            LeaderboardEntry(
                userId = id,
                displayRank = index + 1,
                rewardRank = ranks[index],
                displayName = (data[F_NAME] as? String) ?: "",
                recordLabel = (data[F_LABEL] as? String) ?: "",
                avatarConfig = avatar,
            )
        }
    }

    /**
     * Android `listenLeaderboard`: avatarı henüz okunmamış uid'ler (kendi satırımız yerelden geldiyse hariç);
     * [AVATAR_QUERY_CHUNK]'lık parçalara bölünüp `publicProfiles`'tan sorgulanır.
     */
    fun missingAvatarChunks(
        uids: List<String>,
        avatarCache: Map<String, String>,
        ownUid: String? = null,
        ownAvatarConfig: String? = null,
    ): List<List<String>> {
        val useOwnLocal = !ownUid.isNullOrBlank() && !ownAvatarConfig.isNullOrBlank()
        return uids
            .filterNot { useOwnLocal && it == ownUid }
            .filterNot { avatarCache.containsKey(it) }
            .chunked(AVATAR_QUERY_CHUNK)
    }
}

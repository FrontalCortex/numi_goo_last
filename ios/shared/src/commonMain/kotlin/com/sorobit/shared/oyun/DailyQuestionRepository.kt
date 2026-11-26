// Kaynak: DailyQuestionRepository.kt — saf kısım: alan adları, yollar, Firestore eşlemesi
// (parseChallengeDocument / parseSlot / challengeToFirestoreMap), kart durumu, başarı oranı ve
// çözülen soru sayısı hesabı, yerel önbellek güncelleme kuralları.
// Dışarıda bırakılan: FirebaseAuth / Firestore çağrıları (get/set/update/runTransaction) ve bunların
// dinleyici zinciri. iOS tarafı aynı sırayı bu dosyadaki yardımcılarla kurar.
// Farklar:
//  - Yerel önbellek Android'de Gson JSON'u olarak SharedPreferences'ta duruyordu; burada JSON
//    kütüphanesi olmadığı için [DailyQuestionChallengeCache] arayüzü var (biçim platforma bırakıldı;
//    önbellek cihazlar arası paylaşılmadığı için Android biçimiyle uyum gerekmez).
//  - `lowercase(Locale.ROOT)` → `lowercase()` (ortak Kotlin'de zaten yerelden bağımsız).
//  - `questions` listesinde Map olmayan öğe Android'de ClassCastException'a yol açardı; burada öğe
//    atlanır (sonuçta soru sayısı 3 olmayacağı için belge geçersiz sayılır).
package com.sorobit.shared.oyun

/** Günlük soru meydan okumasının yerel önbelleği (Android: prefs içinde Gson JSON). */
interface DailyQuestionChallengeCache {
    fun read(cacheKey: String): DailyQuestionChallenge?
    fun write(cacheKey: String, challenge: DailyQuestionChallenge)
}

/** Bellek içi önbellek (testler için). */
class InMemoryDailyQuestionChallengeCache : DailyQuestionChallengeCache {
    private val map = mutableMapOf<String, DailyQuestionChallenge>()
    override fun read(cacheKey: String): DailyQuestionChallenge? = map[cacheKey]
    override fun write(cacheKey: String, challenge: DailyQuestionChallenge) { map[cacheKey] = challenge }
}

object DailyQuestionRepository {

    // ── Firestore yolları ───────────────────────────────────────────────────
    const val COLLECTION_USERS = "users"
    const val DOC_CURRENT = "current"
    const val COLLECTION_SUCCESS_RATE = "successRate"
    const val DOC_DAILY_QUESTION_SUCCESS_RATE = "dailyQuestionSuccessRate"

    // ── Firestore alan adları (users/{uid}/dailyQuestion/current) ───────────
    const val FIELD_PERIOD_KEY = "periodKey"
    const val FIELD_SOLVED_COUNT = "solvedCount"
    const val FIELD_REWARD_CLAIMED = "rewardClaimed"
    const val FIELD_PENDING_CONTINUE_SLOT = "pendingContinueSlotIndex"
    const val FIELD_QUESTIONS = "questions"
    const val FIELD_SEQUENCE = "sequence"
    const val FIELD_MATH_FIRST = "mathFirst"
    const val FIELD_MATH_OPERATOR = "mathOperator"
    const val FIELD_MATH_SECOND = "mathSecond"
    const val FIELD_PART_ID = "partId"
    const val FIELD_ITEM_INDEX = "itemIndex"
    const val FIELD_TITLE_UNIT = "titleUnit"
    const val FIELD_DIFFICULTY = "difficulty"
    const val FIELD_DISPLAY_INTERVAL = "displayIntervalMs"

    /** `users/{uid}/dailyQuestion/current` */
    fun challengeDocPath(uid: String): String =
        "$COLLECTION_USERS/$uid/${DailyQuestionPeriod.FIRESTORE_COLLECTION}/$DOC_CURRENT"

    fun challengeCacheKey(uid: String): String =
        "daily_challenge_${uid}_current"

    fun readChallengeFromPrefs(
        cache: DailyQuestionChallengeCache,
        cacheKey: String,
        periodKey: String,
    ): DailyQuestionChallenge? {
        val challenge = try {
            cache.read(cacheKey)
        } catch (_: Exception) {
            null
        } ?: return null
        return if (challenge.periodKey != periodKey) null else challenge
    }

    /**
     * `loadOrCreateChallenge` başarı dalındaki karar: belge varsa ve geçerliyse (3 soru) onu döndürür,
     * değilse null (→ çağıran yeni meydan okuma üretip yazar).
     */
    fun resolveLoadedChallenge(
        docExists: Boolean,
        data: Map<String, Any?>?,
        periodKey: String,
    ): DailyQuestionChallenge? {
        val parsed = if (docExists) parseChallengeDocument(data, periodKey) else null
        if (parsed != null && parsed.questions.size == DailyQuestionPeriod.QUESTIONS_PER_PERIOD) {
            return parsed
        }
        return null
    }

    fun parseChallengeDocument(
        data: Map<String, Any?>?,
        periodKey: String,
    ): DailyQuestionChallenge? {
        if (data == null) return null
        val docPeriodKey = data[FIELD_PERIOD_KEY] as? String
        if (docPeriodKey != periodKey) return null

        val solvedCount = (data[FIELD_SOLVED_COUNT] as? Number)?.toInt() ?: 0
        val rewardClaimed = data[FIELD_REWARD_CLAIMED] as? Boolean == true
        val pendingContinueSlotIndex = (data[FIELD_PENDING_CONTINUE_SLOT] as? Number)?.toInt()
            ?.coerceIn(0, DailyQuestionPeriod.QUESTIONS_PER_PERIOD - 1)
        val rawQuestions = data[FIELD_QUESTIONS] as? List<*> ?: return null
        val questions = rawQuestions.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            parseSlot(map)
        }
        if (questions.size != DailyQuestionPeriod.QUESTIONS_PER_PERIOD) return null
        return DailyQuestionChallenge(
            periodKey = periodKey,
            solvedCount = solvedCount,
            questions = questions,
            rewardClaimed = rewardClaimed,
            pendingContinueSlotIndex = pendingContinueSlotIndex,
        )
    }

    fun parseSlot(map: Map<*, *>): DailyQuestionSlot? {
        val sequenceCsv = map[FIELD_SEQUENCE] as? String
        val sequence = sequenceCsv?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()

        val mathFirst = (map[FIELD_MATH_FIRST] as? Number)?.toInt()
        val mathOperator = map[FIELD_MATH_OPERATOR] as? String
        val mathSecond = (map[FIELD_MATH_SECOND] as? Number)?.toInt()
        val mathOperation = if (mathFirst != null && mathOperator != null && mathSecond != null) {
            MathOperation(mathFirst, mathOperator, mathSecond)
        } else null

        if (sequence.isEmpty() && mathOperation == null) return null

        val partId = (map[FIELD_PART_ID] as? Number)?.toInt() ?: 1
        val itemIndex = (map[FIELD_ITEM_INDEX] as? Number)?.toInt() ?: return null
        val titleUnit = map[FIELD_TITLE_UNIT] as? String ?: return null
        val difficulty = map[FIELD_DIFFICULTY] as? String ?: return null
        val displayIntervalMs = (map[FIELD_DISPLAY_INTERVAL] as? Number)?.toLong()
        return DailyQuestionSlot(
            sequence = sequence,
            mathOperation = mathOperation,
            partId = partId,
            itemIndex = itemIndex,
            titleUnit = titleUnit,
            difficulty = difficulty,
            displayIntervalMs = displayIntervalMs,
        )
    }

    fun challengeToFirestoreMap(challenge: DailyQuestionChallenge): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            FIELD_PERIOD_KEY to challenge.periodKey,
            FIELD_SOLVED_COUNT to challenge.solvedCount,
            FIELD_REWARD_CLAIMED to challenge.rewardClaimed,
            FIELD_QUESTIONS to challenge.questions.map { slot ->
                val slotMap = mutableMapOf<String, Any>(
                    FIELD_PART_ID to slot.partId,
                    FIELD_ITEM_INDEX to slot.itemIndex,
                    FIELD_TITLE_UNIT to slot.titleUnit,
                    FIELD_DIFFICULTY to slot.difficulty,
                )
                if (slot.sequence.isNotEmpty()) {
                    slotMap[FIELD_SEQUENCE] = slot.sequence.joinToString(",")
                }
                if (slot.mathOperation != null) {
                    slot.mathOperation.firstNumber?.let { slotMap[FIELD_MATH_FIRST] = it }
                    slot.mathOperation.operator?.let { slotMap[FIELD_MATH_OPERATOR] = it }
                    slot.mathOperation.secondNumber?.let { slotMap[FIELD_MATH_SECOND] = it }
                }
                slot.displayIntervalMs?.let { slotMap[FIELD_DISPLAY_INTERVAL] = it }
                slotMap
            },
        )
        challenge.pendingContinueSlotIndex?.let { map[FIELD_PENDING_CONTINUE_SLOT] = it }
        return map
    }

    fun toCardUiState(challenge: DailyQuestionChallenge, poolAvailable: Boolean): DailyQuestionCardUiState {
        return DailyQuestionCardUiState(
            periodKey = challenge.periodKey,
            solvedCount = challenge.solvedCount,
            totalCount = DailyQuestionPeriod.QUESTIONS_PER_PERIOD,
            titleUnit = challenge.cardTitleUnit(),
            difficulty = challenge.cardDifficultyLabel(),
            isComplete = challenge.isComplete,
            rewardClaimed = challenge.rewardClaimed,
            poolAvailable = poolAvailable,
            pendingContinueSlotIndex = challenge.pendingContinueSlotIndex,
            isLoaded = true,
        )
    }

    /** `loadChallengeForCard`: meydan okuma yokken (null) gösterilen kart durumu. */
    fun cardUiStateWithoutChallenge(periodKey: String, poolAvailable: Boolean): DailyQuestionCardUiState =
        DailyQuestionCardUiState(
            periodKey = periodKey,
            solvedCount = 0,
            totalCount = DailyQuestionPeriod.QUESTIONS_PER_PERIOD,
            titleUnit = "",
            difficulty = "",
            isComplete = false,
            rewardClaimed = false,
            poolAvailable = poolAvailable,
            isLoaded = true,
        )

    // ── Küresel başarı oranı (successRate/dailyQuestionSuccessRate/…) ───────

    /** "Kolay"/"Orta"/"Zor" → "easy"/"medium"/"hard"; tanınmazsa null (Android: işlem atlanır). */
    fun difficultyKey(difficulty: String): String? = when (difficulty.lowercase()) {
        "kolay" -> "easy"
        "orta" -> "medium"
        "zor" -> "hard"
        else -> null
    }

    fun partCollectionName(partId: Int): String = "generatorForPart${partId}Chest"

    /** Belge kimliği: ünite başlığındaki "/" Firestore yolunu bozmasın diye "-" yapılır. */
    fun successRateDocId(titleUnit: String): String = titleUnit.replace("/", "-")

    /** `successRate/dailyQuestionSuccessRate/generatorForPart{N}Chest/{titleUnit}` */
    fun successRateDocPath(slot: DailyQuestionSlot): String =
        "$COLLECTION_SUCCESS_RATE/$DOC_DAILY_QUESTION_SUCCESS_RATE/" +
            "${partCollectionName(slot.partId)}/${successRateDocId(slot.titleUnit)}"

    fun rateField(difficultyKey: String): String = "${difficultyKey}_rate"
    fun totalField(difficultyKey: String): String = "${difficultyKey}_total"
    fun successField(difficultyKey: String): String = "${difficultyKey}_success"

    /**
     * `recordQuestionResult` transaction gövdesindeki hesap: yeni toplam/başarı/oran.
     * Oran tam sayıya AŞAĞI yuvarlanır (Float bölme → toInt), Android ile aynı.
     */
    fun successRateUpdates(
        difficultyKey: String,
        currentTotal: Long?,
        currentSuccess: Long?,
        isSuccess: Boolean,
    ): Map<String, Any> {
        val total = currentTotal ?: 0L
        val success = currentSuccess ?: 0L

        val newTotal = total + 1
        val newSuccess = if (isSuccess) success + 1 else success
        val newRate = ((newSuccess.toFloat() / newTotal.toFloat()) * 100).toInt()

        return mapOf(
            totalField(difficultyKey) to newTotal,
            successField(difficultyKey) to newSuccess,
            rateField(difficultyKey) to newRate,
        )
    }

    /** `incrementSolvedCount` transaction gövdesindeki hesap. */
    fun nextSolvedCount(currentSolvedCount: Long?): Int {
        val current = (currentSolvedCount ?: 0L).toInt().coerceAtLeast(0)
        return (current + 1).coerceAtMost(DailyQuestionPeriod.QUESTIONS_PER_PERIOD)
    }

    /** `markPendingDiamondContinue`: yazılacak slot 0..2 aralığına sıkıştırılır. */
    fun safePendingContinueSlot(slotIndex: Int): Int =
        slotIndex.coerceIn(0, DailyQuestionPeriod.QUESTIONS_PER_PERIOD - 1)

    // ── Yerel önbellek güncellemeleri (Firestore sonucu ne olursa olsun uygulanır) ──

    /** `markRewardClaimed`: başarıda da hatada da önbellek güncellenir. */
    fun cacheRewardClaimed(cache: DailyQuestionChallengeCache, cacheKey: String, periodKey: String) {
        readChallengeFromPrefs(cache, cacheKey, periodKey)?.let { cached ->
            val updated = cached.copy(rewardClaimed = true)
            cache.write(cacheKey, updated)
        }
    }

    fun updateCachedPendingContinue(
        cache: DailyQuestionChallengeCache,
        cacheKey: String,
        periodKey: String,
        slotIndex: Int?,
    ) {
        readChallengeFromPrefs(cache, cacheKey, periodKey)?.let { cached ->
            val updated = cached.copy(pendingContinueSlotIndex = slotIndex)
            cache.write(cacheKey, updated)
        }
    }

    /** `incrementSolvedCount` başarı dalı: sayaç yazılır, bekleyen "devam" temizlenir. */
    fun cacheSolvedCount(
        cache: DailyQuestionChallengeCache,
        cacheKey: String,
        periodKey: String,
        newCount: Int,
    ) {
        readChallengeFromPrefs(cache, cacheKey, periodKey)?.let { cached ->
            val updated = cached.copy(
                solvedCount = newCount,
                rewardClaimed = cached.rewardClaimed,
                pendingContinueSlotIndex = null,
            )
            cache.write(cacheKey, updated)
        }
    }
}

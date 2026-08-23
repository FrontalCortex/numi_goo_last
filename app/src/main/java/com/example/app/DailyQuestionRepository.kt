package com.example.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DailyQuestionRepository {

    private val gson = Gson()

    fun loadOrCreateChallenge(
        context: Context,
        sources: List<DailyQuestionSource>,
        onResult: (DailyQuestionChallenge?) -> Unit,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(null)
            return
        }
        val periodKey = DailyQuestionPeriod.currentPeriodKey()
        val prefs = context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
        val cacheKey = challengeCacheKey(uid)

        val docRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection(DailyQuestionPeriod.FIRESTORE_COLLECTION)
            .document("current")

        docRef.get()
            .addOnSuccessListener { doc ->
                val parsed = if (doc.exists()) parseChallengeDocument(doc.data, periodKey) else null
                if (parsed != null && parsed.questions.size == DailyQuestionPeriod.QUESTIONS_PER_PERIOD) {
                    prefs.edit().putString(cacheKey, gson.toJson(parsed)).apply()
                    onResult(parsed)
                    return@addOnSuccessListener
                }
                createAndPersistChallenge(context, uid, periodKey, sources, prefs, cacheKey, docRef, onResult)
            }
            .addOnFailureListener {
                val cached = readChallengeFromPrefs(prefs, cacheKey, periodKey)
                if (cached != null) {
                    onResult(cached)
                } else {
                    createAndPersistChallenge(context, uid, periodKey, sources, prefs, cacheKey, docRef, onResult)
                }
            }
    }

    fun loadChallengeForCard(
        context: Context,
        sources: List<DailyQuestionSource>,
        onResult: (DailyQuestionCardUiState?) -> Unit,
    ) {
        loadOrCreateChallenge(context, sources) { challenge ->
            if (challenge == null) {
                onResult(
                    DailyQuestionCardUiState(
                        periodKey = DailyQuestionPeriod.currentPeriodKey(),
                        solvedCount = 0,
                        totalCount = DailyQuestionPeriod.QUESTIONS_PER_PERIOD,
                        titleUnit = "",
                        difficulty = "",
                        isComplete = false,
                        rewardClaimed = false,
                        poolAvailable = sources.isNotEmpty(),
                        isLoaded = true,
                    ),
                )
                return@loadOrCreateChallenge
            }

            val slot = challenge.slotForPlay()
            if (slot == null) {
                onResult(challenge.toCardUiState(poolAvailable = sources.isNotEmpty()))
                return@loadOrCreateChallenge
            }

            val difficultyKey = when (slot.difficulty.lowercase(java.util.Locale.ROOT)) {
                "kolay" -> "easy"
                "orta" -> "medium"
                "zor" -> "hard"
                else -> {
                    onResult(challenge.toCardUiState(poolAvailable = sources.isNotEmpty()))
                    return@loadOrCreateChallenge
                }
            }

            val partCollectionName = "generatorForPart${slot.partId}Chest"

            FirebaseFirestore.getInstance()
                .collection("successRate")
                .document("dailyQuestionSuccessRate")
                .collection(partCollectionName)
                .document(slot.titleUnit.replace("/", "-"))
                .get()
                .addOnSuccessListener { doc ->
                    var rate: Int? = null
                    if (doc.exists()) {
                        rate = doc.getLong("${difficultyKey}_rate")?.toInt()
                    }
                    val state = challenge.toCardUiState(poolAvailable = sources.isNotEmpty())
                    onResult(state.copy(globalSuccessRate = rate))
                }
                .addOnFailureListener {
                    onResult(challenge.toCardUiState(poolAvailable = sources.isNotEmpty()))
                }
        }
    }

    fun markRewardClaimed(
        context: Context,
        periodKey: String,
        onResult: (Boolean) -> Unit,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(false)
            return
        }
        val prefs = context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
        val cacheKey = challengeCacheKey(uid)
        val docRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection(DailyQuestionPeriod.FIRESTORE_COLLECTION)
            .document("current")

        docRef.set(mapOf(FIELD_REWARD_CLAIMED to true), SetOptions.merge())
            .addOnSuccessListener {
                readChallengeFromPrefs(prefs, cacheKey, periodKey)?.let { cached ->
                    val updated = cached.copy(rewardClaimed = true)
                    prefs.edit().putString(cacheKey, gson.toJson(updated)).apply()
                }
                onResult(true)
            }
            .addOnFailureListener {
                readChallengeFromPrefs(prefs, cacheKey, periodKey)?.let { cached ->
                    val updated = cached.copy(rewardClaimed = true)
                    prefs.edit().putString(cacheKey, gson.toJson(updated)).apply()
                }
                onResult(false)
            }
    }

    fun markPendingDiamondContinue(
        context: Context,
        periodKey: String,
        slotIndex: Int,
        onResult: (Boolean) -> Unit,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(false)
            return
        }
        val safeSlot = slotIndex.coerceIn(0, DailyQuestionPeriod.QUESTIONS_PER_PERIOD - 1)
        val prefs = context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
        val cacheKey = challengeCacheKey(uid)
        val docRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection(DailyQuestionPeriod.FIRESTORE_COLLECTION)
            .document("current")

        docRef.set(mapOf(FIELD_PENDING_CONTINUE_SLOT to safeSlot), SetOptions.merge())
            .addOnSuccessListener {
                updateCachedPendingContinue(prefs, cacheKey, periodKey, safeSlot)
                onResult(true)
            }
            .addOnFailureListener {
                updateCachedPendingContinue(prefs, cacheKey, periodKey, safeSlot)
                onResult(false)
            }
    }

    fun clearPendingDiamondContinue(
        context: Context,
        periodKey: String,
        onResult: (Boolean) -> Unit,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(false)
            return
        }
        val prefs = context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
        val cacheKey = challengeCacheKey(uid)
        val docRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection(DailyQuestionPeriod.FIRESTORE_COLLECTION)
            .document("current")

        docRef.update(FIELD_PENDING_CONTINUE_SLOT, FieldValue.delete())
            .addOnSuccessListener {
                updateCachedPendingContinue(prefs, cacheKey, periodKey, null)
                onResult(true)
            }
            .addOnFailureListener {
                updateCachedPendingContinue(prefs, cacheKey, periodKey, null)
                onResult(false)
            }
    }

    private fun updateCachedPendingContinue(
        prefs: android.content.SharedPreferences,
        cacheKey: String,
        periodKey: String,
        slotIndex: Int?,
    ) {
        readChallengeFromPrefs(prefs, cacheKey, periodKey)?.let { cached ->
            val updated = cached.copy(pendingContinueSlotIndex = slotIndex)
            prefs.edit().putString(cacheKey, gson.toJson(updated)).apply()
        }
    }

    fun recordQuestionResult(
        context: Context,
        periodKey: String,
        slotIndex: Int,
        isSuccess: Boolean
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val prefs = context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
        val cacheKey = challengeCacheKey(uid)

        val challenge = readChallengeFromPrefs(prefs, cacheKey, periodKey) ?: return
        val slot = challenge.questions.getOrNull(slotIndex) ?: return

        val difficultyKey = when (slot.difficulty.lowercase(java.util.Locale.ROOT)) {
            "kolay" -> "easy"
            "orta" -> "medium"
            "zor" -> "hard"
            else -> return
        }

        val partCollectionName = "generatorForPart${slot.partId}Chest"

        val globalRef = FirebaseFirestore.getInstance()
            .collection("successRate")
            .document("dailyQuestionSuccessRate")
            .collection(partCollectionName)
            .document(slot.titleUnit.replace("/", "-"))

        globalRef.firestore.runTransaction { transaction ->
            val snapshot = transaction.get(globalRef)
            val currentTotal = snapshot.getLong("${difficultyKey}_total") ?: 0L
            val currentSuccess = snapshot.getLong("${difficultyKey}_success") ?: 0L

            val newTotal = currentTotal + 1
            val newSuccess = if (isSuccess) currentSuccess + 1 else currentSuccess
            val newRate = ((newSuccess.toFloat() / newTotal.toFloat()) * 100).toInt()

            val updates = mapOf(
                "${difficultyKey}_total" to newTotal,
                "${difficultyKey}_success" to newSuccess,
                "${difficultyKey}_rate" to newRate
            )
            transaction.set(globalRef, updates, SetOptions.merge())
        }
    }

    fun incrementSolvedCount(
        context: Context,
        periodKey: String,
        onResult: (Int) -> Unit,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(0)
            return
        }
        val docRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection(DailyQuestionPeriod.FIRESTORE_COLLECTION)
            .document("current")

        FirebaseFirestore.getInstance().runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val current = (snapshot.getLong(FIELD_SOLVED_COUNT) ?: 0L).toInt().coerceAtLeast(0)
            val newCount = (current + 1).coerceAtMost(DailyQuestionPeriod.QUESTIONS_PER_PERIOD)
            transaction.update(
                docRef,
                mapOf(
                    FIELD_SOLVED_COUNT to newCount,
                    FIELD_PENDING_CONTINUE_SLOT to FieldValue.delete(),
                ),
            )
            newCount
        }.addOnSuccessListener { newCount ->
            val prefs = context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
            val cacheKey = challengeCacheKey(uid)
            readChallengeFromPrefs(prefs, cacheKey, periodKey)?.let { cached ->
                val updated = cached.copy(
                    solvedCount = newCount,
                    rewardClaimed = cached.rewardClaimed,
                    pendingContinueSlotIndex = null,
                )
                prefs.edit().putString(cacheKey, gson.toJson(updated)).apply()
            }
            onResult(newCount)
        }.addOnFailureListener {
            onResult(0)
        }
    }

    private fun createAndPersistChallenge(
        context: Context,
        uid: String,
        periodKey: String,
        sources: List<DailyQuestionSource>,
        prefs: android.content.SharedPreferences,
        cacheKey: String,
        docRef: com.google.firebase.firestore.DocumentReference,
        onResult: (DailyQuestionChallenge?) -> Unit,
    ) {
        val uidKey = uid
        val challenge = DailyQuestionPoolBuilder.buildChallenge(periodKey, uidKey, sources)
        if (challenge == null) {
            onResult(null)
            return
        }
        prefs.edit().putString(cacheKey, gson.toJson(challenge)).apply()
        docRef.set(challengeToFirestoreMap(challenge))
            .addOnSuccessListener { onResult(challenge) }
            .addOnFailureListener { onResult(challenge) }
    }

    private fun challengeCacheKey(uid: String): String =
        "daily_challenge_${uid}_current"

    private fun readChallengeFromPrefs(
        prefs: android.content.SharedPreferences,
        cacheKey: String,
        periodKey: String,
    ): DailyQuestionChallenge? {
        val json = prefs.getString(cacheKey, null) ?: return null
        return try {
            val type = object : TypeToken<DailyQuestionChallenge>() {}.type
            val challenge: DailyQuestionChallenge = gson.fromJson(json, type)
            if (challenge.periodKey != periodKey) null else challenge
        } catch (_: Exception) {
            null
        }
    }

    private fun parseChallengeDocument(
        data: Map<String, Any>?,
        periodKey: String,
    ): DailyQuestionChallenge? {
        if (data == null) return null
        val docPeriodKey = data[FIELD_PERIOD_KEY] as? String
        if (docPeriodKey != periodKey) return null
        
        val solvedCount = (data[FIELD_SOLVED_COUNT] as? Number)?.toInt() ?: 0
        val rewardClaimed = data[FIELD_REWARD_CLAIMED] as? Boolean == true
        val pendingContinueSlotIndex = (data[FIELD_PENDING_CONTINUE_SLOT] as? Number)?.toInt()
            ?.coerceIn(0, DailyQuestionPeriod.QUESTIONS_PER_PERIOD - 1)
        @Suppress("UNCHECKED_CAST")
        val rawQuestions = data[FIELD_QUESTIONS] as? List<Map<String, Any>> ?: return null
        val questions = rawQuestions.mapNotNull { map -> parseSlot(map) }
        if (questions.size != DailyQuestionPeriod.QUESTIONS_PER_PERIOD) return null
        return DailyQuestionChallenge(
            periodKey = periodKey,
            solvedCount = solvedCount,
            questions = questions,
            rewardClaimed = rewardClaimed,
            pendingContinueSlotIndex = pendingContinueSlotIndex,
        )
    }

    private fun parseSlot(map: Map<String, Any>): DailyQuestionSlot? {
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

    private fun challengeToFirestoreMap(challenge: DailyQuestionChallenge): Map<String, Any> {
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

    private fun DailyQuestionChallenge.toCardUiState(poolAvailable: Boolean): DailyQuestionCardUiState {
        return DailyQuestionCardUiState(
            periodKey = periodKey,
            solvedCount = solvedCount,
            totalCount = DailyQuestionPeriod.QUESTIONS_PER_PERIOD,
            titleUnit = cardTitleUnit(),
            difficulty = cardDifficultyLabel(),
            isComplete = isComplete,
            rewardClaimed = rewardClaimed,
            poolAvailable = poolAvailable,
            pendingContinueSlotIndex = pendingContinueSlotIndex,
            isLoaded = true,
        )
    }

    private const val FIELD_PERIOD_KEY = "periodKey"
    private const val FIELD_SOLVED_COUNT = "solvedCount"
    private const val FIELD_REWARD_CLAIMED = "rewardClaimed"
    private const val FIELD_PENDING_CONTINUE_SLOT = "pendingContinueSlotIndex"
    private const val FIELD_QUESTIONS = "questions"
    private const val FIELD_SEQUENCE = "sequence"
    private const val FIELD_MATH_FIRST = "mathFirst"
    private const val FIELD_MATH_OPERATOR = "mathOperator"
    private const val FIELD_MATH_SECOND = "mathSecond"
    private const val FIELD_PART_ID = "partId"
    private const val FIELD_ITEM_INDEX = "itemIndex"
    private const val FIELD_TITLE_UNIT = "titleUnit"
    private const val FIELD_DIFFICULTY = "difficulty"
    private const val FIELD_DISPLAY_INTERVAL = "displayIntervalMs"
}




package com.example.app

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlin.random.Random

/** Günlük / haftalık görev sayaçları ve görev seçimi yönetimi. */
object MissionsProgressStore {

    private const val PREFS = "missions_progress"
    private const val KEY_DAY_ID = "day_id"
    private const val KEY_WEEK_ID = "week_id"
    private const val KEY_DAILY_STEP_FINISH_COUNT = "daily_step_finish_count"
    private const val KEY_WEEKLY_STEP_FINISH_COUNT = "weekly_step_finish_count"
    private const val KEY_DAILY_STEP_INCREMENT_COUNT = "daily_step_increment_count"
    private const val KEY_WEEKLY_STEP_INCREMENT_COUNT = "weekly_step_increment_count"
    private const val KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT = "daily_perfect_step_increment_count"
    private const val KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT = "weekly_perfect_step_increment_count"
    private const val KEY_DAILY_CHEST_RECORD_BREAK_COUNT = "daily_chest_record_break_count"
    private const val KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT = "weekly_chest_record_break_count"
    private const val KEY_DAILY_CHEST_STAR_GAIN_COUNT = "daily_chest_star_gain_count"
    private const val KEY_WEEKLY_CHEST_STAR_GAIN_COUNT = "weekly_chest_star_gain_count"
    private const val KEY_DAILY_LEARN_MINUTES_COUNT = "daily_learn_minutes_count"
    private const val KEY_WEEKLY_LEARN_MINUTES_COUNT = "weekly_learn_minutes_count"
    private const val KEY_DAILY_LEARN_REMAINDER_MS = "daily_learn_remainder_ms"
    private const val KEY_WEEKLY_LEARN_REMAINDER_MS = "weekly_learn_remainder_ms"
    private const val KEY_DAILY_SELECTION_NONCE = "daily_selection_nonce"
    private const val KEY_WEEKLY_SELECTION_NONCE = "weekly_selection_nonce"
    private const val KEY_DAILY_SELECTED_MISSION_IDS = "daily_selected_mission_ids"
    private const val KEY_WEEKLY_SELECTED_MISSION_IDS = "weekly_selected_mission_ids"
    private const val FIRESTORE_COLLECTION = "missionProgress"
    private const val FIRESTORE_DOC_STATE = "state"
    private const val FIELD_REWARD_CLAIMED_KEYS = "rewardClaimedKeys"
    private const val DEBUG_LOG_PATH = "debug-33b519.log"
    @Volatile private var cloudSyncRequestedForUid: String? = null
    @Volatile private var cloudStateAppliedForUid: String? = null
    @Volatile private var lastSeenUid: String? = null
    @Volatile private var applyingCloudState = false
    private var missionRealtimeListener: ListenerRegistration? = null
    private var missionRealtimeUid: String? = null
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private fun debugLog(hypothesisId: String, location: String, message: String, data: Map<String, Any?> = emptyMap()) {
        try {
            val payload = mapOf(
                "sessionId" to "33b519",
                "runId" to "baseline",
                "hypothesisId" to hypothesisId,
                "location" to location,
                "message" to message,
                "data" to data,
                "timestamp" to System.currentTimeMillis(),
            )
            val json = Gson().toJson(payload)
            android.util.Log.d("DBG33b519", json)
            File(DEBUG_LOG_PATH).appendText("$json\n")
            Thread {
                try {
                    val conn = (URL("http://127.0.0.1:7913/ingest/8a0b1fc3-fae1-418f-bbbd-80b43a829b14").openConnection() as HttpURLConnection)
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("X-Debug-Session-Id", "33b519")
                    conn.doOutput = true
                    conn.outputStream.use { it.write(json.toByteArray()) }
                    conn.inputStream.close()
                    conn.disconnect()
                } catch (_: Exception) {
                }
            }.start()
        } catch (_: Exception) {
        }
    }

    data class Snapshot(
        val dailyStepFinishCount: Int,
        val weeklyStepFinishCount: Int,
        val dailyStepIncrementCount: Int,
        val weeklyStepIncrementCount: Int,
        val dailyPerfectStepIncrementCount: Int,
        val weeklyPerfectStepIncrementCount: Int,
        val dailyChestRecordBreakCount: Int,
        val weeklyChestRecordBreakCount: Int,
        val dailyChestStarGainCount: Int,
        val weeklyChestStarGainCount: Int,
        val dailyLearnMinutesCount: Int,
        val weeklyLearnMinutesCount: Int,
    )

    private fun prefs(ctx: Context): android.content.SharedPreferences {
        val uid = currentUid() ?: "guest"
        return ctx.applicationContext.getSharedPreferences("${PREFS}_$uid", Context.MODE_PRIVATE)
    }

    private fun ensureSyncStateForCurrentUid() {
        val uid = currentUid()
        if (uid != lastSeenUid) {
            // #region agent log
            debugLog(
                hypothesisId = "H4",
                location = "MissionsProgressStore.kt:ensureSyncStateForCurrentUid",
                message = "uid switch detected",
                data = mapOf("fromUidPresent" to (lastSeenUid != null), "toUidPresent" to (uid != null)),
            )
            // #endregion
            lastSeenUid = uid
            cloudSyncRequestedForUid = null
            cloudStateAppliedForUid = null
            applyingCloudState = false
            missionRealtimeListener?.remove()
            missionRealtimeListener = null
            missionRealtimeUid = null
        }
    }

    private fun currentUid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    private fun stateDoc(uid: String) =
        firestore.collection("users").document(uid)
            .collection(FIRESTORE_COLLECTION).document(FIRESTORE_DOC_STATE)

    private fun ensureMissionRealtimeSync(context: Context) {
        ensureSyncStateForCurrentUid()
        val uid = currentUid() ?: return
        if (missionRealtimeListener != null && missionRealtimeUid == uid) return
        missionRealtimeListener?.remove()
        missionRealtimeListener = null
        missionRealtimeUid = uid
        missionRealtimeListener = stateDoc(uid).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            applyCloudDocument(context, uid, snapshot?.data)
        }
    }

    private fun todayId(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }

    /** Bu haftanın Pazartesi tarihini (yerel) benzersiz anahtar olarak kullanır */
    private fun weekIdNow(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun rewardClaimKey(window: MissionWindow, missionId: String): String {
        val periodId = when (window) {
            MissionWindow.DAILY -> todayId()
            MissionWindow.WEEKLY -> weekIdNow()
        }
        val prefix = when (window) {
            MissionWindow.DAILY -> "reward_claimed_daily"
            MissionWindow.WEEKLY -> "reward_claimed_weekly"
        }
        return "${prefix}_${periodId}_$missionId"
    }

    private fun clearRewardClaimFlags(editor: android.content.SharedPreferences.Editor, p: android.content.SharedPreferences) {
        val keysToRemove = p.all.keys.filter { key ->
            key.startsWith("reward_claimed_daily_") || key.startsWith("reward_claimed_weekly_")
        }
        keysToRemove.forEach { editor.remove(it) }
    }

    private fun rewardClaimKeys(p: android.content.SharedPreferences): List<String> =
        p.all.keys.filter { it.startsWith("reward_claimed_daily_") || it.startsWith("reward_claimed_weekly_") }

    private fun parseMissionIdsCsv(csv: String?): List<String> =
        csv?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    private fun missionIdsCsv(ids: List<String>): String = ids.joinToString(",")

    private fun missionIdsKeyForWindow(window: MissionWindow): String = when (window) {
        MissionWindow.DAILY -> KEY_DAILY_SELECTED_MISSION_IDS
        MissionWindow.WEEKLY -> KEY_WEEKLY_SELECTED_MISSION_IDS
    }

    private fun uploadStateToCloud(context: Context) {
        ensureMissionRealtimeSync(context)
        ensureSyncStateForCurrentUid()
        if (applyingCloudState) return
        val uid = currentUid() ?: return
        if (cloudStateAppliedForUid != uid) {
            // #region agent log
            debugLog(
                hypothesisId = "H5",
                location = "MissionsProgressStore.kt:uploadStateToCloud",
                message = "upload deferred until cloud apply",
                data = mapOf("uidPresent" to true, "cloudStateAppliedForUid" to cloudStateAppliedForUid),
            )
            // #endregion
            requestCloudSync(context)
            return
        }
        val p = prefs(context)
        val payload = hashMapOf<String, Any>(
            KEY_DAY_ID to (p.getString(KEY_DAY_ID, todayId()) ?: todayId()),
            KEY_WEEK_ID to (p.getString(KEY_WEEK_ID, weekIdNow()) ?: weekIdNow()),
            KEY_DAILY_STEP_FINISH_COUNT to p.getInt(KEY_DAILY_STEP_FINISH_COUNT, 0),
            KEY_WEEKLY_STEP_FINISH_COUNT to p.getInt(KEY_WEEKLY_STEP_FINISH_COUNT, 0),
            KEY_DAILY_STEP_INCREMENT_COUNT to p.getInt(KEY_DAILY_STEP_INCREMENT_COUNT, 0),
            KEY_WEEKLY_STEP_INCREMENT_COUNT to p.getInt(KEY_WEEKLY_STEP_INCREMENT_COUNT, 0),
            KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT to p.getInt(KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT, 0),
            KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT to p.getInt(KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT, 0),
            KEY_DAILY_CHEST_RECORD_BREAK_COUNT to p.getInt(KEY_DAILY_CHEST_RECORD_BREAK_COUNT, 0),
            KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT to p.getInt(KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT, 0),
            KEY_DAILY_CHEST_STAR_GAIN_COUNT to p.getInt(KEY_DAILY_CHEST_STAR_GAIN_COUNT, 0),
            KEY_WEEKLY_CHEST_STAR_GAIN_COUNT to p.getInt(KEY_WEEKLY_CHEST_STAR_GAIN_COUNT, 0),
            KEY_DAILY_LEARN_MINUTES_COUNT to p.getInt(KEY_DAILY_LEARN_MINUTES_COUNT, 0),
            KEY_WEEKLY_LEARN_MINUTES_COUNT to p.getInt(KEY_WEEKLY_LEARN_MINUTES_COUNT, 0),
            KEY_DAILY_LEARN_REMAINDER_MS to p.getLong(KEY_DAILY_LEARN_REMAINDER_MS, 0L),
            KEY_WEEKLY_LEARN_REMAINDER_MS to p.getLong(KEY_WEEKLY_LEARN_REMAINDER_MS, 0L),
            KEY_DAILY_SELECTION_NONCE to p.getInt(KEY_DAILY_SELECTION_NONCE, 0),
            KEY_WEEKLY_SELECTION_NONCE to p.getInt(KEY_WEEKLY_SELECTION_NONCE, 0),
            KEY_DAILY_SELECTED_MISSION_IDS to (p.getString(KEY_DAILY_SELECTED_MISSION_IDS, "") ?: ""),
            KEY_WEEKLY_SELECTED_MISSION_IDS to (p.getString(KEY_WEEKLY_SELECTED_MISSION_IDS, "") ?: ""),
            FIELD_REWARD_CLAIMED_KEYS to rewardClaimKeys(p),
        )
        stateDoc(uid).set(payload)
    }

    /**
     * UI okumalarından önce cloud state'i prefs'e işler.
     * Aksi halde `getSnapshot()` gibi fonksiyonlar `requestCloudSync()`'i tetikleyip
     * hemen ardından hâlâ eski local değerleri döndürebiliyor.
     */
    private fun hydrateFromServerIfNeededBlocking(context: Context) {
        ensureMissionRealtimeSync(context)
        val uid = currentUid() ?: return
        if (cloudStateAppliedForUid == uid) return
        try {
            val snap = Tasks.await(stateDoc(uid).get())
            applyCloudDocument(context, uid, snap.data)
        } catch (_: Exception) {
            // Ağ/izin hatasında local devam eder; async `requestCloudSync` yine denenebilir.
        }
    }

    private fun applyCloudDocument(context: Context, uid: String, data: Map<String, Any>?) {
        if (data == null) {
            // #region agent log
            debugLog(
                hypothesisId = "H5",
                location = "MissionsProgressStore.kt:applyCloudDocument:emptyDoc",
                message = "mission cloud doc empty (blocking hydrate)",
                data = mapOf("uidPresent" to true),
            )
            // #endregion
            cloudStateAppliedForUid = uid
            cloudSyncRequestedForUid = null
            uploadStateToCloud(context)
            return
        }

        applyingCloudState = true
        val p = prefs(context)
        val editor = p.edit()
        clearRewardClaimFlags(editor, p)
        fun intOf(key: String) = (data[key] as? Number)?.toInt() ?: p.getInt(key, 0)
        fun longOf(key: String) = (data[key] as? Number)?.toLong() ?: p.getLong(key, 0L)
        fun strOf(key: String, fallback: String) = data[key] as? String ?: fallback
        editor
            .putString(KEY_DAY_ID, strOf(KEY_DAY_ID, todayId()))
            .putString(KEY_WEEK_ID, strOf(KEY_WEEK_ID, weekIdNow()))
            .putInt(KEY_DAILY_STEP_FINISH_COUNT, intOf(KEY_DAILY_STEP_FINISH_COUNT))
            .putInt(KEY_WEEKLY_STEP_FINISH_COUNT, intOf(KEY_WEEKLY_STEP_FINISH_COUNT))
            .putInt(KEY_DAILY_STEP_INCREMENT_COUNT, intOf(KEY_DAILY_STEP_INCREMENT_COUNT))
            .putInt(KEY_WEEKLY_STEP_INCREMENT_COUNT, intOf(KEY_WEEKLY_STEP_INCREMENT_COUNT))
            .putInt(KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT, intOf(KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT))
            .putInt(KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT, intOf(KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT))
            .putInt(KEY_DAILY_CHEST_RECORD_BREAK_COUNT, intOf(KEY_DAILY_CHEST_RECORD_BREAK_COUNT))
            .putInt(KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT, intOf(KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT))
            .putInt(KEY_DAILY_CHEST_STAR_GAIN_COUNT, intOf(KEY_DAILY_CHEST_STAR_GAIN_COUNT))
            .putInt(KEY_WEEKLY_CHEST_STAR_GAIN_COUNT, intOf(KEY_WEEKLY_CHEST_STAR_GAIN_COUNT))
            .putInt(KEY_DAILY_LEARN_MINUTES_COUNT, intOf(KEY_DAILY_LEARN_MINUTES_COUNT))
            .putInt(KEY_WEEKLY_LEARN_MINUTES_COUNT, intOf(KEY_WEEKLY_LEARN_MINUTES_COUNT))
            .putLong(KEY_DAILY_LEARN_REMAINDER_MS, longOf(KEY_DAILY_LEARN_REMAINDER_MS))
            .putLong(KEY_WEEKLY_LEARN_REMAINDER_MS, longOf(KEY_WEEKLY_LEARN_REMAINDER_MS))
            .putInt(KEY_DAILY_SELECTION_NONCE, intOf(KEY_DAILY_SELECTION_NONCE))
            .putInt(KEY_WEEKLY_SELECTION_NONCE, intOf(KEY_WEEKLY_SELECTION_NONCE))
            .putString(KEY_DAILY_SELECTED_MISSION_IDS, data[KEY_DAILY_SELECTED_MISSION_IDS] as? String ?: "")
            .putString(KEY_WEEKLY_SELECTED_MISSION_IDS, data[KEY_WEEKLY_SELECTED_MISSION_IDS] as? String ?: "")
        @Suppress("UNCHECKED_CAST")
        val claimedKeys = (data[FIELD_REWARD_CLAIMED_KEYS] as? List<Any?>)
            ?.mapNotNull { it as? String }
            ?: emptyList()
        claimedKeys.forEach { editor.putBoolean(it, true) }
        editor.apply()
        applyingCloudState = false
        cloudStateAppliedForUid = uid
        cloudSyncRequestedForUid = null
        ensureResets(context)
    }

    private fun requestCloudSync(context: Context) {
        ensureMissionRealtimeSync(context)
        ensureSyncStateForCurrentUid()
        val uid = currentUid() ?: return
        if (cloudStateAppliedForUid == uid || cloudSyncRequestedForUid == uid) return
        // #region agent log
        debugLog(
            hypothesisId = "H5",
            location = "MissionsProgressStore.kt:requestCloudSync:start",
            message = "requesting mission cloud sync",
            data = mapOf("uidPresent" to true, "alreadyApplied" to (cloudStateAppliedForUid == uid)),
        )
        // #endregion
        cloudSyncRequestedForUid = uid
        stateDoc(uid).get()
            .addOnSuccessListener { doc ->
                val data = doc.data
                // #region agent log
                if (data != null) {
                    debugLog(
                        hypothesisId = "H5",
                        location = "MissionsProgressStore.kt:requestCloudSync:success",
                        message = "mission cloud state loaded",
                        data = mapOf(
                            "keysCount" to data.keys.size,
                            "dayId" to (data[KEY_DAY_ID] as? String),
                            "weekId" to (data[KEY_WEEK_ID] as? String),
                            "dailyStepFinish" to ((data[KEY_DAILY_STEP_FINISH_COUNT] as? Number)?.toInt() ?: -1),
                            "weeklyStepFinish" to ((data[KEY_WEEKLY_STEP_FINISH_COUNT] as? Number)?.toInt() ?: -1),
                            "dailyStepIncrement" to ((data[KEY_DAILY_STEP_INCREMENT_COUNT] as? Number)?.toInt() ?: -1),
                            "weeklyStepIncrement" to ((data[KEY_WEEKLY_STEP_INCREMENT_COUNT] as? Number)?.toInt() ?: -1),
                        ),
                    )
                } else {
                    debugLog(
                        hypothesisId = "H5",
                        location = "MissionsProgressStore.kt:requestCloudSync:emptyDoc",
                        message = "mission cloud doc empty",
                        data = mapOf("uidPresent" to true),
                    )
                }
                // #endregion
                applyCloudDocument(context, uid, data)
            }
            .addOnFailureListener { e ->
                // #region agent log
                debugLog(
                    hypothesisId = "H5",
                    location = "MissionsProgressStore.kt:requestCloudSync:failure",
                    message = "mission cloud sync failed",
                    data = mapOf(
                        "uidPresent" to true,
                        "errorType" to e.javaClass.simpleName,
                        "errorMessage" to (e.message ?: "null"),
                    ),
                )
                // #endregion
                cloudSyncRequestedForUid = null
                applyingCloudState = false
            }
    }

    private fun readCounter(snapshot: Snapshot, window: MissionWindow, counterType: MissionCounterType): Int {
        return when (window) {
            MissionWindow.DAILY -> when (counterType) {
                MissionCounterType.STEP_FINISH -> snapshot.dailyStepFinishCount
                MissionCounterType.STEP_INCREMENT -> snapshot.dailyStepIncrementCount
                MissionCounterType.PERFECT_STEP_INCREMENT -> snapshot.dailyPerfectStepIncrementCount
                MissionCounterType.CHEST_RECORD_BREAK -> snapshot.dailyChestRecordBreakCount
                MissionCounterType.CHEST_STAR_GAIN -> snapshot.dailyChestStarGainCount
                MissionCounterType.LEARN_MINUTES -> snapshot.dailyLearnMinutesCount
            }
            MissionWindow.WEEKLY -> when (counterType) {
                MissionCounterType.STEP_FINISH -> snapshot.weeklyStepFinishCount
                MissionCounterType.STEP_INCREMENT -> snapshot.weeklyStepIncrementCount
                MissionCounterType.PERFECT_STEP_INCREMENT -> snapshot.weeklyPerfectStepIncrementCount
                MissionCounterType.CHEST_RECORD_BREAK -> snapshot.weeklyChestRecordBreakCount
                MissionCounterType.CHEST_STAR_GAIN -> snapshot.weeklyChestStarGainCount
                MissionCounterType.LEARN_MINUTES -> snapshot.weeklyLearnMinutesCount
            }
        }
    }

    private fun selectedMissionIdsForWindow(context: Context, window: MissionWindow): List<String> {
        val p = prefs(context)
        val keyForStoredIds = missionIdsKeyForWindow(window)
        val allowedMissionIds = MissionQuestCatalog.all
            .filter { window in it.availableWindows }
            .map { it.id }
            .toSet()
        val stored = parseMissionIdsCsv(p.getString(keyForStoredIds, null))
            .filter { it in allowedMissionIds }
            .distinct()
            .take(3)
        if (stored.size == 3) return stored

        val key = when (window) {
            MissionWindow.DAILY -> "${todayId()}_${p.getInt(KEY_DAILY_SELECTION_NONCE, 0)}"
            MissionWindow.WEEKLY -> "${weekIdNow()}_${p.getInt(KEY_WEEKLY_SELECTION_NONCE, 0)}"
        }
        val random = Random(key.hashCode())
        val generated = MissionQuestCatalog.all
            .filter { window in it.availableWindows }
            .shuffled(random)
            .take(3)
            .map { it.id }
        p.edit().putString(keyForStoredIds, missionIdsCsv(generated)).apply()
        uploadStateToCloud(context)
        return generated
    }

    fun selectedMissionsForWindow(context: Context, window: MissionWindow): List<MissionQuestDefinition> {
        ensureResets(context)
        hydrateFromServerIfNeededBlocking(context)
        requestCloudSync(context)
        val missionById = MissionQuestCatalog.all.associateBy { it.id }
        return selectedMissionIdsForWindow(context, window).mapNotNull { missionById[it] }
    }

    fun selectedMissionsForDaily(context: Context): List<MissionQuestDefinition> =
        selectedMissionsForWindow(context, MissionWindow.DAILY)

    fun selectedMissionsForWeekly(context: Context): List<MissionQuestDefinition> =
        selectedMissionsForWindow(context, MissionWindow.WEEKLY)

    fun missionProgress(snapshot: Snapshot, window: MissionWindow, mission: MissionQuestDefinition): Int {
        return readCounter(snapshot, window, mission.counterType)
    }

    fun isMissionRewardClaimed(context: Context, window: MissionWindow, missionId: String): Boolean {
        ensureResets(context)
        hydrateFromServerIfNeededBlocking(context)
        requestCloudSync(context)
        return prefs(context).getBoolean(rewardClaimKey(window, missionId), false)
    }

    fun markMissionRewardClaimed(context: Context, window: MissionWindow, missionId: String) {
        ensureResets(context)
        prefs(context).edit()
            .putBoolean(rewardClaimKey(window, missionId), true)
            .apply()
        uploadStateToCloud(context)
    }

    fun ensureResets(context: Context) {
        val p = prefs(context)
        val ed = p.edit()
        val t = todayId()
        if (p.getString(KEY_DAY_ID, null) != t) {
            ed.putString(KEY_DAY_ID, t)
            ed.putInt(KEY_DAILY_STEP_FINISH_COUNT, 0)
            ed.putInt(KEY_DAILY_STEP_INCREMENT_COUNT, 0)
            ed.putInt(KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT, 0)
            ed.putInt(KEY_DAILY_CHEST_RECORD_BREAK_COUNT, 0)
            ed.putInt(KEY_DAILY_CHEST_STAR_GAIN_COUNT, 0)
            ed.putInt(KEY_DAILY_LEARN_MINUTES_COUNT, 0)
            ed.putLong(KEY_DAILY_LEARN_REMAINDER_MS, 0L)
            ed.remove(KEY_DAILY_SELECTED_MISSION_IDS)
        }
        val w = weekIdNow()
        if (p.getString(KEY_WEEK_ID, null) != w) {
            ed.putString(KEY_WEEK_ID, w)
            ed.putInt(KEY_WEEKLY_STEP_FINISH_COUNT, 0)
            ed.putInt(KEY_WEEKLY_STEP_INCREMENT_COUNT, 0)
            ed.putInt(KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT, 0)
            ed.putInt(KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT, 0)
            ed.putInt(KEY_WEEKLY_CHEST_STAR_GAIN_COUNT, 0)
            ed.putInt(KEY_WEEKLY_LEARN_MINUTES_COUNT, 0)
            ed.putLong(KEY_WEEKLY_LEARN_REMAINDER_MS, 0L)
            ed.remove(KEY_WEEKLY_SELECTED_MISSION_IDS)
        }
        ed.apply()
    }

    fun getSnapshot(context: Context): Snapshot {
        ensureResets(context)
        hydrateFromServerIfNeededBlocking(context)
        requestCloudSync(context)
        val p = prefs(context)
        return Snapshot(
            dailyStepFinishCount = p.getInt(KEY_DAILY_STEP_FINISH_COUNT, 0),
            weeklyStepFinishCount = p.getInt(KEY_WEEKLY_STEP_FINISH_COUNT, 0),
            dailyStepIncrementCount = p.getInt(KEY_DAILY_STEP_INCREMENT_COUNT, 0),
            weeklyStepIncrementCount = p.getInt(KEY_WEEKLY_STEP_INCREMENT_COUNT, 0),
            dailyPerfectStepIncrementCount = p.getInt(KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT, 0),
            weeklyPerfectStepIncrementCount = p.getInt(KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT, 0),
            dailyChestRecordBreakCount = p.getInt(KEY_DAILY_CHEST_RECORD_BREAK_COUNT, 0),
            weeklyChestRecordBreakCount = p.getInt(KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT, 0),
            dailyChestStarGainCount = p.getInt(KEY_DAILY_CHEST_STAR_GAIN_COUNT, 0),
            weeklyChestStarGainCount = p.getInt(KEY_WEEKLY_CHEST_STAR_GAIN_COUNT, 0),
            dailyLearnMinutesCount = p.getInt(KEY_DAILY_LEARN_MINUTES_COUNT, 0),
            weeklyLearnMinutesCount = p.getInt(KEY_WEEKLY_LEARN_MINUTES_COUNT, 0),
        )
    }

    /** Sandık talebinden sonra çubukta görünür bir değişim oldu mu (tam dolu sayaçlarda artış görünmez). */
    fun hasVisibleMissionProgress(context: Context, before: Snapshot, after: Snapshot): Boolean {
        val visibleDaily = selectedMissionsForDaily(context).any { mission ->
            val beforeValue = missionProgress(before, MissionWindow.DAILY, mission).coerceAtMost(mission.target)
            val afterValue = missionProgress(after, MissionWindow.DAILY, mission).coerceAtMost(mission.target)
            beforeValue != afterValue
        }
        if (visibleDaily) return true
        return selectedMissionsForWeekly(context).any { mission ->
            val beforeValue = missionProgress(before, MissionWindow.WEEKLY, mission).coerceAtMost(mission.target)
            val afterValue = missionProgress(after, MissionWindow.WEEKLY, mission).coerceAtMost(mission.target)
            beforeValue != afterValue
        }
    }

    fun forceReselectMissions(context: Context) {
        ensureResets(context)
        val p = prefs(context)
        p.edit()
            .putInt(KEY_DAILY_SELECTION_NONCE, p.getInt(KEY_DAILY_SELECTION_NONCE, 0) + 1)
            .putInt(KEY_WEEKLY_SELECTION_NONCE, p.getInt(KEY_WEEKLY_SELECTION_NONCE, 0) + 1)
            .remove(KEY_DAILY_SELECTED_MISSION_IDS)
            .remove(KEY_WEEKLY_SELECTED_MISSION_IDS)
            .apply()
        uploadStateToCloud(context)
    }

    fun recordStepFinishProgress(context: Context) {
        ensureResets(context)
        val p = prefs(context)
        p.edit()
            .putInt(KEY_DAILY_STEP_FINISH_COUNT, p.getInt(KEY_DAILY_STEP_FINISH_COUNT, 0) + 1)
            .putInt(KEY_WEEKLY_STEP_FINISH_COUNT, p.getInt(KEY_WEEKLY_STEP_FINISH_COUNT, 0) + 1)
            .apply()
        uploadStateToCloud(context)
    }

    fun recordStepIncrementProgress(context: Context) {
        ensureResets(context)
        val p = prefs(context)
        p.edit()
            .putInt(KEY_DAILY_STEP_INCREMENT_COUNT, p.getInt(KEY_DAILY_STEP_INCREMENT_COUNT, 0) + 1)
            .putInt(KEY_WEEKLY_STEP_INCREMENT_COUNT, p.getInt(KEY_WEEKLY_STEP_INCREMENT_COUNT, 0) + 1)
            .apply()
        uploadStateToCloud(context)
    }

    fun recordPerfectStepIncrementProgress(context: Context) {
        ensureResets(context)
        val p = prefs(context)
        p.edit()
            .putInt(
                KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT,
                p.getInt(KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT, 0) + 1,
            )
            .putInt(
                KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT,
                p.getInt(KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT, 0) + 1,
            )
            .apply()
        uploadStateToCloud(context)
    }

    fun recordChestRecordBreakProgress(context: Context) {
        ensureResets(context)
        val p = prefs(context)
        p.edit()
            .putInt(KEY_DAILY_CHEST_RECORD_BREAK_COUNT, p.getInt(KEY_DAILY_CHEST_RECORD_BREAK_COUNT, 0) + 1)
            .putInt(KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT, p.getInt(KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT, 0) + 1)
            .apply()
        uploadStateToCloud(context)
    }

    fun recordChestStarGainProgress(context: Context, amount: Int = 1) {
        if (amount <= 0) return
        ensureResets(context)
        val p = prefs(context)
        p.edit()
            .putInt(KEY_DAILY_CHEST_STAR_GAIN_COUNT, p.getInt(KEY_DAILY_CHEST_STAR_GAIN_COUNT, 0) + amount)
            .putInt(KEY_WEEKLY_CHEST_STAR_GAIN_COUNT, p.getInt(KEY_WEEKLY_CHEST_STAR_GAIN_COUNT, 0) + amount)
            .apply()
        uploadStateToCloud(context)
    }

    fun recordLearningDurationMs(context: Context, elapsedMs: Long) {
        if (elapsedMs <= 0L) return
        ensureResets(context)
        val p = prefs(context)
        p.edit()
            .putLong(KEY_DAILY_LEARN_REMAINDER_MS, p.getLong(KEY_DAILY_LEARN_REMAINDER_MS, 0L) + elapsedMs)
            .putLong(KEY_WEEKLY_LEARN_REMAINDER_MS, p.getLong(KEY_WEEKLY_LEARN_REMAINDER_MS, 0L) + elapsedMs)
            .apply()
        uploadStateToCloud(context)
    }

    /**
     * Öğrenme süresi birikimini dakikaya çevirip görev sayaçlarına işler.
     * Bu fonksiyon ChestFragment ödül akışı sırasında çağrılarak ilerlemenin
     * görev ekranında toplu görünmesini sağlar.
     */
    fun applyPendingLearningMinutes(context: Context) {
        ensureResets(context)
        val p = prefs(context)
        val dailyTotalMs = p.getLong(KEY_DAILY_LEARN_REMAINDER_MS, 0L)
        val weeklyTotalMs = p.getLong(KEY_WEEKLY_LEARN_REMAINDER_MS, 0L)
        val dailyMinuteGain = (dailyTotalMs / 60_000L).toInt()
        val weeklyMinuteGain = (weeklyTotalMs / 60_000L).toInt()
        if (dailyMinuteGain <= 0 && weeklyMinuteGain <= 0) return
        p.edit()
            .putInt(KEY_DAILY_LEARN_MINUTES_COUNT, p.getInt(KEY_DAILY_LEARN_MINUTES_COUNT, 0) + dailyMinuteGain)
            .putInt(KEY_WEEKLY_LEARN_MINUTES_COUNT, p.getInt(KEY_WEEKLY_LEARN_MINUTES_COUNT, 0) + weeklyMinuteGain)
            .putLong(KEY_DAILY_LEARN_REMAINDER_MS, dailyTotalMs % 60_000L)
            .putLong(KEY_WEEKLY_LEARN_REMAINDER_MS, weeklyTotalMs % 60_000L)
            .apply()
        uploadStateToCloud(context)
    }

    /** Günlük ve haftalık görev sayaçlarını sıfırlar. */
    fun resetAllProgress(context: Context) {
        val p = prefs(context)
        val editor = p.edit()
        clearRewardClaimFlags(editor, p)
        editor
            .putString(KEY_DAY_ID, todayId())
            .putString(KEY_WEEK_ID, weekIdNow())
            .putInt(KEY_DAILY_STEP_FINISH_COUNT, 0)
            .putInt(KEY_WEEKLY_STEP_FINISH_COUNT, 0)
            .putInt(KEY_DAILY_STEP_INCREMENT_COUNT, 0)
            .putInt(KEY_WEEKLY_STEP_INCREMENT_COUNT, 0)
            .putInt(KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT, 0)
            .putInt(KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT, 0)
            .putInt(KEY_DAILY_CHEST_RECORD_BREAK_COUNT, 0)
            .putInt(KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT, 0)
            .putInt(KEY_DAILY_CHEST_STAR_GAIN_COUNT, 0)
            .putInt(KEY_WEEKLY_CHEST_STAR_GAIN_COUNT, 0)
            .putInt(KEY_DAILY_LEARN_MINUTES_COUNT, 0)
            .putInt(KEY_WEEKLY_LEARN_MINUTES_COUNT, 0)
            .putLong(KEY_DAILY_LEARN_REMAINDER_MS, 0L)
            .putLong(KEY_WEEKLY_LEARN_REMAINDER_MS, 0L)
            .remove(KEY_DAILY_SELECTED_MISSION_IDS)
            .remove(KEY_WEEKLY_SELECTED_MISSION_IDS)
            .apply()
        uploadStateToCloud(context)
    }

    /** Bir sonraki yerel gece yarısına kalan tam saat (en az 1) */
    fun hoursUntilDailyReset(): Int {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = next.timeInMillis - now.timeInMillis
        return (diff / (1000 * 60 * 60)).toInt().coerceAtLeast(1)
    }

    /**
     * Haftalık pencere sonu: bu haftanın Pazartesi 00:00 + 7 gün.
     * Kalan süre 24 saatten fazlaysa gün, değilse saat metni için kullanılır.
     */
    fun millisUntilWeeklyReset(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        cal.add(Calendar.DAY_OF_YEAR, 7)
        return (cal.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    }
}

enum class MissionWindow {
    DAILY,
    WEEKLY,
}

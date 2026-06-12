package com.example.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.app.model.LessonItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object GlobalLessonData {
    data class ChestLessonRef(
        val partId: Int,
        val index: Int,
        val item: LessonItem,
    )

    var globalPartId = 1
    private var _lessonItems: MutableList<LessonItem> = mutableListOf()
    val lessonItems: List<LessonItem> get() = _lessonItems

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private const val FIRESTORE_LESSON_PROGRESS = "lessonProgress"
    /** [createLessonItems] ile tanımlı tüm part id'leri; seed ve çapraz-part okumalar için. */
    private val SEED_PART_IDS = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
    private const val AUTH_WAIT_TIMEOUT_MS = 1500L
    private const val DEBUG_LOG_PATH = "debug-33b519.log"
    private var lessonRealtimeListener: ListenerRegistration? = null
    private var lessonRealtimeUid: String? = null
    private var lessonRealtimePartId: Int? = null

    private enum class FirestoreLoadStatus {
        LOADED,
        NOT_FOUND,
        ERROR
    }

    private const val LOG_TAG = "LessonProgress"

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
            Log.d("DBG33b519", json)
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

    fun initialize(context: Context, partId: Int, onReady: (() -> Unit)? = null) {
        globalPartId = partId
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d(LOG_TAG, "initialize partId=$partId uid=${uid?.take(8) ?: "null"} onReady=${onReady != null}")
        // #region agent log
        debugLog(
            hypothesisId = "H1",
            location = "GlobalLessonData.kt:initialize",
            message = "initialize entry",
            data = mapOf("partId" to partId, "uidPresent" to (uid != null), "hasCallback" to (onReady != null)),
        )
        // #endregion

        // Local cache kaynak olarak kullanılmıyor; girişli kullanıcıda kaynak Firestore'dur.

        // Callback yoksa da cloud verisini asenkron çekmeyi dene; yoksa default'a düş.
        // Bu sayede yeni cihazda yanlışlıkla default veriyi cloud'un üstüne yazmayız.
        if (onReady == null) {
            val auth = FirebaseAuth.getInstance()
            val immediateUid = auth.currentUser?.uid
            // #region agent log
            debugLog(
                hypothesisId = "H1",
                location = "GlobalLessonData.kt:initialize:noCallbackBranch",
                message = "no-callback branch decision",
                data = mapOf("partId" to partId, "uidPresent" to (immediateUid != null)),
            )
            // #endregion
            if (immediateUid != null) {
                ensureLessonRealtimeSync(context.applicationContext, partId, immediateUid)
                Log.d(LOG_TAG, "initialize: no callback + uid present -> async loadFromFirestore partId=$partId")
                loadFromFirestore(context, partId, immediateUid) { status ->
                    when (status) {
                        FirestoreLoadStatus.LOADED -> {
                            Log.d(LOG_TAG, "initialize(no callback): using CLOUD data, items=${_lessonItems.size}")
                        }
                        FirestoreLoadStatus.NOT_FOUND -> {
                            Log.d(LOG_TAG, "initialize(no callback): NOT_FOUND -> applyDefaultLessonItems")
                            applyDefaultLessonItems(context, partId)
                        }
                        FirestoreLoadStatus.ERROR -> {
                            Log.w(LOG_TAG, "initialize(no callback): ERROR -> applyDefaultLessonItems(saveRemote=false)")
                            applyDefaultLessonItems(context, partId, saveRemote = false)
                        }
                    }
                }
            } else {
                Log.d(LOG_TAG, "initialize: no callback + uid null -> applyDefaultLessonItems(saveRemote=false)")
                applyDefaultLessonItems(context, partId, saveRemote = false)
            }
            return
        }

        // Callback var: Firestore'u mutlaka dene (auth geç açılıyorsa kısa süre bekle)
        val auth = FirebaseAuth.getInstance()
        val immediateUid = auth.currentUser?.uid
        if (immediateUid != null) {
            ensureLessonRealtimeSync(context.applicationContext, partId, immediateUid)
            Log.d(LOG_TAG, "initialize: uid present -> loadFromFirestore partId=$partId")
            loadFromFirestore(context, partId, immediateUid) { status ->
                Log.d(LOG_TAG, "initialize: loadFromFirestore result=$status")
                // #region agent log
                debugLog(
                    hypothesisId = "H3",
                    location = "GlobalLessonData.kt:initialize:uidPresent:status",
                    message = "loadFromFirestore completed",
                    data = mapOf("partId" to partId, "status" to status.name),
                )
                // #endregion
                when (status) {
                    FirestoreLoadStatus.LOADED -> Log.d(LOG_TAG, "initialize: using CLOUD data, items=${_lessonItems.size}")
                    FirestoreLoadStatus.NOT_FOUND -> {
                        Log.d(LOG_TAG, "initialize: NOT_FOUND -> applyDefaultLessonItems (first time or no cloud data)")
                        applyDefaultLessonItems(context, partId)
                    }
                    FirestoreLoadStatus.ERROR -> {
                        Log.w(LOG_TAG, "initialize: ERROR -> applyDefaultLessonItems(saveRemote=false) to avoid overwriting cloud")
                        applyDefaultLessonItems(context, partId, saveRemote = false)
                    }
                }
                onReady.invoke()
            }
            return
        }

        Log.d(LOG_TAG, "initialize: uid null -> waitForAuth (timeout ${AUTH_WAIT_TIMEOUT_MS}ms)")
        waitForAuthUid(auth, AUTH_WAIT_TIMEOUT_MS) { waitedUid ->
            if (waitedUid == null) {
                Log.d(LOG_TAG, "initialize: after wait still no uid -> applyDefaultLessonItems (guest)")
                applyDefaultLessonItems(context, partId)
                onReady.invoke()
                return@waitForAuthUid
            }
            ensureLessonRealtimeSync(context.applicationContext, partId, waitedUid)
            Log.d(LOG_TAG, "initialize: after wait uid present -> loadFromFirestore partId=$partId")
            loadFromFirestore(context, partId, waitedUid) { status ->
                Log.d(LOG_TAG, "initialize: loadFromFirestore result=$status")
                // #region agent log
                debugLog(
                    hypothesisId = "H3",
                    location = "GlobalLessonData.kt:initialize:waitedUid:status",
                    message = "loadFromFirestore completed after wait",
                    data = mapOf("partId" to partId, "status" to status.name),
                )
                // #endregion
                when (status) {
                    FirestoreLoadStatus.LOADED -> Log.d(LOG_TAG, "initialize: using CLOUD data, items=${_lessonItems.size}")
                    FirestoreLoadStatus.NOT_FOUND -> {
                        Log.d(LOG_TAG, "initialize: NOT_FOUND -> applyDefaultLessonItems")
                        applyDefaultLessonItems(context, partId)
                    }
                    FirestoreLoadStatus.ERROR -> {
                        Log.w(LOG_TAG, "initialize: ERROR -> applyDefaultLessonItems(saveRemote=false)")
                        applyDefaultLessonItems(context, partId, saveRemote = false)
                    }
                }
                onReady.invoke()
            }
        }
    }

    private fun ensureLessonRealtimeSync(appContext: Context, partId: Int, uid: String) {
        if (lessonRealtimeListener != null && lessonRealtimeUid == uid && lessonRealtimePartId == partId) return
        lessonRealtimeListener?.remove()
        lessonRealtimeListener = null
        lessonRealtimeUid = uid
        lessonRealtimePartId = partId

        lessonRealtimeListener = firestore.collection("users")
            .document(uid)
            .collection(FIRESTORE_LESSON_PROGRESS)
            .document(partId.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    LessonProgressDiag.log("Firestore.realtime", "part=$partId ERROR ${error.message}")
                    return@addSnapshotListener
                }
                val fromCache = snapshot?.metadata?.isFromCache == true
                val json = snapshot?.getString("items")
                if (json.isNullOrBlank()) return@addSnapshotListener
                LessonProgressDiag.log(
                    "Firestore.realtime",
                    "part=$partId fromCache=$fromCache localItems=${_lessonItems.size} jsonLen=${json.length}",
                )
                try {
                    val parsed = parseLessonItemsWithMigration(json)
                    val merged = if (_lessonItems.isEmpty()) {
                        LessonProgressDiag.log("Firestore.realtime", "part=$partId local empty → use remote only")
                        parsed
                    } else {
                        LessonProgressMerge.mergeListsPreferMoreProgress(_lessonItems, parsed)
                    }
                    if (LessonProgressMerge.sameProgressState(_lessonItems, merged)) {
                        LessonProgressDiag.log(
                            "Firestore.realtime",
                            "part=$partId SKIP refresh (sameProgressState after merge) fromCache=$fromCache",
                        )
                        return@addSnapshotListener
                    }
                    val regressions = _lessonItems.mapIndexedNotNull { index, localItem ->
                        val m = merged.getOrNull(index) ?: return@mapIndexedNotNull null
                        if (localItem.stepIsFinish && !m.stepIsFinish) index else null
                    }
                    if (regressions.isNotEmpty()) {
                        val last = LessonProgressDiag.lastClaimRecord
                        val nearClaim = last != null &&
                            System.currentTimeMillis() - last.atMs < 8_000L &&
                            partId == last.partId
                        val claimIdxHit = last != null && last.mapIdx in regressions
                        LessonProgressDiag.log(
                            "Firestore.realtime",
                            "H8_REGRESSION indices=$regressions (local finish lost after merge!) " +
                                "nearClaim=$nearClaim claimIdxHit=$claimIdxHit claimRoute=${last?.route} " +
                                "fromCache=$fromCache",
                        )
                    }
                    LessonProgressDiag.logListChestFinishSummary("Firestore.realtime.beforeApply", partId, _lessonItems)
                    LessonProgressDiag.logListChestFinishSummary("Firestore.realtime.afterMerge", partId, merged)
                    _lessonItems = merged
                    LessonProgressDiag.log("Firestore.realtime", "part=$partId → refreshLessonsFromGlobalData")
                    LessonManager.refreshLessonsFromGlobalData()
                } catch (e: Exception) {
                    LessonProgressDiag.log("Firestore.realtime", "part=$partId FAILED ${e.message}")
                    Log.e(LOG_TAG, "realtimeSnapshot parse/merge failed", e)
                }
            }
    }

    /**
     * Varsayılan ders listesi: [createLessonItems] + [AppPrefs] `tutorial1_login_flow_pending` ile part 1 index 1 düzeltmesi.
     * Bellekteki [_lessonItems] / [globalPartId] değiştirilmez; seed ve [applyDefaultLessonItems] ortak kaynak.
     */
    private fun buildDefaultLessonItemsForPart(context: Context, partId: Int): List<LessonItem> {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val tutorial1Pending = prefs.getBoolean("tutorial1_login_flow_pending", false)
        val items = createLessonItems(partId).toMutableList()
        if (partId == 1 && tutorial1Pending && items.size > 1) {
            val original = items[1]
            val updated = original.copy(
                tutorialIsFinish = true,
                currentStep = 2,
                startStepNumber = 2,
                stepCompletionStatus = listOf(true, false, false),
            )
            items[1] = updated
        }
        return items
    }

    /** Varsayılan ders listesini oluşturur ve [_lessonItems] + tercihe göre Firestore'a kaydeder. */
    private fun applyDefaultLessonItems(context: Context, partId: Int, saveRemote: Boolean = true) {
        Log.d(LOG_TAG, "applyDefaultLessonItems partId=$partId saveRemote=$saveRemote (overwrites current _lessonItems with defaults)")
        // #region agent log
        debugLog(
            hypothesisId = "H2",
            location = "GlobalLessonData.kt:applyDefaultLessonItems",
            message = "applying default lesson items",
            data = mapOf(
                "partId" to partId,
                "saveRemote" to saveRemote,
                "uidPresent" to (FirebaseAuth.getInstance().currentUser?.uid != null),
            ),
        )
        // #endregion
        _lessonItems = buildDefaultLessonItemsForPart(context, partId).toMutableList()
        saveToPreferences(context, saveRemote = saveRemote)
    }

    /**
     * Girişli kullanıcı için tüm seed part'larında `lessonProgress/{partId}` dokümanında `items` yoksa veya boşsa
     * varsayılan JSON yazar. Mevcut dolu `items` alanına dokunmaz. [_lessonItems] / [globalPartId] değiştirilmez.
     */
    fun seedAllLessonProgressIfMissing(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.d(LOG_TAG, "seedAllLessonProgressIfMissing skip (no uid)")
            return
        }
        val appCtx = context.applicationContext
        val gson = Gson()
        var idx = 0
        fun processNext() {
            if (idx >= SEED_PART_IDS.size) {
                Log.d(LOG_TAG, "seedAllLessonProgressIfMissing completed for uid=${uid.take(8)}...")
                return
            }
            val partId = SEED_PART_IDS[idx]
            idx++
            val items = buildDefaultLessonItemsForPart(appCtx, partId)
            if (items.isEmpty()) {
                Log.d(LOG_TAG, "seedAllLessonProgressIfMissing skip empty template partId=$partId")
                processNext()
                return
            }
            val ref = firestore.collection("users").document(uid).collection(FIRESTORE_LESSON_PROGRESS)
                .document(partId.toString())
            ref.get()
                .addOnSuccessListener { snap ->
                    val existing = snap.getString("items")
                    if (snap.exists() && !existing.isNullOrBlank()) {
                        processNext()
                        return@addOnSuccessListener
                    }
                    val json = gson.toJson(items)
                    Log.d(LOG_TAG, "seedAllLessonProgressIfMissing writing partId=$partId items=${items.size}")
                    ref.set(mapOf("items" to json))
                        .addOnSuccessListener { processNext() }
                        .addOnFailureListener { e ->
                            Log.e(LOG_TAG, "seedAllLessonProgressIfMissing write failed partId=$partId", e)
                            processNext()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(LOG_TAG, "seedAllLessonProgressIfMissing read failed partId=$partId", e)
                    processNext()
                }
        }
        processNext()
    }

    fun updateLessonItem(context: Context, position: Int, newItem: LessonItem) {
        if (position in _lessonItems.indices) {
            val previous = _lessonItems[position]
            LessonProgressDiag.logItemDelta(
                "GlobalLessonData.updateLessonItem",
                globalPartId,
                position,
                previous,
                newItem,
            )
            _lessonItems[position] = newItem
            Log.d(LOG_TAG, "updateLessonItem position=$position title=${newItem.title.take(30)} stepIsFinish=${newItem.stepIsFinish} -> saving to local+Firestore")
            LessonProgressDiag.log("GlobalLessonData.updateLessonItem", "part=$globalPartId idx=$position → saveToPreferences (Firestore async)")
            saveToPreferences(context)
            // Kupa dersi tamamlandığında liderlik senkronu: [record] tüm zamanların en iyisi (UI);
            // Firestore tahtasına yalnızca [leaderboardSeasonBest] (mevcut sezon) yazılır.
            if (newItem.type == LessonItem.TYPE_CHEST && newItem.stepIsFinish) {
                val seasonNow = SeasonClock.currentSeason()
                val lbScore = newItem.leaderboardSubmitScore(seasonNow)
                if (lbScore != null && lbScore > 0) {
                    LessonLeaderboardRepository.submitBestIfNeeded(
                        globalPartId,
                        position,
                        lbScore,
                        season = seasonNow,
                        titleUnit = newItem.titleUnit?.trim()?.take(127),
                    )
                }
            }
        }
    }

    fun getLessonItem(position: Int): LessonItem? {
        return if (position in _lessonItems.indices) {
            _lessonItems[position]
        } else {
            null
        }
    }

    private fun templateFirstChestLessonIndexOrNull(partId: Int): Int? {
        val idx = createLessonItems(partId).indexOfFirst { it.type == LessonItem.TYPE_CHEST }
        return idx.takeIf { it >= 0 }
    }

    /**
     * Kullanıcının Firestore `lessonProgress/{partId}` listesindeki ilk [LessonItem.TYPE_CHEST] satırının 0-tabanlı indeksi.
     * [LessonLeaderboardRepository.submitBestIfNeeded] bu indeksle aynı `lessonIndex`'i kullanır; şablon listesi buluttan
     * farklıysa burada çözülmek zorunda.
     */
    fun resolveFirstChestLessonIndexForUser(uid: String, partId: Int, onResult: (Int?) -> Unit) {
        if (uid.isBlank()) {
            onResult(templateFirstChestLessonIndexOrNull(partId))
            return
        }
        firestore.collection("users").document(uid).collection(FIRESTORE_LESSON_PROGRESS)
            .document(partId.toString())
            .get()
            .addOnSuccessListener { doc ->
                val cloudJson = doc.getString("items")
                val (source, items) = when {
                    cloudJson.isNullOrBlank() ->
                        "template(empty_cloud)" to createLessonItems(partId)
                    else -> try {
                        "cloud_json" to parseLessonItemsWithMigration(cloudJson)
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "resolveFirstChestLessonIndexForUser parse error partId=$partId", e)
                        "template(parse_error)" to createLessonItems(partId)
                    }
                }
                val templateChest = templateFirstChestLessonIndexOrNull(partId)
                var idx = items.indexOfFirst { it.type == LessonItem.TYPE_CHEST }
                val usedFallback = idx < 0
                if (usedFallback) {
                    idx = templateChest ?: -1
                }
                val typePreview = items.take(8).mapIndexed { i, it -> "$i:t${it.type}" }.joinToString(",")
                Log.d(
                    LOG_TAG,
                    "resolveFirstChest uid=${uid.take(8)} part=$partId docExists=${doc.exists()} source=$source " +
                        "itemCount=${items.size} templateChestIdx=$templateChest resolvedChestIdx=${idx.takeIf { it >= 0 }} " +
                        "fallback=$usedFallback types[$typePreview]",
                )
                onResult(idx.takeIf { it >= 0 })
            }
            .addOnFailureListener { e ->
                Log.e(LOG_TAG, "resolveFirstChestLessonIndexForUser load failed partId=$partId", e)
                onResult(templateFirstChestLessonIndexOrNull(partId))
            }
    }

    /**
     * lessonProgress'ta ilk Chest tamamlanmış ve mevcut sezon için [LessonItem.leaderboardSubmitScore] doluysa
     * liderlik girişini yazar (tüm zamanlar rekoru [record] ile karıştırılmaz).
     * Oyun içinde hiç [submitBestIfNeeded] tetiklenmediyse (veya veri silindiyse) profil senkronu sıralamayı düzeltir.
     * Yalnızca [uid] == oturum uid iken çalışır.
     */
    fun backfillLeaderboardFromStoredChest(uid: String, partId: Int, chestIndex: Int, onDone: () -> Unit) {
        if (uid.isBlank()) {
            onDone()
            return
        }
        val sessionUid = FirebaseAuth.getInstance().currentUser?.uid
        if (sessionUid != uid) {
            Log.d(LOG_TAG, "backfillLeaderboard: skip session uid != target uid")
            onDone()
            return
        }
        firestore.collection("users").document(uid).collection(FIRESTORE_LESSON_PROGRESS)
            .document(partId.toString())
            .get()
            .addOnSuccessListener { doc ->
                val json = doc.getString("items")
                if (json.isNullOrBlank()) {
                    Log.d(LOG_TAG, "backfillLeaderboard: no items json part=$partId")
                    onDone()
                    return@addOnSuccessListener
                }
                try {
                    val items = parseLessonItemsWithMigration(json)
                    val item = items.getOrNull(chestIndex)
                    if (item == null || item.type != LessonItem.TYPE_CHEST) {
                        Log.d(LOG_TAG, "backfillLeaderboard: no chest at idx=$chestIndex part=$partId")
                        onDone()
                        return@addOnSuccessListener
                    }
                    if (!item.stepIsFinish) {
                        Log.d(LOG_TAG, "backfillLeaderboard: chest not finished idx=$chestIndex")
                        onDone()
                        return@addOnSuccessListener
                    }
                    val seasonNow = SeasonClock.currentSeason()
                    val r = item.leaderboardSubmitScore(seasonNow)
                    if (r == null || r <= 0) {
                        Log.d(LOG_TAG, "backfillLeaderboard: no seasonal leaderboard score idx=$chestIndex part=$partId")
                        onDone()
                        return@addOnSuccessListener
                    }
                    Log.d(LOG_TAG, "backfillLeaderboard: submitBest part=$partId idx=$chestIndex seasonalScore=$r")
                    LessonLeaderboardRepository.submitBestIfNeeded(
                        partId,
                        chestIndex,
                        r,
                        season = seasonNow,
                        titleUnit = item.titleUnit?.trim()?.take(127),
                        onComplete = onDone,
                    )
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "backfillLeaderboard parse/error part=$partId", e)
                    onDone()
                }
            }
            .addOnFailureListener { e ->
                Log.e(LOG_TAG, "backfillLeaderboard load failed part=$partId", e)
                onDone()
            }
    }

    /**
     * Global state'i değiştirmeden (globalPartId/_lessonItems), belirli part/index için kullanıcıya ait lesson item'ı döndürür.
     * Kaynak olarak Firestore kullanılır (giriş yoksa default liste).
     */
    fun getLessonItemForPart(
        context: Context,
        partId: Int,
        index: Int,
        onResult: (LessonItem?) -> Unit,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onResult(createLessonItems(partId).getOrNull(index))
            return
        }

        firestore.collection("users").document(uid).collection(FIRESTORE_LESSON_PROGRESS)
            .document(partId.toString())
            .get()
            .addOnSuccessListener { doc ->
                val cloudJson = doc.getString("items")
                if (cloudJson.isNullOrBlank()) {
                    onResult(createLessonItems(partId).getOrNull(index))
                    return@addOnSuccessListener
                }
                try {
                    val cloudItems = parseLessonItemsWithMigration(cloudJson)
                    if (cloudItems.any { it.type == LessonItem.TYPE_CHEST && it.titleUnit.isNullOrBlank() }) {
                        backfillChestTitleUnitFromLocal(partId, cloudItems)
                    }
                    onResult(cloudItems.getOrNull(index))
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "getLessonItemForPart cloud parse error", e)
                    onResult(createLessonItems(partId).getOrNull(index))
                }
            }
            .addOnFailureListener { e ->
                Log.e(LOG_TAG, "getLessonItemForPart cloud load failed", e)
                onResult(createLessonItems(partId).getOrNull(index))
            }
    }

    /**
     * Kullanıcının tüm part'lerindeki (1..8) TYPE_CHEST + stepIsFinish=true item'ları döndürür.
     * Global state'i (_lessonItems/globalPartId) değiştirmez.
     */
    fun getFinishedChestItemsAcrossParts(
        context: Context,
        onResult: (List<ChestLessonRef>) -> Unit,
    ) {
        val partIds = 1..8
        val out = mutableListOf<ChestLessonRef>()

        fun collectAt(offset: Int) {
            if (offset >= partIds.count()) {
                onResult(out)
                return
            }
            val partId = partIds.elementAt(offset)
            getLessonItemsForPart(context, partId) { items ->
                items.forEachIndexed { idx, item ->
                    if (item.type == LessonItem.TYPE_CHEST && item.stepIsFinish) {
                        out.add(
                            ChestLessonRef(
                                partId = partId,
                                index = idx,
                                item = item,
                            ),
                        )
                    }
                }
                collectAt(offset + 1)
            }
        }

        collectAt(0)
    }

    /** Part lesson listesi (Firestore veya yerel şablon); günlük soru havuzu vb. için. */
    fun loadLessonItemsForPart(
        context: Context,
        partId: Int,
        onResult: (List<LessonItem>) -> Unit,
    ) {
        getLessonItemsForPart(context, partId, onResult)
    }

    private fun getLessonItemsForPart(
        context: Context,
        partId: Int,
        onResult: (List<LessonItem>) -> Unit,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onResult(createLessonItems(partId))
            return
        }

        firestore.collection("users").document(uid).collection(FIRESTORE_LESSON_PROGRESS)
            .document(partId.toString())
            .get()
            .addOnSuccessListener { doc ->
                val cloudJson = doc.getString("items")
                if (cloudJson.isNullOrBlank()) {
                    onResult(createLessonItems(partId))
                    return@addOnSuccessListener
                }
                try {
                    val cloudItems = parseLessonItemsWithMigration(cloudJson)
                    if (cloudItems.any { it.type == LessonItem.TYPE_CHEST && it.titleUnit.isNullOrBlank() }) {
                        backfillChestTitleUnitFromLocal(partId, cloudItems)
                    }
                    onResult(cloudItems)
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "getLessonItemsForPart cloud parse error", e)
                    onResult(createLessonItems(partId))
                }
            }
            .addOnFailureListener { e ->
                Log.e(LOG_TAG, "getLessonItemsForPart cloud load failed", e)
                onResult(createLessonItems(partId))
            }
    }


    fun createLessonItems(partId : Int): List<LessonItem> {
        return when(partId) {
            1 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Sayıları Abaküste Tanıma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Sayıları Abaküste Tanıma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 2,
                    currentStep = 1,
                    finishStepNumber = 2,
                    tutorialNumber = 1,
                    startStepNumber = 1,
                    mapFragmentIndex = 1,
                    lessonHint = "En sağdaki sütunu kullan. Aşağıda boncuklar birlik, yukarıdaki beşlik değere sahip."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "2 Basamaklı Sayılar",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 2,
                    currentStep = 1,
                    finishStepNumber = 1001,
                    tutorialNumber = 100,
                    startStepNumber = 1000,
                    mapFragmentIndex = 2,
                    lessonHint = "Sayıları abaküse en büyük basamaktan başlayarak yaz."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "3-4-5 Basamaklı Sayılar",
                    offset = 30,
                    isCompleted = false,
                    stepCount = 2,
                    currentStep = 1,
                    finishStepNumber = 1004,
                    tutorialNumber = 101,
                    startStepNumber = 1003,
                    mapFragmentIndex = 3,
                    lessonHint = "Sayıları abaküse en büyük basamaktan başlayarak yaz."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    titleUnit = "Sayıları Abaküste Tanıma",
                    offset = 0,
                    isCompleted = true,
                    stepIsFinish = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 4,
                    finishStepNumber = 1005,
                    startStepNumber = 1005,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupPoint1 = 1600,
                    cupPoint2 = 1300,
                    worstCupTime = 150 //1 yıldız alması için min kaç saniyede bitirmeli.
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Kuralsız toplama",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Kuralsız Toplama - 1 Basamaklı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 2,
                    currentStep = 1,
                    tutorialNumber = 2,
                    startStepNumber = 4,
                    mapFragmentIndex = 6,
                    finishStepNumber = 5,
                    lessonHint = "İlk sayıyı yaz. Toplanacak sayı değerinde boncuk ekle.",
                    abacusGuideNumber = 1
                ),LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Kuralsız Toplama - 2 Basamaklı",
                    offset = -30,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialNumber = 102,
                    startStepNumber = 1007,
                    mapFragmentIndex = 7,
                    finishStepNumber = 1007,
                    lessonHint = "Toplanacak sayıyı en büyük basamaktan başlayarak ekle.",
                ),LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Kuralsız Toplama - 3 Basamaklı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 2,
                    currentStep = 1,
                    tutorialNumber = 103,
                    startStepNumber = 1010,
                    mapFragmentIndex = 8,
                    finishStepNumber = 1011,
                    lessonHint = "Toplanacak sayıyı en büyük basamaktan başlayarak ekle.",
                ),LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Kuralsız Toplama - 4, 5 Basamaklı",
                    offset = 30,
                    isCompleted = false,
                    stepCount = 2,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    startStepNumber = 1013,
                    mapFragmentIndex = 9,
                    finishStepNumber = 1014,
                    lessonHint = "Toplanacak sayıyı en büyük basamaktan başlayarak ekle.",
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    titleUnit = "Kuralsız Toplama",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    stepIsFinish = true,
                    mapFragmentIndex = 10,
                    finishStepNumber = 7, //finish ve start 7 olarak güncellenecek test için 1005
                    startStepNumber = 7,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupPoint1 = 1300,
                    cupPoint2 = 1000,
                    worstCupTime = 150

                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "5'lik Toplama",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_pink

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "5'lik Toplama - Kurallar",
                    offset = -30,
                    isCompleted = false,
                    stepCount = 3,
                    currentStep = 1,
                    tutorialNumber = 3,
                    startStepNumber = 8,
                    mapFragmentIndex = 12,
                    finishStepNumber = 10,
                    lessonHint = "5 gelir. Eklenecek sayının kardeşi gider.",
                    abacusGuideNumber = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "5'lik Toplama - Kuralsız Toplama Farkı",
                    offset = -60,
                    isCompleted = false,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 12,
                    mapFragmentIndex = 13,
                    finishStepNumber = 14,
                    tutorialNumber = 4,
                    lessonHint = "Önce doğrudan eklemeyi dene. Olmuyorsa 5'lik kuralı kullan.",

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "5'lik Toplama - 3, 4 Basamaklı",
                    offset = -30,
                    isCompleted = false,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 16,
                    mapFragmentIndex = 14,
                    finishStepNumber = 18,
                    tutorialIsFinish = true,
                    lessonHint = "Önce doğrudan eklemeyi dene. Olmuyorsa 5'lik kuralı kullan.",
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    titleUnit = "5'lik Toplama",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    stepIsFinish = true,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 15,
                    startStepNumber = 19,
                    finishStepNumber = 19,
                    cupPoint1 = 1400,
                    cupPoint2 = 1200,
                    worstCupTime = 180,
                    lessonHint = "5 gelir. Eklenecek sayının kardeşi gider.",
                    ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "10'luk Toplama 1-2-3-4-5",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk Toplama - Kurallar",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 20,
                    mapFragmentIndex = 17,
                    finishStepNumber = 22,
                    tutorialNumber = 5,
                    lessonHint = "10 gelir. Büyük kardeş gider.",
                    abacusGuideNumber = 3

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk toplama - Elde Mantığı",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 24,
                    mapFragmentIndex = 18,
                    finishStepNumber = 26,
                    tutorialNumber = 6,
                    lessonHint = "'10 gelir' adımını uygularken 5'lik veya 10'luk kuralı kullan."

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk Toplama - 3 Basamaklı",
                    offset = -60,
                    isCompleted = false,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 28,
                    mapFragmentIndex = 19,
                    finishStepNumber = 31,
                    tutorialIsFinish = true,
                    lessonHint = "Kuralsız, 5'lik toplama ve 10'luk toplama kurallarını kullan."
                ),LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk Toplama - 3, 4 Basamaklı",
                    offset = -30,
                    isCompleted = false,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 32,
                    mapFragmentIndex = 20,
                    finishStepNumber = 34,
                    tutorialIsFinish = true,
                    lessonHint = "Kuralsız, 5'lik toplama ve 10'luk toplama kurallarını kullan."

                ),LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    titleUnit = "10'luk Toplama 1-2-3-4-5",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 21,
                    startStepNumber = 36,
                    finishStepNumber = 36,
                    lessonHint = "Kuralsız, 5'lik toplama ve 10'luk toplama kurallarını kullan.",
                    cupPoint1 = 1350,
                    cupPoint2 = 1150,
                    worstCupTime = 180,

                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "10'luk Toplama 6-7-8-9",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_orange

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk Toplama - Kurallar",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 37,
                    mapFragmentIndex = 23,
                    finishStepNumber = 38,
                    tutorialNumber = 7,
                    lessonHint = "10 gelir. Kardeş gider."

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk Toplama - Elde Mantığı",
                    offset = -30,
                    isCompleted = false,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 41,
                    mapFragmentIndex = 24,
                    finishStepNumber = 43,
                    tutorialNumber = 105,
                    lessonHint = "'10 gelir' adımını uygularken 5'lik veya 10'luk kuralı kullan."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk Toplama - 3 Basamaklı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 44,
                    mapFragmentIndex = 25,
                    finishStepNumber = 46,
                    tutorialIsFinish = true,
                    lessonHint = "İlk sayıyı yaz. Toplamaya 2. sayının en büyük basamağından başla."

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk Toplama - 3, 4 Basamaklı",
                    offset = 30,
                    isCompleted = false,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 48,
                    mapFragmentIndex = 26,
                    finishStepNumber = 50,
                    tutorialIsFinish = true,
                    lessonHint = "İlk sayıyı yaz. Toplamaya 2. sayının en büyük basamağından başla."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    titleUnit = "10'luk Toplama 6-7-8-9",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 27,
                    startStepNumber = 52,
                    finishStepNumber = 52,
                    cupPoint1 = 1350,
                    cupPoint2 = 1150,
                    worstCupTime = 180,

                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Boncuk kuralı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Boncuk Kuralı - Kurallar",
                    offset = -30,
                    isCompleted = false,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 53,
                    mapFragmentIndex = 29,
                    finishStepNumber = 56,
                    tutorialNumber = 8,
                    lessonHint = "Kuralsız ve 10'luk kural ile ekleyemediğinde boncuk kuralını kullan"

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Boncuk Kuralı - Onluk Kural Farkı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 57,
                    mapFragmentIndex = 30,
                    finishStepNumber = 59,
                    tutorialNumber = 9,
                    lessonHint = "10 gelir adımını uygularken 5'lik kuralı kullanman gerekebilir."

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Boncuk Kuralı - 3, 4 Basamaklı",
                    offset = -30,
                    isCompleted = false,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 61,
                    mapFragmentIndex = 31,
                    finishStepNumber = 63,
                    tutorialIsFinish = true,
                    lessonHint = "10 gelir adımını uygularken 5\\'lik kuralı kullanman gerekebilir."

                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    titleUnit = "Boncuk Kuralı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 32,
                    startStepNumber = 65,
                    finishStepNumber = 65,
                    cupPoint1 = 1350,
                    cupPoint2 = 1000,
                    worstCupTime = 180,
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Ustalık Yolu",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_green
                ),
                LessonItem(
                    type = LessonItem.TYPE_RACE,
                    title = "Ustalık Yolu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = false,
                    mapFragmentIndex = 34,
                    racePartId = 7,
                    backRaceId = 1
                    ),
                LessonItem(
                    partId = 2,
                    type = LessonItem.TYPE_PART,
                    title = "2. Kısım",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 35,
                    sectionTitle = "2. Kısım Çıkarma",
                    sectionDescription = "Abaküste çıkarmaya dair her şeyi öğreneceğiz. "

                )
            )
            2 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Kuralsız Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_BACK_PART,
                    title = "1. Kısım",
                    offset = 0,
                    isCompleted = true,
                    partId = 1,
                    stepCount = 1,
                    currentStep = 1,
                    backPart = true,
                    sectionTitle = "2. Kısım Çıkarma",
                    sectionDescription = "Abaküste çıkarmaya dair her şeyi öğreneceğiz."

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Kuralsız Çıkarma - Temeli",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 66,
                    mapFragmentIndex = 2,
                    finishStepNumber = 68,
                    tutorialNumber = 10,
                    lessonHint = "İlk sayıyı yaz. Çıkarmaya 2. sayının en büyük basamağından başla."

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Kuralsız Çıkarma - 3,4,5 basamaklı",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    startStepNumber = 70,
                    mapFragmentIndex = 3,
                    finishStepNumber = 73,
                    lessonHint = "İlk sayıyı yaz. Çıkarmaya 2. sayının en büyük basamağından başla."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    titleUnit = "Kuralsız Çıkarma",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 4,
                    finishStepNumber = 74,
                    startStepNumber = 74,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupPoint1 = 1400,
                    cupPoint2 = 1200,
                    worstCupTime = 180

                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "5'lik Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_green
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "5'lik Çıkarma - Kurallar",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    tutorialNumber = 11,
                    startStepNumber = 75,
                    mapFragmentIndex = 6,
                    finishStepNumber = 77,
                    lessonHint = "Çıkarmaya büyük basamaktan başla. 5 gider. Kardeş gelir."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "5'lik Çıkarma - 3 Basamaklı",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    startStepNumber = 79,
                    mapFragmentIndex = 7,
                    finishStepNumber = 81,
                    lessonHint = "Çıkarmaya büyük basamaktan başla. 5 gider. Kardeş gelir."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "5'lik Çıkarma - Kurallı,Kuralsız Çıkarma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    startStepNumber = 83,
                    tutorialNumber = 1100,
                    mapFragmentIndex = 8,
                    finishStepNumber = 85,
                    lessonHint = "Çıkarmaya büyük basamaktan başla. 5 gider. Kardeş gelir."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 9,
                    finishStepNumber = 87,
                    startStepNumber = 87,
                    tutorialIsFinish = true,
                    lessonHint = "Çıkarmaya büyük basamaktan başla. 5 gider. Kardeş gelir.",
                    cupPoint1 = 1350,
                    cupPoint2 = 1050,
                    worstCupTime = 180
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "10'luk Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk Çıkarma - Kurallar",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    tutorialNumber = 12,
                    startStepNumber = 88,
                    mapFragmentIndex = 11,
                    finishStepNumber = 90,
                    lessonHint = "10 gider. Kardeş gelir."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk Çıkarma - 2 basamaklı",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    tutorialNumber = 13,
                    startStepNumber = 92,
                    mapFragmentIndex = 12,
                    finishStepNumber = 94,
                    lessonHint = "10 gider. Kardeş gelir."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk Çıkarma - Komşu Mantığı",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    tutorialNumber = 14,
                    startStepNumber = 96,
                    mapFragmentIndex = 13,
                    finishStepNumber = 98,
                    lessonHint = "10 gider adımını yaparken 5'lik olmazsa 10'luk çıkarma kuralı kullan."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "10'luk Çıkarma - İleri Seviye",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    tutorialNumber = 15,
                    startStepNumber = 100,
                    mapFragmentIndex = 14,
                    finishStepNumber = 102,
                    lessonHint = "İlk kuralsız çıkarmayı dene. Olmuyorsa 5'lik çıkarma. Olmuyorsa 10'luk çıkarma."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 15,
                    finishStepNumber = 104,
                    startStepNumber = 104,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupPoint1 = 1500,
                    cupPoint2 = 1300,
                    worstCupTime = 180
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Boncuk Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_yellow
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Boncuk Çıkarma - Kurallar",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    tutorialNumber = 16,
                    startStepNumber = 105,
                    mapFragmentIndex = 17,
                    finishStepNumber = 107,
                    lessonHint = "Kardeş gelirken 5'lik kural uygula."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Boncuk Çıkarma - 3 Basamaklı",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    tutorialNumber = 17,
                    startStepNumber = 109,
                    mapFragmentIndex = 18,
                    finishStepNumber = 111,
                    lessonHint = "Kardeş gelirken 5'lik kural uygula."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Boncuk Çıkarma - İleri Seviye",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 3,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    startStepNumber = 113,
                    mapFragmentIndex = 19,
                    finishStepNumber = 115,
                    lessonHint = "Kardeş gelirken 5'lik kural uygula."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 20,
                    finishStepNumber = 117,
                    startStepNumber = 117,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    cupPoint1 = 1300,
                    cupPoint2 = 1000,
                    worstCupTime = 180
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Ustalık Yolu",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_green
                ),
                LessonItem(
                    type = LessonItem.TYPE_RACE,
                    title = "Ustalık Yolu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 21,
                    racePartId = 8,
                    backRaceId = 2
                ),
                LessonItem(
                    partId = 3,
                    type = LessonItem.TYPE_PART,
                    title = "3. Kısım",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 21,
                    sectionTitle = "3. Kısım Çarpma",
                    sectionDescription = "Abaküste çarpmaya dair her şey. "

                )
            )
            3 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "2'ye 1 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_BACK_PART,
                    title = "2. Kısım",
                    offset = 0,
                    isCompleted = true,
                    partId = 2,
                    stepCount = 1,
                    currentStep = 1,
                    backPart = true,
                    sectionTitle = "3. Kısım Çarpma",
                    sectionDescription = "Abaküste çarpmaya dair her şey."

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 2'ye 1 çarpma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 118,
                    mapFragmentIndex = 2,
                    finishStepNumber = 121,
                    tutorialNumber = 18,
                    lessonHint = "Birler ile onlar basamağı çarpılınca sonuç onlar basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 2'ye 1 çarpma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 122,
                    mapFragmentIndex = 3,
                    finishStepNumber = 125,
                    tutorialNumber = 19,
                    lessonHint = "Birler ile onlar basamağı çarpılınca sonuç onlar basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 2'ye 1 çarpma",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 126,
                    mapFragmentIndex = 4,
                    finishStepNumber = 129,
                    tutorialIsFinish = true,
                    lessonHint = "Birler ile onlar basamağı çarpılınca sonuç onlar basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 5,
                    finishStepNumber = 130,
                    startStepNumber = 130,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    //cupTime1 = "1:00",
                    //cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "2'ye 2 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 2'ye 2 çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 131,
                    mapFragmentIndex = 7,
                    finishStepNumber = 134,
                    tutorialNumber = 20,
                    lessonHint = "Onlar ile onlar basamağı çarpılınca sonuç yüzler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 2'ye 2 çarpma",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 131,
                    mapFragmentIndex = 8,
                    finishStepNumber = 134,
                    tutorialIsFinish = true,
                    lessonHint = "Onlar ile onlar basamağı çarpılınca sonuç yüzler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 2'ye 2 çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 135,
                    mapFragmentIndex = 9,
                    finishStepNumber = 138,
                    tutorialIsFinish = true,
                    lessonHint = "Onlar ile onlar basamağı çarpılınca sonuç yüzler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 10,
                    finishStepNumber = 139,
                    startStepNumber = 139,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    //cupTime1 = "3:30",
                    //cupTime2 = "5:00"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "3'e 1 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_orange
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 3'e 1 çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 140,
                    mapFragmentIndex = 12,
                    finishStepNumber = 143,
                    tutorialNumber = 21,
                    lessonHint = "Yüzler ile birler basamağı çarpılınca sonuç yüzler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 3'e 1 çarpma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 140,
                    mapFragmentIndex = 13,
                    finishStepNumber = 143,
                    tutorialIsFinish = true,
                    lessonHint = "Yüzler ile birler basamağı çarpılınca sonuç yüzler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 3'e 1 çarpma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 140,
                    mapFragmentIndex = 14,
                    finishStepNumber = 143,
                    tutorialIsFinish = true,
                    lessonHint = "Yüzler ile birler basamağı çarpılınca sonuç yüzler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 15,
                    finishStepNumber = 144,
                    startStepNumber = 144,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    //cupTime1 = "2:30",
                    //cupTime2 = "3:30"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "3'e 2 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_pink
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 3'e 2 çarpma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 145,
                    mapFragmentIndex = 17,
                    finishStepNumber = 148,
                    tutorialNumber = 22,
                    lessonHint = "Yüzler ile onlar basamağı çarpılınca sonuç binler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 3'e 2 çarpma",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 145,
                    mapFragmentIndex = 18,
                    finishStepNumber = 148,
                    tutorialIsFinish = true,
                    lessonHint = "Yüzler ile onlar basamağı çarpılınca sonuç binler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 3'e 2 çarpma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 149,
                    mapFragmentIndex = 19,
                    finishStepNumber = 152,
                    tutorialNumber = 23,
                    lessonHint = "Yüzler ile onlar basamağı çarpılınca sonuç binler basamağına yazılır."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 20,
                    finishStepNumber = 153,
                    startStepNumber = 153,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    //cupTime1 = "3:00",
                    //cupTime2 = "5:30"
                ),
                LessonItem(
                    partId = 4,
                    type = LessonItem.TYPE_PART,
                    title = "4. Kısım",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 21,
                    sectionTitle = "4. Körleme Toplama",
                    sectionDescription = "Akıldan Toplama"
                )
            )
            4 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Kuralsız Toplama",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_green
                ),
                LessonItem(
                    type = LessonItem.TYPE_BACK_PART,
                    title = "3. Kısım",
                    offset = 0,
                    isCompleted = true,
                    partId = 2,
                    stepCount = 1,
                    currentStep = 1,
                    backPart = true,
                    sectionTitle = "3. Kısım Çarpma",
                    sectionDescription = "Abaküste çarpmaya dair her şey. "

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit Kuralsız Toplama",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 1,
                    mapFragmentIndex = 2,
                    finishStepNumber = 4,
                    isBlinding = true,
                    tutorialNumber = 24,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit Kuralsız Toplama",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 5,
                    mapFragmentIndex = 3,
                    finishStepNumber = 8,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit Kuralsız Toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 9,
                    mapFragmentIndex = 4,
                    finishStepNumber = 12,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 5,
                    finishStepNumber = 13,
                    startStepNumber = 13,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    //cupTime1 = "1:00",
                    //cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "5'lik Toplama",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 5'lik Toplama",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 14,
                    mapFragmentIndex = 7,
                    finishStepNumber = 17,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 5'lik Toplama",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 14,
                    mapFragmentIndex = 8,
                    finishStepNumber = 17,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2500,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 5'lik Toplama",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 14,
                    mapFragmentIndex = 9,
                    finishStepNumber = 17,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 10,
                    finishStepNumber = 18,
                    startStepNumber = 18,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    //cupTime1 = "1:00",
                    //cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "10'luk Toplama 1-2-3-4-5",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_blue
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit 10'luk Toplama",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 19,
                    mapFragmentIndex = 12,
                    finishStepNumber = 22,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 10'luk Toplama",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 19,
                    mapFragmentIndex = 13,
                    finishStepNumber = 22,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 10'luk Toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 19,
                    mapFragmentIndex = 14,
                    finishStepNumber = 22,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 15,
                    finishStepNumber = 23,
                    startStepNumber = 23,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    //cupTime1 = "1:00",
                    //cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "10'luk Toplama 6,7,8,9",
                    offset = 30,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit 10'luk Toplama",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 24,
                    mapFragmentIndex = 17,
                    finishStepNumber = 27,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 10'luk Toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 24,
                    mapFragmentIndex = 18,
                    finishStepNumber = 27,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 10'luk Toplama",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 24,
                    mapFragmentIndex = 19,
                    finishStepNumber = 27,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 20,
                    finishStepNumber = 28,
                    startStepNumber = 28,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    //cupTime1 = "1:00",
                    //cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Boncuk Kuralı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_green
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel Boncuk Toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 29,
                    mapFragmentIndex = 22,
                    finishStepNumber = 32,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor Boncuk Toplama",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 29,
                    mapFragmentIndex = 23,
                    finishStepNumber = 32,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız Boncuk Toplama",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 29,
                    mapFragmentIndex = 24,
                    finishStepNumber = 32,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "5 gelir. Kardeş gider."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 25,
                    finishStepNumber = 33,
                    startStepNumber = 33,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    //cupTime1 = "1:00",
                    //cupTime2 = "1:45"
                ),
                LessonItem(
                    partId = 5,
                    type = LessonItem.TYPE_PART,
                    title = "5. Kısım",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 25,
                    sectionTitle = "5. Körleme Çıkarma",
                    sectionDescription = "Akıldan Çıkarma"
                )

            )
            5 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Kuralsız Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_BACK_PART,
                    title = "4. Kısım",
                    offset = 0,
                    isCompleted = true,
                    partId = 4,
                    stepCount = 1,
                    currentStep = 1,
                    backPart = true,
                    sectionTitle = "4. Kısım Körleme Toplama",
                    sectionDescription = "Körleme Toplama"

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit Kuralsız Çıkarma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 34,
                    mapFragmentIndex = 2,
                    finishStepNumber = 37,
                    isBlinding = true,
                    tutorialNumber = 25,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor Kuralsız Çıkarma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 34,
                    mapFragmentIndex = 3,
                    finishStepNumber = 37,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız Kuralsız Çıkarma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 34,
                    mapFragmentIndex = 4,
                    finishStepNumber = 37,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 5,
                    finishStepNumber = 38,
                    startStepNumber = 38,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    //cupTime1 = "1:00",
                    //cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "5'lik Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit 5'lik Çıkarma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 39,
                    mapFragmentIndex = 7,
                    finishStepNumber = 42,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 5'lik Çıkarma",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 39,
                    mapFragmentIndex = 8,
                    finishStepNumber = 42,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 5'lik Çıkarma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 39,
                    mapFragmentIndex = 9,
                    finishStepNumber = 42,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 10,
                    finishStepNumber = 43,
                    startStepNumber = 43,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    //cupTime1 = "1:00",
                    //cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "10'luk Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel 10'luk Çıkarma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 44,
                    mapFragmentIndex = 12,
                    finishStepNumber = 47,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 10'luk Çıkarma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 44,
                    mapFragmentIndex = 13,
                    finishStepNumber = 47,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 10'luk Çıkarma",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 44,
                    mapFragmentIndex = 14,
                    finishStepNumber = 47,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 15,
                    finishStepNumber = 48,
                    startStepNumber = 48,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    //cupTime1 = "1:00",
                    //cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Boncuk Çıkarma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel Boncuk Çıkarma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 49,
                    mapFragmentIndex = 17,
                    finishStepNumber = 52,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel Boncuk Çıkarma",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 49,
                    mapFragmentIndex = 18,
                    finishStepNumber = 52,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Temel Boncuk Çıkarma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 49,
                    mapFragmentIndex = 19,
                    finishStepNumber = 52,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 20,
                    finishStepNumber = 53,
                    startStepNumber = 53,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    timePeriod = 2000,
                    //cupTime1 = "1:00",
                    //cupTime2 = "1:45"
                ),
                LessonItem(
                    partId = 6,
                    type = LessonItem.TYPE_PART,
                    title = "6. Kısım",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    tutorialIsFinish = true,
                    mapFragmentIndex = 21,
                    sectionTitle = "6. Körleme Çarpma",
                    sectionDescription = "Akıldan Çarpma"
                )
            )
            6 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Körleme Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_BACK_PART,
                    title = "5. Kısım",
                    offset = 0,
                    isCompleted = true,
                    partId = 5,
                    stepCount = 1,
                    currentStep = 1,
                    backPart = true,
                    sectionTitle = "5. Kısım Körleme Çıkarma",
                    sectionDescription = "Körleme Çıkarma"

                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit Temel Çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 54,
                    mapFragmentIndex = 2,
                    finishStepNumber = 57,
                    isBlinding = true,
                    tutorialNumber = 26,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor Temel Çarpma",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 54,
                    mapFragmentIndex = 3,
                    finishStepNumber = 57,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız Çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 58,
                    mapFragmentIndex = 4,
                    finishStepNumber = 61,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 5,
                    finishStepNumber = 62,
                    startStepNumber = 62,
                    blindingMultiplication = true,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    //cupTime1 = "1:00",
                    //cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "2'ye 1 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "2'ye 1 Çarpma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 63,
                    mapFragmentIndex = 7,
                    finishStepNumber = 66,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "2'ye 1 Çarpma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 63,
                    mapFragmentIndex = 8,
                    finishStepNumber = 66,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "2'ye 1 Çarpma",
                    offset = -60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 63,
                    mapFragmentIndex = 9,
                    finishStepNumber = 66,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 10,
                    finishStepNumber = 67,
                    startStepNumber = 67,
                    blindingMultiplication = true,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    //cupTime1 = "1:00",
                    //cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "Temel 2'ye 2 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Basit 2'ye 2 Çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 68,
                    mapFragmentIndex = 12,
                    finishStepNumber = 71,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zor 2'ye 2 Çarpma",
                    offset = 60,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 68,
                    mapFragmentIndex = 13,
                    finishStepNumber = 71,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "İmkansız 2'ye 2 Çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 68,
                    mapFragmentIndex = 14,
                    finishStepNumber = 71,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 15,
                    finishStepNumber = 72,
                    startStepNumber = 72,
                    blindingMultiplication = true,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    //cupTime1 = "1:00",
                    //cupTime2 = "1:45"
                ),
                LessonItem(
                    type = LessonItem.TYPE_HEADER,
                    title = "2'ye 2 Çarpma",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    color = R.color.lesson_header_red
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "2'ye 2 Çarpma",
                    offset = -30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 73,
                    mapFragmentIndex = 17,
                    finishStepNumber = 76,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "2'ye 2 Çarpma",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 73,
                    mapFragmentIndex = 18,
                    finishStepNumber = 76,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "2'ye 2 Çarpma",
                    offset = 30,
                    isCompleted = true,
                    stepCount = 4,
                    currentStep = 1,
                    startStepNumber = 73,
                    mapFragmentIndex = 19,
                    finishStepNumber = 76,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    blindingMultiplication = true,
                    lessonHint = "Hayali abaküsü parmaklarınla hareket ettirmeyi deneyebilirsin."
                ),
                LessonItem(
                    type = LessonItem.TYPE_CHEST,
                    title = "Ünite Maratonu",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    mapFragmentIndex = 20,
                    finishStepNumber = 77,
                    startStepNumber = 77,
                    blindingMultiplication = true,
                    isBlinding = true,
                    tutorialIsFinish = true,
                    lessonHint = "Hatasız, en kısa sürede bitir.",
                    //cupTime1 = "2:00",
                    //cupTime2 = "3:00"
                )
            )
            7 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Acemi Çırak",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 78,
                    mapFragmentIndex = 0,
                    finishStepNumber = 78,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    raceBusyLevel = 1,
                    raceTitle = "1 basamaklı 4 adet sayı"
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Başlangıç Ustası",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 78,
                    mapFragmentIndex = 1,
                    finishStepNumber = 78,
                    tutorialIsFinish = true,
                    timePeriod = 2000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Bilgi Avcısı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 78,
                    mapFragmentIndex = 2,
                    finishStepNumber = 78,
                    tutorialIsFinish = true,
                    timePeriod = 1000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Öğrenme Ustası",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 1000,
                    mapFragmentIndex = 3,
                    finishStepNumber = 1000,
                    tutorialIsFinish = true,
                    timePeriod = 1000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zihin Kaşifi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 80,
                    mapFragmentIndex = 4,
                    finishStepNumber = 80,
                    tutorialIsFinish = true,
                    timePeriod = 4000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Bilgelik Ustası",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 80,
                    mapFragmentIndex = 5,
                    finishStepNumber = 80,
                    tutorialIsFinish = true,
                    timePeriod = 2500,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zeka Kaşifi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 81,
                    mapFragmentIndex = 6,
                    finishStepNumber = 81,
                    tutorialIsFinish = true,
                    timePeriod = 8000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Beyin Mühendisi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 81,
                    mapFragmentIndex = 7,
                    finishStepNumber = 81,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Mantık Üstadı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 82,
                    mapFragmentIndex = 8,
                    finishStepNumber = 82,
                    tutorialIsFinish = true,
                    timePeriod = 4000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Deha",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 82,
                    mapFragmentIndex = 9,
                    finishStepNumber = 82,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Üst Zihin",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1, //burdayız bra
                    startStepNumber = 83,
                    mapFragmentIndex = 10,
                    finishStepNumber = 83,
                    tutorialIsFinish = true,
                    timePeriod = 8000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Bilge Şampiyon",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 83,
                    mapFragmentIndex = 11,
                    finishStepNumber = 83,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zeka Mimarı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 84,
                    mapFragmentIndex = 12,
                    finishStepNumber = 84,
                    tutorialIsFinish = true,
                    timePeriod = 4000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Ustalık Efendisi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 84,
                    mapFragmentIndex = 13,
                    finishStepNumber = 84,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    raceBusyLevel = 2
                ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Efsanevi Bilge",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 84,
                    mapFragmentIndex = 14,
                    finishStepNumber = 84,
                    tutorialIsFinish = true,
                    timePeriod = 2500,
                    raceBusyLevel = 2
                )
            )
            8 -> listOf(
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Acemi Çırak",
                    offset = 0,
                    isCompleted = true,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 85,
                    mapFragmentIndex = 0,
                    finishStepNumber = 85,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Başlangıç Ustası",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 85,
                    mapFragmentIndex = 1,
                    finishStepNumber = 85,
                    tutorialIsFinish = true,
                    timePeriod = 1000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Bilgi Avcısı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 86,
                    mapFragmentIndex = 2,
                    finishStepNumber = 86,
                    tutorialIsFinish = true,
                    timePeriod = 8000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Öğrenme Ustası",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 86,
                    mapFragmentIndex = 3,
                    finishStepNumber = 86,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zihin Kaşifi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 87,
                    mapFragmentIndex = 4,
                    finishStepNumber = 87,
                    tutorialIsFinish = true,
                    timePeriod = 4000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Bilgelik Ustası",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 87,
                    mapFragmentIndex = 5,
                    finishStepNumber = 87,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zeka Kaşifi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 88,
                    mapFragmentIndex = 6,
                    finishStepNumber = 88,
                    tutorialIsFinish = true,
                    timePeriod = 8000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Beyin Mühendisi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 88,
                    mapFragmentIndex = 7,
                    finishStepNumber = 88,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Mantık Üstadı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 89,
                    mapFragmentIndex = 8,
                    finishStepNumber = 89,
                    tutorialIsFinish = true,
                    timePeriod = 4000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Deha",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 89,
                    mapFragmentIndex = 9,
                    finishStepNumber = 89,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Üst Zihin",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1, //burdayız bra
                    startStepNumber = 90,
                    mapFragmentIndex = 10,
                    finishStepNumber = 90,
                    tutorialIsFinish = true,
                    timePeriod = 8000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Bilge Şampiyon",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 90,
                    mapFragmentIndex = 11,
                    finishStepNumber = 90,
                    tutorialIsFinish = true,
                    timePeriod = 5000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Zeka Mimarı",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 90,
                    mapFragmentIndex = 12,
                    finishStepNumber = 90,
                    tutorialIsFinish = true,
                    timePeriod = 4000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Ustalık Efendisi",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 91,
                    mapFragmentIndex = 13,
                    finishStepNumber = 91,
                    tutorialIsFinish = true,
                    timePeriod = 3000,
                    raceBusyLevel = 1,

                    ),
                LessonItem(
                    type = LessonItem.TYPE_LESSON,
                    title = "Efsanevi Bilge",
                    offset = 0,
                    isCompleted = false,
                    stepCount = 1,
                    currentStep = 1,
                    startStepNumber = 91,
                    mapFragmentIndex = 14,
                    finishStepNumber = 91,
                    tutorialIsFinish = true,
                    timePeriod = 2500,
                    raceBusyLevel = 1,

                    )
            )


            else -> emptyList()
        }
    }

    // SharedPreferences işlemleri - her kullanıcıya özel (uid ile ayrı dosya)
    private const val PREFS_PREFIX = "LessonPrefs_"
    private const val KEY_LESSON_ITEMS = "lesson_items"
    private fun getKey(partId: Int) = "lesson_items_part_$partId"

    /** Giriş yapmış kullanıcı için LessonPrefs_uid, giriş yoksa LessonPrefs_guest */
    private fun getLessonPrefs(context: Context): android.content.SharedPreferences {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        return context.getSharedPreferences("$PREFS_PREFIX$uid", Context.MODE_PRIVATE)
    }

    fun saveToPreferences(context: Context, saveRemote: Boolean = true) {
        val gson = Gson()
        val json = gson.toJson(_lessonItems)
        val completedCount = _lessonItems.count { it.stepIsFinish }
        val chestCompletedCount = _lessonItems.count { it.type == LessonItem.TYPE_CHEST && it.stepIsFinish }
        Log.d(LOG_TAG, "saveToPreferences partId=$globalPartId cloud-only (items=${_lessonItems.size})")

        // Giriş yapmış kullanıcı için Firestore'a da kaydet (uygulama silinse bile geri yüklensin)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        // #region agent log
        debugLog(
            hypothesisId = "H2",
            location = "GlobalLessonData.kt:saveToPreferences",
            message = "save local/cloud lesson state",
            data = mapOf(
                "partId" to globalPartId,
                "itemCount" to _lessonItems.size,
                "completedCount" to completedCount,
                "chestCompletedCount" to chestCompletedCount,
                "saveRemote" to saveRemote,
                "uidPresent" to (uid != null),
            ),
        )
        // #endregion
        if (saveRemote && uid != null) {
            Log.d(LOG_TAG, "saveToPreferences -> Firestore users/${uid.take(8)}.../lessonProgress/$globalPartId")
            firestore.collection("users").document(uid).collection(FIRESTORE_LESSON_PROGRESS)
                .document(globalPartId.toString())
                .set(mapOf("items" to json))
                .addOnSuccessListener {
                    LessonProgressDiag.logListChestFinishSummary(
                        "GlobalLessonData.saveToPreferences.SUCCESS",
                        globalPartId,
                        _lessonItems,
                    )
                    Log.d(LOG_TAG, "saveToPreferences Firestore SUCCESS")
                    // #region agent log
                    debugLog(
                        hypothesisId = "H3",
                        location = "GlobalLessonData.kt:saveToPreferences:firestoreSuccess",
                        message = "lesson cloud write success",
                        data = mapOf("partId" to globalPartId),
                    )
                    // #endregion
                }
                .addOnFailureListener { e ->
                    LessonProgressDiag.log(
                        "GlobalLessonData.saveToPreferences.FAILED",
                        "part=$globalPartId err=${e.message}",
                    )
                    Log.e(LOG_TAG, "saveToPreferences Firestore FAILED", e)
                    // #region agent log
                    debugLog(
                        hypothesisId = "H3",
                        location = "GlobalLessonData.kt:saveToPreferences:firestoreFailure",
                        message = "lesson cloud write failed",
                        data = mapOf(
                            "partId" to globalPartId,
                            "errorType" to e.javaClass.simpleName,
                            "errorMessage" to (e.message ?: "null"),
                        ),
                    )
                    // #endregion
                }
        } else {
            if (!saveRemote) Log.d(LOG_TAG, "saveToPreferences skip Firestore (saveRemote=false)")
            else if (uid == null) Log.d(LOG_TAG, "saveToPreferences skip Firestore (uid=null)")
        }
    }

    fun loadFromPreferences(context: Context): Boolean {
        val key = getKey(globalPartId)
        val prefs = getLessonPrefs(context)
        val json = prefs.getString(key, null)
        // #region agent log
        debugLog(
            hypothesisId = "H1",
            location = "GlobalLessonData.kt:loadFromPreferences",
            message = "local lesson prefs lookup",
            data = mapOf(
                "partId" to globalPartId,
                "uidPresent" to (FirebaseAuth.getInstance().currentUser?.uid != null),
                "hasJson" to (json != null),
            ),
        )
        // #endregion
        return if (json != null) {
            try {
                _lessonItems = parseLessonItemsWithMigration(json)
                Log.d(LOG_TAG, "loadFromPreferences key=$key -> loaded ${_lessonItems.size} items from LOCAL")
                true
            } catch (e: Exception) {
                Log.e(LOG_TAG, "loadFromPreferences parse error", e)
                false
            }
        } else {
            Log.d(LOG_TAG, "loadFromPreferences key=$key -> no local data")
            false
        }
    }

    /**
     * Firestore'dan ilgili part'ın ders verisini yükler (kullanıcı giriş yapmışsa).
     * LOADED: _lessonItems set edilir.
     * NOT_FOUND: doc/field yok.
     * ERROR: ağ/izin/diğer hata.
     */
    private fun loadFromFirestore(
        context: Context,
        partId: Int,
        uid: String,
        callback: (FirestoreLoadStatus) -> Unit
    ) {
        firestore.collection("users").document(uid).collection(FIRESTORE_LESSON_PROGRESS)
            .document(partId.toString())
            .get()
            .addOnSuccessListener { doc ->
                val json = doc.getString("items")
                Log.d(LOG_TAG, "loadFromFirestore partId=$partId doc.exists=${doc.exists()} itemsFieldNull=${json == null} itemsBlank=${json.isNullOrBlank()}")
                // #region agent log
                debugLog(
                    hypothesisId = "H3",
                    location = "GlobalLessonData.kt:loadFromFirestore:success",
                    message = "cloud lesson fetch result",
                    data = mapOf(
                        "partId" to partId,
                        "docExists" to doc.exists(),
                        "hasItemsField" to !json.isNullOrBlank(),
                    ),
                )
                // #endregion
                if (!json.isNullOrBlank()) {
                    try {
                        _lessonItems = parseLessonItemsWithMigration(json)
                        val completedCount = _lessonItems.count { it.stepIsFinish }
                        val chestCompletedCount = _lessonItems.count { it.type == LessonItem.TYPE_CHEST && it.stepIsFinish }
                        Log.d(LOG_TAG, "loadFromFirestore LOADED ${_lessonItems.size} items from CLOUD")
                        // #region agent log
                        debugLog(
                            hypothesisId = "H3",
                            location = "GlobalLessonData.kt:loadFromFirestore:loadedStats",
                            message = "cloud lesson payload stats",
                            data = mapOf(
                                "partId" to partId,
                                "itemCount" to _lessonItems.size,
                                "completedCount" to completedCount,
                                "chestCompletedCount" to chestCompletedCount,
                            ),
                        )
                        // #endregion
                        callback(FirestoreLoadStatus.LOADED)
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "loadFromFirestore parse error", e)
                        callback(FirestoreLoadStatus.ERROR)
                    }
                } else {
                    Log.d(LOG_TAG, "loadFromFirestore NOT_FOUND (no items field or empty)")
                    callback(FirestoreLoadStatus.NOT_FOUND)
                }
            }
            .addOnFailureListener { e ->
                Log.e(LOG_TAG, "loadFromFirestore FAILED", e)
                // #region agent log
                debugLog(
                    hypothesisId = "H3",
                    location = "GlobalLessonData.kt:loadFromFirestore:failure",
                    message = "cloud lesson fetch failed",
                    data = mapOf(
                        "partId" to partId,
                        "errorType" to e.javaClass.simpleName,
                        "errorMessage" to (e.message ?: "null"),
                    ),
                )
                // #endregion
                callback(FirestoreLoadStatus.ERROR)
            }
    }

    private fun parseLessonItemsWithMigration(rawJson: String): MutableList<LessonItem> {
        val gson = Gson()
        val type = object : TypeToken<List<LessonItem>>() {}.type
        return try {
            gson.fromJson<List<LessonItem>>(rawJson, type).toMutableList()
        } catch (_: Exception) {
            val migratedJson = migrateLegacyRecordField(rawJson)
            gson.fromJson<List<LessonItem>>(migratedJson, type).toMutableList()
        }
    }

    /**
     * Firestore'da bazı eski kullanıcı kayıtlarında chest satırları için `titleUnit` alanı eksik olabiliyor.
     * Parça listesini hesaplamak için (badge piece senkronu) titleUnit değerini local template'ten geri doldur.
     */
    private fun backfillChestTitleUnitFromLocal(partId: Int, cloudItems: MutableList<LessonItem>) {
        val localItems = createLessonItems(partId)
        cloudItems.forEachIndexed { idx, item ->
            if (item.type != LessonItem.TYPE_CHEST) return@forEachIndexed
            val raw = item.titleUnit?.trim()
            val isMissing = raw.isNullOrBlank() || raw.equals("null", ignoreCase = true)
            if (!isMissing) return@forEachIndexed
            val fallback = localItems.getOrNull(idx)
            val fallbackRaw = fallback?.titleUnit?.trim()
            if (!fallbackRaw.isNullOrBlank() && !fallbackRaw.equals("null", ignoreCase = true)) {
                item.titleUnit = fallback.titleUnit
            }
        }
    }

    private fun migrateLegacyRecordField(rawJson: String): String {
        val root = JsonParser.parseString(rawJson)
        if (!root.isJsonArray) return rawJson
        val migratedArray = JsonArray()
        root.asJsonArray.forEach { element ->
            if (!element.isJsonObject) {
                migratedArray.add(element)
                return@forEach
            }
            val itemObj = element.asJsonObject
            migrateRecordValue(itemObj)
            migratedArray.add(itemObj)
        }
        return migratedArray.toString()
    }

    private fun migrateRecordValue(itemObj: JsonObject) {
        val recordElement = itemObj.get("record") ?: return
        if (!recordElement.isJsonPrimitive) return
        val primitive = recordElement.asJsonPrimitive
        if (primitive.isNumber) return
        if (!primitive.isString) return
        val legacyValue = primitive.asString.trim()
        val migratedRecord = legacyValue.toIntOrNull()
        if (migratedRecord != null) {
            itemObj.addProperty("record", migratedRecord)
        } else {
            // Eski "mm:ss" kayıtları puan modeliyle uyumsuz, güvenli fallback.
            itemObj.add("record", JsonNull.INSTANCE)
        }
    }

    private fun waitForAuthUid(auth: FirebaseAuth, timeoutMs: Long, callback: (String?) -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        var completed = false

        fun finish(uid: String?) {
            if (completed) return
            completed = true
            callback(uid)
        }

        val existing = auth.currentUser?.uid
        if (existing != null) {
            finish(existing)
            return
        }

        lateinit var authListener: FirebaseAuth.AuthStateListener
        val timeoutRunnable = Runnable {
            auth.removeAuthStateListener(authListener)
            finish(auth.currentUser?.uid)
        }

        authListener = FirebaseAuth.AuthStateListener { a ->
            val uid = a.currentUser?.uid
            if (uid != null) {
                auth.removeAuthStateListener(authListener)
                handler.removeCallbacks(timeoutRunnable)
                finish(uid)
            }
        }

        auth.addAuthStateListener(authListener)
        handler.postDelayed(timeoutRunnable, timeoutMs)
    }

    /** Sadece şu anki kullanıcının ders verisini temizler (test / sıfırlama için) */
    fun clearCurrentUserLessonData(context: Context) {
        val appCtx = context.applicationContext
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        appCtx.getSharedPreferences("${PREFS_PREFIX}guest", Context.MODE_PRIVATE).edit().clear().apply()
        if (!uid.isNullOrBlank()) {
            appCtx.getSharedPreferences("${PREFS_PREFIX}$uid", Context.MODE_PRIVATE).edit().clear().apply()
        }
        _lessonItems = mutableListOf()
        Log.d(LOG_TAG, "clearCurrentUserLessonData uidPresent=${!uid.isNullOrBlank()} local cache cleared")
    }
}
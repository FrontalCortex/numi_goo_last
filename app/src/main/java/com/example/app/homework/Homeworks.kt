package com.example.app.homework

import android.util.Log
import com.example.app.R
import com.example.app.appString
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/**
 * Ödevlerin okunup yazıldığı yer (öğretmen ve öğrenci).
 *
 * Ödevi yalnızca sunucu yazıyor (createHomework, functions/homework.js); istemci okuyor ve
 * öğrenci kendi ilerlemesini yazıyor. Kurallar (firestore.rules "Ödevler") ilerlemeye yalnızca
 * bir adım ileri yazımı izin veriyor; buradaki sorgular da kuralların kanıtlayabileceği
 * biçimde kurulu (her sorgu sınıf kimliğiyle), yoksa liste hatasız boş kalırdı.
 */
object Homeworks {

    private const val TAG = "Homeworks"

    data class Homework(
        val id: String,
        val classId: String,
        val className: String,
        val teacherName: String,
        val config: HomeworkConfig,
        val blockCount: Int,
        val wholeClass: Boolean,
        val assignedUids: List<String>,
        val createdAtMs: Long,
        /** Ödevi bitirenler; yalnızca sunucu yazıyor (onHomeworkProgressWritten). */
        val doneUids: List<String> = emptyList(),
    ) {
        /**
         * Ödevin hedefi: sınıfa verildiyse sınıfın ŞU ANKİ öğrencileri, seçilerek verildiyse
         * seçilenler. Sunucunun bildirimindeki "8/12" de aynı hesap.
         */
        fun targetUids(memberUids: Collection<String>): List<String> =
            if (wholeClass) memberUids.toList() else assignedUids

        /** Hedefteki bitirenler (sınıftan ayrılanlar sayılmıyor). */
        fun doneCount(memberUids: Collection<String>): Int {
            val target = targetUids(memberUids).toSet()
            return doneUids.toSet().count { it in target }
        }
    }

    data class Progress(
        val uid: String,
        val completedBlocks: Int,
        val answered: Int,
        val correct: Int,
    ) {
        companion object {
            val EMPTY = Progress("", 0, 0, 0)
        }
    }

    private val db get() = FirebaseFirestore.getInstance()

    // ── Okuma ────────────────────────────────────────────────────────────────

    /** Öğretmen: sınıfın ödevleri, en yeni üstte. */
    fun listenClassHomeworks(classId: String, onChange: (List<Homework>?) -> Unit): ListenerRegistration =
        db.collection("homeworks").whereEqualTo("classId", classId)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.w(TAG, "sınıf ödevleri okunamadı", e)
                    onChange(null)
                    return@addSnapshotListener
                }
                onChange(snap?.documents.orEmpty().mapNotNull { parse(it) }.sortedByDescending { it.createdAtMs })
            }

    /**
     * Öğrenci: sınıfına verilen ve kendisine seçilerek verilen ödevler. İki ayrı sorgu, çünkü
     * kurallar ikisini ayrı kanıtlıyor; sonuçlar birleştirilip en yeni üstte veriliyor.
     *
     * @param classId Öğrencinin sınıfı ([com.example.app.CourseClasses.loadMyClass]).
     */
    fun listenMyHomeworks(classId: String, onChange: (List<Homework>) -> Unit): List<ListenerRegistration> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
        var whole: List<Homework> = emptyList()
        var mine: List<Homework> = emptyList()
        fun emit() = onChange((whole + mine).distinctBy { it.id }.sortedByDescending { it.createdAtMs })
        val col = db.collection("homeworks")
        return listOf(
            col.whereEqualTo("classId", classId).whereEqualTo("wholeClass", true)
                .addSnapshotListener { snap, e ->
                    if (e != null) Log.w(TAG, "sınıf ödevleri okunamadı", e)
                    whole = snap?.documents.orEmpty().mapNotNull { parse(it) }
                    emit()
                },
            col.whereEqualTo("classId", classId).whereArrayContains("assignedUids", uid)
                .addSnapshotListener { snap, e ->
                    if (e != null) Log.w(TAG, "seçili ödevler okunamadı", e)
                    mine = snap?.documents.orEmpty().mapNotNull { parse(it) }
                    emit()
                },
        )
    }

    fun loadHomework(id: String, onResult: (Homework?) -> Unit) {
        db.collection("homeworks").document(id).get()
            .addOnSuccessListener { onResult(parse(it)) }
            .addOnFailureListener { e -> Log.w(TAG, "ödev okunamadı", e); onResult(null) }
    }

    /** Öğrencinin kendi ilerlemesi; doküman yoksa [Progress.EMPTY]. */
    fun listenMyProgress(homeworkId: String, onChange: (Progress) -> Unit): ListenerRegistration? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return progressRef(homeworkId, uid).addSnapshotListener { snap, e ->
            if (e != null) Log.w(TAG, "ilerleme okunamadı", e)
            onChange(snap?.let { parseProgress(it) } ?: Progress.EMPTY)
        }
    }

    fun loadMyProgress(homeworkId: String, onResult: (Progress?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(null)
        progressRef(homeworkId, uid).get()
            .addOnSuccessListener { onResult(parseProgress(it)) }
            .addOnFailureListener { e -> Log.w(TAG, "ilerleme okunamadı", e); onResult(null) }
    }

    /** Öğretmen: ödevin bütün öğrencilerinin ilerlemesi (uid → ilerleme). */
    fun listenAllProgress(homeworkId: String, onChange: (Map<String, Progress>?) -> Unit): ListenerRegistration =
        db.collection("homeworks").document(homeworkId).collection("studentProgress")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.w(TAG, "ilerlemeler okunamadı", e)
                    onChange(null)
                    return@addSnapshotListener
                }
                onChange(snap?.documents.orEmpty().associate { it.id to parseProgress(it) })
            }

    // ── Yazma ────────────────────────────────────────────────────────────────

    /**
     * Öğrenci bir adımı bitirdi. Kurallar yalnızca tam bir adım ileri yazımı kabul ettiği için
     * önceki değerlerin üstüne ekleniyor; aynı adım iki kez gönderilirse (çift dokunuş) ikincisi
     * reddediliyor, bu doğru davranış.
     */
    fun submitBlock(
        homeworkId: String,
        previous: Progress,
        blockAnswered: Int,
        blockCorrect: Int,
        /** Bu adımın kural sayımı: anahtar → [doğru, yanlış] (RuleAnalysis). Sunucu öğrencinin toplamına ekliyor. */
        ruleTally: Map<String, IntArray>,
        onResult: (Boolean) -> Unit,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(false)
        val data = mapOf(
            "uid" to uid,
            "completedBlocks" to previous.completedBlocks + 1,
            "answered" to previous.answered + blockAnswered,
            "correct" to previous.correct + blockCorrect,
            "lastBlockCorrect" to blockCorrect,
            "updatedAtMs" to System.currentTimeMillis(),
            "ruleTally" to ruleTally.mapValues { (_, cw) -> mapOf("c" to cw[0], "w" to cw[1]) },
        )
        progressRef(homeworkId, uid).set(data)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { e -> Log.w(TAG, "ilerleme yazılamadı", e); onResult(false) }
    }

    /** @param studentUids null → sınıfın tamamı. */
    fun create(
        classId: String,
        config: HomeworkConfig,
        studentUids: List<String>?,
        onResult: (recipients: Int?, error: String?) -> Unit,
    ) {
        val target: Map<String, Any> = if (studentUids == null) mapOf("wholeClass" to true)
        else mapOf("studentUids" to studentUids)
        val payload = mapOf("classId" to classId, "config" to configToMap(config), "target" to target)
        FirebaseFunctions.getInstance().getHttpsCallable("createHomework").call(payload)
            .addOnSuccessListener { r ->
                onResult(((r.data as? Map<*, *>)?.get("recipients") as? Number)?.toInt() ?: 0, null)
            }
            .addOnFailureListener { e -> Log.w(TAG, "createHomework başarısız", e); onResult(null, errorMessage(e)) }
    }

    fun delete(homeworkId: String, onResult: (error: String?) -> Unit) {
        FirebaseFunctions.getInstance().getHttpsCallable("deleteHomework").call(mapOf("homeworkId" to homeworkId))
            .addOnSuccessListener { onResult(null) }
            .addOnFailureListener { e -> Log.w(TAG, "deleteHomework başarısız", e); onResult(errorMessage(e)) }
    }

    // ── Dönüşüm ──────────────────────────────────────────────────────────────

    private fun progressRef(homeworkId: String, uid: String) =
        db.collection("homeworks").document(homeworkId).collection("studentProgress").document(uid)

    private fun parseProgress(d: DocumentSnapshot): Progress =
        if (!d.exists()) Progress.EMPTY
        else Progress(
            uid = d.id,
            completedBlocks = (d.getLong("completedBlocks") ?: 0L).toInt(),
            answered = (d.getLong("answered") ?: 0L).toInt(),
            correct = (d.getLong("correct") ?: 0L).toInt(),
        )

    /** Tanınmayan bir ödev (eski/bozuk ayar) listede gösterilmiyor; çökmek yerine atlanıyor. */
    internal fun parse(d: DocumentSnapshot): Homework? {
        if (!d.exists()) return null
        val c = d.get("config") as? Map<*, *> ?: return null
        val config = configFromMap(c) ?: return null
        @Suppress("UNCHECKED_CAST")
        return Homework(
            id = d.id,
            classId = d.getString("classId").orEmpty(),
            className = d.getString("className").orEmpty(),
            teacherName = d.getString("teacherName").orEmpty(),
            config = config,
            blockCount = (d.getLong("blockCount") ?: HomeworkEngine.blockCount(config.questionCount).toLong()).toInt(),
            wholeClass = d.getBoolean("wholeClass") == true,
            assignedUids = (d.get("assignedUids") as? List<String>).orEmpty(),
            createdAtMs = d.getLong("createdAtMs") ?: 0L,
            doneUids = (d.get("doneUids") as? List<String>).orEmpty(),
        )
    }

    internal fun configFromMap(c: Map<*, *>): HomeworkConfig? {
        val op = HomeworkOp.fromKey(c["op"] as? String) ?: return null
        fun int(key: String) = (c[key] as? Number)?.toInt() ?: 0
        val rules = (c["rules"] as? List<*>).orEmpty().mapNotNull { AbacusRule.fromKey(it as? String) }.toSet()
        // Gelişmiş rakam seçimi: { "five": [3, 4] }, çarpmada { "0": [2, 5] }. Firestore iç içe
        // dizi tutmadığı için harita; okurken motorun kurallarıyla temizleniyor.
        fun digitMap(key: String): Map<String, Set<Int>> =
            (c[key] as? Map<*, *>).orEmpty().entries.mapNotNull { (k, v) ->
                val digits = (v as? List<*>).orEmpty().mapNotNull { (it as? Number)?.toInt() }.toSet()
                (k as? String)?.let { it to digits }
            }.toMap()
        return HomeworkEngine.normalized(
            HomeworkConfig(
                op = op,
                rules = rules,
                withAbacus = c["withAbacus"] == true,
                digits = int("digits"),
                digits2 = int("digits2"),
                numberCount = int("numberCount"),
                intervalMs = (c["intervalMs"] as? Number)?.toLong() ?: 0L,
                questionCount = int("questionCount"),
                ruleDigits = digitMap("ruleDigits").mapNotNull { (k, v) -> AbacusRule.fromKey(k)?.let { it to v } }.toMap(),
                mulDigits1 = digitMap("mulDigits1").mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap(),
                mulDigits2 = digitMap("mulDigits2").mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap(),
            )
        )
    }

    internal fun configToMap(cfg: HomeworkConfig): Map<String, Any> = if (cfg.op == HomeworkOp.MUL) {
        buildMap {
            put("op", cfg.op.key)
            put("withAbacus", cfg.withAbacus)
            put("digits", cfg.digits)
            put("digits2", cfg.digits2)
            put("questionCount", cfg.questionCount)
            if (cfg.mulDigits1.isNotEmpty()) put("mulDigits1", cfg.mulDigits1.entries.associate { (k, v) -> k.toString() to v.sorted() })
            if (cfg.mulDigits2.isNotEmpty()) put("mulDigits2", cfg.mulDigits2.entries.associate { (k, v) -> k.toString() to v.sorted() })
        }
    } else {
        buildMap {
            put("op", cfg.op.key)
            put("rules", AbacusRule.values().filter { it in cfg.rules }.map { it.key })
            put("withAbacus", cfg.withAbacus)
            put("digits", cfg.digits)
            put("numberCount", cfg.numberCount)
            put("intervalMs", cfg.intervalMs.toInt())
            put("questionCount", cfg.questionCount)
            if (cfg.ruleDigits.isNotEmpty()) put("ruleDigits", cfg.ruleDigits.entries.associate { (k, v) -> k.key to v.sorted() })
        }
    }

    private fun errorMessage(e: Exception): String {
        val fe = e as? FirebaseFunctionsException
        return when (fe?.code) {
            FirebaseFunctionsException.Code.INVALID_ARGUMENT,
            FirebaseFunctionsException.Code.FAILED_PRECONDITION,
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED,
            FirebaseFunctionsException.Code.PERMISSION_DENIED,
            FirebaseFunctionsException.Code.NOT_FOUND -> fe.message ?: appString(R.string.class_generic_error)
            else -> appString(R.string.class_generic_error)
        }
    }
}

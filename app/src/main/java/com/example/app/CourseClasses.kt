package com.example.app

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/**
 * Kurs sınıfları — öğretmen tarafının okuma ve yazma noktası.
 *
 * Model ve kararlar sunucuda (functions/classes.js); bu dosya yalnızca okuyup callable
 * çağırıyor. İstemci sınıf, üye ya da davet dokümanlarına HİÇ yazamıyor (firestore.rules):
 * üyelik iki yerde birden tutuluyor (öğrencinin kullanıcı dokümanı + `members`) ve ikisi
 * yalnızca sunucuda, aynı transaction'da değişiyor.
 *
 * Tek istisna `helpsGeneralPool`: öğretmenin kendi tercihi, kendi dokümanına yazıyor. Alan
 * yalnızca kurs öğretmeninde anlam taşıyor; başkası yazsa bir şey değişmiyor (kurallar
 * `courseId` boşsa genel havuzu zaten açıyor).
 */
object CourseClasses {

    private const val TAG = "CourseClasses"

    const val POOL_GENERAL = "general"

    /** Öğretmenin hangi havuzları gördüğü. Kaynağı functions/classes.js `poolVisibility`. */
    data class PoolVisibility(
        val classPool: Boolean,
        val generalPool: Boolean,
        /** Kurs öğretmeni mi (sınıf açabilir). */
        val isCourseTeacher: Boolean,
        val helpsGeneralPool: Boolean,
    )

    data class CourseClass(
        val id: String,
        val name: String,
        val courseName: String,
        val joinCode: String,
        val studentCount: Int,
        val createdAtMs: Long,
    )

    data class ClassMember(
        val uid: String,
        val name: String,
        val userId: String,
        val joinedAtMs: Long,
    )

    private val db get() = FirebaseFirestore.getInstance()
    private val functions get() = FirebaseFunctions.getInstance()

    /** Öğretmenin kullanıcı dokümanından havuz görünürlüğü. */
    fun poolVisibilityOf(userDoc: DocumentSnapshot): PoolVisibility {
        val courseId = userDoc.getString("courseId")?.takeIf { it.isNotBlank() }
        val helps = userDoc.getBoolean("helpsGeneralPool") == true
        val isCourse = courseId != null && userDoc.getBoolean("teacherApproved") == true
        return PoolVisibility(
            classPool = isCourse,
            generalPool = !isCourse || helps,
            isCourseTeacher = isCourse,
            helpsGeneralPool = helps,
        )
    }

    fun setHelpsGeneralPool(enabled: Boolean, onResult: (Boolean) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(false)
        db.collection("users").document(uid).update("helpsGeneralPool", enabled)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { e -> Log.w(TAG, "helpsGeneralPool yazılamadı", e); onResult(false) }
    }

    // ── Okuma ────────────────────────────────────────────────────────────────

    /** Öğretmenin sınıfları, en yeni üstte. Kapanmış (archived) sınıflar listede yok. */
    fun listenMyClasses(onChange: (List<CourseClass>?) -> Unit): ListenerRegistration? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        // Sıralama istemcide: array-contains + orderBy yeni bir bileşik indeks isterdi;
        // bir öğretmenin en fazla 20 sınıfı var.
        return db.collection("classes").whereArrayContains("teacherUids", uid)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.w(TAG, "sınıflar okunamadı", e)
                    onChange(null)
                    return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty()
                    .filter { it.getBoolean("archived") != true }
                    .map { classOf(it) }
                    .sortedByDescending { it.createdAtMs }
                onChange(list)
            }
    }

    fun listenClass(classId: String, onChange: (CourseClass?) -> Unit): ListenerRegistration =
        db.collection("classes").document(classId).addSnapshotListener { snap, e ->
            if (e != null) Log.w(TAG, "sınıf okunamadı", e)
            onChange(snap?.takeIf { it.exists() }?.let { classOf(it) })
        }

    /** Sınıftaki öğrenciler, ada göre. */
    fun listenMembers(classId: String, onChange: (List<ClassMember>?) -> Unit): ListenerRegistration =
        db.collection("classes").document(classId).collection("members")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.w(TAG, "öğrenciler okunamadı", e)
                    onChange(null)
                    return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty().map { d ->
                    ClassMember(
                        uid = d.id,
                        name = d.getString("name").orEmpty(),
                        userId = d.getString("userId").orEmpty(),
                        joinedAtMs = d.getLong("joinedAtMs") ?: 0L,
                    )
                }.sortedBy { it.name.ifBlank { it.userId }.lowercase(java.util.Locale("tr")) }
                onChange(list)
            }

    /** Bu sınıftan gönderilmiş, henüz yanıtlanmamış davetlerdeki öğrenci adları. */
    fun listenPendingInvites(classId: String, onChange: (List<String>) -> Unit): ListenerRegistration? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        // `teacherUid` filtresi şart: kural daveti yalnızca gönderen öğretmene ve öğrenciye
        // açıyor; filtresiz sorgu tamamen reddedilirdi.
        return db.collection("classInvites")
            .whereEqualTo("teacherUid", uid)
            .whereEqualTo("classId", classId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, e ->
                if (e != null) Log.w(TAG, "davetler okunamadı", e)
                onChange(snap?.documents.orEmpty().map { d ->
                    d.getString("studentName")?.takeIf { it.isNotBlank() } ?: d.getString("studentUserId").orEmpty()
                })
            }
    }

    /** Avatarlar `publicProfiles`'tan; `members` dokümanında tutulmuyor (öğrenci değiştirebilir). */
    fun loadAvatars(uids: List<String>, onResult: (Map<String, String?>) -> Unit) {
        if (uids.isEmpty()) return onResult(emptyMap())
        val out = HashMap<String, String?>()
        var pending = uids.size
        uids.forEach { uid ->
            db.collection("publicProfiles").document(uid).get()
                .addOnCompleteListener { task ->
                    out[uid] = task.result?.getString(AvatarStore.FIRESTORE_FIELD)
                    pending--
                    if (pending == 0) onResult(out)
                }
        }
    }

    private fun classOf(d: DocumentSnapshot) = CourseClass(
        id = d.id,
        name = d.getString("name").orEmpty(),
        courseName = d.getString("courseName").orEmpty(),
        joinCode = d.getString("joinCode").orEmpty(),
        studentCount = (d.getLong("studentCount") ?: 0L).toInt().coerceAtLeast(0),
        createdAtMs = d.getLong("createdAtMs") ?: 0L,
    )

    // ── Yazma (Cloud Functions) ──────────────────────────────────────────────

    fun createClass(name: String, onResult: (joinCode: String?, error: String?) -> Unit) {
        call("createClass", mapOf("name" to name)) { data, error ->
            onResult(data?.get("joinCode") as? String, error)
        }
    }

    fun regenerateCode(classId: String, onResult: (joinCode: String?, error: String?) -> Unit) {
        call("regenerateClassCode", mapOf("classId" to classId)) { data, error ->
            onResult(data?.get("joinCode") as? String, error)
        }
    }

    /** @param onResult (öğrencinin adı, zaten üye mi, hata) */
    fun inviteStudent(
        classId: String,
        userId: String,
        onResult: (studentName: String?, alreadyMember: Boolean, error: String?) -> Unit,
    ) {
        call("inviteStudentToClass", mapOf("classId" to classId, "userId" to userId)) { data, error ->
            onResult(data?.get("studentName") as? String, data?.get("alreadyMember") == true, error)
        }
    }

    fun removeStudent(classId: String, studentUid: String, onResult: (error: String?) -> Unit) {
        call("removeStudentFromClass", mapOf("classId" to classId, "studentUid" to studentUid)) { _, error ->
            onResult(error)
        }
    }

    private fun call(name: String, payload: Map<String, Any>, onResult: (Map<*, *>?, String?) -> Unit) {
        functions.getHttpsCallable(name).call(payload)
            .addOnSuccessListener { result -> onResult(result.data as? Map<*, *> ?: emptyMap<Any, Any>(), null) }
            .addOnFailureListener { e ->
                Log.w(TAG, "$name başarısız", e)
                onResult(null, errorMessage(e))
            }
    }

    /** Sunucu mesajları kullanıcıya gösterilecek biçimde yazıldı; ağ hatasında genel metin. */
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

    /** Kodu okunur yazar: "ABC234" → "ABC 234". Girerken boşluk sorun değil (sunucu atıyor). */
    fun formatCode(code: String): String =
        if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code
}

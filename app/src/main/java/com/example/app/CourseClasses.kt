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

    /** Sınıflarım kartındaki avatar dizisi için ilk [limit] öğrencinin uid'i (bir kez okunuyor). */
    fun loadMemberPreview(classId: String, limit: Long, onResult: (List<String>) -> Unit) {
        db.collection("classes").document(classId).collection("members").limit(limit).get()
            .addOnSuccessListener { snap -> onResult(snap.documents.map { it.id }) }
            .addOnFailureListener { e -> Log.w(TAG, "öğrenci önizlemesi okunamadı", e); onResult(emptyList()) }
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

    /**
     * Öğretmenleri sınıf listesi satırına çevirir (ad ve kullanıcı adı `publicProfiles`'tan;
     * öğretmenin `users` dokümanını öğrenci okuyamıyor). Okunamayan öğretmen atlanıyor.
     */
    fun loadTeachers(uids: List<String>, onResult: (List<ClassMember>) -> Unit) {
        if (uids.isEmpty()) return onResult(emptyList())
        val out = arrayOfNulls<ClassMember>(uids.size)
        var pending = uids.size
        uids.forEachIndexed { i, uid ->
            db.collection("publicProfiles").document(uid).get()
                .addOnCompleteListener { task ->
                    task.result?.takeIf { it.exists() }?.let { d ->
                        out[i] = ClassMember(uid, d.getString("name").orEmpty(), d.getString("userId").orEmpty(), 0L)
                    }
                    pending--
                    if (pending == 0) onResult(out.filterNotNull())
                }
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

    // ── Öğrenci tarafı ───────────────────────────────────────────────────────

    /** Öğrencinin şu anki sınıfı; öğretmen adı sınıfı açandan (`ownerName`). */
    data class MyClass(
        val id: String,
        val name: String,
        val courseName: String,
        val teacherName: String,
        /** Sınıfın öğretmenleri (Sorularım → Sınıfım'da listenin başında). */
        val teacherUids: List<String> = emptyList(),
        val studentCount: Int = 0,
    )

    data class ClassInvite(
        val id: String,
        val className: String,
        val courseName: String,
        val teacherName: String,
    )

    /** Sunucudaki davet süresi (functions/classes.js INVITE_TTL_MS); eskiler listede gösterilmiyor. */
    private const val INVITE_TTL_MS = 14L * 24 * 60 * 60 * 1000

    /**
     * Öğrencinin sınıfı (yoksa null). İki okuma: üyelik kullanıcı dokümanında, ad ve öğretmen
     * sınıf dokümanında. Sınıf kapandıysa (archived) öğrenci sınıfsız sayılıyor — soruları da
     * zaten genel havuza düşüyor (classes.js questionPoolFields).
     */
    fun loadMyClass(onResult: (MyClass?, error: Boolean) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(null, true)
        db.collection("users").document(uid).get()
            .addOnSuccessListener { user ->
                val classId = user.getString("classId")?.takeIf { it.isNotBlank() }
                    ?: return@addOnSuccessListener onResult(null, false)
                db.collection("classes").document(classId).get()
                    .addOnSuccessListener { c ->
                        if (!c.exists() || c.getBoolean("archived") == true) return@addOnSuccessListener onResult(null, false)
                        onResult(
                            MyClass(
                                id = c.id,
                                name = c.getString("name").orEmpty(),
                                courseName = c.getString("courseName").orEmpty(),
                                teacherName = c.getString("ownerName").orEmpty(),
                                teacherUids = (c.get("teacherUids") as? List<*>).orEmpty().filterIsInstance<String>(),
                                studentCount = (c.getLong("studentCount") ?: 0L).toInt().coerceAtLeast(0),
                            ),
                            false,
                        )
                    }
                    .addOnFailureListener { e -> Log.w(TAG, "sınıf okunamadı", e); onResult(null, true) }
            }
            .addOnFailureListener { e -> Log.w(TAG, "kullanıcı okunamadı", e); onResult(null, true) }
    }

    /** Öğrenciye gelmiş, yanıtlanmamış ve süresi dolmamış davetler. */
    fun listenMyInvites(onChange: (List<ClassInvite>) -> Unit): ListenerRegistration? =
        myInvitesQuery()?.addSnapshotListener { snap, e ->
            if (e != null) Log.w(TAG, "davetler okunamadı", e)
            onChange(invitesOf(snap?.documents.orEmpty()))
        }

    /** [listenMyInvites]'in tek seferlik okuması (Ayarlar satırı). Hata olursa boş liste. */
    fun loadMyInvites(onResult: (List<ClassInvite>) -> Unit) {
        val q = myInvitesQuery() ?: return onResult(emptyList())
        q.get()
            .addOnSuccessListener { snap -> onResult(invitesOf(snap.documents)) }
            .addOnFailureListener { e -> Log.w(TAG, "davetler okunamadı", e); onResult(emptyList()) }
    }

    // `studentUid` filtresi şart: kural daveti yalnızca davet edilen öğrenciye ve gönderen
    // öğretmene açıyor, filtresiz sorgu tamamen reddedilirdi.
    private fun myInvitesQuery() = FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
        db.collection("classInvites")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("status", "pending")
    }

    private fun invitesOf(docs: List<DocumentSnapshot>): List<ClassInvite> {
        val now = System.currentTimeMillis()
        return docs
            .filter { now - (it.getLong("createdAtMs") ?: 0L) <= INVITE_TTL_MS }
            .sortedByDescending { it.getLong("createdAtMs") ?: 0L }
            .map { d ->
                ClassInvite(
                    id = d.id,
                    className = d.getString("className").orEmpty(),
                    courseName = d.getString("courseName").orEmpty(),
                    teacherName = d.getString("teacherName").orEmpty(),
                )
            }
    }

    fun joinByCode(code: String, onResult: (className: String?, error: String?) -> Unit) {
        call("joinClassByCode", mapOf("code" to code)) { data, error ->
            onResult(data?.get("className") as? String, error)
        }
    }

    fun respondInvite(inviteId: String, accept: Boolean, onResult: (error: String?) -> Unit) {
        call("respondClassInvite", mapOf("inviteId" to inviteId, "accept" to accept)) { _, error -> onResult(error) }
    }

    fun leaveMyClass(onResult: (error: String?) -> Unit) {
        call("leaveMyClass", emptyMap()) { _, error -> onResult(error) }
    }

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
            FirebaseFunctionsException.Code.NOT_FOUND,
            // Süresi dolan davet (respondClassInvite). İstemci zaman aşımı da bu kodla gelebilir;
            // o zaman SDK'nın mesajı "DEADLINE_EXCEEDED" oluyor, kullanıcıya genel metin gidiyor.
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                fe.message?.takeUnless { it.equals("DEADLINE_EXCEEDED", ignoreCase = true) }
                    ?: appString(R.string.class_generic_error)
            else -> appString(R.string.class_generic_error)
        }
    }

    /** Kodu okunur yazar: "ABC234" → "ABC 234". Girerken boşluk sorun değil (sunucu atıyor). */
    fun formatCode(code: String): String =
        if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code
}

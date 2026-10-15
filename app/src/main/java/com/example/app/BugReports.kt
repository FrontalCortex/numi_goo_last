package com.example.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.File
import java.io.FileOutputStream

/**
 * Hata bildirimi: kullanıcı ekran görüntüsüyle hata bildirir, admin onaylarsa ödülü
 * Görevler'deki karttan toplar.
 *
 * Yazmaların hepsi Cloud Functions'tan geçiyor (`submitBugReport`, `reviewBugReport`,
 * `claimBugReportReward`; karar mantığı functions/bugReports.js). İstemci yalnızca görseli
 * yükler ve `bugReports` koleksiyonunu okur — kendi bildirimlerini, admin ise bekleyenleri.
 *
 * Admin yetkisi ID token'daki `admin` custom claim'i (functions/scripts/set-admin-claim.js).
 * Buradaki [checkAdmin] yalnızca paneli gösterip gizlemek için; asıl yetki kontrolü
 * kurallarda ve `reviewBugReport`'ta.
 */
object BugReports {

    private const val TAG = "BugReports"
    private const val COLLECTION = "bugReports"

    /** Görselin uzun kenarı en fazla bu kadar piksel; okunaklı ama hafif. */
    private const val MAX_IMAGE_EDGE_PX = 1600
    private const val JPEG_QUALITY = 85

    /** Sunucudaki sınırlarla aynı (functions/bugReports.js). */
    const val TEXT_MIN = 10
    const val TEXT_MAX = 1000

    /** Ödül kademeleri — sunucudaki BUG_REPORT_TIERS ile aynı olmalı (yalnızca gösterim). */
    enum class Tier(val id: String, val gold: Int, val keys: Int, val credits: Int) {
        BIG("big", 10_000, 200, 3),
        SMALL("small", 1_000, 20, 0),
    }

    /** Ret gerekçeleri — sunucudaki BUG_REPORT_REJECT_REASONS ile aynı. */
    enum class RejectReason(val id: String) {
        NOT_A_BUG("not_a_bug"),
        DUPLICATE("duplicate"),
        UNCLEAR("unclear"),
        OTHER("other");

        val label: String
            get() = appString(
                when (this) {
                    NOT_A_BUG -> R.string.bug_report_reject_not_a_bug
                    DUPLICATE -> R.string.bug_report_reject_duplicate
                    UNCLEAR -> R.string.bug_report_reject_unclear
                    OTHER -> R.string.bug_report_reject_other
                }
            )

        companion object {
            fun from(id: String?): RejectReason? = entries.firstOrNull { it.id == id }
        }
    }

    data class Report(
        val id: String,
        val uid: String,
        val text: String,
        val imagePath: String,
        val status: String,
        val createdAtMs: Long,
        val rewardGold: Int,
        val rewardKeys: Int,
        val rewardCredits: Int,
        val rewardClaimed: Boolean,
        val rejectReason: RejectReason?,
        val reviewNote: String,
        val device: Map<String, String>,
    ) {
        val isPending: Boolean get() = status == STATUS_PENDING
        val isApproved: Boolean get() = status == STATUS_APPROVED
        val isRejected: Boolean get() = status == STATUS_REJECTED
        val isClaimable: Boolean get() = isApproved && !rewardClaimed

        /** "3 öğretmene danış + 10.000 altın + 200 anahtar" gibi. */
        val rewardText: String get() = rewardText(rewardCredits, rewardGold, rewardKeys)

        /** Admin için tek satır: sürüm, cihaz, Android, dil. */
        val deviceLine: String
            get() = listOfNotNull(
                device["appVersion"]?.let { v -> "v$v" + (device["versionCode"]?.let { " ($it)" } ?: "") },
                listOfNotNull(device["manufacturer"], device["model"]).joinToString(" ").ifBlank { null },
                device["sdk"]?.let { "SDK $it" },
                device["locale"],
            ).joinToString(" · ")
    }

    const val STATUS_PENDING = "pending"
    const val STATUS_APPROVED = "approved"
    const val STATUS_REJECTED = "rejected"

    fun rewardText(credits: Int, gold: Int, keys: Int): String = listOfNotNull(
        if (credits > 0) appString(R.string.bug_report_reward_credits, credits) else null,
        if (gold > 0) appString(R.string.ui_x_altin, formatAmount(gold)) else null,
        if (keys > 0) appString(R.string.bug_report_reward_keys, keys) else null,
    ).joinToString(" + ")

    private fun formatAmount(value: Int): String =
        java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("tr-TR")).format(value)

    private fun parse(doc: com.google.firebase.firestore.DocumentSnapshot): Report? {
        val uid = doc.getString("uid") ?: return null
        val reward = doc.get("reward") as? Map<*, *>
        val device = (doc.get("device") as? Map<*, *>)
            ?.mapNotNull { (k, v) -> (k as? String)?.let { key -> v?.toString()?.let { key to it } } }
            ?.toMap()
            .orEmpty()
        return Report(
            id = doc.id,
            uid = uid,
            text = doc.getString("text").orEmpty(),
            imagePath = doc.getString("imagePath").orEmpty(),
            status = doc.getString("status") ?: STATUS_PENDING,
            createdAtMs = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
            rewardGold = (reward?.get("gold") as? Number)?.toInt() ?: 0,
            rewardKeys = (reward?.get("keys") as? Number)?.toInt() ?: 0,
            rewardCredits = (reward?.get("credits") as? Number)?.toInt() ?: 0,
            rewardClaimed = doc.getBoolean("rewardClaimed") == true,
            rejectReason = RejectReason.from(doc.getString("rejectReason")),
            reviewNote = doc.getString("reviewNote").orEmpty(),
            device = device,
        )
    }

    // ── Okuma ────────────────────────────────────────────────────────────────

    /**
     * Kullanıcının kendi bildirimleri, yeniden eskiye. Sıralama istemcide: sorguya `orderBy`
     * eklemek bileşik dizin isterdi, kişi başı birkaç bildirim için gereksiz.
     */
    fun listenMine(onChange: (List<Report>) -> Unit): ListenerRegistration? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return FirebaseFirestore.getInstance().collection(COLLECTION)
            .whereEqualTo("uid", uid)
            .limit(50)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.w(TAG, "listenMine başarısız", e)
                    return@addSnapshotListener
                }
                onChange(snap?.documents.orEmpty().mapNotNull(::parse).sortedByDescending { it.createdAtMs })
            }
    }

    /** Admin: incelenmeyi bekleyenler, eskiden yeniye (ilk bildiren önce görünsün). */
    fun listenPending(onChange: (List<Report>) -> Unit, onError: (Exception) -> Unit): ListenerRegistration =
        FirebaseFirestore.getInstance().collection(COLLECTION)
            .whereEqualTo("status", STATUS_PENDING)
            .limit(200)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.w(TAG, "listenPending başarısız", e)
                    onError(e)
                    return@addSnapshotListener
                }
                onChange(snap?.documents.orEmpty().mapNotNull(::parse).sortedBy { it.createdAtMs })
            }

    /**
     * Admin mi? Önbellekteki token'a bakar; [forceRefresh] claim yeni verildiyse token'ı
     * yeniler (aksi halde en geç bir saat sonra kendiliğinden yenilenir).
     */
    fun checkAdmin(forceRefresh: Boolean = false, onResult: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false)
        user.getIdToken(forceRefresh)
            .addOnSuccessListener { onResult(it.claims["admin"] == true) }
            .addOnFailureListener { onResult(false) }
    }

    fun imageUrl(imagePath: String, onResult: (Uri?) -> Unit) {
        if (imagePath.isBlank()) return onResult(null)
        FirebaseStorage.getInstance().reference.child(imagePath).downloadUrl
            .addOnSuccessListener { onResult(it) }
            .addOnFailureListener {
                Log.w(TAG, "Görsel adresi alınamadı: $imagePath", it)
                onResult(null)
            }
    }

    // ── Görsel hazırlama ─────────────────────────────────────────────────────

    /**
     * Seçilen görseli küçültüp önbelleğe JPEG olarak yazar. Dosya hem çizim ekranında
     * (ImageAnnotateDialogFragment aynı dosyanın üzerine yazar) hem yüklemede kullanılıyor.
     * Ağ ve dosya işi olduğu için arka planda çağrılmalı.
     */
    fun prepareImage(context: Context, uri: Uri): File? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_IMAGE_EDGE_PX) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val longEdge = maxOf(decoded.width, decoded.height)
        val bitmap = if (longEdge > MAX_IMAGE_EDGE_PX) {
            val scale = MAX_IMAGE_EDGE_PX.toFloat() / longEdge
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
                .also { if (it !== decoded) decoded.recycle() }
        } else {
            decoded
        }

        val dir = File(context.cacheDir, "bug_report").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // önceki seçimden kalanlar
        val file = File(dir, "bug_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        file
    }.onFailure { Log.w(TAG, "Görsel hazırlanamadı", it) }.getOrNull()

    // ── Yazma (Cloud Functions) ──────────────────────────────────────────────

    /**
     * Bildirimi gönderir; [image] varsa önce onu yükler (görsel isteğe bağlı). Gönderim
     * başarısız olursa yüklenen görsel silinir.
     */
    fun submit(context: Context, image: File?, text: String, onResult: (success: Boolean, message: String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return onResult(false, appString(R.string.ui_oturum_acik_degil))

        fun callSubmit(path: String, onFailedCleanup: () -> Unit) {
            FirebaseFunctions.getInstance()
                .getHttpsCallable("submitBugReport")
                .call(mapOf("text" to text, "imagePath" to path, "device" to deviceInfo(context)))
                .addOnSuccessListener {
                    image?.delete()
                    onResult(true, null)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "submitBugReport başarısız", e)
                    onFailedCleanup()
                    onResult(false, errorMessage(e))
                }
        }

        if (image == null) {
            callSubmit("") {}
            return
        }
        val path = "bug_reports/${uid}_${System.currentTimeMillis()}.jpg"
        val ref = FirebaseStorage.getInstance().reference.child(path)
        val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
        ref.putFile(Uri.fromFile(image), metadata)
            .addOnSuccessListener { callSubmit(path) { ref.delete() } }
            .addOnFailureListener { e ->
                Log.w(TAG, "Görsel yüklenemedi", e)
                onResult(false, appString(R.string.bug_report_upload_failed))
            }
    }

    /** Onaylanan bildirimin ödülünü toplar; başarıda kazanılanın metnini döndürür. */
    fun claimReward(reportId: String, onResult: (success: Boolean, message: String) -> Unit) {
        FirebaseFunctions.getInstance()
            .getHttpsCallable("claimBugReportReward")
            .call(mapOf("reportId" to reportId))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val gold = (data?.get("rewardGold") as? Number)?.toInt() ?: 0
                val keys = (data?.get("rewardKeys") as? Number)?.toInt() ?: 0
                val credits = (data?.get("rewardCredits") as? Number)?.toInt() ?: 0
                onResult(true, appString(R.string.ui_x_kazandin, rewardText(credits, gold, keys)))
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "claimBugReportReward başarısız", e)
                onResult(false, errorMessage(e))
            }
    }

    /** Admin kararı. [tier] onayda, [reason] retteyken dolu. */
    fun review(
        reportId: String,
        tier: Tier?,
        reason: RejectReason?,
        note: String,
        onResult: (success: Boolean, message: String?) -> Unit,
    ) {
        val payload = mutableMapOf<String, Any>("reportId" to reportId, "note" to note)
        if (tier != null) {
            payload["decision"] = "approve"
            payload["tier"] = tier.id
        } else {
            payload["decision"] = "reject"
            payload["rejectReason"] = reason?.id ?: RejectReason.OTHER.id
        }
        FirebaseFunctions.getInstance()
            .getHttpsCallable("reviewBugReport")
            .call(payload)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e ->
                Log.w(TAG, "reviewBugReport başarısız", e)
                onResult(false, errorMessage(e))
            }
    }

    private fun deviceInfo(context: Context): Map<String, String> {
        val pInfo = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        @Suppress("DEPRECATION")
        val versionCode = when {
            pInfo == null -> null
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> pInfo.longVersionCode.toString()
            else -> pInfo.versionCode.toString()
        }
        return buildMap {
            pInfo?.versionName?.let { put("appVersion", it) }
            versionCode?.let { put("versionCode", it) }
            put("manufacturer", Build.MANUFACTURER.orEmpty())
            put("model", Build.MODEL.orEmpty())
            put("sdk", Build.VERSION.SDK_INT.toString())
            put("locale", java.util.Locale.getDefault().toLanguageTag())
        }
    }

    /** Sunucu mesajları kullanıcıya gösterilecek biçimde yazıldı; ağ hatasında genel metin. */
    private fun errorMessage(e: Exception): String {
        val fe = e as? FirebaseFunctionsException
        return when (fe?.code) {
            FirebaseFunctionsException.Code.INVALID_ARGUMENT,
            FirebaseFunctionsException.Code.FAILED_PRECONDITION,
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED,
            FirebaseFunctionsException.Code.ALREADY_EXISTS,
            FirebaseFunctionsException.Code.PERMISSION_DENIED,
            FirebaseFunctionsException.Code.NOT_FOUND -> fe.message ?: appString(R.string.bug_report_generic_error)
            else -> appString(R.string.bug_report_generic_error)
        }
    }
}

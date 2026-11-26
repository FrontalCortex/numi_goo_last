// Kaynak (Android):
//  - app/src/main/java/com/example/app/model/StudentQuestion.kt (StudentQuestion, QuestionMessage)
//  - app/src/main/java/com/example/app/GlobalValues.kt (PendingUploadMeta)
//  - app/src/main/java/com/example/app/NotificationFragment.kt (koleksiyon yolları, okunmamış kuralı)
//
// Soru-mesaj modelleri. Firestore açıklamaları (@DocumentId, @ServerTimestamp) ve Timestamp tipi
// ayıklandı; alan adları sabit olarak korundu ve Map ↔ model eşlemesi yazıldı.
//
// Zaman damgası sözleşmesi: Map'lerde zaman alanları EPOCH MİLİSANİYE (Long) olarak taşınır.
// Platform katmanı okurken Firestore Timestamp → ms, yazarken ms → Timestamp dönüşümünü yapar.
//
// Android'den fark: Firestore'un `toObject`'i alan tipi uyuşmazsa istisna atar; buradaki
// `fromMap` uyuşmayan/eksik alanı varsayılan değere düşürür.
package com.sorobit.shared.model

/** Map'ten epoch-ms okur (platform Timestamp'i Long'a çevirmiş olmalı). */
internal fun Any?.asEpochMs(): Long? = (this as? Number)?.toLong()

internal fun Any?.asStringList(): List<String>? = (this as? List<*>)?.mapNotNull { it as? String }

data class StudentQuestion(
    /** Doküman kimliği (@DocumentId) — alan olarak yazılmaz. */
    val id: String = "",
    val studentUid: String = "",
    val studentEmail: String? = null,
    val screenshotStoragePath: String = "",
    val screenshotUrl: String = "",
    val mediaType: String = MEDIA_TYPE_IMAGE,
    val videoStoragePath: String? = null,
    val videoUrl: String? = null,
    val videoDurationSec: Int? = null,
    val message: String = "",
    val previewText: String = "",
    val lastMessageAt: Long? = null,
    /** @ServerTimestamp: null yazılırsa sunucu zamanı atanır. */
    val createdAt: Long? = null,
    val status: String = STATUS_PENDING,
    val claimedByTeacherUid: String? = null,
    val claimedAt: Long? = null,
    val resolvedAt: Long? = null,
    val deletedForUids: List<String>? = null
) {
    /** Yazma Map'i (kimlik hariç). `createdAt` null ise platform `serverTimestamp()` koymalıdır. */
    fun toMap(): Map<String, Any?> = mapOf(
        FIELD_STUDENT_UID to studentUid,
        FIELD_STUDENT_EMAIL to studentEmail,
        FIELD_SCREENSHOT_STORAGE_PATH to screenshotStoragePath,
        FIELD_SCREENSHOT_URL to screenshotUrl,
        FIELD_MEDIA_TYPE to mediaType,
        FIELD_VIDEO_STORAGE_PATH to videoStoragePath,
        FIELD_VIDEO_URL to videoUrl,
        FIELD_VIDEO_DURATION_SEC to videoDurationSec,
        FIELD_MESSAGE to message,
        FIELD_PREVIEW_TEXT to previewText,
        FIELD_LAST_MESSAGE_AT to lastMessageAt,
        FIELD_CREATED_AT to createdAt,
        FIELD_STATUS to status,
        FIELD_CLAIMED_BY_TEACHER_UID to claimedByTeacherUid,
        FIELD_CLAIMED_AT to claimedAt,
        FIELD_RESOLVED_AT to resolvedAt,
        FIELD_DELETED_FOR_UIDS to deletedForUids,
    )

    companion object {
        const val COLLECTION = "questions"
        const val SUBCOLLECTION_MESSAGES = "messages"

        const val STATUS_PENDING = "pending"
        const val STATUS_CLAIMED = "claimed"
        const val STATUS_RESOLVED = "resolved"
        const val MEDIA_TYPE_IMAGE = "image"
        const val MEDIA_TYPE_VIDEO = "video"

        const val FIELD_STUDENT_UID = "studentUid"
        const val FIELD_STUDENT_EMAIL = "studentEmail"
        const val FIELD_SCREENSHOT_STORAGE_PATH = "screenshotStoragePath"
        const val FIELD_SCREENSHOT_URL = "screenshotUrl"
        const val FIELD_MEDIA_TYPE = "mediaType"
        const val FIELD_VIDEO_STORAGE_PATH = "videoStoragePath"
        const val FIELD_VIDEO_URL = "videoUrl"
        const val FIELD_VIDEO_DURATION_SEC = "videoDurationSec"
        const val FIELD_MESSAGE = "message"
        const val FIELD_PREVIEW_TEXT = "previewText"
        const val FIELD_LAST_MESSAGE_AT = "lastMessageAt"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_STATUS = "status"
        const val FIELD_CLAIMED_BY_TEACHER_UID = "claimedByTeacherUid"
        const val FIELD_CLAIMED_AT = "claimedAt"
        const val FIELD_RESOLVED_AT = "resolvedAt"
        const val FIELD_DELETED_FOR_UIDS = "deletedForUids"

        /** `questions/{questionId}` */
        fun docPath(questionId: String): String = "$COLLECTION/$questionId"

        /** `questions/{questionId}/messages` */
        fun messagesPath(questionId: String): String = "$COLLECTION/$questionId/$SUBCOLLECTION_MESSAGES"

        fun fromMap(id: String, data: Map<String, Any?>): StudentQuestion {
            val d = StudentQuestion()
            return StudentQuestion(
                id = id,
                studentUid = data[FIELD_STUDENT_UID] as? String ?: d.studentUid,
                studentEmail = data[FIELD_STUDENT_EMAIL] as? String,
                screenshotStoragePath = data[FIELD_SCREENSHOT_STORAGE_PATH] as? String ?: d.screenshotStoragePath,
                screenshotUrl = data[FIELD_SCREENSHOT_URL] as? String ?: d.screenshotUrl,
                mediaType = data[FIELD_MEDIA_TYPE] as? String ?: d.mediaType,
                videoStoragePath = data[FIELD_VIDEO_STORAGE_PATH] as? String,
                videoUrl = data[FIELD_VIDEO_URL] as? String,
                videoDurationSec = (data[FIELD_VIDEO_DURATION_SEC] as? Number)?.toInt(),
                message = data[FIELD_MESSAGE] as? String ?: d.message,
                previewText = data[FIELD_PREVIEW_TEXT] as? String ?: d.previewText,
                lastMessageAt = data[FIELD_LAST_MESSAGE_AT].asEpochMs(),
                createdAt = data[FIELD_CREATED_AT].asEpochMs(),
                status = data[FIELD_STATUS] as? String ?: d.status,
                claimedByTeacherUid = data[FIELD_CLAIMED_BY_TEACHER_UID] as? String,
                claimedAt = data[FIELD_CLAIMED_AT].asEpochMs(),
                resolvedAt = data[FIELD_RESOLVED_AT].asEpochMs(),
                deletedForUids = data[FIELD_DELETED_FOR_UIDS].asStringList(),
            )
        }
    }
}

data class QuestionMessage(
    /** Android'de @DocumentId YOK: `id` gerçek bir doküman alanıdır. */
    val id: String = "",
    val senderUid: String = "",
    val senderRole: String = "",
    val type: String = TYPE_TEXT,
    val textContent: String? = null,
    val mediaStoragePath: String? = null,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val mediaSizeBytes: Long? = null,
    val clientId: String? = null,
    /** @ServerTimestamp: null yazılırsa sunucu zamanı atanır. */
    val createdAt: Long? = null,
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val deletedForUids: List<String>? = null
) {
    /** Henüz sunucuya ulaşmamış (yerel) mesaj. */
    fun isPending() = id.startsWith(PENDING_ID_PREFIX)

    /**
     * Android: NotificationFragment'teki okunmamış sayımı —
     * `senderUid != uid && readAt == null`.
     */
    fun isUnreadFor(uid: String): Boolean = senderUid != uid && readAt == null

    fun toMap(): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_SENDER_UID to senderUid,
        FIELD_SENDER_ROLE to senderRole,
        FIELD_TYPE to type,
        FIELD_TEXT_CONTENT to textContent,
        FIELD_MEDIA_STORAGE_PATH to mediaStoragePath,
        FIELD_MEDIA_URL to mediaUrl,
        FIELD_THUMBNAIL_URL to thumbnailUrl,
        FIELD_MEDIA_SIZE_BYTES to mediaSizeBytes,
        FIELD_CLIENT_ID to clientId,
        FIELD_CREATED_AT to createdAt,
        FIELD_DELIVERED_AT to deliveredAt,
        FIELD_READ_AT to readAt,
        FIELD_DELETED_FOR_UIDS to deletedForUids,
    )

    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_AUDIO = "audio"
        const val TYPE_VIDEO = "video"
        const val TYPE_IMAGE = "image"

        const val PENDING_ID_PREFIX = "pending_"

        /** Okunmamış sayımında taranan en çok mesaj (NotificationFragment). */
        const val UNREAD_SCAN_LIMIT = 100L

        const val FIELD_ID = "id"
        const val FIELD_SENDER_UID = "senderUid"
        const val FIELD_SENDER_ROLE = "senderRole"
        const val FIELD_TYPE = "type"
        const val FIELD_TEXT_CONTENT = "textContent"
        const val FIELD_MEDIA_STORAGE_PATH = "mediaStoragePath"
        const val FIELD_MEDIA_URL = "mediaUrl"
        const val FIELD_THUMBNAIL_URL = "thumbnailUrl"
        const val FIELD_MEDIA_SIZE_BYTES = "mediaSizeBytes"
        const val FIELD_CLIENT_ID = "clientId"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_DELIVERED_AT = "deliveredAt"
        const val FIELD_READ_AT = "readAt"
        const val FIELD_DELETED_FOR_UIDS = "deletedForUids"

        fun fromMap(data: Map<String, Any?>): QuestionMessage {
            val d = QuestionMessage()
            return QuestionMessage(
                id = data[FIELD_ID] as? String ?: d.id,
                senderUid = data[FIELD_SENDER_UID] as? String ?: d.senderUid,
                senderRole = data[FIELD_SENDER_ROLE] as? String ?: d.senderRole,
                type = data[FIELD_TYPE] as? String ?: d.type,
                textContent = data[FIELD_TEXT_CONTENT] as? String,
                mediaStoragePath = data[FIELD_MEDIA_STORAGE_PATH] as? String,
                mediaUrl = data[FIELD_MEDIA_URL] as? String,
                thumbnailUrl = data[FIELD_THUMBNAIL_URL] as? String,
                mediaSizeBytes = (data[FIELD_MEDIA_SIZE_BYTES] as? Number)?.toLong(),
                clientId = data[FIELD_CLIENT_ID] as? String,
                createdAt = data[FIELD_CREATED_AT].asEpochMs(),
                deliveredAt = data[FIELD_DELIVERED_AT].asEpochMs(),
                readAt = data[FIELD_READ_AT].asEpochMs(),
                deletedForUids = data[FIELD_DELETED_FOR_UIDS].asStringList(),
            )
        }
    }
}

/** Bekleyen (pending) bir yüklemeyi yeniden deneyebilmek için tutulan bilgiler. */
data class PendingUploadMeta(
    val questionId: String,
    val type: String,
    val filePath: String?,
    val textContent: String?,
    val caption: String?
)

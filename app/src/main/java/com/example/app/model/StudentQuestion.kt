package com.example.app.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class StudentQuestion(
    @DocumentId val id: String = "",
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
    val lastMessageAt: Timestamp? = null,   // ← BU SATIR EKLİ OLMALI
    @ServerTimestamp val createdAt: Timestamp? = null,
    val status: String = STATUS_PENDING,
    val claimedByTeacherUid: String? = null,
    val claimedAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null,
    val deletedForUids: List<String>? = null
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_CLAIMED = "claimed"
        const val STATUS_RESOLVED = "resolved"
        const val MEDIA_TYPE_IMAGE = "image"
        const val MEDIA_TYPE_VIDEO = "video"
    }
}

data class QuestionMessage(
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    val deliveredAt: Timestamp? = null,
    val readAt: Timestamp? = null,
    val deletedForUids: List<String>? = null
) {
    fun isPending() = id.startsWith("pending_")
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_AUDIO = "audio"
        const val TYPE_VIDEO = "video"
        const val TYPE_IMAGE = "image"
    }
}



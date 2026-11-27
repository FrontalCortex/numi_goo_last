package com.example.app.model

import com.example.app.appString
import com.example.app.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class MessageReport(
    @DocumentId val id: String = "",
    val questionId: String = "",
    val messageId: String = "",
    val reportedByUid: String = "",
    val reportedUserUid: String = "",
    val messagePreview: String = "",
    val reason: String = "",
    val type: String = QuestionMessage.TYPE_TEXT,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val reportedAt: Timestamp? = null,
    val status: String = STATUS_PENDING,
    val handledByUid: String? = null,
    val handledAt: Timestamp? = null
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_DISMISSED = "dismissed"
        const val STATUS_ACTION_TAKEN = "action_taken"

        const val REASON_HARASSMENT = "harassment"
        const val REASON_SPAM = "spam"
        const val REASON_INAPPROPRIATE = "inappropriate"
        const val REASON_OTHER = "other"

        fun reasonLabel(reason: String): String = when (reason) {
            REASON_HARASSMENT -> appString(R.string.ui_taciz_veya_zorbalik)
            REASON_SPAM -> "Spam"
            REASON_INAPPROPRIATE -> appString(R.string.ui_uygunsuz_icerik)
            REASON_OTHER -> appString(R.string.ui_diger)
            else -> "Belirtilmedi"
        }
    }
}

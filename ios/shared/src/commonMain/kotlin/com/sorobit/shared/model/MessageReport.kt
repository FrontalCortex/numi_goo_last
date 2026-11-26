// Kaynak (Android):
//  - app/src/main/java/com/example/app/model/MessageReport.kt
//  - app/src/main/java/com/example/app/model/RestrictedUser.kt
//  - app/src/main/java/com/example/app/UserAskQuestionRestriction.kt
//  - app/src/main/java/com/example/app/NotificationFragment.kt
//      (dismissReport / restrictReportedUser'ın yazdığı alanlar ve kısıtlama süresi hesabı)
//
// Mesaj şikâyeti ve kullanıcı kısıtlama modelleri. Zaman alanları epoch ms (Long) taşınır
// (bkz. StudentQuestion.kt başındaki sözleşme).
package com.sorobit.shared.model

data class MessageReport(
    /** Doküman kimliği (@DocumentId). */
    val id: String = "",
    val questionId: String = "",
    val messageId: String = "",
    val reportedByUid: String = "",
    val reportedUserUid: String = "",
    val messagePreview: String = "",
    val reason: String = "",
    val type: String = QuestionMessage.TYPE_TEXT,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val reportedAt: Long? = null,
    val status: String = STATUS_PENDING,
    val handledByUid: String? = null,
    val handledAt: Long? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        FIELD_QUESTION_ID to questionId,
        FIELD_MESSAGE_ID to messageId,
        FIELD_REPORTED_BY_UID to reportedByUid,
        FIELD_REPORTED_USER_UID to reportedUserUid,
        FIELD_MESSAGE_PREVIEW to messagePreview,
        FIELD_REASON to reason,
        FIELD_TYPE to type,
        FIELD_MEDIA_URL to mediaUrl,
        FIELD_THUMBNAIL_URL to thumbnailUrl,
        FIELD_REPORTED_AT to reportedAt,
        FIELD_STATUS to status,
        FIELD_HANDLED_BY_UID to handledByUid,
        FIELD_HANDLED_AT to handledAt,
    )

    companion object {
        const val COLLECTION = "messageReports"

        const val STATUS_PENDING = "pending"
        const val STATUS_DISMISSED = "dismissed"
        const val STATUS_ACTION_TAKEN = "action_taken"

        const val REASON_HARASSMENT = "harassment"
        const val REASON_SPAM = "spam"
        const val REASON_INAPPROPRIATE = "inappropriate"
        const val REASON_OTHER = "other"

        const val FIELD_QUESTION_ID = "questionId"
        const val FIELD_MESSAGE_ID = "messageId"
        const val FIELD_REPORTED_BY_UID = "reportedByUid"
        const val FIELD_REPORTED_USER_UID = "reportedUserUid"
        const val FIELD_MESSAGE_PREVIEW = "messagePreview"
        const val FIELD_REASON = "reason"
        const val FIELD_TYPE = "type"
        const val FIELD_MEDIA_URL = "mediaUrl"
        const val FIELD_THUMBNAIL_URL = "thumbnailUrl"
        const val FIELD_REPORTED_AT = "reportedAt"
        const val FIELD_STATUS = "status"
        const val FIELD_HANDLED_BY_UID = "handledByUid"
        const val FIELD_HANDLED_AT = "handledAt"

        fun reasonLabel(reason: String): String = when (reason) {
            REASON_HARASSMENT -> "Taciz veya zorbalık"
            REASON_SPAM -> "Spam"
            REASON_INAPPROPRIATE -> "Uygunsuz içerik"
            REASON_OTHER -> "Diğer"
            else -> "Belirtilmedi"
        }

        fun fromMap(id: String, data: Map<String, Any?>): MessageReport {
            val d = MessageReport()
            return MessageReport(
                id = id,
                questionId = data[FIELD_QUESTION_ID] as? String ?: d.questionId,
                messageId = data[FIELD_MESSAGE_ID] as? String ?: d.messageId,
                reportedByUid = data[FIELD_REPORTED_BY_UID] as? String ?: d.reportedByUid,
                reportedUserUid = data[FIELD_REPORTED_USER_UID] as? String ?: d.reportedUserUid,
                messagePreview = data[FIELD_MESSAGE_PREVIEW] as? String ?: d.messagePreview,
                reason = data[FIELD_REASON] as? String ?: d.reason,
                type = data[FIELD_TYPE] as? String ?: d.type,
                mediaUrl = data[FIELD_MEDIA_URL] as? String,
                thumbnailUrl = data[FIELD_THUMBNAIL_URL] as? String,
                reportedAt = data[FIELD_REPORTED_AT].asEpochMs(),
                status = data[FIELD_STATUS] as? String ?: d.status,
                handledByUid = data[FIELD_HANDLED_BY_UID] as? String,
                handledAt = data[FIELD_HANDLED_AT].asEpochMs(),
            )
        }

        /** Android: dismissReport'un yazdığı güncelleme (`handledAt` = şimdi). */
        fun dismissUpdate(myUid: String, nowMs: Long): Map<String, Any?> = mapOf(
            FIELD_STATUS to STATUS_DISMISSED,
            FIELD_HANDLED_BY_UID to myUid,
            FIELD_HANDLED_AT to nowMs,
        )

        /** Android: restrictReportedUser'ın şikâyet dokümanına yazdığı güncelleme. */
        fun actionTakenUpdate(myUid: String, nowMs: Long): Map<String, Any?> = mapOf(
            FIELD_STATUS to STATUS_ACTION_TAKEN,
            FIELD_HANDLED_BY_UID to myUid,
            FIELD_HANDLED_AT to nowMs,
        )
    }
}

/** `users/{uid}` dokümanının kısıtlama görünümü ("Kısıtlı Kullanıcılar" listesi). */
data class RestrictedUser(
    /** Doküman kimliği (@DocumentId). */
    val uid: String = "",
    val name: String = "",
    val banned: Boolean = false,
    val restrictedUntil: Long? = null,
    val restrictionReason: String? = null,
    val restrictionMessagePreview: String? = null,
    val restrictionMessageType: String? = null,
    val restrictionMediaUrl: String? = null,
    val restrictionThumbnailUrl: String? = null,
    val restrictedAt: Long? = null,
    val restrictedByUid: String? = null
) {
    companion object {
        const val COLLECTION_USERS = "users"

        const val FIELD_NAME = "name"
        const val FIELD_BANNED = "banned"
        const val FIELD_RESTRICTED_UNTIL = "restrictedUntil"
        const val FIELD_RESTRICTION_REASON = "restrictionReason"
        const val FIELD_RESTRICTION_MESSAGE_PREVIEW = "restrictionMessagePreview"
        const val FIELD_RESTRICTION_MESSAGE_TYPE = "restrictionMessageType"
        const val FIELD_RESTRICTION_MEDIA_URL = "restrictionMediaUrl"
        const val FIELD_RESTRICTION_THUMBNAIL_URL = "restrictionThumbnailUrl"
        const val FIELD_RESTRICTED_AT = "restrictedAt"
        const val FIELD_RESTRICTED_BY_UID = "restrictedByUid"

        fun fromMap(uid: String, data: Map<String, Any?>): RestrictedUser = RestrictedUser(
            uid = uid,
            name = data[FIELD_NAME] as? String ?: "",
            banned = data[FIELD_BANNED] as? Boolean ?: false,
            restrictedUntil = data[FIELD_RESTRICTED_UNTIL].asEpochMs(),
            restrictionReason = data[FIELD_RESTRICTION_REASON] as? String,
            restrictionMessagePreview = data[FIELD_RESTRICTION_MESSAGE_PREVIEW] as? String,
            restrictionMessageType = data[FIELD_RESTRICTION_MESSAGE_TYPE] as? String,
            restrictionMediaUrl = data[FIELD_RESTRICTION_MEDIA_URL] as? String,
            restrictionThumbnailUrl = data[FIELD_RESTRICTION_THUMBNAIL_URL] as? String,
            restrictedAt = data[FIELD_RESTRICTED_AT].asEpochMs(),
            restrictedByUid = data[FIELD_RESTRICTED_BY_UID] as? String,
        )

        /**
         * Kısıtlama bitiş zamanı (epoch ms). Android: `Timestamp(now.seconds + days*24*60*60, 0)` —
         * yani şimdiki zamanın SANİYEYE AŞAĞI yuvarlanmış hâli + gün sayısı (nanosaniye sıfırlanır).
         */
        fun restrictedUntilMs(nowMs: Long, days: Int): Long =
            (nowMs.floorDiv(1000L) + days * 24L * 60L * 60L) * 1000L

        /**
         * Android: restrictReportedUser'ın kullanıcı dokümanına yazdığı güncelleme.
         * [days] == null → kalıcı yasak: `banned = true` ve `restrictedUntil` alanı SİLİNİR
         * (dönen Map'te [silinecekAlanlar] ile bildirilir; platform `FieldValue.delete()` koyar).
         * [days] != null → süreli kısıtlama: `restrictedUntil` = bitiş, `banned = false`.
         */
        fun restrictionUpdate(report: MessageReport, myUid: String, nowMs: Long, days: Int?): RestrictionUpdate {
            val restrictionContext = mapOf<String, Any?>(
                FIELD_RESTRICTION_REASON to report.reason,
                FIELD_RESTRICTION_MESSAGE_PREVIEW to report.messagePreview,
                FIELD_RESTRICTION_MESSAGE_TYPE to report.type,
                FIELD_RESTRICTION_MEDIA_URL to report.mediaUrl,
                FIELD_RESTRICTION_THUMBNAIL_URL to report.thumbnailUrl,
                FIELD_RESTRICTED_AT to nowMs,
                FIELD_RESTRICTED_BY_UID to myUid,
            )
            return if (days == null) {
                RestrictionUpdate(
                    alanlar = restrictionContext + mapOf(FIELD_BANNED to true),
                    silinecekAlanlar = listOf(FIELD_RESTRICTED_UNTIL),
                )
            } else {
                RestrictionUpdate(
                    alanlar = restrictionContext + mapOf(
                        FIELD_RESTRICTED_UNTIL to restrictedUntilMs(nowMs, days),
                        FIELD_BANNED to false,
                    ),
                    silinecekAlanlar = emptyList(),
                )
            }
        }
    }

    /** Kullanıcı dokümanına uygulanacak güncelleme. Zaman alanları epoch ms. */
    data class RestrictionUpdate(
        val alanlar: Map<String, Any?>,
        /** `FieldValue.delete()` ile silinecek alanlar. */
        val silinecekAlanlar: List<String>,
    )
}

/** Firestore `users/{uid}` — "soru sor" düğmesi ve sohbet kısıtları için ortak denetim. */
object UserAskQuestionRestriction {
    /**
     * Yasaklıysa ya da kısıtlama bitişi şimdiden SONRAYSA kısıtlıdır (eşitse kısıtlı değil).
     * Android'de karşılaştırma nanosaniye duyarlıdır; burada milisaniye.
     */
    fun isRestricted(banned: Boolean?, restrictedUntilMs: Long?, nowMs: Long): Boolean {
        val isBanned = banned == true
        return isBanned || (restrictedUntilMs != null && restrictedUntilMs > nowMs)
    }

    fun isRestricted(userData: Map<String, Any?>, nowMs: Long): Boolean =
        isRestricted(
            banned = userData[RestrictedUser.FIELD_BANNED] as? Boolean,
            restrictedUntilMs = userData[RestrictedUser.FIELD_RESTRICTED_UNTIL].asEpochMs(),
            nowMs = nowMs,
        )
}

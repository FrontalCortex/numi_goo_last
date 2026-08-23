package com.example.app.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class RestrictedUser(
    @DocumentId val uid: String = "",
    val name: String = "",
    val banned: Boolean = false,
    val restrictedUntil: Timestamp? = null,
    val restrictionReason: String? = null,
    val restrictionMessagePreview: String? = null,
    val restrictionMessageType: String? = null,
    val restrictionMediaUrl: String? = null,
    val restrictionThumbnailUrl: String? = null,
    val restrictedAt: Timestamp? = null,
    val restrictedByUid: String? = null
)

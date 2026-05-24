package com.example.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/** Firestore `users/{uid}` — askQuestion butonu ve sohbet kısıtları için ortak kontrol. */
object UserAskQuestionRestriction {
    fun isRestricted(userDoc: DocumentSnapshot): Boolean {
        val banned = userDoc.getBoolean("banned") == true
        val restrictedUntil = userDoc.getTimestamp("restrictedUntil")
        val now = Timestamp.now()
        return banned || (restrictedUntil != null && restrictedUntil.compareTo(now) > 0)
    }
}

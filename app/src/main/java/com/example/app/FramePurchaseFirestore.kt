package com.example.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.app.abacus.AbacusPreferences.FrameType

data class FrameData(val owned: Boolean, val colorFeatureActive: Boolean)

/**
 * Manages frame ownership in Firestore.
 * Firestore path: users/{uid}/haveFrame/{frameId}
 * Document fields: { purchasedAt: Timestamp, colorFeatureActive: Boolean }
 */
object FramePurchaseFirestore {

    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_HAVE_FRAME = "haveFrame"
    private const val FIELD_PURCHASED_AT = "purchasedAt"
    private const val FIELD_COLOR_FEATURE = "colorFeatureActive"

    fun loadOwnedFrames(
        uid: String,
        onResult: (Map<String, FrameData>) -> Unit,
        onFailure: ((Exception) -> Unit)? = null,
    ) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_HAVE_FRAME)
            .get()
            .addOnSuccessListener { snapshot ->
                val map = mutableMapOf<String, FrameData>()
                for (doc in snapshot.documents) {
                    val owned = doc.contains(FIELD_PURCHASED_AT)
                    val colorActive = doc.getBoolean(FIELD_COLOR_FEATURE) ?: false
                    map[doc.id] = FrameData(owned, colorActive)
                }
                onResult(map)
            }
            .addOnFailureListener { e ->
                onFailure?.invoke(e)
                onResult(emptyMap())
            }
    }

    fun markFrameOwned(
        uid: String,
        frameType: FrameType,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null,
    ) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_HAVE_FRAME)
            .document(frameType.name)
            .set(mapOf(FIELD_PURCHASED_AT to Timestamp.now()), SetOptions.merge())
            .addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener { e -> onFailure?.invoke(e) }
    }

    fun setColorFeatureActive(
        uid: String,
        frameType: FrameType,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null,
    ) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_HAVE_FRAME)
            .document(frameType.name)
            .set(mapOf(FIELD_COLOR_FEATURE to true), SetOptions.merge())
            .addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener { e -> onFailure?.invoke(e) }
    }
}


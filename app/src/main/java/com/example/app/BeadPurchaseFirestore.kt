package com.example.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

data class BeadData(val count: Int, val colorFeatureActive: Boolean)

/**
 * Manages the "haveBead" sub-collection in Firestore.
 *
 * Firestore path: users/{uid}/haveBead/{beadId}
 * Document fields: { purchasedAt: Timestamp, count: Int, colorFeatureActive: Boolean }
 *
 * "beadId" matches the BeadType enum name (e.g. "SOROBAN2", "BOWLING", "BALL1", "ANIMAL", ?)
 */
object BeadPurchaseFirestore {

    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_HAVE_BEAD = "haveBead"
    private const val FIELD_PURCHASED_AT = "purchasedAt"
    private const val FIELD_COUNT = "count"
    private const val FIELD_COLOR_FEATURE = "colorFeatureActive"

    /**
     * Loads the map of bead IDs to their data that the user already owns or customized.
     * Returns an empty map on failure so the UI can still show all beads as locked.
     */
    fun loadOwnedBeads(
        uid: String,
        onResult: (Map<String, BeadData>) -> Unit,
        onFailure: ((Exception) -> Unit)? = null,
    ) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_HAVE_BEAD)
            .get()
            .addOnSuccessListener { snapshot ->
                val map = mutableMapOf<String, BeadData>()
                for (doc in snapshot.documents) {
                    val count = doc.getLong(FIELD_COUNT)?.toInt() ?: if (doc.contains(FIELD_PURCHASED_AT)) 25 else 0
                    val colorFeatureActive = doc.getBoolean(FIELD_COLOR_FEATURE) ?: false
                    map[doc.id] = BeadData(count, colorFeatureActive)
                }
                onResult(map)
            }
            .addOnFailureListener { e ->
                onFailure?.invoke(e)
                onResult(emptyMap())
            }
    }

    /**
     * Sets or updates the count of a purchased bead in Firestore.
     */
    fun setBeadCount(
        uid: String,
        beadId: String,
        count: Int,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null,
    ) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_HAVE_BEAD)
            .document(beadId)
            .set(mapOf(FIELD_PURCHASED_AT to Timestamp.now(), FIELD_COUNT to count), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener { e -> onFailure?.invoke(e) }
    }

    /**
     * Sets colorFeatureActive for a bead in Firestore without modifying the count.
     */
    fun setColorFeatureActive(
        uid: String,
        beadId: String,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null,
    ) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_HAVE_BEAD)
            .document(beadId)
            .set(mapOf(FIELD_COLOR_FEATURE to true), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener { e -> onFailure?.invoke(e) }
    }
}

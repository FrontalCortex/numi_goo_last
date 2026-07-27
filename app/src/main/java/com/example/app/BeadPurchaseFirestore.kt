package com.example.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Manages the "haveBead" sub-collection in Firestore.
 *
 * Firestore path: users/{uid}/haveBead/{beadId}
 * Document fields: { purchasedAt: Timestamp }
 *
 * "beadId" matches the BeadType enum name (e.g. "SOROBAN2", "BOWLING", "BALL1", "ANIMAL", …)
 */
object BeadPurchaseFirestore {

    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_HAVE_BEAD = "haveBead"
    private const val FIELD_PURCHASED_AT = "purchasedAt"

    /**
     * Loads the set of bead IDs the user already owns.
     * Returns an empty set on failure so the UI can still show all beads as locked.
     */
    fun loadOwnedBeads(
        uid: String,
        onResult: (Set<String>) -> Unit,
        onFailure: ((Exception) -> Unit)? = null,
    ) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_HAVE_BEAD)
            .get()
            .addOnSuccessListener { snapshot ->
                val ids = snapshot.documents.map { it.id }.toSet()
                onResult(ids)
            }
            .addOnFailureListener { e ->
                onFailure?.invoke(e)
                onResult(emptySet())
            }
    }

    /**
     * Saves a newly purchased bead to Firestore.
     */
    fun savePurchasedBead(
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
            .set(mapOf(FIELD_PURCHASED_AT to Timestamp.now()))
            .addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener { e -> onFailure?.invoke(e) }
    }
}

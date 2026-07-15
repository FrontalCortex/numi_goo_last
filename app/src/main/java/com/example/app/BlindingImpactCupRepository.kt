package com.example.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Kullanıcının `blinding_impact_abacus_cup` kupa skorunu Firestore üzerinden okur/yazar.
 * Card6View körleme çarpma kupa modu için kullanılır.
 */
object BlindingImpactCupRepository {

    private const val DEFAULT_CUP_SCORE = 200
    private const val FIELD = "blinding_impact_abacus_cup"
    private const val COLLECTION = "users"

    fun fetchCupScore(onResult: (score: Int) -> Unit) {
        val uid = uid() ?: return
        FirebaseFirestore.getInstance()
            .collection(COLLECTION)
            .document(uid)
            .collection("cupWayProgress")
            .document("progress")
            .get()
            .addOnSuccessListener { doc ->
                val raw = (doc?.get(FIELD) as? Number)?.toInt()
                if (raw == null) {
                    val initialScore = DEFAULT_CUP_SCORE
                    doc?.reference?.set(
                        mapOf(FIELD to initialScore),
                        SetOptions.merge(),
                    )
                    onResult(initialScore)
                } else {
                    onResult(raw)
                }
            }
            .addOnFailureListener {
                onResult(DEFAULT_CUP_SCORE)
            }
    }

    fun updateCupScore(delta: Int, onDone: ((oldScore: Int, newScore: Int) -> Unit)? = null) {
        val uid = uid() ?: return
        val ref = FirebaseFirestore.getInstance()
            .collection(COLLECTION)
            .document(uid)
            .collection("cupWayProgress")
            .document("progress")

        FirebaseFirestore.getInstance().runTransaction { tx ->
            val snapshot = tx.get(ref)
            val current = (snapshot.getLong(FIELD) ?: DEFAULT_CUP_SCORE.toLong()).toInt()
            val updated = (current + delta).coerceAtLeast(0)
            tx.set(ref, mapOf(FIELD to updated), SetOptions.merge())
            Pair(current, updated)
        }.addOnSuccessListener { (oldScore, newScore) ->
            onDone?.invoke(oldScore, newScore)
        }.addOnFailureListener { }
    }

    private fun uid(): String? = FirebaseAuth.getInstance().currentUser?.uid
}

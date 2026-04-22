package com.example.app

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

object LessonLeaderboardRepository {

    private const val TAG = "LessonLeaderboard"
    private const val COLLECTION = "lessonLeaderboards"
    private const val ENTRIES = "entries"

    private const val F_SECONDS = "recordSeconds"
    private const val F_LABEL = "recordLabel"
    private const val F_NAME = "displayName"
    private const val F_PHOTO = "photoUrl"
    private const val F_UPDATED = "updatedAt"

    fun leaderboardDocumentId(partId: Int, lessonIndex: Int): String =
        "part_${partId}_lesson_${lessonIndex}"

    /**
     * Mevcut kayıttan daha iyi (düşük) süre ise Firestore'a yazar.
     */
    fun submitBestIfNeeded(partId: Int, lessonIndex: Int, recordLabel: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val seconds = RecordTimeUtils.parseRecordToSeconds(recordLabel) ?: return
        val db = FirebaseFirestore.getInstance()
        val entryRef = db.collection(COLLECTION)
            .document(leaderboardDocumentId(partId, lessonIndex))
            .collection(ENTRIES)
            .document(user.uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(entryRef)
            val previousBest = snapshot.getLong(F_SECONDS)?.toInt()
            if (previousBest != null && seconds >= previousBest) {
                return@runTransaction null
            }
            val data = hashMapOf<String, Any>(
                F_SECONDS to seconds,
                F_LABEL to recordLabel.trim(),
                F_NAME to (user.displayName?.take(127)?.ifBlank { null } ?: "Kullanıcı"),
                F_UPDATED to FieldValue.serverTimestamp(),
                F_PHOTO to (user.photoUrl?.toString() ?: "")
            )
            transaction.set(entryRef, data, SetOptions.merge())
            null
        }.addOnFailureListener { e ->
            Log.e(TAG, "submitBestIfNeeded failed part=$partId lesson=$lessonIndex", e)
        }
    }

    data class LeaderboardEntry(
        val userId: String,
        val rank: Int,
        val displayName: String,
        val recordLabel: String,
        val photoUrl: String?,
    )

    fun listenLeaderboard(
        partId: Int,
        lessonIndex: Int,
        onUpdate: (List<LeaderboardEntry>) -> Unit,
        onError: (Exception) -> Unit,
    ): ListenerRegistration {
        val db = FirebaseFirestore.getInstance()
        val q = db.collection(COLLECTION)
            .document(leaderboardDocumentId(partId, lessonIndex))
            .collection(ENTRIES)
            .orderBy(F_SECONDS, Query.Direction.ASCENDING)
            .limit(50)

        return q.addSnapshotListener { snapshot, e ->
            if (e != null) {
                onError(e)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                onUpdate(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot.documents.mapIndexed { index, doc ->
                LeaderboardEntry(
                    userId = doc.id,
                    rank = index + 1,
                    displayName = doc.getString(F_NAME) ?: "",
                    recordLabel = doc.getString(F_LABEL) ?: "",
                    photoUrl = doc.getString(F_PHOTO)?.takeIf { it.isNotBlank() }
                )
            }
            onUpdate(list)
        }
    }
}

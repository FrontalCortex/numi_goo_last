package com.example.app

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException

object AccountDeletionHelper {

    private const val TAG = "AccountDeletionHelper"

    /**
     * Cleans up the user's ID from all their followers and following lists,
     * decrements the respective counters, then deletes the user's profile and auth account.
     */
    suspend fun performCompleteAccountDeletion(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        onSuccess: () -> Unit,
        onRequiresRecentLogin: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            withContext(Dispatchers.Main) {
                onFailure(Exception("Kullanıcı bulunamadı."))
            }
            return
        }

        // Güvenlik kontrolü: Hassas işlemler (hesap silme) son 5 dakika içinde giriş yapılmasını gerektirir.
        // Eğer Firebase Auth silme işlemi "RecentLoginRequired" hatası verecekse, Firestore verilerini 
        // silmeden önce işlemi durduruyoruz. (5 dakika = 300,000 ms. Güvenlik payı ile 4.5 dakika kullanıyoruz)
        val lastSignInTime = user.metadata?.lastSignInTimestamp ?: 0
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSignInTime > 270_000) {
            withContext(Dispatchers.Main) {
                onRequiresRecentLogin()
            }
            return
        }

        val myUid = user.uid

        try {
            // 1. Remove from all followers' following lists
            val followersSnapshot = try {
                firestore.collection("users").document(myUid)
                    .collection("followers")
                    .get()
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Followers listesi alınamadı: ${e.message}")
                null
            }

            followersSnapshot?.documents?.forEach { doc ->
                val followerUid = doc.id
                try {
                    // Remove myUid from their following subcollection
                    firestore.collection("users").document(followerUid)
                        .collection("following").document(myUid)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Takipçinin following listesinden silinemedi ($followerUid): ${e.message}")
                }
                
                try {
                    // Decrement their followingCount
                    firestore.collection("users").document(followerUid)
                        .update("followingCount", FieldValue.increment(-1))
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Takipçinin followingCount'u güncellenemedi ($followerUid): ${e.message}")
                }
            }

            // 2. Remove from all following's followers lists
            val followingSnapshot = try {
                firestore.collection("users").document(myUid)
                    .collection("following")
                    .get()
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Following listesi alınamadı: ${e.message}")
                null
            }

            followingSnapshot?.documents?.forEach { doc ->
                val followingUid = doc.id
                try {
                    // Remove myUid from their followers subcollection
                    firestore.collection("users").document(followingUid)
                        .collection("followers").document(myUid)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Takip edilenin followers listesinden silinemedi ($followingUid): ${e.message}")
                }

                try {
                    // Decrement their followersCount
                    firestore.collection("users").document(followingUid)
                        .update("followersCount", FieldValue.increment(-1))
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Takip edilenin followersCount'u güncellenemedi ($followingUid): ${e.message}")
                }
            }

            try {
                // 3. Delete user's profile document from Firestore
                firestore.collection("users").document(myUid).delete().await()
            } catch (e: Exception) {
                throw Exception("Kullanıcı Firestore belgesi silinemedi: ${e.message}")
            }

            try {
                // 4. Delete user's Authentication record
                user.delete().await()
            } catch (e: Exception) {
                if (e is FirebaseAuthRecentLoginRequiredException) {
                    throw e
                }
                throw Exception("Kullanıcı Auth hesabı silinemedi: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during account deletion", e)
            withContext(Dispatchers.Main) {
                if (e is FirebaseAuthRecentLoginRequiredException) {
                    onRequiresRecentLogin()
                } else {
                    onFailure(e)
                }
            }
        }
    }
}

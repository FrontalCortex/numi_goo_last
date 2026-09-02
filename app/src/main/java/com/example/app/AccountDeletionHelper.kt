package com.example.app

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
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

            // Not: followersCount / followingCount sayaçlarını artık burada elle
            // düşürmüyoruz. Bu alanlar istemci yazımına kapatıldı (eskiden herkes
            // başkasının sayacını değiştirebiliyordu); takip kaydı silindiğinde
            // sunucudaki onFollowingDeleted / onFollowerDeleted trigger'ları sayacı
            // kendisi azaltıyor.
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
                    // (followersCount'u sunucudaki onFollowerDeleted trigger'ı düşürüyor.)
                    firestore.collection("users").document(followingUid)
                        .collection("followers").document(myUid)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Takip edilenin followers listesinden silinemedi ($followingUid): ${e.message}")
                }
            }

            // 3. Kendi alt koleksiyonlarını temizle: Firestore'da bir dokümanı silmek
            // alt koleksiyonlarını OTOMATİK silmez, bu yüzden hepsi tek tek temizlenmeli.
            deleteAllUserSubcollections(firestore, myUid)

            try {
                // 4. Delete user's profile document from Firestore
                firestore.collection("users").document(myUid).delete().await()
            } catch (e: Exception) {
                throw Exception("Kullanıcı Firestore belgesi silinemedi: ${e.message}")
            }

            try {
                // 5. Delete user's Authentication record
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

    /**
     * `users/{uid}` altında bilinen tüm alt koleksiyonları siler.
     * Firestore'da üst doküman silinse bile alt koleksiyonlar kendiliğinden silinmediği için,
     * `users/{uid}` dokümanını silmeden önce bunların tek tek temizlenmesi gerekir.
     * Her adım kendi içinde try/catch ile korunur: biri başarısız olsa bile diğer temizlik
     * adımları ve asıl hesap silme işlemi devam eder.
     */
    private suspend fun deleteAllUserSubcollections(firestore: FirebaseFirestore, uid: String) {
        val userDoc = firestore.collection("users").document(uid)

        // Kendi followers/following listesindeki dokümanlar (karşı taraftaki referanslar
        // yukarıda zaten temizlendi, burada sadece kullanıcının kendi alt koleksiyonu siliniyor).
        deleteAllDocsInCollection(firestore, userDoc.collection("followers"))
        deleteAllDocsInCollection(firestore, userDoc.collection("following"))

        // lessonProgress/{partId}/items/{position} — iki seviyeli, önce items sonra partId dokümanı.
        try {
            val partDocs = userDoc.collection("lessonProgress").get().await()
            partDocs.documents.forEach { partDoc ->
                deleteAllDocsInCollection(firestore, partDoc.reference.collection("items"))
                try {
                    partDoc.reference.delete().await()
                } catch (e: Exception) {
                    Log.e(TAG, "lessonProgress/${partDoc.id} silinemedi: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "lessonProgress listesi alınamadı: ${e.message}")
        }

        // Tek dokümanlık alt koleksiyonlar
        deleteSingleDoc(userDoc.collection("badgeProgress").document("state"))
        deleteSingleDoc(userDoc.collection("cupWayProgress").document("progress"))
        deleteSingleDoc(userDoc.collection("abacusCustomization").document("state"))
        deleteSingleDoc(userDoc.collection("missionProgress").document("state"))
        deleteSingleDoc(userDoc.collection("dailyQuestion").document("current"))

        // Çok dokümanlı düz (leaf) alt koleksiyonlar
        deleteAllDocsInCollection(firestore, userDoc.collection("cupHistory"))
        deleteAllDocsInCollection(firestore, userDoc.collection("lessonSuccessRateState"))
    }

    private suspend fun deleteAllDocsInCollection(firestore: FirebaseFirestore, collection: CollectionReference) {
        val snapshot = try {
            collection.get().await()
        } catch (e: Exception) {
            Log.e(TAG, "${collection.path} okunamadı: ${e.message}")
            return
        }
        if (snapshot.isEmpty) return
        snapshot.documents.chunked(450).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            try {
                batch.commit().await()
            } catch (e: Exception) {
                Log.e(TAG, "${collection.path} toplu silme başarısız: ${e.message}")
            }
        }
    }

    private suspend fun deleteSingleDoc(doc: com.google.firebase.firestore.DocumentReference) {
        try {
            doc.delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "${doc.path} silinemedi: ${e.message}")
        }
    }
}

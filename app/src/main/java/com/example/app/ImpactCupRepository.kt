package com.example.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Kullanıcının `impact_abacus_cup` kupa skorunu OKUR.
 *
 * Veri yolu: `users/{uid}/cupWayProgress/progress` içindeki aynı adlı alan.
 *
 * Yazma tarafı burada yok: kupa puanı bir musluk (kupa yolu sandıkları ve kupa rozetleri
 * ona bakıyor), bu yüzden koleksiyon istemciye kapatıldı ve puanı yalnızca sunucudaki
 * `submitCupResult` değiştiriyor — bkz. [CupScoreService].
 *
 * Alan yoksa [DEFAULT_CUP_SCORE] dönülür ama yazılmaz; doküman ilk ders sonucunda sunucuda
 * oluşur.
 */
object ImpactCupRepository {

    private const val DEFAULT_CUP_SCORE = 200
    private const val FIELD = "impact_abacus_cup"
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
                // Alan yoksa varsayılan DÖNÜLÜYOR ama yazılmıyor: cupWayProgress artık
                // istemciye kapalı. Doküman ilk ders sonucunda sunucuda oluşuyor.
                onResult((doc?.get(FIELD) as? Number)?.toInt() ?: DEFAULT_CUP_SCORE)
            }
            .addOnFailureListener {
                onResult(DEFAULT_CUP_SCORE)
            }
    }

    private fun uid(): String? = FirebaseAuth.getInstance().currentUser?.uid
}

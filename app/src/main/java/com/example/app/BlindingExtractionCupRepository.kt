package com.example.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Kullanıcının `blinding_extraction_abacus_cup` kupa skorunu OKUR.
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
object BlindingExtractionCupRepository {

    private const val DEFAULT_CUP_SCORE = 200
    private const val FIELD = "blinding_extraction_abacus_cup"
    private const val COLLECTION = "users"

    // --------------------------------------------------------------------------------------------
    // Okuma
    // --------------------------------------------------------------------------------------------

    /**
     * Güncel kupa skorunu Firestore'dan asenkron olarak okur.
     * Alan yoksa [DEFAULT_CUP_SCORE] ile seed eder ve aynı değeri döner.
     * Kullanıcı giriş yapmamışsa [onResult] çağrılmaz.
     */
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
                // Okuma başarısız → güvenli varsayılan
                onResult(DEFAULT_CUP_SCORE)
            }
    }

    // --------------------------------------------------------------------------------------------
    // Yardımcı
    // --------------------------------------------------------------------------------------------

    private fun uid(): String? = FirebaseAuth.getInstance().currentUser?.uid
}

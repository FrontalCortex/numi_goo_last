package com.example.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Kullanıcının `extraction_abacus_cup` kupa skorunu Firestore üzerinden okur/yazar.
 *
 * Veri yolu: `users/{uid}/extraction_abacus_cup` (INT alanı)
 * İlk erişimde alan yoksa varsayılan değer [DEFAULT_CUP_SCORE] olarak seed edilir.
 */
object ExtractionCupRepository {

    private const val DEFAULT_CUP_SCORE = 200
    private const val FIELD = "extraction_abacus_cup"
    private const val COLLECTION = "users"

    /** Kupa değişiminin sabit adım büyüklüğü. */
    const val CUP_STEP = 5

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
                val raw = (doc?.get(FIELD) as? Number)?.toInt()
                if (raw == null) {
                    // Alan yok → seed et
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
                // Okuma başarısız → güvenli varsayılan
                onResult(DEFAULT_CUP_SCORE)
            }
    }

    // --------------------------------------------------------------------------------------------
    // Yazma
    // --------------------------------------------------------------------------------------------

    /**
     * Kupa skorunu [delta] kadar artırır/azaltır. Sonuç 0'ın altına düşemez.
     * İşlem Firestore transaction ile atomik olarak yapılır.
     *
     * @param delta  Pozitif (kazanç) veya negatif (kayıp) değişim miktarı
     * @param onDone İşlem tamamlandığında yeni skoru iletir
     */
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
        }.addOnFailureListener {
            // Hata durumunda callback'i atla; UI eski değeri göstermeye devam eder
        }
    }

    // --------------------------------------------------------------------------------------------
    // Yardımcı
    // --------------------------------------------------------------------------------------------

    private fun uid(): String? = FirebaseAuth.getInstance().currentUser?.uid
}

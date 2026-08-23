package com.example.app

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Kupa skorlarının günlük geçmişini `users/{uid}/cupHistory/{yyyy-MM-dd}` altında tutar.
 *
 * Her doküman, o günün son değerlerini `cupWayProgress/progress` ile aynı alan adlarıyla saklar.
 * Aynı gün içindeki tekrarlı güncellemeler [SetOptions.merge] ile üzerine yazılır.
 */
object CupHistoryRepository {

    private const val COLLECTION = "users"
    private const val SUBCOLLECTION = "cupHistory"

    private fun todayId(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** Mevcut bir transaction içinde bugünün kupa değerini geçmişe yazar. */
    fun recordSnapshot(tx: Transaction, uid: String, field: String, value: Int) {
        val ref = FirebaseFirestore.getInstance()
            .collection(COLLECTION)
            .document(uid)
            .collection(SUBCOLLECTION)
            .document(todayId())
        tx.set(ref, mapOf(field to value), SetOptions.merge())
    }

    /**
     * Belirtilen kullanıcının, belirtilen kupa alanına ait TÜM geçmiş noktalarını okur.
     * Hesap ne kadar eskiyse okunan doküman sayısı o kadar artar — sadece "Yıllık"/"Tüm Zamanlar"
     * gibi tüm geçmişi gerektiren görünümler için kullanılmalı. Diğer aralıklar için [fetchHistorySince]
     * tercih edilmeli.
     * Sonuç (tarih "yyyy-MM-dd", değer) çiftleri olarak tarihe göre artan sırada döner.
     */
    fun fetchHistory(uid: String, field: String, onResult: (List<Pair<String, Int>>) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION)
            .document(uid)
            .collection(SUBCOLLECTION)
            .get()
            .addOnSuccessListener { snapshot -> onResult(mapSnapshot(snapshot, field)) }
            .addOnFailureListener { onResult(emptyList()) }
    }

    /**
     * [sinceDateId] (dahil, "yyyy-MM-dd") tarihinden bugüne kadarki noktaları okur.
     * Doküman ID'leri tarih string'i olduğundan `documentId()` üzerinden aralık sorgusu yapılabiliyor;
     * bu sayede Günlük/Haftalık/Aylık gibi sınırlı aralıklar için hesap yaşından bağımsız,
     * sabit maliyetli bir okuma yapılır.
     */
    fun fetchHistorySince(uid: String, field: String, sinceDateId: String, onResult: (List<Pair<String, Int>>) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION)
            .document(uid)
            .collection(SUBCOLLECTION)
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), sinceDateId)
            .get()
            .addOnSuccessListener { snapshot -> onResult(mapSnapshot(snapshot, field)) }
            .addOnFailureListener { onResult(emptyList()) }
    }

    private fun mapSnapshot(
        snapshot: com.google.firebase.firestore.QuerySnapshot,
        field: String,
    ): List<Pair<String, Int>> =
        snapshot.documents
            .mapNotNull { doc -> (doc.get(field) as? Number)?.toInt()?.let { doc.id to it } }
            .sortedBy { it.first }
}

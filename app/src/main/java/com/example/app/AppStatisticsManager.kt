package com.example.app

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object AppStatisticsManager {

    private const val TAG = "AppStatisticsManager"
    private const val COLLECTION_APP_STATISTICS = "appStatistics"
    private const val DOC_ACQUISITION_SOURCES = "acquisition_sources"

    /**
     * Kullanıcının nereden geldiğini seçtiği kaynağın istatistiğini
     * 'appStatistics/acquisition_sources' dokümanında atomik olarak 1 artırır.
     * Doküman henüz yoksa otomatik oluşturur (SetOptions.merge).
     */
    fun incrementAcquisitionSource(source: String?) {
        if (source.isNullOrBlank()) return

        val db = FirebaseFirestore.getInstance()
        val statsRef = db.collection(COLLECTION_APP_STATISTICS).document(DOC_ACQUISITION_SOURCES)

        statsRef.set(
            mapOf(source to FieldValue.increment(1)),
            SetOptions.merge()
        ).addOnSuccessListener {
            Log.d(TAG, "Acquisition source statistic incremented for: $source")
        }.addOnFailureListener { e ->
            Log.w(TAG, "Failed to increment acquisition source statistic for: $source", e)
        }
    }
}

package com.example.app

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Kupa dersi sonucunu sunucuya bildirir (`submitCupResult`).
 *
 * ## Neden istemci artık kupa puanı yazmıyor
 * Kupa puanı yalnızca bir skor değil, bir musluk: kupa yolu sandıkları ve kupa rozetleri ona
 * bakıyor. `cupWayProgress` istemciye açıkken "kupamı 999999 yap" demek bedava sandık
 * demekti. Koleksiyon artık yazmaya kapalı; puan yalnızca sunucudan değişiyor.
 *
 * İstemci DEĞİŞİM MİKTARINI da göndermiyor — yalnızca dersin kazanılıp kazanılmadığını,
 * zorluk kademesini ve modunu söylüyor. Miktarı sunucu kendi tablosundan hesaplıyor, yani
 * tablo iki yerde birden tutulmuyor ve istemci miktarı şişiremiyor.
 */
object CupScoreService {

    private const val TAG = "CupScoreService"

    /**
     * Ders sonucunu gönderir.
     *
     * @param onDone Sunucudan dönen eski ve yeni puan. Başarısızlıkta çağrılmaz — arayüz
     *   eski değeri göstermeye devam eder, tıpkı eski transaction'ın başarısız hâlinde
     *   olduğu gibi.
     */
    fun submitResult(
        cupField: String,
        won: Boolean,
        difficultyLevel: Int,
        isMultiplication: Boolean,
        onDone: (oldScore: Int, newScore: Int) -> Unit,
    ) {
        val payload = hashMapOf(
            "cupField" to cupField,
            "won" to won,
            "difficultyLevel" to difficultyLevel.coerceIn(0, 4),
            "isMultiplication" to isMultiplication,
            // Geçmiş grafiğinin günü: sunucu UTC kullansaydı gece yarısından sonraki dersler
            // bir önceki güne yazılırdı.
            "dayId" to SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
        )
        FirebaseFunctions.getInstance()
            .getHttpsCallable("submitCupResult")
            .call(payload)
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val oldScore = (data?.get("oldScore") as? Number)?.toInt()
                val newScore = (data?.get("newScore") as? Number)?.toInt()
                if (oldScore == null || newScore == null) {
                    Log.w(TAG, "submitCupResult beklenen alanları döndürmedi")
                    return@addOnSuccessListener
                }
                onDone(oldScore, newScore)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "submitCupResult başarısız", e)
            }
    }

    /** Ders hangi kupa yoluna yazılacak — kartlardaki eşleşmenin aynısı. */
    fun cupFieldFor(lessonItem: com.example.app.model.LessonItem): String = when {
        lessonItem.isMultiplication == true && lessonItem.isBlinding == true ->
            CupPathRewardRepository.FIELD_BLINDING_IMPACT
        lessonItem.isMultiplication == true -> CupPathRewardRepository.FIELD_IMPACT
        lessonItem.isExtraction == true && lessonItem.isBlinding == true ->
            CupPathRewardRepository.FIELD_BLINDING_EXTRACTION
        lessonItem.isExtraction == true -> CupPathRewardRepository.FIELD_EXTRACTION
        lessonItem.isBlinding == true -> CupPathRewardRepository.FIELD_BLINDING_ADDITION
        else -> CupPathRewardRepository.FIELD_ADDITION
    }
}

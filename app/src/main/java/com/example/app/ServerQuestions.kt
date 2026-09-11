package com.example.app

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/**
 * Öğretmene soru gönderme — sunucu taraflı.
 *
 * Soru dokümanı artık istemciden yazılmıyor: `askTeacherQuestion` krediyi düşer ve soruyu
 * aynı transaction'da oluşturur. Böylece hem kredi harcamadan soru gönderilmesi hem de
 * "kredi gitti ama soru oluşmadı" durumu imkânsız hale gelir.
 *
 * Medya yüklemesi istemcide kalır; buraya yalnızca Storage yolu ve indirme adresi gider.
 */
object ServerQuestions {

    private const val TAG = "ServerQuestions"

    const val MEDIA_IMAGE = "image"
    const val MEDIA_VIDEO = "video"

    fun ask(
        mediaType: String,
        storagePath: String,
        mediaUrl: String,
        message: String,
        previewText: String,
        description: String?,
        videoDurationSec: Int = 0,
        onResult: (questionId: String, remainingCredits: Int) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        val payload = hashMapOf<String, Any>(
            "mediaType" to mediaType,
            "storagePath" to storagePath,
            "mediaUrl" to mediaUrl,
            "message" to message,
            "previewText" to previewText,
        )
        if (!description.isNullOrBlank()) payload["description"] = description
        if (videoDurationSec > 0) payload["videoDurationSec"] = videoDurationSec

        FirebaseFunctions.getInstance()
            .getHttpsCallable("askTeacherQuestion")
            .call(payload)
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val questionId = data?.get("questionId") as? String
                if (questionId.isNullOrBlank()) {
                    onFailure(IllegalStateException("askTeacherQuestion geçersiz yanıt döndü"))
                    return@addOnSuccessListener
                }
                onResult(questionId, (data["credits"] as? Number)?.toInt() ?: 0)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "askTeacherQuestion başarısız", e)
                onFailure(e)
            }
    }

    /**
     * Kredi yetersizliğini diğer hatalardan ayırır; arayüz bu durumda kullanıcıyı
     * mağazaya yönlendirebilir. Sunucu bu durumda `failed-precondition` döndürür.
     */
    fun isInsufficientCredits(e: Exception): Boolean {
        val ffe = e as? FirebaseFunctionsException ?: return false
        return ffe.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION
    }
}

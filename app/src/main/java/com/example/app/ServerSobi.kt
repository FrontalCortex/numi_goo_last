package com.example.app

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/**
 * "Sobi'ye sor" sohbetinin sunucu çağrısı (`sobiChat`, functions/sobi/callable.js).
 *
 * Sohbetin bütün aklı sunucuda: talimat metni, araçlar, güvenlik denetimi, günlük hak. Uygulama
 * yalnızca adımın bağlamını ([SobiContext]) ve çocuğun mesajını gönderir, gelen metni gösterir
 * ve ekran işlerini ([Action]: abaküste göster) yapar. API anahtarı uygulamada YOK.
 */
object ServerSobi {

    private const val TAG = "ServerSobi"

    /** Sunucunun abaküste yapılmasını istediği iş. Şimdilik yalnızca "goster". */
    data class Action(
        val kind: String,
        val base: Int,
        val op: String,
        val number: Int,
        val result: Int,
    )

    data class Reply(
        val sessionId: String,
        /** Çocuğa gösterilecek metin (denetimden geçmiş ya da yedek metin). */
        val text: String?,
        val actions: List<Action>,
        /** Öneri balonu anahtarları: "bana_goster", "neden_boyle", "bir_daha_goster", "anladim". */
        val suggestions: List<String>,
        /** Bu sohbette çocuk mesaj sınırına geldi; sunucu modele hiç gitmedi. */
        val turnLimitReached: Boolean,
        /** Model şu an çağrılamadı; [text] kural motorunun yedek metni. */
        val unavailable: Boolean,
        val remainingToday: Int?,
    )

    const val ACTION_SHOW = "goster"

    fun send(
        sessionId: String?,
        message: String,
        context: SobiContext,
        onResult: (Reply) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        val payload = hashMapOf<String, Any>(
            "message" to message,
            "context" to context.toPayload(),
        )
        if (!sessionId.isNullOrBlank()) payload["sessionId"] = sessionId

        FirebaseFunctions.getInstance()
            .getHttpsCallable("sobiChat")
            .call(payload)
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val id = data?.get("sessionId") as? String
                if (id.isNullOrBlank()) {
                    onFailure(IllegalStateException("sobiChat geçersiz yanıt döndü"))
                    return@addOnSuccessListener
                }
                onResult(
                    Reply(
                        sessionId = id,
                        text = data["reply"] as? String,
                        actions = (data["actions"] as? List<*>).orEmpty().mapNotNull(::parseAction),
                        suggestions = (data["suggestions"] as? List<*>).orEmpty().filterIsInstance<String>(),
                        turnLimitReached = data["turnLimitReached"] == true,
                        unavailable = data["unavailable"] == true,
                        remainingToday = (data["remainingToday"] as? Number)?.toInt(),
                    )
                )
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "sobiChat başarısız", e)
                onFailure(e)
            }
    }

    private fun parseAction(raw: Any?): Action? {
        val m = raw as? Map<*, *> ?: return null
        val kind = m["tur"] as? String ?: return null
        val base = (m["baslangic"] as? Number)?.toInt() ?: return null
        val op = m["islem"] as? String ?: return null
        val number = (m["sayi"] as? Number)?.toInt() ?: return null
        val result = (m["sonuc"] as? Number)?.toInt() ?: return null
        return Action(kind, base, op, number, result)
    }

    /** Günlük hak bitti (`resource-exhausted`). */
    fun isDailyLimit(e: Exception): Boolean =
        (e as? FirebaseFunctionsException)?.code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED

    /** Hesap kısıtlı (`permission-denied`); askQuestion kısıtıyla aynı kural. */
    fun isRestricted(e: Exception): Boolean =
        (e as? FirebaseFunctionsException)?.code == FirebaseFunctionsException.Code.PERMISSION_DENIED
}

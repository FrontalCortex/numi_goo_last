package com.example.app

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/**
 * "Sobi'ye sor" sohbetinin sunucu çağrısı (`sobiChat`, functions/sobi/callable.js).
 *
 * Sohbetin bütün aklı sunucuda: talimat metni, araçlar, güvenlik denetimi, günlük hak ve kredi
 * (functions/sobi/wallet.js). Uygulama
 * yalnızca adımın bağlamını ([SobiContext]) ve çocuğun mesajını gönderir, gelen metni gösterir
 * ve ekran işlerini ([Action]: abaküste göster) yapar. API anahtarı uygulamada YOK.
 */
object ServerSobi {

    private const val TAG = "ServerSobi"

    /**
     * Sunucunun ekranda yapılmasını istediği iş: abaküste göster, örnek aç, sütun parlat; Sobi
     * sekmesinde ayrıca test / kredi / Pro düğmesi (bunlarda sayı alanları 0).
     */
    data class Action(
        val kind: String,
        val base: Int,
        val op: String,
        val number: Int,
        val result: Int,
        /** [ACTION_POINT]: parlatılacak sütun (0 = birler). */
        val column: Int? = null,
        /** [ACTION_LESSON]: açılacak ders bölümü (PartSelectionFragment: 1–6). */
        val part: Int? = null,
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
        /** Bugün kalan ücretsiz mesaj. */
        val remainingToday: Int?,
        /** Kalan Sobi kredisi (ücretsiz hak bitince her model mesajı 1). */
        val credits: Int?,
        /** Cevabın sunucudaki sohbet kaydında sırası; beğen / beğenme buna bağlanır. */
        val messageIndex: Int?,
    )

    const val ACTION_SHOW = "goster"
    /** Sobi ekranında açılacak örnek işlem (alistirma_ver). */
    const val ACTION_PRACTICE = "alistirma"
    /** Abaküste bir sütunu kısa süre parlat (sutunu_goster). */
    const val ACTION_POINT = "isaret"
    /** Yalnızca Sobi sekmesinde, sohbete düğme olarak: Sobi'nin testi, kredi paketleri, Pro. */
    const val ACTION_TEST = "test"
    const val ACTION_CREDITS = "kredi"
    const val ACTION_PRO = "pro"
    /** Bölümün ders haritasına giden düğme (derse_git). */
    const val ACTION_LESSON = "ders"
    /** Sobi sekmesinin "Krediler nasıl çalışır?" balonu: sunucu modelsiz, ücretsiz cevaplıyor. */
    const val KEY_CREDITS = "krediler"

    /** Sobi sekmesinin sohbet listesindeki satır (`sobiSessions`). */
    data class SessionInfo(val id: String, val title: String, val updatedAt: Long)

    /** Yeniden açılan sohbetin satırı. [index]: sunucudaki sırası (beğeni buna bağlı). */
    data class SessionMessage(
        val fromChild: Boolean,
        val text: String,
        val index: Int,
        val rating: Int,
        val reported: Boolean,
        val buttons: List<Action>,
    )

    fun listSessions(onResult: (List<SessionInfo>) -> Unit, onFailure: (Exception) -> Unit) {
        sessionsCall(hashMapOf("action" to "list"), { d ->
            onResult((d["sessions"] as? List<*>).orEmpty().mapNotNull { raw ->
                val m = raw as? Map<*, *> ?: return@mapNotNull null
                val id = m["id"] as? String ?: return@mapNotNull null
                SessionInfo(id, m["title"] as? String ?: "", (m["updatedAt"] as? Number)?.toLong() ?: 0L)
            })
        }, onFailure)
    }

    fun loadSession(sessionId: String, onResult: (List<SessionMessage>) -> Unit, onFailure: (Exception) -> Unit) {
        sessionsCall(hashMapOf("action" to "get", "sessionId" to sessionId), { d ->
            onResult((d["messages"] as? List<*>).orEmpty().mapNotNull { raw ->
                val m = raw as? Map<*, *> ?: return@mapNotNull null
                SessionMessage(
                    fromChild = m["from"] != "sobi",
                    text = m["text"] as? String ?: "",
                    index = (m["index"] as? Number)?.toInt() ?: return@mapNotNull null,
                    rating = (m["rating"] as? Number)?.toInt() ?: 0,
                    reported = m["reported"] == true,
                    buttons = (m["buttons"] as? List<*>).orEmpty().mapNotNull(::parseAction),
                )
            })
        }, onFailure)
    }

    fun deleteSession(sessionId: String, onDone: () -> Unit, onFailure: (Exception) -> Unit) {
        sessionsCall(hashMapOf("action" to "delete", "sessionId" to sessionId), { onDone() }, onFailure)
    }

    private fun sessionsCall(payload: Map<String, Any>, onResult: (Map<*, *>) -> Unit, onFailure: (Exception) -> Unit) {
        FirebaseFunctions.getInstance()
            .getHttpsCallable("sobiSessions")
            .call(payload)
            .addOnSuccessListener { result -> onResult(result.data as? Map<*, *> ?: emptyMap<Any, Any>()) }
            .addOnFailureListener { e ->
                Log.w(TAG, "sobiSessions başarısız", e)
                onFailure(e)
            }
    }

    /**
     * [suggestion]: çocuk bir öneri balonuna dokunduysa balonun anahtarı. Anlatım adımında hazır
     * açıklama varsa sunucu "Bunu anlamadım" ve "Neden böyle?"ye modeli çağırmadan onu döner.
     */
    fun send(
        sessionId: String?,
        message: String,
        context: SobiContext,
        onResult: (Reply) -> Unit,
        onFailure: (Exception) -> Unit,
        suggestion: String? = null,
    ) {
        val payload = hashMapOf<String, Any>(
            "message" to message,
            "context" to context.toPayload(),
        )
        if (!sessionId.isNullOrBlank()) payload["sessionId"] = sessionId
        if (suggestion != null) payload["suggestion"] = suggestion
        call(payload, onResult, onFailure)
    }

    /**
     * Sobi sekmesindeki genel sohbet (`mode: 'genel'`): adım bağlamı gönderilmez; sunucu planı,
     * bakiyeyi ve kural başarısını kendisi okuyor. Ekonomi ders içi sohbetle aynı. [suggestion]:
     * başlangıç balonunun anahtarı ([KEY_CREDITS] sunucuda hazır cevap, ücretsiz).
     */
    fun sendGeneral(
        sessionId: String?,
        message: String,
        onResult: (Reply) -> Unit,
        onFailure: (Exception) -> Unit,
        suggestion: String? = null,
    ) {
        val payload = hashMapOf<String, Any>("message" to message, "mode" to "genel")
        if (!sessionId.isNullOrBlank()) payload["sessionId"] = sessionId
        if (suggestion != null) payload["suggestion"] = suggestion
        call(payload, onResult, onFailure)
    }

    private fun call(payload: Map<String, Any>, onResult: (Reply) -> Unit, onFailure: (Exception) -> Unit) {
        // Uygulamanın dili: "en" ise sunucu talimatın sonuna İngilizce dil bloğu ekliyor (sobi/callable.js).
        val withLang = payload + ("lang" to AppStrings.context().let(AppLanguage::current))
        FirebaseFunctions.getInstance()
            .getHttpsCallable("sobiChat")
            .call(withLang)
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
                        credits = (data["credits"] as? Number)?.toInt(),
                        messageIndex = (data["messageIndex"] as? Number)?.toInt(),
                    )
                )
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "sobiChat başarısız", e)
                onFailure(e)
            }
    }

    /**
     * Beğen / beğenme (`sobiFeedback`): [value] 1, -1 ya da 0 (geri al). Sonuç çocuğa gösterilmiyor;
     * başarısız olursa yalnızca günlüğe yazılır (oy kaybı çocuğun işini bozmaz).
     */
    fun sendFeedback(sessionId: String, messageIndex: Int, value: Int, comment: String? = null) {
        val payload = hashMapOf<String, Any>("sessionId" to sessionId, "messageIndex" to messageIndex, "value" to value)
        if (!comment.isNullOrBlank()) payload["comment"] = comment
        FirebaseFunctions.getInstance()
            .getHttpsCallable("sobiFeedback")
            .call(payload)
            .addOnFailureListener { e -> Log.w(TAG, "sobiFeedback başarısız", e) }
    }

    /** Bildir: uygunsuz ya da zararlı cevap (sunucu hemen günlüğe yazar, ayrı kayıt tutar). */
    fun sendReport(sessionId: String, messageIndex: Int, comment: String?) {
        val payload = hashMapOf<String, Any>("sessionId" to sessionId, "messageIndex" to messageIndex, "report" to true)
        if (!comment.isNullOrBlank()) payload["comment"] = comment
        FirebaseFunctions.getInstance()
            .getHttpsCallable("sobiFeedback")
            .call(payload)
            .addOnFailureListener { e -> Log.w(TAG, "sobiFeedback (bildir) başarısız", e) }
    }

    /**
     * Kural motorunun yazdığı mesaja oy ([value] 1, -1, 0) ya da bildirim: sohbet kaydı olmadığı için
     * metin de gider (functions/sobi/feedback.js, "YEREL MESAJ"). [screen]: "analiz" gibi.
     */
    fun sendLocalFeedback(localId: String, text: String, value: Int?, report: Boolean, comment: String?, screen: String, question: String?) {
        val payload = hashMapOf<String, Any>("local" to true, "localId" to localId, "text" to text.take(1000), "screen" to screen)
        if (value != null && !report) payload["value"] = value
        if (report) payload["report"] = true
        if (!comment.isNullOrBlank()) payload["comment"] = comment
        if (!question.isNullOrBlank()) payload["question"] = question
        FirebaseFunctions.getInstance()
            .getHttpsCallable("sobiFeedback")
            .call(payload)
            .addOnFailureListener { e -> Log.w(TAG, "sobiFeedback (yerel) başarısız", e) }
    }

    private fun parseAction(raw: Any?): Action? {
        val m = raw as? Map<*, *> ?: return null
        val kind = m["tur"] as? String ?: return null
        if (kind == ACTION_TEST || kind == ACTION_CREDITS || kind == ACTION_PRO) return Action(kind, 0, "", 0, 0)
        if (kind == ACTION_LESSON) {
            val part = (m["bolum"] as? Number)?.toInt()?.takeIf { it in 1..6 } ?: return null
            return Action(kind, 0, "", 0, 0, part = part)
        }
        if (kind == ACTION_POINT) {
            val col = (m["sutun"] as? Number)?.toInt()?.takeIf { it in 0..4 } ?: return null
            return Action(kind, 0, "", 0, 0, col)
        }
        val base = (m["baslangic"] as? Number)?.toInt() ?: return null
        val op = m["islem"] as? String ?: return null
        val number = (m["sayi"] as? Number)?.toInt() ?: return null
        val result = (m["sonuc"] as? Number)?.toInt() ?: return null
        return Action(kind, base, op, number, result)
    }

    /** Sobi bakiyesi (`sobiBalance`): Sobi ekranı, mağaza ve veli paneli. */
    data class Balance(
        val plan: String,
        val freeLimit: Int,
        val freeLeft: Int,
        val credits: Int,
        /** Bu ay Sobi'ye gönderilen (yapay zekâ) mesaj. */
        val monthCount: Int,
        val monthCreditsUsed: Int,
        /** Ek testin anahtar bedeli; Pro'da 0. */
        val extraTestKeys: Int,
    )

    fun balance(onResult: (Balance) -> Unit, onFailure: (Exception) -> Unit = {}) {
        FirebaseFunctions.getInstance()
            .getHttpsCallable("sobiBalance")
            .call()
            .addOnSuccessListener { result ->
                val d = result.data as? Map<*, *>
                fun n(k: String) = (d?.get(k) as? Number)?.toInt() ?: 0
                onResult(Balance(d?.get("plan") as? String ?: "Free", n("freeLimit"), n("freeLeft"), n("credits"),
                    n("monthCount"), n("monthCreditsUsed"), n("extraTestKeys")))
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "sobiBalance başarısız", e)
                onFailure(e)
            }
    }

    /**
     * Ek testi başlatmadan önce (`startSobiExtraTest`): Pro değilse 3 anahtar düşer. [testIndex]: bugün
     * bitirilen test sayısı; aynı sıra iki kez ödenmez (yarıda bırakılıp yeniden açılan test).
     * [onResult]: düşen anahtar (0 = ücretsiz) ve kalan anahtar.
     */
    fun startExtraTest(testIndex: Int, onResult: (cost: Int, keys: Int) -> Unit, onFailure: (Exception) -> Unit) {
        FirebaseFunctions.getInstance()
            .getHttpsCallable("startSobiExtraTest")
            .call(hashMapOf("testIndex" to testIndex))
            .addOnSuccessListener { result ->
                val d = result.data as? Map<*, *>
                onResult((d?.get("cost") as? Number)?.toInt() ?: 0, (d?.get("keys") as? Number)?.toInt() ?: 0)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "startSobiExtraTest başarısız", e)
                onFailure(e)
            }
    }

    /**
     * Sobi ekranında yeni örnek ("Başka bir örnek", "Daha zor bir örnek"): 1 ücretsiz hak ya da 1 kredi
     * (`startSobiExample`). [onResult]: kalan ücretsiz hak, kredi ve nasıl ödendiği ("free" / "credit").
     * Hak ve kredi bitince [isDailyLimit].
     */
    fun startExample(onResult: (freeLeft: Int, credits: Int, paidWith: String) -> Unit, onFailure: (Exception) -> Unit) {
        FirebaseFunctions.getInstance()
            .getHttpsCallable("startSobiExample")
            .call()
            .addOnSuccessListener { result ->
                val d = result.data as? Map<*, *>
                onResult(
                    (d?.get("remainingToday") as? Number)?.toInt() ?: 0,
                    (d?.get("credits") as? Number)?.toInt() ?: 0,
                    d?.get("paidWith") as? String ?: "none",
                )
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "startSobiExample başarısız", e)
                onFailure(e)
            }
    }

    /** Günlük ücretsiz hak da kredi de bitti (`resource-exhausted`). */
    fun isDailyLimit(e: Exception): Boolean =
        (e as? FirebaseFunctionsException)?.code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED

    /** Ek test için anahtar yetmiyor (`failed-precondition`). */
    fun isNotEnoughKeys(e: Exception): Boolean =
        (e as? FirebaseFunctionsException)?.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION

    /** Hesap kısıtlı (`permission-denied`); askQuestion kısıtıyla aynı kural. */
    fun isRestricted(e: Exception): Boolean =
        (e as? FirebaseFunctionsException)?.code == FirebaseFunctionsException.Code.PERMISSION_DENIED
}

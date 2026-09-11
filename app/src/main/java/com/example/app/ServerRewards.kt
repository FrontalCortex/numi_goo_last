package com.example.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/**
 * Sunucuda çekilen ödüller.
 *
 * Sandık ve kristal ödüllerinin zarı artık istemcide atılmaz: `openChest` /
 * `openCrystalReward` hem sonucu belirler hem bakiyeyi yazar. İstemci yalnızca dönen
 * sonucu oynatır. Böylece istemci kendi ödül miktarını seçemez.
 *
 * Not: Bu çağrılar ödülü VERİR. Kullanıcıya gösterilmeden önce çağrılırlar; ekran
 * kapansa bile ödül kaybolmaz.
 */
object ServerRewards {

    /** [openChest] sonucu. [rarityPath] kullanıcının tıklamalarında sırayla gösterilir. */
    data class ChestOutcome(
        val rarityPath: List<String>,
        val finalRarity: String,
        val rewardType: String,
        val rewardAmount: Int,
    )

    /** [openCrystal] sonucu. [videoName] oynatılacak kristal videosudur. */
    data class CrystalOutcome(
        val videoName: String,
        val rewardType: String,
        val rewardAmount: Int,
    )

    private const val TAG = "ServerRewards"

    /** SSV callback'i gecikirse yeniden deneme sayisi ve araligi (10 x 1.5 sn = ~15 sn). */
    private const val AD_VERIFY_MAX_ATTEMPTS = 10
    private const val AD_VERIFY_RETRY_DELAY_MS = 1500L

    // Sandık ekranı (NewChestFragment) açılmadan hemen önce başlatılan istek burada bekler.
    // İlk açılıştaki Cloud Functions "soğuk başlangıç" gecikmesini (auth token + bağlantı +
    // fonksiyon kapsayıcısı ısınması, toplamda 1-3 sn sürebiliyor) ders/geçiş animasyonlarının
    // arkasına gizlemek için var — bkz. [prefetchChest]. Aynı sandık için openChest'in İKİNCİ
    // KEZ ağa gitmemesi kritik: bir kez daha çağrılırsa aynı adNonce ikinci kez tüketilmeye
    // çalışılır ve sunucu "already-exists" ile reddeder.
    private var chestPrefetchInFlight = false
    private var chestPrefetchResult: Result<ChestOutcome>? = null
    private var chestPrefetchWaiter: ((Result<ChestOutcome>) -> Unit)? = null

    /**
     * Sandık ekranı gösterilmeden önce (ör. geçiş animasyonu oynarken) çağrılır: [openChest]
     * isteğini hemen başlatır. Aynı parametrelerle daha sonra [openChest] çağrıldığında ağa
     * tekrar gidilmez — bu çağrının sonucu (bitmişse anında, bitmemişse hazır olunca) kullanılır.
     *
     * Çağırmak isteğe bağlıdır: hiç çağrılmazsa [openChest] eskisi gibi kendi isteğini başlatır.
     */
    fun prefetchChest(startRarity: String, adNonce: String? = null) {
        if (chestPrefetchInFlight) return
        chestPrefetchInFlight = true
        chestPrefetchResult = null
        openChestAttempt(
            startRarity, adNonce, attempt = 0,
            onResult = { resolveChestPrefetch(Result.success(it)) },
            onFailure = { resolveChestPrefetch(Result.failure(it)) },
        )
    }

    private fun resolveChestPrefetch(result: Result<ChestOutcome>) {
        chestPrefetchInFlight = false
        val waiter = chestPrefetchWaiter
        if (waiter != null) {
            chestPrefetchWaiter = null
            waiter(result)
        } else {
            chestPrefetchResult = result
        }
    }

    /**
     * @param adNonce Reklamla kazanilan sandiklarda [AdManager]'in urettigi nonce. Sunucu bu
     *   nonce ile AdMob SSV dogrulamasindan gelen tek kullanimlik hakki bozdurur.
     *
     * AdMob'un SSV callback'i reklam bittikten kisa sure sonra gelir; istemci daha once
     * cagirirsa sunucu UNAVAILABLE doner ve burada birkac kez yeniden denenir.
     */
    fun openChest(
        startRarity: String,
        adNonce: String? = null,
        onResult: (ChestOutcome) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        chestPrefetchResult?.let { result ->
            chestPrefetchResult = null
            result.fold(onSuccess = onResult, onFailure = { onFailure(it as? Exception ?: Exception(it)) })
            return
        }
        if (chestPrefetchInFlight) {
            chestPrefetchWaiter = { result -> result.fold(onSuccess = onResult, onFailure = { onFailure(it as? Exception ?: Exception(it)) }) }
            return
        }
        openChestAttempt(startRarity, adNonce, attempt = 0, onResult = onResult, onFailure = onFailure)
    }

    private fun openChestAttempt(
        startRarity: String,
        adNonce: String?,
        attempt: Int,
        onResult: (ChestOutcome) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        val payload = hashMapOf<String, Any>("startRarity" to startRarity)
        if (adNonce != null) payload["adNonce"] = adNonce

        FirebaseFunctions.getInstance()
            .getHttpsCallable("openChest")
            .call(payload)
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                if (data == null) {
                    onFailure(IllegalStateException("openChest boş yanıt döndü"))
                    return@addOnSuccessListener
                }
                val path = (data["rarityPath"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                onResult(
                    ChestOutcome(
                        rarityPath = path,
                        finalRarity = data["finalRarity"] as? String ?: startRarity,
                        rewardType = data["rewardType"] as? String ?: "GOLD",
                        rewardAmount = (data["rewardAmount"] as? Number)?.toInt() ?: 0,
                    )
                )
            }
            .addOnFailureListener { e ->
                // SSV callback'i henuz gelmemis olabilir; kisa araliklarla yeniden dene.
                val notVerifiedYet = (e as? FirebaseFunctionsException)?.code ==
                    FirebaseFunctionsException.Code.UNAVAILABLE
                if (notVerifiedYet && attempt < AD_VERIFY_MAX_ATTEMPTS) {
                    Handler(Looper.getMainLooper()).postDelayed(
                        { openChestAttempt(startRarity, adNonce, attempt + 1, onResult, onFailure) },
                        AD_VERIFY_RETRY_DELAY_MS,
                    )
                    return@addOnFailureListener
                }
                Log.e(TAG, "openChest başarısız", e)
                onFailure(e)
            }
    }

    fun openCrystal(
        onResult: (CrystalOutcome) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        FirebaseFunctions.getInstance()
            .getHttpsCallable("openCrystalReward")
            .call(hashMapOf<String, Any>())
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                if (data == null) {
                    onFailure(IllegalStateException("openCrystalReward boş yanıt döndü"))
                    return@addOnSuccessListener
                }
                onResult(
                    CrystalOutcome(
                        videoName = data["videoName"] as? String ?: ChestCrystalPolicy.FALLBACK_VIDEO,
                        rewardType = data["rewardType"] as? String ?: "GOLD",
                        rewardAmount = (data["rewardAmount"] as? Number)?.toInt() ?: 0,
                    )
                )
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "openCrystalReward başarısız", e)
                onFailure(e)
            }
    }
}

// Kaynak: ServerRewards.kt (tamamı: sonuç modelleri, callable adları, istek gövdesi, yanıt ayrıştırma,
// SSV yeniden deneme kuralı, sandık ön-istek (prefetch) durum makinesi).
// Farklar:
//  - Android'de `object` idi; ön-istek durumu burada [ServerRewards.ChestPrefetcher] sınıfında, ağ
//    çağrısının kendisi (FirebaseFunctions + Handler ile gecikmeli yeniden deneme) [ChestCaller]
//    arayüzünün arkasında. Tek bir paylaşılan [ChestPrefetcher] örneği kullanılmalıdır (Android'deki
//    `object` durumu gibi).
//  - `FirebaseFunctionsException.Code.UNAVAILABLE` denetimi → `isUnavailable` parametresi.
//  - Log çağrıları kaldırıldı.
// Android'deki hâliyle korunan not: `prefetchChest` sürerken farklı parametrelerle `openChest`
// çağrılırsa parametreler karşılaştırılmaz; bekleyen ön-isteğin sonucu döner.
package com.sorobit.shared.oyun

/**
 * Sunucuda çekilen ödüller.
 *
 * Sandık ve kristal ödüllerinin zarı istemcide atılmaz: `openChest` / `openCrystalReward` hem sonucu
 * belirler hem bakiyeyi yazar. İstemci yalnızca dönen sonucu oynatır.
 *
 * Not: Bu çağrılar ödülü VERİR. Kullanıcıya gösterilmeden önce çağrılırlar; ekran kapansa bile
 * ödül kaybolmaz.
 */
object ServerRewards {

    /** `openChest` sonucu. [rarityPath] kullanıcının tıklamalarında sırayla gösterilir. */
    data class ChestOutcome(
        val rarityPath: List<String>,
        val finalRarity: String,
        val rewardType: String,
        val rewardAmount: Int,
    )

    /** `openCrystalReward` sonucu. [videoName] oynatılacak kristal videosudur. */
    data class CrystalOutcome(
        val videoName: String,
        val rewardType: String,
        val rewardAmount: Int,
    )

    // ── Callable fonksiyon adları ───────────────────────────────────────────
    const val FUNCTION_OPEN_CHEST = "openChest"
    const val FUNCTION_OPEN_CRYSTAL_REWARD = "openCrystalReward"

    // ── İstek / yanıt alanları ──────────────────────────────────────────────
    const val PARAM_START_RARITY = "startRarity"
    const val PARAM_AD_NONCE = "adNonce"
    const val RESULT_RARITY_PATH = "rarityPath"
    const val RESULT_FINAL_RARITY = "finalRarity"
    const val RESULT_REWARD_TYPE = "rewardType"
    const val RESULT_REWARD_AMOUNT = "rewardAmount"
    const val RESULT_VIDEO_NAME = "videoName"

    const val REWARD_TYPE_GOLD = "GOLD"
    const val REWARD_TYPE_KEY = "KEY"

    const val ERROR_OPEN_CHEST_EMPTY = "openChest boş yanıt döndü"
    const val ERROR_OPEN_CRYSTAL_EMPTY = "openCrystalReward boş yanıt döndü"

    /** SSV geri çağrısı gecikirse yeniden deneme sayısı ve aralığı. */
    const val AD_VERIFY_MAX_ATTEMPTS = 4
    const val AD_VERIFY_RETRY_DELAY_MS = 1500L

    /** `openChest` istek gövdesi; [adNonce] yalnızca reklamla kazanılan sandıklarda eklenir. */
    fun openChestPayload(startRarity: String, adNonce: String? = null): Map<String, Any> {
        val payload = hashMapOf<String, Any>(PARAM_START_RARITY to startRarity)
        if (adNonce != null) payload[PARAM_AD_NONCE] = adNonce
        return payload
    }

    /** `openCrystalReward` istek gövdesi (boş). */
    fun openCrystalPayload(): Map<String, Any> = hashMapOf()

    /** `openChest` yanıtı; yanıt Map değilse null (Android: "boş yanıt" hatası). */
    fun parseChestOutcome(data: Any?, startRarity: String): ChestOutcome? {
        val map = data as? Map<*, *> ?: return null
        val path = (map[RESULT_RARITY_PATH] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        return ChestOutcome(
            rarityPath = path,
            finalRarity = map[RESULT_FINAL_RARITY] as? String ?: startRarity,
            rewardType = map[RESULT_REWARD_TYPE] as? String ?: REWARD_TYPE_GOLD,
            rewardAmount = (map[RESULT_REWARD_AMOUNT] as? Number)?.toInt() ?: 0,
        )
    }

    /** `openCrystalReward` yanıtı; yanıt Map değilse null (Android: "boş yanıt" hatası). */
    fun parseCrystalOutcome(data: Any?): CrystalOutcome? {
        val map = data as? Map<*, *> ?: return null
        return CrystalOutcome(
            videoName = map[RESULT_VIDEO_NAME] as? String ?: ChestCrystalPolicy.FALLBACK_VIDEO,
            rewardType = map[RESULT_REWARD_TYPE] as? String ?: REWARD_TYPE_GOLD,
            rewardAmount = (map[RESULT_REWARD_AMOUNT] as? Number)?.toInt() ?: 0,
        )
    }

    /**
     * `openChest` hata dalı: SSV geri çağrısı henüz gelmemiş olabilir (sunucu UNAVAILABLE döner);
     * [attempt] (0 tabanlı) sınırın altındaysa [AD_VERIFY_RETRY_DELAY_MS] sonra `attempt + 1` ile
     * yeniden denenir. Toplam en çok 1 + [AD_VERIFY_MAX_ATTEMPTS] istek yapılır.
     */
    fun shouldRetryOpenChest(isUnavailable: Boolean, attempt: Int): Boolean =
        isUnavailable && attempt < AD_VERIFY_MAX_ATTEMPTS

    /**
     * `openChestAttempt(startRarity, adNonce, attempt = 0, …)` karşılığı: isteği yapar, gerekirse
     * [shouldRetryOpenChest] kuralıyla yeniden dener ve sonunda tam olarak bir geri çağrıyı çalıştırır.
     */
    fun interface ChestCaller {
        fun openChestAttempt(
            startRarity: String,
            adNonce: String?,
            onResult: (ChestOutcome) -> Unit,
            onFailure: (Exception) -> Unit,
        )
    }

    /**
     * Sandık ekranı açılmadan hemen önce başlatılan isteğin beklediği yer.
     *
     * İlk açılıştaki Cloud Functions "soğuk başlangıç" gecikmesini ders/geçiş animasyonlarının arkasına
     * gizlemek için var. Aynı sandık için openChest'in İKİNCİ KEZ ağa gitmemesi kritik: bir kez daha
     * çağrılırsa aynı adNonce ikinci kez tüketilmeye çalışılır ve sunucu "already-exists" ile reddeder.
     */
    class ChestPrefetcher(private val caller: ChestCaller) {
        private var chestPrefetchInFlight = false
        private var chestPrefetchResult: Result<ChestOutcome>? = null
        private var chestPrefetchWaiter: ((Result<ChestOutcome>) -> Unit)? = null

        /**
         * Sandık ekranı gösterilmeden önce çağrılır: [openChest] isteğini hemen başlatır. Daha sonra
         * [openChest] çağrıldığında ağa tekrar gidilmez — bu çağrının sonucu (bitmişse anında,
         * bitmemişse hazır olunca) kullanılır.
         *
         * Çağırmak isteğe bağlıdır: hiç çağrılmazsa [openChest] kendi isteğini başlatır.
         */
        fun prefetchChest(startRarity: String, adNonce: String? = null) {
            if (chestPrefetchInFlight) return
            chestPrefetchInFlight = true
            chestPrefetchResult = null
            caller.openChestAttempt(
                startRarity, adNonce,
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
         * @param adNonce Reklamla kazanılan sandıklarda reklam yöneticisinin ürettiği nonce. Sunucu bu
         *   nonce ile AdMob SSV doğrulamasından gelen tek kullanımlık hakkı bozdurur.
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
                chestPrefetchWaiter = { result ->
                    result.fold(onSuccess = onResult, onFailure = { onFailure(it as? Exception ?: Exception(it)) })
                }
                return
            }
            caller.openChestAttempt(startRarity, adNonce, onResult = onResult, onFailure = onFailure)
        }
    }
}

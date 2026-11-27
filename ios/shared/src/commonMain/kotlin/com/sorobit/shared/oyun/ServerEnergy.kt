// Kaynak: ServerEnergy.kt (callable adları, istek gövdeleri, yanıt ayrıştırma) +
// EnergyManager.requestSpendOnServer içindeki `spendEnergy` isteği.
// Farklar: FirebaseFunctions çağrısının kendisi platformda; burada yalnızca fonksiyon adları,
// istek gövdeleri ve yanıt ayrıştırma var. Log çağrıları kaldırıldı.
package com.sorobit.shared.oyun

/**
 * Sunucu taraflı can (enerji) işlemleri.
 *
 * `users/{uid}.energy_full_time` firestore.rules ile istemci yazımına kapalıdır; can
 * yalnızca buradaki fonksiyonlar üzerinden değişir. Böylece reklamı izlemeden can
 * kazanmak veya alanı doğrudan yazıp sınırsız can vermek mümkün değildir.
 */
object ServerEnergy {
    // ── Callable fonksiyon adları ───────────────────────────────────────────
    const val FUNCTION_CLAIM_AD_ENERGY = "claimAdEnergy"
    const val FUNCTION_BUY_ENERGY_WITH_KEYS = "buyEnergyWithKeys"
    const val FUNCTION_SPEND_ENERGY = "spendEnergy"

    // ── İstek / yanıt alanları ──────────────────────────────────────────────
    const val PARAM_AD_NONCE = "adNonce"
    const val PARAM_AMOUNT = "amount"
    const val RESULT_ENERGY_FULL_TIME = "energyFullTime"
    const val RESULT_KEYS = "keys"

    /**
     * Android (güncel): `claimAdEnergy` SSV geri araması gecikirse sunucu UNAVAILABLE döner; en çok
     * [AD_VERIFY_MAX_ATTEMPTS] kez [AD_VERIFY_RETRY_DELAY_MS] arayla yeniden denenir ([ServerRewards] ile aynı bütçe).
     */
    const val AD_VERIFY_MAX_ATTEMPTS = 10
    const val AD_VERIFY_RETRY_DELAY_MS = 1500L

    /** `claimAdEnergy` hata dalı: [attempt] (0 tabanlı) bütçenin altında ve hata UNAVAILABLE ise yeniden dene. */
    fun shouldRetryClaimAdEnergy(isUnavailable: Boolean, attempt: Int): Boolean =
        isUnavailable && attempt < AD_VERIFY_MAX_ATTEMPTS

    const val ERROR_CLAIM_AD_ENERGY_INVALID = "claimAdEnergy geçersiz yanıt döndü"
    const val ERROR_BUY_ENERGY_INVALID = "buyEnergyWithKeys geçersiz yanıt döndü"

    /** [buyWithKeys] yanıtı: yetkili `energy_full_time` ve kalan anahtar bakiyesi. */
    data class BuyWithKeysResult(val fullTime: Long, val keys: Int)

    /**
     * Reklam izleyerek 1 can kazanma isteği.
     * @param adNonce reklam yöneticisinin ürettiği nonce; sunucu bunu AdMob SSV doğrulamasından
     *   gelen tek kullanımlık hakla eşleştirir.
     */
    fun claimAdEnergyPayload(adNonce: String): Map<String, Any> = hashMapOf(PARAM_AD_NONCE to adNonce)

    /** Anahtar karşılığı 1 can satın alma isteği (gövde boş). */
    fun buyWithKeysPayload(): Map<String, Any> = hashMapOf()

    /** Ders başlatırken yapılan harcamanın sunucuya bildirimi. */
    fun spendEnergyPayload(amount: Int): Map<String, Any> = hashMapOf(PARAM_AMOUNT to amount)

    /** `claimAdEnergy` yanıtı → yetkili fullTime; geçersizse null (Android: onFailure). */
    fun parseClaimAdEnergyResult(data: Any?): Long? = readFullTime(data)

    /** `buyEnergyWithKeys` yanıtı; fullTime ya da keys eksikse null (Android: onFailure). */
    fun parseBuyWithKeysResult(data: Any?): BuyWithKeysResult? {
        val map = data as? Map<*, *>
        val fullTime = readFullTime(data)
        val keys = (map?.get(RESULT_KEYS) as? Number)?.toInt()
        return if (fullTime == null || keys == null) null else BuyWithKeysResult(fullTime, keys)
    }

    fun readFullTime(data: Any?): Long? =
        ((data as? Map<*, *>)?.get(RESULT_ENERGY_FULL_TIME) as? Number)?.toLong()
}

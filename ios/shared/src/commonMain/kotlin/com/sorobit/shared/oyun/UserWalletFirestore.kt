// Kaynak: UserWalletFirestore.kt (modeller, alan adları, varsayılanlar, yükleme/dinleme eşlemesi,
// `updateUserWallet` istek gövdesi ve yanıt ayrıştırma, yerel önbellek, eski altın geçişi).
// Farklar: Firestore / FirebaseFunctions çağrıları platformda; SharedPreferences ("app_prefs") →
// [OyunKeyValueStore]. `applyKeyDelta` / `applyCurrencyDelta` içindeki kullanılmayan `uid`
// parametresi taşınmadı (sunucu kimliği oturumdan alır).
package com.sorobit.shared.oyun

data class UserWallet(
    val keys: Int,
    val currency: Int,
    /**
     * Harcama çağrılarında sunucunun ürettiği tek kullanımlık geri alma jetonu.
     *
     * Bir harcamayı geri almak (iade etmek) yalnızca bu jetonla ve birebir aynı miktarla
     * mümkündür; böylece istemci "harcamadan geri alma" yapıp bakiye şişiremez.
     * Kredi çağrılarında ve önbellekten okumalarda null'dır.
     */
    val rollbackToken: String? = null,
)

/**
 * `updateUserWallet` Cloud Function'ına gönderilen gerekçeler.
 *
 * Sunucu, bakiyeyi ARTIRAN çağrılarda gerekçeyi kendi kataloğunda (functions/index.js →
 * `WALLET_CREDIT_RULES`) arar ve miktarı o gerekçenin üst sınırıyla karşılaştırır. Buradaki
 * sabitler ile oradaki anahtarlar birebir aynı kalmalıdır. Bakiyeyi AZALTAN çağrılarda gerekçe
 * yalnızca günlüğe yazılır.
 */
object WalletReason {
    // NOT: Sandık/kristal ödülleri için gerekçe YOKTUR ve eklenmemelidir. O ödüllerin zarı
    // sunucuda atılır ve bakiye `openChest` / `openCrystalReward` içinde yazılır
    // (bkz. [ServerRewards]).

    /**
     * Uygulama içi satın alma kaydı başarısız olduğunda harcamanın geri alınması.
     * Mağaza para iadesiyle ilgisi yoktur — o sunucuda işlenir ve bakiyeyi eksiye düşürebilir.
     */
    const val PURCHASE_ROLLBACK = "purchase_rollback"

    /** Kullanıcının kendi bakiyesinden harcaması. */
    const val SPEND = "spend"
}

/** `loadWallet` başarı dalının sonucu: cüzdan + (varsa) belgeye yazılacak eksik alanlar. */
data class WalletLoadResult(
    val wallet: UserWallet,
    /** Boş değilse `users/{uid}` belgesine `update(patch)` ile yazılır. */
    val patch: Map<String, Any>,
)

object UserWalletFirestore {
    const val COLLECTION_USERS = "users"
    const val FIELD_KEYS = "keys"
    const val FIELD_CURRENCY = "currency"
    const val DEFAULT_KEYS = 1
    const val DEFAULT_CURRENCY = 0

    const val PREFS_APP = "app_prefs"
    const val PREF_CURRENCY = "currency"
    const val PREF_CURRENCY_MIGRATED = "currency_migrated_to_firestore"

    // ── updateUserWallet callable ───────────────────────────────────────────
    const val FUNCTION_UPDATE_USER_WALLET = "updateUserWallet"
    const val PARAM_KEYS = "keys"
    const val PARAM_CURRENCY = "currency"
    const val PARAM_REASON = "reason"
    const val PARAM_ROLLBACK_TOKEN = "rollbackToken"

    fun registrationWalletFields(): Map<String, Any> = mapOf(
        FIELD_KEYS to DEFAULT_KEYS,
        FIELD_CURRENCY to DEFAULT_CURRENCY,
    )

    /**
     * `loadWallet` başarı dalı: belgedeki değerler ([docKeys] / [docCurrency]; alan yoksa null)
     * cüzdana çevrilir, eksik alanlar için yama üretilir ve sonuç yerelde önbelleğe alınır.
     */
    fun resolveLoadedWallet(prefs: OyunKeyValueStore, docKeys: Int?, docCurrency: Int?): WalletLoadResult {
        val patch = mutableMapOf<String, Any>()
        var keys = docKeys
        if (keys == null) {
            keys = DEFAULT_KEYS
            patch[FIELD_KEYS] = keys
        }
        var currency = docCurrency
        if (currency == null) {
            currency = resolveCurrencyForMigration(prefs)
            patch[FIELD_CURRENCY] = currency
        }
        cacheLocally(prefs, keys, currency)
        return WalletLoadResult(UserWallet(keys = keys, currency = currency), patch)
    }

    /** `loadWallet` hata dalı: önbellekteki değerler döner. */
    fun walletFromCache(prefs: OyunKeyValueStore): UserWallet =
        UserWallet(keys = getCachedKeys(prefs), currency = getCachedCurrency(prefs))

    /**
     * `listenToWallet`: anlık görüntü → cüzdan (önbelleğe de yazılır).
     * Hata / belge yok durumunda Android hiçbir şey yapmaz; çağıran bu fonksiyonu çağırmamalıdır.
     */
    fun walletFromSnapshot(prefs: OyunKeyValueStore, docKeys: Int?, docCurrency: Int?): UserWallet {
        val keys = docKeys ?: DEFAULT_KEYS
        val currency = docCurrency ?: resolveCurrencyForMigration(prefs)
        cacheLocally(prefs, keys, currency)
        return UserWallet(keys, currency)
    }

    /**
     * Anahtar bakiyesini [delta] kadar değiştiren istek gövdesi; [delta] 0 ise null (çağrı yapılmaz).
     *
     * @param reason [WalletReason] sabitlerinden biri. Pozitif [delta] için sunucu bu gerekçeyi
     *   kendi kataloğunda arar; tanımsız gerekçeyle ya da katalogdaki üst sınırın üzerinde bir
     *   miktarla yapılan artırma reddedilir.
     */
    fun applyKeyDeltaPayload(delta: Int, reason: String, rollbackToken: String? = null): Map<String, Any>? =
        applyDeltaPayload(keyDelta = delta, currencyDelta = 0, reason = reason, rollbackToken = rollbackToken)

    /** Altın bakiyesini [delta] kadar değiştiren istek gövdesi. Kurallar için bkz. [applyKeyDeltaPayload]. */
    fun applyCurrencyDeltaPayload(delta: Int, reason: String, rollbackToken: String? = null): Map<String, Any>? =
        applyDeltaPayload(keyDelta = 0, currencyDelta = delta, reason = reason, rollbackToken = rollbackToken)

    fun applyDeltaPayload(
        keyDelta: Int,
        currencyDelta: Int,
        reason: String,
        rollbackToken: String?,
    ): Map<String, Any>? {
        if (keyDelta == 0 && currencyDelta == 0) return null
        val data = hashMapOf<String, Any>(
            PARAM_KEYS to keyDelta,
            PARAM_CURRENCY to currencyDelta,
            PARAM_REASON to reason,
        )
        if (rollbackToken != null) data[PARAM_ROLLBACK_TOKEN] = rollbackToken
        return data
    }

    /**
     * `updateUserWallet` yanıtı → cüzdan (önbelleğe de yazılır). Yanıtta alan yoksa önbellekteki
     * değer kullanılır.
     */
    fun walletFromUpdateResult(prefs: OyunKeyValueStore, resultData: Any?): UserWallet {
        val map = resultData as? Map<*, *>
        val keys = (map?.get(PARAM_KEYS) as? Number)?.toInt() ?: getCachedKeys(prefs)
        val currency = (map?.get(PARAM_CURRENCY) as? Number)?.toInt() ?: getCachedCurrency(prefs)

        cacheLocally(prefs, keys, currency)
        return UserWallet(
            keys = keys,
            currency = currency,
            rollbackToken = map?.get(PARAM_ROLLBACK_TOKEN) as? String,
        )
    }

    fun getCachedKeys(prefs: OyunKeyValueStore): Int =
        prefs.getInt(FIELD_KEYS, DEFAULT_KEYS)

    fun getCachedCurrency(prefs: OyunKeyValueStore): Int =
        prefs.getInt(PREF_CURRENCY, DEFAULT_CURRENCY)

    internal fun cacheLocally(prefs: OyunKeyValueStore, keys: Int, currency: Int) {
        prefs.putInt(FIELD_KEYS, keys)
        prefs.putInt(PREF_CURRENCY, currency)
    }

    /**
     * Belgede altın alanı yoksa: daha önce geçiş yapılmadıysa yereldeki eski altın değeri bir kez
     * devralınır (bayrak kalkar); sonraki çağrılarda varsayılan (0) döner.
     */
    internal fun resolveCurrencyForMigration(prefs: OyunKeyValueStore): Int {
        if (!prefs.getBoolean(PREF_CURRENCY_MIGRATED, false)) {
            val legacy = prefs.getInt(PREF_CURRENCY, DEFAULT_CURRENCY)
            prefs.putBoolean(PREF_CURRENCY_MIGRATED, true)
            return legacy
        }
        return DEFAULT_CURRENCY
    }
}

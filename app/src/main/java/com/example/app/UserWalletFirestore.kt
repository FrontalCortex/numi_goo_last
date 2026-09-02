package com.example.app

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

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
    // (bkz. [ServerRewards]). Buraya bir "ödül" gerekçesi eklemek, istemcinin kendi ödül
    // miktarını seçebildiği eski açığı geri getirir.

    /**
     * Uygulama içi satın alma kaydı başarısız olduğunda harcamanın geri alınması.
     * Google Play para iadesiyle ilgisi yoktur — o sunucuda `reconcileVoidedPurchases`
     * tarafından işlenir ve bakiyeyi eksiye düşürebilir.
     */
    const val PURCHASE_ROLLBACK = "purchase_rollback"

    /** Kullanıcının kendi bakiyesinden harcaması. */
    const val SPEND = "spend"
}

object UserWalletFirestore {
    const val FIELD_KEYS = "keys"
    const val FIELD_CURRENCY = "currency"
    const val DEFAULT_KEYS = 1
    const val DEFAULT_CURRENCY = 0

    private const val PREFS_APP = "app_prefs"
    private const val PREF_CURRENCY = "currency"
    private const val PREF_CURRENCY_MIGRATED = "currency_migrated_to_firestore"

    fun registrationWalletFields(): Map<String, Any> = mapOf(
        FIELD_KEYS to DEFAULT_KEYS,
        FIELD_CURRENCY to DEFAULT_CURRENCY,
    )

    fun loadWallet(
        context: Context,
        uid: String,
        onResult: (UserWallet) -> Unit,
        onFailure: ((Exception) -> Unit)? = null,
    ) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val patch = mutableMapOf<String, Any>()
                var keys = doc.getLong(FIELD_KEYS)?.toInt()
                if (keys == null) {
                    keys = DEFAULT_KEYS
                    patch[FIELD_KEYS] = keys
                }
                var currency = doc.getLong(FIELD_CURRENCY)?.toInt()
                if (currency == null) {
                    currency = resolveCurrencyForMigration(context)
                    patch[FIELD_CURRENCY] = currency
                }
                if (patch.isNotEmpty()) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .update(patch)
                }
                cacheLocally(context, keys, currency)
                onResult(UserWallet(keys = keys, currency = currency))
            }
            .addOnFailureListener { e ->
                onFailure?.invoke(e)
                val keys = context.getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE)
                    .getInt(FIELD_KEYS, DEFAULT_KEYS)
                val currency = getCachedCurrency(context)
                onResult(UserWallet(keys = keys, currency = currency))
            }
    }

    fun listenToWallet(
        context: Context,
        uid: String,
        onUpdate: (UserWallet) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val keys = snapshot.getLong(FIELD_KEYS)?.toInt() ?: DEFAULT_KEYS
                val currency = snapshot.getLong(FIELD_CURRENCY)?.toInt() ?: resolveCurrencyForMigration(context)
                cacheLocally(context, keys, currency)
                onUpdate(UserWallet(keys, currency))
            }
    }

    /**
     * Anahtar bakiyesini [delta] kadar değiştirir.
     *
     * @param reason [WalletReason] sabitlerinden biri. Pozitif [delta] için sunucu bu gerekçeyi
     *   kendi kataloğunda arar; tanımsız gerekçeyle ya da katalogdaki üst sınırın üzerinde bir
     *   miktarla yapılan artırma reddedilir.
     */
    fun applyKeyDelta(
        context: Context,
        uid: String,
        delta: Int,
        reason: String,
        rollbackToken: String? = null,
        onSuccess: ((UserWallet) -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null,
    ) = applyDelta(context, keyDelta = delta, currencyDelta = 0, reason = reason, rollbackToken = rollbackToken, onSuccess = onSuccess, onFailure = onFailure)

    /**
     * Altın bakiyesini [delta] kadar değiştirir. Gerekçe kuralları için bkz. [applyKeyDelta].
     */
    fun applyCurrencyDelta(
        context: Context,
        uid: String,
        delta: Int,
        reason: String,
        rollbackToken: String? = null,
        onSuccess: ((UserWallet) -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null,
    ) = applyDelta(context, keyDelta = 0, currencyDelta = delta, reason = reason, rollbackToken = rollbackToken, onSuccess = onSuccess, onFailure = onFailure)

    private fun applyDelta(
        context: Context,
        keyDelta: Int,
        currencyDelta: Int,
        reason: String,
        rollbackToken: String?,
        onSuccess: ((UserWallet) -> Unit)?,
        onFailure: ((Exception) -> Unit)?,
    ) {
        if (keyDelta == 0 && currencyDelta == 0) return
        val data = hashMapOf<String, Any>(
            "keys" to keyDelta,
            "currency" to currencyDelta,
            "reason" to reason,
        )
        if (rollbackToken != null) data["rollbackToken"] = rollbackToken

        com.google.firebase.functions.FirebaseFunctions.getInstance()
            .getHttpsCallable("updateUserWallet")
            .call(data)
            .addOnSuccessListener { result ->
                val resultData = result.data as? Map<*, *>
                val keys = (resultData?.get("keys") as? Number)?.toInt() ?: getCachedKeys(context)
                val currency = (resultData?.get("currency") as? Number)?.toInt() ?: getCachedCurrency(context)

                cacheLocally(context, keys, currency)
                onSuccess?.invoke(
                    UserWallet(
                        keys = keys,
                        currency = currency,
                        rollbackToken = resultData?.get("rollbackToken") as? String,
                    )
                )
            }
            .addOnFailureListener { e ->
                onFailure?.invoke(e)
            }
    }

    fun getCachedKeys(context: Context): Int =
        context.getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE)
            .getInt(FIELD_KEYS, DEFAULT_KEYS)

    fun getCachedCurrency(context: Context): Int =
        context.getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE)
            .getInt(PREF_CURRENCY, DEFAULT_CURRENCY)

    private fun cacheLocally(context: Context, keys: Int, currency: Int) {
        context.getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE)
            .edit()
            .putInt(FIELD_KEYS, keys)
            .putInt(PREF_CURRENCY, currency)
            .apply()
    }

    private fun resolveCurrencyForMigration(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(PREF_CURRENCY_MIGRATED, false)) {
            val legacy = prefs.getInt(PREF_CURRENCY, DEFAULT_CURRENCY)
            prefs.edit().putBoolean(PREF_CURRENCY_MIGRATED, true).apply()
            return legacy
        }
        return DEFAULT_CURRENCY
    }
}

package com.example.app

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

data class UserWallet(
    val keys: Int,
    val currency: Int,
)

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
                        .set(patch, SetOptions.merge())
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

    fun applyKeyDelta(
        context: Context,
        uid: String,
        delta: Int,
        onSuccess: ((UserWallet) -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null,
    ) {
        if (delta == 0) return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update(FIELD_KEYS, FieldValue.increment(delta.toLong()))
            .addOnSuccessListener {
                val cachedKeys = getCachedKeys(context) + delta
                val cachedCurrency = getCachedCurrency(context)
                cacheLocally(context, cachedKeys, cachedCurrency)
                onSuccess?.invoke(UserWallet(keys = cachedKeys, currency = cachedCurrency))
            }
            .addOnFailureListener { e ->
                onFailure?.invoke(e)
            }
    }

    fun applyCurrencyDelta(
        context: Context,
        uid: String,
        delta: Int,
        onSuccess: ((UserWallet) -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null,
    ) {
        if (delta == 0) return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update(FIELD_CURRENCY, FieldValue.increment(delta.toLong()))
            .addOnSuccessListener {
                val cachedKeys = getCachedKeys(context)
                val cachedCurrency = getCachedCurrency(context) + delta
                cacheLocally(context, cachedKeys, cachedCurrency)
                onSuccess?.invoke(UserWallet(keys = cachedKeys, currency = cachedCurrency))
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

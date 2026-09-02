package com.example.app

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions

/**
 * Sunucu taraflı can (enerji) işlemleri.
 *
 * `users/{uid}.energy_full_time` firestore.rules ile istemci yazımına kapalıdır; can
 * yalnızca buradaki fonksiyonlar üzerinden değişir. Böylece reklamı izlemeden can
 * kazanmak veya alanı doğrudan yazıp sınırsız can vermek mümkün değildir.
 *
 * Ders başlatırken yapılan harcama [EnergyManager] içinde iyimser olarak yerel
 * uygulanır ve arka planda `spendEnergy` ile sunucuya bildirilir.
 */
object ServerEnergy {

    private const val TAG = "ServerEnergy"

    /**
     * Reklam izleyerek 1 can kazanır.
     *
     * @param adNonce [AdManager]'ın ürettiği nonce; sunucu bunu AdMob SSV doğrulamasından
     *   gelen tek kullanımlık hakla eşleştirir.
     * @param onResult Sunucunun döndürdüğü yetkili `energy_full_time` değeri.
     */
    fun claimAdEnergy(
        adNonce: String,
        onResult: (fullTime: Long) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        FirebaseFunctions.getInstance()
            .getHttpsCallable("claimAdEnergy")
            .call(hashMapOf("adNonce" to adNonce))
            .addOnSuccessListener { result ->
                val fullTime = readFullTime(result.data)
                if (fullTime == null) {
                    onFailure(IllegalStateException("claimAdEnergy geçersiz yanıt döndü"))
                } else {
                    onResult(fullTime)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "claimAdEnergy başarısız", e)
                onFailure(e)
            }
    }

    /**
     * Anahtar karşılığı 1 can satın alır. Anahtar düşümü ve can eklemesi sunucuda aynı
     * transaction içinde yapılır.
     *
     * @param onResult Yetkili `energy_full_time` ve kalan anahtar bakiyesi.
     */
    fun buyWithKeys(
        onResult: (fullTime: Long, keys: Int) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        FirebaseFunctions.getInstance()
            .getHttpsCallable("buyEnergyWithKeys")
            .call(hashMapOf<String, Any>())
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val fullTime = readFullTime(result.data)
                val keys = (data?.get("keys") as? Number)?.toInt()
                if (fullTime == null || keys == null) {
                    onFailure(IllegalStateException("buyEnergyWithKeys geçersiz yanıt döndü"))
                } else {
                    onResult(fullTime, keys)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "buyEnergyWithKeys başarısız", e)
                onFailure(e)
            }
    }

    private fun readFullTime(data: Any?): Long? =
        ((data as? Map<*, *>)?.get("energyFullTime") as? Number)?.toLong()
}

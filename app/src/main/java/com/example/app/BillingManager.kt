package com.example.app

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

/**
 * Google Play Billing sarmalayıcısı.
 *
 * AKIŞ
 *   1. Play'den satın alma alınır → elimizde yalnızca `productId` + `purchaseToken` olur.
 *   2. Token sunucuya gönderilir; ödül miktarını SUNUCU belirler ve token'ı tek kullanımlık
 *      olarak işaretler. İstemci hiçbir zaman miktar göndermez.
 *   3. Sunucu onayladıktan SONRA ürün tüketilir (consumable) veya onaylanır (abonelik).
 *      Sıralama önemlidir: önce tüketilirse ve sunucu çağrısı düşerse kullanıcı parasını
 *      öder ama ödülü alamaz.
 *
 * Sunucu onayı gelmeden tüketilmeyen satın almalar Play tarafında durur; [refreshPurchases]
 * uygulama her açıldığında bunları yeniden gönderir.
 */
class BillingManager(context: Context) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    private val productDetails = mutableMapOf<String, ProductDetails>()
    private var reconnectAttempts = 0

    /** Fiyat etiketleri hazır olduğunda arayüzü tazelemek için. */
    var onPricesReady: (() -> Unit)? = null

    /** Sunucu ödülü verdikten sonra çağrılır (tüketilebilir ürünler). */
    var onPurchaseGranted: ((productId: String) -> Unit)? = null

    /** Abonelik sunucuda doğrulandıktan sonra çağrılır. */
    var onSubscriptionUpdated: (() -> Unit)? = null

    /** Kullanıcıya gösterilecek hata mesajı. İptal edilen satın almalarda çağrılmaz. */
    var onError: ((message: String) -> Unit)? = null

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    // ── Bağlantı ────────────────────────────────────────────────────────────

    fun start() {
        if (billingClient.isReady) {
            queryProductDetails()
            refreshPurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempts = 0
                    queryProductDetails()
                    refreshPurchases()
                } else {
                    Log.w(TAG, "Billing bağlantısı kurulamadı: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Üstel geri çekilme ile yeniden dene; sonsuz döngüye girme.
                if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return
                val delay = RECONNECT_BASE_DELAY_MS shl reconnectAttempts
                reconnectAttempts++
                main.postDelayed({ start() }, delay)
            }
        })
    }

    fun end() {
        onPricesReady = null
        onPurchaseGranted = null
        onSubscriptionUpdated = null
        onError = null
        if (billingClient.isReady) billingClient.endConnection()
    }

    // ── Ürün bilgileri / fiyatlar ───────────────────────────────────────────

    private fun queryProductDetails() {
        queryProductDetailsFor(BillingCatalog.consumables, BillingClient.ProductType.INAPP)
        queryProductDetailsFor(BillingCatalog.subscriptions, BillingClient.ProductType.SUBS)
    }

    private fun queryProductDetailsFor(productIds: List<String>, type: String) {
        val products = productIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(type)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()

        billingClient.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Ürün bilgisi alınamadı ($type): ${result.debugMessage}")
                return@queryProductDetailsAsync
            }
            // Billing 8'de sonuç ikiye ayrılır: gelenler ve Play'in bulamadıkları.
            val unfetched = details.unfetchedProductList
            if (unfetched.isNotEmpty()) {
                Log.w(TAG, "Play'de bulunamayan ürünler ($type): ${unfetched.joinToString { it.productId }}")
            }
            main.post {
                details.productDetailsList.forEach { productDetails[it.productId] = it }
                onPricesReady?.invoke()
            }
        }
    }

    /**
     * Play'in yerelleştirdiği fiyat etiketi (ör. "₺20,99"). Ürün bilgisi henüz gelmediyse
     * veya ürün Play Console'da tanımlı değilse null döner — arayüz bu durumda kendi
     * yedek metnini göstermelidir.
     */
    fun formattedPrice(productId: String): String? {
        val details = productDetails[productId] ?: return null
        details.oneTimePurchaseOfferDetails?.let { return it.formattedPrice }
        return details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.lastOrNull()
            ?.formattedPrice
    }

    fun isReady(): Boolean = billingClient.isReady

    // ── Satın alma başlatma ─────────────────────────────────────────────────

    fun launchPurchase(activity: Activity, productId: String) {
        val details = productDetails[productId]
        if (details == null) {
            onError?.invoke("Ürün şu anda alınamıyor. Daha sonra tekrar deneyin.")
            // Ürün bilgisi eksikse bağlantı kopmuş olabilir; sessizce tazele.
            start()
            return
        }

        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        // Abonelikler için teklif (offer) token'ı zorunludur.
        if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                onError?.invoke("Abonelik teklifi bulunamadı.")
                return
            }
            paramsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(paramsBuilder.build()))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "Satın alma ekranı açılamadı: ${result.debugMessage}")
            onError?.invoke("Satın alma başlatılamadı.")
        }
    }

    // ── Satın alma sonuçları ────────────────────────────────────────────────

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { processPurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit // sessiz
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Önceki satın alma tüketilmemiş olabilir; yeniden göndermeyi dene.
                refreshPurchases()
            }
            else -> {
                Log.w(TAG, "Satın alma hatası: ${result.responseCode} ${result.debugMessage}")
                onError?.invoke("Satın alma tamamlanamadı.")
            }
        }
    }

    /**
     * Uygulama açılışında ve gerektiğinde çağrılır: Play'de duran ama sunucuya işlenmemiş
     * satın almaları yeniden gönderir (ödeme sonrası uygulama çökerse bu akış kurtarır).
     */
    fun refreshPurchases() {
        if (!billingClient.isReady) return
        listOf(BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS).forEach { type ->
            val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
            billingClient.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.forEach { processPurchase(it) }
                }
            }
        }
    }

    private fun processPurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (FirebaseAuth.getInstance().currentUser == null) {
            // Oturum yoksa ödül hangi hesaba yazılacağı belirsiz; satın alma Play'de durur ve
            // giriş yapıldıktan sonraki refreshPurchases() ile tekrar denenir.
            Log.w(TAG, "Oturum açık değil, satın alma ertelendi")
            return
        }

        val productId = purchase.products.firstOrNull() ?: return
        val isSubscription = productId in BillingCatalog.subscriptions
        val callable = if (isSubscription) FN_SUBSCRIPTION else FN_PRODUCT

        FirebaseFunctions.getInstance()
            .getHttpsCallable(callable)
            .call(
                hashMapOf(
                    "productId" to productId,
                    "purchaseToken" to purchase.purchaseToken,
                )
            )
            .addOnSuccessListener {
                if (isSubscription) {
                    acknowledgeSubscription(purchase)
                    main.post { onSubscriptionUpdated?.invoke() }
                } else {
                    consumeProduct(purchase, productId)
                }
            }
            .addOnFailureListener { e ->
                // "already-exists": token zaten işlenmiş. Ödül verilmiş demektir; ürünü
                // tüketip Play tarafını temizlemek gerekir, yoksa kullanıcı tekrar satın alamaz.
                if (isAlreadyProcessed(e)) {
                    if (isSubscription) acknowledgeSubscription(purchase)
                    else consumeProduct(purchase, productId)
                    return@addOnFailureListener
                }
                Log.e(TAG, "Satın alma sunucuda işlenemedi: $productId", e)
                main.post { onError?.invoke("Satın alma doğrulanamadı. Uygulamayı tekrar açtığınızda denenecek.") }
            }
    }

    private fun isAlreadyProcessed(e: Exception): Boolean {
        val ffe = e as? com.google.firebase.functions.FirebaseFunctionsException ?: return false
        return ffe.code == com.google.firebase.functions.FirebaseFunctionsException.Code.ALREADY_EXISTS
    }

    private fun consumeProduct(purchase: Purchase, productId: String) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(params) { result, _ ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Ürün tüketilemedi: ${result.debugMessage}")
            }
            main.post { onPurchaseGranted?.invoke(productId) }
        }
    }

    private fun acknowledgeSubscription(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Abonelik onaylanamadı: ${result.debugMessage}")
            }
        }
    }

    companion object {
        private const val TAG = "BillingManager"
        private const val FN_PRODUCT = "redeemGooglePlayPurchase"
        private const val FN_SUBSCRIPTION = "redeemGooglePlaySubscription"
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_BASE_DELAY_MS = 1000L
    }
}

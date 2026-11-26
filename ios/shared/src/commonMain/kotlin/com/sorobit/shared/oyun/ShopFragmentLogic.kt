// Kaynak (Android):
//  - app/src/main/java/com/example/app/ShopFragment.kt
//      (CARD_PRODUCTS, CARD_PRICE_LABELS, applyStorePrices, buyLifeWithKeys, updateEnergyUi,
//       animatePurchasedItem, LIFE_KEY_COST, PURCHASE_ANIMATION_ITEM_COUNT, Toast metinleri)
//  - app/src/main/res/layout/fragment_shop.xml (kart görselleri, miktar etiketleri, yedek fiyatlar)
//  - app/src/main/java/com/example/app/BillingManager.kt (hata metinleri)
//
// Mağaza ekranının görünümden bağımsız kararları. Ödeme (Play Billing / StoreKit), reklam ve
// sunucu çağrıları platform katmanındadır.
//
// Android'den farklar:
//  - Kart → ürün eşlemesi görünüm kimliği yerine [PackCard] listesidir.
//  - `String.format("%02d:%02d")` → [ikiHane].
//  - [productKind] Android'de yoktur: [BillingCatalog] listelerinden türetilir (StoreKit'te ürün
//    türü ayrıca bilinmelidir).
//
// Android'deki hâliyle korunan notlar:
//  - Karttaki miktar (1200, 7000, …) yalnızca ETİKETTİR; verilecek miktarı sunucu belirler.
//  - Yedek ₺ fiyatları yalnızca mağaza fiyatı gelene kadar görünür.
//  - "Lite" aboneliği katalogda vardır ama mağaza ekranında kartı yoktur.
package com.sorobit.shared.oyun

object ShopFragmentLogic {

    /** 1 can satın almanın anahtar bedeli. Arayüzdeki etiket de bu değerden yazılır. */
    const val LIFE_KEY_COST = 3

    /** Satın alma sonrası fırlatılan ikon sayısı (yalnızca görsel). */
    const val PURCHASE_ANIMATION_ITEM_COUNT = 30

    /** Fırlatılan simgelerin doğuşu bu süreye yayılır (ms). */
    const val PURCHASE_ANIMATION_SPREAD_MS = 400L

    /** Sayaç yenileme aralığı (ms). */
    const val ENERGY_TICK_MS = 1000L

    // ── Metinler ──────────────────────────────────────────────────────────────

    const val MESSAGE_NOT_ENOUGH_KEYS = "Yetersiz anahtar!"
    const val MESSAGE_BUY_LIFE_FAILED = "İşlem tamamlanamadı. Tekrar deneyin."
    const val MESSAGE_AD_LIFE_FAILED = "Can alınamadı. Tekrar deneyin."

    /** BillingManager.onError metinleri. */
    const val MESSAGE_PRODUCT_UNAVAILABLE = "Ürün şu anda alınamıyor. Daha sonra tekrar deneyin."
    const val MESSAGE_SUBSCRIPTION_OFFER_MISSING = "Abonelik teklifi bulunamadı."
    const val MESSAGE_PURCHASE_NOT_STARTED = "Satın alma başlatılamadı."
    const val MESSAGE_PURCHASE_FAILED = "Satın alma tamamlanamadı."
    const val MESSAGE_PURCHASE_NOT_VERIFIED = "Satın alma doğrulanamadı. Uygulamayı tekrar açtığınızda denenecek."

    const val TEXT_ENERGY_FULL = "DOLU"
    const val TEXT_INFINITE = "∞"

    // ── Renkler (updateEnergyUi) ──────────────────────────────────────────────

    const val COLOR_TIMER_FULL = 0xFF78909C.toInt()
    const val COLOR_TIMER_COUNTING = 0xFF29B6F6.toInt()
    const val COLOR_BUY_BUTTON_DISABLED = 0xFF1E2A30.toInt()
    const val COLOR_BUY_BUTTON_ENABLED = 0xFF37474F.toInt()
    const val COLOR_BUY_TEXT_DISABLED = 0xFF78909C.toInt()
    const val COLOR_BUY_TEXT_ENABLED = 0xFFFFFFFF.toInt()

    // ── Ürün türü ─────────────────────────────────────────────────────────────

    enum class ProductKind { CONSUMABLE, SUBSCRIPTION }

    /** Kimlik katalogda yoksa null. */
    fun productKind(productId: String): ProductKind? = when (productId) {
        in BillingCatalog.consumables -> ProductKind.CONSUMABLE
        in BillingCatalog.subscriptions -> ProductKind.SUBSCRIPTION
        else -> null
    }

    /** Mağazadan sorgulanacak bütün ürün kimlikleri (önce tüketilebilirler, sonra abonelikler). */
    fun allProductIds(): List<String> = BillingCatalog.consumables + BillingCatalog.subscriptions

    // ── Altın / anahtar paket kartları ────────────────────────────────────────

    data class PackCard(
        val productId: String,
        /** Karttaki büyük görsel. */
        val imageResName: String,
        /** Miktar satırındaki küçük simge ("gold_ic" / "key"). */
        val currencyIconResName: String,
        /** Miktar etiketi — yalnızca gösterim; gerçek miktar sunucudadır. */
        val amountLabel: String,
        /** Mağaza fiyatı gelene kadar gösterilen yedek fiyat. */
        val fallbackPrice: String,
    )

    const val TITLE_SPECIAL_OFFERS = "Özel Teklifler"
    const val TITLE_LIFE = "Can"
    const val TITLE_GOLD = "Altın"
    const val TITLE_KEYS = "Anahtar"

    val GOLD_CARDS: List<PackCard> = listOf(
        PackCard(BillingCatalog.GOLD_SMALL, "shop_coin_ic1", "gold_ic", "1200", "₺20,99"),
        PackCard(BillingCatalog.GOLD_MEDIUM, "shop_coin_ic2", "gold_ic", "7000", "₺99,99"),
        PackCard(BillingCatalog.GOLD_LARGE, "shop_coin_ic3", "gold_ic", "15000", "₺204,99"),
    )

    val KEY_CARDS: List<PackCard> = listOf(
        PackCard(BillingCatalog.KEYS_SMALL, "key", "key", "10", "₺20,99"),
        PackCard(BillingCatalog.KEYS_MEDIUM, "key", "key", "50", "₺99,99"),
        PackCard(BillingCatalog.KEYS_LARGE, "key", "key", "100", "₺204,99"),
    )

    /** Karttaki fiyat: mağaza fiyatı geldiyse o, gelmediyse yedek. */
    fun priceLabel(card: PackCard, storePrice: String?): String = storePrice ?: card.fallbackPrice

    /** Pro kartının ürünü. */
    const val PRO_PRODUCT_ID = BillingCatalog.SUB_PRO

    /** Pro kartının yedek fiyat etiketi (fragment_shop.xml). */
    const val PRO_FALLBACK_PRICE_LABEL = "aylık ₺19,99"

    /** applyStorePrices: "aylık $price". Ekranda textAllCaps ile büyük harfe çevrilir. */
    fun proPriceLabel(storePrice: String?): String =
        if (storePrice == null) PRO_FALLBACK_PRICE_LABEL else "aylık $storePrice"

    /**
     * animatePurchasedItem: satın alınan pakette fırlatılacak simge; paket değilse (abonelik,
     * bilinmeyen kimlik) null — animasyon oynatılmaz.
     */
    fun purchaseAnimationIconResName(productId: String): String? = when (productId) {
        BillingCatalog.GOLD_SMALL, BillingCatalog.GOLD_MEDIUM, BillingCatalog.GOLD_LARGE -> "gold_ic"
        BillingCatalog.KEYS_SMALL, BillingCatalog.KEYS_MEDIUM, BillingCatalog.KEYS_LARGE -> "key"
        else -> null
    }

    // ── Can bölümü ────────────────────────────────────────────────────────────

    data class EnergySectionState(
        /** Üst çubuktaki can metni: sonsuzsa "∞", değilse anlık can. */
        val headerEnergyText: String,
        /** "DOLU" ya da "mm:ss". */
        val timerText: String,
        val timerColor: Int,
        /** Anahtarla can al düğmesi ve "CAN KAZAN" reklam düğmesi tıklanabilir mi. */
        val canGetLife: Boolean,
        val buyButtonColor: Int,
        val buyTextColor: Int,
        /** Düğmedeki anahtar simgesinin opaklığı. */
        val buyIconAlpha: Float,
        /** "CAN KAZAN" düğmesinin opaklığı. */
        val watchAdAlpha: Float,
        /** Düğmedeki bedel ([LIFE_KEY_COST]). */
        val buyCostText: String,
    )

    /** `String.format("%02d:%02d", dakika, saniye)`; dakika 99'u aşarsa olduğu gibi yazılır. */
    fun formatTimer(timeToNextMs: Long): String {
        val totalSeconds = timeToNextMs / 1000
        val minutes = (totalSeconds / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        return "${ikiHane(minutes)}:${ikiHane(seconds)}"
    }

    /** updateEnergyUi. */
    fun energySectionState(
        isInfinite: Boolean,
        currentEnergy: Int,
        maxEnergy: Int,
        timeUntilNextEnergyMs: Long,
    ): EnergySectionState {
        val header = if (isInfinite) TEXT_INFINITE else currentEnergy.toString()
        return if (isInfinite || currentEnergy >= maxEnergy) {
            EnergySectionState(
                headerEnergyText = header,
                timerText = TEXT_ENERGY_FULL,
                timerColor = COLOR_TIMER_FULL,
                canGetLife = false,
                buyButtonColor = COLOR_BUY_BUTTON_DISABLED,
                buyTextColor = COLOR_BUY_TEXT_DISABLED,
                buyIconAlpha = 0.5f,
                watchAdAlpha = 0.3f,
                buyCostText = LIFE_KEY_COST.toString(),
            )
        } else {
            EnergySectionState(
                headerEnergyText = header,
                timerText = formatTimer(timeUntilNextEnergyMs),
                timerColor = COLOR_TIMER_COUNTING,
                canGetLife = true,
                buyButtonColor = COLOR_BUY_BUTTON_ENABLED,
                buyTextColor = COLOR_BUY_TEXT_ENABLED,
                buyIconAlpha = 1.0f,
                watchAdAlpha = 1.0f,
                buyCostText = LIFE_KEY_COST.toString(),
            )
        }
    }

    /** buyLifeWithKeys ön denetimi: anahtar yetiyor mu. */
    fun canBuyLifeWithKeys(currentKeys: Int): Boolean = currentKeys >= LIFE_KEY_COST

    /** İyimser gösterim: sunucu yanıtı gelene kadar ekranda görünen anahtar sayısı. */
    fun optimisticKeysAfterBuyLife(currentKeys: Int): Int = currentKeys - LIFE_KEY_COST
}

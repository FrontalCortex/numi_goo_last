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
//  - Karttaki miktar (5 000, 20 000, …) yalnızca ETİKETTİR; verilecek miktarı sunucu belirler.
//  - Yedek ₺ fiyatları yalnızca mağaza fiyatı gelene kadar görünür.
//  - "Lite" aboneliği katalogda vardır ama mağaza ekranında kartı yoktur.
//
// 28.11.2026 güncel Android'e eşitlendi: LIFE_KEY_COST 3 → 1 (ShopFragment.kt companion),
// altın/anahtar kartlarının miktar ve yedek fiyatları ile Pro yedek fiyatı (fragment_shop.xml,
// strings_ui.xml fragment_shop_shop_pro_price_text); Sobi kredisi kartları eklendi.
package com.sorobit.shared.oyun

object ShopFragmentLogic {

    /** 1 can satın almanın anahtar bedeli. Arayüzdeki etiket de bu değerden yazılır. */
    const val LIFE_KEY_COST = 1

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

    /** fragment_shop.xml shopGoldCard1..3 (miktar etiketi ve shopGoldPrice1..3 yedek fiyatı). */
    val GOLD_CARDS: List<PackCard> = listOf(
        PackCard(BillingCatalog.GOLD_SMALL, "shop_coin_ic1", "gold_ic", "5 000", "₺39,00"),
        PackCard(BillingCatalog.GOLD_MEDIUM, "shop_coin_ic2", "gold_ic", "20 000", "₺120,00"),
        PackCard(BillingCatalog.GOLD_LARGE, "shop_coin_ic3", "gold_ic", "100 000", "₺500,00"),
    )

    /** fragment_shop.xml shopKeyCard1..3 (miktar etiketi ve shopKeyPrice1..3 yedek fiyatı). */
    val KEY_CARDS: List<PackCard> = listOf(
        PackCard(BillingCatalog.KEYS_SMALL, "key", "key", "10", "₺29,00"),
        PackCard(BillingCatalog.KEYS_MEDIUM, "key", "key", "50", "₺119,00"),
        PackCard(BillingCatalog.KEYS_LARGE, "key", "key", "200", "₺429,00"),
    )

    /**
     * Sobi kredisi paketi kartı (fragment_shop.xml shopSobiCreditCard1..3).
     * @property amountTextKey miktar metninin strings.xml anahtarı (Android: @string/shop_sobi_credit_amount_N).
     */
    data class SobiCreditCard(
        val productId: String,
        val amountTextKey: String,
        val fallbackPrice: String,
    )

    val SOBI_CREDIT_CARDS: List<SobiCreditCard> = listOf(
        SobiCreditCard(BillingCatalog.SOBI_CREDITS_20, "shop_sobi_credit_amount_20", "₺12,99"),
        SobiCreditCard(BillingCatalog.SOBI_CREDITS_50, "shop_sobi_credit_amount_50", "₺24,99"),
        SobiCreditCard(BillingCatalog.SOBI_CREDITS_120, "shop_sobi_credit_amount_120", "₺49,99"),
    )

    /** Karttaki fiyat: mağaza fiyatı geldiyse o, gelmediyse yedek. */
    fun priceLabel(card: PackCard, storePrice: String?): String = storePrice ?: card.fallbackPrice

    /** Pro kartının ürünü. */
    const val PRO_PRODUCT_ID = BillingCatalog.SUB_PRO

    /** Pro kartının yedek fiyat etiketi (strings_ui.xml fragment_shop_shop_pro_price_text, Türkçe). */
    const val PRO_FALLBACK_PRICE_LABEL = "aylık ₺99,00"

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
        BillingCatalog.CREDITS_SMALL, BillingCatalog.CREDITS_MEDIUM, BillingCatalog.CREDITS_LARGE,
        BillingCatalog.SOBI_CREDITS_20, BillingCatalog.SOBI_CREDITS_50, BillingCatalog.SOBI_CREDITS_120 -> "chat_credit_ic3"
        else -> null
    }

    /**
     * Android (güncel) animatePurchasedItem: fırlatılacak simge sayısı. Öğretmen kredisinde adede göre 1 / 5 / 10,
     * diğer paketlerde [PURCHASE_ANIMATION_ITEM_COUNT]; paket değilse null.
     */
    fun purchaseAnimationItemCount(productId: String): Int? = when (productId) {
        BillingCatalog.CREDITS_SMALL -> 1
        BillingCatalog.CREDITS_MEDIUM -> 5
        BillingCatalog.CREDITS_LARGE -> 10
        else -> if (purchaseAnimationIconResName(productId) != null) PURCHASE_ANIMATION_ITEM_COUNT else null
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

package com.example.app

/**
 * Play Console'daki ürün kimlikleri.
 *
 * Buradaki kimlikler üç yerde birebir aynı olmalıdır:
 *   1. Play Console → Monetize → Products
 *   2. Bu dosya (istemci: hangi kart hangi ürünü açar, fiyat nereden okunur)
 *   3. functions/index.js → `PLAY_PRODUCT_CATALOG` / `PLAY_SUBSCRIPTION_CATALOG`
 *
 * DİKKAT: Verilecek altın/anahtar miktarı burada TANIMLI DEĞİLDİR ve olmamalıdır —
 * miktar yalnızca sunucudaki katalogdan gelir. İstemci sadece "hangi ürün" bilgisini taşır.
 */
object BillingCatalog {

    // ── Tüketilebilir ürünler (altın / anahtar paketleri) ───────────────────
    const val GOLD_SMALL = "gold_1200"
    const val GOLD_MEDIUM = "gold_7000"
    const val GOLD_LARGE = "gold_15000"
    const val KEYS_SMALL = "keys_10"
    const val KEYS_MEDIUM = "keys_50"
    const val KEYS_LARGE = "keys_100"

    val consumables = listOf(
        GOLD_SMALL, GOLD_MEDIUM, GOLD_LARGE,
        KEYS_SMALL, KEYS_MEDIUM, KEYS_LARGE,
    )

    // ── Abonelikler ─────────────────────────────────────────────────────────
    const val SUB_PRO = "pro_monthly"
    const val SUB_LITE = "lite_monthly"

    val subscriptions = listOf(SUB_PRO, SUB_LITE)

    /** PlanFragment'teki "Pro" / "Lite" seçimini ürün kimliğine çevirir. */
    fun subscriptionForPlanName(planName: String): String =
        if (planName.equals("Lite", ignoreCase = true)) SUB_LITE else SUB_PRO
}

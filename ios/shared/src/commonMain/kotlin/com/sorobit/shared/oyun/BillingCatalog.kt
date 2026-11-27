// Kaynak: BillingCatalog.kt (tamamı).
// Android (güncel): ürün kimlikleri yeniden adlandırıldı (gold_1200 → gold_small …), öğretmen danışma kredisi ve Sobi
// kredisi ürünleri ile abonelik rütbesi eklendi. Not: Bunlar Google Play ürün kimlikleridir; App Store'da aynı kimlikler kullanılırsa
// sunucudaki katalogla eşleşir, farklı kimlik seçilirse sunucu kataloğu da genişletilmelidir.
package com.sorobit.shared.oyun

/**
 * Mağaza konsolundaki ürün kimlikleri.
 *
 * Buradaki kimlikler üç yerde birebir aynı olmalıdır:
 *   1. Mağaza konsolu (Play Console → Monetize → Products)
 *   2. Bu dosya (istemci: hangi kart hangi ürünü açar, fiyat nereden okunur)
 *   3. functions/index.js → `PLAY_PRODUCT_CATALOG` / `PLAY_SUBSCRIPTION_CATALOG`
 *
 * DİKKAT: Verilecek altın/anahtar miktarı burada TANIMLI DEĞİLDİR ve olmamalıdır —
 * miktar yalnızca sunucudaki katalogdan gelir. İstemci sadece "hangi ürün" bilgisini taşır.
 */
object BillingCatalog {

    // ── Tüketilebilir ürünler (altın / anahtar paketleri) ───────────────────
    const val GOLD_SMALL = "gold_small"
    const val GOLD_MEDIUM = "gold_medium"
    const val GOLD_LARGE = "gold_large"
    const val KEYS_SMALL = "keys_small"
    const val KEYS_MEDIUM = "keys_medium"
    const val KEYS_LARGE = "keys_large"

    // ── Öğretmen danışma kredileri ──────────────────────────────────────────
    // Her kredi bir soru hakkıdır. Verilen adet yine sunucudan gelir; Pro aboneler büyük paketlerde bonus kredi alır
    // (functions/index.js → PRO_CREDIT_BONUS).
    const val CREDITS_SMALL = "credits_small"
    const val CREDITS_MEDIUM = "credits_medium"
    const val CREDITS_LARGE = "credits_large"

    // ── Sobi (yapay zekâ) mesaj kredisi ─────────────────────────────────────
    // Öğretmen kredisinden ayrı; her kredi günlük ücretsiz hak bittikten sonra bir Sobi mesajı. Adetler (20 / 50 / 120)
    // sunucuda: functions/index.js → PLAY_PRODUCT_CATALOG.sobiCredits.
    const val SOBI_CREDITS_20 = "sobi_credits_20"
    const val SOBI_CREDITS_50 = "sobi_credits_50"
    const val SOBI_CREDITS_120 = "sobi_credits_120"

    val consumables = listOf(
        GOLD_SMALL, GOLD_MEDIUM, GOLD_LARGE,
        KEYS_SMALL, KEYS_MEDIUM, KEYS_LARGE,
        CREDITS_SMALL, CREDITS_MEDIUM, CREDITS_LARGE,
        SOBI_CREDITS_20, SOBI_CREDITS_50, SOBI_CREDITS_120,
    )

    // ── Abonelikler ─────────────────────────────────────────────────────────
    const val SUB_PRO = "pro_monthly"
    const val SUB_LITE = "lite_monthly"

    val subscriptions = listOf(SUB_PRO, SUB_LITE)

    /** Plan ekranındaki "Pro" / "Lite" seçimini ürün kimliğine çevirir. */
    fun subscriptionForPlanName(planName: String): String =
        if (planName.equals("Lite", ignoreCase = true)) SUB_LITE else SUB_PRO

    /**
     * Abonelik rütbesi; yüksek olan daha kapsamlı plandır. Mevcut abonelikten yenisine geçerken YÖN belirlemek için
     * (Play'in CHARGE_PRORATED_PRICE modu yalnızca yükseltmede çalışır) ve sunucudaki plan çakışmasının aynısı
     * (functions/index.js → PLAN_RANK); iki liste birlikte güncellenmelidir.
     */
    fun subscriptionRank(productId: String): Int = when (productId) {
        SUB_PRO -> 2
        SUB_LITE -> 1
        else -> 0
    }
}

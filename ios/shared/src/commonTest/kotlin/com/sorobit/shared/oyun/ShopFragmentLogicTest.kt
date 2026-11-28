// ShopFragmentLogic'in Android'deki ShopFragment kararlarıyla aynı sonucu verdiğini doğrular.
// Beklenen değerler ShopFragment.kt ve fragment_shop.xml'den elle alınmıştır.
package com.sorobit.shared.oyun

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShopFragmentLogicTest {
    private val s = ShopFragmentLogic

    @Test
    fun paketKartlari() {
        assertEquals(listOf("gold_small", "gold_medium", "gold_large"), s.GOLD_CARDS.map { it.productId })
        assertEquals(listOf("keys_small", "keys_medium", "keys_large"), s.KEY_CARDS.map { it.productId })
        assertEquals(listOf("shop_coin_ic1", "shop_coin_ic2", "shop_coin_ic3"), s.GOLD_CARDS.map { it.imageResName })
        assertTrue(s.KEY_CARDS.all { it.imageResName == "key" && it.currencyIconResName == "key" })
        // Güncel Android fragment_shop.xml (28.11.2026).
        assertEquals(listOf("5 000", "20 000", "100 000"), s.GOLD_CARDS.map { it.amountLabel })
        assertEquals(listOf("10", "50", "200"), s.KEY_CARDS.map { it.amountLabel })
        assertEquals(listOf("₺39,00", "₺120,00", "₺500,00"), s.GOLD_CARDS.map { it.fallbackPrice })
        assertEquals(listOf("₺29,00", "₺119,00", "₺429,00"), s.KEY_CARDS.map { it.fallbackPrice })
        assertEquals(listOf("sobi_credits_20", "sobi_credits_50", "sobi_credits_120"), s.SOBI_CREDIT_CARDS.map { it.productId })
        assertEquals(
            listOf("shop_sobi_credit_amount_20", "shop_sobi_credit_amount_50", "shop_sobi_credit_amount_120"),
            s.SOBI_CREDIT_CARDS.map { it.amountTextKey },
        )
        assertEquals(listOf("₺12,99", "₺24,99", "₺49,99"), s.SOBI_CREDIT_CARDS.map { it.fallbackPrice })

        // Altın/anahtar kartlarının kimlikleri katalogdaki tüketilebilirlerin ilk altısıdır (devamı: öğretmen ve Sobi kredileri).
        assertEquals(BillingCatalog.consumables.take(6), (s.GOLD_CARDS + s.KEY_CARDS).map { it.productId })

        assertEquals("₺39,00", s.priceLabel(s.GOLD_CARDS[0], null))
        assertEquals("$0.99", s.priceLabel(s.GOLD_CARDS[0], "$0.99"))
    }

    @Test
    fun urunTurleri() {
        assertEquals(ShopFragmentLogic.ProductKind.CONSUMABLE, s.productKind("gold_small"))
        assertEquals(ShopFragmentLogic.ProductKind.CONSUMABLE, s.productKind("keys_large"))
        assertEquals(ShopFragmentLogic.ProductKind.SUBSCRIPTION, s.productKind("pro_monthly"))
        assertEquals(ShopFragmentLogic.ProductKind.SUBSCRIPTION, s.productKind("lite_monthly"))
        assertNull(s.productKind("bilinmeyen"))
        assertEquals(14, s.allProductIds().size)
        assertEquals(ShopFragmentLogic.ProductKind.CONSUMABLE, s.productKind("credits_medium"))
        assertEquals(ShopFragmentLogic.ProductKind.CONSUMABLE, s.productKind("sobi_credits_120"))
        assertEquals("pro_monthly", s.PRO_PRODUCT_ID)
    }

    @Test
    fun proFiyatiVeAnimasyon() {
        assertEquals("aylık ₺99,00", s.proPriceLabel(null))
        assertEquals("aylık $4.99", s.proPriceLabel("$4.99"))

        assertEquals("gold_ic", s.purchaseAnimationIconResName("gold_medium"))
        assertEquals("key", s.purchaseAnimationIconResName("keys_small"))
        assertNull(s.purchaseAnimationIconResName("pro_monthly"))
        assertEquals(30, s.PURCHASE_ANIMATION_ITEM_COUNT)
        assertEquals("chat_credit_ic3", s.purchaseAnimationIconResName("credits_small"))
        assertEquals("chat_credit_ic3", s.purchaseAnimationIconResName("sobi_credits_20"))
        assertEquals(listOf(1, 5, 10), listOf("credits_small", "credits_medium", "credits_large").map { s.purchaseAnimationItemCount(it) })
        assertEquals(30, s.purchaseAnimationItemCount("gold_large"))
        assertEquals(30, s.purchaseAnimationItemCount("sobi_credits_50"))
        assertNull(s.purchaseAnimationItemCount("pro_monthly"))
    }

    @Test
    fun canBolumu() {
        // Dolu.
        var d = s.energySectionState(isInfinite = false, currentEnergy = 5, maxEnergy = 5, timeUntilNextEnergyMs = 0)
        assertEquals("5", d.headerEnergyText)
        assertEquals("DOLU", d.timerText)
        assertEquals(0xFF78909C.toInt(), d.timerColor)
        assertFalse(d.canGetLife)
        assertEquals(0xFF1E2A30.toInt(), d.buyButtonColor)
        assertEquals(0.5f, d.buyIconAlpha)
        assertEquals(0.3f, d.watchAdAlpha)
        assertEquals("1", d.buyCostText)

        // Sonsuz: can az görünse de dolu sayılır.
        d = s.energySectionState(isInfinite = true, currentEnergy = 0, maxEnergy = 5, timeUntilNextEnergyMs = 90_000)
        assertEquals("∞", d.headerEnergyText)
        assertEquals("DOLU", d.timerText)
        assertFalse(d.canGetLife)

        // Eksik: sayaç.
        d = s.energySectionState(isInfinite = false, currentEnergy = 2, maxEnergy = 5, timeUntilNextEnergyMs = 125_999)
        assertEquals("2", d.headerEnergyText)
        assertEquals("02:05", d.timerText)
        assertEquals(0xFF29B6F6.toInt(), d.timerColor)
        assertTrue(d.canGetLife)
        assertEquals(0xFF37474F.toInt(), d.buyButtonColor)
        assertEquals(0xFFFFFFFF.toInt(), d.buyTextColor)
        assertEquals(1.0f, d.watchAdAlpha)

        assertEquals("00:00", s.formatTimer(999))
        assertEquals("10:00", s.formatTimer(600_000))
    }

    @Test
    fun anahtarlaCan() {
        // Güncel Android ShopFragment.LIFE_KEY_COST = 1.
        assertEquals(1, s.LIFE_KEY_COST)
        assertFalse(s.canBuyLifeWithKeys(0))
        assertTrue(s.canBuyLifeWithKeys(1))
        assertEquals(9, s.optimisticKeysAfterBuyLife(10))
        assertEquals("Yetersiz anahtar!", s.MESSAGE_NOT_ENOUGH_KEYS)
    }
}

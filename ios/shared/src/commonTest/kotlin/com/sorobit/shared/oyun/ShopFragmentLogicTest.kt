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
        assertEquals(listOf("gold_1200", "gold_7000", "gold_15000"), s.GOLD_CARDS.map { it.productId })
        assertEquals(listOf("keys_10", "keys_50", "keys_100"), s.KEY_CARDS.map { it.productId })
        assertEquals(listOf("shop_coin_ic1", "shop_coin_ic2", "shop_coin_ic3"), s.GOLD_CARDS.map { it.imageResName })
        assertTrue(s.KEY_CARDS.all { it.imageResName == "key" && it.currencyIconResName == "key" })
        assertEquals(listOf("1200", "7000", "15000"), s.GOLD_CARDS.map { it.amountLabel })
        assertEquals(listOf("10", "50", "100"), s.KEY_CARDS.map { it.amountLabel })
        assertEquals(listOf("₺20,99", "₺99,99", "₺204,99"), s.GOLD_CARDS.map { it.fallbackPrice })

        // Kartlardaki kimlikler katalogdaki tüketilebilirlerin tamamıdır.
        assertEquals(BillingCatalog.consumables, (s.GOLD_CARDS + s.KEY_CARDS).map { it.productId })

        assertEquals("₺20,99", s.priceLabel(s.GOLD_CARDS[0], null))
        assertEquals("$0.99", s.priceLabel(s.GOLD_CARDS[0], "$0.99"))
    }

    @Test
    fun urunTurleri() {
        assertEquals(ShopFragmentLogic.ProductKind.CONSUMABLE, s.productKind("gold_1200"))
        assertEquals(ShopFragmentLogic.ProductKind.CONSUMABLE, s.productKind("keys_100"))
        assertEquals(ShopFragmentLogic.ProductKind.SUBSCRIPTION, s.productKind("pro_monthly"))
        assertEquals(ShopFragmentLogic.ProductKind.SUBSCRIPTION, s.productKind("lite_monthly"))
        assertNull(s.productKind("bilinmeyen"))
        assertEquals(8, s.allProductIds().size)
        assertEquals("pro_monthly", s.PRO_PRODUCT_ID)
    }

    @Test
    fun proFiyatiVeAnimasyon() {
        assertEquals("aylık ₺19,99", s.proPriceLabel(null))
        assertEquals("aylık $4.99", s.proPriceLabel("$4.99"))

        assertEquals("gold_ic", s.purchaseAnimationIconResName("gold_7000"))
        assertEquals("key", s.purchaseAnimationIconResName("keys_10"))
        assertNull(s.purchaseAnimationIconResName("pro_monthly"))
        assertEquals(30, s.PURCHASE_ANIMATION_ITEM_COUNT)
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
        assertEquals("3", d.buyCostText)

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
        assertFalse(s.canBuyLifeWithKeys(2))
        assertTrue(s.canBuyLifeWithKeys(3))
        assertEquals(7, s.optimisticKeysAfterBuyLife(10))
        assertEquals("Yetersiz anahtar!", s.MESSAGE_NOT_ENOUGH_KEYS)
    }
}

package com.example.app

import org.junit.Assert.assertEquals
import org.junit.Test

/** PlanStatus.effectivePlan: abonelik süresi ve yayın hediyesi (giftProUntil). */
class PlanStatusTest {

    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun plan(stored: String?, expires: Long? = null, gift: Long? = null) =
        PlanStatus.effectivePlan(stored, expires, gift, now)

    @Test fun alanYoksaFree() = assertEquals("Free", plan(null))

    @Test fun suresiGecenAbonelikFree() = assertEquals("Free", plan("Pro", now - 1))

    @Test fun hediyeSuruyorsaPro() = assertEquals("Pro", plan("Free", gift = now + day))

    @Test fun hediyeBittiyseFree() = assertEquals("Free", plan("Free", gift = now - 1))

    @Test fun hediyeLiteyiProYapar() = assertEquals("Pro", plan("Lite", now + day, now + day))

    @Test fun hediyeBitinceLiteKalir() = assertEquals("Lite", plan("Lite", now + day, now - 1))

    @Test fun premiumHediyedenEtkilenmez() = assertEquals("Premium", plan("Premium", gift = now + day))

    @Test fun hediyeliKullaniciProSayilir() =
        assertEquals(true, PlanStatus.isProPlan(plan(null, gift = now + day)))
}

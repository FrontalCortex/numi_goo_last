// Beklentiler Android SubscriptionCta.apply'dan.
package com.sorobit.shared.oyun

import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionCtaTest {
    private val c = SubscriptionCta

    @Test
    fun denemeYoksaGecisMetni() {
        assertEquals(SubscriptionCtaLabel("sub_cta_no_trial_pro", null), c.label(null, BillingCatalog.SUB_PRO))
        assertEquals(SubscriptionCtaLabel("sub_cta_no_trial_lite", null), c.label(null, BillingCatalog.SUB_LITE))
        // Bilinmeyen ürün Pro metnine düşer (Android: SUB_LITE değilse Pro).
        assertEquals(SubscriptionCtaLabel("sub_cta_no_trial_pro", null), c.label(null, "baska"))
    }

    @Test
    fun haftaVeGun() {
        assertEquals(SubscriptionCtaLabel("sub_cta_trial_weeks", 1), c.label(7, BillingCatalog.SUB_PRO))
        assertEquals(SubscriptionCtaLabel("sub_cta_trial_weeks", 2), c.label(14, BillingCatalog.SUB_LITE))
        assertEquals(SubscriptionCtaLabel("sub_cta_trial_days", 3), c.label(3, BillingCatalog.SUB_PRO))
        assertEquals(SubscriptionCtaLabel("sub_cta_trial_days", 10), c.label(10, BillingCatalog.SUB_PRO))
        // 0 gün de 7'ye bölünür (Android'de de "0 HAFTA").
        assertEquals(SubscriptionCtaLabel("sub_cta_trial_weeks", 0), c.label(0, BillingCatalog.SUB_PRO))
    }
}

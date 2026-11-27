// Kayıt sorularının seri adımlarındaki saf kuralların (StreakRepository, StreakViews, StreakMilestones,
// LearningPathPlan) Android ile aynı sonucu verdiğini doğrular. Beklenen değerler Android kaynağı
// okunarak elle türetilmiştir.
package com.sorobit.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignupStreakRulesTest {
    @Test
    fun hatirlatmaSaatleri() {
        assertEquals(listOf(16, 18, 19, 20), SignupStreakRules.REMINDER_HOUR_OPTIONS)
        assertTrue(SignupStreakRules.isPresetReminderHour(SignupFlowRules.DEFAULT_REMINDER_HOUR))
        assertFalse(SignupStreakRules.isPresetReminderHour(7))
        assertEquals("19:00", SignupStreakRules.hourText(19))
        assertEquals("06:00", SignupStreakRules.hourText(6))
        assertEquals("00:00", SignupStreakRules.hourText(0))
    }

    @Test
    fun saatIzgarasiSabahtanBaslar() {
        val izgara = SignupStreakRules.hourGrid()
        assertEquals(24, izgara.size)
        assertEquals(6, izgara.first())
        assertEquals(5, izgara.last())
        assertEquals((0 until 24).toSet(), izgara.toSet())
    }

    @Test
    fun meydanOkumaOdulu() {
        assertEquals(500, SignupStreakRules.challengeReward(3))
        assertEquals(1000, SignupStreakRules.challengeReward(5))
        assertEquals(1500, SignupStreakRules.challengeReward(7))
        assertEquals(0, SignupStreakRules.challengeReward(4))
        // Seçeneklerin hepsinin ödülü var.
        for (gun in SignupFlowRules.CHALLENGE_OPTIONS) {
            assertTrue(SignupStreakRules.challengeReward(gun) > 0)
        }
    }

    @Test
    fun ilerlemeYoluGunleri() {
        // 100 adım * 4 dk / 10 dk = 40 çalışma günü; haftada 5 gün → 56 takvim günü.
        assertEquals(56, SignupStreakRules.daysForSteps(steps = 100, goalMinutes = 10))
        // 100 * 4 / 20 = 20 → 28.
        assertEquals(28, SignupStreakRules.daysForSteps(steps = 100, goalMinutes = 20))
        // Yukarı yuvarlanır: 10 * 4 / 5 = 8 → 11,2 → 12.
        assertEquals(12, SignupStreakRules.daysForSteps(steps = 10, goalMinutes = 5))
        // Sıfır dakika 1'e çekilir (bölme hatası yok).
        assertEquals(56, SignupStreakRules.daysForSteps(steps = 10, goalMinutes = 0))
        // Müfredattan sayım: adım var ve hedef büyüdükçe gün azalır.
        assertTrue(SignupStreakRules.stepsToMentalMath() > 0)
        assertTrue(SignupStreakRules.daysToMentalMath(5) > SignupStreakRules.daysToMentalMath(20))
    }
}

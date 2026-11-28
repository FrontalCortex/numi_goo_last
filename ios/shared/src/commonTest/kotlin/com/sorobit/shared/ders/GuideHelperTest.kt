package com.sorobit.shared.ders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Beklentiler Android GuideHelper.kt sayfa tablosundan ve AbacusFragment / BlindingLessonFragment rehber
// açılış kurallarından.
class GuideHelperTest {

    @Test
    fun sayfaSayilari() {
        assertEquals(5, GuideHelper.getGuideContentsForNumber(1).size)
        assertEquals(2, GuideHelper.getGuideContentsForNumber(2).size)
        assertEquals(2, GuideHelper.getGuideContentsForNumber(3).size)
        assertEquals(3, GuideHelper.getGuideContentsForNumber(4).size)
        assertEquals(2, GuideHelper.getGuideContentsForNumber(5).size)
        assertEquals(1, GuideHelper.getGuideContentsForNumber(6).size)
        assertTrue(GuideHelper.getGuideContentsForNumber(0).isEmpty())
        assertTrue(GuideHelper.getGuideContentsForNumber(7).isEmpty())
    }

    @Test
    fun rehber1Ayrintilari() {
        val r = GuideHelper.getGuideContentsForNumber(1)
        assertEquals(listOf("TEACH_TALK", "TEACH_POINT", "TEACH_POINT", "TEACH_POINT", "TEACH_WARN"), r.map { it.emote })
        assertEquals("ui_once_abakuse_ilk_sayiyi_yazip", r[1].textKey)
        assertEquals(GuideTarget.FIRST_NUMBER_TEXT, r[1].bubbleAnimationTarget)
        assertEquals(0xFFFF00, r[1].bubbleAnimationColor)
        assertEquals(listOf("rod4BottomBead4"), r[1].beadIds)
        assertEquals(listOf("rod4BottomBead1"), r[1].backBeadIds)
        assertEquals(GuideTarget.SECOND_NUMBER_TEXT, r[2].bubbleAnimationTarget)
        assertEquals(listOf("rod4TopBead"), r[2].beadIds)
        assertNull(r[2].backBeadIds)
        assertEquals(GuideTarget.KONTROL_BUTTON, r[3].bubbleAnimationTarget)
        assertNull(r[3].bubbleAnimationColor)
        assertEquals(1.1f, r[3].bubbleAnimationMaxScale)
        assertEquals(listOf("rod4BottomBead1", "rod4TopBead"), r[4].finishBeadIds)
        assertEquals(1.4f, r[0].bubbleAnimationMaxScale)
    }

    @Test
    fun zorunluTiklamalar() {
        val r2 = GuideHelper.getGuideContentsForNumber(2)[1]
        assertEquals(GuideTarget.RULES_PANEL_BUTTON, r2.requiredClickTarget)
        assertEquals(0x8BC34A, r2.bubbleAnimationColor)
        assertEquals(0xDFF0D4, r2.bubbleAnimationTintLight)
        assertEquals(false, r2.requiredClickAdvancesGuide)
        val r4 = GuideHelper.getGuideContentsForNumber(4)
        assertEquals(GuideTarget.RULES_BOOK_BUTTON, r4[1].requiredClickTarget)
        assertTrue(r4[1].requiredClickAdvancesGuide)
        assertTrue(r4[2].waitForRulesTableSelection)
        val r5 = GuideHelper.getGuideContentsForNumber(5)[0]
        assertEquals(GuideTarget.SKIP_STEP_BUTTON, r5.bubbleAnimationTarget)
        assertNull(r5.requiredClickTarget)
        assertEquals(0x263C31, r5.bubbleAnimationColor)
        assertEquals(0x00FF8C, r5.bubbleAnimationTintLight)
        val r6 = GuideHelper.getGuideContentsForNumber(6)[0]
        assertEquals(GuideTarget.ABACUS_MODE_BUTTON, r6.requiredClickTarget)
    }

    @Test
    fun abakusAcilisi() {
        assertNull(GuideHelper.abacusLessonGuideNumber(null, 1, 0))
        assertNull(GuideHelper.abacusLessonGuideNumber(1, 2, 0))
        assertEquals(1, GuideHelper.abacusLessonGuideNumber(1, 1, 5))
        assertEquals(6, GuideHelper.abacusLessonGuideNumber(6, 1, 1))
        assertNull(GuideHelper.abacusLessonGuideNumber(6, 1, 2))
        assertNull(GuideHelper.abacusLessonGuideNumber(9, 1, 0))
    }

    @Test
    fun korDersAcilisi() {
        // Atlama öğreticisi önceliklidir, ders adımından bağımsızdır.
        assertEquals(5, GuideHelper.blindingLessonGuideNumber(2, 3, true, false, 0))
        assertEquals(5, GuideHelper.blindingLessonGuideNumber(null, 1, true, false, 0))
        assertEquals(2, GuideHelper.blindingLessonGuideNumber(2, 1, true, true, 0))
        assertNull(GuideHelper.blindingLessonGuideNumber(2, 2, false, false, 0))
        assertNull(GuideHelper.blindingLessonGuideNumber(5, 1, false, false, 0))
        assertNull(GuideHelper.blindingLessonGuideNumber(null, 1, false, false, 0))
        assertNull(GuideHelper.blindingLessonGuideNumber(6, 1, false, false, 2))
        assertEquals(6, GuideHelper.blindingLessonGuideNumber(6, 1, false, false, 1))
    }

    @Test
    fun gosterimSiniri() {
        assertTrue(GuideHelper.canShow(1, 99))
        assertTrue(GuideHelper.canShow(6, 1))
        assertEquals(false, GuideHelper.canShow(6, 2))
        assertTrue(GuideHelper.recordsShown(6))
        assertEquals(false, GuideHelper.recordsShown(5))
    }

    @Test
    fun tersBoncukSadelestirme() {
        assertEquals(
            listOf("rod4TopBead", "rod2TopBead", "rod4BottomBead3", "rod1BottomBead2"),
            GuideHelper.optimizeBeadIdsForReverse(
                listOf("rod4BottomBead1", "rod4TopBead", "rod4BottomBead3", "rod1BottomBead2", "rod4BottomBead2", "rod2TopBead"),
            ),
        )
        assertEquals(
            listOf("rod4TopBead", "rod4BottomBead1"),
            GuideHelper.optimizeBeadIdsForReverse(listOf("rod4BottomBead1", "rod4TopBead")),
        )
        assertTrue(GuideHelper.optimizeBeadIdsForReverse(emptyList()).isEmpty())
    }
}

package com.sorobit.shared.abakus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Beklentiler Android AbacusFragment / BlindingLessonFragment kural tablolarından (LessonRulesLogic.kt başlığı).
class LessonRulesLogicTest {

    private val v = LessonRulesVisibility.VISIBLE
    private val inv = LessonRulesVisibility.INVISIBLE
    private val gone = LessonRulesVisibility.GONE

    private fun abacus(part: Int, index: Int, signedOut: Boolean = false) =
        LessonRulesLesson(part, index, isBlinding = false, isDailyQuestionMode = false, dailyQuestionPartId = 0, signedOut = signedOut)

    private fun blinding(part: Int, index: Int, dailyPart: Int? = null) =
        LessonRulesLesson(part, index, isBlinding = true, isDailyQuestionMode = dailyPart != null,
            dailyQuestionPartId = dailyPart ?: 0, signedOut = false)

    @Test
    fun abakusPanelTablosuSiraya() {
        assertEquals(LessonRulesTable.NONE, abacus(1, 11).activeRulesPanelTable())
        assertEquals(LessonRulesTable.FIVE, abacus(1, 12).activeRulesPanelTable())
        assertEquals(LessonRulesTable.FIVE, abacus(1, 16).activeRulesPanelTable())
        assertEquals(LessonRulesTable.TEN_FIVE, abacus(1, 17).activeRulesPanelTable())
        assertEquals(LessonRulesTable.TEN, abacus(1, 23).activeRulesPanelTable())
        assertEquals(LessonRulesTable.BEAD, abacus(1, 29).activeRulesPanelTable())
        assertEquals(LessonRulesTable.NONE, abacus(2, 5).activeRulesPanelTable())
        assertEquals(LessonRulesTable.FIVE, abacus(2, 6).activeRulesPanelTable())
        assertEquals(LessonRulesTable.TEN_EXTRACTION, abacus(2, 11).activeRulesPanelTable())
        assertEquals(LessonRulesTable.BEAD_EXTRACTION, abacus(2, 16).activeRulesPanelTable())
        assertEquals(LessonRulesTable.NONE, abacus(3, 40).activeRulesPanelTable())
    }

    @Test
    fun korDersteBoncukCikarmaDaliYok() {
        assertEquals(LessonRulesTable.TEN_EXTRACTION, blinding(2, 16).activeRulesPanelTable())
        assertEquals(LessonRulesTable.BEAD, blinding(1, 30).activeRulesPanelTable())
        // Seçicili derste tablo kitaptan seçilir.
        assertEquals(LessonRulesTable.NONE, blinding(4, 30).activeRulesPanelTable())
        assertEquals(LessonRulesTable.NONE, blinding(1, 30, dailyPart = 1).activeRulesPanelTable())
    }

    @Test
    fun secici() {
        assertTrue(abacus(3, 1).usesRulesTablePicker())
        assertFalse(abacus(4, 1).usesRulesTablePicker())
        assertFalse(blinding(3, 1).usesRulesTablePicker())
        assertTrue(blinding(4, 1).usesRulesTablePicker())
        assertTrue(blinding(9, 1).usesRulesTablePicker())
        assertFalse(blinding(10, 1).usesRulesTablePicker())
        assertTrue(blinding(1, 1, dailyPart = 2).usesRulesTablePicker())
    }

    @Test
    fun abakusDugmeleri() {
        assertEquals(gone, abacus(1, 11).rulesBookButtonVisibility())
        assertEquals(gone, abacus(1, 11).rulesPanelButtonVisibility())
        assertEquals(inv, abacus(1, 12).rulesBookButtonVisibility())
        assertEquals(v, abacus(1, 12).rulesPanelButtonVisibility())
        assertEquals(inv, abacus(1, 16).rulesBookButtonVisibility())
        assertEquals(v, abacus(1, 17).rulesBookButtonVisibility())
        assertEquals(gone, abacus(2, 4).rulesBookButtonVisibility())
        assertEquals(gone, abacus(2, 4).rulesPanelButtonVisibility())
        assertEquals(v, abacus(2, 5).rulesBookButtonVisibility())
        assertEquals(v, abacus(3, 1).rulesBookButtonVisibility())
        assertEquals(gone, abacus(3, 1, signedOut = true).rulesBookButtonVisibility())
        assertEquals(gone, abacus(3, 1, signedOut = true).rulesPanelButtonVisibility())
    }

    @Test
    fun korDugmeleriYalnizcaSecicide() {
        assertEquals(gone, blinding(1, 30).rulesBookButtonVisibility())
        assertEquals(gone, blinding(1, 30).rulesPanelButtonVisibility())
        assertEquals(v, blinding(5, 1).rulesBookButtonVisibility())
        assertEquals(v, blinding(5, 1).rulesPanelButtonVisibility())
    }

    @Test
    fun besliBaslikDili() {
        assertFalse(abacus(2, 5).rulesPanelFiveIsSubtraction())
        assertTrue(abacus(2, 6).rulesPanelFiveIsSubtraction())
        assertTrue(blinding(5, 1).rulesPanelFiveIsSubtraction())
        assertFalse(abacus(1, 20).rulesPanelFiveIsSubtraction())
    }

    @Test
    fun abakusKitabiBolum1() {
        val a = abacus(1, 17).rulesBookSetup()
        assertEquals(LessonRulesSection.ADDITION, a.section)
        assertTrue(a.fiveTable)
        assertTrue(a.tenFiveTable)
        assertFalse(a.tenTable)
        assertFalse(a.beadTable)
        assertFalse(a.multiplicationTables)
        assertEquals(Triple(v, inv, inv), Triple(a.divider1, a.divider2, a.divider3))
        assertFalse(a.subtractionHeaderTexts)
        val b = abacus(1, 29).rulesBookSetup()
        assertTrue(b.tenTable)
        assertTrue(b.beadTable)
        assertEquals(Triple(v, v, v), Triple(b.divider1, b.divider2, b.divider3))
        val c = abacus(1, 12).rulesBookSetup()
        assertEquals(Triple(inv, inv, inv), Triple(c.divider1, c.divider2, c.divider3))
    }

    @Test
    fun abakusKitabiBolum2Ve3() {
        val a = abacus(2, 6).rulesBookSetup()
        assertEquals(LessonRulesSection.EXTRACTION, a.section)
        assertTrue(a.extractionFiveTable)
        assertFalse(a.tenExtractionTable)
        assertFalse(a.beadExtractionTable)
        assertTrue(a.subtractionHeaderTexts)
        assertEquals(Triple(inv, inv, inv), Triple(a.divider1, a.divider2, a.divider3))
        val b = abacus(2, 12).rulesBookSetup()
        assertTrue(b.tenExtractionTable)
        assertFalse(b.beadExtractionTable)
        assertEquals(Triple(v, v, inv), Triple(b.divider1, b.divider2, b.divider3))
        val c = abacus(2, 16).rulesBookSetup()
        assertTrue(c.beadExtractionTable)
        assertEquals(Triple(v, v, v), Triple(c.divider1, c.divider2, c.divider3))
        val d = abacus(3, 1).rulesBookSetup()
        assertEquals(LessonRulesSection.ADDITION, d.section)
        assertTrue(d.multiplicationTables)
        assertTrue(d.tablePickerEnabled)
        assertTrue(d.tenTable && d.beadTable)
        assertEquals(Triple(v, v, v), Triple(d.divider1, d.divider2, d.divider3))
    }

    @Test
    fun korKitabi() {
        val a = blinding(5, 3).rulesBookSetup()
        assertEquals(LessonRulesSection.EXTRACTION, a.section)
        assertTrue(a.tablePickerEnabled)
        assertFalse(a.multiplicationTables)
        assertEquals(Triple(v, v, v), Triple(a.divider1, a.divider2, a.divider3))
        val b = blinding(6, 3).rulesBookSetup()
        assertEquals(LessonRulesSection.ADDITION, b.section)
        assertTrue(b.multiplicationTables)
        val c = blinding(1, 1, dailyPart = 2).rulesBookSetup()
        assertEquals(LessonRulesSection.EXTRACTION, c.section)
        val e = blinding(1, 1, dailyPart = 3).rulesBookSetup()
        assertTrue(e.multiplicationTables)
        // Seçicisiz kör ders: sıra bölücüleri; bölüm 3 dalı abaküse özgü.
        val f = blinding(3, 1).rulesBookSetup()
        assertFalse(f.tablePickerEnabled)
        assertEquals(Triple(inv, inv, inv), Triple(f.divider1, f.divider2, f.divider3))
        val g = blinding(2, 12).rulesBookSetup()
        assertEquals(LessonRulesSection.ADDITION, g.section)
        assertTrue(g.subtractionHeaderTexts)
        assertEquals(Triple(v, v, inv), Triple(g.divider1, g.divider2, g.divider3))
    }
}

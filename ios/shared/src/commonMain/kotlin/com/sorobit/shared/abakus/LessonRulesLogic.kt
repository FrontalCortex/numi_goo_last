// Kaynak (app/src/main/java/com/example/app/):
//  - AbacusFragment.kt: updateActiveRulesPanelTable, applyRulesPanelFiveText, rulesBookVisibility,
//    rulesBookSetup, applyRulesDividersVisibility, changeRulesTableText
//  - BlindingLessonFragment.kt: updateActiveRulesPanelTable, rulesPanelFiveIsSubtraction,
//    rulesBookVisibility, rulesBookSetup, applyRulesDividersVisibility, changeRulesTableText
//  - abacus/LessonRulesPanel.kt (Table), model/RulesFragment.kt (RulesContentSection,
//    applyRulesTableHeaderTexts, kitabın açılıştaki ham durumu)
//
// Ders içi kural paneli ve kural kitabının görünümden bağımsız kuralları: hangi ders hangi panel
// tablosunu gösterir, kitap ve sihirli değnek düğmeleri görünür mü, kitap hangi tabloları açar.
// Android bu kuralları Fragment'larda görünümlere doğrudan yazıyor; burada aynı `when`/`if` tabloları
// sonuç değeri olarak döner. Abaküs dersinde rulesBookVisibility + rulesBookSetup'ın sırayla
// uygulanmasının son hâli tek değere indirildi (aşağıdaki yorumlar).
package com.sorobit.shared.abakus

/** Panelde gösterilecek tablo. Android: LessonRulesPanel.Table (AbacusFragment.RulesPanelTableType). */
enum class LessonRulesTable {
    NONE, FIVE, TEN_FIVE, TEN, BEAD, BEAD_EXTRACTION, TEN_EXTRACTION, MULTIPLICATION
}

/** Android View.VISIBLE / View.INVISIBLE (yer tutar, çizilmez) / View.GONE. */
enum class LessonRulesVisibility { VISIBLE, INVISIBLE, GONE }

/** Kitabın içerik bölümü. Android: RulesFragment.RulesContentSection. */
enum class LessonRulesSection { ADDITION, EXTRACTION }

/**
 * Kural kitabının bu derste açacağı tablolar. Varsayılan (RulesFragment.onViewCreated): toplama
 * bölümü, bütün toplama/çıkarma tabloları görünür, çarpım tabloları kapalı, bölücüler görünür.
 *
 * @property subtractionHeaderTexts changeRulesTableText → applyRulesTableHeaderTexts(globalPartId == 2):
 *   true ise sol başlık hücreleri `ui_cikarilacak_sayi`, değilse `ui_eklenecek_sayi_2`.
 */
data class LessonRulesBookSetup(
    val section: LessonRulesSection,
    val fiveTable: Boolean,
    val tenFiveTable: Boolean,
    val tenTable: Boolean,
    val beadTable: Boolean,
    val extractionFiveTable: Boolean,
    val tenExtractionTable: Boolean,
    val beadExtractionTable: Boolean,
    val multiplicationTables: Boolean,
    val divider1: LessonRulesVisibility,
    val divider2: LessonRulesVisibility,
    val divider3: LessonRulesVisibility,
    val tablePickerEnabled: Boolean,
    val subtractionHeaderTexts: Boolean,
)

/**
 * Bir ders ekranının kural kuralları için bildiği her şey.
 *
 * @property globalPartId GlobalLessonData.globalPartId.
 * @property mapFragmentIndex LessonItem.mapFragmentIndex (haritadaki sıra).
 * @property isBlinding true: BlindingLessonFragment (kör ders), false: AbacusFragment.
 * @property isDailyQuestionMode günlük soru modu (kör ders).
 * @property dailyQuestionPartId günlük soruda daily_part_id; günlük değilse yok sayılır.
 * @property signedOut AbacusFragment: FirebaseAuth.currentUser == null → iki düğme de GONE.
 */
data class LessonRulesLesson(
    val globalPartId: Int,
    val mapFragmentIndex: Int,
    val isBlinding: Boolean,
    val isDailyQuestionMode: Boolean,
    val dailyQuestionPartId: Int,
    val signedOut: Boolean,
) {

    /**
     * Panel tablosu çocuğun kitaptan seçimine göre mi. Kör ders: BlindingLessonRules.usesRulesTablePicker
     * (bölüm 4..9 ya da günlük soru); abaküs: globalPartId == 3.
     */
    fun usesRulesTablePicker(): Boolean =
        if (isBlinding) {
            BlindingLessonRules.usesRulesTablePicker(globalPartId, isDailyQuestionMode)
        } else {
            globalPartId == 3
        }

    /**
     * rulesBookButton. Kör ders: rulesBookVisibility yalnızca seçicide VISIBLE yapar (XML'de GONE).
     * Abaküs: rulesBookVisibility (hesapsız GONE; bölüm 1 sıra < 12 GONE, < 17 INVISIBLE; bölüm 2
     * sıra < 5 GONE) ardından rulesBookSetup'ın VISIBLE yapmaları.
     */
    fun rulesBookButtonVisibility(): LessonRulesVisibility {
        if (isBlinding) {
            return if (usesRulesTablePicker()) LessonRulesVisibility.VISIBLE else LessonRulesVisibility.GONE
        }
        val index = mapFragmentIndex
        if (signedOut) return LessonRulesVisibility.GONE
        if (globalPartId == 1 && index < 12) return LessonRulesVisibility.GONE
        if (globalPartId == 1 && index < 17) return LessonRulesVisibility.INVISIBLE
        if (globalPartId == 2 && index < 5) return LessonRulesVisibility.GONE
        return LessonRulesVisibility.VISIBLE
    }

    /** rulesPanelButton (sihirli değnek). Kurallar [rulesBookButtonVisibility] ile aynı kaynaktan. */
    fun rulesPanelButtonVisibility(): LessonRulesVisibility {
        if (isBlinding) {
            return if (usesRulesTablePicker()) LessonRulesVisibility.VISIBLE else LessonRulesVisibility.GONE
        }
        val index = mapFragmentIndex
        if (signedOut) return LessonRulesVisibility.GONE
        if (globalPartId == 1 && index < 12) return LessonRulesVisibility.GONE
        if (globalPartId == 2 && index < 5) return LessonRulesVisibility.GONE
        return LessonRulesVisibility.VISIBLE
    }

    /**
     * updateActiveRulesPanelTable: seçici yokken dersin sırasına göre tablo. Seçicili derste Android
     * erken döner (tablo kitaptan seçilir); burada NONE.
     * Kör derste bölüm 2'nin boncuk çıkarma dalı yoktur (Android'de de böyle).
     */
    fun activeRulesPanelTable(): LessonRulesTable {
        if (usesRulesTablePicker()) return LessonRulesTable.NONE
        val index = mapFragmentIndex
        return when {
            index > 28 && globalPartId == 1 -> LessonRulesTable.BEAD
            index > 22 && globalPartId == 1 -> LessonRulesTable.TEN
            index > 16 && globalPartId == 1 -> LessonRulesTable.TEN_FIVE
            index >= 12 && globalPartId == 1 -> LessonRulesTable.FIVE
            !isBlinding && index > 15 && globalPartId == 2 -> LessonRulesTable.BEAD_EXTRACTION
            index > 10 && globalPartId == 2 -> LessonRulesTable.TEN_EXTRACTION
            index > 5 && globalPartId == 2 -> LessonRulesTable.FIVE
            else -> LessonRulesTable.NONE
        }
    }

    /**
     * 5'li panel tablosunun başlığı çıkarma diliyle mi (`ui_cikarilacak_sayi` + `ui_5_gider_kardes_gelir`;
     * değilse `ui_eklenecek_sayi` + `ui_5_gelir_kardes_gider`). Android: applyRulesPanelFiveText /
     * rulesPanelFiveIsSubtraction.
     */
    fun rulesPanelFiveIsSubtraction(): Boolean =
        (globalPartId == 2 && mapFragmentIndex > 5) || globalPartId == 5

    /** Kitabın bu derste açacağı tablolar: rulesBookSetup + applyRulesDividersVisibility + changeRulesTableText. */
    fun rulesBookSetup(): LessonRulesBookSetup =
        if (isBlinding) blindingBookSetup() else abacusBookSetup()

    private fun abacusBookSetup(): LessonRulesBookSetup {
        val p = globalPartId
        val i = mapFragmentIndex
        var section = LessonRulesSection.ADDITION
        var ten = true
        var bead = true
        var tenExtraction = true
        var beadExtraction = true
        var multiplication = false
        var picker = false
        if (p == 1 && i > 16) {
            section = LessonRulesSection.ADDITION
            ten = false
            bead = false
        }
        if (p == 1 && i > 22) ten = true
        if (p == 1 && i > 28) bead = true
        if (p == 2 && i > 5) {
            section = LessonRulesSection.EXTRACTION
            tenExtraction = false
            beadExtraction = false
        }
        if (p == 2 && i > 10) tenExtraction = true
        if (p == 2 && i > 15) beadExtraction = true
        if (p == 3) {
            section = LessonRulesSection.ADDITION
            multiplication = true
            picker = true
        }
        val d = dividers()
        return LessonRulesBookSetup(
            section = section,
            fiveTable = true,
            tenFiveTable = true,
            tenTable = ten,
            beadTable = bead,
            extractionFiveTable = true,
            tenExtractionTable = tenExtraction,
            beadExtractionTable = beadExtraction,
            multiplicationTables = multiplication,
            divider1 = d.first,
            divider2 = d.second,
            divider3 = d.third,
            tablePickerEnabled = picker,
            subtractionHeaderTexts = p == 2,
        )
    }

    private fun blindingBookSetup(): LessonRulesBookSetup {
        val p = globalPartId
        val daily = isDailyQuestionMode
        val dailyPart = dailyQuestionPartId
        val picker = usesRulesTablePicker()
        // applyRulesDividersVisibility: seçicide (LessonRulesPanel.enableTablePicker) hepsi VISIBLE.
        var d = if (picker) {
            Triple(LessonRulesVisibility.VISIBLE, LessonRulesVisibility.VISIBLE, LessonRulesVisibility.VISIBLE)
        } else {
            dividers()
        }
        var section = LessonRulesSection.ADDITION
        var multiplication = false
        if (p == 5 || p == 8 || (daily && dailyPart == 2)) {
            section = LessonRulesSection.EXTRACTION
        }
        if (p == 6 || (daily && (dailyPart == 1 || dailyPart == 3))) {
            section = LessonRulesSection.ADDITION
            multiplication = true
            d = Triple(LessonRulesVisibility.VISIBLE, LessonRulesVisibility.VISIBLE, LessonRulesVisibility.VISIBLE)
        }
        return LessonRulesBookSetup(
            section = section,
            fiveTable = true,
            tenFiveTable = true,
            tenTable = true,
            beadTable = true,
            extractionFiveTable = true,
            tenExtractionTable = true,
            beadExtractionTable = true,
            multiplicationTables = multiplication,
            divider1 = d.first,
            divider2 = d.second,
            divider3 = d.third,
            tablePickerEnabled = picker,
            subtractionHeaderTexts = p == 2,
        )
    }

    /** applyRulesDividersVisibility (view1, view2, view3); kör derste bölüm 3 dalı yoktur. */
    private fun dividers(): Triple<LessonRulesVisibility, LessonRulesVisibility, LessonRulesVisibility> {
        val p = globalPartId
        val index = mapFragmentIndex
        val v = LessonRulesVisibility.VISIBLE
        val inv = LessonRulesVisibility.INVISIBLE
        return when {
            p == 1 && index > 11 -> Triple(
                if (index > 16) v else inv,
                if (index > 22) v else inv,
                if (index > 28) v else inv,
            )
            p == 2 && index in 11..15 -> Triple(v, v, inv)
            p == 2 && index in 16..20 -> Triple(v, v, v)
            !isBlinding && p == 3 -> Triple(v, v, v)
            else -> Triple(inv, inv, inv)
        }
    }
}

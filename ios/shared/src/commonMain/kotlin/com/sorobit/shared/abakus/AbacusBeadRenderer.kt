// Kaynak (Android): app/src/main/java/com/example/app/abacus/AbacusBeadRenderer.kt
//
// Boncuk çiziminin görünümden bağımsız kısmı: tip → kaynak (drawable) adı seçimi, özgün renk
// eşlemeleri, piksel düzeyinde renk değiştirme algoritması ve renk yardımcıları (HSV).
// Bitmap/Drawable/Canvas üretimi ve bitmap önbelleği platform katmanında kalır.
//
// Android'den farklar:
//  - `R.drawable.x` → "x" kaynak adı String'i.
//  - `Color.HSVToColor` / `Color.colorToHSV` → aynı dosyadaki saf Kotlin karşılıkları
//    (Android bunları Skia'nın SkHSVToColor / SkRGBToHSV işlevlerine devreder; aynı formüller).
//  - Özgün renk eşlemeleri Android'de `private` idi; açıldı.
package com.sorobit.shared.abakus

import com.sorobit.shared.abakus.AbacusPreferences.BeadType
import kotlin.math.floor

object AbacusBeadRenderer {

    // ── Animal colour maps ────────────────────────────────────────────────────

    val ANIMAL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFFFFFFF.toInt() to 0,
        0xFF777873.toInt() to 1,
        0xFF4D4C48.toInt() to 2,
        0xFF32302D.toInt() to 3
    )

    val ANIMAL_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB2B2B2.toInt() to 0,
        0xFF535450.toInt() to 1,
        0xFF353532.toInt() to 2,
        0xFF23211F.toInt() to 3
    )

    // ── Animal 1 (Dog) colour maps ──────────────────────────────────────────────────

    val ANIMAL2_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB76057.toInt() to 0,
        0xFFFBBB8C.toInt() to 1,
        0xFF32302D.toInt() to 2,
        0xFF914844.toInt() to 3,
        0xFFE8A070.toInt() to 4
    )

    val ANIMAL2_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF80433C.toInt() to 0,
        0xFFAF8262.toInt() to 1,
        0xFF23211F.toInt() to 2,
        0xFF65322F.toInt() to 3,
        0xFFA2704E.toInt() to 4
    )

    // ── Animal 3 colour maps ──────────────────────────────────────────────────

    val ANIMAL3_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFF99E4D.toInt() to 0,
        0xFF32302D.toInt() to 1,
        0xFFE58435.toInt() to 2,
        0xFFFDD2B9.toInt() to 3,
        0xFFD36640.toInt() to 4
    )

    val ANIMAL3_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFAE6E35.toInt() to 0,
        0xFF23211F.toInt() to 1,
        0xFFA05C25.toInt() to 2,
        0xFFB19381.toInt() to 3,
        0xFF93472C.toInt() to 4
    )

    // ── Animal 4 colour maps ──────────────────────────────────────────────────

    val ANIMAL4_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFF3703A.toInt() to 0,
        0xFF32302D.toInt() to 1,
        0xFFFFFFFF.toInt() to 2,
        0xFFCC482E.toInt() to 3
    )

    val ANIMAL4_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFAA4E28.toInt() to 0,
        0xFF23211F.toInt() to 1,
        0xFFB2B2B2.toInt() to 2,
        0xFF8E3220.toInt() to 3
    )

    // ── Animal 5 colour maps ──────────────────────────────────────────────────

    val ANIMAL5_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFF525D65.toInt() to 1,
        0xFF7C8B96.toInt() to 2,
        0xFF929FA9.toInt() to 3,
        0xFFA6B3BD.toInt() to 4,
        0xFFEFC9A5.toInt() to 5
    )

    val ANIMAL5_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFF394146.toInt() to 1,
        0xFF566169.toInt() to 2,
        0xFF666F76.toInt() to 3,
        0xFF747D84.toInt() to 4,
        0xFFA78C73.toInt() to 5
    )

    // ── Animal 6 colour maps ──────────────────────────────────────────────────

    val ANIMAL6_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFFD1D3D4.toInt() to 1,
        0xFFFFFFFF.toInt() to 2
    )

    val ANIMAL6_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFF929394.toInt() to 1,
        0xFFB2B2B2.toInt() to 2
    )

    // ── Animal 7 colour maps ──────────────────────────────────────────────────

    val ANIMAL7_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFFF3757A.toInt() to 1,
        0xFFF9E4D7.toInt() to 2,
        0xFFFFF1E7.toInt() to 3,
        0xFFFFFFFF.toInt() to 4
    )

    val ANIMAL7_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFFAA5155.toInt() to 1,
        0xFFAE9F96.toInt() to 2,
        0xFFB2A8A1.toInt() to 3,
        0xFFB2B2B2.toInt() to 4
    )

    // ── Animal 8 colour maps ──────────────────────────────────────────────────

    val ANIMAL8_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFF525D65.toInt() to 1,
        0xFFD69F90.toInt() to 2,
        0xFFFCD3C6.toInt() to 3,
        0xFFFFFFFF.toInt() to 4
    )

    val ANIMAL8_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFF394146.toInt() to 1,
        0xFF956F64.toInt() to 2,
        0xFFB0938A.toInt() to 3,
        0xFFB2B2B2.toInt() to 4
    )

    // ── Animal 9 colour maps ──────────────────────────────────────────────────

    val ANIMAL9_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFF60C198.toInt() to 1,
        0xFFF2A638.toInt() to 2,
        0xFFFFC94E.toInt() to 3
    )

    val ANIMAL9_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFF43876A.toInt() to 1,
        0xFFA97427.toInt() to 2,
        0xFFB28C36.toInt() to 3
    )

    // ── Bowling colour maps ───────────────────────────────────────────────────

    val BOWLING_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF5A648C.toInt() to 0, // Ball Base
        0xFF4B547A.toInt() to 1, // Shadows/Holes
        0xFF7585B3.toInt() to 2, // Highlight
        0xFF383E61.toInt() to 3, // Stroke
        0xFF404A73.toInt() to 4  // Holes Inner
    )

    val BOWLING_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF323A56.toInt() to 0,
        0xFF272C44.toInt() to 1,
        0xFF404A66.toInt() to 2,
        0xFF1D2033.toInt() to 3,
        0xFF21263A.toInt() to 4
    )

    // ── Ball1 colour maps ───────────────────────────────────────────────────

    val BALL1_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB8DB6A.toInt() to 0,
        0xFFF7FFAE.toInt() to 1,
        0xFF92BF62.toInt() to 2
    )

    val BALL1_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF8AA44F.toInt() to 0,
        0xFFB9BF82.toInt() to 1,
        0xFF6D8F49.toInt() to 2
    )

    // ── Ball3 colour maps ───────────────────────────────────────────────────

    val BALL3_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF2D4049.toInt() to 0,
        0xFFFFFFFF.toInt() to 1,
        0xFF1D272B.toInt() to 2,
        0xFF1B2A30.toInt() to 3
    )

    val BALL3_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF213036.toInt() to 0,
        0xFFBFBFBF.toInt() to 1,
        0xFF151D20.toInt() to 2,
        0xFF141F24.toInt() to 3
    )

    // ── Ball4 colour maps ───────────────────────────────────────────────────

    val BALL4_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFF7845C.toInt() to 0,
        0xFFAA5943.toInt() to 1,
        0xFF873F2E.toInt() to 2,
        0xFFDD694A.toInt() to 3
    )

    val BALL4_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB96345.toInt() to 0,
        0xFF7F4232.toInt() to 1,
        0xFF652F22.toInt() to 2,
        0xFFA54E37.toInt() to 3
    )

    // ── Ball5 colour maps ───────────────────────────────────────────────────

    val BALL5_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFDDE8ED.toInt() to 0,
        0xFF2D4049.toInt() to 1,
        0xFFC5D7DD.toInt() to 2,
        0xFF1D2E35.toInt() to 3
    )

    val BALL5_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFA5AEB1.toInt() to 0,
        0xFF213036.toInt() to 1,
        0xFF93A1A5.toInt() to 2,
        0xFF152227.toInt() to 3
    )

    // ── Ball6 colour maps ───────────────────────────────────────────────────

    val BALL6_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB22D34.toInt() to 0,
        0xFF512A2E.toInt() to 1,
        0xFF932A34.toInt() to 2,
        0xFF381D21.toInt() to 3
    )

    val BALL6_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF852127.toInt() to 0,
        0xFF3C1F22.toInt() to 1,
        0xFF6E1F27.toInt() to 2,
        0xFF2A1518.toInt() to 3
    )

    // ── Ball7 colour maps ───────────────────────────────────────────────────

    val BALL7_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFECECEC.toInt() to 0,
        0xFFDDDDDD.toInt() to 1,
        0xFFFFFFFF.toInt() to 2
    )

    val BALL7_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB1B1B1.toInt() to 0,
        0xFFA5A5A5.toInt() to 1,
        0xFFBFBFBF.toInt() to 2
    )

    // ── Ball8 colour maps ───────────────────────────────────────────────────

    val BALL8_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF7F5B53.toInt() to 0,
        0xFFD8D8D8.toInt() to 1,
        0xFF44302D.toInt() to 2,
        0xFFFFFFFF.toInt() to 3
    )

    val BALL8_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF5F443E.toInt() to 0,
        0xFFA2A2A2.toInt() to 1,
        0xFF332421.toInt() to 2,
        0xFFBFBFBF.toInt() to 3
    )

    // ── Ball9 colour maps ───────────────────────────────────────────────────

    val BALL9_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFECECEC.toInt() to 0,
        0xFFEDD314.toInt() to 1,
        0xFF365FC6.toInt() to 2,
        0xFF274789.toInt() to 3,
        0xFFFFFFFF.toInt() to 4
    )

    val BALL9_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB1B1B1.toInt() to 0,
        0xFFB19E0F.toInt() to 1,
        0xFF284794.toInt() to 2,
        0xFF1D3566.toInt() to 3,
        0xFFBFBFBF.toInt() to 4
    )


    val SOROBAN6_ORIGINAL_MAP = mapOf(
        0xFF9AE2C6.toInt() to 0,
        0xFFFFFFFF.toInt() to 1,
        0xFF005533.toInt() to 2
    )
    val SOROBAN6_SEL_ORIGINAL_MAP = mapOf(
        0xFF9AE2C6.toInt() to 0,
        0xFFFFFFFF.toInt() to 1,
        0xFF005533.toInt() to 2
    )

    // ── Balls 10-14 colour maps ───────────────────────────────────────────────

    val BALL10_ORIGINAL_MAP = mapOf(0xFFECECEC.toInt() to 0, 0xFFCECECE.toInt() to 1, 0xFF878783.toInt() to 2, 0xFF373735.toInt() to 3, 0xFFFFFFFF.toInt() to 4)
    val BALL10_SEL_ORIGINAL_MAP = mapOf(0xFFB1B1B1.toInt() to 0, 0xFF9A9A9A.toInt() to 1, 0xFF656562.toInt() to 2, 0xFF292927.toInt() to 3, 0xFFBFBFBF.toInt() to 4)

    val BALL11_ORIGINAL_MAP = mapOf(0xFFF76D2F.toInt() to 0, 0xFFE0602D.toInt() to 1, 0xFF583A2A.toInt() to 2, 0xFFFFFFFF.toInt() to 3)
    val BALL11_SEL_ORIGINAL_MAP = mapOf(0xFFB95123.toInt() to 0, 0xFFA84821.toInt() to 1, 0xFF422B1F.toInt() to 2, 0xFFBFBFBF.toInt() to 3)

    val BALL12_ORIGINAL_MAP = mapOf(0xFFE0EC44.toInt() to 0, 0xFFFFFFFF.toInt() to 1, 0xFFEAEAEA.toInt() to 2, 0xFFD8D844.toInt() to 3, 0xFFDBDBDB.toInt() to 4)
    val BALL12_SEL_ORIGINAL_MAP = mapOf(0xFFA8B133.toInt() to 0, 0xFFBFBFBF.toInt() to 1, 0xFFAFAFAF.toInt() to 2, 0xFFA2A233.toInt() to 3, 0xFFA4A4A4.toInt() to 4)

    val BALL13_ORIGINAL_MAP = mapOf(0xFFECECEC.toInt() to 0, 0xFFCECECE.toInt() to 1, 0xFFBC3F3D.toInt() to 2, 0xFFFFFFFF.toInt() to 3)
    val BALL13_SEL_ORIGINAL_MAP = mapOf(0xFFB1B1B1.toInt() to 0, 0xFF9A9A9A.toInt() to 1, 0xFF8D2F2D.toInt() to 2, 0xFFBFBFBF.toInt() to 3)

    val BALL14_ORIGINAL_MAP = mapOf(0xFF010102.toInt() to 0, 0xFF3C3C3C.toInt() to 1, 0xFFFFFFFF.toInt() to 2)
    val BALL14_SEL_ORIGINAL_MAP = mapOf(0xFF000001.toInt() to 0, 0xFF2D2D2D.toInt() to 1, 0xFFBFBFBF.toInt() to 2)

    // Tolerance for pixel colour matching (Manhattan distance across R+G+B)
    const val COLOR_TOLERANCE = 150

    // Render size cap for rabbit bitmap (dp). Keeps memory usage low.
    const val RABBIT_RENDER_DP = 120

    // ── Tip → kaynak adı / özgün renk eşlemesi ────────────────────────────────

    /** Boncuğun nasıl çizildiği (Android'deki build*Drawable ailelerine karşılık gelir). */
    enum class BeadRenderKind {
        /** 3 katmanlı LayerDrawable; her katman SRC_IN ile boyanır (buildSorobanDrawable). */
        TINTED_LAYERS,
        /** Kaynak rasterleştirilir, [replaceBeadPixels] ile renkler değiştirilir. */
        REPLACED_BITMAP,
        /** Kaynak yok; hap biçimi + 5 duraklı doğrusal gradyan çizilir (buildSoroban2Drawable). */
        GRADIENT_PILL,
    }

    fun renderKind(type: BeadType): BeadRenderKind = when (type) {
        BeadType.SOROBAN -> BeadRenderKind.TINTED_LAYERS
        BeadType.SOROBAN2 -> BeadRenderKind.GRADIENT_PILL
        else -> BeadRenderKind.REPLACED_BITMAP
    }

    /** buildSorobanDrawable'ın katman kaynakları (sıra: dış kabuk → iç dolgu → sağ parlama). */
    fun sorobanLayerResNames(selected: Boolean): Array<String> =
        if (selected) {
            arrayOf("soroban_bead_selected_layer1", "soroban_bead_selected_layer2", "soroban_bead_selected_layer3")
        } else {
            arrayOf("soroban_bead_layer1", "soroban_bead_layer2", "soroban_bead_layer3")
        }

    /**
     * REPLACED_BITMAP tipleri için kaynak (drawable) adı; SOROBAN ve SOROBAN2 için null
     * (onlar tek kaynaktan çizilmez).
     */
    fun beadResName(type: BeadType, selected: Boolean): String? = when (type) {
        BeadType.SOROBAN, BeadType.SOROBAN2 -> null
        BeadType.ANIMAL -> if (selected) "animal_head_selected_ic1" else "animal_head_ic1"
        BeadType.ANIMAL2 -> if (selected) "animal_head_selected_ic2" else "animal_head_ic2"
        BeadType.ANIMAL3 -> if (selected) "animal_head_selected_ic3" else "animal_head_ic3"
        BeadType.ANIMAL4 -> if (selected) "animal_head_selected_ic4" else "animal_head_ic4"
        BeadType.ANIMAL5 -> if (selected) "animal_head_selected_ic5" else "animal_head_ic5"
        BeadType.ANIMAL6 -> if (selected) "animal_head_selected_ic6" else "animal_head_ic6"
        BeadType.ANIMAL7 -> if (selected) "animal_head_selected_ic7" else "animal_head_ic7"
        BeadType.ANIMAL8 -> if (selected) "animal_head_selected_ic8" else "animal_head_ic8"
        BeadType.ANIMAL9 -> if (selected) "animal_head_selected_ic9" else "animal_head_ic9"
        BeadType.BOWLING -> if (selected) "bowling_ball_selected_ic" else "bowling_ball_ic"
        BeadType.BALL1 -> if (selected) "abacus_bead_selected_ball1" else "abacus_bead_ball1"
        BeadType.BALL3 -> if (selected) "abacus_bead_selected_ball3" else "abacus_bead_ball3"
        BeadType.BALL4 -> if (selected) "abacus_bead_selected_ball4" else "abacus_bead_ball4"
        BeadType.BALL5 -> if (selected) "abacus_bead_selected_ball5" else "abacus_bead_ball5"
        BeadType.BALL6 -> if (selected) "abacus_bead_selected_ball6" else "abacus_bead_ball6"
        BeadType.BALL7 -> if (selected) "abacus_bead_selected_ball7" else "abacus_bead_ball7"
        BeadType.BALL8 -> if (selected) "abacus_bead_selected_ball8" else "abacus_bead_ball8"
        BeadType.BALL9 -> if (selected) "abacus_bead_selected_ball9" else "abacus_bead_ball9"
        BeadType.BALL10 -> if (selected) "abacus_bead_selected_ball10" else "abacus_bead_ball10"
        BeadType.BALL11 -> if (selected) "abacus_bead_selected_ball11" else "abacus_bead_ball11"
        BeadType.BALL12 -> if (selected) "abacus_bead_selected_ball12" else "abacus_bead_ball12"
        BeadType.BALL13 -> if (selected) "abacus_bead_selected_ball13" else "abacus_bead_ball13"
        BeadType.BALL14 -> if (selected) "abacus_bead_selected_ball14" else "abacus_bead_ball14"
        BeadType.SOROBAN6 -> if (selected) "soroban_bead6_selected" else "soroban_bead6"
    }

    /** REPLACED_BITMAP tipleri için "özgün renk → renk dizini" eşlemesi; diğerleri için null. */
    fun originalColorMap(type: BeadType, selected: Boolean): Map<Int, Int>? = when (type) {
        BeadType.SOROBAN, BeadType.SOROBAN2 -> null
        BeadType.ANIMAL -> if (selected) ANIMAL_SEL_ORIGINAL_MAP else ANIMAL_ORIGINAL_MAP
        BeadType.ANIMAL2 -> if (selected) ANIMAL2_SEL_ORIGINAL_MAP else ANIMAL2_ORIGINAL_MAP
        BeadType.ANIMAL3 -> if (selected) ANIMAL3_SEL_ORIGINAL_MAP else ANIMAL3_ORIGINAL_MAP
        BeadType.ANIMAL4 -> if (selected) ANIMAL4_SEL_ORIGINAL_MAP else ANIMAL4_ORIGINAL_MAP
        BeadType.ANIMAL5 -> if (selected) ANIMAL5_SEL_ORIGINAL_MAP else ANIMAL5_ORIGINAL_MAP
        BeadType.ANIMAL6 -> if (selected) ANIMAL6_SEL_ORIGINAL_MAP else ANIMAL6_ORIGINAL_MAP
        BeadType.ANIMAL7 -> if (selected) ANIMAL7_SEL_ORIGINAL_MAP else ANIMAL7_ORIGINAL_MAP
        BeadType.ANIMAL8 -> if (selected) ANIMAL8_SEL_ORIGINAL_MAP else ANIMAL8_ORIGINAL_MAP
        BeadType.ANIMAL9 -> if (selected) ANIMAL9_SEL_ORIGINAL_MAP else ANIMAL9_ORIGINAL_MAP
        BeadType.BOWLING -> if (selected) BOWLING_SEL_ORIGINAL_MAP else BOWLING_ORIGINAL_MAP
        BeadType.BALL1 -> if (selected) BALL1_SEL_ORIGINAL_MAP else BALL1_ORIGINAL_MAP
        BeadType.BALL3 -> if (selected) BALL3_SEL_ORIGINAL_MAP else BALL3_ORIGINAL_MAP
        BeadType.BALL4 -> if (selected) BALL4_SEL_ORIGINAL_MAP else BALL4_ORIGINAL_MAP
        BeadType.BALL5 -> if (selected) BALL5_SEL_ORIGINAL_MAP else BALL5_ORIGINAL_MAP
        BeadType.BALL6 -> if (selected) BALL6_SEL_ORIGINAL_MAP else BALL6_ORIGINAL_MAP
        BeadType.BALL7 -> if (selected) BALL7_SEL_ORIGINAL_MAP else BALL7_ORIGINAL_MAP
        BeadType.BALL8 -> if (selected) BALL8_SEL_ORIGINAL_MAP else BALL8_ORIGINAL_MAP
        BeadType.BALL9 -> if (selected) BALL9_SEL_ORIGINAL_MAP else BALL9_ORIGINAL_MAP
        BeadType.BALL10 -> if (selected) BALL10_SEL_ORIGINAL_MAP else BALL10_ORIGINAL_MAP
        BeadType.BALL11 -> if (selected) BALL11_SEL_ORIGINAL_MAP else BALL11_ORIGINAL_MAP
        BeadType.BALL12 -> if (selected) BALL12_SEL_ORIGINAL_MAP else BALL12_ORIGINAL_MAP
        BeadType.BALL13 -> if (selected) BALL13_SEL_ORIGINAL_MAP else BALL13_ORIGINAL_MAP
        BeadType.BALL14 -> if (selected) BALL14_SEL_ORIGINAL_MAP else BALL14_ORIGINAL_MAP
        BeadType.SOROBAN6 -> if (selected) SOROBAN6_SEL_ORIGINAL_MAP else SOROBAN6_ORIGINAL_MAP
    }

    /** Android: prewarmAllSlotDrawables içindeki "kullanılan tipler" kümesi (25 yuva + genel tip). */
    fun usedBeadTypes(store: AbacusPrefsStore, uid: String?): Set<BeadType> {
        val types = mutableSetOf(AbacusPreferences.getBeadType(store, uid))
        for (rod in 0..4) {
            types.add(AbacusPreferences.getBeadTypeForSlot(store, uid, rod, isTop = true))
            for (i in 0..3) {
                types.add(AbacusPreferences.getBeadTypeForSlot(store, uid, rod, isTop = false, beadIndex = i))
            }
        }
        return types
    }

    // ── Rasterleştirme ölçüleri ───────────────────────────────────────────────

    /** Bitmap önbelleğinin LRU sınırı (Android: MAX_BITMAP_CACHE_ENTRIES). */
    const val MAX_BITMAP_CACHE_ENTRIES = 48

    /** REPLACED_BITMAP hedef genişliği (px): `(RABBIT_RENDER_DP * density).toInt()`, en az 1. */
    fun replacedBitmapTargetWidth(density: Float): Int = (RABBIT_RENDER_DP * density).toInt().coerceAtLeast(1)

    /** REPLACED_BITMAP hedef yüksekliği: kaynağın en-boy oranı korunur. */
    fun replacedBitmapTargetHeight(targetW: Int, intrinsicWidth: Int, intrinsicHeight: Int): Int {
        val intrinsicW = intrinsicWidth.coerceAtLeast(1)
        val intrinsicH = intrinsicHeight.coerceAtLeast(1)
        return (targetW.toFloat() * intrinsicH / intrinsicW).toInt().coerceAtLeast(1)
    }

    /** Android'deki bitmap önbellek anahtarı: `srcRes` tohumu üzerine 31'li karma. */
    fun bitmapCacheKey(seed: Int, newColors: IntArray): Int {
        var cacheKey = seed
        for (c in newColors) {
            cacheKey = 31 * cacheKey + c
        }
        return cacheKey
    }

    /**
     * buildSoroban2Drawable'ın geometri değerleri (hepsi px).
     * DİKKAT (Android davranışı): yükseklik 92/120 oranıyla, dikey ölçek ise 72'ye bölünerek
     * hesaplanır (tutarsız görünür ama aynen korunmuştur).
     */
    data class Soroban2Geometry(
        val targetW: Int,
        val targetH: Int,
        val outerRx: Float,
        val outerRy: Float,
        val padX: Float,
        val padY: Float,
        val innerRx: Float,
        val innerRy: Float,
    )

    fun soroban2Geometry(density: Float): Soroban2Geometry {
        val targetW = (RABBIT_RENDER_DP * density).toInt().coerceAtLeast(1)
        val targetH = (targetW * (92f / 120f)).toInt().coerceAtLeast(1)
        val scaleX = targetW / 120f
        val scaleY = targetH / 72f
        return Soroban2Geometry(
            targetW = targetW,
            targetH = targetH,
            outerRx = 36f * scaleX,
            outerRy = 36f * scaleY,
            padX = 12f * scaleX,
            padY = 12f * scaleY,
            innerRx = 24f * scaleX,
            innerRy = 24f * scaleY,
        )
    }

    /** Soroban2 gradyanı: (0, H) → (W, 0) doğrultusunda, bu duraklarla, CLAMP. Dış hap siyahtır. */
    val SOROBAN2_GRADIENT_STOPS = floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)

    /** Soroban2 önbellek anahtarı tohumu (Android: `if (selected) 2 else 1`). */
    fun soroban2CacheSeed(selected: Boolean): Int = if (selected) 2 else 1

    // ── Piksel renk değiştirme (buildReplacedBitmapDrawable'ın saf kısmı) ─────

    /**
     * [pixels] (ARGB, satır satır, [targetW] × [targetH]) dizisini YERİNDE değiştirir.
     *
     * 1. geçiş: her piksel, özgün renklere ya da iki özgün renk arasındaki doğru parçasına
     *    (RGB uzayında) en yakın olana eşlenir ve yeni renk(ler)le aynı oranda harmanlanır.
     * 2. geçiş: kenardan taşma dolgusuyla gerçek arka plan bulunur; arka plan OLMAYAN yarı saydam
     *    pikseller (iç SVG dikişleri) tam opak yapılır.
     *
     * [originalMap] yineleme sırası önemlidir (eşit puanlarda ilk bulunan kazanır); Android'deki
     * `mapOf` ekleme sırası korunmuştur.
     */
    fun replaceBeadPixels(
        pixels: IntArray,
        targetW: Int,
        targetH: Int,
        originalMap: Map<Int, Int>,
        newColors: IntArray,
    ) {
        val origEntries = originalMap.entries.toList()

        for (idx in pixels.indices) {
            val alpha = pixels[idx] ushr 24
            if (alpha < 10) continue // neredeyse saydam olanları atla

            val pixelArgb = pixels[idx]
            val pr = (pixelArgb ushr 16) and 0xFF
            val pg = (pixelArgb ushr 8) and 0xFF
            val pb = pixelArgb and 0xFF

            var bestScore = Float.MAX_VALUE
            var bestC0 = -1
            var bestC1 = -1
            var bestT = 0f

            for (i in origEntries.indices) {
                val c0 = origEntries[i].key
                val r0 = ((c0 ushr 16) and 0xFF).toFloat()
                val g0 = ((c0 ushr 8) and 0xFF).toFloat()
                val b0 = (c0 and 0xFF).toFloat()

                // Önce düz renge uzaklık
                val d0Sq = (pr - r0) * (pr - r0) + (pg - g0) * (pg - g0) + (pb - b0) * (pb - b0)
                if (d0Sq < bestScore) {
                    bestScore = d0Sq
                    bestC0 = origEntries[i].value
                    bestC1 = origEntries[i].value
                    bestT = 0f
                }

                // Sonra doğru parçalarına uzaklık
                for (j in i + 1 until origEntries.size) {
                    val c1 = origEntries[j].key
                    val r1 = ((c1 ushr 16) and 0xFF).toFloat()
                    val g1 = ((c1 ushr 8) and 0xFF).toFloat()
                    val b1 = (c1 and 0xFF).toFloat()

                    val abR = r1 - r0
                    val abG = g1 - g0
                    val abB = b1 - b0
                    val abLenSq = abR * abR + abG * abG + abB * abB

                    val apR = pr - r0
                    val apG = pg - g0
                    val apB = pb - b0

                    var t = if (abLenSq == 0f) 0f else (apR * abR + apG * abG + apB * abB) / abLenSq
                    t = t.coerceIn(0f, 1f)

                    val projR = r0 + t * abR
                    val projG = g0 + t * abG
                    val projB = b0 + t * abB

                    val distSq = (pr - projR) * (pr - projR) + (pg - projG) * (pg - projG) + (pb - projB) * (pb - projB)

                    // Doğrusal (ör. gri tonlu) parçaları ayırt etmek için parça uzunluğu cezası
                    val score = distSq + 0.001f * abLenSq

                    if (score < bestScore) {
                        bestScore = score
                        bestC0 = origEntries[i].value
                        bestC1 = origEntries[j].value
                        bestT = t
                    }
                }
            }

            // En yakın renk ya da parça koşulsuz uygulanır
            if (bestC0 == bestC1 || bestT <= 0f) {
                val nc = newColors[bestC0]
                pixels[idx] = (alpha shl 24) or (nc and 0x00FFFFFF)
            } else if (bestT >= 1f) {
                val nc = newColors[bestC1]
                pixels[idx] = (alpha shl 24) or (nc and 0x00FFFFFF)
            } else {
                val nc0 = newColors[bestC0]
                val nc1 = newColors[bestC1]

                val nr0 = (nc0 ushr 16) and 0xFF
                val ng0 = (nc0 ushr 8) and 0xFF
                val nb0 = nc0 and 0xFF

                val nr1 = (nc1 ushr 16) and 0xFF
                val ng1 = (nc1 ushr 8) and 0xFF
                val nb1 = nc1 and 0xFF

                val nr = (nr0 + bestT * (nr1 - nr0)).toInt().coerceIn(0, 255)
                val ng = (ng0 + bestT * (ng1 - ng0)).toInt().coerceIn(0, 255)
                val nb = (nb0 + bestT * (nb1 - nb0)).toInt().coerceIn(0, 255)

                val nc = (nr shl 16) or (ng shl 8) or nb
                pixels[idx] = (alpha shl 24) or nc
            }
        }

        // 2. geçiş: kenar piksellerinden taşma dolgusu
        val isBackground = BooleanArray(targetW * targetH) { false }
        val queue = ArrayDeque<Int>()

        for (x in 0 until targetW) {
            for (y in intArrayOf(0, targetH - 1)) {
                val idx = y * targetW + x
                if ((pixels[idx] ushr 24) < 128) { isBackground[idx] = true; queue.add(idx) }
            }
        }
        for (y in 1 until targetH - 1) {
            for (x in intArrayOf(0, targetW - 1)) {
                val idx = y * targetW + x
                if ((pixels[idx] ushr 24) < 128 && !isBackground[idx]) { isBackground[idx] = true; queue.add(idx) }
            }
        }

        val yonler = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val cx = cur % targetW
            val cy = cur / targetW
            for (yon in yonler) {
                val nx = cx + yon[0]
                val ny = cy + yon[1]
                if (nx < 0 || nx >= targetW || ny < 0 || ny >= targetH) continue
                val nIdx = ny * targetW + nx
                if (!isBackground[nIdx] && (pixels[nIdx] ushr 24) < 128) {
                    isBackground[nIdx] = true
                    queue.add(nIdx)
                }
            }
        }

        // Arka plan olmayan yarı saydam pikseller iç dikiştir — alfa 255'e çıkarılır
        for (i in pixels.indices) {
            if (!isBackground[i] && (pixels[i] ushr 24) in 1..254) {
                pixels[i] = (255 shl 24) or (pixels[i] and 0x00FFFFFF)
            }
        }
    }

    // ── Renk yardımcıları (2. sekmedeki kaydırıcı mantığı) ────────────────────

    /**
     * `android.graphics.Color.colorToHSV` karşılığı (Skia SkRGBToHSV): [0] ton 0..360,
     * [1] doygunluk 0..1, [2] değer 0..1. Alfa yok sayılır.
     */
    fun colorToHSV(color: Int, hsv: FloatArray) {
        val r = (color ushr 16) and 0xFF
        val g = (color ushr 8) and 0xFF
        val b = color and 0xFF

        val min = minOf(r, g, b)
        val max = maxOf(r, g, b)
        val delta = max - min

        val v = if (max == 0xFF) 1f else max.toFloat() / 255f

        if (delta == 0) { // gri ton
            hsv[0] = 0f
            hsv[1] = 0f
            hsv[2] = v
            return
        }

        val s = delta.toFloat() / max
        var h = when {
            r == max -> (g - b).toFloat() / delta
            g == max -> 2f + (b - r).toFloat() / delta
            else -> 4f + (r - g).toFloat() / delta
        }
        h *= 60f
        if (h < 0f) h += 360f

        hsv[0] = h
        hsv[1] = s
        hsv[2] = v
    }

    /** Skia SkScalarRoundToInt: `floor(x + 0.5)`. */
    private fun skRound(x: Float): Int = floor(x + 0.5f).toInt()

    /**
     * `android.graphics.Color.HSVToColor(hsv)` karşılığı (Skia SkHSVToColor, alfa = 0xFF).
     * Ton [0, 360) dışındaysa 0 sayılır; doygunluk ve değer [0, 1] aralığına kırpılır.
     */
    fun HSVToColor(hsv: FloatArray): Int {
        val s = hsv[1].coerceIn(0f, 1f)
        val v = hsv[2].coerceIn(0f, 1f)

        val vByte = skRound(v * 255f)

        if (s <= 1f / 4096f) { // SkScalarNearlyZero → gri ton
            return (0xFF shl 24) or (vByte shl 16) or (vByte shl 8) or vByte
        }
        val hx = if (hsv[0] < 0f || hsv[0] >= 360f) 0f else hsv[0] / 60f
        val w = floor(hx)
        val f = hx - w

        val p = skRound((1f - s) * v * 255f)
        val q = skRound((1f - (s * f)) * v * 255f)
        val t = skRound((1f - (s * (1f - f))) * v * 255f)

        val r: Int
        val g: Int
        val b: Int
        when (w.toInt()) {
            0 -> { r = vByte; g = t; b = p }
            1 -> { r = q; g = vByte; b = p }
            2 -> { r = p; g = vByte; b = t }
            3 -> { r = p; g = q; b = vByte }
            4 -> { r = t; g = p; b = vByte }
            else -> { r = vByte; g = p; b = q }
        }
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Ton (0–359) ve açıklıktan (0–100) ARGB renk üretir.
     * 0–50: siyahtan tam renge; 50–100: tam renkten beyaza. Alfa her zaman 0xFF.
     */
    fun makeColor(hueDegrees: Int, lightness100: Int): Int {
        val l = lightness100.coerceIn(0, 100)
        val s = if (l <= 50) FIXED_SATURATION else FIXED_SATURATION * (1f - (l - 50) / 50f)
        val v = if (l <= 50) l / 50f else 1f
        val hsv = floatArrayOf((hueDegrees % 360).toFloat(), s, v)
        return HSVToColor(hsv)
    }

    /** [color]'ın HSV tonu (0–359); renksizlerde 0. */
    fun getHue(color: Int): Int {
        val hsv = FloatArray(3)
        colorToHSV(color, hsv)
        return hsv[0].toInt()
    }

    /** [color]'ın açıklığı (0–100); makeColor gradyanıyla uyumlu. */
    fun getLightness100(color: Int): Int {
        val hsv = FloatArray(3)
        colorToHSV(color, hsv)
        val s = hsv[1]
        val v = hsv[2]
        return if (s < FIXED_SATURATION - 0.05f) {
            (50 + 50 * (1f - s / FIXED_SATURATION)).toInt().coerceIn(50, 100)
        } else {
            (50 * v).toInt().coerceIn(0, 50)
        }
    }

    /**
     * Çerçeve rengi kaydırıcıları için eski tek-kaydırıcı yardımcısı.
     * Tonu kaydırır; değişiklik hep görünür olsun diye doygunluğu/değeri yükseltir. Alfa korunur.
     */
    fun shiftHue(baseColor: Int, hueDegrees: Int, minSaturation: Float = 0.65f): Int {
        val hsv = FloatArray(3)
        colorToHSV(baseColor, hsv)
        hsv[0] = (hueDegrees % 360).toFloat()
        if (hsv[1] < minSaturation) hsv[1] = minSaturation
        if (hsv[2] < 0.12f) hsv[2] = 0.35f
        val rgb = HSVToColor(hsv)
        val alpha = (baseColor ushr 24) and 0xFF
        return (alpha shl 24) or (rgb and 0x00FFFFFF)
    }

    const val FIXED_SATURATION = 0.90f
}

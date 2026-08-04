package com.example.app.abacus

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

/**
 * SharedPreferences-backed store for abacus customization settings.
 * All get/set operations are synchronous (apply() for writes).
 */
object AbacusPreferences {

    private const val PREF_NAME = "abacus_customization"

    // ── Keys ─────────────────────────────────────────────────────────────────
    private const val KEY_BEAD_TYPE = "bead_type"
    private const val KEY_FRAME_TYPE = "frame_type"
    private const val PREFIX_SOROBAN = "soroban_path_"
    private const val PREFIX_SOROBAN_SEL = "soroban_sel_path_"
    private const val PREFIX_RABBIT = "rabbit_c"
    private const val PREFIX_RABBIT_SEL = "rabbit_sel_c"
    private const val PREFIX_ANIMAL = "animal_c"
    private const val PREFIX_ANIMAL_SEL = "animal_sel_c"
    private const val PREFIX_ANIMAL2 = "animal2_c"
    private const val PREFIX_ANIMAL2_SEL = "animal2_sel_c"
    private const val PREFIX_ANIMAL3 = "animal3_c"
    private const val PREFIX_ANIMAL3_SEL = "animal3_sel_c"
    private const val PREFIX_ANIMAL4 = "animal4_c"
    private const val PREFIX_ANIMAL4_SEL = "animal4_sel_c"
    private const val PREFIX_ANIMAL5 = "animal5_c"
    private const val PREFIX_ANIMAL5_SEL = "animal5_sel_c"
    private const val PREFIX_ANIMAL6 = "animal6_c"
    private const val PREFIX_ANIMAL6_SEL = "animal6_sel_c"
    private const val PREFIX_ANIMAL7 = "animal7_c"
    private const val PREFIX_ANIMAL7_SEL = "animal7_sel_c"
    private const val PREFIX_ANIMAL8 = "animal8_c"
    private const val PREFIX_ANIMAL8_SEL = "animal8_sel_c"
    private const val PREFIX_ANIMAL9 = "animal9_c"
    private const val PREFIX_ANIMAL9_SEL = "animal9_sel_c"
    private const val PREFIX_BOWLING = "bowling_c"
    private const val PREFIX_BOWLING_SEL = "bowling_sel_c"
    private const val PREFIX_BALL1 = "ball1_c"
    private const val PREFIX_BALL1_SEL = "ball1_sel_c"
    private const val PREFIX_BALL3 = "ball3_c"
    private const val PREFIX_BALL3_SEL = "ball3_sel_c"
    private const val PREFIX_BALL4 = "ball4_c"
    private const val PREFIX_BALL4_SEL = "ball4_sel_c"
    private const val PREFIX_BALL5 = "ball5_c"
    private const val PREFIX_BALL5_SEL = "ball5_sel_c"
    private const val PREFIX_BALL6 = "ball6_c"
    private const val PREFIX_BALL6_SEL = "ball6_sel_c"
    private const val PREFIX_BALL7 = "ball7_c"
    private const val PREFIX_BALL7_SEL = "ball7_sel_c"
    private const val PREFIX_BALL8 = "ball8_c"
    private const val PREFIX_BALL8_SEL = "ball8_sel_c"
    private const val PREFIX_BALL9 = "ball9_c"
    private const val PREFIX_BALL9_SEL = "ball9_sel_c"
    private const val PREFIX_BALL10 = "ball10_c"
    private const val PREFIX_BALL10_SEL = "ball10_sel_c"
    private const val PREFIX_BALL11 = "ball11_c"
    private const val PREFIX_BALL11_SEL = "ball11_sel_c"
    private const val PREFIX_BALL12 = "ball12_c"
    private const val PREFIX_BALL12_SEL = "ball12_sel_c"
    private const val PREFIX_BALL13 = "ball13_c"
    private const val PREFIX_BALL13_SEL = "ball13_sel_c"
    private const val PREFIX_BALL14 = "ball14_c"
    private const val PREFIX_BALL14_SEL = "ball14_sel_c"
    private const val KEY_FRAME_BG_WOOD = "frame_bg_wood"
    private const val KEY_FRAME_BG2_WOOD = "frame_bg2_wood"
    private const val KEY_FRAME_BG2_HEART = "frame_bg2_heart"
    private const val KEY_FRAME_BG3_WOOD = "frame_bg3_wood"
    private const val KEY_FRAME_BG3_STAR = "frame_bg3_star"
    private const val KEY_FRAME_BG4_C1 = "frame_bg4_c1"
    private const val KEY_FRAME_BG4_C2 = "frame_bg4_c2"
    private const val KEY_FRAME_BG4_C3 = "frame_bg4_c3"
    private const val KEY_FRAME_BG4_C4 = "frame_bg4_c4"
    private const val KEY_FRAME_BG4_C5 = "frame_bg4_c5"

    // ── Enums ─────────────────────────────────────────────────────────────────
    enum class BeadType {
        SOROBAN,
        ANIMAL,
        ANIMAL2,
        ANIMAL3,
        ANIMAL4,
        ANIMAL5,
        ANIMAL6,
        ANIMAL7,
        ANIMAL8,
        ANIMAL9,
        BOWLING,
        BALL1,
        BALL3,
        BALL4,
        BALL5,
        BALL6,
        BALL7,
        BALL8,
        BALL9,
        BALL10,
        BALL11,
        BALL12,
        BALL13,
        BALL14,
        SOROBAN2,
        SOROBAN6
    }
    enum class FrameType { FRAME_BG, FRAME_BG2, FRAME_BG3, FRAME_BG4 }

    // ── Default colors ────────────────────────────────────────────────────────

    /**
     * Default ARGB colors for each soroban_bead.xml path:
     *  [0] outer black shell, [1] inner dark gray, [2] right lighter gray
     */
    val SOROBAN_DEFAULT_COLORS = intArrayOf(
        Color.parseColor("#000000"),
        Color.parseColor("#737373"),
        Color.parseColor("#8C8C8C")
    )

    /**
     * Default ARGB colors for each soroban_bead_selected.xml path:
     *  [0] outer black shell, [1] inner dark red, [2] right medium red
     */
    val SOROBAN_SEL_DEFAULT_COLORS = intArrayOf(
        Color.parseColor("#000000"),
        Color.parseColor("#4D0000"),
        Color.parseColor("#731616")
    )

    /**
     * Rabbit unique fill colors (7 groups):
     *  [0] ears outer #DCD2D1
     *  [1] ears inner #CEADB1
     *  [2] body       #F1E6E6
     *  [3] white areas #FFFFFF  (belly + eye highlights)
     *  [4] nose       #CD92A2
     *  [5] mouth      #FFF6F9
     *  [6] eyes       #332F28
     */
    val RABBIT_DEFAULT_COLORS = intArrayOf(
        Color.parseColor("#DCD2D1"),
        Color.parseColor("#CEADB1"),
        Color.parseColor("#F1E6E6"),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#CD92A2"),
        Color.parseColor("#FFF6F9"),
        Color.parseColor("#332F28")
    )

    /**
     * rabbit_selected_ic.xml unique fill colors (same ordering as RABBIT_DEFAULT_COLORS).
     */
    val RABBIT_SEL_DEFAULT_COLORS = intArrayOf(
        Color.parseColor("#6B3B3F"),
        Color.parseColor("#8B2635"),
        Color.parseColor("#7C4B52"),
        Color.parseColor("#94737A"),
        Color.parseColor("#661426"),
        Color.parseColor("#94737A"),
        Color.parseColor("#1A080B")
    )

    /** Human-readable labels for soroban paths (used in Tab 2 UI). */
    val SOROBAN_PATH_LABELS = arrayOf("Dış çerçeve", "İç zemin", "Sağ yansıma")

    /** Human-readable labels for rabbit color groups (used in Tab 2 UI). */
    val RABBIT_COLOR_LABELS = arrayOf(
        "Kulak (dış)", "Kulak (iç)", "Gövde",
        "Beyaz alanlar", "Burun", "Ağız", "Gözler"
    )

    /**
     * Animal unique fill colors (4 groups):
     *  [0] white areas      #FFFFFF
     *  [1] head base        #777873
     *  [2] snout/ear detail #4D4C48
     *  [3] eyes/nose/mouth  #32302D
     */
    val ANIMAL_DEFAULT_COLORS = intArrayOf(
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#777873"),
        Color.parseColor("#4D4C48"),
        Color.parseColor("#32302D")
    )

    val ANIMAL_SEL_DEFAULT_COLORS = intArrayOf(
        Color.parseColor("#B2B2B2"),
        Color.parseColor("#535450"),
        Color.parseColor("#353532"),
        Color.parseColor("#23211F")
    )

    val ANIMAL_COLOR_LABELS = arrayOf(
        "Kafa", "Göz çevresi", "Kulak/Dudak", "Göz/Ağız"
    )

    /**
     * Animal 2 unique fill colors (5 groups):
     *  [0] Head base        #B76057
     *  [1] Snout/lower face #FBBB8C
     *  [2] Eyes             #32302D
     *  [3] Nose             #914844
     *  [4] Nose bridge      #E8A070
     */
    val ANIMAL2_DEFAULT_COLORS = intArrayOf(
        Color.parseColor("#B76057"),
        Color.parseColor("#FBBB8C"),
        Color.parseColor("#32302D"),
        Color.parseColor("#914844"),
        Color.parseColor("#E8A070")
    )

    val ANIMAL2_SEL_DEFAULT_COLORS = intArrayOf(
        Color.parseColor("#80433C"),
        Color.parseColor("#AF8262"),
        Color.parseColor("#23211F"),
        Color.parseColor("#65322F"),
        Color.parseColor("#A2704E")
    )

    val ANIMAL2_COLOR_LABELS = arrayOf(
        "Kafa", "Yüz", "Gözler", "Burun", "Ağız"
    )

    private val ANIMAL3_COLOR_KEYS = arrayOf(
        "animal3_color_0", "animal3_color_1", "animal3_color_2", "animal3_color_3", "animal3_color_4"
    )

    private val ANIMAL3_DEFAULT_COLORS = intArrayOf(
        0xFFF99E4D.toInt(),
        0xFF32302D.toInt(),
        0xFFE58435.toInt(),
        0xFFFDD2B9.toInt(),
        0xFFD36640.toInt()
    )

    private val ANIMAL3_SEL_DEFAULT_COLORS = intArrayOf(
        0xFFAE6E35.toInt(),
        0xFF23211F.toInt(),
        0xFFA05C25.toInt(),
        0xFFB19381.toInt(),
        0xFF93472C.toInt()
    )

    val ANIMAL3_COLOR_LABELS = arrayOf(
        "Kafa", "Göz/Burun", "İç Kulak", "Ağız Çevresi", "Çizgiler"
    )

    private val ANIMAL4_COLOR_KEYS = arrayOf(
        "animal4_color_0", "animal4_color_1", "animal4_color_2", "animal4_color_3"
    )

    private val ANIMAL4_DEFAULT_COLORS = intArrayOf(
        0xFFF3703A.toInt(),
        0xFF32302D.toInt(),
        0xFFFFFFFF.toInt(),
        0xFFCC482E.toInt()
    )

    private val ANIMAL4_SEL_DEFAULT_COLORS = intArrayOf(
        0xFFAA4E28.toInt(),
        0xFF23211F.toInt(),
        0xFFB2B2B2.toInt(),
        0xFF8E3220.toInt()
    )

    val ANIMAL4_COLOR_LABELS = arrayOf(
        "Gövde/Kafa", "Gözler/Burun", "Yüz/Ağız", "Kulak İçi/Detay"
    )

    private val ANIMAL5_COLOR_KEYS = arrayOf(
        "animal5_color_0", "animal5_color_1", "animal5_color_2", "animal5_color_3", "animal5_color_4", "animal5_color_5"
    )

    private val ANIMAL5_DEFAULT_COLORS = intArrayOf(
        0xFF32302D.toInt(),
        0xFF525D65.toInt(),
        0xFF7C8B96.toInt(),
        0xFF929FA9.toInt(),
        0xFFA6B3BD.toInt(),
        0xFFEFC9A5.toInt()
    )

    private val ANIMAL5_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF23211F.toInt(),
        0xFF394146.toInt(),
        0xFF566169.toInt(),
        0xFF666F76.toInt(),
        0xFF747D84.toInt(),
        0xFFA78C73.toInt()
    )

    val ANIMAL5_COLOR_LABELS = arrayOf(
        "Göz", "Kulak içi", "Kulak dışı", "Kafa", "Burun", "Diş"
    )

    private val ANIMAL6_COLOR_KEYS = arrayOf(
        "animal6_color_0", "animal6_color_1", "animal6_color_2"
    )

    private val ANIMAL6_DEFAULT_COLORS = intArrayOf(
        0xFF32302D.toInt(),
        0xFFD1D3D4.toInt(),
        0xFFFFFFFF.toInt()
    )

    private val ANIMAL6_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF23211F.toInt(),
        0xFF929394.toInt(),
        0xFFB2B2B2.toInt()
    )

    val ANIMAL6_COLOR_LABELS = arrayOf(
        "Göz/Burun", "Kulak/Ağız", "Kafa"
    )

    private val ANIMAL7_COLOR_KEYS = arrayOf(
        "animal7_color_0", "animal7_color_1", "animal7_color_2", "animal7_color_3", "animal7_color_4"
    )

    private val ANIMAL7_DEFAULT_COLORS = intArrayOf(
        0xFF32302D.toInt(),
        0xFFF3757A.toInt(),
        0xFFF9E4D7.toInt(),
        0xFFFFF1E7.toInt(),
        0xFFFFFFFF.toInt()
    )

    private val ANIMAL7_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF23211F.toInt(),
        0xFFAA5155.toInt(),
        0xFFAE9F96.toInt(),
        0xFFB2A8A1.toInt(),
        0xFFB2B2B2.toInt()
    )

    val ANIMAL7_COLOR_LABELS = arrayOf(
        "Göz/Burun", "Kulak", "Ağız", "Kafa", "Bıyık"
    )

    private val ANIMAL8_COLOR_KEYS = arrayOf(
        "animal8_color_0", "animal8_color_1", "animal8_color_2", "animal8_color_3", "animal8_color_4"
    )

    private val ANIMAL8_DEFAULT_COLORS = intArrayOf(
        0xFF32302D.toInt(),
        0xFF525D65.toInt(),
        0xFFD69F90.toInt(),
        0xFFFCD3C6.toInt(),
        0xFFFFFFFF.toInt()
    )

    private val ANIMAL8_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF23211F.toInt(),
        0xFF394146.toInt(),
        0xFF956F64.toInt(),
        0xFFB0938A.toInt(),
        0xFFB2B2B2.toInt()
    )

    val ANIMAL8_COLOR_LABELS = arrayOf(
        "Göz/Burun", "Kafa", "Çene", "Ağız", "Diş"
    )

    private val ANIMAL9_COLOR_KEYS = arrayOf(
        "animal9_color_0", "animal9_color_1", "animal9_color_2", "animal9_color_3"
    )

    private val ANIMAL9_DEFAULT_COLORS = intArrayOf(
        0xFF32302D.toInt(),
        0xFF60C198.toInt(),
        0xFFF2A638.toInt(),
        0xFFFFC94E.toInt()
    )

    private val ANIMAL9_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF23211F.toInt(),
        0xFF43876A.toInt(),
        0xFFA97427.toInt(),
        0xFFB28C36.toInt()
    )

    val ANIMAL9_COLOR_LABELS = arrayOf(
        "Göz", "Kafa", "Burun", "Ağız"
    )

    private val BOWLING_COLOR_KEYS = arrayOf(
        "bowling_color_0", "bowling_color_1", "bowling_color_2", "bowling_color_3", "bowling_color_4"
    )

    private val BOWLING_DEFAULT_COLORS = intArrayOf(
        0xFF5A648C.toInt(), // Ball Base
        0xFF4B547A.toInt(), // Shadows/Holes
        0xFF7585B3.toInt(), // Highlight
        0xFF383E61.toInt(), // Stroke
        0xFF404A73.toInt()  // Holes Inner
    )

    private val BOWLING_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF323A56.toInt(),
        0xFF272C44.toInt(),
        0xFF404A66.toInt(),
        0xFF1D2033.toInt(),
        0xFF21263A.toInt()
    )

    val BOWLING_COLOR_LABELS = arrayOf(
        "Ana Renk", "Gölgeler", "Parlaklık", "Çizgiler", "Delik İçi"
    )

    private val BALL1_COLOR_KEYS = arrayOf(
        "ball1_color_0", "ball1_color_1", "ball1_color_2"
    )

    private val BALL1_DEFAULT_COLORS = intArrayOf(
        0xFFB8DB6A.toInt(),
        0xFFF7FFAE.toInt(),
        0xFF92BF62.toInt()
    )

    private val BALL1_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF8AA44F.toInt(),
        0xFFB9BF82.toInt(),
        0xFF6D8F49.toInt()
    )

    val BALL1_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3"
    )

    private val BALL3_COLOR_KEYS = arrayOf(
        "ball3_color_0", "ball3_color_1", "ball3_color_2", "ball3_color_3"
    )

    private val BALL3_DEFAULT_COLORS = intArrayOf(
        0xFF2D4049.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF1D272B.toInt(),
        0xFF1B2A30.toInt()
    )

    private val BALL3_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF213036.toInt(),
        0xFFBFBFBF.toInt(),
        0xFF151D20.toInt(),
        0xFF141F24.toInt()
    )

    val BALL3_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3", "Renk 4"
    )

    private val BALL4_COLOR_KEYS = arrayOf(
        "ball4_color_0", "ball4_color_1", "ball4_color_2", "ball4_color_3"
    )

    private val BALL4_DEFAULT_COLORS = intArrayOf(
        0xFFF7845C.toInt(),
        0xFFAA5943.toInt(),
        0xFF873F2E.toInt(),
        0xFFDD694A.toInt()
    )

    private val BALL4_SEL_DEFAULT_COLORS = intArrayOf(
        0xFFB96345.toInt(),
        0xFF7F4232.toInt(),
        0xFF652F22.toInt(),
        0xFFA54E37.toInt()
    )

    val BALL4_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3", "Renk 4"
    )

    private val BALL5_COLOR_KEYS = arrayOf(
        "ball5_color_0", "ball5_color_1", "ball5_color_2", "ball5_color_3"
    )

    private val BALL5_DEFAULT_COLORS = intArrayOf(
        0xFFDDE8ED.toInt(),
        0xFF2D4049.toInt(),
        0xFFC5D7DD.toInt(),
        0xFF1D2E35.toInt()
    )

    private val BALL5_SEL_DEFAULT_COLORS = intArrayOf(
        0xFFA5AEB1.toInt(),
        0xFF213036.toInt(),
        0xFF93A1A5.toInt(),
        0xFF152227.toInt()
    )

    val BALL5_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3", "Renk 4"
    )

    private val BALL6_COLOR_KEYS = arrayOf(
        "ball6_color_0", "ball6_color_1", "ball6_color_2", "ball6_color_3"
    )

    private val BALL6_DEFAULT_COLORS = intArrayOf(
        0xFFB22D34.toInt(),
        0xFF512A2E.toInt(),
        0xFF932A34.toInt(),
        0xFF381D21.toInt()
    )

    private val BALL6_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF852127.toInt(),
        0xFF3C1F22.toInt(),
        0xFF6E1F27.toInt(),
        0xFF2A1518.toInt()
    )

    val BALL6_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3", "Renk 4"
    )

    private val BALL7_COLOR_KEYS = arrayOf(
        "ball7_color_0", "ball7_color_1", "ball7_color_2"
    )

    private val BALL7_DEFAULT_COLORS = intArrayOf(
        0xFFECECEC.toInt(),
        0xFFDDDDDD.toInt(),
        0xFFFFFFFF.toInt()
    )

    private val BALL7_SEL_DEFAULT_COLORS = intArrayOf(
        0xFFB1B1B1.toInt(),
        0xFFA5A5A5.toInt(),
        0xFFBFBFBF.toInt()
    )

    val BALL7_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3"
    )

    private val BALL8_COLOR_KEYS = arrayOf(
        "ball8_color_0", "ball8_color_1", "ball8_color_2", "ball8_color_3"
    )

    private val BALL8_DEFAULT_COLORS = intArrayOf(
        0xFF7F5B53.toInt(),
        0xFFD8D8D8.toInt(),
        0xFF44302D.toInt(),
        0xFFFFFFFF.toInt()
    )

    private val BALL8_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF5F443E.toInt(),
        0xFFA2A2A2.toInt(),
        0xFF332421.toInt(),
        0xFFBFBFBF.toInt()
    )

    val BALL8_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3", "Renk 4"
    )

    private val BALL9_COLOR_KEYS = arrayOf(
        "ball9_color_0", "ball9_color_1", "ball9_color_2", "ball9_color_3", "ball9_color_4"
    )

    private val BALL9_DEFAULT_COLORS = intArrayOf(
        0xFFECECEC.toInt(),
        0xFFEDD314.toInt(),
        0xFF365FC6.toInt(),
        0xFF274789.toInt(),
        0xFFFFFFFF.toInt()
    )

    private val BALL9_SEL_DEFAULT_COLORS = intArrayOf(
        0xFFB1B1B1.toInt(),
        0xFFB19E0F.toInt(),
        0xFF284794.toInt(),
        0xFF1D3566.toInt(),
        0xFFBFBFBF.toInt()
    )

    val BALL9_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3", "Renk 4", "Renk 5"
    )

    private val BALL10_COLOR_KEYS = arrayOf("ball10_color_0", "ball10_color_1", "ball10_color_2", "ball10_color_3", "ball10_color_4")
    private val BALL10_DEFAULT_COLORS = intArrayOf(0xFFECECEC.toInt(), 0xFFCECECE.toInt(), 0xFF878783.toInt(), 0xFF373735.toInt(), 0xFFFFFFFF.toInt())
    private val BALL10_SEL_DEFAULT_COLORS = intArrayOf(0xFFB1B1B1.toInt(), 0xFF9A9A9A.toInt(), 0xFF656562.toInt(), 0xFF292927.toInt(), 0xFFBFBFBF.toInt())
    val BALL10_COLOR_LABELS = arrayOf("Renk 1", "Renk 2", "Renk 3", "Renk 4", "Renk 5")

    private val BALL11_COLOR_KEYS = arrayOf("ball11_color_0", "ball11_color_1", "ball11_color_2", "ball11_color_3")
    private val BALL11_DEFAULT_COLORS = intArrayOf(0xFFF76D2F.toInt(), 0xFFE0602D.toInt(), 0xFF583A2A.toInt(), 0xFFFFFFFF.toInt())
    private val BALL11_SEL_DEFAULT_COLORS = intArrayOf(0xFFB95123.toInt(), 0xFFA84821.toInt(), 0xFF422B1F.toInt(), 0xFFBFBFBF.toInt())
    val BALL11_COLOR_LABELS = arrayOf("Renk 1", "Renk 2", "Renk 3", "Renk 4")

    private val BALL12_COLOR_KEYS = arrayOf("ball12_color_0", "ball12_color_1", "ball12_color_2", "ball12_color_3", "ball12_color_4")
    private val BALL12_DEFAULT_COLORS = intArrayOf(0xFFE0EC44.toInt(), 0xFFFFFFFF.toInt(), 0xFFEAEAEA.toInt(), 0xFFD8D844.toInt(), 0xFFDBDBDB.toInt())
    private val BALL12_SEL_DEFAULT_COLORS = intArrayOf(0xFFA8B133.toInt(), 0xFFBFBFBF.toInt(), 0xFFAFAFAF.toInt(), 0xFFA2A233.toInt(), 0xFFA4A4A4.toInt())
    val BALL12_COLOR_LABELS = arrayOf("Renk 1", "Renk 2", "Renk 3", "Renk 4", "Renk 5")

    private val BALL13_COLOR_KEYS = arrayOf("ball13_color_0", "ball13_color_1", "ball13_color_2", "ball13_color_3")
    private val BALL13_DEFAULT_COLORS = intArrayOf(0xFFECECEC.toInt(), 0xFFCECECE.toInt(), 0xFFBC3F3D.toInt(), 0xFFFFFFFF.toInt())
    private val BALL13_SEL_DEFAULT_COLORS = intArrayOf(0xFFB1B1B1.toInt(), 0xFF9A9A9A.toInt(), 0xFF8D2F2D.toInt(), 0xFFBFBFBF.toInt())
    val BALL13_COLOR_LABELS = arrayOf("Renk 1", "Renk 2", "Renk 3", "Renk 4")

    private val BALL14_COLOR_KEYS = arrayOf("ball14_color_0", "ball14_color_1", "ball14_color_2")
    private val BALL14_DEFAULT_COLORS = intArrayOf(0xFF010102.toInt(), 0xFF3C3C3C.toInt(), 0xFFFFFFFF.toInt())
    private val BALL14_SEL_DEFAULT_COLORS = intArrayOf(0xFF000001.toInt(), 0xFF2D2D2D.toInt(), 0xFFBFBFBF.toInt())
    val BALL14_COLOR_LABELS = arrayOf("Renk 1", "Renk 2", "Renk 3")

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun getPrefs(context: Context): SharedPreferences = prefs(context)

    private fun getColors(context: Context, keys: Array<String>, defaults: IntArray): IntArray {
        val p = prefs(context)
        return IntArray(keys.size) { i -> p.getInt(keys[i], defaults[i]) }
    }

    // ── Bead type ─────────────────────────────────────────────────────────────

    private fun getUidPrefix(): String {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
    }

    private fun getBeadTypeKey() = "${getUidPrefix()}_$KEY_BEAD_TYPE"

    fun getBeadType(context: Context): BeadType {
        val name = prefs(context).getString(getBeadTypeKey(), BeadType.SOROBAN.name)
            ?: BeadType.SOROBAN.name
        return try { BeadType.valueOf(name) } catch (_: Exception) { BeadType.SOROBAN }
    }

    fun setBeadType(context: Context, type: BeadType) {
        prefs(context).edit().putString(getBeadTypeKey(), type.name).apply()
    }

    // ── Per-bead slot storage ─────────────────────────────────────────────────
    // Key format:
    //   top bead of rod R   → "bead_slot_top_R"
    //   bottom bead R, i    → "bead_slot_bot_R_I"   (i in 0..3)

    private fun slotKeyTop(rod: Int) = "${getUidPrefix()}_bead_slot_top_$rod"
    private fun slotKeyBottom(rod: Int, beadIndex: Int) = "${getUidPrefix()}_bead_slot_bot_${rod}_${beadIndex}"

    /**
     * Returns the BeadType assigned to a specific bead slot.
     * Falls back to the global BeadType if no slot override exists.
     */
    fun getBeadTypeForSlot(context: Context, rod: Int, isTop: Boolean, beadIndex: Int = 0): BeadType {
        val key = if (isTop) slotKeyTop(rod) else slotKeyBottom(rod, beadIndex)
        val name = prefs(context).getString(key, null) ?: return BeadType.SOROBAN
        return try { BeadType.valueOf(name) } catch (_: Exception) { BeadType.SOROBAN }
    }

    /** Assigns a BeadType to a specific bead slot. */
    fun setBeadTypeForSlot(context: Context, rod: Int, isTop: Boolean, beadIndex: Int = 0, type: BeadType) {
        val key = if (isTop) slotKeyTop(rod) else slotKeyBottom(rod, beadIndex)
        prefs(context).edit().putString(key, type.name).apply()
    }

    /** Clears all per-bead slot overrides (global BeadType will be used for all). */
    fun resetAllBeadSlots(context: Context) {
        val edit = prefs(context).edit()
        for (rod in 0..4) {
            edit.remove(slotKeyTop(rod))
            for (i in 0..3) edit.remove(slotKeyBottom(rod, i))
        }
        edit.apply()
    }

    // ── Frame type ────────────────────────────────────────────────────────────


    // ── SOROBAN 2 (Gradient)
    val SOROBAN2_COLOR_LABELS = arrayOf("Renk 1", "Renk 2", "Renk 3", "Renk 4", "Renk 5")
    private val SOROBAN2_COLOR_KEYS = arrayOf(
        "soroban2_color_0", "soroban2_color_1", "soroban2_color_2", "soroban2_color_3", "soroban2_color_4"
    )
    private val SOROBAN2_DEFAULT_COLORS = intArrayOf(
        0xFFFEDA77.toInt(),
        0xFFF58529.toInt(),
        0xFFDD2A7B.toInt(),
        0xFF8134AF.toInt(),
        0xFF515BD4.toInt()
    )
    private val SOROBAN2_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF957E3C.toInt(),
        0xFF904E18.toInt(),
        0xFF7B1543.toInt(),
        0xFF43195D.toInt(),
        0xFF2A3073.toInt()
    )

    fun getSoroban2Colors(context: Context, isSelected: Boolean): IntArray {
        val prefs = prefs(context)
        val prefix = if (isSelected) "sel_" else ""
        val defaults = if (isSelected) SOROBAN2_SEL_DEFAULT_COLORS else SOROBAN2_DEFAULT_COLORS
        return IntArray(5) { i ->
            prefs.getInt(prefix + SOROBAN2_COLOR_KEYS[i], defaults[i])
        }
    }

    fun setSoroban2Color(context: Context, isSelected: Boolean, pathIndex: Int, color: Int) {
        val prefix = if (isSelected) "sel_" else ""
        prefs(context).edit().putInt(prefix + SOROBAN2_COLOR_KEYS[pathIndex], color).apply()
    }

    fun resetSoroban2Colors(context: Context, isSelected: Boolean) {
        val prefix = if (isSelected) "sel_" else ""
        val editor = prefs(context).edit()
        for (i in 0 until 5) {
            editor.remove(prefix + SOROBAN2_COLOR_KEYS[i])
        }
        editor.apply()
    }

    // ── SOROBAN 6 (Neumorphic)
    val SOROBAN6_COLOR_LABELS = arrayOf("Gövde", "Işık", "Gölge")
    private val SOROBAN6_COLOR_KEYS = arrayOf("soroban6_color_0", "soroban6_color_1", "soroban6_color_2")
    private val SOROBAN6_DEFAULT_COLORS = intArrayOf(
        0xFF9AE2C6.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF000000.toInt()
    )
    private val SOROBAN6_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF9AE2C6.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF000000.toInt()
    )

    fun getSoroban6Colors(context: Context, isSelected: Boolean): IntArray {
        val prefs = prefs(context)
        val prefix = if (isSelected) "sel_" else ""
        val defaults = if (isSelected) SOROBAN6_SEL_DEFAULT_COLORS else SOROBAN6_DEFAULT_COLORS
        return IntArray(3) { i ->
            prefs.getInt(prefix + SOROBAN6_COLOR_KEYS[i], defaults[i])
        }
    }

    fun setSoroban6Color(context: Context, isSelected: Boolean, pathIndex: Int, color: Int) {
        val prefix = if (isSelected) "sel_" else ""
        prefs(context).edit().putInt(prefix + SOROBAN6_COLOR_KEYS[pathIndex], color).apply()
    }

    fun resetSoroban6Colors(context: Context, isSelected: Boolean) {
        val prefix = if (isSelected) "sel_" else ""
        val editor = prefs(context).edit()
        for (i in 0 until 3) {
            editor.remove(prefix + SOROBAN6_COLOR_KEYS[i])
        }
        editor.apply()
    }

    fun getFrameType(context: Context): FrameType {
        val name = prefs(context).getString(KEY_FRAME_TYPE, FrameType.FRAME_BG2.name)
            ?: FrameType.FRAME_BG2.name
        return try { FrameType.valueOf(name) } catch (_: Exception) { FrameType.FRAME_BG2 }
    }

    fun setFrameType(context: Context, type: FrameType) {
        prefs(context).edit().putString(KEY_FRAME_TYPE, type.name).apply()
    }

    // ── Soroban colors ────────────────────────────────────────────────────────

    fun getSorobanColors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) SOROBAN_SEL_DEFAULT_COLORS else SOROBAN_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_SOROBAN_SEL else PREFIX_SOROBAN
        val p = prefs(context)
        return IntArray(3) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setSorobanColor(context: Context, selected: Boolean, pathIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_SOROBAN_SEL else PREFIX_SOROBAN
        prefs(context).edit().putInt("$prefix$pathIndex", color).apply()
    }

    fun resetSorobanColors(context: Context, selected: Boolean) {
        val defaults = if (selected) SOROBAN_SEL_DEFAULT_COLORS else SOROBAN_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_SOROBAN_SEL else PREFIX_SOROBAN
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Rabbit colors ─────────────────────────────────────────────────────────

    fun getRabbitColors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) RABBIT_SEL_DEFAULT_COLORS else RABBIT_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_RABBIT_SEL else PREFIX_RABBIT
        val p = prefs(context)
        return IntArray(7) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setRabbitColor(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_RABBIT_SEL else PREFIX_RABBIT
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetRabbitColors(context: Context, selected: Boolean) {
        val defaults = if (selected) RABBIT_SEL_DEFAULT_COLORS else RABBIT_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_RABBIT_SEL else PREFIX_RABBIT
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Animal colors ─────────────────────────────────────────────────────────

    fun getAnimalColors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) ANIMAL_SEL_DEFAULT_COLORS else ANIMAL_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL_SEL else PREFIX_ANIMAL
        val p = prefs(context)
        return IntArray(4) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setAnimalColor(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_ANIMAL_SEL else PREFIX_ANIMAL
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetAnimalColors(context: Context, selected: Boolean) {
        val defaults = if (selected) ANIMAL_SEL_DEFAULT_COLORS else ANIMAL_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL_SEL else PREFIX_ANIMAL
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Animal 2 colors ───────────────────────────────────────────────────────

    fun getAnimal2Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) ANIMAL2_SEL_DEFAULT_COLORS else ANIMAL2_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL2_SEL else PREFIX_ANIMAL2
        val p = prefs(context)
        return IntArray(5) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setAnimal2Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_ANIMAL2_SEL else PREFIX_ANIMAL2
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetAnimal2Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) ANIMAL2_SEL_DEFAULT_COLORS else ANIMAL2_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL2_SEL else PREFIX_ANIMAL2
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Animal 3 colors ───────────────────────────────────────────────────────

    fun getAnimal3Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) ANIMAL3_SEL_DEFAULT_COLORS else ANIMAL3_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL3_SEL else PREFIX_ANIMAL3
        val p = prefs(context)
        return IntArray(5) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setAnimal3Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_ANIMAL3_SEL else PREFIX_ANIMAL3
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetAnimal3Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) ANIMAL3_SEL_DEFAULT_COLORS else ANIMAL3_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL3_SEL else PREFIX_ANIMAL3
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Animal 4 colors ───────────────────────────────────────────────────────

    fun getAnimal4Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) ANIMAL4_SEL_DEFAULT_COLORS else ANIMAL4_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL4_SEL else PREFIX_ANIMAL4
        val p = prefs(context)
        return IntArray(4) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setAnimal4Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_ANIMAL4_SEL else PREFIX_ANIMAL4
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetAnimal4Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) ANIMAL4_SEL_DEFAULT_COLORS else ANIMAL4_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL4_SEL else PREFIX_ANIMAL4
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Animal 5 colors ───────────────────────────────────────────────────────

    fun getAnimal5Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) ANIMAL5_SEL_DEFAULT_COLORS else ANIMAL5_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL5_SEL else PREFIX_ANIMAL5
        val p = prefs(context)
        return IntArray(6) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setAnimal5Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_ANIMAL5_SEL else PREFIX_ANIMAL5
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetAnimal5Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) ANIMAL5_SEL_DEFAULT_COLORS else ANIMAL5_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL5_SEL else PREFIX_ANIMAL5
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Animal 6 colors ───────────────────────────────────────────────────────

    fun getAnimal6Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) ANIMAL6_SEL_DEFAULT_COLORS else ANIMAL6_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL6_SEL else PREFIX_ANIMAL6
        val p = prefs(context)
        return IntArray(3) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setAnimal6Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_ANIMAL6_SEL else PREFIX_ANIMAL6
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetAnimal6Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) ANIMAL6_SEL_DEFAULT_COLORS else ANIMAL6_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL6_SEL else PREFIX_ANIMAL6
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Animal 7 colors ───────────────────────────────────────────────────────

    fun getAnimal7Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) ANIMAL7_SEL_DEFAULT_COLORS else ANIMAL7_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL7_SEL else PREFIX_ANIMAL7
        val p = prefs(context)
        return IntArray(5) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setAnimal7Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_ANIMAL7_SEL else PREFIX_ANIMAL7
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetAnimal7Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) ANIMAL7_SEL_DEFAULT_COLORS else ANIMAL7_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL7_SEL else PREFIX_ANIMAL7
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Animal 8 colors ───────────────────────────────────────────────────────

    fun getAnimal8Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) ANIMAL8_SEL_DEFAULT_COLORS else ANIMAL8_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL8_SEL else PREFIX_ANIMAL8
        val p = prefs(context)
        return IntArray(5) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setAnimal8Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_ANIMAL8_SEL else PREFIX_ANIMAL8
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetAnimal8Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) ANIMAL8_SEL_DEFAULT_COLORS else ANIMAL8_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL8_SEL else PREFIX_ANIMAL8
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Animal 9 colors ───────────────────────────────────────────────────────

    fun getAnimal9Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) ANIMAL9_SEL_DEFAULT_COLORS else ANIMAL9_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL9_SEL else PREFIX_ANIMAL9
        val p = prefs(context)
        return IntArray(4) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setAnimal9Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_ANIMAL9_SEL else PREFIX_ANIMAL9
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetAnimal9Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) ANIMAL9_SEL_DEFAULT_COLORS else ANIMAL9_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_ANIMAL9_SEL else PREFIX_ANIMAL9
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Bowling colors ───────────────────────────────────────────────────────

    fun getBowlingColors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BOWLING_SEL_DEFAULT_COLORS else BOWLING_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BOWLING_SEL else PREFIX_BOWLING
        val p = prefs(context)
        return IntArray(5) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setBowlingColor(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BOWLING_SEL else PREFIX_BOWLING
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetBowlingColors(context: Context, selected: Boolean) {
        val defaults = if (selected) BOWLING_SEL_DEFAULT_COLORS else BOWLING_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BOWLING_SEL else PREFIX_BOWLING
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Ball1 colors ─────────────────────────────────────────────────────────

    fun getBall1Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BALL1_SEL_DEFAULT_COLORS else BALL1_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL1_SEL else PREFIX_BALL1
        val p = prefs(context)
        return IntArray(3) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setBall1Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BALL1_SEL else PREFIX_BALL1
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetBall1Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) BALL1_SEL_DEFAULT_COLORS else BALL1_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL1_SEL else PREFIX_BALL1
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Ball3 colors ─────────────────────────────────────────────────────────

    fun getBall3Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BALL3_SEL_DEFAULT_COLORS else BALL3_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL3_SEL else PREFIX_BALL3
        val p = prefs(context)
        return IntArray(4) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setBall3Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BALL3_SEL else PREFIX_BALL3
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetBall3Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) BALL3_SEL_DEFAULT_COLORS else BALL3_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL3_SEL else PREFIX_BALL3
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Ball4 colors ─────────────────────────────────────────────────────────

    fun getBall4Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BALL4_SEL_DEFAULT_COLORS else BALL4_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL4_SEL else PREFIX_BALL4
        val p = prefs(context)
        return IntArray(4) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setBall4Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BALL4_SEL else PREFIX_BALL4
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetBall4Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) BALL4_SEL_DEFAULT_COLORS else BALL4_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL4_SEL else PREFIX_BALL4
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Ball5 colors ─────────────────────────────────────────────────────────

    fun getBall5Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BALL5_SEL_DEFAULT_COLORS else BALL5_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL5_SEL else PREFIX_BALL5
        val p = prefs(context)
        return IntArray(4) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setBall5Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BALL5_SEL else PREFIX_BALL5
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetBall5Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) BALL5_SEL_DEFAULT_COLORS else BALL5_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL5_SEL else PREFIX_BALL5
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Ball6 colors ─────────────────────────────────────────────────────────

    fun getBall6Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BALL6_SEL_DEFAULT_COLORS else BALL6_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL6_SEL else PREFIX_BALL6
        val p = prefs(context)
        return IntArray(4) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setBall6Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BALL6_SEL else PREFIX_BALL6
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetBall6Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) BALL6_SEL_DEFAULT_COLORS else BALL6_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL6_SEL else PREFIX_BALL6
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Ball7 colors ─────────────────────────────────────────────────────────

    fun getBall7Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BALL7_SEL_DEFAULT_COLORS else BALL7_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL7_SEL else PREFIX_BALL7
        val p = prefs(context)
        return IntArray(3) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setBall7Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BALL7_SEL else PREFIX_BALL7
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetBall7Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) BALL7_SEL_DEFAULT_COLORS else BALL7_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL7_SEL else PREFIX_BALL7
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Ball8 colors ─────────────────────────────────────────────────────────

    fun getBall8Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BALL8_SEL_DEFAULT_COLORS else BALL8_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL8_SEL else PREFIX_BALL8
        val p = prefs(context)
        return IntArray(4) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setBall8Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BALL8_SEL else PREFIX_BALL8
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetBall8Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) BALL8_SEL_DEFAULT_COLORS else BALL8_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL8_SEL else PREFIX_BALL8
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Ball9 colors ─────────────────────────────────────────────────────────

    fun getBall9Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BALL9_SEL_DEFAULT_COLORS else BALL9_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL9_SEL else PREFIX_BALL9
        val p = prefs(context)
        return IntArray(5) { i -> p.getInt("$prefix$i", defaults[i]) }
    }

    fun setBall9Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BALL9_SEL else PREFIX_BALL9
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }

    fun resetBall9Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) BALL9_SEL_DEFAULT_COLORS else BALL9_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL9_SEL else PREFIX_BALL9
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Balls 10 to 14 colors ────────────────────────────────────────────────

    fun getBall10Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BALL10_SEL_DEFAULT_COLORS else BALL10_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL10_SEL else PREFIX_BALL10
        val p = prefs(context)
        return IntArray(5) { i -> p.getInt("$prefix$i", defaults[i]) }
    }
    fun setBall10Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BALL10_SEL else PREFIX_BALL10
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }
    fun resetBall10Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) BALL10_SEL_DEFAULT_COLORS else BALL10_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL10_SEL else PREFIX_BALL10
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    fun getBall11Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BALL11_SEL_DEFAULT_COLORS else BALL11_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL11_SEL else PREFIX_BALL11
        val p = prefs(context)
        return IntArray(4) { i -> p.getInt("$prefix$i", defaults[i]) }
    }
    fun setBall11Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BALL11_SEL else PREFIX_BALL11
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }
    fun resetBall11Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) BALL11_SEL_DEFAULT_COLORS else BALL11_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL11_SEL else PREFIX_BALL11
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    fun getBall12Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BALL12_SEL_DEFAULT_COLORS else BALL12_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL12_SEL else PREFIX_BALL12
        val p = prefs(context)
        return IntArray(5) { i -> p.getInt("$prefix$i", defaults[i]) }
    }
    fun setBall12Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BALL12_SEL else PREFIX_BALL12
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }
    fun resetBall12Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) BALL12_SEL_DEFAULT_COLORS else BALL12_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL12_SEL else PREFIX_BALL12
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    fun getBall13Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BALL13_SEL_DEFAULT_COLORS else BALL13_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL13_SEL else PREFIX_BALL13
        val p = prefs(context)
        return IntArray(4) { i -> p.getInt("$prefix$i", defaults[i]) }
    }
    fun setBall13Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BALL13_SEL else PREFIX_BALL13
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }
    fun resetBall13Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) BALL13_SEL_DEFAULT_COLORS else BALL13_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL13_SEL else PREFIX_BALL13
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    fun getBall14Colors(context: Context, selected: Boolean): IntArray {
        val defaults = if (selected) BALL14_SEL_DEFAULT_COLORS else BALL14_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL14_SEL else PREFIX_BALL14
        val p = prefs(context)
        return IntArray(3) { i -> p.getInt("$prefix$i", defaults[i]) }
    }
    fun setBall14Color(context: Context, selected: Boolean, colorIndex: Int, color: Int) {
        val prefix = if (selected) PREFIX_BALL14_SEL else PREFIX_BALL14
        prefs(context).edit().putInt("$prefix$colorIndex", color).apply()
    }
    fun resetBall14Colors(context: Context, selected: Boolean) {
        val defaults = if (selected) BALL14_SEL_DEFAULT_COLORS else BALL14_DEFAULT_COLORS
        val prefix = if (selected) PREFIX_BALL14_SEL else PREFIX_BALL14
        val edit = prefs(context).edit()
        defaults.forEachIndexed { i, c -> edit.putInt("$prefix$i", c) }
        edit.apply()
    }

    // ── Frame colors ──────────────────────────────────────────────────────────

    fun getFrameBgWoodColor(context: Context): Int =
        prefs(context).getInt(KEY_FRAME_BG_WOOD, Color.parseColor("#DDA379"))

    fun setFrameBgWoodColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_FRAME_BG_WOOD, color).apply()
    }

    fun getFrameBg2WoodColor(context: Context): Int =
        prefs(context).getInt(KEY_FRAME_BG2_WOOD, Color.parseColor("#DDA379"))

    fun setFrameBg2WoodColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_FRAME_BG2_WOOD, color).apply()
    }

    fun getFrameBg2HeartColor(context: Context): Int =
        prefs(context).getInt(KEY_FRAME_BG2_HEART, Color.parseColor("#E63946"))

    fun setFrameBg2HeartColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_FRAME_BG2_HEART, color).apply()
    }

    fun getFrameBg3WoodColor(context: Context): Int =
        prefs(context).getInt(KEY_FRAME_BG3_WOOD, Color.parseColor("#DDA379"))

    fun setFrameBg3WoodColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_FRAME_BG3_WOOD, color).apply()
    }

    fun getFrameBg3StarColor(context: Context): Int =
        prefs(context).getInt(KEY_FRAME_BG3_STAR, Color.parseColor("#DDFF33"))

    fun setFrameBg3StarColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_FRAME_BG3_STAR, color).apply()
    }

    fun getFrameBg4Colors(context: Context): IntArray {
        val p = prefs(context)
        return intArrayOf(
            p.getInt(KEY_FRAME_BG4_C1, Color.parseColor("#FEDA77")),
            p.getInt(KEY_FRAME_BG4_C2, Color.parseColor("#F58529")),
            p.getInt(KEY_FRAME_BG4_C3, Color.parseColor("#DD2A7B")),
            p.getInt(KEY_FRAME_BG4_C4, Color.parseColor("#8134AF")),
            p.getInt(KEY_FRAME_BG4_C5, Color.parseColor("#515BD4"))
        )
    }

    fun setFrameBg4Color(context: Context, index: Int, color: Int) {
        val key = when (index) {
            0 -> KEY_FRAME_BG4_C1
            1 -> KEY_FRAME_BG4_C2
            2 -> KEY_FRAME_BG4_C3
            3 -> KEY_FRAME_BG4_C4
            4 -> KEY_FRAME_BG4_C5
            else -> return
        }
        prefs(context).edit().putInt(key, color).apply()
    }

    fun resetAllFrameColors(context: Context) {
        prefs(context).edit()
            .putInt(KEY_FRAME_BG_WOOD, Color.parseColor("#DDA379"))
            .putInt(KEY_FRAME_BG2_WOOD, Color.parseColor("#DDA379"))
            .putInt(KEY_FRAME_BG2_HEART, Color.parseColor("#E63946"))
            .putInt(KEY_FRAME_BG3_WOOD, Color.parseColor("#DDA379"))
            .putInt(KEY_FRAME_BG3_STAR, Color.parseColor("#DDFF33"))
            .putInt(KEY_FRAME_BG4_C1, Color.parseColor("#FEDA77"))
            .putInt(KEY_FRAME_BG4_C2, Color.parseColor("#F58529"))
            .putInt(KEY_FRAME_BG4_C3, Color.parseColor("#DD2A7B"))
            .putInt(KEY_FRAME_BG4_C4, Color.parseColor("#8134AF"))
            .putInt(KEY_FRAME_BG4_C5, Color.parseColor("#515BD4"))
            .apply()
    }
}

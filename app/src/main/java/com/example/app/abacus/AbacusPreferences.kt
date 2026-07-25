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
    private const val KEY_FRAME_BG_WOOD = "frame_bg_wood"
    private const val KEY_FRAME_BG2_WOOD = "frame_bg2_wood"
    private const val KEY_FRAME_BG2_HEART = "frame_bg2_heart"

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
        ANIMAL9
    }
    enum class FrameType { FRAME_BG, FRAME_BG2 }

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
        "Beyaz alanlar", "Kafa gövdesi", "Detaylar", "Göz/Burun"
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

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun getPrefs(context: Context): SharedPreferences = prefs(context)

    private fun getColors(context: Context, keys: Array<String>, defaults: IntArray): IntArray {
        val p = prefs(context)
        return IntArray(keys.size) { i -> p.getInt(keys[i], defaults[i]) }
    }

    // ── Bead type ─────────────────────────────────────────────────────────────

    fun getBeadType(context: Context): BeadType {
        val name = prefs(context).getString(KEY_BEAD_TYPE, BeadType.SOROBAN.name)
            ?: BeadType.SOROBAN.name
        return try { BeadType.valueOf(name) } catch (_: Exception) { BeadType.SOROBAN }
    }

    fun setBeadType(context: Context, type: BeadType) {
        prefs(context).edit().putString(KEY_BEAD_TYPE, type.name).apply()
    }

    // ── Frame type ────────────────────────────────────────────────────────────

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

    fun resetAllFrameColors(context: Context) {
        prefs(context).edit()
            .putInt(KEY_FRAME_BG_WOOD, Color.parseColor("#DDA379"))
            .putInt(KEY_FRAME_BG2_WOOD, Color.parseColor("#DDA379"))
            .putInt(KEY_FRAME_BG2_HEART, Color.parseColor("#E63946"))
            .apply()
    }
}

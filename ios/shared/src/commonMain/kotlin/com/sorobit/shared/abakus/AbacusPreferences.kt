// Kaynak (Android): app/src/main/java/com/example/app/abacus/AbacusPreferences.kt
//
// Abaküs özelleştirme ayarlarının kataloğu (boncuk/çerçeve tipleri, varsayılan renkler,
// anahtar adları) ve anahtar/varsayılan kuralları. Android'de SharedPreferences'a doğrudan
// erişiliyordu; burada G/Ç [AbacusPrefsStore] arayüzüne ayrıldı.
//
// Android'den farklar:
//  - `Color.parseColor("#RRGGBB")` → `0xFFRRGGBB.toInt()` (aynı ARGB değeri).
//  - `context` parametresi yerine `store` (ve gerekiyorsa `uid`) parametresi.
//  - Android'deki `getXColors/setXColor/resetXColors` (X = Soroban, Animal..., Ball14) üçlüleri
//    birebir aynı kalıpta olduğundan tek bir tabloya ([AbacusPreferences.beadColorSpec]) indirildi:
//    `getSorobanColors(ctx, sel)` → `getBeadColors(store, BeadType.SOROBAN, sel)` vb.
//    Tavşan (rabbit) bir BeadType olmadığı için ayrı fonksiyonlarda duruyor.
//  - Sabitlerin çoğu Android'de `private` idi; iOS tarafı da okuyabilsin diye açıldı.
package com.sorobit.shared.abakus

/**
 * SharedPreferences karşılığı anahtar-değer deposu. iOS'ta NSUserDefaults(suiteName:) ile
 * uygulanır. Depo, [AbacusPreferences.prefsFileName] ile adlandırılan HESABA ÖZEL dosyadır.
 */
interface AbacusPrefsStore {
    fun getInt(key: String, defaultValue: Int): Int
    fun getString(key: String, defaultValue: String?): String?
    fun putInt(key: String, value: Int)
    fun putString(key: String, value: String)
    fun remove(key: String)
}

/** Testler ve geçici kullanım için bellek içi depo. */
class BellekAbacusPrefsStore : AbacusPrefsStore {
    val degerler = mutableMapOf<String, Any>()
    override fun getInt(key: String, defaultValue: Int): Int = degerler[key] as? Int ?: defaultValue
    override fun getString(key: String, defaultValue: String?): String? = degerler[key] as? String ?: defaultValue
    override fun putInt(key: String, value: Int) { degerler[key] = value }
    override fun putString(key: String, value: String) { degerler[key] = value }
    override fun remove(key: String) { degerler.remove(key) }
}

/**
 * SharedPreferences-backed store for abacus customization settings (Android'deki ad korunmuştur).
 */
object AbacusPreferences {


    const val PREF_NAME = "abacus_customization"

    // ── Keys ─────────────────────────────────────────────────────────────────
    const val KEY_BEAD_TYPE = "bead_type"
    const val KEY_FRAME_TYPE = "frame_type"
    const val PREFIX_SOROBAN = "soroban_path_"
    const val PREFIX_SOROBAN_SEL = "soroban_sel_path_"
    const val PREFIX_RABBIT = "rabbit_c"
    const val PREFIX_RABBIT_SEL = "rabbit_sel_c"
    const val PREFIX_ANIMAL = "animal_c"
    const val PREFIX_ANIMAL_SEL = "animal_sel_c"
    const val PREFIX_ANIMAL2 = "animal2_c"
    const val PREFIX_ANIMAL2_SEL = "animal2_sel_c"
    const val PREFIX_ANIMAL3 = "animal3_c"
    const val PREFIX_ANIMAL3_SEL = "animal3_sel_c"
    const val PREFIX_ANIMAL4 = "animal4_c"
    const val PREFIX_ANIMAL4_SEL = "animal4_sel_c"
    const val PREFIX_ANIMAL5 = "animal5_c"
    const val PREFIX_ANIMAL5_SEL = "animal5_sel_c"
    const val PREFIX_ANIMAL6 = "animal6_c"
    const val PREFIX_ANIMAL6_SEL = "animal6_sel_c"
    const val PREFIX_ANIMAL7 = "animal7_c"
    const val PREFIX_ANIMAL7_SEL = "animal7_sel_c"
    const val PREFIX_ANIMAL8 = "animal8_c"
    const val PREFIX_ANIMAL8_SEL = "animal8_sel_c"
    const val PREFIX_ANIMAL9 = "animal9_c"
    const val PREFIX_ANIMAL9_SEL = "animal9_sel_c"
    const val PREFIX_BOWLING = "bowling_c"
    const val PREFIX_BOWLING_SEL = "bowling_sel_c"
    const val PREFIX_BALL1 = "ball1_c"
    const val PREFIX_BALL1_SEL = "ball1_sel_c"
    const val PREFIX_BALL3 = "ball3_c"
    const val PREFIX_BALL3_SEL = "ball3_sel_c"
    const val PREFIX_BALL4 = "ball4_c"
    const val PREFIX_BALL4_SEL = "ball4_sel_c"
    const val PREFIX_BALL5 = "ball5_c"
    const val PREFIX_BALL5_SEL = "ball5_sel_c"
    const val PREFIX_BALL6 = "ball6_c"
    const val PREFIX_BALL6_SEL = "ball6_sel_c"
    const val PREFIX_BALL7 = "ball7_c"
    const val PREFIX_BALL7_SEL = "ball7_sel_c"
    const val PREFIX_BALL8 = "ball8_c"
    const val PREFIX_BALL8_SEL = "ball8_sel_c"
    const val PREFIX_BALL9 = "ball9_c"
    const val PREFIX_BALL9_SEL = "ball9_sel_c"
    const val PREFIX_BALL10 = "ball10_c"
    const val PREFIX_BALL10_SEL = "ball10_sel_c"
    const val PREFIX_BALL11 = "ball11_c"
    const val PREFIX_BALL11_SEL = "ball11_sel_c"
    const val PREFIX_BALL12 = "ball12_c"
    const val PREFIX_BALL12_SEL = "ball12_sel_c"
    const val PREFIX_BALL13 = "ball13_c"
    const val PREFIX_BALL13_SEL = "ball13_sel_c"
    const val PREFIX_BALL14 = "ball14_c"
    const val PREFIX_BALL14_SEL = "ball14_sel_c"
    const val KEY_FRAME_BG_WOOD = "frame_bg_wood"
    const val KEY_FRAME_BG2_WOOD = "frame_bg2_wood"
    const val KEY_FRAME_BG2_HEART = "frame_bg2_heart"
    const val KEY_FRAME_BG3_WOOD = "frame_bg3_wood"
    const val KEY_FRAME_BG3_STAR = "frame_bg3_star"
    const val KEY_FRAME_BG4_C1 = "frame_bg4_c1"
    const val KEY_FRAME_BG4_C2 = "frame_bg4_c2"
    const val KEY_FRAME_BG4_C3 = "frame_bg4_c3"
    const val KEY_FRAME_BG4_C4 = "frame_bg4_c4"
    const val KEY_FRAME_BG4_C5 = "frame_bg4_c5"

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
        0xFF000000.toInt(),
        0xFF737373.toInt(),
        0xFF8C8C8C.toInt()
    )

    /**
     * Default ARGB colors for each soroban_bead_selected.xml path:
     *  [0] outer black shell, [1] inner dark red, [2] right medium red
     */
    val SOROBAN_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF000000.toInt(),
        0xFF4D0000.toInt(),
        0xFF731616.toInt()
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
        0xFFDCD2D1.toInt(),
        0xFFCEADB1.toInt(),
        0xFFF1E6E6.toInt(),
        0xFFFFFFFF.toInt(),
        0xFFCD92A2.toInt(),
        0xFFFFF6F9.toInt(),
        0xFF332F28.toInt()
    )

    /**
     * rabbit_selected_ic.xml unique fill colors (same ordering as RABBIT_DEFAULT_COLORS).
     */
    val RABBIT_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF6B3B3F.toInt(),
        0xFF8B2635.toInt(),
        0xFF7C4B52.toInt(),
        0xFF94737A.toInt(),
        0xFF661426.toInt(),
        0xFF94737A.toInt(),
        0xFF1A080B.toInt()
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
        0xFFFFFFFF.toInt(),
        0xFF777873.toInt(),
        0xFF4D4C48.toInt(),
        0xFF32302D.toInt()
    )

    val ANIMAL_SEL_DEFAULT_COLORS = intArrayOf(
        0xFFB2B2B2.toInt(),
        0xFF535450.toInt(),
        0xFF353532.toInt(),
        0xFF23211F.toInt()
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
        0xFFB76057.toInt(),
        0xFFFBBB8C.toInt(),
        0xFF32302D.toInt(),
        0xFF914844.toInt(),
        0xFFE8A070.toInt()
    )

    val ANIMAL2_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF80433C.toInt(),
        0xFFAF8262.toInt(),
        0xFF23211F.toInt(),
        0xFF65322F.toInt(),
        0xFFA2704E.toInt()
    )

    val ANIMAL2_COLOR_LABELS = arrayOf(
        "Kafa", "Yüz", "Gözler", "Burun", "Ağız"
    )

    val ANIMAL3_COLOR_KEYS = arrayOf(
        "animal3_color_0", "animal3_color_1", "animal3_color_2", "animal3_color_3", "animal3_color_4"
    )

    val ANIMAL3_DEFAULT_COLORS = intArrayOf(
        0xFFF99E4D.toInt(),
        0xFF32302D.toInt(),
        0xFFE58435.toInt(),
        0xFFFDD2B9.toInt(),
        0xFFD36640.toInt()
    )

    val ANIMAL3_SEL_DEFAULT_COLORS = intArrayOf(
        0xFFAE6E35.toInt(),
        0xFF23211F.toInt(),
        0xFFA05C25.toInt(),
        0xFFB19381.toInt(),
        0xFF93472C.toInt()
    )

    val ANIMAL3_COLOR_LABELS = arrayOf(
        "Kafa", "Göz/Burun", "İç Kulak", "Ağız Çevresi", "Çizgiler"
    )

    val ANIMAL4_COLOR_KEYS = arrayOf(
        "animal4_color_0", "animal4_color_1", "animal4_color_2", "animal4_color_3"
    )

    val ANIMAL4_DEFAULT_COLORS = intArrayOf(
        0xFFF3703A.toInt(),
        0xFF32302D.toInt(),
        0xFFFFFFFF.toInt(),
        0xFFCC482E.toInt()
    )

    val ANIMAL4_SEL_DEFAULT_COLORS = intArrayOf(
        0xFFAA4E28.toInt(),
        0xFF23211F.toInt(),
        0xFFB2B2B2.toInt(),
        0xFF8E3220.toInt()
    )

    val ANIMAL4_COLOR_LABELS = arrayOf(
        "Gövde/Kafa", "Gözler/Burun", "Yüz/Ağız", "Kulak İçi/Detay"
    )

    val ANIMAL5_COLOR_KEYS = arrayOf(
        "animal5_color_0", "animal5_color_1", "animal5_color_2", "animal5_color_3", "animal5_color_4", "animal5_color_5"
    )

    val ANIMAL5_DEFAULT_COLORS = intArrayOf(
        0xFF32302D.toInt(),
        0xFF525D65.toInt(),
        0xFF7C8B96.toInt(),
        0xFF929FA9.toInt(),
        0xFFA6B3BD.toInt(),
        0xFFEFC9A5.toInt()
    )

    val ANIMAL5_SEL_DEFAULT_COLORS = intArrayOf(
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

    val ANIMAL6_COLOR_KEYS = arrayOf(
        "animal6_color_0", "animal6_color_1", "animal6_color_2"
    )

    val ANIMAL6_DEFAULT_COLORS = intArrayOf(
        0xFF32302D.toInt(),
        0xFFD1D3D4.toInt(),
        0xFFFFFFFF.toInt()
    )

    val ANIMAL6_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF23211F.toInt(),
        0xFF929394.toInt(),
        0xFFB2B2B2.toInt()
    )

    val ANIMAL6_COLOR_LABELS = arrayOf(
        "Göz/Burun", "Kulak/Ağız", "Kafa"
    )

    val ANIMAL7_COLOR_KEYS = arrayOf(
        "animal7_color_0", "animal7_color_1", "animal7_color_2", "animal7_color_3", "animal7_color_4"
    )

    val ANIMAL7_DEFAULT_COLORS = intArrayOf(
        0xFF32302D.toInt(),
        0xFFF3757A.toInt(),
        0xFFF9E4D7.toInt(),
        0xFFFFF1E7.toInt(),
        0xFFFFFFFF.toInt()
    )

    val ANIMAL7_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF23211F.toInt(),
        0xFFAA5155.toInt(),
        0xFFAE9F96.toInt(),
        0xFFB2A8A1.toInt(),
        0xFFB2B2B2.toInt()
    )

    val ANIMAL7_COLOR_LABELS = arrayOf(
        "Göz/Burun", "Kulak", "Ağız", "Kafa", "Bıyık"
    )

    val ANIMAL8_COLOR_KEYS = arrayOf(
        "animal8_color_0", "animal8_color_1", "animal8_color_2", "animal8_color_3", "animal8_color_4"
    )

    val ANIMAL8_DEFAULT_COLORS = intArrayOf(
        0xFF32302D.toInt(),
        0xFF525D65.toInt(),
        0xFFD69F90.toInt(),
        0xFFFCD3C6.toInt(),
        0xFFFFFFFF.toInt()
    )

    val ANIMAL8_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF23211F.toInt(),
        0xFF394146.toInt(),
        0xFF956F64.toInt(),
        0xFFB0938A.toInt(),
        0xFFB2B2B2.toInt()
    )

    val ANIMAL8_COLOR_LABELS = arrayOf(
        "Göz/Burun", "Kafa", "Çene", "Ağız", "Diş"
    )

    val ANIMAL9_COLOR_KEYS = arrayOf(
        "animal9_color_0", "animal9_color_1", "animal9_color_2", "animal9_color_3"
    )

    val ANIMAL9_DEFAULT_COLORS = intArrayOf(
        0xFF32302D.toInt(),
        0xFF60C198.toInt(),
        0xFFF2A638.toInt(),
        0xFFFFC94E.toInt()
    )

    val ANIMAL9_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF23211F.toInt(),
        0xFF43876A.toInt(),
        0xFFA97427.toInt(),
        0xFFB28C36.toInt()
    )

    val ANIMAL9_COLOR_LABELS = arrayOf(
        "Göz", "Kafa", "Burun", "Ağız"
    )

    val BOWLING_COLOR_KEYS = arrayOf(
        "bowling_color_0", "bowling_color_1", "bowling_color_2", "bowling_color_3", "bowling_color_4"
    )

    val BOWLING_DEFAULT_COLORS = intArrayOf(
        0xFF5A648C.toInt(), // Ball Base
        0xFF4B547A.toInt(), // Shadows/Holes
        0xFF7585B3.toInt(), // Highlight
        0xFF383E61.toInt(), // Stroke
        0xFF404A73.toInt()  // Holes Inner
    )

    val BOWLING_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF323A56.toInt(),
        0xFF272C44.toInt(),
        0xFF404A66.toInt(),
        0xFF1D2033.toInt(),
        0xFF21263A.toInt()
    )

    val BOWLING_COLOR_LABELS = arrayOf(
        "Ana Renk", "Gölgeler", "Parlaklık", "Çizgiler", "Delik İçi"
    )

    val BALL1_COLOR_KEYS = arrayOf(
        "ball1_color_0", "ball1_color_1", "ball1_color_2"
    )

    val BALL1_DEFAULT_COLORS = intArrayOf(
        0xFFB8DB6A.toInt(),
        0xFFF7FFAE.toInt(),
        0xFF92BF62.toInt()
    )

    val BALL1_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF8AA44F.toInt(),
        0xFFB9BF82.toInt(),
        0xFF6D8F49.toInt()
    )

    val BALL1_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3"
    )

    val BALL3_COLOR_KEYS = arrayOf(
        "ball3_color_0", "ball3_color_1", "ball3_color_2", "ball3_color_3"
    )

    val BALL3_DEFAULT_COLORS = intArrayOf(
        0xFF2D4049.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF1D272B.toInt(),
        0xFF1B2A30.toInt()
    )

    val BALL3_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF213036.toInt(),
        0xFFBFBFBF.toInt(),
        0xFF151D20.toInt(),
        0xFF141F24.toInt()
    )

    val BALL3_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3", "Renk 4"
    )

    val BALL4_COLOR_KEYS = arrayOf(
        "ball4_color_0", "ball4_color_1", "ball4_color_2", "ball4_color_3"
    )

    val BALL4_DEFAULT_COLORS = intArrayOf(
        0xFFF7845C.toInt(),
        0xFFAA5943.toInt(),
        0xFF873F2E.toInt(),
        0xFFDD694A.toInt()
    )

    val BALL4_SEL_DEFAULT_COLORS = intArrayOf(
        0xFFB96345.toInt(),
        0xFF7F4232.toInt(),
        0xFF652F22.toInt(),
        0xFFA54E37.toInt()
    )

    val BALL4_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3", "Renk 4"
    )

    val BALL5_COLOR_KEYS = arrayOf(
        "ball5_color_0", "ball5_color_1", "ball5_color_2", "ball5_color_3"
    )

    val BALL5_DEFAULT_COLORS = intArrayOf(
        0xFFDDE8ED.toInt(),
        0xFF2D4049.toInt(),
        0xFFC5D7DD.toInt(),
        0xFF1D2E35.toInt()
    )

    val BALL5_SEL_DEFAULT_COLORS = intArrayOf(
        0xFFA5AEB1.toInt(),
        0xFF213036.toInt(),
        0xFF93A1A5.toInt(),
        0xFF152227.toInt()
    )

    val BALL5_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3", "Renk 4"
    )

    val BALL6_COLOR_KEYS = arrayOf(
        "ball6_color_0", "ball6_color_1", "ball6_color_2", "ball6_color_3"
    )

    val BALL6_DEFAULT_COLORS = intArrayOf(
        0xFFB22D34.toInt(),
        0xFF512A2E.toInt(),
        0xFF932A34.toInt(),
        0xFF381D21.toInt()
    )

    val BALL6_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF852127.toInt(),
        0xFF3C1F22.toInt(),
        0xFF6E1F27.toInt(),
        0xFF2A1518.toInt()
    )

    val BALL6_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3", "Renk 4"
    )

    val BALL7_COLOR_KEYS = arrayOf(
        "ball7_color_0", "ball7_color_1", "ball7_color_2"
    )

    val BALL7_DEFAULT_COLORS = intArrayOf(
        0xFFECECEC.toInt(),
        0xFFDDDDDD.toInt(),
        0xFFFFFFFF.toInt()
    )

    val BALL7_SEL_DEFAULT_COLORS = intArrayOf(
        0xFFB1B1B1.toInt(),
        0xFFA5A5A5.toInt(),
        0xFFBFBFBF.toInt()
    )

    val BALL7_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3"
    )

    val BALL8_COLOR_KEYS = arrayOf(
        "ball8_color_0", "ball8_color_1", "ball8_color_2", "ball8_color_3"
    )

    val BALL8_DEFAULT_COLORS = intArrayOf(
        0xFF7F5B53.toInt(),
        0xFFD8D8D8.toInt(),
        0xFF44302D.toInt(),
        0xFFFFFFFF.toInt()
    )

    val BALL8_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF5F443E.toInt(),
        0xFFA2A2A2.toInt(),
        0xFF332421.toInt(),
        0xFFBFBFBF.toInt()
    )

    val BALL8_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3", "Renk 4"
    )

    val BALL9_COLOR_KEYS = arrayOf(
        "ball9_color_0", "ball9_color_1", "ball9_color_2", "ball9_color_3", "ball9_color_4"
    )

    val BALL9_DEFAULT_COLORS = intArrayOf(
        0xFFECECEC.toInt(),
        0xFFEDD314.toInt(),
        0xFF365FC6.toInt(),
        0xFF274789.toInt(),
        0xFFFFFFFF.toInt()
    )

    val BALL9_SEL_DEFAULT_COLORS = intArrayOf(
        0xFFB1B1B1.toInt(),
        0xFFB19E0F.toInt(),
        0xFF284794.toInt(),
        0xFF1D3566.toInt(),
        0xFFBFBFBF.toInt()
    )

    val BALL9_COLOR_LABELS = arrayOf(
        "Renk 1", "Renk 2", "Renk 3", "Renk 4", "Renk 5"
    )

    val BALL10_COLOR_KEYS = arrayOf("ball10_color_0", "ball10_color_1", "ball10_color_2", "ball10_color_3", "ball10_color_4")
    val BALL10_DEFAULT_COLORS = intArrayOf(0xFFECECEC.toInt(), 0xFFCECECE.toInt(), 0xFF878783.toInt(), 0xFF373735.toInt(), 0xFFFFFFFF.toInt())
    val BALL10_SEL_DEFAULT_COLORS = intArrayOf(0xFFB1B1B1.toInt(), 0xFF9A9A9A.toInt(), 0xFF656562.toInt(), 0xFF292927.toInt(), 0xFFBFBFBF.toInt())
    val BALL10_COLOR_LABELS = arrayOf("Renk 1", "Renk 2", "Renk 3", "Renk 4", "Renk 5")

    val BALL11_COLOR_KEYS = arrayOf("ball11_color_0", "ball11_color_1", "ball11_color_2", "ball11_color_3")
    val BALL11_DEFAULT_COLORS = intArrayOf(0xFFF76D2F.toInt(), 0xFFE0602D.toInt(), 0xFF583A2A.toInt(), 0xFFFFFFFF.toInt())
    val BALL11_SEL_DEFAULT_COLORS = intArrayOf(0xFFB95123.toInt(), 0xFFA84821.toInt(), 0xFF422B1F.toInt(), 0xFFBFBFBF.toInt())
    val BALL11_COLOR_LABELS = arrayOf("Renk 1", "Renk 2", "Renk 3", "Renk 4")

    val BALL12_COLOR_KEYS = arrayOf("ball12_color_0", "ball12_color_1", "ball12_color_2", "ball12_color_3", "ball12_color_4")
    val BALL12_DEFAULT_COLORS = intArrayOf(0xFFE0EC44.toInt(), 0xFFFFFFFF.toInt(), 0xFFEAEAEA.toInt(), 0xFFD8D844.toInt(), 0xFFDBDBDB.toInt())
    val BALL12_SEL_DEFAULT_COLORS = intArrayOf(0xFFA8B133.toInt(), 0xFFBFBFBF.toInt(), 0xFFAFAFAF.toInt(), 0xFFA2A233.toInt(), 0xFFA4A4A4.toInt())
    val BALL12_COLOR_LABELS = arrayOf("Renk 1", "Renk 2", "Renk 3", "Renk 4", "Renk 5")

    val BALL13_COLOR_KEYS = arrayOf("ball13_color_0", "ball13_color_1", "ball13_color_2", "ball13_color_3")
    val BALL13_DEFAULT_COLORS = intArrayOf(0xFFECECEC.toInt(), 0xFFCECECE.toInt(), 0xFFBC3F3D.toInt(), 0xFFFFFFFF.toInt())
    val BALL13_SEL_DEFAULT_COLORS = intArrayOf(0xFFB1B1B1.toInt(), 0xFF9A9A9A.toInt(), 0xFF8D2F2D.toInt(), 0xFFBFBFBF.toInt())
    val BALL13_COLOR_LABELS = arrayOf("Renk 1", "Renk 2", "Renk 3", "Renk 4")

    val BALL14_COLOR_KEYS = arrayOf("ball14_color_0", "ball14_color_1", "ball14_color_2")
    val BALL14_DEFAULT_COLORS = intArrayOf(0xFF010102.toInt(), 0xFF3C3C3C.toInt(), 0xFFFFFFFF.toInt())
    val BALL14_SEL_DEFAULT_COLORS = intArrayOf(0xFF000001.toInt(), 0xFF2D2D2D.toInt(), 0xFFBFBFBF.toInt())
    val BALL14_COLOR_LABELS = arrayOf("Renk 1", "Renk 2", "Renk 3")

    // ── SOROBAN 2 (Gradient)
    val SOROBAN2_COLOR_LABELS = arrayOf("Renk 1", "Renk 2", "Renk 3", "Renk 4", "Renk 5")
    val SOROBAN2_COLOR_KEYS = arrayOf(
        "soroban2_color_0", "soroban2_color_1", "soroban2_color_2", "soroban2_color_3", "soroban2_color_4"
    )
    val SOROBAN2_DEFAULT_COLORS = intArrayOf(
        0xFFFEDA77.toInt(),
        0xFFF58529.toInt(),
        0xFFDD2A7B.toInt(),
        0xFF8134AF.toInt(),
        0xFF515BD4.toInt()
    )
    val SOROBAN2_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF957E3C.toInt(),
        0xFF904E18.toInt(),
        0xFF7B1543.toInt(),
        0xFF43195D.toInt(),
        0xFF2A3073.toInt()
    )

    // ── SOROBAN 6 (Neumorphic)
    val SOROBAN6_COLOR_LABELS = arrayOf("Gövde", "Işık", "Gölge")
    val SOROBAN6_COLOR_KEYS = arrayOf("soroban6_color_0", "soroban6_color_1", "soroban6_color_2")
    val SOROBAN6_DEFAULT_COLORS = intArrayOf(
        0xFF9AE2C6.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF000000.toInt()
    )
    val SOROBAN6_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF9AE2C6.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF000000.toInt()
    )

    // ── Hesaba özel dosya adı / uid öneki ─────────────────────────────────────

    /** Android: `FirebaseAuth.currentUser?.uid ?: "guest"`. */
    fun getUidPrefix(uid: String?): String = uid ?: "guest"

    /**
     * Hesap başına AYRI SharedPreferences dosyası: `abacus_customization_{uid}`.
     * (Android: `prefs(context)`.)
     */
    fun prefsFileName(uid: String?): String = "${PREF_NAME}_${getUidPrefix(uid)}"

    // ── Bead type ─────────────────────────────────────────────────────────────

    /** Anahtar adının İÇİNDE de uid öneki var (dosya zaten hesaba özel; önek fazladan ama korunuyor). */
    fun getBeadTypeKey(uid: String?) = "${getUidPrefix(uid)}_$KEY_BEAD_TYPE"

    /** `BeadType.valueOf` karşılığı; bilinmeyen ad → null (Android'de istisna → varsayılan). */
    fun beadTypeFromName(name: String?): BeadType? = BeadType.entries.firstOrNull { it.name == name }

    fun frameTypeFromName(name: String?): FrameType? = FrameType.entries.firstOrNull { it.name == name }

    fun getBeadType(store: AbacusPrefsStore, uid: String?): BeadType {
        val name = store.getString(getBeadTypeKey(uid), BeadType.SOROBAN.name)
            ?: BeadType.SOROBAN.name
        return beadTypeFromName(name) ?: BeadType.SOROBAN
    }

    fun setBeadType(store: AbacusPrefsStore, uid: String?, type: BeadType) {
        store.putString(getBeadTypeKey(uid), type.name)
    }

    // ── Per-bead slot storage ─────────────────────────────────────────────────
    // Anahtar biçimi:
    //   R çubuğunun üst boncuğu → "{uid}_bead_slot_top_R"
    //   R çubuğu, i. alt boncuk → "{uid}_bead_slot_bot_R_I"   (i 0..3)

    fun slotKeyTop(uid: String?, rod: Int) = "${getUidPrefix(uid)}_bead_slot_top_$rod"
    fun slotKeyBottom(uid: String?, rod: Int, beadIndex: Int) = "${getUidPrefix(uid)}_bead_slot_bot_${rod}_${beadIndex}"

    /**
     * Belirli bir boncuk yuvasına atanmış BeadType.
     * DİKKAT (Android davranışı): yorum "global BeadType'a düşer" dese de kod yuva kaydı yoksa
     * doğrudan SOROBAN döndürür; aynen korunmuştur.
     */
    fun getBeadTypeForSlot(store: AbacusPrefsStore, uid: String?, rod: Int, isTop: Boolean, beadIndex: Int = 0): BeadType {
        val key = if (isTop) slotKeyTop(uid, rod) else slotKeyBottom(uid, rod, beadIndex)
        val name = store.getString(key, null) ?: return BeadType.SOROBAN
        return beadTypeFromName(name) ?: BeadType.SOROBAN
    }

    fun setBeadTypeForSlot(store: AbacusPrefsStore, uid: String?, rod: Int, isTop: Boolean, beadIndex: Int = 0, type: BeadType) {
        val key = if (isTop) slotKeyTop(uid, rod) else slotKeyBottom(uid, rod, beadIndex)
        store.putString(key, type.name)
    }

    /** Tüm yuva kayıtlarını siler. */
    fun resetAllBeadSlots(store: AbacusPrefsStore, uid: String?) {
        for (rod in 0..4) {
            store.remove(slotKeyTop(uid, rod))
            for (i in 0..3) store.remove(slotKeyBottom(uid, rod, i))
        }
    }

    // ── Frame type ────────────────────────────────────────────────────────────

    /** Çerçeve tipi anahtarında uid öneki YOK (Android ile aynı). */
    fun getFrameType(store: AbacusPrefsStore): FrameType {
        val name = store.getString(KEY_FRAME_TYPE, FrameType.FRAME_BG.name)
            ?: FrameType.FRAME_BG.name
        return frameTypeFromName(name) ?: FrameType.FRAME_BG
    }

    fun setFrameType(store: AbacusPrefsStore, type: FrameType) {
        store.putString(KEY_FRAME_TYPE, type.name)
    }

    // ── Boncuk renkleri (tüm tipler için tek tablo) ───────────────────────────

    /**
     * Bir boncuk tipinin renk grubu tanımı.
     * - [onekli] = true: anahtar `"$prefix$i"` / `"$selPrefix$i"`; sıfırlama VARSAYILANLARI YAZAR
     *   (Android: getSorobanColors/getAnimalColors/getBallNColors... kalıbı).
     * - [onekli] = false (SOROBAN2, SOROBAN6): anahtar `colorKeys[i]`, seçili için başına "sel_";
     *   sıfırlama anahtarları SİLER (Android: getSoroban2Colors/getSoroban6Colors kalıbı).
     */
    class BeadColorSpec(
        val prefix: String,
        val selPrefix: String,
        val defaults: IntArray,
        val selDefaults: IntArray,
        val labels: Array<String>,
        val onekli: Boolean = true,
        val colorKeys: Array<String> = emptyArray(),
    ) {
        val size: Int get() = defaults.size

        fun key(selected: Boolean, index: Int): String =
            if (onekli) "${if (selected) selPrefix else prefix}$index"
            else (if (selected) "sel_" else "") + colorKeys[index]

        fun defaultsFor(selected: Boolean): IntArray = if (selected) selDefaults else defaults
    }

    fun beadColorSpec(type: BeadType): BeadColorSpec = when (type) {
        BeadType.SOROBAN -> BeadColorSpec(PREFIX_SOROBAN, PREFIX_SOROBAN_SEL, SOROBAN_DEFAULT_COLORS, SOROBAN_SEL_DEFAULT_COLORS, SOROBAN_PATH_LABELS)
        BeadType.ANIMAL -> BeadColorSpec(PREFIX_ANIMAL, PREFIX_ANIMAL_SEL, ANIMAL_DEFAULT_COLORS, ANIMAL_SEL_DEFAULT_COLORS, ANIMAL_COLOR_LABELS)
        BeadType.ANIMAL2 -> BeadColorSpec(PREFIX_ANIMAL2, PREFIX_ANIMAL2_SEL, ANIMAL2_DEFAULT_COLORS, ANIMAL2_SEL_DEFAULT_COLORS, ANIMAL2_COLOR_LABELS)
        BeadType.ANIMAL3 -> BeadColorSpec(PREFIX_ANIMAL3, PREFIX_ANIMAL3_SEL, ANIMAL3_DEFAULT_COLORS, ANIMAL3_SEL_DEFAULT_COLORS, ANIMAL3_COLOR_LABELS)
        BeadType.ANIMAL4 -> BeadColorSpec(PREFIX_ANIMAL4, PREFIX_ANIMAL4_SEL, ANIMAL4_DEFAULT_COLORS, ANIMAL4_SEL_DEFAULT_COLORS, ANIMAL4_COLOR_LABELS)
        BeadType.ANIMAL5 -> BeadColorSpec(PREFIX_ANIMAL5, PREFIX_ANIMAL5_SEL, ANIMAL5_DEFAULT_COLORS, ANIMAL5_SEL_DEFAULT_COLORS, ANIMAL5_COLOR_LABELS)
        BeadType.ANIMAL6 -> BeadColorSpec(PREFIX_ANIMAL6, PREFIX_ANIMAL6_SEL, ANIMAL6_DEFAULT_COLORS, ANIMAL6_SEL_DEFAULT_COLORS, ANIMAL6_COLOR_LABELS)
        BeadType.ANIMAL7 -> BeadColorSpec(PREFIX_ANIMAL7, PREFIX_ANIMAL7_SEL, ANIMAL7_DEFAULT_COLORS, ANIMAL7_SEL_DEFAULT_COLORS, ANIMAL7_COLOR_LABELS)
        BeadType.ANIMAL8 -> BeadColorSpec(PREFIX_ANIMAL8, PREFIX_ANIMAL8_SEL, ANIMAL8_DEFAULT_COLORS, ANIMAL8_SEL_DEFAULT_COLORS, ANIMAL8_COLOR_LABELS)
        BeadType.ANIMAL9 -> BeadColorSpec(PREFIX_ANIMAL9, PREFIX_ANIMAL9_SEL, ANIMAL9_DEFAULT_COLORS, ANIMAL9_SEL_DEFAULT_COLORS, ANIMAL9_COLOR_LABELS)
        BeadType.BOWLING -> BeadColorSpec(PREFIX_BOWLING, PREFIX_BOWLING_SEL, BOWLING_DEFAULT_COLORS, BOWLING_SEL_DEFAULT_COLORS, BOWLING_COLOR_LABELS)
        BeadType.BALL1 -> BeadColorSpec(PREFIX_BALL1, PREFIX_BALL1_SEL, BALL1_DEFAULT_COLORS, BALL1_SEL_DEFAULT_COLORS, BALL1_COLOR_LABELS)
        BeadType.BALL3 -> BeadColorSpec(PREFIX_BALL3, PREFIX_BALL3_SEL, BALL3_DEFAULT_COLORS, BALL3_SEL_DEFAULT_COLORS, BALL3_COLOR_LABELS)
        BeadType.BALL4 -> BeadColorSpec(PREFIX_BALL4, PREFIX_BALL4_SEL, BALL4_DEFAULT_COLORS, BALL4_SEL_DEFAULT_COLORS, BALL4_COLOR_LABELS)
        BeadType.BALL5 -> BeadColorSpec(PREFIX_BALL5, PREFIX_BALL5_SEL, BALL5_DEFAULT_COLORS, BALL5_SEL_DEFAULT_COLORS, BALL5_COLOR_LABELS)
        BeadType.BALL6 -> BeadColorSpec(PREFIX_BALL6, PREFIX_BALL6_SEL, BALL6_DEFAULT_COLORS, BALL6_SEL_DEFAULT_COLORS, BALL6_COLOR_LABELS)
        BeadType.BALL7 -> BeadColorSpec(PREFIX_BALL7, PREFIX_BALL7_SEL, BALL7_DEFAULT_COLORS, BALL7_SEL_DEFAULT_COLORS, BALL7_COLOR_LABELS)
        BeadType.BALL8 -> BeadColorSpec(PREFIX_BALL8, PREFIX_BALL8_SEL, BALL8_DEFAULT_COLORS, BALL8_SEL_DEFAULT_COLORS, BALL8_COLOR_LABELS)
        BeadType.BALL9 -> BeadColorSpec(PREFIX_BALL9, PREFIX_BALL9_SEL, BALL9_DEFAULT_COLORS, BALL9_SEL_DEFAULT_COLORS, BALL9_COLOR_LABELS)
        BeadType.BALL10 -> BeadColorSpec(PREFIX_BALL10, PREFIX_BALL10_SEL, BALL10_DEFAULT_COLORS, BALL10_SEL_DEFAULT_COLORS, BALL10_COLOR_LABELS)
        BeadType.BALL11 -> BeadColorSpec(PREFIX_BALL11, PREFIX_BALL11_SEL, BALL11_DEFAULT_COLORS, BALL11_SEL_DEFAULT_COLORS, BALL11_COLOR_LABELS)
        BeadType.BALL12 -> BeadColorSpec(PREFIX_BALL12, PREFIX_BALL12_SEL, BALL12_DEFAULT_COLORS, BALL12_SEL_DEFAULT_COLORS, BALL12_COLOR_LABELS)
        BeadType.BALL13 -> BeadColorSpec(PREFIX_BALL13, PREFIX_BALL13_SEL, BALL13_DEFAULT_COLORS, BALL13_SEL_DEFAULT_COLORS, BALL13_COLOR_LABELS)
        BeadType.BALL14 -> BeadColorSpec(PREFIX_BALL14, PREFIX_BALL14_SEL, BALL14_DEFAULT_COLORS, BALL14_SEL_DEFAULT_COLORS, BALL14_COLOR_LABELS)
        BeadType.SOROBAN2 -> BeadColorSpec("", "", SOROBAN2_DEFAULT_COLORS, SOROBAN2_SEL_DEFAULT_COLORS, SOROBAN2_COLOR_LABELS, onekli = false, colorKeys = SOROBAN2_COLOR_KEYS)
        BeadType.SOROBAN6 -> BeadColorSpec("", "", SOROBAN6_DEFAULT_COLORS, SOROBAN6_SEL_DEFAULT_COLORS, SOROBAN6_COLOR_LABELS, onekli = false, colorKeys = SOROBAN6_COLOR_KEYS)
    }

    /** Tavşan (BeadType değil; Android'de get/set/resetRabbitColors). */
    val RABBIT_COLOR_SPEC: BeadColorSpec
        get() = BeadColorSpec(PREFIX_RABBIT, PREFIX_RABBIT_SEL, RABBIT_DEFAULT_COLORS, RABBIT_SEL_DEFAULT_COLORS, RABBIT_COLOR_LABELS)

    private fun getColors(store: AbacusPrefsStore, spec: BeadColorSpec, selected: Boolean): IntArray {
        val defaults = spec.defaultsFor(selected)
        return IntArray(spec.size) { i -> store.getInt(spec.key(selected, i), defaults[i]) }
    }

    private fun setColor(store: AbacusPrefsStore, spec: BeadColorSpec, selected: Boolean, colorIndex: Int, color: Int) {
        store.putInt(spec.key(selected, colorIndex), color)
    }

    private fun resetColors(store: AbacusPrefsStore, spec: BeadColorSpec, selected: Boolean) {
        if (spec.onekli) {
            spec.defaultsFor(selected).forEachIndexed { i, c -> store.putInt(spec.key(selected, i), c) }
        } else {
            for (i in 0 until spec.size) store.remove(spec.key(selected, i))
        }
    }

    /** Android: getSorobanColors / getAnimalColors / ... / getSoroban6Colors. */
    fun getBeadColors(store: AbacusPrefsStore, type: BeadType, selected: Boolean): IntArray =
        getColors(store, beadColorSpec(type), selected)

    /**
     * Android: setSorobanColor / setAnimalColor / ... / setSoroban6Color.
     * Önekli tiplerde dizin sınırı denetlenmez (Android de denetlemiyor); SOROBAN2/6'da geçersiz
     * dizin Android'deki gibi dizi sınırı hatası verir.
     */
    fun setBeadColor(store: AbacusPrefsStore, type: BeadType, selected: Boolean, colorIndex: Int, color: Int) =
        setColor(store, beadColorSpec(type), selected, colorIndex, color)

    /** Android: resetSorobanColors / ... / resetSoroban6Colors. */
    fun resetBeadColors(store: AbacusPrefsStore, type: BeadType, selected: Boolean) =
        resetColors(store, beadColorSpec(type), selected)

    fun getRabbitColors(store: AbacusPrefsStore, selected: Boolean): IntArray = getColors(store, RABBIT_COLOR_SPEC, selected)
    fun setRabbitColor(store: AbacusPrefsStore, selected: Boolean, colorIndex: Int, color: Int) =
        setColor(store, RABBIT_COLOR_SPEC, selected, colorIndex, color)
    fun resetRabbitColors(store: AbacusPrefsStore, selected: Boolean) = resetColors(store, RABBIT_COLOR_SPEC, selected)

    // ── Frame colors ──────────────────────────────────────────────────────────

    const val FRAME_DEFAULT_WOOD = 0xFFDDA379.toInt()
    const val FRAME_DEFAULT_HEART = 0xFFE63946.toInt()
    const val FRAME_DEFAULT_STAR = 0xFFDDFF33.toInt()
    val FRAME_BG4_DEFAULT_COLORS = intArrayOf(
        0xFFFEDA77.toInt(),
        0xFFF58529.toInt(),
        0xFFDD2A7B.toInt(),
        0xFF8134AF.toInt(),
        0xFF515BD4.toInt()
    )
    val FRAME_BG4_KEYS = arrayOf(KEY_FRAME_BG4_C1, KEY_FRAME_BG4_C2, KEY_FRAME_BG4_C3, KEY_FRAME_BG4_C4, KEY_FRAME_BG4_C5)

    fun getFrameBgWoodColor(store: AbacusPrefsStore): Int = store.getInt(KEY_FRAME_BG_WOOD, FRAME_DEFAULT_WOOD)
    fun setFrameBgWoodColor(store: AbacusPrefsStore, color: Int) = store.putInt(KEY_FRAME_BG_WOOD, color)

    fun getFrameBg2WoodColor(store: AbacusPrefsStore): Int = store.getInt(KEY_FRAME_BG2_WOOD, FRAME_DEFAULT_WOOD)
    fun setFrameBg2WoodColor(store: AbacusPrefsStore, color: Int) = store.putInt(KEY_FRAME_BG2_WOOD, color)

    fun getFrameBg2HeartColor(store: AbacusPrefsStore): Int = store.getInt(KEY_FRAME_BG2_HEART, FRAME_DEFAULT_HEART)
    fun setFrameBg2HeartColor(store: AbacusPrefsStore, color: Int) = store.putInt(KEY_FRAME_BG2_HEART, color)

    fun getFrameBg3WoodColor(store: AbacusPrefsStore): Int = store.getInt(KEY_FRAME_BG3_WOOD, FRAME_DEFAULT_WOOD)
    fun setFrameBg3WoodColor(store: AbacusPrefsStore, color: Int) = store.putInt(KEY_FRAME_BG3_WOOD, color)

    fun getFrameBg3StarColor(store: AbacusPrefsStore): Int = store.getInt(KEY_FRAME_BG3_STAR, FRAME_DEFAULT_STAR)
    fun setFrameBg3StarColor(store: AbacusPrefsStore, color: Int) = store.putInt(KEY_FRAME_BG3_STAR, color)

    fun getFrameBg4Colors(store: AbacusPrefsStore): IntArray =
        IntArray(5) { i -> store.getInt(FRAME_BG4_KEYS[i], FRAME_BG4_DEFAULT_COLORS[i]) }

    /** Geçersiz dizin (0..4 dışı) sessizce yok sayılır (Android ile aynı). */
    fun setFrameBg4Color(store: AbacusPrefsStore, index: Int, color: Int) {
        if (index !in 0..4) return
        store.putInt(FRAME_BG4_KEYS[index], color)
    }

    /** Tüm çerçeve renklerini varsayılanlara YAZAR (silmez). */
    fun resetAllFrameColors(store: AbacusPrefsStore) {
        store.putInt(KEY_FRAME_BG_WOOD, FRAME_DEFAULT_WOOD)
        store.putInt(KEY_FRAME_BG2_WOOD, FRAME_DEFAULT_WOOD)
        store.putInt(KEY_FRAME_BG2_HEART, FRAME_DEFAULT_HEART)
        store.putInt(KEY_FRAME_BG3_WOOD, FRAME_DEFAULT_WOOD)
        store.putInt(KEY_FRAME_BG3_STAR, FRAME_DEFAULT_STAR)
        for (i in 0..4) store.putInt(FRAME_BG4_KEYS[i], FRAME_BG4_DEFAULT_COLORS[i])
    }
}

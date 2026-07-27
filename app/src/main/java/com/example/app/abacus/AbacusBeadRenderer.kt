package com.example.app.abacus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.app.R
import kotlin.math.abs

/**
 * Builds bead drawables that respect user colour preferences stored in [AbacusPreferences].
 *
 * - SOROBAN (3 simple paths, no clip-paths): LayerDrawable from per-path XML files.
 *   Each layer is tinted with [DrawableCompat.setTint] (SRC_IN mode).
 * - RABBIT (13 paths, uses clip-paths): original drawable is rasterised to a Bitmap,
 *   then per-pixel colour replacement maps known fill colours to user-chosen colours.
 *   Anti-aliased edge pixels are handled with a small Manhattan-distance tolerance (≤30).
 */
object AbacusBeadRenderer {


    // ── Animal colour maps ────────────────────────────────────────────────────

    private val ANIMAL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFFFFFFF.toInt() to 0,
        0xFF777873.toInt() to 1,
        0xFF4D4C48.toInt() to 2,
        0xFF32302D.toInt() to 3
    )

    private val ANIMAL_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB2B2B2.toInt() to 0,
        0xFF535450.toInt() to 1,
        0xFF353532.toInt() to 2,
        0xFF23211F.toInt() to 3
    )

    // ── Animal 1 (Dog) colour maps ──────────────────────────────────────────────────

    private val ANIMAL2_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB76057.toInt() to 0,
        0xFFFBBB8C.toInt() to 1,
        0xFF32302D.toInt() to 2,
        0xFF914844.toInt() to 3,
        0xFFE8A070.toInt() to 4
    )

    private val ANIMAL2_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF80433C.toInt() to 0,
        0xFFAF8262.toInt() to 1,
        0xFF23211F.toInt() to 2,
        0xFF65322F.toInt() to 3,
        0xFFA2704E.toInt() to 4
    )

    // ── Animal 3 colour maps ──────────────────────────────────────────────────

    private val ANIMAL3_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFF99E4D.toInt() to 0,
        0xFF32302D.toInt() to 1,
        0xFFE58435.toInt() to 2,
        0xFFFDD2B9.toInt() to 3,
        0xFFD36640.toInt() to 4
    )

    private val ANIMAL3_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFAE6E35.toInt() to 0,
        0xFF23211F.toInt() to 1,
        0xFFA05C25.toInt() to 2,
        0xFFB19381.toInt() to 3,
        0xFF93472C.toInt() to 4
    )

    // ── Animal 4 colour maps ──────────────────────────────────────────────────

    private val ANIMAL4_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFF3703A.toInt() to 0,
        0xFF32302D.toInt() to 1,
        0xFFFFFFFF.toInt() to 2,
        0xFFCC482E.toInt() to 3
    )

    private val ANIMAL4_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFAA4E28.toInt() to 0,
        0xFF23211F.toInt() to 1,
        0xFFB2B2B2.toInt() to 2,
        0xFF8E3220.toInt() to 3
    )

    // ── Animal 5 colour maps ──────────────────────────────────────────────────

    private val ANIMAL5_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFF525D65.toInt() to 1,
        0xFF7C8B96.toInt() to 2,
        0xFF929FA9.toInt() to 3,
        0xFFA6B3BD.toInt() to 4,
        0xFFEFC9A5.toInt() to 5
    )

    private val ANIMAL5_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFF394146.toInt() to 1,
        0xFF566169.toInt() to 2,
        0xFF666F76.toInt() to 3,
        0xFF747D84.toInt() to 4,
        0xFFA78C73.toInt() to 5
    )

    // ── Animal 6 colour maps ──────────────────────────────────────────────────

    private val ANIMAL6_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFFD1D3D4.toInt() to 1,
        0xFFFFFFFF.toInt() to 2
    )

    private val ANIMAL6_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFF929394.toInt() to 1,
        0xFFB2B2B2.toInt() to 2
    )

    // ── Animal 7 colour maps ──────────────────────────────────────────────────

    private val ANIMAL7_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFFF3757A.toInt() to 1,
        0xFFF9E4D7.toInt() to 2,
        0xFFFFF1E7.toInt() to 3,
        0xFFFFFFFF.toInt() to 4
    )

    private val ANIMAL7_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFFAA5155.toInt() to 1,
        0xFFAE9F96.toInt() to 2,
        0xFFB2A8A1.toInt() to 3,
        0xFFB2B2B2.toInt() to 4
    )

    // ── Animal 8 colour maps ──────────────────────────────────────────────────

    private val ANIMAL8_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFF525D65.toInt() to 1,
        0xFFD69F90.toInt() to 2,
        0xFFFCD3C6.toInt() to 3,
        0xFFFFFFFF.toInt() to 4
    )

    private val ANIMAL8_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFF394146.toInt() to 1,
        0xFF956F64.toInt() to 2,
        0xFFB0938A.toInt() to 3,
        0xFFB2B2B2.toInt() to 4
    )

    // ── Animal 9 colour maps ──────────────────────────────────────────────────

    private val ANIMAL9_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF32302D.toInt() to 0,
        0xFF60C198.toInt() to 1,
        0xFFF2A638.toInt() to 2,
        0xFFFFC94E.toInt() to 3
    )

    private val ANIMAL9_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF23211F.toInt() to 0,
        0xFF43876A.toInt() to 1,
        0xFFA97427.toInt() to 2,
        0xFFB28C36.toInt() to 3
    )

    // ── Bowling colour maps ───────────────────────────────────────────────────

    private val BOWLING_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF5A648C.toInt() to 0, // Ball Base
        0xFF4B547A.toInt() to 1, // Shadows/Holes
        0xFF7585B3.toInt() to 2, // Highlight
        0xFF383E61.toInt() to 3, // Stroke
        0xFF404A73.toInt() to 4  // Holes Inner
    )

    private val BOWLING_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF323A56.toInt() to 0,
        0xFF272C44.toInt() to 1,
        0xFF404A66.toInt() to 2,
        0xFF1D2033.toInt() to 3,
        0xFF21263A.toInt() to 4
    )

    // ── Ball1 colour maps ───────────────────────────────────────────────────

    private val BALL1_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB8DB6A.toInt() to 0,
        0xFFF7FFAE.toInt() to 1,
        0xFF92BF62.toInt() to 2
    )

    private val BALL1_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF8AA44F.toInt() to 0,
        0xFFB9BF82.toInt() to 1,
        0xFF6D8F49.toInt() to 2
    )

    // ── Ball3 colour maps ───────────────────────────────────────────────────

    private val BALL3_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF2D4049.toInt() to 0,
        0xFFFFFFFF.toInt() to 1,
        0xFF1D272B.toInt() to 2,
        0xFF1B2A30.toInt() to 3
    )

    private val BALL3_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF213036.toInt() to 0,
        0xFFBFBFBF.toInt() to 1,
        0xFF151D20.toInt() to 2,
        0xFF141F24.toInt() to 3
    )

    // ── Ball4 colour maps ───────────────────────────────────────────────────

    private val BALL4_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFF7845C.toInt() to 0,
        0xFFAA5943.toInt() to 1,
        0xFF873F2E.toInt() to 2,
        0xFFDD694A.toInt() to 3
    )

    private val BALL4_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB96345.toInt() to 0,
        0xFF7F4232.toInt() to 1,
        0xFF652F22.toInt() to 2,
        0xFFA54E37.toInt() to 3
    )

    // ── Ball5 colour maps ───────────────────────────────────────────────────

    private val BALL5_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFDDE8ED.toInt() to 0,
        0xFF2D4049.toInt() to 1,
        0xFFC5D7DD.toInt() to 2,
        0xFF1D2E35.toInt() to 3
    )

    private val BALL5_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFA5AEB1.toInt() to 0,
        0xFF213036.toInt() to 1,
        0xFF93A1A5.toInt() to 2,
        0xFF152227.toInt() to 3
    )

    // ── Ball6 colour maps ───────────────────────────────────────────────────

    private val BALL6_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB22D34.toInt() to 0,
        0xFF512A2E.toInt() to 1,
        0xFF932A34.toInt() to 2,
        0xFF381D21.toInt() to 3
    )

    private val BALL6_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF852127.toInt() to 0,
        0xFF3C1F22.toInt() to 1,
        0xFF6E1F27.toInt() to 2,
        0xFF2A1518.toInt() to 3
    )

    // ── Ball7 colour maps ───────────────────────────────────────────────────

    private val BALL7_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFECECEC.toInt() to 0,
        0xFFDDDDDD.toInt() to 1,
        0xFFFFFFFF.toInt() to 2
    )

    private val BALL7_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB1B1B1.toInt() to 0,
        0xFFA5A5A5.toInt() to 1,
        0xFFBFBFBF.toInt() to 2
    )

    // ── Ball8 colour maps ───────────────────────────────────────────────────

    private val BALL8_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF7F5B53.toInt() to 0,
        0xFFD8D8D8.toInt() to 1,
        0xFF44302D.toInt() to 2,
        0xFFFFFFFF.toInt() to 3
    )

    private val BALL8_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFF5F443E.toInt() to 0,
        0xFFA2A2A2.toInt() to 1,
        0xFF332421.toInt() to 2,
        0xFFBFBFBF.toInt() to 3
    )

    // ── Ball9 colour maps ───────────────────────────────────────────────────

    private val BALL9_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFECECEC.toInt() to 0,
        0xFFEDD314.toInt() to 1,
        0xFF365FC6.toInt() to 2,
        0xFF274789.toInt() to 3,
        0xFFFFFFFF.toInt() to 4
    )

    private val BALL9_SEL_ORIGINAL_MAP: Map<Int, Int> = mapOf(
        0xFFB1B1B1.toInt() to 0,
        0xFFB19E0F.toInt() to 1,
        0xFF284794.toInt() to 2,
        0xFF1D3566.toInt() to 3,
        0xFFBFBFBF.toInt() to 4
    )


    private val SOROBAN6_ORIGINAL_MAP = mapOf(
        0xFF9AE2C6.toInt() to 0,
        0xFFFFFFFF.toInt() to 1,
        0xFF005533.toInt() to 2
    )
    private val SOROBAN6_SEL_ORIGINAL_MAP = mapOf(
        0xFF9AE2C6.toInt() to 0,
        0xFFFFFFFF.toInt() to 1,
        0xFF005533.toInt() to 2
    )

    // ── Balls 10-14 colour maps ───────────────────────────────────────────────

    private val BALL10_ORIGINAL_MAP = mapOf(0xFFECECEC.toInt() to 0, 0xFFCECECE.toInt() to 1, 0xFF878783.toInt() to 2, 0xFF373735.toInt() to 3, 0xFFFFFFFF.toInt() to 4)
    private val BALL10_SEL_ORIGINAL_MAP = mapOf(0xFFB1B1B1.toInt() to 0, 0xFF9A9A9A.toInt() to 1, 0xFF656562.toInt() to 2, 0xFF292927.toInt() to 3, 0xFFBFBFBF.toInt() to 4)

    private val BALL11_ORIGINAL_MAP = mapOf(0xFFF76D2F.toInt() to 0, 0xFFE0602D.toInt() to 1, 0xFF583A2A.toInt() to 2, 0xFFFFFFFF.toInt() to 3)
    private val BALL11_SEL_ORIGINAL_MAP = mapOf(0xFFB95123.toInt() to 0, 0xFFA84821.toInt() to 1, 0xFF422B1F.toInt() to 2, 0xFFBFBFBF.toInt() to 3)

    private val BALL12_ORIGINAL_MAP = mapOf(0xFFE0EC44.toInt() to 0, 0xFFFFFFFF.toInt() to 1, 0xFFEAEAEA.toInt() to 2, 0xFFD8D844.toInt() to 3, 0xFFDBDBDB.toInt() to 4)
    private val BALL12_SEL_ORIGINAL_MAP = mapOf(0xFFA8B133.toInt() to 0, 0xFFBFBFBF.toInt() to 1, 0xFFAFAFAF.toInt() to 2, 0xFFA2A233.toInt() to 3, 0xFFA4A4A4.toInt() to 4)

    private val BALL13_ORIGINAL_MAP = mapOf(0xFFECECEC.toInt() to 0, 0xFFCECECE.toInt() to 1, 0xFFBC3F3D.toInt() to 2, 0xFFFFFFFF.toInt() to 3)
    private val BALL13_SEL_ORIGINAL_MAP = mapOf(0xFFB1B1B1.toInt() to 0, 0xFF9A9A9A.toInt() to 1, 0xFF8D2F2D.toInt() to 2, 0xFFBFBFBF.toInt() to 3)

    private val BALL14_ORIGINAL_MAP = mapOf(0xFF010102.toInt() to 0, 0xFF3C3C3C.toInt() to 1, 0xFFFFFFFF.toInt() to 2)
    private val BALL14_SEL_ORIGINAL_MAP = mapOf(0xFF000001.toInt() to 0, 0xFF2D2D2D.toInt() to 1, 0xFFBFBFBF.toInt() to 2)

    // Tolerance for pixel colour matching (Manhattan distance across R+G+B)
    private const val COLOR_TOLERANCE = 150

    // Render size cap for rabbit bitmap (dp). Keeps memory usage low.
    private const val RABBIT_RENDER_DP = 120

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Builds a drawable for the currently selected bead type, respecting saved colours.
     * Called from [AbacusBeadController.updateBeadAppearance].
     */
    fun buildCurrentBead(context: Context, isSelected: Boolean): Drawable =
        when (AbacusPreferences.getBeadType(context)) {
            AbacusPreferences.BeadType.SOROBAN -> buildSorobanDrawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL -> buildAnimalDrawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL2 -> buildAnimal2Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL3 -> buildAnimal3Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL4 -> buildAnimal4Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL5 -> buildAnimal5Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL6 -> buildAnimal6Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL7 -> buildAnimal7Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL8 -> buildAnimal8Drawable(context, isSelected)
            AbacusPreferences.BeadType.ANIMAL9 -> buildAnimal9Drawable(context, isSelected)
            AbacusPreferences.BeadType.BOWLING -> buildBowlingDrawable(context, isSelected)
            AbacusPreferences.BeadType.BALL1 -> buildBall1Drawable(context, isSelected)
            AbacusPreferences.BeadType.BALL3 -> buildBall3Drawable(context, isSelected)
            AbacusPreferences.BeadType.BALL4 -> buildBall4Drawable(context, isSelected)
            AbacusPreferences.BeadType.BALL5 -> buildBall5Drawable(context, isSelected)
            AbacusPreferences.BeadType.BALL6 -> buildBall6Drawable(context, isSelected)
            AbacusPreferences.BeadType.BALL7 -> buildBall7Drawable(context, isSelected)
            AbacusPreferences.BeadType.BALL8 -> buildBall8Drawable(context, isSelected)
            AbacusPreferences.BeadType.BALL9 -> buildBall9Drawable(context, isSelected)
            AbacusPreferences.BeadType.BALL10 -> buildBall10Drawable(context, isSelected)
            AbacusPreferences.BeadType.BALL11 -> buildBall11Drawable(context, isSelected)
            AbacusPreferences.BeadType.BALL12 -> buildBall12Drawable(context, isSelected)
            AbacusPreferences.BeadType.BALL13 -> buildBall13Drawable(context, isSelected)
            AbacusPreferences.BeadType.BALL14 -> buildBall14Drawable(context, isSelected)
            AbacusPreferences.BeadType.SOROBAN2 -> buildSoroban2Drawable(context, isSelected)
            AbacusPreferences.BeadType.SOROBAN6 -> buildSoroban6Drawable(context, isSelected)

            else -> buildSorobanDrawable(context, isSelected)
        }

    /**
     * Builds a soroban bead [LayerDrawable] with per-path tinting.
     * Layer order mirrors soroban_bead.xml: outer shell → inner fill → right highlight.
     */
    fun buildSorobanDrawable(context: Context, selected: Boolean): Drawable {
        val colors = AbacusPreferences.getSorobanColors(context, selected)
        val resIds = if (selected) {
            intArrayOf(
                R.drawable.soroban_bead_selected_layer1,
                R.drawable.soroban_bead_selected_layer2,
                R.drawable.soroban_bead_selected_layer3
            )
        } else {
            intArrayOf(
                R.drawable.soroban_bead_layer1,
                R.drawable.soroban_bead_layer2,
                R.drawable.soroban_bead_layer3
            )
        }

        val layers = Array(3) { i ->
            val d: Drawable = ContextCompat.getDrawable(context, resIds[i])!!.mutate()
            val wrapped = DrawableCompat.wrap(d)
            DrawableCompat.setTint(wrapped, colors[i])
            DrawableCompat.setTintMode(wrapped, PorterDuff.Mode.SRC_IN)
            wrapped
        }
        return LayerDrawable(layers)
    }

    /**
     * Builds an animal head bead drawable using bitmap pixel-colour replacement.
     */
    fun buildAnimalDrawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimalColors(context, selected)
        val map = if (selected) ANIMAL_SEL_ORIGINAL_MAP else ANIMAL_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic1 else R.drawable.animal_head_ic1
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal2Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal2Colors(context, selected)
        val map = if (selected) ANIMAL2_SEL_ORIGINAL_MAP else ANIMAL2_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic2 else R.drawable.animal_head_ic2
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal3Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal3Colors(context, selected)
        val map = if (selected) ANIMAL3_SEL_ORIGINAL_MAP else ANIMAL3_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic3 else R.drawable.animal_head_ic3
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal4Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal4Colors(context, selected)
        val map = if (selected) ANIMAL4_SEL_ORIGINAL_MAP else ANIMAL4_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic4 else R.drawable.animal_head_ic4
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal5Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal5Colors(context, selected)
        val map = if (selected) ANIMAL5_SEL_ORIGINAL_MAP else ANIMAL5_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic5 else R.drawable.animal_head_ic5
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal6Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal6Colors(context, selected)
        val map = if (selected) ANIMAL6_SEL_ORIGINAL_MAP else ANIMAL6_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic6 else R.drawable.animal_head_ic6
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal7Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal7Colors(context, selected)
        val map = if (selected) ANIMAL7_SEL_ORIGINAL_MAP else ANIMAL7_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic7 else R.drawable.animal_head_ic7
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal8Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal8Colors(context, selected)
        val map = if (selected) ANIMAL8_SEL_ORIGINAL_MAP else ANIMAL8_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic8 else R.drawable.animal_head_ic8
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildAnimal9Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getAnimal9Colors(context, selected)
        val map = if (selected) ANIMAL9_SEL_ORIGINAL_MAP else ANIMAL9_ORIGINAL_MAP
        val res = if (selected) R.drawable.animal_head_selected_ic9 else R.drawable.animal_head_ic9
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBowlingDrawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBowlingColors(context, selected)
        val map = if (selected) BOWLING_SEL_ORIGINAL_MAP else BOWLING_ORIGINAL_MAP
        val res = if (selected) R.drawable.bowling_ball_selected_ic else R.drawable.bowling_ball_ic
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBall1Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBall1Colors(context, selected)
        val map = if (selected) BALL1_SEL_ORIGINAL_MAP else BALL1_ORIGINAL_MAP
        val res = if (selected) R.drawable.abacus_bead_selected_ball1 else R.drawable.abacus_bead_ball1
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBall3Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBall3Colors(context, selected)
        val map = if (selected) BALL3_SEL_ORIGINAL_MAP else BALL3_ORIGINAL_MAP
        val res = if (selected) R.drawable.abacus_bead_selected_ball3 else R.drawable.abacus_bead_ball3
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBall4Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBall4Colors(context, selected)
        val map = if (selected) BALL4_SEL_ORIGINAL_MAP else BALL4_ORIGINAL_MAP
        val res = if (selected) R.drawable.abacus_bead_selected_ball4 else R.drawable.abacus_bead_ball4
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBall5Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBall5Colors(context, selected)
        val map = if (selected) BALL5_SEL_ORIGINAL_MAP else BALL5_ORIGINAL_MAP
        val res = if (selected) R.drawable.abacus_bead_selected_ball5 else R.drawable.abacus_bead_ball5
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBall6Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBall6Colors(context, selected)
        val map = if (selected) BALL6_SEL_ORIGINAL_MAP else BALL6_ORIGINAL_MAP
        val res = if (selected) R.drawable.abacus_bead_selected_ball6 else R.drawable.abacus_bead_ball6
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBall7Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBall7Colors(context, selected)
        val map = if (selected) BALL7_SEL_ORIGINAL_MAP else BALL7_ORIGINAL_MAP
        val res = if (selected) R.drawable.abacus_bead_selected_ball7 else R.drawable.abacus_bead_ball7
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBall8Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBall8Colors(context, selected)
        val map = if (selected) BALL8_SEL_ORIGINAL_MAP else BALL8_ORIGINAL_MAP
        val res = if (selected) R.drawable.abacus_bead_selected_ball8 else R.drawable.abacus_bead_ball8
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBall9Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBall9Colors(context, selected)
        val map = if (selected) BALL9_SEL_ORIGINAL_MAP else BALL9_ORIGINAL_MAP
        val res = if (selected) R.drawable.abacus_bead_selected_ball9 else R.drawable.abacus_bead_ball9
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBall10Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBall10Colors(context, selected)
        val map = if (selected) BALL10_SEL_ORIGINAL_MAP else BALL10_ORIGINAL_MAP
        val res = if (selected) R.drawable.abacus_bead_selected_ball10 else R.drawable.abacus_bead_ball10
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBall11Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBall11Colors(context, selected)
        val map = if (selected) BALL11_SEL_ORIGINAL_MAP else BALL11_ORIGINAL_MAP
        val res = if (selected) R.drawable.abacus_bead_selected_ball11 else R.drawable.abacus_bead_ball11
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBall12Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBall12Colors(context, selected)
        val map = if (selected) BALL12_SEL_ORIGINAL_MAP else BALL12_ORIGINAL_MAP
        val res = if (selected) R.drawable.abacus_bead_selected_ball12 else R.drawable.abacus_bead_ball12
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBall13Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBall13Colors(context, selected)
        val map = if (selected) BALL13_SEL_ORIGINAL_MAP else BALL13_ORIGINAL_MAP
        val res = if (selected) R.drawable.abacus_bead_selected_ball13 else R.drawable.abacus_bead_ball13
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    fun buildBall14Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getBall14Colors(context, selected)
        val map = if (selected) BALL14_SEL_ORIGINAL_MAP else BALL14_ORIGINAL_MAP
        val res = if (selected) R.drawable.abacus_bead_selected_ball14 else R.drawable.abacus_bead_ball14
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }


    fun buildSoroban2Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getSoroban2Colors(context, selected)
        var cacheKey = if (selected) 2 else 1
        for (c in newColors) {
            cacheKey = 31 * cacheKey + c
        }

        bitmapCache[cacheKey]?.let { cachedBitmap ->
            return android.graphics.drawable.BitmapDrawable(context.resources, cachedBitmap)
        }

        val density = context.resources.displayMetrics.density
        val targetW = (RABBIT_RENDER_DP * density).toInt().coerceAtLeast(1)
        val targetH = (targetW * (92f / 120f)).toInt().coerceAtLeast(1)

        val bitmap = android.graphics.Bitmap.createBitmap(targetW, targetH, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.density = context.resources.displayMetrics.densityDpi
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        val scaleX = targetW / 120f
        val scaleY = targetH / 72f
        val offsetY = 0f
        val pillHeight = targetH.toFloat()

        // Draw outer black pill
        paint.color = android.graphics.Color.BLACK
        val outerRect = android.graphics.RectF(0f, 0f, targetW.toFloat(), targetH.toFloat())
        val outerRx = 36f * scaleX
        val outerRy = 36f * scaleY
        canvas.drawRoundRect(outerRect, outerRx, outerRy, paint)

        // Draw inner gradient pill
        val padX = 12f * scaleX
        val padY = 12f * scaleY
        val innerRect = android.graphics.RectF(padX, padY, targetW - padX, targetH - padY)
        val innerRx = 24f * scaleX
        val innerRy = 24f * scaleY

        val gradient = android.graphics.LinearGradient(
            0f, targetH.toFloat(), targetW.toFloat(), 0f,
            newColors,
            floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f),
            android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRoundRect(innerRect, innerRx, innerRy, paint)

        bitmapCache[cacheKey] = bitmap
        return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
    }

    fun buildSoroban6Drawable(context: Context, selected: Boolean): Drawable {
        val newColors = AbacusPreferences.getSoroban6Colors(context, selected)
        val map = if (selected) SOROBAN6_SEL_ORIGINAL_MAP else SOROBAN6_ORIGINAL_MAP
        val res = if (selected) R.drawable.soroban_bead6_selected else R.drawable.soroban_bead6
        return buildReplacedBitmapDrawable(context, res, map, newColors)
    }

    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    private fun buildReplacedBitmapDrawable(
        context: Context,
        srcRes: Int,
        originalMap: Map<Int, Int>,
        newColors: IntArray
    ): Drawable {
        var cacheKey = srcRes
        for (c in newColors) {
            cacheKey = 31 * cacheKey + c
        }

        bitmapCache[cacheKey]?.let { cachedBitmap ->
            return BitmapDrawable(context.resources, cachedBitmap)
        }

        val src: Drawable = ContextCompat.getDrawable(context, srcRes)!!

        val density = context.resources.displayMetrics.density
        val targetW = (RABBIT_RENDER_DP * density).toInt().coerceAtLeast(1)
        val intrinsicW = src.intrinsicWidth.coerceAtLeast(1)
        val intrinsicH = src.intrinsicHeight.coerceAtLeast(1)
        val targetH = (targetW.toFloat() * intrinsicH / intrinsicW).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        bitmap.density = context.resources.displayMetrics.densityDpi
        val canvas = Canvas(bitmap)
        src.setBounds(0, 0, targetW, targetH)
        src.draw(canvas)

        val origEntries = originalMap.entries.toList()
        val pixels = IntArray(targetW * targetH)
        bitmap.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)

        for (idx in pixels.indices) {
            val alpha = pixels[idx] ushr 24
            if (alpha < 10) continue // skip near-transparent

            val pixelArgb = pixels[idx]
            val pr = (pixelArgb ushr 16) and 0xFF
            val pg = (pixelArgb ushr 8) and 0xFF
            val pb = pixelArgb and 0xFF

            var bestScore = Float.MAX_VALUE
            var bestD = Float.MAX_VALUE
            var bestC0 = -1
            var bestC1 = -1
            var bestT = 0f

            for (i in origEntries.indices) {
                val c0 = origEntries[i].key
                val r0 = ((c0 ushr 16) and 0xFF).toFloat()
                val g0 = ((c0 ushr 8) and 0xFF).toFloat()
                val b0 = (c0 and 0xFF).toFloat()

                // Check distance to solid color first
                val d0Sq = (pr - r0) * (pr - r0) + (pg - g0) * (pg - g0) + (pb - b0) * (pb - b0)
                if (d0Sq < bestScore) {
                    bestScore = d0Sq
                    bestD = d0Sq
                    bestC0 = origEntries[i].value
                    bestC1 = origEntries[i].value
                    bestT = 0f
                }

                // Check distance to segments
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
                    
                    // Add penalty for segment length to disambiguate collinear segments (like grayscale)
                    val score = distSq + 0.001f * abLenSq

                    if (score < bestScore) {
                        bestScore = score
                        bestD = distSq
                        bestC0 = origEntries[i].value
                        bestC1 = origEntries[j].value
                        bestT = t
                    }
                }
            }

            // Apply the closest mapped color or segment unconditionally
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

        // Pass 2: Flood-fill from all border pixels to mark true background transparent pixels.
        // Any semi-transparent pixel NOT reachable from the outside = internal SVG seam → boost alpha to 255.
        val isBackground = BooleanArray(targetW * targetH) { false }
        val queue = ArrayDeque<Int>()

        // Seed the queue with all transparent pixels on the image border
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

        // BFS: spread "background" label to connected semi-transparent neighbours
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val cx = cur % targetW
            val cy = cur / targetW
            for ((dx, dy) in arrayOf(intArrayOf(-1,0), intArrayOf(1,0), intArrayOf(0,-1), intArrayOf(0,1))) {
                val nx = cx + dx
                val ny = cy + dy
                if (nx < 0 || nx >= targetW || ny < 0 || ny >= targetH) continue
                val nIdx = ny * targetW + nx
                if (!isBackground[nIdx] && (pixels[nIdx] ushr 24) < 128) {
                    isBackground[nIdx] = true
                    queue.add(nIdx)
                }
            }
        }

        // Any semi-transparent pixel that is NOT background is an internal seam — boost alpha
        for (i in pixels.indices) {
            if (!isBackground[i] && (pixels[i] ushr 24) in 1..254) {
                pixels[i] = (255 shl 24) or (pixels[i] and 0x00FFFFFF)
            }
        }

        bitmap.setPixels(pixels, 0, targetW, 0, 0, targetW, targetH)

        bitmapCache[cacheKey] = bitmap
        return BitmapDrawable(context.resources, bitmap)
    }

    // ── Colour-manipulation helpers (used by Tab 2 seekbar logic) ─────────────

    /**
     * Builds an ARGB colour from hue (0–359) and lightness (0–100 as int).
     * 0–50: Black to Full Color
     * 50–100: Full Color to White
     * Alpha is always 0xFF (fully opaque).
     */
    fun makeColor(hueDegrees: Int, lightness100: Int): Int {
        val l = lightness100.coerceIn(0, 100)
        val s = if (l <= 50) FIXED_SATURATION else FIXED_SATURATION * (1f - (l - 50) / 50f)
        val v = if (l <= 50) l / 50f else 1f
        val hsv = floatArrayOf((hueDegrees % 360).toFloat(), s, v)
        return Color.HSVToColor(hsv)
    }

    /**
     * Returns the hue (0–359) of [color] in HSV space.
     * Returns 0 for achromatic colours.
     */
    fun getHue(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        return hsv[0].toInt()
    }

    /**
     * Returns the lightness (0–100) of [color] matching the makeColor gradient.
     */
    fun getLightness100(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        val s = hsv[1]
        val v = hsv[2]
        return if (s < FIXED_SATURATION - 0.05f) {
            (50 + 50 * (1f - s / FIXED_SATURATION)).toInt().coerceIn(50, 100)
        } else {
            (50 * v).toInt().coerceIn(0, 50)
        }
    }

    /**
     * Legacy single-seekbar helper kept for frame colour seekbars.
     * Shifts hue while boosting saturation so the change is always visible.
     */
    fun shiftHue(baseColor: Int, hueDegrees: Int, minSaturation: Float = 0.65f): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        hsv[0] = (hueDegrees % 360).toFloat()
        if (hsv[1] < minSaturation) hsv[1] = minSaturation
        if (hsv[2] < 0.12f) hsv[2] = 0.35f
        val rgb = Color.HSVToColor(hsv)
        val alpha = (baseColor ushr 24) and 0xFF
        return (alpha shl 24) or (rgb and 0x00FFFFFF)
    }

    internal const val FIXED_SATURATION = 0.90f
}

package com.example.app

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import com.example.app.R

/**
 * Boncuk teşhis logları. Logcat filtresi: TutorialBeadDiag
 *
 * Yalnızca debug derlemesinde çalışır. [rod3Snapshot] / [rod4Snapshot] her çağrıda
 * `getIdentifier` ile kaynak adı araması yapıyor; bu ucuz bir işlem değil ve release'de
 * kullanıcıya hiçbir faydası yok.
 */
object TutorialBeadDiagnostics {
    const val TAG = "TutorialBeadDiag"

    /**
     * Debug'da açık, release'de kapalı.
     *
     * `const val` değil çünkü `BuildConfig.DEBUG` derleme zamanı sabiti değildir. Release'de
     * değer false olduğu için gövdeler çalışmaz; R8 ayrıca çağrıları ve onlara verilen
     * string birleştirmelerini eler.
     */
    val ENABLED = BuildConfig.DEBUG

    fun log(message: String) {
        if (ENABLED) Log.d(TAG, message)
    }

    fun drawableLabel(context: Context, bead: ImageView?): String {
        if (bead == null) return "bead=null"
        val d = bead.drawable ?: return "drawable=null"
        val selected = context.getDrawable(R.drawable.soroban_bead_selected)?.constantState
        val normal = context.getDrawable(R.drawable.soroban_bead)?.constantState
        return when (d.constantState) {
            selected -> "SELECTED"
            normal -> "normal"
            else -> "other(${d.constantState?.hashCode()})"
        }
    }

    fun beadState(context: Context, root: android.view.View, beadId: String): String {
        val bead = root.findViewById<ImageView>(
            root.resources.getIdentifier(beadId, "id", root.context.packageName),
        ) ?: return "$beadId NOT_FOUND"
        val params = bead.layoutParams as? ViewGroup.MarginLayoutParams
        val margin = params?.bottomMargin ?: -1
        return "$beadId drawable=${drawableLabel(context, bead)} marginBottom=$margin translationY=${bead.translationY}"
    }

    fun rod3Snapshot(context: Context, root: android.view.View, label: String) {
        if (!ENABLED) return
        log(
            "[$label] " +
                beadState(context, root, "rod3_bead_bottom4") + " | " +
                beadState(context, root, "rod3_bead_top"),
        )
    }

    fun rod4Snapshot(context: Context, root: android.view.View, label: String) {
        if (!ENABLED) return
        log(
            "[$label] " +
                beadState(context, root, "rod4_bead_bottom2") + " | " +
                beadState(context, root, "rod4_bead_bottom1"),
        )
    }
}

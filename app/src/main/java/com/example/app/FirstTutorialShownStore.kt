package com.example.app

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * İlk tutorial tamamlandı bayrağı: yerel [AppPrefs] + Firestore `users/{uid}.first_tutorial_shown`.
 * Splash okurken bulut false olsa bile yerel true asla ezilmez.
 */
object FirstTutorialShownStore {

    private const val PREFS_NAME = "AppPrefs"
    private const val KEY = "first_tutorial_shown"

    fun readLocal(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY, false)

    /**
     * @param firestoreValue dokümandan okunan değer; doküman yoksa null.
     * @return tutorial gösterilmiş sayılır mı
     */
    fun resolveShown(
        context: Context,
        firestoreValue: Boolean?,
        logSource: String,
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val local = prefs.getBoolean(KEY, false)
        val cloud = firestoreValue == true
        val resolved = local || cloud
        Log.d(
            MainActivity.FIRST_TUTORIAL_LOG_TAG,
            "resolveShown | source=$logSource local=$local firestore=$firestoreValue resolved=$resolved",
        )
        if (resolved && !local) {
            prefs.edit().putBoolean(KEY, true).apply()
            Log.d(
                MainActivity.FIRST_TUTORIAL_LOG_TAG,
                "resolveShown | promoted local=true from cloud ($logSource)",
            )
        }
        if (local && !cloud) {
            Log.w(
                MainActivity.FIRST_TUTORIAL_LOG_TAG,
                "resolveShown | local=true but firestore=false — will repair cloud ($logSource)",
            )
        }
        return resolved
    }

    /** Tutorial tamamlandı: yerel + Firestore (girişli kullanıcı). */
    fun markShown(context: Context, source: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wasLocal = prefs.getBoolean(KEY, false)
        prefs.edit().putBoolean(KEY, true).apply()
        Log.d(
            MainActivity.FIRST_TUTORIAL_LOG_TAG,
            "markShown | source=$source wasLocal=$wasLocal -> local=true",
        )
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.d(
                MainActivity.FIRST_TUTORIAL_LOG_TAG,
                "markShown | source=$source no auth uid — Firestore skip (guest)",
            )
            return
        }
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(mapOf(KEY to true), SetOptions.merge())
            .addOnSuccessListener {
                Log.d(
                    MainActivity.FIRST_TUTORIAL_LOG_TAG,
                    "markShown | source=$source Firestore OK uid=${uid.take(8)}",
                )
            }
            .addOnFailureListener { e ->
                Log.e(
                    MainActivity.FIRST_TUTORIAL_LOG_TAG,
                    "markShown | source=$source Firestore FAIL uid=${uid.take(8)} err=${e.message}",
                )
            }
    }

    /** Yerel true iken bulutta false kaldıysa birleştir (Splash / Main giriş). */
    fun repairFirestoreIfLocalShown(context: Context, source: String) {
        if (!readLocal(context)) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(mapOf(KEY to true), SetOptions.merge())
            .addOnSuccessListener {
                Log.d(
                    MainActivity.FIRST_TUTORIAL_LOG_TAG,
                    "repairFirestore | source=$source OK uid=${uid.take(8)}",
                )
            }
            .addOnFailureListener { e ->
                Log.e(
                    MainActivity.FIRST_TUTORIAL_LOG_TAG,
                    "repairFirestore | source=$source FAIL err=${e.message}",
                )
            }
    }
}

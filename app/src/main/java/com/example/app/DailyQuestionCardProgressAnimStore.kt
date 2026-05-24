package com.example.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/** Kart progress animasyonunun son gösterildiği [solvedCount] (periyot + kullanıcı). */
object DailyQuestionCardProgressAnimStore {

    private fun prefsKey(uid: String, periodKey: String): String =
        "daily_card_anim_shown_${uid}_$periodKey"

    fun getLastShown(context: Context, periodKey: String): Int? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val prefs = context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getInt(prefsKey(uid, periodKey), -1)
        return stored.takeIf { it >= 0 }
    }

    fun setLastShown(context: Context, periodKey: String, solved: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(prefsKey(uid, periodKey), solved.coerceAtLeast(0))
            .apply()
    }
}

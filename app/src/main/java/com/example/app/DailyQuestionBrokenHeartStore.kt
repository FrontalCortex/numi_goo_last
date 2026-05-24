package com.example.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/** Günlük soru kartındaki kırık kalp Lottie durumu (periyot + kullanıcı). */
object DailyQuestionBrokenHeartStore {

    private fun uid(context: Context): String? =
        FirebaseAuth.getInstance().currentUser?.uid

    private fun playKey(uid: String, periodKey: String) =
        "daily_broken_heart_play_${uid}_$periodKey"

    private fun hold116Key(uid: String, periodKey: String) =
        "daily_broken_heart_hold116_${uid}_$periodKey"

    private fun healPlayKey(uid: String, periodKey: String) =
        "daily_broken_heart_heal_${uid}_$periodKey"

    /** Yanlış cevap sonrası 0→116 animasyonu bir sonraki kart bind'de. */
    fun requestPlayOnNextBind(context: Context, periodKey: String) {
        val u = uid(context) ?: return
        context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(playKey(u, periodKey), true)
            .apply()
    }

    fun consumePlayRequest(context: Context, periodKey: String): Boolean {
        val u = uid(context) ?: return false
        val prefs = context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
        val key = playKey(u, periodKey)
        if (!prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, false).apply()
        return true
    }

    fun setBrokenHold116(context: Context, periodKey: String, hold: Boolean) {
        val u = uid(context) ?: return
        context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(hold116Key(u, periodKey), hold)
            .apply()
    }

    fun isBrokenHold116(context: Context, periodKey: String): Boolean {
        val u = uid(context) ?: return false
        return context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(hold116Key(u, periodKey), false)
    }

    fun clearBrokenHold116(context: Context, periodKey: String) {
        setBrokenHold116(context, periodKey, false)
    }

    /** Elmasla devam sonrası 116→son kare→0 animasyonu. */
    fun requestHealPlay(context: Context, periodKey: String) {
        val u = uid(context) ?: return
        context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(healPlayKey(u, periodKey), true)
            .apply()
    }

    fun consumeHealPlay(context: Context, periodKey: String): Boolean {
        val u = uid(context) ?: return false
        val prefs = context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
        val key = healPlayKey(u, periodKey)
        if (!prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, false).apply()
        return true
    }

    fun resetForPeriod(context: Context, periodKey: String) {
        val u = uid(context) ?: return
        val prefs = context.getSharedPreferences(DailyQuestionPeriod.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(playKey(u, periodKey))
            .remove(hold116Key(u, periodKey))
            .remove(healPlayKey(u, periodKey))
            .apply()
    }
}

package com.example.app

import android.content.Context
import org.json.JSONObject

/**
 * BAŞARI ANINDA PAYLAŞIM (kullanıcı kararı 09.11.2026): seri 7 / 30 / 100 gün ve rozet seviye atlaması.
 *
 * Neden "an kaydı + sonra panel": rozet ve ders sonu ekranları uzun uğraşla oturtulmuş ders sonu
 * zincirinin parçası; içlerine düğme koymak o zincire dokunmak demekti. Burada an yalnızca KAYDEDİLİYOR
 * ([record]); panel ([ShareMomentDialog]) zincir bitip ana ekran boşalınca MainActivity'den açılıyor
 * (haftalık kart ve davet ödülü paneliyle aynı kapı, homeIdleBlockReason).
 *
 * Bıktırmamak için: en çok günde bir panel (Türkiye günü), bekleyen seri anı rozet anından önce gelir,
 * aynı an iki kez gösterilmez. Kayıt cihazda (SharedPreferences): kaçarsa yalnızca bir paylaşım önerisi
 * kaçar, başka bir şey değil.
 */
object ShareMoments {

    private const val PREFS = "share_moments"
    private const val KEY_PENDING = "pending"
    private const val KEY_LAST_SHOWN_DAY = "last_shown_day"
    private const val KEY_SEEN = "seen"

    /** Paylaşım önerilen seri günleri (kullanıcı kararı). */
    val STREAK_DAYS = setOf(7, 30, 100)

    enum class Kind { STREAK, BADGE }

    data class Moment(val kind: Kind, val value: Int, val label: String, val atMs: Long) {
        /** Aynı anın iki kez gösterilmemesi için. */
        val key: String get() = "${kind.name}:$label:$value"

        fun toJson(): String = JSONObject().put("kind", kind.name).put("value", value)
            .put("label", label).put("atMs", atMs).toString()

        companion object {
            fun fromJson(s: String?): Moment? = runCatching {
                val o = JSONObject(s ?: return null)
                Moment(Kind.valueOf(o.getString("kind")), o.getInt("value"), o.optString("label"), o.getLong("atMs"))
            }.getOrNull()
        }
    }

    /** Yan etkisiz: yeni an bekleyenin yerini alır mı? Seri, rozeti ezer; rozet, bekleyen seriyi ezmez. */
    fun replaces(pending: Moment?, incoming: Moment): Boolean =
        pending == null || incoming.kind == Kind.STREAK || pending.kind == Kind.BADGE

    /** Yan etkisiz: bugün panel gösterilebilir mi (günde en çok bir). */
    fun canShowToday(lastShownDay: String?, today: String): Boolean = lastShownDay != today

    fun recordStreak(context: Context, streak: Int) {
        if (streak !in STREAK_DAYS) return
        record(context, Moment(Kind.STREAK, streak, "", System.currentTimeMillis()))
    }

    fun recordBadge(context: Context, title: String, level: Int) {
        if (title.isBlank()) return
        record(context, Moment(Kind.BADGE, level, title, System.currentTimeMillis()))
    }

    private fun record(context: Context, m: Moment) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getStringSet(KEY_SEEN, emptySet())!!.contains(m.key)) {
            android.util.Log.d(ShareMomentDialog.TAG, "kayıt atlandı (zaten gösterildi) an=${m.key}")
            return
        }
        val pending = Moment.fromJson(p.getString(KEY_PENDING, null))
        if (!replaces(pending, m)) {
            android.util.Log.d(ShareMomentDialog.TAG, "kayıt atlandı (bekleyen ${pending?.key} öncelikli) an=${m.key}")
            return
        }
        android.util.Log.d(ShareMomentDialog.TAG, "kaydedildi an=${m.key}")
        p.edit().putString(KEY_PENDING, m.toJson()).apply()
        // Ana ekrana dönülünce (ders sonu zinciri bitince) panel denensin.
        MainActivity.currentActivity?.requestShareMomentCheck()
    }

    /** Gösterilecek an varsa ve bugün henüz panel çıkmadıysa. */
    fun pendingToShow(context: Context): Moment? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val m = Moment.fromJson(p.getString(KEY_PENDING, null)) ?: return null
        if (!canShowToday(p.getString(KEY_LAST_SHOWN_DAY, null), WeeklyCardRules.trDay(System.currentTimeMillis()))) return null
        return m
    }

    fun markShown(context: Context, m: Moment) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = p.getStringSet(KEY_SEEN, emptySet())!!.toMutableSet().apply { add(m.key) }
        p.edit()
            .remove(KEY_PENDING)
            .putString(KEY_LAST_SHOWN_DAY, WeeklyCardRules.trDay(System.currentTimeMillis()))
            .putStringSet(KEY_SEEN, seen.toList().takeLast(200).toSet())
            .apply()
    }
}

// Kaynak (Android): app/src/main/java/com/example/app/RecordFragment.kt ve RecordLeaderboardAdapter.kt —
// görünümden bağımsız kısım: sezon geri sayım metni, sezon etiketi, birinci/diğerleri ayrımı, hata metni seçimi,
// satır rozet arka planı ve "bu satır bende mi" kararı.
// R.string.x / R.drawable.x / R.color.x -> kaynak adı String'i (metinlerin Türkçe karşılığı sabit olarak eklendi).
package com.sorobit.shared.kupa.sezon

import com.sorobit.shared.kupa.kupaNowMs

object RecordLogic {

    // strings.xml karşılıkları (app/src/main/res/values/strings.xml)
    const val STRING_RECORD_LEADERBOARD_TITLE = "record_leaderboard_title"
    const val STRING_RECORD_LEADERBOARD_LOAD_ERROR = "record_leaderboard_load_error"
    const val STRING_RECORD_SEASON_COUNTDOWN_DAYS_HOURS = "record_season_countdown_days_hours"
    const val STRING_RECORD_SEASON_COUNTDOWN_HOURS_MINUTES = "record_season_countdown_hours_minutes"

    const val TEXT_RECORD_LEADERBOARD_TITLE = "Liderlik tablosu"
    const val TEXT_RECORD_LEADERBOARD_LOAD_ERROR = "Liste yüklenemedi."

    /** Geri sayım metninin yenilenme aralığı (Android: `postDelayed(this, 1000L)`). */
    const val COUNTDOWN_TICK_MS = 1000L

    /** [formatSeasonCountdown] sonucu: kaynak anahtarı + iki sayı + Türkçe hazır metin. */
    data class SeasonCountdown(
        /** [STRING_RECORD_SEASON_COUNTDOWN_DAYS_HOURS] ya da [STRING_RECORD_SEASON_COUNTDOWN_HOURS_MINUTES]. */
        val stringKey: String,
        val arg1: Int,
        val arg2: Int,
        /** "%1$d gün %2$d sa" / "%1$d sa %2$d dk" biçimiyle üretilmiş metin. */
        val text: String,
    )

    /**
     * Android `formatSeasonCountdown`: 1 günden fazla kaldıysa "G gün S sa", aksi halde "S sa D dk"
     * (saniyeler atılır, negatif süre 0 sayılır).
     */
    fun formatSeasonCountdown(remainingMs: Long): SeasonCountdown {
        val totalMinutes = (remainingMs / 60_000L).toInt().coerceAtLeast(0)
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        return if (days > 0) {
            SeasonCountdown(STRING_RECORD_SEASON_COUNTDOWN_DAYS_HOURS, days, hours, "$days gün $hours sa")
        } else {
            SeasonCountdown(STRING_RECORD_SEASON_COUNTDOWN_HOURS_MINUTES, hours, minutes, "$hours sa $minutes dk")
        }
    }

    /**
     * Android `updateRecordSeasonIndexLabel`: bağlı sezon >= 1 ise o, değilse güncel sezon; "s14" biçiminde.
     */
    fun seasonIndexLabel(boundLeaderboardSeason: Int, nowUtcMs: Long = kupaNowMs()): String {
        val s = if (boundLeaderboardSeason >= 1) boundLeaderboardSeason else SeasonClock.currentSeason(nowUtcMs)
        return "s$s"
    }

    /** Android `onSeasonAdvanced`: yeni sezon bağlı olandan farklıysa dinleyici yeniden bağlanır. */
    fun shouldRebindLeaderboard(newSeason: Int, boundLeaderboardSeason: Int): Boolean =
        newSeason != boundLeaderboardSeason

    /**
     * Android `bindLeaderboard` ayrımı: liste boşsa boş durum; değilse ilk kayıt üstte ayrı gösterilir,
     * kalanı listede.
     */
    data class LeaderboardSections(
        val isEmpty: Boolean,
        val first: LessonLeaderboardRepository.LeaderboardEntry?,
        val rest: List<LessonLeaderboardRepository.LeaderboardEntry>,
    ) {
        /** "Sıralama" başlığı ve liste yalnızca birinciden başka kayıt varsa görünür. */
        val showRestList: Boolean get() = rest.isNotEmpty()
    }

    fun splitLeaderboard(entries: List<LessonLeaderboardRepository.LeaderboardEntry>): LeaderboardSections {
        if (entries.isEmpty()) return LeaderboardSections(isEmpty = true, first = null, rest = emptyList())
        return LeaderboardSections(isEmpty = false, first = entries.first(), rest = entries.drop(1))
    }

    /** Android: `displayName.ifBlank { getString(R.string.record_leaderboard_title) }`. */
    fun displayNameOrFallback(displayName: String, fallback: String = TEXT_RECORD_LEADERBOARD_TITLE): String =
        displayName.ifBlank { fallback }

    /** Avatar: `!url.isNullOrBlank()` ise resim yüklenir, değilse varsayılan ikon. */
    fun hasPhoto(photoUrl: String?): Boolean = !photoUrl.isNullOrBlank()

    /**
     * Android `listenLeaderboard.onError` metni: yetki hatasında (PERMISSION_DENIED) her zaman genel hata metni;
     * diğer hatalarda yerelleştirilmiş hata iletisi, o da yoksa genel hata metni.
     */
    fun leaderboardErrorMessage(
        localizedMessage: String?,
        isPermissionDenied: Boolean,
        loadErrorText: String = TEXT_RECORD_LEADERBOARD_LOAD_ERROR,
    ): String {
        val message = localizedMessage ?: loadErrorText
        return if (isPermissionDenied) loadErrorText else message
    }

    /** Android `RecordLeaderboardAdapter.VH.badgeDrawableForRank` (ödül sırasına göre; R.drawable adı). */
    fun badgeDrawableForRank(rank: Int): String = when (rank) {
        1 -> "bg_record_rank_gold"
        2 -> "bg_record_rank_badge_purple"
        3 -> "bg_record_rank_badge_yellow"
        4 -> "bg_record_rank_badge_blue"
        else -> "bg_record_rank_badge_mint"
    }

    /** Android `RecordLeaderboardAdapter.VH.bind`: satır oturum açmış kullanıcıya mı ait. */
    fun isCurrentUser(entryUserId: String, currentUid: String?): Boolean =
        !currentUid.isNullOrBlank() && entryUserId == currentUid

    /** Satır renkleri (R.color adları): kart, ad, süre. Kendi satırı beyaz kart üzerinde siyah/gri yazıdır. */
    data class RowColors(val cardColor: String, val nameColor: String, val timeColor: String)

    fun rowColors(isCurrentUser: Boolean): RowColors =
        if (isCurrentUser) {
            RowColors(cardColor = "white", nameColor = "black", timeColor = "gray")
        } else {
            RowColors(cardColor = "panel_background", nameColor = "white", timeColor = "button_disabled")
        }
}

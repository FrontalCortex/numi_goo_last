// Kaynak (Android):
//  - app/src/main/java/com/example/app/StreakRepository.kt (REMINDER_HOUR_OPTIONS, DEFAULT_REMINDER_HOUR,
//    GOAL_OPTIONS, CHALLENGE_OPTIONS, FREEZE_COST_GOLD, FREEZES_UNKNOWN, StreakState, FreezeNotice)
//  - app/src/main/java/com/example/app/StreakViews.kt (hourText, reminderHourLabel)
//
// Serinin sabitleri ve ekranda gösterilen durum. Aynı değerlerin bir kısmı kayıt akışı için
// `model/SignupFlowRules` ve `model/SignupStreakRules` içinde de var (onların davranışı değiştirilmedi).
package com.sorobit.shared.seri

object StreakRules {
    /** Hatırlatma için hazır sunulan saatler (yerel, tam saat). */
    val REMINDER_HOUR_OPTIONS: List<Int> = listOf(16, 18, 19, 20)

    /** Seçim yapılmadıysa; sunucunun eski sabit saati (`STREAK_REMINDER_LOCAL_HOUR`) ile aynı. */
    const val DEFAULT_REMINDER_HOUR = 19

    /** Günlük hedefler (dakika). */
    val GOAL_OPTIONS: List<Int> = listOf(5, 10, 20)

    /** "Kaç gün üst üste" hedefleri. */
    val CHALLENGE_OPTIONS: List<Int> = listOf(3, 5, 7)

    /** Hedef hiç seçilmediyse. */
    const val DEFAULT_GOAL_MINUTES = 5
    const val DEFAULT_CHALLENGE_DAYS = 3

    /** Hafta şeridi için saklanan gün sayısı. */
    const val ACHIEVED_HISTORY_DAYS = 21

    /** Eşitleme kuyruğunda en fazla kaç gün beklesin; sunucu da bundan fazlasını almıyor. */
    const val MAX_PENDING_SYNC_DAYS = 7

    /** Seri dondurma için son günün kaç gün geriye kadar arandığı. */
    const val FREEZE_SCAN_DAYS = 30

    /**
     * Seri dondurmanın altın bedeli. YALNIZCA gösterim: düşülen miktarı sunucu belirler
     * (functions/index.js → STREAK_FREEZE_COST = 4000). iOS mağazasındaki `MagazaStil.dondurmaBedeli`
     * ile aynı değer.
     */
    const val FREEZE_COST_GOLD = 4000

    /** Elde en fazla kaç dondurma olabilir (sunucu: STREAK_FREEZE_MAX_HELD). */
    const val FREEZE_MAX_HELD = 1

    /** Sunucu dondurma bilgisini döndürmedi (0 ile aynı şey değil). */
    const val FREEZES_UNKNOWN = -1

    /** Kayıt cevaplarının uygulanacağı "yeni hesap" penceresi (10 dk). */
    const val NEW_ACCOUNT_WINDOW_MS = 10 * 60 * 1000L

    /** Bundan eski bekleyen cevaplar kullanılmıyor (24 saat). */
    const val PENDING_SIGNUP_MAX_AGE_MS = 24 * 60 * 60 * 1000L

    /** Saat ızgarasının ilk saati (sabah). */
    const val HOUR_GRID_START = 6

    /** Hedef seçeneklerinin sol metinleri (Android: StreakFragment.showGoalPicker). */
    val GOAL_LABEL_KEYS: List<String> = listOf("ui_rahat", "ui_duzenli", "ui_ciddi")

    /** "19:00" biçiminde saat. */
    fun hourText(hour: Int): String = hour.toString().padStart(2, '0') + ":00"

    /** 24 saatlik ızgara, ekrandaki sırayla (06:00 … 05:00). */
    fun hourGrid(): List<Int> = (0 until 24).map { (HOUR_GRID_START + it) % 24 }

    /**
     * Hatırlatma saatinin günlük hayattaki karşılığının metin anahtarı; anahtarı olmayanlar için null.
     * Android'de 16 ("Okuldan sonra") ve 20 ("Yemekten sonra") koda gömülü Türkçe metindir:
     * [reminderHourHardcodedLabel] onları verir.
     */
    fun reminderHourLabelKey(hour: Int): String? = when (hour) {
        18 -> "ui_aksamustu"
        19 -> "ui_aksam"
        else -> null
    }

    /** Android'de koda gömülü (strings'te olmayan) etiketler; yoksa null. */
    fun reminderHourHardcodedLabel(hour: Int): String? = when (hour) {
        16 -> "Okuldan sonra"
        20 -> "Yemekten sonra"
        else -> null
    }

    fun isPresetReminderHour(hour: Int): Boolean = hour in REMINDER_HOUR_OPTIONS
}

/** Seri ekranında gösterilecek durum (Android: StreakRepository.StreakState). */
data class StreakState(
    /** Güncel seri; kırıldıysa 0. */
    val current: Int,
    val longest: Int,
    val goalMinutes: Int,
    val secondsToday: Int,
    /** Hedefi tutturulmuş günler (`yyyy-MM-dd`), hafta şeridi için. */
    val achievedDays: Set<String>,
    /** Seri dondurmanın kapattığı günler. */
    val frozenDays: Set<String>,
    /** Elde harcanmamış seri dondurma var mı. */
    val freezeHeld: Boolean,
) {
    val goalSeconds: Int get() = goalMinutes * 60
    val goalReachedToday: Boolean get() = secondsToday >= goalSeconds

    /** Bugünkü ilerleme, 0f–1f. */
    val todayFraction: Float
        get() = if (goalSeconds <= 0) 1f
        else (secondsToday.toFloat() / goalSeconds).coerceIn(0f, 1f)

    /** Hedefe kalan dakika, en az 1. */
    val minutesLeft: Int
        get() = ((goalSeconds - secondsToday + 59) / 60).coerceAtLeast(1)
}

/** Dondurma harcandığında kullanıcıya bir kez söylenecek şey (Android: StreakRepository.FreezeNotice). */
enum class StreakFreezeNotice {
    /** Kaçan gün kapandı, seri yaşıyor. */
    SAVED,

    /** Kaçan ilk gün kapandı ama arkasından bir gün daha kaçtı; seri yine kırıldı. */
    LOST,
}

/** Kayıt sorularının bekleyen seri cevapları (Android: "streak_signup_pending" prefs dosyası). */
data class StreakPendingSignup(
    val goalMinutes: Int,
    val challengeDays: Int,
    /** -1 = cevap yok. */
    val reminderHour: Int,
    /** 0 = bekleyen cevap yok. */
    val savedAtMs: Long,
    /** markAccountCreated işaretinin hesabı; yoksa null. */
    val createdUid: String?,
    /** markAccountCreated işaretinin zamanı; yoksa 0. */
    val createdAtMs: Long,
)

/** [StreakRepository.applyPendingSignup] sonucu. */
enum class StreakSignupResult {
    /** Bekleyen cevap yoktu. */
    NO_PENDING,

    /** Hesap yeni: cevaplar seriye uygulandı. */
    APPLIED,

    /** Cevaplar 24 saatten eski: atıldı. */
    DISCARDED_STALE,

    /** Mevcut bir hesaba girildi: cevaplar atıldı (o hesabın kendi ayarları geçerli). */
    DISCARDED_EXISTING_ACCOUNT,
}

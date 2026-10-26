package com.example.app.homework

/**
 * ÖDEV BAŞARISI GEÇMİŞİ — başarı grafiğinin ve "Bu hafta" kartının hesabı. Saf Kotlin (Android'e
 * bağımlı değil): RuleProgressTest.
 *
 * Veri sunucudan (functions/index.js onHomeworkProgressWritten): homeworkRuleStats/{uid}/months/{yyyy-mm}
 * içinde her gün için soru sayımı (`q`) ve kural sayımı (`t`). Günler Türkiye saatiyle.
 *
 * NEDEN DÖNEMLİK ORAN
 *   Grafikteki her nokta o dönemin (gün/hafta/ay) doğru oranı; toplam oran yüzlerce sorudan sonra
 *   düzleşip son haftanın ilerlemesini göstermiyordu. İlerlemeyi biriken sayı da gösteriyor: her
 *   noktanın üstünde o ana kadar çözülen TOPLAM soru (kullanıcının isteği 26.10.2026).
 *
 * BAŞLANGIÇ
 *   Günlük kayıt 26.10.2026'da başladı; öncesinde yalnızca toplam vardı. O toplam grafikte tek bir
 *   "Başlangıç" noktası: sorularda `base.q` (functions/scripts/backfill-homework-stats-base.js),
 *   kurallarda toplam − günlük kayıtlar.
 */
object RuleProgress {

    /** Ustalık: son ~50 işlem; aynı eşikler sunucuda (functions/homework.js masteryStage). */
    const val MASTERY_WINDOW = 50
    private const val IMPROVING_PCT = 70
    const val MASTER_PCT = 90

    /** Bundan az soruyla dönemin oranı güvenilmez; grafikte soluk çiziliyor. */
    const val LOW_SAMPLE = 5

    private const val DAY_MS = 86_400_000L
    /** Türkiye UTC+3 (2016'dan beri yaz saati yok). */
    private const val TR_OFFSET_MS = 3 * 3_600_000L

    data class Cw(val c: Int, val w: Int) {
        val total: Int get() = c + w
        val pct: Int get() = if (total == 0) 0 else Math.round(c * 100f / total)
        operator fun plus(o: Cw) = Cw(c + o.c, w + o.w)
        operator fun minus(o: Cw) = Cw((c - o.c).coerceAtLeast(0), (w - o.w).coerceAtLeast(0))

        companion object {
            val ZERO = Cw(0, 0)
        }
    }

    /** Bir günün kaydı: soru sayımı ve kural anahtarı → işlem sayımı. */
    data class Day(val q: Cw, val t: Map<String, Cw>)

    enum class Range { WEEK, MONTH, QUARTER, ALL }

    enum class BucketKind { DAY, THREE_DAYS, WEEK, MONTH }

    /** Grafikte bir dönem: [from]..[to] epoch günleri; [cumulative] o dönemin sonuna kadarki toplam. */
    data class Bucket(val from: Int, val to: Int, val kind: BucketKind, val cw: Cw, val cumulative: Int)

    /** 0 başladı, 1 gelişiyor, 2 usta (50 işlemden azsa henüz başladı). */
    fun stage(cw: Cw): Int = when {
        cw.total < MASTERY_WINDOW -> 0
        cw.pct >= MASTER_PCT -> 2
        cw.pct >= IMPROVING_PCT -> 1
        else -> 0
    }

    fun todayTr(nowMs: Long): Int = Math.floorDiv(nowMs + TR_OFFSET_MS, DAY_MS).toInt()

    /** [key] null: soru sayımı ("Hepsi"); değilse o kuralın işlem sayımı. */
    fun valueOf(day: Day?, key: String?): Cw = when {
        day == null -> Cw.ZERO
        key == null -> day.q
        else -> day.t[key] ?: Cw.ZERO
    }

    fun sum(days: Map<Int, Day>, key: String?, from: Int, to: Int): Cw =
        days.entries.fold(Cw.ZERO) { acc, (d, day) -> if (d in from..to) acc + valueOf(day, key) else acc }

    /**
     * Günlük kayıttan önceki çalışma (Başlangıç noktası). Sorularda aktarım betiğinin yazdığı
     * [baseQ]; kurallarda toplamdan günlük kayıtlar çıkarılınca kalan.
     */
    fun start(key: String?, baseQ: Cw?, tally: Map<String, Cw>, days: Map<Int, Day>): Cw? {
        val cw = if (key == null) baseQ else tally[key]?.let { it - sum(days, key, Int.MIN_VALUE, Int.MAX_VALUE) }
        return cw?.takeIf { it.total > 0 }
    }

    /**
     * Grafiğin dönemleri, hepsi bugünle bitiyor: 1 hafta günlük (7 nokta), 1 ay 3 günlük (10 nokta),
     * 3 ay haftalık (13 nokta), Tümü aylık (ilk kayıttan bu aya). 1 ay önce günlüktü; 30 nokta
     * kalabalıktı (kullanıcı kararı 26.10.2026). Dönemin oranı ortalamaların ortalaması değil,
     * dönemdeki bütün soruların doğru oranı: 2 soruluk bir gün 40 soruluk günle eş ağırlık almasın.
     * Boş dönem atlanıyor (çizgi bir sonraki dolu döneme gidiyor).
     *
     * @param startTotal Başlangıç noktasının toplamı; [Bucket.cumulative] bunun üstüne birikiyor.
     */
    fun buckets(days: Map<Int, Day>, key: String?, range: Range, today: Int, startTotal: Int): List<Bucket> {
        val (kind, spans) = when (range) {
            Range.WEEK -> BucketKind.DAY to (today - 6..today).map { it to it }
            Range.MONTH -> BucketKind.THREE_DAYS to (0 until 10).map { i -> (today - 29 + 3 * i).let { it to it + 2 } }
            Range.QUARTER -> BucketKind.WEEK to (0 until 13).map { i -> (today - 90 + 7 * i).let { it to it + 6 } }
            Range.ALL -> BucketKind.MONTH to monthSpans(days.keys.minOrNull()?.coerceAtMost(today) ?: today, today)
        }
        val rangeStart = spans.first().first
        var cumulative = startTotal + sum(days, key, Int.MIN_VALUE, rangeStart - 1).total
        return spans.mapNotNull { (from, to) ->
            val cw = sum(days, key, from, to)
            if (cw.total == 0) return@mapNotNull null
            cumulative += cw.total
            Bucket(from, to.coerceAtMost(today), kind, cw, cumulative)
        }
    }

    private fun monthSpans(first: Int, today: Int): List<Pair<Int, Int>> {
        val (y0, m0) = civil(first)
        val (y1, m1) = civil(today)
        val out = mutableListOf<Pair<Int, Int>>()
        var y = y0
        var m = m0
        while (y < y1 || (y == y1 && m <= m1)) {
            val from = epochDay(y, m, 1)
            val (ny, nm) = if (m == 12) y + 1 to 1 else y to m + 1
            out += from to epochDay(ny, nm, 1) - 1
            y = ny
            m = nm
        }
        return out
    }

    // ── Takvim (java.time API 26 istiyor; minSdk 24) ────────────────────────

    /** Gregoryen tarih → 1970-01-01'den gün sayısı (Howard Hinnant, days_from_civil). */
    fun epochDay(y: Int, m: Int, d: Int): Int {
        val yy = if (m <= 2) y - 1 else y
        val era = Math.floorDiv(yy, 400)
        val yoe = yy - era * 400
        val mp = (m + 9) % 12
        val doy = (153 * mp + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    /** [epochDay]'in tersi: yıl, ay (1–12), gün. */
    fun civil(epochDay: Int): Triple<Int, Int, Int> {
        val z = epochDay + 719468
        val era = Math.floorDiv(z, 146097)
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        val y = yoe + era * 400 + if (m <= 2) 1 else 0
        return Triple(y, m, d)
    }
}

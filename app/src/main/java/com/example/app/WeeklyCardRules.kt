package com.example.app

import kotlin.math.abs

/**
 * HAFTALIK İLERLEME KARTININ KARARLARI (yan etkisiz; birim testi Firebase'siz çalışsın diye ayrı).
 *
 * Kart "geçen hafta → bu hafta" diyor. Ortak dil (kullanıcı kararı 08.11.2026):
 * **seviye = basamak × sayı adedi × hız**; yüzde ve oran YOK, somut önce/sonra var
 * ("1 basamaklı 4–6 sayı → 2 basamaklı 4–6 sayı", "1:20 → 0:52").
 *
 * Veri: sunucunun haftalık kaydı (`users/{uid}/weeklyStats`, functions/weeklyStats.js) iki hafta
 * arka arkaya (kupa puanı ve Sobi seviyesi yalnızca BUGÜNKÜ değerle tutulduğundan kıyas bu iki
 * fotoğraftan) + ders kilometre taşları ([LessonMilestones]; bitiş tarihi, örnek işlem, sandık süresi).
 *
 * ## Neden yalnızca yükselen
 * Kart paylaşılmak için var; kötü geçen hafta paylaşılmaz (döngü 2, "utandırma" riski). Düşen kupa,
 * düşen seviye hiç bir yerde gösterilmiyor; hiç ilerleme yoksa yalnızca emek satırı ve Sobi'nin önerisi.
 *
 * ## Ana satır seçimi (kullanıcı kararı: sabit öncelik + eşik)
 * körleme ilerlemesi → kupa/Sobi seviye atlaması → sandık süresi → yeni beceri. Körleme önde, çünkü
 * soroban eğitiminin asıl hedefi abaküse bakmadan hesap; veliye en çok şey söyleyen o.
 */
object WeeklyCardRules {

    // ── Eşikler (kullanıcı kararı 08.11.2026, "önerilen eşikler") ─────────────

    /** Sandık süresi: en az bu kadar saniye ... */
    const val CHEST_MIN_GAIN_SEC = 3
    /** ... ve ilk süreye göre en az bu kadar yüzde hızlanma (kartta yüzde YAZILMAZ; yalnızca eşik). */
    const val CHEST_MIN_GAIN_PCT = 10
    /** Sobi testi: en az bu kadar seviye. */
    const val SOBI_MIN_LEVELS = 1
    /** Kupa yolu hiç oynanmamışsa başlangıç puanı (sunucu kaydı eksik yolu yazmıyor; bkz. weeklyStats.js). */
    const val CUP_START_SCORE = 200

    /** Kupa alanı → kart sırası (kupa yolu ekranındaki kart sırası; kart başlıkları ona göre). */
    val CUP_FIELDS = listOf(
        "addition_abacus_cup",
        "extraction_abacus_cup",
        "impact_abacus_cup",
        "blinding_addition_abacus_cup",
        "blinding_extraction_abacus_cup",
        "blinding_impact_abacus_cup",
    )

    fun isBlindingCup(field: String) = field.startsWith("blinding_")
    fun isMultiplicationCup(field: String) = field.contains("impact")

    /** Sobi testindeki zihinden kural anahtarının ön eki (SobiTestPlanner.MENTAL_PREFIX ile aynı). */
    const val MENTAL_PREFIX = "m_"

    // ── Girdiler ─────────────────────────────────────────────────────────────

    /** Sunucunun haftalık kaydından kartın kullandığı kısım. */
    data class Snapshot(
        val week: String,
        /** `yyyy-MM-dd`, Pazartesi (Türkiye saati). */
        val weekStart: String,
        /** `yyyy-MM-dd`, Pazar. */
        val weekEnd: String,
        val cups: Map<String, Int> = emptyMap(),
        val sobiLevels: Map<String, Int> = emptyMap(),
        val streakCurrent: Int = 0,
        /** `yyyy-MM-dd` → saniye; yalnızca çalışılan günler. */
        val dailySeconds: Map<String, Int> = emptyMap(),
        val weekSeconds: Int = 0,
    )

    /** `lessonMilestones` belgesi. */
    data class Milestone(
        val partId: Int,
        val stableId: String,
        val finishedAtMs: Long? = null,
        /** "48 + 37 = 85" (LessonMilestoneRules.SampleOp.text). */
        val sampleOp: String? = null,
        val chestFirstSec: Int? = null,
        val chestBestSec: Int? = null,
        val chestBestAtMs: Long? = null,
    )

    // ── Kupa puanının okunuşu ────────────────────────────────────────────────

    /**
     * Bir kupa puanının açtığı kural, karta yazılacak biçimde. Toplama/çıkarmada aralığın EN YÜKSEK
     * basamaklı kuralı (aralıkta birkaç basamak varsa çocuğun ulaştığı en zor şey odur), sayı adedinin
     * en azı–en çoğu ve en yavaş aralık (zorluk 0'ın aralığı; "2,9 sn arayla" kural tablosundaki ilk değer).
     * Çarpmada iki sayının basamağı.
     */
    data class CupLevel(
        val digits: Int,
        val countMin: Int = 0,
        val countMax: Int = 0,
        val intervalMs: Long = 0L,
        /** Çarpmada 2. sayının basamağı; toplama/çıkarmada null. */
        val mulSecondDigits: Int? = null,
    )

    fun cupLevel(field: String, score: Int): CupLevel {
        val blinding = isBlindingCup(field)
        if (isMultiplicationCup(field)) {
            val (a, b) = CupRuleEngine.multiplicationDigitsFor(score, blinding)
            return CupLevel(digits = a, mulSecondDigits = b)
        }
        val top = CupRuleEngine.configsFor(score, blinding).maxByOrNull { it.digits }
            ?: return CupLevel(digits = 1)
        return CupLevel(
            digits = top.digits,
            countMin = top.counts.minOrNull() ?: 0,
            countMax = top.counts.maxOrNull() ?: 0,
            intervalMs = top.intervals.firstOrNull() ?: 0L,
        )
    }

    // ── Çıktı ────────────────────────────────────────────────────────────────

    /** Kartın bir ilerleme satırı. [tier] ana satır önceliği (küçük önce), [magnitude] aynı katmanda büyüklük. */
    sealed class Item {
        abstract val tier: Int
        abstract val magnitude: Double

        data class CupStep(
            val field: String,
            val beforeScore: Int,
            val afterScore: Int,
            val before: CupLevel,
            val after: CupLevel,
        ) : Item() {
            override val tier get() = if (isBlindingCup(field)) 0 else 1
            /** 100 puanlık aralık sayısı: kural tablosu 100'er puanla değişiyor. */
            override val magnitude get() = (afterScore - beforeScore) / 100.0
        }

        data class SobiStep(val key: String, val before: Int, val after: Int) : Item() {
            override val tier get() = if (key.startsWith(MENTAL_PREFIX)) 0 else 1
            override val magnitude get() = (after - before).toDouble()
        }

        data class ChestStep(val partId: Int, val stableId: String, val firstSec: Int, val bestSec: Int) : Item() {
            override val tier get() = 2
            override val magnitude get() = (firstSec - bestSec).toDouble() / firstSec
        }

        data class Learned(val partId: Int, val stableId: String, val sampleOp: String?, val finishedAtMs: Long) : Item() {
            override val tier get() = 3
            override val magnitude get() = finishedAtMs.toDouble()
            /** Part 4–6 körleme: örnek işlem "abaküse bakmadan" çözüldü. */
            val blinding get() = partId in 4..6
        }
    }

    data class Effort(val days: Int, val minutes: Int, val streak: Int)

    data class Card(
        val week: Snapshot,
        /** Kıyaslanan önceki kayıt; ilk haftada null (yalnızca ders/sandık/emek). */
        val previous: Snapshot?,
        val headline: Item?,
        /** Özetteki 2–3 küçük satır (ana satır hariç, her türden en iyisi). */
        val extras: List<Item>,
        val cups: List<Item.CupStep>,
        val sobi: List<Item.SobiStep>,
        val chests: List<Item.ChestStep>,
        /** Yeniden eskiye. */
        val learned: List<Item.Learned>,
        val effort: Effort,
    ) {
        val hasProgress: Boolean get() = headline != null
    }

    /** Türkiye saati: hafta sınırı Pazartesi 00:00 TR (UTC+3, yaz saati yok; sunucuyla aynı varsayım). */
    private const val TR_OFFSET_MS = 3 * 3600 * 1000L
    private const val DAY_MS = 86_400_000L

    /** `yyyy-MM-dd` gününün Türkiye saatiyle başladığı an. */
    fun trDayStartMs(day: String): Long? {
        val p = day.split('-').mapNotNull { it.toIntOrNull() }
        if (p.size != 3) return null
        return java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).run {
            clear()
            set(p[0], p[1] - 1, p[2])
            timeInMillis - TR_OFFSET_MS
        }
    }

    /** Türkiye günü `yyyy-MM-dd`. */
    fun trDay(ms: Long): String {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        f.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return f.format(java.util.Date(ms + TR_OFFSET_MS))
    }

    /**
     * Kartın haftası: haftası KAPANMIŞ (Pazarı bugünden önce) en yeni kayıt (kullanıcı kararı: kapanan
     * hafta). Sunucu Pazartesi 00:10'da kapanan haftayı yazıyor; hafta içinde elle koşu da o haftayı
     * yazabildiği için bitmemiş hafta ("bu hafta şu ana kadar") burada eleniyor.
     * Önceki: ondan eski en yeni kayıt — bir hafta ara verildiyse yine de kıyas olsun diye ardışık
     * olması şart değil, ama [MAX_GAP_WEEKS] haftadan eskisi "geçen hafta" sayılmaz.
     */
    fun pickWeeks(all: List<Snapshot>, todayTr: String): Pair<Snapshot, Snapshot?>? {
        val sorted = all.sortedByDescending { it.weekStart }
        val target = sorted.firstOrNull { it.weekEnd < todayTr } ?: return null
        val targetStart = trDayStartMs(target.weekStart) ?: return target to null
        val prev = sorted.firstOrNull { it.weekStart < target.weekStart }?.takeIf { p ->
            val ps = trDayStartMs(p.weekStart) ?: return@takeIf false
            targetStart - ps <= MAX_GAP_WEEKS * 7 * DAY_MS
        }
        return target to prev
    }

    const val MAX_GAP_WEEKS = 3

    fun build(week: Snapshot, previous: Snapshot?, milestones: List<Milestone>): Card {
        val startMs = trDayStartMs(week.weekStart) ?: 0L
        val endMs = startMs + 7 * DAY_MS
        fun inWeek(ms: Long?) = ms != null && ms >= startMs && ms < endMs

        val cups = if (previous == null) emptyList() else CUP_FIELDS.mapNotNull { f ->
            val after = week.cups[f] ?: return@mapNotNull null
            val before = previous.cups[f] ?: CUP_START_SCORE
            if (after <= before) return@mapNotNull null
            val lb = cupLevel(f, before)
            val la = cupLevel(f, after)
            // Eşik: görünen kural değişmeli (puan artışı tek başına "ilerleme" sayılmıyor; aynı aralıkta
            // 30 puan, veliye anlatılacak bir şey değil).
            if (la == lb) null else Item.CupStep(f, before, after, lb, la)
        }.sortedByDescending { it.magnitude }

        val sobi = if (previous == null) emptyList() else week.sobiLevels.mapNotNull { (k, after) ->
            // Önceki haftada hiç seviyesi olmayan kural "atlama" değil, ilk test (başlangıç seviyesi).
            val before = previous.sobiLevels[k] ?: return@mapNotNull null
            if (after - before >= SOBI_MIN_LEVELS) Item.SobiStep(k, before, after) else null
        }.sortedByDescending { it.magnitude }

        val chests = milestones.mapNotNull { m ->
            val first = m.chestFirstSec ?: return@mapNotNull null
            val best = m.chestBestSec ?: return@mapNotNull null
            if (!inWeek(m.chestBestAtMs) || first <= 0) return@mapNotNull null
            val gain = first - best
            if (gain >= CHEST_MIN_GAIN_SEC && gain * 100 >= CHEST_MIN_GAIN_PCT * first) {
                Item.ChestStep(m.partId, m.stableId, first, best)
            } else null
        }.sortedByDescending { it.magnitude }

        val learned = milestones.mapNotNull { m ->
            val at = m.finishedAtMs?.takeIf { inWeek(it) } ?: return@mapNotNull null
            Item.Learned(m.partId, m.stableId, m.sampleOp?.takeIf { it.isNotBlank() }, at)
        }.sortedByDescending { it.finishedAtMs }

        // Aynı katmanda kupa Sobi'den önce (eşitlikte): kupa yolu çocuğun her gün gördüğü yer.
        val all: List<Item> = cups + sobi + chests + learned.take(1)
        val headline = all.sortedWith(compareBy<Item> { it.tier }.thenByDescending { it.magnitude }).firstOrNull()

        val extras = listOfNotNull<Item>(cups.firstOrNull(), chests.firstOrNull(), learned.firstOrNull(), sobi.firstOrNull())
            .filter { it != headline }
            .take(3)

        val effort = Effort(
            days = week.dailySeconds.count { it.value > 0 },
            minutes = Math.round(week.weekSeconds / 60f),
            streak = week.streakCurrent,
        )
        return Card(week, previous, headline, extras, cups, sobi, chests, learned, effort)
    }

    // ── Sobi'nin önerisi ─────────────────────────────────────────────────────

    /** Kural ustalığında öneri sırası: kurs sırası (önce toplama, sonra çıkarma). */
    val SUGGESTION_ORDER = listOf(
        "add_direct", "add_five", "add_ten", "add_bead",
        "sub_direct", "sub_five", "sub_ten", "sub_bead",
    )

    /**
     * "Sırada X var". [stages]: çalışılmış kural → aşama (0 başladı, 1 gelişiyor, 2 usta; RuleProgress).
     * Önce yarım kalan (çalışılmış ama usta olunmamış) ilk kural: yeni bir şeye geçmeden onu bitirmek.
     * Hepsi ustaysa, ustalaşılan en ileri kuraldan sonraki ilk kural. Hiç veri yoksa null (genel öneri).
     */
    fun suggestNext(stages: Map<String, Int>): String? {
        val practiced = SUGGESTION_ORDER.filter { it in stages }
        if (practiced.isEmpty()) return null
        practiced.firstOrNull { (stages[it] ?: 0) < 2 }?.let { return it }
        val lastMastered = SUGGESTION_ORDER.indexOfLast { stages[it] == 2 }
        return SUGGESTION_ORDER.getOrNull(lastMastered + 1)
    }

    // ── Biçim yardımcıları (dilden bağımsız) ─────────────────────────────────

    /** 80 → "1:20". */
    fun mmss(sec: Int): String = "%d:%02d".format(java.util.Locale.US, abs(sec) / 60, abs(sec) % 60)

    /** Sayı adedi aralığı: "4–6" ya da tek değerse "5". */
    fun countRange(min: Int, max: Int): String = if (min == max) "$min" else "$min–$max"
}

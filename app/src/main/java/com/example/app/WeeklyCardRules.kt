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
        val order = compareBy<Item> { it.tier }.thenByDescending { it.magnitude }
        val all: List<Item> = cups + sobi + chests + learned.take(1)
        val headline = all.sortedWith(order).firstOrNull()

        // Her türün ana satır olmayan en iyisi, ana satırla aynı öncelikle sıralı: özette yalnızca ilki
        // görünüyor (kullanıcı kararı: "bir somut satır daha").
        val extras = listOfNotNull<Item>(
            cups.firstOrNull { it != headline },
            chests.firstOrNull { it != headline },
            learned.firstOrNull { it != headline },
            sobi.firstOrNull { it != headline },
        ).sortedWith(order).take(3)

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

    // ── Somut önce/sonra: örnek sayılar (kullanıcı isteği 08.11.2026) ─────────
    //
    // "1 basamaklı 4–6 sayı → 2 basamaklı" veliye hâlâ soyut geldi. İstenen: "önceden 240 + 320 + 423'ü
    // her sayı 4 saniyede topluyordun, şimdi aynı sayıları 2 saniyede". Kupa yolunda ve Sobi testinde
    // çözülen işlemler saklanmıyor; sayılar o seviyenin kuralıyla ÜRETİLİYOR ve ekranda "örnek" diye
    // yazılıyor. Aynı hafta + aynı yol hep aynı sayıları veriyor (kart her açılışta değişmesin).
    // Yalnızca hız değiştiyse önce ve sonra AYNI sayılar: farkı tek bir şeye indirmek okumayı kolaylaştırıyor.

    enum class Op { ADD, SUB, MUL }

    /** Ana satırın neyi söylediği; cümle buna göre seçiliyor (öncelik bu sırada). */
    enum class Change { DIGITS, COUNT, FASTER, NONE }

    /** Bir seviyenin somut hâli: [count] sayı, her biri [digits] basamaklı, [intervalMs] arayla. */
    data class Level(val digits: Int, val count: Int, val intervalMs: Long, val mulSecond: Int? = null)

    data class Concrete(
        val op: Op,
        /** Abaküse bakmadan (körleme kupası ya da Sobi'nin zihinden kuralı). */
        val mental: Boolean,
        val before: Level,
        val after: Level,
        val beforeText: String,
        val afterText: String,
        val change: Change,
    ) {
        val sameNumbers: Boolean get() = beforeText == afterText
    }

    fun opOfCup(field: String): Op = when {
        field.contains("extraction") -> Op.SUB
        field.contains("impact") -> Op.MUL
        else -> Op.ADD
    }

    /** Sobi kural anahtarı ("m_add_five") → işlem; çarpma anahtarları Sobi seviyesinde yok. */
    fun opOfRuleKey(key: String): Op =
        if (key.removePrefix(MENTAL_PREFIX).startsWith("sub")) Op.SUB else Op.ADD

    fun levelOf(l: CupLevel): Level = Level(l.digits, l.countMin, l.intervalMs, l.mulSecondDigits)

    fun changeOf(b: Level, a: Level): Change = when {
        a.digits > b.digits || (a.mulSecond ?: 0) > (b.mulSecond ?: 0) -> Change.DIGITS
        a.count > b.count -> Change.COUNT
        a.intervalMs in 1 until b.intervalMs -> Change.FASTER
        else -> Change.NONE
    }

    /** Ana satır kupa ya da Sobi ise somut hâli; sandık ve ders kendi verisiyle çiziliyor (null). */
    fun concrete(item: Item, seedKey: String): Concrete? {
        val (op, mental, b, a) = when (item) {
            is Item.CupStep -> {
                val b = levelOf(item.before)
                var a = levelOf(item.after)
                // Aralığın en az sayı adedi aynı kalıp en çoğu arttıysa "daha çok sayı" yine söylenebilsin.
                if (changeOf(b, a) == Change.NONE && item.after.countMax > item.before.countMax) {
                    a = a.copy(count = item.after.countMax)
                }
                Quad(opOfCup(item.field), isBlindingCup(item.field), b, a)
            }
            is Item.SobiStep -> {
                val sb = com.example.app.abacus.SobiTestLevels.spec(item.before)
                val sa = com.example.app.abacus.SobiTestLevels.spec(item.after)
                Quad(opOfRuleKey(item.key), item.key.startsWith(MENTAL_PREFIX),
                    Level(sb.digits, sb.count, sb.intervalMs), Level(sa.digits, sa.count, sa.intervalMs))
            }
            else -> return null
        }
        val seed = seedKey.hashCode().toLong()
        return Concrete(
            op, mental, b, a,
            exampleText(op, exampleNumbers(op, b, seed)),
            exampleText(op, exampleNumbers(op, a, seed)),
            changeOf(b, a),
        )
    }

    private data class Quad(val op: Op, val mental: Boolean, val b: Level, val a: Level)

    private fun pow10(n: Int): Int { var p = 1; repeat(n) { p *= 10 }; return p }

    /**
     * Seviyeye uygun örnek sayılar. Aynı tohum + aynı basamak → aynı başlangıç: yalnızca adet arttıysa
     * "sonra" dizisi "önce"nin devamı oluyor (240 + 320 → 240 + 320 + 423), yalnızca hız değiştiyse aynısı.
     * Çıkarmada sonuç eksiye düşmesin diye çıkanlar kalan miktarı paylaşıyor; gerekirse daha az basamaklı.
     */
    fun exampleNumbers(op: Op, level: Level, seed: Long): List<Int> {
        val r = java.util.Random(seed * 31 + level.digits)
        fun num(d: Int): Int {
            val lo = pow10(d - 1)
            return lo + r.nextInt(pow10(d) - lo)
        }
        if (op == Op.MUL) return listOf(num(level.digits), num(level.mulSecond ?: 1))
        val count = level.count.coerceAtLeast(2)
        if (op == Op.ADD) return List(count) { num(level.digits) }
        // Çıkarma: büyük bir başlangıç (en büyük basamağı 7–9), sonra çıkanlar. Kalan yetmezse o sayı
        // eklenir (işaretli, + olarak yazılır): 1 basamaklı 10+ sayıda yalnızca çıkarma mümkün değil ve
        // çıkarma kupası da sonucun eksiye düşmesine izin vermiyor.
        val d = level.digits
        val start = (7 + r.nextInt(3)) * pow10(d - 1) + if (d > 1) r.nextInt(pow10(d - 1)) else 0
        val out = mutableListOf(start)
        var left = start
        for (i in 1 until count) {
            val v = num(d)
            if (v <= left) { out += -v; left -= v } else { out += v; left += v }
        }
        return out
    }

    /** Çarpmada iki çarpan; toplama/çıkarmada işaretli dizi (eksi = çıkan). */
    /**
     * Ekranda en çok bu kadar sayı yazılır, fazlası "…": yedi tane 3 basamaklı sayı telefonda iki satıra
     * taşıyordu (09.11.2026). Kaç sayı olduğunu merdivendeki etiket ve cümle zaten söylüyor.
     */
    const val EXAMPLE_MAX_SHOWN = 5

    fun exampleText(op: Op, nums: List<Int>): String {
        if (op == Op.MUL) return "${nums[0]} × ${nums[1]}"
        fun signed(n: Int) = if (n < 0) " − ${-n}" else " + $n"
        val shown = if (nums.size > EXAMPLE_MAX_SHOWN) nums.take(EXAMPLE_MAX_SHOWN - 1) else nums
        val more = if (nums.size > EXAMPLE_MAX_SHOWN) " + …" else ""
        return shown.first().toString() + shown.drop(1).joinToString("") { signed(it) } + more
    }

    // ── Seviye merdiveni (kullanıcı kararı 09.11.2026) ───────────────────────
    //
    // "Geçen hafta / bu hafta" iki satır metin olarak net görünmüyordu; çizgi grafik için de henüz yalnızca
    // 1–2 haftalık kayıt var (2 noktalı çizgi bir şey anlatmıyor). Merdiven: basamaklar soldan sağa, geçen
    // haftanın yeri küçük gri, bu haftanın yeri büyük yeşil. 4 hafta birikince altına çizgi eklenecek.

    /**
     * [total] basamak (0..total-1); [from] geçen hafta, [to] bu hafta (0 tabanlı). [gap]: arada atlanmış
     * basamaklar var, yol ortasında kırık işaretiyle çiziliyor (gerçek mesafe ekrana sığmıyor).
     */
    data class Ladder(val total: Int, val from: Int, val to: Int, val gap: Boolean = false)

    /**
     * Sıçrama bundan büyükse merdiven sıkıştırılır (09.11.2026: 14 basamaklı yol "çok uzun bir yol" gibi
     * duruyordu ve noktalar sıkışıyordu): geçen hafta 2., bu hafta sondan 3. basamakta, arada kırık.
     */
    const val LADDER_MAX_JUMP = 5
    private const val LADDER_COMPACT_TOTAL = 9

    fun ladderFor(item: Item): Ladder? = when (item) {
        is Item.CupStep -> {
            // Kural tablosu 100 puanda bir değiştiği için bir basamak = 100 puan. Bir basamak geriden
            // başlayıp iki ileride bitiyor: "daha gidilecek yol var" görünsün.
            val b = item.beforeScore / 100
            val a = item.afterScore / 100
            if (a - b > LADDER_MAX_JUMP) {
                Ladder(LADDER_COMPACT_TOTAL, 1, LADDER_COMPACT_TOTAL - 3, gap = true)
            } else {
                val start = maxOf(0, b - 1)
                val end = maxOf(a + 2, start + 4)
                Ladder(end - start + 1, b - start, a - start)
            }
        }
        is Item.SobiStep -> {
            val max = com.example.app.abacus.SobiTestLevels.MAX
            Ladder(max, (item.before - 1).coerceIn(0, max - 1), (item.after - 1).coerceIn(0, max - 1))
        }
        else -> null
    }

    // ── Biçim yardımcıları (dilden bağımsız) ─────────────────────────────────

    /** 80 → "1:20". */
    fun mmss(sec: Int): String = "%d:%02d".format(java.util.Locale.US, abs(sec) / 60, abs(sec) % 60)

    /** Sayı adedi aralığı: "4–6" ya da tek değerse "5". */
    fun countRange(min: Int, max: Int): String = if (min == max) "$min" else "$min–$max"
}

package com.example.app.abacus

import com.example.app.homework.AbacusRule
import com.example.app.homework.HomeworkConfig
import com.example.app.homework.HomeworkEngine
import com.example.app.homework.HomeworkOp
import com.example.app.homework.HomeworkQuestion
import kotlin.random.Random

/**
 * SOBİ'NİN TESTİ — hangi kurallar, hangi sorular (kullanıcı kararı 01.11.2026: Sobi çocuğun eksiğine
 * göre test verir; günde 1 test + istenirse ek test, 10 soru; zorluk çocuğun seviyesine göre).
 *
 * Kural motoru seçiyor, yapay zekâ yok: kesin ve ücretsiz.
 *  - Zayıf kurallar: kural başarısında (dersler + ödevler + Sobi; son ~50 işlem) en az [MIN_OPS] işlemi
 *    olan kurallardan oranı en düşük olanlar; %[WEAK_PCT] altındaki en çok 2 kural, hiçbiri o kadar düşük
 *    değilse en düşüğü (çocuk yine pekiştirsin).
 *  - Sorular: zayıf kurallardan çoğunluk, kalanı bildiği öbür kurallardan (pekiştirme).
 *  - Zorluk: her soru kendi kuralının seviyesinde ([SobiTestLevels]: basamak, sayı adedi, ara). Seviye
 *    kayıtlı değilse kural başarısından tahmin. [offset] çocuğun seçimi (Kolay −1, Benim seviyem 0, Zor +1);
 *    pekiştirme soruları bir seviye kolay.
 *  - Sorular ödev motoruyla ([HomeworkEngine]) üretiliyor: yalnızca izin verilen kurallarla hamle seçiyor,
 *    çok sayılı soruyu kurabiliyor. Üretilen soru çalışılan kuralı gerçekten içermiyorsa atılıyor. O
 *    seviyede kurulamıyorsa (ör. yalnızca 5'lik kuralla çok sayı) bir alt seviye deneniyor.
 *  - Yalnızca çocuğun verisi olan kurallar: hiç görmediği bir kural çıkmaz.
 *
 * ABAKÜSSÜZ (ZİHİNDEN) SORULAR (kullanıcı kararları 07.11.2026): soroban'ın asıl hedefi zihinden hesap ve
 * Part 4–5 körleme dersleri bunu öğretiyor; test yalnızca abaküslü soru verirken bu eksik hiç ölçülmüyordu.
 *  - Aynı "görmediği kural çıkmaz" kuralı: bir kural ancak Part 4/5'te o kuralın HERHANGİ bir dersi
 *    (ünite maratonu dahil) bitmişse abaküssüz gelir ([mentalSeenRules]). İlk ders şart değil: çocuk
 *    hızlı anlayıp sonraki dersi anahtarla açıp bitirmiş olabilir.
 *  - Başarı ve seviye abaküslüden ayrı, "m_" önekli anahtarlarda ([mentalKey]): körlemede iyi olan
 *    çocuk abaküste zayıf olabilir (ya da tersi), tek oran ikisini de gizlerdi.
 *  - Karışım: abaküssüz görülmüş kural varsa 10 sorunun [MENTAL_SLOTS]'ü abaküssüz (abaküssüzde en
 *    zayıf kuraldan); abaküssüz oran abaküslü en zayıftan [MENTAL_GAP_PCT] puan ve daha düşükse
 *    [MENTAL_SLOTS_WEAK]. Hiç görülmemişse test eskisi gibi tamamen abaküslü.
 *  - Merdiven aynı ([SobiTestLevels]), seviye ayrı; abaküssüz verisi yoksa 1. seviyeden başlar
 *    (körleme derslerinin 3 / 2,5 / 2 sn'lik 1–2 basamaklı soruları 1–3. seviyelere denk).
 * Yeterli veri yoksa null.
 */
object SobiTestPlanner {

    const val QUESTION_COUNT = 10
    const val MIN_OPS = 10
    private const val WEAK_PCT = 90
    private const val TRIES_PER_LEVEL = 40

    /** Abaküssüz başarı ve seviye anahtarlarının öneki: "m_add_ten" (sunucu: functions/homework.js). */
    const val MENTAL_PREFIX = "m_"
    private const val MENTAL_SLOTS = 3
    private const val MENTAL_SLOTS_WEAK = 5
    private const val MENTAL_GAP_PCT = 20

    fun mentalKey(rule: String): String = MENTAL_PREFIX + rule
    fun isMentalKey(key: String): Boolean = key.startsWith(MENTAL_PREFIX)
    /** "m_add_ten" → "add_ten"; abaküslü anahtar olduğu gibi döner. */
    fun baseKey(key: String): String = key.removePrefix(MENTAL_PREFIX)

    /** [key] "add_ten" gibi (abaküssüzde "m_add_ten"); [correct]/[wrong] son penceredeki işlem sayıları. */
    data class RuleScore(val key: String, val correct: Int, val wrong: Int) {
        val total: Int get() = correct + wrong
        val percent: Int get() = if (total == 0) 0 else correct * 100 / total
    }

    /**
     * Testin bir sorusu: çalıştırdığı kural ([rule] hep öneksiz, "add_ten"), oynandığı seviye (sayıların
     * arası buradan) ve abaküssüz mü. Başarının yazıldığı anahtar [statKey].
     */
    data class Question(val question: HomeworkQuestion, val rule: String, val level: Int, val mental: Boolean = false) {
        val intervalMs: Long get() = SobiTestLevels.spec(level).intervalMs
        val statKey: String get() = if (mental) mentalKey(rule) else rule
    }

    /**
     * [weak]: testin çalıştırdığı abaküslü kurallar, [mentalWeak]: abaküssüz ("m_" önekli). [current]:
     * kuralların teste girerken kayıtlı (ya da tahmin) seviyesi; [played]: zayıf kuralların bu testte
     * oynanan seviyesi (seçilen zorlukla). İkisinde de abaküssüz kural "m_" önekiyle.
     */
    data class Plan(
        val weak: List<RuleScore>,
        val current: Map<String, Int>,
        val played: Map<String, Int>,
        val questions: List<Question>,
        val mentalWeak: List<RuleScore> = emptyList(),
    )

    private val KEY = Regex("""^(add|sub)_(direct|five|ten|bead)$""")

    /** Kuralın kayıtlı seviyesi, yoksa kural başarısından tahmin. */
    fun levelOf(score: RuleScore, levels: Map<String, Int>): Int =
        levels[score.key]?.coerceIn(1, SobiTestLevels.MAX) ?: SobiTestLevels.initial(score.percent)

    /**
     * Abaküssüz kuralın seviyesi: kayıtlı, yoksa yeterli abaküssüz veri varsa ondan tahmin, o da yoksa 1
     * (kullanıcı kararı 07.11.2026). Ders bitmiş ama bu sürümden önce bittiği için sayım yoksa abaküslü
     * orandan tahmin edilmiyor: abaküsle hızlı olan çocuk zihinden yavaş olabilir.
     */
    private fun mentalLevelOf(score: RuleScore, levels: Map<String, Int>): Int =
        levels[score.key]?.coerceIn(1, SobiTestLevels.MAX)
            ?: if (score.total >= MIN_OPS) SobiTestLevels.initial(score.percent) else 1

    /** Testin kuralları ve seviyeleri (sorular üretilmeden; zorluk seçim penceresi için). */
    data class Choice(
        val weak: List<RuleScore>,
        val strong: List<RuleScore>,
        val allowed: Set<String>,
        val current: Map<String, Int>,
        /** Abaküssüz çalışılacak kural ("m_" önekli; boşsa test tamamen abaküslü). */
        val mentalWeak: List<RuleScore> = emptyList(),
        /** Abaküssüz sorularda izin verilen kurallar (öneksiz). */
        val mentalAllowed: Set<String> = emptySet(),
        /** Abaküssüz soru sayısı. */
        val mentalSlots: Int = 0,
    )

    /** [mentalSeen]: abaküssüz görülmüş kurallar (öneksiz; [mentalSeenRules]). */
    fun choose(scores: List<RuleScore>, levels: Map<String, Int>, mentalSeen: Set<String> = emptySet()): Choice? {
        val known = scores.filter { KEY.matches(it.key) && it.total > 0 }
        val rated = known.filter { it.total >= MIN_OPS }.sortedWith(compareBy({ it.percent }, { -it.total }))
        if (rated.isEmpty()) return null
        val weak = rated.filter { it.percent < WEAK_PCT }.take(2).ifEmpty { listOf(rated.first()) }
        val allowed = withDirect(known.map { it.key })
        val strong = known.filter { s -> weak.none { it.key == s.key } }
        val current = (weak + strong).associate { it.key to levelOf(it, levels) }.toMutableMap()

        val seen = mentalSeen.filter { KEY.matches(it) }.toSet()
        if (seen.isEmpty()) return Choice(weak, strong, allowed, current)
        val byKey = scores.associateBy { it.key }
        val mScores = seen.map { byKey[mentalKey(it)] ?: RuleScore(mentalKey(it), 0, 0) }
        val mRated = mScores.filter { it.total >= MIN_OPS }.sortedWith(compareBy({ it.percent }, { -it.total }))
        // Önce gerçekten zayıf olan; yoksa henüz az çalışılmış olan (ders bitti ama zihinden pek
        // sorulmadı: ölçülmeden "biliyor" sayılmasın); o da yoksa en düşük oranlı (pekiştirme).
        val mw = mRated.firstOrNull { it.percent < WEAK_PCT }
            ?: mScores.filter { it.total < MIN_OPS }.minWithOrNull(compareBy({ it.total }, { -ORDER.indexOf(baseKey(it.key)) }))
            ?: mRated.first()
        current[mw.key] = mentalLevelOf(mw, levels)
        // Abaküsle yaptığı kural zihinden çok daha kötüyse (ör. %90'a karşı %55) test zihinden ağırlıklı.
        val gap = mw.total >= MIN_OPS && weak.first().percent - mw.percent >= MENTAL_GAP_PCT
        return Choice(weak, strong, allowed, current, listOf(mw), withDirect(seen), if (gap) MENTAL_SLOTS_WEAK else MENTAL_SLOTS)
    }

    /**
     * Kuralsız işlem her sorunun temeli (5'lik/10'luk sorunun öbür basamakları kuralsız): o işlemde bir
     * kural varsa kuralsız da açık. Abaküslüde de böyleydi.
     */
    private fun withDirect(keys: Collection<String>): Set<String> = keys.toMutableSet().apply {
        if (any { it.startsWith("add_") }) add("add_direct")
        if (any { it.startsWith("sub_") }) add("sub_direct")
    }

    fun plan(scores: List<RuleScore>, levels: Map<String, Int>, offset: Int, seed: Int, mentalSeen: Set<String> = emptySet()): Plan? {
        val c = choose(scores, levels, mentalSeen) ?: return null
        val (weak, strong, allowed, current) = c
        val played = (weak + c.mentalWeak).associate { it.key to (current.getValue(it.key) + offset).coerceIn(1, SobiTestLevels.MAX) }

        // Sıralar: abaküssüz sorular araya serpiştirilmiş (art arda abaküs açılıp kapanmasın); pekiştirme
        // eskiden 3./6./9. sıradaydı, abaküssüz soru gelince ikiye (5 abaküssüzde bire) iniyor ki zayıf
        // abaküslü kural yine çoğunlukta kalsın.
        val mentalSlots = when (c.mentalSlots) {
            0 -> emptySet()
            MENTAL_SLOTS_WEAK -> setOf(1, 3, 5, 7, 9)
            else -> setOf(3, 6, 9)
        }
        val strongSlots = when {
            strong.isEmpty() -> emptySet()
            mentalSlots.isEmpty() -> setOf(2, 5, 8)
            mentalSlots.size == MENTAL_SLOTS_WEAK -> setOf(4)
            else -> setOf(2, 7)
        }
        val rnd = Random(seed)
        val out = ArrayList<Question>()
        val seen = HashSet<List<Long>>()
        var weakTurn = 0
        var strongTurn = 0
        for (i in 0 until QUESTION_COUNT) {
            val mental = i in mentalSlots
            val (key, level) = when {
                mental -> c.mentalWeak.first().let { baseKey(it.key) to played.getValue(it.key) }
                i in strongSlots -> {
                    val s = strong[strongTurn++ % strong.size]
                    s.key to (current.getValue(s.key) - 1 + offset).coerceIn(1, SobiTestLevels.MAX)
                }
                else -> {
                    val w = weak[weakTurn++ % weak.size]
                    w.key to played.getValue(w.key)
                }
            }
            var q: Question? = null
            for (attempt in 0 until 4) {
                val cand = question(key, level, if (mental) c.mentalAllowed else allowed, rnd, withAbacus = !mental) ?: break
                if (cand.question.numbers !in seen) { q = cand; break }
            }
            if (q != null) {
                seen += q.question.numbers
                out += q
            }
        }
        if (out.size < QUESTION_COUNT / 2) return null
        return Plan(weak, current, played, out, c.mentalWeak)
    }

    /** [key] kuralını içeren, [level] seviyesinde (kurulamazsa daha aşağıda) bir soru. */
    fun question(key: String, level: Int, allowed: Set<String>, rnd: Random, withAbacus: Boolean = true): Question? {
        val add = key.startsWith("add_")
        val op = if (add) HomeworkOp.ADD else HomeworkOp.SUB
        val prefix = if (add) "add_" else "sub_"
        val rules = allowed.filter { it.startsWith(prefix) }.mapNotNull { AbacusRule.fromKey(it.substringAfter('_')) }.toSet()
        var lv = level.coerceIn(1, SobiTestLevels.MAX)
        while (lv >= 1) {
            val s = SobiTestLevels.spec(lv)
            val want = if (add) s.count else s.count - 1
            val n = minOf(want, HomeworkEngine.maxNumberCount(op, rules, s.digits, withAbacus))
            if (n >= HomeworkEngine.minNumberCount(op)) {
                val cfg = HomeworkConfig(op, rules, withAbacus = withAbacus, digits = s.digits, numberCount = n, intervalMs = s.intervalMs, questionCount = 1)
                repeat(TRIES_PER_LEVEL) {
                    val q = runCatching { HomeworkEngine.generateQuestion(cfg, rnd) }.getOrNull() ?: return@repeat
                    // Abaküssüzde de kurallar abaküsteki hamlelerden: zihinden hesap abaküsü canlandırmak,
                    // yani aynı kural. Abaküssüzde ayrıca sorunun hiçbir hamlesi görülmemiş bir kurala
                    // düşmemeli (motor zaten izin verilenle kuruyor; bu, "görmediği kural çıkmaz"
                    // sözünün ikinci kilidi). Abaküslüde eski davranış korunuyor.
                    val used = rulesOf(q)
                    if (key in used && (withAbacus || used.all { it in allowed })) return Question(q, key, lv, mental = !withAbacus)
                }
            }
            lv--
        }
        // Tek basamaklı çıkarmada 10'luk ve boncuk kuralı kurulamıyor (başlangıç tek basamak): örnek
        // seçicinin 18'e kadar başlangıçlı soruları.
        val (base, n) = ExamplePicker.pick(key, rnd.nextInt(), 1, allowed) ?: return null
        return Question(HomeworkQuestion(op, listOf(base.toLong(), Math.abs(n).toLong())), key, 1, mental = !withAbacus)
    }

    /** Sorunun kullandığı kurallar ("add_ten"…): ilk sayı yazılmış, sonrakiler eklenir ya da çıkarılır. */
    fun rulesOf(q: HomeworkQuestion): Set<String> {
        if (q.op == HomeworkOp.MUL || q.numbers.size < 2) return emptySet()
        val prefix = if (q.op == HomeworkOp.ADD) "add_" else "sub_"
        val out = HashSet<String>()
        var running = q.numbers[0]
        for (n in q.numbers.drop(1)) {
            val e = StepExplainer.explain(running, q.op, n) ?: return emptySet()
            e.moves.forEach { m -> StepExplainer.rulesUsed(m).forEach { out += prefix + it.key } }
            running = e.result
        }
        return out
    }

    // ── Abaküssüz görülmüş kurallar ─────────────────────────────────────────

    /** Kural sırası (müfredat): az çalışılmış abaküssüz kurallarda yenisi önce gelsin. */
    private val ORDER = listOf("add_direct", "add_five", "add_ten", "add_bead", "sub_direct", "sub_five", "sub_ten", "sub_bead")

    /**
     * Part 4 (körleme toplama) ve Part 5 (körleme çıkarma) bölüm başlıkları → öğrettikleri kural. Başlığın
     * altındaki her ders ve ünite maratonu o kural. Kimlikler [com.example.app.model.LessonItem.stableId]:
     * hiç değişmiyor, liste sırası değişse bile eşleme kopmaz. 10'luk toplamada iki bölüm var (1–5, 6–9).
     */
    val BLINDING_SECTIONS: Map<String, String> = mapOf(
        "p4_i00_kuralsiz_toplama" to "add_direct",
        "p4_i05_5_lik_toplama" to "add_five",
        "p4_i10_10_luk_toplama_1_2_3_4_5" to "add_ten",
        "p4_i15_10_luk_toplama_6_7_8_9" to "add_ten",
        "p4_i20_boncuk_kurali" to "add_bead",
        "p5_i00_kuralsiz_cikarma" to "sub_direct",
        "p5_i05_5_lik_cikarma" to "sub_five",
        "p5_i10_10_luk_cikarma" to "sub_ten",
        "p5_i15_boncuk_cikarma" to "sub_bead",
    )

    /** Haritadaki bir satır: [isHeader] bölüm başlığı, [finished] ders bitti mi (LessonItem.stepIsFinish). */
    data class LessonMark(val stableId: String, val isHeader: Boolean, val finished: Boolean)

    /**
     * Abaküssüz görülmüş kurallar (kullanıcı kararı 07.11.2026): o kuralın bölümünde en az bir ders
     * bitmiş. [items]: bir part'ın satırları sırayla. Başlıktan önce gelen ya da tanınmayan bölümdeki
     * ders sayılmaz.
     */
    fun mentalSeenRules(items: List<LessonMark>): Set<String> {
        val out = HashSet<String>()
        var section: String? = null
        for (m in items) {
            if (m.isHeader) {
                // Tanınmayan başlık bölümü kapatır: ileride araya başka bir bölüm eklenirse onun dersleri
                // bir önceki kurala yazılmasın.
                section = BLINDING_SECTIONS[m.stableId]
                continue
            }
            val rule = section ?: continue
            if (m.finished) out += rule
        }
        return out
    }
}

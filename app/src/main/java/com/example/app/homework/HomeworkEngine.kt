package com.example.app.homework

import kotlin.math.min
import kotlin.random.Random

/**
 * ÖDEV SORU MOTORU — öğretmenin seçtiği işlem, kural, basamak ve sayı adedine göre soru üretir.
 *
 * Saf Kotlin (Android'e bağımlı değil): kurallar JVM birim testlerinde binlerce soru üretilerek
 * doğrulanıyor (HomeworkEngineTest). Metinler burada yok; arayüz katmanında.
 *
 * ## Abaküs modeli
 * Her sütunda bir beşlik ve dört birlik boncuk var; sütunun değeri d = 5·h + e (h ∈ {0,1},
 * e ∈ 0..4). Sayılar abaküse SOLDAN SAĞA işlenir (kursta öğretilen sıra): önce en büyük basamak.
 * Her sütundaki hareket [AbacusMoves] ile dört kuraldan birine ayrılır:
 *
 *  - **Kuralsız** (doğrudan): boncuklar doğrudan eklenip çıkarılabiliyor.
 *  - **5'lik kural**: sütun 10'u geçmiyor ama doğrudan olmuyor, beşlik boncuk devreye giriyor
 *    (ör. 3 + 4 = +5 −1; 5 − 1 = −5 +4).
 *  - **10'luk kural**: sütun 10'u geçiyor (çıkarmada sütundaki değer yetmiyor); sola +1 (−1) ve
 *    bu sütunda tamamlayıcı DOĞRUDAN çıkarılıp ekleniyor (ör. 7 + 8 = −2 +10).
 *  - **Boncuk kuralı**: 10'luk kural, ama tamamlayıcıyı çıkarmak/eklemek 5'lik kural istiyor
 *    (ör. 5 + 6 = −4 +10, ve 5'ten 4 çıkarmak −5 +1). Uygulamadaki "Boncuk Kuralı" dersleriyle
 *    aynı tanım (MathOperationGenerator.generateMathOperationBeadRule).
 *
 * ## Elde ve ödünç
 * 10'luk / boncuk kuralının sola eklediği +1 (çıkarmada sola giden −1) doğrudan yapılabiliyorsa
 * kuralın parçası sayılır. Sol sütun 4 ise +1 için 5'lik kural, 9 ise yeniden 10'luk kural
 * gerekir (çıkarmada sol sütun 5 ise 5'lik, 0 ise 10'luk); bunlar ancak o kurallar da seçiliyse
 * çıkar. Bu yüzden yalnızca 10'luk kural seçilince sayı adedi sınırlı: elde alan sütun 4'e
 * gelince takılıyor.
 *
 * ## "Yalnızca seçilen kurallar"
 * Bir sayı ancak abaküse işlenirken YAPILAN HER HAREKET seçilen kurallardan biriyse kabul edilir.
 * Ara basamaklarda 0 yalnızca zorunluysa gelir (sütunda seçilen kurallarla yapılabilecek bir
 * hareket kalmadıysa); en büyük basamak hiçbir zaman 0 olmaz.
 *
 * Gelişmiş ayarda öğretmen her kuralın hangi rakamlarla çalışılacağını da seçebiliyor (ör. 5'likte
 * yalnızca +3 ve +4): hareket ancak kuralı seçiliyse VE rakamı o kuralda açıksa yapılabiliyor
 * ([MoveRules]). Elde/ödünçün +1'i de bir hareket; kuralsız değilse aynı denetimden geçiyor.
 *
 * ## İki üretim yolu
 *  - Elde/ödünç yoksa (kuralsız ve/veya 5'lik): sütunlar birbirinden bağımsız; her sütun için
 *    "kalan sayılar hâlâ yapılabilir mi" tablosu ([ColumnPlan]) tam olarak hesaplanıyor. Üretim
 *    hiç başarısız olmuyor ve en fazla sayı adedi kesin (ör. kuralsız + 5'likte 9; 9 seçilince
 *    bütün basamaklar 1).
 *  - Elde/ödünç varsa (10'luk ve/veya boncuk): sütunlar elde ile birbirine bağlı. Rastgele aday
 *    sayılar abaküste simüle edilip kurala uymayanlar atılıyor; tıkanan soru baştan kuruluyor.
 *    En fazla sayı adedi bu yolun gerçekten üretebildiği değerden ([HomeworkLimitTable]).
 */

enum class HomeworkOp(val key: String) {
    ADD("add"), SUB("sub"), MUL("mul");

    companion object {
        fun fromKey(key: String?): HomeworkOp? = values().firstOrNull { it.key == key }
    }
}

/** Abaküste bir sütundaki tek hareketin hangi kuralla yapıldığı. */
enum class AbacusRule(val key: String) {
    DIRECT("direct"), FIVE("five"), TEN("ten"), BEAD("bead");

    companion object {
        fun fromKey(key: String?): AbacusRule? = values().firstOrNull { it.key == key }

        /** Kural kümesinin 4 bitlik kimliği (tablo anahtarı): DIRECT=1, FIVE=2, TEN=4, BEAD=8. */
        fun maskOf(rules: Set<AbacusRule>): Int = rules.fold(0) { acc, r -> acc or (1 shl r.ordinal) }

        fun fromMask(mask: Int): Set<AbacusRule> = values().filter { mask and (1 shl it.ordinal) != 0 }.toSet()
    }
}

/**
 * Öğretmenin seçtiği ödev içeriği.
 *
 * @param digits      Toplama/çıkarmada sayıların basamağı; çarpmada 1. sayının basamağı.
 * @param digits2     Yalnızca çarpma: 2. sayının basamağı.
 * @param numberCount Toplamada ekranda gösterilen sayı adedi; çıkarmada ÇIKARILAN sayı adedi
 *                    (başlangıç sayısı hariç; kullanıcı kararı 23.10.2026). Çarpmada kullanılmaz.
 * @param intervalMs  Sayıların gösterilme aralığı. Çarpmada kullanılmaz.
 * @param ruleDigits  Gelişmiş ayar: kural → o kuralla sütuna eklenebilecek (çıkarmada çıkarılabilecek)
 *                    rakamlar, ör. 5'lik → {3, 4}. Girişi olmayan kuralda o kuralın bütün rakamları
 *                    ([HomeworkEngine.ruleDigitOptions]). Çarpmada kullanılmaz.
 * @param mulDigits1  Gelişmiş ayar, yalnızca çarpma: 1. sayının basamağı (0 = birler) → o basamağın
 *                    alabileceği rakamlar. Girişi olmayan basamakta bütün rakamlar ([HomeworkEngine.mulDigitOptions]).
 * @param mulDigits2  Aynısı 2. sayı için.
 */
data class HomeworkConfig(
    val op: HomeworkOp,
    val rules: Set<AbacusRule> = emptySet(),
    val withAbacus: Boolean,
    val digits: Int,
    val digits2: Int = 0,
    val numberCount: Int = 0,
    val intervalMs: Long = 0L,
    val questionCount: Int,
    val ruleDigits: Map<AbacusRule, Set<Int>> = emptyMap(),
    val mulDigits1: Map<Int, Set<Int>> = emptyMap(),
    val mulDigits2: Map<Int, Set<Int>> = emptyMap(),
) {
    internal val moveRules: MoveRules get() = MoveRules(rules, ruleDigits)
}

/**
 * Bir sütun hareketine izin var mı: hareketin kuralı seçilmiş olmalı ve (gelişmiş ayarda o kural
 * için rakam seçildiyse) eklenen/çıkarılan rakam o kuralda açık olmalı.
 */
internal data class MoveRules(val rules: Set<AbacusRule>, val digits: Map<AbacusRule, Set<Int>> = emptyMap()) {
    fun allows(rule: AbacusRule, x: Int): Boolean = rule in rules && digits[rule]?.contains(x) != false
}

/**
 * Tek bir soru. Toplamada [numbers] sırayla toplanan sayılar; çıkarmada ilki başlangıç, gerisi
 * çıkarılanlar; çarpmada iki çarpan.
 */
data class HomeworkQuestion(val op: HomeworkOp, val numbers: List<Long>) {
    val answer: Long
        get() = when (op) {
            HomeworkOp.ADD -> numbers.sum()
            HomeworkOp.SUB -> numbers.first() - numbers.drop(1).sum()
            HomeworkOp.MUL -> numbers[0] * numbers[1]
        }
}

/** Tek sütun hareketinin sınıflandırması. d: sütunun değeri (0..9), x: eklenen/çıkarılan rakam (1..9). */
object AbacusMoves {

    fun classifyAdd(d: Int, x: Int): AbacusRule {
        require(d in 0..9 && x in 1..9) { "d=$d x=$x" }
        val h = d / 5
        val e = d % 5
        if (d + x <= 9) {
            // Taşma yok: ya doğrudan ya beşlikle. (h ve x'in beşliği birlikte 2 olamaz: d+x ≥ 10 olurdu.)
            return if (h + x / 5 <= 1 && e + x % 5 <= 4) AbacusRule.DIRECT else AbacusRule.FIVE
        }
        // Taşma: sola +1, bu sütundan (10 − x) çıkar. Çıkarma doğrudansa 10'luk, değilse boncuk.
        val b = 10 - x
        return if (b / 5 <= h && b % 5 <= e) AbacusRule.TEN else AbacusRule.BEAD
    }

    fun classify(op: HomeworkOp, d: Int, x: Int): AbacusRule =
        if (op == HomeworkOp.ADD) classifyAdd(d, x) else classifySub(d, x)

    fun classifySub(d: Int, x: Int): AbacusRule {
        require(d in 0..9 && x in 1..9) { "d=$d x=$x" }
        val h = d / 5
        val e = d % 5
        if (d >= x) {
            return if (x / 5 <= h && x % 5 <= e) AbacusRule.DIRECT else AbacusRule.FIVE
        }
        // Ödünç: soldan −1, bu sütuna (10 − x) ekle (d + 10 − x < 10, yani ekleme taşmaz).
        val b = 10 - x
        return if (h + b / 5 <= 1 && e + b % 5 <= 4) AbacusRule.TEN else AbacusRule.BEAD
    }
}

object HomeworkEngine {

    const val BLOCK_SIZE = 10
    const val MIN_QUESTIONS = 1
    const val MAX_QUESTIONS = 200
    const val MIN_INTERVAL_MS = 100L
    const val MAX_INTERVAL_MS = 30_000L
    const val MAX_NUMBER_COUNT = 100

    /** Rastgele yolda bir soru için baştan kurma denemesi ve sayı başına aday sayısı. */
    private const val ATTEMPTS = 200
    private const val CANDIDATE_TRIES = 40

    /** Abaküs 5 sütun (99.999); abaküssüzde sayılar ve sonuç 8 basamağa kadar (kullanıcı kararı). */
    fun capacityDigits(withAbacus: Boolean): Int = if (withAbacus) 5 else 8

    fun maxDigits(withAbacus: Boolean): Int = capacityDigits(withAbacus)

    fun minNumberCount(op: HomeworkOp): Int = if (op == HomeworkOp.SUB) 1 else 2

    fun usesCarry(rules: Set<AbacusRule>): Boolean = AbacusRule.TEN in rules || AbacusRule.BEAD in rules

    fun blockCount(questionCount: Int): Int = (questionCount + BLOCK_SIZE - 1) / BLOCK_SIZE

    /** [blockIndex]. adımdaki soru sayısı; son adım 10'dan az olabilir (ör. 25 soru → 10, 10, 5). */
    fun blockSize(questionCount: Int, blockIndex: Int): Int =
        (questionCount - blockIndex * BLOCK_SIZE).coerceIn(0, BLOCK_SIZE)

    // ── Gelişmiş ayar: rakam seçenekleri ────────────────────────────────────

    /**
     * Bu kuralla bir sütuna eklenebilen (çıkarmada çıkarılabilen) rakamlar: 5'likte 1–4, boncukta
     * 6–9, kuralsız ve 10'lukta 1–9. Sınıflandırmadan hesaplanıyor (bir sütun değerinde o kurala
     * düşen her rakam).
     */
    fun ruleDigitOptions(op: HomeworkOp, rule: AbacusRule): List<Int> =
        (1..9).filter { x -> (0..9).any { d -> AbacusMoves.classify(op, d, x) == rule } }

    /**
     * Çarpmada [digits] basamaklı sayının [position]. basamağının (0 = birler) alabileceği rakamlar.
     * En büyük basamak 0 olamaz; tek basamaklı çarpanda 1 de yok (1 ile çarpma öğretmez).
     */
    fun mulDigitOptions(digits: Int, position: Int): List<Int> = when {
        position != digits - 1 -> (0..9).toList()
        digits == 1 -> (2..9).toList()
        else -> (1..9).toList()
    }

    /**
     * Gelişmiş rakam seçimini temizler: seçilmeyen kuralların girişi atılır, rakamlar kuralın
     * seçenekleriyle kesiştirilir; boş kalan ya da bütün seçenekleri içeren giriş atılır (hepsi demek).
     */
    fun normalizeRuleDigits(op: HomeworkOp, rules: Set<AbacusRule>, raw: Map<AbacusRule, Set<Int>>): Map<AbacusRule, Set<Int>> {
        if (op == HomeworkOp.MUL) return emptyMap()
        val out = LinkedHashMap<AbacusRule, Set<Int>>()
        for (rule in AbacusRule.values()) {
            if (rule !in rules) continue
            val options = ruleDigitOptions(op, rule)
            val picked = raw[rule]?.filter { it in options }?.toSortedSet() ?: continue
            if (picked.isNotEmpty() && picked.size < options.size) out[rule] = picked
        }
        return out
    }

    /** Çarpmada aynı temizlik: basamak dışı girişler atılır, rakamlar basamağın seçenekleriyle kesişir. */
    fun normalizeMulDigits(digits: Int, raw: Map<Int, Set<Int>>): Map<Int, Set<Int>> {
        val out = LinkedHashMap<Int, Set<Int>>()
        for (position in 0 until digits) {
            val options = mulDigitOptions(digits, position)
            val picked = raw[position]?.filter { it in options }?.toSortedSet() ?: continue
            if (picked.isNotEmpty() && picked.size < options.size) out[position] = picked
        }
        return out
    }

    /** Ayarların gelişmiş kısmı temizlenmiş kopyası (sunucudan gelen ya da formdaki ham seçim için). */
    fun normalized(cfg: HomeworkConfig): HomeworkConfig = cfg.copy(
        ruleDigits = normalizeRuleDigits(cfg.op, cfg.rules, cfg.ruleDigits),
        mulDigits1 = if (cfg.op == HomeworkOp.MUL) normalizeMulDigits(cfg.digits, cfg.mulDigits1) else emptyMap(),
        mulDigits2 = if (cfg.op == HomeworkOp.MUL) normalizeMulDigits(cfg.digits2, cfg.mulDigits2) else emptyMap(),
    )

    // ── Sınırlar ────────────────────────────────────────────────────────────

    /**
     * Bu kurallar ve basamakla seçilebilecek en fazla sayı adedi (toplamada gösterilen,
     * çıkarmada çıkarılan). 0: bu kurallarla hiç soru üretilemiyor.
     *
     * Elde/ödünç gerektiren kurallarda [ruleDigits] varsa buradaki değer yalnızca ÜST SINIR
     * (süzgeçsiz tablo değeri); öğretmenin formu gerçek değeri [measureCarryMax] ile ölçüyor.
     */
    fun maxNumberCount(
        op: HomeworkOp,
        rules: Set<AbacusRule>,
        digits: Int,
        withAbacus: Boolean,
        ruleDigits: Map<AbacusRule, Set<Int>> = emptyMap(),
    ): Int {
        if (op == HomeworkOp.MUL || rules.isEmpty()) return 0
        if (digits !in 1..maxDigits(withAbacus)) return 0
        return if (!usesCarry(rules)) {
            ColumnPlan.of(op, MoveRules(rules, ruleDigits)).maxCount()
        } else {
            HomeworkLimitTable.maxCount(op, AbacusRule.maskOf(rules), digits, withAbacus)
        }
    }

    /** Elde/ödünçlü kurallarda gelişmiş rakam seçimi varsa en fazla adet ölçülmeli ([measureCarryMax]). */
    fun needsMeasuredMax(op: HomeworkOp, rules: Set<AbacusRule>, ruleDigits: Map<AbacusRule, Set<Int>>): Boolean =
        op != HomeworkOp.MUL && usesCarry(rules) && ruleDigits.isNotEmpty()

    private val measuredCache = HashMap<String, Int>()

    /** Önbellek anahtarı ve rastgele kaynağın tohumu: süreçten bağımsız (enum hash'i değil, adları). */
    private fun measureKey(op: HomeworkOp, rules: Set<AbacusRule>, ruleDigits: Map<AbacusRule, Set<Int>>, digits: Int, withAbacus: Boolean): String {
        val filter = AbacusRule.values().filter { it in ruleDigits }.joinToString(";") { "${it.key}=${ruleDigits.getValue(it).sorted()}" }
        return "${op.key}|${AbacusRule.maskOf(rules)}|$filter|$digits|$withAbacus"
    }

    /** [measureCarryMax]'ın daha önce ölçülmüş sonucu; ölçülmediyse null. */
    fun cachedCarryMax(op: HomeworkOp, rules: Set<AbacusRule>, ruleDigits: Map<AbacusRule, Set<Int>>, digits: Int, withAbacus: Boolean): Int? =
        synchronized(measuredCache) { measuredCache[measureKey(op, rules, ruleDigits, digits, withAbacus)] }

    /**
     * Gelişmiş rakam seçimiyle elde/ödünçlü kurallarda en fazla sayı adedi. [HomeworkLimitTable] ile
     * aynı yöntem (bkz. HomeworkLimitTableGeneratorTest): rastgele yolun üst üste üretebildiği değer
     * aranıyor, sonra o değerde [RELIABLE_QUESTIONS] soru daha deneniyor; biri bile üretilemezse
     * değer düşüyor. Öğrencinin telefonunda soru üretimi başarısız olmamalı. Rastgele kaynak
     * ayarlardan türetiliyor: aynı ayar her seferinde aynı sonucu veriyor.
     *
     * Ağır (bir iki saniye sürebilir): arka planda çağrılmalı. Sonuç önbellekte tutuluyor.
     */
    fun measureCarryMax(op: HomeworkOp, rules: Set<AbacusRule>, ruleDigits: Map<AbacusRule, Set<Int>>, digits: Int, withAbacus: Boolean): Int {
        cachedCarryMax(op, rules, ruleDigits, digits, withAbacus)?.let { return it }
        val key = measureKey(op, rules, ruleDigits, digits, withAbacus)
        val upper = maxNumberCount(op, rules, digits, withAbacus)
        val min = minNumberCount(op)
        val rng = Random(key.hashCode())
        var n = if (upper < min) 0 else probeCarryMax(op, rules, digits, withAbacus, rng, questions = 20, upper = upper, ruleDigits = ruleDigits)
        while (n >= min) {
            val cfg = HomeworkConfig(op, rules, withAbacus, digits, numberCount = n, intervalMs = 1000L, questionCount = 1, ruleDigits = ruleDigits)
            if ((1..RELIABLE_QUESTIONS).all { probeGenerate(cfg, rng) }) break
            n--
        }
        val result = if (n < min) 0 else n
        synchronized(measuredCache) { measuredCache[key] = result }
        return result
    }

    private const val RELIABLE_QUESTIONS = 300

    /**
     * Öğretmene önerilen sayı adedi: en fazla değere yaklaştıkça sayılar tek tipe dönüyor. Elde
     * gerektiren kurallarda sayıları zorlayan bir sınır olmadığı için öneri yok (null).
     */
    fun recommendedNumberCount(op: HomeworkOp, rules: Set<AbacusRule>, ruleDigits: Map<AbacusRule, Set<Int>> = emptyMap()): Int? {
        if (op == HomeworkOp.MUL || rules.isEmpty() || usesCarry(rules)) return null
        val max = ColumnPlan.of(op, MoveRules(rules, ruleDigits)).maxCount()
        if (max == 0) return null
        // Kuralsız + 5'lik toplamada 9'dan 4 (kullanıcının önerisi); diğerlerinde aynı oran.
        return ((max * 4 + 8) / 9).coerceIn(minNumberCount(op), max)
    }

    /**
     * Seçilen adette her sayının her basamağı tek bir rakama zorlanıyor mu (ör. kuralsız + 5'lik
     * toplamada 9 sayı → hepsi 1, yani 111 + 111 + …). Arayüz bu durumda öğretmeni uyarır.
     *
     * @return Zorlanan rakam dizisi (ilk sayıdan başlayarak her sayının rakamı), yoksa null.
     */
    fun forcedPattern(
        op: HomeworkOp,
        rules: Set<AbacusRule>,
        numberCount: Int,
        ruleDigits: Map<AbacusRule, Set<Int>> = emptyMap(),
    ): List<Int>? {
        if (op == HomeworkOp.MUL || rules.isEmpty() || usesCarry(rules)) return null
        val plan = ColumnPlan.of(op, MoveRules(rules, ruleDigits))
        var d = plan.stepOptionsAtStart(numberCount).singleOrNull() ?: return null
        val pattern = mutableListOf(d)
        val steps = if (op == HomeworkOp.ADD) numberCount - 1 else numberCount
        for (k in 0 until steps) {
            val x = plan.stepOptions(d, steps - 1 - k).singleOrNull() ?: return null
            pattern += x
            d = plan.next(d, x)
        }
        return pattern
    }

    /** Ayarlar geçerli mi; değilse sebebi. Sunucu aynı aralıkları ayrıca doğruluyor (functions/homework.js). */
    fun validate(cfg: HomeworkConfig): String? {
        if (cfg.questionCount !in MIN_QUESTIONS..MAX_QUESTIONS) return "questionCount"
        val cap = capacityDigits(cfg.withAbacus)
        return when (cfg.op) {
            HomeworkOp.MUL -> when {
                cfg.digits < 1 || cfg.digits2 < 1 -> "digits"
                cfg.digits + cfg.digits2 > cap -> "digits"
                normalizeMulDigits(cfg.digits, cfg.mulDigits1) != cfg.mulDigits1 -> "mulDigits"
                normalizeMulDigits(cfg.digits2, cfg.mulDigits2) != cfg.mulDigits2 -> "mulDigits"
                else -> null
            }
            else -> when {
                cfg.rules.isEmpty() -> "rules"
                cfg.digits !in 1..maxDigits(cfg.withAbacus) -> "digits"
                cfg.intervalMs !in MIN_INTERVAL_MS..MAX_INTERVAL_MS -> "interval"
                normalizeRuleDigits(cfg.op, cfg.rules, cfg.ruleDigits) != cfg.ruleDigits -> "ruleDigits"
                cfg.numberCount < minNumberCount(cfg.op) -> "numberCount"
                cfg.numberCount > maxNumberCount(cfg.op, cfg.rules, cfg.digits, cfg.withAbacus, cfg.ruleDigits) -> "numberCount"
                else -> null
            }
        }
    }

    // ── Üretim ──────────────────────────────────────────────────────────────

    /**
     * Bir adımın soruları. Aynı adımda aynı soru tekrar etmesin diye birkaç kez yeniden
     * deneniyor; seçenek azsa (ör. yalnızca 5'lik, 1 basamak, 2 sayı) tekrar kaçınılmaz.
     */
    fun generateBlock(cfg: HomeworkConfig, blockIndex: Int, rng: Random = Random.Default): List<HomeworkQuestion> {
        val size = blockSize(cfg.questionCount, blockIndex)
        val seen = HashSet<List<Long>>()
        return List(size) {
            var q = generateQuestion(cfg, rng)
            var tries = 0
            while (q.numbers in seen && tries < 5) {
                q = generateQuestion(cfg, rng)
                tries++
            }
            seen += q.numbers
            q
        }
    }

    /** @throws IllegalStateException ayarlar geçersizse ya da soru üretilemediyse. */
    fun generateQuestion(cfg: HomeworkConfig, rng: Random = Random.Default): HomeworkQuestion {
        validate(cfg)?.let { throw IllegalStateException("Geçersiz ödev ayarı: $it") }
        return when (cfg.op) {
            HomeworkOp.MUL -> generateMultiplication(cfg, rng)
            HomeworkOp.ADD, HomeworkOp.SUB ->
                if (!usesCarry(cfg.rules)) generateWithoutCarry(cfg, rng)
                else generateWithCarry(cfg, rng) ?: throw IllegalStateException("Soru üretilemedi: $cfg")
        }
    }

    private fun generateMultiplication(cfg: HomeworkConfig, rng: Random): HomeworkQuestion {
        // Her basamak kendi seçeneklerinden ([mulDigitOptions]; gelişmiş ayarda öğretmenin seçtikleri).
        fun factor(digits: Int, picked: Map<Int, Set<Int>>): Long {
            var n = 0L
            for (position in digits - 1 downTo 0) {
                val options = picked[position]?.toList() ?: mulDigitOptions(digits, position)
                n = n * 10 + options.random(rng)
            }
            return n
        }
        return HomeworkQuestion(HomeworkOp.MUL, listOf(factor(cfg.digits, cfg.mulDigits1), factor(cfg.digits2, cfg.mulDigits2)))
    }

    // ── Elde/ödünç olmayan yol: sütun planı ─────────────────────────────────

    /**
     * Tek sütunda "r hareket daha yapılabilir mi" tablosu. Elde yokken sütunlar bağımsız olduğu
     * için her sütun ayrı planlanıyor; seçilen her rakam, kalan sayıların hâlâ yapılabileceği bir
     * duruma götürüyor. Böylece üretim tıkanmıyor ve en fazla adet kesin.
     *
     * Elde/ödünç varken de aynı tablo rastgele yolu YÖNLENDİRİYOR: sütunlar artık bağımsız
     * değil (soldaki sütuna gelen +1'ler tabloda yok), ama tablo olmadan rastgele seçim dar
     * yolları kaçırıyordu. Örnek: kuralsız olmadan 5'lik + 10'luk toplamada birler sütunu
     * yalnızca 9 → 4 → 8 → 3 → 7 → 2 → 6 → 1 yolunu izleyebiliyor (8 sayı); yönlendirmesiz
     * motor 5'te kalıyordu.
     *
     * Toplamada sayılar `r` = kalan eklenecek sayı; çıkarmada `r` = kalan çıkarılacak sayı.
     */
    internal class ColumnPlan private constructor(
        private val op: HomeworkOp,
        private val rules: MoveRules,
    ) {
        /** feasible[r][d]: sütun d'deyken r hareket daha seçilen kurallarla yapılabilir mi. */
        private val feasible = Array(MAX_NUMBER_COUNT + 1) { BooleanArray(10) }

        init {
            for (d in 0..9) feasible[0][d] = true
            for (r in 1..MAX_NUMBER_COUNT) for (d in 0..9) {
                feasible[r][d] = moves(d).any { x -> feasible[r - 1][next(d, x)] }
            }
        }

        /**
         * d'den seçilen kurallarla yapılabilen hareketler. Elde/ödünç kuralları seçilmediyse
         * sınıflandırma taşan hareketleri zaten 10'luk/boncuk sayıp dışarıda bırakıyor.
         */
        fun moves(d: Int): List<Int> = (1..9).filter { x -> rules.allows(AbacusMoves.classify(op, d, x), x) }

        /** Hareketten sonra SÜTUNUN değeri (elde/ödünç sola gider, burada mod 10). */
        fun next(d: Int, x: Int): Int = if (op == HomeworkOp.ADD) (d + x) % 10 else (d - x + 10) % 10

        /** Toplamada ilk sayı yerleştirilir (hareket değil): kalan n−1 sayı. Çıkarmada n çıkarma. */
        private fun movesAfterStart(numberCount: Int): Int =
            if (op == HomeworkOp.ADD) numberCount - 1 else numberCount

        /** Başlangıçta (ilk sayı / başlangıç sayısı) bu sütuna konabilecek rakamlar. */
        fun stepOptionsAtStart(numberCount: Int): List<Int> {
            val r = movesAfterStart(numberCount)
            if (r !in 0..MAX_NUMBER_COUNT) return emptyList()
            return (1..9).filter { feasible[r][it] }
        }

        fun maxCount(): Int {
            val min = minNumberCount(op)
            return (MAX_NUMBER_COUNT downTo min).firstOrNull { stepOptionsAtStart(it).isNotEmpty() } ?: 0
        }

        /** d'deyken bu hareketten sonra r hareket daha kalacaksa seçilebilecek rakamlar. */
        fun stepOptions(d: Int, remainingAfter: Int): List<Int> =
            moves(d).filter { x -> feasible[remainingAfter][next(d, x)] }

        companion object {
            private val cache = HashMap<Pair<HomeworkOp, MoveRules>, ColumnPlan>()

            fun of(op: HomeworkOp, rules: MoveRules): ColumnPlan = synchronized(cache) {
                cache.getOrPut(op to rules) { ColumnPlan(op, rules) }
            }
        }
    }

    private fun generateWithoutCarry(cfg: HomeworkConfig, rng: Random): HomeworkQuestion {
        val plan = ColumnPlan.of(cfg.op, cfg.moveRules)
        val columns = cfg.digits
        // Sütun değerleri; 0 = birler.
        val startOptions = plan.stepOptionsAtStart(cfg.numberCount)
        val state = IntArray(columns) { pickWeighted(startOptions, cfg.op, -1, cfg.rules, rng) }
        val numbers = mutableListOf(toNumber(state))
        val steps = if (cfg.op == HomeworkOp.ADD) cfg.numberCount - 1 else cfg.numberCount
        for (k in 0 until steps) {
            val remainingAfter = steps - 1 - k
            val digits = IntArray(columns) { col ->
                pickWeighted(plan.stepOptions(state[col], remainingAfter), cfg.op, state[col], cfg.rules, rng)
            }
            for (col in 0 until columns) state[col] = plan.next(state[col], digits[col])
            numbers += toNumber(digits)
        }
        return HomeworkQuestion(cfg.op, numbers)
    }

    // ── Elde/ödünç olan yol: rastgele aday + abaküs simülasyonu ─────────────

    private fun generateWithCarry(cfg: HomeworkConfig, rng: Random): HomeworkQuestion? {
        val cap = capacityDigits(cfg.withAbacus)
        val capMax = pow10(cap) - 1
        val minNumber = pow10(cfg.digits - 1)
        val plan = ColumnPlan.of(cfg.op, cfg.moveRules)
        repeat(ATTEMPTS) {
            val state = IntArray(cap)
            val start = if (cfg.op == HomeworkOp.ADD) randomAliveStart(cfg, plan, cfg.digits, rng)
            else randomAliveStart(cfg, plan, subtractionStartDigits(cfg), rng)
            setState(state, start)
            val numbers = mutableListOf(start)
            var value = start
            val steps = if (cfg.op == HomeworkOp.ADD) cfg.numberCount - 1 else cfg.numberCount
            var ok = true
            for (k in 0 until steps) {
                val remainingAfter = (steps - 1 - k).toLong()
                val cand = pickCandidate(cfg, plan, state, remainingAfter.toInt(), rng) { n ->
                    if (cfg.op == HomeworkOp.ADD) value + n + remainingAfter * minNumber <= capMax
                    else value - n >= remainingAfter * minNumber
                }
                if (cand == null) { ok = false; break }
                numbers += cand
                value = if (cfg.op == HomeworkOp.ADD) value + cand else value - cand
            }
            if (ok) return HomeworkQuestion(cfg.op, numbers)
        }
        return null
    }

    /**
     * Tablo üretimi için (HomeworkLimitTableGeneratorTest): rastgele yolun [questions] soruyu üst
     * üste üretebildiği en fazla adet. Uygulama çalışırken çağrılmaz; sonuç [HomeworkLimitTable]'a
     * yazılıyor.
     */
    internal fun probeCarryMax(
        op: HomeworkOp,
        rules: Set<AbacusRule>,
        digits: Int,
        withAbacus: Boolean,
        rng: Random,
        questions: Int,
        upper: Int = MAX_NUMBER_COUNT,
        ruleDigits: Map<AbacusRule, Set<Int>> = emptyMap(),
    ): Int {
        fun ok(n: Int): Boolean {
            val cfg = HomeworkConfig(
                op, rules, withAbacus, digits, numberCount = n, intervalMs = 1000L, questionCount = 1, ruleDigits = ruleDigits,
            )
            repeat(questions) { if (generateWithCarry(cfg, rng) == null) return false }
            return true
        }
        val min = minNumberCount(op)
        if (!ok(min)) return 0
        if (ok(upper)) return upper
        var lo = min
        var hi = upper
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (ok(mid)) lo = mid else hi = mid
        }
        return lo
    }

    /** Tablo üretimi ve testler için: [cfg] doğrulanmadan bir soru üretilebiliyor mu. */
    internal fun probeGenerate(cfg: HomeworkConfig, rng: Random): Boolean = generateWithCarry(cfg, rng) != null

    /**
     * Çıkarmada başlangıç sayısının basamağı: ödünç için sayıların üstünde en az bir sütun ve
     * çıkarılacak toplamı karşılayacak kadar yer (ör. 2 basamaklı 5 sayı → 3 basamak, 99×5=495).
     * Abaküs sınırını aşamaz.
     */
    internal fun subtractionStartDigits(cfg: HomeworkConfig): Int =
        min(capacityDigits(cfg.withAbacus), cfg.digits + cfg.numberCount.toString().length)

    /**
     * Başlangıç: her sütuna, o sütunda seçilen kurallarla en az bir hareketin yapılabildiği rakam.
     * Sayıların basamağının üstündeki sütunlar (yalnızca çıkarmanın başlangıcında) yalnızca ödünç
     * verir; orada 1..9 yeter.
     */
    private fun randomAliveStart(cfg: HomeworkConfig, plan: ColumnPlan, startDigits: Int, rng: Random): Long {
        // Sayıların en büyük basamağı her sayıda hareket ediyor: oraya bütün adımları
        // taşıyabilecek bir rakam. Ötekilerde (0 atlanabildiği için) en az bir hareket yeter.
        val planned = plan.stepOptionsAtStart(cfg.numberCount)
        val digits = IntArray(startDigits) { col ->
            val leading = col == startDigits - 1
            val pool = (if (leading) 1..9 else 0..9).toList()
            val alive = when {
                col >= cfg.digits -> pool.filter { it != 0 }
                col == cfg.digits - 1 && planned.isNotEmpty() -> pool.filter { it in planned }
                else -> pool.filter { d -> plan.moves(d).isNotEmpty() }
            }
            (alive.ifEmpty { pool.filter { it != 0 } }).random(rng)
        }
        return toNumber(digits)
    }

    /**
     * Sıradaki sayı: her sütun için o sütunun ŞU ANKİ değerinden seçilen kurallarla yapılabilen
     * rakamlar arasından seçiliyor (soldan sağa işlendiği için bir sütunun kendi hareketi, o
     * sayının daha sağdaki sütunlarından etkilenmez; yalnızca elde/ödünçler sonradan gelir).
     * Sonra aday abaküste simüle ediliyor: elde/ödünç adımları da kurala uymalı.
     *
     * Adayların ilk yarısında ara basamaklar yalnızca zorunluysa 0; ikinci yarısında 0'a da izin
     * var (eldeyi bir sütundan uzak tutmak bazen tek yol).
     */
    private fun pickCandidate(
        cfg: HomeworkConfig,
        plan: ColumnPlan,
        state: IntArray,
        remainingAfter: Int,
        rng: Random,
        fits: (Long) -> Boolean,
    ): Long? {
        repeat(CANDIDATE_TRIES) { attempt ->
            val looseZeros = attempt >= CANDIDATE_TRIES / 2
            val digits = IntArray(cfg.digits)
            for (col in cfg.digits - 1 downTo 0) {
                val d = state[col]
                val all = plan.moves(d)
                // Önce kalan sayıları taşıyabilecek hareketler; yoksa (elde planı bozduysa) hepsi.
                val options = plan.stepOptions(d, remainingAfter.coerceIn(0, MAX_NUMBER_COUNT)).ifEmpty { all }
                val leading = col == cfg.digits - 1
                if (leading && options.isEmpty()) return null
                digits[col] = when {
                    options.isEmpty() -> 0
                    !leading && looseZeros && rng.nextInt(4) == 0 -> 0
                    else -> pickWeighted(options, cfg.op, d, cfg.rules, rng)
                }
            }
            val n = toNumber(digits)
            if (!fits(n)) return@repeat
            val trial = state.copyOf()
            val ok = if (cfg.op == HomeworkOp.ADD) applyAdd(trial, digits, cfg.moveRules) else applySub(trial, digits, cfg.moveRules)
            if (ok) {
                trial.copyInto(state)
                return n
            }
        }
        return null
    }

    /**
     * Seçilen kurallar arasında kuralsız olmayanlar iki kat ağırlıklı: öğretmen 5'lik/10'luk
     * seçtiyse o kuralı çalıştırmak istiyor; eşit ağırlıkta kuralsız hareketler baskın çıkıyordu.
     */
    private fun pickWeighted(options: List<Int>, op: HomeworkOp, d: Int, rules: Set<AbacusRule>, rng: Random): Int {
        check(options.isNotEmpty())
        // Başlangıç rakamı (d < 0) bir hareket değil; kuralsız seçilmediyse de ağırlık anlamsız.
        if (d < 0 || rules.size == 1 || AbacusRule.DIRECT !in rules) return options.random(rng)
        val weights = options.map { x ->
            val m = if (op == HomeworkOp.ADD) AbacusMoves.classifyAdd(d, x) else AbacusMoves.classifySub(d, x)
            if (m == AbacusRule.DIRECT) 1 else 2
        }
        var roll = rng.nextInt(weights.sum())
        for (i in options.indices) {
            roll -= weights[i]
            if (roll < 0) return options[i]
        }
        return options.last()
    }

    // ── Abaküs simülasyonu ──────────────────────────────────────────────────

    /** [digits] (0 = birler) sayısını abaküse soldan sağa ekler; bir hareket kurala uymazsa false. */
    internal fun applyAdd(state: IntArray, digits: IntArray, rules: MoveRules): Boolean {
        for (col in digits.indices.reversed()) {
            val x = digits[col]
            if (x != 0 && !addAt(state, col, x, rules, isCarry = false)) return false
        }
        return true
    }

    internal fun applySub(state: IntArray, digits: IntArray, rules: MoveRules): Boolean {
        for (col in digits.indices.reversed()) {
            val x = digits[col]
            if (x != 0 && !subAt(state, col, x, rules, isBorrow = false)) return false
        }
        return true
    }

    private fun addAt(state: IntArray, col: Int, x: Int, rules: MoveRules, isCarry: Boolean): Boolean {
        if (col >= state.size) return false // abaküs taştı
        val d = state[col]
        val move = AbacusMoves.classifyAdd(d, x)
        // Eldenin doğrudan +1'i 10'luk/boncuk kuralının parçası.
        if (!rules.allows(move, x) && !(isCarry && move == AbacusRule.DIRECT)) return false
        return if (d + x >= 10) {
            state[col] = d + x - 10
            addAt(state, col + 1, 1, rules, isCarry = true)
        } else {
            state[col] = d + x
            true
        }
    }

    private fun subAt(state: IntArray, col: Int, x: Int, rules: MoveRules, isBorrow: Boolean): Boolean {
        if (col >= state.size) return false // sonuç negatife düşerdi
        val d = state[col]
        val move = AbacusMoves.classifySub(d, x)
        if (!rules.allows(move, x) && !(isBorrow && move == AbacusRule.DIRECT)) return false
        return if (d < x) {
            state[col] = d + 10 - x
            subAt(state, col + 1, 1, rules, isBorrow = true)
        } else {
            state[col] = d - x
            true
        }
    }

    // ── Yardımcılar ─────────────────────────────────────────────────────────

    internal fun pow10(exp: Int): Long {
        var out = 1L
        repeat(exp) { out *= 10 }
        return out
    }

    /** Rakam dizisi (0 = birler) → sayı. */
    private fun toNumber(digits: IntArray): Long = digits.foldRight(0L) { d, acc -> acc * 10 + d }

    internal fun digitsOf(n: Long, size: Int): IntArray {
        var x = n
        return IntArray(size) { (x % 10).toInt().also { x /= 10 } }
    }

    private fun setState(state: IntArray, n: Long) {
        val digits = digitsOf(n, state.size)
        digits.copyInto(state)
    }
}

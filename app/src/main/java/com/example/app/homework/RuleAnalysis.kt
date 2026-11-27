package com.example.app.homework

/**
 * Ödevde kural kural başarı: öğrenci hangi kuralı doğru, hangisini yanlış uyguladı.
 *
 * NEDEN SORU DEĞİL İŞLEM SAYILIYOR
 *   Bir soruda birden fazla kural geçebiliyor (33 + 49: onlarda 5'lik, birlerde 10'luk). Yalnızca
 *   cevaba bakılsa yanlış bir sorunun hangi kuraldan yandığı bilinemezdi. Bu yüzden her sayının her
 *   basamağı ayrı bir İŞLEM ("onlar +4", "birler +9"), soroban sırasıyla soldan sağa; her işlemin
 *   kuralı ayrı sayılıyor.
 *
 * NASIL BULUNUYOR
 *   Abaküsün değeri öğrenci her boncuk oynattığında zamanıyla kaydediliyor (HomeworkPlayerFragment).
 *   İşlemler bu kayıttaki değerlere sırayla eşleniyor: i. işlemin "sonrası" kayıtta bir konum, bir
 *   önceki işlemin "sonrası" da daha önceki bir konum. İşlem, iki konumdaki değerlerin farkı tam
 *   işlemin miktarıysa DOĞRU. Konumlar, doğru işlem sayısını en çok yapacak biçimde seçiliyor
 *   (dinamik programlama). Böylece:
 *   - Önceki hata sonrakileri etkilemiyor: 44 + 21 + 12'de öğrenci 65 yerine 75 bulduysa "+12"
 *     75 üstünden değerlendirilir (75 → 87 doğru).
 *   - 44 + 49 = 93 yerine 103: kayıtta önce 94 (5 eklendi, 1 çıkarılmadı) varsa onlardaki 5'lik
 *     yanlış, birlerdeki 10'luk (94 → 103) doğru çıkar; 84 sonra 83 varsa 5'lik doğru, 10'luk yanlış.
 *   - Bir işlem ancak sayı tahtada göründükten sonra doğru sayılabiliyor (rastlantı eşleşmesi az).
 *   Son konum öğrencinin cevabı (Kontrol anındaki değer).
 *
 *   Kural, ÖĞRENCİNİN o anki değeri üzerinden hesaplanıyor (75 + 12'nin kuralı 75'e göre). Elde
 *   zinciri başka bir kural gerektiriyorsa (49 + 1: birlerde 10'luk, onlarda 4 + 1 = 5'lik) işlem
 *   ikisine de sayılıyor; elde için doğrudan ±1 kuralın parçası (HomeworkEngine.addAt ile aynı).
 *   Cevap doğruysa bütün işlemler doğru.
 *
 * SAYILMAYAN SORULAR (istatistiği yanıltmasın; kullanıcıyla 25.10.2026'da konuşuldu)
 *   - Adım adım çözülmemiş: her sayıdan sonraki ara toplamların (44 + 21 + 12'de 44, 65) yarısından
 *     azı kayıtta. Abaküslü ödevde sonucu akıldan bulup doğrudan girmek böyle görünür; doğru
 *     cevap bütün kurallara doğru, yanlış cevap bütün kurallara yanlış yazılırdı.
 *   - Cevap yanlış ve ilk sayı abaküse hiç girilmemiş, ya da yanlış işlem gerçekçi bir dikkat
 *     hatasından fazla (2'den ve işlemlerin üçte birinden çok): öğrenci adım adım gitmemiş (ör.
 *     iki sayıyı aklında birleştirip tek seferde eklemiş), hata yanlış kurallara yazılırdı.
 *   Herkes soldan sağa çözüyor varsayılıyor (kullanıcı kararı); sağdan sola çözüm denenmiyor.
 *
 * Çarpmada basamak kuralı yok: soru, basamak çiftine ("2x1") tek deneme olarak sayılıyor.
 * Abaküssüz toplama/çıkarmada kayıt yok, sayılmıyor (kullanıcı kararı, 25.10.2026).
 */
object RuleAnalysis {

    /** Abaküsün t anındaki değeri (her değişimde bir kayıt). */
    data class Sample(val tMs: Long, val value: Long)

    /** Tek deneme: [key] istatistik anahtarı ("add_five", "sub_ten", "mulA_2x1"…). */
    data class Attempt(val key: String, val correct: Boolean)

    fun ruleKey(op: HomeworkOp, rule: AbacusRule) = "${op.key}_${rule.key}"

    fun mulKey(withAbacus: Boolean, digits1: Int, digits2: Int) = "${if (withAbacus) "mulA" else "mulM"}_${digits1}x$digits2"

    /** Çarpma sorusu: basamak çiftine tek deneme. */
    fun multiplication(cfg: HomeworkConfig, correct: Boolean): List<Attempt> =
        listOf(Attempt(mulKey(cfg.withAbacus, cfg.digits, cfg.digits2), correct))

    private data class Op(val number: Int, val col: Int, val x: Int, val delta: Long)

    /** Yanlış cevaplı soruda kabul edilen en çok yanlış işlem (işlemlerin üçte biri daha çoksa o). */
    private const val MAX_SLIPS = 2

    /**
     * Öğrenci adım adım mı gitmiş: her sayıdan sonraki ara toplamların (sonuncusu, yani cevap hariç)
     * en az yarısı kayıtta. Cevap yanlışsa ara toplamlar ilk hatadan sonra kayar; o yüzden yanlış
     * cevapta yalnızca hataya kadarki ara toplamlar beklenebilir: en az biri (ilk sayı) yeter, gerisini
     * yanlış işlem sınırı eliyor.
     */
    private fun steppedThrough(q: HomeworkQuestion, log: List<Sample>): Boolean {
        val seen = log.mapTo(HashSet()) { it.value }
        val sign = if (q.op == HomeworkOp.ADD) 1 else -1
        var sum = q.numbers[0]
        val partials = ArrayList<Long>()
        for (k in 1 until q.numbers.size) { partials += sum; sum += sign * q.numbers[k] }
        val hit = partials.count { it in seen }
        val correct = log.lastOrNull()?.value == q.answer
        return if (correct) hit * 2 >= partials.size else hit >= 1
    }

    /**
     * Abaküslü toplama/çıkarma sorusu.
     * @param log abaküs değerleri, zamana göre; ilk kayıt sorunun başındaki değer
     * @param shownAtMs i. sayının tahtada göründüğü an (numbers ile aynı sırada)
     * @param answer öğrencinin cevabı (Kontrol anındaki değer)
     */
    fun abacus(q: HomeworkQuestion, log: List<Sample>, shownAtMs: List<Long>, answer: Long, capacity: Int = 5): List<Attempt> {
        if (q.op == HomeworkOp.MUL || q.numbers.size < 2) return emptyList()
        val sign = if (q.op == HomeworkOp.ADD) 1 else -1
        val ops = ArrayList<Op>()
        for (k in 1 until q.numbers.size) {
            val ds = HomeworkEngine.digitsOf(q.numbers[k], capacity)
            for (col in ds.indices.reversed()) if (ds[col] != 0) {
                ops += Op(k, col, ds[col], sign * ds[col] * HomeworkEngine.pow10(col))
            }
        }
        if (ops.isEmpty()) return emptyList()
        if (!steppedThrough(q, log)) return emptyList()

        val bases: List<Long>
        val ok: BooleanArray
        if (answer == q.answer) {
            // Doğru cevap: işlemler beklenen sırayla, beklenen değerlerden
            var v = q.numbers[0]
            bases = ops.map { op -> v.also { v += op.delta } }
            ok = BooleanArray(ops.size) { true }
        } else {
            // Kayıt boşsa ya da son değer cevap değilse cevabı sona ekle
            val samples = if (log.isEmpty() || log.last().value != answer) log + Sample(Long.MAX_VALUE, answer) else log
            val (b, c) = align(ops, samples, shownAtMs, q.numbers[0])
            bases = b
            ok = c
            val wrong = ok.count { !it }
            if (log.none { it.value == q.numbers[0] } || wrong > maxOf(MAX_SLIPS, (ops.size + 2) / 3)) return emptyList()
        }
        val out = ArrayList<Attempt>()
        ops.forEachIndexed { i, op ->
            for (rule in rulesOf(q.op, bases[i], op.col, op.x, capacity)) out += Attempt(ruleKey(q.op, rule), ok[i])
        }
        return out
    }

    /**
     * İşlemleri kayda eşler. Döner: her işlemin öğrenci tarafındaki başlangıç değeri ve doğru mu.
     * best[i][j]: ilk i işlem, i. işlemin sonrası j. kayıtta → puan. Doğru işlem 2 puan; taban ilk sayıya
     * eşitse 1 puan (yalnızca eşitliği bozar: yanlış işlemin kuralı öğrencinin girdiği ilk sayıdan
     * hesaplansın, rastgele bir ara değerden değil). Eşit puanlarda en yakın (son) konum seçiliyor.
     */
    private fun align(ops: List<Op>, s: List<Sample>, shownAtMs: List<Long>, first: Long): Pair<List<Long>, BooleanArray> {
        val m = ops.size
        val n = s.size
        val neg = Int.MIN_VALUE / 4
        val best = Array(m + 1) { IntArray(n) }
        val from = Array(m + 1) { IntArray(n) }
        val credited = Array(m + 1) { BooleanArray(n) }
        // i = 0: taban (ilk sayıdan sonra öğrencinin değeri), herhangi bir konum
        for (j in 0 until n) best[0][j] = if (s[j].value == first) 1 else 0
        for (i in 1..m) {
            val op = ops[i - 1]
            val shown = shownAtMs.getOrElse(op.number) { 0L }
            var prefBest = neg
            var prefArg = 0
            val byValue = HashMap<Long, Pair<Int, Int>>() // değer → (en iyi skor, konum)
            for (j in 0 until n) {
                // j' ≤ j: önceki işlemin konumu
                val prev = best[i - 1][j]
                if (prev >= prefBest) { prefBest = prev; prefArg = j }
                val cur = byValue[s[j].value]
                if (cur == null || prev >= cur.first) byValue[s[j].value] = prev to j
                var score = prefBest
                var arg = prefArg
                var cr = false
                if (s[j].tMs >= shown) {
                    val hit = byValue[s[j].value - op.delta]
                    if (hit != null && hit.first + 2 >= score) { score = hit.first + 2; arg = hit.second; cr = true }
                }
                best[i][j] = score
                from[i][j] = arg
                credited[i][j] = cr
            }
        }
        // Son işlemin sonrası = cevap (son kayıt)
        val pos = IntArray(m + 1)
        pos[m] = n - 1
        val ok = BooleanArray(m)
        for (i in m downTo 1) {
            ok[i - 1] = credited[i][pos[i]]
            pos[i - 1] = from[i][pos[i]]
        }
        val bases = (0 until m).map { s[pos[it]].value }
        return bases to ok
    }

    /** [base] değerine [col] basamağında x eklenirken/çıkarılırken gereken kurallar (elde zinciri dahil). */
    internal fun rulesOf(op: HomeworkOp, base: Long, col: Int, x: Int, capacity: Int): Set<AbacusRule> {
        val state = HomeworkEngine.digitsOf(base.coerceIn(0L, HomeworkEngine.pow10(capacity) - 1), capacity)
        val out = LinkedHashSet<AbacusRule>()
        var c = col
        var amount = x
        var carry = false
        while (c < capacity) {
            val d = state[c]
            val rule = if (op == HomeworkOp.ADD) AbacusMoves.classifyAdd(d, amount) else AbacusMoves.classifySub(d, amount)
            if (!(carry && rule == AbacusRule.DIRECT)) out += rule
            val overflow = if (op == HomeworkOp.ADD) d + amount >= 10 else d < amount
            if (!overflow) break
            state[c] = if (op == HomeworkOp.ADD) d + amount - 10 else d + 10 - amount
            c++
            amount = 1
            carry = true
        }
        if (out.isEmpty()) out += AbacusRule.DIRECT
        return out
    }

    /** Denemeleri anahtar başına [doğru, yanlış] sayısına çevirir. */
    fun tally(attempts: List<Attempt>): Map<String, IntArray> {
        val out = LinkedHashMap<String, IntArray>()
        for (a in attempts) out.getOrPut(a.key) { IntArray(2) }[if (a.correct) 0 else 1]++
        return out
    }
}

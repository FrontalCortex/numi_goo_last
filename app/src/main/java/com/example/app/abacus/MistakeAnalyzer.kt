package com.example.app.abacus

import com.example.app.homework.AbacusRule
import com.example.app.homework.HomeworkOp

/**
 * HATA TEŞHİSİ — ders sorusunda çocuğun abaküste NEREDE yanıldığını, kural motoruyla bulur.
 *
 * NEDEN
 *   Yanlış cevapta çocuk yalnızca "doğru cevap 471" görüyordu; neyi yanlış yaptığını bilmiyordu.
 *   Artık yanlış cevap panelinde "Birlerde 3 eklerken '10 gelir' adımını unuttun" diyoruz
 *   (kullanıcı kararı 31.10.2026, docs/ANLAMADIM_YARDIM_PLANI.md "Ders ekranında hata teşhisi").
 *   Yapay zekâ yok: kesin, ücretsiz ve anında.
 *
 * NASIL
 *   Soru `ilk ± ikinci`; abaküs sıfırdan başlar. Doğru yol: önce ilk sayıyı yazmak (0 + ilk,
 *   hep kuralsız), sonra ikinciyi basamak basamak eklemek/çıkarmak ([StepExplainer] hamleleri).
 *   Çocuğun değer kaydında (abaküsün her değişimdeki değeri) her hamlenin sonucu sırayla aranır;
 *   bulunamayan ilk hamle hatanın yeri. Türü:
 *   - Unutulan adım: hamle birden çok adımlıysa ve kayıtta "hamlenin sonucu − o adımın katkısı"
 *     varsa (471 yerine 461: "10 gelir" yapılmamış).
 *   - Yanlış sütun değeri: kayıtta yalnızca o sütunu farklı olan bir değer var.
 *   - İlk sayıyı yanlış yazmak: kayıtta `cevap ∓ ikinci` var (ikinci doğru işlenmiş, başlangıç yanlış).
 *   - Hiçbiri değilse yalnızca hatanın yeri ve o hamlenin doğru adımları.
 *
 * NE ZAMAN SUSAR (null)
 *   Çocuk adım adım gitmediyse (sonucu kafadan yazdı, ilk sayı kayıtta yok ve "yanlış yazılmış ilk
 *   sayı" da yok), çarpma/bölme, abaküse sığmayan işlem. Yanlış bir teşhis hiç teşhis olmamasından
 *   kötü: çocuk doğru yaptığı bir adımı "yanlış" sanar.
 *
 * Metinler derslerdeki sözlerle ("5 gelir, kardeşi gider", "10 gelir, büyük kardeşi gider");
 * sayılara ek getirilmiyor (sunucudaki tools.js ile aynı gerekçe: yanlış ek göze batar).
 */
object MistakeAnalyzer {

    enum class Kind { MISSING_STEP, WRONG_COLUMN, WRONG_FIRST_NUMBER, AT_MOVE }

    /** [move]: hatanın yapıldığı hamle (WRONG_FIRST_NUMBER'da null). [text]: çocuğa gösterilecek metin. */
    data class Diagnosis(val kind: Kind, val text: String, val move: StepExplainer.ColumnMove?)

    private val COLUMN_NAMES = listOf("birler", "onlar", "yüzler", "binler", "on binler")

    fun columnName(col: Int): String = COLUMN_NAMES.getOrElse(col) { "${col + 1}. sütun" }

    fun ruleName(rule: AbacusRule): String = when (rule) {
        AbacusRule.DIRECT -> "kuralsız"
        AbacusRule.FIVE -> "5'lik kural"
        AbacusRule.TEN -> "10'luk kural"
        AbacusRule.BEAD -> "boncuk kuralı"
    }

    /** Adımın derste söylenen hâli (sunucudaki tools.stepPhrase ile aynı sözler, kısa). */
    fun stepPhrase(s: StepExplainer.Step, m: StepExplainer.ColumnMove): String {
        val add = m.op == HomeworkOp.ADD
        val amount = Math.abs(s.upper) * 5 + Math.abs(s.lower)
        return when (s.role) {
            StepExplainer.Role.DIRECT -> "$amount ${if (s.upper + s.lower > 0) "gelir" else "gider"}"
            StepExplainer.Role.FIVE -> if (s.upper > 0) "5 gelir" else "5 gider"
            StepExplainer.Role.FRIEND -> "kardeşi ${Math.abs(s.lower)} ${if (s.lower > 0) "gelir" else "gider"}"
            StepExplainer.Role.BIG_FRIEND -> if (add) "büyük kardeşi $amount gider" else "kardeşi $amount gelir"
            StepExplainer.Role.BEADS -> "${s.lower} boncuk gelir"
            StepExplainer.Role.TEN -> if (add) "10 gelir" else "10 gider"
        }
    }

    /**
     * @param first ilk sayı, @param op "+" ya da "-", @param second ikinci sayı
     * @param log abaküsün bu sorudaki değerleri, sırayla (art arda aynı değerler olabilir)
     * @param answer çocuğun cevabı ("Kontrol Et" anındaki değer)
     */
    fun analyze(first: Int, op: String, second: Int, log: List<Int>, answer: Int): Diagnosis? {
        val hop = when (op) { "+" -> HomeworkOp.ADD; "-" -> HomeworkOp.SUB; else -> return null }
        val expected = if (hop == HomeworkOp.ADD) first + second else first - second
        if (answer == expected) return null
        // İlk sayı abaküse sığmalı (yazma hamleleri hep kuralsız; yalnızca sınır denetimi).
        StepExplainer.explain(0, HomeworkOp.ADD, first.toLong()) ?: return null
        val opPlan = StepExplainer.explain(first.toLong(), hop, second.toLong()) ?: return null
        val values = dedupe(log + answer)

        val firstAt = values.indexOf(first)
        if (firstAt == -1) {
            // İlk sayı hiç yazılmamış: ya yanlış yazıldı ve ikinci doğru işlendi, ya da çocuk adım adım gitmedi.
            val wrongFirst = if (hop == HomeworkOp.ADD) answer - second else answer + second
            if (wrongFirst < 0 || wrongFirst !in values) return null
            return wrongFirstNumber(first, wrongFirst)
        }

        var from = firstAt
        for (m in opPlan.moves) {
            val at = indexFrom(values, m.valueAfter.toInt(), from)
            if (at != -1) { from = at; continue }
            return diagnoseMove(m, values, from)
        }
        // Bütün hamleler doğru yapılmış ama cevap başka: sonradan bir boncuk oynamış; teşhis yok.
        return null
    }

    /**
     * Analiz ekranında tek adım: çocuk bu hamleyi yaparken nerede şaştı. [log] hamlenin başından
     * (ilk kayıt hamlenin başındaki değer), [answer] "Kontrol Et" anındaki değer.
     */
    fun diagnoseStep(m: StepExplainer.ColumnMove, log: List<Int>, answer: Int): Diagnosis {
        val values = dedupe(log + answer)
        val from = values.indexOf(m.valueBefore.toInt()).coerceAtLeast(0)
        return diagnoseMove(m, values, from)
    }

    /** Analiz ekranında sayı yazma adımı: ilk farklı sütun (en büyük basamaktan). */
    fun diagnoseWrite(target: Int, value: Int): String {
        val cols = maxOf(target.toString().length, value.toString().length)
        val col = (cols - 1 downTo 0).firstOrNull { digit(target, it) != digit(value, it) } ?: 0
        return "${capital(columnName(col))} basamağına ${digit(target, col)} yazılmalı, sende ${digit(value, col)} var."
    }

    // ── Flaş dizisi ─────────────────────────────────────────────────────────

    /**
     * Flaş dizisinde yanlış yapılan bir işlem: abaküste [base] varken dizinin [index]. sayısı
     * ([number], eksi = çıkarma) işlenirken [value] olmuş. [rule]: işlemin hatanın düştüğü kuralı
     * (ilk sayıyı yazmakta null). [diagnosis]: o işlemde nerede şaşıldığı (bulunamazsa null).
     */
    data class SequenceMistake(
        val index: Int,
        val base: Int,
        val number: Int,
        val value: Int,
        val rule: AbacusRule?,
        val diagnosis: Diagnosis?,
    )

    /**
     * Sayılar ekrana tek tek gelirken çocuğun abaküste hangi işlemlerde yanıldığı (kullanıcı kararı
     * 31.10.2026: analiz ekranında yalnızca bunlar çözdürülür, ör. "78'e 9 eklemek").
     *
     * [log]: dizinin başından abaküsün değerleri; [marks]: her sayı göründüğü anda kayda eklenen
     * değerin sırası (`log[marks[k]]` = k. sayı göründüğünde abaküste yazan). [answer]: Kontrol Et
     * anındaki değer.
     *
     * Her işlem çocuğun KENDİ önceki değerine göre denetlenir: 9. sayıda şaşan çocuğun ardından doğru
     * yaptığı eklemeler hata sayılmaz. Geç kalan çocuk (sayıyı sıradaki sayı gelince işleyen) hata
     * sayılmasın diye işlemin sonucu sıradaki sayının penceresinde de aranır.
     *
     * Hiç hata bulunamazsa boş liste (ör. cevaptan sonra bir boncuk oynadı); kayıt uygun değilse ya da
     * dizi abaküse sığmıyorsa null: çağıran bütün diziyi çözdürür.
     */
    fun findSequenceMistakes(numbers: List<Int>, log: List<Int>, marks: List<Int>, answer: Int): List<SequenceMistake>? {
        if (numbers.isEmpty() || marks.size != numbers.size) return null
        if (marks.any { it !in log.indices } || marks.zipWithNext().any { (a, b) -> b < a }) return null
        val full = log + answer
        val out = ArrayList<SequenceMistake>()
        var pos = marks[0]
        var base = 0
        for (k in numbers.indices) {
            val n = numbers[k]
            val target = base + n
            if (target !in 0..MAX_VALUE) return null
            val hop = if (n < 0) HomeworkOp.SUB else HomeworkOp.ADD
            val plan = StepExplainer.explain(base.toLong(), hop, Math.abs(n).toLong()) ?: return null
            val end = windowEnd(k, marks, full)
            val found = indexIn(full, target, pos, end)
            if (found != -1) { pos = found; base = target; continue }

            val at = settledAfterMistake(k, numbers, full, marks, pos, end)
            val value = full[at]
            val segment = full.subList(pos, at + 1)
            val diagnosis = if (k == 0) {
                Diagnosis(Kind.WRONG_FIRST_NUMBER, diagnoseWrite(target, value), null)
            } else diagnoseOperation(plan, base, segment, value)
            val rule = if (k == 0) null else diagnosis?.move?.rule ?: mainRule(plan)
            out += SequenceMistake(k, base, n, value, rule, diagnosis)
            pos = at
            base = value
        }
        return out
    }

    /** "10'luk kuralda 2 hata ve boncuk kuralında 1 hata": en çok hatalı kural önce. */
    fun ruleSummary(mistakes: List<SequenceMistake>): String {
        val parts = mistakes.groupBy { it.rule }.entries
            .sortedWith(compareByDescending<Map.Entry<AbacusRule?, List<SequenceMistake>>> { it.value.size }.thenBy { it.key?.ordinal ?: -1 })
            .map { (rule, list) -> "${ruleLocative(rule)} ${list.size} hata" }
        return when (parts.size) {
            0 -> ""
            1 -> parts[0]
            else -> parts.dropLast(1).joinToString(", ") + " ve " + parts.last()
        }
    }

    private fun ruleLocative(rule: AbacusRule?): String = when (rule) {
        null -> "ilk sayıyı yazarken"
        AbacusRule.DIRECT -> "kuralsız işlemlerde"
        AbacusRule.FIVE -> "5'lik kuralda"
        AbacusRule.TEN -> "10'luk kuralda"
        AbacusRule.BEAD -> "boncuk kuralında"
    }

    /** k. işlemin sonucu nereye kadar aranır: sıradaki sayının göründüğü süre de dahil (geç kalan çocuk). */
    private fun windowEnd(k: Int, marks: List<Int>, full: List<Int>): Int =
        if (k + 2 < marks.size) marks[k + 2] else full.lastIndex

    /**
     * Hatalı işlemden sonra çocuğun abaküsteki değeri (sıradaki işlemin başlangıcı): sıradaki işlem
     * ondan doğru yapılmışsa o değer. Pencerenin SONUNDAN aranır: hatalı işlemin ara değerleri
     * (50 + 9 yaparken 55) daha önce gelir; geç kalan çocukta asıl değer sıradaki sayı göründükten
     * sonradır. Tutarlı değer yoksa sıradaki sayı göründüğündeki değer.
     */
    private fun settledAfterMistake(k: Int, numbers: List<Int>, full: List<Int>, marks: List<Int>, pos: Int, end: Int): Int {
        if (k == numbers.lastIndex) return full.lastIndex
        val next = numbers[k + 1]
        val nextEnd = windowEnd(k + 1, marks, full)
        for (i in end downTo pos) {
            if (indexIn(full, full[i] + next, i + 1, nextEnd) != -1) return i
        }
        return marks[k + 1]
    }

    /** Tek işlemde ([plan], [base]'den) çocuğun ilk şaştığı hamle; kayıt [segment], sonuç [value]. */
    private fun diagnoseOperation(plan: StepExplainer.Explanation, base: Int, segment: List<Int>, value: Int): Diagnosis? {
        val values = dedupe(segment + value)
        var from = values.indexOf(base).coerceAtLeast(0)
        for (m in plan.moves) {
            val at = indexFrom(values, m.valueAfter.toInt(), from)
            if (at != -1) { from = at; continue }
            return diagnoseMove(m, values, from)
        }
        return null
    }

    /** İşlemin en zor kuralı (hata yeri bulunamazsa özet için). */
    private fun mainRule(plan: StepExplainer.Explanation): AbacusRule =
        plan.moves.map { it.rule }.maxByOrNull { it.ordinal } ?: AbacusRule.DIRECT

    private fun indexIn(values: List<Int>, v: Int, from: Int, to: Int): Int {
        for (i in from..minOf(to, values.lastIndex)) if (values[i] == v) return i
        return -1
    }

    private const val MAX_VALUE = 99999

    private fun diagnoseMove(m: StepExplainer.ColumnMove, values: List<Int>, from: Int): Diagnosis {
        val after = values.subList(from + 1, values.size)
        val target = m.valueAfter.toInt()
        val head = "${capital(columnName(m.col))} basamağında ${m.x} ${if (m.op == HomeworkOp.ADD) "eklerken" else "çıkarırken"} " +
            "${ruleName(m.rule)} gerekiyordu: ${m.steps.joinToString(", ") { stepPhrase(it, m) }}."

        // Unutulan adım (yalnızca çok adımlı hamlede: tek adımlıda "hiçbir şey yapmamak" da bu kalıba uyardı).
        if (m.steps.size >= 2) {
            for (s in m.steps) {
                val delta = stepDelta(s)
                val missing = target - delta
                val missingAt = after.indexOf(missing)
                // Çocuk sonra aynı sütunda başka bir değere geçtiyse (1 yerine 2 götürmek) adımı unutmamış,
                // yanlış yapmış: aşağıdaki "yanlış sütun değeri" anlatır.
                val movedOn = missingAt != -1 && after.drop(missingAt + 1).any { differsOnlyIn(it, target, m.col) }
                if (missing != m.valueBefore.toInt() && missingAt != -1 && !movedOn) {
                    val tail = when {
                        s.carry != null && m.op == HomeworkOp.ADD ->
                            " ${capital(columnName(s.col))} sütununa 1 boncuk gelmeliydi."
                        s.carry != null -> " ${capital(columnName(s.col))} sütunundan 1 boncuk gitmeliydi."
                        else -> ""
                    }
                    return Diagnosis(Kind.MISSING_STEP, "$head Sen \"${stepPhrase(s, m)}\" adımını yapmamışsın.$tail", m)
                }
            }
        }

        // Yalnızca bu sütunu farklı olan bir değer: o sütunda yanlış sayıda boncuk. Hamlenin başındaki
        // değer de aday: çocuk o sütunda hiçbir şey yapmadıysa son değeri odur.
        val wrong = values.subList(from, values.size).lastOrNull { differsOnlyIn(it, target, m.col) }
        if (wrong != null) {
            return Diagnosis(
                Kind.WRONG_COLUMN,
                "$head ${capital(columnName(m.col))} sütununda ${digit(target, m.col)} olmalıydı, sende ${digit(wrong, m.col)} oldu.",
                m,
            )
        }
        return Diagnosis(Kind.AT_MOVE, "Hata burada: $head", m)
    }

    private fun wrongFirstNumber(first: Int, wrote: Int): Diagnosis {
        // İlk farklı sütun (en büyük basamaktan): çocuğun ilk şaştığı yer.
        val cols = maxOf(first.toString().length, wrote.toString().length)
        val col = (cols - 1 downTo 0).firstOrNull { digit(first, it) != digit(wrote, it) } ?: 0
        return Diagnosis(
            Kind.WRONG_FIRST_NUMBER,
            "İlk sayıyı yazarken ${columnName(col)} basamağına ${digit(first, col)} yazılmalıydı, sende ${digit(wrote, col)} oldu. " +
                "İkinci sayıyı doğru işlemişsin!",
            null,
        )
    }

    /** Adımın abaküsün değerine katkısı; elde adımında sol sütundaki hareketin tamamı. */
    private fun stepDelta(s: StepExplainer.Step): Int {
        val carry = s.carry
        if (carry != null) return (carry.valueAfter - carry.valueBefore).toInt()
        return (s.upper * 5 + s.lower) * pow10(s.col)
    }

    private fun differsOnlyIn(a: Int, b: Int, col: Int): Boolean {
        if (a == b) return false
        for (c in 0..5) if (c != col && digit(a, c) != digit(b, c)) return false
        return true
    }

    private fun digit(v: Int, col: Int): Int = (v / pow10(col)) % 10

    private fun pow10(n: Int): Int { var r = 1; repeat(n) { r *= 10 }; return r }

    private fun capital(s: String) = s.replaceFirstChar { it.uppercase() }

    private fun indexFrom(values: List<Int>, v: Int, from: Int): Int {
        for (i in from until values.size) if (values[i] == v) return i
        return -1
    }

    private fun dedupe(log: List<Int>): List<Int> {
        val out = ArrayList<Int>()
        for (v in log) if (out.lastOrNull() != v) out += v
        return out
    }
}

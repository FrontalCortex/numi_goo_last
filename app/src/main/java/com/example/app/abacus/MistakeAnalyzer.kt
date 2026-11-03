package com.example.app.abacus

import com.example.app.AppLanguage
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
    private val COLUMN_NAMES_EN = listOf("ones", "tens", "hundreds", "thousands", "ten thousands")
    private val COLUMN_NAMES_ID = listOf("satuan", "puluhan", "ratusan", "ribuan", "puluh ribuan")

    /**
     * Metinlerin dili ([AppLanguage.textLanguage]); kurallar ve sıralama her dilde aynı. Endonezce
     * sözler docs/CEVIRI_SOZLUGU_ID.md'deki sempoa kursu terimleri ("tambah 5, kurangi temannya").
     */
    private fun lang() = AppLanguage.textLanguage()
    private const val EN = AppLanguage.ENGLISH
    private const val ID = AppLanguage.INDONESIAN

    fun columnName(col: Int): String = when (lang()) {
        EN -> COLUMN_NAMES_EN.getOrElse(col) { "column ${col + 1}" }
        ID -> COLUMN_NAMES_ID.getOrElse(col) { "kolom ${col + 1}" }
        else -> COLUMN_NAMES.getOrElse(col) { "${col + 1}. sütun" }
    }

    fun ruleName(rule: AbacusRule): String = when (lang()) {
        EN -> when (rule) {
            AbacusRule.DIRECT -> "direct"
            AbacusRule.FIVE -> "small friends"
            AbacusRule.TEN -> "big friends"
            AbacusRule.BEAD -> "mixed friends"
        }
        ID -> when (rule) {
            AbacusRule.DIRECT -> "langsung"
            AbacusRule.FIVE -> "rumus teman kecil"
            AbacusRule.TEN -> "rumus teman besar"
            AbacusRule.BEAD -> "rumus gabungan"
        }
        else -> turkishRuleName(rule)
    }

    private fun turkishRuleName(rule: AbacusRule): String = when (rule) {
        AbacusRule.DIRECT -> "kuralsız"
        AbacusRule.FIVE -> "5'lik kural"
        AbacusRule.TEN -> "10'luk kural"
        AbacusRule.BEAD -> "boncuk kuralı"
    }

    /** Adımın derste söylenen hâli (sunucudaki tools.stepPhrase ile aynı sözler, kısa). */
    fun stepPhrase(s: StepExplainer.Step, m: StepExplainer.ColumnMove): String {
        val add = m.op == HomeworkOp.ADD
        val amount = Math.abs(s.upper) * 5 + Math.abs(s.lower)
        when (lang()) {
            EN -> return when (s.role) {
                StepExplainer.Role.DIRECT -> "${if (s.upper + s.lower > 0) "add" else "take away"} $amount"
                StepExplainer.Role.FIVE -> if (s.upper > 0) "add 5" else "take away 5"
                StepExplainer.Role.FRIEND -> "${if (s.lower > 0) "add" else "take away"} its friend ${Math.abs(s.lower)}"
                StepExplainer.Role.BIG_FRIEND -> if (add) "take away its big friend $amount" else "add its friend $amount"
                StepExplainer.Role.BEADS -> "add ${s.lower} beads"
                StepExplainer.Role.TEN -> if (add) "add 10" else "take away 10"
            }
            ID -> return when (s.role) {
                StepExplainer.Role.DIRECT -> "${if (s.upper + s.lower > 0) "tambah" else "kurangi"} $amount"
                StepExplainer.Role.FIVE -> if (s.upper > 0) "tambah 5" else "kurangi 5"
                StepExplainer.Role.FRIEND -> "${if (s.lower > 0) "tambah" else "kurangi"} temannya ${Math.abs(s.lower)}"
                StepExplainer.Role.BIG_FRIEND -> if (add) "kurangi teman besarnya $amount" else "tambah temannya $amount"
                StepExplainer.Role.BEADS -> "tambah ${s.lower} manik"
                StepExplainer.Role.TEN -> if (add) "tambah 10" else "kurangi 10"
            }
        }
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
        when (lang()) {
            EN -> return "The ${columnName(col)} place should be ${digit(target, col)}, but you have ${digit(value, col)}."
            ID -> return "Kolom ${columnName(col)} seharusnya ${digit(target, col)}, tetapi punyamu ${digit(value, col)}."
        }
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
            .map { (rule, list) ->
                when (lang()) {
                    EN -> "${list.size} ${if (list.size == 1) "mistake" else "mistakes"} ${ruleLocativeEn(rule)}"
                    // Endonezcede ad çoğul eki almıyor: "2 kesalahan".
                    ID -> "${list.size} kesalahan ${ruleLocativeId(rule)}"
                    else -> "${ruleLocative(rule)} ${list.size} hata"
                }
            }
        val and = when (lang()) { EN -> " and "; ID -> " dan "; else -> " ve " }
        return when (parts.size) {
            0 -> ""
            1 -> parts[0]
            else -> parts.dropLast(1).joinToString(", ") + and + parts.last()
        }
    }

    private fun ruleLocativeId(rule: AbacusRule?): String = when (rule) {
        null -> "saat menulis angka pertama"
        AbacusRule.DIRECT -> "pada langkah langsung"
        AbacusRule.FIVE -> "pada rumus teman kecil"
        AbacusRule.TEN -> "pada rumus teman besar"
        AbacusRule.BEAD -> "pada rumus gabungan"
    }

    private fun ruleLocativeEn(rule: AbacusRule?): String = when (rule) {
        null -> "while writing the first number"
        AbacusRule.DIRECT -> "in direct steps"
        AbacusRule.FIVE -> "with small friends"
        AbacusRule.TEN -> "with big friends"
        AbacusRule.BEAD -> "with mixed friends"
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
        val language = lang()
        val head = when (language) {
            EN -> "In the ${columnName(m.col)} place, ${if (m.op == HomeworkOp.ADD) "adding" else "taking away"} ${m.x} " +
                "needed ${ruleName(m.rule)}: ${m.steps.joinToString(", ") { stepPhrase(it, m) }}."
            ID -> "Di kolom ${columnName(m.col)}, untuk ${if (m.op == HomeworkOp.ADD) "menambah" else "mengurangi"} ${m.x} " +
                "dipakai ${ruleName(m.rule)}: ${m.steps.joinToString(", ") { stepPhrase(it, m) }}."
            else -> "${capital(columnName(m.col))} basamağında ${m.x} ${if (m.op == HomeworkOp.ADD) "eklerken" else "çıkarırken"} " +
                "${ruleName(m.rule)} gerekiyordu: ${m.steps.joinToString(", ") { stepPhrase(it, m) }}."
        }

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
                    if (language == ID) {
                        val tailId = when {
                            s.carry != null && m.op == HomeworkOp.ADD -> " Seharusnya 1 manik masuk ke kolom ${columnName(s.col)}."
                            s.carry != null -> " Seharusnya 1 manik keluar dari kolom ${columnName(s.col)}."
                            else -> ""
                        }
                        return Diagnosis(Kind.MISSING_STEP, "$head Kamu melewatkan langkah \"${stepPhrase(s, m)}\".$tailId", m)
                    }
                    if (language == EN) {
                        val tailEn = when {
                            s.carry != null && m.op == HomeworkOp.ADD -> " 1 bead should have been added to the ${columnName(s.col)} column."
                            s.carry != null -> " 1 bead should have been taken away from the ${columnName(s.col)} column."
                            else -> ""
                        }
                        return Diagnosis(Kind.MISSING_STEP, "$head You skipped the \"${stepPhrase(s, m)}\" step.$tailEn", m)
                    }
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
                when (language) {
                    EN -> "$head The ${columnName(m.col)} column should be ${digit(target, m.col)}, but you got ${digit(wrong, m.col)}."
                    ID -> "$head Kolom ${columnName(m.col)} seharusnya ${digit(target, m.col)}, tetapi punyamu jadi ${digit(wrong, m.col)}."
                    else -> "$head ${capital(columnName(m.col))} sütununda ${digit(target, m.col)} olmalıydı, sende ${digit(wrong, m.col)} oldu."
                },
                m,
            )
        }
        val atMove = when (language) { EN -> "Here's the mistake: $head"; ID -> "Kesalahannya di sini: $head"; else -> "Hata burada: $head" }
        return Diagnosis(Kind.AT_MOVE, atMove, m)
    }

    private fun wrongFirstNumber(first: Int, wrote: Int): Diagnosis {
        // İlk farklı sütun (en büyük basamaktan): çocuğun ilk şaştığı yer.
        val cols = maxOf(first.toString().length, wrote.toString().length)
        val col = (cols - 1 downTo 0).firstOrNull { digit(first, it) != digit(wrote, it) } ?: 0
        return Diagnosis(
            Kind.WRONG_FIRST_NUMBER,
            when (lang()) {
                EN -> "When writing the first number, the ${columnName(col)} place should have been ${digit(first, col)}, " +
                    "but you got ${digit(wrote, col)}. You did the second number right!"
                ID -> "Saat menulis angka pertama, kolom ${columnName(col)} seharusnya ${digit(first, col)}, " +
                    "tetapi punyamu jadi ${digit(wrote, col)}. Angka kedua sudah kamu kerjakan dengan benar!"
                else -> "İlk sayıyı yazarken ${columnName(col)} basamağına ${digit(first, col)} yazılmalıydı, sende ${digit(wrote, col)} oldu. " +
                    "İkinci sayıyı doğru işlemişsin!"
            },
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

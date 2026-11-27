package com.example.app.abacus

import com.example.app.homework.AbacusMoves
import com.example.app.homework.AbacusRule
import com.example.app.homework.HomeworkEngine
import com.example.app.homework.HomeworkOp

/**
 * KURAL ANLATICI — abaküste bir toplama/çıkarmanın hangi boncuk hamleleriyle yapıldığını,
 * derslerde öğretilen sırayla çıkarır.
 *
 * NEDEN VAR
 *   "Sobi'ye sor" yardımcısında (docs/ANLAMADIM_YARDIM_PLANI.md) sohbeti dil modeli yapıyor ama
 *   hamleyi o hesaplamıyor: dil modelleri 5'lik/10'luk kurallarda yanlış adım uydurabiliyor, çocuğa
 *   yanlış anlatılan bir kural hiç anlatılmamasından kötü. Model hamleyi bu nesneden (sunucudaki
 *   kopyasından) ister, cevabındaki sayılar da bununla denetlenir. Aynı çıktı "Bana göster"
 *   animasyonunu da besleyecek.
 *
 * SIRA (öğreticilerdeki anlatımla aynı, strings_tutorial.xml)
 *   Sayı soldan sağa işlenir (önce en büyük basamak), her sütundaki hareketin kuralı
 *   [AbacusMoves] ile bulunur; kural mantığı burada ikinci kez yazılmıyor.
 *   Toplama:
 *   - Kuralsız: boncuklar gelir.
 *   - 5'lik: "5 gelir, kardeş gider" (3 + 4: 5 gelir, 1 gider).
 *   - 10'luk: "10 gelir, büyük kardeş gider" (8 + 4: solda 1 gelir, 6 gider).
 *   - Boncuk: "(x − 5) boncuk gelir, 5 gider, 10 gelir" (8 + 6: 1 gelir, 5 gider, 10 gelir).
 *   Çıkarma:
 *   - Kuralsız: boncuklar gider.
 *   - 5'lik: "5 gider, kardeş gelir" (5 − 1: 5 gider, 4 gelir).
 *   - 10'luk: "10 gider, kardeş gelir" (10 − 1: solda 1 gider, 9 gelir).
 *   - Boncuk: "10 gider, kardeş 5'lik kuralla gelir" (23 − 6: 10 gider, 5 gelir, 1 gider).
 *   "10 gelir/gider" sol sütunda kendi başına bir hareket: sol sütun 4 ise 5'lik, 9 ise yine
 *   10'luk kural ister (öğretici 6). Bu yüzden [Role.TEN] adımı o hareketi [Step.carry] olarak
 *   taşır; elde zinciri böyle iç içe çıkar. Kural kümesi ([rulesUsed]) ödevdeki
 *   `RuleAnalysis.rulesOf` ile aynı.
 *
 * SUNUCU KOPYASI
 *   functions/sobi/stepExplainer.js aynı mantığın JavaScript'i (araçlar sunucuda çalışacak). İki
 *   kopya shared/step_explainer_cases.json tablosu üstünden karşılaştırılıyor: burada sıra ya da
 *   rol değişirse JS de değişmeli ve tablo yeniden üretilmeli (bkz. StepExplainerTest başlığı).
 *   [encode] biçimi o tablonun dili; değiştirmek iki tarafı birden değiştirmek demek.
 */
object StepExplainer {

    /** Abaküs 5 sütun (99.999), ödevdeki abaküslü soru kapasitesiyle aynı. */
    const val DEFAULT_CAPACITY = 5

    /** Adımın derste söylenen adı. Anahtarlar JS kopyası ve tabloyla ortak. */
    enum class Role(val key: String) {
        /** Kuralsız: sayının kendisi gelir/gider. */
        DIRECT("direct"),
        /** Beşlik boncuk: "5 gelir" / "5 gider". */
        FIVE("five"),
        /** 5'e tamamlayan kardeş: "kardeş gider" / "kardeş gelir". */
        FRIEND("friend"),
        /** 10'a tamamlayan kardeş, bu sütunda doğrudan: "büyük kardeş gider" / "kardeş gelir". */
        BIG_FRIEND("big_friend"),
        /** Boncuk kuralındaki birlikler: "(x − 5) boncuk gelir". */
        BEADS("beads"),
        /** Sol sütunda "10 gelir" / "10 gider"; boncuklar [Step.carry] hareketinde. */
        TEN("ten"),
    }

    /**
     * Tek hamle. [upper]: beşlik boncuk (+1 gelir, −1 gider, 0 oynamaz). [lower]: birlik boncuk
     * adedi (+ gelir, − gider). İkisi de [col] sütununda. [Role.TEN] adımında bu sütunda boncuk
     * oynamaz (ikisi 0); hareket [carry].
     */
    data class Step(
        val role: Role,
        val col: Int,
        val upper: Int,
        val lower: Int,
        val carry: ColumnMove? = null,
    )

    /**
     * Bir sütuna tek rakamın eklenmesi/çıkarılması. [isCarry]: 10'luk/boncuk kuralının sola
     * gönderdiği ±1. [valueBefore]/[valueAfter]: abaküsün tamamının hareketten önceki ve sonraki
     * değeri (elde hareketinde, eldenin yapıldığı andaki değer).
     */
    data class ColumnMove(
        val op: HomeworkOp,
        val col: Int,
        val x: Int,
        val digitBefore: Int,
        val rule: AbacusRule,
        val isCarry: Boolean,
        val steps: List<Step>,
        val valueBefore: Long,
        val valueAfter: Long,
    )

    data class Explanation(
        val op: HomeworkOp,
        val base: Long,
        val number: Long,
        val result: Long,
        val moves: List<ColumnMove>,
    )

    /**
     * [base] abaküsteyken [number] eklenir/çıkarılır. Sonuç abaküse sığmıyorsa (taşma ya da
     * eksiye düşme) ya da girdi geçersizse null. [number] 0 ise hareket listesi boş.
     */
    fun explain(base: Long, op: HomeworkOp, number: Long, capacity: Int = DEFAULT_CAPACITY): Explanation? {
        require(op != HomeworkOp.MUL) { "Çarpmada sütun kuralı yok" }
        require(capacity in 1..18) { "capacity=$capacity" }
        val max = HomeworkEngine.pow10(capacity) - 1
        if (base !in 0..max || number !in 0..max) return null
        val result = if (op == HomeworkOp.ADD) base + number else base - number
        if (result !in 0..max) return null

        // Ara değerler base ile result arasında kalıyor (toplamada artarak, çıkarmada azalarak);
        // bu yüzden sonuç sığıyorsa hiçbir hareket abaküsün dışına taşmaz.
        val state = HomeworkEngine.digitsOf(base, capacity)
        val digits = HomeworkEngine.digitsOf(number, capacity)
        val moves = ArrayList<ColumnMove>()
        for (col in digits.indices.reversed()) {
            if (digits[col] != 0) moves += move(state, op, col, digits[col], isCarry = false)
        }
        check(valueOf(state) == result) { "base=$base op=$op number=$number → ${valueOf(state)}" }
        return Explanation(op, base, number, result, moves)
    }

    /**
     * Abaküs [base]'den [target]'a getirilecek: fark kadar toplama ya da çıkarma. Öğretici adımında
     * elde işlem değil yalnızca adımın başındaki değer ve beklenen cevap varken kullanılır.
     */
    fun explainTransition(base: Long, target: Long, capacity: Int = DEFAULT_CAPACITY): Explanation? =
        if (target >= base) explain(base, HomeworkOp.ADD, target - base, capacity)
        else explain(base, HomeworkOp.SUB, base - target, capacity)

    /** Hareketin kullandığı kurallar, elde zinciri dahil; eldenin doğrudan ±1'i kural sayılmaz. */
    fun rulesUsed(move: ColumnMove): Set<AbacusRule> {
        val out = LinkedHashSet<AbacusRule>()
        fun walk(m: ColumnMove) {
            if (!(m.isCarry && m.rule == AbacusRule.DIRECT)) out += m.rule
            m.steps.forEach { s -> s.carry?.let(::walk) }
        }
        walk(move)
        return out
    }

    /**
     * Tablo biçimi, JS kopyasıyla birebir aynı olmalı:
     *   hareket: `c<sütun><+|-><rakam>=<kural>[<adımlar>]><sonraki değer>`
     *   adım:    `<rol>@<sütun>` + (beşlik oynuyorsa `u±1`) + (birlik oynuyorsa `l±n`)
     *            elde adımı: `ten{<hareket>}`
     *   null (sığmayan işlem): `error`
     * Örnek 8 + 6: `c0+6=bead[beads@0l+1 five@0u-1 ten{c1+1=direct[direct@1l+1]>14}]>14`
     */
    fun encode(e: Explanation?): String =
        e?.moves?.joinToString(" ") { encodeMove(it) } ?: "error"

    private fun encodeMove(m: ColumnMove): String = buildString {
        append('c').append(m.col).append(if (m.op == HomeworkOp.ADD) '+' else '-').append(m.x)
        append('=').append(m.rule.key).append('[')
        append(m.steps.joinToString(" ") { s ->
            if (s.carry != null) "ten{${encodeMove(s.carry)}}"
            else buildString {
                append(s.role.key).append('@').append(s.col)
                if (s.upper != 0) append('u').append(signed(s.upper))
                if (s.lower != 0) append('l').append(signed(s.lower))
            }
        })
        append(']').append('>').append(m.valueAfter)
    }

    private fun signed(n: Int) = if (n > 0) "+$n" else "$n"

    // ── Hareket üretimi ─────────────────────────────────────────────────────

    /**
     * [state]'in [col] sütununa [x] ekler/çıkarır, adımları sırayla [state] üzerinde uygulayarak.
     * Elde adımı geldiğinde sol sütun kendi hareketiyle (özyinelemeli) işlenir; böylece her adımın
     * gördüğü değer, öğrencinin o adımda abaküste göreceği değerin aynısı.
     */
    private fun move(state: IntArray, op: HomeworkOp, col: Int, x: Int, isCarry: Boolean): ColumnMove {
        check(col < state.size) { "Abaküs taştı: col=$col" }
        val d = state[col]
        val before = valueOf(state)
        val rule = AbacusMoves.classify(op, d, x)
        val steps = ArrayList<Step>()

        fun beads(role: Role, upper: Int, lower: Int) {
            state[col] += upper * 5 + lower
            steps += Step(role, col, upper, lower)
        }
        fun ten() {
            steps += Step(Role.TEN, col + 1, 0, 0, carry = move(state, op, col + 1, 1, isCarry = true))
        }

        val b = 10 - x
        if (op == HomeworkOp.ADD) when (rule) {
            AbacusRule.DIRECT -> beads(Role.DIRECT, x / 5, x % 5)
            AbacusRule.FIVE -> { beads(Role.FIVE, 1, 0); beads(Role.FRIEND, 0, -(5 - x)) }
            AbacusRule.TEN -> { ten(); beads(Role.BIG_FRIEND, -(b / 5), -(b % 5)) }
            AbacusRule.BEAD -> { beads(Role.BEADS, 0, x - 5); beads(Role.FIVE, -1, 0); ten() }
        } else when (rule) {
            AbacusRule.DIRECT -> beads(Role.DIRECT, -(x / 5), -(x % 5))
            AbacusRule.FIVE -> { beads(Role.FIVE, -1, 0); beads(Role.FRIEND, 0, 5 - x) }
            AbacusRule.TEN -> { ten(); beads(Role.BIG_FRIEND, b / 5, b % 5) }
            AbacusRule.BEAD -> { ten(); beads(Role.FIVE, 1, 0); beads(Role.FRIEND, 0, -(x - 5)) }
        }
        return ColumnMove(op, col, x, d, rule, isCarry, steps, before, valueOf(state))
    }

    private fun valueOf(state: IntArray): Long = state.foldRight(0L) { d, acc -> acc * 10 + d }
}

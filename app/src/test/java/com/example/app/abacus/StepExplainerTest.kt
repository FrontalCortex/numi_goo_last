package com.example.app.abacus

import com.example.app.abacus.StepExplainer.Role
import com.example.app.homework.AbacusRule
import com.example.app.homework.HomeworkOp
import com.example.app.homework.RuleAnalysis
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * [StepExplainer] testleri.
 *
 * NEDEN BU KADAR AYRINTILI
 *   Bu çıktı "Sobi'ye sor"da çocuğa anlatılacak hamlenin kaynağı. Yanlış bir hamle sahada hata
 *   vermiyor, çocuk yanlış kuralı öğreniyor. Bu yüzden üç ayrı denetim var:
 *   1. Ders örnekleri: öğreticilerdeki anlatımlar (strings_tutorial.xml) elle tabloya çevrildi.
 *   2. Bağımsız boncuk simülasyonu: her adım beşlik/birlik boncuk olarak oynatılıyor; boncuk
 *      olmayan yerden boncuk gitmiyor, sonuç doğru çıkıyor.
 *   3. Kural kümesi ödevdeki RuleAnalysis.rulesOf ile aynı (öğretmen panelindeki istatistikle
 *      aynı dil).
 *
 * ORTAK TABLO (shared/step_explainer_cases.json)
 *   Sunucudaki JS kopyası (functions/sobi/stepExplainer.js) aynı tabloyla sınanıyor. Anlatıcı
 *   değişince tablo yeniden üretilir, sonra JS de aynı çıktıyı verecek şekilde düzeltilir:
 *     $env:GEN_STEP_CASES = "1"; .\gradlew :app:testDebugUnitTest --tests "*StepExplainerTest*"
 *     cd functions; npm run test:step-explainer
 *   Normal koşuda tablo okunur ve Kotlin çıktısıyla karşılaştırılır.
 */
class StepExplainerTest {

    private fun plan(base: Long, op: HomeworkOp, number: Long) =
        StepExplainer.encode(StepExplainer.explain(base, op, number))

    private val ADD = HomeworkOp.ADD
    private val SUB = HomeworkOp.SUB

    // ── 1. Ders örnekleri ───────────────────────────────────────────────────

    @Test
    fun kuralsiz_toplama() {
        // Öğretici 9: "Önce 68 sayısını abaküse yazıyoruz" — boş abaküse yazmak hep kuralsız.
        assertEquals(
            "c1+6=direct[direct@1u+1l+1]>60 c0+8=direct[direct@0u+1l+3]>68",
            plan(0, ADD, 68),
        )
    }

    @Test
    fun besli_toplama_5_gelir_kardes_gider() {
        // "5 gelir, 4'ün kardeşi 1 gider" — 3 + 4
        assertEquals("c0+4=five[five@0u+1 friend@0l-1]>7", plan(3, ADD, 4))
    }

    @Test
    fun onluk_toplama_10_gelir_buyuk_kardes_gider() {
        // Öğretici 5: 18 + 4 → "10 gelir 4'ün büyük kardeşi 6 gider"
        assertEquals(
            "c0+4=ten[ten{c1+1=direct[direct@1l+1]>28} big_friend@0u-1l-1]>22",
            plan(18, ADD, 4),
        )
    }

    @Test
    fun boncuk_kurali_toplama() {
        // Öğretici 8: "6 için: 1 boncuk gelir, 5 gider, 10 gelir." Cevap 14.
        assertEquals(
            "c0+6=bead[beads@0l+1 five@0u-1 ten{c1+1=direct[direct@1l+1]>14}]>14",
            plan(8, ADD, 6),
        )
        // Öğretici 9: 68 + 76 = 144. Onlarda 6 + 7 boncuk ("2 gelir"), birlerde 8 + 6 boncuk ("1 gelir").
        assertEquals(
            "c1+7=bead[beads@1l+2 five@1u-1 ten{c2+1=direct[direct@2l+1]>138}]>138 " +
                "c0+6=bead[beads@0l+1 five@0u-1 ten{c1+1=direct[direct@1l+1]>144}]>144",
            plan(68, ADD, 76),
        )
    }

    @Test
    fun elde_zinciri_10_gelirken_5lik() {
        // Öğretici 6: "10 gelir işlemini yaparken 5'lik veya 10'luk kuralı kullanmamız gerekebilir."
        // 49 + 1: birlerde 10'luk, solda 4 + 1 = 5'lik.
        assertEquals(
            "c0+1=ten[ten{c1+1=five[five@1u+1 friend@1l-4]>59} big_friend@0u-1l-4]>50",
            plan(49, ADD, 1),
        )
        // 99 + 1: elde iki kez 10'luk.
        assertEquals(
            "c0+1=ten[ten{c1+1=ten[ten{c2+1=direct[direct@2l+1]>199} big_friend@1u-1l-4]>109} big_friend@0u-1l-4]>100",
            plan(99, ADD, 1),
        )
    }

    @Test
    fun besli_cikarma_5_gider_kardes_gelir() {
        // Öğretici 11: "5 gider, kardeş gelir" — 5 − 1
        assertEquals("c0-1=five[five@0u-1 friend@0l+4]>4", plan(5, SUB, 1))
    }

    @Test
    fun onluk_cikarma_10_gider_kardes_gelir() {
        // Öğretici 12: "1'in kardeşi 9. 10 gider. Kardeşi 9 gelir. Cevap 9."
        assertEquals(
            "c0-1=ten[ten{c1-1=direct[direct@1l-1]>0} big_friend@0u+1l+4]>9",
            plan(10, SUB, 1),
        )
    }

    @Test
    fun boncuk_cikarma() {
        // Öğretici 14: "10 gider. 6'nın kardeşi 4 gelir." 4 kuralsız gelemez: "5 gelir. 4'ün kardeşi
        // 1 gider." Cevap 17.
        assertEquals(
            "c0-6=bead[ten{c1-1=direct[direct@1l-1]>13} five@0u+1 friend@0l-1]>17",
            plan(23, SUB, 6),
        )
    }

    @Test
    fun sigmayan_islem_null() {
        assertNull(StepExplainer.explain(99_999, ADD, 1))
        assertNull(StepExplainer.explain(0, SUB, 1))
        assertNull(StepExplainer.explain(-1, ADD, 1))
        assertEquals("error", plan(5, SUB, 6))
    }

    @Test
    fun sifir_eklemek_hareketsiz() {
        val e = StepExplainer.explain(42, ADD, 0)
        assertNotNull(e)
        assertEquals(42L, e!!.result)
        assertTrue(e.moves.isEmpty())
    }

    @Test
    fun gecis_hedefe_gore_toplama_ya_da_cikarma() {
        assertEquals(plan(68, ADD, 76), StepExplainer.encode(StepExplainer.explainTransition(68, 144)))
        assertEquals(plan(23, SUB, 6), StepExplainer.encode(StepExplainer.explainTransition(23, 17)))
    }

    // ── 2. Bağımsız boncuk simülasyonu ─────────────────────────────────────

    /** Sütun başına beşlik (iniş) ve birlik adedi; StepExplainer'ın rakam aritmetiğini kullanmıyor. */
    private class Beads(value: Long, capacity: Int) {
        val heaven = BooleanArray(capacity)
        val earth = IntArray(capacity)

        init {
            var v = value
            for (c in 0 until capacity) {
                val d = (v % 10).toInt(); v /= 10
                heaven[c] = d >= 5; earth[c] = d % 5
            }
        }

        fun value(): Long = (heaven.indices.reversed()).fold(0L) { acc, c ->
            acc * 10 + (if (heaven[c]) 5 else 0) + earth[c]
        }

        fun apply(s: StepExplainer.Step, where: String) {
            val carry = s.carry
            if (carry != null) {
                assertEquals("$where: elde adımında bu sütunda boncuk oynamaz", 0, s.upper)
                assertEquals("$where: elde adımında bu sütunda boncuk oynamaz", 0, s.lower)
                carry.steps.forEach { apply(it, where) }
                return
            }
            assertTrue("$where: boş adım", s.upper != 0 || s.lower != 0)
            when (s.upper) {
                1 -> { assertTrue("$where: beşlik zaten inik", !heaven[s.col]); heaven[s.col] = true }
                -1 -> { assertTrue("$where: gidecek beşlik yok", heaven[s.col]); heaven[s.col] = false }
                0 -> Unit
                else -> fail("$where: upper=${s.upper}")
            }
            val e = earth[s.col] + s.lower
            assertTrue("$where: birlik boncuk sınırı aşıldı ($e)", e in 0..4)
            earth[s.col] = e
        }
    }

    /** Derste öğretilen adım sırası, kurala göre. */
    private fun expectedRoles(op: HomeworkOp, rule: AbacusRule): List<Role> = when (op to rule) {
        ADD to AbacusRule.DIRECT, SUB to AbacusRule.DIRECT -> listOf(Role.DIRECT)
        ADD to AbacusRule.FIVE, SUB to AbacusRule.FIVE -> listOf(Role.FIVE, Role.FRIEND)
        ADD to AbacusRule.TEN, SUB to AbacusRule.TEN -> listOf(Role.TEN, Role.BIG_FRIEND)
        ADD to AbacusRule.BEAD -> listOf(Role.BEADS, Role.FIVE, Role.TEN)
        SUB to AbacusRule.BEAD -> listOf(Role.TEN, Role.FIVE, Role.FRIEND)
        else -> error("$op $rule")
    }

    private fun checkMove(m: StepExplainer.ColumnMove, where: String) {
        assertEquals("$where: adım sırası", expectedRoles(m.op, m.rule), m.steps.map { it.role })
        if (m.isCarry) assertEquals("$where: elde ±1", 1, m.x)
        m.steps.forEach { s -> s.carry?.let { checkMove(it, where) } }
    }

    private fun verify(base: Long, op: HomeworkOp, number: Long, capacity: Int = StepExplainer.DEFAULT_CAPACITY) {
        val where = "$base ${if (op == ADD) '+' else '-'} $number"
        val e = StepExplainer.explain(base, op, number, capacity)
        val expected = if (op == ADD) base + number else base - number
        val max = (1..capacity).fold(1L) { acc, _ -> acc * 10 } - 1
        if (expected !in 0..max) { assertNull(where, e); return }
        assertNotNull(where, e)
        assertEquals(where, expected, e!!.result)
        val beads = Beads(base, capacity)
        var value = base
        for (m in e.moves) {
            assertEquals("$where: hareket öncesi değer", value, m.valueBefore)
            checkMove(m, where)
            m.steps.forEach { beads.apply(it, where) }
            assertEquals("$where: hareket sonrası değer", beads.value(), m.valueAfter)
            value = m.valueAfter
            // 3. Kural kümesi ödevdeki analizle aynı
            assertEquals(
                "$where: kural kümesi",
                RuleAnalysis.rulesOf(op, m.valueBefore, m.col, m.x, capacity),
                StepExplainer.rulesUsed(m),
            )
        }
        assertEquals(where, expected, beads.value())
    }

    @Test
    fun tek_rakam_her_durumda() {
        // Her taban değerinde (0..9999), birler ve onlar sütununa 1..9 ekleme/çıkarma.
        for (base in 0L..9_999L) for (x in 1L..9L) for (op in listOf(ADD, SUB)) {
            verify(base, op, x)
            verify(base, op, x * 10)
        }
    }

    @Test
    fun rastgele_cok_basamakli() {
        val rng = Random(20261028)
        repeat(30_000) {
            val base = rng.nextLong(0, 100_000)
            val number = rng.nextLong(0, 100_000)
            verify(base, if (rng.nextBoolean()) ADD else SUB, number)
        }
    }

    // ── Ortak tablo ────────────────────────────────────────────────────────

    private data class Case(val op: HomeworkOp, val base: Long, val number: Long)

    /** Tablodaki girdiler: her tek rakam durumu (sol sütun bağlamıyla) + sabit tohumlu çok basamaklılar. */
    private fun tableCases(): List<Case> {
        val out = ArrayList<Case>()
        for (op in listOf(ADD, SUB)) for (left in 0L..9L) for (d in 0L..9L) for (x in 1L..9L) {
            out += Case(op, left * 10 + d, x)
        }
        listOf(999L, 9_999L, 99_999L, 49_999L).forEach { out += Case(ADD, it, 1) }
        listOf(1_000L, 10_000L, 50_000L, 0L).forEach { out += Case(SUB, it, 1) }
        val rng = Random(42)
        repeat(300) {
            out += Case(if (rng.nextBoolean()) ADD else SUB, rng.nextLong(0, 100_000), rng.nextLong(0, 100_000))
        }
        return out
    }

    private fun tableFile(): File {
        // Gradle birim testleri modül klasöründe (app/) çalışıyor.
        val candidates = listOf(File("../shared/step_explainer_cases.json"), File("shared/step_explainer_cases.json"))
        return candidates.firstOrNull { it.parentFile?.isDirectory == true } ?: candidates.first()
    }

    @Test
    fun ortak_tablo() {
        val file = tableFile()
        if (System.getenv("GEN_STEP_CASES") == "1") {
            val text = buildString {
                append("[\n")
                append(tableCases().joinToString(",\n") { c ->
                    "{\"op\":\"${c.op.key}\",\"base\":${c.base},\"number\":${c.number}," +
                        "\"plan\":\"${plan(c.base, c.op, c.number)}\"}"
                })
                append("\n]\n")
            }
            file.parentFile.mkdirs()
            file.writeText(text)
            println("Tablo yazıldı: ${file.absolutePath}")
            return
        }
        assertTrue("Tablo yok: ${file.absolutePath} (GEN_STEP_CASES=1 ile üret)", file.isFile)
        val rows = JsonParser.parseString(file.readText()).asJsonArray
        assertTrue(rows.size() > 1000)
        for (row in rows) {
            val o = row.asJsonObject
            val op = HomeworkOp.fromKey(o["op"].asString)!!
            val base = o["base"].asLong
            val number = o["number"].asLong
            assertEquals("$base ${op.key} $number", o["plan"].asString, plan(base, op, number))
        }
    }
}

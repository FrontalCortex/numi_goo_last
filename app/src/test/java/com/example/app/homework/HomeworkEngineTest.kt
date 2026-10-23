package com.example.app.homework

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Ödev soru motoru (HomeworkEngine) testleri.
 *
 * NEDEN BU KADAR AYRINTILI
 *   Kural hatası sahada görünmüyor: öğretmen "yalnızca 5'lik" seçip çocuğun önüne 10'luk bir
 *   işlem düşerse kimse hata almıyor, çocuk yanlış şeyi çalışıyor. Bu yüzden sınıflandırma
 *   bağımsız ikinci bir yazımla (boncuk düzeyinde) karşılaştırılıyor ve üretilen her soru
 *   abaküste yeniden oynatılıp denetleniyor.
 */
class HomeworkEngineTest {

    private val D = AbacusRule.DIRECT
    private val F = AbacusRule.FIVE
    private val T = AbacusRule.TEN
    private val B = AbacusRule.BEAD

    // ── Sınıflandırma ───────────────────────────────────────────────────────

    /**
     * Bağımsız yazım: sütunu boncuk olarak (beşlik inik mi, kaç birlik yukarıda) tutup ders
     * kitabındaki tanımı uyguluyor. AbacusMoves ile bütün (d, x) çiftlerinde aynı sonucu vermeli.
     */
    private fun beadAdd(d: Int, x: Int): AbacusRule {
        val heaven = d >= 5
        val earth = d % 5
        val addHeaven = x >= 5
        val addEarth = x % 5
        val canDirect = !(heaven && addHeaven) && earth + addEarth <= 4
        if (d + x < 10) return if (canDirect) D else F
        // 10 ekle, tamamlayıcıyı çıkar: tamamlayıcıyı boncuk boncuk doğrudan çıkarabiliyor muyuz?
        val comp = 10 - x
        val compHeaven = comp >= 5
        val compEarth = comp % 5
        val directRemove = (!compHeaven || heaven) && compEarth <= earth
        return if (directRemove) T else B
    }

    private fun beadSub(d: Int, x: Int): AbacusRule {
        val heaven = d >= 5
        val earth = d % 5
        if (d >= x) {
            val directRemove = (x < 5 || heaven) && x % 5 <= earth
            return if (directRemove) D else F
        }
        val comp = 10 - x
        val directAdd = !(heaven && comp >= 5) && earth + comp % 5 <= 4
        return if (directAdd) T else B
    }

    @Test
    fun classificationMatchesBeadModel() {
        for (d in 0..9) for (x in 1..9) {
            assertEquals("ekle d=$d x=$x", beadAdd(d, x), AbacusMoves.classifyAdd(d, x))
            assertEquals("çıkar d=$d x=$x", beadSub(d, x), AbacusMoves.classifySub(d, x))
        }
    }

    @Test
    fun textbookExamples() {
        assertEquals(D, AbacusMoves.classifyAdd(1, 1))
        assertEquals(D, AbacusMoves.classifyAdd(2, 6))
        assertEquals(F, AbacusMoves.classifyAdd(3, 4)) // +5 −1
        assertEquals(F, AbacusMoves.classifyAdd(4, 1))
        assertEquals(T, AbacusMoves.classifyAdd(7, 8)) // −2 +10
        assertEquals(T, AbacusMoves.classifyAdd(9, 1))
        // Uygulamanın boncuk kuralı dersindeki işlemler (MathOperationGenerator.generateMathOperationBeadRule).
        for ((d, x) in listOf(8 to 6, 7 to 6, 7 to 7, 6 to 8, 5 to 9, 5 to 6)) {
            assertEquals("$d+$x", B, AbacusMoves.classifyAdd(d, x))
        }
        assertEquals(F, AbacusMoves.classifySub(5, 1)) // −5 +4
        assertEquals(D, AbacusMoves.classifySub(9, 3))
        assertEquals(T, AbacusMoves.classifySub(3, 4)) // −10 +6
        assertEquals(T, AbacusMoves.classifySub(0, 1))
        assertEquals(B, AbacusMoves.classifySub(2, 7)) // −10 +3, 2+3 beşlikle
    }

    // ── Elde/ödünç olmayan kurallar: kesin sınırlar ─────────────────────────

    @Test
    fun noCarryMaxima() {
        // Toplama (gösterilen sayı adedi).
        assertEquals(5, HomeworkEngine.maxNumberCount(HomeworkOp.ADD, setOf(D), 3, true))
        assertEquals(2, HomeworkEngine.maxNumberCount(HomeworkOp.ADD, setOf(F), 3, true))
        assertEquals(9, HomeworkEngine.maxNumberCount(HomeworkOp.ADD, setOf(D, F), 3, true))
        // Çıkarma (çıkarılan sayı adedi, başlangıç hariç).
        assertEquals(5, HomeworkEngine.maxNumberCount(HomeworkOp.SUB, setOf(D), 2, true))
        assertEquals(1, HomeworkEngine.maxNumberCount(HomeworkOp.SUB, setOf(F), 2, true))
        assertEquals(9, HomeworkEngine.maxNumberCount(HomeworkOp.SUB, setOf(D, F), 2, true))
    }

    @Test
    fun userExamples() {
        // "Basamağı 3, yalnızca 5'lik: en fazla 2 sayı, örneğin 132 + 423."
        val state = HomeworkEngine.digitsOf(132, 5)
        assertTrue(HomeworkEngine.applyAdd(state, HomeworkEngine.digitsOf(423, 3), setOf(F)))
        assertEquals(555L, state.foldRight(0L) { d, acc -> acc * 10 + d })
        // Kuralsız + 5'likte 9 sayı → bütün basamaklar 1 (uyarı); önerilen 4.
        assertEquals(List(9) { 1 }, HomeworkEngine.forcedPattern(HomeworkOp.ADD, setOf(D, F), 9))
        assertNull(HomeworkEngine.forcedPattern(HomeworkOp.ADD, setOf(D, F), 8))
        assertEquals(4, HomeworkEngine.recommendedNumberCount(HomeworkOp.ADD, setOf(D, F)))
        val cfg = HomeworkConfig(HomeworkOp.ADD, setOf(D, F), true, 3, numberCount = 9, intervalMs = 1000, questionCount = 5)
        repeat(5) {
            assertEquals(List(9) { 111L }, HomeworkEngine.generateQuestion(cfg, Random(it)).numbers)
        }
        // Elde varken öneri ve zorlama yok.
        assertNull(HomeworkEngine.recommendedNumberCount(HomeworkOp.ADD, setOf(F, T)))
    }

    // ── Üretilen her soru kurala uymalı ─────────────────────────────────────

    /**
     * Soruyu abaküste yeniden oynatır; her sayı seçilen kurallarla işlenebilmeli, sonuç abaküse
     * sığmalı ve negatife düşmemeli. Ayrıca kullanılan kuralları döndürür (kapsam denetimi için).
     */
    private fun replay(cfg: HomeworkConfig, q: HomeworkQuestion): Set<AbacusRule> {
        val cap = HomeworkEngine.capacityDigits(cfg.withAbacus)
        val capMax = HomeworkEngine.pow10(cap) - 1
        val state = HomeworkEngine.digitsOf(q.numbers.first(), cap)
        val used = HashSet<AbacusRule>()
        var value = q.numbers.first()
        for (n in q.numbers.drop(1)) {
            val digits = HomeworkEngine.digitsOf(n, cfg.digits)
            // Kullanılan kurallar: sayının kendi hareketleri (soldan sağa, o anki sütun değerinden).
            val before = state.copyOf()
            for (col in cfg.digits - 1 downTo 0) {
                val x = digits[col]
                if (x == 0) continue
                used += if (cfg.op == HomeworkOp.ADD) AbacusMoves.classifyAdd(before[col], x)
                else AbacusMoves.classifySub(before[col], x)
            }
            val ok = if (cfg.op == HomeworkOp.ADD) HomeworkEngine.applyAdd(state, digits, cfg.rules)
            else HomeworkEngine.applySub(state, digits, cfg.rules)
            assertTrue("kurala uymayan sayı $n, soru ${q.numbers}, $cfg", ok)
            value = if (cfg.op == HomeworkOp.ADD) value + n else value - n
            assertTrue("sınır dışı $value, $cfg", value in 0..capMax)
        }
        assertEquals(value, q.answer)
        return used
    }

    private fun assertShape(cfg: HomeworkConfig, q: HomeworkQuestion) {
        val expectedSize = if (cfg.op == HomeworkOp.ADD) cfg.numberCount else cfg.numberCount + 1
        assertEquals(expectedSize, q.numbers.size)
        val numbers = if (cfg.op == HomeworkOp.SUB) q.numbers.drop(1) else q.numbers
        for (n in numbers) {
            assertEquals("basamak: $n, $cfg", cfg.digits, n.toString().length)
            if (!HomeworkEngine.usesCarry(cfg.rules)) {
                assertFalse("elde yokken ara basamakta 0: $n", n.toString().contains('0'))
            }
        }
        if (cfg.op == HomeworkOp.SUB) {
            assertTrue(q.numbers.first() >= q.numbers.drop(1).sum())
        }
    }

    @Test
    fun everyGeneratedQuestionObeysRules() {
        val rng = Random(42)
        for (op in listOf(HomeworkOp.ADD, HomeworkOp.SUB)) for (abacus in listOf(true, false)) for (mask in 1..15) {
            val rules = AbacusRule.fromMask(mask)
            for (digits in 1..HomeworkEngine.maxDigits(abacus)) {
                val max = HomeworkEngine.maxNumberCount(op, rules, digits, abacus)
                if (max == 0) continue
                val counts = setOf(HomeworkEngine.minNumberCount(op), (HomeworkEngine.minNumberCount(op) + max) / 2, max)
                for (n in counts) {
                    val cfg = HomeworkConfig(op, rules, abacus, digits, numberCount = n, intervalMs = 1000, questionCount = 10)
                    val used = HashSet<AbacusRule>()
                    repeat(8) {
                        val q = HomeworkEngine.generateQuestion(cfg, rng)
                        assertShape(cfg, q)
                        used += replay(cfg, q)
                    }
                    assertTrue("seçilmeyen kural kullanıldı: $used, $cfg", rules.containsAll(used))
                }
            }
        }
    }

    @Test
    fun selectedRulesActuallyAppear() {
        // Seçilen her kural, yeterince soru içinde en az bir kez çalışılmalı (ağırlık sessizce
        // bir kuralı dışarıda bırakmasın).
        val rng = Random(7)
        for (op in listOf(HomeworkOp.ADD, HomeworkOp.SUB)) for (mask in 1..15) {
            val rules = AbacusRule.fromMask(mask)
            val digits = 2
            val max = HomeworkEngine.maxNumberCount(op, rules, digits, true)
            if (max == 0) continue
            val cfg = HomeworkConfig(op, rules, true, digits, numberCount = minOf(max, 5), intervalMs = 1000, questionCount = 10)
            val used = HashSet<AbacusRule>()
            repeat(60) { used += replay(cfg, HomeworkEngine.generateQuestion(cfg, rng)) }
            assertEquals("kapsam ${rules.map { it.key }} $op", rules, used)
        }
    }

    @Test
    fun abacusCapacity() {
        // 5 basamaklı 10 sayı abaküse sığmaz (kullanıcının örneği); abaküssüzde sayılar 8 basamağa kadar.
        val all = setOf(D, F, T, B)
        assertTrue(HomeworkEngine.maxNumberCount(HomeworkOp.ADD, all, 5, true) < 10)
        assertEquals(0, HomeworkEngine.maxNumberCount(HomeworkOp.ADD, all, 6, true))
        assertTrue(HomeworkEngine.maxNumberCount(HomeworkOp.ADD, all, 8, false) >= 2)
    }

    @Test
    fun limitTableValuesAreGeneratable() {
        // Tablodaki her değerde soru gerçekten üretilebilmeli (tablo eski kalırsa yakalanır).
        val rng = Random(99)
        for (op in listOf(HomeworkOp.ADD, HomeworkOp.SUB)) for (abacus in listOf(true, false)) for (mask in 1..15) {
            val rules = AbacusRule.fromMask(mask)
            if (!HomeworkEngine.usesCarry(rules)) continue
            for (digits in 1..HomeworkEngine.maxDigits(abacus)) {
                val max = HomeworkEngine.maxNumberCount(op, rules, digits, abacus)
                if (max == 0) continue
                val cfg = HomeworkConfig(op, rules, abacus, digits, numberCount = max, intervalMs = 1000, questionCount = 1)
                repeat(5) { assertTrue("$cfg", HomeworkEngine.probeGenerate(cfg, rng)) }
            }
        }
    }

    @Test
    fun multiplication() {
        val cfg = HomeworkConfig(HomeworkOp.MUL, withAbacus = true, digits = 2, digits2 = 1, questionCount = 5)
        val rng = Random(3)
        repeat(200) {
            val q = HomeworkEngine.generateQuestion(cfg, rng)
            assertEquals(2, q.numbers[0].toString().length)
            assertEquals(1, q.numbers[1].toString().length)
            assertTrue(q.numbers[1] >= 2)
            assertEquals(q.numbers[0] * q.numbers[1], q.answer)
        }
        // Abaküs 5 sütun: çarpım 5 basamağı geçmesin.
        assertNotNull(HomeworkEngine.validate(cfg.copy(digits = 3, digits2 = 3)))
        assertNull(HomeworkEngine.validate(cfg.copy(digits = 3, digits2 = 3, withAbacus = false)))
    }

    @Test
    fun turkishAccusativeSuffix() {
        // "12 öğrenciden 8'i bitirdi": ek, sayının okunuşunun son kelimesine göre.
        val expected = mapOf(
            1 to "1'i", 2 to "2'si", 3 to "3'ü", 4 to "4'ü", 5 to "5'i", 6 to "6'sı", 7 to "7'si",
            8 to "8'i", 9 to "9'u", 10 to "10'u", 12 to "12'si", 20 to "20'si", 30 to "30'u",
            40 to "40'ı", 50 to "50'si", 60 to "60'ı", 0 to "0'ı",
        )
        expected.forEach { (n, s) -> assertEquals(s, HomeworkText.withAccusative(n)) }
    }

    @Test
    fun blocksOfTen() {
        assertEquals(10, HomeworkEngine.blockCount(100))
        assertEquals(3, HomeworkEngine.blockCount(25))
        assertEquals(5, HomeworkEngine.blockSize(25, 2))
        assertEquals(1, HomeworkEngine.blockCount(1))
        val cfg = HomeworkConfig(HomeworkOp.ADD, setOf(D, F), true, 2, numberCount = 3, intervalMs = 1000, questionCount = 25)
        assertEquals(5, HomeworkEngine.generateBlock(cfg, 2, Random(1)).size)
    }
}

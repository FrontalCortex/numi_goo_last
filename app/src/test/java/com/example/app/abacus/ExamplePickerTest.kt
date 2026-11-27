package com.example.app.abacus

import com.example.app.homework.AbacusRule
import com.example.app.homework.HomeworkOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [ExamplePicker]: öğreticide "Bir örnek ver" dersin kuralını gerçekten kullanan bir işlem seçmeli. */
class ExamplePickerTest {

    @Test
    fun her_kuralin_ornegi_o_kurali_kullanir() {
        for (key in listOf("add_direct", "add_five", "add_ten", "add_bead", "sub_direct", "sub_five", "sub_ten", "sub_bead")) {
            val list = ExamplePicker.candidates(key)
            assertTrue("$key için örnek yok", list.isNotEmpty())
            val rule = AbacusRule.fromKey(key.substringAfter('_'))
            for ((base, n) in list) {
                val op = if (n < 0) HomeworkOp.SUB else HomeworkOp.ADD
                assertEquals(key, if (key.startsWith("add")) HomeworkOp.ADD else HomeworkOp.SUB, op)
                val m = StepExplainer.explain(base.toLong(), op, Math.abs(n).toLong())!!.moves.single()
                assertEquals("$base $n", rule, m.rule)
            }
        }
    }

    @Test
    fun farkli_tohum_farkli_ornek() {
        assertNotEquals(ExamplePicker.pick("add_ten", 0), ExamplePicker.pick("add_ten", 1))
        assertNull(ExamplePicker.pick("carpma", 0))
    }

    /** Öğreticideki "öğrenilen kurallar" (SobiCurriculum ile aynı sıra). */
    private val order = listOf("add_direct", "add_five", "add_ten", "add_bead", "sub_direct", "sub_five", "sub_ten", "sub_bead")
    private fun taughtUpTo(key: String) = order.subList(0, order.indexOf(key) + 1)

    @Test
    fun zorluk_duzeyleri_kurali_ve_basamaklari_tutar() {
        for (key in order) {
            for (level in 2..ExamplePicker.MAX_LEVEL) {
                for (seed in 0..4) {
                    val (base, n) = ExamplePicker.pick(key, seed, level, taughtUpTo(key)) ?: error("$key düzey $level örnek yok")
                    val op = if (n < 0) HomeworkOp.SUB else HomeworkOp.ADD
                    val e = StepExplainer.explain(base.toLong(), op, Math.abs(n).toLong())!!
                    val used = e.moves.flatMap { StepExplainer.rulesUsed(it) }.toSet()
                    val prefix = if (op == HomeworkOp.ADD) "add_" else "sub_"
                    assertTrue("$key $level $base $n: kural yok", AbacusRule.fromKey(key.substringAfter('_')) in used)
                    assertTrue("$key $level $base $n: öğrenilmemiş kural", used.all { (prefix + it.key) in taughtUpTo(key) })
                    // Düzey bulunamayınca bir alt düzeye iner; en azından 2. düzeyin basamağı.
                    assertTrue("$key $level $base", base >= 10)
                }
            }
        }
    }

    @Test
    fun ucuncu_ve_dorduncu_duzey_kurali_iki_sutunda_kullanir() {
        val (base, n) = ExamplePicker.pick("add_ten", 3, 4, taughtUpTo("add_ten"))!!
        assertTrue("$base", base in 100..999)
        assertTrue("$n", n in 100..999)
        val e = StepExplainer.explain(base.toLong(), HomeworkOp.ADD, n.toLong())!!
        assertTrue(e.moves.count { it.rule != AbacusRule.DIRECT } >= 2)
    }

    @Test
    fun besinci_duzey_dort_basamakli_ve_abakuse_sigar() {
        for (key in order) {
            val (base, n) = ExamplePicker.pick(key, 7, 5, taughtUpTo(key)) ?: error("$key düzey 5 örnek yok")
            assertTrue("$key $base", base in 1000..9999)
            assertTrue("$key $n", Math.abs(n) in 1000..9999)
            assertTrue("$key sonuç 5 sütuna sığmalı", base + n in 0..99999)
        }
    }
}

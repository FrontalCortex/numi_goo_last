// Android app/src/test/java/com/example/app/abacus/ExamplePickerTest.kt'in kopyası.
package com.sorobit.shared.sobianaliz

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExamplePickerTest {

    @Test
    fun her_kuralin_ornegi_o_kurali_kullanir() {
        for (key in listOf("add_direct", "add_five", "add_ten", "add_bead", "sub_direct", "sub_five", "sub_ten", "sub_bead")) {
            val list = ExamplePicker.candidates(key)
            assertTrue(list.isNotEmpty(), "$key için örnek yok")
            val rule = AbacusRule.fromKey(key.substringAfter('_'))
            for ((base, n) in list) {
                val op = if (n < 0) HomeworkOp.SUB else HomeworkOp.ADD
                assertEquals(if (key.startsWith("add")) HomeworkOp.ADD else HomeworkOp.SUB, op, key)
                val m = StepExplainer.explain(base.toLong(), op, abs(n).toLong())!!.moves.single()
                assertEquals(rule, m.rule, "$base $n")
            }
        }
    }

    @Test
    fun farkli_tohum_farkli_ornek() {
        assertNotEquals(ExamplePicker.pick("add_ten", 0), ExamplePicker.pick("add_ten", 1))
        assertNull(ExamplePicker.pick("carpma", 0))
        // Negatif tohum da geçerli (Math.floorMod).
        assertTrue(ExamplePicker.pick("add_ten", -5) != null)
    }

    private val order = listOf("add_direct", "add_five", "add_ten", "add_bead", "sub_direct", "sub_five", "sub_ten", "sub_bead")
    private fun taughtUpTo(key: String) = order.subList(0, order.indexOf(key) + 1)

    @Test
    fun zorluk_duzeyleri_kurali_ve_basamaklari_tutar() {
        for (key in order) {
            for (level in 2..ExamplePicker.MAX_LEVEL) {
                for (seed in 0..4) {
                    val (base, n) = ExamplePicker.pick(key, seed, level, taughtUpTo(key)) ?: error("$key düzey $level örnek yok")
                    val op = if (n < 0) HomeworkOp.SUB else HomeworkOp.ADD
                    val e = StepExplainer.explain(base.toLong(), op, abs(n).toLong())!!
                    val used = e.moves.flatMap { StepExplainer.rulesUsed(it) }.toSet()
                    val prefix = if (op == HomeworkOp.ADD) "add_" else "sub_"
                    assertTrue(AbacusRule.fromKey(key.substringAfter('_')) in used, "$key $level $base $n: kural yok")
                    assertTrue(used.all { (prefix + it.key) in taughtUpTo(key) }, "$key $level $base $n: öğrenilmemiş kural")
                    assertTrue(base >= 10, "$key $level $base")
                }
            }
        }
    }

    @Test
    fun ucuncu_ve_dorduncu_duzey_kurali_iki_sutunda_kullanir() {
        val (base, n) = ExamplePicker.pick("add_ten", 3, 4, taughtUpTo("add_ten"))!!
        assertTrue(base in 100..999, "$base")
        assertTrue(n in 100..999, "$n")
        val e = StepExplainer.explain(base.toLong(), HomeworkOp.ADD, n.toLong())!!
        assertTrue(e.moves.count { it.rule != AbacusRule.DIRECT } >= 2)
    }

    @Test
    fun besinci_duzey_dort_basamakli_ve_abakuse_sigar() {
        for (key in order) {
            val (base, n) = ExamplePicker.pick(key, 7, 5, taughtUpTo(key)) ?: error("$key düzey 5 örnek yok")
            assertTrue(base in 1000..9999, "$key $base")
            assertTrue(abs(n) in 1000..9999, "$key $n")
            assertTrue(base + n in 0..99999, "$key sonuç 5 sütuna sığmalı")
        }
    }
}

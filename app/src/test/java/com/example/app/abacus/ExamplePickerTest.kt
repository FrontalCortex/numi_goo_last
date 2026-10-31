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
}

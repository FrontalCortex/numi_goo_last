package com.example.app.abacus

import com.example.app.homework.AbacusRule
import com.example.app.homework.HomeworkOp

/**
 * [SubStepExplainer]'ın Arapçası: aynı mantık, cümleler Arap ülkelerindeki الحساب الذهني
 * kurslarının sözleriyle ("قاعدة الأصدقاء الصغار/الكبار", "أضِف 5 واطرح صديقه";
 * docs/CEVIRI_SOZLUGU_AR.md). Sütun "عمود الآحاد، عمود العشرات…" diye geçiyor.
 *
 * Boncuklar öğretici çevirisindeki gibi "خرزة الخمسة" ve "خرزات الواحد" (birlik boncuklar).
 * Sayı + ad uyumu: boncuk sayısı [lowerBeads] ile, "N من خرزات الواحد" kalıbında (her sayıyla
 * doğru); öbür sayılar tek başına ("يُضاف 7").
 * Hitap birinci çoğul ("نضيفها") ve edilgen fiille: cinsiyetsiz.
 *
 * "Solundaki sütun" Arapçada da sol: abaküs sağdan sola dönmüyor (bkz. RtlSupport).
 */
internal object SubStepExplainerAr {

    fun explain(s: StepExplainer.Step, m: StepExplainer.ColumnMove): String {
        val phrase = "«${MistakeAnalyzer.stepPhrase(s, m)}»"
        val col = MistakeAnalyzer.columnName(m.col)
        return when (s.role) {
            StepExplainer.Role.DIRECT ->
                "$phrase: في عمود $col مكان كافٍ، لذلك ${if (s.upper + s.lower > 0) "تُضاف" else "تُطرح"} ${beads(s)} مباشرة. لا نحتاج إلى قاعدة."
            StepExplainer.Role.FIVE -> if (s.upper > 0) {
                "$phrase: في عمود $col تنزل خرزة الخمسة العلوية إلى العارضة. هذا يعني إضافة 5 إلى العمود."
            } else {
                "$phrase: ترتفع خرزة الخمسة في عمود $col إلى الأعلى. هذا يعني طرح 5 من العمود."
            }
            StepExplainer.Role.FRIEND -> {
                val n = Math.abs(s.lower)
                "$phrase: الأصدقاء الصغار عددان مجموعهما 5: ${5 - n} و $n. " +
                    "${if (s.lower > 0) "تُضاف" else "تُطرح"} ${lowerBeads(n)} ${if (s.lower > 0) "إلى" else "من"} عمود $col."
            }
            StepExplainer.Role.BIG_FRIEND -> {
                val n = Math.abs(s.upper) * 5 + Math.abs(s.lower)
                val add = s.upper + s.lower > 0
                "$phrase: الأصدقاء الكبار عددان مجموعهما 10: ${10 - n} و $n. " +
                    "${if (add) "يُضاف" else "يُطرح"} $n ${if (add) "إلى" else "من"} عمود $col${beadDetail(s)}."
            }
            StepExplainer.Role.BEADS -> "$phrase: تُضاف ${lowerBeads(s.lower)} إلى عمود $col."
            StepExplainer.Role.TEN -> {
                val left = MistakeAnalyzer.columnName(s.col)
                val head = if (m.op == HomeworkOp.ADD) {
                    "$phrase: إضافة 10 إلى عمود $col تعني إضافة خرزة واحدة إلى عمود $left الذي على يساره " +
                        "(خرزة واحدة في العمود الأيسر تساوي 10 في هذا العمود)."
                } else {
                    "$phrase: طرح 10 من عمود $col يعني طرح خرزة واحدة من عمود $left الذي على يساره " +
                        "(خرزة واحدة في العمود الأيسر تساوي 10 في هذا العمود)."
                }
                head + chain(s.carry)
            }
        }
    }

    fun whyRule(m: StepExplainer.ColumnMove): String {
        val d = m.digitBefore
        val x = m.x
        val where = "في عمود ${MistakeAnalyzer.columnName(m.col)} الرقم $d"
        return if (m.op == HomeworkOp.ADD) when (m.rule) {
            AbacusRule.DIRECT -> "$where؛ توجد خرزات حرّة تكفي لإضافة $x، لذلك نضيفها مباشرة."
            AbacusRule.FIVE -> "$where؛ لا يمكن إضافة $x من خرزات الواحد، فالحرّ منها ${4 - d % 5} فقط. " +
                "إضافة $x مثل إضافة 5 ثم طرح ${5 - x}. لذلك: أضِف 5 واطرح صديقه ${5 - x}."
            AbacusRule.TEN -> "$where؛ إذا أضفنا $x يصبح ${d + x}، والعمود يحمل 9 على الأكثر. " +
                "إضافة $x مثل إضافة 10 ثم طرح ${10 - x}. لذلك: أضِف 10 واطرح صديقه الكبير ${10 - x}."
            AbacusRule.BEAD -> "$where؛ إذا أضفنا $x يصبح ${d + x}، والعمود يحمل 9 على الأكثر. وقاعدة الأصدقاء الكبار " +
                "لا تنجح مباشرة أيضًا: يجب طرح الصديق الكبير ${10 - x}، لكن في الأسفل ${d % 5} فقط من خرزات الواحد. " +
                "وطرح ${10 - x} مثل طرح 5 ثم إضافة ${x - 5}. لذلك: أضِف ${MistakeAnalyzer.beadsAr(x - 5)} ثم اطرح 5 ثم أضِف 10."
        } else when (m.rule) {
            AbacusRule.DIRECT -> "$where؛ توجد خرزات تكفي لطرح $x، لذلك نطرحها مباشرة."
            AbacusRule.FIVE -> "$where؛ لا يمكن طرح $x من خرزات الواحد، ففيها ${d % 5} فقط. " +
                "طرح $x مثل طرح 5 ثم إضافة ${5 - x}. لذلك: اطرح 5 وأضِف صديقه ${5 - x}."
            AbacusRule.TEN -> "$where؛ لا يمكن طرح $x من $d. " +
                "طرح $x مثل طرح 10 ثم إضافة ${10 - x}. لذلك: اطرح 10 وأضِف صديقه ${10 - x}."
            AbacusRule.BEAD -> "$where؛ لا يمكن طرح $x من $d. وقاعدة الأصدقاء الكبار لا تنجح مباشرة أيضًا: يجب إضافة " +
                "صديقه ${10 - x}، لكن يمكن إضافة ${4 - d % 5} فقط من خرزات الواحد. وإضافة ${10 - x} مثل إضافة 5 " +
                "ثم طرح ${x - 5}. لذلك: اطرح 10 ثم أضِف 5 ثم اطرح صديقه ${x - 5}."
        }
    }

    /** Elde zinciri: sol sütun 1'i doğrudan alamıyorsa oradaki kural; o da sola taşıyorsa bir sonraki sütun. */
    private fun chain(c: StepExplainer.ColumnMove?): String {
        if (c == null || c.rule == AbacusRule.DIRECT) return ""
        val add = c.op == HomeworkOp.ADD
        val col = MistakeAnalyzer.columnName(c.col)
        val d = c.digitBefore
        val state = when {
            add && d == 9 -> "لكن في عمود $col الرقم 9، والعمود ممتلئ."
            add -> "لكن في عمود $col الرقم $d، فلا يمكن إضافة خرزة أخرى من خرزات الواحد."
            d == 0 -> "لكن عمود $col ليس فيه خرزات."
            else -> "لكن في عمود $col خرزة الخمسة فقط."
        }
        val rule = when (c.rule) {
            AbacusRule.FIVE -> "بقاعدة الأصدقاء الصغار"
            AbacusRule.BEAD -> "بالقاعدة المركّبة"
            else -> "بقاعدة الأصدقاء الكبار"
        }
        val parts = c.steps.joinToString("، ") { st ->
            when (st.role) {
                StepExplainer.Role.TEN -> {
                    val left = MistakeAnalyzer.columnName(st.col)
                    if (add) "تُضاف خرزة واحدة إلى عمود $left" else "تُطرح خرزة واحدة من عمود $left"
                }
                StepExplainer.Role.BIG_FRIEND -> {
                    val n = Math.abs(st.upper) * 5 + Math.abs(st.lower)
                    if (add) "يُطرح $n من عمود $col" else "يُضاف $n إلى عمود $col"
                }
                else -> MistakeAnalyzer.stepPhrase(st, c)
            }
        }
        val next = c.steps.filter { it.role == StepExplainer.Role.TEN }.joinToString("") { chain(it.carry) }
        return " $state ${if (add) "وإضافة 1 هناك تكون أيضًا" else "وطرح 1 من هناك يكون أيضًا"} $rule: $parts.$next"
    }

    /** Fiilden sonra özne (merfu): خرزة الخمسة و… */
    private fun beads(s: StepExplainer.Step): String {
        val lower = Math.abs(s.lower)
        return when {
            s.upper != 0 && lower != 0 -> "خرزة الخمسة و${lowerBeads(lower)}"
            s.upper != 0 -> "خرزة الخمسة"
            else -> lowerBeads(lower)
        }
    }

    private fun beadDetail(s: StepExplainer.Step): String {
        if (s.upper == 0 || s.lower == 0) return ""
        val upper = if (s.upper > 0) "تنزل خرزة الخمسة" else "ترتفع خرزة الخمسة"
        val lower = "${if (s.lower > 0) "تُضاف" else "تُطرح"} ${lowerBeads(Math.abs(s.lower))}"
        return " ($upper، $lower)"
    }

    /** "N birlik boncuk", özne: خرزة واحدة / خرزتان / N من خرزات الواحد (partitif, her sayıyla doğru). */
    private fun lowerBeads(n: Int): String = when (n) {
        1 -> "خرزة واحدة من خرزات الواحد"
        2 -> "خرزتان من خرزات الواحد"
        else -> "$n من خرزات الواحد"
    }
}

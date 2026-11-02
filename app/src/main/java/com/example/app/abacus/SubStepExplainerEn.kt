package com.example.app.abacus

import com.example.app.homework.AbacusRule
import com.example.app.homework.HomeworkOp

/**
 * [SubStepExplainer]'ın İngilizcesi: aynı mantık, cümleler "small/big/mixed friends" sözleriyle
 * (docs/CEVIRI_SOZLUGU.md). Türkçede ek uyumu için ayrı yazılmış yardımcılar burada gerekmiyor.
 */
internal object SubStepExplainerEn {

    fun explain(s: StepExplainer.Step, m: StepExplainer.ColumnMove): String {
        val phrase = "“${MistakeAnalyzer.stepPhrase(s, m)}”"
        val col = MistakeAnalyzer.columnName(m.col)
        return when (s.role) {
            StepExplainer.Role.DIRECT ->
                "$phrase: There\'s room in the $col, so ${beads(s)} ${if (s.upper + s.lower > 0) "go in" else "come out"} directly. No rule needed."
            StepExplainer.Role.FIVE -> if (s.upper > 0) {
                "$phrase: In the $col column, the upper five-bead moves down to the bar. That means adding 5 to the column."
            } else {
                "$phrase: The five-bead in the $col column moves back up. That means taking 5 away from the column."
            }
            StepExplainer.Role.FRIEND -> {
                val n = Math.abs(s.lower)
                "$phrase: Friends are numbers that make 5 together: ${5 - n} and $n. $n one-beads " +
                    "${if (s.lower > 0) "go into" else "come out of"} the $col column from below."
            }
            StepExplainer.Role.BIG_FRIEND -> {
                val n = Math.abs(s.upper) * 5 + Math.abs(s.lower)
                val add = s.upper + s.lower > 0
                "$phrase: Big friends are numbers that make 10 together: ${10 - n} and $n. $n " +
                    "${if (add) "goes into" else "comes out of"} the $col column${beadDetail(s)}."
            }
            StepExplainer.Role.BEADS -> "$phrase: ${s.lower} one-beads go into the $col column from below."
            StepExplainer.Role.TEN -> {
                val left = MistakeAnalyzer.columnName(s.col)
                val head = if (m.op == HomeworkOp.ADD) {
                    "$phrase: Adding 10 to the $col column means adding 1 bead to the $left column on its left " +
                        "(1 bead in the left column is worth 10 in this one)."
                } else {
                    "$phrase: Taking 10 away from the $col column means taking 1 bead away from the $left column on its left " +
                        "(1 bead in the left column is worth 10 in this one)."
                }
                head + chain(s.carry)
            }
        }
    }

    fun whyRule(m: StepExplainer.ColumnMove): String {
        val d = m.digitBefore
        val x = m.x
        val where = "The ${MistakeAnalyzer.columnName(m.col)} column has $d"
        return if (m.op == HomeworkOp.ADD) when (m.rule) {
            AbacusRule.DIRECT -> "$where; there are enough free beads for $x, so it goes in directly."
            AbacusRule.FIVE -> "$where; $x beads can\'t go in from below, only ${4 - d % 5} one-beads are free. " +
                "Adding $x is the same as adding 5 and taking away ${5 - x}. So: add 5, take away its friend ${5 - x}."
            AbacusRule.TEN -> "$where; adding $x makes ${d + x}, but a column holds at most 9. " +
                "Adding $x is the same as adding 10 and taking away ${10 - x}. So: add 10, take away its big friend ${10 - x}."
            AbacusRule.BEAD -> "$where; adding $x makes ${d + x}, but a column holds at most 9. Big friends doesn\'t work " +
                "directly either: the big friend ${10 - x} should come out, but there are only ${d % 5} one-beads below. " +
                "Taking away ${10 - x} is the same as taking away 5 and adding ${x - 5}. So: add ${x - 5} beads, take away 5, add 10."
        } else when (m.rule) {
            AbacusRule.DIRECT -> "$where; there are enough beads for $x, so it comes out directly."
            AbacusRule.FIVE -> "$where; $x beads can\'t come out from below, there are only ${d % 5} one-beads. " +
                "Taking away $x is the same as taking away 5 and adding ${5 - x}. So: take away 5, add its friend ${5 - x}."
            AbacusRule.TEN -> "$where; you can\'t take $x out of $d. " +
                "Taking away $x is the same as taking away 10 and adding ${10 - x}. So: take away 10, add its friend ${10 - x}."
            AbacusRule.BEAD -> "$where; you can\'t take $x out of $d. Big friends doesn\'t work directly either: its friend " +
                "${10 - x} should go in, but only ${4 - d % 5} beads can go in from below. Adding ${10 - x} is the same as " +
                "adding 5 and taking away ${x - 5}. So: take away 10, add 5, take away its friend ${x - 5}."
        }
    }

    /** Elde zinciri: sol sütun 1'i doğrudan alamıyorsa oradaki kural; o da sola taşıyorsa bir sonraki sütun. */
    private fun chain(c: StepExplainer.ColumnMove?): String {
        if (c == null || c.rule == AbacusRule.DIRECT) return ""
        val add = c.op == HomeworkOp.ADD
        val col = MistakeAnalyzer.columnName(c.col)
        val d = c.digitBefore
        val state = when {
            add && d == 9 -> "But the $col column has 9, it\'s full."
            add -> "But the $col column has $d, so 1 more bead can\'t go in from below."
            d == 0 -> "But the $col column has no beads."
            else -> "But the $col column only has the five-bead."
        }
        val rule = when (c.rule) {
            AbacusRule.FIVE -> "small friends"
            AbacusRule.BEAD -> "mixed friends"
            else -> "big friends"
        }
        val parts = c.steps.joinToString(", ") { st ->
            when (st.role) {
                StepExplainer.Role.TEN -> {
                    val left = MistakeAnalyzer.columnName(st.col)
                    if (add) "1 bead goes into the $left" else "1 bead comes out of the $left"
                }
                StepExplainer.Role.BIG_FRIEND -> {
                    val n = Math.abs(st.upper) * 5 + Math.abs(st.lower)
                    if (add) "$n comes out of the $col" else "$n goes into the $col"
                }
                else -> MistakeAnalyzer.stepPhrase(st, c)
            }
        }
        val next = c.steps.filter { it.role == StepExplainer.Role.TEN }.joinToString("") { chain(it.carry) }
        return " $state ${if (add) "Adding 1 there" else "Taking 1 away there"} also uses $rule: $parts.$next"
    }

    private fun beads(s: StepExplainer.Step): String {
        val lower = Math.abs(s.lower)
        return when {
            s.upper != 0 && lower != 0 -> "the five-bead and $lower one-beads"
            s.upper != 0 -> "the five-bead"
            else -> "$lower one-beads"
        }
    }

    private fun beadDetail(s: StepExplainer.Step): String {
        if (s.upper == 0 || s.lower == 0) return ""
        val upper = if (s.upper > 0) "the five-bead moves down" else "the five-bead moves up"
        val lower = "${Math.abs(s.lower)} one-beads ${if (s.lower > 0) "go in" else "come out"}"
        return " ($upper, $lower)"
    }
}

package com.example.app.abacus

import com.example.app.homework.AbacusRule
import com.example.app.homework.HomeworkOp

/**
 * [SubStepExplainer]'ın Endonezcesi: aynı mantık, cümleler Endonezya'daki sempoa kurslarının
 * sözleriyle ("rumus teman kecil/besar/gabungan", "tambah 5, kurangi temannya";
 * docs/CEVIRI_SOZLUGU_ID.md). Sütun adları "kolom satuan, kolom puluhan…" diye geçiyor; Türkçedeki
 * ek uyumu yardımcıları burada da gerekmiyor.
 */
internal object SubStepExplainerId {

    fun explain(s: StepExplainer.Step, m: StepExplainer.ColumnMove): String {
        val phrase = "“${MistakeAnalyzer.stepPhrase(s, m)}”"
        val col = MistakeAnalyzer.columnName(m.col)
        return when (s.role) {
            StepExplainer.Role.DIRECT ->
                "$phrase: Di kolom $col masih ada tempat, jadi ${beads(s)} langsung ${if (s.upper + s.lower > 0) "masuk" else "keluar"}. Tidak perlu rumus."
            StepExplainer.Role.FIVE -> if (s.upper > 0) {
                "$phrase: Di kolom $col, manik lima di atas turun ke batang. Artinya menambah 5 ke kolom itu."
            } else {
                "$phrase: Manik lima di kolom $col naik kembali. Artinya mengurangi 5 dari kolom itu."
            }
            StepExplainer.Role.FRIEND -> {
                val n = Math.abs(s.lower)
                "$phrase: Teman kecil adalah dua angka yang jumlahnya 5: ${5 - n} dan $n. $n manik satu " +
                    "${if (s.lower > 0) "masuk ke" else "keluar dari"} kolom $col dari bawah."
            }
            StepExplainer.Role.BIG_FRIEND -> {
                val n = Math.abs(s.upper) * 5 + Math.abs(s.lower)
                val add = s.upper + s.lower > 0
                "$phrase: Teman besar adalah dua angka yang jumlahnya 10: ${10 - n} dan $n. $n " +
                    "${if (add) "masuk ke" else "keluar dari"} kolom $col${beadDetail(s)}."
            }
            StepExplainer.Role.BEADS -> "$phrase: ${s.lower} manik satu masuk ke kolom $col dari bawah."
            StepExplainer.Role.TEN -> {
                val left = MistakeAnalyzer.columnName(s.col)
                val head = if (m.op == HomeworkOp.ADD) {
                    "$phrase: Menambah 10 ke kolom $col berarti menambah 1 manik ke kolom $left di sebelah kirinya " +
                        "(1 manik di kolom kiri bernilai 10 di kolom ini)."
                } else {
                    "$phrase: Mengurangi 10 dari kolom $col berarti mengurangi 1 manik dari kolom $left di sebelah kirinya " +
                        "(1 manik di kolom kiri bernilai 10 di kolom ini)."
                }
                head + chain(s.carry)
            }
        }
    }

    fun whyRule(m: StepExplainer.ColumnMove): String {
        val d = m.digitBefore
        val x = m.x
        val where = "Kolom ${MistakeAnalyzer.columnName(m.col)} berisi $d"
        return if (m.op == HomeworkOp.ADD) when (m.rule) {
            AbacusRule.DIRECT -> "$where; masih ada cukup manik kosong untuk $x, jadi langsung masuk."
            AbacusRule.FIVE -> "$where; $x manik tidak bisa masuk dari bawah, hanya ada ${4 - d % 5} manik satu yang kosong. " +
                "Menambah $x sama dengan menambah 5 lalu mengurangi ${5 - x}. Jadi: tambah 5, kurangi temannya ${5 - x}."
            AbacusRule.TEN -> "$where; kalau ditambah $x jadi ${d + x}, padahal satu kolom paling banyak 9. " +
                "Menambah $x sama dengan menambah 10 lalu mengurangi ${10 - x}. Jadi: tambah 10, kurangi teman besarnya ${10 - x}."
            AbacusRule.BEAD -> "$where; kalau ditambah $x jadi ${d + x}, padahal satu kolom paling banyak 9. Rumus teman besar " +
                "juga tidak bisa langsung: teman besarnya ${10 - x} harus keluar, tetapi di bawah hanya ada ${d % 5} manik satu. " +
                "Mengurangi ${10 - x} sama dengan mengurangi 5 lalu menambah ${x - 5}. Jadi: tambah ${x - 5} manik, kurangi 5, tambah 10."
        } else when (m.rule) {
            AbacusRule.DIRECT -> "$where; manik untuk $x cukup, jadi langsung keluar."
            AbacusRule.FIVE -> "$where; $x manik tidak bisa keluar dari bawah, hanya ada ${d % 5} manik satu. " +
                "Mengurangi $x sama dengan mengurangi 5 lalu menambah ${5 - x}. Jadi: kurangi 5, tambah temannya ${5 - x}."
            AbacusRule.TEN -> "$where; $x tidak bisa diambil dari $d. " +
                "Mengurangi $x sama dengan mengurangi 10 lalu menambah ${10 - x}. Jadi: kurangi 10, tambah temannya ${10 - x}."
            AbacusRule.BEAD -> "$where; $x tidak bisa diambil dari $d. Rumus teman besar juga tidak bisa langsung: temannya " +
                "${10 - x} harus masuk, tetapi dari bawah hanya ${4 - d % 5} manik yang bisa masuk. Menambah ${10 - x} sama dengan " +
                "menambah 5 lalu mengurangi ${x - 5}. Jadi: kurangi 10, tambah 5, kurangi temannya ${x - 5}."
        }
    }

    /** Elde zinciri: sol sütun 1'i doğrudan alamıyorsa oradaki kural; o da sola taşıyorsa bir sonraki sütun. */
    private fun chain(c: StepExplainer.ColumnMove?): String {
        if (c == null || c.rule == AbacusRule.DIRECT) return ""
        val add = c.op == HomeworkOp.ADD
        val col = MistakeAnalyzer.columnName(c.col)
        val d = c.digitBefore
        val state = when {
            add && d == 9 -> "Tetapi kolom $col berisi 9, sudah penuh."
            add -> "Tetapi kolom $col berisi $d, jadi 1 manik lagi tidak bisa masuk dari bawah."
            d == 0 -> "Tetapi kolom $col tidak ada maniknya."
            else -> "Tetapi di kolom $col hanya ada manik lima."
        }
        val rule = when (c.rule) {
            AbacusRule.FIVE -> "rumus teman kecil"
            AbacusRule.BEAD -> "rumus gabungan"
            else -> "rumus teman besar"
        }
        val parts = c.steps.joinToString(", ") { st ->
            when (st.role) {
                StepExplainer.Role.TEN -> {
                    val left = MistakeAnalyzer.columnName(st.col)
                    if (add) "1 manik masuk ke kolom $left" else "1 manik keluar dari kolom $left"
                }
                StepExplainer.Role.BIG_FRIEND -> {
                    val n = Math.abs(st.upper) * 5 + Math.abs(st.lower)
                    if (add) "$n keluar dari kolom $col" else "$n masuk ke kolom $col"
                }
                else -> MistakeAnalyzer.stepPhrase(st, c)
            }
        }
        val next = c.steps.filter { it.role == StepExplainer.Role.TEN }.joinToString("") { chain(it.carry) }
        return " $state ${if (add) "Menambah 1 di sana" else "Mengurangi 1 di sana"} juga memakai $rule: $parts.$next"
    }

    private fun beads(s: StepExplainer.Step): String {
        val lower = Math.abs(s.lower)
        return when {
            s.upper != 0 && lower != 0 -> "manik lima dan $lower manik satu"
            s.upper != 0 -> "manik lima"
            else -> "$lower manik satu"
        }
    }

    private fun beadDetail(s: StepExplainer.Step): String {
        if (s.upper == 0 || s.lower == 0) return ""
        val upper = if (s.upper > 0) "manik lima turun" else "manik lima naik"
        val lower = "${Math.abs(s.lower)} manik satu ${if (s.lower > 0) "masuk" else "keluar"}"
        return " ($upper, $lower)"
    }
}

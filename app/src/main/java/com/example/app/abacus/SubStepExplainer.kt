package com.example.app.abacus

import com.example.app.AppLanguage
import com.example.app.homework.AbacusRule
import com.example.app.homework.HomeworkOp

/**
 * ALT ADIM ANLATICISI — bir hamlenin tek parçası ("2 boncuk gelir", "5 gider", "10 gelir") ne demek;
 * "Sobi ile analiz et" ekranında parçaya dokununca (kullanıcı kararı 01.11.2026).
 *
 * NEDEN
 *   Çocuk "10 gelir adımını anlamadım" diye yazınca yapay zekâ geri soru sordu; asıl zor yeri (sol
 *   sütun doluysa orada da 10'luk kural) atladı. Bu sorular kural motoruyla kesin cevaplanabiliyor:
 *   ücretsiz (ileride kredi harcamaz), anında ve her seferinde aynı. Sobi'ye yalnızca gerçekten
 *   serbest sorular kalıyor.
 *
 * Sözler derslerle ve [MistakeAnalyzer.stepPhrase] ile aynı ("5 gelir, kardeşi gider", "10 gelir,
 * büyük kardeşi gider"). Elde zinciri sütun sütun söyleniyor: "Ama yüzlerde 9 var, sütun dolu. Oraya
 * 1 eklemek de 10'luk kuralla olur: binlere 1 boncuk gelir, yüzlerdeki 9 gider."
 */
object SubStepExplainer {

    /** Parçanın balon etiketi: “10 gelir” ne demek? */
    fun question(s: StepExplainer.Step, m: StepExplainer.ColumnMove): String = when (AppLanguage.textLanguage()) {
        AppLanguage.ENGLISH -> "What does “${MistakeAnalyzer.stepPhrase(s, m)}” mean?"
        AppLanguage.INDONESIAN -> "Apa arti “${MistakeAnalyzer.stepPhrase(s, m)}”?"
        AppLanguage.ARABIC -> "ماذا تعني «${MistakeAnalyzer.stepPhrase(s, m)}»؟"
        else -> "“${MistakeAnalyzer.stepPhrase(s, m)}” ne demek?"
    }

    /** Hamlenin parçaları balona değer mi: kuralsız tek parça kendini anlatıyor. */
    fun hasParts(m: StepExplainer.ColumnMove): Boolean = m.rule != AbacusRule.DIRECT

    fun explain(s: StepExplainer.Step, m: StepExplainer.ColumnMove): String {
        when (AppLanguage.textLanguage()) {
            AppLanguage.ENGLISH -> return SubStepExplainerEn.explain(s, m)
            AppLanguage.INDONESIAN -> return SubStepExplainerId.explain(s, m)
            AppLanguage.ARABIC -> return SubStepExplainerAr.explain(s, m)
        }
        val phrase = "“${MistakeAnalyzer.stepPhrase(s, m)}”"
        val col = MistakeAnalyzer.columnName(m.col)
        return when (s.role) {
            StepExplainer.Role.DIRECT ->
                "$phrase: ${cap(locative(col))} yer var, ${beads(s)} doğrudan ${if (s.upper + s.lower > 0) "gelir" else "gider"}. Kural gerekmiyor."
            StepExplainer.Role.FIVE -> if (s.upper > 0) {
                "$phrase: ${col.let(::cap)} sütununda üstteki 5'lik boncuk aşağı, çubuğa iner. Bu, sütuna 5 eklemek demek."
            } else {
                "$phrase: ${col.let(::cap)} sütunundaki 5'lik boncuk yukarı kalkar. Bu, sütundan 5 çıkarmak demek."
            }
            StepExplainer.Role.FRIEND -> {
                val n = Math.abs(s.lower)
                "$phrase: Kardeşler toplamı 5 eden sayılardır: ${5 - n} ile $n. ${cap(col)} sütununa alttan $n birlik boncuk " +
                    "${if (s.lower > 0) "gelir" else "gider"}."
            }
            StepExplainer.Role.BIG_FRIEND -> {
                val n = Math.abs(s.upper) * 5 + Math.abs(s.lower)
                val add = s.upper + s.lower > 0
                "$phrase: Büyük kardeşler toplamı 10 eden sayılardır: ${10 - n} ile $n. ${cap(col)} sütununa $n " +
                    "${if (add) "gelir" else "gider"}${beadDetail(s)}."
            }
            StepExplainer.Role.BEADS -> "$phrase: ${cap(col)} sütununa alttan ${s.lower} birlik boncuk gelir."
            StepExplainer.Role.TEN -> {
                val left = MistakeAnalyzer.columnName(s.col)
                val head = if (m.op == HomeworkOp.ADD) {
                    "$phrase: ${cap(col)} sütununa 10 eklemek, solundaki $left sütununa 1 boncuk eklemek demek " +
                        "(soldaki sütunun 1 boncuğu, bu sütunun 10'u eder)."
                } else {
                    "$phrase: ${cap(col)} sütunundan 10 çıkarmak, solundaki $left sütunundan 1 boncuk çıkarmak demek " +
                        "(soldaki sütunun 1 boncuğu, bu sütunun 10'u eder)."
                }
                head + chain(s.carry)
            }
        }
    }

    /** "Neden bu kural?": neden doğrudan olmuyor ve kural neden işe yarıyor (derslerin gerekçesi). */
    fun whyRule(m: StepExplainer.ColumnMove): String {
        when (AppLanguage.textLanguage()) {
            AppLanguage.ENGLISH -> return SubStepExplainerEn.whyRule(m)
            AppLanguage.INDONESIAN -> return SubStepExplainerId.whyRule(m)
            AppLanguage.ARABIC -> return SubStepExplainerAr.whyRule(m)
        }
        val d = m.digitBefore
        val x = m.x
        val where = "${cap(locative(MistakeAnalyzer.columnName(m.col)))} $d var"
        return if (m.op == HomeworkOp.ADD) when (m.rule) {
            AbacusRule.DIRECT -> "$where; $x için yeterli boş boncuk var, doğrudan gelir."
            AbacusRule.FIVE -> "$where; alttan $x boncuk gelemez, boşta yalnızca ${4 - d % 5} birlik boncuk var. " +
                "$x eklemek, 5 ekleyip ${5 - x} çıkarmakla aynı. Bu yüzden 5 gelir, kardeşi ${5 - x} gider."
            AbacusRule.TEN -> "$where; $x eklenince ${d + x} olur, bir sütun en fazla 9 tutar. " +
                "$x eklemek, 10 ekleyip ${10 - x} çıkarmakla aynı. Bu yüzden 10 gelir, büyük kardeşi ${10 - x} gider."
            AbacusRule.BEAD -> "$where; $x eklenince ${d + x} olur, bir sütun en fazla 9 tutar. 10'luk kural da doğrudan " +
                "olmuyor: büyük kardeş ${10 - x} gitmeli ama alttan yalnızca ${d % 5} birlik boncuk var. ${10 - x} çıkarmak " +
                "da 5 çıkarıp ${x - 5} eklemekle aynı. Bu yüzden ${x - 5} boncuk gelir, 5 gider, 10 gelir."
        } else when (m.rule) {
            AbacusRule.DIRECT -> "$where; $x için yeterli boncuk var, doğrudan gider."
            AbacusRule.FIVE -> "$where; alttan $x boncuk gidemez, yalnızca ${d % 5} birlik boncuk var. " +
                "$x çıkarmak, 5 çıkarıp ${5 - x} eklemekle aynı. Bu yüzden 5 gider, kardeşi ${5 - x} gelir."
            AbacusRule.TEN -> "$where; $d içinden $x çıkmaz. " +
                "$x çıkarmak, 10 çıkarıp ${10 - x} eklemekle aynı. Bu yüzden 10 gider, kardeşi ${10 - x} gelir."
            AbacusRule.BEAD -> "$where; $d içinden $x çıkmaz. 10'luk kural da doğrudan olmuyor: kardeşi ${10 - x} " +
                "gelmeli ama alttan yalnızca ${4 - d % 5} boncuk gelebilir. ${10 - x} eklemek de 5 ekleyip ${x - 5} " +
                "çıkarmakla aynı. Bu yüzden 10 gider, 5 gelir, kardeşi ${x - 5} gider."
        }
    }

    /**
     * Elde zinciri: sol sütun 1'i doğrudan alamıyorsa (dolu 9, boş 0, ya da 4/5'te 5'lik kural)
     * oradaki kural; o da sola taşıyorsa bir sonraki sütun.
     */
    private fun chain(c: StepExplainer.ColumnMove?): String {
        if (c == null || c.rule == AbacusRule.DIRECT) return ""
        val add = c.op == HomeworkOp.ADD
        val col = MistakeAnalyzer.columnName(c.col)
        val d = c.digitBefore
        val state = when {
            add && d == 9 -> "Ama ${locative(col)} 9 var, sütun dolu."
            add -> "Ama ${locative(col)} $d var, alttan 1 boncuk daha gelemez."
            d == 0 -> "Ama ${locative(col)} boncuk yok."
            else -> "Ama ${locative(col)} yalnızca 5'lik boncuk var."
        }
        val rule = when (c.rule) {
            AbacusRule.FIVE -> "5'lik kuralla"
            AbacusRule.BEAD -> "boncuk kuralıyla"
            else -> "10'luk kuralla"
        }
        val parts = c.steps.joinToString(", ") { st ->
            when (st.role) {
                StepExplainer.Role.TEN -> {
                    val left = MistakeAnalyzer.columnName(st.col)
                    if (add) "${dative(left)} 1 boncuk gelir" else "${ablative(left)} 1 boncuk gider"
                }
                StepExplainer.Role.BIG_FRIEND -> {
                    val n = Math.abs(st.upper) * 5 + Math.abs(st.lower)
                    if (add) "${locative(col)}ki $n gider" else "${dative(col)} $n gelir"
                }
                else -> MistakeAnalyzer.stepPhrase(st, c)
            }
        }
        val next = c.steps.filter { it.role == StepExplainer.Role.TEN }.joinToString("") { chain(it.carry) }
        return " $state ${if (add) "Oraya 1 eklemek de" else "Oradan 1 çıkarmak da"} $rule olur: $parts.$next"
    }

    /** "5'lik boncuk ve 1 birlik boncuk" gibi: hangi boncukların oynadığı. */
    private fun beads(s: StepExplainer.Step): String {
        val lower = Math.abs(s.lower)
        return when {
            s.upper != 0 && lower != 0 -> "5'lik boncuk ve $lower birlik boncuk"
            s.upper != 0 -> "5'lik boncuk"
            else -> "$lower birlik boncuk"
        }
    }

    private fun beadDetail(s: StepExplainer.Step): String {
        if (s.upper == 0 || s.lower == 0) return ""
        val upper = if (s.upper > 0) "5'lik boncuk iner" else "5'lik boncuk kalkar"
        val lower = "${Math.abs(s.lower)} birlik boncuk ${if (s.lower > 0) "gelir" else "gider"}"
        return " ($upper, $lower)"
    }

    // Sütun adları "birler, onlar, yüzler, binler, on binler": ünlü uyumu son heceye göre.
    private fun back(col: String) = col.endsWith("lar")
    private fun locative(col: String) = col + if (back(col)) "da" else "de"
    private fun dative(col: String) = col + if (back(col)) "a" else "e"
    private fun ablative(col: String) = col + if (back(col)) "dan" else "den"
    private fun cap(s: String) = s.replaceFirstChar { it.uppercase() }
}

package com.example.app.homework

import com.example.app.R
import com.example.app.appPlural
import com.example.app.appString
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Ödev ayarlarının ekranda okunan hâli: başlık ("5'lik ve 10'luk toplama") ve ayrıntı satırı
 * ("Abaküslü · 2 basamak · 5 sayı · 1,5 sn · 25 soru"). Öğretmenin listesi, öğrencinin kartı ve
 * oynatıcı aynı metni gösteriyor.
 */
object HomeworkText {

    fun ruleLabel(rule: AbacusRule): String = appString(
        when (rule) {
            AbacusRule.DIRECT -> R.string.hw_rule_direct
            AbacusRule.FIVE -> R.string.hw_rule_five
            AbacusRule.TEN -> R.string.hw_rule_ten
            AbacusRule.BEAD -> R.string.hw_rule_bead
        }
    )

    fun opLabel(op: HomeworkOp): String = appString(
        when (op) {
            HomeworkOp.ADD -> R.string.hw_op_add
            HomeworkOp.SUB -> R.string.hw_op_sub
            HomeworkOp.MUL -> R.string.hw_op_mul
        }
    )

    fun title(cfg: HomeworkConfig): String {
        if (cfg.op == HomeworkOp.MUL) return appString(R.string.hw_mul_title, cfg.digits, cfg.digits2)
        val labels = AbacusRule.values().filter { it in cfg.rules }.map { ruleLabel(it) }
        val joined = when (labels.size) {
            0 -> ""
            1 -> labels[0]
            else -> labels.dropLast(1).joinToString(", ") + " " + appString(R.string.hw_and) + " " + labels.last()
        }
        val op = appString(if (cfg.op == HomeworkOp.ADD) R.string.hw_op_add_lower else R.string.hw_op_sub_lower)
        return appString(R.string.hw_title, joined, op)
    }

    fun details(cfg: HomeworkConfig): String {
        val parts = mutableListOf(appString(if (cfg.withAbacus) R.string.hw_with_abacus else R.string.hw_without_abacus))
        if (cfg.op == HomeworkOp.MUL) {
            parts += appString(R.string.hw_mul_digits, cfg.digits, cfg.digits2)
        } else {
            parts += appString(R.string.hw_digits, cfg.digits)
            parts += if (cfg.op == HomeworkOp.SUB) appPlural(R.plurals.hw_sub_numbers, cfg.numberCount, cfg.numberCount)
            else appPlural(R.plurals.hw_numbers, cfg.numberCount, cfg.numberCount)
            parts += appString(R.string.hw_seconds, formatSeconds(cfg.intervalMs))
        }
        parts += appPlural(R.plurals.hw_questions, cfg.questionCount, cfg.questionCount)
        parts += digitFilters(cfg)
        return parts.joinToString(" · ")
    }

    /**
     * Gelişmiş rakam seçiminin okunan hâli: "5'lik: 3, 4", çarpmada "2. sayı birler: 2, 3".
     * Seçim yoksa (bütün sayılar) boş liste.
     */
    fun digitFilters(cfg: HomeworkConfig): List<String> {
        if (cfg.op != HomeworkOp.MUL) {
            return AbacusRule.values().mapNotNull { rule ->
                cfg.ruleDigits[rule]?.let { appString(R.string.hw_advanced_rule_digits, ruleLabel(rule), digitList(it)) }
            }
        }
        return listOf(1 to cfg.mulDigits1, 2 to cfg.mulDigits2).flatMap { (factor, picked) ->
            picked.keys.sortedDescending().map { position ->
                appString(R.string.hw_advanced_mul_digits, factor, placeName(position), digitList(picked.getValue(position)))
            }
        }
    }

    /** Basamak adı, 0 = "birler". */
    fun placeName(position: Int): String =
        com.example.app.AppStrings.context().resources.getStringArray(R.array.hw_place_names)
            .getOrElse(position) { (position + 1).toString() }

    private fun digitList(digits: Set<Int>): String = digits.sorted().joinToString(", ")

    /**
     * Kartlar için kısa özet: tür, basamak, soru ("Abaküslü · 2 basamak · 20 soru"). Sayı adedi ve
     * hız ayrıntıda; listede her kartta beş parça bilgi okunmuyordu.
     */
    fun compactDetails(cfg: HomeworkConfig): String {
        val parts = mutableListOf(appString(if (cfg.withAbacus) R.string.hw_with_abacus else R.string.hw_without_abacus))
        parts += if (cfg.op == HomeworkOp.MUL) appString(R.string.hw_mul_digits, cfg.digits, cfg.digits2)
        else appString(R.string.hw_digits, cfg.digits)
        parts += appPlural(R.plurals.hw_questions, cfg.questionCount, cfg.questionCount)
        return parts.joinToString(" · ")
    }

    /** Kartın solundaki işlem simgesi: renkli daire içinde +, − ya da ×. */
    fun bindOpBadge(view: android.widget.TextView, op: HomeworkOp) {
        val (symbol, color) = when (op) {
            HomeworkOp.ADD -> "+" to 0xFF58CC02.toInt()
            HomeworkOp.SUB -> "−" to 0xFFFF9600.toInt()
            HomeworkOp.MUL -> "×" to 0xFFCE82FF.toInt()
        }
        view.text = symbol
        (view.background?.mutate() as? android.graphics.drawable.GradientDrawable)?.setColor(color)
    }

    /** 1500 → "1,5"; 100 → "0,1". Türkçede ondalık ayırıcı virgül. */
    fun formatSeconds(ms: Long): String =
        DecimalFormat("0.##", DecimalFormatSymbols(Locale("tr"))).format(ms / 1000.0)

    /** "1,5" / "1.5" → 1500; çözülemezse null. */
    fun parseSeconds(text: String): Long? {
        val v = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        return Math.round(v * 1000)
    }

    /**
     * Sayıya iyelik eki: 8 → "8'i", 3 → "3'ü", 6 → "6'sı", 12 → "12'si", 40 → "40'ı". Ek sayının
     * OKUNUŞUNUN son kelimesine bakıyor (on iki → "iki" → 'si); sabit bir "'i" çoğu sayıda yanlıştı.
     * Sınıf en fazla 60 kişi; 100'e kadar doğru.
     */
    fun withAccusative(n: Int): String {
        val lastDigit = n % 10
        val suffix = if (lastDigit != 0 || n == 0) {
            when (lastDigit) {
                0 -> "ı"          // sıfır
                1, 5, 8 -> "i"    // bir, beş, sekiz
                2, 7 -> "si"      // iki, yedi
                3, 4 -> "ü"       // üç, dört
                6 -> "sı"         // altı
                else -> "u"       // dokuz
            }
        } else {
            when ((n / 10) % 10) {
                1, 3 -> "u"       // on, otuz
                2, 5 -> "si"      // yirmi, elli
                4, 6, 9 -> "ı"    // kırk, altmış, doksan
                7, 8 -> "i"       // yetmiş, seksen
                else -> "ü"       // yüz
            }
        }
        return "$n'$suffix"
    }

    /** Soruyu tek satır yazar: "12 + 34 + 21", "500 − 23 − 45", "23 × 4". */
    fun questionLine(q: HomeworkQuestion): String = when (q.op) {
        HomeworkOp.ADD -> q.numbers.joinToString(" + ")
        HomeworkOp.SUB -> q.numbers.joinToString(" − ")
        HomeworkOp.MUL -> "${q.numbers[0]} × ${q.numbers[1]}"
    }
}

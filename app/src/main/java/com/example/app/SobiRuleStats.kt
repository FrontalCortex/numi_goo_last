package com.example.app

import android.content.Context
import com.example.app.abacus.MistakeAnalyzer
import com.example.app.abacus.StepExplainer
import com.example.app.homework.HomeworkOp

/**
 * SOBİ'NİN KURAL HAFIZASI — çocuğun her kuralda kaç doğru, kaç yanlış yaptığı (kullanıcı kararı
 * 01.11.2026). Sobi ekranı bunu bağlamla gönderir; Sobi "10'luk kuralda biraz zorlanıyorsun, bir örnek
 * daha yapalım mı?" diyebilsin.
 *
 * Kaynaklar: ders soruları (abaküslü ve flaş), Sobi ekranındaki adımlar ve örnekler. Yanlışta yalnızca
 * hatanın düştüğü kural sayılır ([MistakeAnalyzer] buldu ise); bulunamadıysa sayılmaz (yanlış bir kurala
 * yanlış yazmaktan iyidir). Doğruda işlemin bütün kuralları.
 *
 * Kişisel bilgi yok: yalnızca kural anahtarı ("add_ten") ve iki sayı. Bu cihazda tutulur (hesaba bağlı
 * değil); başka cihazda sıfırdan başlar.
 */
object SobiRuleStats {

    private const val PREFS = "sobi_rule_stats"

    data class Entry(val key: String, val correct: Int, val wrong: Int)

    fun record(context: Context, keys: Collection<String>, correct: Boolean) {
        if (keys.isEmpty()) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val e = p.edit()
        for (k in keys.toSet()) {
            val name = (if (correct) "ok_" else "bad_") + k
            e.putInt(name, p.getInt(name, 0) + 1)
        }
        e.apply()
    }

    fun snapshot(context: Context): List<Entry> {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return KEYS.map { Entry(it, p.getInt("ok_$it", 0), p.getInt("bad_$it", 0)) }
            .filter { it.correct + it.wrong > 0 }
    }

    /**
     * İki sayılı ders sorusu ([numbers] = ilk, ±ikinci). Yanlışta hata [log] ile aranır; bulunamazsa
     * kaydedilmez.
     */
    fun recordAnswer(context: Context, numbers: List<Int>, correct: Boolean, log: List<Int>, answer: Int) {
        if (correct) {
            record(context, rulesOf(numbers), true)
            return
        }
        if (numbers.size != 2 || log.isEmpty()) return
        val d = MistakeAnalyzer.analyze(numbers[0], if (numbers[1] < 0) "-" else "+", Math.abs(numbers[1]), log, answer)
        val m = d?.move ?: return
        record(context, moveRules(m), false)
    }

    /** Flaş dizisi: yanlışta yalnızca yanlış yapılan işlemlerin kuralları (kayıt yoksa hiç). */
    fun recordSequence(context: Context, numbers: List<Int>, correct: Boolean, log: List<Int>, marks: List<Int>, answer: Int) {
        if (correct) {
            record(context, rulesOf(numbers), true)
            return
        }
        if (marks.isEmpty()) return
        val mistakes = MistakeAnalyzer.findSequenceMistakes(numbers, log, marks, answer) ?: return
        val keys = mistakes.mapNotNull { mk ->
            val rule = mk.rule ?: return@mapNotNull null
            (if (mk.number < 0) "sub_" else "add_") + rule.key
        }
        record(context, keys, false)
    }

    /** Hamlenin kuralları, elde zinciri dahil: "add_bead", "add_ten". */
    fun moveRules(m: StepExplainer.ColumnMove): List<String> {
        val prefix = if (m.op == HomeworkOp.ADD) "add_" else "sub_"
        return StepExplainer.rulesUsed(m).map { prefix + it.key }
    }

    /** Dizinin (ilk sayı yazılmış, sonra ± sayılar) bütün kuralları. Abaküse sığmıyorsa boş. */
    fun rulesOf(numbers: List<Int>): List<String> {
        if (numbers.size < 2) return emptyList()
        val out = LinkedHashSet<String>()
        var running = numbers[0].toLong()
        for (n in numbers.drop(1)) {
            if (n == 0) continue
            val e = StepExplainer.explain(running, if (n < 0) HomeworkOp.SUB else HomeworkOp.ADD, Math.abs(n).toLong())
                ?: return emptyList()
            e.moves.forEach { out += moveRules(it) }
            running = e.result
        }
        return out.toList()
    }

    private val KEYS = listOf("add_direct", "add_five", "add_ten", "add_bead", "sub_direct", "sub_five", "sub_ten", "sub_bead")
}

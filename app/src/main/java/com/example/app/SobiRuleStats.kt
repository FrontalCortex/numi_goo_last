package com.example.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.app.abacus.MistakeAnalyzer
import com.example.app.abacus.SobiTestPlanner
import com.example.app.abacus.StepExplainer
import com.example.app.homework.HomeworkOp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

/**
 * KURAL HAFIZASI — çocuğun her kuralda kaç doğru, kaç yanlış yaptığı (kullanıcı kararı 01.11.2026).
 *
 * İki yere gider:
 *  - Sobi'nin bağlamı ([snapshot]): Sobi zorlanılan kuralda nazikçe örnek önerebilsin. Bu cihazdaki toplam.
 *  - Veli panelindeki "Kural başarısı" (eski adı "Ödev başarısı"; [com.example.app.homework.HomeworkRuleStatsFragment]):
 *    ödevlerin kural sayımıyla aynı toplama eklenir (sunucuda recordRuleTally → homeworkRuleStats/{uid}).
 *    Sayım biriktirilir ([FLUSH_DELAY_MS]) ve gönderilir; gönderilemeyen bekler, sonraki kayıtla yeniden
 *    denenir. Sınıf öğretmeni de aynı ekranı görür.
 *
 * Sayım ödevlerle aynı: her basamak işlemi gerektirdiği kurala sayılır (elde zinciri dahil). Yanlış
 * cevapta yalnızca hatanın düştüğü işlem yanlış sayılır ([MistakeAnalyzer] buldu ise); bulunamazsa
 * kural sayılmaz (yanlış bir kurala yanlış yazmaktan iyidir), soru yine yanlış sayılır.
 *
 * Kaynaklar: ders soruları (abaküslü ve flaş), Sobi ekranındaki adımlar ve örnekler (soru sayılmaz,
 * yalnızca kural). Kişisel bilgi yok: kural anahtarı ("add_ten") ve sayılar.
 */
object SobiRuleStats {

    private const val TAG = "SobiRuleStats"
    private const val PREFS = "sobi_rule_stats"
    private const val FLUSH_DELAY_MS = 4000L

    data class Entry(val key: String, val correct: Int, val wrong: Int)

    private val handler = Handler(Looper.getMainLooper())
    private var flushing = false
    private var appContext: Context? = null
    private val flushRunnable = Runnable { appContext?.let { flush(it) } }

    /** [tally]: kural → (doğru, yanlış). [question]: ders sorusuysa sonucu (soru sayısına), değilse null. */
    fun add(context: Context, tally: Map<String, IntArray>, question: Boolean? = null) {
        if (tally.isEmpty() && question == null) return
        val p = prefs(context)
        val e = p.edit()
        for ((k, cw) in tally) {
            if (k !in KEYS) continue
            e.putInt("ok_$k", p.getInt("ok_$k", 0) + cw[0])
            e.putInt("bad_$k", p.getInt("bad_$k", 0) + cw[1])
            e.putInt("p_ok_$k", p.getInt("p_ok_$k", 0) + cw[0])
            e.putInt("p_bad_$k", p.getInt("p_bad_$k", 0) + cw[1])
        }
        if (question != null) {
            val qk = if (question) "p_q_ok" else "p_q_bad"
            e.putInt(qk, p.getInt(qk, 0) + 1)
        }
        e.apply()
        scheduleFlush(context)
    }

    /**
     * [withMental] false: yalnızca abaküslü kurallar. Sobi sohbetine giden bağlam bu; sunucudaki kural
     * adları (functions/sobi/chat.js RULE_LABELS) "m_" anahtarını tanımıyor ve Sobi ekranındaki iş zaten
     * abaküslü. Sobi testi ikisini birden istiyor.
     */
    fun snapshot(context: Context, withMental: Boolean = false): List<Entry> {
        val p = prefs(context)
        return (if (withMental) KEYS else BASE_KEYS).map { Entry(it, p.getInt("ok_$it", 0), p.getInt("bad_$it", 0)) }
            .filter { it.correct + it.wrong > 0 }
    }

    /** Sobi ekranında bir adım: hamlenin kuralları doğru ya da yanlış (soru sayılmaz). */
    fun recordMove(context: Context, m: StepExplainer.ColumnMove, correct: Boolean) {
        add(context, tallyOf(listOf(m), correct))
    }

    /** İki sayılı ders sorusu ([numbers] = ilk, ±ikinci). Yanlışta hata [log] ile aranır. */
    fun recordAnswer(context: Context, numbers: List<Int>, correct: Boolean, log: List<Int>, answer: Int) {
        if (correct) {
            add(context, tallyOf(movesOf(numbers), true), question = true)
            return
        }
        val m = if (numbers.size == 2 && log.isNotEmpty()) {
            MistakeAnalyzer.analyze(numbers[0], if (numbers[1] < 0) "-" else "+", Math.abs(numbers[1]), log, answer)?.move
        } else null
        add(context, if (m != null) tallyOf(listOf(m), false) else emptyMap(), question = false)
    }

    /** Flaş dizisi: yanlışta yalnızca yanlış yapılan işlemlerin kuralları (kayıt yoksa yalnızca soru). */
    fun recordSequence(context: Context, numbers: List<Int>, correct: Boolean, log: List<Int>, marks: List<Int>, answer: Int) {
        if (correct) {
            add(context, tallyOf(movesOf(numbers), true), question = true)
            return
        }
        val mistakes = if (marks.isNotEmpty()) MistakeAnalyzer.findSequenceMistakes(numbers, log, marks, answer).orEmpty() else emptyList()
        val moves = mistakes.mapNotNull { it.diagnosis?.move }
        add(context, tallyOf(moves, false), question = false)
    }

    /**
     * Abaküssüz (körleme / zihinden) soru (kullanıcı kararları 07.11.2026): "m_" önekli ayrı anahtarlara,
     * SORU BAŞINA — soruda geçen her kurala doğruysa 1 doğru, yanlışsa 1 yanlış. Neden işlem başına değil:
     * klavyeyle verilen cevapta boncuk kaydı yok, hatanın hangi işlemde olduğu bulunamıyor. Eskiden
     * körlemede yalnızca doğrular (işlem başına, abaküslü toplama) yazılıyordu; oran hep yüksek çıkıyor,
     * abaküslü başarıyı da şişiriyordu. Soru başına sayım iki yönde eşit: bir yanlış, tek soruluk ağırlıkta.
     * [numbers]: ilk sayı, sonra ± sayılar (çıkarılan eksi).
     */
    fun recordMental(context: Context, numbers: List<Int>, correct: Boolean) {
        val rules = movesOf(numbers).flatMap { moveRules(it) }.toSet()
        add(context, mentalTally(rules, correct), question = correct)
    }

    /**
     * Ders çarpma sorusu (kullanıcı kararı 08.11.2026: dersler de kaydetsin; eskiden yalnızca öğretmen ödevi
     * yazıyordu ve Sobi çarpmadaki eksiği göremiyordu). Ödevle aynı anahtar ve ölçü: basamak çifti başına,
     * soru başına ("mulA_2x1" abaküslü Part 3, "mulM_2x1" körleme Part 6).
     */
    fun recordMultiplication(context: Context, a: Int, b: Int, correct: Boolean, withAbacus: Boolean) {
        val key = SobiTestPlanner.mulKeyOf(a.toLong(), b.toLong(), withAbacus)
        add(context, mapOf(key to if (correct) intArrayOf(1, 0) else intArrayOf(0, 1)), question = correct)
    }

    /** [rules] öneksiz kurallar ("add_ten"); sonuç "m_add_ten" → (1, 0) ya da (0, 1). */
    fun mentalTally(rules: Collection<String>, correct: Boolean): Map<String, IntArray> =
        rules.associate { SobiTestPlanner.mentalKey(it) to if (correct) intArrayOf(1, 0) else intArrayOf(0, 1) }

    /** Hamlenin kuralları, elde zinciri dahil: "add_bead", "add_ten". */
    fun moveRules(m: StepExplainer.ColumnMove): List<String> {
        val prefix = if (m.op == HomeworkOp.ADD) "add_" else "sub_"
        return StepExplainer.rulesUsed(m).map { prefix + it.key }
    }

    private fun tallyOf(moves: List<StepExplainer.ColumnMove>, correct: Boolean): Map<String, IntArray> {
        val out = LinkedHashMap<String, IntArray>()
        for (m in moves) for (k in moveRules(m)) {
            val cw = out.getOrPut(k) { IntArray(2) }
            if (correct) cw[0]++ else cw[1]++
        }
        return out
    }

    /** Dizinin (ilk sayı yazılmış, sonra ± sayılar) hamleleri. Abaküse sığmıyorsa boş. */
    private fun movesOf(numbers: List<Int>): List<StepExplainer.ColumnMove> {
        if (numbers.size < 2) return emptyList()
        val out = ArrayList<StepExplainer.ColumnMove>()
        var running = numbers[0].toLong()
        for (n in numbers.drop(1)) {
            if (n == 0) continue
            val e = StepExplainer.explain(running, if (n < 0) HomeworkOp.SUB else HomeworkOp.ADD, Math.abs(n).toLong())
                ?: return emptyList()
            out += e.moves
            running = e.result
        }
        return out
    }

    // ── Sunucuya gönderim ───────────────────────────────────────────────────

    private fun scheduleFlush(context: Context) {
        appContext = context.applicationContext
        handler.removeCallbacks(flushRunnable)
        handler.postDelayed(flushRunnable, FLUSH_DELAY_MS)
    }

    /** Bekleyen sayımı gönderir; başarılıysa gönderilen kadarı düşülür (arada eklenenler kalır). */
    private fun flush(context: Context) {
        if (flushing || FirebaseAuth.getInstance().currentUser == null) return
        val p = prefs(context)
        val sent = HashMap<String, Int>()
        val tally = HashMap<String, Map<String, Int>>()
        for (k in KEYS) {
            val c = p.getInt("p_ok_$k", 0).coerceAtMost(SEND_MAX)
            val w = p.getInt("p_bad_$k", 0).coerceAtMost(SEND_MAX)
            if (c + w == 0) continue
            tally[k] = mapOf("c" to c, "w" to w)
            sent["p_ok_$k"] = c
            sent["p_bad_$k"] = w
        }
        val qc = p.getInt("p_q_ok", 0).coerceAtMost(SEND_MAX)
        val qw = p.getInt("p_q_bad", 0).coerceAtMost(SEND_MAX)
        sent["p_q_ok"] = qc
        sent["p_q_bad"] = qw
        if (tally.isEmpty() && qc + qw == 0) return
        flushing = true
        FirebaseFunctions.getInstance()
            .getHttpsCallable("recordRuleTally")
            .call(hashMapOf("tally" to tally, "q" to mapOf("c" to qc, "w" to qw)))
            .addOnSuccessListener {
                flushing = false
                val e = p.edit()
                for ((k, v) in sent) e.putInt(k, (p.getInt(k, 0) - v).coerceAtLeast(0))
                e.apply()
                // Sınırdan fazlası birikmişse kalanı da gönder.
                if (sent.values.any { it == SEND_MAX }) scheduleFlush(context)
            }
            .addOnFailureListener { err ->
                flushing = false
                Log.w(TAG, "recordRuleTally başarısız; sayım bekliyor", err)
            }
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Sunucunun çağrı başına sınırı (homework.EXTRA_TALLY_MAX). */
    private const val SEND_MAX = 200

    private val BASE_KEYS = listOf("add_direct", "add_five", "add_ten", "add_bead", "sub_direct", "sub_five", "sub_ten", "sub_bead")
    /** Abaküslü ve abaküssüz ("m_add_ten"); sunucu süzgeci functions/homework.js EXTRA_TALLY_KEY. */
    private val KEYS = BASE_KEYS + BASE_KEYS.map { SobiTestPlanner.mentalKey(it) } + MUL_KEYS

    /**
     * Çarpma çiftleri (büyük çarpan önce): ödevin sınırları içinde, abaküslü 4×1'e, zihinden 6×1'e kadar
     * (HomeworkRuleStatsFragment MUL_ABACUS / MUL_MENTAL). Sunucu süzgeci mul[AM]_[1-8]x[1-8].
     */
    private val MUL_KEYS: List<String>
        get() = (1..6).flatMap { d1 -> (1..minOf(d1, 3)).flatMap { d2 -> listOf("mulA_${d1}x$d2", "mulM_${d1}x$d2") } }
}

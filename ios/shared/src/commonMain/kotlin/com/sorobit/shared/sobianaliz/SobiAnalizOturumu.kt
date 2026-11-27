// Kaynak: app/src/main/java/com/example/app/MistakeReviewDialog.kt — "Sobi ile analiz et" ekranının
// görünümden bağımsız kısmı: adımların kurulması (buildSteps, buildMistakeSteps, transitionSteps,
// moveText, questionText, canReview), sayfa/grup durumu (addGroup, addInfoPage, openPage, showPage),
// Kontrol Et kararı (onCheck), yerel balonlar (localChips: "Abaküste göster", parça açıklamaları,
// "Neden bu kural?", "Başka bir örnek", "Daha zor bir örnek") ve örnek seçimi (startExample,
// openExample, exampleSteps). Analiz kipi (newInstance) taşındı; öğretici kipi (forTutorial) taşınmadı.
//
// Bilinçli farklar:
// - Metinler Android'de getString(R.string.x, ...) ile kuruluyor; burada [SobiMetinCozucu]'ya anahtar +
//   argüman olarak verilir (iOS: Metin.al ile aynı strings.xml anahtarları). Kotlin içinde gömülü
//   Türkçe metinler (MistakeAnalyzer, SubStepExplainer) olduğu gibi gelir.
// - Yapay zekâ ile ilgili her şey (Sobi'ye yazma, sunucu önerileri, "Bunu anlamadım", beğen/bildir,
//   "Sıradaki soru", bakiye) yok. Zamanlama (700 / 500 / 900 ms), ses, sesli okuma ve abaküs
//   animasyonu platformda.
// - Abaküsün "meşgul" hâli (holdAbacus) platformda tutulur; bu sınıf yalnızca kararları verir.
package com.sorobit.shared.sobianaliz

import kotlin.math.abs

/** Android getString(R.string.x, args) karşılığı; iOS tarafı Metin.al ile çözer. */
interface SobiMetinCozucu {
    fun metin(anahtar: String, argumanlar: List<Any>): String
}

/** Sohbet balonu (Android SobiStepTracker.Message'ın görünen kısmı). [cocuktan]: sağda, çocuğun balonu. */
data class SobiAnalizMesaji(val kimlik: Int, val cocuktan: Boolean, val metin: String)

/** Kural motorunun cevapladığı seçenek. [ucretli]: yeşil (Android: yeni örnek 1 hak ya da kredi). */
data class SobiAnalizBalonu(val kod: Int, val etiket: String, val ucretli: Boolean)

/** "Abaküste göster": [baslangic]'tan [sayi] kadar toplama/çıkarma oynatılır (Android ServerSobi.Action). */
data class SobiGosterim(val baslangic: Int, val toplama: Boolean, val sayi: Int)

/** Seçilmiş ama henüz açılmamış örnek (Android: ödemeden önce). */
data class SobiOrnekHazirligi(val ilkSayi: Int, val ikinciSayi: Int, val duzey: Int)

/**
 * Kontrol Et sonucu. [kapat]: bilgi sayfası ("Derse dön"), ekran kapanır. [dogru]: doğruysa platform
 * 900 ms bekleyip [sonrakiSayfa]'yı açar (-1: yok). Yanlışsa 700 ms bekleyip abaküsü [geriDeger]'e
 * döndürür. [hamle]: kural istatistiğine yazılacak hamle (Android SobiRuleStats.recordMove), yoksa null.
 * [mesaj]: eklenen Sobi mesajı (sesli okuma için).
 */
data class SobiKontrolSonucu(
    val kapat: Boolean,
    val dogru: Boolean,
    val sonrakiSayfa: Int,
    val geriDeger: Int,
    val hamle: StepExplainer.ColumnMove?,
    val mesaj: String?,
)

/** Balona dokunma sonucu: ya mesaj eklendi, ya oynatma istendi, ya da örnek seçildi (ödeme bekliyor). */
data class SobiBalonSonucu(
    val mesaj: String?,
    val gosterim: SobiGosterim?,
    val ornek: SobiOrnekHazirligi?,
)

/** MistakeReviewDialog companion'ındaki adım kurucuları. */
object SobiAnalizAdimlari {

    /** Android Step. [label]: adım çubuğunda ne işlendiği; [intro]: talimattan önce; [num]/[of]: grupta sırası. */
    internal data class Adim(
        val base: Int,
        val target: Int,
        val move: StepExplainer.ColumnMove?,
        val text: String,
        val label: String? = null,
        val intro: String? = null,
        val question: String? = null,
        val num: Int = 0,
        val of: Int = 0,
    )

    private object BosCozucu : SobiMetinCozucu {
        override fun metin(anahtar: String, argumanlar: List<Any>): String = ""
    }

    /** "23 + 33 - 12": ilk sayı, sonra işaretleriyle. */
    fun questionText(numbers: List<Int>): String =
        numbers.mapIndexed { i, n -> if (i == 0) "$n" else if (n < 0) "- ${-n}" else "+ $n" }.joinToString(" ")

    /** Yalnızca toplama/çıkarma ve abaküse sığan diziler (Android MistakeReviewDialog.canReview). */
    fun canReview(numbers: List<Int>?): Boolean = numbers != null && buildSteps(numbers, BosCozucu).isNotEmpty()

    private fun signed(n: Int) = if (n < 0) "−${-n}" else "+$n"

    /** Adımlar: ilk sayıyı yazmak, sonra her sayının her basamağı. Abaküse sığmayan dizide boş. */
    internal fun buildSteps(numbers: List<Int>, c: SobiMetinCozucu): List<Adim> {
        if (numbers.size < 2 || numbers[0] !in 0..99999) return emptyList()
        val out = ArrayList<Adim>()
        out += Adim(0, numbers[0], null, c.metin("review_step_write", listOf(numbers[0])))
        var running = numbers[0].toLong()
        for (k in 1 until numbers.size) {
            val n = numbers[k]
            if (n == 0) continue
            val hop = if (n < 0) HomeworkOp.SUB else HomeworkOp.ADD
            val plan = StepExplainer.explain(running, hop, abs(n).toLong()) ?: return emptyList()
            val label = if (numbers.size > 2) signed(n) else null
            for (m in plan.moves) out += Adim(m.valueBefore.toInt(), m.valueAfter.toInt(), m, moveText(m, c), label)
            running = plan.result
        }
        return out
    }

    /** Abaküs [base]'den [target]'a. Sıfırdan yazmaksa tek "yaz" adımı, değilse hamleler. Sığmıyorsa null. */
    internal fun transitionSteps(base: Int, target: Int, question: String?, c: SobiMetinCozucu): List<Adim>? {
        if (base == 0) return listOf(Adim(0, target, null, c.metin("review_step_write", listOf(target)), question = question))
        val e = StepExplainer.explainTransition(base.toLong(), target.toLong()) ?: return null
        return e.moves.map { m -> Adim(m.valueBefore.toInt(), m.valueAfter.toInt(), m, moveText(m, c), question = question) }
    }

    internal fun moveText(m: StepExplainer.ColumnMove, c: SobiMetinCozucu): String {
        val column = MistakeAnalyzer.columnName(m.col).replaceFirstChar { it.uppercase() }
        val rule = MistakeAnalyzer.ruleName(m.rule).replaceFirstChar { it.uppercase() }
        val moves = m.steps.joinToString(", ") { MistakeAnalyzer.stepPhrase(it, m) }
        return if (m.op == HomeworkOp.ADD) c.metin("review_step_add", listOf(column, m.x, rule, moves))
        else c.metin("review_step_sub", listOf(column, m.x, rule, moves))
    }

    /**
     * Flaş dizisinde yalnızca yanlış yapılan işlemler: her biri abaküste çocuğun o anki değerinden
     * başlar, basamak basamak çözülür. İşlemin ilk adımında hatanın başlığı ve teşhisi. Hata yoksa ya da
     * işlem abaküse sığmıyorsa null (bütün dizi çözdürülür).
     */
    internal fun buildMistakeSteps(mistakes: List<MistakeAnalyzer.SequenceMistake>, c: SobiMetinCozucu): List<Adim>? {
        if (mistakes.isEmpty()) return null
        val out = ArrayList<Adim>()
        mistakes.forEachIndexed { j, mk ->
            val target = mk.base + mk.number
            val head = when {
                mk.index == 0 -> c.metin("review_mistake_write", listOf(j + 1, target, mk.value))
                mk.number < 0 -> c.metin("review_mistake_sub", listOf(j + 1, mk.base, -mk.number, mk.value, target))
                else -> c.metin("review_mistake_add", listOf(j + 1, mk.base, mk.number, mk.value, target))
            }
            val intro = listOfNotNull(head, mk.diagnosis?.text?.takeIf { mk.index > 0 }).joinToString(" ")
            if (mk.index == 0) {
                out += Adim(0, target, null, c.metin("review_step_write", listOf(target)), c.metin("review_label_first", emptyList()), intro)
                return@forEachIndexed
            }
            val hop = if (mk.number < 0) HomeworkOp.SUB else HomeworkOp.ADD
            val plan = StepExplainer.explain(mk.base.toLong(), hop, abs(mk.number).toLong()) ?: return null
            val label = "${mk.base} ${if (mk.number < 0) "− ${-mk.number}" else "+ ${mk.number}"}"
            plan.moves.forEachIndexed { i, m ->
                out += Adim(m.valueBefore.toInt(), m.valueAfter.toInt(), m, moveText(m, c), label, if (i == 0) intro else null)
            }
        }
        return out
    }

    /** Sorunun kullandığı kurallar (Sobi başka kural anlatmasın). */
    internal fun rulesOf(moves: List<StepExplainer.ColumnMove>): List<String> =
        (listOf("add_direct") + moves.flatMap { m ->
            val prefix = if (m.op == HomeworkOp.ADD) "add_" else "sub_"
            StepExplainer.rulesUsed(m).map { prefix + it.key }
        }).distinct()
}

/**
 * Bir analiz penceresinin durumu (Android MistakeReviewDialog'un analiz kipi). Platform: ekranı çizer,
 * abaküsü oynatır, bekleme sürelerini uygular ve kararları buradan alır. Ana iş parçacığından kullanılır.
 *
 * @param numbers sorudaki sayılar (çıkarılan eksi), @param answer çocuğun cevabı,
 * @param log abaküs değer kaydı (klavyeyle cevaplananda boş), @param marks flaş dizisinde sayıların
 * göründüğü anların kayıttaki sırası (yoksa boş).
 */
class SobiAnalizOturumu(
    private val numbers: List<Int>,
    private val answer: Int,
    log: List<Int>,
    marks: List<Int>,
    private val cozucu: SobiMetinCozucu,
) {
    private class Sayfa(val adim: SobiAnalizAdimlari.Adim?, val title: String, val value: Int, val firstMessage: String, val group: Int = -1)

    private class Grup(val question: String, val example: Boolean, var wrong: Int = 0, var done: Boolean = false)

    private var steps: List<SobiAnalizAdimlari.Adim> = emptyList()

    /** Android diagnosis: iki sayılı abaküslü soruda teşhis. */
    val teshis: MistakeAnalyzer.Diagnosis?

    /** Flaş dizisinde yanlış yapılan işlemler; doluysa yalnızca bunlar çözdürülür. */
    val diziHatalari: List<MistakeAnalyzer.SequenceMistake>

    /** Analiz ekranında çocuğun ilk yanıldığı adım (teşhis yoksa hatanın yeri sayılır). */
    private var firstReviewMistake = -1

    /** Örneğin kuralı ("add_ten"); sorunun en zor kuralı. Yoksa örnek balonları çıkmaz. */
    val ornekKurali: String?

    /** Örneklerin zorluğu (1..[ExamplePicker.MAX_LEVEL]); çocuk "Daha zor bir örnek"e dokundukça artar. */
    var ornekDuzeyi: Int = 1
        private set

    private var examples = 0
    private val pageList = ArrayList<Sayfa>()
    private val groups = ArrayList<Grup>()
    private val pages = HashMap<Int, ArrayList<SobiAnalizMesaji>>()
    private var mesajSayaci = 0

    /** Görüntülenen sayfa. */
    var sayfa: Int = 0
        private set

    /** Açılmış en ileri sayfa; ileri ok buna kadar gider. */
    var enIleriSayfa: Int = 0
        private set

    init {
        var sequence: List<MistakeAnalyzer.SequenceMistake> = emptyList()
        if (marks.isNotEmpty()) {
            sequence = MistakeAnalyzer.findSequenceMistakes(numbers, log, marks, answer).orEmpty()
            SobiAnalizAdimlari.buildMistakeSteps(sequence, cozucu)?.let { steps = it }
            if (steps.isEmpty()) sequence = emptyList()
        }
        diziHatalari = sequence
        teshis = if (marks.isEmpty() && log.isNotEmpty() && numbers.size == 2) {
            MistakeAnalyzer.analyze(numbers[0], if (numbers[1] < 0) "-" else "+", abs(numbers[1]), log, answer)
        } else null
        if (steps.isEmpty()) steps = SobiAnalizAdimlari.buildSteps(numbers, cozucu)
        // Örnek sorunun en zor kuralından, zorluk sorunun büyüklüğünden başlıyor.
        ornekKurali = steps.mapNotNull { it.move }.maxByOrNull { it.rule.ordinal }?.let { m ->
            (if (m.op == HomeworkOp.ADD) "add_" else "sub_") + m.rule.key
        }
        val digits = numbers.map { abs(it).toString().length }
        ornekDuzeyi = when {
            digits.any { it >= 4 } -> 5
            digits.any { it >= 3 } -> 4
            digits.drop(1).any { it >= 2 } -> 3
            digits.any { it >= 2 } -> 2
            else -> 1
        }.coerceAtMost(ExamplePicker.MAX_LEVEL)
    }

    private fun s(anahtar: String, vararg args: Any): String = cozucu.metin(anahtar, args.toList())

    private val toplam: Int get() = numbers.sum()

    // ── Okuma ───────────────────────────────────────────────────────────────

    val sayfaSayisi: Int get() = pageList.size

    /** Adım çubuğundaki yazı ("Adım 2 / 3 · +22 işleniyor"). */
    fun baslik(): String = pageList.getOrNull(sayfa)?.title.orEmpty()

    /** Abaküsün bu sayfadaki başlangıç değeri. */
    fun sayfaDegeri(): Int = pageList.getOrNull(sayfa)?.value ?: 0

    /** Adım sayfası mı (Kontrol Et), bilgi sayfası mı (Derse dön). */
    fun adimSayfasi(): Boolean = pageList.getOrNull(sayfa)?.adim != null

    /** Talimatın sözünü ettiği sütun (0 = birler); yazma adımında ve bilgi sayfasında -1. */
    fun vurguSutunu(): Int = pageList.getOrNull(sayfa)?.adim?.move?.col ?: -1

    fun mesajlar(): List<SobiAnalizMesaji> = pages[sayfa]?.toList().orEmpty()

    fun geriVar(): Boolean = sayfa > 0

    fun ileriVar(): Boolean = sayfa < enIleriSayfa

    /** Sorunun kullandığı kurallar ("add_direct", "add_ten"). */
    fun kurallar(): List<String> = SobiAnalizAdimlari.rulesOf(steps.mapNotNull { it.move })

    // ── Açılış ──────────────────────────────────────────────────────────────

    /**
     * Android startReview. [abakusDegeri]: abaküsün o anki değeri (adım yoksa bilgi sayfası onunla
     * açılır). Dönen değer: yeni eklenen ilk mesaj (sesli okuma için).
     */
    fun baslat(abakusDegeri: Int): String? {
        if (pageList.isNotEmpty()) return null
        if (steps.isEmpty()) {
            val p = addInfoPage(s("review_step_result"), abakusDegeri, s("review_intro_unknown"))
            return ac(p)
        }
        val d = teshis
        val intro = when {
            diziHatalari.isNotEmpty() -> s("review_intro_sequence", diziHatalari.size, MistakeAnalyzer.ruleSummary(diziHatalari))
            d != null -> s("review_intro_found", d.text)
            else -> s("review_intro_unknown")
        }
        // Soru üstte yazmıyor: iki sayılı soruda ilk mesajda.
        val question = if (numbers.size == 2) {
            s("review_intro_question", SobiAnalizAdimlari.questionText(numbers), answer, toplam)
        } else null
        val tip = d?.move?.let { m ->
            s(
                "review_done_tip",
                MistakeAnalyzer.columnName(m.col),
                m.steps.joinToString(", ") { "\"${MistakeAnalyzer.stepPhrase(it, m)}\"" },
            )
        }.orEmpty()
        val done = if (diziHatalari.isNotEmpty()) s("review_done_sequence", toplam)
        else s("review_done", SobiAnalizAdimlari.questionText(numbers), toplam) + tip
        val first = addGroup(steps, listOfNotNull(question, intro).joinToString("\n\n"), s("review_step_result"), toplam, done, example = false)
        return ac(first)
    }

    // ── Sayfalar ────────────────────────────────────────────────────────────

    /** Adım grubunu sayfalara ekler: adımlar ve sonda bir bilgi sayfası. İlk adımın sayfa sırası döner. */
    private fun addGroup(
        group: List<SobiAnalizAdimlari.Adim>, intro: String?, endTitle: String, endValue: Int, endText: String,
        example: Boolean,
    ): Int {
        val first = pageList.size
        val g = groups.size
        groups += Grup(group.firstOrNull()?.question ?: group.firstOrNull()?.label.orEmpty(), example)
        group.forEachIndexed { i, st ->
            val step = st.copy(num = i + 1, of = group.size)
            val m = step.move
            val wasHere = m != null && teshis?.move?.let { it.col == m.col && it.valueAfter == m.valueAfter } == true
            val text = listOfNotNull(
                if (i == 0) intro else null,
                if (wasHere) s("review_was_here") else null,
                step.intro, step.text,
            ).joinToString("\n\n")
            val label = step.label
            val title = if (label != null) s("review_step_label_number", step.num, step.of, label)
            else s("review_step_label", step.num, step.of)
            pageList += Sayfa(step, title, step.base, text, g)
        }
        pageList += Sayfa(null, endTitle, endValue, endText, g)
        return first
    }

    private fun addInfoPage(title: String, value: Int, text: String): Int {
        pageList += Sayfa(null, title, value, text)
        return pageList.lastIndex
    }

    /** Android openPage: ileri ok artık buraya kadar gider. Dönen değer: yeni eklenen ilk mesaj. */
    fun ac(i: Int): String? {
        if (i !in pageList.indices) return null
        enIleriSayfa = maxOf(enIleriSayfa, i)
        return git(i)
    }

    /**
     * Android showPage: ilk gelişte Sobi'nin ilk mesajı yazılır (dönen değer, sesli okuma için).
     * Abaküsü [sayfaDegeri]'ne getirmek ve sütunu parlatmak platformun işi.
     */
    fun git(i: Int): String? {
        if (i !in pageList.indices) return null
        sayfa = i
        val p = pageList[i]
        var eklenen: String? = null
        if (pages[i].isNullOrEmpty()) {
            ekle(i, false, p.firstMessage)
            eklenen = p.firstMessage
        }
        // Grubun sonuna gelindi: örnek çözüldü.
        if (p.adim == null) groups.getOrNull(p.group)?.done = true
        return eklenen
    }

    private fun ekle(p: Int, cocuktan: Boolean, metin: String) {
        mesajSayaci += 1
        pages.getOrPut(p) { ArrayList() } += SobiAnalizMesaji(mesajSayaci, cocuktan, metin)
    }

    // ── Kontrol Et ──────────────────────────────────────────────────────────

    /**
     * Android onCheck. [deger]: abaküsün değeri, [adimKaydi]: bu adımın başından beri değer kaydı
     * (Android stepLog). Bilgi sayfasında ekran kapanır.
     */
    fun kontrolEt(deger: Int, adimKaydi: List<Int>): SobiKontrolSonucu {
        val p = pageList.getOrNull(sayfa)
        val step = p?.adim ?: return SobiKontrolSonucu(true, false, -1, 0, null, null)
        val correct = deger == step.target
        if (!correct) groups.getOrNull(p.group)?.let { it.wrong++ }
        if (correct) {
            val mesaj = s("review_correct")
            ekle(sayfa, false, mesaj)
            val sonraki = if (sayfa + 1 < pageList.size) sayfa + 1 else -1
            return SobiKontrolSonucu(false, true, sonraki, step.base, step.move, mesaj)
        }
        val move = step.move
        val detail = if (move != null) MistakeAnalyzer.diagnoseStep(move, adimKaydi, deger).text
        else MistakeAnalyzer.diagnoseWrite(step.target, deger)
        val found = if (teshis == null && diziHatalari.isEmpty() && firstReviewMistake == -1) {
            firstReviewMistake = sayfa
            s("review_found_now") + " "
        } else ""
        val mesaj = found + s("review_wrong", detail)
        ekle(sayfa, false, mesaj)
        return SobiKontrolSonucu(false, false, -1, step.base, step.move, mesaj)
    }

    // ── Balonlar ────────────────────────────────────────────────────────────

    private class Eylem(val balon: SobiAnalizBalonu, val calistir: () -> SobiBalonSonucu)

    private fun eylemler(): List<Eylem> {
        val out = ArrayList<Eylem>()
        fun ekleBalon(etiket: String, ucretli: Boolean, calistir: () -> SobiBalonSonucu) {
            out += Eylem(SobiAnalizBalonu(out.size, etiket, ucretli), calistir)
        }
        val step = pageList.getOrNull(sayfa)?.adim
        // Adım sayfasında ilk seçenek doğrudan gösterim, ücretsiz.
        if (step != null) ekleBalon(s("sobi_suggest_show_abacus"), false) { SobiBalonSonucu(null, gosterim(), null) }
        val m = step?.move
        if (m != null && SubStepExplainer.hasParts(m)) {
            val parts = m.steps.map { st -> SubStepExplainer.question(st, m) to { SubStepExplainer.explain(st, m) } }
                .distinctBy { it.first } + (s("review_why_rule") to { SubStepExplainer.whyRule(m) })
            for ((label, answerText) in parts) {
                ekleBalon(label, false) {
                    val p = sayfa
                    ekle(p, true, label)
                    val cevap = answerText()
                    ekle(p, false, cevap)
                    SobiBalonSonucu(cevap, null, null)
                }
            }
        }
        // Analizde soru (ya da örnek) bitince, sonuç sayfasında: aynı kuralla bir tane daha (yeşil: ücretli).
        if (ornekKurali != null && step == null) {
            ekleBalon(s("sobi_screen_another_example"), true) { SobiBalonSonucu(null, null, ornekSec(ornekDuzeyi)) }
            if (ornekDuzeyi < ExamplePicker.MAX_LEVEL) {
                // Kademe ancak örnek açılınca (ödendikten sonra) artar.
                val duzey = ornekDuzeyi + 1
                ekleBalon(s("sobi_screen_harder_example"), true) { SobiBalonSonucu(null, null, ornekSec(duzey)) }
            }
        }
        return out
    }

    /** Android localChips: sayfanın yerel balonları. */
    fun balonlar(): List<SobiAnalizBalonu> = eylemler().map { it.balon }

    /** Balona dokunuldu ([SobiAnalizBalonu.kod]). Geçersiz kodda hiçbir şey olmaz. */
    fun balonaDokun(kod: Int): SobiBalonSonucu =
        eylemler().getOrNull(kod)?.calistir?.invoke() ?: SobiBalonSonucu(null, null, null)

    /**
     * Android showCurrentStep: bu adımın hamlesi adımın başından oynatılır. Yazma adımında sayı
     * sıfırdan yazılır. Bilgi sayfasında null.
     */
    fun gosterim(): SobiGosterim? {
        val step = pageList.getOrNull(sayfa)?.adim ?: return null
        if (step.move == null) return SobiGosterim(0, true, step.target)
        val amount = abs(step.target - step.base)
        return SobiGosterim(step.base, step.target >= step.base, amount)
    }

    // ── Örnek ───────────────────────────────────────────────────────────────

    /** Android startExample'ın seçim kısmı. Örnek bulunamazsa null (hiçbir şey olmaz, ödeme de yok). */
    private fun ornekSec(level: Int): SobiOrnekHazirligi? {
        val ruleKey = ornekKurali ?: return null
        // Analizde öğrenilen kurallar listesi yok: örnek yalnızca sorunun kuralını gerçekten kullansın.
        val seed = examples * 7 + toplam
        val (a, b) = ExamplePicker.pick(ruleKey, seed, level, null) ?: return null
        if (exampleSteps(a, b).isEmpty()) return null
        return SobiOrnekHazirligi(a, b, level)
    }

    /** Örneğin adımları: abaküs ilk sayı yazılmış olarak gelir. */
    private fun exampleSteps(a: Int, b: Int): List<SobiAnalizAdimlari.Adim> {
        val q = SobiAnalizAdimlari.questionText(listOf(a, b))
        return SobiAnalizAdimlari.transitionSteps(a, a + b, q, cozucu).orEmpty().map { it.copy(label = q) }
    }

    /**
     * Ödeme başarılı: Android openExample. Çocuğun balonu, sonra örneğin ilk adımı açılır. Dönen
     * değer: yeni eklenen ilk mesaj.
     */
    fun ornegiAc(h: SobiOrnekHazirligi): String? {
        val group = exampleSteps(h.ilkSayi, h.ikinciSayi)
        if (group.isEmpty()) return null
        ornekDuzeyi = h.duzey
        examples++
        val nums = listOf(h.ilkSayi, h.ikinciSayi)
        val q = SobiAnalizAdimlari.questionText(nums)
        ekle(sayfa, true, s("sobi_suggest_example"))
        val rule = group.mapNotNull { it.move }.maxByOrNull { it.rule.ordinal }?.rule
        val intro = s("sobi_screen_example_level", ornekDuzeyi, ExamplePicker.MAX_LEVEL) + " " +
            if (rule == null || rule == AbacusRule.DIRECT) s("sobi_screen_example_intro_direct", q)
            else s("sobi_screen_example_intro", q, MistakeAnalyzer.ruleName(rule))
        val sum = nums.sum()
        val first = addGroup(
            group, intro, s("sobi_screen_example_title"), sum, s("sobi_screen_example_done", q, sum), example = true,
        )
        return ac(first)
    }

    /** Ödeme başarısız (Android: sobi_error_network; günlük sınır metinleri yapay zekâ bakiyesine bağlı, yok). */
    fun ornekOdenemedi(): String {
        val mesaj = s("sobi_error_network")
        ekle(sayfa, false, mesaj)
        return mesaj
    }
}

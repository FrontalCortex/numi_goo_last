// Kaynak: app/src/main/java/com/example/app/SobiDailyTest.kt — saf kısım: günlük test sayacı ve ek test
// bedeli (SharedPreferences "sobi_test" anahtarları aynı), açılma kuralı (Part 1'in 6. satırı), derslerden
// görülmüş kurallar (LessonItem → LessonMark), seviyelerin birleştirilmesi (sunucu / cihaz), sunucu
// belgesinin çözümü (scoresOf, levelsOf), tohum, kural adı ve seviye açıklaması (ruleLabel, specText).
// Ayrıca SobiTestSheet.fill'in zorluk kartı kuralı (difficultyOptions) burada.
// Taşınmayan (platform işi): Firestore okuması (homeworkRuleStats/{uid}), Cloud Function çağrıları
// (recordSobiLevels, startSobiExtraTest), arka plan iş parçacığı, SharedPreferences dinleyicisi.
// Gün: Android "Europe/Istanbul" saat dilimiyle; Türkiye 2016'dan beri yıl boyu UTC+3 olduğu için burada
// sabit +3 kullanılıyor (aynı sonuç; Native hedefte saat dilimi veritabanına bağlı kalınmıyor).
package com.sorobit.shared.sobitest

import com.sorobit.shared.ders.LessonItem
import com.sorobit.shared.oyun.OyunKeyValueStore
import com.sorobit.shared.oyun.oyunSimdiMs
import com.sorobit.shared.sobianaliz.SobiMetinCozucu
import com.sorobit.shared.sobianaliz.SobiRuleStatEntry
import kotlinx.datetime.Instant
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Sobi'nin testi: haritadaki Sobi düğmesi ile oynatıcı arasındaki kurallar (kullanıcı kararları
 * 01.11.2026: günde 1 test + istenirse ek test, 10 soru, her testte sandık; zorluk çocuğun seviyesine
 * göre, başlamadan Kolay / Benim seviyem / Zor seçilebilir). Ek test 3 anahtar; Pro ve onaylı öğretmen
 * ücretsiz. Yarıda bırakılan test sayılmaz; tohum günden ve o gün bitirilen test sayısından.
 */
object SobiDailyTest {

    /** Android: SharedPreferences dosyası. */
    const val PREFS = "sobi_test"
    const val KEY_DAY = "tests_day"
    const val KEY_COUNT = "tests_count"
    const val KEY_NUDGE_DAY = "nudge_day"
    const val KEY_LEVEL = "level_"
    const val KEY_PAID_DAY = "extra_paid_day"
    const val KEY_PAID_INDEX = "extra_paid_index"
    const val KEY_UNLOCKED = "unlocked_"
    /** Android'de StringSet; burada virgülle birleştirilmiş metin. */
    const val KEY_LESSON_SEEN = "mental_seen_"

    /** Düğme Part 1'in 6. sıradaki satırı (lessonItems[6]) bitince açılır (kullanıcı kararı 02.11.2026). */
    const val UNLOCK_PART = 1
    const val UNLOCK_INDEX = 6

    /** functions/sobi/wallet.js EXTRA_TEST_KEYS ile aynı (yalnızca düğme metni; bedeli sunucu belirler). */
    const val EXTRA_TEST_KEYS = 3

    /** Sunucu sözleşmesi (Android'deki adlar). */
    const val RULE_STATS_COLLECTION = "homeworkRuleStats"
    const val LEVEL_FIELD = "sobiLevel"
    const val FUNCTION_RECORD_LEVELS = "recordSobiLevels"
    const val FUNCTION_START_EXTRA_TEST = "startSobiExtraTest"

    private val KEYS = listOf("add_direct", "add_five", "add_ten", "add_bead", "sub_direct", "sub_five", "sub_ten", "sub_bead")

    /** Seviyesi saklanan anahtarlar: abaküslü ve abaküssüz ("m_add_ten"). */
    val LEVEL_KEYS: List<String> = KEYS + KEYS.map { SobiTestPlanner.mentalKey(it) }

    /** Abaküslü çarpma (3), körleme toplama (4), çıkarma (5), çarpma (6). */
    val LESSON_PARTS: List<Int> = listOf(3, 4, 5, 6)

    /** Plan için gereken her şey (Android: SobiDailyTest.Info). */
    data class Info(
        val scores: List<SobiTestPlanner.RuleScore>,
        val levels: Map<String, Int>,
        val lessonSeen: Set<String>,
    )

    /** "yyyy-MM-dd", Türkiye saatiyle. */
    fun today(nowMs: Long = oyunSimdiMs()): String {
        val d = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(UtcOffset(hours = 3).asTimeZone()).date
        return d.year.toString().padStart(4, '0') + "-" +
            d.monthNumber.toString().padStart(2, '0') + "-" +
            d.dayOfMonth.toString().padStart(2, '0')
    }

    /** Android planAsync'teki tohum: `today().hashCode() * 31 + testsToday * 7919 + offset`. */
    fun seed(today: String, testsToday: Int, offset: Int): Int = today.hashCode() * 31 + testsToday * 7919 + offset

    /** Planı kurar; hata olursa null (Android: runCatching { SobiTestPlanner.plan(...) }). Swift'ten güvenle çağrılır. */
    fun plan(info: Info, offset: Int, seed: Int): SobiTestPlanner.Plan? =
        runCatching { SobiTestPlanner.plan(info.scores, info.levels, offset, seed, info.lessonSeen) }.getOrNull()

    /** Zorluk seçimi penceresinin kuralları (SobiTestSheet: SobiTestPlanner.choose); hata olursa null. */
    fun choose(info: Info): SobiTestPlanner.Choice? =
        runCatching { SobiTestPlanner.choose(info.scores, info.levels, info.lessonSeen) }.getOrNull()

    // ── Açılma ve derslerden görülmüş kurallar ─────────────────────────────

    /** Part 1'in satırlarından: [UNLOCK_INDEX]. satır bitti mi (Android checkUnlocked'ın karar kısmı). */
    fun isUnlockedFrom(part1Items: List<LessonItem>): Boolean = part1Items.getOrNull(UNLOCK_INDEX)?.stepIsFinish == true

    /**
     * Bir part'ın satırları → görülmüş kurallar (Android loadLessonSeen.finishOne): bir adım yeter
     * (stepIsFinish, currentStep > 1 ya da stepCompletionStatus'ta bir true).
     */
    fun seenFromItems(items: List<LessonItem>): Set<String> = SobiTestPlanner.seenFromLessons(items.map {
        val played = it.stepIsFinish || it.currentStep > 1 || it.stepCompletionStatus.any { s -> s }
        SobiTestPlanner.LessonMark(it.stableId, it.type == LessonItem.TYPE_HEADER, played)
    })

    /** Önbellekte hepsi var mı (varsa Firestore'a gidilmez). */
    fun allLessonRulesSeen(cached: Set<String>): Boolean = cached.containsAll(SobiTestPlanner.LESSON_RULES)

    // ── Seviyeler ve sunucu belgesi ─────────────────────────────────────────

    /** Sunucudaki seviye esas; bu cihazda daha yenisi varsa (gönderilemedi) o (Android loadStats). */
    fun mergeLevels(local: Map<String, Int>, server: Map<String, Int>): Map<String, Int> =
        local + server.filter { (k, v) -> v >= (local[k] ?: 0) }

    /** Bu cihazda olup sunucuya gidemeyen seviyeler: yeniden gönderilir. */
    fun unsentLevels(local: Map<String, Int>, server: Map<String, Int>): Map<String, Int> =
        local.filter { (k, v) -> v > (server[k] ?: 0) }

    /** Sunucu sayımı yeterli mi (abaküslü bir kuralda en az MIN_OPS işlem); değilse bu cihazınki. */
    fun pickScores(server: List<SobiTestPlanner.RuleScore>, local: List<SobiTestPlanner.RuleScore>): List<SobiTestPlanner.RuleScore> {
        val enough = server.any { !SobiTestPlanner.isMentalKey(it.key) && it.total >= SobiTestPlanner.MIN_OPS }
        return if (enough) server else local
    }

    /** Bu cihazdaki kural hafızası ([com.sorobit.shared.sobianaliz.SobiRuleStatsStore.snapshot]) → planlayıcının girdisi. */
    fun scoresFromEntries(entries: List<SobiRuleStatEntry>): List<SobiTestPlanner.RuleScore> =
        entries.map { SobiTestPlanner.RuleScore(it.key, it.correct, it.wrong) }

    /** Sunucu belgesinden (homeworkRuleStats/{uid}) kural başına `recent` toplamı, yoksa `tally`. */
    fun scoresOf(data: Map<String, Any?>?): List<SobiTestPlanner.RuleScore> {
        if (data == null) return emptyList()
        val recent = data["recent"] as? Map<*, *>
        val tally = data["tally"] as? Map<*, *>
        val keys = (recent?.keys.orEmpty() + tally?.keys.orEmpty()).mapNotNull { it as? String }.toSet()
        return keys.mapNotNull { key ->
            val window = (recent?.get(key) as? List<*>)?.mapNotNull { it as? Map<*, *> }
            val (c, w) = if (!window.isNullOrEmpty()) {
                window.sumOf { (it["c"] as? Number)?.toInt() ?: 0 } to window.sumOf { (it["w"] as? Number)?.toInt() ?: 0 }
            } else {
                val t = tally?.get(key) as? Map<*, *> ?: return@mapNotNull null
                ((t["c"] as? Number)?.toInt() ?: 0) to ((t["w"] as? Number)?.toInt() ?: 0)
            }
            SobiTestPlanner.RuleScore(key, c, w)
        }
    }

    /** `sobiLevel` alanı (functions/index.js recordSobiLevels); 1..20 dışı atılır. */
    fun levelsOf(data: Map<String, Any?>?): Map<String, Int> {
        val m = data?.get(LEVEL_FIELD) as? Map<*, *> ?: return emptyMap()
        return m.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val level = (v as? Number)?.toInt()?.takeIf { it in 1..SobiTestLevels.MAX } ?: return@mapNotNull null
            key to level
        }.toMap()
    }

    // ── Metinler ────────────────────────────────────────────────────────────

    /** Kuralın çocuğun bildiği adı: "10'luk toplama"; abaküssüzde "… · zihinden"; çarpmada "2×1 çarpma". */
    fun ruleLabel(key: String, metin: SobiMetinCozucu): String {
        val mul = SobiTestPlanner.mulDigits(key)
        if (mul != null) {
            val base = metin.metin("sobi_test_rule_mul", listOf(mul.first, mul.second))
            return if (SobiTestPlanner.isMentalKey(key)) metin.metin("sobi_test_rule_mental", listOf(base)) else base
        }
        if (SobiTestPlanner.isMentalKey(key)) {
            return metin.metin("sobi_test_rule_mental", listOf(baseRuleLabel(SobiTestPlanner.baseKey(key), metin)))
        }
        return baseRuleLabel(key, metin)
    }

    private fun baseRuleLabel(key: String, metin: SobiMetinCozucu): String =
        if (key in KEYS) metin.metin("sobi_test_rule_$key", emptyList()) else key

    /** Saniye metni: "2,4" (Android: "%.1f" Türkçe yerelde, ",0" atılır). Aralıklar 100 ms'nin katı. */
    fun secondsText(intervalMs: Long): String {
        val tenths = (intervalMs + 50) / 100
        val whole = tenths / 10
        val frac = tenths % 10
        return if (frac == 0L) whole.toString() else "$whole,$frac"
    }

    /** "2 basamaklı 3 sayı, 2,4 sn" */
    fun specText(level: Int, metin: SobiMetinCozucu): String {
        val s = SobiTestLevels.spec(level)
        return metin.metin("sobi_test_spec", listOf(s.digits, s.count, secondsText(s.intervalMs)))
    }

    /**
     * Zorluk kartları (SobiTestSheet.fill): ilk seviyeli zayıf kuraldan; Kolay −1 / Benim seviyem 0 / Zor +1.
     * Seviye 1'de Kolay, 20'de Zor aynı seviyeye düşüyor: o kart yok. Zayıf kuralların hepsi çarpmaysa boş
     * (seçim gösterilmez, test "Benim seviyem").
     */
    fun difficultyOptions(choice: SobiTestPlanner.Choice): List<DifficultyOption> {
        val base = choice.weak.firstNotNullOfOrNull { choice.current[it.key] } ?: return emptyList()
        val out = ArrayList<DifficultyOption>()
        for ((o, label) in listOf(-1 to "sobi_test_diff_easy", 0 to "sobi_test_diff_mine", 1 to "sobi_test_diff_hard")) {
            val level = (base + o).coerceIn(1, SobiTestLevels.MAX)
            if (o != 0 && level == base) continue
            out += DifficultyOption(o, label, level)
        }
        return out
    }

    /** Paneldeki kural satırları (zayıf + abaküssüz zayıf); [level] çarpmada null. */
    fun ruleRows(choice: SobiTestPlanner.Choice, metin: SobiMetinCozucu): List<RuleRow> =
        (choice.weak + choice.mentalWeak).map { w ->
            val label = ruleLabel(w.key, metin).replaceFirstChar { it.uppercase() }
            RuleRow(w.key, label, choice.current[w.key])
        }
}

/** Zorluk kartı: [offset] −1/0/+1, [titleResource] strings.xml adı, [level] o kartın seviyesi. */
data class DifficultyOption(val offset: Int, val titleResource: String, val level: Int)

/** Paneldeki kural satırı; [level] null ise seviye yazısı ve çubuğu yok (çarpma). */
data class RuleRow(val key: String, val label: String, val level: Int?)

/**
 * Android SobiDailyTest'in SharedPreferences ("sobi_test") kısmı. [today] her çağrıda dışarıdan
 * ([SobiDailyTest.today]); test edilebilsin diye.
 */
class SobiDailyTestStore(private val prefs: OyunKeyValueStore) {

    /** Bugün bitirilen test sayısı (günlük + ek testler). */
    fun testsToday(today: String): Int =
        if (prefs.getString(SobiDailyTest.KEY_DAY) == today) prefs.getInt(SobiDailyTest.KEY_COUNT, 0) else 0

    fun isDoneToday(today: String): Boolean = testsToday(today) > 0

    fun markDone(today: String) {
        val n = testsToday(today) + 1
        prefs.putString(SobiDailyTest.KEY_DAY, today)
        prefs.putInt(SobiDailyTest.KEY_COUNT, n)
    }

    /** Bu ek testin anahtar bedeli: ilk test, Pro, onaylı öğretmen ya da zaten ödendiyse 0. */
    fun extraTestCost(today: String, isPro: Boolean, isApprovedTeacher: Boolean): Int {
        val n = testsToday(today)
        if (n == 0) return 0
        if (isApprovedTeacher || isPro) return 0
        val paid = if (prefs.getString(SobiDailyTest.KEY_PAID_DAY) == today) prefs.getInt(SobiDailyTest.KEY_PAID_INDEX, 0) else 0
        return if (n <= paid) 0 else SobiDailyTest.EXTRA_TEST_KEYS
    }

    fun markExtraPaid(today: String) {
        val n = testsToday(today)
        prefs.putString(SobiDailyTest.KEY_PAID_DAY, today)
        prefs.putInt(SobiDailyTest.KEY_PAID_INDEX, n)
    }

    /** "Senin için bir test hazırladım!" balonu günde bir kez (çağrı hakkı tüketir). */
    fun shouldNudgeToday(today: String): Boolean {
        if (prefs.getString(SobiDailyTest.KEY_NUDGE_DAY) == today) return false
        prefs.putString(SobiDailyTest.KEY_NUDGE_DAY, today)
        return true
    }

    /** Açıldıktan sonra hesap başına önbellekte (ders geri alınmıyor). [uid] yoksa "guest". */
    fun isUnlockedCached(uid: String?): Boolean = prefs.getBoolean(SobiDailyTest.KEY_UNLOCKED + (uid ?: "guest"), false)

    /** Android checkUnlocked: önbellekte açıksa true; değilse Part 1'in satırlarından, açıksa önbelleğe yazar. */
    fun checkUnlocked(uid: String?, part1Items: List<LessonItem>): Boolean {
        if (isUnlockedCached(uid)) return true
        val unlocked = SobiDailyTest.isUnlockedFrom(part1Items)
        if (unlocked) prefs.putBoolean(SobiDailyTest.KEY_UNLOCKED + (uid ?: "guest"), true)
        return unlocked
    }

    fun cachedLessonSeen(uid: String?): Set<String> {
        val raw = prefs.getString(SobiDailyTest.KEY_LESSON_SEEN + (uid ?: "guest")) ?: return emptySet()
        return raw.split(',').filter { it.isNotEmpty() }.toSet()
    }

    /**
     * Android loadLessonSeen: önbellek + [partItems] (Part 3, 4, 5, 6'nın satırları; hepsi görülmüşse
     * boş verilebilir). Yeni kural bulunduysa önbelleğe yazılır.
     */
    fun lessonSeen(uid: String?, partItems: List<List<LessonItem>>): Set<String> {
        val cached = cachedLessonSeen(uid)
        if (SobiDailyTest.allLessonRulesSeen(cached)) return cached
        val found = HashSet(cached)
        for (items in partItems) found += SobiDailyTest.seenFromItems(items)
        if (found.size > cached.size) prefs.putString(SobiDailyTest.KEY_LESSON_SEEN + (uid ?: "guest"), found.sorted().joinToString(","))
        return found
    }

    fun localLevels(): Map<String, Int> =
        SobiDailyTest.LEVEL_KEYS.mapNotNull { k -> prefs.getInt(SobiDailyTest.KEY_LEVEL + k, 0).takeIf { it > 0 }?.let { k to it } }.toMap()

    /** Testten sonra yeni seviyeler cihaza (sunucuya gönderim platformun işi: recordSobiLevels). */
    fun saveLevels(levels: Map<String, Int>) {
        for ((k, v) in levels) prefs.putInt(SobiDailyTest.KEY_LEVEL + k, v)
    }
}

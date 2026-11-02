package com.example.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.app.abacus.SobiTestLevels
import com.example.app.abacus.SobiTestPlanner
import com.example.app.homework.HomeworkPlayerFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Sobi'nin testi: haritadaki Sobi düğmesi ([MapSobiTestButton]) ile oynatıcı arasındaki iş
 * (kullanıcı kararları 01.11.2026: günde 1 test + istenirse ek test, 10 soru, her testte sandık;
 * zorluk çocuğun seviyesine göre, başlamadan Kolay / Benim seviyem / Zor seçilebilir).
 *
 *  - Kural başarısı: önce sunucudaki toplam (homeworkRuleStats/{uid}, son ~50 işlem penceresi; dersler,
 *    ödevler ve Sobi birlikte), yetmezse bu cihazdaki kural hafızası ([SobiRuleStats]).
 *  - Seviye (kural başına 1..20, [SobiTestLevels]): aynı belgenin `sobiLevel` alanında (sunucu yazar:
 *    recordSobiLevels; telefon değişince kaybolmasın, veli panelinde görünsün) ve bu cihazda (çevrimdışı).
 *  - Plan: [SobiTestPlanner] (kural motoru + ödev motoru; yapay zekâ ve kredi yok). Soru üretimi elde
 *    gerektiren kurallarda birkaç yüz ms sürebildiği için arka planda.
 *  - Gün Türkiye saatiyle. Yarıda bırakılan test sayılmaz; yeniden açılınca aynı sorular gelir (tohum
 *    günden ve o gün bitirilen test sayısından).
 *  - Ek test (günün ilk testinden sonrası) 3 anahtar; Pro ve onaylı öğretmen ücretsiz (kullanıcı kararı
 *    01.11.2026). Anahtar sunucuda düşer (startSobiExtraTest); aynı sıra bir kez ödenir, burada da
 *    işaretleniyor ki düğme yarıda bırakılan test için yeniden "3 🔑" yazmasın.
 */
object SobiDailyTest {

    private const val TAG = "SobiDailyTest"
    private const val PREFS = "sobi_test"
    private const val KEY_DAY = "tests_day"
    private const val KEY_COUNT = "tests_count"
    private const val KEY_NUDGE_DAY = "nudge_day"
    private const val KEY_LEVEL = "level_"
    private const val KEY_PAID_DAY = "extra_paid_day"
    private const val KEY_PAID_INDEX = "extra_paid_index"
    /** functions/sobi/wallet.js EXTRA_TEST_KEYS ile aynı (yalnızca düğme metni; bedeli sunucu belirler). */
    const val EXTRA_TEST_KEYS = 3

    /** Plan için gereken her şey: kural başarısı ve kayıtlı seviyeler. */
    data class Info(val scores: List<SobiTestPlanner.RuleScore>, val levels: Map<String, Int>)

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Europe/Istanbul")
    }.format(Date())

    /** Bugün bitirilen test sayısı (günlük + ek testler). */
    fun testsToday(context: Context): Int {
        val p = prefs(context)
        return if (p.getString(KEY_DAY, null) == today()) p.getInt(KEY_COUNT, 0) else 0
    }

    fun isDoneToday(context: Context): Boolean = testsToday(context) > 0

    fun markDone(context: Context) {
        val n = testsToday(context) + 1
        prefs(context).edit().putString(KEY_DAY, today()).putInt(KEY_COUNT, n).apply()
    }

    /**
     * Bugün bitirilen test sayısı değişince [action] (ana iş parçacığında). Dönen dinleyici saklanmalı
     * (SharedPreferences zayıf tutuyor) ve [removeTestsListener] ile bırakılmalı.
     */
    fun addTestsListener(context: Context, action: () -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key == KEY_COUNT) action() }
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
        return listener
    }

    fun removeTestsListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }

    /** Bu ek testin (bugünkü sırası [testsToday]) anahtar bedeli: Pro, öğretmen ya da zaten ödendiyse 0. */
    fun extraTestCost(activity: MainActivity): Int {
        val n = testsToday(activity)
        if (n == 0) return 0
        if (activity.isApprovedTeacher() || PlanStatus.isProPlan(activity.getEnergyManager().getUserPlan())) return 0
        val p = prefs(activity)
        val paid = if (p.getString(KEY_PAID_DAY, null) == today()) p.getInt(KEY_PAID_INDEX, 0) else 0
        return if (n <= paid) 0 else EXTRA_TEST_KEYS
    }

    fun markExtraPaid(context: Context) {
        prefs(context).edit().putString(KEY_PAID_DAY, today()).putInt(KEY_PAID_INDEX, testsToday(context)).apply()
    }

    /** "Senin için bir test hazırladım!" balonu günde bir kez. */
    fun shouldNudgeToday(context: Context): Boolean {
        val p = prefs(context)
        if (p.getString(KEY_NUDGE_DAY, null) == today()) return false
        p.edit().putString(KEY_NUDGE_DAY, today()).apply()
        return true
    }

    /** Kural başarısı ve seviyeler: sunucu, olmazsa bu cihaz. */
    fun load(context: Context, onResult: (Info) -> Unit) {
        val app = context.applicationContext
        val local = SobiRuleStats.snapshot(app).map { SobiTestPlanner.RuleScore(it.key, it.correct, it.wrong) }
        val localLevels = localLevels(app)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onResult(Info(local, localLevels))
            return
        }
        FirebaseFirestore.getInstance().collection("homeworkRuleStats").document(uid).get()
            .addOnSuccessListener { doc ->
                val server = scoresOf(doc.data)
                // Sunucudaki seviye esas (başka cihazda ilerlemiş olabilir); bu cihazda daha yenisi
                // varsa (gönderilemedi) o.
                val serverLevels = levelsOf(doc.data)
                val levels = localLevels + serverLevels.filter { (k, v) -> v >= (localLevels[k] ?: 0) }
                // Bu cihazda olup sunucuya gidemeyen seviye (çevrimdışı test, fonksiyon henüz yoktu):
                // yeniden gönder, veli panelinde görünsün.
                val unsent = localLevels.filter { (k, v) -> v > (serverLevels[k] ?: 0) }
                if (unsent.isNotEmpty()) saveLevels(app, unsent)
                val enough = server.any { it.total >= SobiTestPlanner.MIN_OPS }
                onResult(Info(if (enough) server else local, levels))
            }
            .addOnFailureListener { onResult(Info(local, localLevels)) }
    }

    /** Planı arka planda kurar; sonuç ana iş parçacığında. [offset]: Kolay −1, Benim seviyem 0, Zor +1. */
    fun planAsync(context: Context, info: Info, offset: Int, onResult: (SobiTestPlanner.Plan?) -> Unit) {
        val seed = today().hashCode() * 31 + testsToday(context) * 7919 + offset
        val main = Handler(Looper.getMainLooper())
        Thread {
            val plan = runCatching { SobiTestPlanner.plan(info.scores, info.levels, offset, seed) }
                .onFailure { Log.e(TAG, "plan kurulamadı", it) }.getOrNull()
            main.post { onResult(plan) }
        }.start()
    }

    /** Testten sonra yeni seviyeler: cihaza hemen, sunucuya (veli paneli, başka cihaz) ardından. */
    fun saveLevels(context: Context, levels: Map<String, Int>) {
        if (levels.isEmpty()) return
        val e = prefs(context).edit()
        for ((k, v) in levels) e.putInt(KEY_LEVEL + k, v)
        e.apply()
        if (FirebaseAuth.getInstance().currentUser == null) return
        FirebaseFunctions.getInstance()
            .getHttpsCallable("recordSobiLevels")
            .call(hashMapOf("levels" to levels))
            .addOnFailureListener { Log.w(TAG, "recordSobiLevels başarısız; seviye bu cihazda", it) }
    }

    private fun localLevels(context: Context): Map<String, Int> {
        val p = prefs(context)
        return KEYS.mapNotNull { k -> p.getInt(KEY_LEVEL + k, 0).takeIf { it > 0 }?.let { k to it } }.toMap()
    }

    /**
     * Sunucu belgesinden kural başına son pencerenin toplamı (`recent`), yoksa bütün zamanlar (`tally`).
     * Biçim: functions/homework.js applyBlockToStats.
     */
    private fun scoresOf(data: Map<String, Any?>?): List<SobiTestPlanner.RuleScore> {
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

    /** `sobiLevel` alanı (functions/index.js recordSobiLevels). */
    fun levelsOf(data: Map<String, Any?>?): Map<String, Int> {
        val m = data?.get("sobiLevel") as? Map<*, *> ?: return emptyMap()
        return m.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val level = (v as? Number)?.toInt()?.takeIf { it in 1..SobiTestLevels.MAX } ?: return@mapNotNull null
            key to level
        }.toMap()
    }

    /** Kuralın çocuğun bildiği adı: "10'luk toplama" (SobiCurriculum anahtarı). */
    fun ruleLabel(key: String): String = when (key) {
        "add_direct" -> appString(R.string.sobi_test_rule_add_direct)
        "add_five" -> appString(R.string.sobi_test_rule_add_five)
        "add_ten" -> appString(R.string.sobi_test_rule_add_ten)
        "add_bead" -> appString(R.string.sobi_test_rule_add_bead)
        "sub_direct" -> appString(R.string.sobi_test_rule_sub_direct)
        "sub_five" -> appString(R.string.sobi_test_rule_sub_five)
        "sub_ten" -> appString(R.string.sobi_test_rule_sub_ten)
        "sub_bead" -> appString(R.string.sobi_test_rule_sub_bead)
        else -> key
    }

    /** "2 basamaklı 3 sayı, 2,4 sn" */
    fun specText(level: Int): String {
        val s = SobiTestLevels.spec(level)
        val sec = String.format(Locale("tr", "TR"), "%.1f", s.intervalMs / 1000.0).removeSuffix(",0")
        return appString(R.string.sobi_test_spec, s.digits, s.count, sec)
    }

    fun start(activity: MainActivity, plan: SobiTestPlanner.Plan) {
        activity.showAbacusOverlayFragment(HomeworkPlayerFragment.newSobiTest(plan))
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val KEYS = listOf("add_direct", "add_five", "add_ten", "add_bead", "sub_direct", "sub_five", "sub_ten", "sub_bead")
}

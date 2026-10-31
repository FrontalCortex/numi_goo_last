package com.example.app

import android.content.Context
import com.example.app.abacus.SobiTestPlanner
import com.example.app.homework.HomeworkPlayerFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Sobi'nin günlük testi: haritadaki Sobi düğmesi ([MapSobiTestButton]) ile oynatıcı arasındaki iş
 * (kullanıcı kararı 01.11.2026: günde 1 test, 10 soru, ödül derslerdeki sandık).
 *
 *  - Kural başarısı: önce sunucudaki toplam (homeworkRuleStats/{uid}, son ~50 işlem penceresi; dersler,
 *    ödevler ve Sobi birlikte), yetmezse bu cihazdaki kural hafızası ([SobiRuleStats]).
 *  - Plan: [SobiTestPlanner] (kural motoru; yapay zekâ ve kredi yok).
 *  - Gün Türkiye saatiyle; test bitince o gün "yapıldı" sayılır. Yarıda bırakılan test sayılmaz,
 *    yeniden açılınca aynı gün aynı sorular gelir (tohum günden).
 *  - Oynatıcı: ödev oynatıcısının Sobi testi kipi ([HomeworkPlayerFragment.newSobiTest]).
 */
object SobiDailyTest {

    private const val PREFS = "sobi_test"
    private const val KEY_DONE_DAY = "done_day"
    private const val KEY_NUDGE_DAY = "nudge_day"

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Europe/Istanbul")
    }.format(Date())

    fun isDoneToday(context: Context): Boolean = prefs(context).getString(KEY_DONE_DAY, null) == today()

    fun markDone(context: Context) {
        prefs(context).edit().putString(KEY_DONE_DAY, today()).apply()
    }

    /** "Senin için bir test hazırladım!" balonu günde bir kez. */
    fun shouldNudgeToday(context: Context): Boolean {
        val p = prefs(context)
        if (p.getString(KEY_NUDGE_DAY, null) == today()) return false
        p.edit().putString(KEY_NUDGE_DAY, today()).apply()
        return true
    }

    /** Bugünün planı (aynı gün hep aynı sorular); veri yoksa null. */
    fun loadPlan(context: Context, onResult: (SobiTestPlanner.Plan?) -> Unit) {
        val seed = today().hashCode()
        val local = SobiRuleStats.snapshot(context).map { SobiTestPlanner.RuleScore(it.key, it.correct, it.wrong) }
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onResult(SobiTestPlanner.plan(local, seed))
            return
        }
        FirebaseFirestore.getInstance().collection("homeworkRuleStats").document(uid).get()
            .addOnSuccessListener { doc ->
                val server = scoresOf(doc.data)
                onResult(SobiTestPlanner.plan(server, seed) ?: SobiTestPlanner.plan(local, seed))
            }
            .addOnFailureListener { onResult(SobiTestPlanner.plan(local, seed)) }
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

    fun start(activity: MainActivity, plan: SobiTestPlanner.Plan) {
        activity.showAbacusOverlayFragment(HomeworkPlayerFragment.newSobiTest(plan.questions, plan.weak.map { it.key }))
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

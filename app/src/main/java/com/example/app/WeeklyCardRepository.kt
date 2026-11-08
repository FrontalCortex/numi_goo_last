package com.example.app

import com.example.app.homework.RuleProgress
import com.example.app.model.LessonItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Haftalık ilerleme kartının verisini okur ve [WeeklyCardRules.build]'e verir.
 *
 * Okuma maliyeti (bir açılış): kullanıcı belgesi + son 4 haftalık kayıt + ders kilometre taşları
 * (ders başına bir belge, en çok ~150) + kural ustalığı belgesi. Bölüm ilerlemesi ("12/20 ders")
 * için [ParentReportRepository] ayrıca ~9 sorgu yapıyor; yalnızca bu hafta ders biten bölüm varsa.
 * Kart haftada bir açıldığı için kabul edilebilir.
 */
object WeeklyCardRepository {

    /** Kartın ekranda yazacağı her şey, Firebase'siz. */
    data class Data(
        val card: WeeklyCardRules.Card,
        /** Takma ad (adın ilk kelimesi; soyad karta girmez). */
        val firstName: String,
        /** 13 yaş ve üstü kendisine ("geçen hafta… bu hafta…"), altı veli diliyle üçüncü şahıs. */
        val speaksToChild: Boolean,
        /** Sobi'nin önerdiği kural anahtarı (ör. "add_ten"); veri yoksa null → genel öneri. */
        val suggestion: String?,
        /** Bu hafta ders biten bölümlerin ilerlemesi; okunamadıysa boş. */
        val parts: List<ParentReportRepository.PartGroup>,
        val lessons: Map<String, LessonInfo>,
    )

    /** Dersin adı ve ünitesi ("5'lik Toplama"); beceri adı olarak ünite kullanılıyor (kullanıcı kararı). */
    data class LessonInfo(val title: String, val unit: String?)

    private const val ADULT_AGE = 13

    /** Kayıt hiç yoksa ya da kapanmış hafta yoksa null. */
    suspend fun load(): Data? = coroutineScope {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@coroutineScope null
        val db = FirebaseFirestore.getInstance()
        val user = db.collection("users").document(uid)

        val userDoc = async { user.get().await() }
        // Belge kimliği "2026-W44": sözlük sırası hafta sırası (yıl başında W53 → W01 de doğru, yıl önde).
        val weeks = async {
            user.collection("weeklyStats").orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .limit(4).get().await()
        }
        val milestones = async { user.collection("lessonMilestones").get().await() }
        val rules = async { runCatching { db.collection("homeworkRuleStats").document(uid).get().await() }.getOrNull() }

        val snaps = weeks.await().documents.mapNotNull { parseSnapshot(it) }
        val (week, prev) = WeeklyCardRules.pickWeeks(snaps, WeeklyCardRules.trDay(System.currentTimeMillis()))
            ?: return@coroutineScope null
        val card = WeeklyCardRules.build(week, prev, milestones.await().documents.mapNotNull { parseMilestone(it) })

        val u = userDoc.await()
        val name = u.getString("name").orEmpty().trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        val birthYear = u.getLong("birthYear")?.toInt() ?: -1
        // Doğum yılı yoksa çocuk sayılıyor (AgeConsentManager'daki varsayımla aynı: şüphede CHILD).
        val adult = birthYear > 0 && Calendar.getInstance().get(Calendar.YEAR) - birthYear >= ADULT_AGE

        val touchedParts = card.learned.map { it.partId }.toSet()
        val parts = if (touchedParts.isEmpty()) emptyList() else runCatching { loadParts() }.getOrDefault(emptyList())
            .filter { it.partId in touchedParts }

        val lessonIds = (card.learned.map { it.partId to it.stableId } + card.chests.map { it.partId to it.stableId }).toSet()
        Data(
            card = card,
            firstName = name,
            speaksToChild = adult,
            suggestion = rules.await()?.let { WeeklyCardRules.suggestNext(stages(it)) },
            parts = parts,
            lessons = lessonInfo(lessonIds),
        )
    }

    private suspend fun loadParts(): List<ParentReportRepository.PartGroup> =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            ParentReportRepository.load { report, _ -> if (cont.isActive) cont.resumeWith(Result.success(report?.parts.orEmpty())) }
        }

    /** Müfredattan ders adı ve ünite başlığı (ünite = dersten önceki son TYPE_HEADER). */
    private fun lessonInfo(ids: Set<Pair<Int, String>>): Map<String, LessonInfo> {
        val out = HashMap<String, LessonInfo>()
        for (part in ids.map { it.first }.toSet()) {
            var section: String? = null
            for (item in GlobalLessonData.createLessonItems(part)) {
                if (item.type == LessonItem.TYPE_HEADER) {
                    section = item.title
                    continue
                }
                if ((part to item.stableId) in ids) out[key(part, item.stableId)] = LessonInfo(item.title, item.titleUnit ?: section)
            }
        }
        return out
    }

    fun key(partId: Int, stableId: String) = "${partId}_$stableId"

    /** Kural → ustalık aşaması; ödev başarısı ekranıyla aynı hesap (son ~50 işlem, yoksa toplam). */
    private fun stages(doc: DocumentSnapshot): Map<String, Int> {
        fun cw(v: Any?): RuleProgress.Cw? {
            val m = v as? Map<*, *> ?: return null
            return RuleProgress.Cw((m["c"] as? Number)?.toInt() ?: 0, (m["w"] as? Number)?.toInt() ?: 0)
        }
        val tally = (doc.get("tally") as? Map<*, *>).orEmpty()
        val recent = (doc.get("recent") as? Map<*, *>).orEmpty()
        val level = (doc.get("level") as? Map<*, *>).orEmpty()
        val out = HashMap<String, Int>()
        for (key in WeeklyCardRules.SUGGESTION_ORDER) {
            val window = (recent[key] as? List<*>)?.mapNotNull { cw(it) }?.takeIf { it.isNotEmpty() }
                ?.fold(RuleProgress.Cw.ZERO) { a, b -> a + b } ?: cw(tally[key])
            if (window == null || window.total == 0) continue
            out[key] = maxOf((level[key] as? Number)?.toInt() ?: 0, RuleProgress.stage(window))
        }
        return out
    }

    @Suppress("UNCHECKED_CAST")
    private fun intMap(v: Any?): Map<String, Int> =
        (v as? Map<String, Any?>).orEmpty().mapNotNull { (k, x) -> (x as? Number)?.toInt()?.let { k to it } }.toMap()

    private fun parseSnapshot(d: DocumentSnapshot): WeeklyCardRules.Snapshot? {
        val start = d.getString("weekStart") ?: return null
        val end = d.getString("weekEnd") ?: return null
        val streak = d.get("streak") as? Map<*, *>
        return WeeklyCardRules.Snapshot(
            week = d.getString("week") ?: d.id,
            weekStart = start,
            weekEnd = end,
            cups = intMap(d.get("cups")),
            sobiLevels = intMap(d.get("sobiLevels")),
            streakCurrent = (streak?.get("current") as? Number)?.toInt() ?: 0,
            dailySeconds = intMap(d.get("dailySeconds")),
            weekSeconds = d.getLong("weekSeconds")?.toInt() ?: 0,
        )
    }

    private fun parseMilestone(d: DocumentSnapshot): WeeklyCardRules.Milestone? {
        val part = d.getLong("partId")?.toInt() ?: return null
        val sid = d.getString("stableId") ?: return null
        val chest = d.get("chest") as? Map<*, *>
        return WeeklyCardRules.Milestone(
            partId = part,
            stableId = sid,
            finishedAtMs = d.getLong("finishedAtMs"),
            sampleOp = (d.get("sampleOp") as? Map<*, *>)?.get("text") as? String,
            chestFirstSec = (chest?.get("firstSec") as? Number)?.toInt(),
            chestBestSec = (chest?.get("bestSec") as? Number)?.toInt(),
            chestBestAtMs = (chest?.get("bestAtMs") as? Number)?.toLong(),
        )
    }
}

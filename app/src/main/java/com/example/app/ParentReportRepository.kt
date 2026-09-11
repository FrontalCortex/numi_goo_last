package com.example.app

import android.util.Log
import com.example.app.model.LessonItem
import java.util.Calendar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Veli panelinin okuduğu veriyi tek yerde toplar.
 *
 * ## Neden yeni bir yazma katmanı yok
 * Panelin ihtiyacı olan her şey ZATEN kayıtlı. Öğrenci bir adımı her denediğinde
 * [LessonSuccessRateRepository] şu dokümanı güncelliyor:
 *
 *     users/{uid}/lessonSuccessRateState/{partId}_{stableId}_{step}
 *         attempted  : true
 *         passed     : true/false
 *         failStreak : ilk geçişe kadar kaç kez başarısız olundu
 *
 * Yani "bu adımı kaç denemede çözdü" sorusunun cevabı bugün mevcut — ve geçmişe dönük
 * olarak mevcut. Panel yayına çıktığı gün her kullanıcının geçmişiyle birlikte çalışır.
 *
 * ## Bilinçli olarak göstermediğimiz şey
 * Adım bazında "başarı yüzdesi" (ör. 10 sorunun 7'si doğru) saklanmıyor — o değer yalnızca
 * Analytics'e gidiyor ve oradan geri okunamaz. Panel bu yüzden DENEME SAYISI üzerine kurulu.
 * Veliye "3. denemede geçti" cümlesi "%73 doğruluk"tan zaten daha okunaklı; yüzde ileride
 * saklanmaya başlanırsa ikincil bilgi olarak eklenebilir.
 *
 * ## Okuma maliyeti
 * Bir panel açılışı ~9 sorgu (2 doküman + 7 koleksiyon), toplamda birkaç yüz doküman okuması
 * demek. Panel seyrek açıldığı için kabul edilebilir; sıklaşırsa `lessonProgress/{part}/items`
 * okumaları (yalnızca ölçüm ÖNCESİ ilerleme için gerekli) tamamen düşürülebilir.
 */
object ParentReportRepository {

    private const val TAG = "ParentReport"

    /** Veli paneline giren bölümler. 7-8 yarış, 9 kupa modu — ders ilerlemesi taşımıyorlar. */
    private val REPORT_PARTS = 1..6

    /**
     * Bölüm adları. Müfredatta part'ın kendi başlığı yok (yalnızca ünite başlıkları var),
     * bu yüzden veli için okunur ad burada tanımlı. Yeni bir part açılırsa buraya da bir
     * satır eklenmeli; eksikse panel sadece "Bölüm N" yazar, çökmez.
     */
    private val PART_NAMES = mapOf(
        1 to "Abaküsün Temeli ve Toplama",
        2 to "Abaküste Çıkarma",
        3 to "Abaküste Çarpma",
        4 to "Körleme Toplama",
        5 to "Körleme Çıkarma",
        6 to "Körleme Çarpma",
    )

    enum class StepState {
        /** Geçildi. */
        PASSED,
        /** Denendi ama henüz geçilemedi. */
        IN_PROGRESS,
        /** Öğrenci buraya henüz gelmedi. */
        NOT_REACHED,
    }

    /**
     * @param attempts Toplam deneme sayısı. **null = bilinmiyor**: adım, bu ölçüm eklenmeden
     *   önceki bir sürümde tamamlanmış olabilir. Panelde sayı yerine nötr bir metin gösterilir;
     *   "0 deneme" gibi yanlış bir şey uydurulmaz.
     */
    data class StepRow(
        val step: Int,
        val state: StepState,
        val attempts: Int?,
    )

    data class LessonRow(
        val stableId: String,
        val title: String,
        /** Bu dersin ait olduğu bölüm başlığı (müfredattaki TYPE_HEADER). */
        val sectionTitle: String?,
        val partId: Int,
        val steps: List<StepRow>,
    ) {
        val stepCount: Int get() = steps.size
        val passedSteps: Int get() = steps.count { it.state == StepState.PASSED }
        val isFinished: Boolean get() = stepCount > 0 && passedSteps == stepCount
        /** Bilinen denemelerin toplamı; "en çok zorlanılan ders" bununla seçilir. */
        val totalKnownAttempts: Int get() = steps.sumOf { it.attempts ?: 0 }
    }

    /** Özet karttaki 7 günlük şeridin tek günü. */
    data class DayTime(
        /** `yyyyMMdd` (bkz. [TimeTracker.dayKey]). */
        val key: String,
        /** Şeritte yazan kısa gün adı: "Pzt", "Sal"... */
        val label: String,
        val seconds: Long,
    )

    data class Summary(
        val finishedLessons: Int,
        /** Müfredattaki toplam ders sayısı — velinin ölçeği görmesi için. */
        val totalLessons: Int,
        val totalTimeSeconds: Long,
        /** Bugünkü çalışma süresi; [lastWeek]'in son elemanıyla aynıdır. */
        val todaySeconds: Long,
        /** Eskiden yeniye 7 gün; bugün her zaman sonuncu. */
        val lastWeek: List<DayTime>,
        val streakDays: Int,
        /** En çok deneme gerektiren, en az bir kez geçilmiş ders. Yoksa null. */
        val hardestLesson: LessonRow?,
    )

    /**
     * Müfredattaki bir ünite (TYPE_HEADER) ve altındaki dersler.
     *
     * [lessons] yalnızca öğrencinin DOKUNDUĞU dersleri taşır; [totalLessons] ise ünitedeki
     * tüm dersleri sayar. İkisinin ayrı olması "4/7 ders" gibi dürüst bir oran verir —
     * dokunulmamış dersler listede görünmez ama paydadan düşmez.
     */
    data class SectionGroup(
        val title: String?,
        val totalLessons: Int,
        val lessons: List<LessonRow>,
    ) {
        val finishedLessons: Int get() = lessons.count { it.isFinished }
    }

    /** Bir part ve altındaki üniteler. Dokunulmamış üniteler [sections] içinde yer almaz. */
    data class PartGroup(
        val partId: Int,
        /** Veliye gösterilen bölüm adı, ör. "Abaküsün Temeli ve Toplama". Tanımsızsa null. */
        val name: String?,
        val totalLessons: Int,
        val sections: List<SectionGroup>,
    ) {
        val finishedLessons: Int get() = sections.sumOf { it.finishedLessons }
    }

    /** [parts] müfredat sırasındadır ve yalnızca öğrencinin dokunduğu bölümleri içerir. */
    data class Report(
        val summary: Summary,
        val parts: List<PartGroup>,
    ) {
        /** Panelde gösterilecek hiçbir ders yoksa true. */
        val isEmpty: Boolean get() = parts.isEmpty()
    }

    // ── Doküman kimliği çözümleme ───────────────────────────────────────────
    //
    // lessonSuccessRateState anahtarı iki biçimde olabilir:
    //   yeni : "{partId}_{stableId}_{step}"   → 1_p1_i06_kuralsiz_toplama_1_basamakli_1
    //   eski : "{partId}_{position}_{step}"   → 1_6_1
    //
    // stableId'nin kendisi alt çizgi içerdiği için basit split çalışmaz. Ama partId ve step
    // her zaman SAYI olduğundan baştan ve sondan kırpmak belirsizlik bırakmaz. Ortadaki parça
    // sayıysa eski biçimdir (o zaman şablondan stableId'ye çevrilir).

    private data class StateKey(val partId: Int, val stableId: String, val step: Int)

    private fun parseStateKey(docId: String, stableIdOfPosition: (Int, Int) -> String?): StateKey? {
        val partId = docId.substringBefore('_').toIntOrNull() ?: return null
        val step = docId.substringAfterLast('_').toIntOrNull() ?: return null
        val middle = docId.substringAfter('_').substringBeforeLast('_')
        if (middle.isEmpty()) return null
        val legacyPosition = middle.toIntOrNull()
        val stableId = if (legacyPosition != null) {
            stableIdOfPosition(partId, legacyPosition) ?: return null
        } else {
            middle
        }
        return StateKey(partId, stableId, step)
    }

    // ── Yükleme ─────────────────────────────────────────────────────────────

    /**
     * Raporu kurar. Tüm okumalar kullanıcının kendi dokümanları üzerindedir; ek güvenlik
     * kuralı gerekmez.
     */
    fun load(onResult: (Report?, Exception?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(null, IllegalStateException("Oturum açık değil"))
            return
        }
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(uid)

        // Şablonlar önce: hem başlıklar hem eski anahtar çözümü için gerekli.
        val templates: Map<Int, List<LessonItem>> =
            REPORT_PARTS.associateWith { GlobalLessonData.createLessonItems(it) }
        val stableIdOfPosition: (Int, Int) -> String? = { partId, position ->
            templates[partId]?.getOrNull(position)?.stableId
        }

        var totalTimeSeconds = 0L
        var dailySeconds: Map<String, Long> = emptyMap()
        var streakDays = 0
        var stateByKey: Map<StateKey, Map<String, Any?>> = emptyMap()
        val progressByPart = HashMap<Int, Map<String, Map<String, Any?>>>()

        var pending = 3 + REPORT_PARTS.count()
        var failure: Exception? = null

        fun finishOne() {
            pending--
            if (pending > 0) return
            if (failure != null) {
                onResult(null, failure)
                return
            }
            onResult(
                build(templates, stateByKey, progressByPart, totalTimeSeconds, dailySeconds, streakDays),
                null,
            )
        }

        userDoc.get()
            .addOnSuccessListener { d ->
                totalTimeSeconds = d.getLong("totalTimeSpent") ?: 0L
                // Günlük süre ayrı bir okuma değil: aynı dokümanda alan olarak duruyor
                // (bkz. TimeTracker.DAILY_FIELD).
                dailySeconds = (d.get(TimeTracker.DAILY_FIELD) as? Map<*, *>)
                    ?.mapNotNull { (k, v) ->
                        val key = k as? String ?: return@mapNotNull null
                        val seconds = (v as? Number)?.toLong() ?: return@mapNotNull null
                        key to seconds
                    }
                    ?.toMap()
                    .orEmpty()
                finishOne()
            }
            .addOnFailureListener { e ->
                // Süre kritik değil; rapor onsuz da anlamlı. Hata raporu düşürmez.
                Log.w(TAG, "totalTimeSpent okunamadı", e); finishOne()
            }

        userDoc.collection("badgeProgress").document("state").get()
            .addOnSuccessListener { d -> streakDays = (d.get("userFishingStreak") as? Number)?.toInt() ?: 0; finishOne() }
            .addOnFailureListener { e -> Log.w(TAG, "seri okunamadı", e); finishOne() }

        userDoc.collection("lessonSuccessRateState").get()
            .addOnSuccessListener { snap ->
                stateByKey = snap.documents.mapNotNull { d ->
                    parseStateKey(d.id, stableIdOfPosition)?.let { it to d.data.orEmpty() }
                }.toMap()
                finishOne()
            }
            .addOnFailureListener { e ->
                // Bu olmadan deneme sayısı gösterilemez; rapor yine de ilerleme listesiyle çıkar.
                Log.w(TAG, "lessonSuccessRateState okunamadı", e); finishOne()
            }

        REPORT_PARTS.forEach { partId ->
            userDoc.collection("lessonProgress").document(partId.toString())
                .collection("items").get()
                .addOnSuccessListener { snap ->
                    progressByPart[partId] = snap.documents.associate { it.id to it.data.orEmpty() }
                    finishOne()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "lessonProgress part=$partId okunamadı", e)
                    failure = e
                    finishOne()
                }
        }
    }

    private fun build(
        templates: Map<Int, List<LessonItem>>,
        stateByKey: Map<StateKey, Map<String, Any?>>,
        progressByPart: Map<Int, Map<String, Map<String, Any?>>>,
        totalTimeSeconds: Long,
        dailySeconds: Map<String, Long>,
        streakDays: Int,
    ): Report {
        val parts = mutableListOf<PartGroup>()
        val allLessons = mutableListOf<LessonRow>()
        var totalLessons = 0

        REPORT_PARTS.forEach { partId ->
            val template = templates[partId] ?: return@forEach
            val progress = progressByPart[partId].orEmpty()

            val sections = mutableListOf<SectionGroup>()
            var currentSection: String? = null
            var sectionTotal = 0
            var sectionLessons = mutableListOf<LessonRow>()
            var partTotal = 0

            // Biriken üniteyi kapat. Dokunulmamış ünite listeye girmez ama dersleri
            // bölüm toplamından düşmez: "4/7" ifadesinin paydası müfredat, ilerleme değil.
            fun closeSection() {
                if (sectionLessons.isNotEmpty()) {
                    sections += SectionGroup(currentSection, sectionTotal, sectionLessons)
                }
                sectionLessons = mutableListOf()
                sectionTotal = 0
            }

            template.forEach { item ->
                if (item.type == LessonItem.TYPE_HEADER) {
                    closeSection()
                    currentSection = item.title
                    return@forEach
                }
                if (item.type != LessonItem.TYPE_LESSON && item.type != LessonItem.TYPE_CHEST) return@forEach
                totalLessons++
                partTotal++
                sectionTotal++

                val itemProgress = progress[item.stableId].orEmpty()
                val finishedByProgress = itemProgress["stepIsFinish"] as? Boolean ?: false
                // Adım bazında tamamlanma; ölçüm eklenmeden önce bitirilmiş adımlar için tek kanıt.
                val completionFlags = itemProgress["stepCompletionStatus"] as? List<*>

                // DİKKAT: `currentStep` burada kullanılmaz. seedAllLessonProgressIfMissing her
                // ders için şablon değerleriyle (currentStep = 1) doküman yazıyor; yani dokümanın
                // VARLIĞI öğrencinin o derse girdiği anlamına GELMİYOR. "Dokunulmuş" kanıtı
                // yalnızca ölçüm kaydı (lessonSuccessRateState) veya tamamlanma bayrağıdır.
                val stepCount = item.stepCount.coerceAtLeast(1)
                val steps = (1..stepCount).map { step ->
                    val data = stateByKey[StateKey(partId, item.stableId, step)]
                    val passed = data?.get("passed") as? Boolean ?: false
                    val attempted = data?.get("attempted") as? Boolean ?: false
                    val failStreak = (data?.get("failStreak") as? Number)?.toInt() ?: 0
                    val flaggedComplete = completionFlags?.getOrNull(step - 1) as? Boolean ?: false

                    when {
                        passed -> StepRow(step, StepState.PASSED, failStreak + 1)
                        // Ölçüm kaydı yok ama ilerleme "bitti" diyorsa: eski sürümde tamamlanmış.
                        // Geçildiğini biliyoruz, kaç denemede olduğunu bilmiyoruz.
                        flaggedComplete || finishedByProgress -> StepRow(step, StepState.PASSED, null)
                        attempted -> StepRow(step, StepState.IN_PROGRESS, failStreak)
                        else -> StepRow(step, StepState.NOT_REACHED, null)
                    }
                }

                // Öğrencinin hiç dokunmadığı dersler panele girmez: veli neyi yaptığını görmek
                // ister, yapmadığı 90 dersi taramak değil. Ölçek başlık satırlarında veriliyor.
                if (steps.any { it.state != StepState.NOT_REACHED }) {
                    val row = LessonRow(
                        stableId = item.stableId,
                        title = item.title,
                        sectionTitle = currentSection,
                        partId = partId,
                        steps = steps,
                    )
                    sectionLessons.add(row)
                    allLessons += row
                }
            }

            closeSection()
            if (sections.isNotEmpty()) {
                parts += PartGroup(partId, PART_NAMES[partId], partTotal, sections)
            }
        }

        // Ölçüt mutlak deneme sayısı DEĞİL, gereğinden fazla deneme: 6 adımlık bir dersi 6
        // denemede bitirmek kusursuz, 3 adımlık dersi 5 denemede bitirmek zorlanmadır.
        val hardest = allLessons
            .filter { it.isFinished }
            .maxByOrNull { it.totalKnownAttempts - it.stepCount }
            ?.takeIf { it.totalKnownAttempts > it.stepCount }

        // Henüz buluta gitmemiş oturum süresi: ProfileFragment de aynı toplamı gösteriyor,
        // iki ekranın farklı sayı söylemesi veliyi haklı olarak şüphelendirirdi.
        val pending = TimeTracker.unsyncedTimeFlow.value.coerceAtLeast(0L)
        val week = lastWeek(dailySeconds, pending)

        return Report(
            summary = Summary(
                finishedLessons = allLessons.count { it.isFinished },
                totalLessons = totalLessons,
                totalTimeSeconds = totalTimeSeconds + pending,
                todaySeconds = week.lastOrNull()?.seconds ?: 0L,
                lastWeek = week,
                streakDays = streakDays,
                hardestLesson = hardest,
            ),
            parts = parts,
        )
    }

    /** Kısa gün adları; [Calendar.DAY_OF_WEEK] 1=Pazar olduğu için dizi de öyle başlıyor. */
    private val DAY_LABELS = arrayOf("Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")

    /**
     * Eskiden yeniye 7 gün. Veri olmayan gün 0 saniyeyle yer tutar: boşluk da bilgidir
     * ("salı günü hiç çalışmamış" satırdan düşerse görünmez olurdu).
     *
     * [pendingSeconds] yalnızca bugüne eklenir; henüz senkronize edilmemiş oturumdur.
     */
    private fun lastWeek(dailySeconds: Map<String, Long>, pendingSeconds: Long): List<DayTime> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        return (0..6).map { i ->
            val key = TimeTracker.dayKey(i - 6)
            val label = DAY_LABELS[cal.get(Calendar.DAY_OF_WEEK) - 1]
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val base = dailySeconds[key] ?: 0L
            DayTime(key, label, if (i == 6) base + pendingSeconds else base)
        }
    }
}

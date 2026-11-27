package com.sorobit.shared.ders

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Firestore yazmalarını ve liderlik gönderimlerini kaydeden sahte köprü. */
internal class KayitSink : LessonProgressSink {
    data class Tekli(val partId: Int, val stableId: String, val fields: Map<String, Any?>)
    data class Toplu(val partId: Int, val stableIds: List<String>, val itemsFields: List<Map<String, Any?>>)
    data class Goc(val partId: Int, val stableIds: List<String>, val itemsFields: List<Map<String, Any?>>, val eskiler: List<String>)

    val tekliler = mutableListOf<Tekli>()
    val toplular = mutableListOf<Toplu>()
    val goculer = mutableListOf<Goc>()
    val gonderimler = mutableListOf<GlobalLessonData.LeaderboardSubmit>()

    /** Çağrı sırasını izlemek için: "tek:<stableId>", "lb:<stableId>", "toplu:1", "goc:1". */
    val sira = mutableListOf<String>()

    override fun writeSingleItem(partId: Int, stableId: String, fields: Map<String, Any?>) {
        tekliler.add(Tekli(partId, stableId, fields))
        sira.add("tek:$stableId")
    }

    override fun writeAllItems(partId: Int, stableIds: List<String>, itemsFields: List<Map<String, Any?>>) {
        toplular.add(Toplu(partId, stableIds, itemsFields))
        sira.add("toplu:$partId")
    }

    override fun migrateLegacyItemKeys(
        partId: Int,
        stableIds: List<String>,
        itemsFields: List<Map<String, Any?>>,
        legacyDocIds: List<String>,
    ) {
        goculer.add(Goc(partId, stableIds, itemsFields, legacyDocIds))
        sira.add("goc:$partId")
    }

    override fun submitBestIfNeeded(partId: Int, lessonKey: String, score: Int, season: Int, titleUnit: String?) {
        gonderimler.add(GlobalLessonData.LeaderboardSubmit(partId, lessonKey, score, season, titleUnit))
        sira.add("lb:$lessonKey")
    }
}

/** Katalogdaki item kimliği (testlerde kısaltma). */
internal fun kimlik(part: Int, index: Int): String = GlobalLessonData.createLessonItems(part)[index].stableId

/** Harita / yarış listesi köprülerinin çağrılarını kaydeder. */
internal class KayitAdapter(var racePanelAcik: Boolean = false) :
    LessonManager.LessonAdapterBridge, LessonManager.RaceAdapterBridge {
    val guncellenen = mutableListOf<Pair<Int, LessonItem>>()
    val yarisGuncellenen = mutableListOf<Pair<Int, LessonItem>>()
    var tumListeSayisi = 0
    var sonListe: List<LessonItem>? = null

    override fun updateLessonItem(position: Int, newItem: LessonItem) {
        guncellenen.add(position to newItem)
    }

    override fun updateItems(newItems: List<LessonItem>) {
        tumListeSayisi++
        sonListe = newItems.toList()
    }

    override fun isRacePanelOpen(): Boolean = racePanelAcik

    override fun updateRaceItem(position: Int, item: LessonItem) {
        yarisGuncellenen.add(position to item)
    }
}

class GlobalLessonDataTest {

    private lateinit var sink: KayitSink

    @BeforeTest
    fun sifirla() {
        GlobalLessonData.resetForTest()
        LessonManager.resetForTest()
        sink = KayitSink()
        GlobalLessonData.sink = sink
    }

    private fun yukle(partId: Int) {
        GlobalLessonData.initialize(partId)
        GlobalLessonData.onLoadFromFirestore(partId, GlobalLessonData.createLessonItems(partId), error = false)
    }

    // ---- Sabitler / yollar ----

    @Test
    fun firestoreYollariVeSabitler() {
        assertEquals("users", GlobalLessonData.FIRESTORE_USERS)
        assertEquals("lessonProgress", GlobalLessonData.FIRESTORE_LESSON_PROGRESS)
        assertEquals("items", GlobalLessonData.FIRESTORE_LESSON_ITEMS)
        assertEquals("users/U1/lessonProgress/3/items", GlobalLessonData.itemsCollectionPath("U1", 3))
        assertEquals("users/U1/lessonProgress/3/items/p3_i04_unite_maratonu", GlobalLessonData.itemDocumentPath("U1", 3, "p3_i04_unite_maratonu"))
        assertEquals("users/U1/lessonProgress/3/items/17", GlobalLessonData.legacyItemDocumentPath("U1", 3, 17))
        assertEquals("teacherLessonsUnlocked", GlobalLessonData.FIELD_TEACHER_LESSONS_UNLOCKED)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8), GlobalLessonData.SEED_PART_IDS)
        assertEquals(1500L, GlobalLessonData.AUTH_WAIT_TIMEOUT_MS)
        assertEquals(setOf(1, 2, 3, 6, 7, 8), GlobalLessonData.LEADERBOARD_SUPPORTED_PARTS)
        assertEquals("AppPrefs", GlobalLessonData.APP_PREFS_NAME)
        assertEquals("tutorial1_login_flow_pending", GlobalLessonData.KEY_TUTORIAL1_LOGIN_FLOW_PENDING)
        assertEquals(1, GlobalLessonData.globalPartId)
        assertTrue(GlobalLessonData.lessonItems.isEmpty())
    }

    @Test
    fun yerelOnbellekAnahtarlari() {
        assertEquals("LessonPrefs_", GlobalLessonData.PREFS_PREFIX)
        assertEquals("lesson_items", GlobalLessonData.KEY_LESSON_ITEMS)
        assertEquals("lesson_items_part_4", GlobalLessonData.getKey(4))
        assertEquals("LessonPrefs_guest", GlobalLessonData.lessonPrefsName(null))
        assertEquals("LessonPrefs_abc", GlobalLessonData.lessonPrefsName("abc"))
        assertEquals("lesson_prefs", LessonDataManager.PREFS_NAME)
        assertEquals("lesson_items", LessonDataManager.KEY_LESSON_ITEMS)
    }

    // ---- progressFieldsOf / applyProgressFields ----

    @Test
    fun progressFieldsOf_dersteNullAlanlarYazilmaz() {
        val item = GlobalLessonData.createLessonItems(1)[1]
        val f = GlobalLessonData.progressFieldsOf(item)
        assertEquals(
            setOf(
                "stepIsFinish", "currentStep", "stepCompletionStatus", "isCompleted", "finalGoldVisualUnlocked",
                "tutorialIsFinish", "tutorialNumber", "stepCupIcon", "progressBarLevel",
            ),
            f.keys,
        )
        assertEquals(false, f["stepIsFinish"])
        assertEquals(1, f["currentStep"])
        assertEquals(listOf(false, false), f["stepCompletionStatus"])
        assertEquals(true, f["isCompleted"])
        assertEquals(false, f["finalGoldVisualUnlocked"])
        assertEquals(false, f["tutorialIsFinish"])
        assertEquals(1, f["tutorialNumber"])
        assertEquals("chest_stars_tier0", f["stepCupIcon"])
        assertEquals(0, f["progressBarLevel"])
    }

    @Test
    fun progressFieldsOf_sandikVeTumAlanlar() {
        val sablon = GlobalLessonData.createLessonItems(1)[4]
        assertEquals(
            setOf(
                "stepIsFinish", "currentStep", "stepCompletionStatus", "isCompleted", "finalGoldVisualUnlocked",
                "tutorialIsFinish", "tutorialNumber", "stepCupIcon", "progressBarLevel",
                "worstCupTime", "cupPoint1", "cupPoint2",
            ),
            GlobalLessonData.progressFieldsOf(sablon).keys,
        )
        val dolu = sablon.copy(
            raceBusyLevel = 0, record = 1700, leaderboardSeasonId = 7, leaderboardSeasonBest = 1650,
            cupDifficultyLevel = 2, cupWinDelta = 30, cupLossDelta = 12,
        )
        val f = GlobalLessonData.progressFieldsOf(dolu)
        // Android'deki 19 alanın tamamı ve yalnızca onlar
        assertEquals(
            listOf(
                "stepIsFinish", "currentStep", "stepCompletionStatus", "isCompleted", "finalGoldVisualUnlocked",
                "tutorialIsFinish", "tutorialNumber", "raceBusyLevel", "record", "leaderboardSeasonId",
                "leaderboardSeasonBest", "stepCupIcon", "cupDifficultyLevel", "cupWinDelta", "cupLossDelta",
                "worstCupTime", "cupPoint1", "cupPoint2", "progressBarLevel",
            ),
            f.keys.toList(),
        )
        assertEquals(0, f["raceBusyLevel"])
        assertEquals(1700, f["record"])
        assertEquals(7, f["leaderboardSeasonId"])
        assertEquals(1650, f["leaderboardSeasonBest"])
        assertEquals(150, f["worstCupTime"])
        assertEquals(1600, f["cupPoint1"])
        assertEquals(1300, f["cupPoint2"])
        // Statik alanlar yazılmaz
        assertFalse(f.containsKey("title"))
        assertFalse(f.containsKey("type"))
        assertFalse(f.containsKey("stepCount"))
        assertFalse(f.containsKey("startStepNumber"))
    }

    @Test
    fun progressFieldsOf_stepCupIconKodlamasiDegistirilebilir() {
        val item = sandik()
        assertEquals(42, GlobalLessonData.progressFieldsOf(item) { 42 }["stepCupIcon"])
        // null dönerse alan hiç yazılmaz
        assertFalse(GlobalLessonData.progressFieldsOf(item) { null }.containsKey("stepCupIcon"))
    }

    @Test
    fun applyProgressFields_bosHaritaAyniNesne() {
        val t = ders()
        assertSame(t, GlobalLessonData.applyProgressFields(t, emptyMap()))
    }

    @Test
    fun applyProgressFields_firestoreTurleri() {
        val t = GlobalLessonData.createLessonItems(1)[4]
        // Firestore sayıları Long (bazen Double) verir
        val data = mapOf<String, Any?>(
            "stepIsFinish" to true,
            "currentStep" to 1L,
            "stepCompletionStatus" to listOf(true),
            "isCompleted" to true,
            "finalGoldVisualUnlocked" to true,
            "tutorialIsFinish" to true,
            "tutorialNumber" to 3L,
            "raceBusyLevel" to 0L,
            "record" to 1700.0,
            "leaderboardSeasonId" to 7L,
            "leaderboardSeasonBest" to 1650L,
            "stepCupIcon" to "chest_stars_tier3",
            "cupDifficultyLevel" to 2L,
            "cupWinDelta" to 30L,
            "cupLossDelta" to 12L,
            "worstCupTime" to 99L,
            "cupPoint1" to 1111L,
            "cupPoint2" to 999L,
            "progressBarLevel" to 4L,
        )
        val r = GlobalLessonData.applyProgressFields(t, data)
        assertEquals(
            t.copy(
                stepIsFinish = true, currentStep = 1, stepCompletionStatus = listOf(true), isCompleted = true,
                finalGoldVisualUnlocked = true, tutorialIsFinish = true, tutorialNumber = 3, raceBusyLevel = 0,
                record = 1700, leaderboardSeasonId = 7, leaderboardSeasonBest = 1650,
                stepCupIcon = "chest_stars_tier3", cupDifficultyLevel = 2, cupWinDelta = 30, cupLossDelta = 12,
                worstCupTime = 99, cupPoint1 = 1111, cupPoint2 = 999, progressBarLevel = 4,
            ),
            r,
        )
        // Statik alanlar şablondan
        assertEquals("Ünite Maratonu", r.title)
        assertEquals(1005, r.startStepNumber)
        // Kimlik şablondan gelir, belgeden değil
        assertEquals("p1_i04_unite_maratonu", r.stableId)
    }

    @Test
    fun applyProgressFields_eksikVeYanlisTurdeSablonKorunur() {
        val t = ders(stepCount = 3, currentStep = 2).copy(
            stepCompletionStatus = listOf(true, false, false), tutorialNumber = 5, record = 10,
        )
        // Yalnızca bir alan var
        val r1 = GlobalLessonData.applyProgressFields(t, mapOf("currentStep" to 3L))
        assertEquals(t.copy(currentStep = 3), r1)
        // Yanlış türler: şablon değeri kalır
        val r2 = GlobalLessonData.applyProgressFields(
            t,
            mapOf(
                "stepIsFinish" to "true",
                "currentStep" to "3",
                "stepCompletionStatus" to "x",
                "isCompleted" to 1L,
                "tutorialNumber" to null,
                "record" to true,
                "stepCupIcon" to 2131165400L, // Android'in yazdığı R.drawable kimliği: çözülemez
            ),
        )
        assertEquals(t, r2)
        // Listede Boolean olmayan öğeler false sayılır; uzunluk şablona uydurulmaz
        val r3 = GlobalLessonData.applyProgressFields(t, mapOf("stepCompletionStatus" to listOf(true, "x", null, true)))
        assertEquals(listOf(true, false, false, true), r3.stepCompletionStatus)
        // Double kesirli sayı toInt ile kırpılır
        assertEquals(2, GlobalLessonData.applyProgressFields(t, mapOf("currentStep" to 2.9)).currentStep)
    }

    @Test
    fun applyProgressFields_stepCupIconOkuyucu() {
        val t = sandik()
        // Boş String → şablon
        assertEquals("chest_stars_tier0", GlobalLessonData.applyProgressFields(t, mapOf("stepCupIcon" to "")).stepCupIcon)
        assertEquals("cup_ic2", GlobalLessonData.applyProgressFields(t, mapOf("stepCupIcon" to "cup_ic2")).stepCupIcon)
        // Özel çözücü (ör. Android sayısal kimlik tablosu)
        val r = GlobalLessonData.applyProgressFields(t, mapOf("stepCupIcon" to 7L)) { raw ->
            if ((raw as? Number)?.toInt() == 7) "chest_stars_tier2" else null
        }
        assertEquals("chest_stars_tier2", r.stepCupIcon)
        assertNull(GlobalLessonData.stepCupIconVarsayilanOku(5))
        assertNull(GlobalLessonData.stepCupIconVarsayilanOku(null))
        assertEquals("x", GlobalLessonData.stepCupIconVarsayilanOku("x"))
    }

    @Test
    fun yazOkuGidisDonus() {
        val sablon = GlobalLessonData.createLessonItems(1)
        val degisen = sablon[4].copy(
            stepIsFinish = true, stepCompletionStatus = listOf(true), record = 1650, leaderboardSeasonId = 9,
            leaderboardSeasonBest = 1650, stepCupIcon = "chest_stars_tier3", finalGoldVisualUnlocked = true,
        )
        val geri = GlobalLessonData.applyProgressFields(sablon[4], GlobalLessonData.progressFieldsOf(degisen))
        assertEquals(degisen, geri)
        // Şablonun kendisi de gidiş-dönüşte değişmez (tüm part'lar)
        for (part in 1..9) {
            for (item in GlobalLessonData.createLessonItems(part)) {
                assertEquals(item, GlobalLessonData.applyProgressFields(item, GlobalLessonData.progressFieldsOf(item)))
            }
        }
    }

    @Test
    fun stepCupIconKayittanTuret() {
        GlobalLessonData.globalPartId = 1
        val bitmis = sandik(record = 1300).copy(stepIsFinish = true)
        assertEquals("chest_stars_tier2", GlobalLessonData.stepCupIconKayittanTuret(bitmis).stepCupIcon)
        // Bitmemiş sandık ve ders değişmez
        val bitmemis = sandik(record = 1300)
        assertSame(bitmemis, GlobalLessonData.stepCupIconKayittanTuret(bitmemis))
        val d = ders().copy(stepIsFinish = true)
        assertSame(d, GlobalLessonData.stepCupIconKayittanTuret(d))
        // record null → 0 → tier0
        assertEquals(
            "chest_stars_tier0",
            GlobalLessonData.stepCupIconKayittanTuret(sandik().copy(stepIsFinish = true)).stepCupIcon,
        )
    }

    // ---- Şablon + belge birleştirme ----

    @Test
    fun mergeTemplateWithDocs() {
        val docs = mapOf<String, Map<String, Any?>?>(
            "1" to mapOf("currentStep" to 2L, "stepCompletionStatus" to listOf(true, false)),
            "abc" to mapOf("currentStep" to 9L),   // kimlik sayı değil → atlanır
            "99" to mapOf("currentStep" to 9L),    // şablon dışı indeks → yok sayılır
            "2" to null,                           // verisi olmayan belge → şablon
            "3" to emptyMap(),                     // boş belge → şablon
        )
        val r = GlobalLessonData.mergeTemplateWithDocs(1, docs)
        val sablon = GlobalLessonData.createLessonItems(1)
        assertEquals(33, r.size)
        assertEquals(sablon[1].copy(currentStep = 2, stepCompletionStatus = listOf(true, false)), r[1])
        assertEquals(sablon[0], r[0])
        assertEquals(sablon[2], r[2])
        assertEquals(sablon[3], r[3])
        assertEquals(sablon.drop(2), r.drop(2))
    }

    @Test
    fun mergeTemplateWithDocs_stableIdAnahtari_ustunlukVeEskiBicim() {
        val sablon = GlobalLessonData.createLessonItems(1)
        val docs = mapOf<String, Map<String, Any?>?>(
            // Yeni biçim: belge kimliği = stableId
            kimlik(1, 2) to mapOf("currentStep" to 2L),
            // Eski biçim: belge kimliği = sıra ("3") → yalnızca stableId belgesi yoksa kullanılır
            "3" to mapOf("currentStep" to 2L),
            "2" to mapOf("currentStep" to 9L),          // idx 2'nin stableId belgesi var → eski "2" yok sayılır
            "p1_i99_olmayan" to mapOf("currentStep" to 9L), // şablonda olmayan kimlik → yok sayılır
        )
        val r = GlobalLessonData.mergeTemplateWithDocs(1, docs)
        assertEquals(33, r.size)
        assertEquals(2, r[2].currentStep)             // stableId belgesi kazandı (9 değil)
        assertEquals(2, r[3].currentStep)             // eski sıra anahtarı
        assertEquals(sablon[1], r[1])
        assertEquals(sablon.drop(4), r.drop(4))
    }

    @Test
    fun legacyMigrationPlan_eskiBelgeVarsaYeniAnahtaraTasir() {
        val docs = mapOf<String, Map<String, Any?>?>(
            "1" to mapOf("currentStep" to 2L, "stepCompletionStatus" to listOf(true, false)),
            "4" to mapOf("stepIsFinish" to true),
            kimlik(1, 6) to mapOf("currentStep" to 2L), // zaten yeni biçim: göçe girmez
        )
        val merged = GlobalLessonData.lessonItemsFromFirestoreDocs(1, docs)!!
        val plan = GlobalLessonData.legacyMigrationPlan(docs, merged)!!
        assertEquals(listOf("1", "4"), plan.legacyDocIds.sorted())
        assertEquals(listOf(kimlik(1, 1), kimlik(1, 4)), plan.writes.map { it.stableId })
        assertEquals(GlobalLessonData.progressFieldsOf(merged[1]), plan.writes[0].fields)
        assertEquals(2, plan.writes[0].fields["currentStep"])
        // Eski anahtarlı belge yoksa plan yok
        assertNull(GlobalLessonData.legacyMigrationPlan(mapOf(kimlik(1, 1) to mapOf("currentStep" to 2L)), merged))
        assertNull(GlobalLessonData.legacyMigrationPlan(emptyMap(), merged))
    }

    @Test
    fun lessonItemsFromFirestoreDocs_bosKoleksiyonNull() {
        assertNull(GlobalLessonData.lessonItemsFromFirestoreDocs(1, emptyMap()))
        val r = GlobalLessonData.lessonItemsFromFirestoreDocs(2, mapOf("0" to mapOf("isCompleted" to true)))
        assertNotNull(r)
        assertEquals(20, r.size)
        assertEquals(true, r[0].isCompleted)
    }

    // ---- Yükleme durumu ----

    @Test
    fun onLoadFromFirestore_dallar() {
        GlobalLessonData.initialize(2)
        assertEquals(2, GlobalLessonData.globalPartId)
        val liste = GlobalLessonData.createLessonItems(2)
        // Part bu arada değişmiş → ERROR, liste yazılmaz
        assertEquals(GlobalLessonData.FirestoreLoadStatus.ERROR, GlobalLessonData.onLoadFromFirestore(3, liste, false))
        assertTrue(GlobalLessonData.lessonItems.isEmpty())
        // Hata → ERROR
        assertEquals(GlobalLessonData.FirestoreLoadStatus.ERROR, GlobalLessonData.onLoadFromFirestore(2, null, true))
        assertEquals(GlobalLessonData.FirestoreLoadStatus.ERROR, GlobalLessonData.onLoadFromFirestore(2, liste, true))
        assertTrue(GlobalLessonData.lessonItems.isEmpty())
        // Boş koleksiyon → NOT_FOUND
        assertEquals(GlobalLessonData.FirestoreLoadStatus.NOT_FOUND, GlobalLessonData.onLoadFromFirestore(2, null, false))
        // Veri var → LOADED
        assertEquals(GlobalLessonData.FirestoreLoadStatus.LOADED, GlobalLessonData.onLoadFromFirestore(2, liste, false))
        // Android: yüklenen liste withDerivedUnlocks'tan geçer → part 2'nin ilk dersi (önceki yok) açılır
        assertEquals(GlobalLessonData.withDerivedUnlocks(2, liste), GlobalLessonData.lessonItems)
        assertEquals(false, liste[1].isCompleted)
        assertEquals(true, GlobalLessonData.getLessonItem(1)?.isCompleted)
        assertEquals(false, GlobalLessonData.getLessonItem(2)?.isCompleted)
    }

    @Test
    fun applyLoadStatus_dallar() {
        GlobalLessonData.initialize(1)
        // LOADED: hiçbir şey yapılmaz
        GlobalLessonData.applyLoadStatus(GlobalLessonData.FirestoreLoadStatus.LOADED, 1, false)
        assertTrue(GlobalLessonData.lessonItems.isEmpty())
        assertTrue(sink.sira.isEmpty())
        // ERROR: varsayılan liste, buluta yazılmaz
        GlobalLessonData.applyLoadStatus(GlobalLessonData.FirestoreLoadStatus.ERROR, 1, false)
        assertEquals(33, GlobalLessonData.lessonItems.size)
        assertTrue(sink.sira.isEmpty())
        // NOT_FOUND: varsayılan liste + tüm item'lar buluta
        GlobalLessonData.applyLoadStatus(GlobalLessonData.FirestoreLoadStatus.NOT_FOUND, 1, false)
        assertEquals(listOf("toplu:1"), sink.sira)
        assertEquals(1, sink.toplular[0].partId)
        assertEquals(33, sink.toplular[0].itemsFields.size)
        assertEquals(GlobalLessonData.createLessonItems(1).map { it.stableId }, sink.toplular[0].stableIds)
        assertEquals(
            GlobalLessonData.createLessonItems(1).map { GlobalLessonData.progressFieldsOf(it) },
            sink.toplular[0].itemsFields,
        )
    }

    @Test
    fun saveToPreferences_aktifPartaYazar_saveRemoteFalseYazmaz() {
        yukle(3)
        GlobalLessonData.saveToPreferences(saveRemote = false)
        assertTrue(sink.toplular.isEmpty())
        GlobalLessonData.saveToPreferences()
        assertEquals(3, sink.toplular.single().partId)
        assertEquals(20, sink.toplular.single().itemsFields.size)
        // sink yoksa sessizce geçer
        GlobalLessonData.sink = null
        GlobalLessonData.saveToPreferences()
    }

    @Test
    fun buildDefaultLessonItemsForPart_tutorial1Duzeltmesi() {
        val sablon = GlobalLessonData.createLessonItems(1)
        // Bayrak yok → şablonun aynısı
        assertEquals(sablon, GlobalLessonData.buildDefaultLessonItemsForPart(1, tutorial1Pending = false))
        // Bayrak var + part 1 → yalnızca index 1 değişir
        val d = GlobalLessonData.buildDefaultLessonItemsForPart(1, tutorial1Pending = true)
        assertEquals(
            sablon[1].copy(
                tutorialIsFinish = true,
                currentStep = 2,
                startStepNumber = 2,
                // Android'deki değer aynen: 2 adımlı derste 3 elemanlı liste
                stepCompletionStatus = listOf(true, false, false),
            ),
            d[1],
        )
        assertEquals(2, d[1].stepCount)
        assertEquals(sablon[0], d[0])
        assertEquals(sablon.drop(2), d.drop(2))
        // Diğer part'larda bayrak etkisiz
        assertEquals(GlobalLessonData.createLessonItems(2), GlobalLessonData.buildDefaultLessonItemsForPart(2, true))
        // Bellek durumu değişmez
        assertTrue(GlobalLessonData.lessonItems.isEmpty())
        assertEquals(1, GlobalLessonData.globalPartId)
    }

    @Test
    fun applyDefaultLessonItems_ve_seed() {
        GlobalLessonData.initialize(1)
        GlobalLessonData.applyDefaultLessonItems(1, tutorial1Pending = true, saveRemote = false)
        assertEquals(2, GlobalLessonData.getLessonItem(1)?.currentStep)
        assertTrue(sink.sira.isEmpty())

        val seed = GlobalLessonData.seedItemsForPart(1, tutorial1Pending = true)
        assertNotNull(seed)
        assertEquals(33, seed.size)
        assertEquals(2, seed[1]["currentStep"])
        assertEquals(listOf(true, false, false), seed[1]["stepCompletionStatus"])
        assertEquals(15, GlobalLessonData.seedItemsForPart(8, false)?.size)
        assertEquals(GlobalLessonData.createLessonItems(1).map { it.stableId }, GlobalLessonData.seedStableIdsForPart(1))
        assertEquals(15, GlobalLessonData.seedStableIdsForPart(8).size)
        // Şablonu olmayan part atlanır
        assertNull(GlobalLessonData.seedItemsForPart(10, false))
    }

    // ---- Canlı senkron ----

    @Test
    fun prepareRealtimeSync_defterTutma() {
        yukle(1)
        assertTrue(GlobalLessonData.prepareRealtimeSync(1, "u1"))
        // Aynı (uid, part) → dinleyici zaten var
        assertFalse(GlobalLessonData.prepareRealtimeSync(1, "u1"))
        assertEquals(33, GlobalLessonData.lessonItems.size)
        // Aynı part, başka kullanıcı → yeni dinleyici, liste temizlenmez
        assertTrue(GlobalLessonData.prepareRealtimeSync(1, "u2"))
        assertEquals(33, GlobalLessonData.lessonItems.size)
        // Part değişti → yeni dinleyici ve eski part listesi temizlenir
        assertTrue(GlobalLessonData.prepareRealtimeSync(2, "u2"))
        assertTrue(GlobalLessonData.lessonItems.isEmpty())
    }

    @Test
    fun onRealtimeSnapshot_dallar() {
        val adapter = KayitAdapter()
        LessonManager.setAdapter(adapter)
        GlobalLessonData.initialize(1)
        val docIlerlemis = mapOf<String, Map<String, Any?>?>(
            "1" to mapOf("currentStep" to 2L, "stepCompletionStatus" to listOf(true, false)),
        )
        // Başka part'ın snapshot'ı yok sayılır
        assertFalse(GlobalLessonData.onRealtimeSnapshot(2, docIlerlemis, 1))
        // Boş snapshot yok sayılır
        assertFalse(GlobalLessonData.onRealtimeSnapshot(1, emptyMap(), 1))
        assertTrue(GlobalLessonData.lessonItems.isEmpty())
        assertEquals(0, adapter.tumListeSayisi)

        // Bellek boş → doğrudan Firestore verisi alınır ve liste yenilenir
        assertTrue(GlobalLessonData.onRealtimeSnapshot(1, docIlerlemis, 1))
        assertEquals(33, GlobalLessonData.lessonItems.size)
        assertEquals(2, GlobalLessonData.getLessonItem(1)?.currentStep)
        assertEquals(1, adapter.tumListeSayisi)

        // Aynı snapshot ikinci kez gelince birleştirme sonucu değişmez → yenileme yok
        // (eskiden şablondaki test bitişli sandıklar yüzünden bir kez daha "değişti" sayılıyordu; o bitişler silindi).
        assertFalse(GlobalLessonData.onRealtimeSnapshot(1, docIlerlemis, 1))
        assertEquals(1, adapter.tumListeSayisi)

        // Belge kimliği stableId olan (yeni biçim) snapshot da aynı şekilde işlenir
        val docYeni = mapOf<String, Map<String, Any?>?>(kimlik(1, 3) to mapOf("currentStep" to 2L))
        assertTrue(GlobalLessonData.onRealtimeSnapshot(1, docYeni, 1))
        assertEquals(2, GlobalLessonData.getLessonItem(3)?.currentStep)
        assertEquals(2, adapter.tumListeSayisi)

        // Eski (geride kalmış) snapshot yerel ilerlemeyi geri almaz
        val docEski = mapOf<String, Map<String, Any?>?>("1" to mapOf("currentStep" to 1L))
        assertFalse(GlobalLessonData.onRealtimeSnapshot(1, docEski, 1))
        assertEquals(2, GlobalLessonData.getLessonItem(1)?.currentStep)

        // Daha ileri snapshot birleştirilir
        val docBitmis = mapOf<String, Map<String, Any?>?>(
            "1" to mapOf("currentStep" to 2L, "stepCompletionStatus" to listOf(true, true), "stepIsFinish" to true),
        )
        assertEquals(false, GlobalLessonData.getLessonItem(2)?.isCompleted)
        assertTrue(GlobalLessonData.onRealtimeSnapshot(1, docBitmis, 1))
        assertEquals(true, GlobalLessonData.getLessonItem(1)?.stepIsFinish)
        // Bitirilen dersin ardından gelen ders türetilmiş kilitle açılır (olay kaçmış olsa bile)
        assertEquals(true, GlobalLessonData.getLessonItem(2)?.isCompleted)
        assertEquals(3, adapter.tumListeSayisi)
    }

    @Test
    fun onRealtimeSnapshot_yarisPaneliAciksaListeYenilenmezAmaBellekGuncellenir() {
        val adapter = KayitAdapter(racePanelAcik = true)
        LessonManager.setAdapter(adapter)
        GlobalLessonData.initialize(1)
        assertTrue(GlobalLessonData.onRealtimeSnapshot(1, mapOf("1" to mapOf("currentStep" to 2L)), 1))
        assertEquals(0, adapter.tumListeSayisi)
        assertEquals(2, GlobalLessonData.getLessonItem(1)?.currentStep)
    }

    // ---- updateLessonItem / liderlik ----

    @Test
    fun updateLessonItem_korumalar() {
        yukle(1)
        val item = GlobalLessonData.getLessonItem(1)!!
        // Başka part'a ait item → reddedilir
        assertFalse(GlobalLessonData.updateLessonItem(1, item.copy(partId = 2, currentStep = 2), 1))
        // Liste dışı pozisyon → reddedilir
        assertFalse(GlobalLessonData.updateLessonItem(33, item.copy(currentStep = 2), 1))
        assertFalse(GlobalLessonData.updateLessonItem(-1, item.copy(currentStep = 2), 1))
        assertTrue(sink.sira.isEmpty())
        assertEquals(1, GlobalLessonData.getLessonItem(1)?.currentStep)
        // partId null olan item kabul edilir
        assertTrue(GlobalLessonData.updateLessonItem(1, item.copy(partId = null, currentStep = 2), 1))
        assertEquals(2, GlobalLessonData.getLessonItem(1)?.currentStep)
        assertEquals(listOf("tek:${kimlik(1, 1)}"), sink.sira)
        assertEquals(1, sink.tekliler[0].partId)
        assertEquals(kimlik(1, 1), sink.tekliler[0].stableId)
        assertEquals(2, sink.tekliler[0].fields["currentStep"])
    }

    @Test
    fun getLessonItem_sinirlar() {
        assertNull(GlobalLessonData.getLessonItem(0))
        yukle(1)
        assertNull(GlobalLessonData.getLessonItem(-1))
        assertNull(GlobalLessonData.getLessonItem(33))
        assertEquals("Ünite Maratonu", GlobalLessonData.getLessonItem(32)?.title)
    }

    @Test
    fun updateLessonItem_sandikBitinceLiderlikGonderimi() {
        yukle(1)
        val s = GlobalLessonData.getLessonItem(4)!!.copy(
            stepIsFinish = true, record = 1700, leaderboardSeasonId = 7, leaderboardSeasonBest = 1650,
        )
        assertTrue(GlobalLessonData.updateLessonItem(4, s, season = 7))
        assertEquals(listOf("tek:${kimlik(1, 4)}", "lb:${kimlik(1, 4)}"), sink.sira)
        // Tahta kimliği artık konum değil stableId (part_1_lesson_p1_i04_unite_maratonu_season_7)
        assertEquals(
            GlobalLessonData.LeaderboardSubmit(1, "p1_i04_unite_maratonu", 1650, 7, "Sayıları Abaküste Tanıma"),
            sink.gonderimler.single(),
        )
    }

    @Test
    fun leaderboardSubmitFor_dallar() {
        val s = sandik().copy(stepIsFinish = true, leaderboardSeasonId = 7, leaderboardSeasonBest = 900, titleUnit = "  Ünite  ")
        // Geçerli: titleUnit kırpılır
        assertEquals(
            GlobalLessonData.LeaderboardSubmit(2, "test_sandik", 900, 7, "Ünite"),
            GlobalLessonData.leaderboardSubmitFor(2, 9, s, 7),
        )
        // Desteklenmeyen part'lar (4, 5, 9)
        assertNull(GlobalLessonData.leaderboardSubmitFor(4, 9, s, 7))
        assertNull(GlobalLessonData.leaderboardSubmitFor(5, 9, s, 7))
        assertNull(GlobalLessonData.leaderboardSubmitFor(9, 9, s, 7))
        // Desteklenenler
        for (p in listOf(1, 2, 3, 6, 7, 8)) assertNotNull(GlobalLessonData.leaderboardSubmitFor(p, 0, s, 7))
        // Bitmemiş sandık
        assertNull(GlobalLessonData.leaderboardSubmitFor(1, 9, s.copy(stepIsFinish = false), 7))
        // Sandık değil
        assertNull(
            GlobalLessonData.leaderboardSubmitFor(
                1, 9, ders().copy(stepIsFinish = true, leaderboardSeasonId = 7, leaderboardSeasonBest = 900), 7,
            ),
        )
        // Sezon uyuşmuyor / sezon skoru yok
        assertNull(GlobalLessonData.leaderboardSubmitFor(1, 9, s, 8))
        assertNull(GlobalLessonData.leaderboardSubmitFor(1, 9, s.copy(leaderboardSeasonBest = 0), 7))
        // titleUnit null kalır; 127 karakterden uzunsa kesilir
        assertNull(GlobalLessonData.leaderboardSubmitFor(1, 9, s.copy(titleUnit = null), 7)?.titleUnit)
        val uzun = "a".repeat(200)
        assertEquals(127, GlobalLessonData.leaderboardSubmitFor(1, 9, s.copy(titleUnit = uzun), 7)?.titleUnit?.length)
    }

    // ---- İlk sandık indeksi / backfill ----

    @Test
    fun templateFirstChestLessonIndexOrNull() {
        assertEquals(4, GlobalLessonData.templateFirstChestLessonIndexOrNull(1))
        assertEquals(3, GlobalLessonData.templateFirstChestLessonIndexOrNull(2))
        assertEquals(4, GlobalLessonData.templateFirstChestLessonIndexOrNull(3))
        assertEquals(4, GlobalLessonData.templateFirstChestLessonIndexOrNull(6))
        assertNull(GlobalLessonData.templateFirstChestLessonIndexOrNull(7))
        assertNull(GlobalLessonData.templateFirstChestLessonIndexOrNull(8))
        assertNull(GlobalLessonData.templateFirstChestLessonIndexOrNull(42))
    }

    @Test
    fun resolveFirstChestLessonIndex_dallar() {
        // Hata → şablon indeksi
        assertEquals(4, GlobalLessonData.resolveFirstChestLessonIndex(1, null, error = true))
        // Boş bulut → şablon listesi
        assertEquals(3, GlobalLessonData.resolveFirstChestLessonIndex(2, null, error = false))
        // Bulut listesindeki ilk sandık
        val bulut = listOf(baslik(), sandik(), ders())
        assertEquals(1, GlobalLessonData.resolveFirstChestLessonIndex(1, bulut, error = false))
        // Bulut listesinde sandık yok → şablona düşer
        assertEquals(4, GlobalLessonData.resolveFirstChestLessonIndex(1, listOf(baslik(), ders()), error = false))
        // Hiçbirinde yok → null
        assertNull(GlobalLessonData.resolveFirstChestLessonIndex(7, null, error = false))
        assertNull(GlobalLessonData.resolveFirstChestLessonIndex(7, listOf(ders()), error = false))
        assertNull(GlobalLessonData.resolveFirstChestLessonIndex(7, null, error = true))
    }

    @Test
    fun backfillLeaderboardSubmit_dallar() {
        val tamam = sandik().copy(
            stepIsFinish = true, leaderboardSeasonId = 7, leaderboardSeasonBest = 800, titleUnit = " T ",
        )
        val liste = listOf(baslik(), tamam, ders())
        assertEquals(
            GlobalLessonData.LeaderboardSubmit(3, "test_sandik", 800, 7, "T"),
            GlobalLessonData.backfillLeaderboardSubmit(3, 1, liste, 7),
        )
        // Liste yok
        assertNull(GlobalLessonData.backfillLeaderboardSubmit(3, 1, null, 7))
        // İndeks dışı / sandık değil
        assertNull(GlobalLessonData.backfillLeaderboardSubmit(3, 5, liste, 7))
        assertNull(GlobalLessonData.backfillLeaderboardSubmit(3, 2, liste, 7))
        // Bitmemiş
        assertNull(GlobalLessonData.backfillLeaderboardSubmit(3, 1, listOf(baslik(), tamam.copy(stepIsFinish = false)), 7))
        // Sezon skoru yok / başka sezon
        assertNull(GlobalLessonData.backfillLeaderboardSubmit(3, 1, liste, 8))
        assertNull(
            GlobalLessonData.backfillLeaderboardSubmit(3, 1, listOf(baslik(), tamam.copy(leaderboardSeasonBest = null)), 7),
        )
        // Not: updateLessonItem'dan farklı olarak burada part desteği kontrol edilmez (Android ile aynı)
        assertNotNull(GlobalLessonData.backfillLeaderboardSubmit(4, 1, liste, 7))
    }

    // ---- Çapraz part okumalar ----

    @Test
    fun getLessonItemForPart_ve_loadLessonItemsForPart() {
        assertEquals("Ünite Maratonu", GlobalLessonData.getLessonItemForPart(1, 4, null)?.title)
        assertNull(GlobalLessonData.getLessonItemForPart(1, 99, null))
        val bulut = listOf(ders(title = "bulut"))
        assertEquals("bulut", GlobalLessonData.getLessonItemForPart(1, 0, bulut)?.title)
        assertNull(GlobalLessonData.getLessonItemForPart(1, 4, bulut))
        assertSame(bulut, GlobalLessonData.loadLessonItemsForPart(1, bulut))
        assertEquals(20, GlobalLessonData.loadLessonItemsForPart(5, null).size)
        // Global durum değişmez
        assertTrue(GlobalLessonData.lessonItems.isEmpty())
    }

    @Test
    fun getFinishedChestItemsAcrossParts() {
        // Bulut yok → güncel Android şablonlarında hiçbir sandık bitmiş gelmez (test bitişleri silindi)
        val r = GlobalLessonData.getFinishedChestItemsAcrossParts(emptyMap())
        assertTrue(r.isEmpty())
        // Bulut listesi şablonun yerine geçer; bitmiş ders sayılmaz, part 9 taranmaz
        val bulut = mapOf<Int, List<LessonItem>?>(
            7 to listOf(ders().copy(stepIsFinish = true), sandik().copy(stepIsFinish = true), sandik()),
            9 to listOf(sandik().copy(stepIsFinish = true)),
            1 to emptyList(),
        )
        val r2 = GlobalLessonData.getFinishedChestItemsAcrossParts(bulut)
        assertEquals(1, r2.size)
        assertEquals(7 to 1, r2.last().let { it.partId to it.index })
        assertTrue(r2.none { it.partId == 1 || it.partId == 9 })
    }

    @Test
    fun calculateGlobalCompletionPercentage() {
        // Part 1..6 şablonlarında ders + sandık: 27 + 16 + 16 + 20 + 16 + 16
        assertEquals(111, GlobalLessonData.totalValidItemsForCompletion())
        // Hiç veri yok → 0
        assertEquals(0, GlobalLessonData.calculateGlobalCompletionPercentage(emptyMap()))
        // Şablonların kendisi: güncel Android'de hiçbir item bitmiş gelmez (eskiden 14 bitmişti → %12) → 0
        val sablonlar = (1..6).associateWith<Int, List<LessonItem>?> { GlobalLessonData.createLessonItems(it) }
        assertEquals(0, GlobalLessonData.calculateGlobalCompletionPercentage(sablonlar))
        // Hepsi bitmiş → 100
        val hepsi = (1..6).associateWith<Int, List<LessonItem>?> { p ->
            GlobalLessonData.createLessonItems(p).map { it.copy(stepIsFinish = true) }
        }
        assertEquals(100, GlobalLessonData.calculateGlobalCompletionPercentage(hepsi))
        // Yalnızca part 1 tamamen bitmiş (27/111 = %24,3 → 24); part 7 sayılmaz; null part 0 sayılır
        val yalniz1 = mapOf<Int, List<LessonItem>?>(
            1 to GlobalLessonData.createLessonItems(1).map { it.copy(stepIsFinish = true) },
            2 to null,
            7 to GlobalLessonData.createLessonItems(7).map { it.copy(stepIsFinish = true) },
        )
        assertEquals(24, GlobalLessonData.calculateGlobalCompletionPercentage(yalniz1))
    }

    // ---- Kilit türetme / öğretmen açması / kimlik köprüleri ----

    @Test
    fun withDerivedUnlocks_kurallar() {
        val liste = listOf(
            baslik(),
            ders(isCompleted = false),                                  // ilk oynanabilir: her zaman açık
            ders(isCompleted = false),                                  // öncekinin bitmesine bağlı
            ders(isCompleted = false).copy(stepIsFinish = true),        // kilitli ama BİTMİŞ (eski veri): bitişi geri alınmaz
            baslik(),
            ders(isCompleted = false),                                  // başlık zincirde şeffaf: öncesi bitmiş → açılır
            ders(isCompleted = false),
        )
        val r = GlobalLessonData.withDerivedUnlocks(1, liste)
        assertEquals(listOf(false, true, false, false, false, true, false), r.map { it.isCompleted }.let {
            // idx 3 bitmiş ama kendisi kilitliydi: önceki (idx 2) bitmediği için açılmaz
            it
        })
        // Bir önceki bitince sonraki açılır
        val r2 = GlobalLessonData.withDerivedUnlocks(
            2,
            listOf(baslik(), ders(isCompleted = true).copy(stepIsFinish = true), ders(isCompleted = false), ders(isCompleted = false)),
        )
        assertEquals(listOf(false, true, true, false), r2.map { it.isCompleted })
        // ASLA kilitlemez: açık ders açık kalır
        val acik = listOf(ders(isCompleted = true), ders(isCompleted = true))
        assertEquals(listOf(true, true), GlobalLessonData.withDerivedUnlocks(1, acik).map { it.isCompleted })
        // Yalnızca 1–6: yarış part'larında dokunulmaz
        val yaris = listOf(ders(isCompleted = false), ders(isCompleted = false))
        assertEquals(yaris, GlobalLessonData.withDerivedUnlocks(7, yaris))
        assertEquals(yaris, GlobalLessonData.withDerivedUnlocks(9, yaris))
        // Girdi değişmez (yeni liste döner)
        assertEquals(false, liste[1].isCompleted)
    }

    @Test
    fun withDerivedUnlocks_katalogSablonu() {
        // Part 2–6'nın şablonunda ilk ders kilitli gelir; yüklemede türetme ilkini açar
        for (part in 2..6) {
            val r = GlobalLessonData.withDerivedUnlocks(part, GlobalLessonData.createLessonItems(part))
            assertEquals(true, r[1].isCompleted, "part $part ilk ders")
            assertEquals(false, r[2].isCompleted, "part $part ikinci ders")
        }
    }

    @Test
    fun withTeacherUnlocks_kurallar() {
        val p1 = GlobalLessonData.createLessonItems(1)
        val a1 = GlobalLessonData.withTeacherUnlocks(1, p1)
        // Ders ve sandık hep açılır, başlığa dokunulmaz
        for (i in p1.indices) {
            val oynanabilir = p1[i].type == LessonItem.TYPE_LESSON || p1[i].type == LessonItem.TYPE_CHEST
            assertEquals(if (oynanabilir) true else p1[i].isCompleted, a1[i].isCompleted, "idx $i")
        }
        // Yarış part'ları: kilitli (2) → BAŞLAT (1); çözülmüş (0) ve 1 değişmez
        val yaris = listOf(ders().copy(raceBusyLevel = 2), ders().copy(raceBusyLevel = 0), ders().copy(raceBusyLevel = 1))
        assertEquals(listOf(1, 0, 1), GlobalLessonData.withTeacherUnlocks(7, yaris).map { it.raceBusyLevel })
        assertEquals(listOf(1, 0, 1), GlobalLessonData.withTeacherUnlocks(8, yaris).map { it.raceBusyLevel })
        // Diğer part'lar (9) dokunulmaz
        assertEquals(yaris, GlobalLessonData.withTeacherUnlocks(9, yaris))
    }

    @Test
    fun teacherUnlockPlan_bosBulutTumListeDoluBulutYalnizcaDegisenler() {
        // Hiç oynanmamış bölüm: bütün liste (açılmış hâli) yazılır
        val bos = GlobalLessonData.teacherUnlockPlan(3, null)
        assertEquals(20, bos.size)
        assertEquals(GlobalLessonData.createLessonItems(3).map { it.stableId }, bos.map { it.stableId })
        assertEquals(true, bos[1].fields["isCompleted"])
        assertEquals(false, bos[0].fields["isCompleted"]) // başlık
        // Dolu bulut: yalnızca değişen item'lar
        val bulut = GlobalLessonData.createLessonItems(3).map { if (it.type == LessonItem.TYPE_CHEST) it.copy(isCompleted = true) else it }
        val plan = GlobalLessonData.teacherUnlockPlan(3, bulut)
        // Sandıklar zaten açık → yazılmaz; geri kalan oynanabilir (ders) item'lar yazılır
        assertEquals(12, plan.size)
        assertTrue(plan.all { it.fields["isCompleted"] == true })
        // Her şey zaten açıksa plan boş
        val hepsiAcik = GlobalLessonData.withTeacherUnlocks(3, GlobalLessonData.createLessonItems(3))
        assertTrue(GlobalLessonData.teacherUnlockPlan(3, hepsiAcik).isEmpty())
        // Part 7: kilitli yarışlar 1'e çekilir
        val p7 = GlobalLessonData.teacherUnlockPlan(7, GlobalLessonData.createLessonItems(7))
        assertEquals(14, p7.size)
        assertTrue(p7.all { it.fields["raceBusyLevel"] == 1 })
    }

    @Test
    fun applyTeacherUnlocksToMemory() {
        assertFalse(GlobalLessonData.applyTeacherUnlocksToMemory()) // bellek boş
        val adapter = KayitAdapter()
        LessonManager.setAdapter(adapter)
        yukle(2)
        assertTrue(GlobalLessonData.applyTeacherUnlocksToMemory())
        assertTrue(GlobalLessonData.lessonItems.filter { it.type != LessonItem.TYPE_HEADER }.all { it.isCompleted })
        assertEquals(1, adapter.tumListeSayisi)
    }

    @Test
    fun turkishItem_stableIdAt_localize() {
        assertEquals("Ünite Maratonu", GlobalLessonData.turkishItem(1, "p1_i04_unite_maratonu")?.title)
        assertEquals("Sayıları Abaküste Tanıma", GlobalLessonData.turkishItem(1, "p1_i04_unite_maratonu")?.titleUnit)
        assertNull(GlobalLessonData.turkishItem(1, "yok"))
        assertEquals(GlobalLessonData.createLessonItems(2), GlobalLessonData.createLessonItemsInTurkish(2))
        assertEquals("Merhaba", GlobalLessonData.localizeLessonText("Merhaba"))
    }

    // ---- Yerel önbellek göçü ----

    @Test
    fun migrateRecordValue() {
        assertEquals(1500, GlobalLessonData.migrateRecordValue(1500))
        assertEquals(1500L, GlobalLessonData.migrateRecordValue(1500L))
        assertEquals(1500, GlobalLessonData.migrateRecordValue(" 1500 "))
        assertEquals(-3, GlobalLessonData.migrateRecordValue("-3"))
        // Eski "mm:ss" metni → null
        assertNull(GlobalLessonData.migrateRecordValue("01:23"))
        assertNull(GlobalLessonData.migrateRecordValue(""))
        assertNull(GlobalLessonData.migrateRecordValue("12.5"))
        assertNull(GlobalLessonData.migrateRecordValue(null))
        // Sayı/String dışı türlere dokunulmaz
        assertEquals(true, GlobalLessonData.migrateRecordValue(true))
        assertEquals(listOf(1), GlobalLessonData.migrateRecordValue(listOf(1)))
    }

    @Test
    fun migrateLegacyRecordField() {
        // Kök dizi değil → aynen
        val nesne = mapOf("record" to "01:23")
        assertSame(nesne, GlobalLessonData.migrateLegacyRecordField(nesne))
        assertNull(GlobalLessonData.migrateLegacyRecordField(null))
        val kok = listOf(
            mapOf("title" to "a", "record" to "900"),
            mapOf("title" to "b", "record" to "01:23"),
            mapOf("title" to "c", "record" to 70),
            mapOf("title" to "d"),
            "nesne-degil",
            null,
        )
        assertEquals(
            listOf(
                mapOf("title" to "a", "record" to 900),
                mapOf("title" to "b", "record" to null),
                mapOf("title" to "c", "record" to 70),
                mapOf("title" to "d"),
                "nesne-degil",
                null,
            ),
            GlobalLessonData.migrateLegacyRecordField(kok),
        )
    }

    @Test
    fun loadFromParsedCache_partIdAtanir() {
        GlobalLessonData.initialize(3)
        val onbellek = listOf(ders().copy(partId = 1), ders().copy(partId = null))
        GlobalLessonData.loadFromParsedCache(onbellek)
        assertEquals(listOf<Int?>(3, 3), GlobalLessonData.lessonItems.map { it.partId })
    }

    @Test
    fun clearCurrentUserLessonData() {
        yukle(1)
        assertEquals(listOf("LessonPrefs_guest", "LessonPrefs_u9"), GlobalLessonData.clearCurrentUserLessonData("u9"))
        assertTrue(GlobalLessonData.lessonItems.isEmpty())
        assertEquals(listOf("LessonPrefs_guest"), GlobalLessonData.clearCurrentUserLessonData(null))
        assertEquals(listOf("LessonPrefs_guest"), GlobalLessonData.clearCurrentUserLessonData("  "))
    }
}

class LessonManagerTest {

    private lateinit var sink: KayitSink

    @BeforeTest
    fun sifirla() {
        GlobalLessonData.resetForTest()
        LessonManager.resetForTest()
        sink = KayitSink()
        GlobalLessonData.sink = sink
        GlobalLessonData.initialize(1)
        GlobalLessonData.onLoadFromFirestore(1, GlobalLessonData.createLessonItems(1), false)
    }

    @Test
    fun ensureInitialized_ayniPartIcinBirKez() {
        val adapter = KayitAdapter()
        LessonManager.setAdapter(adapter)
        val cagrilar = mutableListOf<Int>()
        var hazir: (() -> Unit)? = null
        val baslat: (Int, () -> Unit) -> Unit = { p, onReady -> cagrilar.add(p); hazir = onReady }
        LessonManager.ensureInitialized(1, baslat)
        LessonManager.ensureInitialized(1, baslat)
        assertEquals(listOf(1), cagrilar)
        // onReady → liste yenilenir
        hazir!!.invoke()
        assertEquals(1, adapter.tumListeSayisi)
        assertEquals(33, adapter.sonListe?.size)
        LessonManager.ensureInitialized(2, baslat)
        assertEquals(listOf(1, 2), cagrilar)
    }

    @Test
    fun updateLessonItem_globalVeAdapter() {
        val adapter = KayitAdapter()
        LessonManager.setAdapter(adapter)
        val yeni = LessonManager.getLessonItem(1)!!.copy(currentStep = 2)
        LessonManager.updateLessonItem(1, yeni, 1)
        assertEquals(2, GlobalLessonData.getLessonItem(1)?.currentStep)
        assertEquals(listOf(1 to yeni), adapter.guncellenen)
        assertEquals(listOf("tek:${kimlik(1, 1)}"), sink.sira)
        // Başka part'ın item'ı: ne global ne adapter güncellenir
        LessonManager.updateLessonItem(2, yeni.copy(partId = 5), 1)
        assertEquals(1, adapter.guncellenen.size)
        assertEquals(1, sink.sira.size)
        // Liste dışı pozisyon: global yazmaz AMA adapter yine çağrılır (Android ile aynı)
        LessonManager.updateLessonItem(99, yeni, 1)
        assertEquals(1, sink.sira.size)
        assertEquals(99, adapter.guncellenen.last().first)
    }

    @Test
    fun updateRaceItem_yarisAdapteriniGunceller() {
        val adapter = KayitAdapter()
        LessonManager.setAdapter(adapter)
        LessonManager.setRaceAdapter(adapter)
        val yeni = LessonManager.getLessonItem(2)!!.copy(raceBusyLevel = 0)
        LessonManager.updateRaceItem(2, yeni, 1)
        assertEquals(0, GlobalLessonData.getLessonItem(2)?.raceBusyLevel)
        assertEquals(listOf(2 to yeni), adapter.yarisGuncellenen)
        // Harita adapter'ı updateRaceItem'da çağrılmaz
        assertTrue(adapter.guncellenen.isEmpty())
        LessonManager.updateRaceItem(2, yeni.copy(partId = 7), 1)
        assertEquals(1, adapter.yarisGuncellenen.size)
    }

    @Test
    fun refreshLessonsFromGlobalData_yarisPaneliAciksaAtlar() {
        val adapter = KayitAdapter(racePanelAcik = true)
        LessonManager.setAdapter(adapter)
        LessonManager.refreshLessonsFromGlobalData()
        assertEquals(0, adapter.tumListeSayisi)
        adapter.racePanelAcik = false
        LessonManager.refreshLessonsFromGlobalData()
        assertEquals(1, adapter.tumListeSayisi)
        // Adapter yokken çökmez
        LessonManager.setAdapter(null)
        LessonManager.refreshLessonsFromGlobalData()
    }
}

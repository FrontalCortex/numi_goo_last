package com.sorobit.shared.ders

import com.sorobit.shared.ders.ChestFragmentLogic.SuccessRateEvent
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChestFragmentLogicTest {

    private lateinit var sink: KayitSink
    private lateinit var adapter: KayitAdapter

    @BeforeTest
    fun sifirla() {
        GlobalLessonData.resetForTest()
        LessonManager.resetForTest()
        sink = KayitSink()
        adapter = KayitAdapter()
        GlobalLessonData.sink = sink
        LessonManager.setAdapter(adapter)
        LessonManager.setRaceAdapter(adapter)
    }

    private fun yukle(partId: Int, degistir: (MutableList<LessonItem>) -> Unit = {}) {
        GlobalLessonData.initialize(partId)
        val liste = GlobalLessonData.createLessonItems(partId).toMutableList()
        degistir(liste)
        GlobalLessonData.onLoadFromFirestore(partId, liste, error = false)
    }

    private fun item(i: Int) = GlobalLessonData.getLessonItem(i)!!

    // ---- updateMapProgress: ders ----

    @Test
    fun araAdim_currentStepArtar() {
        // Part 1 idx 1: 2 adımlı ders, currentStep 1, startStepNumber 1
        yukle(1)
        val once2 = item(2)
        val r = ChestFragmentLogic.updateMapProgress(1, recordScore = 450, lessonSuccessRate = 90f, icon = "", season = 3)

        val sonra = item(1)
        assertEquals(2, sonra.currentStep)
        assertEquals(2, sonra.startStepNumber)
        assertEquals(listOf(true, false), sonra.stepCompletionStatus)
        assertFalse(sonra.stepIsFinish)
        // Sıradaki ders bu dalda değişmez
        assertEquals(once2, item(2))

        assertFalse(r.shouldIncrementStepFinishMission)
        assertTrue(r.shouldIncrementStepCountMission)
        assertTrue(r.incrementRocketDailyLessons)
        assertFalse(r.shouldIncrementPerfectStepCountMission)
        assertEquals(listOf<SuccessRateEvent>(SuccessRateEvent.Pass(1, 1, 1)), r.successRateEvents)
        assertEquals(
            ChestFragmentLogic.PendingLessonProgressAnimation("0_-1_1_Sayıları Abaküste Tanıma_1", 0, 1),
            r.pendingAnimation,
        )
        assertEquals(listOf("tek:1"), sink.sira)
        assertEquals(2, sink.tekliler[0].fields["currentStep"])
        assertEquals(listOf(1), adapter.guncellenen.map { it.first })
    }

    @Test
    fun araAdim_yuzde100Basari_perfectGorevi() {
        yukle(1)
        val r = ChestFragmentLogic.updateMapProgress(1, 500, 100f, "", 3)
        assertTrue(r.shouldIncrementPerfectStepCountMission)
        // 99.99 yetmez
        yukle(1)
        assertFalse(ChestFragmentLogic.updateMapProgress(1, 499, 99.99f, "", 3).shouldIncrementPerfectStepCountMission)
    }

    @Test
    fun bitis_sonAdim_stepIsFinishVeSiradakiDersAcilir() {
        yukle(1) { it[2] = it[2].copy(isCompleted = false) }
        ChestFragmentLogic.updateMapProgress(1, 450, 90f, "", 3)
        sink.sira.clear()
        adapter.guncellenen.clear()

        val r = ChestFragmentLogic.updateMapProgress(1, 500, 100f, "", 3)
        val sonra = item(1)
        assertTrue(sonra.stepIsFinish)
        // Bitiş dalında currentStep ve startStepNumber DEĞİŞMEZ
        assertEquals(2, sonra.currentStep)
        assertEquals(2, sonra.startStepNumber)
        assertEquals(listOf(true, true), sonra.stepCompletionStatus)
        // Ders için stepCupIcon'a dokunulmaz, record yazılmaz
        assertEquals("chest_stars_tier0", sonra.stepCupIcon)
        assertNull(sonra.record)
        // Sıradaki ders (idx 2) açılır
        assertTrue(item(2).isCompleted)

        assertTrue(r.shouldIncrementStepFinishMission)
        assertTrue(r.shouldIncrementStepCountMission)
        assertTrue(r.shouldIncrementPerfectStepCountMission)
        assertEquals(
            listOf(SuccessRateEvent.Pass(1, 1, 2), SuccessRateEvent.ItemFirstFinish(1, 1, null)),
            r.successRateEvents,
        )
        assertEquals(
            ChestFragmentLogic.PendingLessonProgressAnimation("0_-1_1_Sayıları Abaküste Tanıma_1", 1, 2),
            r.pendingAnimation,
        )
        assertEquals(listOf("tek:1", "tek:2"), sink.sira)
        assertEquals(listOf(1, 2), adapter.guncellenen.map { it.first })
    }

    @Test
    fun bitmisDersTekrar_gorevVeOlayYok() {
        yukle(1) {
            it[1] = it[1].copy(stepIsFinish = true, currentStep = 2, stepCompletionStatus = listOf(true, true))
        }
        val r = ChestFragmentLogic.updateMapProgress(1, 500, 100f, "", 3)
        assertFalse(r.shouldIncrementStepFinishMission)
        assertFalse(r.shouldIncrementStepCountMission)
        assertFalse(r.shouldIncrementPerfectStepCountMission)
        assertTrue(r.successRateEvents.isEmpty())
        assertNull(r.pendingAnimation)
        // Yine de item ve sıradaki item yeniden yazılır (Android ile aynı)
        assertEquals(listOf("tek:1", "tek:2"), sink.sira)
        assertTrue(item(1).stepIsFinish)
    }

    @Test
    fun tekAdimliDers_ilkCalismadaBiter() {
        // Part 1 idx 7: "Kuralsız Toplama - 2 Basamaklı", stepCount 1, currentStep 1
        yukle(1)
        assertEquals(1, item(7).stepCount)
        val r = ChestFragmentLogic.updateMapProgress(7, 400, 80f, "", 3)
        assertTrue(item(7).stepIsFinish)
        assertEquals(listOf(true), item(7).stepCompletionStatus)
        assertTrue(r.shouldIncrementStepFinishMission)
        assertTrue(r.shouldIncrementStepCountMission)
        assertFalse(r.shouldIncrementPerfectStepCountMission)
        assertEquals(listOf(SuccessRateEvent.Pass(1, 7, 1), SuccessRateEvent.ItemFirstFinish(1, 7, null)), r.successRateEvents)
    }

    // ---- updateMapProgress: sandık ----

    @Test
    fun sandikBitisi_rekorGorselLiderlikVeBasligiAtlayarakKilitAcma() {
        // Part 1 idx 4: sandık (cupPoint1 1600, cupPoint2 1300); idx 5 başlık; idx 6 sıradaki ders
        yukle(1) { it[6] = it[6].copy(isCompleted = false) }
        val baslikOnce = item(5)
        // ChestResult.record(): rekor item üzerinde yerinde güncellenir
        assertFalse(ChestResultLogic.record(item(4), 1700, season = 7))
        val ikon = ChestFragmentLogic.changeCupIcon(item(4), 1)
        assertEquals("chest_stars_tier3", ikon.icon)
        assertEquals(3, ikon.pendingChestStarGainAmount)

        val r = ChestFragmentLogic.updateMapProgress(4, 1700, 100f, ikon.icon, season = 7)
        val s = item(4)
        assertTrue(s.stepIsFinish)
        assertEquals(listOf(true), s.stepCompletionStatus)
        assertEquals("chest_stars_tier3", s.stepCupIcon)
        assertEquals(1700, s.record)
        assertEquals(7, s.leaderboardSeasonId)
        assertEquals(1700, s.leaderboardSeasonBest)
        // cupPoint1 dolu olduğu için index + 2: başlık atlanır, idx 6 açılır
        assertEquals(baslikOnce, item(5))
        assertTrue(item(6).isCompleted)

        // Sandıkta ders görevleri artmaz
        assertFalse(r.shouldIncrementStepFinishMission)
        assertFalse(r.shouldIncrementStepCountMission)
        assertFalse(r.shouldIncrementPerfectStepCountMission)
        assertEquals(
            listOf(SuccessRateEvent.Pass(1, 4, 1), SuccessRateEvent.ItemFirstFinish(1, 4, 3)),
            r.successRateEvents,
        )
        assertEquals(
            ChestFragmentLogic.PendingLessonProgressAnimation("2_-1_1_Ünite Maratonu_4", 0, 1),
            r.pendingAnimation,
        )
        assertEquals(listOf("tek:4", "lb:4", "tek:6"), sink.sira)
        assertEquals(
            GlobalLessonData.LeaderboardSubmit(1, 4, 1700, 7, "Sayıları Abaküste Tanıma"),
            sink.gonderimler.single(),
        )
    }

    @Test
    fun sandikBitisi_dusukSkorYildizSayisiSifir() {
        yukle(1)
        ChestResultLogic.record(item(4), 520, season = 7)
        val ikon = ChestFragmentLogic.changeCupIcon(item(4), 1)
        assertEquals("chest_stars_tier1", ikon.icon)
        val r = ChestFragmentLogic.updateMapProgress(4, 520, 60f, ikon.icon, 7)
        assertEquals(SuccessRateEvent.ItemFirstFinish(1, 4, 1), r.successRateEvents[1])
        assertEquals(520, item(4).record)
    }

    @Test
    fun sonSandik_siradakiYok() {
        // Part 1 idx 32 son item (sandık): index + 2 = 34 liste dışı
        yukle(1)
        ChestFragmentLogic.updateMapProgress(32, 600, 70f, "chest_stars_tier1", 7)
        assertTrue(item(32).stepIsFinish)
        assertEquals(1, sink.tekliler.size)
        assertEquals(32, sink.tekliler[0].position)
    }

    @Test
    fun cupPointsizSandik_part4_basligiAtlamaz() {
        // Part 4 sandıklarında cupPoint1 yok → index + 1 (araya giren BAŞLIK'ın isCompleted'ı true olur; Android ile aynı)
        yukle(4)
        assertEquals(LessonItem.TYPE_CHEST, item(4).type)
        assertNull(item(4).cupPoint1)
        assertEquals(LessonItem.TYPE_HEADER, item(5).type)
        assertFalse(item(5).isCompleted)
        ChestFragmentLogic.updateMapProgress(4, 700, 100f, "star_on_ic", 7)
        assertTrue(item(4).stepIsFinish)
        assertEquals("star_on_ic", item(4).stepCupIcon)
        assertTrue(item(5).isCompleted)
        // Part 4 liderliği desteklemez → gönderim yok
        assertTrue(sink.gonderimler.isEmpty())
        assertEquals(listOf("tek:4", "tek:5"), sink.sira)
    }

    // ---- updateMapProgress: yarış ----

    @Test
    fun yaris_seviye1_tamamlanirVeSiradakiKilitAcilir() {
        yukle(7)
        val r = ChestFragmentLogic.updateMapProgress(0, 300, 100f, "", 7)
        assertEquals(0, item(0).raceBusyLevel)
        assertEquals(1, item(1).raceBusyLevel)
        assertEquals(2, item(2).raceBusyLevel)
        // Yarışta stepIsFinish / adım durumu ayarlanmaz, görev ve olay üretilmez
        assertFalse(item(0).stepIsFinish)
        assertEquals(listOf(false), item(0).stepCompletionStatus)
        assertFalse(r.shouldIncrementStepFinishMission)
        assertFalse(r.shouldIncrementStepCountMission)
        assertFalse(r.shouldIncrementPerfectStepCountMission)
        assertTrue(r.successRateEvents.isEmpty())
        assertNull(r.pendingAnimation)
        assertEquals(listOf("tek:0", "tek:1"), sink.sira)
        assertEquals(listOf(0, 1), adapter.yarisGuncellenen.map { it.first })
        assertTrue(adapter.guncellenen.isEmpty())
    }

    @Test
    fun yaris_seviye2_ileriSar_yalnizcaKendisi() {
        yukle(7)
        ChestFragmentLogic.updateMapProgress(3, 300, 100f, "", 7)
        assertEquals(0, item(3).raceBusyLevel)
        assertEquals(2, item(4).raceBusyLevel)
        assertEquals(1, item(0).raceBusyLevel)
        assertEquals(listOf("tek:3"), sink.sira)
    }

    @Test
    fun yaris_tamamlanmislarAtlanir_ilkKilitliAcilir() {
        yukle(7) {
            it[1] = it[1].copy(raceBusyLevel = 0)
            it[2] = it[2].copy(raceBusyLevel = 0)
        }
        ChestFragmentLogic.updateMapProgress(0, 300, 100f, "", 7)
        assertEquals(listOf<Int?>(0, 0, 0, 1, 2), (0..4).map { item(it).raceBusyLevel })
    }

    @Test
    fun yaris_zatenBaslaDurumundakiGorulurseDurur() {
        yukle(7) {
            it[1] = it[1].copy(raceBusyLevel = 1)
        }
        ChestFragmentLogic.updateMapProgress(0, 300, 100f, "", 7)
        assertEquals(listOf<Int?>(0, 1, 2, 2), (0..3).map { item(it).raceBusyLevel })
        assertEquals(listOf("tek:0"), sink.sira)
    }

    @Test
    fun unlockNextRace_nullSeviyelerAtlanirVeSonaKadarBulunamazsaNull() {
        yukle(7) {
            it[1] = it[1].copy(raceBusyLevel = null)
            it[2] = it[2].copy(raceBusyLevel = 5)
        }
        assertEquals(3, ChestFragmentLogic.unlockNextRaceItemSkippingCompleted(0, 7))
        assertEquals(1, item(3).raceBusyLevel)
        // Son item'dan sonra aranacak bir şey yok
        assertNull(ChestFragmentLogic.unlockNextRaceItemSkippingCompleted(14, 7))
        // Part 8: hepsi BAŞLA (1) → ilk görülen 1'de durur
        yukle(8)
        assertNull(ChestFragmentLogic.unlockNextRaceItemSkippingCompleted(0, 7))
    }

    @Test
    fun yaris_tamamlanmisTekrarOynanirsa_hicbirSeyDegismez() {
        yukle(7) { it[0] = it[0].copy(raceBusyLevel = 0) }
        val once = GlobalLessonData.lessonItems.toList()
        val r = ChestFragmentLogic.updateMapProgress(0, 300, 100f, "", 7)
        assertEquals(once, GlobalLessonData.lessonItems)
        assertTrue(sink.sira.isEmpty())
        assertFalse(r.shouldIncrementStepCountMission)
        assertTrue(r.successRateEvents.isEmpty())
    }

    // ---- updateMapProgress: korumalar ----

    @Test
    fun itemYoksa_hicbirSeyYapilmaz() {
        yukle(1)
        val r = ChestFragmentLogic.updateMapProgress(99, 500, 100f, "", 3)
        assertEquals(
            ChestFragmentLogic.MapProgressResult(false, false, false, emptyList(), null),
            r,
        )
        assertTrue(sink.sira.isEmpty())
    }

    @Test
    fun baskaPartinItemi_yazilmazAmaBayraklarHesaplanir() {
        // Bellekteki item'ın partId'si aktif part'tan farklıysa LessonManager güncellemeyi reddeder;
        // Android'de görev bayrakları ve başarı olayları yine de üretilir.
        yukle(1) { it[1] = it[1].copy(partId = 2) }
        val r = ChestFragmentLogic.updateMapProgress(1, 500, 100f, "", 3)
        assertEquals(1, item(1).currentStep)
        assertTrue(sink.sira.isEmpty())
        assertTrue(r.shouldIncrementStepCountMission)
        assertEquals(listOf<SuccessRateEvent>(SuccessRateEvent.Pass(1, 1, 1)), r.successRateEvents)
    }

    // ---- changeCupIcon ----

    @Test
    fun changeCupIcon_dallar() {
        // Sandık değil
        assertEquals(ChestFragmentLogic.CupIconChange("", 0), ChestFragmentLogic.changeCupIcon(ders(), 1))
        // Rekor yok → tier0, değişim yok
        assertEquals(ChestFragmentLogic.CupIconChange("chest_stars_tier0", 0), ChestFragmentLogic.changeCupIcon(sandik(), 1))
        // 500 → 1 yıldız
        assertEquals(
            ChestFragmentLogic.CupIconChange("chest_stars_tier1", 1),
            ChestFragmentLogic.changeCupIcon(sandik(record = 500), 1),
        )
        // cupPoint2 → 2 yıldız, cupPoint1 → 3 yıldız
        assertEquals(
            ChestFragmentLogic.CupIconChange("chest_stars_tier2", 2),
            ChestFragmentLogic.changeCupIcon(sandik(record = 1200), 1),
        )
        // 1 yıldızdan 3 yıldıza: kazanım 2
        assertEquals(
            ChestFragmentLogic.CupIconChange("chest_stars_tier3", 2),
            ChestFragmentLogic.changeCupIcon(sandik(record = 1400).copy(stepCupIcon = "chest_stars_tier1"), 1),
        )
        // Görsel aynı → kazanım 0
        assertEquals(
            ChestFragmentLogic.CupIconChange("chest_stars_tier3", 0),
            ChestFragmentLogic.changeCupIcon(sandik(record = 2000).copy(stepCupIcon = "chest_stars_tier3"), 1),
        )
        // Görsel düşerse (ör. eşik değişmiş): yeni görsel alınır, kazanım negatif olmaz
        assertEquals(
            ChestFragmentLogic.CupIconChange("chest_stars_tier1", 0),
            ChestFragmentLogic.changeCupIcon(sandik(record = 600).copy(stepCupIcon = "chest_stars_tier3"), 1),
        )
        // Kör ders: part 6 dışında star_on_ic (0 yıldız)
        val kor = sandik(record = 5000).copy(isBlinding = true)
        assertEquals(ChestFragmentLogic.CupIconChange("star_on_ic", 0), ChestFragmentLogic.changeCupIcon(kor, 4))
        assertEquals(ChestFragmentLogic.CupIconChange("chest_stars_tier3", 3), ChestFragmentLogic.changeCupIcon(kor, 6))
        // cupPoint yok → en çok 1 yıldız
        assertEquals(
            ChestFragmentLogic.CupIconChange("chest_stars_tier1", 1),
            ChestFragmentLogic.changeCupIcon(sandik(cupPoint1 = null, cupPoint2 = null, record = 9000), 1),
        )
    }

    @Test
    fun lessonProgressKey() {
        assertEquals("0_-1_-1_Ders_7", ChestFragmentLogic.lessonProgressKey(ders(), 7))
        val s = sandik().copy(id = 12, partId = 3)
        assertEquals("2_12_3_Ünite Maratonu_0", ChestFragmentLogic.lessonProgressKey(s, 0))
    }

    // ---- Claim kararları ----

    @Test
    fun claimKararlari() {
        assertTrue(ChestFragmentLogic.shouldRedirectToLoginForFirstTutorial(1, isLoggedIn = false))
        assertFalse(ChestFragmentLogic.shouldRedirectToLoginForFirstTutorial(1, isLoggedIn = true))
        assertFalse(ChestFragmentLogic.shouldRedirectToLoginForFirstTutorial(0, isLoggedIn = false))
        assertFalse(ChestFragmentLogic.shouldRedirectToLoginForFirstTutorial(2, isLoggedIn = false))

        assertTrue(ChestFragmentLogic.shouldMarkJustFinishedChestForRating(sandik()))
        assertFalse(ChestFragmentLogic.shouldMarkJustFinishedChestForRating(sandik().copy(stepIsFinish = true)))
        assertFalse(ChestFragmentLogic.shouldMarkJustFinishedChestForRating(ders()))
        assertFalse(ChestFragmentLogic.shouldMarkJustFinishedChestForRating(null))

        assertTrue(ChestFragmentLogic.shouldIncrementDartProgress(ders(), 100f))
        assertFalse(ChestFragmentLogic.shouldIncrementDartProgress(ders(), 99.9f))
        assertFalse(ChestFragmentLogic.shouldIncrementDartProgress(ders().copy(stepIsFinish = true), 100f))
        assertFalse(ChestFragmentLogic.shouldIncrementDartProgress(sandik(), 100f))
        assertFalse(ChestFragmentLogic.shouldIncrementDartProgress(null, 100f))

        assertTrue(ChestFragmentLogic.shouldIncrementTornado(7))
        assertFalse(ChestFragmentLogic.shouldIncrementTornado(4))
        assertTrue(ChestFragmentLogic.shouldIncrementVolcano(8))
        assertFalse(ChestFragmentLogic.shouldIncrementVolcano(7))
    }

    @Test
    fun isLastChestOfCupPathPart() {
        yukle(1)
        val son = item(32).copy(stepIsFinish = true)
        assertTrue(ChestFragmentLogic.isLastChestOfCupPathPart(son))
        // Bitmemiş son sandık
        assertFalse(ChestFragmentLogic.isLastChestOfCupPathPart(item(32)))
        // Son olmayan sandık
        assertFalse(ChestFragmentLogic.isLastChestOfCupPathPart(item(4).copy(stepIsFinish = true)))
        // Ders / null
        assertFalse(ChestFragmentLogic.isLastChestOfCupPathPart(item(31).copy(stepIsFinish = true)))
        assertFalse(ChestFragmentLogic.isLastChestOfCupPathPart(null))
        // Part 1..6 dışında asla
        assertFalse(ChestFragmentLogic.isLastChestOfCupPathPart(son, globalPartId = 7))
        assertFalse(ChestFragmentLogic.isLastChestOfCupPathPart(son, globalPartId = 0))
        assertTrue(ChestFragmentLogic.isLastChestOfCupPathPart(son, globalPartId = 6))
        // mapFragmentIndex null ise false
        assertFalse(
            ChestFragmentLogic.isLastChestOfCupPathPart(
                son.copy(mapFragmentIndex = null),
                1,
                listOf(son.copy(mapFragmentIndex = null)),
            ),
        )
        // Listede sandık yoksa false
        assertFalse(ChestFragmentLogic.isLastChestOfCupPathPart(son, 1, listOf(ders())))
    }
}

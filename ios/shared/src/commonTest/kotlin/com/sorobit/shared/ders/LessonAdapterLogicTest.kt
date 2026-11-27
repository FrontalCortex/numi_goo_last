package com.sorobit.shared.ders

import com.sorobit.shared.ders.LessonAdapterLogic.ChestStarSlot
import com.sorobit.shared.ders.LessonAdapterLogic.ProgressAnimationAction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue


class LessonAdapterLogicTest {



    @BeforeTest
    fun sifirla() {
        GlobalLessonData.resetForTest()
        LessonManager.resetForTest()
    }

    // ---- Ders alt paneli ----

    @Test
    fun altPanel_kilitliDers_anahtarlaAcilabilir() {
        // Güncel Android: 1–6. bölümlerdeki kilitli ders/sandık 1 anahtarla açılabilir (düğme açık mavi, tıklanabilir)
        val s = LessonAdapterLogic.lessonBottomSheetState(ders(isCompleted = false).copy(tutorialNumber = 3, tutorialIsFinish = true), 1)
        assertEquals("Ders", s.title)
        assertEquals("Önceki dersleri tamamla ya da 1 anahtarla bu dersi hemen aç.", s.description)
        assertEquals("1 anahtarla kilidi aç", s.actionText)
        assertTrue(s.actionEnabled)
        assertTrue(s.keyUnlockable)
        assertFalse(s.locked)
        assertEquals(LessonAdapterLogic.PanelState.LOCKED, s.panelState)
        assertFalse(s.againTutorialVisible)
        assertNull(s.recordText)
        assertFalse(s.recordClickable)
        assertTrue(s.recordLayoutGone)
        assertFalse(s.fireAnimVisible)
        assertFalse(s.stepRowVisible)
        assertEquals(1, LessonAdapterLogic.LESSON_UNLOCK_KEY_COST)
        // Sandık da açılabilir
        assertTrue(LessonAdapterLogic.lessonBottomSheetState(sandik().copy(isCompleted = false), 2).keyUnlockable)
    }

    @Test
    fun altPanel_kilitliDers_anahtarlaAcilamayan() {
        // Yarış/kupa bölümlerinde (7, 8, 9) ve başlık/yarış türünde anahtar yolu yok
        for (part in listOf(7, 8, 9)) {
            val s = LessonAdapterLogic.lessonBottomSheetState(ders(isCompleted = false), part)
            assertEquals("Kilidi açmak için önceki dersleri tamamla.", s.description, "part $part")
            assertEquals("Kilitli", s.actionText)
            assertFalse(s.actionEnabled)
            assertTrue(s.locked)
            assertFalse(s.keyUnlockable)
        }
        val yaris = ders(isCompleted = false).copy(type = LessonItem.TYPE_RACE)
        assertFalse(LessonAdapterLogic.isKeyUnlockable(yaris, 1))
        assertFalse(LessonAdapterLogic.isKeyUnlockable(baslik(), 1))
        // Açık ders anahtarla "açılabilir" sayılmaz
        assertFalse(LessonAdapterLogic.isKeyUnlockable(ders(), 1))
        // Anahtarla açılan item yalnızca isCompleted = true olur
        val acilmis = LessonAdapterLogic.unlockedWithKey(ders(isCompleted = false, currentStep = 1))
        assertTrue(acilmis.isCompleted)
        assertEquals(1, acilmis.currentStep)
    }

    @Test
    fun altPanel_acikDers_baslaVeDevamEt() {
        // Hiç adım bitmemiş → "Başla"
        val s0 = LessonAdapterLogic.lessonBottomSheetState(ders(stepCount = 3, currentStep = 1), 1)
        assertEquals("Başla", s0.actionText)
        assertEquals("", s0.description)
        assertTrue(s0.actionEnabled)
        assertFalse(s0.locked)
        assertEquals(LessonAdapterLogic.PanelState.OPEN, s0.panelState)
        assertEquals("DERS", s0.kindText)
        assertEquals("profile_book_ic3", s0.iconName)
        assertTrue(s0.stepRowVisible)
        assertEquals(0, s0.doneSteps)
        assertEquals(3, s0.stepCount)
        // Bir adım bitmiş → "Devam et", "1/3"
        val s1 = LessonAdapterLogic.lessonBottomSheetState(
            ders(stepCount = 3, currentStep = 2).copy(stepCompletionStatus = listOf(true, false, false)), 1,
        )
        assertEquals("Devam et", s1.actionText)
        assertEquals(1, s1.doneSteps)
        // tutorialNumber 0 → "Eğitimi tekrarla" görünmez
        assertFalse(s1.againTutorialVisible)
        assertNull(s1.recordText)
        assertTrue(s1.recordLayoutGone)
    }

    @Test
    fun altPanel_egitimiTekrarlaKosulu() {
        fun gorunur(tn: Int, bitti: Boolean) =
            LessonAdapterLogic.lessonBottomSheetState(ders().copy(tutorialNumber = tn, tutorialIsFinish = bitti), 1).againTutorialVisible
        assertTrue(gorunur(5, true))
        assertFalse(gorunur(5, false))
        assertFalse(gorunur(0, true))
    }

    @Test
    fun altPanel_bitmisDers() {
        val s = LessonAdapterLogic.lessonBottomSheetState(ders().copy(stepIsFinish = true), 1)
        assertEquals("", s.description)
        assertEquals("Gözden geçir", s.actionText)
        assertTrue(s.actionEnabled)
        assertEquals(LessonAdapterLogic.PanelState.DONE, s.panelState)
        // Bitmiş derste adım çubuğu tam dolu
        assertEquals(3, s.doneSteps)
        assertTrue(s.stepRowVisible)
        assertNull(s.recordText)
        assertFalse(s.recordClickable)
        assertFalse(s.fireAnimVisible)
    }

    @Test
    fun altPanel_bitmisSandik() {
        val s = LessonAdapterLogic.lessonBottomSheetState(sandik(record = 1450).copy(stepIsFinish = true), 1)
        assertEquals("", s.description)
        assertEquals("Tekrar dene", s.actionText)
        assertTrue(s.actionEnabled)
        assertEquals("Rekor: 1450", s.recordText)
        // Yeni panelde ateş animasyonu yok
        assertFalse(s.fireAnimVisible)
        assertTrue(s.recordClickable)
        assertFalse(s.recordLayoutGone)
        assertEquals("ÜNİTE MARATONU", s.kindText)
        assertEquals("podium_ic2", s.iconName)
        assertFalse(s.stepRowVisible)
        // Rekor null ise metin "Rekor: —" olur ve satır tıklanamaz
        val s2 = LessonAdapterLogic.lessonBottomSheetState(sandik(record = null).copy(stepIsFinish = true), 1)
        assertEquals("Rekor: —", s2.recordText)
        assertFalse(s2.recordClickable)
        // Kürsü ikonu yalnızca 1, 2, 3, 6. bölümlerde; diğerlerinde kitap
        assertEquals("profile_book_ic3", LessonAdapterLogic.lessonBottomSheetState(sandik(), 4).iconName)
        assertEquals("podium_ic2", LessonAdapterLogic.lessonBottomSheetState(sandik(), 6).iconName)
        assertEquals("profile_book_ic3", LessonAdapterLogic.lessonBottomSheetState(ders(), 1).iconName)
    }

    @Test
    fun altPanel_part4ve5RekorSatiriGizli() {
        for (part in listOf(4, 5)) {
            val s = LessonAdapterLogic.lessonBottomSheetState(sandik(record = 900).copy(stepIsFinish = true), part)
            assertTrue(s.recordLayoutGone, "part $part")
            assertNull(s.recordText)
            assertFalse(s.fireAnimVisible)
            assertFalse(s.recordClickable)
            assertEquals("Tekrar dene", s.actionText)
            // Sandık olmayan / bitmemiş item'da da satır gizlenir
            assertTrue(LessonAdapterLogic.lessonBottomSheetState(ders(), part).recordLayoutGone)
            assertTrue(LessonAdapterLogic.lessonBottomSheetState(ders(isCompleted = false), part).recordLayoutGone)
        }
        // Diğer bölümlerde satır yalnızca bitmiş sandıkta görünür
        assertFalse(LessonAdapterLogic.lessonBottomSheetState(sandik(record = 900).copy(stepIsFinish = true), 6).recordLayoutGone)
        assertTrue(LessonAdapterLogic.lessonBottomSheetState(ders(), 6).recordLayoutGone)
        assertTrue(LessonAdapterLogic.lessonBottomSheetState(sandik(), 6).recordLayoutGone)
    }

    @Test
    fun altPanel_varsayilanPartGlobalden() {
        val bitmisSandik = sandik(record = 900).copy(stepIsFinish = true)
        GlobalLessonData.globalPartId = 5
        assertTrue(LessonAdapterLogic.lessonBottomSheetState(bitmisSandik).recordLayoutGone)
        GlobalLessonData.globalPartId = 1
        assertFalse(LessonAdapterLogic.lessonBottomSheetState(bitmisSandik).recordLayoutGone)
    }

    @Test
    fun actionButtonDecision() {
        val acik = ders()
        val kilitli = ders(isCompleted = false)
        // Anahtarla açılamayan kilitli (part 7): panel yalnızca daraltılır
        assertEquals(LessonAdapterLogic.ActionButtonDecision.LOCKED_COLLAPSE, LessonAdapterLogic.actionButtonDecision(kilitli, true, true, 7))
        // 1–6. bölümlerde kilitli ders anahtarla açılır
        assertEquals(LessonAdapterLogic.ActionButtonDecision.UNLOCK_WITH_KEY, LessonAdapterLogic.actionButtonDecision(kilitli, true, true, 1))
        assertEquals(LessonAdapterLogic.ActionButtonDecision.UNLOCK_WITH_KEY, LessonAdapterLogic.actionButtonDecision(kilitli, false, false, 6))
        assertEquals(LessonAdapterLogic.ActionButtonDecision.START_WITHOUT_ENERGY, LessonAdapterLogic.actionButtonDecision(acik, true, false))
        assertEquals(LessonAdapterLogic.ActionButtonDecision.NOT_ENOUGH_ENERGY, LessonAdapterLogic.actionButtonDecision(acik, false, false))
        assertEquals(LessonAdapterLogic.ActionButtonDecision.USE_ENERGY_AND_START, LessonAdapterLogic.actionButtonDecision(acik, false, true))
        assertEquals(1, LessonAdapterLogic.LESSON_ENERGY_COST)
    }

    @Test
    fun continueWithLessonTarget() {
        assertEquals(LessonAdapterLogic.LessonStartTarget.ABACUS, LessonAdapterLogic.continueWithLessonTarget(ders().copy(tutorialIsFinish = true)))
        assertEquals(LessonAdapterLogic.LessonStartTarget.TUTORIAL, LessonAdapterLogic.continueWithLessonTarget(ders().copy(tutorialIsFinish = false)))
        assertEquals(
            LessonAdapterLogic.LessonStartTarget.BLINDING_LESSON,
            LessonAdapterLogic.continueWithLessonTarget(ders().copy(isBlinding = true, tutorialIsFinish = true)),
        )
        assertEquals(
            LessonAdapterLogic.LessonStartTarget.TUTORIAL,
            LessonAdapterLogic.continueWithLessonTarget(ders().copy(isBlinding = true, tutorialIsFinish = false)),
        )
        // isBlinding false → abaküs
        assertEquals(
            LessonAdapterLogic.LessonStartTarget.ABACUS,
            LessonAdapterLogic.continueWithLessonTarget(ders().copy(isBlinding = false, tutorialIsFinish = true)),
        )
        // Katalog: part 1 ilk ders öğreticiyle başlar; part 4'ün ikinci dersi doğrudan kör ders
        assertEquals(LessonAdapterLogic.LessonStartTarget.TUTORIAL, LessonAdapterLogic.continueWithLessonTarget(GlobalLessonData.createLessonItems(1)[1]))
        assertEquals(LessonAdapterLogic.LessonStartTarget.BLINDING_LESSON, LessonAdapterLogic.continueWithLessonTarget(GlobalLessonData.createLessonItems(4)[2]))
    }

    @Test
    fun lessonStartGlobals() {
        val item = GlobalLessonData.createLessonItems(1)[2]
        assertEquals(LessonAdapterLogic.LessonStartGlobals(2, 1000), LessonAdapterLogic.lessonStartGlobals(item))
        assertFailsWith<IllegalStateException> { LessonAdapterLogic.lessonStartGlobals(ders()) }
        assertFailsWith<IllegalStateException> { LessonAdapterLogic.lessonStartGlobals(ders().copy(mapFragmentIndex = 3)) }
        // Yarışta null alanlar mevcut değeri korur
        val mevcut = LessonAdapterLogic.LessonStartGlobals(9, 99)
        assertEquals(mevcut, LessonAdapterLogic.raceStartGlobals(ders(), mevcut))
        assertEquals(
            LessonAdapterLogic.LessonStartGlobals(0, 78),
            LessonAdapterLogic.raceStartGlobals(GlobalLessonData.createLessonItems(7)[0], mevcut),
        )
    }

    // ---- Yarış paneli ----

    @Test
    fun partIdAfterRacePanelClose() {
        val bayrak = ders().copy(backRaceId = 3)
        assertEquals(3, LessonAdapterLogic.partIdAfterRacePanelClose(bayrak, isFromPartSelection = false, currentGlobalPartId = 7))
        assertEquals(7, LessonAdapterLogic.partIdAfterRacePanelClose(bayrak, isFromPartSelection = true, currentGlobalPartId = 7))
        assertFailsWith<IllegalStateException> { LessonAdapterLogic.partIdAfterRacePanelClose(ders(), false, 7) }
        assertEquals(7, LessonAdapterLogic.partIdAfterRacePanelClose(ders(), true, 7))
        assertTrue(LessonAdapterLogic.raceFlagClickAllowed(ders(isCompleted = true)))
        assertFalse(LessonAdapterLogic.raceFlagClickAllowed(ders(isCompleted = false)))
    }

    @Test
    fun formatRaceTimePeriodDescription() {
        assertEquals("Sayı gösterilme periyodu 1 saniye", LessonAdapterLogic.formatRaceTimePeriodDescription(null))
        assertEquals("Sayı gösterilme periyodu 3 saniye", LessonAdapterLogic.formatRaceTimePeriodDescription(3000))
        assertEquals("Sayı gösterilme periyodu 2,5 saniye", LessonAdapterLogic.formatRaceTimePeriodDescription(2500))
        // Katalogdaki diğer değerler: 750 → 0.75 → "%.1f" (yarım yukarı) → 0,8
        assertEquals("Sayı gösterilme periyodu 0,8 saniye", LessonAdapterLogic.formatRaceTimePeriodDescription(750))
        assertEquals("Sayı gösterilme periyodu 0,8 saniye", LessonAdapterLogic.formatRaceTimePeriodDescription(800))
        assertEquals("Sayı gösterilme periyodu 1 saniye", LessonAdapterLogic.formatRaceTimePeriodDescription(1000))
        assertEquals("Sayı gösterilme periyodu 4 saniye", LessonAdapterLogic.formatRaceTimePeriodDescription(4000))
        assertEquals("Sayı gösterilme periyodu 0 saniye", LessonAdapterLogic.formatRaceTimePeriodDescription(0))
        assertEquals("Sayı gösterilme periyodu 1,5 saniye", LessonAdapterLogic.formatRaceTimePeriodDescription(1500))
        assertEquals("Sayı gösterilme periyodu 0,1 saniye", LessonAdapterLogic.formatRaceTimePeriodDescription(100))
        assertEquals("Sayı gösterilme periyodu 12,3 saniye", LessonAdapterLogic.formatRaceTimePeriodDescription(12340))
        // Onda birin altı 0,0'a iner; 1 sn'ye yuvarlanan değer "1,0" yazılır (tam bölünmediği için ondalıklı)
        assertEquals("Sayı gösterilme periyodu 0,0 saniye", LessonAdapterLogic.formatRaceTimePeriodDescription(40))
        assertEquals("Sayı gösterilme periyodu 1,0 saniye", LessonAdapterLogic.formatRaceTimePeriodDescription(999))
    }

    @Test
    fun raceLessonBottomSheetState() {
        val p7 = GlobalLessonData.createLessonItems(7)
        // BAŞLA (1)
        val basla = LessonAdapterLogic.raceLessonBottomSheetState(p7[0])
        assertEquals("1 basamaklı 4 adet sayı", basla.title)
        assertEquals("Sayı gösterilme periyodu 3 saniye", basla.description)
        assertFalse(basla.isLockedRace)
        assertEquals("BAŞLAT", basla.actionText)
        assertTrue(basla.actionAllCaps)
        // Güncel Android: başlat düğmesinin simgesi kaldırıldı
        assertNull(basla.actionIcon)
        // KİLİTLİ (2): anahtar simgeli, metinsiz düğme
        val kilitli = LessonAdapterLogic.raceLessonBottomSheetState(p7[1])
        assertTrue(kilitli.isLockedRace)
        assertEquals("", kilitli.actionText)
        assertEquals("key", kilitli.actionIcon)
        assertEquals("Sayı gösterilme periyodu 2 saniye", kilitli.description)
        // TAMAMLANDI (0)
        val tamam = LessonAdapterLogic.raceLessonBottomSheetState(p7[0].copy(raceBusyLevel = 0))
        assertEquals("Tekrar dene", tamam.actionText)
        assertFalse(tamam.actionAllCaps)
        assertNull(tamam.actionIcon)
        // raceBusyLevel null → BAŞLAT; raceTitle yoksa title
        val bos = LessonAdapterLogic.raceLessonBottomSheetState(ders(title = "Yarış"))
        assertEquals("Yarış", bos.title)
        assertEquals("BAŞLAT", bos.actionText)
        assertEquals("Sayı gösterilme periyodu 1 saniye", bos.description)
    }

    @Test
    fun raceRowStatus() {
        assertEquals(LessonAdapterLogic.RaceRowStatus("TAMAMLANDI", false, true), LessonAdapterLogic.raceRowStatus(ders().copy(raceBusyLevel = 0)))
        assertEquals(LessonAdapterLogic.RaceRowStatus("KİLİTLİ", true, false), LessonAdapterLogic.raceRowStatus(ders().copy(raceBusyLevel = 2)))
        assertEquals(LessonAdapterLogic.RaceRowStatus("BAŞLA", false, true), LessonAdapterLogic.raceRowStatus(ders().copy(raceBusyLevel = 1)))
        assertEquals(LessonAdapterLogic.RaceRowStatus("BAŞLA", false, true), LessonAdapterLogic.raceRowStatus(ders()))
        assertEquals(LessonAdapterLogic.RaceRowStatus("BAŞLA", false, true), LessonAdapterLogic.raceRowStatus(ders().copy(raceBusyLevel = 9)))
    }

    @Test
    fun refreshedRacePanelItems() {
        // Bellekte part 7 yüklü; "Acemi Çırak" tamamlanmış
        val bellek = GlobalLessonData.createLessonItems(7).toMutableList()
        bellek[0] = bellek[0].copy(raceBusyLevel = 0)
        // Başlık eşleşmezse racePartId varsayılanı 7
        val r = LessonAdapterLogic.refreshedRacePanelItems("Bilinmeyen Başlık", bellek)
        assertEquals(15, r.size)
        assertEquals(0, r[0].raceBusyLevel)
        assertEquals(GlobalLessonData.createLessonItems(7).drop(1), r.drop(1))
        // Açık yarış item'ı racePartId taşıyorsa o part'ın şablonu kullanılır; eşleşme başlığa göredir
        val bayrak = ders(title = "Yarışlar").copy(racePartId = 8)
        val r8 = LessonAdapterLogic.refreshedRacePanelItems("Yarışlar", listOf(bayrak) + bellek)
        assertEquals(15, r8.size)
        // Part 8 şablonundaki "Acemi Çırak" yerine bellekteki AYNI BAŞLIKLI (part 7) item gelir
        assertEquals(7, r8[0].partId)
        assertEquals(0, r8[0].raceBusyLevel)
        // Bellek boşsa şablonun kendisi
        assertEquals(GlobalLessonData.createLessonItems(7), LessonAdapterLogic.refreshedRacePanelItems("x", emptyList()))
    }

    // ---- Kart görünümü ----

    @Test
    fun itemViewTypeVeRenkler() {
        val liste = listOf(baslik(), ders(), sandik())
        assertEquals(LessonItem.TYPE_HEADER, LessonAdapterLogic.itemViewType(liste, 0))
        assertEquals(LessonItem.TYPE_LESSON, LessonAdapterLogic.itemViewType(liste, 1))
        assertEquals(LessonItem.TYPE_CHEST, LessonAdapterLogic.itemViewType(liste, 2))
        assertEquals(LessonItem.TYPE_LESSON, LessonAdapterLogic.itemViewType(liste, 3))
        assertEquals(LessonItem.TYPE_LESSON, LessonAdapterLogic.itemViewType(liste, -1))
        assertEquals("lesson_center_blue", LessonAdapterLogic.lessonCardColor(ders()))
        assertEquals("lesson_locked", LessonAdapterLogic.lessonCardColor(ders(isCompleted = false)))
        assertEquals("lesson_completed", LessonAdapterLogic.raceCardColor(ders()))
        assertEquals("lesson_locked", LessonAdapterLogic.raceCardColor(ders(isCompleted = false)))
        assertEquals(16f, LessonAdapterLogic.SEGMENT_GAP_ANGLE)
    }

    @Test
    fun stepSegments() {
        assertEquals(LessonAdapterLogic.StepSegments(3, 0), LessonAdapterLogic.stepSegments(ders(stepCount = 3)))
        assertEquals(
            LessonAdapterLogic.StepSegments(3, 2),
            LessonAdapterLogic.stepSegments(ders(stepCount = 3).copy(stepCompletionStatus = listOf(true, true, false))),
        )
        // Bitmişse durum listesine bakılmaksızın tümü dolu
        assertEquals(LessonAdapterLogic.StepSegments(3, 3), LessonAdapterLogic.stepSegments(ders(stepCount = 3).copy(stepIsFinish = true)))
        // stepCount 0 → en az 1 dilim
        assertEquals(LessonAdapterLogic.StepSegments(1, 0), LessonAdapterLogic.stepSegments(ders(stepCount = 0)))
        // Durum listesi stepCount'tan uzunsa dolu sayısı kırpılır (tutorial1 düzeltmesindeki 3 elemanlı liste gibi)
        assertEquals(
            LessonAdapterLogic.StepSegments(2, 2),
            LessonAdapterLogic.stepSegments(ders(stepCount = 2).copy(stepCompletionStatus = listOf(true, true, true))),
        )
    }

    @Test
    fun lessonProgressKey_chestFragmentIleAyniBicim() {
        val item = GlobalLessonData.createLessonItems(1)[4]
        // Güncel Android: `{type}_{stableId}`; konum ve başlık anahtara girmez
        assertEquals("2_p1_i04_unite_maratonu", LessonAdapterLogic.lessonProgressKey(item, adapterPosition = 99))
        assertEquals(ChestFragmentLogic.lessonProgressKey(item, 4), LessonAdapterLogic.lessonProgressKey(item, 99))
        assertEquals("0_test_ders", LessonAdapterLogic.lessonProgressKey(ders(), 6))
    }

    @Test
    fun progressAnimationDecision_kaliciAltin() {
        val bitmis = ders(stepCount = 2).copy(stepIsFinish = true, finalGoldVisualUnlocked = true)
        val k = LessonAdapterLogic.progressAnimationDecision(bitmis, false, pendingFromFilled = 1, canConsumePending = true, lastSeenFilled = 0)
        assertEquals(ProgressAnimationAction.APPLY_PERSISTENT_GOLD, k.action)
        assertEquals(2, k.targetFilled)
        assertEquals(2, k.lastSeenFilled)
        assertFalse(k.removePending)
        assertFalse(k.persistFinalGold)
        // Animasyon bu oturumda oynamış ama item'a henüz yazılmamış → kalıcı yazma gerekir
        val k2 = LessonAdapterLogic.progressAnimationDecision(bitmis.copy(finalGoldVisualUnlocked = false), true, null, false, null)
        assertEquals(ProgressAnimationAction.APPLY_PERSISTENT_GOLD, k2.action)
        assertTrue(k2.persistFinalGold)
        // Altın bayrağı var ama item tam dolu değil → altın dalına girilmez
        val yarim = ders(stepCount = 2).copy(finalGoldVisualUnlocked = true, stepCompletionStatus = listOf(true, false))
        assertEquals(ProgressAnimationAction.SHOW_TARGET, LessonAdapterLogic.progressAnimationDecision(yarim, true, null, false, null).action)
    }

    @Test
    fun progressAnimationDecision_artisAnimasyonu() {
        val item = ders(stepCount = 3).copy(stepCompletionStatus = listOf(true, true, false))
        // Bekleyen kayıt var ve tüketilebilir → animasyon
        val k = LessonAdapterLogic.progressAnimationDecision(item, false, pendingFromFilled = 1, canConsumePending = true, lastSeenFilled = 2)
        assertEquals(ProgressAnimationAction.ANIMATE_INCREASE, k.action)
        assertEquals(3, k.segmentCount)
        assertEquals(1, k.previousFilled)
        assertEquals(2, k.targetFilled)
        // 1 dilim × 900 ms ama en az 1800 ms
        assertEquals(1800L, k.durationMs)
        assertFalse(k.playGoldAfterIncrease)
        assertTrue(k.removePending)
        assertEquals(2, k.lastSeenFilled)
        // 3 dilim birden (0 → 3) → 2700 ms ve sonunda altın
        val bitmis = ders(stepCount = 3).copy(stepIsFinish = true)
        val k3 = LessonAdapterLogic.progressAnimationDecision(bitmis, false, 0, true, null)
        assertEquals(ProgressAnimationAction.ANIMATE_INCREASE, k3.action)
        assertEquals(2700L, k3.durationMs)
        assertTrue(k3.playGoldAfterIncrease)
        // Bekleyen değer sınır dışıysa kırpılır
        assertEquals(0, LessonAdapterLogic.progressAnimationDecision(item, false, -5, true, null).previousFilled)
    }

    @Test
    fun progressAnimationDecision_bekleyenVarAmaTuketilemez() {
        val item = ders(stepCount = 3).copy(stepCompletionStatus = listOf(true, true, false))
        val k = LessonAdapterLogic.progressAnimationDecision(item, false, pendingFromFilled = 1, canConsumePending = false, lastSeenFilled = 2)
        assertEquals(ProgressAnimationAction.SHOW_PREVIOUS, k.action)
        assertEquals(1, k.previousFilled)
        assertEquals(1, k.lastSeenFilled)
        assertFalse(k.removePending)
    }

    @Test
    fun progressAnimationDecision_bekleyenYok() {
        val item = ders(stepCount = 3).copy(stepCompletionStatus = listOf(true, true, false))
        // Son görülen daha düşük ama bekleyen kayıt yok → animasyonsuz hedef
        val k = LessonAdapterLogic.progressAnimationDecision(item, false, null, true, lastSeenFilled = 1)
        assertEquals(ProgressAnimationAction.SHOW_TARGET, k.action)
        assertEquals(1, k.previousFilled)
        assertEquals(2, k.lastSeenFilled)
        // Hiç görülmemiş → previous = target
        val k2 = LessonAdapterLogic.progressAnimationDecision(item, false, null, false, null)
        assertEquals(ProgressAnimationAction.SHOW_TARGET, k2.action)
        assertEquals(2, k2.previousFilled)
        // Bekleyen değer hedefe eşit/büyük ve tüketilebilir → kayıt silinir, animasyon yok
        val k3 = LessonAdapterLogic.progressAnimationDecision(item, false, 2, true, null)
        assertEquals(ProgressAnimationAction.SHOW_TARGET, k3.action)
        assertTrue(k3.removePending)
        // Aynı durumda tüketilemiyorsa kayıt kalır
        assertFalse(LessonAdapterLogic.progressAnimationDecision(item, false, 2, false, null).removePending)
    }

    @Test
    fun progressAnimationDecision_ilkKezTamDoluGorulurseAltinOynar() {
        val bitmis = ders(stepCount = 2).copy(stepIsFinish = true)
        val k = LessonAdapterLogic.progressAnimationDecision(bitmis, false, null, false, lastSeenFilled = 2)
        assertEquals(ProgressAnimationAction.SHOW_TARGET_AND_PLAY_GOLD, k.action)
        assertEquals(2, k.lastSeenFilled)
        assertEquals(0L, k.durationMs)
    }

    @Test
    fun shouldStartBreathingAnimation() {
        val bitmis = ders().copy(stepIsFinish = true)
        // Güncel Android: yalnızca AÇIK ve BİTMEMİŞ kart ("sıradaki ders") nefes alır; kilitli ve bitmiş kart almaz
        assertTrue(LessonAdapterLogic.shouldStartBreathingAnimation(ders(), false))
        assertFalse(LessonAdapterLogic.shouldStartBreathingAnimation(ders(isCompleted = false), false))
        assertFalse(LessonAdapterLogic.shouldStartBreathingAnimation(bitmis, false))
        assertFalse(LessonAdapterLogic.shouldStartBreathingAnimation(bitmis, true))
        assertFalse(LessonAdapterLogic.shouldStartBreathingAnimation(bitmis.copy(finalGoldVisualUnlocked = true), false))
        // Altın bayrağı var ama bitmemiş → nefes animasyonu sürer
        assertTrue(LessonAdapterLogic.shouldStartBreathingAnimation(ders().copy(finalGoldVisualUnlocked = true), true))
    }

    @Test
    fun cardState_kilitliAcikAltin() {
        assertEquals(LessonAdapterLogic.CardState.LOCKED, LessonAdapterLogic.cardState(ders(isCompleted = false)))
        assertEquals(LessonAdapterLogic.CardState.OPEN, LessonAdapterLogic.cardState(ders()))
        val bitmis = ders().copy(stepIsFinish = true)
        // Bitmiş ama altın dönüşümü henüz oynamadı: kart hâlâ mavi (ilerleme animasyonu sürebilir)
        assertEquals(LessonAdapterLogic.CardState.OPEN, LessonAdapterLogic.cardState(bitmis))
        assertEquals(LessonAdapterLogic.CardState.DONE, LessonAdapterLogic.cardState(bitmis, goldAnimationPlayed = true))
        assertEquals(LessonAdapterLogic.CardState.DONE, LessonAdapterLogic.cardState(bitmis.copy(finalGoldVisualUnlocked = true)))
    }

    @Test
    fun sandikKartYildizlari_yeniTasarim() {
        // Bitmemiş sandık: hiç yıldız yok
        assertEquals(0, LessonAdapterLogic.chestStarCount(sandik().copy(stepCupIcon = "chest_stars_tier3")))
        val b = { icon: String -> sandik().copy(stepIsFinish = true, stepCupIcon = icon) }
        assertEquals(3, LessonAdapterLogic.chestStarCount(b("chest_stars_tier3")))
        assertEquals(3, LessonAdapterLogic.chestStarCount(b("cup_ic3")))
        assertEquals(2, LessonAdapterLogic.chestStarCount(b("chest_stars_tier2")))
        assertEquals(2, LessonAdapterLogic.chestStarCount(b("cup_ic2")))
        assertEquals(1, LessonAdapterLogic.chestStarCount(b("chest_stars_tier1")))
        assertEquals(1, LessonAdapterLogic.chestStarCount(b("cup_ic")))
        // tier0 ve bilinmeyen görsel (kör dersin star_on_ic'i dahil) → 0
        assertEquals(0, LessonAdapterLogic.chestStarCount(b("chest_stars_tier0")))
        assertEquals(0, LessonAdapterLogic.chestStarCount(b("star_on_ic")))
        // chestCardStars: eski boş görsel tier0'a çekilir; açık/bitmiş/kilitli bayrakları
        val bos = b("")
        val k = LessonAdapterLogic.chestCardStars(bos)
        assertEquals("chest_stars_tier0", bos.stepCupIcon)
        assertEquals(LessonAdapterLogic.ChestCardStars(0, unlocked = true, done = false), k)
        assertEquals(1f, k.emptyStarAlpha)
        assertEquals(0.55f, LessonAdapterLogic.chestCardStars(b("chest_stars_tier2").copy(isCompleted = false)).emptyStarAlpha)
        val altin = LessonAdapterLogic.chestCardStars(b("chest_stars_tier2"), goldDone = true)
        assertEquals(2, altin.earned)
        assertEquals(0.3f, altin.emptyStarAlpha)
    }

    @Test
    fun kartIkonu() {
        assertEquals("podium_ic2", LessonAdapterLogic.cardIconName(sandik(), 1))
        assertEquals("podium_ic2", LessonAdapterLogic.cardIconName(sandik(), 6))
        assertEquals("profile_book_ic3", LessonAdapterLogic.cardIconName(sandik(), 4))
        assertEquals("profile_book_ic3", LessonAdapterLogic.cardIconName(ders(), 1))
    }

    @Test
    fun chestStarsRow() {
        val Y = ChestStarSlot.YellowOn
        val G = ChestStarSlot.LightGrayOn
        val O = ChestStarSlot.Off
        assertEquals(LessonAdapterLogic.ChestStarsRow(false, listOf(Y)), LessonAdapterLogic.chestStarsRow("chest_stars_tier0", true))
        assertEquals(LessonAdapterLogic.ChestStarsRow(false, listOf(G)), LessonAdapterLogic.chestStarsRow("chest_stars_tier0", false))
        assertEquals(LessonAdapterLogic.ChestStarsRow(false, listOf(Y)), LessonAdapterLogic.chestStarsRow("", true))
        assertEquals(LessonAdapterLogic.ChestStarsRow(true, listOf(Y, O, O)), LessonAdapterLogic.chestStarsRow("chest_stars_tier1", true))
        assertEquals(LessonAdapterLogic.ChestStarsRow(true, listOf(Y, Y, O)), LessonAdapterLogic.chestStarsRow("chest_stars_tier2", false))
        assertEquals(LessonAdapterLogic.ChestStarsRow(true, listOf(Y, Y, Y)), LessonAdapterLogic.chestStarsRow("chest_stars_tier3", true))
        assertEquals(LessonAdapterLogic.ChestStarsRow(true, listOf(Y, O, O)), LessonAdapterLogic.chestStarsRow("cup_ic", true))
        assertEquals(LessonAdapterLogic.ChestStarsRow(true, listOf(Y, Y, O)), LessonAdapterLogic.chestStarsRow("cup_ic2", true))
        assertEquals(LessonAdapterLogic.ChestStarsRow(true, listOf(Y, Y, Y)), LessonAdapterLogic.chestStarsRow("cup_ic3", true))
        // Bilinmeyen görsel → tek yıldız
        assertEquals(LessonAdapterLogic.ChestStarsRow(false, listOf(G)), LessonAdapterLogic.chestStarsRow("star_on_ic", false))
        assertEquals(LessonAdapterLogic.ChestStarsRow(false, listOf(Y)), LessonAdapterLogic.chestStarsRow("baska", true))
    }

    @Test
    fun normalizedCupIconVeBind() {
        for (bilinen in listOf(
            "chest_stars_tier0", "chest_stars_tier1", "chest_stars_tier2", "chest_stars_tier3", "cup_ic", "cup_ic2", "cup_ic3",
        )) {
            assertEquals(bilinen, LessonAdapterLogic.normalizedCupIcon(bilinen))
        }
        assertEquals("chest_stars_tier0", LessonAdapterLogic.normalizedCupIcon(""))
        assertEquals("chest_stars_tier0", LessonAdapterLogic.normalizedCupIcon("star_on_ic"))
        assertEquals("chest_stars_tier0", LessonAdapterLogic.normalizedCupIcon("x"))

        val Y = ChestStarSlot.YellowOn
        val O = ChestStarSlot.Off
        // Bitmiş sandık: saklanan görsel
        val bitmis = sandik().copy(stepIsFinish = true, stepCupIcon = "chest_stars_tier2")
        assertEquals(LessonAdapterLogic.ChestStarsRow(true, listOf(Y, Y, O)), LessonAdapterLogic.chestStarsForBind(bitmis))
        assertEquals("chest_stars_tier2", bitmis.stepCupIcon)
        // Bitmiş ama görsel boş/bilinmeyen → tier0'a çekilir (yerinde)
        val bos = sandik().copy(stepIsFinish = true, stepCupIcon = "")
        assertEquals(LessonAdapterLogic.ChestStarsRow(false, listOf(Y)), LessonAdapterLogic.chestStarsForBind(bos))
        assertEquals("chest_stars_tier0", bos.stepCupIcon)
        val kor = sandik().copy(stepIsFinish = true, stepCupIcon = "star_on_ic")
        assertEquals(LessonAdapterLogic.ChestStarsRow(false, listOf(Y)), LessonAdapterLogic.chestStarsForBind(kor))
        assertEquals("chest_stars_tier0", kor.stepCupIcon)
        // Bitmemiş sandık: görsel ne olursa olsun tier0 ve item üzerinde sıfırlanır
        val bitmemis = sandik().copy(stepCupIcon = "chest_stars_tier3", isCompleted = false)
        assertEquals(LessonAdapterLogic.ChestStarsRow(false, listOf(ChestStarSlot.LightGrayOn)), LessonAdapterLogic.chestStarsForBind(bitmemis))
        assertEquals("chest_stars_tier0", bitmemis.stepCupIcon)
    }
}

class MapFragmentLogicTest {

    @BeforeTest
    fun sifirla() {
        GlobalLessonData.resetForTest()
        LessonManager.resetForTest()
    }

    @Test
    fun stickyHeaderFor() {
        val p1 = GlobalLessonData.createLessonItems(1)
        // İlk görünen bir ders → yukarıdaki en yakın başlık
        assertEquals(
            MapFragmentLogic.StickyHeader("Sayıları Abaküste Tanıma", "1. KISIM, 1. ÜNİTE", "lesson_header_blue"),
            MapFragmentLogic.stickyHeaderFor(p1, 3),
        )
        // İlk görünen başlığın kendisi
        assertEquals(
            MapFragmentLogic.StickyHeader("Kuralsız toplama", "1. KISIM, 1. ÜNİTE", "lesson_header_red"),
            MapFragmentLogic.stickyHeaderFor(p1, 5),
        )
        assertEquals("5'lik Toplama", MapFragmentLogic.stickyHeaderFor(p1, 15)?.headerTitle)
        assertEquals("lesson_header_pink", MapFragmentLogic.stickyHeaderFor(p1, 15)?.color)
        // Görünen öğe yok (NO_POSITION = -1) → başlık gizlenir
        assertNull(MapFragmentLogic.stickyHeaderFor(p1, -1))
        // Başlığı olmayan liste (part 7) → null
        assertNull(MapFragmentLogic.stickyHeaderFor(GlobalLessonData.createLessonItems(7), 5))
        // Üst satır stepCount/currentStep'ten üretilir
        val ozel = listOf(baslik("B").copy(stepCount = 2, currentStep = 3), ders())
        assertEquals("2. KISIM, 3. ÜNİTE", MapFragmentLogic.stickyHeaderFor(ozel, 1)?.sectionUnit)
        // Liste dışı pozisyon: aşağı inerek ilk başlığı bulur
        assertEquals("B", MapFragmentLogic.stickyHeaderFor(ozel, 10)?.headerTitle)
    }

    @Test
    fun resolveLessonOperationsId() {
        val dolu = setOf(5, 8)
        val bosMu: (Int) -> Boolean = { it !in dolu }
        // İstenen dolu → kendisi
        assertEquals(8, MapFragmentLogic.resolveLessonOperationsId(8, 1, bosMu))
        // Boşsa geriye doğru ilk dolu
        assertEquals(8, MapFragmentLogic.resolveLessonOperationsId(10, 1, bosMu))
        assertEquals(5, MapFragmentLogic.resolveLessonOperationsId(7, 1, bosMu))
        // Alt sınıra kadar bulunamazsa alt sınır döner (boş olsa bile)
        assertEquals(6, MapFragmentLogic.resolveLessonOperationsId(7, 6, bosMu))
        assertEquals(1, MapFragmentLogic.resolveLessonOperationsId(4, 1, bosMu))
        // İstenen zaten alt sınırın altındaysa azaltılmaz
        assertEquals(3, MapFragmentLogic.resolveLessonOperationsId(3, 4, bosMu))
        assertEquals(600L, MapFragmentLogic.GUIDE_BOTTOM_SHEET_DELAY_MS)
    }

    @Test
    fun marathonGuide() {
        assertEquals("GuidePanelPrefs", MarathonGuideLogic.PREFS)
        assertEquals("marathon_guide_shown", MarathonGuideLogic.KEY_SHOWN)
        assertEquals("marathon_guide_pending", MarathonGuideLogic.KEY_PENDING)
        assertEquals("cupFragment_guide_shown", MarathonGuideLogic.LEGACY_SHOWN)
        assertEquals(1, MarathonGuideLogic.GUIDE_PART_ID)
        assertEquals(4, MarathonGuideLogic.firstChestLessonIndexForPart(1))
        assertEquals(3, MarathonGuideLogic.firstChestLessonIndexForPart(2))
        assertNull(MarathonGuideLogic.firstChestLessonIndexForPart(7))
        assertEquals(4, MarathonGuideLogic.firstMarathonLessonIndex())

        val p1 = GlobalLessonData.createLessonItems(1)
        // Uygun: part 1, ilk sandık, henüz bitmemiş
        assertNull(MarathonGuideLogic.eligibilityRejectReason(1, 4, p1[4]))
        assertTrue(MarathonGuideLogic.shouldSchedule(1, 4, p1[4]))
        // Red nedenleri (kontrol sırası Android ile aynı)
        assertEquals("not_type_chest(type=0)", MarathonGuideLogic.eligibilityRejectReason(1, 1, p1[1]))
        assertEquals(
            "step_already_finish_before_claim",
            MarathonGuideLogic.eligibilityRejectReason(1, 4, p1[4].copy(stepIsFinish = true)),
        )
        assertEquals("wrong_part(part=2 need=1)", MarathonGuideLogic.eligibilityRejectReason(2, 4, p1[4]))
        assertEquals(
            "index_mismatch(mapFragmentStepIndex=10 expectedListIdx=4 item.mapFragmentIndex=10)",
            MarathonGuideLogic.eligibilityRejectReason(1, 10, p1[10]),
        )
        assertFalse(MarathonGuideLogic.shouldSchedule(1, 10, p1[10]))
        assertFalse(MarathonGuideLogic.shouldSchedule(1, 4, null))
    }

    @Test
    fun guidePanelVerisi() {
        val d = marathonGuidePanelData()
        assertEquals(
            listOf(
                // Güncel Android: görsel kimliği yerine Sobi hâli (BunnyMascotView.Emote enum adı)
                GuidePanelData("TEACH_TALK", "Ünite maratonunda hatasız en hızlı çözümler kaydedilir."),
                GuidePanelData(
                    "TEACH_POINT",
                    "Ünite maratonu panelindeki Rekor'a tıklayarak liderlik tablosunu ve kendi sıranı görebilirsin.",
                ),
            ),
            d,
        )
    }
}

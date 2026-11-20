package com.sorobit.shared.ders

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Testlerde ortak kullanılan kısa item üreticileri. */
internal fun ders(
    stepCount: Int = 3,
    currentStep: Int = 1,
    isCompleted: Boolean = true,
    title: String = "Ders",
): LessonItem = LessonItem(
    type = LessonItem.TYPE_LESSON,
    title = title,
    offset = 0,
    isCompleted = isCompleted,
    stepCount = stepCount,
    currentStep = currentStep,
)

internal fun sandik(
    cupPoint1: Int? = 1400,
    cupPoint2: Int? = 1200,
    record: Int? = null,
    title: String = "Ünite Maratonu",
): LessonItem = LessonItem(
    type = LessonItem.TYPE_CHEST,
    title = title,
    offset = 0,
    isCompleted = true,
    stepCount = 1,
    currentStep = 1,
    cupPoint1 = cupPoint1,
    cupPoint2 = cupPoint2,
    record = record,
)

internal fun baslik(title: String = "Başlık"): LessonItem = LessonItem(
    type = LessonItem.TYPE_HEADER,
    title = title,
    offset = 0,
    isCompleted = false,
    stepCount = 1,
    currentStep = 1,
    color = "lesson_header_blue",
)

class LessonItemTest {

    @Test
    fun varsayilanStepCompletionStatus_stepCountKadarFalse() {
        assertEquals(listOf(false, false, false), ders(stepCount = 3).stepCompletionStatus)
        assertEquals(emptyList(), ders(stepCount = 0).stepCompletionStatus)
        assertEquals("chest_stars_tier0", ders().stepCupIcon)
        assertEquals(0, ders().tutorialNumber)
    }

    @Test
    fun minLessonOperationsId() {
        // finish - stepCount + 1
        assertEquals(8, ders(stepCount = 3).copy(finishStepNumber = 10, startStepNumber = 99).minLessonOperationsId())
        // finish yoksa start kullanılır
        assertEquals(5, ders(stepCount = 3).copy(startStepNumber = 7).minLessonOperationsId())
        // ikisi de yoksa 1
        assertEquals(-1, ders(stepCount = 3).minLessonOperationsId())
        assertEquals(1, ders(stepCount = 1).minLessonOperationsId())
        // Katalog: part 1 "10'luk Toplama - 3 Basamaklı" start 28, finish 31, stepCount 3 → 29 (Android tutarsızlığı aynen)
        val p1 = GlobalLessonData.createLessonItems(1)
        assertEquals(29, p1[19].minLessonOperationsId())
        assertEquals(1, p1[1].minLessonOperationsId())
    }

    @Test
    fun leaderboardSubmitScore_tumDallar() {
        // Sandık değil → null
        assertNull(ders().copy(leaderboardSeasonId = 3, leaderboardSeasonBest = 900).leaderboardSubmitScore(3))
        // Sezon kimliği yok → null
        assertNull(sandik().copy(leaderboardSeasonBest = 900).leaderboardSubmitScore(3))
        // Sezon uyuşmuyor → null
        assertNull(sandik().copy(leaderboardSeasonId = 2, leaderboardSeasonBest = 900).leaderboardSubmitScore(3))
        // En iyi yok → null
        assertNull(sandik().copy(leaderboardSeasonId = 3).leaderboardSubmitScore(3))
        // 0 ya da negatif → null
        assertNull(sandik().copy(leaderboardSeasonId = 3, leaderboardSeasonBest = 0).leaderboardSubmitScore(3))
        assertNull(sandik().copy(leaderboardSeasonId = 3, leaderboardSeasonBest = -5).leaderboardSubmitScore(3))
        // Geçerli
        assertEquals(900, sandik().copy(leaderboardSeasonId = 3, leaderboardSeasonBest = 900).leaderboardSubmitScore(3))
        // record tek başına yazılmaz
        assertNull(sandik(record = 2000).leaderboardSubmitScore(3))
    }

    @Test
    fun mergeChestRun_sandikDegilseHata() {
        assertFailsWith<IllegalArgumentException> { LessonItem.mergeChestRun(ders(), 100, 1) }
    }

    @Test
    fun mergeChestRun_rekorDallari() {
        // İlk koşu: rekor yok → runScore
        var m = LessonItem.mergeChestRun(sandik(record = null), 700, 4)
        assertEquals(700, m.record)
        assertEquals(4, m.leaderboardSeasonId)
        assertEquals(700, m.leaderboardSeasonBest)
        // Daha yüksek → güncellenir
        m = LessonItem.mergeChestRun(sandik(record = 700), 900, 4)
        assertEquals(900, m.record)
        // Eşit ya da düşük → eski rekor kalır
        assertEquals(700, LessonItem.mergeChestRun(sandik(record = 700), 700, 4).record)
        assertEquals(700, LessonItem.mergeChestRun(sandik(record = 700), 100, 4).record)
        // İlk koşu 0 puan: rekor 0 olur (null değil)
        assertEquals(0, LessonItem.mergeChestRun(sandik(record = null), 0, 4).record)
    }

    @Test
    fun mergeChestRun_sezonDallari() {
        // Aynı sezon: max(önceki en iyi, koşu)
        var onceki = sandik(record = 1500).copy(leaderboardSeasonId = 4, leaderboardSeasonBest = 1000)
        assertEquals(1200, LessonItem.mergeChestRun(onceki, 1200, 4).leaderboardSeasonBest)
        assertEquals(1000, LessonItem.mergeChestRun(onceki, 800, 4).leaderboardSeasonBest)
        // Aynı sezon ama en iyi null → max(0, koşu)
        onceki = sandik(record = 1500).copy(leaderboardSeasonId = 4, leaderboardSeasonBest = null)
        assertEquals(300, LessonItem.mergeChestRun(onceki, 300, 4).leaderboardSeasonBest)
        // Sezon değişti: önceki sezon skoru taşınmaz, koşu skoru yazılır (rekor ise tüm zamanların en iyisi kalır)
        onceki = sandik(record = 1500).copy(leaderboardSeasonId = 3, leaderboardSeasonBest = 1500)
        val m = LessonItem.mergeChestRun(onceki, 200, 4)
        assertEquals(1500, m.record)
        assertEquals(4, m.leaderboardSeasonId)
        assertEquals(200, m.leaderboardSeasonBest)
        // Diğer alanlar korunur
        assertEquals(onceki.copy(leaderboardSeasonId = 4, leaderboardSeasonBest = 200), m)
    }
}

class ChestTypeProgressHelperTest {

    @BeforeTest
    fun sifirla() {
        GlobalLessonData.resetForTest()
        LessonManager.resetForTest()
    }

    @Test
    fun resolvedChestIcon_esikler() {
        val s = sandik(cupPoint1 = 1400, cupPoint2 = 1200)
        assertEquals("chest_stars_tier0", ChestTypeProgressHelper.resolvedChestIcon(s, 0, 1))
        assertEquals("chest_stars_tier0", ChestTypeProgressHelper.resolvedChestIcon(s, 499, 1))
        assertEquals("chest_stars_tier1", ChestTypeProgressHelper.resolvedChestIcon(s, 500, 1))
        assertEquals("chest_stars_tier1", ChestTypeProgressHelper.resolvedChestIcon(s, 1199, 1))
        assertEquals("chest_stars_tier2", ChestTypeProgressHelper.resolvedChestIcon(s, 1200, 1))
        assertEquals("chest_stars_tier2", ChestTypeProgressHelper.resolvedChestIcon(s, 1399, 1))
        assertEquals("chest_stars_tier3", ChestTypeProgressHelper.resolvedChestIcon(s, 1400, 1))
        assertEquals("chest_stars_tier3", ChestTypeProgressHelper.resolvedChestIcon(s, 99999, 1))
        // Negatif skor 0'a yuvarlanır
        assertEquals("chest_stars_tier0", ChestTypeProgressHelper.resolvedChestIcon(s, -50, 1))
    }

    @Test
    fun resolvedChestIcon_cupPointYoksa() {
        val s = sandik(cupPoint1 = null, cupPoint2 = null)
        assertEquals("chest_stars_tier1", ChestTypeProgressHelper.resolvedChestIcon(s, 5000, 1))
        assertEquals("chest_stars_tier0", ChestTypeProgressHelper.resolvedChestIcon(s, 499, 1))
        // Yalnızca cupPoint2 var
        val s2 = sandik(cupPoint1 = null, cupPoint2 = 800)
        assertEquals("chest_stars_tier2", ChestTypeProgressHelper.resolvedChestIcon(s2, 5000, 1))
        // cupPoint2 500'ün altındaysa 2 yıldız 1 yıldızdan önce gelir (sıra: p1, p2, 500)
        val s3 = sandik(cupPoint1 = 1000, cupPoint2 = 300)
        assertEquals("chest_stars_tier2", ChestTypeProgressHelper.resolvedChestIcon(s3, 300, 1))
    }

    @Test
    fun resolvedChestIcon_korDers_part6Haric() {
        val kor = sandik().copy(isBlinding = true)
        assertEquals("star_on_ic", ChestTypeProgressHelper.resolvedChestIcon(kor, 5000, 4))
        assertEquals("star_on_ic", ChestTypeProgressHelper.resolvedChestIcon(kor, 0, 5))
        assertEquals("chest_stars_tier3", ChestTypeProgressHelper.resolvedChestIcon(kor, 5000, 6))
        // isBlinding false/null → normal
        assertEquals("chest_stars_tier3", ChestTypeProgressHelper.resolvedChestIcon(sandik().copy(isBlinding = false), 5000, 4))
    }

    @Test
    fun resolvedChestIcon_varsayilanGlobalPartIdKullanir() {
        val kor = sandik().copy(isBlinding = true)
        GlobalLessonData.globalPartId = 6
        assertEquals("chest_stars_tier3", ChestTypeProgressHelper.resolvedChestIcon(kor, 5000))
        GlobalLessonData.globalPartId = 4
        assertEquals("star_on_ic", ChestTypeProgressHelper.resolvedChestIcon(kor, 5000))
    }

    @Test
    fun starCountForChestIcon() {
        assertEquals(3, ChestTypeProgressHelper.starCountForChestIcon("chest_stars_tier3"))
        assertEquals(2, ChestTypeProgressHelper.starCountForChestIcon("chest_stars_tier2"))
        assertEquals(1, ChestTypeProgressHelper.starCountForChestIcon("chest_stars_tier1"))
        assertEquals(0, ChestTypeProgressHelper.starCountForChestIcon("chest_stars_tier0"))
        assertEquals(0, ChestTypeProgressHelper.starCountForChestIcon("star_on_ic"))
        // Eski kupa görselleri yıldız sayımında 0 (Android: else -> 0)
        assertEquals(0, ChestTypeProgressHelper.starCountForChestIcon("cup_ic3"))
        assertEquals(0, ChestTypeProgressHelper.starCountForChestIcon(""))
    }

    @Test
    fun shouldIncrementKarateForFirstThreeStars() {
        // Sandık değil → false
        assertFalse(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(ders(), 9999, 1))
        // 0 yıldızdan 3 yıldıza
        assertTrue(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(sandik(), 1400, 1))
        // Bu koşu yetmiyor ama eski rekor yetiyor (görsel henüz tier3 değil) → true
        assertTrue(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(sandik(record = 1500), 100, 1))
        // Zaten 3 yıldız → false
        val uc = sandik(record = 1500).copy(stepCupIcon = "chest_stars_tier3")
        assertFalse(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(uc, 2000, 1))
        // 2 yıldızda kalıyor → false
        assertFalse(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(sandik(), 1399, 1))
        // Kör ders (part 6 değil): görsel star_on_ic (0 yıldız) → asla true
        assertFalse(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(sandik().copy(isBlinding = true), 9999, 4))
        assertTrue(ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars(sandik().copy(isBlinding = true), 9999, 6))
    }
}

class LessonProgressMergeTest {

    @BeforeTest
    fun sifirla() {
        GlobalLessonData.resetForTest()
        LessonManager.resetForTest()
    }

    @Test
    fun progressRank() {
        assertEquals(0, LessonProgressMerge.progressRank(baslik().copy(stepIsFinish = true)))
        // currentStep + 1000*dolu adım + 10 (isCompleted)
        val d = ders(currentStep = 2).copy(stepCompletionStatus = listOf(true, false, false))
        assertEquals(2 + 1000 + 10, LessonProgressMerge.progressRank(d))
        assertEquals(2 + 1000 + 10 + 100_000, LessonProgressMerge.progressRank(d.copy(stepIsFinish = true)))
        assertEquals(1, LessonProgressMerge.progressRank(ders(currentStep = 1, isCompleted = false)))
        // Negatif currentStep 0 sayılır
        assertEquals(0, LessonProgressMerge.progressRank(ders(currentStep = -3, isCompleted = false)))
        // Sandık: record / 1000 eklenir (tam sayı bölme)
        assertEquals(1 + 10 + 1, LessonProgressMerge.progressRank(sandik(record = 1999)))
        assertEquals(1 + 10, LessonProgressMerge.progressRank(sandik(record = 999)))
        // Ders için record sayılmaz
        assertEquals(1 + 10, LessonProgressMerge.progressRank(ders().copy(record = 5000)))
    }

    @Test
    fun listeBirlestirme_bosVeFarkliBoyut() {
        val a = listOf(ders(title = "a"))
        assertEquals(a, LessonProgressMerge.mergeListsPreferMoreProgress(emptyList(), a, 1, 1))
        assertEquals(a, LessonProgressMerge.mergeListsPreferMoreProgress(a, emptyList(), 1, 1))
        val uzun = listOf(ders(title = "a"), ders(title = "b"))
        assertEquals(listOf("a", "b"), LessonProgressMerge.mergeListsPreferMoreProgress(a, uzun, 1, 1).map { it.title })
        assertEquals(listOf("a", "b"), LessonProgressMerge.mergeListsPreferMoreProgress(uzun, a, 1, 1).map { it.title })
    }

    @Test
    fun dersBirlestirme_yerelIleride() {
        val local = ders(currentStep = 2).copy(stepCompletionStatus = listOf(true, false, false), startStepNumber = 9)
        val remote = ders(currentStep = 1).copy(startStepNumber = 8, tutorialIsFinish = true)
        val m = LessonProgressMerge.mergeItemPreferMoreProgress(local, remote, 1, 1)
        assertEquals(2, m.currentStep)
        assertEquals(listOf(true, false, false), m.stepCompletionStatus)
        assertFalse(m.stepIsFinish)
        // Kazanan (yerel) taraftan gelen diğer alanlar
        assertEquals(9, m.startStepNumber)
        // tutorialIsFinish VEYA'lanır
        assertTrue(m.tutorialIsFinish)
    }

    @Test
    fun dersBirlestirme_uzakBitmis() {
        val local = ders(currentStep = 2).copy(stepCompletionStatus = listOf(true, false, false))
        val remote = ders(currentStep = 3).copy(
            stepCompletionStatus = listOf(true, true, true),
            stepIsFinish = true,
            finalGoldVisualUnlocked = true,
            stepCupIcon = "cup_ic",
        )
        val m = LessonProgressMerge.mergeItemPreferMoreProgress(local, remote, 1, 1)
        assertTrue(m.stepIsFinish)
        assertEquals(3, m.currentStep)
        assertEquals(listOf(true, true, true), m.stepCompletionStatus)
        assertTrue(m.finalGoldVisualUnlocked)
        // Sandık olmayan item'da görsel kazanan taraftan (uzak) alınır
        assertEquals("cup_ic", m.stepCupIcon)
    }

    @Test
    fun dersBirlestirme_bitmisseTumAdimlarDoluVeCurrentStepEnAzStepCount() {
        // Eski bulut snapshot'ı: yerel bitmiş, uzak bitmemiş ve currentStep 1
        val local = ders(stepCount = 3, currentStep = 1).copy(stepIsFinish = true)
        val remote = ders(stepCount = 3, currentStep = 1)
        val m = LessonProgressMerge.mergeItemPreferMoreProgress(local, remote, 1, 1)
        assertTrue(m.stepIsFinish)
        assertEquals(3, m.currentStep)
        assertEquals(listOf(true, true, true), m.stepCompletionStatus)
    }

    @Test
    fun dersBirlestirme_stepCountBuyukOlanVeDurumVeyalanir() {
        val local = ders(stepCount = 2, currentStep = 1).copy(stepCompletionStatus = listOf(true, false))
        val remote = ders(stepCount = 3, currentStep = 1).copy(stepCompletionStatus = listOf(false, false, true))
        val m = LessonProgressMerge.mergeItemPreferMoreProgress(local, remote, 1, 1)
        assertEquals(3, m.stepCount)
        assertEquals(listOf(true, false, true), m.stepCompletionStatus)
    }

    @Test
    fun dersBirlestirme_esitRankteYerelKazanir_isCompletedVeyalanir() {
        val local = ders(isCompleted = false).copy(raceBusyLevel = 1, lessonHint = "yerel")
        val remote = ders(isCompleted = false).copy(raceBusyLevel = 2, lessonHint = "uzak")
        val m = LessonProgressMerge.mergeItemPreferMoreProgress(local, remote, 1, 1)
        assertEquals("yerel", m.lessonHint)
        assertEquals(1, m.raceBusyLevel)
        // Uzak daha ileride → raceBusyLevel uzaktan; isCompleted VEYA
        val remote2 = ders(isCompleted = true).copy(raceBusyLevel = 0, lessonHint = "uzak")
        val m2 = LessonProgressMerge.mergeItemPreferMoreProgress(local, remote2, 1, 1)
        assertEquals(0, m2.raceBusyLevel)
        assertEquals("uzak", m2.lessonHint)
        assertTrue(m2.isCompleted)
    }

    @Test
    fun farkliTurVeBaslik_rankeGoreSecilir() {
        val d = ders(currentStep = 2)
        val b = baslik()
        assertSame(d, LessonProgressMerge.mergeItemPreferMoreProgress(d, b, 1, 1))
        assertSame(d, LessonProgressMerge.mergeItemPreferMoreProgress(b, d, 1, 1))
        // İki başlık: rank 0 == 0 → yerel
        val b1 = baslik("yerel")
        val b2 = baslik("uzak")
        assertSame(b1, LessonProgressMerge.mergeItemPreferMoreProgress(b1, b2, 1, 1))
    }

    @Test
    fun sandikBirlestirme_rekorSezonVeGorsel() {
        val local = sandik(record = 1200).copy(
            leaderboardSeasonId = 5, leaderboardSeasonBest = 1200, stepCupIcon = "chest_stars_tier1", stepIsFinish = true,
        )
        val remote = sandik(record = 1500).copy(
            leaderboardSeasonId = 5, leaderboardSeasonBest = 900, stepCupIcon = "chest_stars_tier0",
        )
        val m = LessonProgressMerge.mergeItemPreferMoreProgress(local, remote, season = 5, globalPartId = 1)
        assertEquals(1500, m.record)
        assertEquals(5, m.leaderboardSeasonId)
        assertEquals(1200, m.leaderboardSeasonBest)
        // 1500 >= cupPoint1 (1400) → rekordan 3 yıldız; üçünün en çok yıldızlısı seçilir
        assertEquals("chest_stars_tier3", m.stepCupIcon)
        assertTrue(m.stepIsFinish)
    }

    @Test
    fun sandikBirlestirme_sezonDallari() {
        fun s(id: Int?, best: Int?) = sandik(record = 100).copy(leaderboardSeasonId = id, leaderboardSeasonBest = best)
        fun birlestir(l: LessonItem, r: LessonItem) = LessonProgressMerge.mergeItemPreferMoreProgress(l, r, 5, 1)

        // İkisi de bu sezon, ikisi de null → max(0,0)=0 → null
        var m = birlestir(s(5, null), s(5, null))
        assertNull(m.leaderboardSeasonBest)
        assertEquals(5, m.leaderboardSeasonId)
        // Yalnızca yerel bu sezon
        m = birlestir(s(5, 300), s(4, 9000))
        assertEquals(300, m.leaderboardSeasonBest)
        assertEquals(5, m.leaderboardSeasonId)
        // Yalnızca uzak bu sezon: en iyi uzaktan, AMA sezon kimliği yerelden (Android: localSeasonId ?: remoteSeasonId)
        m = birlestir(s(4, 9000), s(5, 300))
        assertEquals(300, m.leaderboardSeasonBest)
        assertEquals(4, m.leaderboardSeasonId)
        // Hiçbiri bu sezon değil: yerelin en iyisi, yoksa uzağın
        m = birlestir(s(3, 700), s(null, null))
        assertEquals(700, m.leaderboardSeasonBest)
        assertEquals(3, m.leaderboardSeasonId)
        m = birlestir(s(null, null), s(2, 650))
        assertEquals(650, m.leaderboardSeasonBest)
        assertEquals(2, m.leaderboardSeasonId)
        // İkisinde de sezon yok → mevcut sezon yazılır
        m = birlestir(s(null, null), s(null, null))
        assertNull(m.leaderboardSeasonBest)
        assertEquals(5, m.leaderboardSeasonId)
    }

    @Test
    fun sandikBirlestirme_rekorYoksaVeEsitYildizdaYerelGorsel() {
        val local = sandik(record = null).copy(stepCupIcon = "cup_ic3")
        val remote = sandik(record = null)
        val m = LessonProgressMerge.mergeItemPreferMoreProgress(local, remote, 5, 1)
        assertNull(m.record)
        // Üç adayın yıldız sayısı da 0 → maxByOrNull ilkini (yerel) döndürür
        assertEquals("cup_ic3", m.stepCupIcon)
    }

    @Test
    fun sandikBirlestirme_korDersteRekordanGorselStarOn() {
        val local = sandik(record = 9000).copy(isBlinding = true, stepCupIcon = "chest_stars_tier0")
        val remote = sandik(record = 100).copy(isBlinding = true, stepCupIcon = "chest_stars_tier0")
        // Part 4: rekordan görsel star_on_ic (0 yıldız) → eşitlikte yerel görsel kalır
        assertEquals("chest_stars_tier0", LessonProgressMerge.mergeItemPreferMoreProgress(local, remote, 5, 4).stepCupIcon)
        // Part 6: normal eşikler → 3 yıldız
        assertEquals("chest_stars_tier3", LessonProgressMerge.mergeItemPreferMoreProgress(local, remote, 5, 6).stepCupIcon)
    }

    @Test
    fun sameProgressState() {
        val a = listOf(ders(), sandik(record = 500))
        assertTrue(LessonProgressMerge.sameProgressState(a, a.map { it.copy() }))
        assertFalse(LessonProgressMerge.sameProgressState(a, a.take(1)))
        // Karşılaştırılan 8 alanın her biri
        fun fark(degistir: (LessonItem) -> LessonItem) =
            LessonProgressMerge.sameProgressState(a, listOf(degistir(a[0]), a[1]))
        assertFalse(fark { it.copy(stepIsFinish = true) })
        assertFalse(fark { it.copy(currentStep = 2) })
        assertFalse(fark { it.copy(isCompleted = false) })
        assertFalse(fark { it.copy(stepCompletionStatus = listOf(true, false, false)) })
        assertFalse(fark { it.copy(stepCupIcon = "cup_ic") })
        assertFalse(fark { it.copy(record = 1) })
        assertFalse(fark { it.copy(raceBusyLevel = 0) })
        assertFalse(fark { it.copy(finalGoldVisualUnlocked = true) })
        // Karşılaştırılmayan alanlar (ör. tutorialIsFinish, leaderboardSeasonBest) farkı görülmez
        assertTrue(fark { it.copy(tutorialIsFinish = true, leaderboardSeasonBest = 9, title = "x") })
    }

    @Test
    fun listeBirlestirme_indeksBazinda() {
        val local = listOf(baslik(), ders(currentStep = 2), sandik(record = 100))
        val remote = listOf(baslik(), ders(currentStep = 1), sandik(record = 800))
        val m = LessonProgressMerge.mergeListsPreferMoreProgress(local, remote, 1, 1)
        assertEquals(3, m.size)
        assertEquals(2, m[1].currentStep)
        assertEquals(800, m[2].record)
        assertEquals("chest_stars_tier1", m[2].stepCupIcon)
    }
}

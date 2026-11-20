// Android kupa depoları (*CupRepository.kt), CupHistoryRepository.kt, BadgePrecalcHelper.kt, CupFragment.kt ve
// CupHistoryFragment.kt saf mantığının testleri.
package com.sorobit.shared.kupa

import com.sorobit.shared.kupa.CupHistoryLogic.RangeTab
import com.sorobit.shared.kupa.rozet.BadgeAnimMode
import com.sorobit.shared.kupa.rozet.BadgeLevelUpPayload
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KupaDepoVeEkranTest {

    private val utc = TimeZone.UTC
    private val artiUc = UtcOffset(hours = 3).asTimeZone() // Türkiye saati (UTC+3)
    private val kasim21 = 1_795_219_200_000L // 2026-11-21T00:00:00Z
    private val ocak5 = 1_767_571_200_000L // 2026-01-05T00:00:00Z

    // ---------------------------------------------------------------- zaman yardımcıları

    @Test
    fun kupaCalendarDayIdBicimi() {
        assertEquals("2026-325", kupaCalendarDayId(kasim21, utc))
        // Sıfır dolgusu yok
        assertEquals("2026-5", kupaCalendarDayId(ocak5, utc))
        // Yerel gün: 20 Kasım 22:00 UTC, UTC+3'te 21 Kasım'dır
        assertEquals("2026-324", kupaCalendarDayId(kasim21 - 7_200_000L, utc))
        assertEquals("2026-325", kupaCalendarDayId(kasim21 - 7_200_000L, artiUc))
        // Yıl sonu: 31 Aralık 2026 = yılın 365. günü
        assertEquals("2026-365", kupaCalendarDayId(1_798_675_200_000L, utc))
        assertTrue(kupaCalendarDayId().matches(Regex("\\d{4}-\\d{1,3}")))
    }

    @Test
    fun kupaDateIdVeDaysAgo() {
        assertEquals("2026-11-21", kupaDateId(0, kasim21, utc))
        assertEquals("2026-11-21", CupHistoryRepository.todayId(kasim21, utc))
        assertEquals("2026-01-05", CupHistoryRepository.todayId(ocak5, utc))
        assertEquals("2026-11-20", CupHistoryRepository.todayId(kasim21 - 7_200_000L, utc))
        assertEquals("2026-11-21", CupHistoryRepository.todayId(kasim21 - 7_200_000L, artiUc))
        assertEquals("2026-11-14", CupHistoryLogic.daysAgo(7, kasim21, utc))
        assertEquals("2026-10-22", CupHistoryLogic.daysAgo(30, kasim21, utc))
        assertEquals("2025-11-21", CupHistoryLogic.daysAgo(365, kasim21, utc))
        assertEquals("2025-12-29", CupHistoryLogic.daysAgo(7, ocak5, utc))
        assertTrue(CupHistoryRepository.todayId().matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    // ---------------------------------------------------------------- kupa depoları

    @Test
    fun depoAlanAdlariVeYollar() {
        assertEquals("addition_abacus_cup", AbacusCupRepository.FIELD)
        assertEquals("extraction_abacus_cup", ExtractionCupRepository.FIELD)
        assertEquals("impact_abacus_cup", ImpactCupRepository.FIELD)
        assertEquals("blinding_addition_abacus_cup", BlindingAdditionCupRepository.FIELD)
        assertEquals("blinding_extraction_abacus_cup", BlindingExtractionCupRepository.FIELD)
        assertEquals("blinding_impact_abacus_cup", BlindingImpactCupRepository.FIELD)
        val hepsi = listOf(
            AbacusCupRepository, ExtractionCupRepository, ImpactCupRepository,
            BlindingAdditionCupRepository, BlindingExtractionCupRepository, BlindingImpactCupRepository,
        )
        for (d in hepsi) {
            assertEquals(200, d.DEFAULT_CUP_SCORE)
            assertEquals("users/u1/cupWayProgress/progress", d.progressDocumentPath("u1"))
            assertEquals(200, d.fetchFailureCupScore())
        }
        assertEquals(5, AbacusCupRepository.CUP_STEP)
        assertEquals(5, ExtractionCupRepository.CUP_STEP)
        assertEquals(5, ImpactCupRepository.CUP_STEP)
        assertEquals(5, BlindingAdditionCupRepository.CUP_STEP)
        assertEquals(5, BlindingExtractionCupRepository.CUP_STEP)
    }

    @Test
    fun resolveFetchedCupScore() {
        val d = AbacusCupRepository
        assertEquals(CupFetchResult(200, shouldSeed = true), d.resolveFetchedCupScore(null))
        assertEquals(CupFetchResult(200, shouldSeed = true), d.resolveFetchedCupScore("350"))
        assertEquals(CupFetchResult(350, shouldSeed = false), d.resolveFetchedCupScore(350L))
        assertEquals(CupFetchResult(0, shouldSeed = false), d.resolveFetchedCupScore(0))
        assertEquals(CupFetchResult(349, shouldSeed = false), d.resolveFetchedCupScore(349.9))
    }

    @Test
    fun computeUpdatedCupScore() {
        val d = BlindingImpactCupRepository
        val f = "blinding_impact_abacus_cup"
        assertEquals(
            CupUpdateResult(200, 224, mapOf(f to 224), mapOf(f to 224)),
            d.computeUpdatedCupScore(null, 24),
        )
        assertEquals(CupUpdateResult(500, 470, mapOf(f to 470), mapOf(f to 470)), d.computeUpdatedCupScore(500L, -30))
        // 0'ın altına düşemez
        assertEquals(CupUpdateResult(20, 0, mapOf(f to 0), mapOf(f to 0)), d.computeUpdatedCupScore(20L, -30))
        assertEquals(CupUpdateResult(0, 10, mapOf(f to 10), mapOf(f to 10)), d.computeUpdatedCupScore(0L, 10))
        // Sayı olmayan alan yok sayılır → varsayılan 200
        assertEquals(190, d.computeUpdatedCupScore("abc", -10).newScore)
        assertEquals("addition_abacus_cup", AbacusCupRepository.computeUpdatedCupScore(200L, 10).progressUpdate.keys.single())
    }

    // ---------------------------------------------------------------- CupHistoryRepository

    @Test
    fun cupHistoryYollarVeHarita() {
        assertEquals("users/u1/cupHistory", CupHistoryRepository.historyCollectionPath("u1"))
        assertEquals("users/u1/cupHistory/2026-11-21", CupHistoryRepository.historyDocumentPath("u1", "2026-11-21"))
        assertEquals(mapOf<String, Any>("impact_abacus_cup" to 315), CupHistoryRepository.recordSnapshot("impact_abacus_cup", 315))
    }

    @Test
    fun cupHistoryMapSnapshot() {
        val docs = listOf<Pair<String, Map<String, Any?>>>(
            "2026-11-20" to mapOf("addition_abacus_cup" to 230L, "impact_abacus_cup" to 100L),
            "2026-11-03" to mapOf("addition_abacus_cup" to 210.7),
            "2026-11-21" to mapOf("impact_abacus_cup" to 105L), // alan yok → atılır
            "2026-11-19" to mapOf("addition_abacus_cup" to "220"), // sayı değil → atılır
            "2026-10-30" to mapOf("addition_abacus_cup" to 205),
        )
        assertEquals(
            listOf("2026-10-30" to 205, "2026-11-03" to 210, "2026-11-20" to 230),
            CupHistoryRepository.mapSnapshot(docs, "addition_abacus_cup"),
        )
        assertEquals(
            listOf("2026-11-20" to 100, "2026-11-21" to 105),
            CupHistoryRepository.mapSnapshot(docs, "impact_abacus_cup"),
        )
        assertEquals(emptyList(), CupHistoryRepository.mapSnapshot(emptyList(), "addition_abacus_cup"))
    }

    // ---------------------------------------------------------------- BadgePrecalcHelper

    @Test
    fun resolvePendingDeltaIlkNullOlmayan() {
        val H = BadgePrecalcHelper
        assertNull(H.resolvePendingDelta(null, null, null, null, null, null))
        assertEquals(1, H.resolvePendingDelta(1, 2, 3, 4, 5, 6))
        assertEquals(2, H.resolvePendingDelta(null, 2, 3, 4, 5, 6))
        assertEquals(3, H.resolvePendingDelta(null, null, 3, 4, 5, 6))
        assertEquals(4, H.resolvePendingDelta(null, null, null, 4, 5, 6))
        assertEquals(5, H.resolvePendingDelta(null, null, null, null, 5, 6))
        assertEquals(6, H.resolvePendingDelta(null, null, null, null, null, 6))
        // 0 da geçerli bir deltadır (null değildir)
        assertEquals(0, H.resolvePendingDelta(0, 9, null, null, null, null))
    }

    @Test
    fun resolveCupPathKind() {
        val H = BadgePrecalcHelper
        assertEquals(CupPathKind.BLINDING_IMPACT, H.resolveCupPathKind(true, null, true))
        assertEquals(CupPathKind.BLINDING_IMPACT, H.resolveCupPathKind(true, true, true)) // çarpma önceliklidir
        assertEquals(CupPathKind.IMPACT, H.resolveCupPathKind(true, true, false))
        assertEquals(CupPathKind.IMPACT, H.resolveCupPathKind(true, null, null))
        assertEquals(CupPathKind.BLINDING_EXTRACTION, H.resolveCupPathKind(false, true, true))
        assertEquals(CupPathKind.EXTRACTION, H.resolveCupPathKind(null, true, null))
        assertEquals(CupPathKind.BLINDING_ADDITION, H.resolveCupPathKind(null, null, true))
        assertEquals(CupPathKind.ADDITION, H.resolveCupPathKind(null, null, null))
        assertEquals(CupPathKind.ADDITION, H.resolveCupPathKind(false, false, false))

        assertEquals(BlindingImpactCupRepository to BadgeAnimMode.TURTLE, CupPathKind.BLINDING_IMPACT.let { it.repository to it.badgeMode })
        assertEquals(ImpactCupRepository to BadgeAnimMode.GOAT, CupPathKind.IMPACT.let { it.repository to it.badgeMode })
        assertEquals(BlindingExtractionCupRepository to BadgeAnimMode.FLY, CupPathKind.BLINDING_EXTRACTION.let { it.repository to it.badgeMode })
        assertEquals(ExtractionCupRepository to BadgeAnimMode.CROCODILE, CupPathKind.EXTRACTION.let { it.repository to it.badgeMode })
        assertEquals(BlindingAdditionCupRepository to BadgeAnimMode.EAGLE, CupPathKind.BLINDING_ADDITION.let { it.repository to it.badgeMode })
        assertEquals(AbacusCupRepository to BadgeAnimMode.DINO, CupPathKind.ADDITION.let { it.repository to it.badgeMode })
    }

    @Test
    fun precalculate() {
        val H = BadgePrecalcHelper
        var r = H.precalculate(24, 490, null, null, null)
        assertEquals(CupPathKind.ADDITION, r.kind)
        assertEquals(490 to 514, r.oldScore to r.newScore)
        assertEquals(listOf(BadgeLevelUpPayload(BadgeAnimMode.DINO, 490, 500, 500)), r.payloadsLocally)

        r = H.precalculate(38, 990, true, null, true)
        assertEquals(listOf(BadgeLevelUpPayload(BadgeAnimMode.TURTLE, 990, 1000, 1000)), r.payloadsLocally)

        // Kayıp: rozet kutlaması yok
        r = H.precalculate(-30, 510, null, true, null)
        assertEquals(CupPathKind.EXTRACTION, r.kind)
        assertEquals(480, r.newScore)
        assertEquals(emptyList(), r.payloadsLocally)

        // Eski skor bilinmiyorsa 0 sayılır; sonuç 0'a kırpılmaz (Android'deki gibi)
        r = H.precalculate(-30, null, null, null, true)
        assertEquals(0 to -30, r.oldScore to r.newScore)
        assertEquals(CupPathKind.BLINDING_ADDITION, r.kind)
        assertEquals(emptyList(), r.payloadsLocally)

        // Eski skor 0 sayıldığında büyük delta birden çok kademe üretir
        r = H.precalculate(1000, null, true, null, null)
        assertEquals(
            listOf(
                BadgeLevelUpPayload(BadgeAnimMode.GOAT, 0, 500, 500),
                BadgeLevelUpPayload(BadgeAnimMode.GOAT, 500, 1000, 1000),
            ),
            r.payloadsLocally,
        )
    }

    // ---------------------------------------------------------------- CupFragment

    @Test
    fun resolveCupTier() {
        val L = CupFragmentLogic
        val bilgelik = L.resolveCupTier(100, 100, 50)
        assertEquals("cup_ic3" to "Bilgelik Kupası", bilgelik.icon to bilgelik.cupName)
        assertEquals(5, bilgelik.cupMotivation.size)
        val usta = L.resolveCupTier(99, 100, 50)
        assertEquals("cup_ic2" to "Usta Kupası", usta.icon to usta.cupName)
        assertEquals(6, usta.cupMotivation.size)
        val deneyim = L.resolveCupTier(49, 100, 50)
        assertEquals("cup_ic" to "Deneyim Kupası", deneyim.icon to deneyim.cupName)
        assertEquals(4, deneyim.cupMotivation.size)
        assertEquals("Her usta bir zamanlar amatördü. Sen de yoldasın!", deneyim.cupMotivation.first())
        val sifir = L.resolveCupTier(0, 100, 50)
        assertEquals("cup_ic" to "Deneyim Kupası", sifir.icon to sifir.cupName)
        assertEquals(6, sifir.cupMotivation.size)
        assertEquals("Ayıp olmasın diye bu kupayı verdik.", sifir.cupMotivation.first())
        // Eşik tanımlı değilse o kademe atlanır
        assertEquals("cup_ic2", L.resolveCupTier(500, null, 50).icon)
        assertEquals("cup_ic", L.resolveCupTier(500, null, null).icon)
        assertEquals(4, L.resolveCupTier(500, null, null).cupMotivation.size)
        assertEquals("cup_ic3", L.resolveCupTier(0, 0, null).icon) // 0 >= 0
        assertEquals(6, L.resolveCupTier(-5, null, null).cupMotivation.size)

        assertEquals(sifir.cupMotivation.first(), L.pickMotivation(sifir, SabitRandom()))
        val rnd = Random(5)
        repeat(30) { assertTrue(L.pickMotivation(usta, rnd) in usta.cupMotivation) }
    }

    @Test
    fun adimDurumu() {
        assertEquals(listOf(true, true, false), CupFragmentLogic.newStepCompletionStatus(3, 2))
        assertEquals(listOf(true), CupFragmentLogic.newStepCompletionStatus(1, 1))
        assertEquals(listOf(false, false), CupFragmentLogic.newStepCompletionStatus(2, 0))
        assertEquals(emptyList(), CupFragmentLogic.newStepCompletionStatus(0, 3))
        assertTrue(CupFragmentLogic.isLastStep(3, 3))
        assertFalse(CupFragmentLogic.isLastStep(3, 2))
    }

    // ---------------------------------------------------------------- CupHistoryFragment

    @Test
    fun sekmeler() {
        assertEquals(listOf(RangeTab.WEEKLY, RangeTab.MONTHLY, RangeTab.YEARLY, RangeTab.ALL), RangeTab.entries.toList())
        assertEquals(listOf("Haftalık", "Aylık", "Yıllık", "Tüm Zamanlar"), CupHistoryLogic.TAB_LABELS)
        assertEquals(7, CupHistoryLogic.windowDaysForTab(RangeTab.WEEKLY))
        assertEquals(30, CupHistoryLogic.windowDaysForTab(RangeTab.MONTHLY))
        assertEquals(365, CupHistoryLogic.windowDaysForTab(RangeTab.YEARLY))
        assertNull(CupHistoryLogic.windowDaysForTab(RangeTab.ALL))
        assertEquals("2026-11-14", CupHistoryLogic.sinceDateIdForTab(RangeTab.WEEKLY, kasim21, utc))
        assertEquals("2026-10-22", CupHistoryLogic.sinceDateIdForTab(RangeTab.MONTHLY, kasim21, utc))
        assertEquals("2025-11-21", CupHistoryLogic.sinceDateIdForTab(RangeTab.YEARLY, kasim21, utc))
        assertNull(CupHistoryLogic.sinceDateIdForTab(RangeTab.ALL, kasim21, utc))
        assertEquals(200, CupHistoryLogic.currentScoreFromField(null))
        assertEquals(200, CupHistoryLogic.currentScoreFromField("5"))
        assertEquals(345, CupHistoryLogic.currentScoreFromField(345L))
    }

    @Test
    fun niceAxisRange() {
        assertEquals(180 to 220, CupHistoryLogic.niceAxisRange(listOf(200)))
        assertEquals(140 to 460, CupHistoryLogic.niceAxisRange(listOf(205, 395, 300)))
        assertEquals(0 to 30, CupHistoryLogic.niceAxisRange(listOf(5, 10)))
        assertEquals(0 to 20, CupHistoryLogic.niceAxisRange(listOf(0)))
        assertEquals(180 to 230, CupHistoryLogic.niceAxisRange(listOf(201, 209)))
        assertFailsWith<NoSuchElementException> { CupHistoryLogic.niceAxisRange(emptyList()) }
    }

    @Test
    fun bucketVeGroupLastPerKey() {
        val p = listOf("2026-01-05" to 1, "2026-01-20" to 2, "2026-02-01" to 3, "2026-02-02" to 4, "2026-03-09" to 5)
        assertEquals(emptyList(), CupHistoryLogic.bucket(emptyList(), RangeTab.YEARLY))
        assertEquals(p, CupHistoryLogic.bucket(p, RangeTab.WEEKLY))
        assertEquals(p, CupHistoryLogic.bucket(p, RangeTab.MONTHLY))
        assertEquals(
            listOf("2026-01-20" to 2, "2026-02-02" to 4, "2026-03-09" to 5),
            CupHistoryLogic.bucket(p, RangeTab.YEARLY),
        )
        // Tüm Zamanlar: 60 noktaya kadar ham, fazlası aya göre gruplanır
        assertEquals(p, CupHistoryLogic.bucket(p, RangeTab.ALL))
        val altmis = (1..60).map { "2026-${(it % 12 + 1).toString().padStart(2, '0')}-01" to it }
        assertEquals(altmis, CupHistoryLogic.bucket(altmis, RangeTab.ALL))
        val altmisBir = (0..60).map { "20${(10 + it / 12)}-${(it % 12 + 1).toString().padStart(2, '0')}-0${it % 9 + 1}" to it }
        assertEquals(61, CupHistoryLogic.bucket(altmisBir, RangeTab.ALL).size) // her nokta ayrı ay
        val ayniAy = (1..61).map { "2026-05-${it.toString().padStart(2, '0')}" to it }
        assertEquals(listOf("2026-05-61" to 61), CupHistoryLogic.bucket(ayniAy, RangeTab.ALL))
        // Anahtar tekrar görülürse İLK görüldüğü sırada kalır, değer son noktadır
        assertEquals(
            listOf("a2" to 3, "b1" to 2),
            CupHistoryLogic.groupLastPerKey(listOf("a1" to 1, "b1" to 2, "a2" to 3)) { it.substring(0, 1) },
        )
        // 7 karakterden kısa tarih kimliği Android'deki gibi istisna atar
        assertFailsWith<IndexOutOfBoundsException> { CupHistoryLogic.bucket(listOf("2026" to 1), RangeTab.YEARLY) }
    }

    @Test
    fun formatLabel() {
        assertEquals("5 Kas", CupHistoryLogic.formatLabel(RangeTab.WEEKLY, "2026-11-05"))
        assertEquals("21 Kas", CupHistoryLogic.formatLabel(RangeTab.MONTHLY, "2026-11-21"))
        assertEquals("Kas 26", CupHistoryLogic.formatLabel(RangeTab.YEARLY, "2026-11-05"))
        assertEquals("Şub 09", CupHistoryLogic.formatLabel(RangeTab.ALL, "2009-02-01"))
        assertEquals("1 Oca", CupHistoryLogic.formatLabel(RangeTab.WEEKLY, "2026-01-01"))
        assertEquals("31 Ağu", CupHistoryLogic.formatLabel(RangeTab.WEEKLY, "2026-08-31"))
        assertEquals(
            listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"),
            (1..12).map { CupHistoryLogic.formatLabel(RangeTab.YEARLY, "2026-${it.toString().padStart(2, '0')}-15").substringBefore(' ') },
        )
        assertEquals("bozuk", CupHistoryLogic.formatLabel(RangeTab.WEEKLY, "bozuk"))
        assertEquals("2026-13-01", CupHistoryLogic.formatLabel(RangeTab.WEEKLY, "2026-13-01"))
    }

    @Test
    fun grafikKurallari() {
        assertTrue(CupHistoryLogic.isSinglePoint(listOf("2026-01-01" to 1)))
        assertFalse(CupHistoryLogic.isSinglePoint(listOf("2026-01-01" to 1, "2026-01-02" to 2)))
        assertEquals(1, CupHistoryLogic.xAxisLabelCount(0))
        assertEquals(4, CupHistoryLogic.xAxisLabelCount(4))
        assertEquals(6, CupHistoryLogic.xAxisLabelCount(30))
    }
}

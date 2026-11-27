package com.sorobit.shared.ders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ortak koddaki ders kataloğunun Android kaynağıyla birebir aynı olduğunu doğrular.
 * Beklentiler [KATALOG_BEKLENTI] içinde (Android dosyasından bağımsız ayrıştırıcıyla üretildi) ve aşağıda
 * Android kaynağı okunarak elle yazılan nokta kontrollerinde.
 */
class GlobalLessonDataKatalogTest {

    /** Betikteki `kanonik()` ile aynı alan sırası; null → "null". */
    private fun kanonik(it: LessonItem): String = listOf(
        it.stableId, it.type, it.title, it.titleUnit, it.offset, it.isCompleted, it.stepCount, it.currentStep,
        it.finishStepNumber, it.startStepNumber, it.tutorialNumber, it.tutorialIsFinish, it.stepIsFinish,
        it.lessonHint, it.color, it.abacusGuideNumber, it.cupPoint1, it.cupPoint2, it.worstCupTime,
        it.isBlinding, it.blindingMultiplication, it.timePeriod, it.raceBusyLevel, it.raceTitle,
    ).joinToString("|") { v -> v.toString() }

    /** Java String.hashCode ile aynı (UTF-16 kod birimleri üzerinden 31'lik polinom, Int taşmalı). */
    private fun javaHash(s: String): Int {
        var h = 0
        for (c in s) h = 31 * h + c.code
        return h
    }

    @Test
    fun partBasinaItemSayisiVeTurDagilimi() {
        for (b in KATALOG_BEKLENTI) {
            val items = GlobalLessonData.createLessonItems(b.part)
            assertEquals(b.adet, items.size, "part ${b.part} adet")
            val tur = listOf(
                items.count { it.type == LessonItem.TYPE_LESSON },
                items.count { it.type == LessonItem.TYPE_HEADER },
                items.count { it.type == LessonItem.TYPE_CHEST },
                items.count { it.type == LessonItem.TYPE_RACE },
            )
            assertEquals(b.tur, tur, "part ${b.part} tür dağılımı")
        }
        // Elle doğrulanan toplamlar (güncel Android kaynağında 169 adet `LessonItem(`):
        assertEquals(listOf(33, 20, 20, 25, 20, 20, 15, 15, 1), (1..9).map { GlobalLessonData.createLessonItems(it).size })
        assertEquals(169, (1..9).sumOf { GlobalLessonData.createLessonItems(it).size })
    }

    @Test
    fun tumAlanlarAndroidIleAyni_parmakIzi() {
        for (b in KATALOG_BEKLENTI) {
            val items = GlobalLessonData.createLessonItems(b.part)
            val satirlar = items.map { kanonik(it) }
            assertEquals(b.ilk, satirlar.first(), "part ${b.part} ilk item")
            assertEquals(b.son, satirlar.last(), "part ${b.part} son item")
            assertEquals(b.basliklarMetin, items.map { it.title }, "part ${b.part} başlık sırası")
            assertEquals(b.hash, javaHash(satirlar.joinToString("\n")), "part ${b.part} parmak izi")
        }
    }

    @Test
    fun toplamlarVeIndeksListeleri() {
        for (b in KATALOG_BEKLENTI) {
            val items = GlobalLessonData.createLessonItems(b.part)
            assertEquals(b.toplamStepCount, items.sumOf { it.stepCount }, "part ${b.part} stepCount toplamı")
            assertEquals(b.toplamFinish, items.sumOf { it.finishStepNumber ?: 0 }, "part ${b.part} finish toplamı")
            assertEquals(b.toplamStart, items.sumOf { it.startStepNumber ?: 0 }, "part ${b.part} start toplamı")
            assertEquals(b.bitmis, items.indices.filter { items[it].stepIsFinish }, "part ${b.part} bitmiş")
            assertEquals(b.sandiklar, items.indices.filter { items[it].type == LessonItem.TYPE_CHEST }, "part ${b.part} sandık")
            assertEquals(b.basliklar, items.indices.filter { items[it].type == LessonItem.TYPE_HEADER }, "part ${b.part} başlık")
            assertEquals(
                b.kilitli,
                items.indices.filter { !items[it].isCompleted && items[it].type != LessonItem.TYPE_HEADER },
                "part ${b.part} kilitli",
            )
            assertEquals(b.raceBusy, items.map { it.raceBusyLevel }, "part ${b.part} raceBusyLevel")
            assertEquals(
                b.renkler,
                items.filter { it.type == LessonItem.TYPE_HEADER }.map { it.color },
                "part ${b.part} başlık renkleri",
            )
        }
    }

    @Test
    fun mapFragmentIndexVePartIdHerZamanIndekstenAtanir() {
        // Android: return items.mapIndexed { index, item -> item.apply { mapFragmentIndex = index; this.partId = partId } }
        // (item'larda bildirilen mapFragmentIndex değerleri — ör. part 2'de 2,3,4 — ezilir)
        for (part in 1..9) {
            val items = GlobalLessonData.createLessonItems(part)
            items.forEachIndexed { index, item ->
                assertEquals(index, item.mapFragmentIndex, "part $part idx $index mapFragmentIndex")
                assertEquals(part, item.partId, "part $part idx $index partId")
            }
        }
        // Part 2'nin ilk dersi kaynakta `mapFragmentIndex = 2` bildirir ama listedeki yeri 1'dir.
        assertEquals(1, GlobalLessonData.createLessonItems(2)[1].mapFragmentIndex)
    }

    @Test
    fun bilinmeyenPartBosListe() {
        assertTrue(GlobalLessonData.createLessonItems(0).isEmpty())
        assertTrue(GlobalLessonData.createLessonItems(10).isEmpty())
        assertTrue(GlobalLessonData.createLessonItems(-1).isEmpty())
    }

    @Test
    fun herCagriYeniNesnelerUretir() {
        val a = GlobalLessonData.createLessonItems(1)
        val b = GlobalLessonData.createLessonItems(1)
        assertEquals(a, b)
        assertNotSame(a[1], b[1])
        a[1].stepIsFinish = true
        assertEquals(false, GlobalLessonData.createLessonItems(1)[1].stepIsFinish)
    }

    @Test
    fun stableIdHerItemdaDoluTekilVeKonumaGoreBicimli() {
        val hepsi = mutableListOf<String>()
        for (part in 1..9) {
            val items = GlobalLessonData.createLessonItems(part)
            val ids = items.map { it.stableId }
            assertTrue(ids.none { it.isBlank() }, "part $part boş stableId")
            assertEquals(ids.size, ids.toSet().size, "part $part yinelenen stableId")
            // Android'de kimlikler "p{part}_i{sıra:2 hane}_{ad}" biçiminde verilmiş (şu anki sürümde sıra = konum)
            ids.forEachIndexed { i, id ->
                assertTrue(id.startsWith("p${part}_i${i.toString().padStart(2, '0')}_"), "part $part idx $i: $id")
            }
            hepsi += ids
        }
        assertEquals(169, hepsi.size)
        assertEquals(169, hepsi.toSet().size, "part'lar arası da tekil")
        assertEquals("p1_i04_unite_maratonu", GlobalLessonData.createLessonItems(1)[4].stableId)
        assertEquals("p9_i00_kupa_modu", GlobalLessonData.createLessonItems(9)[0].stableId)
        assertEquals("p1_i04_unite_maratonu", GlobalLessonData.stableIdAt(1, 4))
        assertNull(GlobalLessonData.stableIdAt(1, 99))
        assertNull(GlobalLessonData.stableIdAt(42, 0))
    }

    @Test
    fun guncelAndroidKatalogu_yalnizcaIlkDersAcikVeBitisYok() {
        // Android (commit 06d4f05): 1-6'da yalnızca part 1'in ilk dersi açık; sandıklardaki test bitişleri silindi.
        for (part in 1..6) {
            val items = GlobalLessonData.createLessonItems(part)
            assertTrue(items.none { it.stepIsFinish }, "part $part: hiçbir item bitmiş gelmemeli")
            val acik = items.indices.filter { items[it].isCompleted && items[it].type != LessonItem.TYPE_HEADER }
            assertEquals(if (part == 1) listOf(1) else emptyList(), acik, "part $part açık dersler")
        }
        // Part 8'in ilk yarışı açık, 7'de hiçbiri açık değil (kilit raceBusyLevel ile)
        assertEquals(true, GlobalLessonData.createLessonItems(8)[0].isCompleted)
        assertTrue(GlobalLessonData.createLessonItems(7).none { it.isCompleted })
    }

    @Test
    fun varsayilanAlanlar() {
        // Katalogda ayarlanmayan alanlar Android varsayılanlarında kalır.
        for (part in 1..9) {
            for (item in GlobalLessonData.createLessonItems(part)) {
                assertNull(item.id)
                assertEquals(0, item.progressBarLevel)
                assertNull(item.fragment)
                assertNull(item.LESSON_ID)
                assertNull(item.isExtraction)
                assertNull(item.isMultiplication)
                assertEquals(List(item.stepCount) { false }, item.stepCompletionStatus)
                assertEquals(false, item.finalGoldVisualUnlocked)
                assertEquals("chest_stars_tier0", item.stepCupIcon)
                assertNull(item.sectionTitle)
                assertNull(item.backPart)
                assertNull(item.sectionDescription)
                assertNull(item.racePartId)
                assertNull(item.backRaceId)
                assertNull(item.cupDifficultyLevel)
                assertNull(item.cupWinDelta)
                assertNull(item.cupLossDelta)
                assertNull(item.record)
                assertNull(item.leaderboardSeasonBest)
                assertNull(item.leaderboardSeasonId)
                assertNull(item.cupDigitSize)
                assertNull(item.cupNumberCount)
            }
        }
    }

    // ---- Android kaynağı okunarak elle yazılan nokta kontrolleri ----

    @Test
    fun part1_ilkDersVeIlkSandik_elleKontrol() {
        val p1 = GlobalLessonData.createLessonItems(1)

        val baslik = p1[0]
        assertEquals(LessonItem.TYPE_HEADER, baslik.type)
        assertEquals("Sayıları Abaküste Tanıma", baslik.title)
        assertEquals(false, baslik.isCompleted)
        assertEquals(1, baslik.stepCount)
        assertEquals(1, baslik.currentStep)
        assertEquals("lesson_header_blue", baslik.color)

        val ders = p1[1]
        assertEquals(LessonItem.TYPE_LESSON, ders.type)
        assertEquals("Sayıları Abaküste Tanıma", ders.title)
        assertEquals(-30, ders.offset)
        assertEquals(true, ders.isCompleted)
        assertEquals(2, ders.stepCount)
        assertEquals(1, ders.currentStep)
        assertEquals(2, ders.finishStepNumber)
        assertEquals(1, ders.tutorialNumber)
        assertEquals(1, ders.startStepNumber)
        assertEquals(false, ders.tutorialIsFinish)
        assertEquals(
            "En sağdaki sütunu kullan. Aşağıda boncuklar birlik, yukarıdaki beşlik değere sahip.",
            ders.lessonHint,
        )
        assertEquals(listOf(false, false), ders.stepCompletionStatus)

        val sandik = p1[4]
        assertEquals(LessonItem.TYPE_CHEST, sandik.type)
        assertEquals("Ünite Maratonu", sandik.title)
        assertEquals("Sayıları Abaküste Tanıma", sandik.titleUnit)
        assertEquals(6, sandik.abacusGuideNumber)
        assertEquals(1005, sandik.finishStepNumber)
        assertEquals(1005, sandik.startStepNumber)
        assertEquals(true, sandik.tutorialIsFinish)
        assertEquals("Hatasız, en kısa sürede bitir.", sandik.lessonHint)
        assertEquals(1600, sandik.cupPoint1)
        assertEquals(1300, sandik.cupPoint2)
        assertEquals(150, sandik.worstCupTime)
        assertEquals(false, sandik.stepIsFinish)

        // "5'lik Toplama" sandığı: eskiden şablonda stepIsFinish = true ile geliyordu; güncel Android'de silindi.
        val sandik3 = p1[15]
        assertEquals("5'lik Toplama", sandik3.titleUnit)
        assertEquals(false, sandik3.stepIsFinish)
        assertEquals(19, sandik3.startStepNumber)
        assertEquals(1400, sandik3.cupPoint1)
        assertEquals(1200, sandik3.cupPoint2)
        assertEquals(180, sandik3.worstCupTime)

        // Metin artık strings_lessons.xml'den (kaçış çözülmüş): düz kesme işareti (eski kaynakta ters eğik çizgi de vardı).
        assertEquals(
            "10 gelir adımını uygularken 5'lik kuralı kullanman gerekebilir.",
            p1[31].lessonHint,
        )
        assertEquals("Boncuk Kuralı - 3, 4 Basamaklı", p1[31].title)
    }

    @Test
    fun part2ve3_elleKontrol() {
        val p2 = GlobalLessonData.createLessonItems(2)
        assertEquals("Kuralsız Çıkarma - Temeli", p2[1].title)
        assertEquals(66, p2[1].startStepNumber)
        assertEquals(68, p2[1].finishStepNumber)
        assertEquals(10, p2[1].tutorialNumber)
        assertEquals(3, p2[1].stepCount)
        // İlk sandık: offset 60, stepIsFinish false (test bitişi silindi)
        assertEquals(LessonItem.TYPE_CHEST, p2[3].type)
        assertEquals(60, p2[3].offset)
        assertEquals(false, p2[3].stepIsFinish)
        assertEquals(74, p2[3].startStepNumber)
        assertEquals(1400, p2[3].cupPoint1)
        assertEquals("lesson_header_green", p2[4].color)
        assertEquals(1100, p2[7].tutorialNumber)

        val p3 = GlobalLessonData.createLessonItems(3)
        assertEquals("2'ye 1 Çarpma - Kuralsız", p3[1].title)
        assertEquals(118, p3[1].startStepNumber)
        assertEquals(120, p3[1].finishStepNumber)
        assertEquals(18, p3[1].tutorialNumber)
        assertEquals(4, p3[1].abacusGuideNumber)
    }

    @Test
    fun part4_korDersler_elleKontrol() {
        val p4 = GlobalLessonData.createLessonItems(4)
        assertEquals("lesson_header_green", p4[0].color)
        val d1 = p4[1]
        assertEquals("Kuralsız Toplama - 3 Saniye Arayla", d1.title)
        assertEquals(30, d1.offset)
        assertEquals(true, d1.isBlinding)
        assertEquals(24, d1.tutorialNumber)
        assertEquals(3000L, d1.timePeriod)
        assertEquals(1, d1.startStepNumber)
        assertEquals(3, d1.finishStepNumber)
        val d2 = p4[2]
        assertEquals("Kuralsız Toplama - 2,5 Saniye Arayla", d2.title)
        assertEquals(2500L, d2.timePeriod)
        assertEquals(true, d2.tutorialIsFinish)
        // Part 4 sandıklarında cupPoint tanımlı değil (liderlik/rekor yok).
        assertTrue(p4.filter { it.type == LessonItem.TYPE_CHEST }.all { it.cupPoint1 == null && it.cupPoint2 == null })
    }

    @Test
    fun part7ve8ve9_yarislar_elleKontrol() {
        val p7 = GlobalLessonData.createLessonItems(7)
        assertEquals("Acemi Çırak", p7[0].title)
        assertEquals(1, p7[0].raceBusyLevel)
        assertEquals(3000L, p7[0].timePeriod)
        assertEquals("1 basamaklı 4 adet sayı", p7[0].raceTitle)
        assertEquals(78, p7[0].startStepNumber)
        assertEquals("Başlangıç Ustası", p7[1].title)
        assertEquals(2, p7[1].raceBusyLevel)
        assertEquals(2000L, p7[1].timePeriod)
        // Part 7: ilk yarış BAŞLA (1), diğerleri KİLİTLİ (2)
        assertEquals(listOf(1) + List(14) { 2 }, p7.map { it.raceBusyLevel })
        assertTrue(p7.all { it.type == LessonItem.TYPE_LESSON && it.stepCount == 1 && it.tutorialIsFinish })

        val p8 = GlobalLessonData.createLessonItems(8)
        // Part 8: Android şablonunda TÜM yarışlar BAŞLA (1) durumunda.
        assertEquals(List(15) { 1 }, p8.map { it.raceBusyLevel })
        assertEquals(true, p8[0].isCompleted)
        assertEquals("Efsanevi Bilge", p8[14].title)
        assertEquals(1011, p8[14].startStepNumber)
        assertEquals(2500L, p8[14].timePeriod)

        val p9 = GlobalLessonData.createLessonItems(9)
        assertEquals(1, p9.size)
        assertEquals("Kupa Modu", p9[0].title)
        assertEquals("Kupa Zorluğu", p9[0].raceTitle)
        assertEquals(1, p9[0].raceBusyLevel)
        assertNull(p9[0].startStepNumber)
        assertNull(p9[0].timePeriod)
    }

    @Test
    fun timePeriodDegerleriKatalogda() {
        val hepsi = (1..9).flatMap { p -> GlobalLessonData.createLessonItems(p).mapNotNull { it.timePeriod } }.toSet()
        assertEquals(setOf(750L, 800L, 1000L, 2000L, 2500L, 3000L, 4000L), hepsi)
    }
}

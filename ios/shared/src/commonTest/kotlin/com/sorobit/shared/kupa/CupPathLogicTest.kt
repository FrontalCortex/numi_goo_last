// CupPathLogic (TasksFragment kupa yolu / zorluk paneli kuralları) ve KupaEkranKoprusu (iOS ekran katmanı) testleri.
package com.sorobit.shared.kupa

import com.sorobit.shared.kupa.rozet.BadgeProgressRepository
import com.sorobit.shared.kupa.rozet.UserBadgeProgress
import com.sorobit.shared.kupa.sezon.SeasonClock
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CupPathLogicTest {

    @AfterTest
    fun temizle() {
        BadgeProgressRepository.update(UserBadgeProgress())
    }

    // ---------------------------------------------------------------- kartlar

    @Test
    fun altiKartAndroidSirasiyla() {
        assertEquals(6, CupPathLogic.CARDS.size)
        assertEquals((1..6).toList(), CupPathLogic.CARDS.map { it.cardNumber })
        assertEquals(
            listOf(
                "Toplama Kupa Yolu", "Çıkarma Kupa Yolu", "Çarpma Kupa Yolu",
                "Toplama Kupa Yolu - Körleme", "Çıkarma Kupa Yolu - Körleme", "Çarpma Kupa Yolu - Körleme",
            ),
            CupPathLogic.CARDS.map { it.title },
        )
        assertEquals(
            listOf(
                "dinosaur_anim.json", "crocodile_anim.json", "goat_anim.json",
                "eagle_anim.json", "fly_anim.json", "turtle_anim.json",
            ),
            CupPathLogic.CARDS.map { it.animFile },
        )
        assertEquals(
            listOf(
                "addition_abacus_cup", "extraction_abacus_cup", "impact_abacus_cup",
                "blinding_addition_abacus_cup", "blinding_extraction_abacus_cup", "blinding_impact_abacus_cup",
            ),
            CupPathLogic.CARDS.map { it.field },
        )
        // setupCupPathCard: 1,4 turuncu; 2,5 yeşil; 3,6 mavi
        assertEquals(
            listOf(
                "lesson_header_orange", "lesson_header_green", "lesson_header_blue",
                "lesson_header_orange", "lesson_header_green", "lesson_header_blue",
            ),
            CupPathLogic.CARDS.map { it.activeColorRes },
        )
        // Kart N, part N'in son sandığıyla açılır
        assertEquals((1..6).toList(), CupPathLogic.CARDS.map { it.unlockPartId })
        // Profilde körleme kartlarında yasak simgesi
        assertEquals(listOf(false, false, false, true, true, true), CupPathLogic.CARDS.map { it.showForbiddenIcon })
    }

    @Test
    fun kartTuruDersBayraklariylaTutarli() {
        for (c in CupPathLogic.CARDS) {
            val kind = BadgePrecalcHelper.resolveCupPathKind(c.isMultiplication, c.isExtraction, c.isBlinding)
            assertEquals(kind, c.kind, "kart ${c.cardNumber}")
            assertEquals(c.field, c.kind.repository.FIELD, "kart ${c.cardNumber}")
        }
    }

    @Test
    fun kartNumarasiKirpilir() {
        assertEquals(1, CupPathLogic.card(0).cardNumber)
        assertEquals(1, CupPathLogic.card(-5).cardNumber)
        assertEquals(3, CupPathLogic.card(3).cardNumber)
        assertEquals(6, CupPathLogic.card(99).cardNumber)
    }

    @Test
    fun kartSonSandikBitinceAcilir() {
        val sandik = CupPathLogic.LESSON_ITEM_TYPE_CHEST
        // Sandık yok
        assertFalse(CupPathLogic.isCardActive(listOf(0, 1, 1), listOf(true, true, true)))
        assertFalse(CupPathLogic.isCardActive(emptyList(), emptyList()))
        // Yalnızca SON sandık belirleyicidir
        assertFalse(CupPathLogic.isCardActive(listOf(1, sandik, 1, sandik), listOf(true, true, true, false)))
        assertTrue(CupPathLogic.isCardActive(listOf(1, sandik, 1, sandik), listOf(false, false, false, true)))
        // Bayrak listesi kısa gelirse kapalı sayılır
        assertFalse(CupPathLogic.isCardActive(listOf(1, sandik), listOf(true)))
    }

    @Test
    fun gosterilenSkorSifirinAltinaInmez() {
        assertEquals(0, CupPathLogic.shownScore(-15))
        assertEquals(200, CupPathLogic.shownScore(200))
    }

    // ---------------------------------------------------------------- zorluk paneli

    @Test
    fun zorlukPaneliMetinleri() {
        assertEquals("Zorluk Seviyesi", CupPathLogic.difficultyTitle(false))
        assertEquals("Çarpma Kupa Yolu", CupPathLogic.difficultyTitle(true))
        assertTrue(CupPathLogic.showsDifficultySlider(false))
        assertFalse(CupPathLogic.showsDifficultySlider(true))
        assertEquals(listOf(0, 25, 50, 75, 100), (0..CupPathLogic.DIFFICULTY_SEEKBAR_MAX).map { CupPathLogic.difficultyPercent(it) })
        assertEquals("3 Basamak", CupPathLogic.digitOptionLabel(3))
    }

    @Test
    fun basamakSecimiYalnizca3000UstuVeCarpmaDisinda() {
        assertTrue(CupPathLogic.digitOptions(2999, isBlinding = false, isMultiplication = false).isEmpty())
        assertTrue(CupPathLogic.digitOptions(5000, isBlinding = false, isMultiplication = true).isEmpty())
        assertEquals(
            CupRuleEngine.getAvailableDigits(3000, false),
            CupPathLogic.digitOptions(3000, isBlinding = false, isMultiplication = false),
        )
        assertEquals(listOf(1, 2, 3, 4), CupPathLogic.digitOptions(3500, isBlinding = true, isMultiplication = false))
        assertEquals(listOf(1, 2, 3, 4), CupPathLogic.digitOptions(3500, isBlinding = false, isMultiplication = false))
    }

    @Test
    fun enerjiKontrolu() {
        assertTrue(CupPathLogic.canStartLesson(isInfiniteEnergy = true, currentEnergy = 0))
        assertTrue(CupPathLogic.canStartLesson(isInfiniteEnergy = false, currentEnergy = 1))
        assertFalse(CupPathLogic.canStartLesson(isInfiniteEnergy = false, currentEnergy = 0))
    }

    // ---------------------------------------------------------------- ders başlatma

    @Test
    fun toplamaDersiMotorlaAyniAlanlariTasir() {
        val beklenen = CupRuleEngine.buildLessonItem(250, 2, isBlinding = false, isExtraction = false, random = Random(7))
        val d = CupPathLogic.buildCupLesson(1, 250, 2, 0, Random(7))
        assertEquals(1, d.cardNumber)
        assertEquals(250, d.cupScore)
        assertEquals(beklenen.timePeriod, d.timePeriod)
        assertEquals(beklenen.cupDigitSize, d.cupDigitSize)
        assertEquals(beklenen.cupNumberCount, d.cupNumberCount)
        assertEquals(2, d.cupDifficultyLevel)
        assertEquals(24, d.cupWinDelta)
        assertEquals(20, d.cupLossDelta)
        assertFalse(d.isBlinding)
        assertFalse(d.isExtraction)
        assertFalse(d.isMultiplication)
        assertEquals(d.cupNumberCount, d.numbers.size)
        assertTrue(d.numbers.all { it > 0 })
        assertEquals(0, d.multiplicationFirst)
        assertEquals(0, d.multiplicationSecond)
    }

    @Test
    fun sayilarBasamakSayisinaUyar() {
        repeat(20) { tur ->
            val d = CupPathLogic.buildCupLesson(4, 1500, tur % 5, 0, Random(tur))
            assertTrue(d.isBlinding)
            val enKucuk = if (d.cupDigitSize == 1) 1 else 10
            val enBuyuk = if (d.cupDigitSize == 1) 9 else 99
            assertTrue(d.numbers.all { it in enKucuk..enBuyuk }, "basamak=${d.cupDigitSize} sayılar=${d.numbers}")
        }
    }

    @Test
    fun cikarmaDersindeToplamNegatifeDusmez() {
        repeat(30) { tur ->
            val d = CupPathLogic.buildCupLesson(if (tur % 2 == 0) 2 else 5, 900 + tur * 40, tur % 5, 0, Random(tur))
            assertTrue(d.isExtraction)
            assertTrue(d.numbers.first() > 0)
            var toplam = 0
            for (n in d.numbers) {
                toplam += n
                assertTrue(toplam >= 0, "sayılar=${d.numbers}")
            }
        }
    }

    @Test
    fun carpmaDersiSabitAlanlariEzer() {
        for (kart in listOf(3, 6)) {
            val d = CupPathLogic.buildCupLesson(kart, 450, 4, 0, Random(3))
            assertTrue(d.isMultiplication)
            assertEquals(kart == 6, d.isBlinding)
            assertEquals(1, d.cupNumberCount)
            assertEquals(30, d.cupWinDelta)
            assertEquals(20, d.cupLossDelta)
            // Çarpmada zorluk kaydırıcısı yok: seviye 0
            assertEquals(0, d.cupDifficultyLevel)
            assertTrue(d.numbers.isEmpty())
            // 400..499: iki basamak x bir basamak, rakamlar 4..7
            assertTrue(d.multiplicationFirst in 44..77, "ilk=${d.multiplicationFirst}")
            assertTrue(d.multiplicationSecond in 4..7, "ikinci=${d.multiplicationSecond}")
        }
    }

    @Test
    fun zorlaBasamakSecimiUygulanir() {
        repeat(10) { tur ->
            val d = CupPathLogic.buildCupLesson(1, 3500, 0, 3, Random(tur))
            assertEquals(3, d.cupDigitSize)
        }
    }

    @Test
    fun negatifSkorIstisnaAtmaz() {
        for (kart in 1..6) {
            val d = CupPathLogic.buildCupLesson(kart, -40, 0, 0)
            assertEquals(-40, d.cupScore)
            assertTrue(d.cupNumberCount >= 1)
        }
    }

    @Test
    fun cikarmaDonusumuAndroidOrnegi() {
        // 5, sonra 3 (5 > 3 → -3, toplam 2), sonra 7 (2 > 7 değil → +7, toplam 9), sonra 9 (9 > 9 değil → +9)
        assertEquals(listOf(5, -3, 7, 9), CupPathLogic.applyExtractionNegation(listOf(5, 3, 7, 9)))
        assertEquals(emptyList(), CupPathLogic.applyExtractionNegation(emptyList()))
        assertEquals(listOf(4), CupPathLogic.applyExtractionNegation(listOf(4)))
    }

    // ---------------------------------------------------------------- ders dönüşü

    @Test
    fun skorAnimasyonuDegerleri() {
        assertEquals(224, CupPathLogic.scoreAfterLesson(200, 24))
        assertEquals(-10, CupPathLogic.scoreAfterLesson(10, -20)) // Android burada kırpmaz
        assertEquals(200, CupPathLogic.scoreAnimationStart(224, 24))
        assertEquals(0, CupPathLogic.scoreAnimationStart(-10, 20))
        assertEquals(0, CupPathLogic.scoreAnimationEnd(-10))
        assertTrue(CupPathLogic.shouldAnimateScore(-5))
        assertFalse(CupPathLogic.shouldAnimateScore(0))
        assertEquals("+24", CupPathLogic.deltaText(24))
        assertEquals("-20", CupPathLogic.deltaText(-20))
        assertEquals("0", CupPathLogic.deltaText(0))
        assertEquals(0x4CAF50, CupPathLogic.deltaColorRgb(5))
        assertEquals(0xF44336, CupPathLogic.deltaColorRgb(-5))
        assertEquals(0xF44336, CupPathLogic.deltaColorRgb(0))
    }

    // ---------------------------------------------------------------- köprü: kupa geçmişi

    @Test
    fun gecmisSekmeleri() {
        assertEquals(listOf("Haftalık", "Aylık", "Yıllık", "Tüm Zamanlar"), KupaEkranKoprusu.gecmisSekmeBasliklari())
        assertEquals(listOf(7, 30, 365, 0), (0..3).map { KupaEkranKoprusu.gecmisPencereGunu(it) })
        assertEquals("", KupaEkranKoprusu.gecmisBaslangicKimligi(3))
        assertTrue(KupaEkranKoprusu.gecmisBaslangicKimligi(0).matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        // Sekme indeksi kırpılır
        assertEquals(0, KupaEkranKoprusu.gecmisPencereGunu(42))
        assertEquals(7, KupaEkranKoprusu.gecmisPencereGunu(-1))
        assertEquals(200, KupaEkranKoprusu.gecmisVarsayilanSkor())
    }

    @Test
    fun gecmisGrafigiBosTekVeCokNokta() {
        val bos = KupaEkranKoprusu.gecmisGrafigi(0, emptyList())
        assertTrue(bos.isEmpty)
        assertTrue(bos.points.isEmpty())

        val tek = KupaEkranKoprusu.gecmisGrafigi(0, listOf(KupaGecmisNoktasi("2026-11-05", 215)))
        assertFalse(tek.isEmpty)
        assertTrue(tek.isSinglePoint)
        assertEquals(listOf(KupaGrafikNoktasi("5 Kas", 215)), tek.points)
        assertEquals(190, tek.axisMin)
        assertEquals(240, tek.axisMax)
        assertEquals(1, tek.xAxisLabelCount)

        val yillik = KupaEkranKoprusu.gecmisGrafigi(
            2,
            listOf(
                KupaGecmisNoktasi("2026-10-03", 200),
                KupaGecmisNoktasi("2026-10-28", 260),
                KupaGecmisNoktasi("2026-11-02", 300),
            ),
        )
        // Yıllık sekme aya göre gruplar ve ayın SON değerini tutar
        assertEquals(listOf(KupaGrafikNoktasi("Eki 26", 260), KupaGrafikNoktasi("Kas 26", 300)), yillik.points)
        assertFalse(yillik.isSinglePoint)
        assertEquals(2, yillik.xAxisLabelCount)
        val (altSinir, ustSinir) = CupHistoryLogic.niceAxisRange(listOf(260, 300))
        assertEquals(altSinir, yillik.axisMin)
        assertEquals(ustSinir, yillik.axisMax)
    }

    // ---------------------------------------------------------------- köprü: sezon ve liderlik

    @Test
    fun sezonVerisiSaatleTutarli() {
        val an = SeasonClock.SEASON_ANCHOR_UTC_MS + 3 * SeasonClock.SEASON_DURATION_MS + 60_000L
        val v = KupaEkranKoprusu.sezonVerisi(-1, an)
        assertEquals(4, v.season)
        assertEquals("s4", v.seasonLabel)
        assertEquals(SeasonClock.SEASON_DURATION_MS - 60_000L, v.remainingMs)
        assertEquals("6 gün 23 sa", v.countdownText)
        // Bağlı sezon verilirse etiket onu gösterir
        assertEquals("s2", KupaEkranKoprusu.sezonVerisi(2, an).seasonLabel)
        assertEquals("part_1_lesson_p1_i04_unite_maratonu_season_4", KupaEkranKoprusu.liderlikTahtaKimligi(1, "p1_i04_unite_maratonu", 4))
        assertEquals(100, KupaEkranKoprusu.liderlikSorguLimiti())
        assertEquals(1000L, KupaEkranKoprusu.sezonSayacAraligiMs())
    }

    @Test
    fun liderSatirlariSiraRenkVeBenimSatirim() {
        val satirlar = KupaEkranKoprusu.liderSatirlari(
            listOf(
                LiderHamKayit("a", 900, "Ada", "0:41", "avatar-a"),
                LiderHamKayit("b", 700, "", "0:55", ""),
                LiderHamKayit("c", 700, "Can", "0:55", ""),
                LiderHamKayit("d", 100, "Deniz", "2:10", ""),
            ),
            currentUid = "c",
        )
        assertEquals(listOf(1, 2, 3, 4), satirlar.map { it.displayRank })
        // Aynı puan ödül sırasını paylaşır
        assertEquals(listOf(1, 2, 2, 4), satirlar.map { it.rewardRank })
        assertEquals(
            listOf(
                "bg_record_rank_gold", "bg_record_rank_badge_purple",
                "bg_record_rank_badge_purple", "bg_record_rank_badge_blue",
            ),
            satirlar.map { it.rankBadgeDrawable },
        )
        assertEquals("Liderlik tablosu", satirlar[1].displayName)
        assertTrue(satirlar[0].hasAvatar)
        assertFalse(satirlar[1].hasAvatar)
        assertEquals(listOf(false, false, true, false), satirlar.map { it.isCurrentUser })
        assertEquals("white", satirlar[2].cardColorRes)
        assertEquals("black", satirlar[2].nameColorRes)
        assertEquals("gray", satirlar[2].timeColorRes)
        assertEquals("panel_background", satirlar[0].cardColorRes)
        assertEquals("button_disabled", satirlar[0].timeColorRes)
        // Oturum yoksa hiçbir satır "benim" değildir
        assertTrue(KupaEkranKoprusu.liderSatirlari(listOf(LiderHamKayit("", 5, "x", "y", "")), "").none { it.isCurrentUser })
    }

    @Test
    fun liderlikHataMetni() {
        assertEquals("Liste yüklenemedi.", KupaEkranKoprusu.liderlikHataMetni("ağ yok", yetkiHatasi = true))
        assertEquals("ağ yok", KupaEkranKoprusu.liderlikHataMetni("ağ yok", yetkiHatasi = false))
        assertEquals("Liste yüklenemedi.", KupaEkranKoprusu.liderlikHataMetni("", yetkiHatasi = false))
    }

    // ---------------------------------------------------------------- köprü: rozetler

    private fun ornekDokuman(): Map<String, Any?> = mapOf(
        "userDartProgress" to 12L,
        "userDinoProgress" to 640L,
        "userKarateProgress" to 2L,
        "goldMedalPiece" to listOf(listOf("Ünite 1", 3L), listOf("Ünite 2", 5L)),
        "cupPiece" to listOf(listOf("Ünite 1", 7L, 4L)),
    )

    @Test
    fun rozetListeleriDepodanGelir() {
        KupaEkranKoprusu.rozetDurumunuYukle(ornekDokuman())
        val rekorlar = KupaEkranKoprusu.kisiselRekorlar()
        assertEquals(5, rekorlar.size)
        // Açık olanlar önce
        assertEquals(rekorlar.map { it.unlocked }, rekorlar.map { it.unlocked }.sortedDescending())
        val altin = rekorlar.first { it.modeName == "GOLD_MEDAL" }
        assertTrue(altin.unlocked)
        assertEquals("2x", altin.subtext)
        assertEquals("", altin.baseAsset)
        assertEquals("gold_medal_anim.json", altin.topAsset)

        val rozetler = KupaEkranKoprusu.rozetler()
        assertEquals(13, rozetler.size)
        assertEquals(3, KupaEkranKoprusu.rozetSatirBoyu())
        val dino = rozetler.first { it.modeName == "DINO" }
        assertTrue(dino.unlocked)
        assertEquals("Dino", dino.title)
        assertEquals("1/5", dino.subtext)
        assertEquals("BRONZE", dino.toneName)
        assertTrue(dino.withCircleFrame)
        val timo = rozetler.first { it.modeName == "CROCODILE" }
        assertFalse(timo.unlocked)
        assertEquals("", timo.toneName)
    }

    @Test
    fun rozetEkraniAcikIlerlemeliRozet() {
        KupaEkranKoprusu.rozetDurumunuYukle(ornekDokuman())
        val e = KupaEkranKoprusu.rozetEkrani("DINO")
        assertEquals("DINO", e.modeName)
        assertTrue(e.unlocked)
        assertTrue(e.replayOnTap)
        assertEquals("Toplama Kupa Yolunda 640 kupa kazanarak Dino rozetini kazandın.", e.detailText)
        assertTrue(e.showProgress)
        assertEquals("640/1000", e.progressText)
        assertEquals(64, e.progressPercent)
        assertEquals("BRONZE", e.toneName)
        assertEquals("daily_tasks_complite_badge2.json", e.baseAsset)
        assertEquals("dinosaur_anim.json", e.topAsset)
        assertEquals("", e.pieceCountText)
        assertTrue(e.pieceRows.isEmpty())
        assertTrue(e.showRewardAmount)
    }

    @Test
    fun rozetEkraniKilitliRozet() {
        KupaEkranKoprusu.rozetDurumunuYukle(null)
        val e = KupaEkranKoprusu.rozetEkrani("CROCODILE")
        assertFalse(e.unlocked)
        assertFalse(e.replayOnTap)
        assertEquals("Bu rozeti kazanmak için Çıkarma Kupa Yolunda 500 kupa kazan.", e.detailText)
        assertEquals("0/500", e.progressText)
        assertEquals(0, e.progressPercent)
        assertEquals("", e.toneName)

        // Kilitli siyah kuşakta sayı 1 gösterilir, ilerleme çubuğu yoktur
        val karate = KupaEkranKoprusu.rozetEkrani("KARATE")
        assertFalse(karate.unlocked)
        assertEquals("1", karate.rewardAmountText)
        assertFalse(karate.showProgress)
        assertEquals("", karate.baseAsset)
    }

    @Test
    fun rozetEkraniMadalyaVeKupaParcalari() {
        KupaEkranKoprusu.rozetDurumunuYukle(ornekDokuman())
        val altin = KupaEkranKoprusu.rozetEkrani("GOLD_MEDAL")
        assertTrue(altin.unlocked)
        assertEquals("2x", altin.pieceCountText)
        assertFalse(altin.pieceIsCup)
        assertEquals(
            listOf(RozetParcaVerisi("Ünite 1", "", "3"), RozetParcaVerisi("Ünite 2", "", "5")),
            altin.pieceRows,
        )
        assertEquals("gold_medal_anim.json", altin.pieceAsset)
        assertFalse(altin.showProgress)

        val kupa = KupaEkranKoprusu.rozetEkrani("CUP_GOOGLE")
        assertEquals("1x", kupa.pieceCountText)
        assertTrue(kupa.pieceIsCup)
        assertEquals(listOf(RozetParcaVerisi("Ünite 1", "7", "4")), kupa.pieceRows)
        assertEquals("cup_google_anim.json", kupa.pieceAsset)

        // Madalyası olmayan kullanıcıda düğme gizli
        val gumus = KupaEkranKoprusu.rozetEkrani("SILVER_MEDAL")
        assertEquals("", gumus.pieceCountText)
        assertTrue(gumus.pieceRows.isEmpty())
    }

    @Test
    fun bilinmeyenModDartSayilir() {
        KupaEkranKoprusu.rozetDurumunuYukle(null)
        assertEquals("DAILY_ONLY", KupaEkranKoprusu.rozetEkrani("BOYLE_BIR_MOD_YOK").modeName)
    }
}

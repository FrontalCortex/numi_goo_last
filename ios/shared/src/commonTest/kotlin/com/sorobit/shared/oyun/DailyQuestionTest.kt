// Günlük soru parçalarının (DailyQuestionPeriod / Challenge / PoolBuilder / Repository / depolar /
// kart kuralları) Android davranışıyla aynı sonucu verdiğini doğrular. Beklenen değerler Android
// kaynak kodundan elle türetilmiştir.
package com.sorobit.shared.oyun

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Hangi üretecin hangi parametreyle çağrıldığını kaydeden sahte üreteç. */
private class KayitliUretec : DailyQuestionGenerators {
    val cagrilar = mutableListOf<String>()
    var bosDizi = false

    private fun dizi(ad: String): List<Int> {
        cagrilar.add(ad)
        return if (bosDizi) emptyList() else listOf(1, -2, 3)
    }

    private fun islem(ad: String): MathOperation {
        cagrilar.add(ad)
        return MathOperation(12, "x", 3)
    }

    override fun generateSequence1Digits(count: Int, digitsOne: Int) = dizi("generateSequence1Digits($count,$digitsOne)")
    override fun generateRelatedNumbers2Blinding(count: Int, digitsOne: Int) = dizi("generateRelatedNumbers2Blinding($count,$digitsOne)")
    override fun generateSequence10RulesEasyNew(count: Int) = dizi("generateSequence10RulesEasyNew($count)")
    override fun generateSequence10RulesEasyOld(count: Int) = dizi("generateSequence10RulesEasyOld($count)")
    override fun generateSequenceBeadRules(count: Int) = dizi("generateSequenceBeadRules($count)")
    override fun generateSequenceExtraction(count: Int) = dizi("generateSequenceExtraction($count)")
    override fun generateSequenceExtractionThreeDigits(count: Int) = dizi("generateSequenceExtractionThreeDigits($count)")
    override fun generateSequenceExtractionFiveRules(count: Int) = dizi("generateSequenceExtractionFiveRules($count)")
    override fun generateSequenceExtractionFiveRulesThreeDigits(count: Int) = dizi("generateSequenceExtractionFiveRulesThreeDigits($count)")
    override fun generateSequenceExtractionTenRules(count: Int) = dizi("generateSequenceExtractionTenRules($count)")
    override fun generateSequenceExtractionTenRulesThreeDigits(count: Int) = dizi("generateSequenceExtractionTenRulesThreeDigits($count)")
    override fun generateSequenceExtractionBeadRules(count: Int) = dizi("generateSequenceExtractionBeadRules($count)")
    override fun generateSequenceExtractionBeadRulesThreeDigits(count: Int) = dizi("generateSequenceExtractionBeadRulesThreeDigits($count)")
    override fun multiplicationLessFive() = islem("multiplicationLessFive()")
    override fun multiplicationLessFiveFull() = islem("multiplicationLessFiveFull()")
    override fun multiplicationFull() = islem("multiplicationFull()")
    override fun multiplicationTwo() = islem("multiplicationTwo()")
    override fun multiplicationTwoFull() = islem("multiplicationTwoFull()")
    override fun multiplicationThreeFull() = islem("multiplicationThreeFull()")
    override fun multiplicationThreeTwoFive() = islem("multiplicationThreeTwoFive()")
    override fun multiplicationThreeTwoFull() = islem("multiplicationThreeTwoFull()")
}

private fun sandik(
    bitti: Boolean = true,
    record: Int? = null,
    cup1: Int? = 2000,
    cup2: Int? = 1000,
    title: String = "Baslik",
    titleUnit: String? = "Unite",
) = OyunLessonItemData(
    type = OyunLessonItem.TYPE_CHEST,
    title = title,
    titleUnit = titleUnit,
    stepIsFinish = bitti,
    record = record,
    cupPoint1 = cup1,
    cupPoint2 = cup2,
)

private fun ders() = OyunLessonItemData(type = OyunLessonItem.TYPE_LESSON, title = "Ders", stepIsFinish = true)

private fun slot(i: Int, zorluk: String = "Kolay", unit: String = "U$i") = DailyQuestionSlot(
    sequence = listOf(i, i + 1),
    partId = 1,
    itemIndex = i,
    titleUnit = unit,
    difficulty = zorluk,
)

private fun meydanOkuma(
    cozulen: Int = 0,
    odul: Boolean = false,
    bekleyen: Int? = null,
    periodKey: String = "period_100",
) = DailyQuestionChallenge(
    periodKey = periodKey,
    solvedCount = cozulen,
    questions = listOf(slot(0, "Kolay"), slot(1, "Orta"), slot(2, "Zor")),
    rewardClaimed = odul,
    pendingContinueSlotIndex = bekleyen,
)

class DailyQuestionPeriodTest {
    private val gun = 86_400_000L

    @Test
    fun sabitler() {
        assertEquals("daily_question_prefs", DailyQuestionPeriod.PREFS_NAME)
        assertEquals("dailyQuestion", DailyQuestionPeriod.FIRESTORE_COLLECTION)
        assertEquals(86_400_000L, DailyQuestionPeriod.PERIOD_MS)
        assertEquals(3, DailyQuestionPeriod.QUESTIONS_PER_PERIOD)
        assertEquals(1, DailyQuestionPeriod.KEY_CONTINUE_COST)
        assertEquals(116, DailyQuestionPeriod.BROKEN_HEART_HOLD_FRAME)
        assertTrue(DailyQuestionPeriod.USE_STABLE_SEQUENCE)
    }

    @Test
    fun periyotAnahtariUtcGunKovasidir() {
        assertEquals("period_0", DailyQuestionPeriod.currentPeriodKey(0L))
        assertEquals("period_0", DailyQuestionPeriod.currentPeriodKey(gun - 1))
        assertEquals("period_1", DailyQuestionPeriod.currentPeriodKey(gun))
        // 2026-06-01T00:00:00Z = 20605. gün
        assertEquals("period_20605", DailyQuestionPeriod.currentPeriodKey(1_780_272_000_000L))
        assertEquals("period_20605", DailyQuestionPeriod.currentPeriodKey(1_780_272_000_000L + gun - 1))
        assertEquals("period_42", DailyQuestionPeriod.periodKeyForBucket(42))
    }

    @Test
    fun kovaAyristirma() {
        assertEquals(42L, DailyQuestionPeriod.periodBucketFromKey("period_42"))
        assertNull(DailyQuestionPeriod.periodBucketFromKey("42"))
        assertNull(DailyQuestionPeriod.periodBucketFromKey("period_x"))
        assertNull(DailyQuestionPeriod.periodBucketFromKey("period_"))
        assertNull(DailyQuestionPeriod.periodBucketFromKey(""))
    }

    @Test
    fun seriKirilmasi() {
        // Boş son anahtar: seri hiç başlamamış → kırık değil
        assertFalse(DailyQuestionPeriod.isFishingStreakBroken("", "period_10"))
        // Aynı ve bir sonraki periyot: kırık değil
        assertFalse(DailyQuestionPeriod.isFishingStreakBroken("period_10", "period_10"))
        assertFalse(DailyQuestionPeriod.isFishingStreakBroken("period_10", "period_11"))
        // Arada en az bir boş periyot: kırık
        assertTrue(DailyQuestionPeriod.isFishingStreakBroken("period_10", "period_12"))
        // Ayrıştırılamayan anahtarlar: kırık
        assertTrue(DailyQuestionPeriod.isFishingStreakBroken("bozuk", "period_12"))
        assertTrue(DailyQuestionPeriod.isFishingStreakBroken("period_10", "bozuk"))
        // Geriye giden saat: kırık değil
        assertFalse(DailyQuestionPeriod.isFishingStreakBroken("period_10", "period_5"))
    }

    @Test
    fun ayniVeArdisikPeriyot() {
        assertTrue(DailyQuestionPeriod.isSamePeriod("period_1", "period_1"))
        assertFalse(DailyQuestionPeriod.isSamePeriod("", ""))
        assertFalse(DailyQuestionPeriod.isSamePeriod("period_1", "period_2"))

        assertTrue(DailyQuestionPeriod.isConsecutivePeriodAfter("period_1", "period_2"))
        assertFalse(DailyQuestionPeriod.isConsecutivePeriodAfter("period_1", "period_1"))
        assertFalse(DailyQuestionPeriod.isConsecutivePeriodAfter("period_1", "period_3"))
        assertFalse(DailyQuestionPeriod.isConsecutivePeriodAfter("", "period_3"))
        assertFalse(DailyQuestionPeriod.isConsecutivePeriodAfter("bozuk", "period_3"))
        assertFalse(DailyQuestionPeriod.isConsecutivePeriodAfter("period_2", "bozuk"))
    }

    @Test
    fun periyotSonunaKalanSure() {
        assertEquals(gun, DailyQuestionPeriod.millisUntilCurrentPeriodEnds(0L))
        assertEquals(1L, DailyQuestionPeriod.millisUntilCurrentPeriodEnds(gun - 1))
        assertEquals(gun, DailyQuestionPeriod.millisUntilCurrentPeriodEnds(gun))
        assertEquals(gun - 3_600_000L, DailyQuestionPeriod.millisUntilCurrentPeriodEnds(5 * gun + 3_600_000L))
    }

    @Test
    fun geriSayimBicimi() {
        assertEquals("00:00", DailyQuestionPeriod.formatCountdown(0))
        assertEquals("00:00", DailyQuestionPeriod.formatCountdown(-5000))
        // Saniye YUKARI yuvarlanır: 1 ms → 1 sn
        assertEquals("00:01", DailyQuestionPeriod.formatCountdown(1))
        assertEquals("00:01", DailyQuestionPeriod.formatCountdown(1000))
        assertEquals("00:02", DailyQuestionPeriod.formatCountdown(1001))
        assertEquals("02:00", DailyQuestionPeriod.formatCountdown(120_000))
        assertEquals("59:59", DailyQuestionPeriod.formatCountdown(3_599_000))
        // 1 saat ve üzeri: saat sıfır doldurulmaz
        assertEquals("1:00:00", DailyQuestionPeriod.formatCountdown(3_600_000))
        assertEquals("1:04:32", DailyQuestionPeriod.formatCountdown(3_872_000))
        assertEquals("24:00:00", DailyQuestionPeriod.formatCountdown(gun))
    }
}

class DailyQuestionChallengeTest {
    @Test
    fun tamamlanmaVeSlotSecimi() {
        val c0 = meydanOkuma(cozulen = 0)
        assertFalse(c0.isComplete)
        assertEquals(0, c0.currentSlotIndex)
        assertEquals(0, c0.slotForPlay()?.itemIndex)
        assertEquals("U0", c0.cardTitleUnit())
        assertEquals("Kolay", c0.cardDifficultyLabel())
        assertEquals(0, c0.playSlotIndex())

        val c2 = meydanOkuma(cozulen = 2)
        assertEquals(2, c2.currentSlotIndex)
        assertEquals("Zor", c2.cardDifficultyLabel())

        // 3/3: tamam; dizin son soruya sıkıştırılır, başlık son sorunun, zorluk boş
        val c3 = meydanOkuma(cozulen = 3)
        assertTrue(c3.isComplete)
        assertEquals(2, c3.currentSlotIndex)
        assertEquals("U2", c3.cardTitleUnit())
        assertEquals("", c3.cardDifficultyLabel())

        // Negatif sayaç 0'a sıkıştırılır
        assertEquals(0, meydanOkuma(cozulen = -4).currentSlotIndex)
    }

    @Test
    fun bekleyenDevamSlotuOnceliklidir() {
        val c = meydanOkuma(cozulen = 0, bekleyen = 1)
        assertTrue(c.needsDiamondContinue)
        assertEquals(1, c.slotForPlay()?.itemIndex)
        assertEquals(1, c.playSlotIndex())
        assertEquals("U1", c.cardTitleUnit())
        assertEquals("Orta", c.cardDifficultyLabel())
        // Geçersiz bekleyen dizin: slot yok (geçerli slota DÜŞMEZ)
        val gecersiz = meydanOkuma(cozulen = 0, bekleyen = 7)
        assertNull(gecersiz.slotForPlay())
        assertEquals("", gecersiz.cardTitleUnit())
        assertFalse(meydanOkuma().needsDiamondContinue)
    }

    @Test
    fun bosSoruListesi() {
        val c = DailyQuestionChallenge("p", 0, emptyList())
        assertEquals(0, c.currentSlotIndex)
        assertNull(c.slotForPlay())
        assertEquals("", c.cardTitleUnit())
        assertEquals("", c.cardDifficultyLabel())
        val tamam = DailyQuestionChallenge("p", 3, emptyList())
        assertEquals("", tamam.cardTitleUnit())
    }

    @Test
    fun kartDurumuKurallari() {
        fun kart(cozulen: Int, toplam: Int = 3, tamam: Boolean = false, odul: Boolean = false, bekleyen: Int? = null) =
            DailyQuestionCardUiState("p", cozulen, toplam, "", "", tamam, odul, true, bekleyen)

        assertFalse(kart(0).shouldShowChallengeCompleteToast())
        assertTrue(kart(3).shouldShowChallengeCompleteToast())
        assertTrue(kart(0, tamam = true).shouldShowChallengeCompleteToast())
        // Ödül alındıysa asla
        assertFalse(kart(3, tamam = true, odul = true).shouldShowChallengeCompleteToast())
        // toplam 0 → 1 kabul edilir
        assertFalse(kart(0, toplam = 0).shouldShowChallengeCompleteToast())
        assertTrue(kart(1, toplam = 0).shouldShowChallengeCompleteToast())

        assertTrue(kart(3, tamam = true).canClaimReward)
        assertFalse(kart(3, tamam = true, odul = true).canClaimReward)
        assertFalse(kart(3, tamam = false).canClaimReward)
        assertTrue(kart(0, bekleyen = 0).needsDiamondContinue)
        assertFalse(kart(0).needsDiamondContinue)
    }
}

class DailyQuestionPoolBuilderTest {
    @Test
    fun zorlukEtiketi() {
        assertEquals("Kolay", DailyQuestionPoolBuilder.difficultyLabel(sandik(record = null)))
        assertEquals("Kolay", DailyQuestionPoolBuilder.difficultyLabel(sandik(record = 999)))
        assertEquals("Orta", DailyQuestionPoolBuilder.difficultyLabel(sandik(record = 1000)))
        assertEquals("Orta", DailyQuestionPoolBuilder.difficultyLabel(sandik(record = 1999)))
        assertEquals("Zor", DailyQuestionPoolBuilder.difficultyLabel(sandik(record = 2000)))
        // cupPoint1 yoksa en fazla Orta; cupPoint2 de yoksa Kolay
        assertEquals("Orta", DailyQuestionPoolBuilder.difficultyLabel(sandik(record = 5000, cup1 = null)))
        assertEquals("Kolay", DailyQuestionPoolBuilder.difficultyLabel(sandik(record = 5000, cup1 = null, cup2 = null)))
        // cupPoint2 yok ama cupPoint1 var: Zor ya da Kolay
        assertEquals("Zor", DailyQuestionPoolBuilder.difficultyLabel(sandik(record = 2000, cup2 = null)))
        assertEquals("Kolay", DailyQuestionPoolBuilder.difficultyLabel(sandik(record = 1500, cup2 = null)))
    }

    @Test
    fun kaynakHavuzuYalnizcaBitmisVeUreteciOlanSandiklar() {
        val b = DailyQuestionPoolBuilder(KayitliUretec())
        // Dizinler: 0 ders, 1 sandık(bitti), 2 sandık(bitmedi), 3 sandık(bitti), 4 ders, 5 sandık(bitti)
        val ogeler = listOf(ders(), sandik(), sandik(bitti = false), sandik(), ders(), sandik())
        // Part 1: 1. bitmiş sandığın üreteci yok → havuza girmez; 2. ve 3. girer
        val p1 = b.buildSourcesForPart(ogeler, 1)
        assertEquals(listOf(3, 5), p1.map { it.listIndex })
        assertEquals(listOf(2, 3), p1.map { it.finishedChestIndex })
        assertTrue(p1.all { it.partId == 1 })
        // Part 2: 1..4 hepsi üreteçli
        val p2 = b.buildSourcesForPart(ogeler, 2)
        assertEquals(listOf(1, 3, 5), p2.map { it.listIndex })
        assertEquals(listOf(1, 2, 3), p2.map { it.finishedChestIndex })
        // Part 4 ve ötesi: üreteç yok
        assertTrue(b.buildSourcesForPart(ogeler, 4).isEmpty())
        assertTrue(b.buildSourcesForPart(ogeler, 0).isEmpty())
    }

    @Test
    fun sandikSayisiSinirlari() {
        val b = DailyQuestionPoolBuilder(KayitliUretec())
        val yedi = List(7) { sandik() }
        // Part 1: 2..6 → 5 kaynak; Part 2 ve 3: 1..4 → 4 kaynak
        assertEquals(listOf(2, 3, 4, 5, 6), b.buildSourcesForPart(yedi, 1).map { it.finishedChestIndex })
        assertEquals(listOf(1, 2, 3, 4), b.buildSourcesForPart(yedi, 2).map { it.finishedChestIndex })
        assertEquals(listOf(1, 2, 3, 4), b.buildSourcesForPart(yedi, 3).map { it.finishedChestIndex })
    }

    /** Tek kaynaklı havuzla (rastgelelik yok) üretilen meydan okumanın çağırdığı üreteç ve süre. */
    private fun tekKaynak(partId: Int, sandikSirasi: Int, record: Int?): Pair<String, Long?> {
        val u = KayitliUretec()
        val b = DailyQuestionPoolBuilder(u)
        val kaynak = DailyQuestionSource(listIndex = 9, item = sandik(record = record), finishedChestIndex = sandikSirasi, partId = partId)
        val c = assertNotNull(b.buildChallenge("period_1", "uid", listOf(kaynak)))
        assertEquals(3, c.questions.size)
        assertEquals(3, u.cagrilar.size)
        assertEquals(1, u.cagrilar.toSet().size)
        return u.cagrilar.first() to c.questions.first().displayIntervalMs
    }

    @Test
    fun uretecTablosuPart1() {
        // record: null → kolay, 1000 → orta, 2000 → zor (cupPoint2=1000, cupPoint1=2000)
        assertEquals("generateSequence1Digits(3,2)" to 3000L, tekKaynak(1, 2, null))
        assertEquals("generateSequence1Digits(4,3)" to 2500L, tekKaynak(1, 2, 1000))
        assertEquals("generateSequence1Digits(4,4)" to 1500L, tekKaynak(1, 2, 2000))
        assertEquals("generateRelatedNumbers2Blinding(3,2)" to 3000L, tekKaynak(1, 3, null))
        assertEquals("generateRelatedNumbers2Blinding(4,3)" to 2500L, tekKaynak(1, 3, 1000))
        assertEquals("generateRelatedNumbers2Blinding(4,4)" to 1500L, tekKaynak(1, 3, 2000))
        assertEquals("generateSequence10RulesEasyNew(3)" to 3000L, tekKaynak(1, 4, null))
        assertEquals("generateSequence10RulesEasyNew(4)" to 2500L, tekKaynak(1, 4, 1000))
        assertEquals("generateSequence10RulesEasyNew(5)" to 1500L, tekKaynak(1, 4, 2000))
        assertEquals("generateSequence10RulesEasyOld(3)" to 3000L, tekKaynak(1, 5, null))
        assertEquals("generateSequence10RulesEasyOld(4)" to 2500L, tekKaynak(1, 5, 1000))
        assertEquals("generateSequence10RulesEasyOld(5)" to 1500L, tekKaynak(1, 5, 2000))
        assertEquals("generateSequenceBeadRules(3)" to 3000L, tekKaynak(1, 6, null))
        assertEquals("generateSequenceBeadRules(4)" to 2500L, tekKaynak(1, 6, 1000))
        assertEquals("generateSequenceBeadRules(5)" to 1500L, tekKaynak(1, 6, 2000))
    }

    @Test
    fun uretecTablosuPart2() {
        assertEquals("generateSequenceExtraction(3)" to 3000L, tekKaynak(2, 1, null))
        assertEquals("generateSequenceExtractionThreeDigits(4)" to 2500L, tekKaynak(2, 1, 1000))
        assertEquals("generateSequenceExtractionThreeDigits(4)" to 1500L, tekKaynak(2, 1, 2000))
        assertEquals("generateSequenceExtractionFiveRules(3)" to 3000L, tekKaynak(2, 2, null))
        assertEquals("generateSequenceExtractionFiveRulesThreeDigits(3)" to 3500L, tekKaynak(2, 2, 1000))
        assertEquals("generateSequenceExtractionFiveRulesThreeDigits(4)" to 1500L, tekKaynak(2, 2, 2000))
        assertEquals("generateSequenceExtractionTenRules(3)" to 3000L, tekKaynak(2, 3, null))
        assertEquals("generateSequenceExtractionTenRulesThreeDigits(3)" to 3500L, tekKaynak(2, 3, 1000))
        assertEquals("generateSequenceExtractionTenRulesThreeDigits(4)" to 1500L, tekKaynak(2, 3, 2000))
        assertEquals("generateSequenceExtractionBeadRules(3)" to 3000L, tekKaynak(2, 4, null))
        assertEquals("generateSequenceExtractionBeadRulesThreeDigits(3)" to 3500L, tekKaynak(2, 4, 1000))
        assertEquals("generateSequenceExtractionBeadRulesThreeDigits(4)" to 1500L, tekKaynak(2, 4, 2000))
    }

    @Test
    fun uretecTablosuPart3() {
        assertEquals("multiplicationLessFive()" to 3000L, tekKaynak(3, 1, null))
        assertEquals("multiplicationFull()" to 2500L, tekKaynak(3, 1, 1000))
        assertEquals("multiplicationLessFiveFull()" to 4000L, tekKaynak(3, 1, 2000))
        assertEquals("multiplicationTwo()" to 3000L, tekKaynak(3, 2, null))
        assertEquals("multiplicationTwo()" to 2500L, tekKaynak(3, 2, 1000))
        assertEquals("multiplicationTwoFull()" to 4000L, tekKaynak(3, 2, 2000))
        assertEquals("multiplicationThreeFull()" to 3000L, tekKaynak(3, 3, null))
        assertEquals("multiplicationThreeFull()" to 2500L, tekKaynak(3, 3, 1000))
        assertEquals("multiplicationThreeFull()" to 4000L, tekKaynak(3, 3, 2000))
        assertEquals("multiplicationThreeTwoFive()" to 3000L, tekKaynak(3, 4, null))
        assertEquals("multiplicationThreeTwoFive()" to 2500L, tekKaynak(3, 4, 1000))
        assertEquals("multiplicationThreeTwoFull()" to 4000L, tekKaynak(3, 4, 2000))
    }

    @Test
    fun slotAlanlari() {
        val b = DailyQuestionPoolBuilder(KayitliUretec())
        // Dizi sorusu
        val dizi = assertNotNull(
            b.buildChallenge(
                "period_7", "uid",
                listOf(DailyQuestionSource(4, sandik(record = 1500, titleUnit = "Toplama / 1"), 2, 1)),
            ),
        )
        assertEquals("period_7", dizi.periodKey)
        assertEquals(0, dizi.solvedCount)
        assertFalse(dizi.rewardClaimed)
        assertNull(dizi.pendingContinueSlotIndex)
        val s = dizi.questions[0]
        assertEquals(listOf(1, -2, 3), s.sequence)
        assertNull(s.mathOperation)
        assertEquals(1, s.partId)
        assertEquals(4, s.itemIndex)
        assertEquals("Toplama / 1", s.titleUnit)
        assertEquals("Orta", s.difficulty)
        assertEquals(2500L, s.displayIntervalMs)

        // Çarpma sorusu: dizi boş, işlem dolu
        val carpma = assertNotNull(b.buildChallenge("p", "uid", listOf(DailyQuestionSource(2, sandik(), 1, 3))))
        assertEquals(emptyList(), carpma.questions[0].sequence)
        assertEquals(MathOperation(12, "x", 3), carpma.questions[0].mathOperation)

        // titleUnit boş/null → title kullanılır
        val bosUnit = assertNotNull(b.buildChallenge("p", "uid", listOf(DailyQuestionSource(2, sandik(titleUnit = "  ", title = "Asil"), 1, 3))))
        assertEquals("Asil", bosUnit.questions[0].titleUnit)
        val nullUnit = assertNotNull(b.buildChallenge("p", "uid", listOf(DailyQuestionSource(2, sandik(titleUnit = null, title = "Asil"), 1, 3))))
        assertEquals("Asil", nullUnit.questions[0].titleUnit)
    }

    @Test
    fun uretilemeyenDurumlarNullDoner() {
        val b = DailyQuestionPoolBuilder(KayitliUretec())
        // Boş havuz
        assertNull(b.buildChallenge("p", "uid", emptyList()))
        // Üreteci olmayan kaynak (part 1, 1. sandık) → 3 soru çıkmaz → null
        assertNull(b.buildChallenge("p", "uid", listOf(DailyQuestionSource(0, sandik(), 1, 1))))
        // Üreteç boş dizi döndürürse slot üretilemez → null
        val bos = KayitliUretec().apply { bosDizi = true }
        assertNull(DailyQuestionPoolBuilder(bos).buildChallenge("p", "uid", listOf(DailyQuestionSource(0, sandik(), 2, 1))))
    }

    @Test
    fun cokKaynakliHavuzdaSecimVerilenRastgeleKaynagiKullanir() {
        // Android'de seçim "TEST MODU" gereği tamamen rastgeledir (periodKey/uid etkisiz).
        // Aynı tohumla aynı seçim, havuzun tamamı da erişilebilir olmalı.
        val kaynaklar = listOf(
            DailyQuestionSource(10, sandik(titleUnit = "A"), 1, 2),
            DailyQuestionSource(11, sandik(titleUnit = "B"), 2, 2),
            DailyQuestionSource(12, sandik(titleUnit = "C"), 3, 2),
        )
        val a = DailyQuestionPoolBuilder(KayitliUretec(), Random(5)).buildChallenge("p1", "u1", kaynaklar)
        val b = DailyQuestionPoolBuilder(KayitliUretec(), Random(5)).buildChallenge("p2", "u2", kaynaklar)
        assertEquals(a?.questions?.map { it.itemIndex }, b?.questions?.map { it.itemIndex })
        val beklenen = Random(5).let { r -> List(3) { kaynaklar[(0 until 3).random(r)].listIndex } }
        assertEquals(beklenen, a?.questions?.map { it.itemIndex })

        val gorulen = mutableSetOf<Int>()
        val builder = DailyQuestionPoolBuilder(KayitliUretec(), Random(1))
        repeat(50) { builder.buildChallenge("p", "u", kaynaklar)?.questions?.forEach { gorulen.add(it.itemIndex) } }
        assertEquals(setOf(10, 11, 12), gorulen)
    }
}

class DailyQuestionRepositoryTest {
    private fun belge(vararg degisiklik: Pair<String, Any?>): Map<String, Any?> {
        val temel = mutableMapOf<String, Any?>(
            "periodKey" to "period_100",
            "solvedCount" to 1L,
            "rewardClaimed" to false,
            "questions" to listOf(
                mapOf("partId" to 1L, "itemIndex" to 4L, "titleUnit" to "U1", "difficulty" to "Kolay", "sequence" to "1, -2,3", "displayIntervalMs" to 3000L),
                mapOf("partId" to 3L, "itemIndex" to 2L, "titleUnit" to "U2", "difficulty" to "Zor", "mathFirst" to 12L, "mathOperator" to "x", "mathSecond" to 3L),
                mapOf("itemIndex" to 6.0, "titleUnit" to "U3", "difficulty" to "Orta", "sequence" to "5"),
            ),
        )
        degisiklik.forEach { (k, v) -> if (v == null) temel.remove(k) else temel[k] = v }
        return temel
    }

    @Test
    fun yollarVeAnahtarlar() {
        assertEquals("users/abc/dailyQuestion/current", DailyQuestionRepository.challengeDocPath("abc"))
        assertEquals("daily_challenge_abc_current", DailyQuestionRepository.challengeCacheKey("abc"))
        assertEquals("generatorForPart2Chest", DailyQuestionRepository.partCollectionName(2))
        assertEquals("Toplama - 1", DailyQuestionRepository.successRateDocId("Toplama / 1"))
        assertEquals("a-b-c", DailyQuestionRepository.successRateDocId("a/b/c"))
        assertEquals(
            "successRate/dailyQuestionSuccessRate/generatorForPart1Chest/5-10",
            DailyQuestionRepository.successRateDocPath(slot(0, unit = "5/10")),
        )
        assertEquals("easy_rate", DailyQuestionRepository.rateField("easy"))
        assertEquals("easy_total", DailyQuestionRepository.totalField("easy"))
        assertEquals("easy_success", DailyQuestionRepository.successField("easy"))
    }

    @Test
    fun zorlukAnahtari() {
        assertEquals("easy", DailyQuestionRepository.difficultyKey("Kolay"))
        assertEquals("medium", DailyQuestionRepository.difficultyKey("ORTA"))
        assertEquals("hard", DailyQuestionRepository.difficultyKey("zor"))
        assertNull(DailyQuestionRepository.difficultyKey(""))
        assertNull(DailyQuestionRepository.difficultyKey("Easy"))
    }

    @Test
    fun belgeAyristirma() {
        val c = assertNotNull(DailyQuestionRepository.parseChallengeDocument(belge(), "period_100"))
        assertEquals("period_100", c.periodKey)
        assertEquals(1, c.solvedCount)
        assertFalse(c.rewardClaimed)
        assertNull(c.pendingContinueSlotIndex)
        assertEquals(
            DailyQuestionSlot(sequence = listOf(1, -2, 3), partId = 1, itemIndex = 4, titleUnit = "U1", difficulty = "Kolay", displayIntervalMs = 3000L),
            c.questions[0],
        )
        assertEquals(
            DailyQuestionSlot(partId = 3, itemIndex = 2, titleUnit = "U2", difficulty = "Zor", mathOperation = MathOperation(12, "x", 3)),
            c.questions[1],
        )
        // partId yoksa 1; sayı Double gelse de Int'e çevrilir; displayIntervalMs yoksa null
        assertEquals(
            DailyQuestionSlot(sequence = listOf(5), partId = 1, itemIndex = 6, titleUnit = "U3", difficulty = "Orta"),
            c.questions[2],
        )
    }

    @Test
    fun belgeAyristirmaReddedilenDurumlar() {
        assertNull(DailyQuestionRepository.parseChallengeDocument(null, "period_100"))
        // Periyot uyuşmazlığı / eksik periyot
        assertNull(DailyQuestionRepository.parseChallengeDocument(belge(), "period_101"))
        assertNull(DailyQuestionRepository.parseChallengeDocument(belge("periodKey" to null), "period_100"))
        // Soru listesi yok ya da liste değil
        assertNull(DailyQuestionRepository.parseChallengeDocument(belge("questions" to null), "period_100"))
        assertNull(DailyQuestionRepository.parseChallengeDocument(belge("questions" to "x"), "period_100"))
        // 3'ten az geçerli soru
        val iki = (belge()["questions"] as List<*>).take(2)
        assertNull(DailyQuestionRepository.parseChallengeDocument(belge("questions" to iki), "period_100"))
        // Bir soru geçersizse (itemIndex yok) toplam 2 kalır → null
        val bozuk = (belge()["questions"] as List<*>).take(2) + mapOf("titleUnit" to "U", "difficulty" to "Kolay", "sequence" to "1")
        assertNull(DailyQuestionRepository.parseChallengeDocument(belge("questions" to bozuk), "period_100"))
        // 4 geçerli soru da reddedilir
        val dort = (belge()["questions"] as List<*>) + (belge()["questions"] as List<*>).take(1)
        assertNull(DailyQuestionRepository.parseChallengeDocument(belge("questions" to dort), "period_100"))
    }

    @Test
    fun belgeAyristirmaAlanVarsayilanlari() {
        val c = assertNotNull(
            DailyQuestionRepository.parseChallengeDocument(
                belge("solvedCount" to null, "rewardClaimed" to true, "pendingContinueSlotIndex" to 9L),
                "period_100",
            ),
        )
        assertEquals(0, c.solvedCount)
        assertTrue(c.rewardClaimed)
        // Bekleyen slot 0..2'ye sıkıştırılır
        assertEquals(2, c.pendingContinueSlotIndex)
        val negatif = assertNotNull(DailyQuestionRepository.parseChallengeDocument(belge("pendingContinueSlotIndex" to -3L), "period_100"))
        assertEquals(0, negatif.pendingContinueSlotIndex)
        // rewardClaimed Boolean değilse false
        val yanlisTip = assertNotNull(DailyQuestionRepository.parseChallengeDocument(belge("rewardClaimed" to "true"), "period_100"))
        assertFalse(yanlisTip.rewardClaimed)
        // solvedCount sıkıştırılmaz (belgedeki değer aynen alınır)
        val buyuk = assertNotNull(DailyQuestionRepository.parseChallengeDocument(belge("solvedCount" to 7L), "period_100"))
        assertEquals(7, buyuk.solvedCount)
    }

    @Test
    fun slotAyristirma() {
        val temel = mapOf("itemIndex" to 1L, "titleUnit" to "U", "difficulty" to "Kolay")
        // Ne dizi ne işlem → null
        assertNull(DailyQuestionRepository.parseSlot(temel))
        // Dizideki bozuk parçalar atlanır; hepsi bozuksa dizi boş → null
        assertEquals(listOf(1, 3), DailyQuestionRepository.parseSlot(temel + ("sequence" to "1,x, 3,"))?.sequence)
        assertNull(DailyQuestionRepository.parseSlot(temel + ("sequence" to "a,b")))
        // İşlemin üç alanı da gerekir
        assertNull(DailyQuestionRepository.parseSlot(temel + mapOf("mathFirst" to 1L, "mathOperator" to "x")))
        assertNotNull(DailyQuestionRepository.parseSlot(temel + mapOf("mathFirst" to 1L, "mathOperator" to "x", "mathSecond" to 2L)))
        // Zorunlu alanlar
        assertNull(DailyQuestionRepository.parseSlot(temel - "itemIndex" + ("sequence" to "1")))
        assertNull(DailyQuestionRepository.parseSlot(temel - "titleUnit" + ("sequence" to "1")))
        assertNull(DailyQuestionRepository.parseSlot(temel - "difficulty" + ("sequence" to "1")))
        // Hem dizi hem işlem birlikte korunur
        val ikisi = assertNotNull(
            DailyQuestionRepository.parseSlot(temel + mapOf("sequence" to "4", "mathFirst" to 1L, "mathOperator" to "x", "mathSecond" to 2L)),
        )
        assertEquals(listOf(4), ikisi.sequence)
        assertEquals(MathOperation(1, "x", 2), ikisi.mathOperation)
    }

    @Test
    fun firestoreEslemesi() {
        val c = DailyQuestionChallenge(
            periodKey = "period_100",
            solvedCount = 2,
            questions = listOf(
                DailyQuestionSlot(sequence = listOf(1, -2, 3), partId = 1, itemIndex = 4, titleUnit = "U1", difficulty = "Kolay", displayIntervalMs = 3000L),
                DailyQuestionSlot(partId = 3, itemIndex = 2, titleUnit = "U2", difficulty = "Zor", mathOperation = MathOperation(12, "x", 3)),
                DailyQuestionSlot(sequence = listOf(5), partId = 2, itemIndex = 6, titleUnit = "U3", difficulty = "Orta", mathOperation = MathOperation(7, null, null)),
            ),
            rewardClaimed = true,
        )
        val m = DailyQuestionRepository.challengeToFirestoreMap(c)
        assertEquals(setOf("periodKey", "solvedCount", "rewardClaimed", "questions"), m.keys)
        assertEquals("period_100", m["periodKey"])
        assertEquals(2, m["solvedCount"])
        assertEquals(true, m["rewardClaimed"])
        val sorular = m["questions"] as List<*>
        assertEquals(
            mapOf<String, Any>("partId" to 1, "itemIndex" to 4, "titleUnit" to "U1", "difficulty" to "Kolay", "sequence" to "1,-2,3", "displayIntervalMs" to 3000L),
            sorular[0],
        )
        assertEquals(
            mapOf<String, Any>("partId" to 3, "itemIndex" to 2, "titleUnit" to "U2", "difficulty" to "Zor", "mathFirst" to 12, "mathOperator" to "x", "mathSecond" to 3),
            sorular[1],
        )
        // İşlemin null alanları yazılmaz
        assertEquals(
            mapOf<String, Any>("partId" to 2, "itemIndex" to 6, "titleUnit" to "U3", "difficulty" to "Orta", "sequence" to "5", "mathFirst" to 7),
            sorular[2],
        )
        // Bekleyen slot yalnızca varsa yazılır
        val bekleyenli = DailyQuestionRepository.challengeToFirestoreMap(c.copy(pendingContinueSlotIndex = 1))
        assertEquals(1, bekleyenli["pendingContinueSlotIndex"])
    }

    @Test
    fun yazOkuGidisDonus() {
        val c = meydanOkuma(cozulen = 1, bekleyen = 2).copy(
            questions = listOf(
                DailyQuestionSlot(sequence = listOf(9, -8), partId = 2, itemIndex = 3, titleUnit = "A", difficulty = "Zor", displayIntervalMs = 1500L),
                DailyQuestionSlot(partId = 3, itemIndex = 1, titleUnit = "B", difficulty = "Orta", displayIntervalMs = 2500L, mathOperation = MathOperation(4, "x", 5)),
                DailyQuestionSlot(sequence = listOf(1), partId = 1, itemIndex = 0, titleUnit = "C", difficulty = "Kolay"),
            ),
        )
        val geri = DailyQuestionRepository.parseChallengeDocument(DailyQuestionRepository.challengeToFirestoreMap(c), "period_100")
        assertEquals(c, geri)
        assertEquals(c, DailyQuestionRepository.resolveLoadedChallenge(true, DailyQuestionRepository.challengeToFirestoreMap(c), "period_100"))
        // Belge yoksa ya da periyot eskiyse yeni meydan okuma üretilmeli (null)
        assertNull(DailyQuestionRepository.resolveLoadedChallenge(false, DailyQuestionRepository.challengeToFirestoreMap(c), "period_100"))
        assertNull(DailyQuestionRepository.resolveLoadedChallenge(true, DailyQuestionRepository.challengeToFirestoreMap(c), "period_101"))
        assertNull(DailyQuestionRepository.resolveLoadedChallenge(true, null, "period_100"))
    }

    @Test
    fun kartDurumunaCevirme() {
        val k = DailyQuestionRepository.toCardUiState(meydanOkuma(cozulen = 1, bekleyen = 2), poolAvailable = true)
        assertEquals(
            DailyQuestionCardUiState(
                periodKey = "period_100", solvedCount = 1, totalCount = 3, titleUnit = "U2", difficulty = "Zor",
                isComplete = false, rewardClaimed = false, poolAvailable = true, pendingContinueSlotIndex = 2,
                globalSuccessRate = null, isLoaded = true,
            ),
            k,
        )
        val tamam = DailyQuestionRepository.toCardUiState(meydanOkuma(cozulen = 3, odul = true), poolAvailable = false)
        assertTrue(tamam.isComplete)
        assertTrue(tamam.rewardClaimed)
        assertEquals("U2", tamam.titleUnit)
        assertEquals("", tamam.difficulty)
        assertFalse(tamam.poolAvailable)

        assertEquals(
            DailyQuestionCardUiState("period_5", 0, 3, "", "", isComplete = false, rewardClaimed = false, poolAvailable = true, isLoaded = true),
            DailyQuestionRepository.cardUiStateWithoutChallenge("period_5", true),
        )
    }

    @Test
    fun basariOraniHesabi() {
        // İlk kayıt, başarılı: 1/1 → %100
        assertEquals(
            mapOf<String, Any>("easy_total" to 1L, "easy_success" to 1L, "easy_rate" to 100),
            DailyQuestionRepository.successRateUpdates("easy", null, null, true),
        )
        // İlk kayıt, başarısız: 0/1 → %0
        assertEquals(
            mapOf<String, Any>("hard_total" to 1L, "hard_success" to 0L, "hard_rate" to 0),
            DailyQuestionRepository.successRateUpdates("hard", null, null, false),
        )
        // 1/2 başarılıyken başarılı → 2/3 = %66 (aşağı yuvarlanır)
        assertEquals(
            mapOf<String, Any>("medium_total" to 3L, "medium_success" to 2L, "medium_rate" to 66),
            DailyQuestionRepository.successRateUpdates("medium", 2L, 1L, true),
        )
        // 1/2 başarılıyken başarısız → 1/3 = %33
        assertEquals(33, DailyQuestionRepository.successRateUpdates("medium", 2L, 1L, false)["medium_rate"])
        // 6/6 → 7/7 = %100
        assertEquals(100, DailyQuestionRepository.successRateUpdates("easy", 6L, 6L, true)["easy_rate"])
    }

    @Test
    fun cozulenSayisiVeSlotSikistirma() {
        assertEquals(1, DailyQuestionRepository.nextSolvedCount(null))
        assertEquals(1, DailyQuestionRepository.nextSolvedCount(0L))
        assertEquals(3, DailyQuestionRepository.nextSolvedCount(2L))
        assertEquals(3, DailyQuestionRepository.nextSolvedCount(3L))
        assertEquals(3, DailyQuestionRepository.nextSolvedCount(99L))
        assertEquals(1, DailyQuestionRepository.nextSolvedCount(-5L))

        assertEquals(0, DailyQuestionRepository.safePendingContinueSlot(-1))
        assertEquals(1, DailyQuestionRepository.safePendingContinueSlot(1))
        assertEquals(2, DailyQuestionRepository.safePendingContinueSlot(3))
    }

    @Test
    fun onbellekKurallari() {
        val cache = InMemoryDailyQuestionChallengeCache()
        val anahtar = DailyQuestionRepository.challengeCacheKey("u")
        // Boş önbellek
        assertNull(DailyQuestionRepository.readChallengeFromPrefs(cache, anahtar, "period_100"))
        cache.write(anahtar, meydanOkuma(cozulen = 1, bekleyen = 1))
        // Periyot uyuşmazsa okunmaz ve güncellenmez
        assertNull(DailyQuestionRepository.readChallengeFromPrefs(cache, anahtar, "period_101"))
        DailyQuestionRepository.cacheRewardClaimed(cache, anahtar, "period_101")
        assertFalse(cache.read(anahtar)!!.rewardClaimed)

        DailyQuestionRepository.cacheRewardClaimed(cache, anahtar, "period_100")
        assertTrue(cache.read(anahtar)!!.rewardClaimed)

        DailyQuestionRepository.updateCachedPendingContinue(cache, anahtar, "period_100", 2)
        assertEquals(2, cache.read(anahtar)!!.pendingContinueSlotIndex)
        DailyQuestionRepository.updateCachedPendingContinue(cache, anahtar, "period_100", null)
        assertNull(cache.read(anahtar)!!.pendingContinueSlotIndex)

        // Sayaç yazılırken bekleyen slot temizlenir, ödül bayrağı korunur
        DailyQuestionRepository.updateCachedPendingContinue(cache, anahtar, "period_100", 1)
        DailyQuestionRepository.cacheSolvedCount(cache, anahtar, "period_100", 2)
        val son = cache.read(anahtar)!!
        assertEquals(2, son.solvedCount)
        assertNull(son.pendingContinueSlotIndex)
        assertTrue(son.rewardClaimed)

        // Okuma istisna fırlatırsa null (Android: bozuk JSON)
        val bozuk = object : DailyQuestionChallengeCache {
            override fun read(cacheKey: String): DailyQuestionChallenge? = throw IllegalStateException("bozuk")
            override fun write(cacheKey: String, challenge: DailyQuestionChallenge) = Unit
        }
        assertNull(DailyQuestionRepository.readChallengeFromPrefs(bozuk, anahtar, "period_100"))
    }
}

class DailyQuestionStoresTest {
    @Test
    fun kirikKalpAnahtarlari() {
        assertEquals("daily_broken_heart_play_u1_period_5", DailyQuestionBrokenHeartStore.playKey("u1", "period_5"))
        assertEquals("daily_broken_heart_hold116_u1_period_5", DailyQuestionBrokenHeartStore.hold116Key("u1", "period_5"))
        assertEquals("daily_broken_heart_heal_u1_period_5", DailyQuestionBrokenHeartStore.healPlayKey("u1", "period_5"))
        assertEquals("daily_card_anim_shown_u1_period_5", DailyQuestionCardProgressAnimStore.prefsKey("u1", "period_5"))
    }

    @Test
    fun kirikKalpDeposu() {
        val prefs = InMemoryKeyValueStore()
        val s = DailyQuestionBrokenHeartStore(prefs) { "u1" }
        assertFalse(s.consumePlayRequest("p"))
        s.requestPlayOnNextBind("p")
        // Tüketim bir kez true döner, sonra false
        assertTrue(s.consumePlayRequest("p"))
        assertFalse(s.consumePlayRequest("p"))
        // Başka periyodu etkilemez
        s.requestPlayOnNextBind("p")
        assertFalse(s.consumePlayRequest("q"))

        assertFalse(s.isBrokenHold116("p"))
        s.setBrokenHold116("p", true)
        assertTrue(s.isBrokenHold116("p"))
        s.clearBrokenHold116("p")
        assertFalse(s.isBrokenHold116("p"))

        assertFalse(s.consumeHealPlay("p"))
        s.requestHealPlay("p")
        assertTrue(s.consumeHealPlay("p"))
        assertFalse(s.consumeHealPlay("p"))

        s.requestHealPlay("p")
        s.setBrokenHold116("p", true)
        s.resetForPeriod("p")
        assertFalse(prefs.contains("daily_broken_heart_play_u1_p"))
        assertFalse(prefs.contains("daily_broken_heart_hold116_u1_p"))
        assertFalse(prefs.contains("daily_broken_heart_heal_u1_p"))
    }

    @Test
    fun oturumYokkenDepolarEtkisiz() {
        val prefs = InMemoryKeyValueStore()
        val s = DailyQuestionBrokenHeartStore(prefs) { null }
        s.requestPlayOnNextBind("p")
        s.setBrokenHold116("p", true)
        s.requestHealPlay("p")
        s.resetForPeriod("p")
        assertTrue(prefs.keys().isEmpty())
        assertFalse(s.consumePlayRequest("p"))
        assertFalse(s.isBrokenHold116("p"))
        assertFalse(s.consumeHealPlay("p"))

        val a = DailyQuestionCardProgressAnimStore(prefs) { null }
        a.setLastShown("p", 2)
        assertNull(a.getLastShown("p"))
        assertTrue(prefs.keys().isEmpty())
    }

    @Test
    fun ilerlemeAnimasyonDeposu() {
        val a = DailyQuestionCardProgressAnimStore(InMemoryKeyValueStore()) { "u1" }
        assertNull(a.getLastShown("p"))
        a.setLastShown("p", 2)
        assertEquals(2, a.getLastShown("p"))
        a.setLastShown("p", 0)
        assertEquals(0, a.getLastShown("p"))
        // Negatif değer 0 olarak yazılır
        a.setLastShown("p", -3)
        assertEquals(0, a.getLastShown("p"))
        assertNull(a.getLastShown("q"))
    }
}

class DailyQuestionCardLogicTest {
    private fun kart(
        cozulen: Int,
        toplam: Int = 3,
        yuklendi: Boolean = true,
        tamam: Boolean = false,
        odul: Boolean = false,
        zorluk: String = "Kolay",
        oran: Int? = null,
    ) = DailyQuestionCardUiState(
        periodKey = "p", solvedCount = cozulen, totalCount = toplam, titleUnit = "U", difficulty = zorluk,
        isComplete = tamam, rewardClaimed = odul, poolAvailable = true, globalSuccessRate = oran, isLoaded = yuklendi,
    )

    @Test
    fun ilerlemePlani() {
        // Daha önce gösterilmemiş: animasyon yok, doğrudan son değer
        var p = DailyQuestionCardLogic.progressPlan(kart(2), lastShown = null)
        assertEquals(2, p.fromSolved)
        assertEquals(p.endPct, p.startPct)
        assertFalse(p.shouldAnimate)
        assertEquals(200f / 3, p.endPct)

        // 1 → 2: animasyon
        p = DailyQuestionCardLogic.progressPlan(kart(2), lastShown = 1)
        assertEquals(1, p.fromSolved)
        assertEquals(100f / 3, p.startPct)
        assertEquals(200f / 3, p.endPct)
        assertTrue(p.shouldAnimate)

        // 0 → 3
        p = DailyQuestionCardLogic.progressPlan(kart(3), lastShown = 0)
        assertEquals(0f, p.startPct)
        assertEquals(100f, p.endPct)
        assertTrue(p.shouldAnimate)

        // Aynı değer: animasyon yok
        assertFalse(DailyQuestionCardLogic.progressPlan(kart(2), lastShown = 2).shouldAnimate)

        // Son gösterilen daha büyükse (yeni periyot vb.) geriye animasyon yapılmaz
        p = DailyQuestionCardLogic.progressPlan(kart(1), lastShown = 3)
        assertEquals(1, p.fromSolved)
        assertFalse(p.shouldAnimate)

        // Sayaç toplamı aşarsa toplamda kırpılır; toplam 0 → 1
        p = DailyQuestionCardLogic.progressPlan(kart(9), lastShown = null)
        assertEquals(3, p.solved)
        assertEquals(100f, p.endPct)
        p = DailyQuestionCardLogic.progressPlan(kart(5, toplam = 0), lastShown = 0)
        assertEquals(1, p.solved)
        assertEquals(100f, p.endPct)
        assertTrue(p.shouldAnimate)

        // Yüklenmemiş kart: depo değeri yok sayılır, animasyon yok
        p = DailyQuestionCardLogic.progressPlan(kart(2, yuklendi = false), lastShown = 0)
        assertFalse(p.shouldAnimate)
        assertEquals(p.endPct, p.startPct)
    }

    @Test
    fun kalicilastirilacakDeger() {
        assertNull(DailyQuestionCardLogic.solvedToPersist(kart(2, yuklendi = false)))
        assertEquals(2, DailyQuestionCardLogic.solvedToPersist(kart(2)))
        assertEquals(3, DailyQuestionCardLogic.solvedToPersist(kart(8)))
        assertEquals(0, DailyQuestionCardLogic.solvedToPersist(kart(-1)))
    }

    @Test
    fun odulDokunusuHazirMi() {
        val hazir = kart(3, tamam = true)
        assertTrue(DailyQuestionCardLogic.isProgressClaimTapReady(hazir, 100f, animatorRunning = false))
        assertTrue(DailyQuestionCardLogic.isProgressClaimTapReady(hazir, 99.5f, animatorRunning = false))
        assertFalse(DailyQuestionCardLogic.isProgressClaimTapReady(hazir, 99.4f, animatorRunning = false))
        assertFalse(DailyQuestionCardLogic.isProgressClaimTapReady(hazir, 100f, animatorRunning = true))
        assertFalse(DailyQuestionCardLogic.isProgressClaimTapReady(kart(3, tamam = true, odul = true), 100f, false))
        assertFalse(DailyQuestionCardLogic.isProgressClaimTapReady(kart(2), 100f, false))
        assertFalse(DailyQuestionCardLogic.isProgressClaimTapReady(null, 100f, false))

        assertTrue(DailyQuestionCardLogic.canTapProgress(null))
        assertTrue(DailyQuestionCardLogic.canTapProgress(kart(1)))
        assertFalse(DailyQuestionCardLogic.canTapProgress(kart(3, odul = true)))
    }

    @Test
    fun odakEtiketi() {
        assertEquals("Kolay", DailyQuestionCardLogic.focusLabelArgument(kart(1)))
        assertEquals("Zor - Başarı: %42", DailyQuestionCardLogic.focusLabelArgument(kart(1, zorluk = "Zor", oran = 42)))
        assertEquals("Orta - Başarı: %0", DailyQuestionCardLogic.focusLabelArgument(kart(1, zorluk = "Orta", oran = 0)))
        assertNull(DailyQuestionCardLogic.focusLabelArgument(kart(3, tamam = true)))
        assertNull(DailyQuestionCardLogic.focusLabelArgument(kart(1, zorluk = " ")))
    }

    @Test
    fun geriSayimTikiVeSabitler() {
        assertEquals(1000L, DailyQuestionCardLogic.countdownTickDelayMs(1))
        assertEquals(250L, DailyQuestionCardLogic.countdownTickDelayMs(0))
        assertEquals(2800L, DailyQuestionCardLogic.DAILY_PROGRESS_ANIM_DURATION_MS)
        assertEquals(99.5f, DailyQuestionCardLogic.CLAIM_READY_VISUAL_PERCENT)
        assertEquals(ChestRarity.RARE, DailyQuestionCardLogic.REWARD_CHEST_START_RARITY)
    }

    @Test
    fun kirikKalpEylemiOncelikSirasi() {
        val s = DailyQuestionBrokenHeartStore(InMemoryKeyValueStore()) { "u" }
        val k = kart(1)
        assertEquals(DailyQuestionBrokenHeartAction.SHOW_FIRST_FRAME, DailyQuestionCardLogic.brokenHeartAction(k, s))

        // Üçü birden kuruluyken sıra: iyileşme → kırılma → 116'da bekleme
        s.requestHealPlay("p")
        s.requestPlayOnNextBind("p")
        s.setBrokenHold116("p", true)
        // Yüklenmemiş kart hiçbir isteği tüketmez
        assertEquals(DailyQuestionBrokenHeartAction.SHOW_FIRST_FRAME, DailyQuestionCardLogic.brokenHeartAction(kart(1, yuklendi = false), s))
        assertEquals(DailyQuestionBrokenHeartAction.PLAY_HEAL, DailyQuestionCardLogic.brokenHeartAction(k, s))
        assertEquals(DailyQuestionBrokenHeartAction.PLAY_BREAK_TO_HOLD_116, DailyQuestionCardLogic.brokenHeartAction(k, s))
        assertEquals(DailyQuestionBrokenHeartAction.SHOW_HOLD_116, DailyQuestionCardLogic.brokenHeartAction(k, s))
        // Bekleme bayrağı tüketilmez
        assertEquals(DailyQuestionBrokenHeartAction.SHOW_HOLD_116, DailyQuestionCardLogic.brokenHeartAction(k, s))
    }

    @Test
    fun kartTiklamaKararlari() {
        assertEquals(DailyQuestionCardClickAction.LOAD_CHALLENGE, DailyQuestionCardLogic.cardClickAction(kart(1)))
        assertEquals(DailyQuestionCardClickAction.SHOW_CHALLENGE_COMPLETE_TOAST, DailyQuestionCardLogic.cardClickAction(kart(3, tamam = true)))
        assertEquals(DailyQuestionCardClickAction.IGNORE, DailyQuestionCardLogic.cardClickAction(kart(3, tamam = true, odul = true)))
        assertEquals(DailyQuestionCardClickAction.IGNORE, DailyQuestionCardLogic.cardClickAction(kart(0, odul = true)))

        assertEquals(DailyQuestionChallengeClickAction.LAUNCH_LESSON, DailyQuestionCardLogic.challengeClickAction(meydanOkuma(1)))
        assertEquals(DailyQuestionChallengeClickAction.IGNORE, DailyQuestionCardLogic.challengeClickAction(meydanOkuma(3, odul = true)))
        assertEquals(DailyQuestionChallengeClickAction.SHOW_CHALLENGE_COMPLETE_TOAST, DailyQuestionCardLogic.challengeClickAction(meydanOkuma(3)))
        assertEquals(DailyQuestionChallengeClickAction.SHOW_CONTINUE_PANEL, DailyQuestionCardLogic.challengeClickAction(meydanOkuma(1, bekleyen = 1)))
        // Tamamlanma, bekleyen devamdan önce gelir
        assertEquals(DailyQuestionChallengeClickAction.SHOW_CHALLENGE_COMPLETE_TOAST, DailyQuestionCardLogic.challengeClickAction(meydanOkuma(3, bekleyen = 1)))
        // Oynanacak slot yoksa yok say
        assertEquals(
            DailyQuestionChallengeClickAction.IGNORE,
            DailyQuestionCardLogic.challengeClickAction(DailyQuestionChallenge("p", 0, emptyList())),
        )
    }

    @Test
    fun periyotDevrindeYenileme() {
        // Kart zaten güncel periyotta: yenileme yok
        assertFalse(DailyQuestionCardLogic.shouldRefreshOnPeriodRollover("period_2", "period_2", 10_000, 0))
        // Farklı periyot ve son yenilemeden 400 ms geçmiş: yenile
        assertTrue(DailyQuestionCardLogic.shouldRefreshOnPeriodRollover("period_1", "period_2", 10_000, 9_600))
        // 400 ms dolmadıysa bekle
        assertFalse(DailyQuestionCardLogic.shouldRefreshOnPeriodRollover("period_1", "period_2", 10_000, 9_601))
        // Boş kart anahtarı da "farklı" sayılır
        assertTrue(DailyQuestionCardLogic.shouldRefreshOnPeriodRollover("", "period_2", 10_000, 0))
    }
}

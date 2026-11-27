// AbacusFragment / AbacusPracticeFragment / BlindingLessonFragment'ten ayıklanan ders mantığının
// Android ile aynı sonucu verdiğini doğrular. Beklenen değerler Android kaynağı okunarak elle
// türetilmiştir.
package com.sorobit.shared.abakus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AbacusLessonRulesTest {
    private val k = AbacusLessonRules

    @Test
    fun sayacBicimi() {
        assertEquals("0:00", k.formatTimer(0))
        assertEquals("0:09", k.formatTimer(9))
        assertEquals("0:59", k.formatTimer(59))
        assertEquals("1:00", k.formatTimer(60))
        assertEquals("10:05", k.formatTimer(605))
        assertEquals("60:00", k.formatTimer(3600)) // saat hanesi yok
    }

    @Test
    fun basariOraniVePuan() {
        assertEquals(75f, k.successRate(3, 4))
        assertEquals(100f, k.successRate(5, 5))
        assertEquals(0f, k.successRate(0, 0))
        assertEquals(0f, k.successRate(3, 0))
        assertEquals(375, k.dersPuani(75f))
        assertEquals(500, k.dersPuani(100f))
        assertEquals(166, k.dersPuani(k.successRate(1, 3))) // 33.33·5 = 166.67 → 166
        assertEquals(0, k.dersPuani(0f))
    }

    @Test
    fun sureCozumleme() {
        assertEquals(90, k.parseTimeToSeconds("1:30"))
        assertEquals(3723, k.parseTimeToSeconds("1:02:03"))
        assertEquals(125, k.parseTimeToSeconds(" 2 : 5 "))
        assertEquals(10, k.parseTimeToSeconds("x:10")) // sayı olmayan parça 0
        assertEquals(0, k.parseTimeToSeconds("abc"))
        assertEquals(0, k.parseTimeToSeconds("90"))
        assertEquals(0, k.parseTimeToSeconds("1:2:3:4"))
        assertEquals(0, k.parseTimeToSeconds(""))
        assertEquals(0, k.parseTimeToSeconds("0:00"))
    }

    @Test
    fun kupaSuresiYedegi() {
        assertEquals(100, k.resolveWorstCupTimeFallback(100, 50))
        assertEquals(50, k.resolveWorstCupTimeFallback(0, 50))
        assertEquals(50, k.resolveWorstCupTimeFallback(null, 50))
        assertEquals(240, k.resolveWorstCupTimeFallback(null, 0))
        assertEquals(240, k.resolveWorstCupTimeFallback(-1, null))
    }

    @Test
    fun sandikPuani() {
        // 1 dk / 240 sn: çarpan 4 − 180/240 = 3.25 → 500·3.25 = 1625
        assertEquals(Pair(3.25f, 1625), k.calculateChestScore(100f, "1:00", 240))
        assertEquals(Pair(1f, 500), k.calculateChestScore(100f, "4:00", 240)) // tam kupa süresi
        assertEquals(Pair(1f, 500), k.calculateChestScore(100f, "8:00", 240)) // alt sınır 1
        assertEquals(Pair(4f, 1000), k.calculateChestScore(50f, "0:00", 240)) // üst sınır 4
        assertEquals(Pair(2.5f, 1000), k.calculateChestScore(80f, "2:00", 240))
        assertEquals(Pair(2.5f, 415), k.calculateChestScore(k.successRate(1, 3), "2:00", 240)) // 166·2.5
        assertEquals(Pair(1f, 500), k.calculateChestScore(100f, "0:10", 0)) // kupa süresi yoksa çarpan 1
        assertEquals(Pair(1f, 250), k.calculateChestScore(50f, "0:10", -5))
        // ceil: 166 × 3.25 = 539.5 → 540
        assertEquals(540, k.calculateChestScore(k.successRate(1, 3), "1:00", 240).second)
    }

    @Test
    fun cesitliKurallar() {
        assertEquals(300L, k.beadAnimationDurationMs(2)) // güncel Android: sandıkta da 300 ms
        assertEquals(300L, k.beadAnimationDurationMs(0))
        assertEquals(300L, k.beadAnimationDurationMs(3))
        assertEquals(0.25f, k.progressEndWeight(0, 4))
        assertEquals(1f, k.progressEndWeight(3, 4))
        assertTrue(k.shouldPlayPerfectAnimation(3, 4, 4, 4))
        assertFalse(k.shouldPlayPerfectAnimation(2, 4, 4, 4))
        assertFalse(k.shouldPlayPerfectAnimation(3, 4, 3, 4))
        assertEquals("multiplication_table_3", k.multiplicationLayoutForDigit(3))
        assertEquals("multiplication_table_9", k.multiplicationLayoutForDigit(9))
        assertEquals("multiplication_table_1", k.multiplicationLayoutForDigit(0))
        assertEquals("multiplication_table_1", k.multiplicationLayoutForDigit(10))
    }

    @Test
    fun metniIkiyeBolme() {
        assertEquals("a b\nc d", k.splitTextEqually("a b c d"))
        assertEquals("a\nb c", k.splitTextEqually("a b c")) // tek sayıda kelimede fazlası ikinci satırda
        assertEquals("\ntek", k.splitTextEqually("tek"))
        assertEquals("\n", k.splitTextEqually(""))
    }

    @Test
    fun geriDonusBoncukSadelestirme() {
        assertEquals(
            listOf("rod4TopBead", "rod4BottomBead3", "rod2BottomBead2"),
            k.optimizeBeadIdsForReverse(
                listOf("rod4BottomBead1", "rod4TopBead", "rod4BottomBead3", "rod2BottomBead2", "rod4BottomBead2"),
            ),
        )
        assertEquals(listOf("rod0TopBead", "x"), k.optimizeBeadIdsForReverse(listOf("rod0TopBead", "x")))
        assertEquals(emptyList(), k.optimizeBeadIdsForReverse(emptyList()))
        assertEquals(listOf("rod1BottomBead4"), k.optimizeBeadIdsForReverse(listOf("rod1BottomBead4", "rod1BottomBead1")))
        // Çubuk 5+ gruplanmaz (yalnızca rod0..rod4 tanınır)
        assertEquals(listOf("rod5BottomBead1", "rod5BottomBead2"), k.optimizeBeadIdsForReverse(listOf("rod5BottomBead1", "rod5BottomBead2")))
    }
}

class AbacusLessonSessionTest {
    private fun op(a: Int?, o: String?, b: Int?) = AbacusOperation(a, o, b)

    @Test
    fun cevapDenetimi() {
        val s = AbacusLessonSession(
            listOf(op(3, "+", 4), op(9, "-", 4), op(3, "x", 4), op(8, "/", 2), op(null, "25", null), op(1, "?", 1), op(null, null, null)),
            lessonType = 0,
        )
        assertEquals(7, s.totalQuestions)
        assertTrue(s.stepAnswerAlgorithm(7)); assertEquals(7, s.answerNumber)
        assertEquals(0, s.controlNumber)
        assertFalse(s.stepAnswerAlgorithm(8))
        assertEquals("Senin cevabın 8", s.correctAnswerText)
        assertEquals("Doğru cevap 7", s.correctAnswerLabel)
        assertEquals(0, s.controlNumber)

        assertTrue(s.onContinue())
        assertTrue(s.stepAnswerAlgorithm(5))
        assertTrue(s.onContinue())
        assertTrue(s.stepAnswerAlgorithm(12))
        assertTrue(s.onContinue())
        // Android davranışı: bölmede beklenen cevap her zaman 1
        assertTrue(s.stepAnswerAlgorithm(1)); assertFalse(s.stepAnswerAlgorithm(4))
        assertTrue(s.onContinue())
        // İki sayı da yoksa işleç metni beklenen sayıdır
        assertTrue(s.stepAnswerAlgorithm(25))
        assertTrue(s.onContinue())
        // Bilinmeyen işleç → 0
        assertTrue(s.stepAnswerAlgorithm(0))
        assertTrue(s.onContinue())
        // Hepsi null → 0
        assertTrue(s.stepAnswerAlgorithm(0))
        assertFalse(s.onContinue()) // sorular bitti
        assertNull(s.currentOperation())
    }

    @Test
    fun androiddeCokenDurumlarIstisnaAtar() {
        assertFailsWith<NumberFormatException> { AbacusLessonSession(listOf(op(null, "abc", null)), 0).stepAnswerAlgorithm(0) }
        assertFailsWith<NullPointerException> { AbacusLessonSession(listOf(op(3, "+", null)), 0).stepAnswerAlgorithm(0) }
        assertFailsWith<NullPointerException> { AbacusLessonSession(listOf(op(null, "+", 4)), 0).stepAnswerAlgorithm(0) }
        // Boş listede yanlış döner
        assertFalse(AbacusLessonSession(emptyList(), 0).stepAnswerAlgorithm(0))
        // Son sorudan sonra gelen dokunuş (dizin sınır dışı): çökmez, yanlış döner
        val bitmis = AbacusLessonSession(listOf(op(3, "+", 4)), 0)
        bitmis.onContinue()
        assertFalse(bitmis.stepAnswerAlgorithm(7))
    }

    @Test
    fun kontrolDugmesiAkisi() {
        val s = AbacusLessonSession(listOf(op(3, "+", 4), op(5, "+", 5)), lessonType = 0)
        assertFalse(s.needsStartButton)
        assertFalse(s.isTimerVisible)
        assertTrue(s.start()); assertFalse(s.start())
        assertFalse(s.isTimerStarted)

        val r1 = s.onControlPressed(nowMs = 1000, isOnline = true, abacusValue = 7)
        assertEquals(AbacusLessonSession.ControlResult.Answered(isCorrect = true, timerStopped = false, progressEndWeight = 0.5f, playPerfectAnimation = false), r1)
        assertEquals(1, s.correctAnswer)
        // 100 ms dolmadan ikinci basış yok sayılır
        assertEquals(AbacusLessonSession.ControlResult.Ignored, s.onControlPressed(1099, true, 7))
        assertEquals(1, s.correctAnswer)
        assertTrue(s.onContinue())
        // Tam 100 ms: kabul edilir. Son soru + hepsi doğru → konfeti
        val r2 = s.onControlPressed(1100, true, 10)
        assertEquals(AbacusLessonSession.ControlResult.Answered(true, false, 1f, true), r2)
        assertEquals(2, s.correctAnswer)
        assertEquals(100f, s.currentSuccessRate())
        assertEquals(500, s.dersPuani())
        assertEquals(AbacusLessonOutcome.LESSON_RESULT, s.outcome(240))
    }

    @Test
    fun cevrimdisiykenCevapDenetlenmez() {
        val s = AbacusLessonSession(listOf(op(3, "+", 4)), lessonType = 0)
        assertEquals(AbacusLessonSession.ControlResult.Offline, s.onControlPressed(1000, false, 7))
        assertEquals(0, s.correctAnswer)
        // Çevrimdışı basış da 100 ms sayacını başlatır
        assertEquals(AbacusLessonSession.ControlResult.Ignored, s.onControlPressed(1050, true, 7))
        assertIs<AbacusLessonSession.ControlResult.Answered>(s.onControlPressed(1100, true, 7))
    }

    @Test
    fun yanlisCevapKonfetiVermez() {
        val s = AbacusLessonSession(listOf(op(1, "+", 1), op(2, "+", 2)), lessonType = 0)
        s.onControlPressed(1000, true, 0) // yanlış
        s.onContinue()
        val r = s.onControlPressed(2000, true, 4) as AbacusLessonSession.ControlResult.Answered
        assertTrue(r.isCorrect)
        assertFalse(r.playPerfectAnimation)
        assertEquals(50f, s.currentSuccessRate())
        assertEquals(AbacusLessonOutcome.LESSON_RESULT_FALSE, s.outcome(240)) // %50 < %75
    }

    @Test
    fun dersSonuYonlendirmesi() {
        fun oturum(dogru: Int, toplam: Int, tip: Int = 0): AbacusLessonSession {
            val s = AbacusLessonSession(List(toplam) { op(1, "+", 1) }, tip)
            s.start()
            var t = 1000L
            repeat(toplam) { i ->
                s.onControlPressed(t, true, if (i < dogru) 2 else 0)
                s.onContinue()
                t += 1000
            }
            return s
        }
        // Başarı < %75 → başarısız ekranı (güncel Android; eskiden %10)
        assertEquals(AbacusLessonOutcome.LESSON_RESULT_FALSE, oturum(0, 2).outcome(240))
        assertEquals(AbacusLessonOutcome.LESSON_RESULT_FALSE, oturum(1, 11).outcome(240)) // %9.09
        assertEquals(AbacusLessonOutcome.LESSON_RESULT_FALSE, oturum(1, 2).outcome(240)) // %50
        assertEquals(AbacusLessonOutcome.LESSON_RESULT_FALSE, oturum(74, 100).outcome(240)) // %74
        assertEquals(AbacusLessonOutcome.LESSON_RESULT, oturum(3, 4).outcome(240)) // tam %75
        assertEquals(AbacusLessonOutcome.LESSON_RESULT, oturum(1, 1).outcome(240)) // %100
        // Sandık: süre "0:00" (hiç tik yok) → çarpan 4
        assertEquals(AbacusLessonOutcome.CHEST_FAILURE, oturum(0, 4, tip = 2).outcome(240)) // 0 puan
        assertEquals(AbacusLessonOutcome.CHEST_FAILURE, oturum(1, 5, tip = 2).outcome(240)) // 100·4 = 400
        assertEquals(AbacusLessonOutcome.CHEST_RESULT, oturum(1, 4, tip = 2).outcome(240)) // 125·4 = 500
    }

    @Test
    fun sandikDersiZamanlayici() {
        val s = AbacusLessonSession(listOf(op(1, "+", 1), op(2, "+", 2)), lessonType = 2)
        assertTrue(s.needsStartButton)
        assertTrue(s.isTimerVisible)
        assertFalse(s.lessonStarted)
        assertEquals("0:00", s.currentTime)
        assertTrue(s.start())
        assertTrue(s.isTimerStarted)
        assertEquals("0:01", s.onTimerTick())
        repeat(64) { s.onTimerTick() }
        assertEquals("1:05", s.currentTime)
        assertEquals(65, s.seconds)

        val r1 = s.onControlPressed(1000, true, 2) as AbacusLessonSession.ControlResult.Answered
        assertFalse(r1.timerStopped) // son soru değil
        s.onContinue()
        val r2 = s.onControlPressed(2000, true, 4) as AbacusLessonSession.ControlResult.Answered
        assertTrue(r2.timerStopped) // son soruda süre hemen durur
        assertFalse(s.isTimerStarted)
        // %100, 65 sn, kupa süresi 240 → çarpan 4 − 195/240 = 3.1875 → ceil(1593.75) = 1594
        assertEquals(AbacusLessonOutcome.CHEST_RESULT, s.outcome(240))
        assertEquals(Pair(3.1875f, 1594), AbacusLessonRules.calculateChestScore(s.currentSuccessRate(), s.currentTime, 240))
    }
}

class AbacusPracticeLogicTest {
    @Test
    fun kontrol() {
        val p = AbacusPracticeLogic
        assertTrue(p.kontrol("12", "30", "+", 42).isCorrect)
        assertTrue(p.kontrol(" 7 ", "3", "-", 4).isCorrect)
        assertTrue(p.kontrol("6", "7", "x", 42).isCorrect)
        // Negatif sonuç abaküste gösterilemez
        val eksi = p.kontrol("5", "9", "-", 0)
        assertFalse(eksi.isCorrect)
        assertEquals(-4L, eksi.expected)
        assertEquals("Doğru cevap: -4", eksi.correctAnswerLabel)
        assertEquals("Senin cevabın: 0", eksi.correctAnswerText)
        // Boş / geçersiz giriş 0 sayılır
        assertTrue(p.kontrol("", "x", "+", 0).isCorrect)
        assertTrue(p.kontrol(null, null, "+", 0).isCorrect)
        assertTrue(p.kontrol("1.5", "4", "+", 4).isCorrect)
        // Taşma yok: Long ile hesaplanır
        assertEquals(9999800001L, p.kontrol("99999", "99999", "x", 99999).expected)
        assertEquals(4294967294L, p.kontrol("2147483647", "2147483647", "+", 0).expected)
        // Bilinmeyen işleç toplama sayılır
        assertEquals(10L, p.kontrol("8", "2", "/", 10).expected)
        assertEquals("+", p.DEFAULT_OPERATOR)
    }
}

class AbacusUiSettingsTest {
    @Test
    fun olcekVeBosluk() {
        val u = AbacusUiSettings
        assertEquals(0.5f, u.scaleFromProgress(0))
        assertEquals(2.0f, u.scaleFromProgress(100))
        assertEquals(1.25f, u.scaleFromProgress(50))
        assertEquals(33, u.progressFromScale(1.0f)) // Android'deki "varsayılan 33"
        assertEquals(0, u.progressFromScale(0.5f))
        assertEquals(0, u.progressFromScale(0.1f))
        assertEquals(100, u.progressFromScale(5f))
        assertEquals(30, u.progressFromMarginDp(45.9f, u.MAX_PROGRESS_BEAD_MARGIN_TOP))
        assertEquals(45, u.progressFromMarginDp(45.9f, u.MAX_PROGRESS_BEAD_MARGIN_BOTTOM))
        assertEquals(0, u.progressFromMarginDp(-3f, 100))

        assertEquals(82f, u.baseBottomMarginDp("rod0_bead_bottom1"))
        assertEquals(58f, u.baseBottomMarginDp("rod1_bead_bottom2"))
        assertEquals(34f, u.baseBottomMarginDp("rod2_bead_bottom3"))
        assertEquals(10f, u.baseBottomMarginDp("rod3_bead_bottom4"))
        assertEquals(0f, u.baseBottomMarginDp("rod3_bead_top"))
        assertEquals(184, u.beadBottomMarginPx("rod0_bead_bottom1", 10f, 2f))
        assertEquals(27, u.beadBottomMarginPx("rod0_bead_bottom4", 0f, 2.75f)) // 27.5 → 27
        assertEquals(13, u.beadTopMarginPx(5f, 2.75f)) // 13.75 → 13
        assertEquals("AbacusUISettings", u.PREFS_NAME)
        assertEquals("savedBeadMarginBottomDp", u.KEY_BEAD_MARGIN_BOTTOM_DP)
    }
}

class BlindingLessonLogicTest {
    private val k = BlindingLessonRules

    @Test
    fun kupaDeltasi() {
        val f = BlindingLessonFlags()
        assertEquals(10, k.cupDelta(f, true))
        assertEquals(-30, k.cupDelta(f, false))
        val ozel = BlindingLessonFlags(cupWinDelta = 5, cupLossDelta = 7)
        assertEquals(5, k.cupDelta(ozel, true))
        assertEquals(-7, k.cupDelta(ozel, false))

        assertEquals(CupDeltaTarget.PENDING_CUP_DELTA, k.cupDeltaTarget(f))
        assertEquals(CupDeltaTarget.PENDING_CUP_DELTA, k.cupDeltaTarget(BlindingLessonFlags(isBlinding = false)))
        assertEquals(CupDeltaTarget.PENDING_BLINDING_CUP_DELTA, k.cupDeltaTarget(BlindingLessonFlags(isBlinding = true)))
        assertEquals(CupDeltaTarget.PENDING_EXTRACTION_CUP_DELTA, k.cupDeltaTarget(BlindingLessonFlags(isExtraction = true)))
        assertEquals(CupDeltaTarget.PENDING_BLINDING_EXTRACTION_CUP_DELTA, k.cupDeltaTarget(BlindingLessonFlags(isExtraction = true, isBlinding = true)))
        assertEquals(CupDeltaTarget.PENDING_IMPACT_CUP_DELTA, k.cupDeltaTarget(BlindingLessonFlags(isMultiplication = true)))
        assertEquals(CupDeltaTarget.PENDING_BLINDING_IMPACT_CUP_DELTA, k.cupDeltaTarget(BlindingLessonFlags(isMultiplication = true, isBlinding = true)))
        // Çarpma, çıkarmadan önce gelir
        assertEquals(CupDeltaTarget.PENDING_IMPACT_CUP_DELTA, k.cupDeltaTarget(BlindingLessonFlags(isMultiplication = true, isExtraction = true)))
    }

    @Test
    fun gorunurlukKurallari() {
        assertTrue(k.isTimerVisible(2, 1))
        assertFalse(k.isTimerVisible(2, 4))
        assertFalse(k.isTimerVisible(2, 5))
        assertFalse(k.isTimerVisible(0, 1))
        assertTrue(k.shouldStartTimer(2, false))
        assertFalse(k.shouldStartTimer(2, true))
        assertFalse(k.shouldStartTimer(0, false))
        assertFalse(k.usesRulesTablePicker(3, false))
        assertTrue(k.usesRulesTablePicker(4, false))
        assertTrue(k.usesRulesTablePicker(9, false))
        assertFalse(k.usesRulesTablePicker(10, false))
        assertTrue(k.usesRulesTablePicker(1, true))
        assertTrue(k.isProgressBarHidden(9)); assertFalse(k.isProgressBarHidden(8))
        assertTrue(k.isHintHidden(4)); assertTrue(k.isHintHidden(9)); assertFalse(k.isHintHidden(3)); assertFalse(k.isHintHidden(10))
        assertTrue(k.shouldResetAbacusAfterAnswer(BlindingLessonFlags()))
        assertFalse(k.shouldResetAbacusAfterAnswer(BlindingLessonFlags(isBlinding = true)))
        assertFalse(k.shouldResetAbacusAfterAnswer(BlindingLessonFlags(isBlinding = false)))
        assertTrue(k.raceLessonPassed(3, 3)); assertFalse(k.raceLessonPassed(2, 3)); assertFalse(k.raceLessonPassed(0, 0))
        assertTrue(k.isDailyQuestionSessionPeriodValid("p1", "p1"))
        assertFalse(k.isDailyQuestionSessionPeriodValid("p1", "p2"))
        assertFalse(k.isDailyQuestionSessionPeriodValid(null, "p1"))
    }

    @Test
    fun yanlisCevapSonrasi() {
        val normal = BlindingLessonFlags()
        val yaris = BlindingLessonFlags(raceBusyLevel = 2)
        assertEquals(BlindingWrongAnswerNext.CUP_RESULT_FALSE, k.wrongAnswerNext(9, true, yaris))
        assertEquals(BlindingWrongAnswerNext.DAILY_QUESTION_PANEL, k.wrongAnswerNext(1, true, yaris))
        assertEquals(BlindingWrongAnswerNext.RACE_QUESTION_PANEL, k.wrongAnswerNext(1, false, yaris))
        assertEquals(BlindingWrongAnswerNext.RACE_QUESTION_PANEL, k.wrongAnswerNext(1, false, BlindingLessonFlags(raceBusyLevel = 0)))
        assertEquals(BlindingWrongAnswerNext.NEXT_QUESTION, k.wrongAnswerNext(1, false, normal))

        assertEquals(BlindingLessonRules.ResultFalseAction.CUP_USE_ENERGY_AND_CLOSE, k.lessonResultFalseAction(9, normal))
        assertEquals(BlindingLessonRules.ResultFalseAction.CLOSE, k.lessonResultFalseAction(1, BlindingLessonFlags(raceBusyLevel = 0)))
        assertEquals(BlindingLessonRules.ResultFalseAction.SHOW_RESULT_FALSE, k.lessonResultFalseAction(1, yaris))
        assertEquals(BlindingLessonRules.ResultFalseAction.SHOW_RESULT_FALSE, k.lessonResultFalseAction(1, normal))
    }

    @Test
    fun dersBitisYonlendirmesi() {
        fun bitir(gunluk: Boolean = false, bolum: Int = 1, f: BlindingLessonFlags = BlindingLessonFlags(), dogru: Int = 3, toplam: Int = 3, puan: Int = 0) =
            k.finishLessonAfterLastQuestion(gunluk, bolum, f, dogru, toplam, puan)

        assertEquals(BlindingLessonOutcome.DAILY_QUESTION_COMPLETE, bitir(gunluk = true, bolum = 9))
        assertEquals(BlindingLessonOutcome.CLOSE, bitir(bolum = 9, f = BlindingLessonFlags(type = 2)))
        // Yarış
        assertEquals(BlindingLessonOutcome.CLOSE, bitir(f = BlindingLessonFlags(raceBusyLevel = 0), puan = 2000))
        assertEquals(BlindingLessonOutcome.CHEST_RESULT, bitir(f = BlindingLessonFlags(raceBusyLevel = 2), puan = 500))
        assertEquals(BlindingLessonOutcome.CHEST_FAILURE, bitir(f = BlindingLessonFlags(raceBusyLevel = 2), puan = 499))
        // Yarış 4./5. bölümde de sandık dalına girer
        assertEquals(BlindingLessonOutcome.CHEST_RESULT, bitir(bolum = 4, f = BlindingLessonFlags(raceBusyLevel = 1), puan = 900))
        // Sandık (4. ve 5. bölüm dışında)
        assertEquals(BlindingLessonOutcome.CHEST_RESULT, bitir(bolum = 3, f = BlindingLessonFlags(type = 2), puan = 500))
        assertEquals(BlindingLessonOutcome.CHEST_FAILURE, bitir(bolum = 6, f = BlindingLessonFlags(type = 2), puan = 0))
        // 4./5. bölümde sandık tipi de başarı oranına göre değerlendirilir (güncel Android: %75 sınırı, eskiden %50)
        assertEquals(BlindingLessonOutcome.LESSON_RESULT_FALSE, bitir(bolum = 4, f = BlindingLessonFlags(type = 2), dogru = 1, toplam = 3, puan = 9999))
        assertEquals(BlindingLessonOutcome.LESSON_RESULT_FALSE, bitir(bolum = 5, f = BlindingLessonFlags(type = 2), dogru = 1, toplam = 2)) // %50
        assertEquals(BlindingLessonOutcome.LESSON_RESULT_FALSE, bitir(bolum = 4, dogru = 74, toplam = 100)) // %74
        assertEquals(BlindingLessonOutcome.LESSON_RESULT, bitir(bolum = 5, f = BlindingLessonFlags(type = 2), dogru = 3, toplam = 4)) // tam %75
        assertEquals(BlindingLessonOutcome.LESSON_RESULT_FALSE, bitir(bolum = 5, dogru = 0, toplam = 0))
        // Diğer: her zaman ders sonucu
        assertEquals(BlindingLessonOutcome.LESSON_RESULT, bitir(bolum = 1, dogru = 0, toplam = 5))
    }

    @Test
    fun diziGosterimi() {
        val p = BlindingSequencePlayback(timePeriod = null)
        val S = BlindingSequencePlayback.Step::class
        assertEquals(
            BlindingSequencePlayback.Step(display = "3", schedule = BlindingSequencePlayback.Timer.SHOW_NEXT_NUMBER, delayMs = 1000, disableControl = true),
            p.start(listOf(3, 5, 2)),
        )
        assertTrue(p.isShowingSequence)
        assertEquals(1, p.currentSequenceIndex)
        assertEquals(
            BlindingSequencePlayback.Step(display = "", schedule = BlindingSequencePlayback.Timer.SEQUENCE_REVEAL, delayMs = 200),
            p.onShowNextNumberStep(),
        )
        assertEquals(
            BlindingSequencePlayback.Step(display = "5", schedule = BlindingSequencePlayback.Timer.SHOW_NEXT_NUMBER, delayMs = 1000),
            p.onSequenceRevealStep(),
        )
        assertEquals(2, p.currentSequenceIndex)
        p.onShowNextNumberStep()
        // Son sayı: gösterilir ve gösterim biter
        assertEquals(BlindingSequencePlayback.Step(display = "2", finished = true), p.onSequenceRevealStep())
        assertFalse(p.isShowingSequence)
        assertEquals(3, p.currentSequenceIndex)
        // Bittikten sonra gelen zamanlayıcılar etkisizdir
        assertEquals(BlindingSequencePlayback.Step(), p.onShowNextNumberStep())
        assertEquals(BlindingSequencePlayback.Step(), p.onSequenceRevealStep())
        assertEquals("Step", S.simpleName)
    }

    @Test
    fun diziGosterimiSinirDurumlari() {
        // Tek sayı: gösterilir; sıradaki adımda gösterim biter
        val tek = BlindingSequencePlayback(timePeriod = 500)
        assertEquals(
            BlindingSequencePlayback.Step(display = "7", schedule = BlindingSequencePlayback.Timer.SHOW_NEXT_NUMBER, delayMs = 500, disableControl = true),
            tek.start(listOf(7)),
        )
        assertEquals(BlindingSequencePlayback.Step(finished = true), tek.onShowNextNumberStep())
        assertFalse(tek.isShowingSequence)

        // Boş dizi: hiçbir şey olmaz
        val bos = BlindingSequencePlayback(null)
        assertEquals(BlindingSequencePlayback.Step(), bos.start(emptyList()))
        assertFalse(bos.isShowingSequence)

        // Durdurulunca bekleyen adımlar etkisiz; yeniden başlatma baştan gösterir
        val p = BlindingSequencePlayback(null)
        p.start(listOf(1, 2, 3))
        p.onShowNextNumberStep()
        p.stop()
        assertEquals(BlindingSequencePlayback.Step(), p.onSequenceRevealStep())
        assertEquals("1", p.start(p.currentSequence).display)
        assertEquals(1, p.currentSequenceIndex)
        assertEquals(200L, BlindingLessonRules.SEQUENCE_RESTART_DELAY_MS)
    }

    @Test
    fun diziAtlamaDugmesi() {
        val p = BlindingSequencePlayback(timePeriod = null)
        // Gösterim yokken yok sayılır
        assertNull(p.skip())
        p.start(listOf(3, 5, 2))
        // Süre beklemeden sıradaki sayıya geçer: kendiliğinden geçişle aynı adım (sayı silinir, 200 ms sonra gelir)
        assertEquals(
            BlindingSequencePlayback.Step(display = "", schedule = BlindingSequencePlayback.Timer.SEQUENCE_REVEAL, delayMs = 200),
            p.skip(),
        )
        assertTrue(p.isBlankPending)
        // Boşluk sürerken ikinci basış yok sayılır (yoksa ekran boş kalırdı)
        assertNull(p.skip())
        assertEquals("5", p.onSequenceRevealStep().display)
        assertFalse(p.isBlankPending)
        // Sıradaki sayı geldi; yeniden atlanabilir
        assertEquals("", p.skip()!!.display)
        assertEquals(BlindingSequencePlayback.Step(display = "2", finished = true), p.onSequenceRevealStep())
        assertNull(p.skip()) // gösterim bitti
        // Durdurma bekleyen boşluğu temizler
        p.start(listOf(1, 2, 3)); p.skip(); p.stop()
        assertFalse(p.isBlankPending)
    }

    @Test
    fun kupaSonucuTekSefer() {
        val k = BlindingLessonRules
        assertTrue(k.shouldSubmitCupQuitResult(9, lessonStarted = true, cupResultSubmitted = false))
        assertFalse(k.shouldSubmitCupQuitResult(9, lessonStarted = true, cupResultSubmitted = true)) // cevabını verip çıkan terk sayılmaz
        assertFalse(k.shouldSubmitCupQuitResult(9, lessonStarted = false, cupResultSubmitted = false))
        assertFalse(k.shouldSubmitCupQuitResult(3, lessonStarted = true, cupResultSubmitted = false))
        val s = BlindingLessonSession(listOf(BlindingOperation.Sequence(listOf(1, 2))), BlindingLessonFlags())
        assertFalse(s.cupResultSubmitted)
        assertTrue(s.claimCupResult())
        assertFalse(s.claimCupResult())
        assertTrue(s.cupResultSubmitted)
        assertTrue(s.claimCupRaceLog()); assertFalse(s.claimCupRaceLog())
        assertTrue(s.claimLessonFinish()); assertFalse(s.claimLessonFinish())
        // Ölçüm adı: körleme birleşimleri önce
        assertEquals("blinding_multiplication", k.cupModeName(BlindingLessonFlags(isMultiplication = true, isBlinding = true)))
        assertEquals("multiplication", k.cupModeName(BlindingLessonFlags(isMultiplication = true)))
        assertEquals("blinding_extraction", k.cupModeName(BlindingLessonFlags(isExtraction = true, isBlinding = true)))
        assertEquals("extraction", k.cupModeName(BlindingLessonFlags(isExtraction = true)))
        assertEquals("blinding", k.cupModeName(BlindingLessonFlags(isBlinding = true)))
        assertEquals("normal", k.cupModeName(BlindingLessonFlags()))
    }

    @Test
    fun basariOraniTelemetriKapsami() {
        val k = BlindingLessonRules
        assertTrue(k.isLessonSuccessRateScope(false, 1, 0))
        assertTrue(k.isLessonSuccessRateScope(false, 3, 2))
        assertFalse(k.isLessonSuccessRateScope(true, 1, 0)) // günlük soru
        assertFalse(k.isLessonSuccessRateScope(false, 9, 0)) // kupa modu
        assertFalse(k.isLessonSuccessRateScope(false, 1, 1)) // ders/sandık dışı tip
        assertTrue(AbacusLessonRules.isLessonSuccessRateType(0))
        assertTrue(AbacusLessonRules.isLessonSuccessRateType(2))
        assertFalse(AbacusLessonRules.isLessonSuccessRateType(3))
    }

    @Test
    fun diziSorusuCevabi() {
        val dizi = listOf(BlindingOperation.Sequence(listOf(3, 5, 2)))
        // Kör (klavye) cevap
        val kor = BlindingLessonSession(dizi, BlindingLessonFlags(isBlinding = true))
        assertTrue(kor.stepAnswerAlgorithm("10", abacusValue = 0, currentSequenceIndex = 3))
        assertEquals(10, kor.answerNumber)
        assertFalse(kor.stepAnswerAlgorithm("9", 10, 3)) // abaküs değeri önemsiz
        assertEquals("Doğru cevap 10", kor.correctAnswerLabel)
        assertEquals("Senin cevabın 9", kor.correctAnswerText)
        assertEquals(0, kor.controlNumber)
        assertFalse(kor.stepAnswerAlgorithm("", 10, 3))
        assertEquals("Senin cevabın 0", kor.correctAnswerText)
        assertFalse(kor.stepAnswerAlgorithm("abc", 10, 3))
        assertFalse(kor.stepAnswerAlgorithm("10", 10, 0)) // dizi hiç gösterilmediyse yanlış

        // Abaküsle cevap (isBlinding == null): giriş boş olsa da abaküs okunur
        val abakus = BlindingLessonSession(dizi, BlindingLessonFlags())
        assertTrue(abakus.stepAnswerAlgorithm("", 10, 3))
        assertFalse(abakus.stepAnswerAlgorithm("10", 9, 3)) // giriş metni önemsiz
        assertEquals("Senin cevabın 9", abakus.correctAnswerText)

        // Android davranışı: isBlinding == false ise hiçbir kaynak okunmaz; 0 ile karşılaştırılır
        val hicbiri = BlindingLessonSession(dizi, BlindingLessonFlags(isBlinding = false))
        assertFalse(hicbiri.stepAnswerAlgorithm("10", 10, 3))
        assertEquals("Senin cevabın 0", hicbiri.correctAnswerText)
        assertFalse(hicbiri.stepAnswerAlgorithm("", 10, 3))
        val sifirToplam = BlindingLessonSession(listOf(BlindingOperation.Sequence(listOf(4, -4))), BlindingLessonFlags(isBlinding = false))
        assertTrue(sifirToplam.stepAnswerAlgorithm("x", 99, 1))
    }

    @Test
    fun carpimSorusuCevabi() {
        val soru = listOf(BlindingOperation.Math(3, "x", 4))
        // Abaküslü çarpım
        val abakus = BlindingLessonSession(soru, BlindingLessonFlags(isMultiplication = true))
        assertTrue(abakus.stepAnswerAlgorithm("", 12, 0))
        assertFalse(abakus.stepAnswerAlgorithm("12", 11, 0))
        assertEquals("Doğru cevap 12", abakus.correctAnswerLabel)
        assertEquals("Senin cevabın 11", abakus.correctAnswerText)
        // isBlinding = false de abaküs yoluna girer (koşul: != true)
        assertTrue(BlindingLessonSession(soru, BlindingLessonFlags(isMultiplication = true, isBlinding = false)).stepAnswerAlgorithm("", 12, 0))

        // Kör çarpım: klavye
        val kor = BlindingLessonSession(soru, BlindingLessonFlags(isMultiplication = true, isBlinding = true))
        assertTrue(kor.stepAnswerAlgorithm("12", 0, 0))
        assertFalse(kor.stepAnswerAlgorithm("", 12, 0))
        assertFalse(kor.stepAnswerAlgorithm("1x", 12, 0))
        assertFalse(kor.stepAnswerAlgorithm("13", 12, 0))
        assertEquals("Senin cevabın 13", kor.correctAnswerText)
        // blindingMultiplication tek başına: klavye yolu
        val kor2 = BlindingLessonSession(soru, BlindingLessonFlags(blindingMultiplication = true))
        assertTrue(kor2.stepAnswerAlgorithm("12", 0, 0))
        assertFalse(kor2.stepAnswerAlgorithm("", 12, 0))

        // İlk sayı null → beklenen 0; yalnızca ikinci null → Android'de çöker
        val ilkNull = BlindingLessonSession(listOf(BlindingOperation.Math(null, "x", 4)), BlindingLessonFlags(isMultiplication = true))
        assertTrue(ilkNull.stepAnswerAlgorithm("", 0, 0))
        val ikinciNull = BlindingLessonSession(listOf(BlindingOperation.Math(3, "x", null)), BlindingLessonFlags(isMultiplication = true))
        assertFailsWith<NullPointerException> { ikinciNull.stepAnswerAlgorithm("", 0, 0) }
    }

    @Test
    fun digerSoruTurleri() {
        // Çarpım olmayan MathOperation her zaman yanlış; panel metinleri güncellenmez
        val normal = BlindingLessonSession(listOf(BlindingOperation.Math(3, "+", 4)), BlindingLessonFlags(isBlinding = true))
        assertFalse(normal.stepAnswerAlgorithm("7", 7, 1))
        assertEquals(0, normal.answerNumber)
        assertEquals("", normal.correctAnswerLabel)

        assertFalse(BlindingLessonSession(listOf(BlindingOperation.Other), BlindingLessonFlags()).stepAnswerAlgorithm("1", 1, 1))
        assertFalse(BlindingLessonSession(emptyList(), BlindingLessonFlags()).stepAnswerAlgorithm("1", 1, 1))

        // İkinci soruya geçiş
        val s = BlindingLessonSession(
            listOf(BlindingOperation.Sequence(listOf(1, 2)), BlindingOperation.Sequence(listOf(10, 20))),
            BlindingLessonFlags(isBlinding = true),
        )
        assertEquals(2, s.totalQuestions)
        assertTrue(s.stepAnswerAlgorithm("3", 0, 2))
        s.currentIndex++
        assertTrue(s.stepAnswerAlgorithm("30", 0, 2))
        s.clearControlNumber()
        assertEquals(0, s.controlNumber)
    }
}

// SobiDailyTest'in saf kısmı ve SobiTestOturumu (Android'de bu ikisinin birim testi yok; beklentiler
// Android kaynağının davranışından: SobiDailyTest.kt, SobiTestSheet.fill, HomeworkPlayerFragment.finishSobiTest).
package com.sorobit.shared.sobitest

import com.sorobit.shared.ders.GlobalLessonData
import com.sorobit.shared.ders.LessonItem
import com.sorobit.shared.oyun.InMemoryKeyValueStore
import com.sorobit.shared.sobianaliz.SobiMetinCozucu
import com.sorobit.shared.sobianaliz.SobiRuleStatsStore
import com.sorobit.shared.sobitest.SobiTestPlanner.RuleScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SobiDailyTestTest {

    /** Anahtarı ve argümanları yazan çözücü: "ad(a|b)". */
    private val metin = object : SobiMetinCozucu {
        override fun metin(anahtar: String, argumanlar: List<Any>): String =
            if (argumanlar.isEmpty()) anahtar else anahtar + "(" + argumanlar.joinToString("|") + ")"
    }

    private val add = listOf(RuleScore("add_direct", 40, 2), RuleScore("add_five", 30, 3), RuleScore("add_ten", 12, 10))

    private fun item(stableId: String, header: Boolean = false, finished: Boolean = false, currentStep: Int = 0, steps: List<Boolean> = listOf(false, false, false)) =
        LessonItem(
            stableId = stableId, type = if (header) LessonItem.TYPE_HEADER else 0, title = stableId, offset = 0,
            isCompleted = false, stepCount = steps.size, currentStep = currentStep, stepCompletionStatus = steps, stepIsFinish = finished,
        )

    @Test
    fun gun_turkiye_saatiyle() {
        // 2026-11-28 20:59:59 UTC = 23:59:59 TSİ; 21:00:00 UTC = ertesi gün 00:00.
        val gece = 1795899599000L
        assertEquals("2026-11-28", SobiDailyTest.today(gece))
        assertEquals("2026-11-29", SobiDailyTest.today(gece + 1000L))
    }

    @Test
    fun tohum_android_ile_ayni() {
        // String.hashCode her platformda Java'nınki (31·h + c) olmalı: tohum Android'le aynı soruları verir.
        val gun = "2026-11-28"
        var h = 0
        for (c in gun) h = 31 * h + c.code
        assertEquals(h, gun.hashCode())
        assertEquals(h * 31 + 2 * 7919 - 1, SobiDailyTest.seed(gun, 2, -1))
    }

    @Test
    fun gunluk_sayac_ve_ek_test_bedeli() {
        val store = SobiDailyTestStore(InMemoryKeyValueStore())
        val gun = "2026-11-28"
        assertEquals(0, store.testsToday(gun))
        assertFalse(store.isDoneToday(gun))
        // Günün ilk testi ücretsiz.
        assertEquals(0, store.extraTestCost(gun, isPro = false, isApprovedTeacher = false))
        store.markDone(gun)
        assertTrue(store.isDoneToday(gun))
        // Ek test: 3 anahtar; Pro ve onaylı öğretmen ücretsiz.
        assertEquals(SobiDailyTest.EXTRA_TEST_KEYS, store.extraTestCost(gun, isPro = false, isApprovedTeacher = false))
        assertEquals(0, store.extraTestCost(gun, isPro = true, isApprovedTeacher = false))
        assertEquals(0, store.extraTestCost(gun, isPro = false, isApprovedTeacher = true))
        // Ödendi ama yarıda bırakıldı: aynı sıra yeniden ödenmez.
        store.markExtraPaid(gun)
        assertEquals(0, store.extraTestCost(gun, isPro = false, isApprovedTeacher = false))
        store.markDone(gun)
        assertEquals(2, store.testsToday(gun))
        assertEquals(SobiDailyTest.EXTRA_TEST_KEYS, store.extraTestCost(gun, isPro = false, isApprovedTeacher = false))
        // Ertesi gün sayaç sıfır.
        assertEquals(0, store.testsToday("2026-11-29"))
        assertEquals(0, store.extraTestCost("2026-11-29", isPro = false, isApprovedTeacher = false))
    }

    @Test
    fun balon_gunde_bir_kez() {
        val store = SobiDailyTestStore(InMemoryKeyValueStore())
        assertTrue(store.shouldNudgeToday("2026-11-28"))
        assertFalse(store.shouldNudgeToday("2026-11-28"))
        assertTrue(store.shouldNudgeToday("2026-11-29"))
    }

    @Test
    fun acilma_kurali_ve_onbellek() {
        val store = SobiDailyTestStore(InMemoryKeyValueStore())
        val kapali = (0..8).map { item("p1_$it") }
        assertFalse(store.checkUnlocked("u1", kapali))
        val acik = kapali.mapIndexed { i, it -> if (i == SobiDailyTest.UNLOCK_INDEX) item("p1_$i", finished = true) else it }
        assertTrue(store.checkUnlocked("u1", acik))
        // Önbellekte: liste ne olursa olsun açık; başka hesapta değil.
        assertTrue(store.checkUnlocked("u1", emptyList()))
        assertFalse(store.checkUnlocked("u2", kapali))
        assertFalse(store.checkUnlocked(null, emptyList()))
    }

    @Test
    fun sablonda_part1_altinci_satir_var_ve_bitmemis() {
        val items = GlobalLessonData.createLessonItems(SobiDailyTest.UNLOCK_PART)
        assertTrue(items.size > SobiDailyTest.UNLOCK_INDEX)
        assertFalse(SobiDailyTest.isUnlockedFrom(items))
    }

    @Test
    fun sablondaki_bolum_basliklari_planlayicida_tanimli() {
        // LESSON_SECTIONS'taki her başlık kimliği şablonda gerçekten bir başlık (eşleme kopmasın).
        val headers = SobiDailyTest.LESSON_PARTS.flatMap { p ->
            GlobalLessonData.createLessonItems(p).filter { it.type == LessonItem.TYPE_HEADER }.map { it.stableId }
        }.toSet()
        for (id in SobiTestPlanner.LESSON_SECTIONS.keys) assertTrue(id in headers, "şablonda yok: $id")
        // Şablonda hiçbir ders oynanmamış: görülmüş kural yok.
        val seen = SobiDailyTest.LESSON_PARTS.flatMap { SobiDailyTest.seenFromItems(GlobalLessonData.createLessonItems(it)) }
        assertTrue(seen.isEmpty(), "$seen")
    }

    @Test
    fun gorulen_kurallar_bir_adim_yeter_ve_onbellek() {
        val part4 = listOf(
            item("p4_i00_kuralsiz_toplama", header = true), item("p4_i01", currentStep = 2),
            item("p4_i05_5_lik_toplama", header = true), item("p4_i06", steps = listOf(true, false, false)),
            item("p4_i10_10_luk_toplama_1_2_3_4_5", header = true), item("p4_i11", currentStep = 1),
        )
        assertEquals(setOf("add_direct", "add_five"), SobiDailyTest.seenFromItems(part4))
        val store = SobiDailyTestStore(InMemoryKeyValueStore())
        assertEquals(setOf("add_direct", "add_five"), store.lessonSeen("u1", listOf(part4)))
        // Önbellekten birikir.
        assertEquals(setOf("add_direct", "add_five"), store.lessonSeen("u1", emptyList()))
        assertEquals(emptySet(), store.lessonSeen("u2", emptyList()))
    }

    @Test
    fun seviyeler_cihaz_ve_sunucu() {
        val store = SobiDailyTestStore(InMemoryKeyValueStore())
        store.saveLevels(mapOf("add_ten" to 5, "m_add_five" to 2, "bilinmeyen" to 9))
        assertEquals(mapOf("add_ten" to 5, "m_add_five" to 2), store.localLevels())
        val server = mapOf("add_ten" to 7, "m_add_five" to 1, "sub_ten" to 3)
        assertEquals(mapOf("add_ten" to 7, "m_add_five" to 2, "sub_ten" to 3), SobiDailyTest.mergeLevels(store.localLevels(), server))
        assertEquals(mapOf("m_add_five" to 2), SobiDailyTest.unsentLevels(store.localLevels(), server))
    }

    @Test
    fun sunucu_belgesi() {
        val doc: Map<String, Any?> = mapOf(
            "recent" to mapOf("add_ten" to listOf(mapOf("c" to 3, "w" to 1), mapOf("c" to 2L, "w" to 2))),
            "tally" to mapOf("add_ten" to mapOf("c" to 99, "w" to 99), "sub_five" to mapOf("c" to 4, "w" to 6)),
            "sobiLevel" to mapOf("add_ten" to 4, "sub_five" to 0, "m_add_ten" to 21, "sub_ten" to 20L),
        )
        val scores = SobiDailyTest.scoresOf(doc).associateBy { it.key }
        assertEquals(RuleScore("add_ten", 5, 3), scores["add_ten"])
        assertEquals(RuleScore("sub_five", 4, 6), scores["sub_five"])
        assertEquals(mapOf("add_ten" to 4, "sub_ten" to 20), SobiDailyTest.levelsOf(doc))
        assertTrue(SobiDailyTest.scoresOf(null).isEmpty())
        // Sunucuda yeterli abaküslü sayım yoksa bu cihazınki.
        val local = listOf(RuleScore("add_five", 10, 0))
        assertEquals(local, SobiDailyTest.pickScores(listOf(RuleScore("m_add_ten", 30, 0), RuleScore("add_ten", 5, 4)), local))
        assertEquals(add, SobiDailyTest.pickScores(add, local))
    }

    @Test
    fun metinler() {
        assertEquals("2,4", SobiDailyTest.secondsText(2400))
        assertEquals("3", SobiDailyTest.secondsText(3000))
        assertEquals("1", SobiDailyTest.secondsText(1000))
        assertEquals("sobi_test_spec(2|3|2,4)", SobiDailyTest.specText(5, metin))
        assertEquals("sobi_test_rule_add_ten", SobiDailyTest.ruleLabel("add_ten", metin))
        assertEquals("sobi_test_rule_mental(sobi_test_rule_sub_five)", SobiDailyTest.ruleLabel("m_sub_five", metin))
        assertEquals("sobi_test_rule_mul(2|1)", SobiDailyTest.ruleLabel("mulA_2x1", metin))
        assertEquals("sobi_test_rule_mental(sobi_test_rule_mul(2|2))", SobiDailyTest.ruleLabel("mulM_2x2", metin))
    }

    @Test
    fun zorluk_kartlari() {
        fun choice(levels: Map<String, Int>) = SobiTestPlanner.choose(add, levels)!!
        assertEquals(listOf(-1, 0, 1), SobiDailyTest.difficultyOptions(choice(mapOf("add_ten" to 5))).map { it.offset })
        assertEquals(listOf(4, 5, 6), SobiDailyTest.difficultyOptions(choice(mapOf("add_ten" to 5))).map { it.level })
        // 1. seviyede Kolay, 20'de Zor yok.
        assertEquals(listOf(0, 1), SobiDailyTest.difficultyOptions(choice(mapOf("add_ten" to 1))).map { it.offset })
        assertEquals(listOf(-1, 0), SobiDailyTest.difficultyOptions(choice(mapOf("add_ten" to 20))).map { it.offset })
        // Kayıtlı seviye yok: %54 → 1. seviye, ilk kart "Benim seviyem".
        assertEquals("sobi_test_diff_mine", SobiDailyTest.difficultyOptions(choice(emptyMap()))[0].titleResource)
        // Zayıf kuralların hepsi çarpma: seçim yok.
        val mulOnly = SobiTestPlanner.choose(listOf(RuleScore("add_direct", 40, 0)) + RuleScore("mulA_2x1", 2, 13), emptyMap(), setOf("mulA_2x1"))!!
        assertEquals(listOf("mulA_2x1"), mulOnly.weak.map { it.key })
        assertTrue(SobiDailyTest.difficultyOptions(mulOnly).isEmpty())
        val rows = SobiDailyTest.ruleRows(mulOnly, metin)
        assertNull(rows.first().level)
    }

    @Test
    fun plan_guvenli_cagri() {
        assertNull(SobiDailyTest.plan(SobiDailyTest.Info(emptyList(), emptyMap(), emptySet()), 0, 1))
        val plan = SobiDailyTest.plan(SobiDailyTest.Info(add, emptyMap(), emptySet()), 0, SobiDailyTest.seed("2026-11-28", 0, 0))
        assertNotNull(plan)
        assertNotNull(SobiDailyTest.choose(SobiDailyTest.Info(add, emptyMap(), emptySet())))
    }

    // ── Oturum ──────────────────────────────────────────────────────────────

    @Test
    fun oturum_hepsi_dogru_seviye_atlar() {
        val plan = SobiTestPlanner.plan(add, mapOf("add_ten" to 5), 0, seed = 11)!!
        val stats = SobiRuleStatsStore(InMemoryKeyValueStore())
        val o = SobiTestOturumu(plan, stats)
        for (i in 0 until o.questionCount) {
            val nums = o.numbers(i)
            // Adım adım çözülmüş abaküs kaydı: her ara toplam, sayılar göründükten sonra.
            val times = ArrayList<Long>()
            val values = ArrayList<Long>()
            val shown = ArrayList<Long>()
            var v = 0L
            times += 0L; values += 0L
            nums.forEachIndexed { k, n ->
                shown += (k + 1) * 1000L
                v = if (k == 0) n else if (o.isSubtraction(i)) v - n else v + n
                times += (k + 1) * 1000L + 500L; values += v
            }
            assertTrue(o.submit(i, o.answer(i), times, values, shown))
        }
        assertEquals(o.questionCount, o.correctCount)
        val sonuc = o.finish(metin)
        assertTrue(sonuc.perfect)
        // 10'luk toplama 5 → 6 (oynanan 5, %100).
        assertEquals(6, sonuc.newLevels["add_ten"])
        assertTrue(sonuc.message.contains("sobi_test_level_up(5|6)"), sonuc.message)
        assertTrue(sonuc.message.endsWith("sobi_test_cheer_good"), sonuc.message)
        // Kural hafızasına da yazıldı.
        assertTrue(stats.snapshot(withMental = false).any { it.key == "add_ten" && it.correct > 0 })
    }

    @Test
    fun oturum_hepsi_yanlis_seviye_duser() {
        val plan = SobiTestPlanner.plan(add, mapOf("add_ten" to 5), 0, seed = 11, lessonSeen = setOf("add_direct", "add_five"))!!
        val o = SobiTestOturumu(plan, null)
        for (i in 0 until o.questionCount) {
            val nums = o.numbers(i)
            val wrong = o.answer(i) + 1
            if (o.withAbacus(i)) {
                // İlk sayı yazılmış, sonra tek seferde yanlış cevap: "adım adım değil" → işlem sayılmaz.
                o.submit(i, wrong, listOf(0L, 1500L, 9000L), listOf(0L, nums[0], wrong), listOf(1000L, 2000L, 3000L))
            } else {
                o.submit(i, wrong, emptyList(), emptyList(), emptyList())
            }
            assertTrue(o.canReview(i, false) == !o.isMultiplication(i))
        }
        assertEquals(0, o.correctCount)
        val sonuc = o.finish(metin)
        assertFalse(sonuc.perfect)
        // Zihinden 5'lik: soru başına, %0 → seviye 1'de kalır (oynanan 1 − 1 = 0 → 1).
        assertEquals(1, sonuc.newLevels["m_add_five"])
        assertTrue(sonuc.message.contains("sobi_test_rule_line_questions(sobi_test_rule_mental(sobi_test_rule_add_five)"), sonuc.message)
        assertTrue(sonuc.message.endsWith("sobi_test_cheer_try"), sonuc.message)
    }

    @Test
    fun oturum_etiketler_ve_inceleme() {
        val scores = listOf(RuleScore("add_direct", 50, 0), RuleScore("sub_direct", 30, 1), RuleScore("sub_five", 20, 2), RuleScore("sub_ten", 15, 9))
        val plan = SobiTestPlanner.plan(scores, mapOf("sub_ten" to 3), 0, seed = 5)!!
        val o = SobiTestOturumu(plan, null)
        val i = (0 until o.questionCount).first { o.isSubtraction(it) }
        val nums = o.numbers(i)
        val labels = o.flashLabels(i)
        assertEquals(nums[0].toString(), labels[0])
        assertTrue(labels.drop(1).all { it.startsWith("−") })
        assertEquals(o.questionLine(i) + " = " + o.answer(i), o.fullLine(i))
        assertEquals(o.answer(i).toString(), o.revealText(i))
        assertEquals(nums.drop(1).map { -it.toInt() }, o.reviewNumbers(i).drop(1))
        // Her sayı göründüğünde işaret; işaret sayısı sayı adediyle aynı.
        val shown = nums.indices.map { (it + 1) * 1000L }
        val r = o.review(i, listOf(0L, 1500L, 2500L), listOf(0L, nums[0], nums[0] - nums[1]), shown)
        assertEquals(nums.size, r.marks.size)
        assertEquals(0, r.values[r.marks[0]])
    }
}

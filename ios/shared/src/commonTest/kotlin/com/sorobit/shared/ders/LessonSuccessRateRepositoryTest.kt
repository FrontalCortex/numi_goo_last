package com.sorobit.shared.ders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LessonSuccessRateRepositoryTest {

    private val R = LessonSuccessRateRepository

    private fun yakin(beklenen: Float, gercek: Any?) {
        assertTrue(gercek is Float, "Float bekleniyordu: $gercek")
        assertEquals(beklenen, gercek as Float, 0.0001f)
    }

    @Test
    fun yollarVeAlanAdlari() {
        assertEquals("successRate/lessonSuccessRate/part3", R.partCollectionPath(3))
        assertEquals("successRate/lessonSuccessRate/part3/12", R.itemPath(3, 12))
        assertEquals("successRate/lessonSuccessRate/part3/12/steps/2", R.stepPath(3, 12, 2))
        // Adım en az 1'e yuvarlanır
        assertEquals("successRate/lessonSuccessRate/part3/12/steps/1", R.stepPath(3, 12, 0))
        assertEquals("successRate/lessonSuccessRate/part3/12/steps/1", R.stepPath(3, 12, -4))
        assertEquals("users/U/lessonSuccessRateState/3_12_2", R.userStepStatePath("U", 3, 12, 2))
        assertEquals("users/U/lessonSuccessRateState/3_12_1", R.userStepStatePath("U", 3, 12, 0))

        assertEquals("attempted", R.FIELD_ATTEMPTED)
        assertEquals("passed", R.FIELD_PASSED)
        assertEquals("failStreak", R.FIELD_FAIL_STREAK)
        assertEquals("attemptFailTotal", R.FIELD_ATTEMPT_FAIL_TOTAL)
        assertEquals("attemptUserCount", R.FIELD_ATTEMPT_USER_COUNT)
        assertEquals("passUserCount", R.FIELD_STEP_PASS_USER_COUNT)
        assertEquals("passOnFirstTry", R.FIELD_PASS_FIRST_TRY)
        assertEquals("passOnSecondTry", R.FIELD_PASS_SECOND_TRY)
        assertEquals("passOnThirdPlusTry", R.FIELD_PASS_THIRD_PLUS_TRY)
        assertEquals("successRatePercent", R.FIELD_SUCCESS_RATE_PERCENT)
        assertEquals("firstTrySuccessRatePercent", R.FIELD_FIRST_TRY_SUCCESS_RATE_PERCENT)
        assertEquals("passUserCount", R.FIELD_ITEM_PASS_USER_COUNT)
        assertEquals("totalCompletionCount", R.FIELD_TOTAL_COMPLETION_COUNT)
        assertEquals("avgReplayCount", R.FIELD_AVG_REPLAY_COUNT)
        assertEquals("oneStarCount", R.FIELD_ONE_STAR_COUNT)
        assertEquals("twoStarCount", R.FIELD_TWO_STAR_COUNT)
        assertEquals("threeStarCount", R.FIELD_THREE_STAR_COUNT)
    }

    @Test
    fun isTrackedPart() {
        assertFalse(R.isTrackedPart(0))
        assertTrue(R.isTrackedPart(1))
        assertTrue(R.isTrackedPart(8))
        assertFalse(R.isTrackedPart(9))
    }

    @Test
    fun oranlar() {
        assertEquals(0f, R.successRatePercent(5, 0))
        assertEquals(0f, R.successRatePercent(5, -1))
        assertEquals(50f, R.successRatePercent(1, 2))
        assertEquals(100f, R.successRatePercent(4, 4))
        assertEquals(33.333332f, R.successRatePercent(1, 3), 0.0001f)
        assertEquals(0f, R.avgReplayCount(7, 0))
        assertEquals(1f, R.avgReplayCount(4, 4))
        assertEquals(2.5f, R.avgReplayCount(5, 2))
    }

    @Test
    fun recordFail_ilkDeneme() {
        val w = R.recordFail(emptyMap(), emptyMap())
        assertNotNull(w)
        assertEquals(mapOf<String, Any>("attempted" to true, "failStreak" to 1L), w.userState)
        assertEquals(setOf("attemptFailTotal", "attemptUserCount", "successRatePercent", "firstTrySuccessRatePercent"), w.stepDoc.keys)
        assertEquals(1L, w.stepDoc["attemptFailTotal"])
        assertEquals(1L, w.stepDoc["attemptUserCount"])
        yakin(0f, w.stepDoc["successRatePercent"])
        yakin(0f, w.stepDoc["firstTrySuccessRatePercent"])
    }

    @Test
    fun recordFail_dahaOnceDenemisKullanici() {
        val w = R.recordFail(
            userSnap = mapOf("attempted" to true, "failStreak" to 2L),
            globalSnap = mapOf(
                "attemptFailTotal" to 10L, "attemptUserCount" to 4L, "passUserCount" to 3L, "passOnFirstTry" to 1L,
            ),
        )
        assertNotNull(w)
        assertEquals(3L, w.userState["failStreak"])
        assertEquals(11L, w.stepDoc["attemptFailTotal"])
        // Kullanıcı zaten sayılmış → attemptUserCount artmaz
        assertEquals(4L, w.stepDoc["attemptUserCount"])
        yakin(75f, w.stepDoc["successRatePercent"])
        yakin(25f, w.stepDoc["firstTrySuccessRatePercent"])
    }

    @Test
    fun recordFail_gecmisKullaniciIcinYazmaYok() {
        assertNull(R.recordFail(mapOf("passed" to true, "attempted" to true), mapOf("attemptFailTotal" to 5L)))
    }

    @Test
    fun recordPass_ilkDenemedeGecis() {
        val w = R.recordPass(emptyMap(), emptyMap())
        assertNotNull(w)
        assertEquals(mapOf<String, Any>("attempted" to true, "passed" to true), w.userState)
        assertEquals(
            setOf(
                "attemptUserCount", "passUserCount", "passOnFirstTry", "passOnSecondTry", "passOnThirdPlusTry",
                "successRatePercent", "firstTrySuccessRatePercent",
            ),
            w.stepDoc.keys,
        )
        assertEquals(1L, w.stepDoc["attemptUserCount"])
        assertEquals(1L, w.stepDoc["passUserCount"])
        assertEquals(1L, w.stepDoc["passOnFirstTry"])
        assertEquals(0L, w.stepDoc["passOnSecondTry"])
        assertEquals(0L, w.stepDoc["passOnThirdPlusTry"])
        yakin(100f, w.stepDoc["successRatePercent"])
        yakin(100f, w.stepDoc["firstTrySuccessRatePercent"])
    }

    @Test
    fun recordPass_ikinciVeUcuncuDeneme() {
        val global = mapOf<String, Any?>(
            "attemptUserCount" to 4L, "passUserCount" to 1L,
            "passOnFirstTry" to 1L, "passOnSecondTry" to 0L, "passOnThirdPlusTry" to 0L,
        )
        // 1 başarısızlıktan sonra → ikinci deneme kovası; kullanıcı zaten denemiş sayılı
        val w2 = R.recordPass(mapOf("attempted" to true, "failStreak" to 1L), global)!!
        assertEquals(4L, w2.stepDoc["attemptUserCount"])
        assertEquals(2L, w2.stepDoc["passUserCount"])
        assertEquals(1L, w2.stepDoc["passOnFirstTry"])
        assertEquals(1L, w2.stepDoc["passOnSecondTry"])
        assertEquals(0L, w2.stepDoc["passOnThirdPlusTry"])
        yakin(50f, w2.stepDoc["successRatePercent"])
        yakin(25f, w2.stepDoc["firstTrySuccessRatePercent"])
        // 2+ başarısızlık → üçüncü+ kovası
        val w3 = R.recordPass(mapOf("attempted" to true, "failStreak" to 5L), global)!!
        assertEquals(1L, w3.stepDoc["passOnFirstTry"])
        assertEquals(0L, w3.stepDoc["passOnSecondTry"])
        assertEquals(1L, w3.stepDoc["passOnThirdPlusTry"])
        // Firestore sayıları Double gelse de çalışır
        val w4 = R.recordPass(mapOf("failStreak" to 1.0), mapOf("attemptUserCount" to 4.0))!!
        assertEquals(5L, w4.stepDoc["attemptUserCount"])
        assertEquals(1L, w4.stepDoc["passOnSecondTry"])
    }

    @Test
    fun recordPass_gecmisKullaniciIcinYazmaYok() {
        assertNull(R.recordPass(mapOf("passed" to true), emptyMap()))
        // passed alanı Boolean değilse "geçmemiş" sayılır
        assertNotNull(R.recordPass(mapOf("passed" to "true"), emptyMap()))
    }

    @Test
    fun recordItemFirstFinish_ders() {
        val u = R.recordItemFirstFinish(emptyMap())
        assertEquals(setOf("totalCompletionCount", "passUserCount", "avgReplayCount"), u.keys)
        assertEquals(1L, u["totalCompletionCount"])
        assertEquals(1L, u["passUserCount"])
        yakin(1f, u["avgReplayCount"])

        val u2 = R.recordItemFirstFinish(mapOf("totalCompletionCount" to 9L, "passUserCount" to 3L))
        assertEquals(10L, u2["totalCompletionCount"])
        assertEquals(4L, u2["passUserCount"])
        yakin(2.5f, u2["avgReplayCount"])
    }

    @Test
    fun recordItemFirstFinish_sandikYildizDagilimi() {
        val snap = mapOf<String, Any?>("oneStarCount" to 5L, "twoStarCount" to 2L, "threeStarCount" to 1L)
        fun yildiz(n: Int) = R.recordItemFirstFinish(snap, n).let {
            listOf(it["oneStarCount"], it["twoStarCount"], it["threeStarCount"])
        }
        assertEquals(listOf<Any?>(6L, 2L, 1L), yildiz(1))
        assertEquals(listOf<Any?>(5L, 3L, 1L), yildiz(2))
        assertEquals(listOf<Any?>(5L, 2L, 2L), yildiz(3))
        // 0 yıldız: sayaçlar artmaz ama üçü de yazılır
        assertEquals(listOf<Any?>(5L, 2L, 1L), yildiz(0))
        assertEquals(6, R.recordItemFirstFinish(snap, 0).size)
        // Belge yokken
        val bos = R.recordItemFirstFinish(emptyMap(), 3)
        assertEquals(0L, bos["oneStarCount"])
        assertEquals(0L, bos["twoStarCount"])
        assertEquals(1L, bos["threeStarCount"])
    }

    @Test
    fun recordItemReplay() {
        val u = R.recordItemReplay(mapOf("totalCompletionCount" to 4L, "passUserCount" to 4L))
        assertEquals(setOf("totalCompletionCount", "avgReplayCount"), u.keys)
        assertEquals(5L, u["totalCompletionCount"])
        yakin(1.25f, u["avgReplayCount"])
        // passUserCount yoksa ortalama 0
        val u2 = R.recordItemReplay(emptyMap())
        assertEquals(1L, u2["totalCompletionCount"])
        yakin(0f, u2["avgReplayCount"])
    }
}

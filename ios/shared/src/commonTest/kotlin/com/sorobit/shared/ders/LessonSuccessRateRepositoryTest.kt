package com.sorobit.shared.ders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Güncel Android: küresel sayaçlar kalktı; yalnızca kullanıcıya özel çift sayım koruması (stableId anahtarlı) kaldı. */
class LessonSuccessRateRepositoryTest {

    private val R = LessonSuccessRateRepository

    @Test
    fun yollarVeAlanAdlari() {
        assertEquals("users/U/lessonSuccessRateState", R.stateCollectionPath("U"))
        assertEquals(
            "users/U/lessonSuccessRateState/3_p3_i01_x_2",
            R.userStepStatePath("U", 3, "p3_i01_x", 2),
        )
        // Adım en az 1'e yuvarlanır
        assertEquals("users/U/lessonSuccessRateState/3_p3_i01_x_1", R.userStepStatePath("U", 3, "p3_i01_x", 0))
        assertEquals("users/U/lessonSuccessRateState/3_12_2", R.legacyUserStepStatePath("U", 3, 12, 2))
        assertEquals("users/U/lessonSuccessRateState/3_12_1", R.legacyUserStepStatePath("U", 3, 12, -4))

        assertEquals("attempted", R.FIELD_ATTEMPTED)
        assertEquals("passed", R.FIELD_PASSED)
        assertEquals("failStreak", R.FIELD_FAIL_STREAK)
    }

    @Test
    fun isTrackedPart() {
        assertFalse(R.isTrackedPart(0))
        assertTrue(R.isTrackedPart(1))
        assertTrue(R.isTrackedPart(8))
        assertFalse(R.isTrackedPart(9))
        assertTrue(R.shouldLogEvent(3, true))
        assertFalse(R.shouldLogEvent(3, false))
        assertFalse(R.shouldLogEvent(9, true))
    }

    @Test
    fun stateRefs_stableIdliVeSablonDisi() {
        val kimlik = GlobalLessonData.createLessonItems(1)[4].stableId
        assertEquals("p1_i04_unite_maratonu", kimlik)
        val r = R.stateRefs("U", 1, 4, 1)
        assertEquals("users/U/lessonSuccessRateState/1_p1_i04_unite_maratonu_1", r.targetPath)
        assertEquals("users/U/lessonSuccessRateState/1_4_1", r.legacyPath)
        assertEquals(kimlik, r.stableId)
        // Şablon dışı konum: eski biçimde kalır, eski belge yok
        val d = R.stateRefs("U", 1, 999, 2)
        assertEquals("users/U/lessonSuccessRateState/1_999_2", d.targetPath)
        assertNull(d.legacyPath)
        assertNull(d.stableId)
    }

    @Test
    fun pickUserSnap() {
        val hedef = mapOf<String, Any?>("a" to 1)
        val eski = mapOf<String, Any?>("b" to 2)
        assertEquals(hedef to false, R.pickUserSnap(hedef, eski))
        assertEquals(eski to true, R.pickUserSnap(null, eski))
        assertEquals(emptyMap<String, Any?>() to false, R.pickUserSnap(null, null))
        // Boş ama var olan hedef belge eskiye tercih edilir
        assertEquals(emptyMap<String, Any?>() to false, R.pickUserSnap(emptyMap(), eski))
    }

    @Test
    fun recordFail() {
        // İlk başarısızlık
        val w = R.recordFail(null)!!
        assertEquals(mapOf(R.FIELD_ATTEMPTED to true, R.FIELD_FAIL_STREAK to 1L), w.userState)
        assertEquals(1L, w.failStreak)
        assertFalse(w.deleteLegacy)
        // Seri artar
        val w2 = R.recordFail(mapOf(R.FIELD_ATTEMPTED to true, R.FIELD_FAIL_STREAK to 2L))!!
        assertEquals(3L, w2.failStreak)
        // Zaten geçilmiş adımda sayılmaz
        assertNull(R.recordFail(mapOf(R.FIELD_PASSED to true)))
        // Eski anahtardan okunursa taşınır (eski silinir)
        val w3 = R.recordFail(null, mapOf(R.FIELD_FAIL_STREAK to 1L))!!
        assertEquals(2L, w3.failStreak)
        assertTrue(w3.deleteLegacy)
        // Eski belgede passed varsa yine sayılmaz
        assertNull(R.recordFail(null, mapOf(R.FIELD_PASSED to true)))
        // Tür uymayan alan "yok" sayılır
        assertEquals(1L, R.recordFail(mapOf(R.FIELD_FAIL_STREAK to "x", R.FIELD_PASSED to "evet"))!!.failStreak)
    }

    @Test
    fun recordPass() {
        val w = R.recordPass(null)!!
        assertEquals(mapOf(R.FIELD_ATTEMPTED to true, R.FIELD_PASSED to true), w.userState)
        assertEquals(0L, w.failStreak) // ilk denemede geçti
        assertFalse(w.deleteLegacy)
        assertEquals(2L, R.recordPass(mapOf(R.FIELD_FAIL_STREAK to 2L))!!.failStreak)
        assertNull(R.recordPass(mapOf(R.FIELD_PASSED to true)))
        val e = R.recordPass(null, mapOf(R.FIELD_FAIL_STREAK to 1L))
        assertNotNull(e)
        assertEquals(1L, e.failStreak)
        assertTrue(e.deleteLegacy)
    }

    @Test
    fun analitikOrani() {
        assertEquals(60f, R.answerSuccessRateForAnalytics(1, 60f))
        assertEquals(60f, R.answerSuccessRateForAnalytics(6, 60f))
        assertNull(R.answerSuccessRateForAnalytics(7, 60f))
        assertNull(R.answerSuccessRateForAnalytics(3, null))
    }
}

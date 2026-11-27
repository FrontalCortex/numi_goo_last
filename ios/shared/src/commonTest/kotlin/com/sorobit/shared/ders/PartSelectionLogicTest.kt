package com.sorobit.shared.ders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PartSelectionLogicTest {

    private fun durum(
        partId: Int,
        pro: Boolean = false,
        p1: Boolean = false,
        p2: Boolean = false,
        p3: Boolean = false,
    ) = PartSelectionLogic.partState(partId, pro, p1, p2, p3)

    private fun oge(type: Int, stepIsFinish: Boolean = false, offset: Int = 0) = LessonItem(
        type = type,
        title = "x",
        offset = offset,
        isCompleted = true,
        stepCount = 1,
        stepIsFinish = stepIsFinish,
    )

    // ---- Plan ----

    @Test
    fun proPlan_buyukKucukHarfDuyarsiz_veOgretmen() {
        assertTrue(PartSelectionLogic.hasProPlan("pro", false))
        assertTrue(PartSelectionLogic.hasProPlan("PREMIUM", false))
        assertTrue(PartSelectionLogic.hasProPlan("Free", true))
        assertFalse(PartSelectionLogic.hasProPlan("Free", false))
        assertFalse(PartSelectionLogic.hasProPlan("", false))
    }

    // ---- Son sandık ----

    @Test
    fun sonSandik_yalnizcaSonSandigaBakar() {
        val ilkBitmisSonBitmemis = listOf(
            oge(LessonItem.TYPE_CHEST, stepIsFinish = true),
            oge(LessonItem.TYPE_LESSON, stepIsFinish = true),
            oge(LessonItem.TYPE_CHEST, stepIsFinish = false),
            oge(LessonItem.TYPE_LESSON, stepIsFinish = true),
        )
        assertFalse(PartSelectionLogic.lastChestComplete(ilkBitmisSonBitmemis))
        val sonBitmis = listOf(
            oge(LessonItem.TYPE_CHEST, stepIsFinish = false),
            oge(LessonItem.TYPE_CHEST, stepIsFinish = true),
        )
        assertTrue(PartSelectionLogic.lastChestComplete(sonBitmis))
    }

    @Test
    fun sonSandik_sandikYoksaYanlis() {
        assertFalse(PartSelectionLogic.lastChestComplete(emptyList()))
        assertFalse(PartSelectionLogic.lastChestComplete(listOf(oge(LessonItem.TYPE_LESSON, stepIsFinish = true))))
    }

    // ---- Kart durumları ----

    @Test
    fun part1_herZamanAcik() {
        val s = durum(1)
        assertTrue(s.visualActive)
        assertTrue(s.functionalActive)
        assertEquals("", s.inactiveMessage)
    }

    @Test
    fun part2_3_4_part1SandiginaBagli() {
        for (id in listOf(2, 3, 4)) {
            val kapali = durum(id, p2 = true, p3 = true, pro = true)
            assertFalse(kapali.visualActive)
            assertFalse(kapali.functionalActive)
            // Güncel Android: kapı önceki bölümün SON ünite maratonu ve uyarı bunu söyler
            assertEquals("Açmak için \"Abaküsün Temeli ve Toplama\" bölümünün son ünite maratonunu bitir.", kapali.inactiveMessage)
            val acik = durum(id, p1 = true)
            assertTrue(acik.visualActive)
            assertTrue(acik.functionalActive)
        }
    }

    @Test
    fun part5_part2ye_part6_part3e_bagli() {
        assertFalse(durum(5, p1 = true, p3 = true).functionalActive)
        assertTrue(durum(5, p2 = true).functionalActive)
        assertEquals("Açmak için \"Abaküste Çıkarma\" bölümünün son ünite maratonunu bitir.", durum(5).inactiveMessage)
        assertFalse(durum(6, p1 = true, p2 = true).functionalActive)
        assertTrue(durum(6, p3 = true).functionalActive)
        assertEquals("Açmak için \"Abaküste Çarpma\" bölümünün son ünite maratonunu bitir.", durum(6).inactiveMessage)
        assertEquals("Açmak için \"Abaküsün Temeli ve Toplama\" bölümünün son ünite maratonunu bitir.", PartSelectionLogic.marathonGateMessage(1))
    }

    @Test
    fun onayliOgretmen_butunKartlarAcik() {
        // Güncel Android: onaylı öğretmende bölüm sırası yok; sandıkları bitmiş sayılmasa da 1–8 açık
        for (id in 1..8) {
            val s = PartSelectionLogic.partState(id, hasProPlan = true, part1ChestComplete = false, part2ChestComplete = false, part3ChestComplete = false, teacherApproved = true)
            assertTrue(s.visualActive, "part $id")
            assertTrue(s.functionalActive, "part $id")
            assertEquals("", s.inactiveMessage)
        }
        val satir = PartSelectionLogic.partStates(listOf(2, 5, 8), true, false, false, false, teacherApproved = true)
        assertEquals(listOf(true, true, true), satir.map { it.functionalActive })
        // Öğretmen değilse eskisi gibi kapalı
        assertFalse(PartSelectionLogic.partState(2, true, false, false, false).functionalActive)
    }

    @Test
    fun part7_8_proPlanIster() {
        val p7Free = durum(7, p1 = true)
        assertFalse(p7Free.visualActive)
        assertFalse(p7Free.functionalActive)
        assertEquals("Bu kısmı açmak için planı Pro'ya yükselt.", p7Free.inactiveMessage)

        val p7ProSandikYok = durum(7, pro = true)
        assertTrue(p7ProSandikYok.visualActive)
        assertFalse(p7ProSandikYok.functionalActive)
        assertEquals("Abaküste Toplama kısmını bitir.", p7ProSandikYok.inactiveMessage)
        assertTrue(durum(7, pro = true, p1 = true).functionalActive)

        val p8ProSandikYok = durum(8, pro = true, p1 = true)
        assertTrue(p8ProSandikYok.visualActive)
        assertFalse(p8ProSandikYok.functionalActive)
        assertEquals("Abaküste Çıkarma kısmını bitir.", p8ProSandikYok.inactiveMessage)
        assertTrue(durum(8, pro = true, p2 = true).functionalActive)
        assertEquals("Bu kısmı açmak için planı Pro'ya yükselt.", durum(8, p2 = true).inactiveMessage)
    }

    @Test
    fun bilinmeyenPart_acik() {
        val s = durum(42)
        assertEquals(42, s.partId)
        assertTrue(s.visualActive)
        assertTrue(s.functionalActive)
    }

    @Test
    fun satirlar_veTopluDurum() {
        assertEquals(listOf(1, 4, 7), PartSelectionLogic.ADDITION_PARTS)
        assertEquals(listOf(2, 5, 8), PartSelectionLogic.SUBTRACTION_PARTS)
        assertEquals(listOf(3, 6), PartSelectionLogic.MULTIPLICATION_PARTS)
        val satir = PartSelectionLogic.partStates(PartSelectionLogic.ADDITION_PARTS, false, true, false, false)
        assertEquals(listOf(1, 4, 7), satir.map { it.partId })
        assertEquals(listOf(true, true, false), satir.map { it.functionalActive })
    }

    @Test
    fun sablonla_part1SonSandigiBitmisSayilir() {
        // Şablonda (boş hesap) part 1-3'ün son sandığının durumu ne ise kart durumu ondan türemeli.
        val p1 = PartSelectionLogic.lastChestComplete(GlobalLessonData.createLessonItems(1))
        // Güncel Android şablonunda hiçbir sandık bitmiş gelmez (eskiden son sandık bitmiş geliyordu)
        assertFalse(p1)
        val s2 = PartSelectionLogic.partState(2, false, p1, false, false)
        assertEquals(p1, s2.functionalActive)
    }

    // ---- Başlık, animasyon, renk ----

    @Test
    fun basliklar() {
        assertEquals("Abaküsün Temeli ve Toplama", PartSelectionLogic.partTitle(1))
        assertEquals("Abaküste Çıkarma", PartSelectionLogic.partTitle(2))
        assertEquals("Abaküste Çarpma", PartSelectionLogic.partTitle(3))
        assertEquals("Körleme Toplama", PartSelectionLogic.partTitle(4))
        assertEquals("Körleme Çıkarma", PartSelectionLogic.partTitle(5))
        assertEquals("Körleme Çarpma", PartSelectionLogic.partTitle(6))
        assertEquals("Ustalık Yolu - Toplama", PartSelectionLogic.partTitle(7))
        assertEquals("Ustalık Yolu - Çıkarma", PartSelectionLogic.partTitle(8))
        assertEquals("Part : 9", PartSelectionLogic.partTitle(9))
    }

    @Test
    fun animasyonAdlari() {
        assertEquals("roller_coaster_anim.json", PartSelectionLogic.partAnimationName(1))
        assertEquals("rocket_anim.json", PartSelectionLogic.partAnimationName(2))
        assertEquals("piata_anim.json", PartSelectionLogic.partAnimationName(5))
        assertEquals("skateboard_anim.json", PartSelectionLogic.partAnimationName(8))
        assertEquals("dinosaur_anim.json", PartSelectionLogic.partAnimationName(0))
    }

    @Test
    fun kartRenkleri() {
        assertEquals("lesson_header_orange", PartSelectionLogic.partCardColor(durum(1)))
        assertEquals("lesson_header_orange", PartSelectionLogic.partCardColor(durum(4, p1 = true)))
        assertEquals("lesson_header_orange", PartSelectionLogic.partCardColor(durum(7, pro = true)))
        assertEquals("lesson_header_green", PartSelectionLogic.partCardColor(durum(2, p1 = true)))
        assertEquals("lesson_header_green", PartSelectionLogic.partCardColor(durum(5, p2 = true)))
        assertEquals("lesson_header_green", PartSelectionLogic.partCardColor(durum(8, pro = true)))
        assertEquals("lesson_header_blue", PartSelectionLogic.partCardColor(durum(3, p1 = true)))
        assertEquals("lesson_header_blue", PartSelectionLogic.partCardColor(durum(6, p3 = true)))
        assertEquals("background_color", PartSelectionLogic.partCardColor(durum(2)))
        assertEquals("white", PartSelectionLogic.partLabelColor(durum(1)))
        assertEquals("lesson_locked", PartSelectionLogic.partLabelColor(durum(2)))
    }

    @Test
    fun yarisPaneli() {
        assertTrue(PartSelectionLogic.opensRacePanel(7))
        assertTrue(PartSelectionLogic.opensRacePanel(8))
        assertFalse(PartSelectionLogic.opensRacePanel(1))
        assertEquals("Toplama Ustalığı", PartSelectionLogic.racePanelTitle(7))
        assertEquals("Çıkarma Ustalığı", PartSelectionLogic.racePanelTitle(8))
    }

    // ---- Yatay kayma ----

    private fun kayma(type: Int, offset: Int, genislik: Float) =
        MapLayoutLogic.lessonHorizontalShift(type, offset, genislik, 100f, 85f, 34f, 8f)

    @Test
    fun kayma_yalnizcaDers() {
        assertEquals(0f, kayma(LessonItem.TYPE_CHEST, 30, 400f))
        assertEquals(0f, kayma(LessonItem.TYPE_HEADER, 30, 400f))
        assertEquals(0f, kayma(LessonItem.TYPE_RACE, 30, 400f))
        assertEquals(60f, kayma(LessonItem.TYPE_LESSON, 30, 400f))
        assertEquals(-60f, kayma(LessonItem.TYPE_LESSON, -30, 400f))
        assertEquals(0f, kayma(LessonItem.TYPE_LESSON, 0, 400f))
    }

    @Test
    fun kayma_darEkrandaSinirlanir() {
        // (320 - 34 - 85) / 2 - 8 = 92.5
        assertEquals(92.5f, kayma(LessonItem.TYPE_LESSON, 60, 320f))
        assertEquals(-92.5f, kayma(LessonItem.TYPE_LESSON, -60, 320f))
        // Kart sığmıyorsa hiç kaymaz
        assertEquals(0f, kayma(LessonItem.TYPE_LESSON, 60, 100f))
    }

    @Test
    fun kayma_genislikBilinmiyorsaSinirlanmaz() {
        assertEquals(120f, kayma(LessonItem.TYPE_LESSON, 60, 0f))
    }

    @Test
    fun kayma_varsayilanBoyutlar() {
        val ders = oge(LessonItem.TYPE_LESSON, offset = -30)
        assertEquals(-60f, MapLayoutLogic.lessonHorizontalShiftDefault(ders, 390f))
        assertEquals(
            kayma(LessonItem.TYPE_LESSON, 60, 320f),
            MapLayoutLogic.lessonHorizontalShiftDefault(oge(LessonItem.TYPE_LESSON, offset = 60), 320f),
        )
    }
}

// AbacusPreferences / AbacusCustomizationFirestore / satın alma kurallarının Android ile aynı
// anahtarları, varsayılanları ve sonuçları ürettiğini doğrular. Beklenen değerler Android
// kaynağından (AbacusPreferences.kt, AbacusCustomizationFirestore.kt, Bead/FramePurchaseFirestore.kt,
// AbacusCustomizationFragment.kt) elle türetilmiştir.
package com.sorobit.shared.abakus

import com.sorobit.shared.abakus.AbacusPreferences.BeadType
import com.sorobit.shared.abakus.AbacusPreferences.FrameType
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun argb(hex: Long): Int = hex.toInt()

class AbacusPreferencesTest {
    private val store = BellekAbacusPrefsStore()

    @Test
    fun enumAdlariVeSirasi() {
        assertEquals(
            listOf(
                "SOROBAN", "ANIMAL", "ANIMAL2", "ANIMAL3", "ANIMAL4", "ANIMAL5", "ANIMAL6", "ANIMAL7", "ANIMAL8",
                "ANIMAL9", "BOWLING", "BALL1", "BALL3", "BALL4", "BALL5", "BALL6", "BALL7", "BALL8", "BALL9",
                "BALL10", "BALL11", "BALL12", "BALL13", "BALL14", "SOROBAN2", "SOROBAN6",
            ),
            BeadType.entries.map { it.name },
        )
        assertEquals(listOf("FRAME_BG", "FRAME_BG2", "FRAME_BG3", "FRAME_BG4"), FrameType.entries.map { it.name })
    }

    @Test
    fun dosyaAdiVeUidOneki() {
        assertEquals("abacus_customization_guest", AbacusPreferences.prefsFileName(null))
        assertEquals("abacus_customization_u1", AbacusPreferences.prefsFileName("u1"))
        assertEquals("guest_bead_type", AbacusPreferences.getBeadTypeKey(null))
        assertEquals("u1_bead_type", AbacusPreferences.getBeadTypeKey("u1"))
        assertEquals("u1_bead_slot_top_3", AbacusPreferences.slotKeyTop("u1", 3))
        assertEquals("guest_bead_slot_bot_3_2", AbacusPreferences.slotKeyBottom(null, 3, 2))
    }

    @Test
    fun boncukTipi() {
        assertEquals(BeadType.SOROBAN, AbacusPreferences.getBeadType(store, "u1"))
        AbacusPreferences.setBeadType(store, "u1", BeadType.BALL3)
        assertEquals("BALL3", store.degerler["u1_bead_type"])
        assertEquals(BeadType.BALL3, AbacusPreferences.getBeadType(store, "u1"))
        // Başka uid öneki ayrı anahtar okur
        assertEquals(BeadType.SOROBAN, AbacusPreferences.getBeadType(store, null))
        // Bilinmeyen ad → SOROBAN
        store.putString("u1_bead_type", "RABBIT")
        assertEquals(BeadType.SOROBAN, AbacusPreferences.getBeadType(store, "u1"))
        assertNull(AbacusPreferences.beadTypeFromName("soroban")) // büyük/küçük harf duyarlı
        assertEquals(BeadType.SOROBAN6, AbacusPreferences.beadTypeFromName("SOROBAN6"))
        assertNull(AbacusPreferences.beadTypeFromName(null))
    }

    @Test
    fun yuvaTipiGenelTipeDusmez() {
        AbacusPreferences.setBeadType(store, "u1", BeadType.BALL1)
        // Android davranışı: yuva kaydı yoksa genel tip değil SOROBAN döner
        assertEquals(BeadType.SOROBAN, AbacusPreferences.getBeadTypeForSlot(store, "u1", 0, isTop = true))
        AbacusPreferences.setBeadTypeForSlot(store, "u1", 2, isTop = false, beadIndex = 3, type = BeadType.ANIMAL7)
        assertEquals("ANIMAL7", store.degerler["u1_bead_slot_bot_2_3"])
        assertEquals(BeadType.ANIMAL7, AbacusPreferences.getBeadTypeForSlot(store, "u1", 2, isTop = false, beadIndex = 3))
        assertEquals(BeadType.SOROBAN, AbacusPreferences.getBeadTypeForSlot(store, "u1", 2, isTop = false, beadIndex = 2))
        AbacusPreferences.setBeadTypeForSlot(store, "u1", 4, isTop = true, type = BeadType.BOWLING)
        assertEquals("BOWLING", store.degerler["u1_bead_slot_top_4"])
        store.putString("u1_bead_slot_top_1", "YOK")
        assertEquals(BeadType.SOROBAN, AbacusPreferences.getBeadTypeForSlot(store, "u1", 1, isTop = true))

        AbacusPreferences.resetAllBeadSlots(store, "u1")
        assertEquals(setOf("u1_bead_type"), store.degerler.keys)
    }

    @Test
    fun cerceveTipi() {
        assertEquals(FrameType.FRAME_BG, AbacusPreferences.getFrameType(store))
        AbacusPreferences.setFrameType(store, FrameType.FRAME_BG4)
        assertEquals("FRAME_BG4", store.degerler["frame_type"]) // uid öneki yok
        assertEquals(FrameType.FRAME_BG4, AbacusPreferences.getFrameType(store))
        store.putString("frame_type", "X")
        assertEquals(FrameType.FRAME_BG, AbacusPreferences.getFrameType(store))
    }

    @Test
    fun sorobanRenkleri() {
        assertContentEquals(
            intArrayOf(argb(0xFF000000), argb(0xFF737373), argb(0xFF8C8C8C)),
            AbacusPreferences.getBeadColors(store, BeadType.SOROBAN, false),
        )
        assertContentEquals(
            intArrayOf(argb(0xFF000000), argb(0xFF4D0000), argb(0xFF731616)),
            AbacusPreferences.getBeadColors(store, BeadType.SOROBAN, true),
        )
        AbacusPreferences.setBeadColor(store, BeadType.SOROBAN, false, 1, 123)
        AbacusPreferences.setBeadColor(store, BeadType.SOROBAN, true, 2, 456)
        assertEquals(123, store.degerler["soroban_path_1"])
        assertEquals(456, store.degerler["soroban_sel_path_2"])
        assertEquals(123, AbacusPreferences.getBeadColors(store, BeadType.SOROBAN, false)[1])

        // Sıfırlama varsayılanları YAZAR (anahtar kalır), yalnızca istenen tarafı etkiler
        AbacusPreferences.resetBeadColors(store, BeadType.SOROBAN, false)
        assertEquals(argb(0xFF000000), store.degerler["soroban_path_0"])
        assertEquals(argb(0xFF737373), store.degerler["soroban_path_1"])
        assertEquals(argb(0xFF8C8C8C), store.degerler["soroban_path_2"])
        assertEquals(456, store.degerler["soroban_sel_path_2"])
    }

    @Test
    fun onekliAnahtarlar() {
        // DİKKAT: Android'deki ANIMAL3_COLOR_KEYS ("animal3_color_N") kullanılmaz; asıl anahtar "animal3_cN"
        AbacusPreferences.setBeadColor(store, BeadType.ANIMAL3, false, 2, 1)
        AbacusPreferences.setBeadColor(store, BeadType.ANIMAL, true, 0, 2)
        AbacusPreferences.setBeadColor(store, BeadType.BALL14, true, 0, 3)
        AbacusPreferences.setBeadColor(store, BeadType.BOWLING, false, 4, 4)
        AbacusPreferences.setBeadColor(store, BeadType.BALL10, false, 4, 5)
        assertEquals(
            mapOf<String, Any>("animal3_c2" to 1, "animal_sel_c0" to 2, "ball14_sel_c0" to 3, "bowling_c4" to 4, "ball10_c4" to 5),
            store.degerler,
        )
        // Önekli tiplerde dizin sınırı denetlenmez (Android ile aynı)
        AbacusPreferences.setBeadColor(store, BeadType.BALL1, false, 9, 6)
        assertEquals(6, store.degerler["ball1_c9"])
    }

    @Test
    fun soroban2veSoroban6() {
        assertContentEquals(
            intArrayOf(argb(0xFFFEDA77), argb(0xFFF58529), argb(0xFFDD2A7B), argb(0xFF8134AF), argb(0xFF515BD4)),
            AbacusPreferences.getBeadColors(store, BeadType.SOROBAN2, false),
        )
        assertContentEquals(
            intArrayOf(argb(0xFF957E3C), argb(0xFF904E18), argb(0xFF7B1543), argb(0xFF43195D), argb(0xFF2A3073)),
            AbacusPreferences.getBeadColors(store, BeadType.SOROBAN2, true),
        )
        // SOROBAN6'da seçili ve normal varsayılanlar aynı
        assertContentEquals(
            intArrayOf(argb(0xFF9AE2C6), argb(0xFFFFFFFF), argb(0xFF000000)),
            AbacusPreferences.getBeadColors(store, BeadType.SOROBAN6, true),
        )
        AbacusPreferences.setBeadColor(store, BeadType.SOROBAN2, false, 3, 7)
        AbacusPreferences.setBeadColor(store, BeadType.SOROBAN2, true, 3, 8)
        AbacusPreferences.setBeadColor(store, BeadType.SOROBAN6, true, 0, 9)
        assertEquals(
            mapOf<String, Any>("soroban2_color_3" to 7, "sel_soroban2_color_3" to 8, "sel_soroban6_color_0" to 9),
            store.degerler,
        )
        // Sıfırlama anahtarları SİLER (önekli tiplerden farklı)
        AbacusPreferences.resetBeadColors(store, BeadType.SOROBAN2, true)
        assertEquals(setOf("soroban2_color_3", "sel_soroban6_color_0"), store.degerler.keys)
        AbacusPreferences.resetBeadColors(store, BeadType.SOROBAN2, false)
        AbacusPreferences.resetBeadColors(store, BeadType.SOROBAN6, true)
        assertTrue(store.degerler.isEmpty())
        // Geçersiz dizin Android'deki gibi dizi sınırı hatası verir
        assertFailsWith<IndexOutOfBoundsException> { AbacusPreferences.setBeadColor(store, BeadType.SOROBAN6, false, 3, 1) }
    }

    @Test
    fun renkGrubuBoyutlari() {
        val beklenen = mapOf(
            BeadType.SOROBAN to 3, BeadType.ANIMAL to 4, BeadType.ANIMAL2 to 5, BeadType.ANIMAL3 to 5,
            BeadType.ANIMAL4 to 4, BeadType.ANIMAL5 to 6, BeadType.ANIMAL6 to 3, BeadType.ANIMAL7 to 5,
            BeadType.ANIMAL8 to 5, BeadType.ANIMAL9 to 4, BeadType.BOWLING to 5, BeadType.BALL1 to 3,
            BeadType.BALL3 to 4, BeadType.BALL4 to 4, BeadType.BALL5 to 4, BeadType.BALL6 to 4,
            BeadType.BALL7 to 3, BeadType.BALL8 to 4, BeadType.BALL9 to 5, BeadType.BALL10 to 5,
            BeadType.BALL11 to 4, BeadType.BALL12 to 5, BeadType.BALL13 to 4, BeadType.BALL14 to 3,
            BeadType.SOROBAN2 to 5, BeadType.SOROBAN6 to 3,
        )
        assertEquals(BeadType.entries.toSet(), beklenen.keys)
        for ((tip, boyut) in beklenen) {
            val spec = AbacusPreferences.beadColorSpec(tip)
            assertEquals(boyut, spec.size, tip.name)
            assertEquals(boyut, spec.selDefaults.size, tip.name)
            assertEquals(boyut, spec.labels.size, tip.name)
            assertEquals(boyut, AbacusPreferences.getBeadColors(store, tip, false).size, tip.name)
            assertEquals(boyut, AbacusPreferences.getBeadColors(store, tip, true).size, tip.name)
        }
    }

    @Test
    fun secmeVarsayilanRenkler() {
        assertEquals(argb(0xFFEFC9A5), AbacusPreferences.getBeadColors(store, BeadType.ANIMAL5, false)[5])
        assertEquals(argb(0xFFB19E0F), AbacusPreferences.getBeadColors(store, BeadType.BALL9, true)[1])
        assertEquals(argb(0xFF5A648C), AbacusPreferences.getBeadColors(store, BeadType.BOWLING, false)[0])
        assertEquals(argb(0xFF000001), AbacusPreferences.getBeadColors(store, BeadType.BALL14, true)[0])
        assertContentEquals(
            intArrayOf(argb(0xFFFFFFFF), argb(0xFF777873), argb(0xFF4D4C48), argb(0xFF32302D)),
            AbacusPreferences.getBeadColors(store, BeadType.ANIMAL, false),
        )
        assertContentEquals(arrayOf("Dış çerçeve", "İç zemin", "Sağ yansıma"), AbacusPreferences.beadColorSpec(BeadType.SOROBAN).labels)
        assertContentEquals(arrayOf("Gövde", "Işık", "Gölge"), AbacusPreferences.beadColorSpec(BeadType.SOROBAN6).labels)
        assertContentEquals(arrayOf("Göz", "Kulak içi", "Kulak dışı", "Kafa", "Burun", "Diş"), AbacusPreferences.beadColorSpec(BeadType.ANIMAL5).labels)
    }

    @Test
    fun tavsanRenkleri() {
        val r = AbacusPreferences.getRabbitColors(store, false)
        assertEquals(7, r.size)
        assertEquals(argb(0xFFDCD2D1), r[0])
        assertEquals(argb(0xFF332F28), r[6])
        assertEquals(argb(0xFF1A080B), AbacusPreferences.getRabbitColors(store, true)[6])
        AbacusPreferences.setRabbitColor(store, false, 0, 1)
        AbacusPreferences.setRabbitColor(store, true, 6, 2)
        assertEquals(mapOf<String, Any>("rabbit_c0" to 1, "rabbit_sel_c6" to 2), store.degerler)
        AbacusPreferences.resetRabbitColors(store, false)
        assertEquals(argb(0xFFDCD2D1), store.degerler["rabbit_c0"])
        assertEquals(8, store.degerler.size) // 7 normal + 1 seçili
    }

    @Test
    fun cerceveRenkleri() {
        assertEquals(argb(0xFFDDA379), AbacusPreferences.getFrameBgWoodColor(store))
        assertEquals(argb(0xFFDDA379), AbacusPreferences.getFrameBg2WoodColor(store))
        assertEquals(argb(0xFFE63946), AbacusPreferences.getFrameBg2HeartColor(store))
        assertEquals(argb(0xFFDDA379), AbacusPreferences.getFrameBg3WoodColor(store))
        assertEquals(argb(0xFFDDFF33), AbacusPreferences.getFrameBg3StarColor(store))
        assertContentEquals(
            intArrayOf(argb(0xFFFEDA77), argb(0xFFF58529), argb(0xFFDD2A7B), argb(0xFF8134AF), argb(0xFF515BD4)),
            AbacusPreferences.getFrameBg4Colors(store),
        )
        AbacusPreferences.setFrameBgWoodColor(store, 1)
        AbacusPreferences.setFrameBg2WoodColor(store, 2)
        AbacusPreferences.setFrameBg2HeartColor(store, 3)
        AbacusPreferences.setFrameBg3WoodColor(store, 4)
        AbacusPreferences.setFrameBg3StarColor(store, 5)
        AbacusPreferences.setFrameBg4Color(store, 0, 6)
        AbacusPreferences.setFrameBg4Color(store, 4, 7)
        AbacusPreferences.setFrameBg4Color(store, 5, 8) // yok sayılır
        AbacusPreferences.setFrameBg4Color(store, -1, 9) // yok sayılır
        assertEquals(
            mapOf<String, Any>(
                "frame_bg_wood" to 1, "frame_bg2_wood" to 2, "frame_bg2_heart" to 3, "frame_bg3_wood" to 4,
                "frame_bg3_star" to 5, "frame_bg4_c1" to 6, "frame_bg4_c5" to 7,
            ),
            store.degerler,
        )
        AbacusPreferences.resetAllFrameColors(store)
        assertEquals(10, store.degerler.size)
        assertEquals(argb(0xFFDDA379), store.degerler["frame_bg_wood"])
        assertEquals(argb(0xFFE63946), store.degerler["frame_bg2_heart"])
        assertEquals(argb(0xFFDDFF33), store.degerler["frame_bg3_star"])
        assertEquals(argb(0xFF8134AF), store.degerler["frame_bg4_c4"])
    }
}

class AbacusCustomizationFirestoreTest {
    @Test
    fun yolVeAlanAdlari() {
        assertEquals("users/abc/abacusCustomization/state", AbacusCustomizationFirestore.docPath("abc"))
        assertEquals("abacus_customization_sync_v2", AbacusCustomizationFirestore.HYDRATION_PREFS)
        assertEquals("hydrated_uid", AbacusCustomizationFirestore.KEY_HYDRATED_UID)
        assertTrue(AbacusCustomizationFirestore.isHydrated("u", "u"))
        assertFalse(AbacusCustomizationFirestore.isHydrated(null, "u"))
        assertFalse(AbacusCustomizationFirestore.isHydrated("v", "u"))
    }

    @Test
    fun anlikGoruntuYardimcilari() {
        val bos = AbacusCustomizationSnapshot()
        assertEquals(BeadType.SOROBAN, bos.beadType)
        assertEquals(FrameType.FRAME_BG, bos.frameType)
        assertEquals(BeadType.SOROBAN, bos.beadTypeForSlot(0, true))
        assertNull(bos.colorsFor(BeadType.SOROBAN, false))
        assertNull(bos.frameColorArray())
        assertNull(bos.previewBeadType)

        val s = AbacusCustomizationSnapshot(
            slots = mapOf("top_2" to BeadType.BALL1, "bot_2_3" to BeadType.ANIMAL),
            beadColors = mapOf("BALL1" to intArrayOf(1, 2, 3), "BALL1_sel" to intArrayOf(4, 5, 6)),
        )
        assertEquals(BeadType.BALL1, s.beadTypeForSlot(2, true))
        assertEquals(BeadType.ANIMAL, s.beadTypeForSlot(2, false, 3))
        assertEquals(BeadType.SOROBAN, s.beadTypeForSlot(2, false)) // bot_2_0 yok
        assertContentEquals(intArrayOf(1, 2, 3), s.colorsFor(BeadType.BALL1, false))
        assertContentEquals(intArrayOf(4, 5, 6), s.colorsFor(BeadType.BALL1, true))
    }

    @Test
    fun cerceveRenkDizisi() {
        fun snap(tip: FrameType, renkler: Map<String, Int>) = AbacusCustomizationSnapshot(frameType = tip, frameColors = renkler)
        assertContentEquals(intArrayOf(1), snap(FrameType.FRAME_BG, mapOf("wood" to 1, "heart" to 2)).frameColorArray())
        assertContentEquals(intArrayOf(1, 2), snap(FrameType.FRAME_BG2, mapOf("wood" to 1, "heart" to 2)).frameColorArray())
        assertNull(snap(FrameType.FRAME_BG2, mapOf("wood" to 1)).frameColorArray())
        assertNull(snap(FrameType.FRAME_BG2, mapOf("heart" to 1)).frameColorArray())
        assertContentEquals(intArrayOf(1, 3), snap(FrameType.FRAME_BG3, mapOf("wood" to 1, "star" to 3)).frameColorArray())
        assertNull(snap(FrameType.FRAME_BG3, mapOf("wood" to 1, "heart" to 2)).frameColorArray())
        assertContentEquals(
            intArrayOf(10, 11, 12, 13, 14),
            snap(FrameType.FRAME_BG4, mapOf("c4" to 14, "c0" to 10, "c1" to 11, "c2" to 12, "c3" to 13)).frameColorArray(),
        )
        assertNull(snap(FrameType.FRAME_BG4, mapOf("c0" to 10, "c1" to 11, "c2" to 12, "c3" to 13)).frameColorArray())
    }

    @Test
    fun dokumandanAnlikGoruntu() {
        assertEquals(BeadType.SOROBAN, AbacusCustomizationFirestore.snapshotFromDocument(false, mapOf("beadType" to "BALL3")).beadType)
        assertEquals(FrameType.FRAME_BG, AbacusCustomizationFirestore.snapshotFromDocument(true, null).frameType)

        val s = AbacusCustomizationFirestore.snapshotFromDocument(
            true,
            mapOf(
                "beadType" to "BALL3",
                "frameType" to "FRAME_BG2",
                "slots" to mapOf("top_0" to "ANIMAL", "bot_1_2" to "NOPE", 5 to "BALL1", "top_1" to 7),
                "beadColors" to mapOf(
                    "BALL3" to listOf(-1L, 4278190080L, "x", 2.0),
                    "kotu" to "liste değil",
                ),
                "frameColors" to mapOf("wood" to 4292715385L, "heart" to 3, "bad" to "x"),
                "previewBeadType" to "ANIMAL2",
                "updatedAt" to 123L,
            ),
        )
        assertEquals(BeadType.BALL3, s.beadType)
        assertEquals(FrameType.FRAME_BG2, s.frameType)
        // Bilinmeyen tip adı SOROBAN olur; String olmayan anahtar/değer atlanır
        assertEquals(mapOf("top_0" to BeadType.ANIMAL, "bot_1_2" to BeadType.SOROBAN), s.slots)
        assertEquals(setOf("BALL3"), s.beadColors.keys)
        // Long → Int kırpılır; sayı olmayan öğe 0 olur
        assertContentEquals(intArrayOf(-1, argb(0xFF000000), 0, 2), s.beadColors["BALL3"])
        assertEquals(mapOf("wood" to argb(0xFFDDA379), "heart" to 3), s.frameColors)
        assertEquals(BeadType.ANIMAL2, s.previewBeadType)
        assertContentEquals(intArrayOf(argb(0xFFDDA379), 3), s.frameColorArray())

        val bozuk = AbacusCustomizationFirestore.snapshotFromDocument(
            true,
            mapOf("beadType" to "XX", "frameType" to 5, "previewBeadType" to "YY", "slots" to "x"),
        )
        assertEquals(BeadType.SOROBAN, bozuk.beadType)
        assertEquals(FrameType.FRAME_BG, bozuk.frameType)
        assertNull(bozuk.previewBeadType)
        assertTrue(bozuk.slots.isEmpty())
    }

    @Test
    fun yazmaVerisi() {
        val s = AbacusCustomizationSnapshot(
            beadType = BeadType.BALL1,
            frameType = FrameType.FRAME_BG3,
            slots = mapOf("top_0" to BeadType.ANIMAL),
            beadColors = mapOf("BALL1" to intArrayOf(1, 2, 3)),
            frameColors = mapOf("wood" to 9, "star" to 8),
            previewBeadType = BeadType.BOWLING,
        )
        assertEquals(
            mapOf(
                "beadType" to "BALL1",
                "frameType" to "FRAME_BG3",
                "slots" to mapOf("top_0" to "ANIMAL"),
                "beadColors" to mapOf("BALL1" to listOf(1, 2, 3)),
                "frameColors" to mapOf("wood" to 9, "star" to 8),
            ),
            AbacusCustomizationFirestore.customizationData(s), // previewBeadType ve updatedAt yok
        )
        assertEquals(mapOf<String, Any>("previewBeadType" to "BOWLING"), AbacusCustomizationFirestore.previewBeadTypeData(BeadType.BOWLING))
    }

    @Test
    fun yereldenAnlikGoruntu() {
        val store = BellekAbacusPrefsStore()
        AbacusPreferences.setBeadType(store, "u", BeadType.BALL1)
        AbacusPreferences.setBeadTypeForSlot(store, "u", 0, isTop = true, type = BeadType.ANIMAL)
        AbacusPreferences.setBeadColor(store, BeadType.ANIMAL, true, 3, 77)
        AbacusPreferences.setFrameType(store, FrameType.FRAME_BG3)
        AbacusPreferences.setFrameBg3StarColor(store, 55)
        AbacusPreferences.setFrameBg2HeartColor(store, 66) // etkin çerçeve değil: alınmaz

        val s = AbacusCustomizationFirestore.buildSnapshotFromLocalPreferences(store, "u")
        assertEquals(BeadType.BALL1, s.beadType)
        assertEquals(FrameType.FRAME_BG3, s.frameType)
        assertEquals(25, s.slots.size)
        assertEquals(BeadType.ANIMAL, s.slots["top_0"])
        assertEquals(BeadType.SOROBAN, s.slots["bot_4_3"])
        // Yalnızca kullanılan tiplerin (yuvalar + genel tip) renkleri
        assertEquals(setOf("SOROBAN", "SOROBAN_sel", "ANIMAL", "ANIMAL_sel", "BALL1", "BALL1_sel"), s.beadColors.keys)
        assertEquals(77, s.beadColors["ANIMAL_sel"]!![3])
        assertContentEquals(intArrayOf(argb(0xFFB8DB6A), argb(0xFFF7FFAE), argb(0xFF92BF62)), s.beadColors["BALL1"])
        assertEquals(mapOf("wood" to argb(0xFFDDA379), "star" to 55), s.frameColors)
        assertNull(s.previewBeadType)
    }

    @Test
    fun cerceveTipineGoreYerelRenkler() {
        val store = BellekAbacusPrefsStore()
        assertEquals(mapOf("wood" to argb(0xFFDDA379)), AbacusCustomizationFirestore.buildSnapshotFromLocalPreferences(store, null).frameColors)
        AbacusPreferences.setFrameType(store, FrameType.FRAME_BG2)
        assertEquals(
            mapOf("wood" to argb(0xFFDDA379), "heart" to argb(0xFFE63946)),
            AbacusCustomizationFirestore.buildSnapshotFromLocalPreferences(store, null).frameColors,
        )
        AbacusPreferences.setFrameType(store, FrameType.FRAME_BG4)
        assertEquals(
            mapOf("c0" to argb(0xFFFEDA77), "c1" to argb(0xFFF58529), "c2" to argb(0xFFDD2A7B), "c3" to argb(0xFF8134AF), "c4" to argb(0xFF515BD4)),
            AbacusCustomizationFirestore.buildSnapshotFromLocalPreferences(store, null).frameColors,
        )
    }

    @Test
    fun anlikGoruntuyuYereleUygulama() {
        val store = BellekAbacusPrefsStore()
        val s = AbacusCustomizationSnapshot(
            beadType = BeadType.SOROBAN2,
            frameType = FrameType.FRAME_BG4,
            slots = mapOf("top_1" to BeadType.BALL7, "bot_3_2" to BeadType.SOROBAN2),
            beadColors = mapOf(
                "BALL7" to intArrayOf(1, 2, 3),
                "SOROBAN2_sel" to intArrayOf(10, 11, 12, 13, 14),
            ),
            frameColors = mapOf("c0" to 20, "c3" to 23, "wood" to 99),
        )
        AbacusCustomizationFirestore.applySnapshotToLocalPreferences(store, "u", s)

        assertEquals("SOROBAN2", store.degerler["u_bead_type"])
        assertEquals("BALL7", store.degerler["u_bead_slot_top_1"])
        assertEquals("SOROBAN2", store.degerler["u_bead_slot_bot_3_2"])
        // Anlık görüntüde olmayan yuvalar SOROBAN olarak YAZILIR (25 yuvanın tamamı)
        assertEquals("SOROBAN", store.degerler["u_bead_slot_top_0"])
        assertEquals(25, store.degerler.keys.count { it.startsWith("u_bead_slot_") })
        assertEquals(1, store.degerler["ball7_c0"])
        assertEquals(3, store.degerler["ball7_c2"])
        assertFalse(store.degerler.containsKey("ball7_sel_c0"))
        assertEquals(14, store.degerler["sel_soroban2_color_4"])
        assertEquals("FRAME_BG4", store.degerler["frame_type"])
        assertEquals(20, store.degerler["frame_bg4_c1"])
        assertEquals(23, store.degerler["frame_bg4_c4"])
        assertFalse(store.degerler.containsKey("frame_bg4_c2"))
        assertFalse(store.degerler.containsKey("frame_bg_wood")) // etkin çerçeveye ait değil

        // Gidiş-dönüş: yerelden yeniden kurulan görüntü aynı tip/yuva/renkleri verir
        val geri = AbacusCustomizationFirestore.buildSnapshotFromLocalPreferences(store, "u")
        assertEquals(BeadType.SOROBAN2, geri.beadType)
        assertEquals(BeadType.BALL7, geri.beadTypeForSlot(1, true))
        assertEquals(BeadType.SOROBAN2, geri.beadTypeForSlot(3, false, 2))
        assertContentEquals(intArrayOf(1, 2, 3), geri.colorsFor(BeadType.BALL7, false))
        assertContentEquals(intArrayOf(10, 11, 12, 13, 14), geri.colorsFor(BeadType.SOROBAN2, true))
        assertEquals(20, geri.frameColors["c0"])
        assertEquals(argb(0xFFF58529), geri.frameColors["c1"])
        assertEquals(23, geri.frameColors["c3"])
    }

    @Test
    fun cerceveRenkleriniYereleUygulama() {
        val store = BellekAbacusPrefsStore()
        AbacusCustomizationFirestore.applySnapshotToLocalPreferences(
            store, null, AbacusCustomizationSnapshot(frameType = FrameType.FRAME_BG2, frameColors = mapOf("wood" to 1, "heart" to 2, "star" to 3)),
        )
        assertEquals(1, store.degerler["frame_bg2_wood"])
        assertEquals(2, store.degerler["frame_bg2_heart"])
        assertFalse(store.degerler.containsKey("frame_bg3_star"))
        AbacusCustomizationFirestore.applySnapshotToLocalPreferences(
            store, null, AbacusCustomizationSnapshot(frameType = FrameType.FRAME_BG3, frameColors = mapOf("wood" to 4, "star" to 5)),
        )
        assertEquals(4, store.degerler["frame_bg3_wood"])
        assertEquals(5, store.degerler["frame_bg3_star"])
        AbacusCustomizationFirestore.applySnapshotToLocalPreferences(
            store, null, AbacusCustomizationSnapshot(frameType = FrameType.FRAME_BG, frameColors = mapOf("wood" to 6)),
        )
        assertEquals(6, store.degerler["frame_bg_wood"])
        assertEquals("FRAME_BG", store.degerler["frame_type"])
    }
}

class AbacusPurchasesTest {
    @Test
    fun boncukDokumani() {
        assertEquals("users/u/haveBead", BeadPurchaseFirestore.collectionPath("u"))
        assertEquals("users/u/haveBead/BALL1", BeadPurchaseFirestore.docPath("u", "BALL1"))
        assertEquals(BeadData(10, true), BeadPurchaseFirestore.beadDataFromDocument(mapOf("count" to 10L, "colorFeatureActive" to true)))
        // count yok + purchasedAt var → eski kayıt: 25
        assertEquals(BeadData(25, false), BeadPurchaseFirestore.beadDataFromDocument(mapOf("purchasedAt" to 1L)))
        assertEquals(BeadData(25, false), BeadPurchaseFirestore.beadDataFromDocument(mapOf("purchasedAt" to null)))
        assertEquals(BeadData(0, false), BeadPurchaseFirestore.beadDataFromDocument(emptyMap()))
        // Yalnızca renk özelliği alınmış (hiç boncuk yok)
        assertEquals(BeadData(0, true), BeadPurchaseFirestore.beadDataFromDocument(mapOf("colorFeatureActive" to true)))
        assertEquals(BeadData(0, false), BeadPurchaseFirestore.beadDataFromDocument(mapOf("count" to 0L, "purchasedAt" to 1L)))
        assertEquals(
            mapOf("A" to BeadData(5, false)),
            BeadPurchaseFirestore.ownedBeadsFromDocuments(mapOf("A" to mapOf("count" to 5L))),
        )
        assertEquals(mapOf<String, Any>("count" to 15), BeadPurchaseFirestore.beadCountData(15))
        assertEquals(mapOf<String, Any>("colorFeatureActive" to true), BeadPurchaseFirestore.colorFeatureActiveData())
    }

    @Test
    fun cerceveDokumani() {
        assertEquals("users/u/haveFrame/FRAME_BG2", FramePurchaseFirestore.docPath("u", FrameType.FRAME_BG2))
        assertEquals(FrameData(true, false), FramePurchaseFirestore.frameDataFromDocument(mapOf("purchasedAt" to 1L)))
        assertEquals(FrameData(false, true), FramePurchaseFirestore.frameDataFromDocument(mapOf("colorFeatureActive" to true)))
        assertEquals(FrameData(false, false), FramePurchaseFirestore.frameDataFromDocument(emptyMap()))
        assertEquals(mapOf("FRAME_BG4" to FrameData(true, true)),
            FramePurchaseFirestore.ownedFramesFromDocuments(mapOf("FRAME_BG4" to mapOf("purchasedAt" to 5L, "colorFeatureActive" to true))))
    }

    @Test
    fun boncukFiyatlari() {
        val r = AbacusCustomizationRules
        assertEquals(0, r.getBeadPrice(BeadType.SOROBAN))
        for (t in listOf(BeadType.SOROBAN2, BeadType.SOROBAN6)) assertEquals(2000, r.getBeadPrice(t), t.name)
        for (t in listOf(BeadType.BOWLING, BeadType.BALL1, BeadType.BALL3, BeadType.BALL4, BeadType.BALL5, BeadType.BALL6, BeadType.BALL7)) {
            assertEquals(3000, r.getBeadPrice(t), t.name)
        }
        for (t in listOf(BeadType.BALL8, BeadType.BALL9, BeadType.BALL10, BeadType.BALL11, BeadType.BALL12, BeadType.BALL13, BeadType.BALL14)) {
            assertEquals(4000, r.getBeadPrice(t), t.name)
        }
        for (t in listOf(BeadType.ANIMAL3, BeadType.ANIMAL8)) {
            assertEquals(40, r.getBeadPrice(t), t.name)
            assertTrue(r.isKeyCurrency(t))
            assertEquals("key", r.currencyIconResName(t))
            assertEquals("Yetersiz anahtar", r.notEnoughMessage(t))
        }
        for (t in listOf(BeadType.ANIMAL, BeadType.ANIMAL2, BeadType.ANIMAL4, BeadType.ANIMAL5, BeadType.ANIMAL6, BeadType.ANIMAL7, BeadType.ANIMAL9)) {
            assertEquals(5000, r.getBeadPrice(t), t.name)
            assertFalse(r.isKeyCurrency(t))
        }
        assertEquals("gold_ic", r.currencyIconResName(BeadType.BALL1))
        assertEquals("Yetersiz altın", r.notEnoughMessage(BeadType.BALL1))
        assertEquals(2000, r.COLOR_FEATURE_PRICE)
        // Renk özelliği fiyatı boncuğun para birimini izler (güncel Android: getBeadColorFeaturePrice)
        assertEquals(40, r.COLOR_FEATURE_KEY_PRICE)
        assertEquals(40, r.getBeadColorFeaturePrice(BeadType.ANIMAL3))
        assertEquals(40, r.getBeadColorFeaturePrice(BeadType.ANIMAL8))
        assertEquals(2000, r.getBeadColorFeaturePrice(BeadType.BALL1))
        assertEquals(2000, r.getBeadColorFeaturePrice(BeadType.SOROBAN))
        assertEquals("key", r.colorFeatureCurrencyIconResName(BeadType.ANIMAL3))
        assertEquals("gold_ic", r.colorFeatureCurrencyIconResName(BeadType.ANIMAL))
    }

    @Test
    fun yeterliBakiye() {
        val r = AbacusCustomizationRules
        assertTrue(r.hasEnough(BeadType.ANIMAL3, cachedKeys = 40, cachedCurrency = 0))
        assertFalse(r.hasEnough(BeadType.ANIMAL3, cachedKeys = 39, cachedCurrency = 999999))
        assertTrue(r.hasEnough(BeadType.BALL8, cachedKeys = 0, cachedCurrency = 4000))
        assertFalse(r.hasEnough(BeadType.BALL8, cachedKeys = 999, cachedCurrency = 3999))
    }

    @Test
    fun ucretliKartListesi() {
        val liste = AbacusCustomizationRules.PAID_BEAD_INFO
        assertEquals(25, liste.size)
        assertEquals(BeadType.entries.toSet() - BeadType.SOROBAN, liste.map { it.first }.toSet())
        assertEquals(BeadType.SOROBAN2 to "soroban_bead2", liste[0])
        assertEquals(BeadType.BOWLING to "bowling_ball_ic", liste[2])
        assertEquals(BeadType.BALL14 to "abacus_bead_ball14", liste[15])
        assertEquals(BeadType.ANIMAL to "animal_head_ic1", liste[16])
        assertEquals(BeadType.ANIMAL9 to "animal_head_ic9", liste[24])
        assertEquals("soroban_bead", AbacusCustomizationRules.beadImageResName(BeadType.SOROBAN))
        assertEquals("soroban_bead6", AbacusCustomizationRules.beadImageResName(BeadType.SOROBAN6))
    }

    @Test
    fun sahiplikVeStok() {
        val r = AbacusCustomizationRules
        val sahip = mapOf("BALL1" to BeadData(5, false), "ANIMAL" to BeadData(25, true), "BALL3" to BeadData(0, true))
        assertTrue(r.isBeadOwned(sahip, BeadType.BALL1))
        assertFalse(r.isBeadOwned(sahip, BeadType.BALL3))
        assertFalse(r.isBeadOwned(sahip, BeadType.BALL4))
        assertEquals(25, r.ownedCount(emptyMap(), BeadType.SOROBAN))
        assertEquals(5, r.ownedCount(sahip, BeadType.BALL1))
        assertEquals(0, r.ownedCount(sahip, BeadType.BALL9))
        assertTrue(r.isStockMaxed(sahip, BeadType.ANIMAL))
        assertTrue(r.isStockMaxed(emptyMap(), BeadType.SOROBAN))
        assertFalse(r.isStockMaxed(sahip, BeadType.BALL1))
        assertEquals(5, r.countAfterPurchase(0))
        assertEquals(25, r.countAfterPurchase(20))
        assertEquals(29, r.countAfterPurchase(24)) // kırpma yok (Android ile aynı)

        val yeni = r.withBeadCount(sahip, BeadType.BALL3, 5)
        assertEquals(BeadData(5, true), yeni["BALL3"]) // renk özelliği korunur
        assertEquals(BeadData(5, false), r.withBeadCount(emptyMap(), BeadType.BALL9, 5)["BALL9"])
        assertEquals(BeadData(0, true), r.withBeadColorFeature(emptyMap(), BeadType.BALL9)["BALL9"])
        assertEquals(BeadData(5, true), r.withBeadColorFeature(sahip, BeadType.BALL1)["BALL1"])

        assertTrue(r.isBeadColorFeatureActive(sahip, BeadType.ANIMAL))
        assertFalse(r.isBeadColorFeatureActive(sahip, BeadType.BALL1))
        assertFalse(r.isBeadColorFeatureActive(sahip, BeadType.SOROBAN)) // ücretsiz boncukta da satın alınmalı
    }

    @Test
    fun yuvayaYerlestirme() {
        val r = AbacusCustomizationRules
        val store = BellekAbacusPrefsStore()
        val sahip = mapOf("BALL1" to BeadData(5, false))
        // Başlangıçta 25 yuvanın hepsi SOROBAN
        assertEquals(25, r.placedCount(store, "u", BeadType.SOROBAN))
        assertEquals(0, r.availableCount(sahip, store, "u", BeadType.SOROBAN))
        assertEquals(5, r.availableCount(sahip, store, "u", BeadType.BALL1))
        // Zaten SOROBAN olan yuvaya SOROBAN: yok sayılır
        assertEquals(AbacusCustomizationRules.PlaceResult.SAME_TYPE_IGNORED, r.placeBead(sahip, store, "u", BeadType.SOROBAN, 0, true))
        // Sahip olunmayan tip: envanter yok
        assertEquals(AbacusCustomizationRules.PlaceResult.NO_BEADS_LEFT, r.placeBead(sahip, store, "u", BeadType.BALL3, 0, true))

        assertEquals(AbacusCustomizationRules.PlaceResult.PLACED, r.placeBead(sahip, store, "u", BeadType.BALL1, 0, true))
        assertEquals(BeadType.BALL1, AbacusPreferences.getBeadTypeForSlot(store, "u", 0, true))
        assertEquals(4, r.availableCount(sahip, store, "u", BeadType.BALL1))
        assertEquals(AbacusCustomizationRules.PlaceResult.SAME_TYPE_IGNORED, r.placeBead(sahip, store, "u", BeadType.BALL1, 0, true))
        for (i in 0..3) assertEquals(AbacusCustomizationRules.PlaceResult.PLACED, r.placeBead(sahip, store, "u", BeadType.BALL1, 1, false, i))
        assertEquals(0, r.availableCount(sahip, store, "u", BeadType.BALL1))
        assertEquals(AbacusCustomizationRules.PlaceResult.NO_BEADS_LEFT, r.placeBead(sahip, store, "u", BeadType.BALL1, 2, true))
        // SOROBAN: 25 − 20 yerleşik = 5 boşta → geri konabilir
        assertEquals(5, r.availableCount(sahip, store, "u", BeadType.SOROBAN))
        assertEquals(AbacusCustomizationRules.PlaceResult.PLACED, r.placeBead(sahip, store, "u", BeadType.SOROBAN, 0, true))
        assertEquals(1, r.availableCount(sahip, store, "u", BeadType.BALL1))
    }

    @Test
    fun cerceveKurallari() {
        val r = AbacusCustomizationRules
        assertEquals(0, r.getFramePrice(FrameType.FRAME_BG))
        assertEquals(2000, r.getFramePrice(FrameType.FRAME_BG2))
        assertEquals(2000, r.getFramePrice(FrameType.FRAME_BG3))
        assertEquals(5000, r.getFramePrice(FrameType.FRAME_BG4))

        val sahip = mapOf("FRAME_BG2" to FrameData(true, false), "FRAME_BG3" to FrameData(false, true))
        assertTrue(r.isFrameSelectable(emptyMap(), FrameType.FRAME_BG))
        assertTrue(r.isFrameSelectable(sahip, FrameType.FRAME_BG2))
        assertFalse(r.isFrameSelectable(sahip, FrameType.FRAME_BG3))
        assertFalse(r.isFrameSelectable(sahip, FrameType.FRAME_BG4))
        assertTrue(r.isFrameColorUnlocked(emptyMap(), FrameType.FRAME_BG))
        assertFalse(r.isFrameColorUnlocked(sahip, FrameType.FRAME_BG2))
        assertTrue(r.isFrameColorUnlocked(sahip, FrameType.FRAME_BG3))
        assertEquals(FrameData(true, true), r.withFrameOwned(sahip, FrameType.FRAME_BG3)["FRAME_BG3"])
        assertEquals(FrameData(true, false), r.withFrameOwned(emptyMap(), FrameType.FRAME_BG4)["FRAME_BG4"])
        assertEquals(FrameData(true, true), r.withFrameColorFeature(sahip, FrameType.FRAME_BG2)["FRAME_BG2"])
    }

    @Test
    fun sesSecimi() {
        assertEquals(1.0f, AbacusSoundPlayer.playbackVolumeFor(4))
        for (i in listOf(1, 2, 3, 5, 0, 9)) assertEquals(0.6f, AbacusSoundPlayer.playbackVolumeFor(i))
        assertEquals("abacus_sound1", AbacusSoundPlayer.soundResFor(1))
        assertEquals("abacus_sound4", AbacusSoundPlayer.soundResFor(4))
        assertEquals("abacus_sound5", AbacusSoundPlayer.soundResFor(5))
        assertEquals("abacus_sound1", AbacusSoundPlayer.soundResFor(-3))
        assertEquals("abacus_sound5", AbacusSoundPlayer.soundResFor(12))
        assertEquals(1.0f, AbacusSoundPlayer.playbackRate(true))
        assertEquals(0.75f, AbacusSoundPlayer.playbackRate(false))
        assertEquals(1, AbacusSoundPlayer.selectedIndex(null))
        assertEquals(5, AbacusSoundPlayer.selectedIndex(9))
        assertEquals(1, AbacusSoundPlayer.selectedIndex(0))
        assertEquals(3, AbacusSoundPlayer.selectedIndex(3))
    }
}

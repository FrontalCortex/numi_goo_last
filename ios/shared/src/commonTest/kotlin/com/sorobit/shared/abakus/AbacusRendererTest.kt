// AbacusBeadRenderer / AbacusFrameRenderer'ın saf kısımlarının (kaynak adı seçimi, özgün renk
// eşlemeleri, piksel renk değiştirme, HSV yardımcıları) Android ile aynı sonucu verdiğini doğrular.
// Beklenen değerler Android kaynağındaki formüllerden (ve Color.HSVToColor / colorToHSV'nin
// dayandığı Skia formüllerinden) elle hesaplanmıştır.
package com.sorobit.shared.abakus

import com.sorobit.shared.abakus.AbacusPreferences.BeadType
import com.sorobit.shared.abakus.AbacusPreferences.FrameType
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun argb(hex: Long): Int = hex.toInt()

class AbacusBeadRendererTest {
    private val r = AbacusBeadRenderer

    @Test
    fun cizimTuruVeKaynakAdlari() {
        assertEquals(AbacusBeadRenderer.BeadRenderKind.TINTED_LAYERS, r.renderKind(BeadType.SOROBAN))
        assertEquals(AbacusBeadRenderer.BeadRenderKind.GRADIENT_PILL, r.renderKind(BeadType.SOROBAN2))
        assertEquals(AbacusBeadRenderer.BeadRenderKind.REPLACED_BITMAP, r.renderKind(BeadType.SOROBAN6))
        assertEquals(AbacusBeadRenderer.BeadRenderKind.REPLACED_BITMAP, r.renderKind(BeadType.ANIMAL))
        assertEquals(AbacusBeadRenderer.BeadRenderKind.REPLACED_BITMAP, r.renderKind(BeadType.BALL14))

        assertContentEquals(
            arrayOf("soroban_bead_layer1", "soroban_bead_layer2", "soroban_bead_layer3"),
            r.sorobanLayerResNames(false),
        )
        assertContentEquals(
            arrayOf("soroban_bead_selected_layer1", "soroban_bead_selected_layer2", "soroban_bead_selected_layer3"),
            r.sorobanLayerResNames(true),
        )

        assertNull(r.beadResName(BeadType.SOROBAN, false))
        assertNull(r.beadResName(BeadType.SOROBAN2, true))
        assertEquals("animal_head_ic1", r.beadResName(BeadType.ANIMAL, false))
        assertEquals("animal_head_selected_ic1", r.beadResName(BeadType.ANIMAL, true))
        assertEquals("animal_head_ic9", r.beadResName(BeadType.ANIMAL9, false))
        assertEquals("animal_head_selected_ic5", r.beadResName(BeadType.ANIMAL5, true))
        assertEquals("bowling_ball_ic", r.beadResName(BeadType.BOWLING, false))
        assertEquals("bowling_ball_selected_ic", r.beadResName(BeadType.BOWLING, true))
        assertEquals("abacus_bead_ball1", r.beadResName(BeadType.BALL1, false))
        assertEquals("abacus_bead_selected_ball14", r.beadResName(BeadType.BALL14, true))
        assertEquals("abacus_bead_ball10", r.beadResName(BeadType.BALL10, false))
        assertEquals("soroban_bead6", r.beadResName(BeadType.SOROBAN6, false))
        assertEquals("soroban_bead6_selected", r.beadResName(BeadType.SOROBAN6, true))
    }

    @Test
    fun ozgunRenkEslemeleri() {
        assertNull(r.originalColorMap(BeadType.SOROBAN, false))
        assertNull(r.originalColorMap(BeadType.SOROBAN2, false))
        assertEquals(
            mapOf(argb(0xFFFFFFFF) to 0, argb(0xFF777873) to 1, argb(0xFF4D4C48) to 2, argb(0xFF32302D) to 3),
            r.originalColorMap(BeadType.ANIMAL, false),
        )
        assertEquals(
            mapOf(argb(0xFFB2B2B2) to 0, argb(0xFF535450) to 1, argb(0xFF353532) to 2, argb(0xFF23211F) to 3),
            r.originalColorMap(BeadType.ANIMAL, true),
        )
        assertEquals(
            mapOf(argb(0xFF010102) to 0, argb(0xFF3C3C3C) to 1, argb(0xFFFFFFFF) to 2),
            r.originalColorMap(BeadType.BALL14, false),
        )
        // Her bitmap tipinin eşlemesi var ve dizinleri o tipin renk dizisi sınırları içinde
        // (replaceBeadPixels `newColors[dizin]` okur).
        for (tip in BeadType.entries) {
            if (tip == BeadType.SOROBAN || tip == BeadType.SOROBAN2) continue
            val boyut = AbacusPreferences.beadColorSpec(tip).size
            for (secili in listOf(false, true)) {
                val esleme = assertNotNull(r.originalColorMap(tip, secili), tip.name)
                assertTrue(esleme.isNotEmpty(), tip.name)
                assertTrue(esleme.values.all { it in 0 until boyut }, "${tip.name} secili=$secili")
                assertNotNull(r.beadResName(tip, secili), tip.name)
            }
        }
    }

    @Test
    fun kullanilanTipler() {
        val store = BellekAbacusPrefsStore()
        assertEquals(setOf(BeadType.SOROBAN), r.usedBeadTypes(store, "u"))
        AbacusPreferences.setBeadType(store, "u", BeadType.BALL5)
        AbacusPreferences.setBeadTypeForSlot(store, "u", 3, isTop = false, beadIndex = 1, type = BeadType.ANIMAL2)
        assertEquals(setOf(BeadType.SOROBAN, BeadType.BALL5, BeadType.ANIMAL2), r.usedBeadTypes(store, "u"))
    }

    @Test
    fun rasterOlculeri() {
        assertEquals(120, r.replacedBitmapTargetWidth(1f))
        assertEquals(330, r.replacedBitmapTargetWidth(2.75f))
        assertEquals(1, r.replacedBitmapTargetWidth(0f))
        assertEquals(165, r.replacedBitmapTargetHeight(330, 100, 50))
        assertEquals(240, r.replacedBitmapTargetHeight(120, 1, 2))
        assertEquals(120, r.replacedBitmapTargetHeight(120, 0, 0)) // 0 → 1'e çekilir
        assertEquals(1, r.replacedBitmapTargetHeight(1, 100, 1))
        assertEquals(6760, r.bitmapCacheKey(7, intArrayOf(1, 2))) // 31·(31·7+1)+2
        assertEquals(7, r.bitmapCacheKey(7, intArrayOf()))
        assertEquals(1, r.soroban2CacheSeed(false))
        assertEquals(2, r.soroban2CacheSeed(true))
        assertEquals(48, r.MAX_BITMAP_CACHE_ENTRIES)
        assertEquals(150, r.COLOR_TOLERANCE)
        assertEquals(120, r.RABBIT_RENDER_DP)

        val g = r.soroban2Geometry(1f)
        assertEquals(120, g.targetW)
        assertEquals(92, g.targetH) // 120 × 92/120
        assertEquals(36f, g.outerRx)
        assertEquals(12f, g.padX)
        assertEquals(24f, g.innerRx)
        // Dikey ölçek yükseklik/72 (Android'deki tutarsızlık korunur): 92/72 ≈ 1.2778
        assertTrue(abs(g.outerRy - 46f) < 0.001f)
        assertTrue(abs(g.padY - 15.3333f) < 0.001f)
        assertTrue(abs(g.innerRy - 30.6667f) < 0.001f)
        assertContentEquals(floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f), r.SOROBAN2_GRADIENT_STOPS)
    }

    @Test
    fun pikselDegistirmeDuzRenkVeKarisim() {
        val kirmizi = argb(0xFFFF0000)
        val esleme = linkedMapOf(kirmizi to 0, argb(0xFF0000FF) to 1)
        val yeni = intArrayOf(argb(0xFF00FF00), argb(0xFFFFFF00)) // yeşil, sarı
        val p = intArrayOf(
            argb(0x05FF0000), kirmizi, kirmizi,
            kirmizi, argb(0x40800080), kirmizi,
            kirmizi, argb(0xFF0000FF), kirmizi,
        )
        r.replaceBeadPixels(p, 3, 3, esleme, yeni)
        // Alfa < 10: dokunulmaz (ve kenarda olduğu için arka plan sayılır)
        assertEquals(argb(0x05FF0000), p[0])
        // Düz kırmızı → yeşil, düz mavi → sarı
        assertEquals(argb(0xFF00FF00), p[1])
        assertEquals(argb(0xFFFFFF00), p[7])
        // Kırmızı-mavi ortası (t = 0.5) → yeşil-sarı ortası: (127, 255, 0);
        // arka plana bağlı olmayan yarı saydam piksel iç dikiştir → alfa 255
        assertEquals(argb(0xFF7FFF00), p[4])
        for (i in listOf(2, 3, 5, 6, 8)) assertEquals(argb(0xFF00FF00), p[i], "i=$i")
    }

    @Test
    fun pikselDegistirmeArkaPlanaBagliYariSaydamKorunur() {
        val kirmizi = argb(0xFFFF0000)
        val esleme = linkedMapOf(kirmizi to 0, argb(0xFF0000FF) to 1)
        val yeni = intArrayOf(argb(0xFF00FF00), argb(0xFFFFFF00))
        val p = intArrayOf(
            kirmizi, argb(0x20FF0000), kirmizi,
            kirmizi, argb(0x40800080), kirmizi,
            kirmizi, kirmizi, kirmizi,
        )
        r.replaceBeadPixels(p, 3, 3, esleme, yeni)
        // Kenardaki yarı saydam piksel: rengi değişir, alfası korunur (arka plan)
        assertEquals(argb(0x2000FF00), p[1])
        // Ona komşu yarı saydam merkez de arka plandır → alfası yükseltilmez
        assertEquals(argb(0x407FFF00), p[4])
        assertEquals(argb(0xFF00FF00), p[0])
    }

    @Test
    fun pikselDegistirmeYeniRenginAlfasiniAtar() {
        val esleme = linkedMapOf(argb(0xFF102030) to 0)
        val p = intArrayOf(argb(0xFF112131))
        r.replaceBeadPixels(p, 1, 1, esleme, intArrayOf(argb(0x00ABCDEF)))
        assertEquals(argb(0xFFABCDEF), p[0]) // pikselin alfası + yeni rengin RGB'si
    }

    @Test
    fun hsvDonusumu() {
        val hsv = FloatArray(3)
        r.colorToHSV(argb(0xFFFF0000), hsv); assertContentEquals(floatArrayOf(0f, 1f, 1f), hsv)
        r.colorToHSV(argb(0xFF00FF00), hsv); assertContentEquals(floatArrayOf(120f, 1f, 1f), hsv)
        r.colorToHSV(argb(0xFF0000FF), hsv); assertContentEquals(floatArrayOf(240f, 1f, 1f), hsv)
        r.colorToHSV(argb(0xFFFFFFFF), hsv); assertContentEquals(floatArrayOf(0f, 0f, 1f), hsv)
        r.colorToHSV(argb(0xFF000000), hsv); assertContentEquals(floatArrayOf(0f, 0f, 0f), hsv)
        r.colorToHSV(argb(0x00808080), hsv) // alfa yok sayılır
        assertEquals(0f, hsv[0]); assertEquals(0f, hsv[1]); assertEquals(128f / 255f, hsv[2])
        // #336699: maks = 153 (mavi), delta = 102 → ton = (4 − 0.5)·60 = 210, doygunluk 102/153, değer 0.6
        r.colorToHSV(argb(0xFF336699), hsv)
        assertEquals(210f, hsv[0]); assertEquals(102f / 153f, hsv[1]); assertEquals(153f / 255f, hsv[2])
        // Macenta-kırmızı arası negatif ton 360 eklenerek düzeltilir: #FF0080 → (0−128)/255·60 + 360
        r.colorToHSV(argb(0xFFFF0080), hsv)
        assertTrue(abs(hsv[0] - 329.88235f) < 0.001f)

        assertEquals(argb(0xFFFF0000), r.HSVToColor(floatArrayOf(0f, 1f, 1f)))
        assertEquals(argb(0xFF00FF00), r.HSVToColor(floatArrayOf(120f, 1f, 1f)))
        assertEquals(argb(0xFF0000FF), r.HSVToColor(floatArrayOf(240f, 1f, 1f)))
        assertEquals(argb(0xFFFFFF00), r.HSVToColor(floatArrayOf(60f, 1f, 1f)))
        assertEquals(argb(0xFF00FFFF), r.HSVToColor(floatArrayOf(180f, 1f, 1f)))
        assertEquals(argb(0xFFFF00FF), r.HSVToColor(floatArrayOf(300f, 1f, 1f)))
        // Aralık dışı ton 0 sayılır; doygunluk/değer kırpılır
        assertEquals(argb(0xFFFF0000), r.HSVToColor(floatArrayOf(360f, 1f, 1f)))
        assertEquals(argb(0xFFFF0000), r.HSVToColor(floatArrayOf(-10f, 2f, 5f)))
        // Doygunluk 0 → gri; 0.5·255 = 127.5 → 128
        assertEquals(argb(0xFF808080), r.HSVToColor(floatArrayOf(77f, 0f, 0.5f)))
        assertEquals(argb(0xFF336699), r.HSVToColor(floatArrayOf(210f, 2f / 3f, 0.6f)))
    }

    @Test
    fun renkUretimi() {
        assertEquals(0.90f, r.FIXED_SATURATION)
        // Açıklık 50: tam renk (doygunluk 0.9 → diğer kanallar 25.5 → 26)
        assertEquals(argb(0xFFFF1A1A), r.makeColor(0, 50))
        assertEquals(argb(0xFF000000), r.makeColor(0, 0))
        assertEquals(argb(0xFFFFFFFF), r.makeColor(0, 100))
        assertEquals(argb(0xFFFFFFFF), r.makeColor(200, 150)) // 100'e kırpılır
        assertEquals(argb(0xFF000000), r.makeColor(200, -5)) // 0'a kırpılır
        // Ton 120, açıklık 25: değer 0.5 → (13, 128, 13)
        assertEquals(argb(0xFF0D800D), r.makeColor(120, 25))
        assertEquals(r.makeColor(120, 25), r.makeColor(480, 25)) // ton mod 360
        assertEquals(r.makeColor(0, 50), r.makeColor(-10, 50)) // negatif ton → 0
        // Açıklık 75: doygunluk 0.45, değer 1 → ton 240: (140, 140, 255)
        assertEquals(argb(0xFF8C8CFF), r.makeColor(240, 75))
    }

    @Test
    fun tonVeAciklikOkuma() {
        assertEquals(210, r.getHue(argb(0xFF336699)))
        assertEquals(0, r.getHue(argb(0xFF808080)))
        assertEquals(120, r.getHue(argb(0xFF00FF00)))
        assertEquals(329, r.getHue(argb(0xFFFF0080)))

        assertEquals(50, r.getLightness100(argb(0xFFFF0000)))
        assertEquals(25, r.getLightness100(argb(0xFF800000))) // 50·(128/255) = 25.09
        assertEquals(100, r.getLightness100(argb(0xFFFFFFFF)))
        // DİKKAT (Android davranışı): siyahın doygunluğu 0 olduğundan açıklığı 100 okunur
        assertEquals(100, r.getLightness100(argb(0xFF000000)))
        // makeColor ile gidiş-dönüş (üst yarı): doygunluk 0.45 → 50 + 50·(1 − 0.45/0.9) = 75
        val l = r.getLightness100(r.makeColor(240, 75))
        assertTrue(l in 74..76, "l=$l")
    }

    @Test
    fun tonKaydirma() {
        // Gri: doygunluk 0.65'e çıkarılır; değer 128/255 kalır → (45, 128, 45)
        assertEquals(argb(0xFF2D802D), r.shiftHue(argb(0xFF808080), 120))
        assertEquals(argb(0x802D802D), r.shiftHue(argb(0x80808080), 120)) // alfa korunur
        assertEquals(r.shiftHue(argb(0xFF808080), 120), r.shiftHue(argb(0xFF808080), 480))
        // Siyah: değer 0.35'e çıkarılır → (89, 31, 31)
        assertEquals(argb(0xFF591F1F), r.shiftHue(argb(0xFF000000), 0))
        // Zaten doygun ve parlak renk: yalnızca ton değişir (kırmızı → mavi)
        assertEquals(argb(0xFF0000FF), r.shiftHue(argb(0xFFFF0000), 240))
        // Özel en düşük doygunluk: 0 → gri aynen kalır
        assertEquals(argb(0xFF808080), r.shiftHue(argb(0xFF808080), 120, minSaturation = 0f))
    }
}

class AbacusFrameRendererTest {
    private val f = AbacusFrameRenderer

    @Test
    fun sabitlerVeKaynakAdlari() {
        assertEquals(argb(0xFFDDA379), f.ORIG_WOOD)
        assertEquals(argb(0xFFE63946), f.ORIG_HEART)
        assertEquals(argb(0xFFDDFF33), f.ORIG_STAR)
        assertEquals(argb(0xFF000000), f.ORIG_BLACK)
        assertEquals(argb(0xFF212226), f.ORIG_DARK)
        assertContentEquals(intArrayOf(f.ORIG_WOOD, f.ORIG_HEART, f.ORIG_BLACK, f.ORIG_DARK), f.ALL_ORIG)
        assertContentEquals(intArrayOf(f.ORIG_WOOD, f.ORIG_STAR, f.ORIG_BLACK, f.ORIG_DARK), f.ALL_ORIG_BG3)
        assertContentEquals(intArrayOf(0, 1, 0, 2, 2, 3), f.EDGES)
        assertEquals(400, f.RENDER_SIZE)
        assertEquals("abacus_frame_bg", f.frameResName(FrameType.FRAME_BG))
        assertEquals("abacus_frame_bg2", f.frameResName(FrameType.FRAME_BG2))
        assertEquals("abacus_frame_bg3", f.frameResName(FrameType.FRAME_BG3))
        assertNull(f.frameResName(FrameType.FRAME_BG4))
        assertEquals(4, f.BG4_LAYERS.size)
        assertEquals(AbacusFrameRenderer.RoundRectLayer(0f, 0f, 400f, 400f, 40f, argb(0xFF000000)), f.BG4_LAYERS[0])
        assertEquals(AbacusFrameRenderer.RoundRectLayer(5.6f, 5.6f, 394.4f, 394.4f, 34.4f, null), f.BG4_LAYERS[1])
        assertEquals(AbacusFrameRenderer.RoundRectLayer(28f, 28f, 372f, 372f, 12f, argb(0xFF000000)), f.BG4_LAYERS[2])
        assertEquals(AbacusFrameRenderer.RoundRectLayer(33.6f, 33.6f, 366.4f, 366.4f, 6.4f, argb(0xFF212226)), f.BG4_LAYERS[3])
        assertContentEquals(floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f), f.BG4_GRADIENT_STOPS)
    }

    @Test
    fun tipVeRenkCozumleme() {
        val store = BellekAbacusPrefsStore()
        assertEquals(FrameType.FRAME_BG, f.resolveFrameType(store))
        assertEquals(FrameType.FRAME_BG3, f.resolveFrameType(store, FrameType.FRAME_BG3))
        AbacusPreferences.setFrameType(store, FrameType.FRAME_BG2)
        assertEquals(FrameType.FRAME_BG2, f.resolveFrameType(store))

        assertContentEquals(intArrayOf(argb(0xFFDDA379)), f.resolveFrameColors(store, FrameType.FRAME_BG))
        assertContentEquals(intArrayOf(7), f.resolveFrameColors(store, FrameType.FRAME_BG, intArrayOf(7, 8)))
        // Eksik override elemanı kayıtlı tercihe düşer
        AbacusPreferences.setFrameBg2HeartColor(store, 99)
        assertContentEquals(intArrayOf(5, 99), f.resolveFrameColors(store, FrameType.FRAME_BG2, intArrayOf(5)))
        assertContentEquals(intArrayOf(5, 6), f.resolveFrameColors(store, FrameType.FRAME_BG2, intArrayOf(5, 6)))
        assertContentEquals(intArrayOf(argb(0xFFDDA379), argb(0xFFDDFF33)), f.resolveFrameColors(store, FrameType.FRAME_BG3))
        assertContentEquals(intArrayOf(1, argb(0xFFDDFF33)), f.resolveFrameColors(store, FrameType.FRAME_BG3, intArrayOf(1)))
        assertContentEquals(AbacusPreferences.FRAME_BG4_DEFAULT_COLORS, f.resolveFrameColors(store, FrameType.FRAME_BG4))
        // BG4'te override olduğu gibi kullanılır
        assertContentEquals(intArrayOf(1, 2), f.resolveFrameColors(store, FrameType.FRAME_BG4, intArrayOf(1, 2)))
        assertContentEquals(intArrayOf(1, 2, f.ORIG_BLACK, f.ORIG_DARK), f.newColorsFor(1, 2))
    }

    @Test
    fun dogrusalKaristirma() {
        assertEquals(10, f.lerp(10, 20, 0f))
        assertEquals(20, f.lerp(10, 20, 1f))
        assertEquals(128, f.lerp(0, 255, 0.5f)) // 127.5 + 0.5
        assertEquals(100, f.lerp(200, 100, 1f))
        assertEquals(150, f.lerp(200, 100, 0.5f))
        assertEquals(255, f.lerp(0, 255, 2f)) // kırpılır
        assertEquals(0, f.lerp(0, 255, -1f))
    }

    @Test
    fun cercevePikselDegistirme() {
        val yeni = f.newColorsFor(argb(0xFF112233), argb(0xFF445566))
        val p = intArrayOf(
            argb(0xFFDDA379), // ahşap → yeni ahşap
            argb(0xFFE63946), // kalp → yeni kalp
            argb(0xFF000000), // siyah kalır
            argb(0xFF212226), // koyu kalır
            argb(0x80DDA379), // alfa korunur
            argb(0x05DDA379), // alfa < 10: dokunulmaz
            argb(0xFFE26E60), // ahşap-kalp ortası (226,110,96): t ≈ 0.4985
        )
        f.replaceFramePixels(p, f.ALL_ORIG, yeni)
        assertEquals(argb(0xFF112233), p[0])
        assertEquals(argb(0xFF445566), p[1])
        assertEquals(argb(0xFF000000), p[2])
        assertEquals(argb(0xFF212226), p[3])
        assertEquals(argb(0x80112233), p[4])
        assertEquals(argb(0x05DDA379), p[5])
        // r = 17 + 51·t → 42, g = 34 + 51·t → 59, b = 51 + 51·t → 76
        assertEquals(argb(0xFF2A3B4C), p[6])
    }

    @Test
    fun yildizliCercevePikselDegistirme() {
        val yeni = f.newColorsFor(argb(0xFF112233), argb(0xFF445566))
        val p = intArrayOf(argb(0xFFDDFF33), argb(0xFFDDA379))
        f.replaceFramePixels(p, f.ALL_ORIG_BG3, yeni)
        assertEquals(argb(0xFF445566), p[0])
        assertEquals(argb(0xFF112233), p[1])
    }
}

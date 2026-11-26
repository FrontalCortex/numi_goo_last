// AbacusCustomizationUiLogic'in Android'deki AbacusCustomizationFragment kararlarıyla aynı sonucu
// verdiğini doğrular. Beklenen değerler Fragment kaynağından ve layout XML'lerinden elle alınmıştır.
package com.sorobit.shared.abakus

import com.sorobit.shared.abakus.AbacusPreferences.BeadType
import com.sorobit.shared.abakus.AbacusPreferences.FrameType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AbacusCustomizationUiLogicTest {
    private val ui = AbacusCustomizationUiLogic
    private val uid = "u1"

    @Test
    fun onizlemeOlculeri() {
        val m = AbacusPreviewMetrics
        // Alt boncukların üst kenarı: 250 − altBoşluk − 40.
        assertEquals(listOf(128, 152, 176, 200), (0..3).map { m.bottomBeadTopDp(it) })
        assertEquals(0, m.topBeadTopDp())
        // Bariyer: (250 − 6) / 2 − 50 = 72; yuva panelinde (250 − 16) / 2 − 50 = 67.
        assertEquals(72, m.barrierTopDp(m.BARRIER_HEIGHT_DP))
        assertEquals(67, m.barrierTopDp(m.SLOT_PANEL_BARRIER_HEIGHT_DP))
    }

    @Test
    fun kartListesiButunTipleriBirKezIcerir() {
        assertEquals(26, ui.BEAD_CARDS.size)
        assertEquals(BeadType.entries.toSet(), ui.BEAD_CARDS.map { it.beadType }.toSet())
        assertEquals(BeadType.SOROBAN, ui.BEAD_CARDS.first().beadType)
        assertEquals(BeadType.ANIMAL3, ui.BEAD_CARDS.last().beadType)
        assertEquals(BeadType.ANIMAL9, ui.BEAD_CARDS[19].beadType)
        // Kart görseli, satın alma panelindeki görselle aynıdır.
        for (card in ui.BEAD_CARDS) {
            assertEquals(AbacusCustomizationRules.beadImageResName(card.beadType), card.imageResName)
        }
        // İlk üç kart 60×46 (6dp boşluk), diğerleri 60×60 (4dp boşluk).
        assertEquals(listOf(46, 46, 46, 60), ui.BEAD_CARDS.take(4).map { it.imageHeightDp })
        assertEquals(listOf(6, 6, 6, 4), ui.BEAD_CARDS.take(4).map { it.labelMarginTopDp })
        assertEquals("Gökkuşağı", ui.BEAD_CARDS[1].label)
        assertEquals("Kaplan", ui.BEAD_CARDS.last().label)
    }

    @Test
    fun kartDurumlariVeDokunma() {
        val store = BellekAbacusPrefsStore()
        var owned: Map<String, BeadData> = emptyMap()

        var states = ui.beadCardStates(store, uid, owned)
        assertTrue(states[0].selected)
        assertFalse(states[0].locked) // SOROBAN hiç kilitli olmaz
        assertTrue(states.drop(1).all { it.locked })
        val kaplan = states.last()
        assertEquals(40, kaplan.price)
        assertEquals("key", kaplan.currencyIconResName)
        assertEquals("gold_ic", states[1].currencyIconResName)
        assertEquals(2000, states[1].price)

        // Satın alınmamış karta dokunma: tip yine de yazılır, satın alma paneli açılır.
        assertFalse(ui.onBeadCardClicked(store, uid, owned, BeadType.BALL1))
        assertEquals(BeadType.BALL1, AbacusPreferences.getBeadType(store, uid))

        owned = AbacusCustomizationRules.withBeadCount(owned, BeadType.BALL1, 5)
        assertTrue(ui.onBeadCardClicked(store, uid, owned, BeadType.BALL1))
        assertTrue(ui.onBeadCardClicked(store, uid, owned, BeadType.SOROBAN))

        states = ui.beadCardStates(store, uid, owned)
        assertFalse(states.first { it.card.beadType == BeadType.BALL1 }.locked)
        assertTrue(states[0].selected)
    }

    @Test
    fun satinAlmaDenetimleri() {
        // Altınla satılan tip.
        var c = ui.checkBeadPurchase(BeadType.BALL8, cachedKeys = 100, cachedCurrency = 3999)
        assertFalse(c.allowed)
        assertEquals("Yetersiz altın", c.message)
        assertEquals(4000, c.price)
        assertFalse(c.isKeyCurrency)
        assertTrue(ui.checkBeadPurchase(BeadType.BALL8, 0, 4000).allowed)

        // Anahtarla satılan tip.
        c = ui.checkBeadPurchase(BeadType.ANIMAL8, cachedKeys = 39, cachedCurrency = 99999)
        assertFalse(c.allowed)
        assertEquals("Yetersiz anahtar", c.message)
        assertTrue(c.isKeyCurrency)
        assertTrue(ui.checkBeadPurchase(BeadType.ANIMAL8, 40, 0).allowed)

        // Yuva paneli: önce stok.
        val dolu = mapOf(BeadType.BALL1.name to BeadData(25, false))
        c = ui.checkSlotPanelPurchase(dolu, BeadType.BALL1, 0, 0)
        assertFalse(c.allowed)
        assertEquals("Stokta kalmadı", c.message)
        // SOROBAN her zaman 25 sayılır.
        assertEquals("Stokta kalmadı", ui.checkSlotPanelPurchase(emptyMap(), BeadType.SOROBAN, 9, 9).message)
        assertTrue(ui.checkSlotPanelPurchase(mapOf(BeadType.BALL1.name to BeadData(20, false)), BeadType.BALL1, 0, 3000).allowed)

        // Sabit fiyatlı altın akışları.
        assertFalse(ui.checkGoldPurchase(2000, 1999).allowed)
        assertEquals("Yetersiz altın", ui.checkGoldPurchase(2000, 1999).message)
        assertNull(ui.checkGoldPurchase(2000, 2000).message)

        // Yeni adet.
        assertEquals(5, ui.beadCountAfterPurchase(emptyMap(), BeadType.BALL1, fromSlotPanel = false))
        assertEquals(25, ui.beadCountAfterPurchase(mapOf(BeadType.BALL1.name to BeadData(20, true)), BeadType.BALL1, true))

        // İade kuralı: yalnızca boncuk renk özelliğinde iade yok.
        assertTrue(ui.BEAD_PURCHASE_PANEL.rollbackOnSaveFailure)
        assertTrue(ui.SLOT_PANEL_PURCHASE.rollbackOnSaveFailure)
        assertFalse(ui.BEAD_COLOR_FEATURE_PANEL.rollbackOnSaveFailure)
        assertTrue(ui.FRAME_PURCHASE_PANEL.rollbackOnSaveFailure)
        assertEquals("İşlem başarısız. Tekrar deneyin.", ui.BEAD_PURCHASE_PANEL.walletFailureMessage)
        assertEquals("Kayıt hatası. Tekrar deneyin.", ui.BEAD_PURCHASE_PANEL.saveFailureMessage)
        assertEquals("Hata oluştu", ui.BEAD_COLOR_FEATURE_PANEL.saveFailureMessage)
        assertEquals("Ödeme hatası", ui.FRAME_PURCHASE_PANEL.walletFailureMessage)
    }

    @Test
    fun yuvaPaneli() {
        val store = BellekAbacusPrefsStore()
        val owned = mapOf(BeadType.BALL1.name to BeadData(5, false))

        var s = ui.slotPanelState(owned, store, uid, BeadType.BALL1)
        assertEquals(5, s.availableCount)
        assertEquals("x5", s.inventoryText)
        assertEquals(3000, s.price)
        assertFalse(s.buyButtonMaxed)

        for (i in 0..3) {
            assertTrue(ui.placeBeadInSlot(owned, store, uid, BeadType.BALL1, 0, false, i).placed)
        }
        assertTrue(ui.placeBeadInSlot(owned, store, uid, BeadType.BALL1, 0, true, 0).placed)
        // Aynı tip aynı yuvaya: sessizce yok sayılır.
        val ayni = ui.placeBeadInSlot(owned, store, uid, BeadType.BALL1, 0, true, 0)
        assertFalse(ayni.placed)
        assertNull(ayni.message)
        // Envanter bitti.
        val bitti = ui.placeBeadInSlot(owned, store, uid, BeadType.BALL1, 1, true, 0)
        assertFalse(bitti.placed)
        assertEquals("Bu boncuktan kalmadı!", bitti.message)

        s = ui.slotPanelState(owned, store, uid, BeadType.BALL1)
        assertEquals("x0", s.inventoryText)
        // SOROBAN: 25 − yerleşik 20.
        val soroban = ui.slotPanelState(owned, store, uid, BeadType.SOROBAN)
        assertEquals("x5", soroban.inventoryText)
        assertTrue(soroban.buyButtonMaxed)
        assertEquals(0, soroban.price)

        val slots = ui.slotBeads(store, uid)
        assertEquals(25, slots.size)
        assertTrue(slots[0].isTop)
        assertEquals(BeadType.BALL1, slots[0].beadType)
        assertEquals(listOf(0, 1, 2, 3), slots.subList(1, 5).map { it.beadIndex })
        assertEquals(5, slots.count { it.beadType == BeadType.BALL1 })
        assertEquals(1, slots[5].rod)
    }

    @Test
    fun boncukBoyasi() {
        val store = BellekAbacusPrefsStore()

        val soroban = ui.beadPaint(store, BeadType.SOROBAN, false)
        assertEquals("TINTED_LAYERS", soroban.kind)
        assertEquals(listOf("soroban_bead_layer1", "soroban_bead_layer2", "soroban_bead_layer3"), soroban.resNames)
        assertEquals(AbacusPreferences.SOROBAN_DEFAULT_COLORS.toList(), soroban.colors)
        assertTrue(soroban.isDefaultColors)

        val hap = ui.beadPaint(store, BeadType.SOROBAN2, true)
        assertEquals("GRADIENT_PILL", hap.kind)
        assertTrue(hap.resNames.isEmpty())
        assertEquals(5, hap.colors.size)

        val top = ui.beadPaint(store, BeadType.BALL1, true)
        assertEquals("REPLACED_BITMAP", top.kind)
        assertEquals(listOf("abacus_bead_selected_ball1"), top.resNames)

        val onceki = ui.beadPaint(store, BeadType.BALL1, false)
        ui.setBeadColorFromSliders(store, BeadType.BALL1, false, 0, 200, 50)
        val sonraki = ui.beadPaint(store, BeadType.BALL1, false)
        assertFalse(sonraki.isDefaultColors)
        assertTrue(onceki.cacheKey != sonraki.cacheKey)

        val g = ui.pillGeometry()
        assertEquals(120f, g.width)
        assertEquals(92f, g.height)
        assertEquals(36f, g.outerRadiusX)
        assertEquals(36f * 92f / 72f, g.outerRadiusY)
        assertEquals(12f, g.padX)
        assertEquals(5, g.stops.size)
    }

    @Test
    fun boncukPikselleriYenidenBoyanir() {
        val store = BellekAbacusPrefsStore()
        val orijinal = AbacusPreferences.BALL14_DEFAULT_COLORS[1]
        // 3×3: ortada özgün 2. renk, çevresi saydam.
        val pikseller = IntArray(9)
        pikseller[4] = orijinal
        val yeni = ui.setBeadColorFromSliders(store, BeadType.BALL14, false, 1, 120, 50)
        ui.recolorBeadPixels(store, BeadType.BALL14, false, pikseller, 3, 3)
        assertEquals(yeni, pikseller[4])
        assertEquals(0, pikseller[0])

        // SOROBAN (katmanlı) ve boyut uyuşmazlığı: dokunulmaz.
        val p2 = intArrayOf(orijinal)
        ui.recolorBeadPixels(store, BeadType.SOROBAN, false, p2, 1, 1)
        ui.recolorBeadPixels(store, BeadType.BALL14, false, p2, 2, 2)
        assertEquals(orijinal, p2[0])
    }

    @Test
    fun renkSatirlari() {
        val store = BellekAbacusPrefsStore()
        // Satır sayısı hiçbir tipte renk/etiket dizisini aşmaz.
        for (type in BeadType.entries) {
            val spec = AbacusPreferences.beadColorSpec(type)
            assertTrue(ui.tab2PathCount(type) <= spec.size, type.name)
            assertTrue(ui.tab2PathCount(type) <= spec.labels.size, type.name)
            assertEquals(ui.tab2PathCount(type), ui.beadColorRows(store, type, false).size)
            assertEquals(ui.tab2PathCount(type), ui.beadColorRows(store, type, true).size)
        }
        assertEquals(3, ui.tab2PathCount(BeadType.SOROBAN))
        assertEquals(6, ui.tab2PathCount(BeadType.ANIMAL5))

        val satirlar = ui.beadColorRows(store, BeadType.SOROBAN, false)
        assertEquals(listOf("Dış çerçeve", "İç zemin", "Sağ yansıma"), satirlar.map { it.label })
        assertEquals(AbacusPreferences.SOROBAN_DEFAULT_COLORS[0], satirlar[0].color)
        assertEquals(AbacusBeadRenderer.getHue(satirlar[1].color), satirlar[1].hue)
        assertEquals(AbacusBeadRenderer.getLightness100(satirlar[1].color), satirlar[1].lightness)

        val yeni = ui.setBeadColorFromSliders(store, BeadType.SOROBAN, true, 2, 240, 50)
        assertEquals(AbacusBeadRenderer.makeColor(240, 50), yeni)
        assertEquals(yeni, ui.colorFromSliders(240, 50))
        assertEquals(yeni, AbacusPreferences.getBeadColors(store, BeadType.SOROBAN, true)[2])
        assertEquals(240, ui.beadColorRows(store, BeadType.SOROBAN, true)[2].hue)

        // Gökkuşağı: kırmızı, sarı, yeşil, camgöbeği, mavi, eflatun, kırmızı.
        assertEquals(
            listOf(0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000).map { it.toInt() },
            ui.rainbowGradientColors(),
        )
        val acik = ui.lightnessGradientColors(0)
        assertEquals(0xFF000000.toInt(), acik[0])
        assertEquals(AbacusBeadRenderer.makeColor(0, 50), acik[1])
        assertEquals(0xFFFFFFFF.toInt(), acik[2])

        // Kilit: SOROBAN için de renk özelliği gerekir.
        assertTrue(ui.isBeadColorLocked(emptyMap(), BeadType.SOROBAN))
        assertFalse(ui.isBeadColorLocked(AbacusCustomizationRules.withBeadColorFeature(emptyMap(), BeadType.SOROBAN), BeadType.SOROBAN))
    }

    @Test
    fun cerceveKartlari() {
        val store = BellekAbacusPrefsStore()
        var owned: Map<String, FrameData> = emptyMap()

        assertEquals(listOf(0, 2000, 2000, 5000), ui.FRAME_CARDS.map { it.price })
        assertEquals(listOf("Ahşap", "Kalpli", "Yıldızlı", "Gradyanlı"), ui.FRAME_CARDS.map { it.label })
        assertNull(ui.FRAME_CARDS[0].purchaseImageResName)

        var states = ui.frameCardStates(store, owned)
        assertEquals(listOf(true, false, false, false), states.map { it.selected })
        assertEquals(listOf(false, true, true, true), states.map { it.locked })

        // Sahip olunmayan çerçeve: tip değişmez, satın alma paneli açılır.
        assertFalse(ui.onFrameCardClicked(store, owned, FrameType.FRAME_BG4))
        assertEquals(FrameType.FRAME_BG, AbacusPreferences.getFrameType(store))

        owned = ui.onFramePurchased(store, owned, FrameType.FRAME_BG4)
        assertEquals(FrameType.FRAME_BG4, AbacusPreferences.getFrameType(store))
        assertTrue(owned.getValue("FRAME_BG4").owned)

        assertTrue(ui.onFrameCardClicked(store, owned, FrameType.FRAME_BG))
        assertTrue(ui.onFrameCardClicked(store, owned, FrameType.FRAME_BG4))
        states = ui.frameCardStates(store, owned)
        assertTrue(states[3].selected)
        assertFalse(states[3].locked)

        // Renk kilidi: ahşap hep açık; diğerlerinde renk özelliği gerekir.
        assertTrue(ui.isFrameColorLocked(store, owned))
        assertFalse(ui.isFrameColorLocked(store, AbacusCustomizationRules.withFrameColorFeature(owned, FrameType.FRAME_BG4)))
        AbacusPreferences.setFrameType(store, FrameType.FRAME_BG)
        assertFalse(ui.isFrameColorLocked(store, owned))

        assertNull(ui.frameColorFeatureImageResName(FrameType.FRAME_BG))
        assertEquals("abacus_frame_bg3", ui.frameColorFeatureImageResName(FrameType.FRAME_BG3))
    }

    @Test
    fun cerceveRenkleriVeBoyasi() {
        val store = BellekAbacusPrefsStore()

        var satirlar = ui.frameColorRows(store)
        assertEquals(listOf("Ahşap rengi"), satirlar.map { it.label })
        assertEquals(AbacusPreferences.FRAME_DEFAULT_WOOD, satirlar[0].color)

        var boya = ui.framePaint(store, null)
        assertEquals("INSET_LAYERS", boya.kind)
        assertTrue(boya.isDefaultColors)
        assertNull(boya.resName)
        assertEquals(listOf(0f, 4f, 20f, 24f), boya.insetLayers.map { it.left })
        assertEquals(listOf(24f, 20f, 8f, 4f), boya.insetLayers.map { it.radius })
        assertEquals(AbacusPreferences.FRAME_DEFAULT_WOOD, boya.insetLayers[1].color)

        val yeniAhsap = ui.setFrameColorFromSliders(store, 0, 200, 40)
        boya = ui.framePaint(store, null)
        assertFalse(boya.isDefaultColors)
        assertEquals(yeniAhsap, boya.insetLayers[1].color)

        AbacusPreferences.setFrameType(store, FrameType.FRAME_BG2)
        satirlar = ui.frameColorRows(store)
        assertEquals(listOf("Ahşap rengi", "Kalp rengi"), satirlar.map { it.label })
        val yeniKalp = ui.setFrameColorFromSliders(store, 1, 120, 50)
        assertEquals(yeniKalp, AbacusPreferences.getFrameBg2HeartColor(store))
        // BG2'nin ahşabı BG'ninkinden ayrı saklanır.
        assertEquals(AbacusPreferences.FRAME_DEFAULT_WOOD, AbacusPreferences.getFrameBg2WoodColor(store))
        boya = ui.framePaint(store, null)
        assertEquals("REPLACED_BITMAP", boya.kind)
        assertEquals("abacus_frame_bg2", boya.resName)
        assertEquals(400, boya.canvasSize)

        // Piksel: özgün kalp rengi → yeni kalp rengi; ahşap değişmedi.
        val p = intArrayOf(AbacusFrameRenderer.ORIG_HEART, AbacusFrameRenderer.ORIG_WOOD, 0)
        ui.recolorFramePixels(store, FrameType.FRAME_BG2, p)
        assertEquals(yeniKalp, p[0])
        assertEquals(AbacusFrameRenderer.ORIG_WOOD, p[1])
        assertEquals(0, p[2])
        // BG4'te piksel değiştirme yok.
        val p4 = intArrayOf(AbacusFrameRenderer.ORIG_HEART)
        ui.recolorFramePixels(store, FrameType.FRAME_BG4, p4)
        assertEquals(AbacusFrameRenderer.ORIG_HEART, p4[0])

        AbacusPreferences.setFrameType(store, FrameType.FRAME_BG3)
        assertEquals(listOf("Ahşap rengi", "Yıldız rengi"), ui.frameColorRows(store).map { it.label })
        ui.setFrameColorFromSliders(store, 1, 10, 50)
        assertEquals(AbacusBeadRenderer.makeColor(10, 50), AbacusPreferences.getFrameBg3StarColor(store))

        AbacusPreferences.setFrameType(store, FrameType.FRAME_BG4)
        assertEquals(5, ui.frameColorRows(store).size)
        ui.setFrameColorFromSliders(store, 4, 300, 50)
        assertEquals(AbacusBeadRenderer.makeColor(300, 50), AbacusPreferences.getFrameBg4Colors(store)[4])
        boya = ui.framePaint(store, null)
        assertEquals("CANVAS_LAYERS", boya.kind)
        assertEquals(4, boya.canvasLayers.size)
        assertEquals(listOf(false, true, false, false), boya.canvasLayers.map { it.isGradient })
        assertEquals(5, boya.gradientStops.size)
        assertEquals(5, boya.colors.size)
        // Tip dışarıdan verilirse kayıtlı tip yerine o kullanılır.
        assertEquals(FrameType.FRAME_BG, ui.framePaint(store, FrameType.FRAME_BG).frameType)

        assertEquals(4, ui.TAB_ICON_RES_NAMES.size)
    }
}

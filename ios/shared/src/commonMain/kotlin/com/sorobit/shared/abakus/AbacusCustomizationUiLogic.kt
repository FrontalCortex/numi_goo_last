// Kaynak (Android):
//  - app/src/main/java/com/example/app/AbacusCustomizationFragment.kt
//      (sekme seçimi, inflateTab1..4, showBeadPurchasePanel, showColorFeaturePurchasePanel,
//       showBeadSlotSelectPanel, showFramePurchasePanel, showFrameColorFeaturePurchasePanel,
//       rainbowDrawable, lightnessGradientDrawable — görünümden bağımsız kararlar)
//  - app/src/main/res/layout/fragment_abacus_customization.xml (sekme simgeleri)
//  - app/src/main/res/layout/layout_tab1_bead_options.xml (kart sırası, adlar, görsel ölçüleri)
//  - app/src/main/res/layout/layout_tab3_frame_options.xml (çerçeve kartları)
//  - app/src/main/res/layout/layout_customization_preview.xml, panel_bead_skin_select.xml ve
//    res/values/dimens.xml (tutorial_abacus_*) — önizleme abaküsünün ölçüleri
//  - app/src/main/res/drawable/abacus_frame_bg.xml, abacus_horizontal_bar.xml (katman ölçüleri)
//
// Bu dosya var olan kuralların ([AbacusCustomizationRules], [AbacusPreferences],
// [AbacusBeadRenderer], [AbacusFrameRenderer]) üstünde, ekranın gösterdiği durumu ve dokunuş
// kararlarını Swift'in doğrudan okuyabileceği yalın türlerle (Int, String, Boolean, List) verir.
// Yeni kural içermez; Fragment'ın içinde dağınık duran tablo ve kararların kopyasıdır.
//
// Android'den farklar:
//  - Görünüm/Dialog/Toast çağrıları yok; karar ve gösterilecek metin döndürülür.
//  - `R.drawable.x` → "x".
//  - Renkler ARGB Int; kaydırıcı değerleri (ton 0..359, açıklık 0..100) Int.
//
// Android'deki hâliyle korunan davranışlar:
//  - Karta dokununca genel boncuk tipi, satın alınmamış olsa bile HEMEN yazılır.
//  - İlk satın alma panelinde 25 üst sınırı denetlenmez; yalnızca yuva panelindeki düğme denetler.
//  - Boncuk renk özelliği: güncel Android sahiplik kaydı başarısız olursa harcananı İADE EDER
//    (eski sürümde etmiyordu); fiyat boncuğun para birimindedir (ANIMAL3/ANIMAL8: 40 anahtar,
//    diğerleri 2000 altın; bkz. [checkBeadColorFeaturePurchase]). Beş akışın hepsinde iade vardır.
//  - 1. sekmedeki kart sırası PAID_BEAD_INFO sırasından farklıdır (ANIMAL9 üçüncü hayvan,
//    ANIMAL3 sonuncu); kart adlarında yinelenenler var ("Futbol", "Bowling", "Tenis", "Basketbol").
package com.sorobit.shared.abakus

import com.sorobit.shared.abakus.AbacusPreferences.BeadType
import com.sorobit.shared.abakus.AbacusPreferences.FrameType

/**
 * Önizleme abaküsünün ölçüleri (dp). layout_customization_preview.xml "gerçek abaküsle birebir
 * aynı" olacak biçimde yazılmıştır (çubuk 45dp, iç boşluk 20/24dp).
 */
object AbacusPreviewMetrics {
    const val ROD_COUNT = 5
    const val BOTTOM_BEADS_PER_ROD = 4

    /** `android:layout_width="45dp"` (çubuk kabı). */
    const val ROD_WIDTH_DP = 45

    /** `tutorial_abacus_rod_height`. */
    const val ROD_HEIGHT_DP = 250

    /** `tutorial_abacus_line_width`; rengi [ROD_LINE_COLOR]. */
    const val ROD_LINE_WIDTH_DP = 5
    const val ROD_LINE_COLOR = 0xFF1A1A1A.toInt()

    /** `tutorial_abacus_bead_size` (kare kap; görsel içine fitCenter ile sığar). */
    const val BEAD_SIZE_DP = 40

    /** `tutorial_abacus_bottom_bead_margin_1..4`: alt boncukların ALT kenar boşluğu (1 = en üstteki). */
    val BOTTOM_BEAD_MARGIN_BOTTOM_DP: List<Int> = listOf(82, 58, 34, 10)

    /** Önizlemedeki bariyer yüksekliği (`tutorial_abacus_barrier_height`). */
    const val BARRIER_HEIGHT_DP = 6

    /** Yuva seçme panelindeki bariyer yüksekliği (panel_bead_skin_select.xml: 16dp). */
    const val SLOT_PANEL_BARRIER_HEIGHT_DP = 16

    /** `tutorial_abacus_barrier_margin_bottom` (dikey ortalanmış bariyeri yukarı iter). */
    const val BARRIER_MARGIN_BOTTOM_DP = 50

    /** abacus_horizontal_bar.xml: siyah zemin, üstten/alttan 4dp içeride ahşap şerit. */
    const val BARRIER_OUTER_COLOR = 0xFF000000.toInt()
    const val BARRIER_INNER_COLOR = 0xFFDDA379.toInt()
    const val BARRIER_INNER_INSET_DP = 4

    /** abacusContainer iç boşlukları. */
    const val FRAME_PADDING_VERTICAL_DP = 20
    const val FRAME_PADDING_HORIZONTAL_DP = 24

    /** Çubuğun üstünden üst boncuğun üst kenarına (gravity top). */
    fun topBeadTopDp(): Int = 0

    /** Çubuğun üstünden [index]. (0 = en üstteki) alt boncuğun üst kenarına. */
    fun bottomBeadTopDp(index: Int): Int =
        ROD_HEIGHT_DP - BOTTOM_BEAD_MARGIN_BOTTOM_DP[index] - BEAD_SIZE_DP

    /**
     * Çubuğun üstünden bariyerin üst kenarına. FrameLayout'ta dikey ortalanmış çocuk alt boşluk
     * kadar yukarı kayar: (çubuk − bariyer) / 2 − altBoşluk.
     */
    fun barrierTopDp(barrierHeightDp: Int): Int =
        (ROD_HEIGHT_DP - barrierHeightDp) / 2 - BARRIER_MARGIN_BOTTOM_DP
}

object AbacusCustomizationUiLogic {

    // ── Sekmeler ──────────────────────────────────────────────────────────────

    const val TAB_BEAD_TYPE = 0
    const val TAB_BEAD_COLOR = 1
    const val TAB_FRAME_TYPE = 2
    const val TAB_FRAME_COLOR = 3

    /** fragment_abacus_customization.xml: tab1..tab4 görselleri. */
    val TAB_ICON_RES_NAMES: List<String> =
        listOf("bead", "abacus_color_tab_ic", "abacus_frame_tab_ic", "abacus_frame_color_tab_ic")

    /** selectTab: seçili sekme 1.0, diğerleri 0.4 opaklık. */
    const val TAB_SELECTED_ALPHA = 1.0f
    const val TAB_UNSELECTED_ALPHA = 0.4f

    /** scheduleFirestoreSync gecikmesi. */
    const val FIRESTORE_SYNC_DEBOUNCE_MS = 1000L

    /** scheduleBeadRefresh(debounce = true) gecikmesi (kaydırıcı sürüklenirken). */
    const val BEAD_REFRESH_DEBOUNCE_MS = 300L

    /** Satın alma panelleri ekran genişliğinin 0.88'i, yuva paneli 0.92'si kadardır. */
    const val PURCHASE_PANEL_WIDTH_RATIO = 0.88f
    const val SLOT_PANEL_WIDTH_RATIO = 0.92f

    /** Kilit örtüsü (#BB000000) ve kart zemini (#1A262B). */
    const val LOCK_OVERLAY_COLOR = 0xBB000000.toInt()
    const val CARD_BACKGROUND_COLOR = 0xFF1A262B.toInt()

    /** Yuva panelinde stok dolunca satın al düğmesinin rengi (#BDBDBD). */
    const val BUY_BUTTON_MAXED_COLOR = 0xFFBDBDBD.toInt()

    const val TITLE_BEAD_TYPE = "Boncuk Tipi Seç"
    const val TITLE_FRAME_TYPE = "Çerçeve Tipi Seç"
    const val TITLE_SLOT_PANEL = "Hangi boncuğun yerine kullanmak istiyorsun?"
    const val LABEL_NORMAL = "Normal"
    const val LABEL_SELECTED = "Seçili"
    const val LABEL_HUE = "Renk"
    const val LABEL_LIGHTNESS = "Parlaklık"

    // ── 1. sekme: boncuk tipi ─────────────────────────────────────────────────

    /** layout_tab1_bead_options.xml: her satırda 4 kart (weightSum = 4). */
    const val BEAD_CARDS_PER_ROW = 4

    data class BeadCard(
        val beadType: BeadType,
        val imageResName: String,
        val label: String,
        val imageWidthDp: Int,
        val imageHeightDp: Int,
        val labelMarginTopDp: Int,
    )

    private fun sorobanCard(type: BeadType, res: String, label: String) = BeadCard(type, res, label, 60, 46, 6)
    private fun roundCard(type: BeadType, res: String, label: String) = BeadCard(type, res, label, 60, 60, 4)

    /** Kartlar, ekrandaki sırayla (layout_tab1_bead_options.xml). */
    val BEAD_CARDS: List<BeadCard> = listOf(
        sorobanCard(BeadType.SOROBAN, "soroban_bead", "Soroban"),
        sorobanCard(BeadType.SOROBAN2, "soroban_bead2", "Gökkuşağı"),
        sorobanCard(BeadType.SOROBAN6, "soroban_bead6", "Bulut"),
        roundCard(BeadType.BOWLING, "bowling_ball_ic", "Bowling"),
        roundCard(BeadType.BALL1, "abacus_bead_ball1", "Tenis"),
        roundCard(BeadType.BALL3, "abacus_bead_ball3", "Bilardo"),
        roundCard(BeadType.BALL4, "abacus_bead_ball4", "Basketbol"),
        roundCard(BeadType.BALL5, "abacus_bead_ball5", "Futbol"),
        roundCard(BeadType.BALL6, "abacus_bead_ball6", "Birto"),
        roundCard(BeadType.BALL7, "abacus_bead_ball7", "Golf"),
        roundCard(BeadType.BALL8, "abacus_bead_ball8", "Futbol"),
        roundCard(BeadType.BALL9, "abacus_bead_ball9", "Plaj Topu"),
        roundCard(BeadType.BALL10, "abacus_bead_ball10", "Bowling"),
        roundCard(BeadType.BALL11, "abacus_bead_ball11", "Basketbol"),
        roundCard(BeadType.BALL12, "abacus_bead_ball12", "Tenis"),
        roundCard(BeadType.BALL13, "abacus_bead_ball13", "Beyzbol"),
        roundCard(BeadType.BALL14, "abacus_bead_ball14", "Buz Hokeyi"),
        roundCard(BeadType.ANIMAL, "animal_head_ic1", "Köpek"),
        roundCard(BeadType.ANIMAL2, "animal_head_ic2", "Maymun"),
        roundCard(BeadType.ANIMAL9, "animal_head_ic9", "Ördek"),
        roundCard(BeadType.ANIMAL4, "animal_head_ic4", "Tilki"),
        roundCard(BeadType.ANIMAL5, "animal_head_ic5", "Fil"),
        roundCard(BeadType.ANIMAL6, "animal_head_ic6", "Panda"),
        roundCard(BeadType.ANIMAL7, "animal_head_ic7", "Kedi"),
        roundCard(BeadType.ANIMAL8, "animal_head_ic8", "Mors"),
        roundCard(BeadType.ANIMAL3, "animal_head_ic3", "Kaplan"),
    )

    data class BeadCardState(
        val card: BeadCard,
        /** Genel boncuk tipi bu kart mı (sarı 6px çerçeve). */
        val selected: Boolean,
        /** Kilit örtüsü görünür mü (SOROBAN'da hiç yoktur). */
        val locked: Boolean,
        /** Örtüdeki fiyat (5'li paket). */
        val price: Int,
        /** Örtüdeki para birimi görseli: "key" ya da "gold_ic". */
        val currencyIconResName: String,
    )

    /** inflateTab1: kartların seçim ve kilit durumu. */
    fun beadCardStates(store: AbacusPrefsStore, uid: String?, ownedBeads: Map<String, BeadData>): List<BeadCardState> {
        val current = AbacusPreferences.getBeadType(store, uid)
        return BEAD_CARDS.map { card ->
            BeadCardState(
                card = card,
                selected = card.beadType == current,
                locked = card.beadType != BeadType.SOROBAN &&
                    !AbacusCustomizationRules.isBeadOwned(ownedBeads, card.beadType),
                price = AbacusCustomizationRules.getBeadPrice(card.beadType),
                currencyIconResName = AbacusCustomizationRules.currencyIconResName(card.beadType),
            )
        }
    }

    /**
     * Karta dokunuldu: genel tip HEMEN yazılır (satın alınmamış olsa da), sonra
     * @return true → yuva seçme paneli açılır; false → satın alma paneli açılır
     *   (satın alma başarılı olunca yuva paneli ardından açılır).
     */
    fun onBeadCardClicked(
        store: AbacusPrefsStore,
        uid: String?,
        ownedBeads: Map<String, BeadData>,
        beadType: BeadType,
    ): Boolean {
        AbacusPreferences.setBeadType(store, uid, beadType)
        return beadType == BeadType.SOROBAN || AbacusCustomizationRules.isBeadOwned(ownedBeads, beadType)
    }

    // ── Satın alma panelleri ──────────────────────────────────────────────────

    /** Bir satın alma akışının hata metinleri ve iade kuralı. */
    data class PurchasePanelRules(
        /** Cüzdan düşümü başarısız olunca gösterilen metin. */
        val walletFailureMessage: String,
        /** Sahiplik kaydı başarısız olunca gösterilen metin. */
        val saveFailureMessage: String,
        /** Sahiplik kaydı başarısız olunca harcanan tutar iade edilir mi. */
        val rollbackOnSaveFailure: Boolean,
    )

    /** showBeadPurchasePanel. */
    val BEAD_PURCHASE_PANEL = PurchasePanelRules(
        AbacusCustomizationRules.MESSAGE_OPERATION_FAILED,
        AbacusCustomizationRules.MESSAGE_SAVE_ERROR_RETRY,
        rollbackOnSaveFailure = true,
    )

    /** showBeadSlotSelectPanel içindeki satın al düğmesi. */
    val SLOT_PANEL_PURCHASE = PurchasePanelRules(
        "İşlem başarısız",
        AbacusCustomizationRules.MESSAGE_SAVE_ERROR,
        rollbackOnSaveFailure = true,
    )

    /** showColorFeaturePurchasePanel — güncel Android sahiplik kaydı başarısız olunca iade eder. */
    val BEAD_COLOR_FEATURE_PANEL = PurchasePanelRules(
        AbacusCustomizationRules.MESSAGE_ERROR,
        AbacusCustomizationRules.MESSAGE_ERROR,
        rollbackOnSaveFailure = true,
    )

    /** showFramePurchasePanel ve showFrameColorFeaturePurchasePanel. */
    val FRAME_PURCHASE_PANEL = PurchasePanelRules(
        "Ödeme hatası",
        AbacusCustomizationRules.MESSAGE_SAVE_ERROR,
        rollbackOnSaveFailure = true,
    )

    /** Satın al düğmesine basıldığında yapılan ön denetimin sonucu. */
    data class PurchaseCheck(
        val allowed: Boolean,
        /** İzin yoksa gösterilecek metin. */
        val message: String?,
        val price: Int,
        /** true → anahtarla, false → altınla ödenir. */
        val isKeyCurrency: Boolean,
    )

    /** showBeadPurchasePanel: yalnızca bakiye denetlenir (stok sınırı denetlenmez). */
    fun checkBeadPurchase(beadType: BeadType, cachedKeys: Int, cachedCurrency: Int): PurchaseCheck {
        val price = AbacusCustomizationRules.getBeadPrice(beadType)
        val isKey = AbacusCustomizationRules.isKeyCurrency(beadType)
        val enough = AbacusCustomizationRules.hasEnough(beadType, cachedKeys, cachedCurrency)
        return PurchaseCheck(
            allowed = enough,
            message = if (enough) null else AbacusCustomizationRules.notEnoughMessage(beadType),
            price = price,
            isKeyCurrency = isKey,
        )
    }

    /** Yuva panelindeki düğme: önce stok ("Stokta kalmadı"), sonra bakiye. */
    fun checkSlotPanelPurchase(
        ownedBeads: Map<String, BeadData>,
        beadType: BeadType,
        cachedKeys: Int,
        cachedCurrency: Int,
    ): PurchaseCheck {
        if (AbacusCustomizationRules.isStockMaxed(ownedBeads, beadType)) {
            return PurchaseCheck(
                allowed = false,
                message = AbacusCustomizationRules.MESSAGE_OUT_OF_STOCK,
                price = AbacusCustomizationRules.getBeadPrice(beadType),
                isKeyCurrency = AbacusCustomizationRules.isKeyCurrency(beadType),
            )
        }
        return checkBeadPurchase(beadType, cachedKeys, cachedCurrency)
    }

    /**
     * showColorFeaturePurchasePanel ön denetimi: fiyat ve para birimi boncuğa bağlıdır
     * (ANIMAL3 / ANIMAL8 → 40 anahtar, diğerleri → 2000 altın).
     */
    fun checkBeadColorFeaturePurchase(beadType: BeadType, cachedKeys: Int, cachedCurrency: Int): PurchaseCheck {
        val price = AbacusCustomizationRules.getBeadColorFeaturePrice(beadType)
        val isKey = AbacusCustomizationRules.isKeyCurrency(beadType)
        val enough = if (isKey) cachedKeys >= price else cachedCurrency >= price
        return PurchaseCheck(
            allowed = enough,
            message = if (enough) null else AbacusCustomizationRules.notEnoughMessage(beadType),
            price = price,
            isKeyCurrency = isKey,
        )
    }

    /** Altınla ödenen sabit fiyatlı akışlar (renk özelliği, çerçeve). */
    fun checkGoldPurchase(price: Int, cachedCurrency: Int): PurchaseCheck {
        val enough = cachedCurrency >= price
        return PurchaseCheck(
            allowed = enough,
            message = if (enough) null else AbacusCustomizationRules.MESSAGE_NOT_ENOUGH_GOLD,
            price = price,
            isKeyCurrency = false,
        )
    }

    /** Boncuk satın alındı: yeni adet (mevcut + 5). Yuva panelinde SOROBAN 25 sayılır. */
    fun beadCountAfterPurchase(ownedBeads: Map<String, BeadData>, beadType: BeadType, fromSlotPanel: Boolean): Int {
        val current = if (fromSlotPanel) {
            AbacusCustomizationRules.ownedCount(ownedBeads, beadType)
        } else {
            ownedBeads[beadType.name]?.count ?: 0
        }
        return AbacusCustomizationRules.countAfterPurchase(current)
    }

    // ── Yuva seçme paneli ─────────────────────────────────────────────────────

    data class SlotPanelState(
        /** Envanterde kalan (negatif olabilir). */
        val availableCount: Int,
        /** "x5" gibi. */
        val inventoryText: String,
        val price: Int,
        val currencyIconResName: String,
        /** Toplam 25'e ulaştıysa düğme gri çizilir. */
        val buyButtonMaxed: Boolean,
    )

    /** showBeadSlotSelectPanel.refreshUI. */
    fun slotPanelState(
        ownedBeads: Map<String, BeadData>,
        store: AbacusPrefsStore,
        uid: String?,
        beadType: BeadType,
    ): SlotPanelState {
        val available = AbacusCustomizationRules.availableCount(ownedBeads, store, uid, beadType)
        return SlotPanelState(
            availableCount = available,
            inventoryText = "x$available",
            price = AbacusCustomizationRules.getBeadPrice(beadType),
            currencyIconResName = AbacusCustomizationRules.currencyIconResName(beadType),
            buyButtonMaxed = AbacusCustomizationRules.isStockMaxed(ownedBeads, beadType),
        )
    }

    data class PlaceOutcome(
        /** Yuva değişti mi (önizleme yenilenir, eşitleme zamanlanır). */
        val placed: Boolean,
        /** Gösterilecek metin (yalnızca envanter bittiyse). */
        val message: String?,
    )

    /** showBeadSlotSelectPanel.placeBead. */
    fun placeBeadInSlot(
        ownedBeads: Map<String, BeadData>,
        store: AbacusPrefsStore,
        uid: String?,
        beadType: BeadType,
        rod: Int,
        isTop: Boolean,
        beadIndex: Int,
    ): PlaceOutcome =
        when (AbacusCustomizationRules.placeBead(ownedBeads, store, uid, beadType, rod, isTop, beadIndex)) {
            AbacusCustomizationRules.PlaceResult.SAME_TYPE_IGNORED -> PlaceOutcome(false, null)
            AbacusCustomizationRules.PlaceResult.NO_BEADS_LEFT ->
                PlaceOutcome(false, AbacusCustomizationRules.MESSAGE_NO_BEADS_LEFT)
            AbacusCustomizationRules.PlaceResult.PLACED -> PlaceOutcome(true, null)
        }

    // ── Önizleme: yuvalar ve boncuk boyası ────────────────────────────────────

    data class SlotBead(
        val rod: Int,
        val isTop: Boolean,
        /** Alt boncuklarda 0..3 (0 = bariyere en yakın); üst boncukta 0. */
        val beadIndex: Int,
        val beadType: BeadType,
    )

    /** 25 yuvanın tipleri; sıra: çubuk 0..4, her çubukta önce üst, sonra alt 0..3. */
    fun slotBeads(store: AbacusPrefsStore, uid: String?): List<SlotBead> {
        val list = mutableListOf<SlotBead>()
        for (rod in 0 until AbacusPreviewMetrics.ROD_COUNT) {
            list += SlotBead(rod, true, 0, AbacusPreferences.getBeadTypeForSlot(store, uid, rod, isTop = true))
            for (i in 0 until AbacusPreviewMetrics.BOTTOM_BEADS_PER_ROD) {
                list += SlotBead(rod, false, i, AbacusPreferences.getBeadTypeForSlot(store, uid, rod, isTop = false, beadIndex = i))
            }
        }
        return list
    }

    /** Bir boncuğun nasıl çizileceği (AbacusBeadRenderer.buildBeadForType'ın girdileri). */
    data class BeadPaint(
        val beadType: BeadType,
        val selected: Boolean,
        /** "TINTED_LAYERS", "REPLACED_BITMAP" ya da "GRADIENT_PILL" ([AbacusBeadRenderer.BeadRenderKind] adı). */
        val kind: String,
        /** TINTED_LAYERS → 3 katman; REPLACED_BITMAP → tek kaynak; GRADIENT_PILL → boş. */
        val resNames: List<String>,
        /** Kullanıcının seçtiği renkler (ARGB), tipin renk sırasıyla. */
        val colors: List<Int>,
        /** Renkler varsayılanla aynı mı (REPLACED_BITMAP'te kaynak görsel olduğu gibi çizilebilir). */
        val isDefaultColors: Boolean,
        /** Önbellek anahtarı: tip, seçililik ve renkler. */
        val cacheKey: String,
    )

    fun beadPaint(store: AbacusPrefsStore, beadType: BeadType, selected: Boolean): BeadPaint {
        val colors = AbacusPreferences.getBeadColors(store, beadType, selected)
        val defaults = AbacusPreferences.beadColorSpec(beadType).defaultsFor(selected)
        val kind = AbacusBeadRenderer.renderKind(beadType)
        val resNames = when (kind) {
            AbacusBeadRenderer.BeadRenderKind.TINTED_LAYERS -> AbacusBeadRenderer.sorobanLayerResNames(selected).toList()
            AbacusBeadRenderer.BeadRenderKind.REPLACED_BITMAP -> listOfNotNull(AbacusBeadRenderer.beadResName(beadType, selected))
            AbacusBeadRenderer.BeadRenderKind.GRADIENT_PILL -> emptyList()
        }
        return BeadPaint(
            beadType = beadType,
            selected = selected,
            kind = kind.name,
            resNames = resNames,
            colors = colors.toList(),
            isDefaultColors = colors.contentEquals(defaults),
            cacheKey = beadType.name + (if (selected) "_sel" else "") + colors.joinToString(separator = "") { "_$it" },
        )
    }

    /**
     * REPLACED_BITMAP tipinde rasterleştirilmiş kaynağın piksellerini (ARGB, önceden çarpılmamış)
     * kullanıcının renkleriyle YERİNDE değiştirir. Diğer tiplerde hiçbir şey yapmaz.
     */
    fun recolorBeadPixels(
        store: AbacusPrefsStore,
        beadType: BeadType,
        selected: Boolean,
        pixels: IntArray,
        width: Int,
        height: Int,
    ) {
        val map = AbacusBeadRenderer.originalColorMap(beadType, selected) ?: return
        if (width <= 0 || height <= 0 || pixels.size != width * height) return
        val colors = AbacusPreferences.getBeadColors(store, beadType, selected)
        AbacusBeadRenderer.replaceBeadPixels(pixels, width, height, map, colors)
    }

    /** SOROBAN2 hap çiziminin 120 × 92 birimlik tuvaldeki ölçüleri (yoğunluk 1). */
    data class PillGeometry(
        val width: Float,
        val height: Float,
        val outerRadiusX: Float,
        val outerRadiusY: Float,
        val padX: Float,
        val padY: Float,
        val innerRadiusX: Float,
        val innerRadiusY: Float,
        /** Gradyan durakları; doğrultu (0, yükseklik) → (genişlik, 0). */
        val stops: List<Float>,
    )

    fun pillGeometry(): PillGeometry {
        val g = AbacusBeadRenderer.soroban2Geometry(1f)
        return PillGeometry(
            width = g.targetW.toFloat(),
            height = g.targetH.toFloat(),
            outerRadiusX = g.outerRx,
            outerRadiusY = g.outerRy,
            padX = g.padX,
            padY = g.padY,
            innerRadiusX = g.innerRx,
            innerRadiusY = g.innerRy,
            stops = AbacusBeadRenderer.SOROBAN2_GRADIENT_STOPS.toList(),
        )
    }

    // ── Önizleme: çerçeve ─────────────────────────────────────────────────────

    /** Yuvarlatılmış dikdörtgen katmanı; ölçüler kendi tuvalinin biriminde. */
    data class FrameLayer(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val radius: Float,
        /** Düz renk (ARGB); gradyan katmanında kullanılmaz. */
        val color: Int,
        /** true → [FramePaint.colors] ile (0, alt) → (sağ, 0) doğrultusunda gradyan. */
        val isGradient: Boolean,
    )

    data class FramePaint(
        val frameType: FrameType,
        /**
         * "INSET_LAYERS": FRAME_BG — [insetLayers] dp cinsinden kenar boşluğu (left = top = right =
         *   bottom = boşluk) ve yarıçap; kabın gerçek boyutuna göre çizilir.
         * "REPLACED_BITMAP": FRAME_BG2 / FRAME_BG3 — [resName] 400×400 rasterleştirilir,
         *   [recolorFramePixels] uygulanır, kaba gerilerek çizilir.
         * "CANVAS_LAYERS": FRAME_BG4 — [canvasLayers] 400×400 tuvalde çizilir, kaba gerilir.
         */
        val kind: String,
        val resName: String?,
        /** Kullanıcı renkleri: BG → [ahşap]; BG2 → [ahşap, kalp]; BG3 → [ahşap, yıldız]; BG4 → 5 renk. */
        val colors: List<Int>,
        val isDefaultColors: Boolean,
        val insetLayers: List<FrameLayer>,
        val canvasLayers: List<FrameLayer>,
        val gradientStops: List<Float>,
        /** Raster tuval kenarı (px). */
        val canvasSize: Int,
        val cacheKey: String,
    )

    const val FRAME_KIND_INSET_LAYERS = "INSET_LAYERS"
    const val FRAME_KIND_REPLACED_BITMAP = "REPLACED_BITMAP"
    const val FRAME_KIND_CANVAS_LAYERS = "CANVAS_LAYERS"

    private fun inset(insetDp: Float, radiusDp: Float, color: Int) =
        FrameLayer(insetDp, insetDp, insetDp, insetDp, radiusDp, color, false)

    /** abacus_frame_bg.xml katmanları; 2. katman (ahşap) kullanıcı rengiyle boyanır. */
    fun frameBgInsetLayers(woodColor: Int): List<FrameLayer> = listOf(
        inset(0f, 24f, AbacusFrameRenderer.ORIG_BLACK),
        inset(4f, 20f, woodColor),
        inset(20f, 8f, AbacusFrameRenderer.ORIG_BLACK),
        inset(24f, 4f, AbacusFrameRenderer.ORIG_DARK),
    )

    private fun defaultFrameColors(type: FrameType): IntArray = when (type) {
        FrameType.FRAME_BG -> intArrayOf(AbacusPreferences.FRAME_DEFAULT_WOOD)
        FrameType.FRAME_BG2 -> intArrayOf(AbacusPreferences.FRAME_DEFAULT_WOOD, AbacusPreferences.FRAME_DEFAULT_HEART)
        FrameType.FRAME_BG3 -> intArrayOf(AbacusPreferences.FRAME_DEFAULT_WOOD, AbacusPreferences.FRAME_DEFAULT_STAR)
        FrameType.FRAME_BG4 -> AbacusPreferences.FRAME_BG4_DEFAULT_COLORS
    }

    /** AbacusFrameRenderer.buildFrameDrawable'ın girdileri; [frameType] null ise kayıtlı tip. */
    fun framePaint(store: AbacusPrefsStore, frameType: FrameType?): FramePaint {
        val type = AbacusFrameRenderer.resolveFrameType(store, frameType)
        val colors = AbacusFrameRenderer.resolveFrameColors(store, type, null)
        val kind = when (type) {
            FrameType.FRAME_BG -> FRAME_KIND_INSET_LAYERS
            FrameType.FRAME_BG2, FrameType.FRAME_BG3 -> FRAME_KIND_REPLACED_BITMAP
            FrameType.FRAME_BG4 -> FRAME_KIND_CANVAS_LAYERS
        }
        return FramePaint(
            frameType = type,
            kind = kind,
            resName = if (type == FrameType.FRAME_BG) null else AbacusFrameRenderer.frameResName(type),
            colors = colors.toList(),
            isDefaultColors = colors.contentEquals(defaultFrameColors(type)),
            insetLayers = if (type == FrameType.FRAME_BG) frameBgInsetLayers(colors[0]) else emptyList(),
            canvasLayers = if (type == FrameType.FRAME_BG4) {
                AbacusFrameRenderer.BG4_LAYERS.map {
                    FrameLayer(it.left, it.top, it.right, it.bottom, it.radius, it.color ?: 0, it.color == null)
                }
            } else {
                emptyList()
            },
            gradientStops = if (type == FrameType.FRAME_BG4) AbacusFrameRenderer.BG4_GRADIENT_STOPS.toList() else emptyList(),
            canvasSize = AbacusFrameRenderer.RENDER_SIZE,
            cacheKey = type.name + colors.joinToString(separator = "") { "_$it" },
        )
    }

    /**
     * FRAME_BG2 / FRAME_BG3 için 400×400 rasterin piksellerini (ARGB, önceden çarpılmamış)
     * kayıtlı renklerle YERİNDE değiştirir. Diğer tiplerde hiçbir şey yapmaz.
     */
    fun recolorFramePixels(store: AbacusPrefsStore, frameType: FrameType, pixels: IntArray) {
        val orig = when (frameType) {
            FrameType.FRAME_BG2 -> AbacusFrameRenderer.ALL_ORIG
            FrameType.FRAME_BG3 -> AbacusFrameRenderer.ALL_ORIG_BG3
            else -> return
        }
        val colors = AbacusFrameRenderer.resolveFrameColors(store, frameType, null)
        AbacusFrameRenderer.replaceFramePixels(pixels, orig, AbacusFrameRenderer.newColorsFor(colors[0], colors[1]))
    }

    // ── 2. sekme: boncuk rengi ────────────────────────────────────────────────

    const val HUE_MAX = 359
    const val LIGHTNESS_MAX = 100

    /** inflateTab2'deki `pathCount` tablosu (kaç renk satırı gösterilir). */
    fun tab2PathCount(beadType: BeadType): Int = when (beadType) {
        BeadType.SOROBAN -> 3
        BeadType.ANIMAL -> 4
        BeadType.ANIMAL2 -> 5
        BeadType.ANIMAL3 -> 5
        BeadType.ANIMAL4 -> 4
        BeadType.ANIMAL5 -> 6
        BeadType.ANIMAL6 -> 3
        BeadType.ANIMAL7 -> 5
        BeadType.ANIMAL8 -> 5
        BeadType.ANIMAL9 -> 4
        BeadType.BOWLING -> 5
        BeadType.BALL1 -> 3
        BeadType.BALL3 -> 4
        BeadType.BALL4 -> 4
        BeadType.BALL5 -> 4
        BeadType.BALL6 -> 4
        BeadType.BALL7 -> 3
        BeadType.BALL8 -> 4
        BeadType.BALL9 -> 5
        BeadType.BALL10 -> 5
        BeadType.BALL11 -> 4
        BeadType.BALL12 -> 5
        BeadType.BALL13 -> 4
        BeadType.BALL14 -> 3
        BeadType.SOROBAN2 -> 5
        BeadType.SOROBAN6 -> 3
    }

    /** Bir renk satırı: etiket, renk ve iki kaydırıcının konumu. */
    data class ColorRow(
        val index: Int,
        val label: String,
        val color: Int,
        /** Ton kaydırıcısı (0..359). */
        val hue: Int,
        /** Parlaklık kaydırıcısı (0..100). */
        val lightness: Int,
    )

    private fun colorRow(index: Int, label: String, color: Int) = ColorRow(
        index = index,
        label = label,
        color = color,
        hue = AbacusBeadRenderer.getHue(color),
        lightness = AbacusBeadRenderer.getLightness100(color),
    )

    /** inflateTab2.buildColorRows: genel boncuk tipinin [selected] durumundaki renk satırları. */
    fun beadColorRows(store: AbacusPrefsStore, beadType: BeadType, selected: Boolean): List<ColorRow> {
        val labels = AbacusPreferences.beadColorSpec(beadType).labels
        val colors = AbacusPreferences.getBeadColors(store, beadType, selected)
        return (0 until tab2PathCount(beadType)).map { i -> colorRow(i, labels[i], colors[i]) }
    }

    /** Kaydırıcı değişti (onAnyChange): yeni renk hesaplanır, kaydedilir ve döndürülür. */
    fun setBeadColorFromSliders(
        store: AbacusPrefsStore,
        beadType: BeadType,
        selected: Boolean,
        index: Int,
        hue: Int,
        lightness: Int,
    ): Int {
        val newColor = AbacusBeadRenderer.makeColor(hue, lightness)
        AbacusPreferences.setBeadColor(store, beadType, selected, index, newColor)
        return newColor
    }

    /** Kaydedilmeden yalnızca renk (kaydırıcı başı ve örnek daire için). */
    fun colorFromSliders(hue: Int, lightness: Int): Int = AbacusBeadRenderer.makeColor(hue, lightness)

    /** rainbowDrawable: 7 duraklı gökkuşağı (ton 0, 60, …, 360; doygunluk ve değer 1). */
    fun rainbowGradientColors(): List<Int> =
        listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { AbacusBeadRenderer.HSVToColor(floatArrayOf(it, 1f, 1f)) }

    /** lightnessGradientDrawable: siyah → tam renk → beyaz. */
    fun lightnessGradientColors(hue: Int): List<Int> = listOf(
        0xFF000000.toInt(),
        AbacusBeadRenderer.HSVToColor(floatArrayOf(hue.toFloat(), AbacusBeadRenderer.FIXED_SATURATION, 1f)),
        0xFFFFFFFF.toInt(),
    )

    /** 2. sekme kilidi: renk özelliği satın alınmadıysa örtü görünür (SOROBAN için de). */
    fun isBeadColorLocked(ownedBeads: Map<String, BeadData>, beadType: BeadType): Boolean =
        !AbacusCustomizationRules.isBeadColorFeatureActive(ownedBeads, beadType)

    // ── 3. sekme: çerçeve tipi ────────────────────────────────────────────────

    data class FrameCard(
        val frameType: FrameType,
        /** Karttaki küçük görsel. */
        val imageResName: String,
        val label: String,
        val price: Int,
        /** Satın alma panelindeki görsel; ücretsiz ahşap çerçevede null. */
        val purchaseImageResName: String?,
    )

    /** layout_tab3_frame_options.xml: 2 × 2 kart. */
    val FRAME_CARDS: List<FrameCard> = listOf(
        FrameCard(FrameType.FRAME_BG, "abacus_frame_tab_ic", "Ahşap", AbacusCustomizationRules.getFramePrice(FrameType.FRAME_BG), null),
        FrameCard(FrameType.FRAME_BG2, "abacus_frame_bg2", "Kalpli", AbacusCustomizationRules.getFramePrice(FrameType.FRAME_BG2), "abacus_frame_bg2"),
        FrameCard(FrameType.FRAME_BG3, "abacus_frame_bg3", "Yıldızlı", AbacusCustomizationRules.getFramePrice(FrameType.FRAME_BG3), "abacus_frame_bg3"),
        FrameCard(FrameType.FRAME_BG4, "abacus_frame_bg4", "Gradyanlı", AbacusCustomizationRules.getFramePrice(FrameType.FRAME_BG4), "abacus_frame_bg4"),
    )

    const val FRAME_CARDS_PER_ROW = 2

    data class FrameCardState(
        val card: FrameCard,
        val selected: Boolean,
        /** Kilit örtüsü (fiyat + altın) görünür mü. */
        val locked: Boolean,
    )

    fun frameCardStates(store: AbacusPrefsStore, ownedFrames: Map<String, FrameData>): List<FrameCardState> {
        val current = AbacusPreferences.getFrameType(store)
        return FRAME_CARDS.map { card ->
            FrameCardState(
                card = card,
                selected = card.frameType == current,
                locked = card.frameType != FrameType.FRAME_BG && ownedFrames[card.frameType.name]?.owned != true,
            )
        }
    }

    /**
     * Çerçeve kartına dokunuldu.
     * @return true → tip seçildi ve yazıldı; false → satın alma paneli açılır (tip DEĞİŞMEZ).
     */
    fun onFrameCardClicked(store: AbacusPrefsStore, ownedFrames: Map<String, FrameData>, frameType: FrameType): Boolean {
        if (!AbacusCustomizationRules.isFrameSelectable(ownedFrames, frameType)) return false
        AbacusPreferences.setFrameType(store, frameType)
        return true
    }

    /** Çerçeve satın alındı: sahiplik işaretlenir ve çerçeve seçili yapılır. */
    fun onFramePurchased(
        store: AbacusPrefsStore,
        ownedFrames: Map<String, FrameData>,
        frameType: FrameType,
    ): Map<String, FrameData> {
        AbacusPreferences.setFrameType(store, frameType)
        return AbacusCustomizationRules.withFrameOwned(ownedFrames, frameType)
    }

    // ── 4. sekme: çerçeve rengi ───────────────────────────────────────────────

    private fun frameColorLabels(type: FrameType): List<String> = when (type) {
        FrameType.FRAME_BG -> listOf("Ahşap rengi")
        FrameType.FRAME_BG2 -> listOf("Ahşap rengi", "Kalp rengi")
        FrameType.FRAME_BG3 -> listOf("Ahşap rengi", "Yıldız rengi")
        FrameType.FRAME_BG4 -> listOf("Renk 1", "Renk 2", "Renk 3", "Renk 4", "Renk 5")
    }

    /** inflateTab4.buildRows: kayıtlı çerçeve tipinin renk satırları. */
    fun frameColorRows(store: AbacusPrefsStore): List<ColorRow> {
        val type = AbacusPreferences.getFrameType(store)
        val colors = AbacusFrameRenderer.resolveFrameColors(store, type, null)
        return frameColorLabels(type).mapIndexed { i, label -> colorRow(i, label, colors[i]) }
    }

    /** Kaydırıcı değişti: kayıtlı çerçeve tipinin [index]. rengi yazılır; yeni renk döner. */
    fun setFrameColorFromSliders(store: AbacusPrefsStore, index: Int, hue: Int, lightness: Int): Int {
        val newColor = AbacusBeadRenderer.makeColor(hue, lightness)
        when (AbacusPreferences.getFrameType(store)) {
            FrameType.FRAME_BG -> if (index == 0) AbacusPreferences.setFrameBgWoodColor(store, newColor)
            FrameType.FRAME_BG2 -> when (index) {
                0 -> AbacusPreferences.setFrameBg2WoodColor(store, newColor)
                1 -> AbacusPreferences.setFrameBg2HeartColor(store, newColor)
            }
            FrameType.FRAME_BG3 -> when (index) {
                0 -> AbacusPreferences.setFrameBg3WoodColor(store, newColor)
                1 -> AbacusPreferences.setFrameBg3StarColor(store, newColor)
            }
            FrameType.FRAME_BG4 -> AbacusPreferences.setFrameBg4Color(store, index, newColor)
        }
        return newColor
    }

    /** 4. sekme kilidi: ahşap dışındaki çerçevelerde renk özelliği gerekir. */
    fun isFrameColorLocked(store: AbacusPrefsStore, ownedFrames: Map<String, FrameData>): Boolean =
        !AbacusCustomizationRules.isFrameColorUnlocked(ownedFrames, AbacusPreferences.getFrameType(store))

    /** 4. sekme kilidine dokununca açılan paneldeki görsel; ahşapta null (panel açılmaz). */
    fun frameColorFeatureImageResName(frameType: FrameType): String? =
        FRAME_CARDS.first { it.frameType == frameType }.purchaseImageResName
}

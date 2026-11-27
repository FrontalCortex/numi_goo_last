// KAYNAK:
//   app/src/main/java/com/example/app/PartSelectionFragment.kt
//     - loadStatesAndSetup()  → [PartSelectionLogic.lastChestComplete], [PartSelectionLogic.hasProPlan]
//     - finalizeSetup()       → [PartSelectionLogic.partState], [PartSelectionLogic.ADDITION_PARTS] vb.
//     - openMapForPart()      → [PartSelectionLogic.opensRacePanel], [PartSelectionLogic.racePanelTitle]
//   app/src/main/java/com/example/app/PartCardAdapter.kt
//     - PartCardViewHolder.bind() → [PartSelectionLogic.partTitle], [PartSelectionLogic.partAnimationName],
//                                   [PartSelectionLogic.partCardColor], [PartSelectionLogic.partLabelColor]
//   app/src/main/java/com/example/app/DynamicOffsetDecoration.kt
//     - getItemOffsets()      → [MapLayoutLogic.lessonHorizontalShift]
//
// Android'de yeni (platform işi): Sobi günlük test düğmesi (MapSobiTestButton), `openingPart` argümanı (Sobi sekmesinden
// kısma git), kilitli kart uyarısının Toast yerine LockedHint, abonelik süresi dolmuşsa Free sayma (PlanStatus.effectivePlan).
//
// Android'den bilinçli farklar: View / Lottie / Toast yok; R.color kimlikleri yerine renk kaynağı adları;
// DynamicOffsetDecoration piksel (Int) yerine dp/pt (Float) ile hesaplanır (kesirli kısım atılmaz).
package com.sorobit.shared.ders

/** Kısım (part) seçim ekranındaki bir kartın durumu (Android: PartState). */
data class PartState(
    val partId: Int,
    /** Kart renkli mi (false: koyu arka plan + gri yazı). */
    val visualActive: Boolean,
    /** Karta basınca kısım açılır mı (false: [inactiveMessage] gösterilir). */
    val functionalActive: Boolean,
    val inactiveMessage: String,
)

object PartSelectionLogic {

    /** "Toplama" satırındaki kısımlar. */
    val ADDITION_PARTS: List<Int> = listOf(1, 4, 7)

    /** "Çıkarma" satırındaki kısımlar. */
    val SUBTRACTION_PARTS: List<Int> = listOf(2, 5, 8)

    /** "Çarpma" satırındaki kısımlar. */
    val MULTIPLICATION_PARTS: List<Int> = listOf(3, 6)

    const val CATEGORY_TITLE_ADDITION = "Toplama"
    const val CATEGORY_TITLE_SUBTRACTION = "Çıkarma"
    const val CATEGORY_TITLE_MULTIPLICATION = "Çarpma"

    /** Android: `plan` Pro/Premium (büyük-küçük harf duyarsız) ya da onaylı öğretmen. */
    fun hasProPlan(plan: String, teacherApproved: Boolean): Boolean =
        plan.equals("Pro", ignoreCase = true) || plan.equals("Premium", ignoreCase = true) || teacherApproved

    /** Kısmın SON sandığı bitmiş mi (Android: `items.filter { TYPE_CHEST }.lastOrNull()?.stepIsFinish == true`). */
    fun lastChestComplete(items: List<LessonItem>): Boolean =
        items.lastOrNull { it.type == LessonItem.TYPE_CHEST }?.stepIsFinish == true

    /** Kapı mesajı (Android: ui_x_bolumunun_son_unite_maratonunu_bitir, Türkçe). */
    fun marathonGateMessage(prerequisitePart: Int): String =
        "Açmak için \"${gatePartTitle(prerequisitePart)}\" bölümünün son ünite maratonunu bitir."

    private fun gatePartTitle(partId: Int): String = when (partId) {
        1 -> "Abaküsün Temeli ve Toplama"
        2 -> "Abaküste Çıkarma"
        else -> "Abaküste Çarpma"
    }

    /**
     * [teacherApproved]: onaylı öğretmende bölüm sırası yoktur (Android): dersleri tek seferde açılır ama sandıkları bitmiş
     * sayılmaz, bu yüzden bütün kartlar (1–8) açık ve kullanılabilirdir. Kapı: 2–4 → 1. bölümün, 5 → 2.'nin, 6 → 3.'nün
     * SON ünite maratonu ([lastChestComplete]; maraton da 1 anahtarla açılabildiği için anahtarla da geçilebilir).
     */
    fun partState(
        partId: Int,
        hasProPlan: Boolean,
        part1ChestComplete: Boolean,
        part2ChestComplete: Boolean,
        part3ChestComplete: Boolean,
        teacherApproved: Boolean = false,
    ): PartState = if (teacherApproved) {
        PartState(partId, visualActive = true, functionalActive = true, inactiveMessage = "")
    } else when (partId) {
        1 -> PartState(1, visualActive = true, functionalActive = true, inactiveMessage = "")
        2 -> PartState(2, part1ChestComplete, part1ChestComplete, marathonGateMessage(1))
        3 -> PartState(3, part1ChestComplete, part1ChestComplete, marathonGateMessage(1))
        4 -> PartState(4, part1ChestComplete, part1ChestComplete, marathonGateMessage(1))
        5 -> PartState(5, part2ChestComplete, part2ChestComplete, marathonGateMessage(2))
        6 -> PartState(6, part3ChestComplete, part3ChestComplete, marathonGateMessage(3))
        7 -> {
            val functional = hasProPlan && part1ChestComplete
            val msg = if (!hasProPlan) "Bu kısmı açmak için planı Pro'ya yükselt." else "Abaküste Toplama kısmını bitir."
            PartState(7, visualActive = hasProPlan, functionalActive = functional, inactiveMessage = msg)
        }
        8 -> {
            val functional = hasProPlan && part2ChestComplete
            val msg = if (!hasProPlan) "Bu kısmı açmak için planı Pro'ya yükselt." else "Abaküste Çıkarma kısmını bitir."
            PartState(8, visualActive = hasProPlan, functionalActive = functional, inactiveMessage = msg)
        }
        else -> PartState(partId, visualActive = true, functionalActive = true, inactiveMessage = "")
    }

    /** [partIds] sırasıyla kart durumları. */
    fun partStates(
        partIds: List<Int>,
        hasProPlan: Boolean,
        part1ChestComplete: Boolean,
        part2ChestComplete: Boolean,
        part3ChestComplete: Boolean,
        teacherApproved: Boolean = false,
    ): List<PartState> =
        partIds.map { partState(it, hasProPlan, part1ChestComplete, part2ChestComplete, part3ChestComplete, teacherApproved) }

    fun partTitle(partId: Int): String = when (partId) {
        1 -> "Abaküsün Temeli ve Toplama"
        2 -> "Abaküste Çıkarma"
        3 -> "Abaküste Çarpma"
        4 -> "Körleme Toplama"
        5 -> "Körleme Çıkarma"
        6 -> "Körleme Çarpma"
        7 -> "Ustalık Yolu - Toplama"
        8 -> "Ustalık Yolu - Çıkarma"
        else -> "Part : $partId"
    }

    /** Karttaki Lottie dosyasının adı (Android assets). */
    fun partAnimationName(partId: Int): String = when (partId) {
        1 -> "roller_coaster_anim.json"
        2 -> "rocket_anim.json"
        3 -> "sailboat_anim.json"
        4 -> "ferris_wheel_anim.json"
        5 -> "piata_anim.json"
        6 -> "flying_saucer_anim.json"
        7 -> "violin_anim.json"
        8 -> "skateboard_anim.json"
        else -> "dinosaur_anim.json"
    }

    /** Kart arka planı (renk kaynağı adı): etkin değilse "background_color". */
    fun partCardColor(state: PartState): String =
        if (!state.visualActive) {
            "background_color"
        } else {
            when (state.partId) {
                2, 5, 8 -> "lesson_header_green"
                3, 6 -> "lesson_header_blue"
                else -> "lesson_header_orange"
            }
        }

    /** Kart yazısı (renk kaynağı adı): etkin "white", değilse "lesson_locked". */
    fun partLabelColor(state: PartState): String = if (state.visualActive) "white" else "lesson_locked"

    /** Kısım 7 ve 8 harita yerine doğrudan yarış panelini açar. */
    fun opensRacePanel(partId: Int): Boolean = partId == 7 || partId == 8

    /** [opensRacePanel] true ise yarış panelinin başlığı (Android: dummyItem.title). */
    fun racePanelTitle(partId: Int): String = if (partId == 7) "Toplama Ustalığı" else "Çıkarma Ustalığı"
}

/** Harita listesinin görünümden bağımsız yerleşim hesabı. */
object MapLayoutLogic {
    /** dimens.xml: max_lesson_offset. */
    const val MAX_LESSON_OFFSET = 100f

    /** dimens.xml: map_lesson_card_size. */
    const val LESSON_CARD_SIZE = 85f

    /** dimens.xml: map_lesson_item_padding. */
    const val LESSON_ITEM_PADDING = 17f

    /** dimens.xml: map_lesson_offset_edge_margin. */
    const val LESSON_OFFSET_EDGE_MARGIN = 8f

    /**
     * Ders kartının yatay kayması (dp/pt; eksi sola). Yalnızca TYPE_LESSON kayar; `offset / 50 * maxOffset`
     * istenir, dar ekranda kart taşmasın diye liste genişliğine göre sınırlanır. [innerWidth] <= 0 ise
     * (genişlik henüz bilinmiyor) sınırlama yapılmaz.
     */
    fun lessonHorizontalShift(
        type: Int,
        offset: Int,
        innerWidth: Float,
        maxOffset: Float,
        lessonCardSize: Float,
        lessonItemHorizontalPaddingTotal: Float,
        edgeSafetyMargin: Float,
    ): Float {
        if (type != LessonItem.TYPE_LESSON) return 0f
        // Android `offset / 50f * maxOffsetPx` yazıp Int'e keser; burada kesme olmadığı için önce çarpılır
        // (30 * 100 / 50 = tam 60; öbür sırada 60.000004 çıkıyordu).
        val desired = offset * maxOffset / 50f
        if (innerWidth <= 0f) return desired
        val usableHalf = (innerWidth - lessonItemHorizontalPaddingTotal - lessonCardSize) / 2f - edgeSafetyMargin
        val maxAllowedShift = usableHalf.coerceAtLeast(0f)
        return desired.coerceIn(-maxAllowedShift, maxAllowedShift)
    }

    /** [lessonHorizontalShift]'in dimens.xml değerleriyle çağrılan kısa biçimi. */
    fun lessonHorizontalShiftDefault(item: LessonItem, innerWidth: Float): Float =
        lessonHorizontalShift(
            type = item.type,
            offset = item.offset,
            innerWidth = innerWidth,
            maxOffset = MAX_LESSON_OFFSET,
            lessonCardSize = LESSON_CARD_SIZE,
            lessonItemHorizontalPaddingTotal = LESSON_ITEM_PADDING * 2f,
            edgeSafetyMargin = LESSON_OFFSET_EDGE_MARGIN,
        )
}

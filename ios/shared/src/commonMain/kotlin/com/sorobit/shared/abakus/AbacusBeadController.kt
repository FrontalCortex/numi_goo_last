// Kaynak (Android): app/src/main/java/com/example/app/abacus/AbacusBeadController.kt
//
// 5 çubuklu soroban'ın görünümden bağımsız durum makinesi. Android'deki denetleyici satır satır
// taşındı; View'a özgü parçalar şu soyutlamalarla değiştirildi:
//  - ImageView                → [AbacusBead] (bottomMargin + translationY taşıyan saf model)
//  - bead.animate()...start() → [AbacusBeadHost.animateTranslationY] / [AbacusBeadHost.cancelAnimation]
//  - root.postDelayed         → [AbacusBeadHost.postDelayed]
//  - AbacusSoundPlayer        → [AbacusBeadHost.playBeadClick]
//  - setImageDrawable         → [AbacusBeadHost.updateBeadAppearance]
//  - MotionEvent              → [AbacusTouchAction] + rawY + eventTime parametreleri
//  - ViewConfiguration.scaledTouchSlop, R.dimen.abacus_bead_step_* → kurucu parametreleri
//
// Android'den farklar: Log / TutorialBeadDiagnostics / AgentDebugLog çağrıları kaldırıldı.
// Davranış (durum geçişleri, eşikler, yuvarlamalar) aynıdır.
//
// Çubuk eşlemesi: rod0 = on binler, rod1 = binler, rod2 = yüzler, rod3 = onlar, rod4 = birler.
package com.sorobit.shared.abakus

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Tek bir boncuğun fiziksel durumu (Android'de ImageView'ın layoutParams.bottomMargin ve
 * translationY değerleri). Görünüm katmanı [onChanged] ile değişiklikleri dinleyip ekrana yansıtır;
 * animasyon sürerken ara [translationY] değerini bu modele GERİ YAZMALIDIR (Android'de
 * ViewPropertyAnimator view.translationY'yi her karede günceller; mantık bu ara değeri okur).
 */
class AbacusBead(
    val rod: Int,
    val isTop: Boolean,
    /** Alt boncuklar için 0..3 (0 = bariyere en yakın, "bottom1"); üst boncuk için 0. */
    val beadIndex: Int,
) {
    var onChanged: ((AbacusBead) -> Unit)? = null

    /** px; yalnızca alt boncuklarda kullanılır. */
    var bottomMargin: Int = 0
        set(value) {
            field = value
            onChanged?.invoke(this)
        }

    /** px; aşağı yön pozitif (Android View.translationY ile aynı). */
    var translationY: Float = 0f
        set(value) {
            field = value
            onChanged?.invoke(this)
        }

    /** Android view id adı: "rod{R}_bead_top" / "rod{R}_bead_bottom{1..4}". */
    val idName: String
        get() = if (isTop) "rod${rod}_bead_top" else "rod${rod}_bead_bottom${beadIndex + 1}"
}

/** Görünüm katmanının sağladığı platform işlevleri. */
interface AbacusBeadHost {
    /**
     * `bead.animate().setDuration(durationMs).setInterpolator(AccelerateDecelerateInterpolator())
     * .withEndAction(endAction).translationY(targetTranslationY).start()` karşılığı.
     * Aynı boncukta süren animasyon varsa onun yerini alır (öncekinin endAction'ı ÇALIŞMAZ).
     * Animasyon doğal olarak bitince [endAction] çağrılır; iptal edilirse çağrılmaz.
     */
    fun animateTranslationY(bead: AbacusBead, targetTranslationY: Float, durationMs: Long, endAction: () -> Unit)

    /** `bead.animate().cancel()`: boncuk o anki translationY'de kalır, endAction çalışmaz. */
    fun cancelAnimation(bead: AbacusBead)

    /** `root.postDelayed(action, delayMs)`. */
    fun postDelayed(delayMs: Long, action: () -> Unit)

    /** `AbacusSoundPlayer.playBeadClick(context, activating)`. */
    fun playBeadClick(activating: Boolean)

    /** Boncuğun seçili/seçili değil görünümünü günceller (Android: setImageDrawable). */
    fun updateBeadAppearance(bead: AbacusBead, isSelected: Boolean)
}

/** MotionEvent.actionMasked karşılığı. */
enum class AbacusTouchAction { DOWN, MOVE, UP, CANCEL, POINTER_UP, OUTSIDE, OTHER }

class AbacusBeadController(
    private val host: AbacusBeadHost,
    /** `ViewConfiguration.get(context).scaledTouchSlop` (px). */
    private val touchSlopPx: Int,
    /** `R.dimen.abacus_bead_step_bottom` (48dp) px karşılığı. */
    private val bottomStepPx: Float,
    /** `R.dimen.abacus_bead_step_top` (32dp) px karşılığı. */
    private val topStepPx: Float,
    private val animationDurationMs: Long = 300L,
) {
    private val animatingBeads = mutableSetOf<AbacusBead>()
    private var touchEnabled: Boolean = true
    private var resetInProgress: Boolean = false

    /**
     * Boncuk durumu (bottomCount / topDown) her yazıldığında çağrılır: dokunma, sürükleme,
     * sıfırlama, değerden yerleştirme. Android: `onValueChanged` (ödev kural çözümlemesi dinliyor).
     * Android her yazımın hemen ardından çağırır (değer değişmese de); aynı davranış korunmuştur.
     */
    var onValueChanged: (() -> Unit)? = null

    private var bottomMoveDistancePx: Float = bottomStepPx
    private var topMoveDistancePx: Float = topStepPx

    /** Ayarlar panelinden eklenen boncuk margin offset'leri (px). */
    private var beadMarginBottomOffsetPx: Float = 0f
    private var beadMarginTopOffsetPx: Float = 0f

    private var initialPositionsCaptured = false
    private val initialBottomMargins: Array<IntArray> = Array(5) { IntArray(4) }

    /** bottomCount[rod] 0..4 (kaç alt boncuk yukarıda) */
    private val bottomCount = IntArray(5) { 0 }
    /** topDown[rod] üst boncuk aşağıdaysa true (o çubuğa 5 ekler) */
    private val topDown = BooleanArray(5) { false }

    /** bottomBeads[rod][0..3] — Android: rod{R}_bead_bottom1..4 */
    val bottomBeads: Array<Array<AbacusBead>> = Array(5) { rod ->
        Array(4) { i -> AbacusBead(rod = rod, isTop = false, beadIndex = i) }
    }

    /** topBeads[rod] — Android: rod{R}_bead_top */
    val topBeads: Array<AbacusBead> = Array(5) { rod ->
        AbacusBead(rod = rod, isTop = true, beadIndex = 0)
    }

    private sealed class DragSession

    private class BottomDragSession(
        val rod: Int,
        val beadNumber: Int,
        val startCount: Int,
        val targetCount: Int,
        val directionUp: Boolean,
        val affected: Array<AbacusBead>,
        val downRawY: Float,
        var isDragging: Boolean,
        var provisionalApplied: Boolean,
        var lastRawY: Float,
        var lastEventTimeMs: Long,
        var currentProgressPx: Float,
        var currentVelocityAbsPxPerMs: Float
    ) : DragSession()

    private class TopDragSession(
        val rod: Int,
        val bead: AbacusBead,
        val startDown: Boolean,
        val targetDown: Boolean,
        val downRawY: Float,
        var isDragging: Boolean,
        var provisionalApplied: Boolean,
        var lastRawY: Float,
        var lastEventTimeMs: Long,
        var currentProgressPx: Float,
        var currentVelocityAbsPxPerMs: Float
    ) : DragSession()

    private var activeDrag: DragSession? = null

    /** Tutorial [BeadAnimation] gibi denetleyici dışı boncuk animasyonları sürerken true dönmeli. */
    private var externalBeadAnimationInProgress: () -> Boolean = { false }

    fun setExternalBeadAnimationInProgress(checker: () -> Boolean) {
        externalBeadAnimationInProgress = checker
    }

    private fun shouldDeferAppearanceDuringSync(): Boolean =
        animatingBeads.isNotEmpty() || externalBeadAnimationInProgress()

    private fun isAnyBottomAnimating(rod: Int): Boolean {
        return bottomBeads[rod].any { it in animatingBeads }
    }

    /**
     * Android'de ses ön yüklemesi + dokunma dinleyicilerinin bağlanması da burada yapılıyordu;
     * onlar görünüm katmanının işi. Burada yalnızca başlangıç margin'leri yakalanır.
     */
    fun setup() {
        captureInitialPositionsIfNeeded()
    }

    fun setEnabled(enabled: Boolean) {
        touchEnabled = enabled
    }

    fun isResetInProgress(): Boolean = resetInProgress

    /** Kullanıcı tıklama/sürükleme veya tutorial boncuk animasyonu sürüyor mu. */
    fun isBeadAnimationInProgress(): Boolean =
        animatingBeads.isNotEmpty() || externalBeadAnimationInProgress()

    /**
     * Çalışma zamanı hareket mesafelerini bariyer aralıklarından hesaplar.
     * [dynamic] = `AbacusBeadMetrics.fromBarrierDistances(...)` sonucu (ölçüm başarısızsa null).
     * Dinamik ölçüm başarılıysa true, yedek (dimen) değerler kullanılıyorsa false döner.
     *
     * DİKKAT (Android davranışı): `force = false` iken mesafeler zaten pozitifse ölçüme bakmadan
     * true döner — yani "dinamik ölçüm başarılı" anlamına gelmeyebilir.
     */
    fun computeMovementDistancesFromLayout(
        dynamic: AbacusBeadMetrics.MoveDistancesPx?,
        force: Boolean = true,
    ): Boolean {
        if (!force && bottomMoveDistancePx > 0f && topMoveDistancePx > 0f) return true
        if (dynamic != null) {
            bottomMoveDistancePx = dynamic.bottomPx.coerceAtLeast(1f)
            topMoveDistancePx = dynamic.topPx.coerceAtLeast(1f)
            return true
        }
        // Ölçüm başarısızsa dimen yedeği kalır.
        bottomMoveDistancePx = (bottomStepPx - beadMarginBottomOffsetPx).coerceAtLeast(1f)
        topMoveDistancePx = (topStepPx - beadMarginTopOffsetPx).coerceAtLeast(1f)
        return false
    }

    /**
     * Ayarlar panelinden eklenen boncuk margin değerlerini px olarak ayarlar.
     * Bu değerler (yalnızca yedek yolda) hareket mesafesinden çıkarılır.
     */
    fun setBeadMarginOffsets(bottomOffsetPx: Float, topOffsetPx: Float) {
        beadMarginBottomOffsetPx = bottomOffsetPx.coerceAtLeast(0f)
        beadMarginTopOffsetPx = topOffsetPx.coerceAtLeast(0f)
    }

    /** Alt boncuk adım mesafesi (px); dokunma/sürükleme ile aynı değer. */
    fun getBottomMoveDistancePx(): Float = bottomMoveDistancePx

    /** Üst boncuk adım mesafesi (px). */
    fun getTopMoveDistancePx(): Float = topMoveDistancePx

    /** Margin okumadan [bottomCount]/[topDown] ayarlar — tutorial kısmi geçişte dokunma durumu. */
    fun applyInternalStateFromValue(value: Int, applyAppearance: Boolean = false) {
        if (resetInProgress) return
        val padded = value.coerceIn(0, 99999).toString().padStart(5, '0')
        for (rod in 0..4) {
            val digit = padded[rod].digitToInt()
            bottomCount[rod] = digit % 5; onValueChanged?.invoke()
            topDown[rod] = digit >= 5; onValueChanged?.invoke()
        }
        // Koşullarda teşhis bayrağı ARANMAZ (Android'de kaldırıldı): applyAppearance = false iken ya da
        // animasyon sürerken görünüm güncellenmez, aksi hâlde güncellenir.
        when {
            !applyAppearance -> Unit
            shouldDeferAppearanceDuringSync() -> Unit
            else -> updateAllAppearance()
        }
    }

    /** Alttan ardışık kaç alt boncuk yukarıda (soroban yığını; sync ile aynı kural). */
    fun countConsecutiveRaisedBottomBeads(rod: Int): Int {
        if (rod !in 0..4) return 0
        captureInitialPositionsIfNeeded()
        val raisedThreshold = bottomMoveDistancePx / 2f
        var count = 0
        for (i in 0..3) {
            val bead = bottomBeads[rod][i]
            val delta =
                bead.bottomMargin - initialBottomMargins[rod][i] + bead.translationY
            if (delta >= raisedThreshold) {
                count = i + 1
            } else {
                break
            }
        }
        return count
    }

    /**
     * Tek bir alt boncuk fiziksel olarak yukarıda mı (margin+translation; ardışık yığım varsayımı yok).
     * DİKKAT (Android davranışı): translationY aşağı yönde pozitif olduğu hâlde delta'ya EKLENİR
     * (yukarı doğru süren animasyonda negatif translation delta'yı azaltır); aynen korunmuştur.
     */
    fun isBottomBeadPhysicallyRaised(rod: Int, beadIndex: Int): Boolean {
        if (rod !in 0..4 || beadIndex !in 0..3) return false
        captureInitialPositionsIfNeeded()
        val bead = bottomBeads[rod][beadIndex]
        val delta =
            bead.bottomMargin - initialBottomMargins[rod][beadIndex] + bead.translationY
        return delta >= bottomMoveDistancePx / 2f
    }

    /** Üst boncuk aşağıda mı (+5 konumu). */
    fun isTopBeadDown(rod: Int): Boolean {
        if (rod !in 0..4) return false
        val top = topBeads[rod]
        return top.translationY >= topMoveDistancePx / 2f
    }

    /**
     * İç durumu boncukların o anki fiziksel konumundan yeniden okur (tutorial animasyonları
     * boncukları bu denetleyiciden geçmeden oynatabildiği için gerekir).
     */
    fun syncStateFromUi(applyAppearance: Boolean = true) {
        // Sıfırlama animasyonu sürerken görsel durum yeniden okunmaz.
        if (resetInProgress) return

        captureInitialPositionsIfNeeded()

        for (rod in 0..4) {
            val count = countConsecutiveRaisedBottomBeads(rod)
            bottomCount[rod] = count; onValueChanged?.invoke()

            // Üst boncuk durumu çizimden değil görsel konumdan çıkarılır.
            val top = topBeads[rod]
            val topByTranslation = top.translationY >= (topMoveDistancePx / 2f)
            topDown[rod] = topByTranslation; onValueChanged?.invoke()
        }

        if (!shouldDeferAppearanceDuringSync() && applyAppearance) {
            updateAllAppearance()
        }
    }

    /**
     * Alt boncuk dokunma dinleyicisi (Android: bindBottomBeadTouchListener içindeki lambda).
     * [beadNumber] 1..4. Dönüş değeri OnTouchListener'ın döndürdüğü "tüketildi" bayrağıdır.
     */
    fun onBottomBeadTouch(rod: Int, beadNumber: Int, action: AbacusTouchAction, rawY: Float, eventTime: Long): Boolean {
        if (!touchEnabled) return true
        val actionDown = action == AbacusTouchAction.DOWN

        // Önceki oturum takılı kaldıysa (UP gelmediyse) yeni DOWN yeniden başlatır.
        val active = activeDrag
        if (active != null && actionDown) {
            activeDrag = null
        }

        when (action) {
            AbacusTouchAction.DOWN -> {
                // Bu çubukta animasyon ortasında alt boncuk varsa durdur ve temiz konuma yapıştır.
                if (isAnyBottomAnimating(rod)) {
                    interruptAndSnapBottomBeads(rod)
                }

                val startCount = bottomCount[rod]
                val target = if (startCount >= beadNumber) beadNumber - 1 else beadNumber
                val targetCount = target.coerceIn(0, 4)
                val directionUp = targetCount > startCount
                val clampedTarget = targetCount
                if (clampedTarget == startCount) {
                    // Değişiklik yok; dokunma gibi ele al.
                    activeDrag = BottomDragSession(
                        rod = rod,
                        beadNumber = beadNumber,
                        startCount = startCount,
                        targetCount = startCount,
                        directionUp = false,
                        affected = emptyArray(),
                        downRawY = rawY,
                        isDragging = false,
                        provisionalApplied = false,
                        lastRawY = rawY,
                        lastEventTimeMs = eventTime,
                        currentProgressPx = 0f,
                        currentVelocityAbsPxPerMs = 0f
                    )
                    return true
                }

                val from = min(startCount, clampedTarget) + 1
                val to = max(startCount, clampedTarget)
                val affected = (from..to)
                    .map { n -> bottomBeads[rod][n - 1] }
                    .toTypedArray()

                activeDrag = BottomDragSession(
                    rod = rod,
                    beadNumber = beadNumber,
                    startCount = startCount,
                    targetCount = clampedTarget,
                    directionUp = directionUp,
                    affected = affected,
                    downRawY = rawY,
                    isDragging = false,
                    provisionalApplied = false,
                    lastRawY = rawY,
                    lastEventTimeMs = eventTime,
                    currentProgressPx = 0f,
                    currentVelocityAbsPxPerMs = 0f
                )
                return true
            }

            AbacusTouchAction.MOVE -> {
                val session = activeDrag as? BottomDragSession ?: return false
                if (session.affected.isEmpty()) {
                    // Sürüklenecek bir şey yok; dokunma için UP beklenir.
                    return true
                }

                val dy = rawY - session.downRawY
                val progress = if (session.directionUp) {
                    (-dy).coerceIn(0f, bottomMoveDistancePx)
                } else {
                    (dy).coerceIn(0f, bottomMoveDistancePx)
                }

                val dtMs = (eventTime - session.lastEventTimeMs).coerceAtLeast(1L)
                val deltaRawY = rawY - session.lastRawY
                val velocityAbs = abs(deltaRawY) / dtMs.toFloat()
                session.currentVelocityAbsPxPerMs = velocityAbs
                session.lastRawY = rawY
                session.lastEventTimeMs = eventTime

                if (!session.isDragging && abs(dy) > touchSlopPx && progress > 0f) {
                    session.isDragging = true
                    // Geçici durum: tıklamadaki gibi görünüm hemen güncellenir.
                    bottomCount[session.rod] = session.targetCount; onValueChanged?.invoke()
                    updateRodAppearance(session.rod)

                    // Süren animasyonları iptal et ve translation denetimini al.
                    for (b in session.affected) {
                        host.cancelAnimation(b)
                        animatingBeads.add(b)
                    }
                    session.provisionalApplied = true
                }

                if (session.isDragging) {
                    val translation = if (session.directionUp) -progress else progress
                    for (b in session.affected) {
                        b.translationY = translation
                    }
                    session.currentProgressPx = progress
                }
                return true
            }

            AbacusTouchAction.UP, AbacusTouchAction.CANCEL -> {
                val session = activeDrag as? BottomDragSession ?: return false

                if (!session.isDragging) {
                    // Geçerli sürükleme başlamadıysa her zaman dokunma sayılmaz:
                    // "yasak yöne sürükleme" dokunma gibi boncuk çevirmesin.
                    val totalDyAbs = abs(rawY - session.downRawY)
                    if (totalDyAbs <= touchSlopPx) {
                        // Gerçek dokunma
                        onBottomBeadClicked(session.rod, session.beadNumber)
                    } else {
                        // Yanlış yöne sürükleme denemesi; iptal (çevirme yok).
                        for (b in session.affected) {
                            host.cancelAnimation(b)
                            b.translationY = 0f
                        }
                    }
                } else {
                    val commit = session.currentProgressPx >= (bottomMoveDistancePx / 2f)
                    val durationMs = computeDurationMs(
                        velocityAbsPxPerMs = session.currentVelocityAbsPxPerMs,
                        moveDistancePx = bottomMoveDistancePx
                    )

                    if (commit) {
                        host.playBeadClick(activating = session.directionUp)
                        // Son durum: mantıksal sayı targetCount olsun, sonra fiziksel hareket.
                        if (!session.provisionalApplied) {
                            bottomCount[session.rod] = session.targetCount; onValueChanged?.invoke()
                            updateRodAppearance(session.rod)
                        }
                        if (session.directionUp) {
                            animateBeadsUpWithDuration(durationMs, *session.affected)
                        } else {
                            animateBeadsDownWithDuration(durationMs, *session.affected)
                        }
                    } else {
                        // Başa geri dön (margin değişmez; yalnızca translation 0'a döner).
                        if (session.provisionalApplied) {
                            bottomCount[session.rod] = session.startCount; onValueChanged?.invoke()
                            updateRodAppearance(session.rod)
                        }

                        for (b in session.affected) {
                            host.cancelAnimation(b)
                            host.animateTranslationY(b, 0f, durationMs) {
                                b.translationY = 0f
                                animatingBeads.remove(b)
                            }
                        }
                    }
                }

                activeDrag = null
                return true
            }

            AbacusTouchAction.POINTER_UP, AbacusTouchAction.OUTSIDE -> {
                // Dokunma dizisi kesildi; activeDrag takılı kalmasın.
                activeDrag = null
                return true
            }

            else -> return true
        }
    }

    /** Üst boncuk dokunma dinleyicisi (Android: bindTopBeadTouchListener içindeki lambda). */
    fun onTopBeadTouch(rod: Int, action: AbacusTouchAction, rawY: Float, eventTime: Long): Boolean {
        if (!touchEnabled) return true
        val beadView = topBeads[rod]
        when (action) {
            AbacusTouchAction.DOWN -> {
                if (beadView in animatingBeads) {
                    interruptAndSnapTopBead(rod)
                }
                // Alt boncuklar animasyondayken de üst boncukla etkileşime izin verilir.

                val startDown = topDown[rod]
                val targetDown = !startDown

                activeDrag = TopDragSession(
                    rod = rod,
                    bead = beadView,
                    startDown = startDown,
                    targetDown = targetDown,
                    downRawY = rawY,
                    isDragging = false,
                    provisionalApplied = false,
                    lastRawY = rawY,
                    lastEventTimeMs = eventTime,
                    currentProgressPx = 0f,
                    currentVelocityAbsPxPerMs = 0f
                )
                return true
            }

            AbacusTouchAction.MOVE -> {
                val session = activeDrag as? TopDragSession ?: return false

                val dy = rawY - session.downRawY
                val progress = if (directionTowardsDown(session.targetDown)) {
                    dy.coerceIn(0f, topMoveDistancePx)
                } else {
                    (-dy).coerceIn(0f, topMoveDistancePx)
                }

                val dtMs = (eventTime - session.lastEventTimeMs).coerceAtLeast(1L)
                val deltaRawY = rawY - session.lastRawY
                val velocityAbs = abs(deltaRawY) / dtMs.toFloat()
                session.currentVelocityAbsPxPerMs = velocityAbs
                session.lastRawY = rawY
                session.lastEventTimeMs = eventTime

                if (!session.isDragging && abs(dy) > touchSlopPx && progress > 0f) {
                    session.isDragging = true
                    topDown[session.rod] = session.targetDown; onValueChanged?.invoke()
                    updateTopAppearance(session.rod)
                    host.cancelAnimation(session.bead)
                    animatingBeads.add(session.bead)
                    session.provisionalApplied = true
                }

                if (session.isDragging) {
                    val translation = if (session.targetDown) progress else (topMoveDistancePx - progress)
                    session.bead.translationY = translation
                    session.currentProgressPx = progress
                }
                return true
            }

            AbacusTouchAction.UP, AbacusTouchAction.CANCEL -> {
                val session = activeDrag as? TopDragSession ?: return false

                if (!session.isDragging) {
                    val totalDyAbs = abs(rawY - session.downRawY)
                    if (totalDyAbs <= touchSlopPx) {
                        toggleTopBead(session.rod)
                    } else {
                        // Yasak yöne sürükleme denemesi; iptal (çevirme yok).
                    }
                } else {
                    val commit = session.currentProgressPx >= (topMoveDistancePx / 2f)
                    val durationMs = computeDurationMs(
                        velocityAbsPxPerMs = session.currentVelocityAbsPxPerMs,
                        moveDistancePx = topMoveDistancePx
                    )

                    if (commit) {
                        host.playBeadClick(activating = session.targetDown)
                        if (!session.provisionalApplied) {
                            topDown[session.rod] = session.targetDown; onValueChanged?.invoke()
                            updateTopAppearance(session.rod)
                        }
                        if (session.targetDown) {
                            animateTopBeadDownWithDuration(durationMs, session.bead)
                        } else {
                            animateTopBeadUpWithDuration(durationMs, session.bead)
                        }
                    } else {
                        // Başlangıç durumuna geri dön.
                        if (session.provisionalApplied) {
                            topDown[session.rod] = session.startDown; onValueChanged?.invoke()
                            updateTopAppearance(session.rod)
                        }

                        val targetTranslation = if (session.startDown) topMoveDistancePx else 0f
                        host.cancelAnimation(session.bead)
                        host.animateTranslationY(session.bead, targetTranslation, durationMs) {
                            session.bead.translationY = targetTranslation
                            animatingBeads.remove(session.bead)
                        }
                    }
                }

                activeDrag = null
                return true
            }

            AbacusTouchAction.POINTER_UP, AbacusTouchAction.OUTSIDE -> {
                activeDrag = null
                return true
            }

            else -> return true
        }
    }

    private fun directionTowardsDown(targetDown: Boolean): Boolean = targetDown

    /** Sürükleme bırakıldığında hıza göre animasyon süresi (Android'de private). */
    fun computeDurationMs(
        velocityAbsPxPerMs: Float,
        moveDistancePx: Float
    ): Long {
        if (velocityAbsPxPerMs <= 0f) return animationDurationMs
        val expected = moveDistancePx / animationDurationMs.toFloat()
        val factor = (expected / velocityAbsPxPerMs).coerceIn(0.35f, 1.6f)
        return (animationDurationMs.toFloat() * factor).roundToInt().coerceAtLeast(60).toLong()
    }

    fun reset(animate: Boolean = true) {
        resetInProgress = true
        captureInitialPositionsIfNeeded()

        // Animasyon ortasındakilerin o anki translation'ı margin'e işlenir.
        animatingBeads.clear()
        for (rod in 0..4) {
            val top = topBeads[rod]
            host.cancelAnimation(top)
        }
        for (rod in 0..4) {
            for (i in 0..3) {
                commitBottomTranslationToMargin(bottomBeads[rod][i])
            }
        }

        if (animate) {
            // "Geri dokunma" gibi sıfır durumuna animasyonla dön.
            for (rod in 0..4) {
                val count = bottomCount[rod]
                if (count > 0) {
                    val beads = bottomBeads[rod].take(count).toTypedArray()
                    if (beads.any { it in animatingBeads }) continue
                    animateBeadsDown(*beads)
                }
                // Üst boncuk için iç duruma değil GÖRSEL konuma bakılır.
                val top = topBeads[rod]
                val topVisuallyDown = top.translationY >= (topMoveDistancePx / 2f)
                if (topVisuallyDown && top !in animatingBeads) {
                    animateTopBeadUp(top)
                }
            }
        } else {
            // Sert sıfırlama (animasyonsuz)
            for (rod in 0..4) {
                for (i in 0..3) {
                    val b = bottomBeads[rod][i]
                    host.cancelAnimation(b)
                    b.bottomMargin = initialBottomMargins[rod][i]
                    b.translationY = 0f
                }
                val top = topBeads[rod]
                host.cancelAnimation(top)
                top.translationY = 0f
            }
            animatingBeads.clear()
        }

        for (i in 0..4) {
            bottomCount[i] = 0; onValueChanged?.invoke()
            topDown[i] = false; onValueChanged?.invoke()
        }
        updateAllAppearance()

        if (!animate) {
            resetInProgress = false
        } else {
            // Sıfırlama animasyonları eşzamansız; bayrak muhtemel bitişten sonra temizlenir.
            val clearDelay = animationDurationMs + 50L
            host.postDelayed(clearDelay) { resetInProgress = false }
        }
    }

    fun getCurrentValue(): Int {
        var value = 0
        // rod4 = birler, rod3 = onlar, ..., rod0 = on binler
        for (rod in 0..4) {
            val digit = bottomCount[rod] + (if (topDown[rod]) 5 else 0)
            val multiplier = when (rod) {
                4 -> 1
                3 -> 10
                2 -> 100
                1 -> 1000
                else -> 10000
            }
            value += digit * multiplier
        }
        return value.coerceIn(0, 99999)
    }

    /** Test/eşleme kolaylığı: çubuğun mantıksal alt boncuk sayısı (Android'de private alan). */
    fun getBottomCount(rod: Int): Int = bottomCount[rod]

    /** Test/eşleme kolaylığı: çubuğun mantıksal üst boncuk durumu (Android'de private alan). */
    fun isTopDownLogical(rod: Int): Boolean = topDown[rod]

    /**
     * AbacusFragment rehber sistemi kullanır. "rod2BottomBead3" / "rod4TopBead" gibi kimlikler alır.
     */
    fun animateGuideBead(beadId: String) {
        val rod = beadId.substringAfter("rod", "").takeWhile { it.isDigit() }.toIntOrNull() ?: return
        if (rod !in 0..4) return
        when {
            beadId.contains("TopBead") -> toggleTopBead(rod)
            beadId.contains("BottomBead") -> {
                val n = beadId.substringAfter("BottomBead", "").toIntOrNull() ?: return
                if (n in 1..4) onBottomBeadClicked(rod, n)
            }
        }
    }

    private fun onBottomBeadClicked(rod: Int, beadNumber: Int) {
        // Bu çubukta animasyon ortasında alt boncuk varsa önce durdur ve yapıştır
        if (isAnyBottomAnimating(rod)) {
            interruptAndSnapBottomBeads(rod)
        }

        val current = bottomCount[rod]
        val target = if (current >= beadNumber) beadNumber - 1 else beadNumber
        val clampedTarget = target.coerceIn(0, 4)
        if (clampedTarget == current) return
        host.playBeadClick(activating = clampedTarget > current)

        val from = min(current, clampedTarget) + 1
        val to = max(current, clampedTarget)
        val affected = (from..to)
            .map { n -> bottomBeads[rod][n - 1] }
            .toTypedArray()

        if (clampedTarget > current) {
            animateBeadsUp(*affected)
        } else {
            animateBeadsDown(*affected)
        }
        bottomCount[rod] = clampedTarget; onValueChanged?.invoke()
        updateRodAppearance(rod)
    }

    private fun toggleTopBead(rod: Int) {
        val bead = topBeads[rod]
        if (bead in animatingBeads) {
            interruptAndSnapTopBead(rod)
        }
        host.playBeadClick(activating = !topDown[rod])
        if (!topDown[rod]) {
            animateTopBeadDown(bead)
            topDown[rod] = true; onValueChanged?.invoke()
        } else {
            animateTopBeadUp(bead)
            topDown[rod] = false; onValueChanged?.invoke()
        }
        updateTopAppearance(rod)
    }

    private fun animateBeadsUp(vararg beads: AbacusBead) = animateBeadsUpWithDuration(animationDurationMs, *beads)

    private fun animateBeadsDown(vararg beads: AbacusBead) = animateBeadsDownWithDuration(animationDurationMs, *beads)

    /** DİKKAT (Android davranışı): tıklamada hedef `roundToInt()` ile yuvarlanmış mesafedir. */
    private fun animateTopBeadDown(bead: AbacusBead) {
        val moveDistance = topMoveDistancePx.roundToInt()
        animatingBeads.add(bead)
        host.animateTranslationY(bead, moveDistance.toFloat(), animationDurationMs) { animatingBeads.remove(bead) }
    }

    private fun animateTopBeadUp(bead: AbacusBead) {
        animatingBeads.add(bead)
        host.animateTranslationY(bead, 0f, animationDurationMs) { animatingBeads.remove(bead) }
    }

    private fun animateBeadsUpWithDuration(durationMs: Long, vararg beads: AbacusBead) {
        val moveDistance = bottomMoveDistancePx.roundToInt()
        beads.forEach { animatingBeads.add(it) }
        beads.forEach { bead ->
            val startMargin = bead.bottomMargin
            val endMargin = startMargin + moveDistance
            host.animateTranslationY(bead, -moveDistance.toFloat(), durationMs) {
                bead.bottomMargin = endMargin
                bead.translationY = 0f
                animatingBeads.remove(bead)
            }
        }
    }

    private fun animateBeadsDownWithDuration(durationMs: Long, vararg beads: AbacusBead) {
        val moveDistance = bottomMoveDistancePx.roundToInt()
        beads.forEach { animatingBeads.add(it) }
        beads.forEach { bead ->
            val startMargin = bead.bottomMargin
            val endMargin = startMargin - moveDistance
            host.animateTranslationY(bead, moveDistance.toFloat(), durationMs) {
                bead.bottomMargin = endMargin
                bead.translationY = 0f
                animatingBeads.remove(bead)
            }
        }
    }

    /** DİKKAT (Android davranışı): sürüklemede hedef YUVARLANMAMIŞ mesafedir (tıklamadan farklı). */
    private fun animateTopBeadDownWithDuration(durationMs: Long, bead: AbacusBead) {
        animatingBeads.add(bead)
        host.animateTranslationY(bead, topMoveDistancePx, durationMs) { animatingBeads.remove(bead) }
    }

    private fun animateTopBeadUpWithDuration(durationMs: Long, bead: AbacusBead) {
        animatingBeads.add(bead)
        host.animateTranslationY(bead, 0f, durationMs) { animatingBeads.remove(bead) }
    }

    private fun updateAllAppearance() {
        for (rod in 0..4) {
            updateRodAppearance(rod)
            updateTopAppearance(rod)
        }
    }

    /** [updateAllAppearance] için açık sarmalayıcı (özelleştirme ekranı tip/renk değişince çağırır). */
    fun refreshAll() {
        updateAllAppearance()
    }

    private fun updateRodAppearance(rod: Int) {
        val count = bottomCount[rod]
        for (i in 1..4) {
            val selected = i <= count
            host.updateBeadAppearance(bottomBeads[rod][i - 1], selected)
        }
    }

    private fun updateTopAppearance(rod: Int) {
        host.updateBeadAppearance(topBeads[rod], topDown[rod])
    }

    private fun commitBottomTranslationToMargin(bead: AbacusBead) {
        // Önce iptal et, sonra o anki translation'ı margin'e işle.
        host.cancelAnimation(bead)
        animatingBeads.remove(bead)
        val dy = bead.translationY
        if (dy != 0f) {
            // translationY > 0: boncuk aşağı kaymış → margin azalır; < 0: yukarı → margin artar.
            bead.bottomMargin = (bead.bottomMargin - dy).roundToInt()
            bead.translationY = 0f
        }
    }

    /**
     * Animasyon ortasındaki alt boncukları durdurur, mevcut translation'ı margin'e yazar,
     * en yakın temiz konuma (tam yukarı veya tam aşağı) yapıştırır ve bottomCount'u günceller.
     */
    private fun interruptAndSnapBottomBeads(rod: Int) {
        val moveDistance = bottomMoveDistancePx.roundToInt()

        // 1. Animasyonları durdur ve translation → margin'e yaz
        for (i in 0..3) {
            val bead = bottomBeads[rod][i]
            if (bead in animatingBeads) {
                commitBottomTranslationToMargin(bead)
            }
        }

        // 2. Fiziksel konuma göre hangi boncukların yukarıda olduğunu bul
        val newCount = countConsecutiveRaisedBottomBeads(rod)

        // 3. Tüm boncukları en yakın temiz konuma yapıştır
        for (i in 0..3) {
            val bead = bottomBeads[rod][i]
            val shouldBeUp = i < newCount
            bead.bottomMargin = if (shouldBeUp) {
                initialBottomMargins[rod][i] + moveDistance
            } else {
                initialBottomMargins[rod][i]
            }
            bead.translationY = 0f
        }

        bottomCount[rod] = newCount; onValueChanged?.invoke()
        updateRodAppearance(rod)
    }

    /** Animasyon ortasındaki üst boncuğu durdurur ve en yakın temiz konuma yapıştırır. */
    private fun interruptAndSnapTopBead(rod: Int) {
        val bead = topBeads[rod]
        host.cancelAnimation(bead)
        animatingBeads.remove(bead)

        val halfStep = topMoveDistancePx / 2f
        if (bead.translationY >= halfStep) {
            bead.translationY = topMoveDistancePx
            topDown[rod] = true; onValueChanged?.invoke()
        } else {
            bead.translationY = 0f
            topDown[rod] = false; onValueChanged?.invoke()
        }
        updateTopAppearance(rod)
    }

    fun forceRecaptureInitialPositions() {
        initialPositionsCaptured = false
        captureInitialPositionsIfNeeded()
    }

    private fun captureInitialPositionsIfNeeded() {
        if (initialPositionsCaptured) return
        for (rod in 0..4) {
            for (i in 0..3) {
                initialBottomMargins[rod][i] = bottomBeads[rod][i].bottomMargin
            }
        }
        initialPositionsCaptured = true
    }
}

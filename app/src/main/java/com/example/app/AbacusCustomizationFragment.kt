package com.example.app

import android.app.Dialog
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.app.abacus.AbacusBeadController
import com.example.app.abacus.AbacusBeadRenderer
import com.example.app.abacus.AbacusFrameRenderer
import com.example.app.abacus.AbacusPreferences
import com.example.app.abacus.AbacusPreferences.BeadType
import com.example.app.abacus.AbacusPreferences.FrameType

import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment that lets the user customise:
 *  Tab 1 — bead type (soroban / rabbit)
 *  Tab 2 — per-path colour via Hue + Brightness seekbars (with gradient backgrounds)
 *  Tab 3 — frame/container background type
 *  Tab 4 — frame colour(s)
 *
 * Rabbit rendering runs on [Dispatchers.Default] with 300 ms debounce so the
 * main thread stays responsive while the user drags seekbars.
 */
class AbacusCustomizationFragment : Fragment() {
    private var isDialogShowing = false


    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var previewContainer: FrameLayout
    private lateinit var tabStrip: LinearLayout
    private lateinit var tabIndicator: View
    private lateinit var tabContent: FrameLayout

    private val tabViews = arrayOfNulls<ImageView>(4)

    private var currencyText: TextView? = null
    private var keyText: TextView? = null
    private var previewController: AbacusBeadController? = null
    private var previewAbacusRoot: View? = null

    private var currentTab = 0

    // Debounce job for async rabbit rendering
    private var renderJob: Job? = null

    // Debounce job for pushing the local customization to Firestore
    private var syncJob: Job? = null

    // ── Bead ownership ────────────────────────────────────────────────────────
    /** Bead IDs the current user owns (loaded from Firestore on fragment creation). */
    private var ownedBeads: Map<String, BeadData> = emptyMap()

    // ── Frame ownership ───────────────────────────────────────────────────────
    /** Frame IDs the current user owns (e.g. "FRAME_BG2", "FRAME_BG3", "FRAME_BG4"). */
    private var ownedFrames: Map<String, FrameData> = emptyMap()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_abacus_customization, container, false)

        previewContainer = v.findViewById(R.id.previewContainer)
        tabStrip = v.findViewById(R.id.tabStrip)
        tabIndicator = v.findViewById(R.id.tabIndicator)
        tabContent = v.findViewById(R.id.tabContent)

        for (i in 0..3) {
            val id = when (i) { 0 -> R.id.tab1; 1 -> R.id.tab2; 2 -> R.id.tab3; else -> R.id.tab4 }
            tabViews[i] = v.findViewById(id)
            val idx = i
            tabViews[i]?.setOnClickListener { selectTab(idx) }
        }
        v.findViewById<ImageButton>(R.id.customizationBackButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        inflatePreviewAbacus()

        currencyText = v.findViewById(R.id.currencyText)
        keyText = v.findViewById(R.id.shopKeyText)
        
        val openShop = View.OnClickListener {
            (requireActivity() as? MainActivity)?.openShopFragment()
        }
        v.findViewById<View>(R.id.currencyContainer)?.setOnClickListener(openShop)
        v.findViewById<View>(R.id.shopKeyContainer)?.setOnClickListener(openShop)
        refreshCurrencyUi()

        // Load owned beads and wallet from Firestore, then refresh tab1 if visible
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            UserWalletFirestore.loadWallet(requireContext(), uid, onResult = { wallet ->
                currencyText?.text = wallet.currency.toString()
                keyText?.text = wallet.keys.toString()
            })
            BeadPurchaseFirestore.loadOwnedBeads(uid, onResult = { owned ->
                ownedBeads = owned
                if (currentTab == 0) selectTab(0)
            })
            FramePurchaseFirestore.loadOwnedFrames(uid, onResult = { owned ->
                ownedFrames = owned
                if (currentTab == 2) selectTab(2)
                if (currentTab == 3) selectTab(3)
            })
            // Başka bir cihazda kaydedilmiş özelleştirme varsa (ve bu cihaz henüz hydrate
            // edilmediyse) lokale uygula, sonra önizlemeyi/aktif sekmeyi yenile.
            val ctx = requireContext().applicationContext
            AbacusCustomizationFirestore.ensureHydrated(requireContext(), uid) {
                if (!isAdded) return@ensureHydrated
                // Pahalı (bitmap tabanlı) boncuk tiplerini ana thread'i kilitlemeden önce
                // arka planda ısıt — bkz. AbacusBeadRenderer.prewarmAllSlotDrawables.
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.Default) {
                        AbacusBeadRenderer.prewarmAllSlotDrawables(ctx)
                    }
                    if (!isAdded) return@launch
                    refreshPreviewFrameBackground()
                    previewController?.refreshAll()
                    selectTab(currentTab)
                }
            }
        }

        selectTab(0)

        return v
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderJob?.cancel()
        syncJob?.cancel()
        previewController = null
        previewAbacusRoot = null
    }

    /** Değişiklikleri debounce'lu şekilde Firestore'a yazar (users/{uid}/abacusCustomization/state). */
    private fun scheduleFirestoreSync() {
        val ctx = requireContext().applicationContext
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        syncJob?.cancel()
        syncJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(1000)
            val snapshot = withContext(Dispatchers.Default) {
                AbacusCustomizationFirestore.buildSnapshotFromLocalPreferences(ctx)
            }
            AbacusCustomizationFirestore.saveCustomization(uid, snapshot)
        }
    }

    // ── Preview Abacus ────────────────────────────────────────────────────────

    private fun inflatePreviewAbacus() {
        val preview = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_customization_preview, previewContainer, false)
        previewContainer.removeAllViews()
        previewContainer.addView(preview)
        previewAbacusRoot = preview

        applyPreviewFrameBackground(preview.findViewById(R.id.abacusContainer))

        previewController = AbacusBeadController(requireContext(), preview, 150L).also { it.setup() }
        preview.post { previewController?.computeMovementDistancesFromLayout(ratio = 1.0f, force = true) }
        scheduleBeadRefresh(debounce = false)
    }

    private fun applyPreviewFrameBackground(target: View) {
        target.background = AbacusFrameRenderer.buildFrameDrawable(requireContext())
    }

    private fun refreshPreviewFrameBackground() {
        val root = previewAbacusRoot ?: return
        applyPreviewFrameBackground(root.findViewById(R.id.abacusContainer))
    }

    /**
     * Refreshes bead drawables in the preview.
     * For RABBIT: runs rendering on Default dispatcher to avoid blocking the main thread.
     * [debounce] = true adds a 300 ms delay (used while seekbar is being dragged).
     */
    private fun scheduleBeadRefresh(debounce: Boolean = true) {
        val ctx = requireContext().applicationContext
        renderJob?.cancel()
        renderJob = viewLifecycleOwner.lifecycleScope.launch {
            if (debounce) delay(300)
            // Build drawables off main thread
            withContext(Dispatchers.Default) {
                AbacusBeadRenderer.buildCurrentBead(ctx, false)  // warm-up cache (soroban is fast)
            }
            // Refresh the controller on main thread
            if (isAdded) previewController?.refreshAll()
        }
    }

    // ── Tab management ────────────────────────────────────────────────────────

    private fun selectTab(index: Int) {
        currentTab = index
        for (i in 0..3) {
            tabViews[i]?.clearColorFilter()
            tabViews[i]?.alpha = if (i == index) 1.0f else 0.4f
        }
        tabStrip.post {
            val tabW = tabStrip.width / 4
            val lp = tabIndicator.layoutParams as? ViewGroup.MarginLayoutParams ?: return@post
            lp.marginStart = tabW * index
            lp.width = tabW
            tabIndicator.layoutParams = lp
        }
        tabContent.removeAllViews()
        tabContent.addView(when (index) {
            0 -> inflateTab1()
            1 -> inflateTab2()
            2 -> inflateTab3()
            else -> inflateTab4()
        })
    }

    // ── Tab 1: Bead type ──────────────────────────────────────────────────────

    /**
     * Maps each paid card to its (BeadType, image drawable res-id) pair.
     * cardBeadSoroban is intentionally absent — it is always free.
     */
    private val PAID_BEAD_INFO: List<Triple<Int, BeadType, Int>> by lazy {
        // Triple<cardResId, BeadType, imageDrawableResId>
        listOf(
            Triple(R.id.cardBeadSoroban2,  BeadType.SOROBAN2,  R.drawable.soroban_bead2),
            Triple(R.id.cardBeadSoroban6,  BeadType.SOROBAN6,  R.drawable.soroban_bead6),
            Triple(R.id.cardBeadBowling,   BeadType.BOWLING,   R.drawable.bowling_ball_ic),
            Triple(R.id.cardBeadBall1,     BeadType.BALL1,     R.drawable.abacus_bead_ball1),
            Triple(R.id.cardBeadBall3,     BeadType.BALL3,     R.drawable.abacus_bead_ball3),
            Triple(R.id.cardBeadBall4,     BeadType.BALL4,     R.drawable.abacus_bead_ball4),
            Triple(R.id.cardBeadBall5,     BeadType.BALL5,     R.drawable.abacus_bead_ball5),
            Triple(R.id.cardBeadBall6,     BeadType.BALL6,     R.drawable.abacus_bead_ball6),
            Triple(R.id.cardBeadBall7,     BeadType.BALL7,     R.drawable.abacus_bead_ball7),
            Triple(R.id.cardBeadBall8,     BeadType.BALL8,     R.drawable.abacus_bead_ball8),
            Triple(R.id.cardBeadBall9,     BeadType.BALL9,     R.drawable.abacus_bead_ball9),
            Triple(R.id.cardBeadBall10,    BeadType.BALL10,    R.drawable.abacus_bead_ball10),
            Triple(R.id.cardBeadBall11,    BeadType.BALL11,    R.drawable.abacus_bead_ball11),
            Triple(R.id.cardBeadBall12,    BeadType.BALL12,    R.drawable.abacus_bead_ball12),
            Triple(R.id.cardBeadBall13,    BeadType.BALL13,    R.drawable.abacus_bead_ball13),
            Triple(R.id.cardBeadBall14,    BeadType.BALL14,    R.drawable.abacus_bead_ball14),
            Triple(R.id.cardBeadAnimal,    BeadType.ANIMAL,    R.drawable.animal_head_ic1),
            Triple(R.id.cardBeadAnimal2,   BeadType.ANIMAL2,   R.drawable.animal_head_ic2),
            Triple(R.id.cardBeadAnimal3,   BeadType.ANIMAL3,   R.drawable.animal_head_ic3),
            Triple(R.id.cardBeadAnimal4,   BeadType.ANIMAL4,   R.drawable.animal_head_ic4),
            Triple(R.id.cardBeadAnimal5,   BeadType.ANIMAL5,   R.drawable.animal_head_ic5),
            Triple(R.id.cardBeadAnimal6,   BeadType.ANIMAL6,   R.drawable.animal_head_ic6),
            Triple(R.id.cardBeadAnimal7,   BeadType.ANIMAL7,   R.drawable.animal_head_ic7),
            Triple(R.id.cardBeadAnimal8,   BeadType.ANIMAL8,   R.drawable.animal_head_ic8),
            Triple(R.id.cardBeadAnimal9,   BeadType.ANIMAL9,   R.drawable.animal_head_ic9),
        )
    }

    private fun inflateTab1(): View {
        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_tab1_bead_options, tabContent, false)

        val ctx = requireContext()

        // ── Free card (always unlocked) ───────────────────────────────────────
        val cardSoroban = v.findViewById<MaterialCardView>(R.id.cardBeadSoroban)

        fun updateSelection() {
            val type = AbacusPreferences.getBeadType(ctx)
            val sel = Color.parseColor("#FFFFD600")
            val selStateList = android.content.res.ColorStateList.valueOf(sel)
            // Soroban
            if (type == BeadType.SOROBAN) {
                cardSoroban.setStrokeColor(selStateList)
                cardSoroban.strokeWidth = 6
            } else {
                cardSoroban.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT))
                cardSoroban.strokeWidth = 0
            }
            cardSoroban.invalidate()
            // All paid beads
            for ((cardId, beadType, _) in PAID_BEAD_INFO) {
                val card = v.findViewById<MaterialCardView>(cardId)
                if (type == beadType) {
                    card.setStrokeColor(selStateList)
                    card.strokeWidth = 6
                } else {
                    card.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT))
                    card.strokeWidth = 0
                }
                card.invalidate()
            }
        }
        updateSelection()

        // Free card click
        cardSoroban.setOnClickListener {
            AbacusPreferences.setBeadType(ctx, BeadType.SOROBAN)
            updateSelection()
            scheduleFirestoreSync()
            showBeadSlotSelectPanel(BeadType.SOROBAN)
        }

        // ── Paid cards ────────────────────────────────────────────────────────
        val dp = ctx.resources.displayMetrics.density

        for ((cardId, beadType, imageResId) in PAID_BEAD_INFO) {
            val card = v.findViewById<MaterialCardView>(cardId)
            val owned = ((ownedBeads[beadType.name]?.count ?: 0) > 0)
            val price = getBeadPrice(beadType)

            // Build lock overlay
            val overlay = FrameLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(0xBB000000.toInt())   // dark semi-transparent
                visibility = if (owned) View.GONE else View.VISIBLE
                // Ensure overlay sits on top; card is already a FrameLayout internally
            }

            // Price label inside the overlay
            val container = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val lockLabel = TextView(ctx).apply {
                text = price.toString()
                textSize = 13f
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val lockIcon = ImageView(ctx).apply {
                val isKeyCurrency = (beadType == BeadType.ANIMAL3 || beadType == BeadType.ANIMAL8)
                setImageResource(if (isKeyCurrency) R.drawable.key else R.drawable.gold_ic)
                val size = (20 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = (4 * resources.displayMetrics.density).toInt()
                }
            }
            container.addView(lockLabel)
            container.addView(lockIcon)
            overlay.addView(container)

            // Add overlay into the card's internal content view
            // MaterialCardView wraps its children in a FrameLayout
            (card as? ViewGroup)?.addView(overlay)

            // Click logic
            card.setOnClickListener {
                AbacusPreferences.setBeadType(ctx, beadType)
                updateSelection()
                scheduleFirestoreSync()

                if (((ownedBeads[beadType.name]?.count ?: 0) > 0)) {
                    // Already owned → show slot selection panel directly
                    showBeadSlotSelectPanel(beadType)
                } else {
                    // Not owned → show purchase panel
                    showBeadPurchasePanel(beadType, imageResId) {
                        // onPurchased callback: hide overlay, then show slot selection panel
                        overlay.visibility = View.GONE
                        showBeadSlotSelectPanel(beadType)
                    }
                }
            }
        }

        return v
    }

    /**
     * Shows the bead purchase Dialog.
     * [onPurchased] is called on the main thread after a successful purchase.
     */
    private fun showBeadPurchasePanel(
        beadType: BeadType,
        imageResId: Int,
        onPurchased: () -> Unit,
    ) {
        if (isDialogShowing) return
        isDialogShowing = true
        
        val ctx = requireContext()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.panel_bead_purchase)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val width = (ctx.resources.displayMetrics.widthPixels * 0.88f).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCanceledOnTouchOutside(true)

        // Close
        dialog.findViewById<View>(R.id.beadPurchaseClose).setOnClickListener { dialog.dismiss() }

        // Bead image
        dialog.findViewById<ImageView>(R.id.beadPurchaseImage)
            .setImageResource(imageResId)

        val price = getBeadPrice(beadType)
        val isKeyCurrency = (beadType == BeadType.ANIMAL3 || beadType == BeadType.ANIMAL8)
        dialog.findViewById<TextView>(R.id.beadPurchasePriceText)?.text = price.toString()
        if (isKeyCurrency) {
            dialog.findViewById<ImageView>(R.id.beadPurchaseCurrencyIcon)?.setImageResource(R.drawable.key)
        }

        // Buy button
        val purchaseBtn = dialog.findViewById<View>(R.id.beadPurchaseButton)
        val closeBtn = dialog.findViewById<View>(R.id.beadPurchaseClose)

        purchaseBtn.setOnClickListener {
            if (!purchaseBtn.isEnabled) return@setOnClickListener
            purchaseBtn.isEnabled = false
            closeBtn.isEnabled = false
            dialog.setCancelable(false)

            val hasEnough = if (isKeyCurrency) UserWalletFirestore.getCachedKeys(ctx) >= price
                            else UserWalletFirestore.getCachedCurrency(ctx) >= price
            if (!hasEnough) {
                Toast.makeText(ctx, if (isKeyCurrency) "Yetersiz anahtar" else "Yetersiz altın", Toast.LENGTH_SHORT).show()
                purchaseBtn.isEnabled = true
                closeBtn.isEnabled = true
                dialog.setCancelable(true)
                return@setOnClickListener
            }

            fun onDeltaSuccess(rollbackToken: String?) {
                val currentCount = (ownedBeads[beadType.name]?.count ?: 0)
                val newCount = currentCount + 5
                BeadPurchaseFirestore.setBeadCount(
                    uid = uid,
                    beadId = beadType.name,
                    count = newCount,
                    onSuccess = {
                        ownedBeads = ownedBeads + (beadType.name to (ownedBeads[beadType.name]?.copy(count = newCount) ?: BeadData(newCount, false)))
                        refreshCurrencyUi()
                        isDialogShowing = false
                        dialog.dismiss()
                        onPurchased()
                    },
                    onFailure = {
                        Toast.makeText(ctx, "Kayıt hatası. Tekrar deneyin.", Toast.LENGTH_SHORT).show()
                        // Geri iade
                        if (isKeyCurrency) UserWalletFirestore.applyKeyDelta(ctx, uid, +price, WalletReason.PURCHASE_ROLLBACK, rollbackToken)
                        else UserWalletFirestore.applyCurrencyDelta(ctx, uid, +price, WalletReason.PURCHASE_ROLLBACK, rollbackToken)
                        refreshCurrencyUi()
                        purchaseBtn.isEnabled = true
                        closeBtn.isEnabled = true
                        dialog.setCancelable(true)
                    }
                )
            }

            fun onDeltaFailure() {
                Toast.makeText(ctx, "İşlem başarısız. Tekrar deneyin.", Toast.LENGTH_SHORT).show()
                purchaseBtn.isEnabled = true
                closeBtn.isEnabled = true
                dialog.setCancelable(true)
            }

            if (isKeyCurrency) {
                UserWalletFirestore.applyKeyDelta(
                    context = ctx, uid = uid, delta = -price, reason = WalletReason.SPEND,
                    onSuccess = { wallet -> onDeltaSuccess(wallet.rollbackToken) },
                    onFailure = { ::onDeltaFailure.invoke() }
                )
            } else {
                UserWalletFirestore.applyCurrencyDelta(
                    context = ctx, uid = uid, delta = -price, reason = WalletReason.SPEND,
                    onSuccess = { wallet -> onDeltaSuccess(wallet.rollbackToken) },
                    onFailure = { ::onDeltaFailure.invoke() }
                )
            }
        }

        dialog.setOnDismissListener { isDialogShowing = false }
        dialog.show()
    }

    /**
     * After a successful bead purchase, shows a panel with a full abacus.
     * The user taps a specific bead to assign the new [beadType] skin to that slot.
     */
    private fun showColorFeaturePurchasePanel(
        beadType: BeadType,
        imageResId: Int
    ) {
        if (isDialogShowing) return
        isDialogShowing = true
        
        val ctx = requireContext()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.panel_bead_purchase)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val width = (ctx.resources.displayMetrics.widthPixels * 0.88f).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCanceledOnTouchOutside(true)

        // Close
        val closeBtn = dialog.findViewById<View>(R.id.beadPurchaseClose)
        closeBtn.setOnClickListener { dialog.dismiss() }

        // Bead image
        dialog.findViewById<ImageView>(R.id.beadPurchaseImage)
            .setImageResource(imageResId)
            
        dialog.findViewById<ImageView>(R.id.beadPurchaseFeatureIcon)
            ?.visibility = View.VISIBLE

        val price = 2000 // Color feature is always 2000
        dialog.findViewById<TextView>(R.id.beadPurchasePriceText)?.text = price.toString()

        // Buy button
        val purchaseBtn = dialog.findViewById<View>(R.id.beadPurchaseButton)
        
        purchaseBtn.setOnClickListener {
            // Disable immediately to prevent double-tap
            if (!purchaseBtn.isEnabled) return@setOnClickListener
            purchaseBtn.isEnabled = false
            closeBtn.isEnabled = false
            dialog.setCancelable(false)

            val currentCurrency = UserWalletFirestore.getCachedCurrency(ctx)
            if (currentCurrency < price) {
                Toast.makeText(ctx, "Yetersiz altın", Toast.LENGTH_SHORT).show()
                purchaseBtn.isEnabled = true
                closeBtn.isEnabled = true
                dialog.setCancelable(true)
                return@setOnClickListener
            }
            
            // Deduct currency
            UserWalletFirestore.applyCurrencyDelta(
                context = ctx,
                uid = uid,
                delta = -price,
                reason = WalletReason.SPEND,
                onSuccess = {
                    BeadPurchaseFirestore.setColorFeatureActive(uid, beadType.name, onSuccess = {
                        val currentData = ownedBeads[beadType.name] ?: BeadData(0, false)
                        ownedBeads = ownedBeads + (beadType.name to currentData.copy(colorFeatureActive = true))
                        refreshCurrencyUi()
                        
                        dialog.dismiss()
                        
                        // Refresh tab 2 to hide the overlay
                        if (currentTab == 1) selectTab(1)
                    }, onFailure = {
                        Toast.makeText(ctx, "Hata oluştu", Toast.LENGTH_SHORT).show()
                        purchaseBtn.isEnabled = true
                        closeBtn.isEnabled = true
                        dialog.setCancelable(true)
                    })
                },
                onFailure = {
                    Toast.makeText(ctx, "Hata oluştu", Toast.LENGTH_SHORT).show()
                    purchaseBtn.isEnabled = true
                    closeBtn.isEnabled = true
                    dialog.setCancelable(true)
                }
            )
        }

        dialog.setOnDismissListener { isDialogShowing = false }
        dialog.show()
    }

    private fun showBeadSlotSelectPanel(beadType: BeadType) {
        if (isDialogShowing) return
        isDialogShowing = true
        
        val ctx = requireContext()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.panel_bead_skin_select)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val width = (ctx.resources.displayMetrics.widthPixels * 0.92f).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCanceledOnTouchOutside(true)

        dialog.findViewById<View>(R.id.beadSlotSelectClose).setOnClickListener { dialog.dismiss() }

        // Build bead IDs lookup: rod -> (topId, [bottom1..4 ids])
        data class RodBeadIds(val top: Int, val bottoms: List<Int>)
        val rodIds = listOf(
            RodBeadIds(R.id.bss_rod0_bead_top, listOf(R.id.bss_rod0_bead_bottom1, R.id.bss_rod0_bead_bottom2, R.id.bss_rod0_bead_bottom3, R.id.bss_rod0_bead_bottom4)),
            RodBeadIds(R.id.bss_rod1_bead_top, listOf(R.id.bss_rod1_bead_bottom1, R.id.bss_rod1_bead_bottom2, R.id.bss_rod1_bead_bottom3, R.id.bss_rod1_bead_bottom4)),
            RodBeadIds(R.id.bss_rod2_bead_top, listOf(R.id.bss_rod2_bead_bottom1, R.id.bss_rod2_bead_bottom2, R.id.bss_rod2_bead_bottom3, R.id.bss_rod2_bead_bottom4)),
            RodBeadIds(R.id.bss_rod3_bead_top, listOf(R.id.bss_rod3_bead_bottom1, R.id.bss_rod3_bead_bottom2, R.id.bss_rod3_bead_bottom3, R.id.bss_rod3_bead_bottom4)),
            RodBeadIds(R.id.bss_rod4_bead_top, listOf(R.id.bss_rod4_bead_bottom1, R.id.bss_rod4_bead_bottom2, R.id.bss_rod4_bead_bottom3, R.id.bss_rod4_bead_bottom4)),
        )

        fun refreshUI() {
            // Render current per-bead skins in the panel's abacus
            for ((rod, ids) in rodIds.withIndex()) {
                val topView = dialog.findViewById<ImageView>(ids.top)
                val topType = AbacusPreferences.getBeadTypeForSlot(ctx, rod, isTop = true)
                topView.setImageDrawable(AbacusBeadRenderer.buildBeadForType(ctx, topType, false))

                for ((beadIndex, bottomId) in ids.bottoms.withIndex()) {
                    val bottomView = dialog.findViewById<ImageView>(bottomId)
                    val bottomType = AbacusPreferences.getBeadTypeForSlot(ctx, rod, isTop = false, beadIndex = beadIndex)
                    bottomView.setImageDrawable(AbacusBeadRenderer.buildBeadForType(ctx, bottomType, false))
                }
            }

            // Calculate total owned and placed beads
            val count = if (beadType == BeadType.SOROBAN) 25 else ((ownedBeads[beadType.name]?.count ?: 0))
            var placedCount = 0
            for (r in 0..4) {
                if (AbacusPreferences.getBeadTypeForSlot(ctx, r, true) == beadType) placedCount++
                for (i in 0..3) {
                    if (AbacusPreferences.getBeadTypeForSlot(ctx, r, false, i) == beadType) placedCount++
                }
            }
            
            // Available is total minus placed
            val available = count - placedCount

            // Update inventory status UI
            dialog.findViewById<TextView>(R.id.beadSlotInventoryText).text = "x$available"
            dialog.findViewById<ImageView>(R.id.beadSlotInventoryImage)
                .setImageDrawable(AbacusBeadRenderer.buildBeadForType(ctx, beadType, false))

            // Update buy button
            val price = getBeadPrice(beadType)
            val buyBtn = dialog.findViewById<MaterialCardView>(R.id.beadSlotBuyButton)
            val buyText = dialog.findViewById<TextView>(R.id.beadSlotBuyText)
            buyText.text = price.toString() // Always show price
            if (count >= 25) { // Total count is max 25
                buyBtn.setCardBackgroundColor(Color.parseColor("#BDBDBD")) // gray out
            } else {
                buyBtn.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.button_enabled))
            }
        }

        // Buy button logic
        val isKeyCurrency = (beadType == BeadType.ANIMAL3 || beadType == BeadType.ANIMAL8)
        if (isKeyCurrency) {
            dialog.findViewById<ImageView>(R.id.beadSlotBuyIcon)?.setImageResource(R.drawable.key)
        }

        val slotBuyBtn = dialog.findViewById<MaterialCardView>(R.id.beadSlotBuyButton)
        slotBuyBtn.setOnClickListener {
            if (!slotBuyBtn.isEnabled) return@setOnClickListener
            slotBuyBtn.isEnabled = false
            dialog.setCancelable(false)

            val price = getBeadPrice(beadType)
            val count = if (beadType == BeadType.SOROBAN) 25 else (ownedBeads[beadType.name]?.count ?: 0)
            if (count >= 25) {
                Toast.makeText(ctx, "Stokta kalmadı", Toast.LENGTH_SHORT).show()
                slotBuyBtn.isEnabled = true
                dialog.setCancelable(true)
                return@setOnClickListener
            }

            val hasEnough = if (isKeyCurrency) UserWalletFirestore.getCachedKeys(ctx) >= price
                            else UserWalletFirestore.getCachedCurrency(ctx) >= price
            if (!hasEnough) {
                Toast.makeText(ctx, if (isKeyCurrency) "Yetersiz anahtar" else "Yetersiz altın", Toast.LENGTH_SHORT).show()
                slotBuyBtn.isEnabled = true
                dialog.setCancelable(true)
                return@setOnClickListener
            }

            fun onDeltaSuccess(rollbackToken: String?) {
                val newCount = count + 5
                BeadPurchaseFirestore.setBeadCount(uid, beadType.name, newCount,
                    onSuccess = {
                        ownedBeads = ownedBeads + (beadType.name to (ownedBeads[beadType.name]?.copy(count = newCount) ?: BeadData(newCount, false)))
                        refreshCurrencyUi()
                        refreshUI()
                        slotBuyBtn.isEnabled = true
                        dialog.setCancelable(true)
                    },
                    onFailure = {
                        Toast.makeText(ctx, "Kayıt hatası", Toast.LENGTH_SHORT).show()
                        if (isKeyCurrency) UserWalletFirestore.applyKeyDelta(ctx, uid, +price, WalletReason.PURCHASE_ROLLBACK, rollbackToken)
                        else UserWalletFirestore.applyCurrencyDelta(ctx, uid, +price, WalletReason.PURCHASE_ROLLBACK, rollbackToken)
                        refreshCurrencyUi()
                        slotBuyBtn.isEnabled = true
                        dialog.setCancelable(true)
                    }
                )
            }

            fun onDeltaFailure() {
                Toast.makeText(ctx, "İşlem başarısız", Toast.LENGTH_SHORT).show()
                slotBuyBtn.isEnabled = true
                dialog.setCancelable(true)
            }

            if (isKeyCurrency) {
                UserWalletFirestore.applyKeyDelta(
                    context = ctx, uid = uid, delta = -price, reason = WalletReason.SPEND,
                    onSuccess = { wallet -> onDeltaSuccess(wallet.rollbackToken) },
                    onFailure = { ::onDeltaFailure.invoke() }
                )
            } else {
                UserWalletFirestore.applyCurrencyDelta(
                    context = ctx, uid = uid, delta = -price, reason = WalletReason.SPEND,
                    onSuccess = { wallet -> onDeltaSuccess(wallet.rollbackToken) },
                    onFailure = { ::onDeltaFailure.invoke() }
                )
            }
        }

        fun placeBead(rod: Int, isTop: Boolean, beadIndex: Int = 0) {
            val oldType = AbacusPreferences.getBeadTypeForSlot(ctx, rod, isTop, beadIndex)
            // Ignore if placing same bead type
            if (oldType == beadType) return

            val count = if (beadType == BeadType.SOROBAN) 25 else ((ownedBeads[beadType.name]?.count ?: 0))
            var placedCount = 0
            for (r in 0..4) {
                if (AbacusPreferences.getBeadTypeForSlot(ctx, r, true) == beadType) placedCount++
                for (i in 0..3) {
                    if (AbacusPreferences.getBeadTypeForSlot(ctx, r, false, i) == beadType) placedCount++
                }
            }
            val available = count - placedCount
            
            if (available <= 0) {
                Toast.makeText(ctx, "Bu boncuktan kalmadı!", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Just save placement locally. Firestore count remains the total purchased.
            AbacusPreferences.setBeadTypeForSlot(ctx, rod, isTop, beadIndex, beadType)
            refreshUI()
            scheduleBeadRefresh(debounce = false)
            scheduleFirestoreSync()
        }

        // Click listeners: assign skin to tapped bead slot
        for ((rod, ids) in rodIds.withIndex()) {
            dialog.findViewById<ImageView>(ids.top).setOnClickListener {
                placeBead(rod, isTop = true)
            }
            for ((beadIndex, bottomId) in ids.bottoms.withIndex()) {
                dialog.findViewById<ImageView>(bottomId).setOnClickListener {
                    placeBead(rod, isTop = false, beadIndex = beadIndex)
                }
            }
        }

        refreshUI()
        dialog.setOnDismissListener { isDialogShowing = false }
        dialog.show()
    }

    // ── Tab 2: Bead colour ────────────────────────────────────────────────────

    private fun inflateTab2(): View {
        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_tab2_bead_color, tabContent, false)

        val previewNormal = v.findViewById<ImageView>(R.id.previewBeadNormal)
        val previewSelected = v.findViewById<ImageView>(R.id.previewBeadSelected)
        val columnNormal = v.findViewById<LinearLayout>(R.id.columnNormal)
        val columnSelected = v.findViewById<LinearLayout>(R.id.columnSelected)
        val ctx = requireContext()
        val beadType = AbacusPreferences.getBeadType(ctx)

        fun refreshHeaders() {
            previewNormal.setImageDrawable(AbacusBeadRenderer.buildCurrentBead(ctx, false))
            previewSelected.setImageDrawable(AbacusBeadRenderer.buildCurrentBead(ctx, true))
        }
        refreshHeaders()

        val labels: Array<String>
        val pathCount: Int
        when (beadType) {
            BeadType.SOROBAN -> { labels = AbacusPreferences.SOROBAN_PATH_LABELS; pathCount = 3 }
            BeadType.ANIMAL  -> { labels = AbacusPreferences.ANIMAL_COLOR_LABELS; pathCount = 4 }
            BeadType.ANIMAL2 -> { labels = AbacusPreferences.ANIMAL2_COLOR_LABELS; pathCount = 5 }
            BeadType.ANIMAL3 -> { labels = AbacusPreferences.ANIMAL3_COLOR_LABELS; pathCount = 5 }
            BeadType.ANIMAL4 -> { labels = AbacusPreferences.ANIMAL4_COLOR_LABELS; pathCount = 4 }
            BeadType.ANIMAL5 -> { labels = AbacusPreferences.ANIMAL5_COLOR_LABELS; pathCount = 6 }
            BeadType.ANIMAL6 -> { labels = AbacusPreferences.ANIMAL6_COLOR_LABELS; pathCount = 3 }
            BeadType.ANIMAL7 -> { labels = AbacusPreferences.ANIMAL7_COLOR_LABELS; pathCount = 5 }
            BeadType.ANIMAL8 -> { labels = AbacusPreferences.ANIMAL8_COLOR_LABELS; pathCount = 5 }
            BeadType.ANIMAL9 -> { labels = AbacusPreferences.ANIMAL9_COLOR_LABELS; pathCount = 4 }
            BeadType.BOWLING -> { labels = AbacusPreferences.BOWLING_COLOR_LABELS; pathCount = 5 }
            BeadType.BALL1 -> { labels = AbacusPreferences.BALL1_COLOR_LABELS; pathCount = 3 }
            BeadType.BALL3 -> { labels = AbacusPreferences.BALL3_COLOR_LABELS; pathCount = 4 }
            BeadType.BALL4 -> { labels = AbacusPreferences.BALL4_COLOR_LABELS; pathCount = 4 }
            BeadType.BALL5 -> { labels = AbacusPreferences.BALL5_COLOR_LABELS; pathCount = 4 }
            BeadType.BALL6 -> { labels = AbacusPreferences.BALL6_COLOR_LABELS; pathCount = 4 }
            BeadType.BALL7 -> { labels = AbacusPreferences.BALL7_COLOR_LABELS; pathCount = 3 }
            BeadType.BALL8 -> { labels = AbacusPreferences.BALL8_COLOR_LABELS; pathCount = 4 }
            BeadType.BALL9 -> { labels = AbacusPreferences.BALL9_COLOR_LABELS; pathCount = 5 }
            BeadType.BALL10 -> { labels = AbacusPreferences.BALL10_COLOR_LABELS; pathCount = 5 }
            BeadType.BALL11 -> { labels = AbacusPreferences.BALL11_COLOR_LABELS; pathCount = 4 }
            BeadType.BALL12 -> { labels = AbacusPreferences.BALL12_COLOR_LABELS; pathCount = 5 }
            BeadType.BALL13 -> { labels = AbacusPreferences.BALL13_COLOR_LABELS; pathCount = 4 }
            BeadType.BALL14 -> { labels = AbacusPreferences.BALL14_COLOR_LABELS; pathCount = 3 }
            BeadType.SOROBAN2 -> { labels = AbacusPreferences.SOROBAN2_COLOR_LABELS; pathCount = 5 }
            BeadType.SOROBAN6 -> { labels = AbacusPreferences.SOROBAN6_COLOR_LABELS; pathCount = 3 }
        }
        /**
         * Builds a column of H+V dual-seekbar rows for one bead state (normal or selected).
         * Each seekbar row:
         *   - Hue seekbar:        rainbow gradient background, progress 0–359
         *   - Brightness seekbar: black→current-hue gradient,  progress 0–100
         * Changes are saved immediately; rabbit rendering is debounced 300 ms.
         */
        fun buildColorRows(parent: LinearLayout, isSelected: Boolean) {
            parent.removeAllViews()
            val currentColors = when (beadType) {
                BeadType.SOROBAN -> AbacusPreferences.getSorobanColors(ctx, isSelected).copyOf()
                BeadType.ANIMAL  -> AbacusPreferences.getAnimalColors(ctx, isSelected).copyOf()
                BeadType.ANIMAL2 -> AbacusPreferences.getAnimal2Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL3 -> AbacusPreferences.getAnimal3Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL4 -> AbacusPreferences.getAnimal4Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL5 -> AbacusPreferences.getAnimal5Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL6 -> AbacusPreferences.getAnimal6Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL7 -> AbacusPreferences.getAnimal7Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL8 -> AbacusPreferences.getAnimal8Colors(ctx, isSelected).copyOf()
                BeadType.ANIMAL9 -> AbacusPreferences.getAnimal9Colors(ctx, isSelected).copyOf()
                BeadType.BOWLING -> AbacusPreferences.getBowlingColors(ctx, isSelected).copyOf()
                BeadType.BALL1 -> AbacusPreferences.getBall1Colors(ctx, isSelected).copyOf()
                BeadType.BALL3 -> AbacusPreferences.getBall3Colors(ctx, isSelected).copyOf()
                BeadType.BALL4 -> AbacusPreferences.getBall4Colors(ctx, isSelected).copyOf()
                BeadType.BALL5 -> AbacusPreferences.getBall5Colors(ctx, isSelected).copyOf()
                BeadType.BALL6 -> AbacusPreferences.getBall6Colors(ctx, isSelected).copyOf()
                BeadType.BALL7 -> AbacusPreferences.getBall7Colors(ctx, isSelected).copyOf()
                BeadType.BALL8 -> AbacusPreferences.getBall8Colors(ctx, isSelected).copyOf()
                BeadType.BALL9 -> AbacusPreferences.getBall9Colors(ctx, isSelected).copyOf()
                BeadType.BALL10 -> AbacusPreferences.getBall10Colors(ctx, isSelected).copyOf()
                BeadType.BALL11 -> AbacusPreferences.getBall11Colors(ctx, isSelected).copyOf()
                BeadType.BALL12 -> AbacusPreferences.getBall12Colors(ctx, isSelected).copyOf()
                BeadType.BALL13 -> AbacusPreferences.getBall13Colors(ctx, isSelected).copyOf()
                BeadType.BALL14 -> AbacusPreferences.getBall14Colors(ctx, isSelected).copyOf()
                BeadType.SOROBAN2 -> AbacusPreferences.getSoroban2Colors(ctx, isSelected).copyOf()
                BeadType.SOROBAN6 -> AbacusPreferences.getSoroban6Colors(ctx, isSelected).copyOf()
            }
            for (i in 0 until pathCount) {
                val color = currentColors[i]
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 4.dp, 0, 10.dp)
                }

                // ── Label + swatch ──────────────────────────────────────────
                val headerRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                val swatch = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(14.dp, 14.dp).also { it.marginEnd = 4.dp }
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
                }
                val label = TextView(ctx).apply {
                    text = labels[i]; textSize = 9f; setTextColor(0xAAFFFFFF.toInt())
                }
                headerRow.addView(swatch); headerRow.addView(label)
                row.addView(headerRow)

                // ── Hue seekbar (rainbow gradient) ──────────────────────────
                val hueLabel = TextView(ctx).apply {
                    text = "Renk"; textSize = 8f; setTextColor(0x88FFFFFF.toInt())
                    setPadding(0, 2.dp, 0, 0)
                }
                row.addView(hueLabel)

                val hueBar = SeekBar(ctx).apply {
                    splitTrack = false
                    background = null
                    max = 359
                    progress = AbacusBeadRenderer.getHue(color)
                    progressDrawable = rainbowDrawable()
                    thumb = roundThumb(color)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // ── Brightness seekbar (black → colour) ─────────────────────
                val briLabel = TextView(ctx).apply {
                    text = "Parlaklık"; textSize = 8f; setTextColor(0x88FFFFFF.toInt())
                    setPadding(0, 2.dp, 0, 0)
                }
                row.addView(briLabel)

                val briBar = SeekBar(ctx).apply {
                    splitTrack = false
                    background = null
                    max = 100
                    progress = AbacusBeadRenderer.getLightness100(color)
                    progressDrawable = lightnessGradientDrawable(AbacusBeadRenderer.getHue(color))
                    thumb = roundThumb(color)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val pathIdx = i
                val swatchRef = swatch

                // Shared update: recalculate colour from current H + V values
                fun onAnyChange() {
                    val hue = hueBar.progress
                    val bri = briBar.progress
                    val newColor = AbacusBeadRenderer.makeColor(hue, bri)
                    currentColors[pathIdx] = newColor
                    // Update swatch and thumbs immediately
                    swatchRef.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(newColor) }
                    hueBar.thumb = roundThumb(newColor)
                    briBar.thumb = roundThumb(newColor)
                    briBar.progressDrawable = lightnessGradientDrawable(hue)
                    // Persist
                    when (beadType) {
                        BeadType.SOROBAN -> AbacusPreferences.setSorobanColor(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL  -> AbacusPreferences.setAnimalColor(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL2 -> AbacusPreferences.setAnimal2Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL3 -> AbacusPreferences.setAnimal3Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL4 -> AbacusPreferences.setAnimal4Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL5 -> AbacusPreferences.setAnimal5Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL6 -> AbacusPreferences.setAnimal6Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL7 -> AbacusPreferences.setAnimal7Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL8 -> AbacusPreferences.setAnimal8Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.ANIMAL9 -> AbacusPreferences.setAnimal9Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.BOWLING -> AbacusPreferences.setBowlingColor(ctx, isSelected, pathIdx, newColor)
                        BeadType.BALL1 -> AbacusPreferences.setBall1Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.BALL3 -> AbacusPreferences.setBall3Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.BALL4 -> AbacusPreferences.setBall4Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.BALL5 -> AbacusPreferences.setBall5Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.BALL6 -> AbacusPreferences.setBall6Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.BALL7 -> AbacusPreferences.setBall7Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.BALL8 -> AbacusPreferences.setBall8Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.BALL9 -> AbacusPreferences.setBall9Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.BALL10 -> AbacusPreferences.setBall10Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.BALL11 -> AbacusPreferences.setBall11Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.BALL12 -> AbacusPreferences.setBall12Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.BALL13 -> AbacusPreferences.setBall13Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.BALL14 -> AbacusPreferences.setBall14Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.SOROBAN2 -> AbacusPreferences.setSoroban2Color(ctx, isSelected, pathIdx, newColor)
                        BeadType.SOROBAN6 -> AbacusPreferences.setSoroban6Color(ctx, isSelected, pathIdx, newColor)
                    }
                    refreshHeaders()
                    scheduleBeadRefresh(debounce = true)
                    scheduleFirestoreSync()
                }

                hueBar.setOnSeekBarChangeListener(simpleSeekListener { onAnyChange() })
                briBar.setOnSeekBarChangeListener(simpleSeekListener { onAnyChange() })

                row.addView(hueBar)
                row.addView(briBar)
                parent.addView(row)
            }

            // Reset button
            val resetBtn = ImageButton(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(56.dp, 56.dp).also {
                    it.topMargin = 8.dp
                    it.gravity = android.view.Gravity.CENTER_HORIZONTAL
                }
                setImageResource(R.drawable.special_reset_ic)
                background = null; scaleType = ImageView.ScaleType.FIT_CENTER; contentDescription = null
            }
            resetBtn.setOnClickListener {
                when (beadType) {
                    BeadType.SOROBAN -> AbacusPreferences.resetSorobanColors(ctx, isSelected)
                    BeadType.ANIMAL  -> AbacusPreferences.resetAnimalColors(ctx, isSelected)
                    BeadType.ANIMAL2 -> AbacusPreferences.resetAnimal2Colors(ctx, isSelected)
                    BeadType.ANIMAL3 -> AbacusPreferences.resetAnimal3Colors(ctx, isSelected)
                    BeadType.ANIMAL4 -> AbacusPreferences.resetAnimal4Colors(ctx, isSelected)
                    BeadType.ANIMAL5 -> AbacusPreferences.resetAnimal5Colors(ctx, isSelected)
                    BeadType.ANIMAL6 -> AbacusPreferences.resetAnimal6Colors(ctx, isSelected)
                    BeadType.ANIMAL7 -> AbacusPreferences.resetAnimal7Colors(ctx, isSelected)
                    BeadType.ANIMAL8 -> AbacusPreferences.resetAnimal8Colors(ctx, isSelected)
                    BeadType.ANIMAL9 -> AbacusPreferences.resetAnimal9Colors(ctx, isSelected)
                    BeadType.BOWLING -> AbacusPreferences.resetBowlingColors(ctx, isSelected)
                    BeadType.BALL1 -> AbacusPreferences.resetBall1Colors(ctx, isSelected)
                    BeadType.BALL3 -> AbacusPreferences.resetBall3Colors(ctx, isSelected)
                    BeadType.BALL4 -> AbacusPreferences.resetBall4Colors(ctx, isSelected)
                    BeadType.BALL5 -> AbacusPreferences.resetBall5Colors(ctx, isSelected)
                    BeadType.BALL6 -> AbacusPreferences.resetBall6Colors(ctx, isSelected)
                    BeadType.BALL7 -> AbacusPreferences.resetBall7Colors(ctx, isSelected)
                    BeadType.BALL8 -> AbacusPreferences.resetBall8Colors(ctx, isSelected)
                    BeadType.BALL9 -> AbacusPreferences.resetBall9Colors(ctx, isSelected)
                    BeadType.BALL10 -> AbacusPreferences.resetBall10Colors(ctx, isSelected)
                    BeadType.BALL11 -> AbacusPreferences.resetBall11Colors(ctx, isSelected)
                    BeadType.BALL12 -> AbacusPreferences.resetBall12Colors(ctx, isSelected)
                    BeadType.BALL13 -> AbacusPreferences.resetBall13Colors(ctx, isSelected)
                    BeadType.BALL14 -> AbacusPreferences.resetBall14Colors(ctx, isSelected)
                    BeadType.SOROBAN2 -> AbacusPreferences.resetSoroban2Colors(ctx, isSelected)
                    BeadType.SOROBAN6 -> AbacusPreferences.resetSoroban6Colors(ctx, isSelected)
                }
                buildColorRows(parent, isSelected)
                refreshHeaders()
                scheduleBeadRefresh(debounce = false)
                scheduleFirestoreSync()
            }
            parent.addView(resetBtn)
        }

        buildColorRows(columnNormal, false)
        buildColorRows(columnSelected, true)

        val isColorFeatureActive = ownedBeads[beadType.name]?.colorFeatureActive == true
        val overlay = v.findViewById<View>(R.id.tab2ColorLockOverlay)
        if (!isColorFeatureActive) {
            overlay.visibility = View.VISIBLE
            overlay.setOnClickListener {
                val imageResId = PAID_BEAD_INFO.find { it.second == beadType }?.third ?: R.drawable.soroban_bead
                showColorFeaturePurchasePanel(beadType, imageResId)
            }
        } else {
            overlay.visibility = View.GONE
        }

        return v
    }

    // ── Tab 3: Frame type ─────────────────────────────────────────────────────

    private fun inflateTab3(): View {
        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_tab3_frame_options, tabContent, false)
        val cardBg  = v.findViewById<MaterialCardView>(R.id.cardFrameBg)
        val cardBg2 = v.findViewById<MaterialCardView>(R.id.cardFrameBg2)
        val cardBg3 = v.findViewById<MaterialCardView>(R.id.cardFrameBg3)
        val cardBg4 = v.findViewById<MaterialCardView>(R.id.cardFrameBg4)
        val overlayBg2 = v.findViewById<View>(R.id.overlayFrameBg2)
        val overlayBg3 = v.findViewById<View>(R.id.overlayFrameBg3)
        val overlayBg4 = v.findViewById<View>(R.id.overlayFrameBg4)

        fun updateOverlays() {
            overlayBg2.visibility = if (ownedFrames[FrameType.FRAME_BG2.name]?.owned == true) View.GONE else View.VISIBLE
            overlayBg3.visibility = if (ownedFrames[FrameType.FRAME_BG3.name]?.owned == true) View.GONE else View.VISIBLE
            overlayBg4.visibility = if (ownedFrames[FrameType.FRAME_BG4.name]?.owned == true) View.GONE else View.VISIBLE
        }

        fun updateSelection() {
            val type = AbacusPreferences.getFrameType(requireContext())
            val sel = Color.parseColor("#FFFFD600")
            cardBg.strokeColor  = if (type == FrameType.FRAME_BG)  sel else Color.TRANSPARENT
            cardBg.strokeWidth  = if (type == FrameType.FRAME_BG)  6 else 0
            cardBg2.strokeColor = if (type == FrameType.FRAME_BG2) sel else Color.TRANSPARENT
            cardBg2.strokeWidth = if (type == FrameType.FRAME_BG2) 6 else 0
            cardBg3.strokeColor = if (type == FrameType.FRAME_BG3) sel else Color.TRANSPARENT
            cardBg3.strokeWidth = if (type == FrameType.FRAME_BG3) 6 else 0
            cardBg4.strokeColor = if (type == FrameType.FRAME_BG4) sel else Color.TRANSPARENT
            cardBg4.strokeWidth = if (type == FrameType.FRAME_BG4) 6 else 0
        }
        updateSelection()
        updateOverlays()

        // Ahşap — her zaman ücretsiz
        cardBg.setOnClickListener {
            AbacusPreferences.setFrameType(requireContext(), FrameType.FRAME_BG)
            updateSelection()
            refreshPreviewFrameBackground()
            scheduleFirestoreSync()
        }

        // Kalpli — 2000 altın
        cardBg2.setOnClickListener {
            if (ownedFrames[FrameType.FRAME_BG2.name]?.owned == true) {
                AbacusPreferences.setFrameType(requireContext(), FrameType.FRAME_BG2)
                updateSelection()
                refreshPreviewFrameBackground()
                scheduleFirestoreSync()
            } else {
                showFramePurchasePanel(
                    frameType  = FrameType.FRAME_BG2,
                    imageResId = R.drawable.abacus_frame_bg2,
                    price      = 2000,
                    overlayView = overlayBg2,
                    onPurchased = {
                        updateSelection()
                        updateOverlays()
                        refreshPreviewFrameBackground()
                    }
                )
            }
        }

        // Yıldızlı — 2000 altın
        cardBg3.setOnClickListener {
            if (ownedFrames[FrameType.FRAME_BG3.name]?.owned == true) {
                AbacusPreferences.setFrameType(requireContext(), FrameType.FRAME_BG3)
                updateSelection()
                refreshPreviewFrameBackground()
                scheduleFirestoreSync()
            } else {
                showFramePurchasePanel(
                    frameType  = FrameType.FRAME_BG3,
                    imageResId = R.drawable.abacus_frame_bg3,
                    price      = 2000,
                    overlayView = overlayBg3,
                    onPurchased = {
                        updateSelection()
                        updateOverlays()
                        refreshPreviewFrameBackground()
                    }
                )
            }
        }

        // Gradyanlı — 5000 altın
        cardBg4.setOnClickListener {
            if (ownedFrames[FrameType.FRAME_BG4.name]?.owned == true) {
                AbacusPreferences.setFrameType(requireContext(), FrameType.FRAME_BG4)
                updateSelection()
                refreshPreviewFrameBackground()
                scheduleFirestoreSync()
            } else {
                showFramePurchasePanel(
                    frameType  = FrameType.FRAME_BG4,
                    imageResId = R.drawable.abacus_frame_bg4,
                    price      = 5000,
                    overlayView = overlayBg4,
                    onPurchased = {
                        updateSelection()
                        updateOverlays()
                        refreshPreviewFrameBackground()
                    }
                )
            }
        }

        return v
    }

    private fun showFramePurchasePanel(
        frameType: FrameType,
        imageResId: Int,
        price: Int,
        overlayView: View,
        onPurchased: () -> Unit,
    ) {
        if (isDialogShowing) return
        isDialogShowing = true

        val ctx = requireContext()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            isDialogShowing = false
            return
        }

        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.panel_bead_purchase)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val width = (ctx.resources.displayMetrics.widthPixels * 0.88f).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnDismissListener { isDialogShowing = false }

        dialog.findViewById<View>(R.id.beadPurchaseClose).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<android.widget.ImageView>(R.id.beadPurchaseImage).setImageResource(imageResId)
        dialog.findViewById<android.widget.TextView>(R.id.beadPurchasePriceText)?.text = price.toString()

        val purchaseBtn = dialog.findViewById<View>(R.id.beadPurchaseButton)
        val closeBtn    = dialog.findViewById<View>(R.id.beadPurchaseClose)

        purchaseBtn.setOnClickListener {
            if (!purchaseBtn.isEnabled) return@setOnClickListener
            purchaseBtn.isEnabled = false
            closeBtn.isEnabled    = false
            dialog.setCancelable(false)

            if (UserWalletFirestore.getCachedCurrency(ctx) < price) {
                Toast.makeText(ctx, "Yetersiz altın", Toast.LENGTH_SHORT).show()
                purchaseBtn.isEnabled = true
                closeBtn.isEnabled    = true
                dialog.setCancelable(true)
                return@setOnClickListener
            }

            UserWalletFirestore.applyCurrencyDelta(
                context = ctx,
                uid     = uid,
                delta   = -price,
                reason  = WalletReason.SPEND,
                onSuccess = { wallet ->
                    FramePurchaseFirestore.markFrameOwned(uid, frameType,
                        onSuccess = {
                            val currentData = ownedFrames[frameType.name] ?: FrameData(owned = false, colorFeatureActive = false)
                            ownedFrames = ownedFrames + (frameType.name to currentData.copy(owned = true))
                            AbacusPreferences.setFrameType(ctx, frameType)
                            scheduleFirestoreSync()
                            refreshCurrencyUi()
                            overlayView.visibility = View.GONE
                            onPurchased()
                            dialog.dismiss()
                        },
                        onFailure = {
                            // Firestore kaydı başarısız — parayı iade et
                            UserWalletFirestore.applyCurrencyDelta(ctx, uid, +price, WalletReason.PURCHASE_ROLLBACK, wallet.rollbackToken)
                            Toast.makeText(ctx, "Kayıt hatası", Toast.LENGTH_SHORT).show()
                            purchaseBtn.isEnabled = true
                            closeBtn.isEnabled    = true
                            dialog.setCancelable(true)
                        }
                    )
                },
                onFailure = {
                    Toast.makeText(ctx, "Ödeme hatası", Toast.LENGTH_SHORT).show()
                    purchaseBtn.isEnabled = true
                    closeBtn.isEnabled    = true
                    dialog.setCancelable(true)
                }
            )
        }

        dialog.show()
    }

    private fun showFrameColorFeaturePurchasePanel(
        frameType: FrameType,
        imageResId: Int,
        overlayView: View,
        onPurchased: () -> Unit,
    ) {
        if (isDialogShowing) return
        isDialogShowing = true

        val ctx = requireContext()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            isDialogShowing = false
            return
        }

        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.panel_bead_purchase)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val width = (ctx.resources.displayMetrics.widthPixels * 0.88f).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnDismissListener { isDialogShowing = false }

        dialog.findViewById<View>(R.id.beadPurchaseClose).setOnClickListener { dialog.dismiss() }
        
        dialog.findViewById<android.widget.ImageView>(R.id.beadPurchaseImage).setImageResource(imageResId)
        dialog.findViewById<android.widget.ImageView>(R.id.beadPurchaseFeatureIcon)?.visibility = View.VISIBLE
        
        val price = 2000
        dialog.findViewById<android.widget.TextView>(R.id.beadPurchasePriceText)?.text = price.toString()

        val purchaseBtn = dialog.findViewById<View>(R.id.beadPurchaseButton)
        val closeBtn    = dialog.findViewById<View>(R.id.beadPurchaseClose)

        purchaseBtn.setOnClickListener {
            if (!purchaseBtn.isEnabled) return@setOnClickListener
            purchaseBtn.isEnabled = false
            closeBtn.isEnabled    = false
            dialog.setCancelable(false)

            if (UserWalletFirestore.getCachedCurrency(ctx) < price) {
                Toast.makeText(ctx, "Yetersiz altın", Toast.LENGTH_SHORT).show()
                purchaseBtn.isEnabled = true
                closeBtn.isEnabled    = true
                dialog.setCancelable(true)
                return@setOnClickListener
            }

            UserWalletFirestore.applyCurrencyDelta(
                context = ctx,
                uid     = uid,
                delta   = -price,
                reason  = WalletReason.SPEND,
                onSuccess = { wallet ->
                    FramePurchaseFirestore.setColorFeatureActive(uid, frameType,
                        onSuccess = {
                            val currentData = ownedFrames[frameType.name] ?: FrameData(owned = false, colorFeatureActive = false)
                            ownedFrames = ownedFrames + (frameType.name to currentData.copy(colorFeatureActive = true))
                            refreshCurrencyUi()
                            overlayView.visibility = View.GONE
                            onPurchased()
                            dialog.dismiss()
                        },
                        onFailure = {
                            UserWalletFirestore.applyCurrencyDelta(ctx, uid, +price, WalletReason.PURCHASE_ROLLBACK, wallet.rollbackToken)
                            Toast.makeText(ctx, "Kayıt hatası", Toast.LENGTH_SHORT).show()
                            purchaseBtn.isEnabled = true
                            closeBtn.isEnabled    = true
                            dialog.setCancelable(true)
                        }
                    )
                },
                onFailure = {
                    Toast.makeText(ctx, "Ödeme hatası", Toast.LENGTH_SHORT).show()
                    purchaseBtn.isEnabled = true
                    closeBtn.isEnabled    = true
                    dialog.setCancelable(true)
                }
            )
        }

        dialog.show()
    }

    // ── Tab 4: Frame colour ───────────────────────────────────────────────────

    private fun inflateTab4(): View {
        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_tab4_frame_color, tabContent, false)

        val rowsContainer = v.findViewById<LinearLayout>(R.id.frameColorRows)
        val resetBtn = v.findViewById<ImageButton>(R.id.resetFrameColors)
        val ctx = requireContext()

        data class ColorEntry(
            val label: String,
            val currentColor: Int,
            val onChanged: (Int) -> Unit
        )

        fun buildRows() {
            rowsContainer.removeAllViews()

            val entries: List<ColorEntry> = when (AbacusPreferences.getFrameType(ctx)) {
                FrameType.FRAME_BG -> listOf(
                    ColorEntry("Ahşap rengi", AbacusPreferences.getFrameBgWoodColor(ctx))
                    { c -> AbacusPreferences.setFrameBgWoodColor(ctx, c); refreshPreviewFrameBackground() }
                )
                FrameType.FRAME_BG2 -> listOf(
                    ColorEntry("Ahşap rengi", AbacusPreferences.getFrameBg2WoodColor(ctx))
                    { c -> AbacusPreferences.setFrameBg2WoodColor(ctx, c); refreshPreviewFrameBackground() },
                    ColorEntry("Kalp rengi", AbacusPreferences.getFrameBg2HeartColor(ctx))
                    { c -> AbacusPreferences.setFrameBg2HeartColor(ctx, c); refreshPreviewFrameBackground() }
                )
                FrameType.FRAME_BG3 -> listOf(
                    ColorEntry("Ahşap rengi", AbacusPreferences.getFrameBg3WoodColor(ctx))
                    { c -> AbacusPreferences.setFrameBg3WoodColor(ctx, c); refreshPreviewFrameBackground() },
                    ColorEntry("Yıldız rengi", AbacusPreferences.getFrameBg3StarColor(ctx))
                    { c -> AbacusPreferences.setFrameBg3StarColor(ctx, c); refreshPreviewFrameBackground() }
                )
                FrameType.FRAME_BG4 -> {
                    val colors = AbacusPreferences.getFrameBg4Colors(ctx)
                    listOf(
                        ColorEntry("Renk 1", colors[0]) { c -> AbacusPreferences.setFrameBg4Color(ctx, 0, c); refreshPreviewFrameBackground() },
                        ColorEntry("Renk 2", colors[1]) { c -> AbacusPreferences.setFrameBg4Color(ctx, 1, c); refreshPreviewFrameBackground() },
                        ColorEntry("Renk 3", colors[2]) { c -> AbacusPreferences.setFrameBg4Color(ctx, 2, c); refreshPreviewFrameBackground() },
                        ColorEntry("Renk 4", colors[3]) { c -> AbacusPreferences.setFrameBg4Color(ctx, 3, c); refreshPreviewFrameBackground() },
                        ColorEntry("Renk 5", colors[4]) { c -> AbacusPreferences.setFrameBg4Color(ctx, 4, c); refreshPreviewFrameBackground() }
                    )
                }
            }

            for (entry in entries) {
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 4.dp, 0, 14.dp)
                }

                // ── Label + swatch ──────────────────────────────────────────
                val headerRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                val swatch = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(14.dp, 14.dp).also { it.marginEnd = 6.dp }
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(entry.currentColor) }
                }
                val label = TextView(ctx).apply {
                    text = entry.label; textSize = 10f; setTextColor(0xAAFFFFFF.toInt())
                }
                headerRow.addView(swatch); headerRow.addView(label)
                row.addView(headerRow)

                // ── Hue seekbar (rainbow gradient) ──────────────────────────
                val hueLabel = TextView(ctx).apply {
                    text = "Renk"; textSize = 8f; setTextColor(0x88FFFFFF.toInt())
                    setPadding(0, 2.dp, 0, 0)
                }
                row.addView(hueLabel)

                val hueBar = SeekBar(ctx).apply {
                    splitTrack = false
                    background = null
                    max = 359
                    progress = AbacusBeadRenderer.getHue(entry.currentColor)
                    progressDrawable = rainbowDrawable()
                    thumb = roundThumb(entry.currentColor)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // ── Brightness seekbar (black → colour) ─────────────────────
                val briLabel = TextView(ctx).apply {
                    text = "Parlaklık"; textSize = 8f; setTextColor(0x88FFFFFF.toInt())
                    setPadding(0, 2.dp, 0, 0)
                }
                row.addView(briLabel)

                val briBar = SeekBar(ctx).apply {
                    splitTrack = false
                    background = null
                    max = 100
                    progress = AbacusBeadRenderer.getLightness100(entry.currentColor)
                    progressDrawable = lightnessGradientDrawable(AbacusBeadRenderer.getHue(entry.currentColor))
                    thumb = roundThumb(entry.currentColor)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val onChanged = entry.onChanged
                val swatchRef = swatch

                fun onAnyChange() {
                    val hue = hueBar.progress
                    val bri = briBar.progress
                    val newColor = AbacusBeadRenderer.makeColor(hue, bri)
                    swatchRef.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(newColor) }
                    hueBar.thumb = roundThumb(newColor)
                    briBar.thumb = roundThumb(newColor)
                    briBar.progressDrawable = lightnessGradientDrawable(hue)
                    onChanged(newColor)
                    scheduleFirestoreSync()
                }

                hueBar.setOnSeekBarChangeListener(simpleSeekListener { onAnyChange() })
                briBar.setOnSeekBarChangeListener(simpleSeekListener { onAnyChange() })

                row.addView(hueBar)
                row.addView(briBar)
                rowsContainer.addView(row)
            }
        }

        buildRows()
        resetBtn.setOnClickListener {
            AbacusPreferences.resetAllFrameColors(ctx)
            buildRows()
            refreshPreviewFrameBackground()
            scheduleFirestoreSync()
        }

        // Tab 4 kilit overlay: seçili çerçeve satın alınmamışsa veya renk özelliği yoksa kilit
        val frameColorLockOverlay = v.findViewById<View>(R.id.tab4ColorLockOverlay)
        val currentFrameType = AbacusPreferences.getFrameType(ctx)
        val isFrameColorUnlocked = currentFrameType == com.example.app.abacus.AbacusPreferences.FrameType.FRAME_BG
            || ownedFrames[currentFrameType.name]?.colorFeatureActive == true
        if (isFrameColorUnlocked) {
            frameColorLockOverlay?.visibility = View.GONE
        } else {
            frameColorLockOverlay?.visibility = View.VISIBLE
            frameColorLockOverlay?.setOnClickListener {
                val imgRes = when (currentFrameType) {
                    com.example.app.abacus.AbacusPreferences.FrameType.FRAME_BG2 -> R.drawable.abacus_frame_bg2
                    com.example.app.abacus.AbacusPreferences.FrameType.FRAME_BG3 -> R.drawable.abacus_frame_bg3
                    com.example.app.abacus.AbacusPreferences.FrameType.FRAME_BG4 -> R.drawable.abacus_frame_bg4
                    else -> return@setOnClickListener
                }
                showFrameColorFeaturePurchasePanel(
                    frameType   = currentFrameType,
                    imageResId  = imgRes,
                    overlayView = frameColorLockOverlay,
                    onPurchased = {
                        buildRows()
                        refreshPreviewFrameBackground()
                    }
                )
            }
        }

        return v
    }

    // ── Seekbar gradient helpers ───────────────────────────────────────────────

    /**
     * Returns a drawable that paints a full rainbow gradient across the seekbar track.
     * Uses 7-stop GradientDrawable (hue: 0→60→120→180→240→300→360).
     */
    private fun rainbowDrawable(): GradientDrawable {
        val colors = intArrayOf(
            Color.HSVToColor(floatArrayOf(0f,   1f, 1f)),
            Color.HSVToColor(floatArrayOf(60f,  1f, 1f)),
            Color.HSVToColor(floatArrayOf(120f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(180f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(240f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(300f, 1f, 1f)),
            Color.HSVToColor(floatArrayOf(360f, 1f, 1f))
        )
        return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply {
            cornerRadius = 8f
        }
    }

    /**
     * Returns a gradient from black → full-brightness colour → white at [hueDegrees].
     * This visually shows what the lightness seekbar does.
     */
    private fun lightnessGradientDrawable(hueDegrees: Int): GradientDrawable {
        val fullColor = Color.HSVToColor(floatArrayOf(hueDegrees.toFloat(), AbacusBeadRenderer.FIXED_SATURATION, 1f))
        return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.BLACK, fullColor, Color.WHITE)).apply {
            cornerRadius = 8f
        }
    }

    /**
     * Returns a circular thumb drawable filled with [color].
     * Gives instant visual feedback of the chosen colour.
     */
    private fun roundThumb(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(2.dp, 0xFFFFFFFF.toInt())
        val sz = 20.dp
        setSize(sz, sz)
    }

    // ── Misc helpers ──────────────────────────────────────────────────────────

    /** Convenience: only calls [onChange] on fromUser=true progress changes. */
    private fun simpleSeekListener(onChange: () -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) { if (fromUser) onChange() }
        override fun onStartTrackingTouch(bar: SeekBar) {}
        override fun onStopTrackingTouch(bar: SeekBar) {}
    }

    private fun refreshCurrencyUi() {
        val ctx = context ?: return
        currencyText?.text = UserWalletFirestore.getCachedCurrency(ctx).toString()
        keyText?.text = UserWalletFirestore.getCachedKeys(ctx).toString()
        (activity as? MainActivity)?.refreshWalletUi()
    }

    private fun getBeadPrice(beadType: BeadType): Int {
        return when (beadType) {
            BeadType.SOROBAN -> 0
            BeadType.SOROBAN2, BeadType.SOROBAN6 -> 2000
            BeadType.BOWLING, BeadType.BALL1, BeadType.BALL3, BeadType.BALL4, BeadType.BALL5, BeadType.BALL6, BeadType.BALL7 -> 3000
            BeadType.BALL8, BeadType.BALL9, BeadType.BALL10, BeadType.BALL11, BeadType.BALL12, BeadType.BALL13, BeadType.BALL14 -> 4000
            BeadType.ANIMAL3, BeadType.ANIMAL8 -> 40
            BeadType.ANIMAL, BeadType.ANIMAL2, BeadType.ANIMAL4, BeadType.ANIMAL5, BeadType.ANIMAL6, BeadType.ANIMAL7, BeadType.ANIMAL9 -> 5000
        }
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        fun newInstance() = AbacusCustomizationFragment()
    }
}

package com.example.app.abacus

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import com.airbnb.lottie.LottieAnimationView
import com.example.app.R
import com.example.app.databinding.ViewAbacusSettingsPanelBinding

/**
 * Abaküs ayar düğmesi ve sağdan kayan ayar paneli ([R.layout.view_abacus_settings_panel]):
 * abaküsün eni/boyu ve alt boşluğu, boncukların eni/boyu ve üst/alt boşlukları.
 *
 * BlindingLessonFragment ile ödev oynatıcısında ortak. Ayarlar "AbacusUISettings" tercihlerinde,
 * yani bir ekranda kaydedilen boyut diğerinde de geçerli. Abaküs ölçeği yan düğme sütununa
 * sığdırmayla birlikte uygulanıyor ([AbacusSideButtonFit]; onu da bu sınıf kuruyor).
 *
 * [attach] onViewCreated'da (abaküs denetleyicisi kurulmadan önce: boncuk boşlukları [applyBeadOffsets]
 * ile ona aktarılıyor), [detach] onDestroyView'da çağrılmalı.
 *
 * @param controller abaküs denetleyicisi henüz kurulmadıysa null.
 * @param onOpen panel açılırken (ekran abaküsü sıfırlayıp dokunmayı kapatıyor).
 * @param onClose panel kapanırken (dokunma geri açılıyor).
 */
class AbacusSettingsPanel(
    private val panel: ViewAbacusSettingsPanelBinding,
    private val abacusContainer: View,
    private val modeButton: LottieAnimationView,
    private val sideButtons: List<View>,
    private val controller: () -> AbacusBeadController?,
    private val onOpen: () -> Unit,
    private val onClose: () -> Unit,
) {
    private val context: Context get() = abacusContainer.context
    private val density get() = context.resources.displayMetrics.density

    var isOpen = false
        private set
    private var sideFit: AbacusSideButtonFit? = null

    private var savedAbacusScaleX = 1.0f
    private var savedAbacusScaleY = 1.0f
    private var savedAbacusMarginBottomDp = 0f
    private var savedBeadScaleX = 1.0f
    private var savedBeadScaleY = 1.0f
    private var savedBeadMarginTopDp = 0f
    private var savedBeadMarginBottomDp = 0f

    fun attach() {
        loadSettings()

        sideFit?.detach()
        sideFit = AbacusSideButtonFit(
            abacus = abacusContainer,
            sideButtons = sideButtons,
            gapPx = 6f * density,
        ).also {
            it.attach()
            it.setUserScale(savedAbacusScaleX, savedAbacusScaleY)
        }
        val initParams = abacusContainer.layoutParams as? ViewGroup.MarginLayoutParams
        if (savedAbacusMarginBottomDp > 0f) {
            initParams?.bottomMargin = (savedAbacusMarginBottomDp * density).toInt()
            abacusContainer.layoutParams = initParams
        }

        if (savedAbacusMarginBottomDp == 0f) {
            val params = abacusContainer.layoutParams as? ViewGroup.MarginLayoutParams
            savedAbacusMarginBottomDp = (params?.bottomMargin ?: 0) / density
        }

        // Abacus boyutları (0.5x ile 2.0x aralığında ayarlansın, progress 0-100)
        panel.seekBarScaleX.progress = ((savedAbacusScaleX - 0.5f) * (100f / 1.5f)).toInt().coerceIn(0, 100)
        panel.seekBarScaleY.progress = ((savedAbacusScaleY - 0.5f) * (100f / 1.5f)).toInt().coerceIn(0, 100)
        panel.seekBarMarginBottom.progress = savedAbacusMarginBottomDp.toInt().coerceIn(0, 100)

        // Boncuk boyutları (0.5x ile 2.0x aralığında ayarlansın, progress 0-100)
        panel.seekBarBeadScaleX.progress = ((savedBeadScaleX - 0.5f) * (100f / 1.5f)).toInt().coerceIn(0, 100)
        panel.seekBarBeadScaleY.progress = ((savedBeadScaleY - 0.5f) * (100f / 1.5f)).toInt().coerceIn(0, 100)

        panel.seekBarBeadMarginTop.progress = savedBeadMarginTopDp.toInt().coerceIn(0, 30)
        panel.seekBarBeadMarginBottom.progress = savedBeadMarginBottomDp.toInt().coerceIn(0, 50)

        // Panel açıldığında kaydedilmiş boncuk boyutlarını ve boşluklarını uygula
        setAllBeadsScale(savedBeadScaleX, savedBeadScaleY)
        setAllBeadsMargins(savedBeadMarginTopDp, savedBeadMarginBottomDp)

        panel.seekBarScaleX.onProgress { sideFit?.setUserScaleX(0.5f + (it / 100f) * 1.5f) }
        panel.seekBarScaleY.onProgress { sideFit?.setUserScaleY(0.5f + (it / 100f) * 1.5f) }
        panel.seekBarMarginBottom.onProgress {
            val params = abacusContainer.layoutParams as? ViewGroup.MarginLayoutParams
            params?.bottomMargin = (it * density).toInt()
            abacusContainer.layoutParams = params
        }
        // 0-100 -> 0.5 - 2.0
        panel.seekBarBeadScaleX.onProgress { setAllBeadsScale(0.5f + (it / 100f) * 1.5f, null) }
        panel.seekBarBeadScaleY.onProgress { setAllBeadsScale(null, 0.5f + (it / 100f) * 1.5f) }
        panel.seekBarBeadMarginTop.onProgress {
            setAllBeadsMargins(it.toFloat(), null)
            recomputeBeadOffsets(topDp = it, bottomDp = panel.seekBarBeadMarginBottom.progress)
        }
        panel.seekBarBeadMarginBottom.onProgress {
            setAllBeadsMargins(null, it.toFloat())
            recomputeBeadOffsets(topDp = panel.seekBarBeadMarginTop.progress, bottomDp = it)
        }

        panel.abacusSettingsResetButton.setOnClickListener { resetToDefaults() }
        panel.abacusSettingsSaveButton.setOnClickListener { saveAndClose() }
        modeButton.setOnClickListener { toggle() }
    }

    fun detach() {
        sideFit?.detach()
        sideFit = null
    }

    /** Kaydedilmiş boncuk boşluklarını denetleyiciye aktarır (denetleyici kurulurken). */
    fun applyBeadOffsets(controller: AbacusBeadController) {
        controller.setBeadMarginOffsets(savedBeadMarginBottomDp * density, savedBeadMarginTopDp * density)
    }

    fun toggle() {
        if (!isOpen) {
            // Panelı aç: 0 → 25. frame
            modeButton.setMinAndMaxFrame(0, 25)
            modeButton.playAnimation()
            open()
        } else {
            // Panelı kapat: 25 → 45. frame
            modeButton.setMinAndMaxFrame(25, 45)
            modeButton.playAnimation()
            close()
        }
    }

    /** Açıksa kapatır (ör. soru akarken ekran kapanmasını istiyor). */
    fun closeIfOpen() {
        if (isOpen) toggle()
    }

    private fun open() {
        onOpen()
        isOpen = true
        panel.root.animate()
            .translationX(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun close() {
        onClose()
        isOpen = false
        val offset = panel.root.width.toFloat() + (100f * density)
        panel.root.animate()
            .translationX(offset)
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .start()
    }

    private fun resetToDefaults() {
        // Default progress for 1.0f on 0.5-2.0 scale is 33
        val defaultProgress = ((1.0f - 0.5f) * (100f / 1.5f)).toInt()

        // Abacus default: 1.0f, 1.0f
        panel.seekBarScaleX.progress = defaultProgress
        panel.seekBarScaleY.progress = defaultProgress

        // Margin bottom default
        val defaultMarginPx = context.resources.getDimensionPixelSize(R.dimen.tutorial_abacus_linear_margin_bottom)
        panel.seekBarMarginBottom.progress = (defaultMarginPx / density).toInt()

        // Bead default: 1.0f, 1.0f
        panel.seekBarBeadScaleX.progress = defaultProgress
        panel.seekBarBeadScaleY.progress = defaultProgress

        panel.seekBarBeadMarginTop.progress = 0
        panel.seekBarBeadMarginBottom.progress = 0

        sideFit?.setUserScale(1.0f, 1.0f)

        val params = abacusContainer.layoutParams as? ViewGroup.MarginLayoutParams
        params?.bottomMargin = defaultMarginPx
        abacusContainer.layoutParams = params

        setAllBeadsScale(1.0f, 1.0f)
        setAllBeadsMargins(0f, 0f)

        clearSettings()

        abacusContainer.post {
            controller()?.let {
                it.setBeadMarginOffsets(0f, 0f)
                it.computeMovementDistancesFromLayout(force = true)
            }
        }
    }

    private fun saveAndClose() {
        // Görünen ölçek değil kullanıcının seçtiği ölçek kaydedilir (sığdırma payı ekrana göre değişir).
        savedAbacusScaleX = sideFit?.userScaleX ?: abacusContainer.scaleX
        savedAbacusScaleY = sideFit?.userScaleY ?: abacusContainer.scaleY
        val params = abacusContainer.layoutParams as? ViewGroup.MarginLayoutParams
        savedAbacusMarginBottomDp = ((params?.bottomMargin ?: 0) / density)

        savedBeadScaleX = 0.5f + (panel.seekBarBeadScaleX.progress / 100f) * 1.5f
        savedBeadScaleY = 0.5f + (panel.seekBarBeadScaleY.progress / 100f) * 1.5f

        savedBeadMarginTopDp = panel.seekBarBeadMarginTop.progress.toFloat()
        savedBeadMarginBottomDp = panel.seekBarBeadMarginBottom.progress.toFloat()

        // Offset'leri controller'a kaydet
        controller()?.let {
            applyBeadOffsets(it)
            it.computeMovementDistancesFromLayout(force = true)
            it.forceRecaptureInitialPositions()
        }

        saveSettings()

        close()
        modeButton.setMinAndMaxFrame(25, 45)
        modeButton.playAnimation()
    }

    private fun recomputeBeadOffsets(topDp: Int, bottomDp: Int) {
        abacusContainer.post {
            controller()?.let {
                it.setBeadMarginOffsets(bottomDp * density, topDp * density)
                it.computeMovementDistancesFromLayout(force = true)
            }
        }
    }

    private fun prefs() = context.getSharedPreferences("AbacusUISettings", Context.MODE_PRIVATE)

    private fun loadSettings() {
        val prefs = prefs()
        savedAbacusScaleX = prefs.getFloat("savedAbacusScaleX", 1.0f)
        savedAbacusScaleY = prefs.getFloat("savedAbacusScaleY", 1.0f)
        savedAbacusMarginBottomDp = prefs.getFloat("savedAbacusMarginBottomDp", 0f)
        savedBeadScaleX = prefs.getFloat("savedBeadScaleX", 1.0f)
        savedBeadScaleY = prefs.getFloat("savedBeadScaleY", 1.0f)
        savedBeadMarginTopDp = prefs.getFloat("savedBeadMarginTopDp", 0f)
        savedBeadMarginBottomDp = prefs.getFloat("savedBeadMarginBottomDp", 0f)
    }

    private fun saveSettings() {
        prefs().edit().apply {
            putFloat("savedAbacusScaleX", savedAbacusScaleX)
            putFloat("savedAbacusScaleY", savedAbacusScaleY)
            putFloat("savedAbacusMarginBottomDp", savedAbacusMarginBottomDp)
            putFloat("savedBeadScaleX", savedBeadScaleX)
            putFloat("savedBeadScaleY", savedBeadScaleY)
            putFloat("savedBeadMarginTopDp", savedBeadMarginTopDp)
            putFloat("savedBeadMarginBottomDp", savedBeadMarginBottomDp)
            apply()
        }
    }

    private fun clearSettings() {
        prefs().edit().clear().apply()
        savedAbacusScaleX = 1.0f
        savedAbacusScaleY = 1.0f
        savedAbacusMarginBottomDp = 0f
        savedBeadScaleX = 1.0f
        savedBeadScaleY = 1.0f
        savedBeadMarginTopDp = 0f
        savedBeadMarginBottomDp = 0f
    }

    private fun setAllBeadsScale(scaleX: Float?, scaleY: Float?) {
        forEachBead { v, _ ->
            scaleX?.let { v.scaleX = it }
            scaleY?.let { v.scaleY = it }
        }
    }

    private fun setAllBeadsMargins(marginTopDp: Float?, marginBottomDp: Float?) {
        forEachBead { v, idName ->
            if (marginTopDp != null && idName.contains("top")) {
                val params = v.layoutParams as? ViewGroup.MarginLayoutParams
                params?.topMargin = (marginTopDp * density).toInt()
                v.layoutParams = params
            }
            if (marginBottomDp != null && idName.contains("bottom")) {
                val params = v.layoutParams as? ViewGroup.MarginLayoutParams
                val baseMarginDp = when {
                    idName.contains("bottom1") -> 82f
                    idName.contains("bottom2") -> 58f
                    idName.contains("bottom3") -> 34f
                    idName.contains("bottom4") -> 10f
                    else -> 0f
                }
                params?.bottomMargin = ((baseMarginDp + marginBottomDp) * density).toInt()
                v.layoutParams = params
            }
        }
    }

    /** Abaküsteki boncuklar: kimlik adında "bead" geçen resimler (rodN_bead_*). */
    private fun forEachBead(action: (ImageView, String) -> Unit) {
        val unvisited = ArrayList<View>()
        unvisited.add(abacusContainer)
        while (unvisited.isNotEmpty()) {
            val v = unvisited.removeAt(0)
            if (v is ViewGroup) for (i in 0 until v.childCount) unvisited.add(v.getChildAt(i))
            if (v.id == View.NO_ID || v !is ImageView) continue
            val idName = try {
                context.resources.getResourceEntryName(v.id)
            } catch (e: Exception) {
                continue // Ignore views without resource names
            }
            if (idName.contains("bead")) action(v, idName)
        }
    }

    private fun SeekBar.onProgress(block: (Int) -> Unit) {
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) = block(progress)
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }
}

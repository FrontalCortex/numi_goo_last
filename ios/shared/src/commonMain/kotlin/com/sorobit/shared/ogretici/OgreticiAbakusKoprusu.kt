// Kaynak (Android):
//   model/BeadAnimation.kt — öğretici adımının tek boncuk animasyonu (tür 1..4). Android boncuk
//     görünümünü denetleyiciden geçmeden oynatır; denetleyici sonra fiziksel konumdan okur
//     (AbacusBeadController.syncStateFromUi — Android'de ensureTutorialAbacusMetricsIfVisible / setupBeads).
//   TutorialFragment.writeAnswerNumber + updateBeadForDigit (~1302-1512) — bir sayıyı animasyonla
//     yazma: yalnızca konumu hedefle uyuşmayan boncuklar oynar; 400 ms sonra iç durum sayıdan kurulur
//     (applyInternalStateFromValue(number, applyAppearance = true)).
//
// iOS görünüm katmanı (Abakus/AbakusTahta.swift) bu fonksiyonları kendi AbacusBeadHost'uyla çağırır;
// animasyonun kendisini host oynatır (animateTranslationY).
package com.sorobit.shared.ogretici

import com.sorobit.shared.abakus.AbacusBead
import com.sorobit.shared.abakus.AbacusBeadController
import com.sorobit.shared.abakus.AbacusBeadHost
import com.sorobit.shared.matematik.BeadAnimation
import com.sorobit.shared.matematik.BeadMoveDistances

object OgreticiAbakusKoprusu {
    /** writeAnswerNumber: animasyonların bitip iç durumun sayıdan kurulduğu gecikme. */
    const val YAZMA_GECIKMESI_MS = 400L

    /**
     * Tek boncuğu [tur]'e göre oynatır (Android BeadAnimation.animate). Bitince iç durum fiziksel
     * konumdan okunur ve [bitti] çağrılır. Bilinmeyen tür ya da çubuk/sıra dışında false döner ve
     * [bitti] HEMEN çağrılır.
     */
    fun oynat(
        denetleyici: AbacusBeadController,
        host: AbacusBeadHost,
        cubuk: Int,
        ust: Boolean,
        sira: Int,
        tur: Int,
        bitti: () -> Unit,
    ): Boolean {
        val boncuk = boncukBul(denetleyici, cubuk, ust, sira)
        val mesafe = BeadMoveDistances(denetleyici.getBottomMoveDistancePx(), denetleyici.getTopMoveDistancePx())
        val plan = BeadAnimation.planFor(tur, mesafe)
        if (boncuk == null || plan == null) {
            bitti()
            return false
        }
        host.updateBeadAppearance(boncuk, plan.drawableKey == BeadAnimation.DRAWABLE_BEAD_SELECTED)
        host.animateTranslationY(boncuk, plan.translationY, plan.durationMs) {
            if (plan.resetTranslationAtEnd) {
                boncuk.bottomMargin = boncuk.bottomMargin + plan.bottomMarginDelta
                boncuk.translationY = 0f
            }
            // Görünüm (seçili / değil) Android'deki gibi animasyonun başında verildi; burada ezilmez.
            denetleyici.syncStateFromUi(false)
            bitti()
        }
        return true
    }

    /**
     * Abaküse [deger]'i (0..99999) animasyonla yazar (Android writeAnswerNumber). [bitti],
     * [YAZMA_GECIKMESI_MS] sonra iç durum sayıdan kurulunca çağrılır. Bu sürede denetleyici
     * kapatılır; açmak çağıranın işidir (Android: adım abacusClickable ise setEnabled(true)).
     */
    fun sayiyiYaz(
        denetleyici: AbacusBeadController,
        host: AbacusBeadHost,
        deger: Int,
        bitti: () -> Unit,
    ) {
        denetleyici.setEnabled(false)
        val metin = deger.coerceIn(0, 99999).toString().padStart(5, '0')
        val mesafe = BeadMoveDistances(denetleyici.getBottomMoveDistancePx(), denetleyici.getTopMoveDistancePx())
        // Android sırası: birler (rod4) → on binler (rod0).
        for (cubuk in 4 downTo 0) {
            val rakam = metin[cubuk].digitToInt()
            val ustAsagi = rakam >= 5
            val ustBoncuk = denetleyici.topBeads[cubuk]
            if (denetleyici.isTopBeadDown(cubuk) != ustAsagi) {
                val tur = if (ustAsagi) 3 else 4
                hareketEttir(host, ustBoncuk, tur, mesafe)
            }
            host.updateBeadAppearance(ustBoncuk, ustAsagi)
            val altHedef = rakam % 5
            for (i in 0..3) {
                val yukarida = (i + 1) <= altHedef
                val boncuk = denetleyici.bottomBeads[cubuk][i]
                if (denetleyici.isBottomBeadPhysicallyRaised(cubuk, i) != yukarida) {
                    hareketEttir(host, boncuk, if (yukarida) 1 else 2, mesafe)
                }
                host.updateBeadAppearance(boncuk, yukarida)
            }
        }
        host.postDelayed(YAZMA_GECIKMESI_MS) {
            denetleyici.applyInternalStateFromValue(deger.coerceIn(0, 99999), true)
            bitti()
        }
    }

    private fun hareketEttir(host: AbacusBeadHost, boncuk: AbacusBead, tur: Int, mesafe: BeadMoveDistances) {
        val plan = BeadAnimation.planFor(tur, mesafe) ?: return
        host.animateTranslationY(boncuk, plan.translationY, plan.durationMs) {
            if (plan.resetTranslationAtEnd) {
                boncuk.bottomMargin = boncuk.bottomMargin + plan.bottomMarginDelta
                boncuk.translationY = 0f
            }
        }
    }

    private fun boncukBul(denetleyici: AbacusBeadController, cubuk: Int, ust: Boolean, sira: Int): AbacusBead? {
        if (cubuk !in 0..4) return null
        if (ust) return denetleyici.topBeads[cubuk]
        if (sira !in 0..3) return null
        return denetleyici.bottomBeads[cubuk][sira]
    }
}

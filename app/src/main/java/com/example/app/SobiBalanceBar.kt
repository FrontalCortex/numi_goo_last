package com.example.app

import android.view.View
import android.widget.TextView

/**
 * Sobi'nin bakiye satırı: "Bugün 2 ücretsiz mesaj · 15 kredi". İkisi de bitince etiket "Kredi al ›"
 * olur ve dokununca [onBuyCredits]; denemeye uygun kullanıcıya altında "1 hafta Pro'yu ücretsiz dene ›"
 * ([onProTrial]). Ders içi Sobi ekranı ([MistakeReviewDialog]) ve Sobi sekmesi ([SobiChatFragment])
 * aynı satırı gösteriyor; açılış yolu ekrana göre değiştiği için (ders içinde ekran kapanıp kapanış
 * bildirimi bekletiliyor) dokunuşlar dışarıdan veriliyor.
 *
 * Deneme yalnızca hak ve kredi bitince: Pro'da günde 20 mesaj. Uygun değilse (zaten Pro, teklif yok,
 * hediye cihazda kullanılmış) yalnızca kredi ([SobiProTrialFragment.canOfferTrial]).
 */
class SobiBalanceBar(
    private val balanceView: TextView,
    private val trialView: TextView?,
    private val activity: () -> MainActivity?,
    private val onBuyCredits: () -> Unit,
    private val onProTrial: () -> Unit,
) {

    fun show(freeLeft: Int, credits: Int) {
        val v = balanceView
        v.visibility = View.VISIBLE
        val empty = freeLeft <= 0 && credits <= 0
        v.text = when {
            empty -> appString(R.string.sobi_balance_empty)
            freeLeft > 0 && credits > 0 -> appString(R.string.sobi_balance_free_credits, freeLeft, credits)
            freeLeft > 0 -> appString(R.string.sobi_balance_free, freeLeft)
            else -> appString(R.string.sobi_balance_credits, credits)
        }
        v.setTextColor(if (empty) 0xFF1CB0F6.toInt() else 0xFF8B949E.toInt())
        v.setOnClickListener(if (empty) View.OnClickListener { onBuyCredits() } else null)
        val trial = trialView ?: return
        val main = activity()
        val offerTrial = empty && SobiProTrialFragment.canOfferTrial(main)
        trial.visibility = if (offerTrial) View.VISIBLE else View.GONE
        if (offerTrial) {
            val days = main?.billingManager?.freeTrialDays(BillingCatalog.SUB_PRO)
            trial.text = SobiProTrialFragment.titleText(trial.context, days) + " ›"
            trial.setOnClickListener { onProTrial() }
        }
    }
}

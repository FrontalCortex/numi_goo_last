// KAYNAK: app/src/main/java/com/example/app/SubscriptionCta.kt (apply; tamamı)
//
// Abonelik ekranlarındaki "ücretsiz dene" düğmesinin metnini, kullanıcının o an gerçekten denemeye
// UYGUN olup olmadığına göre seçer. Deneme günü null ise (teklif yok ya da kullanıcı uygun değil)
// deneme vaat edilmez.
//
// Android'den bilinçli fark: TextView'a yazmak yerine metin anahtarını (strings.xml adı) ve biçim
// argümanını döndürür; metni ekran çözer (Android getString(id, arg) karşılığı).
package com.sorobit.shared.oyun

/**
 * @property textKey strings.xml anahtarı: sub_cta_trial_weeks / sub_cta_trial_days /
 *   sub_cta_no_trial_pro / sub_cta_no_trial_lite.
 * @property formatArg `%1$d` argümanı (hafta ya da gün sayısı); denemesiz metinlerde null.
 */
data class SubscriptionCtaLabel(
    val textKey: String,
    val formatArg: Int?,
)

object SubscriptionCta {

    const val TEXT_TRIAL_WEEKS = "sub_cta_trial_weeks"
    const val TEXT_TRIAL_DAYS = "sub_cta_trial_days"
    const val TEXT_NO_TRIAL_PRO = "sub_cta_no_trial_pro"
    const val TEXT_NO_TRIAL_LITE = "sub_cta_no_trial_lite"

    /**
     * SubscriptionCta.apply. [freeTrialDays]: BillingManager.freeTrialDays(productId) (iOS: StoreKit
     * tanıtım teklifi + uygunluk); [productId]: Android'de varsayılan BillingCatalog.SUB_PRO.
     */
    fun label(freeTrialDays: Int?, productId: String): SubscriptionCtaLabel {
        val days = freeTrialDays
            ?: return SubscriptionCtaLabel(
                textKey = if (productId == BillingCatalog.SUB_LITE) TEXT_NO_TRIAL_LITE else TEXT_NO_TRIAL_PRO,
                formatArg = null,
            )
        // 7, 14, 21… gün "1 hafta / 2 hafta" olarak daha doğal okunuyor.
        return if (days % 7 == 0) {
            SubscriptionCtaLabel(TEXT_TRIAL_WEEKS, days / 7)
        } else {
            SubscriptionCtaLabel(TEXT_TRIAL_DAYS, days)
        }
    }
}

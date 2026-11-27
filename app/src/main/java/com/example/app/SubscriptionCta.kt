package com.example.app

import android.widget.TextView

/**
 * Abonelik ekranlarındaki "ücretsiz dene" düğmesinin metnini, kullanıcının o an gerçekten
 * denemeye UYGUN olup olmadığına göre yazar.
 *
 * NEDEN
 *   Layout'lardaki metin sabitti ("₺0,00 ÖDEYEREK DENE"). Play, ücretsiz denemeyi Google
 *   hesabı başına bir kez veriyor; daha önce abone olmuş bir kullanıcı aynı düğmeye bastığında
 *   ilk gün ücretlendiriliyor. Sabit metin o kullanıcıya yanlış vaatte bulunuyordu.
 *
 *   [BillingManager.freeTrialDays] null dönüyorsa iki durumdan biri geçerlidir: teklif Play
 *   Console'da tanımlı değildir ya da bu kullanıcı uygun değildir. İkisinde de deneme vaat
 *   edilmemeli — davranış aynı olduğu için ayırt etmeye gerek yok.
 *
 * Ürün bilgisi Play'den henüz gelmemiş olabileceği için ekranların onResume'unda tekrar
 * çağrılmalıdır (fiyat yazımıyla aynı desen).
 */
object SubscriptionCta {

    fun apply(
        billing: BillingManager?,
        target: TextView?,
        productId: String = BillingCatalog.SUB_PRO,
    ) {
        val view = target ?: return
        val days = billing?.freeTrialDays(productId)
        if (days == null) {
            view.text = view.context.getString(
                if (productId == BillingCatalog.SUB_LITE) {
                    R.string.sub_cta_no_trial_lite
                } else {
                    R.string.sub_cta_no_trial_pro
                }
            )
            return
        }
        // 7, 14, 21… gün "1 hafta / 2 hafta" olarak daha doğal okunuyor.
        view.text = if (days % 7 == 0) {
            view.context.getString(R.string.sub_cta_trial_weeks, days / 7)
        } else {
            view.context.getString(R.string.sub_cta_trial_days, days)
        }
    }
}

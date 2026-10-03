package com.example.app

/**
 * Reklam atlama panelini ([AdSkipFragment]) test etmek için geliştirici anahtarı.
 *
 * ## Neden gerekli
 * Panel bilerek seyrek ([AdSkipPolicy]): dört reklamda bir, iki panel arasında en az iki saat,
 * günde en fazla iki. Paneli (ve içindeki maskot animasyonunu) denemek için saatlerce
 * reklam izlemek gerekiyordu.
 *
 * ## Nasıl kullanılır
 * [FORCE] `true` iken debug derlemesinde panel her uygun reklamdan sonra çıkar (sayaç, süre ve
 * gün kapıları atlanır). Çağıran akışın "bu reklamdan sonra panel çıkmasın" kararı
 * (`eligible = false`, ör. kupa yolu açılışı) yine geçerli. Test bitince `false`'a geri al.
 *
 * ## Release'e sızmaz
 * [everyAd] `BuildConfig.DEBUG` ile çarpılıyor; release APK'sinde hep false.
 */
object AdSkipDebug {

    /** Test etmek için elle `true` yapılır; depoda her zaman `false` durmalı. */
    private const val FORCE = false

    /** Panel her uygun reklamdan sonra mı çıksın. Release'de her zaman false. */
    val everyAd: Boolean
        get() = BuildConfig.DEBUG && FORCE
}

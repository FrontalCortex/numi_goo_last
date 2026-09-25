package com.example.app

/**
 * Öğretmene sorma tanıtımını ([AskQuestionOpenFragment]) test etmek için geliştirici anahtarı.
 *
 * ## Neden gerekli
 * Tanıtımın kapısı dört koşulun HEPSİNİ istiyor (bkz. `MainActivity.isAskQuestionPromoEligible`):
 * oturum açık, rol öğretmen değil, plan Pro/Premium DEĞİL ve cihaz hoş geldin kredisini henüz
 * tüketmemiş. Üstüne bir de sayaç var: her üçüncü LESSON türü ders dönüşünde açılıyor.
 *
 * Pro bir hesapla bu ekranı görmek imkânsız. Dördüncü koşul ise CİHAZ başına olduğu için yeni
 * bir hesap açmak da yetmiyor — aynı telefonda kredi bir kez tüketildiyse tanıtım yine çıkmaz.
 * Yani ekranı test etmenin kodu değiştirmeden bir yolu yok.
 *
 * ## Nasıl kullanılır
 * [FORCE] değerini `true` yap, debug derlemesini çalıştır. Plan ve cihaz kredisi kapıları
 * atlanır, sayaç eşiği 1'e iner: türü LESSON olan HER ders dönüşünde tanıtım açılır.
 * Test bitince `false`'a geri al.
 *
 * ## Release'e sızmaz
 * [forceShow] `BuildConfig.DEBUG` ile çarpılıyor. O derleme zamanı sabiti olduğu için release
 * APK'sinde koşul hep false ve kapının davranışı hiç değişmiyor — [FORCE] yanlışlıkla `true`
 * bırakılsa bile.
 */
object AskQuestionPromoDebug {

    /** Test etmek için elle `true` yapılır; depoda her zaman `false` durmalı. */
    private const val FORCE = false

    /** Kapılar atlanacak mı. Release'de her zaman false. */
    val forceShow: Boolean
        get() = BuildConfig.DEBUG && FORCE
}

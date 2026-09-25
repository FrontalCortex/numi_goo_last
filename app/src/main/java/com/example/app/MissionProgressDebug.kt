package com.example.app

/**
 * Görev ilerlemesini her uygulama açılışında sıfırlayan geliştirici anahtarı.
 *
 * ## Neden gerekli
 * Ders sonundaki görev ödülü paneli ([MissionChestRewardFragment]) yalnızca o derste bir görev
 * TAMAMLANDIĞINDA açılıyor. Günün/haftanın görevleri bir kez bittiyse panel bir daha hiç
 * çıkmıyor ve test edilemiyor.
 *
 * Firestore'dan silmek de çözmüyor: ilerleme önce yerel önbellekten
 * ([MissionsProgressStore] SharedPreferences) okunuyor, bulut yalnızca yerel boşken devreye
 * giriyor. Sadece yerelden silmek de yetmiyor — bir sonraki okumada buluttaki eski değerler
 * geri hydrate ediliyor. İkisini birden temizlemek gerekiyor.
 *
 * ## Nasıl kullanılır
 * [RESET_ON_LAUNCH] değerini `true` yap, debug derlemesini çalıştır. Her açılışta günlük ve
 * haftalık sayaçlar sıfırlanıyor, görev kombinasyonu yeniden seçiliyor ve sıfırlanmış durum
 * buluta yazılıyor. Bir ders bitir: görev tamamlanınca panel açılır. Test bitince `false`'a
 * geri al.
 *
 * ## Dikkat
 * Anahtar açıkken gerçek ilerleme her açılışta gidiyor — bu bir test aracı, geliştirme
 * hesabında kullan.
 *
 * ## Release'e sızmaz
 * [resetOnLaunch] `BuildConfig.DEBUG` ile çarpılıyor. O derleme zamanı sabiti olduğu için
 * release APK'sinde koşul hep false ve blok hiç çalışmıyor — [RESET_ON_LAUNCH] yanlışlıkla
 * `true` bırakılsa bile.
 */
object MissionProgressDebug {

    /** Test etmek için elle `true` yapılır; depoda her zaman `false` durmalı. */
    private const val RESET_ON_LAUNCH = false

    /** Açılışta sıfırlama yapılacak mı. Release'de her zaman false. */
    val resetOnLaunch: Boolean
        get() = BuildConfig.DEBUG && RESET_ON_LAUNCH
}

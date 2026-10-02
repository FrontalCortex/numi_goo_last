package com.example.app

/**
 * Yeni seri sorusunu ([NewStreakFragment]) test etmek için geliştirici anahtarı.
 *
 * ## Neden gerekli
 * Sorunun iki koşulu var (bkz. [StreakRepository.needsNewStreakPrompt]): seri 0 olmalı ve o
 * gün henüz sorulmamış olmalı. İkisini birden elde etmek zor:
 *  - Yeni açılan hesapta seri 0 ama kayıt akışı soruyu o gün için "soruldu" diye işaretliyor.
 *  - Serisi olan hesapta sorunun çıkması için serinin kırılması, yani en az bir gün
 *    beklemek gerekiyor; çıktıktan sonra da aynı gün bir daha çıkmıyor.
 *  - Yerel durumu elle sıfırlamak işe yaramıyor: sunucudaki seri bir sonraki açılışta geri
 *    yükleniyor.
 * Yani ekranı — ve onu açan dört ayrı yolu (haritadaki ders dönüşü, Görevler'deki kupa testi
 * dönüşü, Görevler'deki günlük soru dönüşü, yarış panelindeki ders dönüşü) — günde en fazla
 * bir kez, o da ancak seri kırıkken denemek mümkün.
 *
 * ## Nasıl kullanılır
 * [FORCE] değerini `true` yap, debug derlemesini çalıştır. İki koşul da atlanır: haritadaki
 * ve yarış panelindeki her ders dönüşünde (bitirme, başarısızlık, yarıda bırakma), her kupa
 * testi kapanışında ve her günlük soru kapanışında soru sorulur. Test bitince `false`'a
 * geri al.
 *
 * Ekranda "Başla"ya basmak gerçek bir seçim kaydeder (meydan okuma yerelde değişir ve
 * sunucuya gönderilir); yalnızca açılıp açılmadığına bakıyorsan geri tuşuyla kapat.
 *
 * ## Release'e sızmaz
 * [forceShow] `BuildConfig.DEBUG` ile çarpılıyor. O derleme zamanı sabiti olduğu için release
 * APK'sinde koşul hep false ve sorunun davranışı hiç değişmiyor — [FORCE] yanlışlıkla `true`
 * bırakılsa bile.
 */
object NewStreakPromptDebug {

    /** Test etmek için elle `true` yapılır; depoda her zaman `false` durmalı. */
    private const val FORCE = false

    /** Koşullar atlanacak mı. Release'de her zaman false. */
    val forceShow: Boolean
        get() = BuildConfig.DEBUG && FORCE
}

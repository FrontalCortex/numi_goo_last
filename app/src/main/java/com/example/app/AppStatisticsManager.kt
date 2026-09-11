package com.example.app

/**
 * Kullanıcının "bizi nereden duydunuz" cevabını Firebase Analytics'e bildirir.
 *
 * ## Taşıma notu (önceki mimari)
 * Bu değer eskiden `appStatistics/acquisition_sources` dokümanında `FieldValue.increment(1)`
 * ile tutulan bir sayaçtı. Güvenlik kuralı zaten `allow read: false` diyordu — yani veri
 * tasarım gereği uygulamaya hiç geri dönmüyordu, tanımı gereği ölçüm verisiydi.
 *
 * Analytics iki sebeple daha iyi:
 * - Sayaç manipüle edilebiliyordu (kural giriş yapmış herkese yazma izni veriyordu).
 * - Tek bir toplam sayı, kaynağı kullanıcının geri kalan yolculuğuna bağlayamıyordu.
 *   Kullanıcı özelliği olarak yazıldığında "Instagram'dan gelenlerin D7 retention'ı"
 *   gibi sorular cevaplanabilir hâle geliyor.
 *
 * Kaynak değeri kullanıcının SERBEST YAZDIĞI bir metin değil, sabit bir listeden seçilen
 * bir etikettir (bkz. [UserInfoFragment]); bu yüzden Analytics'e gönderilmesi güvenlidir.
 */
object AppStatisticsManager {

    /**
     * Kullanıcının seçtiği edinim kaynağını kaydeder.
     *
     * İmza korundu (nullable [String]) — üç çağrı noktası değişmedi:
     * [UserInfoFragment], [com.example.app.auth.AuthManager], [OtpVerificationFragment].
     */
    fun incrementAcquisitionSource(source: String?) {
        if (source.isNullOrBlank()) return
        AnalyticsLogger.logAcquisitionSource(source)
    }
}

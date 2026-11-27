package com.example.app

/**
 * Kullanıcının Pro akışına hangi kapıdan girdiğini akış boyunca taşır.
 *
 * ## Neden gerekli
 * [ProDiffirentFragment] dört ayrı yerden açılıyor: reklam sonrası paneli, "öğretmene sor"
 * tanıtımı ve mağazadaki iki düğme. Huni adımları ekran adıyla eşlendiği için bu dördü tek
 * havuzda toplanıyordu; "Pro panelini 80 kişi gördü" cümlesinden kaçının reklamdan geldiği
 * çıkarılamıyordu. Yani "reklam sonrası panel işe yarıyor mu" sorusu ölçülüyor gibi görünüp
 * aslında ölçülmüyordu.
 *
 * Kapı bilgisi panelin açılışında buraya yazılıyor ve satın alma olaylarına kadar taşınıyor;
 * böylece "hangi kapı satın almaya dönüşüyor" tek tabloda cevaplanabiliyor.
 *
 * ## Neden yalnızca bellekte
 * Akış tek oturum içinde bitiyor: panel açılır, plan seçilir, Play penceresi kapanır. Süreç
 * arada öldürülürse kapı bilgisi kaybolur ve olay [AnalyticsLogger.PRO_ENTRY_UNKNOWN] ile
 * gider — kalıcı saklamanın maliyetine değmeyecek kadar seyrek bir durum, ve yanlış kapıya
 * yazmaktansa "bilinmiyor" demek doğru.
 */
object ProFlow {

    @Volatile
    private var current: String = AnalyticsLogger.PRO_ENTRY_UNKNOWN

    /** Pro paneli açılırken çağrılır; sonraki satın alma olayları bu kapıyı taşır. */
    fun start(entryPoint: String) {
        current = entryPoint
    }

    /** Akışın başladığı kapı; hiç başlamadıysa [AnalyticsLogger.PRO_ENTRY_UNKNOWN]. */
    fun entryPoint(): String = current
}

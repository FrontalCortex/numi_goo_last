package com.example.app

/**
 * [TrustedClock]'un hesabı; Android'e dokunmadığı için birim testiyle sabitlenebiliyor
 * (`TrustedClockRulesTest`).
 */
object TrustedClockRules {

    /**
     * Sunucudan öğrenilen bir an ve o anda cihazın MONOTON saatinin gösterdiği değer.
     *
     * @param serverNowMs Sunucunun bildirdiği "şimdi" (Unix ms).
     * @param elapsedMs Cevap geldiğinde `SystemClock.elapsedRealtime()`.
     * @param bootCount Cevap geldiğinde cihazın açılış sayacı; monoton saat her yeniden
     *   başlatmada sıfırlandığı için çapanın hangi açılışa ait olduğu bilinmeli.
     */
    data class Anchor(val serverNowMs: Long, val elapsedMs: Long, val bootCount: Int)

    /**
     * Cihazın duvar saati elle değiştirilse de kaymayan "şimdi".
     *
     * - Çapa bu açılışa aitse: sunucu anı + o zamandan beri GERÇEKTEN geçen süre. Duvar
     *   saatine hiç bakılmıyor; ileri de alınsa geri de alınsa sonuç aynı.
     * - Çapa eski bir açılıştan kalmışsa: monoton saat sıfırlandığı için aradan ne kadar
     *   geçtiği bilinemez. Duvar saatine dönülüyor ama çapadaki andan geriye gidilmiyor.
     * - Çapa hiç yoksa: düz duvar saati (eski davranış).
     */
    fun trustedNow(anchor: Anchor?, wallNowMs: Long, elapsedNowMs: Long, bootCount: Int): Long {
        if (anchor == null) return wallNowMs
        val sameBoot = anchor.bootCount == bootCount && elapsedNowMs >= anchor.elapsedMs
        if (sameBoot) return anchor.serverNowMs + (elapsedNowMs - anchor.elapsedMs)
        return maxOf(wallNowMs, anchor.serverNowMs)
    }
}

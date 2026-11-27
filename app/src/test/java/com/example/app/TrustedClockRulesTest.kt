package com.example.app

import com.example.app.TrustedClockRules.Anchor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Güvenilir saat kuralı: cihazın duvar saati elle değiştirilse de "şimdi" kaymamalı.
 *
 * Çalıştırma: `.\gradlew testDebugUnitTest --tests "*TrustedClockRulesTest"`
 */
class TrustedClockRulesTest {

    private companion object {
        const val SERVER = 1_790_000_000_000L
        const val HOUR = 60L * 60 * 1000
        const val MIN = 60L * 1000
        const val BOOT = 7

        /** Sunucu SERVER dediğinde cihaz 5 saattir açıktı. */
        val ANCHOR = Anchor(serverNowMs = SERVER, elapsedMs = 5 * HOUR, bootCount = BOOT)
    }

    private fun now(anchor: Anchor?, wall: Long, elapsed: Long, boot: Int = BOOT) =
        TrustedClockRules.trustedNow(anchor, wall, elapsed, boot)

    @Test
    fun capaYokkenDuvarSaati() {
        assertEquals(SERVER + 3 * MIN, now(null, wall = SERVER + 3 * MIN, elapsed = 9 * HOUR))
    }

    @Test
    fun ayniAcilistaGecenGercekSureEklenir() {
        // 10 dakika geçti, duvar saati de doğru: sonuç sunucu anı + 10 dk.
        assertEquals(
            SERVER + 10 * MIN,
            now(ANCHOR, wall = SERVER + 10 * MIN, elapsed = 5 * HOUR + 10 * MIN),
        )
    }

    @Test
    fun duvarSaatiIleriAlinincaSonucDegismez() {
        // Asıl senaryo: çocuk saati 2 gün ileri aldı ama gerçekte 1 dakika geçti.
        assertEquals(
            SERVER + 1 * MIN,
            now(ANCHOR, wall = SERVER + 48 * HOUR, elapsed = 5 * HOUR + 1 * MIN),
        )
    }

    @Test
    fun duvarSaatiGeriAlinincaSonucDegismez() {
        assertEquals(
            SERVER + 1 * MIN,
            now(ANCHOR, wall = SERVER - 48 * HOUR, elapsed = 5 * HOUR + 1 * MIN),
        )
    }

    @Test
    fun yenidenBaslatmadaDuvarSaatineDonulur() {
        // Açılış sayacı değişti: monoton saat sıfırlandı, aradan ne kadar geçtiği bilinemez.
        assertEquals(
            SERVER + 3 * HOUR,
            now(ANCHOR, wall = SERVER + 3 * HOUR, elapsed = 2 * MIN, boot = BOOT + 1),
        )
    }

    @Test
    fun yenidenBaslatmadaCapadanGeriyeGidilmez() {
        // Yeniden başlatıldı ve duvar saati çapadan ÖNCEYİ gösteriyor: en az çapa anı.
        assertEquals(
            SERVER,
            now(ANCHOR, wall = SERVER - 3 * HOUR, elapsed = 2 * MIN, boot = BOOT + 1),
        )
    }

    @Test
    fun acilisiBilinmeyenCapaYalnizcaAltSinirdir() {
        // Diskten yüklenen ama hangi açılışa ait olduğu kanıtlanamayan çapa (-1): monoton
        // saat çapadakinden büyük olsa bile "aynı açılış" sayılmaz. Sayılsaydı sonuç
        // SERVER + 1 saat olur, yani gerçeğin (duvar saati: +9 saat) gerisinde kalırdı.
        val anchor = Anchor(serverNowMs = SERVER, elapsedMs = 5 * HOUR, bootCount = -1)
        assertEquals(
            SERVER + 9 * HOUR,
            now(anchor, wall = SERVER + 9 * HOUR, elapsed = 6 * HOUR, boot = 0),
        )
    }

    @Test
    fun acilisSayaciOkunamazsaMonotonSaatinGeriGitmesiYeniAcilisSayilir() {
        // Sayaç iki tarafta da 0 (okunamadı); monoton saat çapadakinden küçükse cihaz
        // yeniden başlatılmıştır.
        val anchor = Anchor(serverNowMs = SERVER, elapsedMs = 5 * HOUR, bootCount = 0)
        assertEquals(
            SERVER + 3 * HOUR,
            now(anchor, wall = SERVER + 3 * HOUR, elapsed = 2 * MIN, boot = 0),
        )
    }
}

package com.sorobit.shared.profil

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// iOS paketine kopyalanan gerçek parça dosyası (ios/iosApp/iosApp/Profil/Avatar/personas_parts.json)
// okunur; Android'in kaynağıyla (app/src/main/assets/avatar/personas_parts.json) aynı olmalı.
class AvatarPaketDosyasiTest {

    private fun dosya(yol: String): File? {
        // Gradle testi ios/shared içinde çalıştırır; başka dizinden çalıştırılırsa birkaç üst klasöre bakılır.
        var kok: File? = File("").absoluteFile
        repeat(4) {
            val f = File(kok, yol)
            if (f.isFile) return f
            kok = kok?.parentFile
        }
        return null
    }

    private val iosDosyasi: File? = dosya("iosApp/iosApp/Profil/Avatar/personas_parts.json")

    @Test
    fun androidKopyasiylaAyni() {
        val ios = iosDosyasi ?: return
        val android = dosya("app/src/main/assets/avatar/personas_parts.json") ?: return
        assertEquals(android.readText(), ios.readText())
    }

    @Test
    fun gercekPaketOkunurVeHerSecenekCizilir() {
        val f = iosDosyasi ?: error("personas_parts.json bulunamadı")
        val paket = AvatarParts.parse(f.readText())
        assertFalse(paket.isEmpty)
        assertEquals(
            setOf("body", "eyes", "facialHair", "hair", "mouth", "nose", "root"),
            paket.groupNames().toSet(),
        )
        assertEquals(20, paket.names("hair").size)
        assertEquals("long", paket.names("hair").first())

        val varsayilan = AvatarRules.defaultEncoded()
        for (tab in AvatarStyle.PERSONAS.tabs) {
            val secenekler = AvatarRules.options(paket, varsayilan, tab)
            assertTrue(secenekler.isNotEmpty(), tab.key)
            for (deger in secenekler) {
                val c = AvatarRules.withTabValue(varsayilan, tab, deger)
                for (daire in listOf(true, false)) {
                    for (yakin in listOf(true, false)) {
                        val svg = AvatarRules.svg(paket, c, daire, yakin)
                        assertFalse("{{" in svg, "${tab.key}=$deger: doldurulmamış yer tutucu")
                        assertTrue(svg.startsWith("<svg "))
                        assertTrue(svg.endsWith("</svg>"))
                    }
                }
            }
        }
    }
}

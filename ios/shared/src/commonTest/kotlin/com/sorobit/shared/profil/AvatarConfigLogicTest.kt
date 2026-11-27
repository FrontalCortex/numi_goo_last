package com.sorobit.shared.profil

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Beklenen değerler Android kaynağı (AvatarConfig.kt, AvatarArt.kt, AvatarCustomFragment.kt,
// ProfileFragment.applyHeaderColor) okunarak elle türetildi.
class AvatarConfigLogicTest {

    private val R = AvatarRules

    private val varsayilan =
        "personas|backgroundColor=b6e3f4;body=rounded;clothingColor=f55d81;eyes=open;facialHair=;" +
            "hair=bobBangs;hairColor=e15c66;mouth=smile;nose=smallRound;skinColor=e5a07e"

    private val ornekPaket = AvatarParts.parse(
        """
        {
          "root": {"default": "<a fill=\"{{c:skin}}\"/><g>{{p:eyes}}</g><g>{{p:facialHair}}</g><g>{{p:hair}}</g>"},
          "eyes": {"open": "<e fill=\"{{c:hair}}\"/>", "wink": "<w/>"},
          "facialHair": {"walrus": "<f fill=\"{{c:clothing}}\"/>", "goatee": "<g2/>"},
          "hair": {"long": "<h A=\"\/\"/>", "bobBangs": "<b fill=\"{{c:hair}}\"/>"}
        }
        """.trimIndent(),
    )

    // ---- Biçim ----

    @Test
    fun varsayilanKodlama() {
        assertEquals(varsayilan, AvatarConfig.default(AvatarStyle.PERSONAS).encode())
        assertEquals(varsayilan, R.defaultEncoded())
    }

    @Test
    fun cozmeVeYenidenKodlama() {
        val c = AvatarConfig.decode("personas|hair=long;eyes=wink;bilinmeyen=x;bozuk;=y")
        assertNotNull(c)
        assertEquals(AvatarStyle.PERSONAS, c.style)
        assertEquals("long", c.values["hair"])
        assertEquals("wink", c.values["eyes"])
        // Bilinmeyen anahtar atlanır, eksikler varsayılan.
        assertNull(c.values["bilinmeyen"])
        assertEquals("smile", c.values["mouth"])
        assertEquals(10, c.values.size)
    }

    @Test
    fun bilinmeyenStilPersonasaDuser() {
        val c = AvatarConfig.decode("avataaars|hair=long")
        assertNotNull(c)
        assertEquals(AvatarStyle.PERSONAS, c.style)
        assertEquals("long", c.values["hair"])
    }

    @Test
    fun okunamayanDizge() {
        assertNull(AvatarConfig.decode(null))
        assertNull(AvatarConfig.decode(""))
        assertNull(AvatarConfig.decode("   "))
        assertNull(AvatarConfig.decode("personas"))
        assertFalse(R.hasAvatar(""))
        assertTrue(R.hasAvatar("personas|"))
        assertEquals(varsayilan, R.normalized("bozuk"))
        assertEquals(varsayilan, R.normalized("personas|"))
    }

    @Test
    fun esittirIcerenDeger() {
        // Android: ilk '=' ayırır, değerin geri kalanı olduğu gibi kalır.
        val c = AvatarConfig.decode("personas|hair=a=b")
        assertEquals("a=b", c?.values?.get("hair"))
    }

    // ---- Sekmeler ----

    @Test
    fun sekmeler() {
        val tabs = AvatarStyle.PERSONAS.tabs
        assertEquals(
            listOf(
                "hair", "hairColor", "skinColor", "eyes", "nose", "mouth", "facialHair", "body", "clothingColor",
                "backgroundColor",
            ),
            tabs.map { it.key },
        )
        assertEquals(listOf("ui_sac", "ui_sac_rengi", null, "ui_goz", null, "ui_agiz", null, "ui_kiyafet", null, null), tabs.map { it.labelRes })
        assertEquals("Ten", tabs[2].label)
        assertTrue(tabs[6].optional)
        assertTrue(tabs[3].closeUp)
        assertFalse(tabs[0].closeUp)
        assertTrue(tabs[9].swatch)
        assertEquals(9, tabs[9].palette.size)
        assertEquals("262e33", tabs[9].palette.last())
    }

    @Test
    fun secenekler() {
        val tabs = AvatarStyle.PERSONAS.tabs
        assertEquals(listOf("long", "bobBangs"), R.options(ornekPaket, AvatarStyle.PERSONAS, tabs[0]))
        // İsteğe bağlı parçada başta "Yok".
        assertEquals(listOf("", "walrus", "goatee"), R.options(ornekPaket, AvatarStyle.PERSONAS, tabs[6]))
        // Renk sekmesi paleti verir.
        assertEquals(tabs[1].palette, R.options(ornekPaket, varsayilan, tabs[1]))
        // Pakette olmayan grup boş.
        assertEquals(emptyList(), R.options(ornekPaket, AvatarStyle.PERSONAS, tabs[4]))
    }

    @Test
    fun sekmeDegeri() {
        val sac = AvatarStyle.PERSONAS.tabs[0]
        assertEquals("bobBangs", R.currentValue(varsayilan, sac))
        val yeni = R.withTabValue(varsayilan, sac, "long")
        assertEquals("long", R.value(yeni, "hair"))
        assertEquals("long", R.currentValue(yeni, sac))
        assertEquals("", R.value(varsayilan, "facialHair"))
    }

    @Test
    fun gerekliAlanliSekme() {
        val desen = AvatarTab.part(null, "Desen", "pattern", requires = "body" to "squared")
        // Gerekli alan başka değerdeyken hiçbiri seçili değil.
        assertEquals(AvatarRules.NO_SELECTION, R.currentValue(varsayilan, desen))
        // Seçim gerekli alanı da değiştirir (pattern anahtarı stilde olmadığı için kayıtta kalmaz).
        val yeni = R.withTabValue(varsayilan, desen, "x")
        assertEquals("squared", R.value(yeni, "body"))
    }

    @Test
    fun degisenParcaSayisi() {
        assertEquals(0, R.changedPartCount(null, varsayilan))
        assertEquals(0, R.changedPartCount(varsayilan, varsayilan))
        val iki = R.withTabValue(R.withTabValue(varsayilan, AvatarStyle.PERSONAS.tabs[0], "long"), AvatarStyle.PERSONAS.tabs[3], "wink")
        assertEquals(2, R.changedPartCount(varsayilan, iki))
    }

    // ---- Rastgele ----

    @Test
    fun rastgeleYalnizcaSeceneklerdenSecer() {
        val rng = Random(42)
        repeat(200) {
            val c = AvatarConfig.random(ornekPaket, AvatarStyle.PERSONAS, rng)
            assertTrue(c.values["hair"] in listOf("long", "bobBangs"))
            assertTrue(c.values["facialHair"] in listOf("", "walrus", "goatee"))
            assertTrue(c.values["hairColor"] in AvatarStyle.PERSONAS.tabs[1].palette)
            // Pakette olmayan grupta varsayılan kalır.
            assertEquals("smallRound", c.values["nose"])
            assertEquals(10, c.values.size)
        }
    }

    @Test
    fun rastgeleSakalCogunluklaYok() {
        val rng = Random(7)
        var yok = 0
        val n = 2000
        repeat(n) {
            if (AvatarConfig.random(ornekPaket, AvatarStyle.PERSONAS, rng).values["facialHair"] == "") yok++
        }
        // %70 doğrudan "yok" + %30'un üçte biri ("" seçeneği) ≈ %80.
        assertTrue(yok in 1450..1750, "yok=$yok")
    }

    // ---- SVG ----

    @Test
    fun svgDairesiz() {
        val svg = R.svg(ornekPaket, varsayilan, circle = false, closeUp = false)
        assertEquals(
            """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0.0 0.0 64.0 64.0" fill="none">""" +
                """<a fill="#e5a07e"/><g><e fill="#e15c66"/></g><g></g><g><b fill="#e15c66"/></g></svg>""",
            svg,
        )
    }

    @Test
    fun svgDaireliVeYakinPlan() {
        val c = "personas|hair=long;facialHair=walrus;backgroundColor=65c9ff;clothingColor=456dff"
        val svg = R.svg(ornekPaket, c, circle = true, closeUp = true)
        assertEquals(
            """<svg xmlns="http://www.w3.org/2000/svg" viewBox="15.0 17.0 34.0 32.0" fill="none">""" +
                """<clipPath id="avatarCircle"><circle cx="32" cy="32" r="32"/></clipPath>""" +
                """<circle cx="32" cy="32" r="32" fill="#65c9ff"/>""" +
                """<g clip-path="url(#avatarCircle)"><a fill="#e5a07e"/><g><e fill="#e15c66"/></g>""" +
                """<g><f fill="#456dff"/></g><g><h A="/"/></g></g></svg>""",
            svg,
        )
    }

    @Test
    fun bozukRenkVarsayilanaDuserBilinmeyenParcaCizilmez() {
        val c = "personas|hairColor=zzzzzz;skinColor=\"/><x/>;eyes=yok;backgroundColor=12345"
        val svg = R.svg(ornekPaket, c, circle = true, closeUp = false)
        assertTrue("fill=\"#e15c66\"" in svg)
        assertTrue("fill=\"#e5a07e\"" in svg)
        assertTrue("<x/>" !in svg)
        assertTrue("fill=\"#b6e3f4\"" in svg)
        assertTrue("<e " !in svg)
    }

    @Test
    fun bozukJsonBosPaket() {
        assertTrue(AvatarParts.parse("{").isEmpty)
        assertTrue(AvatarParts.parse("[]").isEmpty)
        assertTrue(AvatarParts.parse("{\"a\": {\"b\": 1}}").isEmpty)
        assertTrue(AvatarParts.parse("{}").isEmpty)
        assertFalse(ornekPaket.isEmpty)
        assertEquals(listOf("root", "eyes", "facialHair", "hair"), ornekPaket.groupNames())
    }

    // ---- Uzak değer ----

    @Test
    fun uzakDegerYereleAlinir() {
        assertNull(R.remoteToApply(varsayilan, null))
        assertNull(R.remoteToApply(varsayilan, "bozuk"))
        assertNull(R.remoteToApply(varsayilan, varsayilan))
        assertEquals(varsayilan, R.remoteToApply(null, varsayilan))
        // Uzaktaki dizge yeniden kodlanır (anahtar sırası).
        val sirasiz = "personas|skinColor=e5a07e;hair=bobBangs;backgroundColor=b6e3f4;body=rounded;clothingColor=f55d81;" +
            "eyes=open;facialHair=;hairColor=e15c66;mouth=smile;nose=smallRound"
        assertNull(R.remoteToApply(varsayilan, sirasiz))
    }

    // ---- Profil üst alanı ----

    @Test
    fun ustAlanRengi() {
        assertEquals("FFB6E3F4", R.headerColorArgb(varsayilan))
        assertNull(R.headerColorArgb(null))
        assertNull(R.headerColorArgb("personas|backgroundColor=12345"))
        assertNull(R.headerColorArgb("personas|backgroundColor=gggggg"))
        assertEquals("80112233", R.headerColorArgb("personas|backgroundColor=80112233"))
    }

    @Test
    fun ustAlanYaziRengi() {
        // Pastel zeminler açık → koyu yazı; 262e33 koyu → beyaz.
        assertEquals(ProfileHeaderColors.DARK_TEXT, ProfileHeaderColors.foreground("FFB6E3F4"))
        assertEquals(ProfileHeaderColors.DARK_TEXT, ProfileHeaderColors.foreground("FFFFFFB1"))
        assertEquals(ProfileHeaderColors.WHITE, ProfileHeaderColors.foreground("FF262E33"))
        // 65c9ff: göreli parlaklık ≈ 0,51 → açık.
        assertEquals(ProfileHeaderColors.DARK_TEXT, ProfileHeaderColors.foreground("FF65C9FF"))
        assertEquals(ProfileHeaderColors.WHITE, ProfileHeaderColors.foreground("FF141F23"))
        assertEquals(1.0, ProfileHeaderColors.luminance("FFFFFFFF"))
        assertEquals(0.0, ProfileHeaderColors.luminance("FF000000"))
    }

    @Test
    fun altCizgiRengi() {
        // 0xB6*0.85 = 154.7 → 154 (9A); 0xE3*0.85 = 192.95 → 192 (C0); 0xF4*0.85 = 207.4 → 207 (CF).
        assertEquals("FF9AC0CF", ProfileHeaderColors.divider("FFB6E3F4"))
        assertEquals("FF000000", ProfileHeaderColors.divider("FF000000"))
        assertEquals("FF7F7F7F", ProfileHeaderColors.blend("FFFFFFFF", "FF000000", 0.5f))
        assertEquals("FF000000", ProfileHeaderColors.divider("bozuk"))
    }
}

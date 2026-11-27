// Kaynak (Android): app/src/main/java/com/example/app/
//   AvatarConfig.kt         — AvatarConfig (encode / decode / default / random), AvatarStore.FIRESTORE_FIELD,
//                             AvatarStore.applyRemote kararı
//   AvatarArt.kt            — AvatarStyle (PERSONAS: tuval, yüz yakın planı, sekmeler, varsayılanlar),
//                             AvatarTab, AvatarArt.options, AvatarArt.buildSvg (yer tutucuların doldurulması)
//   AvatarCustomFragment.kt — currentValue / withValue (sekme seçimi), changedPartCount, GRID_COLUMNS,
//                             CHIP_IDLE_COLOR
//   ProfileFragment.kt      — showAvatar / applyHeaderColor: avatar alanının rengi, açık zeminde koyu yazı
//                             (ColorUtils.calculateLuminance > 0.5), alt çizgi (ColorUtils.blendARGB %15 siyah)
// Parça verisi: app/src/main/assets/avatar/personas_parts.json (iOS kopyası Profil/Avatar/ altında).
//
// Android'den farklar (yalnızca biçim; davranış aynı):
// - Android'de AvatarTab bir `sealed class` (Part / Color); Swift'ten kolay kullanılsın diye burada tek sınıf
//   ve [AvatarTab.isColor] bayrağı var.
// - Sekme etiketleri Android'de `appString(R.string.x)` ya da koda gömülü Türkçe metin; burada string adı
//   ([AvatarTab.labelRes]) ve Türkçe karşılığı ([AvatarTab.label]) birlikte tutuluyor.
// - Parça paketi Android'de `context.assets`'ten okunuyor; burada JSON metni [AvatarParts.parse]'a veriliyor
//   (okuma platformun işi). Seçenek sırası paketteki sırayla aynı.
// - `AvatarConfig.random` rastgele kaynağını parametre alıyor (test için); Android `Math.random()` ve
//   `List.random()` kullanıyor, olasılıklar aynı.
// - Android cihazda avatarın bir kopyasını SharedPreferences'ta tutuyor (`avatar_prefs`, kullanıcıya göre);
//   bu depolama iOS'ta platformun işi, yalnızca uzak değerin yerele alınma kararı ([AvatarRules.remoteToApply])
//   burada.
package com.sorobit.shared.profil

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/** Android `RectF` karşılığı (avatar tuvali ve yakın plan için). */
data class AvatarRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun width(): Float = right - left
    fun height(): Float = bottom - top
}

/**
 * Düzenleme ekranındaki bir sekme. [key] aynı zamanda [AvatarConfig.values] anahtarı.
 *
 * Parça sekmesinde ([isColor] false) [key] paketteki grup adıdır; [optional] ise başta "Yok" (boş metin)
 * seçeneği vardır, [closeUp] ise küçük resim yüze yaklaşır, [requiresKey]/[requiresValue] verilmişse bu
 * sekmede seçim yapılınca o alan da bu değere geçer. Renk sekmesinde seçenekler [palette]'tir; [swatch]
 * ise kutuda avatar yerine yalnızca renk dairesi çizilir.
 */
class AvatarTab private constructor(
    /** Android'de etiket `appString(R.string.x)` ise string adı; koda gömülü metinse null. */
    val labelRes: String?,
    /** Etiketin Türkçe metni (Android'deki gömülü metin ya da [labelRes]'in Türkçe değeri). */
    val label: String,
    val key: String,
    val isColor: Boolean,
    val optional: Boolean,
    val closeUp: Boolean,
    val requiresKey: String?,
    val requiresValue: String?,
    val palette: List<String>,
    val swatch: Boolean,
) {
    companion object {
        /** Android: AvatarTab.Part. */
        fun part(
            labelRes: String?,
            label: String,
            group: String,
            optional: Boolean = false,
            closeUp: Boolean = false,
            requires: Pair<String, String>? = null,
        ): AvatarTab = AvatarTab(
            labelRes, label, group, false, optional, closeUp, requires?.first, requires?.second, emptyList(), false,
        )

        /** Android: AvatarTab.Color. */
        fun color(labelRes: String?, label: String, key: String, palette: List<String>, swatch: Boolean = false): AvatarTab =
            AvatarTab(labelRes, label, key, true, false, false, null, null, palette, swatch)
    }
}

/**
 * Avatar stilleri. KAYNAK: DiceBear 9.4.2 "personas" paketi — Draftbit'in Personas'ı, CC BY 4.0;
 * uygulamada "Personas by Draftbit" diye kaynak gösterilmesi ŞART (Android: CreditsFragment).
 */
enum class AvatarStyle(
    val key: String,
    val label: String,
    /** Android assets yolu; iOS paketinde dosya `personas_parts.json` adıyla kökte. */
    val asset: String,
    /** Çizimin tuvali (viewBox). */
    val canvas: AvatarRect,
    /** Kaş, göz ve ağzı kapsayan yakın plan; bu sekmelerin küçük resimleri için. */
    val faceCloseUp: AvatarRect,
    val tabs: List<AvatarTab>,
    val defaults: Map<String, String>,
) {
    PERSONAS(
        key = "personas",
        label = "Personas",
        asset = "avatar/personas_parts.json",
        canvas = AvatarRect(0f, 0f, 64f, 64f),
        faceCloseUp = AvatarRect(15f, 17f, 49f, 49f),
        tabs = listOf(
            AvatarTab.part("ui_sac", "Saç", "hair"),
            AvatarTab.color(
                "ui_sac_rengi", "Saç rengi", "hairColor",
                listOf("362c47", "6c4545", "e15c66", "e16381", "f27d65", "f29c65", "dee1f5"),
            ),
            AvatarTab.color(
                null, "Ten", "skinColor",
                listOf("eeb4a4", "e7a391", "e5a07e", "d78774", "b16a5b", "92594b", "623d36"),
            ),
            AvatarTab.part("ui_goz", "Göz", "eyes", closeUp = true),
            AvatarTab.part(null, "Burun", "nose", closeUp = true),
            AvatarTab.part("ui_agiz", "Ağız", "mouth", closeUp = true),
            AvatarTab.part(null, "Sakal", "facialHair", optional = true, closeUp = true),
            AvatarTab.part("ui_kiyafet", "Kıyafet", "body"),
            AvatarTab.color(
                null, "Renk", "clothingColor",
                listOf("456dff", "54d7c7", "7555ca", "6dbb58", "e24553", "f3b63a", "f55d81"),
            ),
            // Arka plan renkleri (DiceBear sitesindeki pastel zeminler; Android: BACKGROUNDS).
            AvatarTab.color(
                null, "Arka plan", "backgroundColor",
                listOf("b6e3f4", "65c9ff", "c0aede", "d1d4f9", "ffd5dc", "ffdfbf", "a7ffc4", "ffffb1", "262e33"),
                swatch = true,
            ),
        ),
        defaults = mapOf(
            "hair" to "bobBangs", "hairColor" to "e15c66", "skinColor" to "e5a07e", "eyes" to "open",
            "nose" to "smallRound", "mouth" to "smile", "facialHair" to "", "body" to "rounded",
            "clothingColor" to "f55d81", "backgroundColor" to "b6e3f4",
        ),
    );

    companion object {
        fun fromKey(key: String?): AvatarStyle = entries.firstOrNull { it.key == key } ?: PERSONAS
    }
}

/**
 * Kullanıcının avatarı: stil ve o stilin her sekmesinde seçilen değer (parçada seçeneğin ADI, renkte #
 * olmadan onaltılık renk; boş metin "yok"). Resim değil yalnızca bu seçimler saklanıyor; avatar her yerde
 * bunlardan yeniden çiziliyor.
 */
data class AvatarConfig(
    val style: AvatarStyle,
    val values: Map<String, String>,
) {
    fun with(key: String, value: String): AvatarConfig = copy(values = values + (key to value))

    /** "personas|hair=long;eyes=open;..." (anahtarlar alfabetik). */
    fun encode(): String =
        style.key + "|" + values.entries.sortedBy { it.key }.joinToString(";") { "${it.key}=${it.value}" }

    companion object {
        fun default(style: AvatarStyle): AvatarConfig = AvatarConfig(style, style.defaults)

        /** Bilinmeyen anahtarlar atlanır, eksikler stilin varsayılanında kalır. Okunamazsa null. */
        fun decode(s: String?): AvatarConfig? {
            if (s.isNullOrBlank() || '|' !in s) return null
            val style = AvatarStyle.fromKey(s.substringBefore('|'))
            val read = s.substringAfter('|').split(';').mapNotNull { part ->
                val i = part.indexOf('=')
                if (i <= 0) null else part.substring(0, i) to part.substring(i + 1)
            }.toMap()
            return AvatarConfig(style, style.defaults + read.filterKeys { it in style.defaults })
        }

        /** İsteğe bağlı parçalar (sakal) her seferinde çıkmasın; %70 "yok". */
        fun random(parts: AvatarParts, style: AvatarStyle, rng: Random): AvatarConfig {
            val values = style.tabs.associate { tab ->
                val options = AvatarRules.options(parts, style, tab)
                val value = if (!tab.isColor && tab.optional && rng.nextDouble() < 0.7) {
                    ""
                } else if (options.isEmpty()) {
                    style.defaults[tab.key].orEmpty()
                } else {
                    options.random(rng)
                }
                tab.key to value
            }
            return AvatarConfig(style, style.defaults + values)
        }
    }
}

/**
 * Bir stilin parça paketi: grup → (seçenek adı → SVG şablonu), paketteki sırayla. Şablonlarda renkler
 * `{{c:hair}}`, içine giren başka parçalar `{{p:eyes}}` yer tutucusudur.
 */
class AvatarParts private constructor(
    private val groups: Map<String, List<Pair<String, String>>>,
) {
    /** Grubun seçenek adları (paket sırası); grup yoksa boş. */
    fun names(group: String): List<String> = groups[group]?.map { it.first }.orEmpty()

    fun template(group: String, name: String): String? = groups[group]?.firstOrNull { it.first == name }?.second

    fun groupNames(): List<String> = groups.keys.toList()

    val isEmpty: Boolean get() = groups.isEmpty()

    companion object {
        val EMPTY: AvatarParts = AvatarParts(emptyMap())

        /**
         * `{"grup": {"ad": "şablon", ...}, ...}` biçimindeki JSON'u okur. Bozuk metinde istisna atmaz,
         * boş paket döner (Swift'ten çağrılıyor; Kotlin istisnası orada uygulamayı çökertir).
         */
        fun parse(json: String): AvatarParts = try {
            AvatarParts(AvatarJsonReader(json).readGroups())
        } catch (e: Exception) {
            EMPTY
        }
    }
}

/** Yalnızca "nesne içinde nesne, en içte metin" biçimini okuyan küçük JSON okuyucu. */
private class AvatarJsonReader(private val s: String) {
    private var i = 0

    fun readGroups(): Map<String, List<Pair<String, String>>> {
        val result = mutableMapOf<String, List<Pair<String, String>>>()
        readObject { group ->
            val options = mutableListOf<Pair<String, String>>()
            readObject { name -> options.add(name to readString()) }
            result[group] = options
        }
        skipSpace()
        require(i == s.length) { "JSON sonunda fazladan metin" }
        return result
    }

    private fun skipSpace() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    private fun expect(c: Char) {
        skipSpace()
        require(i < s.length && s[i] == c) { "'$c' bekleniyordu ($i)" }
        i++
    }

    /** `{ "anahtar": değer, ... }`; her anahtar için [onKey] değeri okur. */
    private fun readObject(onKey: (String) -> Unit) {
        expect('{')
        skipSpace()
        if (i < s.length && s[i] == '}') {
            i++
            return
        }
        while (true) {
            val key = readString()
            expect(':')
            onKey(key)
            skipSpace()
            require(i < s.length) { "Nesne kapanmadı" }
            if (s[i] == ',') {
                i++
                continue
            }
            expect('}')
            return
        }
    }

    private fun readString(): String {
        expect('"')
        val sb = StringBuilder()
        while (true) {
            require(i < s.length) { "Metin kapanmadı" }
            val c = s[i++]
            when (c) {
                '"' -> return sb.toString()
                '\\' -> {
                    require(i < s.length) { "Yarım kaçış" }
                    when (val e = s[i++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            require(i + 4 <= s.length) { "Yarım \\u" }
                            sb.append(s.substring(i, i + 4).toInt(16).toChar())
                            i += 4
                        }
                        else -> throw IllegalArgumentException("Bilinmeyen kaçış \\$e")
                    }
                }
                else -> sb.append(c)
            }
        }
    }
}

/** Avatarın saf kuralları; Swift'ten kodlanmış dizge ([AvatarConfig.encode]) ile çağrılıyor. */
object AvatarRules {
    /** `users/{uid}.avatarConfig`; `mirrorPublicProfile` onu `publicProfiles/{uid}.avatarConfig`'e aynalıyor. */
    const val FIRESTORE_FIELD = "avatarConfig"

    /** iOS paketindeki parça dosyasının adı (uzantısız) ve uzantısı. */
    const val PARTS_RESOURCE_NAME = "personas_parts"
    const val PARTS_RESOURCE_EXTENSION = "json"

    /** Düzenleme ekranındaki seçenek ızgarasının sütun sayısı (Android: GRID_COLUMNS). */
    const val GRID_COLUMNS = 4

    /** Seçili olmayan çipin zemini (Android: CHIP_IDLE_COLOR 0xFF22303A). */
    const val CHIP_IDLE_COLOR = "22303A"

    /** Gerekli alan başka değerdeyken "hiçbir seçenek seçili değil" anlamına gelen değer. */
    const val NO_SELECTION = "\u0000"

    private val HEX = Regex("[0-9a-fA-F]{6}")
    private val COLOR_KEYS = setOf("hairColor", "skinColor", "clothingColor", "backgroundColor")
    private val COLOR_TOKEN = Regex("""\{\{c:(\w+)\}\}""")
    private val PART_TOKEN = Regex("""\{\{p:(\w+)\}\}""")

    /** Kaydedilmiş avatar yoksa ya da okunamıyorsa varsayılan (Android: AvatarStore.loadActive). */
    fun decodeOrDefault(encoded: String?): AvatarConfig =
        AvatarConfig.decode(encoded) ?: AvatarConfig.default(AvatarStyle.PERSONAS)

    /** Dizge geçerli bir avatar mı (Android: `AvatarConfig.decode(x) != null`; listelerde harfli daire yerine). */
    fun hasAvatar(encoded: String?): Boolean = AvatarConfig.decode(encoded) != null

    fun defaultEncoded(): String = AvatarConfig.default(AvatarStyle.PERSONAS).encode()

    /** Okunup yeniden yazılmış hâli (bilinmeyen anahtarlar atılır, eksikler varsayılanla dolar). */
    fun normalized(encoded: String?): String = decodeOrDefault(encoded).encode()

    /** Avatarın stilinin sekmeleri (Android: style.tabs). */
    fun tabs(encoded: String?): List<AvatarTab> = decodeOrDefault(encoded).style.tabs

    /** Sekmenin seçenekleri; isteğe bağlı parçalarda başta "" (Yok). Android: AvatarArt.options. */
    fun options(parts: AvatarParts, style: AvatarStyle, tab: AvatarTab): List<String> =
        if (tab.isColor) {
            tab.palette
        } else {
            val names = parts.names(tab.key)
            if (tab.optional) listOf("") + names else names
        }

    fun options(parts: AvatarParts, encoded: String?, tab: AvatarTab): List<String> =
        options(parts, decodeOrDefault(encoded).style, tab)

    /** Bir alanın değeri (yoksa boş). */
    fun value(encoded: String?, key: String): String = decodeOrDefault(encoded).values[key].orEmpty()

    /**
     * Sekmenin şu anki değeri (Android: AvatarCustomFragment.currentValue). Gerekli alan başka bir değerdeyse
     * hiçbir seçenek seçili görünmesin diye [NO_SELECTION].
     */
    fun currentValue(encoded: String?, tab: AvatarTab): String {
        val config = decodeOrDefault(encoded)
        val rk = tab.requiresKey
        if (!tab.isColor && rk != null && config.values[rk] != tab.requiresValue) return NO_SELECTION
        return config.values[tab.key].orEmpty()
    }

    /** Sekmede [value] seçilmiş hâl (Android: AvatarCustomFragment.withValue). */
    fun withTabValue(encoded: String?, tab: AvatarTab, value: String): String {
        var c = decodeOrDefault(encoded).with(tab.key, value)
        val rk = tab.requiresKey
        val rv = tab.requiresValue
        if (!tab.isColor && rk != null && rv != null) c = c.with(rk, rv)
        return c.encode()
    }

    /** Rastgele avatar (Android: AvatarConfig.random), avatarın kendi stilinde. */
    fun randomEncoded(parts: AvatarParts, encoded: String?): String =
        AvatarConfig.random(parts, decodeOrDefault(encoded).style, Random.Default).encode()

    /** Kaydetmede ölçülen "kaç parça değişti" (Android: changedPartCount). */
    fun changedPartCount(baseline: String?, current: String?): Int {
        val before = AvatarConfig.decode(baseline) ?: return 0
        val now = decodeOrDefault(current)
        if (before.style != now.style) return now.values.size
        return now.values.count { (key, value) -> before.values[key] != value }
    }

    /**
     * Firestore'daki değer yerele alınmalı mı (Android: AvatarStore.applyRemote). Uzakta okunur bir değer
     * yoksa ya da yereldeki ile aynıysa null (yerele dokunulmaz); değilse yerele yazılacak dizge.
     */
    fun remoteToApply(localEncoded: String?, remoteEncoded: String?): String? {
        val remote = AvatarConfig.decode(remoteEncoded) ?: return null
        val encoded = remote.encode()
        if (localEncoded == encoded) return null
        return encoded
    }

    /**
     * Avatarı tek bir SVG belgesine çevirir (Android: AvatarArt.buildSvg). [closeUp] true ise yalnızca yüz
     * bölgesi ([AvatarStyle.faceCloseUp]) gösterilir. [circle] true ise arkada renkli daire var ve avatar o
     * daireye kırpılıyor.
     */
    fun svg(parts: AvatarParts, encoded: String?, circle: Boolean, closeUp: Boolean): String {
        val config = decodeOrDefault(encoded)
        val viewBox = if (closeUp) config.style.faceCloseUp else config.style.canvas
        return buildSvg(parts, config, circle, viewBox)
    }

    /** Android: AvatarArt.buildSvg (birebir). */
    fun buildSvg(parts: AvatarParts, config: AvatarConfig, circle: Boolean, viewBox: AvatarRect): String {
        val style = config.style
        // Başkalarının avatarı publicProfiles'tan, yani başka bir istemcinin yazdığı metinden geliyor.
        // Parça adları zaten listede aranıyor (bilinmeyen ad → çizilmez); renkler ise SVG'ye
        // doğrudan yazılıyor, bu yüzden 6 haneli onaltılık değilse stilin varsayılanına düşüyor.
        val v = config.values.mapValues { (k, value) ->
            if (k in COLOR_KEYS && !HEX.matches(value)) style.defaults[k].orEmpty() else value
        }
        val colors: Map<String, String>
        val rootTemplate: String
        when (style) {
            AvatarStyle.PERSONAS -> {
                colors = mapOf("skin" to v["skinColor"], "hair" to v["hairColor"], "clothing" to v["clothingColor"])
                    .mapValues { it.value.orEmpty() }
                val body = parts.template("root", "default").orEmpty()
                // Paketin kendi arka planı yok; profil resmi gibi yuvarlak dursun diye daire ve kırpma burada.
                rootTemplate = if (circle) {
                    """<clipPath id="avatarCircle"><circle cx="32" cy="32" r="32"/></clipPath>""" +
                        """<circle cx="32" cy="32" r="32" fill="#${v["backgroundColor"]}"/>""" +
                        """<g clip-path="url(#avatarCircle)">$body</g>"""
                } else {
                    body
                }
            }
        }

        // Seçilmemiş grup (sekmesi olmayan) → ilk seçeneği.
        fun partFor(group: String): String? {
            val name = v[group] ?: parts.names(group).firstOrNull() ?: return null
            if (name.isEmpty()) return null
            return parts.template(group, name)
        }

        fun expand(template: String): String {
            val withColors = COLOR_TOKEN.replace(template) { m -> "#" + (colors[m.groupValues[1]] ?: "000000") }
            return PART_TOKEN.replace(withColors) { m -> partFor(m.groupValues[1])?.let { expand(it) } ?: "" }
        }

        val vb = "${viewBox.left} ${viewBox.top} ${viewBox.width()} ${viewBox.height()}"
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="$vb" fill="none">${expand(rootTemplate)}</svg>"""
    }

    /**
     * Profil üstündeki avatar alanının rengi (Android: ProfileFragment.showAvatar —
     * `Color.parseColor("#" + values["backgroundColor"])`). 8 haneli AARRGGBB onaltılık; okunamazsa null
     * (Android: `background_color`). parseColor 6 (RRGGBB) ya da 8 (AARRGGBB) haneyi kabul eder.
     */
    fun headerColorArgb(encoded: String?): String? {
        val config = AvatarConfig.decode(encoded) ?: return null
        val raw = config.values["backgroundColor"] ?: return null
        val ok = raw.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        if (!ok) return null
        return when (raw.length) {
            6 -> ("FF" + raw).uppercase()
            8 -> raw.uppercase()
            else -> null
        }
    }
}

/** Profil üst alanının renkleri (Android: ProfileFragment.applyHeaderColor). Renkler AARRGGBB onaltılık. */
object ProfileHeaderColors {
    /** Açık zeminde ad ve ayarlar ikonu bu renkte (Android: HEADER_DARK_TEXT 0xFF3C3C3C). */
    const val DARK_TEXT = "FF3C3C3C"
    const val WHITE = "FFFFFFFF"

    /** Okunamayan metin opak siyah sayılır (Swift'ten çağrılıyor; istisna atılmamalı). */
    private fun channels(argbHex: String): IntArray {
        val v = argbHex.toLongOrNull(16) ?: 0xFF000000L
        return intArrayOf(
            ((v shr 24) and 0xFF).toInt(),
            ((v shr 16) and 0xFF).toInt(),
            ((v shr 8) and 0xFF).toInt(),
            (v and 0xFF).toInt(),
        )
    }

    private fun linear(c: Int): Double {
        val s = c / 255.0
        return if (s < 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    /** Android: ColorUtils.calculateLuminance (alfa hesaba katılmaz). */
    fun luminance(argbHex: String): Double {
        val ch = channels(argbHex)
        val y = linear(ch[1]) * 0.2126 + linear(ch[2]) * 0.7152 + linear(ch[3]) * 0.0722
        return min(1.0, max(0.0, y))
    }

    /** Zemin açıksa yazı/ikon [DARK_TEXT], koyuysa [WHITE] (eşik 0.5). */
    fun foreground(backgroundArgb: String): String =
        if (luminance(backgroundArgb) > 0.5) DARK_TEXT else WHITE

    /** Kaydırınca görünen alt çizgi: zeminin %15 siyahla karışımı (Android: ColorUtils.blendARGB). */
    fun divider(backgroundArgb: String): String = blend(backgroundArgb, "FF000000", 0.15f)

    /** Android: ColorUtils.blendARGB — kanal başına doğrusal karışım, sonuç tam sayıya kesilir. */
    fun blend(argb1: String, argb2: String, ratio: Float): String {
        val a = channels(argb1)
        val b = channels(argb2)
        val inverse = 1f - ratio
        val sb = StringBuilder()
        for (k in 0..3) {
            val mixed = (a[k] * inverse + b[k] * ratio).toInt()
            sb.append(mixed.toString(16).uppercase().padStart(2, '0'))
        }
        return sb.toString()
    }
}

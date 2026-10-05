package com.example.app

import android.content.Context
import android.graphics.RectF
import org.json.JSONObject

/**
 * Avatar stilleri ve parçalarından tek bir SVG kurma.
 *
 * KAYNAK: DiceBear 9.4.2 "personas" paketi — Draftbit'in Personas'ı. Lisans CC BY 4.0
 * (design/avatar/PERSONAS_LICENSE.txt): ticari kullanım ve satış serbest, ama uygulamada
 * "Personas by Draftbit" diye kaynak gösterilmesi ŞART.
 *
 * Avataaars stili de denendi; tarzı uygulamaya daha az uyduğu için kaldırıldı. Yapı birden çok
 * stili destekliyor: yeni stil [AvatarStyle]'a bir giriş ve [AvatarArt.buildSvg]'de bir dal.
 *
 * assets/avatar/<stil>_parts.json design/avatar/gen.mjs ile paketlerden üretildi: her parça,
 * içindeki renkler `{{c:hair}}`, içine giren başka parçalar `{{p:eyes}}` yer tutucusu olacak
 * şekilde düz SVG metni. Burada yer tutucular doldurulup [AvatarView] AndroidSVG ile çiziyor.
 * Parçaların dizilişi paketin kendi gövde şablonunda; burada elle tekrar edilmiyor.
 */
enum class AvatarStyle(
    val key: String,
    val label: String,
    val asset: String,
    /** Çizimin tuvali (viewBox). */
    val canvas: RectF,
    /** Kaş, göz ve ağzı kapsayan yakın plan; bu sekmelerin küçük resimleri için. */
    val faceCloseUp: RectF,
    val tabs: List<AvatarTab>,
    val defaults: Map<String, String>,
) {
    PERSONAS(
        key = "personas",
        label = "Personas",
        asset = "avatar/personas_parts.json",
        canvas = RectF(0f, 0f, 64f, 64f),
        faceCloseUp = RectF(15f, 17f, 49f, 49f),
        tabs = listOf(
            AvatarTab.Part("Saç", "hair"),
            AvatarTab.Color("Saç rengi", "hairColor", listOf("362c47", "6c4545", "e15c66", "e16381", "f27d65", "f29c65", "dee1f5")),
            AvatarTab.Color("Ten", "skinColor", listOf("eeb4a4", "e7a391", "e5a07e", "d78774", "b16a5b", "92594b", "623d36")),
            AvatarTab.Part("Göz", "eyes", closeUp = true),
            AvatarTab.Part("Burun", "nose", closeUp = true),
            AvatarTab.Part("Ağız", "mouth", closeUp = true),
            AvatarTab.Part("Sakal", "facialHair", optional = true, closeUp = true),
            AvatarTab.Part("Kıyafet", "body"),
            AvatarTab.Color("Renk", "clothingColor", listOf("456dff", "54d7c7", "7555ca", "6dbb58", "e24553", "f3b63a", "f55d81")),
            AvatarTab.Color("Arka plan", "backgroundColor", BACKGROUNDS, swatch = true),
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

/** Arka plan renkleri (DiceBear sitesindeki pastel zeminler). */
private val BACKGROUNDS = listOf("b6e3f4", "65c9ff", "c0aede", "d1d4f9", "ffd5dc", "ffdfbf", "a7ffc4", "ffffb1", "262e33")

/** Düzenleme ekranındaki bir sekme. [key] aynı zamanda [AvatarConfig.values] anahtarı. */
sealed class AvatarTab(val label: String, val key: String) {
    /**
     * Bir parça grubu. [optional] ise başta "Yok" (boş metin) var. [closeUp] ise küçük resim
     * yüze yaklaşır. [requires]: bu sekmede seçim yapılınca başka bir alan da bu değere geçer.
     */
    class Part(
        label: String,
        val group: String,
        val optional: Boolean = false,
        val closeUp: Boolean = false,
        val requires: kotlin.Pair<String, String>? = null,
    ) : AvatarTab(label, group)

    /** Renk; [swatch] ise kutuda avatar yerine yalnızca renk dairesi. */
    class Color(label: String, key: String, val palette: List<String>, val swatch: Boolean = false) : AvatarTab(label, key)
}

object AvatarArt {

    /** Stil → grup → (seçenek adı → SVG şablonu). Sıra paketteki sırayla aynı. */
    private val parts = HashMap<AvatarStyle, Map<String, LinkedHashMap<String, String>>>()

    private fun load(context: Context, style: AvatarStyle): Map<String, LinkedHashMap<String, String>> {
        parts[style]?.let { return it }
        val text = context.assets.open(style.asset).bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val map = HashMap<String, LinkedHashMap<String, String>>()
        for (group in root.keys()) {
            val obj = root.getJSONObject(group)
            val options = LinkedHashMap<String, String>()
            for (name in obj.keys()) options[name] = obj.getString(name)
            map[group] = options
        }
        parts[style] = map
        return map
    }

    /** Sekmenin seçenekleri; isteğe bağlı parçalarda başta "" (Yok). */
    fun options(context: Context, style: AvatarStyle, tab: AvatarTab): List<String> = when (tab) {
        is AvatarTab.Color -> tab.palette
        is AvatarTab.Part -> {
            val names = load(context, style)[tab.group]?.keys?.toList().orEmpty()
            if (tab.optional) listOf("") + names else names
        }
    }

    /**
     * [config]'i tek bir SVG belgesine çevirir. [viewBox] yalnızca o bölgeyi gösterir (yakın
     * plan için). [circle] true ise arkada renkli daire var ve avatar o daireye sığdırılıyor.
     */
    fun buildSvg(context: Context, config: AvatarConfig, circle: Boolean, viewBox: RectF): String {
        val style = config.style
        val all = load(context, style)
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
                val body = all.getValue("root").getValue("default")
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
            val name = v[group] ?: all[group]?.keys?.firstOrNull() ?: return null
            if (name.isEmpty()) return null
            return all[group]?.get(name)
        }

        fun expand(template: String): String {
            val withColors = COLOR_TOKEN.replace(template) { m -> "#" + (colors[m.groupValues[1]] ?: "000000") }
            return PART_TOKEN.replace(withColors) { m -> partFor(m.groupValues[1])?.let { expand(it) } ?: "" }
        }

        val vb = "${viewBox.left} ${viewBox.top} ${viewBox.width()} ${viewBox.height()}"
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="$vb" fill="none">${expand(rootTemplate)}</svg>"""
    }

    private val HEX = Regex("[0-9a-fA-F]{6}")
    private val COLOR_KEYS = setOf("hairColor", "skinColor", "clothingColor", "backgroundColor")
    private val COLOR_TOKEN = Regex("""\{\{c:(\w+)\}\}""")
    private val PART_TOKEN = Regex("""\{\{p:(\w+)\}\}""")
}

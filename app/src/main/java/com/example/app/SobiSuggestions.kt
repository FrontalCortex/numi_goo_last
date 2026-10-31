package com.example.app

/**
 * Sobi ekranındaki öneri balonlarının anahtarları ve etiketleri. Anahtarlar sunucunun önerileriyle
 * aynı (functions/sobi/chat.js `suggestionsFor`); etiket hem balonda görünen hem sunucuya giden metin.
 * "Bana göster", "Bir daha göster" ve "Bir örnek ver" sunucuya gitmez, ekranda yerelde yapılır.
 */
object SobiSuggestions {
    const val KEY_DONT_KNOW = "ne_yapacagim"
    const val KEY_WHY = "neden_boyle"
    const val KEY_SHOW = "bana_goster"
    const val KEY_SHOW_AGAIN = "bir_daha_goster"
    const val KEY_GOT_IT = "anladim"
    const val KEY_DIDNT_GET = "bunu_anlamadim"
    const val KEY_EXAMPLE = "ornek_ver"

    fun labelOf(key: String): String? = when (key) {
        KEY_DONT_KNOW -> appString(R.string.sobi_suggest_dont_know)
        KEY_WHY -> appString(R.string.sobi_suggest_why)
        KEY_SHOW -> appString(R.string.sobi_suggest_show)
        KEY_SHOW_AGAIN -> appString(R.string.sobi_suggest_show_again)
        KEY_GOT_IT -> appString(R.string.sobi_suggest_got_it)
        KEY_DIDNT_GET -> appString(R.string.sobi_suggest_didnt_get)
        KEY_EXAMPLE -> appString(R.string.sobi_suggest_example)
        else -> null
    }
}

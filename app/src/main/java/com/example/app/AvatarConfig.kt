package com.example.app

import android.content.Context

/**
 * Kullanıcının avatarı: stil ve o stilin her sekmesinde seçilen değer (parçada seçeneğin
 * ADI, renkte # olmadan onaltılık renk; boş metin "yok").
 *
 * Resim değil yalnızca bu seçimler saklanıyor; avatar her yerde ([AvatarView]) bunlardan
 * yeniden çiziliyor. Sıra numarası yerine ad tutuluyor: pakete yeni seçenek gelse de kayıtlı
 * avatarlar değişmez.
 */
data class AvatarConfig(
    val style: AvatarStyle,
    val values: Map<String, String>,
) {
    fun with(key: String, value: String) = copy(values = values + (key to value))

    /** "personas|hair=long;eyes=open;..." */
    fun encode(): String =
        style.key + "|" + values.entries.sortedBy { it.key }.joinToString(";") { "${it.key}=${it.value}" }

    companion object {
        fun default(style: AvatarStyle) = AvatarConfig(style, style.defaults)

        /** Bilinmeyen anahtarlar atlanır, eksikler stilin varsayılanında kalır. */
        fun decode(s: String?): AvatarConfig? {
            if (s.isNullOrBlank() || '|' !in s) return null
            val style = AvatarStyle.fromKey(s.substringBefore('|'))
            val read = s.substringAfter('|').split(';').mapNotNull { part ->
                val i = part.indexOf('=')
                if (i <= 0) null else part.substring(0, i) to part.substring(i + 1)
            }.toMap()
            return AvatarConfig(style, style.defaults + read.filterKeys { it in style.defaults })
        }

        fun random(context: Context, style: AvatarStyle): AvatarConfig {
            val values = style.tabs.associate { tab ->
                val options = AvatarArt.options(context, style, tab)
                // İsteğe bağlı parçalar (sakal, gözlük) her seferinde çıkmasın; çoğunlukla "yok".
                val value = if (tab is AvatarTab.Part && tab.optional && Math.random() < 0.7) "" else options.random()
                tab.key to value
            }
            return AvatarConfig(style, style.defaults + values)
        }
    }
}

/**
 * Avatarın kaydı.
 *
 * Kullanılan avatar Firestore'da `users/{uid}.avatarConfig` alanında ([AvatarConfig.encode]);
 * `mirrorPublicProfile` onu `publicProfiles`'a aynalıyor, başkalarının listesi oradan çiziyor.
 * Cihazda da bir kopyası var: alt bar ikonu ve profil, açılışta ağı beklemeden çizilebilsin.
 * Yerel kayıt kullanıcıya göre ayrı (aynı telefonda hesap değişince başkasının avatarı görünmesin);
 * her stilin son hâli de ayrı tutuluyor, stil değiştirip dönünce seçimler kaybolmasın.
 */
object AvatarStore {
    const val FIRESTORE_FIELD = "avatarConfig"

    private const val PREFS = "avatar_prefs"

    private fun uid(): String = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anon"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun activeKey() = "active_${uid()}"
    private fun styleKey(style: AvatarStyle) = "config_${uid()}_${style.key}"

    /** Kullanılan avatar; hiç kaydedilmemişse varsayılan. */
    fun loadActive(context: Context): AvatarConfig =
        AvatarConfig.decode(prefs(context).getString(activeKey(), null)) ?: AvatarConfig.default(AvatarStyle.PERSONAS)

    /** Kullanıcı hiç avatar kaydetti mi (yerelde). */
    fun hasSaved(context: Context): Boolean = prefs(context).contains(activeKey())

    /** Bir stilin son hâli (düzenleme ekranı için). */
    fun load(context: Context, style: AvatarStyle): AvatarConfig =
        AvatarConfig.decode(prefs(context).getString(styleKey(style), null))?.takeIf { it.style == style }
            ?: loadActive(context).takeIf { it.style == style }
            ?: AvatarConfig.default(style)

    /** Kaydeder: yerelde ve Firestore'da. Firestore yazımı başarısız olsa da yerel kayıt kalır. */
    fun save(context: Context, config: AvatarConfig) {
        saveLocal(context, config)
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .update(FIRESTORE_FIELD, config.encode())
            .addOnFailureListener { e -> android.util.Log.w("AvatarStore", "avatarConfig yazılamadı", e) }
    }

    /**
     * Firestore'daki değeri yerele alır (yeni cihaz, yeniden kurulum). Değişiklik olduysa true.
     * Uzakta değer yoksa yerel kayda dokunulmaz.
     */
    fun applyRemote(context: Context, encoded: String?): Boolean {
        val remote = AvatarConfig.decode(encoded) ?: return false
        if (prefs(context).getString(activeKey(), null) == remote.encode()) return false
        saveLocal(context, remote)
        return true
    }

    private fun saveLocal(context: Context, config: AvatarConfig) {
        prefs(context).edit()
            .putString(styleKey(config.style), config.encode())
            .putString(activeKey(), config.encode())
            .apply()
    }
}

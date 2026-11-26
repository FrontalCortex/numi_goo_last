// Kaynak: Android'de karşılığı yok — android.content.SharedPreferences yerine geçen ince arayüz.
// `oyun` paketindeki depolar (MissionsProgressStore, EnergyManager, UserWalletFirestore, …)
// Android'de SharedPreferences okuyup yazıyordu; burada G/Ç bu arayüzün arkasına alındı.
// iOS tarafı NSUserDefaults ile uygular; testler [InMemoryKeyValueStore] kullanır.
package com.sorobit.shared.oyun

/**
 * SharedPreferences davranışını taklit eden anahtar-değer deposu.
 *
 * Android'deki `edit()…apply()` toplu yazımı burada anında yazıma dönüşür; taşınan kodda
 * okuma/yazma sırası Android'dekiyle aynı sonucu verecek şekilde korunmuştur.
 */
interface OyunKeyValueStore {
    /** Anahtar yoksa (ya da tipi farklıysa) null. */
    fun getString(key: String): String?
    fun getInt(key: String, defaultValue: Int): Int
    fun getLong(key: String, defaultValue: Long): Long
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun contains(key: String): Boolean

    /** SharedPreferences.getAll().keys karşılığı. */
    fun keys(): Set<String>

    fun putString(key: String, value: String)
    fun putInt(key: String, value: Int)
    fun putLong(key: String, value: Long)
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)

    /** SharedPreferences.Editor.clear() karşılığı. */
    fun clear()
}

/** Bellek içi uygulama: testler ve kalıcılık gerekmeyen durumlar için. */
class InMemoryKeyValueStore : OyunKeyValueStore {
    private val map = mutableMapOf<String, Any>()

    override fun getString(key: String): String? = map[key] as? String
    override fun getInt(key: String, defaultValue: Int): Int = map[key] as? Int ?: defaultValue
    override fun getLong(key: String, defaultValue: Long): Long = map[key] as? Long ?: defaultValue
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = map[key] as? Boolean ?: defaultValue
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun keys(): Set<String> = map.keys.toSet()

    override fun putString(key: String, value: String) { map[key] = value }
    override fun putInt(key: String, value: Int) { map[key] = value }
    override fun putLong(key: String, value: Long) { map[key] = value }
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    override fun remove(key: String) { map.remove(key) }
    override fun clear() { map.clear() }
}

/** `String.format("%02d", n)` karşılığı (negatif olmayan değerler için). */
internal fun ikiHane(n: Int): String = if (n in 0..9) "0$n" else n.toString()

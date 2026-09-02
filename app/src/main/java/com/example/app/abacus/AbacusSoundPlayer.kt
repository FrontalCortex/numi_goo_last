package com.example.app.abacus

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.app.R

/**
 * Abaküs boncuklarına dokunulduğunda çalınan kısa tık sesini yönetir.
 * Kullanıcı Ayarlar > Ses ekranından 4 farklı ses arasından seçim yapabilir;
 * seçim SharedPreferences ("AppPrefs") içinde kalıcı olarak saklanır ve
 * uygulamadaki tüm abaküs ekranları (AbacusBeadController kullanan her yer)
 * aynı seçimi paylaşır.
 */
object AbacusSoundPlayer {
    private const val PREFS_NAME = "AppPrefs"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_ABACUS_SOUND_CHOICE = "abacus_sound_choice"
    const val DEFAULT_SOUND_INDEX = 1
    const val SOUND_COUNT = 5

    /** Boncuk aktif edilirken (yukarı/aşağı doğru "açılırken") normal perde. */
    private const val ACTIVATE_RATE = 1.0f
    /** Boncuk inaktif edilirken (geri alınırken) daha kalın/pes bir ton için düşük perde. */
    private const val DEACTIVATE_RATE = 0.75f

    /** Ses dosyaları arasındaki kayıt seviyesi farkını dengelemek için sese özel çalma seviyesi. */
    private fun playbackVolumeFor(index: Int): Float = when (index.coerceIn(1, SOUND_COUNT)) {
        4 -> 1.0f
        else -> 0.6f
    }

    private var soundPool: SoundPool? = null
    private var loadedIndex: Int = 0
    private var loadedSampleId: Int = 0
    private var isLoaded: Boolean = false
    private var autoPlayOnLoad: Boolean = false

    private fun soundResFor(index: Int): Int = when (index.coerceIn(1, SOUND_COUNT)) {
        1 -> R.raw.abacus_sound1
        2 -> R.raw.abacus_sound2
        3 -> R.raw.abacus_sound3
        4 -> R.raw.abacus_sound4
        else -> R.raw.abacus_sound5
    }

    private fun ensurePool(): SoundPool {
        soundPool?.let { return it }
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setAudioAttributes(attrs)
            .setMaxStreams(4)
            .build()
        pool.setOnLoadCompleteListener { p, sampleId, status ->
            if (status == 0 && sampleId == loadedSampleId) {
                isLoaded = true
                if (autoPlayOnLoad) {
                    autoPlayOnLoad = false
                    val volume = playbackVolumeFor(loadedIndex)
                    p.play(sampleId, volume, volume, 1, 0, 1f)
                }
            }
        }
        soundPool = pool
        return pool
    }

    private fun loadIndex(appContext: Context, index: Int, autoPlay: Boolean) {
        val pool = ensurePool()
        val clamped = index.coerceIn(1, SOUND_COUNT)
        isLoaded = false
        loadedIndex = clamped
        autoPlayOnLoad = autoPlay
        loadedSampleId = pool.load(appContext, soundResFor(clamped), 1)
    }

    fun getSelectedIndex(context: Context): Int {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ABACUS_SOUND_CHOICE, DEFAULT_SOUND_INDEX).coerceIn(1, SOUND_COUNT)
    }

    /** Abaküs ekranı kurulurken çağrılır; seçili sesi arka planda hazırlar (ilk dokunuşta gecikme olmasın diye). */
    fun preload(context: Context) {
        val appContext = context.applicationContext
        val selected = getSelectedIndex(appContext)
        if (soundPool == null || loadedIndex != selected) {
            loadIndex(appContext, selected, autoPlay = false)
        }
    }

    /**
     * Boncuğa dokunulup durumu değiştiğinde çağrılır; "Ses efektleri" kapalıysa sessiz kalır.
     * [activating] true ise boncuk aktif ediliyor demektir (normal perde), false ise aktif
     * boncuk geri alınıyor demektir (daha kalın/pes perde).
     */
    fun playBeadClick(context: Context, activating: Boolean) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SOUND_ENABLED, true)) return
        val selected = getSelectedIndex(appContext)
        if (soundPool == null || loadedIndex != selected) {
            // Henüz yüklenmedi (örn. seçim az önce değişti); bu tıklamada sessiz kal, arka planda yükle.
            loadIndex(appContext, selected, autoPlay = false)
            return
        }
        if (!isLoaded || loadedSampleId == 0) return
        val rate = if (activating) ACTIVATE_RATE else DEACTIVATE_RATE
        val volume = playbackVolumeFor(selected)
        soundPool?.play(loadedSampleId, volume, volume, 1, 0, rate)
    }

    /** Ayarlar ekranında kullanıcı bir ses seçeneğine dokunduğunda çağrılır: seçimi kaydeder ve hemen çalar. */
    fun choose(context: Context, index: Int) {
        val appContext = context.applicationContext
        val clamped = index.coerceIn(1, SOUND_COUNT)
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ABACUS_SOUND_CHOICE, clamped).apply()
        loadIndex(appContext, clamped, autoPlay = true)
    }
}

package com.example.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * BadgeFragment'te level atlama/rozet kazanma anında (progress bar dolumu bitince) çalınan sesi yönetir.
 */
object BadgeSoundPlayer {
    private const val PREFS_NAME = "AppPrefs"
    private const val KEY_SOUND_ENABLED = "sound_enabled"

    private var soundPool: SoundPool? = null
    private var sampleId: Int = 0
    private var isLoaded: Boolean = false

    private fun ensurePool(appContext: Context): SoundPool {
        soundPool?.let { return it }
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setAudioAttributes(attrs)
            .setMaxStreams(2)
            .build()
        pool.setOnLoadCompleteListener { _, id, status ->
            if (status == 0 && id == sampleId) isLoaded = true
        }
        soundPool = pool
        sampleId = pool.load(appContext, R.raw.badge_sound, 1)
        return pool
    }

    /** BadgeFragment açılırken çağrılır; sesi arka planda hazırlar. */
    fun preload(context: Context) {
        ensurePool(context.applicationContext)
    }

    /** Rozet kazanma progress animasyonu tamamlanınca çağrılır. */
    fun play(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SOUND_ENABLED, true)) return
        val pool = ensurePool(appContext)
        if (!isLoaded || sampleId == 0) {
            pool.setOnLoadCompleteListener { p, id, status ->
                if (status == 0 && id == sampleId) {
                    isLoaded = true
                    p.play(sampleId, 0.8f, 0.8f, 1, 0, 1f)
                }
            }
            return
        }
        pool.play(sampleId, 0.8f, 0.8f, 1, 0, 1f)
    }
}

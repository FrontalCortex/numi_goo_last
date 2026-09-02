package com.example.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Sandık ekranında (NewChestFragment) çalınan sesleri yönetir:
 * - Level atlama adımı: ton, mevcut rarity'nin sırasına göre belirlenir; sandık bir üst rarity'ye
 *   geçtiğinde ses incelir ve rarity değişmeden devam eden tıklamalarda da o rarity'nin incelmiş
 *   tonuyla çalar (bir sonraki tıklamada eski/normal tona geri dönmez).
 * - Ödül sesi: sandık açılıp ödül gösterilirken bir kez çalar.
 */
object ChestSoundPlayer {
    private const val PREFS_NAME = "AppPrefs"
    private const val KEY_SOUND_ENABLED = "sound_enabled"

    private var soundPool: SoundPool? = null

    private var levelSampleId: Int = 0
    private var isLevelLoaded: Boolean = false

    private var rewardSampleId: Int = 0
    private var isRewardLoaded: Boolean = false

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
            if (status != 0) return@setOnLoadCompleteListener
            when (id) {
                levelSampleId -> isLevelLoaded = true
                rewardSampleId -> isRewardLoaded = true
            }
        }
        soundPool = pool
        levelSampleId = pool.load(appContext, R.raw.chest_level_sound, 1)
        rewardSampleId = pool.load(appContext, R.raw.reward_sound, 1)
        return pool
    }

    /** Sandık ekranı açılırken çağrılır; sesleri arka planda hazırlar. */
    fun preload(context: Context) {
        ensurePool(context.applicationContext)
    }

    /**
     * Ekrana her adım tıklamasında çağrılır. [rarityOrdinal] tıklama sonrası mevcut rarity'nin
     * sırasıdır (COMMON=0, RARE=1, EPIC=2, LEGENDARY=3); ton buna göre belirlenir, böylece rarity
     * değişmeden devam eden tıklamalarda da son ulaşılan seviyenin ince tonu korunur.
     */
    fun playForRarity(context: Context, rarityOrdinal: Int) {
        val rate = (1f + rarityOrdinal * 0.25f).coerceIn(1f, 2f)
        play(context, sampleId = levelSampleId, isLoaded = isLevelLoaded, rate = rate)
    }

    /** Sandık açılıp ödül gösterilirken çağrılır. */
    fun playReward(context: Context) {
        play(context, sampleId = rewardSampleId, isLoaded = isRewardLoaded, rate = 1f)
    }

    private fun play(context: Context, sampleId: Int, isLoaded: Boolean, rate: Float) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SOUND_ENABLED, true)) return
        val pool = ensurePool(appContext)
        if (!isLoaded || sampleId == 0) return
        pool.play(sampleId, 0.7f, 0.7f, 1, 0, rate)
    }
}

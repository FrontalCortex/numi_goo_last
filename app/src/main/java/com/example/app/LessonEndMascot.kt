package com.example.app

import android.content.Context
import com.example.app.BunnyMascotView.Emote

/**
 * Ders sonu ekranlarındaki Sobi sahnesini seçer.
 *
 *  - Başarılı ders ([LessonResult]) ve sandık sonucu ([ChestResult]): [SUCCESS] torbasından.
 *    Dokuz sahne karışık sırayla çekiliyor, torba bitmeden hiçbiri tekrar etmiyor (düz rastgele
 *    seçimde aynı sahne üst üste gelebiliyordu). Torba cihazda saklanıyor; uygulama kapanıp
 *    açılsa da sıra kaldığı yerden sürüyor. İki ekran aynı torbayı paylaşıyor.
 *  - Başarısız ders ([LessonResultFalse]): hep [FAILURE] (üzgün → kararlı).
 *
 * Sahnelerin çoğu bir kez oynayıp son pozunda bekliyor (bkz. [Emote.holdFromMs]); gitar,
 * hokkabaz ve rapçi döngüde.
 */
object LessonEndMascot {

    /** Başarıda gösterilebilecek sahneler; hepsi eşit şansla. */
    val SUCCESS = listOf(
        Emote.GUITAR,
        Emote.JUGGLE,
        Emote.PERFECT,
        Emote.COWBOY_FRONT,
        Emote.KARATE,
        Emote.NINJA,
        Emote.PIRATE,
        Emote.GLASSES,
        Emote.RAPPER,
    )

    val FAILURE = Emote.DETERMINED

    /**
     * Sonuç ekranları sağdan kayarak giriyor (queue_screen_in, 400 ms). Sahne o bitince
     * başlıyor; yoksa ilk saniyesi kayma sırasında kaçıyordu.
     */
    const val ENTER_DELAY_MS = 450L

    private const val PREFS = "lesson_end_mascot"
    private const val KEY_BAG = "bag"
    private const val KEY_LAST = "last"

    /** Torbadan sıradaki başarı sahnesi; torba boşsa yeniden karıştırılıyor. */
    fun nextSuccess(context: Context): Emote {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Listeden çıkarılmış eski bir sahne adı torbada kalmışsa atlanıyor.
        val bag = prefs.getString(KEY_BAG, null)
            ?.split(',')
            ?.mapNotNull { name -> SUCCESS.firstOrNull { it.name == name } }
            .orEmpty()
            .toMutableList()
        if (bag.isEmpty()) {
            bag += SUCCESS.shuffled()
            // Yeni torbanın ilki bir önceki sahne olmasın: torba sınırında da üst üste gelmesin.
            val last = prefs.getString(KEY_LAST, null)
            if (bag.size > 1 && bag[0].name == last) bag.add(bag.removeAt(0))
        }
        val next = bag.removeAt(0)
        prefs.edit()
            .putString(KEY_BAG, bag.joinToString(",") { it.name })
            .putString(KEY_LAST, next.name)
            .apply()
        return next
    }
}

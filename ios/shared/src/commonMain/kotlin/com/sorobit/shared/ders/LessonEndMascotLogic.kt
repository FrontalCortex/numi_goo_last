// KAYNAK: app/src/main/java/com/example/app/LessonEndMascot.kt (güncel Android, versionCode 26)
//
// Ders sonu ekranlarındaki Sobi sahnesinin seçimi:
//  - Başarılı ders (LessonResult) ve sandık sonucu (ChestResult): [SUCCESS] "torbasından". Dokuz sahne
//    karışık sırayla çekilir, torba bitmeden hiçbiri tekrar etmez; yeni torbanın ilki bir önceki
//    sahne olmaz. İki ekran aynı torbayı paylaşır.
//  - Başarısız ders (LessonResultFalse): hep [FAILURE].
//
// Android'den bilinçli farklar:
//  - `BunnyMascotView.Emote` enum'u platform işi (çizim); burada enum ADI (String) taşınır
//    (GuidePanelData.emote ile aynı yaklaşım).
//  - Torbayı cihazda saklamak (SharedPreferences "lesson_end_mascot": "bag", "last") platform işi:
//    buradaki fonksiyon saklı iki dizgeyi alır, çekilen sahneyi ve yeni dizgeleri döndürür; çağıran
//    (iOS: UserDefaults) yeni dizgeleri saklar.
//  - Rastgelelik RastgeleKaynagi'ndan gelir (testte tohumlanabilir).
package com.sorobit.shared.ders

import com.sorobit.shared.matematik.RastgeleKaynagi
import kotlin.random.Random

/** [LessonEndMascotLogic.nextSuccess] sonucu: çekilen sahne ve saklanacak yeni durum. */
data class LessonEndMascotDraw(
    /** Çekilen sahnenin `BunnyMascotView.Emote` enum adı. */
    val emote: String,
    /** Saklanacak yeni torba (virgülle ayrılmış enum adları; boş olabilir). */
    val bag: String,
    /** Saklanacak "son çekilen" (= [emote]). */
    val last: String,
)

object LessonEndMascotLogic {

    /** Başarıda gösterilebilecek sahneler (`BunnyMascotView.Emote` adları); hepsi eşit şansla. */
    val SUCCESS: List<String> = listOf(
        "GUITAR",
        "JUGGLE",
        "PERFECT",
        "COWBOY_FRONT",
        "KARATE",
        "NINJA",
        "PIRATE",
        "GLASSES",
        "RAPPER",
    )

    /** Başarısız derste hep gösterilen sahne (üzgün → kararlı). */
    const val FAILURE: String = "DETERMINED"

    /**
     * Sonuç ekranları sağdan kayarak girer (400 ms); sahne o bitince başlar (Android: ENTER_DELAY_MS).
     */
    const val ENTER_DELAY_MS: Long = 450L

    /**
     * Torbadan sıradaki başarı sahnesi; torba boşsa yeniden karıştırılır
     * (Android: `LessonEndMascot.nextSuccess`).
     *
     * @param storedBag saklı torba ("GUITAR,NINJA,..."); yoksa null. Listeden çıkarılmış eski adlar atlanır.
     * @param storedLast saklı son çekilen sahne; yoksa null.
     */
    fun nextSuccess(storedBag: String?, storedLast: String?): LessonEndMascotDraw =
        nextSuccessWith(storedBag, storedLast, RastgeleKaynagi.rastgele)

    /** [nextSuccess]'in rastgelelik kaynağı verilen biçimi (test için). */
    fun nextSuccessWith(storedBag: String?, storedLast: String?, random: Random): LessonEndMascotDraw {
        // Listeden çıkarılmış eski bir sahne adı torbada kalmışsa atlanıyor.
        val bag: MutableList<String> = (storedBag ?: "")
            .split(',')
            .mapNotNull { name -> SUCCESS.firstOrNull { it == name } }
            .toMutableList()
        if (bag.isEmpty()) {
            bag += SUCCESS.shuffled(random)
            // Yeni torbanın ilki bir önceki sahne olmasın: torba sınırında da üst üste gelmesin.
            if (bag.size > 1 && bag[0] == storedLast) bag.add(bag.removeAt(0))
        }
        val next: String = bag.removeAt(0)
        return LessonEndMascotDraw(emote = next, bag = bag.joinToString(","), last = next)
    }
}

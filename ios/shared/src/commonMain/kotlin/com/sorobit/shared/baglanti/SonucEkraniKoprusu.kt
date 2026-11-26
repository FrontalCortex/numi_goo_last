// Android karşılığı yok: iOS sonuç ekranının (ios/iosApp/iosApp/Sonuc) ortak koda tek giriş kapısı.
// Yeni kural içermez; var olan kuralları çağırır.
//
// Neden var: `ChestResultLogic` adında İKİ nesne var (`ders` ve `oyun` paketlerinde). Swift başlığında
// biri `_` son eki alır ve hangisinin alacağı ancak üretilen başlıktan (ios/docs/Shared.h) öğrenilebilir.
// Bu nesnenin adı tekildir; Swift tarafı yalnızca bunu çağırır, böylece derleme ad çakışmasının nasıl
// çözüldüğüne bağlı kalmaz. Ayrıca Swift'e taşınmayan varsayılan parametreler burada doldurulur.
//
// Kaynak kurallar: ders/LessonResultLogic.kt (LessonResultLogic, LessonResultFalseLogic, ChestResultLogic)
// ve oyun/ChestResultLogic.kt (animasyon listesi, çubuk rengi, yıldız oranı, yıldız sesi).
package com.sorobit.shared.baglanti

import com.sorobit.shared.ders.GlobalLessonData
import com.sorobit.shared.ders.LessonResultFalseLogic
import com.sorobit.shared.ders.LessonResultLogic
import com.sorobit.shared.matematik.RastgeleKaynagi
import com.sorobit.shared.oyun.ScoreBarColorBlend
import com.sorobit.shared.ders.ChestResultLogic as DersChestResultLogic
import com.sorobit.shared.oyun.ChestResultLogic as OyunChestResultLogic

object SonucEkraniKoprusu {

    // ── LessonResult / LessonResultFalse ────────────────────────────────────

    /** Başarı yüzdesi (0..100). */
    fun basariOrani(dogru: Int, toplam: Int): Float = LessonResultLogic.successRate(dogru, toplam)

    /** Ders puanı: başarı yüzdesi × 5. */
    fun dersPuani(basariOrani: Float): Int = LessonResultLogic.lessonScore(basariOrani)

    /** Başarısız sonuç ekranındaki neden satırı. */
    fun basarisizlikAciklamasi(sandikBasarisizligi: Boolean): String =
        LessonResultFalseLogic.resultTitleComment(sandikBasarisizligi)

    // ── Lottie ──────────────────────────────────────────────────────────────

    /** Sonuç ekranlarının üstünde rastgele oynatılan Lottie dosyaları (assets içindeki adlarıyla). */
    fun animasyonlar(): List<String> = OyunChestResultLogic.animations

    /** `showRandomAnimation`: listeden rastgele bir dosya adı. */
    fun rastgeleAnimasyon(): String = OyunChestResultLogic.animations.random(RastgeleKaynagi.rastgele)

    // ── ChestResult ─────────────────────────────────────────────────────────

    /** Puan çubuğunun tavanı (3 yıldız eşiği); bellekteki ders listesinden çözülür. */
    fun puanTavani(mapFragmentStepIndex: Int): Int =
        DersChestResultLogic.resolveScoreCap(mapFragmentStepIndex, GlobalLessonData.globalPartId)

    /** 2 yıldız eşiği; bellekteki ders listesinden çözülür. */
    fun ikiYildizEsigi(mapFragmentStepIndex: Int, puanTavani: Int): Int =
        DersChestResultLogic.resolveCupPoint2Threshold(mapFragmentStepIndex, puanTavani, GlobalLessonData.globalPartId)

    /** "d:ss" / "s:dd:ss" → saniye; biçim bozuksa 0 (Android: `parseTimeToSeconds(time) ?: 0`). */
    fun sureSaniye(sureMetni: String): Int = OyunChestResultLogic.parseTimeToSeconds(sureMetni) ?: 0

    /** Saniye → "d:ss". */
    fun sureMetni(saniye: Int): String = OyunChestResultLogic.formatSeconds(saniye)

    /** Puan çubuğunun doluluk oranı (0..1). [puanTavani] en az 1 olmalıdır. */
    fun cubukOrani(puan: Int, puanTavani: Int): Float = OyunChestResultLogic.scoreProgress(puan, puanTavani)

    /** Puan çubuğunun rengi: hangi iki renk (#RRGGBB) arasında hangi oranla. */
    fun cubukRengi(puan: Int, oncekiPuan: Int, puanTavani: Int): ScoreBarColorBlend =
        OyunChestResultLogic.scoreBarColorBlend(puan, oncekiPuan, puanTavani)

    /** [sira] (1..3) numaralı yıldızın puan eşiği: 500, 2 yıldız eşiği, tavan. Aralık dışı sırada tavan. */
    fun yildizEsigi(sira: Int, ikiYildizEsigi: Int, puanTavani: Int): Int =
        OyunChestResultLogic.starThresholds(ikiYildizEsigi, puanTavani).getOrElse(sira - 1) { puanTavani }

    /** Yıldız işaretinin çubuk üzerindeki yatay oranı (0..1). */
    fun yildizOrani(esik: Int, puanTavani: Int): Float = OyunChestResultLogic.starMarkerRatio(esik, puanTavani)

    /** Yıldız sesinin hız çarpanı (kademe 1..3). */
    fun yildizSesHizi(kademe: Int): Float = OyunChestResultLogic.starSoundRate(kademe)
}

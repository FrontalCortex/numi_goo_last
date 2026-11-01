package com.example.app.abacus

/**
 * SOBİ TESTİNİN SEVİYE MERDİVENİ — kural başına 1..[MAX] (kullanıcı kararı 01.11.2026: seviye günlük
 * testin başarısından; 20 seviye).
 *
 * Seviye üç şeyden: sayıların basamağı, kaç sayı geldiği, sayıların arası. Basamak arttıkça adet bir
 * geriden başlıyor ki her basamak yeni bir sıçrama olmasın. Toplamada [count] bütün sayılar; çıkarmada
 * başlangıç sayısı + [count] − 1 çıkarılan (ekranda yine [count] sayı görünür).
 *
 * Değişim ([next]): testte o kuralın işlemlerinde %[UP_PCT]+ → oynanan seviyenin bir üstü (seçilen
 * zorluk seviyesinden yüksekse); %[DOWN_PCT] altı → oynanan seviyenin bir altı (mevcut seviyeden
 * düşükse); arası aynı. "Kolay" seçip başarmak seviyeyi şişirmez, "Zor" seçip başarmak hızlandırır.
 */
object SobiTestLevels {

    const val MAX = 20
    const val UP_PCT = 80
    const val DOWN_PCT = 50

    data class Spec(val digits: Int, val count: Int, val intervalMs: Long)

    private val LADDER = listOf(
        Spec(1, 2, 3000), Spec(1, 3, 3000), Spec(2, 2, 2800), Spec(2, 3, 2800), Spec(2, 3, 2400),
        Spec(2, 4, 2400), Spec(2, 4, 2000), Spec(2, 5, 2000), Spec(3, 2, 2400), Spec(3, 3, 2200),
        Spec(3, 3, 1900), Spec(3, 4, 1900), Spec(3, 4, 1600), Spec(3, 5, 1600), Spec(3, 5, 1300),
        Spec(4, 3, 1800), Spec(4, 4, 1600), Spec(4, 4, 1300), Spec(4, 5, 1200), Spec(4, 6, 1000),
    )

    fun spec(level: Int): Spec = LADDER[level.coerceIn(1, MAX) - 1]

    /**
     * Hiç test çözülmemiş kuralın başlangıç seviyesi: kural başarısındaki orandan ([percent];
     * dersler ve ödevler). Yüksek oranlı çocuk 1. seviyede sıkılmasın, düşük oranlı zorlanmasın.
     */
    fun initial(percent: Int): Int = when {
        percent < 60 -> 1
        percent < 85 -> 2
        else -> 3
    }

    /** Testten sonra yeni seviye: [current] kayıtlı, [played] oynanan, [percent] o kuralın işlemlerindeki başarı. */
    fun next(current: Int, played: Int, percent: Int): Int = when {
        percent >= UP_PCT -> maxOf(current, played + 1)
        percent < DOWN_PCT -> minOf(current, played - 1)
        else -> current
    }.coerceIn(1, MAX)
}

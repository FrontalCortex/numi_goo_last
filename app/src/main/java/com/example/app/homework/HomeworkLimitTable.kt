package com.example.app.homework

/**
 * Elde/ödünç gerektiren kurallarda (10'luk ve/veya boncuk) seçilebilecek en fazla sayı adedi.
 *
 * Bu değer hesapla bulunamıyor: sütunlar elde ile birbirine bağlı ve abaküsün sütun sınırı da
 * işin içinde. Bu yüzden motorun ([HomeworkEngine.probeCarryMax]) gerçekten, üst üste ve
 * güvenle üretebildiği değer ölçülüp buraya yazıldı. Motor değişirse tablo yeniden üretilmeli:
 *
 *     $env:GEN_HOMEWORK_TABLE = "1"; .\gradlew testDebugUnitTest --tests "*HomeworkLimitTableGeneratorTest*"
 *
 * Çıktı `app/build/homework_limit_table.txt`; içeriği aşağıdaki [TABLE]'ın yerine konur.
 * HomeworkEngineTest, tablodaki her değerde soruların gerçekten üretildiğini denetliyor.
 *
 * Anahtar: "<işlem>:<a|n>:<kural maskesi>" (a = abaküslü, n = abaküssüz; maske [AbacusRule.maskOf]).
 * Değer: basamak sayısına göre (1. eleman 1 basamak) en fazla adet; 0 = hiç üretilemiyor.
 */
internal object HomeworkLimitTable {

    fun maxCount(op: HomeworkOp, mask: Int, digits: Int, withAbacus: Boolean): Int {
        val key = "${op.key}:${if (withAbacus) "a" else "n"}:$mask"
        return TABLE[key]?.getOrNull(digits - 1) ?: 0
    }

    // Üretildi: 23.10.2026 (HomeworkLimitTableGeneratorTest).
    private val TABLE: Map<String, IntArray> = mapOf(
        "add:a:4" to intArrayOf(5, 5, 5, 5, 0), // ten
        "add:a:5" to intArrayOf(13, 11, 11, 11, 4), // direct+ten
        "add:a:6" to intArrayOf(10, 10, 11, 11, 2), // five+ten
        "add:a:7" to intArrayOf(100, 100, 100, 25, 6), // direct+five+ten
        "add:a:8" to intArrayOf(2, 2, 2, 2, 0), // bead
        "add:a:9" to intArrayOf(9, 9, 9, 9, 5), // direct+bead
        "add:a:10" to intArrayOf(20, 20, 20, 20, 2), // five+bead
        "add:a:11" to intArrayOf(28, 23, 26, 27, 8), // direct+five+bead
        "add:a:12" to intArrayOf(5, 5, 5, 5, 0), // ten+bead
        "add:a:13" to intArrayOf(12, 10, 10, 10, 4), // direct+ten+bead
        "add:a:14" to intArrayOf(100, 100, 100, 18, 2), // five+ten+bead
        "add:a:15" to intArrayOf(100, 100, 100, 23, 5), // direct+five+ten+bead
        "add:n:4" to intArrayOf(5, 5, 5, 5, 5, 5, 5, 0), // ten
        "add:n:5" to intArrayOf(13, 11, 10, 10, 11, 10, 11, 4), // direct+ten
        "add:n:6" to intArrayOf(10, 10, 11, 11, 12, 13, 13, 2), // five+ten
        "add:n:7" to intArrayOf(100, 100, 100, 100, 100, 100, 25, 6), // direct+five+ten
        "add:n:8" to intArrayOf(2, 2, 2, 2, 2, 2, 2, 0), // bead
        "add:n:9" to intArrayOf(9, 9, 9, 9, 9, 9, 9, 5), // direct+bead
        "add:n:10" to intArrayOf(20, 20, 20, 20, 20, 20, 20, 2), // five+bead
        "add:n:11" to intArrayOf(28, 23, 26, 26, 26, 25, 24, 8), // direct+five+bead
        "add:n:12" to intArrayOf(5, 5, 5, 5, 5, 5, 5, 0), // ten+bead
        "add:n:13" to intArrayOf(12, 11, 10, 10, 10, 10, 10, 4), // direct+ten+bead
        "add:n:14" to intArrayOf(100, 100, 100, 100, 100, 100, 17, 2), // five+ten+bead
        "add:n:15" to intArrayOf(100, 100, 100, 100, 100, 100, 23, 5), // direct+five+ten+bead
        "sub:a:4" to intArrayOf(4, 4, 4, 4, 0), // ten
        "sub:a:5" to intArrayOf(20, 17, 17, 9, 3), // direct+ten
        "sub:a:6" to intArrayOf(9, 9, 10, 9, 1), // five+ten
        "sub:a:7" to intArrayOf(100, 100, 100, 22, 5), // direct+five+ten
        "sub:a:8" to intArrayOf(1, 1, 1, 1, 0), // bead
        "sub:a:9" to intArrayOf(9, 9, 9, 9, 4), // direct+bead
        "sub:a:10" to intArrayOf(19, 17, 18, 18, 1), // five+bead
        "sub:a:11" to intArrayOf(25, 19, 22, 23, 8), // direct+five+bead
        "sub:a:12" to intArrayOf(4, 4, 4, 4, 0), // ten+bead
        "sub:a:13" to intArrayOf(19, 16, 16, 9, 3), // direct+ten+bead
        "sub:a:14" to intArrayOf(100, 100, 100, 15, 1), // five+ten+bead
        "sub:a:15" to intArrayOf(100, 100, 100, 20, 5), // direct+five+ten+bead
        "sub:n:4" to intArrayOf(4, 4, 4, 4, 4, 4, 4, 0), // ten
        "sub:n:5" to intArrayOf(19, 17, 17, 16, 17, 17, 9, 3), // direct+ten
        "sub:n:6" to intArrayOf(9, 9, 10, 10, 11, 12, 11, 1), // five+ten
        "sub:n:7" to intArrayOf(100, 100, 100, 100, 100, 100, 22, 5), // direct+five+ten
        "sub:n:8" to intArrayOf(1, 1, 1, 1, 1, 1, 1, 0), // bead
        "sub:n:9" to intArrayOf(9, 9, 9, 9, 9, 9, 9, 4), // direct+bead
        "sub:n:10" to intArrayOf(19, 17, 18, 17, 18, 17, 17, 1), // five+bead
        "sub:n:11" to intArrayOf(25, 19, 22, 22, 21, 20, 20, 7), // direct+five+bead
        "sub:n:12" to intArrayOf(4, 4, 4, 4, 4, 4, 4, 0), // ten+bead
        "sub:n:13" to intArrayOf(18, 16, 16, 16, 16, 16, 9, 3), // direct+ten+bead
        "sub:n:14" to intArrayOf(100, 100, 100, 100, 100, 100, 15, 1), // five+ten+bead
        "sub:n:15" to intArrayOf(100, 100, 100, 100, 100, 100, 20, 5), // direct+five+ten+bead
    )
}

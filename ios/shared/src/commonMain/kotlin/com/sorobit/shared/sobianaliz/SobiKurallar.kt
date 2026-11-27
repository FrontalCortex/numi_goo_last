// Kaynak: app/src/main/java/com/example/app/homework/HomeworkEngine.kt — yalnızca hata analizinin
// kullandığı kısım: HomeworkOp, AbacusRule, AbacusMoves (sütun hareketinin kural sınıflandırması) ve
// HomeworkEngine.pow10 / digitsOf yardımcıları. Ödev üreteci (HomeworkEngine'in geri kalanı) taşınmadı.
// Davranış birebir aynıdır.
package com.sorobit.shared.sobianaliz

enum class HomeworkOp(val key: String) {
    ADD("add"), SUB("sub"), MUL("mul");

    companion object {
        fun fromKey(key: String?): HomeworkOp? = entries.firstOrNull { it.key == key }
    }
}

/** Abaküste bir sütundaki tek hareketin hangi kuralla yapıldığı. */
enum class AbacusRule(val key: String) {
    DIRECT("direct"), FIVE("five"), TEN("ten"), BEAD("bead");

    companion object {
        fun fromKey(key: String?): AbacusRule? = entries.firstOrNull { it.key == key }

        /** Kural kümesinin 4 bitlik kimliği (tablo anahtarı): DIRECT=1, FIVE=2, TEN=4, BEAD=8. */
        fun maskOf(rules: Set<AbacusRule>): Int = rules.fold(0) { acc, r -> acc or (1 shl r.ordinal) }

        fun fromMask(mask: Int): Set<AbacusRule> = entries.filter { mask and (1 shl it.ordinal) != 0 }.toSet()
    }
}

/** Tek sütun hareketinin sınıflandırması. d: sütunun değeri (0..9), x: eklenen/çıkarılan rakam (1..9). */
object AbacusMoves {

    fun classifyAdd(d: Int, x: Int): AbacusRule {
        require(d in 0..9 && x in 1..9) { "d=$d x=$x" }
        val h = d / 5
        val e = d % 5
        if (d + x <= 9) {
            // Taşma yok: ya doğrudan ya beşlikle. (h ve x'in beşliği birlikte 2 olamaz: d+x ≥ 10 olurdu.)
            return if (h + x / 5 <= 1 && e + x % 5 <= 4) AbacusRule.DIRECT else AbacusRule.FIVE
        }
        // Taşma: sola +1, bu sütundan (10 − x) çıkar. Çıkarma doğrudansa 10'luk, değilse boncuk.
        val b = 10 - x
        return if (b / 5 <= h && b % 5 <= e) AbacusRule.TEN else AbacusRule.BEAD
    }

    fun classify(op: HomeworkOp, d: Int, x: Int): AbacusRule =
        if (op == HomeworkOp.ADD) classifyAdd(d, x) else classifySub(d, x)

    fun classifySub(d: Int, x: Int): AbacusRule {
        require(d in 0..9 && x in 1..9) { "d=$d x=$x" }
        val h = d / 5
        val e = d % 5
        if (d >= x) {
            return if (x / 5 <= h && x % 5 <= e) AbacusRule.DIRECT else AbacusRule.FIVE
        }
        // Ödünç: soldan −1, bu sütuna (10 − x) ekle (d + 10 − x < 10, yani ekleme taşmaz).
        val b = 10 - x
        return if (h + b / 5 <= 1 && e + b % 5 <= 4) AbacusRule.TEN else AbacusRule.BEAD
    }
}

/** HomeworkEngine.pow10 / digitsOf (Android'de `internal`). */
internal object SobiSayi {
    fun pow10(exp: Int): Long {
        var out = 1L
        repeat(exp) { out *= 10 }
        return out
    }

    /** Rakam dizisi (0 = birler). */
    fun digitsOf(n: Long, size: Int): IntArray {
        var x = n
        return IntArray(size) { (x % 10).toInt().also { x /= 10 } }
    }
}

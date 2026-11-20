// iOS kopyasına özgü dosya: `java.util.Random`'un (48 bit doğrusal eşlik üreteci, LCG) saf Kotlin
// kopyası. Aynı tohumla java.util.Random ile BİREBİR aynı diziyi üretir (jvmTest'te gerçek sınıfla
// karşılaştırılarak doğrulanır). Android'de `java.util.Random` kullanılan yerlerde bu kullanılır.
package com.sorobit.shared.matematik

class JavaRandom(seed: Long) {
    private var seed: Long = 0L

    init {
        setSeed(seed)
    }

    /** java.util.Random.setSeed: tohum çarpanla XOR'lanıp 48 bite kırpılır. */
    fun setSeed(seed: Long) {
        this.seed = (seed xor MULTIPLIER) and MASK
    }

    /** java.util.Random.next(bits): ilerletilen 48 bit durumun üst [bits] biti. */
    private fun next(bits: Int): Int {
        seed = (seed * MULTIPLIER + ADDEND) and MASK
        return (seed ushr (48 - bits)).toInt()
    }

    fun nextInt(): Int = next(32)

    /** java.util.Random.nextInt(bound): 2'nin kuvveti kısayolu ve reddetme döngüsü dahil. */
    fun nextInt(bound: Int): Int {
        if (bound <= 0) throw IllegalArgumentException("bound must be positive")
        var r = next(31)
        val m = bound - 1
        if ((bound and m) == 0) {
            // bound 2'nin kuvveti
            r = ((bound.toLong() * r.toLong()) shr 31).toInt()
        } else {
            var u = r
            while (true) {
                r = u % bound
                if (u - r + m >= 0) break
                u = next(31)
            }
        }
        return r
    }

    fun nextLong(): Long = (next(32).toLong() shl 32) + next(32)

    fun nextBoolean(): Boolean = next(1) != 0

    fun nextFloat(): Float = next(24) / (1 shl 24).toFloat()

    fun nextDouble(): Double = ((next(26).toLong() shl 27) + next(27)) * DOUBLE_UNIT

    private companion object {
        const val MULTIPLIER = 0x5DEECE66DL
        const val ADDEND = 0xBL
        const val MASK = 0xFFFFFFFFFFFFL // (1L shl 48) - 1
        // 2^-53 (Kotlin/Native const başlatıcıda bölme kabul etmediği için düz val)
        val DOUBLE_UNIT = 1.0 / (1L shl 53)
    }
}

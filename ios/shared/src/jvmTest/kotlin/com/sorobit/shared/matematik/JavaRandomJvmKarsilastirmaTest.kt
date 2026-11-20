package com.sorobit.shared.matematik

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Yalnızca JVM: saf Kotlin [JavaRandom] ile gerçek `java.util.Random` aynı tohumla
 * çok sayıda tohum ve karışık çağrı sırası üzerinde birebir karşılaştırılır.
 */
class JavaRandomJvmKarsilastirmaTest {
    private val tohumlar: List<Long> =
        listOf(0L, 1L, -1L, 42L, Long.MAX_VALUE, Long.MIN_VALUE, 0x5DEECE66DL, (1L shl 48) - 1, 1L shl 48) +
            java.util.Random(2026).let { r -> List(300) { r.nextLong() } }

    @Test
    fun tumMetotlarGercekJavaRandomIleAyni() {
        val sinirlar = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 31, 100, 1000, 1 shl 20, 1_000_000_007, 1_500_000_000, Int.MAX_VALUE)
        for (tohum in tohumlar) {
            val gercek = java.util.Random(tohum)
            val kopya = JavaRandom(tohum)
            repeat(40) {
                assertEquals(gercek.nextInt(), kopya.nextInt(), "nextInt tohum=$tohum")
                for (s in sinirlar) assertEquals(gercek.nextInt(s), kopya.nextInt(s), "nextInt($s) tohum=$tohum")
                assertEquals(gercek.nextLong(), kopya.nextLong(), "nextLong tohum=$tohum")
                assertEquals(gercek.nextBoolean(), kopya.nextBoolean(), "nextBoolean tohum=$tohum")
                assertEquals(gercek.nextDouble(), kopya.nextDouble(), "nextDouble tohum=$tohum")
                assertEquals(gercek.nextFloat(), kopya.nextFloat(), "nextFloat tohum=$tohum")
            }
        }
    }

    @Test
    fun setSeedGercekJavaRandomIleAyni() {
        val gercek = java.util.Random(5)
        val kopya = JavaRandom(5)
        for (tohum in tohumlar) {
            gercek.setSeed(tohum)
            kopya.setSeed(tohum)
            repeat(5) { assertEquals(gercek.nextInt(1000), kopya.nextInt(1000), "tohum=$tohum") }
        }
    }

    @Test
    fun ureteclerinKullandigiCagrilar() {
        // Android: `Random().nextInt(liste.size)` (generateRelatedNumbers0) ve `random.nextBoolean()`
        // (generateMathOperationBeadRule) — tam bu kullanım biçimleri.
        for (tohum in tohumlar) {
            for (boyut in 1..7) {
                assertEquals(java.util.Random(tohum).nextInt(boyut), JavaRandom(tohum).nextInt(boyut))
            }
            assertEquals(java.util.Random(tohum).nextBoolean(), JavaRandom(tohum).nextBoolean())
        }
    }
}

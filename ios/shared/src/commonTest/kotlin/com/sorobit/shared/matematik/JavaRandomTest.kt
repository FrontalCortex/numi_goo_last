package com.sorobit.shared.matematik

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * [JavaRandom]'un `java.util.Random` ile aynı diziyi ürettiğini doğrular.
 * Beklenen değerler gerçek java.util.Random'dan (JDK 21) alınmıştır; çağrı sırası her tohum için:
 * 4×nextInt(), 6×nextInt(10), 4×nextInt(16), 4×nextInt(1_500_000_000), 2×nextLong(),
 * 8×nextBoolean(), 2×nextDouble(), 2×nextFloat().
 * (JVM'de ayrıca jvmTest/JavaRandomJvmKarsilastirmaTest gerçek sınıfla canlı karşılaştırır.)
 */
class JavaRandomTest {
    private class Vektor(
        val tohum: Long,
        val ints: List<Int>,
        val b10: List<Int>,
        val b16: List<Int>,
        val bBuyuk: List<Int>,
        val longs: List<Long>,
        val bools: List<Boolean>,
        val dbl: List<Double>,
        val flt: List<Float>,
    )

    private val vektorler = listOf(
        Vektor(
            0L,
            listOf(-1155484576, -723955400, 1033096058, -1690734402),
            listOf(5, 3, 1, 1, 9, 4),
            listOf(5, 4, 6, 9),
            listOf(49567875, 377907320, 590459143, 672024888),
            listOf(2377732757510138102L, 2704323167362897208L),
            listOf(false, false, true, false, true, true, false, false),
            listOf(0.6251463634655593, 0.4107961954910617),
            listOf(0.77631223f, 0.9766978f),
        ),
        Vektor(
            42L,
            listOf(-1170105035, 234785527, -1360544799, 205897768),
            listOf(0, 5, 5, 8, 9, 3),
            listOf(14, 7, 5, 6),
            listOf(592164476, 1482678032, 995688456, 326327863),
            listOf(8051837266862454915L, -4613416830416070574L),
            listOf(false, true, false, false, true, false, false, false),
            listOf(0.825965871887821, 0.17221793768785243),
            listOf(0.5874274f, 0.27417254f),
        ),
        Vektor(
            -1L,
            listOf(1155099827, 1887904451, 52699159, -1941176418),
            listOf(4, 8, 7, 8, 5, 1),
            listOf(12, 4, 0, 1),
            listOf(176890342, 1481972250, 1201094686, 1286832617),
            listOf(6478582416207549374L, 8379439471558954800L),
            listOf(false, false, false, false, false, true, true, false),
            listOf(0.10549937349985072, 0.6964863627596639),
            listOf(0.3508733f, 0.9448929f),
        ),
        Vektor(
            123456789012345L,
            listOf(-1421006947, -2071311287, -583755257, -1678701992),
            listOf(5, 8, 0, 9, 9, 4),
            listOf(13, 9, 11, 1),
            listOf(600863488, 1442513241, 1448108067, 496414028),
            listOf(-6059919002218650048L, 1792389284863968299L),
            listOf(true, false, false, false, false, true, true, false),
            listOf(0.5593298290176115, 0.6849941900335123),
            listOf(0.9406746f, 0.077147186f),
        ),
    )

    @Test
    fun javaUtilRandomIleAyniDizi() {
        for (v in vektorler) {
            val r = JavaRandom(v.tohum)
            assertEquals(v.ints, List(4) { r.nextInt() }, "nextInt tohum=${v.tohum}")
            assertEquals(v.b10, List(6) { r.nextInt(10) }, "nextInt(10) tohum=${v.tohum}")
            assertEquals(v.b16, List(4) { r.nextInt(16) }, "nextInt(16) tohum=${v.tohum}")
            assertEquals(v.bBuyuk, List(4) { r.nextInt(1_500_000_000) }, "nextInt(büyük) tohum=${v.tohum}")
            assertEquals(v.longs, List(2) { r.nextLong() }, "nextLong tohum=${v.tohum}")
            assertEquals(v.bools, List(8) { r.nextBoolean() }, "nextBoolean tohum=${v.tohum}")
            assertEquals(v.dbl, List(2) { r.nextDouble() }, "nextDouble tohum=${v.tohum}")
            assertEquals(v.flt, List(2) { r.nextFloat() }, "nextFloat tohum=${v.tohum}")
        }
    }

    @Test
    fun tohumYalnizca48BitKullanir() {
        // java.util.Random tohumu 48 bite kırpar: Long.MIN_VALUE ile 0 aynı diziyi verir.
        val a = JavaRandom(Long.MIN_VALUE)
        val b = JavaRandom(0L)
        assertEquals(List(10) { a.nextInt() }, List(10) { b.nextInt() })
    }

    @Test
    fun setSeedDiziyiBastanBaslatir() {
        val r = JavaRandom(7L)
        val ilk = List(5) { r.nextInt(100) }
        r.setSeed(7L)
        assertEquals(ilk, List(5) { r.nextInt(100) })
    }

    @Test
    fun nextIntSinirKontrolu() {
        val r = JavaRandom(1L)
        assertFailsWith<IllegalArgumentException> { r.nextInt(0) }
        assertFailsWith<IllegalArgumentException> { r.nextInt(-5) }
        repeat(2000) {
            assertEquals(0, r.nextInt(1))
            assertTrue(r.nextInt(7) in 0..6)
            assertTrue(r.nextInt(8) in 0..7)
            val d = r.nextDouble()
            assertTrue(d >= 0.0 && d < 1.0)
            val f = r.nextFloat()
            assertTrue(f >= 0f && f < 1f)
        }
    }

    @Test
    fun yeniJavaRandomTohumuKaynaktanCeker() {
        // Aynı tohumlu kaynak → aynı JavaRandom dizisi; ardışık iki çağrı → farklı örnekler.
        RastgeleKaynagi.rastgele = Random(99)
        val a1 = RastgeleKaynagi.yeniJavaRandom()
        val a2 = RastgeleKaynagi.yeniJavaRandom()
        RastgeleKaynagi.rastgele = Random(99)
        val b1 = RastgeleKaynagi.yeniJavaRandom()
        RastgeleKaynagi.varsayilanaDon()
        val d1 = List(8) { a1.nextInt() }
        assertEquals(d1, List(8) { b1.nextInt() })
        assertTrue(d1 != List(8) { a2.nextInt() })
        assertEquals(Random.Default, RastgeleKaynagi.rastgele)
        // Beklenen tohum: kaynağın ilk nextLong() değeri
        val beklenen = JavaRandom(Random(99).nextLong())
        RastgeleKaynagi.rastgele = Random(99)
        val c = RastgeleKaynagi.yeniJavaRandom()
        RastgeleKaynagi.varsayilanaDon()
        assertEquals(List(4) { beklenen.nextInt(50) }, List(4) { c.nextInt(50) })
    }
}

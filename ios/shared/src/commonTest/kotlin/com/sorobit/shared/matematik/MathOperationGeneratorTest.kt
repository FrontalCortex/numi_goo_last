package com.sorobit.shared.matematik

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Üretecin her modu için özellik testleri: kurallar Android kaynak dosyası
 * (MathOperationGenerator.kt) okunarak elle türetilmiştir. Tohum sabit olduğu için
 * sonuçlar her platformda aynıdır (kararsız test olmaz).
 */
class MathOperationGeneratorTest {
    private val M = MathOperationGenerator

    private fun tohumla(tohum: Int = 20261121, blok: () -> Unit) {
        RastgeleKaynagi.rastgele = Random(tohum)
        try {
            blok()
        } finally {
            RastgeleKaynagi.varsayilanaDon()
        }
    }

    private fun tekrar(n: Int = 6000, blok: () -> Unit) = tohumla { repeat(n) { blok() } }

    /** Sayının rakamları (soldan sağa); [uzunluk] verilirse soldan sıfırla doldurulur. */
    private fun rk(n: Int, uzunluk: Int = 0): List<Int> = n.toString().padStart(uzunluk, '0').map { it - '0' }

    private val B19 = (1..9).toList()
    private val B09 = (0..9).toList()

    /** Anahtar rakam → görülen değerler; hem üyelik hem de "tablonun tamamı çıkıyor mu" denetimi. */
    private class Destek(val ad: String) {
        val gorulen = mutableMapOf<Int, MutableSet<Int>>()

        fun gor(anahtar: Int, deger: Int, izinli: Collection<Int>) {
            assertTrue(deger in izinli, "$ad: anahtar=$anahtar değer=$deger izinli=$izinli")
            gorulen.getOrPut(anahtar) { mutableSetOf() }.add(deger)
        }

        /** [erisilmez]: kuralların yapısı gereği toplamda hiç oluşamayan anahtar rakamlar. */
        fun tamMi(tablo: Map<Int, List<Int>>, erisilmez: Set<Int> = emptySet()) {
            for ((k, v) in tablo) {
                if (k in erisilmez) {
                    assertTrue(k !in gorulen, "$ad: anahtar=$k hiç oluşmamalı")
                    continue
                }
                assertEquals<Set<Int>?>(v.toSet(), gorulen[k], "$ad: anahtar=$k için tablodaki tüm değerler görülmeli")
            }
        }
    }

    private fun MathOperation.a(): Int = firstNumber!!
    private fun MathOperation.b(): Int = secondNumber!!

    /** Listelerdeki "havuz bitince karışık yeniden dolar" kuralı: her parça havuzun tekrarsız alt kümesi. */
    private fun havuzKontrol(degerler: List<Int>, havuz: List<Int>) {
        degerler.chunked(havuz.size).forEach { parca ->
            assertEquals(parca.size, parca.toSet().size, "parça içinde tekrar olmamalı: $parca")
            assertTrue(parca.all { it in havuz })
            if (parca.size == havuz.size) assertEquals(havuz.toSet(), parca.toSet())
        }
    }

    // ------------------------------------------------------------------ Toplama

    private val related0Tablo = mapOf(
        1 to listOf(1, 2, 3, 5, 6, 7, 8), 2 to listOf(1, 2, 5, 6, 7), 3 to listOf(1, 5, 6), 4 to listOf(5),
        5 to listOf(1, 2, 3, 4), 6 to listOf(1, 2, 3), 7 to listOf(1, 2), 8 to listOf(1),
    )

    @Test
    fun generateRelatedNumbers0() {
        val d = Destek("related0")
        tekrar {
            // Parametreler Android'de de kullanılmıyor: her zaman tek basamak.
            val op = M.generateRelatedNumbers0(3, 2)
            assertEquals("+", op.operator)
            assertTrue(op.a() in 1..8)
            d.gor(op.a(), op.b(), related0Tablo.getValue(op.a()))
        }
        d.tamMi(related0Tablo)
    }

    private val digitRules = mapOf(
        1 to listOf(0, 1, 2, 3, 5, 6, 7, 8), 2 to listOf(0, 1, 2, 5, 6, 7), 3 to listOf(0, 1, 5, 6), 4 to listOf(0, 5),
        5 to listOf(0, 1, 2, 3, 4), 6 to listOf(0, 1, 2, 3), 7 to listOf(0, 1, 2), 8 to listOf(0, 1), 9 to listOf(0),
    )

    private fun relatedKontrol(op: MathOperation, f: Int, s: Int) {
        assertEquals("+", op.operator)
        val a = rk(op.a())
        assertEquals(f, a.size, "ilk sayı $f basamaklı olmalı: ${op.a()}")
        assertTrue(a[0] in 1..8)
        assertTrue(a.all { it in 0..8 }, "ilk sayıda 9 olmaz: ${op.a()}")
        val b = rk(op.b(), s)
        assertEquals(s, b.size)
        for (i in 0 until s) { // i: birler basamağından itibaren
            val sd = b[s - 1 - i]
            val sifirsiz = i == 1 || s == 1 || i == s - 1
            val izinli = if (i < a.size) {
                val t = digitRules[a[a.size - 1 - i]] ?: listOf(0)
                if (sifirsiz) t.filter { it != 0 }.ifEmpty { listOf(1) } else t
            } else {
                if (sifirsiz) B19 else B09
            }
            assertTrue(sd in izinli, "related($f,$s): $op basamak=$i sd=$sd izinli=$izinli")
        }
        // En soldaki basamak sıfır olamaz → ikinci sayı tam s basamaklı
        assertEquals(s, op.b().toString().length)
    }

    @Test
    fun generateRelatedNumbers() {
        for ((f, s) in listOf(1 to 1, 2 to 2, 3 to 2, 2 to 3, 3 to 3, 4 to 4, 2 to 1, 1 to 2, 5 to 4)) {
            tekrar(3000) { relatedKontrol(M.generateRelatedNumbers(f, s), f, s) }
        }
    }

    @Test
    fun generateRelatedNumbersTekBasamakTamDestek() {
        // Tek basamakta: ilk 1..8, ikinci = digitRules'un sıfırsız hali (related0 tablosu ile aynı)
        val d = Destek("related(1,1)")
        tekrar { M.generateRelatedNumbers(1, 1).let { d.gor(it.a(), it.b(), related0Tablo.getValue(it.a())) } }
        d.tamMi(related0Tablo)
    }

    @Test
    fun generateRelatedNumbersList() = tohumla {
        assertEquals(emptyList(), M.generateRelatedNumbersList(0, 2, 2))
        assertEquals(emptyList(), M.generateRelatedNumbersList(-5, 2, 2))
        repeat(300) {
            // Havuz (1..8) yeterli: ilk sayılar tekrarsız
            val bes = M.generateRelatedNumbersList(5, 1, 1)
            assertEquals(5, bes.size)
            assertEquals(5, bes.map { it.a() }.toSet().size)
            bes.forEach { relatedKontrol(it, 1, 1) }
            // Havuzun tamamı
            val sekiz = M.generateRelatedNumbersList(8, 1, 1)
            assertEquals((1..8).toSet(), sekiz.map { it.a() }.toSet())
            // Havuz yetmez: ilk 8'i tekrarsız, sonrası tekrara düşer
            val onIki = M.generateRelatedNumbersList(12, 1, 1)
            assertEquals(12, onIki.size)
            assertEquals((1..8).toSet(), onIki.take(8).map { it.a() }.toSet())
            onIki.forEach { relatedKontrol(it, 1, 1) }
            val ikiBasamak = M.generateRelatedNumbersList(10, 2, 2)
            assertEquals(10, ikiBasamak.map { it.a() }.toSet().size)
            ikiBasamak.forEach { relatedKontrol(it, 2, 2) }
        }
    }

    private val related2Tablo = mapOf(
        1 to listOf(1, 2, 3, 4, 5, 6, 7, 8), 2 to listOf(1, 2, 3, 4, 5, 6, 7), 3 to listOf(1, 2, 3, 4, 5, 6),
        4 to listOf(1, 2, 3, 4, 5), 5 to listOf(1, 2, 3, 4), 6 to listOf(1, 2, 3), 7 to listOf(1, 2), 8 to listOf(1),
        9 to listOf(0),
    )

    @Test
    fun generateRelatedNumbers2() {
        for ((f, s) in listOf(1 to 1, 2 to 2, 3 to 3, 2 to 3, 3 to 2)) {
            val d = Destek("related2($f,$s)")
            var dusuk = 0
            var toplam = 0
            var birIcinDort = 0
            var bir = 0
            tekrar(8000) {
                val op = M.generateRelatedNumbers2(f, s)
                assertEquals("+", op.operator)
                val a = rk(op.a())
                assertEquals(f, a.size)
                assertTrue(a.all { it in 1..9 })
                a.forEach { toplam++; if (it <= 4) dusuk++ }
                val b = rk(op.b(), s)
                assertEquals(s, b.size)
                for (i in 0 until s) {
                    val sd = b[s - 1 - i]
                    if (i < a.size) {
                        val fd = a[a.size - 1 - i]
                        d.gor(fd, sd, related2Tablo.getValue(fd))
                        // Elde oluşmaz
                        assertTrue(fd + sd <= 9)
                        if (fd == 1) {
                            bir++
                            if (sd == 4) birIcinDort++
                        }
                    } else {
                        assertTrue(sd in 0..9)
                    }
                }
            }
            d.tamMi(related2Tablo)
            // İlk sayının rakamları %80 ihtimalle 1..4
            val oran = dusuk.toDouble() / toplam
            assertTrue(oran > 0.77 && oran < 0.83, "düşük rakam oranı $oran")
            // İlk rakam 1 ise %80 (+ kalan %20'nin 1/8'i) ihtimalle 4
            val dortOran = birIcinDort.toDouble() / bir
            assertTrue(dortOran > 0.78 && dortOran < 0.87, "1→4 oranı $dortOran")
        }
    }

    private val blindingTablo = mapOf(
        0 to listOf(1, 2, 3, 4), 1 to listOf(1, 2, 3, 4), 2 to listOf(1, 2, 3, 4), 3 to listOf(1, 2, 3, 4),
        4 to listOf(1, 2, 3, 4), 5 to listOf(1, 2, 3, 4), 6 to listOf(1, 2, 3), 7 to listOf(1, 2), 8 to listOf(1),
        9 to listOf(0),
    )

    @Test
    fun generateRelatedNumbers2Blinding() = tohumla {
        assertEquals(emptyList(), M.generateRelatedNumbers2Blinding(0, 2))
        assertEquals(emptyList(), M.generateRelatedNumbers2Blinding(-1, 2))
        var dusuk = 0
        var toplam = 0
        for ((count, digits) in listOf(1 to 1, 5 to 1, 5 to 2, 8 to 3, 12 to 2, 4 to 0, 3 to -2)) {
            val dc = if (digits < 1) 1 else digits
            repeat(1500) {
                val dizi = M.generateRelatedNumbers2Blinding(count, digits)
                assertEquals(count, dizi.size)
                val ilk = rk(dizi[0])
                assertEquals(dc, ilk.size)
                assertTrue(ilk.all { it in 1..9 })
                ilk.forEach { toplam++; if (it <= 4) dusuk++ }
                for (i in 1 until dizi.size) {
                    val sumD = rk(dizi.take(i).sum(), dc).takeLast(dc)
                    val yeni = rk(dizi[i], dc)
                    assertEquals(dc, yeni.size)
                    for (p in 0 until dc) {
                        assertTrue(yeni[p] in blindingTablo.getValue(sumD[p]), "blinding: toplam=$sumD yeni=$yeni")
                        // Hiçbir sütunda elde oluşmaz
                        assertTrue(sumD[p] + yeni[p] <= 9)
                    }
                }
            }
        }
        val oran = dusuk.toDouble() / toplam
        assertTrue(oran > 0.88 && oran < 0.92, "ilk sayı düşük rakam oranı $oran (beklenen ~0.90)")
    }

    private val besKuraliBirler = mapOf(1 to listOf(4), 2 to listOf(3, 4), 3 to listOf(2, 3, 4), 4 to listOf(1, 2, 3, 4))

    @Test
    fun generateRandomMathOperation1() {
        val onlar = Destek("op1 onlar")
        val aktif = Destek("op1 birler (5 kuralı onlarda)")
        val pasif = Destek("op1 birler")
        val onlarTablo = mapOf(
            1 to listOf(1, 2, 3, 4, 5, 6, 7, 8), 2 to listOf(1, 2, 3, 4, 5, 6, 7), 3 to listOf(1, 2, 3, 4, 5, 6),
            4 to listOf(1, 2, 3, 4, 5), 5 to listOf(1, 2, 3, 4), 6 to listOf(1, 2, 3), 7 to listOf(1, 2), 8 to listOf(1),
            9 to listOf(0),
        )
        val aktifTablo = related0Tablo + (9 to listOf(0))
        tekrar(20000) {
            val op = M.generateRandomMathOperation1()
            assertEquals("+", op.operator)
            val ft = op.a() / 10
            val fo = op.a() % 10
            val st = op.b() / 10
            val so = op.b() % 10
            assertTrue(ft in 1..9)
            onlar.gor(ft, st, onlarTablo.getValue(ft))
            val besKuraliAktif = ft < 5 && st <= 4 && ft + st >= 5
            if (besKuraliAktif) {
                assertTrue(fo in 1..9)
                aktif.gor(fo, so, aktifTablo.getValue(fo))
            } else {
                assertTrue(fo in 1..4)
                pasif.gor(fo, so, besKuraliBirler.getValue(fo))
            }
            // Onlar basamağında elde oluşmaz
            assertTrue(ft + st <= 9)
        }
        onlar.tamMi(onlarTablo)
        aktif.tamMi(aktifTablo)
        pasif.tamMi(besKuraliBirler)
    }

    private val ONLAR_49_HARIC = listOf(1, 2, 3, 5, 6, 7, 8)

    private fun op10Kontrol(op: MathOperation, d: Destek) {
        assertEquals("+", op.operator)
        assertTrue(op.b() in 1..5)
        assertTrue(op.a() / 10 in ONLAR_49_HARIC)
        d.gor(op.b(), op.a() % 10, ((10 - op.b())..9).toList())
        // Birlerde her zaman 10'a tamamlama (elde) gerekir
        assertTrue(op.a() % 10 + op.b() >= 10)
    }

    @Test
    fun generateMathOperationVeListesi() {
        val d = Destek("op10")
        val onlar = mutableSetOf<Int>()
        tekrar {
            val op = M.generateMathOperation()
            op10Kontrol(op, d)
            onlar.add(op.a() / 10)
        }
        d.tamMi((1..5).associateWith { ((10 - it)..9).toList() })
        assertEquals(ONLAR_49_HARIC.toSet(), onlar)
        tohumla {
            assertEquals(emptyList(), M.generateMathOperationList(0))
            assertEquals(emptyList(), M.generateMathOperationList(-2))
            repeat(500) {
                for (count in listOf(1, 5, 7, 12)) {
                    val liste = M.generateMathOperationList(count)
                    assertEquals(count, liste.size)
                    liste.forEach { op10Kontrol(it, Destek("liste")) }
                    havuzKontrol(liste.map { it.b() }, (1..5).toList())
                }
            }
        }
    }

    @Test
    fun generateMathOperation2() {
        val d = Destek("op2")
        val onlar = mutableSetOf<Int>()
        tekrar {
            val op = M.generateMathOperation2()
            assertEquals("+", op.operator)
            onlar.add(op.a() / 10)
            val o = op.a() % 10
            assertTrue(o in 5..9)
            d.gor(o, op.b(), ((10 - o)..5).toList())
        }
        assertEquals(setOf(4, 9), onlar)
        d.tamMi((5..9).associateWith { ((10 - it)..5).toList() })
    }

    @Test
    fun generateMathOperationWithDigits() {
        for ((f, s) in listOf(1 to 1, 2 to 2, 3 to 3, 3 to 2, 4 to 1)) {
            val d = Destek("withDigits($f,$s)")
            tekrar(4000) {
                val op = M.generateMathOperationWithDigits(f, s)
                assertEquals("+", op.operator)
                val a = rk(op.a())
                assertEquals(f, a.size)
                assertTrue(a.all { it in 5..9 })
                val b = rk(op.b())
                assertEquals(s, b.size)
                // Android'deki gibi: ikinci sayının rakamları ilk sayının SOLDAN rakamlarıyla eşleşir.
                for (i in 0 until s) d.gor(a[i], b[i], ((10 - a[i])..5).toList())
            }
            d.tamMi((5..9).associateWith { ((10 - it)..5).toList() })
        }
        // İkinci sayı daha uzunsa Android'de de dizin taşması olur.
        tohumla { assertFailsWith<IndexOutOfBoundsException> { M.generateMathOperationWithDigits(2, 3) } }
    }

    private val op3Tablo = mapOf(
        6 to listOf(4, 9), 7 to listOf(3, 4, 8, 9), 8 to listOf(2, 3, 4, 7, 8, 9), 9 to listOf(1, 2, 3, 6, 7, 8, 9),
    )

    @Test
    fun generateMathOperation3VeListesi() {
        val d = Destek("op3")
        val onlar = mutableSetOf<Int>()
        tekrar {
            val op = M.generateMathOperation3()
            assertEquals("+", op.operator)
            assertTrue(op.b() in 6..9)
            onlar.add(op.a() / 10)
            d.gor(op.b(), op.a() % 10, op3Tablo.getValue(op.b()))
        }
        assertEquals(ONLAR_49_HARIC.toSet(), onlar)
        d.tamMi(op3Tablo)
        tohumla {
            assertEquals(emptyList(), M.generateMathOperationList3(0))
            repeat(500) {
                for (count in listOf(1, 4, 6, 10)) {
                    val liste = M.generateMathOperationList3(count)
                    assertEquals(count, liste.size)
                    havuzKontrol(liste.map { it.b() }, (6..9).toList())
                    liste.forEach {
                        assertTrue(it.a() / 10 in ONLAR_49_HARIC)
                        assertTrue(it.a() % 10 in op3Tablo.getValue(it.b()))
                    }
                }
            }
        }
    }

    @Test
    fun generateMathOperation4VeListesi() {
        // Tekli sürümde 4 → [6,7,8] (9 yok); liste sürümü ise op3 tablosunu kullanır. Android'deki fark aynen korunur.
        val tablo = mapOf(
            1 to listOf(9), 2 to listOf(8, 9), 3 to listOf(7, 8, 9), 4 to listOf(6, 7, 8), 5 to listOf(5),
            6 to listOf(9), 7 to listOf(8, 9), 8 to listOf(7, 8, 9), 9 to listOf(6, 7, 8, 9),
        )
        val d = Destek("op4")
        val onlar = mutableSetOf<Int>()
        tekrar {
            val op = M.generateMathOperation4()
            assertEquals("+", op.operator)
            onlar.add(op.a() / 10)
            d.gor(op.a() % 10, op.b(), tablo.getValue(op.a() % 10))
        }
        assertEquals(setOf(4, 9), onlar)
        d.tamMi(tablo)
        val dl = Destek("op4 liste")
        tohumla {
            assertEquals(emptyList(), M.generateMathOperationList4(0))
            repeat(800) {
                for (count in listOf(1, 4, 6, 10)) {
                    val liste = M.generateMathOperationList4(count)
                    assertEquals(count, liste.size)
                    havuzKontrol(liste.map { it.b() }, (6..9).toList())
                    liste.forEach {
                        assertEquals("+", it.operator)
                        assertTrue(it.a() / 10 in listOf(4, 9))
                        dl.gor(it.b(), it.a() % 10, op3Tablo.getValue(it.b()))
                    }
                }
            }
        }
        dl.tamMi(op3Tablo)
    }

    @Test
    fun generateMathOperationWithDigits2() {
        val tablo = mapOf(
            1 to listOf(9), 2 to listOf(8, 9), 3 to listOf(7, 8, 9), 4 to listOf(6, 7, 8, 9), 5 to listOf(5),
            6 to listOf(9), 7 to listOf(8, 9), 8 to listOf(7, 8, 9), 9 to listOf(6, 7, 8, 9),
        )
        for ((f, s) in listOf(1 to 1, 2 to 2, 3 to 2, 3 to 3, 2 to 3, 1 to 3)) {
            val d = Destek("withDigits2($f,$s)")
            tekrar(4000) {
                val op = M.generateMathOperationWithDigits2(f, s)
                assertEquals("+", op.operator)
                val a = rk(op.a())
                assertEquals(f, a.size)
                assertTrue(a.all { it in 1..9 })
                val b = rk(op.b())
                assertEquals(s, b.size)
                // Sağdan hizalı; ilk sayıda karşılığı olmayan (soldaki) basamaklar 9 kabul edilir.
                for (i in 0 until s) {
                    val kaynak = f - s + i
                    val fd = if (kaynak in 0 until f) a[kaynak] else 9
                    d.gor(fd, b[i], tablo.getValue(fd))
                }
            }
            if (f >= 2) d.tamMi(tablo)
        }
    }

    @Test
    fun generateMathOperationWithDigits3() {
        val tablo = mapOf(
            1 to B19, 2 to B19, 3 to B19, 4 to B19, 5 to listOf(1, 2, 3, 4, 5), 6 to listOf(1, 2, 3, 4, 5, 9),
            7 to listOf(1, 2, 3, 4, 5, 8, 9), 8 to listOf(1, 2, 3, 4, 5, 7, 8, 9), 9 to B19,
        )
        for ((f, s) in listOf(1 to 1, 2 to 2, 3 to 3, 3 to 1)) {
            val d = Destek("withDigits3($f,$s)")
            tekrar(5000) {
                val op = M.generateMathOperationWithDigits3(f, s)
                assertEquals("+", op.operator)
                val a = rk(op.a())
                assertEquals(f, a.size)
                assertTrue(a.all { it in 1..9 })
                val b = rk(op.b())
                assertEquals(s, b.size)
                for (i in 0 until s) d.gor(a[i], b[i], tablo.getValue(a[i]))
            }
            d.tamMi(tablo)
        }
        tohumla { assertFailsWith<IndexOutOfBoundsException> { M.generateMathOperationWithDigits3(1, 2) } }
    }

    @Test
    fun generateMathOperationBeadRule() {
        val tablo = mapOf(8 to listOf(6), 7 to listOf(6, 7), 6 to listOf(6, 7, 8), 5 to listOf(6, 7, 8, 9))
        val d = Destek("beadRule")
        val onlar = mutableSetOf<Int>()
        var bes = 0
        var besDokuz = 0
        tekrar(12000) {
            val op = M.generateMathOperationBeadRule()
            assertEquals("+", op.operator)
            onlar.add(op.a() / 10)
            val o = op.a() % 10
            d.gor(o, op.b(), tablo.getValue(o))
            if (o == 5) {
                bes++
                if (op.b() == 9) besDokuz++
            }
        }
        assertEquals(ONLAR_49_HARIC.toSet(), onlar)
        d.tamMi(tablo)
        // Birler 5 iken java.util.Random.nextBoolean() ile %50 ihtimalle 9
        val oran = besDokuz.toDouble() / bes
        assertTrue(oran > 0.46 && oran < 0.54, "5→9 oranı $oran")
    }

    @Test
    fun generateMathOperationWithDigitsBeadRule() {
        for ((f, s) in listOf(1 to 1, 2 to 2, 3 to 3, 3 to 2)) {
            val ikinci = mutableSetOf<Int>()
            tekrar(3000) {
                val op = M.generateMathOperationWithDigitsBeadRule(f, s)
                assertEquals("+", op.operator)
                val a = rk(op.a())
                assertEquals(f, a.size)
                assertTrue(a.all { it in 5..8 })
                val b = rk(op.b())
                assertEquals(s, b.size)
                assertTrue(b.all { it in 6..9 })
                ikinci.addAll(b)
            }
            assertEquals(setOf(6, 7, 8, 9), ikinci)
        }
        tohumla { assertFailsWith<IndexOutOfBoundsException> { M.generateMathOperationWithDigitsBeadRule(1, 2) } }
    }

    // ------------------------------------------------------------------ Çıkarma (tek işlem)

    @Test
    fun irregularExtraction() {
        val tablo = mapOf(
            1 to listOf(1), 2 to listOf(2, 1), 3 to listOf(3, 2, 1), 4 to listOf(4, 3, 2, 1), 5 to listOf(5),
            6 to listOf(6, 5, 1), 7 to listOf(7, 6, 5, 2, 1), 8 to listOf(8, 7, 6, 5, 3, 2, 1),
            9 to listOf(9, 8, 7, 6, 5, 4, 3, 2, 1),
        )
        for ((f, s) in listOf(1 to 1, 2 to 2, 3 to 2, 3 to 3)) {
            val d = Destek("irregular($f,$s)")
            tekrar(6000) {
                val op = M.irregularExtraction(f, s)
                assertEquals("-", op.operator)
                val a = rk(op.a())
                assertEquals(f, a.size)
                assertTrue(a.all { it in 1..9 })
                val b = rk(op.b())
                assertEquals(s, b.size)
                for (i in 0 until s) d.gor(a[i], b[i], tablo.getValue(a[i]))
                assertTrue(op.a() != op.b())
                assertTrue(op.a() > op.b(), "sonuç negatif/sıfır olmamalı: $op")
            }
            if (f == 1) {
                // Tek basamakta ilk == ikinci olan sonuçlar elenir: 1 ve 5 hiç çıkamaz, x→x görülmez.
                assertTrue(1 !in d.gorulen && 5 !in d.gorulen)
                for ((k, v) in d.gorulen) assertEquals<Set<Int>>(tablo.getValue(k).toSet() - k, v)
            } else {
                d.tamMi(tablo)
            }
        }
    }

    @Test
    fun irregularExtractionFiveRules() {
        val tablo = mapOf(5 to listOf(4, 3, 2, 1), 6 to listOf(4, 3, 2), 7 to listOf(4, 3), 8 to listOf(4))
        for ((f, s) in listOf(1 to 1, 2 to 2, 3 to 3, 2 to 1)) {
            val d = Destek("fiveRules($f,$s)")
            tekrar(3000) {
                val op = M.irregularExtractionFiveRules(f, s)
                assertEquals("-", op.operator)
                val a = rk(op.a())
                assertEquals(f, a.size)
                assertTrue(a.all { it in 5..8 })
                val b = rk(op.b())
                assertEquals(s, b.size)
                for (i in 0 until s) {
                    d.gor(a[i], b[i], tablo.getValue(a[i]))
                    // 5 kuralı: çıkan ≤ 4 ve sonuç basamağı 5'in altına düşer
                    assertTrue(a[i] - b[i] in 1..4)
                }
            }
            d.tamMi(tablo)
        }
        tohumla { assertFailsWith<IndexOutOfBoundsException> { M.irregularExtractionFiveRules(1, 2) } }
    }

    @Test
    fun irregularExtractionFiveRulesMix() {
        for ((f, s) in listOf(2 to 2, 3 to 3, 3 to 2)) {
            val d = Destek("fiveRulesMix($f,$s)")
            var yuksek = 0
            var toplam = 0
            tekrar(8000) {
                val op = M.irregularExtractionFiveRulesMix(f, s)
                assertEquals("-", op.operator)
                val a = rk(op.a())
                assertEquals(f, a.size)
                assertTrue(a.all { it in 1..9 })
                a.forEach { toplam++; if (it >= 5) yuksek++ }
                val b = rk(op.b())
                assertEquals(s, b.size)
                // Her tabloda (7 ve 8'in iki alt listesi dahil) izinli küme 1..rakam
                for (i in 0 until s) d.gor(a[i], b[i], (1..a[i]).toList())
                assertTrue(op.a() != op.b())
                assertTrue(op.a() > op.b())
            }
            d.tamMi((1..9).associateWith { (1..it).toList() })
            val oran = yuksek.toDouble() / toplam
            assertTrue(oran > 0.87 && oran < 0.93, "yüksek rakam oranı $oran (beklenen ~0.90)")
        }
    }

    private val cikarmaSecenek = mapOf(
        0 to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9), 1 to listOf(2, 3, 4, 5, 7, 8, 9), 2 to listOf(3, 4, 5, 8, 9),
        3 to listOf(4, 5, 9), 4 to listOf(5), 5 to listOf(6, 7, 8, 9), 6 to listOf(7, 8, 9), 7 to listOf(8, 9),
        8 to listOf(9),
    )
    private val ONLAR_5_HARIC = listOf(1, 2, 3, 4, 6, 7, 8, 9)

    @Test
    fun extractionGenerateMathOperationVeListesi() {
        val d = Destek("çıkarma10")
        val onlar = mutableSetOf<Int>()
        tekrar(10000) {
            val op = M.extractionGenerateMathOperation()
            assertEquals("-", op.operator)
            onlar.add(op.a() / 10)
            d.gor(op.a() % 10, op.b(), cikarmaSecenek.getValue(op.a() % 10))
            // Birlerden her zaman ödünç alınır (10 kuralı)
            assertTrue(op.b() > op.a() % 10)
            assertTrue(op.a() - op.b() > 0)
        }
        assertEquals(ONLAR_5_HARIC.toSet(), onlar)
        d.tamMi(cikarmaSecenek)

        val dl = Destek("çıkarma10 liste")
        tohumla {
            assertEquals(emptyList(), M.extractionGenerateMathOperationList(0))
            assertEquals(emptyList(), M.extractionGenerateMathOperationList(-1))
            repeat(1500) {
                for (count in listOf(1, 9, 20)) {
                    val liste = M.extractionGenerateMathOperationList(count)
                    assertEquals(count, liste.size)
                    // İkinci sayılar karıştırılmadan 1..9 sırayla döner
                    assertEquals(List(count) { it % 9 + 1 }, liste.map { it.b() })
                    liste.forEach {
                        assertEquals("-", it.operator)
                        assertTrue(it.a() / 10 in ONLAR_5_HARIC)
                        val o = it.a() % 10
                        dl.gor(o, it.b(), cikarmaSecenek.getValue(o))
                    }
                }
            }
        }
        dl.tamMi(cikarmaSecenek)
    }

    @Test
    fun extractionGenerateMathOperationTen() {
        val dt = Destek("ten onlar")
        val dob = Destek("ten birler")
        val yuzler = mutableSetOf<Int>()
        tekrar(15000) {
            val op = M.extractionGenerateMathOperationTen()
            assertEquals("-", op.operator)
            val (h, t, o) = rk(op.a(), 3)
            yuzler.add(h)
            assertTrue(t in 0..8 && o in 0..8)
            assertTrue(op.b() in 11..99)
            dt.gor(t, op.b() / 10, cikarmaSecenek.getValue(t))
            dob.gor(o, op.b() % 10, cikarmaSecenek.getValue(o))
        }
        assertEquals(ONLAR_5_HARIC.toSet(), yuzler)
        dt.tamMi(cikarmaSecenek)
        dob.tamMi(cikarmaSecenek)
    }

    private val digerKurallarOnlar = mapOf(
        0 to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), 1 to listOf(1, 2, 3, 4, 5, 7, 8, 9), 2 to listOf(2, 3, 4, 5, 8, 9),
        3 to listOf(3, 4, 5, 9), 4 to listOf(4, 5), 5 to listOf(5, 1, 2, 3, 4, 6, 7, 8, 9),
        6 to listOf(6, 1, 2, 3, 4, 5, 7, 8, 9), 7 to listOf(7, 2, 1, 3, 4, 5, 6, 8, 9),
        8 to listOf(8, 3, 1, 2, 4, 5, 6, 7, 9),
        // Android'de 9 için ikinci liste [1,2,3,5,6,7] — 8 yok; aynen korunur.
        9 to listOf(9, 4, 1, 2, 3, 5, 6, 7),
    )

    private fun digerKurallarKontrol(op: MathOperation, dt: Destek, dob: Destek): Int {
        assertEquals("-", op.operator)
        val (h, t, o) = rk(op.a(), 3)
        assertTrue(o in 0..8)
        assertTrue(op.b() in 1..99)
        dt.gor(t, op.b() / 10, digerKurallarOnlar.getValue(t))
        dob.gor(o, op.b() % 10, cikarmaSecenek.getValue(o))
        return h
    }

    @Test
    fun extractionGenerateMathOperationTenWithOtherRules() {
        val dt = Destek("otherRules onlar")
        val dob = Destek("otherRules birler")
        val yuzler = mutableSetOf<Int>()
        var sifirOnlar = 0
        var ayni = 0
        tekrar(40000) {
            val op = M.extractionGenerateMathOperationTenWithOtherRules()
            yuzler.add(digerKurallarKontrol(op, dt, dob))
            val t = op.a() / 10 % 10
            if (t in 1..4) {
                sifirOnlar++
                if (op.b() / 10 == t) ayni++
            }
        }
        assertEquals(ONLAR_5_HARIC.toSet(), yuzler)
        dt.tamMi(digerKurallarOnlar)
        dob.tamMi(cikarmaSecenek)
        // `(0..100).random() < 80` → 80/101 ≈ %79,2 ihtimalle aynı rakam (Android'deki gibi 101 değerli aralık)
        val oran = ayni.toDouble() / sifirOnlar
        assertTrue(oran > 0.77 && oran < 0.815, "aynı rakam oranı $oran")
    }

    @Test
    fun extractionGenerateMathOperationTenWithOtherRulesExtreme() {
        val dt = Destek("extreme onlar")
        val dob = Destek("extreme birler")
        val yuzler = mutableListOf<Int>()
        tekrar(40000) {
            yuzler.add(digerKurallarKontrol(M.extractionGenerateMathOperationTenWithOtherRulesExtreme(), dt, dob))
        }
        assertEquals((1..9).toSet(), yuzler.toSet())
        dt.tamMi(digerKurallarOnlar)
        dob.tamMi(cikarmaSecenek)
        // Yüzler basamağı %50 ihtimalle 5
        val oran = yuzler.count { it == 5 }.toDouble() / yuzler.size
        assertTrue(oran > 0.48 && oran < 0.52, "yüzler=5 oranı $oran")
    }

    @Test
    fun extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton() {
        val onlarTablo = mapOf(
            0 to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), 1 to listOf(1, 2, 3, 4, 5, 7, 8, 9), 2 to listOf(2, 3, 4, 5, 8, 9),
            3 to listOf(3, 4, 5, 9), 4 to listOf(4, 5), 5 to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
            6 to listOf(2, 3, 4, 6, 7, 8, 9), 7 to listOf(3, 4, 7, 8, 9), 8 to listOf(4, 8, 9), 9 to listOf(4, 9),
        )
        val dt = Destek("maraton onlar")
        val dob = Destek("maraton birler")
        val yuzler = mutableSetOf<Int>()
        tekrar(20000) {
            val op = M.extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton()
            assertEquals("-", op.operator)
            val (h, t, o) = rk(op.a(), 3)
            yuzler.add(h)
            assertTrue(o in 0..8)
            dt.gor(t, op.b() / 10, onlarTablo.getValue(t))
            dob.gor(o, op.b() % 10, cikarmaSecenek.getValue(o))
            assertTrue(op.a() - op.b() > 0)
        }
        assertEquals((1..9).toSet(), yuzler)
        dt.tamMi(onlarTablo)
        dob.tamMi(cikarmaSecenek)
    }

    /** Boncuk kuralı (çıkarma): rakam 1..4 → çıkan 6..(5 + rakam). */
    private val boncukCikarma = (1..4).associateWith { (6..(5 + it)).toList() }

    @Test
    fun extractionBeadRulesVeListesi() {
        val d = Destek("çıkarma boncuk")
        val onlar = mutableSetOf<Int>()
        tekrar {
            val op = M.extractionBeadRules()
            assertEquals("-", op.operator)
            onlar.add(op.a() / 10)
            d.gor(op.a() % 10, op.b(), boncukCikarma.getValue(op.a() % 10))
        }
        assertEquals(ONLAR_5_HARIC.toSet(), onlar)
        d.tamMi(boncukCikarma)

        val tersTablo = mapOf(6 to listOf(1, 2, 3, 4), 7 to listOf(2, 3, 4), 8 to listOf(3, 4), 9 to listOf(4))
        val dl = Destek("çıkarma boncuk liste")
        tohumla {
            assertEquals(emptyList(), M.extractionBeadRulesList(0))
            repeat(800) {
                for (count in listOf(1, 4, 6, 10)) {
                    val liste = M.extractionBeadRulesList(count)
                    assertEquals(count, liste.size)
                    havuzKontrol(liste.map { it.b() }, (6..9).toList())
                    liste.forEach {
                        assertEquals("-", it.operator)
                        assertTrue(it.a() / 10 in ONLAR_5_HARIC)
                        dl.gor(it.b(), it.a() % 10, tersTablo.getValue(it.b()))
                    }
                }
            }
        }
        dl.tamMi(tersTablo)
    }

    @Test
    fun extractionBeadRulesThreeTwoVeFourThree() {
        val d = Destek("boncuk 3-2")
        val yuzler = mutableSetOf<Int>()
        tekrar(8000) {
            val op = M.extractionBeadRulesThreeTwo()
            assertEquals("-", op.operator)
            val (h, t, o) = rk(op.a(), 3)
            yuzler.add(h)
            val (st, so) = rk(op.b(), 2)
            d.gor(t, st, boncukCikarma.getValue(t))
            d.gor(o, so, boncukCikarma.getValue(o))
        }
        assertEquals((1..9).toSet(), yuzler)
        d.tamMi(boncukCikarma)

        val d4 = Destek("boncuk 4-3")
        val binler = mutableSetOf<Int>()
        tekrar(8000) {
            val op = M.extractionBeadRulesFourThree()
            assertEquals("-", op.operator)
            val a = rk(op.a(), 4)
            binler.add(a[0])
            val b = rk(op.b(), 3)
            assertEquals(3, b.size)
            for (i in 0..2) d4.gor(a[i + 1], b[i], boncukCikarma.getValue(a[i + 1]))
        }
        assertEquals((1..9).toSet(), binler)
        d4.tamMi(boncukCikarma)
    }

    // ------------------------------------------------------------------ Çarpma

    private fun carpmaKontrol(
        uret: () -> MathOperation,
        birinciRakamlar: List<IntRange>,
        ikinciRakamlar: List<IntRange>,
    ) {
        val g1 = List(birinciRakamlar.size) { mutableSetOf<Int>() }
        val g2 = List(ikinciRakamlar.size) { mutableSetOf<Int>() }
        tekrar(5000) {
            val op = uret()
            assertEquals("x", op.operator)
            val a = rk(op.a())
            val b = rk(op.b())
            assertEquals(birinciRakamlar.size, a.size)
            assertEquals(ikinciRakamlar.size, b.size)
            a.forEachIndexed { i, r -> assertTrue(r in birinciRakamlar[i], "$op"); g1[i].add(r) }
            b.forEachIndexed { i, r -> assertTrue(r in ikinciRakamlar[i], "$op"); g2[i].add(r) }
        }
        birinciRakamlar.forEachIndexed { i, r -> assertEquals(r.toSet(), g1[i]) }
        ikinciRakamlar.forEachIndexed { i, r -> assertEquals(r.toSet(), g2[i]) }
    }

    @Test
    fun carpmaModlari() {
        carpmaKontrol(M::multiplicationLessFive, listOf(1..5, 0..5), listOf(2..5))
        carpmaKontrol(M::multiplicationLessFiveFull, listOf(5..9, 5..9), listOf(5..9))
        carpmaKontrol(M::multiplicationFull, listOf(1..9, 0..9), listOf(2..9))
        carpmaKontrol(M::multiplicationTwo, listOf(1..4, 0..4), listOf(2..4, 0..4))
        carpmaKontrol(M::multiplicationTwoFull, listOf(1..9, 0..9), listOf(1..9, 0..9))
        carpmaKontrol(M::multiplicationThreeFull, listOf(1..9, 0..9, 0..9), listOf(2..9))
        carpmaKontrol(M::multiplicationThreeTwoFive, listOf(1..5, 0..5, 0..5), listOf(1..5, 0..5))
        carpmaKontrol(M::multiplicationThreeTwoFull, listOf(1..9, 0..9, 0..9), listOf(1..9, 0..9))
    }

    // ------------------------------------------------------------------ Diziler

    /**
     * Dizi kuralı: i. sayı, önceki sayıların toplamının rakamlarına göre seçilir.
     * [tablolar]: basamak konumuna göre (0 = birler) "toplam rakamı → izinli rakamlar"; tabloda
     * olmayan rakam için Android'deki `else` dalı [varsayilan] geçerlidir.
     * [isaret] = -1 ise sayılar eksi (çıkarma dizisi).
     */
    private fun diziKontrol(
        ad: String,
        dizi: List<Int>,
        isaret: Int,
        tablolar: List<Map<Int, List<Int>>>,
        varsayilan: List<Int> = B19,
        destekler: List<Destek>? = null,
    ) {
        var us = 1
        repeat(tablolar.size) { us *= 10 }
        for (i in 1 until dizi.size) {
            val toplam = dizi.take(i).sum()
            assertTrue(toplam >= 0, "$ad: ara toplam negatif olmamalı: $dizi")
            val ts = toplam.toString()
            val v = isaret * dizi[i]
            assertTrue(v in 0 until us, "$ad: $i. sayı aralık dışı: $dizi")
            var kalan = v
            for (p in tablolar.indices) {
                val toplamRakam = if (ts.length > p) ts[ts.length - 1 - p] - '0' else 0
                val yeniRakam = kalan % 10
                kalan /= 10
                val izinli = tablolar[p][toplamRakam] ?: varsayilan
                if (destekler != null) {
                    destekler[p].gor(toplamRakam, yeniRakam, izinli)
                } else {
                    assertTrue(yeniRakam in izinli, "$ad: konum=$p toplam=$toplam yeni=${dizi[i]} izinli=$izinli dizi=$dizi")
                }
            }
        }
    }

    /** count ≤ 0 olsa bile ilk sayı eklenir (Android'deki davranış): boyut = max(count, 1). */
    private fun boyutKontrol(uret: (Int) -> List<Int>, uzun: Int = 7) = tohumla {
        assertEquals(1, uret(0).size)
        assertEquals(1, uret(-3).size)
        assertEquals(1, uret(1).size)
        assertEquals(uzun, uret(uzun).size)
    }

    private val seq1Tablo = mapOf(
        0 to listOf(1, 2, 5), 1 to listOf(1, 2, 5), 2 to listOf(1, 5), 3 to listOf(1, 5), 4 to listOf(5),
        5 to listOf(1, 2), 6 to listOf(1, 2), 7 to listOf(1), 8 to listOf(1), 9 to listOf(0),
    )

    @Test
    fun generateSequence1() {
        boyutKontrol(M::generateSequence1)
        val d = listOf(Destek("seq1 birler"), Destek("seq1 onlar"))
        tekrar(4000) {
            val dizi = M.generateSequence1(8)
            assertTrue(dizi[0] in listOf(11, 12, 21, 22))
            diziKontrol("seq1", dizi, 1, listOf(seq1Tablo, seq1Tablo), destekler = d)
        }
        // Rakamlar 9'da doyar (elde oluşmaz) → 0 hiç oluşmaz; 1-2 ile başlayıp 1→{2,3,6}, 2→{3,7}, 3→{4,8}, 4→{9} gidildiğinden 5 de oluşmaz.
        d[0].tamMi(seq1Tablo, setOf(0, 5))
        d[1].tamMi(seq1Tablo, setOf(0, 5))
    }

    @Test
    fun generateSequence1Digits() = tohumla {
        assertEquals(emptyList(), M.generateSequence1Digits(0, 2))
        assertEquals(emptyList(), M.generateSequence1Digits(-1, 2))
        for ((count, digits) in listOf(1 to 1, 6 to 1, 6 to 2, 10 to 3, 8 to 4, 5 to 0, 5 to -1)) {
            val dc = if (digits < 1) 1 else digits
            repeat(1500) {
                val dizi = M.generateSequence1Digits(count, digits)
                assertEquals(count, dizi.size)
                val ilk = rk(dizi[0])
                assertEquals(dc, ilk.size)
                assertTrue(ilk[0] in 1..2)
                assertTrue(ilk.all { it in 0..2 })
                diziKontrol("seq1Digits($count,$digits)", dizi, 1, List(dc) { seq1Tablo })
            }
        }
    }

    private val bes5Tablo = mapOf(
        0 to listOf(1, 2, 3), 1 to listOf(1, 2, 3, 4), 2 to listOf(1, 2, 3, 4), 3 to listOf(1, 2, 3, 4),
        4 to listOf(1, 2, 3, 4), 5 to listOf(1, 2), 6 to listOf(1, 2), 7 to listOf(1), 8 to listOf(1), 9 to listOf(0),
    )

    @Test
    fun generateSequence5RulesNew() {
        boyutKontrol(M::generateSequence5RulesNew)
        val d = listOf(Destek("5kural birler"), Destek("5kural onlar"))
        tekrar(4000) {
            val dizi = M.generateSequence5RulesNew(8)
            assertTrue(dizi[0] in listOf(11, 12, 21, 22))
            diziKontrol("5kural", dizi, 1, listOf(bes5Tablo, bes5Tablo), destekler = d)
        }
        d[0].tamMi(bes5Tablo, setOf(0))
        d[1].tamMi(bes5Tablo, setOf(0))
    }

    private val kolayOnlar = mapOf(
        0 to listOf(1, 2, 5), 1 to listOf(1, 2, 3, 4), 2 to listOf(1, 2, 3, 4), 3 to listOf(1, 2, 3, 4),
        4 to listOf(1, 2, 3, 4), 5 to listOf(1, 2), 6 to listOf(1, 2), 7 to listOf(1), 8 to listOf(1), 9 to listOf(0),
    )
    private val kolayBirler = mapOf(
        0 to B19, 1 to listOf(4, 5, 6, 7, 8), 2 to listOf(3, 4, 5, 6, 7), 3 to listOf(2, 3, 4, 5, 6),
        4 to listOf(1, 2, 3, 4, 5), 5 to listOf(5), 6 to listOf(4, 5), 7 to listOf(3, 4, 5), 8 to listOf(2, 3, 4, 5),
        9 to listOf(1, 2, 3, 4, 5),
    )
    private val hepSifir = (0..9).associateWith { listOf(0) }

    @Test
    fun generateSequence10RulesEasyNew() {
        boyutKontrol(M::generateSequence10RulesEasyNew)
        val d = listOf(Destek("10kolay birler"), Destek("10kolay onlar"))
        tekrar(5000) {
            val dizi = M.generateSequence10RulesEasyNew(8)
            assertTrue(dizi[0] / 10 in 1..4 && dizi[0] % 10 in 1..4)
            diziKontrol("10kolay", dizi, 1, listOf(kolayBirler, kolayOnlar), destekler = d)
        }
        d[0].tamMi(kolayBirler)
        d[1].tamMi(kolayOnlar)
    }

    @Test
    fun generateSequence10RulesEasyOneCount() {
        boyutKontrol(M::generateSequence10RulesEasyOneCount)
        val d = listOf(Destek("10kolay tek birler"), Destek("10kolay tek onlar"))
        tekrar(5000) {
            val dizi = M.generateSequence10RulesEasyOneCount(8)
            assertTrue(dizi[0] / 10 in 1..9 && dizi[0] % 10 in 1..9)
            // Sonraki sayılar tek basamaklı (onlar her zaman 0)
            assertTrue(dizi.drop(1).all { it in 1..9 })
            diziKontrol("10kolay tek", dizi, 1, listOf(kolayBirler, hepSifir), destekler = d)
        }
        d[0].tamMi(kolayBirler)
    }

    @Test
    fun generateSequence10RulesEasyOldOneCount() {
        boyutKontrol(M::generateSequence10RulesEasyOldOneCount)
        val birler = mapOf(
            0 to B19, 1 to listOf(4, 9), 2 to listOf(3, 4, 8, 9), 3 to listOf(2, 3, 4, 7, 8, 9), 4 to listOf(6, 7, 8),
            5 to listOf(5), 6 to listOf(4, 5, 9), 7 to listOf(3, 4, 5, 8, 9), 8 to listOf(2, 3, 4, 5, 7, 8, 9),
            9 to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
        )
        val d = listOf(Destek("10eski tek birler"), Destek("10eski tek onlar"))
        var dort = 0
        var dortAlti = 0
        tekrar(8000) {
            val dizi = M.generateSequence10RulesEasyOldOneCount(8)
            assertTrue(dizi[0] / 10 in 1..9 && dizi[0] % 10 in 1..9)
            assertTrue(dizi.drop(1).all { it in 1..9 })
            diziKontrol("10eski tek", dizi, 1, listOf(birler, hepSifir), destekler = d)
            for (i in 1 until dizi.size) if (dizi.take(i).sum() % 10 == 4) {
                dort++
                if (dizi[i] == 6) dortAlti++
            }
        }
        d[0].tamMi(birler)
        // Birler 4 iken ağırlıklı liste: 10 seçenekten 8'i 6
        val oran = dortAlti.toDouble() / dort
        assertTrue(oran > 0.77 && oran < 0.83, "4→6 oranı $oran")
    }

    @Test
    fun generateSequence10RulesEasyOld() {
        boyutKontrol(M::generateSequence10RulesEasyOld)
        val birler = mapOf(
            1 to listOf(9), 2 to listOf(9, 8), 3 to listOf(9, 8, 7), 4 to listOf(9, 8, 7, 6),
            5 to listOf(1, 2, 3, 4, 5), 6 to listOf(9), 7 to listOf(9, 8), 8 to listOf(9, 8, 7), 9 to listOf(9, 8, 7, 6),
            0 to B19,
        )
        val d = listOf(Destek("10eski birler"), Destek("10eski onlar"))
        tekrar(6000) {
            val dizi = M.generateSequence10RulesEasyOld(8)
            assertTrue(dizi[0] / 10 in 1..3)
            assertTrue(dizi[0] % 10 in listOf(1, 2, 3, 4, 6, 7, 8, 9))
            diziKontrol("10eski", dizi, 1, listOf(birler, kolayOnlar), destekler = d)
        }
        d[0].tamMi(birler)
        d[1].tamMi(kolayOnlar)
    }

    @Test
    fun generateSequenceBeadRules() {
        boyutKontrol(M::generateSequenceBeadRules)
        val birler = mapOf(
            1 to listOf(4, 5, 6, 7), 2 to listOf(3, 4, 5, 6), 3 to listOf(2, 3, 4, 5), 4 to listOf(1, 2, 3, 4),
            5 to listOf(6, 7, 8, 9), 6 to listOf(6, 7, 8), 7 to listOf(6, 7), 8 to listOf(6), 0 to B19, 9 to B19,
        )
        val d = listOf(Destek("boncuk dizi birler"), Destek("boncuk dizi onlar"))
        tekrar(6000) {
            val dizi = M.generateSequenceBeadRules(8)
            assertTrue(dizi[0] / 10 in 1..2 && dizi[0] % 10 in 1..4)
            diziKontrol("boncuk dizi", dizi, 1, listOf(birler, seq1Tablo), destekler = d)
        }
        // Birlerde toplam rakamı 0 ve 9 hiç oluşmaz (izinli rakamlar bu değerlere götürmez).
        d[0].tamMi(birler, setOf(0, 9))
        d[1].tamMi(seq1Tablo)
    }

    private val cikarmaOnlar = mapOf(
        0 to listOf(0), 1 to listOf(1), 2 to listOf(1), 3 to listOf(1, 2), 4 to listOf(1, 2, 3), 5 to listOf(5),
        6 to listOf(1, 5), 7 to listOf(1, 5), 8 to listOf(1, 2, 5), 9 to listOf(1, 2, 3, 5),
    )

    // Birler tablosu onlar tablosundan 7 ve 8'de farklıdır (Android'de böyle).
    private val cikarmaBirler = cikarmaOnlar + mapOf(7 to listOf(1, 2, 5), 8 to listOf(1, 2, 3, 5))

    @Test
    fun generateSequenceExtraction() {
        boyutKontrol(M::generateSequenceExtraction)
        val d = listOf(Destek("çıkarma dizi birler"), Destek("çıkarma dizi onlar"))
        tekrar(6000) {
            val dizi = M.generateSequenceExtraction(10)
            assertTrue(dizi[0] in listOf(88, 89, 98, 99))
            assertTrue(dizi.drop(1).all { it <= 0 })
            diziKontrol("çıkarma dizi", dizi, -1, listOf(cikarmaBirler, cikarmaOnlar), destekler = d)
            assertTrue(dizi.sum() >= 0)
        }
        d[0].tamMi(cikarmaBirler)
        d[1].tamMi(cikarmaOnlar)
    }

    @Test
    fun generateSequenceExtractionThreeDigits() {
        boyutKontrol(M::generateSequenceExtractionThreeDigits)
        val d = listOf(Destek("çıkarma3 birler"), Destek("çıkarma3 onlar"), Destek("çıkarma3 yüzler"))
        tekrar(6000) {
            val dizi = M.generateSequenceExtractionThreeDigits(10)
            assertTrue(rk(dizi[0]).let { it.size == 3 && it.all { r -> r in 8..9 } })
            diziKontrol("çıkarma3", dizi, -1, listOf(cikarmaBirler, cikarmaOnlar, cikarmaOnlar), destekler = d)
            assertTrue(dizi.sum() >= 0)
        }
        d[0].tamMi(cikarmaBirler)
        d[1].tamMi(cikarmaOnlar)
        d[2].tamMi(cikarmaOnlar)
    }

    private val cikarma5 = mapOf(
        0 to listOf(0), 1 to listOf(1), 2 to listOf(1), 3 to listOf(1, 2), 4 to listOf(1, 2, 3),
        5 to listOf(4, 3, 2, 1), 6 to listOf(4, 3, 2), 7 to listOf(4, 3), 8 to listOf(4), 9 to listOf(1, 2),
    )

    @Test
    fun generateSequenceExtractionFiveRules() {
        boyutKontrol(M::generateSequenceExtractionFiveRules)
        val d = listOf(Destek("çıkarma5 birler"), Destek("çıkarma5 onlar"))
        tekrar(6000) {
            val dizi = M.generateSequenceExtractionFiveRules(10)
            assertTrue(dizi[0] in listOf(88, 89, 98, 99))
            diziKontrol("çıkarma5", dizi, -1, listOf(cikarma5, cikarma5), destekler = d)
            assertTrue(dizi.sum() >= 0)
        }
        // 8-9 ile başlayıp 9→{8,7}, 8→{4}, 7→{4,3} ... gidildiği için 5 ve 6 rakamı hiç oluşmaz.
        d[0].tamMi(cikarma5, setOf(5, 6))
        d[1].tamMi(cikarma5, setOf(5, 6))

        boyutKontrol(M::generateSequenceExtractionFiveRulesThreeDigits)
        val d3 = listOf(Destek("çıkarma5-3 birler"), Destek("çıkarma5-3 onlar"), Destek("çıkarma5-3 yüzler"))
        tekrar(6000) {
            val dizi = M.generateSequenceExtractionFiveRulesThreeDigits(10)
            assertTrue(rk(dizi[0]).let { it.size == 3 && it.all { r -> r in 8..9 } })
            diziKontrol("çıkarma5-3", dizi, -1, listOf(cikarma5, cikarma5, cikarma5), destekler = d3)
            assertTrue(dizi.sum() >= 0)
        }
        d3.forEach { it.tamMi(cikarma5, setOf(5, 6)) }
    }

    private val cikarma10Onlar = mapOf(
        1 to listOf(1), 2 to listOf(1), 3 to listOf(1, 2), 4 to listOf(1, 2, 3), 5 to listOf(4, 3, 2, 1),
        6 to listOf(4, 3, 2, 1, 5), 7 to listOf(4, 3, 5, 2, 1), 8 to listOf(4, 3, 5, 2, 1), 9 to listOf(1, 2, 5, 6),
    )
    private val cikarma10Birler = mapOf(
        1 to listOf(2, 3, 4, 5, 7, 8, 9), 2 to listOf(3, 4, 5, 8, 9), 3 to listOf(4, 5, 9), 4 to listOf(5),
        5 to listOf(9, 8, 7, 6), 6 to listOf(9, 8, 7), 7 to listOf(9, 8), 8 to listOf(9),
    )

    @Test
    fun generateSequenceExtractionTenRules() {
        boyutKontrol(M::generateSequenceExtractionTenRules, 3)
        // Uygulama bu modu count = 3 ile çağırır; bu uzunlukta ara toplam hiç eksiye düşmez.
        tekrar(8000) {
            val dizi = M.generateSequenceExtractionTenRules(3)
            assertEquals(3, dizi.size)
            assertTrue(dizi[0] / 10 in 8..9 && dizi[0] % 10 in 5..8)
            diziKontrol("çıkarma10", dizi, -1, listOf(cikarma10Birler, cikarma10Onlar))
            // İlk adımda birler her zaman ödünç gerektirir (ilk sayının birleri 5..8)
            assertTrue(-dizi[1] % 10 > dizi[0] % 10)
        }
        boyutKontrol(M::generateSequenceExtractionTenRulesThreeDigits, 3)
        // Uygulama: count = 3 ve 4.
        for (count in listOf(3, 4)) tekrar(6000) {
            val dizi = M.generateSequenceExtractionTenRulesThreeDigits(count)
            assertEquals(count, dizi.size)
            val ilk = rk(dizi[0])
            assertEquals(4, ilk.size)
            assertTrue(ilk[0] in 1..4 && ilk[1] in 8..9 && ilk[2] in 8..9 && ilk[3] in 5..8)
            diziKontrol("çıkarma10-3", dizi, -1, listOf(cikarma10Birler, cikarma10Onlar, cikarma10Onlar))
            assertTrue(dizi.sum() > 0)
        }
    }

    @Test
    fun generateSequenceExtractionTenRulesUzunDizideAndroidGibiCoker() = tohumla(1) {
        // Android'deki hata aynen korunur: ara toplam -9..-1 aralığına düşerse "-5" metninin
        // onlar "rakamı" '-' olur ve toInt() NumberFormatException atar (uygulama yalnızca 3-4 kullanır).
        assertFailsWith<NumberFormatException> { M.generateSequenceExtractionTenRules(40) }
    }

    private val boncukDiziOnlar = mapOf(
        1 to listOf(1), 2 to listOf(1), 3 to listOf(1, 2), 4 to listOf(1, 2), 5 to listOf(1, 2, 3, 4),
        6 to listOf(1, 5), 7 to listOf(1), 8 to listOf(1, 2), 9 to listOf(1, 2),
    )
    private val boncukDiziBirler = mapOf(
        1 to listOf(6), 2 to listOf(6, 7), 3 to listOf(6, 7, 8), 4 to listOf(6, 7, 8, 9), 5 to listOf(1, 2, 3, 4),
        6 to listOf(2, 3, 4), 7 to listOf(3, 4), 8 to listOf(4), 9 to listOf(5),
    )

    @Test
    fun generateSequenceExtractionBeadRules() {
        boyutKontrol(M::generateSequenceExtractionBeadRules, 3)
        // Uygulama: count = 3. `control` bayrağı Android'de hiç false olmaz → onlar her adımda tablodan seçilir.
        tekrar(8000) {
            val dizi = M.generateSequenceExtractionBeadRules(3)
            assertEquals(3, dizi.size)
            assertTrue(dizi[0] / 10 in 8..9 && dizi[0] % 10 in 1..4)
            diziKontrol("boncuk çıkarma dizi", dizi, -1, listOf(boncukDiziBirler, boncukDiziOnlar))
            // İlk adım: birler 1..4 → çıkan 6..(5+birler) (boncuk kuralı, ödünçlü)
            assertTrue(-dizi[1] % 10 in 6..(5 + dizi[0] % 10))
            assertTrue(-dizi[1] / 10 in 1..2)
        }
        boyutKontrol(M::generateSequenceExtractionBeadRulesThreeDigits, 3)
        for (count in listOf(3, 4)) tekrar(6000) {
            val dizi = M.generateSequenceExtractionBeadRulesThreeDigits(count)
            assertEquals(count, dizi.size)
            val ilk = rk(dizi[0])
            assertEquals(4, ilk.size)
            assertTrue(ilk[0] in 1..4 && ilk[1] in 8..9 && ilk[2] in 8..9 && ilk[3] in 1..4)
            diziKontrol("boncuk çıkarma dizi-3", dizi, -1, listOf(boncukDiziBirler, boncukDiziOnlar, boncukDiziOnlar))
            assertTrue(dizi.sum() > 0)
        }
    }

    // ------------------------------------------------------------------ Genel toplama ve yarış dizileri

    private fun rakamAraliklari(n: Int, araliklar: List<IntRange>, ad: String) {
        val r = rk(n)
        assertEquals(araliklar.size, r.size, "$ad: $n")
        r.forEachIndexed { i, x -> assertTrue(x in araliklar[i], "$ad: $n") }
    }

    private fun genelKontrol(
        ad: String,
        uret: (Int) -> List<Int>,
        ilk: List<IntRange>,
        sonraki: List<IntRange>,
        isaret: Int,
    ) {
        boyutKontrol(uret)
        val gorulenIlk = mutableSetOf<Int>()
        val gorulenSon = mutableSetOf<Int>()
        tekrar(3000) {
            val dizi = uret(6)
            assertEquals(6, dizi.size)
            rakamAraliklari(dizi[0], ilk, ad)
            gorulenIlk.add(rk(dizi[0])[0])
            dizi.drop(1).forEach {
                rakamAraliklari(isaret * it, sonraki, ad)
                gorulenSon.add(rk(isaret * it).last())
            }
        }
        assertEquals(ilk[0].toSet(), gorulenIlk)
        assertEquals(sonraki.last().toSet(), gorulenSon)
    }

    @Test
    fun generalCollection() {
        genelKontrol("genel1", M::generalCollectionOneDigits, listOf(1..9), listOf(1..9), 1)
        // İki basamakta ilk sayının birleri 0..9, sonrakilerin 1..9 (Android'de böyle)
        genelKontrol("genel2", M::generalCollectionTwoDigits, listOf(1..9, 0..9), listOf(1..9, 1..9), 1)
        genelKontrol("genel3", M::generalCollectionThreeDigits, listOf(1..9, 0..9, 0..9), listOf(1..9, 0..9, 0..9), 1)
        genelKontrol(
            "genel4", M::generalCollectionFourDigits,
            listOf(1..9, 0..9, 0..9, 0..9), listOf(1..9, 0..9, 0..9, 0..9), 1,
        )
    }

    @Test
    fun generateSequenceExtractionRace() {
        val r19 = 1..9
        val r09 = 0..9
        genelKontrol("yarış", M::generateSequenceExtractionRace, listOf(5..9, r09), listOf(r19), -1)
        genelKontrol("yarış zor", M::generateSequenceExtractionRaceHard, listOf(5..9, r09, r09), listOf(r19), -1)
        genelKontrol("yarış 2", M::generateSequenceExtractionRaceTwoDigits, listOf(5..9, r09, r09), listOf(r19, r19), -1)
        genelKontrol(
            "yarış 2 zor", M::generateSequenceExtractionRaceTwoDigitsHard,
            listOf(5..9, r09, r09, r09), listOf(r19, r19), -1,
        )
        genelKontrol(
            "yarış 3", M::generateSequenceExtractionRaceThreeDigits,
            listOf(5..9, r09, r09, r09), listOf(r19, r19, r19), -1,
        )
        genelKontrol(
            "yarış 3 zor", M::generateSequenceExtractionRaceThreeDigitsHard,
            listOf(5..9, r09, r09, r09, r09), listOf(r19, r19, r19), -1,
        )
        genelKontrol(
            "yarış 4", M::generateSequenceExtractionRaceFourDigits,
            listOf(5..9, r09, r09, r09, r09), listOf(r19, r19, r19, r19), -1,
        )
    }

    // ------------------------------------------------------------------ Kaynak enjeksiyonu

    @Test
    fun varsayilanKaynakTohumsuzdur() {
        assertEquals(Random.Default, RastgeleKaynagi.rastgele)
        // Tohumsuz çalışmada da kurallar geçerli ve çıktı çeşitli
        val sonuclar = List(200) { M.multiplicationTwoFull() }
        assertTrue(sonuclar.toSet().size > 100)
    }

    @Test
    fun ayniTohumAyniIslemler() {
        fun uret(): List<Any> {
            RastgeleKaynagi.rastgele = Random(555)
            return try {
                listOf(
                    M.generateRelatedNumbers(3, 3), M.generateMathOperationBeadRule(), M.generateRelatedNumbers0(1, 1),
                    M.generateRelatedNumbers2(2, 2), M.generateMathOperationList(7), M.generateSequence1(6),
                )
            } finally {
                RastgeleKaynagi.varsayilanaDon()
            }
        }
        assertEquals(uret(), uret())
    }
}

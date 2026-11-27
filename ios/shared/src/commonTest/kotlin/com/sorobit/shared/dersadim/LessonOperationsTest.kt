// MapFragment.getLessonOperations / getLessonOperationsBlinding tablolarının Android'le aynı olduğunu doğrular.
package com.sorobit.shared.dersadim

import com.sorobit.shared.matematik.MathOperation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LessonOperationsTest {
    private val tumAralik = -20..3000

    @Test
    fun dalSayilariAndroidDosyasiylaAyni() {
        // Android dosyasından bağımsız betikle sayılan değerler.
        assertEquals(69, ANDROID_DALLAR_OPERATIONS.size)
        assertEquals(55, ANDROID_DALLAR_BLINDING.size)
        assertEquals(69, ANDROID_DALLAR_OPERATIONS.toSet().size, "tekrarlı dal olmamalı")
        assertEquals(55, ANDROID_DALLAR_BLINDING.toSet().size, "tekrarlı dal olmamalı")
    }

    @Test
    fun abakusTablosuTamOlarakAndroidDallariniKapsar() {
        val dolu = tumAralik.filter { id ->
            DersAdimTestYardimci.tohumla(7) { LessonOperations.getLessonOperations(id) }.isNotEmpty()
        }
        assertEquals(ANDROID_DALLAR_OPERATIONS.sorted(), dolu)
    }

    @Test
    fun korTablosuTamOlarakAndroidDallariniKapsar() {
        val dolu = tumAralik.filter { id ->
            DersAdimTestYardimci.tohumla(7) { LessonOperations.getLessonOperationsBlinding(id) }.isNotEmpty()
        }
        // Android'de kör tablonun iki dalı BOŞ liste döndürür (aynen korunur): 83'ün bütün satırları yorum
        // satırına alınmış, 100'ün listesi boş. Yani bu kimlikler "dal var ama soru yok" durumundadır.
        assertEquals(listOf(83, 100), ANDROID_DALLAR_BLINDING.sorted() - dolu.toSet())
        assertEquals((ANDROID_DALLAR_BLINDING - setOf(83, 100)).sorted(), dolu)
        // Çözümleme bu yüzden 83'ten 82'ye iner.
        assertEquals(82, LessonOperations.resolveLessonOperationsBlindingId(83, 1))
    }

    @Test
    fun bilinmeyenKimlikBosListe() {
        // 18: güncel Android'de dal KALDIRILDI (eskiden 7 soruluk tabloydu)
        for (id in listOf(Int.MIN_VALUE, -1, 0, 3, 6, 10, 11, 18, 154, 999, 1001, 1014, 5000, Int.MAX_VALUE)) {
            assertEquals(emptyList(), LessonOperations.getLessonOperations(id), "id=$id")
        }
        for (id in listOf(Int.MIN_VALUE, -1, 0, 6, 10, 92, 99, 101, 999, 1012, 5000, Int.MAX_VALUE)) {
            assertEquals(emptyList(), LessonOperations.getLessonOperationsBlinding(id), "id=$id")
        }
    }

    @Test
    fun altinVeriTumDallariVeTohumlariKapsar() {
        // 17 ve 1013'ün altın satırı yok (Android'de değişti; JVM referansı çalıştırılamadı) — yapısal testle sınanır.
        val beklenenAnahtarlar = DersAdimTestYardimci.TOHUMLAR.flatMap { t ->
            ANDROID_DALLAR_OPERATIONS.filter { it !in ALTIN_ELLE_DALLAR }.map { "$t|O|$it" } +
                ANDROID_DALLAR_BLINDING.map { "$t|B|$it" }
        }
        assertEquals(beklenenAnahtarlar, ALTIN_DERS_ADIM.map { it.split("|").take(3).joinToString("|") })
        assertEquals(3 * (67 + 55), ALTIN_DERS_ADIM.size)
    }

    /** Altın satırı olmayan, Android'de değişmiş dallar. */
    private val ALTIN_ELLE_DALLAR = setOf(17, 1013)

    @Test
    fun guncelAndroidDegisiklikleri_1013_17_18() {
        for (tohum in 1..10) {
            // 1013: artık 6 soru, 4 ve 5 basamaklı ilişkili sayılar dönüşümlü (eskiden tek soru, 1 basamaklı)
            val s1013 = DersAdimTestYardimci.tohumla(tohum) { LessonOperations.getLessonOperations(1013) }
            assertEquals(6, s1013.size)
            assertTrue(s1013.all { it.operator != null })
            // 17: 6 soru (sondaki generateRelatedNumbers2(5, 4) kalktı; eskiden 7)
            val s17 = DersAdimTestYardimci.tohumla(tohum) { LessonOperations.getLessonOperations(17) }
            assertEquals(6, s17.size)
        }
        // 18: dal kalktı → boş
        assertEquals(emptyList(), LessonOperations.getLessonOperations(18))
        // Çözümleme: 18 istenirse 17'ye iner (alt sınır izin veriyorsa)
        assertEquals(17, LessonOperations.resolveLessonOperationsId(18, 16))
    }

    @Test
    fun tohumluSonuclarAndroidReferansiylaAyni() {
        for (satir in ALTIN_DERS_ADIM) {
            val p = satir.split("|", limit = 4)
            val tohum = p[0].toInt()
            val id = p[2].toInt()
            val gercek = when (p[1]) {
                "O" -> DersAdimTestYardimci.ozet(tohum) { LessonOperations.getLessonOperations(id) }
                "B" -> DersAdimTestYardimci.ozet(tohum) { LessonOperations.getLessonOperationsBlinding(id) }
                else -> error("bilinmeyen tablo: $satir")
            }
            assertEquals(p[3], gercek, "tohum=$tohum tablo=${p[1]} id=$id")
        }
    }

    @Test
    fun ayniTohumAyniSonucFarkliTohumFarkliSonuc() {
        val a = DersAdimTestYardimci.tohumla(11) { LessonOperations.getLessonOperations(7) }
        val b = DersAdimTestYardimci.tohumla(11) { LessonOperations.getLessonOperations(7) }
        val c = DersAdimTestYardimci.tohumla(12) { LessonOperations.getLessonOperations(7) }
        assertEquals(a, b)
        assertTrue(a != c)
    }

    @Test
    fun sabitDallarAndroiddekiDegerler() {
        // id 1: Android'de elle yazılmış sabit liste ("8" iki kez geçer — aynen korunur).
        assertEquals(
            listOf("2", "5", "3", "8", "4", "1", "9", "6", "7", "8"),
            LessonOperations.getLessonOperations(1).map { it.operator },
        )
        assertTrue(LessonOperations.getLessonOperations(1).all { it.firstNumber == null && it.secondNumber == null })
        // id 8: sabit 5'lik toplama soruları.
        assertEquals(
            listOf(
                MathOperation(3, "+", 4), MathOperation(2, "+", 3), MathOperation(4, "+", 1), MathOperation(3, "+", 2),
                MathOperation(1, "+", 4), MathOperation(4, "+", 2), MathOperation(2, "+", 3), MathOperation(3, "+", 3),
            ),
            LessonOperations.getLessonOperations(8),
        )
        // id 4: ilk soru sabit, ardından 6 üretilmiş soru.
        val dort = DersAdimTestYardimci.tohumla(5) { LessonOperations.getLessonOperations(4) }
        assertEquals(MathOperation(4, "+", 5), dort.first())
        assertEquals(7, dort.size)
        // Kör tablo id 1 ve 3 Android'de aynı sabit listedir.
        val bir = LessonOperations.getLessonOperationsBlinding(1)
        assertEquals(
            listOf(listOf(10, 3, 5, 10), listOf(7, 20, 10, 11), listOf(12, 10, 5, 50), listOf(2, 5, 2, 40), listOf(5, 3, 20, 10)),
            bir,
        )
        assertEquals(bir, LessonOperations.getLessonOperationsBlinding(3))
    }

    @Test
    fun sayiTanimaDallariBicimi() {
        // id 2 / 1000: tekrarsız 7 sayı (1 ve 2 basamaklı); id 1005: 2+2+3+2+3 = 12 sayı, basamak sırasıyla.
        for (tohum in 1..20) {
            val iki = DersAdimTestYardimci.tohumla(tohum) { LessonOperations.getLessonOperations(2) }.map { it.operator!! }
            assertEquals(7, iki.toSet().size)
            assertTrue(iki.all { it.length == 1 })
            val bin = DersAdimTestYardimci.tohumla(tohum) { LessonOperations.getLessonOperations(1000) }.map { it.operator!! }
            assertEquals(7, bin.toSet().size)
            assertTrue(bin.all { it.length == 2 })
            val karisik = DersAdimTestYardimci.tohumla(tohum) { LessonOperations.getLessonOperations(1005) }
                .map { it.operator!!.length }
            assertEquals(listOf(1, 1, 2, 2, 3, 3, 3, 4, 4, 5, 5, 5), karisik)
        }
    }

    @Test
    fun resolveAndroidReferansiylaAyni() {
        for (satir in ALTIN_RESOLVE) {
            val (istenen, min, o, b) = satir.split("|").map { it.toInt() }
            assertEquals(o, LessonOperations.resolveLessonOperationsId(istenen, min), satir)
            assertEquals(b, LessonOperations.resolveLessonOperationsBlindingId(istenen, min), satir)
        }
    }

    @Test
    fun resolveKurallari() {
        // Dolu kimlik aynen döner.
        assertEquals(9, LessonOperations.resolveLessonOperationsId(9, 8))
        // Boş kimlikten geriye doğru ilk dolu kimliğe iner (10 ve 11 boş, 9 dolu).
        assertEquals(9, LessonOperations.resolveLessonOperationsId(11, 8))
        // Alt sınırda durur; alt sınır boş olsa bile onu döner (29 abaküs tablosunda yok).
        assertEquals(29, LessonOperations.resolveLessonOperationsId(31, 29))
        assertTrue(LessonOperations.getLessonOperations(29).isEmpty())
        // İstenen kimlik alt sınırın altındaysa hiç değişmez.
        assertEquals(3, LessonOperations.resolveLessonOperationsId(3, 10))
        assertEquals(5, LessonOperations.resolveLessonOperationsBlindingId(8, 1))
        assertEquals(1024, LessonOperations.resolveLessonOperationsBlindingId(1027, 1024))
    }
}

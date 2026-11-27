// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 1
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps1 (satır 1893-2311).
// Elle düzenlenirse betik yeniden çalıştırılmadan önce değişiklik buraya not edilmeli.
package com.sorobit.shared.ogretici.icerik

import com.sorobit.shared.ogretici.OgreticiAdimi
import com.sorobit.shared.ogretici.OgreticiBoncukHareketi
import com.sorobit.shared.ogretici.OgreticiGorunurluk
import com.sorobit.shared.ogretici.OgreticiIcerigi
import com.sorobit.shared.ogretici.OgreticiOdakIslemi
import com.sorobit.shared.ogretici.OgreticiRenkliHarf
import com.sorobit.shared.ogretici.OgreticiSabit.CAPA_ALT_KILAVUZ
import com.sorobit.shared.ogretici.OgreticiSabit.CAPA_ORTA
import com.sorobit.shared.ogretici.OgreticiSabit.CAPA_UST_KILAVUZ
import com.sorobit.shared.ogretici.OgreticiSabit.GORUNMEZ
import com.sorobit.shared.ogretici.OgreticiSabit.GORUNUR
import com.sorobit.shared.ogretici.OgreticiSabit.ILK_ACILISTA_GORUNMEZ
import com.sorobit.shared.ogretici.OgreticiSabit.ONCEKI
import com.sorobit.shared.ogretici.OgreticiSabit.YOK

internal fun ogretici1(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 1,
    kuralTablosu = null,
    adimlar = listOf(
        // 1 · satır 1895
        OgreticiAdimi(
            metin = "tutorial1_1",
            ses = "tutorial1_1",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("abacusLinear", GORUNMEZ),
                OgreticiGorunurluk("abacusContainer", GORUNMEZ),
                OgreticiGorunurluk("backButton", GORUNMEZ),
                OgreticiGorunurluk("skipTutorialButton", ILK_ACILISTA_GORUNMEZ),
            ),
        ),
        // 2 · satır 1910
        OgreticiAdimi(
            metin = "tutorial1_2",
            ses = "tutorial1_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("backButton", GORUNUR),
            ),
        ),
        // 3 · satır 1919
        OgreticiAdimi(
            metin = "tutorial1_4",
            ses = "tutorial1_4",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("abacusLinear", GORUNUR),
                OgreticiGorunurluk("abacusContainer", GORUNUR),
            ),
        ),
        // 4 · satır 1929
        OgreticiAdimi(
            metin = "tutorial1_5",
            ses = "tutorial1_5",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 5 · satır 1935
        OgreticiAdimi(
            metin = "tutorial1_6",
            ses = "tutorial1_6",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 6 · satır 1941
        OgreticiAdimi(
            metin = "tutorial1_7",
            ses = "tutorial1_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 19f),
            ),
        ),
        // 7 · satır 1960
        OgreticiAdimi(
            metin = "tutorial1_8",
            ses = "tutorial1_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 64f),
            ),
        ),
        // 8 · satır 1978
        OgreticiAdimi(
            metin = "tutorial1_9",
            ses = "tutorial1_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 109f),
            ),
        ),
        // 9 · satır 1996
        OgreticiAdimi(
            metin = "tutorial1_10",
            ses = "tutorial1_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 154f),
            ),
        ),
        // 10 · satır 2014
        OgreticiAdimi(
            metin = "tutorial1_11",
            ses = "tutorial1_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 199f),
            ),
        ),
        // 11 · satır 2032
        OgreticiAdimi(
            metin = "tutorial1_12",
            ses = "tutorial1_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 0f),
                OgreticiOdakIslemi(genislikDp = 270f, yukseklikDp = 280f),
            ),
        ),
        // 12 · satır 2061
        OgreticiAdimi(
            metin = "tutorial1_13",
            ses = "tutorial1_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 0f),
                OgreticiOdakIslemi(capa = CAPA_UST_KILAVUZ),
                OgreticiOdakIslemi(genislikDp = 270f, yukseklikDp = 60f),
            ),
        ),
        // 13 · satır 2088
        OgreticiAdimi(
            metin = "tutorial1_14",
            ses = "tutorial1_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 0f),
                OgreticiOdakIslemi(capa = CAPA_ALT_KILAVUZ),
                OgreticiOdakIslemi(genislikDp = 270f, yukseklikDp = 180f),
            ),
        ),
        // 14 · satır 2118
        OgreticiAdimi(
            metin = "tutorial1_15",
            ses = "tutorial1_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
            ),
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", YOK),
            ),
        ),
        // 15 · satır 2131
        OgreticiAdimi(
            metin = "tutorial1_100",
            ses = "tutorial1_100",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
            ),
        ),
        // 16 · satır 2140
        OgreticiAdimi(
            metin = "tutorial1_101",
            ses = "tutorial1_101",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
            ),
        ),
        // 17 · satır 2149
        OgreticiAdimi(
            metin = "tutorial1_102",
            ses = "tutorial1_102",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
        ),
        // 18 · satır 2158
        OgreticiAdimi(
            metin = "tutorial1_103",
            ses = "tutorial1_103",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
        ),
        // 19 · satır 2170
        OgreticiAdimi(
            metin = "tutorial1_104",
            ses = "tutorial1_104",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
        ),
        // 20 · satır 2179
        OgreticiAdimi(
            metin = "tutorial1_17",
            ses = "tutorial1_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
            ),
        ),
        // 21 · satır 2188
        OgreticiAdimi(
            metin = "tutorial1_105",
            ses = "tutorial1_105",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
            ),
        ),
        // 22 · satır 2197
        OgreticiAdimi(
            metin = "tutorial1_106",
            ses = "tutorial1_106",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
            ),
        ),
        // 23 · satır 2206
        OgreticiAdimi(
            metin = "tutorial1_107",
            ses = "tutorial1_107",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
        ),
        // 24 · satır 2215
        OgreticiAdimi(
            metin = "tutorial1_108",
            ses = "tutorial1_108",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            abakusSifirla = true,
        ),
        // 25 · satır 2229
        OgreticiAdimi(
            metin = "tutorial1_109",
            ses = "tutorial1_109",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t1_1_asagidaki",
            soru = "1",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 1,
            abakusSifirla = true,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_001",
        ),
        // 26 · satır 2244
        OgreticiAdimi(
            metin = "tutorial1_110",
            ses = "tutorial1_110",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t1_2_yaz",
            soru = "2",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 2,
            abakusSifirla = true,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_002",
        ),
        // 27 · satır 2259
        OgreticiAdimi(
            metin = "tutorial1_111",
            ses = "tutorial1_111",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t1_5_yaz",
            soru = "5",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 5,
            abakusSifirla = true,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_003",
        ),
        // 28 · satır 2274
        OgreticiAdimi(
            metin = "tutorial1_111",
            ses = "tutorial1_111",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t1_6_yaz",
            soru = "6",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 6,
            abakusSifirla = true,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_004",
        ),
        // 29 · satır 2289
        OgreticiAdimi(
            metin = "tutorial1_113",
            ses = "tutorial1_113",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t1_8_yaz",
            soru = "8",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 8,
            abakusSifirla = true,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_005",
        ),
        // 30 · satır 2304
        OgreticiAdimi(
            metin = "tutorial1_21",
            ses = "tutorial1_21",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

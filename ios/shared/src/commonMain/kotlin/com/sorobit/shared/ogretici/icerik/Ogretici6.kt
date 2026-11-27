// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 6
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps6 (satır 5951-6776).
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

internal fun ogretici6(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 6,
    kuralTablosu = "tenRuleTable",
    adimlar = listOf(
        // 1 · satır 5953
        OgreticiAdimi(
            metin = "tutorial6_1",
            ses = "tutorial6_1",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 2 · satır 5958
        OgreticiAdimi(
            metin = "tutorial6_2",
            ses = "tutorial6_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 1),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 3 · satır 5977
        OgreticiAdimi(
            metin = "tutorial6000_0",
            ses = "tutorial6000_0",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 4 · satır 5984
        OgreticiAdimi(
            metin = "tutorial6000_1",
            ses = "tutorial6000_1",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 19f),
                OgreticiOdakIslemi(capa = CAPA_ALT_KILAVUZ),
                OgreticiOdakIslemi(sagBoslukDp = 19f, ustBoslukDp = -10f),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 200f),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 5 · satır 6026
        OgreticiAdimi(
            metin = "tutorial6000_2",
            ses = "tutorial6000_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 19f),
                OgreticiOdakIslemi(capa = CAPA_UST_KILAVUZ),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 100f),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 6 · satır 6050
        OgreticiAdimi(
            metin = "tutorial6000_3",
            ses = "tutorial6000_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", YOK),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 7 · satır 6058
        OgreticiAdimi(
            metin = "tutorial6000_4",
            ses = "tutorial6000_4",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_001",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 8 · satır 6067
        OgreticiAdimi(
            metin = "tutorial6000_5",
            ses = "tutorial6000_5",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_002",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 9 · satır 6075
        OgreticiAdimi(
            metin = "tutorial6000_6",
            ses = "tutorial6000_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(capa = CAPA_ALT_KILAVUZ),
                OgreticiOdakIslemi(sagBoslukDp = 64f),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 200f),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_002",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 10 · satır 6112
        OgreticiAdimi(
            metin = "tutorial6000_7",
            ses = "tutorial6000_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(genislikDp = 60f, yukseklikDp = 100f),
                OgreticiOdakIslemi(capa = CAPA_UST_KILAVUZ),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 100f),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_002",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 11 · satır 6151
        OgreticiAdimi(
            metin = "tutorial6000_8",
            ses = "tutorial6000_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_002",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 12 · satır 6159
        OgreticiAdimi(
            metin = "tutorial6_9",
            ses = "tutorial6_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", YOK),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_002",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 13 · satır 6172
        OgreticiAdimi(
            metin = "tutorial3_13",
            ses = "tutorial3_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_002",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 14 · satır 6183
        OgreticiAdimi(
            metin = "tutorial3_14",
            ses = "tutorial3_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 2),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_002",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 15 · satır 6197
        OgreticiAdimi(
            metin = "tutorial6000_9",
            ses = "tutorial6000_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_003",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 16 · satır 6205
        OgreticiAdimi(
            metin = "tutorial6000_9",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_003",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 17 · satır 6214
        OgreticiAdimi(
            metin = "tutorial6000_10",
            ses = "tutorial6000_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 18 · satır 6221
        OgreticiAdimi(
            metin = "tutorial6_13",
            ses = "tutorial6_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 19 · satır 6234
        OgreticiAdimi(
            metin = "tutorial6_14",
            ses = "tutorial6_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 1),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 20 · satır 6253
        OgreticiAdimi(
            metin = "tutorial6000_11",
            ses = "tutorial6000_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 21 · satır 6260
        OgreticiAdimi(
            metin = "tutorial6000_12",
            ses = "tutorial6000_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_002",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 22 · satır 6268
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 2),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_002",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 23 · satır 6283
        OgreticiAdimi(
            metin = "tutorial6000_13",
            ses = "tutorial6000_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_003",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 24 · satır 6291
        OgreticiAdimi(
            metin = "tutorial6000_13",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_003",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 25 · satır 6300
        OgreticiAdimi(
            metin = "tutorial6000_14",
            ses = "tutorial6000_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 4",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_001",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 26 · satır 6309
        OgreticiAdimi(
            metin = "tutorial1_108",
            ses = "tutorial1_108",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
            ),
            soru = "48 + 2",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 27 · satır 6323
        OgreticiAdimi(
            metin = "tutorial5_40",
            ses = "tutorial6000_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t6_48p2_ilk_sayi",
            soru = "48 + 2",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 48,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_052",
        ),
        // 28 · satır 6336
        OgreticiAdimi(
            metin = "tutorial6000_16",
            ses = "tutorial6000_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t6_48p2_2yi_10luk_kural",
            soru = "48 + 2",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 58,
            geriCevap = 48,
            ipucu = "tutorial_ipucu_053",
            kuralMetni = "tutorial_kural_004",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 29 · satır 6349
        OgreticiAdimi(
            metin = "tutorial6000_17",
            ses = "tutorial6000_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t6_48p2_2nin_buyuk_kardesi",
            soru = "48 + 2",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 50,
            geriCevap = 58,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_054",
            kuralMetni = "tutorial_kural_005",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 30 · satır 6365
        OgreticiAdimi(
            metin = "tutorial6_20",
            ses = "tutorial6_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 31 · satır 6384
        OgreticiAdimi(
            metin = "tutorial6000_18",
            ses = "tutorial6000_18",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 32 · satır 6391
        OgreticiAdimi(
            metin = "tutorial6000_1",
            ses = "tutorial6000_1",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 19f),
                OgreticiOdakIslemi(capa = CAPA_ALT_KILAVUZ),
                OgreticiOdakIslemi(sagBoslukDp = 19f, ustBoslukDp = -10f),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 200f),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 33 · satır 6433
        OgreticiAdimi(
            metin = "tutorial_txt_002",
            ses = "tutorial6000_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 19f),
                OgreticiOdakIslemi(capa = CAPA_UST_KILAVUZ),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 100f),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 34 · satır 6457
        OgreticiAdimi(
            metin = "tutorial6000_3",
            ses = "tutorial6000_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", YOK),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 35 · satır 6465
        OgreticiAdimi(
            metin = "tutorial6000_19",
            ses = "tutorial6000_19",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_006",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 36 · satır 6474
        OgreticiAdimi(
            metin = "tutorial6000_5",
            ses = "tutorial6000_5",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_007",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 37 · satır 6482
        OgreticiAdimi(
            metin = "tutorial6000_6",
            ses = "tutorial6000_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(capa = CAPA_ALT_KILAVUZ),
                OgreticiOdakIslemi(sagBoslukDp = 64f),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 200f),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_007",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 38 · satır 6519
        OgreticiAdimi(
            metin = "tutorial6000_7",
            ses = "tutorial6000_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(genislikDp = 60f, yukseklikDp = 100f),
                OgreticiOdakIslemi(capa = CAPA_UST_KILAVUZ),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 100f),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_007",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 39 · satır 6558
        OgreticiAdimi(
            metin = "tutorial6000_20",
            ses = "tutorial6000_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_007",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 40 · satır 6566
        OgreticiAdimi(
            metin = "tutorial6000_21",
            ses = "tutorial6000_21",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", YOK),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_007",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 41 · satır 6579
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_007",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 42 · satır 6590
        OgreticiAdimi(
            metin = "tutorial6000_22",
            ses = "tutorial6000_22",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 2),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_007",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 43 · satır 6605
        OgreticiAdimi(
            metin = "tutorial6000_23",
            ses = "tutorial6000_23",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_008",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 44 · satır 6613
        OgreticiAdimi(
            metin = "tutorial6000_23",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_008",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 45 · satır 6623
        OgreticiAdimi(
            metin = "tutorial6000_24",
            ses = "tutorial6000_24",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 46 · satır 6630
        OgreticiAdimi(
            metin = "tutorial6_13",
            ses = "tutorial6_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 47 · satır 6640
        OgreticiAdimi(
            metin = "tutorial6_31",
            ses = "tutorial6_31",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 48 · satır 6660
        OgreticiAdimi(
            metin = "tutorial6000_25",
            ses = "tutorial6000_25",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 49 · satır 6667
        OgreticiAdimi(
            metin = "tutorial6000_26",
            ses = "tutorial6000_26",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_007",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 50 · satır 6675
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial6_34",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_007",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 51 · satır 6690
        OgreticiAdimi(
            metin = "tutorial6000_27",
            ses = "tutorial6000_27",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_008",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 52 · satır 6698
        OgreticiAdimi(
            metin = "tutorial6000_27",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
            kuralMetni = "tutorial_kural_008",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 53 · satır 6707
        OgreticiAdimi(
            metin = "tutorial6000_28",
            ses = "tutorial6000_28",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 54 · satır 6714
        OgreticiAdimi(
            metin = "tutorial1_108",
            ses = "tutorial1_108",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
            ),
            soru = "96 + 5",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
            sonrakindeSifirla = true,
        ),
        // 55 · satır 6727
        OgreticiAdimi(
            metin = "tutorial5_40",
            ses = "tutorial6000_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t6_96p5_ilk_sayi",
            soru = "96 + 5",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 96,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_055",
        ),
        // 56 · satır 6740
        OgreticiAdimi(
            metin = "tutorial6000_29",
            ses = "tutorial6000_29",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t6_96p5_5i_10luk_kuralla",
            soru = "96 + 5",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 106,
            geriCevap = 96,
            ipucu = "tutorial_ipucu_056",
            kuralMetni = "tutorial_kural_009",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 57 · satır 6753
        OgreticiAdimi(
            metin = "tutorial6000_30",
            ses = "tutorial6000_30",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t6_96p5_5in_buyuk_kardesi",
            soru = "96 + 5",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 101,
            geriCevap = 106,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_057",
            kuralMetni = "tutorial_kural_010",
            kuralMetniGorunurlugu = GORUNUR,
        ),
        // 58 · satır 6768
        OgreticiAdimi(
            metin = "tutorial3_50",
            ses = "tutorial6_37",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
    ),
)

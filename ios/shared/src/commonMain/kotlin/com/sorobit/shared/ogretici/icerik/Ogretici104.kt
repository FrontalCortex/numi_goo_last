// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 104
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps104 (satır 6779-7358).
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

internal fun ogretici104(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 104,
    kuralTablosu = null,
    adimlar = listOf(
        // 1 · satır 6782
        OgreticiAdimi(
            metin = "tutorial_txt_003",
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
            soru = "846 + 345",
            soruGorunurlugu = GORUNUR,
        ),
        // 2 · satır 6801
        OgreticiAdimi(
            metin = "tutorial2_101",
            ses = "tutorial6_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 3 · satır 6808
        OgreticiAdimi(
            metin = "tutorial_txt_004",
            ses = "tutorial6_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 4 · satır 6815
        OgreticiAdimi(
            metin = "tutorial6_4",
            ses = "tutorial6_4",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 5 · satır 6822
        OgreticiAdimi(
            metin = "tutorial6_5",
            ses = "tutorial6_5",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 6 · satır 6829
        OgreticiAdimi(
            metin = "tutorial6_6",
            ses = "tutorial6_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 64f),
            ),
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 7 · satır 6850
        OgreticiAdimi(
            metin = "tutorial6_7",
            ses = "tutorial6_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 8 · satır 6857
        OgreticiAdimi(
            metin = "tutorial6_8",
            ses = "tutorial6_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(genislikDp = 60f, yukseklikDp = 100f),
                OgreticiOdakIslemi(capa = CAPA_UST_KILAVUZ),
            ),
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 9 · satır 6886
        OgreticiAdimi(
            metin = "tutorial6_9",
            ses = "tutorial6_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 2),
            ),
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", YOK),
            ),
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 10 · satır 6906
        OgreticiAdimi(
            metin = "tutorial6_10",
            ses = "tutorial6_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 11 · satır 6913
        OgreticiAdimi(
            metin = "tutorial6_11",
            ses = "tutorial6_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 12 · satır 6921
        OgreticiAdimi(
            metin = "tutorial6_11",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 13 · satır 6928
        OgreticiAdimi(
            metin = "tutorial6_12",
            ses = "tutorial6_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 14 · satır 6935
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
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 15 · satır 6949
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
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 16 · satır 6968
        OgreticiAdimi(
            metin = "tutorial6_15",
            ses = "tutorial6_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 17 · satır 6975
        OgreticiAdimi(
            metin = "tutorial6_16",
            ses = "tutorial6_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 18 · satır 6982
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial6_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 2),
            ),
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 19 · satır 6997
        OgreticiAdimi(
            metin = "tutorial6_18",
            ses = "tutorial6_18",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 20 · satır 7004
        OgreticiAdimi(
            metin = "tutorial6_18",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 21 · satır 7011
        OgreticiAdimi(
            metin = "tutorial6_19",
            ses = "tutorial6_19",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "49 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 22 · satır 7018
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
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
            ),
            soru = "48 + 2",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 23 · satır 7033
        OgreticiAdimi(
            metin = "tutorial_txt_001",
            ses = "tutorial9_1000",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t104_48p2_ilk_sayi",
            soru = "48 + 2",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 48,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_052",
        ),
        // 24 · satır 7047
        OgreticiAdimi(
            metin = "tutorial6000_16",
            ses = "tutorial5_1000",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t104_48p2_2yi_10luk_kural",
            soru = "48 + 2",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 58,
            geriCevap = 48,
            ipucu = "tutorial_ipucu_053",
        ),
        // 25 · satır 7061
        OgreticiAdimi(
            metin = "tutorial5_1001",
            ses = "tutorial5_1001",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t104_48p2_2nin_buyuk_kardesini",
            soru = "48 + 2",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 50,
            geriCevap = 58,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_054",
        ),
        // 26 · satır 7075
        OgreticiAdimi(
            metin = "tutorial6_20",
            ses = "tutorial6_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 27 · satır 7082
        OgreticiAdimi(
            metin = "tutorial6_21",
            ses = "tutorial6_21",
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
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 28 · satır 7103
        OgreticiAdimi(
            metin = "tutorial6_22",
            ses = "tutorial6_22",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 29 · satır 7110
        OgreticiAdimi(
            metin = "tutorial6_23",
            ses = "tutorial6_23",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 30 · satır 7117
        OgreticiAdimi(
            metin = "tutorial6_24",
            ses = "tutorial6_24",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(genislikDp = 60f, yukseklikDp = 250f),
                OgreticiOdakIslemi(sagBoslukDp = 64f),
            ),
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 31 · satır 7151
        OgreticiAdimi(
            metin = "tutorial6_25",
            ses = "tutorial6_25",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", YOK),
            ),
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 32 · satır 7165
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial6_26",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
            ),
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 33 · satır 7174
        OgreticiAdimi(
            metin = "tutorial6_27",
            ses = "tutorial6_27",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 34 · satır 7181
        OgreticiAdimi(
            metin = "tutorial6_27",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
            ),
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 35 · satır 7193
        OgreticiAdimi(
            metin = "tutorial6_28",
            ses = "tutorial6_28",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 36 · satır 7200
        OgreticiAdimi(
            metin = "tutorial6_28",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 37 · satır 7207
        OgreticiAdimi(
            metin = "tutorial6_29",
            ses = "tutorial6_29",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 38 · satır 7214
        OgreticiAdimi(
            metin = "tutorial6_13",
            ses = "tutorial6_30",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
            ),
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 39 · satır 7225
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
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 40 · satır 7245
        OgreticiAdimi(
            metin = "tutorial6_32",
            ses = "tutorial6_32",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 41 · satır 7252
        OgreticiAdimi(
            metin = "tutorial6_23",
            ses = "tutorial6_33",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 42 · satır 7259
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
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 43 · satır 7273
        OgreticiAdimi(
            metin = "tutorial6_35",
            ses = "tutorial6_35",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 44 · satır 7280
        OgreticiAdimi(
            metin = "tutorial6_35",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 45 · satır 7287
        OgreticiAdimi(
            metin = "tutorial6_36",
            ses = "tutorial6_36",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "98 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 46 · satır 7294
        OgreticiAdimi(
            metin = "tutorial1_108",
            ses = "tutorial1_108",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
            ),
            soru = "96 + 5",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 47 · satır 7307
        OgreticiAdimi(
            metin = "tutorial_txt_001",
            ses = "tutorial9_1000",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t104_96p5_ilk_sayi",
            soru = "96 + 5",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 96,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_055",
        ),
        // 48 · satır 7321
        OgreticiAdimi(
            metin = "tutorial5_1002",
            ses = "tutorial5_1002",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t104_96p5_5i_10luk_kural",
            soru = "96 + 5",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 106,
            geriCevap = 96,
            ipucu = "tutorial_ipucu_056",
        ),
        // 49 · satır 7335
        OgreticiAdimi(
            metin = "tutorial5_1003",
            ses = "tutorial5_1003",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t104_96p5_5in_buyuk_kardesini",
            soru = "96 + 5",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 101,
            geriCevap = 106,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_057",
        ),
        // 50 · satır 7349
        OgreticiAdimi(
            metin = "tutorial3_50",
            ses = "tutorial6_37",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
    ),
)

// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 101
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps101 (satır 2656-3131).
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

internal fun ogretici101(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 101,
    kuralTablosu = null,
    adimlar = listOf(
        // 1 · satır 2658
        OgreticiAdimi(
            metin = "tutorial4_100",
            ses = "tutorial4_100",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 2 · satır 2664
        OgreticiAdimi(
            metin = "tutorial4_101",
            ses = "tutorial4_101",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 3 · satır 2670
        OgreticiAdimi(
            metin = "tutorial4_102",
            ses = "tutorial4_102",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "154",
            soruGorunurlugu = GORUNUR,
        ),
        // 4 · satır 2678
        OgreticiAdimi(
            metin = "tutorial4_103",
            ses = "tutorial4_103",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
            ),
            soru = "154",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 5 · satır 2690
        OgreticiAdimi(
            metin = "tutorial4_104",
            ses = "tutorial4_104",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
            ),
            soru = "154",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF)),
        ),
        // 6 · satır 2702
        OgreticiAdimi(
            metin = "tutorial4_105",
            ses = "tutorial4_105",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "154",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(2, 0xFF00BFFF)),
        ),
        // 7 · satır 2717
        OgreticiAdimi(
            metin = "tutorial3_118",
            ses = "tutorial3_118",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            soru = "687",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 8 · satır 2734
        OgreticiAdimi(
            metin = "tutorial4_107",
            ses = "tutorial4_107",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t101_687_yuzler_3",
            soru = "687",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 600,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_011",
        ),
        // 9 · satır 2749
        OgreticiAdimi(
            metin = "tutorial4_108",
            ses = "tutorial4_108",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t101_687_onlar_2",
            soru = "687",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 680,
            geriCevap = 600,
            ipucu = "tutorial_ipucu_012",
        ),
        // 10 · satır 2764
        OgreticiAdimi(
            metin = "tutorial4_109",
            ses = "tutorial4_109",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t101_687_birler",
            soru = "687",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(2, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 687,
            geriCevap = 680,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_013",
        ),
        // 11 · satır 2780
        OgreticiAdimi(
            metin = "tutorial4_3000",
            ses = "tutorial4_3000",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "7319",
            soruGorunurlugu = GORUNUR,
        ),
        // 12 · satır 2788
        OgreticiAdimi(
            metin = "tutorial4_3001",
            ses = "tutorial4_3001",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 1, ust = true, sira = 0, tur = 3),
            ),
            soru = "7319",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 13 · satır 2802
        OgreticiAdimi(
            metin = "tutorial4_3002",
            ses = "tutorial4_3002",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 1),
            ),
            soru = "7319",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF)),
        ),
        // 14 · satır 2816
        OgreticiAdimi(
            metin = "tutorial4_2008",
            ses = "tutorial4_2008",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "7319",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(2, 0xFF00BFFF)),
        ),
        // 15 · satır 2828
        OgreticiAdimi(
            metin = "tutorial4_105",
            ses = "tutorial4_105",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "7319",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(3, 0xFF00BFFF)),
        ),
        // 16 · satır 2844
        OgreticiAdimi(
            metin = "tutorial3_118",
            ses = "tutorial3_118",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 1, ust = true, sira = 0, tur = 4),
            ),
            soru = "4813",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 17 · satır 2867
        OgreticiAdimi(
            metin = "tutorial4_3003",
            ses = "tutorial4_3003",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t101_4813_binler_4",
            soru = "4813",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 4000,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_014",
        ),
        // 18 · satır 2882
        OgreticiAdimi(
            metin = "tutorial4_3004",
            ses = "tutorial4_3004",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t101_4813_yuzler_3",
            soru = "4813",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 4800,
            geriCevap = 4000,
            ipucu = "tutorial_ipucu_012",
        ),
        // 19 · satır 2897
        OgreticiAdimi(
            metin = "tutorial4_108",
            ses = "tutorial4_108",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t101_4813_onlar_2",
            soru = "4813",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(2, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 4810,
            geriCevap = 4800,
            ipucu = "tutorial_ipucu_015",
        ),
        // 20 · satır 2912
        OgreticiAdimi(
            metin = "tutorial4_109",
            ses = "tutorial4_109",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t101_4813_birler",
            soru = "4813",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(3, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 4813,
            geriCevap = 4810,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_016",
        ),
        // 21 · satır 2928
        OgreticiAdimi(
            metin = "tutorial101_0",
            ses = "tutorial101_0",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t101_soru_sayilari_hangi_basamaktan",
            ipucu = "tutorial_ipucu_017",
            secenekler = listOf("tutorial_sik_001", "tutorial_sik_002", "tutorial_sik_003"),
            dogruSecenekler = listOf(2),
            cokluSecim = false,
            secenekBasligi = "tutorial101_0",
        ),
        // 22 · satır 2944
        OgreticiAdimi(
            metin = "tutorial4_3000",
            ses = "tutorial4_3000",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "39125",
            soruGorunurlugu = GORUNUR,
        ),
        // 23 · satır 2952
        OgreticiAdimi(
            metin = "tutorial4_3005",
            ses = "tutorial4_3005",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 0, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 0, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 0, ust = false, sira = 2, tur = 1),
            ),
            soru = "39125",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 24 · satır 2966
        OgreticiAdimi(
            metin = "tutorial4_2006",
            ses = "tutorial4_2006",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 1, ust = true, sira = 0, tur = 3),
            ),
            soru = "39125",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF)),
        ),
        // 25 · satır 2982
        OgreticiAdimi(
            metin = "tutorial4_2007",
            ses = "tutorial4_2007",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
            ),
            soru = "39125",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(2, 0xFF00BFFF)),
        ),
        // 26 · satır 2994
        OgreticiAdimi(
            metin = "tutorial4_2008",
            ses = "tutorial4_2008",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
            ),
            soru = "39125",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(3, 0xFF00BFFF)),
        ),
        // 27 · satır 3007
        OgreticiAdimi(
            metin = "tutorial4_105",
            ses = "tutorial4_105",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "39125",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 28 · satır 3019
        OgreticiAdimi(
            metin = "tutorial3_118",
            ses = "tutorial4_106",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 1, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 0, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 0, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 0, ust = false, sira = 2, tur = 2),
            ),
            soru = "84192",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 29 · satır 3042
        OgreticiAdimi(
            metin = "tutorial4_2009",
            ses = "tutorial4_2009",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t101_84192_on_binler_5",
            soru = "84192",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 80000,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_012",
        ),
        // 30 · satır 3057
        OgreticiAdimi(
            metin = "tutorial4_3003",
            ses = "tutorial4_3003",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t101_84192_binler_4",
            soru = "84192",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 84000,
            geriCevap = 80000,
            ipucu = "tutorial_ipucu_014",
        ),
        // 31 · satır 3072
        OgreticiAdimi(
            metin = "tutorial4_107",
            ses = "tutorial4_107",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t101_84192_yuzler_3",
            soru = "84192",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(2, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 84100,
            geriCevap = 84000,
            ipucu = "tutorial_ipucu_015",
        ),
        // 32 · satır 3087
        OgreticiAdimi(
            metin = "tutorial4_2010",
            ses = "tutorial4_2010",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t101_84192_onlar_2",
            soru = "84192",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(3, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 84190,
            geriCevap = 84100,
            ipucu = "tutorial_ipucu_018",
        ),
        // 33 · satır 3102
        OgreticiAdimi(
            metin = "tutorial4_109",
            ses = "tutorial4_109",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t101_84192_birler",
            soru = "84192",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 84192,
            geriCevap = 84190,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_019",
        ),
        // 34 · satır 3118
        OgreticiAdimi(
            metin = "tutorial4_110",
            ses = "tutorial4_110",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 35 · satır 3124
        OgreticiAdimi(
            metin = "tutorial15_27",
            ses = "tutorial15_27",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

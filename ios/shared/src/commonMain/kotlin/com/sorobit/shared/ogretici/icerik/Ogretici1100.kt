// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 1100
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps1100 (satır 10244-10815).
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

internal fun ogretici1100(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 1100,
    kuralTablosu = "extractionFiveRuleTable",
    adimlar = listOf(
        // 1 · satır 10246
        OgreticiAdimi(
            metin = "tutorial11000_0",
            ses = "tutorial11000_0",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 2 · satır 10252
        OgreticiAdimi(
            metin = "tutorial9_2",
            ses = "tutorial9_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
        ),
        // 3 · satır 10260
        OgreticiAdimi(
            metin = "tutorial11000_1",
            ses = "tutorial11000_1",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 3),
            ),
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(2, 0xFF00BFFF)),
        ),
        // 4 · satır 10283
        OgreticiAdimi(
            metin = "tutorial11000_2",
            ses = "tutorial11000_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 5 · satır 10294
        OgreticiAdimi(
            metin = "tutorial11000_3",
            ses = "tutorial11000_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(capa = CAPA_ALT_KILAVUZ),
                OgreticiOdakIslemi(sagBoslukDp = 109f),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 200f),
            ),
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 6 · satır 10331
        OgreticiAdimi(
            metin = "tutorial8_31",
            ses = "tutorial8_31",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNMEZ),
            ),
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 7 · satır 10342
        OgreticiAdimi(
            metin = "tutorial8_31",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 4),
            ),
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 8 · satır 10353
        OgreticiAdimi(
            metin = "tutorial11000_7",
            ses = "tutorial11000_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 9 · satır 10364
        OgreticiAdimi(
            metin = "tutorial11000_7",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 3, tur = 1),
            ),
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 10 · satır 10375
        OgreticiAdimi(
            metin = "tutorial11000_4",
            ses = "tutorial11000_4",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 11 · satır 10386
        OgreticiAdimi(
            metin = "tutorial1100_5",
            ses = "tutorial1100_5",
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
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 12 · satır 10422
        OgreticiAdimi(
            metin = "tutorial1100_5",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNMEZ),
            ),
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 13 · satır 10434
        OgreticiAdimi(
            metin = "tutorial1100_6",
            ses = "tutorial1100_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(8, 0xFF00BFFF)),
        ),
        // 14 · satır 10446
        OgreticiAdimi(
            metin = "tutorial1100_7",
            ses = "tutorial1100_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(capa = CAPA_ALT_KILAVUZ),
                OgreticiOdakIslemi(sagBoslukDp = 19f),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 200f),
            ),
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(8, 0xFF00BFFF)),
        ),
        // 15 · satır 10483
        OgreticiAdimi(
            metin = "tutorial8_31",
            ses = "tutorial8_31",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNMEZ),
            ),
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(8, 0xFF00BFFF)),
        ),
        // 16 · satır 10495
        OgreticiAdimi(
            metin = "tutorial8_31",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(8, 0xFF00BFFF)),
        ),
        // 17 · satır 10506
        OgreticiAdimi(
            metin = "tutorial1100_2",
            ses = "tutorial1100_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(8, 0xFF00BFFF)),
        ),
        // 18 · satır 10517
        OgreticiAdimi(
            metin = "tutorial1100_2",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
            ),
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(8, 0xFF00BFFF)),
        ),
        // 19 · satır 10530
        OgreticiAdimi(
            metin = "tutorial1100_8",
            ses = "tutorial1100_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "875 - 422",
            soruGorunurlugu = GORUNUR,
        ),
        // 20 · satır 10538
        OgreticiAdimi(
            metin = "tutorial9_21",
            ses = "tutorial9_21",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 3, tur = 2),
            ),
            soru = "768 - 323",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 21 · satır 10557
        OgreticiAdimi(
            metin = "tutorial9_1000",
            ses = "tutorial9_1000",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t1100_768m323_ilk_sayi",
            soru = "768 - 323",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(2, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 768,
            abakusSifirla = true,
        ),
        // 22 · satır 10574
        OgreticiAdimi(
            metin = "tutorial1100_9",
            ses = "tutorial1100_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(capa = CAPA_UST_KILAVUZ),
                OgreticiOdakIslemi(sagBoslukDp = 109f),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 270f),
            ),
            soru = "768 - 323",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 23 · satır 10609
        OgreticiAdimi(
            metin = "tutorial1100_10",
            ses = "tutorial1100_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t1100_soru_hangi_kural_cikaracaksin",
            soru = "768 - 323",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
            geriCevap = 768,
            ipucu = "tutorial_ipucu_077",
            secenekler = listOf("tutorial_sik_026", "tutorial_sik_027", "tutorial_sik_028"),
            dogruSecenekler = listOf(1),
            cokluSecim = false,
            secenekBasligi = "tutorial_soru_007",
        ),
        // 24 · satır 10627
        OgreticiAdimi(
            metin = "tutorial1100_11",
            ses = "tutorial1100_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNMEZ),
            ),
            kimlik = "t1100_768m323_3u_5lik_cikarma",
            soru = "768 - 323",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 468,
            geriCevap = 768,
            ipucu = "tutorial_ipucu_078",
        ),
        // 25 · satır 10644
        OgreticiAdimi(
            metin = "tutorial4_1003",
            ses = "tutorial4_1003",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 64f),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 270f),
            ),
            soru = "768 - 323",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 26 · satır 10674
        OgreticiAdimi(
            metin = "tutorial1100_12",
            ses = "tutorial1100_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "768 - 323",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
            geriCevap = 468,
        ),
        // 27 · satır 10685
        OgreticiAdimi(
            metin = "tutorial1100_10",
            ses = "tutorial1100_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t1100_soru_hangi_kural_cikaracaksin_2",
            soru = "768 - 323",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
            geriCevap = 468,
            ipucu = "tutorial_ipucu_077",
            secenekler = listOf("tutorial_sik_026", "tutorial_sik_027", "tutorial_sik_028"),
            dogruSecenekler = listOf(1),
            cokluSecim = false,
            secenekBasligi = "tutorial_soru_008",
        ),
        // 28 · satır 10703
        OgreticiAdimi(
            metin = "tutorial1100_13",
            ses = "tutorial1100_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNMEZ),
            ),
            kimlik = "t1100_768m323_2yi_5lik_kurala",
            soru = "768 - 323",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 448,
            geriCevap = 468,
            ipucu = "tutorial_ipucu_079",
        ),
        // 29 · satır 10722
        OgreticiAdimi(
            metin = "tutorial9_27",
            ses = "tutorial9_27",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 19f),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 270f),
            ),
            soru = "768 - 323",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(8, 0xFF00BFFF)),
        ),
        // 30 · satır 10752
        OgreticiAdimi(
            metin = "tutorial1100_14",
            ses = "tutorial1100_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "768 - 323",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(8, 0xFF00BFFF)),
            geriCevap = 448,
        ),
        // 31 · satır 10764
        OgreticiAdimi(
            metin = "tutorial1100_10",
            ses = "tutorial1100_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t1100_soru_hangi_kural_cikaracaksin_3",
            soru = "768 - 323",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(8, 0xFF00BFFF)),
            geriCevap = 448,
            ipucu = "tutorial_ipucu_080",
            secenekler = listOf("tutorial_sik_026", "tutorial_sik_027", "tutorial_sik_028"),
            dogruSecenekler = listOf(0),
            cokluSecim = false,
            secenekBasligi = "tutorial_soru_009",
        ),
        // 32 · satır 10782
        OgreticiAdimi(
            metin = "tutorial1100_15",
            ses = "tutorial1100_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNMEZ),
            ),
            kimlik = "t1100_768m323_3u_birler_basamagindan",
            soru = "768 - 323",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(8, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 445,
            geriCevap = 448,
            ipucu = "tutorial_ipucu_041",
        ),
        // 33 · satır 10800
        OgreticiAdimi(
            metin = "tutorial1100_16",
            ses = "tutorial1100_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 34 · satır 10807
        OgreticiAdimi(
            metin = "tutorial15_27",
            ses = "tutorial15_27",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = YOK,
        ),
    ),
)

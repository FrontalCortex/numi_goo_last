// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 13
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps13 (satır 11368-11746).
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

internal fun ogretici13(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 13,
    kuralTablosu = "tenRuleExtractionTableLayout",
    adimlar = listOf(
        // 1 · satır 11370
        OgreticiAdimi(
            metin = "tutorial13_1",
            ses = "tutorial13_1",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 2 · satır 11376
        OgreticiAdimi(
            metin = "tutorial13_2",
            ses = "tutorial13_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
        ),
        // 3 · satır 11384
        OgreticiAdimi(
            metin = "tutorial13_3",
            ses = "tutorial13_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(2, 0xFF00BFFF)),
        ),
        // 4 · satır 11405
        OgreticiAdimi(
            metin = "tutorial13_4",
            ses = "tutorial13_4",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 5 · satır 11416
        OgreticiAdimi(
            metin = "tutorial13_5",
            ses = "tutorial13_5",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 64f),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 270f),
            ),
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 6 · satır 11450
        OgreticiAdimi(
            metin = "tutorial13_6",
            ses = "tutorial13_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", YOK),
            ),
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 7 · satır 11463
        OgreticiAdimi(
            metin = "tutorial13_7",
            ses = "tutorial13_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 8 · satır 11474
        OgreticiAdimi(
            metin = "tutorial13_8",
            ses = "tutorial13_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 64f),
                OgreticiOdakIslemi(capa = CAPA_UST_KILAVUZ),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 70f),
            ),
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 9 · satır 11515
        OgreticiAdimi(
            metin = "tutorial13_9",
            ses = "tutorial13_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", YOK),
            ),
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 10 · satır 11528
        OgreticiAdimi(
            metin = "tutorial12_17",
            ses = "tutorial13_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 11 · satır 11539
        OgreticiAdimi(
            metin = "tutorial12_17",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
            ),
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 12 · satır 11549
        OgreticiAdimi(
            metin = "tutorial13_11",
            ses = "tutorial13_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 13 · satır 11560
        OgreticiAdimi(
            metin = "tutorial13_11",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
            ),
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 14 · satır 11572
        OgreticiAdimi(
            metin = "tutorial13_12",
            ses = "tutorial13_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 15 · satır 11583
        OgreticiAdimi(
            metin = "tutorial13_13",
            ses = "tutorial13_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 16 · satır 11594
        OgreticiAdimi(
            metin = "tutorial12_17",
            ses = "tutorial13_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 17 · satır 11605
        OgreticiAdimi(
            metin = "tutorial12_17",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 2),
            ),
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 18 · satır 11615
        OgreticiAdimi(
            metin = "tutorial13_14",
            ses = "tutorial13_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 19 · satır 11626
        OgreticiAdimi(
            metin = "tutorial13_14",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 20 · satır 11636
        OgreticiAdimi(
            metin = "tutorial13_15",
            ses = "tutorial13_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "128 - 39",
            soruGorunurlugu = GORUNUR,
        ),
        // 21 · satır 11644
        OgreticiAdimi(
            metin = "tutorial7_24",
            ses = "tutorial13_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
            ),
            soru = "165 - 96",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 22 · satır 11664
        OgreticiAdimi(
            metin = "tutorial9_1000",
            ses = "tutorial9_1000",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t13_165m96_ilk_sayi",
            soru = "165 - 96",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(2, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 165,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_082",
        ),
        // 23 · satır 11683
        OgreticiAdimi(
            metin = "tutorial13_18",
            ses = "tutorial13_18",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "165 - 96",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
            geriCevap = 165,
        ),
        // 24 · satır 11695
        OgreticiAdimi(
            metin = "tutorial13_19",
            ses = "tutorial13_19",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t13_165m96_onlar_basamagindan_9u",
            soru = "165 - 96",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 75,
            geriCevap = 165,
            ipucu = "tutorial_ipucu_083",
        ),
        // 25 · satır 11712
        OgreticiAdimi(
            metin = "tutorial13_20",
            ses = "tutorial13_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t13_165m96_birler_basamagindan_6yi",
            soru = "165 - 96",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 69,
            geriCevap = 75,
            ipucu = "tutorial_ipucu_084",
        ),
        // 26 · satır 11729
        OgreticiAdimi(
            metin = "tutorial13_21",
            ses = "tutorial13_21",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "165 - 96",
            soruGorunurlugu = GORUNUR,
        ),
        // 27 · satır 11737
        OgreticiAdimi(
            metin = "tutorial13_22",
            ses = "tutorial13_22",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

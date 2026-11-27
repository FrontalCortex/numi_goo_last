// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 19
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps19 (satır 14767-15297).
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

internal fun ogretici19(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 19,
    kuralTablosu = null,
    adimlar = listOf(
        // 1 · satır 14769
        OgreticiAdimi(
            metin = "tutorial19_1",
            ses = "tutorial19_1",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 2 · satır 14775
        OgreticiAdimi(
            metin = "tutorial13_2",
            ses = "tutorial13_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "97 x 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 3 · satır 14783
        OgreticiAdimi(
            metin = "tutorial19_2",
            ses = "tutorial19_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "97 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_045",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 4 · satır 14798
        OgreticiAdimi(
            metin = "tutorial19_2",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 1),
            ),
            soru = "97 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_045",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 5 · satır 14817
        OgreticiAdimi(
            metin = "tutorial19_3",
            ses = "tutorial19_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "97 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_046",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 6 · satır 14832
        OgreticiAdimi(
            metin = "tutorial19_4",
            ses = "tutorial19_4",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "97 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_046",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 7 · satır 14847
        OgreticiAdimi(
            metin = "tutorial19_4",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 3),
            ),
            soru = "97 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_046",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 8 · satır 14861
        OgreticiAdimi(
            metin = "tutorial19_5",
            ses = "tutorial19_5",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 64f),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 280f),
            ),
            soru = "97 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_046",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 9 · satır 14898
        OgreticiAdimi(
            metin = "tutorial19_6",
            ses = "tutorial19_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", YOK),
            ),
            soru = "97 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_046",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 10 · satır 14915
        OgreticiAdimi(
            metin = "tutorial19_7",
            ses = "tutorial19_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "97 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_046",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 11 · satır 14930
        OgreticiAdimi(
            metin = "tutorial19_7",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 2),
            ),
            soru = "97 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_046",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 12 · satır 14945
        OgreticiAdimi(
            metin = "tutorial19_8",
            ses = "tutorial19_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "97 x 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 13 · satır 14953
        OgreticiAdimi(
            metin = "tutorial4_2",
            ses = "tutorial4_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 4),
            ),
            soru = "84 x 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 14 · satır 14969
        OgreticiAdimi(
            metin = "tutorial19_9",
            ses = "tutorial19_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "84 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_037",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 15 · satır 14985
        OgreticiAdimi(
            metin = "tutorial19_9",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "84 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_037",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 16 · satır 15004
        OgreticiAdimi(
            metin = "tutorial19_10",
            ses = "tutorial19_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "84 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_047",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 17 · satır 15019
        OgreticiAdimi(
            metin = "tutorial19_11",
            ses = "tutorial19_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "84 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_047",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 18 · satır 15034
        OgreticiAdimi(
            metin = "tutorial19_11",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 3, tur = 1),
            ),
            soru = "84 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_047",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 19 · satır 15051
        OgreticiAdimi(
            metin = "tutorial19_12",
            ses = "tutorial19_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            soru = "84 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_047",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 20 · satır 15068
        OgreticiAdimi(
            metin = "tutorial19_13",
            ses = "tutorial19_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", YOK),
            ),
            soru = "84 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_047",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 21 · satır 15085
        OgreticiAdimi(
            metin = "tutorial19_14",
            ses = "tutorial19_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "84 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_047",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 22 · satır 15100
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 3, tur = 2),
            ),
            soru = "84 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_047",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 23 · satır 15121
        OgreticiAdimi(
            metin = "tutorial3_30",
            ses = "tutorial3_30",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "84 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_047",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 24 · satır 15136
        OgreticiAdimi(
            metin = "tutorial3_30",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "84 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            kuralMetni = "tutorial_kural_047",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 25 · satır 15151
        OgreticiAdimi(
            metin = "tutorial19_15",
            ses = "tutorial19_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "84 x 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 26 · satır 15159
        OgreticiAdimi(
            metin = "tutorial19_22",
            ses = "tutorial19_22",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
        ),
        // 27 · satır 15173
        OgreticiAdimi(
            metin = "tutorial7_24",
            ses = "tutorial7_24",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "48 x 9",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 28 · satır 15182
        OgreticiAdimi(
            metin = "tutorial19_16",
            ses = "tutorial19_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t19_48x9_9_8i_carpip",
            soru = "48 x 9",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 72,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_108",
        ),
        // 29 · satır 15200
        OgreticiAdimi(
            metin = "tutorial19_17",
            ses = "tutorial19_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "48 x 9",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            geriCevap = 72,
        ),
        // 30 · satır 15213
        OgreticiAdimi(
            metin = "tutorial19_18",
            ses = "tutorial19_18",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t19_48x9_9_x_4",
            soru = "48 x 9",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 372,
            geriCevap = 72,
            ipucu = "tutorial_ipucu_109",
        ),
        // 31 · satır 15231
        OgreticiAdimi(
            metin = "tutorial19_19",
            ses = "tutorial19_19",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "48 x 9",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
        ),
        // 32 · satır 15242
        OgreticiAdimi(
            metin = "tutorial19_23",
            ses = "tutorial19_23",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t19_soru_hangi_toplama_kurali",
            soru = "48 x 9",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            geriCevap = 372,
            ipucu = "tutorial_ipucu_110",
            secenekler = listOf("tutorial_sik_012", "tutorial_sik_013", "tutorial_sik_023"),
            dogruSecenekler = listOf(2),
            cokluSecim = false,
            secenekBasligi = "tutorial19_23",
        ),
        // 33 · satır 15261
        OgreticiAdimi(
            metin = "tutorial9_1002",
            ses = "tutorial9_1002",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t19_48x9_6yi_kurali_onlar",
            soru = "48 x 9",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 432,
            geriCevap = 372,
            ipucu = "tutorial_ipucu_111",
            kuralMetni = "tutorial_kural_048",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 34 · satır 15282
        OgreticiAdimi(
            metin = "tutorial19_21",
            ses = "tutorial19_21",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "48 x 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 35 · satır 15290
        OgreticiAdimi(
            metin = "tutorial15_27",
            ses = "tutorial15_27",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

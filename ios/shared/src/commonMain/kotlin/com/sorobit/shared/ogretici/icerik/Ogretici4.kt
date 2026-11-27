// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 4
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps4 (satır 4861-5157).
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

internal fun ogretici4(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 4,
    kuralTablosu = "fiveRuleTable",
    adimlar = listOf(
        // 1 · satır 4863
        OgreticiAdimi(
            metin = "tutorial4_1000",
            ses = "tutorial4_1000",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 2 · satır 4869
        OgreticiAdimi(
            metin = "tutorial4_2",
            ses = "tutorial4_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
        ),
        // 3 · satır 4877
        OgreticiAdimi(
            metin = "tutorial4_3",
            ses = "tutorial4_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 1),
            ),
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(2, 0xFF00BFFF)),
        ),
        // 4 · satır 4897
        OgreticiAdimi(
            metin = "tutorial4_1001",
            ses = "tutorial4_1001",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 5 · satır 4906
        OgreticiAdimi(
            metin = "tutorial4_1010",
            ses = "tutorial4_1010",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 6 · satır 4915
        OgreticiAdimi(
            metin = "tutorial3_13",
            ses = "tutorial3_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 7 · satır 4924
        OgreticiAdimi(
            metin = "tutorial3_13",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 3),
            ),
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 8 · satır 4934
        OgreticiAdimi(
            metin = "tutorial4_1002",
            ses = "tutorial4_1002",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 9 · satır 4943
        OgreticiAdimi(
            metin = "tutorial4_1002",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 2),
            ),
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 10 · satır 4954
        OgreticiAdimi(
            metin = "tutorial4_1003",
            ses = "tutorial4_1003",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 11 · satır 4963
        OgreticiAdimi(
            metin = "tutorial4_2000",
            ses = "tutorial4_2000",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 12 · satır 4972
        OgreticiAdimi(
            metin = "tutorial3_13",
            ses = "tutorial3_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 13 · satır 4981
        OgreticiAdimi(
            metin = "tutorial3_13",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
            ),
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 14 · satır 4991
        OgreticiAdimi(
            metin = "tutorial3_37",
            ses = "tutorial3_37",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 15 · satır 5000
        OgreticiAdimi(
            metin = "tutorial3_37",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 2),
            ),
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
        ),
        // 16 · satır 5010
        OgreticiAdimi(
            metin = "tutorial4_2001",
            ses = "tutorial4_2001",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(8, 0xFF00BFFF)),
        ),
        // 17 · satır 5019
        OgreticiAdimi(
            metin = "tutorial4_2001",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(8, 0xFF00BFFF)),
        ),
        // 18 · satır 5030
        OgreticiAdimi(
            metin = "tutorial4_2002",
            ses = "tutorial4_2002",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
        ),
        // 19 · satır 5038
        OgreticiAdimi(
            metin = "tutorial4_2002",
            daktiloHiziMs = 40L,
            kimlik = "t4_soru_cevap_589",
            soru = "243 + 346",
            soruGorunurlugu = GORUNUR,
            ipucu = "tutorial_ipucu_042",
            secenekler = listOf("tutorial_sik_009", "tutorial_sik_010"),
            dogruSecenekler = listOf(0),
            cokluSecim = false,
            secenekBasligi = "tutorial_soru_003",
        ),
        // 20 · satır 5053
        OgreticiAdimi(
            metin = "tutorial1_108",
            ses = "tutorial3_45",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            abakusSifirla = true,
        ),
        // 21 · satır 5072
        OgreticiAdimi(
            metin = "tutorial3_46",
            ses = "tutorial3_46",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t4_423p322_ilk_sayi",
            soru = "423 + 322",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(2, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 423,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_043",
        ),
        // 22 · satır 5087
        OgreticiAdimi(
            metin = "tutorial4_1005",
            ses = "tutorial4_1005",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "423 + 322",
            soruGorunurlugu = GORUNUR,
            geriCevap = 423,
        ),
        // 23 · satır 5096
        OgreticiAdimi(
            metin = "tutorial4_1006",
            ses = "tutorial4_1006",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t4_423p322_300u_ekleyip",
            soru = "423 + 322",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 723,
            geriCevap = 423,
            ipucu = "tutorial_ipucu_044",
        ),
        // 24 · satır 5111
        OgreticiAdimi(
            metin = "tutorial4_2003",
            ses = "tutorial4_2003",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t4_423p322_20yi_ekleyip",
            soru = "423 + 322",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(7, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 743,
            geriCevap = 723,
            ipucu = "tutorial_ipucu_025",
        ),
        // 25 · satır 5126
        OgreticiAdimi(
            metin = "tutorial4_2004",
            ses = "tutorial4_2004",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t4_423p322_2yi_ekleyip",
            soru = "423 + 322",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(8, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 745,
            geriCevap = 743,
            ipucu = "tutorial_ipucu_045",
        ),
        // 26 · satır 5141
        OgreticiAdimi(
            metin = "tutorial4_2005",
            ses = "tutorial4_2005",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "423 + 322",
            soruGorunurlugu = GORUNUR,
            ipucu = "tutorial_ipucu_039",
        ),
        // 27 · satır 5150
        OgreticiAdimi(
            metin = "tutorial15_27",
            ses = "tutorial15_27",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

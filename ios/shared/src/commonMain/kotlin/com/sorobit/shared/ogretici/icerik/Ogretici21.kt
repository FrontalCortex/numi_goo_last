// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 21
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps21 (satır 16029-16298).
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

internal fun ogretici21(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 21,
    kuralTablosu = null,
    adimlar = listOf(
        // 1 · satır 16031
        OgreticiAdimi(
            metin = "tutorial21_1",
            ses = "tutorial21_1",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 2 · satır 16037
        OgreticiAdimi(
            metin = "tutorial21_2",
            ses = "tutorial21_2",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 3 · satır 16043
        OgreticiAdimi(
            metin = "tutorial21_3",
            ses = "tutorial21_3",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 4 · satır 16049
        OgreticiAdimi(
            metin = "tutorial13_2",
            ses = "tutorial13_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "300 x 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 5 · satır 16057
        OgreticiAdimi(
            metin = "tutorial20_10",
            ses = "tutorial21_4",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "300 x 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 6 · satır 16065
        OgreticiAdimi(
            metin = "tutorial21_5",
            ses = "tutorial21_5",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "300 x 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 7 · satır 16073
        OgreticiAdimi(
            metin = "tutorial21_5",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 1),
            ),
            soru = "300 x 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 8 · satır 16084
        OgreticiAdimi(
            metin = "tutorial21_6",
            ses = "tutorial21_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 2),
            ),
        ),
        // 9 · satır 16096
        OgreticiAdimi(
            metin = "tutorial21_7",
            ses = "tutorial21_7",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 10 · satır 16102
        OgreticiAdimi(
            metin = "tutorial21_8",
            ses = "tutorial21_8",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 11 · satır 16108
        OgreticiAdimi(
            metin = "tutorial21_9",
            ses = "tutorial21_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "284 x 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 12 · satır 16116
        OgreticiAdimi(
            metin = "tutorial21_10",
            ses = "tutorial21_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "284 x 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(2, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFFFFFF00)),
        ),
        // 13 · satır 16128
        OgreticiAdimi(
            metin = "tutorial21_11",
            ses = "tutorial21_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "284 x 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(2, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFFFFFF00)),
        ),
        // 14 · satır 16140
        OgreticiAdimi(
            metin = "tutorial21_11",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "284 x 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(2, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFFFFFF00)),
        ),
        // 15 · satır 16153
        OgreticiAdimi(
            metin = "tutorial21_12",
            ses = "tutorial21_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "284 x 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFFFFFF00)),
        ),
        // 16 · satır 16165
        OgreticiAdimi(
            metin = "tutorial21_12",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 1),
            ),
            soru = "284 x 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFFFFFF00)),
        ),
        // 17 · satır 16180
        OgreticiAdimi(
            metin = "tutorial21_13",
            ses = "tutorial21_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "284 x 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFFFFFF00)),
        ),
        // 18 · satır 16192
        OgreticiAdimi(
            metin = "tutorial21_13",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 2),
            ),
            soru = "284 x 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFFFFFF00)),
        ),
        // 19 · satır 16205
        OgreticiAdimi(
            metin = "tutorial21_14",
            ses = "tutorial21_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "284 x 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 20 · satır 16213
        OgreticiAdimi(
            metin = "tutorial5_39",
            ses = "tutorial5_39",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "723 x 6",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 21 · satır 16231
        OgreticiAdimi(
            metin = "tutorial21_15",
            ses = "tutorial21_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t21_723x6_6_3u_carpip",
            soru = "723 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(2, 0xFFFFFF00), OgreticiRenkliHarf(6, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 18,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_119",
        ),
        // 22 · satır 16248
        OgreticiAdimi(
            metin = "tutorial21_16",
            ses = "tutorial21_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t21_723x6_6_onlar_basamaginda",
            soru = "723 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFFFFFF00), OgreticiRenkliHarf(6, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 138,
            geriCevap = 18,
            ipucu = "tutorial_ipucu_120",
        ),
        // 23 · satır 16265
        OgreticiAdimi(
            metin = "tutorial21_17",
            ses = "tutorial21_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t21_723x6_6_yuzler_basamaginda",
            soru = "723 x 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFFFFFF00), OgreticiRenkliHarf(6, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 4338,
            geriCevap = 138,
            ipucu = "tutorial_ipucu_121",
        ),
        // 24 · satır 16283
        OgreticiAdimi(
            metin = "tutorial21_18",
            ses = "tutorial21_18",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "723 x 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 25 · satır 16291
        OgreticiAdimi(
            metin = "tutorial15_27",
            ses = "tutorial15_27",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

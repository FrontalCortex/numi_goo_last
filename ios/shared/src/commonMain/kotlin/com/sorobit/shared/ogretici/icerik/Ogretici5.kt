// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 5
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps5 (satır 5160-5950).
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

internal fun ogretici5(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 5,
    kuralTablosu = "tenRuleTable",
    adimlar = listOf(
        // 1 · satır 5162
        OgreticiAdimi(
            metin = "tutorial5_1",
            ses = "tutorial5_1",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 2 · satır 5169
        OgreticiAdimi(
            metin = "tutorial5_2",
            ses = "tutorial5_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 3 · satır 5176
        OgreticiAdimi(
            metin = "tutorial5_3",
            ses = "tutorial5_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 4 · satır 5192
        OgreticiAdimi(
            metin = "tutorial5_4",
            ses = "tutorial5_4",
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
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 5 · satır 5227
        OgreticiAdimi(
            metin = "tutorial5_5",
            ses = "tutorial5_5",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 19f),
                OgreticiOdakIslemi(capa = CAPA_UST_KILAVUZ),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 100f),
            ),
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 6 · satır 5251
        OgreticiAdimi(
            metin = "tutorial5_6",
            ses = "tutorial5_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 7 · satır 5261
        OgreticiAdimi(
            metin = "tutorial5_7",
            ses = "tutorial5_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 8 · satır 5271
        OgreticiAdimi(
            metin = "tutorial5000_2",
            ses = "tutorial5000_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", YOK),
            ),
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 9 · satır 5288
        OgreticiAdimi(
            metin = "tutorial5_8",
            ses = "tutorial5_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 10 · satır 5295
        OgreticiAdimi(
            metin = "tutorial5_9",
            ses = "tutorial5_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 11 · satır 5302
        OgreticiAdimi(
            metin = "tutorial5_10",
            ses = "tutorial5_10",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 12 · satır 5308
        OgreticiAdimi(
            metin = "tutorial5_11",
            ses = "tutorial5_11",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 13 · satır 5314
        OgreticiAdimi(
            metin = "tutorial5_12",
            ses = "tutorial5_12",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 14 · satır 5320
        OgreticiAdimi(
            metin = "tutorial5_13",
            ses = "tutorial5_13",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 15 · satır 5326
        OgreticiAdimi(
            metin = "tutorial5_14",
            ses = "tutorial5_14",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 16 · satır 5332
        OgreticiAdimi(
            metin = "tutorial5_15",
            ses = "tutorial5_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
        ),
        // 17 · satır 5347
        OgreticiAdimi(
            metin = "tutorial5_16",
            ses = "tutorial5_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 18 · satır 5356
        OgreticiAdimi(
            metin = "tutorial5_17",
            ses = "tutorial5_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", GORUNUR),
            ),
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 19f),
                OgreticiOdakIslemi(capa = CAPA_ALT_KILAVUZ),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 200f),
                OgreticiOdakIslemi(sagBoslukDp = 19f, ustBoslukDp = -10f),
            ),
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 19 · satır 5391
        OgreticiAdimi(
            metin = "tutorial5_18",
            ses = "tutorial5_18",
            daktilo = true,
            daktiloHiziMs = 40L,
            odak = listOf(
                OgreticiOdakIslemi(sagBoslukDp = 19f),
                OgreticiOdakIslemi(capa = CAPA_UST_KILAVUZ),
                OgreticiOdakIslemi(genislikDp = 55f, yukseklikDp = 100f),
            ),
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 20 · satır 5414
        OgreticiAdimi(
            metin = "tutorial5_19",
            ses = "tutorial5_19",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("focusView", YOK),
            ),
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 21 · satır 5426
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 22 · satır 5435
        OgreticiAdimi(
            metin = "tutorial5_20",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 23 · satır 5445
        OgreticiAdimi(
            metin = "tutorial5_21",
            ses = "tutorial5_21",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 24 · satır 5454
        OgreticiAdimi(
            metin = "tutorial5_21",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 25 · satır 5468
        OgreticiAdimi(
            metin = "tutorial5_22",
            ses = "tutorial5_22",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 26 · satır 5477
        OgreticiAdimi(
            metin = "tutorial5000_1",
            ses = "tutorial5000_1",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "9 + 1",
            soruGorunurlugu = GORUNUR,
        ),
        // 27 · satır 5485
        OgreticiAdimi(
            metin = "tutorial5_23",
            ses = "tutorial5_23",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "8 + 2",
            soruGorunurlugu = GORUNUR,
        ),
        // 28 · satır 5496
        OgreticiAdimi(
            metin = "tutorial5_24",
            ses = "tutorial5_24",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "8 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 29 · satır 5511
        OgreticiAdimi(
            metin = "tutorial5_25",
            ses = "tutorial5_25",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "8 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 30 · satır 5520
        OgreticiAdimi(
            metin = "tutorial5_26",
            ses = "tutorial5_26",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "8 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 31 · satır 5529
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "8 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 32 · satır 5538
        OgreticiAdimi(
            metin = "tutorial5_20",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "8 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 33 · satır 5548
        OgreticiAdimi(
            metin = "tutorial5_27",
            ses = "tutorial5_27",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "8 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 34 · satır 5557
        OgreticiAdimi(
            metin = "tutorial5_27",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "8 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 35 · satır 5570
        OgreticiAdimi(
            metin = "tutorial5_22",
            ses = "tutorial5_22",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "8 + 2",
            soruGorunurlugu = GORUNUR,
        ),
        // 36 · satır 5578
        OgreticiAdimi(
            metin = "tutorial5_28",
            ses = "tutorial5_28",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "9 + 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 37 · satır 5589
        OgreticiAdimi(
            metin = "tutorial5_29",
            ses = "tutorial5_29",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "9 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 38 · satır 5605
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "9 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 39 · satır 5614
        OgreticiAdimi(
            metin = "tutorial5_20",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "9 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 40 · satır 5624
        OgreticiAdimi(
            metin = "tutorial5_30",
            ses = "tutorial5_30",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "9 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 41 · satır 5633
        OgreticiAdimi(
            metin = "tutorial5_30",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "9 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 42 · satır 5645
        OgreticiAdimi(
            metin = "tutorial5_31",
            ses = "tutorial5_31",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "9 + 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 43 · satır 5653
        OgreticiAdimi(
            metin = "tutorial5_28",
            ses = "tutorial5_32",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "7 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 44 · satır 5666
        OgreticiAdimi(
            metin = "tutorial5_33",
            ses = "tutorial5_33",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "7 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 45 · satır 5680
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "7 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 46 · satır 5689
        OgreticiAdimi(
            metin = "tutorial5_20",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "7 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 47 · satır 5699
        OgreticiAdimi(
            metin = "tutorial5_34",
            ses = "tutorial5_34",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "7 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 48 · satır 5708
        OgreticiAdimi(
            metin = "tutorial5_34",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "7 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 49 · satır 5719
        OgreticiAdimi(
            metin = "tutorial5_35",
            ses = "tutorial5_35",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "7 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 50 · satır 5728
        OgreticiAdimi(
            metin = "tutorial5_28",
            ses = "tutorial5_32",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "6 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 51 · satır 5740
        OgreticiAdimi(
            metin = "tutorial5_36",
            ses = "tutorial5_36",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "6 + 5",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 52 · satır 5753
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "6 + 5",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 53 · satır 5762
        OgreticiAdimi(
            metin = "tutorial5_20",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "6 + 5",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 54 · satır 5772
        OgreticiAdimi(
            metin = "tutorial5_37",
            ses = "tutorial5_37",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "6 + 5",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 55 · satır 5781
        OgreticiAdimi(
            metin = "tutorial5_37",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "6 + 5",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 56 · satır 5791
        OgreticiAdimi(
            metin = "tutorial5_38",
            ses = "tutorial5_38",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "6 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 57 · satır 5799
        OgreticiAdimi(
            metin = "tutorial5_46",
            ses = "tutorial5_46",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t5_soru_10luk_kurali_hangi",
            ipucu = "tutorial_ipucu_046",
            secenekler = listOf("tutorial_sik_011", "tutorial_sik_012", "tutorial_sik_013"),
            dogruSecenekler = listOf(0, 1),
            cokluSecim = true,
            secenekBasligi = "tutorial5_46",
        ),
        // 58 · satır 5815
        OgreticiAdimi(
            metin = "tutorial5_39",
            ses = "tutorial5_39",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "18 + 4",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 59 · satır 5828
        OgreticiAdimi(
            metin = "tutorial5_40",
            ses = "tutorial5_40",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t5_18p4_ilk_sayi",
            soru = "18 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 18,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_047",
        ),
        // 60 · satır 5843
        OgreticiAdimi(
            metin = "tutorial5_41",
            ses = "tutorial5_41",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "18 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
            geriCevap = 18,
        ),
        // 61 · satır 5853
        OgreticiAdimi(
            metin = "tutorial5_42",
            ses = "tutorial5_42",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t5_18p4_yaz",
            soru = "18 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 22,
            geriCevap = 18,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_048",
        ),
        // 62 · satır 5869
        OgreticiAdimi(
            metin = "tutorial5_43",
            ses = "tutorial5_43",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "18 + 4",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 63 · satır 5878
        OgreticiAdimi(
            metin = "tutorial3000_6",
            ses = "tutorial3000_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "17 + 3",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 64 · satır 5887
        OgreticiAdimi(
            metin = "tutorial_txt_001",
            ses = "tutorial9_1000",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t5_17p3_ilk_sayi",
            soru = "17 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 17,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_049",
        ),
        // 65 · satır 5902
        OgreticiAdimi(
            metin = "tutorial5000_3",
            ses = "tutorial5000_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "17 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
            geriCevap = 17,
        ),
        // 66 · satır 5912
        OgreticiAdimi(
            metin = "tutorial5_42",
            ses = "tutorial5_42",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t5_17p3_yaz",
            soru = "17 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 20,
            geriCevap = 17,
            ipucu = "tutorial_ipucu_050",
        ),
        // 67 · satır 5927
        OgreticiAdimi(
            metin = "tutorial5_45",
            ses = "tutorial5_45",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t5_soru_10luk_kurali_nasil",
            ipucu = "tutorial_ipucu_051",
            secenekler = listOf("tutorial_ipucu_051", "tutorial_sik_014", "tutorial_sik_015"),
            dogruSecenekler = listOf(0),
            cokluSecim = false,
            secenekBasligi = "tutorial5_45",
        ),
        // 68 · satır 5943
        OgreticiAdimi(
            metin = "tutorial5_44",
            ses = "tutorial5_44",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

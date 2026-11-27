// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 3
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps3 (satır 4037-4858).
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

internal fun ogretici3(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 3,
    kuralTablosu = "fiveRuleTable",
    adimlar = listOf(
        // 1 · satır 4039
        OgreticiAdimi(
            metin = "tutorial3_1",
            ses = "tutorial3_1",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 2 · satır 4046
        OgreticiAdimi(
            metin = "tutorial3000_1",
            ses = "tutorial3000_1",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 3 · satır 4053
        OgreticiAdimi(
            metin = "tutorial3000_2",
            ses = "tutorial3000_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "4 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 4 · satır 4069
        OgreticiAdimi(
            metin = "tutorial3000_3",
            ses = "tutorial3000_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 5 · satır 4079
        OgreticiAdimi(
            metin = "tutorial3000_4",
            ses = "tutorial3000_4",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 6 · satır 4092
        OgreticiAdimi(
            metin = "tutorial3_2",
            ses = "tutorial3_2",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 7 · satır 4098
        OgreticiAdimi(
            metin = "tutorial3_3",
            ses = "tutorial3_3",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 8 · satır 4104
        OgreticiAdimi(
            metin = "tutorial3_4",
            ses = "tutorial3_4",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 9 · satır 4110
        OgreticiAdimi(
            metin = "tutorial3_5",
            ses = "tutorial3_5",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 10 · satır 4116
        OgreticiAdimi(
            metin = "tutorial3_6",
            ses = "tutorial3_6",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 11 · satır 4122
        OgreticiAdimi(
            metin = "tutorial3000_5",
            ses = "tutorial3000_5",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 12 · satır 4128
        OgreticiAdimi(
            metin = "tutorial3_7",
            ses = "tutorial3_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 1",
            soruGorunurlugu = GORUNUR,
        ),
        // 13 · satır 4136
        OgreticiAdimi(
            metin = "tutorial3_8",
            ses = "tutorial3_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "4 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 14 · satır 4151
        OgreticiAdimi(
            metin = "tutorial3_9",
            ses = "tutorial3_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 15 · satır 4160
        OgreticiAdimi(
            metin = "tutorial3_10",
            ses = "tutorial3_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 16 · satır 4169
        OgreticiAdimi(
            metin = "tutorial3_11",
            ses = "tutorial3_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 17 · satır 4178
        OgreticiAdimi(
            metin = "tutorial3_3",
            ses = "tutorial3_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 18 · satır 4187
        OgreticiAdimi(
            metin = "tutorial3_13",
            ses = "tutorial3_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 19 · satır 4196
        OgreticiAdimi(
            metin = "tutorial3_13",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "4 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 20 · satır 4206
        OgreticiAdimi(
            metin = "tutorial3_14",
            ses = "tutorial3_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 21 · satır 4215
        OgreticiAdimi(
            metin = "tutorial3_14",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            soru = "4 + 1",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 22 · satır 4228
        OgreticiAdimi(
            metin = "tutorial3_15",
            ses = "tutorial3_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 1",
            soruGorunurlugu = GORUNUR,
        ),
        // 23 · satır 4236
        OgreticiAdimi(
            metin = "tutorial2_17",
            ses = "tutorial3_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "4 + 2",
            soruGorunurlugu = GORUNUR,
        ),
        // 24 · satır 4247
        OgreticiAdimi(
            metin = "tutorial3_17",
            ses = "tutorial3_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "4 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 25 · satır 4262
        OgreticiAdimi(
            metin = "tutorial3_18",
            ses = "tutorial3_18",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 26 · satır 4271
        OgreticiAdimi(
            metin = "tutorial3_19",
            ses = "tutorial3_19",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 27 · satır 4280
        OgreticiAdimi(
            metin = "tutorial3_20",
            ses = "tutorial3_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 28 · satır 4289
        OgreticiAdimi(
            metin = "tutorial3_13",
            ses = "tutorial3_21",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 29 · satır 4298
        OgreticiAdimi(
            metin = "tutorial3_13",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "4 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 30 · satır 4308
        OgreticiAdimi(
            metin = "tutorial3_22",
            ses = "tutorial3_22",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 31 · satır 4317
        OgreticiAdimi(
            metin = "tutorial3_22",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            soru = "4 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 32 · satır 4329
        OgreticiAdimi(
            metin = "tutorial3_23",
            ses = "tutorial3_23",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 33 · satır 4335
        OgreticiAdimi(
            metin = "tutorial2_17",
            ses = "tutorial3_24",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
            ),
            soru = "3 + 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 34 · satır 4347
        OgreticiAdimi(
            metin = "tutorial3_25",
            ses = "tutorial3_25",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
            ),
            soru = "3 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 35 · satır 4361
        OgreticiAdimi(
            metin = "tutorial3_26",
            ses = "tutorial3_26",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 36 · satır 4370
        OgreticiAdimi(
            metin = "tutorial3_19",
            ses = "tutorial3_27",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 37 · satır 4379
        OgreticiAdimi(
            metin = "tutorial3_28",
            ses = "tutorial3_28",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 38 · satır 4388
        OgreticiAdimi(
            metin = "tutorial3_13",
            ses = "tutorial3_29",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 39 · satır 4397
        OgreticiAdimi(
            metin = "tutorial3_13",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "3 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 40 · satır 4407
        OgreticiAdimi(
            metin = "tutorial3_30",
            ses = "tutorial3_30",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 41 · satır 4416
        OgreticiAdimi(
            metin = "tutorial3_30",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
            ),
            soru = "3 + 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 42 · satır 4427
        OgreticiAdimi(
            metin = "tutorial3_23",
            ses = "tutorial3_31",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 43 · satır 4433
        OgreticiAdimi(
            metin = "tutorial3_23",
            daktiloHiziMs = 40L,
            kimlik = "t3_soru_cevap_6",
            ipucu = "tutorial_ipucu_035",
            secenekler = listOf("tutorial_ipucu_035", "tutorial_sik_004", "tutorial_sik_005"),
            dogruSecenekler = listOf(0),
            cokluSecim = false,
            secenekBasligi = "tutorial_soru_001",
        ),
        // 44 · satır 4447
        OgreticiAdimi(
            metin = "tutorial3_32",
            ses = "tutorial3_32",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
            ),
            soru = "2 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 45 · satır 4459
        OgreticiAdimi(
            metin = "tutorial3_33",
            ses = "tutorial3_33",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
            ),
            soru = "2 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 46 · satır 4472
        OgreticiAdimi(
            metin = "tutorial3_34",
            ses = "tutorial3_34",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "2 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 47 · satır 4481
        OgreticiAdimi(
            metin = "tutorial3_19",
            ses = "tutorial3_35",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "2 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 48 · satır 4490
        OgreticiAdimi(
            metin = "tutorial3_6",
            ses = "tutorial3_36",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "2 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 49 · satır 4499
        OgreticiAdimi(
            metin = "tutorial3_13",
            ses = "tutorial3_29",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "2 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 50 · satır 4508
        OgreticiAdimi(
            metin = "tutorial3_13",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "2 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 51 · satır 4518
        OgreticiAdimi(
            metin = "tutorial3_37",
            ses = "tutorial3_37",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "2 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 52 · satır 4527
        OgreticiAdimi(
            metin = "tutorial3_37",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
            ),
            soru = "2 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 53 · satır 4537
        OgreticiAdimi(
            metin = "tutorial3_23",
            ses = "tutorial3_38",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "2 + 4",
            soruGorunurlugu = GORUNUR,
        ),
        // 54 · satır 4545
        OgreticiAdimi(
            metin = "tutorial3_23",
            daktiloHiziMs = 40L,
            kimlik = "t3_soru_cevap_6_2",
            soru = "2 + 4",
            soruGorunurlugu = GORUNUR,
            ipucu = "tutorial_ipucu_036",
            secenekler = listOf("tutorial_sik_006", "tutorial_sik_007", "tutorial_sik_008"),
            dogruSecenekler = listOf(1),
            cokluSecim = false,
            secenekBasligi = "tutorial_soru_002",
        ),
        // 55 · satır 4561
        OgreticiAdimi(
            metin = "tutorial3_39",
            ses = "tutorial3_39",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
            ),
        ),
        // 56 · satır 4571
        OgreticiAdimi(
            metin = "tutorial3_40",
            ses = "tutorial3_40",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "30 + 40",
            soruGorunurlugu = GORUNUR,
        ),
        // 57 · satır 4579
        OgreticiAdimi(
            metin = "tutorial3_41",
            ses = "tutorial3_41",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 1),
            ),
            soru = "30 + 40",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF)),
        ),
        // 58 · satır 4593
        OgreticiAdimi(
            metin = "tutorial3_42",
            ses = "tutorial3_42",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "30 + 40",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 59 · satır 4602
        OgreticiAdimi(
            metin = "tutorial3_19",
            ses = "tutorial3_43",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "30 + 40",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 60 · satır 4611
        OgreticiAdimi(
            metin = "tutorial3_6",
            ses = "tutorial3_36",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "30 + 40",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 61 · satır 4620
        OgreticiAdimi(
            metin = "tutorial3_13",
            ses = "tutorial3_29",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "30 + 40",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 62 · satır 4629
        OgreticiAdimi(
            metin = "tutorial3_13",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
            ),
            soru = "30 + 40",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 63 · satır 4639
        OgreticiAdimi(
            metin = "tutorial3_37",
            ses = "tutorial3_37",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "30 + 40",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 64 · satır 4648
        OgreticiAdimi(
            metin = "tutorial3_37",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
            ),
            soru = "30 + 40",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 65 · satır 4658
        OgreticiAdimi(
            metin = "tutorial3_44",
            ses = "tutorial3_44",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "30 + 40",
            soruGorunurlugu = GORUNUR,
        ),
        // 66 · satır 4666
        OgreticiAdimi(
            metin = "tutorial1_108",
            ses = "tutorial3_45",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
            ),
            abakusSifirla = true,
        ),
        // 67 · satır 4678
        OgreticiAdimi(
            metin = "tutorial3_46",
            ses = "tutorial3_46",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t3_4p4_ilk_sayi",
            soru = "4 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 4,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_037",
        ),
        // 68 · satır 4693
        OgreticiAdimi(
            metin = "tutorial3_47",
            ses = "tutorial3_47",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 69 · satır 4702
        OgreticiAdimi(
            metin = "tutorial3_48",
            ses = "tutorial3_48",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 70 · satır 4711
        OgreticiAdimi(
            metin = "tutorial3_6",
            ses = "tutorial3_36",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            geriCevap = 4,
        ),
        // 71 · satır 4721
        OgreticiAdimi(
            metin = "tutorial3_13",
            ses = "tutorial3_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t3_4p4_5_gelir",
            soru = "4 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 9,
            geriCevap = 4,
            ipucu = "tutorial_ipucu_038",
        ),
        // 72 · satır 4736
        OgreticiAdimi(
            metin = "tutorial3_37",
            ses = "tutorial3_37",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t3_4p4_kardesi_1_gider",
            soru = "4 + 4",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 8,
            geriCevap = 9,
            ipucu = "tutorial_ipucu_039",
        ),
        // 73 · satır 4751
        OgreticiAdimi(
            metin = "tutorial3_49",
            ses = "tutorial3_49",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 4",
            soruGorunurlugu = GORUNUR,
            geriCevap = 8,
            sonrakindeSifirla = true,
        ),
        // 74 · satır 4761
        OgreticiAdimi(
            metin = "tutorial3000_6",
            ses = "tutorial3000_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 2",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 75 · satır 4770
        OgreticiAdimi(
            metin = "tutorial3_46",
            ses = "tutorial3_46",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t3_3p2_ilk_sayi",
            soru = "3 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 3,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_040",
        ),
        // 76 · satır 4785
        OgreticiAdimi(
            metin = "tutorial3000_7",
            ses = "tutorial3000_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 77 · satır 4794
        OgreticiAdimi(
            metin = "tutorial3_48",
            ses = "tutorial3_48",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 78 · satır 4803
        OgreticiAdimi(
            metin = "tutorial3_20",
            ses = "tutorial3_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            geriCevap = 3,
        ),
        // 79 · satır 4813
        OgreticiAdimi(
            metin = "tutorial3_13",
            ses = "tutorial3_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t3_3p2_5_gelir",
            soru = "3 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 8,
            geriCevap = 3,
            ipucu = "tutorial_ipucu_038",
        ),
        // 80 · satır 4828
        OgreticiAdimi(
            metin = "tutorial3_22",
            ses = "tutorial3_22",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t3_3p2_kardesi_3_gider",
            soru = "3 + 2",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 5,
            geriCevap = 8,
            ipucu = "tutorial_ipucu_041",
        ),
        // 81 · satır 4843
        OgreticiAdimi(
            metin = "tutorial3_15",
            ses = "tutorial3_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 2",
            soruGorunurlugu = GORUNUR,
        ),
        // 82 · satır 4851
        OgreticiAdimi(
            metin = "tutorial3_50",
            ses = "tutorial3_50",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

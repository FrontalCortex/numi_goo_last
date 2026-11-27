// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 8
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps8 (satır 8433-9197).
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

internal fun ogretici8(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 8,
    kuralTablosu = "BeadRuleTable",
    adimlar = listOf(
        // 1 · satır 8435
        OgreticiAdimi(
            metin = "tutorial8_1",
            ses = "tutorial8_1",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 2 · satır 8441
        OgreticiAdimi(
            metin = "tutorial8_2",
            ses = "tutorial8_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 3 · satır 8448
        OgreticiAdimi(
            metin = "tutorial8_3",
            ses = "tutorial8_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 4 · satır 8455
        OgreticiAdimi(
            metin = "tutorial8_4",
            ses = "tutorial8_4",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 5 · satır 8462
        OgreticiAdimi(
            metin = "tutorial8_5",
            ses = "tutorial8_5",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
            ),
            soru = "1 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 6 · satır 8473
        OgreticiAdimi(
            metin = "tutorial8_5",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "1 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 7 · satır 8482
        OgreticiAdimi(
            metin = "tutorial8_6",
            ses = "tutorial8_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            kuralTablosuGorunurlugu = YOK,
        ),
        // 8 · satır 8493
        OgreticiAdimi(
            metin = "tutorial8_7",
            ses = "tutorial8_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 9 · satır 8507
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial8_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 10 · satır 8517
        OgreticiAdimi(
            metin = "tutorial3_14",
            ses = "tutorial8_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 11 · satır 8526
        OgreticiAdimi(
            metin = "tutorial3_14",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 12 · satır 8537
        OgreticiAdimi(
            metin = "tutorial5_22",
            ses = "tutorial8_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 13 · satır 8546
        OgreticiAdimi(
            metin = "tutorial8_11",
            ses = "tutorial8_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            kimlik = "t8_soru_kurallari_uygulayamadigimiz",
            ipucu = "tutorial_ipucu_064",
            kuralTablosuGorunurlugu = YOK,
            secenekler = listOf("tutorial_sik_011", "tutorial_sik_013", "tutorial_sik_012"),
            dogruSecenekler = listOf(0, 1),
            cokluSecim = true,
            secenekBasligi = "tutorial_soru_004",
        ),
        // 14 · satır 8560
        OgreticiAdimi(
            metin = "tutorial8_12",
            ses = "tutorial8_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "6 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 15 · satır 8572
        OgreticiAdimi(
            metin = "tutorial8_13",
            ses = "tutorial8_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "6 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 16 · satır 8583
        OgreticiAdimi(
            metin = "tutorial8_14",
            ses = "tutorial8_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            kuralTablosuGorunurlugu = YOK,
        ),
        // 17 · satır 8595
        OgreticiAdimi(
            metin = "tutorial8_16",
            ses = "tutorial8_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 18 · satır 8609
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 19 · satır 8620
        OgreticiAdimi(
            metin = "tutorial3_14",
            ses = "tutorial3_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 20 · satır 8634
        OgreticiAdimi(
            metin = "tutorial8_17",
            ses = "tutorial8_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "8 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 21 · satır 8650
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "8 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 22 · satır 8661
        OgreticiAdimi(
            metin = "tutorial8_1000",
            ses = "tutorial8_1000",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "8 + 6",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 23 · satır 8669
        OgreticiAdimi(
            metin = "tutorial8_18",
            ses = "tutorial8_18",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "4 + 7",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 24 · satır 8682
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "4 + 7",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 25 · satır 8693
        OgreticiAdimi(
            metin = "tutorial3_22",
            ses = "tutorial3_22",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            soru = "4 + 7",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 26 · satır 8705
        OgreticiAdimi(
            metin = "tutorial8_19",
            ses = "tutorial8_19",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "5 + 7",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 27 · satır 8719
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "5 + 7",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 28 · satır 8730
        OgreticiAdimi(
            metin = "tutorial8_1001",
            ses = "tutorial8_1001",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "5 + 7",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 29 · satır 8739
        OgreticiAdimi(
            metin = "tutorial8_1010",
            ses = "tutorial8_1010",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t8_soru_islemde_neden_kuralini",
            soru = "5 + 7",
            soruGorunurlugu = GORUNUR,
            ipucu = "tutorial_ipucu_065",
            kuralTablosuGorunurlugu = YOK,
            secenekler = listOf("tutorial_sik_019", "tutorial_sik_020", "tutorial_sik_021"),
            dogruSecenekler = listOf(0),
            cokluSecim = false,
            secenekBasligi = "tutorial8_1010",
        ),
        // 30 · satır 8753
        OgreticiAdimi(
            metin = "tutorial8_100",
            ses = "tutorial8_100",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "2 + 8",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 31 · satır 8766
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "2 + 8",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 32 · satır 8777
        OgreticiAdimi(
            metin = "tutorial3_30",
            ses = "tutorial3_30",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
            ),
            soru = "2 + 8",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 33 · satır 8788
        OgreticiAdimi(
            metin = "tutorial8_20",
            ses = "tutorial8_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
            ),
            soru = "6 + 8",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 34 · satır 8800
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "6 + 8",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 35 · satır 8811
        OgreticiAdimi(
            metin = "tutorial8_1002",
            ses = "tutorial8_1002",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "6 + 8",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 36 · satır 8819
        OgreticiAdimi(
            metin = "tutorial8_21",
            ses = "tutorial8_21",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "4 + 9",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 37 · satır 8833
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "4 + 9",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 38 · satır 8844
        OgreticiAdimi(
            metin = "tutorial3_37",
            ses = "tutorial3_37",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            soru = "4 + 9",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 39 · satır 8854
        OgreticiAdimi(
            metin = "tutorial8_22",
            ses = "tutorial8_22",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "5 + 9",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 40 · satır 8868
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial5_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "5 + 9",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 41 · satır 8879
        OgreticiAdimi(
            metin = "tutorial8_1003",
            ses = "tutorial8_1003",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "5 + 9",
            soruGorunurlugu = GORUNUR,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 42 · satır 8887
        OgreticiAdimi(
            metin = "tutorial8_23",
            ses = "tutorial8_23",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            kuralTablosuGorunurlugu = YOK,
        ),
        // 43 · satır 8897
        OgreticiAdimi(
            metin = "tutorial8_24",
            ses = "tutorial8_24",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 44 · satır 8904
        OgreticiAdimi(
            metin = "tutorial8_25",
            ses = "tutorial8_25",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 45 · satır 8910
        OgreticiAdimi(
            metin = "tutorial8_26",
            ses = "tutorial8_26",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 46 · satır 8916
        OgreticiAdimi(
            metin = "tutorial8_27",
            ses = "tutorial8_27",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 47 · satır 8921
        OgreticiAdimi(
            metin = "tutorial8_28",
            ses = "tutorial8_28",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 48 · satır 8927
        OgreticiAdimi(
            metin = "tutorial8_29",
            ses = "tutorial8_29",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "6 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 49 · satır 8938
        OgreticiAdimi(
            metin = "tutorial8_30",
            ses = "tutorial8_30",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "6 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 50 · satır 8946
        OgreticiAdimi(
            metin = "tutorial8_30",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
            ),
            soru = "6 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 51 · satır 8953
        OgreticiAdimi(
            metin = "tutorial8_31",
            ses = "tutorial8_31",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "6 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 52 · satır 8962
        OgreticiAdimi(
            metin = "tutorial8_31",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "6 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 53 · satır 8969
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial8_32",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "6 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 54 · satır 8977
        OgreticiAdimi(
            metin = "tutorial5_20",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "6 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 55 · satır 8984
        OgreticiAdimi(
            metin = "tutorial5_31",
            ses = "tutorial8_33",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "6 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 56 · satır 8992
        OgreticiAdimi(
            metin = "tutorial5_28",
            ses = "tutorial8_34",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "5 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 57 · satır 9004
        OgreticiAdimi(
            metin = "tutorial8_35",
            ses = "tutorial8_35",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "5 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 58 · satır 9011
        OgreticiAdimi(
            metin = "tutorial8_35",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
            ),
            soru = "5 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 59 · satır 9018
        OgreticiAdimi(
            metin = "tutorial8_31",
            ses = "tutorial8_36",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "5 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 60 · satır 9025
        OgreticiAdimi(
            metin = "tutorial8_31",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "5 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 61 · satır 9031
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial8_37",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "5 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 62 · satır 9038
        OgreticiAdimi(
            metin = "tutorial5_20",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "5 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 63 · satır 9044
        OgreticiAdimi(
            metin = "tutorial5_31",
            ses = "tutorial8_38",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "5 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 64 · satır 9051
        OgreticiAdimi(
            metin = "tutorial5_28",
            ses = "tutorial8_39",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "6 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 65 · satır 9062
        OgreticiAdimi(
            metin = "tutorial8_40",
            ses = "tutorial8_40",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "6 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 66 · satır 9070
        OgreticiAdimi(
            metin = "tutorial8_40",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "6 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 67 · satır 9079
        OgreticiAdimi(
            metin = "tutorial8_31",
            ses = "tutorial8_41",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "6 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 68 · satır 9087
        OgreticiAdimi(
            metin = "tutorial8_31",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "6 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 69 · satır 9094
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial8_42",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "6 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 70 · satır 9102
        OgreticiAdimi(
            metin = "tutorial5_20",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "6 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 71 · satır 9109
        OgreticiAdimi(
            metin = "tutorial8_43",
            ses = "tutorial8_43",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "6 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 72 · satır 9117
        OgreticiAdimi(
            metin = "tutorial5_28",
            ses = "tutorial8_44",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "5 + 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 73 · satır 9132
        OgreticiAdimi(
            metin = "tutorial8_45",
            ses = "tutorial8_45",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "5 + 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 74 · satır 9140
        OgreticiAdimi(
            metin = "tutorial8_45",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "5 + 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 75 · satır 9150
        OgreticiAdimi(
            metin = "tutorial8_31",
            ses = "tutorial8_41",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "5 + 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 76 · satır 9158
        OgreticiAdimi(
            metin = "tutorial8_31",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "5 + 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 77 · satır 9165
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial8_46",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "5 + 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 78 · satır 9173
        OgreticiAdimi(
            metin = "tutorial5_20",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "5 + 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 79 · satır 9180
        OgreticiAdimi(
            metin = "tutorial8_43",
            ses = "tutorial8_47",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "5 + 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 80 · satır 9188
        OgreticiAdimi(
            metin = "tutorial8_48",
            ses = "tutorial8_48",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = YOK,
        ),
    ),
)

// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 7
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps7 (satır 7360-7723).
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

internal fun ogretici7(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 7,
    kuralTablosu = "tenRuleTableLinearLayout",
    adimlar = listOf(
        // 1 · satır 7362
        OgreticiAdimi(
            metin = "tutorial7_1",
            ses = "tutorial7_1",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = GORUNMEZ,
        ),
        // 2 · satır 7370
        OgreticiAdimi(
            metin = "tutorial7_2",
            ses = "tutorial7_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            kuralTablosuGorunurlugu = YOK,
        ),
        // 3 · satır 7378
        OgreticiAdimi(
            metin = "tutorial7_3",
            ses = "tutorial7_3",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 4 · satır 7384
        OgreticiAdimi(
            metin = "tutorial7_4",
            ses = "tutorial7_4",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 5 · satır 7391
        OgreticiAdimi(
            metin = "tutorial7_5",
            ses = "tutorial7_5",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 6 · satır 7398
        OgreticiAdimi(
            metin = "tutorial7_6",
            ses = "tutorial7_6",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 7 · satır 7404
        OgreticiAdimi(
            metin = "tutorial7_7",
            ses = "tutorial7_7",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 8 · satır 7411
        OgreticiAdimi(
            metin = "tutorial7_8",
            ses = "tutorial7_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
            ),
            soru = "1 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 9 · satır 7422
        OgreticiAdimi(
            metin = "tutorial7_8",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
            ),
            soru = "1 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 10 · satır 7431
        OgreticiAdimi(
            metin = "tutorial7_9",
            ses = "tutorial7_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "4 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 11 · satır 7442
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial7_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 12 · satır 7451
        OgreticiAdimi(
            metin = "tutorial5_20",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "4 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 13 · satır 7459
        OgreticiAdimi(
            metin = "tutorial7_11",
            ses = "tutorial7_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 14 · satır 7468
        OgreticiAdimi(
            metin = "tutorial7_11",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            soru = "4 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 15 · satır 7478
        OgreticiAdimi(
            metin = "tutorial5_35",
            ses = "tutorial7_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 16 · satır 7487
        OgreticiAdimi(
            metin = "tutorial7_13",
            ses = "tutorial7_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 17 · satır 7501
        OgreticiAdimi(
            metin = "tutorial7_14",
            ses = "tutorial7_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 18 · satır 7510
        OgreticiAdimi(
            metin = "tutorial7_15",
            ses = "tutorial7_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 19 · satır 7518
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial7_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 20 · satır 7527
        OgreticiAdimi(
            metin = "tutorial5_20",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 21 · satır 7535
        OgreticiAdimi(
            metin = "tutorial7_17",
            ses = "tutorial7_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 22 · satır 7544
        OgreticiAdimi(
            metin = "tutorial7_17",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 23 · satır 7554
        OgreticiAdimi(
            metin = "tutorial5_22",
            ses = "tutorial7_18",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 24 · satır 7563
        OgreticiAdimi(
            metin = "tutorial5_28",
            ses = "tutorial7_19",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
            ),
            soru = "3 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 25 · satır 7577
        OgreticiAdimi(
            metin = "tutorial7_20",
            ses = "tutorial7_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 26 · satır 7587
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial7_21",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 27 · satır 7596
        OgreticiAdimi(
            metin = "tutorial5_20",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "3 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 28 · satır 7604
        OgreticiAdimi(
            metin = "tutorial3_30",
            ses = "tutorial7_22",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 29 · satır 7613
        OgreticiAdimi(
            metin = "tutorial3_30",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
            ),
            soru = "3 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 30 · satır 7622
        OgreticiAdimi(
            metin = "tutorial5_35",
            ses = "tutorial7_23",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 31 · satır 7629
        OgreticiAdimi(
            metin = "tutorial7_30",
            ses = "tutorial7_30",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t7_soru_6_7_8",
            ipucu = "tutorial_ipucu_058",
            secenekler = listOf("tutorial_sik_016", "tutorial_sik_017", "tutorial_sik_018"),
            dogruSecenekler = listOf(2),
            cokluSecim = false,
            secenekBasligi = "tutorial7_30",
        ),
        // 32 · satır 7641
        OgreticiAdimi(
            metin = "tutorial7_24",
            ses = "tutorial7_24",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
            ),
            soru = "4 + 9",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 33 · satır 7653
        OgreticiAdimi(
            metin = "tutorial5_40",
            ses = "tutorial6000_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t7_4p9_ilk_sayi",
            soru = "4 + 9",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 4,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_037",
        ),
        // 34 · satır 7667
        OgreticiAdimi(
            metin = "tutorial7_26",
            ses = "tutorial7_26",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 35 · satır 7675
        OgreticiAdimi(
            metin = "tutorial7_27",
            ses = "tutorial7_27",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "4 + 9",
            soruGorunurlugu = GORUNUR,
            geriCevap = 4,
        ),
        // 36 · satır 7685
        OgreticiAdimi(
            metin = "tutorial5_20",
            ses = "tutorial7_28",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t7_4p9_10_gelir",
            soru = "4 + 9",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 14,
            geriCevap = 4,
            ipucu = "tutorial_ipucu_059",
        ),
        // 37 · satır 7700
        OgreticiAdimi(
            metin = "tutorial7_29",
            ses = "tutorial7_29",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t7_4p9_9un_kardesi_1",
            soru = "4 + 9",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 13,
            geriCevap = 14,
            ipucu = "tutorial_ipucu_039",
        ),
        // 38 · satır 7715
        OgreticiAdimi(
            metin = "tutorial15_27",
            ses = "tutorial15_27",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

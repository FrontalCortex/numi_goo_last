// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 20
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps20 (satır 15299-16027).
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

internal fun ogretici20(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 20,
    kuralTablosu = null,
    adimlar = listOf(
        // 1 · satır 15301
        OgreticiAdimi(
            metin = "tutorial20_1",
            ses = "tutorial20_1",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 2 · satır 15307
        OgreticiAdimi(
            metin = "tutorial20_2",
            ses = "tutorial20_2",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 3 · satır 15313
        OgreticiAdimi(
            metin = "tutorial20_3",
            ses = "tutorial20_3",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 4 · satır 15319
        OgreticiAdimi(
            metin = "tutorial5_3",
            ses = "tutorial5_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "5 x 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 5 · satır 15327
        OgreticiAdimi(
            metin = "tutorial20_4",
            ses = "tutorial20_4",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "5 x 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 6 · satır 15335
        OgreticiAdimi(
            metin = "tutorial20_5",
            ses = "tutorial20_5",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "5 x 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 7 · satır 15343
        OgreticiAdimi(
            metin = "tutorial20_5",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "5 x 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 8 · satır 15351
        OgreticiAdimi(
            metin = "tutorial20_6",
            ses = "tutorial20_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
        ),
        // 9 · satır 15360
        OgreticiAdimi(
            metin = "tutorial5_3",
            ses = "tutorial5_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "40 x 3",
            soruGorunurlugu = GORUNUR,
        ),
        // 10 · satır 15368
        OgreticiAdimi(
            metin = "tutorial20_8",
            ses = "tutorial20_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "40 x 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
        ),
        // 11 · satır 15380
        OgreticiAdimi(
            metin = "tutorial20_8",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
            ),
            soru = "40 x 3",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
        ),
        // 12 · satır 15393
        OgreticiAdimi(
            metin = "tutorial20_9",
            ses = "tutorial20_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
            ),
        ),
        // 13 · satır 15403
        OgreticiAdimi(
            metin = "tutorial5_3",
            ses = "tutorial5_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "30 x 60",
            soruGorunurlugu = GORUNUR,
        ),
        // 14 · satır 15411
        OgreticiAdimi(
            metin = "tutorial20_10",
            ses = "tutorial20_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "30 x 60",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
        ),
        // 15 · satır 15423
        OgreticiAdimi(
            metin = "tutorial20_11",
            ses = "tutorial20_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "30 x 60",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
        ),
        // 16 · satır 15435
        OgreticiAdimi(
            metin = "tutorial20_11",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 1),
            ),
            soru = "30 x 60",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
        ),
        // 17 · satır 15450
        OgreticiAdimi(
            metin = "tutorial10_6",
            ses = "tutorial10_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 2),
            ),
            soru = "43 x 21",
            soruGorunurlugu = GORUNUR,
        ),
        // 18 · satır 15464
        OgreticiAdimi(
            metin = "tutorial20_12",
            ses = "tutorial20_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "43 x 21",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00)),
        ),
        // 19 · satır 15475
        OgreticiAdimi(
            metin = "tutorial20_13",
            ses = "tutorial20_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "43 x 21",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_040",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 20 · satır 15490
        OgreticiAdimi(
            metin = "tutorial20_13",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
            ),
            soru = "43 x 21",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_040",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 21 · satır 15506
        OgreticiAdimi(
            metin = "tutorial20_14",
            ses = "tutorial20_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "43 x 21",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00), OgreticiRenkliHarf(0, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_049",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 22 · satır 15521
        OgreticiAdimi(
            metin = "tutorial20_14",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
            ),
            soru = "43 x 21",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00), OgreticiRenkliHarf(0, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_049",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 23 · satır 15538
        OgreticiAdimi(
            metin = "tutorial20_15",
            ses = "tutorial20_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "43 x 21",
            soruGorunurlugu = GORUNUR,
        ),
        // 24 · satır 15546
        OgreticiAdimi(
            metin = "tutorial20_16",
            ses = "tutorial20_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "43 x 21",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFFFFFF00), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_050",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 25 · satır 15561
        OgreticiAdimi(
            metin = "tutorial20_16",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
            ),
            soru = "43 x 21",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFFFFFF00), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_050",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 26 · satır 15579
        OgreticiAdimi(
            metin = "tutorial20_17",
            ses = "tutorial20_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "43 x 21",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFFFFFF00), OgreticiRenkliHarf(0, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_051",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 27 · satır 15594
        OgreticiAdimi(
            metin = "tutorial20_17",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 1),
            ),
            soru = "43 x 21",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFFFFFF00), OgreticiRenkliHarf(0, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_051",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 28 · satır 15611
        OgreticiAdimi(
            metin = "tutorial20_18",
            ses = "tutorial20_18",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "43 x 21",
            soruGorunurlugu = GORUNUR,
        ),
        // 29 · satır 15619
        OgreticiAdimi(
            metin = "tutorial10_6",
            ses = "tutorial10_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
            ),
            soru = "78 x 43",
            soruGorunurlugu = GORUNUR,
        ),
        // 30 · satır 15637
        OgreticiAdimi(
            metin = "tutorial20_12",
            ses = "tutorial20_19",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "78 x 43",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00)),
        ),
        // 31 · satır 15648
        OgreticiAdimi(
            metin = "tutorial20_20",
            ses = "tutorial20_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "78 x 43",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_052",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 32 · satır 15663
        OgreticiAdimi(
            metin = "tutorial20_20",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "78 x 43",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_052",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 33 · satır 15682
        OgreticiAdimi(
            metin = "tutorial20_21",
            ses = "tutorial20_21",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "78 x 43",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00), OgreticiRenkliHarf(0, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_053",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 34 · satır 15697
        OgreticiAdimi(
            metin = "tutorial20_21",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 1),
            ),
            soru = "78 x 43",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00), OgreticiRenkliHarf(0, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_053",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 35 · satır 15713
        OgreticiAdimi(
            metin = "tutorial20_22",
            ses = "tutorial20_22",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "78 x 43",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFFFFFF00), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_054",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 36 · satır 15728
        OgreticiAdimi(
            metin = "tutorial20_22",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
            ),
            soru = "78 x 43",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFFFFFF00), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_054",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 37 · satır 15748
        OgreticiAdimi(
            metin = "tutorial20_23",
            ses = "tutorial20_23",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "78 x 43",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFFFFFF00), OgreticiRenkliHarf(0, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_055",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 38 · satır 15763
        OgreticiAdimi(
            metin = "tutorial20_23",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 2, ust = true, sira = 0, tur = 4),
            ),
            soru = "78 x 43",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFFFFFF00), OgreticiRenkliHarf(0, 0xFF00BFFF)),
            kuralMetni = "tutorial_kural_055",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 39 · satır 15783
        OgreticiAdimi(
            metin = "tutorial20_100",
            ses = "tutorial20_100",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "78 x 43",
            soruGorunurlugu = GORUNUR,
        ),
        // 40 · satır 15791
        OgreticiAdimi(
            metin = "tutorial7_24",
            ses = "tutorial7_24",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 1, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 2, ust = false, sira = 2, tur = 2),
            ),
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
        ),
        // 41 · satır 15811
        OgreticiAdimi(
            metin = "tutorial20_24",
            ses = "tutorial20_24",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00)),
        ),
        // 42 · satır 15822
        OgreticiAdimi(
            metin = "tutorial20_30",
            ses = "tutorial20_30",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00), OgreticiRenkliHarf(1, 0xFF00BFFF)),
        ),
        // 43 · satır 15833
        OgreticiAdimi(
            metin = "tutorial20_31",
            ses = "tutorial20_31",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t20_soru_hangi_basamaga_ekleyeceksin",
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFFFFFF00)),
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_112",
            secenekler = listOf("tutorial_sik_034", "tutorial_sik_035", "tutorial_sik_036"),
            dogruSecenekler = listOf(0),
            cokluSecim = false,
            secenekBasligi = "tutorial20_31",
        ),
        // 44 · satır 15852
        OgreticiAdimi(
            metin = "tutorial20_25",
            ses = "tutorial20_25",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t20_69x54_4_9u_carpip",
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 36,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_113",
        ),
        // 45 · satır 15869
        OgreticiAdimi(
            metin = "tutorial20_32",
            ses = "tutorial20_32",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00), OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 46 · satır 15880
        OgreticiAdimi(
            metin = "tutorial20_31",
            ses = "tutorial20_31",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t20_soru_hangi_basamaga_ekleyeceksin_2",
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFFFFFF00)),
            geriCevap = 36,
            ipucu = "tutorial_ipucu_114",
            secenekler = listOf("tutorial_sik_034", "tutorial_sik_035", "tutorial_sik_036"),
            dogruSecenekler = listOf(1),
            cokluSecim = false,
            secenekBasligi = "tutorial20_31",
        ),
        // 47 · satır 15899
        OgreticiAdimi(
            metin = "tutorial20_33",
            ses = "tutorial20_33",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t20_69x54_4_6yi_carpip",
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFFFFFF00), OgreticiRenkliHarf(0, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 276,
            geriCevap = 36,
            ipucu = "tutorial_ipucu_115",
        ),
        // 48 · satır 15916
        OgreticiAdimi(
            metin = "tutorial20_34",
            ses = "tutorial20_34",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFFFFFF00), OgreticiRenkliHarf(1, 0xFF00BFFF)),
        ),
        // 49 · satır 15927
        OgreticiAdimi(
            metin = "tutorial20_31",
            ses = "tutorial20_31",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t20_soru_hangi_basamaga_ekleyeceksin_3",
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            geriCevap = 276,
            ipucu = "tutorial_ipucu_114",
            secenekler = listOf("tutorial_sik_034", "tutorial_sik_035", "tutorial_sik_036"),
            dogruSecenekler = listOf(1),
            cokluSecim = false,
            secenekBasligi = "tutorial20_31",
        ),
        // 50 · satır 15946
        OgreticiAdimi(
            metin = "tutorial20_35",
            ses = "tutorial20_35",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t20_69x54_5_9u_carpip",
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFFFFFF00), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 726,
            geriCevap = 276,
            ipucu = "tutorial_ipucu_116",
        ),
        // 51 · satır 15963
        OgreticiAdimi(
            metin = "tutorial20_36",
            ses = "tutorial20_36",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFFFFFF00), OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 52 · satır 15974
        OgreticiAdimi(
            metin = "tutorial20_31",
            ses = "tutorial20_31",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t20_soru_hangi_basamaga_ekleyeceksin_4",
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(5, 0xFFFFFF00)),
            geriCevap = 726,
            ipucu = "tutorial_ipucu_117",
            secenekler = listOf("tutorial_sik_034", "tutorial_sik_035", "tutorial_sik_036"),
            dogruSecenekler = listOf(2),
            cokluSecim = false,
            secenekBasligi = "tutorial20_31",
        ),
        // 53 · satır 15993
        OgreticiAdimi(
            metin = "tutorial20_37",
            ses = "tutorial20_37",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t20_69x54_5_6yi_carpip",
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFFFFFF00), OgreticiRenkliHarf(0, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 3726,
            geriCevap = 726,
            ipucu = "tutorial_ipucu_118",
        ),
        // 54 · satır 16012
        OgreticiAdimi(
            metin = "tutorial20_29",
            ses = "tutorial20_29",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "69 x 54",
            soruGorunurlugu = GORUNUR,
        ),
        // 55 · satır 16020
        OgreticiAdimi(
            metin = "tutorial15_27",
            ses = "tutorial15_27",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

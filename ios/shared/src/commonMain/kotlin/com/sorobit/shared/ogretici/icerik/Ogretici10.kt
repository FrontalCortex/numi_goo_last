// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 10
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps10 (satır 9625-9883).
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

internal fun ogretici10(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 10,
    kuralTablosu = null,
    adimlar = listOf(
        // 1 · satır 9627
        OgreticiAdimi(
            metin = "tutorial10_1",
            ses = "tutorial10_1",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 2 · satır 9633
        OgreticiAdimi(
            metin = "tutorial2_17",
            ses = "tutorial10_2",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "8 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 3 · satır 9641
        OgreticiAdimi(
            metin = "tutorial10_3",
            ses = "tutorial10_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "8 - 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 4 · satır 9652
        OgreticiAdimi(
            metin = "tutorial10_3",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "8 - 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 5 · satır 9665
        OgreticiAdimi(
            metin = "tutorial10_4",
            ses = "tutorial10_4",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "8 - 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 6 · satır 9677
        OgreticiAdimi(
            metin = "tutorial10_4",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "8 - 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 7 · satır 9688
        OgreticiAdimi(
            metin = "tutorial10_5",
            ses = "tutorial10_5",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "8 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 8 · satır 9696
        OgreticiAdimi(
            metin = "tutorial10_6",
            ses = "tutorial10_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
            ),
            soru = "78 - 63",
            soruGorunurlugu = GORUNUR,
        ),
        // 9 · satır 9707
        OgreticiAdimi(
            metin = "tutorial10_7",
            ses = "tutorial10_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
            ),
            soru = "78 - 63",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(1, 0xFF00BFFF), OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 10 · satır 9727
        OgreticiAdimi(
            metin = "tutorial10_8",
            ses = "tutorial10_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "78 - 63",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 11 · satır 9739
        OgreticiAdimi(
            metin = "tutorial10_9",
            ses = "tutorial10_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "78 - 63",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
        ),
        // 12 · satır 9750
        OgreticiAdimi(
            metin = "tutorial10_9",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
            ),
            soru = "78 - 63",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
        ),
        // 13 · satır 9761
        OgreticiAdimi(
            metin = "tutorial10_10",
            ses = "tutorial10_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "78 - 63",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 14 · satır 9772
        OgreticiAdimi(
            metin = "tutorial10_10",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
            ),
            soru = "78 - 63",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 15 · satır 9784
        OgreticiAdimi(
            metin = "tutorial10_11",
            ses = "tutorial10_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "78 - 63",
            soruGorunurlugu = GORUNUR,
        ),
        // 16 · satır 9792
        OgreticiAdimi(
            metin = "tutorial7_24",
            ses = "tutorial10_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "94 - 52",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 17 · satır 9804
        OgreticiAdimi(
            metin = "tutorial10_13",
            ses = "tutorial10_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t10_94m52_94u",
            soru = "94 - 52",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 94,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_071",
        ),
        // 18 · satır 9821
        OgreticiAdimi(
            metin = "tutorial10_14",
            ses = "tutorial10_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "94 - 52",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF), OgreticiRenkliHarf(6, 0xFF00BFFF)),
            geriCevap = 94,
        ),
        // 19 · satır 9834
        OgreticiAdimi(
            metin = "tutorial10_15",
            ses = "tutorial10_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t10_94m52_50",
            soru = "94 - 52",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 44,
            geriCevap = 94,
            ipucu = "tutorial_ipucu_072",
        ),
        // 20 · satır 9851
        OgreticiAdimi(
            metin = "tutorial10_16",
            ses = "tutorial10_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t10_94m52_2_cikarip_bitir",
            soru = "94 - 52",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 42,
            geriCevap = 44,
            ipucu = "tutorial_ipucu_073",
        ),
        // 21 · satır 9868
        OgreticiAdimi(
            metin = "tutorial10_17",
            ses = "tutorial10_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "94 - 52",
            soruGorunurlugu = GORUNUR,
        ),
        // 22 · satır 9876
        OgreticiAdimi(
            metin = "tutorial15_27",
            ses = "tutorial15_27",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

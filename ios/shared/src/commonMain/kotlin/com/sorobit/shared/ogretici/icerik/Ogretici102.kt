// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 102
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps102 (satır 3335-3629).
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

internal fun ogretici102(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 102,
    kuralTablosu = null,
    adimlar = listOf(
        // 1 · satır 3337
        OgreticiAdimi(
            metin = "tutorial2_11",
            ses = "tutorial2_11",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 2 · satır 3343
        OgreticiAdimi(
            metin = "tutorial2_12",
            ses = "tutorial2_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "21 + 13",
            soruGorunurlugu = GORUNUR,
        ),
        // 3 · satır 3351
        OgreticiAdimi(
            metin = "tutorial2_13",
            ses = "tutorial2_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "21 + 13",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF)),
        ),
        // 4 · satır 3360
        OgreticiAdimi(
            metin = "tutorial2_13",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
            ),
            soru = "21 + 13",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF)),
        ),
        // 5 · satır 3372
        OgreticiAdimi(
            metin = "tutorial2_14",
            ses = "tutorial2_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "21 + 13",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
        ),
        // 6 · satır 3381
        OgreticiAdimi(
            metin = "tutorial2_14",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 1),
            ),
            soru = "21 + 13",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
        ),
        // 7 · satır 3391
        OgreticiAdimi(
            metin = "tutorial2_14",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
            ),
            soru = "21 + 13",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 8 · satır 3403
        OgreticiAdimi(
            metin = "tutorial2_15",
            ses = "tutorial2_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "21 + 13",
            soruGorunurlugu = GORUNUR,
        ),
        // 9 · satır 3411
        OgreticiAdimi(
            metin = "tutorial1_108",
            ses = "tutorial2_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
        ),
        // 10 · satır 3426
        OgreticiAdimi(
            metin = "tutorial2_17",
            ses = "tutorial2_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "16 + 21",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 11 · satır 3435
        OgreticiAdimi(
            metin = "tutorial2_18",
            ses = "tutorial2_18",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t102_16p21_16_ete",
            soru = "16 + 21",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 16,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_024",
        ),
        // 12 · satır 3450
        OgreticiAdimi(
            metin = "tutorial2_19",
            ses = "tutorial2_19",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t102_16p21_21i_eklemek_20yi",
            soru = "16 + 21",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 36,
            geriCevap = 16,
            ipucu = "tutorial_ipucu_025",
        ),
        // 13 · satır 3465
        OgreticiAdimi(
            metin = "tutorial2_20",
            ses = "tutorial2_20",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t102_16p21_1i_birler",
            soru = "16 + 21",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 37,
            geriCevap = 36,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_026",
        ),
        // 14 · satır 3481
        OgreticiAdimi(
            metin = "tutorial10_6",
            ses = "tutorial10_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "36 + 63",
            soruGorunurlugu = GORUNUR,
        ),
        // 15 · satır 3489
        OgreticiAdimi(
            metin = "tutorial102_0",
            ses = "tutorial102_0",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "36 + 63",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF)),
        ),
        // 16 · satır 3498
        OgreticiAdimi(
            metin = "tutorial102_0",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "36 + 63",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF)),
        ),
        // 17 · satır 3512
        OgreticiAdimi(
            metin = "tutorial102_1",
            ses = "tutorial102_1",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "36 + 63",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
        ),
        // 18 · satır 3521
        OgreticiAdimi(
            metin = "tutorial102_1",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 3),
            ),
            soru = "36 + 63",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
        ),
        // 19 · satır 3532
        OgreticiAdimi(
            metin = "tutorial102_1",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
            ),
            soru = "36 + 63",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
        ),
        // 20 · satır 3544
        OgreticiAdimi(
            metin = "tutorial102_2",
            ses = "tutorial102_2",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 21 · satır 3550
        OgreticiAdimi(
            metin = "tutorial1_108",
            ses = "tutorial1_108",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
        ),
        // 22 · satır 3568
        OgreticiAdimi(
            metin = "tutorial2_17",
            ses = "tutorial2_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "23 + 71",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 23 · satır 3577
        OgreticiAdimi(
            metin = "tutorial102_3",
            ses = "tutorial102_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t102_23p71_ilk_sayi",
            soru = "23 + 71",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 23,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_027",
        ),
        // 24 · satır 3592
        OgreticiAdimi(
            metin = "tutorial102_4",
            ses = "tutorial102_4",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t102_23p71_71i_eklemek_70i",
            soru = "23 + 71",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 93,
            geriCevap = 23,
            ipucu = "tutorial_ipucu_028",
        ),
        // 25 · satır 3607
        OgreticiAdimi(
            metin = "tutorial102_5",
            ses = "tutorial102_5",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t102_23p71_1i_birler",
            soru = "23 + 71",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 94,
            geriCevap = 93,
            ipucu = "tutorial_ipucu_026",
        ),
        // 26 · satır 3622
        OgreticiAdimi(
            metin = "tutorial15_27",
            ses = "tutorial15_27",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

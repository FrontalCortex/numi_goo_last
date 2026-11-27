// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 2
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps2 (satır 3134-3332).
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

internal fun ogretici2(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 2,
    kuralTablosu = null,
    adimlar = listOf(
        // 1 · satır 3136
        OgreticiAdimi(
            metin = "tutorial2_1",
            ses = "tutorial2_1",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 2 · satır 3142
        OgreticiAdimi(
            metin = "tutorial2_3",
            ses = "tutorial2_3",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 3 · satır 3150
        OgreticiAdimi(
            metin = "tutorial2_4",
            ses = "tutorial2_4",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
            ),
            soru = "3 + 5",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 4 · satır 3164
        OgreticiAdimi(
            metin = "tutorial2_5",
            ses = "tutorial2_5",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "3 + 5",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 5 · satır 3176
        OgreticiAdimi(
            metin = "tutorial2_6",
            ses = "tutorial2_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "3 + 5",
            soruGorunurlugu = GORUNUR,
        ),
        // 6 · satır 3184
        OgreticiAdimi(
            metin = "tutorial12_9",
            ses = "tutorial12_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
            ),
            soru = "1 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 7 · satır 3198
        OgreticiAdimi(
            metin = "tutorial2_101",
            ses = "tutorial2_101",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
            ),
            soru = "1 + 7",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
        ),
        // 8 · satır 3210
        OgreticiAdimi(
            metin = "tutorial2_102",
            ses = "tutorial2_102",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "1 + 7",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 9 · satır 3219
        OgreticiAdimi(
            metin = "tutorial2_102",
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
            ),
            soru = "1 + 7",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
        ),
        // 10 · satır 3231
        OgreticiAdimi(
            metin = "tutorial2_6",
            ses = "tutorial2_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "1 + 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 11 · satır 3239
        OgreticiAdimi(
            metin = "tutorial2_7",
            ses = "tutorial2_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
            ),
            soru = "2 + 6",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 12 · satır 3254
        OgreticiAdimi(
            metin = "tutorial2_8",
            ses = "tutorial2_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t2_2p6_2_ete",
            soru = "2 + 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 2,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_020",
        ),
        // 13 · satır 3269
        OgreticiAdimi(
            metin = "tutorial2_9",
            ses = "tutorial2_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t2_2p6_6yi_ekleyip_ete",
            soru = "2 + 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 8,
            geriCevap = 2,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_021",
        ),
        // 14 · satır 3285
        OgreticiAdimi(
            metin = "tutorial2_103",
            ses = "tutorial2_103",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "1 + 8",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 15 · satır 3294
        OgreticiAdimi(
            metin = "tutorial2_104",
            ses = "tutorial2_104",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t2_1p8_1_ete",
            soru = "1 + 8",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 1,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_022",
        ),
        // 16 · satır 3309
        OgreticiAdimi(
            metin = "tutorial2_105",
            ses = "tutorial2_105",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t2_1p8_8i_ekleyip_ete",
            soru = "1 + 8",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(4, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 9,
            geriCevap = 1,
            sonrakindeSifirla = true,
            ipucu = "tutorial_ipucu_023",
        ),
        // 17 · satır 3325
        OgreticiAdimi(
            metin = "tutorial15_27",
            ses = "tutorial15_27",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js 16
// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps16 (satır 13260-13944).
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

internal fun ogretici16(): OgreticiIcerigi = OgreticiIcerigi(
    numara = 16,
    kuralTablosu = null,
    adimlar = listOf(
        // 1 · satır 13262
        OgreticiAdimi(
            metin = "tutorial16_1",
            ses = "tutorial16_1",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 2 · satır 13269
        OgreticiAdimi(
            metin = "tutorial16_2",
            ses = "tutorial16_2",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 3 · satır 13276
        OgreticiAdimi(
            metin = "tutorial10_6",
            ses = "tutorial10_6",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
            ),
            soru = "11 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 4 · satır 13288
        OgreticiAdimi(
            metin = "tutorial16_7",
            ses = "tutorial16_7",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("tenRuleExtractionTableLayout", GORUNUR),
            ),
            soru = "11 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 5 · satır 13299
        OgreticiAdimi(
            metin = "tutorial12_17",
            ses = "tutorial12_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "11 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 6 · satır 13308
        OgreticiAdimi(
            metin = "tutorial12_17",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "11 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 7 · satır 13316
        OgreticiAdimi(
            metin = "tutorial16_8",
            ses = "tutorial16_8",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "11 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 8 · satır 13325
        OgreticiAdimi(
            metin = "tutorial16_9",
            ses = "tutorial16_9",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("tenRuleExtractionTableLayout", YOK),
                OgreticiGorunurluk("fiveRuleTable", GORUNUR),
            ),
            soru = "11 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 9 · satır 13338
        OgreticiAdimi(
            metin = "tutorial16_10",
            ses = "tutorial16_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "11 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 10 · satır 13347
        OgreticiAdimi(
            metin = "tutorial_txt_007",
            ses = "tutorial3_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "11 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 11 · satır 13359
        OgreticiAdimi(
            metin = "tutorial16_11",
            ses = "tutorial16_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "11 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 12 · satır 13368
        OgreticiAdimi(
            metin = "tutorial16_11",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
            ),
            soru = "11 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 13 · satır 13376
        OgreticiAdimi(
            metin = "tutorial3_15",
            ses = "tutorial3_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "11 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 14 · satır 13385
        OgreticiAdimi(
            metin = "tutorial5_28",
            ses = "tutorial5_28",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
            ),
            gorunurlukler = listOf(
                OgreticiGorunurluk("tenRuleExtractionTableLayout", GORUNUR),
                OgreticiGorunurluk("fiveRuleTable", YOK),
            ),
            soru = "12 - 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 15 · satır 13403
        OgreticiAdimi(
            metin = "tutorial16_13",
            ses = "tutorial16_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "12 - 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 16 · satır 13412
        OgreticiAdimi(
            metin = "tutorial12_17",
            ses = "tutorial12_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "12 - 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 17 · satır 13421
        OgreticiAdimi(
            metin = "tutorial12_17",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "12 - 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 18 · satır 13429
        OgreticiAdimi(
            metin = "tutorial16_14",
            ses = "tutorial16_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "12 - 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 19 · satır 13438
        OgreticiAdimi(
            metin = "tutorial16_15",
            ses = "tutorial16_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("fiveRuleTable", GORUNUR),
                OgreticiGorunurluk("tenRuleExtractionTableLayout", YOK),
            ),
            soru = "12 - 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 20 · satır 13451
        OgreticiAdimi(
            metin = "tutorial3_13",
            ses = "tutorial3_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "12 - 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 21 · satır 13460
        OgreticiAdimi(
            metin = "tutorial3_13",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "12 - 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 22 · satır 13468
        OgreticiAdimi(
            metin = "tutorial3_30",
            ses = "tutorial3_30",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "12 - 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 23 · satır 13477
        OgreticiAdimi(
            metin = "tutorial3_30",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
            ),
            soru = "12 - 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 24 · satır 13486
        OgreticiAdimi(
            metin = "tutorial3_15",
            ses = "tutorial3_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "12 - 7",
            soruGorunurlugu = GORUNUR,
        ),
        // 25 · satır 13495
        OgreticiAdimi(
            metin = "tutorial_txt_008",
            ses = "tutorial5_28",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
            ),
            gorunurlukler = listOf(
                OgreticiGorunurluk("fiveRuleTable", YOK),
                OgreticiGorunurluk("tenRuleExtractionTableLayout", GORUNUR),
            ),
            soru = "13 - 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 26 · satır 13514
        OgreticiAdimi(
            metin = "tutorial12_17",
            ses = "tutorial12_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "13 - 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 27 · satır 13523
        OgreticiAdimi(
            metin = "tutorial12_17",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "13 - 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 28 · satır 13531
        OgreticiAdimi(
            metin = "tutorial16_16",
            ses = "tutorial16_16",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("fiveRuleTable", GORUNUR),
                OgreticiGorunurluk("tenRuleExtractionTableLayout", YOK),
            ),
            soru = "13 - 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 29 · satır 13544
        OgreticiAdimi(
            metin = "tutorial16_16",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
            ),
            soru = "13 - 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 30 · satır 13555
        OgreticiAdimi(
            metin = "tutorial3_15",
            ses = "tutorial3_15",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "13 - 8",
            soruGorunurlugu = GORUNUR,
        ),
        // 31 · satır 13564
        OgreticiAdimi(
            metin = "tutorial_txt_008",
            ses = "tutorial5_28",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
            ),
            gorunurlukler = listOf(
                OgreticiGorunurluk("fiveRuleTable", YOK),
                OgreticiGorunurluk("tenRuleExtractionTableLayout", GORUNUR),
            ),
            soru = "14 - 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 32 · satır 13585
        OgreticiAdimi(
            metin = "tutorial12_17",
            ses = "tutorial12_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "14 - 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 33 · satır 13594
        OgreticiAdimi(
            metin = "tutorial12_17",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "14 - 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 34 · satır 13601
        OgreticiAdimi(
            metin = "tutorial16_17",
            ses = "tutorial16_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("fiveRuleTable", GORUNUR),
                OgreticiGorunurluk("tenRuleExtractionTableLayout", YOK),
            ),
            soru = "14 - 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 35 · satır 13614
        OgreticiAdimi(
            metin = "tutorial16_17",
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 3, tur = 2),
            ),
            soru = "14 - 9",
            soruGorunurlugu = GORUNUR,
        ),
        // 36 · satır 13625
        OgreticiAdimi(
            metin = "tutorial15_29",
            ses = "tutorial15_29",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t16_soru_cikarmanin_10luk_cikarmadan",
            ipucu = "tutorial_ipucu_094",
            secenekler = listOf("tutorial_ipucu_094", "tutorial_sik_032", "tutorial_sik_033"),
            dogruSecenekler = listOf(0),
            cokluSecim = false,
            secenekBasligi = "tutorial15_29",
        ),
        // 37 · satır 13637
        OgreticiAdimi(
            metin = "tutorial7_24",
            ses = "tutorial14_41",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            gorunurlukler = listOf(
                OgreticiGorunurluk("fiveRuleTable", YOK),
                OgreticiGorunurluk("tenRuleExtractionTableLayout", YOK),
            ),
            soru = "23 - 6",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 38 · satır 13652
        OgreticiAdimi(
            metin = "tutorial9_1000",
            ses = "tutorial9_1000",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t16_23m6_ilk_sayi",
            soru = "23 - 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(0, 0xFF00BFFF), OgreticiRenkliHarf(1, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 23,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_095",
        ),
        // 39 · satır 13670
        OgreticiAdimi(
            metin = "tutorial1400_10",
            ses = "tutorial1400_10",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "23 - 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
        ),
        // 40 · satır 13681
        OgreticiAdimi(
            metin = "tutorial1400_11",
            ses = "tutorial1400_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("fiveRuleTable", YOK),
                OgreticiGorunurluk("tenRuleExtractionTableLayout", YOK),
            ),
            soru = "23 - 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
            geriCevap = 23,
            ipucu = "tutorial_ipucu_087",
            kuralMetni = "tutorial_kural_030",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 41 · satır 13700
        OgreticiAdimi(
            metin = "tutorial1400_12",
            ses = "tutorial1400_12",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t16_23m6_10_gider",
            soru = "23 - 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 13,
            geriCevap = 23,
            ipucu = "tutorial_ipucu_096",
            kuralMetni = "tutorial_kural_031",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 42 · satır 13718
        OgreticiAdimi(
            metin = "tutorial1400_13",
            ses = "tutorial1400_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("fiveRuleTable", GORUNUR),
                OgreticiGorunurluk("tenRuleExtractionTableLayout", YOK),
            ),
            soru = "23 - 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(6, 0xFF00BFFF)),
            kuralTablosuGorunurlugu = GORUNMEZ,
            kuralMetni = "tutorial_kural_032",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 43 · satır 13736
        OgreticiAdimi(
            metin = "tutorial1400_14",
            ses = "tutorial1400_14",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t16_23m6_5_gelir_4un",
            soru = "23 - 6",
            soruGorunurlugu = GORUNUR,
            soruRenkleri = listOf(OgreticiRenkliHarf(5, 0xFF00BFFF)),
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 17,
            geriCevap = 13,
            ipucu = "tutorial_ipucu_097",
            kuralTablosuGorunurlugu = GORUNMEZ,
            kuralMetni = "tutorial_kural_032",
            kuralMetniGorunurlugu = GORUNUR,
            kuralMetniCapasi = "guide_line_2_7",
        ),
        // 44 · satır 13756
        OgreticiAdimi(
            metin = "tutorial14_455",
            ses = "tutorial14_455",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "23 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 45 · satır 13767
        OgreticiAdimi(
            metin = "tutorial16_1000",
            ses = "tutorial16_1000",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            gorunurlukler = listOf(
                OgreticiGorunurluk("BeadRuleExtractionTable", YOK),
            ),
        ),
        // 46 · satır 13780
        OgreticiAdimi(
            metin = "tutorial16_1003",
            ses = "tutorial16_1003",
            daktilo = true,
            daktiloHiziMs = 40L,
            gorunurlukler = listOf(
                OgreticiGorunurluk("BeadRuleExtractionTable", GORUNUR),
            ),
        ),
        // 47 · satır 13788
        OgreticiAdimi(
            metin = "tutorial13_2",
            ses = "tutorial14_21",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 1),
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 1),
            ),
            soru = "13 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 48 · satır 13800
        OgreticiAdimi(
            metin = "tutorial16_1001",
            ses = "tutorial16_1001",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "13 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 49 · satır 13807
        OgreticiAdimi(
            metin = "tutorial16_11",
            ses = "tutorial16_11",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 2, tur = 2),
            ),
            soru = "13 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 50 · satır 13816
        OgreticiAdimi(
            metin = "tutorial3_13",
            ses = "tutorial3_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 3),
            ),
            soru = "13 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 51 · satır 13825
        OgreticiAdimi(
            metin = "tutorial12_17",
            ses = "tutorial12_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 3, ust = false, sira = 0, tur = 2),
            ),
            soru = "13 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 52 · satır 13834
        OgreticiAdimi(
            metin = "tutorial16_1005",
            ses = "tutorial16_1005",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "13 - 6",
            soruGorunurlugu = GORUNUR,
        ),
        // 53 · satır 13841
        OgreticiAdimi(
            metin = "tutorial5_39",
            ses = "tutorial5_39",
            daktilo = true,
            daktiloHiziMs = 40L,
            hareketler = listOf(
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 1, tur = 2),
                OgreticiBoncukHareketi(cubuk = 4, ust = true, sira = 0, tur = 4),
            ),
            soru = "33 - 8",
            soruGorunurlugu = GORUNUR,
            abakusSifirla = true,
        ),
        // 54 · satır 13854
        OgreticiAdimi(
            metin = "tutorial9_1000",
            ses = "tutorial9_1000",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t16_33m8_ilk_sayi",
            soru = "33 - 8",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 33,
            abakusSifirla = true,
            ipucu = "tutorial_ipucu_098",
        ),
        // 55 · satır 13868
        OgreticiAdimi(
            metin = "tutorial16_1002",
            ses = "tutorial16_1002",
            daktilo = true,
            daktiloHiziMs = 40L,
            soru = "33 - 8",
            soruGorunurlugu = GORUNUR,
            geriCevap = 33,
        ),
        // 56 · satır 13877
        OgreticiAdimi(
            metin = "tutorial16_1006",
            ses = "tutorial16_1006",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t16_33m8_3_gider",
            soru = "33 - 8",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 30,
            geriCevap = 33,
            ipucu = "tutorial_ipucu_041",
        ),
        // 57 · satır 13891
        OgreticiAdimi(
            metin = "tutorial3_13",
            ses = "tutorial3_13",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t16_33m8_5_gelir",
            soru = "33 - 8",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 35,
            geriCevap = 30,
            ipucu = "tutorial_ipucu_099",
        ),
        // 58 · satır 13905
        OgreticiAdimi(
            metin = "tutorial12_17",
            ses = "tutorial12_17",
            daktilo = true,
            daktiloHiziMs = 40L,
            kimlik = "t16_33m8_10_gider",
            soru = "33 - 8",
            soruGorunurlugu = GORUNUR,
            ilerlenebilir = false,
            abakusTiklanabilir = true,
            cevap = 25,
            geriCevap = 35,
            ipucu = "tutorial_ipucu_096",
        ),
        // 59 · satır 13918
        OgreticiAdimi(
            metin = "tutorial16_1004",
            ses = "tutorial16_1004",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
        // 60 · satır 13936
        OgreticiAdimi(
            metin = "tutorial15_27",
            ses = "tutorial15_27",
            daktilo = true,
            daktiloHiziMs = 40L,
        ),
    ),
)

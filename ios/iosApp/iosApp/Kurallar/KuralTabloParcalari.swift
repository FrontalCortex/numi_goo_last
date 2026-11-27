import SwiftUI

// Kural kitabı ve kural panelinin tabloları.
//
// Android: res/layout/fragment_rules.xml (kitap; bölüm başlığı yeşil) ve res/layout/view_rules_panel.xml
// (panel; başlıksız; metinler fragment_blinding_lesson_* ya da fragment_abacus_*), çarpım tabloları
// res/layout/multiplication_table_1..9.xml.
//
// Hücre, başlık, etiket ve açıklama çizimi öğretici için yazılmış ortak parçalardır (Ogretici/
// OgreticiToplamaTabloParcalari.swift ve OgreticiBoncukTabloParcalari.swift; ölçüler aynı rules_fragment_*
// dimens'idir). Burada yalnızca hangi metin anahtarının hangi hücreye gittiği ve çarpım tabloları vardır.

// MARK: - Tablo tanımı

/// Bir kural tablosunun çizilecek içeriği.
struct KuralTabloTanimi {
    /// Kitapta tablonun üstündeki yeşil bölüm başlığı (panelde yok).
    var baslik: String? = nil
    /// Başlık hücresi: "Eklenecek sayı → Kardeşi".
    var ustSol: String
    var ustSag: String
    /// "1 → 4" kartlarından oluşan satırlar (iki, üç ya da tek kartlı).
    var hucreSatirlari: [OgreticiToplamaSatir] = []
    /// "6 → [1 gelir] [5 gider] [10 gelir]" satırları (boncuk tabloları).
    var etiketSatirlari: [OgreticiBoncukTabloSatiri] = []
    var aciklama: String? = nil
}

/// Tabloyu çizer (kabuk: en az 200 pt, içeriğe göre büyür).
struct KuralTabloGorunumu: View {
    let tanim: KuralTabloTanimi

    var body: some View {
        OgreticiBoncukTabloKabugu {
            if let baslik = tanim.baslik {
                KuralTabloBasligi(metin: baslik)
            }
            OgreticiBoncukTabloBasligi(sol: tanim.ustSol, sag: tanim.ustSag)
            ForEach(tanim.hucreSatirlari) { (satir: OgreticiToplamaSatir) in
                OgreticiToplamaSatirGorunumu(satir: satir)
            }
            OgreticiBoncukTabloSatirListesi(satirlar: tanim.etiketSatirlari)
            if let aciklama = tanim.aciklama {
                OgreticiBoncukTabloAciklamasi(metin: aciklama)
            }
        }
    }
}

/// Bölüm başlığı: lesson_header_green, rules_fragment_header_text_size (16), ortalı.
struct KuralTabloBasligi: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(Tema.yazi(16))
            .foregroundColor(Color("lesson_header_green"))
            .multilineTextAlignment(.center)
            .lineLimit(1)
            .frame(maxWidth: .infinity)
    }
}

// MARK: - Veri

enum KuralTabloVerisi {
    private static let besHucreleri: [[[String]]] = [
        [["1", "4"], ["2", "3"]],
        [["3", "2"], ["4", "1"]],
    ]
    private static let onBesHucreleri: [[[String]]] = [
        [["1", "9"], ["2", "8"]],
        [["3", "7"], ["4", "6"]],
        [["5", "5"]],
    ]
    private static let onHucreleri: [[[String]]] = [
        [["6", "4"], ["7", "3"]],
        [["8", "2"], ["9", "1"]],
    ]
    private static let onCikarmaHucreleri: [[[String]]] = [
        [["1", "9"], ["2", "8"], ["3", "7"]],
        [["4", "6"], ["5", "5"], ["6", "4"]],
        [["7", "3"], ["8", "2"], ["9", "1"]],
    ]

    /// [[["1","4"],["2","3"]], ...] → kart satırları.
    private static func hucreSatirlari(_ veri: [[[String]]]) -> [OgreticiToplamaSatir] {
        var sonuc: [OgreticiToplamaSatir] = []
        for (no, satir) in veri.enumerated() {
            var kartlar: [OgreticiToplamaKart] = []
            for cift in satir {
                kartlar.append(OgreticiToplamaKart(sol: cift[0], sag: cift[1]))
            }
            sonuc.append(OgreticiToplamaSatir(id: no, kartlar: kartlar))
        }
        return sonuc
    }

    /// 6..9 satırları; her satırda `gelirDizilimi.count` etiket, anahtarlar `onEk + "text_" + N` (ilkNo'dan).
    private static func etiketSatirlari(
        onEk: String, ilkNo: Int, gelirDizilimi: [Bool]
    ) -> [OgreticiBoncukTabloSatiri] {
        let sayilar: [String] = ["6", "7", "8", "9"]
        var sonuc: [OgreticiBoncukTabloSatiri] = []
        var no: Int = ilkNo
        for (sira, sayi) in sayilar.enumerated() {
            var etiketler: [OgreticiBoncukTabloEtiketi] = []
            for (k, gelir) in gelirDizilimi.enumerated() {
                let metin: String = Metin.al(onEk + "text_" + String(no))
                etiketler.append(OgreticiBoncukTabloEtiketi(id: k, metin: metin, gelir: gelir))
                no += 1
            }
            sonuc.append(OgreticiBoncukTabloSatiri(id: sira, sayi: sayi, etiketler: etiketler))
        }
        return sonuc
    }

    // MARK: Kitap (fragment_rules.xml)

    /// Kitaptaki tablo. `etiket`: changeRulesTableText'in başlık hücresine yazdığı metin (nil: XML'deki metin).
    static func kitap(_ secim: KuralSecimi, etiket: String?) -> KuralTabloTanimi? {
        let o: String = "fragment_rules_"
        switch secim {
        case .bes:
            return KuralTabloTanimi(
                baslik: Metin.al(o + "text"),
                ustSol: etiket ?? Metin.al(o + "five_text"), ustSag: Metin.al(o + "text_2"),
                hucreSatirlari: hucreSatirlari(besHucreleri),
                aciklama: Metin.al(o + "five_rule_description_text"))
        case .onBes:
            return KuralTabloTanimi(
                baslik: Metin.al(o + "text_3"),
                ustSol: etiket ?? Metin.al(o + "ten_text"), ustSag: Metin.al(o + "text_4"),
                hucreSatirlari: hucreSatirlari(onBesHucreleri),
                aciklama: Metin.al(o + "ten_rule_five_description_text"))
        case .on:
            return KuralTabloTanimi(
                baslik: Metin.al(o + "text_5"),
                ustSol: etiket ?? Metin.al(o + "ten_text_second"), ustSag: Metin.al(o + "text_6"),
                hucreSatirlari: hucreSatirlari(onHucreleri),
                aciklama: Metin.al(o + "ten_rule_description_text"))
        case .boncuk:
            return KuralTabloTanimi(
                baslik: Metin.al(o + "text_7"),
                ustSol: etiket ?? Metin.al(o + "bead_text"), ustSag: Metin.al(o + "text_8"),
                etiketSatirlari: etiketSatirlari(onEk: o, ilkNo: 9, gelirDizilimi: [true, false, true]))
        case .cikarmaBes:
            return KuralTabloTanimi(
                baslik: Metin.al(o + "text_21"),
                ustSol: etiket ?? Metin.al(o + "extraction_five_text"), ustSag: Metin.al(o + "text_22"),
                hucreSatirlari: hucreSatirlari(besHucreleri),
                aciklama: Metin.al(o + "extraction_five_rule_description_text"))
        case .onCikarma:
            return KuralTabloTanimi(
                baslik: Metin.al(o + "text_23"),
                ustSol: Metin.al(o + "text_24"), ustSag: Metin.al(o + "text_25"),
                hucreSatirlari: hucreSatirlari(onCikarmaHucreleri),
                aciklama: Metin.al(o + "extraction_ten_rule_description_text"))
        case .boncukCikarma:
            return KuralTabloTanimi(
                baslik: Metin.al(o + "text_26"),
                ustSol: Metin.al(o + "bead_extraction_text"), ustSag: Metin.al(o + "text_27"),
                etiketSatirlari: etiketSatirlari(onEk: o, ilkNo: 28, gelirDizilimi: [false, true, false]))
        case .carpim:
            return nil
        }
    }

    // MARK: Panel (view_rules_panel.xml / fragment_abacus.xml içindeki panel)

    /// Panelde gösterilen tablo; `onEk`: "fragment_blinding_lesson_" ya da "fragment_abacus_".
    /// 5'li tablonun başlık ve açıklaması çıkarma / toplama diline göre değişir (applyFiveText).
    static func panel(_ tablo: KuralTablosu, onEk p: String, besCikarma: Bool) -> KuralTabloTanimi? {
        switch tablo {
        case .bes:
            let sol: String = besCikarma ? Metin.al("ui_cikarilacak_sayi") : Metin.al("ui_eklenecek_sayi")
            let aciklama: String = besCikarma
                ? Metin.al("ui_5_gider_kardes_gelir") : Metin.al("ui_5_gelir_kardes_gider")
            return KuralTabloTanimi(
                ustSol: sol, ustSag: Metin.al(p + "text"),
                hucreSatirlari: hucreSatirlari(besHucreleri),
                aciklama: aciklama)
        case .onBes:
            return KuralTabloTanimi(
                ustSol: Metin.al(p + "ten_text"), ustSag: Metin.al(p + "text_2"),
                hucreSatirlari: hucreSatirlari(onBesHucreleri),
                aciklama: Metin.al(p + "ten_rule_five_description_text"))
        case .on:
            return KuralTabloTanimi(
                ustSol: Metin.al(p + "text_3"), ustSag: Metin.al(p + "text_4"),
                hucreSatirlari: hucreSatirlari(onHucreleri),
                aciklama: Metin.al(p + "ten_rule_description_text"))
        case .boncuk:
            return KuralTabloTanimi(
                ustSol: Metin.al(p + "text_5"), ustSag: Metin.al(p + "text_6"),
                etiketSatirlari: etiketSatirlari(onEk: p, ilkNo: 7, gelirDizilimi: [true, false, true]))
        case .boncukCikarma:
            return KuralTabloTanimi(
                ustSol: Metin.al(p + "text_19"), ustSag: Metin.al(p + "text_20"),
                etiketSatirlari: etiketSatirlari(onEk: p, ilkNo: 21, gelirDizilimi: [false, true, false]))
        case .onCikarma:
            return KuralTabloTanimi(
                ustSol: Metin.al(p + "text_33"), ustSag: Metin.al(p + "text_34"),
                hucreSatirlari: hucreSatirlari(onCikarmaHucreleri),
                aciklama: Metin.al(p + "extraction_ten_rule_description_text"))
        case .carpim, .yok:
            return nil
        }
    }
}

// MARK: - Çarpım tabloları

/// multiplication_table_N.xml: yeşil başlık ("Çarpım Tablosu Nx") ve 9 satır ("N x 1 = N"), tablo 200 dp
/// genişliğinde (rules_fragment_table_width), hücreler ortalı, 14 yazı, iç boşluk 2.
struct KuralCarpimTablosu: View {
    let basamak: Int

    var body: some View {
        VStack(spacing: 0) {
            KuralTabloBasligi(metin: Metin.al("multiplication_table_" + String(basamak) + "_text"))
            VStack(spacing: 0) {
                ForEach(2..<11, id: \.self) { (k: Int) in
                    KuralCarpimSatiri(
                        metin: Metin.al("multiplication_table_" + String(basamak) + "_text_" + String(k)),
                        sonSatir: k == 10)
                }
            }
            .frame(width: 200)
        }
        .fixedSize()
    }
}

struct KuralCarpimSatiri: View {
    let metin: String
    /// Son satır table_cell_border, öncekiler table_cell_border6 (altta çerçevesi görünmez).
    let sonSatir: Bool

    var body: some View {
        Text(metin)
            .font(Tema.yazi(14))
            .foregroundColor(Color.white)
            .multilineTextAlignment(.center)
            .padding(2)
            .frame(maxWidth: .infinity)
            .background(KuralCarpimZemini(sonSatir: sonSatir))
    }
}

/// table_cell_border: #222 zemin, 3 siyah çerçeve, 3 köşe. table_cell_border6: aynısı ama çerçeve alttan
/// 2 dp dışarı taşar, yani alt kenarda çerçeve görünmez (sonraki satırın üst çerçevesi ayırıcıdır).
struct KuralCarpimZemini: View {
    let sonSatir: Bool

    var body: some View {
        let sekil: RoundedRectangle = RoundedRectangle(cornerRadius: 3)
        ZStack {
            sekil.fill(OgreticiBoncukTabloRenk.hucreZemin)
            if sonSatir {
                sekil.strokeBorder(Color.black, lineWidth: 3)
            } else {
                sekil.strokeBorder(Color.black, lineWidth: 3).padding(.bottom, -2)
            }
        }
        .clipped()
    }
}

// MARK: - Panel içeriği

/// Panelde gösterilen tablo (view_rules_panel.xml). Boyutu içeriğine göre, en az 200 pt genişlikte.
struct KuralPaneli: View {
    let tablo: KuralTablosu
    let carpimBasamagi: Int?
    let metinOnEki: String
    let besCikarma: Bool

    var body: some View {
        if tablo == .carpim {
            if let basamak = carpimBasamagi {
                KuralCarpimTablosu(basamak: basamak)
            }
        } else if let tanim = KuralTabloVerisi.panel(tablo, onEk: metinOnEki, besCikarma: besCikarma) {
            KuralTabloGorunumu(tanim: tanim)
        }
    }
}

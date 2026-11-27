import SwiftUI

// Paket C ve D'nin ortak tablo parçaları: başlık, "a → b" hücresi, etiketli işlem satırı, açıklama.
// Android: res/layout/fragment_tutorial.xml (BeadRuleTable, extractionFiveRuleTable,
// tenRuleExtractionTableLayout, BeadRuleExtractionTable), drawable/table_cell_border.xml
// (#222 zemin, 3 dp siyah çerçeve, 3 dp köşe), drawable/rule_step_chip.xml (#333, 6 dp köşe),
// dimens.xml (rules_fragment_*). Tablolar durağandır: Android'de hücre vurgulaması yoktur;
// yalnızca görünürlük (rulesPanelVisibility / widgetVisibilityMap) değişir.

/// dimens.xml rules_fragment_* ölçüleri (dp → pt).
enum OgreticiBoncukTabloOlcu {
    /// rules_fragment_table_width (minWidth)
    static let enAzGenislik: CGFloat = 200
    /// rules_fragment_cell_padding
    static let hucreIc: CGFloat = 2
    /// rules_fragment_cell_text_size
    static let hucreYazi: CGFloat = 14
    /// rules_fragment_description_text_size
    static let aciklamaYazi: CGFloat = 14
    /// rules_fragment_description_padding
    static let aciklamaIc: CGFloat = 6
    /// rules_fragment_description_margin_top
    static let aciklamaUst: CGFloat = 10
    /// İşlem satırı: paddingVertical 3, paddingHorizontal 8
    static let satirDikey: CGFloat = 3
    static let satirYatay: CGFloat = 8
    /// rule_step_chip: marginStart 4, paddingHorizontal 5, paddingVertical 1, köşe 6
    static let etiketBaslangic: CGFloat = 4
    static let etiketYatay: CGFloat = 5
    static let etiketDikey: CGFloat = 1
    static let etiketKose: CGFloat = 6
    /// table_cell_border: köşe 3, çerçeve 3
    static let hucreKose: CGFloat = 3
    static let hucreCerceve: CGFloat = 3
}

enum OgreticiBoncukTabloRenk {
    /// table_cell_border zemini #222
    static let hucreZemin: Color = Color(red: 0x22 / 255.0, green: 0x22 / 255.0, blue: 0x22 / 255.0)
    /// rule_step_chip zemini #333
    static let etiketZemin: Color = Color(red: 0x33 / 255.0, green: 0x33 / 255.0, blue: 0x33 / 255.0)
    /// Ok işareti #AAFFFFFF
    static let ok: Color = Color.white.opacity(Double(0xAA) / 255.0)
    /// @color/yellow
    static let sari: Color = Color("yellow")
    /// @color/lesson_header_green (gelir)
    static let gelir: Color = Color("lesson_header_green")
    /// @color/lesson_header_red (gider)
    static let gider: Color = Color("lesson_header_red")
}

/// drawable/table_cell_border: #222 zemin, içe çizilen 3 dp siyah çerçeve, 3 dp köşe.
struct OgreticiBoncukTabloZemini: View {
    var body: some View {
        let sekil: RoundedRectangle = RoundedRectangle(cornerRadius: OgreticiBoncukTabloOlcu.hucreKose)
        sekil
            .fill(OgreticiBoncukTabloRenk.hucreZemin)
            .overlay(sekil.strokeBorder(Color.black, lineWidth: OgreticiBoncukTabloOlcu.hucreCerceve))
    }
}

/// Bir "a → b" metni: a beyaz, ok soluk beyaz, b sarı.
struct OgreticiBoncukTabloOkluMetin: View {
    let sol: String
    let sag: String

    var body: some View {
        let yazi: Font = Tema.yazi(OgreticiBoncukTabloOlcu.hucreYazi)
        let ilk: Text = Text(sol).font(yazi).foregroundColor(Color.white)
        let ok: Text = Text(" → ").font(yazi).foregroundColor(OgreticiBoncukTabloRenk.ok)
        let son: Text = Text(sag).font(yazi).foregroundColor(OgreticiBoncukTabloRenk.sari)
        let birlesik: Text = ilk + ok + son
        birlesik
            .lineLimit(1)
            .fixedSize()
    }
}

/// Tablo başlığı: match_parent, ortalı, padding 2, table_cell_border zemin.
struct OgreticiBoncukTabloBasligi: View {
    let sol: String
    let sag: String

    var body: some View {
        OgreticiBoncukTabloOkluMetin(sol: sol, sag: sag)
            .padding(OgreticiBoncukTabloOlcu.hucreIc)
            .frame(maxWidth: .infinity)
            .background(OgreticiBoncukTabloZemini())
    }
}

/// Tek bir "1 → 9" hücresi (satırda eşit genişlikte).
struct OgreticiBoncukTabloHucresi: View {
    let sol: String
    let sag: String

    var body: some View {
        OgreticiBoncukTabloOkluMetin(sol: sol, sag: sag)
            .padding(OgreticiBoncukTabloOlcu.hucreIc)
            .frame(maxWidth: .infinity)
            .background(OgreticiBoncukTabloZemini())
    }
}

/// Hücre çifti verisi.
struct OgreticiBoncukTabloCifti: Identifiable {
    let id: Int
    let sol: String
    let sag: String
}

/// Hücre satırı (TableRow / yatay LinearLayout): hücreler eşit genişlikte.
struct OgreticiBoncukTabloHucreSatiri: View {
    let ciftler: [OgreticiBoncukTabloCifti]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(ciftler) { cift in
                OgreticiBoncukTabloHucresi(sol: cift.sol, sag: cift.sag)
            }
        }
    }
}

/// Çift dizisini kuran yardımcı: [["1", "9"], ...] → numaralı veri.
enum OgreticiBoncukTabloVeri {
    static func ciftler(_ degerler: [[String]]) -> [OgreticiBoncukTabloCifti] {
        var sonuc: [OgreticiBoncukTabloCifti] = []
        for (sira, d) in degerler.enumerated() {
            sonuc.append(OgreticiBoncukTabloCifti(id: sira, sol: d[0], sag: d[1]))
        }
        return sonuc
    }
}

/// İşlem etiketi ("1 gelir" yeşil, "5 gider" kırmızı).
struct OgreticiBoncukTabloEtiketi: Identifiable {
    let id: Int
    let metin: String
    let gelir: Bool
}

/// Etiketli satır verisi: sayı + etiketler.
struct OgreticiBoncukTabloSatiri: Identifiable {
    let id: Int
    let sayi: String
    let etiketler: [OgreticiBoncukTabloEtiketi]
}

enum OgreticiBoncukTabloSatirVeri {
    /// `ilkNo`'dan başlayan fragment_tutorial_text_N anahtarlarıyla, her satırda `gelirDizilimi.count` etiket.
    /// `gelirDizilimi`: etiketlerin renkleri (true = gelir/yeşil, false = gider/kırmızı).
    static func satirlar(sayilar: [String], ilkNo: Int, gelirDizilimi: [Bool]) -> [OgreticiBoncukTabloSatiri] {
        var sonuc: [OgreticiBoncukTabloSatiri] = []
        var no: Int = ilkNo
        for (sira, sayi) in sayilar.enumerated() {
            var etiketler: [OgreticiBoncukTabloEtiketi] = []
            for (k, gelir) in gelirDizilimi.enumerated() {
                let metin: String = Metin.al("fragment_tutorial_text_" + String(no))
                etiketler.append(OgreticiBoncukTabloEtiketi(id: k, metin: metin, gelir: gelir))
                no += 1
            }
            sonuc.append(OgreticiBoncukTabloSatiri(id: sira, sayi: sayi, etiketler: etiketler))
        }
        return sonuc
    }
}

struct OgreticiBoncukTabloEtiketGorunumu: View {
    let etiket: OgreticiBoncukTabloEtiketi

    var body: some View {
        let renk: Color = etiket.gelir ? OgreticiBoncukTabloRenk.gelir : OgreticiBoncukTabloRenk.gider
        let zemin: RoundedRectangle = RoundedRectangle(cornerRadius: OgreticiBoncukTabloOlcu.etiketKose)
        Text(etiket.metin)
            .font(Tema.yazi(OgreticiBoncukTabloOlcu.hucreYazi))
            .foregroundColor(renk)
            .lineLimit(1)
            .fixedSize()
            .padding(.horizontal, OgreticiBoncukTabloOlcu.etiketYatay)
            .padding(.vertical, OgreticiBoncukTabloOlcu.etiketDikey)
            .background(zemin.fill(OgreticiBoncukTabloRenk.etiketZemin))
    }
}

/// Etiketli işlem satırı: "6 → [1 gider] [5 gelir] [10 gider]".
struct OgreticiBoncukTabloIslemSatiri: View {
    let sayi: String
    let etiketler: [OgreticiBoncukTabloEtiketi]

    var body: some View {
        HStack(spacing: 0) {
            Text(sayi)
                .font(Tema.yazi(OgreticiBoncukTabloOlcu.hucreYazi))
                .foregroundColor(Color.white)
                .lineLimit(1)
                .fixedSize()
            Text(" → ")
                .font(Tema.yazi(OgreticiBoncukTabloOlcu.hucreYazi))
                .foregroundColor(OgreticiBoncukTabloRenk.ok)
                .lineLimit(1)
                .fixedSize()
            ForEach(etiketler) { etiket in
                OgreticiBoncukTabloEtiketGorunumu(etiket: etiket)
                    .padding(.leading, OgreticiBoncukTabloOlcu.etiketBaslangic)
            }
        }
        .padding(.vertical, OgreticiBoncukTabloOlcu.satirDikey)
        .padding(.horizontal, OgreticiBoncukTabloOlcu.satirYatay)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(OgreticiBoncukTabloZemini())
    }
}

/// Satır listesi (başlığın altı).
struct OgreticiBoncukTabloSatirListesi: View {
    let satirlar: [OgreticiBoncukTabloSatiri]

    var body: some View {
        ForEach(satirlar) { satir in
            OgreticiBoncukTabloIslemSatiri(sayi: satir.sayi, etiketler: satir.etiketler)
        }
    }
}

/// Açıklama kutusu (extraction*RuleDescriptionText): 14 yazı, padding 6, üst boşluk 10.
struct OgreticiBoncukTabloAciklamasi: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(Tema.yazi(OgreticiBoncukTabloOlcu.aciklamaYazi))
            .foregroundColor(Color.white)
            .multilineTextAlignment(.center)
            .fixedSize(horizontal: false, vertical: true)
            .padding(OgreticiBoncukTabloOlcu.aciklamaIc)
            .frame(maxWidth: .infinity)
            .background(OgreticiBoncukTabloZemini())
            .padding(.top, OgreticiBoncukTabloOlcu.aciklamaUst)
    }
}

/// Tablo kabuğu: wrap_content, en az 200 pt; satırlar genişliği doldurur.
struct OgreticiBoncukTabloKabugu<Icerik: View>: View {
    let icerik: Icerik

    init(@ViewBuilder icerik: () -> Icerik) {
        self.icerik = icerik()
    }

    var body: some View {
        VStack(spacing: 0) {
            icerik
        }
        .frame(minWidth: OgreticiBoncukTabloOlcu.enAzGenislik)
        .fixedSize(horizontal: true, vertical: true)
    }
}

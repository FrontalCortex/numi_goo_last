import SwiftUI

// Toplama kural tablolarının ortak parçaları (Paket B).
//
// Android: res/layout/fragment_tutorial.xml — fiveRuleTable, tenRuleTable, tenRuleTableLinearLayout;
// hücre zemini drawable/table_cell_border.xml (#222 dolgu, 3 dp siyah çerçeve, 3 dp köşe);
// dimens.xml: rules_fragment_table_width 200, rules_fragment_cell_padding 2,
// rules_fragment_cell_text_size 14, rules_fragment_description_text_size 14,
// rules_fragment_description_padding 6, rules_fragment_description_margin_top 10.

/// table_cell_border: #222 zemin, 3 siyah çerçeve (şeklin içinde), 3 köşe.
struct OgreticiToplamaCerceve: View {
    static let zemin: Color = Color(red: 0x22 / 255.0, green: 0x22 / 255.0, blue: 0x22 / 255.0)

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 3).fill(OgreticiToplamaCerceve.zemin)
            RoundedRectangle(cornerRadius: 3).strokeBorder(Color.black, lineWidth: 3)
        }
    }
}

/// "sol → sağ" biçiminde tek kart: sol beyaz, ok #AAFFFFFF, sağ sarı (yellow). Başlık satırı da aynı biçimdedir.
struct OgreticiToplamaIslemSatiri: View {
    let sol: String
    let sag: String

    private static let ok: Color = Color.white.opacity(Double(0xAA) / 255.0)

    var body: some View {
        HStack(spacing: 0) {
            Text(sol)
                .font(Tema.yazi(14))
                .foregroundColor(Color.white)
            Text(" → ")
                .font(Tema.yazi(14))
                .foregroundColor(OgreticiToplamaIslemSatiri.ok)
            Text(sag)
                .font(Tema.yazi(14))
                .foregroundColor(Color("yellow"))
        }
        .frame(maxWidth: .infinity)
        .padding(2)
        .background(OgreticiToplamaCerceve())
    }
}

/// Bir kartın sol ve sağ değeri.
struct OgreticiToplamaKart: Equatable {
    let sol: String
    let sag: String
}

/// Tablonun bir satırı: iki kart yan yana (eşit genişlik) ya da tek kart (iki kartın ortasında, yarım genişlik).
struct OgreticiToplamaSatir: Identifiable {
    let id: Int
    let kartlar: [OgreticiToplamaKart]
}

/// Bir kural tablosunun verisi (başlık + kart satırları + açıklama).
struct OgreticiToplamaTabloVerisi {
    let baslik: OgreticiToplamaKart
    let satirlar: [OgreticiToplamaSatir]
    let aciklama: String

    private static func satir(_ no: Int, _ k: [OgreticiToplamaKart]) -> OgreticiToplamaSatir {
        return OgreticiToplamaSatir(id: no, kartlar: k)
    }

    private static func kart(_ sol: String, _ sag: String) -> OgreticiToplamaKart {
        return OgreticiToplamaKart(sol: sol, sag: sag)
    }

    /// android:id → veri. Grup 1 dışındaki adlar için nil.
    static func bul(_ ad: String) -> OgreticiToplamaTabloVerisi? {
        switch ad {
        case "fiveRuleTable":
            return OgreticiToplamaTabloVerisi(
                baslik: kart(Metin.al("fragment_tutorial_five_text"), Metin.al("fragment_tutorial_text")),
                satirlar: [
                    satir(0, [kart("1", "4"), kart("2", "3")]),
                    satir(1, [kart("3", "2"), kart("4", "1")]),
                ],
                aciklama: Metin.al("fragment_tutorial_five_rule_description_text")
            )
        case "tenRuleTable":
            return OgreticiToplamaTabloVerisi(
                baslik: kart(Metin.al("fragment_tutorial_ten_text"), Metin.al("fragment_tutorial_text_3")),
                satirlar: [
                    satir(0, [kart("1", "9"), kart("2", "8")]),
                    satir(1, [kart("3", "7"), kart("4", "6")]),
                    satir(2, [kart("5", "5")]),
                ],
                aciklama: Metin.al("fragment_tutorial_ten_rule_five_description_text")
            )
        case "tenRuleTableLinearLayout":
            return OgreticiToplamaTabloVerisi(
                baslik: kart(Metin.al("fragment_tutorial_ten_text_second"), Metin.al("fragment_tutorial_text_4")),
                satirlar: [
                    satir(0, [kart("6", "4"), kart("7", "3")]),
                    satir(1, [kart("8", "2"), kart("9", "1")]),
                ],
                aciklama: Metin.al("fragment_tutorial_ten_rule_description_text")
            )
        default:
            return nil
        }
    }
}

/// Başlık + kart satırları + açıklama kutusu. Genişlik: en az 200 (rules_fragment_table_width),
/// içeriğe göre büyür (wrap_content).
struct OgreticiToplamaTablosu: View {
    let veri: OgreticiToplamaTabloVerisi

    var body: some View {
        VStack(spacing: 0) {
            OgreticiToplamaIslemSatiri(sol: veri.baslik.sol, sag: veri.baslik.sag)
            ForEach(veri.satirlar) { (satir: OgreticiToplamaSatir) in
                OgreticiToplamaSatirGorunumu(satir: satir)
            }
            Text(veri.aciklama)
                .font(Tema.yazi(14))
                .foregroundColor(Color.white)
                .multilineTextAlignment(.center)
                .padding(6)
                .frame(maxWidth: .infinity)
                .background(OgreticiToplamaCerceve())
                .padding(.top, 10)
        }
        .frame(minWidth: 200)
        .fixedSize()
    }
}

struct OgreticiToplamaSatirGorunumu: View {
    let satir: OgreticiToplamaSatir

    var body: some View {
        if satir.kartlar.count == 1 {
            OgreticiToplamaTekKartSatiri(kart: satir.kartlar[0])
        } else {
            HStack(spacing: 0) {
                ForEach(0..<satir.kartlar.count, id: \.self) { (i: Int) in
                    OgreticiToplamaIslemSatiri(sol: satir.kartlar[i].sol, sag: satir.kartlar[i].sag)
                }
            }
        }
    }
}

/// Tek kartlı satır (weightSum 2, ortalı): iki görünmez kartla satırın ölçüsü alınır, gerçek kart
/// satırın yarı genişliğinde ortalanır.
struct OgreticiToplamaTekKartSatiri: View {
    let kart: OgreticiToplamaKart

    var body: some View {
        HStack(spacing: 0) {
            OgreticiToplamaIslemSatiri(sol: kart.sol, sag: kart.sag).opacity(0)
            OgreticiToplamaIslemSatiri(sol: kart.sol, sag: kart.sag).opacity(0)
        }
        .overlay(yerlesmisKart)
    }

    private var yerlesmisKart: some View {
        GeometryReader { (g: GeometryProxy) in
            OgreticiToplamaIslemSatiri(sol: kart.sol, sag: kart.sag)
                .frame(width: g.size.width / 2, height: g.size.height)
                .position(x: g.size.width / 2, y: g.size.height / 2)
        }
    }
}

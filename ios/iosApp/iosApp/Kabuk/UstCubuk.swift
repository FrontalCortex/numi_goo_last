import SwiftUI

// Üst durum çubuğu: elmas, enerji, anahtar. Android activity_main.xml -> currencyPanel.
// Dizilim: [elmas] boşluk [enerji] boşluk [anahtar] (aradaki iki boşluk eşit ağırlıklı).
struct UstCubuk: View {
    @ObservedObject var durum: OturumDurumu
    // Android'de göstergelerin hepsi dokununca mağazayı (ShopFragment) açar.
    var magazaAc: () -> Void = {}

    var body: some View {
        HStack(spacing: 0) {
            if !durum.ogretmenMi {
                elmasGostergesi
                Spacer(minLength: 0)
                enerjiGostergesi
                Spacer(minLength: 0)
                anahtarGostergesi
            }
        }
        .frame(maxWidth: .infinity, minHeight: KabukOlculeri.elmasYukseklik)
        .padding(.vertical, KabukOlculeri.ustDikeyBosluk)
        .padding(.horizontal, KabukOlculeri.ustYatayBosluk)
        .background(Color("background_color"))
    }

    private var elmasGostergesi: some View {
        UstGosterge(
            simgeAdi: "gold_ic",
            simgeBoyutu: KabukOlculeri.elmasYukseklik,
            metin: String(durum.elmas),
            metinRengi: KabukOlculeri.elmasYaziRengi,
            aralik: 0,
            erisilebilirlikAdi: "Elmas",
            eylem: magazaAc
        )
    }

    private var enerjiGostergesi: some View {
        UstGosterge(
            simgeAdi: "heart_ic",
            simgeBoyutu: KabukOlculeri.gostergeSimge,
            metin: durum.enerjiMetni,
            metinRengi: KabukOlculeri.enerjiYaziRengi,
            aralik: KabukOlculeri.gostergeYaziBosluk,
            erisilebilirlikAdi: "Enerji",
            eylem: magazaAc
        )
    }

    private var anahtarGostergesi: some View {
        UstGosterge(
            simgeAdi: "key",
            simgeBoyutu: KabukOlculeri.gostergeSimge,
            metin: String(durum.anahtar),
            metinRengi: KabukOlculeri.anahtarYaziRengi,
            aralik: KabukOlculeri.gostergeYaziBosluk,
            erisilebilirlikAdi: "Anahtar",
            eylem: magazaAc
        )
    }
}

// Tek gösterge: simge + sayı.
private struct UstGosterge: View {
    let simgeAdi: String
    let simgeBoyutu: CGFloat
    let metin: String
    let metinRengi: Color
    let aralik: CGFloat
    let erisilebilirlikAdi: String
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            HStack(spacing: aralik) {
                Image(simgeAdi)
                    .renderingMode(.original)
                    .resizable()
                    .scaledToFit()
                    .frame(width: simgeBoyutu, height: simgeBoyutu)
                Text(metin)
                    .font(Tema.yazi(KabukOlculeri.gostergeYazi))
                    .foregroundColor(metinRengi)
                    .lineLimit(1)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(erisilebilirlikAdi + ": " + metin))
    }
}

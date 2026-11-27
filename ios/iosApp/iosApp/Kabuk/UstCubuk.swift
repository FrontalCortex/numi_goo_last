import SwiftUI

// Üst durum çubuğu: elmas, enerji, anahtar. Android activity_main.xml -> currencyPanel.
// Dizilim: [geri] [elmas] boşluk [enerji] boşluk [anahtar] (aradaki iki boşluk eşit ağırlıklı).
// Geri düğmesi (lessonPartBackButton) yalnızca ders haritası (MapFragment) açıkken görünür.
struct UstCubuk: View {
    @ObservedObject var durum: OturumDurumu
    // Android'de göstergelerin hepsi dokununca mağazayı (ShopFragment) açar.
    var magazaAc: () -> Void = {}
    // Android: lessonPartBackButton. Öğretmen hesabında göstergeler gizlenir ama bu düğme kalır.
    var geriGorunur: Bool = false
    var geri: () -> Void = {}

    var body: some View {
        HStack(spacing: 0) {
            if geriGorunur {
                geriDugmesi
            }
            if !durum.ogretmenMi {
                elmasGostergesi
                Spacer(minLength: 0)
                enerjiGostergesi
                Spacer(minLength: 0)
                anahtarGostergesi
            }
        }
        .frame(maxWidth: .infinity, minHeight: KabukOlculeri.elmasYukseklik, alignment: .leading)
        .padding(.vertical, KabukOlculeri.ustDikeyBosluk)
        .padding(.horizontal, KabukOlculeri.ustYatayBosluk)
        .background(Color("background_color"))
    }

    // 30x30 pt'lik kutunun ortasında 24 pt'lik beyaz ok (ImageButton, scaleType varsayılanı: center).
    private var geriDugmesi: some View {
        Button(action: geri) {
            Image("ic_back_arrow")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(Color.white)
                .frame(width: KabukOlculeri.geriOku, height: KabukOlculeri.geriOku)
                .frame(width: KabukOlculeri.geriDugmesi, height: KabukOlculeri.geriDugmesi)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text("Geri"))
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
                    // Çok basamaklı bakiyelerde dar ekranda (375 pt) taşmak yerine küçülsün.
                    .minimumScaleFactor(0.6)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(erisilebilirlikAdi + ": " + metin))
    }
}

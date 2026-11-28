import SwiftUI

// Abaküs özelleştirme ekranı: alt çubuğun 4. sekmesi.
// Android karşılığı: AbacusCustomizationFragment.kt + res/layout/fragment_abacus_customization.xml.
//
// Yapı (yukarıdan aşağı): üst satır (kapat, altın, anahtar) · abaküs önizlemesi · dört simgeli
// sekme şeridi ve altında sarı gösterge · seçili sekmenin içeriği (boncuk tipi, boncuk rengi,
// çerçeve tipi, çerçeve rengi). Satın alma ve yuva seçme panelleri ekranın üstünde açılır.
struct OzellestirmeEkrani: View {
    @StateObject private var model: OzellestirmeModeli
    private let magazaAc: () -> Void
    private let kapat: (() -> Void)?

    /// - Parameters:
    ///   - magazaAc: Altın ya da anahtar göstergesine dokunuldu (Android: openShopFragment).
    ///   - kapat: Sol üstteki çarpıya basıldı (Android: popBackStack). Verilmezse çarpı gizlenir
    ///     (ekran sekme olarak gösterilirken kapatılacak bir şey yoktur).
    ///   - servisler: Sahiplik, cüzdan ve yerel ayar servisleri; verilmezse sahte servisler.
    init(
        magazaAc: @escaping () -> Void = {},
        kapat: (() -> Void)? = nil,
        servisler: OzellestirmeBagimliliklar = OzellestirmeBagimliliklar.sahte
    ) {
        self.magazaAc = magazaAc
        self.kapat = kapat
        _model = StateObject(wrappedValue: OzellestirmeModeli(servisler: servisler))
    }

    var body: some View {
        VStack(spacing: 0) {
            OzellestirmeUstSatir(model: model, magazaAc: magazaAc, kapat: kapat)
            OzellestirmeOnizleme(
                yuvalar: model.yuvalar,
                olculer: model.olculer,
                cerceve: model.cerceve
            )
            .padding(12)
            .frame(maxWidth: .infinity, minHeight: 120)
            .padding(.top, 8)
            OzellestirmeSekmeSeridi(model: model)
                .padding(.top, 4)
            sekmeIcerigi
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(Color("background_color").ignoresSafeArea())
        .overlay(
            OzellestirmePanelKatmani(model: model)
                .allowsHitTesting(model.panel != nil)
        )
        .overlay(alignment: .bottom) {
            if let metin = model.bildirim {
                OzellestirmeBildirimGorunumu(metin: metin)
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: model.bildirim)
        .preferredColorScheme(.dark)
        .onAppear { model.basla() }
    }

    @ViewBuilder
    private var sekmeIcerigi: some View {
        switch model.sekme {
        case 0:
            OzellestirmeBoncukTipiSekmesi(model: model)
        case 1:
            OzellestirmeBoncukRengiSekmesi(model: model)
        case 2:
            OzellestirmeCerceveTipiSekmesi(model: model)
        default:
            OzellestirmeCerceveRengiSekmesi(model: model)
        }
    }
}

// MARK: - Üst satır

/// Solda kapatma çarpısı (40 dp, kenardan 10 dp), sağda altın ve anahtar bakiyesi.
private struct OzellestirmeUstSatir: View {
    @ObservedObject var model: OzellestirmeModeli
    let magazaAc: () -> Void
    let kapat: (() -> Void)?

    var body: some View {
        HStack(spacing: 0) {
            kapatDugmesi
            Spacer(minLength: 0)
            Button(action: magazaAc) {
                HStack(spacing: 4) {
                    OzellestirmeGorsel(ad: "gold_ic", genislik: 24, yukseklik: 24)
                    Text(String(model.altin))
                        .font(Tema.yazi(16))
                        .foregroundColor(OzellestirmeStil.altinYaziRengi)
                        .lineLimit(1)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(OzellestirmeBasmaStili())
            .padding(.trailing, 16)
            .accessibilityLabel(Text(Metin.al("ios_erisim_altin", String(model.altin))))
            Button(action: magazaAc) {
                HStack(spacing: 4) {
                    OzellestirmeGorsel(ad: "key", genislik: 22, yukseklik: 22)
                    Text(String(model.anahtar))
                        .font(Tema.yazi(15))
                        .foregroundColor(OzellestirmeStil.anahtarYaziRengi)
                        .lineLimit(1)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(OzellestirmeBasmaStili())
            .padding(.trailing, 16)
            .accessibilityLabel(Text(Metin.al("ios_erisim_anahtar", String(model.anahtar))))
        }
        .frame(height: 60)
    }

    @ViewBuilder
    private var kapatDugmesi: some View {
        if let kapat = kapat {
            Button(action: kapat) {
                OzellestirmeGorsel(ad: "cross", genislik: 24, yukseklik: 24)
                    .frame(width: 40, height: 40)
                    .contentShape(Rectangle())
            }
            .buttonStyle(OzellestirmeBasmaStili())
            .padding(.leading, 10)
            .accessibilityLabel(Text(Metin.al("ios_erisim_kapat")))
        } else {
            Color.clear
                .frame(width: 40, height: 40)
                .padding(.leading, 10)
        }
    }
}

// MARK: - Sekme şeridi

/// Dört eşit genişlikte simge (52 dp yüksek, #1A1F22 zemin); seçili olmayanlar 0.4 opaklıkta.
/// Altında seçili sekmenin genişliğinde 3 dp'lik sarı gösterge.
private struct OzellestirmeSekmeSeridi: View {
    @ObservedObject var model: OzellestirmeModeli

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                ForEach(model.sekmeSimgeleri.indices, id: \.self) { sira in
                    sekme(sira)
                }
            }
            .frame(height: 52)
            .background(OzellestirmeStil.sekmeSeridiZemini)
            gosterge
        }
    }

    private func sekme(_ sira: Int) -> some View {
        let seciliMi: Bool = sira == model.sekme
        let simgeAdi: String = model.sekmeSimgeleri[sira]
        let germe: CGFloat = OzellestirmeStil.dikeyGerme(simgeAdi)
        return Button(action: { model.sekmeSec(sira) }) {
            Image(simgeAdi)
                .renderingMode(.original)
                .resizable()
                .scaledToFit()
                .scaleEffect(x: 1.0, y: germe, anchor: UnitPoint.center)
                .padding(12)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .opacity(seciliMi ? model.seciliSekmeOpakligi : model.digerSekmeOpakligi)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(seciliMi ? AccessibilityTraits.isSelected : AccessibilityTraits())
    }

    private var gosterge: some View {
        GeometryReader { geo in
            gostergeCubugu(toplamGenislik: geo.size.width)
        }
        .frame(height: 3)
    }

    private func gostergeCubugu(toplamGenislik: CGFloat) -> some View {
        let adet: Int = max(1, model.sekmeSimgeleri.count)
        let genislik: CGFloat = toplamGenislik / CGFloat(adet)
        return OzellestirmeStil.sekmeGostergesi
            .frame(width: genislik, height: 3)
            .offset(x: genislik * CGFloat(model.sekme), y: 0)
    }
}

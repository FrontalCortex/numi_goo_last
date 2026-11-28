import SwiftUI

// Üst durum çubuğu. Android activity_main.xml -> currencyPanel.
// Dizilim: [geri] [seri] boşluk [elmas] boşluk [enerji] boşluk [anahtar]
// (aradaki boşluklar eşit ağırlıklı). Geri düğmesi (lessonPartBackButton) yalnızca ders haritası
// (MapFragment) açıkken görünür. Android'de anahtardan sonra bir boşluk ve Sobi kredisi
// (SobiHeadView + creditText) da var; Sobi'nin kafası kodla çizildiği ve kredi altyapısı iOS'ta
// olmadığı için o gösterge henüz eklenmedi.
struct UstCubuk: View {
    @ObservedObject var durum: OturumDurumu
    // Android'de elmas, enerji ve anahtar göstergeleri dokununca mağazayı (ShopFragment) açar.
    var magazaAc: () -> Void = {}
    // Android: streakContainer → openStreakFragment (seri ekranı; mağazaya gitmez).
    var seriAc: () -> Void = {}
    // Android: lessonPartBackButton. Öğretmen hesabında göstergeler gizlenir ama bu düğme kalır.
    var geriGorunur: Bool = false
    var geri: () -> Void = {}

    var body: some View {
        HStack(spacing: 0) {
            if geriGorunur {
                geriDugmesi
            }
            if !durum.ogretmenMi {
                // Öğretmende altın, anahtar, can ve seri gizli (MainActivity.applyTeacherCurrencyPanel).
                seriGostergesi
                Spacer(minLength: 0)
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
        .accessibilityLabel(Text(Metin.al("notification_download_cancel")))
    }

    // MARK: Seri (streakContainer)

    /// Seri kırıkken (0) alev gri ve soluk (alfa 0,45, #78909C), sayı da gri.
    private var seriGostergesi: some View {
        let suruyor: Bool = durum.seri > 0
        return Button(action: seriAc) {
            HStack(spacing: KabukOlculeri.gostergeYaziBosluk) {
                alev(suruyor: suruyor)
                Text(String(durum.seri))
                    .font(Tema.yazi(KabukOlculeri.gostergeYazi))
                    .foregroundColor(suruyor ? KabukOlculeri.seriYaziRengi : KabukOlculeri.seriSonukRengi)
                    .lineLimit(1)
            }
            .padding(.trailing, KabukOlculeri.seriSagBosluk)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(Metin.al("activity_main_streak_icon_desc") + ": " + String(durum.seri)))
    }

    @ViewBuilder
    private func alev(suruyor: Bool) -> some View {
        let boy: CGFloat = KabukOlculeri.gostergeSimge
        if suruyor {
            Image("streak_flame_ic")
                .renderingMode(.original)
                .resizable()
                .scaledToFit()
                .frame(width: boy, height: boy)
        } else {
            Image("streak_flame_ic")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(KabukOlculeri.seriSonukRengi)
                .frame(width: boy, height: boy)
                .opacity(0.45)
        }
    }

    // MARK: Elmas, enerji, anahtar

    /// diamondID: yükseklik 30, genişlik görselin oranında; yazıyla arasında boşluk yok.
    private var elmasGostergesi: some View {
        Button(action: magazaAc) {
            HStack(spacing: 0) {
                Image("gold_ic")
                    .renderingMode(.original)
                    .resizable()
                    .scaledToFit()
                    .frame(height: KabukOlculeri.elmasYukseklik)
                gostergeYazisi(String(durum.elmas), renk: KabukOlculeri.elmasYaziRengi)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("Altın: " + String(durum.elmas)))
    }

    /// energyIconFrame: kalp ve sonsuz enerjide üstünde "∞" (13pt, beyaz); o zaman sayı boşalır.
    private var enerjiGostergesi: some View {
        Button(action: magazaAc) {
            HStack(spacing: KabukOlculeri.gostergeYaziBosluk) {
                ZStack {
                    Image(durum.proPlan ? "heart_pro_ic" : "heart_ic")
                        .renderingMode(.original)
                        .resizable()
                        .scaledToFit()
                    if durum.sonsuzRozeti {
                        Text("∞")
                            .font(Tema.yazi(KabukOlculeri.sonsuzYazi))
                            .foregroundColor(Color.white)
                    }
                }
                .frame(width: KabukOlculeri.gostergeSimge, height: KabukOlculeri.gostergeSimge)
                gostergeYazisi(durum.enerjiMetni, renk: KabukOlculeri.enerjiYaziRengi)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("Enerji: " + (durum.sonsuzRozeti ? "∞" : durum.enerjiMetni)))
    }

    private var anahtarGostergesi: some View {
        Button(action: magazaAc) {
            HStack(spacing: KabukOlculeri.gostergeYaziBosluk) {
                Image("key")
                    .renderingMode(.original)
                    .resizable()
                    .scaledToFit()
                    .frame(width: KabukOlculeri.gostergeSimge, height: KabukOlculeri.gostergeSimge)
                gostergeYazisi(String(durum.anahtar), renk: KabukOlculeri.anahtarYaziRengi)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("Anahtar: " + String(durum.anahtar)))
    }

    private func gostergeYazisi(_ metin: String, renk: Color) -> some View {
        Text(metin)
            .font(Tema.yazi(KabukOlculeri.gostergeYazi))
            .foregroundColor(renk)
            .lineLimit(1)
            // Çok basamaklı bakiyelerde dar ekranda (375 pt) taşmak yerine küçülsün.
            .minimumScaleFactor(0.6)
    }
}

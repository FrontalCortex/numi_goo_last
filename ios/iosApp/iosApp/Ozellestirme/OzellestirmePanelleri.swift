import SwiftUI
import UIKit

// Özelleştirme ekranının üstünde açılan paneller (Android: Dialog).
//  - Satın alma paneli: res/layout/panel_bead_purchase.xml — boncuk, boncuk renk özelliği,
//    çerçeve ve çerçeve renk özelliği akışlarının dördü de bu paneli kullanır.
//  - Yuva seçme paneli: res/layout/panel_bead_skin_select.xml — abaküsteki bir boncuğa dokunarak
//    seçilen tipi o yuvaya yerleştirir; altta envanter ve "5 tane daha al" düğmesi vardır.

/// Karartılmış zemin + ortada panel. Zemine dokununca kapanır (Android: setCanceledOnTouchOutside).
struct OzellestirmePanelKatmani: View {
    @ObservedObject var model: OzellestirmeModeli

    var body: some View {
        GeometryReader { geo in
            ZStack {
                if let panel = model.panel {
                    Color.black.opacity(0.6)
                        .ignoresSafeArea()
                        .contentShape(Rectangle())
                        .onTapGesture { model.paneliKapat() }
                    icerik(panel, ekranGenisligi: geo.size.width)
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
    }

    @ViewBuilder
    private func icerik(_ panel: OzellestirmePanel, ekranGenisligi: CGFloat) -> some View {
        switch panel {
        case .satinAlma(let bilgi):
            OzellestirmeSatinAlmaGorunumu(model: model, bilgi: bilgi)
                .frame(width: ekranGenisligi * model.satinAlmaPaneliGenislikOrani)
        case .yuvaSecimi:
            if let bilgi = model.yuvaPaneli {
                OzellestirmeYuvaSecimGorunumu(model: model, bilgi: bilgi)
                    .frame(width: ekranGenisligi * model.yuvaPaneliGenislikOrani)
            }
        }
    }
}

/// Panellerin ortak zemini: bg_panel_daily_question_continue.xml (@color/panel_background, köşe 18).
private struct OzellestirmePanelZemini: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(20)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Color("panel_background"))
            )
    }
}

/// Sağ üstteki kapatma düğmesi (ic_close_black, beyaza boyanmış; 40 dp kutu, 8 dp iç boşluk).
private struct OzellestirmePanelKapatDugmesi: View {
    let etkin: Bool
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            Image("ic_close_black")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(.white)
                .frame(width: 24, height: 24)
                .frame(width: 40, height: 40)
                .contentShape(Rectangle())
        }
        .buttonStyle(OzellestirmeBasmaStili())
        .disabled(!etkin)
        .accessibilityLabel(Text(Metin.al("ios_erisim_kapat")))
    }
}

/// Fiyatı ve para birimini gösteren yeşil satın alma düğmesi (yükseklik 60, köşe 15, en az 160 genişlik).
private struct OzellestirmeSatinAlDugmesi: View {
    let fiyatMetni: String
    let simgeAdi: String
    let zemin: Color
    let etkin: Bool
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            OzellestirmeFiyat(
                metin: fiyatMetni,
                simgeAdi: simgeAdi,
                yaziBoyu: 24,
                simgeBoyu: 40,
                aralik: 5,
                renk: .black
            )
            .padding(.horizontal, 24)
            .frame(minWidth: 160)
            .frame(height: 60)
            .background(RoundedRectangle(cornerRadius: 15, style: .continuous).fill(zemin))
            .contentShape(Rectangle())
        }
        .buttonStyle(OzellestirmeBasmaStili())
        .disabled(!etkin)
    }
}

// MARK: - Satın alma paneli

private struct OzellestirmeSatinAlmaGorunumu: View {
    @ObservedObject var model: OzellestirmeModeli
    let bilgi: OzellestirmeSatinAlmaPaneli

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                Spacer(minLength: 0)
                OzellestirmePanelKapatDugmesi(etkin: !model.panelMesgul, eylem: model.paneliKapat)
            }
            gorsel
                .padding(.top, 8)
            OzellestirmeSatinAlDugmesi(
                fiyatMetni: bilgi.fiyatMetni,
                simgeAdi: bilgi.birimSimgeAdi,
                zemin: Color("button_enabled"),
                etkin: !model.panelMesgul,
                eylem: model.satinAlmayiOnayla
            )
            .padding(.top, 20)
        }
        .modifier(OzellestirmePanelZemini())
    }

    /// 120 dp'lik ürün görseli; renk özelliği akışlarında sağ altında 48 dp'lik palet simgesi.
    private var gorsel: some View {
        ZStack(alignment: .bottomTrailing) {
            OzellestirmeGorsel(ad: bilgi.gorselAdi, genislik: 120, yukseklik: 120)
            if bilgi.ozellikSimgesiGorunur {
                OzellestirmeGorsel(ad: "color_ic", genislik: 48, yukseklik: 48)
                    .padding(.trailing, 8)
                    .padding(.bottom, 8)
            }
        }
    }
}

// MARK: - Yuva seçme paneli

private struct OzellestirmeYuvaSecimGorunumu: View {
    @ObservedObject var model: OzellestirmeModeli
    let bilgi: OzellestirmeYuvaPaneli

    var body: some View {
        VStack(spacing: 0) {
            baslik
            OzellestirmeOnizleme(
                yuvalar: model.yuvalar,
                olculer: model.olculer,
                cerceve: model.cerceve,
                sabitCerceveGorseli: "abacus_frame_bg4",
                kalinBariyer: true,
                dokun: { yuva in
                    model.yuvayaYerlestir(cubuk: yuva.cubuk, ustMu: yuva.ustMu, sira: yuva.sira)
                }
            )
            .padding(.top, 16)
            HStack(spacing: 8) {
                OzellestirmeBoncukGorunumu(resim: bilgi.envanterResmi, genislik: 40, yukseklik: 40)
                Text(bilgi.envanterMetni)
                    .font(Tema.yazi(20))
                    .foregroundColor(.white)
            }
            .padding(.top, 16)
            OzellestirmeSatinAlDugmesi(
                fiyatMetni: bilgi.fiyatMetni,
                simgeAdi: bilgi.birimSimgeAdi,
                zemin: bilgi.dugmeRengi,
                etkin: !model.panelMesgul,
                eylem: model.yuvaPanelindenSatinAl
            )
            .padding(.top, 12)
            .padding(.bottom, 16)
        }
        .modifier(OzellestirmePanelZemini())
    }

    /// Başlık ortada, kapatma düğmesi sağ üstte (başlığın yanlarında 48 dp boşluk kalır).
    private var baslik: some View {
        ZStack(alignment: .topTrailing) {
            Text(model.yuvaPaneliBasligi)
                .font(Tema.yazi(15))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 48)
                .padding(.top, 8)
                .frame(maxWidth: .infinity)
                .frame(minHeight: 40, alignment: .top)
            OzellestirmePanelKapatDugmesi(etkin: !model.panelMesgul, eylem: model.paneliKapat)
        }
    }
}

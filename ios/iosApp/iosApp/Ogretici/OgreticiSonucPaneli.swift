import SwiftUI

// Doğru / yanlış panelleri (fragment_tutorial.xml: correctPanel, incorrectPanel; bottom_sheet_background:
// #222, üst köşeler 16). Panel açıkken ekranın geri kalanı dokunmaya kapalıdır
// (Android enableTutorialResultTouchBlock). Kapsam dışı: "Ne zorladı?" anketi (stuckSurveyGroup) ve
// yanlış panelindeki "Sobi'ye sor" (hesap ister).

enum OgreticiSonucOlcu {
    /// tutorial_result_icon_size
    static let simge: CGFloat = 24
    /// tutorial_result_item_margin
    static let oge: CGFloat = 10
    /// tutorial_result_title_start_margin
    static let baslikBaslangic: CGFloat = 8
    /// tutorial_result_title_text_size / _body_text_size / _button_text_size
    static let baslikYazi: CGFloat = 18
    static let govdeYazi: CGFloat = 16
    static let dugmeYazi: CGFloat = 18
    /// tutorial_panel_button_height / tutorial_result_button_corner_radius / _top_margin
    static let dugmeYukseklik: CGFloat = 50
    static let dugmeKose: CGFloat = 15
    static let dugmeUst: CGFloat = 16
    /// tutorial_result_panel_padding (doğru paneli); yanlış panelinin dolgusu 3.
    static let dogruDolgu: CGFloat = 16
    static let yanlisDolgu: CGFloat = 3
    /// tutorial_result_label_top_margin
    static let etiketUst: CGFloat = 16
    static let panelKose: CGFloat = 16
}

enum OgreticiSonucRenk {
    /// correctText #8CC63F; bg_depth_tutorial_continue yüz #8CC63F, dudak #62982A
    static let dogru = Color(red: 0x8C / 255.0, green: 0xC6 / 255.0, blue: 0x3F / 255.0)
    static let dogruGolge = Color(red: 0x62 / 255.0, green: 0x98 / 255.0, blue: 0x2A / 255.0)
    /// incorrectText #FF4B4B; bg_depth_tutorial_okay yüz #FF4B4B, dudak #D12F2F
    static let yanlis = Color(red: 0xFF / 255.0, green: 0x4B / 255.0, blue: 0x4B / 255.0)
    static let yanlisGolge = Color(red: 0xD1 / 255.0, green: 0x2F / 255.0, blue: 0x2F / 255.0)
    /// bottom_sheet_background #222
    static let zemin = Color(red: 0x22 / 255.0, green: 0x22 / 255.0, blue: 0x22 / 255.0)
}

struct OgreticiSonucKatmani: View {
    @ObservedObject var model: OgreticiGorunumModeli

    var body: some View {
        ZStack(alignment: .bottom) {
            if model.sonuc != nil {
                // Dokunma kilidi: panel dışına dokunmak bir şey yapmaz.
                Color.black.opacity(0.001)
                    .contentShape(Rectangle())
                    .onTapGesture {}
                    .ignoresSafeArea()
            }
            if model.sonuc == .dogru {
                OgreticiDogruPaneli(devam: { model.dogruDevam() })
                    .transition(.move(edge: .bottom))
            } else if model.sonuc == .yanlis {
                OgreticiYanlisPaneli(ipucu: model.adim.ipucu, tekrar: { model.tekrarDene() })
                    .transition(.move(edge: .bottom))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
    }
}

/// Alttan açılan panel zemini: üst köşeler yuvarlak, alt kenar güvenli alanın altına uzar.
struct OgreticiPanelZemini: View {
    var body: some View {
        OgreticiUstKoseliDikdortgen(yaricap: OgreticiSonucOlcu.panelKose)
            .fill(OgreticiSonucRenk.zemin)
            .ignoresSafeArea(edges: .bottom)
    }
}

/// Yalnızca üst iki köşesi yuvarlak dikdörtgen (iOS 16'da UnevenRoundedRectangle yok).
struct OgreticiUstKoseliDikdortgen: Shape {
    let yaricap: CGFloat

    func path(in rect: CGRect) -> Path {
        let r: CGFloat = min(yaricap, rect.width / 2, rect.height / 2)
        var yol = Path()
        yol.move(to: CGPoint(x: rect.minX, y: rect.maxY))
        yol.addLine(to: CGPoint(x: rect.minX, y: rect.minY + r))
        yol.addArc(
            center: CGPoint(x: rect.minX + r, y: rect.minY + r), radius: r,
            startAngle: Angle.degrees(180), endAngle: Angle.degrees(270), clockwise: false
        )
        yol.addLine(to: CGPoint(x: rect.maxX - r, y: rect.minY))
        yol.addArc(
            center: CGPoint(x: rect.maxX - r, y: rect.minY + r), radius: r,
            startAngle: Angle.degrees(270), endAngle: Angle.degrees(360), clockwise: false
        )
        yol.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        yol.closeSubpath()
        return yol
    }
}

/// correctPanel: simge + "Aferin!", altında "Devam et".
struct OgreticiDogruPaneli: View {
    let devam: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: OgreticiSonucOlcu.baslikBaslangic) {
                Image("circle_correct")
                    .resizable()
                    .scaledToFit()
                    .frame(width: OgreticiSonucOlcu.simge, height: OgreticiSonucOlcu.simge)
                Text(Metin.al("fragment_tutorial_correct_text"))
                    .font(Tema.yazi(OgreticiSonucOlcu.baslikYazi))
                    .foregroundColor(OgreticiSonucRenk.dogru)
            }
            Button(action: devam) {
                Text(Metin.al("fragment_tutorial_continue_button"))
                    .font(Tema.yazi(OgreticiSonucOlcu.dugmeYazi))
                    .foregroundColor(Color.black)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(GocenDugmeStili(zemin: OgreticiSonucRenk.dogru, golge: OgreticiSonucRenk.dogruGolge, koseYaricapi: OgreticiSonucOlcu.dugmeKose))
            .frame(height: OgreticiSonucOlcu.dugmeYukseklik)
            .padding(.top, OgreticiSonucOlcu.dugmeUst)
        }
        .padding(OgreticiSonucOlcu.dogruDolgu)
        .frame(maxWidth: .infinity)
        .background(OgreticiPanelZemini())
    }
}

/// incorrectPanel: simge + "Yanlış", "Talimata uygun işlemi yap", adımın ipucu, "Tekrar Dene".
struct OgreticiYanlisPaneli: View {
    let ipucu: String?
    let tekrar: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 0) {
                // Android: @android:drawable/ic_delete (sistem görseli, asset yok) #FF4B4B ile boyanır.
                Image(systemName: "xmark.circle.fill")
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(OgreticiSonucRenk.yanlis)
                    .frame(width: OgreticiSonucOlcu.simge, height: OgreticiSonucOlcu.simge)
                    .padding(OgreticiSonucOlcu.oge)
                    .contentShape(Rectangle())
                    .onTapGesture { tekrar() }
                Text(Metin.al("fragment_tutorial_incorrect_text"))
                    .font(Tema.yazi(OgreticiSonucOlcu.baslikYazi))
                    .foregroundColor(OgreticiSonucRenk.yanlis)
                    .padding(.vertical, OgreticiSonucOlcu.oge)
                Spacer(minLength: 0)
            }
            Text(Metin.al("fragment_tutorial_correct_answer_label"))
                .font(Tema.yazi(OgreticiSonucOlcu.govdeYazi, kalin: false))
                .foregroundColor(OgreticiSonucRenk.yanlis)
                .padding(.horizontal, OgreticiSonucOlcu.oge)
                .padding(.top, OgreticiSonucOlcu.etiketUst - OgreticiSonucOlcu.oge)
            if let metin = ipucu, !metin.isEmpty {
                Text(metin)
                    .font(Tema.yazi(OgreticiSonucOlcu.govdeYazi, kalin: false))
                    .foregroundColor(Color.white)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal, OgreticiSonucOlcu.oge)
                    .padding(.top, 8)
            }
            Button(action: tekrar) {
                Text(Metin.al("fragment_tutorial_okay_button"))
                    .font(Tema.yazi(OgreticiSonucOlcu.dugmeYazi))
                    .foregroundColor(Color.black)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(GocenDugmeStili(zemin: OgreticiSonucRenk.yanlis, golge: OgreticiSonucRenk.yanlisGolge, koseYaricapi: OgreticiSonucOlcu.dugmeKose))
            .frame(height: OgreticiSonucOlcu.dugmeYukseklik)
            .padding(.horizontal, OgreticiSonucOlcu.oge)
            .padding(.top, OgreticiSonucOlcu.dugmeUst)
        }
        .padding(OgreticiSonucOlcu.yanlisDolgu)
        .frame(maxWidth: .infinity)
        .background(OgreticiPanelZemini())
    }
}

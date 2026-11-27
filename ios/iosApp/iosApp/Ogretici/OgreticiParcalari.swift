import SwiftUI

// Öğretici ekranının parçaları: ölçüler, renkler, üst çubuk, metin, soru kutusu, alt panel.
// Android kaynakları: res/layout/fragment_tutorial.xml, values/dimens.xml (tutorial_*),
// drawable/bg_depth_tutorial_control.xml, bg_depth_tutorial_skip.xml, table_cell_border5.xml.

/// fragment_tutorial.xml / dimens.xml ölçüleri (dp → pt).
enum OgreticiOlcu {
    /// tutorial_quit_button_size
    static let cikisBoyu: CGFloat = 40
    static let cikisBaslangic: CGFloat = 15
    static let cikisUst: CGFloat = 10
    /// tutorial_text_size
    static let metinYazi: CGFloat = 20
    /// autoSizeMinTextSize 12sp / 20sp
    static let metinEnKucukOran: CGFloat = 0.6
    static let metinYatay: CGFloat = 10
    /// tutorial_question_width
    static let soruGenislik: CGFloat = 190
    /// tutorial_question_margin_bottom
    static let soruAltBosluk: CGFloat = 2
    /// table_cell_border5: 5 dp siyah çerçeve, 3 dp köşe
    static let soruCerceve: CGFloat = 5
    static let soruKose: CGFloat = 3
    /// tutorial_abacus_linear_margin_bottom
    static let abakusAltBosluk: CGFloat = 10
    /// bottomPanelID: marginHorizontal 5, marginBottom tutorial_bottom_panel_margin_bottom
    static let altPanelYatay: CGFloat = 5
    static let altPanelAlt: CGFloat = 16
    /// tutorial_back_icon_button_size, marginStart 10, padding 12 (alt 16)
    static let geriBoyu: CGFloat = 48
    static let geriBaslangic: CGFloat = 10
    /// tutorial_control_button_height / _side_margin / _corner_radius / _text_size
    static let kontrolYukseklik: CGFloat = 48
    static let kontrolYan: CGFloat = 16
    static let kontrolKose: CGFloat = 10
    static let kontrolYazi: CGFloat = 17
    /// tutorial_skip_button_height / _text_size / _text_margin_h
    static let atlaYukseklik: CGFloat = 36
    static let atlaYazi: CGFloat = 14
    static let atlaYatay: CGFloat = 10
    /// devamButton: layout_marginHorizontal 5 (geri düğmesinin bitişinden)
    static let devamBaslangic: CGFloat = 5
}

/// Renkler (drawable XML'lerinden).
enum OgreticiRenk {
    /// bg_depth_tutorial_control: yüz #8FD14F, dudak #62A12E
    static let kontrolZemin = Color(red: 0x8F / 255.0, green: 0xD1 / 255.0, blue: 0x4F / 255.0)
    static let kontrolGolge = Color(red: 0x62 / 255.0, green: 0xA1 / 255.0, blue: 0x2E / 255.0)
    /// bg_depth_tutorial_skip: yüz #424242, dudak #262626
    static let atlaZemin = Color(red: 0x42 / 255.0, green: 0x42 / 255.0, blue: 0x42 / 255.0)
    static let atlaGolge = Color(red: 0x26 / 255.0, green: 0x26 / 255.0, blue: 0x26 / 255.0)
    /// table_cell_border5 zemini #222
    static let soruZemin = Color(red: 0x22 / 255.0, green: 0x22 / 255.0, blue: 0x22 / 255.0)

    /// ARGB tamsayıdan renk (questionTextColorPositions).
    static func argb(_ deger: UInt32) -> Color {
        let a: Double = Double((deger >> 24) & 0xFF) / 255.0
        let r: Double = Double((deger >> 16) & 0xFF) / 255.0
        let g: Double = Double((deger >> 8) & 0xFF) / 255.0
        let b: Double = Double(deger & 0xFF) / 255.0
        return Color(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}

/// Sol üstteki çıkış düğmesi (quitButton). Gizliyken (hesapsız ilk açılış) yer kaplar.
struct OgreticiUstCubuk: View {
    let gorunur: Bool
    let cik: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            Button(action: cik) {
                Image("quit_ic")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(Color("lesson_completed"))
                    .frame(width: OgreticiOlcu.cikisBoyu, height: OgreticiOlcu.cikisBoyu)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .opacity(gorunur ? 1 : 0)
            .allowsHitTesting(gorunur)
            Spacer(minLength: 0)
        }
        .padding(.leading, OgreticiOlcu.cikisBaslangic)
        .padding(.top, OgreticiOlcu.cikisUst)
    }
}

/// Adım metni (tutorialText): kalan yüksekliği kaplar, alta yaslı, ortalı. Daktiloda metnin tamamı
/// yerleşir, henüz yazılmamış kısım saydam çizilir (Android setTypewriterProgress: satır kırılımları
/// yazarken kaymaz).
struct OgreticiMetinAlani: View {
    let metin: String
    let gorunenHarf: Int

    var body: some View {
        metinGorunumu
            .font(Tema.yazi(OgreticiOlcu.metinYazi))
            .multilineTextAlignment(.center)
            .minimumScaleFactor(OgreticiOlcu.metinEnKucukOran)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
            .padding(.horizontal, OgreticiOlcu.metinYatay)
    }

    private var metinGorunumu: Text {
        let harfler: [Character] = Array(metin)
        let sinir: Int = min(max(gorunenHarf, 0), harfler.count)
        let yazilan: String = String(harfler[0..<sinir])
        let kalan: String = String(harfler[sinir..<harfler.count])
        let ilk: Text = Text(yazilan).foregroundColor(Color.white)
        let son: Text = Text(kalan).foregroundColor(Color.clear)
        return ilk + son
    }
}

/// Soru kutusu (questionText): 190 genişlik, #222 zemin, 5 siyah çerçeve. GORUNMEZ'de yer kaplar.
/// Renkli harfler questionTextColorPositions'tan.
struct OgreticiSoruKutusu: View {
    let adim: OgreticiAdimVerisi

    var body: some View {
        if adim.soruGorunurlugu.yerKaplar {
            soruMetni
                .font(Tema.yazi(OgreticiOlcu.metinYazi))
                .multilineTextAlignment(.center)
                .padding(OgreticiOlcu.soruCerceve)
                .frame(width: OgreticiOlcu.soruGenislik)
                .background(
                    RoundedRectangle(cornerRadius: OgreticiOlcu.soruKose)
                        .fill(OgreticiRenk.soruZemin)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: OgreticiOlcu.soruKose)
                        .strokeBorder(Color.black, lineWidth: OgreticiOlcu.soruCerceve)
                )
                .opacity(adim.soruGorunurlugu.cizilir ? 1 : 0)
        }
    }

    private var soruMetni: Text {
        // Boş soruda da kutunun yüksekliği korunur (Android: wrap_content, tek satır).
        let metin: String = adim.soru ?? " "
        if adim.soruRenkleri.isEmpty {
            return Text(metin.isEmpty ? " " : metin).foregroundColor(Color.white)
        }
        var renkler: [Int: UInt32] = [:]
        for r in adim.soruRenkleri {
            renkler[r.konum] = r.renk
        }
        var sonuc: Text = Text("")
        var konum: Int = 0
        // Android konumları UTF-16 birimidir; soru metinleri rakam ve işaretlerden oluştuğu için
        // karakter sırası aynıdır.
        for harf in metin {
            let renk: Color = renkler[konum].map { OgreticiRenk.argb($0) } ?? Color.white
            sonuc = sonuc + Text(String(harf)).foregroundColor(renk)
            konum += 1
        }
        return sonuc
    }
}

/// Alt panel (bottomPanelID): geri, "Kontrol Et", "Eğitimi atla"; son adımda "Devam Et".
struct OgreticiAltPanel: View {
    @ObservedObject var model: OgreticiGorunumModeli

    var body: some View {
        HStack(spacing: 0) {
            geriDugmesi
                .padding(.leading, OgreticiOlcu.geriBaslangic)
            if model.sahne.devamGorunur {
                devamDugmesi
                    .padding(.leading, OgreticiOlcu.devamBaslangic)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            } else {
                kontrolDugmesi
                    .padding(.horizontal, OgreticiOlcu.kontrolYan)
                atlaDugmesi
            }
        }
        .frame(height: OgreticiOlcu.kontrolYukseklik)
    }

    private var geriDugmesi: some View {
        let gorunur: Bool = model.sahne.geri.cizilir
        return Button(action: { model.geriBasildi() }) {
            Image("ic_tutorial_back_arrow")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(Color.black)
                .padding(.horizontal, 12)
                .padding(.top, 12)
                .padding(.bottom, 12)
        }
        .buttonStyle(GocenDugmeStili(zemin: OgreticiRenk.kontrolZemin, golge: OgreticiRenk.kontrolGolge, koseYaricapi: OgreticiOlcu.kontrolKose))
        .frame(width: OgreticiOlcu.geriBoyu, height: OgreticiOlcu.geriBoyu)
        .opacity(gorunur ? 1 : 0)
        .allowsHitTesting(gorunur)
    }

    private var kontrolDugmesi: some View {
        let gorunur: Bool = model.sahne.kontrolGorunur
        return Button(action: { model.kontrolEt() }) {
            Text(Metin.al("fragment_tutorial_kontrol_button"))
                .font(Tema.yazi(OgreticiOlcu.kontrolYazi))
                .foregroundColor(Color.black)
                .lineLimit(1)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(GocenDugmeStili(zemin: OgreticiRenk.kontrolZemin, golge: OgreticiRenk.kontrolGolge, koseYaricapi: OgreticiOlcu.kontrolKose))
        .frame(maxWidth: .infinity)
        .frame(height: OgreticiOlcu.kontrolYukseklik)
        .opacity(gorunur ? 1 : 0)
        .allowsHitTesting(gorunur)
    }

    private var atlaDugmesi: some View {
        let gorunur: Bool = model.sahne.atla.cizilir
        return Button(action: { model.atla() }) {
            Text(Metin.al("fragment_tutorial_text_35"))
                .font(Tema.yazi(OgreticiOlcu.atlaYazi))
                .foregroundColor(Color.white)
                .lineLimit(1)
                .padding(.horizontal, OgreticiOlcu.atlaYatay)
        }
        .buttonStyle(GocenDugmeStili(zemin: OgreticiRenk.atlaZemin, golge: OgreticiRenk.atlaGolge, koseYaricapi: 8))
        .frame(height: OgreticiOlcu.atlaYukseklik)
        .fixedSize(horizontal: true, vertical: false)
        .opacity(gorunur ? 1 : 0)
        .allowsHitTesting(gorunur)
    }

    private var devamDugmesi: some View {
        Button(action: { model.derseGec() }) {
            Text(Metin.al("fragment_tutorial_devam_button"))
                .font(Tema.yazi(OgreticiOlcu.kontrolYazi))
                .foregroundColor(Color.black)
                .lineLimit(1)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(GocenDugmeStili(zemin: OgreticiRenk.kontrolZemin, golge: OgreticiRenk.kontrolGolge, koseYaricapi: OgreticiOlcu.kontrolKose))
        .frame(maxWidth: .infinity)
        .frame(height: OgreticiOlcu.kontrolYukseklik)
    }
}

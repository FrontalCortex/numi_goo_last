import Shared
import SwiftUI

// Sobi'nin testi paneli: haritadaki Sobi düğmesinden alttan açılır.
//
// Android kaynakları: SobiTestSheet.kt, res/layout/sheet_sobi_test.xml, item_sobi_test_rule.xml,
// item_sobi_test_option.xml, drawable/bg_sobi_test_sheet.xml (background_color, üst köşeler 28),
// bg_sobi_test_handle.xml (40 × 5, #37464F, köşe 3), bg_sobi_test_rule.xml (bright_background_color,
// köşe 16), bg_hw_chip.xml / bg_sobi_test_option_selected.xml (dudaklı kart), bg_depth_tutorial_control.xml.
//
// Yerleşim (dp → pt): panel iç boşluğu 20 / 10 / 20 / 16; tutma çizgisi; 8 sonra Sobi (84 × 84, GREET)
// + 8 boşluk + başlık (21 sp kalın) / alt başlık (14 sp, 2 üstte); yüklenirken çark (36, üst-alt 24);
// boş durum metni (16 sp, ortalı, üst 16 alt 8); kurallar etiketi (13 sp kalın, üst 14) ve satırlar (üst 8,
// iç boşluk 14 / 10 / 14 / 12; ad 16 sp, "Seviye 5/20" 14 sp #1CB0F6; çubuk 8 kalın, 8 üstte); zorluk
// etiketi (13 sp, üst 16) ve kartlar (üst 8, iç boşluk 14 / 10 / 14 / 14; başlık 16 sp, seviye 14 sp,
// açıklama 14 sp); "Başla" (48, üst 18, siyah 17 sp; hazır değilken %50 saydam); "Pro'da ek testler
// ücretsiz ›" (44, #1CB0F6 15 sp; yalnızca anahtar yetmeyince); "Sonra" (44, ikincil 15 sp).
// Android BottomSheetDialog arkayı karartır; dokununca kapanır.
struct SobiTestSayfasi: View {
    @StateObject private var model: SobiTestSayfaModeli
    private let kapat: () -> Void
    private let basla: (SobiTestBaslangici) -> Void
    private let proAc: () -> Void
    @State private var acik: Bool = false

    /// - Parameters:
    ///   - model: nil ise sahte servislerle yeni model (veri açılışta yüklenir).
    ///   - kapat: "Sonra", "Tamam", karartıya dokunma.
    ///   - basla: plan kuruldu; panel kapanıp oynatıcı açılmalı.
    ///   - proAc: "Pro'da ek testler ücretsiz ›" (Android SobiProTrialFragment; iOS'ta plan karşılaştırma).
    init(
        model: SobiTestSayfaModeli? = nil,
        kapat: @escaping () -> Void,
        basla: @escaping (SobiTestBaslangici) -> Void,
        proAc: @escaping () -> Void
    ) {
        let kullanilacak: SobiTestSayfaModeli = model ?? SobiTestSayfaModeli()
        _model = StateObject(wrappedValue: kullanilacak)
        self.kapat = kapat
        self.basla = basla
        self.proAc = proAc
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.5)
                .ignoresSafeArea()
                .onTapGesture { kapat() }
            panel
                .offset(y: acik ? 0 : 700)
        }
        .onAppear {
            withAnimation(Animation.easeOut(duration: 0.25)) {
                acik = true
            }
            model.yukle()
        }
    }

    private var panel: some View {
        VStack(alignment: .leading, spacing: 0) {
            SobiTestTutmaCizgisi()
            SobiTestSayfaBasligi(model: model)
                .padding(.top, 8)
            SobiTestSayfaGovdesi(model: model)
            SobiTestSayfaDugmeleri(model: model, kapat: kapat, basla: basla, proAc: proAc)
        }
        .padding(.leading, 20)
        .padding(.trailing, 20)
        .padding(.top, 10)
        .padding(.bottom, 16)
        .frame(maxWidth: .infinity)
        .background(
            SobiTestUstKoseler(yaricap: 28)
                .fill(Color("background_color"))
                .ignoresSafeArea(edges: .bottom)
        )
        // Panelin içine dokunmak karartının kapatmasına gitmesin.
        .contentShape(Rectangle())
        .onTapGesture {}
    }
}

/// Yalnızca üst köşeleri yuvarlak dikdörtgen (iOS 16'da UnevenRoundedRectangle yok).
struct SobiTestUstKoseler: Shape {
    let yaricap: CGFloat

    func path(in rect: CGRect) -> Path {
        let r: CGFloat = min(yaricap, min(rect.width, rect.height) / 2)
        var yol = Path()
        yol.move(to: CGPoint(x: rect.minX, y: rect.maxY))
        yol.addLine(to: CGPoint(x: rect.minX, y: rect.minY + r))
        yol.addArc(center: CGPoint(x: rect.minX + r, y: rect.minY + r), radius: r,
                   startAngle: Angle.degrees(180), endAngle: Angle.degrees(270), clockwise: false)
        yol.addLine(to: CGPoint(x: rect.maxX - r, y: rect.minY))
        yol.addArc(center: CGPoint(x: rect.maxX - r, y: rect.minY + r), radius: r,
                   startAngle: Angle.degrees(270), endAngle: Angle.degrees(0), clockwise: false)
        yol.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        yol.closeSubpath()
        return yol
    }
}

struct SobiTestTutmaCizgisi: View {
    var body: some View {
        RoundedRectangle(cornerRadius: 3)
            .fill(SobiTestRenk.kenar)
            .frame(width: 40, height: 5)
            .frame(maxWidth: .infinity)
    }
}

/// Sobi + başlık + alt başlık.
struct SobiTestSayfaBasligi: View {
    @ObservedObject var model: SobiTestSayfaModeli

    var body: some View {
        HStack(alignment: .center, spacing: 8) {
            SobiMaskotGorunumu(emote: model.maskot)
                .frame(width: 84, height: 84)
            VStack(alignment: .leading, spacing: 2) {
                Text(model.baslik)
                    .font(Tema.yazi(21))
                    .foregroundColor(SobiTestRenk.yazi)
                    .fixedSize(horizontal: false, vertical: true)
                Text(model.altBaslik)
                    .font(Tema.yazi(14))
                    .foregroundColor(SobiTestRenk.ikincilYazi)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

/// Çark, boş durum metni ya da kurallar + zorluk kartları.
struct SobiTestSayfaGovdesi: View {
    @ObservedObject var model: SobiTestSayfaModeli

    var body: some View {
        switch model.durum {
        case .yukleniyor:
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: SobiTestRenk.ikincilYazi))
                .scaleEffect(1.5)
                .frame(width: 36, height: 36)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 24)
        case .bos(let metin):
            Text(metin)
                .font(Tema.yazi(16))
                .foregroundColor(SobiTestRenk.yazi)
                .multilineTextAlignment(.center)
                .lineSpacing(3)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity)
                .padding(.top, 16)
                .padding(.bottom, 8)
        case .hazir:
            SobiTestSayfaIcerigi(model: model)
        }
    }
}

struct SobiTestSayfaIcerigi: View {
    @ObservedObject var model: SobiTestSayfaModeli

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            SobiTestBolumEtiketi(metin: Metin.al("sobi_test_rules_label"))
                .padding(.top, 14)
            ForEach(model.kurallar) { satir in
                SobiTestKuralSatiriGorunumu(satir: satir)
                    .padding(.top, 8)
            }
            if !model.kartlar.isEmpty {
                SobiTestBolumEtiketi(metin: Metin.al("sobi_test_difficulty_label"))
                    .padding(.top, 16)
                ForEach(model.kartlar) { kart in
                    SobiTestZorlukKartiGorunumu(
                        kart: kart,
                        secili: kart.id == model.secilen,
                        etkin: !model.hazirlaniyor,
                        sec: { model.sec(kart.id) }
                    )
                    .padding(.top, 8)
                }
            }
        }
    }
}

struct SobiTestBolumEtiketi: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(Tema.yazi(13))
            .foregroundColor(SobiTestRenk.ikincilYazi)
    }
}

/// item_sobi_test_rule.xml
struct SobiTestKuralSatiriGorunumu: View {
    let satir: SobiTestKuralSatiri

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center, spacing: 0) {
                Text(satir.ad)
                    .font(Tema.yazi(16))
                    .foregroundColor(SobiTestRenk.yazi)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let seviye = satir.seviye {
                    Text(Metin.al("sobi_test_level_of", seviye, Int(SobiTestLevels.shared.MAX)))
                        .font(Tema.yazi(14))
                        .foregroundColor(SobiTestRenk.mavi)
                }
            }
            if let seviye = satir.seviye {
                SobiTestSeviyeCubugu(seviye: seviye, azami: Int(SobiTestLevels.shared.MAX))
                    .padding(.top, 8)
            }
        }
        .padding(.leading, 14)
        .padding(.trailing, 14)
        .padding(.top, 10)
        .padding(.bottom, 12)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color("bright_background_color")))
    }
}

/// LinearProgressIndicator: 8 kalın, iz #1AFFFFFF, gösterge #1CB0F6, köşe 4.
struct SobiTestSeviyeCubugu: View {
    let seviye: Int
    let azami: Int

    var body: some View {
        GeometryReader { geo in
            let oran: CGFloat = azami > 0 ? CGFloat(min(max(seviye, 0), azami)) / CGFloat(azami) : 0
            let genislik: CGFloat = geo.size.width * oran
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(Color.white.opacity(0.1))
                RoundedRectangle(cornerRadius: 4)
                    .fill(SobiTestRenk.mavi)
                    .frame(width: genislik)
            }
        }
        .frame(height: 8)
    }
}

/// item_sobi_test_option.xml: dudaklı kart; seçili kart mavi.
struct SobiTestZorlukKartiGorunumu: View {
    let kart: SobiTestZorlukKarti
    let secili: Bool
    let etkin: Bool
    let sec: () -> Void

    var body: some View {
        Button(action: sec) {
            VStack(alignment: .leading, spacing: 2) {
                HStack(alignment: .center, spacing: 0) {
                    Text(kart.baslik)
                        .font(Tema.yazi(16))
                        .foregroundColor(SobiTestRenk.yazi)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(kart.seviye)
                        .font(Tema.yazi(14))
                        .foregroundColor(secili ? SobiTestRenk.mavi : SobiTestRenk.ikincilYazi)
                }
                Text(kart.aciklama)
                    .font(Tema.yazi(14))
                    .foregroundColor(SobiTestRenk.ikincilYazi)
            }
            .padding(.leading, 14)
            .padding(.trailing, 14)
            .padding(.top, 10)
            .padding(.bottom, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .buttonStyle(SobiTestKartStili(secili: secili))
        .disabled(!etkin)
    }
}

/// bg_hw_chip / bg_sobi_test_option_selected: yüz + 2 kenar çizgisi, altta 4 dudak; basılınca yüz iner.
struct SobiTestKartStili: ButtonStyle {
    let secili: Bool

    func makeBody(configuration: Configuration) -> some View {
        let basili: Bool = configuration.isPressed
        let yuz: Color = secili ? SobiTestRenk.seciliYuz : Color("background_color")
        let kenar: Color = secili ? SobiTestRenk.mavi : SobiTestRenk.kenar
        let dudak: Color = secili ? SobiTestRenk.maviDudak : SobiTestRenk.kenar
        return ZStack(alignment: .top) {
            RoundedRectangle(cornerRadius: 16)
                .fill(dudak)
                .padding(.top, 4)
                .opacity(basili ? 0 : 1)
            configuration.label
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(yuz)
                        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(kenar, lineWidth: 2))
                )
                .padding(.bottom, 4)
                .offset(y: basili ? 4 : 0)
        }
        .contentShape(Rectangle())
    }
}

/// "Başla" (ya da "Tamam"), "Pro'da ek testler ücretsiz ›", "Sonra".
struct SobiTestSayfaDugmeleri: View {
    @ObservedObject var model: SobiTestSayfaModeli
    let kapat: () -> Void
    let basla: (SobiTestBaslangici) -> Void
    let proAc: () -> Void

    var body: some View {
        VStack(spacing: 4) {
            anaDugme
                .padding(.top, 18)
            if model.proGoster {
                metinDugmesi(Metin.al("sobi_test_pro_free"), renk: SobiTestRenk.mavi) {
                    kapat()
                    proAc()
                }
            }
            if !bosMu {
                metinDugmesi(Metin.al("sobi_test_dialog_later"), renk: SobiTestRenk.ikincilYazi, eylem: kapat)
            }
        }
    }

    private var bosMu: Bool {
        if case .bos = model.durum { return true }
        return false
    }

    private var hazirMi: Bool {
        if case .hazir = model.durum { return true }
        return false
    }

    private var anaDugme: some View {
        let etkin: Bool = (hazirMi && !model.hazirlaniyor) || bosMu
        let tutamak: SobiTestSayfaModeli = model
        let cikis: () -> Void = kapat
        let baslat: (SobiTestBaslangici) -> Void = basla
        return Button(action: {
            if case .bos = tutamak.durum {
                cikis()
            } else {
                tutamak.basla(baslat)
            }
        }) {
            anaDugmeEtiketi
        }
        .buttonStyle(GocenDugmeStili(zemin: SobiTestRenk.yesil, golge: SobiTestRenk.yesilDudak, koseYaricapi: 10, derinlik: 4))
        .frame(height: 48)
        .opacity(hazirMi || bosMu ? 1.0 : 0.5)
        .disabled(!etkin)
    }

    @ViewBuilder
    private var anaDugmeEtiketi: some View {
        if bosMu {
            dugmeYazisi(Metin.al("sobi_test_ok"))
        } else if model.hazirlaniyor {
            dugmeYazisi(Metin.al("sobi_test_preparing"))
        } else if model.bedel > 0 {
            // "Başla · 3" + uygulamanın anahtar görseli (metnin 1,25 katı).
            HStack(spacing: 4) {
                dugmeYazisi(Metin.al("sobi_test_start_keys", model.bedel))
                Image("key")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 21, height: 21)
            }
        } else {
            dugmeYazisi(Metin.al("sobi_test_dialog_start"))
        }
    }

    private func dugmeYazisi(_ metin: String) -> some View {
        Text(metin)
            .font(Tema.yazi(17))
            .foregroundColor(Color.black)
            .lineLimit(1)
    }

    private func metinDugmesi(_ metin: String, renk: Color, eylem: @escaping () -> Void) -> some View {
        Button(action: eylem) {
            Text(metin)
                .font(Tema.yazi(15))
                .foregroundColor(renk)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

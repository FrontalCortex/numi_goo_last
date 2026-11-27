import SwiftUI

// Abaküs ayar paneli (abaküsün eni/boyu/alt boşluğu, boncukların eni/boyu/üst ve alt boşlukları) ve
// abaküsün yan düğme sütununa sığdırılması.
//
// Android kaynakları:
//  - abacus/AbacusSettingsPanel.kt + res/layout/view_abacus_settings_panel.xml (panel, kaydırıcılar,
//    kaydet / sıfırla; ayarlar "AbacusUISettings" tercihlerinde, ekranlar arasında ortak)
//  - abacus/AbacusSideButtonFit.kt (abaküs, sağ kenardaki düğme sütunu ile çakışmasın diye küçülür;
//    görünen ölçek = kullanıcı ölçeği × sığdırma payı, pivot alt-orta, en küçük pay 0,6)
//
// Aynı model abaküs dersinde ve kör dersin abaküslü türlerinde (bölüm 7, 8, 9) kullanılır.

/// Ayar panelinin durumu. Değerler kaydırıcıyı oynatınca anında uygulanır; "kaydet" kalıcı yapar,
/// paneli kayıtsız kapatmak değerleri geri almaz (Android'deki gibi).
final class AbakusGorunumAyari: ObservableObject {
    /// Abaküs ölçeği (0,5...2,0).
    @Published var abakusX: Double = 1
    @Published var abakusY: Double = 1
    /// Abaküsün alt boşluğu, dp (0...100). Varsayılan tutorial_abacus_linear_margin_bottom = 10.
    @Published var altBosluk: Double = AbakusGorunumAyari.varsayilanAltBosluk
    /// Boncuk ölçeği (0,5...2,0).
    @Published var boncukX: Double = 1
    @Published var boncukY: Double = 1
    /// Üst boncuğun üst boşluğu (0...30) ve alt boncukların ek alt boşluğu (0...50), dp.
    @Published var boncukUst: Double = 0
    @Published var boncukAlt: Double = 0
    /// Panel açık mı (kayma animasyonunu görünüm çizer).
    @Published private(set) var acik: Bool = false

    static let varsayilanAltBosluk: Double = 10

    private static let anahtarOnEki: String = "AbacusUISettings."

    init() {
        yukle()
    }

    /// Boncuk ölçeği ve boşluklarının tahtaya verilen karşılığı.
    var boncukAyari: AbakusBoncukAyari {
        return AbakusBoncukAyari(
            olcekX: CGFloat(boncukX), olcekY: CGFloat(boncukY),
            ustBosluk: CGFloat(boncukUst), altBosluk: CGFloat(boncukAlt)
        )
    }

    /// Android: abacusModeButton tıklaması. Açılırken ekran abaküsü sıfırlayıp dokunmayı kapatır
    /// (`acildi`), kapanırken dokunma geri açılır (`kapandi`).
    func degistir(acildi: () -> Void, kapandi: () -> Void) {
        if acik {
            kapat(kapandi: kapandi)
        } else {
            acildi()
            withAnimation(.easeOut(duration: 0.3)) { acik = true }
        }
    }

    /// Açıksa kapatır.
    func kapat(kapandi: () -> Void) {
        if !acik { return }
        kapandi()
        withAnimation(.easeIn(duration: 0.3)) { acik = false }
    }

    /// Android: abacusSettingsSaveButton (değerleri kaydedip paneli kapatır).
    func kaydet(kapandi: () -> Void) {
        let ud = UserDefaults.standard
        let on: String = AbakusGorunumAyari.anahtarOnEki
        ud.set(abakusX, forKey: on + "savedAbacusScaleX")
        ud.set(abakusY, forKey: on + "savedAbacusScaleY")
        ud.set(altBosluk, forKey: on + "savedAbacusMarginBottomDp")
        ud.set(boncukX, forKey: on + "savedBeadScaleX")
        ud.set(boncukY, forKey: on + "savedBeadScaleY")
        ud.set(boncukUst, forKey: on + "savedBeadMarginTopDp")
        ud.set(boncukAlt, forKey: on + "savedBeadMarginBottomDp")
        kapat(kapandi: kapandi)
    }

    /// Android: abacusSettingsResetButton (varsayılana döner ve kayıtlı ayarı siler).
    func sifirla() {
        abakusX = 1
        abakusY = 1
        altBosluk = AbakusGorunumAyari.varsayilanAltBosluk
        boncukX = 1
        boncukY = 1
        boncukUst = 0
        boncukAlt = 0
        let ud = UserDefaults.standard
        let on: String = AbakusGorunumAyari.anahtarOnEki
        let anahtarlar: [String] = [
            "savedAbacusScaleX", "savedAbacusScaleY", "savedAbacusMarginBottomDp",
            "savedBeadScaleX", "savedBeadScaleY", "savedBeadMarginTopDp", "savedBeadMarginBottomDp",
        ]
        for anahtar in anahtarlar {
            ud.removeObject(forKey: on + anahtar)
        }
    }

    private func yukle() {
        let ud = UserDefaults.standard
        let on: String = AbakusGorunumAyari.anahtarOnEki
        abakusX = AbakusGorunumAyari.oku(ud, on + "savedAbacusScaleX", 1)
        abakusY = AbakusGorunumAyari.oku(ud, on + "savedAbacusScaleY", 1)
        // Android: kayıtlı alt boşluk 0 ise yerleşimdeki varsayılan (10 dp) kullanılır.
        let kayitliAlt: Double = AbakusGorunumAyari.oku(ud, on + "savedAbacusMarginBottomDp", 0)
        altBosluk = kayitliAlt > 0 ? kayitliAlt : AbakusGorunumAyari.varsayilanAltBosluk
        boncukX = AbakusGorunumAyari.oku(ud, on + "savedBeadScaleX", 1)
        boncukY = AbakusGorunumAyari.oku(ud, on + "savedBeadScaleY", 1)
        boncukUst = AbakusGorunumAyari.oku(ud, on + "savedBeadMarginTopDp", 0)
        boncukAlt = AbakusGorunumAyari.oku(ud, on + "savedBeadMarginBottomDp", 0)
    }

    private static func oku(_ ud: UserDefaults, _ anahtar: String, _ varsayilan: Double) -> Double {
        if ud.object(forKey: anahtar) == nil { return varsayilan }
        return ud.double(forKey: anahtar)
    }
}

// MARK: - Sığdırma

/// Android: AbacusSideButtonFit.computeFit — abaküs ortalı, kenardaki düğme sütunu + 6 dp boşluk kadar yer
/// karşı kenarda da bırakılır; sığmıyorsa küçülür (en çok 0,6'ya kadar).
enum AbakusYanSigdirma {
    /// Düğme sütununun kenardan kapladığı genişlik (abacus_side_button_size; marginEnd −10 + kök dolgu 10).
    static let sutunGenisligi: CGFloat = 48
    /// AbacusSideButtonFit(gapPx = 6 dp).
    static let aralik: CGFloat = 6
    static let enKucuk: CGFloat = 0.6

    /// - Parameters:
    ///   - ekranGenisligi: kök görünümün genişliği (dolgu dahil).
    ///   - kullaniciX: ayar panelindeki yatay abaküs ölçeği.
    ///   - sutunVar: abaküsle aynı yükseklikte görünen en az bir yan düğme var mı.
    static func pay(ekranGenisligi: CGFloat, kullaniciX: CGFloat, sutunVar: Bool) -> CGFloat {
        if !sutunVar { return 1 }
        let abakusEni: CGFloat = AbakusGorunumu.boyut(basamakSayisi: 5).width * kullaniciX
        if abakusEni <= 0 { return 1 }
        let ayrilan: CGFloat = sutunGenisligi + aralik
        let bos: CGFloat = ekranGenisligi - 2 * ayrilan
        let oran: CGFloat = bos / abakusEni
        return min(max(oran, enKucuk), 1)
    }
}

// MARK: - Panel

/// view_abacus_settings_panel.xml: 340 dp genişlik, iç boşluk 16, guide_panel_background (#222, köşe 18).
struct AbakusAyarPaneli: View {
    @ObservedObject var ayar: AbakusGorunumAyari
    let kaydet: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .top, spacing: 0) {
                abakusSutunu
                    .padding(.trailing, 8)
                boncukSutunu
                    .padding(.leading, 8)
            }
            dugmeSatiri
                .padding(.top, 8)
        }
        .padding(16)
        .frame(width: 340)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous).fill(AbakusRenk.renk(0x222222))
        )
    }

    private var abakusSutunu: some View {
        VStack(spacing: 0) {
            Image("abacus_svg_ic")
                .resizable()
                .scaledToFit()
                .frame(width: 32, height: 32)
                .padding(.bottom, 12)
            Slider(value: $ayar.abakusX, in: 0.5...2.0)
                .padding(.bottom, 16)
            Slider(value: $ayar.abakusY, in: 0.5...2.0)
                .padding(.bottom, 16)
            Slider(value: $ayar.altBosluk, in: 0...100, step: 1)
        }
        .frame(maxWidth: .infinity)
    }

    private var boncukSutunu: some View {
        VStack(spacing: 0) {
            Image("bead")
                .resizable()
                .scaledToFit()
                .frame(width: 32, height: 32)
                .padding(.bottom, 12)
            Slider(value: $ayar.boncukX, in: 0.5...2.0)
                .padding(.bottom, 16)
            Slider(value: $ayar.boncukY, in: 0.5...2.0)
                .padding(.bottom, 16)
            Slider(value: $ayar.boncukUst, in: 0...30, step: 1)
                .padding(.bottom, 16)
            Slider(value: $ayar.boncukAlt, in: 0...50, step: 1)
        }
        .frame(maxWidth: .infinity)
    }

    /// abacusSettingsResetButton (special_reset_ic, dolgu 8) ve abacusSettingsSaveButton (correct_ic,
    /// dolgu 6): 40 dp yuvarlak, arada 16 dp.
    private var dugmeSatiri: some View {
        HStack(spacing: 0) {
            Spacer(minLength: 0)
            Button(action: { ayar.sifirla() }) {
                ZStack {
                    Circle().fill(Color("side_button_background"))
                    Image("special_reset_ic")
                        .resizable()
                        .scaledToFit()
                        .padding(8)
                }
                .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)
            .padding(.trailing, 16)
            Button(action: kaydet) {
                ZStack {
                    Circle().fill(Color("side_button_background"))
                    Image("correct_ic")
                        .resizable()
                        .scaledToFit()
                        .padding(6)
                }
                .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)
        }
    }
}

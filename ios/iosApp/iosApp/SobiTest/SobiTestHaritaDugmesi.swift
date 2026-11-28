import Shared
import SwiftUI

// Haritadaki Sobi düğmesi: Sobi'nin günlük testi.
//
// Android kaynakları: MapSobiTestButton.kt, res/layout/view_map_sobi_test.xml (56 dp; yüz bg_hw_send:
// #1CB0F6, dudak #1899D6, köşe 16, dudak 4; iç boşluk 8/7/8/12; içinde SobiHeadView), bg_map_sobi_dot.xml
// (14 dp kırmızı #FF4B4B nokta, 2 dp background_color kenar, sağ üstte), SobiNudge.kt +
// view_sobi_nudge.xml (#1CB0F6 balon, köşe 16, 15 sp beyaz kalın, en çok 230 dp, düğmenin solunda 4 dp,
// 5 sn sonra kendiliğinden kapanır; 1,2 sn gecikmeyle günde bir kez).
//
// Yerleşim (çağıran verir): ders listesinde sağdan 18, alttan 16 ("Öğretmene sor" gizli; onun 60 dp'lik
// ekseninde: 16 + (60 − 56) / 2); kısım seçiminde sağdan 16, alttan 16.
// Görünürlük: Part 1'in 6. satırı bitince (SobiDailyTest.checkUnlocked); öğretmende hiç (SobiAccess).
// Nokta: bugünün testi bitmediyse.

/// Düğmenin durumu; uygulama boyunca tek (SobiTestOrtak.dugme).
final class SobiTestDugmeModeli: ObservableObject {
    @Published private(set) var gorunur: Bool = false
    @Published private(set) var nokta: Bool = false
    @Published private(set) var balon: Bool = false

    /// Düğmeye (ya da balona) dokunuldu: test sayfası açılır. Yönetici bağlar.
    var acildi: (() -> Void)? = nil

    /// Ekran galerisinde haritanın bütün durumlarında düğme ve nokta görünür (açılma verisi yok).
    private let galeriKipi: Bool
    private var balonKimligi: Int = 0

    init() {
        let ad: String = EkranGalerisi.istenenAd ?? ""
        galeriKipi = ad.hasPrefix("harita")
        if galeriKipi {
            gorunur = true
            nokta = true
        }
    }

    /// Android MapSobiTestButton.refresh: ekran her göründüğünde. `balonla` false: yalnızca görünürlük ve
    /// nokta (ders dönüşü, test bitişi).
    func yenile(balonla: Bool) {
        if galeriKipi {
            return
        }
        let servis: SobiTestVeriServisi = SobiTestOrtak.servis
        let acik: Bool = servis.sobiAcik && SobiTestOrtak.acikMi()
        if gorunur != acik {
            gorunur = acik
        }
        if !acik {
            balonuKapat()
            return
        }
        let bugun: String = SobiTestOrtak.bugun()
        let bitti: Bool = SobiTestOrtak.depo.isDoneToday(today: bugun)
        if nokta == bitti {
            nokta = !bitti
        }
        if balonla && !bitti && SobiTestOrtak.depo.shouldNudgeToday(today: bugun) {
            balonuGoster()
        }
    }

    func dokunuldu() {
        balonuKapat()
        acildi?()
    }

    private func balonuGoster() {
        balonKimligi += 1
        let kimlik: Int = balonKimligi
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) { [weak self] in
            guard let self = self, self.balonKimligi == kimlik, self.gorunur else { return }
            self.balon = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 5.0) { [weak self] in
                guard let self = self, self.balonKimligi == kimlik else { return }
                self.balon = false
            }
        }
    }

    private func balonuKapat() {
        balonKimligi += 1
        if balon {
            balon = false
        }
    }
}

/// Haritadaki düğme (ve solundaki balon). Görünmüyorsa hiçbir şey çizmez.
struct SobiTestHaritaDugmesi: View {
    @ObservedObject var model: SobiTestDugmeModeli

    init(model: SobiTestDugmeModeli = SobiTestOrtak.dugme) {
        self.model = model
    }

    var body: some View {
        icerik
            .onAppear {
                let tutamak: SobiTestDugmeModeli = model
                DispatchQueue.main.async {
                    tutamak.yenile(balonla: true)
                }
            }
    }

    @ViewBuilder
    private var icerik: some View {
        if model.gorunur {
            HStack(alignment: .center, spacing: 4) {
                if model.balon {
                    SobiTestBalonu(metin: Metin.al("sobi_test_nudge"), dokun: { model.dokunuldu() })
                        .transition(AnyTransition.opacity.combined(with: AnyTransition.scale(scale: 0.85, anchor: .trailing)))
                }
                dugme
            }
            .animation(Animation.easeOut(duration: 0.22), value: model.balon)
        }
    }

    private var dugme: some View {
        let tutamak: SobiTestDugmeModeli = model
        return Button(action: { tutamak.dokunuldu() }) {
            SobiTestKafasi(boyut: 37)
                .padding(.top, 7)
                .padding(.bottom, 8)
                .frame(width: 56, height: 56)
        }
        .buttonStyle(GocenDugmeStili(zemin: SobiTestRenk.mavi, golge: SobiTestRenk.maviDudak, koseYaricapi: 16, derinlik: 4))
        .frame(width: 56, height: 56)
        .overlay(alignment: .topTrailing) { noktaGorunumu }
        .accessibilityLabel(Text(Metin.al("sobi_test_button_desc")))
    }

    @ViewBuilder
    private var noktaGorunumu: some View {
        if model.nokta {
            Circle()
                .fill(SobiTestRenk.kirmizi)
                .overlay(Circle().stroke(Color("background_color"), lineWidth: 2))
                .frame(width: 14, height: 14)
                .allowsHitTesting(false)
        }
    }
}

/// "Senin için bir test hazırladım!" balonu; kuyruğu sağda, düğmeye bakar.
struct SobiTestBalonu: View {
    let metin: String
    let dokun: () -> Void

    var body: some View {
        Button(action: dokun) {
            Text(metin)
                .font(Tema.yazi(15))
                .foregroundColor(.white)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.leading, 14)
                .padding(.trailing, 14)
                .padding(.vertical, 9)
                .frame(maxWidth: 221, alignment: .leading)
                .background(RoundedRectangle(cornerRadius: 16).fill(SobiTestRenk.mavi))
                .overlay(alignment: .trailing) { kuyruk }
                .padding(.trailing, 9)
        }
        .buttonStyle(.plain)
    }

    /// Android bg_sobi_nudge_bubble: sağ kenarda köşesi 2 dp yuvarlak, 45° döndürülmüş kare.
    private var kuyruk: some View {
        RoundedRectangle(cornerRadius: 2)
            .fill(SobiTestRenk.mavi)
            .frame(width: 12, height: 12)
            .rotationEffect(Angle.degrees(45))
            .offset(x: 5)
    }
}

/// Sobi'nin yalnızca kafası (Android SobiHeadView). Ayrı kafa çizimi yok: durağan tam gövde maskot kafa
/// bölgesine kırpılır (Magaza/MagazaSobiKafasi ile aynı kırpma; değerler çıktı görülmeden seçilmiş tahmin).
struct SobiTestKafasi: View {
    let boyut: CGFloat

    var body: some View {
        let kafaBirimi: CGFloat = 210
        let tuval: CGFloat = boyut * 314.0 / kafaBirimi
        let kayma: CGFloat = tuval * 52.0 / 314.0
        SobiMaskotGorunumu(emote: "NOTR", animasyonlu: false)
            .frame(width: tuval, height: tuval)
            .offset(y: kayma)
            .frame(width: boyut, height: boyut)
            .clipped()
    }
}

/// Sobi testi ekranlarının renkleri (Android drawable'larındaki sabit değerler).
enum SobiTestRenk {
    static func renk(_ onaltilik: UInt32) -> Color {
        let r: Double = Double((onaltilik >> 16) & 0xFF) / 255.0
        let g: Double = Double((onaltilik >> 8) & 0xFF) / 255.0
        let b: Double = Double(onaltilik & 0xFF) / 255.0
        return Color(red: r, green: g, blue: b)
    }

    /// bg_hw_send yüzü, seviye yazısı ve çubuğu.
    static let mavi: Color = renk(0x1CB0F6)
    static let maviDudak: Color = renk(0x1899D6)
    static let kirmizi: Color = renk(0xFF4B4B)
    static let kirmiziDudak: Color = renk(0xD12F2F)
    /// bg_hw_chip kenarı ve dudağı, tutma çizgisi.
    static let kenar: Color = renk(0x37464F)
    /// Seçili zorluk kartının yüzü.
    static let seciliYuz: Color = renk(0x16384A)
    /// bg_depth_tutorial_control (Başla / Kontrol).
    static let yesil: Color = renk(0x8FD14F)
    static let yesilDudak: Color = renk(0x62A12E)
    /// bg_depth_tutorial_continue (doğru paneli) ve özet puanı.
    static let devamYesil: Color = renk(0x8CC63F)
    static let devamDudak: Color = renk(0x62982A)
    /// bg_depth_hw_control pasif.
    static let pasif: Color = Color("button_disabled")
    static let pasifDudak: Color = renk(0xA6A6A6)
    /// bottom_sheet_background (sonuç paneli).
    static let panel: Color = renk(0x222222)
    static let ikincilYazi: Color = Color("dark_text_secondary")
    static let yazi: Color = Color("dark_text")
}

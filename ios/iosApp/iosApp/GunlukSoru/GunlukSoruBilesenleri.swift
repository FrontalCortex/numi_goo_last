import SwiftUI

// Görevler sekmesinde (Gorevler/GorevlerEkrani) ve Kupa Yolu sekmesinde kullanılan günlük soru
// bileşenleri: ekran katmanları, sıradan kart (Abaküs, Kupa Yolu), ok, balon ve renkler.
// Android karşılığı: TasksFragment + res/layout/item_bulletin_card.xml.
// Ölçüler layout XML'lerinden birebir alınmıştır (dp → pt, sp → pt).

// MARK: - Ekran katmanları

/// Günlük soru modelinin ekranın üstüne bindirdiği şeyler: "Mücadeleye devam et" paneli, geçiş
/// sürerken dokunuşları yutan katman ve kısa süreli bilgi balonu (Toast). Giriş görünümü
/// (GorevlerEkrani) bunu kaydırılan listenin üstüne koyar.
struct GunlukSoruKatmanlari: View {
    @ObservedObject var model: GunlukSoruModeli

    var body: some View {
        ZStack {
            devamKatmani
            kilitKatmani
            balonKatmani
        }
        .animation(.easeInOut(duration: 0.2), value: model.bildirim)
        .animation(.easeInOut(duration: 0.2), value: model.devamPaneli?.id)
    }

    @ViewBuilder
    private var devamKatmani: some View {
        if let istek = model.devamPaneli {
            GunlukSoruDevamKatmani(
                istek: istek,
                kapat: { model.devamPaneliniKapat() },
                devam: { model.anahtarlaDevam() }
            )
            .transition(.opacity)
            .zIndex(1)
        }
    }

    /// Android: launch touch blocker — geçiş sürerken dokunuşları yutar.
    @ViewBuilder
    private var kilitKatmani: some View {
        if model.dokunmaKilitli {
            Color.black.opacity(0.001)
                .ignoresSafeArea()
        }
    }

    @ViewBuilder
    private var balonKatmani: some View {
        if let bildirim = model.bildirim {
            VStack {
                Spacer()
                GunlukSoruBildirimGorunumu(metin: bildirim.metin)
            }
            .allowsHitTesting(false)
            .transition(.opacity)
        }
    }
}

// MARK: - Sıradan kart (item_bulletin_card.xml)

/// Sıradan kartların içeriği. Android'de metinler TasksFragment.submitBulletinList'te strings'ten
/// verilir (ui_abakus, ui_kupa_yolu ...).
struct GunlukSoruKesfetKartIcerigi {
    let baslik: String
    let altBaslik: String
    let simge: String
    let zemin: Color

    static func al(_ tur: GunlukSoruKesfetKarti) -> GunlukSoruKesfetKartIcerigi {
        switch tur {
        case .abakusPratik:
            // Android: colorRes verilmezse R.color.button_enabled.
            return GunlukSoruKesfetKartIcerigi(
                baslik: Metin.al("ui_abakus"),
                altBaslik: Metin.al("ui_abakuste_pratik_yaparak_kendini_gelistir"),
                simge: "abacus_svg_ic",
                zemin: Color("button_enabled")
            )
        case .kupaYolu:
            return GunlukSoruKesfetKartIcerigi(
                baslik: Metin.al("ui_kupa_yolu"),
                altBaslik: Metin.al("ui_basarilarini_kupalarla_olc_seviyeni_yukselt"),
                simge: "infinity_cup_ic",
                zemin: Color("lesson_header_yellow")
            )
        }
    }
}

struct GunlukSoruKesfetKartGorunumu: View {
    let tur: GunlukSoruKesfetKarti
    let dokunuldu: () -> Void

    var body: some View {
        let icerik: GunlukSoruKesfetKartIcerigi = GunlukSoruKesfetKartIcerigi.al(tur)
        return Button(action: dokunuldu) {
            HStack(alignment: .center, spacing: 0) {
                VStack(alignment: .leading, spacing: 6) {
                    HStack(alignment: .center, spacing: 6) {
                        Image(icerik.simge)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 28, height: 28)
                        Text(icerik.baslik)
                            .font(Tema.yazi(18))
                            .foregroundColor(.white)
                    }
                    Text(icerik.altBaslik)
                        .font(Tema.yazi(14))
                        .foregroundColor(GunlukSoruStil.altBaslikRengi)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                GunlukSoruOk()
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 18)
                    .fill(icerik.zemin)
            )
            // cardElevation = 2dp
            .shadow(color: Color.black.opacity(0.25), radius: 2, x: 0, y: 1)
        }
        .buttonStyle(GunlukSoruKartBasmaStili())
    }
}

/// Kartın sağındaki beyaz ok (dualingo_arrow_ic, 16dp, beyaz).
struct GunlukSoruOk: View {
    var body: some View {
        Image("dualingo_arrow_ic")
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .frame(width: 16, height: 16)
            .foregroundColor(.white)
    }
}

/// MaterialCardView dalga efektinin karşılığı: basılıyken hafif kararma.
struct GunlukSoruKartBasmaStili: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.85 : 1.0)
    }
}

/// Kısa süreli bilgi balonu (Android: Toast).
struct GunlukSoruBildirimGorunumu: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(Tema.yazi(14, kalin: false))
            .foregroundColor(.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Capsule().fill(GunlukSoruStil.balonZemini))
            .padding(.horizontal, 24)
            .padding(.bottom, 48)
    }
}

// MARK: - Sabit renkler

enum GunlukSoruStil {
    static func renk(_ rgb: UInt32) -> Color {
        let k: Double = Double((rgb >> 16) & 0xFF) / 255.0
        let y: Double = Double((rgb >> 8) & 0xFF) / 255.0
        let m: Double = Double(rgb & 0xFF) / 255.0
        return Color(red: k, green: y, blue: m)
    }

    /// item_bulletin_card.xml: alt başlık #F1F8E9.
    static let altBaslikRengi: Color = renk(0xF1F8E9)
    /// bg_daily_question_card.xml: 135° çizgisel geçiş, başlangıç #FFB74D, bitiş #FF8F00.
    static let kartGecisBaslangic: Color = renk(0xFFB74D)
    static let kartGecisBitis: Color = renk(0xFF8F00)
    /// item_bulletin_daily_question_card.xml: ünite alt başlığı #FFF3E0.
    static let uniteRengi: Color = renk(0xFFF3E0)
    /// Odak etiketi, geri sayım ve devam paneli mesajı: #FFECB3.
    static let acikSari: Color = renk(0xFFECB3)
    /// Toast zemini.
    static let balonZemini: Color = renk(0x323232)
}

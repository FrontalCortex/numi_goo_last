import SwiftUI

// Keşfet sekmesi (alt çubuğun 6. sekmesi): günlük soru kartı ve diğer kartlar.
// Android karşılığı: TasksFragment + res/layout/fragment_tasks.xml, item_bulletin_card.xml,
// item_bulletin_daily_question_card.xml, panel_daily_question_continue.xml.
// Ölçüler layout XML'lerinden birebir alınmıştır (dp → pt, sp → pt).
struct GunlukSoruEkrani: View {
    /// Günlük soruyu oynatan ekranı (kör ders) açan kapanış. Soru bitince başlangıcın `sonuc`
    /// kapanışı çağrılmalıdır. nil ise ekranın kendi yer tutucusu gösterilir.
    var soruBaslat: ((GunlukSoruBaslangici) -> Void)?
    /// Ödül ekranını (Android: sandık açılışı) açan kapanış. Ekran kapanınca isteğin `kapandi`
    /// kapanışı çağrılmalıdır. nil ise `GunlukSoruOdulEkrani` gösterilir.
    var odulAc: ((GunlukSoruOdulIstegi) -> Void)?
    /// Abaküs / Kupa Yolu / Bize Ulaşın kartlarına dokunulunca çağrılır. nil ise "Yakında" balonu.
    var kartSecildi: ((GunlukSoruKesfetKarti) -> Void)?
    /// Ödülden sonra rozet seviye atlama kuyruğu doluysa çağrılır.
    var rozetGoster: (([String]) -> Void)?

    @StateObject private var model: GunlukSoruModeli
    @Environment(\.scenePhase) private var sahneDurumu

    private let depo: GunlukSoruDeposu
    private let odul: GunlukSoruOdulServisi

    /// Alt çubuk sekmesi olarak gömülen hâl: örnek veriyle çalışır.
    init() {
        self.init(
            soruBaslat: nil,
            odulAc: nil,
            kartSecildi: nil,
            rozetGoster: nil,
            depo: SahteGunlukSoruDeposu.ortak,
            cuzdan: SahteGunlukSoruCuzdani.ortak,
            odul: SahteGunlukSoruOdulServisi()
        )
    }

    init(
        soruBaslat: ((GunlukSoruBaslangici) -> Void)?,
        odulAc: ((GunlukSoruOdulIstegi) -> Void)?,
        kartSecildi: ((GunlukSoruKesfetKarti) -> Void)?,
        rozetGoster: (([String]) -> Void)?,
        depo: GunlukSoruDeposu,
        cuzdan: GunlukSoruCuzdani,
        odul: GunlukSoruOdulServisi
    ) {
        self.soruBaslat = soruBaslat
        self.odulAc = odulAc
        self.kartSecildi = kartSecildi
        self.rozetGoster = rozetGoster
        self.depo = depo
        self.odul = odul
        _model = StateObject(wrappedValue: GunlukSoruModeli(depo: depo, cuzdan: cuzdan, odul: odul))
    }

    var body: some View {
        ZStack {
            liste
            if let istek = model.devamPaneli {
                GunlukSoruDevamKatmani(
                    istek: istek,
                    kapat: { model.devamPaneliniKapat() },
                    devam: { model.anahtarlaDevam() }
                )
                .transition(.opacity)
                .zIndex(1)
            }
            if model.dokunmaKilitli {
                // Android: launch touch blocker — geçiş sürerken dokunuşları yutar.
                Color.black.opacity(0.001)
                    .ignoresSafeArea()
            }
            if let bildirim = model.bildirim {
                VStack {
                    Spacer()
                    GunlukSoruBildirimGorunumu(metin: bildirim.metin)
                }
                .allowsHitTesting(false)
                .transition(.opacity)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color("background_color"))
        .animation(.easeInOut(duration: 0.2), value: model.bildirim)
        .animation(.easeInOut(duration: 0.2), value: model.devamPaneli?.id)
        .onAppear {
            cikislariBagla()
            model.gorundu()
        }
        .onChange(of: sahneDurumu) { yeni in
            if yeni == .active {
                model.kartiYenile()
            }
        }
        .fullScreenCover(item: $model.yerTutucuSoru) { veri in
            GunlukSoruYerTutucuSoru(veri: veri)
        }
        .fullScreenCover(item: $model.yerTutucuOdul) { istek in
            GunlukSoruOdulEkrani(
                periodKey: istek.periodKey,
                rozetKuyrugu: istek.rozetKuyrugu,
                depo: depo,
                odul: odul,
                kapat: { _ in istek.kapandi() }
            )
        }
    }

    private func cikislariBagla() {
        var cikislar = GunlukSoruCikislari()
        cikislar.soruBaslat = soruBaslat
        cikislar.odulAc = odulAc
        cikislar.kartSecildi = kartSecildi
        cikislar.rozetGoster = rozetGoster
        model.cikislar = cikislar
    }

    /// fragment_tasks.xml: RecyclerView — yatay 16dp, üst 8dp, alt 16dp iç boşluk.
    /// Kartların her birinde üst ve alt 10dp dış boşluk vardır (arada toplam 20dp).
    private var liste: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 20) {
                GunlukSoruKesfetKartGorunumu(tur: .abakusPratik) {
                    model.kesfetKartinaDokunuldu(.abakusPratik)
                }
                GunlukSoruKarti(model: model)
                GunlukSoruKesfetKartGorunumu(tur: .kupaYolu) {
                    model.kesfetKartinaDokunuldu(.kupaYolu)
                }
                GunlukSoruKesfetKartGorunumu(tur: .bizeUlasin) {
                    model.kesfetKartinaDokunuldu(.bizeUlasin)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 18)
            .padding(.bottom, 26)
        }
    }
}

// MARK: - Sıradan kart (item_bulletin_card.xml)

/// Sıradan kartların içeriği. Metinler Android'de de koda yazılıdır (strings.xml'de yoktur).
struct GunlukSoruKesfetKartIcerigi {
    let baslik: String
    let altBaslik: String
    let simge: String
    let zemin: Color

    static func al(_ tur: GunlukSoruKesfetKarti) -> GunlukSoruKesfetKartIcerigi {
        switch tur {
        case .abakusPratik:
            return GunlukSoruKesfetKartIcerigi(
                baslik: "Abaküs",
                altBaslik: "Abaküste pratik yaparak kendini geliştir.",
                simge: "abacus_svg_ic",
                zemin: Color("button_enabled")
            )
        case .kupaYolu:
            return GunlukSoruKesfetKartIcerigi(
                baslik: "Kupa Yolu",
                altBaslik: "Başarılarını kupalarla ölç, seviyeni yükselt ve yeni zorluklara ilerle.",
                simge: "infinity_cup_ic",
                zemin: Color("lesson_header_yellow")
            )
        case .bizeUlasin:
            return GunlukSoruKesfetKartIcerigi(
                baslik: "Bize Ulaşın",
                altBaslik: "Bir sorun mu yaşadınız? Görüşlerinizi ve önerilerinizi bizimle paylaşın.",
                simge: "feedback_ic",
                // android.R.color.holo_blue_dark = #0099CC
                zemin: GunlukSoruStil.holoKoyuMavi
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
    /// android.R.color.holo_blue_dark.
    static let holoKoyuMavi: Color = renk(0x0099CC)
    /// bg_daily_question_card.xml: 135° çizgisel geçiş, başlangıç #FFB74D, bitiş #FF8F00.
    static let kartGecisBaslangic: Color = renk(0xFFB74D)
    static let kartGecisBitis: Color = renk(0xFF8F00)
    /// item_bulletin_daily_question_card.xml: ünite alt başlığı #FFF3E0.
    static let uniteRengi: Color = renk(0xFFF3E0)
    /// Odak etiketi, geri sayım ve devam paneli mesajı: #FFECB3.
    static let acikSari: Color = renk(0xFFECB3)
    /// Toast zemini.
    static let balonZemini: Color = renk(0x323232)
    /// fragment_chest.xml: zemin #101820, yazı ve düğme #FFD600.
    static let odulZemini: Color = renk(0x101820)
    static let odulSarisi: Color = renk(0xFFD600)
}

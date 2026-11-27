import SwiftUI

// Görevler sekmesi (alt çubuğun 3. sekmesi).
// Android karşılığı: MissionsFragment + res/layout/fragment_missions.xml.
// Ölçüler layout XML'inden birebir alınmıştır (dp → pt, sp → pt).
struct GorevlerEkrani: View {
    /// Ödül sandığı ekranını açan kapanış. nil ise ekranın kendi yer tutucusu gösterilir.
    /// Sandık kapanınca isteğin `kapandi` kapanışı çağrılmalıdır.
    var sandikAc: ((GorevlerSandikIstegi) -> Void)?

    @StateObject private var model: GorevlerModeli
    @Environment(\.scenePhase) private var sahneDurumu

    /// Dakikada bir yenileme: geri sayım saatleri ve gün/hafta devri ekrandayken de güncellensin.
    private let saat = Timer.publish(every: 60, on: .main, in: .common).autoconnect()

    /// Alt çubuk sekmesi olarak gömülen hâl: örnek veriyle çalışır.
    init() {
        self.init(
            sandikAc: nil,
            veri: SahteGorevlerVeriKaynagi.ortak,
            odul: SahteGorevlerOdulServisi()
        )
    }

    init(
        sandikAc: ((GorevlerSandikIstegi) -> Void)?,
        veri: GorevlerVeriKaynagi,
        odul: GorevlerOdulServisi
    ) {
        self.sandikAc = sandikAc
        _model = StateObject(wrappedValue: GorevlerModeli(veri: veri, odul: odul))
    }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 20) {
                GorevlerBolumu(
                    baslik: Metin.al("missions_weekly_title"),
                    sayac: model.haftalikSayac,
                    satirlar: model.haftalik,
                    dokunuldu: satiraDokun
                )
                GorevlerBolumu(
                    baslik: Metin.al("missions_daily_title"),
                    sayac: model.gunlukSayac,
                    satirlar: model.gunluk,
                    dokunuldu: satiraDokun
                )
            }
            .padding(16)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color("background_color"))
        .onAppear {
            model.yenile()
        }
        .onReceive(saat) { _ in
            model.yenile()
        }
        .onChange(of: sahneDurumu) { yeni in
            if yeni == .active {
                model.yenile()
            }
        }
        .fullScreenCover(item: $model.yerTutucuIstegi) { istek in
            GorevlerSandikYerTutucu(istek: istek)
        }
    }

    private func satiraDokun(_ satir: GorevlerSatiri) {
        model.satiraDokunuldu(satir, disariAc: sandikAc)
    }
}

// MARK: - Bölüm çerçevesi

/// "HAFTALIK GÖREVLER" / "GÜNLÜK GÖREVLER" çerçevesi (bg_mission_section_frame.xml:
/// zemin #1A262B, 2dp #4D6673 çizgi, 16dp köşe; iç boşluk 12dp).
struct GorevlerBolumu: View {
    let baslik: String
    let sayac: String
    let satirlar: [GorevlerSatiri]
    let dokunuldu: (GorevlerSatiri) -> Void

    var body: some View {
        VStack(spacing: 0) {
            GorevlerBolumBasligi(baslik: baslik, sayac: sayac)
            // Başlık altı çizgisi: 1dp, #20FFFFFF, alt boşluk 4dp.
            GorevlerStil.baslikCizgisi
                .frame(height: 1)
                .padding(.bottom, 4)
            ForEach(Array(satirlar.enumerated()), id: \.element.id) { sira, satir in
                GorevlerSatirGorunumu(satir: satir, dokunuldu: dokunuldu)
                if sira < satirlar.count - 1 {
                    GorevlerSatirAyirici()
                }
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(GorevlerStil.cerceveZemini)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .strokeBorder(GorevlerStil.cerceveCizgisi, lineWidth: 2)
        )
    }
}

/// Bölüm başlığı: ad + saat simgesi + geri sayım (13sp, missions_header_muted).
struct GorevlerBolumBasligi: View {
    let baslik: String
    let sayac: String

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            Text(baslik.uppercased(with: Locale(identifier: "tr")))
                .font(Tema.yazi(13))
                .foregroundColor(Color("missions_header_muted"))
                .frame(maxWidth: .infinity, alignment: .leading)
            // Android: @android:drawable/ic_menu_recent_history (sistem simgesi), 16dp.
            Image(systemName: "clock.arrow.circlepath")
                .resizable()
                .scaledToFit()
                .frame(width: 16, height: 16)
                .foregroundColor(Color("missions_header_muted"))
                .padding(.trailing, 4)
            Text(sayac)
                .font(Tema.yazi(13))
                .foregroundColor(Color("missions_header_muted"))
        }
        .padding(.leading, 4)
        .padding(.top, 4)
        .padding(.trailing, 4)
        .padding(.bottom, 8)
    }
}

/// Satırlar arası çizgi. Android'de kodla eklenir: yükseklik 1 px, üst/alt boşluk 6 px, #1AFFFFFF.
struct GorevlerSatirAyirici: View {
    var body: some View {
        let olcek: CGFloat = max(UIScreen.main.scale, 1)
        let kalinlik: CGFloat = 1 / olcek
        let bosluk: CGFloat = 6 / olcek
        return GorevlerStil.satirCizgisi
            .frame(height: kalinlik)
            .padding(.vertical, bosluk)
    }
}

// MARK: - Görev satırı

/// Tek görev satırı (item_mission_quest.xml).
struct GorevlerSatirGorunumu: View {
    let satir: GorevlerSatiri
    let dokunuldu: (GorevlerSatiri) -> Void

    var body: some View {
        Button(action: { dokunuldu(satir) }) {
            icerik
        }
        .buttonStyle(GorevlerSatirBasmaStili())
        // Android: yalnızca ödül alınabilirken tıklanır.
        .disabled(!satir.alinabilir)
    }

    private var icerik: some View {
        HStack(alignment: .bottom, spacing: 0) {
            VStack(alignment: .leading, spacing: 8) {
                Text(Metin.al(satir.baslikAdi))
                    .font(Tema.yazi(20))
                    .foregroundColor(baslikRengi)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)
                GorevlerIlerlemeCubugu(satir: satir, etiket: etiket, etiketRengi: etiketRengi)
            }
            Image(satir.simge)
                .resizable()
                .scaledToFit()
                .frame(width: 48, height: 48)
                .padding(.leading, 10)
        }
        .padding(.leading, 16)
        .padding(.top, 10)
        .padding(.trailing, 12)
        .padding(.bottom, 10)
        .contentShape(Rectangle())
    }

    /// Android createQuestItemView: bitmiş ve ödülü bekleyen görevin başlığı altın.
    private var baslikRengi: Color {
        switch satir.durum {
        case .bitti: return Color("missions_progress_complete")
        case .alindi, .suruyor: return Color("missions_quest_title_normal")
        }
    }

    private var etiket: String {
        switch satir.durum {
        case .alindi: return Metin.al("mission_reward_claimed_label")
        case .bitti: return Metin.al("mission_completed_label")
        case .suruyor: return Metin.al("mission_progress_format", satir.ilerleme, satir.hedef)
        }
    }

    private var etiketRengi: Color {
        switch satir.durum {
        case .alindi: return Color("black")
        case .bitti: return Color("background_color")
        // Android'de de aynı: beyaz yazı + koyu gölge (mavi dolgu ve parlama üstünde okunsun diye).
        case .suruyor: return Color.white
        }
    }
}

/// Android `?attr/selectableItemBackground` karşılığı: basılıyken hafif aydınlanma.
struct GorevlerSatirBasmaStili: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.white.opacity(configuration.isPressed ? 0.12 : 0))
            )
    }
}

// MARK: - İlerleme çubuğu

/// 32dp yüksekliğinde ilerleme çubuğu: zemin (missions_track, 16dp köşe), dolgu, üstte ince
/// parlama şeridi (6dp, üstten 8dp, soldan 14dp, 3dp köşe) ve ortada etiket (18sp).
/// Dolgu ve parlama genişlikleri ortak koddan gelir (MissionUiLogic.missionProgressOverlay).
struct GorevlerIlerlemeCubugu: View {
    let satir: GorevlerSatiri
    let etiket: String
    let etiketRengi: Color

    var body: some View {
        GeometryReader { alan in
            cubuk(genislik: alan.size.width)
        }
        .frame(height: 32)
    }

    private func cubuk(genislik: CGFloat) -> some View {
        let olcu: GorevlerCubukOlcusu? = GorevlerCubukHesabi.hesapla(genislik: genislik, satir: satir)
        return ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 16)
                .fill(Color("missions_track"))
                .frame(width: genislik, height: 32)
            if let o = olcu, o.dolguGorunur {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(o.dolguRengi))
                    .frame(width: o.dolguGenisligi, height: 32)
            }
            if let o = olcu, o.parlamaGorunur {
                RoundedRectangle(cornerRadius: 3)
                    .fill(Color(o.parlamaRengi))
                    .frame(width: o.parlamaGenisligi, height: 6)
                    .offset(x: 14, y: 8)
            }
            Text(etiket)
                .font(Tema.yazi(18))
                .foregroundColor(etiketRengi)
                // Yalnızca süren görevde (beyaz sayı) koyu gölge; bitmiş/alınmış etiketler koyu renklidir.
                .shadow(color: Color.black.opacity(satir.durum == .suruyor ? 0.9 : 0), radius: 2, x: 0, y: 1)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .frame(width: genislik, height: 32, alignment: .center)
        }
    }
}

// MARK: - Sabit renkler

enum GorevlerStil {
    /// bg_mission_section_frame.xml: zemin #1A262B.
    static let cerceveZemini = Color(red: 26.0 / 255.0, green: 38.0 / 255.0, blue: 43.0 / 255.0)
    /// bg_mission_section_frame.xml: çizgi #4D6673.
    static let cerceveCizgisi = Color(red: 77.0 / 255.0, green: 102.0 / 255.0, blue: 115.0 / 255.0)
    /// fragment_missions.xml: başlık altı çizgisi #20FFFFFF (alfa 0x20).
    static let baslikCizgisi = Color.white.opacity(32.0 / 255.0)
    /// MissionsFragment.addDivider: 0x1AFFFFFF (alfa 0x1A).
    static let satirCizgisi = Color.white.opacity(26.0 / 255.0)
    /// fragment_chest.xml / yer tutucu zemini: #101820.
    static let sandikZemini = Color(red: 16.0 / 255.0, green: 24.0 / 255.0, blue: 32.0 / 255.0)
}

// MARK: - Sandık yer tutucusu

/// Sandık açma ekranı (Android: NewChestFragment) bağlanana dek gösterilen yer tutucu:
/// kapalı sandık, görev adı ve "Devam et" düğmesi. Düğmeye basınca ödül alınmış sayılır.
struct GorevlerSandikYerTutucu: View {
    let istek: GorevlerSandikIstegi

    var body: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(istek.simge)
                .resizable()
                .scaledToFit()
                .frame(width: 200, height: 200)
            Text(istek.gorevBasligi)
                .font(Tema.yazi(20))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
            Text(Metin.al("mission_chest_toast"))
                .font(Tema.yazi(16))
                .foregroundColor(Color("missions_header_muted"))
            Spacer()
            Button(action: istek.kapandi) {
                Text(Metin.al("mission_continue"))
                    .font(Tema.yazi(20))
                    .foregroundColor(GorevlerStil.sandikZemini)
                    .padding(.horizontal, 32)
                    .frame(height: 52)
                    .background(
                        RoundedRectangle(cornerRadius: 15)
                            .fill(Color("missions_progress_complete"))
                    )
            }
            .buttonStyle(.plain)
            .padding(.bottom, 48)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(GorevlerStil.sandikZemini.ignoresSafeArea())
        .preferredColorScheme(.dark)
    }
}

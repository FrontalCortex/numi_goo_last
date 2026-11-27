import SwiftUI

// Sandık ekranı: üç dokunuşta nadirlik yükselir, dördüncüde sandık açılır, ödül gösterilir,
// son dokunuşta kapanır.
//
// Android kaynakları: NewChestFragment.kt + fragment_new_chest.xml (görünen ekran),
// ChestSoundPlayer.kt (sesler), ChestFragment.kt (görünmez sarmalayıcı).
//
// Bu ekran yalnızca sandığı oynatır. Android'de ChestFragment'in sandık kapanınca yaptığı işler
// (harita ilerlemesi, görev sayaçları, rozetler, başarı oranı kayıtları, sonraki ekran kararı)
// ekranı açan yöneticiye aittir; kuralları ortak koddadır: ChestFragmentLogic.updateMapProgress,
// changeCupIcon, shouldIncrementDartProgress, isLastChestOfCupPathPart, … (ders paketi).
// Giriş yapmamış ilk öğretici kullanıcısı için sandık HİÇ açılmamalıdır
// (ChestFragmentLogic.shouldRedirectToLoginForFirstTutorial).

struct SandikEkrani: View {
    @StateObject private var model: SandikModeli

    /// - Parameters:
    ///   - girdi: başlangıç nadirliği ve (varsa) reklam değeri.
    ///   - kapat: ekran kapanırken kazanılan ödülle çağrılır.
    init(girdi: SandikGirdisi, kapat: @escaping (SandikSonucu) -> Void) {
        self.init(
            girdi: girdi,
            odulServisi: SahteSandikOdulServisi(),
            ses: SahteSandikSesCalar(),
            kapat: kapat
        )
    }

    /// Sunucu ve ses uygulamaları dışarıdan verilen biçim.
    init(
        girdi: SandikGirdisi,
        odulServisi: SandikOdulServisi,
        ses: SandikSesCalar,
        kapat: @escaping (SandikSonucu) -> Void
    ) {
        _model = StateObject(
            wrappedValue: SandikModeli(girdi: girdi, odulServisi: odulServisi, ses: ses, kapat: kapat)
        )
    }

    var body: some View {
        ZStack {
            model.arkaPlan.ignoresSafeArea()
            SandikSahnesi(model: model)
                .modifier(SandikAcilisKaymasi(acildi: model.asama != .kapali))
            // chestOverlay: açılışta uygulamanın olağan arka plan rengi sahnenin üstüne gelir.
            Color("background_color")
                .ignoresSafeArea()
                .opacity(model.asama == .kapali ? 0 : 1)
                .animation(.easeInOut(duration: 0.7), value: model.asama)
            SandikOdulGorunumu(model: model)
        }
        .contentShape(Rectangle())
        .onTapGesture { model.dokunuldu() }
        .onAppear { model.basla() }
        .onDisappear { model.durdur() }
    }
}

/// openChest: sahnedeki her şey 700 ms'de 500 piksel aşağı kayar ve kaybolur.
struct SandikAcilisKaymasi: ViewModifier {
    let acildi: Bool

    func body(content: Content) -> some View {
        content
            .offset(y: acildi ? SandikStil.pikseldenNokta(500) : 0)
            .opacity(acildi ? 0 : 1)
            .animation(.easeInOut(duration: 0.7), value: acildi)
    }
}

// MARK: - Kapalı sandık sahnesi

/// fragment_new_chest.xml: etiket – sandık – adım göstergesi dikeyde ortalanmış bir zincirdir
/// (üst kenar ile ipucu metninin üstü arasında); ipucu alttan 120 pt yukarıdadır.
struct SandikSahnesi: View {
    @ObservedObject var model: SandikModeli

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            Text(model.etiket)
                .font(Tema.yazi(28))
                .foregroundColor(model.etiketRengi)
                .padding(.bottom, 20)
            SandikGovdesi(model: model)
            SandikAdimlari(model: model)
                .padding(.top, 50)
            Spacer(minLength: 0)
            Text(model.ipucuMetni)
                .font(Tema.yazi(17))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
                .frame(maxWidth: .infinity)
                .opacity(model.ipucuGorunur ? 1 : 0)
                .animation(.linear(duration: 0.3), value: model.ipucuGorunur)
        }
        .padding(.bottom, 120)
    }
}

/// Platform (210×140, sandığın alt kenarına hizalı), sandık görseli (250×250), yükleniyor
/// göstergesi ve yıldız katmanı.
struct SandikGovdesi: View {
    @ObservedObject var model: SandikModeli
    @State private var olcek: CGFloat = 1.0

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 16)
                .fill(model.platformRengi)
                .frame(width: 210, height: 140)
                // Android platformu kendi merkezinden ölçekler: önce ölçek, sonra kaydırma.
                .scaleEffect(olcek)
                .offset(y: 55)
            sandik
            if model.yukleniyor {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    .scaleEffect(1.5)
                    .frame(width: 48, height: 48)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 250)
        .overlay(SandikYildizKatmani(model: model))
        .onChange(of: model.ziplamaSayaci) { _ in
            zipla()
        }
    }

    /// Sandık görseli; boştayken `bosBaslangic` anından itibaren döngüyle zıplar.
    private var sandik: some View {
        TimelineView(.animation) { zaman in
            let hareket: (CGFloat, Double) = bosHareket(simdi: zaman.date)
            Image(model.sandikGorseli)
                .resizable()
                .scaledToFit()
                .frame(width: 250, height: 250)
                .scaleEffect(olcek)
                .rotationEffect(.degrees(hareket.1))
                .offset(y: hareket.0)
        }
        .frame(width: 250, height: 250)
    }

    private func bosHareket(simdi: Date) -> (CGFloat, Double) {
        guard let baslangic = model.bosBaslangic else { return (0, 0) }
        // `bosBaslangic` sistem çalışma süresi cinsindendir; şimdiki değerle farkı alınır.
        let gecen: Double = max(0, ProcessInfo.processInfo.systemUptime - baslangic)
        return SandikStil.bosHareket(nadirlikSirasi: model.nadirlikSirasi, gecen: gecen)
    }

    /// playBounceAnimation: 130 ms'de 1.18'e büyür, 200 ms'de taşarak (overshoot) geri döner.
    private func zipla() {
        withAnimation(.easeInOut(duration: 0.13)) {
            olcek = 1.18
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.13) {
            withAnimation(.spring(response: 0.2, dampingFraction: 0.4)) {
                olcek = 1.0
            }
        }
    }
}

/// Üç adım dairesi (32 pt, aralarında 12 pt). Etkin olan yukarı aşağı zıplar.
struct SandikAdimlari: View {
    @ObservedObject var model: SandikModeli

    var body: some View {
        TimelineView(.animation) { _ in
            let ziplama: CGFloat = SandikStil.adimZiplamasi(simdi: ProcessInfo.processInfo.systemUptime)
            HStack(spacing: 12) {
                daire(0, ziplama: ziplama)
                daire(1, ziplama: ziplama)
                daire(2, ziplama: ziplama)
            }
        }
        .frame(height: 32)
    }

    private func daire(_ sira: Int, ziplama: CGFloat) -> some View {
        let durum: SandikAdimDurumu = sira < model.adimlar.count ? model.adimlar[sira] : .gelecek
        let renk: Color
        switch durum {
        case .tamam:
            renk = SandikStil.adimTamamRengi
        case .etkin:
            renk = model.adimEtkinRengi
        case .gelecek:
            renk = model.adimGelecekRengi
        }
        let kayma: CGFloat = durum == .etkin ? ziplama : 0
        return Circle()
            .fill(renk)
            .frame(width: 32, height: 32)
            .offset(y: kayma)
    }
}

/// Sandığın çevresinde yukarı süzülen yıldızlar. Katman, sandık satırının üstüne oturur
/// (tam genişlik × 250 pt); yıldızlar sınırların dışına taşabilir.
struct SandikYildizKatmani: View {
    @ObservedObject var model: SandikModeli

    var body: some View {
        GeometryReader { geo in
            TimelineView(.animation) { _ in
                let simdi: TimeInterval = ProcessInfo.processInfo.systemUptime
                ZStack(alignment: .topLeading) {
                    ForEach(model.yildizlar) { yildiz in
                        yildizGorunumu(yildiz, simdi: simdi, genislik: geo.size.width)
                    }
                }
                .frame(width: geo.size.width, height: geo.size.height, alignment: .topLeading)
            }
        }
        .allowsHitTesting(false)
    }

    private func yildizGorunumu(_ yildiz: SandikYildizi, simdi: TimeInterval, genislik: CGFloat) -> some View {
        let ham: Double = yildiz.sure > 0 ? (simdi - yildiz.dogum) / yildiz.sure : 1
        let oran: Double = min(1, max(0, ham))
        // Saydamlık: 0 → 1 (%30) → 1 (%70) → 0
        let saydamlikKareleri: [(Double, Double)] = [(0, 0), (0.3, 1), (0.7, 1), (1, 0)]
        let saydamlik: Double = SandikStil.araKare(saydamlikKareleri, oran)
        let bosluk: CGFloat = max(0, genislik - yildiz.boy)
        let x: CGFloat = yildiz.yatayOran * bosluk + yildiz.boy / 2
        let y: CGFloat = yildiz.baslangicY - yildiz.yukselis * CGFloat(oran) + yildiz.boy / 2
        return Image(yildiz.gorsel)
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .foregroundColor(model.yildizRengi)
            .frame(width: yildiz.boy, height: yildiz.boy)
            .opacity(saydamlik)
            .position(x: x, y: y)
    }
}

// MARK: - Ödül

/// showReward: büyük ödül görseli (240×240, dikeyde %45) 500 ms'de taşarak büyür; altında
/// (160 pt sonra) küçük simgeyle birlikte miktar yazısı belirir.
struct SandikOdulGorunumu: View {
    @ObservedObject var model: SandikModeli

    var body: some View {
        GeometryReader { geo in
            let gorunur: Bool = model.asama == .odul
            let gorselBoyu: CGFloat = 240
            let ustBosluk: CGFloat = max(0, geo.size.height - gorselBoyu) * 0.45
            let gorselMerkezi: CGFloat = ustBosluk + gorselBoyu / 2
            let yaziMerkezi: CGFloat = ustBosluk + gorselBoyu + 160 + 20
            ZStack(alignment: .topLeading) {
                Image(model.odulBuyukGorsel)
                    .resizable()
                    .scaledToFit()
                    .frame(width: gorselBoyu, height: gorselBoyu)
                    .scaleEffect(gorunur ? 1 : 0.001)
                    .opacity(gorunur ? 1 : 0)
                    .position(x: geo.size.width / 2, y: gorselMerkezi)
                odulYazisi
                    .opacity(gorunur ? 1 : 0)
                    .position(x: geo.size.width / 2, y: yaziMerkezi)
            }
            .animation(.spring(response: 0.5, dampingFraction: 0.55), value: gorunur)
        }
        .allowsHitTesting(false)
    }

    /// chestRewardText: drawableStart kendi boyutunda çizilir (gold_ic / key vektörleri 36dp),
    /// drawablePadding 8dp.
    private var odulYazisi: some View {
        HStack(spacing: 8) {
            Image(model.odulKucukGorsel)
                .resizable()
                .scaledToFit()
                .frame(width: 36, height: 36)
            Text(model.odulMetni)
                .font(Tema.yazi(28))
                .foregroundColor(.white)
        }
    }
}

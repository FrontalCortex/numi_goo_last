import AVFoundation
import SwiftUI

// Kristal kırma ekranı: tam ekran video üç yerde durur ve dokunuş bekler; ilk iki durakta
// nadirlik etiketi değişebilir. Video bitince ekran kendiliğinden kapanır.
//
// Android kaynakları: CrystalBreakVideoFragment.kt + fragment_crystal_break_video.xml.
// Android'de bu ekranı DailyQuestionRewardFragment ve MissionRewardRevealDialogFragment açar
// (ders sonrası sandık akışı açmaz); oynatılacak video ve ödül sunucudan gelir
// (ServerRewards.CrystalOutcome, Cloud Function `openCrystalReward`). Ödülün gösterimi bu
// ekranın işi değildir; kapanınca çağıran ekran gösterir (ChestCrystalPolicy.outcomeFromServer).

/// Kristal ekranına aktarılan değerler (Android: CrystalBreakVideoFragment.newInstance).
struct SandikKristalGirdisi {
    /// Oynatılacak videonun adı (ör. "crystal_red_yellow"); `nil` ya da boşsa "crystal_blue_blue".
    var videoAdi: String? = nil

    init(videoAdi: String? = nil) {
        self.videoAdi = videoAdi
    }
}

struct SandikKristalEkrani: View {
    @StateObject private var model: SandikKristalModeli

    /// - Parameters:
    ///   - girdi: oynatılacak video.
    ///   - kapat: video bitince çağrılır.
    init(girdi: SandikKristalGirdisi, kapat: @escaping () -> Void) {
        self.init(girdi: girdi, ses: SahteSandikSesCalar(), kapat: kapat)
    }

    /// Ses uygulaması dışarıdan verilen biçim.
    init(girdi: SandikKristalGirdisi, ses: SandikSesCalar, kapat: @escaping () -> Void) {
        _model = StateObject(
            wrappedValue: SandikKristalModeli(videoAdi: girdi.videoAdi, ses: ses, kapat: kapat)
        )
    }

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .topLeading) {
                etiket
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.top, 28)
                SandikKristalEfektleri(model: model)
            }
            .frame(width: geo.size.width, height: geo.size.height)
            .contentShape(Rectangle())
            .gesture(
                // Dokunulan yer gerektiği için sürükleme hareketi kullanılır (mesafe 0 = dokunuş).
                DragGesture(minimumDistance: 0, coordinateSpace: .local)
                    .onEnded { deger in
                        model.dokunuldu(deger.location)
                    }
            )
            .onAppear {
                model.ekranBoyunuAyarla(geo.size)
                model.basla()
            }
            .onChange(of: geo.size) { yeni in
                model.ekranBoyunuAyarla(yeni)
            }
        }
        .background(arkaKatman.ignoresSafeArea())
        .onDisappear { model.durdur() }
    }

    /// Siyah zemin, video ve karartma. Android'de pencere tam ekrandır (durum çubuğu gizli) ve
    /// video ekranın tamamını kırparak doldurur; burada da güvenli alanın dışına taşar.
    private var arkaKatman: some View {
        ZStack {
            Color.black
            SandikKristalVideosu(model: model)
            // crystalVideoDimOverlay: #58000000
            Color.black.opacity(Double(0x58) / 255.0)
        }
        .allowsHitTesting(false)
    }

    /// crystalRarityText: 34 sp, gölgeli; video oynamaya başlayınca görünür.
    private var etiket: some View {
        Text(model.etiket)
            .font(Tema.yazi(34))
            .foregroundColor(model.etiketRengi)
            .shadow(color: Color.black.opacity(0.6), radius: 3, x: 0, y: 2)
            .opacity(model.etiketGorunur ? 1 : 0)
            .allowsHitTesting(false)
    }
}

/// Video (dosya pakette yoksa yer tutucu) ve üzerindeki ölçek animasyonları: durakta nabız,
/// dalgada sarsıntı.
struct SandikKristalVideosu: View {
    @ObservedObject var model: SandikKristalModeli

    var body: some View {
        TimelineView(.animation) { _ in
            let simdi: TimeInterval = ProcessInfo.processInfo.systemUptime
            let olcek: CGSize = olcekHesapla(simdi: simdi)
            icerik
                .scaleEffect(x: olcek.width, y: olcek.height)
        }
    }

    /// Dosya pakette varsa AVPlayer katmanı, yoksa (ya da açılamadıysa) durağan görsel.
    @ViewBuilder
    private var icerik: some View {
        if model.gercekVideo, let oynatici = model.videoOynaticisi {
            SandikVideoGorunumu(oynatici: oynatici)
        } else {
            SandikVideoYerTutucu(ad: model.videoAdi)
        }
    }

    private func olcekHesapla(simdi: TimeInterval) -> CGSize {
        if let sarsinti = model.sarsintiBaslangici {
            // animateVideoShockwave: 420 ms, yatay ve dikey ayrı anahtar kareler.
            let oran: Double = min(1, max(0, (simdi - sarsinti) / 0.42))
            let egri: Double = SandikStil.hizlanYavasla(oran)
            let yatayKareler: [(Double, Double)] = [
                (0, 1), (0.2, 1.04), (0.4, 0.972), (0.6, 1.028), (0.8, 0.986), (1, 1)
            ]
            let dikeyKareler: [(Double, Double)] = [
                (0, 1), (0.2, 1.052), (0.4, 0.964), (0.6, 1.034), (0.8, 0.982), (1, 1)
            ]
            let yatay: Double = SandikStil.araKare(yatayKareler, egri)
            let dikey: Double = SandikStil.araKare(dikeyKareler, egri)
            return CGSize(width: yatay, height: dikey)
        }
        if let nabiz = model.nabizBaslangici {
            // maybeStartIdlePulse: 480 ms'de 1 → 1.035, geri dönerek sonsuz.
            let yarim: Double = 0.48
            let konum: Double = (simdi - nabiz).truncatingRemainder(dividingBy: yarim * 2)
            let ham: Double = konum < yarim ? konum / yarim : 2.0 - konum / yarim
            let deger: Double = 1.0 + 0.035 * SandikStil.hizlanYavasla(max(0, ham))
            return CGSize(width: deger, height: deger)
        }
        return CGSize(width: 1, height: 1)
    }
}

/// Dalgalar ve saçılan yıldızlar.
struct SandikKristalEfektleri: View {
    @ObservedObject var model: SandikKristalModeli

    var body: some View {
        TimelineView(.animation) { _ in
            let simdi: TimeInterval = ProcessInfo.processInfo.systemUptime
            ZStack(alignment: .topLeading) {
                ForEach(model.dalgalar) { dalga in
                    parilti(dalga, simdi: simdi)
                    halka(dalga, simdi: simdi)
                }
                ForEach(model.yildizlar) { yildiz in
                    yildizGorunumu(yildiz, simdi: simdi)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
        .allowsHitTesting(false)
    }

    /// Halka: dokunmada 78 pt / 4 pt çizgi, 0.55 → 3.2; büyük dalgada 86 pt / 5 pt çizgi, 0.8 → 8.8.
    /// 340 ms'de büyür; saydamlık 80 (70) ms'de belirir, 260 (270) ms'de söner.
    private func halka(_ dalga: SandikKristalDalgasi, simdi: TimeInterval) -> some View {
        let gecen: Double = simdi - dalga.dogum
        let cap: CGFloat = dalga.buyuk ? 86 : 78
        let cizgi: CGFloat = dalga.buyuk ? 5 : 4
        let ilkOlcek: Double = dalga.buyuk ? 0.8 : 0.55
        let sonOlcek: Double = dalga.buyuk ? 8.8 : 3.2
        let belirme: Double = dalga.buyuk ? 0.07 : 0.08
        let sonme: Double = dalga.buyuk ? 0.27 : 0.26
        let olcek: Double = olcekDegeri(gecen: gecen, sure: 0.34, ilk: ilkOlcek, son: sonOlcek)
        let saydamlik: Double = saydamlikDegeri(gecen: gecen, belirme: belirme, sonme: sonme, tepe: 1.0)
        return Circle()
            .strokeBorder(dalga.renk, lineWidth: cizgi)
            .frame(width: cap, height: cap)
            .scaleEffect(CGFloat(olcek))
            .opacity(saydamlik)
            .position(dalga.merkez)
    }

    /// Parıltı: dokunmada 132 pt, 0.5 → 2.8; büyük dalgada 150 pt, 0.75 → 7.8. 360 ms'de büyür.
    private func parilti(_ dalga: SandikKristalDalgasi, simdi: TimeInterval) -> some View {
        let gecen: Double = simdi - dalga.dogum
        let cap: CGFloat = dalga.buyuk ? 150 : 132
        let ilkOlcek: Double = dalga.buyuk ? 0.75 : 0.5
        let sonOlcek: Double = dalga.buyuk ? 7.8 : 2.8
        let tepe: Double = dalga.buyuk ? 1.0 : 0.95
        let olcek: Double = olcekDegeri(gecen: gecen, sure: 0.36, ilk: ilkOlcek, son: sonOlcek)
        let saydamlik: Double = saydamlikDegeri(gecen: gecen, belirme: 0.07, sonme: 0.30, tepe: tepe)
        // bg_tap_wave_glow.xml: ortası ve kenarı saydam, arası yarı saydam dairesel geçiş.
        let ortaSaydamlik: Double = dalga.buyuk ? 0.8 : 0.4
        let gecis = RadialGradient(
            gradient: Gradient(colors: [
                dalga.renk.opacity(0),
                dalga.renk.opacity(ortaSaydamlik),
                dalga.renk.opacity(0)
            ]),
            center: .center,
            startRadius: 0,
            endRadius: cap / 2
        )
        return Circle()
            .fill(gecis)
            .frame(width: cap, height: cap)
            .scaleEffect(CGFloat(olcek))
            .opacity(saydamlik)
            .position(dalga.merkez)
    }

    /// Yıldız: 50 ms'de belirir, sonra `sure` boyunca dışarı uçup dönerek söner.
    private func yildizGorunumu(_ yildiz: SandikKristalYildizi, simdi: TimeInterval) -> some View {
        let gecen: Double = simdi - yildiz.dogum
        let belirme: Double = 0.05
        var saydamlik: Double = 0
        var ilerleme: Double = 0
        if gecen < belirme {
            saydamlik = max(0, gecen / belirme)
        } else {
            let ham: Double = yildiz.sure > 0 ? (gecen - belirme) / yildiz.sure : 1
            ilerleme = SandikStil.hizlanYavasla(min(1, max(0, ham)))
            saydamlik = 1.0 - ilerleme
        }
        let x: CGFloat = yildiz.baslangic.x + yildiz.kayma.width * CGFloat(ilerleme)
        let y: CGFloat = yildiz.baslangic.y + yildiz.kayma.height * CGFloat(ilerleme)
        let donme: Double = yildiz.ilkDonme + yildiz.ekDonme * ilerleme
        return Image("ic_tap_spark_star")
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .foregroundColor(yildiz.renk)
            .frame(width: yildiz.boy, height: yildiz.boy)
            .rotationEffect(.degrees(donme))
            .opacity(saydamlik)
            .position(x: x, y: y)
    }

    private func olcekDegeri(gecen: Double, sure: Double, ilk: Double, son: Double) -> Double {
        let ham: Double = min(1, max(0, gecen / sure))
        let egri: Double = SandikStil.hizlanYavasla(ham)
        return ilk + (son - ilk) * egri
    }

    private func saydamlikDegeri(gecen: Double, belirme: Double, sonme: Double, tepe: Double) -> Double {
        if gecen < 0 { return 0 }
        if gecen < belirme {
            return tepe * (gecen / belirme)
        }
        let kalan: Double = (gecen - belirme) / sonme
        return tepe * max(0, 1.0 - kalan)
    }
}

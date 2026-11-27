import SwiftUI

// Günlük soru ödül ekranı.
// Android karşılığı: DailyQuestionRewardFragment + res/layout/fragment_chest.xml.
// Akış: ekran açılır açılmaz sunucudan kristal ödülü istenir (ödül sunucuda verilir ve bakiye
// orada yazılır) → kristal kırılma videosu → ödül görseli, miktarı ve "Ödülü Al" düğmesi.
//
// Not: Android'in güncel akışında TasksFragment bu fragment yerine sandık açılışını
// (NewChestFragment, RARE) kullanır; DailyQuestionRewardFragment kodda durur ama çağrılmaz.
// GunlukSoruEkrani, dışarıdan bir ödül ekranı bağlanmadığında bu ekranı gösterir.

/// Ödül ekranının görünüm modeli.
final class GunlukSoruOdulModeli: ObservableObject {
    enum Asama: Equatable {
        /// Sunucudan sonuç bekleniyor (Android: prepareHiddenRewardUi — her şey gizli).
        case bekliyor
        /// Kristal kırılıyor (Android: CrystalBreakVideoFragment).
        case kristal
        /// Ödül gösteriliyor (Android: revealChestRewardUi).
        case odul
    }

    @Published private(set) var asama: Asama = .bekliyor
    /// fragment_chest.xml varsayılanı: open_chest.
    @Published private(set) var simge: String = "open_chest"
    @Published private(set) var etiket: String = ""
    @Published private(set) var hata: String? = nil
    /// Sunucunun seçtiği kristal videosunun adı (video oynatıcı bağlanınca kullanılır).
    private(set) var videoAdi: String = ""

    private let periodKey: String
    private let depo: GunlukSoruDeposu
    private let odul: GunlukSoruOdulServisi
    private var basladi = false
    /// Android: claimRewardInProgress.
    private var aliniyor = false

    init(periodKey: String, depo: GunlukSoruDeposu, odul: GunlukSoruOdulServisi) {
        self.periodKey = periodKey
        self.depo = depo
        self.odul = odul
    }

    /// Android: onViewCreated → ServerRewards.openCrystal.
    func basla(hataKapat: @escaping () -> Void) {
        if basladi {
            return
        }
        basladi = true
        odul.kristalAc { [weak self] sonuc in
            guard let ben = self else { return }
            guard let gelen = sonuc else {
                // Android: Toast "Ödül alınamadı. Tekrar deneyin." ve ekran kapanır.
                ben.hata = "Ödül alınamadı. Tekrar deneyin."
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                    hataKapat()
                }
                return
            }
            let gorunum: GunlukSoruOdulGorunumu = GunlukSoruMantik.odulGorunumu(gelen)
            ben.simge = gorunum.simge
            ben.etiket = gorunum.etiket
            ben.videoAdi = gelen.videoAdi
            ben.kristaliGoster()
        }
    }

    /// Android: showCrystalBreakAtStart — video kapanınca ödül görünür.
    /// Video oynatıcı bağlanana dek kısa bir bekleme ekranı gösterilir.
    private func kristaliGoster() {
        asama = .kristal
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) { [weak self] in
            self?.asama = .odul
        }
    }

    /// Android: claimRewardButton — ödül sunucuda zaten verildi; yalnızca işaretlenir ve kapanır.
    /// true dönerse ekran kapatılmalıdır.
    func odulAl() -> Bool {
        if aliniyor || asama != .odul {
            return false
        }
        aliniyor = true
        if !periodKey.isEmpty {
            depo.odulAlindiIsaretle(periodKey: periodKey)
        }
        return true
    }
}

struct GunlukSoruOdulEkrani: View {
    /// Rozet seviye atlama kuyruğu (Android: ARG_BADGE_PAYLOAD_QUEUE); kapanışta geri verilir.
    let rozetKuyrugu: [String]
    /// Ekran kapanınca çağrılır; kuyruk boş değilse çağıran rozet ekranını açar.
    let kapat: ([String]) -> Void

    @StateObject private var model: GunlukSoruOdulModeli

    /// Örnek servislerle çalışan hâl.
    init(periodKey: String, rozetKuyrugu: [String], kapat: @escaping ([String]) -> Void) {
        self.init(
            periodKey: periodKey,
            rozetKuyrugu: rozetKuyrugu,
            depo: SahteGunlukSoruDeposu.ortak,
            odul: SahteGunlukSoruOdulServisi(),
            kapat: kapat
        )
    }

    init(
        periodKey: String,
        rozetKuyrugu: [String],
        depo: GunlukSoruDeposu,
        odul: GunlukSoruOdulServisi,
        kapat: @escaping ([String]) -> Void
    ) {
        self.rozetKuyrugu = rozetKuyrugu
        self.kapat = kapat
        _model = StateObject(
            wrappedValue: GunlukSoruOdulModeli(periodKey: periodKey, depo: depo, odul: odul)
        )
    }

    var body: some View {
        ZStack {
            GunlukSoruStil.odulZemini
                .ignoresSafeArea()
            if model.asama == .kristal {
                GunlukSoruKristalBekleme()
                    .transition(.opacity)
            }
            if model.asama == .odul {
                odulIcerigi
                    .transition(.opacity)
            }
            if let hata = model.hata {
                VStack {
                    Spacer()
                    GunlukSoruBildirimGorunumu(metin: hata)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .animation(.easeInOut(duration: 0.25), value: model.asama)
        .preferredColorScheme(.dark)
        .onAppear {
            model.basla(hataKapat: { kapat([]) })
        }
    }

    /// fragment_chest.xml: görsel (200dp) ve yazı (35sp, #FFD600, üst boşluk 16dp) düğmenin üstünde
    /// kalan alanda birlikte ortalanır; düğme alttan 48dp yukarıda.
    private var odulIcerigi: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            Image(model.simge)
                .resizable()
                .scaledToFit()
                .frame(width: 200, height: 200)
            Text(model.etiket)
                .font(Tema.yazi(35))
                .foregroundColor(GunlukSoruStil.odulSarisi)
                .padding(.top, 16)
            Spacer(minLength: 0)
            Button(action: odulAl) {
                // Android: düğme metni layout'ta yazılı ("Ödülü Al"), strings.xml'de yok.
                Text("Ödülü Al")
                    .font(Tema.yazi(20))
                    .foregroundColor(GunlukSoruStil.odulZemini)
                    // Widget.MaterialComponents.Button: yatay iç boşluk 16dp, en az 88×48dp,
                    // insetTop/insetBottom 6dp (görünen zemin 36dp), köşe 4dp.
                    .padding(.horizontal, 16)
                    .frame(minWidth: 88)
                    .frame(height: 48)
                    .background(
                        RoundedRectangle(cornerRadius: 4)
                            .fill(GunlukSoruStil.odulSarisi)
                            .padding(.vertical, 6)
                    )
                    .contentShape(Rectangle())
            }
            .buttonStyle(GunlukSoruKartBasmaStili())
            .padding(.bottom, 48)
        }
    }

    private func odulAl() {
        if model.odulAl() {
            kapat(rozetKuyrugu)
        }
    }
}

/// Kristal kırılma videosunun (Android: CrystalBreakVideoFragment) yerini tutan kısa bekleme.
struct GunlukSoruKristalBekleme: View {
    @State private var buyuk = false

    var body: some View {
        Image(systemName: "diamond.fill")
            .resizable()
            .scaledToFit()
            .frame(width: 120, height: 120)
            .foregroundColor(GunlukSoruStil.renk(0x4FC3F7))
            .scaleEffect(buyuk ? 1.15 : 0.85)
            .animation(.easeInOut(duration: 0.4).repeatForever(autoreverses: true), value: buyuk)
            .onAppear {
                buyuk = true
            }
    }
}

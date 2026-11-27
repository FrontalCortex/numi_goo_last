import Foundation
import Shared
import SwiftUI

// Sandık ekranının durumu.
//
// Sandık ekranının ortak Kotlin koduna yaptığı BÜTÜN çağrılar bu dosyadadır: NewChestState
// (dokunma durum makinesi), NewChestLogic (adım göstergesi, ipucu), ChestRarity (tema, görsel,
// etiket), ServerRewards.ChestOutcome. Buradaki Swift kodu yalnızca zamanlama ve görünüm
// durumudur (Android: NewChestFragment'in animasyon ve Handler kodu).

/// Gecikmeli işler; ekran kapanınca hepsi iptal edilir.
final class SandikZamanlayici {
    private var isler: [DispatchWorkItem] = []
    private var durduruldu = false

    @discardableResult
    func sonra(_ saniye: Double, _ yapilacak: @escaping () -> Void) -> DispatchWorkItem? {
        if durduruldu { return nil }
        let isParcasi = DispatchWorkItem { [weak self] in
            guard let self = self, !self.durduruldu else { return }
            yapilacak()
        }
        isler.append(isParcasi)
        DispatchQueue.main.asyncAfter(deadline: .now() + saniye, execute: isParcasi)
        return isParcasi
    }

    func durdur() {
        durduruldu = true
        for isParcasi in isler { isParcasi.cancel() }
        isler.removeAll()
    }
}

/// Adım göstergesi dairesinin durumu (ortak kod: ChestStepIndicator).
enum SandikAdimDurumu {
    case tamam
    case etkin
    case gelecek
}

/// Sandığın çevresinde yukarı süzülen bir yıldız (NewChestFragment.spawnStar).
struct SandikYildizi: Identifiable {
    let id: Int
    let gorsel: String
    let boy: CGFloat
    /// Yatay konum, 0…1 (katman genişliği − boy ile çarpılır).
    let yatayOran: CGFloat
    let baslangicY: CGFloat
    let yukselis: CGFloat
    let sure: Double
    let dogum: TimeInterval
}

final class SandikModeli: ObservableObject {
    enum Asama {
        /// Sandık kapalı; adım dokunuşları bekleniyor.
        case kapali
        /// Sandık açıldı; sahne 700 ms'de aşağı kayıp kayboluyor.
        case aciliyor
        /// Ödül gösteriliyor.
        case odul
    }

    // MARK: Yayınlanan durum

    @Published var yukleniyor = true
    @Published var asama: Asama = .kapali
    @Published var arkaPlan: Color = Color("background_color")
    @Published var platformRengi: Color = SandikStil.renk(0x263238)
    @Published var yildizRengi: Color = SandikStil.renk(0xFFD54F)
    @Published var etiket: String = ""
    @Published var etiketRengi: Color = .white
    @Published var sandikGorseli: String = "new_chest_close_ic1"
    @Published var adimlar: [SandikAdimDurumu] = [.etkin, .gelecek, .gelecek]
    @Published var adimEtkinRengi: Color = SandikStil.renk(0xFFB300)
    @Published var adimGelecekRengi: Color = SandikStil.renk(0x3A3010)
    /// fragment_new_chest.xml'deki ilk ipucu metni.
    /// (Android: R.string.fragment_new_chest_chest_hint_text)
    @Published var ipucuMetni: String = Metin.al("fragment_new_chest_chest_hint_text")
    @Published var ipucuGorunur = false
    /// Her zıplamada bir artar; görünüm bunu izleyip zıplama animasyonunu oynatır.
    @Published var ziplamaSayaci: Int = 0
    /// Boşta sallanma animasyonunun başladığı an; `nil` iken animasyon durur.
    @Published var bosBaslangic: TimeInterval? = nil
    /// Boşta animasyonun biçimi nadirliğe göre değişir (ChestRarity.ordinal).
    @Published var nadirlikSirasi: Int = 0
    @Published var yildizlar: [SandikYildizi] = []
    @Published var odulBuyukGorsel: String = "shop_coin_ic1"
    @Published var odulKucukGorsel: String = "gold_ic"
    @Published var odulMetni: String = ""

    // MARK: Özel

    private let girdi: SandikGirdisi
    private let odulServisi: SandikOdulServisi
    private let ses: SandikSesCalar
    private let kapat: (SandikSonucu) -> Void
    private let durum: NewChestState
    private let mantik = NewChestLogic.shared
    private let zaman = SandikZamanlayici()
    private var ipucuIsi: DispatchWorkItem?
    private var basladi = false
    private var kapandi = false
    private var yildizSayaci = 0

    init(
        girdi: SandikGirdisi,
        odulServisi: SandikOdulServisi,
        ses: SandikSesCalar,
        kapat: @escaping (SandikSonucu) -> Void
    ) {
        self.girdi = girdi
        self.odulServisi = odulServisi
        self.ses = ses
        self.kapat = kapat
        let baslangic: ChestRarity = ChestRarity.companion.startRarityFromArgument(name: girdi.baslangicNadirligi)
        self.durum = NewChestState(startRarity: baslangic)
    }

    // MARK: Yaşam döngüsü (NewChestFragment.onViewCreated)

    func basla() {
        if basladi { return }
        basladi = true
        ses.hazirla()
        nadirligiUygula()
        adimlariGuncelle()
        bosAnimasyonuBaslat()
        ipucuSayaciniSifirla()
        sonucuIste()
        yildizUret()
    }

    func durdur() {
        zaman.durdur()
        ipucuIsi?.cancel()
    }

    /// requestOutcomeFromServer: yanıt gelene kadar dokunuşlar yok sayılır.
    private func sonucuIste() {
        let baslangicAdi: String = durum.currentRarity.name
        let yanitGeldi: (Result<SandikSunucuSonucu, Error>) -> Void = { [weak self] sonuc in
            DispatchQueue.main.async {
                self?.sunucuYaniti(sonuc, baslangicAdi: baslangicAdi)
            }
        }
        // Kupa yolu sandığı ayrı çağrıdan gelir (Android: ARG_CUP_FIELD doluysa claimCupPathChest).
        if let kupaAlani = girdi.kupaAlani, !kupaAlani.isEmpty {
            odulServisi.kupaYoluSandiginiAc(kupaAlani: kupaAlani, esik: girdi.kupaEsigi, tamamla: yanitGeldi)
            return
        }
        odulServisi.sandikAc(baslangicNadirligi: baslangicAdi, reklamNonce: girdi.reklamNonce, tamamla: yanitGeldi)
    }

    private func sunucuYaniti(_ sonuc: Result<SandikSunucuSonucu, Error>, baslangicAdi: String) {
        if kapandi { return }
        yukleniyor = false
        switch sonuc {
        case .success(let yanit):
            let kotlinSonucu = ServerRewards.ChestOutcome(
                rarityPath: yanit.nadirlikYolu,
                finalRarity: yanit.sonNadirlik,
                rewardType: yanit.odulTuru,
                rewardAmount: Int32(yanit.odulMiktari)
            )
            durum.applyServerOutcome(outcome: kotlinSonucu)
        case .failure:
            // Android: Toast + closeFragment. Ödül verilmedi; kullanıcı bir şey kaybetmez.
            bitir(acildi: false)
        }
    }

    // MARK: Dokunuş (onScreenTapped)

    func dokunuldu() {
        if kapandi { return }
        // Android: sunucu sonucu gelmeden ipucu sayacı da sıfırlanmaz.
        if !durum.outcomeReady { return }
        ipucuSayaciniSifirla()
        let sonuc: ChestTapResult = durum.onScreenTapped()
        let eylem: ChestTapAction = sonuc.action
        if eylem == ChestTapAction.step {
            adimlariGuncelle()
            ses.seviyeSesiCal(hiz: sonuc.soundRate)
            if sonuc.rarityChanged {
                nadirligiUygula()
            }
            zipla()
            if sonuc.showOpenHint {
                // Android: appString(R.string.ui_sandigi_acmak_icin_dokun)
                ipucuMetni = Metin.al("ui_sandigi_acmak_icin_dokun")
            }
        } else if eylem == ChestTapAction.openChest {
            bosBaslangic = nil
            ses.seviyeSesiCal(hiz: sonuc.soundRate)
            sandigiAc()
        } else if eylem == ChestTapAction.close {
            bitir(acildi: true)
        }
    }

    // MARK: Görünüm durumu

    /// applyRarity
    private func nadirligiUygula() {
        let nadirlik: ChestRarity = durum.currentRarity
        let tema: RarityTheme = nadirlik.theme
        sandikGorseli = nadirlik.drawableRes
        arkaPlan = SandikStil.renk(argb: tema.bgColor)
        platformRengi = SandikStil.renk(argb: tema.platformColor)
        yildizRengi = SandikStil.renk(argb: tema.starColor)
        adimEtkinRengi = SandikStil.renk(argb: tema.stepActiveColor)
        adimGelecekRengi = SandikStil.renk(argb: tema.stepFutureColor)
        // Android: appString(rarity.labelRes) — ortak koddaki `label` Türkçe sabittir, dile bağlı değil.
        etiket = Metin.al(SandikModeli.etiketAnahtari(nadirlikAdi: nadirlik.name))
        etiketRengi = SandikStil.renk(argb: nadirlik.labelColor)
        nadirlikSirasi = Int(nadirlik.ordinal)
    }

    /// ChestRarity.labelRes karşılığı: nadirlik adı → strings.xml anahtarı.
    private static func etiketAnahtari(nadirlikAdi: String) -> String {
        switch nadirlikAdi {
        case "RARE":
            return "ui_ender"
        case "EPIC":
            return "ui_destansi"
        case "LEGENDARY":
            return "ui_efsanevi"
        default:
            return "ui_siradan"
        }
    }

    /// updateStepIndicators
    private func adimlariGuncelle() {
        let dokunusSayisi: Int32 = durum.tapCount
        var yeni: [SandikAdimDurumu] = []
        for sira in 0..<3 {
            let gosterge: ChestStepIndicator = mantik.stepIndicator(index: Int32(sira), tapCount: dokunusSayisi)
            if gosterge == ChestStepIndicator.done {
                yeni.append(.tamam)
            } else if gosterge == ChestStepIndicator.active {
                yeni.append(.etkin)
            } else {
                yeni.append(.gelecek)
            }
        }
        adimlar = yeni
    }

    /// startIdleAnimation: açılmaya hazır/açılmış sandıkta başlamaz.
    private func bosAnimasyonuBaslat() {
        if durum.tapCount >= 4 { return }
        bosBaslangic = ProcessInfo.processInfo.systemUptime
    }

    /// playBounceAnimation: boşta animasyon durur, zıplar, 350 ms sonra boşta animasyon yeniden başlar.
    private func zipla() {
        bosBaslangic = nil
        ziplamaSayaci += 1
        zaman.sonra(0.35) { [weak self] in
            self?.bosAnimasyonuBaslat()
        }
    }

    /// resetHintTimer: ipucu gizlenir, HINT_DELAY_MS hareketsizlikten sonra 300 ms'de belirir.
    private func ipucuSayaciniSifirla() {
        ipucuIsi?.cancel()
        ipucuGorunur = false
        let gecikme: Double = Double(mantik.HINT_DELAY_MS) / 1000.0
        ipucuIsi = zaman.sonra(gecikme) { [weak self] in
            guard let self = self, self.asama == .kapali else { return }
            self.ipucuGorunur = true
        }
    }

    /// openChest: açık sandık görseli, sahne 700 ms'de kaybolur, sonra ödül.
    private func sandigiAc() {
        ipucuIsi?.cancel()
        sandikGorseli = durum.currentRarity.openDrawableRes
        asama = .aciliyor
        zaman.sonra(0.7) { [weak self] in
            self?.oduluGoster()
        }
    }

    /// showReward: ödül sunucuda zaten yazıldı; burada yalnızca gösterilir.
    private func oduluGoster() {
        ses.odulSesiCal()
        let gosterim: ChestRewardDisplay = durum.showReward()
        odulMetni = gosterim.textValue
        odulKucukGorsel = gosterim.textDrawable
        odulBuyukGorsel = gosterim.coinDrawable
        asama = .odul
        // Ödül animasyonu 500 ms; bitince dokununca kapanabilir.
        zaman.sonra(0.5) { [weak self] in
            self?.durum.onRewardAnimationEnd()
        }
    }

    private func bitir(acildi: Bool) {
        if kapandi { return }
        kapandi = true
        durdur()
        let sonuc = SandikSonucu(
            acildi: acildi,
            kazanilanAltin: Int(durum.earnedGoldAmount),
            kazanilanAnahtar: Int(durum.earnedKeyAmount),
            sonNadirlik: durum.currentRarity.name
        )
        kapat(sonuc)
    }

    // MARK: Yıldızlar (starSpawner / spawnStar)

    /// 200–500 ms arayla bir yıldız üretir. Android ölçüleri pikseldir; burada 3'e bölünerek
    /// pt'ye çevrildi (bkz. SandikStil.pikseldenNokta).
    private func yildizUret() {
        if kapandi { return }
        let simdi: TimeInterval = ProcessInfo.processInfo.systemUptime
        var kalanlar: [SandikYildizi] = yildizlar.filter { simdi - $0.dogum < $0.sure }
        if asama == .kapali {
            yildizSayaci += 1
            let boy: CGFloat = SandikStil.pikseldenNokta(CGFloat(Int.random(in: 35..<75)))
            // Dikey aralık: platformun üstü (sandık görselinin 110 pt altı) ile adım
            // göstergesinin üstü (sandık görselinin altından 50 pt sonra = 300 pt).
            let baslangicY: CGFloat = CGFloat.random(in: 110...300)
            let yukselis: CGFloat = SandikStil.pikseldenNokta(CGFloat(Int.random(in: 200..<400)))
            let sure: Double = Double.random(in: 2.0..<3.5)
            let yildiz = SandikYildizi(
                id: yildizSayaci,
                gorsel: Bool.random() ? "ic_star_diamond" : "ic_star_four_point",
                boy: boy,
                yatayOran: CGFloat.random(in: 0...1),
                baslangicY: baslangicY,
                yukselis: yukselis,
                sure: sure,
                dogum: simdi
            )
            kalanlar.append(yildiz)
        }
        yildizlar = kalanlar
        let bekleme: Double = Double.random(in: 0.2..<0.5)
        zaman.sonra(bekleme) { [weak self] in
            self?.yildizUret()
        }
    }
}

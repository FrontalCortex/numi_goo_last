import Foundation
import Shared
import SwiftUI

// Kristal kırma videosunun durumu.
//
// Kristal ekranının ortak Kotlin koduna yaptığı BÜTÜN çağrılar bu dosyadadır: CrystalBreakState
// (durak / dokunuş durum makinesi), CrystalBreakLogic (yıldız sayısı), CrystalRarityTier
// (etiket ve renk). Bu türler ios/shared/.../oyun/CrystalBreakLogic.kt içindedir.
//
// Video oynatıcı henüz bağlı değil: oynatım konumu burada bir saatle TAKLİT edilir
// (`yerTutucuVideoSuresi` boyunca ilerler). AVPlayer bağlanınca `kareIlerlet` içindeki konum
// hesabı oynatıcının gerçek konumuyla, "video bitti" denetimi de oynatıcının bildirimiyle
// değiştirilmeli; gerisi aynı kalır.

/// Dokunma / durak dalgası (halka + parıltı).
struct SandikKristalDalgasi: Identifiable {
    let id: Int
    let merkez: CGPoint
    let renk: Color
    /// `true`: durağı geçiren büyük dalga (ekran ortasında); `false`: dokunma dalgası.
    let buyuk: Bool
    let dogum: TimeInterval
}

/// Dalgadan saçılan yıldız (emitTapStars).
struct SandikKristalYildizi: Identifiable {
    let id: Int
    let baslangic: CGPoint
    let kayma: CGSize
    let boy: CGFloat
    let ilkDonme: Double
    let ekDonme: Double
    let renk: Color
    let sure: Double
    let dogum: TimeInterval
}

final class SandikKristalModeli: ObservableObject {
    // MARK: Yayınlanan durum

    @Published var etiket: String = ""
    @Published var etiketRengi: Color = .white
    @Published var etiketGorunur = false
    @Published var dalgalar: [SandikKristalDalgasi] = []
    @Published var yildizlar: [SandikKristalYildizi] = []
    /// Durakta beklerken video hafifçe büyüyüp küçülür (maybeStartIdlePulse).
    @Published var nabizBaslangici: TimeInterval? = nil
    /// Durağı geçiren dalgayla birlikte videonun sarsılması (animateVideoShockwave).
    @Published var sarsintiBaslangici: TimeInterval? = nil

    /// Oynatılacak videonun adı (res/raw içindeki dosyanın uzantısız adı).
    let videoAdi: String

    // MARK: Özel

    /// Gerçek videoların süresi bilinmiyor (dosyalar depoda yok); son durak 2200 ms'de olduğu
    /// için yer tutucu saat 3000 ms'de "video bitti" sayar.
    private let yerTutucuVideoSuresi: Double = 3.0

    private let ses: SandikSesCalar
    private let kapat: () -> Void
    private let durum: CrystalBreakState
    private let mantik = CrystalBreakLogic.shared
    private let zaman = SandikZamanlayici()
    private var saat: Timer?
    private var oynuyor = false
    private var konum: Double = 0
    private var sonKare: TimeInterval = 0
    private var basladi = false
    private var kapandi = false
    private var sayac = 0
    private var ekranBoyu: CGSize = .zero

    init(videoAdi: String?, ses: SandikSesCalar, kapat: @escaping () -> Void) {
        self.ses = ses
        self.kapat = kapat
        let yeniDurum = CrystalBreakState(requestedVideoName: videoAdi)
        self.durum = yeniDurum
        self.videoAdi = yeniDurum.videoName
    }

    // MARK: Yaşam döngüsü

    func basla() {
        if basladi { return }
        basladi = true
        ses.hazirla()
        etiketiGuncelle()
        // Oynatıcı hazır olup oynamaya başladı (Android: onIsPlayingChanged(true)).
        oynuyor = true
        durum.onPlaybackStarted()
        etiketGorunur = durum.rarityLabelVisible
        sonKare = ProcessInfo.processInfo.systemUptime
        saat = Timer.scheduledTimer(withTimeInterval: 1.0 / 60.0, repeats: true) { [weak self] _ in
            self?.kareIlerlet()
        }
    }

    func durdur() {
        saat?.invalidate()
        saat = nil
        zaman.durdur()
    }

    func ekranBoyunuAyarla(_ boy: CGSize) {
        ekranBoyu = boy
    }

    // MARK: Saat (checkpointTick)

    private func kareIlerlet() {
        if kapandi { return }
        let simdi: TimeInterval = ProcessInfo.processInfo.systemUptime
        let fark: Double = simdi - sonKare
        sonKare = simdi
        efektleriTemizle(simdi: simdi)
        if !oynuyor { return }

        konum += fark
        if konum >= yerTutucuVideoSuresi {
            // Player.STATE_ENDED: Android'de ekran kendiliğinden kapanır.
            durum.onVideoEnded()
            etiketGorunur = durum.rarityLabelVisible
            bitir()
            return
        }

        let konumMs = Int64(konum * 1000.0)
        let sonuc: CrystalTickResult = durum.onTick(positionMs: konumMs, isPlaying: true)
        if sonuc.rarityChanged {
            etiketiGuncelle()
        }
        let dalgaSirasi: Int32 = sonuc.resumeWaveForContinueTap
        if dalgaSirasi != 0 {
            buyukDalgayiZamanla(devamSirasi: dalgaSirasi)
        }
        if sonuc.pausePlayback {
            oynuyor = false
            if durum.shouldIdlePulse {
                nabizBaslangici = simdi
            }
        }
    }

    // MARK: Dokunuş

    func dokunuldu(_ nokta: CGPoint) {
        if kapandi { return }
        ses.kristalTikSesiCal()
        // Dalga rengi dokunuş anındaki etikete göre belirlenir (currentRarityWaveColor).
        let dalgaRengi: Color = SandikStil.renk(argb: durum.tapWaveColor)
        let sonuc: CrystalTapResult = durum.onTap(isPlaying: oynuyor)
        let eylem: CrystalTapAction = sonuc.action
        if eylem == CrystalTapAction.tapWave {
            dokunmaDalgasi(nokta, renk: dalgaRengi)
        } else if eylem == CrystalTapAction.resume {
            dokunmaDalgasi(nokta, renk: dalgaRengi)
            nabizBaslangici = nil
            let dalgaSirasi: Int32 = sonuc.resumeWaveForContinueTap
            if dalgaSirasi != 0 {
                buyukDalgayiZamanla(devamSirasi: dalgaSirasi)
            }
            sonKare = ProcessInfo.processInfo.systemUptime
            oynuyor = true
        } else if eylem == CrystalTapAction.dismiss {
            bitir()
        }
    }

    // MARK: Etiket

    private func etiketiGuncelle() {
        let kademe: CrystalRarityTier = durum.currentRarityTier
        etiket = kademe.label
        etiketRengi = SandikStil.renk(argb: kademe.color)
    }

    // MARK: Dalgalar

    /// showTapWave: dokunulan yerde halka + parıltı + 12 yıldız.
    private func dokunmaDalgasi(_ nokta: CGPoint, renk: Color) {
        let simdi: TimeInterval = ProcessInfo.processInfo.systemUptime
        sayac += 1
        dalgalar.append(SandikKristalDalgasi(id: sayac, merkez: nokta, renk: renk, buyuk: false, dogum: simdi))
        // Halka 78 pt; yıldızlar halkanın 18 pt dışından başlar, 12–20 pt boyundadır.
        yildizSac(merkez: nokta, renk: renk, halkaYaricapi: 39 + 18, adet: 12, enKucuk: 12, enBuyuk: 20, simdi: simdi)
    }

    /// maybeTriggerResumeTapRarityWave: durak geçildikten 50 ms sonra etiket güncellenir ve
    /// ekranın ortasında etiket rengiyle büyük dalga gösterilir.
    private func buyukDalgayiZamanla(devamSirasi: Int32) {
        let gecikme: Double = Double(mantik.RARITY_APPLY_DELAY_MS) / 1000.0
        zaman.sonra(gecikme) { [weak self] in
            self?.buyukDalga(devamSirasi: devamSirasi)
        }
    }

    private func buyukDalga(devamSirasi: Int32) {
        if kapandi { return }
        let gosterilsin: Bool = durum.onResumeWaveDelayElapsed(continueTapCount: devamSirasi)
        if !gosterilsin { return }
        etiketiGuncelle()
        let kademe: CrystalRarityTier = durum.currentRarityTier
        let renk: Color = SandikStil.renk(argb: kademe.color)
        let merkez = CGPoint(x: ekranBoyu.width / 2, y: ekranBoyu.height / 2)
        let simdi: TimeInterval = ProcessInfo.processInfo.systemUptime
        sayac += 1
        dalgalar.append(SandikKristalDalgasi(id: sayac, merkez: merkez, renk: renk, buyuk: true, dogum: simdi))
        let adet: Int = Int(mantik.rarityWaveStarCount(tier: kademe))
        // Halka 86 pt; yıldızlar halkanın 26 pt dışından başlar, 16–28 pt boyundadır.
        yildizSac(merkez: merkez, renk: renk, halkaYaricapi: 43 + 26, adet: adet, enKucuk: 16, enBuyuk: 28, simdi: simdi)
        sarsintiBaslangici = simdi
    }

    /// emitTapStars: yıldızlar çemberin dışından başlayıp dışarı doğru uçar.
    private func yildizSac(
        merkez: CGPoint,
        renk: Color,
        halkaYaricapi: CGFloat,
        adet: Int,
        enKucuk: Int,
        enBuyuk: Int,
        simdi: TimeInterval
    ) {
        if adet <= 0 { return }
        var yeniler: [SandikKristalYildizi] = []
        for sira in 0..<adet {
            let temelAci: Double = (360.0 / Double(adet)) * Double(sira)
            let sapma: Double = Double(Int.random(in: -15..<15))
            let aci: Double = (temelAci + sapma) * Double.pi / 180.0
            let boy: CGFloat = CGFloat(Int.random(in: enKucuk...enBuyuk))
            let baslangicX: CGFloat = merkez.x + CGFloat(cos(aci)) * halkaYaricapi
            let baslangicY: CGFloat = merkez.y + CGFloat(sin(aci)) * halkaYaricapi
            // Uçuş mesafesi Android'de 220–420 pikseldir.
            let mesafe: CGFloat = SandikStil.pikseldenNokta(CGFloat(Int.random(in: 220..<420)))
            let kayma = CGSize(width: CGFloat(cos(aci)) * mesafe, height: CGFloat(sin(aci)) * mesafe)
            let sure: Double = Double(Int.random(in: 320..<520)) / 1000.0
            sayac += 1
            let yildiz = SandikKristalYildizi(
                id: sayac,
                baslangic: CGPoint(x: baslangicX, y: baslangicY),
                kayma: kayma,
                boy: boy,
                ilkDonme: Double(Int.random(in: 0..<360)),
                ekDonme: Double(Int.random(in: -90..<90)),
                renk: renk,
                sure: sure,
                dogum: simdi
            )
            yeniler.append(yildiz)
        }
        yildizlar.append(contentsOf: yeniler)
    }

    /// Süresi dolan efektleri listeden çıkarır.
    private func efektleriTemizle(simdi: TimeInterval) {
        if !dalgalar.isEmpty {
            let kalan: [SandikKristalDalgasi] = dalgalar.filter { simdi - $0.dogum < 0.45 }
            if kalan.count != dalgalar.count { dalgalar = kalan }
        }
        if !yildizlar.isEmpty {
            let kalan: [SandikKristalYildizi] = yildizlar.filter { simdi - $0.dogum < $0.sure + 0.05 }
            if kalan.count != yildizlar.count { yildizlar = kalan }
        }
        if let sarsinti = sarsintiBaslangici, simdi - sarsinti > 0.42 {
            sarsintiBaslangici = nil
        }
    }

    private func bitir() {
        if kapandi { return }
        kapandi = true
        durdur()
        kapat()
    }
}

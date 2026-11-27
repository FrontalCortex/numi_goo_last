import AVFoundation
import Foundation
import Shared
import SwiftUI

// Kristal kırma videosunun durumu.
//
// Kristal ekranının ortak Kotlin koduna yaptığı BÜTÜN çağrılar bu dosyadadır: CrystalBreakState
// (durak / dokunuş durum makinesi), CrystalBreakLogic (yıldız sayısı), CrystalRarityTier
// (etiket ve renk). Bu türler ios/shared/.../oyun/CrystalBreakLogic.kt içindedir.
//
// Video: dosya pakette varsa (iosApp/Videolar/<ad>.mp4) AVPlayer ile oynatılır; konum
// oynatıcıdan okunur, "video bitti" oynatıcının bildirimiyle gelir (SandikVideoOynatici).
// Dosya yoksa ya da açılamazsa oynatım konumu burada bir saatle TAKLİT edilir
// (`yerTutucuVideoSuresi` boyunca ilerler) ve ekranda yer tutucu görsel çizilir.

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
    /// `true`: video dosyası pakette var ve AVPlayer ile oynatılıyor; `false`: yer tutucu.
    @Published private(set) var gercekVideo: Bool = false
    /// Video dosyası pakette varsa oynatıcısı (görünüm bunu AVPlayerLayer'a bağlar).
    var videoOynaticisi: AVPlayer? {
        return video.oynatici
    }

    // MARK: Özel

    /// Gerçek videoların süresi bilinmiyor (dosyalar depoda yok); son durak 2200 ms'de olduğu
    /// için yer tutucu saat 3000 ms'de "video bitti" sayar.
    private let yerTutucuVideoSuresi: Double = 3.0

    private let ses: SandikSesCalar
    private let kapat: () -> Void
    private let durum: CrystalBreakState
    private let mantik = CrystalBreakLogic.shared
    private let zaman = SandikZamanlayici()
    private let video: SandikVideoOynatici
    /// Gerçek videoda: model oynatımın sürmesini istiyor mu (durakta ve bitişte `false`).
    private var oynatmaIsteniyor = false
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
        let oynatici = SandikVideoOynatici(ad: yeniDurum.videoName, sesDuzeyi: ses.videoSesDuzeyi)
        self.video = oynatici
        self.gercekVideo = oynatici.varMi
    }

    // MARK: Yaşam döngüsü

    func basla() {
        if basladi { return }
        basladi = true
        ses.hazirla()
        etiketiGuncelle()
        if gercekVideo {
            // Etiket, oynatıcı gerçekten oynamaya başlayınca görünür (bkz. gercekKare).
            video.bitince = { [weak self] in
                self?.videoBitti()
            }
            oynatmaIsteniyor = true
            video.oynat()
        } else {
            // Yer tutucu: oynatıcı hazır olup oynamaya başlamış sayılır
            // (Android: onIsPlayingChanged(true)).
            oynuyor = true
            durum.onPlaybackStarted()
            etiketGorunur = durum.rarityLabelVisible
        }
        sonKare = ProcessInfo.processInfo.systemUptime
        saat = Timer.scheduledTimer(withTimeInterval: 1.0 / 60.0, repeats: true) { [weak self] _ in
            self?.kareIlerlet()
        }
    }

    func durdur() {
        saat?.invalidate()
        saat = nil
        zaman.durdur()
        oynatmaIsteniyor = false
        video.bitince = nil
        video.duraklat()
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
        if gercekVideo {
            gercekKare(simdi: simdi)
            return
        }
        if !oynuyor { return }

        konum += fark
        if konum >= yerTutucuVideoSuresi {
            videoBitti()
            return
        }

        let konumMs = Int64(konum * 1000.0)
        tikIsle(konumMs: konumMs, simdi: simdi)
    }

    /// Gerçek video: durum ve konum oynatıcıdan okunur.
    private func gercekKare(simdi: TimeInterval) {
        if video.basarisiz {
            yerTutucuyaDus(simdi: simdi)
            return
        }
        let oynuyorMu: Bool = video.oynuyor
        oynuyor = oynuyorMu
        if !oynuyorMu {
            // Uygulama arka plana gidip dönünce sistem oynatıcıyı duraklatır; durakta
            // beklenmiyorsa oynatım sürdürülür (Android: onResume → exoPlayer.play()).
            if oynatmaIsteniyor && video.duraklatilmis {
                video.oynat()
            }
            return
        }
        if !durum.hasPlaybackStarted {
            // Android: onIsPlayingChanged(true) → etiket görünür.
            durum.onPlaybackStarted()
            etiketGorunur = durum.rarityLabelVisible
        }
        let konumMs = Int64(video.konumSaniye * 1000.0)
        tikIsle(konumMs: konumMs, simdi: simdi)
    }

    /// Video dosyası açılamadı: yer tutucu saate geçilir, akış kaldığı yerden sürer.
    private func yerTutucuyaDus(simdi: TimeInterval) {
        video.bitince = nil
        video.duraklat()
        oynatmaIsteniyor = false
        gercekVideo = false
        konum = video.konumSaniye
        sonKare = simdi
        if !durum.hasPlaybackStarted {
            durum.onPlaybackStarted()
            etiketGorunur = durum.rarityLabelVisible
        }
        oynuyor = !durum.waitingTapToContinue
    }

    /// Player.STATE_ENDED: Android'de ekran kendiliğinden kapanır.
    private func videoBitti() {
        if kapandi { return }
        oynatmaIsteniyor = false
        durum.onVideoEnded()
        etiketGorunur = durum.rarityLabelVisible
        bitir()
    }

    /// checkpointTick.run gövdesi: `konumMs` oynatım konumudur (oynatıcı oynarken çağrılır).
    private func tikIsle(konumMs: Int64, simdi: TimeInterval) {
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
            if gercekVideo {
                oynatmaIsteniyor = false
                video.duraklat()
            }
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
        let oynuyorMu: Bool = gercekVideo ? video.oynuyor : oynuyor
        let sonuc: CrystalTapResult = durum.onTap(isPlaying: oynuyorMu)
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
            if gercekVideo {
                oynatmaIsteniyor = true
                video.oynat()
            } else {
                oynuyor = true
            }
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

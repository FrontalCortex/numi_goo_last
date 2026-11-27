import AVFoundation
import SwiftUI
import UIKit

// Kristal videosunun oynatıcısı (Android: CrystalBreakVideoFragment içindeki ExoPlayer + PlayerView).
//
// Android videoyu `res/raw/<ad>.mp4` kaynağından oynatır; bu dosyalar depoda YOKTUR. iOS'ta dosyalar
// `iosApp/Videolar/` klasörüne aynı adla konacak (XcodeGen klasördeki her dosyayı pakete ekler).
// Beklenen adlar ortak koddaki listedir (CrystalBreakLogic.videoNames):
//   crystal_blue_blue, crystal_blue_red, crystal_red_red, crystal_red_purple, crystal_red_yellow,
//   crystal_blue_purple, crystal_purple_purple, crystal_purple_yellow
// Dosya pakette yoksa `oynatici` nil olur ve ekran yer tutucuya (SandikVideoYerTutucu) düşer;
// oynatım konumu o durumda SandikKristalModeli'ndeki saatle taklit edilir.
//
// Yalnızca iOS 16'da var olan AVFoundation API'leri kullanılır (AVPlayer, AVPlayerItem,
// AVPlayerLayer, timeControlStatus, AVPlayerItemDidPlayToEndTime bildirimi).

final class SandikVideoOynatici {
    /// Dosya pakette bulunduysa oynatıcı; yoksa nil.
    let oynatici: AVPlayer?
    /// Video sonuna kadar oynadığında ana iş parçacığında çağrılır (Android: Player.STATE_ENDED).
    var bitince: (() -> Void)? = nil

    private var bitisGozlemcisi: NSObjectProtocol? = nil

    /// - Parameters:
    ///   - ad: videonun uzantısız dosya adı (Android `res/raw` adı).
    ///   - sesDuzeyi: 0…1 (Android: ses açıksa 0.5, kapalıysa 0).
    init(ad: String, sesDuzeyi: Float) {
        let adres: URL? = SandikVideoOynatici.adresBul(ad: ad)
        var yeniOynatici: AVPlayer? = nil
        var yeniOge: AVPlayerItem? = nil
        if let bulunan = adres {
            let oge = AVPlayerItem(url: bulunan)
            let kurulan = AVPlayer(playerItem: oge)
            kurulan.volume = sesDuzeyi
            // Video bitince son karede durur; ekranı kapatma kararı modeldedir.
            kurulan.actionAtItemEnd = AVPlayer.ActionAtItemEnd.pause
            // Yerel dosya: ara belleğe alma beklemesine gerek yok.
            kurulan.automaticallyWaitsToMinimizeStalling = false
            yeniOynatici = kurulan
            yeniOge = oge
        }
        self.oynatici = yeniOynatici

        if let oge = yeniOge {
            bitisGozlemcisi = NotificationCenter.default.addObserver(
                forName: NSNotification.Name.AVPlayerItemDidPlayToEndTime,
                object: oge,
                queue: OperationQueue.main
            ) { [weak self] _ in
                self?.bitince?()
            }
        }
    }

    deinit {
        if let gozlemci = bitisGozlemcisi {
            NotificationCenter.default.removeObserver(gozlemci)
        }
        oynatici?.pause()
    }

    /// Dosyayı paketin kökünde, bulunamazsa "Videolar" alt dizininde arar (klasör pakete klasör
    /// başvurusu olarak girerse dosyalar alt dizinde kalır). Android dosyaları .mp4'tür; .mov ve
    /// .m4v de kabul edilir.
    static func adresBul(ad: String) -> URL? {
        if ad.isEmpty { return nil }
        let uzantilar: [String] = ["mp4", "mov", "m4v"]
        for uzanti in uzantilar {
            if let kokte = Bundle.main.url(forResource: ad, withExtension: uzanti) {
                return kokte
            }
            if let altDizinde = Bundle.main.url(forResource: ad, withExtension: uzanti, subdirectory: "Videolar") {
                return altDizinde
            }
        }
        return nil
    }

    /// Dosya pakette var mı.
    var varMi: Bool {
        return oynatici != nil
    }

    /// Oynatıcı gerçekten oynuyor mu (Android: STATE_READY && isPlaying).
    var oynuyor: Bool {
        guard let oynatici = oynatici else { return false }
        return oynatici.timeControlStatus == AVPlayer.TimeControlStatus.playing
    }

    /// Oynatıcı duraklatılmış mı (beklemede ya da oynuyor değil).
    var duraklatilmis: Bool {
        guard let oynatici = oynatici else { return true }
        return oynatici.timeControlStatus == AVPlayer.TimeControlStatus.paused
    }

    /// Dosya açılamadı mı (bozuk / desteklenmeyen biçim).
    var basarisiz: Bool {
        guard let oynatici = oynatici else { return false }
        if oynatici.status == AVPlayer.Status.failed { return true }
        guard let oge = oynatici.currentItem else { return false }
        return oge.status == AVPlayerItem.Status.failed
    }

    /// Oynatım konumu, saniye (Android: player.currentPosition / 1000).
    var konumSaniye: Double {
        guard let oynatici = oynatici else { return 0 }
        let saniye: Double = CMTimeGetSeconds(oynatici.currentTime())
        if saniye.isNaN || saniye.isInfinite || saniye < 0 { return 0 }
        return saniye
    }

    func oynat() {
        oynatici?.play()
    }

    func duraklat() {
        oynatici?.pause()
    }
}

/// Katmanı AVPlayerLayer olan UIKit görünümü.
final class SandikVideoKatmanGorunumu: UIView {
    override class var layerClass: AnyClass {
        return AVPlayerLayer.self
    }

    var oynaticiKatmani: AVPlayerLayer? {
        return layer as? AVPlayerLayer
    }
}

/// Videoyu alanı kırparak dolduracak biçimde çizer (Android: PlayerView `resize_mode="zoom"`,
/// denetim düğmeleri yok).
struct SandikVideoGorunumu: UIViewRepresentable {
    let oynatici: AVPlayer

    func makeUIView(context: Context) -> SandikVideoKatmanGorunumu {
        let gorunum = SandikVideoKatmanGorunumu()
        gorunum.backgroundColor = UIColor.black
        gorunum.isUserInteractionEnabled = false
        gorunum.oynaticiKatmani?.videoGravity = AVLayerVideoGravity.resizeAspectFill
        gorunum.oynaticiKatmani?.player = oynatici
        return gorunum
    }

    func updateUIView(_ uiView: SandikVideoKatmanGorunumu, context: Context) {
        if uiView.oynaticiKatmani?.player !== oynatici {
            uiView.oynaticiKatmani?.player = oynatici
        }
    }
}

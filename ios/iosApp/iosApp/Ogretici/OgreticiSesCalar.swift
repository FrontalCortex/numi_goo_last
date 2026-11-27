import AVFoundation
import Foundation
import UIKit

// Öğreticinin gerçek ses çalıcısı (AVAudioPlayer).
//
// Android: TutorialFragment.playSound / playCorretSound / onPause / onResume / onDestroyView
// (app/.../TutorialFragment.kt:19783-19870, 19698-19730, 19946-19952).
//  - Adım sesi adım gösterilirken başlar (showStep → playSound); önceki ses bırakılır (susar).
//  - Adım sesi yalnızca "tutorial_sound_enabled" (varsayılan açık) ile; sonuç sesi "sound_enabled" ile.
//  - Doğru cevap sesi 0.3 ses düzeyiyle, yanlış cevap sesi tam düzeyle çalınır.
//  - Uygulama arka plana geçince ses duraklatılır, dönünce kaldığı yerden sürer.
//  - Dosya yoksa/çalınamazsa sessizce geçilir; ses akışı ilerletmez.
//  - Uzak (Firebase Storage) sesler: öğretici içeriğinde kullanılmıyor (uzakSes hiçbir adımda
//    dolu değil); Türkçe dışı diller henüz yok. `uzakYol` şimdilik yok sayılır.
//
// Ses oturumu: Android MediaPlayer ses odağı istemiyor, medya akışından çalıyor (telefonun zil
// sessizliğinden etkilenmez) ve başka uygulamanın müziğini kesmiyor. iOS karşılığı: `.playback`
// kategorisi (sessiz anahtar çalmayı engellemez) + `.mixWithOthers` (başkasının sesini kesmez).
// Ses oturumu ilk çalmada kurulur (çalıcının kurulması ucuzdur).
//
// İş parçacığı: protokol yöntemleri ana iş parçacığından çağrılır; başka iş parçacığından
// gelirse ana iş parçacığına aktarılır.

final class OgreticiGercekSesCalici: OgreticiSesCalici {
    /// Ayar anahtarları (Android AppPrefs ile aynı adlar). Ayar ekranı yazılınca bu anahtarlar yazılır.
    static let ogreticiSesiAnahtari: String = "tutorial_sound_enabled"
    static let sesAcikAnahtari: String = "sound_enabled"

    /// Çalınabilecek uzantılar, sırayla denenir.
    private static let uzantilar: [String] = ["mp3", "wav", "m4a", "flac"]

    private let ayarlar: UserDefaults
    private var calici: AVAudioPlayer? = nil
    private var oturumKuruldu: Bool = false
    /// Arka plana geçerken biz duraklattık mı (Android wasPausedByLifecycle).
    private var yasamDongusuyleDuraklatildi: Bool = false
    private var gozlemciler: [NSObjectProtocol] = []

    init(ayarlar: UserDefaults = UserDefaults.standard) {
        self.ayarlar = ayarlar
        let merkez: NotificationCenter = NotificationCenter.default
        let birakirken: NSObjectProtocol = merkez.addObserver(
            forName: UIApplication.willResignActiveNotification, object: nil, queue: OperationQueue.main
        ) { [weak self] (_: Notification) in
            self?.arkaPlanaGecildi()
        }
        let donerken: NSObjectProtocol = merkez.addObserver(
            forName: UIApplication.didBecomeActiveNotification, object: nil, queue: OperationQueue.main
        ) { [weak self] (_: Notification) in
            self?.onPlanaDonuldu()
        }
        gozlemciler = [birakirken, donerken]
    }

    deinit {
        for g in gozlemciler {
            NotificationCenter.default.removeObserver(g)
        }
        calici?.stop()
    }

    // MARK: OgreticiSesCalici

    func adimSesiniCal(anahtar: String?, uzakYol: String?) {
        anaIsParcacigindaCalistir { [weak self] in
            guard let self = self else { return }
            // Android: önce önceki ses bırakılır, sonra ayar ve kaynak denetlenir (ses yoksa sessiz kalınır).
            self.sustur()
            guard self.ayarAcikMi(OgreticiGercekSesCalici.ogreticiSesiAnahtari) else { return }
            guard let ad = anahtar, !ad.isEmpty else { return }
            self.cal(ad: ad, sesDuzeyi: 1.0)
        }
    }

    func sonucSesiniCal(dogru: Bool) {
        anaIsParcacigindaCalistir { [weak self] in
            guard let self = self else { return }
            guard self.ayarAcikMi(OgreticiGercekSesCalici.sesAcikAnahtari) else { return }
            // Android: correct_answer_sound 0.3, incorrect_answer_sound 1.0.
            self.sustur()
            if dogru {
                self.cal(ad: "correct_answer_sound", sesDuzeyi: 0.3)
            } else {
                self.cal(ad: "incorrect_answer_sound", sesDuzeyi: 1.0)
            }
        }
    }

    func durdur() {
        anaIsParcacigindaCalistir { [weak self] in
            self?.sustur()
        }
    }

    // MARK: İç işler

    private func anaIsParcacigindaCalistir(_ isi: @escaping () -> Void) {
        if Thread.isMainThread {
            isi()
        } else {
            DispatchQueue.main.async(execute: isi)
        }
    }

    /// Anahtar yoksa varsayılan açık (Android getBoolean(…, true)).
    private func ayarAcikMi(_ anahtar: String) -> Bool {
        if let deger = ayarlar.object(forKey: anahtar) as? Bool {
            return deger
        }
        return true
    }

    private func sustur() {
        calici?.stop()
        calici = nil
        yasamDongusuyleDuraklatildi = false
    }

    private func dosyaAdresi(_ ad: String) -> URL? {
        for uzanti in OgreticiGercekSesCalici.uzantilar {
            if let adres = Bundle.main.url(forResource: ad, withExtension: uzanti) {
                return adres
            }
        }
        return nil
    }

    private func oturumuKur() {
        if oturumKuruldu { return }
        oturumKuruldu = true
        let oturum: AVAudioSession = AVAudioSession.sharedInstance()
        do {
            try oturum.setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try oturum.setActive(true)
        } catch {
            // Oturum kurulamazsa varsayılan oturumla denenir; hata sessizce geçilir.
        }
    }

    private func cal(ad: String, sesDuzeyi: Float) {
        guard let adres = dosyaAdresi(ad) else { return }
        oturumuKur()
        do {
            let yeni: AVAudioPlayer = try AVAudioPlayer(contentsOf: adres)
            yeni.volume = sesDuzeyi
            yeni.prepareToPlay()
            if yeni.play() {
                calici = yeni
            }
        } catch {
            // Çözülemeyen dosya: sessiz geç (Android'de de hata yalnızca günlüğe yazılır).
            calici = nil
        }
    }

    // MARK: Yaşam döngüsü (Android onPause / onResume)

    private func arkaPlanaGecildi() {
        guard let c = calici, c.isPlaying else { return }
        c.pause()
        yasamDongusuyleDuraklatildi = true
    }

    private func onPlanaDonuldu() {
        guard yasamDongusuyleDuraklatildi else { return }
        yasamDongusuyleDuraklatildi = false
        _ = calici?.play()
    }
}

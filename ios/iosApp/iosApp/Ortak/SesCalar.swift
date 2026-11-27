import AVFoundation
import Foundation

// Uygulamanın ortak ses efekti çalıcısı.
//
// Android karşılığı: AbacusSoundPlayer, BadgeSoundPlayer, ChestSoundPlayer, LessonResultSoundPlayer
// (SoundPool, USAGE_GAME) ve ekranların kendi MediaPlayer'ları. Hepsi aynı kalıbı izler:
//  - "AppPrefs" / "sound_enabled" (varsayılan açık) kapalıysa hiçbir şey çalmaz;
//  - ses dosyası res/raw'da ada göre bulunur (iOS'ta paket kökünde, uzantıyla); yoksa sessiz geçilir;
//  - ses düzeyi (setVolume / play volume) ve perde (SoundPool rate) her çalmada verilir;
//  - SoundPool eşzamanlı akış çalar (boncuk tıkları hızlı art arda gelir) → ad başına küçük oynatıcı
//    havuzu: boştaki oynatıcı kullanılır, hepsi doluysa en ilerlemiş olan baştan çalınır.
//
// Perde notu: Android SoundPool rate hem hızı hem perdeyi değiştirir; AVAudioPlayer `rate` (enableRate)
// hızı değiştirir ve sistem perdeyi korumaya çalışabilir. Bu fark cihazda dinlenmeden doğrulanamaz.
//
// İş parçacığı: yöntemler ana iş parçacığından çağrılır; başka iş parçacığından gelirse ana iş
// parçacığına aktarılır (AVAudioPlayer durumu yalnızca orada değişir).

/// Ses oturumunu kuran tek yer. Android MediaPlayer/SoundPool ses odağı istemiyor, medya akışından
/// çalıyor (telefonun zil sessizliğinden etkilenmez) ve başka uygulamanın müziğini kesmiyor; iOS
/// karşılığı: `.playback` kategorisi (sessiz anahtar çalmayı engellemez) + `.mixWithOthers`.
/// Birden çok çalıcı çağırabilir; kurulum yalnızca bir kez yapılır.
enum SesOturumu {
    private static var kuruldu: Bool = false

    static func kur() {
        if kuruldu { return }
        kuruldu = true
        let oturum: AVAudioSession = AVAudioSession.sharedInstance()
        do {
            try oturum.setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try oturum.setActive(true)
        } catch {
            // Oturum kurulamazsa varsayılan oturumla denenir; hata sessizce geçilir.
        }
    }
}

final class SesCalar {
    /// Uygulama genelinde tek örnek.
    static let ortak: SesCalar = SesCalar()

    /// Android AppPrefs "sound_enabled" (ses efektleri; varsayılan açık).
    static let sesAcikAnahtari: String = "sound_enabled"

    /// Çalınabilecek uzantılar, sırayla denenir.
    private static let uzantilar: [String] = ["mp3", "wav", "m4a", "flac"]
    /// Ad başına en çok oynatıcı sayısı (Android abaküs SoundPool: 4 akış).
    private static let havuzSiniri: Int = 4

    private let ayarlar: UserDefaults
    private var havuzlar: [String: [AVAudioPlayer]] = [:]
    private var adresler: [String: URL] = [:]
    private var bulunamayanlar: Set<String> = []
    /// Ekran galerisi (`-ekran <ad>`) kipinde ses çalınmaz: galeri sahte uygulamalarla çalışır.
    private let galeriKipi: Bool = EkranGalerisi.istenenAd != nil

    init(ayarlar: UserDefaults = UserDefaults.standard) {
        self.ayarlar = ayarlar
    }

    /// Anahtar yoksa varsayılan açık (Android getBoolean(…, true)).
    func ayarAcikMi(_ anahtar: String) -> Bool {
        if let deger = ayarlar.object(forKey: anahtar) as? Bool {
            return deger
        }
        return true
    }

    /// Sesi önceden yükler (Android preload): ilk çalmada gecikme olmasın.
    func hazirla(ad: String) {
        anaIsParcacigindaCalistir { [weak self] in
            guard let self = self else { return }
            guard let adres = self.dosyaAdresi(ad) else { return }
            SesOturumu.kur()
            let mevcut: [AVAudioPlayer] = self.havuzlar[ad] ?? []
            if mevcut.isEmpty, let yeni = SesCalar.yeniCalici(adres) {
                self.havuzlar[ad] = [yeni]
            }
        }
    }

    /// Sesi çalar. `sesDuzeyi` 0...1 (Android play volume); `hiz` oynatma hızı/perdesi (1,0 normal;
    /// AVAudioPlayer 0,5...2,0 dışını kabul etmez, kırpılır). `ayarAnahtari` kapalıysa çalmaz.
    func cal(
        ad: String,
        sesDuzeyi: Float = 1.0,
        hiz: Float = 1.0,
        ayarAnahtari: String = SesCalar.sesAcikAnahtari
    ) {
        anaIsParcacigindaCalistir { [weak self] in
            self?.calIc(ad: ad, sesDuzeyi: sesDuzeyi, hiz: hiz, ayarAnahtari: ayarAnahtari)
        }
    }

    /// Ad başına çalan bütün sesleri susturur.
    func sustur(ad: String) {
        anaIsParcacigindaCalistir { [weak self] in
            guard let havuz = self?.havuzlar[ad] else { return }
            for c in havuz {
                c.stop()
            }
        }
    }

    // MARK: İç işler

    private func calIc(ad: String, sesDuzeyi: Float, hiz: Float, ayarAnahtari: String) {
        if galeriKipi { return }
        guard ayarAcikMi(ayarAnahtari) else { return }
        guard let adres = dosyaAdresi(ad) else { return }
        SesOturumu.kur()
        guard let c = bosCalici(ad: ad, adres: adres) else { return }
        c.volume = min(max(sesDuzeyi, 0.0), 1.0)
        c.rate = min(max(hiz, 0.5), 2.0)
        c.currentTime = 0
        _ = c.play()
    }

    private func bosCalici(ad: String, adres: URL) -> AVAudioPlayer? {
        var havuz: [AVAudioPlayer] = havuzlar[ad] ?? []
        for c in havuz {
            if !c.isPlaying {
                return c
            }
        }
        if havuz.count < SesCalar.havuzSiniri, let yeni = SesCalar.yeniCalici(adres) {
            havuz.append(yeni)
            havuzlar[ad] = havuz
            return yeni
        }
        // Hepsi çalıyor: en ilerlemiş olan baştan çalınır.
        return havuz.max(by: { (a: AVAudioPlayer, b: AVAudioPlayer) -> Bool in
            return a.currentTime < b.currentTime
        })
    }

    private static func yeniCalici(_ adres: URL) -> AVAudioPlayer? {
        do {
            let c: AVAudioPlayer = try AVAudioPlayer(contentsOf: adres)
            c.enableRate = true
            c.prepareToPlay()
            return c
        } catch {
            // Çözülemeyen dosya: sessiz geç (Android'de de hata yalnızca günlüğe yazılır).
            return nil
        }
    }

    private func dosyaAdresi(_ ad: String) -> URL? {
        if let bilinen = adresler[ad] {
            return bilinen
        }
        if bulunamayanlar.contains(ad) {
            return nil
        }
        for uzanti in SesCalar.uzantilar {
            if let adres = Bundle.main.url(forResource: ad, withExtension: uzanti) {
                adresler[ad] = adres
                return adres
            }
        }
        bulunamayanlar.insert(ad)
        return nil
    }

    private func anaIsParcacigindaCalistir(_ isi: @escaping () -> Void) {
        if Thread.isMainThread {
            isi()
        } else {
            DispatchQueue.main.async(execute: isi)
        }
    }
}

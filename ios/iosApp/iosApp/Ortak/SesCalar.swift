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
// İş parçacığı: yöntemler her iş parçacığından çağrılabilir ve ateşle-ve-unutturur (çağıran beklemez).
// Bütün AVFoundation işleri (oturum kurulumu, oynatıcı kurma/hazırlama/çalma/susturma, havuz) seri
// `SesOturumu.kuyruk` üzerinde yapılır; havuz ve adres önbelleği yalnızca orada okunur/yazılır.
// Neden: `AVAudioSession.setActive` ve `AVAudioPlayer(contentsOf:)` ses sunucusuyla eşzamanlı konuşur ve
// ana iş parçacığını saniyelerce bekletebilir (bkz. öğretici çalıcısı, 13.-16. derlemelerde boş ekran).
// Ayar okuma (UserDefaults) ve galeri kipi denetimi ucuz olduğundan çağıran tarafta kalır.

/// Ses oturumunu kuran tek yer. Android MediaPlayer/SoundPool ses odağı istemiyor, medya akışından
/// çalıyor (telefonun zil sessizliğinden etkilenmez) ve başka uygulamanın müziğini kesmiyor; iOS
/// karşılığı: `.playback` kategorisi (sessiz anahtar çalmayı engellemez) + `.mixWithOthers`.
/// Birden çok çalıcı çağırabilir; kurulum yalnızca bir kez yapılır.
///
/// Ana iş parçacığını BEKLETMEZ: `setActive` ses sunucusuyla (coreaudiod) eşzamanlı konuşur ve
/// simülatörde/ilk açılışta saniyelerce sürebilir (Apple da oturumu ana iş parçacığı dışında etkinleştirmeyi
/// önerir). Kurulum `kuyruk` üzerinde sırayla yapılır; bütün çalıcılar (öğretici ve SesCalar) aynı
/// kuyrukta çalar ve ilk çalmadan önce kurulumu yapar.
enum SesOturumu {
    /// Ses işlerinin seri kuyruğu (oturum kurulumu + öğretici çalıcısı).
    static let kuyruk: DispatchQueue = DispatchQueue(label: "com.sorobit.app.ses", qos: .userInitiated)
    /// Yalnızca `kuyruk` üzerinde okunur/yazılır.
    private static var kuruldu: Bool = false

    /// Kurulumu ses kuyruğuna sıraya koyar; çağıran beklemez (SesCalar ana iş parçacığından çağırır).
    static func kur() {
        SesOturumu.kuyruk.async {
            SesOturumu.kurIc()
        }
    }

    /// Yalnızca `kuyruk` üzerinde çalışan bir işin içinden (öğretici çalıcısı): kurulumu sıraya koymadan
    /// hemen yapar; böylece aynı işte hemen ardından gelen çalma kurulmuş oturumla çalar.
    static func kuyruktaKur() {
        dispatchPrecondition(condition: .onQueue(SesOturumu.kuyruk))
        SesOturumu.kurIc()
    }

    private static func kurIc() {
        if kuruldu { return }
        kuruldu = true
        let baslangic: Date = Date()
        let oturum: AVAudioSession = AVAudioSession.sharedInstance()
        do {
            try oturum.setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try oturum.setActive(true)
        } catch {
            // Oturum kurulamazsa varsayılan oturumla denenir; hata sessizce geçilir.
        }
        let sureMs: Int = Int(Date().timeIntervalSince(baslangic) * 1000.0)
        YonetimGunlugu.yaz("ses oturumu kuruldu (\(sureMs) ms)")
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
    // Aşağıdaki üç durum ve `ilkKurulumYazildi` YALNIZCA `SesOturumu.kuyruk` üzerinde okunur/yazılır.
    private var havuzlar: [String: [AVAudioPlayer]] = [:]
    private var adresler: [String: URL] = [:]
    private var bulunamayanlar: Set<String> = []
    /// İlk oynatıcı kurulum süresi günlüğe bir kez yazılır.
    private var ilkKurulumYazildi: Bool = false
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
        SesOturumu.kuyruk.async { [weak self] in
            guard let self = self else { return }
            guard let adres = self.dosyaAdresi(ad) else { return }
            SesOturumu.kuyruktaKur()
            let mevcut: [AVAudioPlayer] = self.havuzlar[ad] ?? []
            if mevcut.isEmpty, let yeni = self.yeniCalici(adres) {
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
        // Ucuz denetimler çağıran tarafta: kapalıysa kuyruğa hiç iş konmaz.
        if galeriKipi { return }
        guard ayarAcikMi(ayarAnahtari) else { return }
        SesOturumu.kuyruk.async { [weak self] in
            self?.calKuyrukta(ad: ad, sesDuzeyi: sesDuzeyi, hiz: hiz)
        }
    }

    /// Ad başına çalan bütün sesleri susturur.
    func sustur(ad: String) {
        SesOturumu.kuyruk.async { [weak self] in
            guard let havuz = self?.havuzlar[ad] else { return }
            for c in havuz {
                c.stop()
            }
        }
    }

    // MARK: İç işler (hepsi `SesOturumu.kuyruk` üzerinde çalışır)

    private func calKuyrukta(ad: String, sesDuzeyi: Float, hiz: Float) {
        guard let adres = dosyaAdresi(ad) else { return }
        // Oturum, aynı kuyrukta ilk çalmadan önce kurulur (öğretici çalıcısıyla aynı sıra güvencesi).
        SesOturumu.kuyruktaKur()
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
        if havuz.count < SesCalar.havuzSiniri, let yeni = yeniCalici(adres) {
            havuz.append(yeni)
            havuzlar[ad] = havuz
            return yeni
        }
        // Hepsi çalıyor: en ilerlemiş olan baştan çalınır.
        return havuz.max(by: { (a: AVAudioPlayer, b: AVAudioPlayer) -> Bool in
            return a.currentTime < b.currentTime
        })
    }

    private func yeniCalici(_ adres: URL) -> AVAudioPlayer? {
        let baslangic: Date = Date()
        do {
            let c: AVAudioPlayer = try AVAudioPlayer(contentsOf: adres)
            c.enableRate = true
            c.prepareToPlay()
            if !ilkKurulumYazildi {
                ilkKurulumYazildi = true
                let sureMs: Int = Int(Date().timeIntervalSince(baslangic) * 1000.0)
                YonetimGunlugu.yaz("ses calici (SesCalar) ilk oynatici kuruldu (\(sureMs) ms)")
            }
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
}

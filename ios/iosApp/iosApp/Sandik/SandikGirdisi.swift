import Foundation

// Sandık ekranının girdisi, sonucu ve dış bağımlılıkları (ödül sunucusu, ses, reklam).
//
// Android'de ders sonrası akış: ChestFragment (kendisi görünmez; ilerlemeyi yazar) →
// NewChestFragment (görünen sandık mini oyunu). iOS'ta `SandikEkrani` yalnızca görünen kısımdır.

/// Sandık ekranına aktarılan değerler (Android: NewChestFragment.newInstance argümanları).
struct SandikGirdisi {
    /// Başlangıç nadirliği: "COMMON", "RARE", "EPIC", "LEGENDARY" (Android: ARG_START_RARITY).
    /// Tanınmayan ad COMMON sayılır. Ders sonrası sandıkta her zaman "COMMON".
    var baslangicNadirligi: String = "COMMON"

    /// Reklamla kazanılan sandıklarda reklam yöneticisinin ürettiği tek kullanımlık değer
    /// (Android: ARG_AD_NONCE). Ders ve görev sandıklarında `nil`.
    var reklamNonce: String? = nil

    init(baslangicNadirligi: String = "COMMON", reklamNonce: String? = nil) {
        self.baslangicNadirligi = baslangicNadirligi
        self.reklamNonce = reklamNonce
    }
}

/// Sandık ekranı kapanırken dışarı verilen sonuç (Android: "chest_closed" FragmentResult).
struct SandikSonucu {
    /// Sunucu sonucu geldi mi. `false` ise sandık açılamadı: ödül verilmedi, Android bu durumda
    /// `SandikSonucu.acilamadiMetni` içeriğiyle bir Toast gösterip ekranı kapatır.
    let acildi: Bool
    /// Android: RESULT_EARNED_GOLD
    let kazanilanAltin: Int
    /// Android: RESULT_EARNED_KEY
    let kazanilanAnahtar: Int
    /// Kapanıştaki nadirlik adı ("COMMON" …).
    let sonNadirlik: String

    /// NewChestFragment.requestOutcomeFromServer hata dalındaki Toast metni.
    static let acilamadiMetni = "Sandık açılamadı. Tekrar deneyin."
}

/// Sunucunun sandık yanıtı (ortak kod: ServerRewards.ChestOutcome).
struct SandikSunucuSonucu {
    /// Kullanıcının üç dokunuşunda sırayla gösterilecek nadirlik adları.
    var nadirlikYolu: [String]
    var sonNadirlik: String
    /// "GOLD" ya da "KEY"
    var odulTuru: String
    var odulMiktari: Int
}

/// Sandık ödülünü veren sunucu çağrısı.
///
/// Android: ServerRewards.openChest → Cloud Function `openChest` (gövde: `startRarity`, varsa
/// `adNonce`; yanıt: `rarityPath`, `finalRarity`, `rewardType`, `rewardAmount`). Zar sunucuda
/// atılır ve ödül bu çağrıda bakiyeye YAZILIR (`users/{uid}.currency` / `keys`); ekran yalnızca
/// sonucu oynatır. Gerçek uygulama ortak koddaki `ServerRewards.ChestPrefetcher` üzerinden
/// çağırmalıdır: aynı sandık için ikinci kez ağa gidilmemesi oradaki kuralla sağlanır.
protocol SandikOdulServisi {
    func sandikAc(
        baslangicNadirligi: String,
        reklamNonce: String?,
        tamamla: @escaping (Result<SandikSunucuSonucu, Error>) -> Void
    )
}

/// Örnek sonuç döndüren sahte sunucu.
final class SahteSandikOdulServisi: SandikOdulServisi {
    private let sonuc: SandikSunucuSonucu
    private let gecikme: Double

    init(
        sonuc: SandikSunucuSonucu = SandikSunucuSonucu(
            nadirlikYolu: ["COMMON", "RARE", "EPIC"],
            sonNadirlik: "EPIC",
            odulTuru: "GOLD",
            odulMiktari: 150
        ),
        gecikme: Double = 0.4
    ) {
        self.sonuc = sonuc
        self.gecikme = gecikme
    }

    func sandikAc(
        baslangicNadirligi: String,
        reklamNonce: String?,
        tamamla: @escaping (Result<SandikSunucuSonucu, Error>) -> Void
    ) {
        let yanit = sonuc
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme) {
            tamamla(.success(yanit))
        }
    }
}

/// Sandık ve kristal ekranlarının sesleri. "Ses açık" tercihi (Android: AppPrefs /
/// "sound_enabled") gerçek uygulamanın içinde denetlenmeli.
protocol SandikSesCalar {
    /// Ekran açılırken sesleri hazırlar (Android: ChestSoundPlayer.preload).
    func hazirla()
    /// Adım ve açılış dokunuşu. Android: res/raw/chest_level_sound.mp3, ses düzeyi 0.7;
    /// `hiz` oynatma hızıdır (nadirlik yükseldikçe incelir: 1.0, 1.25, 1.5, 1.75).
    func seviyeSesiCal(hiz: Float)
    /// Ödül görünürken bir kez. Android: res/raw/reward_sound.mp3, ses düzeyi 0.7.
    func odulSesiCal()
    /// Kristal videosunda her dokunuş. Android: res/raw/crystal_click1.mp3, ses düzeyi 0.5.
    func kristalTikSesiCal()
    /// Kristal videosunun kendi ses düzeyi, 0…1. Android: "ses açık" ise 0.5, kapalıysa 0
    /// (CrystalBreakVideoFragment: `player.volume = if (soundEnabled) 0.5f else 0f`).
    /// Varsayılan uygulaması 0.5 döndürür; gerçek uygulama tercihe göre 0 döndürmeli.
    var videoSesDuzeyi: Float { get }
}

extension SandikSesCalar {
    var videoSesDuzeyi: Float {
        return 0.5
    }
}

struct SahteSandikSesCalar: SandikSesCalar {
    func hazirla() {}
    func seviyeSesiCal(hiz: Float) {}
    func odulSesiCal() {}
    func kristalTikSesiCal() {}
}

/// Ödüllü reklam.
///
/// Sandık ekranının KENDİSİ reklam göstermez ve Android'de "reklamla ikiye katla" akışı yoktur.
/// Reklamın sandıkla tek ilişkisi şudur: reklamla kazanılan sandıkta çağıran taraf önce reklamı
/// gösterir, reklam yöneticisinin ürettiği değeri `SandikGirdisi.reklamNonce` olarak verir;
/// sunucu bu değerle AdMob sunucu doğrulamasından gelen hakkı bozdurur (ortak kod: AdRules,
/// ServerRewards.shouldRetryOpenChest). Bu protokol o çağıran taraf içindir.
protocol SandikReklamServisi {
    /// Ödüllü reklamı gösterir; izlendiyse sandık için kullanılacak değeri, izlenmediyse `nil` verir.
    func odulluReklamGoster(tamamla: @escaping (String?) -> Void)
}

/// Reklam göstermeden hemen sahte bir değer döndürür.
struct SahteSandikReklamServisi: SandikReklamServisi {
    func odulluReklamGoster(tamamla: @escaping (String?) -> Void) {
        tamamla("sahte-reklam-nonce")
    }
}

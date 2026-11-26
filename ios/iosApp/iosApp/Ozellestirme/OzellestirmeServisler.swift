import Foundation
import Shared

// Özelleştirme ekranının dış dünyayla konuştuğu her şey bu dosyadaki protokollerin arkasındadır.
// Firebase henüz bağlı olmadığı için sahiplik kayıtları bellekte tutulan sahte bir serviste durur.
// Cüzdan için Mağaza klasöründeki `MagazaCuzdanServisi` kullanılır (iki ekran aynı bakiyeyi görür).

// MARK: - Sahiplik kayıtları

/// Bir boncuk tipinin sahiplik kaydı.
/// Firestore: `users/{uid}/haveBead/{beadId}` — alanlar `count`, `colorFeatureActive`, `purchasedAt`
/// (beadId = BeadType adı, ör. "SOROBAN2"). Dokümandan okuma kuralı ortak kodda:
/// BeadPurchaseFirestore.beadDataFromDocument.
struct OzellestirmeBoncukKaydi: Equatable {
    var adet: Int
    var renkOzelligi: Bool
}

/// Bir çerçeve tipinin sahiplik kaydı.
/// Firestore: `users/{uid}/haveFrame/{frameId}` — alanlar `purchasedAt`, `colorFeatureActive`
/// (frameId = FrameType adı, ör. "FRAME_BG2"). Okuma kuralı: FramePurchaseFirestore.frameDataFromDocument.
struct OzellestirmeCerceveKaydi: Equatable {
    var sahip: Bool
    var renkOzelligi: Bool
}

/// Android: BeadPurchaseFirestore, FramePurchaseFirestore ve AbacusCustomizationFirestore'un
/// okuma/yazma çağrıları. Yazılacak alan adları ve veriler ortak koddadır
/// (BeadPurchaseFirestore.beadCountData, colorFeatureActiveData, AbacusCustomizationFirestore.customizationData).
protocol OzellestirmeSahiplikServisi: AnyObject {
    /// Android: BeadPurchaseFirestore.loadOwnedBeads. Hata olursa boş sözlük döner.
    func boncuklariYukle(tamam: @escaping ([String: OzellestirmeBoncukKaydi]) -> Void)
    /// Android: FramePurchaseFirestore.loadOwnedFrames. Hata olursa boş sözlük döner.
    func cerceveleriYukle(tamam: @escaping ([String: OzellestirmeCerceveKaydi]) -> Void)
    /// Android: BeadPurchaseFirestore.setBeadCount (merge; `purchasedAt` = şimdi eklenir).
    func boncukAdediniYaz(boncukKimligi: String, adet: Int, tamam: @escaping (Bool) -> Void)
    /// Android: BeadPurchaseFirestore.setColorFeatureActive.
    func boncukRenkOzelliginiAc(boncukKimligi: String, tamam: @escaping (Bool) -> Void)
    /// Android: FramePurchaseFirestore.markFrameOwned (`purchasedAt` yazılır).
    func cerceveyiSahiplen(cerceveKimligi: String, tamam: @escaping (Bool) -> Void)
    /// Android: FramePurchaseFirestore.setColorFeatureActive.
    func cerceveRenkOzelliginiAc(cerceveKimligi: String, tamam: @escaping (Bool) -> Void)
    /// Android: AbacusCustomizationFirestore.saveCustomization — `users/{uid}/abacusCustomization/state`
    /// dokümanına merge ile yazılır; servis ayrıca `updatedAt` = şimdi ekler.
    func ozellestirmeyiKaydet(veri: [String: Any])
}

// MARK: - Yerel ayar deposu

/// Ortak koddaki `AbacusPrefsStore`'un UserDefaults ile uygulanması (Android: hesaba özel
/// "abacus_customization_{uid}" SharedPreferences dosyası). Abaküs ekranları da aynı ayarları
/// okumak için aynı depo adını kullanmalıdır: AbacusPreferences.prefsFileName(uid:).
final class OzellestirmeYerelDepo: NSObject, AbacusPrefsStore {
    private let depo: UserDefaults

    init(kullaniciKimligi: String?) {
        let ad: String = AbacusPreferences.shared.prefsFileName(uid: kullaniciKimligi)
        depo = UserDefaults(suiteName: ad) ?? UserDefaults.standard
        super.init()
    }

    func getInt(key: String, defaultValue: Int32) -> Int32 {
        guard let sayi = depo.object(forKey: key) as? NSNumber else { return defaultValue }
        return sayi.int32Value
    }

    func getString(key: String, defaultValue: String?) -> String? {
        guard let metin = depo.object(forKey: key) as? String else { return defaultValue }
        return metin
    }

    func putInt(key: String, value: Int32) {
        depo.set(NSNumber(value: value), forKey: key)
    }

    func putString(key: String, value: String) {
        depo.set(value, forKey: key)
    }

    func remove(key: String) {
        depo.removeObject(forKey: key)
    }
}

// MARK: - Bağımlılıklar

struct OzellestirmeBagimliliklar {
    let sahiplik: OzellestirmeSahiplikServisi
    let cuzdan: MagazaCuzdanServisi
    /// Abaküs ayarlarının yerel deposu.
    let depo: AbacusPrefsStore
    /// Oturum açmış kullanıcının kimliği (Android: FirebaseAuth.currentUser.uid).
    /// `nil` iken Android'de satın alma ve yuva panelleri açılmaz; burada da açılmaz.
    let kullaniciKimligi: String?

    /// Sahte servisler; cüzdan Mağaza ekranıyla ortaktır.
    static let sahte: OzellestirmeBagimliliklar = {
        let kimlik = "sahte-kullanici"
        return OzellestirmeBagimliliklar(
            sahiplik: OzellestirmeSahteSahiplikServisi(),
            cuzdan: MagazaBagimliliklar.sahte.cuzdan,
            depo: OzellestirmeYerelDepo(kullaniciKimligi: kimlik),
            kullaniciKimligi: kimlik
        )
    }()
}

// MARK: - Sahte uygulama

/// Sahiplik kayıtlarını bellekte tutar; uygulama kapanınca sıfırlanır. Başlangıçta hiçbir
/// ücretli boncuk ya da çerçeve alınmamıştır.
final class OzellestirmeSahteSahiplikServisi: OzellestirmeSahiplikServisi {
    private var boncuklar: [String: OzellestirmeBoncukKaydi] = [:]
    private var cerceveler: [String: OzellestirmeCerceveKaydi] = [:]
    /// En son kaydedilen özelleştirme (yalnızca hata ayıklarken bakmak için).
    private(set) var sonKaydedilen: [String: Any] = [:]

    func boncuklariYukle(tamam: @escaping ([String: OzellestirmeBoncukKaydi]) -> Void) {
        let kopya = boncuklar
        DispatchQueue.main.async { tamam(kopya) }
    }

    func cerceveleriYukle(tamam: @escaping ([String: OzellestirmeCerceveKaydi]) -> Void) {
        let kopya = cerceveler
        DispatchQueue.main.async { tamam(kopya) }
    }

    func boncukAdediniYaz(boncukKimligi: String, adet: Int, tamam: @escaping (Bool) -> Void) {
        var kayit = boncuklar[boncukKimligi] ?? OzellestirmeBoncukKaydi(adet: 0, renkOzelligi: false)
        kayit.adet = adet
        boncuklar[boncukKimligi] = kayit
        gecikmeyle(tamam)
    }

    func boncukRenkOzelliginiAc(boncukKimligi: String, tamam: @escaping (Bool) -> Void) {
        var kayit = boncuklar[boncukKimligi] ?? OzellestirmeBoncukKaydi(adet: 0, renkOzelligi: false)
        kayit.renkOzelligi = true
        boncuklar[boncukKimligi] = kayit
        gecikmeyle(tamam)
    }

    func cerceveyiSahiplen(cerceveKimligi: String, tamam: @escaping (Bool) -> Void) {
        var kayit = cerceveler[cerceveKimligi] ?? OzellestirmeCerceveKaydi(sahip: false, renkOzelligi: false)
        kayit.sahip = true
        cerceveler[cerceveKimligi] = kayit
        gecikmeyle(tamam)
    }

    func cerceveRenkOzelliginiAc(cerceveKimligi: String, tamam: @escaping (Bool) -> Void) {
        var kayit = cerceveler[cerceveKimligi] ?? OzellestirmeCerceveKaydi(sahip: false, renkOzelligi: false)
        kayit.renkOzelligi = true
        cerceveler[cerceveKimligi] = kayit
        gecikmeyle(tamam)
    }

    func ozellestirmeyiKaydet(veri: [String: Any]) {
        sonKaydedilen = veri
    }

    private func gecikmeyle(_ tamam: @escaping (Bool) -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            tamam(true)
        }
    }
}

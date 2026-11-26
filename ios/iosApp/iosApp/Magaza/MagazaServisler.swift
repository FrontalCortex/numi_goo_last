import Foundation
import Shared

// Mağazanın dış dünyayla konuştuğu her şey (ödeme, cüzdan, can, reklam) bu dosyadaki
// protokollerin arkasındadır. Firebase, StoreKit ve reklam SDK'sı henüz bağlı olmadığı için
// her protokolün örnek veriyle çalışan sahte bir uygulaması vardır (Sahte…).
// Özelleştirme ekranı da cüzdan için buradaki `MagazaCuzdanServisi`'ni kullanır.

// MARK: - Ödeme (StoreKit 2 bağlanacak)

/// Mağazadan gelen ürün bilgisi.
struct MagazaUrun: Equatable {
    /// Ürün kimliği (BillingCatalog; App Store'da Play ile aynı).
    let kimlik: String
    /// Mağazanın yerelleştirdiği fiyat (StoreKit: `Product.displayPrice`).
    let fiyatMetni: String
}

enum MagazaSatinAlmaSonucu: Equatable {
    /// Ödeme alındı VE sunucu ödülü verdi (cüzdan/plan güncellendi).
    case verildi
    /// Kullanıcı vazgeçti; hiçbir şey gösterilmez.
    case iptal
    /// Onay bekliyor (StoreKit: `.pending`, ör. Aile Paylaşımı onayı); hiçbir şey gösterilmez.
    case beklemede
    /// Kullanıcıya gösterilecek hata metni.
    case hata(String)
}

/// Android: BillingManager. StoreKit 2 ile uygulanırken:
/// - `urunleriGetir` → `Product.products(for:)`
/// - `satinAl` → `product.purchase()`; doğrulanmış işlemin JWS'i sunucuya gönderilir, sunucu
///   ödülü verdikten SONRA `transaction.finish()` çağrılır (Android: redeemGooglePlayPurchase /
///   redeemGooglePlaySubscription, ardından consume / acknowledge).
/// - `geriYukle` → `AppStore.sync()` + `Transaction.currentEntitlements`
/// Uygulama açılışında `Transaction.updates` dinlenmeli ve bitmemiş işlemler yeniden
/// sunucuya gönderilmelidir (Android: BillingManager.refreshPurchases).
protocol MagazaOdemeServisi: AnyObject {
    /// Ürün bilgilerini getirir. Bulunamayan kimlikler sonuçta yer almaz; hata olursa boş döner.
    func urunleriGetir(kimlikler: [String], tamam: @escaping ([MagazaUrun]) -> Void)
    /// Satın alma akışını başlatır.
    func satinAl(urunKimligi: String, tamam: @escaping (MagazaSatinAlmaSonucu) -> Void)
    /// Önceki satın alımları (abonelik) geri yükler; `true` = en az bir hak geri geldi.
    func geriYukle(tamam: @escaping (Bool) -> Void)
}

// MARK: - Cüzdan

struct MagazaCuzdan: Equatable {
    /// Altın. Firestore: `users/{uid}.currency`.
    var altin: Int
    /// Anahtar. Firestore: `users/{uid}.keys`.
    var anahtar: Int
}

struct MagazaCuzdanDegisimi {
    let cuzdan: MagazaCuzdan
    /// Harcamayı geri almak için sunucunun verdiği tek kullanımlık jeton.
    let geriAlmaJetonu: String?
}

/// Android: UserWalletFirestore. Bakiye yalnızca sunucuda değişir
/// (Cloud Function `updateUserWallet`; gövde: UserWalletFirestore.applyDeltaPayload).
protocol MagazaCuzdanServisi: AnyObject {
    /// Son bilinen bakiye (Android: getCachedCurrency / getCachedKeys).
    var cuzdan: MagazaCuzdan { get }
    /// Bakiyeyi sunucudan okur (Android: loadWallet). Hata olursa önbellekteki değer döner.
    func yukle(tamam: @escaping (MagazaCuzdan) -> Void)
    /// Bakiyeyi değiştirir (Android: applyCurrencyDelta / applyKeyDelta). `nil` = başarısız.
    /// `gerekce`: WalletReason.SPEND ya da PURCHASE_ROLLBACK.
    func degistir(
        altinFarki: Int,
        anahtarFarki: Int,
        gerekce: String,
        geriAlmaJetonu: String?,
        tamam: @escaping (MagazaCuzdanDegisimi?) -> Void
    )
    /// Bakiye her değiştiğinde çağrılır (Android: listenToWallet). Dönen kimlikle bırakılır.
    func dinle(_ dinleyici: @escaping (MagazaCuzdan) -> Void) -> UUID
    func dinlemeyiBirak(_ kimlik: UUID)
}

// MARK: - Can (enerji)

struct MagazaEnerjiDurumu: Equatable {
    /// Android: EnergyManager.getCurrentEnergy().
    var enerji: Int
    /// Android: EnergyManager.getMaxEnergy().
    var azami: Int
    /// Android: MainActivity.isInfiniteEnergy().
    var sonsuz: Bool
    /// Android: EnergyManager.getTimeUntilNextEnergy() (ms).
    var sonrakiCanaKalanMs: Int64
}

/// Android: EnergyManager + ServerEnergy. Gerçek uygulamada ortak koddaki `EnergyManager`
/// kullanılmalı; sunucu yanıtındaki `energyFullTime` `adoptServerFullTime` ile benimsenmelidir.
protocol MagazaEnerjiServisi: AnyObject {
    func durum() -> MagazaEnerjiDurumu
    /// Reklam izlenerek 1 can (Cloud Function `claimAdEnergy`, gövde: ServerEnergy.claimAdEnergyPayload).
    func reklamlaCanAl(reklamNonce: String, tamam: @escaping (Bool) -> Void)
    /// Anahtar karşılığı 1 can (Cloud Function `buyEnergyWithKeys`). Sonuç: kalan anahtar; `nil` = başarısız.
    func anahtarlaCanAl(tamam: @escaping (Int?) -> Void)
}

// MARK: - Reklam

/// Android: AdManager.showRewardedAd(showAdSkipAfter = false).
protocol MagazaReklamServisi: AnyObject {
    /// Ödüllü reklamı gösterir. Ödül kazanıldıysa sunucuda bozdurulacak nonce döner
    /// (SSV özel verisi: AdRules.ssvCustomData); reklam hazır değilse ya da ödül kazanılmadıysa
    /// `hataMetni` dolu ya da ikisi de boş gelir.
    func odulluReklamGoster(tamam: @escaping (_ nonce: String?, _ hataMetni: String?) -> Void)
}

// MARK: - Bağımlılıklar

struct MagazaBagimliliklar {
    let odeme: MagazaOdemeServisi
    let cuzdan: MagazaCuzdanServisi
    let enerji: MagazaEnerjiServisi
    let reklam: MagazaReklamServisi

    /// Bütün uygulamada paylaşılan sahte servisler (cüzdan Özelleştirme ekranıyla ortaktır).
    static let sahte: MagazaBagimliliklar = {
        let cuzdan = MagazaSahteCuzdanServisi()
        let enerji = MagazaSahteEnerjiServisi(cuzdan: cuzdan)
        return MagazaBagimliliklar(
            odeme: MagazaSahteOdemeServisi(cuzdan: cuzdan, enerji: enerji),
            cuzdan: cuzdan,
            enerji: enerji,
            reklam: MagazaSahteReklamServisi()
        )
    }()
}

// MARK: - Sahte uygulamalar

/// Bellekte tutulan cüzdan. Başlangıç bakiyesi satın alma akışları denenebilsin diye yüksektir.
final class MagazaSahteCuzdanServisi: MagazaCuzdanServisi {
    private(set) var cuzdan = MagazaCuzdan(altin: 12_000, anahtar: 45)
    private var dinleyiciler: [UUID: (MagazaCuzdan) -> Void] = [:]

    func yukle(tamam: @escaping (MagazaCuzdan) -> Void) {
        let deger = cuzdan
        DispatchQueue.main.async { tamam(deger) }
    }

    func degistir(
        altinFarki: Int,
        anahtarFarki: Int,
        gerekce: String,
        geriAlmaJetonu: String?,
        tamam: @escaping (MagazaCuzdanDegisimi?) -> Void
    ) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            let yeniAltin: Int = self.cuzdan.altin + altinFarki
            let yeniAnahtar: Int = self.cuzdan.anahtar + anahtarFarki
            // Sunucu eksiye düşen harcamayı reddeder.
            if yeniAltin < 0 || yeniAnahtar < 0 {
                tamam(nil)
                return
            }
            self.yaz(MagazaCuzdan(altin: yeniAltin, anahtar: yeniAnahtar))
            // Harcamalarda geri alma jetonu üretilir.
            let harcamaMi: Bool = altinFarki < 0 || anahtarFarki < 0
            let jeton: String? = harcamaMi ? UUID().uuidString : nil
            tamam(MagazaCuzdanDegisimi(cuzdan: self.cuzdan, geriAlmaJetonu: jeton))
        }
    }

    func dinle(_ dinleyici: @escaping (MagazaCuzdan) -> Void) -> UUID {
        let kimlik = UUID()
        dinleyiciler[kimlik] = dinleyici
        return kimlik
    }

    func dinlemeyiBirak(_ kimlik: UUID) {
        dinleyiciler[kimlik] = nil
    }

    /// Sunucunun bakiyeyi kendisinin değiştirdiği durumlar (paket satın alma, anahtarla can).
    func sunucuDegistirdi(altinFarki: Int, anahtarFarki: Int) {
        yaz(MagazaCuzdan(altin: cuzdan.altin + altinFarki, anahtar: cuzdan.anahtar + anahtarFarki))
    }

    private func yaz(_ yeni: MagazaCuzdan) {
        cuzdan = yeni
        for dinleyici in dinleyiciler.values {
            dinleyici(yeni)
        }
    }
}

/// Bellekte tutulan can durumu: 5 canın 3'ü dolu, sıradaki can 10 dakikalık sayaçla gelir.
/// Yalnızca ekranı denemek içindir; gerçek kural ortak koddaki EnergyManager'dadır.
final class MagazaSahteEnerjiServisi: MagazaEnerjiServisi {
    private let cuzdan: MagazaSahteCuzdanServisi
    private var enerji: Int = 3
    private let azami: Int = 5
    private var sonrakiCanZamani = Date().addingTimeInterval(125)
    private let yenilenmeSuresi: TimeInterval = 600
    /// Sahte Pro aboneliği alınınca `true` olur.
    var sonsuz: Bool = false

    init(cuzdan: MagazaSahteCuzdanServisi) {
        self.cuzdan = cuzdan
    }

    func durum() -> MagazaEnerjiDurumu {
        // Sayaç dolduysa can ekle.
        while enerji < azami && sonrakiCanZamani <= Date() {
            enerji += 1
            sonrakiCanZamani = sonrakiCanZamani.addingTimeInterval(yenilenmeSuresi)
        }
        let kalanSaniye: TimeInterval = enerji < azami ? max(0, sonrakiCanZamani.timeIntervalSinceNow) : 0
        return MagazaEnerjiDurumu(
            enerji: enerji,
            azami: azami,
            sonsuz: sonsuz,
            sonrakiCanaKalanMs: Int64(kalanSaniye * 1000)
        )
    }

    func reklamlaCanAl(reklamNonce: String, tamam: @escaping (Bool) -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            self.canEkle()
            tamam(true)
        }
    }

    func anahtarlaCanAl(tamam: @escaping (Int?) -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            let bedel: Int = Int(ShopFragmentLogic.shared.LIFE_KEY_COST)
            if self.cuzdan.cuzdan.anahtar < bedel {
                tamam(nil)
                return
            }
            // Sunucu anahtarı düşer ve canı aynı işlemde ekler.
            self.cuzdan.sunucuDegistirdi(altinFarki: 0, anahtarFarki: -bedel)
            self.canEkle()
            tamam(self.cuzdan.cuzdan.anahtar)
        }
    }

    private func canEkle() {
        if enerji < azami {
            enerji += 1
        }
    }
}

/// Örnek fiyatlarla çalışan sahte ödeme servisi. Gerçek para alınmaz; satın alma her zaman
/// başarılı olur ve sunucunun vereceği ödül sahte cüzdana yazılır.
final class MagazaSahteOdemeServisi: MagazaOdemeServisi {
    private let cuzdan: MagazaSahteCuzdanServisi
    private let enerji: MagazaSahteEnerjiServisi

    /// Örnek fiyatlar (gerçek fiyat App Store Connect'te belirlenir).
    private let fiyatlar: [String: String]
    /// Sunucu kataloğunun taklidi: ürün → (altın, anahtar). Gerçekte miktarı sunucu belirler
    /// (functions/index.js → PLAY_PRODUCT_CATALOG).
    private let oduller: [String: (altin: Int, anahtar: Int)]

    init(cuzdan: MagazaSahteCuzdanServisi, enerji: MagazaSahteEnerjiServisi) {
        self.cuzdan = cuzdan
        self.enerji = enerji
        let katalog = BillingCatalog.shared
        fiyatlar = [
            katalog.GOLD_SMALL: "₺29,99",
            katalog.GOLD_MEDIUM: "₺149,99",
            katalog.GOLD_LARGE: "₺299,99",
            katalog.KEYS_SMALL: "₺29,99",
            katalog.KEYS_MEDIUM: "₺149,99",
            katalog.KEYS_LARGE: "₺299,99",
            katalog.SUB_PRO: "₺39,99",
            katalog.SUB_LITE: "₺19,99",
        ]
        oduller = [
            katalog.GOLD_SMALL: (altin: 1200, anahtar: 0),
            katalog.GOLD_MEDIUM: (altin: 7000, anahtar: 0),
            katalog.GOLD_LARGE: (altin: 15000, anahtar: 0),
            katalog.KEYS_SMALL: (altin: 0, anahtar: 10),
            katalog.KEYS_MEDIUM: (altin: 0, anahtar: 50),
            katalog.KEYS_LARGE: (altin: 0, anahtar: 100),
        ]
    }

    func urunleriGetir(kimlikler: [String], tamam: @escaping ([MagazaUrun]) -> Void) {
        var urunler: [MagazaUrun] = []
        for kimlik in kimlikler {
            if let fiyat = fiyatlar[kimlik] {
                urunler.append(MagazaUrun(kimlik: kimlik, fiyatMetni: fiyat))
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            tamam(urunler)
        }
    }

    func satinAl(urunKimligi: String, tamam: @escaping (MagazaSatinAlmaSonucu) -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            guard self.fiyatlar[urunKimligi] != nil else {
                tamam(.hata(ShopFragmentLogic.shared.MESSAGE_PRODUCT_UNAVAILABLE))
                return
            }
            if let odul = self.oduller[urunKimligi] {
                self.cuzdan.sunucuDegistirdi(altinFarki: odul.altin, anahtarFarki: odul.anahtar)
            } else if urunKimligi == BillingCatalog.shared.SUB_PRO {
                self.enerji.sonsuz = true
            }
            tamam(.verildi)
        }
    }

    func geriYukle(tamam: @escaping (Bool) -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            tamam(self.enerji.sonsuz)
        }
    }
}

/// Reklam göstermeden kısa bir beklemeyle ödül nonce'u döndürür.
final class MagazaSahteReklamServisi: MagazaReklamServisi {
    func odulluReklamGoster(tamam: @escaping (_ nonce: String?, _ hataMetni: String?) -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
            tamam(UUID().uuidString, nil)
        }
    }
}

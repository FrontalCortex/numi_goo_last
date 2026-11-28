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
    /// Ücretsiz deneme gün sayısı; `nil` = teklif yok ya da kullanıcı uygun değil
    /// (Android: BillingManager.freeTrialDays; StoreKit: `Product.subscription.introductoryOffer`
    /// ve `isEligibleForIntroOffer`).
    func ucretsizDenemeGunu(urunKimligi: String) -> Int?
}

// MARK: - Cüzdan

struct MagazaCuzdan: Equatable {
    /// Altın. Firestore: `users/{uid}.currency`.
    var altin: Int
    /// Anahtar. Firestore: `users/{uid}.keys`.
    var anahtar: Int
    /// Öğretmene soru kredisi. Firestore: `users/{uid}.questionCredits` (Öğretmene sor kapalı; satılmıyor).
    var soruKredisi: Int = 0
    /// Sobi mesaj kredisi. Firestore: `sobiChats/{uid}.credits` (Android: ServerSobi.balance);
    /// `nil` = bakiye henüz okunmadı. Negatif olabilir (iade sonrası); göstergede 0 yazılır.
    var sobiKredisi: Int? = nil
    /// Günlük ücretsiz Sobi mesaj hakkı (Android: ServerSobi.balance().freeLimit; sunucu: FREE_DAILY).
    var sobiUcretsizSinir: Int = 0
    /// Geçerli plan: "Free", "Lite", "Pro", "Premium". Firestore: `users/{uid}.plan` (+ `planExpiresAt`,
    /// `giftProUntil`; kural ortak koddaki PlanStatus.effectivePlan).
    var plan: String = "Free"
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
    /// `urunKimligi`: harcamanın ölçüm kimliği (Android: `itemId`; "bead_<Tip>", "bead_color_<Tip>",
    /// "frame_<Tip>", "frame_color_<Tip>"); iadede `nil`. Ortak kod bu alanı taşımaz, gerçek
    /// servis `updateUserWallet` isteğine ekler.
    func degistir(
        altinFarki: Int,
        anahtarFarki: Int,
        gerekce: String,
        urunKimligi: String?,
        geriAlmaJetonu: String?,
        tamam: @escaping (MagazaCuzdanDegisimi?) -> Void
    )
    /// Bakiye her değiştiğinde çağrılır (Android: listenToWallet). Dönen kimlikle bırakılır.
    func dinle(_ dinleyici: @escaping (MagazaCuzdan) -> Void) -> UUID
    func dinlemeyiBirak(_ kimlik: UUID)
}

extension MagazaCuzdanServisi {
    /// Ölçüm kimliği (`urunKimligi`) olmayan harcamalar için kısa biçim (ör. Harita: anahtarla ders açma).
    func degistir(
        altinFarki: Int,
        anahtarFarki: Int,
        gerekce: String,
        geriAlmaJetonu: String?,
        tamam: @escaping (MagazaCuzdanDegisimi?) -> Void
    ) {
        degistir(
            altinFarki: altinFarki,
            anahtarFarki: anahtarFarki,
            gerekce: gerekce,
            urunKimligi: nil,
            geriAlmaJetonu: geriAlmaJetonu,
            tamam: tamam
        )
    }
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
    /// `adet` can harcar (Android: EnergyManager.useEnergy(amount) — iyimser yerel düşüm, ardından
    /// Cloud Function `spendEnergy`, gövde ServerEnergy.spendEnergyPayload; yanıttaki `energyFullTime`
    /// benimsenir). `tamam(true)`: harcandı ya da can sonsuz (Pro/onaylı öğretmen; düşülmez).
    /// `tamam(false)`: can yetmiyor ya da onaysız öğretmen; hiçbir şey düşülmez (Android: mağaza açılır).
    /// Karar yerel kurala göre hemen verilir; sahte uygulama `tamam`'ı aynı çağrının içinde çağırır,
    /// gerçek uygulama da sunucuyu beklemeden çağırmalıdır (Android useEnergy eşzamanlıdır).
    func harca(adet: Int, tamam: @escaping (Bool) -> Void)
    /// Reklam izlenerek 1 can (Cloud Function `claimAdEnergy`, gövde: ServerEnergy.claimAdEnergyPayload).
    func reklamlaCanAl(reklamNonce: String, tamam: @escaping (Bool) -> Void)
    /// Anahtar karşılığı 1 can (Cloud Function `buyEnergyWithKeys`).
    func anahtarlaCanAl(tamam: @escaping (MagazaAnahtarCanSonucu) -> Void)
}

/// Anahtarla can alımının sonucu (Android: ServerEnergy.buyWithKeys onResult / onFailure).
enum MagazaAnahtarCanSonucu: Equatable {
    /// Can eklendi; kalan anahtar sayısı.
    case verildi(kalanAnahtar: Int)
    /// Sunucu günde en çok 5 can satar (RESOURCE_EXHAUSTED, ENERGY_KEY_BUY_DAILY_LIMIT).
    case gunlukSinir
    /// Başka hata (yetersiz bakiye, ağ).
    case hata
}

// MARK: - Seri dondurma

/// Android: StreakRepository.freezesHeld + StreakSyncService (buyFreeze, refreshFromServer).
/// Seri ekranı iOS'ta henüz yok; mağazadaki kart bu protokolün sahte uygulamasıyla çalışır.
protocol MagazaSeriServisi: AnyObject {
    /// Elde seri dondurma var mı (Android: StreakRepository.freezesHeld(ctx) > 0).
    var dondurmaVar: Bool { get }
    /// Sunucudaki durumu okur (Android: StreakSyncService.refreshFromServer).
    func yenile(tamam: @escaping () -> Void)
    /// Altınla bir seri dondurma alır (Cloud Function `buyStreakFreeze`; 4000 altın).
    /// Başarıda yeni altın bakiyesi gelir; başarısızlıkta kullanıcıya gösterilecek metin.
    func dondurmaAl(tamam: @escaping (_ basarili: Bool, _ mesaj: String?, _ yeniAltin: Int?) -> Void)
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
    let seri: MagazaSeriServisi

    /// Bütün uygulamada paylaşılan sahte servisler (cüzdan Özelleştirme ekranıyla ortaktır). Cüzdan ve can
    /// oturum boyunca tek nesnedir (`MagazaSahteCuzdanServisi.ortak`, `MagazaSahteEnerjiServisi.ortak`);
    /// uygulamanın sahte ortamı (Yonetim/SahteOrtam.swift) da bunları kullanır.
    static let sahte: MagazaBagimliliklar = {
        let cuzdan: MagazaSahteCuzdanServisi = MagazaSahteCuzdanServisi.ortak
        let enerji: MagazaSahteEnerjiServisi = MagazaSahteEnerjiServisi.ortak
        return MagazaBagimliliklar(
            odeme: MagazaSahteOdemeServisi(cuzdan: cuzdan, enerji: enerji),
            cuzdan: cuzdan,
            enerji: enerji,
            reklam: MagazaSahteReklamServisi(),
            // Seri dondurma kartı uygulamanın tek seri kaynağından beslenir (Seri/SahteSeriServisi;
            // SeriOrtak.servis bu nesnedir).
            seri: SahteSeriServisi(cuzdan: cuzdan)
        )
    }()
}

// MARK: - Sahte uygulamalar

/// Bellekte tutulan cüzdan. Başlangıç bakiyesi satın alma akışları denenebilsin diye yüksektir.
final class MagazaSahteCuzdanServisi: MagazaCuzdanServisi {
    /// Uygulamanın tek sahte cüzdanı (Android: `users/{uid}` belgesindeki currency/keys/plan tek kaynaktır).
    /// Galeri ekranları kendi örneklerini kurabilir.
    static let ortak: MagazaSahteCuzdanServisi = MagazaSahteCuzdanServisi()

    private(set) var cuzdan = MagazaCuzdan(
        altin: 12_000,
        anahtar: 45,
        soruKredisi: 0,
        sobiKredisi: 7,
        sobiUcretsizSinir: 2,
        plan: "Free"
    )
    private var dinleyiciler: [UUID: (MagazaCuzdan) -> Void] = [:]

    func yukle(tamam: @escaping (MagazaCuzdan) -> Void) {
        let deger = cuzdan
        DispatchQueue.main.async { tamam(deger) }
    }

    func degistir(
        altinFarki: Int,
        anahtarFarki: Int,
        gerekce: String,
        urunKimligi: String?,
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
            var yeni: MagazaCuzdan = self.cuzdan
            yeni.altin = yeniAltin
            yeni.anahtar = yeniAnahtar
            self.yaz(yeni)
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

    /// Sunucunun bakiyeyi kendisinin değiştirdiği durumlar (paket satın alma, anahtarla can,
    /// seri dondurma). `sobiFarki`: Sobi kredisi paketi.
    func sunucuDegistirdi(altinFarki: Int, anahtarFarki: Int, sobiFarki: Int) {
        var yeni: MagazaCuzdan = cuzdan
        yeni.altin = cuzdan.altin + altinFarki
        yeni.anahtar = cuzdan.anahtar + anahtarFarki
        if sobiFarki != 0 {
            yeni.sobiKredisi = (cuzdan.sobiKredisi ?? 0) + sobiFarki
        }
        yaz(yeni)
    }

    /// Abonelik değişince plan ve günlük ücretsiz Sobi hakkı değişir (sunucu: FREE_DAILY).
    func planiDegistir(_ plan: String, sobiUcretsizSinir: Int) {
        var yeni: MagazaCuzdan = cuzdan
        yeni.plan = plan
        yeni.sobiUcretsizSinir = sobiUcretsizSinir
        yaz(yeni)
    }

    /// Hesap değişince (giriş/çıkış) o hesabın bakiyesi yüklenir; dinleyiciler (üst çubuk, mağaza) haber alır.
    /// Android: listenToWallet yeni uid'nin `users/{uid}` belgesini dinlemeye başlar.
    func hesabinCuzdaniniYukle(_ yeni: MagazaCuzdan) {
        yaz(yeni)
    }

    private func yaz(_ yeni: MagazaCuzdan) {
        cuzdan = yeni
        for dinleyici in dinleyiciler.values {
            dinleyici(yeni)
        }
    }
}

/// Hesap değişince saklanıp geri yüklenen can durumu (Android: `users/{uid}.energy_full_time`).
struct MagazaSahteEnerjiKaydi {
    /// Canın tam dolacağı an (epoch ms); geçmişte ya da şimdi = dolu.
    let tamDolumMs: Int64
    /// O gün anahtarla alınan can sayısı (sunucu sayacı).
    let gunlukAnahtarCani: Int
}

/// EnergyManager'ın platform kapısı. Zamanlayıcı yok (üst çubuk ve mağaza kendi saatleriyle `durum()`
/// okur); "oturum yok" denir ki ortak kod sunucu çağrısı (`spendEnergy`, belge okuma) istemesin — sahte
/// serviste yerel değer yetkilidir.
final class MagazaSahteEnerjiPlatformu: NSObject, EnergyPlatform {
    func callSpendEnergy(payload: [String: Any]) {}

    func cancelTick() {}

    func isSignedIn() -> Bool {
        return false
    }

    func loadUserDoc() {}

    func scheduleTick(delayMs: Int64) {}
}

/// Bellekte tutulan can durumu. Kural ortak koddaki `EnergyManager`'dır (Android ile aynı: en çok 5 can,
/// Lite'ta 10; yenilenme 10 dk, Lite'ta 7; Pro/Premium ve onaylı öğretmende sonsuz; onaysız öğretmende 0).
/// Başlangıç örneği: 5 canın 3'ü dolu, sıradaki can 2 dk 5 sn sonra.
final class MagazaSahteEnerjiServisi: MagazaEnerjiServisi {
    /// Uygulamanın tek sahte can servisi (cüzdanı `MagazaSahteCuzdanServisi.ortak`).
    static let ortak: MagazaSahteEnerjiServisi = MagazaSahteEnerjiServisi(cuzdan: MagazaSahteCuzdanServisi.ortak)

    private let cuzdan: MagazaSahteCuzdanServisi
    private let platform: MagazaSahteEnerjiPlatformu
    /// Android: "energy_prefs" SharedPreferences.
    private let depo: InMemoryKeyValueStore
    private let yonetici: EnergyManager
    /// Bugün anahtarla alınan can sayısı (sunucu günde en çok 5 can satar).
    private var gunlukAnahtarCani: Int = 0

    /// Sonsuz can (Android: MainActivity.isInfiniteEnergy). Sahte Pro aboneliği alınınca `true` yapılır;
    /// yazınca EnergyManager'ın planı değişir (true → "Pro", false → "Free").
    var sonsuz: Bool {
        get {
            return yonetici.isInfiniteEnergy()
        }
        set {
            let plan: String = newValue ? EnergyManager.companion.PLAN_PRO : EnergyManager.companion.PLAN_FREE
            yonetici.setUserPlan(plan: plan)
        }
    }

    init(cuzdan: MagazaSahteCuzdanServisi) {
        self.cuzdan = cuzdan
        let platform = MagazaSahteEnerjiPlatformu()
        let depo = InMemoryKeyValueStore()
        self.platform = platform
        self.depo = depo
        self.yonetici = EnergyManager(
            prefs: depo,
            platform: platform,
            nowMs: { () -> KotlinLong in
                return KotlinLong(longLong: MagazaSahteEnerjiServisi.simdiMs())
            }
        )
        // Örnek: 2 can eksik, sıradaki can 125 sn sonra (dolum = şimdi + 125 sn + 1 dilim).
        let ornekDolum: Int64 = MagazaSahteEnerjiServisi.simdiMs() + 125_000 + yonetici.getEnergyRefreshMillis()
        yonetici.adoptServerFullTime(fullTime: ornekDolum)
    }

    private static func simdiMs() -> Int64 {
        return Int64(Date().timeIntervalSince1970 * 1000.0)
    }

    /// Yerelde saklanan dolum anı (EnergyManager.KEY_ENERGY_FULL_TIME; yoksa şimdi = dolu).
    private func tamDolumMs() -> Int64 {
        let simdi: Int64 = MagazaSahteEnerjiServisi.simdiMs()
        return depo.getLong(key: EnergyManager.companion.KEY_ENERGY_FULL_TIME, defaultValue: simdi)
    }

    func durum() -> MagazaEnerjiDurumu {
        let su: Int = Int(yonetici.getCurrentEnergy())
        let azami: Int = Int(yonetici.getMaxEnergy())
        let kalan: Int64 = yonetici.getTimeUntilNextEnergy()
        return MagazaEnerjiDurumu(
            enerji: su,
            azami: azami,
            sonsuz: yonetici.isInfiniteEnergy(),
            sonrakiCanaKalanMs: kalan
        )
    }

    func harca(adet: Int, tamam: @escaping (Bool) -> Void) {
        // Android EnergyManager.useEnergy: sonsuzda düşmeden true, yetmezse false, yoksa dolum anı uzar.
        let verildi: Bool = yonetici.useEnergy(amount: Int32(adet))
        tamam(verildi)
    }

    // MARK: Hesap (Yonetim/SahteOrtam.swift)

    /// Plan değişince (Android: EnergyManager.setUserPlan, `users/{uid}.plan` dinleyicisinden).
    func planiUygula(_ plan: String) {
        yonetici.setUserPlan(plan: plan)
    }

    /// Android: EnergyManager.setUserRoleApproval (`users/{uid}.role` / `teacherApproved`).
    func rolUygula(rol: String, ogretmenOnayli: Bool) {
        yonetici.setUserRoleApproval(role: rol, teacherApproved: ogretmenOnayli)
    }

    func hesapKaydi() -> MagazaSahteEnerjiKaydi {
        return MagazaSahteEnerjiKaydi(tamDolumMs: tamDolumMs(), gunlukAnahtarCani: gunlukAnahtarCani)
    }

    /// Saklanan hesabın canı geri yüklenir (dolum anı mutlak olduğu için aradaki süre de sayılır).
    func hesapKaydiniYukle(_ kayit: MagazaSahteEnerjiKaydi) {
        gunlukAnahtarCani = kayit.gunlukAnahtarCani
        yonetici.adoptServerFullTime(fullTime: kayit.tamDolumMs)
    }

    /// Yeni hesap: can dolu (Android: belgede `energy_full_time` yoksa EnergyManager dolu başlar).
    static func yeniHesapKaydi() -> MagazaSahteEnerjiKaydi {
        return MagazaSahteEnerjiKaydi(tamDolumMs: MagazaSahteEnerjiServisi.simdiMs(), gunlukAnahtarCani: 0)
    }

    func reklamlaCanAl(reklamNonce: String, tamam: @escaping (Bool) -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            self.canEkle()
            tamam(true)
        }
    }

    func anahtarlaCanAl(tamam: @escaping (MagazaAnahtarCanSonucu) -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            if self.gunlukAnahtarCani >= MagazaStil.gunlukAnahtarCanSiniri {
                tamam(.gunlukSinir)
                return
            }
            let bedel: Int = MagazaStil.canAnahtarBedeli
            if self.cuzdan.cuzdan.anahtar < bedel {
                tamam(.hata)
                return
            }
            // Sunucu anahtarı düşer ve canı aynı işlemde ekler.
            self.cuzdan.sunucuDegistirdi(altinFarki: 0, anahtarFarki: -bedel, sobiFarki: 0)
            self.gunlukAnahtarCani += 1
            self.canEkle()
            tamam(.verildi(kalanAnahtar: self.cuzdan.cuzdan.anahtar))
        }
    }

    /// Sunucunun 1 can eklemesi (claimAdEnergy / buyEnergyWithKeys): dolum anı bir dilim geri çekilir, şimdiden
    /// geriye gitmez; can zaten doluysa değişmez. Sonuç EnergyManager'a sunucu yanıtı gibi verilir
    /// (Android: ServerEnergy yanıtındaki `energyFullTime` → adoptServerFullTime).
    private func canEkle() {
        let simdi: Int64 = MagazaSahteEnerjiServisi.simdiMs()
        let dolum: Int64 = tamDolumMs()
        if dolum <= simdi {
            return
        }
        let yeni: Int64 = max(simdi, dolum - yonetici.getEnergyRefreshMillis())
        yonetici.adoptServerFullTime(fullTime: yeni)
    }
}

/// Örnek fiyatlarla çalışan sahte ödeme servisi. Gerçek para alınmaz; satın alma her zaman
/// başarılı olur ve sunucunun vereceği ödül sahte cüzdana yazılır.
final class MagazaSahteOdemeServisi: MagazaOdemeServisi {
    private let cuzdan: MagazaSahteCuzdanServisi
    private let enerji: MagazaSahteEnerjiServisi

    /// Örnek fiyatlar (gerçek fiyat App Store Connect'te belirlenir; değerler fragment_shop.xml'deki yedeklerdir).
    private let fiyatlar: [String: String]
    /// Sunucu kataloğunun taklidi: ürün → (altın, anahtar, Sobi kredisi). Gerçekte miktarı sunucu
    /// belirler (functions/index.js → PLAY_PRODUCT_CATALOG).
    private let oduller: [String: (altin: Int, anahtar: Int, sobi: Int)]

    init(cuzdan: MagazaSahteCuzdanServisi, enerji: MagazaSahteEnerjiServisi) {
        self.cuzdan = cuzdan
        self.enerji = enerji
        let katalog = BillingCatalog.shared
        fiyatlar = [
            katalog.GOLD_SMALL: "₺39,00",
            katalog.GOLD_MEDIUM: "₺120,00",
            katalog.GOLD_LARGE: "₺500,00",
            katalog.KEYS_SMALL: "₺29,00",
            katalog.KEYS_MEDIUM: "₺119,00",
            katalog.KEYS_LARGE: "₺429,00",
            katalog.SOBI_CREDITS_20: "₺12,99",
            katalog.SOBI_CREDITS_50: "₺24,99",
            katalog.SOBI_CREDITS_120: "₺49,99",
            katalog.SUB_PRO: "₺99,00",
        ]
        oduller = [
            katalog.GOLD_SMALL: (altin: 5000, anahtar: 0, sobi: 0),
            katalog.GOLD_MEDIUM: (altin: 20000, anahtar: 0, sobi: 0),
            katalog.GOLD_LARGE: (altin: 100000, anahtar: 0, sobi: 0),
            katalog.KEYS_SMALL: (altin: 0, anahtar: 10, sobi: 0),
            katalog.KEYS_MEDIUM: (altin: 0, anahtar: 50, sobi: 0),
            katalog.KEYS_LARGE: (altin: 0, anahtar: 200, sobi: 0),
            katalog.SOBI_CREDITS_20: (altin: 0, anahtar: 0, sobi: 20),
            katalog.SOBI_CREDITS_50: (altin: 0, anahtar: 0, sobi: 50),
            katalog.SOBI_CREDITS_120: (altin: 0, anahtar: 0, sobi: 120),
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
                tamam(.hata(Metin.cevir(ShopFragmentLogic.shared.MESSAGE_PRODUCT_UNAVAILABLE)))
                return
            }
            if let odul = self.oduller[urunKimligi] {
                self.cuzdan.sunucuDegistirdi(altinFarki: odul.altin, anahtarFarki: odul.anahtar, sobiFarki: odul.sobi)
            } else if urunKimligi == BillingCatalog.shared.SUB_PRO {
                // Plan tek kaynaktır: cüzdandaki plan (Ayarlar, harita, profil okur) ve EnergyManager'ın planı.
                self.cuzdan.planiDegistir("Pro", sobiUcretsizSinir: 20)
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

    func ucretsizDenemeGunu(urunKimligi: String) -> Int? {
        // Pro olan kullanıcı denemeye uygun değildir.
        if urunKimligi == BillingCatalog.shared.SUB_PRO && !enerji.sonsuz {
            return 7
        }
        return nil
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

/// Bellekte tutulan seri dondurma durumu: başlangıçta elde yok; altınla alınır.
final class MagazaSahteSeriServisi: MagazaSeriServisi {
    private let cuzdan: MagazaSahteCuzdanServisi
    private(set) var dondurmaVar: Bool = false

    init(cuzdan: MagazaSahteCuzdanServisi) {
        self.cuzdan = cuzdan
    }

    func yenile(tamam: @escaping () -> Void) {
        DispatchQueue.main.async { tamam() }
    }

    func dondurmaAl(tamam: @escaping (_ basarili: Bool, _ mesaj: String?, _ yeniAltin: Int?) -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            let bedel: Int = MagazaStil.dondurmaBedeli
            if self.dondurmaVar || self.cuzdan.cuzdan.altin < bedel {
                tamam(false, Metin.al("ui_islem_tamamlanamadi_tekrar_deneyin"), nil)
                return
            }
            self.cuzdan.sunucuDegistirdi(altinFarki: -bedel, anahtarFarki: 0, sobiFarki: 0)
            self.dondurmaVar = true
            tamam(true, nil, self.cuzdan.cuzdan.altin)
        }
    }
}

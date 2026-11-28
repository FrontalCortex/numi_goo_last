import Foundation
import Shared
import SwiftUI

// Mağaza ekranının görünüm modeli. Android karşılığı: ShopFragment.kt.
//
// Ortak Kotlin koduna yapılan BÜTÜN çağrılar bu dosyada (ve sahte servislerde) toplanmıştır;
// görünümler yalnızca aşağıdaki Swift yapılarını okur. Kurallar ortak koddadır:
// ShopFragmentLogic (ios/shared/.../oyun/ShopFragmentLogic.kt), BillingCatalog ve PlanStatus.
//
// Kart katalogları (ShopFragmentLogic.GOLD_CARDS / KEY_CARDS / SOBI_CREDIT_CARDS), can bedeli
// (LIFE_KEY_COST), seri dondurma bedeli (StreakRules.FREEZE_COST_GOLD) ve Süper düğmesinin metni
// (SubscriptionCta) de ortak koddan okunur; Swift'te sabit tutulmaz.

/// Mağazanın açılışta kaydırılacağı yer (Android: ARG_FOCUS_STREAK_FREEZE / ARG_FOCUS_SOBI_CREDITS).
enum MagazaOdak: Hashable {
    /// Seri dondurma kartı.
    case seriDondurma
    /// Sobi kredisi bölümü (üstteki Sobi kredisi göstergesine dokununca açılır).
    case sobiKredisi
    /// Sayfanın sonu (altın, anahtar ve geri yükle bölümleri; ekran galerisi için).
    case alt
}

/// Altın / anahtar paketi kartı.
struct MagazaPaketKarti: Identifiable {
    let urunKimligi: String
    let gorselAdi: String
    let birimSimgeAdi: String
    let miktarMetni: String
    let fiyatMetni: String

    var id: String { urunKimligi }
}

/// Sobi kredisi paketi kartı (shopSobiCreditCard1..3).
struct MagazaSobiPaketi: Identifiable {
    let urunKimligi: String
    let miktarMetni: String
    let fiyatMetni: String

    var id: String { urunKimligi }
}

/// "Can" bölümünün anlık görünümü (Android: updateEnergyUi).
struct MagazaCanBolumu: Equatable {
    /// Üst çubuktaki can sayısı ("∞" yerine boş: sonsuzda rozet çizilir).
    var ustCanMetni: String = "0"
    /// Sonsuz can (Pro ya da onaylı öğretmen): kalbin üzerinde "∞" rozeti görünür.
    var sonsuz: Bool = false
    var sayacMetni: String = ""
    var sayacRengi: Color = .white
    var canAlinabilir: Bool = false
    var dugmeRengi: Color = .clear
    var dugmeDudagi: Color = .clear
    var dugmeYaziRengi: Color = .white
    var dugmeSimgeOpakligi: Double = 1
    var reklamOpakligi: Double = 1
    var bedelMetni: String = ""
}

/// Katalogdaki bir paket kartının sabit tanımı (fragment_shop.xml: görsel, miktar, yedek fiyat).
private struct MagazaKartTanimi {
    let urunKimligi: String
    let gorselAdi: String
    let birimSimgeAdi: String
    let miktarMetni: String
    let yedekFiyat: String
}

/// Kartların listeleri (ortak: ShopFragmentLogic). Miktarlar yalnızca etikettir; verilecek miktarı sunucu
/// belirler (functions/index.js → PLAY_PRODUCT_CATALOG).
private enum MagazaKatalog {
    static func altin() -> [MagazaKartTanimi] {
        return tanimlar(ShopFragmentLogic.shared.GOLD_CARDS)
    }

    static func anahtar() -> [MagazaKartTanimi] {
        return tanimlar(ShopFragmentLogic.shared.KEY_CARDS)
    }

    private static func tanimlar(_ kartlar: [ShopFragmentLogic.PackCard]) -> [MagazaKartTanimi] {
        var sonuc: [MagazaKartTanimi] = []
        for kart in kartlar {
            sonuc.append(
                MagazaKartTanimi(
                    urunKimligi: kart.productId,
                    gorselAdi: kart.imageResName,
                    birimSimgeAdi: kart.currencyIconResName,
                    miktarMetni: kart.amountLabel,
                    yedekFiyat: kart.fallbackPrice
                )
            )
        }
        return sonuc
    }

    /// Sobi kredisi paketleri: (ürün, miktar metni anahtarı, yedek fiyat).
    static func sobi() -> [(urunKimligi: String, miktarAnahtari: String, yedekFiyat: String)] {
        var sonuc: [(urunKimligi: String, miktarAnahtari: String, yedekFiyat: String)] = []
        for kart in ShopFragmentLogic.shared.SOBI_CREDIT_CARDS {
            sonuc.append((urunKimligi: kart.productId, miktarAnahtari: kart.amountTextKey, yedekFiyat: kart.fallbackPrice))
        }
        return sonuc
    }

    /// Mağazadan fiyatı sorgulanacak ürünler: ekranda kartı olanlar (Öğretmene sor kredileri ve
    /// Lite aboneliği satılmaz).
    static func urunKimlikleri() -> [String] {
        var kimlikler: [String] = []
        for kart in altin() { kimlikler.append(kart.urunKimligi) }
        for kart in anahtar() { kimlikler.append(kart.urunKimligi) }
        for kart in sobi() { kimlikler.append(kart.urunKimligi) }
        kimlikler.append(ShopFragmentLogic.shared.PRO_PRODUCT_ID)
        return kimlikler
    }
}

final class MagazaModeli: ObservableObject {
    // MARK: Yayınlanan durum

    @Published private(set) var altin: Int = 0
    @Published private(set) var anahtar: Int = 0
    /// Üst çubuktaki Sobi kredisi (negatif bakiye 0 görünür).
    @Published private(set) var sobiKredisi: Int = 0
    /// Sobi bölümünün alt yazısı: bakiye gelene kadar genel bilgi, sonra "Sobi kredin: N · …".
    @Published private(set) var sobiBilgiMetni: String = Metin.al("shop_sobi_credit_info")
    /// Plan Pro / Premium mi (kalp simgesi Pro'da farklıdır).
    @Published private(set) var proPlan: Bool = false
    @Published private(set) var canBolumu = MagazaCanBolumu()
    @Published private(set) var altinKartlari: [MagazaPaketKarti] = []
    @Published private(set) var anahtarKartlari: [MagazaPaketKarti] = []
    @Published private(set) var sobiPaketleri: [MagazaSobiPaketi] = []
    @Published private(set) var proFiyatMetni: String = ""
    /// Süper kartın düğmesi (Android: SubscriptionCta: "1 HAFTA ÜCRETSİZ DENE" / "PRO'YA GEÇ").
    @Published private(set) var superDugmeMetni: String = Metin.al("fragment_shop_super_button")
    /// Elde seri dondurma var mı (kart "HAZIR" rozetine döner).
    @Published private(set) var dondurmaVar: Bool = false
    /// Seri dondurma onay penceresi açık mı.
    @Published var dondurmaOnayiAcik: Bool = false
    /// Kısa süreli bilgi balonu.
    @Published private(set) var bildirim: String?
    /// Satın alınan paketin kartından fırlayan simgeler (kart, ürün kimliğiyle eşleşir).
    @Published private(set) var paketUcusu: (urunKimligi: String, istek: MagazaUcusIstegi)?
    /// "Anahtarla can al" düğmesinden fırlayan kalp.
    @Published private(set) var anahtarKalpUcusu: MagazaUcusIstegi?
    /// "CAN KAZAN" düğmesinden fırlayan kalp.
    @Published private(set) var reklamKalpUcusu: MagazaUcusIstegi?
    /// Seri dondurma düğmesinden fırlayan simge.
    @Published private(set) var dondurmaUcusu: MagazaUcusIstegi?
    /// Şu an vurgulanan (hafifçe büyüyen) kart.
    @Published private(set) var vurgulanan: MagazaOdak?

    // MARK: Sabit metinler (fragment_shop.xml)

    let ozelTekliflerBasligi: String = Metin.al("fragment_shop_text")
    let canBasligi: String = Metin.al("fragment_shop_text_9")
    let altinBasligi: String = Metin.al("fragment_shop_text_15")
    let anahtarBasligi: String = Metin.al("fragment_shop_text_16")
    let sobiBasligi: String = Metin.al("shop_sobi_credit_title")

    /// Açılışta kaydırılacak yer (yoksa tepeden açılır).
    let baslangicOdagi: MagazaOdak?

    // MARK: Bağımlılıklar

    private let servisler: MagazaBagimliliklar
    private let sandikAc: ((String) -> Void)?
    private let planKarsilastirmaAc: (() -> Void)?

    private var fiyatlar: [String: String] = [:]
    private var cuzdanDinleyicisi: UUID?
    private var sayac: Timer?
    private var bildirimSirasi: Int = 0
    /// Android: buyLifeInProgress.
    private var canAlimiSuruyor: Bool = false
    /// Android: buyFreezeInProgress.
    private var dondurmaAlimiSuruyor: Bool = false
    private var satinAlmaSuruyor: Bool = false
    private var reklamSuruyor: Bool = false

    init(
        servisler: MagazaBagimliliklar,
        sandikAc: ((String) -> Void)?,
        planKarsilastirmaAc: (() -> Void)?,
        baslangicOdagi: MagazaOdak?
    ) {
        self.servisler = servisler
        self.sandikAc = sandikAc
        self.planKarsilastirmaAc = planKarsilastirmaAc
        self.baslangicOdagi = baslangicOdagi
        // Android: önce önbellekteki bakiye yazılır.
        cuzdaniYaz(servisler.cuzdan.cuzdan)
        kartlariKur()
        canBolumunuYenile()
        dondurmaVar = servisler.seri.dondurmaVar
    }

    deinit {
        sayac?.invalidate()
        if let kimlik = cuzdanDinleyicisi {
            servisler.cuzdan.dinlemeyiBirak(kimlik)
        }
    }

    // MARK: Yaşam döngüsü

    /// Ekran göründü (Android: onCreateView + onViewCreated).
    func basla() {
        if cuzdanDinleyicisi == nil {
            cuzdanDinleyicisi = servisler.cuzdan.dinle { [weak self] cuzdan in
                self?.cuzdaniYaz(cuzdan)
            }
        }
        servisler.cuzdan.yukle { [weak self] cuzdan in
            self?.cuzdaniYaz(cuzdan)
        }
        // Fiyatlar mağazadan gelene kadar layout'taki yedek değerler görünür (applyStorePrices).
        servisler.odeme.urunleriGetir(kimlikler: MagazaKatalog.urunKimlikleri()) { [weak self] urunler in
            guard let self = self else { return }
            for urun in urunler {
                self.fiyatlar[urun.kimlik] = urun.fiyatMetni
            }
            self.kartlariKur()
            self.superDugmesiniYaz()
        }
        // Seri dondurma kartı önbellekten çizilir, sonra sunucudaki durumla yenilenir.
        dondurmaKartiniYenile()
        servisler.seri.yenile { [weak self] in
            self?.dondurmaKartiniYenile()
        }
        // Can sayacı saniyede bir yenilenir.
        sayac?.invalidate()
        let aralik: TimeInterval = Double(ShopFragmentLogic.shared.ENERGY_TICK_MS) / 1000.0
        sayac = Timer.scheduledTimer(withTimeInterval: aralik, repeats: true) { [weak self] _ in
            self?.canBolumunuYenile()
        }
        canBolumunuYenile()
    }

    /// Ekran kapandı (Android: onDestroyView).
    func dur() {
        sayac?.invalidate()
        sayac = nil
    }

    /// Ekran belirli bir karta kaydırılarak açıldıktan sonra kart bir kez vurgulanır; vurgu açılış
    /// animasyonu bittikten sonra başlar (Android: FOCUS_PULSE_DELAY_MS = 450, yarım süre 160 ms).
    func vurguyuBaslat() {
        guard let odak = baslangicOdagi, odak != .alt else { return }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) { [weak self] in
            self?.vurgulanan = odak
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.16) { [weak self] in
                self?.vurgulanan = nil
            }
        }
    }

    // MARK: Eylemler

    /// Süper kart ve Pro kartı (Android: ProDiffirentFragment açılır — plan karşılaştırma penceresi).
    func superKartaDokunuldu() {
        if let ac = planKarsilastirmaAc {
            ac()
        } else {
            bildir("Plan karşılaştırma ekranı henüz hazır değil.")
        }
    }

    /// "SANDIK KAZAN": ödüllü reklam, ardından sıradan sandık açılır
    /// (Android: NewChestFragment.newInstance(COMMON, adNonce); sandık kapanınca bakiye yenilenir).
    func sandikIcinReklamIzle() {
        reklamGoster { [weak self] nonce in
            guard let self = self else { return }
            if let ac = self.sandikAc {
                ac(nonce)
            } else {
                self.bildir("Sandık ekranı henüz hazır değil.")
            }
        }
    }

    /// "CAN KAZAN": ödüllü reklam, ardından sunucudan 1 can.
    func canIcinReklamIzle() {
        guard canBolumu.canAlinabilir else { return }
        reklamGoster { [weak self] nonce in
            guard let self = self else { return }
            self.servisler.enerji.reklamlaCanAl(reklamNonce: nonce) { [weak self] basarili in
                guard let self = self else { return }
                if basarili {
                    self.reklamKalpUcusu = self.kalpUcusu()
                    self.canBolumunuYenile()
                } else {
                    self.bildir(Metin.al("ui_can_alinamadi_tekrar_deneyin"))
                }
            }
        }
    }

    /// Anahtar karşılığı 1 can (Android: buyLifeWithKeys).
    func anahtarlaCanAl() {
        guard canBolumu.canAlinabilir else { return }
        if canAlimiSuruyor { return }
        let bedel: Int = MagazaStil.canAnahtarBedeli
        if anahtar < bedel {
            bildir(Metin.al("ui_yetersiz_anahtar_2"))
            return
        }
        canAlimiSuruyor = true
        // İyimser gösterim: sunucu reddederse aşağıda önbellekteki değere dönülür.
        anahtar = anahtar - bedel
        servisler.enerji.anahtarlaCanAl { [weak self] sonuc in
            guard let self = self else { return }
            self.canAlimiSuruyor = false
            switch sonuc {
            case .verildi(let kalanAnahtar):
                self.anahtar = kalanAnahtar
                self.anahtarKalpUcusu = self.kalpUcusu()
                self.canBolumunuYenile()
            case .gunlukSinir:
                self.cuzdaniYaz(self.servisler.cuzdan.cuzdan)
                self.bildir(Metin.al("ui_gunluk_sinira_ulasildi_yarin_tekrar_deneyin"))
            case .hata:
                self.cuzdaniYaz(self.servisler.cuzdan.cuzdan)
                self.bildir(Metin.al("ui_islem_tamamlanamadi_tekrar_deneyin"))
            }
        }
    }

    /// Seri dondurma düğmesi: yeterli altın varsa onay penceresi açılır (Android: confirmBuyStreakFreeze).
    func seriDondurmayaDokunuldu() {
        if dondurmaAlimiSuruyor || dondurmaVar { return }
        if altin < MagazaStil.dondurmaBedeli {
            bildir(Metin.al("ui_yetersiz_altin_2"))
            return
        }
        dondurmaOnayiAcik = true
    }

    /// Onay penceresinde "Satın al" (Android: buyStreakFreeze). Bakiye iyimser düşülmez; yeni
    /// bakiye sunucu yanıtından yazılır.
    func seriDondurmayiOnayla() {
        dondurmaOnayiAcik = false
        if dondurmaAlimiSuruyor { return }
        dondurmaAlimiSuruyor = true
        servisler.seri.dondurmaAl { [weak self] basarili, mesaj, yeniAltin in
            guard let self = self else { return }
            self.dondurmaAlimiSuruyor = false
            if basarili {
                if let yeni = yeniAltin {
                    self.altin = yeni
                }
                self.dondurmaUcusu = MagazaUcusIstegi(simgeAdi: "streak_freeze_ic", adet: 1, yayilma: 0)
                self.dondurmaKartiniYenile()
            } else {
                if let metin = mesaj {
                    self.bildir(metin)
                }
                // Reddin sebebi bayat önbellek olabilir: doğrusu sunucudan çekilip kart ona göre çizilir.
                self.servisler.seri.yenile { [weak self] in
                    self?.dondurmaKartiniYenile()
                }
            }
        }
    }

    /// Pro kartı: Android'de önce plan karşılaştırma ekranı açılır, doğrudan ödeme başlamaz.
    func proKartinaDokunuldu() {
        superKartaDokunuldu()
    }

    /// Altın / anahtar paketi kartı.
    func paketSatinAl(_ kart: MagazaPaketKarti) {
        satinAl(urunKimligi: kart.urunKimligi)
    }

    /// Sobi kredisi paketi kartı.
    func sobiPaketiSatinAl(_ paket: MagazaSobiPaketi) {
        satinAl(urunKimligi: paket.urunKimligi)
    }

    /// Önceki satın alımları geri yükler. Android'de karşılığı yoktur (Play bunu kendiliğinden
    /// yapar); App Store kuralı 3.1.1 abonelik satan uygulamada bu düğmeyi ister.
    func geriYukle() {
        if satinAlmaSuruyor { return }
        satinAlmaSuruyor = true
        servisler.odeme.geriYukle { [weak self] bulundu in
            guard let self = self else { return }
            self.satinAlmaSuruyor = false
            self.canBolumunuYenile()
            self.cuzdaniYaz(self.servisler.cuzdan.cuzdan)
            self.bildir(bulundu ? Metin.al("ios_magaza_geri_yuklendi") : Metin.al("ios_magaza_geri_yuklenecek_yok"))
        }
    }

    // MARK: İç işler

    private func satinAl(urunKimligi: String) {
        if satinAlmaSuruyor { return }
        satinAlmaSuruyor = true
        servisler.odeme.satinAl(urunKimligi: urunKimligi) { [weak self] sonuc in
            guard let self = self else { return }
            self.satinAlmaSuruyor = false
            switch sonuc {
            case .verildi:
                // Android: onPurchaseGranted → bakiye ve Sobi kredisi yenilenir, kutlama animasyonu oynar.
                self.cuzdaniYaz(self.servisler.cuzdan.cuzdan)
                self.canBolumunuYenile()
                self.superDugmesiniYaz()
                self.paketUcusunuBaslat(urunKimligi: urunKimligi)
            case .iptal, .beklemede:
                break
            case .hata(let metin):
                self.bildir(metin)
            }
        }
    }

    private func reklamGoster(odul: @escaping (String) -> Void) {
        if reklamSuruyor { return }
        reklamSuruyor = true
        servisler.reklam.odulluReklamGoster { [weak self] nonce, hataMetni in
            guard let self = self else { return }
            self.reklamSuruyor = false
            if let nonce = nonce {
                odul(nonce)
            } else if let metin = hataMetni {
                self.bildir(metin)
            }
        }
    }

    /// Android: animatePurchasedItem. Simge ve adet ortak koddadır (altın/anahtar/Sobi: 30 simge).
    private func paketUcusunuBaslat(urunKimligi: String) {
        let mantik = ShopFragmentLogic.shared
        guard let simge = mantik.purchaseAnimationIconResName(productId: urunKimligi),
              let adet = mantik.purchaseAnimationItemCount(productId: urunKimligi) else { return }
        let istek = MagazaUcusIstegi(
            simgeAdi: simge,
            adet: Int(adet.intValue),
            yayilma: Double(mantik.PURCHASE_ANIMATION_SPREAD_MS) / 1000.0
        )
        paketUcusu = (urunKimligi: urunKimligi, istek: istek)
    }

    private func kalpUcusu() -> MagazaUcusIstegi {
        MagazaUcusIstegi(simgeAdi: "heart_ic", adet: 1, yayilma: 0)
    }

    private func cuzdaniYaz(_ cuzdan: MagazaCuzdan) {
        altin = cuzdan.altin
        // İyimser anahtar gösterimi sürerken sunucu yanıtı beklenir.
        if !canAlimiSuruyor {
            anahtar = cuzdan.anahtar
        }
        // Android: refreshSobiCredits — bakiye yazılır; negatif bakiye göstergede 0 görünür.
        if let kredi = cuzdan.sobiKredisi {
            sobiKredisi = max(0, kredi)
            sobiBilgiMetni = Metin.al("shop_sobi_credit_balance", kredi, cuzdan.sobiUcretsizSinir)
        }
        proPlan = PlanStatus.shared.isProPlan(plan: cuzdan.plan)
    }

    private func dondurmaKartiniYenile() {
        let yeni: Bool = servisler.seri.dondurmaVar
        if yeni != dondurmaVar {
            dondurmaVar = yeni
        }
    }

    private func kartlariKur() {
        var altinListesi: [MagazaPaketKarti] = []
        for tanim in MagazaKatalog.altin() {
            altinListesi.append(paketKarti(tanim))
        }
        altinKartlari = altinListesi
        var anahtarListesi: [MagazaPaketKarti] = []
        for tanim in MagazaKatalog.anahtar() {
            anahtarListesi.append(paketKarti(tanim))
        }
        anahtarKartlari = anahtarListesi
        var sobiListesi: [MagazaSobiPaketi] = []
        for tanim in MagazaKatalog.sobi() {
            let fiyat: String = fiyatlar[tanim.urunKimligi] ?? tanim.yedekFiyat
            sobiListesi.append(
                MagazaSobiPaketi(
                    urunKimligi: tanim.urunKimligi,
                    miktarMetni: Metin.al(tanim.miktarAnahtari),
                    fiyatMetni: fiyat
                )
            )
        }
        sobiPaketleri = sobiListesi
        // Android: applyStorePrices → "aylık $price"; layout'ta yedek metin; textAllCaps="true".
        let proKimligi: String = ShopFragmentLogic.shared.PRO_PRODUCT_ID
        let proMetni: String
        if let fiyat = fiyatlar[proKimligi] {
            proMetni = Metin.al("ui_aylik_x", fiyat)
        } else {
            proMetni = Metin.al("fragment_shop_shop_pro_price_text")
        }
        proFiyatMetni = proMetni.uppercased(with: Metin.yerel)
    }

    private func paketKarti(_ tanim: MagazaKartTanimi) -> MagazaPaketKarti {
        let fiyat: String = fiyatlar[tanim.urunKimligi] ?? tanim.yedekFiyat
        return MagazaPaketKarti(
            urunKimligi: tanim.urunKimligi,
            gorselAdi: tanim.gorselAdi,
            birimSimgeAdi: tanim.birimSimgeAdi,
            miktarMetni: tanim.miktarMetni,
            fiyatMetni: fiyat
        )
    }

    /// Android: SubscriptionCta.apply (ortak: SubscriptionCta.label).
    private func superDugmesiniYaz() {
        let urun: String = ShopFragmentLogic.shared.PRO_PRODUCT_ID
        let gun: Int? = servisler.odeme.ucretsizDenemeGunu(urunKimligi: urun)
        superDugmeMetni = MagazaAbonelikCta.metin(denemeGunu: gun, urunKimligi: urun)
    }

    private func canBolumunuYenile() {
        let durum: MagazaEnerjiDurumu = servisler.enerji.durum()
        let d = ShopFragmentLogic.shared.energySectionState(
            isInfinite: durum.sonsuz,
            currentEnergy: Int32(durum.enerji),
            maxEnergy: Int32(durum.azami),
            timeUntilNextEnergyMs: durum.sonrakiCanaKalanMs
        )
        let yeni = MagazaCanBolumu(
            ustCanMetni: durum.sonsuz ? "" : d.headerEnergyText,
            sonsuz: durum.sonsuz,
            sayacMetni: d.timerText,
            sayacRengi: MagazaStil.renk(argb: d.timerColor),
            canAlinabilir: d.canGetLife,
            dugmeRengi: MagazaStil.renk(argb: d.buyButtonColor),
            dugmeDudagi: d.canGetLife ? MagazaStil.koyulastir(argb: d.buyButtonColor) : Color.clear,
            dugmeYaziRengi: MagazaStil.renk(argb: d.buyTextColor),
            dugmeSimgeOpakligi: Double(d.buyIconAlpha),
            reklamOpakligi: Double(d.watchAdAlpha),
            bedelMetni: d.buyCostText
        )
        if yeni != canBolumu {
            canBolumu = yeni
        }
    }

    private func bildir(_ metin: String) {
        bildirimSirasi += 1
        let sira: Int = bildirimSirasi
        bildirim = metin
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            guard let self = self, self.bildirimSirasi == sira else { return }
            self.bildirim = nil
        }
    }
}

/// Abonelik düğmesinin metni (Android: SubscriptionCta.apply; kural ortak koddadır: SubscriptionCta.label).
/// Mağazadaki Süper kartı ve plan ekranı birlikte kullanır.
enum MagazaAbonelikCta {
    /// - Parameters:
    ///   - denemeGunu: ödeme servisinin verdiği ücretsiz deneme günü; teklif yoksa ya da kullanıcı uygun değilse nil.
    ///   - urunKimligi: abonelik ürünü (BillingCatalog.SUB_PRO / SUB_LITE).
    static func metin(denemeGunu: Int?, urunKimligi: String) -> String {
        var gun: KotlinInt? = nil
        if let g = denemeGunu {
            gun = KotlinInt(int: Int32(g))
        }
        let etiket: SubscriptionCtaLabel = SubscriptionCta.shared.label(freeTrialDays: gun, productId: urunKimligi)
        if let arguman = etiket.formatArg {
            return Metin.al(etiket.textKey, Int(arguman.intValue))
        }
        return Metin.al(etiket.textKey)
    }
}

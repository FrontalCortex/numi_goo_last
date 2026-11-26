import Foundation
import Shared
import SwiftUI

// Mağaza ekranının görünüm modeli. Android karşılığı: ShopFragment.kt.
//
// Ortak Kotlin koduna yapılan BÜTÜN çağrılar bu dosyada (ve sahte servislerde) toplanmıştır;
// görünümler yalnızca aşağıdaki Swift yapılarını okur. Kurallar ortak koddadır:
// ShopFragmentLogic (ios/shared/.../oyun/ShopFragmentLogic.kt) ve BillingCatalog.

/// Altın / anahtar paketi kartı.
struct MagazaPaketKarti: Identifiable {
    let urunKimligi: String
    let gorselAdi: String
    let birimSimgeAdi: String
    let miktarMetni: String
    let fiyatMetni: String

    var id: String { urunKimligi }
}

/// "Can" bölümünün anlık görünümü (Android: updateEnergyUi).
struct MagazaCanBolumu: Equatable {
    var ustCanMetni: String = "0"
    var sayacMetni: String = ""
    var sayacRengi: Color = .white
    var canAlinabilir: Bool = false
    var dugmeRengi: Color = .clear
    var dugmeYaziRengi: Color = .white
    var dugmeSimgeOpakligi: Double = 1
    var reklamOpakligi: Double = 1
    var bedelMetni: String = ""
}

final class MagazaModeli: ObservableObject {
    // MARK: Yayınlanan durum

    @Published private(set) var altin: Int = 0
    @Published private(set) var anahtar: Int = 0
    @Published private(set) var canBolumu = MagazaCanBolumu()
    @Published private(set) var altinKartlari: [MagazaPaketKarti] = []
    @Published private(set) var anahtarKartlari: [MagazaPaketKarti] = []
    @Published private(set) var proFiyatMetni: String = ""
    /// Kısa süreli bilgi balonu.
    @Published private(set) var bildirim: String?
    /// Satın alınan paketin kartından fırlayan simgeler (kart, ürün kimliğiyle eşleşir).
    @Published private(set) var paketUcusu: (urunKimligi: String, istek: MagazaUcusIstegi)?
    /// "Anahtarla can al" düğmesinden fırlayan kalp.
    @Published private(set) var anahtarKalpUcusu: MagazaUcusIstegi?
    /// "CAN KAZAN" düğmesinden fırlayan kalp.
    @Published private(set) var reklamKalpUcusu: MagazaUcusIstegi?

    // MARK: Sabit metinler (fragment_shop.xml)

    let ozelTekliflerBasligi: String = ShopFragmentLogic.shared.TITLE_SPECIAL_OFFERS
    let canBasligi: String = ShopFragmentLogic.shared.TITLE_LIFE
    let altinBasligi: String = ShopFragmentLogic.shared.TITLE_GOLD
    let anahtarBasligi: String = ShopFragmentLogic.shared.TITLE_KEYS

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
    private var satinAlmaSuruyor: Bool = false
    private var reklamSuruyor: Bool = false

    init(
        servisler: MagazaBagimliliklar,
        sandikAc: ((String) -> Void)?,
        planKarsilastirmaAc: (() -> Void)?
    ) {
        self.servisler = servisler
        self.sandikAc = sandikAc
        self.planKarsilastirmaAc = planKarsilastirmaAc
        // Android: önce önbellekteki bakiye yazılır.
        let onbellek: MagazaCuzdan = servisler.cuzdan.cuzdan
        altin = onbellek.altin
        anahtar = onbellek.anahtar
        kartlariKur()
        canBolumunuYenile()
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
        let kimlikler: [String] = ShopFragmentLogic.shared.allProductIds()
        servisler.odeme.urunleriGetir(kimlikler: kimlikler) { [weak self] urunler in
            guard let self = self else { return }
            for urun in urunler {
                self.fiyatlar[urun.kimlik] = urun.fiyatMetni
            }
            self.kartlariKur()
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

    // MARK: Eylemler

    /// Süper kart (Android: ProDiffirentFragment açılır — plan karşılaştırma penceresi).
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
                    self.bildir(ShopFragmentLogic.shared.MESSAGE_AD_LIFE_FAILED)
                }
            }
        }
    }

    /// Anahtar karşılığı 1 can (Android: buyLifeWithKeys).
    func anahtarlaCanAl() {
        guard canBolumu.canAlinabilir else { return }
        if canAlimiSuruyor { return }
        let mantik = ShopFragmentLogic.shared
        if !mantik.canBuyLifeWithKeys(currentKeys: Int32(anahtar)) {
            bildir(mantik.MESSAGE_NOT_ENOUGH_KEYS)
            return
        }
        canAlimiSuruyor = true
        // İyimser gösterim: sunucu reddederse aşağıda önbellekteki değere dönülür.
        anahtar = Int(mantik.optimisticKeysAfterBuyLife(currentKeys: Int32(anahtar)))
        servisler.enerji.anahtarlaCanAl { [weak self] kalanAnahtar in
            guard let self = self else { return }
            self.canAlimiSuruyor = false
            if let kalan = kalanAnahtar {
                self.anahtar = kalan
                self.anahtarKalpUcusu = self.kalpUcusu()
                self.canBolumunuYenile()
            } else {
                self.cuzdaniYaz(self.servisler.cuzdan.cuzdan)
                self.bildir(ShopFragmentLogic.shared.MESSAGE_BUY_LIFE_FAILED)
            }
        }
    }

    /// Pro kartı: aylık abonelik.
    func proSatinAl() {
        satinAl(urunKimligi: ShopFragmentLogic.shared.PRO_PRODUCT_ID)
    }

    /// Altın / anahtar paketi kartı.
    func paketSatinAl(_ kart: MagazaPaketKarti) {
        satinAl(urunKimligi: kart.urunKimligi)
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
            self.bildir(bulundu ? "Satın alımların geri yüklendi." : "Geri yüklenecek satın alım bulunamadı.")
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
                // Android: onPurchaseGranted → bakiye yenilenir, kutlama animasyonu oynar.
                self.cuzdaniYaz(self.servisler.cuzdan.cuzdan)
                self.canBolumunuYenile()
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

    private func paketUcusunuBaslat(urunKimligi: String) {
        let mantik = ShopFragmentLogic.shared
        guard let simge = mantik.purchaseAnimationIconResName(productId: urunKimligi) else { return }
        let istek = MagazaUcusIstegi(
            simgeAdi: simge,
            adet: Int(mantik.PURCHASE_ANIMATION_ITEM_COUNT),
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
    }

    private func kartlariKur() {
        let mantik = ShopFragmentLogic.shared
        altinKartlari = mantik.GOLD_CARDS.map { kartaCevir($0) }
        anahtarKartlari = mantik.KEY_CARDS.map { kartaCevir($0) }
        // Android: textAllCaps="true".
        let proMetni: String = mantik.proPriceLabel(storePrice: fiyatlar[mantik.PRO_PRODUCT_ID])
        proFiyatMetni = proMetni.uppercased(with: Locale(identifier: "tr_TR"))
    }

    private func kartaCevir(_ kart: ShopFragmentLogic.PackCard) -> MagazaPaketKarti {
        MagazaPaketKarti(
            urunKimligi: kart.productId,
            gorselAdi: kart.imageResName,
            birimSimgeAdi: kart.currencyIconResName,
            miktarMetni: kart.amountLabel,
            fiyatMetni: ShopFragmentLogic.shared.priceLabel(card: kart, storePrice: fiyatlar[kart.productId])
        )
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
            ustCanMetni: d.headerEnergyText,
            sayacMetni: d.timerText,
            sayacRengi: MagazaStil.renk(argb: d.timerColor),
            canAlinabilir: d.canGetLife,
            dugmeRengi: MagazaStil.renk(argb: d.buyButtonColor),
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

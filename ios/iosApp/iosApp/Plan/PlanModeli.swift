import Foundation
import Shared
import SwiftUI

// Plan karşılaştırma ekranının durumu.
//
// Android karşılığı iki ekrandır: ProDiffirentFragment (fragment_pro_diffirent.xml: Ücretsiz / Pro
// karşılaştırma tablosu, "N HAFTA ÜCRETSİZ DENE" düğmesi) ve onun açtığı PlanFragment
// (fragment_plan.xml: Pro ve Lite kartları, seçili plana göre satın alma düğmesi). Android'de ikisi
// ayrı penceredir; iOS'ta tek ekranın iki sayfasıdır (kapat tek yerde: `PlanKarsilastirmaEkrani.kapat`).
// Satın alma, geri yükleme ve deneme süresi `MagazaOdemeServisi`'nden, plan bilgisi `MagazaCuzdanServisi`'nden
// gelir (Magaza/MagazaServisler.swift); kurallar ortak koddadır (BillingCatalog, PlanStatus).

/// Ekranın iki sayfası.
enum PlanSayfasi: Equatable {
    /// ProDiffirentFragment: karşılaştırma tablosu.
    case karsilastirma
    /// PlanFragment: plan kartları ve satın alma.
    case planlar
}

/// Hoş geldin kredisi CİHAZ başına bir kez verilir (Android: WelcomeCreditEligibility.isEligible,
/// sunucuda `welcomeCreditGrants`). Cihaz krediyi tükettiyse "Hoş geldin: 20 Sobi kredisi" satırı
/// getirilemeyecek bir vaat olur ve gizlenir.
protocol PlanHosgeldinKredisiKaynagi: AnyObject {
    var uygun: Bool { get }
}

/// Her zaman uygun sayan sahte kaynak.
final class SahtePlanHosgeldinKredisi: PlanHosgeldinKredisiKaynagi {
    let uygun: Bool

    init(uygun: Bool = true) {
        self.uygun = uygun
    }
}

final class PlanModeli: ObservableObject {
    @Published private(set) var sayfa: PlanSayfasi
    /// Android: PlanFragment.selectedPlan ("Pro" / "Lite").
    @Published private(set) var seciliPlan: String = "Pro"
    /// Ürün kimliği → mağazanın yerelleştirdiği fiyat. Mağaza döndürmediği ürün yoktur.
    @Published private(set) var fiyatlar: [String: String] = [:]
    /// Satın alma düğmesinin metni (Android: SubscriptionCta.apply).
    @Published private(set) var ctaMetni: String = ""
    /// Kullanıcı zaten Pro (ya da Premium).
    @Published private(set) var proMu: Bool
    /// Satın alma ya da geri yükleme sürüyor.
    @Published private(set) var islemSuruyor: Bool = false
    @Published private(set) var bildirim: String? = nil

    let hosgeldinSatiriGorunur: Bool

    private let servisler: MagazaBagimliliklar
    private var cuzdanDinleyicisi: UUID? = nil
    private var bildirimSirasi: Int = 0

    init(
        servisler: MagazaBagimliliklar,
        hosgeldin: PlanHosgeldinKredisiKaynagi,
        baslangicSayfasi: PlanSayfasi
    ) {
        self.servisler = servisler
        self.sayfa = baslangicSayfasi
        self.hosgeldinSatiriGorunur = hosgeldin.uygun
        self.proMu = PlanStatus.shared.isProPlan(plan: servisler.cuzdan.cuzdan.plan)
        ctaYenile()
    }

    deinit {
        if let kimlik = cuzdanDinleyicisi {
            servisler.cuzdan.dinlemeyiBirak(kimlik)
        }
    }

    // MARK: Ürünler

    private var proKimligi: String {
        return BillingCatalog.shared.SUB_PRO
    }

    private var liteKimligi: String {
        return BillingCatalog.shared.SUB_LITE
    }

    /// Mağaza Lite'ı da döndürdüyse Lite kartı gösterilir. iOS'ta tek abonelik (`pro_monthly`) tanımlıysa
    /// Lite satılamayacağı için kart hiç çıkmaz (Android'de kart her zaman vardır).
    var liteVar: Bool {
        return fiyatlar[liteKimligi] != nil
    }

    /// Seçili planın ürün kimliği (ortak kural: BillingCatalog.subscriptionForPlanName).
    private var seciliKimlik: String {
        return BillingCatalog.shared.subscriptionForPlanName(planName: seciliPlan)
    }

    /// Kartın sağ üstündeki "Aylık ₺99,00" yazısı; fiyat henüz gelmediyse boş.
    func fiyatYazisi(plan: String) -> String {
        let kimlik: String = BillingCatalog.shared.subscriptionForPlanName(planName: plan)
        guard let fiyat = fiyatlar[kimlik] else {
            return ""
        }
        return Metin.al("plan_price_monthly", fiyat)
    }

    /// Seçili plan için yenileme açıklaması (fiyat, deneme, iptal).
    var yenilemeAciklamasi: String {
        return PlanMetin.yenilemeAciklamasi(fiyat: fiyatlar[seciliKimlik], denemeGunu: denemeGunu)
    }

    private var denemeGunu: Int? {
        return servisler.odeme.ucretsizDenemeGunu(urunKimligi: seciliKimlik)
    }

    // MARK: Yaşam döngüsü

    /// Ekran göründü (Android: onViewCreated + onResume'daki applyLivePrices).
    func basla() {
        if cuzdanDinleyicisi == nil {
            cuzdanDinleyicisi = servisler.cuzdan.dinle { [weak self] cuzdan in
                self?.cuzdanDegisti(cuzdan)
            }
        }
        cuzdanDegisti(servisler.cuzdan.cuzdan)
        let istenen: [String] = [proKimligi, liteKimligi]
        servisler.odeme.urunleriGetir(kimlikler: istenen) { [weak self] urunler in
            guard let self = self else { return }
            var yeni: [String: String] = [:]
            for urun in urunler {
                yeni[urun.kimlik] = urun.fiyatMetni
            }
            self.fiyatlar = yeni
            if self.seciliPlan == "Lite" && yeni[self.liteKimligi] == nil {
                self.seciliPlan = "Pro"
            }
            self.ctaYenile()
        }
        ctaYenile()
    }

    /// Ekran kapandı.
    func dur() {
        if let kimlik = cuzdanDinleyicisi {
            servisler.cuzdan.dinlemeyiBirak(kimlik)
            cuzdanDinleyicisi = nil
        }
    }

    // MARK: Eylemler

    /// Karşılaştırma sayfasındaki düğme (Android: PlanFragment().show).
    func planlaraGec() {
        if proMu {
            return
        }
        sayfa = .planlar
    }

    /// Kart seçimi (Android: cardBireysel / cardLite tıklaması).
    func planSec(_ plan: String) {
        if plan == "Lite" && !liteVar {
            return
        }
        seciliPlan = plan
        ctaYenile()
    }

    /// Satın alma düğmesi. Android'de pencere satın alma başlar başlamaz kapanır; iOS'ta sonuç beklenir,
    /// başarıda `bitti` çağrılır (ekran kapanır), iptalde ekran açık kalır.
    func satinAl(bitti: @escaping () -> Void) {
        if islemSuruyor {
            return
        }
        let katalog = BillingCatalog.shared
        let kimlik: String = seciliKimlik
        // Aynı ya da daha yüksek rütbeli bir aboneliğin üstüne satın alma yapılmaz (ortak kural: subscriptionRank).
        let mevcut: Int32 = mevcutRutbe(servisler.cuzdan.cuzdan.plan)
        if mevcut >= katalog.subscriptionRank(productId: kimlik) {
            bildir(PlanMetin.zatenSahip)
            return
        }
        islemSuruyor = true
        servisler.odeme.satinAl(urunKimligi: kimlik) { [weak self] sonuc in
            guard let self = self else { return }
            self.islemSuruyor = false
            switch sonuc {
            case .verildi:
                self.cuzdanDegisti(self.servisler.cuzdan.cuzdan)
                bitti()
            case .iptal, .beklemede:
                break
            case .hata(let metin):
                self.bildir(metin)
            }
        }
    }

    /// "Satın alımları geri yükle" (App Store kuralı; Android'de Play kendiliğinden yapar).
    func geriYukle() {
        if islemSuruyor {
            return
        }
        islemSuruyor = true
        servisler.odeme.geriYukle { [weak self] bulundu in
            guard let self = self else { return }
            self.islemSuruyor = false
            self.cuzdanDegisti(self.servisler.cuzdan.cuzdan)
            self.bildir(bulundu ? PlanMetin.geriYuklendi : PlanMetin.geriYuklenecekYok)
        }
    }

    // MARK: İç işler

    private func mevcutRutbe(_ plan: String) -> Int32 {
        let katalog = BillingCatalog.shared
        if PlanStatus.shared.isProPlan(plan: plan) {
            return katalog.subscriptionRank(productId: katalog.SUB_PRO)
        }
        if plan.lowercased() == "lite" {
            return katalog.subscriptionRank(productId: katalog.SUB_LITE)
        }
        return 0
    }

    private func cuzdanDegisti(_ cuzdan: MagazaCuzdan) {
        let yeni: Bool = PlanStatus.shared.isProPlan(plan: cuzdan.plan)
        if yeni != proMu {
            proMu = yeni
        }
        ctaYenile()
    }

    /// Android: SubscriptionCta.apply — deneme yalnızca uygun kullanıcıya vaat edilir.
    private func ctaYenile() {
        let lite: Bool = seciliPlan == "Lite"
        guard let gun = denemeGunu else {
            ctaMetni = Metin.al(lite ? "sub_cta_no_trial_lite" : "sub_cta_no_trial_pro")
            return
        }
        // 7, 14, 21… gün "1 hafta / 2 hafta" olarak daha doğal okunuyor.
        if gun % 7 == 0 {
            ctaMetni = Metin.al("sub_cta_trial_weeks", gun / 7)
        } else {
            ctaMetni = Metin.al("sub_cta_trial_days", gun)
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

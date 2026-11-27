import Combine
import Foundation
import SwiftUI

// Uygulamanın yöneticisi: ekranları birbirine bağlar.
//
// Android karşılığı: MainActivity'nin fragment yığını (supportFragmentManager) ve ekranların
// birbirini açtığı yerler (LessonAdapter.continueWithLesson, AbacusFragment.showLessonResult,
// LessonResult claim → ChestFragment → NewChestFragment, TasksFragment.showCupPathPanel ...).
//
// Sunum tek katmanlıdır: kök görünüm (UygulamaKokGorunumu) tabanı (giriş ya da kabuk) çizer ve
// üstüne `katmanlar` yığınındaki tam ekranları sırayla koyar. `fullScreenCover` kullanılmaz
// (iOS 16'da üst üste sunum sorunlu). Alttaki katmanlar bellekte kalır (Android geri yığını gibi):
// kupa dersi bitince Kupa Yolu paneli, rozet ayrıntısı kapanınca rozet listesi olduğu gibi durur.
//
// Ortak Kotlin koduna yapılan çağrıların hepsi YonetimIlerlemeDeposu'ndadır.

/// Tabanda ne var.
enum UygulamaAsamasi {
    /// Giriş akışı (GirisAkisi).
    case giris
    /// Ana kabuk (AnaKabuk): üst çubuk + sekmeler + alt çubuk.
    case kabuk
}

/// Kör dersi kimin açtığı; ders bitince ne yapılacağını belirler.
enum YonetimDersKaynagi {
    /// Haritadaki ders: sonuç ekranı → sandık → harita.
    case harita
    /// Kupa Yolu: ders kapanır, kupa değişimi panele `KupaDersSonucu` olarak döner.
    case kupa(kartNo: Int)
    /// Günlük soru: ders kapanır, doğru/yanlış `sonuc` ile Keşfet sekmesine bildirilir.
    case gunlukSoru(sonuc: (Bool) -> Void)
}

/// Sonuç ekranından sonra ilerlemeyi işlemek için gerekenler.
struct YonetimSonucBaglami {
    let partId: Int32
    /// Android: GlobalValues.mapFragmentStepIndex.
    let sira: Int32
    let dersPuani: Int
    let toplamPuan: Int
    let basariOrani: Float
    /// "Tekrar dene": aynı dersi baştan açar (Android'de bu düğme yok; bkz. SonucGirdisi).
    let tekrar: () -> Void
}

/// Öğretici (TutorialFragment) henüz yazılmadı; yerine gösterilen yer tutucunun içeriği.
struct YonetimOgreticiHedefi {
    let baslik: String
    let ogreticiNo: Int
    let partId: Int32
    let sira: Int32
    let listeSirasi: Int
}

/// Henüz yazılmamış bir ekranın yer tutucusu.
struct YonetimYerTutucu {
    let baslik: String
    let aciklama: String
    let ayrintilar: [String]
}

/// Tabanın üstünde açılan ekranlar.
enum YonetimEkrani {
    case abakusDers(AbakusDersBaslangici)
    case korDers(KorDersBaslangici, YonetimDersKaynagi)
    case sonuc(SonucGirdisi, YonetimSonucBaglami)
    /// `kapandi`: sandık kapanınca isteği yapan ekrana haber verilir (görev / günlük soru).
    case sandik(SandikGirdisi, kapandi: (SandikSonucu) -> Void)
    case magaza
    case kupa
    case kupaGecmisi(ProfilKupaSecimi)
    case rozetler
    case rozetDetay(modAdi: String)
    case liderTablosu(partId: Int, dersAnahtari: String, baslik: String)
    case ogretici(YonetimOgreticiHedefi)
    case yerTutucu(YonetimYerTutucu)

    /// Altındaki ekran görünür kalır mı (Kupa Yolu alttan açılan paneldir).
    var saydam: Bool {
        if case .kupa = self { return true }
        return false
    }

    var magazaMi: Bool {
        if case .magaza = self { return true }
        return false
    }
}

struct YonetimKatmani: Identifiable {
    let id: Int
    let ekran: YonetimEkrani
}

final class UygulamaYoneticisi: ObservableObject {
    @Published private(set) var asama: UygulamaAsamasi
    @Published private(set) var katmanlar: [YonetimKatmani] = []
    /// Kupa dersinden dönüş; Kupa Yolu paneline verilir.
    @Published private(set) var kupaSonucu: KupaDersSonucu? = nil
    /// Çıkıştan sonra giriş akışının sıfırdan kurulması için artırılır.
    @Published private(set) var girisKimligi: Int = 0

    /// Üst çubuğun durumu. Elmas ve anahtar mağaza/özelleştirmenin (sahte) cüzdanından, enerji
    /// mağazanın (sahte) can servisinden okunur; böylece üçü tek kaynağı gösterir.
    let oturum = OturumDurumu()
    /// Harita modeli burada tutulur: hedefi okumak ve ders bitince listeyi yenilemek için.
    let haritaModeli = HaritaModeli()
    private(set) var kimlikServisi: SahteKimlikServisi

    let ilerleme = YonetimIlerlemeDeposu()

    private var abonelikler: Set<AnyCancellable> = []
    /// Haritada en son açılan ders paneli; `HaritaHedefi` yalnızca metin taşıdığı için dersin
    /// kendisi buradan okunur.
    private var sonPanel: HaritaPanelDurumu? = nil
    private var katmanSayaci: Int = 0
    private var cuzdanDinleyicisi: UUID? = nil

    /// Yalnızca animasyon tetiklemek için.
    var katmanKimlikleri: [Int] {
        return katmanlar.map { $0.id }
    }

    /// - Parameter girisAtla: true ise giriş akışı gösterilmeden kabuk açılır (ekran galerisi).
    ///
    /// Varsayılan davranış: sahte kimlik servisinde oturum KAPALI başlar
    /// (`SahteKimlikAyarlari.acilistaOturumEposta == nil`), yani uygulama açılış → başlangıç
    /// ekranıyla açılır. `-oturum acik` başlatma argümanı verilirse hazır öğrenci hesabının
    /// oturumu açık sayılır ve giriş akışı kendi tek cihaz denetiminden sonra kabuğa geçer.
    init(girisAtla: Bool = false) {
        asama = girisAtla ? .kabuk : .giris
        kimlikServisi = UygulamaYoneticisi.yeniKimlikServisi(
            oturumAcik: UserDefaults.standard.string(forKey: "oturum") == "acik"
        )
        haritaModeli.ilerlemeKaynagi = ilerleme
        haritayiDinle()
        cuzdaniBagla()
    }

    deinit {
        if let kimlik = cuzdanDinleyicisi {
            MagazaBagimliliklar.sahte.cuzdan.dinlemeyiBirak(kimlik)
        }
    }

    private static func yeniKimlikServisi(oturumAcik: Bool) -> SahteKimlikServisi {
        var ayarlar = SahteKimlikAyarlari()
        if oturumAcik {
            // SahteKimlikServisi'nin hazır öğrenci hesabı.
            ayarlar.acilistaOturumEposta = "ogrenci@sorobit.test"
        }
        return SahteKimlikServisi(ayarlar: ayarlar)
    }

    // MARK: Giriş / çıkış

    /// GirisAkisi.girisYapildi.
    func girisYapildi() {
        asama = .kabuk
        oturumuYenile()
    }

    /// ProfilEkrani.cikisYapildi (Android: LoginStartActivity temiz görev yığınıyla açılır).
    func cikisYapildi() {
        katmanlar = []
        kupaSonucu = nil
        haritaModeli.kisimSeciminDon()
        // Profilin sahte deposu ile sahte kimlik servisi ayrı nesnelerdir; servisteki oturum da
        // kapansın diye servis yeniden kurulur (oturum kapalı başlar).
        kimlikServisi = UygulamaYoneticisi.yeniKimlikServisi(oturumAcik: false)
        girisKimligi += 1
        asama = .giris
    }

    // MARK: Katman yığını

    func ac(_ ekran: YonetimEkrani) {
        katmanSayaci += 1
        katmanlar.append(YonetimKatmani(id: katmanSayaci, ekran: ekran))
    }

    func kapat(_ kimlik: Int) {
        katmanlar.removeAll { $0.id == kimlik }
        oturumuYenile()
    }

    /// Katmanı yerinde başka bir ekranla değiştirir (ders → sonuç → sandık).
    func degistir(_ kimlik: Int, _ ekran: YonetimEkrani) {
        katmanSayaci += 1
        let yeni = YonetimKatmani(id: katmanSayaci, ekran: ekran)
        if let i = katmanlar.firstIndex(where: { $0.id == kimlik }) {
            katmanlar[i] = yeni
        } else {
            katmanlar.append(yeni)
        }
    }

    func yerTutucuAc(_ baslik: String, _ aciklama: String, _ ayrintilar: [String] = []) {
        ac(.yerTutucu(YonetimYerTutucu(baslik: baslik, aciklama: aciklama, ayrintilar: ayrintilar)))
    }

    // MARK: Üst çubuk (cüzdan + enerji)

    /// Mağaza ve özelleştirme ekranları varsayılan olarak `MagazaBagimliliklar.sahte.cuzdan`'ı
    /// kullanır; üst çubuk da aynı nesneyi dinler. Gerçek servisler bağlanınca buradaki kaynak
    /// değiştirilir (Android: UserWalletFirestore.listenToWallet → MainActivity.applyWalletToUi).
    private func cuzdaniBagla() {
        let cuzdan: MagazaCuzdanServisi = MagazaBagimliliklar.sahte.cuzdan
        cuzdanDinleyicisi = cuzdan.dinle { [weak self] yeni in
            self?.cuzdaniUygula(yeni)
        }
        oturumuYenile()
    }

    private func cuzdaniUygula(_ cuzdan: MagazaCuzdan) {
        if oturum.elmas != cuzdan.altin {
            oturum.elmas = cuzdan.altin
        }
        if oturum.anahtar != cuzdan.anahtar {
            oturum.anahtar = cuzdan.anahtar
        }
    }

    /// Cüzdanı ve enerjiyi kaynaklarından yeniden okur. Enerji servisinde dinleyici yoktur;
    /// bu yüzden katman kapanınca, sekme değişince ve kabuğun saatinde çağrılır.
    func oturumuYenile() {
        cuzdaniUygula(MagazaBagimliliklar.sahte.cuzdan.cuzdan)
        let enerji: MagazaEnerjiDurumu = MagazaBagimliliklar.sahte.enerji.durum()
        if oturum.enerji != enerji.enerji {
            oturum.enerji = enerji.enerji
        }
        if oturum.azamiEnerji != enerji.azami {
            oturum.azamiEnerji = enerji.azami
        }
        if oturum.sonsuzEnerji != enerji.sonsuz {
            oturum.sonsuzEnerji = enerji.sonsuz
        }
        // Haritadaki "başla" düğmesinin enerji kararı da aynı kaynağa bakar.
        haritaModeli.sonsuzEnerji = enerji.sonsuz
        haritaModeli.enerjiYeterli = enerji.sonsuz || enerji.enerji >= ilerleme.dersEnerjiBedeli
    }

    // MARK: Mağaza

    /// Üst çubuk göstergeleri, özelleştirmedeki bakiye, Kupa Yolu'ndaki can göstergesi
    /// (Android: MainActivity.openShopFragment).
    func magazaAc() {
        if let ust = katmanlar.last, ust.ekran.magazaMi {
            return
        }
        ac(.magaza)
    }

    /// Mağazada reklam izlendi: sıradan sandık reklam değeriyle açılır
    /// (Android: ShopFragment → NewChestFragment.newInstance(COMMON, adNonce)).
    func reklamSandiginiAc(nonce: String) {
        ac(.sandik(SandikGirdisi(baslangicNadirligi: "COMMON", reklamNonce: nonce), kapandi: { _ in }))
    }

    // MARK: Görevler

    /// GorevlerEkrani.sandikAc (Android: MissionsFragment.onQuestClicked → NewChestFragment).
    func gorevSandiginiAc(_ istek: GorevlerSandikIstegi) {
        let kapandi: () -> Void = istek.kapandi
        ac(.sandik(SandikGirdisi(baslangicNadirligi: istek.nadirlik), kapandi: { _ in kapandi() }))
    }

    // MARK: Keşfet (günlük soru)

    /// GunlukSoruEkrani.soruBaslat (Android: BlindingLessonFragment.newDailyQuestionInstance).
    func gunlukSoruyuBaslat(_ veri: GunlukSoruBaslangici) {
        var islemler: [KorDersIslemi] = []
        if let islem = veri.islem {
            islemler.append(.carpma(birinci: islem.ilk, islec: islem.islec, ikinci: islem.ikinci))
        } else {
            islemler.append(.dizi(veri.dizi))
        }
        let baslangic = KorDersBaslangici.gunlukSoruIcin(
            partId: veri.partId,
            islemler: islemler,
            sayiSuresiMs: veri.gosterimAraligiMs,
            gunlukSoru: KorDersGunlukSoru(
                donemAnahtari: veri.periodKey, yuva: veri.slotIndex, partId: veri.partId
            )
        )
        ac(.korDers(baslangic, .gunlukSoru(sonuc: veri.sonuc)))
    }

    /// GunlukSoruEkrani.odulAc (Android: TasksFragment.startDailyQuestionRewardFlow →
    /// NewChestFragment.newInstance(RARE)).
    func gunlukSoruOdulunuAc(_ istek: GunlukSoruOdulIstegi) {
        let kapandi: () -> Void = istek.kapandi
        ac(.sandik(SandikGirdisi(baslangicNadirligi: istek.nadirlik), kapandi: { _ in kapandi() }))
    }

    /// GunlukSoruEkrani.kartSecildi.
    func kesfetKartiSecildi(_ kart: GunlukSoruKesfetKarti) {
        switch kart {
        case .kupaYolu:
            kupaSonucu = nil
            ac(.kupa)
        case .abakusPratik:
            yerTutucuAc("Abaküs", "Serbest abaküs alıştırması", ["Android: AbacusPracticeFragment"])
        case .bizeUlasin:
            yerTutucuAc("Bize Ulaşın", "Geri bildirim ekranı", ["Android: FeedbackFragment"])
        }
    }

    // MARK: Kupa Yolu

    /// KupaEkrani.kupaDersiBaslat (Android: TasksFragment.launchCupModeLesson).
    func kupaDersiniBaslat(_ veri: KupaDersBaslangici) {
        var islemler: [KorDersIslemi] = []
        if veri.carpma {
            islemler.append(.carpma(birinci: veri.carpan1, islec: "x", ikinci: veri.carpan2))
        } else {
            islemler.append(.dizi(veri.sayilar))
        }
        // Android (CupRuleEngine.buildLessonItem): bayraklar "true ya da null"dır.
        let baslangic = KorDersBaslangici.kupaIcin(
            islemler: islemler,
            kor: veri.korleme ? true : nil,
            carpma: veri.carpma ? true : nil,
            cikarma: veri.cikarma ? true : nil,
            sayiSuresiMs: veri.sureMs,
            kupaKazanc: veri.kazancDeltasi,
            kupaKayip: veri.kayipDeltasi
        )
        kupaSonucu = nil
        ac(.korDers(baslangic, .kupa(kartNo: veri.kartNo)))
    }

    // MARK: Profil

    /// ProfilEkrani.kupaAc (Android: CupHistoryFragment).
    func profilKupasiniAc(_ secim: ProfilKupaSecimi) {
        ac(.kupaGecmisi(secim))
    }

    /// ProfilEkrani.rozetlerAc (Android: BadgeFragment).
    func rozetleriAc() {
        ac(.rozetler)
    }

    /// RozetlerEkrani.rozetSecildi (Android: BadgeDetailFragment).
    func rozetSecildi(_ secim: RozetSecimi) {
        ac(.rozetDetay(modAdi: secim.modAdi))
    }

    // MARK: Kör dersin bitişi (üç kaynak)

    /// KorDersEkrani.bitti.
    func korDersBitti(_ sonuc: KorDersSonucu, kaynak: YonetimDersKaynagi, baslangic: KorDersBaslangici, katman: Int) {
        switch kaynak {
        case .harita:
            haritaKorDersiBitti(sonuc, baslangic: baslangic, katman: katman)
        case .kupa(let kartNo):
            // Android: GlobalValues.pending…CupDelta + TasksFragment.consumePendingCupDelta.
            if let delta = sonuc.kupaDeltasi {
                kupaSonucu = KupaDersSonucu(kartNo: kartNo, delta: delta)
            }
            kapat(katman)
        case .gunlukSoru(let bildir):
            if sonuc.yonlendirme == .gunlukSoruTamam {
                bildir(true)
            } else if sonuc.yonlendirme == .gunlukSoruYanlis {
                bildir(false)
            }
            kapat(katman)
        }
    }

    // MARK: Harita

    private func haritayiDinle() {
        haritaModeli.$panel
            .sink { [weak self] panel in
                if let acilan = panel {
                    self?.sonPanel = acilan
                }
            }
            .store(in: &abonelikler)
        // @Published değeri atanmadan ÖNCE yayınlar; hedefi aynı anda nil'lemek atamayla
        // çakışacağı için teslim bir sonraki tura bırakılır.
        haritaModeli.$hedef
            .compactMap { $0 }
            .receive(on: DispatchQueue.main)
            .sink { [weak self] hedef in
                self?.haritaHedefiGeldi(hedef, panel: self?.sonPanel)
            }
            .store(in: &abonelikler)
    }
}

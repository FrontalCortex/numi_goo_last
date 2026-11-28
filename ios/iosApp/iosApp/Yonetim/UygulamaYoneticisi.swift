import Combine
import Foundation
import Shared
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

/// Açılacak öğretici (TutorialFragment) ve ardından açılacak ders. İçeriği taşınmamış öğreticide
/// eski yer tutucu gösterilir (bkz. OgreticiBaslangici.icerikVar).
struct YonetimOgreticiHedefi {
    let baslik: String
    let ogreticiNo: Int
    let partId: Int32
    let sira: Int32
    let listeSirasi: Int
    /// Hesapsız ilk açılış (Android isFirstRunTutorial): çıkış ve "Eğitimi atla" gizli.
    var ilkAcilis: Bool = false
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
    /// Kupa Yolu ödül yolu (Android CupPathRoadFragment); kupa alanı ("addition_abacus_cup" ...).
    case kupaYoluOdul(kupaAlani: String)
    case kupaGecmisi(ProfilKupaSecimi)
    case rozetler
    case rozetDetay(modAdi: String)
    case liderTablosu(partId: Int, dersAnahtari: String, baslik: String)
    case ogretici(YonetimOgreticiHedefi)
    case yerTutucu(YonetimYerTutucu)
    /// Günlük seri ekranları (Seri/; bağlantılar YonetimSeriAkisi.swift).
    case seri(YonetimSeriKatmani)
    /// Plan karşılaştırma / Pro satın alma (Android ProDiffirentFragment + PlanFragment; Plan/; bağlantı YonetimPlanRozetAkisi.swift).
    case plan
    /// Rozet kutlaması (Android BadgeFragment kutlama kipi); `bitti`: kuyruk bitip ekran kapanınca çalışır.
    case rozetKutlama(kuyruk: [String], bitti: () -> Void)
    /// "Sobi ile analiz et" penceresi (Android MistakeReviewDialog; SobiAnaliz/). Kapanınca `istek.kapandi`.
    case sobiAnaliz(AbakusSobiAnalizIstegi)
    /// Yarış paneli "Ustalık Yolu" (Android LessonAdapter.showRacePanel; Yaris/; bağlantı Yaris/YarisAkisi.swift).
    case yarisPaneli(YarisPaneliGirdisi)
    /// Sobi'nin günlük testi: panel ve oynatıcı (Android SobiTestSheet + HomeworkPlayerFragment; SobiTest/;
    /// bağlantı YonetimSobiTestAkisi.swift).
    case sobiTest(YonetimSobiTestKatmani)

    /// Altındaki ekran görünür kalır mı (Kupa Yolu alttan açılan paneldir).
    var saydam: Bool {
        if case .kupa = self { return true }
        if case .sobiTest(.sayfa) = self { return true }
        return false
    }

    var magazaMi: Bool {
        if case .magaza = self { return true }
        if case .seri(.dondurmaMagazasi) = self { return true }
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
    /// Android `GlobalValues.currentTutorialNumber == 1` (hesapsız): 1. öğretici oturumsuz açıldı;
    /// arkasındaki ders bitince sandık yerine başlangıç (kayıt) ekranı açılır. Dersin çıkış düğmesi
    /// de gizlenir (Android isFirstRunLesson).
    @Published private(set) var ilkOgreticiAkisi: Bool = false
    /// Oturum açılmadan kabuğa geçildi (GirisAkisi.misafirOlarakDevam). Kimlik servisi ana aktöre
    /// bağlı olduğu için oturum durumu buradan izlenir (Android: FirebaseAuth.currentUser == null).
    private var misafir: Bool = false
    /// Android `tutorial1_login_flow_pending`: hesapsız 1. öğreticiden sonra giriş ekranına geçildi; girişte
    /// misafirin ders ilerlemesi (1. ders öğreticisi geçilmiş kısım 1 listesi) yeni hesaba taşınır.
    private var ilkOgreticiIlerlemesiBekliyor: Bool = false
    /// Oturum yok mu (Android AbacusFragment.isFirstRunLesson: FirebaseAuth.currentUser == null).
    /// Hesapsız her abaküs dersinde çıkış ve kural düğmeleri gizlenir.
    var misafirMi: Bool {
        return misafir
    }

    /// Üst çubuğun durumu. Elmas ve anahtar mağaza/özelleştirmenin (sahte) cüzdanından, enerji
    /// mağazanın (sahte) can servisinden okunur; böylece üçü tek kaynağı gösterir.
    let oturum = OturumDurumu()
    /// Harita modeli burada tutulur: hedefi okumak ve ders bitince listeyi yenilemek için.
    let haritaModeli = HaritaModeli()
    /// Oturum boyunca tek kimlik servisi (kayıt deposuyla birlikte; Yonetim/SahteOrtam.swift). Çıkışta
    /// yeniden kurulmaz: yalnızca oturum kapanır, bu oturumda açılan hesaplarla yeniden giriş yapılabilir.
    let kimlikServisi: SahteKimlikServisi
    /// Ekran galerisi kipi (`girisAtla`): ekranlar sahte ortam yerine kendi örnek verileriyle kurulur
    /// ve hesap değişimi işlenmez.
    let ornekKipi: Bool

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
        ornekKipi = girisAtla
        // `-oturum acik` argümanı SahteOrtam'da okunur (hazır öğrenci hesabının oturumu açık başlar).
        kimlikServisi = SahteOrtam.ortak.kimlik
        haritaModeli.ilerlemeKaynagi = ilerleme
        haritayiDinle()
        cuzdaniBagla()
        seriyiBagla()
        sobiTestiBagla()
    }

    deinit {
        if let kimlik = cuzdanDinleyicisi {
            MagazaBagimliliklar.sahte.cuzdan.dinlemeyiBirak(kimlik)
        }
    }

    // MARK: Giriş / çıkış

    /// GirisAkisi.girisYapildi.
    @MainActor
    func girisYapildi() {
        misafir = false
        hesabaGec(kimlikServisi.mevcutOturum?.uid)
        ilkOgreticiIlerlemesiBekliyor = false
        asama = .kabuk
        oturumuYenile()
    }

    /// Hesap değişti (giriş, misafir, çıkış): hesaba bağlı bellek durumu (cüzdan, can, plan, kupa,
    /// rozet belgesi, ders ilerlemesi) yeni hesabınkiyle değişir; önceki hesabın rozet kuyruğu atılır.
    /// Android: çıkışta LoginStartActivity temiz görev yığınıyla açılır, girişte GlobalLessonData.initialize
    /// ve cüzdan/enerji dinleyicileri yeni uid'nin belgelerini okur.
    @MainActor
    private func hesabaGec(_ uid: String?) {
        if ornekKipi {
            return
        }
        let degisti: Bool = SahteOrtam.ortak.hesabaGec(
            uid: uid,
            ilerleme: ilerleme,
            misafirIlerlemesiniTasi: ilkOgreticiIlerlemesiBekliyor
        )
        if !degisti {
            return
        }
        YonetimRozetKuyrugu.ortak.bekleyen = []
        haritaModeli.yenile()
    }

    /// GirisAkisi.misafirOlarakDevam: hesapsız ilk açılış (SplashRules.Route.GUEST_MAIN).
    /// Android hesap geçmişi yoksa öğreticiye (START_DESTINATION_TUTORIAL), varsa haritaya gider.
    /// Öğretici kısım 1'in 1. sırasındaki dersindir (MainActivity.showFirstTutorial →
    /// renderFirstTutorial: harita tabanda, öğretici üstünde); öğretici → ders → sonuç → başlangıç
    /// ekranı (ilkDerstenSonraGiriseGec).
    @MainActor
    func misafirOlarakDevam(_ hedef: GirisMisafirHedefi) {
        misafir = true
        hesabaGec(nil)
        asama = .kabuk
        oturumuYenile()
        if case .ogretici = hedef {
            ilkOgreticiyiAc()
        }
    }

    /// MainActivity.showFirstTutorial: kısım 1'in ders listesi, `getLessonItem(1)`.
    private func ilkOgreticiyiAc() {
        guard let ilk = ilerleme.ilkOgreticiOgesi() else { return }
        var hedef = YonetimOgreticiHedefi(
            baslik: ilk.baslik,
            ogreticiNo: ilk.ogreticiNo,
            partId: 1,
            sira: ilk.sira,
            listeSirasi: 1
        )
        hedef.ilkAcilis = true
        ogreticiAcildi(hedef)
        ac(.ogretici(hedef))
    }

    /// Bir öğretici açılırken (Android TutorialFragment.onViewCreated: tutorialNumber == 1 ise
    /// `GlobalValues.currentTutorialNumber = 1`; yönlendirme yalnızca oturum yokken yapılır).
    func ogreticiAcildi(_ hedef: YonetimOgreticiHedefi) {
        if hedef.ogreticiNo == 1 {
            ilkOgreticiAkisi = misafir
        }
    }

    /// İlk öğreticinin dersi bitti ve oturum yok: sandık yerine başlangıç ekranı (Android
    /// ChestFragment.redirectToLoginForFirstTutorial / LessonResultFalse claim → LoginStartActivity,
    /// EXTRA_BLOCK_BACK). `tutorial1_login_flow_pending`: girişten sonra kısım 1'in listesi 1. ders
    /// geçilmiş olarak kurulur. Dönüş yok: katmanlar kapanır, giriş akışı başlangıçtan açılır.
    /// - Returns: yönlendirildiyse true.
    func ilkDerstenSonraGiriseGec() -> Bool {
        if !ilerleme.ilkOgreticidenGiriseGecilir(ilkOgretici: ilkOgreticiAkisi, oturumAcik: !misafir) {
            return false
        }
        ilkOgreticiAkisi = false
        misafir = false
        ilkOgreticiIlerlemesiBekliyor = true
        ilerleme.ilkOgreticiSonrasiListeyiKur()
        haritaModeli.yenile()
        katmanlar = []
        kupaSonucu = nil
        girisKimligi += 1
        asama = .giris
        return true
    }

    /// ProfilEkrani.cikisYapildi (Android: AuthManager.logout → auth.signOut, LoginStartActivity temiz
    /// görev yığınıyla açılır).
    @MainActor
    func cikisYapildi() {
        katmanlar = []
        kupaSonucu = nil
        haritaModeli.kisimSeciminDon()
        // Kimlik servisi (ve kayıt deposu) yeniden KURULMAZ; yalnızca oturum kapanır (profil deposu da
        // aynı servisi kapatır; ikinci çağrı zararsızdır).
        kimlikServisi.oturumuKapat()
        // Hesaba bağlı bellek durumu: öğretici bayrağı, seri bağlantısı, cüzdan/ilerleme/rozet kuyruğu.
        ilkOgreticiAkisi = false
        misafir = false
        ilkOgreticiIlerlemesiBekliyor = false
        seri.hesaptanAyril()
        hesabaGec(nil)
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
        // Pro kilidi: kısım 7/8 (yarış paneli) kartları planı buradan okur (Android: PlanStatus.effectivePlan).
        let guncelPlan: String = MagazaBagimliliklar.sahte.cuzdan.cuzdan.plan
        if haritaModeli.plan != guncelPlan {
            haritaModeli.plan = guncelPlan
            haritaModeli.yenile()
        }
        // Üst çubuktaki kalp: Pro/Premium planda `heart_pro_ic` (Android: EnergyDisplay.apply isPremium).
        let proPlan: Bool = PlanStatus.shared.isProPlan(plan: guncelPlan)
        if oturum.proPlan != proPlan {
            oturum.proPlan = proPlan
        }
        // Günlük seri (Android: MainActivity.refreshStreakUi).
        seriyiYenile()
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

    /// GorevlerEkrani.soruBaslat (Android: BlindingLessonFragment.newDailyQuestionInstance).
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

    /// GorevlerEkrani.odulAc (Android: TasksFragment.startDailyQuestionRewardFlow →
    /// NewChestFragment.newInstance(RARE)).
    func gunlukSoruOdulunuAc(_ istek: GunlukSoruOdulIstegi) {
        let kapandi: () -> Void = istek.kapandi
        ac(.sandik(SandikGirdisi(baslangicNadirligi: istek.nadirlik), kapandi: { _ in kapandi() }))
    }

    /// GorevlerEkrani.kartSecildi (Abaküs kartı) ve Kupa Yolu sekmesinin kartı.
    func kesfetKartiSecildi(_ kart: GunlukSoruKesfetKarti) {
        switch kart {
        case .kupaYolu:
            kupaSonucu = nil
            ac(.kupa)
        case .abakusPratik:
            yerTutucuAc("Abaküs", "Serbest abaküs alıştırması", ["Android: AbacusPracticeFragment"])
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
            // Android: TasksFragment → requestNewStreakPromptOnTasks (kupa testi dönüşü).
            seriSorusunuDene()
        case .gunlukSoru(let bildir):
            if sonuc.yonlendirme == .gunlukSoruTamam {
                bildir(true)
            } else if sonuc.yonlendirme == .gunlukSoruYanlis {
                bildir(false)
            }
            kapat(katman)
            // Android: günlük soru dönüşü de yeni seri sorusunu dener (requestNewStreakPromptOnTasks).
            seriSorusunuDene()
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

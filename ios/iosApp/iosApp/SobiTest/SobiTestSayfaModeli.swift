import Foundation
import Shared

// Sobi'nin testi panelinin durumu ve akışı.
//
// Android kaynağı: SobiTestSheet.kt (show → SobiDailyTest.load → choose → fill; pay → begin → planAsync).
// Kurallar ortak koddadır: SobiDailyTest (choose, plan, difficultyOptions, ruleRows, specText, seed,
// mergeLevels, unsentLevels, pickScores), SobiDailyTestStore (testsToday, extraTestCost, markExtraPaid).
//
// Basitleştirme: Android planı arka plan iş parçacığında kurar (birkaç yüz ms sürebilir). Ortak kod
// tek iş parçacığından çağrılmak üzere yazıldığı için (GELISTIRME.md) burada ana iş parçacığında, düğme
// "Hazırlanıyor…" yazısını çizdikten sonraki turda kurulur.

/// Oynatıcıya giden: plan ve testin bugünkü sırası.
struct SobiTestBaslangici {
    let plan: SobiTestPlanner.Plan
}

/// Paneldeki bir zorluk kartı.
struct SobiTestZorlukKarti: Identifiable {
    let id: Int
    let baslik: String
    let seviye: String
    let aciklama: String
}

/// Paneldeki bir kural satırı; `seviye` nil ise (çarpma) seviye yazısı ve çubuğu yok.
struct SobiTestKuralSatiri: Identifiable {
    let id: String
    let ad: String
    let seviye: Int?
}

enum SobiTestSayfaDurumu {
    case yukleniyor
    /// Kurallar ve zorluk kartları.
    case hazir
    /// Veri yok / anahtar yetmedi / ağ hatası: Sobi bir cümle söyler, düğme "Tamam".
    case bos(String)
}

final class SobiTestSayfaModeli: ObservableObject {
    @Published private(set) var durum: SobiTestSayfaDurumu = .yukleniyor
    @Published private(set) var baslik: String
    @Published private(set) var altBaslik: String
    @Published private(set) var kurallar: [SobiTestKuralSatiri] = []
    @Published private(set) var kartlar: [SobiTestZorlukKarti] = []
    @Published private(set) var secilen: Int = 0
    /// Ek testin anahtar bedeli (0: ücretsiz). Doluysa düğme "Başla · 3" + anahtar.
    @Published private(set) var bedel: Int = 0
    @Published private(set) var hazirlaniyor: Bool = false
    /// Sobi'nin hâli (Android: GREET açılışta, THINK hazırlanırken ve anahtar yetmeyince).
    @Published private(set) var maskot: String = "GREET"
    /// "Pro'da ek testler ücretsiz ›" bağlantısı.
    @Published private(set) var proGoster: Bool = false

    private let servis: SobiTestVeriServisi
    private let depo: SobiDailyTestStore
    private var bilgi: SobiDailyTest.Info? = nil
    private var yuklendi: Bool = false

    init(servis: SobiTestVeriServisi = SobiTestOrtak.servis, depo: SobiDailyTestStore = SobiTestOrtak.depo) {
        self.servis = servis
        self.depo = depo
        let ek: Bool = depo.isDoneToday(today: SobiTestOrtak.bugun())
        baslik = Metin.al(ek ? "sobi_test_dialog_extra_title" : "sobi_test_dialog_title")
        altBaslik = Metin.al(ek ? "sobi_test_subtitle_extra" : "sobi_test_subtitle")
    }

    /// Hazır bir durumla kurulmuş model (ekran galerisi).
    static func galeri(_ durum: SobiTestSayfaDurumu, bilgi: SobiDailyTest.Info?, ekTest: Bool, proGoster: Bool) -> SobiTestSayfaModeli {
        let model = SobiTestSayfaModeli()
        model.yuklendi = true
        model.baslik = Metin.al(ekTest ? "sobi_test_dialog_extra_title" : "sobi_test_dialog_title")
        model.altBaslik = Metin.al(ekTest ? "sobi_test_subtitle_extra" : "sobi_test_subtitle")
        if let b = bilgi {
            model.doldur(b)
        } else {
            model.durum = durum
        }
        if case .bos = durum {
            model.durum = durum
            model.maskot = "THINK"
        }
        model.proGoster = proGoster
        return model
    }

    /// Android SobiDailyTest.load: sunucu (olmazsa bu cihaz) + derslerden görülmüş kurallar.
    func yukle() {
        if yuklendi {
            return
        }
        yuklendi = true
        let yerelPuanlar: [SobiTestPlanner.RuleScore] = SobiDailyTest.shared.scoresFromEntries(
            entries: SahteSobiAnalizKuralDeposu.ortak.depo.snapshot(withMental: true)
        )
        let yerelSeviyeler: [String: KotlinInt] = depo.localLevels()
        let gorulen: Set<String> = SobiTestOrtak.gorulenKurallar()
        servis.sunucuVerisiniOku { [weak self] (veri: SobiTestSunucuVerisi?) in
            guard let self = self else { return }
            var puanlar: [SobiTestPlanner.RuleScore] = yerelPuanlar
            var seviyeler: [String: KotlinInt] = yerelSeviyeler
            if let sunucu = veri {
                let sunucuSeviyeleri: [String: KotlinInt] = SobiTestOrtak.kotlinSozluk(sunucu.seviyeler)
                seviyeler = SobiDailyTest.shared.mergeLevels(local: yerelSeviyeler, server: sunucuSeviyeleri)
                let gonderilmemis: [String: KotlinInt] = SobiDailyTest.shared.unsentLevels(local: yerelSeviyeler, server: sunucuSeviyeleri)
                if !gonderilmemis.isEmpty {
                    self.servis.seviyeleriGonder(SobiTestOrtak.sozluk(gonderilmemis))
                }
                puanlar = SobiDailyTest.shared.pickScores(server: sunucu.puanlar, local: yerelPuanlar)
            }
            let bilgi = SobiDailyTest.Info(scores: puanlar, levels: seviyeler, lessonSeen: gorulen)
            self.doldur(bilgi)
        }
    }

    /// Android SobiTestSheet.fill.
    private func doldur(_ yeniBilgi: SobiDailyTest.Info) {
        guard let secim = SobiDailyTest.shared.choose(info: yeniBilgi) else {
            durum = .bos(Metin.al("sobi_test_no_data"))
            return
        }
        bilgi = yeniBilgi
        let metin: SobiAnalizMetinCozucusu = SobiTestOrtak.metin
        var satirlar: [SobiTestKuralSatiri] = []
        for satir in SobiDailyTest.shared.ruleRows(choice: secim, metin: metin) {
            let seviye: Int? = satir.level.map { Int($0.intValue) }
            satirlar.append(SobiTestKuralSatiri(id: satir.key, ad: satir.label, seviye: seviye))
        }
        kurallar = satirlar
        var yeniKartlar: [SobiTestZorlukKarti] = []
        for secenek in SobiDailyTest.shared.difficultyOptions(choice: secim) {
            let seviye: Int32 = secenek.level
            yeniKartlar.append(SobiTestZorlukKarti(
                id: Int(secenek.offset),
                baslik: Metin.al(secenek.titleResource),
                seviye: Metin.al("sobi_test_level_short", Int(seviye)),
                aciklama: SobiDailyTest.shared.specText(level: seviye, metin: metin)
            ))
        }
        kartlar = yeniKartlar
        secilen = 0
        let bugun: String = SobiTestOrtak.bugun()
        bedel = Int(depo.extraTestCost(today: bugun, isPro: servis.proMu, isApprovedTeacher: servis.onayliOgretmen))
        durum = .hazir
    }

    func sec(_ kaydirma: Int) {
        if hazirlaniyor {
            return
        }
        secilen = kaydirma
    }

    /// Android SobiTestSheet.pay: ek testin anahtarı (sunucuda); ücretsizse doğrudan başlar.
    func basla(_ hazir: @escaping (SobiTestBaslangici) -> Void) {
        guard case .hazir = durum, !hazirlaniyor else { return }
        let bugun: String = SobiTestOrtak.bugun()
        // Android: ücretsizse ya da hesap yoksa doğrudan başlar.
        if bedel == 0 || servis.kullaniciKimligi == nil {
            hazirla(hazir)
            return
        }
        hazirlaniyor = true
        let sira: Int = Int(depo.testsToday(today: bugun))
        let istenen: Int = bedel
        servis.ekTestiBaslat(sira: sira) { [weak self] (sonuc: SobiTestEkTestSonucu) in
            guard let self = self else { return }
            switch sonuc {
            case .tamam:
                self.depo.markExtraPaid(today: bugun)
                self.hazirlaniyor = false
                self.hazirla(hazir)
            case .anahtarYetmedi:
                self.hazirlaniyor = false
                self.maskot = "THINK"
                self.durum = .bos(Metin.al("sobi_test_not_enough_keys", istenen, self.servis.anahtar))
                self.proGoster = self.servis.proTeklifiUygun
            case .agHatasi:
                self.hazirlaniyor = false
                self.maskot = "THINK"
                self.durum = .bos(Metin.al("sobi_error_network"))
            }
        }
    }

    /// Android SobiTestSheet.begin → SobiDailyTest.planAsync.
    private func hazirla(_ hazir: @escaping (SobiTestBaslangici) -> Void) {
        guard let b = bilgi else { return }
        hazirlaniyor = true
        maskot = "THINK"
        let kaydirma: Int32 = Int32(secilen)
        let bugun: String = SobiTestOrtak.bugun()
        let tohum: Int32 = SobiDailyTest.shared.seed(today: bugun, testsToday: depo.testsToday(today: bugun), offset: kaydirma)
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            let plan: SobiTestPlanner.Plan? = SobiDailyTest.shared.plan(info: b, offset: kaydirma, seed: tohum)
            self.hazirlaniyor = false
            guard let kurulan = plan else {
                self.durum = .bos(Metin.al("sobi_test_no_data"))
                return
            }
            hazir(SobiTestBaslangici(plan: kurulan))
        }
    }
}

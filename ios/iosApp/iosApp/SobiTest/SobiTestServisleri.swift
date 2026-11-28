import Combine
import Foundation
import Shared

// Sobi'nin günlük testinin dış bağımlılıkları (Firestore, Cloud Function, cüzdan, plan) ve uygulama
// boyunca paylaşılan durum (günlük sayaç deposu, harita düğmesinin modeli).
//
// Android kaynakları: SobiDailyTest.kt (load / loadStats / saveLevels), SobiTestSheet.pay
// (ServerSobi.startExtraTest, UserWalletFirestore.cacheKeys), SobiAccess.kt.
// Kurallar ortak koddadır (shared/.../sobitest/: SobiDailyTest, SobiDailyTestStore, SobiTestPlanner).
//
// Sunucu sözleşmesi (Android'deki adlarla; gerçek servis bunları uygulamalı):
//   - Okuma: Firestore `homeworkRuleStats/{uid}` belgesi. Alanlar: `recent` (kural → [{c, w}] son ~50
//     işlem penceresi), `tally` (kural → {c, w}, bütün zamanlar), `sobiLevel` (kural → 1..20). Çözümü
//     ortak kodda: SobiDailyTest.scoresOf / levelsOf (belge sözlüğüyle çağrılabilir).
//   - Yazma: Cloud Function `recordSobiLevels`, gövde `{"levels": {"add_ten": 6, "m_add_five": 2}}`.
//     Başarısızsa seviye cihazda kalır ve bir sonraki okumada yeniden gönderilir (SobiDailyTest.unsentLevels).
//   - Ek test: Cloud Function `startSobiExtraTest` (bugünkü sıra ile; functions/sobi/wallet.js,
//     EXTRA_TEST_KEYS = 3). Döner: kalan anahtar. Yetersiz anahtarda hata kodu (ServerSobi.isNotEnoughKeys).
//   - Kural sayımı: her cevap SobiRuleStatsStore'a (bekleyen sayım sunucuya recordRuleTally ile; bkz.
//     SobiAnaliz/SobiAnalizHizmetleri.swift, ortak sobianaliz/SobiRuleStats.kt).

/// Sunucudaki kural başarısı ve seviyeler (homeworkRuleStats/{uid}, çözülmüş hali).
struct SobiTestSunucuVerisi {
    let puanlar: [SobiTestPlanner.RuleScore]
    let seviyeler: [String: Int]
}

/// Ek testin anahtar ödemesinin sonucu (Android: ServerSobi.startExtraTest onResult / onFailure).
enum SobiTestEkTestSonucu {
    /// Anahtar düştü; `kalanAnahtar` üst paneldeki göstergeye yazılır.
    case tamam(kalanAnahtar: Int)
    /// Anahtar yetmedi (ServerSobi.isNotEnoughKeys).
    case anahtarYetmedi
    /// Ağ ya da sunucu hatası.
    case agHatasi
}

protocol SobiTestVeriServisi: AnyObject {
    /// Oturumdaki hesap (Android: FirebaseAuth.currentUser?.uid); nil = hesapsız ("guest").
    var kullaniciKimligi: String? { get }
    /// Android: PlanStatus.isProPlan(energyManager.getUserPlan()).
    var proMu: Bool { get }
    /// Android: MainActivity.isApprovedTeacher() (onaylı öğretmende ek test ücretsiz).
    var onayliOgretmen: Bool { get }
    /// Android: SobiAccess.isAllowed — öğretmen hesabında Sobi testi yok.
    var sobiAcik: Bool { get }
    /// Son bilinen anahtar (Android: UserWalletFirestore.getCachedKeys).
    var anahtar: Int { get }
    /// Android: MainActivity.isProTrialPromoEligible (anahtar yetmeyince "Pro'da ek testler ücretsiz").
    var proTeklifiUygun: Bool { get }
    /// homeworkRuleStats/{uid}; okunamazsa nil (Android: addOnFailureListener → bu cihazın verisi).
    func sunucuVerisiniOku(tamam: @escaping (SobiTestSunucuVerisi?) -> Void)
    /// recordSobiLevels (Android: başarısızlık yalnızca günlüğe yazılır).
    func seviyeleriGonder(_ seviyeler: [String: Int])
    /// startSobiExtraTest: [sira] bugün bitirilmiş test sayısı (SobiDailyTest.testsToday).
    func ekTestiBaslat(sira: Int, tamam: @escaping (SobiTestEkTestSonucu) -> Void)
}

/// Sahte servis: Firebase yok. Sunucu belgesi örnek kural başarısı döndürür (Android SobiTestPlannerTest'teki
/// `add` listesi: kuralsız 40/2, 5'lik 30/3, 10'luk 12/10), seviyeler bellekte; ek test mağazanın sahte
/// cüzdanından 3 anahtar düşer.
final class SahteSobiTestVeriServisi: SobiTestVeriServisi {
    static let ortak: SahteSobiTestVeriServisi = SahteSobiTestVeriServisi()

    /// Sunucuya "yazılmış" seviyeler (bellekte).
    private(set) var sunucuSeviyeleri: [String: Int] = [:]
    /// false ise sunucu belgesi boş gelir (galeride "Biraz ders çöz…" durumu).
    var ornekVeriVar: Bool = true

    /// Sahte hesap: ek test ödeme yolu da denensin diye dolu.
    var kullaniciKimligi: String? { return "sahte-ogrenci" }

    var proMu: Bool {
        return PlanStatus.shared.isProPlan(plan: MagazaBagimliliklar.sahte.cuzdan.cuzdan.plan)
    }

    var onayliOgretmen: Bool { return false }

    var sobiAcik: Bool { return true }

    var anahtar: Int {
        return MagazaBagimliliklar.sahte.cuzdan.cuzdan.anahtar
    }

    var proTeklifiUygun: Bool { return !proMu }

    func sunucuVerisiniOku(tamam: @escaping (SobiTestSunucuVerisi?) -> Void) {
        let ornek: Bool = ornekVeriVar
        let seviyeler: [String: Int] = sunucuSeviyeleri
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            if !ornek {
                tamam(SobiTestSunucuVerisi(puanlar: [], seviyeler: seviyeler))
                return
            }
            let puanlar: [SobiTestPlanner.RuleScore] = [
                SobiTestPlanner.RuleScore(key: "add_direct", correct: 40, wrong: 2),
                SobiTestPlanner.RuleScore(key: "add_five", correct: 30, wrong: 3),
                SobiTestPlanner.RuleScore(key: "add_ten", correct: 12, wrong: 10),
            ]
            tamam(SobiTestSunucuVerisi(puanlar: puanlar, seviyeler: seviyeler))
        }
    }

    func seviyeleriGonder(_ seviyeler: [String: Int]) {
        for (anahtar, deger) in seviyeler {
            sunucuSeviyeleri[anahtar] = deger
        }
    }

    func ekTestiBaslat(sira: Int, tamam: @escaping (SobiTestEkTestSonucu) -> Void) {
        let bedel: Int = Int(SobiDailyTest.shared.EXTRA_TEST_KEYS)
        let cuzdan: MagazaCuzdanServisi = MagazaBagimliliklar.sahte.cuzdan
        if cuzdan.cuzdan.anahtar < bedel {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                tamam(.anahtarYetmedi)
            }
            return
        }
        cuzdan.degistir(
            altinFarki: 0,
            anahtarFarki: -bedel,
            gerekce: WalletReason.shared.SPEND,
            geriAlmaJetonu: nil,
            tamam: { (degisim: MagazaCuzdanDegisimi?) in
                if let yeni = degisim {
                    tamam(.tamam(kalanAnahtar: yeni.cuzdan.anahtar))
                } else {
                    tamam(.agHatasi)
                }
            }
        )
    }
}

/// Uygulama boyunca tek: günlük sayaç deposu, veri servisi, ilerleme kaynağı ve harita düğmesinin modeli.
/// Yönetici (Yonetim/YonetimSobiTestAkisi.swift) `ilerlemeKaynagi` ve `dugme.acildi`'yı bağlar.
enum SobiTestOrtak {
    /// Android: SharedPreferences "sobi_test". Kalıcılık yok (bellek; diğer sahte depolar gibi).
    static let depo: SobiDailyTestStore = SobiDailyTestStore(prefs: InMemoryKeyValueStore())
    static var servis: SobiTestVeriServisi = SahteSobiTestVeriServisi.ortak
    /// Ders ilerlemesi (Part 1'in açılma satırı, Part 3–6'nın görülmüş kuralları). nil ise şablon.
    static weak var ilerlemeKaynagi: HaritaIlerlemeKaynagi? = nil
    /// Haritadaki düğmenin durumu (kısım seçimi ve ders listesi aynı düğmeyi gösterir).
    static let dugme: SobiTestDugmeModeli = SobiTestDugmeModeli()
    /// Ortak kodun metin çözücüsü (Metin.al).
    static let metin: SobiAnalizMetinCozucusu = SobiAnalizMetinCozucusu()
    /// Yöneticinin katman yığını boşalınca düğme tazelenir (Android: ders dönüşünde refresh(nudge = false)).
    static var katmanAboneligi: AnyCancellable? = nil

    /// Türkiye saatiyle bugün ("yyyy-MM-dd").
    static func bugun() -> String {
        let simdi: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
        return SobiDailyTest.shared.today(nowMs: simdi)
    }

    /// Bir kısmın güncel satırları (Android: GlobalLessonData.loadLessonItemsForPart).
    static func satirlar(_ partId: Int) -> [LessonItem] {
        let kimlik: Int32 = Int32(partId)
        let bulut: [LessonItem]? = ilerlemeKaynagi?.bulutOgeleri(partId: kimlik)
        return GlobalLessonData.shared.loadLessonItemsForPart(partId: kimlik, cloudItems: bulut)
    }

    /// Android: SobiDailyTest.checkUnlocked (Part 1'in 6. satırı; açıldıktan sonra önbellekte).
    static func acikMi() -> Bool {
        let part: Int = Int(SobiDailyTest.shared.UNLOCK_PART)
        return depo.checkUnlocked(uid: servis.kullaniciKimligi, part1Items: satirlar(part))
    }

    /// Android: SobiDailyTest.loadLessonSeen (Part 3, 4, 5, 6).
    static func gorulenKurallar() -> Set<String> {
        var listeler: [[LessonItem]] = []
        for kutu in SobiDailyTest.shared.LESSON_PARTS {
            listeler.append(satirlar(Int(kutu.intValue)))
        }
        return depo.lessonSeen(uid: servis.kullaniciKimligi, partItems: listeler)
    }

    /// Kotlin `Map<String, Int>` → Swift.
    static func sozluk(_ ham: [String: KotlinInt]) -> [String: Int] {
        var sonuc: [String: Int] = [:]
        for (anahtar, deger) in ham {
            sonuc[anahtar] = Int(deger.intValue)
        }
        return sonuc
    }

    /// Swift → Kotlin `Map<String, Int>`.
    static func kotlinSozluk(_ ham: [String: Int]) -> [String: KotlinInt] {
        var sonuc: [String: KotlinInt] = [:]
        for (anahtar, deger) in ham {
            sonuc[anahtar] = KotlinInt(int: Int32(clamping: deger))
        }
        return sonuc
    }
}

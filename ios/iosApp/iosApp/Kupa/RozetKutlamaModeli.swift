import Foundation
import Shared
import SwiftUI

// Rozet kutlamasının (seviye atlama) durumu.
//
// Android karşılığı: BadgeFragment'ın kutlama kipi — newLevelUpSequenceInstance(payloads, index) ile açılır,
// onViewCreated → startLevelUpCelebration:
//   1. Rozet BİR ÖNCEKİ durumunda gösterilir (taban son karede; önceki ton yoksa düz gri, varsa o ton; üst
//      animasyon tutma karesinde, önceki ton yoksa gri) ve eski sayı yazılıdır.
//   2. İlerleme çubuğu 3 sn boyunca (DecelerateInterpolator) "from/target"tan "target/target"a dolar.
//   3. Dolunca: rozet sesi çalar, yazı "Tamamlandı !", çubuk altın, taban yeni tona boyanıp baştan oynar,
//      sayı yeni değerle 400 ms'de sıfırdan büyür, üst animasyon baştan oynar (bazı modlarda 1 sn'de
//      sıfırdan büyüyerek) ve tutma karesinde durur.
//   4. Animasyon bitince dokunmak kuyruktaki sonraki rozete (goToNextCelebrationInQueue) ya da
//      kapanışa (closeCelebrationFragmentWithSlide) götürür.
// Sayaç değeri, yüzde, eski/yeni değer, ton ve animasyon ayarı ortak koddandır (BadgeFragmentLogic).

/// Kuyruktaki bir kutlamanın ekrana dönük verisi (hepsi ortak koddan çözülür).
struct RozetKutlamaVerisi: Equatable {
    /// Ortak kod `BadgeAnimMode` adı (ör. "DAILY_ONLY").
    let modAdi: String
    let karate: Bool
    /// Çubuğun başlangıç sayısı (lockedProgressCurrent) ve hedefi (lockedProgressTarget).
    let baslangic: Int
    let hedef: Int
    /// Eski ve yeni gösterilen sayı (rozetin üstündeki büyük sayı).
    let eskiDeger: Int
    let yeniDeger: Int
    /// "BRONZE" / "SILVER" / "ORIGINAL" / "RED" / "PURPLE"; boş = kademe yok (kilitli).
    let oncekiTon: String
    let yeniTon: String
    let aciklama: String
    /// Boş = taban yok (siyah kuşak).
    let tabanDosyasi: String
    let ustDosyasi: String
    /// Üst animasyonun durduğu kare (Android topHoldFrame): 9999 = son kare.
    let ustTutmaKaresi: Double
    let ustOlcek: CGFloat
    let ustHiz: Double
    /// bg_badge_circle_frame: üst animasyon daireye kırpılır.
    let daireli: Bool
    /// Üst animasyonun ek tekrarı (Android repeatCount).
    let ustTekrar: Int
    /// Üst animasyon 1 sn'de sıfırdan büyür.
    let sifirdanBuyur: Bool
    /// false: kutlama kipi yok (madalya, kupa) — düz rozet ekranı gösterilir.
    let kutlamaVar: Bool

    /// Android: `daily_tasks_complite_badge2.json` (BadgeFragment.setupLevelUpStaticBadge).
    static let tabanAdi: String = "daily_tasks_complite_badge2.json"

    /// Kuyruktaki `sira`. öğeyi çözer (ortak kod: BadgeFragmentLogic.parseQueueItem).
    static func olustur(kuyruk: [String], sira: Int) -> RozetKutlamaVerisi {
        let mantik = BadgeFragmentLogic.shared
        let oge = mantik.parseQueueItem(payloads: kuyruk, index: Int32(sira))
        let plan = mantik.levelUpCelebrationPlan(
            mode: oge.mode,
            fromProgress: oge.from,
            toProgress: oge.to,
            reachedTarget: oge.target
        )
        let ayar = mantik.levelUpAnimConfig(mode: oge.mode)
        let hedefDegeri: KotlinInt = KotlinInt(int: oge.target)
        let aciklama: String = mantik.badgeDetailText(mode: oge.mode, unlocked: true, celebrateTarget: hedefDegeri)
        let ad: String = oge.mode.name
        let karate: Bool = ad == "KARATE"
        // Android: onViewCreated — kutlama yalnızca ilerleme çubuklu modlarda ve karatede çalışır.
        let kutlamaVar: Bool = mantik.shouldShowProgressForMode(mode: oge.mode) || karate
        return RozetKutlamaVerisi(
            modAdi: ad,
            karate: karate,
            baslangic: Int(plan.lockedProgressCurrent),
            hedef: Int(plan.lockedProgressTarget),
            eskiDeger: Int(plan.oldShownValue),
            yeniDeger: Int(plan.newShownValue),
            oncekiTon: plan.previousTone?.name ?? "",
            yeniTon: plan.newTone?.name ?? "",
            aciklama: aciklama,
            tabanDosyasi: karate ? "" : RozetKutlamaVerisi.tabanAdi,
            ustDosyasi: ayar.topAsset,
            ustTutmaKaresi: Double(ayar.topHoldFrame),
            ustOlcek: CGFloat(ayar.topScale),
            ustHiz: Double(ayar.topSpeed),
            daireli: ayar.withCircleFrame,
            ustTekrar: Int(ayar.topRepeatCount),
            sifirdanBuyur: ayar.growFromZero,
            kutlamaVar: kutlamaVar
        )
    }
}

final class RozetKutlamaModeli: ObservableObject {
    enum Asama: Equatable {
        /// Çubuk doluyor (rozet eski hâlinde).
        case sayim
        /// Yeni renkler ve animasyonlar oynuyor.
        case kutlama
        /// Animasyon bitti; dokunmak ilerletir.
        case bitti
    }

    let veri: RozetKutlamaVerisi

    @Published private(set) var asama: Asama = Asama.sayim
    @Published private(set) var ilerlemeMetni: String
    @Published private(set) var ilerlemeYuzdesi: Int
    /// Çubuk altın ve "Tamamlandı !" (Android applyMissionProgressOverlay done = true).
    @Published private(set) var tamamlandi: Bool = false
    @Published private(set) var gosterilenDeger: Int
    /// Sayının ölçeği (yeni değer 400 ms'de sıfırdan büyür).
    @Published private(set) var degerOlcegi: CGFloat = 1
    /// Üst animasyonun ölçek çarpanı (sıfırdan büyüyen modlarda 0 → 1).
    @Published private(set) var ustCarpani: CGFloat = 1
    /// Üst animasyonun kaçıncı oynatması (tekrar için görünüm yeniden kurulur).
    @Published private(set) var ustTur: Int = 0

    private let ses: KupaSesCalici
    private var sayac: Timer? = nil
    private var baslangicZamani: Date = Date()
    private var kalanTekrar: Int
    private var basladi: Bool = false

    init(veri: RozetKutlamaVerisi, ses: KupaSesCalici) {
        self.veri = veri
        self.ses = ses
        let ilk = BadgeFragmentLogic.shared.lockedProgressUi(
            lockedProgressCurrent: Int32(veri.baslangic),
            lockedProgressTarget: Int32(veri.hedef)
        )
        self.ilerlemeMetni = ilk.text
        self.ilerlemeYuzdesi = Int(ilk.percent)
        self.gosterilenDeger = veri.eskiDeger
        self.kalanTekrar = veri.ustTekrar
    }

    deinit {
        sayac?.invalidate()
    }

    // MARK: Yaşam döngüsü

    /// Ekran göründü (Android: onViewCreated → BadgeSoundPlayer.preload + startLevelUpCelebration).
    func basla() {
        if basladi {
            return
        }
        basladi = true
        ses.rozetSesiniHazirla()
        baslangicZamani = Date()
        sayac = Timer.scheduledTimer(withTimeInterval: 1.0 / 60.0, repeats: true) { [weak self] _ in
            self?.sayimAdimi()
        }
    }

    func dur() {
        sayac?.invalidate()
        sayac = nil
    }

    // MARK: Sayım (ValueAnimator, 3000 ms, DecelerateInterpolator)

    private func sayimAdimi() {
        let toplam: Double = Double(BadgeFragmentLogic.shared.CELEBRATION_COUNT_DURATION_MS) / 1000.0
        let gecen: Double = Date().timeIntervalSince(baslangicZamani)
        let oran: Double = min(1.0, max(0.0, gecen / toplam))
        // DecelerateInterpolator(1.0): 1 − (1 − t)²
        let kalan: Double = 1.0 - oran
        let kesir: Float = Float(1.0 - kalan * kalan)
        let mantik = BadgeFragmentLogic.shared
        let simdiki: Int32 = mantik.celebrationAnimatedCurrent(
            fromProgress: Int32(veri.baslangic),
            reachedTarget: Int32(veri.hedef),
            animatedFraction: kesir
        )
        let arayuz = mantik.celebrationProgressUi(fraction: kesir, current: simdiki, target: Int32(veri.hedef))
        ilerlemeMetni = arayuz.text
        ilerlemeYuzdesi = Int(arayuz.percent)
        if oran >= 1.0 {
            sayac?.invalidate()
            sayac = nil
            kutlamayiBaslat()
        }
    }

    /// Android: ValueAnimator.onAnimationEnd.
    private func kutlamayiBaslat() {
        ses.rozetSesiniCal()
        tamamlandi = true
        ilerlemeMetni = Metin.al("ui_tamamlandi")
        ilerlemeYuzdesi = 100
        gosterilenDeger = veri.yeniDeger
        asama = Asama.kutlama
        // Android applyRewardAmount: sayı 0 → 1, 400 ms.
        degerOlcegi = 0
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) { [weak self] in
            withAnimation(.easeInOut(duration: 0.4)) {
                self?.degerOlcegi = 1
            }
        }
        // Android cfg.growFromZero: üst animasyon 1 sn'de 0 → hedef ölçek (ObjectAnimator, AnimatorSet).
        if veri.sifirdanBuyur {
            ustCarpani = 0
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) { [weak self] in
                withAnimation(.easeInOut(duration: 1.0)) {
                    self?.ustCarpani = 1
                }
            }
        }
        // Güvenlik: animasyon dosyası bulunamaz ya da bitiş haberi gelmezse ekran kilitli kalmasın.
        DispatchQueue.main.asyncAfter(deadline: .now() + 15.0) { [weak self] in
            guard let ben = self else { return }
            if ben.asama == Asama.kutlama {
                ben.asama = Asama.bitti
            }
        }
    }

    /// Üst animasyon sonuna kadar oynadı (Android: rocketBadgeAnim.onAnimationEnd → progress = topHold).
    func ustAnimasyonuBitti() {
        if asama != Asama.kutlama {
            return
        }
        if kalanTekrar > 0 {
            kalanTekrar -= 1
            ustTur += 1
            return
        }
        asama = Asama.bitti
    }
}

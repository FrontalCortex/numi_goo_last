import Foundation
import Shared
import SwiftUI

// Kupa Yolu ekranının durumu.
//
// Android karşılığı: TasksFragment.kt — showCupPathPanel, displayCupPathDialog,
// updateExistingCupPathDialog, showCupDifficultyPanel, launchCupModeLesson, consumePendingCupDelta,
// animateCupScore. Kurallar ortak koddadır (CupPathLogic, CupRuleEngine); burada yalnızca
// ekran durumu tutulur ve servisler çağrılır.

/// Paneldeki bir kartın ekran durumu.
struct KupaKartDurumu: Identifiable, Equatable {
    /// 1..6
    let kartNo: Int
    let baslik: String
    let animasyonDosyasi: String
    /// Kart açıkken arka plan rengi (Android R.color adı).
    let acikRenkAdi: String
    /// Firestore alanı.
    let alan: String
    /// Kartı açan part.
    let partId: Int
    let korleme: Bool
    let cikarma: Bool
    let carpma: Bool

    var acik: Bool = false
    /// Firestore'dan okunan skor; henüz gelmediyse nil.
    var skor: Int? = nil
    /// Ekranda yazan skor (sayaç animasyonu sırasında ara değerler).
    var gosterilenSkor: Int = 200
    /// "+24" / "-20"; nil = gizli.
    var deltaMetni: String? = nil
    var deltaRengi: UInt32 = 0x4CAF50

    var id: Int { kartNo }
}

/// Zorluk panelinin durumu (Android panel_cup_difficulty).
struct KupaZorlukPaneliDurumu: Equatable {
    let kart: KupaKartDurumu
    let baslik: String
    let kaydiriciGorunur: Bool
    /// Kupa skoru okununca dolar; o zamana kadar BAŞLAT kapalıdır.
    var kupaSkoru: Int? = nil
    /// Basamak seçenekleri; boşsa seçim satırı gizlidir.
    var basamakSecenekleri: [Int] = []
    /// 0 = "Rastgele".
    var secilenBasamak: Int = 0
}

final class KupaModeli: ObservableObject {
    @Published private(set) var kartlar: [KupaKartDurumu] = []
    /// Kilit durumları gelene kadar true: kartlar kapalı görünür, skorlar gizlidir.
    @Published private(set) var yukleniyor: Bool = true
    @Published private(set) var enerjiMetni: String = "0"
    @Published var zorlukPaneli: KupaZorlukPaneliDurumu? = nil
    /// Kaydırıcı konumu (0..4). Panel kapanıp açılınca korunur (Android lastCupDifficultyProgress).
    @Published var zorluk: Double = 0

    private let ortam: KupaOrtami
    private let mantik: CupPathLogic = CupPathLogic.shared
    private var yuklendi: Bool = false
    private var sonKartTiklamasi: Date = Date.distantPast
    private var islenenSonuc: KupaDersSonucu? = nil
    /// Kart → ders başlarken okunan skor (Android GlobalValues.currentLessonOldCupScore).
    private var dersOncesiSkor: [Int: Int] = [:]
    private var sayacZamanlayicisi: Timer? = nil

    init(ortam: KupaOrtami) {
        self.ortam = ortam
        self.kartlar = mantik.CARDS.map { k in
            KupaKartDurumu(
                kartNo: Int(k.cardNumber),
                baslik: k.title,
                animasyonDosyasi: k.animFile,
                acikRenkAdi: k.activeColorRes,
                alan: k.field,
                partId: Int(k.unlockPartId),
                korleme: k.isBlinding,
                cikarma: k.isExtraction,
                carpma: k.isMultiplication
            )
        }
        enerjiyiYenile()
    }

    deinit {
        sayacZamanlayicisi?.invalidate()
    }

    // MARK: - Metinler (ortak koddan)

    var panelBasligi: String { mantik.PANEL_TITLE }
    var zorlukIletisi: String { mantik.DIFFICULTY_MESSAGE }
    var basamakEtiketi: String { mantik.DIGIT_LABEL }
    var rastgeleEtiketi: String { mantik.DIGIT_RANDOM_LABEL }
    var baslatEtiketi: String { mantik.START_LABEL }
    var kaydiriciEnBuyuk: Double { Double(mantik.DIFFICULTY_SEEKBAR_MAX) }

    /// Kaydırıcının üstündeki sayı (0, 25, 50, 75, 100).
    var zorlukYuzdesi: String {
        let yuzde: Int32 = mantik.difficultyPercent(progress: Int32(zorluk.rounded()))
        return String(yuzde)
    }

    func basamakSecenekEtiketi(_ basamak: Int) -> String {
        if basamak <= 0 { return rastgeleEtiketi }
        return mantik.digitOptionLabel(digits: Int32(basamak))
    }

    // MARK: - Yükleme

    /// Panel açılırken: kartlar kapalı gösterilir, skorlar ve kilitler arka planda okunur.
    func yukle() {
        if yuklendi {
            enerjiyiYenile()
            return
        }
        yuklendi = true
        skorlariOku(atlanacakKart: nil)
        kilitleriOku()
    }

    private func skorlariOku(atlanacakKart: Int?) {
        for kart in kartlar where kart.kartNo != atlanacakKart {
            let kartNo: Int = kart.kartNo
            ortam.skorDeposu.kupaSkorunuGetir(alan: kart.alan) { [weak self] skor in
                self?.skoruYaz(kartNo: kartNo, skor: skor)
            }
        }
    }

    private func skoruYaz(kartNo: Int, skor: Int) {
        guard let i = kartlar.firstIndex(where: { $0.kartNo == kartNo }) else { return }
        kartlar[i].skor = skor
        kartlar[i].gosterilenSkor = Int(mantik.shownScore(score: Int32(skor)))
    }

    private func kilitleriOku() {
        var kalan: Int = kartlar.count
        var sonuclar: [Int: Bool] = [:]
        for kart in kartlar {
            let kartNo: Int = kart.kartNo
            ortam.kilitKaynagi.kartAcikMi(partId: kart.partId) { [weak self] acik in
                sonuclar[kartNo] = acik
                kalan -= 1
                if kalan == 0 {
                    self?.kilitleriUygula(sonuclar)
                }
            }
        }
    }

    private func kilitleriUygula(_ sonuclar: [Int: Bool]) {
        for i in kartlar.indices {
            kartlar[i].acik = sonuclar[kartlar[i].kartNo] ?? false
        }
        yukleniyor = false
    }

    /// Android startEnergyUpdateTimer: saniyede bir can metnini yeniler.
    func enerjiyiYenile() {
        let yeni: String = ortam.enerjiKaynagi.sonsuzEnerji ? "∞" : String(ortam.enerjiKaynagi.guncelEnerji)
        if yeni != enerjiMetni {
            enerjiMetni = yeni
        }
    }

    // MARK: - Kart ve zorluk paneli

    func kartaDokunuldu(_ kart: KupaKartDurumu) {
        guard kart.acik, !yukleniyor else { return }
        let simdi = Date()
        let aralik: TimeInterval = Double(mantik.CARD_CLICK_DEBOUNCE_MS) / 1000.0
        if simdi.timeIntervalSince(sonKartTiklamasi) < aralik { return }
        sonKartTiklamasi = simdi

        zorlukPaneli = KupaZorlukPaneliDurumu(
            kart: kart,
            baslik: mantik.difficultyTitle(isMultiplication: kart.carpma),
            kaydiriciGorunur: mantik.showsDifficultySlider(isMultiplication: kart.carpma)
        )
        // BAŞLAT, kupa skoru okunduktan sonra açılır.
        let kartNo: Int = kart.kartNo
        ortam.skorDeposu.kupaSkorunuGetir(alan: kart.alan) { [weak self] skor in
            self?.panelSkoruGeldi(kartNo: kartNo, skor: skor)
        }
    }

    private func panelSkoruGeldi(kartNo: Int, skor: Int) {
        guard var panel = zorlukPaneli, panel.kart.kartNo == kartNo else { return }
        panel.kupaSkoru = skor
        let secenekler: [KotlinInt] = mantik.digitOptions(
            cupScore: Int32(skor),
            isBlinding: panel.kart.korleme,
            isMultiplication: panel.kart.carpma
        )
        panel.basamakSecenekleri = secenekler.map { Int($0.intValue) }
        panel.secilenBasamak = 0
        zorlukPaneli = panel
    }

    func zorlukPaneliniKapat() {
        zorlukPaneli = nil
    }

    func basamakSec(_ basamak: Int) {
        guard var panel = zorlukPaneli else { return }
        panel.secilenBasamak = basamak
        zorlukPaneli = panel
    }

    enum BaslatSonucu {
        case bekle
        case enerjiYok
        case basla(KupaDersBaslangici)
    }

    /// BAŞLAT düğmesi. Enerji yoksa mağazaya yönlendirilir; varsa ders parametreleri üretilir.
    func baslat() -> BaslatSonucu {
        guard let panel = zorlukPaneli, let skor = panel.kupaSkoru else { return .bekle }
        let enerjiVar: Bool = mantik.canStartLesson(
            isInfiniteEnergy: ortam.enerjiKaynagi.sonsuzEnerji,
            currentEnergy: Int32(ortam.enerjiKaynagi.guncelEnerji)
        )
        zorlukPaneli = nil
        if !enerjiVar {
            return .enerjiYok
        }
        let d: CupLessonLaunch = mantik.buildCupLesson(
            cardNumber: Int32(panel.kart.kartNo),
            cupScore: Int32(skor),
            difficultyLevel: Int32(zorluk.rounded()),
            forcedDigitSize: Int32(panel.secilenBasamak)
        )
        dersOncesiSkor[panel.kart.kartNo] = skor
        let baslangic = KupaDersBaslangici(
            kartNo: Int(d.cardNumber),
            alan: panel.kart.alan,
            kupaSkoru: Int(d.cupScore),
            sureMs: Int(d.timePeriod),
            basamakSayisi: Int(d.cupDigitSize),
            sayiAdedi: Int(d.cupNumberCount),
            zorlukSeviyesi: Int(d.cupDifficultyLevel),
            kazancDeltasi: Int(d.cupWinDelta),
            kayipDeltasi: Int(d.cupLossDelta),
            korleme: d.isBlinding,
            cikarma: d.isExtraction,
            carpma: d.isMultiplication,
            sayilar: d.numbers.map { Int($0.intValue) },
            carpan1: Int(d.multiplicationFirst),
            carpan2: Int(d.multiplicationSecond)
        )
        return .basla(baslangic)
    }

    // MARK: - Ders dönüşü

    /// Dersten dönüldüğünde ilgili kartın skorunu eski değerden yenisine sayar, diğerlerini yeniden okur.
    func sonucuIsle(_ sonuc: KupaDersSonucu?) {
        guard let sonuc = sonuc, sonuc != islenenSonuc else { return }
        islenenSonuc = sonuc
        enerjiyiYenile()
        kilitleriOku()
        skorlariOku(atlanacakKart: sonuc.kartNo)

        guard let i = kartlar.firstIndex(where: { $0.kartNo == sonuc.kartNo }) else { return }
        let eski: Int = dersOncesiSkor[sonuc.kartNo] ?? kartlar[i].skor ?? 0
        let yeni: Int32 = mantik.scoreAfterLesson(oldScore: Int32(eski), delta: Int32(sonuc.delta))
        let bitis: Int = Int(mantik.scoreAnimationEnd(updatedScore: yeni))
        kartlar[i].skor = bitis
        dersOncesiSkor[sonuc.kartNo] = nil

        if !mantik.shouldAnimateScore(delta: Int32(sonuc.delta)) {
            kartlar[i].gosterilenSkor = bitis
            return
        }
        let baslangic: Int = Int(mantik.scoreAnimationStart(updatedScore: yeni, delta: Int32(sonuc.delta)))
        kartlar[i].gosterilenSkor = baslangic
        kartlar[i].deltaMetni = mantik.deltaText(delta: Int32(sonuc.delta))
        kartlar[i].deltaRengi = UInt32(mantik.deltaColorRgb(delta: Int32(sonuc.delta)))
        skoruSay(kartNo: sonuc.kartNo, baslangic: baslangic, bitis: bitis)
    }

    /// Android animateCupScore: 1 saniyede eski skordan yenisine sayar, bitince delta metnini gizler.
    private func skoruSay(kartNo: Int, baslangic: Int, bitis: Int) {
        sayacZamanlayicisi?.invalidate()
        let sure: TimeInterval = Double(mantik.SCORE_ANIMATION_MS) / 1000.0
        let baslama = Date()
        let fark: Double = Double(bitis - baslangic)
        sayacZamanlayicisi = Timer.scheduledTimer(withTimeInterval: 1.0 / 30.0, repeats: true) { [weak self] zamanlayici in
            guard let self = self else {
                zamanlayici.invalidate()
                return
            }
            guard let i = self.kartlar.firstIndex(where: { $0.kartNo == kartNo }) else {
                zamanlayici.invalidate()
                return
            }
            let gecen: TimeInterval = Date().timeIntervalSince(baslama)
            let oran: Double = min(1.0, gecen / sure)
            let ara: Double = Double(baslangic) + fark * oran
            self.kartlar[i].gosterilenSkor = Int(ara.rounded())
            if oran >= 1.0 {
                self.kartlar[i].gosterilenSkor = bitis
                self.kartlar[i].deltaMetni = nil
                zamanlayici.invalidate()
                self.sayacZamanlayicisi = nil
            }
        }
    }
}

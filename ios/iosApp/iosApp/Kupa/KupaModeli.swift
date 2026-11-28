import Foundation
import Shared
import SwiftUI

// Kupa Yolu ekranının durumu.
//
// Android karşılığı: TasksFragment.kt — showCupPathPanel, displayCupPathDialog,
// updateExistingCupPathDialog, showCupDifficultyPanel, launchCupModeLesson, consumePendingCupDelta,
// animateCupScore, bindCupPathRewards / bindCupPathRewardCard / applyCupPathRewardLoadingState /
// revealCupPathRewards, showCupCardLockedToastIfLocked. Kurallar ortak koddadır (CupPathLogic,
// CupPathRewardRepository, CupRuleEngine, CupScoreService); burada yalnızca ekran durumu tutulur ve
// servisler çağrılır.
//
// Güncel Android'e göre: kupa puanını istemci YAZMAZ; ders sonucu `submitCupResult` ile sunucuya gider.

/// Bir kupa yolunun ödül durumu (ortak kod). Kotlin iç sınıf adı yalnızca burada yazılır.
typealias KupaYolDurumu = CupPathRewardRepository.CupPathState

/// Kartın ödül satırı (ilerleme çubuğu + sandık) için ekran durumu.
struct KupaOdulDurumu: Equatable {
    /// "250 / 300", "Sandık hazır!" ya da "Tamamlandı".
    let etiket: String
    /// 0...1 (`CupPathState.fraction`).
    let oran: Double
    /// Sandık hak edildi (Android: applyDailyQuestionProgressOverlayNow `complete` = `claimable`): altın dolgu.
    let hazir: Bool
    /// Sıradaki eşiğin sandığı (kapalı çizim): 1000'in katı destansı, 500'ün katı ender, gerisi sıradan.
    let sandikResmi: String
}

enum KupaOdulHesabi {
    /// Ortak koddaki durumdan ekran durumu. `skor` verilirse (sayaç animasyonu) puan onunla değiştirilir.
    static func olustur(_ kaynak: KupaYolDurumu, skor: Int?) -> KupaOdulDurumu {
        var durum: KupaYolDurumu = kaynak
        if let s = skor {
            durum = KupaYolDurumu(
                cupField: kaynak.cupField,
                cupScore: Int32(max(0, s)),
                lastClaimed: kaynak.lastClaimed,
                claimedAbove: kaynak.claimedAbove
            )
        }
        let nadirlik: ChestRarity = CupPathRewardRepository.shared.rarityOf(milestone: durum.nextMilestone)
        return KupaOdulDurumu(
            etiket: etiket(durum),
            oran: Double(durum.fraction),
            hazir: durum.claimable,
            sandikResmi: nadirlik.drawableRes
        )
    }

    /// Android `CupPathState.label` (metinler strings.xml'den).
    static func etiket(_ durum: KupaYolDurumu) -> String {
        if durum.finished {
            return Metin.al("ui_tamamlandi_2")
        }
        if durum.claimable {
            return Metin.al("ui_sandik_hazir")
        }
        return String(durum.cupScore) + " / " + String(durum.nextMilestone)
    }
}

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
    /// Ödül satırı; nil = veri gelmedi ya da kartlar henüz açılmadı (gri yükleme hâli).
    var odul: KupaOdulDurumu? = nil

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
    /// Kilit durumları gelene kadar true: kartlar kapalı görünür, skorlar ve ödül satırları gizlidir.
    @Published private(set) var yukleniyor: Bool = true
    /// Sonsuz enerjide boştur ("∞" rozeti kalbin üstünde gösterilir; Android EnergyDisplay.apply).
    @Published private(set) var enerjiMetni: String = "0"
    @Published private(set) var sonsuzEnerji: Bool = false
    @Published private(set) var proPlan: Bool = false
    @Published var zorlukPaneli: KupaZorlukPaneliDurumu? = nil
    /// Kilitli karta dokununca çıkan uyarı (Android LockedHint); birkaç saniye sonra kendiliğinden gizlenir.
    @Published private(set) var kilitIpucu: String? = nil
    /// Kaydırıcı konumu (0..4). Panel kapanıp açılınca korunur (Android lastCupDifficultyProgress).
    @Published var zorluk: Double = 0

    private let ortam: KupaOrtami
    private let mantik: CupPathLogic = CupPathLogic.shared
    private var yuklendi: Bool = false
    private var sonKartTiklamasi: Date = Date.distantPast
    private var islenenSonuc: KupaDersSonucu? = nil
    /// Kupa dersinin sonucu işlenip sayaç başladığı an (Android: panelShownAtMs); rozet kutlaması
    /// sayaç animasyonu bitmeden açılmasın diye gecikme bundan hesaplanır.
    private var sonucIslemeZamani: Date = Date.distantPast
    /// Rozet kazanılınca kutlama kuyruğuyla çağrılır (Android: openCupBadgeCelebrationAfterAnimation →
    /// BadgeProgressFirestore.openBadgeCelebration). Dışarıdan (yönetici) verilir; nil ise kutlama yoktur.
    var rozetKutlamasi: (([String]) -> Void)? = nil
    /// Kart → ders başlarken okunan skor (Android GlobalValues.currentLessonOldCupScore).
    private var dersOncesiSkor: [Int: Int] = [:]
    /// Kart → ders başlarken seçilen zorluk (submitCupResult `difficultyLevel`).
    private var dersZorlugu: [Int: Int] = [:]
    private var sayacZamanlayicisi: Timer? = nil
    /// Sayaç animasyonu süren kart (ödül satırı o kart için animasyon bitene kadar elle sürülür).
    private var animasyonKartNo: Int? = nil
    /// Alan → ödül durumu (Android cupPathStatesCache).
    private var odulDurumlari: [String: KupaYolDurumu] = [:]
    /// Kartlar gerçek durumlarıyla çizildi mi (Android cupPathCardsRevealed): ödül satırı ancak
    /// ondan sonra çizilir, yoksa panelin yarısı canlı yarısı gri görünürdü.
    private var kartlarAcildi: Bool = false
    private var ipucuSayaci: Int = 0

    init(ortam: KupaOrtami) {
        self.ortam = ortam
        self.kartlar = mantik.CARDS.map { (k: CupPathCard) -> KupaKartDurumu in
            let baslik: String = Metin.al("panel_cup_path_card" + String(k.cardNumber) + "_title")
            return KupaKartDurumu(
                kartNo: Int(k.cardNumber),
                baslik: baslik,
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

    // MARK: - Metinler

    /// panel_cup_path.xml başlığı.
    var panelBasligi: String { Metin.al("panel_cup_path_text") }
    /// panel_cup_difficulty.xml "Seviyeyi Ayarla".
    var zorlukIletisi: String { Metin.al("panel_cup_difficulty_cup_difficulty_message") }
    /// panel_cup_difficulty.xml "Basamak Sayısı:".
    var basamakEtiketi: String { Metin.al("panel_cup_difficulty_text") }
    /// Android bu etiketi koda gömmüştür ("Rastgele"); ortak koddan gelir.
    var rastgeleEtiketi: String { mantik.DIGIT_RANDOM_LABEL }
    /// panel_cup_difficulty.xml BAŞLAT.
    var baslatEtiketi: String { Metin.al("panel_cup_difficulty_cup_difficulty_start_button") }
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

    /// Panel açılırken: kartlar kapalı gösterilir, skorlar, kilitler ve ödül defteri arka planda okunur.
    func yukle() {
        if yuklendi {
            enerjiyiYenile()
            return
        }
        yuklendi = true
        skorlariOku(atlanacakKart: nil)
        kilitleriOku()
        odulleriOku()
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
        // Kartlar açıldı: çubuk ve sandık da aynı anda açılsın (Android revealCupPathRewards).
        kartlarAcildi = true
        odulleriUygula()
    }

    /// Android startEnergyUpdateTimer: saniyede bir can göstergesini yeniler.
    func enerjiyiYenile() {
        let sonsuz: Bool = ortam.enerjiKaynagi.sonsuzEnerji
        let yeni: String = sonsuz ? "" : String(ortam.enerjiKaynagi.guncelEnerji)
        if yeni != enerjiMetni {
            enerjiMetni = yeni
        }
        if sonsuz != sonsuzEnerji {
            sonsuzEnerji = sonsuz
        }
        let pro: Bool = ortam.enerjiKaynagi.proPlan
        if pro != proPlan {
            proPlan = pro
        }
    }

    // MARK: - Ödül defteri (Kupa Yolu sandıkları)

    /// Android bindCupPathRewards: iki dokümanı (puanlar + defter) tek okumayla alır, durumu ortak kod kurar.
    private func odulleriOku() {
        ortam.yolBelgeDeposu.belgeleriGetir { [weak self] kupaBelgesi, defter in
            guard let self = self else { return }
            let durumlar = CupPathRewardRepository.shared.buildStates(cupDoc: kupaBelgesi, ledgerDoc: defter)
            self.odulDurumlari = durumlar
            self.odulleriUygula()
        }
    }

    /// Android bindCupPathRewardCard: yalnızca kartlar açıldıktan sonra çizer; sayaç animasyonu süren
    /// kartı atlar (animasyon bitince kendisi çizer).
    private func odulleriUygula() {
        if !kartlarAcildi { return }
        for i in kartlar.indices {
            if animasyonKartNo == kartlar[i].kartNo { continue }
            guard let durum = odulDurumlari[kartlar[i].alan] else { continue }
            kartlar[i].odul = KupaOdulHesabi.olustur(durum, skor: nil)
        }
    }

    /// Karttaki sandığa dokunulunca (hak edilmişse) açılacak sandık; değilse nil (kupa yolu ekranı açılır).
    /// Karttaki sandık her zaman SIRADAKİ eşiği açar; belirli eşik seçmek kupa yolu ekranının işidir.
    /// Hak ediş kontrolü yalnızca görünüm içindir; asıl doğrulamayı sunucu yapar (claimCupPathChest).
    func sandikGirdisi(kart: KupaKartDurumu) -> SandikGirdisi? {
        guard let durum = odulDurumlari[kart.alan] else { return nil }
        if !durum.claimable { return nil }
        let nadirlik: ChestRarity = CupPathRewardRepository.shared.rarityOf(milestone: durum.nextMilestone)
        return SandikGirdisi(
            baslangicNadirligi: nadirlik.name,
            reklamNonce: nil,
            kupaAlani: durum.cupField,
            kupaEsigi: Int(durum.nextMilestone)
        )
    }

    // MARK: - Kart ve zorluk paneli

    func kartaDokunuldu(_ kart: KupaKartDurumu) {
        // Kartlar yüklenirken dokunma kapalıdır (Android: isClickable = false).
        if yukleniyor { return }
        if !kart.acik {
            kilitIpucunuGoster(kart)
            return
        }
        let simdi = Date()
        let aralik: TimeInterval = Double(mantik.CARD_CLICK_DEBOUNCE_MS) / 1000.0
        if simdi.timeIntervalSince(sonKartTiklamasi) < aralik { return }
        sonKartTiklamasi = simdi

        zorlukPaneli = KupaZorlukPaneliDurumu(
            kart: kart,
            baslik: kart.carpma ? Metin.al("ui_carpma_kupa_yolu") : Metin.al("panel_cup_difficulty_cup_difficulty_title"),
            kaydiriciGorunur: mantik.showsDifficultySlider(isMultiplication: kart.carpma)
        )
        // BAŞLAT, kupa skoru okunduktan sonra açılır.
        let kartNo: Int = kart.kartNo
        ortam.skorDeposu.kupaSkorunuGetir(alan: kart.alan) { [weak self] skor in
            self?.panelSkoruGeldi(kartNo: kartNo, skor: skor)
        }
    }

    /// Android showCupCardLockedToastIfLocked: "Açmak için \"X\" bölümünün son ünite maratonunu bitir."
    private func kilitIpucunuGoster(_ kart: KupaKartDurumu) {
        let bolumlar: [String] = [
            "ui_abakusun_temeli_ve_toplama",
            "ui_abakuste_cikarma",
            "ui_abakuste_carpma",
            "ui_korleme_toplama",
            "ui_korleme_cikarma",
            "ui_korleme_carpma"
        ]
        let sira: Int = max(0, min(bolumlar.count - 1, kart.kartNo - 1))
        let bolum: String = Metin.al(bolumlar[sira])
        kilitIpucu = Metin.al("ui_x_bolumunun_son_unite_maratonunu_bitir", bolum)
        ipucuSayaci += 1
        let sayac: Int = ipucuSayaci
        // Snackbar LENGTH_LONG ≈ 2,75 sn.
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.75) { [weak self] in
            guard let self = self, self.ipucuSayaci == sayac else { return }
            self.kilitIpucu = nil
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
        dersZorlugu[panel.kart.kartNo] = Int(d.cupDifficultyLevel)
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

    /// Dersten dönüldüğünde ilgili kartın skorunu eski değerden yenisine sayar, diğerlerini yeniden okur;
    /// sonucu sunucuya bildirir (Android: BadgePrecalcHelper.executeCupDeltaUpdateAsync → submitCupResult).
    func sonucuIsle(_ sonuc: KupaDersSonucu?) {
        guard let sonuc = sonuc, sonuc != islenenSonuc else { return }
        islenenSonuc = sonuc
        sonucIslemeZamani = Date()
        enerjiyiYenile()
        kilitleriOku()
        skorlariOku(atlanacakKart: sonuc.kartNo)

        guard let i = kartlar.firstIndex(where: { $0.kartNo == sonuc.kartNo }) else { return }
        let eski: Int = dersOncesiSkor[sonuc.kartNo] ?? kartlar[i].skor ?? 0
        let yeni: Int32 = mantik.scoreAfterLesson(oldScore: Int32(eski), delta: Int32(sonuc.delta))
        let bitis: Int = Int(mantik.scoreAnimationEnd(updatedScore: yeni))
        kartlar[i].skor = bitis
        dersOncesiSkor[sonuc.kartNo] = nil
        let zorlukSeviyesi: Int = dersZorlugu[sonuc.kartNo] ?? 0
        dersZorlugu[sonuc.kartNo] = nil

        // Sayaç hemen başlar (sunucu yanıtı beklenmez); sunucu farklı bir puan döndürürse düzeltilir.
        if !mantik.shouldAnimateScore(delta: Int32(sonuc.delta)) {
            kartlar[i].gosterilenSkor = bitis
        } else {
            let baslangic: Int = Int(mantik.scoreAnimationStart(updatedScore: yeni, delta: Int32(sonuc.delta)))
            kartlar[i].gosterilenSkor = baslangic
            kartlar[i].deltaMetni = mantik.deltaText(delta: Int32(sonuc.delta))
            kartlar[i].deltaRengi = UInt32(mantik.deltaColorRgb(delta: Int32(sonuc.delta)))
            skoruSay(kartNo: sonuc.kartNo, baslangic: baslangic, bitis: bitis)
        }
        sunucuyaBildir(kart: kartlar[i], delta: sonuc.delta, zorluk: zorlukSeviyesi)
    }

    /// `submitCupResult`: yalnızca kazanma, zorluk ve mod gönderilir; miktarı sunucu hesaplar.
    private func sunucuyaBildir(kart: KupaKartDurumu, delta: Int, zorluk: Int) {
        let yuk: [String: Any] = CupScoreService.shared.buildSubmitPayload(
            cupField: kart.alan,
            won: delta > 0,
            difficultyLevel: Int32(zorluk),
            isMultiplication: kart.carpma,
            dayId: KupaModeli.bugunKimligi()
        )
        let kartNo: Int = kart.kartNo
        ortam.skorDeposu.sonucuSunucuyaGonder(yuk: yuk, yerelDelta: delta) { [weak self] yanit in
            self?.sunucuYaniti(kartNo: kartNo, yanit: yanit)
        }
    }

    private func sunucuYaniti(kartNo: Int, yanit: KupaSunucuYaniti?) {
        // Yanıt ayrıştırılamadıysa arayüz yerel değeri göstermeye devam eder (Android).
        guard let yanit = yanit else { return }
        guard let i = kartlar.firstIndex(where: { $0.kartNo == kartNo }) else { return }
        kartlar[i].skor = yanit.yeniSkor
        if animasyonKartNo != kartNo {
            kartlar[i].gosterilenSkor = Int(mantik.shownScore(score: Int32(yanit.yeniSkor)))
        }
        // Defter (hangi eşik alındı) ve puan sunucudan yeniden okunur.
        odulleriOku()
        rozetleriSenkronla(kart: kartlar[i], yeniSkor: yanit.yeniSkor)
    }

    /// Android: BadgePrecalcHelper.executeCupDeltaUpdateAsync → sunucudan dönen yeni puanla ilgili hayvan rozetinin
    /// senkronu (`sync{Dino,...}ProgressAndDetectLevelUp`) → `awaitCupBadgePayloads` → kutlama. Hangi hayvan rozeti
    /// olduğunu `BadgePrecalcHelper.resolveCupPathKind` (CupPathKind.badgeMode) söyler; kademe hesabı ortak koddadır.
    /// Kutlama, sayaç animasyonu (1 sn) + 350 ms boşluk dolduktan sonra açılır (Android CUP_SCORE_ANIM_MS +
    /// CUP_CELEBRATION_GAP_MS); panel bu arada kapandıysa (model yok olduysa) kutlama oynamaz, rozet yine kazanılmış olur.
    private func rozetleriSenkronla(kart: KupaKartDurumu, yeniSkor: Int) {
        let tur: CupPathKind = BadgePrecalcHelper.shared.resolveCupPathKind(
            isMultiplication: KotlinBoolean(bool: kart.carpma),
            isExtraction: KotlinBoolean(bool: kart.cikarma),
            isBlinding: KotlinBoolean(bool: kart.korleme)
        )
        let islemZamani: Date = sonucIslemeZamani
        SahteRozetKazanimServisi.ortak.kupaRozetiniSenkronla(mod: tur.badgeMode, yeniSkor: yeniSkor) { [weak self] kuyruk in
            if kuyruk.isEmpty {
                return
            }
            let animasyonSonu: TimeInterval = Double(CupPathLogic.shared.SCORE_ANIMATION_MS) / 1000.0 + 0.35
            let gecen: TimeInterval = Date().timeIntervalSince(islemZamani)
            let bekleme: TimeInterval = max(0, animasyonSonu - gecen)
            DispatchQueue.main.asyncAfter(deadline: .now() + bekleme) { [weak self] in
                guard let self = self else { return }
                self.rozetKutlamasi?(kuyruk)
            }
        }
    }

    /// Yerel saat diliminde "yyyy-MM-dd" (ortak: CupHistoryRepository.todayId; Android CupScoreService.buildSubmitPayload
    /// varsayılanı, geçmiş grafiği günü).
    private static func bugunKimligi() -> String {
        let dilim: Kotlinx_datetimeTimeZone = Kotlinx_datetimeTimeZone.companion.currentSystemDefault()
        return CupHistoryRepository.shared.todayId(nowMs: KupaZamanKt.kupaNowMs(), timeZone: dilim)
    }

    /// Android animateCupScore: 1 saniyede eski skordan yenisine sayar, bitince delta metnini gizler.
    /// Ödül çubuğu da aynı anda ve aynı sürede eski değerden yenisine akar (bindCupPathRewardCard `from`).
    private func skoruSay(kartNo: Int, baslangic: Int, bitis: Int) {
        sayacZamanlayicisi?.invalidate()
        animasyonKartNo = kartNo
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
            let araSkor: Int = Int(ara.rounded())
            self.kartlar[i].gosterilenSkor = araSkor
            if let durum = self.odulDurumlari[self.kartlar[i].alan] {
                self.kartlar[i].odul = KupaOdulHesabi.olustur(durum, skor: araSkor)
            }
            if oran >= 1.0 {
                // Sunucu farklı bir puan döndürdüyse o esas alınır (skor alanı güncellenmiş olur).
                let son: Int = self.kartlar[i].skor ?? bitis
                self.kartlar[i].gosterilenSkor = Int(self.mantik.shownScore(score: Int32(son)))
                self.kartlar[i].deltaMetni = nil
                self.animasyonKartNo = nil
                self.sayacZamanlayicisi = nil
                zamanlayici.invalidate()
                self.odulleriUygula()
            }
        }
    }
}

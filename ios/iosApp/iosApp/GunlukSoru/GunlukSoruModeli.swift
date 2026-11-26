import Foundation
import Shared
import SwiftUI

// MARK: - Dışarı verilen istekler

/// Keşfet sekmesindeki, günlük soru dışındaki kartlar (TasksFragment.submitBulletinList).
enum GunlukSoruKesfetKarti: Int, CaseIterable, Identifiable {
    /// "Abaküs" — Android: AbacusPracticeFragment.
    case abakusPratik
    /// "Kupa Yolu" — Android: TasksFragment.showCupPathPanel.
    case kupaYolu
    /// "Bize Ulaşın" — Android: FeedbackFragment.
    case bizeUlasin

    var id: Int { rawValue }
}

/// Günlük soru ödülünü açma isteği (3/3 tamamlanıp ilerleme çubuğuna dokunulunca).
/// Android: TasksFragment.startDailyQuestionRewardFlow → NewChestFragment.newInstance(RARE),
/// sandık kapanınca ("chest_closed") varsa rozet seviye atlama ekranı.
struct GunlukSoruOdulIstegi: Identifiable {
    let id = UUID()
    let periodKey: String
    /// Sandığın başlangıç nadirliği ("RARE").
    let nadirlik: String
    /// Balıkçılık rozetinde seviye atlandıysa gösterilecek rozet kuyruğu (çoğunlukla boş).
    let rozetKuyrugu: [String]
    /// Ödül ekranı kapanınca çağrılır.
    let kapandi: () -> Void
}

/// "Mücadeleye devam et" panelinin isteği.
struct GunlukSoruDevamIstegi: Identifiable {
    let id = UUID()
    let meydanOkuma: DailyQuestionChallenge
}

/// Kısa süreli bilgi balonu (Android: Toast).
struct GunlukSoruBildirimi: Identifiable, Equatable {
    let id: Int
    let metin: String
}

/// Kartın sağ üstündeki kalbin durumu (Android: broken_heart_anim.json Lottie kareleri).
enum GunlukSoruKalpDurumu: Equatable {
    /// İlk kare: sağlam kalp.
    case saglam
    /// 0 → 116: kırılıyor.
    case kiriliyor
    /// 116. kare: kırık kalp.
    case kirik
    /// 116 → son kare → 0: iyileşiyor.
    case iyilesiyor
}

/// Ekranın dışarıya verdiği geçişler; nil olanlar için ekranın kendi yer tutucusu çalışır.
struct GunlukSoruCikislari {
    var soruBaslat: ((GunlukSoruBaslangici) -> Void)? = nil
    var odulAc: ((GunlukSoruOdulIstegi) -> Void)? = nil
    var kartSecildi: ((GunlukSoruKesfetKarti) -> Void)? = nil
    var rozetGoster: (([String]) -> Void)? = nil
}

// MARK: - Görünüm modeli

/// Keşfet sekmesinin görünüm modeli. Android karşılığı: TasksFragment'ın günlük soru kısmı
/// (BulletinAdapter.DailyQuestionVH + handleDailyQuestionCardClick + ödül ve devam akışları).
final class GunlukSoruModeli: ObservableObject {
    @Published private(set) var kart: GunlukSoruKartGorunumu
    @Published private(set) var cubuk: GunlukSoruCubukCizimi = GunlukSoruCubukCizimi.bos
    @Published private(set) var geriSayim: String = ""
    @Published private(set) var kalp: GunlukSoruKalpDurumu = .saglam
    /// İlerleme bölgesi tıklanabilir mi (ödül alınmadıysa evet).
    @Published private(set) var ilerlemeTiklanir: Bool = false
    /// Android: launch touch blocker — bir geçiş sürerken ekran dokunuşları yutar.
    @Published private(set) var dokunmaKilitli: Bool = false
    @Published private(set) var bildirim: GunlukSoruBildirimi? = nil
    @Published var devamPaneli: GunlukSoruDevamIstegi? = nil
    /// Dışarıya bağlanmamış geçişlerin yer tutucuları.
    @Published var yerTutucuSoru: GunlukSoruBaslangici? = nil
    @Published var yerTutucuOdul: GunlukSoruOdulIstegi? = nil

    var cikislar = GunlukSoruCikislari()

    private let depo: GunlukSoruDeposu
    private let cuzdan: GunlukSoruCuzdani
    private let odul: GunlukSoruOdulServisi

    /// Ortak koddaki kart durumu (Android: dailyCardState / boundState).
    private var hamKart: DailyQuestionCardUiState
    /// Kart en az bir kez bağlandı mı (aynı durum yeniden gelirse bağlama atlanır — DiffUtil gibi).
    private var bagli = false
    private var havuzVar = false
    private var gorselYuzde: Float = 0

    private var animasyonZamanlayici: Timer? = nil
    private var animasyonda = false
    private var sayacNesli = 0
    private var kalpNesli = 0
    private var bildirimNo = 0
    private var bagliPeriyot = ""
    private var sonDevirYenilemesiMs: Int64 = 0
    /// Aynı periyotta "Mücadeleyi tamamladın!" yalnızca bir kez gösterilir.
    private var tamamlandiBildirilenPeriyot: String? = nil
    /// Kalp iyileşme animasyonu bitince başlatılacak soru (anahtarla devam).
    private var iyilesmeSonrasi: (() -> Void)? = nil

    init(depo: GunlukSoruDeposu, cuzdan: GunlukSoruCuzdani, odul: GunlukSoruOdulServisi) {
        self.depo = depo
        self.cuzdan = cuzdan
        self.odul = odul
        let ilk: DailyQuestionCardUiState = GunlukSoruMantik.varsayilanKart()
        hamKart = ilk
        kart = GunlukSoruMantik.gorunum(ilk)
    }

    deinit {
        animasyonZamanlayici?.invalidate()
    }

    // MARK: Yaşam döngüsü

    /// Android: onViewCreated / onResume / onHiddenChanged(false).
    func gorundu() {
        dokunmaKilitli = false
        if !bagli {
            bagla(hamKart)
        }
        kartiYenile()
    }

    /// Android: refreshDailyQuestionCard.
    func kartiYenile() {
        depo.kartiYukle { [weak self] yeni in
            guard let ben = self else { return }
            ben.havuzVar = yeni.poolAvailable
            ben.kartiAyarla(yeni)
        }
    }

    /// Android: submitBulletinList — içerik değişmediyse satır yeniden bağlanmaz.
    private func kartiAyarla(_ yeni: DailyQuestionCardUiState) {
        if bagli && yeni.isEqual(hamKart) {
            return
        }
        bagla(yeni)
    }

    // MARK: Kartı bağlama (DailyQuestionVH.bind)

    private func bagla(_ yeni: DailyQuestionCardUiState) {
        hepsiniDurdur()
        bagli = true
        hamKart = yeni
        kart = GunlukSoruMantik.gorunum(yeni)
        ilerlemeTiklanir = false
        sayaciBaslat(periyot: yeni.periodKey)
        kalbiBagla(yeni)

        let plan: GunlukSoruIlerlemePlani = GunlukSoruMantik.ilerlemePlani(yeni, anim: depo.animDeposu)
        yuzdeUygula(plan.baslangicYuzdesi)
        if !plan.animasyon {
            yuzdeUygula(plan.bitisYuzdesi)
            GunlukSoruMantik.gosterileniKaydet(hamKart, anim: depo.animDeposu)
            return
        }
        animasyonuBaslat(baslangic: plan.baslangicYuzdesi, bitis: plan.bitisYuzdesi)
    }

    /// DailyQuestionVH.cancelAnim.
    private func hepsiniDurdur() {
        sayacNesli += 1
        kalpNesli += 1
        animasyonuDurdur()
    }

    /// DailyQuestionVH.applyVisualPercent.
    private func yuzdeUygula(_ yuzde: Float) {
        gorselYuzde = yuzde
        cubuk = GunlukSoruMantik.cubuk(hamKart, yuzde: yuzde)
        ilerlemeTiklanir = GunlukSoruMantik.ilerlemeTiklanir(hamKart)
    }

    private func animasyonuBaslat(baslangic: Float, bitis: Float) {
        animasyonuDurdur()
        animasyonda = true
        let sure: Double = max(GunlukSoruMantik.animasyonSuresi, 0.01)
        let baslamaAni = Date()
        let zamanlayici = Timer(timeInterval: 1.0 / 60.0, repeats: true) { [weak self] _ in
            guard let ben = self else { return }
            let gecen: Double = Date().timeIntervalSince(baslamaAni)
            let t: Float = Float(min(gecen / sure, 1.0))
            ben.yuzdeUygula(GunlukSoruMantik.animasyonYuzdesi(baslangic: baslangic, bitis: bitis, t: t))
            if t >= 1.0 {
                ben.animasyonuDurdur()
            }
        }
        animasyonZamanlayici = zamanlayici
        RunLoop.main.add(zamanlayici, forMode: .common)
    }

    /// Android: onAnimationEnd / onAnimationCancel — ikisinde de gösterilen değer kaydedilir.
    private func animasyonuDurdur() {
        animasyonZamanlayici?.invalidate()
        animasyonZamanlayici = nil
        if animasyonda {
            animasyonda = false
            GunlukSoruMantik.gosterileniKaydet(hamKart, anim: depo.animDeposu)
            ilerlemeTiklanir = GunlukSoruMantik.ilerlemeTiklanir(hamKart)
        }
    }

    // MARK: Geri sayım (DailyQuestionVH.startCountdownTicker)

    private func sayaciBaslat(periyot: String) {
        sayacNesli += 1
        bagliPeriyot = periyot
        sayacTiki(nesil: sayacNesli)
    }

    private func sayacTiki(nesil: Int) {
        if nesil != sayacNesli {
            return
        }
        let guncel: String = GunlukSoruMantik.guncelPeriyot()
        if guncel != bagliPeriyot {
            periyotDevretti()
            return
        }
        let kalan: Int64 = GunlukSoruMantik.kalanMs()
        geriSayim = Metin.al("daily_question_refresh_countdown", GunlukSoruMantik.geriSayimMetni(kalanMs: kalan))
        let gecikme: Double = GunlukSoruMantik.sayacGecikmesi(kalanMs: kalan)
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme) { [weak self] in
            self?.sayacTiki(nesil: nesil)
        }
    }

    /// Android: onDailyQuestionPeriodRolledOver — periyot bitince kart ekrandayken yenilenir.
    private func periyotDevretti() {
        tamamlandiBildirilenPeriyot = nil
        let kartPeriyodu: String = hamKart.periodKey
        if !kartPeriyodu.isEmpty {
            depo.kalpDeposu.resetForPeriod(periodKey: kartPeriyodu)
        }
        let simdi: Int64 = GunlukSoruMantik.simdiMs()
        let yenile: Bool = GunlukSoruMantik.devirdeYenile(
            kartPeriyodu: kartPeriyodu,
            guncel: GunlukSoruMantik.guncelPeriyot(),
            simdi: simdi,
            sonYenileme: sonDevirYenilemesiMs
        )
        if !yenile {
            return
        }
        sonDevirYenilemesiMs = simdi
        kartiYenile()
    }

    // MARK: Kırık kalp (DailyQuestionVH.bindBrokenHeart)

    /// Lottie yerine geçen kısa animasyonun süresi (sn).
    private static let kalpAnimasyonSuresi: Double = 1.2

    private func kalbiBagla(_ durum: DailyQuestionCardUiState) {
        kalpNesli += 1
        let nesil: Int = kalpNesli
        let periyot: String = durum.periodKey
        switch GunlukSoruMantik.kalpEylemi(durum, kalp: depo.kalpDeposu) {
        case .ilkKare:
            kalp = .saglam
        case .kirikDur:
            kalp = .kirik
        case .kiril:
            // Android: playBrokenHeartBreakToHold116 — bitince 116. karede kalır. Android "kırık"
            // bayrağını animasyon bitince yazar; burada başta yazılır ki animasyon yarıda kesilirse
            // (sekme değişimi, kartın yeniden bağlanması) kalp sağlam görünmesin.
            kalp = .kiriliyor
            depo.kalpDeposu.setBrokenHold116(periodKey: periyot, hold: true)
            DispatchQueue.main.asyncAfter(deadline: .now() + GunlukSoruModeli.kalpAnimasyonSuresi) { [weak self] in
                guard let ben = self, ben.kalpNesli == nesil else { return }
                ben.kalp = .kirik
            }
        case .iyiles:
            // Android: playBrokenHeartHealThenFirstFrame — bitince bekleyen soru başlar.
            kalp = .iyilesiyor
            DispatchQueue.main.asyncAfter(deadline: .now() + GunlukSoruModeli.kalpAnimasyonSuresi) { [weak self] in
                guard let ben = self, ben.kalpNesli == nesil else { return }
                ben.depo.kalpDeposu.clearBrokenHold116(periodKey: periyot)
                ben.kalp = .saglam
                let bekleyen = ben.iyilesmeSonrasi
                ben.iyilesmeSonrasi = nil
                bekleyen?()
            }
        }
    }

    // MARK: Bildirimler (Toast)

    private func bildir(_ metin: String) {
        bildirimNo += 1
        let no: Int = bildirimNo
        bildirim = GunlukSoruBildirimi(id: no, metin: metin)
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            guard let ben = self, ben.bildirim?.id == no else { return }
            ben.bildirim = nil
        }
    }

    /// Android: showDailyQuestionChallengeCompleteToast.
    private func tamamlandiBildir(periyot: String) {
        if periyot.isEmpty || tamamlandiBildirilenPeriyot == periyot {
            return
        }
        tamamlandiBildirilenPeriyot = periyot
        bildir(Metin.al("daily_question_challenge_complete"))
    }

    /// Android: showDailyQuestionPoolEmptyMessage.
    private func havuzBosBildir() {
        bildir(Metin.al("daily_question_pool_empty"))
        dokunmaKilitli = false
    }

    // MARK: Karta dokunma (handleDailyQuestionCardClick)

    func kartaDokunuldu() {
        if dokunmaKilitli {
            return
        }
        dokunmaKilitli = true
        switch GunlukSoruMantik.kartTiklama(hamKart) {
        case .tamamlandiBildirimi:
            tamamlandiBildir(periyot: hamKart.periodKey)
            dokunmaKilitli = false
        case .yokSay:
            dokunmaKilitli = false
        case .yukle:
            depo.meydanOkumayiYukle { [weak self] sonuc in
                self?.meydanOkumaGeldi(sonuc)
            }
        }
    }

    private func meydanOkumaGeldi(_ sonuc: GunlukSoruYukleme) {
        switch sonuc {
        case .havuzBos:
            havuzBosBildir()
        case .hazir(let meydanOkuma):
            switch GunlukSoruMantik.meydanOkumaTiklama(meydanOkuma) {
            case .yokSay:
                dokunmaKilitli = false
            case .tamamlandiBildirimi:
                tamamlandiBildir(periyot: meydanOkuma.periodKey)
                dokunmaKilitli = false
            case .devamPaneli:
                devamPaneli = GunlukSoruDevamIstegi(meydanOkuma: meydanOkuma)
                dokunmaKilitli = false
            case .dersiBaslat:
                let veri: GunlukSoruBaslangici? = GunlukSoruMantik.baslangic(meydanOkuma, sonuc: sonucKapanisi())
                if let hazir = veri {
                    soruyuBaslat(hazir)
                } else {
                    dokunmaKilitli = false
                }
            }
        }
    }

    /// Android: launchDailyQuestionLesson → BlindingLessonFragment.newDailyQuestionInstance.
    private func soruyuBaslat(_ veri: GunlukSoruBaslangici) {
        dokunmaKilitli = false
        if let baslat = cikislar.soruBaslat {
            baslat(veri)
        } else {
            yerTutucuSoru = veri
        }
    }

    private func sonucKapanisi() -> (String, Int, Bool) -> Void {
        return { [weak self] periyot, slot, dogru in
            self?.soruBitti(periyot: periyot, slot: slot, dogru: dogru)
        }
    }

    /// Soru ekranı kapandı: sonuç yazılır ve kart yenilenir (Android: ders ekranı yazar,
    /// TasksFragment.onResume kartı yeniler).
    private func soruBitti(periyot: String, slot: Int, dogru: Bool) {
        yerTutucuSoru = nil
        depo.soruSonucunuYaz(periodKey: periyot, slotIndex: slot, dogru: dogru) { [weak self] in
            self?.kartiYenile()
        }
    }

    // MARK: Anahtarla devam (showDailyQuestionContinuePanel)

    func devamPaneliniKapat() {
        devamPaneli = nil
    }

    func anahtarlaDevam() {
        guard let istek = devamPaneli else {
            return
        }
        let meydanOkuma: DailyQuestionChallenge = istek.meydanOkuma
        if !cuzdan.anahtarHarca(GunlukSoruMantik.devamBedeli) {
            bildir(Metin.al("daily_question_insufficient_keys"))
            return
        }
        guard let veri = GunlukSoruMantik.devamBaslangici(meydanOkuma, sonuc: sonucKapanisi()) else {
            return
        }
        let periyot: String = meydanOkuma.periodKey
        devamPaneli = nil
        dokunmaKilitli = true
        depo.devamBeklemesiniKaldir(periodKey: periyot) { [weak self] in
            guard let ben = self else { return }
            // Kalp iyileşme animasyonu bitince soru başlar.
            ben.iyilesmeSonrasi = { [weak ben] in
                ben?.soruyuBaslat(veri)
            }
            ben.depo.kalpDeposu.requestHealPlay(periodKey: periyot)
            ben.kartiAyarla(GunlukSoruMantik.devamSonrasiKart(meydanOkuma, havuzVar: ben.havuzVar))
        }
    }

    // MARK: Ödül (ilerleme bölgesine dokunma)

    /// Android: progressZone.setOnClickListener.
    func ilerlemeyeDokunuldu() {
        if dokunmaKilitli || hamKart.rewardClaimed {
            return
        }
        let hazir: Bool = GunlukSoruMantik.odulAlmayaHazir(hamKart, gorselYuzde: gorselYuzde, animasyonda: animasyonda)
        if !hazir {
            // Android: showDailyQuestionClaimRequiresCompleteToast.
            bildir(Metin.al("daily_question_claim_requires_complete"))
            return
        }
        // Android: performProgressClaim + onDailyQuestionProgressClaimTapped.
        let periyot: String = hamKart.periodKey
        tamamlandiBildirilenPeriyot = periyot
        dokunmaKilitli = true
        kartiAyarla(GunlukSoruMantik.odulAlindiKart(hamKart))
        yuzdeUygula(100)
        odulAkisiniBaslat(periyot: periyot)
    }

    /// Android: startDailyQuestionRewardFlow.
    private func odulAkisiniBaslat(periyot: String) {
        odul.rozetIlerlet(periodKey: periyot) { [weak self] kuyruk in
            guard let ben = self else { return }
            ben.depo.odulAlindiIsaretle(periodKey: periyot)
            let nadirlik: String = GunlukSoruMantik.odulSandigiNadirligi
            ben.odul.sandikOnIstegi(nadirlik: nadirlik)
            let istek = GunlukSoruOdulIstegi(
                periodKey: periyot,
                nadirlik: nadirlik,
                rozetKuyrugu: kuyruk,
                kapandi: { [weak ben] in
                    ben?.odulKapandi(kuyruk: kuyruk)
                }
            )
            if let ac = ben.cikislar.odulAc {
                ac(istek)
            } else {
                ben.yerTutucuOdul = istek
            }
        }
    }

    /// Android: "chest_closed" sonucu — kilit kalkar, rozet kuyruğu varsa rozet ekranı açılır.
    private func odulKapandi(kuyruk: [String]) {
        yerTutucuOdul = nil
        dokunmaKilitli = false
        if !kuyruk.isEmpty {
            cikislar.rozetGoster?(kuyruk)
        }
        kartiYenile()
    }

    // MARK: Diğer kartlar

    /// Android: BulletinAdapter onClick (Abaküs / Kupa Yolu / Bize Ulaşın).
    func kesfetKartinaDokunuldu(_ secilen: GunlukSoruKesfetKarti) {
        if dokunmaKilitli {
            return
        }
        if let sec = cikislar.kartSecildi {
            sec(secilen)
        } else {
            bildir("Yakında")
        }
    }
}

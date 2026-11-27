import Foundation
import Shared
import SwiftUI

// Kör ders ekranının durumu.
//
// Android: BlindingLessonFragment. Kurallar ortak koddadır (KorDersOturumu →
// BlindingLessonSession / BlindingSequencePlayback / BlindingLessonRules / AbacusLessonRules);
// burada yalnızca zamanlayıcılar, animasyon tetikleri ve servis çağrıları vardır.

/// Alttan çıkan sonuç paneli.
enum KorDersPanel: Equatable {
    case dogru
    case yanlis
}

final class KorDersModeli: ObservableObject {
    // MARK: Görünümün okuduğu durum

    /// Android: lessonStarted ("Başla"ya basıldı).
    @Published private(set) var basladi: Bool = false
    /// Android: numberText (firstNumberText) — dizinin o an gösterilen sayısı.
    @Published private(set) var tahtaMetni: String = ""
    /// Android: firstNumberText2 / operator / secondNumberText (çarpım gösterimi).
    @Published private(set) var birinciSayi: String = ""
    @Published private(set) var islec: String = ""
    @Published private(set) var ikinciSayi: String = ""
    /// Android: controlButton.isEnabled (dizi gösterilirken kapalı).
    @Published private(set) var kontrolEtkin: Bool = true
    /// Android: numberInput metni.
    @Published var girdi: String = ""
    /// Android: timerTextView.
    @Published private(set) var sureMetni: String = "0:00"
    /// Gösterilen sonuç paneli; nil = yok.
    @Published private(set) var panel: KorDersPanel? = nil
    /// Android: correctAnswerLabel / correctAnswerText.
    @Published private(set) var dogruCevapEtiketi: String = ""
    @Published private(set) var seninCevabinMetni: String = ""
    /// Android: progressBarFill ağırlığı (0...1) ve rengi.
    @Published private(set) var ilerleme: CGFloat = 0
    @Published private(set) var ilerlemeRengi: Color = KorDersStil.ilerlemeYesil
    /// Android: isHintVisible.
    @Published private(set) var ipucuAcik: Bool = false
    /// Android: lottieView (animaton_thirteen) — son soruda bütün cevaplar doğruysa bir kez oynar.
    @Published private(set) var konfeti: Bool = false
    /// Yanlış panelinde "Sobi ile analiz et" düğmesi görünür mü (Android: bindReviewButton).
    @Published private(set) var analizGorunur: Bool = false

    /// Android: abacusContainer + AbacusBeadController (cevabı abaküsle verilen derslerde görünür).
    let abakus = AbakusDenetleyici()

    // MARK: Sabit görünüm bayrakları (ortak koddan)

    let klavyeyleCevap: Bool
    let carpmaGosterimi: Bool
    let sureGorunur: Bool
    let ilerlemeGizli: Bool
    let ipucuGizli: Bool
    let adimAtlaGizli: Bool
    let sifirlaGizli: Bool
    /// Android: abacusModeVisibility — ayar paneli düğmesi yalnızca bölüm 7, 8, 9'da ve cevabı abaküsle
    /// verilen derste görünür.
    let ayarGorunur: Bool
    /// Android: splitTextEqually(lessonHint).
    let ipucuMetni: String

    // MARK: İç durum

    private let baslangic: KorDersBaslangici
    private let oturum: KorDersOturumu
    private let kayit: KorDersKayitServisi
    private let enerji: KorDersEnerjiServisi
    private let ses: KorDersSesServisi
    private let sobiAnaliz: AbakusSobiAnalizBaglantisi?
    private let bitti: (KorDersSonucu) -> Void
    private let kapat: () -> Void

    /// Android: flashLog / flashMarks — flaş dizisinde her sayı göründüğü anda abaküsün değeri (abaküslü
    /// derste) ve o değerlerin kayıttaki sırası. Dizi her başladığında (yeniden başlatma dahil) sıfırlanır.
    private var flashKaydi: [Int] = []
    private var flashIsaretleri: [Int] = []
    private static let flashKaydiEnCok: Int = 400
    /// Bu soruda kartta gösterilen sayılar, sırayla. Android analizi `operations[currentIndex]`'ten
    /// alır; ortak koddaki soru listesi Swift'e açılmadığından gösterim sırasında toplanır (dizi
    /// bitmeden kontrol açılmadığı için cevap anında tam dizi bellidir).
    private var gosterilenSayilar: [Int] = []
    /// Android: lastWrongAnswer — son yanlış cevap; kontrolden sonra girdi sıfırlandığı için saklanır.
    private var sonCevap: Int = 0
    private var analizSayilari: [Int] = []
    private var analizKaydi: [Int] = []
    private var analizIsaretleri: [Int] = []
    private var analizAcik: Bool = false
    /// Android: questionSessionStartMs / hasSubmittedAnyAnswer (başarı oranı ölçümü).
    private let girisAni: Date = Date()
    private var cevapVerildi: Bool = false
    private var terkKaydedildi: Bool = false
    private var girisKaydedildi: Bool = false

    /// Android: showNextNumberRunnable / sequenceRevealRunnable / restartSequenceRunnable.
    private var diziIsi: DispatchWorkItem?
    /// Android: süre Runnable'ı (saniyede bir).
    private var sureSayaci: Timer?
    private var saniye: Int32 = 0
    private var sureBasladi: Bool = false
    /// Android: isResultPanelAnimating.
    private var panelAnimasyonda: Bool = false
    /// Android: sonuç paneli açıkken controlButton dinleyicisi kaldırılır; sonra gecikmeyle geri takılır.
    private var kontrolKilitli: Bool = false
    /// Android: lastClickTime.
    private var sonKontrolAni: Date = .distantPast
    private var sonKupaDeltasi: Int? = nil
    private var harcananEnerji: Int = 0
    private var sonlandi: Bool = false
    /// Android: learningSessionStartMs.
    private var ogrenmeBaslangici: Date? = nil

    init(
        baslangic: KorDersBaslangici,
        kayit: KorDersKayitServisi,
        enerji: KorDersEnerjiServisi,
        ses: KorDersSesServisi,
        sobiAnaliz: AbakusSobiAnalizBaglantisi?,
        bitti: @escaping (KorDersSonucu) -> Void,
        kapat: @escaping () -> Void
    ) {
        self.baslangic = baslangic
        self.kayit = kayit
        self.enerji = enerji
        self.ses = ses
        self.sobiAnaliz = sobiAnaliz
        self.bitti = bitti
        self.kapat = kapat

        let oturum: KorDersOturumu = KorDersModeli.oturumKur(baslangic)
        self.oturum = oturum
        self.klavyeyleCevap = oturum.klavyeyleCevap
        self.carpmaGosterimi = oturum.carpmaGosterimi
        self.sureGorunur = oturum.sureGorunur
        self.ilerlemeGizli = oturum.ilerlemeGizli
        self.adimAtlaGizli = oturum.adimAtlaGizli
        self.sifirlaGizli = oturum.sifirlaGizli
        let ayarPartlari: [Int] = [7, 8, 9]
        self.ayarGorunur = ayarPartlari.contains(baslangic.partId) && !oturum.klavyeyleCevap

        let hamIpucu: String = baslangic.ipucu ?? ""
        self.ipucuGizli = oturum.ipucuGizli || hamIpucu.isEmpty
        self.ipucuMetni = hamIpucu.isEmpty ? "" : oturum.ipucuMetni(ipucu: hamIpucu)
    }

    deinit {
        diziIsi?.cancel()
        sureSayaci?.invalidate()
    }

    /// Android: setupAbacusController — BEAD_ANIMATION_MS, her ders türünde 300 ms (ortak koddan).
    var boncukAnimasyonMs: Int {
        return Int(oturum.boncukAnimasyonMs)
    }

    /// Android: skipStepButton yalnızca dizi gösterilirken (isShowingSequence) çalışır ve oynar.
    var adimAtlanabilir: Bool {
        return oturum.diziGosteriliyor
    }

    /// Abaküsle aynı yükseklikte görünen en az bir yan düğme var mı (abaküsü sütuna sığdırmak için;
    /// Android: AbacusSideButtonFit yalnızca görünür düğmeleri sayar).
    var yanDugmeVar: Bool {
        return !ipucuGizli || !sifirlaGizli || !adimAtlaGizli || ayarGorunur
    }

    /// Android: onCreate (lessonItem) + uploadLessonData.
    private static func oturumKur(_ b: KorDersBaslangici) -> KorDersOturumu {
        let kurucu = KorDersKurucu(
            globalPartId: Int32(b.partId),
            lessonType: Int32(b.dersTuru),
            isDailyQuestionMode: b.gunlukSoru != nil,
            mapFragmentStepIndex: Int32(b.dersIndeksi)
        )
        if let d = b.kor { kurucu.korAyarla(deger: d) }
        if let d = b.carpma { kurucu.carpmaAyarla(deger: d) }
        if let d = b.korCarpma { kurucu.korCarpmaAyarla(deger: d) }
        if let d = b.cikarma { kurucu.cikarmaAyarla(deger: d) }
        if let d = b.yarisYogunlugu { kurucu.yarisYogunluguAyarla(deger: Int32(d)) }
        if let d = b.sayiSuresiMs { kurucu.sayiSuresiAyarla(ms: Int64(d)) }
        if let d = b.kupaKazanc { kurucu.kupaKazancAyarla(deger: Int32(d)) }
        if let d = b.kupaKayip { kurucu.kupaKayipAyarla(deger: Int32(d)) }
        if let d = b.enKotuKupaSuresi { kurucu.enKotuKupaSuresiAyarla(deger: Int32(d)) }

        for islem in b.islemler {
            switch islem {
            case .dizi(let sayilar):
                kurucu.diziBaslat()
                for sayi in sayilar {
                    kurucu.diziyeEkle(sayi: Int32(truncatingIfNeeded: sayi))
                }
            case .carpma(let birinci, let islec, let ikinci):
                kurucu.islemEkle(
                    birinci: Int32(truncatingIfNeeded: birinci),
                    islec: islec,
                    ikinci: Int32(truncatingIfNeeded: ikinci)
                )
            }
        }
        // Sorular verilmediyse ders adımı tablosundan (kupa modunda sorulmaz; kural ortak kodda).
        if b.islemler.isEmpty, let ilkAdim = b.baslangicAdimi {
            kurucu.tablodanYukle(
                baslangicAdimi: Int32(ilkAdim),
                bitisAdimi: Int32(b.bitisAdimi ?? 0),
                bitisAdimiVar: b.bitisAdimi != nil,
                adimSayisi: Int32(b.adimSayisi),
                adimBitti: b.adimBitti
            )
        }
        return kurucu.kur()
    }

    // MARK: Yaşam döngüsü

    /// Android: onResume → startLearningSessionTracking.
    func gorundu() {
        if ogrenmeBaslangici == nil { ogrenmeBaslangici = Date() }
        // Android: onCreate → LessonSuccessRateRepository.recordQuestionEntry (günlük soru ve kupa modu
        // hariç; yalnızca ders ve sandık adımları). Ekran bir kez kurulur.
        if !girisKaydedildi {
            girisKaydedildi = true
            if oturum.basariOraniKapsaminda() {
                kayit.soruGirisiniKaydet(partId: baslangic.partId, dersIndeksi: baslangic.dersIndeksi)
            }
        }
    }

    /// Android: onPause / onDestroyView.
    func kayboldu() {
        // Android: onDestroyView → hiç cevap verilmeden çıkıldıysa recordAbandonWithoutAnswer.
        if !cevapVerildi && !sonlandi && !terkKaydedildi && oturum.basariOraniKapsaminda() {
            terkKaydedildi = true
            kayit.cevapsizTerkiKaydet(partId: baslangic.partId, dersIndeksi: baslangic.dersIndeksi)
        }
        if let baslangicAni = ogrenmeBaslangici {
            ogrenmeBaslangici = nil
            let gecen: Int = Int(Date().timeIntervalSince(baslangicAni) * 1000)
            if gecen > 0 { kayit.ogrenmeSuresiniKaydet(milisaniye: gecen) }
        }
        diziZamanlayicisiniIptalEt()
        sureyiDurdur()
    }

    // MARK: Başla

    /// Android: setupStartButton.
    func basla() {
        if basladi { return }
        basladi = true
        if oturum.sureBaslatilmali(sureBasladi: sureBasladi) {
            sureBasladi = true
            sureyiBaslat()
        }
        suankiSoruyuGoster()
    }

    /// Android: startTimer — ilk artış hemen, sonra saniyede bir.
    private func sureyiBaslat() {
        sureTik()
        let sayac = Timer(timeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.sureTik()
        }
        RunLoop.main.add(sayac, forMode: .common)
        sureSayaci = sayac
    }

    private func sureTik() {
        saniye += 1
        sureMetni = oturum.sureMetni(saniye: saniye)
    }

    private func sureyiDurdur() {
        sureSayaci?.invalidate()
        sureSayaci = nil
    }

    // MARK: Soru gösterimi

    /// Android: showCurrentOperation.
    private func suankiSoruyuGoster() {
        if oturum.soruSayisi == 0 { return }
        if carpmaGosterimi {
            if let m = oturum.birinciSayiMetni() { birinciSayi = m }
            if let m = oturum.islecMetni() { islec = m }
            if let m = oturum.ikinciSayiMetni() { ikinciSayi = m }
            return
        }
        diziZamanlayicisiniIptalEt()
        diziKayitlariniSifirla()
        if let adim = oturum.diziBaslat() {
            adimiUygula(adim)
        } else if let m = oturum.tekSayiMetni() {
            tahtaMetni = m
        }
    }

    /// Android: startShowingSequence → flashLog.clear(); flashMarks.clear() (dizi her başladığında).
    private func diziKayitlariniSifirla() {
        flashKaydi = []
        flashIsaretleri = []
        gosterilenSayilar = []
    }

    /// Android: markFlashNumber — abaküslü derste (isBlinding == null) sayı göründüğü anda abaküsün
    /// değeri kayda eklenir ve sırası işaretlenir.
    private func sayiGoruldu(_ metin: String) {
        guard let sayi = Int(metin) else { return }
        gosterilenSayilar.append(sayi)
        if !oturum.abakusleCevap { return }
        if flashKaydi.count >= KorDersModeli.flashKaydiEnCok { return }
        flashKaydi.append(abakus.anlikDeger)
        flashIsaretleri.append(flashKaydi.count - 1)
    }

    /// Abaküs değeri değişince (AbakusGorunumu.degerDegisti): işaretli kayıt varsa değer eklenir.
    /// Android: onValueChanged — sıfırlama sürerken, kayıt boşken, üst sınırda ve aynı değer üst üste
    /// gelince eklenmez.
    func abakusDegeriDegisti(_ deger: Int) {
        if flashIsaretleri.isEmpty || abakus.sifirlaniyor { return }
        if flashKaydi.count >= KorDersModeli.flashKaydiEnCok { return }
        if flashKaydi.last == deger { return }
        flashKaydi.append(deger)
    }

    /// Ortak kodun döndürdüğü dizi adımını ekrana ve zamanlayıcıya uygular.
    private func adimiUygula(_ adim: KorDersAdimi) {
        if let metin = adim.metin {
            tahtaMetni = metin
            if !metin.isEmpty { sayiGoruldu(metin) }
        }
        if adim.kontrolKapat { kontrolEtkin = false }
        if adim.bitti { kontrolEtkin = true }

        let tur: Int32 = adim.zamanlayici
        if tur == 0 { return }
        let gecikme: Double = Double(adim.gecikmeMs) / 1000.0
        diziIsiniKur(gecikme: gecikme) { [weak self] in
            guard let self = self else { return }
            if tur == 1 {
                // Android: onShowNextNumberStep — ekran boşalır.
                self.adimiUygula(self.oturum.sonrakiSayiAdimi())
            } else {
                // Android: onSequenceRevealStep — sıradaki sayı.
                self.adimiUygula(self.oturum.sayiGosterAdimi())
            }
        }
    }

    private func diziIsiniKur(gecikme: Double, _ govde: @escaping () -> Void) {
        diziIsi?.cancel()
        let yeni = DispatchWorkItem(block: govde)
        diziIsi = yeni
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme, execute: yeni)
    }

    private func diziZamanlayicisiniIptalEt() {
        diziIsi?.cancel()
        diziIsi = nil
    }

    /// Android: skipToNextSequenceNumber — süresini beklemeden sıradaki sayıya geçer; kendiliğinden geçişle
    /// aynı yoldan gider (sayı silinir, 200 ms sonra sıradaki gelir). Ortak kod `nil` döndürürse basış
    /// yok sayılır (gösterim yok ya da boşluk sürüyor); değilse bekleyen zamanlayıcı iptal edilip dönen
    /// adım uygulanır.
    func adimAtla() {
        guard let adim = oturum.diziAtla() else { return }
        diziZamanlayicisiniIptalEt()
        adimiUygula(adim)
    }

    /// Android: resetClickListener.
    func sifirla() {
        oturum.kontrolSifirla()
        // isBlinding == null → abaküs sıfırlanır (resetAbacus); true → kutu temizlenir.
        if oturum.abakusleCevap { abakus.sifirla(animasyonlu: true) }
        if klavyeyleCevap { girdi = "" }
        if oturum.diziVar {
            diziZamanlayicisiniIptalEt()
            oturum.diziDurdur()
            tahtaMetni = ""
            let gecikme: Double = Double(BlindingLessonRules.shared.SEQUENCE_RESTART_DELAY_MS) / 1000.0
            diziIsiniKur(gecikme: gecikme) { [weak self] in
                guard let self = self else { return }
                self.diziKayitlariniSifirla()
                self.adimiUygula(self.oturum.diziYenidenBaslat())
            }
        }
    }

    // MARK: İpucu

    /// Android: fubHintClickListener. Balon ampulün kuyruğundan büyüyerek açılır (hint_bubble_in:
    /// 350 ms, hafif aşan eğri) ve ampule doğru küçülerek kapanır (hint_bubble_out: 200 ms).
    func ipucunuDegistir() {
        if ipucuAcik {
            withAnimation(.easeIn(duration: 0.2)) { ipucuAcik = false }
        } else {
            withAnimation(.spring(response: 0.35, dampingFraction: 0.65)) { ipucuAcik = true }
        }
    }

    // MARK: Kontrol

    /// Kontrol düğmesi basılabilir mi (görünüm de aynı koşulla soluk çizer).
    var kontrolBasilabilir: Bool { basladi && kontrolEtkin }

    /// Android: controlButtonAnim (ACTION_UP).
    func kontrolEt() {
        if !basladi || !kontrolEtkin || kontrolKilitli || panel != nil || sonlandi { return }
        let simdi = Date()
        let aralik: Double = Double(AbacusLessonRules.shared.CONTROL_DEBOUNCE_MS) / 1000.0
        if simdi.timeIntervalSince(sonKontrolAni) < aralik { return }
        sonKontrolAni = simdi

        // Android: controlNumber = abacusController.getCurrentValue() (abaküsle cevaplanan dersler).
        let abakusDegeri: Int32 = oturum.abakusleCevap ? Int32(abakus.anlikDeger) : 0
        // Android: lastWrongAnswer = controlNumber (kutudaki sayı ya da abaküs değeri).
        let cevapDegeri: Int = klavyeyleCevap ? (Int(girdi) ?? 0) : Int(abakusDegeri)
        let dogru: Bool = oturum.kontrolEt(girdi: girdi, abakusDegeri: abakusDegeri)
        cevapVerildi = true
        sonCevap = cevapDegeri
        ilerlemeyiGuncelle(dogru: dogru)

        if oturum.kupaModu {
            kupaSonucunuYaz(delta: Int(oturum.kupaDeltasi(dogru: dogru)))
        }

        sonucPaneliniGoster(dogru: dogru)
        oturum.kontrolSifirla()
        girdi = ""
    }

    /// Android: updateProgressBar — 700 ms, yavaşlayarak; doğruda yeşil → açık sarı → yeşil,
    /// yanlışta yeşil → koyu kırmızı.
    private func ilerlemeyiGuncelle(dogru: Bool) {
        let hedef: CGFloat = CGFloat(oturum.ilerlemeBitisAgirligi())
        let sure: Double = Double(AbacusLessonRules.shared.PROGRESS_ANIMATION_MS) / 1000.0
        ilerlemeRengi = KorDersStil.ilerlemeYesil
        withAnimation(.easeOut(duration: sure)) { ilerleme = hedef }
        if dogru {
            let yari: Double = sure / 2.0
            withAnimation(.easeOut(duration: yari)) { ilerlemeRengi = KorDersStil.ilerlemeSari }
            DispatchQueue.main.asyncAfter(deadline: .now() + yari) { [weak self] in
                withAnimation(.easeOut(duration: yari)) {
                    self?.ilerlemeRengi = KorDersStil.ilerlemeYesil
                }
            }
        } else {
            withAnimation(.easeOut(duration: sure)) { ilerlemeRengi = KorDersStil.ilerlemeKoyuKirmizi }
        }
    }

    /// Android: showResultPanel — panel 200 ms'de alttan kayarak ve belirerek gelir.
    private func sonucPaneliniGoster(dogru: Bool) {
        if panel != nil { return }
        let kurallar = AbacusLessonRules.shared
        if dogru {
            // Dönüş değeri "konfeti oynatılsın mı" (lottieView: animaton_thirteen, bir kez).
            if oturum.dogruIslendi() { konfeti = true }
            ses.cal(ad: kurallar.SOUND_CORRECT, sesDuzeyi: kurallar.SOUND_CORRECT_VOLUME)
        } else {
            ses.cal(ad: kurallar.SOUND_INCORRECT, sesDuzeyi: 1.0)
            dogruCevapEtiketi = oturum.dogruCevapEtiketi
            seninCevabinMetni = oturum.seninCevabinMetni
            analizDugmesiniHazirla()
        }
        kontrolKilitli = true
        panelAnimasyonda = true
        withAnimation(.easeInOut(duration: 0.2)) { panel = dogru ? .dogru : .yanlis }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            self?.panelAnimasyonda = false
        }
    }

    /// Paneli 200 ms'de aşağı kaydırıp gizler (Android: AccelerateInterpolator), sonra `sonra` çalışır.
    private func paneliGizle(sonra: (() -> Void)? = nil) {
        withAnimation(.easeIn(duration: 0.2)) { panel = nil }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            self?.panelAnimasyonda = false
            sonra?()
        }
    }

    // MARK: Panel düğmeleri

    /// Android: continueButton (doğru paneli).
    func devamEt() {
        if panelAnimasyonda || panel != .dogru { return }
        panelAnimasyonda = true
        if oturum.kupaModu {
            paneliGizle { [weak self] in self?.dersiBitir() }
            return
        }
        let gecikme: Double = Double(AbacusLessonRules.shared.CONTROL_REENABLE_DELAY_CORRECT_MS) / 1000.0
        sonrakiSoruyaGec(kontrolGecikmesi: gecikme)
    }

    /// Android: okayButton (yanlış paneli).
    func tamam() {
        if panelAnimasyonda || panel != .yanlis { return }
        panelAnimasyonda = true
        let sonraki: String = oturum.yanlisSonrasi()
        switch sonraki {
        case "CUP_RESULT_FALSE":
            paneliGizle { [weak self] in self?.basarisizSonuc(sandikBasarisiz: false) }
        case "DAILY_QUESTION_PANEL":
            // Android: finishLesson(isWrongDailyQuestion = true) → handleDailyQuestionWrongAnswer.
            paneliGizle { [weak self] in
                guard let self = self else { return }
                self.sureyiDurdur()
                if self.oturum.dersBitisiHakkiAl() {
                    self.gunlukSoruyuBitir(basarili: false)
                }
            }
        case "RACE_QUESTION_PANEL":
            // Android: finishLesson(isWrongRaceLesson = true) → showLessonResultFalse.
            paneliGizle { [weak self] in
                guard let self = self else { return }
                self.sureyiDurdur()
                if self.oturum.dersBitisiHakkiAl() {
                    self.basarisizSonuc(sandikBasarisiz: false)
                }
            }
        default:
            let gecikme: Double =
                Double(AbacusLessonRules.shared.CONTROL_REENABLE_DELAY_INCORRECT_MS) / 1000.0
            sonrakiSoruyaGec(kontrolGecikmesi: gecikme)
        }
    }

    /// İki panelin ortak "sıradaki soru" yolu (Android: currentIndex++ ve sonrası).
    private func sonrakiSoruyaGec(kontrolGecikmesi: Double) {
        let soruKaldi: Bool = oturum.sonrakiSoru()
        DispatchQueue.main.asyncAfter(deadline: .now() + kontrolGecikmesi) { [weak self] in
            self?.kontrolKilitli = false
        }
        paneliGizle()
        // Android: isBlinding == null ise abaküs sıfırlanır (resetAbacus).
        if oturum.abakusleCevap { abakus.sifirla(animasyonlu: true) }
        if soruKaldi {
            suankiSoruyuGoster()
        } else {
            // Android: finishLesson — süre durur; kapı (lessonFinishStarted) ikinci çağrıyı yok sayar.
            // Ders sonu anketi güncel Android'de kalktı: doğrudan sonuç.
            sureyiDurdur()
            if oturum.dersBitisiHakkiAl() {
                dersiBitir()
            }
        }
    }

    // MARK: Çıkış

    /// Android: setupQuitButton.
    func cik() {
        if sonlandi || panel != nil { return }
        if baslangic.gunlukSoru != nil && basladi {
            gunlukSoruyuBitir(basarili: false)
            return
        }
        if oturum.kupaModu {
            // Android: `globalPartId == 9 && lessonStarted && !cupResultSubmitted` — cevabını verip ekranı
            // kapatan kullanıcı "terk etti" sayılmaz: ikinci kez kupa kaybetmez, ikinci kez enerji ödemez.
            if oturum.kupaCikisCezasiVerilmeli(dersBasladi: basladi) {
                kupaSonucunuYaz(delta: Int(oturum.kupaDeltasi(dogru: false)))
                enerjiHarca(1)
            }
            if basladi {
                sonucuVer(.kapat, sandikBasarisiz: false)
                return
            }
        }
        sonlandir()
        kapat()
    }

    /// Android: submitCupResultOnce — kupa sonucu tur başına bir kez yazılır (cevap, çıkış düğmesi ve geri
    /// tuşu aynı turun sonucunu göndermeye çalışabilir). Kapı ortak kodda.
    private func kupaSonucunuYaz(delta: Int) {
        if !oturum.kupaSonucuHakkiAl() { return }
        sonKupaDeltasi = delta
        kayit.kupaDeltasiniIsle(hedef: oturum.kupaDeltaHedefi(), delta: delta)
    }

    // MARK: Hata analizi

    /// Android: bindReviewButton — yalnızca sayı dizisi sorularında (çarpma yok) ve analiz yapılabiliyorsa
    /// düğme görünür. Kupa modunda da vardır. Kayıt yalnızca dizi tam izlendiyse (her sayı bir kez
    /// işaretli) verilir.
    private func analizDugmesiniHazirla() {
        analizGorunur = false
        guard let baglanti = sobiAnaliz else { return }
        if carpmaGosterimi || !oturum.diziVar || gosterilenSayilar.isEmpty { return }
        if !baglanti.yapilabilir(gosterilenSayilar) { return }
        let tam: Bool = flashIsaretleri.count == gosterilenSayilar.count
        analizSayilari = gosterilenSayilar
        analizKaydi = tam ? flashKaydi : []
        analizIsaretleri = tam ? flashIsaretleri : []
        analizGorunur = true
    }

    /// Android: incorrectSobiAnalyze tıklaması. Sandık dersinde analiz açıkken süre durur, pencere kapanınca
    /// kaldığı yerden devam eder.
    func sobiAnaliziDokunuldu() {
        guard let baglanti = sobiAnaliz, analizGorunur, !analizAcik else { return }
        analizAcik = true
        let calisiyor: Bool = sureSayaci != nil
        if calisiyor {
            sureyiDurdur()
        }
        let istek = AbakusSobiAnalizIstegi(
            sayilar: analizSayilari,
            cevap: sonCevap,
            kayit: analizKaydi,
            isaretler: analizIsaretleri,
            dersBasligi: baslangic.baslik,
            kapandi: { [weak self] in
                DispatchQueue.main.async {
                    self?.analizKapandi(sureDevamEtsin: calisiyor)
                }
            }
        )
        baglanti.istendi(istek)
    }

    /// Android: MistakeReviewDialog.RESULT_CLOSED → `handler.postDelayed(runnable, 1000)`.
    private func analizKapandi(sureDevamEtsin: Bool) {
        analizAcik = false
        if sureDevamEtsin && !sonlandi && sureSayaci == nil {
            let sayac = Timer(timeInterval: 1.0, repeats: true) { [weak self] _ in
                self?.sureTik()
            }
            RunLoop.main.add(sayac, forMode: .common)
            sureSayaci = sayac
        }
    }

    // MARK: Bitiş

    /// Android: finishLessonAfterLastQuestion.
    private func dersiBitir() {
        let sonuc: String = oturum.bitis(sureMetni: sureMetni)
        switch sonuc {
        case "DAILY_QUESTION_COMPLETE":
            gunlukSoruyuBitir(basarili: true)
        case "CLOSE":
            sonucuVer(.kapat, sandikBasarisiz: false)
        case "CHEST_RESULT":
            sonucuVer(.sandikSonucu, sandikBasarisiz: false)
        case "CHEST_FAILURE":
            basarisizSonuc(sandikBasarisiz: true)
        case "LESSON_RESULT_FALSE":
            basarisizSonuc(sandikBasarisiz: false)
        default:
            sonucuVer(.dersSonucu, sandikBasarisiz: false)
        }
    }

    /// Android: showLessonResultFalse.
    private func basarisizSonuc(sandikBasarisiz: Bool) {
        let eylem: String = oturum.yanlisSonucEylemi()
        switch eylem {
        case "CUP_USE_ENERGY_AND_CLOSE":
            enerjiHarca(1)
            sonucuVer(.kapat, sandikBasarisiz: false)
        case "CLOSE":
            sonucuVer(.kapat, sandikBasarisiz: false)
        default:
            sonucuVer(.dersSonucuBasarisiz, sandikBasarisiz: sandikBasarisiz)
        }
    }

    /// Android: handleDailyQuestionLessonComplete / handleDailyQuestionWrongAnswer.
    private func gunlukSoruyuBitir(basarili: Bool) {
        guard let gunluk = baslangic.gunlukSoru else {
            sonucuVer(.kapat, sandikBasarisiz: false)
            return
        }
        if sonlandi { return }
        let yon: KorDersYonlendirme = basarili ? .gunlukSoruTamam : .gunlukSoruYanlis
        kayit.gunlukSoruSonucunuKaydet(
            donemAnahtari: gunluk.donemAnahtari, yuva: gunluk.yuva, basarili: basarili
        ) { [weak self] in
            self?.sonucuVer(yon, sandikBasarisiz: false)
        }
    }

    private func enerjiHarca(_ miktar: Int) {
        harcananEnerji += miktar
        enerji.enerjiHarca(miktar)
    }

    private func sonlandir() {
        sonlandi = true
        diziZamanlayicisiniIptalEt()
        oturum.diziDurdur()
        sureyiDurdur()
    }

    private func sonucuVer(_ yon: KorDersYonlendirme, sandikBasarisiz: Bool) {
        if sonlandi { return }
        sonlandir()
        let kupaHedefi: String? = sonKupaDeltasi == nil ? nil : oturum.kupaDeltaHedefi()
        let sonuc = KorDersSonucu(
            yonlendirme: yon,
            dogruSayisi: Int(oturum.dogruSayisi),
            toplamSoru: Int(oturum.soruSayisi),
            basariOrani: oturum.basariOrani(),
            dersPuani: Int(oturum.dersPuani()),
            sure: sureMetni,
            enKotuKupaSuresi: Int(oturum.enKotuKupaSuresi),
            carpan: oturum.carpan,
            toplamPuan: Int(oturum.toplamPuan),
            sandikBasarisiz: sandikBasarisiz,
            yarisDersi: oturum.yarisDersi,
            partId: baslangic.partId,
            dersIndeksi: baslangic.dersIndeksi,
            dersTuru: baslangic.dersTuru,
            kupaDeltasi: sonKupaDeltasi,
            kupaDeltaHedefi: kupaHedefi,
            harcananEnerji: harcananEnerji,
            soruSuresiMs: Int(Date().timeIntervalSince(girisAni) * 1000.0)
        )
        bitti(sonuc)
    }
}

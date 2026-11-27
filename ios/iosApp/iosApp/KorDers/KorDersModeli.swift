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
    /// Android: splitTextEqually(lessonHint).
    let ipucuMetni: String

    // MARK: İç durum

    private let baslangic: KorDersBaslangici
    private let oturum: KorDersOturumu
    private let kayit: KorDersKayitServisi
    private let enerji: KorDersEnerjiServisi
    private let ses: KorDersSesServisi
    private let bitti: (KorDersSonucu) -> Void
    private let kapat: () -> Void

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
        bitti: @escaping (KorDersSonucu) -> Void,
        kapat: @escaping () -> Void
    ) {
        self.baslangic = baslangic
        self.kayit = kayit
        self.enerji = enerji
        self.ses = ses
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

        let hamIpucu: String = baslangic.ipucu ?? ""
        self.ipucuGizli = oturum.ipucuGizli || hamIpucu.isEmpty
        self.ipucuMetni = hamIpucu.isEmpty ? "" : oturum.ipucuMetni(ipucu: hamIpucu)
    }

    deinit {
        diziIsi?.cancel()
        sureSayaci?.invalidate()
    }

    /// Android: setupAbacusController — lessonItem.type == 2 ise 50 ms, değilse 300 ms.
    var boncukAnimasyonMs: Int {
        return Int(oturum.boncukAnimasyonMs)
    }

    /// Android: skipStepButton yalnızca dizi gösterilirken (isShowingSequence) çalışır ve oynar.
    var adimAtlanabilir: Bool {
        return oturum.diziGosteriliyor
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
    }

    /// Android: onPause / onDestroyView.
    func kayboldu() {
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
        if let adim = oturum.diziBaslat() {
            adimiUygula(adim)
        } else if let m = oturum.tekSayiMetni() {
            tahtaMetni = m
        }
    }

    /// Ortak kodun döndürdüğü dizi adımını ekrana ve zamanlayıcıya uygular.
    private func adimiUygula(_ adim: KorDersAdimi) {
        if let metin = adim.metin { tahtaMetni = metin }
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

    /// Android: setupSkipStepButton — beklemeden sıradaki sayıya geçer.
    func adimAtla() {
        if !oturum.diziGosteriliyor { return }
        diziZamanlayicisiniIptalEt()
        adimiUygula(oturum.sayiGosterAdimi())
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
                self.adimiUygula(self.oturum.diziYenidenBaslat())
            }
        }
    }

    // MARK: İpucu

    /// Android: fubHintClickListener.
    func ipucunuDegistir() {
        withAnimation(.linear(duration: 0.5)) { ipucuAcik.toggle() }
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
        let dogru: Bool = oturum.kontrolEt(girdi: girdi, abakusDegeri: abakusDegeri)
        ilerlemeyiGuncelle(dogru: dogru)

        if oturum.kupaModu {
            let delta: Int = Int(oturum.kupaDeltasi(dogru: dogru))
            sonKupaDeltasi = delta
            kayit.kupaDeltasiniIsle(hedef: oturum.kupaDeltaHedefi(), delta: delta)
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
            paneliGizle { [weak self] in self?.dersiBitir(soruPaneli: false) }
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
            paneliGizle { [weak self] in self?.basarisizSonuc(sandikBasarisiz: false, soruPaneli: false) }
        case "DAILY_QUESTION_PANEL":
            // Android: önce soru paneli (isWrongDailyQuestion), sonra handleDailyQuestionWrongAnswer.
            paneliGizle { [weak self] in
                self?.sureyiDurdur()
                self?.gunlukSoruyuBitir(basarili: false, soruPaneli: true)
            }
        case "RACE_QUESTION_PANEL":
            // Android: önce soru paneli (isWrongRaceLesson), sonra showLessonResultFalse.
            paneliGizle { [weak self] in
                self?.sureyiDurdur()
                self?.basarisizSonuc(sandikBasarisiz: false, soruPaneli: true)
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
            // Android: showQuestionPanel — süre durur, özet ekranından sonra ders biter.
            sureyiDurdur()
            dersiBitir(soruPaneli: true)
        }
    }

    // MARK: Çıkış

    /// Android: setupQuitButton.
    func cik() {
        if sonlandi || panel != nil { return }
        if baslangic.gunlukSoru != nil && basladi {
            gunlukSoruyuBitir(basarili: false, soruPaneli: false)
            return
        }
        if oturum.kupaModu && basladi {
            let delta: Int = Int(oturum.kupaDeltasi(dogru: false))
            sonKupaDeltasi = delta
            kayit.kupaDeltasiniIsle(hedef: oturum.kupaDeltaHedefi(), delta: delta)
            enerjiHarca(1)
            sonucuVer(.kapat, sandikBasarisiz: false, soruPaneli: false)
            return
        }
        sonlandir()
        kapat()
    }

    // MARK: Bitiş

    /// Android: finishLessonAfterLastQuestion.
    private func dersiBitir(soruPaneli: Bool) {
        let sonuc: String = oturum.bitis(sureMetni: sureMetni)
        switch sonuc {
        case "DAILY_QUESTION_COMPLETE":
            gunlukSoruyuBitir(basarili: true, soruPaneli: soruPaneli)
        case "CLOSE":
            sonucuVer(.kapat, sandikBasarisiz: false, soruPaneli: soruPaneli)
        case "CHEST_RESULT":
            sonucuVer(.sandikSonucu, sandikBasarisiz: false, soruPaneli: soruPaneli)
        case "CHEST_FAILURE":
            basarisizSonuc(sandikBasarisiz: true, soruPaneli: soruPaneli)
        case "LESSON_RESULT_FALSE":
            basarisizSonuc(sandikBasarisiz: false, soruPaneli: soruPaneli)
        default:
            sonucuVer(.dersSonucu, sandikBasarisiz: false, soruPaneli: soruPaneli)
        }
    }

    /// Android: showLessonResultFalse.
    private func basarisizSonuc(sandikBasarisiz: Bool, soruPaneli: Bool) {
        let eylem: String = oturum.yanlisSonucEylemi()
        switch eylem {
        case "CUP_USE_ENERGY_AND_CLOSE":
            enerjiHarca(1)
            sonucuVer(.kapat, sandikBasarisiz: false, soruPaneli: soruPaneli)
        case "CLOSE":
            sonucuVer(.kapat, sandikBasarisiz: false, soruPaneli: soruPaneli)
        default:
            sonucuVer(.dersSonucuBasarisiz, sandikBasarisiz: sandikBasarisiz, soruPaneli: soruPaneli)
        }
    }

    /// Android: handleDailyQuestionLessonComplete / handleDailyQuestionWrongAnswer.
    private func gunlukSoruyuBitir(basarili: Bool, soruPaneli: Bool) {
        guard let gunluk = baslangic.gunlukSoru else {
            sonucuVer(.kapat, sandikBasarisiz: false, soruPaneli: soruPaneli)
            return
        }
        if sonlandi { return }
        let yon: KorDersYonlendirme = basarili ? .gunlukSoruTamam : .gunlukSoruYanlis
        kayit.gunlukSoruSonucunuKaydet(
            donemAnahtari: gunluk.donemAnahtari, yuva: gunluk.yuva, basarili: basarili
        ) { [weak self] in
            self?.sonucuVer(yon, sandikBasarisiz: false, soruPaneli: soruPaneli)
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

    private func sonucuVer(_ yon: KorDersYonlendirme, sandikBasarisiz: Bool, soruPaneli: Bool) {
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
            soruPaneliKupaSuresi: Int(oturum.soruPaneliKupaSuresi),
            carpan: oturum.carpan,
            toplamPuan: Int(oturum.toplamPuan),
            sandikBasarisiz: sandikBasarisiz,
            soruPaneliGosterilmeli: soruPaneli,
            yarisDersi: oturum.yarisDersi,
            partId: baslangic.partId,
            dersIndeksi: baslangic.dersIndeksi,
            dersTuru: baslangic.dersTuru,
            kupaDeltasi: sonKupaDeltasi,
            kupaDeltaHedefi: kupaHedefi,
            harcananEnerji: harcananEnerji
        )
        bitti(sonuc)
    }
}

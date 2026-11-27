import Foundation
import Shared
import SwiftUI

// Abaküs dersi ekranının görünüm modeli.
//
// Android karşılığı: AbacusFragment (setupStartButton, showCurrentOperation, controlButtonAnim,
// updateProgressBar, showResultPanel, finishLesson, closeFragment, bindReviewButton).
// Kurallar ortak koddan gelir (AbacusLessonSession; Swift'e AbakusDersKoprusu / AbakusDersAkisi
// üzerinden yalın imzalarla açılır). Ortak koda yapılan bütün çağrılar bu dosyadadır.
//
// Güncel Android'de ipucu düğmesi ekrandan kaldırıldı (hintContainer gone); ipucu durumu bu modelden
// de silindi. Ders sonu anketi (QuestionPanelFragment) de kalktı: sorular bitince doğrudan sonuç.

/// Alttan çıkan sonuç paneli.
enum AbakusSonucPaneli {
    case yok
    case dogru
    case yanlis
}

final class AbakusDersModeli: ObservableObject {
    // MARK: Görünümün okuduğu durum

    /// Tahtadaki soru. Android'deki gibi yalnızca dolu gelen parça değişir.
    @Published private(set) var ilkSayi: String = ""
    @Published private(set) var islec: String = ""
    @Published private(set) var ikinciSayi: String = ""

    @Published private(set) var sureMetni: String = "0:00"
    /// 0...1
    @Published private(set) var ilerleme: CGFloat = 0
    @Published private(set) var ilerlemeRengi: Color = AbakusDersModeli.yesil

    @Published private(set) var baslaGorunur: Bool = false
    @Published private(set) var kontrolGorunur: Bool = false
    @Published private(set) var kontrolEtkin: Bool = true

    /// Görünüm bu değerin değişimini kayma + belirme geçişiyle çizer.
    @Published private(set) var panel: AbakusSonucPaneli = .yok
    /// Android: overlay (ekranın tamamını kaplayan, dokunmayı yutan görünüm).
    @Published private(set) var ortuAcik: Bool = false
    @Published private(set) var cevapMetni: String = ""
    @Published private(set) var dogruCevapMetni: String = ""

    /// Yanlış panelinde "Sobi ile analiz et" düğmesi görünür mü (Android: bindReviewButton).
    @Published private(set) var analizGorunur: Bool = false
    /// Kapatma düğmesine basıldı; ekran sola kayıyor.
    @Published private(set) var kapaniyor: Bool = false
    /// Kısa bilgi balonu (Android: Toast).
    @Published private(set) var bildirim: String?
    /// Son soruda bütün cevaplar doğru (Android: konfeti Lottie'si).
    @Published private(set) var konfeti: Bool = false

    let abakus = AbakusDenetleyici()
    let baslangic: AbakusDersBaslangici
    let hizmetler: AbakusDersHizmetleri

    var sureGorunur: Bool {
        return akis?.sureGorunur ?? false
    }

    /// Android: BEAD_ANIMATION_MS — her ders türünde aynı (eskiden sandıkta 50 ms'ydi). Değer ortak
    /// koddadır.
    var boncukAnimasyonSuresiMs: Int {
        return Int(AbacusLessonRules.shared.beadAnimationDurationMs(lessonType: Int32(baslangic.dersTuru)))
    }

    // MARK: İç durum

    private let akis: AbakusDersAkisi?
    private let bitti: (AbakusDersSonucu) -> Void
    private let sobiAnaliz: AbakusSobiAnalizBaglantisi?
    private var zamanlayici: Timer?
    /// Android: mistakeLog — bu sorudaki abaküs değerleri, sırayla (0'dan başlar). Yanlış cevapta hata
    /// analizi çocuğun nerede yanıldığını bununla bulur. Sıfırlama ara değerleri alınmaz.
    private var hataKaydi: [Int] = [0]
    private static let hataKaydiEnCok: Int = 400
    /// Yanlış panelindeki analiz düğmesi için o anın görüntüsü (cevap, kayıt, sayılar).
    private var analizSayilari: [Int] = []
    private var analizCevabi: Int = 0
    private var analizKaydi: [Int] = []
    private var analizAcik: Bool = false
    /// Android: questionSessionStartMs / hasSubmittedAnyAnswer (başarı oranı ölçümü).
    private let girisAni: Date = Date()
    private var cevapVerildi: Bool = false
    private var terkKaydedildi: Bool = false
    private var panelAnimasyonda: Bool = false
    private var dersBitti: Bool = false
    private var basladi: Bool = false
    private var gorunmeAni: Date?
    private var bildirimSayaci: Int = 0

    /// Android: updateProgressBar renkleri (#7CFC00, #FFFF99, #990000).
    static let yesil = AbakusRenk.renk(0x7CFC00)
    static let acikSari = AbakusRenk.renk(0xFFFF99)
    static let koyuKirmizi = AbakusRenk.renk(0x990000)

    init(
        baslangic: AbakusDersBaslangici,
        hizmetler: AbakusDersHizmetleri,
        sobiAnaliz: AbakusSobiAnalizBaglantisi?,
        bitti: @escaping (AbakusDersSonucu) -> Void
    ) {
        self.baslangic = baslangic
        self.hizmetler = hizmetler
        self.sobiAnaliz = sobiAnaliz
        self.bitti = bitti
        self.akis = AbakusDersModeli.akisOlustur(baslangic)
    }

    deinit {
        zamanlayici?.invalidate()
    }

    private static func akisOlustur(_ baslangic: AbakusDersBaslangici) -> AbakusDersAkisi? {
        let kopru = AbakusDersKoprusu.shared
        let tur = Int32(baslangic.dersTuru)
        switch baslangic.soruKaynagi {
        case .dersAdimi(let kimlik):
            return kopru.akisDersKimliginden(lessonOperationsId: Int32(kimlik), lessonType: tur)
        case .hazir(let sorular):
            let liste = AbakusSoruListesi()
            for soru in sorular {
                if let ilk = soru.ilkSayi, let ikinci = soru.ikinciSayi {
                    liste.ekle(ilkSayi: Int32(ilk), islec: soru.islec, ikinciSayi: Int32(ikinci))
                } else if let sayi = Int32(soru.islec) {
                    liste.ekleTekSayi(sayi: sayi)
                } else {
                    // Cevabı hesaplanamayan soru: Android'de çöker; burada ders açılmaz.
                    return nil
                }
            }
            return kopru.akisSorulardan(sorular: liste, lessonType: tur)
        }
    }

    // MARK: Yaşam döngüsü

    /// Android: onViewCreated → setupStartButton; onResume → startLearningSessionTracking.
    func gorundu() {
        if gorunmeAni == nil {
            gorunmeAni = Date()
        }
        if basladi { return }
        basladi = true
        guard let akis = akis else {
            bildir("Bu dersin soruları açılamadı.")
            return
        }
        // Android: onCreate → LessonSuccessRateRepository.recordQuestionEntry (ders ve sandık).
        if baslangic.basariOraniKapsaminda {
            hizmetler.soruGirisiniKaydet(baslangic: baslangic)
        }
        if akis.baslaDugmesiGerekli {
            // Sandık: "Başla" düğmesine basılana kadar soru ve süre başlamaz.
            baslaGorunur = true
            kontrolGorunur = false
        } else {
            baslaGorunur = false
            kontrolGorunur = true
            dersiBaslat()
        }
    }

    /// Android: onPause / onDestroyView → stopLearningSessionTracking.
    func kayboldu() {
        // Android: onDestroyView → hiç cevap verilmeden çıkıldıysa recordAbandonWithoutAnswer.
        if !cevapVerildi && !dersBitti && baslangic.basariOraniKapsaminda && !terkKaydedildi {
            terkKaydedildi = true
            hizmetler.cevapsizTerkiKaydet(baslangic: baslangic)
        }
        guard let an = gorunmeAni else { return }
        gorunmeAni = nil
        let gecen: Double = Date().timeIntervalSince(an) * 1000.0
        if gecen > 0 {
            hizmetler.ogrenmeSuresiniKaydet(milisaniye: Int(gecen))
        }
    }

    // MARK: Başlatma ve süre

    /// Android: startButton.setOnClickListener.
    func baslaDokunuldu() {
        guard let akis = akis, !akis.basladi else { return }
        baslaGorunur = false
        kontrolGorunur = true
        dersiBaslat()
    }

    private func dersiBaslat() {
        guard let akis = akis else { return }
        if !akis.baslat() { return }
        hizmetler.enerjiDus(baslangic: baslangic)
        if akis.sureCalisiyor {
            sureyiBaslat()
        }
        soruyuGoster()
    }

    /// Android: startTimer — ilk adım hemen atılır (süre "0:01" ile başlar), sonra saniyede bir.
    private func sureyiBaslat() {
        zamanlayici?.invalidate()
        sureAdimi()
        let yeni = Timer(timeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.sureAdimi()
        }
        RunLoop.main.add(yeni, forMode: .common)
        zamanlayici = yeni
    }

    private func sureAdimi() {
        guard let akis = akis, akis.sureCalisiyor else {
            sureyiDurdur()
            return
        }
        sureMetni = akis.sureAdimi()
    }

    private func sureyiDurdur() {
        zamanlayici?.invalidate()
        zamanlayici = nil
    }

    /// Android: analiz penceresi kapanınca `handler.postDelayed(runnable, 1000)` — ilk adım 1 sn sonra.
    private func sureyiDevamEttir() {
        zamanlayici?.invalidate()
        let yeni = Timer(timeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.sureAdimi()
        }
        RunLoop.main.add(yeni, forMode: .common)
        zamanlayici = yeni
    }

    /// Android: showCurrentOperation.
    private func soruyuGoster() {
        guard let akis = akis else { return }
        // Yeni soru: abaküs sıfırdan başlıyor, hata kaydı da.
        hataKaydi = [0]
        if let metin = akis.ilkSayiMetni() {
            ilkSayi = metin
        }
        if let metin = akis.islecMetni() {
            islec = metin
        }
        if let metin = akis.ikinciSayiMetni() {
            ikinciSayi = metin
        }
    }

    /// Android: rehberin son sayfasından sonra showCurrentOperation() — yeni soru gibi hata kaydı sıfırlanır
    /// (rehber boncukları oynatırken biriken ara değerler analize karışmasın). Rehber katmanı çağırır.
    func rehberBitti() {
        soruyuGoster()
    }

    // MARK: Kontrol

    /// Android: controlButtonAnim, ACTION_UP dalı.
    func kontrolDokunuldu() {
        guard let akis = akis, kontrolEtkin, !dersBitti, panel == .yok else { return }
        let simdiMs: Double = Date().timeIntervalSince1970 * 1000.0
        let sonuc = akis.kontrol(
            nowMs: Int64(simdiMs), isOnline: hizmetler.cevrimici, abacusValue: Int32(abakus.anlikDeger)
        )
        let durum = Int(sonuc.durum)
        if durum == 0 {
            // 100 ms içinde ikinci basış: yok sayılır.
            return
        }
        if durum == 1 {
            hizmetler.cevrimdisiGoster()
            bildir(Metin.al("ui_internet_baglantiniz_yok"))
            return
        }
        cevapVerildi = true
        if sonuc.sureDurdu {
            sureyiDurdur()
        }
        ilerlemeyiGuncelle(dogru: sonuc.dogru, hedef: sonuc.ilerlemeHedefi)
        paneliGoster(sonuc)
    }

    /// Android: updateProgressBar — 700 ms, DecelerateInterpolator. Doğruda renk yeşil → açık sarı →
    /// yeşil; yanlışta yeşil → koyu kırmızı (ve öyle kalır).
    private func ilerlemeyiGuncelle(dogru: Bool, hedef: Float) {
        var sinirli: CGFloat = 1
        if hedef.isFinite {
            sinirli = min(max(CGFloat(hedef), 0), 1)
        }
        ilerlemeRengi = AbakusDersModeli.yesil
        withAnimation(.easeOut(duration: 0.7)) {
            ilerleme = sinirli
        }
        if dogru {
            withAnimation(.linear(duration: 0.35)) {
                ilerlemeRengi = AbakusDersModeli.acikSari
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { [weak self] in
                guard let self = self else { return }
                withAnimation(.linear(duration: 0.35)) {
                    self.ilerlemeRengi = AbakusDersModeli.yesil
                }
            }
        } else {
            withAnimation(.easeOut(duration: 0.7)) {
                ilerlemeRengi = AbakusDersModeli.koyuKirmizi
            }
        }
    }

    /// Android: showResultPanel — panel 200 ms'de alttan kayarak ve belirerek gelir; kontrol
    /// düğmesi ve abaküs dokunmaya kapanır.
    private func paneliGoster(_ sonuc: AbakusKontrolSonucu) {
        let yeniPanel: AbakusSonucPaneli = sonuc.dogru ? .dogru : .yanlis
        if sonuc.dogru {
            hizmetler.sesCal(.dogru)
            hizmetler.titret(.dogru)
            if sonuc.konfeti {
                konfeti = true
            }
        } else {
            hizmetler.sesCal(.yanlis)
            hizmetler.titret(.yanlis)
            cevapMetni = sonuc.cevapMetni
            dogruCevapMetni = sonuc.dogruCevapMetni
            analizDugmesiniHazirla()
        }
        ortuAcik = true
        kontrolEtkin = false
        panelAnimasyonda = true
        withAnimation(.easeInOut(duration: 0.2)) {
            panel = yeniPanel
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            self?.panelAnimasyonda = false
        }
    }

    /// Android: continueButton / okayButton, ACTION_UP dalı.
    func panelDugmesiDokunuldu() {
        guard let akis = akis, panel != .yok, !panelAnimasyonda else { return }
        panelAnimasyonda = true
        let dogruydu: Bool = panel == .dogru
        let soruVar: Bool = akis.devam()
        abakus.sifirla(animasyonlu: true)
        ortuAcik = false
        analizGorunur = false

        // Kontrol düğmesi doğrudan sonra 0,2 sn, yanlıştan sonra 0,7 sn kapalı kalır.
        let gecikme: Double = dogruydu ? 0.2 : 0.7
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme) { [weak self] in
            self?.kontrolEtkin = true
        }

        withAnimation(.easeIn(duration: 0.2)) {
            panel = .yok
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            self?.panelAnimasyonda = false
        }

        if soruVar {
            soruyuGoster()
        } else {
            dersiBitir()
        }
    }

    /// Android: finishLesson + sonuç ekranı seçimi. Ders sonu anketi (QuestionPanelFragment) güncel
    /// Android'de yok; sonuç doğrudan dışarı verilir. Kapı (`dersBitti`): Android'de `lessonFinishStarted`.
    private func dersiBitir() {
        guard let akis = akis, !dersBitti else { return }
        dersBitti = true
        sureyiDurdur()
        let b = akis.bitis(worstCupTime: Int32(baslangic.enKotuKupaSuresi))
        let yon = AbakusDersYonlendirmesi(rawValue: Int(b.yonlendirme)) ?? .dersSonucu
        let sonuc = AbakusDersSonucu(
            partId: baslangic.partId,
            haritaSirasi: baslangic.haritaSirasi,
            dersTuru: baslangic.dersTuru,
            yonlendirme: yon,
            dogruSayisi: Int(b.correctAnswers),
            soruSayisi: Int(b.totalQuestions),
            basariOrani: b.successRate,
            dersPuani: Int(b.dersPuani),
            sure: b.time,
            saniye: Int(b.seconds),
            enKotuKupaSuresi: Int(b.worstCupTime),
            carpan: b.carpan,
            toplamPuan: Int(b.toplamPuan),
            soruSuresiMs: Int(Date().timeIntervalSince(girisAni) * 1000.0)
        )
        hizmetler.ilerlemeyiKaydet(sonuc: sonuc)
        bitti(sonuc)
    }

    // MARK: Hata analizi

    /// Android: bindReviewButton — yalnızca toplama/çıkarma sorusunda ve analiz yapılabiliyorsa düğme
    /// görünür. Cevap ve abaküs değer kaydı bu anki hâliyle alınır.
    private func analizDugmesiniHazirla() {
        analizGorunur = false
        guard let akis = akis, let baglanti = sobiAnaliz else { return }
        guard let ilk = akis.ilkSayiMetni().flatMap({ Int($0) }),
              let ikinci = akis.ikinciSayiMetni().flatMap({ Int($0) }),
              let isaret = akis.islecMetni() else {
            return
        }
        if isaret != "+" && isaret != "-" { return }
        let sayilar: [Int] = [ilk, isaret == "-" ? -ikinci : ikinci]
        if !baglanti.yapilabilir(sayilar) { return }
        analizSayilari = sayilar
        analizCevabi = abakus.anlikDeger
        analizKaydi = hataKaydi
        analizGorunur = true
    }

    /// Android: incorrectSobiAnalyze tıklaması. Sandık dersinde analiz açıkken süre durur (süre sandık
    /// puanına giriyor), pencere kapanınca kaldığı yerden devam eder.
    func sobiAnaliziDokunuldu() {
        guard let baglanti = sobiAnaliz, analizGorunur, !analizAcik else { return }
        analizAcik = true
        let calisiyor: Bool = zamanlayici != nil
        if calisiyor {
            sureyiDurdur()
        }
        let istek = AbakusSobiAnalizIstegi(
            sayilar: analizSayilari,
            cevap: analizCevabi,
            kayit: analizKaydi,
            isaretler: [],
            dersBasligi: baslangic.baslik,
            kapandi: { [weak self] in
                DispatchQueue.main.async {
                    self?.analizKapandi(sureDevamEtsin: calisiyor)
                }
            }
        )
        baglanti.istendi(istek)
    }

    private func analizKapandi(sureDevamEtsin: Bool) {
        analizAcik = false
        if sureDevamEtsin && !dersBitti && zamanlayici == nil {
            sureyiDevamEttir()
        }
    }

    /// Abaküs değeri değişince (AbakusGorunumu.degerDegisti): hata analizi için kayda eklenir.
    /// Android: onValueChanged — sıfırlama sürerken ve aynı değer üst üste gelince eklenmez.
    func abakusDegeriDegisti(_ deger: Int) {
        if abakus.sifirlaniyor { return }
        if hataKaydi.last == deger { return }
        if hataKaydi.count >= AbakusDersModeli.hataKaydiEnCok { return }
        hataKaydi.append(deger)
    }

    // MARK: Kapatma

    /// Android: closeFragment — ekran 400 ms'de SOLA kayar (ders akışındaki bütün ekranlar gibi; eskiden
    /// 300 ms ve sağa), sonra kapanır.
    func kapatDokunuldu(_ kapat: @escaping () -> Void) {
        if kapaniyor { return }
        sureyiDurdur()
        withAnimation(.easeInOut(duration: 0.4)) {
            kapaniyor = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            kapat()
        }
    }

    private func bildir(_ metin: String) {
        bildirimSayaci += 1
        let no = bildirimSayaci
        bildirim = metin
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            guard let self = self else { return }
            if self.bildirimSayaci == no {
                self.bildirim = nil
            }
        }
    }
}

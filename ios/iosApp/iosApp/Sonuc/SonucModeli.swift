import Foundation
import Shared
import SwiftUI

// Sonuç ekranının durumu ve açılış animasyonlarının sırası.
//
// Ortak Kotlin koduna yapılan BÜTÜN çağrılar bu dosyadadır (SonucEkraniKoprusu ve
// ChestResultStarTracker). Görünümler yalnızca buradaki yayınlanan alanları okur.
// Buradaki Swift kodu yalnızca zamanlamadır (Android: ValueAnimator / Handler zincirleri);
// puan, oran, eşik, renk seçimi ve metin kuralları ortak koddan gelir.

/// Gecikmeli işler ve sayaç animasyonları; ekran kapanınca hepsi iptal edilir.
final class SonucZamanlayici {
    private var sayaclar: [Timer] = []
    private var isler: [DispatchWorkItem] = []
    private var durduruldu = false

    /// Android: Handler.postDelayed.
    func sonra(_ saniye: Double, _ yapilacak: @escaping () -> Void) {
        if durduruldu { return }
        let isParcasi = DispatchWorkItem { [weak self] in
            guard let self = self, !self.durduruldu else { return }
            yapilacak()
        }
        isler.append(isParcasi)
        DispatchQueue.main.asyncAfter(deadline: .now() + saniye, execute: isParcasi)
    }

    /// Android: ValueAnimator + DecelerateInterpolator. `adim` 0…1 arası eğrilenmiş oranla çağrılır.
    func say(sure: Double, adim: @escaping (Double) -> Void, bitince: @escaping () -> Void) {
        if durduruldu { return }
        let baslangic: TimeInterval = ProcessInfo.processInfo.systemUptime
        let sayac = Timer.scheduledTimer(withTimeInterval: 1.0 / 60.0, repeats: true) { [weak self] zamanlayici in
            guard let self = self, !self.durduruldu else {
                zamanlayici.invalidate()
                return
            }
            let gecen: TimeInterval = ProcessInfo.processInfo.systemUptime - baslangic
            let ham: Double = sure > 0 ? min(1.0, gecen / sure) : 1.0
            // DecelerateInterpolator (çarpan 1): 1 - (1 - t)^2
            let ters: Double = 1.0 - ham
            let egri: Double = 1.0 - ters * ters
            adim(egri)
            if ham >= 1.0 {
                zamanlayici.invalidate()
                bitince()
            }
        }
        sayaclar.append(sayac)
    }

    func durdur() {
        durduruldu = true
        for sayac in sayaclar { sayac.invalidate() }
        for isParcasi in isler { isParcasi.cancel() }
        sayaclar.removeAll()
        isler.removeAll()
    }
}

final class SonucModeli: ObservableObject {
    // MARK: Sabit değerler (ortak koddan)

    let girdi: SonucGirdisi
    /// Başarı yüzdesi, tam sayıya kırpılmış (Android: successRate.toInt()).
    let basariYuzdesi: Int
    /// Ders puanı = başarı yüzdesi × 5.
    let dersPuani: Int
    /// Ders sonu Sobi sahnesi: `BunnyMascotView.Emote` enum adı. Başarıda torbadan çekilir
    /// (LessonEndMascot.nextSuccess), başarısızlıkta hep "DETERMINED" (Android: LessonEndMascot).
    let maskotSahnesi: String
    /// Başarısız sonuçtaki neden satırı.
    let aciklama: String
    let puanTavani: Int
    let ikiYildizEsigi: Int
    let sureSaniye: Int
    /// Üç yıldız işaretinin çubuk üzerindeki yatay oranı (0…1).
    let yildizOranlari: [CGFloat]

    // MARK: Yayınlanan durum

    /// Sobi sahnesi başladı mı (ekran girişinden 450 ms sonra; Android: ENTER_DELAY_MS).
    @Published var maskotBasladi = false
    @Published var puanKutusuGorunur = false
    @Published var basariKutusuGorunur = false
    @Published var puanMetni = "0"
    @Published var basariMetni = "0%"
    @Published var dugmeGorunur = false
    @Published var dugmeEtkin = false

    // Sandık sonucu
    @Published var buyukPuan: Int = 0
    @Published var cubukOrani: CGFloat = 0
    @Published var cubukRengi: Color = SonucStil.renk(0x8CC63F)
    @Published var sureKutusuGorunur = false
    @Published var sureMetni = "0:00"
    @Published var carpanMetni = "x0.00"
    @Published var balonGorunur = false
    @Published var balonUcusta = false
    @Published var parlama = false
    @Published var puanZipla = false
    @Published var yildizYandi: [Bool] = [false, false, false]

    // MARK: Özel

    private let ses: SonucSesCalar
    private let zaman = SonucZamanlayici()
    private let kopru = SonucEkraniKoprusu.shared
    private let yildizIzleyici: ChestResultStarTracker
    private var gosterilenPuan: Int = 0
    private var puanKesinlesti = false
    private var basladi = false
    private var devamEdildi = false

    // Android sabitleri (LessonResult.companion)
    private let kutuBelirmeSuresi: Double = 0.35
    private let saymaSuresi: Double = 1.0
    private let dugmeBelirmeSuresi: Double = 0.3
    private let adimArasi: Double = 0.1
    /// ChestResult: her sayma aşaması 2000 ms.
    private let sandikSaymaSuresi: Double = 2.0

    init(girdi: SonucGirdisi, ses: SonucSesCalar) {
        self.girdi = girdi
        self.ses = ses
        // Bütün alanlar atanmadan `self.kopru` okunamaz; init içinde yerel başvuru kullanılır.
        let kopru = SonucEkraniKoprusu.shared

        let oran: Float = kopru.basariOrani(dogru: Int32(girdi.dogruSayisi), toplam: Int32(girdi.toplamSoru))
        self.basariYuzdesi = Int(oran)
        self.dersPuani = Int(kopru.dersPuani(basariOrani: oran))
        // Sobi sahnesi: başarısızlıkta hep kararlı hâl; başarıda (ders ve sandık) torbadan sıradaki.
        let sahne: String
        switch girdi.tur {
        case .dersBasarisiz, .sandikBasarisiz:
            sahne = LessonEndMascotLogic.shared.FAILURE
        case .dersBasarili, .sandikSonucu:
            sahne = SonucModeli.basariSahnesiCek()
        }
        self.maskotSahnesi = sahne
        // Android: appString(R.string.ui_puan_500_den_dusuk / ui_basari_orani_75_ten_dusuk).
        self.aciklama = girdi.tur == .sandikBasarisiz
            ? Metin.al("ui_puan_500_den_dusuk")
            : Metin.al("ui_basari_orani_75_ten_dusuk")

        // Ortak kuralın bölme yapabilmesi için tavan en az 1 olmalı (Android: resolveScoreCap en az 1 döner).
        let tavan: Int = max(1, girdi.puanTavani)
        let ikiYildiz: Int = max(1, girdi.ikiYildizEsigi)
        self.puanTavani = tavan
        self.ikiYildizEsigi = ikiYildiz
        self.sureSaniye = Int(kopru.sureSaniye(sureMetni: girdi.sure))
        self.yildizIzleyici = ChestResultStarTracker(cupPoint2Threshold: Int32(ikiYildiz), scoreCap: Int32(tavan))

        var oranlar: [CGFloat] = []
        for sira in 1...3 {
            let esik: Int32 = kopru.yildizEsigi(sira: Int32(sira), ikiYildizEsigi: Int32(ikiYildiz), puanTavani: Int32(tavan))
            let yatay: Float = kopru.yildizOrani(esik: esik, puanTavani: Int32(tavan))
            oranlar.append(CGFloat(yatay))
        }
        self.yildizOranlari = oranlar
    }

    // MARK: Yaşam döngüsü

    /// Ekran göründüğünde bir kez.
    func basla() {
        if basladi { return }
        basladi = true
        // Ekran sağdan kayarak girerken Sobi bekler; giriş bitince sahne başlar.
        let maskotGecikmesi: Double = Double(LessonEndMascotLogic.shared.ENTER_DELAY_MS) / 1000.0
        zaman.sonra(maskotGecikmesi) { [weak self] in
            self?.maskotBasladi = true
        }
        switch girdi.tur {
        case .dersBasarili:
            ses.sonucSesiCal()
            dersAkisiniBaslat()
        case .dersBasarisiz, .sandikBasarisiz:
            // LessonResultFalse: animasyon yok, değerler doğrudan yazılır.
            puanMetni = String(dersPuani)
            basariMetni = "\(basariYuzdesi)%"
            puanKutusuGorunur = true
            basariKutusuGorunur = true
            dugmeGorunur = true
            dugmeEtkin = true
        case .sandikSonucu:
            sandikAkisiniBaslat()
        }
    }

    func durdur() {
        zaman.durdur()
    }

    /// Düğmeye çift basmayı engeller (Android: claimHandled). İlk basışta `true` döner.
    func devamaIzinVar() -> Bool {
        if devamEdildi || !dugmeEtkin { return false }
        devamEdildi = true
        return true
    }

    // MARK: LessonResult.playResultRevealSequence

    private func dersAkisiniBaslat() {
        puanKutusuGorunur = true
        zaman.sonra(kutuBelirmeSuresi + adimArasi) { [weak self] in
            self?.dersPuaniniSay()
        }
    }

    private func dersPuaniniSay() {
        let hedef: Int = dersPuani
        zaman.say(sure: saymaSuresi, adim: { [weak self] oran in
            self?.puanMetni = String(SonucModeli.araDeger(0, hedef, oran))
        }, bitince: { [weak self] in
            guard let self = self else { return }
            self.zaman.sonra(self.adimArasi) { [weak self] in
                self?.basariKutusunuGoster()
            }
        })
    }

    private func basariKutusunuGoster() {
        basariKutusuGorunur = true
        zaman.sonra(kutuBelirmeSuresi + adimArasi) { [weak self] in
            self?.basariyiSay()
        }
    }

    private func basariyiSay() {
        let hedef: Int = basariYuzdesi
        zaman.say(sure: saymaSuresi, adim: { [weak self] oran in
            self?.basariMetni = "\(SonucModeli.araDeger(0, hedef, oran))%"
        }, bitince: { [weak self] in
            guard let self = self else { return }
            self.zaman.sonra(self.adimArasi) { [weak self] in
                self?.dugmeyiGoster()
            }
        })
    }

    private func dugmeyiGoster() {
        dugmeGorunur = true
        zaman.sonra(dugmeBelirmeSuresi) { [weak self] in
            self?.dugmeEtkin = true
        }
    }

    // MARK: ChestResult.startRevealSequence

    private func sandikAkisiniBaslat() {
        yildizIzleyici.reset()
        cubuguGuncelle(puan: 0, onceki: 0)
        if basariYuzdesi <= 0 {
            ikinciAsama()
            return
        }
        let puanHedefi: Int = dersPuani
        let basariHedefi: Int = basariYuzdesi
        zaman.say(sure: sandikSaymaSuresi, adim: { [weak self] oran in
            guard let self = self else { return }
            self.puaniGoster(SonucModeli.araDeger(0, puanHedefi, oran))
            self.basariMetni = "\(SonucModeli.araDeger(0, basariHedefi, oran))%"
        }, bitince: { [weak self] in
            self?.ikinciAsama()
        })
    }

    /// Süre kutusu belirir, çarpan balonu üstüne konur; süre ve çarpan 0'dan sayılır.
    private func ikinciAsama() {
        sureKutusuGorunur = true
        balonGorunur = true
        let sureHedefi: Int = sureSaniye
        let carpanHedefi: Double = Double(girdi.carpan)
        zaman.say(sure: sandikSaymaSuresi, adim: { [weak self] oran in
            guard let self = self else { return }
            let saniye: Int = SonucModeli.araDeger(0, sureHedefi, oran)
            self.sureMetni = self.kopru.sureMetni(saniye: Int32(saniye))
            self.carpanMetni = SonucModeli.carpanYazisi(carpanHedefi * oran)
        }, bitince: { [weak self] in
            self?.balonuPuanaUcur()
        })
    }

    /// animateBubbleToScore: balon 650 ms'de büyük puan yazısının üstüne uçar.
    private func balonuPuanaUcur() {
        if puanKesinlesti { return }
        balonUcusta = true
        zaman.sonra(0.65) { [weak self] in
            self?.carpmaVeSonPuan()
        }
    }

    /// playScoreImpactAndFinalize: 100 ms'lik parlama, sonra balon kaybolur ve son puana sayılır.
    private func carpmaVeSonPuan() {
        if puanKesinlesti { return }
        parlama = true
        zaman.sonra(0.1) { [weak self] in
            guard let self = self, !self.puanKesinlesti else { return }
            self.parlama = false
            self.balonGorunur = false
            self.sonPuanaSay()
        }
    }

    /// animateFinalScoreTransition
    private func sonPuanaSay() {
        if puanKesinlesti { return }
        let baslangic: Int = gosterilenPuan
        let hedef: Int = girdi.toplamPuan
        if baslangic == hedef {
            puaniKesinlestir()
            return
        }
        zaman.say(sure: sandikSaymaSuresi, adim: { [weak self] oran in
            self?.puaniGoster(SonucModeli.araDeger(baslangic, hedef, oran))
        }, bitince: { [weak self] in
            self?.puaniKesinlestir()
        })
    }

    /// finalizeScoreDisplay
    private func puaniKesinlestir() {
        if puanKesinlesti { return }
        puanKesinlesti = true
        parlama = false
        balonGorunur = false
        puaniGoster(girdi.toplamPuan)
        // playFinalScorePop: 260 ms'de 1 → 1.1 → 1
        puanZipla = true
        zaman.sonra(0.13) { [weak self] in
            self?.puanZipla = false
        }
        // playClaimButtonAppear: düğme hemen etkin, 300 ms'de belirir.
        dugmeGorunur = true
        dugmeEtkin = true
    }

    private func puaniGoster(_ puan: Int) {
        buyukPuan = puan
        cubuguGuncelle(puan: puan, onceki: gosterilenPuan)
        gosterilenPuan = puan
    }

    /// updateScoreProgressBar + maybeTriggerStarMarkers
    private func cubuguGuncelle(puan: Int, onceki: Int) {
        let tavan = Int32(puanTavani)
        let oran: Float = kopru.cubukOrani(puan: Int32(puan), puanTavani: tavan)
        cubukOrani = CGFloat(oran)

        let karisim: ScoreBarColorBlend = kopru.cubukRengi(puan: Int32(puan), oncekiPuan: Int32(onceki), puanTavani: tavan)
        cubukRengi = SonucStil.karisim(karisim.fromColor, karisim.toColor, oran: Double(karisim.fraction))

        let gecilenler: [KotlinInt] = yildizIzleyici.onScoreChanged(previousScore: Int32(onceki), currentScore: Int32(puan))
        for kutulu in gecilenler {
            let kademe: Int = kutulu.intValue
            if kademe >= 1 && kademe <= 3 {
                yildizYandi[kademe - 1] = true
                ses.yildizSesiCal(hiz: kopru.yildizSesHizi(kademe: Int32(kademe)))
            }
        }
    }

    // MARK: Yardımcılar

    private static let torbaAnahtari: String = "lesson_end_mascot.bag"
    private static let sonSahneAnahtari: String = "lesson_end_mascot.last"

    /// LessonEndMascot.nextSuccess: torba cihazda saklanır (Android: SharedPreferences
    /// "lesson_end_mascot" → "bag", "last"); ders ve sandık sonucu aynı torbayı paylaşır.
    /// Çekim kuralı ortak koddadır (LessonEndMascotLogic); burada yalnızca saklama vardır.
    private static func basariSahnesiCek() -> String {
        let depo: UserDefaults = UserDefaults.standard
        let torba: String? = depo.string(forKey: torbaAnahtari)
        let sonSahne: String? = depo.string(forKey: sonSahneAnahtari)
        let cekim: LessonEndMascotDraw = LessonEndMascotLogic.shared.nextSuccess(storedBag: torba, storedLast: sonSahne)
        depo.set(cekim.bag, forKey: torbaAnahtari)
        depo.set(cekim.last, forKey: sonSahneAnahtari)
        return cekim.emote
    }

    /// ValueAnimator.ofInt(a, b) ara değeri (kesirli kısım atılır).
    static func araDeger(_ baslangic: Int, _ bitis: Int, _ oran: Double) -> Int {
        let fark: Double = Double(bitis - baslangic)
        return Int(Double(baslangic) + oran * fark)
    }

    /// Android: "x" + String.format("%.2f", değer) — cihaz yereline göre ondalık ayırıcı.
    static func carpanYazisi(_ deger: Double) -> String {
        return String(format: "x%.2f", locale: Metin.yerel, deger)
    }
}

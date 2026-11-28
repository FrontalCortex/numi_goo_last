import Combine
import Foundation
import SwiftUI

// Öğretici ekranının durumu. Kurallar ortak Kotlin kodundadır (OgreticiOynatici, OgreticiKoprusu
// üzerinden); burada yalnızca oynatıcının istediği işler (OgreticiIs) SIRAYLA yürütülür:
// metin (daktilo), ses, boncuk animasyonları, abaküs sıfırlama / sayı yazma, sonuç panelleri.
//
// Android karşılıkları: TutorialFragment.showStep (adımı göster), showTextWithTypewriter /
// handleTutorialTapToAdvance (daktilo), isAnyAnimationRunning (`mesgul`), resetAbacus (400 ms
// dokunma kilidi), showResultPanelForOptions / dismissIncorrectResultPanel (paneller),
// closeFragment (400 ms sola kayma), skipTutorialButton, devamButton.

/// Açık sonuç paneli.
enum OgreticiSonucPaneli: Equatable {
    case dogru
    case yanlis
}

final class OgreticiGorunumModeli: ObservableObject {
    @Published private(set) var adim: OgreticiAdimVerisi = OgreticiAdimVerisi.bos
    @Published private(set) var sahne: OgreticiSahneVerisi = OgreticiSahneVerisi.bos
    /// Daktiloda görünen harf sayısı (Character).
    @Published private(set) var gorunenHarf: Int = 0
    @Published private(set) var sonuc: OgreticiSonucPaneli? = nil
    /// Kısa bildirim (Android Toast).
    @Published private(set) var uyari: String? = nil
    /// Çıkış kayması (Android closeFragment: 400 ms sola).
    @Published private(set) var kapaniyor: Bool = false
    /// Seçenek panelinde işaretli şıklar (seçenek paneli paketi kullanır).
    @Published var secilenSecenekler: Set<Int> = []

    let abakus = AbakusDenetleyici()
    let baslangic: OgreticiBaslangici
    /// false: öğreticinin içeriği taşınmamış; ekran yer tutucuyu gösterir.
    let icerikVar: Bool

    private let kopru: OgreticiKoprusu?
    private let ses: OgreticiSesCalici
    private let bittiBildir: (OgreticiSonucu) -> Void
    private let kapatBildir: () -> Void

    private var kuyruk: [OgreticiIs] = []
    /// Kuyruk bir işin bitmesini bekliyor (BEKLE ya da bekleyen SAYI_YAZ).
    private var kuyrukDurdu: Bool = false
    /// Boncuk animasyonları başlayınca kısa süre dokunma yutulur (Android currentAnimations.isAnimating).
    private var hareketKilidiBitisi: Date = Date.distantPast
    private var sayiYaziliyor: Bool = false
    /// Android resetAbacus: 400 ms boyunca dokunmalar yutulur.
    private var dokunmaKilidiBitisi: Date = Date.distantPast
    private var daktiloZamanlayici: Timer? = nil
    /// Geçerli adımın harf aralığı (sn) ve arka plana geçerken daktilonun durdurulduğu bilgisi.
    private var daktiloAraligi: Double = 0.05
    private var daktiloYasamDongusuyleDurdu: Bool = false
    private var uyariIsi: DispatchWorkItem? = nil
    private var basladi: Bool = false
    private var cikildi: Bool = false

    init(
        baslangic: OgreticiBaslangici,
        ses: OgreticiSesCalici,
        bitti: @escaping (OgreticiSonucu) -> Void,
        kapat: @escaping () -> Void
    ) {
        self.baslangic = baslangic
        self.ses = ses
        self.bittiBildir = bitti
        self.kapatBildir = kapat
        let kurulan: OgreticiKoprusu? = OgreticiKoprusu(
            numara: baslangic.ogreticiNo,
            ilkAcilis: baslangic.ilkAcilis,
            baslangicAdimi: baslangic.baslangicAdimi
        )
        self.kopru = kurulan
        self.icerikVar = kurulan != nil
        if let k = kurulan {
            adim = k.adimVerisi()
            sahne = k.sahne()
        }
    }

    deinit {
        daktiloZamanlayici?.invalidate()
        uyariIsi?.cancel()
    }

    // MARK: Yaşam döngüsü

    /// Ekran göründü: ilk adım bir sonraki turda gösterilir (abaküs görünümü bağlansın diye).
    func gorundu() {
        if basladi {
            // Görünüm kaybolup yeniden göründü: `kayboldu` daktiloyu durdurmuştu, kaldığı harften sürer
            // (yoksa metin yarım/boş kalırdı; `basladi` ilk gösterimi bir daha çalıştırmaz).
            YonetimGunlugu.yaz("ogretici yeniden gorundu (harf \(gorunenHarf)/\(adim.metin.count))")
            let yarim: Bool = gorunenHarf < adim.metin.count
            if !cikildi && adim.daktilo && yarim && daktiloZamanlayici == nil && !daktiloYasamDongusuyleDurdu {
                daktiloZamanlayiciyiKur(aralik: daktiloAraligi)
            }
            return
        }
        basladi = true
        YonetimGunlugu.yaz("ogretici gorundu (no \(baslangic.ogreticiNo), ilk acilis \(baslangic.ilkAcilis))")
        DispatchQueue.main.async { [weak self] in
            guard let self = self, let k = self.kopru else { return }
            self.calistir(k.basla())
        }
    }

    func kayboldu() {
        YonetimGunlugu.yaz("ogretici kayboldu (harf \(gorunenHarf)/\(adim.metin.count))")
        daktiloyuDurdur()
        ses.durdur()
    }

    // MARK: Durum

    /// Daktilo sürüyor mu (Android typewriterRunnable != null).
    var daktiloSuruyor: Bool {
        return gorunenHarf < adim.metin.count
    }

    /// Android isAnyAnimationRunning + writeAnswerNumber + resetAbacus kilidi.
    var mesgul: Bool {
        if kuyrukDurdu || sayiYaziliyor {
            return true
        }
        let simdi = Date()
        if simdi < dokunmaKilidiBitisi || simdi < hareketKilidiBitisi {
            return true
        }
        return abakus.ogreticiMesgul
    }

    // MARK: Kullanıcı olayları

    /// Ekrana ya da kilitli abaküse dokunuldu.
    func ekranaDokunuldu() {
        guard let k = kopru, !cikildi else { return }
        calistir(k.dokunuldu(mesgul: mesgul, daktiloSuruyor: daktiloSuruyor))
    }

    /// Geri düğmesi. Seçenek paneli açıkken Android'de çalışmaz (optionsInteractionLocked).
    func geriBasildi() {
        guard let k = kopru, !cikildi else { return }
        let secenekKilidi: Bool = adim.gecerliSecenekVar
        calistir(k.geri(mesgul: mesgul || secenekKilidi))
    }

    /// "Kontrol Et" (abaküslü soru).
    func kontrolEt() {
        guard let k = kopru, !cikildi, !sayiYaziliyor else { return }
        calistir(k.kontrol(deger: abakus.anlikDeger))
    }

    /// Seçenek panelindeki "Kontrol Et".
    func secenekleriKontrolEt() {
        guard let k = kopru, !cikildi else { return }
        calistir(k.secenekKontrol(secilenler: Array(secilenSecenekler)))
    }

    /// Doğru panelindeki "Devam et".
    func dogruDevam() {
        guard let k = kopru, !cikildi else { return }
        calistir(k.dogruDevam())
    }

    /// Yanlış panelindeki "Tekrar Dene".
    func tekrarDene() {
        guard let k = kopru, !cikildi else { return }
        calistir(k.yanlisTamam())
    }

    /// "Eğitimi atla": doğrudan derse (Android tutorialSkipSetup). Seçenek paneli açıkken çalışmaz.
    func atla() {
        if cikildi || adim.gecerliSecenekVar { return }
        bitir(.atlandi)
    }

    /// Son adımdaki "Devam Et" (Android devametFragment → devamButton).
    func derseGec() {
        bitir(.derseGec)
    }

    /// Çıkış düğmesi (Android closeFragment: 400 ms sola kayar, sonra kapanır).
    func cik() {
        if cikildi { return }
        cikildi = true
        kayboldu()
        withAnimation(.easeInOut(duration: 0.4)) {
            kapaniyor = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { [weak self] in
            self?.kapatBildir()
        }
    }

    private func bitir(_ sonuc: OgreticiSonucu) {
        if cikildi { return }
        cikildi = true
        kayboldu()
        bittiBildir(sonuc)
    }

    // MARK: İş kuyruğu

    private func calistir(_ yeni: [OgreticiIs]) {
        if yeni.isEmpty { return }
        kuyruk.append(contentsOf: yeni)
        ilerlet()
    }

    private func ilerlet() {
        while !kuyrukDurdu && !kuyruk.isEmpty && !cikildi {
            let siradaki: OgreticiIs = kuyruk.removeFirst()
            yurut(siradaki)
        }
        sahneyiYenile()
    }

    private func yurut(_ is_: OgreticiIs) {
        switch is_ {
        case .adimGoster(_, _, let hareketler):
            adimiGoster(hareketler)
        case .metniTamamla:
            daktiloyuBitir()
        case .abakusSifirla:
            abakus.sifirla(animasyonlu: true)
            dokunmaKilidiBitisi = Date().addingTimeInterval(0.4)
        case .sayiYaz(let deger, let bekle):
            sayiYaz(deger, bekle: bekle)
        case .degerKur(let deger):
            abakus.degeriAyarla(deger)
        case .bekle(let ms):
            kuyrukDurdu = true
            let saniye: Double = Double(ms) / 1000.0
            DispatchQueue.main.asyncAfter(deadline: .now() + saniye) { [weak self] in
                guard let self = self else { return }
                self.kuyrukDurdu = false
                self.ilerlet()
            }
        case .sonucGoster(let dogru):
            ses.sonucSesiniCal(dogru: dogru)
            withAnimation(.easeInOut(duration: 0.2)) {
                sonuc = dogru ? OgreticiSonucPaneli.dogru : OgreticiSonucPaneli.yanlis
            }
        case .sonucGizle:
            withAnimation(.easeIn(duration: 0.2)) {
                sonuc = nil
            }
        case .kapat:
            cik()
        case .secimUyarisi:
            uyariGoster(Metin.al("tutorial_options_pick_first"))
        }
    }

    private func sayiYaz(_ deger: Int, bekle: Bool) {
        sayiYaziliyor = true
        if bekle {
            kuyrukDurdu = true
        }
        abakus.ogreticiSayiyiYaz(deger) { [weak self] in
            guard let self = self else { return }
            self.sayiYaziliyor = false
            if bekle {
                self.kuyrukDurdu = false
                self.ilerlet()
            }
        }
    }

    /// Android showStep: metin (daktilo), ses, soru, boncuk animasyonları. Sahne kuyruk bitince yenilenir.
    private func adimiGoster(_ hareketler: [OgreticiHareket]) {
        guard let k = kopru else { return }
        let veri: OgreticiAdimVerisi = k.adimVerisi()
        adim = veri
        secilenSecenekler = []
        daktiloyuBaslat(veri)
        YonetimGunlugu.yaz("ogretici adimi gosterildi (\(veri.metin.count) harf, daktilo \(veri.daktilo), ses \(veri.sesAnahtari ?? "-"))")
        // Ses çalıcısı beklemeden döner (gerçek çalıcı işi ses kuyruğunda yapar; OgreticiSesCalar.swift).
        ses.adimSesiniCal(anahtar: veri.sesAnahtari, uzakYol: veri.uzakSes)
        if hareketler.isEmpty { return }
        // Süren animasyonu abaküs kendisi bildirir (ogreticiMesgul); bir animasyonun yerini başkası
        // alırsa ilkinin bitişi hiç çağrılmaz, bu yüzden sayaç değil süre kilidi tutulur
        // (Android BeadAnimation: 300 ms).
        hareketKilidiBitisi = Date().addingTimeInterval(0.35)
        for h in hareketler {
            abakus.ogreticiBoncukOynat(cubuk: h.cubuk, ust: h.ust, sira: h.sira, tur: h.tur) {}
        }
    }

    private func sahneyiYenile() {
        guard let k = kopru else { return }
        let yeni: OgreticiSahneVerisi = k.sahne()
        if yeni == sahne { return }
        // Android: odak AnimateMargin 200 ms, AnimateSize 400 ms; devam düğmesi 300 ms.
        withAnimation(.easeInOut(duration: 0.3)) {
            sahne = yeni
        }
    }

    /// Başlangıçtan şu anki adıma kadar biriken yerlesim değişiklikleri (görsel alanı kullanır).
    func birikmisYerlesimler() -> [String] {
        guard let k = kopru else { return [] }
        return k.yerlesimlerAdimaKadar()
    }

    // MARK: Daktilo (Android showTextWithTypewriter)

    private func daktiloyuBaslat(_ veri: OgreticiAdimVerisi) {
        daktiloyuDurdur()
        daktiloYasamDongusuyleDurdu = false
        let toplam: Int = veri.metin.count
        if !veri.daktilo || toplam == 0 {
            gorunenHarf = toplam
            return
        }
        gorunenHarf = 0
        let aralik: Double = Double(max(veri.daktiloHiziMs, 1)) / 1000.0
        daktiloAraligi = aralik
        daktiloZamanlayiciyiKur(aralik: aralik)
    }

    private func daktiloZamanlayiciyiKur(aralik: Double) {
        let zamanlayici = Timer(timeInterval: aralik, repeats: true) { [weak self] _ in
            self?.daktiloAdimi()
        }
        RunLoop.main.add(zamanlayici, forMode: .common)
        daktiloZamanlayici = zamanlayici
    }

    /// Uygulama arka plana geçiyor (Android TutorialFragment.onPause): daktilo kaldığı harfte durur.
    func arkaPlanaGecildi() {
        YonetimGunlugu.yaz("ogretici arka plana gecti (harf \(gorunenHarf)/\(adim.metin.count))")
        if daktiloZamanlayici == nil { return }
        daktiloyuDurdur()
        daktiloYasamDongusuyleDurdu = true
    }

    /// Uygulama öne döndü (Android onResume → resumeTypewriter): daktilo kaldığı harften sürer.
    func onPlanaDonuldu() {
        if !daktiloYasamDongusuyleDurdu { return }
        daktiloYasamDongusuyleDurdu = false
        if cikildi || daktiloZamanlayici != nil || gorunenHarf >= adim.metin.count { return }
        daktiloZamanlayiciyiKur(aralik: daktiloAraligi)
    }

    private func daktiloAdimi() {
        let toplam: Int = adim.metin.count
        if gorunenHarf >= toplam {
            daktiloyuDurdur()
            YonetimGunlugu.yaz("ogretici daktilosu bitti (\(toplam) harf)")
            return
        }
        gorunenHarf += 1
    }

    private func daktiloyuBitir() {
        daktiloyuDurdur()
        gorunenHarf = adim.metin.count
    }

    private func daktiloyuDurdur() {
        daktiloZamanlayici?.invalidate()
        daktiloZamanlayici = nil
    }

    // MARK: Bildirim

    private func uyariGoster(_ metin: String) {
        uyariIsi?.cancel()
        withAnimation(.easeInOut(duration: 0.2)) {
            uyari = metin
        }
        let is_ = DispatchWorkItem { [weak self] in
            withAnimation(.easeInOut(duration: 0.2)) {
                self?.uyari = nil
            }
        }
        uyariIsi = is_
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0, execute: is_)
    }
}

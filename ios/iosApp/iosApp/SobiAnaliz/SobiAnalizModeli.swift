import Foundation
import Shared
import SwiftUI

// "Sobi ile analiz et" penceresinin görünüm modeli.
//
// Android karşılığı: MistakeReviewDialog (analiz kipi: newInstance). Kararların hepsi ortak koddaki
// SobiAnalizOturumu'ndadır (sayfalar, Kontrol Et, balonlar, örnek); burada yalnızca Android'deki
// zamanlama (yanlışta 700 ms sonra adımın başına dönüş, abaküs hareketinden 500 ms sonra kayıt,
// doğruda 900 ms sonra sonraki adım, sütun parlaması hareketten 650 ms sonra), abaküs oynatma
// (SobiAbacusPlayer → SobiAbakusOynatici planı) ve "meşgul" durumu (holdAbacus) tutulur.
// Ortak koda yapılan bütün çağrılar bu dosyadadır.

/// Sohbetteki tek balon (ortak koddaki SobiAnalizMesaji'nin Swift kopyası).
struct SobiAnalizGorunenMesaj: Identifiable, Equatable {
    let id: Int
    let cocuktan: Bool
    let metin: String
}

/// Kural motorunun seçeneği (ortak koddaki SobiAnalizBalonu'nun Swift kopyası).
struct SobiAnalizGorunenBalon: Identifiable, Equatable {
    let id: Int
    let etiket: String
    let ucretli: Bool
}

/// Sütun parlaması isteği; `kimlik` her istekte artar ki aynı sütun yeniden parlasın.
struct SobiAnalizVurgu: Equatable {
    let sutun: Int
    let kimlik: Int
}

final class SobiAnalizModeli: ObservableObject {
    // MARK: Görünümün okuduğu durum

    /// Adım çubuğu (Android reviewStepLabel).
    @Published private(set) var baslik: String = ""
    @Published private(set) var mesajlar: [SobiAnalizGorunenMesaj] = []
    @Published private(set) var balonlar: [SobiAnalizGorunenBalon] = []
    /// Adım sayfası: alt düğme "Kontrol Et"; bilgi sayfası: "Derse dön".
    @Published private(set) var adimSayfasi: Bool = true
    @Published private(set) var geriVar: Bool = false
    @Published private(set) var ileriVar: Bool = false
    /// Android abacusBusy: abaküs Sobi'nin elinde; dokunuş, düğmeler ve kayıt kapalı.
    @Published private(set) var mesgul: Bool = false
    @Published private(set) var vurgu: SobiAnalizVurgu? = nil
    @Published private(set) var otomatikOku: Bool

    /// Sesli okuma kullanılabilir mi (Android: SobiSpeaker hazır → reviewSpeak görünür).
    let seslendiriciVar: Bool

    let abakus: AbakusDenetleyici = AbakusDenetleyici()

    // MARK: İç durum

    private let oturum: SobiAnalizOturumu
    private let hizmetler: SobiAnalizHizmetleri
    /// Android stepLog: bu adımın başından beri abaküs değerleri (en çok 400).
    private var adimKaydi: [Int] = []
    private var oynatmaKimligi: Int = 0
    private var vurguSayaci: Int = 0
    private var basladi: Bool = false
    private var kapandi: Bool = false
    private var odemeBekliyor: Bool = false

    static let adimKaydiEnCok: Int = 400
    /// MistakeReviewDialog sabitleri.
    static let geriAlmaGecikmesi: Double = 0.7
    static let yerlesmeSuresi: Double = 0.5
    static let sonrakiAdimGecikmesi: Double = 0.9
    static let vurguGecikmesi: Double = 0.65

    init(istek: AbakusSobiAnalizIstegi, hizmetler: SobiAnalizHizmetleri) {
        self.hizmetler = hizmetler
        self.seslendiriciVar = hizmetler.seslendirici.kullanilabilir
        self.otomatikOku = hizmetler.seslendirici.otomatikOku
        let cozucu = SobiAnalizMetinCozucusu()
        self.oturum = SobiAnalizOturumu(
            numbers: SobiAnalizModeli.kotlinDizisi(istek.sayilar),
            answer: Int32(clamping: istek.cevap),
            log: SobiAnalizModeli.kotlinDizisi(istek.kayit),
            marks: SobiAnalizModeli.kotlinDizisi(istek.isaretler),
            cozucu: cozucu
        )
    }

    static func kotlinDizisi(_ dizi: [Int]) -> [KotlinInt] {
        return dizi.map { KotlinInt(int: Int32(clamping: $0)) }
    }

    /// Android MistakeReviewDialog.canReview: yalnızca toplama/çıkarma ve abaküse sığan diziler
    /// ("Sobi ile analiz et" düğmesi buna göre görünür).
    static func analizYapilabilir(_ sayilar: [Int]) -> Bool {
        return SobiAnalizAdimlari.shared.canReview(numbers: kotlinDizisi(sayilar))
    }

    // MARK: Açılış ve kapanış

    /// Android onViewCreated → startReview (abaküs ölçüldükten sonra).
    func baslat() {
        if basladi { return }
        basladi = true
        let deger: Int32 = Int32(clamping: abakus.anlikDeger)
        let eklenen: String? = oturum.baslat(abakusDegeri: deger)
        sayfayiYansit(eklenen: eklenen)
    }

    /// Pencere kapanırken: süren oynatma ve gecikmeli işler durur, ses kapanır (Android onDestroyView).
    func kapanis() {
        kapandi = true
        oynatmaKimligi += 1
        hizmetler.seslendirici.kapat()
    }

    // MARK: Sayfalar

    func geriDokunuldu() {
        if mesgul { return }
        let sayfa: Int = Int(oturum.sayfa)
        if sayfa > 0 {
            git(sayfa - 1)
        }
    }

    func ileriDokunuldu() {
        if mesgul { return }
        let sayfa: Int = Int(oturum.sayfa)
        if sayfa < Int(oturum.enIleriSayfa) {
            git(sayfa + 1)
        }
    }

    private func git(_ sayfa: Int) {
        let eklenen: String? = oturum.git(i: Int32(sayfa))
        sayfayiYansit(eklenen: eklenen)
    }

    private func ac(_ sayfa: Int) {
        let eklenen: String? = oturum.ac(i: Int32(sayfa))
        sayfayiYansit(eklenen: eklenen)
    }

    /// Android showPage'in görünüm kısmı: ses durur, ilk mesaj okunur, abaküs sayfanın başındaki değere
    /// gelir, talimatın sütunu parlar (abaküs hareket ettiyse 650 ms sonra).
    private func sayfayiYansit(eklenen: String?) {
        hizmetler.seslendirici.durdur()
        if let metin = eklenen {
            hizmetler.seslendirici.otomatikseOku(metin)
        }
        yenile()
        let hedef: Int = Int(oturum.sayfaDegeri())
        let hareket: Bool = abakus.anlikDeger != hedef
        abakusuGetir(hedef)
        let sutun: Int = Int(oturum.vurguSutunu())
        if sutun < 0 { return }
        let sayfa: Int32 = oturum.sayfa
        let gecikme: Double = hareket ? SobiAnalizModeli.vurguGecikmesi : 0
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme) { [weak self] in
            guard let self = self, !self.kapandi, self.oturum.sayfa == sayfa else { return }
            self.vurguSayaci += 1
            self.vurgu = SobiAnalizVurgu(sutun: sutun, kimlik: self.vurguSayaci)
        }
    }

    /// Ortak oturumdan görünür durumu yeniden okur (Android renderPage + updateNav).
    private func yenile() {
        baslik = oturum.baslik()
        adimSayfasi = oturum.adimSayfasi()
        geriVar = oturum.geriVar()
        ileriVar = oturum.ileriVar()
        let ham: [SobiAnalizMesaji] = oturum.mesajlar()
        mesajlar = ham.map { m in
            SobiAnalizGorunenMesaj(id: Int(m.kimlik), cocuktan: m.cocuktan, metin: m.metin)
        }
        let hamBalonlar: [SobiAnalizBalonu] = oturum.balonlar()
        balonlar = hamBalonlar.map { b in
            SobiAnalizGorunenBalon(id: Int(b.kod), etiket: b.etiket, ucretli: b.ucretli)
        }
    }

    // MARK: Abaküs

    /// Android onValueChanged: meşgulken ve sıfırlama sürerken kayda alınmaz.
    func abakusDegisti(_ deger: Int) {
        if mesgul || abakus.sifirlaniyor { return }
        if adimKaydi.last == deger { return }
        if adimKaydi.count >= SobiAnalizModeli.adimKaydiEnCok { return }
        adimKaydi.append(deger)
    }

    /// Android holdAbacus.
    private func tut(_ yeni: Bool) {
        mesgul = yeni
    }

    /// Android moveAbacus: abaküsü değere getirir; hareket bitene dek (500 ms) dokunuş ve düğmeler kapalı,
    /// kayıt durur; sonra kayıt o anki değerle yeniden başlar.
    private func abakusuGetir(_ deger: Int) {
        if abakus.anlikDeger == deger {
            adimKaydi = [deger]
            return
        }
        // Süren gösterim varsa durur (Android: if (p.isPlaying) p.cancel()).
        oynatmaKimligi += 1
        tut(true)
        abakus.ogreticiSayiyiYaz(deger, bitti: {})
        DispatchQueue.main.asyncAfter(deadline: .now() + SobiAnalizModeli.yerlesmeSuresi) { [weak self] in
            guard let self = self, !self.kapandi else { return }
            self.tut(false)
            self.adimKaydi = [self.abakus.anlikDeger]
        }
    }

    /// Android SobiAbacusPlayer.play: işlemin başına getir, hamleleri dersteki sırayla oynat, yine başa
    /// dön. Sonunda abaküs sayfanın başında değilse oraya döner.
    private func oynat(_ gosterim: SobiGosterim) {
        if mesgul { return }
        guard let plan = SobiAbakusOynatici.shared.plan(
            baslangic: gosterim.baslangic, toplama: gosterim.toplama, sayi: gosterim.sayi
        ) else {
            return
        }
        let sayfa: Int32 = oturum.sayfa
        tut(true)
        oynatmaKimligi += 1
        let kimlik: Int = oynatmaKimligi
        let kareler: [SobiOynatmaKaresi] = plan.kareler
        for kare in kareler {
            let deger: Int = Int(kare.deger)
            let saniye: Double = Double(kare.zamanMs) / 1000.0
            DispatchQueue.main.asyncAfter(deadline: .now() + saniye) { [weak self] in
                guard let self = self, !self.kapandi, self.oynatmaKimligi == kimlik else { return }
                if self.abakus.anlikDeger != deger {
                    self.abakus.ogreticiSayiyiYaz(deger, bitti: {})
                }
            }
        }
        let bitis: Double = Double(plan.bitisMs) / 1000.0
        DispatchQueue.main.asyncAfter(deadline: .now() + bitis) { [weak self] in
            guard let self = self, !self.kapandi, self.oynatmaKimligi == kimlik else { return }
            self.tut(false)
            let taban: Int = Int(self.oturum.sayfaDegeri())
            if self.oturum.sayfa == sayfa && self.abakus.anlikDeger != taban {
                self.abakusuGetir(taban)
            } else {
                self.adimKaydi = [self.abakus.anlikDeger]
            }
        }
    }

    // MARK: Kontrol Et

    /// Android onCheck. Bilgi sayfasında ("Derse dön") pencere kapanır.
    func kontrolDokunuldu(kapat: () -> Void) {
        if !oturum.adimSayfasi() {
            kapat()
            return
        }
        if mesgul { return }
        let deger: Int = abakus.anlikDeger
        let sonuc: SobiKontrolSonucu = oturum.kontrolEt(
            deger: Int32(clamping: deger), adimKaydi: SobiAnalizModeli.kotlinDizisi(adimKaydi)
        )
        if sonuc.kapat {
            kapat()
            return
        }
        if let hamle = sonuc.hamle {
            hizmetler.kurallar.hamleyiKaydet(hamle, dogru: sonuc.dogru)
        }
        yenile()
        if let metin = sonuc.mesaj {
            hizmetler.seslendirici.otomatikseOku(metin)
        }
        let sayfa: Int32 = oturum.sayfa
        if sonuc.dogru {
            hizmetler.sesler.dogruSesi()
            let sonraki: Int = Int(sonuc.sonrakiSayfa)
            tut(true)
            DispatchQueue.main.asyncAfter(deadline: .now() + SobiAnalizModeli.sonrakiAdimGecikmesi) { [weak self] in
                guard let self = self, !self.kapandi, self.oturum.sayfa == sayfa else { return }
                self.tut(false)
                if sonraki >= 0 {
                    self.ac(sonraki)
                }
            }
            return
        }
        hizmetler.sesler.yanlisSesi()
        // Android rewindToStepStart: çocuk önce kendi yaptığını görsün, sonra abaküs adımın başına döner.
        let geri: Int = Int(sonuc.geriDeger)
        tut(true)
        DispatchQueue.main.asyncAfter(deadline: .now() + SobiAnalizModeli.geriAlmaGecikmesi) { [weak self] in
            guard let self = self, !self.kapandi else { return }
            self.tut(false)
            if self.oturum.sayfa == sayfa {
                self.abakusuGetir(geri)
            }
        }
    }

    // MARK: Balonlar

    /// Android localChips tıklaması (meşgulken yok sayılır).
    func balonaDokunuldu(_ kod: Int) {
        if mesgul { return }
        let sonuc: SobiBalonSonucu = oturum.balonaDokun(kod: Int32(kod))
        if let gosterim = sonuc.gosterim {
            oynat(gosterim)
            return
        }
        if let hazirlik = sonuc.ornek {
            ornekIcinOde(hazirlik)
            return
        }
        yenile()
        if let metin = sonuc.mesaj {
            hizmetler.seslendirici.otomatikseOku(metin)
        }
    }

    /// Android payForExample → openExample. Ödeme başarısızsa örnek açılmaz, Sobi bunu söyler.
    private func ornekIcinOde(_ hazirlik: SobiOrnekHazirligi) {
        if odemeBekliyor { return }
        odemeBekliyor = true
        hizmetler.odeme.ornekIcinOde { [weak self] basarili in
            DispatchQueue.main.async {
                guard let self = self else { return }
                self.odemeBekliyor = false
                if self.kapandi { return }
                if basarili {
                    let eklenen: String? = self.oturum.ornegiAc(h: hazirlik)
                    self.sayfayiYansit(eklenen: eklenen)
                } else {
                    let metin: String = self.oturum.ornekOdenemedi()
                    self.yenile()
                    self.hizmetler.seslendirici.otomatikseOku(metin)
                }
            }
        }
    }

    // MARK: Sesli okuma

    /// Android reviewSpeak: otomatik okumayı açar / kapatır.
    func otomatikOkumayiDegistir() {
        let yeni: Bool = !hizmetler.seslendirici.otomatikOku
        hizmetler.seslendirici.otomatikOku = yeni
        if !yeni {
            hizmetler.seslendirici.durdur()
        }
        otomatikOku = yeni
    }

    /// Mesajın yanındaki 🔊.
    func mesajiOku(_ metin: String) {
        hizmetler.seslendirici.oku(metin)
    }
}

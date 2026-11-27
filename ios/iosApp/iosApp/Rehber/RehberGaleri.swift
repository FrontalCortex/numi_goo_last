import SwiftUI
import Shared

// Ekran galerisi (Yonetim/EkranGalerisi) için rehber örnekleri:
//   rehber-ders      abaküs dersinde toplama rehberi (rehber 1) açık
//   rehber-kor-ders  kör derste rehber ("sayıyı geç" öğreticisi ya da rehber 1)
//   rehber-maraton   haritada ünite maratonu rehberi (bitmiş ilk sandık, Rekor satırı vurgulu)
//
// Galeri her açılışta kayıtları bellekte tutan sahte depo kullanır: simülatörde önceki açılışın
// "gösterildi" bayrakları sonucu değiştirmesin.

enum RehberGaleri {
    static let adlar: Set<String> = ["rehber-ders", "rehber-kor-ders", "rehber-maraton"]

    /// HaritaModeli yalnızca zayıf başvuru tuttuğundan kaynak burada yaşar.
    static let ilerleme: RehberGaleriIlerleme = RehberGaleriIlerleme()
}

/// Hiçbir şey kalıcı değil: her galeri açılışı temiz başlar.
final class SahteRehberDeposu: RehberKayitDeposu {
    private var maratonBekleyen: Bool
    private var maratonGosterilen: Bool = false
    private var sayaclar: [Int: Int] = [:]
    private var atlaGosterilen: Bool = false

    init(maratonBekliyor: Bool) {
        self.maratonBekleyen = maratonBekliyor
    }

    func maratonGosterildi() -> Bool {
        return maratonGosterilen
    }

    func maratonBekliyor() -> Bool {
        return maratonBekleyen
    }

    func maratonBekleyenAyarla(_ bekliyor: Bool) {
        maratonBekleyen = bekliyor
    }

    func maratonuGosterildiOlarakIsaretle() {
        maratonGosterilen = true
        maratonBekleyen = false
    }

    func gosterimSayisi(rehberNo: Int) -> Int {
        return sayaclar[rehberNo] ?? 0
    }

    func gosterimiKaydet(rehberNo: Int) {
        sayaclar[rehberNo] = (sayaclar[rehberNo] ?? 0) + 1
    }

    func atlaOgreticisiGosterildi() -> Bool {
        return atlaGosterilen
    }

    func atlaOgreticisiniIsaretle() {
        atlaGosterilen = true
    }
}

/// 1. kısmın ilk ünite maratonu bitmiş (rekor 42) gibi görünen ilerleme kaynağı.
final class RehberGaleriIlerleme: HaritaIlerlemeKaynagi {
    func bulutOgeleri(partId: Int32) -> [LessonItem]? {
        if partId != 1 {
            return nil
        }
        let liste: [LessonItem] = GlobalLessonData.shared.createLessonItems(partId: partId)
        let sira: Int = Int(MarathonGuideLogic.shared.firstMarathonLessonIndex())
        if sira >= 0 && sira < liste.count {
            let sandik: LessonItem = liste[sira]
            sandik.isCompleted = true
            sandik.stepIsFinish = true
            sandik.record = KotlinInt(int: 42)
        }
        return liste
    }

    func dersiYaz(partId: Int32, sira: Int32, oge: LessonItem) {
    }
}

struct RehberGaleriEkrani: View {
    let ad: String

    init(ad: String) {
        self.ad = ad
        RehberOrtak.depo = SahteRehberDeposu(maratonBekliyor: ad == "rehber-maraton")
    }

    var body: some View {
        icerik
    }

    @ViewBuilder
    private var icerik: some View {
        if ad == "rehber-ders" {
            AbakusDersEkrani(dersBaslangici: RehberGaleriEkrani.abakusDersi, bitti: { _ in }, kapat: {})
        } else if ad == "rehber-kor-ders" {
            KorDersEkrani(baslangic: RehberGaleriEkrani.korDersi, bitti: { _ in }, kapat: {})
        } else {
            RehberGaleriMaraton()
        }
    }

    /// 1. kısmın ilk dersi gibi: toplama rehberi (rehber 1), ilk adım.
    private static var abakusDersi: AbakusDersBaslangici {
        var baslangic: AbakusDersBaslangici = AbakusDersBaslangici(
            partId: 1,
            haritaSirasi: 1,
            baslik: "Toplama",
            soruKaynagi: .hazir([
                AbakusSoru.islem(4, "+", 5),
                AbakusSoru.islem(3, "+", 2),
            ])
        )
        baslangic.rehberNo = 1
        baslangic.rehberIlkAdim = true
        return baslangic
    }

    /// Kör ders: "sayıyı geç" düğmesi görünüyorsa rehber 5, değilse rehber 1 açılır.
    private static var korDersi: KorDersBaslangici {
        var baslangic: KorDersBaslangici = KorDersBaslangici(partId: 4)
        baslangic.islemler = [KorDersIslemi.dizi([3, 4, 2]), KorDersIslemi.dizi([5, 1, 3])]
        baslangic.kor = true
        baslangic.sayiSuresiMs = 1000
        baslangic.rehberNo = 1
        baslangic.rehberIlkAdim = true
        return baslangic
    }
}

/// Haritada 1. kısım açık, ilk sandık bitmiş; rehber kendiliğinden başlar (HaritaDersListesi.onAppear).
struct RehberGaleriMaraton: View {
    @StateObject private var model: HaritaModeli

    init() {
        _model = StateObject(wrappedValue: RehberGaleriMaraton.hazirla())
    }

    var body: some View {
        HaritaEkrani(model: model, yerTutucuGoster: true)
    }

    private static func hazirla() -> HaritaModeli {
        let model: HaritaModeli = HaritaModeli()
        model.ilerlemeKaynagi = RehberGaleri.ilerleme
        for satir in model.kisimSatirlari {
            for kart in satir.kartlar where kart.partId == 1 {
                model.kisimSec(kart)
            }
        }
        return model
    }
}

import SwiftUI
import UIKit

// Yeniden kullanılabilir abaküs bileşeni (ders, serbest alıştırma, mağaza önizlemesi).
//
// Android karşılığı: fragment_abacus.xml / fragment_abacus_practice.xml içindeki abacusContainer
// ile onu süren AbacusBeadController. Boncuk mantığı ortak koddadır (bkz. AbakusTahta.swift).
//
// Kullanım:
//
//     @StateObject private var abakus = AbakusDenetleyici()
//     ...
//     AbakusGorunumu(denetleyici: abakus)
//     Button("Sıfırla") { abakus.sifirla() }
//     Text("\(abakus.deger)")
//
// Bileşenin boyutu sabittir (`AbakusGorunumu.boyut(basamakSayisi:)`; 5 basamakta 273 × 290 pt).
// Küçültmek / büyütmek için dışarıdan `.scaleEffect` uygulanabilir; dokunma hesabı görünümün
// kendi koordinatında yapıldığı için ölçekten etkilenmez.

/// Abaküse dışarıdan komut vermek ve değerini okumak için tutamak.
final class AbakusDenetleyici: ObservableObject {
    /// Abaküsün gösterdiği sayı (0...99999). Boncuk hareket etmeye başladığı anda güncellenir
    /// (Android'deki getCurrentValue gibi mantıksal değerdir; animasyonun bitmesini beklemez).
    @Published private(set) var deger: Int = 0

    fileprivate weak var tahta: AbakusTahtaGorunumu?
    /// Görünüm henüz bağlanmadan istenen başlangıç değeri.
    private var bekleyenDeger: Int?

    init() {}

    /// Görünümden o anda okunan değer (yayınlanan `deger` ile aynıdır; görünüm güncellemesi
    /// beklemeden okumak için).
    var anlikDeger: Int {
        return tahta?.deger ?? deger
    }

    /// Kullanıcı dokunuşu ya da sıfırlama animasyonu sürüyor mu.
    var hareketEdiyor: Bool {
        return tahta?.hareketEdiyor ?? false
    }

    /// Sıfırlama animasyonu sürüyor mu (Android: isResetInProgress). Sürerken görülen ara değerler
    /// soru kaydına (hata analizi) alınmaz.
    var sifirlaniyor: Bool {
        return tahta?.sifirlaniyor ?? false
    }

    /// Bütün boncukları sıfır konumuna döndürür (Android: abacusController.reset()).
    func sifirla(animasyonlu: Bool = true) {
        bekleyenDeger = nil
        tahta?.sifirla(animasyonlu: animasyonlu)
    }

    /// Boncukları animasyonsuz olarak verilen değere yerleştirir (0...99999).
    func degeriAyarla(_ yeni: Int) {
        if let tahta = tahta {
            tahta.degeriAyarla(yeni)
        } else {
            bekleyenDeger = yeni
        }
    }

    fileprivate func bagla(_ yeniTahta: AbakusTahtaGorunumu) {
        tahta = yeniTahta
        if let bekleyen = bekleyenDeger {
            bekleyenDeger = nil
            yeniTahta.degeriAyarla(bekleyen)
        }
        let anlik = yeniTahta.deger
        if anlik != deger {
            // Görünüm güncellemesi sırasında yayın yapılmaz.
            DispatchQueue.main.async { [weak self] in
                self?.deger = anlik
            }
        }
    }

    fileprivate func degerGeldi(_ yeni: Int) {
        if yeni != deger {
            deger = yeni
        }
    }
}

/// Abaküs bileşeni.
struct AbakusGorunumu: View {
    private let denetleyici: AbakusDenetleyici
    private let basamakSayisi: Int
    private let kilitli: Bool
    private let stil: AbakusStili
    private let animasyonSuresiMs: Int
    private let ses: AbakusBoncukSesi?
    private let degerDegisti: ((Int) -> Void)?
    private let boncukAyari: AbakusBoncukAyari

    /// - Parameters:
    ///   - denetleyici: değer okuma, sıfırlama ve değer yerleştirme tutamağı.
    ///   - basamakSayisi: gösterilecek çubuk sayısı (1...5); sağdan (birler) başlayarak sayılır.
    ///   - kilitli: true iken dokunmalar yutulur, boncuklar oynamaz.
    ///   - stil: boncuk ve çerçeve görünümü.
    ///   - animasyonSuresiMs: boncuğun bir adımı kaç ms'de aldığı (güncel Android: her derste 300).
    ///     Yalnızca görünüm ilk kurulurken okunur.
    ///   - ses: boncuk tık sesi; nil ise sessiz.
    ///   - degerDegisti: değer her değiştiğinde çağrılır (`denetleyici.deger` ile aynı bilgi).
    ///   - boncukAyari: ayar panelindeki boncuk ölçeği ve boşlukları (varsayılan: değişiklik yok).
    init(
        denetleyici: AbakusDenetleyici,
        basamakSayisi: Int = 5,
        kilitli: Bool = false,
        stil: AbakusStili = .varsayilan,
        animasyonSuresiMs: Int = 300,
        ses: AbakusBoncukSesi? = nil,
        degerDegisti: ((Int) -> Void)? = nil,
        boncukAyari: AbakusBoncukAyari = AbakusBoncukAyari.varsayilan
    ) {
        self.denetleyici = denetleyici
        self.basamakSayisi = basamakSayisi
        self.kilitli = kilitli
        self.stil = stil
        self.animasyonSuresiMs = animasyonSuresiMs
        self.ses = ses
        self.degerDegisti = degerDegisti
        self.boncukAyari = boncukAyari
    }

    /// Bileşenin pt cinsinden boyutu.
    static func boyut(basamakSayisi: Int = 5) -> CGSize {
        return AbakusTahtaGorunumu.boyut(basamakSayisi: basamakSayisi)
    }

    var body: some View {
        let boyut = AbakusGorunumu.boyut(basamakSayisi: basamakSayisi)
        AbakusTahtaKoprusu(
            denetleyici: denetleyici,
            basamakSayisi: basamakSayisi,
            kilitli: kilitli,
            stil: stil,
            animasyonSuresiMs: animasyonSuresiMs,
            ses: ses,
            degerDegisti: degerDegisti,
            boncukAyari: boncukAyari
        )
        .frame(width: boyut.width, height: boyut.height)
    }
}

/// UIKit görünümünü SwiftUI'a bağlar.
private struct AbakusTahtaKoprusu: UIViewRepresentable {
    let denetleyici: AbakusDenetleyici
    let basamakSayisi: Int
    let kilitli: Bool
    let stil: AbakusStili
    let animasyonSuresiMs: Int
    let ses: AbakusBoncukSesi?
    let degerDegisti: ((Int) -> Void)?
    let boncukAyari: AbakusBoncukAyari

    func makeUIView(context: Context) -> AbakusTahtaGorunumu {
        let tahta = AbakusTahtaGorunumu(animasyonSuresiMs: animasyonSuresiMs)
        // Önce bağlanır: bekleyen başlangıç değeri, değer bildirimi kurulmadan yerleştirilir.
        denetleyici.bagla(tahta)
        uygula(tahta)
        return tahta
    }

    func updateUIView(_ uiView: AbakusTahtaGorunumu, context: Context) {
        uygula(uiView)
    }

    private func uygula(_ tahta: AbakusTahtaGorunumu) {
        tahta.basamakSayisiniAyarla(basamakSayisi)
        tahta.stiliAyarla(stil)
        tahta.kilitle(kilitli)
        tahta.ses = ses
        tahta.boncukAyariniAyarla(boncukAyari)
        let tutamak = denetleyici
        let bildirim = degerDegisti
        tahta.degerDegisti = { [weak tutamak] yeni in
            tutamak?.degerGeldi(yeni)
            bildirim?(yeni)
        }
    }
}

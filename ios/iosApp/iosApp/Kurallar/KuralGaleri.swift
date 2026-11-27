import SwiftUI

// Ekran galerisi (hata ayıklama) için kural ekranları: `-ekran kural-kitabi` gibi.
//
//  - kural-kitabi: kitap, toplama bölümü (5'li, 10'lu-5'li, 10'lu, boncuk ve 1x–9x çarpım tabloları).
//  - kural-kitabi-2: kitap, çıkarma bölümü (5'li, 10'lu, boncuk çıkarma tabloları).
//  - kural-paneli: abaküs dersinde (bölüm 1, sıra 24) sihirli değnek paneli açık (10'luk toplama tablosu).
//  - kural-paneli-carpim: kör derste (bölüm 6) kitaptan 7x çarpım tablosu seçilmiş, panel açık.
enum KuralGaleri {
    static let adlar: [String] = [
        "kural-kitabi",
        "kural-kitabi-2",
        "kural-paneli",
        "kural-paneli-carpim",
    ]

    /// Bölüm 1, sıra 24: panel 10'luk toplama tablosunu gösterir (sıra > 22).
    static var abakusDersi: AbakusDersBaslangici {
        return AbakusDersBaslangici(
            partId: 1,
            haritaSirasi: 24,
            dersTuru: 0,
            baslik: "Büyük Kardeş",
            soruKaynagi: .hazir([
                AbakusSoru.islem(6, "+", 7),
                AbakusSoru.islem(8, "+", 5),
            ])
        )
    }

    /// Bölüm 6 (kör toplama + çarpma): tablo seçicili; abaküsle cevaplanır.
    static var korDersi: KorDersBaslangici {
        var b = KorDersBaslangici(partId: 6)
        b.islemler = [KorDersIslemi.dizi([3, 4, 2]), KorDersIslemi.dizi([5, 1, 3])]
        b.sayiSuresiMs = 1000
        return b
    }
}

struct KuralGaleriEkrani: View {
    let ad: String

    var body: some View {
        icerik
    }

    @ViewBuilder
    private var icerik: some View {
        switch ad {
        case "kural-kitabi":
            KuralKitabiEkrani(baslangicSayfasi: .toplama, kapat: {})
                .padding(10)
        case "kural-kitabi-2":
            KuralKitabiEkrani(baslangicSayfasi: .cikarma, kapat: {})
                .padding(10)
        case "kural-paneli":
            AbakusDersEkrani(
                dersBaslangici: KuralGaleri.abakusDersi,
                baslangicKurali: .panel(nil),
                bitti: { _ in },
                kapat: {}
            )
        default:
            KorDersEkrani(
                baslangic: KuralGaleri.korDersi,
                baslangicKurali: .panel(.carpim(7)),
                bitti: { _ in },
                kapat: {}
            )
        }
    }
}

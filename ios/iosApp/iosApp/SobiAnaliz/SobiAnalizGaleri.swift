import SwiftUI

// Ekran galerisi (Yonetim/EkranGalerisi.swift) için "Sobi ile analiz et" örnekleri:
//   sobi-analiz    248 + 223 = 461 (doğrusu 471): birlerde "10 gelir" unutulmuş (toplama, 10'luk kural).
//   sobi-analiz-2  41 − 3 = 31 (doğrusu 38): 10 gitti ama kardeşi 7 gelmedi (çıkarma, 10'luk kural).
// Kayıtlar çocuğun boncukları tek tek oynatırken abaküste görünen değerlerdir (ortak koddaki
// MistakeAnalyzerTest / SobiAnalizOturumuTest örnekleriyle aynı).
enum SobiAnalizGaleri {
    static let adlar: [String] = ["sobi-analiz", "sobi-analiz-2"]

    static func istek(_ ad: String) -> AbakusSobiAnalizIstegi {
        if ad == "sobi-analiz-2" {
            return AbakusSobiAnalizIstegi(
                sayilar: [41, -3],
                cevap: 31,
                kayit: [0, 10, 20, 30, 40, 41, 31],
                isaretler: [],
                dersBasligi: "10'luk Çıkarma",
                kapandi: {}
            )
        }
        let yazma: [Int] = [0, 100, 200, 210, 220, 230, 240, 245, 246, 247, 248]
        let islem: [Int] = [348, 448, 498, 488, 478, 468, 463, 462, 461]
        return AbakusSobiAnalizIstegi(
            sayilar: [248, 223],
            cevap: 461,
            kayit: yazma + islem,
            isaretler: [],
            dersBasligi: "10'luk Toplama",
            kapandi: {}
        )
    }
}

struct SobiAnalizGaleriEkrani: View {
    let ad: String

    var body: some View {
        SobiAnalizEkrani(istek: SobiAnalizGaleri.istek(ad), kapat: {})
    }
}

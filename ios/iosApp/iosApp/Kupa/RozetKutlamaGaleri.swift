import SwiftUI

// Rozet kutlamasının galeri adı (Yonetim/EkranGalerisi.swift ekran listesine ekler):
//   rozet-kutlama  iki rozetlik kuyruk: Dart rozeti 9 → 10 (Bronz → Gümüş; çubuk 3 sn'de dolar, sonra yeni
//                  renkler ve animasyon) ve ardından Siyah Kuşak (0 → 1). Ekran görüntüsü açılıştan 5 sn
//                  sonra alınır: o anda sayım bitmiş, kutlama animasyonu oynuyordur.

enum RozetKutlamaGaleri {
    static let adlar: [String] = ["rozet-kutlama"]
}

struct RozetKutlamaGaleriEkrani: View {
    let ad: String

    var body: some View {
        RozetKutlamaEkrani(kuyruk: ["DAILY_ONLY|9|10|10", "KARATE|0|1|1"], bitti: {})
    }
}

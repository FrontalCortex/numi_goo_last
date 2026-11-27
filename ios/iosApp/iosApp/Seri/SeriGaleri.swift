import SwiftUI

// Ekran galerisinin seri ekranları (Yonetim/EkranGalerisi.swift `-ekran <ad>` ile açar). Her ekran kendi
// sahte servisiyle (ayrı cüzdan) ve ortak koddaki `seedForPreview` ile kurulan hazır durumla açılır.
enum SeriGaleri {
    static let adlar: [String] = [
        "seri",
        "seri-kirik",
        "seri-meydan-okuma",
        "seri-kutlama",
        "seri-yeni",
        "seri-yeni-soru",
        "seri-hedef",
        "seri-hatirlatma",
    ]
}

struct SeriGaleriEkrani: View {
    let ad: String
    @StateObject private var kaynak: SeriGaleriKaynagi

    init(ad: String) {
        self.ad = ad
        _kaynak = StateObject(wrappedValue: SeriGaleriKaynagi(ad: ad))
    }

    var body: some View {
        switch ad {
        case "seri-kutlama":
            VStack(spacing: 0) {
                SeriKutlamaSeridi(seri: 4, dokunuldu: {})
                Spacer(minLength: 0)
            }
            .background(Color("background_color").ignoresSafeArea())
        case "seri-yeni":
            SeriYeniSeriEkrani(servis: kaynak.servis, bitti: {})
        case "seri-yeni-soru":
            SeriYeniSeriEkrani(servis: kaynak.servis, soruSayfasiyla: true, bitti: {})
        case "seri-hedef":
            SeriEkrani(servis: kaynak.servis, kapat: {}, acikPencere: .hedef)
        case "seri-hatirlatma":
            SeriEkrani(servis: kaynak.servis, kapat: {}, acikPencere: .hatirlatma)
        default:
            // "seri", "seri-kirik", "seri-meydan-okuma"
            SeriEkrani(servis: kaynak.servis, kapat: {})
        }
    }
}

/// Galeri ekranının sahte servisi (bir kez kurulur).
final class SeriGaleriKaynagi: ObservableObject {
    let servis: SahteSeriServisi

    init(ad: String) {
        var ayarlar = SahteSeriAyarlari()
        ayarlar.dersSonuEkSaniye = 0
        servis = SahteSeriServisi(cuzdan: MagazaSahteCuzdanServisi(), ayarlar: ayarlar)
        switch ad {
        case "seri-kirik", "seri-yeni", "seri-yeni-soru":
            // Seri kırık: büyük sayı 0, alev sönük, meydan okuma seçilmemiş (kart yok), dondurma yok.
            servis.ornekKur(
                seri: 0, enUzun: 4, bugunTamam: false, hedefDakika: 5, bugunDakika: 1,
                meydanGun: 0, meydanAlindi: 0, dondurma: 0, sunucuSerisi: 0, toplananlar: []
            )
        case "seri-meydan-okuma":
            // 3 günlük meydan okuma tamamlandı ve sunucu da biliyor: "Ödülü topla"; 3. gün taşı toplanabilir.
            servis.ornekKur(
                seri: 3, enUzun: 3, bugunTamam: true, hedefDakika: 5, bugunDakika: 6,
                meydanGun: 3, meydanAlindi: 0, dondurma: 0, sunucuSerisi: 3, toplananlar: []
            )
        default:
            // Süren seri: 2 gün, bugün 3/10 dakika, 5 günlük meydan okumanın 2. günü, dondurma hazır.
            servis.ornekKur(
                seri: 2, enUzun: 6, bugunTamam: false, hedefDakika: 10, bugunDakika: 3,
                meydanGun: 5, meydanAlindi: 0, dondurma: 1, sunucuSerisi: 2, toplananlar: []
            )
        }
    }
}

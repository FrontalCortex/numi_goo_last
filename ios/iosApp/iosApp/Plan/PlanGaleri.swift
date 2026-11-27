import Shared
import SwiftUI

// Plan karşılaştırma ekranının galeri adları (Yonetim/EkranGalerisi.swift bunları ekran listesine ekler):
//   plan        karşılaştırma tablosu, Pro olmayan kullanıcı (deneme teklifli düğme)
//   plan-secim  kartlar sayfası (Pro ve Lite kartı, yenileme açıklaması, yasal bağlantılar)
//   plan-pro    kullanıcı zaten Pro: düğme yerine "PRO ÜYELİĞİN AKTİF" etiketi

enum PlanGaleri {
    static let adlar: [String] = ["plan", "plan-secim", "plan-pro"]
}

/// Galeri için ödeme servisi: hem Pro hem Lite fiyatı döner (Lite kartı da görünsün), Pro'da 7 gün deneme
/// vardır. Satın alma hiçbir şey yapmaz (iptal döner).
final class PlanGaleriOdemeServisi: MagazaOdemeServisi {
    func urunleriGetir(kimlikler: [String], tamam: @escaping ([MagazaUrun]) -> Void) {
        let katalog = BillingCatalog.shared
        var urunler: [MagazaUrun] = []
        for kimlik in kimlikler {
            if kimlik == katalog.SUB_PRO {
                urunler.append(MagazaUrun(kimlik: kimlik, fiyatMetni: "₺99,00"))
            } else if kimlik == katalog.SUB_LITE {
                urunler.append(MagazaUrun(kimlik: kimlik, fiyatMetni: "₺49,00"))
            }
        }
        DispatchQueue.main.async {
            tamam(urunler)
        }
    }

    func satinAl(urunKimligi: String, tamam: @escaping (MagazaSatinAlmaSonucu) -> Void) {
        DispatchQueue.main.async {
            tamam(MagazaSatinAlmaSonucu.iptal)
        }
    }

    func geriYukle(tamam: @escaping (Bool) -> Void) {
        DispatchQueue.main.async {
            tamam(false)
        }
    }

    func ucretsizDenemeGunu(urunKimligi: String) -> Int? {
        if urunKimligi == BillingCatalog.shared.SUB_PRO {
            return 7
        }
        return nil
    }
}

struct PlanGaleriEkrani: View {
    let ad: String
    private let servisler: MagazaBagimliliklar

    init(ad: String) {
        self.ad = ad
        self.servisler = PlanGaleriEkrani.servisleriKur(pro: ad == "plan-pro")
    }

    var body: some View {
        let sayfa: PlanSayfasi = ad == "plan-secim" ? PlanSayfasi.planlar : PlanSayfasi.karsilastirma
        return PlanKarsilastirmaEkrani(servisler: servisler, baslangicSayfasi: sayfa, kapat: {})
    }

    private static func servisleriKur(pro: Bool) -> MagazaBagimliliklar {
        let cuzdan = MagazaSahteCuzdanServisi()
        if pro {
            cuzdan.planiDegistir("Pro", sobiUcretsizSinir: 20)
        }
        let enerji = MagazaSahteEnerjiServisi(cuzdan: cuzdan)
        enerji.sonsuz = pro
        return MagazaBagimliliklar(
            odeme: PlanGaleriOdemeServisi(),
            cuzdan: cuzdan,
            enerji: enerji,
            reklam: MagazaSahteReklamServisi(),
            seri: MagazaSahteSeriServisi(cuzdan: cuzdan)
        )
    }
}

import Combine
import Shared
import SwiftUI

// Yarış paneli ekranlarının galeri adları (Yonetim/EkranGalerisi.swift bunları ekran listesine ekler):
//   yaris-paneli            Toplama Ustalığı listesi (TAMAMLANDI, BAŞLA ve KİLİTLİ satırlar)
//   yaris-paneli-kilitli    Pro olmayan kullanıcı: Android'de panele hiç girilmez; kısım 7 kartı kilitlidir ve
//                           "Bu kısmı açmak için planı Pro'ya yükselt." uyarısı çıkar (kısım seçimi + uyarı)
//   yaris-ders              yarış kipinde kör ders (sayılar ekranda, cevap abaküsle; kısım 7)
//   yaris-paneli-alt        BAŞLAT alt paneli (race_lesson_bottom_sheet.xml)
//   yaris-paneli-ileri-sar  kilitli yarış: anahtar düğmeli alt panel + "İleri Sar" penceresi

enum YarisGaleri {
    static let adlar: [String] = [
        "yaris-paneli",
        "yaris-paneli-kilitli",
        "yaris-ders",
        "yaris-paneli-alt",
        "yaris-paneli-ileri-sar",
    ]

    /// Kısım 7'nin şablonu: ilk yarış tamamlandı, ikincisi açık, ötekiler kilitli (Android'deki ilerleme sırası).
    static func ornekOgeler(partId: Int32) -> [LessonItem] {
        let liste: [LessonItem] = GlobalLessonData.shared.createLessonItems(partId: partId)
        if partId == 7 && liste.count > 2 {
            liste[0].raceBusyLevel = KotlinInt(int: 0)
            liste[1].raceBusyLevel = KotlinInt(int: 1)
        }
        return liste
    }

    static var girdi: YarisPaneliGirdisi {
        return YarisPaneliGirdisi(
            partId: 7,
            baslik: Metin.al("ui_toplama_ustaligi"),
            partSecimindenMi: true,
            geriPartId: nil
        )
    }

    static var baglanti: YarisPaneliBaglantisi {
        let yenileme: AnyPublisher<Void, Never> = Empty<Void, Never>().eraseToAnyPublisher()
        let liste: [LessonItem] = YarisGaleri.ornekOgeler(partId: 7)
        return YarisPaneliBaglantisi(
            ogeler: { (partId: Int32) -> [LessonItem] in
                return liste
            },
            baslat: { (partId: Int32, sira: Int, oge: LessonItem) -> Void in },
            yenileme: yenileme
        )
    }

    /// Yarış kipindeki kör ders: raceBusyLevel dolu (1 = BAŞLA), isBlinding yok → cevap abaküsle.
    static var dersBaslangici: KorDersBaslangici {
        var b = KorDersBaslangici(partId: 7)
        b.dersIndeksi = 1
        b.dersTuru = 0
        b.islemler = [
            KorDersIslemi.dizi([3, 4, 2, 6]),
            KorDersIslemi.dizi([5, 1, 3, 2]),
        ]
        b.yarisYogunlugu = 1
        b.sayiSuresiMs = 3000
        b.baslik = "Başlangıç Ustası"
        b.baslangicAdimi = 78
        b.bitisAdimi = 78
        return b
    }
}

struct YarisGaleriEkrani: View {
    let ad: String

    var body: some View {
        switch ad {
        case "yaris-ders":
            KorDersEkrani(baslangic: YarisGaleri.dersBaslangici, bitti: { _ in }, kapat: {})
        case "yaris-paneli-kilitli":
            YarisKilitliGaleriEkrani()
        case "yaris-paneli-alt":
            panel(.altPanel(satir: 1))
        case "yaris-paneli-ileri-sar":
            panel(.ileriSar(satir: 2))
        default:
            panel(.liste)
        }
    }

    private func panel(_ baslangic: YarisBaslangicDurumu) -> YarisPaneliEkrani {
        return YarisPaneliEkrani(
            girdi: YarisGaleri.girdi,
            baglanti: YarisGaleri.baglanti,
            cuzdan: MagazaSahteCuzdanServisi(),
            baslangic: baslangic,
            kapat: {}
        )
    }
}

/// Pro olmayan kullanıcı: kısım seçimi + kısım 7 kartına dokununca çıkan kilit uyarısı (Android: LockedHint).
struct YarisKilitliGaleriEkrani: View {
    @StateObject private var model: HaritaModeli = HaritaModeli()

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            HaritaKisimSecimi(model: model)
            uyari
        }
    }

    private var uyari: some View {
        let bildirim = HaritaBildirimi(
            id: 1,
            metin: Metin.al("ui_bu_kismi_acmak_icin_plani_pro"),
            uzun: true
        )
        return VStack(spacing: 0) {
            Spacer(minLength: 0)
            HaritaBildirimGorunumu(bildirim: bildirim)
        }
        .allowsHitTesting(false)
    }
}

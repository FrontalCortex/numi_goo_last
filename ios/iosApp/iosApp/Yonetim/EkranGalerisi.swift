import SwiftUI
import UIKit

// Ekran galerisi (hata ayıklama): uygulama `-ekran <ad>` başlatma argümanıyla açılırsa giriş ve
// kabuk yerine doğrudan o ekran örnek veriyle tam ekran gösterilir. Amaç, Codemagic
// simülatöründe her ekranın ekran görüntüsünü ayrı ayrı alabilmek:
//
//   xcrun simctl launch booted <paket kimliği> -ekran sonuc-sandik
//
// Başlatma argümanları UserDefaults'un geçici argüman alanına da düşer; bu yüzden
// `UserDefaults.standard.string(forKey: "ekran")` aynı değeri verir (kalıcı yazılmaz).
//
// Ekranlar yalnızca dışa açık giriş görünümleriyle kurulur; kapanışlar boştur (düğmeler bir yere
// götürmez). İki istisna EkranGalerisiIcDurumlar.swift dosyasındadır.
@MainActor
struct EkranGalerisi: View {
    /// Geçerli ekran adları (küçük harf, ASCII).
    static let adlar: [String] = [
        "giris",
        "giris-acilis",
        "giris-tanisma",
        "giris-eposta",
        "kayit",
        "kayit-yas",
        "kayit-kaynak",
        "kayit-abakus",
        "kayit-hedef",
        "kayit-yol",
        "kayit-meydan-okuma",
        "kayit-hatirlatma",
        "kayit-veli-onayi",
        "kayit-kod",
        "kabuk",
        "harita-kisim",
        "harita-ders",
        "harita-popover",
        "harita-kilitli-popover",
        "abakus-ders",
        "abakus-sandik",
        "kor-ders",
        "sonuc-basarili",
        "sonuc-basarisiz",
        "sonuc-sandik-basarisiz",
        "sonuc-sandik",
        "sandik",
        "gorevler",
        "gorevler-yeni",
        "kupa",
        "kupa-gecmisi",
        "kupa-yolu-odul",
        "lider-tablosu",
        "rozetler",
        "rozet-detay",
        "ozellestirme",
        "magaza",
        "magaza-alt",
        "profil",
        "profil-duzenle",
        "avatar",
        "avatar-duzenle",
        "yer-tutucu",
        "yazi-tipi",
        "lottie",
        "maskot",
        "maskot-2",
        "maskot-3",
    ]

    /// `-ekran <ad>` ile istenen ekran; argüman yoksa nil (uygulama olağan akışla açılır).
    nonisolated static var istenenAd: String? {
        var ham: String? = nil
        let argumanlar: [String] = ProcessInfo.processInfo.arguments
        if let i = argumanlar.firstIndex(of: "-ekran"), i + 1 < argumanlar.count {
            ham = argumanlar[i + 1]
        }
        if ham == nil {
            ham = UserDefaults.standard.string(forKey: "ekran")
        }
        guard let deger = ham else { return nil }
        let ad: String = deger.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return ad.isEmpty ? nil : ad
    }

    let ad: String

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            ekran
        }
        .preferredColorScheme(.dark)
    }

    // Dallar üç gruba bölünmüştür (tek büyük `switch` derleyicinin tip çıkarımını zorlamasın).
    @ViewBuilder
    private var ekran: some View {
        if EkranGalerisi.dersAdlari.contains(ad) {
            dersEkranlari
        } else if EkranGalerisi.sekmeAdlari.contains(ad) {
            sekmeEkranlari
        } else if let ornek = GirisOrnekAdimi(rawValue: ad) {
            // Giriş ve kayıt akışının adımları ("giris", "kayit", "giris-…", "kayit-…").
            GirisAkisi(servis: SahteKimlikServisi(), girisYapildi: {}, ornek: ornek)
        } else {
            digerEkranlar
        }
    }

    private static let dersAdlari: Set<String> = [
        "abakus-ders", "abakus-sandik", "kor-ders", "sonuc-basarili", "sonuc-basarisiz",
        "sonuc-sandik-basarisiz", "sonuc-sandik", "sandik",
    ]

    private static let sekmeAdlari: Set<String> = [
        "gorevler", "gorevler-yeni", "kupa", "kupa-gecmisi", "kupa-yolu-odul", "lider-tablosu",
        "rozetler", "rozet-detay", "ozellestirme", "magaza", "magaza-alt", "profil",
    ]

    // MARK: Ders, sonuç, sandık

    @ViewBuilder
    private var dersEkranlari: some View {
        switch ad {
        case "abakus-ders":
            AbakusDersEkrani(dersBaslangici: EkranGalerisiVeri.abakusDersi, bitti: { _ in }, kapat: {})
        case "abakus-sandik":
            AbakusDersEkrani(dersBaslangici: EkranGalerisiVeri.abakusSandigi, bitti: { _ in }, kapat: {})
        case "kor-ders":
            KorDersEkrani(baslangic: EkranGalerisiVeri.korDers, bitti: { _ in }, kapat: {})
        case "sonuc-basarili":
            SonucEkrani(girdi: EkranGalerisiVeri.sonucBasarili, devam: {}, tekrar: {})
        case "sonuc-basarisiz":
            SonucEkrani(girdi: EkranGalerisiVeri.sonucBasarisiz, devam: {}, tekrar: {})
        case "sonuc-sandik-basarisiz":
            SonucEkrani(girdi: EkranGalerisiVeri.sonucSandikBasarisiz, devam: {}, tekrar: {})
        case "sonuc-sandik":
            SonucEkrani(girdi: EkranGalerisiVeri.sonucSandik, devam: {}, tekrar: {})
        default:
            // "sandik" (kristal ekranı güncel Android'de silindi)
            SandikEkrani(girdi: SandikGirdisi(baslangicNadirligi: "COMMON"), kapat: { _ in })
        }
    }

    // MARK: Sekmeler ve onlardan açılan ekranlar

    @ViewBuilder
    private var sekmeEkranlari: some View {
        switch ad {
        case "gorevler":
            // İlerlemiş kullanıcı: görev sayaçları dolu, günlük soru havuzu dolu.
            GorevlerEkrani()
        case "gorevler-yeni":
            // Hiç oynamamış kullanıcı: görev sayaçları sıfır, günlük soru havuzu boş.
            GorevlerEkrani.yeniKullanici()
        case "kupa":
            KupaEkrani(kapat: nil, kupaDersiBaslat: nil)
        case "kupa-gecmisi":
            KupaGecmisiEkrani(
                kullaniciKimligi: "sahte-kullanici",
                alan: "addition_abacus_cup",
                baslik: "Toplama",
                animasyonDosyasi: "dinosaur_anim.json",
                kapat: nil
            )
        case "kupa-yolu-odul":
            // Sahte defter: toplama yolunda 500'e kadar alınmış, 640 kupa (600 sandığı hazır).
            KupaYoluOdulEkrani(kupaAlani: "addition_abacus_cup", kapat: nil)
        case "lider-tablosu":
            KupaLiderTablosuEkrani(partId: 1, dersAnahtari: "p1_i04_unite_maratonu", dersBasligi: "Ünite Maratonu", kapat: nil)
        case "rozetler":
            RozetlerEkrani(kapat: nil, rozetSecildi: nil)
        case "rozet-detay":
            RozetDetayEkrani(modAdi: "DINO", kapat: nil)
        case "ozellestirme":
            OzellestirmeEkrani()
        case "magaza":
            MagazaEkrani(kapat: {})
        case "magaza-alt":
            // Alta kaydırılmış durum: Sobi kredisi, altın, anahtar bölümleri ve geri yükle düğmesi.
            MagazaEkrani(kapat: {}, odak: .alt)
        default:
            ProfilEkrani()
        }
    }

    // MARK: Giriş, kabuk, harita, yardımcılar

    @ViewBuilder
    private var digerEkranlar: some View {
        switch ad {
        case "kabuk":
            // Giriş atlanır; kabuk bütün geçişleriyle çalışır.
            UygulamaKokGorunumu(girisAtla: true)
        case "harita-kisim":
            EkranGalerisiHarita(durum: .kisim)
        case "harita-ders":
            EkranGalerisiHarita(durum: .dersListesi)
        case "harita-popover":
            EkranGalerisiHarita(durum: .panel)
        case "harita-kilitli-popover":
            EkranGalerisiHarita(durum: .kilitliPanel)
        case "profil-duzenle":
            EkranGalerisiProfilDuzenle()
        case "avatar":
            // Farklı avatar dizgeleriyle 12 avatar (varsayılan, maskeler, degrade, dairesiz, yakın plan).
            AvatarOrnekIzgarasi()
        case "avatar-duzenle":
            AvatarDuzenleEkrani(
                baslangic: "personas|hair=curly;eyes=happy;hairColor=6c4545;backgroundColor=c0aede",
                kaydet: { _ in },
                kapat: {}
            )
        case "yer-tutucu":
            YonetimYerTutucuEkrani(
                baslik: "Sayıları Abaküste Tanıma",
                aciklama: "Öğretici (1)",
                ayrintilar: ["Kısım: 1", "Harita sırası: 1"],
                birincilDugme: "Öğreticiyi geç, derse başla",
                birincilEylem: {},
                kapat: {}
            )
        case "yazi-tipi":
            EkranGalerisiYaziTipi()
        case "lottie":
            LottieDenemeEkrani()
        case "maskot":
            SobiMaskotDenemeEkrani(sayfa: 1)
        case "maskot-2":
            SobiMaskotDenemeEkrani(sayfa: 2)
        case "maskot-3":
            SobiMaskotDenemeEkrani(sayfa: 3)
        default:
            EkranGalerisiAdListesi(istenen: ad)
        }
    }
}

// MARK: - Örnek veri

/// Galerideki ekranların örnek girdileri.
enum EkranGalerisiVeri {
    /// Dört soruluk olağan ders (iki tek sayı, iki işlem). Harita sırası 5: abaküs ayar düğmesi görünür
    /// (bölüm 1'de ilk üç ders için gizlidir) ve iki sayılı sorularda "Sobi ile analiz et" çıkabilir.
    static var abakusDersi: AbakusDersBaslangici {
        return AbakusDersBaslangici(
            partId: 1,
            haritaSirasi: 5,
            dersTuru: 0,
            baslik: "Sayıları Abaküste Tanıma",
            ipucu: "En sağdaki sütunu kullan.",
            soruKaynagi: .hazir([
                AbakusSoru.tekSayi(7),
                AbakusSoru.tekSayi(3),
                AbakusSoru.islem(2, "+", 5),
                AbakusSoru.islem(9, "-", 4),
            ])
        )
    }

    /// Sandık dersi (ünite maratonu): süre tutulur, "Başla" düğmesiyle açılır.
    static var abakusSandigi: AbakusDersBaslangici {
        return AbakusDersBaslangici(
            partId: 1,
            haritaSirasi: 4,
            dersTuru: AbakusDersBaslangici.sandikTuru,
            baslik: "Ünite Maratonu",
            ipucu: "Hatasız, en kısa sürede bitir.",
            soruKaynagi: .hazir([
                AbakusSoru.tekSayi(12),
                AbakusSoru.tekSayi(48),
                AbakusSoru.tekSayi(305),
            ])
        )
    }

    /// Sayı kutusuyla cevaplanan kör ders: iki dizi sorusu.
    static var korDers: KorDersBaslangici {
        var b = KorDersBaslangici(partId: 4)
        b.islemler = [KorDersIslemi.dizi([3, 4, 2]), KorDersIslemi.dizi([5, 1, 3])]
        b.kor = true
        b.sayiSuresiMs = 1000
        b.ipucu = "Sayıları aklında topla."
        return b
    }

    static var sonucBasarili: SonucGirdisi {
        return SonucGirdisi(tur: .dersBasarili, dogruSayisi: 9, toplamSoru: 10)
    }

    static var sonucBasarisiz: SonucGirdisi {
        return SonucGirdisi(tur: .dersBasarisiz, dogruSayisi: 3, toplamSoru: 10)
    }

    static var sonucSandikBasarisiz: SonucGirdisi {
        return SonucGirdisi(tur: .sandikBasarisiz, dogruSayisi: 4, toplamSoru: 20)
    }

    static var sonucSandik: SonucGirdisi {
        return SonucGirdisi(
            tur: .sandikSonucu,
            dogruSayisi: 18,
            toplamSoru: 20,
            sure: "1:25",
            carpan: 2.5,
            toplamPuan: 1125,
            puanTavani: 1500,
            ikiYildizEsigi: 1000
        )
    }
}

// MARK: - Harita durumları

/// Haritayı belirli bir durumda açar; yalnızca `HaritaModeli`'nin dışa açık işlevlerini kullanır.
@MainActor
struct EkranGalerisiHarita: View {
    enum Durum {
        /// Kısım seçimi.
        case kisim
        /// 1. kısmın ders listesi.
        case dersListesi
        /// 1. kısmın ilk açık dersinin karta bağlı ders paneli.
        case panel
        /// 1. kısmın ilk kilitli dersinin paneli (anahtarla açma önerisi).
        case kilitliPanel
    }

    @StateObject private var model: HaritaModeli

    init(durum: Durum) {
        _model = StateObject(wrappedValue: EkranGalerisiHarita.hazirla(durum))
    }

    var body: some View {
        // Kendi yer tutucularıyla çalışır (yönetici yok).
        HaritaEkrani(model: model, yerTutucuGoster: true)
    }

    private static func hazirla(_ durum: Durum) -> HaritaModeli {
        let model = HaritaModeli()
        if durum == .kisim {
            return model
        }
        var ilkKisim: HaritaKisimKarti? = nil
        for satir in model.kisimSatirlari {
            for kart in satir.kartlar where kart.partId == 1 {
                ilkKisim = kart
            }
        }
        if let kart = ilkKisim {
            model.kisimSec(kart)
        }
        if durum == .panel {
            if let ders = model.ogeler.first(where: { $0.tur == .ders && $0.kilitsiz }) {
                model.ogeyeDokun(ders)
            }
        } else if durum == .kilitliPanel {
            if let ders = model.ogeler.first(where: { $0.tur == .ders && !$0.kilitsiz }) {
                model.ogeyeDokun(ders)
            }
        }
        return model
    }
}

// MARK: - Yazı tipi denetimi

/// Baloo 2 yüklendi mi: aynı satırlar solda `Tema.yazi`, sağda sistem fontuyla çizilir.
struct EkranGalerisiYaziTipi: View {
    private let boyutlar: [CGFloat] = [14, 17, 21, 28]

    private var yuklendi: Bool {
        return UIFont(name: Tema.yaziTipiAdi, size: 17) != nil
    }

    private var balooAileleri: String {
        let bulunan: [String] = UIFont.familyNames.filter { $0.lowercased().contains("baloo") }
        return bulunan.isEmpty ? "(yok)" : bulunan.joined(separator: ", ")
    }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 14) {
                Text("Yazı tipi: " + Tema.yaziTipiAdi)
                    .font(.system(size: 15, weight: .regular))
                    .foregroundColor(Color.white)
                Text(yuklendi ? "YÜKLENDİ" : "YÜKLENMEDİ (sistem fontuna düşüyor)")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(yuklendi ? Color.green : Color.red)
                Text("Sistemdeki Baloo aileleri: " + balooAileleri)
                    .font(.system(size: 13, weight: .regular))
                    .foregroundColor(Color.white.opacity(0.7))
                ForEach(boyutlar, id: \.self) { boyut in
                    EkranGalerisiYaziSatiri(boyut: boyut)
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

struct EkranGalerisiYaziSatiri: View {
    let boyut: CGFloat
    private let ornek: String = "Çağ öğüş ıİ 0123456789"

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(Int(boyut)) pt")
                .font(.system(size: 11, weight: .regular))
                .foregroundColor(Color.white.opacity(0.5))
            Text("Tema kalın: " + ornek)
                .font(Tema.yazi(boyut))
                .foregroundColor(Color.white)
            Text("Tema normal: " + ornek)
                .font(Tema.yazi(boyut, kalin: false))
                .foregroundColor(Color.white)
            Text("Sistem kalın: " + ornek)
                .font(.system(size: boyut, weight: .bold))
                .foregroundColor(Color.yellow)
        }
    }
}

// MARK: - Bilinmeyen ad

/// Tanınmayan bir ad verilirse geçerli adların listesi gösterilir.
struct EkranGalerisiAdListesi: View {
    let istenen: String

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Bilinmeyen ekran: " + istenen)
                    .font(Tema.yazi(18))
                    .foregroundColor(Color.red)
                Text("Geçerli adlar (-ekran <ad>):")
                    .font(Tema.yazi(15))
                    .foregroundColor(Color.white)
                    .padding(.top, 8)
                ForEach(EkranGalerisi.adlar, id: \.self) { ad in
                    Text(ad)
                        .font(Tema.yazi(14, kalin: false))
                        .foregroundColor(Color.white.opacity(0.85))
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

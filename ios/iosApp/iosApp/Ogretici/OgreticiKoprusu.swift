import CoreGraphics
import Foundation
import Shared

// Öğreticinin ortak Kotlin koduna (ios/shared/.../ogretici/) yaptığı BÜTÜN çağrılar bu dosyadadır;
// Kotlin → Swift adlandırmasından doğabilecek bir derleme hatası tek dosyada aransın diye
// Ogretici/ klasörünün başka hiçbir dosyası `Shared` türlerine dokunmaz (abaküs kancaları hariç:
// onlar Abakus/AbakusTahta.swift'tedir).
//
// Kotlin tarafı: OgreticiKatalogu (içerik), OgreticiOynatici (adım geçiş kuralları),
// OgreticiKurallari.sahne (bir adımın ekranı). Burada yalnızca Swift değerlerine çevrilir.

/// Android View.VISIBLE / INVISIBLE / GONE.
enum OgreticiGorunurlukTuru: Equatable {
    case gorunur
    /// Yer kaplar, çizilmez.
    case gorunmez
    /// Yer de kaplamaz.
    case yok

    var cizilir: Bool {
        return self == .gorunur
    }

    var yerKaplar: Bool {
        return self != .yok
    }
}

/// Odak çerçevesinin üst bağı (Android focusView constraint'i).
enum OgreticiOdakCapasi: Equatable {
    /// Abaküs kabında dikeyde ortalı.
    case orta
    /// `guideline`: kabın üst kenarı.
    case ustKilavuz
    /// `guideline2`: kabın %30 yüksekliği.
    case altKilavuz
}

/// Tek boncuk animasyonu (Android BeadAnimation). `tur`: 1 alt yukarı, 2 alt aşağı, 3 üst aşağı, 4 üst yukarı.
struct OgreticiHareket: Equatable {
    let cubuk: Int
    let ust: Bool
    let sira: Int
    let tur: Int
}

/// Soru metninde boyanan harf (Android questionTextColorPositions).
struct OgreticiHarfRengi: Equatable {
    let konum: Int
    /// ARGB
    let renk: UInt32
}

/// Odak çerçevesi (Android focusView); ölçüler pt, abaküs kabının sağ kenarına göre.
struct OgreticiOdakVerisi: Equatable {
    let sagBosluk: CGFloat
    let genislik: CGFloat
    let yukseklik: CGFloat
    let capa: OgreticiOdakCapasi
    let ustBosluk: CGFloat
}

/// Bir adımın gösterilecek verisi; metinler çözülmüştür (Metin.al).
struct OgreticiAdimVerisi: Equatable {
    let sira: Int
    let metin: String
    let sesAnahtari: String?
    let uzakSes: String?
    let daktilo: Bool
    let daktiloHiziMs: Int
    let soru: String?
    let soruGorunurlugu: OgreticiGorunurlukTuru
    let soruRenkleri: [OgreticiHarfRengi]
    /// Yanlış cevap panelindeki ipucu (requestText).
    let ipucu: String?
    let abakusTiklanabilir: Bool
    let kimlik: String?
    let secenekler: [String]
    let dogruSecenekler: [Int]
    let cokluSecim: Bool
    let secenekBasligi: String?
    let gecerliSecenekVar: Bool
    /// "[köşeli parantez]" işaretli kural metni (rulesStepTextContent), çözülmüş.
    let kuralMetni: String?
    let kuralMetniGorunurlugu: OgreticiGorunurlukTuru
    let kuralMetniCapasi: String?
    /// Odak dışı yerleşim değişiklikleri, ham ("tutorialText:bottomToTop=blinding_tutorial1;…").
    let yerlesimler: [String]

    static let bos = OgreticiAdimVerisi(
        sira: 0, metin: "", sesAnahtari: nil, uzakSes: nil, daktilo: false, daktiloHiziMs: 50,
        soru: nil, soruGorunurlugu: .gorunmez, soruRenkleri: [], ipucu: nil, abakusTiklanabilir: false,
        kimlik: nil, secenekler: [], dogruSecenekler: [], cokluSecim: false, secenekBasligi: nil,
        gecerliSecenekVar: false, kuralMetni: nil, kuralMetniGorunurlugu: .yok, kuralMetniCapasi: nil,
        yerlesimler: []
    )
}

/// Bir adımın ekranı (OgreticiSahnesi).
struct OgreticiSahneVerisi: Equatable {
    let abakus: OgreticiGorunurlukTuru
    let abakusKilitli: Bool
    let geri: OgreticiGorunurlukTuru
    let atla: OgreticiGorunurlukTuru
    let cikisGorunur: Bool
    let kontrolGorunur: Bool
    let devamGorunur: Bool
    let sonAdim: Bool
    /// nil: odak çerçevesi gizli.
    let odak: OgreticiOdakVerisi?
    /// Öğreticinin kural tablosu (android:id) ve bu adımdaki görünürlüğü.
    let kuralTablosu: String?
    let kuralTablosuGorunurlugu: OgreticiGorunurlukTuru
    /// Görünürlüğü işlemlerle değişmiş öteki görünümler (android:id → görünürlük).
    let gorunumler: [String: OgreticiGorunurlukTuru]

    /// Değişmemişse `varsayilan` (fragment_tutorial.xml'deki değer).
    func gorunurluk(_ gorunum: String, varsayilan: OgreticiGorunurlukTuru) -> OgreticiGorunurlukTuru {
        return gorunumler[gorunum] ?? varsayilan
    }

    static let bos = OgreticiSahneVerisi(
        abakus: .gorunmez, abakusKilitli: true, geri: .gorunmez, atla: .gorunmez, cikisGorunur: true,
        kontrolGorunur: false, devamGorunur: false, sonAdim: false, odak: nil, kuralTablosu: nil,
        kuralTablosuGorunurlugu: .yok, gorunumler: [:]
    )
}

/// Oynatıcının istediği tek iş (OgreticiEtki).
enum OgreticiIs: Equatable {
    case adimGoster(adim: Int, ileri: Bool, hareketler: [OgreticiHareket])
    case metniTamamla
    case abakusSifirla
    /// `bekle`: yazma bitmeden sıradaki işe geçilmez.
    case sayiYaz(deger: Int, bekle: Bool)
    case degerKur(deger: Int)
    case bekle(ms: Int)
    case sonucGoster(dogru: Bool)
    case sonucGizle
    case kapat
    case secimUyarisi
}

/// Kotlin OgreticiOynatici'nin sarmalayıcısı.
final class OgreticiKoprusu {
    private let oynatici: OgreticiOynatici
    private let icerik: OgreticiIcerigi
    let numara: Int

    /// İçerik yoksa (taşınmamış öğretici) nil.
    init?(numara: Int, ilkAcilis: Bool, baslangicAdimi: Int) {
        let kurulan: OgreticiOynatici? = OgreticiKatalogu.shared.oynaticiKur(
            numara: Int32(numara), ilkAcilis: ilkAcilis, baslangicAdimi: Int32(max(0, baslangicAdimi))
        )
        guard let o = kurulan else { return nil }
        self.oynatici = o
        self.icerik = o.icerik
        self.numara = numara
    }

    /// Bu numaranın içeriği taşındı mı (Yonetim yer tutucuya düşmek için sorar).
    static func icerikVar(_ numara: Int) -> Bool {
        return OgreticiKatalogu.shared.varMi(numara: Int32(numara))
    }

    var adim: Int {
        return Int(oynatici.adim)
    }

    var adimSayisi: Int {
        return Int(oynatici.adimSayisi)
    }

    // MARK: Olaylar (Android karşılıkları OgreticiOynatici.kt'de)

    func basla() -> [OgreticiIs] {
        return isler(oynatici.basla())
    }

    func dokunuldu(mesgul: Bool, daktiloSuruyor: Bool) -> [OgreticiIs] {
        return isler(oynatici.dokunuldu(mesgul: mesgul, daktiloSuruyor: daktiloSuruyor))
    }

    func geri(mesgul: Bool) -> [OgreticiIs] {
        return isler(oynatici.geri(mesgul: mesgul))
    }

    func kontrol(deger: Int) -> [OgreticiIs] {
        return isler(oynatici.kontrol(deger: Int32(deger)))
    }

    func secenekKontrol(secilenler: [Int]) -> [OgreticiIs] {
        let kutulu: [KotlinInt] = secilenler.sorted().map { KotlinInt(int: Int32($0)) }
        return isler(oynatici.secenekKontrol(secilenler: kutulu))
    }

    func dogruDevam() -> [OgreticiIs] {
        return isler(oynatici.dogruDevam())
    }

    func yanlisTamam() -> [OgreticiIs] {
        return isler(oynatici.yanlisTamam())
    }

    // MARK: Okuma

    func adimVerisi() -> OgreticiAdimVerisi {
        let sira: Int32 = oynatici.adim
        guard let a = icerik.adim(sira: sira) else { return OgreticiAdimVerisi.bos }
        return OgreticiKoprusu.cevir(a, sira: Int(sira))
    }

    func sahne() -> OgreticiSahneVerisi {
        let s: OgreticiSahnesi = oynatici.sahne()
        var gorunumler: [String: OgreticiGorunurlukTuru] = [:]
        let degisenler: [String] = s.degisenGorunumler()
        for ad in degisenler {
            let kod: Int32 = s.gorunurluk(gorunum: ad, varsayilan: OgreticiSabit.shared.GORUNUR)
            gorunumler[ad] = OgreticiKoprusu.gorunurluk(kod)
        }
        var odak: OgreticiOdakVerisi? = nil
        if s.odakGorunur {
            odak = OgreticiOdakVerisi(
                sagBosluk: CGFloat(s.odakSagBoslukDp),
                genislik: CGFloat(s.odakGenislikDp),
                yukseklik: CGFloat(s.odakYukseklikDp),
                capa: OgreticiKoprusu.capa(s.odakCapa),
                ustBosluk: CGFloat(s.odakUstBoslukDp)
            )
        }
        return OgreticiSahneVerisi(
            abakus: OgreticiKoprusu.gorunurluk(s.abakusGorunurlugu),
            abakusKilitli: s.abakusKilitli,
            geri: OgreticiKoprusu.gorunurluk(s.geriGorunurlugu),
            atla: OgreticiKoprusu.gorunurluk(s.atlaGorunurlugu),
            cikisGorunur: s.cikisGorunur,
            kontrolGorunur: s.kontrolGorunur,
            devamGorunur: s.devamGorunur,
            sonAdim: s.sonAdim,
            odak: odak,
            kuralTablosu: icerik.kuralTablosu,
            kuralTablosuGorunurlugu: OgreticiKoprusu.gorunurluk(s.kuralTablosuGorunurlugu),
            gorunumler: gorunumler
        )
    }

    // MARK: Çeviriler

    private static func gorunurluk(_ kod: Int32) -> OgreticiGorunurlukTuru {
        let sabit = OgreticiSabit.shared
        if kod == sabit.GORUNUR {
            return .gorunur
        }
        if kod == sabit.YOK {
            return .yok
        }
        return .gorunmez
    }

    private static func capa(_ kod: Int32) -> OgreticiOdakCapasi {
        let sabit = OgreticiSabit.shared
        if kod == sabit.CAPA_UST_KILAVUZ {
            return .ustKilavuz
        }
        if kod == sabit.CAPA_ALT_KILAVUZ {
            return .altKilavuz
        }
        return .orta
    }

    private static func hareket(_ h: OgreticiBoncukHareketi) -> OgreticiHareket {
        return OgreticiHareket(cubuk: Int(h.cubuk), ust: h.ust, sira: Int(h.sira), tur: Int(h.tur))
    }

    private static func cevir(_ a: OgreticiAdimi, sira: Int) -> OgreticiAdimVerisi {
        var renkler: [OgreticiHarfRengi] = []
        for r in a.soruRenkleri {
            let argb: UInt32 = UInt32(truncatingIfNeeded: r.renk)
            renkler.append(OgreticiHarfRengi(konum: Int(r.konum), renk: argb))
        }
        var secenekler: [String] = []
        for anahtar in a.secenekler {
            secenekler.append(Metin.al(anahtar))
        }
        var dogrular: [Int] = []
        for kutulu in a.dogruSecenekler {
            dogrular.append(Int(kutulu.intValue))
        }
        let ipucu: String? = a.ipucu.map { Metin.al($0) }
        let baslik: String? = a.secenekBasligi.map { Metin.al($0) }
        let kural: String? = a.kuralMetni.map { Metin.al($0) }
        return OgreticiAdimVerisi(
            sira: sira,
            metin: Metin.al(a.metin),
            sesAnahtari: a.ses,
            uzakSes: a.uzakSes,
            daktilo: a.daktilo,
            daktiloHiziMs: Int(a.daktiloHiziMs),
            soru: a.soru,
            soruGorunurlugu: gorunurluk(a.soruGorunurlugu),
            soruRenkleri: renkler,
            ipucu: ipucu,
            abakusTiklanabilir: a.abakusTiklanabilir,
            kimlik: a.kimlik,
            secenekler: secenekler,
            dogruSecenekler: dogrular,
            cokluSecim: a.cokluSecim,
            secenekBasligi: baslik,
            gecerliSecenekVar: a.gecerliSecenekVar(),
            kuralMetni: kural,
            kuralMetniGorunurlugu: gorunurluk(a.kuralMetniGorunurlugu),
            kuralMetniCapasi: a.kuralMetniCapasi,
            yerlesimler: a.yerlesimler
        )
    }

    private func isler(_ etkiler: [OgreticiEtki]) -> [OgreticiIs] {
        let k = OgreticiEtki.companion
        var sonuc: [OgreticiIs] = []
        for e in etkiler {
            let tur: Int32 = e.tur
            if tur == k.ADIM_GOSTER {
                let hareketler: [OgreticiHareket] = e.hareketler.map { OgreticiKoprusu.hareket($0) }
                sonuc.append(.adimGoster(adim: Int(e.adim), ileri: e.ileri, hareketler: hareketler))
            } else if tur == k.METNI_TAMAMLA {
                sonuc.append(.metniTamamla)
            } else if tur == k.ABAKUS_SIFIRLA {
                sonuc.append(.abakusSifirla)
            } else if tur == k.SAYI_YAZ {
                sonuc.append(.sayiYaz(deger: Int(e.deger), bekle: e.bekle))
            } else if tur == k.DEGER_KUR {
                sonuc.append(.degerKur(deger: Int(e.deger)))
            } else if tur == k.BEKLE {
                sonuc.append(.bekle(ms: Int(e.gecikmeMs)))
            } else if tur == k.SONUC_GOSTER {
                sonuc.append(.sonucGoster(dogru: e.deger == 1))
            } else if tur == k.SONUC_GIZLE {
                sonuc.append(.sonucGizle)
            } else if tur == k.KAPAT {
                sonuc.append(.kapat)
            } else if tur == k.SECIM_UYARISI {
                sonuc.append(.secimUyarisi)
            }
        }
        return sonuc
    }
}

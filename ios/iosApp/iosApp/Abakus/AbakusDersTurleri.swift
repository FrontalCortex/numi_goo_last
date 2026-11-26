import Foundation
import Shared

// Abaküs dersi ekranının giriş / çıkış türleri ve dış bağımlılık arayüzleri.
//
// Android karşılıkları:
//  - Giriş: AbacusFragment'in okuduğu genel durum (GlobalLessonData.globalPartId,
//    GlobalValues.mapFragmentStepIndex, LessonManager.getLessonItem(...)) ve "operations" argümanı.
//  - Çıkış: showLessonResult / showLessonResultFalseOnly / showChestResult'ın sonuç ekranlarına
//    Bundle ile verdiği alanlar.

/// Tek bir soru (Android: MathOperation).
struct AbakusSoru: Equatable {
    /// İki sayılı soruda sayılar; tek sayılı soruda (abaküse bu sayıyı yaz) ikisi de nil.
    let ilkSayi: Int?
    /// "+", "-", "x", "/"; tek sayılı soruda sayının metni (Android böyle saklar).
    let islec: String
    let ikinciSayi: Int?

    static func islem(_ ilk: Int, _ islec: String, _ ikinci: Int) -> AbakusSoru {
        return AbakusSoru(ilkSayi: ilk, islec: islec, ikinciSayi: ikinci)
    }

    static func tekSayi(_ sayi: Int) -> AbakusSoru {
        return AbakusSoru(ilkSayi: nil, islec: String(sayi), ikinciSayi: nil)
    }
}

/// Soruların nereden geleceği.
enum AbakusSoruKaynagi: Equatable {
    /// Ortak koddaki ders adımı tablosu (LessonOperations.getLessonOperations). Kimlik, Android'in
    /// uploadLessonData'da hesapladığı çözümlenmiş kimliktir (resolvedLessonId).
    case dersAdimi(kimlik: Int)
    /// Hazır soru listesi (Android: Fragment'a "operations" argümanıyla verilen sorular).
    case hazir([AbakusSoru])
}

/// Abaküs dersini başlatmak için gereken her şey.
struct AbakusDersBaslangici: Equatable {
    /// Android: GlobalLessonData.globalPartId.
    var partId: Int
    /// Android: GlobalValues.mapFragmentStepIndex (haritadaki sıra).
    var haritaSirasi: Int
    /// Android: LessonItem.type — 0 ders, 2 sandık (süre tutulur, "Başla" düğmesi, 50 ms boncuk).
    var dersTuru: Int
    /// Android: LessonItem.isBlinding == true. Kör dersler ayrı ekranda oynanır; burada yalnızca
    /// bilgi olarak taşınır.
    var korMu: Bool
    /// Android: GlobalValues.lessonStep'e yazılan başlangıç adımı (LessonItem.startStepNumber).
    var dersAdimi: Int
    var baslik: String
    /// Android: LessonItem.lessonHint; yoksa boş.
    var ipucu: String
    /// Android: resolveWorstCupTimeFallback() (sandık puanının çarpanı için, sn).
    var enKotuKupaSuresi: Int
    var soruKaynagi: AbakusSoruKaynagi

    static let sandikTuru: Int = 2

    init(
        partId: Int,
        haritaSirasi: Int,
        dersTuru: Int = 0,
        korMu: Bool = false,
        dersAdimi: Int = 1,
        baslik: String = "",
        ipucu: String = "",
        enKotuKupaSuresi: Int = 240,
        soruKaynagi: AbakusSoruKaynagi
    ) {
        self.partId = partId
        self.haritaSirasi = haritaSirasi
        self.dersTuru = dersTuru
        self.korMu = korMu
        self.dersAdimi = dersAdimi
        self.baslik = baslik
        self.ipucu = ipucu
        self.enKotuKupaSuresi = enKotuKupaSuresi
        self.soruKaynagi = soruKaynagi
    }

    var sandikMi: Bool {
        return dersTuru == AbakusDersBaslangici.sandikTuru
    }

    /// Haritadaki ders öğesinden başlangıç değerlerini çıkarır (Android: AbacusFragment.onCreate +
    /// uploadLessonData). Öğenin başlangıç adımı yoksa (Android'de çöker) nil döner.
    /// `hazirSorular` boş değilse tablo yerine onlar kullanılır.
    static func dersOgesinden(
        _ oge: LessonItem, partId: Int, haritaSirasi: Int, hazirSorular: [AbakusSoru] = []
    ) -> AbakusDersBaslangici? {
        guard let girdi = AbakusDersKoprusu.shared.dersGirdisi(
            lessonItem: oge, partId: Int32(partId), mapIndex: Int32(haritaSirasi)
        ) else {
            return nil
        }
        let kaynak: AbakusSoruKaynagi = hazirSorular.isEmpty
            ? .dersAdimi(kimlik: Int(girdi.lessonOperationsId))
            : .hazir(hazirSorular)
        return AbakusDersBaslangici(
            partId: partId,
            haritaSirasi: haritaSirasi,
            dersTuru: Int(girdi.lessonType),
            korMu: girdi.isBlinding,
            dersAdimi: Int(girdi.lessonStep),
            baslik: girdi.title,
            ipucu: girdi.lessonHint,
            enKotuKupaSuresi: Int(girdi.worstCupTime),
            soruKaynagi: kaynak
        )
    }
}

/// Dersten sonra açılacak ekran (Android: showQuestionPanel'deki sonuç dinleyicisi).
enum AbakusDersYonlendirmesi: Int {
    /// LessonResult
    case dersSonucu = 0
    /// LessonResultFalse (başarı < %10)
    case dersBasarisiz = 1
    /// ChestResult
    case sandikSonucu = 2
    /// LessonResultFalse, isChestFailure = true (toplam puan < 500)
    case sandikBasarisiz = 3
}

/// Ders bitince dışarı verilen ham değerler. Yıldız / kupa / ilerleme hesabı sonuç ekranınındır.
struct AbakusDersSonucu: Equatable {
    let partId: Int
    let haritaSirasi: Int
    let dersTuru: Int
    let yonlendirme: AbakusDersYonlendirmesi
    /// Android Bundle: "correctAnswers"
    let dogruSayisi: Int
    /// "totalQuestions"
    let soruSayisi: Int
    /// "successRate" (0...100)
    let basariOrani: Float
    /// "dersPuani" = (başarı × 5) tam kısmı
    let dersPuani: Int
    /// "time" — "d:ss"; sandık dışında "0:00"
    let sure: String
    /// Sürenin saniye karşılığı
    let saniye: Int
    /// "worstCupTime"
    let enKotuKupaSuresi: Int
    /// "carpan" = max(1, 4 − 3·süre/kupaSüresi)
    let carpan: Float
    /// "toplamPuan" = ceil(dersPuanı × çarpan)
    let toplamPuan: Int
}

/// Ekranın çaldığı sesler (Android: res/raw).
enum AbakusDersSesi {
    /// correct_answer_sound, ses düzeyi 0.3
    case dogru
    /// incorrect_answer_sound
    case yanlis
}

enum AbakusTitresimi {
    case dogru
    case yanlis
}

/// Ekranın dış dünyayla bütün bağlantıları. Firebase, ses ve titreşim bağlanana kadar
/// `SahteAbakusDersHizmetleri` kullanılır.
protocol AbakusDersHizmetleri: AbakusBoncukSesi {
    /// Android: AbacusFragment.isDeviceOnline. Çevrimdışıyken cevap denetlenmez.
    var cevrimici: Bool { get }

    /// Android: playSound(R.raw.correct_answer_sound, 0.3f) / playSound(R.raw.incorrect_answer_sound);
    /// AppPrefs "sound_enabled" kapalıysa çalmaz.
    func sesCal(_ ses: AbakusDersSesi)

    /// Android'de bu ekranda titreşim yok; iOS'ta eklenmek istenirse diye bırakıldı.
    func titret(_ tur: AbakusTitresimi)

    /// Android: MainActivity.showOfflineFragment().
    func cevrimdisiGoster()

    /// Android: MissionsProgressStore.recordLearningDurationMs (ekran görünürken geçen süre).
    func ogrenmeSuresiniKaydet(milisaniye: Int)

    /// Android'de enerji harita düğmesinde düşer (LessonAdapter → EnergyManager), bu ekranda
    /// değil. Ekran, ders başlarken bir kez haber verir; enerji haritada düşülüyorsa boş bırakın.
    func enerjiDus(baslangic: AbakusDersBaslangici)

    /// Android'de ilerleme sonuç ekranlarında yazılır (LessonResult / ChestResult →
    /// Firestore users/{uid}/lessonData). Ekran, ders bitince bir kez haber verir.
    func ilerlemeyiKaydet(sonuc: AbakusDersSonucu)
}

/// Hiçbir şey yapmayan örnek uygulama.
final class SahteAbakusDersHizmetleri: AbakusDersHizmetleri {
    var cevrimici: Bool = true
    private(set) var kaydedilenSureMs: Int = 0
    private(set) var sonSonuc: AbakusDersSonucu?

    init() {}

    func boncukSesi(etkinlesiyor: Bool) {}

    func sesCal(_ ses: AbakusDersSesi) {}

    func titret(_ tur: AbakusTitresimi) {}

    func cevrimdisiGoster() {}

    func ogrenmeSuresiniKaydet(milisaniye: Int) {
        kaydedilenSureMs += milisaniye
    }

    func enerjiDus(baslangic: AbakusDersBaslangici) {}

    func ilerlemeyiKaydet(sonuc: AbakusDersSonucu) {
        sonSonuc = sonuc
    }
}

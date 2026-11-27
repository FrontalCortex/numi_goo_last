import Foundation

// Kör (zihinden) ders ekranının giriş ve çıkış değerleri.
//
// Android: BlindingLessonFragment'a verilenler iki yerden gelir:
//   - arguments: "operations" (List<Any>), "cup_lesson_item" (kupa modu), günlük soru anahtarları
//     (daily_mode, daily_period_key, daily_slot_index, daily_interval_ms, daily_part_id);
//   - genel durum: GlobalLessonData.globalPartId, GlobalValues.mapFragmentStepIndex ve
//     LessonManager.getLessonItem(mapFragmentStepIndex) ile okunan LessonItem.
// Burada hepsi yalın alanlar olarak [KorDersBaslangici] içinde toplanır.

/// "operations" listesinin bir öğesi.
enum KorDersIslemi: Equatable {
    /// Ekranda sırayla gösterilen sayılar (Android: `List<Int>`); cevap toplamlarıdır.
    case dizi([Int])
    /// Çarpım sorusu (Android: `MathOperation`).
    case carpma(birinci: Int, islec: String, ikinci: Int)
}

/// Günlük soru modunun anahtarları (Android: newDailyQuestionInstance).
struct KorDersGunlukSoru: Equatable {
    /// Kart açılırken kilitlenen periyot (daily_period_key).
    var donemAnahtari: String
    /// Oturumdaki soru indeksi, 0...2 (daily_slot_index).
    var yuva: Int
    /// daily_part_id.
    var partId: Int
}

struct KorDersBaslangici {
    /// GlobalLessonData.globalPartId (9 = kupa modu).
    var partId: Int
    /// GlobalValues.mapFragmentStepIndex.
    var dersIndeksi: Int = 0
    /// LessonItem.type (0 = ders, 2 = sandık, 3 = yarış).
    var dersTuru: Int = 0
    /// Dışarıdan verilen sorular. Boşsa (ve kupa modu değilse) sorular ders adımı tablosundan yüklenir.
    var islemler: [KorDersIslemi] = []

    // LessonItem bayrakları; nil değerler Android'deki gibi anlamlıdır.

    /// isBlinding: true = sayı kutusuyla cevap; nil = abaküsle cevap.
    var kor: Bool? = nil
    /// isMultiplication.
    var carpma: Bool? = nil
    /// blindingMultiplication.
    var korCarpma: Bool? = nil
    /// isExtraction.
    var cikarma: Bool? = nil
    /// raceBusyLevel; nil = yarış dersi değil.
    var yarisYogunlugu: Int? = nil
    /// timePeriod: dizide bir sayının ekranda kalma süresi (ms); nil → 1000.
    var sayiSuresiMs: Int? = nil
    /// cupWinDelta; nil → 10.
    var kupaKazanc: Int? = nil
    /// cupLossDelta; nil → 30.
    var kupaKayip: Int? = nil

    // Soruların tablodan yüklenmesi için (uploadLessonData).

    /// startStepNumber.
    var baslangicAdimi: Int? = nil
    /// finishStepNumber.
    var bitisAdimi: Int? = nil
    /// stepCount.
    var adimSayisi: Int = 1
    /// stepIsFinish.
    var adimBitti: Bool = false

    /// LessonItem.title ("Sobi ile analiz et" penceresinin başlığı için; yoksa boş).
    var baslik: String = ""

    /// lessonHint.
    var ipucu: String? = nil
    /// worstCupTime (sandık puanı çarpanı için).
    var enKotuKupaSuresi: Int? = nil

    /// abacusGuideNumber — dersin Sobi rehberinin numarası (1...6); yoksa nil.
    var rehberNo: Int? = nil
    /// currentStep == 1 — rehber yalnızca dersin ilk adımında açılır.
    var rehberIlkAdim: Bool = false

    /// Doluysa günlük soru modu.
    var gunlukSoru: KorDersGunlukSoru? = nil

    /// Android: TasksFragment'ın kupa dersini açışı. Sayılar ve zorluk alanları çağıran tarafta
    /// ortak koddan (CupRuleEngine) üretilir; `partId` 9'dur.
    static func kupaIcin(
        islemler: [KorDersIslemi],
        kor: Bool?,
        carpma: Bool?,
        cikarma: Bool?,
        sayiSuresiMs: Int?,
        kupaKazanc: Int?,
        kupaKayip: Int?
    ) -> KorDersBaslangici {
        var b = KorDersBaslangici(partId: 9)
        b.islemler = islemler
        b.kor = kor
        b.carpma = carpma
        b.cikarma = cikarma
        b.sayiSuresiMs = sayiSuresiMs
        b.kupaKazanc = kupaKazanc
        b.kupaKayip = kupaKayip
        return b
    }

    /// Android: BlindingLessonFragment.onCreate'in günlük soru için kurduğu LessonItem
    /// (isBlinding = null → abaküsle cevap; ilk soru MathOperation ise çarpım gösterimi).
    static func gunlukSoruIcin(
        partId: Int,
        islemler: [KorDersIslemi],
        sayiSuresiMs: Int?,
        gunlukSoru: KorDersGunlukSoru
    ) -> KorDersBaslangici {
        var b = KorDersBaslangici(partId: partId)
        b.islemler = islemler
        var carpmaVar = false
        if let ilk = islemler.first, case .carpma = ilk { carpmaVar = true }
        b.korCarpma = carpmaVar
        b.carpma = carpmaVar
        b.sayiSuresiMs = sayiSuresiMs
        b.gunlukSoru = gunlukSoru
        return b
    }
}

/// Ders bitince açılacak ekran (Android: finishLessonAfterLastQuestion / showLessonResultFalse).
enum KorDersYonlendirme: Equatable {
    /// Ekran kapanır (kupa modu ya da raceBusyLevel == 0). Kupa modunda `kupaDeltasi` doludur.
    case kapat
    /// LessonResult.
    case dersSonucu
    /// LessonResultFalse (`sandikBasarisiz` = isChestFailure).
    case dersSonucuBasarisiz
    /// ChestResult; yarış dersinde (`yarisDersi`) ChestFragment.
    case sandikSonucu
    /// Günlük soru doğru tamamlandı (sonuç kaydedildi).
    case gunlukSoruTamam
    /// Günlük soru yanlış cevaplandı ya da başladıktan sonra bırakıldı (sonuç kaydedildi).
    case gunlukSoruYanlis
}

struct KorDersSonucu {
    var yonlendirme: KorDersYonlendirme

    // Sonuç ekranlarına aktarılanlar (Android: lessonResultArgs / showChestResult).

    /// correctAnswers.
    var dogruSayisi: Int
    /// totalQuestions.
    var toplamSoru: Int
    /// successRate (0...100).
    var basariOrani: Float
    /// dersPuani.
    var dersPuani: Int
    /// time / currentTime ("d:ss").
    var sure: String
    /// ChestResult'a giden worstCupTime (yedekli: güncel → şablon → 240).
    var enKotuKupaSuresi: Int
    /// carpan.
    var carpan: Float
    /// toplamPuan.
    var toplamPuan: Int
    /// isChestFailure.
    var sandikBasarisiz: Bool

    /// Yarış dersi (raceBusyLevel != nil).
    var yarisDersi: Bool
    var partId: Int
    var dersIndeksi: Int
    var dersTuru: Int

    // Kupa modu ham değerleri.

    /// Bu oturumda yazılan kupa değişimi (+kazanç / −kayıp); yazılmadıysa nil.
    var kupaDeltasi: Int?
    /// Değişimin yazıldığı bekleyen alan (CupDeltaTarget adı, ör. "PENDING_BLINDING_CUP_DELTA").
    var kupaDeltaHedefi: String?
    /// Bu oturumda harcanan enerji (kupa modunda yanlış cevap ya da bırakma: 1).
    var harcananEnerji: Int

    /// Android: lessonResultArgs'taki "questionElapsedMs" — ekrana girişten bitişe geçen süre (ms). Sonuç
    /// ekranlarına verilir ve ChestFragmentLogic.updateMapProgress(questionElapsedMs:) ile sunucuya gider.
    var soruSuresiMs: Int? = nil
}

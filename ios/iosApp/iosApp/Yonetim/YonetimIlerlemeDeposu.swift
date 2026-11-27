import Foundation
import Shared

// Ders ilerlemesinin BELLEKTEKİ durumu ve yönetimin ortak Kotlin koduna yaptığı BÜTÜN çağrılar.
// (Kotlin → Swift adlandırmasından doğabilecek bir derleme hatası tek dosyada aransın diye
// Yonetim/ klasörünün başka hiçbir dosyası `Shared` türlerinin üyelerini çağırmaz.)
//
// Firestore yok: ilerleme hiçbir yere yazılmaz, uygulama kapanınca sıfırlanır. Android'de aynı
// işleri sonuç ekranları ve ChestFragment yapar; kurallar ortak koddadır:
//   LessonResult.claim       → LessonResultLogic.onClaim
//   ChestResult.record       → ChestResultLogic.record            (ders paketi)
//   ChestResult.continue     → ChestResultLogic.shouldSkipChest / replayFinishedChest
//   ChestFragment            → ChestFragmentLogic.changeCupIcon + updateMapProgress
// Bu kurallar `GlobalLessonData`'nın bellek listesi üzerinde çalışır; liste her dersten önce
// `bellegeYukle` ile kurulur, her değişiklikten sonra `bellektenAl` ile buraya geri alınır.
// Harita bu listeyi `HaritaIlerlemeKaynagi` üzerinden okur.
final class YonetimIlerlemeDeposu: HaritaIlerlemeKaynagi {
    /// Kısım → o kısmın güncel ders listesi. Hiç oynanmamış kısım burada yoktur (şablon kullanılır).
    private var partlar: [Int32: [LessonItem]] = [:]

    init() {}

    // MARK: HaritaIlerlemeKaynagi

    func bulutOgeleri(partId: Int32) -> [LessonItem]? {
        return partlar[partId]
    }

    /// Haritada anahtarla açılan ders (Android: LessonAdapter.unlockLessonWithKey →
    /// LessonManager.updateLessonItem(context, position, unlocked)). `sira`: listedeki konum.
    func dersiYaz(partId: Int32, sira: Int32, oge: LessonItem) {
        bellegeYukle(partId: partId)
        LessonManager.shared.updateLessonItem(position: sira, item: oge, season: sezon)
        bellektenAl()
    }

    // MARK: Bellek

    /// Android: GlobalLessonData.globalPartId.
    var aktifPart: Int32 {
        return GlobalLessonData.shared.globalPartId
    }

    /// Kısmın listesini ortak kodun belleğine koyar (Android: GlobalLessonData.initialize →
    /// loadFromFirestore başarı dalı). Sink bağlı olmadığı için hiçbir yere yazılmaz.
    func bellegeYukle(partId: Int32) {
        let veri = GlobalLessonData.shared
        let liste: [LessonItem] = partlar[partId] ?? veri.createLessonItems(partId: partId)
        partlar[partId] = liste
        veri.globalPartId = partId
        _ = veri.onLoadFromFirestore(partId: partId, items: liste, error: false)
    }

    private func bellektenAl() {
        let veri = GlobalLessonData.shared
        partlar[veri.globalPartId] = veri.lessonItems
    }

    /// Android: LessonManager.getLessonItem(mapFragmentStepIndex).
    func dersOgesi(sira: Int32) -> LessonItem? {
        return LessonManager.shared.getLessonItem(position: sira)
    }

    /// Android: SeasonClock.currentSeason().
    private var sezon: Int32 {
        let simdiMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000.0)
        return SeasonClock.shared.currentSeason(nowUtcMs: simdiMs)
    }

    // MARK: Ders başlatma

    /// Android: LessonAdapter.continueWithLesson kararı — "TUTORIAL", "BLINDING_LESSON" ya da "ABACUS".
    func baslamaHedefi(_ oge: LessonItem) -> String {
        return LessonAdapterLogic.shared.continueWithLessonTarget(item: oge).name
    }

    /// Android: GlobalValues.mapFragmentStepIndex = item.mapFragmentIndex!! (yoksa liste sırası).
    func haritaSirasi(_ oge: LessonItem, listeSirasi: Int) -> Int32 {
        if let sira = oge.mapFragmentIndex {
            return sira.int32Value
        }
        return Int32(listeSirasi)
    }

    func ogreticiNumarasi(_ oge: LessonItem) -> Int {
        return Int(oge.tutorialNumber)
    }

    func korMu(_ oge: LessonItem) -> Bool {
        return oge.isBlinding?.boolValue == true
    }

    /// Android: LessonAdapterLogic.LESSON_ENERGY_COST.
    var dersEnerjiBedeli: Int {
        return Int(LessonAdapterLogic.shared.LESSON_ENERGY_COST)
    }

    /// Android: BlindingLessonFragment.onCreate'in LessonManager.getLessonItem(...) ile okuduğu alanlar.
    func korDersBaslangici(oge: LessonItem, partId: Int32, sira: Int32) -> KorDersBaslangici {
        var b = KorDersBaslangici(partId: Int(partId))
        b.dersIndeksi = Int(sira)
        b.dersTuru = Int(oge.type)
        b.kor = oge.isBlinding?.boolValue
        b.carpma = oge.isMultiplication?.boolValue
        b.korCarpma = oge.blindingMultiplication?.boolValue
        b.cikarma = oge.isExtraction?.boolValue
        b.yarisYogunlugu = oge.raceBusyLevel?.intValue
        if let sure = oge.timePeriod {
            b.sayiSuresiMs = Int(sure.int64Value)
        }
        b.kupaKazanc = oge.cupWinDelta?.intValue
        b.kupaKayip = oge.cupLossDelta?.intValue
        b.baslangicAdimi = oge.startStepNumber?.intValue
        b.bitisAdimi = oge.finishStepNumber?.intValue
        b.adimSayisi = Int(oge.stepCount)
        b.adimBitti = oge.stepIsFinish
        b.ipucu = oge.lessonHint
        b.enKotuKupaSuresi = oge.worstCupTime?.intValue
        return b
    }

    // MARK: Sandık sonucu (ChestResult)

    /// Puan çubuğunun tavanı (3 yıldız eşiği) ve 2 yıldız eşiği; bellekteki listeden çözülür.
    func puanTavani(sira: Int32) -> Int {
        return Int(SonucEkraniKoprusu.shared.puanTavani(mapFragmentStepIndex: sira))
    }

    func ikiYildizEsigi(sira: Int32, puanTavani: Int) -> Int {
        let esik: Int32 = SonucEkraniKoprusu.shared.ikiYildizEsigi(
            mapFragmentStepIndex: sira, puanTavani: Int32(puanTavani)
        )
        return Int(esik)
    }

    /// Android: ChestResult.record() — ekran açılırken rekor alanları yerinde güncellenir.
    func sandikRekorunuIsle(sira: Int32, toplamPuan: Int) {
        guard let oge = dersOgesi(sira: sira) else { return }
        _ = ChestResultLogic.shared.record(lessonItem: oge, toplamPuan: Int32(toplamPuan), season: sezon)
        bellektenAl()
    }

    /// Android: ChestResult "Devam et" — zaten bitmiş sandık tekrar oynandıysa ödül ekranı atlanır.
    func sandikTekrarOynandi(sira: Int32) -> Bool {
        guard let oge = dersOgesi(sira: sira) else { return false }
        return ChestResultLogic.shared.shouldSkipChest(lessonItem: oge)
    }

    /// Android: ChestResult.continueFragment'ın tekrar oynama dalı (rekor ve yıldız görseli).
    func tekrarSandiginiIsle(sira: Int32, toplamPuan: Int) {
        _ = ChestResultLogic.shared.replayFinishedChest(idx: sira, toplamPuan: Int32(toplamPuan), season: sezon)
        bellektenAl()
    }

    // MARK: Ders sonucu (LessonResult) ve ödül (ChestFragment)

    /// Android: LessonResult claim düğmesi. true → ders zaten bitmişti, ödül ekranı atlanır.
    /// Yan etki (Android ile aynı): öğenin `tutorialIsFinish` alanı true olur.
    func dersOduluAtlanir(sira: Int32) -> Bool {
        let karar = LessonResultLogic.shared.onClaim(lessonItem: dersOgesi(sira: sira))
        bellektenAl()
        return karar.branch.name == "SKIP_TO_MAP"
    }

    /// Android: ChestFragment — changeCupIcon + updateMapProgress (adım ilerler / ders biter /
    /// sıradaki dersin kilidi açılır). `puan`: derste ders puanı, sandıkta toplam puan.
    func ilerlet(sira: Int32, puan: Int, basariOrani: Float) {
        guard let oge = dersOgesi(sira: sira) else { return }
        let simge: String = ChestFragmentLogic.shared.changeCupIcon(lessonItem: oge, globalPartId: aktifPart).icon
        _ = ChestFragmentLogic.shared.updateMapProgress(
            mapFragmentStepIndex: sira,
            recordScore: Int32(puan),
            lessonSuccessRate: basariOrani,
            icon: simge,
            season: sezon,
            // Android: ChestFragment.questionElapsedMs (sorunun süresi, ms). iOS'ta henüz taşınmıyor: nil (bilinmiyor).
            questionElapsedMs: nil
        )
        bellektenAl()
    }
}

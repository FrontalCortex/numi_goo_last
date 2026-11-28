import Foundation
import Shared

// Rozet kazanımının (seviye atlamanın) hesaplandığı yer.
//
// Android: BadgeProgressFirestore.incrementBadgeProgressAndDetectLevelUp — `users/{uid}/badgeProgress/state`
// dokümanında transaction; ChestFragment (ders/sandık ödülü alınınca) ve TasksFragment
// (startDailyQuestionRewardFlow: günlük soru ödülü) çağırır. Sonuç kutlama kuyruğudur: her seviye atlama
// "MODE|from|to|target" biçiminde bir metin (BadgeProgressFirestore.payloadToQueueItem); kuyruk
// BadgeFragment.newLevelUpSequenceInstance ile gösterilir (iOS: RozetKutlamaEkrani).

/// Sayaçları artıracak bayraklar. Android'de ChestFragment.claim'in ve günlük soru ödülünün
/// `incrementBadgeProgressAndDetectLevelUp` çağrısına verdiği parametrelerle aynıdır.
struct RozetKazanimGirdisi: Equatable {
    /// Dart rozeti: ilk kez bitirilen ders, %100 başarı (ChestFragmentLogic.shouldIncrementDartProgress).
    var dart: Bool = false
    /// Bowling rozeti: bu işlemde tamamlanan görev sayısı (ChestFragment.countCompletedMissions).
    var bowlingAdedi: Int = 0
    /// Karate rozeti: sandıkta ilk kez 3 yıldız (ChestTypeProgressHelper.shouldIncrementKarateForFirstThreeStars).
    var karate: Bool = false
    /// Roket rozeti: günün ders sayacı (ChestFragmentLogic.updateMapProgress sonucu `incrementRocketDailyLessons`).
    var roketGunlukDers: Bool = false
    var golf: Bool = false
    /// Olta rozeti: günlük soru ödülü (TasksFragment.startDailyQuestionRewardFlow).
    var olta: Bool = false
    /// Kasırga rozeti: 7. kısım (ChestFragmentLogic.shouldIncrementTornado).
    var kasirga: Bool = false
    /// Yanardağ rozeti: 8. kısım (ChestFragmentLogic.shouldIncrementVolcano).
    var yanardag: Bool = false
    /// Günlük soru periyodu (olta serisi için); yoksa nil.
    var gunlukSoruPeriyodu: String? = nil

    /// Hiçbir sayaç artmayacaksa Android da hiç işlem başlatmaz.
    var bosMu: Bool {
        return !dart && bowlingAdedi <= 0 && !karate && !roketGunlukDers && !golf && !olta && !kasirga && !yanardag
    }
}

/// Rozet sayaçlarını sunucuda artıran servis (Firestore transaction'ı).
protocol RozetKazanimServisi: AnyObject {
    /// Sayaçları artırır ve seviye atlayan rozetlerin kutlama kuyruğunu verir (hiç yoksa boş dizi).
    /// Her öğe "MODE|from|to|target" biçimindedir (ortak kod: BadgeProgressFirestore.payloadToQueueItem).
    func sayaclariArtir(_ girdi: RozetKazanimGirdisi, tamam: @escaping ([String]) -> Void)
}

/// Bellekteki rozet belgesi üzerinde ortak kuralla çalışan sahte servis.
///
/// Belge, `KupaOrtami.sahte` içindeki rozet deposunun örnek `badgeProgress/state` dokümanıdır; böylece Rozetler
/// ve rozet ayrıntı ekranları da yeni sayıları gösterir. Hesap tamamen ortak koddadır:
/// `BadgeProgressFirestore.computeIncrementBadgeProgressTransaction` (transaction gövdesi) ve
/// `finishIncrementBadgeProgress` (bellek durumu ve kutlama listesi). Uygulama kapanınca sıfırlanır.
final class SahteRozetKazanimServisi: RozetKazanimServisi {
    /// Uygulamanın tek sahte rozet kazanım servisi.
    static let ortak: SahteRozetKazanimServisi = SahteRozetKazanimServisi()

    private let kopru: KupaEkranKoprusu = KupaEkranKoprusu.shared

    func sayaclariArtir(_ girdi: RozetKazanimGirdisi, tamam: @escaping ([String]) -> Void) {
        if girdi.bosMu {
            DispatchQueue.main.async {
                tamam([])
            }
            return
        }
        let depo: SahteKupaRozetDeposu? = KupaOrtami.sahte.rozetDeposu as? SahteKupaRozetDeposu
        var belge: [String: Any] = depo?.dokuman ?? [:]
        // Android'de bellek durumu (BadgeProgressRepository) uygulama açılışında dokümandan kurulmuştur.
        kopru.rozetDurumunuYukle(doc: belge)

        let simdiMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000.0)
        let periyot: String = FishingStreakPeriod.shared.currentPeriodKey(nowMs: simdiMs)
        let sonuc: BadgeIncrementTransactionResult = BadgeProgressFirestore.shared.computeIncrementBadgeProgressTransaction(
            snapshot: belge,
            incrementDart: girdi.dart,
            incrementBowlingBy: Int32(girdi.bowlingAdedi),
            incrementKarate: girdi.karate,
            incrementRocketDailyLessons: girdi.roketGunlukDers,
            incrementGolf: girdi.golf,
            incrementFishing: girdi.olta,
            incrementTornado: girdi.kasirga,
            incrementVolcano: girdi.yanardag,
            dailyQuestionPeriodKey: girdi.gunlukSoruPeriyodu,
            today: SahteRozetKazanimServisi.gunKimligi(),
            currentPeriodKey: periyot
        )
        // transaction.set(docRef, updateMap, SetOptions.merge())
        for (alan, deger) in sonuc.updateMap {
            belge[alan] = deger
        }
        depo?.dokuman = belge

        let yukler: [BadgeLevelUpPayload] = BadgeProgressFirestore.shared.finishIncrementBadgeProgress(
            result: sonuc,
            incrementDart: girdi.dart,
            incrementBowlingBy: Int32(girdi.bowlingAdedi),
            incrementKarate: girdi.karate,
            incrementGolf: girdi.golf,
            incrementFishing: girdi.olta,
            incrementTornado: girdi.kasirga,
            incrementVolcano: girdi.yanardag
        )
        var kuyruk: [String] = []
        for yuk in yukler {
            kuyruk.append(BadgeProgressFirestore.shared.payloadToQueueItem(payload: yuk))
        }
        // Ağ gecikmesini taklit eder (Android: Firestore round-trip).
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            tamam(kuyruk)
        }
    }

    /// Kupa dersi sonrası hayvan rozeti (Dino/Timsah/Keçi/Kartal/Sinek/Kaplumbağa) senkronu.
    /// Android: BadgeProgressFirestore.sync{Dino,...}ProgressAndDetectLevelUp — transaction gövdesi
    /// `computeCupBadgeProgressSync` (yeni puan kayıtlı ilerlemeden büyükse alan yazılır), kutlama zinciri
    /// `resolveCupBadgeLevelUpChain`. Sonuç "MODE|from|to|target" kuyruğudur (hiç yoksa boş dizi).
    func kupaRozetiniSenkronla(mod: BadgeAnimMode, yeniSkor: Int, tamam: @escaping ([String]) -> Void) {
        let firestore = BadgeProgressFirestore.shared
        guard let alan: String = firestore.cupBadgeProgressField(mode: mod) else {
            DispatchQueue.main.async {
                tamam([])
            }
            return
        }
        let depo: SahteKupaRozetDeposu? = KupaOrtami.sahte.rozetDeposu as? SahteKupaRozetDeposu
        var belge: [String: Any] = depo?.dokuman ?? [:]
        let sonuc: CupBadgeSyncResult = firestore.computeCupBadgeProgressSync(
            field: alan, snapshot: belge, newCupScore: Int32(max(0, yeniSkor))
        )
        if let yazilacak = sonuc.updateMap {
            for (ad, deger) in yazilacak {
                belge[ad] = deger
            }
            depo?.dokuman = belge
        }
        let yukler: [BadgeLevelUpPayload] = firestore.resolveCupBadgeLevelUpChain(
            mode: mod, before: sonuc.before, after: sonuc.after
        )
        var kuyruk: [String] = []
        for yuk in yukler {
            kuyruk.append(firestore.payloadToQueueItem(payload: yuk))
        }
        // Ağ gecikmesini taklit eder (Android: Firestore round-trip).
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            tamam(kuyruk)
        }
    }

    /// Android `MissionsProgressStore.calendarDayId()` / `kupaCalendarDayId`: "yıl-yılınGünü" (sıfır dolgusuz,
    /// ör. "2026-332"); roket rozetinin günlük sayacı bu anahtarla sıfırlanır.
    private static func gunKimligi() -> String {
        let takvim = Calendar(identifier: Calendar.Identifier.gregorian)
        let simdi = Date()
        let yil: Int = takvim.component(Calendar.Component.year, from: simdi)
        let gun: Int = takvim.ordinality(of: Calendar.Component.day, in: Calendar.Component.year, for: simdi) ?? 1
        return "\(yil)-\(gun)"
    }
}

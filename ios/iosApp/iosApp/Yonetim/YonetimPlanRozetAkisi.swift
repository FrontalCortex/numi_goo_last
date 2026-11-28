import Foundation
import Shared
import SwiftUI

// Plan karşılaştırma ekranı ve rozet kutlamasının uygulamaya bağlandığı yerler.
//
// Android karşılıkları:
//   ShopFragment.shopSuperCard / shopProCard  → planKarsilastirmaAc (ProDiffirentFragment.newInstance(PRO_ENTRY_SHOP))
//   ChestFragment.claim → incrementBadgeProgressAndDetectLevelUp → MainActivity.enqueuePendingBadgePayloads
//                                              → rozetSayaclariniArtir (sonuç: YonetimRozetKuyrugu.bekleyen)
//   MainActivity.runPostLessonQueue → showBadgeStep → showNewStreakStep → dersSonrasiKuyruguIsle
//   BadgeProgressFirestore.openBadgeCelebration → rozetKutlamasiniAc (görev / günlük soru ödülü: TasksFragment)
//
// Ders sonrası sıra (Android): reklam → sezon kapısı → ROZET → yeni seri → öğretmene sorma → puanlama →
// maraton rehberi → kupa yolu. iOS'ta bunlardan rozet ve yeni seri var. Rozet kutlaması kapanınca sıra
// kaldığı yerden devam eder (Android: kuyruk "bir şeyler yatıştı" diyen her yerden dürtülür).
//
// Plan ekranına şu an yalnızca mağaza açar (Süper kart ve Pro kartı). Android'de ProDiffirentFragment ayrıca
// reklam sonrası paneli (AdSkipFragment), "öğretmene sor" tanıtımı (AskQuestionOpenFragment) ve Sobi deneme
// penceresinden (SobiProTrialFragment) açılır; bu üç ekran iOS'ta henüz yok.

/// Ders sonrası kuyruğun "rozet" adımının bekleyen listesi (Android: MainActivity.pendingBadgeStringPayloadsForAd).
/// Her öğe "MODE|from|to|target". Sandık kapanınca (dersSonrasiKuyruguIsle) sırası gelir.
final class YonetimRozetKuyrugu {
    static let ortak: YonetimRozetKuyrugu = YonetimRozetKuyrugu()
    var bekleyen: [String] = []
}

extension UygulamaYoneticisi {

    // MARK: Plan karşılaştırma

    /// Mağazadaki Süper kart ve Pro kartı (Android: ProDiffirentFragment.show). Zaten açıksa tekrar açmaz.
    func planKarsilastirmaAc() {
        if let ust = katmanlar.last, case .plan = ust.ekran {
            return
        }
        ac(.plan)
    }

    // MARK: Rozet kutlaması

    /// Kutlama kuyruğunu tam ekran açar (Android: BadgeProgressFirestore.openBadgeCelebration). `bitti`: kuyruğun
    /// son rozetine dokunulup ekran kapandıktan sonra çalışır.
    func rozetKutlamasiniAc(_ kuyruk: [String], bitti: @escaping () -> Void) {
        if kuyruk.isEmpty {
            return
        }
        ac(.rozetKutlama(kuyruk: kuyruk, bitti: bitti))
    }

    // MARK: Ders sonrası sıra

    /// Ders ödülü alınırken (Android: ChestFragment.claim) hangi rozet sayaçlarının artacağı. Ders ilerlemesi
    /// (`ilerlet`) öğeyi değiştirdiği için ÖNCE çağrılır. Bowling (tamamlanan görev sayısı) ve golf iOS'ta bu
    /// akışta yoktur; roket bayrağı `ilerlet`in dönüş değerinden eklenir.
    func rozetGirdisiniHazirla(baglam: YonetimSonucBaglami, puan: Int) -> RozetKazanimGirdisi {
        var girdi = RozetKazanimGirdisi()
        let oge: LessonItem? = ilerleme.dersOgesi(sira: baglam.sira)
        let part: Int32 = ilerleme.aktifPart
        let mantik = ChestFragmentLogic.shared
        girdi.dart = mantik.shouldIncrementDartProgress(lessonBeforeClaim: oge, lessonSuccessRate: baglam.basariOrani)
        girdi.kasirga = mantik.shouldIncrementTornado(globalPartId: part)
        girdi.yanardag = mantik.shouldIncrementVolcano(globalPartId: part)
        if let bulunan = oge {
            girdi.karate = ChestTypeProgressHelper.shared.shouldIncrementKarateForFirstThreeStars(
                item: bulunan,
                recordScoreThisRun: Int32(puan),
                globalPartId: part
            )
        }
        return girdi
    }

    /// Sayaçları artırır; seviye atlayan rozetler ders sonrası kuyruğun rozet adımına eklenir
    /// (Android: incrementBadgeProgressAndDetectLevelUp → enqueuePendingBadgePayloads).
    func rozetSayaclariniArtir(_ girdi: RozetKazanimGirdisi) {
        if girdi.bosMu {
            return
        }
        SahteRozetKazanimServisi.ortak.sayaclariArtir(girdi) { [weak self] kuyruk in
            if kuyruk.isEmpty {
                return
            }
            YonetimRozetKuyrugu.ortak.bekleyen.append(contentsOf: kuyruk)
            // Android: enqueuePendingBadgePayloads her gelişte kuyruğu dürter (pumpPostLessonQueue). Sunucu yanıtı
            // sandık kapandıktan SONRA gelirse rozet bir sonraki derse kadar beklemesin.
            self?.gecKalanRozetiDene()
        }
    }

    /// Rozet yükü, ders sonrası kuyruğun ilk turundan sonra geldiyse kuyruğu yeniden dürter. Üstte ders, sonuç,
    /// sandık ya da başka bir kutlama varsa dokunmaz: onların kapanışı kuyruğu zaten işler.
    private func gecKalanRozetiDene() {
        if let ust = katmanlar.last {
            switch ust.ekran {
            case .abakusDers, .korDers, .ogretici, .sonuc, .sandik, .rozetKutlama:
                return
            default:
                break
            }
        }
        dersSonrasiKuyruguIsle()
    }

    /// Ders sonrası kuyruğun bir turu (Android: MainActivity.runPostLessonQueue): önce rozet kutlaması, yoksa
    /// yeni seri sorusu. Rozet ekranı kapanınca yeniden çağrılır, sıra böylece devam eder.
    func dersSonrasiKuyruguIsle() {
        if asama != .kabuk {
            return
        }
        if rozetAdiminiDene() {
            return
        }
        seriSorusunuDene()
    }

    /// Android: showBadgeStep. Bekleyen rozet varsa kutlamayı açar ve true döner.
    private func rozetAdiminiDene() -> Bool {
        let bekleyen: [String] = YonetimRozetKuyrugu.ortak.bekleyen
        if bekleyen.isEmpty {
            return false
        }
        YonetimRozetKuyrugu.ortak.bekleyen = []
        ac(.rozetKutlama(kuyruk: bekleyen, bitti: { [weak self] in
            self?.dersSonrasiKuyruguIsle()
        }))
        return true
    }
}

// MARK: - Katman görünümleri

extension YonetimKatmanGorunumu {
    /// YonetimEkrani.plan
    var planKatmani: some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return PlanKarsilastirmaEkrani(
            servisler: MagazaBagimliliklar.sahte,
            kapat: { nesne.kapat(no) }
        )
    }

    /// YonetimEkrani.rozetKutlama
    func rozetKutlamaKatmani(_ kuyruk: [String], _ sonra: @escaping () -> Void) -> some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        // Rozet sesi (Android: BadgeSoundPlayer) gerçek çalıcıyla: Kupa/KupaSesCaliciGercek.swift.
        return RozetKutlamaEkrani(
            kuyruk: kuyruk,
            ortam: KupaOrtami.gercekSesli,
            bitti: {
                nesne.kapat(no)
                sonra()
            }
        )
    }
}

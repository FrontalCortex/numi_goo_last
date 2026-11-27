import Foundation
import Shared

// Görevler ekranının veri katmanı.
//
// Ortak Kotlin koduna yapılan BÜTÜN çağrılar bu dosyadadır; görünümler ve görünüm modeli yalnızca
// aşağıdaki Swift yapılarını okur. Kurallar ortak koddadır: MissionsProgressStore (görev seçimi,
// sayaçlar, gün/hafta sıfırlama, güvenilir saat), MissionUiLogic (satır durumu, geri sayım,
// ilerleme çubuğu ölçüleri) ve GorevGunlukSoruKopruleri.kt (MissionsSection.bind'in
// okuma sırası).

// MARK: - Görünümlerin okuduğu veri yapıları

/// Görev satırının durumu (ortak kod: MissionQuestState).
enum GorevlerSatirDurumu {
    /// Ödül alınmış.
    case alindi
    /// Hedefe ulaşılmış, ödül bekliyor.
    case bitti
    /// Sürüyor.
    case suruyor
}

/// Tek bir görev satırı (ortak kod: MissionQuestData).
struct GorevlerSatiri: Identifiable {
    /// Pencere + görev kimliği ("WEEKLY_finish_lessons_20").
    let id: String
    let gorevKimligi: String
    /// Görev başlığının string kaynağı adı.
    let baslikAdi: String
    let ilerleme: Int
    let hedef: Int
    /// Çubuğa verilen tam sayı yüzde (0–100).
    let yuzde: Int
    let durum: GorevlerSatirDurumu
    /// Ödül sandığının görsel adı.
    let simge: String
    /// Satıra dokununca ödül sandığı açılabilir mi.
    let alinabilir: Bool
    let haftalik: Bool
    /// Ödül sandığının başlangıç nadirliği (sunucuya giden ad: "COMMON" / "RARE").
    let sandikNadirligi: String
    /// Ortak koddaki pencere (ödülü işaretlerken geri verilir).
    let pencere: MissionWindow
}

/// Bölüm başlığındaki geri sayım: string kaynağı adı + sayı ("missions_hours_short", 5 → "5 SA.").
struct GorevlerGeriSayim {
    let metinAdi: String
    let deger: Int
}

/// Ekranın tek seferde okunan durumu.
struct GorevlerDurumu {
    let haftalik: [GorevlerSatiri]
    let gunluk: [GorevlerSatiri]
    let haftalikSayac: GorevlerGeriSayim
    let gunlukSayac: GorevlerGeriSayim
}

/// İlerleme çubuğunun dolgu ve parlama ölçüleri (ortak kod: MissionProgressOverlay).
struct GorevlerCubukOlcusu {
    let dolguGenisligi: CGFloat
    let dolguGorunur: Bool
    let parlamaGenisligi: CGFloat
    let parlamaGorunur: Bool
    /// Renk kaynağı adları.
    let dolguRengi: String
    let parlamaRengi: String
}

// MARK: - Veri kaynağı

/// Görev ilerlemesinin kaynağı.
///
/// Android: MissionsProgressStore — yerelde SharedPreferences `missions_progress_<uid|guest>`,
/// bulutta Firestore `users/{uid}/missionProgress/state` belgesi (sayaç alanları, `day_id`,
/// `week_id`, seçili görev kimlikleri, `rewardClaimedKeys`).
protocol GorevlerVeriKaynagi: AnyObject {
    /// MissionsSection.bind: sayaçlar, seçili görevler, ödül durumu, geri sayımlar.
    func durumuOku() -> GorevlerDurumu
    /// MissionsProgressStore.markMissionRewardClaimed (sandık kapanınca çağrılır).
    func odulAlindiIsaretle(_ satir: GorevlerSatiri)
}

/// Ödül sandığıyla ilgili sunucu çağrıları.
///
/// Android: ServerRewards.prefetchChest → Cloud Function `openChest` (`startRarity` gövdesiyle).
/// Zar sunucuda atılır ve bakiye orada yazılır; bu ekran yalnızca isteği erkenden başlatır.
protocol GorevlerOdulServisi: AnyObject {
    /// Sandık ekranı açılmadan hemen önce çağrılır (ilk açılıştaki gecikmeyi gizlemek için).
    func sandikOnIstegi(nadirlik: String)
}

/// Sunucu bağlanana dek: hiçbir şey yapmaz. Bağlanırken uygulama genelinde TEK bir
/// ServerRewards.ChestPrefetcher örneğinin `prefetchChest(startRarity:adNonce:)` çağrısına
/// yönlendirilmelidir (sandık ekranı aynı örnekten `openChest` ile sonucu alır).
final class SahteGorevlerOdulServisi: GorevlerOdulServisi {
    func sandikOnIstegi(nadirlik: String) {}
}

/// Firestore ve kalıcı depo bağlanana dek kullanılan kaynak: ortak koddaki gerçek
/// MissionsProgressStore çalışır, ama tercih dosyaları ve bulut belgesi bellektedir
/// (uygulama kapanınca sıfırlanır). `ilerlemis` ise örnek sayaçlar "buluttan gelmiş" gibi
/// yüklenir; değilse hiç oynamamış kullanıcı gibi bütün sayaçlar sıfırdır.
final class SahteGorevlerVeriKaynagi: GorevlerVeriKaynagi {
    /// Sekme değişince ilerleme kaybolmasın diye tek örnek (ilerlemiş kullanıcı).
    static let ortak = SahteGorevlerVeriKaynagi(ilerlemis: true)

    private let depo: MissionsProgressStore

    init(ilerlemis: Bool) {
        // Anahtarlar MissionsProgressStore.KEY_*_COUNT sabitleridir (Firestore alan adları).
        var ornek: [String: KotlinInt] = [:]
        if ilerlemis {
            ornek = [
                "daily_step_finish_count": KotlinInt(int: 1),
                "weekly_step_finish_count": KotlinInt(int: 3),
                "daily_step_increment_count": KotlinInt(int: 5),
                "weekly_step_increment_count": KotlinInt(int: 12),
                "daily_perfect_step_increment_count": KotlinInt(int: 1),
                "weekly_perfect_step_increment_count": KotlinInt(int: 4),
                "daily_chest_record_break_count": KotlinInt(int: 0),
                "weekly_chest_record_break_count": KotlinInt(int: 1),
                "daily_chest_star_gain_count": KotlinInt(int: 2),
                "weekly_chest_star_gain_count": KotlinInt(int: 3),
                "daily_learn_minutes_count": KotlinInt(int: 6),
                "weekly_learn_minutes_count": KotlinInt(int: 25),
            ]
        }
        depo = GorevGunlukSoruKopruleriKt.bellekIciGorevDeposuKur(
            elapsedRealtimeMs: { KotlinLong(longLong: GorevlerSaat.acilistanBeriMs()) },
            ornekSayaclar: ornek
        )
    }

    func durumuOku() -> GorevlerDurumu {
        let ham = GorevGunlukSoruKopruleriKt.gorevEkraniDurumu(store: depo)
        return GorevlerCeviri.durum(ham)
    }

    func odulAlindiIsaretle(_ satir: GorevlerSatiri) {
        depo.markMissionRewardClaimed(window: satir.pencere, missionId: satir.gorevKimligi)
    }
}

// MARK: - Ortak koddan Swift yapılarına çeviri

enum GorevlerCeviri {
    static func durum(_ ham: GorevEkraniDurumu) -> GorevlerDurumu {
        var haftalik: [GorevlerSatiri] = []
        for veri in ham.weekly {
            haftalik.append(satir(veri))
        }
        var gunluk: [GorevlerSatiri] = []
        for veri in ham.daily {
            gunluk.append(satir(veri))
        }
        return GorevlerDurumu(
            haftalik: haftalik,
            gunluk: gunluk,
            haftalikSayac: sayac(ham.weeklyCountdown),
            gunlukSayac: sayac(ham.dailyCountdown)
        )
    }

    static func sayac(_ etiket: MissionCountdownLabel) -> GorevlerGeriSayim {
        return GorevlerGeriSayim(metinAdi: etiket.resKey, deger: Int(etiket.value))
    }

    static func satir(_ veri: MissionQuestData) -> GorevlerSatiri {
        let hamDurum: MissionQuestState = veri.state
        var durum: GorevlerSatirDurumu = .suruyor
        if hamDurum == MissionQuestState.claimed {
            durum = .alindi
        } else if hamDurum == MissionQuestState.done {
            durum = .bitti
        }
        let pencere: MissionWindow = veri.window
        let haftalik: Bool = pencere == MissionWindow.weekly
        let nadirlik: ChestRarity = MissionUiLogic.shared.chestStartRarityForWindow(window: pencere)
        // PENDING etiketindeki "%d / %d" argümanları ortak koddan gelir.
        let etiket = veri.progressLabelArgs
        let ilerleme: Int = Int(etiket.first?.int32Value ?? veri.progress)
        let hedef: Int = Int(etiket.second?.int32Value ?? veri.target)
        return GorevlerSatiri(
            id: pencere.name + "_" + veri.missionId,
            gorevKimligi: veri.missionId,
            baslikAdi: veri.titleResId,
            ilerleme: ilerleme,
            hedef: hedef,
            yuzde: Int(veri.percent),
            durum: durum,
            simge: veri.iconRes,
            alinabilir: veri.canClaim,
            haftalik: haftalik,
            sandikNadirligi: nadirlik.name,
            pencere: pencere
        )
    }
}

// MARK: - İlerleme çubuğu ölçüleri

enum GorevlerCubukHesabi {
    /// dimens.xml: mission_progress_shine_inset_start = 14dp, mission_progress_shine_gap_end = 10dp.
    static let parlamaBaslangicBoslugu: Int32 = 14
    static let parlamaBitisBoslugu: Int32 = 10

    /// MissionProgressLayout.applyMissionProgressOverlayNow. Genişlik henüz yoksa nil.
    static func hesapla(genislik: CGFloat, satir: GorevlerSatiri) -> GorevlerCubukOlcusu? {
        let iz: Int32 = Int32(genislik.rounded(.down))
        let sonuc = MissionUiLogic.shared.missionProgressOverlay(
            trackWidth: iz,
            percent: Float(satir.yuzde),
            done: satir.durum == .bitti,
            claimed: satir.durum == .alindi,
            startInset: parlamaBaslangicBoslugu,
            endGap: parlamaBitisBoslugu
        )
        guard let katman = sonuc else {
            return nil
        }
        return GorevlerCubukOlcusu(
            dolguGenisligi: CGFloat(katman.fillWidth),
            dolguGorunur: katman.fillVisible,
            parlamaGenisligi: CGFloat(katman.shineWidth),
            parlamaGorunur: katman.shineVisible,
            dolguRengi: renkAdi(katman.fillDrawable),
            parlamaRengi: renkAdi(katman.shineDrawable)
        )
    }

    /// Android'de şekil drawable'ı olan dolguların (res/drawable/mission_progress_*.xml) düz rengi.
    static func renkAdi(_ sekil: String) -> String {
        switch sekil {
        case "mission_progress_fill_blue": return "missions_progress_blue"
        case "mission_progress_fill_gold": return "missions_progress_complete"
        case "mission_progress_fill_claimed": return "button_disabled"
        case "mission_progress_shine_blue": return "missions_progress_blue_shine"
        case "mission_progress_shine_gold": return "missions_progress_gold_shine"
        case "mission_progress_shine_claimed": return "chest_star_light_gray"
        default: return "missions_track"
        }
    }
}

// MARK: - Saat

enum GorevlerSaat {
    /// Android `SystemClock.elapsedRealtime()` karşılığı: cihaz açılışından beri geçen, uykuyu da
    /// sayan tekdüze süre (ms). MissionsProgressStore bunu cihaz saatiyle oynanmasına karşı kullanır.
    static func acilistanBeriMs() -> Int64 {
        let nano: UInt64 = clock_gettime_nsec_np(CLOCK_MONOTONIC_RAW)
        return Int64(nano / 1_000_000)
    }
}

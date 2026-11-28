import Foundation
import Shared

// Günlük soru (Görevler sekmesi) veri katmanı.
//
// Ortak Kotlin koduna yapılan BÜTÜN çağrılar bu dosyadadır. Kurallar ortak koddadır:
// DailyQuestionPeriod (periyot anahtarı, geri sayım), DailyQuestionCardLogic (kart tıklama kararları,
// ilerleme planı, kırık kalp sırası), DailyQuestionRepository (belge eşlemesi), DailyQuestionPoolBuilder
// (soru havuzu) ve GorevGunlukSoruKopruleri.kt (TasksFragment ile
// DailyQuestionRepository içinde kalmış akışların kopyası).

// MARK: - Görünümlerin okuduğu veri yapıları

/// Günlük soru kartının metinleri (ortak kod: DailyQuestionCardUiState).
struct GunlukSoruKartGorunumu {
    /// Ünite adı; boşsa varsayılan alt başlık gösterilir.
    let uniteBasligi: String
    /// "Seviye: …" etiketinin içine konacak metin; nil ise etiket gizlenir.
    let odakMetni: String?
}

/// Kart ilerleme çubuğunun o anki çizimi (ortak kod: GunlukSoruCubukGorunumu).
struct GunlukSoruCubukCizimi {
    /// Renk kaynağı adları.
    let zeminRengi: String
    let dolguRengi: String
    let parlamaRengi: String
    let dolguGenisligi: CGFloat
    let dolguGorunur: Bool
    let parlamaGenisligi: CGFloat
    let parlamaGorunur: Bool
    let etiket: String
    let etiketRengi: String

    static let bos = GunlukSoruCubukCizimi(
        zeminRengi: "daily_question_progress_track",
        dolguRengi: "daily_question_progress_fill",
        parlamaRengi: "daily_question_progress_shine",
        dolguGenisligi: 0,
        dolguGorunur: false,
        parlamaGenisligi: 0,
        parlamaGorunur: false,
        etiket: "",
        etiketRengi: "white"
    )
}

/// Çarpma sorusu (ortak kod: MathOperation).
struct GunlukSoruIslemi {
    let ilk: Int
    let islec: String
    let ikinci: Int
}

/// Bir günlük soruyu oynatmak için kör ders ekranına verilen her şey.
/// Android: BlindingLessonFragment.newDailyQuestionInstance(operations, periodKey, slotIndex,
/// displayIntervalMs, partId).
struct GunlukSoruBaslangici: Identifiable {
    let id = UUID()
    /// Sayı dizisi sorusu (art arda gösterilen sayılar); çarpma sorusunda boş.
    let dizi: [Int]
    /// Çarpma sorusu; dizi sorusunda nil.
    let islem: GunlukSoruIslemi?
    /// Sorunun ait olduğu periyot ("period_20419").
    let periodKey: String
    /// Üç sorudan hangisi (0..2).
    let slotIndex: Int
    /// Sayıların ekranda kalma süresi (ms); nil ise dersin varsayılanı.
    let gosterimAraligiMs: Int?
    /// Sorunun üretildiği part (1..3); kör ders ekranı kural tablosunu buna göre seçer.
    let partId: Int
    /// Soru bitince ders ekranı tarafından çağrılır: true = doğru, false = yanlış.
    /// Sonuç belgeye işlenir ve kart yenilenir.
    let sonuc: (Bool) -> Void
}

/// Meydan okumanın yüklenme sonucu.
enum GunlukSoruYukleme {
    /// Havuz boş (bitmiş sandık yok) ya da meydan okuma üretilemedi.
    case havuzBos
    case hazir(DailyQuestionChallenge)
}

// MARK: - Protokoller

/// Günlük soru verisinin kaynağı.
///
/// Android: DailyQuestionRepository —
///  - meydan okuma: Firestore `users/{uid}/dailyQuestion/current` (alanlar: periodKey, solvedCount,
///    rewardClaimed, pendingContinueSlotIndex, questions[]), yerel kopyası SharedPreferences
///    `daily_question_prefs` içinde `daily_challenge_<uid>_current`;
///  - küresel başarı oranı: `successRate/dailyQuestionSuccessRate/generatorForPart{N}Chest/{ünite}`
///    belgesindeki `<easy|medium|hard>_rate`;
///  - soru havuzu: part 1–3 ders listeleri (GlobalLessonData.loadLessonItemsForPart).
protocol GunlukSoruDeposu: AnyObject {
    /// Kırık kalp durumu (SharedPreferences `daily_question_prefs`, `daily_broken_heart_*`).
    var kalpDeposu: DailyQuestionBrokenHeartStore { get }
    /// Kart ilerleme animasyonunun son gösterilen değeri (`daily_card_anim_shown_*`).
    var animDeposu: DailyQuestionCardProgressAnimStore { get }

    /// TasksFragment.refreshDailyQuestionCard: loadDailyQuestionSources + loadChallengeForCard.
    func kartiYukle(tamam: @escaping (DailyQuestionCardUiState) -> Void)
    /// TasksFragment.handleDailyQuestionCardClick: loadDailyQuestionSources + loadOrCreateChallenge.
    func meydanOkumayiYukle(tamam: @escaping (GunlukSoruYukleme) -> Void)
    /// DailyQuestionRepository.markRewardClaimed.
    func odulAlindiIsaretle(periodKey: String)
    /// DailyQuestionRepository.clearPendingDiamondContinue.
    func devamBeklemesiniKaldir(periodKey: String, tamam: @escaping () -> Void)
    /// BlindingLessonFragment.handleDailyQuestionWrongAnswer / handleDailyQuestionLessonComplete:
    /// recordQuestionResult + markPendingDiamondContinue ya da incrementSolvedCount.
    func soruSonucunuYaz(periodKey: String, slotIndex: Int, dogru: Bool, tamam: @escaping () -> Void)
}

/// Anahtar bakiyesi.
/// Android: MainActivity.spendKeys → Firestore `users/{uid}.keys`, Cloud Function `updateUserWallet`
/// (gövde: UserWalletFirestore.applyKeyDeltaPayload, gerekçe WalletReason.SPEND).
protocol GunlukSoruCuzdani: AnyObject {
    /// Bakiye yetiyorsa düşer ve true döner; yetmiyorsa hiçbir şey yapmaz, false döner.
    func anahtarHarca(_ adet: Int) -> Bool
}

/// Ödülle ilgili sunucu çağrıları.
protocol GunlukSoruOdulServisi: AnyObject {
    /// Android: BadgeProgressFirestore.incrementBadgeProgressAndDetectLevelUp(incrementFishing = true,
    /// dailyQuestionPeriodKey = …) — balıkçılık rozeti serisi; seviye atlama varsa rozet kuyruğu döner.
    func rozetIlerlet(periodKey: String, tamam: @escaping ([String]) -> Void)
    /// Android: ServerRewards.prefetchChest → Cloud Function `openChest`.
    func sandikOnIstegi(nadirlik: String)
}

// MARK: - Sahte uygulamalar

/// Firestore bağlanana dek: ortak koddaki gerçek kurallar çalışır, belge bellekte durur.
/// Ders listeleri yerel şablondan gelir. Güncel şablonda hiçbir sandık bitmiş gelmediği için havuz
/// boştur (hiç oynamamış kullanıcı); örnek bir ilerleme için 1. kısmın ilk `bitenSandikSayisi`
/// sandığı bitmiş sayılır (havuz, bitmiş sandıklardan kurulur).
final class SahteGunlukSoruDeposu: GunlukSoruDeposu {
    /// Sekme değişince ilerleme kaybolmasın diye tek örnek (ilerlemiş kullanıcı). Havuza ilk bitmiş
    /// sandık girmez (DailyQuestionPoolBuilder: 1. sandığın üreteci yok); 2. ve sonrakiler girer.
    static let ortak = SahteGunlukSoruDeposu(bitenSandikSayisi: 4)

    private static let ornekKullanici = "ornek"

    let kalpDeposu: DailyQuestionBrokenHeartStore
    let animDeposu: DailyQuestionCardProgressAnimStore
    private let havuz: DailyQuestionPoolBuilder
    private let depo: BellekIciGunlukSoruDeposu
    /// Kısımların ders listeleri (Android: Firestore `lessonProgress`); sandık ilerlemesi bunlardadır.
    private let kisim1: [LessonItem]
    private let kisim2: [LessonItem]
    private let kisim3: [LessonItem]

    init(bitenSandikSayisi: Int) {
        let uid: String = SahteGunlukSoruDeposu.ornekKullanici
        let veri = GlobalLessonData.shared
        kisim1 = SahteGunlukSoruDeposu.sandiklariBitir(veri.createLessonItems(partId: 1), adet: bitenSandikSayisi)
        kisim2 = veri.createLessonItems(partId: 2)
        kisim3 = veri.createLessonItems(partId: 3)
        havuz = GorevGunlukSoruKopruleriKt.gunlukSoruHavuzuVarsayilan()
        depo = GorevGunlukSoruKopruleriKt.bellekIciGunlukSoruDeposuKur(
            uid: uid,
            builder: havuz,
            ornekBasariOrani: KotlinInt(int: 64)
        )
        kalpDeposu = GorevGunlukSoruKopruleriKt.bellekIciKalpDeposuKur(uid: uid)
        animDeposu = GorevGunlukSoruKopruleriKt.bellekIciKartAnimDeposuKur(uid: uid)
    }

    /// Listedeki ilk `adet` sandığı bitmiş sayar (Android: sandık bitince `stepIsFinish = true`).
    private static func sandiklariBitir(_ liste: [LessonItem], adet: Int) -> [LessonItem] {
        let sandikTuru: Int32 = LessonItem.companion.TYPE_CHEST
        var kalan: Int = adet
        for oge in liste {
            if kalan <= 0 {
                break
            }
            if oge.type == sandikTuru {
                oge.stepIsFinish = true
                kalan -= 1
            }
        }
        return liste
    }

    /// TasksFragment.loadDailyQuestionSources: part 1, 2 ve 3.
    private func kaynaklar() -> [DailyQuestionSource] {
        let veri = GlobalLessonData.shared
        let p1: [LessonItem] = veri.loadLessonItemsForPart(partId: 1, cloudItems: kisim1)
        let p2: [LessonItem] = veri.loadLessonItemsForPart(partId: 2, cloudItems: kisim2)
        let p3: [LessonItem] = veri.loadLessonItemsForPart(partId: 3, cloudItems: kisim3)
        return GorevGunlukSoruKopruleriKt.gunlukSoruKaynaklari(
            builder: havuz,
            part1Items: p1,
            part2Items: p2,
            part3Items: p3
        )
    }

    func kartiYukle(tamam: @escaping (DailyQuestionCardUiState) -> Void) {
        let durum: DailyQuestionCardUiState = depo.loadChallengeForCard(sources: kaynaklar())
        DispatchQueue.main.async {
            tamam(durum)
        }
    }

    func meydanOkumayiYukle(tamam: @escaping (GunlukSoruYukleme) -> Void) {
        let kaynak: [DailyQuestionSource] = kaynaklar()
        var sonuc: GunlukSoruYukleme = .havuzBos
        if !kaynak.isEmpty, let meydanOkuma = depo.loadOrCreateChallenge(sources: kaynak) {
            sonuc = .hazir(meydanOkuma)
        }
        DispatchQueue.main.async {
            tamam(sonuc)
        }
    }

    func odulAlindiIsaretle(periodKey: String) {
        _ = depo.markRewardClaimed(periodKey: periodKey)
    }

    func devamBeklemesiniKaldir(periodKey: String, tamam: @escaping () -> Void) {
        _ = depo.clearPendingDiamondContinue(periodKey: periodKey)
        DispatchQueue.main.async {
            tamam()
        }
    }

    func soruSonucunuYaz(periodKey: String, slotIndex: Int, dogru: Bool, tamam: @escaping () -> Void) {
        _ = GorevGunlukSoruKopruleriKt.gunlukSoruSonucunuIsle(
            depo: depo,
            kalpDeposu: kalpDeposu,
            periodKey: periodKey,
            slotIndex: Int32(slotIndex),
            isSuccess: dogru,
            nowMs: GunlukSoruMantik.simdiMs()
        )
        DispatchQueue.main.async {
            tamam()
        }
    }
}

/// Ana cüzdanı saran ince uyarlayıcı: anahtar uygulamanın tek sahte cüzdanından düşülür, üst çubuk ve
/// mağaza aynı bakiyeyi görür (Android: MainActivity.spendKeys → `users/{uid}.keys`).
/// `anaCuzdan` verilmezse bellekte kendi örnek bakiyesini tutar (yalnızca bağımsız denemeler için).
final class SahteGunlukSoruCuzdani: GunlukSoruCuzdani {
    static let ortak = SahteGunlukSoruCuzdani(anaCuzdan: MagazaSahteCuzdanServisi.ortak)

    private let anaCuzdan: MagazaSahteCuzdanServisi?
    /// Ana cüzdan yokken kullanılan örnek bakiye (Android varsayılanı UserWalletFirestore.DEFAULT_KEYS = 1; denemek için 3).
    private var anahtar: Int = 3

    init(anaCuzdan: MagazaSahteCuzdanServisi? = nil) {
        self.anaCuzdan = anaCuzdan
    }

    func anahtarHarca(_ adet: Int) -> Bool {
        if adet <= 0 {
            return true
        }
        if let ana = anaCuzdan {
            // Sunucu eksiye düşen harcamayı reddeder; yetiyorsa aynı anda düşer (sahte updateUserWallet).
            if ana.cuzdan.anahtar < adet {
                return false
            }
            ana.sunucuDegistirdi(altinFarki: 0, anahtarFarki: -adet, sobiFarki: 0)
            return true
        }
        if anahtar < adet {
            return false
        }
        anahtar -= adet
        return true
    }
}

/// Sunucu bağlanana dek: rozet sayacı bellekteki sahte rozet belgesinde ortak kuralla artar (Kupa/RozetKutlamaServisi.swift),
/// sandık ön isteği boş.
final class SahteGunlukSoruOdulServisi: GunlukSoruOdulServisi {
    func rozetIlerlet(periodKey: String, tamam: @escaping ([String]) -> Void) {
        // Android: TasksFragment.startDailyQuestionRewardFlow → incrementBadgeProgressAndDetectLevelUp(incrementFishing = true).
        var girdi = RozetKazanimGirdisi()
        girdi.olta = true
        girdi.gunlukSoruPeriyodu = periodKey
        SahteRozetKazanimServisi.ortak.sayaclariArtir(girdi, tamam: tamam)
    }

    func sandikOnIstegi(nadirlik: String) {}
}

// MARK: - Ortak kurallara ince sarmalayıcılar

/// Kart tıklamasının ilk aşaması (ortak kod: DailyQuestionCardClickAction).
enum GunlukSoruKartTiklama {
    case tamamlandiBildirimi
    case yokSay
    case yukle
}

/// Kart tıklamasının ikinci aşaması (ortak kod: DailyQuestionChallengeClickAction).
enum GunlukSoruMeydanOkumaTiklama {
    case yokSay
    case tamamlandiBildirimi
    case devamPaneli
    case dersiBaslat
}

/// Kırık kalbin kart bağlanırken yapacağı şey (ortak kod: DailyQuestionBrokenHeartAction).
enum GunlukSoruKalpEylemi {
    case iyiles
    case kiril
    case kirikDur
    case ilkKare
}

/// Kart ilerleme çubuğunun bağlanma planı (ortak kod: DailyQuestionProgressPlan).
struct GunlukSoruIlerlemePlani {
    let baslangicYuzdesi: Float
    let bitisYuzdesi: Float
    let animasyon: Bool
}

enum GunlukSoruMantik {
    /// dimens.xml: daily_question_progress_width = 200dp.
    static let cubukGenisligi: Int32 = 200
    /// dimens.xml: mission_progress_shine_inset_start = 14dp, mission_progress_shine_gap_end = 10dp.
    static let parlamaBaslangicBoslugu: Int32 = 14
    static let parlamaBitisBoslugu: Int32 = 10

    // -- Saat ve periyot --

    static func simdiMs() -> Int64 {
        return OyunSaatKt.oyunSimdiMs()
    }

    static func guncelPeriyot() -> String {
        return DailyQuestionPeriod.shared.currentPeriodKey(nowMs: simdiMs())
    }

    /// Periyodun bitmesine kalan süre (ms).
    static func kalanMs() -> Int64 {
        return DailyQuestionPeriod.shared.millisUntilCurrentPeriodEnds(nowMs: simdiMs())
    }

    /// "04:32" ya da "1:04:32".
    static func geriSayimMetni(kalanMs: Int64) -> String {
        return DailyQuestionPeriod.shared.formatCountdown(remainingMs: kalanMs)
    }

    /// Bir sonraki sayaç tıkına kadar beklenecek süre (sn).
    static func sayacGecikmesi(kalanMs: Int64) -> Double {
        let ms: Int64 = DailyQuestionCardLogic.shared.countdownTickDelayMs(remainingMs: kalanMs)
        return Double(ms) / 1000.0
    }

    static func devirdeYenile(kartPeriyodu: String, guncel: String, simdi: Int64, sonYenileme: Int64) -> Bool {
        return DailyQuestionCardLogic.shared.shouldRefreshOnPeriodRollover(
            cardPeriodKey: kartPeriyodu,
            currentPeriodKey: guncel,
            nowMs: simdi,
            lastRefreshMs: sonYenileme
        )
    }

    /// Yanlış cevaptan sonra aynı soruya devam etmenin anahtar bedeli.
    static var devamBedeli: Int {
        return Int(DailyQuestionPeriod.shared.KEY_CONTINUE_COST)
    }

    /// Kart ilerleme animasyonunun süresi (sn).
    static var animasyonSuresi: Double {
        return Double(DailyQuestionCardLogic.shared.DAILY_PROGRESS_ANIM_DURATION_MS) / 1000.0
    }

    /// Günlük soru ödül sandığının başlangıç nadirliği ("RARE").
    static var odulSandigiNadirligi: String {
        return DailyQuestionCardLogic.shared.REWARD_CHEST_START_RARITY.name
    }

    // -- Kart durumu --

    /// Veri gelmeden önce gösterilen kart (TasksFragment.defaultDailyCardState).
    static func varsayilanKart() -> DailyQuestionCardUiState {
        return GorevGunlukSoruKopruleriKt.gunlukSoruVarsayilanKart(poolAvailable: false, nowMs: simdiMs())
    }

    static func odulAlindiKart(_ kart: DailyQuestionCardUiState) -> DailyQuestionCardUiState {
        return GorevGunlukSoruKopruleriKt.gunlukSoruOdulAlindiKart(state: kart)
    }

    static func devamSonrasiKart(_ meydanOkuma: DailyQuestionChallenge, havuzVar: Bool) -> DailyQuestionCardUiState {
        return GorevGunlukSoruKopruleriKt.gunlukSoruDevamSonrasiKart(challenge: meydanOkuma, poolAvailable: havuzVar)
    }

    static func gorunum(_ kart: DailyQuestionCardUiState) -> GunlukSoruKartGorunumu {
        // Android (TasksFragment): kayıttaki ünite adı ve zorluk Türkçe kimliktir; ekranda uygulamanın
        // dilinde (DailyQuestionPoolBuilder.displayTitleUnit / displayDifficulty, ui_basari_x).
        return GunlukSoruKartGorunumu(
            uniteBasligi: Metin.ders(kart.titleUnit),
            odakMetni: odakMetni(kart)
        )
    }

    /// Android: focusLabel metni (ortak kodda focusLabelArgument; Türkçe çıktısı aynıdır).
    private static func odakMetni(_ kart: DailyQuestionCardUiState) -> String? {
        if kart.isComplete || kart.difficulty.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return nil
        }
        var oran: String = ""
        if let basari = kart.globalSuccessRate {
            oran = Metin.al("ui_basari_x", Int(basari.int32Value))
        }
        return zorlukMetni(kart.difficulty) + oran
    }

    /// Android: DailyQuestionPoolBuilder.displayDifficulty — kayıttaki "Kolay"/"Orta"/"Zor" seçili dilde.
    static func zorlukMetni(_ kayitli: String) -> String {
        switch kayitli {
        case "Kolay": return Metin.al("daily_question_difficulty_easy")
        case "Orta": return Metin.al("daily_question_difficulty_medium")
        case "Zor": return Metin.al("daily_question_difficulty_hard")
        default: return kayitli
        }
    }

    // -- Tıklama kararları --

    static func kartTiklama(_ kart: DailyQuestionCardUiState) -> GunlukSoruKartTiklama {
        let eylem: DailyQuestionCardClickAction = DailyQuestionCardLogic.shared.cardClickAction(state: kart)
        if eylem == DailyQuestionCardClickAction.showChallengeCompleteToast {
            return .tamamlandiBildirimi
        }
        if eylem == DailyQuestionCardClickAction.loadChallenge {
            return .yukle
        }
        return .yokSay
    }

    static func meydanOkumaTiklama(_ meydanOkuma: DailyQuestionChallenge) -> GunlukSoruMeydanOkumaTiklama {
        let eylem: DailyQuestionChallengeClickAction =
            DailyQuestionCardLogic.shared.challengeClickAction(challenge: meydanOkuma)
        if eylem == DailyQuestionChallengeClickAction.showChallengeCompleteToast {
            return .tamamlandiBildirimi
        }
        if eylem == DailyQuestionChallengeClickAction.showContinuePanel {
            return .devamPaneli
        }
        if eylem == DailyQuestionChallengeClickAction.launchLesson {
            return .dersiBaslat
        }
        return .yokSay
    }

    /// İlerleme bölgesi tıklanabilir mi (ödül alınmadıysa evet).
    static func ilerlemeTiklanir(_ kart: DailyQuestionCardUiState) -> Bool {
        return DailyQuestionCardLogic.shared.canTapProgress(ui: kart)
    }

    /// İlerleme bölgesine dokununca ödül alınabilir mi (3/3, çubuk dolu, animasyon bitmiş).
    static func odulAlmayaHazir(_ kart: DailyQuestionCardUiState, gorselYuzde: Float, animasyonda: Bool) -> Bool {
        return DailyQuestionCardLogic.shared.isProgressClaimTapReady(
            ui: kart,
            lastVisualPercent: gorselYuzde,
            animatorRunning: animasyonda
        )
    }

    // -- İlerleme çubuğu --

    static func ilerlemePlani(_ kart: DailyQuestionCardUiState, anim: DailyQuestionCardProgressAnimStore) -> GunlukSoruIlerlemePlani {
        // Android kart yüklenmemişken depoyu hiç okumaz.
        var sonGosterilen: KotlinInt? = nil
        if kart.isLoaded {
            sonGosterilen = anim.getLastShown(periodKey: kart.periodKey)
        }
        let plan: DailyQuestionProgressPlan =
            DailyQuestionCardLogic.shared.progressPlan(state: kart, lastShown: sonGosterilen)
        return GunlukSoruIlerlemePlani(
            baslangicYuzdesi: plan.startPct,
            bitisYuzdesi: plan.endPct,
            animasyon: plan.shouldAnimate
        )
    }

    /// DailyQuestionVH.persistProgressShown.
    static func gosterileniKaydet(_ kart: DailyQuestionCardUiState, anim: DailyQuestionCardProgressAnimStore) {
        if let cozulen = DailyQuestionCardLogic.shared.solvedToPersist(ui: kart) {
            anim.setLastShown(periodKey: kart.periodKey, solved: cozulen.int32Value)
        }
    }

    /// Animasyonun `t` (0..1) anındaki görsel yüzde (DecelerateInterpolator(1.6f)).
    static func animasyonYuzdesi(baslangic: Float, bitis: Float, t: Float) -> Float {
        return GorevGunlukSoruKopruleriKt.gunlukSoruCubukAnimasyonYuzdesi(startPct: baslangic, endPct: bitis, t: t)
    }

    /// DailyQuestionVH.applyVisualPercent.
    static func cubuk(_ kart: DailyQuestionCardUiState, yuzde: Float) -> GunlukSoruCubukCizimi {
        let g: GunlukSoruCubukGorunumu = GorevGunlukSoruKopruleriKt.gunlukSoruCubukGorunumu(
            ui: kart,
            pct: yuzde,
            trackWidth: cubukGenisligi,
            startInset: parlamaBaslangicBoslugu,
            endGap: parlamaBitisBoslugu
        )
        var etiket: String = Metin.al(g.labelResKey)
        if g.mode == GunlukSoruCubukModu.progress {
            etiket = Metin.al(g.labelResKey, Int(g.labelCurrent), Int(g.labelTotal))
        }
        var dolguRengi = "daily_question_progress_fill"
        var parlamaRengi = "daily_question_progress_shine"
        var dolguGenisligi: CGFloat = 0
        var parlamaGenisligi: CGFloat = 0
        var dolguGorunur = false
        var parlamaGorunur = false
        if let katman = g.overlay {
            dolguRengi = renkAdi(katman.fillDrawable)
            parlamaRengi = renkAdi(katman.shineDrawable)
            dolguGenisligi = CGFloat(katman.fillWidth)
            parlamaGenisligi = CGFloat(katman.shineWidth)
            dolguGorunur = katman.fillVisible
            parlamaGorunur = katman.shineVisible
        }
        return GunlukSoruCubukCizimi(
            zeminRengi: renkAdi(g.trackDrawable),
            dolguRengi: dolguRengi,
            parlamaRengi: parlamaRengi,
            dolguGenisligi: dolguGenisligi,
            dolguGorunur: dolguGorunur,
            parlamaGenisligi: parlamaGenisligi,
            parlamaGorunur: parlamaGorunur,
            etiket: etiket,
            etiketRengi: g.labelColor
        )
    }

    /// Android'de şekil drawable'ı olan zemin/dolguların (res/drawable/*progress*.xml) düz rengi.
    static func renkAdi(_ sekil: String) -> String {
        switch sekil {
        case "mission_progress_track": return "missions_track"
        case "daily_question_progress_track": return "daily_question_progress_track"
        case "daily_question_progress_fill": return "daily_question_progress_fill"
        case "daily_question_progress_fill_complete": return "missions_progress_complete"
        case "daily_question_progress_shine": return "daily_question_progress_shine"
        case "daily_question_progress_shine_complete": return "missions_progress_gold_shine"
        case "mission_progress_fill_claimed": return "button_disabled"
        case "mission_progress_shine_claimed": return "chest_star_light_gray"
        default: return "daily_question_progress_track"
        }
    }

    // -- Kırık kalp --

    /// DailyQuestionVH.bindBrokenHeart: depodaki istekleri Android'deki sırayla tüketir.
    static func kalpEylemi(_ kart: DailyQuestionCardUiState, kalp: DailyQuestionBrokenHeartStore) -> GunlukSoruKalpEylemi {
        let eylem: DailyQuestionBrokenHeartAction =
            DailyQuestionCardLogic.shared.brokenHeartAction(state: kart, store: kalp)
        if eylem == DailyQuestionBrokenHeartAction.playHeal {
            return .iyiles
        }
        if eylem == DailyQuestionBrokenHeartAction.playBreakToHold116 {
            return .kiril
        }
        if eylem == DailyQuestionBrokenHeartAction.showHold116 {
            return .kirikDur
        }
        return .ilkKare
    }

    // -- Soruyu başlatma --

    /// TasksFragment.launchDailyQuestionLesson.
    static func baslangic(_ meydanOkuma: DailyQuestionChallenge, sonuc: @escaping (String, Int, Bool) -> Void) -> GunlukSoruBaslangici? {
        guard let veri = GorevGunlukSoruKopruleriKt.gunlukSoruBaslatma(challenge: meydanOkuma) else {
            return nil
        }
        return cevir(veri, sonuc: sonuc)
    }

    /// TasksFragment.showDailyQuestionContinuePanel: anahtarla devam edilen soru.
    static func devamBaslangici(_ meydanOkuma: DailyQuestionChallenge, sonuc: @escaping (String, Int, Bool) -> Void) -> GunlukSoruBaslangici? {
        guard let veri = GorevGunlukSoruKopruleriKt.gunlukSoruDevamBaslatma(challenge: meydanOkuma) else {
            return nil
        }
        return cevir(veri, sonuc: sonuc)
    }

    private static func cevir(_ veri: GunlukSoruBaslatma, sonuc: @escaping (String, Int, Bool) -> Void) -> GunlukSoruBaslangici {
        var dizi: [Int] = []
        for sayi in veri.sequence {
            dizi.append(Int(sayi.int32Value))
        }
        var islem: GunlukSoruIslemi? = nil
        if let ilk = veri.mathFirst, let islec = veri.mathOperator, let ikinci = veri.mathSecond {
            islem = GunlukSoruIslemi(ilk: Int(ilk.int32Value), islec: islec, ikinci: Int(ikinci.int32Value))
        }
        var aralik: Int? = nil
        if let ms = veri.displayIntervalMs {
            aralik = Int(ms.int64Value)
        }
        let periyot: String = veri.periodKey
        let slot: Int = Int(veri.slotIndex)
        return GunlukSoruBaslangici(
            dizi: dizi,
            islem: islem,
            periodKey: periyot,
            slotIndex: slot,
            gosterimAraligiMs: aralik,
            partId: Int(veri.partId),
            sonuc: { dogru in
                sonuc(periyot, slot, dogru)
            }
        )
    }
}

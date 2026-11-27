import Foundation
import Shared

// Kupa / rozet / liderlik ekranlarının dış dünyaya (Firestore, enerji, ders ilerlemesi, ses)
// açılan kapıları. Firebase henüz bağlı olmadığı için her protokolün örnek veri döndüren
// sahte bir uygulaması vardır. Android'de verinin geldiği yer protokolün üstünde yazılıdır.

// MARK: - Dışa verilen değerler

/// Kupa dersini başlatmak için gereken her şey (ortak kod `CupPathLogic.buildCupLesson` çıktısı).
///
/// Android karşılığı: `TasksFragment.launchCupModeLesson` — `CupRuleEngine.buildLessonItem`'ın
/// part 9 yer tutucusuna yazdığı alanlar + BlindingLessonFragment'e verilen "operations".
struct KupaDersBaslangici: Equatable {
    /// 1..6 — Kupa Yolu kartı (1 Toplama, 2 Çıkarma, 3 Çarpma, 4–6 bunların körleme hâli).
    let kartNo: Int
    /// `users/{uid}/cupWayProgress/progress` dokümanında güncellenecek alan.
    let alan: String
    /// Ders başlarken okunan kupa skoru (Android: GlobalValues.currentLessonOldCupScore).
    let kupaSkoru: Int
    /// LessonItem.timePeriod — sayılar arası süre (ms).
    let sureMs: Int
    /// LessonItem.cupDigitSize
    let basamakSayisi: Int
    /// LessonItem.cupNumberCount
    let sayiAdedi: Int
    /// LessonItem.cupDifficultyLevel (0..4)
    let zorlukSeviyesi: Int
    /// LessonItem.cupWinDelta — doğru cevapta eklenecek kupa.
    let kazancDeltasi: Int
    /// LessonItem.cupLossDelta — yanlış cevapta düşülecek kupa.
    let kayipDeltasi: Int
    let korleme: Bool
    let cikarma: Bool
    let carpma: Bool
    /// Çarpma dışı modlarda sırayla gösterilecek sayılar (çıkarmada negatifler dahil).
    let sayilar: [Int]
    /// Çarpma modunda soru: `carpan1 x carpan2`; diğer modlarda 0.
    let carpan1: Int
    let carpan2: Int
}

/// Kupa dersinden dönüş: hangi kartın skoru ne kadar değişti.
/// Android: `GlobalValues.pending*CupDelta` + `consumePendingCupDelta`.
struct KupaDersSonucu: Equatable {
    let kartNo: Int
    /// Dersin kupa değişimi (+kazanç / −kayıp).
    let delta: Int
}

/// Kupa geçmişinin bir günü (`users/{uid}/cupHistory/{yyyy-MM-dd}` dokümanındaki alan değeri).
struct KupaGecmisKaydi: Equatable {
    let tarihKimligi: String
    let deger: Int
}

/// Liderlik tablosunun ham kaydı (`lessonLeaderboards/{tahta}/entries/{uid}`).
struct KupaLiderKaydi: Equatable {
    let kullaniciKimligi: String
    /// recordScore
    let puan: Int
    /// displayName
    let ad: String
    /// recordLabel
    let rekorEtiketi: String
    /// avatarConfig (Android AvatarConfig.encode biçimi; boş = varsayılan avatar). Eski photoUrl alanı kalktı.
    let avatarConfig: String
}

// MARK: - Protokoller

/// Sunucunun `submitCupResult` yanıtı (ortak kod: `CupScoreService.CupScoreResult`).
struct KupaSunucuYaniti: Equatable {
    /// Gönderimden önceki puan (yanıttaki `oldScore`).
    let eskiSkor: Int
    /// Sunucunun hesapladığı yeni puan (yanıttaki `newScore`); 0'ın altına düşmez.
    let yeniSkor: Int
}

/// Kupa skorları.
///
/// Android: `users/{uid}/cupWayProgress/progress` dokümanı; alanlar `addition_abacus_cup`,
/// `extraction_abacus_cup`, `impact_abacus_cup`, `blinding_addition_abacus_cup`,
/// `blinding_extraction_abacus_cup`, `blinding_impact_abacus_cup`.
///
/// OKUMA: alan yoksa ya da okuma hatasında 200 döner, hiçbir şey YAZILMAZ (güncel Android'de
/// tohumlama yok; ortak kod `CupScoreRepositorySpec.resolveFetchedCupScore`, `shouldSeed` hep false).
///
/// YAZMA: istemci kupa puanını yazamaz (`cupWayProgress` ve `cupHistory` kurallarla yazmaya kapalı).
/// Ders bitince yalnızca sonuç `submitCupResult` Cloud Function'ına gönderilir; miktarı sunucu
/// kendi tablosundan hesaplar, puanı ve geçmiş gününü tek işlemde yazar.
protocol KupaSkorDeposu: AnyObject {
    func kupaSkorunuGetir(alan: String, tamam: @escaping (Int) -> Void)

    /// Android: `CupScoreService.submitResult` → Cloud Function `submitCupResult`.
    ///
    /// - Parameters:
    ///   - yuk: istek gövdesi (`cupField`, `won`, `difficultyLevel`, `isMultiplication`, `dayId`);
    ///     ortak kod `CupScoreService.buildSubmitPayload` / `BadgePrecalcHelper.buildCupSubmitPayload`
    ///     üretir. Gerçek uygulama yanıtı `CupScoreService.parseResult(resultData:)` ile ayrıştırır.
    ///   - yerelDelta: YALNIZCA sahte servis içindir (gerçek sunucu bunu görmez, kendi tablosunu kullanır):
    ///     sahte servis puanı yerelde bu kadar değiştirip döndürür.
    ///   - tamam: yanıt; ayrıştırılamadıysa ya da hata olursa nil (Android: arayüz eski değeri göstermeye devam eder).
    func sonucuSunucuyaGonder(yuk: [String: Any], yerelDelta: Int, tamam: @escaping (KupaSunucuYaniti?) -> Void)
}

/// Kupa Yolu ödül defteri (sandık eşikleri).
///
/// Android: `CupPathRewardRepository.fetchStates` TEK çağrıda iki dokümanı okur:
/// `users/{uid}/cupWayProgress/progress` (puanlar) ve `users/{uid}/cupPathRewards/progress`
/// (defter: her alan için `{lastClaimed: Int, claimed: [Int]}`; yalnızca sunucu yazar).
/// Ham dokümanlar verilir; durumu ortak kod kurar (`CupPathRewardRepository.buildStates`).
/// Belge yoksa ilgili parametre nil; okuma hatasında Android varsayılan durumu (puan 200, ödül yok) kullanır.
protocol KupaYolBelgeDeposu: AnyObject {
    func belgeleriGetir(tamam: @escaping (_ kupaBelgesi: [String: Any]?, _ defter: [String: Any]?) -> Void)
}

/// Sezon saati. Android: `SeasonClock.refreshFromServer` → Cloud Function `getSeasonInfo`
/// (yanıtta `serverNowMs`) → `SeasonClock.onServerTime`. Sezon hesapları düz cihaz saatinden değil
/// sunucu sapmasıyla düzeltilmiş saatten yapılır; sezon yanlışsa başka tahta okunur ve liste boş kalır.
protocol KupaSezonSaatiKaynagi: AnyObject {
    /// Sunucu saatini sorar; yanıt gelince ortak koddaki sapmayı günceller (`SeasonClock.onServerTime`),
    /// sonra `tamam` çağrılır. Hata olursa sapma değişmez ama `tamam` yine çağrılır.
    func sunucuSaatiniYenile(tamam: @escaping () -> Void)
}

/// Kupa Yolu kartının kilidi.
/// Android: `GlobalLessonData.loadLessonItemsForPart(partId)` → o part'ın SON sandık dersinin
/// `stepIsFinish` değeri; ONAYLI ÖĞRETMENDE hepsi açıktır (ortak kod:
/// `CupPathLogic.isCardActive(lessonItemTypes:stepIsFinish:isApprovedTeacher:)`).
protocol KupaKilitKaynagi: AnyObject {
    func kartAcikMi(partId: Int, tamam: @escaping (Bool) -> Void)
}

/// Can (enerji). Android: `MainActivity.getEnergyManager()` / `isInfiniteEnergy()`.
protocol KupaEnerjiKaynagi: AnyObject {
    var sonsuzEnerji: Bool { get }
    var guncelEnerji: Int { get }
    /// Pro/Premium plan mı (Android: `PlanStatus.isProPlan(energyManager.getUserPlan())`): kalp `heart_pro_ic` olur.
    /// Sonsuz enerjiden AYRIDIR: onaylı öğretmenin enerjisi sonsuzdur ama abone değildir.
    var proPlan: Bool { get }
}

/// Kupa geçmişi.
/// Android: `CupHistoryRepository.fetchHistorySince / fetchHistory` —
/// `users/{uid}/cupHistory` koleksiyonu, doküman kimliği "yyyy-MM-dd", her dokümanda kupa
/// alanının o günkü son değeri. Güncel skor `cupWayProgress/progress` dokümanından okunur.
protocol KupaGecmisDeposu: AnyObject {
    /// - Parameter baslangicKimligi: `documentId() >= …` alt sınırı; boş = tüm geçmiş.
    /// Sonuç tarihe göre artan sırada olmalıdır; hata durumunda boş dizi.
    func gecmisiGetir(
        kullaniciKimligi: String,
        alan: String,
        baslangicKimligi: String,
        tamam: @escaping ([KupaGecmisKaydi]) -> Void
    )
    /// Alan yoksa nil (ekran 200 gösterir). Hata durumunda hiç çağrılmaz (Android böyle).
    func guncelSkoruGetir(kullaniciKimligi: String, alan: String, tamam: @escaping (Int?) -> Void)
}

/// Canlı dinlemeyi durdurmak için döndürülen tutamak (Android: ListenerRegistration).
final class KupaDinlemeTutamagi {
    private var iptalEylemi: (() -> Void)?

    init(iptal: @escaping () -> Void) {
        self.iptalEylemi = iptal
    }

    func iptalEt() {
        iptalEylemi?()
        iptalEylemi = nil
    }
}

/// Ders liderlik tablosu.
/// Android: `LessonLeaderboardRepository.listenLeaderboard` —
/// `lessonLeaderboards/part_{p}_lesson_{stableId}_season_{s}/entries`, `orderBy recordScore DESC`,
/// en çok 100 kayıt, canlı dinleme.
protocol KupaLiderlikDeposu: AnyObject {
    /// Oturum açmış kullanıcının kimliği (yoksa boş). Android: FirebaseAuth.currentUser?.uid.
    var oturumKimligi: String { get }

    /// - Parameter hata: (yerelleştirilmiş ileti, yetki hatası mı).
    func dinle(
        partId: Int,
        dersAnahtari: String,
        sezon: Int,
        guncelle: @escaping ([KupaLiderKaydi]) -> Void,
        hata: @escaping (String, Bool) -> Void
    ) -> KupaDinlemeTutamagi
}

/// Rozet ilerlemesi.
/// Android: `users/{uid}/badgeProgress/state` dokümanı (alan adları ortak kodda
/// `BadgeProgressFirestore.F_*`). Doküman ham hâliyle verilir; ayrıştırmayı ortak kod yapar.
/// Yalnızca OKUMA: rozet sayaçları ve madalya/kupa parçaları güncel Android'de istemci yazımına
/// kapalıdır (kurallar; yazmayı sunucu yapar). Kademe eşikleri tek tablodadır
/// (`BadgeProgressEngine.levelSpec`; Golf [3,5,10,15,20], Roket [3,10,15,25,30]).
protocol KupaRozetDeposu: AnyObject {
    func rozetDurumunuGetir(tamam: @escaping ([String: Any]?) -> Void)
}

/// Rozet sesi. Android: `BadgeSoundPlayer` (res/raw/badge_sound.mp3).
protocol KupaSesCalici: AnyObject {
    func rozetSesiniHazirla()
    func rozetSesiniCal()
}

// MARK: - Ortam

/// Ekranlara verilen servis demeti. Firebase bağlanınca yönetici oturum gerçek uygulamaları verir.
struct KupaOrtami {
    var skorDeposu: KupaSkorDeposu
    var yolBelgeDeposu: KupaYolBelgeDeposu
    var sezonSaati: KupaSezonSaatiKaynagi
    var kilitKaynagi: KupaKilitKaynagi
    var enerjiKaynagi: KupaEnerjiKaynagi
    var gecmisDeposu: KupaGecmisDeposu
    var liderlikDeposu: KupaLiderlikDeposu
    var rozetDeposu: KupaRozetDeposu
    var sesCalici: KupaSesCalici

    /// Örnek veriyle çalışan ortam.
    static let sahte: KupaOrtami = {
        let depo = SahteKupaDeposu()
        return KupaOrtami(
            skorDeposu: depo,
            yolBelgeDeposu: depo,
            sezonSaati: SahteKupaSezonSaati(),
            kilitKaynagi: depo,
            enerjiKaynagi: depo,
            gecmisDeposu: depo,
            liderlikDeposu: SahteKupaLiderlikDeposu(),
            rozetDeposu: SahteKupaRozetDeposu(),
            sesCalici: SahteKupaSesCalici()
        )
    }()
}

// MARK: - Sahte uygulamalar

/// Kupa skorları, kart kilitleri, enerji ve kupa geçmişi için örnek veri.
final class SahteKupaDeposu: KupaSkorDeposu, KupaYolBelgeDeposu, KupaKilitKaynagi, KupaEnerjiKaynagi, KupaGecmisDeposu {
    /// Alan → skor. Olmayan alan 200 (Android varsayılanı) döner.
    var skorlar: [String: Int] = [
        "addition_abacus_cup": 640,
        "extraction_abacus_cup": 315,
        "impact_abacus_cup": 200,
        "blinding_addition_abacus_cup": 3120,
        "blinding_extraction_abacus_cup": 200,
        "blinding_impact_abacus_cup": 200
    ]

    /// Son sandığı bitmiş part'lar (kart N ↔ part N).
    var bitenPartlar: Set<Int> = [1, 2, 3, 4]

    /// Kupa Yolu ödül defteri: alan → `{lastClaimed, claimed}` (Android `cupPathRewards/progress`).
    /// Olmayan alan = hiç sandık alınmamış (su seviyesi 200, ilk eşik 300).
    var defter: [String: Any] = SahteKupaDeposu.ornekDefter()

    var sonsuzEnerji: Bool = false
    var guncelEnerji: Int = 5
    var proPlan: Bool = false

    /// Ağ gecikmesini taklit eder.
    private let gecikme: TimeInterval = 0.35

    func kupaSkorunuGetir(alan: String, tamam: @escaping (Int) -> Void) {
        let skor: Int = skorlar[alan] ?? 200
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme) {
            tamam(skor)
        }
    }

    func sonucuSunucuyaGonder(yuk: [String: Any], yerelDelta: Int, tamam: @escaping (KupaSunucuYaniti?) -> Void) {
        // Sunucunun işini yerelde taklit eder: puan yerelDelta kadar değişir, 0'ın altına düşmez
        // (gerçek sunucu miktarı kendi tablosundan hesaplar; istemci miktar göndermez).
        let alan: String = (yuk["cupField"] as? String) ?? ""
        let eski: Int = skorlar[alan] ?? 200
        let yeni: Int = max(0, eski + yerelDelta)
        skorlar[alan] = yeni
        let yanit = KupaSunucuYaniti(eskiSkor: eski, yeniSkor: yeni)
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme) {
            tamam(yanit)
        }
    }

    func belgeleriGetir(tamam: @escaping (_ kupaBelgesi: [String: Any]?, _ defter: [String: Any]?) -> Void) {
        let kupaBelgesi: [String: Any] = skorlar
        let defterBelgesi: [String: Any] = defter
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme) {
            tamam(kupaBelgesi, defterBelgesi)
        }
    }

    func kartAcikMi(partId: Int, tamam: @escaping (Bool) -> Void) {
        let acik: Bool = bitenPartlar.contains(partId)
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme) {
            tamam(acik)
        }
    }

    func gecmisiGetir(
        kullaniciKimligi: String,
        alan: String,
        baslangicKimligi: String,
        tamam: @escaping ([KupaGecmisKaydi]) -> Void
    ) {
        let bugunkuSkor: Int = skorlar[alan] ?? 200
        let hepsi: [KupaGecmisKaydi] = SahteKupaDeposu.ornekGecmis(bitisSkoru: bugunkuSkor)
        let suzulmus: [KupaGecmisKaydi]
        if baslangicKimligi.isEmpty {
            suzulmus = hepsi
        } else {
            // "yyyy-MM-dd" kimlikleri sözlük sırasıyla karşılaştırılabilir (Firestore da böyle yapar).
            suzulmus = hepsi.filter { $0.tarihKimligi >= baslangicKimligi }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme) {
            tamam(suzulmus)
        }
    }

    func guncelSkoruGetir(kullaniciKimligi: String, alan: String, tamam: @escaping (Int?) -> Void) {
        let skor: Int? = skorlar[alan]
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme) {
            tamam(skor)
        }
    }

    /// Örnek defter: toplama yolunda 500'e kadar alınmış (600 hazır), körleme toplamada 3000'e kadar
    /// alınmış ve 3200 tek başına alınmış (3100 hazır).
    private static func ornekDefter() -> [String: Any] {
        let toplama: [String: Any] = ["lastClaimed": 500, "claimed": [Any]()]
        let korleme: [String: Any] = ["lastClaimed": 3000, "claimed": [3200] as [Any]]
        var d: [String: Any] = [:]
        d["addition_abacus_cup"] = toplama
        d["blinding_addition_abacus_cup"] = korleme
        return d
    }

    /// Son 75 günün bir kısmında oynanmış gibi görünen, bugünkü skorda biten örnek geçmiş.
    private static func ornekGecmis(bitisSkoru: Int) -> [KupaGecmisKaydi] {
        var takvim = Calendar(identifier: .gregorian)
        takvim.timeZone = TimeZone.current
        let bicim = DateFormatter()
        bicim.calendar = takvim
        bicim.locale = Locale(identifier: "en_US_POSIX")
        bicim.timeZone = TimeZone.current
        bicim.dateFormat = "yyyy-MM-dd"

        let bugun = Date()
        // Geriye doğru gidilirken skor düşer; oynanmayan günler atlanır.
        let gunFarklari: [Int] = [0, 1, 2, 4, 5, 8, 11, 15, 19, 24, 30, 37, 45, 52, 60, 75]
        let dususler: [Int] = [0, 24, 10, 38, 17, 30, 24, 10, 31, 38, 17, 24, 10, 30, 24, 17]
        var sonuc: [KupaGecmisKaydi] = []
        var skor: Int = bitisSkoru
        for (sira, fark) in gunFarklari.enumerated() {
            skor = max(0, skor - dususler[sira])
            guard let gun = takvim.date(byAdding: .day, value: -fark, to: bugun) else { continue }
            sonuc.append(KupaGecmisKaydi(tarihKimligi: bicim.string(from: gun), deger: skor))
        }
        return sonuc.sorted { $0.tarihKimligi < $1.tarihKimligi }
    }
}

/// Sunucu saatini cihaz saatiyle aynı sayan sahte sezon saati (sapma 0).
final class SahteKupaSezonSaati: KupaSezonSaatiKaynagi {
    func sunucuSaatiniYenile(tamam: @escaping () -> Void) {
        let simdi: Int64 = Int64(Date().timeIntervalSince1970 * 1000.0)
        _ = SeasonClock.shared.onServerTime(serverNowUtcMs: simdi, deviceNowMs: simdi)
        DispatchQueue.main.async {
            tamam()
        }
    }
}

/// Liderlik tablosu için örnek veri (tek seferlik anlık görüntü; canlı güncelleme yok).
final class SahteKupaLiderlikDeposu: KupaLiderlikDeposu {
    var oturumKimligi: String = "ornek-ben"

    // Not: `ad` boş bırakılırsa satırda "Liderlik tablosu" yazar — Android de boş adı
    // R.string.record_leaderboard_title ile doldurur (RecordLeaderboardAdapter.bind; ortak kod
    // RecordLogic.displayNameOrFallback). Örnek veride bu yüzden her kaydın adı doludur.
    var kayitlar: [KupaLiderKaydi] = [
        KupaLiderKaydi(kullaniciKimligi: "ornek-1", puan: 9820, ad: "Elif", rekorEtiketi: "0:41", avatarConfig: ""),
        KupaLiderKaydi(kullaniciKimligi: "ornek-2", puan: 9510, ad: "Yusuf", rekorEtiketi: "0:47", avatarConfig: ""),
        KupaLiderKaydi(kullaniciKimligi: "ornek-3", puan: 9510, ad: "Zeynep", rekorEtiketi: "0:47", avatarConfig: ""),
        KupaLiderKaydi(kullaniciKimligi: "ornek-ben", puan: 9100, ad: "Sen", rekorEtiketi: "0:55", avatarConfig: ""),
        KupaLiderKaydi(kullaniciKimligi: "ornek-5", puan: 8740, ad: "Ayşe", rekorEtiketi: "1:02", avatarConfig: ""),
        KupaLiderKaydi(kullaniciKimligi: "ornek-6", puan: 8100, ad: "Mert", rekorEtiketi: "1:15", avatarConfig: ""),
        KupaLiderKaydi(kullaniciKimligi: "ornek-7", puan: 7650, ad: "Defne", rekorEtiketi: "1:28", avatarConfig: "")
    ]

    func dinle(
        partId: Int,
        dersAnahtari: String,
        sezon: Int,
        guncelle: @escaping ([KupaLiderKaydi]) -> Void,
        hata: @escaping (String, Bool) -> Void
    ) -> KupaDinlemeTutamagi {
        var iptalEdildi: Bool = false
        let anlikGoruntu: [KupaLiderKaydi] = kayitlar
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
            if !iptalEdildi {
                guncelle(anlikGoruntu)
            }
        }
        return KupaDinlemeTutamagi(iptal: { iptalEdildi = true })
    }
}

/// Rozet ilerlemesi için örnek `badgeProgress/state` dokümanı.
final class SahteKupaRozetDeposu: KupaRozetDeposu {
    var dokuman: [String: Any]? = SahteKupaRozetDeposu.ornekDokuman()

    private static func ornekDokuman() -> [String: Any] {
        // Parça satırları Firestore'daki gibi: [ünite adı, sezon] ve [ünite adı, sıra, sezon].
        let altin1: [Any] = ["Ünite 1", 3]
        let bronz1: [Any] = ["Ünite 2", 4]
        let bronz2: [Any] = ["Ünite 1", 5]
        let kupa1: [Any] = ["Ünite 1", 7, 2]
        let kupa2: [Any] = ["Ünite 3", 42, 4]
        let altinlar: [Any] = [altin1]
        let bronzlar: [Any] = [bronz1, bronz2]
        let kupalar: [Any] = [kupa1, kupa2]

        var d: [String: Any] = [:]
        d["userDartProgress"] = 12
        d["userKarateProgress"] = 2
        d["userGolfProgress"] = 6
        d["userBowlingProgress"] = 23
        d["userFishingProgress"] = 3
        d["userFishingStreak"] = 0
        d["userRocketProgress"] = 3
        d["userTornadoProgress"] = 4
        d["userVolcanoProgress"] = 0
        d["userDinoProgress"] = 640
        d["userCrocodileProgress"] = 315
        d["userEagleProgress"] = 3120
        d["goldMedalPiece"] = altinlar
        d["bronzeMedalPiece"] = bronzlar
        d["cupPiece"] = kupalar
        return d
    }

    func rozetDurumunuGetir(tamam: @escaping ([String: Any]?) -> Void) {
        let anlik: [String: Any]? = dokuman
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            tamam(anlik)
        }
    }
}

/// Ses çalmayan yer tutucu.
final class SahteKupaSesCalici: KupaSesCalici {
    func rozetSesiniHazirla() {}
    func rozetSesiniCal() {}
}

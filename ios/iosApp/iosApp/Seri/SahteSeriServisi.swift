import Foundation
import Shared

/// Sahte servisin denemeyi kolaylaştıran ayarları.
struct SahteSeriAyarlari {
    /// Bir ders ekranı kapanınca bugüne eklenen süre (saniye). Gerçek sayaç da çalışır; bu ek, simülatörde
    /// kısa bir dersin günlük hedefi (en az 5 dk) doldurabilmesi içindir (Android karşılığı: yalnızca DEBUG
    /// derlemesindeki StudyTimeTracker.addSecondsForDebug). Gerçek serviste YOK.
    var dersSonuEkSaniye: Int = 300
    /// Sunucu çağrılarının taklit gecikmesi (saniye).
    var gecikmeSaniye: Double = 0.3
}

/// Bellekte çalışan seri servisi: yerel kurallar ortak koddaki `StreakRepository` / `StudyTimeTracker`,
/// "sunucu" ortak koddaki `StreakServerModel` (functions/index.js kopyası). Cüzdan mağazanın sahte
/// cüzdanıdır (altın düşümü ve ödül eklemesi sunucu adına oraya yazılır).
///
/// Kalıcılık yok: depolar `InMemoryKeyValueStore` (Android'de SharedPreferences "streak_prefs" ve
/// "study_time"; gerçek uygulamada UserDefaults sarmalayıcısı gerekir). Uygulama kapanınca seri sıfırlanır.
final class SahteSeriServisi: SeriServisi {
    let saat: SystemStreakClock
    private let seriDeposu: InMemoryKeyValueStore
    private let sureDeposu: InMemoryKeyValueStore
    let izleyici: StudyTimeTracker
    let depo: StreakRepository
    /// `users/{uid}/streak/state` + Cloud Functions taklidi.
    let sunucu: StreakServerModel
    private let cuzdan: MagazaSahteCuzdanServisi
    let hatirlatma: SeriHatirlatmaServisi
    var ayarlar: SahteSeriAyarlari

    private var dinleyiciler: [UUID: () -> Void] = [:]
    private var bagliUid: String? = nil
    private var acikDersEkrani: String? = nil
    /// Android: StreakSyncService.syncing (aynı anda tek gönderim).
    private var gonderiliyor: Bool = false

    init(cuzdan: MagazaSahteCuzdanServisi, ayarlar: SahteSeriAyarlari = SahteSeriAyarlari()) {
        let saat = SystemStreakClock()
        let seriDeposu = InMemoryKeyValueStore()
        let sureDeposu = InMemoryKeyValueStore()
        let izleyici = StudyTimeTracker(store: sureDeposu, clock: saat)
        self.saat = saat
        self.seriDeposu = seriDeposu
        self.sureDeposu = sureDeposu
        self.izleyici = izleyici
        self.depo = StreakRepository(store: seriDeposu, tracker: izleyici, clock: saat)
        self.sunucu = StreakServerModel(clock: saat)
        self.cuzdan = cuzdan
        self.hatirlatma = SahteSeriHatirlatmaServisi()
        self.ayarlar = ayarlar
    }

    // MARK: Durum

    func durum() -> StreakState {
        return depo.refresh()
    }

    func ekranVerisi() -> SeriEkranVerisi {
        let state: StreakState = depo.refresh()
        let bugun: String = StreakDays.shared.dayId(clock: saat, offsetDays: 0)
        let ui: StreakScreenUi = StreakScreenLogic.shared.screen(
            state: state,
            chosenChallengeDays: depo.chosenChallengeDays(),
            challengeClaimed: depo.challengeClaimed(),
            challengeClaimedDay: depo.challengeClaimedDay(),
            today: bugun,
            serverCurrent: depo.serverCurrent(),
            claimedMilestones: depo.claimedMilestones(),
            reminderHour: depo.reminderHour()
        )
        let hafta: [StreakWeekDayUi] = StreakScreenLogic.shared.weekStrip(
            clock: saat,
            achievedDays: state.achievedDays,
            frozenDays: state.frozenDays
        )
        return SeriEkranVerisi(ui: ui, hafta: hafta)
    }

    var profilEnUzunSeri: Int {
        return Int(sunucu.longest)
    }

    // MARK: Hesap

    @discardableResult
    func hesabaBagla(uid: String?, hesapAcilisMs: Int64?, bekleyenKayit: StreakPendingSignup?) -> StreakSignupResult {
        guard let kimlik = uid, !kimlik.isEmpty else {
            return StreakSignupResult.noPending
        }
        if bagliUid != kimlik {
            // Sahte sunucu da hesap başınadır: başka hesaba geçilince "doküman" sıfırlanır.
            if bagliUid != nil {
                sunucu.reset()
            }
            bagliUid = kimlik
        }
        depo.bindToUser(uid: kimlik)
        let acilis: KotlinLong? = hesapAcilisMs.map { KotlinLong(longLong: $0) }
        let sonuc: StreakSignupResult = depo.applyPendingSignup(pending: bekleyenKayit, uid: kimlik, accountCreatedAtMs: acilis)
        if sonuc == StreakSignupResult.applied {
            hatirlatma.saatAyarlandi(Int(depo.reminderHour()))
        }
        return sonuc
    }

    // MARK: Çalışma süresi

    func calismaEkrani(_ ad: String?) {
        let onceki: String? = acikDersEkrani
        acikDersEkrani = izleyici.isStudyScreen(screen: ad) ? ad : nil
        izleyici.setActiveScreen(screen: ad)
        // Sahte akış: ders ekranı kapandı → ek süre (bkz. SahteSeriAyarlari.dersSonuEkSaniye).
        if onceki != nil && acikDersEkrani == nil && ayarlar.dersSonuEkSaniye > 0 {
            izleyici.addSecondsForDebug(seconds: Int32(ayarlar.dersSonuEkSaniye))
        }
    }

    // MARK: Eşitleme

    func esitle(tamam: @escaping () -> Void) {
        // Android: uid yoksa, gönderim sürüyorsa ya da gönderilecek bir şey yoksa sessizce döner.
        if bagliUid == nil || gonderiliyor {
            tamam()
            return
        }
        guard let istek: StreakSubmitRequest = StreakSync.shared.buildSubmitRequest(repo: depo, clock: saat) else {
            tamam()
            return
        }
        gonderiliyor = true
        DispatchQueue.main.asyncAfter(deadline: .now() + ayarlar.gecikmeSaniye) { [weak self] in
            guard let self = self else { return }
            self.gonderiliyor = false
            let hata: StreakServerError? = self.sunucu.submit(request: istek)
            if hata == nil {
                StreakSync.shared.onSubmitSucceeded(repo: self.depo, request: istek, response: self.sunucu.snapshot())
                self.bildir()
            }
            tamam()
        }
    }

    // MARK: Ayarlar

    var hedefDakika: Int {
        return Int(depo.goalMinutes())
    }

    func hedefiDegistir(_ dakika: Int) {
        depo.setGoalMinutes(minutes: Int32(dakika))
        bildir()
    }

    var hatirlatmaSaati: Int {
        return Int(depo.reminderHour())
    }

    func hatirlatmaSaatiniDegistir(_ saat: Int) {
        // Android: aynı saat ve gönderilmeyi bekleyen yoksa hiçbir şey yapılmaz.
        if saat == Int(depo.reminderHour()) && !depo.reminderHourPending() {
            return
        }
        depo.setReminderHour(hour: Int32(saat))
        hatirlatma.saatAyarlandi(saat)
        // Android 13+: izin yoksa burada sorulur; sonuç ne olursa olsun saat kayıtlı (yalnızca ölçüme gider).
        hatirlatma.izinIste(tamam: { _ in })
        bildir()
        esitle(tamam: {})
    }

    // MARK: Yeni seri sorusu

    func yeniSeriSorusuGerekli() -> Bool {
        return depo.needsNewStreakPrompt()
    }

    func yeniSeriSorusuGosterildi() {
        depo.markNewStreakPromptShown()
    }

    func yeniMeydanOkuma(_ gun: Int) {
        depo.startNewChallenge(days: Int32(gun))
        bildir()
        esitle(tamam: {})
    }

    // MARK: Kutlama ve bildirim

    func kutlamayiAl() -> Int {
        let seri: Int = Int(depo.pendingCelebration())
        if seri > 0 {
            depo.clearPendingCelebration()
        }
        return seri
    }

    func dondurmaBildiriminiAl() -> StreakFreezeNotice? {
        return depo.takeFreezeNotice()
    }

    // MARK: Ödüller

    func kilometreTasiTopla(_ tas: Int, tamam: @escaping (SeriIslemSonucu) -> Void) {
        if bagliUid == nil {
            tamam(SeriIslemSonucu(basarili: false, mesaj: Metin.al("ui_odulu_almak_icin_giris_yapman_gerekiyor")))
            return
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + ayarlar.gecikmeSaniye) { [weak self] in
            guard let self = self else { return }
            let sonuc: StreakClaimResult = self.sunucu.claimMilestone(milestone: Int32(tas))
            if let hata = sonuc.error {
                tamam(SeriIslemSonucu(basarili: false, mesaj: SahteSeriServisi.hataMetni(hata, "ui_odul_alinamadi")))
                return
            }
            StreakSync.shared.onMilestoneClaimed(repo: self.depo, milestone: Int32(tas))
            self.oduluCuzdanaYaz(sonuc)
            tamam(SeriIslemSonucu(basarili: true, mesaj: SahteSeriServisi.kazanildiMetni(sonuc)))
            self.bildir()
        }
    }

    func meydanOkumaOdulunuTopla(tamam: @escaping (SeriIslemSonucu) -> Void) {
        if bagliUid == nil {
            tamam(SeriIslemSonucu(basarili: false, mesaj: Metin.al("ui_odulu_almak_icin_giris_yapman_gerekiyor")))
            return
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + ayarlar.gecikmeSaniye) { [weak self] in
            guard let self = self else { return }
            let sonuc: StreakClaimResult = self.sunucu.claimChallenge()
            if let hata = sonuc.error {
                tamam(SeriIslemSonucu(basarili: false, mesaj: SahteSeriServisi.hataMetni(hata, "ui_odul_alinamadi")))
                return
            }
            StreakSync.shared.onChallengeClaimed(repo: self.depo, serverChallengeDays: sonuc.challengeDays)
            self.oduluCuzdanaYaz(sonuc)
            tamam(SeriIslemSonucu(basarili: true, mesaj: SahteSeriServisi.kazanildiMetni(sonuc)))
            self.bildir()
        }
    }

    /// Sunucu ödülü cüzdana kendisi ekler (Android: grantWalletDelta); burada sahte cüzdana yazılır.
    private func oduluCuzdanaYaz(_ sonuc: StreakClaimResult) {
        let altin: Int = Int(sonuc.rewardGold)
        let anahtar: Int = Int(sonuc.rewardKeys)
        if altin != 0 || anahtar != 0 {
            cuzdan.sunucuDegistirdi(altinFarki: altin, anahtarFarki: anahtar, sobiFarki: 0)
        }
    }

    /// Android: StreakSyncService.claim → "X kazandın!" ("N anahtar" Android'de koda gömülü Türkçe).
    private static func kazanildiMetni(_ sonuc: StreakClaimResult) -> String {
        var parcalar: [String] = []
        if sonuc.rewardKeys > 0 {
            parcalar.append(String(sonuc.rewardKeys) + " anahtar")
        }
        if sonuc.rewardGold > 0 {
            parcalar.append(Metin.al("ui_x_altin", Int(sonuc.rewardGold)))
        }
        return Metin.al("ui_x_kazandin", parcalar.joined(separator: " + "))
    }

    /// Sunucunun hata kodu → kullanıcı metni (ortak kod: StreakScreenLogic.errorText).
    private static func hataMetni(_ hata: StreakServerError, _ olmadiAnahtari: String) -> String {
        let metin: StreakErrorText = StreakScreenLogic.shared.errorText(
            code: hata.code,
            serverMessage: hata.message,
            failedKey: olmadiAnahtari,
            turkish: Metin.dil == "tr"
        )
        return SeriMetin.hata(metin)
    }

    // MARK: Dondurma

    /// Android: StreakSyncService.buyFreeze (`buyStreakFreeze` { today }). Başarıda yeni altın bakiyesi.
    func dondurmaSatinAl(tamam: @escaping (_ basarili: Bool, _ mesaj: String?, _ yeniAltin: Int?) -> Void) {
        if bagliUid == nil {
            tamam(false, Metin.al("ui_seri_dondurma_almak_icin_giris_yapman"), nil)
            return
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + ayarlar.gecikmeSaniye) { [weak self] in
            guard let self = self else { return }
            let bugun: String = StreakDays.shared.dayId(clock: self.saat, offsetDays: 0)
            let sonuc: StreakFreezeBuyResult = self.sunucu.buyFreeze(today: bugun, currency: Int32(self.cuzdan.cuzdan.altin))
            if let hata = sonuc.error {
                tamam(false, SahteSeriServisi.hataMetni(hata, "ui_satin_alinamadi"), nil)
                return
            }
            // Sunucu altını aynı işlemde düşer.
            let bedel: Int = Int(StreakRules.shared.FREEZE_COST_GOLD)
            self.cuzdan.sunucuDegistirdi(altinFarki: -bedel, anahtarFarki: 0, sobiFarki: 0)
            StreakSync.shared.onFreezeBought(repo: self.depo, freezes: sonuc.freezes, freezeDay: sonuc.freezeDay)
            tamam(true, nil, self.cuzdan.cuzdan.altin)
            self.bildir()
        }
    }

    // MARK: Dinleyiciler

    func dinle(_ dinleyici: @escaping () -> Void) -> UUID {
        let kimlik = UUID()
        dinleyiciler[kimlik] = dinleyici
        return kimlik
    }

    func dinlemeyiBirak(_ kimlik: UUID) {
        dinleyiciler[kimlik] = nil
    }

    private func bildir() {
        let liste: [() -> Void] = Array(dinleyiciler.values)
        for dinleyici in liste {
            dinleyici()
        }
    }

    // MARK: Ekran galerisi

    /// Galeri için hazır durum (ortak kod: seedForPreview + sunucu modelinin seed'i).
    func ornekKur(
        seri: Int,
        enUzun: Int,
        bugunTamam: Bool,
        hedefDakika: Int,
        bugunDakika: Int,
        meydanGun: Int,
        meydanAlindi: Int,
        dondurma: Int,
        sunucuSerisi: Int,
        toplananlar: [Int]
    ) {
        let uid = "galeri-ogrenci"
        bagliUid = uid
        let kayma: Int32 = bugunTamam ? 0 : -1
        let toplanan: [KotlinInt] = toplananlar.map { KotlinInt(int: Int32($0)) }
        depo.seedForPreview(
            ownerUid: uid,
            current: Int32(seri),
            longest: Int32(enUzun),
            lastDayOffset: kayma,
            goalMinutes: Int32(hedefDakika),
            challengeDays: Int32(meydanGun),
            challengeClaimed: Int32(meydanAlindi),
            freezes: Int32(dondurma),
            serverCurrent: Int32(sunucuSerisi),
            claimed: toplanan
        )
        let sonGun: String = StreakDays.shared.dayId(clock: saat, offsetDays: kayma)
        sunucu.seed(
            current: Int32(sunucuSerisi),
            longest: Int32(enUzun),
            lastDay: seri > 0 ? sonGun : "",
            claimed: toplanan,
            challengeDays: Int32(meydanGun),
            challengeClaimed: Int32(meydanAlindi),
            freezes: Int32(dondurma),
            freezeDay: dondurma > 0 ? StreakDays.shared.dayId(clock: saat, offsetDays: 0) : ""
        )
        if bugunDakika > 0 {
            izleyici.addSecondsForDebug(seconds: Int32(bugunDakika * 60))
        }
        // Günlük "buradayım" bu gün yapılmış sayılsın: galeride eşitleme durumu değiştirmesin.
        depo.markDailyPing()
    }
}

// MARK: - Mağazanın seri dondurma kartı

/// Mağaza ekranı seri dondurma kartını bu servisten okur (tek doğruluk kaynağı).
extension SahteSeriServisi: MagazaSeriServisi {
    /// Android: StreakRepository.freezesHeld(ctx) > 0.
    var dondurmaVar: Bool {
        return depo.freezesHeld() > 0
    }

    /// Android: StreakSyncService.refreshFromServer (sahte: bekleyen gün ya da günlük bildirim varsa eşitler).
    func yenile(tamam: @escaping () -> Void) {
        esitle(tamam: tamam)
    }

    /// Android: StreakSyncService.buyFreeze.
    func dondurmaAl(tamam: @escaping (_ basarili: Bool, _ mesaj: String?, _ yeniAltin: Int?) -> Void) {
        dondurmaSatinAl(tamam: tamam)
    }
}

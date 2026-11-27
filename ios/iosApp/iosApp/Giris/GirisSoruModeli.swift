import Foundation
import Shared
import SwiftUI

// Kayıt soruları akışının durumu. Android: UserInfoFragment (fragment_user_info.xml).
//
// Öğrenci akışı (SignupFlowRules.flowSteps(streakStepsEnabled = true)):
//   tanışma (MEET) → özet (BRIEF) → yaş → kaynak → abaküs → günlük hedef → ilerleme yolu →
//   meydan okuma girişi → meydan okuma → hatırlatma saati
// Öğretmen akışı: yaş → kaynak. Adımlar, ilerleme yüzdesi ve soru sayısı ortak koddan gelir
// (SignupFlowRules); Swift yalnızca hangi adımın ekranda olduğunu tutar.
//
// Cevaplar hesap açılmadan sunucuya yazılmaz: yaş (doğum yılı) ve kaynak kayıt çağrısıyla gider;
// abaküs seviyesi, hedef, meydan okuma ve saat cihazda bekler (GirisYerelDepo) ve hesap açılınca
// uygulanır (Android: StreakRepository.applyPendingSignup, SorobanLevel.applyFromUserDoc).

/// UserInfoFragment.Step; sıra SignupFlowRules.Step ile aynıdır.
enum GirisSoruAdimi: Int, CaseIterable {
    case tanisma
    case ozet
    case yas
    case kaynak
    case abakus
    case hedef
    case yol
    case meydanGiris
    case meydan
    case hatirlatma

    var kotlin: SignupFlowRules.Step {
        switch self {
        case .tanisma: return SignupFlowRules.Step.meet
        case .ozet: return SignupFlowRules.Step.brief
        case .yas: return SignupFlowRules.Step.age
        case .kaynak: return SignupFlowRules.Step.source
        case .abakus: return SignupFlowRules.Step.soroban
        case .hedef: return SignupFlowRules.Step.goal
        case .yol: return SignupFlowRules.Step.roadmap
        case .meydanGiris: return SignupFlowRules.Step.challengeIntro
        case .meydan: return SignupFlowRules.Step.challenge
        case .hatirlatma: return SignupFlowRules.Step.reminder
        }
    }

    init?(kotlin: SignupFlowRules.Step) {
        for aday in GirisSoruAdimi.allCases where aday.kotlin == kotlin {
            self = aday
            return
        }
        return nil
    }

    /// Step.isIntro: tanışma ekranları (ilerleme çubuğu gizli, sayılmaz).
    var tanismaMi: Bool {
        return self == .tanisma || self == .ozet
    }

    /// Soru başlığı (maskot + balon) görünen adımlar.
    var basliklimi: Bool {
        switch self {
        case .yas, .kaynak, .abakus, .hedef, .yol:
            return true
        default:
            return false
        }
    }

    /// Seri maskotunun (170dp, ortada) göründüğü adımlar.
    var seriMaskotluMu: Bool {
        return self == .meydanGiris || self == .meydan || self == .hatirlatma
    }

    /// Kayan kabın kimliği: aynı kabı paylaşan adımlar arasında Android kabı çıkarıp yeni içerikle
    /// geri sokar; burada her adım ayrı kimlik alır, tanışmanın iki ekranı hariç (onlar arasında
    /// kayma yok, yalnızca balondaki yazı değişir).
    var kapKimligi: Int {
        return tanismaMi ? -1 : rawValue
    }
}

/// Seçenek satırlarının bir satırı (StreakViews.buildOptionRows).
struct GirisSecenekSatiri: Identifiable {
    let id: Int
    let etiket: String
    let sagMetin: String
    let secili: Bool
}

/// Kayıt sorularının sonucu: kayıt ekranına taşınan değerler.
struct GirisSoruSonucu {
    let dogumYili: Int
    let kaynak: String
}

@MainActor
final class GirisSoruModeli: ObservableObject {
    /// Android: Step geçişi ve ilerleme çubuğu süresi (STEP_ANIM_MS).
    static let gecisSuresi: Double = 0.22
    /// Tanışmada el sallama / alkış arasındaki bekleme (INTRO_PAUSE_MS).
    static let tanismaAraMs: Double = 2000
    /// Balondaki daktilonun harf başı süresi (TYPE_MS_PER_CHAR).
    static let harfMs: Double = 22

    let ogretmen: Bool
    /// SignupFlowRules.streakStepsEnabled(forceTeacher).
    let seriAdimlari: Bool
    /// Bu akışta gösterilen adımlar, sırayla.
    let adimlar: [GirisSoruAdimi]

    @Published private(set) var adim: GirisSoruAdimi
    /// Son geçişin yönü (kayma yönü).
    @Published private(set) var ileriYon: Bool = true
    /// İlerleme çubuğunun yüzdesi (0–100).
    @Published private(set) var ilerleme: Double = 0
    @Published var yasMetni: String = ""
    @Published private(set) var seciliKaynak: String? = nil
    @Published private(set) var seciliAbakus: String? = nil
    @Published private(set) var hedefDakika: Int
    /// Meydan okuma bilerek ön seçimsiz (0): kullanıcı bu sözü kendisi vermeli.
    @Published private(set) var meydanGun: Int = 0
    @Published private(set) var hatirlatmaSaati: Int
    /// İlerleme yolu çizimi bitti mi (bitene kadar düğme kapalı).
    @Published private(set) var yolBitti: Bool = false
    /// "Başka bir saat" penceresi açık mı.
    @Published var saatPenceresiAcik: Bool = false
    /// Soru başlığındaki maskotun hâli: defter elde bekliyor; cevapta yazıyor.
    @Published private(set) var soruMaskotHali: String = "NOTEBOOK_HOLD"
    /// İlerleme yolunun yeniden oynatılması için (geri dönünce baştan oynar).
    @Published private(set) var yolOynatmaSayaci: Int = 0

    private(set) var dogrulananDogumYili: Int? = nil
    /// Hatırlatma cevaplandı, kayıt ekranına geçiliyor: ikinci dokunuş ikinci kez geçirmesin.
    private var hatirlatmaCevaplandi: Bool = false
    /// Adım geçişi sürüyor; bu sırada ileri/geri girdileri yok sayılır.
    private var gecisSuruyor: Bool = false
    private var yazmaGorevi: Task<Void, Never>? = nil

    private let depo: GirisYerelDepo
    private let bildirimServisi: GirisBildirimServisi
    private let bildir: (String) -> Void
    private let kapat: () -> Void
    private let bitti: (GirisSoruSonucu) -> Void

    init(
        ogretmen: Bool,
        depo: GirisYerelDepo,
        bildirimServisi: GirisBildirimServisi,
        bildir: @escaping (String) -> Void,
        kapat: @escaping () -> Void,
        bitti: @escaping (GirisSoruSonucu) -> Void
    ) {
        self.ogretmen = ogretmen
        self.depo = depo
        self.bildirimServisi = bildirimServisi
        self.bildir = bildir
        self.kapat = kapat
        self.bitti = bitti
        let kurallar = SignupFlowRules.shared
        let seri: Bool = kurallar.streakStepsEnabled(forceTeacher: ogretmen)
        seriAdimlari = seri
        let kotlinAdimlari: [SignupFlowRules.Step] = kurallar.flowSteps(streakStepsEnabled: seri)
        let liste: [GirisSoruAdimi] = kotlinAdimlari.compactMap { GirisSoruAdimi(kotlin: $0) }
        let akis: [GirisSoruAdimi] = liste.isEmpty ? [GirisSoruAdimi.yas, GirisSoruAdimi.kaynak] : liste
        let ilk: GirisSoruAdimi = akis[0]
        adimlar = akis
        adim = ilk
        // Seçimler cihazdaki değerden değil varsayılandan başlar (Android: goalMinutes =
        // GOAL_OPTIONS.first(), reminderHour = DEFAULT_REMINDER_HOUR).
        let hedefler: [Int] = kurallar.GOAL_OPTIONS.map { Int($0.int32Value) }
        hedefDakika = hedefler.first ?? 5
        hatirlatmaSaati = Int(kurallar.DEFAULT_REMINDER_HOUR)
        ilerleme = GirisSoruModeli.yuzde(ilk, seri: seri)
    }

    // MARK: Ortak kod sarmalayıcıları

    static func yuzde(_ adim: GirisSoruAdimi, seri: Bool) -> Double {
        let deger: Int32 = SignupFlowRules.shared.progressFor(step: adim.kotlin, streakStepsEnabled: seri)
        return Double(deger)
    }

    var hedefSecenekleri: [Int] {
        return SignupFlowRules.shared.GOAL_OPTIONS.map { Int($0.int32Value) }
    }

    var meydanSecenekleri: [Int] {
        return SignupFlowRules.shared.CHALLENGE_OPTIONS.map { Int($0.int32Value) }
    }

    var hazirSaatler: [Int] {
        return SignupStreakRules.shared.REMINDER_HOUR_OPTIONS.map { Int($0.int32Value) }
    }

    /// Tanışmadaki "N kısa sorum var" sayısı (Step.asks).
    var soruSayisi: Int {
        return Int(SignupFlowRules.shared.questionCount(streakStepsEnabled: seriAdimlari))
    }

    // MARK: Devam düğmesi

    var devamEtkin: Bool {
        switch adim {
        case .tanisma, .ozet, .meydanGiris:
            return true
        case .yas:
            return UserInfoRules.shared.isValidAge(rawAge: yasMetni)
        case .kaynak:
            return seciliKaynak != nil
        case .abakus:
            return seciliAbakus != nil
        case .hedef:
            return hedefSecenekleri.contains(hedefDakika)
        case .yol:
            return yolBitti
        case .meydan:
            return meydanSecenekleri.contains(meydanGun)
        case .hatirlatma:
            return hatirlatmaSaati >= 0 && hatirlatmaSaati <= 23
        }
    }

    /// UserInfoFragment.bindStep: düğme metni.
    var devamMetni: String {
        switch adim {
        case .meydan:
            return GirisMetin.hedefimiOnayla
        case .hatirlatma:
            return Metin.al("ui_evet_hatirlat")
        default:
            return Metin.al("ui_devam_et_2")
        }
    }

    // MARK: Seçimler

    func kaynakSec(_ deger: String) {
        seciliKaynak = deger
    }

    func abakusSec(_ deger: String) {
        seciliAbakus = deger
    }

    func hedefSec(_ dakika: Int) {
        hedefDakika = dakika
    }

    func meydanSec(_ gun: Int) {
        meydanGun = gun
    }

    func saatSec(_ saat: Int) {
        hatirlatmaSaati = saat
        saatPenceresiAcik = false
    }

    func yolCizimiBitti() {
        if adim == .yol {
            yolBitti = true
        }
    }

    /// Kayıt ekranından geri dönüldü (UserInfoFragment.onResume): hatırlatma yeniden cevaplanabilsin.
    func yenidenGorundu() {
        hatirlatmaCevaplandi = false
    }

    // MARK: İleri

    func devam() {
        if gecisSuruyor || !devamEtkin { return }
        switch adim {
        case .tanisma:
            gec(.ozet, ileri: true)
        case .ozet:
            gec(.yas, ileri: true)
        case .yas:
            yasiDogrula()
        case .kaynak:
            guard dogrulananDogumYili != nil, seciliKaynak != nil else { return }
            deftereYaz()
            if seriAdimlari {
                gec(.abakus, ileri: true)
            } else {
                bitir()
            }
        case .abakus:
            guard let seviye = seciliAbakus else { return }
            // Hesap henüz yok: cevap cihazda bekliyor, hesap açılınca yazılıyor.
            depo.abakusSeviyesiniKaydet(seviye, simdiMs: GirisYerelDepo.simdiMs())
            deftereYaz()
            gec(.hedef, ileri: true)
        case .hedef:
            deftereYaz()
            gec(.yol, ileri: true)
        case .yol:
            gec(.meydanGiris, ileri: true)
        case .meydanGiris:
            gec(.meydan, ileri: true)
        case .meydan:
            gec(.hatirlatma, ileri: true)
        case .hatirlatma:
            Task { await hatirlatmayiCevapla(izinIste: true) }
        }
    }

    /// "Şimdi değil": saat yine kaydedilir, izin sorulmaz.
    func simdiDegil() {
        if gecisSuruyor || adim != .hatirlatma { return }
        Task { await hatirlatmayiCevapla(izinIste: false) }
    }

    private func yasiDogrula() {
        let buYil = Int32(Calendar.current.component(.year, from: Date()))
        let dogumYili: Int32 = UserInfoRules.shared.birthYearFromRawAge(rawAge: yasMetni, currentYear: buYil)
        if dogumYili <= 0 {
            bildir(Metin.al("ui_lutfen_gecerli_bir_yas_girin"))
            return
        }
        dogrulananDogumYili = Int(dogumYili)
        GirisStil.klavyeyiKapat()
        deftereYaz()
        gec(.kaynak, ileri: true)
    }

    /// Android answerReminder: seri cevaplarının hepsi burada, kayda geçmeden bekleyen cevaplara
    /// yazılır; "evet"te bildirim izni istenir ve sonuç ne olursa olsun kayda devam edilir.
    private func hatirlatmayiCevapla(izinIste: Bool) async {
        if hatirlatmaCevaplandi { return }
        hatirlatmaCevaplandi = true
        depo.seriCevaplariniKaydet(
            hedefDakika: hedefDakika,
            meydanGun: meydanGun,
            hatirlatmaSaati: hatirlatmaSaati,
            simdiMs: GirisYerelDepo.simdiMs()
        )
        depo.bildirimIzniSoruldu()
        if izinIste {
            // Sahte servis sistem penceresini AÇMAZ (bkz. GirisServisleri.swift).
            _ = await bildirimServisi.izinIste()
        }
        bitir()
    }

    private func bitir() {
        guard let yil = dogrulananDogumYili, let kaynak = seciliKaynak else { return }
        bitti(GirisSoruSonucu(dogumYili: yil, kaynak: kaynak))
    }

    // MARK: Geri

    /// Geri: bir önceki adıma döner, ilk adımdaysa ekranı kapatır (UserInfoFragment.onBackStep).
    func geri() {
        if gecisSuruyor { return }
        guard let sira = adimlar.firstIndex(of: adim) else {
            kapat()
            return
        }
        if sira > 0 {
            gec(adimlar[sira - 1], ileri: false)
        } else {
            kapat()
        }
    }

    // MARK: Geçiş

    /// Android showStep: ilerleme çubuğu akar, kap kayar, içerik yenilenir.
    private func gec(_ yeni: GirisSoruAdimi, ileri: Bool) {
        gecisSuruyor = true
        // Önce yön çizilsin (kalkan görünüm geçişini son çiziminden alır), bir kare sonra adım.
        ileriYon = ileri
        let hedefYuzde: Double = GirisSoruModeli.yuzde(yeni, seri: seriAdimlari)
        let kilit: UInt64 = UInt64(GirisSoruModeli.gecisSuresi * 2 * 1_000_000_000)
        Task { [weak self] in
            try? await Task.sleep(nanoseconds: 20_000_000)
            guard let model = self else { return }
            withAnimation(.easeInOut(duration: GirisSoruModeli.gecisSuresi)) {
                model.adim = yeni
                model.ilerleme = hedefYuzde
            }
            model.adimaGirildi(yeni)
            try? await Task.sleep(nanoseconds: kilit)
            model.gecisSuruyor = false
        }
    }

    private func adimaGirildi(_ yeni: GirisSoruAdimi) {
        if yeni == .yol {
            // Düğme yol çizimi bitene kadar kapalı; geri dönülünce çizim baştan oynar.
            yolBitti = false
            yolOynatmaSayaci += 1
        }
        if yeni != .hatirlatma {
            saatPenceresiAcik = false
        }
    }

    /// Bilgi sorularında maskot cevabı deftere yazar, sonra defterle beklemeye döner
    /// (Android: play(NOTEBOOK_WRITE, then = NOTEBOOK_HOLD)).
    private func deftereYaz() {
        yazmaGorevi?.cancel()
        soruMaskotHali = "NOTEBOOK_WRITE"
        let sure: Double = SobiMaskotHal.NOTEBOOK_WRITE.sureMs
        yazmaGorevi = Task { [weak self] in
            try? await Task.sleep(nanoseconds: UInt64(sure * 1_000_000))
            if Task.isCancelled { return }
            self?.soruMaskotHali = "NOTEBOOK_HOLD"
        }
    }

    // MARK: Satırlar

    /// "Bizi nereden duydun?" satırları: değer SOURCE_NAMES (istatistik anahtarı), görünen ad
    /// Android `sources` listesinin ikinci öğesi (dile göre metin). Ortak koddaki
    /// SOURCE_DISPLAY_NAMES_TR'nin 7. öğesi ("Veli/WhatsApp") Android ekranındakinden
    /// (ui_baska_veli_whatsapp) farklı olduğu için görünen adlar buradan alınır.
    var kaynakSatirlari: [GirisSecenekSatiri] {
        let degerler: [String] = UserInfoRules.shared.SOURCE_NAMES
        let gorunenler: [String] = GirisSoruModeli.kaynakGorunenAdlari
        var sonuc: [GirisSecenekSatiri] = []
        for (sira, deger) in degerler.enumerated() {
            let ad: String = sira < gorunenler.count ? gorunenler[sira] : deger
            sonuc.append(GirisSecenekSatiri(id: sira, etiket: ad, sagMetin: "", secili: deger == seciliKaynak))
        }
        return sonuc
    }

    /// UserInfoFragment.sources (ikinci öğeler), SOURCE_NAMES ile aynı sırada.
    static var kaynakGorunenAdlari: [String] {
        return [
            "Facebook",
            "Instagram",
            "YouTube",
            Metin.al("ui_google_aramasi"),
            Metin.al("ui_arkadas_aile"),
            Metin.al("ui_okul_ogretmen"),
            Metin.al("ui_baska_veli_whatsapp"),
            "TikTok",
            Metin.al("ui_uygulama_magazasi"),
            Metin.al("ui_diger"),
        ]
    }

    func kaynakSecildi(_ sira: Int) {
        let degerler: [String] = UserInfoRules.shared.SOURCE_NAMES
        if sira >= 0 && sira < degerler.count {
            kaynakSec(degerler[sira])
        }
    }

    var abakusSatirlari: [GirisSecenekSatiri] {
        let degerler: [String] = SignupFlowRules.shared.SOROBAN_LEVELS
        let etiketler: [String] = [
            Metin.al("ui_abakus_hic_bilmiyorum"),
            Metin.al("ui_abakus_biraz_biliyorum"),
            Metin.al("ui_abakus_iyi_biliyorum"),
        ]
        var sonuc: [GirisSecenekSatiri] = []
        for (sira, deger) in degerler.enumerated() {
            let ad: String = sira < etiketler.count ? etiketler[sira] : deger
            sonuc.append(GirisSecenekSatiri(id: sira, etiket: ad, sagMetin: "", secili: deger == seciliAbakus))
        }
        return sonuc
    }

    func abakusSecildi(_ sira: Int) {
        let degerler: [String] = SignupFlowRules.shared.SOROBAN_LEVELS
        if sira >= 0 && sira < degerler.count {
            abakusSec(degerler[sira])
        }
    }

    var hedefSatirlari: [GirisSecenekSatiri] {
        let etiketler: [String] = [Metin.al("ui_rahat"), Metin.al("ui_duzenli"), Metin.al("ui_ciddi")]
        var sonuc: [GirisSecenekSatiri] = []
        for (sira, dakika) in hedefSecenekleri.enumerated() {
            let ad: String = sira < etiketler.count ? etiketler[sira] : String(dakika)
            let sag: String = Metin.cogul("ui_x_dakika", dakika, dakika)
            sonuc.append(GirisSecenekSatiri(id: dakika, etiket: ad, sagMetin: sag, secili: dakika == hedefDakika))
        }
        return sonuc
    }

    var meydanSatirlari: [GirisSecenekSatiri] {
        var sonuc: [GirisSecenekSatiri] = []
        for gun in meydanSecenekleri {
            let odul: Int = Int(SignupStreakRules.shared.challengeReward(days: Int32(gun)))
            let ad: String = Metin.cogul("ui_x_gun", gun, gun)
            let sag: String = Metin.al("ui_x_altin_2", odul)
            sonuc.append(GirisSecenekSatiri(id: gun, etiket: ad, sagMetin: sag, secili: gun == meydanGun))
        }
        return sonuc
    }

    /// "Başka bir saat" satırının değeri (gerçek bir saat değil).
    static let ozelSaatSatiri: Int = -1

    /// StreakViews.buildReminderRows: hazır saatler + "Başka bir saat".
    var hatirlatmaSatirlari: [GirisSecenekSatiri] {
        let saatler: [Int] = hazirSaatler
        let ozel: Bool = !SignupStreakRules.shared.isPresetReminderHour(hour: Int32(hatirlatmaSaati))
        var sonuc: [GirisSecenekSatiri] = []
        for saat in saatler {
            let sag: String = SignupStreakRules.shared.hourText(hour: Int32(saat))
            let ad: String = GirisSoruModeli.saatEtiketi(saat)
            sonuc.append(GirisSecenekSatiri(id: saat, etiket: ad, sagMetin: sag, secili: !ozel && saat == hatirlatmaSaati))
        }
        let gecerli: Bool = hatirlatmaSaati >= 0 && hatirlatmaSaati <= 23
        let ozelSag: String = ozel && gecerli
            ? SignupStreakRules.shared.hourText(hour: Int32(hatirlatmaSaati))
            : Metin.al("ui_sec")
        sonuc.append(
            GirisSecenekSatiri(
                id: GirisSoruModeli.ozelSaatSatiri,
                etiket: Metin.al("ui_baska_bir_saat"),
                sagMetin: ozelSag,
                secili: ozel
            )
        )
        return sonuc
    }

    func hatirlatmaSatiriSecildi(_ deger: Int) {
        if deger == GirisSoruModeli.ozelSaatSatiri {
            saatPenceresiAcik = true
        } else {
            saatSec(deger)
        }
    }

    /// StreakViews.reminderHourLabel. "Okuldan sonra" ve "Yemekten sonra" Android'de koda gömülü
    /// Türkçe metindir (strings.xml'de yok).
    static func saatEtiketi(_ saat: Int) -> String {
        switch saat {
        case 16: return GirisMetin.okuldanSonra
        case 18: return Metin.al("ui_aksamustu")
        case 19: return Metin.al("ui_aksam")
        case 20: return GirisMetin.yemektenSonra
        default: return SignupStreakRules.shared.hourText(hour: Int32(saat))
        }
    }

    /// Saat penceresinin hücreleri, ekrandaki sırayla (06:00'dan başlar).
    var saatIzgarasi: [Int] {
        return SignupStreakRules.shared.hourGrid().map { Int($0.int32Value) }
    }

    // MARK: Örnek durum (ekran galerisi)

    /// Galeri için adımı ve tutarlı cevapları animasyonsuz kurar.
    func ornekAdimaGit(_ hedef: GirisSoruAdimi) {
        let degerler: [String] = UserInfoRules.shared.SOURCE_NAMES
        if hedef.rawValue > GirisSoruAdimi.yas.rawValue {
            yasMetni = "9"
            dogrulananDogumYili = Calendar.current.component(.year, from: Date()) - 9
        }
        if hedef.rawValue > GirisSoruAdimi.kaynak.rawValue {
            seciliKaynak = degerler.count > 5 ? degerler[5] : degerler.first
        }
        if hedef.rawValue > GirisSoruAdimi.abakus.rawValue {
            seciliAbakus = SignupFlowRules.shared.SOROBAN_LEVELS.first
        }
        if hedef.rawValue >= GirisSoruAdimi.hedef.rawValue {
            hedefDakika = hedefSecenekleri.count > 1 ? hedefSecenekleri[1] : hedefDakika
        }
        if hedef == .kaynak {
            seciliKaynak = degerler.count > 5 ? degerler[5] : degerler.first
        }
        if hedef == .meydan {
            meydanGun = meydanSecenekleri.count > 1 ? meydanSecenekleri[1] : 0
        }
        adim = hedef
        ilerleme = GirisSoruModeli.yuzde(hedef, seri: seriAdimlari)
        adimaGirildi(hedef)
    }
}

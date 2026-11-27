import SwiftUI

private typealias S = SobiMaskotSabit
private typealias K = SobiMaskotKanal

// Sobi maskotunun duruş motoru: Android BunnyMascotView'un "durum" bölümü (updateState,
// setIdlePose, play, hop, earsWiggle …) birebir. Hâllerin kendisi SobiMaskotMotoruHaller.swift
// ve SobiMaskotMotoruSahneler.swift'te, çizim SobiMaskotCizim*.swift'te.
//
// Nasıl çalışıyor (Android sınıf açıklamasından): her karede bir "duruş" hesaplanıyor. Oynayan
// hâl yalnızca kendini ilgilendiren kanalların HEDEFİNİ (`hedef`) yazıyor; hedefler `simdi`de
// üstel yumuşatılıyor, hızlı salınımlar `sal`a doğrudan yazılıp üstüne ekleniyor. Göz ve ağız
// türü o karede oynayan hâlinki.
//
// Durağan kip (animasyonsuz): hâl kendi `duraganAn`ında değerlendiriliyor, yumuşatma yok
// (hedef doğrudan alınıyor), bekleme salınımları (nefes, baş sallanması, göz kırpma, kulak
// oynatma, bakış kayması) sıfırda.
final class SobiMaskotMotoru: ObservableObject {
    var hedef: [CGFloat] = [CGFloat](repeating: 0, count: SobiMaskotKanal.sayi)
    var simdi: [CGFloat] = [CGFloat](repeating: 0, count: SobiMaskotKanal.sayi)
    var sal: [CGFloat] = [CGFloat](repeating: 0, count: SobiMaskotKanal.sayi)
    var goz: SobiMaskotGoz = .normal
    var agiz: SobiMaskotAgiz = .bekleme

    var hal: SobiMaskotHal? = nil
    var halBaslangicMs: Double = 0
    var baslangicMs: Double = 0
    var sonKareMs: Double = 0
    var duragan: Bool = false

    /// O karenin zamanı (ms), bekleme saati t (sn) ve hâlin içindeki zaman e (sn).
    var simdiMs: Double = 0
    var tSn: CGFloat = 0
    var eSn: CGFloat = 0

    // Göz kırpma, bakış, kulak oynatma
    var sonrakiKirpmaMs: Double = 1500
    var kirpmaBaslangicMs: Double = -1
    var beklemeBakisi: CGFloat = 0
    var sonrakiBakisMs: Double = 2200
    var kulakSeyirmeYonu: Int = 0
    var kulakSeyirmeBaslangicMs: Double = -1
    var sonrakiKulakMs: Double = 3000
    /// O karede gözlerin açıklığı (1 açık; kırpmada 0,1'e iniyor) ve kulak seyirmesi (derece).
    var gozAcikligi: CGFloat = 1
    var kulakSeyirmesi: CGFloat = 0

    var noktaSola: Bool = false
    var tacDususu: CGFloat = 0
    var gozlukDususu: CGFloat = 0
    var gozlukParilti: CGFloat = -1
    var isirikSayisi: Int = 0
    var durbunCevirme: CGFloat = 0
    var sonKafaDonusu: CGFloat = 0
    var mercekKirpma: CGFloat = 1
    var turSayisi: Int = 0

    /// Android `streakDays`, `alarmHour`, `starCount`: −1 = deneme ekranı davranışı.
    var seriGunu: Int = -1
    var seriDegisimMs: Double = 0
    var alarmSaati: Int = -1
    var alarmDegisimMs: Double = 0
    var yildizSayisi: Int = -1
    var alarmAcisi: CGFloat = 0

    private var istenenAd: String? = nil
    private var istenenAnimasyonlu: Bool = true

    init() {
        sifirla(0)
    }

    // MARK: Görünümden

    /// Görünümün verdiği hâli ve kipi uygular; değiştiyse hâl baştan oynuyor.
    func hazirla(ad: String, animasyonlu: Bool, simdiMs suan: Double) {
        if istenenAd == ad && istenenAnimasyonlu == animasyonlu { return }
        istenenAd = ad
        istenenAnimasyonlu = animasyonlu
        duragan = !animasyonlu
        sifirla(suan)
        // Bilinmeyen ad → nötr bekleme duruşu.
        hal = SobiMaskotHal(rawValue: ad)
        halBaslangicMs = suan
    }

    private func sifirla(_ suan: Double) {
        for i in 0..<K.sayi {
            hedef[i] = 0
            simdi[i] = 0
            sal[i] = 0
        }
        simdi[K.hangLA] = 1
        simdi[K.hangRA] = 1
        simdi[K.catL] = S.cheerUpL
        simdi[K.catR] = S.cheerUpR
        simdi[K.peace] = S.peaceRest
        simdi[K.think] = S.thinkUp
        simdi[K.flameH] = SobiMaskotMotoru.alevBoyu(0)
        baslangicMs = suan
        sonKareMs = 0
        sonrakiKirpmaMs = suan + 1500
        kirpmaBaslangicMs = -1
        beklemeBakisi = 0
        sonrakiBakisMs = suan + 2200
        kulakSeyirmeYonu = 0
        kulakSeyirmeBaslangicMs = -1
        sonrakiKulakMs = suan + 3000
        turSayisi = 0
        alarmAcisi = 0
        hal = nil
    }

    /// Bir kare: Android onDraw'ın başı (updateState) + drawEyes/drawEars'taki zaman hesapları.
    func kare(_ suan: Double) {
        if duragan {
            duraganKare()
            return
        }
        let dt: CGFloat = sonKareMs == 0 ? 0 : CGFloat(min(suan - sonKareMs, 100)) / 1000
        sonKareMs = suan
        simdiMs = suan
        tSn = CGFloat((suan - baslangicMs) / 1000)
        durumuGuncelle(suan, dt)
        eSn = CGFloat((suan - halBaslangicMs) / 1000)
        // Göz kırpma (Android drawEyes)
        gozAcikligi = 1
        if kirpmaBaslangicMs >= 0 {
            let p: CGFloat = CGFloat((suan - kirpmaBaslangicMs) / S.blinkMs)
            if p >= 1 {
                kirpmaBaslangicMs = -1
                sonrakiKirpmaMs = suan + Double.random(in: 1800..<4200)
            } else {
                gozAcikligi = 1 - 0.9 * sin(CGFloat.pi * p)
            }
        }
        // Kulak oynatma (Android drawEars)
        kulakSeyirmesi = 0
        if kulakSeyirmeBaslangicMs >= 0 {
            let p: CGFloat = SobiMaskotMat.sinirla(CGFloat((suan - kulakSeyirmeBaslangicMs) / S.earFlickMs), 0, 1)
            kulakSeyirmesi = 12 * sin(CGFloat.pi * p)
        }
    }

    private func duraganKare() {
        simdiMs = 0
        tSn = 0
        gozAcikligi = 1
        kulakSeyirmesi = 0
        kirpmaBaslangicMs = -1
        kulakSeyirmeBaslangicMs = -1
        beklemeBakisi = 0
        turSayisi = 0
        let e: CGFloat = hal?.duraganAn ?? 0
        eSn = e
        halBaslangicMs = -Double(e) * 1000
        beklemeDurusu()
        if let h = hal {
            haliUygula(h, e, 0)
        }
        for i in 0..<K.sayi {
            simdi[i] = hedef[i]
        }
        if hal == .ALARM {
            alarmAcisi = CGFloat(gosterilenAlarmSaati() % 12) * 30
        }
    }

    // MARK: Durum (Android updateState)

    private func durumuGuncelle(_ suan: Double, _ dt: CGFloat) {
        if kirpmaBaslangicMs < 0 && suan >= sonrakiKirpmaMs { kirpmaBaslangicMs = suan }
        if suan >= sonrakiBakisMs {
            let secenekler: [CGFloat] = [-1.5, 0, 0, 1.5]
            beklemeBakisi = secenekler.randomElement() ?? 0
            sonrakiBakisMs = suan + Double.random(in: 1500..<3500)
        }
        if kulakSeyirmeBaslangicMs < 0 && suan >= sonrakiKulakMs {
            kulakSeyirmeYonu = Bool.random() ? 1 : -1
            kulakSeyirmeBaslangicMs = suan
        }
        if kulakSeyirmeBaslangicMs >= 0 && suan - kulakSeyirmeBaslangicMs >= S.earFlickMs {
            kulakSeyirmeBaslangicMs = -1
            sonrakiKulakMs = suan + Double.random(in: 2500..<6000)
        }

        if let oynayan = hal, suan - halBaslangicMs >= oynayan.sureMs {
            let sure: Double = oynayan.sureMs
            if oynayan.tutmaMs >= 0 {
                // Bir kez oynadı; artık yalnızca son bölüm (bekleme) kendi içinde dönüyor.
                let tut: Double = oynayan.tutmaMs
                let fazla: Double = (suan - halBaslangicMs - tut).truncatingRemainder(dividingBy: sure - tut)
                halBaslangicMs = suan - tut - fazla
                turSayisi += 1
            } else if oynayan.dongu {
                let turlar: Double = ((suan - halBaslangicMs) / sure).rounded(.down)
                halBaslangicMs += turlar * sure
                turSayisi += Int(turlar)
            } else {
                // Tek seferlik hâl bitti: beklemeye dön (Android'de olduğu gibi).
                hal = nil
            }
        }

        beklemeDurusu()
        if let h = hal {
            let e: CGFloat = CGFloat((suan - halBaslangicMs) / 1000)
            haliUygula(h, e, suan)
        }

        // Üstel yumuşatma: kare hızından bağımsız.
        let k: CGFloat = 1 - exp(-dt * 14)
        for i in 0..<K.sayi {
            simdi[i] += (hedef[i] - simdi[i]) * k
        }
        if hal == .ALARM {
            let hedefAci: CGFloat = CGFloat(gosterilenAlarmSaati() % 12) * 30
            alarmAcisi += (hedefAci - alarmAcisi) * (1 - exp(-dt * S.alarmHandSpeed))
        }
    }

    /// Bekleme duruşu; oynayan hâl bunun üstüne yazıyor (Android setIdlePose).
    func beklemeDurusu() {
        let alevBoyu: CGFloat = simdi[K.flameH]
        for i in 0..<K.sayi {
            hedef[i] = 0
            sal[i] = 0
        }
        hedef[K.hangLA] = 1
        hedef[K.hangRA] = 1
        hedef[K.catL] = S.cheerUpL
        hedef[K.catR] = S.cheerUpR
        hedef[K.peace] = S.peaceRest
        hedef[K.think] = S.thinkUp
        hedef[K.flameH] = alevBoyu
        hedef[K.gazeX] = beklemeBakisi
        tacDususu = 0
        gozlukDususu = 0
        gozlukParilti = -1
        isirikSayisi = 0
        durbunCevirme = 0
        mercekKirpma = 1
        goz = .normal
        agiz = .bekleme
    }

    /// Değer = yumuşatılmış hedef + doğrudan salınım (Android `v`).
    func v(_ ch: Int) -> CGFloat {
        return simdi[ch] + sal[ch]
    }

    // MARK: Ortak yardımcılar (Android'deki adlarıyla)

    func bothCatArms() {
        hedef[K.hangLA] = 0
        hedef[K.hangRA] = 0
        hedef[K.catLA] = 1
        hedef[K.catRA] = 1
    }

    func frontHands() {
        hedef[K.hangLA] = 0
        hedef[K.hangRA] = 0
        hedef[K.frontA] = 1
    }

    func earsWiggle(_ e: CGFloat, _ env: CGFloat, base: CGFloat) {
        hedef[K.earL] = base
        hedef[K.earR] = base
        let dalga: CGFloat = 6 * sin(e * S.tau * 5) * env
        sal[K.earL] = dalga
        sal[K.earR] = dalga
    }

    /// [hops] sekmelik zıplama: yükseklik, ezilme/uzama, inişten sonra sönen yaylanma.
    func hop(_ e: CGFloat, hops: Int, hopS: CGFloat, height: CGFloat) {
        if e < 0 { return }
        let simdiki: CGFloat = SobiMaskotMotoru.hopLift(e, hops, hopS, height)
        let gecikmeli: CGFloat = SobiMaskotMotoru.hopLift(e - S.headLagS, hops, hopS, height)
        sal[K.lift] = simdiki
        sal[K.headDy] += simdiki - gecikmeli
        let p: CGFloat = e / hopS
        if p < CGFloat(hops) {
            let frac: CGFloat = p - floor(p)
            let ezilme: CGFloat = max(0, 1 - min(frac, 1 - frac) / 0.18)
            sal[K.squash] = ezilme - 0.5 * sin(CGFloat.pi * frac)
        } else {
            let sonra: CGFloat = e - CGFloat(hops) * hopS
            sal[K.squash] = exp(-sonra * 7) * cos(sonra * S.tau * 3)
        }
    }

    static func hopLift(_ e: CGFloat, _ hops: Int, _ hopS: CGFloat, _ height: CGFloat) -> CGFloat {
        let p: CGFloat = e / hopS
        if p < 0 || p >= CGFloat(hops) { return 0 }
        return height * sin(CGFloat.pi * (p - floor(p)))
    }

    static func alevBoyu(_ gun: Int) -> CGFloat {
        if gun >= 7 { return 46 }
        if gun >= 5 { return 38 }
        if gun >= 3 { return 30 }
        return 24
    }

    /// [e]'nin [from]–[to] aralığındaki ilerleyişi, 0..1.
    static func seg(_ e: CGFloat, _ from: CGFloat, _ to: CGFloat) -> CGFloat {
        return SobiMaskotMat.sinirla((e - from) / (to - from), 0, 1)
    }

    static func smooth01(_ x: CGFloat) -> CGFloat {
        let c: CGFloat = SobiMaskotMat.sinirla(x, 0, 1)
        return c * c * (3 - 2 * c)
    }

    /// Hafif taşan beliriş: 0 → ~1,2 → 1 (0,3 sn). Henüz zamanı gelmediyse 0.
    static func popScale(_ e: CGFloat) -> CGFloat {
        if e <= 0 { return 0 }
        let p: CGFloat = min(e / 0.3, 1)
        return p + 0.25 * sin(CGFloat.pi * p)
    }

    /// ([px], [py]) noktasını ([cx], [cy]) etrafında [deg] derece (saat yönü +) döndürür.
    static func rotateAround(_ px: CGFloat, _ py: CGFloat, _ cx: CGFloat, _ cy: CGFloat, _ deg: CGFloat) -> CGPoint {
        let r: CGFloat = SobiMaskotMat.radyan(deg)
        let c: CGFloat = cos(r)
        let s: CGFloat = sin(r)
        let dx: CGFloat = px - cx
        let dy: CGFloat = py - cy
        return CGPoint(x: cx + dx * c - dy * s, y: cy + dx * s + dy * c)
    }

    /// Yukarıdan düşen nesnenin dikey kayması (negatif = yukarıda).
    static func dropOffset(_ t: CGFloat) -> CGFloat {
        if t < 0 { return -S.adDropHeight }
        if t < S.adDropS {
            let p: CGFloat = t / S.adDropS
            return -S.adDropHeight * (1 - p * p)
        }
        let q: CGFloat = t - S.adDropS
        return -5 * abs(sin(q * S.tau * 2)) * exp(-q * 6)
    }

    /// İnişte kafanın kısa süre aşağı sarsılması.
    static func landBump(_ t: CGFloat) -> CGFloat {
        if t < 0 || t > 0.3 { return 0 }
        return 2.5 * sin(CGFloat.pi * t / 0.3)
    }

    static func adTvX(_ e: CGFloat) -> CGFloat {
        return S.adTvStartX - S.adTvSpeed * (e - S.adTvEnterS)
    }

    static func wandWave(_ e: CGFloat) -> CGFloat {
        let t: CGFloat = e - S.wandUpS
        return 14 * sin(t * S.tau * S.wandHz) + 4 * sin(t * S.tau * S.wandHz * 2)
    }

    /// Değnek ucunun gövde koordinatındaki yeri, kol açısı [up] iken.
    static func wandTip(_ up: CGFloat) -> CGPoint {
        let rad: CGFloat = SobiMaskotMat.radyan(S.wandDirDeg)
        let lx: CGFloat = S.catHandX + S.catDx + cos(rad) * S.wandLen
        let ly: CGFloat = S.catHandY + sin(rad) * S.wandLen
        return rotateAround(lx, ly, S.cheerShoulderRX, S.cheerShoulderRY, -up)
    }

    // MARK: Seri, saat, takvim

    func flameDays(_ e: CGFloat) -> Int {
        if seriGunu >= 0 { return seriGunu }
        let i: Int = max(0, min(2, Int(e / S.flameStepS)))
        return S.demoDays[i]
    }

    func flameReactAge(_ e: CGFloat, _ suan: Double) -> CGFloat {
        if seriGunu < 0 && e < S.flameStepS && turSayisi > 0 { return CGFloat.greatestFiniteMagnitude }
        if seriGunu < 0 { return SobiMaskotMat.kalan(e, S.flameStepS) }
        if seriDegisimMs == 0 { return CGFloat.greatestFiniteMagnitude }
        return CGFloat((suan - seriDegisimMs) / 1000)
    }

    func gosterilenAlarmSaati() -> Int {
        if alarmSaati >= 0 { return alarmSaati }
        return S.demoAlarmHours[turSayisi % S.demoAlarmHours.count]
    }

    func alarmRingAge(_ e: CGFloat, _ suan: Double) -> CGFloat {
        if alarmDegisimMs != 0 && alarmSaati >= 0 {
            let tepki: CGFloat = CGFloat((suan - alarmDegisimMs) / 1000)
            if tepki <= S.alarmRingS { return tepki }
            if tepki < S.alarmRingS + S.alarmQuietAfterS { return -1 }
        }
        return e - S.alarmRingAtS
    }

    static func alarmRingEnv(_ age: CGFloat) -> CGFloat {
        if age < 0 || age > S.alarmRingS { return 0 }
        let giris: CGFloat = min(age / 0.08, 1)
        let cikis: CGFloat = min((S.alarmRingS - age) / 0.15, 1)
        return giris * cikis
    }

    func calendarDays() -> Int {
        if seriGunu >= 0 { return seriGunu }
        return S.demoDays[turSayisi % S.demoDays.count]
    }

    static func calendarDoneS(_ n: Int) -> CGFloat {
        return S.calFirstTickS + CGFloat(max(0, n - 1)) * S.calTickS + 0.3
    }

    func shownStars() -> Int {
        if yildizSayisi >= 0 { return max(0, min(3, yildizSayisi)) }
        return 3
    }
}

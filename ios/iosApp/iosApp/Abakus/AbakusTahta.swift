import QuartzCore
import Shared
import UIKit

// Abaküsün UIKit görünümü: çerçeve, 5 çubuk, çubuk başına 1 üst + 4 alt boncuk.
//
// Android kaynakları: res/layout/fragment_abacus.xml (abacusContainer / abacusLinear / rod0..4;
// fragment_abacus_practice.xml aynı yerleşimi kullanır), abacus/AbacusBeadController.kt.
//
// Boncukların durumu ve bütün kararlar (hangi boncuk hangisini iter, sürükleme eşiği, yarı yolda
// bırakınca hangi yöne yapışır, animasyon süresi) ortak koddaki AbacusBeadController'dadır.
// Bu dosya yalnızca: (1) dokunmaları ham hâliyle denetleyiciye iletir, (2) denetleyicinin
// istediği animasyonları kare kare oynatıp ara değeri modele geri yazar, (3) boncukları
// modeldeki konuma çizer.
//
// Birimler: denetleyici Android'deki gibi PİKSEL ile çalışır (yoğunluk = ekran ölçeği);
// çizimde piksel / ölçek = pt.

/// Boncuk tık sesi (Android: AbacusSoundPlayer.playBeadClick). `etkinlesiyor` false ise boncuk
/// geri alınıyordur (Android'de 0.75 perdeyle çalınır).
protocol AbakusBoncukSesi: AnyObject {
    func boncukSesi(etkinlesiyor: Bool)
}

/// Ortak koddaki AbacusBeadHost arayüzünün uygulaması. Ayrı bir nesnedir çünkü denetleyici
/// bunu güçlü tutar; görünümü zayıf tutarak döngü önlenir.
final class AbakusBoncukEvSahibi: NSObject, AbacusBeadHost {
    private struct Animasyon {
        let kimlik: Int
        let boncuk: AbacusBead
        let baslangic: Float
        let hedef: Float
        let baslamaAni: CFTimeInterval
        let sure: CFTimeInterval
        let bitince: () -> Void
    }

    weak var tahta: AbakusTahtaGorunumu?
    weak var ses: AbakusBoncukSesi?

    private var animasyonlar: [Int: Animasyon] = [:]
    private var sayac: Int = 0
    private var kareZamanlayici: CADisplayLink?

    /// `bead.animate().setDuration(ms).setInterpolator(AccelerateDecelerateInterpolator())
    /// .withEndAction(endAction).translationY(hedef).start()` karşılığı.
    func animateTranslationY(
        bead: AbacusBead, targetTranslationY: Float, durationMs: Int64, endAction: @escaping () -> Void
    ) {
        let kod = Int(AbakusBoncukKoprusu.shared.yuvaKodu(boncuk: bead))
        sayac += 1
        // Aynı boncukta süren animasyonun yerini alır; öncekinin bitiş eylemi çalışmaz.
        animasyonlar[kod] = Animasyon(
            kimlik: sayac,
            boncuk: bead,
            baslangic: bead.translationY,
            hedef: targetTranslationY,
            baslamaAni: CACurrentMediaTime(),
            sure: Double(durationMs) / 1000.0,
            bitince: endAction
        )
        zamanlayiciyiBaslat()
    }

    /// `bead.animate().cancel()`: boncuk o anki konumunda kalır, bitiş eylemi çalışmaz.
    func cancelAnimation(bead: AbacusBead) {
        let kod = Int(AbakusBoncukKoprusu.shared.yuvaKodu(boncuk: bead))
        animasyonlar[kod] = nil
    }

    func postDelayed(delayMs: Int64, action: @escaping () -> Void) {
        let gecikme: Double = Double(delayMs) / 1000.0
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme) {
            action()
        }
    }

    func playBeadClick(activating: Bool) {
        ses?.boncukSesi(etkinlesiyor: activating)
    }

    func updateBeadAppearance(bead: AbacusBead, isSelected: Bool) {
        let kod = Int(AbakusBoncukKoprusu.shared.yuvaKodu(boncuk: bead))
        tahta?.boncukGorunumunuGuncelle(kod: kod, secili: isSelected)
    }

    var animasyonVar: Bool {
        return !animasyonlar.isEmpty
    }

    func durdur() {
        animasyonlar.removeAll()
        kareZamanlayici?.invalidate()
        kareZamanlayici = nil
    }

    private func zamanlayiciyiBaslat() {
        if kareZamanlayici != nil { return }
        let yeni = CADisplayLink(target: self, selector: #selector(kare))
        yeni.add(to: .main, forMode: .common)
        kareZamanlayici = yeni
    }

    /// Android'in AccelerateDecelerateInterpolator'ı: cos((t + 1)π) / 2 + 0.5.
    private func yumusat(_ t: Double) -> Double {
        return cos((t + 1.0) * Double.pi) / 2.0 + 0.5
    }

    @objc private func kare() {
        guard let tahta = tahta else {
            durdur()
            return
        }
        let simdi = CACurrentMediaTime()
        let anlik = Array(animasyonlar)
        var bitenler: [Animasyon] = []
        for (kod, a) in anlik {
            // Bu turda başka bir animasyonun bitiş eylemi bunu iptal etmiş ya da değiştirmiş olabilir.
            guard let guncel = animasyonlar[kod], guncel.kimlik == a.kimlik else { continue }
            var oran: Double = 1.0
            if a.sure > 0 {
                oran = min(max((simdi - a.baslamaAni) / a.sure, 0.0), 1.0)
            }
            if oran >= 1.0 {
                a.boncuk.translationY = a.hedef
                animasyonlar[kod] = nil
                bitenler.append(a)
            } else {
                let fark: Float = a.hedef - a.baslangic
                a.boncuk.translationY = a.baslangic + fark * Float(yumusat(oran))
            }
        }
        for a in bitenler {
            a.bitince()
        }
        tahta.modeldenYerlestir()
        if animasyonlar.isEmpty {
            kareZamanlayici?.invalidate()
            kareZamanlayici = nil
        }
        if !bitenler.isEmpty {
            tahta.degeriDenetle()
        }
    }
}

final class AbakusTahtaGorunumu: UIView {
    // MARK: Ölçüler (fragment_abacus.xml + dimens.xml; dp → pt birebir)

    /// rod0..4: layout_width
    static let cubukGenisligi: CGFloat = 45
    /// tutorial_abacus_rod_height
    static let cubukYuksekligi: CGFloat = 250
    /// tutorial_abacus_bead_size
    static let boncukBoyu: CGFloat = 40
    /// tutorial_abacus_line_width
    static let cizgiGenisligi: CGFloat = 5
    /// rodN_barrier: layout_height
    static let bariyerYuksekligi: CGFloat = 16
    /// tutorial_abacus_barrier_margin_bottom (layout_gravity=center ile birlikte)
    static let bariyerAltMarj: CGFloat = 50
    /// abacusContainer: paddingStart / paddingEnd
    static let yatayDolgu: CGFloat = 24
    /// abacusContainer: paddingTop / paddingBottom
    static let dikeyDolgu: CGFloat = 20
    /// Çubuk sayısı ortak denetleyicide sabittir.
    static let cubukSayisi: Int = 5

    /// Bariyerin çubuk içindeki üst kenarı: FrameLayout'ta gravity=center + marginBottom,
    /// görünümü marjın yarısı kadar değil TAMAMI kadar yukarı iter: (250 − 16) / 2 − 50 = 67.
    static var bariyerUstu: CGFloat {
        return (cubukYuksekligi - bariyerYuksekligi) / 2 - bariyerAltMarj
    }

    static func boyut(basamakSayisi: Int) -> CGSize {
        let adet = CGFloat(min(max(basamakSayisi, 1), cubukSayisi))
        return CGSize(
            width: adet * cubukGenisligi + 2 * yatayDolgu,
            height: cubukYuksekligi + 2 * dikeyDolgu
        )
    }

    // MARK: Durum

    private let yogunluk: CGFloat
    private let evSahibi = AbakusBoncukEvSahibi()
    private var denetleyici: AbacusBeadController!
    private let kopru = AbakusBoncukKoprusu.shared

    private(set) var basamakSayisi: Int = 5
    private(set) var stil: AbakusStili = .varsayilan
    private(set) var kilitli: Bool = false

    /// Değer her değiştiğinde çağrılır.
    var degerDegisti: ((Int) -> Void)?
    private var sonBildirilenDeger: Int = 0

    var ses: AbakusBoncukSesi? {
        get { return evSahibi.ses }
        set { evSahibi.ses = newValue }
    }

    // Çerçeve katmanları (abacus_frame_bg.xml).
    private let disSinir = UIView()
    private let ahsap = UIView()
    private let icSinir = UIView()
    private let zemin = UIView()
    private let cerceveResmi = UIImageView()

    private var cizgiler: [UIView] = []
    private var bariyerler: [UIView] = []
    private var bariyerSeritleri: [UIView] = []
    /// Anahtar: yuva kodu (çubuk × 10 + 0 üst / 1..4 alt).
    private var boncukGorunumleri: [Int: UIImageView] = [:]
    private var boncuklar: [Int: AbacusBead] = [:]
    private var seciliDurumu: [Int: Bool] = [:]

    private var varsayilanResim: UIImage?
    private var varsayilanSeciliResim: UIImage?

    private var aktifDokunma: UITouch?
    private var aktifBoncuk: AbacusBead?

    // MARK: Kurulum

    /// `animasyonSuresiMs`: Android'de sandık dersinde 50, diğerlerinde 300.
    init(animasyonSuresiMs: Int) {
        self.yogunluk = max(UIScreen.main.scale, 1)
        super.init(frame: CGRect(origin: .zero, size: AbakusTahtaGorunumu.boyut(basamakSayisi: 5)))
        isMultipleTouchEnabled = false
        isExclusiveTouch = true
        backgroundColor = .clear
        evSahibi.tahta = self
        gorunumleriKur()
        denetleyici = kopru.denetleyiciOlustur(
            host: evSahibi, yogunluk: Float(yogunluk), animasyonSuresiMs: Int64(animasyonSuresiMs)
        )
        boncuklariEsle()
        resimleriHazirla()
        statikYerlesim()
        mesafeleriOlc()
        denetleyici.refreshAll()
        modeldenYerlestir()
    }

    required init?(coder: NSCoder) {
        fatalError("AbakusTahtaGorunumu yalnizca kodla kurulur")
    }

    deinit {
        evSahibi.durdur()
    }

    override var intrinsicContentSize: CGSize {
        return AbakusTahtaGorunumu.boyut(basamakSayisi: basamakSayisi)
    }

    private func gorunumleriKur() {
        // Çerçeve: dıştan içe dört yuvarlak köşeli dikdörtgen.
        disSinir.backgroundColor = .black
        disSinir.layer.cornerRadius = 24
        icSinir.backgroundColor = .black
        icSinir.layer.cornerRadius = 8
        ahsap.layer.cornerRadius = 20
        zemin.layer.cornerRadius = 4
        cerceveResmi.contentMode = .scaleToFill
        cerceveResmi.isHidden = true
        for g in [disSinir, ahsap, icSinir, zemin, cerceveResmi] {
            g.isUserInteractionEnabled = false
            addSubview(g)
        }
        // Çubuklar. Android'deki çizim sırası korunur: çizgi, alt4, alt3, alt2, alt1, bariyer, üst.
        for cubuk in 0..<AbakusTahtaGorunumu.cubukSayisi {
            let cizgi = UIView()
            cizgi.isUserInteractionEnabled = false
            addSubview(cizgi)
            cizgiler.append(cizgi)
            for sira in stride(from: 3, through: 0, by: -1) {
                boncukGorunumuEkle(kod: cubuk * 10 + sira + 1)
            }
            let bariyer = UIView()
            bariyer.backgroundColor = .black
            bariyer.isUserInteractionEnabled = false
            let serit = UIView()
            serit.isUserInteractionEnabled = false
            bariyer.addSubview(serit)
            addSubview(bariyer)
            bariyerler.append(bariyer)
            bariyerSeritleri.append(serit)
            boncukGorunumuEkle(kod: cubuk * 10)
        }
    }

    private func boncukGorunumuEkle(kod: Int) {
        let g = UIImageView()
        // Android: ImageView varsayılanı fitCenter.
        g.contentMode = .scaleAspectFit
        g.isUserInteractionEnabled = false
        addSubview(g)
        boncukGorunumleri[kod] = g
    }

    private func boncuklariEsle() {
        for cubuk in 0..<AbakusTahtaGorunumu.cubukSayisi {
            boncuklar[cubuk * 10] = kopru.ustBoncuk(denetleyici: denetleyici, rod: Int32(cubuk))
            for sira in 0..<4 {
                boncuklar[cubuk * 10 + sira + 1] = kopru.altBoncuk(
                    denetleyici: denetleyici, rod: Int32(cubuk), beadIndex: Int32(sira)
                )
            }
        }
    }

    /// Android: ensureAbacusMetricsIfVisible. Ölçüler bu görünümün kendi yerleşiminden gelir.
    private func mesafeleriOlc() {
        let boncukPx: CGFloat = AbakusTahtaGorunumu.boncukBoyu * yogunluk
        // 1. alt boncuğun üst kenarı: 250 − 82 − 40 = 128 dp.
        let alt1Ustu: CGFloat = AbakusTahtaGorunumu.cubukYuksekligi - 82 - AbakusTahtaGorunumu.boncukBoyu
        let alt1Px: CGFloat = alt1Ustu * yogunluk
        let bariyerPx: CGFloat = AbakusTahtaGorunumu.bariyerUstu * yogunluk
        let bariyerYukseklikPx: CGFloat = AbakusTahtaGorunumu.bariyerYuksekligi * yogunluk
        _ = kopru.mesafeleriOlc(
            denetleyici: denetleyici,
            topBeadY: 0,
            topBeadHeight: Int32(boncukPx.rounded()),
            bottom1Y: Int32(alt1Px.rounded()),
            barrierY: Int32(bariyerPx.rounded()),
            barrierHeight: Int32(bariyerYukseklikPx.rounded())
        )
    }

    // MARK: Dışarıdan ayarlar

    func basamakSayisiniAyarla(_ yeni: Int) {
        let sinirli = min(max(yeni, 1), AbakusTahtaGorunumu.cubukSayisi)
        if sinirli == basamakSayisi { return }
        basamakSayisi = sinirli
        invalidateIntrinsicContentSize()
        statikYerlesim()
        modeldenYerlestir()
    }

    func stiliAyarla(_ yeni: AbakusStili) {
        if yeni == stil { return }
        stil = yeni
        resimleriHazirla()
        statikYerlesim()
        for (kod, secili) in seciliDurumu {
            boncukGorunumunuGuncelle(kod: kod, secili: secili)
        }
    }

    /// Android: abacusController.setEnabled(false) — dokunmalar yutulur, boncuk oynamaz.
    func kilitle(_ yeni: Bool) {
        if yeni == kilitli { return }
        kilitli = yeni
        denetleyici.setEnabled(enabled: !yeni)
    }

    var deger: Int {
        return Int(denetleyici.getCurrentValue())
    }

    var hareketEdiyor: Bool {
        return denetleyici.isBeadAnimationInProgress()
    }

    /// Android: abacusController.reset(animate).
    func sifirla(animasyonlu: Bool) {
        dokunmayiBirak()
        denetleyici.reset(animate: animasyonlu)
        modeldenYerlestir()
        degeriDenetle()
    }

    /// Boncukları animasyonsuz olarak verilen değere yerleştirir (0...99999).
    func degeriAyarla(_ yeni: Int) {
        dokunmayiBirak()
        kopru.degeriYerlestir(denetleyici: denetleyici, deger: Int32(min(max(yeni, 0), 99999)))
        modeldenYerlestir()
        degeriDenetle()
    }

    // MARK: Çizim

    private func resimleriHazirla() {
        let genislik: CGFloat = AbakusTahtaGorunumu.boncukBoyu
        varsayilanResim = AbakusBoncukCizici.resim(
            renkler: stil.boncukRenkleri, secili: false, genislik: genislik
        )
        varsayilanSeciliResim = AbakusBoncukCizici.resim(
            renkler: stil.seciliBoncukRenkleri, secili: true, genislik: genislik
        )
    }

    private var ilkGorunenCubuk: Int {
        return AbakusTahtaGorunumu.cubukSayisi - basamakSayisi
    }

    private func cubukSolu(_ cubuk: Int) -> CGFloat {
        return AbakusTahtaGorunumu.yatayDolgu
            + CGFloat(cubuk - ilkGorunenCubuk) * AbakusTahtaGorunumu.cubukGenisligi
    }

    /// Çerçeve, çizgiler ve bariyerler: boncuk hareketinden etkilenmeyen parçalar.
    private func statikYerlesim() {
        let boyut = AbakusTahtaGorunumu.boyut(basamakSayisi: basamakSayisi)
        let tam = CGRect(origin: .zero, size: boyut)
        disSinir.frame = tam
        ahsap.frame = tam.insetBy(dx: 4, dy: 4)
        icSinir.frame = tam.insetBy(dx: 20, dy: 20)
        zemin.frame = tam.insetBy(dx: 24, dy: 24)
        cerceveResmi.frame = tam
        ahsap.backgroundColor = stil.cerceveRengi
        zemin.backgroundColor = stil.zeminRengi
        let resimli: Bool = stil.cerceveResmi != nil
        cerceveResmi.image = stil.cerceveResmi
        cerceveResmi.isHidden = !resimli
        for g in [disSinir, ahsap, icSinir, zemin] {
            g.isHidden = resimli
        }

        let ust: CGFloat = AbakusTahtaGorunumu.dikeyDolgu
        for cubuk in 0..<AbakusTahtaGorunumu.cubukSayisi {
            let gorunur: Bool = cubuk >= ilkGorunenCubuk
            let sol: CGFloat = cubukSolu(cubuk)
            let cizgi = cizgiler[cubuk]
            let cizgiSolu: CGFloat = sol
                + (AbakusTahtaGorunumu.cubukGenisligi - AbakusTahtaGorunumu.cizgiGenisligi) / 2
            cizgi.isHidden = !gorunur
            cizgi.backgroundColor = stil.cubukRengi
            cizgi.frame = CGRect(
                x: cizgiSolu, y: ust,
                width: AbakusTahtaGorunumu.cizgiGenisligi, height: AbakusTahtaGorunumu.cubukYuksekligi
            )
            let bariyer = bariyerler[cubuk]
            bariyer.isHidden = !gorunur
            bariyer.frame = CGRect(
                x: sol, y: ust + AbakusTahtaGorunumu.bariyerUstu,
                width: AbakusTahtaGorunumu.cubukGenisligi, height: AbakusTahtaGorunumu.bariyerYuksekligi
            )
            // abacus_horizontal_bar.xml: siyah zemin, üstten ve alttan 4 dp içeride ahşap şerit.
            let serit = bariyerSeritleri[cubuk]
            serit.backgroundColor = stil.bariyerRengi
            serit.frame = CGRect(
                x: 0, y: 4,
                width: AbakusTahtaGorunumu.cubukGenisligi, height: AbakusTahtaGorunumu.bariyerYuksekligi - 8
            )
            for sira in 0...4 {
                boncukGorunumleri[cubuk * 10 + sira]?.isHidden = !gorunur
            }
        }
    }

    /// Boncukları ortak modeldeki konuma (bottomMargin + translationY) çizer.
    func modeldenYerlestir() {
        let boy: CGFloat = AbakusTahtaGorunumu.boncukBoyu
        let ust: CGFloat = AbakusTahtaGorunumu.dikeyDolgu
        let yanBosluk: CGFloat = (AbakusTahtaGorunumu.cubukGenisligi - boy) / 2
        for (kod, boncuk) in boncuklar {
            guard let g = boncukGorunumleri[kod] else { continue }
            let cubuk: Int = kod / 10
            let x: CGFloat = cubukSolu(cubuk) + yanBosluk
            let kayma: CGFloat = CGFloat(boncuk.translationY) / yogunluk
            var y: CGFloat = ust + kayma
            if kod % 10 != 0 {
                // layout_gravity=bottom: üst kenar = çubuk yüksekliği − alt marj − boncuk boyu.
                let altMarj: CGFloat = CGFloat(boncuk.bottomMargin) / yogunluk
                y += AbakusTahtaGorunumu.cubukYuksekligi - altMarj - boy
            }
            g.frame = CGRect(x: x, y: y, width: boy, height: boy)
        }
    }

    /// Ortak denetleyicinin "seçili / değil" kararı (AbacusBeadHost.updateBeadAppearance).
    func boncukGorunumunuGuncelle(kod: Int, secili: Bool) {
        seciliDurumu[kod] = secili
        guard let g = boncukGorunumleri[kod] else { return }
        let yuva = AbakusBoncukYuvasi(cubuk: kod / 10, ust: kod % 10 == 0, sira: max(kod % 10 - 1, 0))
        if let ozel = stil.yuvaResimleri[yuva] {
            g.image = secili ? ozel.secili : ozel.normal
        } else if let genel = stil.boncukResmi {
            g.image = secili ? genel.secili : genel.normal
        } else {
            g.image = secili ? varsayilanSeciliResim : varsayilanResim
        }
    }

    func degeriDenetle() {
        let yeni = deger
        if yeni != sonBildirilenDeger {
            sonBildirilenDeger = yeni
            degerDegisti?(yeni)
        }
    }

    // MARK: Dokunma
    //
    // Android'de her boncuğun kendi OnTouchListener'ı vardır ve olaylar ham gelir (DOWN / MOVE /
    // UP / CANCEL). Jest tanıyıcılar (pan, tap) kendi eşiklerini araya koyduğu için burada
    // dokunmalar doğrudan alınır; eşik (touch slop) ve dokunma/sürükleme ayrımı denetleyicidedir.

    /// Android'deki z sırası: üst boncuk en üstte, sonra alt1, alt2, alt3, alt4.
    private func boncukBul(_ nokta: CGPoint) -> AbacusBead? {
        for cubuk in ilkGorunenCubuk..<AbakusTahtaGorunumu.cubukSayisi {
            for sira in 0...4 {
                let kod = cubuk * 10 + sira
                guard let g = boncukGorunumleri[kod], let boncuk = boncuklar[kod] else { continue }
                if g.frame.contains(nokta) {
                    return boncuk
                }
            }
        }
        return nil
    }

    private func gonder(eylem: Int32, dokunma: UITouch) {
        guard let boncuk = aktifBoncuk else { return }
        // Android rawY (ekran pikseli) kullanır; burada görünümün kendi koordinatı piksele çevrilir.
        let y: CGFloat = dokunma.location(in: self).y * yogunluk
        let zamanMs: Double = dokunma.timestamp * 1000.0
        _ = kopru.dokunma(
            denetleyici: denetleyici, boncuk: boncuk, eylemKodu: eylem,
            rawY: Float(y), eventTimeMs: Int64(zamanMs)
        )
        modeldenYerlestir()
        degeriDenetle()
    }

    private func dokunmayiBirak() {
        aktifDokunma = nil
        aktifBoncuk = nil
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard aktifDokunma == nil, let dokunma = touches.first else { return }
        guard let boncuk = boncukBul(dokunma.location(in: self)) else { return }
        aktifDokunma = dokunma
        aktifBoncuk = boncuk
        gonder(eylem: 0, dokunma: dokunma)
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let dokunma = aktifDokunma, touches.contains(dokunma) else { return }
        gonder(eylem: 2, dokunma: dokunma)
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let dokunma = aktifDokunma, touches.contains(dokunma) else { return }
        gonder(eylem: 1, dokunma: dokunma)
        dokunmayiBirak()
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let dokunma = aktifDokunma, touches.contains(dokunma) else { return }
        gonder(eylem: 3, dokunma: dokunma)
        dokunmayiBirak()
    }
}

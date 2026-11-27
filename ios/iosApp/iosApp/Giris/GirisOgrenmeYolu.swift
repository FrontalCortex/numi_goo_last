import Shared
import SwiftUI

// Kayıttaki ilerleme yolu (günlük hedef sorusundan sonra). Android: LearningPathView +
// LearningPathPlan. Soldan sağa yükselen bir abaküs teli; adımlar telin üstündeki boncuklar (içinde
// ikon), yazılar boncukların altında; telin ucunda altın ok başı; altta zaman çizgisi ve etiketleri.
// Renk soldan sağa mavi → yeşil → altın.
//
// Animasyon (playIntro): tel ve zaman çizgisi 900 ms'de çizilir, ok başı 800 ms'de belirir; sonra
// her adım için sırayla boncuk → yazı → zaman etiketi balon gibi (0 → %120 → %100) belirir.
// Ölçüler ve zamanlama LearningPathView sabitleriyle aynı (dp → pt). Yazı yüksekliği Android'de
// StaticLayout'la ölçülür; burada en çok iki satır varsayılır (TEXT_ALANI).

/// Yoldaki bir adım (LearningPathView.Milestone).
struct GirisYolAdimi {
    let simge: String
    let metin: String
    let zaman: String
}

/// Basit sRGB renk: Android ColorUtils.blendARGB ile karıştırmak için.
struct GirisRGB {
    let k: Double
    let y: Double
    let m: Double

    init(_ rgb: UInt32) {
        k = Double((rgb >> 16) & 0xFF) / 255.0
        y = Double((rgb >> 8) & 0xFF) / 255.0
        m = Double(rgb & 0xFF) / 255.0
    }

    init(k: Double, y: Double, m: Double) {
        self.k = k
        self.y = y
        self.m = m
    }

    static func karistir(_ a: GirisRGB, _ b: GirisRGB, _ t: Double) -> GirisRGB {
        let o: Double = min(max(t, 0), 1)
        return GirisRGB(k: a.k + (b.k - a.k) * o, y: a.y + (b.y - a.y) * o, m: a.m + (b.m - a.m) * o)
    }

    var renk: Color {
        return Color(.sRGB, red: k, green: y, blue: m, opacity: 1)
    }
}

/// LearningPathView'in yerleşimi (computeLayout) — genişliğe göre.
struct GirisYolYerlesimi {
    static let adimSayisi: Int = 3
    static let boncukR: CGFloat = 26
    static let simgeBoyu: CGFloat = 32
    static let basamak: CGFloat = 56
    static let yaziArasi: CGFloat = 12
    static let baslangicDususu: CGFloat = 22
    static let kuyrukYukselisi: CGFloat = 28
    static let okBoyu: CGFloat = 16
    static let okYariEn: CGFloat = 11
    static let okGeri: CGFloat = 4
    static let okCentik: CGFloat = 1
    static let okAcisiDerece: Double = 40
    static let tasma: CGFloat = 1.2
    static let kenar: CGFloat = 4
    /// Yazının varsayılan yüksekliği: 15sp kalın, iki satır + 2dp satır arası.
    static let yaziAlani: CGFloat = 44

    /// Mavi (başlangıç) → yeşil → altın (hedef).
    static let telRenkleri: [GirisRGB] = [GirisRGB(0x58A6FF), GirisRGB(0x8CC63F), GirisRGB(0xFFD54F)]
    static let izRengi: GirisRGB = GirisRGB(0x3D4A50)

    let genislik: CGFloat
    let xs: [CGFloat]
    let ys: [CGFloat]
    let yaziUstleri: [CGFloat]
    let yaziGenisligi: CGFloat
    let telSonu: CGPoint
    let zamanCizgisiY: CGFloat
    let zamanBasi: CGFloat
    let zamanSonu: CGFloat
    let etiketTabani: CGFloat
    let yukseklik: CGFloat
    let tel: Path

    init(genislik w: CGFloat) {
        genislik = w
        let kenar: CGFloat = GirisYolYerlesimi.kenar
        let r: CGFloat = GirisYolYerlesimi.boncukR
        let aci: Double = GirisYolYerlesimi.okAcisiDerece * Double.pi / 180
        let cosA: CGFloat = CGFloat(cos(aci))
        let sinA: CGFloat = CGFloat(sin(aci))
        yaziGenisligi = min(w * 0.29, 132)
        let x0: CGFloat = w * 0.16
        let x1: CGFloat = w * 0.49
        let x2: CGFloat = w * 0.78
        xs = [x0, x1, x2]
        // Ok başı tabanından büyür; %120'ye taştığında ucu kesilmesin diye sağda ve üstte pay.
        let okKenari: CGFloat = 1.5
        let okUzunlugu: CGFloat = GirisYolYerlesimi.okBoyu * GirisYolYerlesimi.tasma
        let sonX: CGFloat = w - kenar - okUzunlugu * cosA - okKenari
        let sonY: CGFloat = kenar + okUzunlugu * sinA + okKenari
        telSonu = CGPoint(x: sonX, y: sonY)
        let y2: CGFloat = sonY + GirisYolYerlesimi.kuyrukYukselisi
        let y1: CGFloat = y2 + GirisYolYerlesimi.basamak
        let y0: CGFloat = y1 + GirisYolYerlesimi.basamak
        ys = [y0, y1, y2]
        let araliK: CGFloat = r + GirisYolYerlesimi.yaziArasi
        yaziUstleri = [y0 + araliK, y1 + araliK, y2 + araliK]
        let yaziAlti: CGFloat = y0 + araliK + GirisYolYerlesimi.yaziAlani
        zamanCizgisiY = yaziAlti + 20
        zamanBasi = kenar + 3
        zamanSonu = w - kenar - 3
        etiketTabani = zamanCizgisiY + 24
        yukseklik = (etiketTabani + 4 + kenar + 4).rounded()

        var yol = Path()
        let sx: CGFloat = kenar + 4
        let sy: CGFloat = y0 + GirisYolYerlesimi.baslangicDususu
        yol.move(to: CGPoint(x: sx, y: sy))
        GirisYolYerlesimi.basamakEgrisi(&yol, CGPoint(x: sx, y: sy), CGPoint(x: x0, y: y0))
        GirisYolYerlesimi.basamakEgrisi(&yol, CGPoint(x: x0, y: y0), CGPoint(x: x1, y: y1))
        GirisYolYerlesimi.basamakEgrisi(&yol, CGPoint(x: x1, y: y1), CGPoint(x: x2, y: y2))
        // Son parça boncuktan yatay çıkıp ok başının eğimiyle ucuna varır.
        let dx: CGFloat = sonX - x2
        let uzunluk: CGFloat = dx * 0.55
        yol.addCurve(
            to: CGPoint(x: sonX, y: sonY),
            control1: CGPoint(x: x2 + dx * 0.5, y: y2),
            control2: CGPoint(x: sonX - uzunluk * cosA, y: sonY + uzunluk * sinA)
        )
        tel = yol
    }

    /// Basamak eğrisi: iki uçta yatay, arada yumuşakça yükselir.
    private static func basamakEgrisi(_ yol: inout Path, _ a: CGPoint, _ b: CGPoint) {
        let yari: CGFloat = (b.x - a.x) / 2
        yol.addCurve(to: b, control1: CGPoint(x: a.x + yari, y: a.y), control2: CGPoint(x: b.x - yari, y: b.y))
    }

    /// Telin gradyanındaki renk; `f` soldan sağa 0..1 (LearningPathView.colorAt).
    static func renk(_ f: Double) -> GirisRGB {
        let c: Double = min(max(f, 0), 1)
        let renkler: [GirisRGB] = GirisYolYerlesimi.telRenkleri
        if c < 0.5 {
            return GirisRGB.karistir(renkler[0], renkler[1], c / 0.5)
        }
        return GirisRGB.karistir(renkler[1], renkler[2], (c - 0.5) / 0.5)
    }

    func adimRengi(_ i: Int) -> GirisRGB {
        let f: Double = Double(xs[i] / max(genislik, 1))
        return GirisYolYerlesimi.renk(f)
    }
}

/// Animasyonun zaman ölçeği (LearningPathView companion sabitleri, ms).
enum GirisYolZamani {
    static let telMs: Double = 900
    static let okAni: Double = 800
    static let adimBasi: Double = 1000
    static let adimAraligi: Double = 560
    static let ogeAraligi: Double = 150
    static let belirmeMs: Double = 360
    static let tepe: Double = 0.55
    static let tasma: Double = 1.2
    static var toplamMs: Double {
        return adimBasi + 2 * adimAraligi + 2 * ogeAraligi + belirmeMs
    }

    static func ilerleme(_ t: Double, _ baslangic: Double) -> Double {
        return min(max((t - baslangic) / belirmeMs, 0), 1)
    }

    static func yumusak(_ x: Double) -> Double {
        return x * x * (3 - 2 * x)
    }

    /// Balon gibi belirme: 0 → %120 (hızlı) → %100 (yerine oturma).
    static func patla(_ p: Double) -> Double {
        if p <= 0 { return 0 }
        if p >= 1 { return 1 }
        if p < tepe {
            let x: Double = p / tepe
            return tasma * (1 - (1 - x) * (1 - x))
        }
        return tasma - (tasma - 1) * yumusak((p - tepe) / (1 - tepe))
    }

    static func saydamlik(_ p: Double) -> Double {
        return min(max(p / 0.3, 0), 1)
    }
}

/// İlerleme yolu görünümü. `oynatma` değişince çizim baştan oynar; sona varınca `bitti` çağrılır
/// (yarıda kesilirse çağrılmaz: yeniden oynatmada yalnızca yenisinin bitişi sayılır).
struct GirisOgrenmeYolu: View {
    let adimlar: [GirisYolAdimi]
    let oynatma: Int
    /// Başlamadan önceki bekleme (kap kayarak gelirken çizilen ilk kareler görünmesin).
    let gecikmeMs: Double
    let bitti: () -> Void

    @State private var baslangic: Date = Date()

    var body: some View {
        GeometryReader { alan in
            let yer = GirisYolYerlesimi(genislik: alan.size.width)
            TimelineView(.animation) { zaman in
                let t: Double = max(0, zaman.date.timeIntervalSince(baslangic) * 1000)
                GirisOgrenmeYoluKaresi(yer: yer, adimlar: adimlar, t: t)
            }
        }
        .frame(height: GirisYolYerlesimi(genislik: 360).yukseklik)
        .task(id: oynatma) {
            baslangic = Date().addingTimeInterval(gecikmeMs / 1000)
            let toplam: Double = gecikmeMs + GirisYolZamani.toplamMs
            try? await Task.sleep(nanoseconds: UInt64(toplam * 1_000_000))
            if Task.isCancelled { return }
            bitti()
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(adimlar.map { $0.metin + ": " + $0.zaman }.joined(separator: ". "))
    }
}

/// Tek kare: tel, zaman çizgisi ve ok Canvas'ta; boncuk, yazı ve etiketler görünüm olarak.
struct GirisOgrenmeYoluKaresi: View {
    let yer: GirisYolYerlesimi
    let adimlar: [GirisYolAdimi]
    let t: Double

    var body: some View {
        ZStack(alignment: .topLeading) {
            cizim
            ForEach(0..<min(adimlar.count, GirisYolYerlesimi.adimSayisi), id: \.self) { i in
                adim(i)
            }
        }
        .frame(width: yer.genislik, height: yer.yukseklik, alignment: .topLeading)
    }

    private var cizimIlerlemesi: Double {
        return GirisYolZamani.yumusak(min(max(t / GirisYolZamani.telMs, 0), 1))
    }

    private var cizim: some View {
        let p: Double = cizimIlerlemesi
        let okP: Double = GirisYolZamani.ilerleme(t, GirisYolZamani.okAni)
        let yerlesim: GirisYolYerlesimi = yer
        return Canvas { ctx, _ in
            GirisOgrenmeYoluKaresi.telCiz(ctx, yerlesim, p)
            GirisOgrenmeYoluKaresi.okCiz(ctx, yerlesim, okP)
        }
        .frame(width: yer.genislik, height: yer.yukseklik)
    }

    private static func telCiz(_ ctx: GraphicsContext, _ yer: GirisYolYerlesimi, _ p: Double) {
        if p <= 0 { return }
        let parca: Path = yer.tel.trimmedPath(from: 0, to: CGFloat(p))
        let renkler: [Color] = GirisYolYerlesimi.telRenkleri.map { $0.renk }
        let gradyan = Gradient(colors: renkler)
        let boya = GraphicsContext.Shading.linearGradient(
            gradyan,
            startPoint: CGPoint(x: 0, y: 0),
            endPoint: CGPoint(x: yer.genislik, y: 0)
        )
        ctx.stroke(parca, with: boya, style: StrokeStyle(lineWidth: 8, lineCap: .round))
        // Telin üstündeki ince parlama: yuvarlak bir tel gibi görünsün.
        var parlama = ctx
        parlama.translateBy(x: 0, y: -1.8)
        parlama.stroke(parca, with: .color(Color.white.opacity(Double(0x55) / 255.0)), style: StrokeStyle(lineWidth: 2, lineCap: .round))
        // Zaman çizgisi telle birlikte soldan sağa.
        let son: CGFloat = yer.zamanBasi + (yer.zamanSonu - yer.zamanBasi) * CGFloat(p)
        var cizgi = Path()
        cizgi.move(to: CGPoint(x: yer.zamanBasi, y: yer.zamanCizgisiY))
        cizgi.addLine(to: CGPoint(x: son, y: yer.zamanCizgisiY))
        ctx.stroke(cizgi, with: .color(Color("missions_track")), style: StrokeStyle(lineWidth: 3, lineCap: .round))
    }

    /// Ok başı telin ucunda, telin son eğimiyle; tabanından büyüyerek belirir.
    private static func okCiz(_ ctx: GraphicsContext, _ yer: GirisYolYerlesimi, _ p: Double) {
        if p <= 0 { return }
        let s: CGFloat = CGFloat(GirisYolZamani.patla(p))
        var yol = Path()
        yol.move(to: CGPoint(x: GirisYolYerlesimi.okBoyu, y: 0))
        yol.addLine(to: CGPoint(x: -GirisYolYerlesimi.okGeri, y: -GirisYolYerlesimi.okYariEn))
        yol.addLine(to: CGPoint(x: GirisYolYerlesimi.okCentik, y: 0))
        yol.addLine(to: CGPoint(x: -GirisYolYerlesimi.okGeri, y: GirisYolYerlesimi.okYariEn))
        yol.closeSubpath()
        var ok = ctx
        ok.opacity = GirisYolZamani.saydamlik(p)
        ok.translateBy(x: yer.telSonu.x, y: yer.telSonu.y)
        ok.rotate(by: Angle.degrees(-GirisYolYerlesimi.okAcisiDerece))
        ok.scaleBy(x: s, y: s)
        let altin: Color = GirisYolYerlesimi.telRenkleri[2].renk
        ok.fill(yol, with: .color(altin))
        ok.stroke(yol, with: .color(altin), style: StrokeStyle(lineWidth: 3, lineJoin: .round))
    }

    @ViewBuilder
    private func adim(_ i: Int) -> some View {
        let taban: Double = GirisYolZamani.adimBasi + Double(i) * GirisYolZamani.adimAraligi
        let boncukP: Double = GirisYolZamani.ilerleme(t, taban)
        let yaziP: Double = GirisYolZamani.ilerleme(t, taban + GirisYolZamani.ogeAraligi)
        let etiketP: Double = GirisYolZamani.ilerleme(t, taban + 2 * GirisYolZamani.ogeAraligi)
        let renk: GirisRGB = yer.adimRengi(i)
        let cizgiSonu: CGFloat = yer.zamanBasi + (yer.zamanSonu - yer.zamanBasi) * CGFloat(cizimIlerlemesi)
        if cizimIlerlemesi > 0 && cizgiSonu >= yer.xs[i] {
            nokta(i, renk: renk, p: etiketP)
        }
        boncuk(i, renk: renk, p: boncukP)
        yazi(i, p: yaziP)
        etiket(i, renk: renk, p: etiketP)
    }

    /// Nokta çizgi ona varınca gri belirir, etiketi gelince adımın rengine döner.
    private func nokta(_ i: Int, renk: GirisRGB, p: Double) -> some View {
        let karisim: GirisRGB = GirisRGB.karistir(GirisYolYerlesimi.izRengi, renk, p)
        let yaricap: CGFloat = 5 * max(1, CGFloat(GirisYolZamani.patla(p)))
        return Circle()
            .fill(karisim.renk)
            .frame(width: yaricap * 2, height: yaricap * 2)
            .position(x: yer.xs[i], y: yer.zamanCizgisiY)
    }

    /// Boncuk: açık iç (#E6EEF2; ikonların koyu çizgileri okunsun), adımın renginde 3dp halka, ikon.
    private func boncuk(_ i: Int, renk: GirisRGB, p: Double) -> some View {
        let r: CGFloat = GirisYolYerlesimi.boncukR
        let s: CGFloat = CGFloat(GirisYolZamani.patla(p))
        return ZStack {
            Circle().fill(GirisStil.renk(0xE6EEF2))
            Circle().strokeBorder(renk.renk, lineWidth: 3)
            Image(adimlar[i].simge)
                .resizable()
                .scaledToFit()
                .frame(width: GirisYolYerlesimi.simgeBoyu, height: GirisYolYerlesimi.simgeBoyu)
        }
        .frame(width: r * 2, height: r * 2)
        .scaleEffect(s)
        .opacity(GirisYolZamani.saydamlik(p))
        .position(x: yer.xs[i], y: yer.ys[i])
    }

    /// Yazı boncuğun hemen altından, üst ortasından büyüyerek çıkar.
    private func yazi(_ i: Int, p: Double) -> some View {
        let s: CGFloat = CGFloat(GirisYolZamani.patla(p))
        let alan: CGFloat = GirisYolYerlesimi.yaziAlani + 16
        return Text(adimlar[i].metin)
            .font(Tema.yazi(15))
            .foregroundColor(Color("dark_text"))
            .multilineTextAlignment(.center)
            .lineSpacing(2)
            .frame(width: yer.yaziGenisligi, height: alan, alignment: .top)
            .scaleEffect(s, anchor: .top)
            .opacity(GirisYolZamani.saydamlik(p))
            .position(x: yer.xs[i], y: yer.yaziUstleri[i] + alan / 2)
    }

    private func etiket(_ i: Int, renk: GirisRGB, p: Double) -> some View {
        let s: CGFloat = CGFloat(GirisYolZamani.patla(p))
        return Text(adimlar[i].zaman)
            .font(Tema.yazi(12))
            .tracking(12 * 0.08)
            .foregroundColor(renk.renk)
            .lineLimit(1)
            .fixedSize()
            .scaleEffect(s)
            .opacity(GirisYolZamani.saydamlik(p))
            .position(x: yer.xs[i], y: yer.etiketTabani - 5)
    }
}

/// İlerleme yolunun adımları ve tarih etiketleri (LearningPathPlan.timeLabels): "BUGÜN",
/// ilk haftanın sonu ("10 EKİM") ve kafadan hesap eşiğinin ayı ("ARALIK 2026"). Gün sayısı ortak
/// koddan (SignupStreakRules.daysToMentalMath); tarih uygulamanın dilinde büyük harfle.
enum GirisYolPlani {
    static func adimlar(hedefDakika: Int, bugun: Date = Date()) -> [GirisYolAdimi] {
        let yerel = Locale(identifier: Metin.dil)
        let takvim = Calendar.current
        let hafta: Int = Int(SignupStreakRules.shared.STREAK_STEP_DAYS)
        let gun: Int = Int(SignupStreakRules.shared.daysToMentalMath(goalMinutes: Int32(hedefDakika)))
        let haftaSonu: Date = takvim.date(byAdding: .day, value: hafta, to: bugun) ?? bugun
        let hedef: Date = takvim.date(byAdding: .day, value: gun, to: bugun) ?? bugun
        let gunAy = DateFormatter()
        gunAy.locale = yerel
        gunAy.dateFormat = "d MMMM"
        let ayYil = DateFormatter()
        ayYil.locale = yerel
        // LLLL: ayın tek başına kullanılan adı.
        ayYil.dateFormat = "LLLL yyyy"
        let etiketler: [String] = [
            Metin.al("ui_bugun"),
            gunAy.string(from: haftaSonu).uppercased(with: yerel),
            ayYil.string(from: hedef).uppercased(with: yerel),
        ]
        return [
            GirisYolAdimi(simge: "abacus_svg_ic", metin: Metin.al("ui_abakusu_tani"), zaman: etiketler[0]),
            // Süre başlıkta zaten yazıyor; burada tekrar etmiyor.
            GirisYolAdimi(simge: "streak_flame_ic", metin: Metin.al("ui_buyuk_sayilar_ile_islem_yap"), zaman: etiketler[1]),
            GirisYolAdimi(simge: "brain", metin: Metin.al("ui_kafadan_hesap_ustasi_ol"), zaman: etiketler[2]),
        ]
    }
}

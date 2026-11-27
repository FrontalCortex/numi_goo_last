import SwiftUI

// Sobi maskotu (mavi tavşan), kodla çizilir. Android karşılığı: BunnyMascotView.
//
// Kullanım:
//   SobiMaskotGorunumu(emote: "TEACH_TALK")
//       .frame(width: 120, height: 120)
//
// - `emote`: Android `BunnyMascotView.Emote` sabitinin adı ("GREET", "TEACH_POINT" …; liste
//   `SobiMaskotHal`). Bilinmeyen ad → nötr bekleme duruşu (çökme yok).
// - `animasyonlu: true`: Android'deki gibi bekleme canlandırması (nefes, baş sallanması, göz
//   kırpma, bakış kayması, kulak oynatma) sürer; hâl görünüm açılınca (ya da `emote`
//   değişince) baştan oynar. Döngülü hâller dönmeye devam eder, bekleme bölümü olanlar
//   (Android `holdFromMs`) son bölümünde döner, tek seferlik hâller (GREET, CHEER, … ,
//   NOTEBOOK_WRITE) bitince beklemeye döner — Android `play(emote)` ile aynı.
// - `animasyonlu: false`: tamamen durağan; hâlin seçili tek karesi (`SobiMaskotHal.duraganAn`).
// - Boyut çağıranın `.frame(...)`'ı. Android görünümü kısa kenara kare bir alan sığdırıyor
//   (en-boy oranı 1:1); burada da çizim verilen alanın kısa kenarına kare olarak ortalanıyor.
// - Dokunma almaz (alttaki görünüme geçirir), erişilebilirlikte gizli.
struct SobiMaskotGorunumu: View {
    private let emote: String
    private let animasyonlu: Bool

    @StateObject private var motor: SobiMaskotMotoru = SobiMaskotMotoru()

    init(emote: String, animasyonlu: Bool = true) {
        self.emote = emote
        self.animasyonlu = animasyonlu
    }

    var body: some View {
        icerik
            .allowsHitTesting(false)
            .accessibilityHidden(true)
    }

    @ViewBuilder
    private var icerik: some View {
        if animasyonlu {
            TimelineView(.animation) { zaman in
                SobiMaskotTuvali(motor: motor, emote: emote, animasyonlu: true, tarih: zaman.date)
            }
        } else {
            SobiMaskotTuvali(motor: motor, emote: emote, animasyonlu: false, tarih: Date(timeIntervalSinceReferenceDate: 0))
        }
    }
}

/// Tek kareyi çizen tuval: motoru o anın zamanıyla ilerletip sahneyi ölçekleyerek çizer.
private struct SobiMaskotTuvali: View {
    let motor: SobiMaskotMotoru
    let emote: String
    let animasyonlu: Bool
    let tarih: Date

    var body: some View {
        Canvas { ctx, boyut in
            let ms: Double = tarih.timeIntervalSinceReferenceDate * 1000
            motor.hazirla(ad: emote, animasyonlu: animasyonlu, simdiMs: ms)
            motor.kare(ms)
            let kenar: CGFloat = min(boyut.width, boyut.height)
            if kenar <= 0 { return }
            let birim: CGFloat = SobiMaskotSabit.units
            // Android onDraw: kısa kenara UNITS'lik kare, ortada; tavşan yatayda ortalı,
            // üstte zıplarken kulakların sığacağı pay (TOP_Y).
            ctx.translateBy(x: (boyut.width - kenar) / 2, y: (boyut.height - kenar) / 2)
            ctx.scaleBy(x: kenar / birim, y: kenar / birim)
            ctx.translateBy(x: birim / 2 - SobiMaskotSabit.centerX, y: -SobiMaskotSabit.topY)
            let tuval = SobiMaskotTuval(ctx)
            motor.sahneyiCiz(tuval)
        }
    }
}

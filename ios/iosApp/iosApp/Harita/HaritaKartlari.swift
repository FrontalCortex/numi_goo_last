import Foundation
import SwiftUI

// Harita listesinin satır görünümleri.
// Android karşılıkları: item_lesson.xml (ders + sandık), item_header.xml (ünite başlığı),
// item_race.xml (yarış bayrağı), CircleProgressBar.kt (dilimli ilerleme halkası).

// MARK: - Satır seçici

/// Tek bir liste satırı; türe göre doğru görünümü seçer.
struct HaritaSatiri: View {
    let oge: HaritaOgesi
    /// Ders kartının zikzak yatay kayması (ortak kod hesaplar).
    let kayma: CGFloat
    let dokun: () -> Void

    var body: some View {
        icerik
            .frame(maxWidth: .infinity)
    }

    @ViewBuilder
    private var icerik: some View {
        switch oge.tur {
        case .baslik:
            HaritaBaslikSatiri(baslik: oge.baslik)
        case .yaris:
            HaritaYarisKarti(oge: oge, dokun: dokun)
        case .ders, .sandik:
            HaritaDersKarti(oge: oge, dokun: dokun)
                .offset(x: kayma)
        }
    }
}

// MARK: - Ünite başlığı (item_header.xml)

struct HaritaBaslikSatiri: View {
    let baslik: String

    var body: some View {
        HStack(spacing: 0) {
            cizgi
            // Android: headerText wrap_content, çizgiler layout_weight=1 → önce yazı kendi
            // genişliğini alır, kalan yer iki çizgiye bölünür. Öncelik verilmezse SwiftUI yeri
            // üçe böler ve başlığı "Sayıları…" diye kısaltır.
            Text(baslik)
                .font(HaritaStil.yazi(17))
                .foregroundColor(HaritaStil.baslikCizgiRengi)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 16)
                .layoutPriority(1)
            cizgi
        }
        .padding(.horizontal, HaritaStil.yapiskanYatayBosluk)
        .padding(.vertical, 14)
    }

    private var cizgi: some View {
        Rectangle()
            .fill(HaritaStil.baslikCizgiRengi)
            .frame(height: 2)
            .frame(maxWidth: .infinity)
    }
}

// MARK: - Ders ve sandık kartı (item_lesson.xml)

struct HaritaDersKarti: View {
    let oge: HaritaOgesi
    let dokun: () -> Void

    @State private var nefesAcik: Bool = false

    var body: some View {
        ZStack {
            halka
            Button(action: dokun) {
                HaritaKartGovdesi(oge: oge)
            }
            .buttonStyle(HaritaKartBasmaStili())
        }
        .frame(width: HaritaStil.halkaBoyu, height: HaritaStil.halkaBoyu)
        .padding(HaritaStil.kartSatirBoslugu)
        .onAppear {
            if oge.nefes {
                // İlk yerleşimle aynı işlemde başlarsa sonsuz animasyon kartın konumuna da bulaşabiliyor;
                // bu yüzden yerleşim oturduktan sonra başlatılır.
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                    nefesAcik = true
                }
            }
        }
    }

    /// İlerleme halkası. Altın bitiş görünümünde gizlidir (Android: alpha 0, ölçek 0.35).
    @ViewBuilder
    private var halka: some View {
        if !oge.altin {
            HaritaIlerlemeHalkasi(dilimSayisi: oge.dilimSayisi, doluDilim: oge.doluDilim)
                .frame(width: HaritaStil.halkaBoyu, height: HaritaStil.halkaBoyu)
                .scaleEffect(nefesAcik ? 1.08 : 1.0)
                .animation(nefesAnimasyonu, value: nefesAcik)
        }
    }

    /// Android: ValueAnimator.ofFloat(1f, 1.08f, 1f), 2200 ms, sonsuz, AccelerateDecelerate.
    private var nefesAnimasyonu: Animation {
        return Animation.easeInOut(duration: 1.1).repeatForever(autoreverses: true)
    }
}

/// Kartın yuvarlak gövdesi: zemin rengi, simge ya da yıldızlar, altın parlamaları.
struct HaritaKartGovdesi: View {
    let oge: HaritaOgesi

    var body: some View {
        ZStack {
            Circle().fill(zeminRengi)
            simge
            parlamalar
        }
        .frame(width: HaritaStil.kartBoyu, height: HaritaStil.kartBoyu)
        .clipShape(Circle())
        .contentShape(Circle())
        // Gölge kartın bütününe tek parça uygulansın (simgeye ayrıca değil).
        .compositingGroup()
        .shadow(color: Color.black.opacity(0.3), radius: 2, x: 0, y: 1)
    }

    private var zeminRengi: Color {
        return oge.altin ? Color("lesson_center_gold") : Color(oge.kartRengi)
    }

    @ViewBuilder
    private var simge: some View {
        if oge.tur == .sandik {
            HaritaSandikYildizlari(ucYildiz: oge.ucYildiz, yildizlar: oge.yildizlar)
        } else {
            Image("book_icon")
                .resizable()
                .scaledToFit()
                .frame(width: HaritaStil.simgeBoyu, height: HaritaStil.simgeBoyu)
        }
    }

    /// lessonGoldShinePrimary / lessonGoldShineSecondary (yalnızca altın görünümde).
    @ViewBuilder
    private var parlamalar: some View {
        if oge.altin {
            HaritaParlama(orta: 0.8)
                .frame(width: 42, height: 10)
                .rotationEffect(.degrees(-24))
                .opacity(0.75)
                .padding(.leading, 6)
                .padding(.top, 8)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            HaritaParlama(orta: 0.5)
                .frame(width: 24, height: 8)
                .rotationEffect(.degrees(-24))
                .opacity(0.55)
                .padding(.trailing, 8)
                .padding(.top, 17)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
        }
    }
}

/// lesson_card_shine_primary.xml / _secondary.xml: 45 derecelik saydam-beyaz-saydam geçiş.
struct HaritaParlama: View {
    /// Ortadaki beyazın opaklığı (#CC → 0.8, #80 → 0.5).
    let orta: Double

    var body: some View {
        Capsule().fill(
            LinearGradient(
                colors: [Color.white.opacity(0.0), Color.white.opacity(orta), Color.white.opacity(0.0)],
                startPoint: .bottomLeading,
                endPoint: .topTrailing
            )
        )
    }
}

// MARK: - Sandık yıldızları (item_lesson.xml: chestStarsRow)

struct HaritaSandikYildizlari: View {
    let ucYildiz: Bool
    let yildizlar: [HaritaYildizi]

    var body: some View {
        icerik
            .frame(width: HaritaStil.yildizAlani, height: HaritaStil.yildizAlani)
    }

    @ViewBuilder
    private var icerik: some View {
        if ucYildiz {
            // Üç yıldız alanı eşit böler (layout_weight=1, fitCenter).
            HStack(spacing: 0) {
                ForEach(Array(yildizlar.enumerated()), id: \.offset) { yuva in
                    HaritaYildizGorseli(yildiz: yuva.element)
                        .frame(width: HaritaStil.yildizAlani / 3.0, height: HaritaStil.yildizAlani)
                }
            }
        } else {
            // Ortada tek yıldız (map_lesson_icon_size).
            HaritaYildizGorseli(yildiz: yildizlar.first ?? HaritaYildizi.acikGri)
                .frame(width: HaritaStil.simgeBoyu, height: HaritaStil.simgeBoyu)
        }
    }
}

struct HaritaYildizGorseli: View {
    let yildiz: HaritaYildizi

    var body: some View {
        switch yildiz {
        case .sari:
            Image("star_on_ic")
                .resizable()
                .scaledToFit()
        case .acikGri:
            // Android: star_on_ic + chest_star_light_gray (SRC_IN boyama).
            Image("star_on_ic")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(Color("chest_star_light_gray"))
        case .bos:
            Image("star_off_ic")
                .resizable()
                .scaledToFit()
        }
    }
}

// MARK: - Yarış bayrağı (item_race.xml)

struct HaritaYarisKarti: View {
    let oge: HaritaOgesi
    let dokun: () -> Void

    var body: some View {
        Button(action: dokun) {
            ZStack {
                Circle().fill(Color(oge.kartRengi))
                bayrak
            }
            .frame(width: HaritaStil.kartBoyu, height: HaritaStil.kartBoyu)
            .clipShape(Circle())
            .contentShape(Circle())
            .shadow(color: Color.black.opacity(0.3), radius: 2, x: 0, y: 1)
        }
        .buttonStyle(HaritaKartBasmaStili())
        .padding(HaritaStil.kartSatirBoslugu)
    }

    @ViewBuilder
    private var bayrak: some View {
        if oge.kilitsiz {
            bayrakGorseli
        } else {
            // Android applyRaceFlagLockedTone: doygunluk 0, soğuk gri ölçek, alfa 230/255.
            bayrakGorseli
                .saturation(0)
                .colorMultiply(Color(.sRGB, red: 0.72, green: 0.76, blue: 0.80, opacity: 1.0))
                .opacity(230.0 / 255.0)
        }
    }

    private var bayrakGorseli: some View {
        Image("flag_ic")
            .resizable()
            .scaledToFit()
            .frame(width: HaritaStil.yarisSimgeBoyu, height: HaritaStil.yarisSimgeBoyu)
    }
}

// MARK: - Dilimli ilerleme halkası (CircleProgressBar.kt)

struct HaritaIlerlemeHalkasi: View {
    let dilimSayisi: Int
    let doluDilim: Int

    var body: some View {
        ZStack {
            HaritaHalkaDilimleri(dilimSayisi: dilimSayisi, ilk: doluDilim, son: dilimSayisi)
                .stroke(Color("lesson_ring_inactive_dark"), style: cizgi)
            HaritaHalkaDilimleri(dilimSayisi: dilimSayisi, ilk: 0, son: doluDilim)
                .stroke(Color("lesson_ring_active_blue"), style: cizgi)
        }
    }

    private var cizgi: StrokeStyle {
        return StrokeStyle(lineWidth: HaritaStil.halkaKalinligi, lineCap: .round)
    }
}

/// Halkanın [ilk, son) aralığındaki dilimlerini çizer.
/// Android drawSegmentedRing ile aynı geometri: başlangıç -90 derece, saat yönünde,
/// dilimler arası boşluk 16 derece, yay çizgi kalınlığının yarısı kadar içeride.
struct HaritaHalkaDilimleri: Shape {
    let dilimSayisi: Int
    let ilk: Int
    let son: Int

    func path(in rect: CGRect) -> Path {
        var yol = Path()
        let adet = max(dilimSayisi, 1)
        let bas = max(ilk, 0)
        let bitis = min(son, adet)
        if bitis <= bas {
            return yol
        }
        let dilimAcisi: Double = 360.0 / Double(adet)
        let bosluk: Double = min(max(HaritaStil.dilimBoslugu, 0.0), dilimAcisi - 1.0)
        let yayAcisi: Double = max(dilimAcisi - bosluk, 1.0)
        let kenar: CGFloat = min(rect.width, rect.height)
        let yaricap: CGFloat = (kenar - HaritaStil.halkaKalinligi) / 2.0
        let merkez = CGPoint(x: rect.midX, y: rect.midY)

        for sira in bas..<bitis {
            let baslangic: Double = -90.0 + Double(sira) * dilimAcisi + bosluk / 2.0
            let radyan: Double = baslangic * Double.pi / 180.0
            let ilkNokta = CGPoint(
                x: merkez.x + yaricap * CGFloat(cos(radyan)),
                y: merkez.y + yaricap * CGFloat(sin(radyan))
            )
            yol.move(to: ilkNokta)
            // SwiftUI'da y ekseni aşağı baktığı için clockwise: false ekranda saat yönünde çizer.
            yol.addArc(
                center: merkez,
                radius: yaricap,
                startAngle: Angle.degrees(baslangic),
                endAngle: Angle.degrees(baslangic + yayAcisi),
                clockwise: false
            )
        }
        return yol
    }
}

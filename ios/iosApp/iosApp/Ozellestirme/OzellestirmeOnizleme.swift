import SwiftUI
import UIKit

// Özelleştirme ekranının yalın, etkileşimsiz abaküs önizlemesi.
// Android karşılığı: res/layout/layout_customization_preview.xml (5 çubuk, her çubukta 1 üst +
// 4 alt boncuk, hepsi sıfır konumunda) ve yuva seçme panelindeki abaküs (panel_bead_skin_select.xml).
//
// Android'de önizleme gerçek AbacusBeadController ile çalışır (boncuklar oynatılabilir); burada
// boncuklar oynamaz. Ölçüler ortak koddan (AbacusPreviewMetrics), resimler OzellestirmeBoyaci'ndan gelir.
struct OzellestirmeOnizleme: View {
    let yuvalar: [OzellestirmeYuva]
    let olculer: OzellestirmeOlculer
    /// Çerçeve tarifi; `sabitCerceveGorseli` verilirse kullanılmaz.
    let cerceve: OzellestirmeCerceve
    /// Yuva panelinde çerçeve hep aynı görseldir (abacus_frame_bg4).
    var sabitCerceveGorseli: String? = nil
    /// Yuva panelinde bariyer daha kalındır (16 dp).
    var kalinBariyer: Bool = false
    /// Verilirse boncuklar dokunulabilir olur.
    var dokun: ((OzellestirmeYuva) -> Void)? = nil

    var body: some View {
        HStack(spacing: 0) {
            ForEach(0..<olculer.cubukSayisi, id: \.self) { cubuk in
                OzellestirmeCubuk(
                    yuvalar: yuvalar.filter { $0.cubuk == cubuk },
                    olculer: olculer,
                    bariyerYuksekligi: kalinBariyer ? olculer.yuvaPaneliBariyerYuksekligi : olculer.bariyerYuksekligi,
                    bariyerUstu: kalinBariyer ? olculer.yuvaPaneliBariyerUstu : olculer.bariyerUstu,
                    dokun: dokun
                )
            }
        }
        .padding(.vertical, olculer.cerceveDikeyBosluk)
        .padding(.horizontal, olculer.cerceveYatayBosluk)
        .background(zemin)
    }

    @ViewBuilder
    private var zemin: some View {
        if let gorsel = sabitCerceveGorseli {
            Image(gorsel)
                .renderingMode(.original)
                .resizable()
        } else {
            OzellestirmeCerceveGorunumu(cerceve: cerceve)
        }
    }
}

/// Tek çubuk: dikey çizgi, alttan üste dört alt boncuk, bariyer ve üst boncuk.
/// Çizim sırası Android'deki görünüm sırasıyla aynıdır (alt4, alt3, alt2, alt1, bariyer, üst).
private struct OzellestirmeCubuk: View {
    let yuvalar: [OzellestirmeYuva]
    let olculer: OzellestirmeOlculer
    let bariyerYuksekligi: CGFloat
    let bariyerUstu: CGFloat
    let dokun: ((OzellestirmeYuva) -> Void)?

    var body: some View {
        ZStack(alignment: .topLeading) {
            olculer.cizgiRengi
                .frame(width: olculer.cizgiGenisligi, height: olculer.cubukYuksekligi)
                .offset(x: (olculer.cubukGenisligi - olculer.cizgiGenisligi) / 2, y: 0)
            ForEach(altBoncuklar) { yuva in
                boncuk(yuva, ust: altBoncukUstu(yuva.sira))
            }
            bariyer
                .offset(x: 0, y: bariyerUstu)
            ForEach(ustBoncuklar) { yuva in
                boncuk(yuva, ust: olculer.ustBoncukUstu)
            }
        }
        .frame(width: olculer.cubukGenisligi, height: olculer.cubukYuksekligi, alignment: .topLeading)
    }

    /// En alttaki boncuk önce çizilir.
    private var altBoncuklar: [OzellestirmeYuva] {
        yuvalar.filter { !$0.ustMu }.sorted { $0.sira > $1.sira }
    }

    private var ustBoncuklar: [OzellestirmeYuva] {
        yuvalar.filter { $0.ustMu }
    }

    private func altBoncukUstu(_ sira: Int) -> CGFloat {
        if sira >= 0 && sira < olculer.altBoncukUstleri.count {
            return olculer.altBoncukUstleri[sira]
        }
        return 0
    }

    private func boncuk(_ yuva: OzellestirmeYuva, ust: CGFloat) -> some View {
        let yatay: CGFloat = (olculer.cubukGenisligi - olculer.boncukBoyu) / 2
        return OzellestirmeBoncukGorunumu(
            resim: yuva.resim,
            genislik: olculer.boncukBoyu,
            yukseklik: olculer.boncukBoyu
        )
        .contentShape(Rectangle())
        .onTapGesture {
            dokun?(yuva)
        }
        .allowsHitTesting(dokun != nil)
        .offset(x: yatay, y: ust)
    }

    /// abacus_horizontal_bar.xml: siyah zemin; üstten ve alttan içeride ahşap şerit
    /// (bariyer ince olduğunda şeride yer kalmaz, yalnızca siyah görünür).
    private var bariyer: some View {
        let icYukseklik: CGFloat = max(0, bariyerYuksekligi - 2 * olculer.bariyerIcBoslugu)
        return ZStack {
            olculer.bariyerDisRengi
            olculer.bariyerIcRengi
                .frame(height: icYukseklik)
        }
        .frame(width: olculer.cubukGenisligi, height: bariyerYuksekligi)
    }
}

/// Çerçeve zemini; bulunduğu alanı doldurur.
/// Android: AbacusFrameRenderer.buildFrameDrawable (görünümün arka planı olarak gerilir).
struct OzellestirmeCerceveGorunumu: View {
    let cerceve: OzellestirmeCerceve

    var body: some View {
        GeometryReader { geo in
            icerik(boyut: geo.size)
        }
    }

    @ViewBuilder
    private func icerik(boyut: CGSize) -> some View {
        switch cerceve.tur {
        case .resim:
            if let hazir = cerceve.resim {
                Image(uiImage: hazir)
                    .resizable()
                    .frame(width: boyut.width, height: boyut.height)
            } else {
                Color.clear
            }
        case .kenarKatmanlari:
            katmanlar(boyut: boyut, yatayOlcek: 1, dikeyOlcek: 1)
        case .tuvalKatmanlari:
            katmanlar(
                boyut: boyut,
                yatayOlcek: boyut.width / max(1, cerceve.tuvalKenari),
                dikeyOlcek: boyut.height / max(1, cerceve.tuvalKenari)
            )
        }
    }

    private func katmanlar(boyut: CGSize, yatayOlcek: CGFloat, dikeyOlcek: CGFloat) -> some View {
        ZStack(alignment: .topLeading) {
            ForEach(cerceve.katmanlar) { katman in
                OzellestirmeCerceveKatmanGorunumu(
                    katman: katman,
                    boyut: boyut,
                    yatayOlcek: yatayOlcek,
                    dikeyOlcek: dikeyOlcek,
                    gradyan: gradyan
                )
            }
        }
        .frame(width: boyut.width, height: boyut.height, alignment: .topLeading)
    }

    /// Gradyanlı çerçevenin dolgusu: sol alttan sağ üste, ortak koddaki duraklarla.
    private var gradyan: LinearGradient {
        let renkler: [Color] = cerceve.gradyanRenkleri
        let duraklar: [CGFloat] = cerceve.gradyanDuraklari
        if renkler.count == duraklar.count && renkler.count >= 2 {
            var durakListesi: [Gradient.Stop] = []
            for i in 0..<renkler.count {
                durakListesi.append(Gradient.Stop(color: renkler[i], location: duraklar[i]))
            }
            return LinearGradient(
                gradient: Gradient(stops: durakListesi),
                startPoint: .bottomLeading,
                endPoint: .topTrailing
            )
        }
        let yedek: [Color] = renkler.isEmpty ? [Color.clear] : renkler
        return LinearGradient(colors: yedek, startPoint: .bottomLeading, endPoint: .topTrailing)
    }
}

/// Çerçevenin tek katmanı: kenar boşlukları ve yarıçap ölçeklenerek yerleştirilir.
private struct OzellestirmeCerceveKatmanGorunumu: View {
    let katman: OzellestirmeCerceveKatmani
    let boyut: CGSize
    let yatayOlcek: CGFloat
    let dikeyOlcek: CGFloat
    let gradyan: LinearGradient

    var body: some View {
        let genislik: CGFloat = max(0, boyut.width - (katman.sol + katman.sag) * yatayOlcek)
        let yukseklik: CGFloat = max(0, boyut.height - (katman.ust + katman.alt) * dikeyOlcek)
        let kose = CGSize(
            width: min(katman.yaricap * yatayOlcek, genislik / 2),
            height: min(katman.yaricap * dikeyOlcek, yukseklik / 2)
        )
        return dolgu(kose: kose)
            .frame(width: genislik, height: yukseklik)
            .offset(x: katman.sol * yatayOlcek, y: katman.ust * dikeyOlcek)
    }

    @ViewBuilder
    private func dolgu(kose: CGSize) -> some View {
        if katman.gradyanMi {
            RoundedRectangle(cornerSize: kose, style: .circular)
                .fill(gradyan)
        } else {
            RoundedRectangle(cornerSize: kose, style: .circular)
                .fill(katman.renk)
        }
    }
}

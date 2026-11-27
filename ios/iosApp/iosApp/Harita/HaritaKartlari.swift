import Foundation
import SwiftUI

// Harita listesinin satır görünümleri.
// Android karşılıkları: item_lesson_card.xml (ders + sandık kartı, LessonAdapter.LessonViewHolder),
// item_header.xml (ünite başlığı), item_race.xml (yarış bayrağı), SegmentBarView.kt (dilimli çubuk).

/// Ders/sandık kartlarının liste içindeki çerçeveleri (ders paneli karta bağlanırken okunur).
struct HaritaKartKonumAnahtari: PreferenceKey {
    static var defaultValue: [Int: Anchor<CGRect>] = [:]

    static func reduce(value: inout [Int: Anchor<CGRect>], nextValue: () -> [Int: Anchor<CGRect>]) {
        value.merge(nextValue(), uniquingKeysWith: { _, yeni in yeni })
    }
}

// MARK: - Satır seçici

/// Tek bir liste satırı; türe göre doğru görünümü seçer.
struct HaritaSatiri: View {
    let oge: HaritaOgesi
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
            // genişliğini alır, kalan yer iki çizgiye bölünür.
            Text(baslik)
                .font(HaritaStil.yazi(17))
                .foregroundColor(HaritaStil.baslikRengi)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 16)
                .layoutPriority(1)
            cizgi
        }
        .padding(.horizontal, HaritaStil.baslikYatayBosluk)
        .padding(.vertical, HaritaStil.baslikDikeyBosluk)
    }

    private var cizgi: some View {
        Rectangle()
            .fill(HaritaStil.baslikRengi)
            .frame(height: 2)
            .frame(maxWidth: .infinity)
    }
}

// MARK: - Ders ve sandık kartı (item_lesson_card.xml)

/// Listedeki dokunulabilir kart: basma küçülmesi, "sıradaki ders" nefesi ve bir kezlik altın geçişi.
struct HaritaDersKarti: View {
    let oge: HaritaOgesi
    let dokun: () -> Void

    @State private var nefesAcik: Bool = false
    @State private var altinGosterildi: Bool = false

    /// Altın geçişi bekleyen kart önce açık (mavi) görünür.
    private var gosterilen: HaritaKartDurumu {
        if oge.altinGecisi && !altinGosterildi {
            return .acik
        }
        return oge.durum
    }

    var body: some View {
        Button(action: dokun) {
            HaritaDersKartGorunumu(oge: oge, durum: gosterilen)
        }
        .buttonStyle(HaritaKartBasmaStili())
        .scaleEffect(nefesAcik ? 1.02 : 1.0)
        .animation(nefesAcik ? nefesAnimasyonu : Animation.default, value: nefesAcik)
        .anchorPreference(key: HaritaKartKonumAnahtari.self, value: .bounds) { cerceve in
            [oge.id: cerceve]
        }
        .padding(.horizontal, HaritaStil.kartYatayBosluk)
        .padding(.vertical, HaritaStil.kartDikeyBosluk)
        .onAppear { basla() }
        .onChange(of: oge.nefes) { yeni in
            nefesAcik = false
            if yeni {
                nefesiBaslat()
            }
        }
    }

    private func basla() {
        if oge.nefes {
            nefesiBaslat()
        }
        if oge.altinGecisi && !altinGosterildi {
            // Android: son dilim dolunca mavi kart 520 ms'de altına döner, ok onaya dönüşür.
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                withAnimation(Animation.easeInOut(duration: 0.52)) {
                    altinGosterildi = true
                }
            }
        }
    }

    /// İlk yerleşimle aynı işlemde başlarsa sonsuz animasyon kartın konumuna da bulaşabiliyor;
    /// bu yüzden yerleşim oturduktan sonra başlatılır.
    private func nefesiBaslat() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            if oge.nefes {
                nefesAcik = true
            }
        }
    }

    /// Android: ValueAnimator.ofFloat(1f, 1.02f, 1f), 2400 ms, sonsuz, AccelerateDecelerate.
    private var nefesAnimasyonu: Animation {
        return Animation.easeInOut(duration: 1.2).repeatForever(autoreverses: true)
    }
}

/// Kartın renkleri (LessonViewHolder.applyBaseLook / applyPersistentFinalGoldState).
struct HaritaKartRenkleri {
    let zemin: Color
    let cizgi: Color
    let yazi: Color
    let dolgu: Color
    let iz: Color
    let durumSimgesi: String
    let durumRengi: Color
    /// Kilitli kartta sol ikon soluk (doygunluk 0, alfa 0,6).
    let solukSimge: Bool

    static func al(_ durum: HaritaKartDurumu) -> HaritaKartRenkleri {
        switch durum {
        case .acik:
            return HaritaKartRenkleri(
                zemin: Color("lesson_center_blue"),
                cizgi: Color.white,
                yazi: Color.white,
                dolgu: Color.white,
                iz: Color("lesson_card_open_track"),
                durumSimgesi: "ic_lesson_card_chevron",
                durumRengi: Color.white,
                solukSimge: false
            )
        case .kilitli:
            return HaritaKartRenkleri(
                zemin: Color("lesson_locked"),
                cizgi: Color.clear,
                yazi: Color("lesson_card_locked_text"),
                dolgu: Color("lesson_card_locked_fill"),
                iz: Color("lesson_card_locked_track"),
                durumSimgesi: "lock_ic",
                durumRengi: Color("lesson_card_locked_text"),
                solukSimge: true
            )
        case .bitti:
            return HaritaKartRenkleri(
                zemin: Color("lesson_center_gold"),
                cizgi: Color.clear,
                yazi: Color("lesson_card_done_text"),
                dolgu: Color("lesson_card_done_fill"),
                iz: Color("lesson_card_done_track"),
                durumSimgesi: "ic_lesson_card_check",
                durumRengi: Color("lesson_card_done_text"),
                solukSimge: false
            )
        }
    }
}

/// Kartın kendisi (dokunma yok): ders panelinin karartma üstündeki kart görüntüsü de bunu çizer.
/// Solda ikon, ortada başlık ve altında adım dilimleri ("2/5") ya da sandık yıldızları,
/// sağda durum ikonu (ok / onay / kilit).
struct HaritaDersKartGorunumu: View {
    let oge: HaritaOgesi
    let durum: HaritaKartDurumu

    var body: some View {
        let r: HaritaKartRenkleri = HaritaKartRenkleri.al(durum)
        return HStack(alignment: .center, spacing: 0) {
            solSimge(r)
            orta(r)
                .padding(.horizontal, 12)
            durumSimgesi(r)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: HaritaStil.kartKoseYaricapi)
                .fill(r.zemin)
        )
        .overlay(
            RoundedRectangle(cornerRadius: HaritaStil.kartKoseYaricapi)
                .strokeBorder(r.cizgi, lineWidth: HaritaStil.kartCizgi)
        )
        .contentShape(RoundedRectangle(cornerRadius: HaritaStil.kartKoseYaricapi))
    }

    @ViewBuilder
    private func solSimge(_ r: HaritaKartRenkleri) -> some View {
        let boy: CGFloat = HaritaStil.kartSimgeBoyu
        if r.solukSimge {
            Image(oge.simge)
                .resizable()
                .scaledToFit()
                .frame(width: boy, height: boy)
                .saturation(0)
                .opacity(0.6)
        } else {
            Image(oge.simge)
                .resizable()
                .scaledToFit()
                .frame(width: boy, height: boy)
        }
    }

    private func orta(_ r: HaritaKartRenkleri) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(oge.baslik)
                .font(HaritaStil.yazi(15))
                .foregroundColor(r.yazi)
                .lineLimit(2)
                .truncationMode(.tail)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
            alt(r)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func alt(_ r: HaritaKartRenkleri) -> some View {
        if oge.tur == .sandik {
            if let yildiz = yildizlar {
                HaritaSandikYildizSatiri(yildiz: yildiz)
                    .padding(.top, 6)
            }
        } else {
            adimSatiri(r)
                .padding(.top, 8)
        }
    }

    /// Altına dönmüş kartta yıldızlar koyu kahve; geçişten önce eski hâl.
    private var yildizlar: HaritaYildizDurumu? {
        return durum == .bitti ? oge.yildizSonra : oge.yildizOnce
    }

    private func adimSatiri(_ r: HaritaKartRenkleri) -> some View {
        HStack(alignment: .center, spacing: 0) {
            HaritaDilimCubugu(
                dilimSayisi: oge.dilimSayisi,
                doluDilim: oge.doluDilim,
                dolgu: r.dolgu,
                iz: r.iz
            )
            .frame(height: HaritaStil.kartCubukYuksekligi)
            .frame(maxWidth: .infinity)
            Text("\(oge.doluDilim)/\(oge.dilimSayisi)")
                .font(HaritaStil.yazi(12))
                .foregroundColor(r.yazi)
                .lineLimit(1)
                .fixedSize()
                .frame(minWidth: 26, alignment: .trailing)
                .padding(.leading, 8)
        }
    }

    private func durumSimgesi(_ r: HaritaKartRenkleri) -> some View {
        let boy: CGFloat = HaritaStil.kartDurumSimgesi
        return Image(r.durumSimgesi)
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .foregroundColor(r.durumRengi)
            .frame(width: boy, height: boy)
            .id(r.durumSimgesi)
            .transition(AnyTransition.scale.combined(with: AnyTransition.opacity))
    }
}

// MARK: - Dilimli çubuk (SegmentBarView.kt)

/// Her adım bir dilim, aralarında 4pt boşluk; dolu dilimler `dolgu`, boşlar `iz` renginde.
struct HaritaDilimCubugu: View {
    let dilimSayisi: Int
    let doluDilim: Int
    let dolgu: Color
    let iz: Color

    var body: some View {
        ZStack {
            HaritaDilimSekli(adet: dilimSayisi, ilk: doluDilim, son: dilimSayisi)
                .fill(iz)
            HaritaDilimSekli(adet: dilimSayisi, ilk: 0, son: doluDilim)
                .fill(dolgu)
        }
    }
}

/// Çubuğun [ilk, son) aralığındaki dilimleri (yuvarlak uçlu dikdörtgenler).
struct HaritaDilimSekli: Shape {
    let adet: Int
    let ilk: Int
    let son: Int

    func path(in rect: CGRect) -> Path {
        var yol = Path()
        let n: Int = max(adet, 1)
        let bas: Int = max(ilk, 0)
        let bitis: Int = min(son, n)
        if bitis <= bas || rect.width <= 0 || rect.height <= 0 {
            return yol
        }
        let toplamBosluk: CGFloat = HaritaStil.dilimBoslugu * CGFloat(n - 1)
        let genislik: CGFloat = max((rect.width - toplamBosluk) / CGFloat(n), 0)
        let yaricap: CGFloat = min(rect.height / 2.0, genislik / 2.0)
        for i in bas..<bitis {
            let x: CGFloat = rect.minX + CGFloat(i) * (genislik + HaritaStil.dilimBoslugu)
            let dilim = CGRect(x: x, y: rect.minY, width: genislik, height: rect.height)
            yol.addRoundedRect(in: dilim, cornerSize: CGSize(width: yaricap, height: yaricap))
        }
        return yol
    }
}

// MARK: - Sandık yıldızları (item_lesson_card.xml: chestStarsRow)

/// Hep üç yuva (22pt, aralarında 4pt). Bitmiş kartta yıldızlar koyu kahveye boyanır;
/// kazanılmamış yıldızın opaklığı ortak koddan gelir (bitmiş 0,3; açık 1; kilitli 0,55).
struct HaritaSandikYildizSatiri: View {
    let yildiz: HaritaYildizDurumu

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<3, id: \.self) { sira in
                yildizGorseli(kazanildi: sira < yildiz.kazanilan)
            }
        }
    }

    @ViewBuilder
    private func yildizGorseli(kazanildi: Bool) -> some View {
        let ad: String = kazanildi ? "star_on_ic" : "star_off_ic"
        let opaklik: Double = kazanildi ? 1.0 : yildiz.bosOpaklik
        let boy: CGFloat = HaritaStil.kartDurumSimgesi
        if yildiz.bitti {
            Image(ad)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(Color("lesson_card_done_fill"))
                .frame(width: boy, height: boy)
                .opacity(opaklik)
        } else {
            Image(ad)
                .resizable()
                .scaledToFit()
                .frame(width: boy, height: boy)
                .opacity(opaklik)
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
                Circle().fill(Color(oge.yarisRengi))
                bayrak
            }
            .frame(width: HaritaStil.yarisKartBoyu, height: HaritaStil.yarisKartBoyu)
            .clipShape(Circle())
            .contentShape(Circle())
            .shadow(color: Color.black.opacity(0.3), radius: 2, x: 0, y: 1)
        }
        .buttonStyle(HaritaKartBasmaStili(olcek: 0.80))
        .padding(HaritaStil.yarisSatirBoslugu)
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

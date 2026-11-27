import SwiftUI

// Rehber paneli görünümleri.
//
// Android karşılıkları:
//   res/layout/view_guide_panel.xml + GuidePanelView.kt (harita, kör ders) ve fragment_abacus.xml içindeki
//   panelContent (abaküs dersi) — aynı parçalar: #222 köşe 18 panel, üstte adım noktaları (● / ○, 16 sp,
//   8 dp aralık, üstten 8), solda 140 dp Sobi (başlangıç boşluğu 30), yanında 15 sp ortalı beyaz yazı
//   (başlangıç boşluğu 16), alt köşelerde geri / ileri daireleri (40 dp kutu, 10 dp dolgu: görünen daire
//   30 dp, panelin köşesinden 10 içeride).
// Sobi hâlleri için `SobiMaskotGorunumu(emote:)` kullanılır (emote adı Android'deki hâl adıdır).

// MARK: - Panel

/// Rehberin görünen kutusu (yerleşimi `RehberKatmani` verir).
struct RehberPaneli: View {
    let adim: RehberAdimi
    let indeks: Int
    let adimSayisi: Int
    let duzen: RehberDuzeni
    let ileriGorunur: Bool
    let geri: () -> Void
    let ileri: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            icerik
            dugmeler
        }
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(RehberRenk.panel)
        )
    }

    // MARK: İçerik

    private var icerik: some View {
        VStack(spacing: 0) {
            noktalar
            maskotVeMetin
        }
        .padding(.top, 10)
        .padding(.horizontal, 10)
        .padding(.bottom, duzen.altDolgu)
    }

    /// stepDotsContainer: geçerli sayfa dolu daire.
    @ViewBuilder
    private var noktalar: some View {
        if !(duzen.tekAdimdaNoktaGizli && adimSayisi <= 1) {
            HStack(spacing: 8) {
                ForEach(0..<adimSayisi, id: \.self) { (sira: Int) in
                    Text(sira == indeks ? "●" : "○")
                        .font(Tema.yazi(16))
                        .foregroundColor(.white)
                }
            }
            // abacus_fragment_step_dots_margin_top
            .padding(.top, 8)
        }
    }

    /// guideMascot (140 dp, başlangıç 30) ve tvGuideText (başlangıç 16, 15 sp, ortalı).
    private var maskotVeMetin: some View {
        HStack(spacing: 0) {
            SobiMaskotGorunumu(emote: adim.emote)
                // guide_mascot_size
                .frame(width: 140, height: 140)
                // abacus_fragment_guide_image_margin_start
                .padding(.leading, 30)
            Text(adim.metin)
                .font(Tema.yazi(15))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity)
                .padding(.leading, 16)
                .padding(.trailing, duzen.metinSonBosluk)
        }
        .padding(.top, duzen.maskotUstBosluk)
    }

    // MARK: Geri / ileri

    private var dugmeler: some View {
        HStack(spacing: 0) {
            geriDugmesi
            Spacer(minLength: 0)
            ileriDugmesi
        }
    }

    /// btnBack: back_circle_button; 40 dp kutuda 10 dp başlangıç ve alt dolgusu.
    private var geriDugmesi: some View {
        Button(action: geri) {
            Image("back_circle_button")
                .resizable()
                .scaledToFit()
                .frame(width: 30, height: 30)
                .frame(width: 40, height: 40, alignment: .topTrailing)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(Metin.al("view_guide_panel_btn_back_desc")))
    }

    /// btnForward: aynı görsel yatay çevrilmiş (rotationY 180); görünmezken yer tutar ama dokunulmaz
    /// (Android: hedefli son sayfada GONE).
    private var ileriDugmesi: some View {
        Button(action: ileri) {
            Image("back_circle_button")
                .resizable()
                .scaledToFit()
                .frame(width: 30, height: 30)
                .scaleEffect(x: -1, y: 1)
                .frame(width: 40, height: 40, alignment: .topLeading)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .opacity(ileriGorunur ? 1 : 0)
        .allowsHitTesting(ileriGorunur)
        .accessibilityLabel(Text(Metin.al("view_guide_panel_btn_forward_desc")))
    }
}

// MARK: - Vurgu

/// İşaret edilen öğenin üstünde atan çerçeve (Android: animateBubbleEffect — 1,0 → en büyük → 1,0 ölçek,
/// 600 ms, sonsuz; renk geçişi). iOS'ta asıl öğe değil, üstündeki bir çerçeve atar.
struct RehberVurgu: View {
    let cerceve: CGRect
    let renk: Color?
    let acikRenk: Color?
    let enBuyuk: CGFloat

    @State private var buyuk: Bool = false

    var body: some View {
        let ana: Color = renk ?? Color.white
        let acik: Color = acikRenk ?? ana
        let cizgi: Color = buyuk ? acik : ana
        let kose: CGFloat = 12
        return RoundedRectangle(cornerRadius: kose, style: .continuous)
            .fill(acik.opacity(0.2))
            .overlay(
                RoundedRectangle(cornerRadius: kose, style: .continuous)
                    .strokeBorder(cizgi, lineWidth: 3)
            )
            .frame(width: cerceve.width, height: cerceve.height)
            .scaleEffect(buyuk ? enBuyuk : 1.0)
            .position(x: cerceve.midX, y: cerceve.midY)
            .allowsHitTesting(false)
            .onAppear {
                withAnimation(Animation.easeInOut(duration: 0.3).repeatForever(autoreverses: true)) {
                    buyuk = true
                }
            }
    }
}

// MARK: - Katman

/// Ekranın üstüne biner: dokunma engeli, vurgu, zorunlu dokunma alanı ve kayan panel.
///
/// Kullanım (ders ekranının en dışındaki görünümde):
///
///     .overlayPreferenceValue(RehberHedefAnahtari.self) { hedefler in
///         RehberKatmani(akis: rehber, hedefler: hedefler, duzen: .abakusDersi, yer: .abakusUstu(aralik: 0))
///     }
struct RehberKatmani: View {
    @ObservedObject var akis: RehberAkisi
    let hedefler: [RehberHedefi: Anchor<CGRect>]
    let duzen: RehberDuzeni
    let yer: RehberYeri

    var body: some View {
        GeometryReader { (geo: GeometryProxy) in
            katman(geo: geo)
        }
        .allowsHitTesting(akis.engel)
    }

    @ViewBuilder
    private func katman(geo: GeometryProxy) -> some View {
        if akis.aktif {
            let cerceve: [RehberHedefi: CGRect] = cerceveler(geo: geo)
            ZStack(alignment: .topLeading) {
                engelKatmani(geo: geo, cerceve: cerceve)
                panelKatmani(geo: geo, cerceve: cerceve)
            }
            .frame(width: geo.size.width, height: geo.size.height, alignment: .topLeading)
        }
    }

    /// Bildirilen çerçeveler bu katmanın koordinatında. Boş (görünmeyen, yer tutmayan) öğeler kayıtlı sayılmaz:
    /// böylece gizli bir düğmeyi "dokunmak zorunlu" sayıp rehberi kilitlemeyiz.
    private func cerceveler(geo: GeometryProxy) -> [RehberHedefi: CGRect] {
        var sonuc: [RehberHedefi: CGRect] = [:]
        for (hedef, capa) in hedefler {
            let dikdortgen: CGRect = geo[capa]
            if dikdortgen.width >= 1 && dikdortgen.height >= 1 {
                sonuc[hedef] = dikdortgen
            }
        }
        return sonuc
    }

    // MARK: Engel, vurgu, zorunlu dokunma

    /// Android: overlay (alfa 0,01, dokunmayı yutar).
    @ViewBuilder
    private func engelKatmani(geo: GeometryProxy, cerceve: [RehberHedefi: CGRect]) -> some View {
        if akis.engel {
            ZStack(alignment: .topLeading) {
                if !kuralSecimiAciliyor(cerceve: cerceve) {
                    Color.black.opacity(0.001)
                        .ignoresSafeArea()
                        .contentShape(Rectangle())
                        .onTapGesture {}
                }
                vurguKatmani(cerceve: cerceve)
            }
        }
    }

    /// "Kural seç" sayfasında kural kitabı rehberin altında kalmaz: dokunuşlar kitaba geçer
    /// (Android: waitForRulesTableSelection → rulesFragmentContainer öne alınır).
    private func kuralSecimiAciliyor(cerceve: [RehberHedefi: CGRect]) -> Bool {
        guard let adim = akis.gecerliAdim else {
            return false
        }
        return adim.kuralSecimiBekler && cerceve[.kuralKitabiDugmesi] != nil
    }

    @ViewBuilder
    private func vurguKatmani(cerceve: [RehberHedefi: CGRect]) -> some View {
        if let adim = akis.gecerliAdim, let hedef = adim.hedef, let alan = cerceve[hedef] {
            RehberVurgu(
                cerceve: alan, renk: adim.vurguRengi, acikRenk: adim.vurguAcikRengi,
                enBuyuk: adim.vurguOlcegi
            )
            .id(akis.indeks)
            if adim.tiklamaGerekli {
                tiklamaAlani(cerceve: alan, kayitli: Set(cerceve.keys))
            }
        }
    }

    /// requiredClickTarget: hedefin üstünde, engelin üstünde duran dokunma alanı.
    private func tiklamaAlani(cerceve: CGRect, kayitli: Set<RehberHedefi>) -> some View {
        return Color.clear
            .frame(width: cerceve.width, height: cerceve.height)
            .contentShape(Rectangle())
            .onTapGesture {
                akis.hedefeTiklandi(kayitli: kayitli)
            }
            .position(x: cerceve.midX, y: cerceve.midY)
    }

    // MARK: Panel

    @ViewBuilder
    private func panelKatmani(geo: GeometryProxy, cerceve: [RehberHedefi: CGRect]) -> some View {
        if let adim = akis.gecerliAdim {
            let genislik: CGFloat = max(geo.size.width - 2 * duzen.yatayBosluk, 0)
            dikeyYerlesim(
                boyut: geo.size, cerceve: cerceve,
                panel: panelGorunumu(adim: adim, genislik: genislik, kayitli: Set(cerceve.keys))
            )
            .offset(x: kaymaMesafesi(genislik: geo.size.width))
        }
    }

    private func panelGorunumu(adim: RehberAdimi, genislik: CGFloat, kayitli: Set<RehberHedefi>) -> some View {
        let gorunur: Bool = akis.ileriGorunur(kayitli: kayitli)
        return RehberPaneli(
            adim: adim,
            indeks: akis.indeks,
            adimSayisi: akis.adimlar.count,
            duzen: duzen,
            ileriGorunur: gorunur,
            geri: { akis.geri() },
            ileri: { akis.ileri(kayitli: kayitli) }
        )
        .frame(width: genislik)
        .fixedSize(horizontal: false, vertical: true)
    }

    /// Giriş tarafında bekler, 0'da durur, sola çıkar.
    private func kaymaMesafesi(genislik: CGFloat) -> CGFloat {
        switch akis.konum {
        case .bekliyor:
            return duzen.girisSagdan ? genislik : -genislik
        case .icerde:
            return 0
        case .cikti:
            return -genislik
        }
    }

    // MARK: Dikey yerleşim

    /// Paneli dikeyde yerine koyar. `ust` doluysa panelin üst kenarı o noktadadır; değilse alt kenarı
    /// `alt` kadar yukarıdadır.
    private func dikeyYerlesim<Panel: View>(
        boyut: CGSize, cerceve: [RehberHedefi: CGRect], panel: Panel
    ) -> some View {
        let plan: (ust: CGFloat?, alt: CGFloat) = yerlesimPlani(boyut: boyut, cerceve: cerceve)
        return VStack(spacing: 0) {
            if let ust = plan.ust {
                Color.clear.frame(height: ust)
                panel
                Spacer(minLength: 0)
            } else {
                Spacer(minLength: 0)
                panel
                Color.clear.frame(height: plan.alt)
            }
        }
        .frame(width: boyut.width, height: boyut.height)
    }

    /// Android yerleşim kuralları: abaküs dersinde panel abaküs alanının hemen üstünde; haritada hedefin altında
    /// (ekranın altından 12 pt içeride kalacak şekilde sınırlı).
    private func yerlesimPlani(boyut: CGSize, cerceve: [RehberHedefi: CGRect]) -> (ust: CGFloat?, alt: CGFloat) {
        let yukseklik: CGFloat = boyut.height
        switch yer {
        case .abakusUstu(let aralik):
            var altKenar: CGFloat = 0
            if let abakus = cerceve[.abakusAlani] {
                altKenar = abakus.minY - aralik
            } else {
                // Abaküs alanı bildirilmediyse: alt düğme alanı + abaküs yüksekliği kadar yukarıda.
                let abakusYuksekligi: CGFloat = AbakusGorunumu.boyut(basamakSayisi: 5).height
                altKenar = yukseklik - 10 - 63 - 4 - abakusYuksekligi - aralik
            }
            return (nil, max(yukseklik - altKenar, 0))
        case .hedefAltinda(let hedef, let aralik):
            let kenar: CGFloat = 12
            let tahminiYukseklik: CGFloat = 200
            if let alan = cerceve[hedef] {
                let ust: CGFloat = alan.maxY + aralik
                let enAsagi: CGFloat = yukseklik - kenar - tahminiYukseklik
                return (max(min(ust, enAsagi), 0), 0)
            }
            return (nil, kenar)
        }
    }
}

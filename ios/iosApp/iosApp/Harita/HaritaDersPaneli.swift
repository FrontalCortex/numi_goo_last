import Foundation
import SwiftUI

// Derse dokununca açılan, tıklanan karta küçük bir okla bağlı ders paneli.
// Android: lesson_popover.xml + LessonAdapter.showLessonBottomSheet / placeLessonPanel,
// PanelPointerView.kt (ok). Metinler, düğme durumu ve renk durumu ortak koddan
// (LessonAdapterLogic.lessonBottomSheetState) gelir; burada yalnızca yerleşim ve renkler var.

// MARK: - Katman: karartma + kart görüntüsü + ok + panel

/// Haritanın ders listesi alanını örten katman. `kart`: tıklanan kartın bu katmandaki çerçevesi
/// (kart görünmüyorsa nil → panel ortada, oksuz açılır).
struct HaritaPanelKatmani: View {
    let panel: HaritaPanelDurumu
    let kart: CGRect?
    let alan: CGSize
    @ObservedObject var model: HaritaModeli

    /// Android: panel 0,92 ölçekten 180 ms'de büyüyerek açılır (DecelerateInterpolator).
    @State private var acildi: Bool = false

    var body: some View {
        ZStack(alignment: .topLeading) {
            // Android: scrimView (alfa 0,7; dokununca panel kapanır).
            Color.black.opacity(HaritaStil.karartma)
                .contentShape(Rectangle())
                .onTapGesture { model.paneliKapat() }
            kartGoruntusu
            yerlesim
                .scaleEffect(acildi ? 1.0 : 0.92, anchor: pivot)
        }
        .frame(width: alan.width, height: alan.height)
        .clipped()
        .onAppear {
            withAnimation(Animation.easeOut(duration: 0.18)) {
                acildi = true
            }
        }
    }

    /// Layout örneği önce kurulur, sonra içerikle çağrılır (callAsFunction).
    private var yerlesim: some View {
        let duzen = HaritaPanelYerlesimi(kart: kart)
        let renk: HaritaKartDurumu = panel.renk
        return duzen {
            HaritaPanelOku(yukari: true, durum: renk)
            HaritaPanelOku(yukari: false, durum: renk)
            HaritaDersPaneli(durum: panel, model: model)
        }
    }

    /// Android: lessonPanelCardSnapshot — kart karartmanın üstünde aydınlık kalır.
    @ViewBuilder
    private var kartGoruntusu: some View {
        if let cerceve = kart {
            HaritaDersKartGorunumu(oge: panel.oge, durum: panel.oge.durum)
                .frame(width: cerceve.width, height: cerceve.height)
                .offset(x: cerceve.minX, y: cerceve.minY)
                .allowsHitTesting(false)
        }
    }

    /// Panel karttan büyüyerek açılır (Android: pivot okun ucunda).
    private var pivot: UnitPoint {
        guard let cerceve = kart, alan.width > 0, alan.height > 0 else {
            return UnitPoint.center
        }
        let x: CGFloat = cerceve.midX / alan.width
        let y: CGFloat = cerceve.maxY / alan.height
        return UnitPoint(x: min(max(x, 0), 1), y: min(max(y, 0), 1))
    }
}

/// placeLessonPanel'in yerleşimi. Alt görünümler sırasıyla: yukarıyı gösteren ok, aşağıyı
/// gösteren ok, panel. Kullanılmayan ok sıfır boyutla yerleştirilir (çizilmez).
struct HaritaPanelYerlesimi: Layout {
    let kart: CGRect?

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        return proposal.replacingUnspecifiedDimensions()
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        if subviews.count < 3 {
            return
        }
        let kenar: CGFloat = HaritaStil.panelKenarPayi
        let aralik: CGFloat = HaritaStil.okKartArasi
        let bindirme: CGFloat = HaritaStil.okBindirme
        let okY: CGFloat = HaritaStil.okYuksekligi
        let okG: CGFloat = HaritaStil.okGenisligi
        let yatay: CGFloat = HaritaStil.kartYatayBosluk
        let genislik: CGFloat = max(bounds.width - yatay * 2.0, 0)
        let olcu: CGSize = subviews[2].sizeThatFits(ProposedViewSize(width: genislik, height: nil))
        let panelH: CGFloat = olcu.height
        let alanH: CGFloat = bounds.height

        var yukari: Bool = true
        var panelY: CGFloat = (alanH - panelH) / 2.0
        var okMerkezX: CGFloat = bounds.width / 2.0
        var okVar: Bool = false
        if let k = kart {
            let altaSigar: Bool = k.maxY + aralik + okY + panelH + kenar <= alanH
            let usteSigar: Bool = k.minY - aralik - okY - panelH - kenar >= 0
            yukari = altaSigar || !usteSigar
            if yukari {
                panelY = k.maxY + aralik + okY - bindirme
            } else {
                panelY = k.minY - aralik - okY + bindirme - panelH
            }
            let ustSinir: CGFloat = max(alanH - panelH - kenar, kenar)
            panelY = min(max(panelY, kenar), ustSinir)
            okMerkezX = k.midX
            okVar = true
        }

        let panelNoktasi = CGPoint(x: bounds.minX + yatay, y: bounds.minY + panelY)
        subviews[2].place(
            at: panelNoktasi,
            anchor: .topLeading,
            proposal: ProposedViewSize(width: genislik, height: panelH)
        )

        let okYeriY: CGFloat = yukari ? panelY - okY + bindirme : panelY + panelH - bindirme
        let okNoktasi = CGPoint(x: bounds.minX + okMerkezX - okG / 2.0, y: bounds.minY + okYeriY)
        let okBoyu = ProposedViewSize(width: okG, height: okY)
        let sifir = ProposedViewSize(width: 0, height: 0)
        subviews[0].place(at: okNoktasi, anchor: .topLeading, proposal: (okVar && yukari) ? okBoyu : sifir)
        subviews[1].place(at: okNoktasi, anchor: .topLeading, proposal: (okVar && !yukari) ? okBoyu : sifir)
    }
}

/// PanelPointerView: içi panel zemininin rengi, iki eğik kenarı panelin çerçeve rengi; taban
/// çizilmez ki panelin çerçevesiyle birleşsin. Boyutu yerleşimden gelir (kullanılmayan ok 0×0).
struct HaritaPanelOku: View {
    let yukari: Bool
    let durum: HaritaKartDurumu

    var body: some View {
        ZStack {
            HaritaOkSekli(yukari: yukari, kenarli: false)
                .fill(Color("background_color"))
            HaritaOkSekli(yukari: yukari, kenarli: true)
                .stroke(
                    HaritaPanelRenkleri.al(durum).cerceve,
                    style: StrokeStyle(lineWidth: HaritaStil.panelCizgi, lineCap: .round, lineJoin: .round)
                )
        }
        .allowsHitTesting(false)
    }
}

/// PanelPointerView.onDraw: dolgu üçgeni; `kenarli` ise yalnızca iki eğik kenar.
struct HaritaOkSekli: Shape {
    let yukari: Bool
    let kenarli: Bool

    func path(in rect: CGRect) -> Path {
        var yol = Path()
        if rect.width <= 0 || rect.height <= 0 {
            return yol
        }
        let s: CGFloat = HaritaStil.panelCizgi / 2.0
        let uc: CGFloat = yukari ? rect.minY + s : rect.maxY - s
        let taban: CGFloat = yukari ? rect.maxY : rect.minY
        if kenarli {
            yol.move(to: CGPoint(x: rect.minX + s, y: taban))
            yol.addLine(to: CGPoint(x: rect.midX, y: uc))
            yol.addLine(to: CGPoint(x: rect.maxX - s, y: taban))
        } else {
            yol.move(to: CGPoint(x: rect.minX, y: taban))
            yol.addLine(to: CGPoint(x: rect.midX, y: uc))
            yol.addLine(to: CGPoint(x: rect.maxX, y: taban))
            yol.closeSubpath()
        }
        return yol
    }
}

// MARK: - Renkler (LessonAdapter.panelPalette)

struct HaritaPanelRenkleri {
    let cerceve: Color
    let vurgu: Color
    let dolgu: Color
    /// Düğme yüzünün renk kaynağı adı (dudak bundan koyulaştırılır).
    let dugmeZemini: String
    let dugmeYazisi: Color

    static func al(_ durum: HaritaKartDurumu) -> HaritaPanelRenkleri {
        switch durum {
        case .acik:
            return HaritaPanelRenkleri(
                cerceve: Color("lesson_center_blue"),
                vurgu: Color("lesson_panel_open_accent"),
                dolgu: Color("lesson_center_blue"),
                dugmeZemini: "lesson_center_blue",
                dugmeYazisi: Color.white
            )
        case .bitti:
            return HaritaPanelRenkleri(
                cerceve: Color("lesson_center_gold"),
                vurgu: Color("lesson_center_gold"),
                dolgu: Color("lesson_center_gold"),
                dugmeZemini: "lesson_center_gold",
                dugmeYazisi: Color("lesson_card_done_text")
            )
        case .kilitli:
            return HaritaPanelRenkleri(
                cerceve: Color("lesson_locked"),
                vurgu: Color("lesson_card_locked_fill"),
                dolgu: Color("lesson_card_locked_fill"),
                dugmeZemini: "lesson_panel_track",
                dugmeYazisi: Color("lesson_card_locked_fill")
            )
        }
    }
}

// MARK: - Panel (lessonPanelCard)

struct HaritaDersPaneli: View {
    let durum: HaritaPanelDurumu
    @ObservedObject var model: HaritaModeli

    private var renkler: HaritaPanelRenkleri {
        return HaritaPanelRenkleri.al(durum.renk)
    }

    private var kilitli: Bool {
        return durum.renk == .kilitli
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ustSatir
            baslik
            adimlar
            aciklama
            rekor
            eylemDugmesi
                .padding(.top, 16)
            egitimTekrar
        }
        .padding(HaritaStil.panelIcBosluk)
        .frame(maxWidth: .infinity, alignment: .leading)
        .fixedSize(horizontal: false, vertical: true)
        .background(zemin)
        // Panelin boş yerine dokunuş karartmaya geçmesin (Android: lessonPanelCard clickable).
        .contentShape(RoundedRectangle(cornerRadius: HaritaStil.panelKoseYaricapi))
        .onTapGesture {}
    }

    private var zemin: some View {
        RoundedRectangle(cornerRadius: HaritaStil.panelKoseYaricapi)
            .fill(Color("background_color"))
            .overlay(
                RoundedRectangle(cornerRadius: HaritaStil.panelKoseYaricapi)
                    .strokeBorder(renkler.cerceve, lineWidth: HaritaStil.panelCizgi)
            )
    }

    // MARK: Üst satır: tür etiketi + ikon

    private var ustSatir: some View {
        HStack(alignment: .center, spacing: 0) {
            Text(durum.turMetni)
                .font(HaritaStil.yazi(12))
                // letterSpacing 0.06 em (12sp → 0,72pt)
                .tracking(0.72)
                .foregroundColor(renkler.vurgu)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            panelSimgesi
        }
    }

    @ViewBuilder
    private var panelSimgesi: some View {
        if kilitli {
            Image(durum.simge)
                .resizable()
                .scaledToFit()
                .frame(width: 28, height: 28)
                .saturation(0)
                .opacity(0.6)
        } else {
            Image(durum.simge)
                .resizable()
                .scaledToFit()
                .frame(width: 28, height: 28)
        }
    }

    private var baslik: some View {
        Text(durum.baslik)
            .font(HaritaStil.yazi(20))
            .foregroundColor(kilitli ? Color("lesson_card_locked_text") : Color.white)
            .multilineTextAlignment(.leading)
            .fixedSize(horizontal: false, vertical: true)
            .padding(.top, 4)
    }

    // MARK: Adımlar (lessonPanelStepRow)

    @ViewBuilder
    private var adimlar: some View {
        if durum.adimGorunur {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 0) {
                    Text(Metin.al("lesson_popover_text"))
                        .font(HaritaStil.yazi(13))
                        .foregroundColor(Color("lesson_card_locked_text"))
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text("\(durum.doluAdim)/\(durum.adimSayisi)")
                        .font(HaritaStil.yazi(13))
                        .foregroundColor(Color.white)
                }
                HaritaDilimCubugu(
                    dilimSayisi: durum.adimSayisi,
                    doluDilim: durum.doluAdim,
                    dolgu: renkler.dolgu,
                    iz: Color("lesson_panel_track")
                )
                .frame(height: 9)
                .padding(.top, 6)
            }
            .padding(.top, 14)
        }
    }

    // MARK: Kilitli derste açıklama

    @ViewBuilder
    private var aciklama: some View {
        if !durum.aciklama.isEmpty {
            Text(durum.aciklama)
                .font(HaritaStil.yazi(14))
                .foregroundColor(Color("lesson_card_locked_fill"))
                .lineSpacing(2)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 8)
        }
    }

    // MARK: Rekor (recordLayout)

    @ViewBuilder
    private var rekor: some View {
        if let metin = durum.rekorMetni {
            Button(action: { model.rekoraDokun(durum) }) {
                HaritaRekorSatiri(metin: metin)
            }
            .buttonStyle(.plain)
            .disabled(!durum.rekorTiklanir)
            .padding(.top, 12)
        }
    }

    // MARK: Eylem düğmesi (actionButton)

    @ViewBuilder
    private var eylemDugmesi: some View {
        if durum.dugmeEtkin {
            etkinDugme
        } else {
            kapaliDugme
        }
    }

    /// Anahtarla açılabilen kilitli derste düğme "açık" mavisinde (gri kalsaydı devre dışı sanılırdı).
    private var dugmeRenkleri: HaritaPanelRenkleri {
        if durum.anahtarlaAcilir {
            return HaritaPanelRenkleri.al(.acik)
        }
        return renkler
    }

    private var etkinDugme: some View {
        let r: HaritaPanelRenkleri = dugmeRenkleri
        return Button(action: { model.eylemDugmesi(durum) }) {
            dugmeEtiketi(r)
        }
        .buttonStyle(
            GocenDugmeStili(
                zemin: Color(r.dugmeZemini),
                golge: HaritaStil.dudak(r.dugmeZemini),
                koseYaricapi: HaritaStil.dugmeKoseYaricapi,
                derinlik: HaritaStil.dugmeDerinligi
            )
        )
        .frame(height: HaritaStil.dugmeYuksekligi)
        .frame(maxWidth: .infinity)
        .disabled(model.anahtarIslemde)
    }

    /// Devre dışı düğme (Android: DepthButtons, state_enabled=false): dudaksız düz yüz.
    private var kapaliDugme: some View {
        let r: HaritaPanelRenkleri = dugmeRenkleri
        return dugmeEtiketi(r)
            .frame(maxWidth: .infinity)
            .frame(height: HaritaStil.dugmeYuksekligi - HaritaStil.dugmeDerinligi)
            .background(
                RoundedRectangle(cornerRadius: HaritaStil.dugmeKoseYaricapi)
                    .fill(Color(r.dugmeZemini))
            )
            .padding(.bottom, HaritaStil.dugmeDerinligi)
    }

    /// Metin 17pt kalın; anahtarla açmada metnin başında 20pt'lik anahtar (iconGravity textStart,
    /// MaterialButton iconPadding 8dp).
    private func dugmeEtiketi(_ r: HaritaPanelRenkleri) -> some View {
        HStack(spacing: 8) {
            if durum.anahtarlaAcilir {
                Image("key")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 20, height: 20)
            }
            Text(durum.dugmeMetni)
                .font(HaritaStil.yazi(17))
                .foregroundColor(r.dugmeYazisi)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .padding(.horizontal, 12)
    }

    // MARK: "Eğitimi tekrarla"

    @ViewBuilder
    private var egitimTekrar: some View {
        if durum.egitimTekrarGorunur {
            Button(action: { model.egitimiTekrarla(durum) }) {
                Text(Metin.al("lesson_popover_again_tutorial"))
                    .font(HaritaStil.yazi(14))
                    .foregroundColor(Color("lesson_card_locked_text"))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .padding(.top, 6)
        }
    }
}

/// lesson_popover.xml: recordLayout (lesson_panel_record_bg, köşe 12dp; yatay 10dp, dikey 4dp
/// iç boşluk): solda 28pt ateş animasyonu (fire_anim, döngülü), "Rekor: N", sağda ok.
struct HaritaRekorSatiri: View {
    let metin: String

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            LottieGorunumu("fire_anim", dongu: true)
                .frame(width: 28, height: 28)
                .padding(.trailing, 6)
            Text(metin)
                .font(HaritaStil.yazi(14))
                .foregroundColor(Color("lesson_panel_record_text"))
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            Image("ic_lesson_card_chevron")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(Color("lesson_panel_record_text"))
                .frame(width: 18, height: 18)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 4)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color("lesson_panel_record_bg"))
        )
        .contentShape(Rectangle())
    }
}

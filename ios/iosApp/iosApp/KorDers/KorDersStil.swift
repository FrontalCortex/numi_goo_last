import SwiftUI

// Kör ders ekranının ölçüleri, renkleri ve ortak düğmesi.
// Kaynak: res/layout/fragment_blinding_lesson.xml, res/values/dimens.xml, ilgili shape drawable'ları.

enum KorDersStil {
    // MARK: Ölçüler (dp → pt birebir)

    /// Kök ConstraintLayout: padding 10dp.
    static let kokBosluk: CGFloat = 10
    /// quitButton: 48dp.
    static let cikisBoyu: CGFloat = 48
    /// progressBarContainer: 250×16dp, margin 16dp.
    static let ilerlemeGenislik: CGFloat = 250
    static let ilerlemeYukseklik: CGFloat = 16
    static let ilerlemeUstBosluk: CGFloat = 16
    /// blackboardImage: yükseklik 300dp, marginTop 20dp.
    static let tahtaYukseklik: CGFloat = 300
    static let tahtaUstBosluk: CGFloat = 20
    /// Tahtadaki sayılar: marginBottom 20dp (dikey ortadan 10dp yukarı), padding 16dp, 40sp.
    static let tahtaYaziAltBosluk: CGFloat = 20
    static let tahtaYaziBosluk: CGFloat = 16
    static let tahtaYaziBoyu: CGFloat = 40
    /// timerTextView: 18sp, marginTop -10dp.
    static let sureYaziBoyu: CGFloat = 18
    static let sureUstBosluk: CGFloat = -10
    /// numberInput: 180×98dp, 32sp; rounded_edittext: köşe 12dp, çerçeve 10dp.
    static let kutuGenislik: CGFloat = 180
    static let kutuYukseklik: CGFloat = 98
    static let kutuYaziBoyu: CGFloat = 32
    static let kutuKose: CGFloat = 12
    static let kutuCerceve: CGFloat = 10
    /// kontrolButton / startButton: yükseklik 60dp, yan boşluk 24dp, alt boşluk 15dp, köşe 15dp, 18sp.
    static let anaDugmeYukseklik: CGFloat = 60
    static let anaDugmeYanBosluk: CGFloat = 24
    static let anaDugmeAltBosluk: CGFloat = 15
    static let dugmeKose: CGFloat = 15
    static let dugmeYaziBoyu: CGFloat = 18
    /// MaterialButton'ın varsayılan dikey iç boşluğu (insetTop/insetBottom 6dp): 60dp'lik düğmenin
    /// görünen gövdesi 48dp'dir.
    static let dugmeDikeyIcBosluk: CGFloat = 6
    /// Yan düğmeler (tutorial_ask_button_size 56dp; aralarında abacus_fragment_ask_button_margin_top 4dp).
    static let yanDugmeBoyu: CGFloat = 56
    static let yanDugmeAraligi: CGFloat = 4
    /// tvHint: yükseklik 55dp, 16sp, paddingLeft 10dp, paddingRight 30dp; hint_background köşe 8dp.
    static let ipucuYukseklik: CGFloat = 55
    static let ipucuYaziBoyu: CGFloat = 16
    /// Sonuç panelleri (dimens: tutorial_result_*).
    static let panelBosluk: CGFloat = 16
    static let panelSimgeBoyu: CGFloat = 24
    static let panelOgeBoslugu: CGFloat = 10
    static let panelBaslikSolBosluk: CGFloat = 8
    static let panelEtiketUstBosluk: CGFloat = 16
    static let panelBaslikYaziBoyu: CGFloat = 18
    static let panelGovdeYaziBoyu: CGFloat = 16
    static let panelDugmeUstBosluk: CGFloat = 16
    /// bottom_sheet_background: üst köşeler 16dp.
    static let panelKose: CGFloat = 16

    // MARK: Renkler

    /// 0xRRGGBB biçimindeki sabit renk.
    static func renk(_ rgb: UInt32) -> Color {
        let kirmizi = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: 1.0)
    }

    /// progressBarContainer zemini (#333) ve boş kısmı (#444).
    static let ilerlemeZemin: Color = KorDersStil.renk(0x333333)
    static let ilerlemeBos: Color = KorDersStil.renk(0x444444)
    /// updateProgressBar renkleri: #7CFC00, #FFFF99, #990000.
    static let ilerlemeYesil: Color = KorDersStil.renk(0x7CFC00)
    static let ilerlemeSari: Color = KorDersStil.renk(0xFFFF99)
    static let ilerlemeKoyuKirmizi: Color = KorDersStil.renk(0x990000)
    /// rounded_edittext: dolgu #BAB9B9, çerçeve siyah; yazı #222222.
    static let kutuDolgu: Color = KorDersStil.renk(0xBAB9B9)
    static let kutuYazi: Color = KorDersStil.renk(0x222222)
    /// fab_background: #263C31.
    static let yanDugmeZemin: Color = KorDersStil.renk(0x263C31)
    /// hint_background: @color/message_getter (#292E2C).
    static let ipucuZemin: Color = KorDersStil.renk(0x292E2C)
    /// bottom_sheet_background: #222.
    static let panelZemin: Color = KorDersStil.renk(0x222222)
    /// Doğru paneli ve ana düğmeler: #8CC63F; yanlış paneli: #FF4B4B.
    static let dogruYesil: Color = KorDersStil.renk(0x8CC63F)
    static let yanlisKirmizi: Color = KorDersStil.renk(0xFF4B4B)

    // MARK: Metinler (layout XML'inde sabit yazılmış; strings.xml'de karşılıkları yok)

    static let baslaMetni = "Başla"
    static let kontrolMetni = "Kontrol Et"
    static let dogruBaslik = "Aferin!"
    static let dogruDugme = "Devam et"
    static let yanlisBaslik = "Yanlış"
    static let yanlisDugme = "Devam Et"
    static let kutuIpucu = "?"
    static let abakusYerTutucuMetni = "Abaküs burada olacak. Şimdilik cevabı kutuya yaz."
}

/// Android'deki dokunma dinleyicili düğme: basınca 100 ms'de 0,85'e küçülür, bırakınca 400 ms'de
/// zıplayarak (BounceInterpolator) eski boyuna döner; eylem parmak kalkınca çalışır.
/// `basmaAnimasyonu` kapalıysa düz tıklama düğmesidir (startButton).
struct KorDersBasmaDugmesi: View {
    let baslik: String
    let zemin: Color
    let yaziRengi: Color
    let etkin: Bool
    let basmaAnimasyonu: Bool
    let eylem: () -> Void

    @State private var basili: Bool = false

    init(
        baslik: String,
        zemin: Color,
        yaziRengi: Color,
        etkin: Bool = true,
        basmaAnimasyonu: Bool = true,
        eylem: @escaping () -> Void
    ) {
        self.baslik = baslik
        self.zemin = zemin
        self.yaziRengi = yaziRengi
        self.etkin = etkin
        self.basmaAnimasyonu = basmaAnimasyonu
        self.eylem = eylem
    }

    var body: some View {
        govde
            .scaleEffect(basili ? 0.85 : 1.0)
            .gesture(dokunma)
            .allowsHitTesting(etkin)
    }

    private var govde: some View {
        Text(baslik)
            .font(Tema.yazi(KorDersStil.dugmeYaziBoyu))
            .foregroundColor(yaziRengi)
            .frame(maxWidth: .infinity)
            .frame(height: KorDersStil.anaDugmeYukseklik - 2 * KorDersStil.dugmeDikeyIcBosluk)
            .background(
                RoundedRectangle(cornerRadius: KorDersStil.dugmeKose, style: .continuous).fill(zemin)
            )
            .padding(.vertical, KorDersStil.dugmeDikeyIcBosluk)
            .contentShape(Rectangle())
    }

    private var dokunma: some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { _ in
                if basili || !basmaAnimasyonu { return }
                withAnimation(.easeInOut(duration: 0.1)) { basili = true }
            }
            .onEnded { _ in
                if basmaAnimasyonu {
                    // BounceInterpolator'ın yaklaşık karşılığı: az sönümlü yay, ~400 ms.
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.4)) { basili = false }
                }
                eylem()
            }
    }
}

/// Sağ kenardaki yuvarlak düğme (fab_background).
struct KorDersYanDugme<Icerik: View>: View {
    let eylem: () -> Void
    let icerik: () -> Icerik

    init(eylem: @escaping () -> Void, @ViewBuilder icerik: @escaping () -> Icerik) {
        self.eylem = eylem
        self.icerik = icerik
    }

    var body: some View {
        Button(action: eylem) {
            ZStack {
                Circle().fill(KorDersStil.yanDugmeZemin)
                icerik()
            }
            .frame(width: KorDersStil.yanDugmeBoyu, height: KorDersStil.yanDugmeBoyu)
        }
        .buttonStyle(.plain)
    }
}

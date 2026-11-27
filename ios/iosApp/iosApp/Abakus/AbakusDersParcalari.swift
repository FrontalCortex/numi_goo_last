import SwiftUI
import UIKit

// Abaküs dersi ekranının küçük parçaları (fragment_abacus.xml'deki görünümler).

// MARK: - Kara tahta ve soru

/// blackboardImage + firstNumberText / operator / secondNumberText.
struct AbakusKaraTahta: View {
    let ilkSayi: String
    let islec: String
    let ikinciSayi: String
    /// Kısa ekranlarda tahtanın küçültülme oranı (bkz. AbakusDersOlcegi); 1 = Android ölçüleri.
    var olcek: CGFloat = 1

    var body: some View {
        Color.clear
            // blackboardImage: layout_height
            .frame(height: 300 * olcek)
            .frame(maxWidth: .infinity)
            .overlay(
                // scaleType=centerCrop
                Image("class_board")
                    .resizable()
                    .scaledToFill()
            )
            .clipped()
            .overlay(soru)
    }

    /// İki sayı yatayda eşit boşlukla dağılır (ConstraintLayout zinciri), işleç ikisinin ortasında.
    /// abacus_fragment_equation_margin_bottom (20) dikey ortayı 10 yukarı kaydırır.
    private var soru: some View {
        ZStack {
            HStack(spacing: 0) {
                Spacer(minLength: 0)
                sayi(ilkSayi)
                Spacer(minLength: 0)
                sayi(ikinciSayi)
                Spacer(minLength: 0)
            }
            Text(islec)
                .font(Tema.yazi(36 * olcek))
                .foregroundColor(.white)
        }
        .offset(y: -10 * olcek)
    }

    private func sayi(_ metin: String) -> some View {
        Text(metin)
            // abacus_fragment_equation_text_size
            .font(Tema.yazi(36 * olcek))
            .foregroundColor(.white)
            .lineLimit(1)
            // abacus_fragment_equation_padding
            .padding(16 * olcek)
    }
}

// MARK: - İlerleme çubuğu

/// progressBarContainer: 250 × 16, zemin #333; dolu kısım progress_bar_fill, boş kısım #444.
struct AbakusIlerlemeCubugu: View {
    /// 0...1
    let oran: CGFloat
    let renk: Color

    private let genislik: CGFloat = 250
    private let yukseklik: CGFloat = 16

    var body: some View {
        let dolu: CGFloat = genislik * min(max(oran, 0), 1)
        let bos: CGFloat = genislik - dolu
        HStack(spacing: 0) {
            // progress_bar_fill köşeleri: solda 3, sağda 4 dp.
            RoundedRectangle(cornerRadius: 4)
                .fill(renk)
                .frame(width: dolu, height: yukseklik)
            RoundedRectangle(cornerRadius: 4)
                .fill(AbakusRenk.renk(0x444444))
                .frame(width: bos, height: yukseklik)
        }
        .frame(width: genislik, height: yukseklik)
        .background(AbakusRenk.renk(0x333333))
    }
}

// MARK: - İpucu

/// hintContainer: sağda yuvarlak düğme, dokununca solundan kayarak açılan ipucu balonu.
struct AbakusIpucuBolumu: View {
    @ObservedObject var model: AbakusDersModeli

    var body: some View {
        HStack(spacing: 0) {
            Spacer(minLength: 0)
            ZStack(alignment: .trailing) {
                if model.ipucuAcik && !model.ipucuMetni.isEmpty {
                    balon
                        // slide_in_right / slide_out_right_hint: 500 ms, sağdan kayma + belirme.
                        .transition(.move(edge: .trailing).combined(with: .opacity))
                }
                dugme
            }
        }
        .frame(height: 56)
    }

    private var balon: some View {
        Text(model.ipucuMetni)
            // abacus_fragment_hint_tv_text_size
            .font(Tema.yazi(16))
            .foregroundColor(.white)
            .multilineTextAlignment(.leading)
            .lineLimit(2)
            .minimumScaleFactor(0.7)
            .padding(.leading, 10)
            .padding(.trailing, 30)
            // abacus_fragment_hint_tv_height
            .frame(height: 55)
            // hint_background
            .background(RoundedRectangle(cornerRadius: 8).fill(Color("message_getter")))
            // Balonun sağ kenarı düğmenin altında kalır (kök dolgudan 31 pt içeride).
            .padding(.trailing, 31)
    }

    /// fabHint: raw/lesson_hint_ic, lottie_autoPlay=false, lottie_loop=false.
    /// showHint → playAnimation + setFrame(25) (25. kareden sona bir kez);
    /// hideHint → cancelAnimation + setFrame(0).
    private var simge: some View {
        let acik: Bool = model.ipucuAcik
        let kare: Double = acik ? 25.0 : 0.0
        return LottieGorunumu("lesson_hint_ic", dongu: false, oynat: acik, baslangicKare: kare)
            // 56 − 2 × abacus_fragment_hint_fab_padding (5)
            .frame(width: 46, height: 46)
    }

    private var dugme: some View {
        Button(action: { model.ipucuDokunuldu() }) {
            ZStack {
                // fab_background
                Circle().fill(AbakusRenk.renk(0x263C31))
                simge
            }
            // tutorial_ask_button_size
            .frame(width: 56, height: 56)
        }
        .buttonStyle(.plain)
        // abacus_fragment_hint_fab_margin_end (−25) ile düğme ekran kenarına 1 pt kalır.
        .padding(.trailing, -9)
    }
}

// MARK: - Düğmeler

/// Android: basınca 100 ms'de 0,85'e küçülür, bırakınca 400 ms'de zıplayarak (BounceInterpolator)
/// eski boyuna döner.
struct AbakusBasmaStili: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        let basili: Bool = configuration.isPressed
        return configuration.label
            .scaleEffect(basili ? 0.85 : 1.0)
            .animation(
                basili
                    ? Animation.easeInOut(duration: 0.1)
                    : Animation.interpolatingSpring(stiffness: 320, damping: 11),
                value: basili
            )
    }
}

/// kontrolButton / startButton / continueButton / okayButton: MaterialButton.
struct AbakusAnaDugme: View {
    let metin: String
    let yaziBoyu: CGFloat
    let kose: CGFloat
    /// Android'deki layout_height. MaterialButton üstten ve alttan 6 dp iç boşluk bırakır;
    /// görünen zemin bu yüzden 12 pt daha kısadır.
    let yukseklik: CGFloat
    let renk: Color
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            Text(metin)
                .font(Tema.yazi(yaziBoyu))
                .foregroundColor(.black)
                .frame(maxWidth: .infinity)
                .frame(height: yukseklik - 12)
                .background(RoundedRectangle(cornerRadius: kose).fill(renk))
                .frame(height: yukseklik)
                .contentShape(Rectangle())
        }
        .buttonStyle(AbakusBasmaStili())
    }
}

// MARK: - Sonuç panelleri

/// bottom_sheet_background: #222, yalnızca üst köşeler 16 dp yuvarlak.
struct AbakusPanelZemini: Shape {
    func path(in rect: CGRect) -> Path {
        let yol = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: [.topLeft, .topRight],
            cornerRadii: CGSize(width: 16, height: 16)
        )
        return Path(yol.cgPath)
    }
}

/// correctPanel
struct AbakusDogruPaneli: View {
    let devam: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 8) {
                Image("circle_correct")
                    .resizable()
                    .scaledToFit()
                    // tutorial_result_icon_size
                    .frame(width: 24, height: 24)
                Text("Aferin!")
                    // tutorial_result_title_text_size
                    .font(Tema.yazi(18))
                    .foregroundColor(AbakusRenk.renk(0x8CC63F))
                Spacer(minLength: 0)
            }
            AbakusAnaDugme(
                metin: "Devam et", yaziBoyu: 18, kose: 15, yukseklik: 60,
                renk: AbakusRenk.renk(0x8CC63F), eylem: devam
            )
            // tutorial_result_button_top_margin
            .padding(.top, 16)
        }
        // tutorial_result_panel_padding
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(
            AbakusPanelZemini()
                .fill(AbakusRenk.renk(0x222222))
                .ignoresSafeArea(edges: .bottom)
        )
    }
}

/// incorrectPanel
struct AbakusYanlisPaneli: View {
    /// correctAnswerLabel: "Doğru cevap N" (kırmızı)
    let dogruCevapMetni: String
    /// correctAnswerText: "Senin cevabın N" (beyaz)
    let cevapMetni: String
    let tamam: () -> Void

    private let kirmizi = AbakusRenk.renk(0xFF4B4B)

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 8) {
                Image("ic_delete")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                    .foregroundColor(kirmizi)
                Text("Yanlış")
                    .font(Tema.yazi(18))
                    .foregroundColor(kirmizi)
                Spacer(minLength: 0)
            }
            // tutorial_result_item_margin
            .padding(.top, 10)
            .padding(.leading, 10)
            Text(dogruCevapMetni)
                // tutorial_result_body_text_size
                .font(Tema.yazi(16))
                .foregroundColor(kirmizi)
                // tutorial_result_label_top_margin
                .padding(.top, 16)
                .padding(.leading, 10)
            Text(cevapMetni)
                .font(Tema.yazi(16))
                .foregroundColor(.white)
                .padding(.top, 10)
                .padding(.leading, 10)
            AbakusAnaDugme(
                metin: "Tekrar Dene", yaziBoyu: 18, kose: 15, yukseklik: 60,
                renk: kirmizi, eylem: tamam
            )
            .padding(.top, 16)
            .padding(.horizontal, 10)
        }
        // incorrectPanel: padding
        .padding(3)
        .frame(maxWidth: .infinity)
        .background(
            AbakusPanelZemini()
                .fill(AbakusRenk.renk(0x222222))
                .ignoresSafeArea(edges: .bottom)
        )
    }
}

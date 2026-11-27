import SwiftUI

// Kör ders ekranının parçaları: ilerleme çubuğu, tahta, sayı kutusu, yan düğmeler, sonuç panelleri.

/// Android: progressBarContainer (progressBarFill + progressBarEmpty; ağırlıklar 0...1).
struct KorDersIlerlemeCubugu: View {
    let oran: CGFloat
    let renk: Color

    var body: some View {
        let genislik: CGFloat = KorDersStil.ilerlemeGenislik
        let sinirli: CGFloat = min(max(oran, 0), 1)
        let dolu: CGFloat = genislik * sinirli
        return HStack(spacing: 0) {
            // progress_bar_fill: köşeler 3–4dp.
            RoundedRectangle(cornerRadius: 4, style: .continuous)
                .fill(renk)
                .frame(width: dolu)
            RoundedRectangle(cornerRadius: 4, style: .continuous)
                .fill(KorDersStil.ilerlemeBos)
                .frame(width: genislik - dolu)
        }
        .frame(width: genislik, height: KorDersStil.ilerlemeYukseklik)
        .background(KorDersStil.ilerlemeZemin)
    }
}

/// Android: blackboardImage + üstündeki sayı metinleri.
struct KorDersTahta: View {
    @ObservedObject var model: KorDersModeli
    /// Kısa ekranlarda tahtanın küçültülme oranı; 1 = Android ölçüleri.
    var olcek: CGFloat = 1

    var body: some View {
        ZStack {
            Color.clear
                .overlay(
                    Image("class_board")
                        .resizable()
                        .scaledToFill()
                )
                .clipped()
            yazilar
                .padding(.bottom, KorDersStil.tahtaYaziAltBosluk * olcek)
        }
        .frame(maxWidth: .infinity)
        .frame(height: KorDersStil.tahtaYukseklik * olcek)
        .allowsHitTesting(false)
    }

    @ViewBuilder
    private var yazilar: some View {
        if model.carpmaGosterimi {
            carpmaYazilari
        } else {
            // firstNumberText: tahtanın ortasında tek sayı.
            sayi(model.tahtaMetni)
        }
    }

    /// firstNumberText2 ve secondNumberText tahtaya eşit aralıkla yayılır; operator ikisinin ortasında.
    private var carpmaYazilari: some View {
        ZStack {
            HStack(spacing: 0) {
                Spacer(minLength: 0)
                sayi(model.birinciSayi)
                Spacer(minLength: 0)
                sayi(model.ikinciSayi)
                Spacer(minLength: 0)
            }
            Text(model.islec)
                .font(Tema.yazi(KorDersStil.tahtaYaziBoyu * olcek))
                .foregroundColor(.white)
        }
    }

    private func sayi(_ metin: String) -> some View {
        Text(metin)
            .font(Tema.yazi(KorDersStil.tahtaYaziBoyu * olcek))
            .foregroundColor(.white)
            .lineLimit(1)
            .minimumScaleFactor(0.5)
            .padding(KorDersStil.tahtaYaziBosluk * olcek)
    }
}

/// Android: numberInput (rounded_edittext; inputType="number" → yalnızca rakam, sistem klavyesi).
struct KorDersSayiKutusu: View {
    @Binding var metin: String
    var odak: FocusState<Bool>.Binding

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: KorDersStil.kutuKose, style: .continuous)
                .fill(KorDersStil.kutuDolgu)
            RoundedRectangle(cornerRadius: KorDersStil.kutuKose, style: .continuous)
                .strokeBorder(Color.black, lineWidth: KorDersStil.kutuCerceve)
            TextField(KorDersStil.kutuIpucu, text: $metin)
                .font(Tema.yazi(KorDersStil.kutuYaziBoyu))
                .foregroundColor(KorDersStil.kutuYazi)
                .multilineTextAlignment(.center)
                .keyboardType(.numberPad)
                .focused(odak)
                .padding(.horizontal, KorDersStil.kutuCerceve + 4)
        }
        .frame(width: KorDersStil.kutuGenislik, height: KorDersStil.kutuYukseklik)
        .onChange(of: metin) { yeni in
            // Yalnızca rakam; Int sınırını aşmasın diye en çok 9 hane.
            let rakamlar: String = String(yeni.filter { $0.isASCII && $0.isNumber }.prefix(9))
            if rakamlar != yeni { metin = rakamlar }
        }
    }
}

/// Sağ kenardaki düğme sütunu. Android'de alttan üste: skipStepButton, resetButton,
/// askQuestionButton, hintContainer (aralarında 4dp; hepsi startButton'ın üstünde).
/// Ekran kenarına uzaklık (kök dolgu 10dp + marginEnd): skipStepButton ve askQuestionButton
/// −10dp → 0, resetButton −9dp → 1, fabHint (16 − 25 = −9dp) → 1.
///
/// Üçü de Android'de LottieAnimationView'dur (autoPlay=false, loop=false):
///  - fabHint (raw/lesson_hint_ic): showHint'te bir kez oynar, hideHint'te ilk kareye döner.
///  - resetButton (raw/reset): 20. karede bekler; dokununca baştan sona bir kez oynar, bitişten
///    1 sn sonra yeniden 20. kareye alınır.
///  - skipStepButton (skip_step_arrow_anim.json): dokununca (dizi gösterilirken) bir kez oynar.
struct KorDersYanDugmeler: View {
    @ObservedObject var model: KorDersModeli

    @State private var sifirlaOynuyor: Bool = false
    @State private var sifirlaSayaci: Int = 0
    @State private var atlaOynuyor: Bool = false
    @State private var atlaSayaci: Int = 0

    /// raw/reset: 75 kare, 25 kare/sn; Android bitişten 1 sn sonra bekleme karesine döner.
    private static let sifirlaSuresi: Double = 75.0 / 25.0 + 1.0
    /// skip_step_arrow_anim: 10. kareden 37. kareye, 30 kare/sn.
    private static let atlaSuresi: Double = 27.0 / 30.0 + 0.1

    var body: some View {
        VStack(alignment: .trailing, spacing: KorDersStil.yanDugmeAraligi) {
            if !model.ipucuGizli {
                ipucu
                    .padding(.trailing, 1)
            }
            // askQuestionButton (öğretmene soru sor) bu ekranda yok; yeri boş bırakılır.
            Color.clear
                .frame(width: KorDersStil.yanDugmeBoyu, height: KorDersStil.yanDugmeBoyu)
                .allowsHitTesting(false)
            if !model.sifirlaGizli {
                sifirlaDugmesi
                    .padding(.trailing, 1)
            }
            if !model.adimAtlaGizli {
                atlaDugmesi
            }
        }
    }

    // MARK: Sıfırla

    private var sifirlaDugmesi: some View {
        KorDersYanDugme(eylem: { sifirlaDokunuldu() }) {
            sifirlaSimgesi
        }
    }

    /// Animasyonun ilk ve son karesi boştur; Android bu yüzden 20. karede bekletir
    /// (findsId: resetButton.frame = 20). Beklerken 20. kare, oynarken 0. kareden sona.
    /// Kimlik her dokunuşta değişir: oynarken yeniden dokunulursa baştan başlar.
    private var sifirlaSimgesi: some View {
        let kare: Double = sifirlaOynuyor ? 0.0 : 20.0
        return LottieGorunumu("reset", dongu: false, oynat: sifirlaOynuyor, baslangicKare: kare)
            .frame(width: KorDersStil.yanDugmeLottieBoyu, height: KorDersStil.yanDugmeLottieBoyu)
            .id(sifirlaSayaci)
    }

    /// Android: resetButton.setOnClickListener — her dokunuşta playAnimation (baştan başlar).
    private func sifirlaDokunuldu() {
        let no: Int = sifirlaSayaci + 1
        sifirlaSayaci = no
        sifirlaOynuyor = true
        model.sifirla()
        DispatchQueue.main.asyncAfter(deadline: .now() + KorDersYanDugmeler.sifirlaSuresi) {
            if sifirlaSayaci == no {
                sifirlaOynuyor = false
            }
        }
    }

    // MARK: Adım atla

    private var atlaDugmesi: some View {
        KorDersYanDugme(eylem: { atlaDokunuldu() }) {
            // Android: setMinFrame(10) — 10. karede bekler, dokununca 10. kareden sona oynar.
            LottieGorunumu("skip_step_arrow_anim", dongu: false, oynat: atlaOynuyor, baslangicKare: 10.0)
                .frame(width: KorDersStil.adimAtlaLottieBoyu, height: KorDersStil.adimAtlaLottieBoyu)
                .id(atlaSayaci)
        }
    }

    /// Android: setupSkipStepButton — dizi gösterilmiyorsa hiçbir şey yapmaz (animasyon da oynamaz).
    private func atlaDokunuldu() {
        if !model.adimAtlanabilir { return }
        let no: Int = atlaSayaci + 1
        atlaSayaci = no
        atlaOynuyor = true
        model.adimAtla()
        DispatchQueue.main.asyncAfter(deadline: .now() + KorDersYanDugmeler.atlaSuresi) {
            if atlaSayaci == no {
                atlaOynuyor = false
            }
        }
    }

    // MARK: İpucu

    /// Android: hintContainer — fabHint (lottie lesson_hint_ic) ve soldan açılan tvHint.
    private var ipucu: some View {
        ZStack(alignment: .trailing) {
            if model.ipucuAcik {
                ipucuBalonu
            }
            KorDersYanDugme(eylem: { model.ipucunuDegistir() }) {
                ipucuSimgesi
            }
        }
    }

    /// Android: showHint → playAnimation + setFrame(25) (25. kareden sona bir kez);
    /// hideHint → cancelAnimation + setFrame(0).
    private var ipucuSimgesi: some View {
        let acik: Bool = model.ipucuAcik
        let kare: Double = acik ? 25.0 : 0.0
        return LottieGorunumu("lesson_hint_ic", dongu: false, oynat: acik, baslangicKare: kare)
            .frame(width: KorDersStil.yanDugmeLottieBoyu, height: KorDersStil.yanDugmeLottieBoyu)
    }

    private var ipucuBalonu: some View {
        Text(model.ipucuMetni)
            .font(Tema.yazi(KorDersStil.ipucuYaziBoyu))
            .foregroundColor(.white)
            .lineLimit(2)
            .fixedSize(horizontal: true, vertical: false)
            .padding(.leading, 10)
            .padding(.trailing, 30)
            .frame(height: KorDersStil.ipucuYukseklik)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(KorDersStil.ipucuZemin)
            )
            // tvHint'in sağ kenarı düğmenin sağ kenarından 40dp içeridedir (15 + 25).
            .padding(.trailing, 40)
            // anim/slide_in_right ve slide_out_right_hint: 500 ms, kayma + belirme.
            .transition(.move(edge: .trailing).combined(with: .opacity))
    }
}

/// Android: correctPanel.
struct KorDersDogruPaneli: View {
    let devam: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 0) {
                Image("circle_correct")
                    .resizable()
                    .scaledToFit()
                    .frame(width: KorDersStil.panelSimgeBoyu, height: KorDersStil.panelSimgeBoyu)
                Text(KorDersStil.dogruBaslik)
                    .font(Tema.yazi(KorDersStil.panelBaslikYaziBoyu))
                    .foregroundColor(KorDersStil.dogruYesil)
                    .padding(.leading, KorDersStil.panelBaslikSolBosluk)
                Spacer(minLength: 0)
            }
            KorDersBasmaDugmesi(
                baslik: KorDersStil.dogruDugme,
                zemin: KorDersStil.dogruYesil,
                yaziRengi: .black,
                eylem: devam
            )
            .padding(.top, KorDersStil.panelDugmeUstBosluk)
        }
        .padding(KorDersStil.panelBosluk)
        .frame(maxWidth: .infinity)
        .background(KorDersPanelZemini())
    }
}

/// Android: incorrectPanel.
struct KorDersYanlisPaneli: View {
    let dogruCevapEtiketi: String
    let seninCevabinMetni: String
    let tamam: () -> Void

    var body: some View {
        let oge: CGFloat = KorDersStil.panelOgeBoslugu
        return VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 0) {
                Image("ic_delete")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(KorDersStil.yanlisKirmizi)
                    .frame(width: KorDersStil.panelSimgeBoyu, height: KorDersStil.panelSimgeBoyu)
                    .padding(oge)
                Text(KorDersStil.yanlisBaslik)
                    .font(Tema.yazi(KorDersStil.panelBaslikYaziBoyu))
                    .foregroundColor(KorDersStil.yanlisKirmizi)
                    .padding(.leading, KorDersStil.panelBaslikSolBosluk)
                    .padding(.top, oge)
                Spacer(minLength: 0)
            }
            Text(dogruCevapEtiketi)
                .font(Tema.yazi(KorDersStil.panelGovdeYaziBoyu))
                .foregroundColor(KorDersStil.yanlisKirmizi)
                .padding(.horizontal, oge)
                .padding(.top, KorDersStil.panelEtiketUstBosluk)
                .padding(.bottom, oge)
            Text(seninCevabinMetni)
                .font(Tema.yazi(KorDersStil.panelGovdeYaziBoyu))
                .foregroundColor(.white)
                .padding(oge)
            KorDersBasmaDugmesi(
                baslik: KorDersStil.yanlisDugme,
                zemin: KorDersStil.yanlisKirmizi,
                yaziRengi: .black,
                eylem: tamam
            )
            .padding(.horizontal, oge)
            .padding(.top, KorDersStil.panelDugmeUstBosluk)
        }
        // incorrectPanel: padding 3dp.
        .padding(3)
        .frame(maxWidth: .infinity)
        .background(KorDersPanelZemini())
    }
}

/// bottom_sheet_background: #222, yalnızca üst köşeler yuvarlak; alt güvenli alana taşar.
struct KorDersPanelZemini: View {
    var body: some View {
        KorDersUstKoseliSekil(yaricap: KorDersStil.panelKose)
            .fill(KorDersStil.panelZemin)
            .ignoresSafeArea(.container, edges: .bottom)
    }
}

/// Yalnızca üst köşeleri yuvarlatılmış dikdörtgen.
struct KorDersUstKoseliSekil: Shape {
    let yaricap: CGFloat

    func path(in rect: CGRect) -> Path {
        let r: CGFloat = min(yaricap, min(rect.width, rect.height) / 2)
        var yol = Path()
        yol.move(to: CGPoint(x: rect.minX, y: rect.maxY))
        yol.addLine(to: CGPoint(x: rect.minX, y: rect.minY + r))
        yol.addArc(
            center: CGPoint(x: rect.minX + r, y: rect.minY + r),
            radius: r, startAngle: .degrees(180), endAngle: .degrees(270), clockwise: false
        )
        yol.addLine(to: CGPoint(x: rect.maxX - r, y: rect.minY))
        yol.addArc(
            center: CGPoint(x: rect.maxX - r, y: rect.minY + r),
            radius: r, startAngle: .degrees(270), endAngle: .degrees(0), clockwise: false
        )
        yol.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        yol.closeSubpath()
        return yol
    }
}

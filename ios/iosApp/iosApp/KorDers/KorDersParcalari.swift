import SwiftUI

// Kör ders ekranının parçaları: ilerleme çubuğu, sayı kartı, sayı kutusu, yan düğmeler. Sonuç panelleri,
// göçen düğme ve yan düğme zemini abaküs dersiyle ortaktır (Abakus/AbakusDersOrtak.swift,
// Abakus/AbakusDersParcalari.swift).

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

/// Android: blackboardImage (artık bg_flash_card sayı kartı) + üstündeki sayı metinleri.
struct KorDersTahta: View {
    @ObservedObject var model: KorDersModeli

    var body: some View {
        AbakusKartZemini()
            // flash_card_height
            .frame(height: AbakusDersOlcu.kartYuksekligi)
            .overlay(
                yazilar
                    .padding(.bottom, KorDersStil.tahtaYaziAltBosluk)
            )
            .allowsHitTesting(false)
    }

    @ViewBuilder
    private var yazilar: some View {
        if model.carpmaGosterimi {
            carpmaYazilari
        } else {
            // firstNumberText: kartın ortasında tek sayı.
            sayi(model.tahtaMetni)
                .rehberHedefi(.ilkSayi)
        }
    }

    /// firstNumberText2 ve secondNumberText karta eşit aralıkla yayılır; operator ikisinin ortasında.
    private var carpmaYazilari: some View {
        ZStack {
            HStack(spacing: 0) {
                Spacer(minLength: 0)
                sayi(model.birinciSayi)
                    .rehberHedefi(.ilkSayi)
                Spacer(minLength: 0)
                sayi(model.ikinciSayi)
                    .rehberHedefi(.ikinciSayi)
                Spacer(minLength: 0)
            }
            Text(model.islec)
                .font(Tema.yazi(KorDersStil.tahtaYaziBoyu))
                .foregroundColor(.white)
        }
    }

    private func sayi(_ metin: String) -> some View {
        Text(metin)
            .font(Tema.yazi(KorDersStil.tahtaYaziBoyu))
            .foregroundColor(.white)
            .lineLimit(1)
            .minimumScaleFactor(0.5)
            .padding(KorDersStil.tahtaYaziBosluk)
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

/// Sağ kenardaki düğme sütunu. Android'de alttan üste (hepsi startButton'ın üstünde, aralarında 4dp):
/// abacusModeButton, skipStepButton, rulesPanelButton, resetButton, askQuestionButton, hintContainer.
/// Sihirli değnek (rulesPanelButton, kural paneli; bkz. Kurallar/) yalnızca tablo seçicili derslerde görünür;
/// öğretmene sor düğmesi Android'de gizli (TeacherQuestionsFeature.ENABLED = false), yeri boş kalır.
/// Ekran kenarına uzaklık (kök dolgu 10dp + marginEnd): skipStepButton, abacusModeButton −10dp → 0;
/// resetButton −9dp → 1; fabHint (16 − 25 = −9dp) → 1.
///
/// Dördü de Android'de LottieAnimationView'dur (autoPlay=false, loop=false):
///  - fabHint (raw/lesson_hint_ic): showHint'te bir kez oynar, hideHint'te ilk kareye döner.
///  - resetButton (raw/reset): 20. karede bekler; dokununca baştan sona bir kez oynar, bitişten
///    1 sn sonra yeniden 20. kareye alınır (LessonSideButtons.setupReset).
///  - skipStepButton (skip_step_arrow_anim.json): dokununca (dizi gösterilirken) bir kez oynar,
///    10. kareden başlar (LessonSideButtons.setupSkip).
///  - abacusModeButton (abacus_mode_ic_anim.json): ayar paneli (bkz. AbakusAyarDugmesi).
struct KorDersYanDugmeler: View {
    @ObservedObject var model: KorDersModeli
    @ObservedObject var ayar: AbakusGorunumAyari
    let ayarDokunuldu: () -> Void
    /// Sihirli değnek (rulesPanelButton, kural paneli): sıfırla ile adım atla düğmelerinin arasında (bkz. Kurallar/).
    @ObservedObject var kural: KuralModeli
    /// Sayı kutusu odakta (klavye açık): Android değneği devre dışı bırakır.
    let klavyeAcik: Bool

    @State private var sifirlaOynuyor: Bool = false
    @State private var sifirlaSayaci: Int = 0
    @State private var atlaOynuyor: Bool = false
    @State private var atlaSayaci: Int = 0

    /// raw/reset: 75 kare, 25 kare/sn; Android bitişten 1 sn sonra bekleme karesine döner.
    private static let sifirlaSuresi: Double = 75.0 / 25.0 + 1.0
    /// skip_step_arrow_anim: 10. kareden 37. kareye, 30 kare/sn.
    private static let atlaSuresi: Double = 27.0 / 30.0 + 0.1

    var body: some View {
        VStack(alignment: .trailing, spacing: AbakusDersOlcu.yanDugmeAraligi) {
            if !model.ipucuGizli {
                ipucu
                    .padding(.trailing, 1)
            }
            if !model.sifirlaGizli {
                sifirlaDugmesi
                    .padding(.trailing, 1)
            }
            if kural.ders.panelDugmesi != .yok {
                KuralDegnekDugmesi(eylem: { _ = kural.panelDugmesiDokunuldu() })
                    .rehberHedefi(.kuralPaneliDugmesi)
                    .allowsHitTesting(!klavyeAcik)
            }
            if !model.adimAtlaGizli {
                atlaDugmesi
                    .rehberHedefi(.adimAtlaDugmesi)
            }
            if model.ayarGorunur {
                AbakusAyarDugmesi(acik: ayar.acik, eylem: ayarDokunuldu)
                    .rehberHedefi(.abakusAyarDugmesi)
            }
        }
    }

    // MARK: Sıfırla

    private var sifirlaDugmesi: some View {
        AbakusYanDugme(eylem: { sifirlaDokunuldu() }) {
            sifirlaSimgesi
        }
    }

    /// Animasyonun ilk ve son karesi boştur; Android bu yüzden 20. karede bekletir
    /// (LessonSideButtons: RESET_IDLE_FRAME = 20). Beklerken 20. kare, oynarken 0. kareden sona.
    /// Kimlik her dokunuşta değişir: oynarken yeniden dokunulursa baştan başlar.
    private var sifirlaSimgesi: some View {
        let kare: Double = sifirlaOynuyor ? 0.0 : 20.0
        return LottieGorunumu("reset", dongu: false, oynat: sifirlaOynuyor, baslangicKare: kare)
            .frame(width: KorDersStil.yanDugmeLottieBoyu, height: KorDersStil.yanDugmeLottieBoyu)
            .id(sifirlaSayaci)
    }

    /// Android: resetButton — her dokunuşta playAnimation (baştan başlar) ve sıfırlama.
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
        AbakusYanDugme(eylem: { atlaDokunuldu() }) {
            // Android: setMinFrame(10) — 10. karede bekler, dokununca 10. kareden sona oynar.
            LottieGorunumu("skip_step_arrow_anim", dongu: false, oynat: atlaOynuyor, baslangicKare: 10.0)
                .frame(width: KorDersStil.adimAtlaLottieBoyu, height: KorDersStil.adimAtlaLottieBoyu)
                .id(atlaSayaci)
        }
    }

    /// Android: skipToNextSequenceNumber — dizi gösterilmiyorsa hiçbir şey yapmaz (animasyon da oynamaz);
    /// boşluk sürerken animasyon oynar ama sayı atlanmaz.
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

    /// Android: hintContainer — fabHint (lottie lesson_hint_ic) ve solunda açılan tvHint.
    private var ipucu: some View {
        ZStack(alignment: .trailing) {
            if model.ipucuAcik {
                ipucuBalonu
                    // Balon ampulün sol kenarından 3dp uzakta (düğme 48dp).
                    .padding(.trailing, AbakusDersOlcu.yanDugmeBoyu + KorDersStil.ipucuDugmeAraligi)
            }
            AbakusYanDugme(eylem: { model.ipucunuDegistir() }) {
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

    /// hint_bubble_background: gövde (köşe 16, sağdan 9dp içeride) + sağ kenarda 45° döndürülmüş 12dp'lik
    /// kuyruk (merkezi sağ kenardan 9dp içeride), ikisi de side_button_background renginde.
    private var ipucuBalonu: some View {
        Text(model.ipucuMetni)
            .font(Tema.yazi(KorDersStil.ipucuYaziBoyu))
            .foregroundColor(.white)
            .lineSpacing(KorDersStil.ipucuSatirAraligi)
            .multilineTextAlignment(.leading)
            .fixedSize(horizontal: true, vertical: true)
            .padding(.leading, KorDersStil.ipucuBasBosluk)
            .padding(.trailing, KorDersStil.ipucuSonBosluk)
            .padding(.vertical, KorDersStil.ipucuDikeyBosluk)
            // minHeight abacus_side_button_size
            .frame(minHeight: AbakusDersOlcu.yanDugmeBoyu)
            .background(ipucuZemini)
            // hint_bubble_in: ampule doğru kuyruğundan büyüyerek 350 ms'de açılır (hafif aşma);
            // hint_bubble_out: 200 ms'de 0,6'ya küçülüp kaybolur.
            .transition(
                AnyTransition.asymmetric(
                    insertion: AnyTransition.scale(scale: 0.5, anchor: .trailing)
                        .combined(with: AnyTransition.opacity),
                    removal: AnyTransition.scale(scale: 0.6, anchor: .trailing)
                        .combined(with: AnyTransition.opacity)
                )
            )
    }

    private var ipucuZemini: some View {
        ZStack(alignment: .trailing) {
            RoundedRectangle(cornerRadius: KorDersStil.ipucuKose, style: .continuous)
                .fill(AbakusDersRenk.yanDugme)
                .padding(.trailing, KorDersStil.ipucuKuyrukPayi)
            RoundedRectangle(cornerRadius: 2, style: .continuous)
                .fill(AbakusDersRenk.yanDugme)
                .frame(width: 12, height: 12)
                .rotationEffect(.degrees(45))
                .padding(.trailing, 3)
        }
    }
}

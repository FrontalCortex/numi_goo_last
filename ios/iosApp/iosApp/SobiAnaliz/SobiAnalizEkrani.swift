import Shared
import SwiftUI

// "Sobi ile analiz et" penceresi: yanlış cevaplanan soruyu Sobi anlatarak adım adım yeniden çözdürür.
//
// Android kaynakları: MistakeReviewDialog.kt (analiz kipi), res/layout/dialog_mistake_review.xml,
// MistakeAnalyzer.kt, StepExplainer.kt, SubStepExplainer.kt, SobiAbacusPlayer.kt, SobiRuleStats.kt,
// ExamplePicker.kt. Kural motoru ortak kodda (shared/.../sobianaliz/); yapay zekâ yok.
//
// Yerleşim (dp → pt): üstte kapat (44, kenarlardan 8), ‹ (44 × 40, 4 sonra), adım yazısı (15 sp kalın,
// tek satır, yanlarda 6), sesli okuma (40 × 40, yalnızca ses motoru hazırsa), › (44 × 40, sağdan 10);
// ortada adımın sohbeti (yanlardan 12, üstten 10, alttan 6 + yazma satırının 6'sı); altta abaküs
// (ortalı, alttan 10) ve Kontrol Et / Derse dön (48, yanlardan 12, alttan 16; bg_depth_tutorial_control).
//
// Android'de olup burada olmayanlar (yapay zekâya bağlı): Sobi'ye yazma kutusu ve "hesap açınca
// yazabilirsin" notu, bakiye / Pro deneme satırı, sunucu önerileri ("Neden böyle?", "Bunu anlamadım",
// "Sıradaki soru", "Bir daha göster"), beğen / beğenme / bildir. Öğretici kipi (forTutorial) de yok.
struct SobiAnalizEkrani: View {
    @StateObject private var model: SobiAnalizModeli
    private let kapat: () -> Void

    /// - Parameters:
    ///   - istek: ders ekranının verdiği soru, cevap, değer kaydı ve işaretler.
    ///   - hizmetler: sesli okuma, sesler, kural istatistikleri, örnek ödemesi (varsayılan: sahte).
    ///   - kapat: kapat düğmesi ya da sonuç sayfasındaki "Derse dön". `istek.kapandi`'yı çağırmak
    ///     açan tarafın işidir.
    init(
        istek: AbakusSobiAnalizIstegi,
        hizmetler: SobiAnalizHizmetleri = SobiAnalizHizmetleri.sahte(),
        kapat: @escaping () -> Void
    ) {
        _model = StateObject(wrappedValue: SobiAnalizModeli(istek: istek, hizmetler: hizmetler))
        self.kapat = kapat
    }

    var body: some View {
        let tutamak: SobiAnalizModeli = model
        let cikis: () -> Void = kapat
        let kapatVeBitir: () -> Void = {
            tutamak.kapanis()
            cikis()
        }
        return ZStack {
            SobiAnalizRenk.zemin.ignoresSafeArea()
            VStack(spacing: 0) {
                SobiAnalizUstCubuk(model: model, kapat: kapatVeBitir)
                SobiAnalizSohbet(model: model)
                    .padding(.horizontal, 12)
                    .padding(.top, 10)
                    .padding(.bottom, 12)
                SobiAnalizAbakusAlani(model: model)
                    .padding(.bottom, 10)
                SobiAnalizKontrolDugmesi(model: model, kapat: kapatVeBitir)
                    .padding(.horizontal, 12)
                    .padding(.bottom, 16)
            }
        }
        .onAppear {
            // Android: ilk sayfa abaküs ölçüldükten SONRA açılır (abacusLinear.post); tahta bağlansın.
            DispatchQueue.main.async {
                tutamak.baslat()
            }
        }
    }
}

// MARK: - Üst çubuk

struct SobiAnalizUstCubuk: View {
    @ObservedObject var model: SobiAnalizModeli
    let kapat: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            kapatDugmesi
                .padding(.leading, 8)
            SobiAnalizGezinmeDugmesi(
                simge: "ic_review_chevron_left",
                etkin: model.geriVar && !model.mesgul,
                aciklama: Metin.al("review_prev"),
                eylem: { model.geriDokunuldu() }
            )
            .padding(.leading, 4)
            Text(model.baslik)
                .font(Tema.yazi(15))
                .foregroundColor(SobiAnalizRenk.yazi)
                .lineLimit(1)
                .truncationMode(.tail)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 6)
            if model.seslendiriciVar {
                okumaDugmesi
                    .padding(.trailing, 6)
            }
            SobiAnalizGezinmeDugmesi(
                simge: "ic_lesson_card_chevron",
                etkin: model.ileriVar && !model.mesgul,
                aciklama: Metin.al("review_next"),
                eylem: { model.ileriDokunuldu() }
            )
            .padding(.trailing, 10)
        }
        .frame(height: 44)
        .padding(.top, 8)
    }

    /// reviewClose: 44 × 44, iç boşluk 10, ic_close_black @color/dark_text_secondary ile boyanır.
    private var kapatDugmesi: some View {
        Button(action: kapat) {
            Image("ic_close_black")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(SobiAnalizRenk.ikincilYazi)
                .frame(width: 24, height: 24)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(Metin.al("sobi_close_desc")))
    }

    /// reviewSpeak: 40 × 40, bg_hw_chip; iç boşluk 8/7/8/11, volume_up_ic; otomatik okuma kapalıyken 0,4.
    private var okumaDugmesi: some View {
        Button(action: { model.otomatikOkumayiDegistir() }) {
            Image("volume_up_ic")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(SobiAnalizRenk.yazi)
                .frame(width: 24, height: 22)
                .frame(width: 40, height: 36)
        }
        .buttonStyle(SobiAnalizGriCipStili())
        .opacity(model.otomatikOku ? 1 : 0.4)
        .accessibilityLabel(Text(Metin.al("sobi_auto_read")))
    }
}

/// reviewPrev / reviewNext: 44 × 40, bg_hw_chip, 24 dp ok @color/dark_text; geçilemezken 0,35.
struct SobiAnalizGezinmeDugmesi: View {
    let simge: String
    let etkin: Bool
    let aciklama: String
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            Image(simge)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(SobiAnalizRenk.yazi)
                .frame(width: 24, height: 24)
                .frame(width: 44, height: 36)
        }
        .buttonStyle(SobiAnalizGriCipStili())
        .disabled(!etkin)
        .opacity(etkin ? 1 : 0.35)
        .accessibilityLabel(Text(aciklama))
    }
}

// MARK: - Sohbet

/// reviewSobiScroll / reviewMessages: sayfanın mesajları ve altında kural motorunun seçenekleri; her
/// değişiklikte sona kayar.
struct SobiAnalizSohbet: View {
    @ObservedObject var model: SobiAnalizModeli

    private static let sonKimlik: String = "sobi-analiz-son"

    var body: some View {
        ScrollViewReader { okuyucu in
            ScrollView(.vertical, showsIndicators: true) {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(model.mesajlar) { mesaj in
                        SobiAnalizMesajSatiri(
                            mesaj: mesaj,
                            sesliOkuma: model.seslendiriciVar,
                            oku: { model.mesajiOku(mesaj.metin) }
                        )
                    }
                    if !model.balonlar.isEmpty {
                        SobiAnalizSecenekSatiri(
                            balonlar: model.balonlar,
                            dokunuldu: { kod in model.balonaDokunuldu(kod) }
                        )
                    }
                    Color.clear
                        .frame(height: 1)
                        .id(SobiAnalizSohbet.sonKimlik)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .onChange(of: model.mesajlar) { _ in
                sonaKaydir(okuyucu)
            }
            .onChange(of: model.balonlar) { _ in
                sonaKaydir(okuyucu)
            }
            .onAppear {
                sonaKaydir(okuyucu)
            }
        }
        .frame(maxHeight: .infinity)
    }

    private func sonaKaydir(_ okuyucu: ScrollViewProxy) {
        DispatchQueue.main.async {
            withAnimation(.easeOut(duration: 0.2)) {
                okuyucu.scrollTo(SobiAnalizSohbet.sonKimlik, anchor: .bottom)
            }
        }
    }
}

// MARK: - Abaküs

/// abacusContainer: fragment_homework_player.xml'deki bloğun kopyası (5 çubuk); talimatın sütunu parlar.
struct SobiAnalizAbakusAlani: View {
    @ObservedObject var model: SobiAnalizModeli

    var body: some View {
        let tutamak: SobiAnalizModeli = model
        return AbakusGorunumu(
            denetleyici: model.abakus,
            basamakSayisi: 5,
            kilitli: model.mesgul,
            degerDegisti: { deger in tutamak.abakusDegisti(deger) }
        )
        .overlay(alignment: .topLeading) {
            SobiAnalizSutunVurgusu(vurgu: model.vurgu)
        }
    }
}

// MARK: - Alt düğme

/// reviewCheck: Kontrol Et (bilgi sayfasında Derse dön); 48, yüz #8FD14F, dudak #62A12E, köşe 10,
/// siyah 17 sp kalın. Abaküs meşgulken kapalı ve yarı saydam.
struct SobiAnalizKontrolDugmesi: View {
    @ObservedObject var model: SobiAnalizModeli
    let kapat: () -> Void

    var body: some View {
        let metin: String = model.adimSayfasi ? Metin.al("review_check") : Metin.al("review_back")
        let tutamak: SobiAnalizModeli = model
        let cikis: () -> Void = kapat
        return AbakusGocenDugme(
            metin: metin,
            yaziBoyu: 17,
            yaziRengi: Color.black,
            yuz: AbakusDersRenk.kontrolYuz,
            dudak: AbakusDersRenk.kontrolDudak,
            kose: 10,
            yukseklik: 48,
            eylem: { tutamak.kontrolDokunuldu(kapat: cikis) }
        )
        .disabled(model.mesgul)
        .opacity(model.mesgul ? 0.5 : 1)
    }
}

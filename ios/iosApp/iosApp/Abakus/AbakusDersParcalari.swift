import SwiftUI

// Abaküs dersi ekranının küçük parçaları (fragment_abacus.xml'deki görünümler). Sayı kartı, yan düğme ve
// göçen düğme gibi kör dersle ortak parçalar AbakusDersOrtak.swift'tedir.

// MARK: - Sayı kartı ve soru

/// blackboardImage (artık bg_flash_card sayı kartı) + firstNumberText / operator / secondNumberText.
struct AbakusSayiKarti: View {
    let ilkSayi: String
    let islec: String
    let ikinciSayi: String

    var body: some View {
        AbakusKartZemini()
            // flash_card_height
            .frame(height: AbakusDersOlcu.kartYuksekligi)
            .overlay(soru)
    }

    /// İki sayı yatayda eşit boşlukla dağılır (ConstraintLayout zinciri), işleç ikisinin ortasında.
    /// abacus_fragment_equation_margin_bottom (4) dikey ortayı 2 yukarı kaydırır.
    private var soru: some View {
        ZStack {
            HStack(spacing: 0) {
                Spacer(minLength: 0)
                sayi(ilkSayi)
                    .rehberHedefi(.ilkSayi)
                Spacer(minLength: 0)
                sayi(ikinciSayi)
                    .rehberHedefi(.ikinciSayi)
                Spacer(minLength: 0)
            }
            Text(islec)
                .font(Tema.yazi(36))
                .foregroundColor(.white)
        }
        .offset(y: -2)
    }

    private func sayi(_ metin: String) -> some View {
        Text(metin)
            // abacus_fragment_equation_text_size
            .font(Tema.yazi(36))
            .foregroundColor(.white)
            .lineLimit(1)
            // abacus_fragment_equation_padding
            .padding(16)
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

// MARK: - Sonuç panelleri

/// correctPanel: 16 dp iç boşluk, 24 dp simge, "Aferin!" 18 sp, 50 dp göçen "Devam et" düğmesi.
struct AbakusDogruPaneli: View {
    /// Başlık ("Aferin!") ve düğme metni ("Devam et"): abaküs dersi fragment_abacus_*, kör ders
    /// fragment_blinding_lesson_* anahtarlarını verir.
    let baslik: String
    let dugmeMetni: String
    let devam: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 8) {
                Image("circle_correct")
                    .resizable()
                    .scaledToFit()
                    // tutorial_result_icon_size
                    .frame(width: 24, height: 24)
                Text(baslik)
                    // tutorial_result_title_text_size
                    .font(Tema.yazi(18))
                    .foregroundColor(AbakusDersRenk.yesilYazi)
                Spacer(minLength: 0)
            }
            AbakusGocenDugme(
                metin: dugmeMetni, yaziBoyu: 18, yaziRengi: .black,
                yuz: AbakusDersRenk.devamYuz, dudak: AbakusDersRenk.devamDudak, kose: 15,
                yukseklik: AbakusDersOlcu.panelDugmeYuksekligi, eylem: devam
            )
            // tutorial_result_button_top_margin
            .padding(.top, 16)
        }
        // tutorial_result_panel_padding
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(
            AbakusPanelZemini()
                .fill(AbakusDersRenk.panelZemin)
                .ignoresSafeArea(edges: .bottom)
        )
    }
}

/// incorrectPanel: simge + "Yanlış", kırmızı "Doğru cevap N", beyaz "Senin cevabın N", sağ üstte
/// "Sobi ile analiz et" (varsa), altta 50 dp göçen "Tekrar Dene" düğmesi.
struct AbakusYanlisPaneli: View {
    /// correctAnswerLabel: "Doğru cevap N" (kırmızı)
    let dogruCevapMetni: String
    /// correctAnswerText: "Senin cevabın N" (beyaz)
    let cevapMetni: String
    let analizGorunur: Bool
    let analiz: () -> Void
    let tamam: () -> Void
    /// Başlık ("Yanlış") ve düğme metni ("Tekrar Dene" / kör derste "Devam Et").
    let baslik: String
    let dugmeMetni: String

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ust
            Text(dogruCevapMetni)
                // tutorial_result_body_text_size
                .font(Tema.yazi(16))
                .foregroundColor(AbakusDersRenk.kirmizi)
                // tutorial_result_label_top_margin
                .padding(.top, 16)
                .padding(.leading, 10)
            Text(cevapMetni)
                .font(Tema.yazi(16))
                .foregroundColor(.white)
                .padding(.top, 10)
                .padding(.leading, 10)
            AbakusGocenDugme(
                metin: dugmeMetni, yaziBoyu: 18, yaziRengi: .black,
                yuz: AbakusDersRenk.tamamYuz, dudak: AbakusDersRenk.tamamDudak, kose: 15,
                yukseklik: AbakusDersOlcu.panelDugmeYuksekligi, eylem: tamam
            )
            .padding(.top, 16)
            .padding(.horizontal, 10)
        }
        // incorrectPanel: padding
        .padding(3)
        .frame(maxWidth: .infinity)
        .background(
            AbakusPanelZemini()
                .fill(AbakusDersRenk.panelZemin)
                .ignoresSafeArea(edges: .bottom)
        )
    }

    /// Başlık satırı: solda simge ve "Yanlış", sağda (görünürse) analiz düğmesi (üst kenara hizalı, 10 dp
    /// boşluklu).
    private var ust: some View {
        HStack(alignment: .top, spacing: 8) {
            Image("ic_delete")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: 24, height: 24)
                .foregroundColor(AbakusDersRenk.kirmizi)
            Text(baslik)
                .font(Tema.yazi(18))
                .foregroundColor(AbakusDersRenk.kirmizi)
            Spacer(minLength: 0)
            if analizGorunur {
                AbakusSobiAnalizDugmesi(eylem: analiz)
            }
        }
        // tutorial_result_item_margin
        .padding(.top, 10)
        .padding(.leading, 10)
        .padding(.trailing, analizGorunur ? 10 : 0)
    }
}

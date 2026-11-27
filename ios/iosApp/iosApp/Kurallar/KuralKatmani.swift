import SwiftUI

// Ders ekranlarına eklenen kural katmanı: kural kitabı düğmesi (sağ üst), kitap ve kural paneli, sihirli değnek
// düğmesi (yan düğme sütununda).
//
// Android: fragment_abacus.xml / fragment_blinding_lesson.xml içindeki rulesBookButton, rulesPanelButton,
// rulesFragmentContainer ve rulesPanelScrollView (view_rules_panel). Yerleşim ConstraintLayout kısıtlarından
// çıkarıldı (bkz. KuralYerlesimi).

// MARK: - Düğmeler

/// rulesBookButton: 56 dp daire (fab_background), 10 dp iç boşluk → 36 dp math_book_ic2. Sağ üstte, ekran
/// kenarına yaslı (marginEnd −10 + kök dolgu 10). GONE: hiç yok; INVISIBLE: yer tutar, görünmez ve dokunulmaz.
struct KuralKitapDugmesi: View {
    @ObservedObject var kural: KuralModeli

    var body: some View {
        let durum: KuralGorunurluk = kural.ders.kitapDugmesi
        if durum != .yok {
            Button(action: { kural.kitabiAc() }) {
                ZStack {
                    Circle().fill(AbakusDersRenk.yanDugme)
                    Image("math_book_ic2")
                        .resizable()
                        .scaledToFit()
                        // abacus_fragment_rules_book_padding
                        .frame(width: 36, height: 36)
                }
                // abacus_fragment_rules_book_size
                .frame(width: 56, height: 56)
                .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .opacity(durum == .gizli ? 0 : 1)
            .allowsHitTesting(durum == .gorunur)
        }
    }
}

/// rulesPanelButton (sihirli değnek): 48 dp daire, 12 dp iç boşluk → 24 dp magic_wand_ic.
struct KuralDegnekDugmesi: View {
    let eylem: () -> Void

    var body: some View {
        AbakusYanDugme(eylem: eylem) {
            Image("magic_wand_ic")
                .resizable()
                .scaledToFit()
                // tutorial_rules_panel_button_padding
                .frame(width: 24, height: 24)
        }
    }
}

// MARK: - Yerleşim

/// Kitap ve panelin ekrandaki yeri (ekranın üstünden / altından uzaklıklar, kök dolgu 10 dahil).
///
/// Android:
///  - rulesFragmentContainer: üstü sayı kartının altı, altı abaküs çerçevesinin üstü, kenarlardan kök dolgu.
///  - rulesPanelScrollView: üstü süre yazısının altından 4 dp aşağıda, altı abaküsün üstünden 5 dp içeride,
///    yatayda ortalı, içerik kısaysa abaküse yapışık (bias 1), uzunsa kaydırılır. Kör derste sayı kutusuyla
///    cevaplanırken yükseklik 160 dp ve altı sayı kutusunun 5 dp üstündedir.
/// Abaküsün yeri dersin kendi ölçeğinden (kısa ekran küçültmesi, alt boşluk ayarı) hesaplanır; bu iki
/// ekranın düzen hesabının kopyasıdır (AbakusDersEkrani.AbakusDersIcerigi, KorDersEkrani.olcekHesapla).
struct KuralYerlesimi: Equatable {
    var kitapUst: CGFloat
    var kitapAlt: CGFloat
    var panelUst: CGFloat
    var panelAlt: CGFloat
    /// Kör derste sayı kutusu varken panelin sabit yüksekliği (160); aksi halde nil.
    var panelSabitYukseklik: CGFloat?

    private static let kok: CGFloat = AbakusDersOlcu.kokBosluk
    /// timerTextView (18 sp) yaklaşık yüksekliği; kartın altından abacus_fragment_timer_margin_top sonra gelir.
    private static let sureYuksekligi: CGFloat = 27

    /// Abaküs dersi (AbakusDersEkrani): düğme alanı 48 + 16.
    static func abakusDersi(ekran: CGSize, altBosluk: CGFloat, sureGorunur: Bool) -> KuralYerlesimi {
        let dugme: CGFloat = AbakusDersOlcu.anaDugmeYuksekligi + 16
        let ust: CGFloat = AbakusDersOlcu.cikisBoyu + AbakusDersOlcu.kartUstBosluk + AbakusDersOlcu.kartYuksekligi
        let sabit: CGFloat = ust + AbakusDersOlcu.enAzAra + altBosluk + dugme
        let olcek: CGFloat = AbakusDersOlcu.abakusOlcegi(yukseklik: ekran.height - 2 * kok, sabit: sabit)
        let abakusBoyu: CGFloat = AbakusGorunumu.boyut(basamakSayisi: 5).height * olcek
        let abakusUstu: CGFloat = ekran.height - kok - dugme - altBosluk - abakusBoyu
        return olustur(
            yukseklik: ekran.height, abakusUstu: abakusUstu, sureGorunur: sureGorunur, sayiKutusuUstu: nil)
    }

    /// Kör ders (KorDersEkrani): düğme alanı 48 + 15. Sayı kutusuyla cevaplanırken abaküs görünmez ama
    /// Android'de yerini tutar (doğal boyutta); panel sayı kutusunun üstüne oturur.
    static func korDersi(
        ekran: CGSize, altBosluk: CGFloat, sureGorunur: Bool, klavyeyleCevap: Bool
    ) -> KuralYerlesimi {
        let dugme: CGFloat = AbakusDersOlcu.anaDugmeYuksekligi + KorDersStil.anaDugmeAltBosluk
        let ust: CGFloat = AbakusDersOlcu.cikisBoyu + AbakusDersOlcu.kartUstBosluk + AbakusDersOlcu.kartYuksekligi
        let sabit: CGFloat = ust + AbakusDersOlcu.enAzAra + altBosluk + dugme
        var olcek: CGFloat = AbakusDersOlcu.abakusOlcegi(yukseklik: ekran.height - 2 * kok, sabit: sabit)
        if klavyeyleCevap {
            olcek = 1
        }
        let abakusBoyu: CGFloat = AbakusGorunumu.boyut(basamakSayisi: 5).height * olcek
        let abakusUstu: CGFloat = ekran.height - kok - dugme - altBosluk - abakusBoyu
        var kutuUstu: CGFloat? = nil
        if klavyeyleCevap {
            // numberInput: kartın altı ile kontrolButton'ın üstü arasında dikey ortalı, 98 dp yüksekliğinde.
            let kartAlti: CGFloat = kok + ust
            let kontrolUstu: CGFloat = ekran.height - kok - dugme
            kutuUstu = (kartAlti + kontrolUstu) / 2 - KorDersStil.kutuYukseklik / 2
        }
        return olustur(
            yukseklik: ekran.height, abakusUstu: abakusUstu, sureGorunur: sureGorunur, sayiKutusuUstu: kutuUstu)
    }

    private static func olustur(
        yukseklik: CGFloat, abakusUstu: CGFloat, sureGorunur: Bool, sayiKutusuUstu: CGFloat?
    ) -> KuralYerlesimi {
        let kartAlti: CGFloat = kok + AbakusDersOlcu.cikisBoyu + AbakusDersOlcu.kartUstBosluk
            + AbakusDersOlcu.kartYuksekligi
        let sure: CGFloat = sureGorunur ? AbakusDersOlcu.sureUstBosluk + sureYuksekligi : 0
        let panelUst: CGFloat = kartAlti + sure + 4
        var panelAltKenar: CGFloat = abakusUstu + 5
        var sabit: CGFloat? = nil
        if let kutuUstu = sayiKutusuUstu {
            panelAltKenar = kutuUstu - 5
            sabit = 160
        }
        let kitapAltKenar: CGFloat = max(abakusUstu, kartAlti)
        return KuralYerlesimi(
            kitapUst: kartAlti,
            kitapAlt: max(yukseklik - kitapAltKenar, 0),
            panelUst: panelUst,
            panelAlt: max(yukseklik - panelAltKenar, 0),
            panelSabitYukseklik: sabit
        )
    }
}

// MARK: - Katman

/// Kitap düğmesi, kitap ve panel. Ders ekranının ZStack'ine, yan düğme sütunundan sonra eklenir; boş
/// yerlerinde dokunuşu geçirir.
struct KuralKatmani: View {
    @ObservedObject var kural: KuralModeli
    let yerlesim: KuralYerlesimi
    /// Kör derste sayı kutusu odakta (klavye açık): Android paneli gizler.
    let klavyeAcik: Bool

    var body: some View {
        ZStack {
            panelAlani
            kitapAlani
            kitapDugmeSatiri
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: Kitap düğmesi

    private var kitapDugmeSatiri: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                Spacer(minLength: 0)
                KuralKitapDugmesi(kural: kural)
                    .rehberHedefi(.kuralKitabiDugmesi)
            }
            Spacer(minLength: 0)
        }
        // kök dolgu (üst)
        .padding(.top, AbakusDersOlcu.kokBosluk)
    }

    // MARK: Panel

    @ViewBuilder
    private var panelAlani: some View {
        if kural.panelAcik && kural.icerikVar && !klavyeAcik {
            panelIcerigi
                .frame(
                    maxWidth: .infinity,
                    maxHeight: yerlesim.panelSabitYukseklik ?? CGFloat.infinity,
                    alignment: .bottom
                )
                .padding(.top, yerlesim.panelUst)
                .padding(.bottom, yerlesim.panelAlt)
                .frame(maxHeight: .infinity, alignment: .bottom)
        }
    }

    /// İçerik sığıyorsa olduğu gibi, sığmıyorsa kaydırılarak (Android: wrap_content ScrollView).
    private var panelIcerigi: some View {
        let tablo: KuralPaneli = KuralPaneli(
            tablo: kural.aktifTablo,
            carpimBasamagi: kural.carpimBasamagi,
            metinOnEki: kural.ders.panelMetinOnEki,
            besCikarma: kural.ders.besCikarmaMi
        )
        return ViewThatFits(in: .vertical) {
            tablo
            ScrollView(.vertical, showsIndicators: true) {
                tablo
                    .frame(maxWidth: .infinity)
            }
        }
    }

    // MARK: Kitap

    private var kitapAlani: some View {
        ZStack {
            if kural.kitapAcik {
                KuralKitabiEkrani(
                    ayar: kural.kitapAyari,
                    secildi: { (secim: KuralSecimi) -> Void in kural.tabloSecildi(secim) },
                    kapat: { kural.kitabiKapat() }
                )
                // slide_down / slide_up: kutunun üst kenarından kayarak girer ve çıkar.
                .transition(.move(edge: .top))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
        .padding(.top, yerlesim.kitapUst)
        .padding(.bottom, yerlesim.kitapAlt)
        .padding(.horizontal, AbakusDersOlcu.kokBosluk)
        .allowsHitTesting(kural.kitapAcik)
    }
}

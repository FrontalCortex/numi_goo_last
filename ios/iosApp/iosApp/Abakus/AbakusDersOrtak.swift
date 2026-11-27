import SwiftUI
import UIKit

// Abaküs dersi ile kör dersin ortak parçaları: sayı kartı, yan düğme sütunu, göçen düğmeler ve
// "Sobi ile analiz et" düğmesi.
//
// Android kaynakları (güncel): res/drawable/bg_flash_card.xml, fab_background.xml,
// bg_depth_tutorial_control / _continue / _okay.xml, bg_depth_hw_control.xml, bg_hw_send.xml,
// res/values/dimens.xml (flash_card_height, abacus_side_button_size, tutorial_control_button_height,
// tutorial_panel_button_height), fragment_abacus.xml / fragment_blinding_lesson.xml.

// MARK: - Ölçüler ve renkler

enum AbakusDersOlcu {
    /// Kök ConstraintLayout dolgusu (abacus_fragment_root_padding).
    static let kokBosluk: CGFloat = 10
    /// quitButton (abacus_fragment_quit_size).
    static let cikisBoyu: CGFloat = 48
    /// Sayı kartı: flash_card_height, kenar boşlukları 24, çıkış düğmesinin altından 12.
    static let kartYuksekligi: CGFloat = 140
    static let kartYanBosluk: CGFloat = 24
    static let kartUstBosluk: CGFloat = 12
    /// bg_flash_card: köşe 22, altta 4 dp "dudak", çerçeve 2 dp.
    static let kartKose: CGFloat = 22
    static let kartDudak: CGFloat = 4
    static let kartCerceve: CGFloat = 2
    /// timerTextView: kartın altından (abacus_fragment_timer_margin_top).
    static let sureUstBosluk: CGFloat = 6
    /// Sağ düğme sütunu: abacus_side_button_size, aralık abacus_fragment_ask_button_margin_top.
    static let yanDugmeBoyu: CGFloat = 48
    static let yanDugmeAraligi: CGFloat = 4
    /// kontrolButton / startButton: tutorial_control_button_height (dudak dahil).
    static let anaDugmeYuksekligi: CGFloat = 48
    /// Sonuç panellerinin düğmesi: tutorial_panel_button_height.
    static let panelDugmeYuksekligi: CGFloat = 50
    /// Tahta ile abaküs arasında bırakılan en az boşluk (süre yazısı buraya düşer).
    static let enAzAra: CGFloat = 20
    static let enKucukOlcek: CGFloat = 0.5

    /// Kısa ekranda abaküsün küçültülme oranı (iOS'a özgü; Android sabit dp kullanır ve ekrana göre
    /// ölçeklemez). Yer yetiyorsa 1'dir.
    /// - Parameters:
    ///   - yukseklik: kök dolgunun (10 pt) içinde kalan alanın yüksekliği.
    ///   - sabit: ölçeklenmeyen parçaların (çıkış satırı, kart, boşluklar, düğme) toplam yüksekliği.
    static func abakusOlcegi(yukseklik: CGFloat, sabit: CGFloat) -> CGFloat {
        let abakusBoyu: CGFloat = AbakusGorunumu.boyut(basamakSayisi: 5).height
        let kalan: CGFloat = yukseklik - sabit
        if kalan >= abakusBoyu {
            return 1
        }
        let oran: CGFloat = kalan / abakusBoyu
        return min(max(oran, enKucukOlcek), 1)
    }
}

enum AbakusDersRenk {
    /// bg_flash_card: dudak ve çerçeve #37464F, yüz @color/bright_background_color.
    static let kartDudak: Color = AbakusRenk.renk(0x37464F)
    static let kartYuz: Color = Color("bright_background_color")
    /// fab_background: @color/side_button_background.
    static let yanDugme: Color = Color("side_button_background")
    /// bg_depth_tutorial_control (Başla / Kontrol Et): yüz #8FD14F, dudak #62A12E.
    static let kontrolYuz: Color = AbakusRenk.renk(0x8FD14F)
    static let kontrolDudak: Color = AbakusRenk.renk(0x62A12E)
    /// bg_depth_hw_control pasif hali (kör derste sayılar akarken): yüz @color/button_disabled, dudak #A6A6A6.
    static let kontrolPasifYuz: Color = Color("button_disabled")
    static let kontrolPasifDudak: Color = AbakusRenk.renk(0xA6A6A6)
    /// bg_depth_tutorial_continue (Devam et).
    static let devamYuz: Color = AbakusRenk.renk(0x8CC63F)
    static let devamDudak: Color = AbakusRenk.renk(0x62982A)
    /// bg_depth_tutorial_okay (Tekrar Dene / Devam Et, yanlış paneli).
    static let tamamYuz: Color = AbakusRenk.renk(0xFF4B4B)
    static let tamamDudak: Color = AbakusRenk.renk(0xD12F2F)
    /// bg_hw_send ("Sobi ile analiz et").
    static let analizYuz: Color = AbakusRenk.renk(0x1CB0F6)
    static let analizDudak: Color = AbakusRenk.renk(0x1899D6)
    static let yesilYazi: Color = AbakusRenk.renk(0x8CC63F)
    static let kirmizi: Color = AbakusRenk.renk(0xFF4B4B)
    static let panelZemin: Color = AbakusRenk.renk(0x222222)
}

// MARK: - Sayı kartı

/// bg_flash_card: altta 4 dp koyu "dudak", üstte 2 dp çerçeveli yüz (blackboardImage'in yerini aldı).
struct AbakusKartZemini: View {
    var body: some View {
        ZStack(alignment: .top) {
            RoundedRectangle(cornerRadius: AbakusDersOlcu.kartKose, style: .continuous)
                .fill(AbakusDersRenk.kartDudak)
                .padding(.top, AbakusDersOlcu.kartDudak)
            RoundedRectangle(cornerRadius: AbakusDersOlcu.kartKose, style: .continuous)
                .fill(AbakusDersRenk.kartYuz)
                .overlay(
                    RoundedRectangle(cornerRadius: AbakusDersOlcu.kartKose, style: .continuous)
                        .strokeBorder(AbakusDersRenk.kartDudak, lineWidth: AbakusDersOlcu.kartCerceve)
                )
                .padding(.bottom, AbakusDersOlcu.kartDudak)
        }
    }
}

// MARK: - Göçen düğme

/// Göçen (derinlikli) düğme: etiket + GocenDugmeStili. `yukseklik` dudak dahil toplam yüksekliktir.
struct AbakusGocenDugme: View {
    let metin: String
    let yaziBoyu: CGFloat
    let yaziRengi: Color
    let yuz: Color
    let dudak: Color
    let kose: CGFloat
    let yukseklik: CGFloat
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            Text(metin)
                .font(Tema.yazi(yaziBoyu))
                .foregroundColor(yaziRengi)
        }
        .buttonStyle(GocenDugmeStili(zemin: yuz, golge: dudak, koseYaricapi: kose))
        .frame(maxWidth: .infinity)
        .frame(height: yukseklik)
    }
}

// MARK: - Yan düğme

/// Sağ kenardaki yuvarlak düğme (fab_background, 48 dp).
struct AbakusYanDugme<Icerik: View>: View {
    let eylem: () -> Void
    let icerik: () -> Icerik

    init(eylem: @escaping () -> Void, @ViewBuilder icerik: @escaping () -> Icerik) {
        self.eylem = eylem
        self.icerik = icerik
    }

    var body: some View {
        Button(action: eylem) {
            ZStack {
                Circle().fill(AbakusDersRenk.yanDugme)
                icerik()
            }
            .frame(width: AbakusDersOlcu.yanDugmeBoyu, height: AbakusDersOlcu.yanDugmeBoyu)
            .contentShape(Circle())
        }
        .buttonStyle(.plain)
    }
}

/// abacusModeButton (lottie abacus_mode_ic_anim): ayar panelini açar / kapatır. Açılırken 0 → 25,
/// kapanırken 25 → 45. kareler oynar; ilk karede bekler.
struct AbakusAyarDugmesi: View {
    let acik: Bool
    let eylem: () -> Void

    @State private var oynadi: Bool = false

    var body: some View {
        AbakusYanDugme(eylem: eylem) {
            simge
        }
        .onChange(of: acik) { _ in
            oynadi = true
        }
    }

    private var simge: some View {
        let baslangic: Double = (oynadi && !acik) ? 25.0 : 0.0
        let bitis: Double? = oynadi ? (acik ? 25.0 : 45.0) : nil
        // padding 2 dp
        return LottieGorunumu(
            "abacus_mode_ic_anim", dongu: false, oynat: oynadi,
            baslangicKare: baslangic, bitisKare: bitis
        )
        .frame(width: 44, height: 44)
    }
}

// MARK: - Sobi ile analiz et

/// Ekranın "Sobi ile analiz et" için dışarıya verdiği istek (Android: MistakeReviewDialog.newInstance).
/// Pencere SobiAnaliz/SobiAnalizEkrani'dır (kural motoru ortak kodda: shared/.../sobianaliz/); onu
/// yönetici (Yonetim/UygulamaKokGorunumu) açar.
struct AbakusSobiAnalizIstegi {
    /// Sorudaki sayılar; çıkarılan sayı eksi işaretlidir (Android: toplama/çıkarma için
    /// `listOf(first, if (sign == "-") -second else second)`; kör derste flaş dizisi).
    let sayilar: [Int]
    /// Çocuğun verdiği (yanlış) cevap.
    let cevap: Int
    /// Abaküslü derste bu sorudaki abaküs değer kaydı (sıfırlama ara değerleri hariç); klavyeyle
    /// cevaplanan derste boş.
    let kayit: [Int]
    /// Flaş dizisinde her sayı göründüğünde kayda eklenen değerin sırası (yalnızca kör ders; dizi
    /// tam izlendiyse dolu).
    let isaretler: [Int]
    /// Android: lessonItem.title.
    let dersBasligi: String
    /// Analiz penceresi kapanınca çağrılır; sandık dersinde analiz süresince durdurulan süre bu çağrıyla
    /// kaldığı yerden devam eder (Android: MistakeReviewDialog.RESULT_CLOSED).
    let kapandi: () -> Void
}

/// "Sobi ile analiz et" düğmesinin dışarıya bağlantısı.
struct AbakusSobiAnalizBaglantisi {
    /// Android: `MistakeReviewDialog.canReview(numbers) && SobiAccess.isAllowed(context)`. Kararı ekranı
    /// açan taraf verir (yönetici: SobiAnalizModeli.analizYapilabilir); "hayır" ise düğme gizlenir.
    var yapilabilir: ([Int]) -> Bool
    /// Düğmeye basılınca çağrılır.
    var istendi: (AbakusSobiAnalizIstegi) -> Void

    init(yapilabilir: @escaping ([Int]) -> Bool, istendi: @escaping (AbakusSobiAnalizIstegi) -> Void) {
        self.yapilabilir = yapilabilir
        self.istendi = istendi
    }
}

/// incorrectSobiAnalyze: 44 dp yüksek, bg_hw_send göçen düğme (yüz #1CB0F6, dudak #1899D6, köşe 16),
/// iç boşluk başta 8 / sonda 14; 30 dp Sobi kafası + 15 sp yazı (arada 6).
struct AbakusSobiAnalizDugmesi: View {
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            HStack(spacing: 6) {
                // Android SobiHeadView (yalnızca kafa) iOS'ta yok; tam boy Sobi durağan karede.
                SobiMaskotGorunumu(emote: "GREET", animasyonlu: false)
                    .frame(width: 30, height: 30)
                Text(Metin.al("review_open_button"))
                    .font(Tema.yazi(15))
                    .foregroundColor(.white)
                    .lineLimit(1)
            }
            .padding(.leading, 8)
            .padding(.trailing, 14)
        }
        .buttonStyle(
            GocenDugmeStili(
                zemin: AbakusDersRenk.analizYuz, golge: AbakusDersRenk.analizDudak, koseYaricapi: 16
            )
        )
        .fixedSize(horizontal: true, vertical: false)
        .frame(height: 44)
    }
}

// MARK: - Sonuç paneli zemini

/// bottom_sheet_background: #222, yalnızca üst köşeler 16 dp yuvarlak; alt güvenli alana taşar (Android:
/// NavBarPanelExtension panelin gezinme çubuğunun arkasına uzanmasını sağlar).
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

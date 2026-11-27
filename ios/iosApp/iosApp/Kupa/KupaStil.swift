import SwiftUI
import UIKit

// Kupa klasörünün ortak görsel yardımcıları: sabit renkler, şekiller, çerçeveli sayı yazısı
// (Android BadgeRewardTextView), Lottie kabuğu ve Android layout'larında doğrudan
// yazılmış (strings.xml'de olmayan) metinler.

enum KupaStil {
    /// 0xRRGGBB biçimindeki sabit renk (layout XML'lerinde kaynak adı olmadan yazılmış renkler için).
    static func renk(_ rgb: UInt32, alfa: Double = 1.0) -> Color {
        let kirmizi: Double = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil: Double = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi: Double = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: alfa)
    }

    /// Soluk gri-mavi ikincil yazı (#93A5B3): kupa geçmişi ve rozet ekranlarında ortak.
    static let solukYazi: Color = KupaStil.renk(0x93A5B3)
    /// Kupa geçmişi ayırıcı ve ızgara çizgisi (#1A3040).
    static let izgaraCizgisi: Color = KupaStil.renk(0x1A3040)
    /// Kupa geçmişi grafik rengi (#8B7CD9).
    static let grafikRengi: Color = KupaStil.renk(0x8B7CD9)
    /// Sekme göstergesi (#1CB0F6).
    static let sekmeRengi: Color = KupaStil.renk(0x1CB0F6)
    /// BadgeRewardTextView varsayılan çerçeve rengi (0xFFFF8F00).
    static let sayiCercevesi: Color = KupaStil.renk(0xFF8F00)

    /// "Son kare" (Android progress = 1): sarmalayıcı sınır dışındaki kareyi son kareye kırpar.
    static let sonKare: Double = 9999

    /// Ortak kodun verdiği Android assets dosya adından `LottieGorunumu` adını üretir: sondaki
    /// ".json" atılır. Android'de iki dosyanın adı çift uzantılıdır ("silver_medal_anim.json.json");
    /// iOS paketine tek uzantıyla kopyalandıkları için (Lottie/silver_medal_anim.json) hepsi atılır.
    static func lottieAdi(_ dosya: String) -> String {
        let uzanti: String = ".json"
        var ad: String = dosya
        while ad.hasSuffix(uzanti) {
            ad = String(ad.dropLast(uzanti.count))
        }
        return ad
    }

    /// Android drawable adı → dolgu rengi (bg_record_rank_* oval şekilleri).
    static func siraRozetiRengi(_ drawableAdi: String) -> Color {
        switch drawableAdi {
        case "bg_record_rank_gold": return KupaStil.renk(0xFFE082)
        case "bg_record_rank_badge_purple": return KupaStil.renk(0xD1C4E9)
        case "bg_record_rank_badge_yellow": return KupaStil.renk(0xFFF59D)
        case "bg_record_rank_badge_blue": return KupaStil.renk(0x90CAF9)
        default: return KupaStil.renk(0x80CBC4)
        }
    }
}

/// Android layout metinleri. Güncel Android'de hepsi strings.xml'dedir; adlar aynıdır, `Metin.al` ile okunur
/// (dil değişince güncel kalsın diye hesaplanan özelliklerdir).
enum KupaMetin {
    // fragment_cup_history.xml
    static var guncelKupa: String { return Metin.al("fragment_cup_history_text") }
    static var tekNoktaIpucu: String { return Metin.al("fragment_cup_history_tv_cup_history_single_hint") }
    static var gecmisYok: String { return Metin.al("fragment_cup_history_tv_cup_history_empty") }
    // fragment_badge_detail.xml
    static var basarilar: String { return Metin.al("fragment_badge_detail_tv_badge_detail_title") }
    static var kisiselRekorlar: String { return Metin.al("fragment_badge_detail_tv_personal_records_title") }
    static var rozetler: String { return Metin.al("fragment_badge_detail_tv_awards_title") }
    // fragment_badge.xml (parça paneli başlıkları; madalya ve kupa için ayrı anahtarlar)
    static func uniteBasligi(kupa: Bool) -> String {
        return Metin.al(kupa ? "fragment_badge_tv_badge_piece_header_unit_cup" : "fragment_badge_tv_badge_piece_header_unit_medal")
    }
    static func sezonBasligi(kupa: Bool) -> String {
        return Metin.al(kupa ? "fragment_badge_tv_badge_piece_header_season_cup" : "fragment_badge_tv_badge_piece_header_season_medal")
    }
}

/// Kısa süreli bilgi balonu (Android Toast: ortalanmış kapsül; alt kenarda). Kupa Yolu ekranlarının
/// "Bu sandığı zaten aldın" / "X kupaya ulaşınca açılır" uyarıları için. Gösterip gizlemeyi çağıran yapar.
struct KupaToast: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(Tema.yazi(14, kalin: false))
            .foregroundColor(Color.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Capsule().fill(KupaStil.renk(0x323232)))
            .padding(.horizontal, 24)
            .padding(.bottom, 48)
            .allowsHitTesting(false)
    }
}

/// Kilitli karta dokununca çıkan uyarı (Android LockedHint: Snackbar, alt kenarda tam genişlik,
/// 4 köşe yuvarlaklığı, en çok 5 satır).
struct KupaKilitIpucu: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(Tema.yazi(14, kalin: false))
            .foregroundColor(Color.white)
            .lineLimit(5)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(RoundedRectangle(cornerRadius: 4).fill(KupaStil.renk(0x323232)))
            .padding(8)
            .allowsHitTesting(false)
    }
}

/// Yalnızca üst köşeleri yuvarlatılmış dikdörtgen (bottom_sheet_background.xml).
struct KupaUstYuvarlakSekil: Shape {
    let yaricap: CGFloat

    func path(in rect: CGRect) -> Path {
        let yol = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: [.topLeft, .topRight],
            cornerRadii: CGSize(width: yaricap, height: yaricap)
        )
        return Path(yol.cgPath)
    }
}

/// Android BadgeRewardTextView: kalın çerçeveli (5dp), içi dolu sayı yazısı.
struct KupaCerceveliYazi: View {
    let metin: String
    let boyut: CGFloat
    let dolgu: Color
    let cerceve: Color

    /// Çerçeve kalınlığının yarısı (Android: 5dp çizgi, yazının iki yanına 2.5dp taşar).
    private let tasma: CGFloat = 2.5

    private var kaymalar: [CGSize] {
        let d: CGFloat = tasma
        let c: CGFloat = tasma * 0.7071
        return [
            CGSize(width: d, height: 0), CGSize(width: -d, height: 0),
            CGSize(width: 0, height: d), CGSize(width: 0, height: -d),
            CGSize(width: c, height: c), CGSize(width: -c, height: c),
            CGSize(width: c, height: -c), CGSize(width: -c, height: -c)
        ]
    }

    var body: some View {
        ZStack {
            ForEach(Array(kaymalar.enumerated()), id: \.offset) { kayma in
                Text(metin)
                    .font(Tema.yazi(boyut))
                    .foregroundColor(cerceve)
                    .offset(x: kayma.element.width, y: kayma.element.height)
            }
            Text(metin)
                .font(Tema.yazi(boyut))
                .foregroundColor(dolgu)
        }
        .fixedSize()
    }
}

/// Android `LottieAnimationView` karşılığı: ortak `LottieGorunumu` sarmalayıcısının Kupa
/// klasörüne özgü ince kabuğu. Dosya adı ortak koddan uzantılı gelir ("dinosaur_anim.json");
/// uzantı burada atılır. Boyut çağıran tarafta `.frame` ile verilir.
///
/// Android'de hangi dosya nerede, nasıl oynar (hepsi app/src/main/assets):
/// - Kupa Yolu kartları (70dp) ve zorluk penceresi (120dp): hayvan animasyonu, döngülü.
/// - Kupa geçmişi (56dp): seçilen kupanın hayvan animasyonu, döngülü.
/// - Rozet listesi: taban `daily_tasks_complite_badge2` son karede, üst animasyon tutma karesinde sabit.
/// - Tek rozet: taban bir kez oynar; üst animasyon bir kez oynar (madalyalarda döngülü).
/// - Parça satırları (40dp): `cup_google_anim` 135. karede, madalya ilk karede sabit.
///
/// Renk filtresi sarmalayıcının sözleşmesinde yoktur; Android'in düz renk filtresi
/// (PorterDuff SRC_ATOP — kilitli kart/rozet) SwiftUI renk efektleriyle karşılanır:
/// önce bütün renkler beyaza çekilir, sonra istenen renkle çarpılır (saydamlık korunur).
struct KupaAnimasyonGorunumu: View {
    /// Android assets dosya adı (ör. "dinosaur_anim.json").
    let dosya: String
    /// Düz renk filtresi (kilitli kart/rozet); nil = özgün renkler.
    var duzRenk: Color? = nil
    /// Sürekli döner mi (Android lottie_loop / repeatCount = -1).
    var dongu: Bool = false
    /// false ise ilk karede durur (Android progress = 0 + pauseAnimation).
    var oynat: Bool = true
    /// Tek seferlik oynatma bitince çağrılır.
    var bitti: (() -> Void)? = nil
    /// nil değilse animasyon oynatılmaz, bu karede sabit durur (Android progress / holdFrame +
    /// pauseAnimation). Animasyonun sınırı dışındaki kare en yakın sınıra kırpılır: 9999 = son kare.
    var sabitKare: Double? = nil

    var body: some View {
        let parlaklik: Double = duzRenk == nil ? 0.0 : 1.0
        let carpim: Color = duzRenk ?? Color.white
        let oynasin: Bool = sabitKare == nil ? oynat : false
        return LottieGorunumu(
            KupaStil.lottieAdi(dosya),
            dongu: dongu,
            hiz: 1.0,
            oynat: oynasin,
            bitti: bitti,
            baslangicKare: sabitKare,
            bitisKare: nil
        )
            .brightness(parlaklik)
            .colorMultiply(carpim)
            .accessibilityHidden(true)
    }
}

/// Açık rozetin tabanına uygulanan kademe tonu.
///
/// Android (BadgeDetailFragment / BadgeFragment.applyUnlockedLevelToneToBase): BRONZE, SILVER, RED
/// → `badge_tone_*_filter` rengiyle PorterDuff MULTIPLY (burada `colorMultiply`, birebir);
/// PURPLE → `badge_tone_purple_filter` ile PorterDuff OVERLAY. SwiftUI'da görünüm başına OVERLAY
/// renk filtresi yoktur; turuncu tabanın sarıya kayması ton döndürmeyle YAKLAŞIK verilir.
/// ORIGINAL / boş → filtre yok.
struct KupaRozetTonu: ViewModifier {
    let ton: String

    private var carpim: Color {
        switch ton {
        case "BRONZE": return Color("badge_tone_bronze_filter")
        case "SILVER": return Color("badge_tone_silver_filter")
        case "RED": return Color("badge_tone_red_filter")
        default: return Color.white
        }
    }

    private var tonAcisi: Double {
        return ton == "PURPLE" ? 22.0 : 0.0
    }

    func body(content: Content) -> some View {
        content
            .hueRotation(Angle(degrees: tonAcisi))
            .colorMultiply(carpim)
    }
}

/// Üst çubuktaki geri düğmesi (ic_back_arrow, renklendirilmiş).
struct KupaGeriDugmesi: View {
    let boyut: CGFloat
    let icBosluk: CGFloat
    let renk: Color
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            Image("ic_back_arrow")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(renk)
                .padding(icBosluk)
                .frame(width: boyut, height: boyut)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

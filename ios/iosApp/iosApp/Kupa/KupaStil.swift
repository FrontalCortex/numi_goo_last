import SwiftUI
import UIKit

// Kupa klasörünün ortak görsel yardımcıları: sabit renkler, şekiller, çerçeveli sayı yazısı
// (Android BadgeRewardTextView), Lottie yer tutucusu ve Android layout'larında doğrudan
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

/// Android layout'larında sabit yazılmış metinler (strings.xml'de karşılığı yok).
enum KupaMetin {
    // panel_cup_path.xml / panel_cup_difficulty.xml metinleri ortak koddan (CupPathLogic) gelir.
    // fragment_cup_history.xml
    static let guncelKupa = "Güncel Kupa"
    static let tekNoktaIpucu = "Tek veri noktası var — karşılaştırma için birkaç gün sonra tekrar bak"
    static let gecmisYok = "Henüz geçmiş veri yok"
    // fragment_badge_detail.xml
    static let basarilar = "Başarılar"
    static let kisiselRekorlar = "Kişisel Rekorlar"
    static let rozetler = "Rozetler"
    // fragment_badge.xml
    static let unite = "Ünite"
    static let sezon = "Sezon"
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

/// Lottie animasyonlarının yer tutucusu.
///
/// Android bu ekranlarda Lottie kullanır (assets/*.json). iOS projesinde henüz Lottie
/// kütüphanesi ve animasyon dosyaları yok; o gelene kadar dosya adına karşılık bir SF Symbols
/// simgesi çizilir. Lottie eklendiğinde yalnızca bu görünümün gövdesi değişir: dosya adı,
/// tutma karesi ve renk filtresi çağıranlardan zaten geliyor.
struct KupaAnimasyonGorunumu: View {
    /// Android assets dosya adı (ör. "dinosaur_anim.json").
    let dosya: String
    /// Düz renk filtresi (kilitli kart/rozet); nil = özgün renkler.
    var duzRenk: Color? = nil
    /// Animasyon oynuyor mu (yer tutucuda yalnızca hafif nabız).
    var oynuyor: Bool = false

    @State private var nabiz: Bool = false

    private var simge: String {
        switch dosya {
        case "dinosaur_anim.json": return "lizard.fill"
        case "crocodile_anim.json": return "fossil.shell.fill"
        case "goat_anim.json": return "pawprint.fill"
        case "eagle_anim.json": return "bird.fill"
        case "fly_anim.json": return "ladybug.fill"
        case "turtle_anim.json": return "tortoise.fill"
        case "cup_google_anim.json": return "trophy.fill"
        case "karate_anim.json": return "figure.martial.arts"
        case "gold_medal_anim.json", "silver_medal_anim.json.json", "bronze_medal_anim.json.json": return "medal.fill"
        case "dart_anim.json": return "target"
        case "rocket_badge_anim2.json": return "paperplane.fill"
        case "bowling_anim.json": return "figure.bowling"
        case "fishing_pole_anim.json": return "fish.fill"
        case "golf_anim.json": return "figure.golf"
        case "tornado_anim.json": return "tornado"
        case "volcano_anim.json": return "flame.fill"
        case "daily_tasks_complite_badge.json", "daily_tasks_complite_badge2.json": return "seal.fill"
        default: return "star.fill"
        }
    }

    private var ozgunRenk: Color {
        switch dosya {
        case "gold_medal_anim.json", "cup_google_anim.json": return KupaStil.renk(0xFFD54F)
        case "silver_medal_anim.json.json": return KupaStil.renk(0xCFD8DC)
        case "bronze_medal_anim.json.json": return KupaStil.renk(0xCD7F32)
        case "daily_tasks_complite_badge.json", "daily_tasks_complite_badge2.json": return KupaStil.renk(0xFFB300)
        default: return Color.white
        }
    }

    var body: some View {
        Image(systemName: simge)
            .resizable()
            .scaledToFit()
            .foregroundColor(duzRenk ?? ozgunRenk)
            .scaleEffect(nabiz ? 1.06 : 1.0)
            .onAppear {
                guard oynuyor else { return }
                withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) {
                    nabiz = true
                }
            }
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

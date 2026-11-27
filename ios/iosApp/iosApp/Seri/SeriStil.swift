import Shared
import SwiftUI

// Seri ekranlarının ortak parçaları: renkler, kart zemini, ilerleme çubuğu, seçenek satırı ve pencere.
// Android: res/drawable/bg_streak_*.xml (şekiller SwiftUI'da çizilir), StreakViews.kt.

enum SeriStil {
    static func renk(_ rgb: UInt32, alfa: Double = 1.0) -> Color {
        let r: Double = Double((rgb >> 16) & 0xFF) / 255.0
        let g: Double = Double((rgb >> 8) & 0xFF) / 255.0
        let b: Double = Double(rgb & 0xFF) / 255.0
        return Color(red: r, green: g, blue: b).opacity(alfa)
    }

    /// StreakFragment.COLOR_ACCENT / StreakViews.COLOR_ACCENT.
    static let turuncu: Color = renk(0xFF9800)
    /// StreakFragment.COLOR_DEAD: seri kırık.
    static let sonuk: Color = renk(0x78909C)
    /// StreakFragment.COLOR_FREEZE: "Hazır".
    static let buz: Color = renk(0x4FC3F7)
    /// StreakViews.COLOR_MUTED.
    static let soluk: Color = renk(0x93A5B3)
    /// StreakViews.COLOR_GOLD.
    static let altin: Color = renk(0xFFC107)
    /// bg_streak_claim / "Topla" yazısı #1B2B31.
    static let toplaYazi: Color = renk(0x1B2B31)

    /// bg_streak_option: #203137, 2dp #2E4149, köşe 14dp.
    static let secenekZemini: Color = renk(0x203137)
    static let secenekCizgisi: Color = renk(0x2E4149)
    /// bg_streak_option_selected: #2A3B42, 3dp #FF9800.
    static let seciliZemin: Color = renk(0x2A3B42)
    /// bg_streak_dialog: #18272D, 2dp #2E4149, köşe 20dp.
    static let pencereZemini: Color = renk(0x18272D)
    /// bg_streak_reward_badge: #16252A, 1dp #3A525B, köşe 12dp.
    static let rozetZemini: Color = renk(0x16252A)
    static let rozetCizgisi: Color = renk(0x3A525B)
    /// bg_streak_day_empty #2E4149, bg_streak_day_frozen #29B6F6.
    static let gunBos: Color = renk(0x2E4149)
    static let gunDonmus: Color = renk(0x29B6F6)
    /// bg_streak_celebration: soldan sağa #E65100 → #FF9800, köşe 16dp.
    static let kutlamaBas: Color = renk(0xE65100)
    /// streakCelebrationSub #FFE0B2.
    static let kutlamaAlt: Color = renk(0xFFE0B2)
}

/// bg_streak_option / bg_streak_option_selected zemini.
struct SeriKartZemini: ViewModifier {
    var secili: Bool = false

    func body(content: Content) -> some View {
        let zemin: Color = secili ? SeriStil.seciliZemin : SeriStil.secenekZemini
        let cizgi: Color = secili ? SeriStil.turuncu : SeriStil.secenekCizgisi
        let kalinlik: CGFloat = secili ? 3 : 2
        return content
            .background(RoundedRectangle(cornerRadius: 14).fill(zemin))
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(cizgi, lineWidth: kalinlik))
    }
}

extension View {
    func seriKart(secili: Bool = false) -> some View {
        return modifier(SeriKartZemini(secili: secili))
    }
}

// MARK: - İlerleme çubuğu

/// Günlük soru çubuğu (daily_question_progress_*, applyDailyQuestionProgressOverlayNow): 28dp yükseklik,
/// zemin daily_question_progress_track (16dp köşe), dolgu turuncu (tamamlanınca altın), üstte 6dp
/// parlama (üstten 8dp, soldan 14dp, 3dp köşe), ortada 13sp kalın beyaz yazı.
struct SeriIlerlemeCubugu: View {
    let yuzde: Float
    let tamam: Bool
    let yazi: String

    private static let yukseklik: CGFloat = 28

    var body: some View {
        GeometryReader { alan in
            cubuk(genislik: alan.size.width)
        }
        .frame(height: SeriIlerlemeCubugu.yukseklik)
    }

    private func cubuk(genislik: CGFloat) -> some View {
        let iz: Int32 = Int32(genislik.rounded(.down))
        let katman: MissionProgressOverlay? = MissionUiLogic.shared.dailyQuestionProgressOverlay(
            trackWidth: iz,
            percent: yuzde,
            complete: tamam,
            startInset: 14,
            endGap: 10
        )
        let boy: CGFloat = SeriIlerlemeCubugu.yukseklik
        return ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 16)
                .fill(Color("daily_question_progress_track"))
                .frame(width: genislik, height: boy)
            if let k = katman, k.fillVisible {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(SeriIlerlemeCubugu.renkAdi(k.fillDrawable)))
                    .frame(width: CGFloat(k.fillWidth), height: boy)
            }
            if let k = katman, k.shineVisible {
                RoundedRectangle(cornerRadius: 3)
                    .fill(Color(SeriIlerlemeCubugu.renkAdi(k.shineDrawable)))
                    .frame(width: CGFloat(k.shineWidth), height: 6)
                    .offset(x: 14, y: 8)
            }
            Text(yazi)
                .font(Tema.yazi(13))
                .foregroundColor(Color.white)
                // Android'den bilinçli sapma (okunurluk): Android'de etiket gölgesiz beyaz;
                // iOS'ta turuncu dolgu ve parlama şeridi üstünde okunmadığı için koyu gölge var.
                .shadow(color: Color.black.opacity(0.9), radius: 2, x: 0, y: 1)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .frame(width: genislik, height: boy, alignment: .center)
        }
    }

    /// Şekil drawable'larının düz rengi (GunlukSoru/GunlukSoruVeri.renkAdi ile aynı eşleme).
    static func renkAdi(_ sekil: String) -> String {
        switch sekil {
        case "daily_question_progress_fill_complete": return "missions_progress_complete"
        case "daily_question_progress_shine": return "daily_question_progress_shine"
        case "daily_question_progress_shine_complete": return "missions_progress_gold_shine"
        default: return "daily_question_progress_fill"
        }
    }
}

// MARK: - Seçenek satırı ve pencere

/// StreakViews.buildOptionRows satırı: solda 17sp kalın beyaz, sağda 15sp; iç boşluk 16dp.
struct SeriSecenekSatiri: View {
    let satir: StreakOptionRowUi
    let sagRengi: Color
    let secildi: (Int) -> Void

    var body: some View {
        let deger: Int = Int(satir.value)
        return Button {
            secildi(deger)
        } label: {
            HStack(spacing: 0) {
                Text(SeriMetin.sol(satir))
                    .font(Tema.yazi(17))
                    .foregroundColor(Color.white)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(SeriMetin.sag(satir))
                    .font(Tema.yazi(15))
                    .foregroundColor(sagRengi)
                    .lineLimit(1)
            }
            .padding(16)
            .seriKart(secili: satir.selected)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Satır listesi (aralarında 12dp).
struct SeriSecenekListesi: View {
    let satirlar: [StreakOptionRowUi]
    let sagRengi: Color
    let secildi: (Int) -> Void

    var body: some View {
        VStack(spacing: 12) {
            ForEach(Array(satirlar.enumerated()), id: \.offset) { oge in
                SeriSecenekSatiri(satir: oge.element, sagRengi: sagRengi, secildi: secildi)
            }
        }
    }
}

/// dialog_streak_options.xml: karartma üstünde, yanlardan 24dp; zemin bg_streak_dialog, iç boşluk 20dp;
/// başlık 18sp kalın beyaz, alt başlık 13sp soluk (6dp), içerik 16dp altında. Dışına dokununca kapanır
/// (AlertDialog varsayılanı).
struct SeriPencere<Icerik: View>: View {
    let baslik: String
    let altBaslik: String
    let kapat: () -> Void
    let icerik: Icerik

    init(baslik: String, altBaslik: String, kapat: @escaping () -> Void, @ViewBuilder icerik: () -> Icerik) {
        self.baslik = baslik
        self.altBaslik = altBaslik
        self.kapat = kapat
        self.icerik = icerik()
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.6)
                .ignoresSafeArea()
                .onTapGesture { kapat() }
            pencere
                .padding(.horizontal, 24)
        }
    }

    private var pencere: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(baslik)
                .font(Tema.yazi(18))
                .foregroundColor(Color.white)
            Text(altBaslik)
                .font(Tema.yazi(13))
                .foregroundColor(SeriStil.soluk)
                .padding(.top, 6)
            icerik
                .padding(.top, 16)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 20).fill(SeriStil.pencereZemini))
        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(SeriStil.secenekCizgisi, lineWidth: 2))
    }
}

/// StreakViews.showHourPicker: 24 saat, 4 sütun, hücre 44dp, aralık 8dp; 06:00'dan başlar.
struct SeriSaatIzgarasi: View {
    let secili: Int
    let secildi: (Int) -> Void

    var body: some View {
        let saatler: [Int] = StreakRules.shared.hourGrid().map { Int($0.int32Value) }
        let satirSayisi: Int = (saatler.count + 3) / 4
        return VStack(spacing: 8) {
            ForEach(0..<satirSayisi, id: \.self) { satir in
                HStack(spacing: 8) {
                    ForEach(0..<4, id: \.self) { sutun in
                        hucre(saatler, satir * 4 + sutun)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func hucre(_ saatler: [Int], _ sira: Int) -> some View {
        if sira < saatler.count {
            let saat: Int = saatler[sira]
            Button {
                secildi(saat)
            } label: {
                Text(StreakRules.shared.hourText(hour: Int32(saat)))
                    .font(Tema.yazi(15))
                    .foregroundColor(Color.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .seriKart(secili: saat == secili)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        } else {
            Color.clear.frame(maxWidth: .infinity).frame(height: 44)
        }
    }
}

// MARK: - Bildirim (Android Toast)

struct SeriBildirimGorunumu: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(Tema.yazi(14, kalin: false))
            .foregroundColor(Color.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Capsule().fill(SeriStil.renk(0x323232)))
            .padding(.horizontal, 24)
            .padding(.bottom, 48)
    }
}

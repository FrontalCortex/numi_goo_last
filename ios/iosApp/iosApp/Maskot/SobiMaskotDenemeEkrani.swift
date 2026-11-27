import SwiftUI

// Maskot deneme ekranı (hata ayıklama): ekran galerisinde `-ekran maskot`, `maskot-2`,
// `maskot-3` ile açılır. Bütün hâller durağan (animasyonlu: false) olarak ad etiketleriyle
// 3 sütunlu ızgarada; ekran görüntüsüne sığsın diye her sayfada 18 hâl (6 satır).
// Android karşılığı: MascotPlaygroundFragment (oyun alanı).
struct SobiMaskotDenemeEkrani: View {
    /// 1'den başlayan sayfa numarası.
    let sayfa: Int

    static let sayfaBasina: Int = 18

    init(sayfa: Int = 1) {
        self.sayfa = sayfa
    }

    private var haller: [SobiMaskotHal] {
        let tum: [SobiMaskotHal] = SobiMaskotHal.allCases
        let bas: Int = max(0, (sayfa - 1) * SobiMaskotDenemeEkrani.sayfaBasina)
        if bas >= tum.count { return [] }
        let son: Int = min(tum.count, bas + SobiMaskotDenemeEkrani.sayfaBasina)
        return Array(tum[bas..<son])
    }

    private let sutunlar: [GridItem] = [
        GridItem(.flexible(), spacing: 6),
        GridItem(.flexible(), spacing: 6),
        GridItem(.flexible(), spacing: 6),
    ]

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            LazyVGrid(columns: sutunlar, spacing: 4) {
                ForEach(haller, id: \.rawValue) { hal in
                    SobiMaskotDenemeHucresi(hal: hal)
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
        }
    }
}

struct SobiMaskotDenemeHucresi: View {
    let hal: SobiMaskotHal

    var body: some View {
        VStack(spacing: 0) {
            SobiMaskotGorunumu(emote: hal.rawValue, animasyonlu: false)
                .frame(width: 96, height: 96)
            Text(verbatim: hal.rawValue)
                .font(Font.system(size: 9, weight: .bold))
                .foregroundColor(Color.white)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            Text(verbatim: hal.etiket)
                .font(Font.system(size: 8, weight: .regular))
                .foregroundColor(Color.white.opacity(0.6))
                .lineLimit(1)
                .minimumScaleFactor(0.6)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 2)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color.white.opacity(0.05))
        )
    }
}

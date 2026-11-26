import SwiftUI

// Kısım (part) seçim ekranı.
// Android karşılıkları: fragment_part_selection.xml (üç başlık + üç yatay liste),
// item_part_card.xml (160x180 kart), PartSelectionFragment + PartCardAdapter.
// Kartların açık/kapalı durumu, başlığı ve rengi ortak koddan (PartSelectionLogic) gelir.

struct HaritaKisimSecimi: View {
    @ObservedObject var model: HaritaModeli

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(model.kisimSatirlari) { satir in
                    HaritaKisimSatirGorunumu(
                        satir: satir,
                        sonSatir: satir.id == model.kisimSatirlari.count - 1,
                        sec: { kart in model.kisimSec(kart) }
                    )
                }
            }
            .padding(.top, 16)
            .padding(.bottom, 32)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Color("background_color"))
    }
}

/// Bir kategori: başlık ("Toplama") + yatay kaydırılan kartlar.
struct HaritaKisimSatirGorunumu: View {
    let satir: HaritaKisimSatiri
    let sonSatir: Bool
    let sec: (HaritaKisimKarti) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(satir.baslik)
                .font(HaritaStil.yazi(20))
                .foregroundColor(.white)
                .padding(.leading, 20)
                .padding(.bottom, 12)
            kartlar
                .padding(.bottom, sonSatir ? 0 : 28)
        }
    }

    private var kartlar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(satir.kartlar) { kart in
                    HaritaKisimKartGorunumu(kart: kart, dokun: { sec(kart) })
                }
            }
            // Android: paddingStart 20dp, paddingEnd 8dp + son kartın 12dp sağ boşluğu.
            .padding(.leading, 20)
            .padding(.trailing, 20)
            // Kart gölgesi kırpılmasın.
            .padding(.vertical, 6)
        }
        .padding(.vertical, -6)
    }
}

/// item_part_card.xml: üstte iki satıra kadar başlık, altta 100pt'lik animasyon alanı.
struct HaritaKisimKartGorunumu: View {
    let kart: HaritaKisimKarti
    let dokun: () -> Void

    var body: some View {
        Button(action: dokun) {
            icerik
        }
        .buttonStyle(.plain)
    }

    private var icerik: some View {
        VStack(spacing: 0) {
            Text(kart.baslik)
                .font(HaritaStil.yazi(14))
                .foregroundColor(Color(kart.yaziRengi))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .frame(maxWidth: .infinity)
                .padding(.top, 5)
            Spacer(minLength: 0)
            animasyonAlani
        }
        .padding(8)
        .frame(width: 160, height: 180)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color(kart.kartRengi)))
        .contentShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: Color.black.opacity(0.35), radius: 4, x: 0, y: 2)
    }

    /// Android'de burada Lottie animasyonu oynar (kart.animasyon). lottie-ios henüz projede yok;
    /// yer boş bırakılır ki kart ölçüleri Android ile aynı kalsın.
    private var animasyonAlani: some View {
        Color.clear
            .frame(height: 100)
            .frame(maxWidth: .infinity)
            .padding(.bottom, 4)
    }
}

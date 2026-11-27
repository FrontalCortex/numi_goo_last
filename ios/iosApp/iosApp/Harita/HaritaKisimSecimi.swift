import Combine
import SwiftUI

// Kısım (part) seçim ekranı.
// Android karşılıkları: fragment_part_selection.xml (üç başlık + üç yatay liste),
// item_part_card.xml (160x180 kart), PartSelectionFragment + PartCardAdapter.
// Kartların açık/kapalı durumu, kilit mesajı ve rengi ortak koddan (PartSelectionLogic) gelir:
// 2–6. kartların kapısı önceki bölümün son ünite maratonu, onaylı öğretmende hepsi açık.
// Kilitli karta dokununca mesaj Snackbar olarak (Android: LockedHint) HaritaEkrani'nda çıkar.

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
            // fragment_part_selection.xml: paddingBottom 88dp (Sobi testi düğmesinin altından son kartlar
            // kaydırılıp çıkarılabilsin diye; düğme iOS'ta henüz yok, boşluk Android'le aynı tutuldu).
            .padding(.bottom, 88)
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
            HaritaKisimAnimasyonu(kart: kart)
                .padding(.bottom, 4)
        }
        .padding(8)
        .frame(width: 160, height: 180)
        .background(zemin)
        .contentShape(RoundedRectangle(cornerRadius: 16))
    }

    /// Gölge yalnızca zemin şekline verilir (cardElevation 4dp). Kabın tamamına verilirse SwiftUI
    /// gölgeyi her alt görünüme ayrı ayrı uygular ve başlık yazısı da gölgelenir.
    private var zemin: some View {
        RoundedRectangle(cornerRadius: 16)
            .fill(Color(kart.kartRengi))
            .shadow(color: Color.black.opacity(0.35), radius: 4, x: 0, y: 2)
    }
}

/// item_part_card.xml: partCardLottie (genişlik kart içi, yükseklik 100dp, fitCenter).
/// Android (PartCardAdapter.bind):
///   - kart açıksa animasyon hemen ve ardından her 10 saniyede bir, döngüsüz oynar
///     (kısım 2'de repeatCount = 2 → her seferinde art arda 3 kez);
///   - kart kapalıysa ilk karede durur ve lesson_locked rengiyle boyanır (SRC_ATOP).
struct HaritaKisimAnimasyonu: View {
    let kart: HaritaKisimKarti

    /// Her 10 saniyede bir artar; görünüm kimliği değişince animasyon baştan oynar.
    @State private var tur: Int = 0
    /// Bu turda yapılan ek tekrar sayısı (yalnızca kısım 2'de 0'dan büyük olur).
    @State private var tekrar: Int = 0

    /// Android: itemView.postDelayed(this, 10000L).
    private let saat = Timer.publish(every: 10, on: .main, in: .common).autoconnect()

    var body: some View {
        icerik
            .frame(height: 100)
            .frame(maxWidth: .infinity)
            // Dokunuşu kart düğmesi alır; animasyon görünümü yutmasın.
            .allowsHitTesting(false)
    }

    @ViewBuilder
    private var icerik: some View {
        if kart.gorselEtkin {
            oynayan
        } else {
            duran
        }
    }

    private var oynayan: some View {
        let kimlik: Int = tur * 100 + tekrar
        return LottieGorunumu(kart.animasyon, dongu: false, hiz: 1.0, oynat: true, bitti: { oynatmaBitti() })
            .id(kimlik)
            .onReceive(saat) { _ in
                tekrar = 0
                tur += 1
            }
    }

    /// Android burada bütün katmanları düz lesson_locked rengine boyar. SwiftUI'da aynı sonucu
    /// güvenle veren bir yol olmadığı için renk doygunluğu sıfırlanıp kilit rengiyle çarpılır
    /// (silüet aynı tonda kalır, içindeki açık-koyu farkı hafifçe seçilir).
    private var duran: some View {
        LottieGorunumu(kart.animasyon, dongu: false, hiz: 1.0, oynat: false, bitti: nil)
            .saturation(0)
            .colorMultiply(Color("lesson_locked"))
    }

    private func oynatmaBitti() {
        if tekrar < kart.animasyonTekrari {
            tekrar += 1
        }
    }
}

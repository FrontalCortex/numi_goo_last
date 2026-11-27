import SwiftUI

// Lottie deneme ekranı (hata ayıklama): ekran galerisinde `-ekran lottie` ile açılır.
// Amaç, Codemagic ekran görüntüsünde Lottie paketinin bağlandığını, dosyaların pakete girdiğini
// ve `LottieGorunumu` sarmalayıcısının çalıştığını görmek:
// - döngülü ve tek seferlik oynatma,
// - `bitti` geri çağrısı (tek seferlik olanların altında "bitti" yazar),
// - `oynat: false` (ilk karede bekler),
// - pakette olmayan ad (çökmez, boş çizilir),
// - kare parametreleri: sabit kare, son kareye kırpma, aralık oynatma.

/// Izgaradaki tek animasyonun tanımı.
struct LottieDenemeOgesi: Identifiable {
    let ad: String
    let dongu: Bool
    let oynat: Bool
    let aciklama: String
    var baslangicKare: Double? = nil
    var bitisKare: Double? = nil

    var id: String { return ad }
}

struct LottieDenemeEkrani: View {
    private let ogeler: [LottieDenemeOgesi] = [
        LottieDenemeOgesi(ad: "confetti_anim", dongu: false, oynat: true, aciklama: "bir kez"),
        LottieDenemeOgesi(ad: "gold_medal_anim", dongu: false, oynat: true, aciklama: "bir kez"),
        LottieDenemeOgesi(ad: "fire_anim", dongu: true, oynat: true, aciklama: "döngü"),
        LottieDenemeOgesi(ad: "rocket_anim", dongu: true, oynat: true, aciklama: "döngü"),
        LottieDenemeOgesi(ad: "lesson_hint_ic", dongu: true, oynat: true, aciklama: "döngü"),
        LottieDenemeOgesi(ad: "reset", dongu: true, oynat: true, aciklama: "döngü"),
        LottieDenemeOgesi(ad: "star", dongu: false, oynat: false, aciklama: "oynat: false"),
        LottieDenemeOgesi(ad: "olmayan_animasyon", dongu: false, oynat: true, aciklama: "dosya yok"),
        // Kare parametreleri: sabit kare, son kareye kırpma (9999) ve aralık oynatma.
        LottieDenemeOgesi(
            ad: "broken_heart_anim", dongu: false, oynat: false, aciklama: "sabit kare 116",
            baslangicKare: 116
        ),
        LottieDenemeOgesi(
            ad: "dart_anim", dongu: false, oynat: false, aciklama: "son kare (9999)",
            baslangicKare: 9999
        ),
        LottieDenemeOgesi(
            ad: "abacus_mode_ic_anim", dongu: false, oynat: true, aciklama: "0 → 25",
            baslangicKare: 0, bitisKare: 25
        ),
    ]

    private let sutunlar: [GridItem] = [
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8),
    ]

    /// `bitti` geri çağrısı gelen animasyonların adları.
    @State private var bitenler: Set<String> = []

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            LazyVGrid(columns: sutunlar, spacing: 12) {
                ForEach(ogeler) { oge in
                    LottieDenemeHucresi(
                        oge: oge,
                        bittiMi: bitenler.contains(oge.ad),
                        bitti: { _ = bitenler.insert(oge.ad) }
                    )
                }
            }
            .padding(16)
        }
    }
}

struct LottieDenemeHucresi: View {
    let oge: LottieDenemeOgesi
    let bittiMi: Bool
    let bitti: () -> Void

    private var durumMetni: String {
        return bittiMi ? oge.aciklama + " · bitti" : oge.aciklama
    }

    var body: some View {
        VStack(spacing: 4) {
            LottieGorunumu(
                oge.ad,
                dongu: oge.dongu,
                hiz: 1.0,
                oynat: oge.oynat,
                bitti: bitti,
                baslangicKare: oge.baslangicKare,
                bitisKare: oge.bitisKare
            )
                .frame(width: 96, height: 96)
                // Çerçeve, görünümün `.frame` ile verilen alana uyduğunu gösterir.
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.white.opacity(0.25), lineWidth: 1)
                )
            Text(oge.ad)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(Color.white)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(durumMetni)
                .font(.system(size: 11, weight: .regular))
                .foregroundColor(bittiMi ? Color.green : Color.white.opacity(0.6))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }
}

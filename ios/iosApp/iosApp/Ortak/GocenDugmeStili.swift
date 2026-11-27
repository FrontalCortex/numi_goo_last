import SwiftUI

// Göçen (derinlikli) düğme: altta koyu bir "dudak" olan, basılınca yüzü dudak kalınlığı kadar
// aşağı inen düğme (Duolingo tarzı).
//
// Android kaynakları:
//   res/drawable/bg_depth_result_continue.xml (zemin #8CC63F, dudak #62A12E, köşe 15dp, dudak 4dp;
//     ders sonu ekranlarının Devam / Çıkış düğmesi; bg_depth_tutorial_*, bg_depth_card ... aynı desen)
//   homework/DepthPress.kt (DEPTH_DP = 4: basılı durumda görünüm 4dp aşağı iner)
//
// Android'de bu iki parça birlikte çalışır:
//   - Basılı değil: arka plan iki katmandır. Yüz üstten 0, alttan 4dp boşlukla çizilir; dudak üstten
//     4dp boşlukla (yani yüzün 4dp altına taşan bir şerit olarak) arkada görünür.
//   - Basılı: dudak çizilmez, yalnızca yüz kalır ve DepthPress görünümü 4dp aşağı kaydırır; yüz
//     böylece daha önce dudağın olduğu yere oturur.
//
// Kullanım: çağıran düğmenin toplam yüksekliğini (dudak dahil; Android layout_height) `.frame`
// ile verir ve etiketi kendisi yazar. Etiket yüzün (yükseklik - derinlik) içinde ortalanır
// (Android: paddingBottom = 4dp):
//
//     Button(action: eylem) {
//         Text("Devam et").font(Tema.yazi(18)).foregroundColor(.black)
//     }
//     .buttonStyle(GocenDugmeStili(zemin: z, golge: g, koseYaricapi: 15))
//     .frame(height: 60)

struct GocenDugmeStili: ButtonStyle {
    /// Yüzün rengi (Android: ön katman `solid`).
    let zemin: Color
    /// Dudağın rengi (Android: arka katman `solid`).
    let golge: Color
    let koseYaricapi: CGFloat
    /// Dudak kalınlığı = basılınca inme miktarı (Android: 4dp).
    let derinlik: CGFloat

    init(zemin: Color, golge: Color, koseYaricapi: CGFloat = 16, derinlik: CGFloat = 4) {
        self.zemin = zemin
        self.golge = golge
        self.koseYaricapi = koseYaricapi
        self.derinlik = derinlik
    }

    func makeBody(configuration: Configuration) -> some View {
        let basili: Bool = configuration.isPressed
        let inme: CGFloat = basili ? derinlik : 0
        let dudakSaydamligi: Double = basili ? 0 : 1
        return ZStack(alignment: .top) {
            // Dudak: üstten `derinlik` boşluklu, yüzün altına taşan şerit. Basılıyken çizilmez.
            RoundedRectangle(cornerRadius: koseYaricapi)
                .fill(golge)
                .padding(.top, derinlik)
                .opacity(dudakSaydamligi)
            // Yüz: alttan `derinlik` boşluklu; etiket yüzün içinde ortalanır. Basılıyken aşağı iner.
            ZStack {
                RoundedRectangle(cornerRadius: koseYaricapi)
                    .fill(zemin)
                configuration.label
            }
            .padding(.bottom, derinlik)
            .offset(y: inme)
        }
        .contentShape(Rectangle())
    }
}

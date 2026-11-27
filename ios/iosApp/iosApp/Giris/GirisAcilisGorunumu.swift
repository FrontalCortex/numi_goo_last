import Shared
import SwiftUI

// Açılış ekranı. Android: SplashActivity + activity_splash.xml (10.11.2026: yalnızca marka).
// Zemin background_color; ortada splash_icon 288dp (sistem ikonuyla aynı tuval: ortadaki 160dp mor
// daire, çevresi saydam) ve altında "Sorobit" (34sp kalın, dark_text, -44dp üst boşluk: daireye
// yaklaşır). Çark ve "Öğrenmeye hazır mısın?" kaldırıldı.
//
// Android'de sistem açılış ekranının ikonu bizimkinin tam üstüne oturtulup kaldırılır, sonra ikon
// yerine kayarken yazı BRAND_REVEAL_MS (450 ms) içinde belirir (gecikme 450/3 ms, alttan 12dp,
// DecelerateInterpolator(1.6)). iOS'ta sistem açılış ekranı (project.yml) bu klasörün dışında; burada
// yalnızca yazının belirişi oynar.
@MainActor
struct GirisAcilisGorunumu: View {
    @ObservedObject var model: GirisModeli
    @State private var yaziGorunur: Bool = false

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            VStack(spacing: 0) {
                Image("splash_icon")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 288, height: 288)
                Text(Metin.al("activity_splash_text"))
                    .font(Tema.yazi(34))
                    .foregroundColor(Color("dark_text"))
                    .padding(.top, -44)
                    .opacity(yaziGorunur ? 1 : 0)
                    .offset(y: yaziGorunur ? 0 : 12)
            }
        }
        .onAppear {
            let sure: Double = Double(SplashRules.shared.BRAND_REVEAL_MS) / 1000.0
            // DecelerateInterpolator(1.6) yaklaşığı: hızlı başlayıp yavaşlayan eğri.
            withAnimation(.timingCurve(0.0, 0.0, 0.3, 1.0, duration: sure).delay(sure / 3)) {
                yaziGorunur = true
            }
        }
        .task {
            await model.acilisBasladi()
        }
    }
}

import SwiftUI

// Açılış ekranı. Android: SplashActivity + activity_splash.xml.
// Ortalanmış dikey dizilim: logo 240dp (scaleX 0.9), başlık 24sp (üst 24dp), alt başlık 16sp
// (üst 8dp), dönen gösterge (üst 48dp). Zemin background_color.
// Android bu sırada Remote Config (öğretici içeriği) de çeker; iOS'ta henüz yok.
@MainActor
struct GirisAcilisGorunumu: View {
    @ObservedObject var model: GirisModeli

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            VStack(spacing: 0) {
                Image("abacus_svg_ic")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 240, height: 240)
                    .scaleEffect(x: 0.9, y: 1.0)
                Text("Sorobit")
                    .font(Tema.yazi(24))
                    .foregroundColor(Color("dark_text"))
                    .padding(.top, 24)
                Text("Öğrenmeye hazır mısın?")
                    .font(Tema.yazi(16))
                    .foregroundColor(Color("dark_text"))
                    .padding(.top, 8)
                ProgressView()
                    .progressViewStyle(.circular)
                    .tint(Color("dark_text"))
                    .scaleEffect(1.4)
                    .padding(.top, 48)
            }
        }
        .task {
            await model.acilisBasladi()
        }
    }
}

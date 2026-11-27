import SwiftUI

// Henüz yazılmamış sekmeler için yer tutucu: sekmenin simgesi, adı ve "Yakında" yazısı.
struct YakindaEkrani: View {
    let sekme: Sekme

    var body: some View {
        VStack(spacing: 16) {
            Image(sekme.simgeAdi)
                .renderingMode(.original)
                .resizable()
                .scaledToFit()
                .frame(width: 72, height: 72)
            Text(sekme.ad)
                .font(Tema.yazi(24))
                .foregroundColor(Color.white)
            Text("Yakında")
                .font(Tema.yazi(17))
                .foregroundColor(Color.white.opacity(0.6))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color("background_color"))
    }
}

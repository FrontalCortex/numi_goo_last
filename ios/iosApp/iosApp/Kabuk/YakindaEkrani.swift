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
                .font(.system(size: 24, weight: .bold, design: .rounded))
                .foregroundColor(Color.white)
            Text("Yakında")
                .font(.system(size: 17, weight: .bold, design: .rounded))
                .foregroundColor(Color.white.opacity(0.6))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color("background_color"))
    }
}

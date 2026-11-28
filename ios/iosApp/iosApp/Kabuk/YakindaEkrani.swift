import SwiftUI

// Henüz yazılmamış sekmeler için yer tutucu: sekmenin simgesi, adı, Android karşılığı ve
// "Yakında" yazısı. Şu an: öğrencide Sobi sohbeti (SobiChatFragment), öğretmende Sınıflarım
// (TeacherClassesFragment).
struct YakindaEkrani: View {
    let simgeAdi: String
    let baslik: String
    /// Android'deki ekranın adı (yalnızca bilgi).
    let androidKarsiligi: String

    init(sekme: Sekme, ogretmen: Bool, androidKarsiligi: String) {
        self.simgeAdi = sekme.simgeAdi(ogretmen: ogretmen)
        self.baslik = sekme.ad(ogretmen: ogretmen)
        self.androidKarsiligi = androidKarsiligi
    }

    var body: some View {
        VStack(spacing: 16) {
            Image(simgeAdi)
                .renderingMode(.original)
                .resizable()
                .scaledToFit()
                .frame(width: 72, height: 72)
            Text(baslik)
                .font(Tema.yazi(24))
                .foregroundColor(Color.white)
            Text(Metin.al("ios_yakinda"))
                .font(Tema.yazi(17))
                .foregroundColor(Color.white.opacity(0.6))
            Text("Android: " + androidKarsiligi)
                .font(Tema.yazi(14, kalin: false))
                .foregroundColor(Color.white.opacity(0.4))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color("background_color"))
    }
}

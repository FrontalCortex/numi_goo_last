import SwiftUI

// Profil sayfasından açılan iki küçük yüzey:
//   - ProfilAbakusPaneli: "Kullanıcı Abaküsü" kartına dokununca açılan panel
//     (Android: ProfileFragment.showOwnAbacusPanel / showOtherUserAbacusPanel, panel_profile_abacus.xml)
//   - ProfilAvatarSecici: avatar seçme sayfası (Android: AvatarPickerFragment, fragment_avatar_picker.xml)

/// Android'de panelin içinde gerçek boyutlu, sürüklenebilir abaküs durur (layout_customization_preview +
/// AbacusBeadController). Abaküs çizimi Abakus klasörünün işi olduğu için burada yalnızca yer tutucu
/// boncuk kartı vardır; kendi profilimizde alttaki boncuk seçici Android'deki gibi çalışır.
struct ProfilAbakusPaneli: View {
    @ObservedObject var model: ProfilSayfaModeli
    let bildir: (String) -> Void

    /// Sahip olunan tipler ağdan gelene kadar yalnızca karttaki boncuk gösterilir.
    @State private var tipler: [String] = []
    @State private var sira: Int = 0

    var body: some View {
        GeometryReader { alan in
            let genislik: CGFloat = alan.size.width * 0.92
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    Spacer(minLength: 0)
                    Button(action: { model.abakusPaneliAcik = false }) {
                        Image("ic_close_black")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .foregroundColor(Color.white)
                            .frame(width: 24, height: 24)
                            .frame(width: 40, height: 40)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
                ProfilBoncukKarti(tip: model.boncukTipi)
                    .scaleEffect(1.6)
                    .frame(width: 150, height: 150)
                    .padding(.top, 12)
                if !model.baskasi {
                    secici
                        .padding(.top, 16)
                }
            }
            .padding(20)
            .frame(width: genislik)
            .background(RoundedRectangle(cornerRadius: 24).fill(Color("panel_background")))
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .task { await tipleriYukle() }
    }

    /// previewBeadSelectorRow: "Profil ikonunu değiştir" + < kart >.
    private var secici: some View {
        VStack(spacing: 10) {
            Text("Profil ikonunu değiştir")
                .font(ProfilStil.yazi(14))
                .foregroundColor(ProfilStil.soluk)
            HStack(spacing: 8) {
                okDugmesi(ileri: false)
                ProfilBoncukKarti(tip: model.boncukTipi)
                okDugmesi(ileri: true)
            }
        }
    }

    private func okDugmesi(ileri: Bool) -> some View {
        Button(action: { adimAt(ileri: ileri) }) {
            Image("ic_back_arrow")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(Color.white)
                .frame(width: 24, height: 24)
                .scaleEffect(x: ileri ? -1 : 1, y: 1)
                .frame(width: 40, height: 40)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func tipleriYukle() async {
        if model.baskasi { return }
        guard let baslangic = model.boncukTipi else { return }
        let adetler = await model.sahipOlunanBoncuklar()
        let liste = ProfilOrtak.sahipOlunanBoncuklar(
            tumTipler: ProfilOrtak.tumBoncukTipleri, adetler: adetler, baslangic: baslangic
        )
        tipler = liste
        sira = liste.firstIndex(of: baslangic) ?? 0
    }

    private func adimAt(ileri: Bool) {
        // Liste henüz gelmediyse Android'de de oklar bir şey yapmaz.
        if tipler.isEmpty { return }
        guard let yeni = ProfilOrtak.boncukAdimi(sira: sira, adet: tipler.count, ileri: ileri) else {
            bildir(ProfilOrtak.tekBoncukBildirimi)
            return
        }
        sira = yeni
        model.boncukSecildi(tipler[yeni])
    }
}

/// 12 avatar, 3 sütun x 4 satır. Seçim `users/{uid}.selectedAvatar` alanına yazılır.
struct ProfilAvatarSecici: View {
    let sec: (Int) -> Void

    private let sutunlar: [GridItem] = [
        GridItem(.flexible(), spacing: 0),
        GridItem(.flexible(), spacing: 0),
        GridItem(.flexible(), spacing: 0)
    ]

    var body: some View {
        VStack(spacing: 0) {
            // Tutamaç
            Capsule()
                .fill(Color.white.opacity(0.3))
                .frame(width: 40, height: 4)
                .padding(.top, 16)
                .padding(.bottom, 16)
            Text("Avatar Seç")
                .font(ProfilStil.yazi(18))
                .foregroundColor(Color.white)
                .padding(.bottom, 20)
            ScrollView(.vertical, showsIndicators: false) {
                LazyVGrid(columns: sutunlar, spacing: 0) {
                    ForEach(1...12, id: \.self) { sira in
                        Button(action: { sec(sira) }) {
                            Image(ProfilOrtak.avatarAdi(sira))
                                .resizable()
                                .scaledToFit()
                                .frame(width: 80, height: 80)
                                .frame(maxWidth: .infinity)
                                .background(RoundedRectangle(cornerRadius: 16).fill(Color.white.opacity(0.1)))
                                .padding(8)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(Text("Avatar \(sira)"))
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(Color("background_color").ignoresSafeArea())
        .presentationDetents([.medium, .large])
        .preferredColorScheme(.dark)
    }
}

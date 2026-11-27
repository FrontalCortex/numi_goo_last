import SwiftUI

// Ekran galerisi için (Yonetim/EkranGalerisi.swift "avatar"): farklı avatar dizgeleriyle çizilmiş 12 avatar.
// Amaç Codemagic ekran görüntüsünde çizicinin (yollar, yaylar, maskeler, degrade, daire kırpma) Android'le
// karşılaştırılabilmesi. İlk hücre boş dizge (Android'in varsayılan avatarı), son satır dairesiz ve yakın plan.

struct AvatarOrnekIzgarasi: View {
    /// (etiket, dizge, arka plan dairesi, yakın plan)
    static let ornekler: [(String, String, Bool, Bool)] = [
        ("varsayılan (boş)", "", true, false),
        ("long / sunglasses", "personas|hair=long;eyes=sunglasses;mouth=smirk;hairColor=362c47;backgroundColor=65c9ff", true, false),
        ("curly (maske)", "personas|hair=curly;eyes=happy;hairColor=6c4545;skinColor=b16a5b;backgroundColor=c0aede", true, false),
        ("fade (alfa maskesi)", "personas|hair=fade;eyes=wink;facialHair=beardMustache;hairColor=f27d65;skinColor=92594b;backgroundColor=ffdfbf", true, false),
        ("curlyHighTop / small", "personas|hair=curlyHighTop;body=small;clothingColor=6dbb58;eyes=glasses;backgroundColor=a7ffc4", true, false),
        ("bunUndercut / checkered", "personas|hair=bunUndercut;body=checkered;clothingColor=e24553;mouth=surprise;backgroundColor=ffd5dc", true, false),
        ("mohawk / pacifier", "personas|hair=mohawk;mouth=pacifier;hairColor=e16381;nose=wrinkles;backgroundColor=ffffb1", true, false),
        ("beanie / koyu zemin", "personas|hair=beanie;eyes=sleep;mouth=frown;facialHair=walrus;skinColor=623d36;backgroundColor=262e33", true, false),
        ("extraLong / bozuk renk", "personas|hair=extraLong;hairColor=zzzzzz;body=squared;clothingColor=7555ca;backgroundColor=d1d4f9", true, false),
        ("dairesiz", "personas|hair=pigtails;eyes=happy;mouth=bigSmile;backgroundColor=ffd5dc", false, false),
        ("yakın plan", "personas|hair=pigtails;eyes=happy;mouth=bigSmile;facialHair=goatee", false, true),
        ("bozuk dizge", "bozuk-dizge", true, false),
    ]

    private let sutunlar: [GridItem] = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
    ]

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            LazyVGrid(columns: sutunlar, spacing: 16) {
                ForEach(0..<AvatarOrnekIzgarasi.ornekler.count, id: \.self) { sira in
                    AvatarOrnekHucresi(ornek: AvatarOrnekIzgarasi.ornekler[sira])
                }
            }
            .padding(16)
        }
        .background(Color("background_color").ignoresSafeArea())
    }
}

struct AvatarOrnekHucresi: View {
    let ornek: (String, String, Bool, Bool)

    var body: some View {
        VStack(spacing: 6) {
            AvatarGorunumu(config: ornek.1, arkaPlan: ornek.2, yakinPlan: ornek.3)
                .frame(width: 96, height: 96)
                .background(RoundedRectangle(cornerRadius: 12).fill(Color("bright_background_color")))
            Text(ornek.0)
                .font(Tema.yazi(11, kalin: false))
                .foregroundColor(Color.white.opacity(0.8))
                .lineLimit(2)
                .multilineTextAlignment(.center)
        }
    }
}

import SwiftUI

// Kullanıcının avatarını kodlanmış dizgesinden ("personas|hair=long;eyes=open;…", Android
// `AvatarConfig.encode`) çizen görünüm. Android: AvatarView (+ AvatarArt.buildSvg, AndroidSVG).
//
// DIŞA AÇIK SÖZLEŞME (başka klasörler buna güveniyor; değiştirmeyin):
//
//     AvatarGorunumu(config: satir.avatarConfig)
//         .frame(width: 48, height: 48)
//
// - `config` boş ya da okunamıyorsa Android'in varsayılan avatarı çizilir (AvatarConfig.default; Android
//   lider tablosu `leaderboardAvatarBitmap` ve alt çubuk da böyle yapıyor).
// - Boyut `.frame` ile verilir; çizim alana orantılı sığdırılıp ortalanır (Android fitCenter gibi).
// - Varsayılan çizim Android AvatarView'in varsayılanıdır (`showBackground = true`): arkada avatarın
//   arka plan rengiyle dolu bir DAİRE ve avatar o daireye kırpılmış. Daire kırpma görünümün kendisinde,
//   çağıranın ayrıca `clipShape` vermesi gerekmez. Dairenin dışı saydamdır.
//
// Profil ve düzenleme ekranındaki dairesiz / yakın plan çizimler için ikinci kurucu:
//
//     AvatarGorunumu(config: c, arkaPlan: false, yakinPlan: true)

struct AvatarGorunumu: View {
    let config: String
    /// Arkadaki renkli daire ve daireye kırpma (Android: AvatarView.showBackground).
    let arkaPlan: Bool
    /// Yalnızca yüz bölgesi (Android: AvatarView.closeUp, AvatarStyle.faceCloseUp).
    let yakinPlan: Bool

    init(config: String) {
        self.config = config
        self.arkaPlan = true
        self.yakinPlan = false
    }

    init(config: String, arkaPlan: Bool, yakinPlan: Bool) {
        self.config = config
        self.arkaPlan = arkaPlan
        self.yakinPlan = yakinPlan
    }

    private var belge: AvatarSvgBelgesi? {
        return AvatarOrtak.belge(config: config, daire: arkaPlan, yakinPlan: yakinPlan)
    }

    var body: some View {
        AvatarTuvali(belge: belge)
            .accessibilityHidden(true)
    }
}

/// Çözülmüş belgeyi çizen tuval (gövde küçük kalsın diye ayrı).
struct AvatarTuvali: View {
    let belge: AvatarSvgBelgesi?

    var body: some View {
        let cizilecek: AvatarSvgBelgesi? = belge
        return Canvas { baglam, boyut in
            guard let b = cizilecek else { return }
            AvatarSvgCizici(belge: b).ciz(baglam, boyut: boyut)
        }
    }
}

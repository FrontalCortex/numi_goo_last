import Combine
import SwiftUI

// Giriş akışının dışa açılan tek görünümü:
// açılış → başlangıç → (giriş | yaş + kaynak → kayıt) → doğrulama kodu → `girisYapildi()`.
// Android karşılığı: SplashActivity → LoginStartActivity → LoginActivity / UserInfoFragment →
// RegisterActivity → OtpVerificationFragment → MainActivity (ve öğretmen için TeacherLoginActivity).
//
// Yaş adımı Android'de olduğu gibi kayıttan ÖNCE gelir: doğum yılı kayıt çağrısıyla sunucuya
// gider ve kullanıcı belgesine orada yazılır. Android'de ayrı bir rıza ekranı yoktur; rıza yaşı
// tablosu (AgeConsentManager) girişten sonra reklam ayarları için kullanılır.
@MainActor
struct GirisAkisi: View {
    @StateObject private var model: GirisModeli

    private let saat = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    @MainActor
    init(servis: KimlikServisi, girisYapildi: @escaping () -> Void) {
        _model = StateObject(
            wrappedValue: GirisModeli(servis: servis, depo: GirisYerelDepo(), girisYapildi: girisYapildi)
        )
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            Color("dark_background").ignoresSafeArea()
            ekranGorunumu
                .allowsHitTesting(!model.mesgul)
            if let bildirim = model.bildirim {
                GirisBildirimGorunumu(metin: bildirim.metin)
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: model.bildirim)
        .onReceive(saat) { _ in
            model.saatVurdu()
        }
        .preferredColorScheme(.dark)
    }

    @ViewBuilder
    private var ekranGorunumu: some View {
        switch model.ekran {
        case .acilis:
            GirisAcilisGorunumu(model: model)
        case .baslangic:
            GirisBaslangicGorunumu(model: model)
        case .ogrenciGiris:
            GirisEpostaGorunumu(model: model)
        case .kullaniciBilgisi:
            GirisYasGorunumu(model: model)
        case .kayit:
            KayitGorunumu(model: model)
        case .kod(let eposta, let tur):
            GirisKodGorunumu(model: model, eposta: eposta, tur: tur)
        case .ogretmenGiris:
            GirisOgretmenGorunumu(model: model)
        case .sifremiUnuttum:
            GirisSifremiUnuttumGorunumu(model: model)
        case .yeniSifre(let eposta, let kod):
            GirisYeniSifreGorunumu(model: model, eposta: eposta, kod: kod)
        }
    }
}

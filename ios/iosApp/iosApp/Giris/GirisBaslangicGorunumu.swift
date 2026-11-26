import SwiftUI

// Giriş başlangıç ekranı. Android: LoginStartActivity + activity_login_start.xml.
// Yerleşim (ConstraintLayout, kenar boşluğu 24dp):
//   - yüksekliğin %25'inde başlık (24sp), 24dp altında "GİRİŞ YAP" (56dp, dolu)
//   - %50'sinde 1dp gri çizgi
//   - %55'inde alt başlık (26sp), 16dp altında "BAŞLA" (56dp, çerçeveli)
//   - sağ altta öğretmen/öğrenci modu yazısı (14sp, dark_text_secondary, iç boşluk 8dp)
// Android'de geri tuşu bu ekranda işlevsizdir; burada geri düğmesi yoktur.
@MainActor
struct GirisBaslangicGorunumu: View {
    @ObservedObject var model: GirisModeli

    var body: some View {
        GeometryReader { alan in
            let yukseklik: CGFloat = alan.size.height
            ZStack(alignment: .top) {
                ustGrup
                    .padding(.top, yukseklik * 0.25)
                Rectangle()
                    .fill(Color("gray"))
                    .frame(height: 1)
                    .padding(.top, yukseklik * 0.50)
                altGrup
                    .padding(.top, yukseklik * 0.55)
                modDugmesi
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .padding(.horizontal, GirisStil.kenarBoslugu)
        }
        .padding(.bottom, GirisStil.kenarBoslugu)
        .onAppear {
            model.baslangicGoruldu()
        }
    }

    private var ustGrup: some View {
        VStack(spacing: 0) {
            Text(model.ogretmenModu ? GirisMetin.ogretmenBaslik : GirisMetin.ogrenciBaslik)
                .font(Tema.yazi(24))
                .foregroundColor(Color("dark_text"))
                .multilineTextAlignment(.center)
            GirisDoluDugme(metin: GirisMetin.girisYap, buyukHarf: false, yaziBoyu: 18) {
                model.baslangicGirisYap()
            }
            .padding(.top, 24)
        }
    }

    private var altGrup: some View {
        VStack(spacing: 0) {
            Text(model.ogretmenModu ? GirisMetin.ogretmenAltBaslik : GirisMetin.ogrenciAltBaslik)
                .font(Tema.yazi(26))
                .foregroundColor(Color("dark_text"))
                .multilineTextAlignment(.center)
                .minimumScaleFactor(0.7)
            GirisCerceveliDugme(metin: GirisMetin.basla, buyukHarf: false, yaziBoyu: 18) {
                model.baslangicBasla()
            }
            .padding(.top, 16)
        }
    }

    private var modDugmesi: some View {
        Button {
            model.modDegistir()
        } label: {
            Text(model.ogretmenModu ? GirisMetin.ogrenciGirisi : GirisMetin.ogretmenGirisi)
                .font(Tema.yazi(14))
                .foregroundColor(Color("dark_text_secondary"))
                .padding(8)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

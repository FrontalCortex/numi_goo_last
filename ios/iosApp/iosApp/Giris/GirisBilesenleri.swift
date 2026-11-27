import SwiftUI
import UIKit

// Giriş ekranlarının ortak parçaları: düğmeler, metin alanı, geri düğmesi, bildirim.
// Android'de bunlar Material bileşenleridir (MaterialButton, TextInputLayout, Toast); asset olarak
// gelmedikleri için SwiftUI'da çizilir. Ölçüler layout XML'lerinden (dp → pt birebir).

enum GirisStil {
    /// Ekran kenar boşluğu (activity_login.xml, activity_register.xml: padding).
    static let kenarBoslugu: CGFloat = 24
    /// MaterialButton köşe yarıçapı (app:cornerRadius).
    static let dugmeKosesi: CGFloat = 8
    /// MaterialButton'ın üst/alt iç boşluğu (Material bileşenlerinin varsayılan insetTop/insetBottom):
    /// 56dp yüksekliğindeki düğmenin görünen gövdesi 44dp'dir.
    static let dugmeIcBoslugu: CGFloat = 6
    /// Göçen düğmenin dudak kalınlığı (DepthButtons.DEPTH_DP).
    static let dudak: CGFloat = 4

    /// 0xRRGGBB biçimindeki sabit renk (layout XML'lerinde kaynak adı olmadan yazılmış renkler için).
    static func renk(_ rgb: UInt32, alfa: Double = 1.0) -> Color {
        let kirmizi = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: alfa)
    }

    /// Google düğmesinin yazı rengi (activity_login.xml: #453535).
    static let saglayiciYaziRengi: Color = GirisStil.renk(0x453535)
    /// dark_primary (#58A6FF) yüzün dudağı: DepthButtons.darken (kanal × 0,78, aşağı yuvarlanır) → #4481C6.
    static let anaDudak: Color = GirisStil.renk(0x4481C6)
    /// Android teması (Theme.MaterialComponents, koyu) colorPrimary tanımlamaz; Material'ın koyu
    /// tema varsayılanı #BB86FC kalır. TextInputLayout odak çizgisi ve imleç bu rengi kullanır.
    static let materyalAnaRenk: Color = GirisStil.renk(0xBB86FC)
    /// Material düğme yazısının harf aralığı (TextAppearance.MaterialComponents.Button:
    /// letterSpacing = 0.0892857143 em).
    static let dugmeHarfAraligi: CGFloat = 0.0892857143

    static func klavyeyiKapat() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}

/// Sol üstteki geri düğmesi (btnBack: 44x44, ic_back_arrow 24dp, dark_text).
@MainActor
struct GirisGeriDugmesi: View {
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            Image("ic_back_arrow")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: 24, height: 24)
                .foregroundColor(Color("dark_text"))
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Metin.al("activity_login_btn_back_desc"))
    }
}

/// Dolu düz düğme (MaterialButton, backgroundTint = dark_primary, yazı beyaz). Göçen biçim
/// verilmeyen düğmeler için (doğrulama kodu, veli onayı, öğretmen girişi).
/// Material düğmeleri metni büyük harfe çevirir; `buyukHarf` false ise (textAllCaps="false") çevrilmez.
@MainActor
struct GirisDoluDugme: View {
    let metin: String
    var etkin: Bool = true
    var buyukHarf: Bool = true
    var yaziBoyu: CGFloat = 16
    var yukseklik: CGFloat = 56
    let eylem: () -> Void

    var body: some View {
        // app:backgroundTint tek renktir (durum listesi değil): devre dışıyken de zemin aynı kalır,
        // yalnızca yazı soluklaşır (mtrl_btn_text_color_selector: colorOnSurface %38).
        let yaziRengi: Color = etkin ? Color.white : Color.white.opacity(0.38)
        Button(action: eylem) {
            Text(metin)
                .tracking(yaziBoyu * GirisStil.dugmeHarfAraligi)
                .font(Tema.yazi(yaziBoyu))
                .textCase(buyukHarf ? .uppercase : nil)
                .foregroundColor(yaziRengi)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .frame(maxWidth: .infinity)
                .frame(height: yukseklik - GirisStil.dugmeIcBoslugu * 2)
                .background(RoundedRectangle(cornerRadius: GirisStil.dugmeKosesi).fill(Color("dark_primary")))
                .padding(.vertical, GirisStil.dugmeIcBoslugu)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!etkin)
    }
}

/// Çerçeveli düğme (MaterialButton: saydam zemin, 2dp dark_primary çizgi, dark_primary yazı).
@MainActor
struct GirisCerceveliDugme: View {
    let metin: String
    var etkin: Bool = true
    var buyukHarf: Bool = true
    var yaziBoyu: CGFloat = 16
    var yukseklik: CGFloat = 56
    let eylem: () -> Void

    var body: some View {
        // Çizgi ve yazı rengi tek renktir (durum listesi değil): Android'de devre dışıyken
        // görünüm değişmez, yalnızca dokunma kapanır.
        let renk: Color = Color("dark_primary")
        Button(action: eylem) {
            Text(metin)
                .tracking(yaziBoyu * GirisStil.dugmeHarfAraligi)
                .font(Tema.yazi(yaziBoyu))
                .textCase(buyukHarf ? .uppercase : nil)
                .foregroundColor(renk)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .frame(maxWidth: .infinity)
                .frame(height: yukseklik - GirisStil.dugmeIcBoslugu * 2)
                .background(RoundedRectangle(cornerRadius: GirisStil.dugmeKosesi).stroke(renk, lineWidth: 2))
                .padding(.vertical, GirisStil.dugmeIcBoslugu)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!etkin)
    }
}

/// Göçen (derinlikli) düğme. Android: DepthButtons.style — altta 4dp koyu dudak, basılıyken dudak
/// gizlenir ve yüz 4dp iner (Ortak/GocenDugmeStili). Devre dışıyken dudak yok, düz yüz
/// ("basılamayacağı belli olsun"); yazı soluk (colorOnSurface %38).
/// Android'de bu arka plan MaterialButton'ın kendi zemininin YERİNE konduğu için Material'ın
/// üst/alt 6dp iç boşluğu yoktur: yüz + dudak bütün yüksekliği kaplar.
@MainActor
struct GirisGocenDugme: View {
    let metin: String
    var etkin: Bool = true
    var buyukHarf: Bool = true
    var yaziBoyu: CGFloat = 16
    var yukseklik: CGFloat = 56
    var kose: CGFloat = GirisStil.dugmeKosesi
    var zemin: Color = Color("dark_primary")
    /// Dudak; Android varsayılanı yüzün %78 koyusu (DepthButtons.darken).
    var golge: Color = GirisStil.anaDudak
    var yaziRengi: Color = Color.white
    /// Yüzün kenar çizgisi (1,5dp); nil = yok.
    var cizgi: Color? = nil
    /// Metnin solundaki görsel (Google: ic_google 24dp, aralık 12dp).
    var simge: String? = nil
    /// Metnin solundaki sistem yazı tipi karakteri (Apple logosu U+F8FF).
    var harfSimge: String? = nil
    /// Yazı rengi layout'ta sabit verildiyse (ör. Google düğmesinin #453535'i) devre dışıyken de
    /// aynı kalır; verilmediyse Material'ın durum listesi gibi soluklaşır.
    var sabitYaziRengi: Bool = false
    let eylem: () -> Void

    var body: some View {
        if etkin {
            Button(action: eylem) {
                etiket(soluk: false)
            }
            .buttonStyle(GocenDugmeStili(zemin: zemin, golge: golge, koseYaricapi: kose, derinlik: GirisStil.dudak))
            .frame(height: yukseklik)
        } else {
            ZStack {
                RoundedRectangle(cornerRadius: kose).fill(zemin)
                etiket(soluk: true)
            }
            .padding(.bottom, GirisStil.dudak)
            .frame(height: yukseklik)
        }
    }

    private func etiket(soluk: Bool) -> some View {
        let renk: Color = (soluk && !sabitYaziRengi) ? Color.white.opacity(0.38) : yaziRengi
        let aralik: CGFloat = buyukHarf ? yaziBoyu * GirisStil.dugmeHarfAraligi : 0
        return HStack(spacing: 12) {
            if let ad = simge {
                Image(ad)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24)
            }
            if let harf = harfSimge {
                Text(harf)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundColor(renk)
            }
            Text(metin)
                .tracking(aralik)
                .font(Tema.yazi(yaziBoyu))
                .textCase(buyukHarf ? .uppercase : nil)
                .foregroundColor(renk)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .padding(.horizontal, 12)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .overlay(cizgiKatmani)
        .contentShape(Rectangle())
    }

    @ViewBuilder
    private var cizgiKatmani: some View {
        if let renk = cizgi {
            RoundedRectangle(cornerRadius: kose).strokeBorder(renk, lineWidth: 1.5)
        }
    }
}

/// Google ile giriş/kayıt düğmesi. Android: btnGoogleSignIn + DepthButtons.google — beyaz yüz,
/// dark_text_secondary dudak ve 1,5dp kenar; ic_google 24dp, simge ile yazı arası 12dp, yazı #453535.
@MainActor
struct GirisGoogleDugmesi: View {
    let metin: String
    var etkin: Bool = true
    let eylem: () -> Void

    var body: some View {
        GirisGocenDugme(
            metin: metin,
            etkin: etkin,
            zemin: Color.white,
            golge: Color("dark_text_secondary"),
            yaziRengi: GirisStil.saglayiciYaziRengi,
            cizgi: Color("dark_text_secondary"),
            simge: "ic_google",
            sabitYaziRengi: true,
            eylem: eylem
        )
    }
}

/// Apple ile giriş/kayıt düğmesi. Android'de karşılığı yok: App Store kuralı 4.8 (üçüncü taraf
/// giriş varsa Apple ile giriş şart). Google düğmesinin hemen altında, aynı göçen biçimde: siyah
/// yüz, beyaz yazı, Apple logosu (sistem yazı tipindeki U+F8FF). `AuthenticationServices` içe
/// aktarılmadı; Firebase bağlanırken `ASAuthorizationAppleIDButton` ile değiştirilmesi önerilir.
@MainActor
struct GirisAppleDugmesi: View {
    let metin: String
    var etkin: Bool = true
    let eylem: () -> Void

    var body: some View {
        GirisGocenDugme(
            metin: metin,
            etkin: etkin,
            zemin: Color.black,
            golge: Color("dark_text_secondary"),
            yaziRengi: Color.white,
            cizgi: Color("dark_text_secondary"),
            harfSimge: "\u{F8FF}",
            sabitYaziRengi: true,
            eylem: eylem
        )
    }
}

/// Bağlantı görünümlü ikincil düğme ("Zaten hesabım var", "Şimdi değil"): kalın dark_primary,
/// en az 48dp yükseklik, yanlarda 16dp iç boşluk (selectableItemBackground; zemin yok).
@MainActor
struct GirisBaglantiDugmesi: View {
    let metin: String
    var yaziBoyu: CGFloat = 17
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            Text(metin)
                .font(Tema.yazi(yaziBoyu))
                .foregroundColor(Color("dark_primary"))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 16)
                .frame(minHeight: 48)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

enum GirisAlanTuru {
    case eposta
    case sifre
    case sayi
    case ad
}

/// Dolu kutunun biçimi: üst köşeler 4dp yuvarlak, alt köşeler düz
/// (Widget.MaterialComponents.TextInputLayout.FilledBox).
struct GirisUstYuvarlakSekil: Shape {
    var yaricap: CGFloat

    func path(in rect: CGRect) -> Path {
        var yol = Path()
        let solUst = CGPoint(x: rect.minX + yaricap, y: rect.minY + yaricap)
        let sagUst = CGPoint(x: rect.maxX - yaricap, y: rect.minY + yaricap)
        yol.move(to: CGPoint(x: rect.minX, y: rect.maxY))
        yol.addLine(to: CGPoint(x: rect.minX, y: rect.minY + yaricap))
        yol.addArc(
            center: solUst,
            radius: yaricap,
            startAngle: Angle.degrees(180),
            endAngle: Angle.degrees(270),
            clockwise: false
        )
        yol.addLine(to: CGPoint(x: rect.maxX - yaricap, y: rect.minY))
        yol.addArc(
            center: sagUst,
            radius: yaricap,
            startAngle: Angle.degrees(270),
            endAngle: Angle.degrees(360),
            clockwise: false
        )
        yol.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        yol.closeSubpath()
        return yol
    }
}

/// Metin alanı (TextInputLayout, dolu kutu: boxBackgroundColor = dark_surface, yazı dark_text,
/// ipucu dark_text_secondary; yan iç boşluk 12dp, üst köşeler 4dp).
/// Durumlar (Material 2 varsayılanları; Android layout'larında hata ya da parola göster/gizle
/// simgesi yoktur, hatalar Toast ile bildirilir):
///   - boş ve odaksız: ipucu 16sp kutunun ortasında, alt çizgi 1dp (%42 beyaz)
///   - odakta ya da dolu: ipucu 12sp olarak yukarı kayar (hintTextColor = dark_text_secondary)
///   - odakta: alt çizgi 2dp colorPrimary, imleç colorPrimary
@MainActor
struct GirisMetinAlani: View {
    let ipucu: String
    @Binding var metin: String
    var tur: GirisAlanTuru = .eposta
    var azamiUzunluk: Int? = nil

    @FocusState private var odakta: Bool

    init(ipucu: String, metin: Binding<String>, tur: GirisAlanTuru = .eposta, azamiUzunluk: Int? = nil) {
        self.ipucu = ipucu
        self._metin = metin
        self.tur = tur
        self.azamiUzunluk = azamiUzunluk
    }

    var body: some View {
        let yukarida: Bool = odakta || !metin.isEmpty
        let cizgiKalinligi: CGFloat = odakta ? 2 : 1
        let cizgiRengi: Color = odakta ? GirisStil.materyalAnaRenk : Color.white.opacity(0.42)
        ZStack(alignment: .topLeading) {
            etiket(yukarida: yukarida)
            alan
                .focused($odakta)
                .font(Tema.yazi(16))
                .foregroundColor(Color("dark_text"))
                .tint(GirisStil.materyalAnaRenk)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                .padding(.bottom, 4)
        }
        .padding(.horizontal, 12)
        .frame(height: 56)
        .background(Color("dark_surface"))
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(cizgiRengi)
                .frame(height: cizgiKalinligi)
        }
        .clipShape(GirisUstYuvarlakSekil(yaricap: 4))
        .contentShape(Rectangle())
        .onTapGesture {
            odakta = true
        }
        .animation(.easeOut(duration: 0.167), value: yukarida)
        .onChange(of: metin) { yeni in
            if let azami = azamiUzunluk, yeni.count > azami {
                metin = String(yeni.prefix(azami))
            }
        }
    }

    private func etiket(yukarida: Bool) -> some View {
        let boyut: CGFloat = yukarida ? 12 : 16
        let hiza: Alignment = yukarida ? .topLeading : .leading
        let ustBosluk: CGFloat = yukarida ? 4 : 0
        return Text(ipucu)
            .font(Tema.yazi(boyut))
            .foregroundColor(Color("dark_text_secondary"))
            .lineLimit(1)
            .padding(.top, ustBosluk)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: hiza)
            .allowsHitTesting(false)
    }

    @ViewBuilder
    private var alan: some View {
        switch tur {
        case .eposta:
            TextField("", text: $metin)
                .keyboardType(.emailAddress)
                .textContentType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled(true)
        case .sifre:
            SecureField("", text: $metin)
        case .sayi:
            TextField("", text: $metin)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
        case .ad:
            TextField("", text: $metin)
                .autocorrectionDisabled(true)
        }
    }
}

/// Kısa bildirim (Android Toast / Snackbar): ekranın altında, ortada.
@MainActor
struct GirisBildirimGorunumu: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(Tema.yazi(14))
            .foregroundColor(Color.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(RoundedRectangle(cornerRadius: 22).fill(GirisStil.renk(0x323232)))
            .padding(.horizontal, 32)
            .padding(.bottom, 64)
            .allowsHitTesting(false)
    }
}

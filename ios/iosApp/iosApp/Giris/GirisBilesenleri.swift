import SwiftUI
import UIKit

// Giriş ekranlarının ortak parçaları: düğmeler, metin alanı, geri düğmesi, bildirim.
// Android'de bunlar Material bileşenleridir (MaterialButton, TextInputLayout, Toast); asset olarak
// gelmedikleri için SwiftUI'da çizilir. Ölçüler layout XML'lerinden (dp → pt birebir).

enum GirisStil {
    /// Ekran kenar boşluğu (activity_login.xml, activity_register.xml, activity_login_start.xml: padding).
    static let kenarBoslugu: CGFloat = 24
    /// MaterialButton köşe yarıçapı (app:cornerRadius).
    static let dugmeKosesi: CGFloat = 8
    /// MaterialButton'ın üst/alt iç boşluğu (Material bileşenlerinin varsayılan insetTop/insetBottom):
    /// 56dp yüksekliğindeki düğmenin görünen gövdesi 44dp'dir.
    static let dugmeIcBoslugu: CGFloat = 6

    /// 0xRRGGBB biçimindeki sabit renk (layout XML'lerinde kaynak adı olmadan yazılmış renkler için).
    static func renk(_ rgb: UInt32, alfa: Double = 1.0) -> Color {
        let kirmizi = Double((rgb >> 16) & 0xFF) / 255.0
        let yesil = Double((rgb >> 8) & 0xFF) / 255.0
        let mavi = Double(rgb & 0xFF) / 255.0
        return Color(.sRGB, red: kirmizi, green: yesil, blue: mavi, opacity: alfa)
    }

    /// Google düğmesinin yazı rengi (activity_login.xml: #453535).
    static let saglayiciYaziRengi: Color = GirisStil.renk(0x453535)
    /// Seçili olmayan kaynak kartının çizgisi (fragment_user_info.xml: #33FFFFFF).
    static let kartCizgisi: Color = Color.white.opacity(Double(0x33) / 255.0)
    /// bg_edit_profile_input.xml çizgisi (#30363D).
    static let yasAlaniCizgisi: Color = GirisStil.renk(0x30363D)
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
        .accessibilityLabel("Geri")
    }
}

/// Dolu ana düğme (MaterialButton, backgroundTint = dark_primary, yazı beyaz).
/// Material düğmeleri metni büyük harfe çevirir; `buyukHarf` false ise (textAllCaps="false") çevrilmez.
@MainActor
struct GirisDoluDugme: View {
    let metin: String
    var etkin: Bool = true
    var buyukHarf: Bool = true
    var yaziBoyu: CGFloat = 16
    var yukseklik: CGFloat = 56
    /// Devre dışı zemin rengi; nil ise Material varsayılanı (yüzey üstünde %12).
    var devreDisiZemin: Color? = nil
    let eylem: () -> Void

    private var zemin: Color {
        // app:backgroundTint tek renktir (durum listesi değil): devre dışıyken de zemin aynı kalır,
        // yalnızca yazı soluklaşır (mtrl_btn_text_color_selector: colorOnSurface %38).
        if etkin { return Color("dark_primary") }
        return devreDisiZemin ?? Color("dark_primary")
    }

    private var yaziRengi: Color {
        if etkin || devreDisiZemin != nil { return Color.white }
        return Color.white.opacity(0.38)
    }

    var body: some View {
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
                .background(RoundedRectangle(cornerRadius: GirisStil.dugmeKosesi).fill(zemin))
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

/// Google ile giriş düğmesi (btnGoogleSignIn: beyaz zemin, 1dp dark_text_secondary çizgi,
/// ic_google 24dp, simge ile yazı arası 12dp, yazı #453535).
@MainActor
struct GirisGoogleDugmesi: View {
    let metin: String
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            HStack(spacing: 12) {
                Image("ic_google")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                Text(metin)
                    .tracking(16 * GirisStil.dugmeHarfAraligi)
                    .font(Tema.yazi(16))
                    .textCase(.uppercase)
                    .foregroundColor(GirisStil.saglayiciYaziRengi)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 56 - GirisStil.dugmeIcBoslugu * 2)
            .background(RoundedRectangle(cornerRadius: GirisStil.dugmeKosesi).fill(Color.white))
            .overlay(
                RoundedRectangle(cornerRadius: GirisStil.dugmeKosesi)
                    .stroke(Color("dark_text_secondary"), lineWidth: 1)
            )
            .padding(.vertical, GirisStil.dugmeIcBoslugu)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Apple ile giriş düğmesi. Android'de karşılığı yok (App Store kuralı 4.8 gereği eklendi).
/// `AuthenticationServices` içe aktarılmadığı için sistemin hazır düğmesi yerine yalın bir düğme
/// çizilir: siyah zemin, beyaz yazı, Apple logosu (sistem yazı tipindeki U+F8FF karakteri).
/// Firebase bağlanırken `ASAuthorizationAppleIDButton` ile değiştirilmesi önerilir.
@MainActor
struct GirisAppleDugmesi: View {
    let metin: String
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            HStack(spacing: 12) {
                Text("\u{F8FF}")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundColor(Color.white)
                Text(metin)
                    .tracking(16 * GirisStil.dugmeHarfAraligi)
                    .font(Tema.yazi(16))
                    .textCase(.uppercase)
                    .foregroundColor(Color.white)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 56 - GirisStil.dugmeIcBoslugu * 2)
            .background(RoundedRectangle(cornerRadius: GirisStil.dugmeKosesi).fill(Color.black))
            .overlay(
                RoundedRectangle(cornerRadius: GirisStil.dugmeKosesi)
                    .stroke(Color("dark_text_secondary"), lineWidth: 1)
            )
            .padding(.vertical, GirisStil.dugmeIcBoslugu)
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

/// Kısa bildirim (Android Toast): ekranın altında, ortada.
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

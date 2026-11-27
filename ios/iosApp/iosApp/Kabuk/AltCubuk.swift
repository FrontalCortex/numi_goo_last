import SwiftUI

// Alt gezinme çubuğu. Android activity_main.xml -> bottomNavigationID (menü: bottom_menu.xml).
// Altı sekme eşit genişlikte; yalnızca simge var, başlık yok; simgeler renklendirilmez
// (itemIconTint=@null). Seçili sekmenin arkasında çerçeveli yuvarlak kutu (bottom_nav_item_bg)
// ve seçimde simgenin kısa zıplaması (MainActivity.bounceBottomNavIcon).
struct AltCubuk: View {
    @Binding var secili: Sekme
    /// Öğretmende sohbet sekmesinin simgesi ve adı farklı (Sınıflarım).
    var ogretmen: Bool = false
    // Zaten açık olan sekmeye yeniden dokunuldu (Android: setOnItemSelectedListener her dokunuşta
    // çağrılır; harita sekmesinde MapFragment açıksa kısım seçimine döner).
    var ayniSekmeyeDokunuldu: (Sekme) -> Void = { _ in }

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Sekme.allCases) { sekme in
                AltCubukOgesi(
                    sekme: sekme,
                    ogretmen: ogretmen,
                    seciliMi: sekme == secili,
                    eylem: { dokun(sekme) }
                )
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: KabukOlculeri.altYukseklik)
        .background(Color("background_color"))
    }

    private func dokun(_ sekme: Sekme) {
        if sekme == secili {
            ayniSekmeyeDokunuldu(sekme)
        } else {
            secili = sekme
        }
    }
}

// Tek sekme düğmesi: simge üstten 11 pt boşlukla yerleşir (Android itemPaddingTop).
private struct AltCubukOgesi: View {
    let sekme: Sekme
    let ogretmen: Bool
    let seciliMi: Bool
    let eylem: () -> Void

    /// Simgenin zıplama ölçeği (Android: 0,85'ten 280 ms'de OvershootInterpolator(3) ile 1'e).
    @State private var olcek: CGFloat = 1.0

    var body: some View {
        Button(action: dokunuldu) {
            ZStack(alignment: .top) {
                seciliKutu
                Image(sekme.simgeAdi(ogretmen: ogretmen))
                    .renderingMode(.original)
                    .resizable()
                    .scaledToFit()
                    .frame(width: KabukOlculeri.altSimge, height: KabukOlculeri.altSimge)
                    .scaleEffect(olcek)
                    .padding(.top, KabukOlculeri.altSimgeUstBosluk)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(sekme.ad(ogretmen: ogretmen)))
        .accessibilityAddTraits(seciliMi ? AccessibilityTraits.isSelected : AccessibilityTraits())
    }

    /// bottom_nav_item_bg.xml (state_checked): 7/5 pt içeride, köşe 14, bright_background_color
    /// dolgu, 2 pt bottom_nav_selected_stroke çerçeve. Seçili olmayanda arka plan yok.
    @ViewBuilder
    private var seciliKutu: some View {
        if seciliMi {
            RoundedRectangle(cornerRadius: KabukOlculeri.seciliKutuKose)
                .fill(Color("bright_background_color"))
                .overlay(
                    RoundedRectangle(cornerRadius: KabukOlculeri.seciliKutuKose)
                        .strokeBorder(Color("bottom_nav_selected_stroke"), lineWidth: KabukOlculeri.seciliKutuCizgi)
                )
                .padding(.horizontal, KabukOlculeri.seciliKutuYatayPay)
                .padding(.vertical, KabukOlculeri.seciliKutuDikeyPay)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    /// Android: her seçimde (aynı sekme dahil) simge zıplar. Titreşim (Haptics.click) iOS'ta yok.
    private func dokunuldu() {
        Titresim.tik()
        olcek = 0.85
        DispatchQueue.main.async {
            withAnimation(Animation.spring(response: 0.28, dampingFraction: 0.45)) {
                olcek = 1.0
            }
        }
        eylem()
    }
}

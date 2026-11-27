import SwiftUI

// Alt gezinme çubuğu. Android activity_main.xml -> bottomNavigationID (menü: bottom_menu.xml).
// Altı sekme eşit genişlikte; yalnızca simge var, başlık yok; simgeler renklendirilmez.
struct AltCubuk: View {
    @Binding var secili: Sekme
    // Zaten açık olan sekmeye yeniden dokunuldu (Android: setOnItemSelectedListener her dokunuşta
    // çağrılır; harita sekmesinde MapFragment açıksa kısım seçimine döner).
    var ayniSekmeyeDokunuldu: (Sekme) -> Void = { _ in }

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Sekme.allCases) { sekme in
                AltCubukOgesi(sekme: sekme, seciliMi: sekme == secili, eylem: { dokun(sekme) })
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
    let seciliMi: Bool
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            VStack(spacing: 0) {
                Image(sekme.simgeAdi)
                    .renderingMode(.original)
                    .resizable()
                    .scaledToFit()
                    .frame(width: KabukOlculeri.altSimge, height: KabukOlculeri.altSimge)
                    .padding(.top, KabukOlculeri.altSimgeUstBosluk)
                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .opacity(seciliMi ? 1.0 : KabukOlculeri.seciliOlmayanOpaklik)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(sekme.ad))
        .accessibilityAddTraits(seciliMi ? AccessibilityTraits.isSelected : AccessibilityTraits())
    }
}

import SwiftUI

// Ders haritası sekmesinin giriş noktası.
//
// Android'deki akış: PartSelectionFragment (kısım seçimi) → MapFragment (ders listesi) →
// derse dokununca lesson_bottom_sheet (alt panel). Burada üçü tek kök görünümde toplanır:
//   - kısım seçilmemişse HaritaKisimSecimi,
//   - seçilmişse HaritaDersListesi,
//   - üstlerinde karartma + HaritaDersAltPaneli ve kısa bilgi balonu.
// Abaküs dersi, öğretici, yarış paneli gibi henüz yazılmamış ekranlar yer tutucu olarak açılır.
//
// Uygulama kabuğu bu görünümü parametresiz `HaritaEkrani()` ile ilk sekmeye gömer;
// ad ve imza değişmemelidir.

struct HaritaEkrani: View {
    @StateObject private var model = HaritaModeli()

    init() {}

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            icerik
            altPanelKatmani
            bildirimKatmani
        }
        .animation(.easeInOut(duration: 0.25), value: model.seciliPart)
        .animation(.easeOut(duration: 0.3), value: model.altPanel?.id)
        .animation(.easeInOut(duration: 0.2), value: model.bildirim)
        .fullScreenCover(item: $model.hedef) { hedef in
            HaritaYerTutucuEkrani(hedef: hedef)
        }
    }

    /// Kısım seçimi ya da seçili kısmın ders listesi (Android: slide_in/slide_out geçişi).
    @ViewBuilder
    private var icerik: some View {
        if model.seciliPart != nil {
            HaritaDersListesi(model: model)
                .transition(.move(edge: .trailing))
        } else {
            HaritaKisimSecimi(model: model)
                .transition(.move(edge: .leading))
        }
    }

    /// Android: scrimView (alfa 0.5; dokununca panel kapanır) + bottom sheet.
    @ViewBuilder
    private var altPanelKatmani: some View {
        if let panel = model.altPanel {
            Color.black.opacity(0.5)
                .ignoresSafeArea()
                .onTapGesture { model.altPaneliKapat() }
                .transition(.opacity)
                .zIndex(1)
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                HaritaDersAltPaneli(durum: panel, model: model)
            }
            .transition(.move(edge: .bottom))
            .zIndex(2)
        }
    }

    /// Android: Toast.
    @ViewBuilder
    private var bildirimKatmani: some View {
        if let bildirim = model.bildirim {
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                HaritaBildirimGorunumu(metin: bildirim.metin)
            }
            .allowsHitTesting(false)
            .transition(.opacity)
            .zIndex(3)
        }
    }
}

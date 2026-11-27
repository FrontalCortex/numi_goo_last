import SwiftUI

// Ders haritası sekmesinin giriş noktası.
//
// Android'deki akış: PartSelectionFragment (kısım seçimi) → MapFragment (alt alta ders kartları)
// → derse dokununca karta bağlı ders paneli (lesson_popover). Burada ikisi tek kök görünümde:
//   - kısım seçilmemişse HaritaKisimSecimi,
//   - seçilmişse HaritaDersListesi (ders paneli onun içinde, karta göre yerleşir),
//   - üstlerinde kısa bilgi balonu (Toast / LockedHint).
// Abaküs dersi, öğretici, yarış paneli gibi henüz yazılmamış ekranlar yer tutucu olarak açılır.
//
// `HaritaEkrani()` kendi modeliyle ve yer tutucularla çalışır (ekran galerisi). Uygulama kabuğu
// `HaritaEkrani(model:yerTutucuGoster:)` ile yöneticinin modelini verir; adlar değişmemelidir.

struct HaritaEkrani: View {
    @StateObject private var model: HaritaModeli
    /// false ise `model.hedef` dolunca yer tutucu AÇILMAZ; hedefi modeli veren taraf
    /// (Yonetim/UygulamaYoneticisi) `model.$hedef` üzerinden okur ve gerçek ekranı kendisi açar.
    private let yerTutucuGoster: Bool

    init() {
        self.init(model: nil, yerTutucuGoster: true)
    }

    /// Modeli dışarıdan alan biçim: yönetici oturum aynı modeli tutar (hedefi okumak, ders bitince
    /// `yenile()` çağırmak, galeride belirli bir durumda açmak için).
    init(model: HaritaModeli?, yerTutucuGoster: Bool) {
        let kullanilacak: HaritaModeli = model ?? HaritaModeli()
        _model = StateObject(wrappedValue: kullanilacak)
        self.yerTutucuGoster = yerTutucuGoster
    }

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            icerik
            bildirimKatmani
        }
        .animation(.easeInOut(duration: 0.25), value: model.seciliPart)
        .animation(.easeInOut(duration: 0.2), value: model.bildirim)
        .fullScreenCover(item: yerTutucuHedefi) { hedef in
            HaritaYerTutucuEkrani(hedef: hedef)
        }
    }

    /// Yer tutucu kapalıyken sunum hiç tetiklenmez (hedef dışarıda işlenir).
    private var yerTutucuHedefi: Binding<HaritaHedefi?> {
        let goster: Bool = yerTutucuGoster
        let kaynak: Binding<HaritaHedefi?> = $model.hedef
        return Binding<HaritaHedefi?>(
            get: { goster ? kaynak.wrappedValue : nil },
            set: { yeni in kaynak.wrappedValue = yeni }
        )
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

    /// Android: Toast (kısa) ya da LockedHint Snackbar'ı (alt kenarda).
    @ViewBuilder
    private var bildirimKatmani: some View {
        if let bildirim = model.bildirim {
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                HaritaBildirimGorunumu(bildirim: bildirim)
            }
            .allowsHitTesting(false)
            .transition(.opacity)
            .zIndex(3)
        }
    }
}

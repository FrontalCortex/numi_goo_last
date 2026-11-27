import SwiftUI

// YER TUTUCU (paket işi, bkz. ios/docs/OGRETICI_PLANI.md): kör ders anlatımlarının görselleri.
//
// Android: fragment_tutorial.xml blinding_tutorial1..3 ve rulesBookTutorial (alt ortada,
// tutorial_blinding_width × _height = 245 × 520, alt boşluk 2; varsayılan INVISIBLE). 24. öğretici
// bunları widgetOperations ile açar ve tutorialText'i görselin üstüne bağlar
// (model.adim.yerlesimler: "tutorialText:bottomToTop=blinding_tutorial1;…").
//
// Kullanılacaklar: model.sahne.gorunurluk("blinding_tutorial1", varsayilan: .gorunmez) …
// Görseller: Image("blinding_tutorial1") … (Gorseller/ içinde var).
struct OgreticiGorselAlani: View {
    @ObservedObject var model: OgreticiGorunumModeli

    var body: some View {
        EmptyView()
    }
}

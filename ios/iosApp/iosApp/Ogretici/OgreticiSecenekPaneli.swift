import SwiftUI

// YER TUTUCU (paket işi, bkz. ios/docs/OGRETICI_PLANI.md): çoktan seçmeli adımların paneli.
//
// Android: fragment_tutorial.xml optionsPanel (bottom_sheet_background, optionsTitleText,
// optionsRecyclerView + item_tutorial_option.xml, optionsCheckButton), TutorialOptionsAdapter,
// TutorialFragment.updateOptionsPanelForStep (adım gösterildikten 1 sn sonra alttan kayar; bu sürede
// geri ve "Eğitimi atla" kilitli), syncOptionsContentTextSizeStep (bütün şıklar tek yazı boyutunda).
//
// Kullanılacaklar: model.adim.gecerliSecenekVar / secenekler / cokluSecim / secenekBasligi,
// model.secilenSecenekler (işaretleme), model.secenekleriKontrolEt() ("Kontrol Et").
// Bu dosya doldurulana kadar seçenekli adımda ekrana dokunmak ilerletmez (Android ile aynı kural):
// o öğreticiler bu panel yazılmadan açılmamalı.
struct OgreticiSecenekPaneli: View {
    @ObservedObject var model: OgreticiGorunumModeli

    var body: some View {
        EmptyView()
    }
}

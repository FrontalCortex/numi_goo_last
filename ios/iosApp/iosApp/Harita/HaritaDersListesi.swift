import SwiftUI
import UIKit

// Seçili kısmın ders listesi (Android: fragment_map.xml + MapFragment.setupRecyclerView).
// Yapı: tam ekran kaydırılan liste (üst 16, alt 92 boşluk; dersler alt alta kartlar), üstünde
// derse dokununca karta bağlı ders paneli. Sabit bölüm başlığı ve zikzak yerleşim Android'de
// kaldırıldı; "Öğretmene sor" düğmesi TeacherQuestionsFeature.ENABLED = false olduğu için gizli.
// Geri düğmesi Android'de MainActivity araç çubuğundadır (lessonPartBackButton); kabuk çizer.

struct HaritaDersListesi: View {
    @ObservedObject var model: HaritaModeli

    /// true ise geri düğmesini kabuğun üst çubuğu çizer (Android'deki yeri).
    @Environment(\.haritaGeriKabukta) private var geriKabukta: Bool

    var body: some View {
        liste
            .overlayPreferenceValue(HaritaKartKonumAnahtari.self) { konumlar in
                GeometryReader { geo in
                    panelKatmani(konumlar: konumlar, geo: geo)
                }
                // Panel kapalıyken katman listeye gelen dokunuşları almasın.
                .allowsHitTesting(model.panel != nil)
            }
            .overlay(alignment: .topLeading) { geriKatmani }
            .background(Color("background_color"))
            .animation(Animation.easeOut(duration: 0.18), value: model.panel?.id)
    }

    // MARK: Liste

    private var liste: some View {
        ScrollView(.vertical, showsIndicators: false) {
            LazyVStack(spacing: 0) {
                ForEach(model.ogeler) { oge in
                    HaritaSatiri(oge: oge, dokun: { model.ogeyeDokun(oge) })
                }
            }
            .padding(.top, HaritaStil.listeUstBosluk)
            .padding(.bottom, HaritaStil.listeAltBosluk)
            .frame(maxWidth: .infinity)
        }
    }

    // MARK: Ders paneli

    @ViewBuilder
    private func panelKatmani(konumlar: [Int: Anchor<CGRect>], geo: GeometryProxy) -> some View {
        if let panel = model.panel {
            HaritaPanelKatmani(
                panel: panel,
                kart: kartCercevesi(panel: panel, konumlar: konumlar, geo: geo),
                alan: geo.size,
                model: model
            )
            .id(panel.id)
            .transition(AnyTransition.opacity)
        }
    }

    /// Kart listede görünmüyorsa (tembel listede kurulmamışsa) nil: panel ortada, oksuz açılır.
    private func kartCercevesi(panel: HaritaPanelDurumu, konumlar: [Int: Anchor<CGRect>], geo: GeometryProxy) -> CGRect? {
        guard let capa = konumlar[panel.id] else {
            return nil
        }
        let cerceve: CGRect = geo[capa]
        let gorunur: Bool = cerceve.maxY > 0 && cerceve.minY < geo.size.height
        return gorunur ? cerceve : nil
    }

    // MARK: Geri düğmesi (yalnızca kabuksuz açılışta)

    @ViewBuilder
    private var geriKatmani: some View {
        if !geriKabukta && model.panel == nil {
            Button(action: { model.kisimSeciminDon() }) {
                geriGorseli
                    .frame(width: HaritaStil.geriOkuBoyu, height: HaritaStil.geriOkuBoyu)
                    .frame(width: HaritaStil.geriDugmesiBoyu, height: HaritaStil.geriDugmesiBoyu)
                    .background(Circle().fill(Color("background_color").opacity(0.85)))
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .padding(.leading, 12)
            .padding(.top, 6)
            .accessibilityLabel(Text("Geri"))
        }
    }

    /// Görsel asset catalog'da yoksa geri dönüş yolu kaybolmasın diye sistem simgesi.
    @ViewBuilder
    private var geriGorseli: some View {
        if UIImage(named: "ic_back_arrow") != nil {
            Image("ic_back_arrow")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(.white)
        } else {
            Image(systemName: "chevron.left")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.white)
        }
    }
}

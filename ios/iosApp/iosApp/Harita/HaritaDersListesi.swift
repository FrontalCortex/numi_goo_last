import SwiftUI
import UIKit

// Seçili kısmın ders listesi (Android: fragment_map.xml + MapFragment.setupRecyclerView).
// Yapı: tam ekran kaydırılan liste, üstünde sabit duran ünite başlığı (stickyHeader),
// sağ ortada "öğretmene sor" düğmesi (askQuestionButton), sol üstte geri düğmesi
// (Android'de MainActivity araç çubuğundaki lessonPartBackButton).

/// Listenin en üstünde görünen satırın sırası (Android: findFirstVisibleItemPosition).
struct HaritaIlkGorunenAnahtari: PreferenceKey {
    static var defaultValue: Int = Int.max

    static func reduce(value: inout Int, nextValue: () -> Int) {
        value = min(value, nextValue())
    }
}

/// Sabit başlığın ölçülen yüksekliği (liste üst boşluğu buna göre verilir).
struct HaritaYapiskanYukseklikAnahtari: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = max(value, nextValue())
    }
}

struct HaritaDersListesi: View {
    @ObservedObject var model: HaritaModeli

    @State private var ilkGorunen: Int = 0
    @State private var yapiskanYukseklik: CGFloat = 81
    /// askQuestionButton'ın süzülme animasyonu başladı mı.
    @State private var suzuluyor: Bool = false
    /// true ise geri düğmesini kabuğun üst çubuğu çizer (Android'deki yeri).
    @Environment(\.haritaGeriKabukta) private var geriKabukta: Bool

    private let listeAlani = "haritaListesi"

    var body: some View {
        GeometryReader { geo in
            liste(genislik: geo.size.width)
                .overlay(alignment: .top) { ustKatman }
                .overlay(alignment: .trailing) { soruDugmesi }
        }
        .background(Color("background_color"))
    }

    // MARK: Liste

    private func liste(genislik: CGFloat) -> some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 0) {
                ForEach(model.ogeler) { oge in
                    satir(oge, genislik: genislik)
                }
            }
            // Android: liste sabit başlığın altından başlar (başlık yüksekliği + 16dp), altta 16dp.
            .padding(.top, yapiskanYukseklik + HaritaStil.listeUstBosluk)
            .padding(.bottom, 16)
            .frame(maxWidth: .infinity)
        }
        .coordinateSpace(name: listeAlani)
        .onPreferenceChange(HaritaIlkGorunenAnahtari.self) { deger in
            ilkGorunen = (deger == Int.max) ? 0 : deger
        }
    }

    private func satir(_ oge: HaritaOgesi, genislik: CGFloat) -> some View {
        HaritaSatiri(
            oge: oge,
            kayma: model.yatayKayma(oge, listeGenisligi: genislik),
            dokun: { model.ogeyeDokun(oge) }
        )
        .background(gorunurlukOlcer(sira: oge.id))
    }

    /// Satırın alt kenarı listenin üst kenarının altındaysa satır "görünür" sayılır;
    /// en küçük sıra, Android'deki findFirstVisibleItemPosition karşılığıdır.
    private func gorunurlukOlcer(sira: Int) -> some View {
        GeometryReader { geo in
            Color.clear.preference(
                key: HaritaIlkGorunenAnahtari.self,
                value: geo.frame(in: .named(listeAlani)).maxY > 0 ? sira : Int.max
            )
        }
    }

    // MARK: Sabit başlık ve geri düğmesi

    private var ustKatman: some View {
        ZStack(alignment: .topLeading) {
            yapiskanAlan
            if !geriKabukta {
                geriDugmesi
            }
        }
    }

    private var yapiskanAlan: some View {
        VStack(spacing: 0) {
            if let baslik = model.yapiskanBaslik(ilkGorunen: ilkGorunen) {
                HaritaYapiskanBaslikGorunumu(baslik: baslik)
            }
        }
        .frame(maxWidth: .infinity)
        .background(yukseklikOlcer)
        .onPreferenceChange(HaritaYapiskanYukseklikAnahtari.self) { deger in
            yapiskanYukseklik = deger
        }
    }

    private var yukseklikOlcer: some View {
        GeometryReader { geo in
            Color.clear.preference(key: HaritaYapiskanYukseklikAnahtari.self, value: geo.size.height)
        }
    }

    /// Kısım seçimine dönüş; yalnızca harita kabuksuz açıldığında (ekran galerisi) çizilir.
    /// Sabit başlığın solundaki 32pt'lik boşlukta durur.
    private var geriDugmesi: some View {
        Button(action: { model.kisimSeciminDon() }) {
            geriGorseli
                .frame(width: HaritaStil.geriOkuBoyu, height: HaritaStil.geriOkuBoyu)
                .frame(width: HaritaStil.yapiskanYatayBosluk, height: max(yapiskanYukseklik, 44))
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text("Geri"))
    }

    /// Görsel henüz asset catalog'a taşınmadıysa geri dönüş yolu kaybolmasın diye sistem simgesi.
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

    // MARK: Öğretmene sor düğmesi (askQuestionButton)

    private var soruDugmesi: some View {
        Button(action: { model.soruSorDokunuldu() }) {
            Image("teacher_ask_ic")
                .resizable()
                .scaledToFit()
                .padding(8)
                .frame(width: HaritaStil.soruDugmesiBoyu, height: HaritaStil.soruDugmesiBoyu)
                .background(Circle().fill(HaritaStil.soruDugmesiZemini))
        }
        .buttonStyle(.plain)
        .padding(.trailing, 6)
        .offset(x: 0, y: suzuluyor ? -HaritaStil.soruDugmesiSuzulme : 0)
        .animation(suzulmeAnimasyonu, value: suzuluyor)
        .onAppear {
            // Sonsuz animasyon ilk yerleşimle aynı işlemde başlarsa konuma da bulaşabiliyor.
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                suzuluyor = true
            }
        }
    }

    /// Android: ObjectAnimator.ofFloat(btn, "translationY", 0f, -20f, 0f), 1800 ms, sonsuz.
    private var suzulmeAnimasyonu: Animation {
        return Animation.easeInOut(duration: 0.9).repeatForever(autoreverses: true)
    }
}

/// fragment_map.xml: stickyHeader (corner_background + ünite rengi).
struct HaritaYapiskanBaslikGorunumu: View {
    let baslik: HaritaYapiskanBaslik

    var body: some View {
        VStack(spacing: 0) {
            Text(baslik.kisimUnite)
                .font(HaritaStil.yazi(16))
                .foregroundColor(HaritaStil.yapiskanUstYaziRengi)
                .multilineTextAlignment(.center)
                .padding(.top, 10)
                .padding(.leading, 5)
                .frame(maxWidth: .infinity)
            Text(baslik.baslik)
                .font(HaritaStil.yazi(27))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .padding(.leading, 6)
                .padding(.bottom, 12)
                .frame(maxWidth: .infinity)
        }
        .background(RoundedRectangle(cornerRadius: 16).fill(zemin))
        .padding(.horizontal, HaritaStil.yapiskanYatayBosluk)
    }

    /// Android: başlıkta renk yoksa geçerli renk korunur; burada nötr gri kullanılır.
    private var zemin: Color {
        if let ad = baslik.renk {
            return Color(ad)
        }
        return Color.gray
    }
}

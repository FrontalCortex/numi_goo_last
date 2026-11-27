import SwiftUI

// Öğreticinin abaküsü ve odak çerçevesi.
//
// Android: fragment_tutorial.xml'deki abacusContainer (273 × 290 dp; ortak AbakusGorunumu ile aynı
// ölçüler), focusView (tutorial_focus_border: 4 dp beyaz çizgi, 10 dp köşe; abacusContainer'ın sağ
// kenarına bağlı) ve abacusTapBlocker (abaküse dokunulamayan adımda dokunuş ekrana gider ve adımı
// ilerletir). Öğreticide boncuklar her zaman varsayılan soroban görünümündedir (setupBeads: kullanıcının
// özelleştirmesi yok sayılır).

struct OgreticiAbakusAlani: View {
    @ObservedObject var model: OgreticiGorunumModeli

    var body: some View {
        let boyut: CGSize = AbakusGorunumu.boyut(basamakSayisi: 5)
        let sahne: OgreticiSahneVerisi = model.sahne
        let cizilir: Bool = sahne.abakus.cizilir
        let dokunulur: Bool = cizilir && !sahne.abakusKilitli
        return ZStack(alignment: .topLeading) {
            AbakusGorunumu(denetleyici: model.abakus, basamakSayisi: 5, kilitli: sahne.abakusKilitli)
                .opacity(cizilir ? 1 : 0)
                // Kilitliyken dokunuş alttaki ekran katmanına geçer (abacusTapBlocker).
                .allowsHitTesting(dokunulur)
            if let odak = sahne.odak, cizilir {
                OgreticiOdakCercevesi(odak: odak, kap: boyut)
                    .allowsHitTesting(false)
                    .transition(.opacity)
            }
        }
        .frame(width: boyut.width, height: boyut.height)
    }
}

/// focusView: abaküs kabına göre konumlanan beyaz çerçeve.
struct OgreticiOdakCercevesi: View {
    let odak: OgreticiOdakVerisi
    let kap: CGSize

    /// tutorial_question_border_width
    static let cizgi: CGFloat = 4
    /// tutorial_focus_corner_radius
    static let kose: CGFloat = 10
    /// guideline2: layout_constraintVertical_bias="0.3", yükseklik 1 dp.
    static let altKilavuzOrani: CGFloat = 0.3

    var body: some View {
        let x: CGFloat = kap.width - odak.sagBosluk - odak.genislik
        let y: CGFloat = ustKenar
        return RoundedRectangle(cornerRadius: OgreticiOdakCercevesi.kose)
            .strokeBorder(Color.white, lineWidth: OgreticiOdakCercevesi.cizgi)
            .frame(width: odak.genislik, height: odak.yukseklik)
            .offset(x: x, y: y)
    }

    /// ConstraintLayout: üstü ve altı kaba bağlıysa ortalanır (üst boşluk yarısı kadar kaydırır);
    /// kılavuza bağlıysa kılavuzun üstünden başlar.
    private var ustKenar: CGFloat {
        switch odak.capa {
        case .orta:
            let bos: CGFloat = kap.height - odak.yukseklik
            return bos / 2 + odak.ustBosluk / 2
        case .ustKilavuz:
            return odak.ustBosluk
        case .altKilavuz:
            let kilavuz: CGFloat = (kap.height - 1) * OgreticiOdakCercevesi.altKilavuzOrani
            return kilavuz + odak.ustBosluk
        }
    }
}

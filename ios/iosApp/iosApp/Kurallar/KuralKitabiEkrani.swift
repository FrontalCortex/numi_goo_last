import SwiftUI

// Kural kitabı.
//
// Android: model/RulesFragment.kt + res/layout/fragment_rules.xml. Kök: 16 köşeli koyu kutu (result_box_green,
// backgroundTint #333C42; yeşil çerçeve tint'le silinir), sol üstte 40 dp beyaz çarpı (kenar boşluğu 10),
// altında kaydırılan içerik: bölüm başlığı (yeşil) + tablolar, aralarında 2 dp siyah bölücüler.
//
// Android'de sekme yoktur: hangi bölümün (toplama / çıkarma) ve hangi tabloların görüneceğini ders ekranı
// belirler (RulesFragment.setActiveRulesContentSection / update...Visibility). Bu yüzden `ayar`
// (bkz. KuralDersBilgisi.kitapAyari) dersten gelir; ders olmadan açılırken `baslangicSayfasi` seçilen
// bölümün bütün tablolarını gösterir.
//
// Açılış / kapanış animasyonu (slide_down / slide_up, 300 ms) ekranı barındıran katmandadır
// (KuralKatmani); burada yalnızca içerik vardır.
//
// Basitleştirme: Android `RulesFragment.lastScrollY` ile kapanırken kaydırma konumunu saklayıp bir sonraki
// açılışta geri yükler; SwiftUI (iOS 16) ScrollView'de konum okuma / yazma yok, kitap her açılışta
// başa dizilir.

/// Kitabın hangi bölümle açılacağı (ders olmadan).
enum KuralKitabiSayfasi {
    case toplama
    case cikarma
}

struct KuralKitabiEkrani: View {
    private let ayar: KuralKitabiAyari
    private let secildi: ((KuralSecimi) -> Void)?
    private let kapat: () -> Void

    /// - Parameters:
    ///   - baslangicSayfasi: ders verilmediğinde açılacak bölüm; `ayar` verilmişse yok sayılır.
    ///   - ayar: dersin açtığı tablolar (KuralDersBilgisi.kitapAyari()); nil ise seçilen bölümün hepsi.
    ///   - secildi: tablo seçici açıkken dokunulan tablo; nil ise dokunuş hiçbir şey yapmaz.
    ///   - kapat: çarpı düğmesine basılınca.
    init(
        baslangicSayfasi: KuralKitabiSayfasi = .toplama,
        ayar: KuralKitabiAyari? = nil,
        secildi: ((KuralSecimi) -> Void)? = nil,
        kapat: @escaping () -> Void
    ) {
        if let verilen = ayar {
            self.ayar = verilen
        } else {
            self.ayar = KuralKitabiAyari.hepsi(baslangicSayfasi == .toplama ? .toplama : .cikarma)
        }
        self.secildi = secildi
        self.kapat = kapat
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(KuralRenk.kitapZemin)
            ScrollView(.vertical, showsIndicators: true) {
                KuralKitapIcerigi(ayar: ayar, secildi: secildi)
            }
            cikisDugmesi
        }
    }

    /// quitButton: rules_fragment_quit_size (40), kenar boşluğu 10, beyaz tint.
    private var cikisDugmesi: some View {
        Button(action: kapat) {
            Image("cross")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(Color.white)
                .frame(width: 40, height: 40)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(10)
    }
}

enum KuralRenk {
    /// fragment_rules.xml: backgroundTint #333C42.
    static let kitapZemin: Color = AbakusRenk.renk(0x333C42)
}

// MARK: - İçerik

/// Kaydırılan içerik: tablolar ve bölücüler, Android'deki sırayla.
struct KuralKitapIcerigi: View {
    let ayar: KuralKitabiAyari
    let secildi: ((KuralSecimi) -> Void)?

    var body: some View {
        let ogeler: [KuralKitapOgesi] = ayar.ogeler()
        VStack(spacing: 0) {
            ForEach(ogeler) { (oge: KuralKitapOgesi) in
                KuralKitapOgeGorunumu(oge: oge, ayar: ayar, secildi: secildi)
            }
        }
        .frame(maxWidth: .infinity)
    }
}

struct KuralKitapOgeGorunumu: View {
    let oge: KuralKitapOgesi
    let ayar: KuralKitabiAyari
    let secildi: ((KuralSecimi) -> Void)?

    var body: some View {
        switch oge.tur {
        case .tablo(let secim):
            KuralKitapTablosu(secim: secim, etiket: ayar.etiket, secilebilir: ayar.tabloSecici, secildi: secildi)
                .padding(.top, oge.ustBosluk)
                .padding(.bottom, oge.altBosluk)
        case .bolucu(let gorunurluk):
            KuralBolucu(gorunurluk: gorunurluk)
        case .bosluk:
            Color.clear.frame(height: oge.altBosluk)
        }
    }
}

/// Bir tablo, yatayda ortalı. Seçici açıksa dokunuş `secildi`'yi çağırır (setRulesTablePickerClick).
struct KuralKitapTablosu: View {
    let secim: KuralSecimi
    let etiket: String?
    let secilebilir: Bool
    let secildi: ((KuralSecimi) -> Void)?

    var body: some View {
        icerik
            .contentShape(Rectangle())
            .onTapGesture {
                if secilebilir {
                    secildi?(secim)
                }
            }
            .frame(maxWidth: .infinity)
    }

    @ViewBuilder
    private var icerik: some View {
        if case .carpim(let basamak) = secim {
            KuralCarpimTablosu(basamak: basamak)
        } else if let tanim = KuralTabloVerisi.kitap(secim, etiket: etiket) {
            KuralTabloGorunumu(tanim: tanim)
        }
    }
}

/// view1...view14: 2 dp siyah çizgi, alt-üst 20, yanlardan 30 (INVISIBLE yer tutar, çizilmez).
struct KuralBolucu: View {
    let gorunurluk: KuralGorunurluk

    var body: some View {
        Rectangle()
            .fill(Color.black)
            .frame(height: 2)
            .padding(.horizontal, 30)
            .padding(.vertical, 20)
            .opacity(gorunurluk == .gizli ? 0 : 1)
    }
}

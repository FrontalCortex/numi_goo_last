import Shared
import SwiftUI
import UIKit

// "Başarılar" ekranı: kişisel rekorlar şeridi ve rozet ızgarası.
//
// Android karşılığı: BadgeDetailFragment.kt + res/layout/fragment_badge_detail.xml,
// item_badge_detail_record.xml, item_badge_detail_award.xml.
// Profildeki rozetler kartına (ya da okuna) basınca açılır (ProfileFragment.openBadgeDetailFragment).
// Bir rozete basınca tek rozet ekranı açılır (Android BadgeFragment → burada RozetDetayEkrani);
// geçiş `rozetSecildi` ile dışarı verilir.
// Sıralama, başlıklar, alt yazılar ve tonlar ortak koddadır (BadgeDetailLogic, BadgeProgressEngine).

/// Listeden seçilen rozet; `RozetDetayEkrani`'ne verilir.
struct RozetSecimi: Equatable, Identifiable {
    /// Ortak kod `BadgeAnimMode` adı (ör. "DINO", "GOLD_MEDAL").
    let modAdi: String
    let acik: Bool

    var id: String { modAdi }
}

/// Listedeki bir rozet kutusunun ekran durumu.
struct RozetKutuDurumu: Identifiable, Equatable {
    let modAdi: String
    let baslik: String
    let altYazi: String
    let degerMetni: String
    let degerGorunur: Bool
    let acik: Bool
    /// "BRONZE" / "SILVER" / "ORIGINAL" / "RED" / "PURPLE" ya da boş.
    let ton: String
    /// Alt (taban) animasyon dosyası; boş = taban yok (madalya, kupa, kuşak).
    let tabanDosyasi: String
    let ustDosyasi: String
    let ustOlcek: CGFloat
    /// Üst animasyonun durduğu kare (Android topHoldFrame): 0 = ilk kare, 9999 = son kare.
    let ustTutmaKaresi: CGFloat

    var id: String { modAdi }
}

final class RozetlerModeli: ObservableObject {
    @Published private(set) var rekorlar: [RozetKutuDurumu] = []
    @Published private(set) var rozetSatirlari: [[RozetKutuDurumu]] = []
    @Published private(set) var satirBoyu: Int = 3

    private let depo: KupaRozetDeposu
    private let kopru: KupaEkranKoprusu = KupaEkranKoprusu.shared

    init(depo: KupaRozetDeposu) {
        self.depo = depo
    }

    /// Önce bellekteki durumla çizer, sonra `badgeProgress/state` dokümanını okuyup yeniler.
    func yukle() {
        listeleriKur()
        depo.rozetDurumunuGetir { [weak self] dokuman in
            guard let self = self else { return }
            self.kopru.rozetDurumunuYukle(doc: dokuman)
            self.listeleriKur()
        }
    }

    private func listeleriKur() {
        rekorlar = kopru.kisiselRekorlar().map { RozetlerModeli.durum($0) }
        let hepsi: [RozetKutuDurumu] = kopru.rozetler().map { RozetlerModeli.durum($0) }
        let boy: Int = max(1, Int(kopru.rozetSatirBoyu()))
        satirBoyu = boy
        var satirlar: [[RozetKutuDurumu]] = []
        var i: Int = 0
        while i < hepsi.count {
            let son: Int = min(i + boy, hepsi.count)
            satirlar.append(Array(hepsi[i..<son]))
            i = son
        }
        rozetSatirlari = satirlar
    }

    private static func durum(_ v: RozetKutuVerisi) -> RozetKutuDurumu {
        return RozetKutuDurumu(
            modAdi: v.modeName,
            baslik: v.title,
            altYazi: v.subtext,
            degerMetni: v.valueText,
            degerGorunur: v.showCount,
            acik: v.unlocked,
            ton: v.toneName,
            tabanDosyasi: v.baseAsset,
            ustDosyasi: v.topAsset,
            ustOlcek: CGFloat(v.topScale),
            ustTutmaKaresi: CGFloat(v.topHoldFrame)
        )
    }
}

struct RozetlerEkrani: View {
    private let kapat: (() -> Void)?
    private let rozetSecildi: ((RozetSecimi) -> Void)?

    @StateObject private var model: RozetlerModeli

    /// - Parameters:
    ///   - kapat: geri düğmesi.
    ///   - rozetSecildi: bir rozete basınca çağrılır (tek rozet ekranına geçiş için).
    ///   - ortam: veri servisleri (varsayılan: örnek veri).
    init(
        kapat: (() -> Void)?,
        rozetSecildi: ((RozetSecimi) -> Void)?,
        ortam: KupaOrtami = KupaOrtami.sahte
    ) {
        self.kapat = kapat
        self.rozetSecildi = rozetSecildi
        _model = StateObject(wrappedValue: RozetlerModeli(depo: ortam.rozetDeposu))
    }

    var body: some View {
        VStack(spacing: 0) {
            ustCubuk
            Rectangle()
                .fill(KupaStil.renk(0x2D404D))
                .frame(height: 2)
            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(KupaMetin.kisiselRekorlar)
                        .font(Tema.yazi(22))
                        .foregroundColor(.white)
                        .padding(.bottom, 14)
                    rekorSeridi
                    Text(KupaMetin.rozetler)
                        .font(Tema.yazi(22))
                        .foregroundColor(.white)
                        .padding(.top, 24)
                    rozetIzgarasi
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
                .padding(.bottom, 28)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color("background_color").ignoresSafeArea())
        .onAppear {
            model.yukle()
        }
    }

    private var ustCubuk: some View {
        ZStack {
            Text(KupaMetin.basarilar)
                .font(Tema.yazi(20))
                .foregroundColor(KupaStil.renk(0xDCE6ED))
            HStack(spacing: 0) {
                KupaGeriDugmesi(boyut: 40, icBosluk: 8, renk: KupaStil.solukYazi, eylem: { kapat?() })
                Spacer(minLength: 0)
            }
        }
        .padding(.horizontal, 12)
        .frame(height: 56)
    }

    private var rekorSeridi: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(model.rekorlar) { rozet in
                    Button(action: { sec(rozet) }) {
                        RozetRekorKarti(rozet: rozet)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.vertical, 1)
        }
    }

    private var rozetIzgarasi: some View {
        VStack(spacing: 0) {
            ForEach(Array(model.rozetSatirlari.enumerated()), id: \.offset) { satir in
                rozetSatiri(satir.element)
            }
        }
    }

    private func rozetSatiri(_ rozetler: [RozetKutuDurumu]) -> some View {
        let eksik: Int = max(0, model.satirBoyu - rozetler.count)
        return HStack(alignment: .top, spacing: 0) {
            ForEach(rozetler) { rozet in
                Button(action: { sec(rozet) }) {
                    RozetIzgaraKutusu(rozet: rozet)
                }
                .buttonStyle(.plain)
                .frame(maxWidth: .infinity)
            }
            // Eksik hücreler boşlukla doldurulur ki son satırdaki rozetler sola hizalı kalsın.
            ForEach(0..<eksik, id: \.self) { _ in
                Color.clear
                    .frame(maxWidth: .infinity)
                    .frame(height: 0)
            }
        }
    }

    private func sec(_ rozet: RozetKutuDurumu) {
        rozetSecildi?(RozetSecimi(modAdi: rozet.modAdi, acik: rozet.acik))
    }
}

// MARK: - Rozet görseli

/// Rozetin durağan görünümü: taban + üst animasyon (Android'de üst üste iki LottieAnimationView;
/// BadgeDetailFragment.bindSingleBadge).
///
/// - Taban son karesinde sabit durur (Android progress = 1 + pauseAnimation). Tabansız rozetlerde
///   (madalya, kupa, siyah kuşak) Android tabanı INVISIBLE yapar; burada çizilmez.
/// - Üst animasyon tutma karesinde sabit durur (Android topHoldFrame; 9999 = son kare).
/// - Kilitli rozet düz griye boyanır (taban badge_locked_base, üst badge_locked_top); açık rozette
///   taban kademe tonuyla renklenir (`KupaRozetTonu`).
struct RozetGorseli: View {
    let tabanDosyasi: String
    let ustDosyasi: String
    let ustOlcek: CGFloat
    /// Android topHoldFrame.
    let ustTutmaKaresi: CGFloat
    let acik: Bool
    let ton: String
    /// Çerçevenin ve taban Lottie'sinin kenarı (rozet şekli tuvalin ortasındaki ~%43'tür).
    let tabanBoyutu: CGFloat
    /// Üst Lottie'nin ölçeklenmeden önceki kenarı.
    let ustBoyutu: CGFloat

    private var ustKenar: CGFloat {
        return ustBoyutu * ustOlcek
    }

    var body: some View {
        ZStack {
            taban
            KupaAnimasyonGorunumu(
                dosya: ustDosyasi,
                duzRenk: acik ? nil : Color("badge_locked_top"),
                sabitKare: Double(ustTutmaKaresi)
            )
            .frame(width: ustKenar, height: ustKenar)
            // Android: top.translationY = −1dp.
            .offset(y: -1)
        }
        .frame(width: tabanBoyutu, height: tabanBoyutu)
        // Çerçeve hücresinden taşar; dokunuşu hücrenin kendisi alır.
        .allowsHitTesting(false)
    }

    @ViewBuilder
    private var taban: some View {
        if !tabanDosyasi.isEmpty {
            KupaAnimasyonGorunumu(
                dosya: tabanDosyasi,
                duzRenk: acik ? nil : Color("badge_locked_base"),
                sabitKare: KupaStil.sonKare
            )
            .modifier(KupaRozetTonu(ton: acik ? ton : ""))
            .frame(width: tabanBoyutu, height: tabanBoyutu)
        }
    }
}

/// Rozetin üzerindeki çerçeveli sayı: açıkken beyaz + turuncu çerçeve, kilitliyken gri tonlar.
struct RozetDegerYazisi: View {
    let metin: String
    let boyut: CGFloat
    let acik: Bool

    var body: some View {
        KupaCerceveliYazi(
            metin: metin,
            boyut: boyut,
            dolgu: acik ? Color.white : Color("badge_locked_base"),
            cerceve: acik ? KupaStil.sayiCercevesi : Color("badge_locked_top")
        )
    }
}

// MARK: - Kutular

/// item_badge_detail_record.xml: 164×260 çerçeveli kart. recordBadgeFrame 120dp; taban ve üst
/// animasyon çerçevenin tamamını kaplar (match_parent).
private struct RozetRekorKarti: View {
    let rozet: RozetKutuDurumu

    var body: some View {
        VStack(spacing: 0) {
            RozetGorseli(
                tabanDosyasi: rozet.tabanDosyasi,
                ustDosyasi: rozet.ustDosyasi,
                ustOlcek: rozet.ustOlcek,
                ustTutmaKaresi: rozet.ustTutmaKaresi,
                acik: rozet.acik,
                ton: rozet.ton,
                tabanBoyutu: 120,
                ustBoyutu: 120
            )
            // Sayı gösterilmeyen rozetlerde de yer tutulur (Android: View.INVISIBLE).
            RozetDegerYazisi(metin: rozet.degerMetni, boyut: 38, acik: rozet.acik)
                .opacity(rozet.degerGorunur ? 1 : 0)
                .padding(.top, -16)
            Text(rozet.baslik)
                .font(Tema.yazi(14))
                .foregroundColor(KupaStil.renk(0xE5EDF2))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .frame(maxWidth: .infinity)
                .padding(.top, 8)
            Text(rozet.altYazi)
                .font(Tema.yazi(13))
                .foregroundColor(KupaStil.renk(0x6F8595))
                .lineLimit(1)
                .frame(maxWidth: .infinity)
                .padding(.top, 4)
            Spacer(minLength: 0)
        }
        .padding(.top, 12)
        .padding(.bottom, 10)
        .frame(width: 164, height: 260)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color("background_color")))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(KupaStil.renk(0x425563), lineWidth: 2))
        .contentShape(Rectangle())
    }
}

/// item_badge_detail_award.xml: ızgaradaki rozet (sütun genişliğinin 1/3'ü).
///
/// awardBadgeFrame 200×200dp'dir ve hücreden (~1/3 ekran) geniştir: Android'de ortalanıp iki yana
/// taşar (clipChildren=false). Taban 200dp, üst animasyon 100dp × ölçek. Sayı çerçevenin altına
/// 72dp bindirilir (layout_marginTop=−72dp); alt yazı başlığa 8dp biner.
private struct RozetIzgaraKutusu: View {
    let rozet: RozetKutuDurumu

    private var gorsel: RozetGorseli {
        return RozetGorseli(
            tabanDosyasi: rozet.tabanDosyasi,
            ustDosyasi: rozet.ustDosyasi,
            ustOlcek: rozet.ustOlcek,
            ustTutmaKaresi: rozet.ustTutmaKaresi,
            acik: rozet.acik,
            ton: rozet.ton,
            tabanBoyutu: 200,
            ustBoyutu: 100
        )
    }

    var body: some View {
        VStack(spacing: 0) {
            // Yerleşimde yalnızca yükseklik tutulur; 200dp'lik görsel bindirme olarak çizilir ki
            // hücrenin genişliğini büyütmesin.
            Color.clear
                .frame(maxWidth: .infinity)
                .frame(height: 200)
                .overlay(gorsel)
            if rozet.degerGorunur {
                RozetDegerYazisi(metin: rozet.degerMetni, boyut: 36, acik: rozet.acik)
                    .padding(.top, -72)
            }
            RozetIzgaraBasligi(metin: rozet.baslik)
            Text(rozet.altYazi)
                .font(Tema.yazi(16))
                .foregroundColor(KupaStil.renk(0x6F8595))
                .lineLimit(1)
                .frame(maxWidth: .infinity)
                .padding(.top, -8)
        }
        .padding(.bottom, 18)
        .frame(maxWidth: .infinity)
        .contentShape(Rectangle())
    }
}

/// awardBadgeTitle: 20sp, ortalı, en çok 2 satır, lineSpacingMultiplier=0.6 (iki satıra bölünen
/// başlıkta satırlar sıkışıktır).
///
/// SwiftUI `Text` satır aralığını daraltamaz; bu yüzden başlık tek satıra sığıyorsa tek `Text`,
/// sığmıyorsa ve iki kelimeyse kelimeler alt alta, satır yüksekliğinin 0.4'ü kadar bindirilerek
/// çizilir (Android'deki satır kırılımıyla aynı sonuç).
private struct RozetIzgaraBasligi: View {
    let metin: String

    private let renk: Color = KupaStil.renk(0xE5EDF2)

    private var kelimeler: [String] {
        return metin.split(separator: " ").map { String($0) }
    }

    /// İki satır arasındaki (eksi) boşluk: satır yüksekliği × (0.6 − 1).
    private var satirAraligi: CGFloat {
        let satirYuksekligi: CGFloat = Tema.uiYazi(20).lineHeight
        return -0.4 * satirYuksekligi
    }

    var body: some View {
        ViewThatFits(in: .horizontal) {
            tekSatir
            cokSatir
        }
        .frame(maxWidth: .infinity)
    }

    private var tekSatir: some View {
        Text(metin)
            .font(Tema.yazi(20))
            .foregroundColor(renk)
            .lineLimit(1)
            .fixedSize()
    }

    @ViewBuilder
    private var cokSatir: some View {
        if kelimeler.count == 2 {
            VStack(spacing: satirAraligi) {
                kelime(kelimeler[0])
                kelime(kelimeler[1])
            }
        } else {
            Text(metin)
                .font(Tema.yazi(20))
                .foregroundColor(renk)
                .multilineTextAlignment(.center)
                .lineLimit(2)
        }
    }

    private func kelime(_ yazi: String) -> some View {
        Text(yazi)
            .font(Tema.yazi(20))
            .foregroundColor(renk)
            .lineLimit(1)
            .minimumScaleFactor(0.7)
    }
}

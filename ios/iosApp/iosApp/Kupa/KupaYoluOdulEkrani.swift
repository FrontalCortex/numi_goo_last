import Combine
import Shared
import SwiftUI

// Kupa Yolu ödül yolu ("Trophy Road") ekranı.
//
// Android karşılığı: CupPathRoadFragment.kt + res/layout/fragment_cup_path_road.xml,
// item_cup_path_milestone.xml, drawable/bg_cup_path_* ; veri: CupPathRewardRepository.kt.
// Kupa Yolu panelindeki karttan (çubuğa ya da hazır olmayan sandığa dokununca) açılır.
//
// Bir kupa yolunda kullanıcının nerede olduğunu ve ileride hangi kupa sayılarında sandık kazanacağını
// gösterir. Kupa AŞAĞIDAN YUKARI artar: ilk eşik (300) en altta, liste kullanıcının bulunduğu yerde açılır.
// Eşik matematiği tamamen ortak koddadır (CupPathRewardRepository: milestones, rarityOf, label ...);
// burada yalnızca çizim vardır. Asıl doğrulama sunucudadır (claimCupPathChest): ekrandaki veri
// eskimiş olsa bile hak edilmemiş sandık açılamaz.
//
// Hazır sandığa dokunmak sandığı açar: sandık ekranı `Sandik/` içindedir ve dışarıdan `sandikAc` ile
// çağrılır; sandık kapanınca bu ekran geri gelir (Android geri yığını) ve veriyi tazeler (onResume).
// Birden fazla eşik birikmişse hangisine dokunulursa dokunulsun SIRADAKİ eşik alınır (sunucu her
// çağrıda yalnızca sıradaki eşiği verir); sandıklar aynı olduğu için bu fark edilmez.

/// Bir eşiğin durumu (ortak kod: `CupPathRewardRepository.MilestoneStatus`).
enum KupaEsikDurumu: Equatable {
    case alindi
    case hazir
    case kilitli
}

/// Yoldaki tek bir eşik satırı.
struct KupaEsikSatiri: Identifiable, Equatable {
    /// Eşiğin kupa sayısı (300, 400 ...).
    let deger: Int
    let durum: KupaEsikDurumu
    /// Kapalı ve açık sandık çizimleri (1000'in katı destansı, 500'ün katı ender, gerisi sıradan).
    let kapaliSandik: String
    let acikSandik: String
    /// "Şu an buradasın" etiketi bu satırda mı (sıradaki alınmamış eşik; yol bittiyse hiçbirinde).
    let simdi: Bool
    /// Yalnızca `simdi` satırında: şeridin dolan kısmı, 0...1.
    let oran: Double

    var id: Int { deger }
}

final class KupaYoluOdulModeli: ObservableObject {
    @Published private(set) var yukleniyor: Bool = true
    @Published private(set) var baslik: String = ""
    @Published private(set) var skor: Int = 0
    /// Artan sırada (300 en başta). Ekran bunu ters çevirerek çizer.
    @Published private(set) var satirlar: [KupaEsikSatiri] = []
    /// İlk açılışta bir kez ayarlanır: kaydırılacak eşik (bulunulan eşiğin bir altındaki satır).
    @Published private(set) var kaydirmaHedefi: Int? = nil
    @Published private(set) var toast: String? = nil

    private let alan: String
    private let ortam: KupaOrtami
    private var kaydirildi: Bool = false
    private var toastSayaci: Int = 0

    init(kupaAlani: String, ortam: KupaOrtami) {
        // Android onCreate: bilinmeyen alan toplama yoluna düşer.
        let gecerli: Bool = CupPathRewardRepository.shared.FIELDS.contains(kupaAlani)
        self.alan = gecerli ? kupaAlani : CupPathRewardRepository.shared.FIELD_ADDITION
        self.ortam = ortam
        self.baslik = KupaYoluOdulModeli.baslikBul(self.alan)
    }

    var kupaAlani: String { alan }

    /// Android CupPathRewardRepository.titleOf: kartların başlığı (panel_cup_path_cardN_title).
    private static func baslikBul(_ alan: String) -> String {
        for kart in CupPathLogic.shared.CARDS where kart.field == alan {
            return Metin.al("panel_cup_path_card" + String(kart.cardNumber) + "_title")
        }
        return Metin.al("fragment_cup_path_road_tv_cup_path_road_title")
    }

    /// Android loadState (onResume): iki dokümanı okur ve çizer.
    func yukle() {
        let alanAdi: String = alan
        ortam.yolBelgeDeposu.belgeleriGetir { [weak self] kupaBelgesi, defter in
            guard let self = self else { return }
            let durumlar = CupPathRewardRepository.shared.buildStates(cupDoc: kupaBelgesi, ledgerDoc: defter)
            let durum: KupaYolDurumu = durumlar[alanAdi] ?? CupPathRewardRepository.shared.defaultState(cupField: alanAdi)
            self.ciz(durum)
        }
    }

    /// Android render.
    private func ciz(_ durum: KupaYolDurumu) {
        yukleniyor = false
        skor = Int(durum.cupScore)
        let esikler = durum.milestones(future: CupPathRewardRepository.shared.FUTURE_MILESTONES)
        var yeni: [KupaEsikSatiri] = []
        for esik in esikler {
            yeni.append(satirKur(esik.cupValue, esik.status, durum))
        }
        satirlar = yeni
        if !kaydirildi && !yeni.isEmpty {
            kaydirildi = true
            // Kullanıcının bulunduğu eşik: ilk alınmamış olan; hepsi alınmışsa liste sonu.
            var hedef: Int = yeni.count - 1
            for (sira, s) in yeni.enumerated() where s.durum != KupaEsikDurumu.alindi {
                hedef = sira
                break
            }
            // Bir satırlık pay: kullanıcı geçtiği eşiği de görsün, yolun yukarı devam ettiği anlaşılsın.
            kaydirmaHedefi = yeni[max(0, hedef - 1)].deger
        }
    }

    private func satirKur(_ deger: Int32, _ durum: CupPathRewardRepository.MilestoneStatus, _ yol: KupaYolDurumu) -> KupaEsikSatiri {
        let nadirlik: ChestRarity = CupPathRewardRepository.shared.rarityOf(milestone: deger)
        var esikDurumu: KupaEsikDurumu = KupaEsikDurumu.kilitli
        if durum == CupPathRewardRepository.MilestoneStatus.claimed {
            esikDurumu = KupaEsikDurumu.alindi
        } else if durum == CupPathRewardRepository.MilestoneStatus.claimable {
            esikDurumu = KupaEsikDurumu.hazir
        }
        let simdi: Bool = !yol.finished && deger == yol.nextMilestone
        return KupaEsikSatiri(
            deger: Int(deger),
            durum: esikDurumu,
            kapaliSandik: nadirlik.drawableRes,
            acikSandik: nadirlik.openDrawableRes,
            simdi: simdi,
            oran: simdi ? Double(yol.fraction) : 0
        )
    }

    /// Bir eşiğe dokunuldu. Hazırsa açılacak sandığın girdisini döndürür; alınmış / kilitli eşikte uyarı gösterir.
    func esigeDokunuldu(_ satir: KupaEsikSatiri) -> SandikGirdisi? {
        switch satir.durum {
        case .hazir:
            // Eşiğin nadirliği: sunucu da aynı kuralı uygular; buradaki değer yalnızca sandık ekranının
            // hangi seviyeden BAŞLAYACAĞINI belirler.
            let nadirlik: ChestRarity = CupPathRewardRepository.shared.rarityOf(milestone: Int32(satir.deger))
            return SandikGirdisi(
                baslangicNadirligi: nadirlik.name,
                reklamNonce: nil,
                kupaAlani: alan,
                kupaEsigi: satir.deger
            )
        case .alindi:
            toastGoster(Metin.al("ui_bu_sandigi_zaten_aldin"))
            return nil
        case .kilitli:
            toastGoster(Metin.al("ui_x_kupaya_ulasinca_acilir", satir.deger))
            return nil
        }
    }

    /// Android Toast.LENGTH_SHORT (≈ 2 sn).
    private func toastGoster(_ metin: String) {
        toast = metin
        toastSayaci += 1
        let sayac: Int = toastSayaci
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            guard let self = self, self.toastSayaci == sayac else { return }
            self.toast = nil
        }
    }
}

struct KupaYoluOdulEkrani: View {
    private let kapat: (() -> Void)?
    private let sandikAc: ((SandikGirdisi, @escaping (SandikSonucu) -> Void) -> Void)?

    @StateObject private var model: KupaYoluOdulModeli

    /// - Parameters:
    ///   - kupaAlani: kupa yolunun alanı (ör. "addition_abacus_cup"; ortak kod `CupPathCard.field`).
    ///     Bilinmeyen alan toplama yoluna düşer (Android).
    ///   - kapat: geri düğmesi.
    ///   - sandikAc: hazır eşiğe dokununca çağrılır; sandık girdisi ve sandık kapanınca çağrılacak
    ///     kapanış verilir (Android: NewChestFragment, geri yığınına eklenir). nil ise dokunma bir şey açmaz.
    ///   - ortam: veri servisleri (varsayılan: örnek veri).
    init(
        kupaAlani: String,
        kapat: (() -> Void)?,
        sandikAc: ((SandikGirdisi, @escaping (SandikSonucu) -> Void) -> Void)? = nil,
        ortam: KupaOrtami = KupaOrtami.sahte
    ) {
        self.kapat = kapat
        self.sandikAc = sandikAc
        _model = StateObject(wrappedValue: KupaYoluOdulModeli(kupaAlani: kupaAlani, ortam: ortam))
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            Color("background_color").ignoresSafeArea()
            VStack(spacing: 0) {
                ustCubuk
                durumSatiri
                Rectangle()
                    .fill(KupaStil.izgaraCizgisi)
                    .frame(height: 1)
                liste
            }
            if let metin = model.toast {
                KupaToast(metin: metin)
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: model.toast != nil)
        .onAppear {
            model.yukle()
        }
    }

    /// fragment_cup_path_road.xml üst çubuğu: 56dp, geri düğmesi 48dp, ortalı başlık (en çok 2 satır).
    private var ustCubuk: some View {
        HStack(spacing: 0) {
            KupaGeriDugmesi(boyut: 48, icBosluk: 12, renk: .white, eylem: { kapat?() })
            Text(model.baslik)
                .font(Tema.yazi(18))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .frame(maxWidth: .infinity)
            Color.clear.frame(width: 48, height: 48)
        }
        .padding(.leading, 4)
        .padding(.trailing, 16)
        .frame(height: 56)
    }

    /// Güncel durum: 40dp kupa simgesi ve 30sp kupa sayısı.
    private var durumSatiri: some View {
        HStack(spacing: 12) {
            Image("infinity_cup_ic")
                .resizable()
                .scaledToFit()
                .frame(width: 40, height: 40)
            Text(String(model.skor))
                .font(Tema.yazi(30))
                .foregroundColor(.white)
            Spacer(minLength: 0)
        }
        .padding(.leading, 20)
        .padding(.trailing, 20)
        .padding(.top, 4)
        .padding(.bottom, 12)
    }

    private var liste: some View {
        ZStack {
            if model.yukleniyor {
                ProgressView()
            } else {
                kaydirmaAlani
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// Ters dizilim (Android reverseLayout): en büyük eşik üstte, 300 en altta; ilk açılışta bulunulan
    /// eşik alt kenara oturur (scrollToPositionWithOffset(index − 1, 0)).
    private var kaydirmaAlani: some View {
        ScrollViewReader { vekil in
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    ForEach(model.satirlar.reversed()) { satir in
                        KupaEsikSatiriGorunumu(satir: satir, skor: model.skor, dokunuldu: { esigeDokunuldu(satir) })
                            .id(satir.deger)
                    }
                }
                .padding(.bottom, 16)
            }
            .onAppear {
                // Liste, veri geldikten SONRA kurulur; hedef o anda hazırdır (onChange burada tetiklenmezdi).
                guard let hedef = model.kaydirmaHedefi else { return }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    vekil.scrollTo(hedef, anchor: .bottom)
                }
            }
        }
    }

    private func esigeDokunuldu(_ satir: KupaEsikSatiri) {
        guard let girdi = model.esigeDokunuldu(satir) else { return }
        let nesne: KupaYoluOdulModeli = model
        sandikAc?(girdi) { _ in
            // Sandık kapandı: defter değişmiş olur, ekran tazelenir (Android onResume).
            nesne.yukle()
        }
    }
}

// MARK: - Satır

/// item_cup_path_milestone.xml: 76dp yüksek satır. Soldan sağa: yol şeridi (4dp çizgi + 16dp eşik noktası, 44dp
/// genişlik), eşiğin kupa sayısı (şeritten 34dp sonra), en sağda 60dp sandık kutusu ve altında durum yazısı.
/// Ulaşılmış satırın zemini bir tık açık (#18262F).
private struct KupaEsikSatiriGorunumu: View {
    let satir: KupaEsikSatiri
    let skor: Int
    let dokunuldu: () -> Void

    /// dimens.xml: cup_path_road_row_height.
    private let satirYuksekligi: CGFloat = 76
    /// dimens.xml: cup_path_road_chest_center_offset (kapalı sandık çizimindeki üst boşluğu telafi eder).
    private let sandikKaldirma: CGFloat = 7

    private let yolRengi: Color = KupaStil.renk(0xFFB300)
    private let yolBosRengi: Color = KupaStil.renk(0x1A3040)
    private let yaziSoluk: Color = KupaStil.renk(0x93A5B3)

    private var ulasildi: Bool {
        return satir.durum != KupaEsikDurumu.kilitli
    }

    var body: some View {
        ZStack(alignment: .leading) {
            icerik
            if satir.simdi {
                isaretci
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: satirYuksekligi)
        .background(ulasildi ? KupaStil.renk(0x18262F) : Color.clear)
        .contentShape(Rectangle())
        .onTapGesture(perform: dokunuldu)
    }

    private var icerik: some View {
        HStack(spacing: 0) {
            serit
            deger
                .padding(.leading, 34)
                .frame(maxWidth: .infinity, alignment: .leading)
            odul
        }
        .padding(.leading, 12)
        .padding(.trailing, 16)
    }

    /// Yol şeridi: dikey çizgi + eşik noktası. Bulunulan satırda şeridin alttan dolan kısmı altın rengidir.
    private var serit: some View {
        let renk: Color = ulasildi ? yolRengi : yolBosRengi
        let dolu: CGFloat = satir.simdi ? (satirYuksekligi * CGFloat(satir.oran)).rounded(.down) : 0
        return ZStack {
            Rectangle()
                .fill(renk)
                .frame(width: 4, height: satirYuksekligi)
            if dolu > 0 {
                Rectangle()
                    .fill(yolRengi)
                    .frame(width: 4, height: dolu)
                    .frame(height: satirYuksekligi, alignment: .bottom)
            }
            Circle()
                .fill(renk)
                .frame(width: 16, height: 16)
        }
        .frame(width: 44, height: satirYuksekligi)
    }

    private var deger: some View {
        HStack(spacing: 4) {
            Image("infinity_cup_ic")
                .resizable()
                .scaledToFit()
                .frame(width: 16, height: 16)
                .opacity(ulasildi ? 1.0 : 0.5)
            Text(String(satir.deger))
                .font(Tema.yazi(15))
                .foregroundColor(ulasildi ? Color.white : yaziSoluk)
        }
    }

    /// Ödül: sandık kutusu ve durum yazısı.
    private var odul: some View {
        VStack(spacing: 0) {
            sandikKutusu
            Text(durumYazisi)
                .font(Tema.yazi(10))
                .foregroundColor(durumRengi)
                .frame(minHeight: 14)
        }
    }

    private var durumYazisi: String {
        switch satir.durum {
        case .alindi: return Metin.al("ui_alindi_caps")
        case .hazir: return Metin.al("ui_hazir_caps")
        case .kilitli: return ""
        }
    }

    private var durumRengi: Color {
        return satir.durum == KupaEsikDurumu.hazir ? yolRengi : yaziSoluk
    }

    /// 60dp kutu: alınmış eşikte açık sandık (alfa 0.6) ve sağ üstte yeşil tik, hazır eşikte halkalı kapalı
    /// sandık, kilitli eşikte soluk (alfa 0.45) kapalı sandık.
    private var sandikKutusu: some View {
        let alinmis: Bool = satir.durum == KupaEsikDurumu.alindi
        let hazir: Bool = satir.durum == KupaEsikDurumu.hazir
        let resim: String = alinmis ? satir.acikSandik : satir.kapaliSandik
        // Açık sandık çizimi kendi kutusunda zaten ortalıdır; kapalıda üst boşluk için yukarı kaydırılır.
        let kaydirma: CGFloat = alinmis ? 0 : -sandikKaldirma
        var alfa: Double = 0.45
        if alinmis {
            alfa = 0.6
        } else if hazir {
            alfa = 1.0
        }
        return ZStack {
            if hazir {
                // bg_cup_path_milestone_ready: #33FFB300 dolgu, 3dp #FFB300 çizgi.
                Circle()
                    .fill(KupaStil.renk(0xFFB300, alfa: 0.2))
                Circle()
                    .strokeBorder(yolRengi, lineWidth: 3)
            }
            Image(resim)
                .resizable()
                .scaledToFit()
                .frame(width: 46, height: 46)
                .offset(y: kaydirma)
                .opacity(alfa)
        }
        .frame(width: 60, height: 60)
        .overlay(alignment: .topTrailing) {
            if alinmis {
                Image("ic_done")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(KupaStil.renk(0x21D07A))
                    .frame(width: 20, height: 20)
            }
        }
    }

    /// "Şu an buradasın" etiketi: şeridin üstüne biner (eşik noktasını kapatması kasıtlıdır), güncel kupa
    /// sayısını taşır. #FFB300 dolgu, 2dp #141F23 çizgi, 12dp köşe.
    private var isaretci: some View {
        HStack(spacing: 0) {
            Image("infinity_cup_ic")
                .resizable()
                .scaledToFit()
                .frame(width: 14, height: 14)
            Text(String(skor))
                .font(Tema.yazi(13))
                .foregroundColor(KupaStil.renk(0x141F23))
                .padding(.leading, 4)
        }
        .padding(.leading, 7)
        .padding(.trailing, 9)
        .padding(.vertical, 3)
        .background(RoundedRectangle(cornerRadius: 12).fill(yolRengi))
        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(KupaStil.renk(0x141F23), lineWidth: 2))
        .shadow(color: Color.black.opacity(0.3), radius: 2, x: 0, y: 2)
    }
}

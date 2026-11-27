import SwiftUI

// Çoktan seçmeli adımların paneli (Paket A).
//
// Android kaynakları:
//   fragment_tutorial.xml 2259-2329: optionsPanel (bottom_sheet_background: #222, üst köşeler 16 dp;
//     padding 8 dp; üstü `guideline` = abaküs kabının üst kenarı, altı ekranın altı; elevation 16 dp),
//     optionsTitleText, optionsRecyclerView, optionsCheckButton
//   item_tutorial_option.xml, TutorialOptionsAdapter.kt (satır, seçim kuralı)
//   drawable/tutorial_option_row_selector.xml, tutorial_option_indicator_on/off.xml
//   TutorialFragment.updateOptionsPanelForStep (17909), syncOptionsContentTextSizeStep (18026),
//     dismissOptionsPanelBeforeResult (18111), checkOptionsAnswer (17708; iOS'ta model.secenekleriKontrolEt)
//
// Davranış:
//   - Adım gösterilince panel 1 sn gizli bekler; sonra şıklar yüklenir, yazı boyutu sığdırılırken
//     panel görünmez kalır, ardından alttan 300 ms kayarak girer.
//   - Panel açıkken geri ve "Eğitimi atla" kilitlidir (model: adim.gecerliSecenekVar).
//   - Tek seçimde dokunulan şık tek işaretli olur (zaten işaretliyse öyle kalır); çoklu seçimde
//     dokunmak işareti açıp kapatır.
//   - Sonuç paneli açılınca (doğru ya da yanlış) panel hemen kapanır; yanlıştan sonra aynı adım
//     yeniden gösterilir ve panel 1 sn sonra yine açılır (seçimler model tarafından sıfırlanır).
//   - Başlık ve şıklar aynı yazı boyutunu paylaşır: 22 sp'den başlar, liste alana sığmazsa 0,5'er
//     küçülür (en az 13 sp).

/// fragment_tutorial.xml / dimens.xml / drawable ölçüleri (dp → pt).
enum OgreticiSecenekOlcu {
    /// tutorial_options_panel_padding
    static let panelDolgu: CGFloat = 8
    /// bottom_sheet_background üst köşe yarıçapı: OgreticiPanelZemini (16) kullanılır.
    /// tutorial_options_title_margin_vertical / _horizontal
    static let baslikDikey: CGFloat = 18
    static let baslikYatay: CGFloat = 4
    /// tutorial_options_question_top_margin (liste ile başlık arası)
    static let listeUst: CGFloat = 10
    /// tutorial_options_item_row_margin_bottom
    static let satirAlt: CGFloat = 8
    /// tutorial_options_item_row_padding_h / _v
    static let satirYatay: CGFloat = 14
    static let satirDikey: CGFloat = 12
    /// tutorial_options_item_text_gap_end
    static let metinBosluk: CGFloat = 10
    /// tutorial_option_row_selector: köşe 10, çizgi 1
    static let satirKose: CGFloat = 10
    static let satirCizgi: CGFloat = 1
    /// tutorial_options_checkbox_size / _ring_stroke
    static let isaretBoyu: CGFloat = 24
    static let isaretHalka: CGFloat = 1.5
    /// tutorial_options_text_size / tutorial_options_title_text_size (sp → pt)
    static let yaziBaslangic: CGFloat = 22
    /// tutorial_options_auto_text_min_sp
    static let yaziEnKucuk: CGFloat = 13
    static let yaziAdim: CGFloat = 0.5
    /// tutorial_options_title_line_spacing_extra
    static let baslikSatirAraligi: CGFloat = -4
    /// tutorial_options_check_button_height / _margin_top / _margin_end / _padding_horizontal / _text_size
    static let dugmeYukseklik: CGFloat = 50
    static let dugmeUst: CGFloat = 8
    static let dugmeSag: CGFloat = 8
    static let dugmeYatay: CGFloat = 16
    static let dugmeYazi: CGFloat = 18
    /// tutorial_control_button_corner_radius
    static let dugmeKose: CGFloat = 10
    /// Sığdırma: liste taşması toleransı (computeVerticalScrollRange > height + 2)
    static let tasmaToleransi: CGFloat = 2
    /// updateOptionsPanelForStep: postDelayed 1000 ms; kayma 300 ms
    static let gecikme: Double = 1.0
    static let kaymaSuresi: Double = 0.3
    /// Sığdırma sonuçsuz kalırsa (ölçüm gelmezse) panel yine de açılır.
    static let olcumZamanAsimi: Double = 1.5
}

/// Renkler (colors.xml ve drawable XML'lerinden; bu adlar varlık kataloğunda yok, sabit yazıldı).
enum OgreticiSecenekRenk {
    /// tutorial_option_row_normal_bg #161B22, _stroke #30363D
    static let satirZemin: Color = Color(.sRGB, red: 22.0 / 255.0, green: 27.0 / 255.0, blue: 34.0 / 255.0, opacity: 1.0)
    static let satirCizgi: Color = Color(.sRGB, red: 48.0 / 255.0, green: 54.0 / 255.0, blue: 61.0 / 255.0, opacity: 1.0)
    /// tutorial_option_row_selected_bg #33FFD600 (alfa 0x33), _stroke #FFD600
    static let seciliZemin: Color = Color(.sRGB, red: 1.0, green: 214.0 / 255.0, blue: 0.0, opacity: 51.0 / 255.0)
    static let seciliCizgi: Color = Color(.sRGB, red: 1.0, green: 214.0 / 255.0, blue: 0.0, opacity: 1.0)
    /// yellow #FFD600 (işaretli gösterge)
    static let sari: Color = Color(.sRGB, red: 1.0, green: 214.0 / 255.0, blue: 0.0, opacity: 1.0)
    /// tutorial_option_indicator_ring #8B949E
    static let halka: Color = Color(.sRGB, red: 139.0 / 255.0, green: 148.0 / 255.0, blue: 158.0 / 255.0, opacity: 1.0)
}

// MARK: Ölçüm

/// Liste içeriğinin ve görünür liste alanının yüksekliği; `boyut` ölçümün hangi yazı boyutunda alındığını
/// da taşır (boyut değişince değer her zaman değişir, böylece sığdırma döngüsü durmaz).
struct OgreticiSecenekOlcumu: Equatable {
    var icerik: CGFloat = 0
    var alan: CGFloat = 0
    var boyut: CGFloat = 0
}

struct OgreticiSecenekOlcumAnahtari: PreferenceKey {
    static var defaultValue: OgreticiSecenekOlcumu = OgreticiSecenekOlcumu()

    static func reduce(value: inout OgreticiSecenekOlcumu, nextValue: () -> OgreticiSecenekOlcumu) {
        let sonraki: OgreticiSecenekOlcumu = nextValue()
        value = OgreticiSecenekOlcumu(
            icerik: max(value.icerik, sonraki.icerik),
            alan: max(value.alan, sonraki.alan),
            boyut: max(value.boyut, sonraki.boyut)
        )
    }
}

// MARK: Zamanlama ve sığdırma durumu

/// Panelin açılış sırası (updateOptionsPanelForStep) ve yazı boyutu sığdırma (syncOptionsContentTextSizeStep).
/// Ana iş parçacığında kullanılır; her yeni adım `nesil` sayacını artırıp bekleyen işleri geçersiz kılar
/// (Android: optionsPanelShowRunnable iptali).
final class OgreticiSecenekDurumu: ObservableObject {
    /// Panel yerleşimde (şıklar yüklü); henüz görünmeyebilir.
    @Published private(set) var monte: Bool = false
    /// Başlangıç konumu ekranın altına alındı (kayma başlamak üzere).
    @Published private(set) var kaymaHazir: Bool = false
    /// Panel görünür ve dokunulabilir.
    @Published private(set) var gorunur: Bool = false
    /// Başlık ve şıkların ortak yazı boyutu.
    @Published private(set) var yaziBoyu: CGFloat = OgreticiSecenekOlcu.yaziBaslangic

    private var nesil: Int = 0
    private var olculdu: Bool = false

    /// Her şeyi kapatır ve (panel gerekiyorsa) açılışı 1 sn sonrasına planlar.
    func planla(gecerli: Bool) {
        nesil += 1
        monte = false
        kaymaHazir = false
        gorunur = false
        olculdu = false
        yaziBoyu = OgreticiSecenekOlcu.yaziBaslangic
        if !gecerli { return }
        let bu: Int = nesil
        DispatchQueue.main.asyncAfter(deadline: .now() + OgreticiSecenekOlcu.gecikme) { [weak self] in
            guard let self = self, self.nesil == bu else { return }
            self.yerlestir(bu)
        }
    }

    /// Şıklar yüklenir; panel görünmez kalarak ölçülür.
    private func yerlestir(_ bu: Int) {
        monte = true
        DispatchQueue.main.asyncAfter(deadline: .now() + OgreticiSecenekOlcu.olcumZamanAsimi) { [weak self] in
            guard let self = self, self.nesil == bu else { return }
            self.olcumuBitir(bu)
        }
    }

    /// Yeni ölçüm: liste taşıyorsa yazı 0,5 küçülür; sığıyorsa panel gösterilir.
    func olc(_ v: OgreticiSecenekOlcumu) {
        if !monte || olculdu { return }
        if v.icerik <= 0 || v.alan <= 0 { return }
        let tasiyor: Bool = v.icerik > v.alan + OgreticiSecenekOlcu.tasmaToleransi
        if tasiyor && yaziBoyu > OgreticiSecenekOlcu.yaziEnKucuk {
            let kucuk: CGFloat = yaziBoyu - OgreticiSecenekOlcu.yaziAdim
            yaziBoyu = max(OgreticiSecenekOlcu.yaziEnKucuk, kucuk)
        } else {
            olcumuBitir(nesil)
        }
    }

    private func olcumuBitir(_ bu: Int) {
        if olculdu || nesil != bu { return }
        olculdu = true
        // Önce panel ekranın altına alınır (görünmez), sonraki turda kayarak girer.
        kaymaHazir = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.03) { [weak self] in
            guard let self = self, self.nesil == bu else { return }
            withAnimation(.easeInOut(duration: OgreticiSecenekOlcu.kaymaSuresi)) {
                self.gorunur = true
            }
        }
    }
}

/// Panelin yeniden planlanmasını gerektiren değişimler: adım, sonuç paneli, geçerli seçenek var mı.
struct OgreticiSecenekAnahtari: Equatable {
    let sira: Int
    let sonucYok: Bool
    let gecerli: Bool
}

// MARK: Panel

struct OgreticiSecenekPaneli: View {
    @ObservedObject var model: OgreticiGorunumModeli
    @StateObject private var durum: OgreticiSecenekDurumu = OgreticiSecenekDurumu()

    private var anahtar: OgreticiSecenekAnahtari {
        return OgreticiSecenekAnahtari(
            sira: model.adim.sira,
            sonucYok: model.sonuc == nil,
            gecerli: model.adim.gecerliSecenekVar
        )
    }

    /// Panelin planlanacağı durum: seçenek geçerli ve sonuç paneli kapalı.
    private func planla() {
        let a: OgreticiSecenekAnahtari = anahtar
        durum.planla(gecerli: a.gecerli && a.sonucYok)
    }

    var body: some View {
        GeometryReader { alan in
            yerlesim(ekranYuksekligi: alan.size.height + alan.safeAreaInsets.bottom)
        }
        .onAppear { planla() }
        .onChange(of: anahtar) { (_: OgreticiSecenekAnahtari) in planla() }
    }

    /// Panel abaküs kabının üstünden ekranın altına kadar uzanır (guideline → parent bottom).
    private func yerlesim(ekranYuksekligi: CGFloat) -> some View {
        let abakus: CGSize = AbakusGorunumu.boyut(basamakSayisi: 5)
        let yukseklik: CGFloat = abakus.height + OgreticiOlcu.abakusAltBosluk
            + OgreticiOlcu.kontrolYukseklik + OgreticiOlcu.altPanelAlt
        let ofset: CGFloat = durum.gorunur ? 0 : (durum.kaymaHazir ? ekranYuksekligi : 0)
        return VStack(spacing: 0) {
            Spacer(minLength: 0)
            if durum.monte {
                panelGovdesi
                    .frame(height: yukseklik)
                    .background(OgreticiPanelZemini())
                    .contentShape(Rectangle())
                    .onTapGesture {}
                    .offset(y: ofset)
                    .opacity(durum.gorunur ? 1 : 0)
                    .allowsHitTesting(durum.gorunur)
                    .onPreferenceChange(OgreticiSecenekOlcumAnahtari.self) { (v: OgreticiSecenekOlcumu) in
                        durum.olc(v)
                    }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
    }

    private var panelGovdesi: some View {
        OgreticiSecenekIcerigi(
            baslik: model.adim.secenekBasligi,
            secenekler: model.adim.secenekler,
            cokluSecim: model.adim.cokluSecim,
            secili: model.secilenSecenekler,
            boyut: durum.yaziBoyu,
            sec: { (sira: Int) in secimYap(sira) },
            kontrol: { model.secenekleriKontrolEt() }
        )
        .padding(OgreticiSecenekOlcu.panelDolgu)
    }

    /// TutorialOptionsAdapter tıklama kuralı.
    private func secimYap(_ sira: Int) {
        if model.adim.cokluSecim {
            if model.secilenSecenekler.contains(sira) {
                model.secilenSecenekler.remove(sira)
            } else {
                model.secilenSecenekler.insert(sira)
            }
        } else {
            model.secilenSecenekler = [sira]
        }
    }
}

/// Panelin içi: başlık, kaydırılabilir şık listesi, "Kontrol Et".
struct OgreticiSecenekIcerigi: View {
    let baslik: String?
    let secenekler: [String]
    let cokluSecim: Bool
    let secili: Set<Int>
    let boyut: CGFloat
    let sec: (Int) -> Void
    let kontrol: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            baslikGorunumu
            liste
            kontrolSatiri
        }
    }

    /// optionsTitleText: yalnızca optionText doluysa görünür.
    @ViewBuilder
    private var baslikGorunumu: some View {
        if let b = baslik, !b.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            Text(b)
                .font(Tema.yazi(boyut))
                .foregroundColor(Color.white)
                .lineSpacing(OgreticiSecenekOlcu.baslikSatirAraligi)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, OgreticiSecenekOlcu.baslikDikey)
                .padding(.horizontal, OgreticiSecenekOlcu.baslikYatay)
        }
    }

    /// optionsRecyclerView: kalan yüksekliği doldurur; içerik sığmazsa kayar.
    private var liste: some View {
        ScrollView(.vertical, showsIndicators: false) {
            satirlar
                .background(icerikOlcer)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(alanOlcer)
        .padding(.top, OgreticiSecenekOlcu.listeUst)
    }

    private var satirlar: some View {
        VStack(spacing: 0) {
            ForEach(0..<secenekler.count, id: \.self) { (i: Int) in
                OgreticiSecenekSatiri(
                    metin: secenekler[i],
                    secili: secili.contains(i),
                    boyut: boyut,
                    sec: { sec(i) }
                )
                .padding(.bottom, OgreticiSecenekOlcu.satirAlt)
            }
        }
    }

    private var icerikOlcer: some View {
        let b: CGFloat = boyut
        return GeometryReader { (g: GeometryProxy) in
            Color.clear.preference(
                key: OgreticiSecenekOlcumAnahtari.self,
                value: OgreticiSecenekOlcumu(icerik: g.size.height, alan: 0, boyut: b)
            )
        }
    }

    private var alanOlcer: some View {
        let b: CGFloat = boyut
        return GeometryReader { (g: GeometryProxy) in
            Color.clear.preference(
                key: OgreticiSecenekOlcumAnahtari.self,
                value: OgreticiSecenekOlcumu(icerik: 0, alan: g.size.height, boyut: b)
            )
        }
    }

    /// optionsCheckButton: sağa yaslı, 50 yükseklik, düz (göçmeyen) MaterialButton.
    private var kontrolSatiri: some View {
        HStack(spacing: 0) {
            Spacer(minLength: 0)
            Button(action: kontrol) {
                Text(Metin.al("fragment_tutorial_options_check_button"))
                    .font(Tema.yazi(OgreticiSecenekOlcu.dugmeYazi))
                    .foregroundColor(Color.black)
                    .lineLimit(1)
                    .padding(.horizontal, OgreticiSecenekOlcu.dugmeYatay)
                    .frame(height: OgreticiSecenekOlcu.dugmeYukseklik)
                    .background(
                        RoundedRectangle(cornerRadius: OgreticiSecenekOlcu.dugmeKose)
                            .fill(Color("lesson_completed"))
                    )
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .padding(.top, OgreticiSecenekOlcu.dugmeUst)
        .padding(.trailing, OgreticiSecenekOlcu.dugmeSag)
    }
}

// MARK: Satır ve gösterge

/// item_tutorial_option.xml: metin solda, işaret sağda; satırın tamamı dokunulur.
struct OgreticiSecenekSatiri: View {
    let metin: String
    let secili: Bool
    let boyut: CGFloat
    let sec: () -> Void

    var body: some View {
        Button(action: sec) {
            satir
        }
        .buttonStyle(.plain)
    }

    private var satir: some View {
        let zemin: Color = secili ? OgreticiSecenekRenk.seciliZemin : OgreticiSecenekRenk.satirZemin
        let cizgi: Color = secili ? OgreticiSecenekRenk.seciliCizgi : OgreticiSecenekRenk.satirCizgi
        let sekil: RoundedRectangle = RoundedRectangle(cornerRadius: OgreticiSecenekOlcu.satirKose)
        return HStack(spacing: 0) {
            Text(metin)
                .font(Tema.yazi(boyut))
                .foregroundColor(Color("dark_text"))
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.trailing, OgreticiSecenekOlcu.metinBosluk)
            OgreticiSecenekIsareti(secili: secili)
        }
        .padding(.horizontal, OgreticiSecenekOlcu.satirYatay)
        .padding(.vertical, OgreticiSecenekOlcu.satirDikey)
        .background(sekil.fill(zemin))
        .overlay(sekil.strokeBorder(cizgi, lineWidth: OgreticiSecenekOlcu.satirCizgi))
        .contentShape(Rectangle())
    }
}

/// tutorial_option_checkbox_btn: işaretsiz gri halka; işaretli sarı halka + sarı tik (Material check_circle).
struct OgreticiSecenekIsareti: View {
    let secili: Bool

    var body: some View {
        ZStack {
            if secili {
                seciliGosterge
            } else {
                Circle()
                    .strokeBorder(OgreticiSecenekRenk.halka, lineWidth: OgreticiSecenekOlcu.isaretHalka)
            }
        }
        .frame(width: OgreticiSecenekOlcu.isaretBoyu, height: OgreticiSecenekOlcu.isaretBoyu)
    }

    /// Vektör 960 birimlik alanda: halka çapı 800, kalınlığı 80 (24 pt'te 20 ve 2).
    private var seciliGosterge: some View {
        let olcek: CGFloat = OgreticiSecenekOlcu.isaretBoyu / 960.0
        let cap: CGFloat = 800.0 * olcek
        let kalinlik: CGFloat = 80.0 * olcek
        return ZStack {
            Circle()
                .strokeBorder(OgreticiSecenekRenk.sari, lineWidth: kalinlik)
                .frame(width: cap, height: cap)
            OgreticiSecenekTik()
                .fill(OgreticiSecenekRenk.sari)
        }
    }
}

/// tutorial_option_indicator_on.xml'deki tik çokgeni (960 × 960 alanda).
struct OgreticiSecenekTik: Shape {
    func path(in rect: CGRect) -> Path {
        let k: CGFloat = rect.width / 960.0
        let noktalar: [CGPoint] = [
            CGPoint(x: 424.0 * k, y: 664.0 * k),
            CGPoint(x: 706.0 * k, y: 382.0 * k),
            CGPoint(x: 650.0 * k, y: 326.0 * k),
            CGPoint(x: 424.0 * k, y: 552.0 * k),
            CGPoint(x: 310.0 * k, y: 438.0 * k),
            CGPoint(x: 254.0 * k, y: 494.0 * k)
        ]
        var yol = Path()
        yol.move(to: noktalar[0])
        for i in 1..<noktalar.count {
            yol.addLine(to: noktalar[i])
        }
        yol.closeSubpath()
        return yol
    }
}

import SwiftUI
import UIKit

// "Sobi ile analiz et" penceresinin parçaları: renkler, göçen çip stilleri, sohbet balonları, kural
// motoru seçenekleri ve sütun parlaması.
//
// Android kaynakları: res/drawable/chat_bubble_sobi.xml (#2C4A52, köşe 12), chat_bubble_me.xml
// (#1E6655, köşe 8), bg_hw_chip.xml (yüz @color/background_color + 2 dp #37464F çerçeve, köşe 16,
// altta 4 dp #37464F dudak), bg_sobi_pill.xml (#21D07A, köşe 20), bg_rod_highlight.xml (#5532C5FF dolgu,
// 2 dp #FF32C5FF çerçeve, köşe 14); SobiMessageViews.bubble (16 sp, satır aralığı 1.15, iç boşluk
// 12/9, en geniş ekranın %74'ü, satır arası 4); MistakeReviewDialog.localChips / styleChip /
// highlightColumn.

enum SobiAnalizRenk {
    static let zemin: Color = Color("background_color")
    static let yazi: Color = Color("dark_text")
    static let ikincilYazi: Color = Color("dark_text_secondary")
    static let sobiBalonu: Color = SobiAnalizRenk.renk(0x2C4A52)
    static let cocukBalonu: Color = SobiAnalizRenk.renk(0x1E6655)
    static let cipCizgi: Color = SobiAnalizRenk.renk(0x37464F)
    static let ucretliCip: Color = SobiAnalizRenk.renk(0x21D07A)
    static let vurgu: Color = SobiAnalizRenk.renk(0x32C5FF)

    static func renk(_ onaltilik: Int) -> Color {
        let k: Double = Double((onaltilik >> 16) & 0xFF) / 255.0
        let y: Double = Double((onaltilik >> 8) & 0xFF) / 255.0
        let m: Double = Double(onaltilik & 0xFF) / 255.0
        return Color(red: k, green: y, blue: m)
    }
}

// MARK: - Çip stilleri

/// bg_hw_chip + DepthPress: yüz arka plan renginde, 2 pt çerçeveli; altta 4 pt dudak. Basılıyken dudak
/// gizlenir, yüz 4 pt iner. Yükseklik dudak dahildir (etiket + 4).
struct SobiAnalizGriCipStili: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        let basili: Bool = configuration.isPressed
        let inme: CGFloat = basili ? 4 : 0
        let dudakSaydamligi: Double = basili ? 0 : 1
        return configuration.label
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(SobiAnalizRenk.zemin)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .strokeBorder(SobiAnalizRenk.cipCizgi, lineWidth: 2)
                    )
            )
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(SobiAnalizRenk.cipCizgi)
                    .offset(y: 4)
                    .opacity(dudakSaydamligi)
            )
            .offset(y: inme)
            .padding(.bottom, 4)
    }
}

/// bg_sobi_pill + DepthPress: yeşil, dudaksız; basılıyken 4 pt iner.
struct SobiAnalizYesilCipStili: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        let inme: CGFloat = configuration.isPressed ? 4 : 0
        return configuration.label
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(SobiAnalizRenk.ucretliCip)
            )
            .offset(y: inme)
    }
}

// MARK: - Sohbet balonu

/// SobiMessageViews.bubble: Sobi solda (#2C4A52), çocuk sağda (#1E6655). Sesli okuma varsa Sobi'nin
/// balonunun altında 🔊 (beğen / beğenme / bildir yok: kural motoru mesajları oturumsuz).
struct SobiAnalizMesajSatiri: View {
    let mesaj: SobiAnalizGorunenMesaj
    let sesliOkuma: Bool
    let oku: () -> Void

    private var enGenis: CGFloat {
        return UIScreen.main.bounds.width * 0.74
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            satir
            if sesliOkuma && !mesaj.cocuktan {
                okuDugmesi
            }
        }
    }

    private var satir: some View {
        HStack(spacing: 0) {
            if mesaj.cocuktan {
                Spacer(minLength: 0)
            }
            balon
            if !mesaj.cocuktan {
                Spacer(minLength: 0)
            }
        }
        .padding(.vertical, 4)
    }

    private var balon: some View {
        let kose: CGFloat = mesaj.cocuktan ? 8 : 12
        let renk: Color = mesaj.cocuktan ? SobiAnalizRenk.cocukBalonu : SobiAnalizRenk.sobiBalonu
        let hiza: Alignment = mesaj.cocuktan ? Alignment.trailing : Alignment.leading
        return Text(mesaj.metin)
            .font(Tema.yazi(16))
            .foregroundColor(SobiAnalizRenk.yazi)
            .lineSpacing(2.4)
            .fixedSize(horizontal: false, vertical: true)
            .padding(.horizontal, 12)
            .padding(.vertical, 9)
            .background(
                RoundedRectangle(cornerRadius: kose)
                    .fill(renk)
            )
            .frame(maxWidth: enGenis, alignment: hiza)
    }

    /// speakButton: 🔊 18 sp, iç boşluk 8/2; satırın başında 6.
    private var okuDugmesi: some View {
        Button(action: oku) {
            Text("🔊")
                .font(.system(size: 18))
                .padding(.horizontal, 8)
                .padding(.vertical, 2)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(Metin.al("sobi_speak")))
        .padding(.leading, 6)
    }
}

// MARK: - Seçenekler

/// localChips: yatay kayan satır; gri seçenekler ücretsiz (gösterim, kural motorunun açıklamaları),
/// yeşiller yeni örnek (1 hak ya da kredi). Yazı 14 sp; iç boşluk 12/7/12/10 (gri, dudak dahil) ya da
/// 12/7/12/7 (yeşil); aralık 8; satırın üstünde 6.
struct SobiAnalizSecenekSatiri: View {
    let balonlar: [SobiAnalizGorunenBalon]
    let dokunuldu: (Int) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(alignment: .top, spacing: 8) {
                ForEach(balonlar) { balon in
                    cip(balon)
                }
            }
            .padding(.bottom, 4)
        }
        .padding(.top, 6)
    }

    @ViewBuilder
    private func cip(_ balon: SobiAnalizGorunenBalon) -> some View {
        let kod: Int = balon.id
        if balon.ucretli {
            Button(action: { dokunuldu(kod) }) {
                Text(balon.etiket)
                    .font(Tema.yazi(14))
                    .foregroundColor(Color.white)
                    .lineLimit(1)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 7)
            }
            .buttonStyle(SobiAnalizYesilCipStili())
        } else {
            Button(action: { dokunuldu(kod) }) {
                Text(balon.etiket)
                    .font(Tema.yazi(14))
                    .foregroundColor(SobiAnalizRenk.yazi)
                    .lineLimit(1)
                    .padding(.horizontal, 12)
                    .padding(.top, 7)
                    .padding(.bottom, 6)
            }
            .buttonStyle(SobiAnalizGriCipStili())
        }
    }
}

// MARK: - Sütun parlaması

/// highlightColumn: talimatın sözünü ettiği çubuk 1600 ms boyunca yanıp söner (saydamlık 0 → 1 →
/// 0,24 → 1 → 0; Android ValueAnimator.ofInt(0, 255, 60, 255, 0), varsayılan hızlanıp yavaşlayan eğri).
/// Abaküs görünümünün sol üstüne yerleştirilir; çubuğun çerçevesi AbakusTahtaGorunumu ölçülerinden.
struct SobiAnalizSutunVurgusu: View {
    let vurgu: SobiAnalizVurgu?

    @State private var saydamlik: Double = 0
    @State private var sayac: Int = 0

    var body: some View {
        let sutun: Int = vurgu?.sutun ?? 0
        let cubuk: Int = min(max(AbakusTahtaGorunumu.cubukSayisi - 1 - sutun, 0), AbakusTahtaGorunumu.cubukSayisi - 1)
        let x: CGFloat = AbakusTahtaGorunumu.yatayDolgu + CGFloat(cubuk) * AbakusTahtaGorunumu.cubukGenisligi
        let y: CGFloat = AbakusTahtaGorunumu.dikeyDolgu
        return RoundedRectangle(cornerRadius: 14)
            .fill(SobiAnalizRenk.vurgu.opacity(Double(0x55) / 255.0))
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .strokeBorder(SobiAnalizRenk.vurgu, lineWidth: 2)
            )
            .frame(width: AbakusTahtaGorunumu.cubukGenisligi, height: AbakusTahtaGorunumu.cubukYuksekligi)
            .opacity(saydamlik)
            .offset(x: x, y: y)
            .allowsHitTesting(false)
            .onChange(of: vurgu) { yeni in
                if yeni != nil {
                    canlandir()
                }
            }
    }

    private func canlandir() {
        sayac += 1
        let kimlik: Int = sayac
        let adimlar: [Double] = [1.0, 60.0 / 255.0, 1.0, 0.0]
        saydamlik = 0
        for (i, hedef) in adimlar.enumerated() {
            let baslangic: Double = Double(i) * 0.4
            DispatchQueue.main.asyncAfter(deadline: .now() + baslangic) {
                if sayac != kimlik { return }
                withAnimation(.easeInOut(duration: 0.4)) {
                    saydamlik = hedef
                }
            }
        }
    }
}

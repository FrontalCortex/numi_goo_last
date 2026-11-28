import SwiftUI

// Yarış paneli ekranı ("Ustalık Yolu"): kısım 7 Toplama Ustalığı ve kısım 8 Çıkarma Ustalığı.
//
// Android karşılıkları:
//   race_bottom_sheet.xml            → YarisSayfasi (yeşil başlık + yarış listesi, tam ekran zemin)
//   item_race_list.xml + RaceAdapter → YarisSatirGorunumu
//   race_lesson_bottom_sheet.xml     → YarisAltPanelGorunumu (BAŞLAT / Tekrar dene / anahtar düğmesi)
//   panel_race_fast_forward.xml      → YarisIleriSarKatmani (kilitli yarışı 1 anahtarla açan pencere)
//
// Giriş görünümü: `YarisPaneliEkrani(girdi:baglanti:cuzdan:baslangic:kapat:)`. Kurallar (satır durumu,
// alt panel metni ve düğme biçimi, anahtar bedeli) ortak koddan gelir; bkz. YarisModeli.swift.

struct YarisPaneliEkrani: View {
    @StateObject private var model: YarisPaneliModeli
    private let kapat: () -> Void

    init(
        girdi: YarisPaneliGirdisi,
        baglanti: YarisPaneliBaglantisi,
        cuzdan: MagazaCuzdanServisi,
        baslangic: YarisBaslangicDurumu = .liste,
        kapat: @escaping () -> Void
    ) {
        let nesne = YarisPaneliModeli(girdi: girdi, baglanti: baglanti, cuzdan: cuzdan, baslangic: baslangic)
        _model = StateObject(wrappedValue: nesne)
        self.kapat = kapat
    }

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            YarisSayfasi(model: model, kapat: kapat)
            altPanelKatmani
            ileriSarKatmani
            bildirimKatmani
        }
        .animation(Animation.easeOut(duration: 0.3), value: model.altPanel != nil)
        .animation(Animation.easeOut(duration: 0.2), value: model.ileriSarSatiri != nil)
        .animation(Animation.easeInOut(duration: 0.2), value: model.bildirim)
    }

    /// Android: scrimView (alfa 0,5) + race_lesson_bottom_sheet; karartmaya dokununca alt panel kapanır.
    @ViewBuilder
    private var altPanelKatmani: some View {
        if let panel = model.altPanel {
            Color.black.opacity(0.5)
                .ignoresSafeArea()
                .contentShape(Rectangle())
                .onTapGesture { model.altPaneliKapat() }
                .transition(AnyTransition.opacity)
                .zIndex(1)
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                YarisAltPanelGorunumu(panel: panel, eylem: { model.altPanelEylemi() })
            }
            .transition(AnyTransition.move(edge: .bottom))
            .zIndex(1)
        }
    }

    @ViewBuilder
    private var ileriSarKatmani: some View {
        if model.ileriSarSatiri != nil {
            YarisIleriSarKatmani(
                islemde: model.anahtarIslemde,
                kapat: { model.ileriSarKapat() },
                onayla: { model.ileriSarOnayla() }
            )
            .transition(AnyTransition.opacity)
            .zIndex(2)
        }
    }

    /// Android: Toast ("Yeterli anahtarın yok.").
    @ViewBuilder
    private var bildirimKatmani: some View {
        if let metin = model.bildirim {
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                YarisBildirimGorunumu(metin: metin)
            }
            .allowsHitTesting(false)
            .transition(AnyTransition.opacity)
            .zIndex(3)
        }
    }
}

// MARK: - Sayfa (race_bottom_sheet.xml)

struct YarisSayfasi: View {
    @ObservedObject var model: YarisPaneliModeli
    let kapat: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            baslikSatiri
            liste
        }
    }

    /// Başlık: lesson_header_green zemin, 17dp iç boşluk, 21sp beyaz ortalı yazı, sağda "✕".
    private var baslikSatiri: some View {
        HStack(alignment: .center, spacing: 0) {
            Text(model.girdi.baslik)
                .font(YarisStil.yazi(YarisStil.baslikYazisi))
                .foregroundColor(Color.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
            kapatDugmesi
        }
        .padding(YarisStil.baslikIcBosluk)
        .frame(maxWidth: .infinity)
        .background(Color("lesson_header_green").ignoresSafeArea(edges: .top))
    }

    private var kapatDugmesi: some View {
        Button(action: kapat) {
            Text("✕")
                .font(YarisStil.yazi(YarisStil.kapatYazisi))
                .foregroundColor(Color.white)
                .frame(width: YarisStil.kapatKutusu, height: YarisStil.kapatKutusu)
                .padding(10)
                .contentShape(Rectangle())
                .padding(-10)
        }
        .buttonStyle(YarisBasmaStili())
    }

    /// RecyclerView: 17dp iç boşluk; satırlar arası 12dp (her satırın layout_marginBottom'u).
    private var liste: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 0) {
                ForEach(model.satirlar) { satir in
                    YarisSatirGorunumu(satir: satir, dokun: { model.satirDokun(satir) })
                }
            }
            .padding(YarisStil.listeIcBosluk)
        }
    }
}

// MARK: - Satır (item_race_list.xml)

struct YarisSatirGorunumu: View {
    let satir: YarisSatiri
    let dokun: () -> Void

    var body: some View {
        Button(action: dokun) {
            icerik
        }
        .buttonStyle(YarisBasmaStili())
        .padding(.bottom, YarisStil.satirAltBosluk)
    }

    private var icerik: some View {
        HStack(alignment: .center, spacing: 0) {
            Image("flag_ic")
                .resizable()
                .scaledToFit()
                .frame(width: YarisStil.satirSimgeBoyu, height: YarisStil.satirSimgeBoyu)
                .padding(.trailing, YarisStil.satirSimgeBosluk)
            Text(satir.ad)
                .font(YarisStil.yazi(YarisStil.satirAdYazisi))
                .foregroundColor(Color.black)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
            durumKutusu
        }
        .padding(YarisStil.satirIcBosluk)
        .frame(maxWidth: .infinity)
        .background(kartZemini)
        .contentShape(RoundedRectangle(cornerRadius: YarisStil.satirKoseYaricapi))
    }

    /// RaceAdapter.bind: 0 → lesson_completed, 2 → lesson_locked, diğerleri beyaz.
    private var kartRengi: Color {
        if satir.seviye == 0 {
            return Color("lesson_completed")
        }
        if satir.seviye == 2 {
            return Color("lesson_locked")
        }
        return Color.white
    }

    /// cardElevation 4dp: gölge yalnızca zemin şekline verilir.
    private var kartZemini: some View {
        RoundedRectangle(cornerRadius: YarisStil.satirKoseYaricapi)
            .fill(kartRengi)
            .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
    }

    /// buttonBackgroundID: durum yazısı + (kilitliyse) kilit simgesi. Zemin: 2 → race_button_background,
    /// 1 → button_background, 0 → yok.
    private var durumKutusu: some View {
        HStack(alignment: .center, spacing: 0) {
            durumYazisi
            kilit
        }
        .background(durumZemini)
    }

    private var durumYazisi: some View {
        Text(satir.durumMetni)
            .font(YarisStil.yazi(YarisStil.satirDurumYazisi))
            .foregroundColor(Color.black)
            .lineLimit(1)
            .opacity(satir.durumEtkin ? 1.0 : 0.8)
            .padding(.horizontal, YarisStil.durumYatayBosluk)
            .padding(.vertical, YarisStil.durumDikeyBosluk)
    }

    @ViewBuilder
    private var kilit: some View {
        if satir.kilitGorunur {
            Image("lock_ic")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(YarisStil.kilitSimgesi)
                .frame(width: YarisStil.kilitBoyu, height: YarisStil.kilitBoyu)
                .padding(.trailing, YarisStil.kilitSagBosluk)
        }
    }

    @ViewBuilder
    private var durumZemini: some View {
        if satir.seviye == 0 {
            Color.clear
        } else if satir.seviye == 2 {
            RoundedRectangle(cornerRadius: YarisStil.durumKoseYaricapi)
                .fill(YarisStil.kilitliRozetZemini)
        } else {
            RoundedRectangle(cornerRadius: YarisStil.durumKoseYaricapi)
                .fill(Color("lesson_completed"))
        }
    }
}

// MARK: - Alt panel (race_lesson_bottom_sheet.xml)

struct YarisAltPanelGorunumu: View {
    let panel: YarisAltPanel
    let eylem: () -> Void

    /// Kilitli yarışta başlık, açıklama lesson_locked; değilse lesson_completed (Android: setTextColor).
    private var vurgu: Color {
        return panel.kilitli ? Color("lesson_locked") : Color("lesson_completed")
    }

    /// backgroundTintList: kilitli → background_color, değilse panel_background.
    private var zemin: Color {
        return panel.kilitli ? Color("background_color") : Color("panel_background")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            baslik
            aciklama
            eylemSatiri
        }
        .padding(YarisStil.altPanelIcBosluk)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            YarisUstKoseSekli(yaricap: YarisStil.altPanelKoseYaricapi)
                .fill(zemin)
                .ignoresSafeArea(edges: .bottom)
        )
        // Panelin boş yerine dokunuş karartmaya geçmesin (Android: bottomSheetLayout.setOnClickListener {}).
        .contentShape(Rectangle())
        .onTapGesture {}
    }

    private var baslik: some View {
        Text(panel.baslik)
            .font(YarisStil.yazi(YarisStil.altPanelBaslikYazisi))
            .foregroundColor(vurgu)
            .lineLimit(2)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, YarisStil.altPanelYatayBosluk)
            .padding(.top, YarisStil.altPanelBaslikUstBosluk)
            .padding(.bottom, YarisStil.altPanelBaslikAltBosluk)
    }

    private var aciklama: some View {
        Text(panel.aciklama)
            .font(YarisStil.yazi(YarisStil.altPanelGovdeYazisi))
            .foregroundColor(vurgu)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, YarisStil.altPanelYatayBosluk)
            .padding(.bottom, YarisStil.altPanelOrtaAltBosluk)
    }

    @ViewBuilder
    private var eylemSatiri: some View {
        if panel.kilitli {
            anahtarDugmesi
        } else {
            baslatDugmesi
        }
    }

    /// Kilitli yarış: 120×60dp, button_enabled zemin, ortada 32dp anahtar simgesi, metin yok.
    private var anahtarDugmesi: some View {
        Button(action: eylem) {
            Image("key")
                .resizable()
                .scaledToFit()
                .frame(width: YarisStil.anahtarSimgeBoyu, height: YarisStil.anahtarSimgeBoyu)
                .frame(width: YarisStil.anahtarDugmeGenisligi, height: YarisStil.dugmeGorunenYukseklik)
                .background(
                    RoundedRectangle(cornerRadius: YarisStil.dugmeKoseYaricapi)
                        .fill(Color("button_enabled"))
                )
                .contentShape(Rectangle())
        }
        .buttonStyle(YarisBasmaStili())
        .frame(maxWidth: .infinity)
    }

    /// BAŞLAT / Tekrar dene: tam genişlik, lesson_completed zemin, panel_background yazı.
    private var baslatDugmesi: some View {
        Button(action: eylem) {
            Text(panel.eylemMetni)
                .font(YarisStil.yazi(YarisStil.altPanelDugmeYazisi))
                .foregroundColor(Color("panel_background"))
                .lineLimit(1)
                .frame(maxWidth: .infinity)
                .frame(height: YarisStil.dugmeGorunenYukseklik)
                .background(
                    RoundedRectangle(cornerRadius: YarisStil.dugmeKoseYaricapi)
                        .fill(Color("lesson_completed"))
                )
                .contentShape(Rectangle())
        }
        .buttonStyle(YarisBasmaStili())
        .padding(.horizontal, YarisStil.altPanelYatayBosluk)
    }
}

// MARK: - İleri Sar penceresi (panel_race_fast_forward.xml)

/// Pencereyi ekranın ortasında, karartılmış zeminin üstünde gösterir (Android: Dialog, genişlik ekranın
/// %88'i, dışına dokununca kapanır).
struct YarisIleriSarKatmani: View {
    let islemde: Bool
    let kapat: () -> Void
    let onayla: () -> Void

    @State private var acik: Bool = false

    var body: some View {
        GeometryReader { alan in
            ZStack {
                Color.black.opacity(0.6)
                    .ignoresSafeArea()
                    .contentShape(Rectangle())
                    .onTapGesture(perform: kapat)
                YarisIleriSarPaneli(islemde: islemde, kapat: kapat, onayla: onayla)
                    .frame(width: alan.size.width * YarisStil.pencereGenislikOrani)
                    .scaleEffect(acik ? 1.0 : 0.9)
                    .opacity(acik ? 1.0 : 0.0)
            }
            .frame(width: alan.size.width, height: alan.size.height)
        }
        .onAppear {
            withAnimation(Animation.easeOut(duration: 0.2)) {
                acik = true
            }
        }
    }
}

/// panel_race_fast_forward.xml: zemin panel_background (köşe 18dp), iç boşluk 20dp.
struct YarisIleriSarPaneli: View {
    let islemde: Bool
    let kapat: () -> Void
    let onayla: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ustSatir
            LottieGorunumu(YarisStil.pencereAnimasyonu, dongu: true)
                .frame(width: YarisStil.pencereAnimasyonBoyu, height: YarisStil.pencereAnimasyonBoyu)
                .padding(.top, 8)
            mesaj
            anahtarDugmesi
                .padding(.top, 20)
        }
        .padding(YarisStil.pencereIcBosluk)
        .background(
            RoundedRectangle(cornerRadius: YarisStil.pencereKoseYaricapi)
                .fill(Color("panel_background"))
        )
    }

    /// Başlık (20sp, ortalı; kapatma düğmesinin soluna kadar) ve sağ üstte 40dp kapatma düğmesi.
    private var ustSatir: some View {
        HStack(alignment: .center, spacing: 0) {
            Text(Metin.al("race_fast_forward_title"))
                .font(YarisStil.yazi(YarisStil.pencereBaslikYazisi))
                .foregroundColor(Color.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 8)
            Button(action: kapat) {
                Image("ic_close_black")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(Color.white)
                    .padding(8)
                    .frame(width: YarisStil.pencereKapatKutusu, height: YarisStil.pencereKapatKutusu)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(Metin.al("daily_question_continue_close")))
        }
    }

    private var mesaj: some View {
        Text(Metin.al("race_fast_forward_message"))
            .font(YarisStil.yazi(YarisStil.pencereMesajYazisi))
            .foregroundColor(YarisStil.mesajRengi)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(.top, 8)
    }

    /// Yeşil düğme: anahtar simgesi (32dp) + "x1" (18sp, siyah); yükseklik 60dp, en az 160dp, köşe 15dp.
    /// MaterialButton'ın 6dp üst/alt insetiyle görünen zemin 48dp yüksekliğindedir.
    private var anahtarDugmesi: some View {
        Button(action: onayla) {
            HStack(alignment: .center, spacing: 8) {
                Image("key")
                    .resizable()
                    .scaledToFit()
                    .frame(width: YarisStil.anahtarSimgeBoyu, height: YarisStil.anahtarSimgeBoyu)
                Text(Metin.al("daily_question_continue_diamond"))
                    .font(YarisStil.yazi(YarisStil.pencereDugmeYazisi))
                    .foregroundColor(Color.black)
            }
            .padding(.horizontal, 24)
            .frame(minWidth: YarisStil.pencereDugmeEnaz)
            .frame(height: YarisStil.pencereDugmeYuksekligi)
            .background(
                RoundedRectangle(cornerRadius: YarisStil.dugmeKoseYaricapi)
                    .fill(Color("button_enabled"))
                    .padding(.vertical, 6)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(YarisBasmaStili())
        .disabled(islemde)
    }
}

// MARK: - Bilgi balonu

struct YarisBildirimGorunumu: View {
    let metin: String

    var body: some View {
        Text(metin)
            .font(YarisStil.yazi(14))
            .foregroundColor(Color.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Capsule().fill(YarisStil.toastZemini))
            .padding(.horizontal, 24)
            .padding(.bottom, 48)
    }
}

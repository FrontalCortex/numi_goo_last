import SwiftUI

// Öğretici ekranı (dersin önündeki anlatım).
//
// Android karşılığı: TutorialFragment (app/src/main/java/com/example/app/TutorialFragment.kt) ve
// res/layout/fragment_tutorial.xml. Adım içerikleri ortak Kotlin kodundadır
// (ios/shared/.../ogretici/icerik/OgreticiN.kt, Android'den betikle üretilir); geçiş kuralları
// OgreticiOynatici'dedir. Yapı ve paketler: ios/docs/OGRETICI_PLANI.md.
//
// Yerleşim (fragment_tutorial.xml, dp → pt):
//   sol üstte çıkış (40, başlangıç 15, üst 10, lesson_completed renkli);
//   [kural tablosu / kural metni: OgreticiKuralAlani — paket işi];
//   adım metni (20, beyaz, ortalı, alta yaslı; kalan bütün yüksekliği kaplar);
//   soru kutusu (190 genişlik, #222 zemin, 5 siyah çerçeve, alt boşluk 2);
//   abaküs (273 × 290, alt boşluk 10) ve üstünde odak çerçevesi (focusView);
//   alt panel (alt 16, yatay 5): geri (48), "Kontrol Et" (48 yükseklik, iki yanda 16), "Eğitimi atla" (36);
//   son adımda "Kontrol Et" ve "Eğitimi atla" yerine "Devam Et";
//   alttan açılan doğru / yanlış panelleri.
//
// Kapsam dışı (Android'de var, burada yok): Sobi'ye sor düğmesi ve dürtmeleri (hesap ister; hesapsız ilk
// açılışta Android'de de görünmez), öğretmene sor (Android'de gizli), ölçüm, öğretici sonu anketi
// (TutorialSurveyPolicy), "Ne zorladı?" anketi, kısa ekran ölçeklemesi (applyTutorialViewScaling).

/// Öğreticinin girdisi.
struct OgreticiBaslangici {
    /// Android tutorialNumber (LessonItem.tutorialNumber).
    let ogreticiNo: Int
    /// Hesapsız ilk açılış (Android isFirstRunTutorial): çıkış ve "Eğitimi atla" gizli.
    var ilkAcilis: Bool
    /// Ekran galerisi / ortadan açılış: bu adımdan başlanır, abaküs o adıma gelinceki sayıya kurulur.
    var baslangicAdimi: Int
    /// Yer tutucuda gösterilen ders başlığı.
    var baslik: String

    init(ogreticiNo: Int, ilkAcilis: Bool = false, baslangicAdimi: Int = 0, baslik: String = "") {
        self.ogreticiNo = ogreticiNo
        self.ilkAcilis = ilkAcilis
        self.baslangicAdimi = baslangicAdimi
        self.baslik = baslik
    }

    /// Bu öğreticinin içeriği taşındı mı. false ise yönetici eski yer tutucuyu açar.
    static func icerikVar(_ ogreticiNo: Int) -> Bool {
        return OgreticiKoprusu.icerikVar(ogreticiNo)
    }
}

/// Öğreticiden derse nasıl geçildi.
enum OgreticiSonucu {
    /// Son adımdaki "Devam Et" (Android devametFragment → devamButton).
    case derseGec
    /// "Eğitimi atla" (Android tutorialSkipSetup) ya da içeriği olmayan öğreticinin yer tutucusu.
    case atlandi
}

struct OgreticiEkrani: View {
    @StateObject private var model: OgreticiGorunumModeli

    /// - Parameters:
    ///   - baslangic: hangi öğretici, hangi adımdan.
    ///   - ses: adım sesleri (şimdilik sahte; ses dosyaları kopyalanmadı).
    ///   - bitti: derse geçilecek (Devam Et ya da Eğitimi atla). Dersi yönetici açar.
    ///   - kapat: çıkış düğmesi (kayma animasyonundan sonra) ya da ilk adımda yanlış → harita.
    init(
        baslangic: OgreticiBaslangici,
        ses: OgreticiSesCalici = SahteOgreticiSesCalici(),
        bitti: @escaping (OgreticiSonucu) -> Void,
        kapat: @escaping () -> Void
    ) {
        _model = StateObject(
            wrappedValue: OgreticiGorunumModeli(baslangic: baslangic, ses: ses, bitti: bitti, kapat: kapat)
        )
    }

    var body: some View {
        GeometryReader { alan in
            ZStack {
                Color("background_color").ignoresSafeArea()
                if model.icerikVar {
                    OgreticiIcerigiGorunumu(model: model)
                } else {
                    OgreticiIcerikYokGorunumu(model: model)
                }
            }
            // closeFragment: ekran genişliği kadar sola, 400 ms.
            .offset(x: model.kapaniyor ? -alan.size.width : 0)
        }
        .onAppear { model.gorundu() }
        .onDisappear { model.kayboldu() }
    }
}

/// İçeriği olan öğreticinin katmanları.
struct OgreticiIcerigiGorunumu: View {
    @ObservedObject var model: OgreticiGorunumModeli

    var body: some View {
        ZStack {
            // Ekrana dokunma: üstteki metin katmanları dokunmayı geçirir (allowsHitTesting(false)),
            // düğmeler ve açık abaküs kendileri alır (Android root.setOnClickListener + abacusTapBlocker).
            Color.clear
                .contentShape(Rectangle())
                .onTapGesture { model.ekranaDokunuldu() }
            // Kör ders görselleri alt panelin ve metnin ALTINDA çizilir (Android'de XML sırası).
            OgreticiGorselAlani(model: model)
            OgreticiAnaDuzen(model: model)
            OgreticiSecenekPaneli(model: model)
            OgreticiSonucKatmani(model: model)
            OgreticiUyariKatmani(metin: model.uyari)
        }
    }
}

/// Dikey yerleşim: üst çubuk, kural alanı, metin, soru, abaküs, alt panel.
struct OgreticiAnaDuzen: View {
    @ObservedObject var model: OgreticiGorunumModeli

    var body: some View {
        VStack(spacing: 0) {
            // Kural tabloları sağ üstte, çıkış düğmesiyle aynı yükseklikten başlar; adım metni ikisinin
            // de altından başlar (Android rulesBarrier).
            ZStack(alignment: .topTrailing) {
                OgreticiUstCubuk(gorunur: model.sahne.cikisGorunur, cik: { model.cik() })
                OgreticiKuralAlani(model: model)
            }
            OgreticiMetinAlani(metin: model.adim.metin, gorunenHarf: model.gorunenHarf)
                .opacity(model.metinGorselYuksekligi == nil ? 1 : 0)
                .allowsHitTesting(false)
            OgreticiSoruKutusu(adim: model.adim)
                .allowsHitTesting(false)
                .padding(.bottom, OgreticiOlcu.soruAltBosluk)
            OgreticiAbakusAlani(model: model)
                .padding(.bottom, OgreticiOlcu.abakusAltBosluk)
            OgreticiAltPanel(model: model)
                .padding(.horizontal, OgreticiOlcu.altPanelYatay)
                .padding(.bottom, OgreticiOlcu.altPanelAlt)
        }
        .modifier(OgreticiMetinGorselBagi(model: model))
    }
}

/// İçeriği taşınmamış öğretici: çökmeden derse geçirir ya da kapatır.
struct OgreticiIcerikYokGorunumu: View {
    @ObservedObject var model: OgreticiGorunumModeli

    var body: some View {
        VStack(spacing: 16) {
            Text(model.baslangic.baslik)
                .font(Tema.yazi(24))
                .foregroundColor(Color.white)
                .multilineTextAlignment(.center)
            // Android'de karşılığı yok (geçici metin).
            Text("Öğretici \(model.baslangic.ogreticiNo) henüz hazır değil.")
                .font(Tema.yazi(17))
                .foregroundColor(Color("lesson_completed"))
                .multilineTextAlignment(.center)
            Button(action: { model.atla() }) {
                Text(Metin.al("fragment_tutorial_text_35"))
                    .font(Tema.yazi(OgreticiOlcu.kontrolYazi))
                    .foregroundColor(Color.black)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(GocenDugmeStili(zemin: OgreticiRenk.kontrolZemin, golge: OgreticiRenk.kontrolGolge, koseYaricapi: 10))
            .frame(height: OgreticiOlcu.kontrolYukseklik)
            Button(action: { model.cik() }) {
                Image("quit_ic")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(Color("lesson_completed"))
                    .frame(width: OgreticiOlcu.cikisBoyu, height: OgreticiOlcu.cikisBoyu)
            }
            .buttonStyle(.plain)
        }
        .padding(24)
    }
}

/// Kısa bildirim (Android Toast).
struct OgreticiUyariKatmani: View {
    let metin: String?

    var body: some View {
        VStack {
            Spacer()
            if let m = metin {
                Text(m)
                    .font(Tema.yazi(15, kalin: false))
                    .foregroundColor(Color.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Capsule().fill(Color.black.opacity(0.8)))
                    .padding(.bottom, 90)
                    .transition(.opacity)
            }
        }
        .allowsHitTesting(false)
    }
}

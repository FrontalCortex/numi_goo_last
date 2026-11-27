import SwiftUI

// Ders sonucu ekranı.
//
// Android kaynakları:
//   LessonResult.kt       + fragment_lesson_result.xml        (başarılı ders)
//   LessonResultFalse.kt  + fragment_lesson_result_false.xml  (başarısız ders / başarısız sandık)
//   ChestResult.kt        + fragment_chest_result.xml         (sandık = ünite maratonu sonucu)
//
// Bu ekran yalnızca gösterir. Android'de "Devam et" düğmesinin arkasındaki işler (ders zaten
// bitmişse haritaya dönmek, değilse sandık ekranını açmak, başarı oranı ve görev kayıtları,
// rekor güncellemesi) ekranı açan yöneticiye aittir; kuralları ortak koddadır:
// LessonResultLogic.onClaim, LessonResultFalseLogic.failRecordFor / shouldRedirectToLogin,
// ChestResultLogic.record / shouldSkipChest / replayFinishedChest (ders paketi).

struct SonucEkrani: View {
    private let devam: () -> Void
    private let tekrar: () -> Void
    @StateObject private var model: SonucModeli

    /// - Parameters:
    ///   - girdi: gösterilecek sonuç.
    ///   - devam: "Devam et" (başarılı) ya da "Çıkış" (başarısız) düğmesine basıldı.
    ///   - tekrar: "Tekrar dene" düğmesine basıldı (yalnızca `girdi.tekrarDugmesiGoster` ise görünür).
    init(girdi: SonucGirdisi, devam: @escaping () -> Void, tekrar: @escaping () -> Void) {
        self.init(girdi: girdi, ses: SahteSonucSesCalar(), devam: devam, tekrar: tekrar)
    }

    /// Ses uygulaması dışarıdan verilen biçim.
    init(
        girdi: SonucGirdisi,
        ses: SonucSesCalar,
        devam: @escaping () -> Void,
        tekrar: @escaping () -> Void
    ) {
        self.devam = devam
        self.tekrar = tekrar
        _model = StateObject(wrappedValue: SonucModeli(girdi: girdi, ses: ses))
    }

    var body: some View {
        ZStack {
            SonucStil.zemin.ignoresSafeArea()
            icerik
        }
        .onAppear { model.basla() }
        .onDisappear { model.durdur() }
    }

    @ViewBuilder
    private var icerik: some View {
        switch model.girdi.tur {
        case .dersBasarili:
            SonucDersBasariliGorunumu(model: model, devam: devamaBas)
        case .dersBasarisiz, .sandikBasarisiz:
            SonucBasarisizGorunumu(model: model, cikis: devamaBas, tekrar: tekraraBas)
        case .sandikSonucu:
            SonucSandikGorunumu(model: model, devam: devamaBas)
        }
    }

    private func devamaBas() {
        if model.devamaIzinVar() { devam() }
    }

    private func tekraraBas() {
        if model.devamaIzinVar() { tekrar() }
    }
}

// MARK: - Başarılı ders (fragment_lesson_result.xml)

struct SonucDersBasariliGorunumu: View {
    @ObservedObject var model: SonucModeli
    let devam: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            SonucAnimasyonu(ad: model.animasyonAdi)
                .frame(width: 200, height: 200)
            Text("Ders Bitti!")
                .font(Tema.yazi(28))
                .foregroundColor(SonucStil.baslikSarisi)
            kutular
                .padding(.bottom, 32)
            Spacer(minLength: 0)
            SonucDugmesi(
                metin: "Devam et",
                zemin: SonucStil.devamDugmesi,
                yaziRengi: .black,
                yukseklik: 60,
                kose: 15,
                eylem: devam
            )
            .padding(.horizontal, 10)
            .padding(.top, 16)
            .padding(.bottom, 20)
            .opacity(model.dugmeGorunur ? 1 : 0)
            .animation(.linear(duration: 0.3), value: model.dugmeGorunur)
            .disabled(!model.dugmeEtkin)
        }
        .padding(.horizontal, 24)
        .padding(.top, 80)
    }

    /// İki kutu eşit genişlikte; Android kenar boşlukları: [kutu]–8–8–[kutu]–8.
    private var kutular: some View {
        HStack(spacing: 16) {
            SonucKutusu(
                baslik: "TOPLAM PUAN",
                deger: model.puanMetni,
                yaziRengi: SonucStil.kutuSarisi,
                cerceveRengi: SonucStil.kutuSarisi
            )
            .modifier(SonucKutuBelirme(gorunur: model.puanKutusuGorunur))
            SonucKutusu(
                baslik: "BAŞARI",
                deger: model.basariMetni,
                yaziRengi: SonucStil.kutuYesilYazi,
                cerceveRengi: SonucStil.kutuYesilCerceve
            )
            .modifier(SonucKutuBelirme(gorunur: model.basariKutusuGorunur))
        }
        .padding(.trailing, 8)
    }
}

/// LessonResult.revealBox: 350 ms'de %75 ölçekten %100'e büyüyerek belirir (yavaşlayan eğri).
struct SonucKutuBelirme: ViewModifier {
    let gorunur: Bool

    func body(content: Content) -> some View {
        content
            .opacity(gorunur ? 1 : 0)
            .scaleEffect(gorunur ? 1 : 0.75)
            .animation(.easeOut(duration: 0.35), value: gorunur)
    }
}

// MARK: - Başarısız (fragment_lesson_result_false.xml)

struct SonucBasarisizGorunumu: View {
    @ObservedObject var model: SonucModeli
    let cikis: () -> Void
    let tekrar: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            SonucAnimasyonu(ad: model.animasyonAdi)
                .frame(width: 200, height: 200)
            Text("Ders Geçilemedi!")
                .font(Tema.yazi(28))
                .foregroundColor(SonucStil.hataBaslik)
            Text(model.aciklama)
                .font(Tema.yazi(20))
                .foregroundColor(SonucStil.hataAciklama)
                .multilineTextAlignment(.center)
                .padding(.bottom, 32)
            kutular
                .padding(.bottom, 32)
            Spacer(minLength: 0)
            if model.girdi.tekrarDugmesiGoster {
                // Android'de yok; yalnızca girdi isterse görünür.
                SonucDugmesi(
                    metin: "Tekrar dene",
                    zemin: SonucStil.devamDugmesi,
                    yaziRengi: SonucStil.zemin,
                    yukseklik: 56,
                    kose: 4,
                    eylem: tekrar
                )
                .padding(.bottom, 12)
            }
            SonucDugmesi(
                metin: "Çıkış",
                zemin: SonucStil.cikisDugmesi,
                yaziRengi: SonucStil.zemin,
                yukseklik: 56,
                kose: 4,
                eylem: cikis
            )
            .padding(.bottom, 20)
        }
        .padding(.horizontal, 24)
        .padding(.top, 80)
    }

    private var kutular: some View {
        HStack(spacing: 16) {
            SonucKutusu(
                baslik: "TOPLAM PUAN",
                deger: model.puanMetni,
                yaziRengi: SonucStil.kutuSarisi,
                cerceveRengi: SonucStil.kutuSarisi
            )
            SonucKutusu(
                baslik: "BAŞARI",
                deger: model.basariMetni,
                yaziRengi: SonucStil.kutuYesilYazi,
                cerceveRengi: SonucStil.kutuYesilCerceve
            )
        }
        .padding(.trailing, 8)
    }
}

// MARK: - Sandık sonucu (fragment_chest_result.xml)

struct SonucSandikGorunumu: View {
    @ObservedObject var model: SonucModeli
    let devam: () -> Void
    @State private var cerceveler: [Int: CGRect] = [:]

    /// Çerçeve kimlikleri
    private static let sureKutusu = 1
    private static let puanYazisi = 2

    var body: some View {
        ZStack(alignment: .topLeading) {
            sutun
            balon
            parlama
        }
        .coordinateSpace(name: "sonucAlani")
        .onPreferenceChange(SonucCerceveAnahtari.self) { yeni in
            cerceveler = yeni
        }
    }

    private var sutun: some View {
        VStack(spacing: 0) {
            SonucAnimasyonu(ad: model.animasyonAdi)
                .frame(width: 200, height: 200)
            Text("Ders Bitti!")
                .font(Tema.yazi(28))
                .foregroundColor(SonucStil.baslikSarisi)
                .padding(.bottom, 50)
            kutular
                .padding(.bottom, 32)
            buyukPuan
            SonucPuanCubugu(model: model)
                .padding(.top, 8)
                .padding(.bottom, 10)
            Spacer(minLength: 0)
            SonucDugmesi(
                metin: "Devam et",
                zemin: SonucStil.devamDugmesi,
                yaziRengi: .black,
                yukseklik: 60,
                kose: 15,
                eylem: devam
            )
            .padding(.horizontal, 10)
            .padding(.top, 16)
            .padding(.bottom, 20)
            .opacity(model.dugmeGorunur ? 1 : 0)
            .animation(.linear(duration: 0.3), value: model.dugmeGorunur)
            .disabled(!model.dugmeEtkin)
        }
        .padding(.horizontal, 24)
        .padding(.top, 80)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var kutular: some View {
        HStack(spacing: 16) {
            SonucKutusu(
                baslik: "Süre",
                deger: model.sureMetni,
                yaziRengi: SonucStil.kutuSarisi,
                cerceveRengi: SonucStil.kutuSarisi,
                baslikBoyu: 16
            )
            .sonucCercevesiniBildir(SonucSandikGorunumu.sureKutusu)
            .opacity(model.sureKutusuGorunur ? 1 : 0)
            .animation(.linear(duration: 0.22), value: model.sureKutusuGorunur)
            SonucKutusu(
                baslik: "BAŞARI",
                deger: model.basariMetni,
                yaziRengi: SonucStil.kutuYesilYazi,
                cerceveRengi: SonucStil.kutuYesilCerceve
            )
        }
    }

    private var buyukPuan: some View {
        Text(String(model.buyukPuan))
            .font(Tema.yazi(44))
            .foregroundColor(SonucStil.kutuSarisi)
            .scaleEffect(model.puanZipla ? 1.1 : 1.0)
            .animation(.easeInOut(duration: 0.13), value: model.puanZipla)
            .frame(maxWidth: .infinity)
            .sonucCercevesiniBildir(SonucSandikGorunumu.puanYazisi)
            .padding(.vertical, 12)
    }

    /// Çarpan balonu (86×42): süre kutusunun 16 PİKSEL üstünde durur (positionBubbleOverTimeBox
    /// `- 16f`: dp değil ham piksel), sonra puan yazısına uçar.
    private var balon: some View {
        let sure: CGRect = cerceveler[SonucSandikGorunumu.sureKutusu] ?? .zero
        let puan: CGRect = cerceveler[SonucSandikGorunumu.puanYazisi] ?? .zero
        let aralik: CGFloat = SonucStil.pikseldenNokta(16)
        let ustKenar: CGFloat = max(0, sure.minY - 42 - aralik)
        let bekleme = CGPoint(x: sure.midX, y: ustKenar + 21)
        let hedef = CGPoint(x: puan.midX, y: puan.midY)
        let konum: CGPoint = model.balonUcusta ? hedef : bekleme
        return Text(model.carpanMetni)
            .font(Tema.yazi(16))
            .foregroundColor(SonucStil.kutuSarisi)
            .frame(width: 86, height: 42)
            .background(RoundedRectangle(cornerRadius: 16).fill(SonucStil.kutuDolgu))
            .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(SonucStil.kutuSarisi, lineWidth: 2))
            .position(konum)
            .animation(.easeOut(duration: 0.65), value: model.balonUcusta)
            .opacity(model.balonGorunur ? 1 : 0)
            .allowsHitTesting(false)
    }

    /// scoreImpactFlash: 26×26 sarı kutu, 100 ms'de 0.5 → 2 ölçek, belirip söner.
    private var parlama: some View {
        let puan: CGRect = cerceveler[SonucSandikGorunumu.puanYazisi] ?? .zero
        return RoundedRectangle(cornerRadius: 16)
            .fill(SonucStil.kutuDolgu)
            .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(SonucStil.kutuSarisi, lineWidth: 2))
            .frame(width: 26, height: 26)
            .scaleEffect(model.parlama ? 2.0 : 0.5)
            .opacity(model.parlama ? 1 : 0)
            .animation(.linear(duration: 0.05), value: model.parlama)
            .position(x: puan.midX, y: puan.midY)
            .allowsHitTesting(false)
    }
}

/// scoreProgressZone: 230×58 alan; altta 14 pt'lik çubuk, üstünde üç yıldız işareti.
struct SonucPuanCubugu: View {
    @ObservedObject var model: SonucModeli

    private let genislik: CGFloat = 230
    private let yukseklik: CGFloat = 58
    private let cubukYuksekligi: CGFloat = 14

    var body: some View {
        ZStack(alignment: .topLeading) {
            cubuk
                .frame(width: genislik, height: cubukYuksekligi)
                .position(x: genislik / 2, y: yukseklik - cubukYuksekligi / 2)
            yildiz(0)
            yildiz(1)
            yildiz(2)
        }
        .frame(width: genislik, height: yukseklik)
    }

    /// scoreProgressBarContainer: #333333 zemin üstünde yan yana iki parça (dolu, boş); ikisi de
    /// progress_bar_fill şeklidir (köşeler solda 3dp, sağda 4dp — burada dördü de 4 pt çizilir).
    private var cubuk: some View {
        let oran: CGFloat = min(1, max(0, model.cubukOrani))
        let dolu: CGFloat = genislik * oran
        let bos: CGFloat = genislik - dolu
        let doluRenk: Color = SonucStil.cubukSabitRenkli ? SonucStil.cubukDolu : model.cubukRengi
        return HStack(spacing: 0) {
            RoundedRectangle(cornerRadius: 4)
                .fill(doluRenk)
                .frame(width: dolu)
            RoundedRectangle(cornerRadius: 4)
                .fill(SonucStil.cubukBos)
                .frame(width: bos)
        }
        .frame(width: genislik, height: cubukYuksekligi, alignment: .leading)
        .background(SonucStil.cubukZemin)
    }

    /// Yıldızlar 100×100'lük Lottie görünümleridir (star.json, döngüsüz) ve çubuğun ortasına
    /// hizalanır; çubuğun sınırları dışına taşabilirler (Android: kırpma yok).
    /// Android'de ayrı bir dolu/boş yıldız görseli YOKTUR: animasyonun ilk karesi yalnızca
    /// çerçeveden oluşan yıldızdır (resetStarMarker: progress = 0), puan eşiği geçilince bir kez
    /// oynar ve dolu yıldızın olduğu son karede kalır (playStarOnceAndHold).
    private func yildiz(_ sira: Int) -> some View {
        let oran: CGFloat = sira < model.yildizOranlari.count ? model.yildizOranlari[sira] : 1
        let yandi: Bool = sira < model.yildizYandi.count ? model.yildizYandi[sira] : false
        let merkezY: CGFloat = yukseklik - cubukYuksekligi / 2
        return SonucAnimasyonu(ad: "star.json", dongu: false, oynat: yandi)
            .frame(width: 100, height: 100)
            .position(x: oran * genislik, y: merkezY)
    }
}

import SwiftUI
import UIKit

// Avatar düzenleme ekranı (Profil → avatara ya da etrafındaki renkli alana dokun).
// Android: AvatarCustomFragment + res/layout/fragment_avatar_custom.xml, res/drawable/avatar_option_bg.xml.
//
// Yapı (yukarıdan aşağı): "Kapat" | "Avatarını Oluştur" | "Kaydet" (48 pt satır) — büyük önizleme (genişliğin
// %45'i, en çok 220 pt, kare) — "Rastgele" — yatay kaydırılan sekme çipleri — 4 sütunlu seçenek ızgarası.
// Seçenek kutularında o seçenekle çizilmiş avatarın kendisi var (göz/burun/ağız/sakalda yüz yakın planı);
// arka plan sekmesinde renk dairesi; "Yok" seçeneğinde yasak işareti.
//
// Sözleşme: `kaydet` yeni dizgeyle çağrılır, ardından ekran kendisi `kapat`'ı çağırır (Android: AvatarStore.save
// → "Avatarın kaydedildi" → popBackStack). Kayıt (yerel + `users/{uid}.avatarConfig`) ve bildirim çağıranın işi.
// "Kapat" kaydetmeden çıkar.
//
// Android'de olup burada olmayanlar: stil seçim çipleri (tek stil varken Android'de de gizli), dokunma
// titreşimi (Haptics.click) ve ölçüm olayları (AnalyticsLogger.logAvatarPartChanged / logAvatarSaved).

@MainActor
final class AvatarDuzenleModeli: ObservableObject {
    @Published private(set) var config: String
    @Published private(set) var sekmeler: [AvatarSekmesi] = []
    @Published private(set) var seciliSekme: Int = 0
    @Published private(set) var secenekler: [String] = []
    /// Önizlemenin zıplaması (Android: bouncePreview 0.92 → 1).
    @Published var onizlemeOlcegi: CGFloat = 1

    /// Ekran açılırken yüklenen hâl (Android: baseline; kaç parça değiştiğinin ölçümü için).
    let ilkHal: String
    /// "Rastgele" kullanıldı mı (Android: usedRandom; ölçüm için tutulur).
    private(set) var rastgeleKullanildi: Bool = false

    init(baslangic: String) {
        let duz: String = AvatarOrtak.duzgun(baslangic)
        config = duz
        ilkHal = duz
        sekmeler = AvatarOrtak.sekmeler(duz)
        sekmeyiGoster(0)
    }

    var seciliSekmeBilgisi: AvatarSekmesi? {
        guard seciliSekme >= 0 && seciliSekme < sekmeler.count else { return nil }
        return sekmeler[seciliSekme]
    }

    /// Android: showTab.
    func sekmeyiGoster(_ sira: Int) {
        guard sira >= 0 && sira < sekmeler.count else { return }
        seciliSekme = sira
        secenekler = AvatarOrtak.secenekler(sekmeler[sira], config: config)
    }

    func seciliMi(_ deger: String) -> Bool {
        guard let sekme = seciliSekmeBilgisi else { return false }
        return AvatarOrtak.secili(sekme, config: config) == deger
    }

    /// Kutudaki küçük resmin avatarı: o seçenek uygulanmış hâl (Android: holder.avatar.config = withValue(value)).
    func onizleme(_ deger: String) -> String {
        guard let sekme = seciliSekmeBilgisi else { return config }
        return AvatarOrtak.sec(sekme, deger: deger, config: config)
    }

    /// Android: select.
    func sec(_ deger: String) {
        Titresim.tik()
        guard let sekme = seciliSekmeBilgisi else { return }
        if AvatarOrtak.secili(sekme, config: config) == deger { return }
        uygula(AvatarOrtak.sec(sekme, deger: deger, config: config))
    }

    /// Android: btnRandom.
    func rastgele() {
        Titresim.tik()
        rastgeleKullanildi = true
        uygula(AvatarOrtak.rastgele(config))
    }

    private func uygula(_ yeni: String) {
        config = yeni
        ziplat()
    }

    /// Android: scaleX/Y 0.92 → 1, 260 ms, OvershootInterpolator(3f). SwiftUI'da özel ara değerleyici yok;
    /// taşan bir kübik eğriyle yaklaşık çizilir (tepe ≈ %102).
    private func ziplat() {
        var islem = Transaction()
        islem.disablesAnimations = true
        withTransaction(islem) {
            onizlemeOlcegi = 0.92
        }
        ziplamaGorevi?.cancel()
        // Küçültülmüş hâl bir kare çizilsin, sonra büyüme canlandırılsın.
        ziplamaGorevi = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 16_000_000)
            if Task.isCancelled { return }
            withAnimation(.timingCurve(0.3, 2.2, 0.6, 1.0, duration: 0.26)) {
                self?.onizlemeOlcegi = 1
            }
        }
    }

    private var ziplamaGorevi: Task<Void, Never>? = nil
}

struct AvatarDuzenleEkrani: View {
    let kaydet: (String) -> Void
    let kapat: () -> Void
    @StateObject private var model: AvatarDuzenleModeli

    init(baslangic: String, kaydet: @escaping (String) -> Void, kapat: @escaping () -> Void) {
        self.kaydet = kaydet
        self.kapat = kapat
        _model = StateObject(wrappedValue: AvatarDuzenleModeli(baslangic: baslangic))
    }

    var body: some View {
        VStack(spacing: 0) {
            AvatarDuzenleUstSatir(
                kapat: kapat,
                kaydet: {
                    kaydet(model.config)
                    kapat()
                }
            )
            AvatarDuzenleOnizleme(model: model)
                .padding(.top, 8)
            AvatarDuzenleRastgeleDugmesi(eylem: { model.rastgele() })
                .padding(.top, 10)
            AvatarDuzenleSekmeCubugu(model: model)
                .padding(.top, 12)
            AvatarDuzenleIzgara(model: model)
                .padding(.top, 8)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(Color("background_color").ignoresSafeArea())
        // Kök tıklanabilir: alttaki ekrana dokunuş geçmesin (Android: clickable=true).
        .contentShape(Rectangle())
    }
}

/// btnClose (sol), tvTitle (orta), btnSave (sağ); 48 pt satır.
struct AvatarDuzenleUstSatir: View {
    let kapat: () -> Void
    let kaydet: () -> Void

    var body: some View {
        ZStack {
            Text(Metin.al("fragment_avatar_custom_tv_title"))
                .font(Tema.yazi(18))
                .foregroundColor(Color("dark_text"))
                .lineLimit(1)
            HStack(spacing: 0) {
                Button(action: kapat) {
                    Text(Metin.al("fragment_avatar_custom_btn_close"))
                        .font(Tema.yazi(16))
                        .foregroundColor(Color("dark_text_secondary"))
                        .padding(.horizontal, 16)
                        .frame(height: 48)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                Spacer(minLength: 0)
                Button(action: kaydet) {
                    Text(Metin.al("fragment_avatar_custom_btn_save"))
                        .font(Tema.yazi(16))
                        .foregroundColor(Color("dark_primary"))
                        .padding(.horizontal, 16)
                        .frame(height: 48)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .frame(height: 48)
    }
}

/// avatarPreview: genişliğin %45'i, en çok 220 pt, kare; arkada renkli daire (AvatarView varsayılanı).
struct AvatarDuzenleOnizleme: View {
    @ObservedObject var model: AvatarDuzenleModeli

    var body: some View {
        GeometryReader { alan in
            let kenar: CGFloat = AvatarDuzenleOnizleme.kenar(alan.size.width)
            AvatarGorunumu(config: model.config)
                .frame(width: kenar, height: kenar)
                .scaleEffect(model.onizlemeOlcegi)
                .frame(maxWidth: .infinity)
        }
        .frame(height: AvatarDuzenleOnizleme.kenar(AvatarDuzenleOnizleme.ekranGenisligi))
    }

    static func kenar(_ genislik: CGFloat) -> CGFloat {
        let oran: CGFloat = genislik * 0.45
        return min(220, oran)
    }

    /// Yükseklik GeometryReader'dan önce bilinmeli; ekran tam genişlikte açıldığı için ekran genişliği.
    static var ekranGenisligi: CGFloat {
        return UIScreen.main.bounds.width
    }
}

/// btnRandom: 40 pt yükseklik, yatay 18 iç boşluk, 15 pt metin, avatar_option_bg (seçili değil) zemini.
struct AvatarDuzenleRastgeleDugmesi: View {
    let eylem: () -> Void

    var body: some View {
        Button(action: eylem) {
            Text(Metin.al("fragment_avatar_custom_btn_random"))
                .font(Tema.yazi(15))
                .foregroundColor(Color("dark_text"))
                .padding(.horizontal, 18)
                .frame(height: 40)
                .background(RoundedRectangle(cornerRadius: 22).fill(Color("bright_background_color")))
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// categoryChips: tek seçimli Material çipler (32 pt, 16 pt köşe, 14 pt metin, yatay 12 iç boşluk; dokunma
/// alanı 48 pt), aralık 8, kenar boşluğu 16. Seçili: dark_primary zemin + background_color metin;
/// değil: #22303A zemin + dark_text metin.
struct AvatarDuzenleSekmeCubugu: View {
    @ObservedObject var model: AvatarDuzenleModeli

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(model.sekmeler) { sekme in
                    AvatarDuzenleCipi(
                        etiket: sekme.etiket,
                        secili: sekme.id == model.seciliSekme,
                        eylem: { model.sekmeyiGoster(sekme.id) }
                    )
                }
            }
            .padding(.horizontal, 16)
        }
        .frame(height: 48)
    }
}

struct AvatarDuzenleCipi: View {
    let etiket: String
    let secili: Bool
    let eylem: () -> Void

    private static let bosZemin: Color = AvatarRenkCevirici.renk(rrggbb: AvatarOrtak.bosCipRengi)

    var body: some View {
        Button(action: eylem) {
            Text(etiket)
                .font(Tema.yazi(14))
                .foregroundColor(secili ? Color("background_color") : Color("dark_text"))
                .lineLimit(1)
                .padding(.horizontal, 12)
                .frame(height: 32)
                .background(Capsule().fill(secili ? Color("dark_primary") : AvatarDuzenleCipi.bosZemin))
                .frame(height: 48)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// optionGrid: 4 sütun, yatay 12 iç boşluk, altta 120 boşluk; hücre kare, dış boşluk 5, iç boşluk 4,
/// zemin avatar_option_bg (22 pt köşe, bright_background_color; seçili: 2,5 pt bottom_nav_selected_stroke).
struct AvatarDuzenleIzgara: View {
    @ObservedObject var model: AvatarDuzenleModeli

    private var sutunlar: [GridItem] {
        let adet: Int = max(1, AvatarOrtak.izgaraSutunu)
        return Array(repeating: GridItem(.flexible(), spacing: 0), count: adet)
    }

    var body: some View {
        ScrollViewReader { kaydirici in
            ScrollView(.vertical, showsIndicators: false) {
                LazyVGrid(columns: sutunlar, spacing: 0) {
                    ForEach(Array(model.secenekler.enumerated()), id: \.offset) { sira, deger in
                        AvatarDuzenleHucresi(
                            deger: deger,
                            sekme: model.seciliSekmeBilgisi,
                            onizleme: model.onizleme(deger),
                            secili: model.seciliMi(deger),
                            sec: { model.sec(deger) }
                        )
                        .id(sira)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.bottom, 120)
            }
            // Android: sekme değişince liste başa kayar (scrollToPosition(0)).
            .onChange(of: model.seciliSekme) { _ in
                kaydirici.scrollTo(0, anchor: .top)
            }
        }
    }
}

struct AvatarDuzenleHucresi: View {
    let deger: String
    let sekme: AvatarSekmesi?
    let onizleme: String
    let secili: Bool
    let sec: () -> Void

    var body: some View {
        Button(action: sec) {
            GeometryReader { alan in
                let kenar: CGFloat = alan.size.width
                ZStack {
                    RoundedRectangle(cornerRadius: 22).fill(Color("bright_background_color"))
                    icerik(kenar - 8)
                    if secili {
                        RoundedRectangle(cornerRadius: 22)
                            .strokeBorder(Color("bottom_nav_selected_stroke"), lineWidth: 2.5)
                    }
                }
                .frame(width: kenar, height: kenar)
            }
            .aspectRatio(1, contentMode: .fit)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(5)
    }

    @ViewBuilder
    private func icerik(_ ic: CGFloat) -> some View {
        if deger.isEmpty {
            // "Yok": avatar yerine yalnızca yasak işareti (44 pt).
            Image("forbidden_ic")
                .resizable()
                .scaledToFit()
                .frame(width: 44, height: 44)
        } else if let s = sekme, s.renk && s.renkDairesi {
            Circle()
                .fill(AvatarRenkCevirici.renk(rrggbb: deger))
                .frame(width: 40, height: 40)
        } else {
            // Ten, saç ve kıyafet renginde de kutuda o renge boyanmış avatar var. Yakın planda avatar kutuyu
            // dolduruyor; köşeleri kutununkine uysun diye 18 pt yuvarlatılmış kırpılıyor.
            AvatarGorunumu(config: onizleme, arkaPlan: false, yakinPlan: sekme?.yakinPlan ?? false)
                .frame(width: max(0, ic), height: max(0, ic))
                .clipShape(RoundedRectangle(cornerRadius: 18))
        }
    }
}

/// "RRGGBB" / "AARRGGBB" onaltılık metinden renk (Avatar ve Profil klasörlerinde ortak).
enum AvatarRenkCevirici {
    static func renk(rrggbb: String) -> Color {
        let temiz: String = rrggbb.replacingOccurrences(of: "#", with: "")
        let deger: UInt64 = UInt64(temiz, radix: 16) ?? 0
        if temiz.count == 8 {
            return renk(argb: deger)
        }
        return renk(argb: 0xFF00_0000 | (deger & 0xFF_FFFF))
    }

    static func renk(argb: UInt64) -> Color {
        let a: Double = Double((argb >> 24) & 0xFF) / 255.0
        let r: Double = Double((argb >> 16) & 0xFF) / 255.0
        let g: Double = Double((argb >> 8) & 0xFF) / 255.0
        let b: Double = Double(argb & 0xFF) / 255.0
        return Color(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}

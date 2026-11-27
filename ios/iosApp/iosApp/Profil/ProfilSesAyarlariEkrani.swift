import SwiftUI

// "Ses ve Bildirimler" ekranı. Android: SoundSettingsFragment + res/layout/fragment_sound_settings.xml
// + res/layout/dialog_abacus_sound_picker.xml. Açılış: Ayarlar → Hesap kartı → "Ses ve Bildirimler"
// (AccountSettingsFragment.kt:196).
//
// Satırlar (yukarıdan aşağı): Ses efektleri (anahtar, "sound_enabled") · Abaküs ses seçenekleri (seçim
// paneli, "abacus_sound_choice") · Eğitim seslendirme (anahtar, "tutorial_sound_enabled") · Titreşim
// (anahtar, "vibration_enabled") · Bildirimler (ana anahtar) + altında üç tür (Öğretmen mesajları,
// Günlük hatırlatma, Ödüller ve etkinlikler). Hepsi `UserDefaults`'a Android SharedPreferences
// ("AppPrefs") ile aynı anahtarlarla yazılır; varsayılanlar Android'deki gibi açıktır.
//
// Basitleştirme: Android bildirim tercihlerini ayrıca Firestore'a (users/{uid}.notificationPrefs) ve
// AnalyticsLogger'a yazar; iOS'ta Firebase ve bildirim altyapısı henüz yok, yalnızca yerel tercih
// yazılır (mantık Android NotificationPrefs ile aynıdır).

/// Android NotificationPrefs: tür anahtarı "notify_<tür>", eski tek anahtar "notifications_enabled".
enum ProfilBildirimTercihi {
    static let eskiTekAnahtar: String = "notifications_enabled"
    static let sohbet: String = "chat"
    static let seri: String = "streak"
    static let odul: String = "reward"
    static let hepsi: [String] = ["chat", "streak", "reward"]

    private static func anahtar(_ tur: String) -> String {
        return "notify_" + tur
    }

    /// Öncelik: türe özel anahtar > eski tek anahtar > açık.
    static func acikMi(_ tur: String, ayarlar: UserDefaults) -> Bool {
        if let ozel = ayarlar.object(forKey: anahtar(tur)) as? Bool {
            return ozel
        }
        if let eski = ayarlar.object(forKey: eskiTekAnahtar) as? Bool {
            return eski
        }
        return true
    }

    static func ayarla(_ tur: String, _ acik: Bool, ayarlar: UserDefaults) {
        ayarlar.set(acik, forKey: anahtar(tur))
    }

    /// Android NotificationPrefs.setAll: eski anahtar ve üç tür birden aynı değere çekilir.
    static func hepsiniAyarla(_ acik: Bool, ayarlar: UserDefaults) {
        ayarlar.set(acik, forKey: eskiTekAnahtar)
        for tur in hepsi {
            ayarlar.set(acik, forKey: anahtar(tur))
        }
    }
}

/// Ekranın durumu: anahtarlar açılışta `UserDefaults`'tan okunur, değişince hemen yazılır.
final class ProfilSesAyarlariModeli: ObservableObject {
    @Published var sesAcik: Bool
    @Published var ogreticiSesiAcik: Bool
    @Published var titresimAcik: Bool
    @Published var bildirimlerAcik: Bool
    @Published var sohbetAcik: Bool
    @Published var seriAcik: Bool
    @Published var odulAcik: Bool
    @Published var abakusSesi: Int

    private let ayarlar: UserDefaults

    init(ayarlar: UserDefaults = UserDefaults.standard) {
        self.ayarlar = ayarlar
        self.sesAcik = ProfilSesAyarlariModeli.oku("sound_enabled", ayarlar)
        self.ogreticiSesiAcik = ProfilSesAyarlariModeli.oku("tutorial_sound_enabled", ayarlar)
        self.titresimAcik = ProfilSesAyarlariModeli.oku(Titresim.anahtar, ayarlar)
        let sohbet: Bool = ProfilBildirimTercihi.acikMi(ProfilBildirimTercihi.sohbet, ayarlar: ayarlar)
        let seri: Bool = ProfilBildirimTercihi.acikMi(ProfilBildirimTercihi.seri, ayarlar: ayarlar)
        let odul: Bool = ProfilBildirimTercihi.acikMi(ProfilBildirimTercihi.odul, ayarlar: ayarlar)
        self.sohbetAcik = sohbet
        self.seriAcik = seri
        self.odulAcik = odul
        // Ana anahtar türlerden türetilir: en az biri açıksa açık.
        self.bildirimlerAcik = sohbet || seri || odul
        self.abakusSesi = Int(GercekAbakusBoncukSesi.seciliIndeks(ayarlar: ayarlar))
    }

    private static func oku(_ anahtar: String, _ ayarlar: UserDefaults) -> Bool {
        if let deger = ayarlar.object(forKey: anahtar) as? Bool {
            return deger
        }
        return true
    }

    func sesiAyarla(_ yeni: Bool) {
        sesAcik = yeni
        ayarlar.set(yeni, forKey: "sound_enabled")
    }

    func ogreticiSesiniAyarla(_ yeni: Bool) {
        ogreticiSesiAcik = yeni
        ayarlar.set(yeni, forKey: "tutorial_sound_enabled")
    }

    func titresimiAyarla(_ yeni: Bool) {
        titresimAcik = yeni
        ayarlar.set(yeni, forKey: Titresim.anahtar)
    }

    /// Ana anahtar: üç türü birden aynı değere çeker.
    func bildirimleriAyarla(_ yeni: Bool) {
        ProfilBildirimTercihi.hepsiniAyarla(yeni, ayarlar: ayarlar)
        bildirimlerAcik = yeni
        sohbetAcik = yeni
        seriAcik = yeni
        odulAcik = yeni
    }

    /// Tür anahtarı: ana anahtar türlerden yeniden türetilir.
    func turuAyarla(_ tur: String, _ yeni: Bool) {
        ProfilBildirimTercihi.ayarla(tur, yeni, ayarlar: ayarlar)
        if tur == ProfilBildirimTercihi.sohbet {
            sohbetAcik = yeni
        } else if tur == ProfilBildirimTercihi.seri {
            seriAcik = yeni
        } else {
            odulAcik = yeni
        }
        bildirimlerAcik = sohbetAcik || seriAcik || odulAcik
    }

    /// Android AbacusSoundPlayer.choose: seçimi kaydeder ve hemen çalar.
    func abakusSesiniSec(_ numara: Int) {
        abakusSesi = numara
        GercekAbakusBoncukSesi.sec(indeks: Int32(numara), ayarlar: ayarlar)
    }
}

struct ProfilSesAyarlariEkrani: View {
    @ObservedObject var gezinme: ProfilGezinme
    @StateObject private var model: ProfilSesAyarlariModeli = ProfilSesAyarlariModeli()
    @State private var secimPaneliAcik: Bool = false

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                baslik
                ProfilStil.ayarAyirici
                    .frame(height: 2)
                    .padding(.top, 3)
                ScrollView(.vertical, showsIndicators: false) {
                    satirlar
                        .padding(.horizontal, 20)
                        .padding(.top, 14)
                        .padding(.bottom, 20)
                }
            }
            if secimPaneliAcik {
                ProfilAbakusSesSecici(
                    secili: model.abakusSesi,
                    sec: { (numara: Int) -> Void in model.abakusSesiniSec(numara) },
                    kapat: { secimPaneliAcik = false }
                )
                .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.15), value: secimPaneliAcik)
    }

    /// soundSettingsHeader: ortada başlık (20, #BDBDBD), solda 48x48 geri düğmesi; üst boşluk 5.
    private var baslik: some View {
        ZStack {
            Text(Metin.al("fragment_sound_settings_text"))
                .font(ProfilStil.yazi(20))
                .foregroundColor(ProfilStil.ayarBaslik)
            HStack(spacing: 0) {
                Button(action: { gezinme.geri() }) {
                    Image("ic_arrow_back")
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .foregroundColor(Color.white)
                        .frame(width: 24, height: 24)
                        .frame(width: 48, height: 48)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text(Metin.al("fragment_sound_settings_btn_close_desc")))
                Spacer(minLength: 0)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 5)
    }

    private var satirlar: some View {
        VStack(spacing: 12) {
            anahtarSatiri(
                simge: "volume_up_ic",
                metin: Metin.al("fragment_sound_settings_text_2"),
                acik: Binding<Bool>(
                    get: { model.sesAcik },
                    set: { (yeni: Bool) -> Void in model.sesiAyarla(yeni) }
                )
            )
            abakusSesSatiri
            anahtarSatiri(
                simge: "volume_up_ic",
                metin: Metin.al("fragment_sound_settings_text_4"),
                acik: Binding<Bool>(
                    get: { model.ogreticiSesiAcik },
                    set: { (yeni: Bool) -> Void in model.ogreticiSesiniAyarla(yeni) }
                )
            )
            anahtarSatiri(
                simge: "vibration_ic",
                metin: Metin.al("fragment_sound_settings_text_5"),
                acik: Binding<Bool>(
                    get: { model.titresimAcik },
                    set: { (yeni: Bool) -> Void in model.titresimiAyarla(yeni) }
                )
            )
            anahtarSatiri(
                simge: "notification_ic",
                metin: Metin.al("fragment_sound_settings_text_6"),
                acik: Binding<Bool>(
                    get: { model.bildirimlerAcik },
                    set: { (yeni: Bool) -> Void in model.bildirimleriAyarla(yeni) }
                )
            )
            turSatiri(
                metin: Metin.al("settings_notify_chat"),
                aciklama: Metin.al("settings_notify_chat_desc"),
                acik: Binding<Bool>(
                    get: { model.sohbetAcik },
                    set: { (yeni: Bool) -> Void in model.turuAyarla(ProfilBildirimTercihi.sohbet, yeni) }
                )
            )
            turSatiri(
                metin: Metin.al("settings_notify_streak"),
                aciklama: nil,
                acik: Binding<Bool>(
                    get: { model.seriAcik },
                    set: { (yeni: Bool) -> Void in model.turuAyarla(ProfilBildirimTercihi.seri, yeni) }
                )
            )
            turSatiri(
                metin: Metin.al("settings_notify_reward"),
                aciklama: nil,
                acik: Binding<Bool>(
                    get: { model.odulAcik },
                    set: { (yeni: Bool) -> Void in model.turuAyarla(ProfilBildirimTercihi.odul, yeni) }
                )
            )
        }
    }

    /// 64 yüksekliğinde hint_background (köşe 8) satırı: simge 24, metin 18, sağda anahtar.
    private func anahtarSatiri(simge: String, metin: String, acik: Binding<Bool>) -> some View {
        HStack(spacing: 0) {
            Image(simge)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(ProfilStil.ayarOku)
                .frame(width: 24, height: 24)
            Text(metin)
                .font(ProfilStil.yazi(18))
                .foregroundColor(Color("dark_text"))
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.leading, 16)
            ProfilSesAnahtari(acik: acik)
        }
        .padding(.horizontal, 16)
        .frame(height: 64)
        .background(RoundedRectangle(cornerRadius: 8).fill(ProfilStil.ipucuZemini))
    }

    /// Bildirim türü satırı: soldan 32 içeride, metin 17 (+ isteğe bağlı 13'lük açıklama), sağda anahtar.
    private func turSatiri(metin: String, aciklama: String?, acik: Binding<Bool>) -> some View {
        HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                Text(metin)
                    .font(ProfilStil.yazi(17))
                    .foregroundColor(Color("dark_text"))
                    .lineLimit(1)
                if let aciklamaMetni = aciklama {
                    Text(aciklamaMetni)
                        .font(ProfilStil.yazi(13))
                        .foregroundColor(Color("dark_text_secondary"))
                        .lineLimit(2)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            ProfilSesAnahtari(acik: acik)
        }
        .padding(.leading, 32)
        .padding(.trailing, 16)
        .frame(height: 64)
        .background(RoundedRectangle(cornerRadius: 8).fill(ProfilStil.ipucuZemini))
    }

    /// rowAbacusSoundPicker: sağda anahtar yerine "Ses N" değeri ve ok; dokununca seçim paneli açılır.
    private var abakusSesSatiri: some View {
        Button(action: { secimPaneliAcik = true }) {
            HStack(spacing: 0) {
                Image("volume_up_ic")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(ProfilStil.ayarOku)
                    .frame(width: 24, height: 24)
                Text(Metin.al("fragment_sound_settings_text_3"))
                    .font(ProfilStil.yazi(18))
                    .foregroundColor(Color("dark_text"))
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.leading, 16)
                Text(Metin.al("ui_ses_x", model.abakusSesi))
                    .font(ProfilStil.yazi(15))
                    .foregroundColor(Color("dark_text_secondary"))
                    .padding(.trailing, 8)
                Image("ic_back_arrow")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(ProfilStil.ayarOku)
                    .frame(width: 16, height: 16)
                    .scaleEffect(x: -1, y: 1)
            }
            .padding(.horizontal, 16)
            .frame(height: 64)
            .background(RoundedRectangle(cornerRadius: 8).fill(ProfilStil.ipucuZemini))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// switch_track_custom (48x28, köşe 14; açık #2CDA5E, kapalı #3B4A55) + switch_thumb_custom
/// (24 çaplı siyah daire, 2 kenar boşluğu).
struct ProfilSesAnahtari: View {
    @Binding var acik: Bool

    var body: some View {
        Button(action: { acik.toggle() }) {
            ZStack {
                Capsule()
                    .fill(acik ? ProfilStil.renk(0x2CDA5E) : ProfilStil.renk(0x3B4A55))
                    .frame(width: 48, height: 28)
                Circle()
                    .fill(Color.black)
                    .frame(width: 24, height: 24)
                    .offset(x: acik ? 10 : -10)
            }
            .frame(width: 48, height: 28)
            .animation(.easeInOut(duration: 0.15), value: acik)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityValue(Text(acik ? "1" : "0"))
    }
}

/// dialog_abacus_sound_picker: ekran genişliğinin %88'i, panel_background (#2A2A2A, köşe 18), iç boşluk 20;
/// başlık (17, beyaz), sağ üstte 40x40 kapatma, beş satırlık liste. Dışına dokunmak kapatır.
struct ProfilAbakusSesSecici: View {
    let secili: Int
    let sec: (Int) -> Void
    let kapat: () -> Void

    private static let sesSayisi: Int = 5

    var body: some View {
        ZStack {
            Color.black.opacity(0.6)
                .ignoresSafeArea()
                .onTapGesture(perform: kapat)
            GeometryReader { (geo: GeometryProxy) in
                pencere
                    .frame(width: geo.size.width * 0.88)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }

    private var pencere: some View {
        ZStack(alignment: .topTrailing) {
            VStack(alignment: .leading, spacing: 0) {
                Text(Metin.al("dialog_abacus_sound_picker_abacus_sound_picker_title"))
                    .font(ProfilStil.yazi(17))
                    .foregroundColor(Color.white)
                    .padding(.top, 10)
                    .padding(.trailing, 48)
                VStack(spacing: 8) {
                    ForEach(1...ProfilAbakusSesSecici.sesSayisi, id: \.self) { (numara: Int) in
                        satir(numara)
                    }
                }
                .padding(.top, 18)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: kapat) {
                Image("ic_close_black")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(Color.white)
                    .padding(8)
                    .frame(width: 40, height: 40)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .background(RoundedRectangle(cornerRadius: 18).fill(Color("panel_background")))
    }

    /// tutorial_option_row_selector: seçiliyken #33FFD600 zemin + #FFD600 çizgi, değilse #161B22 + #30363D.
    private func satir(_ numara: Int) -> some View {
        let seciliMi: Bool = (numara == secili)
        let zemin: Color = seciliMi ? Color("tutorial_option_row_selected_bg") : Color("tutorial_option_row_normal_bg")
        let cizgi: Color = seciliMi ? Color("tutorial_option_row_selected_stroke") : Color("tutorial_option_row_normal_stroke")
        return Button(action: { sec(numara) }) {
            HStack(spacing: 0) {
                Image("ic_play_circle")
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(ProfilStil.renk(0x8B949E))
                    .frame(width: 20, height: 20)
                Text(Metin.al("ui_ses_x", numara))
                    .font(ProfilStil.yazi(16))
                    .foregroundColor(Color("dark_text"))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.leading, 12)
                Image(seciliMi ? "tutorial_option_indicator_on" : "tutorial_option_indicator_off")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 10)
                    .fill(zemin)
                    .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(cizgi, lineWidth: 1))
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

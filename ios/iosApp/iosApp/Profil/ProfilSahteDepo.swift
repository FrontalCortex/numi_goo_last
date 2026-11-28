import Foundation
import Shared
import UIKit

/// Sahte profil deposunu uygulamanın sahte ortamına bağlayan nesneler (Yonetim/SahteOrtam.swift kurar).
/// Bağlı kipte "ben" giriş yapan hesaptır; plan cüzdandan, rozet/kupa belgeleri Kupa ekranlarının
/// depolarından, sahip olunan boncuklar Özelleştirme'nin sahiplik kayıtlarından, en uzun seri seri
/// servisinden okunur (Android: hepsi `users/{uid}` ve alt koleksiyonlarıdır).
struct SahteProfilBaglantisi {
    let kimlik: SahteKimlikServisi
    let cuzdan: MagazaSahteCuzdanServisi
    let kupaDeposu: SahteKupaDeposu?
    let rozetDeposu: SahteKupaRozetDeposu?
    let sahiplikler: SahteOrtamSahiplikleri
}

// Firebase bağlanana kadar kullanılan sahte depo: örnek veriyi bellekte tutar, yazımlar yalnızca
// bu nesnenin ömrü boyunca geçerlidir. Gecikmeler yükleme durumlarının görülebilmesi içindir.
//
// İki kip:
// - `init()` (ekran galerisi): sabit örnek kişi "Elif" ve örnek takip listeleri.
// - `init(baglanti:)` (uygulamanın gerçek akışı): oturum, kimlik servisindeki hesaptır; çıkış ve
//   hesap silme kimlik servisinde yapılır. Örnek diğer kişiler (arama/takip için) yine vardır.
@MainActor
final class SahteProfilDeposu: ProfilDeposu {
    private struct Kisi {
        var belge: ProfilBelgesi
        var rozet: [String: Any]
        var kupa: [String: Any]
        var boncuk: String?
    }

    private let baglanti: SahteProfilBaglantisi?
    private var oturumBilgisi: ProfilOturumBilgisi?
    private var kisiler: [String: Kisi] = [:]
    /// Hesap → takip ettikleri (en yeni başta).
    private var takipEdilenlerHaritasi: [String: [ProfilTakipKaydi]] = [:]
    /// Hesap → takipçileri (en yeni başta).
    private var takipcilerHaritasi: [String: [ProfilTakipKaydi]] = [:]
    /// Galeri kipinde sahip olunan boncuklar (bağlı kipte Özelleştirme'nin sahiplik kayıtları okunur).
    private var boncuklarim: [String: Int] = ["BOWLING": 1, "BALL3": 2]
    private var aramaKonumu: Int = 0
    private var sonArama: String = ""
    private var zamanlar: String = ""
    /// Cihazdaki avatar kopyası, hesap başına (Android: avatar_prefs `active_{uid}`); örnekte hiç kaydedilmemiş.
    private var yerelAvatarlar: [String: String] = [:]

    private static let ornekUid = "ornek-uid-ben"

    init() {
        baglanti = nil
        let simdiMs = Int64(Date().timeIntervalSince1970 * 1000)
        oturumBilgisi = ProfilOturumBilgisi(
            uid: SahteProfilDeposu.ornekUid,
            kullaniciKodu: "ELIF482913",
            ad: "Elif",
            gorunenAd: "Elif",
            eposta: "elif@ornek.com",
            saglayicilar: ["password"],
            kayitYili: 2025,
            sonGirisMs: simdiMs
        )
        ornekVeriyiKur()
        ornekKisileriKur()
    }

    /// Uygulamanın sahte ortamına bağlı depo (Yonetim/SahteOrtam.swift kurar). Örnek diğer kişiler
    /// ilk kullanımda kurulur.
    nonisolated init(baglanti: SahteProfilBaglantisi) {
        self.baglanti = baglanti
        self.oturumBilgisi = nil
    }

    /// "Ben": galeride örnek kişi, bağlı kipte oturumdaki hesap (oturum yoksa boş).
    private var benimUid: String {
        if let b = baglanti {
            return b.kimlik.mevcutOturum?.uid ?? ""
        }
        return SahteProfilDeposu.ornekUid
    }

    private var takipEdilenler: [ProfilTakipKaydi] {
        get { return takipEdilenlerHaritasi[benimUid] ?? [] }
        set { takipEdilenlerHaritasi[benimUid] = newValue }
    }

    private var takipciler: [ProfilTakipKaydi] {
        get { return takipcilerHaritasi[benimUid] ?? [] }
        set { takipcilerHaritasi[benimUid] = newValue }
    }

    private func ornekVeriyiKur() {
        let ben = ProfilBelgesi(
            uid: SahteProfilDeposu.ornekUid, ad: "Elif", kullaniciKodu: "ELIF482913",
            avatarConfig: "personas|backgroundColor=ffd5dc;body=rounded;clothingColor=7555ca;eyes=happy;facialHair=;hair=pigtails;hairColor=6c4545;mouth=bigSmile;nose=smallRound;skinColor=eeb4a4",
            toplamSureSn: 93_784, enUzunSeri: 7, plan: "Free", takipciSayisi: 2, takipSayisi: 2,
            olusturmaYili: 2025, rol: "STUDENT"
        )
        kisiler[ben.uid] = Kisi(
            belge: ben,
            rozet: [
                "userDartProgress": 12, "userKarateProgress": 1, "userGolfProgress": 6,
                "userRocketProgress": 4, "userDinoProgress": 620
            ],
            kupa: ["addition_abacus_cup": 640, "extraction_abacus_cup": 310, "blinding_addition_abacus_cup": 255],
            boncuk: "SOROBAN"
        )
    }

    /// Arama ve takip için örnek diğer kişiler (iki kipte de aynı).
    private func ornekKisileriKur() {
        // Avatarı olmayan kullanıcı (nil) listelerde harfli daireyle çizilir.
        let ornekler: [(String, String, String, String?, String)] = [
            ("ornek-uid-1", "Ahmet", "AHME120344", "personas|backgroundColor=65c9ff;body=squared;clothingColor=456dff;eyes=open;facialHair=;hair=shortCombover;hairColor=362c47;mouth=smile;nose=mediumRound;skinColor=d78774", "Pro"),
            ("ornek-uid-2", "Zeynep", "ZEYN558210", nil, "Free"),
            ("ornek-uid-3", "Mert", "MERT903117", "personas|backgroundColor=262e33;body=checkered;clothingColor=e24553;eyes=sunglasses;facialHair=;hair=cap;hairColor=f29c65;mouth=smirk;nose=wrinkles;skinColor=92594b", "Lite"),
            ("ornek-uid-4", "Elçin", "ELCI774402", "personas|backgroundColor=a7ffc4;body=small;clothingColor=f3b63a;eyes=wink;facialHair=;hair=curlyBun;hairColor=e16381;mouth=lips;nose=smallRound;skinColor=e7a391", "Free"),
            ("ornek-uid-5", "Elanur", "ELAN230915", nil, "Free"),
            ("ornek-uid-6", "Defne", "DEFN661208", "personas|backgroundColor=ffdfbf;body=rounded;clothingColor=54d7c7;eyes=glasses;facialHair=;hair=bobCut;hairColor=dee1f5;mouth=surprise;nose=mediumRound;skinColor=623d36", "Free")
        ]
        for (sira, ornek) in ornekler.enumerated() {
            let belge = ProfilBelgesi(
                uid: ornek.0, ad: ornek.1, kullaniciKodu: ornek.2, avatarConfig: ornek.3,
                toplamSureSn: Int64(3_600 * (sira + 1) + 540), enUzunSeri: Int64(sira * 3), plan: ornek.4,
                takipciSayisi: sira + 1, takipSayisi: sira, olusturmaYili: 2024 + (sira % 2), rol: nil
            )
            kisiler[belge.uid] = Kisi(
                belge: belge,
                rozet: ["userDartProgress": 3 * (sira + 1), "userBowlingProgress": 5 * sira],
                kupa: ["addition_abacus_cup": 200 + 75 * sira],
                boncuk: sira % 2 == 0 ? "BOWLING" : nil
            )
        }
        if baglanti == nil {
            takipEdilenler = [kayit("ornek-uid-1"), kayit("ornek-uid-2")].compactMap { $0 }
            takipciler = [kayit("ornek-uid-3"), kayit("ornek-uid-1")].compactMap { $0 }
        }
    }

    /// Bağlı kipte örnek diğer kişiler ilk kullanımda, oturumdaki hesabın profil kaydı da yoksa
    /// `users/{uid}` belgesinden kurulur (yeni hesap: avatar yok, süre 0, takip 0).
    private func beniHazirla() {
        guard let b = baglanti else { return }
        if kisiler["ornek-uid-1"] == nil {
            ornekKisileriKur()
        }
        guard let acik = b.kimlik.mevcutOturum, kisiler[acik.uid] == nil else { return }
        let kurallar = AuthRules.shared
        let belge: [String: Any] = b.kimlik.kayit.anlikBelge(uid: acik.uid) ?? [:]
        let ad: String = (belge[kurallar.FIELD_NAME] as? String) ?? acik.ad
        let kod: String = (belge[kurallar.FIELD_USER_ID] as? String) ?? acik.kullaniciKimligi
        let rol: String? = belge[kurallar.FIELD_ROLE] as? String
        let yil: Int = SahteProfilDeposu.yil(belge[kurallar.FIELD_CREATED_AT])
        let profil = ProfilBelgesi(
            uid: acik.uid, ad: ad, kullaniciKodu: kod, avatarConfig: nil,
            toplamSureSn: 0, enUzunSeri: 0, plan: nil, takipciSayisi: 0, takipSayisi: 0,
            olusturmaYili: yil, rol: rol
        )
        kisiler[acik.uid] = Kisi(belge: profil, rozet: [:], kupa: [:], boncuk: nil)
    }

    /// Epoch ms (NSNumber) → yıl; yoksa bu yıl.
    private static func yil(_ deger: Any?) -> Int {
        let takvim = Calendar(identifier: .gregorian)
        var tarih: Date = Date()
        if let sayi = deger as? NSNumber {
            tarih = Date(timeIntervalSince1970: sayi.doubleValue / 1000.0)
        }
        return takvim.component(.year, from: tarih)
    }

    /// Android providerData: sahte kimlik servisi Google/Apple hesaplarının uid'sine sağlayıcı adını koyar.
    private static func saglayicilar(uid: String) -> [String] {
        if uid.hasPrefix("sahte-google-") {
            return ["google.com"]
        }
        if uid.hasPrefix("sahte-apple-") {
            return ["apple.com"]
        }
        return ["password"]
    }

    private func kayit(_ uid: String) -> ProfilTakipKaydi? {
        guard let kisi = kisiler[uid], let ad = kisi.belge.ad else { return nil }
        return ProfilTakipKaydi(uid: uid, ad: ad, kullaniciKodu: kisi.belge.kullaniciKodu)
    }

    private func bekle(_ milisaniye: UInt64 = 350) async {
        try? await Task.sleep(nanoseconds: milisaniye * 1_000_000)
    }

    /// Bağlı kipte verilen uid oturumdaki hesap mı.
    private func bagliBenMi(_ uid: String) -> Bool {
        return baglanti != nil && !uid.isEmpty && uid == benimUid
    }

    // MARK: ProfilDeposu

    var oturum: ProfilOturumBilgisi? {
        guard let b = baglanti else { return oturumBilgisi }
        guard let acik = b.kimlik.mevcutOturum else { return nil }
        let kurallar = AuthRules.shared
        let belge: [String: Any]? = b.kimlik.kayit.anlikBelge(uid: acik.uid)
        let ad: String = (belge?[kurallar.FIELD_NAME] as? String) ?? acik.ad
        let kod: String = (belge?[kurallar.FIELD_USER_ID] as? String) ?? acik.kullaniciKimligi
        return ProfilOturumBilgisi(
            uid: acik.uid,
            kullaniciKodu: kod,
            ad: ad,
            gorunenAd: ad,
            eposta: acik.eposta,
            saglayicilar: SahteProfilDeposu.saglayicilar(uid: acik.uid),
            kayitYili: SahteProfilDeposu.yil(belge?[kurallar.FIELD_CREATED_AT]),
            sonGirisMs: b.kimlik.oturumAcilisMs
        )
    }

    func profilBelgesi(uid: String, baskasi: Bool) async throws -> ProfilBelgesi? {
        await bekle()
        beniHazirla()
        guard var belge = kisiler[uid]?.belge else { return nil }
        if let b = baglanti, bagliBenMi(uid) {
            // Plan tek kaynaktan: cüzdan (Android: `users/{uid}.plan`); en uzun seri seri servisinden.
            belge.plan = b.cuzdan.cuzdan.plan
            belge.enUzunSeri = Int64(SeriOrtak.servis.profilEnUzunSeri)
        }
        return belge
    }

    func tamamlanmaYuzdesi(uid: String) async -> Int {
        await bekle(500)
        return uid == benimUid ? 12 : 27
    }

    func senkronlanmamisSureSn() -> Int64 {
        return 95
    }

    func rozetDurumBelgesi(uid: String) async throws -> [String: Any]? {
        await bekle(450)
        if let b = baglanti, bagliBenMi(uid) {
            // Rozetler ekranıyla aynı belge (`users/{uid}/badgeProgress/state`).
            return b.rozetDeposu?.dokuman
        }
        return kisiler[uid]?.rozet
    }

    func rozetVarsayilanlariniYaz(uid: String, alanlar: [String: Any]) async {
        if let b = baglanti, bagliBenMi(uid) {
            guard let depo = b.rozetDeposu else { return }
            var belge: [String: Any] = depo.dokuman ?? [:]
            for (anahtar, deger) in alanlar where belge[anahtar] == nil {
                belge[anahtar] = deger
            }
            depo.dokuman = belge
            return
        }
        guard var kisi = kisiler[uid] else { return }
        for (anahtar, deger) in alanlar where kisi.rozet[anahtar] == nil {
            kisi.rozet[anahtar] = deger
        }
        kisiler[uid] = kisi
    }

    func kupaIlerlemeBelgesi(uid: String) async throws -> [String: Any]? {
        await bekle(400)
        if let b = baglanti, bagliBenMi(uid) {
            // Kupa Yolu ekranlarıyla aynı belge (`users/{uid}/cupWayProgress/progress`).
            guard let depo = b.kupaDeposu else { return nil }
            let skorlar: [String: Int] = depo.skorlar
            var belge: [String: Any] = [:]
            for (alan, skor) in skorlar {
                belge[alan] = skor
            }
            return belge
        }
        return kisiler[uid]?.kupa
    }

    func onizlemeBoncukTipi(uid: String) async -> String? {
        await bekle(200)
        return kisiler[uid]?.boncuk
    }

    func sahipOlunanBoncuklar(uid: String) async -> [String: Int] {
        await bekle(300)
        if let b = baglanti {
            if !bagliBenMi(uid) {
                return [:]
            }
            // Özelleştirme ekranının sahiplik kayıtları (`users/{uid}/haveBead/{tip}.count`).
            let servis: OzellestirmeSahteSahiplikServisi = b.sahiplikler.servis(uid: uid)
            let kayitlar: [String: OzellestirmeBoncukKaydi] = await withCheckedContinuation {
                (devam: CheckedContinuation<[String: OzellestirmeBoncukKaydi], Never>) in
                servis.boncuklariYukle { sonuc in
                    devam.resume(returning: sonuc)
                }
            }
            var adetler: [String: Int] = [:]
            for (tip, boncukKaydi) in kayitlar where boncukKaydi.adet > 0 {
                adetler[tip] = boncukKaydi.adet
            }
            return adetler
        }
        return uid == benimUid ? boncuklarim : [:]
    }

    func onizlemeBoncukTipiniYaz(uid: String, tip: String) async {
        beniHazirla()
        kisiler[uid]?.boncuk = tip
    }

    func takipEdiyorMuyum(hedefUid: String) async throws -> Bool {
        await bekle(250)
        return takipEdilenler.contains { $0.uid == hedefUid }
    }

    func takipEt(hedefUid: String, hedefKod: String, hedefAd: String) async throws {
        await bekle(300)
        guard oturum != nil else { throw ProfilHata.diger("Oturum yok") }
        beniHazirla()
        let ben: String = benimUid
        if !takipEdilenler.contains(where: { $0.uid == hedefUid }) {
            takipEdilenler.insert(ProfilTakipKaydi(uid: hedefUid, ad: hedefAd, kullaniciKodu: hedefKod), at: 0)
            // Sayaçları gerçekte sunucudaki tetikleyiciler günceller.
            kisiler[hedefUid]?.belge.takipciSayisi += 1
            kisiler[ben]?.belge.takipSayisi += 1
        }
    }

    func takibiBirak(hedefUid: String) async throws {
        await bekle(300)
        guard oturum != nil else { throw ProfilHata.diger("Oturum yok") }
        beniHazirla()
        let ben: String = benimUid
        if takipEdilenler.contains(where: { $0.uid == hedefUid }) {
            takipEdilenler.removeAll { $0.uid == hedefUid }
            // Sayaçları gerçekte sunucudaki tetikleyiciler (onFollowerDeleted / onFollowingDeleted) düşürür.
            if let sayi = kisiler[hedefUid]?.belge.takipciSayisi, sayi > 0 {
                kisiler[hedefUid]?.belge.takipciSayisi = sayi - 1
            }
            if let sayi = kisiler[ben]?.belge.takipSayisi, sayi > 0 {
                kisiler[ben]?.belge.takipSayisi = sayi - 1
            }
        }
    }

    func takipEttiklerim() async -> Set<String> {
        return Set(takipEdilenler.map { $0.uid })
    }

    func takipListesi(sekme: Int) async throws -> [ProfilTakipKaydi] {
        await bekle()
        return sekme == 0 ? takipEdilenler : takipciler
    }

    func acikAvatarlar(uidler: [String]) async throws -> [String: String] {
        var sonuc: [String: String] = [:]
        for uid in uidler {
            if let config = kisiler[uid]?.belge.avatarConfig {
                sonuc[uid] = config
            }
        }
        return sonuc
    }

    func ara(sorgu: String, sayfaBoyu: Int, devam: Bool) async -> [ProfilAramaSonucu] {
        await bekle()
        beniHazirla()
        if !devam || sorgu != sonArama {
            aramaKonumu = 0
            sonArama = sorgu
        }
        let benimKodum = oturum?.kullaniciKodu ?? ""
        // Firestore önek sorgusu büyük/küçük harfe duyarlıdır; burada da öyle.
        let eslesenler = kisiler.values
            .map { $0.belge }
            .filter { belge in
                guard let ad = belge.ad else { return false }
                if belge.kullaniciKodu == benimKodum { return false }
                return ad.hasPrefix(sorgu) || belge.kullaniciKodu.hasPrefix(sorgu)
            }
            .sorted { ($0.ad ?? "") < ($1.ad ?? "") }
        let dilim = eslesenler.dropFirst(aramaKonumu).prefix(sayfaBoyu)
        aramaKonumu += dilim.count
        return dilim.map { belge in
            ProfilAramaSonucu(
                uid: belge.uid, ad: belge.ad ?? "", kullaniciKodu: belge.kullaniciKodu, avatarConfig: belge.avatarConfig
            )
        }
    }

    var yerelAvatar: String? {
        get { return yerelAvatarlar[benimUid] }
        set { yerelAvatarlar[benimUid] = newValue }
    }

    func avatarKaydet(_ config: String) async throws {
        await bekle(200)
        beniHazirla()
        kisiler[benimUid]?.belge.avatarConfig = config
    }

    func adGuncelle(_ ad: String) async throws {
        await bekle()
        guard oturum != nil else { throw ProfilHata.kimlik }
        beniHazirla()
        let ben: String = benimUid
        if let b = baglanti {
            // Android: Auth displayName + `users/{uid}.name`.
            do {
                try await b.kimlik.kayit.alanlariGuncelle(uid: ben, alanlar: [AuthRules.shared.FIELD_NAME: ad])
            } catch {
                throw ProfilHata.veritabani
            }
        } else {
            oturumBilgisi?.gorunenAd = ad
            oturumBilgisi?.ad = ad
        }
        kisiler[ben]?.belge.ad = ad
    }

    func parolaGuncelle(_ parola: String) async throws {
        await bekle()
        guard oturum != nil else { throw ProfilHata.diger("Oturum yok") }
    }

    func cikisYap() {
        if let b = baglanti {
            // Yalnızca oturum kapanır; hesap ve verileri kalır (Android: AuthManager.logout).
            b.kimlik.oturumuKapat()
            return
        }
        oturumBilgisi = nil
    }

    func hesabiSil() async -> ProfilHesapSilmeSonucu {
        await bekle(1200)
        guard oturum != nil else { return .hata("Kullanıcı bulunamadı.") }
        let ben: String = benimUid
        kisiler[ben] = nil
        takipEdilenlerHaritasi[ben] = nil
        takipcilerHaritasi[ben] = nil
        yerelAvatarlar[ben] = nil
        if let b = baglanti {
            // Hesap kimlik servisinden de silinir: aynı hesapla artık giriş yapılamaz.
            b.sahiplikler.sil(uid: ben)
            b.kimlik.hesabiSil(uid: ben)
        } else {
            oturumBilgisi = nil
        }
        return .basarili
    }

    var geriBildirimZamanlari: String {
        get { zamanlar }
        set { zamanlar = newValue }
    }

    func geriBildirimGonder(mesaj: String) async throws {
        await bekle(700)
        let bilgi: ProfilOturumBilgisi? = oturum
        // Gerçek uygulamada yazılacak doküman:
        _ = ProfilOrtak.geriBildirimBelgesi(
            uid: bilgi?.uid,
            eposta: bilgi?.eposta,
            mesaj: mesaj,
            surum: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String,
            isletimSistemi: UIDevice.current.systemVersion,
            cihaz: "Apple " + UIDevice.current.model
        )
    }
}

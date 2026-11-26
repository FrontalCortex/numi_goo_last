import Foundation
import UIKit

// Firebase bağlanana kadar kullanılan sahte depo: örnek veriyi bellekte tutar, yazımlar yalnızca
// bu nesnenin ömrü boyunca geçerlidir. Gecikmeler yükleme durumlarının görülebilmesi içindir.
@MainActor
final class SahteProfilDeposu: ProfilDeposu {
    private struct Kisi {
        var belge: ProfilBelgesi
        var rozet: [String: Any]
        var kupa: [String: Any]
        var boncuk: String?
    }

    private var oturumBilgisi: ProfilOturumBilgisi?
    private var kisiler: [String: Kisi] = [:]
    /// Benim takip ettiklerim (en yeni başta).
    private var takipEdilenler: [ProfilTakipKaydi] = []
    /// Beni takip edenler (en yeni başta).
    private var takipciler: [ProfilTakipKaydi] = []
    private var boncuklarim: [String: Int] = ["BOWLING": 1, "BALL3": 2]
    private var aramaKonumu: Int = 0
    private var sonArama: String = ""
    private var zamanlar: String = ""

    private static let benimUid = "ornek-uid-ben"

    init() {
        let simdiMs = Int64(Date().timeIntervalSince1970 * 1000)
        oturumBilgisi = ProfilOturumBilgisi(
            uid: SahteProfilDeposu.benimUid,
            kullaniciKodu: "ELIF482913",
            ad: "Elif",
            gorunenAd: "Elif",
            eposta: "elif@ornek.com",
            saglayicilar: ["password"],
            kayitYili: 2025,
            sonGirisMs: simdiMs
        )
        ornekVeriyiKur()
    }

    private func ornekVeriyiKur() {
        let ben = ProfilBelgesi(
            uid: SahteProfilDeposu.benimUid, ad: "Elif", kullaniciKodu: "ELIF482913", seciliAvatar: 3,
            toplamSureSn: 93_784, plan: "Free", takipciSayisi: 2, takipSayisi: 2, olusturmaYili: 2025, rol: "STUDENT"
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

        let ornekler: [(String, String, String, Int, String)] = [
            ("ornek-uid-1", "Ahmet", "AHME120344", 5, "Pro"),
            ("ornek-uid-2", "Zeynep", "ZEYN558210", 0, "Free"),
            ("ornek-uid-3", "Mert", "MERT903117", 8, "Lite"),
            ("ornek-uid-4", "Elçin", "ELCI774402", 11, "Free"),
            ("ornek-uid-5", "Elanur", "ELAN230915", 0, "Free"),
            ("ornek-uid-6", "Defne", "DEFN661208", 2, "Free")
        ]
        for (sira, ornek) in ornekler.enumerated() {
            let belge = ProfilBelgesi(
                uid: ornek.0, ad: ornek.1, kullaniciKodu: ornek.2, seciliAvatar: ornek.3,
                toplamSureSn: Int64(3_600 * (sira + 1) + 540), plan: ornek.4,
                takipciSayisi: sira + 1, takipSayisi: sira, olusturmaYili: 2024 + (sira % 2), rol: nil
            )
            kisiler[belge.uid] = Kisi(
                belge: belge,
                rozet: ["userDartProgress": 3 * (sira + 1), "userBowlingProgress": 5 * sira],
                kupa: ["addition_abacus_cup": 200 + 75 * sira],
                boncuk: sira % 2 == 0 ? "BOWLING" : nil
            )
        }
        takipEdilenler = [kayit("ornek-uid-1"), kayit("ornek-uid-2")].compactMap { $0 }
        takipciler = [kayit("ornek-uid-3"), kayit("ornek-uid-1")].compactMap { $0 }
    }

    private func kayit(_ uid: String) -> ProfilTakipKaydi? {
        guard let kisi = kisiler[uid], let ad = kisi.belge.ad else { return nil }
        return ProfilTakipKaydi(uid: uid, ad: ad, kullaniciKodu: kisi.belge.kullaniciKodu)
    }

    private func bekle(_ milisaniye: UInt64 = 350) async {
        try? await Task.sleep(nanoseconds: milisaniye * 1_000_000)
    }

    // MARK: ProfilDeposu

    var oturum: ProfilOturumBilgisi? { oturumBilgisi }

    func profilBelgesi(uid: String, baskasi: Bool) async throws -> ProfilBelgesi? {
        await bekle()
        return kisiler[uid]?.belge
    }

    func tamamlanmaYuzdesi(uid: String) async -> Int {
        await bekle(500)
        return uid == SahteProfilDeposu.benimUid ? 12 : 27
    }

    func senkronlanmamisSureSn() -> Int64 {
        return 95
    }

    func rozetDurumBelgesi(uid: String) async throws -> [String: Any]? {
        await bekle(450)
        return kisiler[uid]?.rozet
    }

    func rozetVarsayilanlariniYaz(uid: String, alanlar: [String: Any]) async {
        guard var kisi = kisiler[uid] else { return }
        for (anahtar, deger) in alanlar where kisi.rozet[anahtar] == nil {
            kisi.rozet[anahtar] = deger
        }
        kisiler[uid] = kisi
    }

    func kupaIlerlemeBelgesi(uid: String) async throws -> [String: Any]? {
        await bekle(400)
        return kisiler[uid]?.kupa
    }

    func onizlemeBoncukTipi(uid: String) async -> String? {
        await bekle(200)
        return kisiler[uid]?.boncuk
    }

    func sahipOlunanBoncuklar(uid: String) async -> [String: Int] {
        await bekle(300)
        return uid == SahteProfilDeposu.benimUid ? boncuklarim : [:]
    }

    func onizlemeBoncukTipiniYaz(uid: String, tip: String) async {
        kisiler[uid]?.boncuk = tip
    }

    func takipEdiyorMuyum(hedefUid: String) async throws -> Bool {
        await bekle(250)
        return takipEdilenler.contains { $0.uid == hedefUid }
    }

    func takipEt(hedefUid: String, hedefKod: String, hedefAd: String) async throws {
        await bekle(300)
        guard oturumBilgisi != nil else { throw ProfilHata.diger("Oturum yok") }
        if !takipEdilenler.contains(where: { $0.uid == hedefUid }) {
            takipEdilenler.insert(ProfilTakipKaydi(uid: hedefUid, ad: hedefAd, kullaniciKodu: hedefKod), at: 0)
            // Sayaçları gerçekte sunucudaki tetikleyiciler günceller.
            kisiler[hedefUid]?.belge.takipciSayisi += 1
            kisiler[SahteProfilDeposu.benimUid]?.belge.takipSayisi += 1
        }
    }

    func geriTakipEt(hedefUid: String, hedefKod: String, hedefAd: String) async throws {
        // Android'deki çapraz yazım burada canlandırılmaz; liste doğru adla tutulur.
        try await takipEt(hedefUid: hedefUid, hedefKod: hedefKod, hedefAd: hedefAd)
    }

    func takipEttiklerim() async -> Set<String> {
        return Set(takipEdilenler.map { $0.uid })
    }

    func takipListesi(sekme: Int) async throws -> [ProfilTakipKaydi] {
        await bekle()
        return sekme == 0 ? takipEdilenler : takipciler
    }

    func acikAvatarlar(uidler: [String]) async throws -> [String: Int] {
        var sonuc: [String: Int] = [:]
        for uid in uidler {
            sonuc[uid] = kisiler[uid]?.belge.seciliAvatar ?? 0
        }
        return sonuc
    }

    func ara(sorgu: String, sayfaBoyu: Int, devam: Bool) async -> [ProfilAramaSonucu] {
        await bekle()
        if !devam || sorgu != sonArama {
            aramaKonumu = 0
            sonArama = sorgu
        }
        let benimKodum = oturumBilgisi?.kullaniciKodu ?? ""
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
                uid: belge.uid, ad: belge.ad ?? "", kullaniciKodu: belge.kullaniciKodu, seciliAvatar: belge.seciliAvatar
            )
        }
    }

    func avatarKaydet(_ avatar: Int) async throws {
        await bekle(200)
        kisiler[SahteProfilDeposu.benimUid]?.belge.seciliAvatar = avatar
    }

    func adGuncelle(_ ad: String) async throws {
        await bekle()
        guard oturumBilgisi != nil else { throw ProfilHata.kimlik }
        oturumBilgisi?.gorunenAd = ad
        oturumBilgisi?.ad = ad
        kisiler[SahteProfilDeposu.benimUid]?.belge.ad = ad
    }

    func parolaGuncelle(_ parola: String) async throws {
        await bekle()
        guard oturumBilgisi != nil else { throw ProfilHata.diger("Oturum yok") }
    }

    func cikisYap() {
        oturumBilgisi = nil
    }

    func hesabiSil() async -> ProfilHesapSilmeSonucu {
        await bekle(1200)
        guard oturumBilgisi != nil else { return .hata("Kullanıcı bulunamadı.") }
        kisiler[SahteProfilDeposu.benimUid] = nil
        takipEdilenler = []
        takipciler = []
        oturumBilgisi = nil
        return .basarili
    }

    var geriBildirimZamanlari: String {
        get { zamanlar }
        set { zamanlar = newValue }
    }

    func geriBildirimGonder(mesaj: String) async throws {
        await bekle(700)
        // Gerçek uygulamada yazılacak doküman:
        _ = ProfilOrtak.geriBildirimBelgesi(
            uid: oturumBilgisi?.uid,
            eposta: oturumBilgisi?.eposta,
            mesaj: mesaj,
            surum: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String,
            isletimSistemi: UIDevice.current.systemVersion,
            cihaz: "Apple " + UIDevice.current.model
        )
    }
}

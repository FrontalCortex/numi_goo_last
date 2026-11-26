import Foundation
import SwiftUI

// Profil sayfasının (kendi profili ya da başkasının profili) durumu.
// Android karşılığı: ProfileFragment — loadUserData, updateFollowButtonState, followTargetUser,
// loadBadgeProgressFromFirestore, loadAbacusPreview, loadCupPathScores(ForUser).
// Metin biçimleri, rozet seçimi ve kupa kartı tablosu ortak koddan gelir (ProfilOrtak).

/// Kupa yolu kartının ekrandaki hali.
struct ProfilKupaKarti: Identifiable {
    let id: Int
    let tanim: ProfilKupaKartTanimi
    /// Yüklenene kadar "-".
    var skor: String
}

@MainActor
final class ProfilSayfaModeli: ObservableObject {
    /// Başkasının profili gösteriliyorsa onun uid'si; kendi profilinde nil.
    let hedefUid: String?
    private let depo: ProfilDeposu

    @Published var avatarAdi: String = "non_user"
    @Published var ad: String = ""
    @Published var kullaniciSatiri: String = ""
    @Published var takipSayisi: String = "-"
    @Published var takipciSayisi: String = "-"

    /// Android: showLoadingState / hideLoadingState — üç ilerleme satırı yüklenene kadar gizlidir.
    @Published var ilerlemeGorunur: Bool = false
    @Published var tamamlanma: String = ""
    @Published var toplamSure: String = ""
    @Published var abonelik: String = ""

    @Published var kupalar: [ProfilKupaKarti] = []
    @Published var rozetler: [ProfilRozetYuvasi] = []
    /// Android: profileBadgesCard rozet belgesi okunana kadar gizlidir.
    @Published var rozetKartiGorunur: Bool = false

    /// Abaküs önizleme kartındaki boncuk tipi (BeadType adı); bilinmiyorsa nil.
    @Published var boncukTipi: String? = nil
    @Published var abakusPaneliAcik: Bool = false
    @Published var avatarSeciciAcik: Bool = false

    // Başkasının profilindeki TAKİP ET düğmesi.
    @Published var takipDugmesiMetni: String = ProfilOrtak.takipEtMetni
    @Published var takipDugmesiEtkin: Bool = true
    @Published var takipDugmesiOpaklik: Double = 1.0

    private var yuklendi: Bool = false
    private var yukleniyor: Bool = false
    private var boncukKayitGorevi: Task<Void, Never>? = nil

    init(depo: ProfilDeposu, hedefUid: String?) {
        self.depo = depo
        self.hedefUid = hedefUid
        var sira = 0
        var kartlar: [ProfilKupaKarti] = []
        for tanim in ProfilOrtak.kupaKartlari {
            kartlar.append(ProfilKupaKarti(id: sira, tanim: tanim, skor: "-"))
            sira += 1
        }
        kupalar = kartlar
    }

    /// Android: isOtherUser.
    var baskasi: Bool {
        guard let hedef = hedefUid else { return false }
        return hedef != depo.oturum?.uid
    }

    private var uid: String? {
        return hedefUid ?? depo.oturum?.uid
    }

    /// Android: onViewCreated → loadUserData; onResume'da henüz yüklenmediyse yeniden.
    func gorundu() {
        if !yuklendi && !yukleniyor {
            Task { await yukle() }
        }
    }

    /// Profil düzenleme / avatar değişikliği sonrası yeniden okuma.
    func yenile() {
        if !yukleniyor {
            Task { await yukle() }
        }
    }

    func yukle() async {
        if yukleniyor { return }
        guard let uid = uid else { return }
        yukleniyor = true
        defer {
            yukleniyor = false
            yuklendi = true
        }

        let belge: ProfilBelgesi?
        do {
            belge = try await depo.profilBelgesi(uid: uid, baskasi: baskasi)
        } catch {
            // Android: hata dalı — boş rozet durumu, satırlar görünür, rozet kartı gizli kalır.
            rozetler = ProfilOrtak.rozetYuvalari(durumBelgesi: nil)
            ilerlemeGorunur = true
            return
        }

        guard let profil = belge else {
            // Android: belge yok — boş rozet durumu, avatar yok, plan "Free".
            rozetler = ProfilOrtak.rozetYuvalari(durumBelgesi: nil)
            avatarAdi = ProfilOrtak.avatarAdi(0)
            abonelik = ProfilOrtak.abonelikMetni(nil)
            ilerlemeGorunur = true
            return
        }

        // Android bu üç yüklemeyi belge gelir gelmez başlatır; sonuçlar ayrı ayrı gelir.
        Task { await rozetleriYukle(uid: uid) }
        Task { await boncuguYukle(uid: uid) }
        Task { await kupalariYukle(uid: uid) }

        avatarAdi = ProfilOrtak.avatarAdi(profil.seciliAvatar)
        let oturum = depo.oturum
        ad = ProfilOrtak.gorunenAd(belgeAdi: profil.ad, oturumAdi: oturum?.gorunenAd, baskasi: baskasi)

        // Katılım yılı: kendi profilinde Auth kaydı, başkasında belgedeki createdAt (yoksa bu yıl).
        let buYil = Calendar.current.component(.year, from: Date())
        let yil: Int = baskasi ? (profil.olusturmaYili ?? buYil) : (oturum?.kayitYili ?? buYil)
        kullaniciSatiri = ProfilOrtak.kullaniciSatiri(kod: profil.kullaniciKodu, yil: yil)

        tamamlanma = ProfilOrtak.tamamlanmaBekleme
        Task {
            let yuzde = await depo.tamamlanmaYuzdesi(uid: uid)
            self.tamamlanma = ProfilOrtak.tamamlanmaMetni(yuzde)
        }

        // Kendi profilinde henüz Firestore'a yazılmamış süre de eklenir (Android: TimeTracker.unsyncedTimeFlow;
        // orada akış dinlenir, burada tek seferlik okunur).
        var sure: Int64 = profil.toplamSureSn
        if !baskasi {
            sure += depo.senkronlanmamisSureSn()
        }
        toplamSure = ProfilOrtak.sureMetni(sure)
        abonelik = ProfilOrtak.abonelikMetni(profil.plan)

        takipciSayisi = String(profil.takipciSayisi)
        takipSayisi = String(profil.takipSayisi)

        if baskasi {
            Task { await takipDugmesiniGuncelle(hedef: uid) }
        }
        ilerlemeGorunur = true
    }

    // MARK: Rozetler

    private func rozetleriYukle(uid: String) async {
        rozetKartiGorunur = false
        do {
            let belge = try await depo.rozetDurumBelgesi(uid: uid)
            // Eksik varsayılan alanlar yalnızca kendi profilimizde yazılır.
            if !baskasi {
                let mevcut = Set((belge ?? [:]).keys)
                let eksik = ProfilOrtak.eksikRozetVarsayilanlari(mevcutAlanlar: mevcut)
                if !eksik.isEmpty {
                    await depo.rozetVarsayilanlariniYaz(uid: uid, alanlar: eksik)
                }
            }
            rozetler = ProfilOrtak.rozetYuvalari(durumBelgesi: belge)
        } catch {
            rozetler = ProfilOrtak.rozetYuvalari(durumBelgesi: nil)
        }
        rozetKartiGorunur = true
    }

    // MARK: Kupa yolu

    private func kupalariYukle(uid: String) async {
        var belge: [String: Any]? = nil
        do {
            belge = try await depo.kupaIlerlemeBelgesi(uid: uid)
        } catch {
            belge = nil
        }
        var yeni = kupalar
        for sira in yeni.indices {
            let ham: Any? = belge?[yeni[sira].tanim.alan]
            yeni[sira].skor = String(ProfilOrtak.kupaSkoru(ham))
        }
        kupalar = yeni
    }

    func kupaSecimi(_ kart: ProfilKupaKarti) -> ProfilKupaSecimi? {
        guard let uid = uid else { return nil }
        return ProfilKupaSecimi(
            uid: uid, alan: kart.tanim.alan, baslik: kart.tanim.baslik, animasyonDosyasi: kart.tanim.animasyonDosyasi
        )
    }

    // MARK: Abaküs önizlemesi

    private func boncuguYukle(uid: String) async {
        boncukTipi = await depo.onizlemeBoncukTipi(uid: uid)
    }

    /// Android: schedulePreviewBeadTypeSync — art arda ok dokunuşlarında yazım 500 ms geciktirilir.
    func boncukSecildi(_ tip: String) {
        boncukTipi = tip
        guard let uid = depo.oturum?.uid else { return }
        boncukKayitGorevi?.cancel()
        let gecikme = ProfilOrtak.boncukKayitGecikmesiNs
        let kaynak = depo
        boncukKayitGorevi = Task {
            try? await Task.sleep(nanoseconds: gecikme)
            if Task.isCancelled { return }
            await kaynak.onizlemeBoncukTipiniYaz(uid: uid, tip: tip)
        }
    }

    func sahipOlunanBoncuklar() async -> [String: Int] {
        guard let uid = depo.oturum?.uid else { return [:] }
        return await depo.sahipOlunanBoncuklar(uid: uid)
    }

    // MARK: Avatar

    /// Android: AvatarPickerFragment.onAvatarSelected — yazım başarısız olsa da ekran güncellenir.
    func avatarSecildi(_ avatar: Int) {
        avatarSeciciAcik = false
        let kaynak = depo
        Task {
            try? await kaynak.avatarKaydet(avatar)
            if ProfilOrtak.avatarGecerli(avatar) {
                self.avatarAdi = ProfilOrtak.avatarAdi(avatar)
            }
        }
    }

    // MARK: Takip (başkasının profili)

    private func takipDugmesiniGuncelle(hedef: String) async {
        // Android: okuma hatasında düğme olduğu gibi kalır.
        guard let takipte = try? await depo.takipEdiyorMuyum(hedefUid: hedef) else { return }
        if takipte {
            takipDugmesiMetni = ProfilOrtak.takipEdiliyorMetni
            takipDugmesiEtkin = false
            takipDugmesiOpaklik = 0.5
        } else {
            takipDugmesiMetni = ProfilOrtak.takipEtMetni
            takipDugmesiEtkin = true
            takipDugmesiOpaklik = 1.0
        }
    }

    /// Android: followTargetUser. Sonuç bildirimi `bildir` ile dışarı verilir.
    func takipEt(bildir: @escaping (String) -> Void) {
        guard let hedef = hedefUid, depo.oturum != nil else { return }
        // Hedefin adı ve kodu ekrandaki metinlerden alınır (Android'deki gibi).
        let hedefAd = ad
        let hedefKod = ProfilOrtak.hedefKod(kullaniciSatiri: kullaniciSatiri)
        takipDugmesiEtkin = false
        let kaynak = depo
        Task {
            do {
                try await kaynak.takipEt(hedefUid: hedef, hedefKod: hedefKod, hedefAd: hedefAd)
                self.takipDugmesiMetni = ProfilOrtak.takipEdiliyorMetni
                self.takipDugmesiOpaklik = 0.5
                bildir(ProfilOrtak.takipEdildiBildirimi)
            } catch {
                self.takipDugmesiEtkin = true
                bildir("Hata: " + ProfilSayfaModeli.hataMetni(error))
            }
        }
    }

    static func hataMetni(_ hata: Error) -> String {
        if let profilHatasi = hata as? ProfilHata {
            return profilHatasi.mesaj
        }
        return hata.localizedDescription
    }
}
